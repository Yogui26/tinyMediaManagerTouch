/*
 * Copyright 2012 - 2026 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.thirdparty.simkl;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmStore;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.license.TmmFeature;
import org.tinymediamanager.scraper.http.TmmHttpClient;
import org.tinymediamanager.thirdparty.simkl.model.SimklDeviceCodeResponse;
import org.tinymediamanager.thirdparty.simkl.model.SimklOAuth2Error;
import org.tinymediamanager.thirdparty.simkl.model.SimklTokenResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Sync your watched status with Simkl.com<br>
 * New connections use the AUTH V2 device flow according to https://api.simkl.org/api-reference/oauth2-device<br>
 * The app is registered as *TV, devices &amp; command line* (no client secret, no redirect URI), since TMM also runs headless and in Docker where no
 * browser can call back.<br>
 * Existing AUTH V1 connections (retired by Simkl around April 2027) keep working side by side until the user reconnects.<br>
 * The access token of a V2 connection is only valid for 7 days, so it is refreshed shortly before it expires or on a 401.
 * 
 * @author Manuel Laggner
 */
public class Simkl implements TmmFeature {
  private static final Logger LOGGER                 = LoggerFactory.getLogger(Simkl.class);

  private static final String API_BASE               = "https://api.simkl.com/";
  private static final String APP_NAME               = "tinyMediaManager";

  /** the path prefix of all OAuth 2.0 endpoints - they always need the V2 client id and must never carry a bearer token */
  private static final String OAUTH2_PATH_PREFIX     = "/oauth2/";

  /** AUTH V2 access tokens have this prefix, AUTH V1 tokens are 64 lowercase hex characters */
  private static final String V2_ACCESS_TOKEN_PREFIX = "simkl_at_";

  /** the index of the AUTH V2 client id in the keys this feature gets from the license bundle (index 0 is the AUTH V1 client id) */
  private static final int    V2_CLIENT_ID_KEY_INDEX = 2;

  /** the scope needed to read and update the watched state of the user */
  private static final String SCOPE                  = "media:read media:write";

  private static final String GRANT_TYPE_DEVICE_CODE = "urn:ietf:params:oauth:grant-type:device_code";
  private static final String GRANT_TYPE_REFRESH     = "refresh_token";

  /** refresh the access token when it expires within this timespan */
  private static final long   REFRESH_LEAD_TIME_MS   = 24 * 60 * 60 * 1000L;

  private static final String ACCESS_TOKEN_KEY       = "simkl.access_token.secret";
  private static final String REFRESH_TOKEN_KEY      = "simkl.refresh_token.secret";
  private static final String TOKEN_EXPIRES_AT_KEY   = "simkl.token.expires_at";
  private static final String TOKEN_SCOPE_KEY        = "simkl.token.scope";
  private static final String SYNC_STATE_KEY         = "simkl.sync.state";

  private static final int    MAX_RETRIES            = 5;
  private static final long   RETRY_BASE_DELAY       = 1000;
  private static final long   RETRY_MAX_DELAY        = 60000;

  private static Simkl        instance;

  private Retrofit            retrofit;
  private SimklApi            api;
  private Gson                gson;

  /** overrides for the client ids of this feature - only to be used by unit tests */
  private String              v1ClientIdOverride;
  private String              v2ClientIdOverride;

  private Simkl() {
    // private constructor - internal usage
  }

  /**
   * Get an instance of {@link Simkl}
   * 
   * @return the instance
   */
  public static synchronized Simkl getInstance() {
    if (instance == null) {
      instance = new Simkl();
    }
    return instance;
  }

  /**
   * Is the given access token an AUTH V2 token? AUTH V1 tokens have no prefix, so they are treated as legacy.
   * 
   * @param accessToken
   *          the token to check
   * @return true when the token was issued by the AUTH V2 endpoints
   */
  public static boolean isV2Token(String accessToken) {
    return StringUtils.startsWith(accessToken, V2_ACCESS_TOKEN_PREFIX);
  }

  /**
   * Chooses the client id which belongs to the request: the OAuth 2.0 endpoints always need the V2 client id, all other calls need the client id
   * which matches the stored token - a V1 token is only accepted together with the V1 client id and vice versa.
   * 
   * @param path
   *          the path of the request
   * @param accessToken
   *          the currently stored access token (may be empty)
   * @param v1ClientId
   *          the AUTH V1 client id
   * @param v2ClientId
   *          the AUTH V2 client id
   * @return the client id to use, or an empty string when none is available
   */
  static String resolveClientId(String path, String accessToken, String v1ClientId, String v2ClientId) {
    boolean useV2 = isOAuth2Path(path) || isV2Token(accessToken);
    return useV2 ? StringUtils.trimToEmpty(v2ClientId) : StringUtils.trimToEmpty(v1ClientId);
  }

