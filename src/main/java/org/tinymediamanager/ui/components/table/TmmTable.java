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

import static javax.swing.ScrollPaneConstants.UPPER_RIGHT_CORNER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.button.FlatButton;
import org.tinymediamanager.ui.components.treetable.TmmTreeTableModel;

import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.impl.gui.SortingStrategy;
import ca.odell.glazedlists.swing.SortableRenderer;
import net.miginfocom.swing.MigLayout;

/**
 * The Class TmmTable. It's being used to draw the tables like our designer designed it
 *
 * @author Manuel Laggner
 */
public class TmmTable extends JTable {
  private TmmTableComparatorChooser<?> tableComparatorChooser;
  protected boolean                    columnsAdjusting;

  public TmmTable() {
    super();
    init();
  }

  public TmmTable(TableModel dm) {
    setModel(dm);
    init();
  }

  @Override
  protected TableColumnModel createDefaultColumnModel() {
    return new TmmTableColumnModel();
  }

  @Override
  public void createDefaultColumnsFromModel() {
    columnsAdjusting = true;
    super.createDefaultColumnsFromModel();
    columnsAdjusting = false;
    adjustColumnPreferredWidths(3);
  }

  @Override
  public void addColumn(@NotNull TableColumn aColumn) {
    if (aColumn.getIdentifier() == null && getModel() instanceof TmmTableModel<?> tableModel) {
      aColumn.setHeaderRenderer(new SortableIconHeaderRenderer());
      tableModel.setUpColumn(aColumn);
    }
    super.addColumn(aColumn);
  }

  private void init() {
    // remove next line on enter
    getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "none");
    getTableHeader().setReorderingAllowed(false);
    setOpaque(false);

