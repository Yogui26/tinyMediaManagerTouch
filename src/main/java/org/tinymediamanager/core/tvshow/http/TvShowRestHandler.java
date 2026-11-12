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
package org.tinymediamanager.core.tvshow.http;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.http.AbstractRestHandler;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;

import com.sun.net.httpserver.HttpExchange;

/**
 * The Class {@link TvShowRestHandler} provides the read-only TV show resources of the v2 HTTP API:
 * <ul>
 * <li>{@code GET /api/v2/tvshows} - paged, filterable and sortable list of all TV shows</li>
 * <li>{@code GET /api/v2/tvshows/{id}} - details of a single TV show including its seasons</li>
 * <li>{@code GET /api/v2/tvshows/{id}/artwork/{type}} - local artwork of a TV show</li>
 * <li>{@code GET /api/v2/tvshows/{id}/seasons/{season}/episodes} - all episodes of a season</li>
 * <li>{@code GET /api/v2/tvshows/{id}/seasons/{season}/artwork/{type}} - local artwork of a season</li>
 * </ul>
 *
 * @author Manuel Laggner
 */
public class TvShowRestHandler extends AbstractRestHandler {
  /** the context path (without the {@code /api/} prefix) this handler is registered at */
  public static final String                      CONTEXT              = "v2/tvshows";

  private static final Map<String, MediaFileType> ARTWORK_TYPES        = createArtworkTypes();
  private static final Map<String, MediaFileType> SEASON_ARTWORK_TYPES = createSeasonArtworkTypes();

  /**
   * Creates a new handler.
   */
  public TvShowRestHandler() {
    super("/api/" + CONTEXT);
  }

  private static Map<String, MediaFileType> createArtworkTypes() {
    Map<String, MediaFileType> types = new LinkedHashMap<>();
    types.put("poster", MediaFileType.POSTER);
    types.put("fanart", MediaFileType.FANART);
    types.put("banner", MediaFileType.BANNER);
    types.put("thumb", MediaFileType.THUMB);
    types.put("clearlogo", MediaFileType.CLEARLOGO);
    return types;
  }

  private static Map<String, MediaFileType> createSeasonArtworkTypes() {
    Map<String, MediaFileType> types = new LinkedHashMap<>();
    types.put("poster", MediaFileType.SEASON_POSTER);
    types.put("fanart", MediaFileType.SEASON_FANART);
    types.put("banner", MediaFileType.SEASON_BANNER);
    types.put("thumb", MediaFileType.SEASON_THUMB);
    return types;
  }

  @Override
  protected void handleGet(HttpExchange httpExchange, List<String> segments, Map<String, String> query) throws Exception {
    if (segments.isEmpty()) {
      sendJson(httpExchange, 200, "OK", createPage(listTvShows(query), query));
      return;
    }

    UUID uuid = parseUuid(segments.get(0));
    TvShow tvShow = uuid == null ? null : TvShowModuleManager.getInstance().getTvShowList().lookupTvShow(uuid);
    if (tvShow == null) {
      sendJson(httpExchange, 404, "TV show not found", null);
      return;
    }

    if (segments.size() == 1) {
      sendJson(httpExchange, 200, "OK", toDetail(tvShow));
    }
    else if (segments.size() == 3 && "artwork".equals(segments.get(1))) {
      sendArtwork(httpExchange, tvShow, segments.get(2), ARTWORK_TYPES);
    }
    else if (segments.size() >= 4 && "seasons".equals(segments.get(1))) {
      handleSeason(httpExchange, tvShow, segments);
    }
    else {
      sendJson(httpExchange, 404, "unknown resource", null);
    }
  }

