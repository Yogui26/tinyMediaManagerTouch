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
package org.tinymediamanager.core.tvshow.jmte;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.bus.Event;
import org.tinymediamanager.core.bus.EventBus;
import org.tinymediamanager.core.bus.IEventListener;
import org.tinymediamanager.core.jmte.TmmModelAdaptor;
import org.tinymediamanager.core.tvshow.TvShowCustomColumn;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowRenamer;
import org.tinymediamanager.core.tvshow.TvShowRenamerProfile;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.message.ParseException;
import com.floreysoft.jmte.template.Template;

/**
 * The Class {@link TvShowCustomColumnEvaluator} renders the content of the freely configurable columns of the TV show table.
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
 * The TV show table is hierarchical, so it asks this evaluator for three different levels: a {@link TvShow} is rendered with the show as its only
 * context, a {@link TvShowSeason} additionally gets its show, and a {@link TvShowEpisode} gets its show and its season. Since a season or an episode
 * is rendered with data of its parents, changing a parent has to invalidate the cached values of its children too.
 * <p>
 * The evaluator is meant to live as long as the table using it, so it simply gets created as a field of the table format.
 *
 * @author Manuel Laggner
 */
public class TvShowCustomColumnEvaluator {
  private static final Logger                  LOGGER         = LoggerFactory.getLogger(TvShowCustomColumnEvaluator.class);

  private final TvShowSettings                 settings;

  /** the one and only JMTE engine, created lazily and reused for every single evaluation */
  private volatile Engine                      engine;

  /** the parsed templates by their already compiled pattern */
  private final Map<String, Template>          templates      = new ConcurrentHashMap<>();

  /** the already known broken patterns, to not log them over and over again */
  private final Set<String>                    brokenPatterns = ConcurrentHashMap.newKeySet();

  /** the rendered values of every custom column by its id, and within that by the db id of the entity it was rendered for */
  private final Map<String, Map<UUID, String>> values         = new ConcurrentHashMap<>();

  /** drops the cached values of a show, its seasons and its episodes as soon as it changes */
  private final IEventListener                 eventListener;

  /** drops the complete cache as soon as the patterns or the renamer settings change */
  private final PropertyChangeListener         settingsListener;

  /**
   * Creates a new evaluator and starts listening for TV show, season, episode and settings changes.
   */
  public TvShowCustomColumnEvaluator() {
    settings = TvShowModuleManager.getInstance().getSettings();

    // drop the cached values whenever a show, a season or an episode has been changed - exactly the moment the table itself gets updated too
    eventListener = event -> {
      switch (event.eventType()) {
        case Event.TYPE_SAVE:
        case Event.TYPE_ADD:
        case Event.TYPE_REMOVE:
          if (event.sender() instanceof TvShow tvShow) {
            invalidate(tvShow);
          }
          else if (event.sender() instanceof TvShowSeason season) {
            invalidate(season);
          }
          else if (event.sender() instanceof TvShowEpisode episode) {
            invalidate(episode);
          }
          break;

        default:
          break;
      }
    };
    EventBus.registerListener(EventBus.TOPIC_TV_SHOWS, eventListener);

    // new patterns or new renamer settings need a complete restart of the caches
    settingsListener = evt -> {
      if (TvShowSettings.CUSTOM_COLUMNS.equals(evt.getPropertyName()) || TvShowSettings.CUSTOM_COLUMN_CONTENT.equals(evt.getPropertyName())
          || TvShowSettings.RENAMER_PROFILES.equals(evt.getPropertyName())) {
        reload();
      }
    };
    settings.addPropertyChangeListener(settingsListener);
  }

  /**
   * Stops the evaluator from listening to TV show and settings changes. The table format does not need to be evaluated anymore after this.
   */
  public void dispose() {
    EventBus.removeListener(EventBus.TOPIC_TV_SHOWS, eventListener);
    settings.removePropertyChangeListener(settingsListener);
    reload();
  }

  /**
   * Gets the rendered content of the given custom column for the given TV show.
   *
   * @param tvShow
   *          the TV show to render the column for
   * @param customColumn
   *          the custom column to render
   * @return the rendered value, an empty string when the column is not configured or the pattern could not be processed
   */
  public String evaluate(TvShow tvShow, TvShowCustomColumn customColumn) {
    String pattern = StringUtils.defaultString(customColumn.getShowPattern());
    if (StringUtils.isBlank(pattern)) {
      return "";
    }

    return evaluate(customColumn, tvShow.getDbId(), () -> render(tvShow, pattern));
  }

  /**
   * Gets the rendered content of the given custom column for the given season.
   *
   * @param season
   *          the season to render the column for
   * @param customColumn
   *          the custom column to render
   * @return the rendered value, an empty string when the column is not configured or the pattern could not be processed
   */
  public String evaluate(TvShowSeason season, TvShowCustomColumn customColumn) {
    String pattern = StringUtils.defaultString(customColumn.getSeasonPattern());
    if (StringUtils.isBlank(pattern)) {
      return "";
    }

    return evaluate(customColumn, season.getDbId(), () -> render(season, pattern));
  }

