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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.TmmStore;
import org.tinymediamanager.thirdparty.simkl.model.SimklActivities;
import org.tinymediamanager.thirdparty.simkl.model.SimklDeviceCodeResponse;
import org.tinymediamanager.thirdparty.simkl.model.SimklTokenResponse;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Tests the AUTH V2 device flow plumbing of {@link Simkl}: token storage, refresh handling and the OAuth2 error vocabulary.
 *
 * @author Manuel Laggner
 */
public class SimklV2AuthTest extends BasicTest {

  private static final String V2_ACCESS_TOKEN  = "simkl_at_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
  private static final String V2_REFRESH_TOKEN = "simkl_rt_BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";
  private static final String V1_ACCESS_TOKEN  = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

  private final MockWebServer mockWebServer    = new MockWebServer();
  private Simkl               simkl;

  @Before
  public void setupSimkl() throws Exception {
    mockWebServer.start();
    simkl = Simkl.getInstance();
    simkl.clearTokens();
    simkl.initForTesting(mockWebServer.url("/").toString(), "v1-client-id", "v2-client-id");
  }

  @After
  public void shutDownSimkl() throws Exception {
    if (simkl != null) {
      simkl.clearTokens();
    }
    mockWebServer.shutdown();
  }

  @Test
  public void testTokenVersionDetection() {
    assertThat(Simkl.isV2Token(V2_ACCESS_TOKEN)).isTrue();
    assertThat(Simkl.isV2Token(V1_ACCESS_TOKEN)).isFalse();
    assertThat(Simkl.isV2Token("")).isFalse();
    assertThat(Simkl.isV2Token(null)).isFalse();

    simkl.setAccessToken(V1_ACCESS_TOKEN);
    assertThat(simkl.hasLegacyV1Token()).isTrue();
    assertThat(simkl.hasV2Tokens()).isFalse();

    simkl.setAccessToken(V2_ACCESS_TOKEN);
    assertThat(simkl.hasLegacyV1Token()).isFalse();
    assertThat(simkl.hasV2Tokens()).isTrue();
  }

  @Test
  public void testStoreAndClearTokens() {
    simkl.storeV2Tokens(createTokenResponse(V2_ACCESS_TOKEN, V2_REFRESH_TOKEN, "604800", "media:read media:write"));

    assertThat(TmmStore.getInstance().get("simkl.access_token.secret")).isEqualTo(V2_ACCESS_TOKEN);
    assertThat(TmmStore.getInstance().get("simkl.refresh_token.secret")).isEqualTo(V2_REFRESH_TOKEN);
    assertThat(TmmStore.getInstance().getPlain("simkl.token.scope")).isEqualTo("media:read media:write");
    assertThat(simkl.getAccessTokenExpiresAt()).isGreaterThan(System.currentTimeMillis());

    simkl.clearTokens();
    assertThat(simkl.getAccessToken()).isEmpty();
    assertThat(simkl.getRefreshToken()).isEmpty();
    assertThat(simkl.getScope()).isEmpty();
    assertThat(simkl.getAccessTokenExpiresAt()).isZero();
  }

  @Test
  public void testStoreTokensKeepsExpiryUnknownWhenUnparseable() {
    simkl.storeV2Tokens(createTokenResponse(V2_ACCESS_TOKEN, V2_REFRESH_TOKEN, "n.a.", "media:read"));

    assertThat(simkl.getAccessTokenExpiresAt()).isZero();
    assertThat(simkl.getScope()).isEqualTo("media:read");
  }

  @Test
  public void testClientIdIsChosenPerAuthVersion() {
    // the OAuth2 endpoints always need the V2 client id, even while a legacy V1 token is stored
    assertThat(Simkl.resolveClientId("/oauth2/token", V1_ACCESS_TOKEN, "v1", "v2")).isEqualTo("v2");
    assertThat(Simkl.resolveClientId("/oauth2/device", "", "v1", "v2")).isEqualTo("v2");

    // data endpoints need the client id which matches the stored token
    assertThat(Simkl.resolveClientId("/sync/activities", V2_ACCESS_TOKEN, "v1", "v2")).isEqualTo("v2");
    assertThat(Simkl.resolveClientId("/sync/activities", V1_ACCESS_TOKEN, "v1", "v2")).isEqualTo("v1");
    assertThat(Simkl.resolveClientId("/sync/activities", "", "v1", "v2")).isEqualTo("v1");

    assertThat(Simkl.resolveClientId("/oauth2/token", "", null, null)).isEmpty();
  }

