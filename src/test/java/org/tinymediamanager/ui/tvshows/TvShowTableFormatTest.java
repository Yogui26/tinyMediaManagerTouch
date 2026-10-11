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

package org.tinymediamanager.ui.tvshows;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.BasicTvShowTest;
import org.tinymediamanager.core.tvshow.TvShowCustomColumn;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;

public class TvShowTableFormatTest extends BasicTvShowTest {

  private TvShowSettings settings;

  private TvShow         aladdin;
  private TvShowSeason   season;
  private TvShowEpisode  episode;

  @Before
  public void setupSettings() {
    settings = TvShowModuleManager.getInstance().getSettings();
    settings.setCustomColumns(new ArrayList<>());

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
  public void resetSettings() {
    settings.setCustomColumns(new ArrayList<>());
  }

  @Test
  public void testCustomColumnsAreMountedAsBeans() {
    TvShowCustomColumn first = new TvShowCustomColumn("first", "${tvShow.title}");
    TvShowCustomColumn second = new TvShowCustomColumn("second", "${episode.title}");
    settings.setCustomColumns(new ArrayList<>(Arrays.asList(first, second)));

    TvShowTableFormat tableFormat = new TvShowTableFormat();
    int indexFirst = tableFormat.getColumnIndex(first.getColumnIdentifier());
    int indexSecond = tableFormat.getColumnIndex(second.getColumnIdentifier());

    assertThat(indexFirst).isNotEqualTo(-1);
    assertThat(indexSecond).isNotEqualTo(-1);
    assertThat(tableFormat.getColumnName(indexFirst)).isEqualTo("first");

    // a new title is no structural change, it shows up without any rebuild
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
  public void testCustomColumnsFollowTheirLevel() {
    TmmTreeNode showNode = new TmmTreeNode(aladdin, null);
    TmmTreeNode seasonNode = new TmmTreeNode(season, null);
    TmmTreeNode episodeNode = new TmmTreeNode(episode, null);

    TvShowCustomColumn allLevels = new TvShowCustomColumn("all", "${tvShow.title}");
    allLevels.setSeasonPattern("season ${season.season}");
    allLevels.setEpisodePattern("${episode.title}");
    TvShowCustomColumn showOnly = new TvShowCustomColumn("show only", "${tvShow.title}");
    settings.setCustomColumns(new ArrayList<>(Arrays.asList(allLevels, showOnly)));

    TvShowTableFormat tableFormat = new TvShowTableFormat();
    int indexAll = tableFormat.getColumnIndex(allLevels.getColumnIdentifier());
    int indexShowOnly = tableFormat.getColumnIndex(showOnly.getColumnIdentifier());

    assertThat(tableFormat.getColumnValue(showNode, indexAll)).isEqualTo("Aladdin");
    assertThat(tableFormat.getColumnValue(seasonNode, indexAll)).isEqualTo("season 1");
    assertThat(tableFormat.getColumnValue(episodeNode, indexAll)).isEqualTo("Don't Pet the Teacher");

    // a column which only knows the show level is empty on the other levels - but still worth showing
    assertThat(tableFormat.getColumnValue(showNode, indexShowOnly)).isEqualTo("Aladdin");
    assertThat(tableFormat.getColumnValue(seasonNode, indexShowOnly)).isEqualTo("");
    assertThat(tableFormat.getColumnValue(episodeNode, indexShowOnly)).isEqualTo("");
  }

  @Test
  public void testUnconfiguredCustomColumnIsNotOffered() {
    TvShowCustomColumn configuredForEpisodes = new TvShowCustomColumn("episodes", "");
    configuredForEpisodes.setEpisodePattern("${episode.title}");
    TvShowCustomColumn untouched = new TvShowCustomColumn();

    settings.setCustomColumns(new ArrayList<>(Arrays.asList(configuredForEpisodes, untouched)));

    TvShowTableFormat tableFormat = new TvShowTableFormat();
    assertThat(tableFormat.isColumnEnabled(tableFormat.getColumnIndex(configuredForEpisodes.getColumnIdentifier()))).isTrue();
    assertThat(tableFormat.isColumnEnabled(tableFormat.getColumnIndex(untouched.getColumnIdentifier()))).isFalse();
  }

  @Test
  public void testCustomColumnsArePlacedBeforeTheIconColumns() {
    TvShowCustomColumn customColumn = new TvShowCustomColumn("custom", "${tvShow.title}");
    settings.setCustomColumns(new ArrayList<>(Arrays.asList(customColumn)));

    TvShowTableFormat tableFormat = new TvShowTableFormat();
    int customColumnIndex = tableFormat.getColumnIndex(customColumn.getColumnIdentifier());
    int metadataColumn = tableFormat.getColumnIndex("metadata");

    assertThat(customColumnIndex).isNotEqualTo(-1);

    // the custom columns join the metadata columns, the icons stay at the very end
    assertThat(customColumnIndex).isLessThan(metadataColumn);
    assertThat(tableFormat.getColumnIndex("watched")).isGreaterThan(metadataColumn);

    settings.setCustomColumns(new ArrayList<>());
    assertThat(new TvShowTableFormat().getColumnIndex("metadata")).isEqualTo(metadataColumn - 1);

    // the tree column of the table itself is never touched, it keeps its index independent title
    assertThat(tableFormat.getColumnName(-1)).isEqualTo(TmmResourceBundle.getString("metatag.title"));
  }
}
