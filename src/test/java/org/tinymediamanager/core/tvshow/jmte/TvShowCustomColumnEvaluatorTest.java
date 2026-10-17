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

package org.tinymediamanager.core.tvshow.jmte;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.bus.Event;
import org.tinymediamanager.core.bus.EventBus;
import org.tinymediamanager.core.tvshow.BasicTvShowTest;
import org.tinymediamanager.core.tvshow.TvShowCustomColumn;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;

public class TvShowCustomColumnEvaluatorTest extends BasicTvShowTest {

  private static final long           TIMEOUT_MS = 10000;

  private TvShowSettings              settings;

  private TvShowCustomColumnEvaluator evaluator;

  private TvShow                      aladdin;
  private TvShowSeason                season;
  private TvShowEpisode               episode;

  @Before
  public void setupEvaluator() {
    settings = TvShowModuleManager.getInstance().getSettings();
    settings.setCustomColumns(new ArrayList<>());

    evaluator = new TvShowCustomColumnEvaluator();

    aladdin = new TvShow();
    aladdin.setTitle("Aladdin");

    episode = new TvShowEpisode();
    episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 3));
    episode.setTitle("Don't Pet the Teacher");
    episode.setTvShow(aladdin);
    aladdin.addEpisode(episode);

    season = episode.getTvShowSeason();
  }

  @After
  public void teardownEvaluator() {
    evaluator.dispose();
  }

  @Test
  public void testUnconfiguredColumnRendersEmptyOnEveryLevel() {
    TvShowCustomColumn customColumn = new TvShowCustomColumn("whatever", "   ");

    assertThat(customColumn.isConfigured()).isFalse();
    assertThat(evaluator.evaluate(aladdin, customColumn)).isEmpty();
    assertThat(evaluator.evaluate(season, customColumn)).isEmpty();
    assertThat(evaluator.evaluate(episode, customColumn)).isEmpty();
  }

  @Test
  public void testEveryLevelUsesItsOwnPattern() {
    TvShowCustomColumn customColumn = new TvShowCustomColumn("Header", "${tvShow.title}");
    customColumn.setSeasonPattern("season ${season.season}");
    customColumn.setEpisodePattern("${episode.title}");

    assertThat(evaluator.evaluate(aladdin, customColumn)).isEqualTo("Aladdin");
    assertThat(evaluator.evaluate(season, customColumn)).isEqualTo("season 1");
    assertThat(evaluator.evaluate(episode, customColumn)).isEqualTo("Don't Pet the Teacher");
  }

  @Test
  public void testEpisodeAndSeasonRowsAlsoKnowTheirShow() {
    TvShowCustomColumn customColumn = new TvShowCustomColumn("", "${tvShow.title}");
    customColumn.setSeasonPattern("${tvShow.title}");
    customColumn.setEpisodePattern("${tvShow.title}");

    assertThat(evaluator.evaluate(season, customColumn)).isEqualTo("Aladdin");
    assertThat(evaluator.evaluate(episode, customColumn)).isEqualTo("Aladdin");
  }

  @Test
  public void testLegacyTokensAreNotExpanded() {
    TvShowCustomColumn customColumn = new TvShowCustomColumn("show", "${showTitle}");
    customColumn.setSeasonPattern("${seasonName}");
    customColumn.setEpisodePattern("${title}");

    // only full JMTE patterns are supported, the short renamer tokens are not expanded here
    assertThat(evaluator.evaluate(aladdin, customColumn)).isEmpty();
    assertThat(evaluator.evaluate(season, customColumn)).isEmpty();
    assertThat(evaluator.evaluate(episode, customColumn)).isEmpty();
  }

  @Test
  public void testInvalidPatternRendersEmptyAndDoesNotThrow() {
    TvShowCustomColumn customColumn = new TvShowCustomColumn("broken", "${tvShow.thisIsNoPropertyOfTvShow}");

    assertThat(evaluator.evaluate(aladdin, customColumn)).isEmpty();
    // and it stays empty, a broken pattern must not be retried over and over again
    assertThat(evaluator.evaluate(episode, customColumn)).isEmpty();
  }

  @Test
  public void testValuesAreCachedPerEntity() throws Exception {
    TvShowCustomColumn customColumn = new TvShowCustomColumn("Header", "${tvShow.title}");
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
  public void testShowChangeInvalidatesSeasonsAndEpisodes() {
    TvShowCustomColumn customColumn = new TvShowCustomColumn("Header", "${tvShow.title}");
    customColumn.setSeasonPattern("${tvShow.title}");
    customColumn.setEpisodePattern("${tvShow.title}");
    settings.addCustomColumn(customColumn);

    assertThat(evaluator.evaluate(season, customColumn)).isEqualTo("Aladdin");
    assertThat(evaluator.evaluate(episode, customColumn)).isEqualTo("Aladdin");

    aladdin.setTitle("Aladdin 2");
    evaluator.invalidate(aladdin);

    // a season or an episode row is rendered with data of its show, so changing the show has to invalidate them too
    assertThat(evaluator.evaluate(season, customColumn)).isEqualTo("Aladdin 2");
    assertThat(evaluator.evaluate(episode, customColumn)).isEqualTo("Aladdin 2");
  }

  @Test
  public void testEpisodeChangeEventDropsTheCachedValue() throws Exception {
    TvShowCustomColumn customColumn = new TvShowCustomColumn("Header", "");
    customColumn.setEpisodePattern("${episode.title}");
    settings.addCustomColumn(customColumn);
    assertThat(evaluator.evaluate(episode, customColumn)).isEqualTo("Don't Pet the Teacher");

    episode.setTitle("Abu");

    EventBus.publishEvent(EventBus.TOPIC_TV_SHOWS, Event.createSaveEvent(episode));
    awaitValue(() -> evaluator.evaluate(episode, customColumn), "Abu");
  }

  @Test
  public void testPatternChangeDropsTheCachedValue() throws Exception {
    TvShowCustomColumn customColumn = new TvShowCustomColumn("Header", "${tvShow.title}");
    settings.addCustomColumn(customColumn);
    assertThat(evaluator.evaluate(aladdin, customColumn)).isEqualTo("Aladdin");

    customColumn.setShowPattern("the ${tvShow.title}");

    awaitValue(() -> evaluator.evaluate(aladdin, customColumn), "the Aladdin");
  }

  @Test
  public void testPlaceholderTitleUsesThePosition() {
    TvShowCustomColumn first = new TvShowCustomColumn(null, "${tvShow.title}");
    TvShowCustomColumn second = new TvShowCustomColumn(null, "${tvShow.title}");

    String prefix = TmmResourceBundle.getString("Table.columns.custom");
    assertThat(first.getTitleOrPlaceholder(1)).isEqualTo(prefix + " 1");
    assertThat(second.getTitleOrPlaceholder(2)).isEqualTo(prefix + " 2");

    first.setTitle("Mine");
    assertThat(first.getTitleOrPlaceholder(1)).isEqualTo("Mine");
  }

  @Test
  public void testColumnIdentifierIsStableAcrossAddAndRemove() {
    TvShowCustomColumn first = new TvShowCustomColumn("first", "${tvShow.title}");
    TvShowCustomColumn second = new TvShowCustomColumn("second", "${tvShow.title}");
    settings.addCustomColumn(first);
    settings.addCustomColumn(second);

    String identifierOfSecond = second.getColumnIdentifier();
    assertThat(identifierOfSecond).startsWith(TvShowCustomColumn.COLUMN_IDENTIFIER_PREFIX);

    settings.removeCustomColumn(first);

    // the surviving column keeps its identifier, so the user keeps its width and visibility within the table
    assertThat(second.getColumnIdentifier()).isEqualTo(identifierOfSecond);
    assertThat(settings.getCustomColumns()).containsExactly(second);
  }

  @Test
  public void testStructureAndContentChangesFireDifferentEvents() throws Exception {
    final List<String> properties = new ArrayList<>();
    settings.addPropertyChangeListener(event -> properties.add(event.getPropertyName()));

    TvShowCustomColumn customColumn = new TvShowCustomColumn();
    settings.addCustomColumn(customColumn);
    Thread.sleep(200);
    assertThat(properties).contains(TvShowSettings.CUSTOM_COLUMNS);

    properties.clear();
    customColumn.setTitle("mine");
    Thread.sleep(200);
    assertThat(properties).contains(TvShowSettings.CUSTOM_COLUMN_CONTENT);
    assertThat(properties).doesNotContain(TvShowSettings.CUSTOM_COLUMNS);
  }

  /**
   * Events and settings property changes are dispatched asynchronously, so the result has to be polled instead of sleeping a fixed amount of time.
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
