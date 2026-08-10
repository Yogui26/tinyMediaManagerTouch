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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Settings;

/**
 * The class {@link AiRateLimiter} provides rate limiting for AI API calls with per-provider tracking and exponential backoff on 429 responses.
 *
 * @author Manuel Laggner
 */
public class AiRateLimiter {

  private static final Logger                                     LOGGER               = LoggerFactory.getLogger(AiRateLimiter.class);

  private static final AiRateLimiter                              INSTANCE             = new AiRateLimiter();

  // Call history per provider (timestamps)
  private final Map<String, ConcurrentLinkedQueue<LocalDateTime>> callHistory          = new ConcurrentHashMap<>();

  // Service stats per provider
  private final Map<String, ServiceStats>                         serviceStats         = new ConcurrentHashMap<>();

  // Dynamic wait (exponential backoff from 429s) per provider
  private final Map<String, Long>                                 dynamicWaitTimeMs    = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger>                        consecutive429Errors = new ConcurrentHashMap<>();

  private AiRateLimiter() {
  }

  /**
   * Gets the singleton instance.
   *
   * @return the rate limiter instance
   */
  public static AiRateLimiter getInstance() {
    return INSTANCE;
  }

  /**
   * Requests permission to make an API call for the given provider. Blocks until permitted.
   *
   * @param providerName
   *          the provider name
   * @param timeoutMs
   *          maximum time to wait in milliseconds
   * @return true if permission was granted, false if timeout was reached
   */
  public boolean waitForPermission(String providerName, long timeoutMs) {
    Settings settings = Settings.getInstance();
    if (!settings.isAiRateLimitEnabled()) {
      return true;
    }

    long deadline = System.currentTimeMillis() + timeoutMs;
    ConcurrentLinkedQueue<LocalDateTime> history = callHistory.computeIfAbsent(providerName, k -> new ConcurrentLinkedQueue<>());

    while (System.currentTimeMillis() < deadline) {
      synchronized (history) {
        if (canProceed(history, providerName, settings)) {
          history.add(LocalDateTime.now());
          return true;
        }
      }

      try {
        Thread.sleep(Math.min(200, deadline - System.currentTimeMillis()));
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }

    LOGGER.warn("Rate limit wait timeout for provider '{}' after {}ms", providerName, timeoutMs);
    return false;
  }

  private boolean canProceed(ConcurrentLinkedQueue<LocalDateTime> history, String providerName, Settings settings) {
    LocalDateTime now = LocalDateTime.now();

    // Clean old entries
    LocalDateTime oneMinuteAgo = now.minusMinutes(1);
    LocalDateTime oneHourAgo = now.minusHours(1);
    history.removeIf(t -> t.isBefore(oneHourAgo));

    // Check max calls per minute
    if (settings.getAiMaxCallsPerMinute() > 0) {
      long callsLastMinute = history.stream().filter(t -> t.isAfter(oneMinuteAgo)).count();
      if (callsLastMinute >= settings.getAiMaxCallsPerMinute()) {
        return false;
      }
    }

    // Check max calls per hour
    if (settings.getAiMaxCallsPerHour() > 0) {
      if (history.size() >= settings.getAiMaxCallsPerHour()) {
        return false;
      }
    }

    // Check min interval
    if (settings.getAiMinIntervalSeconds() > 0) {
      LocalDateTime lastCall = history.stream().reduce((first, second) -> second).orElse(null);
      if (lastCall != null && lastCall.plusSeconds(settings.getAiMinIntervalSeconds()).isAfter(now)) {
        return false;
      }
    }

    // Check dynamic wait from 429 responses
    Long dynamicWait = dynamicWaitTimeMs.get(providerName);
    if (dynamicWait != null && dynamicWait > 0) {
      LOGGER.debug("Dynamic wait of {}ms for '{}'", dynamicWait, providerName);
      try {
        Thread.sleep(dynamicWait);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    return true;
  }

  /**
   * Records a successful API call for the given provider.
   *
   * @param providerName
   *          the provider name
   */
  public void recordSuccessfulCall(String providerName) {
    consecutive429Errors.computeIfAbsent(providerName, k -> new AtomicInteger()).set(0);
    dynamicWaitTimeMs.put(providerName, 0L);

    ServiceStats stats = serviceStats.computeIfAbsent(providerName, k -> new ServiceStats());
    stats.successfulCalls++;
    stats.totalCalls++;
  }

  /**
   * Records a failed API call for the given provider.
   *
   * @param providerName
   *          the provider name
   */
  public void recordFailedCall(String providerName) {
    ServiceStats stats = serviceStats.computeIfAbsent(providerName, k -> new ServiceStats());
    stats.failedCalls++;
    stats.totalCalls++;
  }

  /**
   * Records a 429 (rate limit) error and increases the dynamic wait time exponentially.
   *
   * @param providerName
   *          the provider name
   */
  public void record429Error(String providerName) {
    AtomicInteger counter = consecutive429Errors.computeIfAbsent(providerName, k -> new AtomicInteger());
    int count = counter.incrementAndGet();

    long waitTime = (long) Math.min(1000L * (1L << (count - 1)), 60000L);
    dynamicWaitTimeMs.put(providerName, waitTime);

    ServiceStats stats = serviceStats.computeIfAbsent(providerName, k -> new ServiceStats());
    stats.failedCalls++;
    stats.totalCalls++;
  }

  /**
   * Gets statistics for all providers as a human-readable string.
   *
   * @return statistics string
   */
  public String getStatistics() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, ServiceStats> entry : serviceStats.entrySet()) {
      sb.append(entry.getKey())
          .append(": ")
          .append("total=")
          .append(entry.getValue().totalCalls)
          .append(", success=")
          .append(entry.getValue().successfulCalls)
          .append(", failed=")
          .append(entry.getValue().failedCalls)
          .append("\n");
    }
    return sb.toString();
  }

  /**
   * Resets all rate limiter state.
   */
  public void reset() {
    callHistory.clear();
    serviceStats.clear();
    dynamicWaitTimeMs.clear();
    consecutive429Errors.clear();
  }

  private static class ServiceStats {
    int totalCalls      = 0;
    int successfulCalls = 0;
    int failedCalls     = 0;
  }
}
