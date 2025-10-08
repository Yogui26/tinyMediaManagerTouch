/*
 * Copyright 2012 - 2025 Manuel Laggner
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
package org.tinymediamanager.ui.images;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RGBImageFilter;
import java.net.URL;
import java.util.function.Function;

import javax.swing.ImageIcon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.formdev.flatlaf.util.MultiResolutionImageSupport;
import com.formdev.flatlaf.util.UIScale;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.renderer.PlatformSupport;

/**
 * A convenience class to create SVG icons for tinyMediaManager
 * 
 * @author Manuel Laggner
 */
public class TmmSvgIcon extends ImageIcon {
  protected static final SVGLoader SVG_LOADER = new SVGLoader();

  private URL                      url;
  private Dimension                preferredSize;
  private Dimension                iconSize;

  private SvgColorFilter           colorFilter;
  private SVGDocument              document;
  private boolean                  loadFailed;

  public TmmSvgIcon() {
    this(null);
  }

  public TmmSvgIcon(URL url) {
    this.url = url;
    this.preferredSize = null;
    this.iconSize = null;
  }

  public TmmSvgIcon(URL url, Color color) {
    this(url);
    setColor(color);
  }

  protected void setURL(URL url) {
    this.url = url;

    if (this.document != null) {
      update(true);
    }
  }

  protected URL getUrl() {
    return url;
  }

  public void setPreferredSize(Dimension size) {
    this.preferredSize = size;
    calulateSize();
  }

  public void setPreferredHeight(int height) {
    setPreferredSize(new Dimension(0, height));
  }

  public void setColor(Color newColor) {
    this.setColorFilter(new SvgColorFilter(c -> newColor));

    if (this.document != null) {
      update(true);
    }
  }

  public void setColor(String oldColorCode, Color newColor) {
    Color oldColor = Color.decode(oldColorCode);

    this.setColorFilter(new SvgColorFilter(c -> {
      if (c.equals(oldColor)) {
        return newColor;
      }
      else {
        return c;
      }
    }));
  }

  public SvgColorFilter getColorFilter() {
    return this.colorFilter;
  }

  public TmmSvgIcon setColorFilter(SvgColorFilter colorFilter) {
    this.colorFilter = colorFilter;
    return this;
  }

  private void update() {
    update(false);
  }

  private synchronized void update(boolean force) {
    if ((this.document == null && !this.loadFailed) || force) {
      URL url = this.url;
      if (url == null) {
        this.document = null;
        this.loadFailed = true;
        this.iconSize = null;
        return;
      }

      this.document = loadSVG();
      calulateSize();
      this.loadFailed = document == null;
    }
  }

  /**
   * Loads the SVG document from the URL - default implementation
   * 
   * @return the loaded {@link SVGDocument}
   */
  protected synchronized SVGDocument loadSVG() {
    return SVG_LOADER.load(url);
  }

  public boolean hasFound() {
    update();
    return this.document != null;
  }

  @Override
  public int getIconWidth() {
    update();
    return iconSize != null ? iconSize.width : 16;
  }

  @Override
  public int getIconHeight() {
    update();
    return iconSize != null ? iconSize.height : 16;
  }

