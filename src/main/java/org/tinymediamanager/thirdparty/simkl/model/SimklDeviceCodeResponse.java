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
 * The response of the <code>/oauth2/device</code> endpoint (AUTH V2 device flow according to RFC 8628).
 *
 * @author Manuel Laggner
 */
public class SimklDeviceCodeResponse {
  /** The device code to poll the token endpoint with - this is a credential and must never be shown to the user */
  public String device_code;

  /** The code the user has to enter on Simkl.com, if the pre-filled URL could not be used */
  public String user_code;

  /** The plain approval page on Simkl.com */
  public String verification_uri;

  /** The approval page on Simkl.com with the user code already pre-filled */
  public String verification_uri_complete;

  /** The seconds the device code is valid */
  public String expires_in;

  /** The seconds to wait between two polls of the token endpoint */
  public String interval;
}