  private void handleSeason(HttpExchange httpExchange, TvShow tvShow, List<String> segments) throws Exception {
    TvShowSeason season;
    try {
      season = tvShow.getSeason(Integer.parseInt(segments.get(2)));
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("the season must be a number");
    }

    if (season == null) {
      sendJson(httpExchange, 404, "season not found", null);
      return;
    }

    if (segments.size() == 4 && "episodes".equals(segments.get(3))) {
      sendJson(httpExchange, 200, "OK", listEpisodes(season));
    }
    else if (segments.size() == 5 && "artwork".equals(segments.get(3))) {
      sendArtwork(httpExchange, season, segments.get(4), SEASON_ARTWORK_TYPES);
    }
    else {
      sendJson(httpExchange, 404, "unknown resource", null);
    }
  }

  private List<Map<String, Object>> listTvShows(Map<String, String> query) {
    String search = StringUtils.trimToEmpty(query.get("q")).toLowerCase(Locale.ROOT);
    String genre = StringUtils.trimToEmpty(query.get("genre"));
    String watched = StringUtils.trimToEmpty(query.get("watched"));

    List<TvShow> tvShows = new ArrayList<>();
    for (TvShow tvShow : TvShowModuleManager.getInstance().getTvShowList().getTvShows()) {
      if (!search.isEmpty() && !containsIgnoreCase(tvShow.getTitle(), search) && !containsIgnoreCase(tvShow.getOriginalTitle(), search)) {
        continue;
      }
      if (!watched.isEmpty() && Boolean.parseBoolean(watched) != tvShow.isWatched()) {
        continue;
      }
      if (!genre.isEmpty() && !hasGenre(tvShow, genre)) {
        continue;
      }
      tvShows.add(tvShow);
    }

    tvShows.sort(createComparator(query.get("sort"), query.get("order")));

    List<Map<String, Object>> result = new ArrayList<>(tvShows.size());
    for (TvShow tvShow : tvShows) {
      result.add(toSummary(tvShow));
    }
    return result;
  }

  private boolean hasGenre(TvShow tvShow, String genre) {
    for (MediaGenres mediaGenre : tvShow.getGenres()) {
      if (genre.equalsIgnoreCase(mediaGenre.name()) || genre.equalsIgnoreCase(mediaGenre.getLocalizedName())) {
        return true;
      }
    }
    return false;
  }

  private Comparator<TvShow> createComparator(String sort, String order) {
    Comparator<TvShow> byTitle = Comparator.comparing(tvShow -> StringUtils.defaultString(tvShow.getTitleSortable()),
        String.CASE_INSENSITIVE_ORDER);

    Comparator<TvShow> comparator;
    switch (StringUtils.defaultString(sort)) {
      case "year":
        comparator = Comparator.comparingInt(TvShow::getYear);
        break;

      case "rating":
        comparator = Comparator.comparingDouble(tvShow -> ratingValue(tvShow.getRating()));
        break;

      case "dateAdded":
        comparator = Comparator.comparing(TvShow::getDateAdded, Comparator.nullsFirst(Comparator.naturalOrder()));
        break;

      case "", "title":
        comparator = byTitle;
        break;

      default:
        throw new IllegalArgumentException("unknown sort '" + sort + "' - supported values are title, year, rating and dateAdded");
    }

    if ("desc".equalsIgnoreCase(order)) {
      comparator = comparator.reversed();
    }

    // stable secondary order
    return comparator.thenComparing(byTitle);
  }

