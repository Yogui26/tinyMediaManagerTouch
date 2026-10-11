package org.tinymediamanager.ui.components.treetable;

public interface ITmmTreeTableSortingStrategy {
  enum SortDirection {
    ASCENDING,
    DESCENDING
  }

  void columnClicked(int column, boolean shift, boolean control);

  SortDirection getSortDirection(int column);

  /**
   * Gets the index of the column which is currently sorted. Needed to find this column again after the table structure has changed.
   *
   * @return the 0-based index of the sorted column
   */
  int getSortColumn();

  void fromString(String stringEncoded);
}
