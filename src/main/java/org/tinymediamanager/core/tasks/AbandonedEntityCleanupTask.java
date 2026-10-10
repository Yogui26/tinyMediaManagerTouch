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
package org.tinymediamanager.core.tasks;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;

/**
 * The class {@link AbandonedEntityCleanupTask} scans the database for abandoned entities and removes them. An entity is considered abandoned when its
 * data source is no longer configured in the settings - those leftovers can never be found again by a data source scan and would just keep bloating
 * the database.
 * <p>
 * The check is a pure path comparison and never touches the filesystem, so entities of a temporarily unavailable (but still configured) data source
 * are always left untouched. Only the database entries are removed, no files, NFOs or artwork. Locked entities are removed as well, since a manually
 * locked entity is still useless when its data source is gone.
 * </p>
 * <p>
 * Movie sets, seasons and episodes are never handled on their own: they are only cascaded by the removal of their parent entity, and containers which
 * end up empty are kept.
 * </p>
 *
 * @author Manuel Laggner
 */
public class AbandonedEntityCleanupTask extends TmmTask {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbandonedEntityCleanupTask.class);

  public AbandonedEntityCleanupTask() {
    super(TmmResourceBundle.getString("tmm.cleanupabandoned"), 0, TaskType.BACKGROUND_TASK);
  }

  @Override
  protected void doInBackground() {
    int removedMovies = 0;
    int removedTvShows = 0;

    if (MovieModuleManager.getInstance().isEnabled()) {
      removedMovies = cleanupAbandonedMovies();
    }

    if (!cancel && TvShowModuleManager.getInstance().isEnabled()) {
      removedTvShows = cleanupAbandonedTvShows();
    }

    LOGGER.info("Abandoned entity cleanup done - removed {} movies and {} TV shows from the database", removedMovies, removedTvShows);
    if (removedMovies > 0 || removedTvShows > 0) {
      MessageManager.getInstance()
          .pushMessage(new Message(MessageLevel.INFO, "cleanup.abandoned", "tmm.cleanupabandoned.result",
              new String[] { String.valueOf(removedMovies), String.valueOf(removedTvShows) }));
    }
  }

  /**
   * Removes all movies whose data source is no longer configured.
   *
   * @return the number of removed movies
   */
  private int cleanupAbandonedMovies() {
    MovieModuleManager module = MovieModuleManager.getInstance();
    List<Path> dataSources = normalizeDataSources(module.getSettings().getMovieDataSource());
    if (dataSources.isEmpty()) {
      // would flag the complete database as abandoned - definitely a broken setup, so do not touch anything here
      warnAboutMissingDataSources(module.getModuleTitle());
      return 0;
    }

    List<Movie> movies = new ArrayList<>(module.getMovieList().getMovies());
    setWorkUnits(getWorkUnits() + movies.size());
    LOGGER.debug("Checking {} movies for abandoned database entries", movies.size());

    List<Movie> abandonedMovies = new ArrayList<>();
    for (Movie movie : movies) {
      if (cancel) {
        break;
      }

      if (!isCoveredByDataSource(movie.getDataSource(), movie.getPath(), dataSources)) {
        LOGGER.debug("Removing abandoned movie '{}' ('{}') - data source '{}' is no longer configured", movie.getTitle(), movie.getPath(),
            movie.getDataSource());
        abandonedMovies.add(movie);
      }

      publishState(getProgressDone() + 1);
    }

    if (!abandonedMovies.isEmpty()) {
      module.getMovieList().removeMovies(abandonedMovies);
    }

    return abandonedMovies.size();
  }

  /**
   * Removes all TV shows whose data source is no longer configured. Their seasons and episodes are cascaded away by the removal itself.
   *
   * @return the number of removed TV shows
   */
  private int cleanupAbandonedTvShows() {
    TvShowModuleManager module = TvShowModuleManager.getInstance();
    List<Path> dataSources = normalizeDataSources(module.getSettings().getTvShowDataSource());
    if (dataSources.isEmpty()) {
      // would flag the complete database as abandoned - definitely a broken setup, so do not touch anything here
      warnAboutMissingDataSources(module.getModuleTitle());
      return 0;
    }

    List<TvShow> tvShows = new ArrayList<>(module.getTvShowList().getTvShows());
    setWorkUnits(getWorkUnits() + tvShows.size());
    LOGGER.debug("Checking {} TV shows for abandoned database entries", tvShows.size());

    List<TvShow> abandonedTvShows = new ArrayList<>();
    for (TvShow tvShow : tvShows) {
      if (cancel) {
        break;
      }

      if (!isCoveredByDataSource(tvShow.getDataSource(), tvShow.getPath(), dataSources)) {
        LOGGER.debug("Removing abandoned TV show '{}' ('{}') - data source '{}' is no longer configured", tvShow.getTitle(), tvShow.getPath(),
            tvShow.getDataSource());
        abandonedTvShows.add(tvShow);
      }

      publishState(getProgressDone() + 1);
    }

    for (TvShow tvShow : abandonedTvShows) {
      module.getTvShowList().removeTvShow(tvShow);
    }

    return abandonedTvShows.size();
  }

  /**
   * Checks whether the given entity is covered by one of the configured data sources. An entity is covered when its data source is one of the
   * configured roots or located inside one of them - and as a safety net also when its path is located inside a configured root (which catches
   * entities whose stored data source has become outdated).
   *
   * @param dataSource
   *          the data source of the entity
   * @param path
   *          the path of the entity
   * @param dataSources
   *          the normalized roots of all configured data sources
   * @return true if the entity still belongs to a configured data source
   */
  private boolean isCoveredByDataSource(String dataSource, String path, List<Path> dataSources) {
    if (isInsideDataSources(toPath(dataSource), dataSources)) {
      return true;
    }

    // the data source is gone from the settings, but the entity still lives inside another configured one
    return isInsideDataSources(toPath(path), dataSources);
  }

  /**
   * Checks whether the given path equals one of the data source roots or is located below one of them.
   *
   * @param candidate
   *          the path to check (may be null)
   * @param dataSources
   *          the normalized roots of all configured data sources
   * @return true if the path is covered by one of the data sources
   */
  private boolean isInsideDataSources(Path candidate, List<Path> dataSources) {
    if (candidate == null) {
      return false;
    }

    for (Path dataSource : dataSources) {
      if (dataSource.equals(candidate) || candidate.startsWith(dataSource)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Normalizes the configured data sources to absolute paths, ignoring blank and invalid ones.
   *
   * @param configuredDataSources
   *          the data sources from the settings
   * @return the normalized data source roots
   */
  private List<Path> normalizeDataSources(List<String> configuredDataSources) {
    List<Path> dataSources = new ArrayList<>();

    for (String configuredDataSource : configuredDataSources) {
      Path dataSource = toPath(configuredDataSource);
      if (dataSource == null) {
        LOGGER.debug("Ignoring the invalid data source '{}'", configuredDataSource);
        continue;
      }

      dataSources.add(dataSource);
    }

    return dataSources;
  }

  /**
   * Converts the given path into a normalized absolute path.
   *
   * @param path
   *          the path to convert (may be blank)
   * @return the normalized absolute path, or null when the path is blank or invalid
   */
  private Path toPath(String path) {
    if (StringUtils.isBlank(path)) {
      return null;
    }

    try {
      return Paths.get(path).normalize().toAbsolutePath();
    }
    catch (InvalidPathException e) {
      LOGGER.debug("Could not resolve the path '{}': {}", path, e.getMessage());
      return null;
    }
  }

  /**
   * Pushes a warning that the abandoned check has been skipped because the module has no usable data source at all.
   *
   * @param moduleTitle
   *          the title of the skipped module
   */
  private void warnAboutMissingDataSources(String moduleTitle) {
    LOGGER.warn("No data source configured for '{}' - skipping the abandoned entity check", moduleTitle);
    MessageManager.getInstance()
        .pushMessage(new Message(MessageLevel.WARN, "cleanup.abandoned", "tmm.cleanupabandoned.skipped", new String[] { moduleTitle }));
  }
}
