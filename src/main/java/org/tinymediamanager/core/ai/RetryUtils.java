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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link RetryUtils} provides utility methods for retrying AI API calls with exponential backoff.
 *
 * @author Manuel Laggner
 */
public final class RetryUtils {

  private static final Logger LOGGER                = LoggerFactory.getLogger(RetryUtils.class);

  private static final long   DEFAULT_BASE_DELAY_MS = 3000L;
  private static final long   MAX_DELAY_MS          = 60000L;

  private RetryUtils() {
  }

  /**
   * Calculates an exponential backoff delay: baseDelay * 2^(attempt-1) capped at MAX_DELAY_MS.
   *
   * @param attempt
   *          the current attempt (1-indexed)
   * @param baseDelayMs
   *          the base delay in milliseconds
   * @return the delay in milliseconds
   */
  public static long exponentialBackoffDelay(int attempt, long baseDelayMs) {
    long delay = baseDelayMs * (1L << (attempt - 1));
    return Math.min(delay, MAX_DELAY_MS);
  }

  /**
   * Calculates an exponential backoff delay using the default base delay.
   *
   * @param attempt
   *          the current attempt (1-indexed)
   * @return the delay in milliseconds
   */
  public static long exponentialBackoffDelay(int attempt) {
    return exponentialBackoffDelay(attempt, DEFAULT_BASE_DELAY_MS);
  }

  /**
   * Sleeps for the specified duration, handling InterruptedException.
   *
   * @param millis
   *          the duration to sleep in milliseconds
   */
  public static void sleepWithInterruptHandling(long millis) {
    try {
      Thread.sleep(millis);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn("Retry sleep interrupted");
    }
  }

  /**
   * Waits before a retry with exponential backoff and logging.
   *
   * @param attempt
   *          the current attempt (1-indexed)
   * @param context
   *          a description of what is being retried
   */
  public static void waitBeforeRetry(int attempt, String context) {
    long delay = exponentialBackoffDelay(attempt);
    LOGGER.info("Retrying {} in {}ms (attempt {})", context, delay, attempt);
    sleepWithInterruptHandling(delay);
  }

  /**
   * Waits before a retry with a custom delay and logging.
   *
   * @param delayMs
   *          the custom delay in milliseconds
   * @param context
   *          a description of what is being retried
   */
  public static void waitBeforeRetry(long delayMs, String context) {
    LOGGER.info("Retrying {} in {}ms", context, delayMs);
    sleepWithInterruptHandling(delayMs);
  }

  /**
   * Determines whether a retry should be attempted.
   *
   * @param currentAttempt
   *          the current attempt (1-indexed)
   * @param maxRetries
   *          the maximum number of retries allowed
   * @return true if the attempt should be retried
   */
  public static boolean shouldRetry(int currentAttempt, int maxRetries) {
    return currentAttempt < maxRetries;
  }

  /**
   * Determines whether an exception is retryable.
   *
   * @param e
   *          the exception
   * @return true if the error is retryable
   */
  public static boolean isRetryable(AiException e) {
    return e instanceof AiException.RateLimit || e instanceof AiException.Timeout || e instanceof AiException.Network
        || e instanceof AiException.Api api && (api.getStatusCode() >= 500 || api.getStatusCode() == 429);
  }
}
