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
package org.tinymediamanager.core.tvshow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.MediaMetadata;

public class TvShowMetadataForceOverwriteTest extends BasicTvShowTest {

  @Before
  public void setup() throws Exception {
    super.setup();
    TmmModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().getSettings().clearNfoFilenames();
    TvShowModuleManager.getInstance().getSettings().clearEpisodeNfoFilenames();
  }

  @After
  public void tearDown() throws Exception {
    TvShowModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  @Test
  public void shouldNotOverwriteWithEmptyValuesByDefault() {
    TvShow tvShow = createTvShow();

    tvShow.setMetadata(createEmptyMetadata(), textConfig(), true, false);

    assertThat(tvShow.getPlot()).isEqualTo("existing plot");
    assertThat(tvShow.getTitle()).isEqualTo("Existing Title");
  }

  @Test
  public void shouldOverwriteWithEmptyValuesWhenForced() {
    TvShow tvShow = createTvShow();

    tvShow.setMetadata(createEmptyMetadata(), config(), true, true);

    assertThat(tvShow.getPlot()).isEmpty();
    assertThat(tvShow.getGenres()).isEmpty();
    assertThat(tvShow.getTitle()).isEmpty();
  }

  @Test
  public void shouldIgnoreForceWhenOverwriteIsDisabled() {
    TvShow tvShow = createTvShow();

    tvShow.setMetadata(createEmptyMetadata(), config(), false, true);

    assertThat(tvShow.getPlot()).isEqualTo("existing plot");
    assertThat(tvShow.getGenres()).containsExactly(MediaGenres.ACTION);
    assertThat(tvShow.getTitle()).isEqualTo("Existing Title");
  }

  @Test
  public void shouldHandleEpisodeMetadata() {
    TvShowEpisode episode = createEpisode();

    episode.setMetadata(createEmptyMetadata(), Arrays.asList(TvShowEpisodeScraperMetadataConfig.TITLE, TvShowEpisodeScraperMetadataConfig.PLOT), true,
        false);
    assertThat(episode.getPlot()).isEqualTo("existing plot");
    assertThat(episode.getTitle()).isEqualTo("Existing Title");

    episode.setMetadata(createEmptyMetadata(), Arrays.asList(TvShowEpisodeScraperMetadataConfig.TITLE, TvShowEpisodeScraperMetadataConfig.PLOT), true,
        true);
    assertThat(episode.getPlot()).isEmpty();
    assertThat(episode.getTitle()).isEmpty();
  }

  private TvShowEpisode createEpisode() {
    TvShow tvShow = createTvShow();
    tvShow.setTitle("Show Title");

    TvShowEpisode episode = new TvShowEpisode() {
      @Override
      public void saveToDb() {
        // nothing to persist in this test
      }

      @Override
      public void writeNFO() {
        // no NFO needed in this test
      }
    };
    episode.setTitle("Existing Title");
    episode.setPlot("existing plot");
    episode.setId(MediaMetadata.TMDB, 1);
    episode.setTvShow(tvShow);
    tvShow.addEpisode(episode);
    return episode;
  }

  private TvShow createTvShow() {
    TvShow tvShow = new TvShow() {
      @Override
      public void saveToDb() {
        // nothing to persist in this test
      }

      @Override
      public void writeNFO() {
        // no NFO needed in this test
      }
    };
    tvShow.setPath(getWorkFolder().toString());
    tvShow.setTitle("Existing Title");
    tvShow.setPlot("existing plot");
    tvShow.setGenres(Collections.singletonList(MediaGenres.ACTION));
    tvShow.setId(MediaMetadata.TMDB, 1);
    return tvShow;
  }

  private MediaMetadata createEmptyMetadata() {
    MediaMetadata metadata = new MediaMetadata(MediaMetadata.TMDB);
    metadata.setId(MediaMetadata.TMDB, 1);
    metadata.setTitle("");
    metadata.setPlot("");
    metadata.setGenres(Collections.emptyList());
    return metadata;
  }

  private List<TvShowScraperMetadataConfig> textConfig() {
    return Arrays.asList(TvShowScraperMetadataConfig.TITLE, TvShowScraperMetadataConfig.PLOT);
  }

  private List<TvShowScraperMetadataConfig> config() {
    return Arrays.asList(TvShowScraperMetadataConfig.TITLE, TvShowScraperMetadataConfig.PLOT, TvShowScraperMetadataConfig.GENRES);
  }
}
