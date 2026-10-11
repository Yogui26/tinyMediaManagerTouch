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
package org.tinymediamanager.core.movie.jmte;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.bus.Event;
import org.tinymediamanager.core.bus.EventBus;
import org.tinymediamanager.core.bus.IEventListener;
import org.tinymediamanager.core.jmte.TmmModelAdaptor;
import org.tinymediamanager.core.movie.MovieCustomColumn;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.MovieRenamerProfile;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.message.ParseException;
import com.floreysoft.jmte.template.Template;

/**
 * The Class {@link MovieCustomColumnEvaluator} renders the content of the freely configurable columns of the movie table and the movie set table.
 * <p>
 * Since these columns are evaluated on every single cell paint and on every single comparison while sorting, this evaluator is very picky about
 * performance:
 * <ul>
 * <li>one single {@link Engine} instance is created and reused instead of a fresh one per call - JMTE engines are thread safe, their
 * {@code transform()} is synchronized</li>
 * <li>parsed {@link Template}s are cached by their pattern - JMTE does <b>not</b> cache parsed templates internally, it would re-parse the pattern on
 * every single call otherwise</li>
 * <li>the rendered value is cached per entity and column, which turns sorting a column from O(n log n) template renders into O(n) ones</li>
 * </ul>
 * The movie set table is hierarchical, so it asks this evaluator for two different levels: a {@link MovieSet} is rendered by the pattern of the
 * column which uses the set as its only context, a {@link Movie} by the pattern which uses the movie (and its set) as context.
 * <p>
 * The evaluator is meant to live as long as the table using it, so it simply gets created as a field of the table format.
 *
 * @author Manuel Laggner
 */
public class MovieCustomColumnEvaluator {
  private static final Logger                    LOGGER         = LoggerFactory.getLogger(MovieCustomColumnEvaluator.class);

  private final MovieSettings                    settings;

  /** the one and only JMTE engine, created lazily and reused for every single evaluation */
  private volatile Engine                        engine;

  /** the parsed templates by their already compiled pattern */
  private final Map<String, Template>            templates      = new ConcurrentHashMap<>();

  /** the already known broken patterns, to not log them over and over again */
  private final Set<String>                      brokenPatterns = ConcurrentHashMap.newKeySet();

  /** the rendered values of every custom column by its id, and within that by the movie together with the set it was rendered in */
  private final Map<String, Map<String, String>> movieValues    = new ConcurrentHashMap<>();

  /** the rendered values of every custom column by its id, and within that by the db id of the movie set */
  private final Map<String, Map<UUID, String>>   setValues      = new ConcurrentHashMap<>();

  /** drops the cached values of a movie as soon as it changes */
  private final IEventListener                   movieListener;

  /** drops the cached values of a movie set as soon as it changes */
  private final IEventListener                   movieSetListener;

  /** drops the complete cache as soon as the patterns or the renamer settings change */
  private final PropertyChangeListener           settingsListener;

  /**
   * Creates a new evaluator and starts listening for movie, movie set and settings changes.
   */
  public MovieCustomColumnEvaluator() {
    settings = MovieModuleManager.getInstance().getSettings();

    // drop the cached values whenever a movie or a movie set has been changed - exactly the moment the table itself gets updated too
    movieListener = event -> {
      if (event.sender() instanceof Movie movie) {
        switch (event.eventType()) {
          case Event.TYPE_SAVE:
          case Event.TYPE_ADD:
          case Event.TYPE_REMOVE:
            invalidate(movie);
            break;

          default:
            break;
        }
      }
    };
    EventBus.registerListener(EventBus.TOPIC_MOVIES, movieListener);

    movieSetListener = event -> {
      if (event.sender() instanceof MovieSet movieSet) {
        switch (event.eventType()) {
          case Event.TYPE_SAVE:
          case Event.TYPE_ADD:
          case Event.TYPE_REMOVE:
            invalidate(movieSet);
            break;

          default:
            break;
        }
      }
    };
    EventBus.registerListener(EventBus.TOPIC_MOVIE_SETS, movieSetListener);

    // new patterns or new renamer settings need a complete restart of the caches
    settingsListener = evt -> {
      if (MovieSettings.CUSTOM_COLUMNS.equals(evt.getPropertyName()) || MovieSettings.MOVIE_SET_CUSTOM_COLUMNS.equals(evt.getPropertyName())
          || MovieSettings.CUSTOM_COLUMN_CONTENT.equals(evt.getPropertyName()) || MovieSettings.RENAMER_PROFILES.equals(evt.getPropertyName())) {
        reload();
      }
    };
    settings.addPropertyChangeListener(settingsListener);
  }

