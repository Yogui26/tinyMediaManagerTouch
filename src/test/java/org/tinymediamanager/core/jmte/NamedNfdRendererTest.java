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
package org.tinymediamanager.core.jmte;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;

import org.junit.Test;

/**
 * The class {@link NamedNfdRendererTest} tests the {@link NamedNfdRenderer}.
 *
 * @author Manuel Laggner
 */
public class NamedNfdRendererTest {
  private final NamedNfdRenderer renderer = new NamedNfdRenderer();

  @Test
  public void rendersStringsInNfd() {
    assertThat(renderer.getName()).isEqualTo("nfd");
    assertThat(renderer.render("Caf\u00e9", null, null, null)).isEqualTo("Cafe\u0301");
  }

  @Test
  public void rendersPathsInNfd() {
    assertThat(renderer.render(Paths.get("Caf\u00e9.mkv"), null, null, null)).isEqualTo("Cafe\u0301.mkv");
  }

  @Test
  public void returnsEmptyStringForNull() {
    assertThat(renderer.render(null, null, null, null)).isEmpty();
  }
}