    getColumnModel().addColumnModelListener(columnModelListener);
  }

  /**
   * Keeps the column widths in sync with the table content. Made a field to be able to re-register it on a fresh column model.
   */
  private final TableColumnModelListener columnModelListener = new TableColumnModelListener() {
    @Override
    public void columnAdded(TableColumnModelEvent e) {
      // do not adjust column widths when loading the table model
      if (!columnsAdjusting) {
        adjustColumnPreferredWidths(3);
      }
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
      // do not adjust column widths when loading the table model
      if (!columnsAdjusting) {
        adjustColumnPreferredWidths(3);
      }
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
      // nothing to do
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
      // nothing to do
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
      // nothing to do
    }
  };

  public void installComparatorChooser(SortedList<?> sortedList) {
    installComparatorChooser(sortedList, new MouseKeyboardSortingStrategy());
  }

  public void installComparatorChooser(SortedList<?> sortedList, SortingStrategy sortingStrategy) {
    tableComparatorChooser = TmmTableComparatorChooser.install(this, sortedList, sortingStrategy);
  }

  public TmmTableComparatorChooser<?> getTableComparatorChooser() {
    return tableComparatorChooser;
  }

  /**
   * The Class {@link ColumnState} represents the state of a table column including its identifier and width.
   *
   * @author Manuel Laggner
   */
  public static class ColumnState {
    private final String identifier;
    private final int    width;

    /**
     * Creates a new column state.
     *
     * @param identifier
     *          the column identifier
     * @param width
     *          the column width in pixels
     */
    public ColumnState(String identifier, int width) {
      this.identifier = identifier;
      this.width = width;
    }

    /**
     * Gets the column identifier.
     *
     * @return the column identifier
     */
    public String getIdentifier() {
      return identifier;
    }

    /**
     * Gets the column width.
     *
     * @return the column width in pixels
     */
    public int getWidth() {
      return width;
    }
  }

  /**
   * Rebuilds the columns of this table from its (probably changed) table format. Needed when columns have been added or removed, since
   * {@code setModel()} does nothing when the very same model instance gets set again.
   * <p>
   * A completely new column model gets installed on purpose: the old one would keep its hidden columns (and their positions), which are not part of
   * the new structure anymore.
   */
  public void rebuildColumnsFromModel() {
    // drops all cached column state, especially the hidden columns of the previous structure
    int columnMargin = getColumnModel().getColumnMargin();
    TableColumnModel columnModel = createDefaultColumnModel();
    columnModel.addColumnModelListener(columnModelListener);
    // a fresh column model falls back to the default margin, which would lay out every cell beside the separator line the viewport paints
    columnModel.setColumnMargin(columnMargin);
    setColumnModel(columnModel);

    // let everyone know that the structure changed - the table recreates its columns and the comparator chooser resets its sorting
    if (getModel() instanceof TmmTableModel<?> tableModel) {
      tableModel.fireTableStructureChanged();
    }
    else if (getModel() instanceof TmmTreeTableModel treeTableModel) {
      treeTableModel.fireTableStructureChanged();
    }

    configureEnclosingScrollPane();

    // the viewport paints the grid lines of the empty rows, but it listens to the columns it knew - which were thrown away above
    if (SwingUtilities.getAncestorOfClass(JScrollPane.class, this) instanceof JScrollPane scrollPane
        && scrollPane.getViewport() instanceof TmmViewport tmmViewport) {
      tmmViewport.updateColumns();
    }
  }

  /**
   * Pushes the current column names of the table format back into the header of the table and repaints it. Needed when the title of a column changes
   * without changing the column structure itself, e.g. the freely configurable columns.
   */
  public void refreshColumnHeaders() {
    if (getModel() instanceof TmmTableModel<?> || getModel() instanceof TmmTreeTableModel) {
      if (getColumnModel() instanceof TmmTableColumnModel tmmTableColumnModel) {
        // all columns, including the hidden ones, to not leave a stale title behind
        for (TableColumn column : tmmTableColumnModel.getAllColumns()) {
          refreshColumnHeader(getModel(), column);
        }
      }
      else {
        for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
          refreshColumnHeader(getModel(), getColumnModel().getColumn(i));
        }
      }

      getTableHeader().repaint();
    }
  }

  private void refreshColumnHeader(TableModel tableModel, TableColumn column) {
    // columns with an icon as header are not affected by a title change
    if (column.getHeaderValue() == null || column.getHeaderValue() instanceof String) {
      column.setHeaderValue(tableModel.getColumnName(column.getModelIndex()));
    }
  }

  /**
   * Gets the list of visible columns with their current widths.
   *
   * @return the list of visible columns with their widths
   */
  public List<ColumnState> getVisibleColumnsWithWidths() {
    List<ColumnState> visibleColumns = new ArrayList<>();

    TableColumnModel columnModel = getColumnModel();
    for (int i = 0; i < columnModel.getColumnCount(); i++) {
      TableColumn col = columnModel.getColumn(i);
      if (col.getIdentifier() instanceof String identifier && StringUtils.isNotBlank(identifier)) {
        visibleColumns.add(new ColumnState(identifier, col.getWidth()));
      }
    }

    return visibleColumns;
  }

  /**
   * Sets the visible columns with their widths. All columns not in the list will be hidden.
   *
   * @param visibleColumns
   *          the list of visible columns with their widths
   */
  public void setVisibleColumnsWithWidths(List<ColumnState> visibleColumns) {
    if (getColumnModel() instanceof TmmTableColumnModel tmmTableColumnModel) {
      columnsAdjusting = true;
      tmmTableColumnModel.setVisibleColumnsWithWidths(visibleColumns);
      columnsAdjusting = false;
    }
  }

  /**
   * Gets the list of hidden column identifiers (legacy method for backward compatibility).
   *
   * @return the list of hidden column identifiers
   * @deprecated Use {@link #getVisibleColumnsWithWidths()} instead
   */
  @Deprecated
  public List<String> getHiddenColumns() {
    List<String> hiddenColumns = new ArrayList<>();

    if (getColumnModel() instanceof TmmTableColumnModel tableColumnModel) {
      List<TableColumn> cols = tableColumnModel.getHiddenColumns();
      for (TableColumn col : cols) {
        if (col.getIdentifier() instanceof String identifier && StringUtils.isNotBlank(identifier)) {
          hiddenColumns.add(identifier);
        }
      }
    }

    return hiddenColumns;
  }

  /**
   * Sets the hidden columns (legacy method for backward compatibility).
   *
   * @param hiddenColumns
   *          the list of column identifiers to hide
   * @deprecated Use {@link #setVisibleColumnsWithWidths(List)} instead
   */
  @Deprecated
  public void readHiddenColumns(List<String> hiddenColumns) {
    if (getColumnModel() instanceof TmmTableColumnModel tmmTableColumnModel) {
      tmmTableColumnModel.setHiddenColumns(hiddenColumns);
    }
  }

  /**
   * Sets the default column visibility based on the table format. Columns marked as default hidden in the table format will be hidden, all others
   * will be visible with their default widths.
   */
  public void setDefaultColumnVisibility() {
    if (getColumnModel() instanceof TmmTableColumnModel tmmTableColumnModel && getModel() instanceof TmmTableModel<?> tableModel) {
      TmmTableFormat<?> tableFormat = (TmmTableFormat<?>) tableModel.getTableFormat();

      List<String> hiddenColumns = new ArrayList<>();

      for (int i = 0; i < tableFormat.getColumnCount(); i++) {
        if (tableFormat.isColumnDefaultHidden(i)) {
          hiddenColumns.add(tableFormat.getColumnIdentifier(i));
        }
      }

      tmmTableColumnModel.setHiddenColumns(hiddenColumns);

      // adjust column widths after setting visibility
      adjustColumnPreferredWidths(3);
    }
  }

  /**
   * Sets the default hidden columns (legacy method for backward compatibility).
   *
   * @deprecated Use {@link #setDefaultColumnVisibility()} instead
   */
  @Deprecated
  public void setDefaultHiddenColumns() {
    setDefaultColumnVisibility();
  }

  /**
   * Set the preferred width of all columns according to its contents If a column is marked as non resizeable, the max-width is set
   *
   * @param margin
   *          the margin left and right
   */
  public void adjustColumnPreferredWidths(int margin) {
    // strategy - get max width for cells in header and column and
    // make that the preferred width
    TableColumnModel columnModel = getColumnModel();
    for (int col = 0; col < getColumnCount(); col++) {

      int maxWidth = 0;
      int minWidth = columnModel.getColumn(col).getMinWidth();

      // header
      TableCellRenderer rend = columnModel.getColumn(col).getHeaderRenderer();
      Object value = columnModel.getColumn(col).getHeaderValue();
      if (rend == null) {
        rend = getTableHeader().getDefaultRenderer();
      }
      Component comp = rend.getTableCellRendererComponent(this, value, false, false, -1, col);
      maxWidth = Math.max(comp.getPreferredSize().width + 2 * margin, maxWidth);

      // rows
      for (int row = 0; row < getRowCount(); row++) {
        try {
          rend = getCellRenderer(row, col);
          value = getValueAt(row, col);
          comp = rend.getTableCellRendererComponent(this, value, false, false, row, col);
          maxWidth = Math.max(comp.getPreferredSize().width + margin, maxWidth);
        }
        catch (Exception e) {
          // catch any issues getting the value
        }
      }

      // do not set the max width below the min width
      if (maxWidth < minWidth) {
        maxWidth = minWidth;
      }

      TableColumn column = columnModel.getColumn(col);
      if (!column.getResizable()) {
        column.setMinWidth(minWidth);
        column.setMaxWidth(maxWidth);
      }

      column.setPreferredWidth(maxWidth);
    }
    resizeAndRepaint();
  }

  public void adjustRowHeights() {
    for (int row = 0; row < getRowCount(); row++) {
      int maxHeight = 0;

      for (int col = 0; col < getColumnCount(); col++) {
        TableCellRenderer renderer = getCellRenderer(row, col);
        Component comp = renderer.getTableCellRendererComponent(this, getValueAt(row, col), false, false, row, col);

        comp.setSize(getColumnModel().getColumn(col).getWidth(), Integer.MAX_VALUE);
        int preferredHeight = comp.getPreferredSize().height;
        maxHeight = Math.max(maxHeight, preferredHeight);
      }

      if (getRowHeight(row) != maxHeight) {
        setRowHeight(row, maxHeight);
      }
    }
  }

  /**
   * Overridden to install special button into the upper right hand corner.
   *
   * @see javax.swing.JTable#configureEnclosingScrollPane()
   */
  @Override
  protected void configureEnclosingScrollPane() {
    super.configureEnclosingScrollPane();

    Container p = getParent();
    if (p instanceof JViewport) {
      Container parent = p.getParent();
      if (parent instanceof JScrollPane scrollPane) {
        scrollPane.setBorder(null);

        // Make certain we are the viewPort's view and not, for
        // example, the rowHeaderView of the scrollPane -
        // an implementor of fixed columns might do this.
        JViewport viewport = scrollPane.getViewport();
        if (viewport == null || viewport.getView() != this) {
          return;
        }

        // just hide it to do not draw the scrollbar but preserve space for the button
        if (scrollPane.getVerticalScrollBarPolicy() == ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER) {
          scrollPane.getVerticalScrollBar().setEnabled(false);
        }

        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);

        if (useColumnConfigurator()) {
          final JButton b = new FlatButton(IconManager.CONFIGURE) {
            @Override
            public void updateUI() {
              super.updateUI();
              setBorder(BorderFactory.createMatteBorder(0, 1, 1, 0, UIManager.getColor("TableHeader.bottomSeparatorColor")));
            }
          };
          b.setContentAreaFilled(false);
          b.setToolTipText(TmmResourceBundle.getString("Button.selectvisiblecolumns"));
          b.updateUI();

          b.addActionListener(evt -> TmmTableColumnSelectionPopup.showColumnSelectionPopup(b, TmmTable.this));
          b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
              scrollPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
              scrollPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseClicked(MouseEvent me) {
              TmmTableColumnSelectionPopup.showColumnSelectionPopup(b, TmmTable.this);
            }
          });
          b.setFocusable(false);
          scrollPane.setCorner(UPPER_RIGHT_CORNER, b);
        }
      }
    }
  }

  protected boolean useColumnConfigurator() {
    return getModel() instanceof TmmTableModel;
  }

  public void configureScrollPane(JScrollPane scrollPane) {
    int[] columnsWithoutRightVerticalGrid = {};
    configureScrollPane(scrollPane, columnsWithoutRightVerticalGrid);
  }

  public void configureScrollPane(JScrollPane scrollPane, int[] columnsWithoutRightVerticalGrid) {
    if (!(scrollPane.getViewport() instanceof TmmViewport)) {
      // NEEDED to repaint the grid of empty rows
      scrollPane.setViewport(new TmmViewport(this, columnsWithoutRightVerticalGrid));
      scrollPane.getViewport().setView(this);
    }
  }

  protected static class IconHeaderRenderer extends DefaultTableCellRenderer {
    public IconHeaderRenderer() {
      setHorizontalAlignment(CENTER);
      setOpaque(true);
    }

    @Override
    public void updateUI() {
      super.updateUI();
      setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
      JTableHeader h = table != null ? table.getTableHeader() : null;

      if (h != null) {
        setEnabled(h.isEnabled());
        setComponentOrientation(h.getComponentOrientation());

        setForeground(h.getForeground());
        setBackground(h.getBackground());
        setFont(h.getFont());
      }
      else {
        /* Use sensible values instead of random leftover values from the last call */
        setEnabled(true);
        setComponentOrientation(ComponentOrientation.UNKNOWN);

        setForeground(UIManager.getColor("TableHeader.foreground"));
        setBackground(UIManager.getColor("TableHeader.background"));
        setFont(UIManager.getFont("TableHeader.font"));
      }

      if (value instanceof ImageIcon imageIcon) {
        setIcon(imageIcon);
        setText("");
      }
      else {
        setText((value == null) ? "" : value.toString());
        setIcon(null);
        setHorizontalAlignment(CENTER);
      }

      return this;
    }
  }

  protected static class SortableIconHeaderRenderer extends JPanel implements TableCellRenderer, SortableRenderer {
    private final TableCellRenderer delegate;
    private final JLabel            labelLeft;
    private final JLabel            labelRight;

    private Icon                    sortIcon;

    public SortableIconHeaderRenderer() {
      delegate = new DefaultTableCellRenderer();
      setLayout(new MigLayout("insets 0, hidemode 3, center", "[]", "[grow]"));
      this.labelLeft = new JLabel();

      add(this.labelLeft, "cell 0 0");
      this.labelRight = new JLabel();
      this.labelRight.setIconTextGap(0);

      add(this.labelRight, "cell 0 0, gapx 1");
    }

    @Override
    public void setSortIcon(Icon sortIcon) {
      this.sortIcon = sortIcon;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
      Component c = delegate.getTableCellRendererComponent(table, value, selected, focused, row, column);
      if (!(c instanceof JLabel label)) {
        return c;
      }

      setForeground(label.getForeground());
      setBackground(label.getBackground());
      setFont(label.getFont());

      labelLeft.setForeground(label.getForeground());
      labelLeft.setBackground(label.getBackground());
      labelLeft.setFont(label.getFont());

      labelRight.setForeground(label.getForeground());
      labelRight.setBackground(label.getBackground());
      labelRight.setFont(label.getFont());

      // move the sort icon to the right label
      if (sortIcon == null) {
        labelRight.setVisible(false);
      }
      else {
        labelRight.setVisible(true);
        labelRight.setIcon(sortIcon);
      }

      if (value instanceof ImageIcon imageIcon) {
        labelLeft.setIcon(imageIcon);
        labelLeft.setText("");
      }
      else {
        labelLeft.setText((value == null) ? "" : value.toString());
        labelLeft.setIcon(null);
      }

      return this;
    }
  }

  /**
   * provide table cell tooltips via our table model
   * 
   * @param e
   *          the mouse event
   * @return the tooltip or null
   */
  @Override
  public String getToolTipText(@NotNull MouseEvent e) {
    if (!(getModel() instanceof TmmTableModel<?> tableModel)) {
      return super.getToolTipText(e);
    }

    Point p = e.getPoint();
    int rowIndex = rowAtPoint(p);
    int colIndex = columnAtPoint(p);
    int realColumnIndex = convertColumnIndexToModel(colIndex);

    return tableModel.getTooltipAt(rowIndex, realColumnIndex);
  }

  @Override
  public Point getToolTipLocation(MouseEvent e) {
    // do not return a coordinate if the tooltip text ist empty
    if (StringUtils.isBlank(getToolTipText(e))) {
      return null;
    }

    Point p = e.getPoint();
    int rowIndex = rowAtPoint(p);
    int colIndex = columnAtPoint(p);
    Rectangle r = getCellRect(rowIndex, colIndex, false);

    return new Point(r.x + 20, r.y + (int) (1.2 * r.height));
  }
}
