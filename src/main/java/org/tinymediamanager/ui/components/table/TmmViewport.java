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
package org.tinymediamanager.ui.components.table;

import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JViewport;

/**
 * The Viewport which is used by the TmmTable
 * 
 * @author Manuel Laggner
 */
class TmmViewport extends JViewport {
  private final JTable fTable;

  /**
   * a listener to cause the whole table to repaint when a column is resized, because the extended grid lines may need to be repainted
   */
  private final PropertyChangeListener columnWidthListener;

  TmmViewport(JTable table, int[] cols) {
    fTable = table;
    List<Integer> colsWoRightGrid = new ArrayList<>(cols.length);
    for (int i : cols) {
      colsWoRightGrid.add(i);
    }
    fTable.putClientProperty("borderNotToDraw", colsWoRightGrid);
    setOpaque(false);
    columnWidthListener = evt -> repaint();
    initListeners();
  }

  private void initListeners() {
    for (int i = 0; i < fTable.getColumnModel().getColumnCount(); i++) {
      fTable.getColumnModel().getColumn(i).addPropertyChangeListener(columnWidthListener);
    }
  }

  /**
   * Takes over the columns the table currently has. Needed after the table rebuilt its columns from scratch: those are brand new objects which know
   * nothing about the listener above, and without it the grid lines would stay painted at the positions of the structure before the rebuild.
   */
  void updateColumns() {
    initListeners();
    repaint();
  }

  @Override
  public void setViewPosition(Point p) {
    super.setViewPosition(p);
    repaint();
  }
}
