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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * The class {@link BadgeIcon} decorates a base {@link Icon} with a counter badge (a small pill with a number) painted at its top right corner - the
 * place where macOS and Android put their notification badges.
 * <p>
 * The badge is painted as an overlay, so the icon always reports the dimensions of the base icon: swapping the count never changes the layout of the
 * component using it. The size of the badge is capped to the decorated icon, so that small icons stay readable: the counter falls back to
 * {@code 99+}, then to {@code 9+} and finally to a plain dot if the number would still cover too much of the icon. A wider counter grows to the left,
 * so the badge always stays in the corner. The badge is only drawn for two or more messages - a single one is already announced by the decorated icon
 * itself.
 *
 * @author Manuel Laggner
 */
public class BadgeIcon implements Icon {
  /** the lowest count which gets a badge; a single message is already announced by the decorated icon itself */
  private static final int    MIN_COUNT          = 2;

  /** the highest number shown as plain digits; anything above gets rendered as {@code 99+} */
  private static final int    MAX_COUNT          = 99;

  /** the coarser counter used when even {@code 99+} would cover too much of the icon */
  private static final int    SMALL_MAX_COUNT    = 9;

  /** the font size of the badge relative to the font of the painted component */
  private static final double FONT_SCALE         = 0.66;

  /** the horizontal padding around the counter text inside the badge */
  private static final int    HORIZONTAL_PADDING = 2;

  /** the width of the ring which separates the badge from the decorated icon */
  private static final int    RING_WIDTH         = 1;

  /** the distance of the badge from the corner of the icon (to leave room for its ring) */
  private static final int    BADGE_INSET        = RING_WIDTH;

  /** the smallest width/height of the badge so that the counter stays readable */
  private static final int    MIN_BADGE_SIZE     = 8;

  /** the smallest font size to shrink the counter down to */
  private static final float  MIN_FONT_SIZE      = 6.0f;

  /** the smallest diameter of the plain dot which replaces the counter */
  private static final int    MIN_DOT_SIZE       = 5;

  /** the part of the icon height the badge must not exceed, so that small icons stay readable */
  private static final double MAX_HEIGHT_FACTOR  = 0.5;

  /** the part of the icon width the badge must not exceed, so that small icons stay readable */
  private static final double MAX_WIDTH_FACTOR   = 0.65;

  /** the part of the icon height the plain dot gets (used as counter fallback) */
  private static final double DOT_HEIGHT_FACTOR  = 0.35;

  /** the background of the badge */
  private static final Color  BADGE_BACKGROUND   = Color.RED;

  /** the color of the counter */
  private static final Color  BADGE_FOREGROUND   = Color.WHITE;

  private Icon                baseIcon;
  private int                 count;

  /**
   * Creates a new {@link BadgeIcon} without a visible counter.
   *
   * @param baseIcon
   *          the icon to decorate
   */
  public BadgeIcon(Icon baseIcon) {
    this(baseIcon, 0);
  }

  /**
   * Creates a new {@link BadgeIcon} showing the given counter.
   *
   * @param baseIcon
   *          the icon to decorate
   * @param count
   *          the counter to display; fewer than two messages hide the badge
   */
  public BadgeIcon(Icon baseIcon, int count) {
    this.baseIcon = baseIcon;
    this.count = count;
  }

  /**
   * Update base icon and counter at once.
   *
   * @param newBaseIcon
   *          the icon to decorate; ignored when {@code null}
   * @param newCount
   *          the counter to display; fewer than two messages hide the badge
   */
  public void update(Icon newBaseIcon, int newCount) {
    if (newBaseIcon != null) {
      baseIcon = newBaseIcon;
    }
    count = newCount;
  }

  /**
   * Update only the counter, keeping the base icon.
   *
   * @param newCount
   *          the counter to display; fewer than two messages hide the badge
   */
  public void setCount(int newCount) {
    count = newCount;
  }