  /**
   * Stops the evaluator from listening to movie, movie set and settings changes. The table format does not need to be evaluated anymore after this.
   */
  public void dispose() {
    EventBus.removeListener(EventBus.TOPIC_MOVIES, movieListener);
    EventBus.removeListener(EventBus.TOPIC_MOVIE_SETS, movieSetListener);
    settings.removePropertyChangeListener(settingsListener);
    reload();
  }

  /**
   * Gets the rendered content of the given custom column for the given movie.
   *
   * @param movie
   *          the movie to render the column for
   * @param customColumn
   *          the custom column to render
   * @return the rendered value, an empty string when the column is not configured or the pattern could not be processed
   */
  public String evaluate(Movie movie, MovieCustomColumn customColumn) {
    return evaluate(movie, null, customColumn);
  }

  /**
   * Gets the rendered content of the given custom column for the given movie, which is displayed as a child row of the given movie set. Needed by the
   * movie set table, where a movie of a set does not necessarily know its own set.
   *
   * @param movie
   *          the movie to render the column for
   * @param movieSet
   *          the movie set this movie is shown in, or null to use only what the movie itself knows
   * @param customColumn
   *          the custom column to render
   * @return the rendered value, an empty string when the column is not configured or the pattern could not be processed
   */
  public String evaluate(Movie movie, MovieSet movieSet, MovieCustomColumn customColumn) {
    String pattern = StringUtils.defaultString(customColumn.getPattern());
    if (StringUtils.isBlank(pattern)) {
      return "";
    }

    // the very same movie renders differently when it is shown in another set, so the set belongs into the cache key
    MovieSet effectiveSet = movie.getMovieSet() != null ? movie.getMovieSet() : movieSet;
    String cacheKey = effectiveSet == null ? movie.getDbId().toString() : movie.getDbId() + "@" + effectiveSet.getDbId();

    Map<String, String> valueCache = movieValues.computeIfAbsent(customColumn.getId(), key -> new ConcurrentHashMap<>());

    String cached = valueCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    String rendered = render(movie, movieSet, pattern);
    valueCache.put(cacheKey, rendered);
    return rendered;
  }

  /**
   * Gets the rendered content of the given custom column for the given movie set.
   *
   * @param movieSet
   *          the movie set to render the column for
   * @param customColumn
   *          the custom column to render
   * @return the rendered value, an empty string when the column is not configured or the pattern could not be processed
   */
  public String evaluate(MovieSet movieSet, MovieCustomColumn customColumn) {
    String pattern = StringUtils.defaultString(customColumn.getSetPattern());
    if (StringUtils.isBlank(pattern)) {
      return "";
    }

    Map<UUID, String> valueCache = setValues.computeIfAbsent(customColumn.getId(), key -> new ConcurrentHashMap<>());
    UUID dbId = movieSet.getDbId();

    String cached = valueCache.get(dbId);
    if (cached != null) {
      return cached;
    }

    String rendered = render(movieSet, pattern);
    valueCache.put(dbId, rendered);
    return rendered;
  }

  /**
   * Renders the given JMTE pattern for the given movie without using any value cache.
   *
   * @param movie
   *          the movie to render the pattern for
   * @param movieSet
   *          the movie set this movie is shown in, or null to use only what the movie itself knows
   * @param pattern
   *          the JMTE pattern, which has to be a full pattern like {@code ${movie.title}}
   * @return the rendered value, an empty string when the pattern could not be processed
   */
  private String render(Movie movie, MovieSet movieSet, String pattern) {
    Template template = getTemplate(pattern);
    if (template == null) {
      return "";
    }

    try {
      Map<String, Object> root = new HashMap<>();
      root.put("movie", movie);

      MovieSet set = movie.getMovieSet() != null ? movie.getMovieSet() : movieSet;
      if (set != null) {
        root.put("movieSet", set);
      }

      return template.transform(root, Locale.getDefault());
    }
    catch (Exception e) {
      complain(pattern, e);
      return "";
    }
  }

