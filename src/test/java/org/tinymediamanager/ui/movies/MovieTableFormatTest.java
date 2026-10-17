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

package org.tinymediamanager.ui.movies;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

import org.junit.Test;
import org.tinymediamanager.core.bus.Event;
import org.tinymediamanager.core.bus.EventBus;
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.MovieCustomColumn;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;

public class MovieTableFormatTest extends BasicMovieTest {

  @Test
  public void testCustomColumnsAreMountedAsBeans() {
    MovieSettings settings = MovieModuleManager.getInstance().getSettings();

    MovieCustomColumn first = new MovieCustomColumn("first", "${title}");
    MovieCustomColumn second = new MovieCustomColumn("second", "${year}");
    settings.setCustomColumns(new ArrayList<>(Arrays.asList(first, second)));

    MovieTableFormat tableFormat = new MovieTableFormat();
    int indexFirst = tableFormat.getColumnIndex(first.getColumnIdentifier());
    int indexSecond = tableFormat.getColumnIndex(second.getColumnIdentifier());

    assertThat(indexFirst).isNotEqualTo(-1);
    assertThat(indexSecond).isNotEqualTo(-1);
    assertThat(tableFormat.getColumnName(indexFirst)).isEqualTo("first");
    assertThat(tableFormat.getColumnName(indexSecond)).isEqualTo("second");

    // a new title is no structural change, so it shows up without any rebuild
    first.setTitle("renamed");
    assertThat(tableFormat.getColumnName(indexFirst)).isEqualTo("renamed");

    // but a reorder does not touch the mounted columns until the table actually gets rebuilt
    settings.swapCustomColumns(0, 1);
    assertThat(tableFormat.getColumnName(indexFirst)).isEqualTo("renamed");
    assertThat(tableFormat.getColumnName(indexSecond)).isEqualTo("second");

    tableFormat.rebuildCustomColumns();
    assertThat(tableFormat.getColumnIndex(second.getColumnIdentifier())).isLessThan(tableFormat.getColumnIndex(first.getColumnIdentifier()));
  }

  @Test
  public void testCustomColumnsSurviveAShrunkenSettingsList() {
    MovieSettings settings = MovieModuleManager.getInstance().getSettings();

    MovieCustomColumn first = new MovieCustomColumn("first", "${title}");
    MovieCustomColumn second = new MovieCustomColumn("second", "${year}");
    settings.setCustomColumns(new ArrayList<>(Arrays.asList(first, second)));

    MovieTableFormat tableFormat = new MovieTableFormat();

    // the settings could already have lost a column while the table still shows the old structure - that must not run off the list
    settings.removeCustomColumn(second);
    assertThat(tableFormat.getColumnName(tableFormat.getColumnIndex(first.getColumnIdentifier()))).isEqualTo("first");
    // the mounted column keeps its own meaning, it does not silently become its neighbour
    assertThat(tableFormat.getColumnName(tableFormat.getColumnIndex(second.getColumnIdentifier()))).isEqualTo("second");

    tableFormat.rebuildCustomColumns();
    assertThat(tableFormat.getColumnIndex(second.getColumnIdentifier())).isEqualTo(-1);
    assertThat(tableFormat.getColumnName(tableFormat.getColumnIndex(first.getColumnIdentifier()))).isEqualTo("first");
  }

  @Test
  public void testColumnValuesAreCachedAndDroppedOnChange() throws Exception {
    Movie movie = new Movie();
    movie.setTitle("Aladdin");
    movie.setYear(1992);

    MovieTableFormat tableFormat = new MovieTableFormat();
    int yearIndex = tableFormat.getColumnIndex("year");

    assertThat(tableFormat.getColumnValue(movie, yearIndex)).isEqualTo(1992);

    // without a change notification the cached value is used
    movie.setYear(2000);
    assertThat(tableFormat.getColumnValue(movie, yearIndex)).isEqualTo(1992);

    // a change event on the event bus drops the cached value
    EventBus.publishEvent(EventBus.TOPIC_MOVIES, Event.createSaveEvent(movie));
    awaitValue(() -> tableFormat.getColumnValue(movie, yearIndex), 2000);
  }

  private void awaitValue(Callable<Object> callable, Object expected) throws Exception {
    long deadline = System.currentTimeMillis() + 10000;
    while (System.currentTimeMillis() < deadline) {
      if (expected.equals(callable.call())) {
        return;
      }
      Thread.sleep(50);
    }
    assertThat(callable.call()).isEqualTo(expected);
  }
}