  /**
   * Get the currently displayed counter.
   *
   * @return the counter
   */
  public int getCount() {
    return count;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (baseIcon != null) {
      baseIcon.paintIcon(c, g, x, y);
    }

    if (count < MIN_COUNT) {
      return;
    }

    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      Font font = g2.getFont().deriveFont(Font.BOLD, (float) (g2.getFont().getSize2D() * FONT_SCALE));
      FontMetrics fm = g2.getFontMetrics(font);

      int maxBadgeHeight = cappedSize(getIconHeight(), MAX_HEIGHT_FACTOR, MIN_BADGE_SIZE);
      int maxBadgeWidth = cappedSize(getIconWidth(), MAX_WIDTH_FACTOR, MIN_BADGE_SIZE);

      // shrink the counter until it fits the height cap, so that small icons stay readable
      while (fm.getHeight() > maxBadgeHeight && font.getSize2D() > MIN_FONT_SIZE) {
        font = font.deriveFont((float) Math.max(MIN_FONT_SIZE, font.getSize2D() * maxBadgeHeight / fm.getHeight()));
        fm = g2.getFontMetrics(font);
      }

      String label = pickCounterLabel(fm, maxBadgeWidth);
      int badgeWidth;
      int badgeHeight;

      if (label.isEmpty()) {
        // not even a coarse counter fits - go for a plain dot
        badgeHeight = cappedSize(getIconHeight(), DOT_HEIGHT_FACTOR, MIN_DOT_SIZE);
        badgeWidth = Math.min(maxBadgeWidth, badgeHeight);
      }
      else {
        badgeHeight = Math.min(maxBadgeHeight, fm.getHeight());
        badgeWidth = Math.min(maxBadgeWidth, Math.max(badgeHeight, fm.stringWidth(label) + 2 * HORIZONTAL_PADDING));
      }

      // anchor the badge at the top right corner; a wider counter grows to the left
      int badgeX = x + getIconWidth() - badgeWidth - BADGE_INSET;
      int badgeY = y + BADGE_INSET;

      g2.setColor(ringColor(c, g2));
      g2.fillRoundRect(badgeX - RING_WIDTH, badgeY - RING_WIDTH, badgeWidth + 2 * RING_WIDTH, badgeHeight + 2 * RING_WIDTH, badgeHeight, badgeHeight);

      g2.setColor(BADGE_BACKGROUND);
      g2.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, badgeHeight, badgeHeight);

      if (!label.isEmpty()) {
        g2.setFont(font);
        g2.setColor(BADGE_FOREGROUND);
        g2.drawString(label, badgeX + (badgeWidth - fm.stringWidth(label)) / 2, badgeY + (badgeHeight + fm.getAscent() - fm.getDescent()) / 2);
      }
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
   * The counter to show: the exact number, clamped at {@link #MAX_COUNT} - or the coarser clamp at {@link #SMALL_MAX_COUNT} - or nothing at all when
   * even that would cover more than the given width.
   *
   * @param fm
   *          the font metrics of the counter
   * @param maxWidth
   *          the widest the badge may become
   * @return the label to render, empty when the counter does not fit at all
   */
  private String pickCounterLabel(FontMetrics fm, int maxWidth) {
    String label = clampedLabel(count, MAX_COUNT);
    if (badgeWidth(label, fm) <= maxWidth) {
      return label;
    }

    label = clampedLabel(count, SMALL_MAX_COUNT);
    return badgeWidth(label, fm) <= maxWidth ? label : "";
  }

  private String clampedLabel(int value, int max) {
    return value > max ? max + "+" : String.valueOf(value);
  }

  private int badgeWidth(String label, FontMetrics fm) {
    return Math.max(MIN_BADGE_SIZE, fm.stringWidth(label) + 2 * HORIZONTAL_PADDING);
  }

  private int cappedSize(int size, double factor, int minSize) {
    return Math.max(minSize, (int) Math.round(size * factor));
  }

  /**
   * The color of the ring which separates the badge from the icon: the background of the first opaque ancestor, so the badge looks like it floats
   * above the icon.
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
