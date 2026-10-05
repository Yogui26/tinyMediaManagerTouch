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

/**
 * Thrown when an AUTH V2 token request (device code exchange or refresh) was rejected by Simkl.com. The {@link SimklAuthResult} tells the caller
 * whether the credentials of this installation are broken or whether the user has to reconnect.
 *
 * @author Manuel Laggner
 */
public class SimklAuthException extends IOException {
  private static final long     serialVersionUID = 1L;

  private final SimklAuthResult result;

  /**
   * Creates the exception.
   *
   * @param message
   *          the detail message
   * @param result
   *          the rejected token request
   */
  public SimklAuthException(String message, SimklAuthResult result) {
    super(message);
    this.result = result;
  }

  /**
   * @return the rejected token request
   */
  public SimklAuthResult getResult() {
    return result;
  }
}
