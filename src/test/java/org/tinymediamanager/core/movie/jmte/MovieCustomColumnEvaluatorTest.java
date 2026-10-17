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

package org.tinymediamanager.core.movie.jmte;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.bus.Event;
import org.tinymediamanager.core.bus.EventBus;
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.MovieCustomColumn;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;

public class MovieCustomColumnEvaluatorTest extends BasicMovieTest {

  private static final long          TIMEOUT_MS = 10000;

  private MovieSettings              settings;

  private MovieCustomColumnEvaluator evaluator;

  private Movie                      aladdin;
  private Movie                      gladiator;

  @Before
  public void setupEvaluator() {
    settings = MovieModuleManager.getInstance().getSettings();
    settings.setCustomColumns(new ArrayList<>());
    settings.setMovieSetCustomColumns(new ArrayList<>());

    evaluator = new MovieCustomColumnEvaluator();

    aladdin = new Movie();
    aladdin.setTitle("Aladdin");
    aladdin.setOriginalTitle("Aladdin");
    aladdin.setYear(1992);

    gladiator = new Movie();
    gladiator.setTitle("Gladiator");
    gladiator.setOriginalTitle("Gladiator");
    gladiator.setYear(2000);
  }

  @After
  public void teardownEvaluator() {
    evaluator.dispose();
  }

  @Test
  public void testUnconfiguredColumnRendersEmpty() {
    MovieCustomColumn customColumn = new MovieCustomColumn("whatever", "   ");

    assertThat(customColumn.isConfigured()).isFalse();
    assertThat(evaluator.evaluate(aladdin, customColumn)).isEmpty();
  }

  @Test
  public void testLegacyTokensAreNotExpanded() {
    MovieCustomColumn customColumn = new MovieCustomColumn("Header", "${title}");

    // only full JMTE patterns are supported, the short renamer tokens are not expanded here
    assertThat(evaluator.evaluate(aladdin, customColumn)).isEmpty();
    assertThat(evaluator.evaluate(gladiator, customColumn)).isEmpty();
  }

  @Test
  public void testJmteTokensAndRenderers() {
    MovieCustomColumn upper = new MovieCustomColumn("upper", "${movie.title;upper}");
    MovieCustomColumn year = new MovieCustomColumn("year", "${movie.year}");

    assertThat(evaluator.evaluate(aladdin, upper)).isEqualTo("ALADDIN");
    assertThat(evaluator.evaluate(aladdin, year)).isEqualTo("1992");
  }

  @Test
  public void testInvalidPatternRendersEmptyAndDoesNotThrow() {
    MovieCustomColumn customColumn = new MovieCustomColumn("broken", "${movie.thisIsNoPropertyOfMovie}");

    assertThat(evaluator.evaluate(aladdin, customColumn)).isEmpty();
    // and it stays empty, a broken pattern must not be retried over and over again
    assertThat(evaluator.evaluate(gladiator, customColumn)).isEmpty();
  }

  @Test
  public void testPatternOfAnotherColumnIsNotAffected() {
    MovieCustomColumn first = new MovieCustomColumn("first", "${movie.title}");
    MovieCustomColumn second = new MovieCustomColumn("second", "${movie.year}");
    settings.addCustomColumn(first);
    settings.addCustomColumn(second);

    assertThat(evaluator.evaluate(aladdin, first)).isEqualTo("Aladdin");
    assertThat(evaluator.evaluate(aladdin, second)).isEqualTo("1992");
  }

  @Test
  public void testValuesAreCachedPerMovie() throws Exception {
    MovieCustomColumn customColumn = new MovieCustomColumn("Header", "${movie.title}");
    settings.addCustomColumn(customColumn);

    // the structural change is announced asynchronously and drops all caches, so wait until that has happened
    Thread.sleep(200);

    assertThat(evaluator.evaluate(aladdin, customColumn)).isEqualTo("Aladdin");

    // without a change notification the cached value is being used
    aladdin.setTitle("Aladdin 2");
    assertThat(evaluator.evaluate(aladdin, customColumn)).isEqualTo("Aladdin");

    evaluator.invalidate(aladdin);
    assertThat(evaluator.evaluate(aladdin, customColumn)).isEqualTo("Aladdin 2");
  }

  @Test
  public void testMovieChangeEventDropsTheCachedValue() throws Exception {
    MovieCustomColumn customColumn = new MovieCustomColumn("Header", "${movie.title}");
    settings.addCustomColumn(customColumn);
    assertThat(evaluator.evaluate(aladdin, customColumn)).isEqualTo("Aladdin");

    aladdin.setTitle("Aladdin 2");
    EventBus.publishEvent(EventBus.TOPIC_MOVIES, Event.createSaveEvent(aladdin));

    // the event bus collects events for a short while before dispatching them
    awaitValue(() -> evaluator.evaluate(aladdin, customColumn), "Aladdin 2");
  }

