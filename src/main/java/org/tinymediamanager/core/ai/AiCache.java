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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link AiCache} provides an in-memory cache for AI API responses with TTL and LRU eviction.
 *
 * @author Manuel Laggner
 */
public class AiCache {
  private static final Logger           LOGGER      = LoggerFactory.getLogger(AiCache.class);
  private static final AiCache          INSTANCE    = new AiCache();
  private static final int              MAX_ENTRIES = 5000;
  private static final Duration         DEFAULT_TTL = Duration.ofHours(24);

  private final Map<String, CacheEntry> cache       = new ConcurrentHashMap<>();

  private AiCache() {
  }

  /**
   * Gets the singleton instance.
   *
   * @return the cache instance
   */
  public static AiCache getInstance() {
    return INSTANCE;
  }

  /**
   * Generates a deterministic cache key from provider + model + messages.
   *
   * @param providerName
   *          the provider name
   * @param model
   *          the model name
   * @param request
   *          the request
   * @return a hex string cache key
   */
  public static String generateKey(String providerName, String model, AiRequest request) {
    StringBuilder sb = new StringBuilder();
    sb.append(providerName).append("|").append(model).append("|");
    for (AiMessage msg : request.getMessages()) {
      sb.append(msg.getRole()).append(":").append(msg.getContent()).append("||");
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    }
    catch (NoSuchAlgorithmException e) {
      return sb.toString();
    }
  }

  /**
   * Gets a cached response.
   *
   * @param cacheKey
   *          the cache key
   * @return the cached content, or null if not found or expired
   */
  public String get(String cacheKey) {
    CacheEntry entry = cache.get(cacheKey);
    if (entry == null) {
      return null;
    }
    if (entry.isExpired()) {
      cache.remove(cacheKey);
      return null;
    }
    return entry.content;
  }

  /**
   * Stores a response in the cache.
   *
   * @param cacheKey
   *          the cache key
   * @param content
   *          the response content
   */
  public void put(String cacheKey, String content) {
    put(cacheKey, content, DEFAULT_TTL);
  }

  /**
   * Stores a response in the cache with a custom TTL.
   *
   * @param cacheKey
   *          the cache key
   * @param content
   *          the response content
   * @param ttl
   *          the time-to-live
   */
  public void put(String cacheKey, String content, Duration ttl) {
    if (cache.size() >= MAX_ENTRIES) {
      evictOldest();
    }
    cache.put(cacheKey, new CacheEntry(content, ttl));
  }

  /**
   * Clears the entire cache.
   */
  public void clear() {
    cache.clear();
    LOGGER.debug("AI cache cleared");
  }

  /**
   * Gets the current cache size.
   *
   * @return the number of cached entries
   */
  public int size() {
    return cache.size();
  }

  private void evictOldest() {
    String oldestKey = null;
    LocalDateTime oldestTime = LocalDateTime.MAX;

    for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
      if (entry.getValue().created.isBefore(oldestTime)) {
        oldestTime = entry.getValue().created;
        oldestKey = entry.getKey();
      }
    }
    if (oldestKey != null) {
      cache.remove(oldestKey);
    }
  }

  private static class CacheEntry {
    final String        content;
    final LocalDateTime created;
    final Duration      ttl;

    CacheEntry(String content, Duration ttl) {
      this.content = content;
      this.created = LocalDateTime.now();
      this.ttl = ttl;
    }

    boolean isExpired() {
      return LocalDateTime.now().isAfter(created.plus(ttl));
    }
  }
}
