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

package org.tinymediamanager.ui.moviesets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.MovieCustomColumn;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;

public class MovieSetTableFormatTest extends BasicMovieTest {

  private MovieSettings settings;

  @Before
  public void setupSettings() {
    settings = MovieModuleManager.getInstance().getSettings();
    settings.setMovieSetCustomColumns(new ArrayList<>());
  }

  @After
  public void resetSettings() {
    settings.setMovieSetCustomColumns(new ArrayList<>());
  }

  @Test
  public void testCustomColumnsAreMountedAsBeans() {
    MovieCustomColumn first = new MovieCustomColumn("first", "${movie.title}");
    MovieCustomColumn second = new MovieCustomColumn("second", "${movieSet.title}");
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(first, second)));

    MovieSetTableFormat tableFormat = new MovieSetTableFormat();
    int indexFirst = tableFormat.getColumnIndex(first.getColumnIdentifier());
    int indexSecond = tableFormat.getColumnIndex(second.getColumnIdentifier());

    assertThat(indexFirst).isNotEqualTo(-1);
    assertThat(indexSecond).isNotEqualTo(-1);
    assertThat(tableFormat.getColumnName(indexFirst)).isEqualTo("first");

    // a new title is no structural change, it shows up without any rebuild
    first.setTitle("renamed");
    assertThat(tableFormat.getColumnName(indexFirst)).isEqualTo("renamed");

    // but a reorder does not touch the mounted columns until the table actually gets rebuilt
    settings.swapMovieSetCustomColumns(0, 1);
    assertThat(tableFormat.getColumnName(indexFirst)).isEqualTo("renamed");
    assertThat(tableFormat.getColumnName(indexSecond)).isEqualTo("second");

    tableFormat.rebuildCustomColumns();
    assertThat(tableFormat.getColumnIndex(second.getColumnIdentifier())).isLessThan(tableFormat.getColumnIndex(first.getColumnIdentifier()));
  }

  @Test
  public void testCustomColumnsFollowTheirLevel() {
    MovieSet movieSet = new MovieSet("Pirates of the Caribbean");

    Movie movie = new Movie();
    movie.setTitle("Dead Man's Chest");
    // a missing movie of a set does not know its set, the tree has to hand it over
    MovieSet.MovieSetMovie dummy = new MovieSet.MovieSetMovie();
    dummy.setTitle("At World's End");

    TmmTreeNode setNode = new TmmTreeNode(movieSet, null);
    TmmTreeNode movieNode = new TmmTreeNode(movie, null);
    TmmTreeNode dummyNode = new TmmTreeNode(dummy, null);
    setNode.add(movieNode);
    setNode.add(dummyNode);

    MovieCustomColumn both = new MovieCustomColumn("both", "${movie.title}");
    both.setSetPattern("${movieSet.title}");
    MovieCustomColumn movieOnly = new MovieCustomColumn("movie only", "${movie.title}");
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(both, movieOnly)));

    MovieSetTableFormat tableFormat = new MovieSetTableFormat();
    int indexBoth = tableFormat.getColumnIndex(both.getColumnIdentifier());
    int indexMovieOnly = tableFormat.getColumnIndex(movieOnly.getColumnIdentifier());

    assertThat(tableFormat.getColumnValue(setNode, indexBoth)).isEqualTo("Pirates of the Caribbean");
    assertThat(tableFormat.getColumnValue(movieNode, indexBoth)).isEqualTo("Dead Man's Chest");
    assertThat(tableFormat.getColumnValue(dummyNode, indexBoth)).isEqualTo("At World's End");

    // a column which only knows the movie level is empty on a set row - but still worth showing
    assertThat(tableFormat.getColumnValue(setNode, indexMovieOnly)).isEqualTo("");
    assertThat(tableFormat.getColumnValue(movieNode, indexMovieOnly)).isEqualTo("Dead Man's Chest");
  }

  @Test
  public void testUnconfiguredCustomColumnIsNotOffered() {
    MovieCustomColumn configuredForSets = new MovieCustomColumn("sets", "");
    configuredForSets.setSetPattern("${movieSet.title}");
    MovieCustomColumn untouched = new MovieCustomColumn();

    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(configuredForSets, untouched)));

    MovieSetTableFormat tableFormat = new MovieSetTableFormat();
    assertThat(tableFormat.isColumnEnabled(tableFormat.getColumnIndex(configuredForSets.getColumnIdentifier()))).isTrue();
    assertThat(tableFormat.isColumnEnabled(tableFormat.getColumnIndex(untouched.getColumnIdentifier()))).isFalse();

    // the flat movie table does not care about the set level at all
    assertThat(configuredForSets.isConfigured()).isFalse();
  }

  @Test
  public void testCustomColumnsArePlacedBeforeTheIconColumns() {
    MovieCustomColumn customColumn = new MovieCustomColumn("custom", "${movie.title}");
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(customColumn)));

    MovieSetTableFormat tableFormat = new MovieSetTableFormat();
    int customColumnIndex = tableFormat.getColumnIndex(customColumn.getColumnIdentifier());
    int metadataColumn = tableFormat.getColumnIndex("metadata");

    assertThat(customColumnIndex).isNotEqualTo(-1);

    // the custom columns join the metadata columns, the icons stay at the very end
    assertThat(customColumnIndex).isLessThan(metadataColumn);
    assertThat(tableFormat.getColumnIndex("watched")).isGreaterThan(metadataColumn);

    settings.setMovieSetCustomColumns(new ArrayList<>());
    assertThat(new MovieSetTableFormat().getColumnIndex("metadata")).isEqualTo(metadataColumn - 1);

    // the tree column of the table itself is never touched, it keeps its index independent title
    assertThat(tableFormat.getColumnName(-1)).isEqualTo(TmmResourceBundle.getString("metatag.title"));
  }
}