  /**
   * Is the given request path an OAuth 2.0 endpoint?
   * 
   * @param path
   *          the path of the request
   * @return true for all <code>/oauth2/*</code> paths
   */
  static boolean isOAuth2Path(String path) {
    return StringUtils.startsWith(path, OAUTH2_PATH_PREFIX);
  }

  /**
   * Returns the stored access token, or empty string.
   */
  public String getAccessToken() {
    String token = TmmStore.getInstance().get(ACCESS_TOKEN_KEY);
    return StringUtils.isNotBlank(token) ? token : "";
  }

  /**
   * Sets the access token in the encrypted TmmStore.
   */
  public void setAccessToken(String token) {
    if (StringUtils.isBlank(token)) {
      TmmStore.getInstance().remove(ACCESS_TOKEN_KEY);
    }
    else {
      TmmStore.getInstance().put(ACCESS_TOKEN_KEY, token.strip());
    }
  }

  /**
   * Returns the stored refresh token of an AUTH V2 connection, or empty string.
   */
  public String getRefreshToken() {
    String token = TmmStore.getInstance().get(REFRESH_TOKEN_KEY);
    return StringUtils.isNotBlank(token) ? token : "";
  }

  /**
   * Sets the refresh token in the encrypted TmmStore.
   */
  public void setRefreshToken(String token) {
    if (StringUtils.isBlank(token)) {
      TmmStore.getInstance().remove(REFRESH_TOKEN_KEY);
    }
    else {
      TmmStore.getInstance().put(REFRESH_TOKEN_KEY, token.strip());
    }
  }

  /**
   * Returns the timestamp (in milliseconds) when the access token expires, or 0 when this is unknown.
   */
  public long getAccessTokenExpiresAt() {
    String value = TmmStore.getInstance().getPlain(TOKEN_EXPIRES_AT_KEY);
    try {
      return StringUtils.isNotBlank(value) ? Long.parseLong(value.strip()) : 0L;
    }
    catch (NumberFormatException e) {
      return 0L;
    }
  }

  /**
   * Returns the scope which Simkl.com granted for this connection, or empty string.
   */
  public String getScope() {
    String scope = TmmStore.getInstance().getPlain(TOKEN_SCOPE_KEY);
    return StringUtils.isNotBlank(scope) ? scope : "";
  }

  /**
   * Does this installation have an AUTH V2 connection?
   */
  public boolean hasV2Tokens() {
    return isV2Token(getAccessToken());
  }

  /**
   * Does this installation still use a legacy AUTH V1 connection? Those keep working until Simkl retires AUTH V1 (around April 2027), but the user
   * should reconnect once to get an AUTH V2 connection.
   */
  public boolean hasLegacyV1Token() {
    String accessToken = getAccessToken();
    return StringUtils.isNotBlank(accessToken) && !isV2Token(accessToken);
  }

  /**
   * Stores the tokens issued by an AUTH V2 grant together with their expiry and the granted scope.
   * 
   * @param tokens
   *          the issued tokens
   */
  public void storeV2Tokens(SimklTokenResponse tokens) {
    if (tokens == null || StringUtils.isBlank(tokens.access_token)) {
      return;
    }

    setAccessToken(tokens.access_token);
    // the refresh token is not rotating - keep the stored one when the response does not carry a new one
    if (StringUtils.isNotBlank(tokens.refresh_token)) {
      setRefreshToken(tokens.refresh_token);
    }

    String scope = StringUtils.trimToEmpty(tokens.scope);
    if (StringUtils.isBlank(scope)) {
      TmmStore.getInstance().remove(TOKEN_SCOPE_KEY);
    }
    else {
      TmmStore.getInstance().put(TOKEN_SCOPE_KEY, scope);
    }

    long expiresAt = 0L;
    try {
      expiresAt = System.currentTimeMillis() + Long.parseLong(StringUtils.trimToEmpty(tokens.expires_in)) * 1000L;
    }
    catch (NumberFormatException e) {
      LOGGER.debug("Could not parse the Simkl.com token expiry - '{}'", e.getMessage());
    }
    if (expiresAt > 0L) {
      TmmStore.getInstance().put(TOKEN_EXPIRES_AT_KEY, String.valueOf(expiresAt));
    }
    else {
      TmmStore.getInstance().remove(TOKEN_EXPIRES_AT_KEY);
    }

    if (!StringUtils.containsIgnoreCase(scope, "media:write")) {
      LOGGER.warn("Simkl.com granted only the scope '{}' - writing the watched state will not work", scope);
    }
  }

