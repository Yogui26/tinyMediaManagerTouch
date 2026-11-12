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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * The Class {@link WebAppHandler} serves the static files of the bundled web UI from the classpath folder {@code /webapp}. Unknown paths without a
 * file extension are answered with the {@code index.html} to support client-side routing.
 *
 * @author Manuel Laggner
 */
public class WebAppHandler implements HttpHandler {
  private static final Logger LOGGER        = LoggerFactory.getLogger(WebAppHandler.class);

  /** the classpath folder containing the built web UI */
  public static final String  RESOURCE_ROOT = "/webapp";

  private static final String INDEX         = "/index.html";

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    try {
      if (!"GET".equalsIgnoreCase(httpExchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(httpExchange.getRequestMethod())) {
        send(httpExchange, 405, "text/plain; charset=utf-8", "method not allowed".getBytes(StandardCharsets.UTF_8));
        return;
      }

      String resourcePath = resolveResourcePath(httpExchange.getRequestURI().getPath());
      byte[] content = resourcePath == null ? null : readResource(resourcePath);

      if (content == null && resourcePath != null && !hasFileExtension(resourcePath)) {
        // client-side route - deliver the application shell
        resourcePath = RESOURCE_ROOT + INDEX;
        content = readResource(resourcePath);
      }

      if (content == null) {
        send(httpExchange, 404, "text/plain; charset=utf-8", "not found".getBytes(StandardCharsets.UTF_8));
        return;
      }

      // the hashed assets never change; the application shell must always be revalidated
      boolean immutable = resourcePath.startsWith(RESOURCE_ROOT + "/assets/");
      httpExchange.getResponseHeaders().set("Cache-Control", immutable ? "public, max-age=31536000, immutable" : "no-cache");
      send(httpExchange, 200, contentType(resourcePath), content);
    }
    catch (Exception e) {
      LOGGER.error("Could not deliver web UI resource '{}' - '{}'", httpExchange.getRequestURI().getPath(), e.getMessage());
      send(httpExchange, 500, "text/plain; charset=utf-8", "internal error".getBytes(StandardCharsets.UTF_8));
    }
    finally {
      httpExchange.close();
    }
  }

  /**
   * Maps the request path to the classpath resource to deliver.
   *
   * @param requestPath
   *          the (already decoded) request path
   * @return the classpath resource path or null if the request path is not allowed
   */
  public static String resolveResourcePath(String requestPath) {
    if (requestPath == null || requestPath.isEmpty() || "/".equals(requestPath)) {
      return RESOURCE_ROOT + INDEX;
    }

    // unknown API paths must not be answered with the application shell
    if (requestPath.equals("/api") || requestPath.startsWith("/api/")) {
      return null;
    }

    // never leave the web UI folder
    if (!requestPath.startsWith("/") || requestPath.contains("..") || requestPath.contains("\\") || requestPath.contains("//")
        || requestPath.indexOf('\0') >= 0) {
      return null;
    }

    return RESOURCE_ROOT + requestPath;
  }

  /**
   * Checks whether the last segment of the given path has a file extension.
   *
   * @param path
   *          the path to check
   * @return true if the last path segment contains a dot
   */
  public static boolean hasFileExtension(String path) {
    return path.substring(path.lastIndexOf('/') + 1).contains(".");
  }

  /**
   * Gets the content type for the given resource path.
   *
   * @param path
   *          the resource path
   * @return the content type
   */
  public static String contentType(String path) {
    String lowerCase = path.toLowerCase(Locale.ROOT);
    String extension = lowerCase.substring(lowerCase.lastIndexOf('.') + 1);

    return switch (extension) {
      case "html" -> "text/html; charset=utf-8";
      case "js", "mjs" -> "text/javascript; charset=utf-8";
      case "css" -> "text/css; charset=utf-8";
      case "json", "map" -> "application/json; charset=utf-8";
      case "webmanifest" -> "application/manifest+json; charset=utf-8";
      case "svg" -> "image/svg+xml";
      case "png" -> "image/png";
      case "jpg", "jpeg" -> "image/jpeg";
      case "webp" -> "image/webp";
      case "ico" -> "image/x-icon";
      case "woff2" -> "font/woff2";
      case "woff" -> "font/woff";
      case "txt" -> "text/plain; charset=utf-8";
      default -> "application/octet-stream";
    };
  }

  private byte[] readResource(String resourcePath) throws IOException {
    if (resourcePath.endsWith("/")) {
      return null;
    }

    try (InputStream is = WebAppHandler.class.getResourceAsStream(resourcePath)) {
      return is == null ? null : is.readAllBytes();
    }
  }

  private void send(HttpExchange httpExchange, int responseCode, String contentType, byte[] content) throws IOException {
    httpExchange.getResponseHeaders().set("Content-Type", contentType);
    httpExchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");

    if ("HEAD".equalsIgnoreCase(httpExchange.getRequestMethod())) {
      httpExchange.sendResponseHeaders(responseCode, -1);
      return;
    }

    httpExchange.sendResponseHeaders(responseCode, content.length);
    try (OutputStream out = httpExchange.getResponseBody()) {
      out.write(content);
    }
  }
}
