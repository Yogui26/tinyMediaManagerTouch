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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.CustomColumn;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;

import ca.odell.glazedlists.EventList;

/**
 * The class {@link CustomColumnTable} is used to display and arrange the freely configurable columns of a table.
 * <p>
 * The columns never get edited inline - {@link #editButtonClicked(int)} is the hook to do that, since a JMTE pattern is way too long to be typed into
 * a table cell. The order of the rows is the order of the columns within the table.
 *
 * @param <C>
 *          the concrete type of the custom column bean
 * @author Manuel Laggner
 */
public abstract class CustomColumnTable<C extends CustomColumn> extends TmmEditorTable {

  /** the table always keeps room for this many rows, so it does not look squashed next to the button column of the settings */
  private static final int MIN_VISIBLE_ROWS = 3;

  /**
   * @param customColumns
   *          the live list of the configured columns
   * @param levels
   *          the hierarchy levels this table carries a JMTE pattern for, in the order they should be shown
   */
  public CustomColumnTable(EventList<C> customColumns, List<CustomColumnLevel<C>> levels) {
    super();

    setModel(new TmmTableModel<>(customColumns, new CustomColumnTableFormat<>(levels)));
    getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    // draw the row/column separators by ourselves - this table is never put into a scroll pane, so no viewport paints them for us
    setShowGrid(true);

    adjustColumnPreferredWidths(3);
  }

  /**
   * Makes sure the table is always at least tall enough for {@link #MIN_VISIBLE_ROWS} rows. Since it is not put into a scroll pane, this minimum
   * height is what keeps it from looking squashed next to the button column of the settings.
   *
   * @return the preferred size with a raised height
   */
  @Override
  public Dimension getPreferredSize() {
    Dimension preferredSize = super.getPreferredSize();
    preferredSize.height = Math.max(preferredSize.height, MIN_VISIBLE_ROWS * getRowHeight());
    return preferredSize;
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  /**
   * Returns the table together with its header, wrapped into a plain panel and without any scroll pane.
   * <p>
   * Since it is not put into a scroll pane, this table grows to the height of all its rows and no viewport paints grid lines into the empty area
   * below them. The scroll pane of the whole settings panel does the scrolling instead.
   *
   * @return the table ready to be added to a layout
   */
  public JComponent createComponent() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.add(getTableHeader(), BorderLayout.NORTH);
    panel.add(this, BorderLayout.CENTER);
    return panel;
  }

  /**
   * The table format of the {@link CustomColumnTable}.
   */
  private static class CustomColumnTableFormat<C extends CustomColumn> extends TmmTableFormat<C> {
    private CustomColumnTableFormat(List<CustomColumnLevel<C>> levels) {
      /*
       * the header title
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.title"), "title", customColumn -> customColumn.getTitle(), String.class);
      addColumn(col);

      /*
       * one column per hierarchy level
       */
      for (int i = 0; i < levels.size(); i++) {
        CustomColumnLevel<C> level = levels.get(i);
        col = new Column(level.getLabel(), "pattern" + i, customColumn -> StringUtils.defaultString(level.getPattern(customColumn)), String.class);
        addColumn(col);
      }

      /*
       * the editor
       */
      col = new Column(TmmResourceBundle.getString("Button.edit"), "edit", customColumn -> IconManager.EDIT, ImageIcon.class);
      col.setColumnResizeable(false);
      col.setHeaderIcon(IconManager.EDIT_HEADER);
      addColumn(col);
    }
  }
}