  /**
   * Removes all stored tokens of this installation.
   */
  public void clearTokens() {
    TmmStore.getInstance().remove(ACCESS_TOKEN_KEY);
    TmmStore.getInstance().remove(REFRESH_TOKEN_KEY);
    TmmStore.getInstance().remove(TOKEN_EXPIRES_AT_KEY);
    TmmStore.getInstance().remove(TOKEN_SCOPE_KEY);
  }

  /**
   * Loads the persisted incremental sync state (watermarks + cached data), or a fresh state if none has been stored yet.
   */
  public SimklSyncState getSyncState() {
    String json = TmmStore.getInstance().getPlain(SYNC_STATE_KEY);
    if (StringUtils.isBlank(json)) {
      return new SimklSyncState();
    }

    try {
      return getGson().fromJson(json, SimklSyncState.class);
    }
    catch (Exception e) {
      LOGGER.debug("Could not parse stored Simkl.com sync state - '{}'", e.getMessage());
      return new SimklSyncState();
    }
  }

  /**
   * Persist the incremental sync state (watermarks + cached data).
   */
  public void saveSyncState(SimklSyncState state) {
    TmmStore.getInstance().put(SYNC_STATE_KEY, getGson().toJson(state));
  }

  /**
   * Reset the incremental sync state (watermarks + cached data), so the next sync performs a full re-sync.
   */
  public void clearSyncState() {
    TmmStore.getInstance().remove(SYNC_STATE_KEY);
    LOGGER.info("Cleared Simkl.com incremental sync state");
  }

  private Gson getGson() {
    if (gson == null) {
      gson = new Gson();
    }
    return gson;
  }

  /**
   * The AUTH V1 client id, delivered by the license bundle.
   * 
   * @return the client id, or an empty string when it is not available
   */
  String getV1ClientId() {
    if (v1ClientIdOverride != null) {
      return v1ClientIdOverride;
    }

    try {
      return StringUtils.trimToEmpty(getApiKey());
    }
    catch (Exception e) {
      LOGGER.warn("Could not read the Simkl.com AUTH V1 client id - '{}'", e.getMessage());
      return "";
    }
  }

  /**
   * The AUTH V2 client id, delivered by the license bundle at {@link #V2_CLIENT_ID_KEY_INDEX}. V2 client ids are public by design - a V2 client id
   * without a user token can only read the public catalogue data.
   * 
   * @return the client id, or an empty string when the license bundle does not provide it yet
   */
  String getV2ClientId() {
    if (v2ClientIdOverride != null) {
      return v2ClientIdOverride;
    }

    try {
      return readV2ClientId(getApiKeys());
    }
    catch (Exception e) {
      LOGGER.warn("Could not read the Simkl.com AUTH V2 client id - '{}'", e.getMessage());
    }
    return "";
  }

  /**
   * Picks the AUTH V2 client id out of the keys of this feature, which the license bundle delivers as {@code [ V1 id, ... , V2 id ]}.
   * 
   * @param keys
   *          the keys of this feature, may be null or incomplete on an outdated license bundle
   * @return the client id, or an empty string when it is not part of the bundle
   */
  static String readV2ClientId(String[] keys) {
    if (keys == null || keys.length <= V2_CLIENT_ID_KEY_INDEX) {
      return "";
    }

    // an empty slot means the bundle does not know the V2 app yet - never fall back to the V1 credentials
    return StringUtils.trimToEmpty(keys[V2_CLIENT_ID_KEY_INDEX]);
  }

  private synchronized void initAPI() {
    initAPI(API_BASE);
  }

