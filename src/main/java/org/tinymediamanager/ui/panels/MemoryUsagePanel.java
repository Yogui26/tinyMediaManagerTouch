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
package org.tinymediamanager.ui.panels;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;

/**
 * The Class {@link MemoryUsagePanel} provides a compact, theme-aware visualization of the current JVM memory usage in the status bar.
 * <p>
 * It periodically samples heap statistics (used, free, and max) and paints a horizontal usage bar. To better capture sustained pressure rather than
 * short spikes, it maintains a rolling average of the last 10 timer ticks. When the average memory usage exceeds the configured threshold (default:
 * 85%), the bar is rendered using a warning color that adapts to the current Look and Feel. If an OutOfMemoryError has been detected, the panel
 * highlights the condition using the error color and a red warning icon.
 * </p>
 *
 * <ul>
 * <li>Sampling interval: 1 second</li>
 * <li>Rolling average window: 10 samples</li>
 * <li>High usage threshold: 85% (average across the last samples)</li>
 * <li>Colors: derived from UI defaults (Component.warning.*, Component.errorColor, Panel.tmmAlternateBackground)</li>
 * </ul>
 *
 * Usage: add this panel to the status bar; visibility controls start/stop the internal timer automatically.
 *
 * @author Manuel Laggner
 * @since 4.0
 */
class MemoryUsagePanel extends JPanel {
  /**
   * One mebibyte (MiB) in bytes.
   */
  private static final int MB                   = 1048576;
  /**
   * Number of samples to consider for high-usage rolling average.
   */
  private static final int HISTORY_SIZE         = 10;
  /**
   * Threshold (in percent) for considering memory usage as high.
   */
  private static final int HIGH_USAGE_THRESHOLD = 85;
  /**
   * Flag indicating whether an OutOfMemoryError has been detected during the application's lifetime. When set, the panel permanently shows an error
   * indication until the application is restarted.
   */
  private static boolean   oomDetected          = false;

  /**
   * Periodic sampler that refreshes memory statistics and triggers repaints.
   */
  private final Timer      timer;
  /**
   * Label displaying textual memory usage and (if applicable) a warning icon.
   */
  private final JLabel     lblMemory;

  /**
   * The maximum amount of memory that the JVM will attempt to use (Xmx), in bytes.
   */
  private long             maxMem;
  /**
   * The current total amount of memory allocated to the JVM, in bytes.
   */
  private long             totalMem;
  /**
   * The current amount of free memory within the allocated heap, in bytes.
   */
  private long             freeMem;
  /**
   * The current amount of used memory (total - free), in bytes.
   */
  private long             usedMem;
  /**
   * Circular buffer with the last memory usage percentages (0-100).
   */
  private final int[]      usageHistory         = new int[HISTORY_SIZE];
  /**
   * Current count of valid samples in {@link #usageHistory}.
   */
  private int              usageCount           = 0;
  /**
   * Current write index into {@link #usageHistory}.
   */
  private int              usageIndex           = 0;
  /**
   * Whether the rolling average exceeds the {@link #HIGH_USAGE_THRESHOLD}.
   */
  private boolean          highUsage            = false;

  /**
   * Creates a new MemoryUsagePanel and initializes UI components and the sampling timer.
   * <p>
   * The timer runs every second to refresh the memory statistics, maintain the rolling average window, update the label text, and repaint the usage
   * bar. The panel uses theme-aware colors from the current LaF to ensure good contrast in both light and dark modes.
   * </p>
   */
  public MemoryUsagePanel() {
    setLayout(new FlowLayout());

    setOpaque(false);
    setFocusable(false);

    getMemory();

    lblMemory = new JLabel();
    lblMemory.setHorizontalTextPosition(SwingConstants.LEFT);
    lblMemory.setMinimumSize(getLabelMinimumSize());
    lblMemory.setOpaque(false);
    add(lblMemory);

    setMemoryText();

    timer = new Timer(1000, null);
    timer.addActionListener(evt -> {
      if (oomDetected && lblMemory.getIcon() == null) {
        lblMemory.setIcon(IconManager.WARN_RED);
      }

      getMemory();

      // update high usage state from rolling average
      updateUsageHistoryAndAverage();

      setMemoryText();
      repaint();
    });

    ToolTipManager.sharedInstance().registerComponent(this);
  }

