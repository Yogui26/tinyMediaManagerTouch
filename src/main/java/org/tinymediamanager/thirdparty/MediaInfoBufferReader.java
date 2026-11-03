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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.thirdparty.MediaInfo.StreamKind;

/**
 * The Class {@link MediaInfoBufferReader} feeds media files to libmediainfo in bounded chunks.
 * <p>
 * libmediainfo calls may hang while parsing some files. Reading the file ourselves and supplying it through the buffer API limits how much data
 * libmediainfo can consume, even if it keeps requesting more data.
 * </p>
 * <p>
 * The maximum amount of data read per file can be configured with the JVM property {@value #MAX_FILE_SIZE_PROPERTY}, specified in MiB (default
 * {@value #DEFAULT_MAX_FILE_SIZE_MB}). The property is read once at startup.
 * </p>
 *
 * @author Manuel Laggner
 * @since v5
 */
public final class MediaInfoBufferReader {

  /** JVM property for the maximum amount of data read per file, in MiB. */
  public static final String             MAX_FILE_SIZE_PROPERTY   = "tmm.mediainfo.maxfilesize";

  /** Default maximum amount of data read per file, in MiB. */
  public static final int                DEFAULT_MAX_FILE_SIZE_MB = 64;

  private static final Logger            LOGGER                   = LoggerFactory.getLogger(MediaInfoBufferReader.class);

  private static final int               BUFFER_SIZE              = 256 * 1024;
  private static final long              MEBIBYTE                 = 1024L * 1024L;
  private static final long              MAX_FILE_SIZE_BYTES      = parseMaxFileSize(System.getProperty(MAX_FILE_SIZE_PROPERTY));
  private static final DateTimeFormatter DATE_TIME_FORMATTER      = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

  /**
   * Reads data from an underlying media source.
   */
  @FunctionalInterface
  public interface BufferReader {
    /**
     * Reads up to {@code length} bytes from the given position.
     *
     * @param position
     *          the position to read from
     * @param buffer
     *          the buffer to fill
     * @param offset
     *          the offset in the buffer
     * @param length
     *          the maximum number of bytes to read
     * @return the number of bytes read, or zero/negative at the end of the source
     * @throws IOException
     *           if the data could not be read
     */
    int read(long position, byte[] buffer, int offset, int length) throws IOException;
  }

  private MediaInfoBufferReader() {
    throw new IllegalAccessError();
  }

  /**
   * Gets the maximum amount of data read per file.
   *
   * @return the maximum amount of data in bytes
   */
  public static long getMaxFileSizeBytes() {
    return MAX_FILE_SIZE_BYTES;
  }

  /**
   * Reads a media file through the libmediainfo buffer API.
   *
   * @param file
   *          the file to read
   * @return the MediaInfo snapshot, or an empty map if nothing could be read
   * @throws IOException
   *           if the file could not be read
   */
  public static Map<StreamKind, List<Map<String, String>>> read(Path file) throws IOException {
    try (MediaInfo mediaInfo = new MediaInfo(); FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
      long fileSize = channel.size();
      Map<StreamKind, List<Map<String, String>>> snapshot = read(mediaInfo, fileSize,
          (position, buffer, offset, length) -> readFully(channel, position, buffer, offset, length), file.toString(), MAX_FILE_SIZE_BYTES);
      addFileMetadata(snapshot, file);
      return snapshot;
    }
  }

  /**
   * Reads a media source through the libmediainfo buffer API.
   *
   * @param mediaInfo
   *          the MediaInfo instance to use
   * @param fileSize
   *          the total size of the source
   * @param reader
   *          the source to read from
   * @param source
   *          a description of the source for logging
   * @return the MediaInfo snapshot, or an empty map if nothing could be read
   * @throws IOException
   *           if the source could not be read
   */
  public static Map<StreamKind, List<Map<String, String>>> read(MediaInfo mediaInfo, long fileSize, BufferReader reader, String source)
      throws IOException {
    return read(mediaInfo, fileSize, reader, source, MAX_FILE_SIZE_BYTES);
  }