  private Map<String, Object> toSummary(TvShow tvShow) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", tvShow.getDbId().toString());
    map.put("title", tvShow.getTitle());
    map.put("originalTitle", tvShow.getOriginalTitle());
    map.put("year", tvShow.getYear());
    map.put("rating", ratingValue(tvShow.getRating()));
    map.put("watched", tvShow.isWatched());
    map.put("genres", toGenres(tvShow));
    map.put("certification", tvShow.getCertification() == null ? null : tvShow.getCertification().getLocalizedName());
    map.put("status", tvShow.getStatus() == null ? null : tvShow.getStatus().getLocalizedName());
    map.put("seasonCount", tvShow.getSeasonCount());
    map.put("episodeCount", tvShow.getEpisodeCount());
    map.put("dateAdded", toIsoString(tvShow.getDateAdded()));
    map.put("hasPoster", hasArtwork(tvShow, MediaFileType.POSTER));
    map.put("hasFanart", hasArtwork(tvShow, MediaFileType.FANART));
    return map;
  }

  private Map<String, Object> toDetail(TvShow tvShow) {
    Map<String, Object> map = toSummary(tvShow);
    map.put("plot", tvShow.getPlot());
    map.put("firstAired", toIsoString(tvShow.getFirstAired()));
    map.put("runtime", tvShow.getRuntime());
    map.put("country", tvShow.getCountry());
    map.put("productionCompany", tvShow.getProductionCompany());
    map.put("tags", new ArrayList<>(tvShow.getTags()));
    map.put("ratings", toRatings(tvShow));
    map.put("actors", toPersons(tvShow.getActors()));
    map.put("dataSource", tvShow.getDataSource());
    map.put("path", tvShow.getPathNIO() == null ? null : tvShow.getPathNIO().toString());

    Map<String, Boolean> artwork = new LinkedHashMap<>();
    for (Map.Entry<String, MediaFileType> entry : ARTWORK_TYPES.entrySet()) {
      artwork.put(entry.getKey(), hasArtwork(tvShow, entry.getValue()));
    }
    map.put("artwork", artwork);

    List<TvShowSeason> seasons = new ArrayList<>(tvShow.getSeasons());
    seasons.sort(Comparator.comparingInt(TvShowSeason::getSeason));

    List<Map<String, Object>> seasonList = new ArrayList<>();
    for (TvShowSeason season : seasons) {
      int episodeCount = countEpisodes(season);
      if (episodeCount == 0) {
        // only seasons with episodes on disk
        continue;
      }

      Map<String, Object> seasonMap = new LinkedHashMap<>();
      seasonMap.put("season", season.getSeason());
      seasonMap.put("title", season.getTitle());
      seasonMap.put("episodeCount", episodeCount);
      seasonMap.put("watched", season.isWatched());
      seasonMap.put("hasPoster", hasArtwork(season, MediaFileType.SEASON_POSTER));
      seasonList.add(seasonMap);
    }
    map.put("seasons", seasonList);

    return map;
  }

  private int countEpisodes(TvShowSeason season) {
    int count = 0;
    for (TvShowEpisode episode : season.getEpisodes()) {
      if (!episode.isDummy()) {
        count++;
      }
    }
    return count;
  }

  private List<Map<String, Object>> listEpisodes(TvShowSeason season) {
    List<TvShowEpisode> episodes = new ArrayList<>();
    for (TvShowEpisode episode : season.getEpisodes()) {
      if (!episode.isDummy()) {
        episodes.add(episode);
      }
    }
    episodes.sort(Comparator.comparingInt(TvShowEpisode::getEpisode));

    List<Map<String, Object>> result = new ArrayList<>(episodes.size());
    for (TvShowEpisode episode : episodes) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("id", episode.getDbId().toString());
      map.put("season", episode.getSeason());
      map.put("episode", episode.getEpisode());
      map.put("title", episode.getTitle());
      map.put("plot", episode.getPlot());
      map.put("firstAired", toIsoString(episode.getFirstAired()));
      map.put("rating", ratingValue(episode.getRating()));
      map.put("runtime", episode.getRuntime());
      map.put("watched", episode.isWatched());
      map.put("videoFormat", episode.getMediaInfoVideoFormat());
      result.add(map);
    }
    return result;
  }

  private List<String> toGenres(TvShow tvShow) {
    List<String> genres = new ArrayList<>();
    for (MediaGenres genre : tvShow.getGenres()) {
      genres.add(genre.getLocalizedName());
    }
    return genres;
  }
}
