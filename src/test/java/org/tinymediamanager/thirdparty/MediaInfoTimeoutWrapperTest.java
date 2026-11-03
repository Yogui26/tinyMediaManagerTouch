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

import org.junit.Test;

public class MediaInfoTimeoutWrapperTest {

  @Test
  public void testDefaultMaxThreads() {
    assertThat(MediaInfoTimeoutWrapper.parseMaxThreads(null)).isEqualTo(4);
    assertThat(MediaInfoTimeoutWrapper.parseMaxThreads("")).isEqualTo(4);
    assertThat(MediaInfoTimeoutWrapper.parseMaxThreads("  ")).isEqualTo(4);
  }

  @Test
  public void testConfiguredMaxThreads() {
    assertThat(MediaInfoTimeoutWrapper.parseMaxThreads("1")).isEqualTo(1);
    assertThat(MediaInfoTimeoutWrapper.parseMaxThreads(" 8 ")).isEqualTo(8);
  }

  @Test
  public void testInvalidMaxThreadsFallsBackToDefault() {
    assertThat(MediaInfoTimeoutWrapper.parseMaxThreads("0")).isEqualTo(4);
    assertThat(MediaInfoTimeoutWrapper.parseMaxThreads("-1")).isEqualTo(4);
    assertThat(MediaInfoTimeoutWrapper.parseMaxThreads("not-a-number")).isEqualTo(4);
  }

  @Test
  public void testMaxThreadsIsCapped() {
    assertThat(MediaInfoTimeoutWrapper.parseMaxThreads("999")).isEqualTo(16);
  }
}
