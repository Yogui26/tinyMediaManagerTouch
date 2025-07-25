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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.image.RGBImageFilter;

import com.formdev.flatlaf.util.Graphics2DProxy;

public class SvgGraphicsFilter extends Graphics2DProxy {
  private final SvgColorFilter colorFilter;
  private final SvgColorFilter globalColorFilter;
  private final RGBImageFilter grayFilter;

  SvgGraphicsFilter(Graphics2D delegate, SvgColorFilter colorFilter, SvgColorFilter globalColorFilter, RGBImageFilter grayFilter) {
    super(delegate);
    this.colorFilter = colorFilter;
    this.globalColorFilter = globalColorFilter;
    this.grayFilter = grayFilter;
  }

  public Graphics create() {
    return new SvgGraphicsFilter((Graphics2D) super.create(), this.colorFilter, this.globalColorFilter, this.grayFilter);
  }

  public Graphics create(int x, int y, int width, int height) {
    return new SvgGraphicsFilter((Graphics2D) super.create(x, y, width, height), this.colorFilter, this.globalColorFilter, this.grayFilter);
  }

  public void setColor(Color c) {
    super.setColor(this.filterColor(c));
  }

  void setColorUnfiltered(Color c) {
    super.setColor(c);
  }

  public void setPaint(Paint paint) {
    if (paint instanceof Color) {
      paint = this.filterColor((Color) paint);
    }
    else if (paint instanceof LinearGradientPaint) {
      LinearGradientPaint oldPaint = (LinearGradientPaint) paint;
      Color[] newColors = this.filterColors(oldPaint.getColors());
      if (newColors != null) {
        paint = new LinearGradientPaint(oldPaint.getStartPoint(), oldPaint.getEndPoint(), oldPaint.getFractions(), newColors,
            oldPaint.getCycleMethod(), oldPaint.getColorSpace(), oldPaint.getTransform());
      }
    }

    super.setPaint(paint);
  }

  private Color filterColor(Color color) {
    if (this.colorFilter != null) {
      Color newColor = this.colorFilter.filter(color);
      color = newColor != color ? newColor : this.globalColorFilter.filter(color);
    }
    else {
      color = this.globalColorFilter.filter(color);
    }

    if (this.grayFilter != null) {
      int oldRGB = color.getRGB();
      int newRGB = this.grayFilter.filterRGB(0, 0, oldRGB);
      color = newRGB != oldRGB ? new Color(newRGB, true) : color;
    }

    return color;
  }

  private Color[] filterColors(Color[] colors) {
    Color[] newColors = new Color[colors.length];
    boolean changed = false;

    for (int i = 0; i < colors.length; ++i) {
      newColors[i] = this.filterColor(colors[i]);
      changed = changed || newColors[i] != colors[i];
    }

    return changed ? newColors : null;
  }
}