  private synchronized void initAPI(String baseUrl) {
    if (retrofit == null) {
      OkHttpClient httpClient = TmmHttpClient.newBuilder(true).addInterceptor(chain -> {
        Request original = chain.request();
        Request.Builder request = original.newBuilder().method(original.method(), original.body());

        final String path = original.url().encodedPath();
        final String accessToken = getAccessToken();

        // add the common query parameters - the OAuth 2.0 endpoints need the V2 client id, everything else the one matching the token
        HttpUrl.Builder urlBuilder = original.url().newBuilder();
        String clientId = resolveClientId(path, accessToken, getV1ClientId(), getV2ClientId());
        if (StringUtils.isNotBlank(clientId)) {
          urlBuilder.addQueryParameter("client_id", clientId);
        }
        request.url(urlBuilder //
            .addQueryParameter("app-name", APP_NAME) //
            .addQueryParameter("app-version", ReleaseInfo.getRealVersion()) //
            .build());

        // add the auth header if a token is available - but never on the OAuth 2.0 endpoints
        if (!isOAuth2Path(path) && StringUtils.isNotBlank(accessToken)) {
          request.addHeader("Authorization", "Bearer " + accessToken);
        }
        request.header("User-Agent", APP_NAME + "/" + ReleaseInfo.getRealVersion());

        return chain.proceed(request.build());
      }).build();

      GsonBuilder gsonBuilder = new GsonBuilder().setLenient();
      gsonBuilder.registerTypeAdapter(Integer.class, (JsonDeserializer<Integer>) (json, typeOfT, context) -> {
        try {
          return json.getAsInt();
        }
        catch (NumberFormatException e) {
          return 0;
        }
      });

      retrofit = new Retrofit.Builder().baseUrl(baseUrl)
          .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
          .client(httpClient)
          .build();
      api = retrofit.create(SimklApi.class);
    }
  }

  SimklApi getApi() {
    initAPI();
    return api;
  }

  /**
   * creates the API client against a different base URL and overrides the client ids of this feature<br>
   * <b>Should only be used for unit testing et al.!</b><br>
   *
   * @param baseUrl
   *          the base URL to use
   * @param v1ClientId
   *          the AUTH V1 client id
   * @param v2ClientId
   *          the AUTH V2 client id
   */
  synchronized void initForTesting(String baseUrl, String v1ClientId, String v2ClientId) {
    this.v1ClientIdOverride = v1ClientId;
    this.v2ClientIdOverride = v2ClientId;
    retrofit = null;
    api = null;
    initAPI(baseUrl);
  }

  // ---------------------------------------------------------------------------------------------
  // AUTH V2 device flow
  // ---------------------------------------------------------------------------------------------

  /**
   * Step 1 of the AUTH V2 device flow: request a device code and the user code from Simkl.com.
   * 
   * @return the device code response including the verification URLs
   * @throws IOException
   *           when the device code could not be requested
   */
  public SimklDeviceCodeResponse getDeviceCode() throws IOException {
    initAPI();

    checkV2ClientId();
    Response<SimklDeviceCodeResponse> response = api.getDeviceCode(getV2ClientId(), SCOPE).execute();
    if (!response.isSuccessful() || response.body() == null) {
      SimklOAuth2Error error = SimklAuthResult.readError(response, getGson());
      String details = error != null ? " - " + error.error : "";
      throw new IOException("Failed to get the Simkl.com device code: " + response.code() + details);
    }

    SimklDeviceCodeResponse deviceCode = response.body();
    if (StringUtils.isAnyBlank(deviceCode.device_code, deviceCode.user_code, deviceCode.verification_uri)) {
      throw new IOException("The Simkl.com device code response is incomplete");
    }

    return deviceCode;
  }

  /**
   * Step 3 of the AUTH V2 device flow: poll the token endpoint with the device code. Pending, slow down and expired are returned as result - they are
   * a normal part of the polling and no exception.
   * 
   * @param deviceCode
   *          the device code from {@link #getDeviceCode()}
   * @return the result of the poll
   * @throws IOException
   *           on network errors
   */
  public SimklAuthResult pollForV2Token(String deviceCode) throws IOException {
    initAPI();

    checkV2ClientId();

    Response<SimklTokenResponse> response = api.exchangeDeviceCode(GRANT_TYPE_DEVICE_CODE, getV2ClientId(), deviceCode).execute();
    return SimklAuthResult.from(response, getGson());
  }

  /**
   * Gets a fresh access token for the stored AUTH V2 refresh token. The refresh token itself is not rotating, but its lifetime slides forward with
   * every use.
   * 
   * @throws SimklAuthException
   *           when Simkl.com rejected the refresh - see {@link SimklAuthException#getResult()} to distinguish a dead grant from broken app
   *           credentials
   * @throws IOException
   *           on network errors
   */
  public void refreshV2AccessToken() throws IOException {
    String refreshToken = getRefreshToken();
    if (StringUtils.isBlank(refreshToken)) {
      throw new IOException("No Simkl.com refresh token found");
    }

    initAPI();
    checkV2ClientId();

    Response<SimklTokenResponse> response = api.refreshV2AccessToken(GRANT_TYPE_REFRESH, getV2ClientId(), refreshToken).execute();
    SimklAuthResult result = SimklAuthResult.from(response, getGson());
    if (!result.isApproved()) {
      throw new SimklAuthException("Refreshing the Simkl.com access token failed: " + result.errorCode, result);
    }

    storeV2Tokens(result.tokens);
  }

