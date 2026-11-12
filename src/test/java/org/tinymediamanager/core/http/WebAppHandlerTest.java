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
package org.tinymediamanager.core.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * The class {@link WebAppHandlerTest} verifies that the web UI handler only ever maps requests into its own resource folder.
 */
public class WebAppHandlerTest {

  /**
   * Tests the mapping of regular request paths.
   */
  @Test
  public void resolvesRegularPaths() {
    assertThat(WebAppHandler.resolveResourcePath("/")).isEqualTo("/webapp/index.html");
    assertThat(WebAppHandler.resolveResourcePath("")).isEqualTo("/webapp/index.html");
    assertThat(WebAppHandler.resolveResourcePath("/assets/index-abc.js")).isEqualTo("/webapp/assets/index-abc.js");
    assertThat(WebAppHandler.resolveResourcePath("/movies/123")).isEqualTo("/webapp/movies/123");
  }

  /**
   * Tests that path traversal attempts and API paths are rejected.
   */
  @Test
  public void rejectsForbiddenPaths() {
    assertThat(WebAppHandler.resolveResourcePath("/../logback.xml")).isNull();
    assertThat(WebAppHandler.resolveResourcePath("/assets/../../secret")).isNull();
    assertThat(WebAppHandler.resolveResourcePath("/assets\\..\\secret")).isNull();
    assertThat(WebAppHandler.resolveResourcePath("//evil")).isNull();
    assertThat(WebAppHandler.resolveResourcePath("/api/unknown")).isNull();
    assertThat(WebAppHandler.resolveResourcePath("/api")).isNull();
  }

  /**
   * Tests the detection of file extensions and content types.
   */
  @Test
  public void detectsExtensionsAndContentTypes() {
    assertThat(WebAppHandler.hasFileExtension("/webapp/movies/123")).isFalse();
    assertThat(WebAppHandler.hasFileExtension("/webapp/assets/app.js")).isTrue();
    assertThat(WebAppHandler.contentType("/webapp/index.html")).startsWith("text/html");
    assertThat(WebAppHandler.contentType("/webapp/assets/app.js")).startsWith("text/javascript");
    assertThat(WebAppHandler.contentType("/webapp/unknown.bin")).isEqualTo("application/octet-stream");
  }
}
