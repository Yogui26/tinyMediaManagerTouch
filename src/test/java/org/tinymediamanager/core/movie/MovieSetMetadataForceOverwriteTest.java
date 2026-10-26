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
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.scraper.MediaMetadata;

public class MovieSetMetadataForceOverwriteTest extends BasicMovieTest {

  @Before
  public void setup() throws Exception {
    super.setup();
    MovieModuleManager.getInstance().getSettings().clearMovieSetNfoFilenames();
  }

  @Test
  public void shouldNotOverwriteWithEmptyValuesByDefault() {
    MovieSet movieSet = createMovieSet();

    movieSet.setMetadata(createEmptyMetadata(), config(), false, false);

    assertThat(movieSet.getTitle()).isEqualTo("Existing Title");
    assertThat(movieSet.getEnglishTitle()).isEqualTo("Existing English Title");
    assertThat(movieSet.getPlot()).isEqualTo("existing plot");
  }

  @Test
  public void shouldOverwriteWithEmptyValuesWhenForced() {
    MovieSet movieSet = createMovieSet();

    movieSet.setMetadata(createEmptyMetadata(), config(), true, true);

    assertThat(movieSet.getTitle()).isEmpty();
    assertThat(movieSet.getEnglishTitle()).isEmpty();
    assertThat(movieSet.getPlot()).isEmpty();
  }

  @Test
  public void shouldIgnoreForceWhenOverwriteIsDisabled() {
    MovieSet movieSet = createMovieSet();

    movieSet.setMetadata(createEmptyMetadata(), config(), false, true);

    assertThat(movieSet.getTitle()).isEqualTo("Existing Title");
    assertThat(movieSet.getEnglishTitle()).isEqualTo("Existing English Title");
    assertThat(movieSet.getPlot()).isEqualTo("existing plot");
  }

  private MovieSet createMovieSet() {
    MovieSet movieSet = new MovieSet() {
      @Override
      public void saveToDb() {
        // nothing to persist in this test
      }

      @Override
      public void writeNFO() {
        // no NFO needed in this test
      }
    };
    movieSet.setTitle("Existing Title");
    movieSet.setEnglishTitle("Existing English Title");
    movieSet.setPlot("existing plot");
    movieSet.setId(MediaMetadata.TMDB, 1);
    return movieSet;
  }

  private MediaMetadata createEmptyMetadata() {
    MediaMetadata metadata = new MediaMetadata(MediaMetadata.TMDB);
    metadata.setId(MediaMetadata.TMDB, 1);
    metadata.setTitle("");
    metadata.setEnglishTitle("");
    metadata.setPlot("");
    return metadata;
  }

  private List<MovieSetScraperMetadataConfig> config() {
    return Arrays.asList(MovieSetScraperMetadataConfig.TITLE, MovieSetScraperMetadataConfig.ENGLISH_TITLE, MovieSetScraperMetadataConfig.PLOT);
  }
}