  /**
   * Refreshes the access token when it expires within the next 24 hours. Errors are only logged - the request itself will run into the 401 handling
   * of {@link #executeCall(Call)} if the token really is no longer valid.
   */
  private void refreshV2AccessTokenIfNeeded() {
    if (!hasV2Tokens() || StringUtils.isBlank(getRefreshToken())) {
      return;
    }

    long expiresAt = getAccessTokenExpiresAt();
    if (expiresAt <= 0L || expiresAt - System.currentTimeMillis() > REFRESH_LEAD_TIME_MS) {
      return;
    }

    try {
      refreshV2AccessToken();
    }
    catch (Exception e) {
      LOGGER.warn("Could not refresh the Simkl.com access token - '{}'", e.getMessage());
    }
  }

  /**
   * Revokes the stored AUTH V2 grant at Simkl.com and removes the local tokens. Revoking is best effort - Simkl always answers with 200, so there is
   * no signal whether something was revoked at all.
   */
  public void disconnect() {
    if (hasV2Tokens() && StringUtils.isNotBlank(getV2ClientId())) {
      try {
        initAPI();
        String token = StringUtils.isNotBlank(getRefreshToken()) ? getRefreshToken() : getAccessToken();
        if (StringUtils.isNotBlank(token)) {
          api.revokeToken(getV2ClientId(), token).execute();
        }
      }
      catch (Exception e) {
        LOGGER.warn("Could not revoke the Simkl.com tokens - '{}'", e.getMessage());
      }
    }

    clearTokens();
    LOGGER.info("Disconnected from Simkl.com");
  }

  // ---------------------------------------------------------------------------------------------
  // API calls
  // ---------------------------------------------------------------------------------------------

  /**
   * Test the connection by calling /sync/activities.
   */
  public boolean testConnection() {
    initAPI();

    try {
      // just to test if the connection works - no need to analyze the response
      executeCall(api.getActivities());
      return true;
    }
    catch (Exception e) {
      return false;
    }
  }

  /**
   * Check if the feature is enabled and we have a valid access token. An AUTH V2 connection also needs the refresh token, since the access token
   * expires after 7 days.
   */
  public boolean isEnabled() {
    if (!isFeatureEnabled()) {
      return false;
    }

    String accessToken = getAccessToken();
    if (StringUtils.isBlank(accessToken)) {
      return false;
    }

    return isV2Token(accessToken) ? StringUtils.isNotBlank(getRefreshToken()) : true;
  }

  /**
   * Execute a Simkl API call, handling errors and 401 (expired or revoked token).
   */
  <T> T executeCall(Call<T> call) throws IOException {
    return executeCall(call, 1, RETRY_BASE_DELAY, false);
  }

  private <T> T executeCall(Call<T> call, int attempt, long waitMs, boolean retriedAfterRefresh) throws IOException {
    refreshV2AccessTokenIfNeeded();

    Response<T> response = call.execute();
    int code = response.code();

    if (code == 401) {
      if (hasV2Tokens()) {
        if (!retriedAfterRefresh && StringUtils.isNotBlank(getRefreshToken())) {
          try {
            // the access token expired - get a fresh one and retry exactly once
            refreshV2AccessToken();
            return executeCall(call.clone(), attempt, waitMs, true);
          }
          catch (SimklAuthException e) {
            if (e.getResult().isInvalidClient()) {
              // our app credentials are broken - the user cannot fix this by reconnecting, so keep the tokens
              LOGGER.error("Simkl.com rejected the app credentials - '{}'", e.getMessage());
              throw e;
            }
            // the grant is dead (expired refresh token or revoked by the user)
            clearTokens();
            pushReconnectMessage();
            throw e;
          }
        }

        // no refresh token left, or the fresh token was rejected as well
        clearTokens();
        pushReconnectMessage();
        throw new IOException("Simkl.com access token rejected (401)");
      }

      // legacy AUTH V1 token was rejected
      clearTokens();
      pushReconnectMessage();
      throw new IOException("Simkl.com access token rejected (401)");
    }

    // Retry transient errors with exponential backoff (per Simkl API rules)
    if ((code == 429 || code == 500 || code == 502 || code == 503) && attempt <= MAX_RETRIES) {
      LOGGER.warn("Simkl.com request failed with '{}' - retrying in {} ms (attempt {}/{})", code, waitMs, attempt, MAX_RETRIES);
      try {
        Thread.sleep(waitMs);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while waiting to retry the Simkl.com request", e);
      }
      return executeCall(call.clone(), attempt + 1, Math.min(waitMs * 2, RETRY_MAX_DELAY), retriedAfterRefresh);
    }

    if (!response.isSuccessful()) {
      String message = "Simkl.com request failed: " + code + " " + response.message();
      LOGGER.error(message);
      throw new IOException(message);
    }

    return response.body();
  }

