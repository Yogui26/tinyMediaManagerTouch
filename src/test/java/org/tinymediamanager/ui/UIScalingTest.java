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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.MultiResolutionImage;
import java.awt.image.RGBImageFilter;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTextPane;
import javax.swing.text.GlyphView;
import javax.swing.text.View;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import org.junit.Test;
import org.tinymediamanager.ui.components.toolbar.ToolbarButton;
import org.tinymediamanager.ui.images.TmmSvgIcon;
import org.tinymediamanager.ui.plaf.dark.TmmDarkLaf;
import org.tinymediamanager.ui.plaf.light.TmmLightLaf;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.UIScale;
import com.formdev.flatlaf.util.GrayFilter;
import com.kitfox.svg.app.beans.SVGIcon;

/** Tests rendered artwork, controls and fonts at fractional zoom levels. */
public class UIScalingTest {
  /** Fixed SVGs must enlarge their artwork as well as their allocated space. */
  @Test
  public void scalesArtworkAndButtonsWithoutChangingBaseline() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FlatLaf.setup(new TmmLightLaf());
      UIScale.setZoomFactor(1);
      TmmUIScaling.applyFont(new FontUIResource("Dialog", Font.PLAIN, 12));
      try {
        TmmSvgIcon original = new TmmSvgIcon(IconManager.class.getResource("images/svg/icn_refresh.svg").toURI());
        original.setPreferredSize(new Dimension(36, 36));
        original.setAutosize(SVGIcon.AUTOSIZE_NONE);
        Icon icon = IconManager.TOOLBAR_REFRESH;
        BufferedImage baseline = render(original);
        assertImagesEqual(baseline, render(icon));
        Rectangle originalBounds = bounds(baseline);
        ToolbarButton baselineButton = new ToolbarButton(icon, IconManager.TOOLBAR_REFRESH_HOVER);
        Dimension originalButtonSize = baselineButton.getPreferredSize();

        for (float zoom : new float[] { 1, 1.25f, 1.5f, 1.75f, 2, 2.5f, 3 }) {
          UIScale.setZoomFactor(zoom);
          ToolbarButton button = new ToolbarButton(icon, IconManager.TOOLBAR_REFRESH_HOVER);
          assertEquals(Math.round(original.getIconWidth() * zoom), icon.getIconWidth());
          assertEquals(Math.round(original.getIconHeight() * zoom), icon.getIconHeight());
          Rectangle actual = bounds(render(icon));
          assertEquals(originalBounds.width * zoom, actual.width, 2);
          assertEquals(originalBounds.height * zoom, actual.height, 2);
          assertEquals(originalButtonSize.width * zoom, button.getPreferredSize().width, 2);
          assertEquals(originalButtonSize.height * zoom, button.getPreferredSize().height, 2);
          for (Icon state : new Icon[] { button.getIcon(), button.getRolloverIcon(), button.getPressedIcon(), button.getDisabledIcon() }) {
            assertNotNull(state);
            assertEquals(icon.getIconWidth(), state.getIconWidth());
            assertEquals(icon.getIconHeight(), state.getIconHeight());
            assertFalse(bounds(render(state)).isEmpty());
          }
          assertEquals(Math.round(12 * zoom), UIManager.getFont("defaultFont").getSize());
        }
      }
      catch (java.net.URISyntaxException e) {
        throw new AssertionError(e);
      }
      finally {
        UIScale.setZoomFactor(1);
      }
    });
  }

  /** Saved base fonts remain unscaled and refreshes do not compound zoom. */
  @Test
  public void preservesZoomAcrossFontAndThemeChanges() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FlatLaf.setup(new TmmLightLaf());
      UIScale.setZoomFactor(1);
      TmmUIScaling.applyFont(new FontUIResource("Dialog", Font.PLAIN, 12));
      UIScale.setZoomFactor(1.5f);
      try {
        for (int i = 0; i < 3; i++) {
          FlatLaf.setup(i % 2 == 0 ? new TmmDarkLaf() : new TmmLightLaf());
          TmmUIScaling.applyFont(new FontUIResource("Dialog", Font.PLAIN, 16));
          TmmUIHelper.updateUI();
          assertEquals(24, UIManager.getFont("defaultFont").getSize());
          assertEquals(1.5f, UIScale.getZoomFactor(), 0.001f);
          TmmUIScaling.applyFont(new FontUIResource("Dialog", Font.PLAIN, 12));
          TmmUIHelper.updateUI();
          assertEquals(18, UIManager.getFont("defaultFont").getSize());
        }
      }
      finally {
        UIScale.setZoomFactor(1);
      }
    });
  }

  /** HTML must paint with the scaled font, including links, rather than its default CSS size. */
  @Test
  public void scalesRenderedChangelogText() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FlatLaf.setup(new TmmLightLaf());
      UIScale.setZoomFactor(1);
      TmmUIScaling.applyFont(new FontUIResource("Dialog", Font.PLAIN, 12));
      try {
        for (float zoom : new float[] { 1, 1.25f, 1.5f, 1.75f, 2, 2.5f, 3 }) {
          UIScale.setZoomFactor(zoom);
          JTextPane pane = new JTextPane();
          TmmUIScaling.applyChangelogFont(pane);
          pane.setContentType("text/html");
          pane.setText("<html><pre>Release notes <a href='https://example.com'>link</a></pre></html>");
          pane.setSize(1000, 300);
          BufferedImage image = new BufferedImage(1000, 300, BufferedImage.TYPE_INT_ARGB);
          Graphics2D g = image.createGraphics();
          try {
            pane.paint(g);
          }
          finally {
            g.dispose();
          }
          assertEquals(13 * zoom, pane.getFont().getSize2D(), 0.6f);
          assertTrue(assertRenderedFont(pane.getUI().getRootView(pane), pane.getFont().getSize2D()) >= 2);
        }
      }
      finally {
        UIScale.setZoomFactor(1);
      }
    });
  }

  /** Checks actual visible glyphs, excluding the HTML document's hidden initial element. */
  private static int assertRenderedFont(View view, float expected) {
    int checked = 0;
    if (view instanceof GlyphView glyph && view.getStartOffset() > 0) {
      assertEquals(expected, glyph.getFont().getSize2D(), 1f);
      checked++;
    }
    for (int i = 0; i < view.getViewCount(); i++) {
      checked += assertRenderedFont(view.getView(i), expected);
    }
    return checked;
  }

  /** Disabled artwork must be rasterized at device resolution rather than upscaled from a small bitmap. */
  @Test
  public void retainsDisabledIconResolution() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      FlatLaf.setup(new TmmLightLaf());
      UIScale.setZoomFactor(1);
      TmmUIScaling.applyFont(new FontUIResource("Dialog", Font.PLAIN, 12));
      try {
        for (float zoom : new float[] { 1, 1.5f, 3 }) {
          UIScale.setZoomFactor(zoom);
          Icon icon = IconManager.TOOLBAR_REFRESH;
          ToolbarButton button = new ToolbarButton(icon, IconManager.TOOLBAR_REFRESH_HOVER);
          Image disabled = ((ImageIcon) button.getDisabledIcon()).getImage();
          assertTrue(disabled instanceof MultiResolutionImage);
          for (int deviceScale : new int[] { 1, 2, 3 }) {
            int width = icon.getIconWidth() * deviceScale;
            int height = icon.getIconHeight() * deviceScale;
            Image variant = ((MultiResolutionImage) disabled).getResolutionVariant(width, height);
            BufferedImage actual = render(new ImageIcon(variant));
            assertEquals(width, actual.getWidth());
            assertEquals(height, actual.getHeight());

            BufferedImage expected = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = expected.createGraphics();
            try {
              g.scale(deviceScale, deviceScale);
              icon.paintIcon(null, g, 0, 0);
            }
            finally {
              g.dispose();
            }
            RGBImageFilter filter = UIManager.get("Component.grayFilter") instanceof RGBImageFilter gray ? gray : GrayFilter.createDisabledIconFilter(false);
            for (int y = 0; y < height; y++) {
              for (int x = 0; x < width; x++) {
                expected.setRGB(x, y, filter.filterRGB(x, y, expected.getRGB(x, y)));
              }
            }
            assertImagesEqual(render(new ImageIcon(expected)), actual);
          }
        }
      }
      finally {
        UIScale.setZoomFactor(1);
      }
    });
  }

  /** Rasterizes an icon on transparency to verify its actual painted bounds. */
  private static BufferedImage render(Icon icon) {
    BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    try {
      icon.paintIcon(null, g, 0, 0);
    }
    finally {
      g.dispose();
    }
    return image;
  }

  /** Finds nontransparent artwork rather than only the icon's declared size. */
  private static Rectangle bounds(BufferedImage image) {
    int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if ((image.getRGB(x, y) >>> 24) > 16) {
          left = Math.min(left, x);
          top = Math.min(top, y);
          right = Math.max(right, x);
          bottom = Math.max(bottom, y);
        }
      }
    }
    return new Rectangle(left, top, Math.max(0, right - left + 1), Math.max(0, bottom - top + 1));
  }

  /** Verifies that 100% zoom preserves every rendered pixel. */
  private static void assertImagesEqual(BufferedImage expected, BufferedImage actual) {
    assertEquals(expected.getWidth(), actual.getWidth());
    assertEquals(expected.getHeight(), actual.getHeight());
    for (int y = 0; y < expected.getHeight(); y++) {
      for (int x = 0; x < expected.getWidth(); x++) {
        assertEquals(expected.getRGB(x, y), actual.getRGB(x, y));
      }
    }
  }
}