  /**
   * Renders the given JMTE pattern for the given movie set without using any value cache. Such a pattern always addresses the set itself, since a set
   * row has no movie in its context.
   *
   * @param movieSet
   *          the movie set to render the pattern for
   * @param pattern
   *          the JMTE pattern, which has to be a full pattern like {@code ${movieSet.title}}
   * @return the rendered value, an empty string when the pattern could not be processed
   */
  private String render(MovieSet movieSet, String pattern) {
    Template template = getTemplate(pattern);
    if (template == null) {
      return "";
    }

    try {
      Map<String, Object> root = new HashMap<>();
      root.put("movieSet", movieSet);

      return template.transform(root, Locale.getDefault());
    }
    catch (Exception e) {
      complain(pattern, e);
      return "";
    }
  }

  /**
   * Drops all cached templates, values and the engine itself. Needed when patterns or renamer settings have been changed.
   */
  public void reload() {
    engine = null;
    templates.clear();
    brokenPatterns.clear();
    movieValues.clear();
    setValues.clear();
  }

  /**
   * Drops the cached values of the given movie in all custom columns.
   *
   * @param movie
   *          the movie which has been changed
   */
  public void invalidate(Movie movie) {
    String dbId = movie.getDbId().toString();

    // the movie itself, and every rendering of it which remembered its parent set
    for (Map<String, String> values : movieValues.values()) {
      values.keySet().removeIf(key -> key.equals(dbId) || key.startsWith(dbId + "@"));
    }
  }

  /**
   * Drops the cached values of the given movie set and of every movie which was rendered in the context of this set.
   *
   * @param movieSet
   *          the movie set which has been changed
   */
  public void invalidate(MovieSet movieSet) {
    UUID dbId = movieSet.getDbId();

    // the set rows themselves
    for (Map<UUID, String> values : setValues.values()) {
      values.remove(dbId);
    }

    // and every movie row which remembered this set (both the movie set table and the flat movie table render like this)
    String setSuffix = "@" + dbId;
    for (Map<String, String> values : movieValues.values()) {
      values.keySet().removeIf(key -> key.endsWith(setSuffix));
    }
  }

  private Template getTemplate(String pattern) {
    if (brokenPatterns.contains(pattern)) {
      return null;
    }

    Template cached = templates.get(pattern);
    if (cached != null) {
      return cached;
    }

    try {
      Template template = getEngine().getTemplate(pattern);
      templates.put(pattern, template);
      return template;
    }
    catch (ParseException e) {
      brokenPatterns.add(pattern);
      LOGGER.warn("Invalid JMTE pattern in a custom table column: '{}' - '{}'", pattern, e.getMessage());
      return null;
    }
  }

  private void complain(String pattern, Exception e) {
    // a pattern which renders wrong should be complained about once, not once per entity and repaint
    if (brokenPatterns.add(pattern)) {
      LOGGER.warn("Unable to process the JMTE pattern of a custom table column: '{}' - '{}'", pattern, e.getMessage());
    }
  }

  private Engine getEngine() {
    Engine currentEngine = engine;
    if (currentEngine == null) {
      synchronized (this) {
        currentEngine = engine;
        if (currentEngine == null) {
          // copy to make it immutable, we only need the first character replacement of the default profile
          MovieRenamerProfile renamerProfile = new MovieRenamerProfile(settings.getDefaultRenamerProfile());

          currentEngine = MovieRenamer.createEngine(renamerProfile);
          currentEngine.setModelAdaptor(new TmmModelAdaptor());
          // no custom output appender here: a table cell must not be sanitized for the filesystem, it just has to be readable
          engine = currentEngine;
        }
      }
    }
    return currentEngine;
  }
}
