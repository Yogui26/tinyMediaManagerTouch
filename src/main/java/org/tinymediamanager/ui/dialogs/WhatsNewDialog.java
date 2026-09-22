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
package org.tinymediamanager.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.TmmUIScaling;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.label.LinkLabel;

import com.formdev.flatlaf.util.UIScale;

import net.miginfocom.swing.MigLayout;

/**
 * The class WhatsNewDialog. Used to show the user a list of changelogs after each upgrade
 * 
 * @author Manuel Laggner
 */
public class WhatsNewDialog extends TmmDialog {
  private static final Logger LOGGER = LoggerFactory.getLogger(WhatsNewDialog.class);

  public WhatsNewDialog(String changelog) {
    // includes the dialog's name to force the dialog to be centered above the main window
    super(MainWindow.getInstance(), TmmResourceBundle.getString("whatsnew.title"), "whatsnewdialog"); // includes the dialog's name in the settings

    {
      JPanel panelContent = new JPanel();
      getContentPane().add(panelContent, BorderLayout.CENTER);
      panelContent.setLayout(new MigLayout("", "[0:700lp,grow]", "[0:400lp,grow][]"));

      JScrollPane scrollPane = new NoBorderScrollPane();
      panelContent.add(scrollPane, "cell 0 0,grow,wmin 0,hmin 0");

      JTextPane textPane = new JTextPane();
      TmmUIScaling.applyChangelogFont(textPane);
      scrollPane.setViewportView(textPane);

      textPane.setContentType("text/html");
      textPane.setText(prepareTextAsHtml(changelog));
      textPane.setEditable(false);
      textPane.setCaretPosition(0);
      textPane.addHyperlinkListener(hle -> {
        if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
          try {
            TmmUIHelper.browseUrl(hle.getURL().toString());
          }
          catch (Exception e) {
            LOGGER.error("Could not open '{}' in browser - '{}'", hle.getURL(), e.getMessage());
          }
        }
      });

      JLabel lblHint = new JLabel(TmmResourceBundle.getString("whatsnew.hint"));
      panelContent.add(lblHint, "flowx,cell 0 1");

      LinkLabel lblLink = new LinkLabel("https://www.tinymediamanager.org");
      lblLink.addActionListener(arg0 -> {
        try {
          TmmUIHelper.browseUrl("https://www.tinymediamanager.org/changelog/");
        }
        catch (Exception ignored) {
          // ignored
        }
      });
      panelContent.add(lblLink, "cell 0 1, growx");
    }
    {
      JButton btnClose = new JButton(TmmResourceBundle.getString("Button.close"));
      btnClose.addActionListener(arg0 -> setVisible(false));
      addDefaultButton(btnClose);
    }
  }

  @Override
  public Dimension getPreferredSize() {
    // Prefer a scaled window, but keep the Close button on screen at large zoom levels.
    Rectangle screen = getGraphicsConfiguration().getBounds();
    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
    // Leave room for native decorations, whose insets may be unknown until the window is shown.
    int availableWidth = (int) ((screen.width - insets.left - insets.right) * 0.9);
    int availableHeight = (int) ((screen.height - insets.top - insets.bottom) * 0.9);
    return new Dimension(Math.min(UIScale.scale(800), availableWidth), Math.min(UIScale.scale(600), availableHeight));
  }

  private String prepareTextAsHtml(String originalText) {
    // mask tags
    originalText = originalText.replace("<", "&lt;").replace(">", "&gt;");

    // links
    Pattern pattern = Pattern.compile("(http[s]?://.*?)[\\n\\r\\s)]");
    Matcher matcher = pattern.matcher(originalText);
    while (matcher.find()) {
      originalText = originalText.replace(matcher.group(1), "<a href=\"" + matcher.group(1) + "\">" + matcher.group(1) + "</a>");
    }

    // issues
    pattern = Pattern.compile("(#\\d{3,5})[\\n\\r\\s)]");
    matcher = pattern.matcher(originalText);
    while (matcher.find()) {
      originalText = originalText.replace(matcher.group(1), "<a href=\"https://gitlab.com/tinyMediaManager/tinyMediaManager/-/issues/"
          + matcher.group(1).replace("#", "") + "\">" + matcher.group(1) + "</a>");
    }

    // set the foreground color of the content
    Color foreground = UIManager.getColor("TextPane.foreground");
    String color = HexFormat.of().toHexDigits(foreground.getRed(), 2) + HexFormat.of().toHexDigits(foreground.getGreen(), 2)
        + HexFormat.of().toHexDigits(foreground.getBlue(), 2);

    return "<html><pre style=\"color: #" + color + "\">" + originalText + "</pre><html>";
  }

  public static void showChangelog() {
    try {
      final String changelog = Utils.readFileToString(Paths.get("changelog.txt"));
      if (StringUtils.isNotBlank(changelog)) {
        EventQueue.invokeLater(() -> {
          WhatsNewDialog dialog = new WhatsNewDialog(changelog);
          dialog.setVisible(true);
        });
      }
    }
    catch (IOException e) {
      // no file found
      LOGGER.warn("Could not display changelog - '{}'", e.getMessage());
    }
  }
}
