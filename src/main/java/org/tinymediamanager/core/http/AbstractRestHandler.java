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
package org.tinymediamanager.core.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * The Class {@link AbstractRestHandler} provides the shared functionality for the read-only resource endpoints of the v2 HTTP API (request parsing,
 * JSON responses, paging, and artwork delivery).
 *
 * @author Manuel Laggner
 */
public abstract class AbstractRestHandler implements HttpHandler {
  private static final Logger       LOGGER        = LoggerFactory.getLogger(AbstractRestHandler.class);

  /** the default page size of list responses */
  public static final int           DEFAULT_LIMIT = 100;

  /** the maximum page size of list responses */
  public static final int           MAX_LIMIT     = 500;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final String              contextPath;

  /**
   * Creates a new handler.
   *
   * @param contextPath
   *          the full context path this handler is registered at (e.g. {@code /api/v2/movies})
   */
  protected AbstractRestHandler(String contextPath) {
    this.contextPath = contextPath;
  }

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    try {
      if (!"GET".equalsIgnoreCase(httpExchange.getRequestMethod())) {
        sendJson(httpExchange, 405, "only GET is supported", null);
        return;
      }

      List<String> segments = parsePathSegments(httpExchange.getRequestURI().getPath(), contextPath);
      Map<String, String> query = parseQuery(httpExchange.getRequestURI().getRawQuery());

      handleGet(httpExchange, segments, query);
    }
    catch (IllegalArgumentException e) {
      sendJson(httpExchange, 400, e.getMessage(), null);
    }
    catch (Exception e) {
      LOGGER.error("Could not process request '{}' - '{}'", httpExchange.getRequestURI().getPath(), e.getMessage());
      sendJson(httpExchange, 500, "internal error", null);
    }
    finally {
      httpExchange.close();
    }
  }

  /**
   * Handles a GET request.
   *
   * @param httpExchange
   *          the HTTP exchange
   * @param segments
   *          the decoded path segments after the context path
   * @param query
   *          the decoded query parameters
   * @throws Exception
   *           any {@link Exception} occurred while processing the request
   */
  protected abstract void handleGet(HttpExchange httpExchange, List<String> segments, Map<String, String> query) throws Exception;

  /**
   * Splits the part of the request path after the context path into its decoded segments.
   *
   * @param requestPath
   *          the (already decoded) request path
   * @param contextPath
   *          the context path of the handler
   * @return a {@link List} of all non-empty path segments
   */
  public static List<String> parsePathSegments(String requestPath, String contextPath) {
    List<String> segments = new ArrayList<>();
    if (requestPath == null || !requestPath.startsWith(contextPath)) {
      return segments;
    }

    for (String segment : requestPath.substring(contextPath.length()).split("/")) {
      if (StringUtils.isNotBlank(segment)) {
        segments.add(segment);
      }
    }

    return segments;
  }

  /**
   * Parses the raw query string into a map of decoded parameters. If a parameter is given multiple times, the last one wins.
   *
   * @param rawQuery
   *          the raw (still encoded) query string, may be null
   * @return a {@link Map} of all query parameters
   */
  public static Map<String, String> parseQuery(String rawQuery) {
    Map<String, String> query = new LinkedHashMap<>();
    if (StringUtils.isBlank(rawQuery)) {
      return query;
    }

    for (String pair : rawQuery.split("&")) {
      if (pair.isEmpty()) {
        continue;
      }
      int index = pair.indexOf('=');
      String key = index >= 0 ? pair.substring(0, index) : pair;
      String value = index >= 0 ? pair.substring(index + 1) : "";
      query.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
    }

    return query;
  }

  /**
   * Parses the given value as a UUID.
   *
   * @param value
   *          the value to parse
   * @return the {@link UUID} or null if the value is not a valid UUID
   */
  public static UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    }
    catch (Exception e) {
      return null;
    }
  }

  /**
   * Reads an integer query parameter and clamps it to the given range.
   *
   * @param query
   *          the query parameters
   * @param key
   *          the parameter name
   * @param defaultValue
   *          the value to use if the parameter is missing
   * @param min
   *          the minimum allowed value
   * @param max
   *          the maximum allowed value
   * @return the parsed and clamped value
   * @throws IllegalArgumentException
   *           if the parameter is not a number
   */
  public static int parseInt(Map<String, String> query, String key, int defaultValue, int min, int max) {
    String value = query.get(key);
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }

    try {
      return Math.max(min, Math.min(max, Integer.parseInt(value.trim())));
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("parameter '" + key + "' must be a number");
    }
  }

  /**
   * Builds the payload of a paged list response out of the complete list of items.
   *
   * @param items
   *          all (filtered and sorted) items
   * @param query
   *          the query parameters ({@code offset} and {@code limit} are evaluated)
   * @param <T>
   *          the item type
   * @return the requested page of the given items
   */
  public static <T> Page<T> createPage(List<T> items, Map<String, String> query) {
    int offset = parseInt(query, "offset", 0, 0, Integer.MAX_VALUE);
    int limit = parseInt(query, "limit", DEFAULT_LIMIT, 1, MAX_LIMIT);

    int from = Math.min(offset, items.size());
    int to = (int) Math.min((long) from + limit, items.size());

    return new Page<>(items.size(), from, limit, new ArrayList<>(items.subList(from, to)));
  }

  /**
   * Checks whether the given text contains the search term (case-insensitive).
   *
   * @param text
   *          the text to search in, may be null
   * @param lowerCaseTerm
   *          the search term in lower case
   * @return true if the text contains the term
   */
  protected static boolean containsIgnoreCase(String text, String lowerCaseTerm) {
    return text != null && text.toLowerCase(Locale.ROOT).contains(lowerCaseTerm);
  }

  /**
   * Converts the given date to its ISO-8601 representation.
   *
   * @param date
   *          the date, may be null
   * @return the ISO-8601 string or null
   */
  protected static String toIsoString(Date date) {
    return date == null ? null : date.toInstant().toString();
  }

  /**
   * Gets the value of the given rating.
   *
   * @param rating
   *          the rating, may be null
   * @return the rating value or 0 if there is no rating
   */
  protected static float ratingValue(MediaRating rating) {
    return rating == null ? 0 : rating.getRating();
  }

  /**
   * Converts all ratings of the given entity for the JSON response.
   *
   * @param entity
   *          the entity to get the ratings from
   * @return a {@link List} of all ratings
   */
  protected static List<Map<String, Object>> toRatings(MediaEntity entity) {
    List<Map<String, Object>> ratings = new ArrayList<>();
    for (MediaRating rating : entity.getRatings().values()) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("id", rating.getId());
      map.put("rating", rating.getRating());
      map.put("votes", rating.getVotes());
      map.put("maxValue", rating.getMaxValue());
      ratings.add(map);
    }
    return ratings;
  }

  /**
   * Converts the given persons for the JSON response.
   *
   * @param persons
   *          the persons to convert
   * @return a {@link List} of all persons
   */
  protected static List<Map<String, Object>> toPersons(List<Person> persons) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Person person : persons) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("name", person.getName());
      map.put("role", person.getRole());
      result.add(map);
    }
    return result;
  }

  /**
   * Checks whether the given entity has a local artwork file of the given type.
   *
   * @param entity
   *          the entity
   * @param type
   *          the artwork type
   * @return true if there is a local artwork file
   */
  protected static boolean hasArtwork(MediaEntity entity, MediaFileType type) {
    return StringUtils.isNotBlank(entity.getArtworkFilename(type));
  }

  /**
   * Sends the local artwork of the given entity. The cached (downscaled) image is preferred over the original file.
   *
   * @param httpExchange
   *          the HTTP exchange
   * @param entity
   *          the entity to send the artwork for
   * @param typeName
   *          the artwork type from the request (e.g. {@code poster})
   * @param allowedTypes
   *          the artwork types which may be requested for this entity
   * @throws IOException
   *           any {@link IOException} occurred while sending the response
   */
  protected void sendArtwork(HttpExchange httpExchange, MediaEntity entity, String typeName, Map<String, MediaFileType> allowedTypes)
      throws IOException {
    MediaFileType type = allowedTypes.get(typeName.toLowerCase(Locale.ROOT));
    if (type == null) {
      sendJson(httpExchange, 400, "unknown artwork type '" + typeName + "' - supported types are " + allowedTypes.keySet(), null);
      return;
    }

    String filename = entity.getArtworkFilename(type);
    if (StringUtils.isBlank(filename)) {
      sendJson(httpExchange, 404, "no artwork of type '" + typeName + "' available", null);
      return;
    }

    Path original = Paths.get(filename);
    Path file = null;
    try {
      file = ImageCache.getCachedFile(original);
    }
    catch (Exception e) {
      LOGGER.debug("Could not get cached image for '{}' - '{}'", original, e.getMessage());
    }
    if (file == null || !Files.isRegularFile(file)) {
      file = original;
    }
    if (!Files.isRegularFile(file)) {
      sendJson(httpExchange, 404, "artwork file not found", null);
      return;
    }

    httpExchange.getResponseHeaders().set("Content-Type", imageContentType(file.getFileName().toString()));
    httpExchange.getResponseHeaders().set("Cache-Control", "private, max-age=3600");
    httpExchange.sendResponseHeaders(200, Files.size(file));
    try (OutputStream out = httpExchange.getResponseBody()) {
      Files.copy(file, out);
    }
  }

  /**
   * Gets the content type for the given image file name.
   *
   * @param filename
   *          the file name
   * @return the content type
   */
  public static String imageContentType(String filename) {
    String lowerCase = filename.toLowerCase(Locale.ROOT);
    if (lowerCase.endsWith(".png")) {
      return "image/png";
    }
    if (lowerCase.endsWith(".webp")) {
      return "image/webp";
    }
    if (lowerCase.endsWith(".gif")) {
      return "image/gif";
    }
    if (lowerCase.endsWith(".bmp")) {
      return "image/bmp";
    }
    return "image/jpeg";
  }

  /**
   * Sends a JSON response using the same envelope as the command API ({@code message} and optional {@code data}).
   *
   * @param httpExchange
   *          the HTTP exchange
   * @param responseCode
   *          the HTTP response code
   * @param message
   *          the human-readable result message
   * @param payload
   *          the optional payload, sent as {@code data}
   * @throws IOException
   *           any {@link IOException} occurred while sending the response
   */
  protected void sendJson(HttpExchange httpExchange, int responseCode, String message, Object payload) throws IOException {
    TmmHttpServer.Response response = new TmmHttpServer.Response();
    response.message = message;
    response.data = payload;

    byte[] body = OBJECT_MAPPER.writeValueAsBytes(response);
    httpExchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    httpExchange.getResponseHeaders().set("Cache-Control", "no-store");
    httpExchange.sendResponseHeaders(responseCode, body.length);
    try (OutputStream out = httpExchange.getResponseBody()) {
      out.write(body);
    }
  }

  /**
   * The Class {@link Page} is the payload of a paged list response.
   *
   * @param <T>
   *          the item type
   */
  public static class Page<T> {
    /** the total amount of items matching the request */
    public final int     total;

    /** the offset of the first returned item */
    public final int     offset;

    /** the requested page size */
    public final int     limit;

    /** the items of this page */
    public final List<T> items;

    /**
     * Creates a new page.
     *
     * @param total
     *          the total amount of items matching the request
     * @param offset
     *          the offset of the first returned item
     * @param limit
     *          the requested page size
     * @param items
     *          the items of this page
     */
    public Page(int total, int offset, int limit, List<T> items) {
      this.total = total;
      this.offset = offset;
      this.limit = limit;
      this.items = items;
    }
  }
}
