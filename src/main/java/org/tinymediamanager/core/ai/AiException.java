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

/**
 * The base exception class for all AI-related errors.
 *
 * @author Manuel Laggner
 */
public class AiException extends Exception {

  public AiException(String message) {
    super(message);
  }

  public AiException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Thrown when the API key is invalid or missing.
   */
  public static class Authentication extends AiException {
    public Authentication(String message) {
      super(message);
    }
  }

  /**
   * Thrown when the API rate limit is exceeded (HTTP 429).
   */
  public static class RateLimit extends AiException {
    public RateLimit(String message) {
      super(message);
    }
  }

  /**
   * Thrown when an API call times out.
   */
  public static class Timeout extends AiException {
    public Timeout(String message) {
      super(message);
    }

    public Timeout(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Thrown when the API returns a non-retryable error (4xx, 5xx).
   */
  public static class Api extends AiException {
    private final int statusCode;

    public Api(int statusCode, String message) {
      super(message);
      this.statusCode = statusCode;
    }

    public int getStatusCode() {
      return statusCode;
    }
  }

  /**
   * Thrown when there is a network/connection error.
   */
  public static class Network extends AiException {
    public Network(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Thrown when the AI provider is not properly configured.
   */
  public static class Configuration extends AiException {
    public Configuration(String message) {
      super(message);
    }
  }
}
