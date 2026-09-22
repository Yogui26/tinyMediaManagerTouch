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
package org.tinymediamanager.ui.wizard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.ui.TmmUIScaling;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.UIScale;

/** Checks font changes through the actual setup wizard controls. */
public class UiSettingsPanelLiteTest {
  /** Isolates the singleton settings from personal configuration. */
  @ClassRule
  public static TemporaryFolder folder = new TemporaryFolder();

  /** Changing the base font must retain the active application zoom. */
  @Test
  public void preservesZoomWhenChangingFont() throws Exception {
    System.setProperty("tmm.contentfolder", folder.getRoot().getAbsolutePath());
    SwingUtilities.invokeAndWait(() -> {
      // The application text-area delegate queries native shortcut keys, unavailable in headless tests.
      FlatLaf.setup(new FlatLightLaf());
      UIScale.setZoomFactor(1);
      TmmUIScaling.applyFont(new FontUIResource("Dialog", Font.PLAIN, 12));
      UIScale.setZoomFactor(1.5f);
      try {
        UiSettingsPanelLite panel = new UiSettingsPanelLite();
        JComboBox<?> fontSize = findFontSize(panel);
        assertNotNull(fontSize);
        fontSize.setSelectedItem(16);
        assertEquals(16, Settings.getInstance().getFontSize());
        assertEquals(24, UIManager.getFont("defaultFont").getSize());
        assertEquals(1.5f, UIScale.getZoomFactor(), 0.001f);
      }
      finally {
        UIScale.setZoomFactor(1);
      }
    });
  }

  /** Finds the wizard's numeric font-size selector in its component tree. */
  private static JComboBox<?> findFontSize(Container container) {
    for (Component component : container.getComponents()) {
      if (component instanceof JComboBox<?> combo && combo.getItemCount() > 0 && Integer.valueOf(12).equals(combo.getItemAt(0))) {
        return combo;
      }
      if (component instanceof Container child) {
        JComboBox<?> combo = findFontSize(child);
        if (combo != null) {
          return combo;
        }
      }
    }
    return null;
  }
}
