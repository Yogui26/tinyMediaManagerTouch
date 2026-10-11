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
package org.tinymediamanager.ui.components.table;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The class {@link ColumnValueCache} caches the rendered value of a single table column per entity. Values are loaded lazily on first access and kept
 * until they are invalidated for the given entity again.
 * <p>
 * A cell value can very well be {@code null}, so {@link Optional} is used to be able to remember that {@code null} has already been computed.
 *
 * @author Manuel Laggner
 */
class ColumnValueCache {
  private final Map<Object, Optional<Object>> cache = new ConcurrentHashMap<>();

  /**
   * Gets the cached value for the given key, computing and remembering it when it has not been cached yet.
   *
   * @param key
   *          the cache key, identifying the entity this value belongs to
   * @param loader
   *          the loader which computes the value on a cache miss
   * @return the cached (or freshly computed) value, which may be null
   */
  Object get(Object key, Supplier<Object> loader) {
    return cache.computeIfAbsent(key, k -> Optional.ofNullable(loader.get())).orElse(null);
  }

  /**
   * Drops the cached value of the given key. Needed when the underlying entity has been changed or removed.
   *
   * @param key
   *          the cache key to drop
   */
  void invalidate(Object key) {
    if (key != null) {
      cache.remove(key);
    }
  }

  /**
   * Drops all cached values. Needed when a setting changed which all values depend on.
   */
  void clear() {
    cache.clear();
  }
}
