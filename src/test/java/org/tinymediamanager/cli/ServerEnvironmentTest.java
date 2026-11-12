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
package org.tinymediamanager.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * The class {@link ServerEnvironmentTest} verifies the parsing of the environment based server configuration.
 */
public class ServerEnvironmentTest {

  /**
   * Tests the splitting of data source lists.
   */
  @Test
  public void splitsPaths() {
    assertThat(ServerEnvironment.splitPaths(null)).isEmpty();
    assertThat(ServerEnvironment.splitPaths("  ")).isEmpty();
    assertThat(ServerEnvironment.splitPaths("/media/movies")).containsExactly("/media/movies");
    assertThat(ServerEnvironment.splitPaths(" /media/movies ;/media/My Films;;/media/movies")).containsExactly("/media/movies", "/media/My Films");
  }

  /**
   * Tests the parsing of the port.
   */
  @Test
  public void parsesPort() {
    assertThat(ServerEnvironment.parsePort(null)).isNull();
    assertThat(ServerEnvironment.parsePort("7878")).isEqualTo(7878);
    assertThat(ServerEnvironment.parsePort(" 8080 ")).isEqualTo(8080);
    assertThat(ServerEnvironment.parsePort("0")).isNull();
    assertThat(ServerEnvironment.parsePort("70000")).isNull();
    assertThat(ServerEnvironment.parsePort("abc")).isNull();
  }
}
