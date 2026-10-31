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
package org.tinymediamanager.thirdparty;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.thirdparty.MediaInfo.StreamKind;

/**
 * The Class {@link MediaInfoTimeoutWrapper} provides timeout protection for MediaInfo operations.
 * <p>
 * MediaInfo JNA calls can sometimes hang indefinitely when processing problematic media files. This wrapper executes MediaInfo operations in a
 * separate thread with a configurable timeout to prevent the application from becoming unresponsive.
 * </p>
 * <p>
 * <b>Important:</b> Due to limitations with JNA/native library calls, thread interruption may not immediately terminate stuck operations. The timeout
 * mechanism provides application-level protection by returning control to the caller, while the underlying native operation may continue running in
 * the background until it completes naturally.
 * </p>
 *
 * @author Manuel Laggner
 * @since v5
 */
public class MediaInfoTimeoutWrapper {

  private static final Logger          LOGGER           = LoggerFactory.getLogger(MediaInfoTimeoutWrapper.class);

  private static final ExecutorService EXECUTOR_SERVICE = createExecutorService();

  private static final int             TIMEOUT_SECONDS  = Constants.MI_TIMEOUT_SECONDS;

  /**
   * Creates a daemon thread pool for MediaInfo operations.
   * <p>
   * <b>Important:</b> Threads in this pool are daemon threads and will be terminated forcefully if they don't respond to interruption. This is
   * necessary because MediaInfo JNA calls can block indefinitely in native code and not respond to Java interrupts.
   * </p>
   *
   * @return the executor service
   */
  private static ExecutorService createExecutorService() {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactory() {
      private final AtomicInteger count = new AtomicInteger(0);

      @Override
      public Thread newThread(@NonNull Runnable r) {
        Thread t = new Thread(r, "MediaInfoTimeout-" + count.incrementAndGet());
        t.setDaemon(true);
        return t;
      }
    });

    // Allow core threads to timeout to prevent resource leaks
    executor.allowCoreThreadTimeOut(true);

