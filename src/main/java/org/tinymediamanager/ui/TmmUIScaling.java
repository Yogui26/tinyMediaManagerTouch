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
package org.tinymediamanager.ui;

import java.awt.Font;

import javax.swing.JEditorPane;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.formdev.flatlaf.util.UIScale;

/**
 * Applies saved base fonts while preserving the active application zoom.
 */
public final class TmmUIScaling {
  private TmmUIScaling() {
    // utility class
  }

  /**
   * Applies an unzoomed font without compounding zoom on subsequent theme or font changes.
   *
   * @param font
   *          the saved base font
   */
  public static void applyFont(FontUIResource font) {
    float zoom = UIScale.getZoomFactor();
    UIScale.setZoomFactor(1);
    UIManager.put("defaultFont", font);
    UIScale.getUserScaleFactor();
    UIScale.setZoomFactor(zoom);
  }

  /**
   * Makes HTML changelogs honor the active scaled UI font while retaining monospaced text.
   *
   * @param pane
   *          the changelog pane
   */
  public static void applyChangelogFont(JEditorPane pane) {
    pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    pane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 1)
        .deriveFont(UIManager.getFont("TextPane.font").getSize2D() + UIScale.scale(1f)));
  }
}
