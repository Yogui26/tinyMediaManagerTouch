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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.tinymediamanager.thirdparty.MediaInfo.StreamKind;

public class MediaInfoBufferReaderTest {

  private static final long MEBIBYTE = 1024L * 1024L;

  @Test
  public void testDefaultMaxFileSize() {
    assertThat(MediaInfoBufferReader.parseMaxFileSize(null)).isEqualTo(64 * MEBIBYTE);
    assertThat(MediaInfoBufferReader.parseMaxFileSize("")).isEqualTo(64 * MEBIBYTE);
    assertThat(MediaInfoBufferReader.parseMaxFileSize("  ")).isEqualTo(64 * MEBIBYTE);
  }

  @Test
  public void testConfiguredMaxFileSize() {
    assertThat(MediaInfoBufferReader.parseMaxFileSize("1")).isEqualTo(MEBIBYTE);
    assertThat(MediaInfoBufferReader.parseMaxFileSize(" 128 ")).isEqualTo(128 * MEBIBYTE);
  }

  @Test
  public void testInvalidMaxFileSizeFallsBackToDefault() {
    assertThat(MediaInfoBufferReader.parseMaxFileSize("0")).isEqualTo(64 * MEBIBYTE);
    assertThat(MediaInfoBufferReader.parseMaxFileSize("-1")).isEqualTo(64 * MEBIBYTE);
    assertThat(MediaInfoBufferReader.parseMaxFileSize("not-a-number")).isEqualTo(64 * MEBIBYTE);
  }

  @Test
  public void testStopsAtMaximumReadSize() throws Exception {
    TestMediaInfo mediaInfo = new TestMediaInfo();
    List<Long> positions = new ArrayList<>();

    Map<StreamKind, List<Map<String, String>>> snapshot = MediaInfoBufferReader.read(mediaInfo, 1_000_000, source(1_000_000, positions), "test",
        300_000);

    assertThat(snapshot).isNotEmpty();
    assertThat(positions).containsExactly(0L, 262_144L);
    assertThat(mediaInfo.continueCount).isEqualTo(2);
    assertThat(mediaInfo.finalizeCount).isEqualTo(1);
  }

  @Test
  public void testStopsWhenMediaInfoFinalizes() throws Exception {
    TestMediaInfo mediaInfo = new TestMediaInfo();
    mediaInfo.finalizeAfterContinue = 1;

    Map<StreamKind, List<Map<String, String>>> snapshot = MediaInfoBufferReader.read(mediaInfo, 1_000_000, source(1_000_000, new ArrayList<>()),
        "test", 64 * MEBIBYTE);

    assertThat(snapshot).isNotEmpty();
    assertThat(mediaInfo.continueCount).isEqualTo(1);
    assertThat(mediaInfo.finalizeCount).isEqualTo(1);
  }

  @Test
  public void testFollowsSeekRequests() throws Exception {
    TestMediaInfo mediaInfo = new TestMediaInfo();
    mediaInfo.finalizeAfterContinue = 2;
    mediaInfo.seekRequests.add(100L);
    List<Long> positions = new ArrayList<>();

    Map<StreamKind, List<Map<String, String>>> snapshot = MediaInfoBufferReader.read(mediaInfo, 1_000, source(1_000, positions), "test",
        64 * MEBIBYTE);

    assertThat(snapshot).isNotEmpty();
    assertThat(positions).containsExactly(0L, 100L);
    assertThat(mediaInfo.initOffsets).containsExactly(0L, 100L);
    assertThat(mediaInfo.continueCount).isEqualTo(2);
    assertThat(mediaInfo.finalizeCount).isEqualTo(1);
  }

  @Test
  public void testStopsAtEndOfFile() throws Exception {
    TestMediaInfo mediaInfo = new TestMediaInfo();
    List<Long> positions = new ArrayList<>();

    Map<StreamKind, List<Map<String, String>>> snapshot = MediaInfoBufferReader.read(mediaInfo, 1_000, source(1_000, positions), "test",
        64 * MEBIBYTE);

    assertThat(snapshot).isNotEmpty();
    assertThat(positions).containsExactly(0L);
    assertThat(mediaInfo.finalizeCount).isEqualTo(1);
  }

  @Test
  public void testStopsOnInvalidSeek() throws Exception {
    TestMediaInfo mediaInfo = new TestMediaInfo();
    mediaInfo.seekRequests.add(1_001L);

    Map<StreamKind, List<Map<String, String>>> snapshot = MediaInfoBufferReader.read(mediaInfo, 1_000, source(1_000, new ArrayList<>()), "test",
        64 * MEBIBYTE);

    assertThat(snapshot).isNotEmpty();
    assertThat(mediaInfo.initOffsets).containsExactly(0L);
    assertThat(mediaInfo.finalizeCount).isEqualTo(1);
  }

  @Test
  public void testStopsOnSeekToEndOfFile() throws Exception {
    TestMediaInfo mediaInfo = new TestMediaInfo();
    mediaInfo.seekRequests.add(1_000L);

    Map<StreamKind, List<Map<String, String>>> snapshot = MediaInfoBufferReader.read(mediaInfo, 1_000, source(1_000, new ArrayList<>()), "test",
        64 * MEBIBYTE);

    assertThat(snapshot).isNotEmpty();
    assertThat(mediaInfo.initOffsets).containsExactly(0L);
    assertThat(mediaInfo.finalizeCount).isEqualTo(1);
  }

