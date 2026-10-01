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
 * the class {@link TvShowTmdbIdFilter} is used to filter TV shows for their TMDB id
 *
 * @author Manuel Laggner
 */
public class TvShowTmdbIdFilter extends AbstractTextTvShowUIFilter {

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.tmdb") + " (" + TmmResourceBundle.getString("metatag.tvshow") + ")");
  }

  @Override
  public String getId() {
    return "tvShowTmdbId";
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    final String tmdbId = Integer.toString(tvShow.getTmdbId());

    try {
      boolean matches;
      if (StringUtils.isBlank(normalizedFilterText) && tmdbId.equals("0")) {
        // no filter text: show only TV shows without a TMDB id
        matches = true;
      }
      else if (StringUtils.isNoneBlank(normalizedFilterText, tmdbId)) {
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(tmdbId));
        matches = matcher.find();
      }
      else {
        matches = false;
      }

      return invert ^ matches;
    }
    catch (Exception e) {
      // if any exceptions are thrown, just return true
      return true;
    }
  }
}
