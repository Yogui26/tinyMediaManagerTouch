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

package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.tasks.TvShowRemoveDummyEpisodesTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowRemoveMissingEpisodesAction. To remove the dummy episodes from the selected TV shows
 * 
 * @author Manuel Laggner
 */
public class TvShowRemoveMissingEpisodesAction extends TmmAction {
  public TvShowRemoveMissingEpisodesAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.scrape.removemissingepisodes"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.scrape.removemissingepisodes.desc"));

    putValue(LARGE_ICON_KEY, IconManager.REMOVE);
    putValue(SMALL_ICON, IconManager.REMOVE);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<TvShow> selectedTvShows = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShowsRecursive();

    if (selectedTvShows.isEmpty()) {
      return;
    }

    TmmTaskManager.getInstance().addMainTask(new TvShowRemoveDummyEpisodesTask(selectedTvShows));
  }
}