    return executor;
  }

  /**
   * Opens a media file with MediaInfo and returns the snapshot with timeout protection.
   *
   * @param file
   *          the path to the media file
   * @return a map containing the media information snapshot, or an empty map if operation times out or fails
   */
  public static Map<StreamKind, List<Map<String, String>>> getSnapshot(Path file) {
    return getSnapshot(file, TIMEOUT_SECONDS);
  }

  /**
   * Opens a media file with MediaInfo and returns the snapshot with timeout protection.
   *
   * @param file
   *          the path to the media file
   * @param timeoutSeconds
   *          the timeout in seconds
   * @return a map containing the media information snapshot, or an empty map if operation times out or fails
   */
  public static Map<StreamKind, List<Map<String, String>>> getSnapshot(Path file, int timeoutSeconds) {
    if (file == null) {
      LOGGER.warn("Cannot get snapshot: file path is null");
      return Map.of();
    }

    // the analysis of this file already timed out earlier (and the file did not change since) - a repeated native call would hang again
    if (MediaInfoTimeoutRegistry.shouldSkip(file)) {
      LOGGER.warn("MediaInfo skipped for '{}': the analysis timed out earlier and the file has not changed", file);
      return Map.of();
    }

    try {
      Callable<Map<StreamKind, List<Map<String, String>>>> task = () -> {
        try (MediaInfo mediaInfo = new MediaInfo()) {
          if (!mediaInfo.open(file)) {
            LOGGER.error("MediaInfo could not open file '{}'", file);
            return Map.of();
          }
          return mediaInfo.snapshot();
        }
      };

      Future<Map<StreamKind, List<Map<String, String>>>> future = EXECUTOR_SERVICE.submit(task);

      try {
        LOGGER.trace("Getting MediaInfo snapshot for '{}' with timeout of {} seconds", file, timeoutSeconds);
        return future.get(timeoutSeconds, TimeUnit.SECONDS);
      }
      catch (TimeoutException e) {
        LOGGER.error("MediaInfo operation timed out after {} seconds for file '{}'", timeoutSeconds, file);
        future.cancel(true);
        MediaInfoTimeoutRegistry.recordTimeout(file);
        return Map.of();
      }
    }
    catch (InterruptedException e) {
      LOGGER.warn("MediaInfo operation was interrupted for file '{}'", file);
      Thread.currentThread().interrupt();
      return Map.of();
    }
    catch (Exception e) {
      LOGGER.error("Error getting MediaInfo snapshot for file '{}': {}", file, e.getMessage());
      return Map.of();
    }
  }

  /**
   * Opens a media file with buffer-based processing and timeout protection (for streaming/ISO files).
   * <p>
   * This method initializes the buffer for streaming operations.
   *
   * @param file
   *          the path to the media file
   * @return a MediaInfo instance ready for buffer operations, or null if operation times out
   */
  public static MediaInfo openForBufferProcessing(Path file) {
    return openForBufferProcessing(file, TIMEOUT_SECONDS);
  }

  /**
   * Opens a media file with buffer-based processing and timeout protection (for streaming/ISO files).
   * <p>
   * This method initializes the buffer for streaming operations.
   *
   * @param file
   *          the path to the media file
   * @param timeoutSeconds
   *          the timeout in seconds
   * @return a MediaInfo instance ready for buffer operations, or null if operation times out
   */
  public static MediaInfo openForBufferProcessing(Path file, int timeoutSeconds) {
    if (file == null) {
      LOGGER.warn("Cannot open file for buffer processing: file path is null");
      return null;
    }

    // the analysis of this file already timed out earlier (and the file did not change since) - a repeated native call would hang again
    if (MediaInfoTimeoutRegistry.shouldSkip(file)) {
      LOGGER.warn("MediaInfo buffer processing skipped for '{}': the analysis timed out earlier and the file has not changed", file);
      return null;
    }

    try {
      Callable<MediaInfo> task = () -> {
        MediaInfo mediaInfo = new MediaInfo();
        if (!mediaInfo.open(file)) {
          LOGGER.warn("MediaInfo could not open file '{}' for buffer processing", file);
          mediaInfo.close();
          return null;
        }
        return mediaInfo;
      };

      Future<MediaInfo> future = EXECUTOR_SERVICE.submit(task);

      try {
        LOGGER.trace("Opening MediaInfo for buffer processing of '{}' with timeout of {} seconds", file, timeoutSeconds);
        return future.get(timeoutSeconds, TimeUnit.SECONDS);
      }
      catch (TimeoutException e) {
        LOGGER.warn("MediaInfo buffer initialization timed out after {} seconds for file '{}'", timeoutSeconds, file);
        future.cancel(true);
        MediaInfoTimeoutRegistry.recordTimeout(file);
        return null;
      }
    }
    catch (InterruptedException e) {
      LOGGER.warn("MediaInfo buffer operation was interrupted for file '{}'", file);
      Thread.currentThread().interrupt();
      return null;
    }
    catch (Exception e) {
      LOGGER.error("Error opening MediaInfo for buffer processing of file '{}': {}", file, e.getMessage());
      return null;
    }
  }

  /**
   * Executes a buffer-based MediaInfo processing operation with timeout protection.
   * <p>
   * Use this method for ISO file extraction or streaming scenarios where MediaInfo data is fed incrementally via buffers. The entire operation must
   * complete within the configured timeout period.
   * </p>
   *
   * @param operation
   *          a callable that performs the complete buffer-based operation and returns the media information snapshot. The callable should handle
   *          MediaInfo object lifecycle (creation, cleanup) internally.
   * @return the snapshot from the operation, or an empty map if operation times out or fails
   */
  public static Map<StreamKind, List<Map<String, String>>> executeBufferOperation(Callable<Map<StreamKind, List<Map<String, String>>>> operation) {
    return executeBufferOperation(operation, TIMEOUT_SECONDS);
  }

  /**
   * Allows overriding the timeout for specific operations.
   * <p>
   * Executes a buffer-based MediaInfo processing operation with a custom timeout.
   * </p>
   *
   * @param operation
   *          a callable that performs the complete buffer-based operation
   * @param timeoutSeconds
   *          the timeout in seconds
   * @return the snapshot from the operation, or an empty map if operation times out or fails
   */
  public static Map<StreamKind, List<Map<String, String>>> executeBufferOperation(Callable<Map<StreamKind, List<Map<String, String>>>> operation,
      int timeoutSeconds) {
    return executeBufferOperation(null, operation, timeoutSeconds);
  }

  /**
   * Executes a buffer-based MediaInfo processing operation with the default timeout, tied to the file the operation reads from.
   *
   * @param file
   *          the media file the operation reads from (used for the timeout registry; may be null)
   * @param operation
   *          a callable that performs the complete buffer-based operation
   * @return the snapshot from the operation, or an empty map if operation times out or fails
   */
  public static Map<StreamKind, List<Map<String, String>>> executeBufferOperation(Path file,
      Callable<Map<StreamKind, List<Map<String, String>>>> operation) {
    return executeBufferOperation(file, operation, TIMEOUT_SECONDS);
  }

  /**
   * Executes a buffer-based MediaInfo processing operation with a custom timeout, tied to the file the operation reads from.
   * <p>
   * When the file's analysis already timed out earlier (and the file did not change since), the operation is skipped entirely - a repeated native
   * call would hang again. On a timeout, the file is remembered so later runs do not re-pay the full timeout.
   * </p>
   *
   * @param file
   *          the media file the operation reads from (used for the timeout registry; may be null for anonymous operations, which are neither skipped
   *          nor recorded)
   * @param operation
   *          a callable that performs the complete buffer-based operation
   * @param timeoutSeconds
   *          the timeout in seconds
   * @return the snapshot from the operation, or an empty map if operation times out or fails
   */
  public static Map<StreamKind, List<Map<String, String>>> executeBufferOperation(Path file,
      Callable<Map<StreamKind, List<Map<String, String>>>> operation, int timeoutSeconds) {
    if (operation == null) {
      LOGGER.warn("Cannot execute buffer operation: operation is null");
      return Map.of();
    }

    if (file != null && MediaInfoTimeoutRegistry.shouldSkip(file)) {
      LOGGER.warn("MediaInfo buffer operation skipped for '{}': the analysis timed out earlier and the file has not changed", file);
      return Map.of();
    }

    if (timeoutSeconds <= 0) {
      LOGGER.warn("Invalid timeout value: {}, using default timeout", timeoutSeconds);
      return executeBufferOperation(file, operation);
    }

    try {
      Future<Map<StreamKind, List<Map<String, String>>>> future = EXECUTOR_SERVICE.submit(operation);

      try {
        LOGGER.trace("Executing buffer-based MediaInfo operation with custom timeout of {} seconds", timeoutSeconds);
        return future.get(timeoutSeconds, TimeUnit.SECONDS);
      }
      catch (TimeoutException e) {
        LOGGER.warn("Buffer-based MediaInfo operation timed out after {} seconds", timeoutSeconds);
        future.cancel(true);
        if (file != null) {
          MediaInfoTimeoutRegistry.recordTimeout(file);
        }
        return Map.of();
      }
    }
    catch (InterruptedException e) {
      LOGGER.warn("Buffer-based MediaInfo operation was interrupted");
      Thread.currentThread().interrupt();
      return Map.of();
    }
    catch (Exception e) {
      LOGGER.error("Error executing buffer-based MediaInfo operation: {}", e.getMessage());
      return Map.of();
    }
  }

  private MediaInfoTimeoutWrapper() {
    throw new IllegalAccessError();
  }
}
