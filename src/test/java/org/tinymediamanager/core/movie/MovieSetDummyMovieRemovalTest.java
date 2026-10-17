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
package org.tinymediamanager.core.movie;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.entities.MovieSet;

/**
 * The Class MovieSetDummyMovieRemovalTest.
 * 
 * @author Manuel Laggner
 */
public class MovieSetDummyMovieRemovalTest extends BasicMovieTest {

  @Test
  public void testRemoveAllDummyMovies() throws Exception {
    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();
    try {
      MovieSettings.getInstance().setDisplayMovieSetMissingMovies(true);

      MovieSet movieSet = new MovieSet();
      movieSet.setTitle("Test Set");
      movieSet.setPath(getWorkFolder().resolve("set").toString());

      MovieSet.MovieSetMovie dummyOne = createDummyMovie("Missing Movie");
      MovieSet.MovieSetMovie dummyTwo = createDummyMovie("Another Missing Movie");
      movieSet.setDummyMovies(List.of(dummyOne, dummyTwo));

      assertThat(movieSet.getDummyMovies()).containsExactly(dummyOne, dummyTwo);
      assertThat(movieSet.getMoviesForDisplay()).contains(dummyOne, dummyTwo);

      movieSet.removeAllDummyMovies();

      assertThat(movieSet.getDummyMovies()).isEmpty();
      assertThat(movieSet.getMoviesForDisplay()).isEmpty();
    }
    finally {
      MovieModuleManager.getInstance().shutDown();
      TmmModuleManager.getInstance().shutDown();
    }
  }

  private MovieSet.MovieSetMovie createDummyMovie(String title) throws Exception {
    MovieSet.MovieSetMovie dummy = new MovieSet.MovieSetMovie();
    dummy.setTitle(title);
    return dummy;
  }
}
