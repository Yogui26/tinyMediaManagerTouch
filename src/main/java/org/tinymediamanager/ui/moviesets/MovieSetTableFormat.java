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
package org.tinymediamanager.ui.moviesets;

import java.awt.FontMetrics;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.bus.Event;
import org.tinymediamanager.core.bus.EventBus;
import org.tinymediamanager.core.bus.IEventListener;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaSource;
import org.tinymediamanager.core.movie.MovieCustomColumn;
import org.tinymediamanager.core.movie.MovieEdition;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetScraperMetadataConfig;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.movie.jmte.MovieCustomColumnEvaluator;
import org.tinymediamanager.scraper.util.DateUtils;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;
import org.tinymediamanager.ui.components.treetable.TmmTreeTableFormat;
import org.tinymediamanager.ui.renderer.DateTableCellRenderer;
import org.tinymediamanager.ui.renderer.IntegerTableCellRenderer;
import org.tinymediamanager.ui.renderer.RightAlignTableCellRenderer;
import org.tinymediamanager.ui.renderer.RuntimeTableCellRenderer;

/**
 * The class MovieSetTableFormat is used to define the columns for the movie set tree table
 *
 * @author Manuel Laggner
 */
public class MovieSetTableFormat extends TmmTreeTableFormat<TmmTreeNode> {

  private final MovieList                  movieList;

  /** the evaluator for the freely configurable columns */
  private final MovieCustomColumnEvaluator customColumnEvaluator  = new MovieCustomColumnEvaluator();

  /** the model index of the first custom column */
  private int                              customColumnBaseIndex;

  /** the custom columns which are currently mounted into this table, in the very same order as the table columns */
  private final List<MovieCustomColumn>    mountedCustomColumns   = new ArrayList<>();

  /** the comparator used by all custom columns */
  private final Comparator<String>         customColumnComparator = new StringComparator();

