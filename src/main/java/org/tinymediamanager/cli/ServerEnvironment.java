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
package org.tinymediamanager.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;

/**
 * The Class {@link ServerEnvironment} applies the configuration given by environment variables when tinyMediaManager is started as a server
 * ({@code --start-api}). This allows running without any GUI (e.g. in a container), where the settings dialog is not reachable.
 *
 * @author Manuel Laggner
 */
public final class ServerEnvironment {
  private static final Logger LOGGER                 = LoggerFactory.getLogger(ServerEnvironment.class);

  /** the API key to use; setting it also enables the HTTP API */
  public static final String  ENV_API_KEY            = "TMM_API_KEY";

  /** the port of the HTTP API */
  public static final String  ENV_API_PORT           = "TMM_API_PORT";

  /** the movie data sources to add (separated by {@code ;}) */
  public static final String  ENV_MOVIE_DATASOURCES  = "TMM_MOVIE_DATASOURCES";

  /** the TV show data sources to add (separated by {@code ;}) */
  public static final String  ENV_TVSHOW_DATASOURCES = "TMM_TVSHOW_DATASOURCES";

  private ServerEnvironment() {
    throw new IllegalAccessError();
  }

  /**
   * Applies the given environment to the settings and persists every changed settings file.
   *
   * @param environment
   *          the environment variables
   */
  public static void apply(Map<String, String> environment) {
    applyHttpApi(environment);
    applyDataSources(environment);
  }

  private static void applyHttpApi(Map<String, String> environment) {
    String apiKey = StringUtils.trimToEmpty(environment.get(ENV_API_KEY));
    Integer port = parsePort(environment.get(ENV_API_PORT));

    if (apiKey.isEmpty() && port == null) {
      return;
    }

    Settings settings = Settings.getInstance();
    if (port != null) {
      settings.setHttpServerPort(port.intValue());
    }
    if (!apiKey.isEmpty()) {
      settings.setHttpApiKey(apiKey);
      settings.setEnableHttpServer(true);
      LOGGER.info("HTTP API enabled by the environment on port {}", settings.getHttpServerPort());
    }
    settings.saveSettings();
  }

  private static void applyDataSources(Map<String, String> environment) {
    List<String> movieDataSources = splitPaths(environment.get(ENV_MOVIE_DATASOURCES));
    if (!movieDataSources.isEmpty()) {
      MovieSettings movieSettings = MovieModuleManager.getInstance().getSettings();
      for (String dataSource : movieDataSources) {
        if (!movieSettings.getMovieDataSource().contains(dataSource) && movieSettings.addMovieDataSources(dataSource)) {
          LOGGER.info("added movie data source '{}' from the environment", dataSource);
        }
      }
      movieSettings.saveSettings();
    }

    List<String> tvShowDataSources = splitPaths(environment.get(ENV_TVSHOW_DATASOURCES));
    if (!tvShowDataSources.isEmpty()) {
      TvShowSettings tvShowSettings = TvShowModuleManager.getInstance().getSettings();
      for (String dataSource : tvShowDataSources) {
        if (!tvShowSettings.getTvShowDataSource().contains(dataSource) && tvShowSettings.addTvShowDataSources(dataSource)) {
          LOGGER.info("added TV show data source '{}' from the environment", dataSource);
        }
      }
      tvShowSettings.saveSettings();
    }
  }

  /**
   * Splits a list of paths separated by {@code ;}.
   *
   * @param value
   *          the value of the environment variable, may be null
   * @return a {@link List} of all non-blank, trimmed paths without duplicates
   */
  public static List<String> splitPaths(String value) {
    List<String> paths = new ArrayList<>();
    if (StringUtils.isBlank(value)) {
      return paths;
    }

    for (String path : value.split(";")) {
      String trimmed = path.trim();
      if (!trimmed.isEmpty() && !paths.contains(trimmed)) {
        paths.add(trimmed);
      }
    }

    return paths;
  }

  /**
   * Parses the given port.
   *
   * @param value
   *          the value of the environment variable, may be null
   * @return the port or null if the value is missing or not a valid port
   */
  public static Integer parsePort(String value) {
    if (StringUtils.isBlank(value)) {
      return null;
    }

    try {
      int port = Integer.parseInt(value.trim());
      if (port >= 1 && port <= 65535) {
        return port;
      }
    }
    catch (NumberFormatException e) {
      // handled below
    }

    LOGGER.warn("ignoring invalid HTTP API port '{}'", value);
    return null;
  }
}
