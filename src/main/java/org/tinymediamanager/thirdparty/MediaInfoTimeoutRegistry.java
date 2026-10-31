/*
 * Copyright 2012 - 2026 Manuel Laggner
 *
 * Licensed under the Apache License, Version Version 2.0 (the "License");
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
package org.tinymediamanager.thirdparty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmStore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang3.StringUtils;

/**
 * Remembers files whose MediaInfo analysis already timed out, so the (potentially minutes long) native call is not paid again on every update run.
 * <p>
 * libmediainfo can hang deterministically on a file - visibly damaged ones (interrupted writes) as well as perfectly ordinary looking ones. The
 * native call cannot be interrupted from Java, and the only reliable signal is "this exact file timed out before" - which is exactly the state that
 * would otherwise be thrown away. This registry persists that state in the {@link TmmStore}: a hanging file costs the full timeout once per library
 * lifetime instead of once per update run, and every later file in the batch is analyzed normally instead of starving behind the wedged parser pool.
 * <p>
 * An entry is invalidated when the file size changes (re-downloaded / re-muxed files are examined again).
 *
 * @since 5.3.6
 */
public final class MediaInfoTimeoutRegistry {
  private static final Logger       LOGGER    = LoggerFactory.getLogger(MediaInfoTimeoutRegistry.class);
  private static final String       STORE_KEY = "tmm.mediainfo.timeouts.v1";

  /** absolute path -> file size at the time of the timeout */
  private static final Map<String, Long> TIMED_OUT = new ConcurrentHashMap<>();

  static {
    load();
  }

  private MediaInfoTimeoutRegistry() {
    throw new IllegalAccessError();
  }

  /**
   * should the MediaInfo analysis of this file be skipped, because it already timed out earlier and the file has not changed since?
   *
   * @param file
   *          the file to check
   * @return true/false
   */
  public static boolean shouldSkip(Path file) {
    try {
      Long knownSize = TIMED_OUT.get(absolute(file));
      if (knownSize == null) {
        return false;
      }

      if (knownSize == Files.size(file)) {
        return true;
      }

      // the file has been changed since the timeout - examine it again
      LOGGER.debug("MediaInfo timeout entry invalidated - '{}' changed size ({} -> {})", file, knownSize, Files.size(file));
      TIMED_OUT.remove(absolute(file));
      persist();
      return false;
    }
    catch (Exception e) {
      // never let registry problems block the analysis
      LOGGER.debug("Could not check the MediaInfo timeout registry for '{}': {}", file, e.getMessage());
      return false;
    }
  }

  /**
   * remembers that the MediaInfo analysis of this file timed out (persisted across restarts).
   *
   * @param file
   *          the file whose analysis timed out
   */
  public static void recordTimeout(Path file) {
    try {
      TIMED_OUT.put(absolute(file), Files.size(file));
      persist();
      LOGGER.debug("Recorded MediaInfo timeout for '{}' ({} entries)", file, TIMED_OUT.size());
    }
    catch (Exception e) {
      LOGGER.warn("Could not record the MediaInfo timeout for '{}': {}", file, e.getMessage());
    }
  }

  private static String absolute(Path file) {
    return file.toAbsolutePath().normalize().toString();
  }

  private static synchronized void load() {
    TIMED_OUT.clear();
    try {
      String json = TmmStore.getInstance().get(STORE_KEY);
      if (StringUtils.isBlank(json)) {
        return;
      }

      JsonNode root = new ObjectMapper().readTree(json);
      Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        TIMED_OUT.put(field.getKey(), field.getValue().asLong());
      }
      LOGGER.debug("Loaded {} MediaInfo timeout entries from the store", TIMED_OUT.size());
    }
    catch (Exception e) {
      LOGGER.debug("Could not load the MediaInfo timeout registry - '{}'", e.getMessage());
    }
  }

  private static synchronized void persist() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode root = mapper.createObjectNode();
      TIMED_OUT.forEach((path, size) -> root.put(path, size.longValue()));
      TmmStore.getInstance().put(STORE_KEY, mapper.writeValueAsString(root));
    }
    catch (Exception e) {
      LOGGER.debug("Could not persist the MediaInfo timeout registry - '{}'", e.getMessage());
    }
  }

  /** drops all in-memory entries and reloads them from the store (used by tests) */
  static void reload() {
    load();
  }
}