  private void calulateSize() {
    SVGDocument document = this.document;
    if (document == null) {
      iconSize = null;
      return;
    }

    int docWidth = (int) document.size().getWidth();
    int docHeight = (int) document.size().getHeight();
    int prefWidth = this.preferredSize != null ? this.preferredSize.width : 0;
    int prefHeight = this.preferredSize != null ? this.preferredSize.height : 0;

    if (prefWidth == 0 && prefHeight == 0) {
      iconSize = new Dimension(docWidth, docHeight);
    }
    else if (prefWidth == 0 && prefHeight > 0) {
      double scale = (double) prefHeight / docHeight;
      iconSize = new Dimension((int) Math.round(docWidth * scale), prefHeight);
    }
    else if (prefWidth > 0 && prefHeight == 0) {
      double scale = (double) prefWidth / docWidth;
      iconSize = new Dimension(prefWidth, (int) Math.round(docHeight * scale));
    }
    else {
      double scaleW = (double) prefWidth / docWidth;
      double scaleH = (double) prefHeight / docHeight;
      double scale = Math.min(scaleW, scaleH);
      iconSize = new Dimension((int) Math.round(docWidth * scale), (int) Math.round(docHeight * scale));
    }
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    update();

    Rectangle clipBounds = g.getClipBounds();

    if (clipBounds == null || clipBounds.intersects(new Rectangle(x, y, this.getIconWidth(), this.getIconHeight()))) {
      RGBImageFilter grayFilter = null;

      SvgColorFilter globalColorFilter = SvgColorFilter.getInstance();
      globalColorFilter.c = c;
      if (this.colorFilter != null) {
        this.colorFilter.c = c;
      }

      Graphics2D g2 = new SvgGraphicsFilter((Graphics2D) g.create(), this.colorFilter, globalColorFilter, grayFilter);

      try {
        setRenderingHints(g2);
        this.paintSvg(g2, x, y);
      }
      finally {
        g2.dispose();
        globalColorFilter.c = null;
        if (this.colorFilter != null) {
          this.colorFilter.c = null;
        }
      }
    }
  }

  private void paintSvg(Graphics2D g, int x, int y) {
    if (this.document == null) {
      this.paintSvgError(g, x, y);
    }
    else {
      g.translate(x, y);
      g.clipRect(0, 0, this.getIconWidth(), this.getIconHeight());
      UIScale.scaleGraphics(g);
      if (this.iconSize != null) {
        Dimension2D svgSize = this.document.size();
        double sx = this.iconSize.width > 0 ? (double) this.iconSize.width / svgSize.getWidth() : (double) 1.0F;
        double sy = this.iconSize.height > 0 ? (double) this.iconSize.height / svgSize.getHeight() : (double) 1.0F;
        if (sx != (double) 1.0F || sy != (double) 1.0F) {
          g.scale(sx, sy);
        }
      }

      try {
        this.document.renderWithPlatform(TmmPlatformSupport.INSTANCE, g, null);
      }
      catch (Exception var9) {
        this.paintSvgError(g, 0, 0);
      }

    }
  }

  private void paintSvgError(Graphics2D g, int x, int y) {
    if (g instanceof SvgGraphicsFilter) {
      ((SvgGraphicsFilter) g).setColorUnfiltered(Color.red);
    }
    else {
      g.setColor(Color.red);
    }

    g.fillRect(x, y, this.getIconWidth(), this.getIconHeight());
  }

  public Image getImage() {
    this.update();
    int iconWidth = this.getIconWidth();
    int iconHeight = this.getIconHeight();
    Dimension[] dimensions = new Dimension[] { new Dimension(iconWidth, iconHeight), new Dimension(iconWidth * 2, iconHeight * 2) };
    Function<Dimension, Image> producer = (size) -> {
      BufferedImage image = new BufferedImage(size.width, size.height, 2);
      Graphics2D g = image.createGraphics();

      try {
        double sx = size.width > 0 ? (double) ((float) size.width / (float) iconWidth) : (double) 1.0F;
        double sy = size.height > 0 ? (double) ((float) size.height / (float) iconHeight) : (double) 1.0F;
        if (sx != (double) 1.0F || sy != (double) 1.0F) {
          g.scale(sx, sy);
        }

        this.paintIcon(null, g, 0, 0);
      }
      finally {
        g.dispose();
      }

      return image;
    };
    return MultiResolutionImageSupport.create(0, dimensions, producer);
  }

  static void setRenderingHints(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
  }

  private static class TmmPlatformSupport implements PlatformSupport {
    private final static TmmPlatformSupport INSTANCE    = new TmmPlatformSupport();

    private final static String             FONT_FAMILY = "Dialog";

    @NotNull
    @Override
    public String fontFamily() {
      return FONT_FAMILY;
    }

    @Override
    public ImageObserver imageObserver() {
      return null;
    }

    @Nullable
    @Override
    public TargetSurface targetSurface() {
      return null;
    }
  }
}
