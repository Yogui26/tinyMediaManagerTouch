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
package org.tinymediamanager.core.ai;

import java.time.Duration;

/**
 * The interface {@link AiClient} defines the contract for calling an OpenAI-compatible chat completions API.
 *
 * @author Manuel Laggner
 */
public interface AiClient {

  /**
   * Sends a chat completion request and returns the response.
   *
   * @param request
   *          the request to send
   * @return the response
   * @throws AiException
   *           if the API call fails
   */
  AiResponse chat(AiRequest request) throws AiException;

  /**
   * Sends a chat completion request with a custom timeout and returns the response.
   *
   * @param request
   *          the request to send
   * @param timeout
   *          the timeout duration
   * @return the response
   * @throws AiException
   *           if the API call fails
   */
  AiResponse chat(AiRequest request, Duration timeout) throws AiException;

  /**
   * Tests the connection to the AI provider by sending a minimal request.
   *
   * @return true if the connection is successful
   * @throws AiException
   *           if the connection fails
   */
  boolean testConnection() throws AiException;

  /**
   * Returns the provider name for this client.
   *
   * @return the provider name
   */
  String getProviderName();
}