  @Test
  public void testPatternChangeDropsTheCachedValue() throws Exception {
    MovieCustomColumn customColumn = new MovieCustomColumn("Header", "${movie.title}");
    settings.addCustomColumn(customColumn);
    assertThat(evaluator.evaluate(aladdin, customColumn)).isEqualTo("Aladdin");

    customColumn.setPattern("${movie.year}");

    awaitValue(() -> evaluator.evaluate(aladdin, customColumn), "1992");
  }

  @Test
  public void testPlaceholderTitleUsesThePosition() {
    MovieCustomColumn first = new MovieCustomColumn(null, "${movie.title}");
    MovieCustomColumn second = new MovieCustomColumn(null, "${movie.year}");
    settings.addCustomColumn(first);
    settings.addCustomColumn(second);

    String prefix = TmmResourceBundle.getString("Table.columns.custom");
    assertThat(first.getTitleOrPlaceholder(1)).isEqualTo(prefix + " 1");
    assertThat(second.getTitleOrPlaceholder(2)).isEqualTo(prefix + " 2");

    first.setTitle("Mine");
    assertThat(first.getTitleOrPlaceholder(1)).isEqualTo("Mine");
  }

  @Test
  public void testColumnIdentifierIsStableAcrossAddAndRemove() {
    MovieCustomColumn first = new MovieCustomColumn("first", "${movie.title}");
    MovieCustomColumn second = new MovieCustomColumn("second", "${movie.year}");
    settings.addCustomColumn(first);
    settings.addCustomColumn(second);

    String identifierOfSecond = second.getColumnIdentifier();
    assertThat(identifierOfSecond).startsWith(MovieCustomColumn.COLUMN_IDENTIFIER_PREFIX);

    settings.removeCustomColumn(first);

    // the surviving column keeps its identifier, so the user keeps its width and visibility within the table
    assertThat(second.getColumnIdentifier()).isEqualTo(identifierOfSecond);
    assertThat(settings.getCustomColumns()).containsExactly(second);
  }

  @Test
  public void testStructureAndContentChangesFireDifferentEvents() throws Exception {
    final List<String> properties = new ArrayList<>();
    settings.addPropertyChangeListener(event -> properties.add(event.getPropertyName()));

    MovieCustomColumn customColumn = new MovieCustomColumn();
    settings.addCustomColumn(customColumn);
    Thread.sleep(200);
    assertThat(properties).contains(MovieSettings.CUSTOM_COLUMNS);

    properties.clear();
    customColumn.setPattern("${movie.title}");
    Thread.sleep(200);
    assertThat(properties).contains(MovieSettings.CUSTOM_COLUMN_CONTENT);
    assertThat(properties).doesNotContain(MovieSettings.CUSTOM_COLUMNS);
  }

  @Test
  public void testCustomColumnsGetPersisted() throws Exception {
    MovieCustomColumn customColumn = new MovieCustomColumn("Watchlist note", "${movie.note}");
    settings.addCustomColumn(customColumn);

    // let the dirty flag set by the async propertychange listener
    Thread.sleep(100);
    settings.saveSettings();

    String config = Utils.readFileToString(getSettingsFolder().resolve(MovieModuleManager.getInstance().getSettings().getConfigFilename()));
    assertThat(config).contains("\"title\" : \"Watchlist note\"");
    assertThat(config).contains("\"pattern\" : \"${movie.note}\"");
    assertThat(config).contains("\"id\" : \"" + customColumn.getId() + "\"");
  }

  @Test
  public void testLegacyIndexOnlyEntriesStillLoad() {
    // a hand-tuned or outdated entry without an id simply gets a fresh one
    MovieCustomColumn legacy = new MovieCustomColumn();
    legacy.setId(null);
    legacy.setPattern("${title}");

    settings.addCustomColumn(legacy);
    settings.setCustomColumns(new ArrayList<>(settings.getCustomColumns()));

    assertThat(settings.getCustomColumns().get(0).getColumnIdentifier()).isNotBlank();
  }

  @Test
  public void testSetLevelPatternUsesTheMovieSetContext() {
    MovieSet movieSet = new MovieSet("Pirates of the Caribbean");
    MovieCustomColumn customColumn = new MovieCustomColumn("set", "${movie.title}");
    customColumn.setSetPattern("the ${movieSet.title}");

    // the set level has no movie in its context, only the set itself
    assertThat(evaluator.evaluate(movieSet, customColumn)).isEqualTo("the Pirates of the Caribbean");
  }

  @Test
  public void testUnconfiguredSetPatternRendersEmpty() {
    MovieSet movieSet = new MovieSet("Pirates of the Caribbean");
    MovieCustomColumn customColumn = new MovieCustomColumn("set", "${movie.title}");

    // the movie level pattern says nothing about the set level, and a set without a pattern of its own stays empty
    assertThat(customColumn.isConfigured()).isTrue();
    assertThat(customColumn.isConfiguredForSets()).isTrue();
    assertThat(evaluator.evaluate(movieSet, customColumn)).isEmpty();
  }

