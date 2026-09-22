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
package org.tinymediamanager.ui.images;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import com.formdev.flatlaf.util.MultiResolutionImageSupport;
import com.formdev.flatlaf.util.UIScale;

/**
 * Scales a fixed-size icon without changing its original SVG rendering or logical dimensions.
 */
public class TmmScaledIcon extends ImageIcon {
  private final ImageIcon delegate;

  /**
   * @param delegate
   *          the icon rendered at its original size
   */
  public TmmScaledIcon(ImageIcon delegate) {
    this.delegate = delegate;
  }

  @Override
  public int getIconWidth() {
    return UIScale.scale(delegate.getIconWidth());
  }

  @Override
  public int getIconHeight() {
    return UIScale.scale(delegate.getIconHeight());
  }

  @Override
  public void paintIcon(Component component, Graphics graphics, int x, int y) {
    Graphics2D g = (Graphics2D) graphics.create();
    try {
      g.translate(x, y);
      UIScale.scaleGraphics(g);
      delegate.paintIcon(component, g, 0, 0);
    }
    finally {
      g.dispose();
    }
  }

  @Override
  public Image getImage() {
    int width = Math.max(1, getIconWidth());
    int height = Math.max(1, getIconHeight());
    float scale = UIScale.getUserScaleFactor();
    // FlatLaf preserves these lazily rendered variants when creating disabled icons.
    return MultiResolutionImageSupport.create(0, new Dimension[] { new Dimension(width, height), new Dimension(width * 2, height * 2) }, size -> {
      BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = image.createGraphics();
      try {
        graphics.scale(size.width / (double) width * scale, size.height / (double) height * scale);
        delegate.paintIcon(null, graphics, 0, 0);
      }
      finally {
        graphics.dispose();
      }
      return image;
    });
  }
}
