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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.BasicTest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;

/**
 * Tests for {@link MediaInfoTimeoutRegistry} and the skip behavior of {@link MediaInfoTimeoutWrapper}.
 * <p>
 * No corrupted media files are needed: a timeout of 0 seconds makes the wrapper's future.get() throw the TimeoutException deterministically, which
 * exercises the record path; the subsequent call must be served from the registry instead of invoking libmediainfo again.
 * </p>
 */
public class MediaInfoTimeoutRegistryTest extends BasicTest {

  private Path mediaFile;

  @Before
  public void setup() throws Exception {
    super.setup();

    mediaFile = getWorkFolder().resolve("movie.mkv");
    Files.write(mediaFile, "dummy".getBytes(StandardCharsets.US_ASCII));
  }

  @Test
  public void timeoutIsRememberedAndSkipped() {
    assertThat(MediaInfoTimeoutRegistry.shouldSkip(mediaFile)).isFalse();

    MediaInfoTimeoutRegistry.recordTimeout(mediaFile);

    assertThat(MediaInfoTimeoutRegistry.shouldSkip(mediaFile)).isTrue();
  }

  @Test
  public void sizeChangeInvalidatesTheEntry() throws Exception {
    MediaInfoTimeoutRegistry.recordTimeout(mediaFile);
    assertThat(MediaInfoTimeoutRegistry.shouldSkip(mediaFile)).isTrue();

    // a re-downloaded / re-muxed file must be examined again
    Files.write(mediaFile, "dummy with a different size".getBytes(StandardCharsets.US_ASCII));

    assertThat(MediaInfoTimeoutRegistry.shouldSkip(mediaFile)).isFalse();
  }

  @Test
  public void registrySurvivesARestart() {
    MediaInfoTimeoutRegistry.recordTimeout(mediaFile);

    // simulate an application restart: drop the memory and read the store again
    MediaInfoTimeoutRegistry.reload();

    assertThat(MediaInfoTimeoutRegistry.shouldSkip(mediaFile)).isTrue();
  }

  @Test
  public void wrapperSkipsTheSecondAnalysisAfterATimeout() throws Exception {
    // a timeout of 0 seconds throws the TimeoutException deterministically - no corrupted file needed
    assertThat(MediaInfoTimeoutWrapper.getSnapshot(mediaFile, 0)).isEmpty();
    assertThat(MediaInfoTimeoutRegistry.shouldSkip(mediaFile)).as("the timeout must have been recorded").isTrue();

    Logger wrapperLogger = (Logger) LoggerFactory.getLogger(MediaInfoTimeoutWrapper.class);
    ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    wrapperLogger.addAppender(appender);

    try {
      long t0 = System.nanoTime();
      assertThat(MediaInfoTimeoutWrapper.getSnapshot(mediaFile)).isEmpty();
      long elapsedMs = (System.nanoTime() - t0) / 1_000_000;

      boolean skippedLogged = appender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("MediaInfo skipped for"));
      assertThat(skippedLogged).as("the second analysis must be served from the registry, not libmediainfo (took %d ms)", elapsedMs).isTrue();
    }
    finally {
      wrapperLogger.detachAppender(appender);
      appender.stop();
    }
  }
}