  @Test
  public void testLegacyTokensDoNotWorkOnTheSetLevel() {
    MovieSet movieSet = new MovieSet("Pirates of the Caribbean");
    movieSet.setPlot("Captain Jack Sparrow");

    MovieCustomColumn legacyTokens = new MovieCustomColumn("set", "");
    legacyTokens.setSetPattern("${title}");
    assertThat(evaluator.evaluate(movieSet, legacyTokens)).isEmpty();

    MovieCustomColumn setProperty = new MovieCustomColumn("set", "");
    setProperty.setSetPattern("${movieSet.plot}");
    assertThat(evaluator.evaluate(movieSet, setProperty)).isEqualTo("Captain Jack Sparrow");
  }

  @Test
  public void testSetLevelAndMovieLevelAreKeptApart() {
    MovieSet movieSet = new MovieSet("Pirates of the Caribbean");
    aladdin.setMovieSet(movieSet);

    MovieCustomColumn customColumn = new MovieCustomColumn("set", "${movieSet.title}");
    customColumn.setSetPattern("set level");

    // the pattern of the set level is simply not used for a movie row
    assertThat(evaluator.evaluate(aladdin, customColumn)).isEqualTo("Pirates of the Caribbean");
    assertThat(evaluator.evaluate(movieSet, customColumn)).isEqualTo("set level");
  }

  @Test
  public void testMovieRowOfAMovieSetKnowsItsParentSet() {
    MovieSet movieSet = new MovieSet("Pirates of the Caribbean");
    MovieSet.MovieSetMovie dummy = new MovieSet.MovieSetMovie();
    dummy.setTitle("Dead Man's Chest");

    MovieCustomColumn customColumn = new MovieCustomColumn("movie", "${movie.title} / ${movieSet.title}");

    // a missing movie of a set does not know its set, so the caller has to hand it over
    assertThat(evaluator.evaluate(dummy, customColumn)).isEqualTo("Dead Man's Chest / ");
    assertThat(evaluator.evaluate(dummy, movieSet, customColumn)).isEqualTo("Dead Man's Chest / Pirates of the Caribbean");
  }

  @Test
  public void testSetPatternChangeDropsTheCachedValue() throws Exception {
    MovieSet movieSet = new MovieSet("Pirates of the Caribbean");
    MovieCustomColumn customColumn = new MovieCustomColumn("set", "");
    customColumn.setSetPattern("${movieSet.title}");
    settings.addMovieSetCustomColumn(customColumn);
    assertThat(evaluator.evaluate(movieSet, customColumn)).isEqualTo("Pirates of the Caribbean");

    customColumn.setSetPattern("the ${movieSet.title}");

    awaitValue(() -> evaluator.evaluate(movieSet, customColumn), "the Pirates of the Caribbean");
  }

  @Test
  public void testMovieSetChangeEventDropsTheCachedValue() throws Exception {
    MovieSet movieSet = new MovieSet("Pirates of the Caribbean");
    MovieCustomColumn customColumn = new MovieCustomColumn("set", "");
    customColumn.setSetPattern("${movieSet.title}");
    settings.addMovieSetCustomColumn(customColumn);
    assertThat(evaluator.evaluate(movieSet, customColumn)).isEqualTo("Pirates of the Caribbean");

    movieSet.setTitle("Pirates");
    EventBus.publishEvent(EventBus.TOPIC_MOVIE_SETS, Event.createSaveEvent(movieSet));

    awaitValue(() -> evaluator.evaluate(movieSet, customColumn), "Pirates");
  }

  @Test
  public void testMovieSetChangeEventDropsTheCachedValueOfMovieRows() throws Exception {
    MovieSet movieSet = new MovieSet("Pirates of the Caribbean");
    aladdin.setMovieSet(movieSet);

    MovieCustomColumn customColumn = new MovieCustomColumn("set", "${movieSet.title}");
    settings.addMovieSetCustomColumn(customColumn);
    assertThat(evaluator.evaluate(aladdin, customColumn)).isEqualTo("Pirates of the Caribbean");

    movieSet.setTitle("Pirates");
    EventBus.publishEvent(EventBus.TOPIC_MOVIE_SETS, Event.createSaveEvent(movieSet));

    // a movie row rendered in the context of the set has to be dropped too
    awaitValue(() -> evaluator.evaluate(aladdin, customColumn), "Pirates");
  }

  /**
   * Movie change events and settings property changes are dispatched asynchronously, so the result has to be polled instead of sleeping a fixed
   * amount of time.
   *
   * @param callable
   *          the value to wait for
   * @param expected
   *          the expected value
   * @throws Exception
   *           when the value never showed up in time
   */
  private void awaitValue(Callable<String> callable, String expected) throws Exception {
    long deadline = System.currentTimeMillis() + TIMEOUT_MS;
    while (System.currentTimeMillis() < deadline) {
      if (expected.equals(callable.call())) {
        return;
      }
      Thread.sleep(50);
    }

    assertThat(callable.call()).isEqualTo(expected);
  }
}
