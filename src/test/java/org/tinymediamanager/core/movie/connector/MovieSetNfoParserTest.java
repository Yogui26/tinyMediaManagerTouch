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

package org.tinymediamanager.core.movie.connector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MovieSetNfoParserTest {

  @Test
  public void testMultilineFieldsPreserveLineBreaks() throws Exception {
    MovieSetNfoParser parser = MovieSetNfoParser.parseNfo("<collection><title>foo</title>" //
        + "<plot>plot line 1\nplot line 2</plot>" //
        + "<user_note>note line 1\nnote line 2</user_note>" //
        + "</collection>");

    assertThat(parser.plot).isEqualTo("plot line 1\nplot line 2");
    assertThat(parser.userNote).isEqualTo("note line 1\nnote line 2");
  }
}