  @Test
  public void testReturnsEmptyWhenInitializationFails() throws Exception {
    TestMediaInfo mediaInfo = new TestMediaInfo();
    mediaInfo.initSucceeds = false;
    List<Long> positions = new ArrayList<>();

    Map<StreamKind, List<Map<String, String>>> snapshot = MediaInfoBufferReader.read(mediaInfo, 1_000, source(1_000, positions), "test",
        64 * MEBIBYTE);

    assertThat(snapshot).isEmpty();
    assertThat(positions).isEmpty();
    assertThat(mediaInfo.finalizeCount).isZero();
  }

  @Test
  public void testAlreadyInterruptedDoesNotStartProcessing() throws Exception {
    TestMediaInfo mediaInfo = new TestMediaInfo();

    Thread.currentThread().interrupt();
    try {
      Map<StreamKind, List<Map<String, String>>> snapshot = MediaInfoBufferReader.read(mediaInfo, 1_000, source(1_000, new ArrayList<>()), "test",
          64 * MEBIBYTE);

      assertThat(snapshot).isEmpty();
      assertThat(mediaInfo.initOffsets).isEmpty();
      assertThat(mediaInfo.finalizeCount).isZero();
    }
    finally {
      Thread.interrupted();
    }
  }

  @Test
  public void testFinalizeFailureDoesNotDiscardSnapshot() throws Exception {
    TestMediaInfo mediaInfo = new TestMediaInfo();
    mediaInfo.finalizeThrows = true;
    mediaInfo.finalizeAfterContinue = 1;

    Map<StreamKind, List<Map<String, String>>> snapshot = MediaInfoBufferReader.read(mediaInfo, 1_000, source(1_000, new ArrayList<>()), "test",
        64 * MEBIBYTE);

    assertThat(snapshot).isNotEmpty();
    assertThat(mediaInfo.finalizeCount).isEqualTo(1);
  }

  @Test
  public void testOversizedSourceReadIsClamped() throws Exception {
    TestMediaInfo mediaInfo = new TestMediaInfo();
    mediaInfo.finalizeAfterContinue = 1;

    Map<StreamKind, List<Map<String, String>>> snapshot = MediaInfoBufferReader.read(mediaInfo, 1_000,
        (position, buffer, offset, length) -> length + 100, "test", 64 * MEBIBYTE);

    assertThat(snapshot).isNotEmpty();
    assertThat(mediaInfo.continueSizes).containsExactly(1_000);
  }

  @Test
  public void testFileMetadataIsAddedToSnapshot() throws Exception {
    Path file = Files.createTempFile("tmm-mediainfo", ".mkv");
    try {
      Map<String, String> general = new LinkedHashMap<>();
      general.put("Format", "Matroska");
      Map<StreamKind, List<Map<String, String>>> snapshot = new EnumMap<>(StreamKind.class);
      snapshot.put(StreamKind.General, new ArrayList<>(List.of(general)));

      MediaInfoBufferReader.addFileMetadata(snapshot, file);

      Map<String, String> enrichedGeneral = snapshot.get(StreamKind.General).get(0);
      assertThat(enrichedGeneral.get("CompleteName")).isEqualTo(file.toAbsolutePath().toString());
      assertThat(enrichedGeneral.get("FolderName")).isEqualTo(file.toAbsolutePath().getParent().toString());
      assertThat(enrichedGeneral.get("FileNameExtension")).isEqualTo(file.getFileName().toString());
      assertThat(enrichedGeneral.get("FileName")).isEqualTo(FilenameUtils.getBaseName(file.getFileName().toString()));
      assertThat(enrichedGeneral.get("FileExtension")).isEqualTo("mkv");
      assertThat(enrichedGeneral).containsKeys("File_Modified_Date", "File_Modified_Date_Local");
    }
    finally {
      Files.deleteIfExists(file);
    }
  }

  private static MediaInfoBufferReader.BufferReader source(long fileSize, List<Long> positions) {
    return (position, buffer, offset, length) -> {
      positions.add(position);
      int bytesRead = (int) Math.min(length, fileSize - position);
      if (bytesRead > 0) {
        Arrays.fill(buffer, offset, offset + bytesRead, (byte) 1);
      }
      return bytesRead;
    };
  }

  private static class TestMediaInfo extends MediaInfo {
    private final Deque<Long>   seekRequests          = new ArrayDeque<>();
    private final List<Long>    initOffsets           = new ArrayList<>();
    private final List<Integer> continueSizes         = new ArrayList<>();
    private int                 continueCount;
    private int                 finalizeCount;
    private int                 finalizeAfterContinue = Integer.MAX_VALUE;
    private boolean             initSucceeds          = true;
    private boolean             finalizeThrows;

    @Override
    public boolean openBufferInit(long length, long offset) {
      initOffsets.add(offset);
      return initSucceeds;
    }

    @Override
    public int openBufferContinue(byte[] buffer, int size) {
      continueCount++;
      continueSizes.add(size);
      return continueCount >= finalizeAfterContinue ? 8 : 0;
    }

    @Override
    public long openBufferContinueGoToGet() {
      Long nextPosition = seekRequests.poll();
      return nextPosition == null ? -1 : nextPosition;
    }

    @Override
    public int openBufferFinalize() {
      finalizeCount++;
      if (finalizeThrows) {
        throw new IllegalStateException("finalize failed");
      }
      return 0;
    }

    @Override
    public Map<StreamKind, List<Map<String, String>>> snapshot() {
      return Map.of(StreamKind.General, List.of(Map.of("Format", "test")));
    }

    @Override
    public void close() {
      // no native resources
    }
  }
}
