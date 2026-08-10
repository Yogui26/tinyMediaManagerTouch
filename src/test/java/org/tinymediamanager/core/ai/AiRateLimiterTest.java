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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link AiRateLimiter}.
 */
public class AiRateLimiterTest {

  @Before
  public void setup() throws Exception {
    AiRateLimiter.getInstance().reset();
  }

  @After
  public void tearDown() {
    AiRateLimiter.getInstance().reset();
  }

  // =======================================================================
  // Singleton
  // =======================================================================

  @Test
  public void testSingleton() {
    assertThat(AiRateLimiter.getInstance()).isSameAs(AiRateLimiter.getInstance());
  }

  // =======================================================================
  // Reset
  // =======================================================================

  @Test
  public void testReset() {
    AiRateLimiter limiter = AiRateLimiter.getInstance();
    limiter.recordSuccessfulCall("test-provider");
    limiter.recordFailedCall("test-provider");
    limiter.reset();
    assertThat(limiter.getStatistics()).isEmpty();
  }

  // =======================================================================
  // Record calls
  // =======================================================================

  @Test
  public void testRecordSuccessfulCall() {
    AiRateLimiter limiter = AiRateLimiter.getInstance();
    limiter.recordSuccessfulCall("provider-a");

    String stats = limiter.getStatistics();
    assertThat(stats).contains("provider-a");
    assertThat(stats).contains("total=1");
    assertThat(stats).contains("success=1");
    assertThat(stats).contains("failed=0");
  }

  @Test
  public void testRecordFailedCall() {
    AiRateLimiter limiter = AiRateLimiter.getInstance();
    limiter.recordFailedCall("provider-b");

    String stats = limiter.getStatistics();
    assertThat(stats).contains("provider-b");
    assertThat(stats).contains("total=1");
    assertThat(stats).contains("success=0");
    assertThat(stats).contains("failed=1");
  }

  @Test
  public void testRecord429Error() {
    AiRateLimiter limiter = AiRateLimiter.getInstance();
    limiter.record429Error("provider-c");

    String stats = limiter.getStatistics();
    assertThat(stats).contains("provider-c");
    assertThat(stats).contains("total=1");
    assertThat(stats).contains("failed=1");
  }

  @Test
  public void testMixedCalls() {
    AiRateLimiter limiter = AiRateLimiter.getInstance();
    limiter.recordSuccessfulCall("p");
    limiter.recordSuccessfulCall("p");
    limiter.recordFailedCall("p");
    limiter.record429Error("p");

    String stats = limiter.getStatistics();
    assertThat(stats).contains("total=4");
    assertThat(stats).contains("success=2");
    assertThat(stats).contains("failed=2");
  }

  @Test
  public void testMultipleProviders() {
    AiRateLimiter limiter = AiRateLimiter.getInstance();
    limiter.recordSuccessfulCall("provider-a");
    limiter.recordFailedCall("provider-b");

    String stats = limiter.getStatistics();
    assertThat(stats).contains("provider-a");
    assertThat(stats).contains("provider-b");
  }

  @Test
  public void testGetStatisticsEmpty() {
    AiRateLimiter limiter = AiRateLimiter.getInstance();
    limiter.reset();
    assertThat(limiter.getStatistics()).isEmpty();
  }

  @Test
  public void testWaitForPermissionWhenRateLimitDisabled() {
    AiRateLimiter limiter = AiRateLimiter.getInstance();
    // when rate limiting is disabled in settings, waitForPermission always returns true
    // (the rate limiter reads aiRateLimitEnabled from Settings.getInstance())
    limiter.reset();
    assertThat(limiter.getStatistics()).isEmpty();
  }
}