  /**
   * Gets the rendered content of the given custom column for the given episode.
   *
   * @param episode
   *          the episode to render the column for
   * @param customColumn
   *          the custom column to render
   * @return the rendered value, an empty string when the column is not configured or the pattern could not be processed
   */
  public String evaluate(TvShowEpisode episode, TvShowCustomColumn customColumn) {
    String pattern = StringUtils.defaultString(customColumn.getEpisodePattern());
    if (StringUtils.isBlank(pattern)) {
      return "";
    }

    return evaluate(customColumn, episode.getDbId(), () -> render(episode, pattern));
  }

  private String evaluate(TvShowCustomColumn customColumn, UUID dbId, Supplier<String> renderer) {
    if (dbId == null) {
      return renderer.get();
    }

    Map<UUID, String> valueCache = values.computeIfAbsent(customColumn.getId(), key -> new ConcurrentHashMap<>());

    String cached = valueCache.get(dbId);
    if (cached != null) {
      return cached;
    }

    String rendered = renderer.get();
    valueCache.put(dbId, rendered);
    return rendered;
  }

  /**
   * Renders the given JMTE pattern for the given TV show without using any value cache.
   *
   * @param tvShow
   *          the TV show to render the pattern for
   * @param pattern
   *          the JMTE pattern, which has to be a full pattern like {@code ${tvShow.title}}
   * @return the rendered value, an empty string when the pattern could not be processed
   */
  private String render(TvShow tvShow, String pattern) {
    Map<String, Object> root = new HashMap<>();
    root.put("tvShow", tvShow);
    return render(pattern, root);
  }

  /**
   * Renders the given JMTE pattern for the given season without using any value cache.
   *
   * @param season
   *          the season to render the pattern for
   * @param pattern
   *          the JMTE pattern, which has to be a full pattern like {@code ${season.season}}
   * @return the rendered value, an empty string when the pattern could not be processed
   */
  private String render(TvShowSeason season, String pattern) {
    Map<String, Object> root = new HashMap<>();
    root.put("tvShow", season.getTvShow());
    root.put("season", season);
    return render(pattern, root);
  }

  /**
   * Renders the given JMTE pattern for the given episode without using any value cache.
   *
   * @param episode
   *          the episode to render the pattern for
   * @param pattern
   *          the JMTE pattern, which has to be a full pattern like {@code ${episode.title}}
   * @return the rendered value, an empty string when the pattern could not be processed
   */
  private String render(TvShowEpisode episode, String pattern) {
    Map<String, Object> root = new HashMap<>();
    root.put("tvShow", episode.getTvShow());
    root.put("season", episode.getTvShowSeason());
    root.put("episode", episode);
    return render(pattern, root);
  }

  private String render(String pattern, Map<String, Object> root) {
    Template template = getTemplate(pattern);
    if (template == null) {
      return "";
    }

    try {
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
    values.clear();
  }

  /**
   * Drops the cached values of the given TV show, its seasons and its episodes in all custom columns.
   *
   * @param tvShow
   *          the TV show which has been changed
   */
  public void invalidate(TvShow tvShow) {
    removeValue(tvShow.getDbId());
    for (TvShowSeason season : tvShow.getSeasons()) {
      removeValue(season.getDbId());
    }
    for (TvShowEpisode episode : tvShow.getEpisodes()) {
      removeValue(episode.getDbId());
    }
  }

  /**
   * Drops the cached values of the given season and its episodes in all custom columns.
   *
   * @param season
   *          the season which has been changed
   */
  public void invalidate(TvShowSeason season) {
    removeValue(season.getDbId());
    for (TvShowEpisode episode : season.getEpisodes()) {
      removeValue(episode.getDbId());
    }
  }

  /**
   * Drops the cached values of the given episode in all custom columns.
   *
   * @param episode
   *          the episode which has been changed
   */
  public void invalidate(TvShowEpisode episode) {
    removeValue(episode.getDbId());
  }

  private void removeValue(UUID dbId) {
    if (dbId == null) {
      return;
    }
    for (Map<UUID, String> valueCache : values.values()) {
      valueCache.remove(dbId);
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
          // copy to make it immutable
          TvShowRenamerProfile renamerProfile = new TvShowRenamerProfile(settings.getDefaultRenamerProfile());

          currentEngine = TvShowRenamer.createEngine(renamerProfile);
          currentEngine.setModelAdaptor(new TmmModelAdaptor());
          // no custom output appender here: a table cell must not be sanitized for the filesystem, it just has to be readable
          engine = currentEngine;
        }
      }
    }
    return currentEngine;
  }
}
