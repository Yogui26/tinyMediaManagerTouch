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

package org.tinymediamanager.core.tvshow.tasks;

import java.util.List;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.entities.TvShow;

/**
 * The class TvShowRemoveDummyEpisodesTask. Removes all dummy episodes from the given TV shows
 * 
 * @author Manuel Laggner
 */
public class TvShowRemoveDummyEpisodesTask extends TmmThreadPool {
  private final List<TvShow> tvShows;

  public TvShowRemoveDummyEpisodesTask(List<TvShow> tvShows) {
    super(TmmResourceBundle.getString("tvshow.scrape.removemissingepisodes"));
    this.tvShows = tvShows;
  }

  @Override
  protected void doInBackground() {
    initThreadPool(1, "removeDummyEpisodes");

    for (TvShow tvShow : tvShows) {
      submitTask(() -> tvShow.removeAllDummyEpisodes());
    }

    waitForCompletionOrCancel();
  }

  @Override
  public void callback(Object obj) {
    // nothing to do here
  }
}
