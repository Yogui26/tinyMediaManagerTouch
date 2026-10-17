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

package org.tinymediamanager.ui.components.treetable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.MovieCustomColumn;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableColumnModel;
import org.tinymediamanager.ui.moviesets.MovieSetTableFormat;
import org.tinymediamanager.ui.moviesets.MovieSetTreeDataProvider;

/**
 * The tree table has no column structure which is fixed for its lifetime anymore - these tests make sure that a rebuild really recreates everything
 * from the table format, just like the movie set panel does when the user configures a custom column.
 */
public class TmmTreeTableColumnRebuildTest extends BasicMovieTest {

  private MovieSettings       settings;

  private MovieSetTableFormat tableFormat;

  private TmmTreeTable        tree;

  @Before
  public void setupTree() {
    settings = MovieModuleManager.getInstance().getSettings();
    settings.setMovieSetCustomColumns(new ArrayList<>());

    tableFormat = new MovieSetTableFormat();
    tree = new TmmTreeTable(new MovieSetTreeDataProvider(tableFormat), tableFormat);
  }

  @After
  public void resetSettings() {
    settings.setMovieSetCustomColumns(new ArrayList<>());
  }

  @Test
  public void testRebuildCreatesTheColumnsOfTheNewStructure() {
    int columnsBefore = tree.getColumnCount();

    MovieCustomColumn customColumn = new MovieCustomColumn("custom", "${movie.title}");
    customColumn.setSetPattern("${movieSet.title}");
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(customColumn)));

    rebuild();

    assertThat(tree.getColumnCount()).isEqualTo(columnsBefore + 1);
    assertThat(findColumn(customColumn.getColumnIdentifier())).isNotNull();

    // the tree column is still the very first one and still knows itself
    assertThat(tree.getColumnModel().getColumn(0).getIdentifier()).isEqualTo("node");
  }

  @Test
  public void testRebuildDropsColumnsWhichAreGone() {
    MovieCustomColumn customColumn = new MovieCustomColumn("custom", "${movie.title}");
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(customColumn)));

    rebuild();
    assertThat(findColumn(customColumn.getColumnIdentifier())).isNotNull();

    settings.setMovieSetCustomColumns(new ArrayList<>());
    rebuild();

    assertThat(findColumn(customColumn.getColumnIdentifier())).isNull();
  }

  @Test
  public void testRebuildKeepsWidthAndVisibilityOfTheSurvivingColumns() {
    MovieCustomColumn first = new MovieCustomColumn("first", "${movie.title}");
    MovieCustomColumn second = new MovieCustomColumn("second", "${movieSet.title}");
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(first, second)));

    rebuild();

    TableColumn firstColumn = findColumn(first.getColumnIdentifier());
    assertThat(firstColumn).isNotNull();
    firstColumn.setWidth(123);

    // hide one column, exactly like the user does it in the column chooser
    ((TmmTableColumnModel) tree.getColumnModel()).setColumnHidden(findColumn(second.getColumnIdentifier()), true);

    // remembering the state by identifier is what survives the rebuild
    List<TmmTable.ColumnState> visibleColumns = new ArrayList<>(tree.getVisibleColumnsWithWidths());
    assertThat(identifiersOf(visibleColumns)).doesNotContain(second.getColumnIdentifier());

    // and now one more column joins
    MovieCustomColumn third = new MovieCustomColumn("third", "${movie.year}");
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(first, second, third)));

    rebuild();

    // the freshly added column has no stored state, so the panel has to ask for it explicitly
    visibleColumns.add(new TmmTable.ColumnState(third.getColumnIdentifier(), 0));
    tree.setVisibleColumnsWithWidths(visibleColumns);

    assertThat(findColumn(first.getColumnIdentifier()).getWidth()).isEqualTo(123);
    assertThat(findColumn(second.getColumnIdentifier())).isNull();
    assertThat(findColumn(third.getColumnIdentifier())).isNotNull();
  }

  @Test
  public void testRebuildDecoratesTheNewHeadersForSorting() {
    MovieCustomColumn customColumn = new MovieCustomColumn("custom", "${movie.title}");
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(customColumn)));

    // the sorter wraps the header renderers of all columns, and a freshly created column knows nothing about that
    TmmTreeTableComparatorChooser.install(tree);
    rebuild();

    TableColumn column = findColumn(customColumn.getColumnIdentifier());
    assertThat(column).isNotNull();
    assertThat(column.getHeaderRenderer()).isInstanceOf(TmmTreeTableComparatorChooser.SortArrowHeaderRenderer.class);
  }

  @Test
  public void testRebuildKeepsTheCellsInSyncWithThePaintedSeparators() {
    // the look and feel gives our tables a column margin of its own, while the viewport paints its separators purely from the column widths
    tree.getColumnModel().setColumnMargin(0);

    MovieCustomColumn customColumn = new MovieCustomColumn("custom", "${movie.title}");
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(customColumn)));
    rebuild();

    // a rebuilt column model must not fall back to the Swing default margin, or every cell ends up left of its separator line
    assertThat(tree.getColumnModel().getColumnMargin()).isZero();

    int x = 0;
    for (int i = 0; i < tree.getColumnCount() - 1; i++) {
      x += tree.getColumnModel().getColumn(i).getWidth();
      assertThat(tree.getCellRect(0, i + 1, false).x).isEqualTo(x);
    }
  }

  @Test
  public void testRebuildHandsTheNewColumnsToTheViewport() {
    JScrollPane scrollPane = new JScrollPane();
    tree.configureScrollPane(scrollPane);

    // the viewport paints the grid lines below and beside the last row, and it only keeps them in sync while it listens to every column
    int listenersOfOneColumn = tree.getColumnModel().getColumn(0).getPropertyChangeListeners().length;
    assertThat(listenersOfOneColumn).isGreaterThan(0);

    MovieCustomColumn customColumn = new MovieCustomColumn("custom", "${movie.title}");
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(customColumn)));
    rebuild();

    for (int i = 0; i < tree.getColumnCount(); i++) {
      assertThat(tree.getColumnModel().getColumn(i).getPropertyChangeListeners()).hasSize(listenersOfOneColumn);
    }
  }

  /**
   * The table only accepts a new column structure on the event dispatch thread, exactly like the settings events which trigger it do.
   */
  private void rebuild() {
    try {
      SwingUtilities.invokeAndWait(() -> {
        tableFormat.rebuildCustomColumns();
        tree.rebuildColumnsFromModel();
      });
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private TableColumn findColumn(String identifier) {
    for (int i = 0; i < tree.getColumnCount(); i++) {
      TableColumn column = tree.getColumnModel().getColumn(i);
      if (identifier.equals(column.getIdentifier())) {
        return column;
      }
    }
    return null;
  }

  private static List<String> identifiersOf(List<TmmTable.ColumnState> columnStates) {
    List<String> identifiers = new ArrayList<>();
    for (TmmTable.ColumnState columnState : columnStates) {
      identifiers.add(columnState.getIdentifier());
    }
    return identifiers;
  }
}