  /**
   * Reads a media source through the libmediainfo buffer API with an explicit limit.
   *
   * @param mediaInfo
   *          the MediaInfo instance to use
   * @param fileSize
   *          the total size of the source
   * @param reader
   *          the source to read from
   * @param source
   *          a description of the source for logging
   * @param maxFileSizeBytes
   *          the maximum amount of data to read
   * @return the MediaInfo snapshot, or an empty map if nothing could be read
   * @throws IOException
   *           if the source could not be read
   */
  static Map<StreamKind, List<Map<String, String>>> read(MediaInfo mediaInfo, long fileSize, BufferReader reader, String source,
      long maxFileSizeBytes) throws IOException {
    if (fileSize <= 0 || maxFileSizeBytes <= 0) {
      return Collections.emptyMap();
    }

    if (Thread.currentThread().isInterrupted()) {
      LOGGER.debug("MediaInfo buffer processing was interrupted before start for '{}'", source);
      return Collections.emptyMap();
    }

    if (!mediaInfo.openBufferInit(fileSize, 0)) {
      LOGGER.warn("MediaInfo could not initialize buffer processing for '{}'", source);
      return Collections.emptyMap();
    }

    byte[] buffer = new byte[BUFFER_SIZE];
    long position = 0;
    long bytesRead = 0;
    boolean interrupted = false;
    boolean maximumReached = false;

    try {
      while (true) {
        if (Thread.currentThread().isInterrupted()) {
          interrupted = true;
          break;
        }

        if (position < 0 || position > fileSize) {
          LOGGER.warn("MediaInfo requested an invalid position {} for '{}'", position, source);
          break;
        }
        if (position == fileSize) {
          break;
        }

        long remainingBytes = maxFileSizeBytes - bytesRead;
        if (remainingBytes <= 0) {
          maximumReached = true;
          break;
        }

        int bytesToRead = (int) Math.min(BUFFER_SIZE, Math.min(fileSize - position, remainingBytes));
        int read = reader.read(position, buffer, 0, bytesToRead);
        if (read <= 0) {
          break;
        }
        if (read > bytesToRead) {
          LOGGER.warn("MediaInfo source '{}' returned {} bytes, but only {} were requested", source, read, bytesToRead);
          read = bytesToRead;
        }

        bytesRead += read;
        int result = mediaInfo.openBufferContinue(buffer, read);
        if ((result & 8) == 8) { // finalized
          break;
        }

        long requestedPosition = mediaInfo.openBufferContinueGoToGet();
        if (requestedPosition != -1) {
          if (requestedPosition < 0 || requestedPosition > fileSize) {
            LOGGER.warn("MediaInfo requested an invalid seek position {} for '{}'", requestedPosition, source);
            break;
          }
          if (requestedPosition == fileSize) {
            break;
          }
          position = requestedPosition;
          if (!mediaInfo.openBufferInit(fileSize, position)) {
            LOGGER.warn("MediaInfo could not reinitialize buffer processing at position {} for '{}'", position, source);
            break;
          }
        }
        else {
          position += read;
        }
      }
    }
    finally {
      if (interrupted) {
        LOGGER.debug("MediaInfo buffer processing was interrupted for '{}'", source);
      }
      else {
        try {
          mediaInfo.openBufferFinalize();
        }
        catch (Exception | Error e) {
          LOGGER.warn("Could not finalize MediaInfo buffer processing for '{}' - {}", source, e.getMessage());
        }
      }
    }

    if (maximumReached) {
      LOGGER.warn("MediaInfo reached the maximum read size of {} bytes for '{}' (file size: {})", maxFileSizeBytes, source, fileSize);
    }

    if (interrupted) {
      return Collections.emptyMap();
    }
    return mediaInfo.snapshot();
  }

  private static int readFully(FileChannel channel, long position, byte[] buffer, int offset, int length) throws IOException {
    int totalRead = 0;
    while (totalRead < length) {
      int read = channel.read(ByteBuffer.wrap(buffer, offset + totalRead, length - totalRead), position + totalRead);
      if (read <= 0) {
        break;
      }
      totalRead += read;
    }
    return totalRead;
  }

