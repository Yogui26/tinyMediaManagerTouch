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
package org.tinymediamanager.ui.tvshows.filters;

import java.util.List;
import java.util.regex.Matcher;

import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.components.label.TmmLabel;

/**
 * the class {@link TvShowEpisodeTmdbIdFilter} is used to filter episodes for their TMDB id
 *
 * @author Manuel Laggner
 */
public class TvShowEpisodeTmdbIdFilter extends AbstractTextTvShowUIFilter {

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.tmdb") + " (" + TmmResourceBundle.getString("metatag.episode") + ")");
  }

  @Override
  public String getId() {
    return "tvShowEpisodeTmdbId";
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    try {
      final boolean blankFilter = StringUtils.isBlank(normalizedFilterText);

      for (TvShowEpisode episode : episodes) {
        final String tmdbId = episode.getTmdbId();

        boolean foundEpisode;
        if (blankFilter && (StringUtils.isBlank(tmdbId) || tmdbId.equals("0"))) {
          // no filter text: show only episodes without a TMDB id
          foundEpisode = true;
        }
        else if (StringUtils.isNoneBlank(normalizedFilterText, tmdbId)) {
          Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(tmdbId));
          foundEpisode = matcher.find();
        }
        else {
          foundEpisode = false;
        }

        if (invert && !foundEpisode) {
          return true;
        }
        else if (!invert && foundEpisode) {
          return true;
        }
      }
    }
    catch (Exception e) {
      // if any exceptions are thrown, just return true
      return true;
    }

    return false;
  }
}
