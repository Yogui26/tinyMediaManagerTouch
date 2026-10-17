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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;

/**
 * The Class TvShowDummyEpisodeRemovalTest.
 * 
 * @author Manuel Laggner
 */
public class TvShowDummyEpisodeRemovalTest extends BasicTvShowTest {

  @Test
  public void testRemoveAllDummyEpisodes() throws Exception {
    TmmModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().startUp();
    try {
      TvShowSettings.getInstance().setDisplayMissingEpisodes(true);

      TvShow show = new TvShow();
      show.setTitle("Test Show");
      show.setPath(getWorkFolder().resolve("show").toString());
      TvShowModuleManager.getInstance().getTvShowList().addTvShow(show);

      // one real episode
      TvShowEpisode realEpisode = new TvShowEpisode();
      realEpisode.setTitle("Real Episode");
      realEpisode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, 1));
      realEpisode.addToMediaFiles(new MediaFile(getWorkFolder().resolve("show/s01e01.avi")));
      realEpisode.setTvShow(show);
      show.addEpisode(realEpisode);

      // two dummy episodes
      List<TvShowEpisode> dummies = new ArrayList<>();
      TvShowEpisode dummyEpisode = createDummyEpisode(show, 1, 2, "Missing Episode");
      dummies.add(dummyEpisode);
      dummies.add(createDummyEpisode(show, 2, 1, "Another Missing Episode"));
      show.setDummyEpisodes(dummies);

      assertThat(show.getDummyEpisodes()).hasSize(2);
      assertThat(show.getEpisodesForDisplay()).contains(realEpisode, dummyEpisode);

      show.removeAllDummyEpisodes();

      assertThat(show.getDummyEpisodes()).isEmpty();
      assertThat(show.getEpisodes()).containsExactly(realEpisode);
      assertThat(show.getEpisodesForDisplay()).containsExactly(realEpisode);

      for (TvShowSeason season : show.getSeasons()) {
        assertThat(season.getEpisodesForDisplay()).doesNotContainAnyElementsOf(dummies);
      }
    }
    finally {
      TvShowModuleManager.getInstance().shutDown();
      TmmModuleManager.getInstance().shutDown();
    }
  }

  private TvShowEpisode createDummyEpisode(TvShow show, int season, int episode, String title) throws Exception {
    TvShowEpisode dummy = new TvShowEpisode();
    dummy.setTitle(title);
    dummy.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, season, episode));
    dummy.setFirstAired(new Date(0));
    dummy.setDummy(true);
    dummy.setTvShow(show);
    return dummy;
  }
}
