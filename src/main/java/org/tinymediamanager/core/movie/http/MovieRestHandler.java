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
package org.tinymediamanager.core.movie.http;

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
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;

import com.sun.net.httpserver.HttpExchange;

/**
 * The Class {@link MovieRestHandler} provides the read-only movie resources of the v2 HTTP API:
 * <ul>
 * <li>{@code GET /api/v2/movies} - paged, filterable and sortable list of all movies</li>
 * <li>{@code GET /api/v2/movies/{id}} - details of a single movie</li>
 * <li>{@code GET /api/v2/movies/{id}/artwork/{type}} - local artwork of a movie</li>
 * </ul>
 *
 * @author Manuel Laggner
 */
public class MovieRestHandler extends AbstractRestHandler {
  /** the context path (without the {@code /api/} prefix) this handler is registered at */
  public static final String                      CONTEXT       = "v2/movies";

  private static final Map<String, MediaFileType> ARTWORK_TYPES = createArtworkTypes();

  /**
   * Creates a new handler.
   */
  public MovieRestHandler() {
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

  @Override
  protected void handleGet(HttpExchange httpExchange, List<String> segments, Map<String, String> query) throws Exception {
    if (segments.isEmpty()) {
      sendJson(httpExchange, 200, "OK", createPage(listMovies(query), query));
      return;
    }

    Movie movie = lookupMovie(parseUuid(segments.get(0)));
    if (movie == null) {
      sendJson(httpExchange, 404, "movie not found", null);
      return;
    }

    if (segments.size() == 1) {
      sendJson(httpExchange, 200, "OK", toDetail(movie));
    }
    else if (segments.size() == 3 && "artwork".equals(segments.get(1))) {
      sendArtwork(httpExchange, movie, segments.get(2), ARTWORK_TYPES);
    }
    else {
      sendJson(httpExchange, 404, "unknown resource", null);
    }
  }

  private Movie lookupMovie(UUID uuid) {
    if (uuid == null) {
      return null;
    }

    for (Movie movie : MovieModuleManager.getInstance().getMovieList().getMovies()) {
      if (uuid.equals(movie.getDbId())) {
        return movie;
      }
    }

    return null;
  }

  private List<Map<String, Object>> listMovies(Map<String, String> query) {
    String search = StringUtils.trimToEmpty(query.get("q")).toLowerCase(Locale.ROOT);
    String genre = StringUtils.trimToEmpty(query.get("genre"));
    String watched = StringUtils.trimToEmpty(query.get("watched"));

    List<Movie> movies = new ArrayList<>();
    for (Movie movie : MovieModuleManager.getInstance().getMovieList().getMovies()) {
      if (!search.isEmpty() && !containsIgnoreCase(movie.getTitle(), search) && !containsIgnoreCase(movie.getOriginalTitle(), search)) {
        continue;
      }
      if (!watched.isEmpty() && Boolean.parseBoolean(watched) != movie.isWatched()) {
        continue;
      }
      if (!genre.isEmpty() && !hasGenre(movie, genre)) {
        continue;
      }
      movies.add(movie);
    }

    movies.sort(createComparator(query.get("sort"), query.get("order")));

    List<Map<String, Object>> result = new ArrayList<>(movies.size());
    for (Movie movie : movies) {
      result.add(toSummary(movie));
    }
    return result;
  }

  private boolean hasGenre(Movie movie, String genre) {
    for (MediaGenres mediaGenre : movie.getGenres()) {
      if (genre.equalsIgnoreCase(mediaGenre.name()) || genre.equalsIgnoreCase(mediaGenre.getLocalizedName())) {
        return true;
      }
    }
    return false;
  }

  private Comparator<Movie> createComparator(String sort, String order) {
    Comparator<Movie> byTitle = Comparator.comparing(movie -> StringUtils.defaultString(movie.getTitleSortable()), String.CASE_INSENSITIVE_ORDER);

    Comparator<Movie> comparator;
    switch (StringUtils.defaultString(sort)) {
      case "year":
        comparator = Comparator.comparingInt(Movie::getYear);
        break;

      case "rating":
        comparator = Comparator.comparingDouble(movie -> ratingValue(movie.getRating()));
        break;

      case "dateAdded":
        comparator = Comparator.comparing(Movie::getDateAdded, Comparator.nullsFirst(Comparator.naturalOrder()));
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

  private Map<String, Object> toSummary(Movie movie) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", movie.getDbId().toString());
    map.put("title", movie.getTitle());
    map.put("originalTitle", movie.getOriginalTitle());
    map.put("year", movie.getYear());
    map.put("rating", ratingValue(movie.getRating()));
    map.put("runtime", movie.getRuntime());
    map.put("watched", movie.isWatched());
    map.put("genres", toGenres(movie));
    map.put("certification", movie.getCertification() == null ? null : movie.getCertification().getLocalizedName());
    map.put("videoFormat", movie.getMediaInfoVideoFormat());
    map.put("dateAdded", toIsoString(movie.getDateAdded()));
    map.put("hasPoster", hasArtwork(movie, MediaFileType.POSTER));
    map.put("hasFanart", hasArtwork(movie, MediaFileType.FANART));
    return map;
  }

  private Map<String, Object> toDetail(Movie movie) {
    Map<String, Object> map = toSummary(movie);
    map.put("sortTitle", movie.getSortTitle());
    map.put("tagline", movie.getTagline());
    map.put("plot", movie.getPlot());
    map.put("releaseDate", toIsoString(movie.getReleaseDate()));
    map.put("country", movie.getCountry());
    map.put("spokenLanguages", movie.getSpokenLanguages());
    map.put("productionCompany", movie.getProductionCompany());
    map.put("top250", movie.getTop250());
    map.put("imdbId", movie.getImdbId());
    map.put("tmdbId", movie.getTmdbId());
    map.put("tags", new ArrayList<>(movie.getTags()));
    map.put("ratings", toRatings(movie));
    map.put("actors", toPersons(movie.getActors()));
    map.put("directors", toPersons(movie.getDirectors()));
    map.put("writers", toPersons(movie.getWriters()));

    MovieSet movieSet = movie.getMovieSet();
    if (movieSet != null) {
      Map<String, Object> movieSetMap = new LinkedHashMap<>();
      movieSetMap.put("id", movieSet.getDbId().toString());
      movieSetMap.put("title", movieSet.getTitle());
      map.put("movieSet", movieSetMap);
    }

    Map<String, Object> mediaInfo = new LinkedHashMap<>();
    mediaInfo.put("container", movie.getMediaInfoContainerFormat());
    mediaInfo.put("videoFormat", movie.getMediaInfoVideoFormat());
    mediaInfo.put("videoCodec", movie.getMediaInfoVideoCodec());
    mediaInfo.put("videoResolution", movie.getMediaInfoVideoResolution());
    mediaInfo.put("hdrFormat", movie.getVideoHDRFormat());
    mediaInfo.put("audioCodec", movie.getMediaInfoAudioCodec());
    mediaInfo.put("audioChannels", movie.getMediaInfoAudioChannels());
    mediaInfo.put("audioLanguage", movie.getMediaInfoAudioLanguage());
    mediaInfo.put("fileSize", movie.getVideoFilesize());
    map.put("mediaInfo", mediaInfo);

    map.put("dataSource", movie.getDataSource());
    map.put("path", movie.getPathNIO() == null ? null : movie.getPathNIO().toString());

    Map<String, Boolean> artwork = new LinkedHashMap<>();
    for (Map.Entry<String, MediaFileType> entry : ARTWORK_TYPES.entrySet()) {
      artwork.put(entry.getKey(), hasArtwork(movie, entry.getValue()));
    }
    map.put("artwork", artwork);

    return map;
  }

  private List<String> toGenres(Movie movie) {
    List<String> genres = new ArrayList<>();
    for (MediaGenres genre : movie.getGenres()) {
      genres.add(genre.getLocalizedName());
    }
    return genres;
  }
}
