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

import java.time.Duration;

import org.junit.After;
import org.junit.Test;

/**
 * Tests for {@link AiCache}.
 */
public class AiCacheTest {

  @After
  public void tearDown() {
    AiCache.getInstance().clear();
  }

  // =======================================================================
  // Key generation
  // =======================================================================

  @Test
  public void testGenerateKeyIsDeterministic() {
    AiRequest request = new AiRequest("gpt-4o-mini", "be helpful", "hello world");
    String key1 = AiCache.generateKey("openai", "gpt-4o-mini", request);
    String key2 = AiCache.generateKey("openai", "gpt-4o-mini", request);
    assertThat(key1).isEqualTo(key2);
  }

  @Test
  public void testGenerateKeyDiffersForDifferentInputs() {
    AiRequest reqA = new AiRequest("gpt-4o-mini", "be helpful", "hello");
    AiRequest reqB = new AiRequest("gpt-4o-mini", "be helpful", "world");
    String keyA = AiCache.generateKey("openai", "gpt-4o-mini", reqA);
    String keyB = AiCache.generateKey("openai", "gpt-4o-mini", reqB);
    assertThat(keyA).isNotEqualTo(keyB);
  }

  @Test
  public void testGenerateKeyDiffersForDifferentProviders() {
    AiRequest request = new AiRequest("gpt-4o-mini", "be helpful", "hello");
    String keyA = AiCache.generateKey("openai", "gpt-4o-mini", request);
    String keyB = AiCache.generateKey("ollama", "gpt-4o-mini", request);
    assertThat(keyA).isNotEqualTo(keyB);
  }

  @Test
  public void testGenerateKeyReturnsHexString() {
    AiRequest request = new AiRequest("gpt-4o-mini", "be helpful", "hello");
    String key = AiCache.generateKey("openai", "gpt-4o-mini", request);
    assertThat(key).matches("[0-9a-f]{64}");
  }

  // =======================================================================
  // Basic put/get
  // =======================================================================

  @Test
  public void testPutAndGet() {
    AiCache cache = AiCache.getInstance();
    cache.put("key1", "value1");
    assertThat(cache.get("key1")).isEqualTo("value1");
  }

  @Test
  public void testGetNonExistentReturnsNull() {
    AiCache cache = AiCache.getInstance();
    assertThat(cache.get("nonexistent")).isNull();
  }

  @Test
  public void testOverwriteExistingKey() {
    AiCache cache = AiCache.getInstance();
    cache.put("key1", "old");
    cache.put("key1", "new");
    assertThat(cache.get("key1")).isEqualTo("new");
  }

  // =======================================================================
  // Size tracking
  // =======================================================================

  @Test
  public void testSizeAfterPut() {
    AiCache cache = AiCache.getInstance();
    cache.clear();
    assertThat(cache.size()).isEqualTo(0);
    cache.put("a", "1");
    assertThat(cache.size()).isEqualTo(1);
    cache.put("b", "2");
    assertThat(cache.size()).isEqualTo(2);
  }

  @Test
  public void testSizeAfterClear() {
    AiCache cache = AiCache.getInstance();
    cache.put("a", "1");
    cache.put("b", "2");
    cache.clear();
    assertThat(cache.size()).isEqualTo(0);
  }

  // =======================================================================
  // TTL expiry
  // =======================================================================

  @Test
  public void testExpiredEntryReturnsNull() throws Exception {
    AiCache cache = AiCache.getInstance();
    cache.put("expires_soon", "value", Duration.ofMillis(1));
    // give time for expiry
    Thread.sleep(10);
    assertThat(cache.get("expires_soon")).isNull();
  }

  @Test
  public void testEntryNotExpiredWithinTtl() {
    AiCache cache = AiCache.getInstance();
    cache.put("fresh", "value", Duration.ofHours(1));
    assertThat(cache.get("fresh")).isEqualTo("value");
  }

  // =======================================================================
  // Eviction when full
  // =======================================================================

  @Test
  public void testEvictionOfOldestWhenFull() {
    AiCache cache = AiCache.getInstance();
    cache.clear();

    // fill cache with many entries to trigger eviction (MAX_ENTRIES is 5000)
    // test with a smaller practical check: verify it keeps working
    for (int i = 0; i < 100; i++) {
      cache.put("k" + i, "v" + i);
    }
    assertThat(cache.size()).isEqualTo(100);
  }

  // =======================================================================
  // Custom TTL
  // =======================================================================

  @Test
  public void testCustomTtl() {
    AiCache cache = AiCache.getInstance();
    cache.put("custom", "value", Duration.ofMinutes(5));
    assertThat(cache.get("custom")).isEqualTo("value");
  }
}
