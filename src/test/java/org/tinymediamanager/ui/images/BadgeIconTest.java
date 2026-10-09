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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.junit.Test;

/**
 * The class {@link BadgeIconTest} checks the geometry of the notification dot: it is sized to be seen on a small toolbar icon, hangs in its top right
 * corner and never shows anything but the dot itself.
 */
public class BadgeIconTest {
  /** a toolbar icon is quite small - the bell in the toolbar is even smaller with 17 pixels */
  private static final int SMALL_ICON = 17;
  private static final int SIZE       = 24;

  /** the inset of the dot and the width of its ring - keep in sync with BadgeIcon */
  private static final int INSET      = 1;

  /** the pixels the dot reaches past the corner of the icon - keep in sync with BadgeIcon */
  private static final int OVERHANG   = 2;

  /** the icons are painted at an offset, so that a dot which ignores it cannot slip through */
  private static final int PAINT_X    = 5;
  private static final int PAINT_Y    = 7;

  /** the smallest and the biggest dot we paint - keep in sync with BadgeIcon */
  private static final int MIN_DOT    = 7;
  private static final int MAX_DOT    = 9;

  @Test
  public void boxKeepsTheSizeOfTheDecoratedIcon() {
    final BadgeIcon badge = new BadgeIcon(new SolidIcon(SIZE), 3);

    // the dot is an overlay, so nothing around the button shifts when it appears
    assertThat(badge.getIconWidth()).isEqualTo(SIZE);
    assertThat(badge.getIconHeight()).isEqualTo(SIZE);
  }

  @Test
  public void dotIsBigEnoughToBeSeen() {
    // the dot is the only hint now - it must not shrink into something you notice only when you already look for it
    assertThat(BadgeIcon.dotSize(SMALL_ICON)).isGreaterThanOrEqualTo(MIN_DOT);
    assertThat(BadgeIcon.dotSize(SIZE)).isGreaterThanOrEqualTo(MIN_DOT);
  }

  @Test
  public void dotNeverSwallowsTheIcon() {
    for (int width = 16; width <= 48; width += 4) {
      final int dot = BadgeIcon.dotSize(width);
      final Rectangle rect = BadgeIcon.dotRect(width);

      assertThat(dot).isBetween(MIN_DOT, MAX_DOT);
      assertThat(dot).isLessThanOrEqualTo((int) (width * 0.6) + 1);
      // the dot sits in the corner and leaves at least half of the artwork to itself
      assertThat(rect.x).isGreaterThanOrEqualTo(width / 2);
      assertThat(rect.y + rect.height + INSET).isLessThanOrEqualTo(width);
      // the ring stays within the overhang, so it never reaches into whatever comes next
      assertThat(rect.x + rect.width + INSET).isLessThanOrEqualTo(width + OVERHANG + INSET);
    }
  }

  @Test
  public void dotHangsInTheTopRightCorner() {
    final Rectangle dot = BadgeIcon.dotRect(SIZE);

    assertThat(dot.y).isEqualTo(INSET);
    // the icon has some dead space around its artwork, so the dot has to reach past the box to sit on the visual corner
    assertThat(dot.x + dot.width).isEqualTo(SIZE + OVERHANG);
    assertThat(dot.width * dot.height).isLessThanOrEqualTo(SIZE * SIZE / 2);
  }

  @Test
  public void showsNothingWithoutNewMessages() {
    final BufferedImage image = rasterize(new BadgeIcon(new SolidIcon(SIZE), 0));

    assertThat(redPixels(image)).isEmpty();
    // just the plain icon
    assertThat(paintedPixels(image)).isEqualTo(SIZE * SIZE);
  }

  @Test
  public void showsTheDotForEveryCountAndNeverANumber() {
    for (int count : new int[] { 1, 3, 1200 }) {
      final BufferedImage image = rasterize(new BadgeIcon(new SolidIcon(SIZE), count));
      final Rectangle dot = paintedDotRect();

      final List<int[]> red = redPixels(image);
      assertThat(red).isNotEmpty();
      // nothing but the dot - no digit ever leaks out of it
      assertThat(red).allSatisfy(pixel -> assertThat(dot.contains(pixel[0], pixel[1])).isTrue());
      // and the dot is filled, not only outlined
      assertThat(red.size()).isGreaterThan(dot.width * dot.height / 4);
    }
  }

