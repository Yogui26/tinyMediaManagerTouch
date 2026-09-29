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
package org.tinymediamanager.ui.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tinymediamanager.core.Settings;

import com.formdev.flatlaf.util.UIScale;

/** Verifies that the interface selector saves changes without applying pending zoom. */
public class UiSettingsPanelTest {
  /** Isolated settings folder for the UI test. */
  @ClassRule
  public static TemporaryFolder folder = new TemporaryFolder();

  /** Selecting a percentage must save it even when no other setting changes. */
  @Test
  public void savesSelectionForNextRestart() throws Exception {
    System.setProperty("tmm.contentfolder", folder.getRoot().getAbsolutePath());
    float activeZoom = UIScale.getZoomFactor();
    SwingUtilities.invokeAndWait(() -> {
      UiSettingsPanel panel = new UiSettingsPanel();
      JComboBox<?> selector = findSelector(panel);
      assertNotNull(selector);
      selector.setSelectedItem(150);
    });
    // checkChanges is queued on the EDT by the selector's listener.
    SwingUtilities.invokeAndWait(() -> {
      assertEquals(150, Settings.getInstance().getUiScalePercent());
      assertEquals(activeZoom, UIScale.getZoomFactor(), 0.001f);
    });
  }

  /** Finds the percentage selector through the actual settings component tree. */
  private static JComboBox<?> findSelector(Container container) {
    for (Component component : container.getComponents()) {
      if (component instanceof JComboBox<?> combo && combo.getItemCount() == Settings.UI_SCALE_PERCENTAGES.size()
          && Integer.valueOf(100).equals(combo.getItemAt(0))) {
        return combo;
      }
      if (component instanceof Container child) {
        JComboBox<?> combo = findSelector(child);
        if (combo != null) {
          return combo;
        }
      }
    }
    return null;
  }
}