  static void addFileMetadata(Map<StreamKind, List<Map<String, String>>> snapshot, Path file) {
    List<Map<String, String>> generalStreams = snapshot.get(StreamKind.General);
    if (generalStreams == null || generalStreams.isEmpty()) {
      return;
    }

    Map<String, String> general = generalStreams.get(0);
    Path absoluteFile = Utils.adaptForNativeAccess(file).toAbsolutePath();
    Path folder = absoluteFile.getParent();
    String fileNameExtension = absoluteFile.getFileName() != null ? absoluteFile.getFileName().toString() : "";
    String fileName = FilenameUtils.getBaseName(fileNameExtension);
    String fileExtension = FilenameUtils.getExtension(fileNameExtension);

    putIfAbsent(general, "CompleteName", absoluteFile.toString());
    putIfAbsent(general, "FolderName", folder != null ? folder.toString() : "");
    putIfAbsent(general, "FileNameExtension", fileNameExtension);
    putIfAbsent(general, "FileName", fileName);
    putIfAbsent(general, "FileExtension", fileExtension);

    try {
      BasicFileAttributes attributes = Files.readAttributes(absoluteFile, BasicFileAttributes.class);
      addFileDate(general, "File_Created_Date", attributes.creationTime(), true);
      addFileDate(general, "File_Created_Date_Local", attributes.creationTime(), false);
      addFileDate(general, "File_Modified_Date", attributes.lastModifiedTime(), true);
      addFileDate(general, "File_Modified_Date_Local", attributes.lastModifiedTime(), false);
    }
    catch (IOException e) {
      LOGGER.trace("Could not read file attributes for '{}' - {}", absoluteFile, e.getMessage());
    }
  }

  private static void addFileDate(Map<String, String> general, String key, FileTime fileTime, boolean utc) {
    if (fileTime == null || fileTime.toMillis() <= 0) {
      return;
    }

    Instant instant = fileTime.toInstant();
    if (utc) {
      putIfAbsent(general, key, DATE_TIME_FORMATTER.format(instant.atZone(ZoneOffset.UTC)) + " UTC");
    }
    else {
      putIfAbsent(general, key, DATE_TIME_FORMATTER.format(instant.atZone(ZoneId.systemDefault())));
    }
  }

  private static void putIfAbsent(Map<String, String> general, String key, String value) {
    if (StringUtils.isNotEmpty(value) && !general.containsKey(key)) {
      general.put(key, value);
    }
  }

  static long parseMaxFileSize(String configuredValue) {
    if (StringUtils.isBlank(configuredValue)) {
      return DEFAULT_MAX_FILE_SIZE_MB * MEBIBYTE;
    }

    try {
      long mebibytes = Long.parseLong(configuredValue.trim());
      if (mebibytes <= 0) {
        LOGGER.warn("Invalid value '{}' for '{}'; using default of {} MiB", configuredValue, MAX_FILE_SIZE_PROPERTY, DEFAULT_MAX_FILE_SIZE_MB);
        return DEFAULT_MAX_FILE_SIZE_MB * MEBIBYTE;
      }
      if (mebibytes > Long.MAX_VALUE / MEBIBYTE) {
        LOGGER.warn("Value '{}' for '{}' is too large; using {} MiB", configuredValue, MAX_FILE_SIZE_PROPERTY, Long.MAX_VALUE / MEBIBYTE);
        return (Long.MAX_VALUE / MEBIBYTE) * MEBIBYTE;
      }
      return mebibytes * MEBIBYTE;
    }
    catch (NumberFormatException e) {
      LOGGER.warn("Invalid value '{}' for '{}'; using default of {} MiB", configuredValue, MAX_FILE_SIZE_PROPERTY, DEFAULT_MAX_FILE_SIZE_MB);
      return DEFAULT_MAX_FILE_SIZE_MB * MEBIBYTE;
    }
  }
}
