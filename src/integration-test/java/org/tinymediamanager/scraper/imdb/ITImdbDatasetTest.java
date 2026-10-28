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

package org.tinymediamanager.scraper.imdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviders;
import org.tinymediamanager.scraper.imdb.datasets.ImdbDatasetEpisodes;
import org.tinymediamanager.scraper.imdb.datasets.ImdbDatasetTitleBasics;
import org.tinymediamanager.scraper.imdb.datasets.ImdbDatasetTitleRatings;

public class ITImdbDatasetTest extends BasicITest {

  @Before
  public void setup() throws Exception {
    super.setup();
    MediaProviders.loadMediaProviders();
  }

  @Test
  public void episodes() {
    ImdbDatasetEpisodes i = new ImdbDatasetEpisodes();
    List<MediaMetadata> eps = i.getEpisodesByShowId("tt0434733"); // Sandmaennchen 9000+ EPs
    System.out.println(eps.size());
    assertThat(eps.size()).isGreaterThan(9000);
  }

  @Test
  public void basics() {
    ImdbDatasetTitleBasics i = new ImdbDatasetTitleBasics();
    String title = i.getTitleById("tt9916840"); // quite a high number
    assertThat(title).isNotBlank();
    System.out.println(title);
  }

  @Test
  public void ratings() {
    ImdbDatasetTitleRatings i = new ImdbDatasetTitleRatings();
    MediaRating rating = i.getImdbRating("tt1948150");
    assertThat(rating).isNotNull();
    System.out.println(rating);
  }
}
