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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * The class {@link AbstractRestHandlerTest} verifies the request parsing and paging helpers of the v2 HTTP API.
 */
public class AbstractRestHandlerTest {

  /**
   * Tests that the path after the context path is split into its segments.
   */
  @Test
  public void parsesPathSegments() {
    assertThat(AbstractRestHandler.parsePathSegments("/api/v2/movies", "/api/v2/movies")).isEmpty();
    assertThat(AbstractRestHandler.parsePathSegments("/api/v2/movies/", "/api/v2/movies")).isEmpty();
    assertThat(AbstractRestHandler.parsePathSegments("/api/v2/movies/abc/artwork/poster", "/api/v2/movies")).containsExactly("abc", "artwork",
        "poster");
    assertThat(AbstractRestHandler.parsePathSegments("/somewhere/else", "/api/v2/movies")).isEmpty();
  }

  /**
   * Tests that query parameters are decoded.
   */
  @Test
  public void parsesQuery() {
    Map<String, String> query = AbstractRestHandler.parseQuery("q=the%20matrix&sort=year&flag&empty=");

    assertThat(query).containsEntry("q", "the matrix").containsEntry("sort", "year").containsEntry("flag", "").containsEntry("empty", "");
    assertThat(AbstractRestHandler.parseQuery(null)).isEmpty();
    assertThat(AbstractRestHandler.parseQuery("")).isEmpty();
  }

  /**
   * Tests that numeric parameters are clamped and invalid ones are rejected.
   */
  @Test
  public void parsesAndClampsNumbers() {
    assertThat(AbstractRestHandler.parseInt(Map.of(), "limit", 100, 1, 500)).isEqualTo(100);
    assertThat(AbstractRestHandler.parseInt(Map.of("limit", "9999"), "limit", 100, 1, 500)).isEqualTo(500);
    assertThat(AbstractRestHandler.parseInt(Map.of("limit", "-3"), "limit", 100, 1, 500)).isEqualTo(1);
    assertThatThrownBy(() -> AbstractRestHandler.parseInt(Map.of("limit", "abc"), "limit", 100, 1, 500))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /**
   * Tests the paging of list responses including out-of-range offsets.
   */
  @Test
  public void createsPages() {
    List<Integer> items = List.of(1, 2, 3, 4, 5);

    AbstractRestHandler.Page<Integer> page = AbstractRestHandler.createPage(items, Map.of("offset", "1", "limit", "2"));
    assertThat(page.total).isEqualTo(5);
    assertThat(page.offset).isEqualTo(1);
    assertThat(page.limit).isEqualTo(2);
    assertThat(page.items).containsExactly(2, 3);

    assertThat(AbstractRestHandler.createPage(items, Map.of("offset", "4")).items).containsExactly(5);
    assertThat(AbstractRestHandler.createPage(items, Map.of("offset", "50")).items).isEmpty();
    assertThat(AbstractRestHandler.createPage(items, Map.of("offset", String.valueOf(Integer.MAX_VALUE), "limit", "500")).items).isEmpty();
  }

  /**
   * Tests the UUID parsing and the image content types.
   */
  @Test
  public void parsesUuidAndContentTypes() {
    assertThat(AbstractRestHandler.parseUuid("not-a-uuid")).isNull();
    assertThat(AbstractRestHandler.parseUuid("123e4567-e89b-12d3-a456-426614174000")).isNotNull();
    assertThat(AbstractRestHandler.imageContentType("poster.PNG")).isEqualTo("image/png");
    assertThat(AbstractRestHandler.imageContentType("fanart.jpg")).isEqualTo("image/jpeg");
  }
}
