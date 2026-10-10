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

package org.tinymediamanager.core.movie.tasks;

import java.util.List;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.threading.TmmThreadPool;

/**
 * The class MovieSetRemoveDummyMoviesTask. Removes all dummy movies from the given movie sets
 * 
 * @author Manuel Laggner
 */
public class MovieSetRemoveDummyMoviesTask extends TmmThreadPool {
  private final List<MovieSet> movieSets;

  public MovieSetRemoveDummyMoviesTask(List<MovieSet> movieSets) {
    super(TmmResourceBundle.getString("movieset.scrape.removemissingmovies"));
    this.movieSets = movieSets;
  }

  @Override
  protected void doInBackground() {
    initThreadPool(1, "removeDummyMovies");
    for (MovieSet movieSet : movieSets) {
      submitTask(() -> movieSet.removeAllDummyMovies());
    }

    waitForCompletionOrCancel();
  }

  @Override
  public void callback(Object obj) {
    // nothing to do here
  }
}
