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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Tests for {@link RetryUtils}.
 */
public class RetryUtilsTest {

  // =======================================================================
  // Exponential backoff
  // =======================================================================

  @Test
  public void testExponentialBackoffDelay() {
    long expected = 3000L;
    for (int attempt = 1; attempt <= 5; attempt++) {
      long delay = RetryUtils.exponentialBackoffDelay(attempt);
      assertThat(delay).as("Attempt " + attempt).isEqualTo(expected);
      expected = Math.min(expected * 2, 60000L);
    }
  }

  @Test
  public void testExponentialBackoffCappedAtMax() {
    long delay = RetryUtils.exponentialBackoffDelay(10); // 3_000 * 2^9 = 1_536_000
    assertThat(delay).isEqualTo(60000L);
  }

  @Test
  public void testExponentialBackoffWithCustomBase() {
    long delay = RetryUtils.exponentialBackoffDelay(1, 1000L);
    assertThat(delay).isEqualTo(1000L);

    delay = RetryUtils.exponentialBackoffDelay(2, 1000L);
    assertThat(delay).isEqualTo(2000L);

    delay = RetryUtils.exponentialBackoffDelay(3, 1000L);
    assertThat(delay).isEqualTo(4000L);
  }

  @Test
  public void testExponentialBackoffCustomBaseCapped() {
    long delay = RetryUtils.exponentialBackoffDelay(10, 1000L);
    assertThat(delay).isEqualTo(60000L);
  }

  // =======================================================================
  // shouldRetry
  // =======================================================================

  @Test
  public void testShouldRetryWhenUnderMax() {
    assertThat(RetryUtils.shouldRetry(0, 3)).isTrue();
    assertThat(RetryUtils.shouldRetry(1, 3)).isTrue();
    assertThat(RetryUtils.shouldRetry(2, 3)).isTrue();
  }

  @Test
  public void testShouldNotRetryWhenAtMax() {
    assertThat(RetryUtils.shouldRetry(3, 3)).isFalse();
  }

  @Test
  public void testShouldNotRetryWhenOverMax() {
    assertThat(RetryUtils.shouldRetry(4, 3)).isFalse();
  }

  // =======================================================================
  // isRetryable
  // =======================================================================

  @Test
  public void testRateLimitIsRetryable() {
    assertThat(RetryUtils.isRetryable(new AiException.RateLimit("test"))).isTrue();
  }

  @Test
  public void testTimeoutIsRetryable() {
    assertThat(RetryUtils.isRetryable(new AiException.Timeout("test"))).isTrue();
  }

  @Test
  public void testNetworkIsRetryable() {
    assertThat(RetryUtils.isRetryable(new AiException.Network("test", new Exception()))).isTrue();
  }

  @Test
  public void testServerErrorIsRetryable() {
    assertThat(RetryUtils.isRetryable(new AiException.Api(500, "server error"))).isTrue();
    assertThat(RetryUtils.isRetryable(new AiException.Api(502, "bad gateway"))).isTrue();
    assertThat(RetryUtils.isRetryable(new AiException.Api(503, "unavailable"))).isTrue();
  }

  @Test
  public void testClientErrorIsNotRetryable() {
    assertThat(RetryUtils.isRetryable(new AiException.Api(400, "bad request"))).isFalse();
    assertThat(RetryUtils.isRetryable(new AiException.Api(401, "unauthorized"))).isFalse();
    assertThat(RetryUtils.isRetryable(new AiException.Api(403, "forbidden"))).isFalse();
    assertThat(RetryUtils.isRetryable(new AiException.Api(404, "not found"))).isFalse();
  }

  @Test
  public void testAuthenticationIsNotRetryable() {
    assertThat(RetryUtils.isRetryable(new AiException.Authentication("bad key"))).isFalse();
  }

  @Test
  public void testConfigurationIsNotRetryable() {
    assertThat(RetryUtils.isRetryable(new AiException.Configuration("no provider"))).isFalse();
  }

  // =======================================================================
  // sleepWithInterruptHandling
  // =======================================================================

  @Test
  public void testSleepWithInterruptHandling() {
    // just verify it returns without exception for a small sleep
    RetryUtils.sleepWithInterruptHandling(1);
  }
}
