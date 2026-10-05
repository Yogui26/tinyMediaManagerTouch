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
 * The RFC 6749 section 5.2 error envelope of the Simkl AUTH V2 <code>/oauth2/*</code> endpoints. Branch on {@link #error} only,
 * {@link #error_description} is human-readable and may change.
 *
 * @author Manuel Laggner
 */
public class SimklOAuth2Error {
  /** The machine-readable error code, e.g. <code>authorization_pending</code>, <code>slow_down</code> or <code>expired_token</code> */
  public String error;

  /** The human-readable description - for logging and display, never for branching */
  public String error_description;
}
