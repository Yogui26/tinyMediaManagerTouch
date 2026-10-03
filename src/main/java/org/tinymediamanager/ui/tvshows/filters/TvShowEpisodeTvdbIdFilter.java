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
 * the class {@link TvShowEpisodeTvdbIdFilter} is used to filter episodes for their TheTVDB id
 *
 * @author Manuel Laggner
 */
public class TvShowEpisodeTvdbIdFilter extends AbstractTextTvShowUIFilter {

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.tvdb") + " (" + TmmResourceBundle.getString("metatag.episode") + ")");
  }

  @Override
  public String getId() {
    return "tvShowEpisodeTvdbId";
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    try {
      final boolean blankFilter = StringUtils.isBlank(normalizedFilterText);

      for (TvShowEpisode episode : episodes) {
        final String tvdbId = episode.getTvdbId();

        boolean foundEpisode;
        if (blankFilter && (StringUtils.isBlank(tvdbId) || tvdbId.equals("0"))) {
          // no filter text: show only episodes without a TheTVDB id
          foundEpisode = true;
        }
        else if (StringUtils.isNoneBlank(normalizedFilterText, tvdbId)) {
          Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(tvdbId));
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
