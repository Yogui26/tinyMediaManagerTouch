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
package org.tinymediamanager.thirdparty.simkl.model;

/**
 * The token response of the <code>/oauth2/token</code> endpoint. All AUTH V2 grants (device code, refresh token, authorization code) share this
 * shape.
 *
 * @author Manuel Laggner
 */
public class SimklTokenResponse {
  /** The bearer access token (prefixed <code>simkl_at_</code>), valid for 7 days */
  public String access_token;

  /** Always <code>Bearer</code> - compare it case-insensitively */
  public String token_type;

  /** The refresh token (prefixed <code>simkl_rt_</code>), valid for 180 days and renewed on every use */
  public String refresh_token;

  /** The scope which has actually been granted - either <code>media:read</code> or <code>media:read media:write</code> */
  public String scope;

  /** The seconds until the access token expires */
  public String expires_in;
}
