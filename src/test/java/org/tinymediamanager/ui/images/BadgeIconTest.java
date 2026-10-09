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

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.function.Predicate;

import javax.swing.Icon;

import org.junit.Test;

/**
 * The class {@link BadgeIconTest} checks that the counter badge is painted as an overlay which never changes the size of the decorated icon - and
 * never covers more than a fraction of it.
 */
public class BadgeIconTest {
  private static final int    SIZE              = 24;

  /** the part of the icon the badge may cover - keep in sync with BadgeIcon */
  private static final double MAX_HEIGHT_FACTOR = 0.5;

  private static final double MAX_WIDTH_FACTOR  = 0.65;

  private static final int    MIN_BADGE_SIZE    = 8;

  @Test
  public void keepsDimensionsOfBaseIcon() {
    BadgeIcon badge = new BadgeIcon(new SolidIcon(SIZE), 3);

    assertThat(badge.getIconWidth()).isEqualTo(SIZE);
    assertThat(badge.getIconHeight()).isEqualTo(SIZE);
  }

  @Test
  public void paintsNothingForLessThanTwoMessages() {
    // a single message is already announced by the decorated icon itself
    for (int count : new int[] { 0, 1 }) {
      BufferedImage image = rasterize(new BadgeIcon(new SolidIcon(SIZE), count));

      assertThat(countPixels(image, this::isBadgeColor)).as("badge pixels for count " + count).isZero();
      assertThat(countPixels(image, color -> !color.equals(Color.RED))).as("base icon pixels").isPositive();
    }
  }

  @Test
  public void paintsBadgeWithCount() {
    BufferedImage image = rasterize(new BadgeIcon(new SolidIcon(SIZE), 3));

    assertThat(countPixels(image, this::isBadgeColor)).as("badge pixels").isPositive();
  }

  @Test
  public void updateChangesCountAndBaseIcon() {
    BadgeIcon badge = new BadgeIcon(new SolidIcon(SIZE));

    badge.update(new SolidIcon(SIZE), 0);
    assertThat(badge.getCount()).isZero();

    badge.update(new SolidIcon(SIZE), 1);
    assertThat(countPixels(rasterize(badge), this::isBadgeColor)).as("single message stays without a badge").isZero();

    badge.update(new SolidIcon(SIZE), 5);
    assertThat(badge.getCount()).isEqualTo(5);

    // a null base icon keeps the previous one
    badge.update(null, 7);
    assertThat(badge.getIconWidth()).isEqualTo(SIZE);
    assertThat(badge.getCount()).isEqualTo(7);

    badge.setCount(0);
    assertThat(badge.getCount()).isZero();
  }

  @Test
  public void badgeStaysAnchoredAtTheTopRightCorner() {
    Rectangle singleDigit = badgeBounds(rasterize(new BadgeIcon(new SolidIcon(SIZE), 3)));
    Rectangle bigCounter = badgeBounds(rasterize(new BadgeIcon(new SolidIcon(SIZE), 120)));

    // both sit in the corner (a hair off it to leave room for the ring of the badge)
    assertThat(singleDigit.x + singleDigit.width).isBetween(SIZE - 3, SIZE);
    assertThat(bigCounter.x + bigCounter.width).isBetween(SIZE - 3, SIZE);
    assertThat(singleDigit.y).isBetween(0, 3);
    assertThat(bigCounter.y).isBetween(0, 3);

    // the wider counter reaches further to the left
    assertThat(bigCounter.x).isLessThan(singleDigit.x);
  }

  @Test
  public void badgeNeverCoversMoreThanAFractionOfTheIcon() {
    for (int size : new int[] { 17, 24, 36 }) {
      for (int count : new int[] { 2, 9, 12, 99, 120 }) {
        Rectangle bounds = badgeBounds(rasterize(new BadgeIcon(new SolidIcon(size), count)));

        assertThat(bounds).as("badge of a " + size + "px icon with count " + count).isNotNull();
        assertThat(bounds.width).as("width for " + size + "px / " + count)
            .isLessThanOrEqualTo(Math.max(MIN_BADGE_SIZE, (int) Math.round(size * MAX_WIDTH_FACTOR)));
        assertThat(bounds.height).as("height for " + size + "px / " + count)
            .isLessThanOrEqualTo(Math.max(MIN_BADGE_SIZE, (int) Math.round(size * MAX_HEIGHT_FACTOR)));
      }
    }
  }

  @Test
  public void fallsBackToPlainDotWhenTheCounterDoesNotFit() {
    // even "9+" would cover too much of this tiny icon
    BufferedImage tiny = rasterize(new BadgeIcon(new SolidIcon(12), 120));

    assertThat(badgeBounds(tiny).width).isLessThan(badgeBounds(rasterize(new BadgeIcon(new SolidIcon(SIZE), 120))).width);
    assertThat(countPixels(tiny, this::isForegroundColor)).as("no counter on a dot").isZero();

    // on a regular icon the counter is rendered
    assertThat(countPixels(rasterize(new BadgeIcon(new SolidIcon(SIZE), 120)), this::isForegroundColor)).as("counter pixels").isPositive();
  }

  private BufferedImage rasterize(BadgeIcon icon) {
    int size = icon.getIconWidth();
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    icon.paintIcon(null, image.createGraphics(), 0, 0);
    return image;
  }

  private Rectangle badgeBounds(BufferedImage image) {
    Rectangle bounds = null;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (isBadgeColor(new Color(image.getRGB(x, y), true))) {
          if (bounds == null) {
            bounds = new Rectangle(x, y, 1, 1);
          }
          else {
            bounds.add(new Rectangle(x, y, 1, 1));
          }
        }
      }
    }
    return bounds;
  }

  private boolean isBadgeColor(Color color) {
    return color.getRed() > 200 && color.getGreen() < 100 && color.getBlue() < 100;
  }

  /**
   * the counter is anti aliased, so look for bright pixels instead of the exact foreground color
   */
  private boolean isForegroundColor(Color color) {
    return color.getRed() > 200 && color.getGreen() > 200 && color.getBlue() > 200;
  }

  private int countPixels(BufferedImage image, Predicate<Color> matcher) {
    int count = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if ((image.getRGB(x, y) >>> 24) != 0 && matcher.test(new Color(image.getRGB(x, y), true))) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * A simple icon with a solid background to have a deterministic base icon
   */
  private static final class SolidIcon implements Icon {
    private final int size;

    private SolidIcon(int size) {
      this.size = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      g.setColor(Color.DARK_GRAY);
      g.fillRect(x, y, size, size);
    }

    @Override
    public int getIconWidth() {
      return size;
    }

    @Override
    public int getIconHeight() {
      return size;
    }
  }
}