  /**
   * The AUTH V2 endpoints are useless without the V2 client id - fail fast instead of sending empty credentials
   */
  private void checkV2ClientId() throws IOException {
    if (StringUtils.isBlank(getV2ClientId())) {
      throw new IOException("No Simkl.com AUTH V2 client id available - please update tinyMediaManager");
    }
  }

  private void pushReconnectMessage() {
    MessageManager.getInstance().pushMessage(new Message(Message.MessageLevel.ERROR, "simkl.sync", "simkl.error.unauthorized"));
  }

  // @formatter:off
  // ███╗   ███╗ ██████╗ ██╗   ██╗██╗███████╗███████╗
  // ████╗ ████║██╔═══██╗██║   ██║██║██╔════╝██╔════╝
  // ██╔████╔██║██║   ██║██║   ██║██║█████╗  ███████╗
  // ██║╚██╔╝██║██║   ██║╚██╗ ██╔╝██║██╔══╝  ╚════██║
  // ██║ ╚═╝ ██║╚██████╔╝ ╚████╔╝ ██║███████╗███████║
  // ╚═╝     ╚═╝ ╚═════╝   ╚═══╝  ╚═╝╚══════╝╚══════╝
  // @formatter:on

  /**
   * Syncs the Simkl movie watched state (2-way).
   */
  public void syncMovieWatched(List<Movie> moviesInTmm) throws Exception {
    if (!isEnabled()) {
      return;
    }

    new SimklMovie(this).syncWatched(moviesInTmm);
  }

  /**
   * Removes specified movies from Simkl watched history.
   */
  public void removeFromSimklMovieWatched(List<Movie> moviesInTmm) throws Exception {
    if (!isEnabled()) {
      return;
    }

    new SimklMovie(this).removeFromWatched(moviesInTmm);
  }

  /**
   * Clears all Simkl movie data.
   */
  void clearSimklMovies() throws Exception {
    if (!isEnabled()) {
      return;
    }

    new SimklMovie(this).clearMovies();
  }

  // @formatter:off
  //  ████████╗██╗   ██╗███████╗██╗  ██╗ ██████╗ ██╗    ██╗███████╗
  //  ╚══██╔══╝██║   ██║██╔════╝██║  ██║██╔═══██╗██║    ██║██╔════╝
  //     ██║   ██║   ██║███████╗███████║██║   ██║██║ █╗ ██║███████╗
  //     ██║   ╚██╗ ██╔╝╚════██║██╔══██║██║   ██║██║███╗██║╚════██║
  //     ██║    ╚████╔╝ ███████║██║  ██║╚██████╔╝╚███╔███╔╝███████║
  //     ╚═╝     ╚═══╝  ╚══════╝╚═╝  ╚═╝ ╚═════╝  ╚══╝╚══╝╚══════╝
  // @formatter:on

  /**
   * Syncs the Simkl TV show watched state (2-way).
   */
  public void syncTvShowWatched(List<TvShow> tvShowsInTmm) throws Exception {
    if (!isEnabled()) {
      return;
    }

    new SimklTvShow(this).syncWatched(tvShowsInTmm);
  }

  /**
   * Removes specified TV shows from Simkl watched history.
   */
  public void removeFromSimklTvShowWatched(List<TvShow> tvShowsInTmm) throws Exception {
    if (!isEnabled()) {
      return;
    }

    new SimklTvShow(this).removeFromWatched(tvShowsInTmm);
  }

  /**
   * Clears all Simkl TV show data.
   */
  public void clearSimklTvShows() throws Exception {
    if (!isEnabled()) {
      return;
    }

    new SimklTvShow(this).clearTvShows();
  }
}