  @Test
  public void paintsTheBaseIconAndTheDot() {
    final BufferedImage image = rasterize(new BadgeIcon(new SolidIcon(SIZE), 3));
    final Rectangle dot = paintedDotRect();

    assertThat(new Color(image.getRGB(PAINT_X + 2, PAINT_Y + SIZE - 2), true).getAlpha()).isPositive();
    assertThat(image.getRGB(dot.x + dot.width / 2, dot.y + dot.height / 2)).isEqualTo(Color.RED.getRGB());
  }

  @Test
  public void dotFollowsThePaintedIcon() {
    // the bell does not sit at the very top of the button - its dot has to move along with it
    final Rectangle dot = paintedDotRect();
    assertThat(dot.x).isEqualTo(PAINT_X + SIZE - BadgeIcon.dotSize(SIZE) + OVERHANG);
    assertThat(dot.y).isGreaterThan(PAINT_Y);
    assertThat(dot.y + dot.height + INSET).isLessThanOrEqualTo(PAINT_Y + SIZE);
  }

  @Test
  public void updateChangesCountAndBaseIcon() {
    final BadgeIcon badge = new BadgeIcon(new SolidIcon(SIZE));
    assertThat(badge.getCount()).isZero();

    badge.update(new SolidIcon(SIZE + 6), 5);
    assertThat(badge.getIconWidth()).isEqualTo(SIZE + 6);
    assertThat(badge.getCount()).isEqualTo(5);
    assertThat(redPixels(rasterize(badge))).isNotEmpty();

    badge.update(null, 0);
    assertThat(badge.getIconWidth()).isEqualTo(SIZE + 6);
    assertThat(redPixels(rasterize(badge))).isEmpty();
  }

  @Test
  public void setCountShowsAndHidesTheDot() {
    final BadgeIcon badge = new BadgeIcon(new SolidIcon(SIZE));

    badge.setCount(2);
    assertThat(redPixels(rasterize(badge))).isNotEmpty();

    badge.setCount(0);
    assertThat(redPixels(rasterize(badge))).isEmpty();
  }

  @Test
  public void worksWithoutBaseIcon() {
    final BadgeIcon badge = new BadgeIcon(null, 3);

    assertThat(badge.getIconWidth()).isZero();
    assertThat(redPixels(rasterize(badge))).isEmpty();
  }

  private Rectangle paintedDotRect() {
    final Rectangle dot = BadgeIcon.dotRect(SIZE);
    return new Rectangle(PAINT_X + dot.x, PAINT_Y + dot.y, dot.width, dot.height);
  }

  private BufferedImage rasterize(Icon icon) {
    final BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2 = image.createGraphics();
    icon.paintIcon(null, g2, PAINT_X, PAINT_Y);
    g2.dispose();
    return image;
  }

  private List<int[]> redPixels(BufferedImage image) {
    final List<int[]> pixels = new ArrayList<>();
    for (int py = 0; py < image.getHeight(); py++) {
      for (int px = 0; px < image.getWidth(); px++) {
        final Color color = new Color(image.getRGB(px, py), true);
        if (color.getAlpha() > 128 && color.getRed() > 150 && color.getGreen() < 100 && color.getBlue() < 100) {
          pixels.add(new int[] { px, py });
        }
      }
    }
    return pixels;
  }

  private int paintedPixels(BufferedImage image) {
    int count = 0;
    for (int py = 0; py < image.getHeight(); py++) {
      for (int px = 0; px < image.getWidth(); px++) {
        if (new Color(image.getRGB(px, py), true).getAlpha() > 0) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * a solid square as stand in for the decorated icon
   */
  private static class SolidIcon implements Icon {
    private final int size;

    SolidIcon(int size) {
      this.size = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      g.setColor(Color.BLUE);
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
