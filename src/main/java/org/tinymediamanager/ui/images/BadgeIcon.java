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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * The class {@link BadgeIcon} decorates a base {@link Icon} with a red notification dot at its top right corner - the way the IDEs announce that
 * something is waiting for you.
 * <p>
 * The dot never counts: one message or a hundred look exactly the same, because the number was always too small to read on a toolbar icon. What
 * matters is that the dot is impossible to overlook, so it is sized to a good half of the decorated icon - but never bigger than the icon can take. A
 * ring in the background color of the surrounding component separates it from the artwork.
 * <p>
 * The dot is painted as an overlay, so the icon always reports the dimensions of the decorated icon and nothing shifts around when it appears. It
 * does reach a little past that corner to sit on the visual corner of the artwork - the component showing the icon has room for that, the decorated
 * icon itself never moves.
 *
 * @author Manuel Laggner
 */
public class BadgeIcon implements Icon {
  /**
   * the inset the dot keeps from the corner of the icon - at the same time the width of the separating ring around it
   */
  private static final int    INSET           = 1;

  /**
   * the pixels the dot reaches past the corner of the icon: icons leave some dead space around their artwork, so a dot which stays inside the box
   * never sits on the visual corner - the surrounding component has enough room for this
   */
  private static final int    CORNER_OVERHANG = 2;

  /**
   * the fraction of the decorated icon the dot is sized to
   */
  private static final double DOT_FACTOR      = 0.3;

  /**
   * the smallest dot we paint - anything below is barely noticeable
   */
  private static final int    MIN_DOT_SIZE    = 7;

  /**
   * the biggest dot we paint - a bigger one would swallow the whole icon
   */
  private static final int    MAX_DOT_SIZE    = 9;

  private static final Color  DOT_COLOR       = Color.RED;

  private volatile Icon       baseIcon;

  /**
   * the number of waiting messages - the dot appears as soon as this is greater than zero
   */
  private volatile int        count;

  /**
   * Creates a new {@link BadgeIcon} without a visible dot.
   *
   * @param baseIcon
   *          the icon to decorate
   */
  public BadgeIcon(Icon baseIcon) {
    this(baseIcon, 0);
  }

  /**
   * Creates a new {@link BadgeIcon} showing the dot for the given number of waiting messages.
   *
   * @param baseIcon
   *          the icon to decorate
   * @param count
   *          the number of waiting messages; zero hides the dot
   */
  public BadgeIcon(Icon baseIcon, int count) {
    this.baseIcon = baseIcon;
    this.count = count;
  }

  /**
   * Update base icon and number of waiting messages at once.
   *
   * @param newBaseIcon
   *          the icon to decorate; ignored when {@code null}
   * @param newCount
   *          the number of waiting messages; zero hides the dot
   */
  public void update(Icon newBaseIcon, int newCount) {
    if (newBaseIcon != null) {
      baseIcon = newBaseIcon;
    }
    count = newCount;
  }

  /**
   * Update only the number of waiting messages, keeping the base icon.
   *
   * @param newCount
   *          the number of waiting messages; zero hides the dot
   */
  public void setCount(int newCount) {
    count = newCount;
  }

  /**
   * Get the currently displayed number of waiting messages.
   *
   * @return the number of waiting messages
   */
  public int getCount() {
    return count;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (baseIcon != null) {
      baseIcon.paintIcon(c, g, x, y);
    }

    if (count <= 0 || getIconWidth() <= 0 || getIconHeight() <= 0) {
      // nothing decorated, nothing to announce
      return;
    }

    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      Rectangle dot = dotRect(getIconWidth());

      // the dot is placed relative to the icon, so it has to follow the position the icon was painted at
      g2.setColor(ringColor(c, g2));
      g2.fillRect(x + dot.x - INSET, y + dot.y - INSET, dot.width + 2 * INSET, dot.height + 2 * INSET);

      g2.setColor(DOT_COLOR);
      g2.fillOval(x + dot.x, y + dot.y, dot.width, dot.height);
    }
    finally {
      g2.dispose();
    }
  }

  @Override
  public int getIconWidth() {
    return baseIcon == null ? 0 : baseIcon.getIconWidth();
  }

  @Override
  public int getIconHeight() {
    return baseIcon == null ? 0 : baseIcon.getIconHeight();
  }

  /**
   * The dot as it is painted for an icon of the given width - package private for testing.
   *
   * @param iconWidth
   *          the width of the decorated icon
   * @return position and size of the dot, relative to the top left corner of the icon
   */
  static Rectangle dotRect(int iconWidth) {
    int side = dotSize(iconWidth);
    // anchored at the top right corner, reaching a little past it to sit on the visual corner of the artwork
    return new Rectangle(Math.max(INSET, iconWidth - side + CORNER_OVERHANG), INSET, side, side);
  }

  /**
   * The edge length of the dot for an icon of the given width - package private for testing.
   *
   * @param iconWidth
   *          the width of the decorated icon
   * @return the edge length of the dot
   */
  static int dotSize(int iconWidth) {
    int side = (int) Math.round(Math.max(iconWidth, 1) * DOT_FACTOR);
    return Math.min(MAX_DOT_SIZE, Math.max(MIN_DOT_SIZE, side));
  }

  /**
   * The color of the ring which separates the dot from the icon: the background of the first opaque ancestor, so the dot looks like it floats above
   * the icon.
   *
   * @param c
   *          the component the icon is painted on (may be {@code null})
   * @param g2
   *          the graphics the icon is painted on
   * @return the color of the ring
   */
  private Color ringColor(Component c, Graphics2D g2) {
    Component current = c;
    while (current != null) {
      if (current instanceof JComponent component && component.isOpaque()) {
        return component.getBackground();
      }
      current = current.getParent();
    }

    return g2.getBackground();
  }
}
