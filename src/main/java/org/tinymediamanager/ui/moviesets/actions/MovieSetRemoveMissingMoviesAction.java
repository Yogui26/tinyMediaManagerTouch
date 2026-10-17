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

package org.tinymediamanager.ui.moviesets.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.movie.tasks.MovieSetRemoveDummyMoviesTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;

/**
 * The class {@link MovieSetRemoveMissingMoviesAction}. To remove the dummy movies from the selected movie sets
 * 
 * @author Manuel Laggner
 */
public class MovieSetRemoveMissingMoviesAction extends TmmAction {
  public MovieSetRemoveMissingMoviesAction() {
    putValue(NAME, TmmResourceBundle.getString("movieset.scrape.removemissingmovies"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movieset.scrape.removemissingmovies.desc"));

    putValue(LARGE_ICON_KEY, IconManager.REMOVE);
    putValue(SMALL_ICON, IconManager.REMOVE);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<MovieSet> selectedMovieSets = new ArrayList<>(MovieSetUIModule.getInstance().getSelectionModel().getSelectedMovieSets());

    if (selectedMovieSets.isEmpty()) {
      return;
    }

    TmmTaskManager.getInstance().addMainTask(new MovieSetRemoveDummyMoviesTask(selectedMovieSets));
  }
}