  /**
   * Starts or stops the internal sampling timer based on visibility.
   *
   * @param visible
   *          {@code true} to start sampling and painting; {@code false} to stop
   */
  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      timer.start();
    }
    else {
      timer.stop();
    }
  }

  /**
   * Reads current JVM memory statistics (total, free, max) and calculates used memory.
   */
  private void getMemory() {
    Runtime rt = Runtime.getRuntime();
    totalMem = rt.totalMemory();
    maxMem = rt.maxMemory(); // = Xmx
    freeMem = rt.freeMemory();

    // see http://stackoverflow.com/a/18375641
    usedMem = totalMem - freeMem;
  }

  /**
   * Updates the memory label text (used and max) in MiB.
   */
  private void setMemoryText() {
    lblMemory.setText(usedMem / MB + " / " + maxMem / MB + "M");
  }

  /**
   * Paints the usage bar using theme-aware colors according to the current state.
   * <p>
   * - If the rolling average exceeds the threshold, the bar is painted with a warning color.<br>
   * - Otherwise, the bar uses the default background accent color.
   * </p>
   */
  @Override
  public void paintComponent(Graphics g) {
    Dimension size = getSize();
    int barWidth = size.width;

    int usedBarLength = (int) (barWidth * usedMem / (maxMem == 0 ? 1 : maxMem));

    // gauge (used)
    // choose color depending on state: high average usage -> warning, else default
    if (highUsage) {
      // use a warning color that adapts to the LaF
      g.setColor(UIManager.getColor("Component.warning.focusedBorderColor"));
    }
    else {
      g.setColor(UIManager.getColor("Panel.tmmAlternateBackground"));
    }
    g.fillRect(0, 0, usedBarLength, size.height - 1);

    // text
    super.paintComponent(g);
  }

  @Override
  public String getToolTipText() {
    long megs = 1048576;

    // see http://stackoverflow.com/a/18375641
    long used = totalMem - freeMem;
    long free = maxMem - used;

    String text = TmmResourceBundle.getString("tmm.memoryused") + " " + used / megs + " MiB  /  " + TmmResourceBundle.getString("tmm.memoryfree")
        + " " + free / megs + " MiB  /  " + TmmResourceBundle.getString("tmm.memorymax") + " " + maxMem / megs + " MiB";

    if (oomDetected) {
      text += "\n\n" + TmmResourceBundle.getString("tmm.oom");
    }

    return text;
  }

  private Dimension getLabelMinimumSize() {
    String text = maxMem * 10 / MB + " / " + maxMem / MB + "M";
    Insets insets = lblMemory.getInsets();
    int width = insets.left + insets.right + lblMemory.getFontMetrics(lblMemory.getFont()).stringWidth(text);
    int height = lblMemory.getMinimumSize().height;
    return new Dimension(width, height);
  }

  /**
   * Update the rolling memory usage history and compute the average percentage to set the {@link #highUsage} flag. The current usage percentage is
   * calculated as usedMem / maxMem, clamped to [0,100].
   */
  private void updateUsageHistoryAndAverage() {
    // avoid division by zero
    int percent = 0;
    if (maxMem > 0) {
      double p = (usedMem * 100.0) / maxMem;
      if (p < 0) {
        p = 0;
      }
      else if (p > 100) {
        p = 100;
      }
      percent = (int) Math.round(p);
    }

    usageHistory[usageIndex] = percent;
    usageIndex = (usageIndex + 1) % HISTORY_SIZE;
    if (usageCount < HISTORY_SIZE) {
      usageCount++;
    }

    int sum = 0;
    for (int i = 0; i < usageCount; i++) {
      sum += usageHistory[i];
    }
    int avg = usageCount > 0 ? (sum / usageCount) : 0;
    highUsage = avg >= HIGH_USAGE_THRESHOLD;
  }

  /**
   * Call this when an OutOfMemoryError has been detected! This will force the panel to draw an exception mark until tmm is newly launched
   */
  static void setOomDetected() {
    oomDetected = true;
  }
}
