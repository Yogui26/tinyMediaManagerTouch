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

import org.tinymediamanager.thirdparty.simkl.model.SimklOAuth2Error;
import org.tinymediamanager.thirdparty.simkl.model.SimklTokenResponse;

import com.google.gson.Gson;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * The result of an AUTH V2 <code>/oauth2/token</code> call - either the issued tokens or the OAuth2 error the endpoint responded with. This wrapper
 * keeps the polling loop of the device flow free of HTTP details, since the pending/slow down/expired states arrive as normal error responses.
 *
 * @author Manuel Laggner
 */
public class SimklAuthResult {
  /** The user has not approved the request (yet) */
  public static final String      ERROR_AUTHORIZATION_PENDING = "authorization_pending";
  /** The polling was too fast - add 5 seconds to the interval and wait */
  public static final String      ERROR_SLOW_DOWN             = "slow_down";
  /** The user declined the request - Simkl does not send this for the device flow, but RFC 8628 knows it */
  public static final String      ERROR_ACCESS_DENIED         = "access_denied";
  /** The device code is no longer valid */
  public static final String      ERROR_EXPIRED_TOKEN         = "expired_token";
  /** The client id is unknown or not enabled for OAuth2 - a configuration problem, retrying cannot help */
  public static final String      ERROR_INVALID_CLIENT        = "invalid_client";
  /** The grant is invalid, e.g. a refresh token which has been revoked or has expired */
  public static final String      ERROR_INVALID_GRANT         = "invalid_grant";

  /** The issued tokens, or {@code null} when the request did not succeed */
  public final SimklTokenResponse tokens;

  /** The machine-readable OAuth2 error code, or {@code null} on success */
  public final String             errorCode;

  /** The human-readable error description */
  public final String             errorDescription;

  /** The HTTP status code of the response */
  public final int                httpStatus;

  /**
   * Creates the result of a <code>/oauth2/token</code> call.
   *
   * @param response
   *          the raw response
   * @param gson
   *          the Gson instance used to parse the error envelope
   * @return the result
   */
  public static SimklAuthResult from(Response<SimklTokenResponse> response, Gson gson) {
    if (response.isSuccessful() && response.body() != null) {
      return new SimklAuthResult(response.code(), response.body(), null, null);
    }

    SimklOAuth2Error error = readError(response, gson);
    String errorCode = error != null ? error.error : null;
    String errorDescription = error != null ? error.error_description : response.message();
    return new SimklAuthResult(response.code(), null, errorCode, errorDescription);
  }

  /**
   * Reads and consumes the OAuth2 error envelope of a failed response.
   *
   * @param response
   *          the failed response
   * @param gson
   *          the Gson instance used to parse the error envelope
   * @return the error, or {@code null} when there is no body or it cannot be parsed
   */
  public static SimklOAuth2Error readError(Response<?> response, Gson gson) {
    ResponseBody errorBody = response.errorBody();
    if (errorBody == null) {
      return null;
    }

    try (errorBody) {
      return gson.fromJson(errorBody.string(), SimklOAuth2Error.class);
    }
    catch (Exception e) {
      // a non-parseable error body is handled like an unknown error
      return null;
    }
  }

  /**
   * Creates a successful result.
   *
   * @param tokens
   *          the issued tokens
   * @return the result
   */
  public static SimklAuthResult approved(SimklTokenResponse tokens) {
    return new SimklAuthResult(200, tokens, null, null);
  }

  /**
   * Creates a failed result.
   *
   * @param httpStatus
   *          the HTTP status code
   * @param errorCode
   *          the machine-readable OAuth2 error code
   * @param errorDescription
   *          the human-readable error description
   * @return the result
   */
  public static SimklAuthResult failed(int httpStatus, String errorCode, String errorDescription) {
    return new SimklAuthResult(httpStatus, null, errorCode, errorDescription);
  }

  private SimklAuthResult(int httpStatus, SimklTokenResponse tokens, String errorCode, String errorDescription) {
    this.httpStatus = httpStatus;
    this.tokens = tokens;
    this.errorCode = errorCode;
    this.errorDescription = errorDescription;
  }

  /**
   * @return true when the tokens have been issued
   */
  public boolean isApproved() {
    return tokens != null;
  }

  /**
   * @return true when the polling is worth another attempt later - the user has either not decided yet, or we have to slow down. Any other failure is
   *         final for this device code.
   */
  public boolean isRetryable() {
    return isPending() || isSlowDown();
  }

  /**
   * @return true when the user declined the request - Simkl does not send this for the device flow, but an approving app might
   */
  public boolean isAccessDenied() {
    return ERROR_ACCESS_DENIED.equals(errorCode);
  }

  /**
   * @return true when the user has not approved the request yet - keep polling
   */
  public boolean isPending() {
    return ERROR_AUTHORIZATION_PENDING.equals(errorCode);
  }

  /**
   * @return true when the polling was too fast - add 5 seconds to the interval and wait
   */
  public boolean isSlowDown() {
    return ERROR_SLOW_DOWN.equals(errorCode);
  }

  /**
   * @return true when the device code has expired - a new device code is needed
   */
  public boolean isExpired() {
    return ERROR_EXPIRED_TOKEN.equals(errorCode);
  }

  /**
   * @return true when the app credentials are rejected - this is not fixable by retrying
   */
  public boolean isInvalidClient() {
    return ERROR_INVALID_CLIENT.equals(errorCode);
  }

  /**
   * @return true when the stored refresh token is no longer usable and the user has to reconnect
   */
  public boolean isInvalidGrant() {
    return ERROR_INVALID_GRANT.equals(errorCode);
  }
}
