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
package org.tinymediamanager.ui.panels;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.Test;

/**
 * The class {@link MemoryUsagePanelTest} verifies the Xmx-dependent high-usage threshold calculation of {@link MemoryUsagePanel}.
 *
 * @author Manuel Laggner
 */
public class MemoryUsagePanelTest {

  private static long mib(long mebibytes) {
    return mebibytes * 1048576L;
  }

  @Test
  public void testThresholdAnchoredAt512MiB() {
    assertThat(MemoryUsagePanel.calculateHighUsageThreshold(mib(512))).isCloseTo(85.0, offset(0.001));
  }

  @Test
  public void testThresholdAt8GiB() {
    assertThat(MemoryUsagePanel.calculateHighUsageThreshold(mib(8192))).isCloseTo(95.0, offset(0.001));
  }

  @Test
  public void testThresholdBelowAnchor() {
    assertThat(MemoryUsagePanel.calculateHighUsageThreshold(mib(256))).isCloseTo(82.5, offset(0.001));
  }

  @Test
  public void testThresholdAboveAnchor() {
    assertThat(MemoryUsagePanel.calculateHighUsageThreshold(mib(16384))).isCloseTo(97.5, offset(0.001));
  }

  @Test
  public void testThresholdFlooredOnSmallHeaps() {
    // 128 MiB would extrapolate to 80, anything smaller is clamped to the 80% lower bound
    assertThat(MemoryUsagePanel.calculateHighUsageThreshold(mib(128))).isCloseTo(80.0, offset(0.001));
    assertThat(MemoryUsagePanel.calculateHighUsageThreshold(mib(64))).isCloseTo(80.0, offset(0.001));
  }

  @Test
  public void testThresholdCeiledOnHugeHeaps() {
    // 32 GiB would extrapolate to 100, clamped to the 98% upper bound
    assertThat(MemoryUsagePanel.calculateHighUsageThreshold(mib(32768))).isCloseTo(98.0, offset(0.001));
  }

  @Test
  public void testThresholdWithUnboundedMaxHeap() {
    // when no -Xmx is set, Runtime.maxMemory() reports Long.MAX_VALUE; must not overflow the clamp
    assertThat(MemoryUsagePanel.calculateHighUsageThreshold(Long.MAX_VALUE)).isCloseTo(98.0, offset(0.001));
  }

  @Test
  public void testThresholdWithNonPositiveMaxHeap() {
    assertThat(MemoryUsagePanel.calculateHighUsageThreshold(0)).isEqualTo(85.0);
    assertThat(MemoryUsagePanel.calculateHighUsageThreshold(-1)).isEqualTo(85.0);
  }
}
