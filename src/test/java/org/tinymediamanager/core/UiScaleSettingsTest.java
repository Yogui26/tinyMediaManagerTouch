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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Tests backward-compatible persistence of the interface zoom setting. */
public class UiScaleSettingsTest {
  /** Existing configurations retain normal sizing, and supported values round-trip. */
  @Test
  public void defaultsAndRoundTrips() throws Exception {
    Settings settings = AbstractSettings.objectMapper.readValue("{}", Settings.class);
    assertEquals(100, settings.getUiScalePercent());
    for (int percent : Settings.UI_SCALE_PERCENTAGES) {
      settings.setUiScalePercent(percent);
      String json = AbstractSettings.objectMapper.writeValueAsString(settings);
      assertEquals(percent, AbstractSettings.objectMapper.readValue(json, Settings.class).getUiScalePercent());
    }
  }

  /** Malformed ranges cannot make the UI unusable, including field-based JSON loading. */
  @Test
  public void rejectsUnsupportedValues() throws Exception {
    for (int value : new int[] { -1, 0, 99, 151, 400, Integer.MAX_VALUE }) {
      Settings settings = AbstractSettings.objectMapper.readValue("{\"uiScalePercent\":" + value + "}", Settings.class);
      assertEquals(100, settings.getUiScalePercent());
      settings.setUiScalePercent(value);
      assertEquals(100, settings.getUiScalePercent());
    }
  }
}
