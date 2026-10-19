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
package org.tinymediamanager.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * The class {@link PostProcessExecutorTest} tests the helper methods of the {@link PostProcessExecutor}.
 *
 * @author Manuel Laggner
 */
public class PostProcessExecutorTest {

  @Test
  public void blankProgramPathStaysUnchanged() {
    // scripting mode (no program set) must not be turned into an absolute path
    assertThat(PostProcessExecutor.adaptProgramPath("")).isEmpty();
    assertThat(PostProcessExecutor.adaptProgramPath(null)).isNull();
    assertThat(PostProcessExecutor.adaptProgramPath("  ")).isEqualTo("  ");
  }

  @Test
  public void relativeProgramPathStaysUnchanged() {
    // relative paths are resolved by the shell against the working directory
    assertThat(PostProcessExecutor.adaptProgramPath("script.sh")).isEqualTo("script.sh");
    assertThat(PostProcessExecutor.adaptProgramPath("./script.sh")).isEqualTo("./script.sh");
  }
}
