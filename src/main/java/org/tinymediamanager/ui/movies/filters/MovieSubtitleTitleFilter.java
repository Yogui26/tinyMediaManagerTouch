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
package org.tinymediamanager.ui.movies.filters;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.bus.EventBus;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.SetUtils;
import org.tinymediamanager.ui.components.label.TmmLabel;

/**
 * the class {@link MovieSubtitleTitleFilter} is a filter for subtitle titles for movies
 *
 * @author Manuel Laggner
 */
public class MovieSubtitleTitleFilter extends AbstractCheckComboBoxMovieUIFilter<String> {

  public MovieSubtitleTitleFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.contains(s2.toLowerCase(Locale.ROOT)));

    buildAndInstallSubtitleTitleArray();
    EventBus.registerListener(EventBus.TOPIC_MOVIES_UI, event -> buildAndInstallSubtitleTitleArray());
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.subtitletitle"));
  }

  @Override
  public String getId() {
    return "movieSubtitleTitle";
  }

  @Override
  public boolean accept(Movie movie) {

    List<String> selectedItems = checkComboBox.getSelectedItems();
    List<MediaFile> mediaFileList = movie.getMediaFiles(MediaFileType.VIDEO, MediaFileType.SUBTITLE);

    for (MediaFile mf : mediaFileList) {
      // check for explicit empty search
      if (selectedItems.isEmpty() && mf.getSubtitleTitleList().isEmpty()) {
        return true;
      }
      for (String title : mf.getSubtitleTitleList()) {
        if (selectedItems.contains(title)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }

  private void buildAndInstallSubtitleTitleArray() {
    // do it lazy because otherwise there is too much UI overhead
    // also use a set for faster lookups
    Set<String> subtitleTitlesInMovies = new HashSet<>(movieList.getSubtitleTitlesInMovies());

    if (!SetUtils.equals(oldValues, subtitleTitlesInMovies)) {
      oldValues.clear();
      oldValues.addAll(subtitleTitlesInMovies);

      List<String> sortedSubtitleTitles = ListUtils.asSortedList(subtitleTitlesInMovies);

      // update the combobox in the EDT
      SwingUtilities.invokeLater(() -> setValues(sortedSubtitleTitles));
    }
  }
}