  @Test
  public void testPollForTokenPendingAndApproved() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":\"authorization_pending\"}"));
    SimklAuthResult pending = simkl.pollForV2Token("devicecode");
    assertThat(pending.isPending()).isTrue();
    assertThat(pending.isApproved()).isFalse();

    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(tokenJson(V2_ACCESS_TOKEN, V2_REFRESH_TOKEN, 604800)));
    SimklAuthResult approved = simkl.pollForV2Token("devicecode");
    assertThat(approved.isApproved()).isTrue();
    assertThat(approved.tokens.access_token).isEqualTo(V2_ACCESS_TOKEN);

    // the polling must never carry a bearer token, and it needs the device grant in the form body
    RecordedRequest first = mockWebServer.takeRequest();
    assertThat(first.getPath()).startsWith("/oauth2/token");
    assertThat(first.getHeader("Authorization")).isNull();
    assertThat(first.getBody().readUtf8()).contains("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code")
        .contains("device_code=devicecode");
    mockWebServer.takeRequest();
  }

  @Test
  public void testPollForTokenSlowDownAndExpired() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":\"slow_down\"}"));
    assertThat(simkl.pollForV2Token("devicecode").isSlowDown()).isTrue();

    mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":\"expired_token\"}"));
    assertThat(simkl.pollForV2Token("devicecode").isExpired()).isTrue();

    mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"invalid_client\"}"));
    assertThat(simkl.pollForV2Token("devicecode").isInvalidClient()).isTrue();
  }

  @Test
  public void testRefreshStoresNewExpiry() throws Exception {
    simkl.storeV2Tokens(createTokenResponse(V2_ACCESS_TOKEN, V2_REFRESH_TOKEN, "60", "media:read media:write"));
    long firstExpiry = simkl.getAccessTokenExpiresAt();

    mockWebServer
        .enqueue(new MockResponse().setResponseCode(200).setBody(tokenJson("simkl_at_CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", V2_REFRESH_TOKEN, 604800)));
    simkl.refreshV2AccessToken();

    assertThat(simkl.getAccessToken()).isEqualTo("simkl_at_CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC");
    // the refresh token is not rotating - the same string comes back
    assertThat(simkl.getRefreshToken()).isEqualTo(V2_REFRESH_TOKEN);
    assertThat(simkl.getAccessTokenExpiresAt()).isGreaterThan(firstExpiry);

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getBody().readUtf8()).contains("grant_type=refresh_token").contains("refresh_token=" + V2_REFRESH_TOKEN);
  }

  @Test
  public void testRefreshWithDeadGrant() {
    simkl.storeV2Tokens(createTokenResponse(V2_ACCESS_TOKEN, V2_REFRESH_TOKEN, "60", "media:read media:write"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":\"invalid_grant\"}"));

    try {
      simkl.refreshV2AccessToken();
      failBecauseExceptionWasNotThrown(SimklAuthException.class);
    }
    catch (Exception e) {
      assertThat(e).isInstanceOf(SimklAuthException.class);
      assertThat(((SimklAuthException) e).getResult().isInvalidGrant()).isTrue();
      assertThat(((SimklAuthException) e).getResult().isInvalidClient()).isFalse();
    }
  }

  @Test
  public void testRefreshWithoutToken() {
    try {
      simkl.refreshV2AccessToken();
      failBecauseExceptionWasNotThrown(IOException.class);
    }
    catch (Exception e) {
      assertThat(e).isInstanceOf(IOException.class);
    }
  }

  @Test
  public void testExpiringTokenIsRefreshedBeforeTheCall() throws Exception {
    // the access token expires within 24 hours -> refresh first, then do the real call
    simkl.storeV2Tokens(createTokenResponse(V2_ACCESS_TOKEN, V2_REFRESH_TOKEN, "60", "media:read media:write"));

    mockWebServer
        .enqueue(new MockResponse().setResponseCode(200).setBody(tokenJson("simkl_at_EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE", V2_REFRESH_TOKEN, 604800)));
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"all\":\"1\"}"));

    assertThat(simkl.executeCall(simkl.getApi().getActivities())).isNotNull();
    assertThat(simkl.getAccessToken()).isEqualTo("simkl_at_EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");

    assertThat(mockWebServer.takeRequest().getPath()).startsWith("/oauth2/token");
    RecordedRequest dataCall = mockWebServer.takeRequest();
    assertThat(dataCall.getPath()).startsWith("/sync/activities");
    assertThat(dataCall.getHeader("Authorization")).isEqualTo("Bearer simkl_at_EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
  }

  @Test
  public void testRejectedCallRefreshesAndRetriesOnce() throws Exception {
    // a fresh token, so no proactive refresh interferes with the reactive 401 handling
    simkl.storeV2Tokens(createTokenResponse(V2_ACCESS_TOKEN, V2_REFRESH_TOKEN, "604800", "media:read media:write"));

    // 1: the data call runs into a 401, 2: the refresh gives a new access token, 3: the retried call succeeds
    mockWebServer.enqueue(new MockResponse().setResponseCode(401));
    mockWebServer
        .enqueue(new MockResponse().setResponseCode(200).setBody(tokenJson("simkl_at_DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD", V2_REFRESH_TOKEN, 604800)));
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"all\":\"1\"}"));

    SimklActivities activities = simkl.executeCall(simkl.getApi().getActivities());
    assertThat(activities).isNotNull();
    assertThat(simkl.getAccessToken()).isEqualTo("simkl_at_DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");

    assertThat(mockWebServer.takeRequest().getPath()).startsWith("/sync/activities");
    assertThat(mockWebServer.takeRequest().getPath()).startsWith("/oauth2/token");
    RecordedRequest retried = mockWebServer.takeRequest();
    assertThat(retried.getPath()).startsWith("/sync/activities");
    assertThat(retried.getHeader("Authorization")).isEqualTo("Bearer simkl_at_DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
  }

  @Test
  public void testRejectedCallClearsDeadGrant() throws Exception {
    simkl.storeV2Tokens(createTokenResponse(V2_ACCESS_TOKEN, V2_REFRESH_TOKEN, "604800", "media:read media:write"));

    // 401, then the refresh itself is rejected because the grant has been revoked
    mockWebServer.enqueue(new MockResponse().setResponseCode(401));
    mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":\"invalid_grant\"}"));

    try {
      simkl.executeCall(simkl.getApi().getActivities());
      failBecauseExceptionWasNotThrown(IOException.class);
    }
    catch (Exception e) {
      assertThat(simkl.getAccessToken()).isEmpty();
      assertThat(simkl.getRefreshToken()).isEmpty();
    }
  }

  @Test
  public void testRejectedLegacyTokenIsCleared() throws Exception {
    simkl.setAccessToken(V1_ACCESS_TOKEN);
    mockWebServer.enqueue(new MockResponse().setResponseCode(401));

    try {
      simkl.executeCall(simkl.getApi().getActivities());
      failBecauseExceptionWasNotThrown(IOException.class);
    }
    catch (Exception e) {
      assertThat(simkl.getAccessToken()).isEmpty();
    }
  }

  @Test
  public void testEnabledRequiresRefreshTokenForV2() {
    simkl.setAccessToken(V2_ACCESS_TOKEN);
    assertThat(simkl.isEnabled()).isFalse();

    simkl.setRefreshToken(V2_REFRESH_TOKEN);
    // the feature itself is still disabled without a license, but the token check passes
    assertThat(Simkl.isV2Token(simkl.getAccessToken())).isTrue();
    assertThat(simkl.getRefreshToken()).isEqualTo(V2_REFRESH_TOKEN);
  }

  @Test
  public void testDisconnectRevokesAndClears() throws Exception {
    simkl.storeV2Tokens(createTokenResponse(V2_ACCESS_TOKEN, V2_REFRESH_TOKEN, "604800", "media:read media:write"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(200));

    simkl.disconnect();

    assertThat(simkl.getAccessToken()).isEmpty();
    assertThat(simkl.getRefreshToken()).isEmpty();

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).startsWith("/oauth2/revoke");
    assertThat(request.getBody().readUtf8()).contains("token=" + V2_REFRESH_TOKEN);
  }

  @Test
  public void testGetDeviceCodeAsksForReadWriteScope() throws Exception {
    String json = "{\"device_code\":\"dc\",\"user_code\":\"ABCD-1234\"," + "\"verification_uri\":\"https://simkl.com/pin\","
        + "\"verification_uri_complete\":\"https://simkl.com/pin?user_code=ABCD-1234\"," + "\"expires_in\":\"900\",\"interval\":\"5\"}";
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(json));

    SimklDeviceCodeResponse deviceCode = simkl.getDeviceCode();
    assertThat(deviceCode.device_code).isEqualTo("dc");
    assertThat(deviceCode.verification_uri_complete).contains("user_code=ABCD-1234");
    assertThat(deviceCode.interval).isEqualTo("5");

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).startsWith("/oauth2/device").contains("client_id=v2-client-id");
    assertThat(request.getHeader("Authorization")).isNull();
    assertThat(request.getBody().readUtf8()).contains("scope=media%3Aread%20media%3Awrite");
  }

  @Test
  public void testIncompleteDeviceCodeResponseFails() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"user_code\":\"ABCD-1234\"}"));

    try {
      simkl.getDeviceCode();
      failBecauseExceptionWasNotThrown(IOException.class);
    }
    catch (IOException e) {
      assertThat(e.getMessage()).contains("incomplete");
    }
  }

  @Test
  public void testUnparseableErrorBodyIsNotFatal() throws Exception {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("<html>oops</html>"));

    SimklAuthResult result = simkl.pollForV2Token("devicecode");
    assertThat(result.isApproved()).isFalse();
    assertThat(result.errorCode).isNull();
    assertThat(result.isPending()).isFalse();
    // a server-side problem is no decision, so the caller retries it a few times instead of waiting for the expiry
    assertThat(result.httpStatus).isEqualTo(500);
    assertThat(result.isRetryable()).isFalse();
  }

  @Test
  public void testReadV2ClientIdFromLicenseKeys() {
    assertThat(Simkl.readV2ClientId(null)).isEmpty();
    assertThat(Simkl.readV2ClientId(new String[0])).isEmpty();

    // an outdated license bundle does not know the V2 app - the V1 credentials must never be used as a fallback
    assertThat(Simkl.readV2ClientId(new String[] { "v1" })).isEmpty();
    assertThat(Simkl.readV2ClientId(new String[] { "v1", "other" })).isEmpty();

    assertThat(Simkl.readV2ClientId(new String[] { "v1", "other", " v2 " })).isEqualTo("v2");
    assertThat(Simkl.readV2ClientId(new String[] { "v1", "other", "" })).isEmpty();
  }

  @Test
  public void testOnlyPendingAndSlowDownAreRetryable() {
    assertThat(SimklAuthResult.failed(400, SimklAuthResult.ERROR_AUTHORIZATION_PENDING, null).isRetryable()).isTrue();
    assertThat(SimklAuthResult.failed(400, SimklAuthResult.ERROR_SLOW_DOWN, null).isRetryable()).isTrue();
    assertThat(SimklAuthResult.failed(400, SimklAuthResult.ERROR_EXPIRED_TOKEN, null).isRetryable()).isFalse();
    assertThat(SimklAuthResult.failed(401, SimklAuthResult.ERROR_INVALID_CLIENT, null).isRetryable()).isFalse();

    SimklAuthResult denied = SimklAuthResult.failed(400, SimklAuthResult.ERROR_ACCESS_DENIED, "the user declined");
    assertThat(denied.isAccessDenied()).isTrue();
    assertThat(denied.isRetryable()).isFalse();

    assertThat(SimklAuthResult.approved(createTokenResponse(V2_ACCESS_TOKEN, V2_REFRESH_TOKEN, "604800", "media:read media:write")).isApproved())
        .isTrue();
  }

  @Test
  public void testDataCallsUseTheClientIDMatchingTheToken() throws Exception {
    // a legacy AUTH V1 token must always be sent together with the AUTH V1 client id
    simkl.setAccessToken(V1_ACCESS_TOKEN);
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"all\":\"1\"}"));
    simkl.executeCall(simkl.getApi().getActivities());
    RecordedRequest legacy = mockWebServer.takeRequest();
    assertThat(legacy.getPath()).contains("client_id=v1-client-id");
    assertThat(legacy.getHeader("Authorization")).isEqualTo("Bearer " + V1_ACCESS_TOKEN);

    // an AUTH V2 token belongs to the AUTH V2 client id
    simkl.clearTokens();
    simkl.storeV2Tokens(createTokenResponse(V2_ACCESS_TOKEN, V2_REFRESH_TOKEN, "604800", "media:read media:write"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"all\":\"1\"}"));
    simkl.executeCall(simkl.getApi().getActivities());
    RecordedRequest v2 = mockWebServer.takeRequest();
    assertThat(v2.getPath()).contains("client_id=v2-client-id");
    assertThat(v2.getHeader("Authorization")).isEqualTo("Bearer " + V2_ACCESS_TOKEN);
    assertThat(v2.getPath()).contains("app-name=tinyMediaManager");
  }

  private SimklTokenResponse createTokenResponse(String accessToken, String refreshToken, String expiresIn, String scope) {
    SimklTokenResponse tokens = new SimklTokenResponse();
    tokens.access_token = accessToken;
    tokens.refresh_token = refreshToken;
    tokens.token_type = "Bearer";
    tokens.expires_in = expiresIn;
    tokens.scope = scope;
    return tokens;
  }

  private String tokenJson(String accessToken, String refreshToken, long expiresIn) {
    return "{\"access_token\":\"" + accessToken + "\",\"token_type\":\"Bearer\",\"expires_in\":" + expiresIn + ",\"refresh_token\":\"" + refreshToken
        + "\",\"scope\":\"media:read media:write\"}";
  }
}
