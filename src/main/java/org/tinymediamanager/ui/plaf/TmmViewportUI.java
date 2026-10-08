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
package org.tinymediamanager.ui.plaf;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.table.TableColumn;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.ui.FlatViewportUI;

/**
 * The class TmmViewportUI paints the table grid lines over the whole viewport, so that the empty areas outside the table view (below the last row and
 * right of the last column) show a continued grid.
 *
 * @author Manuel Laggner
 */
public class TmmViewportUI extends FlatViewportUI {

  private boolean paintTmmGrid;
  private Color   gridColor;
  private Color   gridColor2;

  public static ComponentUI createUI(JComponent c) {
    return FlatUIUtils.createSharedUI(TmmViewportUI.class, TmmViewportUI::new);
  }

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);

    paintTmmGrid = Boolean.TRUE.equals(UIManager.getBoolean("Table.paintTmmGrid"));
    if (paintTmmGrid) {
      gridColor = UIManager.getColor("Table.gridColor");
      gridColor2 = UIManager.getColor("Table.gridColor2");
      paintTmmGrid = gridColor != null && gridColor2 != null;
    }
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);

    if (paintTmmGrid) {
      JViewport viewport = (JViewport) c;

      if (viewport.getView() instanceof JTable) {
        JTable table = (JTable) viewport.getView();
        paintVerticalGridLines(g, viewport, table);
        paintHorizontalGridLines(g, viewport, table);
      }
    }
  }

  private void paintVerticalGridLines(Graphics g, JViewport viewport, JTable table) {
    // the viewport x coordinate of the border after the first column, adjusted by the current horizontal scroll offset
    int x = -viewport.getViewPosition().x;

    // the trailing line after the last column is not painted, consistent with FlatLaf hiding the trailing vertical line
    for (int i = 0; i < table.getColumnCount() - 1; i++) {
      TableColumn column = table.getColumnModel().getColumn(i);
      // increase the x position by the width of the current column
      x += column.getWidth();

      if (x > 0) {
        // the grid line replaces the last pixel of the column margin, hence the -1 (same as BasicTableUI does)
        g.setColor(gridColor);
        g.drawLine(x - 1, 0, x - 1, viewport.getHeight() - 1);
      }
    }
  }

  private void paintHorizontalGridLines(Graphics g, JViewport viewport, JTable table) {
    // switch into the coordinate space of the table (the view of the viewport)
    Point viewPosition = viewport.getViewPosition();
    g.translate(0, -viewPosition.y);

    // if the delegate is painted outside a regular paint cycle, the clip may be unset, so fall back to the full
    // viewport bounds
    Rectangle clip = g.getClipBounds();
    if (clip == null) {
      clip = new Rectangle(0, viewPosition.y, viewport.getWidth(), viewport.getHeight());
    }

    // get the row index at the top of the clip bounds (the first row to paint). rowAtPoint returns -1 if the clip
    // starts below the last row (e.g. on partial repaints over the empty rows) or if the table has no rows at all
    int rowAtPoint = table.rowAtPoint(clip.getLocation());
    int rowCount = table.getRowCount();

    // anchor the grid always at the row geometry (independent of the clip bounds), so that partial and full repaints
    // paint the exact same lines. rows below the last table row continue with the default row height
    int topY;
    int currentRow;
    if (rowAtPoint >= 0) {
      topY = table.getCellRect(rowAtPoint, 0, true).y;
      currentRow = rowAtPoint;
    }
    else {
      currentRow = rowCount;
      if (rowCount == 0) {
        topY = 0;
      }
      else {
        Rectangle lastRow = table.getCellRect(rowCount - 1, 0, true);
        topY = lastRow.y + lastRow.height;
      }
    }

    int viewPortWidth = viewport.getWidth();

    // the boundary line pair at the top of the first row to paint belongs to the row above it. paint it as well, so
    // that a partial repaint (whose clip can start exactly on such a boundary) results in the same pixels as a full
    // repaint. pixels outside the clip are discarded by the graphics context anyway
    if (currentRow > 0) {
      paintGridLinePair(g, topY, viewPortWidth);
    }

    int defaultRowHeight = table.getRowHeight();
    while (topY < clip.y + clip.height) {
      int rowHeight = table.getRowHeight(currentRow);
      if (rowHeight <= 0) {
        rowHeight = defaultRowHeight;
      }

      int bottomY = topY + rowHeight;
      paintGridLinePair(g, bottomY, viewPortWidth);

      topY = bottomY;
      currentRow++;
    }

    g.translate(0, viewPosition.y);
  }

  private void paintGridLinePair(Graphics g, int bottomY, int viewPortWidth) {
    // the first line replaces the last pixel of the row margin, the second line is the highlight below it
    g.setColor(gridColor);
    g.drawLine(0, bottomY - 1, viewPortWidth - 1, bottomY - 1);
    g.setColor(gridColor2);
    g.drawLine(0, bottomY, viewPortWidth - 1, bottomY);
  }
}