  public MovieSetTableFormat() {

    movieList = MovieModuleManager.getInstance().getMovieList();

    // cache the column values per entity, they are recomputed lazily and dropped as soon as the entity changes
    enableValueCaching();

    // drops the cached column values of a movie and its movie set as soon as it changes - the set aggregates the values of its movies
    IEventListener movieListener = event -> {
      if (event.sender() instanceof Movie movie) {
        switch (event.eventType()) {
          case Event.TYPE_SAVE, Event.TYPE_ADD, Event.TYPE_REMOVE:
            invalidateCache(movie.getDbId());
            if (movie.getMovieSet() != null) {
              invalidateCache(movie.getMovieSet().getDbId());
            }
            break;

          default:
            break;
        }
      }
    };
    EventBus.registerListener(EventBus.TOPIC_MOVIES, movieListener);

    // drops the cached column values of a movie set and its movies as soon as it changes
    IEventListener movieSetListener = event -> {
      if (event.sender() instanceof MovieSet movieSet) {
        switch (event.eventType()) {
          case Event.TYPE_SAVE, Event.TYPE_ADD, Event.TYPE_REMOVE:
            invalidateCache(movieSet.getDbId());
            for (Movie movie : movieSet.getMoviesForDisplay()) {
              invalidateCache(movie.getDbId());
            }
            break;

          default:
            break;
        }
      }
    };
    EventBus.registerListener(EventBus.TOPIC_MOVIE_SETS, movieSetListener);

    // drops all cached column values as soon as a setting they depend on changes
    PropertyChangeListener settingsListener = evt -> {
      // always clear the cache, because the value of a column can depend on multiple settings (e.g. the rating column depends on the rating source
      // and the rating scale). And going this way avoids new properties to be forgotten to be added to this listener. So just clear the cache on any
      // change.
      clearCache();
    };
    MovieModuleManager.getInstance().getSettings().addPropertyChangeListener(settingsListener);

    FontMetrics fontMetrics = getFontMetrics();

    Comparator<Integer> integerComparator = new IntegerComparator();
    Comparator<ImageIcon> imageComparator = new ImageComparator();
    Comparator<String> stringComparator = new StringComparator();

    /*
     * sorttitle (hidden per default)
     */
    Column col = new Column(TmmResourceBundle.getString("metatag.sorttitle"), "sortTitle", this::getSortTitle, String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setCellTooltip(this::getSortTitle);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * movie count
     */
    col = new Column(TmmResourceBundle.getString("movieset.moviecount"), "moviecount", this::getMovieCount, Integer.class);
    col.setHeaderIcon(IconManager.COUNT);
    col.setCellRenderer(new IntegerTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("99") + getCellPadding());
    col.setColumnComparator(integerComparator);
    addColumn(col);

    /*
     * missing movie count
     */
    col = new Column(TmmResourceBundle.getString("movieset.missingmoviecount"), "missingmoviecount", this::getMissingMovieCount, Integer.class);
    col.setHeaderIcon(IconManager.MISSING);
    col.setCellRenderer(new IntegerTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("999") + getCellPadding());
    col.setColumnComparator(integerComparator);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * year
     */
    col = new Column(TmmResourceBundle.getString("metatag.year"), "year", this::getYear, String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("2000 - 2000") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * file name (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.filename"), "filename", this::getVideoFilename, String.class);
    col.setColumnResizeable(true);
    col.setCellTooltip(this::getVideoFilename);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * folder name (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.path"), "path", this::getMoviePath, String.class);
    col.setColumnResizeable(true);
    col.setCellTooltip(this::getMoviePath);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.rating"), "rating", this::getRating, String.class);
    col.setHeaderIcon(IconManager.RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("99.9") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * user rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.userrating"), "userrating", this::getUserRating, String.class);
    col.setHeaderIcon(IconManager.RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("99.9") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * votes (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.votes"), "votes", this::getVotes, Integer.class);
    col.setHeaderIcon(IconManager.VOTES);
    col.setCellRenderer(new IntegerTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("1000000") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * date added (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.dateadded"), "dateAdded", this::getDateAdded, Date.class);
    col.setHeaderIcon(IconManager.DATE_ADDED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    try {
      Date date = DateUtils.parseDate("2012-12-12");
      col.setMinWidth(fontMetrics.stringWidth(TmmDateFormat.getDateFormat().format(date)) + getCellPadding());
    }
    catch (Exception ignored) {
      // ignore
    }
    addColumn(col);

    /*
     * runtime (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.runtime") + " [min]", "runtime", this::getRuntime, Integer.class);
    col.setHeaderIcon(IconManager.RUNTIME);
    col.setCellRenderer(new RuntimeTableCellRenderer(RuntimeTableCellRenderer.FORMAT.MINUTES));
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("200") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * runtime HH:MM (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.runtime") + " [hh:mm]", "runtime2", this::getRuntime, Integer.class);
    col.setHeaderIcon(IconManager.RUNTIME);
    col.setCellRenderer(new RuntimeTableCellRenderer(RuntimeTableCellRenderer.FORMAT.HOURS_MINUTES));
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("4:00") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * video format
     */
    col = new Column(TmmResourceBundle.getString("metatag.format"), "format", this::getMediaInfoVideoFormat, String.class);
    col.setHeaderIcon(IconManager.VIDEO_FORMAT);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("1080p") + getCellPadding());
    addColumn(col);

    /*
     * video codec (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videocodec"), "videoCodec", this::getMediaInfoVideoCodec, String.class);
    col.setHeaderIcon(IconManager.VIDEO_CODEC);
    col.setMinWidth(fontMetrics.stringWidth("MPEG-2") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * video bitrate (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videobitrate"), "videoBitrate", this::getMediaInfoVideoBitrate, String.class);
    col.setHeaderIcon(IconManager.VIDEO_BITRATE);
    col.setMinWidth(fontMetrics.stringWidth("20000") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * audio codec and channels(hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.audio"), "audio", this::getAudioInformation, String.class);
    col.setHeaderIcon(IconManager.AUDIO);
    col.setMinWidth(fontMetrics.stringWidth("DTS 7ch") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * main video file size (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videofilesize"), "fileSize", this::getFileSize, String.class);
    col.setHeaderIcon(IconManager.FILE_SIZE);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("500.00 MB") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * total file size (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.totalfilesize"), "totalFileSize", this::getTotalFileSize, String.class);
    col.setHeaderIcon(IconManager.FILE_SIZE);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth(fontMetrics.stringWidth("500.00 MB") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * Edition (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.edition"), "edition", this::getEdition, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.EDITION);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * Source (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.source"), "mediaSource", this::getMediaSource, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.SOURCE);
    col.setMinWidth(fontMetrics.stringWidth("Blu-ray") + getCellPadding());
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * the freely configurable columns - their content is evaluated lazily by the MovieCustomColumnEvaluator and they are placed right before the icon
     * columns
     */
    customColumnBaseIndex = columns.size();
    addCustomColumns();

    /*
     * metadata
     */
    col = new Column(TmmResourceBundle.getString("tmm.metadata"), "metadata", this::hasMetadata, ImageIcon.class);
    col.setHeaderIcon(IconManager.NFO);
    col.setColumnResizeable(false);
    col.setColumnComparator(imageComparator);
    col.setCellTooltip(this::hasMetadataTooltip);
    addColumn(col);

    /*
     * images
     */
    col = new Column(TmmResourceBundle.getString("tmm.images"), "images", this::hasImages, ImageIcon.class);
    col.setHeaderIcon(IconManager.IMAGES);
    col.setColumnResizeable(false);
    col.setColumnComparator(imageComparator);
    col.setCellTooltip(this::hasImagesTooltip);
    addColumn(col);

    /*
     * watched
     */
    col = new Column(TmmResourceBundle.getString("metatag.watched"), "watched", this::isWatched, ImageIcon.class);
    col.setHeaderIcon(IconManager.WATCHED);
    col.setColumnResizeable(false);
    col.setColumnComparator(imageComparator);
    addColumn(col);
  }

  /**
   * Mounts all currently configured custom columns right before the icon columns. They are hidden per default, so an unconfigured or unused column
   * costs nothing at all.
   */
  private void addCustomColumns() {
    // copy the settings, so that an already mounted column can never be swapped against another one by a tinkered around settings list
    mountedCustomColumns.clear();
    mountedCustomColumns.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetCustomColumns());

    for (int i = 0; i < mountedCustomColumns.size(); i++) {
      final int position = i + 1;
      final MovieCustomColumn customColumn = mountedCustomColumns.get(i);

      Column col = new Column(customColumn.getTitleOrPlaceholder(position), customColumn.getColumnIdentifier(),
          node -> getCustomColumnValue(node, customColumn), String.class);
      col.setColumnComparator(customColumnComparator);
      col.setCellTooltip(showTooltip(node -> StringUtils.defaultString(getCustomColumnPattern(node, customColumn))));
      col.setDefaultHidden(true);
      // the evaluator caches these values already, so do not cache them a second time here
      col.setCacheEnabled(false);
      insertColumn(customColumnBaseIndex + i, col);
    }
  }

  /**
   * Remounts the freely configurable columns according to the current settings. Needed when columns have been added, removed or reordered.
   */
  public void rebuildCustomColumns() {
    for (int i = 0; i < mountedCustomColumns.size(); i++) {
      columns.remove(customColumnBaseIndex);
    }

    addCustomColumns();
  }

  /**
   * Evaluates a custom column for one row of the hierarchical movie set table: the movie set rows use the pattern of the set level, the movie rows
   * the pattern of the movie level.
   *
   * @param node
   *          the node of the row to evaluate
   * @param customColumn
   *          the custom column to evaluate
   * @return the value of this cell
   */
  private String getCustomColumnValue(TmmTreeNode node, MovieCustomColumn customColumn) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet movieSet) {
      return customColumnEvaluator.evaluate(movieSet, customColumn);
    }
    if (userObject instanceof Movie movie) {
      return customColumnEvaluator.evaluate(movie, getParentMovieSet(node), customColumn);
    }
    return "";
  }

  /**
   * Gets the JMTE pattern belonging to the level of the given row, so that the tooltip tells the user where the content of this cell is coming from.
   *
   * @param node
   *          the node of the row to evaluate
   * @param customColumn
   *          the custom column to evaluate
   * @return the pattern of the row level
   */
  private String getCustomColumnPattern(TmmTreeNode node, MovieCustomColumn customColumn) {
    if (node.getUserObject() instanceof MovieSet) {
      return customColumn.getSetPattern();
    }
    if (node.getUserObject() instanceof Movie) {
      return customColumn.getPattern();
    }
    return null;
  }

  private static MovieSet getParentMovieSet(TmmTreeNode node) {
    return node.getParent() instanceof TmmTreeNode parent && parent.getUserObject() instanceof MovieSet movieSet ? movieSet : null;
  }

  @Override
  public String getColumnName(int i) {
    if (i == NODE_COLUMN) {
      return TmmResourceBundle.getString("metatag.title");
    }

    MovieCustomColumn customColumn = getCustomColumn(i);
    if (customColumn != null) {
      // the title is read live from the mounted bean, so no table rebuild is needed when the user only renames a column
      return customColumn.getTitleOrPlaceholder(i - customColumnBaseIndex + 1);
    }

    return super.getColumnName(i);
  }

  @Override
  public boolean isColumnEnabled(int i) {
    MovieCustomColumn customColumn = getCustomColumn(i);
    if (customColumn != null) {
      return customColumn.isConfiguredForSets();
    }
    return true;
  }

  /**
   * Gets the custom column which is mounted at the given model index. The mounted columns are the ones the user currently sees, they can well be out
   * of sync with the settings until the table has been rebuilt.
   *
   * @param i
   *          the model index of the column
   * @return the {@link MovieCustomColumn} at this index, or null when the column is no custom column
   */
  private MovieCustomColumn getCustomColumn(int i) {
    int position = i - customColumnBaseIndex;
    if (position < 0 || position >= mountedCustomColumns.size()) {
      return null;
    }
    return mountedCustomColumns.get(position);
  }

  @Override
  protected Object getCacheKey(TmmTreeNode node) {
    // the value of a row is cached by the entity behind the node, not by the node itself (which gets recreated on structure changes)
    return node.getUserObject() instanceof MediaEntity entity ? entity.getDbId() : null;
  }

  private static <E> Function<E, String> showTooltip(Function<E, String> tooltipFunction) {
    return node -> {
      if (MovieModuleManager.getInstance().getSettings().isShowMovieSetTableTooltips()) {
        return tooltipFunction.apply(node);
      }
      else {
        return null;
      }
    };
  }

  private String getSortTitle(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet movieSet) {
      return movieSet.getSortTitle();
    }
    else if (userObject instanceof Movie movie) {
      return movie.getSortTitle();
    }
    return null;
  }

  private Integer getMovieCount(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet movieSet) {
      int size = movieSet.getMovies().size();
      if (size > 0) {
        return size;
      }
    }
    return null;
  }

  private Integer getMissingMovieCount(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet movieSet) {
      int size = 0;
      for (Movie movie : movieSet.getMoviesForDisplay()) {
        if (movie instanceof MovieSet.MovieSetMovie)
          size++;
      }
      if (size > 0) {
        return size;
      }
    }
    return null;
  }

  private String getRating(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof Movie) {
      MediaRating mediaRating = ((MediaEntity) userObject).getRating();
      if (mediaRating != null && mediaRating.getRating() > 0) {
        return String.valueOf(mediaRating.getRating());
      }
    }
    return null;
  }

  private String getFileSize(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie movie) {
      return Utils.formatFileSizeForDisplay(movie.getVideoFilesize());
    }
    return null;
  }

  private String getTotalFileSize(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie movie) {
      return Utils.formatFileSizeForDisplay(movie.getTotalFilesize());
    }
    return null;
  }

