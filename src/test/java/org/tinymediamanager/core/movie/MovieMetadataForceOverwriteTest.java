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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.MediaMetadata;

public class MovieMetadataForceOverwriteTest extends BasicMovieTest {

  @Before
  public void setup() throws Exception {
    super.setup();
    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().getSettings().clearNfoFilenames();
  }

  @After
  public void tearDown() throws Exception {
    MovieModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  @Test
  public void shouldNotOverwriteWithEmptyValuesByDefault() {
    Movie movie = createMovie();

    movie.setMetadata(createEmptyMetadata(), textConfig(), true, false);

    assertThat(movie.getPlot()).isEqualTo("existing plot");
    assertThat(movie.getTitle()).isEqualTo("Existing Title");
  }

  @Test
  public void shouldOverwriteWithEmptyValuesWhenForced() {
    Movie movie = createMovie();

    movie.setMetadata(createEmptyMetadata(), config(), true, true);

    assertThat(movie.getPlot()).isEmpty();
    assertThat(movie.getGenres()).isEmpty();
    assertThat(movie.getTitle()).isEmpty();
  }

  @Test
  public void shouldIgnoreForceWhenOverwriteIsDisabled() {
    Movie movie = createMovie();

    movie.setMetadata(createEmptyMetadata(), config(), false, true);

    assertThat(movie.getPlot()).isEqualTo("existing plot");
    assertThat(movie.getGenres()).containsExactly(MediaGenres.ACTION);
    assertThat(movie.getTitle()).isEqualTo("Existing Title");
  }

  private Movie createMovie() {
    Movie movie = new Movie();
    movie.setTitle("Existing Title");
    movie.setPlot("existing plot");
    movie.setGenres(Collections.singletonList(MediaGenres.ACTION));
    movie.setId(MediaMetadata.TMDB, 1);
    return movie;
  }

  private MediaMetadata createEmptyMetadata() {
    MediaMetadata metadata = new MediaMetadata(MediaMetadata.TMDB);
    metadata.setId(MediaMetadata.TMDB, 1);
    metadata.setTitle("");
    metadata.setPlot("");
    metadata.setGenres(Collections.emptyList());
    return metadata;
  }

  private List<MovieScraperMetadataConfig> textConfig() {
    return Arrays.asList(MovieScraperMetadataConfig.TITLE, MovieScraperMetadataConfig.PLOT);
  }

  private List<MovieScraperMetadataConfig> config() {
    return Arrays.asList(MovieScraperMetadataConfig.TITLE, MovieScraperMetadataConfig.PLOT, MovieScraperMetadataConfig.GENRES);
  }
}