  private String getEdition(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie movie) {
      MovieEdition edition = movie.getEdition();
      if (edition != null) {
        return edition.toString();
      }
    }

    return null;
  }

  private String getMediaSource(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie movie) {
      MediaSource mediaSource = movie.getMediaSource();
      if (mediaSource != null) {
        return mediaSource.toString();
      }
    }

    return null;
  }

  private ImageIcon hasMetadata(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet movieSet) {
      return getCheckIcon(movieList.detectMissingMetadata(movieSet).isEmpty());
    }
    else if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie movie) {
      return getCheckIcon(movieList.detectMissingMetadata(movie).isEmpty());
    }
    return null;
  }

  private String hasMetadataTooltip(TmmTreeNode node) {
    if (!MovieModuleManager.getInstance().getSettings().isShowMovieSetTableTooltips()) {
      return null;
    }

    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet movieSet) {
      List<MovieSetScraperMetadataConfig> values = new ArrayList<>();
      if (MovieModuleManager.getInstance().getSettings().isMovieSetDisplayAllMissingMetadata()) {
        for (MovieSetScraperMetadataConfig config : MovieSetScraperMetadataConfig.values()) {
          if (config.isMetaData() || config.isCast()) {
            values.add(config);
          }
        }
      }
      else {
        values.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetCheckMetadata());
      }
      return getMissingToolTip(movieList.detectMissingFields(movieSet, values));
    }
    else if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie movie) {
      List<MovieScraperMetadataConfig> values = new ArrayList<>();
      if (MovieModuleManager.getInstance().getSettings().isMovieDisplayAllMissingMetadata()) {
        for (MovieScraperMetadataConfig config : MovieScraperMetadataConfig.values()) {
          if (config.isMetaData() || config.isCast()) {
            values.add(config);
          }
        }
      }
      else {
        values.addAll(MovieModuleManager.getInstance().getSettings().getMovieCheckMetadata());
      }
      return getMissingToolTip(movieList.detectMissingFields(movie, values));
    }
    return null;
  }

  private ImageIcon hasImages(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet movieSet) {
      return getCheckIcon(movieList.detectMissingArtwork(movieSet).isEmpty());
    }
    else if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie movie) {
      return getCheckIcon(movieList.detectMissingArtwork(movie).isEmpty());
    }
    return null;
  }

  private String hasImagesTooltip(TmmTreeNode node) {
    if (!MovieModuleManager.getInstance().getSettings().isShowMovieSetTableTooltips()) {
      return null;
    }

    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet movieSet) {
      List<MovieSetScraperMetadataConfig> values = new ArrayList<>();
      if (MovieModuleManager.getInstance().getSettings().isMovieSetDisplayAllMissingArtwork()) {
        for (MovieSetScraperMetadataConfig config : MovieSetScraperMetadataConfig.values()) {
          if (config.isArtwork()) {
            values.add(config);
          }
        }
      }
      else {
        values.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetCheckArtwork());
      }
      return getMissingToolTip(movieList.detectMissingFields(movieSet, values));
    }
    else if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie movie) {
      List<MovieScraperMetadataConfig> values = new ArrayList<>();
      if (MovieModuleManager.getInstance().getSettings().isMovieDisplayAllMissingArtwork()) {
        for (MovieScraperMetadataConfig config : MovieScraperMetadataConfig.values()) {
          if (config.isArtwork()) {
            values.add(config);
          }
        }
      }
      else {
        values.addAll(MovieModuleManager.getInstance().getSettings().getMovieCheckArtwork());
      }
      return getMissingToolTip(movieList.detectMissingFields(movie, values));
    }
    return null;
  }

  private ImageIcon isWatched(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet movieSet) {
      return getCheckIcon(movieSet.isWatched());
    }
    else if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie movie) {
      return getCheckIcon(movie.isWatched());
    }
    return null;
  }

  private Integer getRuntime(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof Movie movie) {
      int runtime = movie.getRuntime();

      if (runtime > 0) {
        return runtime;
      }
    }

    return null;
  }

  private String getYear(TmmTreeNode node) {
    Object userobject = node.getUserObject();

    if (userobject instanceof MovieSet movieSet) {
      String years = movieSet.getYears();
      if (StringUtils.isNotBlank(years)) {
        return years;
      }
    }

    if (userobject instanceof Movie movie) {
      int year = movie.getYear();

      if (year > 0) {
        return String.valueOf(year);
      }
    }

    return null;
  }

  private String getMediaInfoVideoFormat(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie movie) {
      return movie.getMediaInfoVideoFormat();
    }

    return null;
  }

  private String getMediaInfoVideoCodec(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie movie) {
      return movie.getMediaInfoVideoCodec();
    }

    return null;
  }

  private String getMediaInfoVideoBitrate(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie movie) {
      return String.valueOf(movie.getMediaInfoVideoBitrate());
    }

    return null;
  }

  private String getUserRating(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie movie) {
      MediaRating mediaRating = movie.getUserRating();
      if (mediaRating != null && mediaRating.getRating() > 0) {
        return String.valueOf(mediaRating.getRating());
      }
    }

    return null;
  }

  private Integer getVotes(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie movie) {
      return movie.getRating().getVotes();
    }

    return null;
  }

  private Date getDateAdded(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof MovieSet movieSet) {
      // get the latest date added from the movies in the set
      List<Movie> modifyableList = new ArrayList<>(movieSet.getMovies());
      if (!modifyableList.isEmpty()) {
        Movie latest = Collections.max(modifyableList, Comparator.comparing(Movie::getDateAddedForUi));
        return latest.getDateAddedForUi();
      }
      return null;
    }
    else if (userObject instanceof Movie movie) {
      return movie.getDateAddedForUi();
    }

    return null;
  }

  private String getAudioInformation(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie movie) {
      List<MediaFile> videos = movie.getMediaFiles(MediaFileType.VIDEO);
      if (!videos.isEmpty()) {
        MediaFile mediaFile = videos.get(0);
        if (StringUtils.isNotBlank(mediaFile.getAudioCodec())) {
          return mediaFile.getAudioCodec() + " " + mediaFile.getAudioChannels();
        }
      }
    }

    return null;
  }

  private String getMoviePath(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie movie) {
      return movie.getPathNIO().toString();
    }

    return null;
  }

  private String getVideoFilename(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie movie) {
      return movie.getMainVideoFile().getFilename();
    }

    return null;
  }

  private String getMissingToolTip(List<? extends ScraperMetadataConfig> missingValues) {
    if (!missingValues.isEmpty()) {
      StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");

      for (ScraperMetadataConfig metadataConfig : missingValues) {
        missing.append("\n").append(metadataConfig.getDescription());
      }

      return missing.toString();
    }

    return null;
  }
}
