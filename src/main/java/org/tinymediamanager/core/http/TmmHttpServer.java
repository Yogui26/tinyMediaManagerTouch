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
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.TinyMediaManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.threading.TmmTaskManager;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * the class {@link TmmHttpServer} is used to provide a restful server for HTTP access to tinyMediaManager
 *
 * @author Manuel Laggner
 */
public class TmmHttpServer {
  private static final Logger            LOGGER         = LoggerFactory.getLogger(TmmHttpServer.class);
  private static final String            CONTEXT_PREFIX = "/api/";

  private static TmmHttpServer           instance;

  private final ObjectWriter             objectWriter;
  private final Map<String, HttpHandler> contextMap;

  private static boolean                 initialized    = false;

  private HttpServer                     httpServer;
  private boolean                        running        = false;
  private String                         apiKey         = "";
  private volatile boolean               shuttingDown   = false;

  /**
   * the action which terminates the JVM after a shutdown via the HTTP API has been finished
   */
  private Runnable                       exitTerminator = TmmHttpServer::terminateApplication;

  private TmmHttpServer() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setTimeZone(TimeZone.getDefault());
    objectWriter = objectMapper.writerFor(Response.class);

    contextMap = new LinkedHashMap<>();

    httpServer = HttpServer.create();

    // default contexts
    createContext("system", new SystemCommandHandler(), false);
    createContext("docs", new ApiDocsHandler());

    // the bundled web UI (static files only - all data is fetched via the API key protected API)
    createWebContext("/", new WebAppHandler());

    updateConfiguration(Settings.getInstance().isEnableHttpServer(), Settings.getInstance().getHttpServerPort(),
        Settings.getInstance().getHttpApiKey());
  }

  public static void init() {
    initialized = true;
  }

  private void start() {
    httpServer.start();
    running = true;
  }

  private void stop() {
    httpServer.stop(1);
    running = false;
  }

  private void bindPort(int port) throws IOException {
    httpServer.bind(new InetSocketAddress(port), 0);
  }

  /**
   * Gets the singleton instance of the HTTP server.
   *
   * @return the {@link TmmHttpServer}
   * @throws Exception
   *           thrown if the server could not be created
   */
  public static synchronized TmmHttpServer getInstance() throws Exception {
    if (instance == null) {
      instance = new TmmHttpServer();
    }

    return instance;
  }

  public void createContext(String contextPath, ITmmCommandHandler commandHandler) {
    createContext(contextPath, commandHandler, true);
  }

  /**
   * Creates a context which delegates the request to the given command handler.
   *
   * @param contextPath
   *          the context path (without the {@code /api/} prefix)
   * @param commandHandler
   *          the handler to delegate to
   * @param rejectWhenShuttingDown
   *          whether requests should be rejected with 503 as soon a shutdown has been requested
   */
  public void createContext(String contextPath, ITmmCommandHandler commandHandler, boolean rejectWhenShuttingDown) {
    HttpHandler httpHandler = httpExchange -> {
      if (rejectWhenShuttingDown && shuttingDown) {
        sendResponse(503, "tinyMediaManager is shutting down - no more commands are accepted", null, httpExchange);
        return;
      }

      int responseCode;
      String responseMessage;
      Object payload = null;

      // delegate the request to the handler
      try {
        TmmCommandResponse commandResponse = commandHandler.post(httpExchange);
        responseCode = commandResponse.getResponseCode();
        responseMessage = commandResponse.getResponseMessage();
        payload = commandResponse.getPayload();
      }
      catch (Exception e) {
        LOGGER.error("Could not process command '{}' - '{}'", commandHandler.getClass().getName(), e.getMessage());
        responseCode = 500;
        responseMessage = e.getMessage();
      }

      sendResponse(responseCode, responseMessage, payload, httpExchange);
    };

    createContext(contextPath, httpHandler);
  }

  public void createContext(String contextPath, HttpHandler handler) {
    HttpHandler httpHandler = httpExchange -> {
      if (StringUtils.isNotBlank(this.apiKey)) {
        // API key check
        List<String> apiKeyFromRequest = httpExchange.getRequestHeaders().get("api-key");
        if (apiKeyFromRequest == null || !apiKeyFromRequest.contains(this.apiKey)) {
          sendResponse(403, "Invalid API key", null, httpExchange);
          return;
        }
      }

      handler.handle(httpExchange);
    };

    httpServer.createContext(CONTEXT_PREFIX + contextPath, httpHandler);
    contextMap.put(CONTEXT_PREFIX + contextPath, httpHandler);
  }

  /**
   * Creates a context outside of the {@code /api/} prefix which is <b>not</b> protected by the API key. This must only be used for static, public
   * content like the files of the web UI - never for anything exposing or changing data.
   *
   * @param path
   *          the full context path (e.g. {@code /})
   * @param handler
   *          the handler to delegate to
   */
  public void createWebContext(String path, HttpHandler handler) {
    httpServer.createContext(path, handler);
    contextMap.put(path, handler);
  }

  private void sendResponse(int responseCode, String responseMessage, Object payload, HttpExchange httpExchange) {
    Response response = new Response();
    response.message = responseMessage;
    response.data = payload;

    try (OutputStream out = httpExchange.getResponseBody()) {
      String body = objectWriter.writeValueAsString(response);
      httpExchange.getResponseHeaders().add("Content-Type", "application/json");
      httpExchange.sendResponseHeaders(responseCode, body.length());
      out.write(body.getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException ex) {
      LOGGER.error("Could not send response - '{}'", ex.getMessage());
    }
  }

  /**
   * Initiates the shutdown of tinyMediaManager: no further commands will be accepted, and the JVM will be terminated as soon as all queued and
   * running tasks have finished.
   *
   * @param force
   *          if true, running tasks will not be awaited
   * @return true if the shutdown has been initiated by this call, false if a shutdown was already in progress
   */
  public synchronized boolean requestShutdown(boolean force) {
    if (shuttingDown) {
      return false;
    }

    shuttingDown = true;
    LOGGER.info("HTTP API: shutdown requested (force: {}) - no more commands will be accepted", force);

    Thread shutdownThread = new Thread(() -> initiateShutdown(force), "http-api-shutdown");
    shutdownThread.start();

    return true;
  }

  /**
   * @return true if a shutdown has been requested via the HTTP API
   */
  public boolean isShuttingDown() {
    return shuttingDown;
  }

  /**
   * Waits (if not forced) until all main tasks and worker pools are idle and then runs the {@link #exitTerminator}.
   *
   * @param force
   *          if true, no running tasks will be awaited
   */
  private void initiateShutdown(boolean force) {
    // give the HTTP client some time to read the response
    sleep(1000);
    if (Thread.currentThread().isInterrupted()) {
      return;
    }

    if (!force) {
      TmmTaskManager taskManager = TmmTaskManager.getInstance();
      while (taskManager.isMainTaskRunning() || taskManager.isPoolRunning()) {
        sleep(2000);
        if (Thread.currentThread().isInterrupted()) {
          return;
        }
      }
    }

    LOGGER.info("HTTP API: all tasks finished - exiting tinyMediaManager");
    exitTerminator.run();
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Replaces the action which terminates the JVM after all tasks have finished - only used by tests to avoid killing the test runner.
   *
   * @param exitTerminator
   *          the action to run instead of {@link #terminateApplication()}
   */
  void setExitTerminator(Runnable exitTerminator) {
    this.exitTerminator = exitTerminator;
  }

  /**
   * Clears the shutdown flag again - only used by tests to restore a reusable server instance.
   */
  void clearShutdownRequest() {
    shuttingDown = false;
  }

  /**
   * Makes the clean shutdown of tinyMediaManager and terminates the JVM - the default {@link #exitTerminator}. The registered shutdown hook might
   * still trigger a second cleanup afterwards, which is the same behavior as exiting the application over the GUI.
   */
  static void terminateApplication() {
    TinyMediaManager.shutdown();
    TinyMediaManager.shutdownLogger();
    System.exit(0);
  }

  /**
   * Stops the HTTP server instance if it exists and is running - called from the application shutdown.
   */
  public static synchronized void stopServer() {
    if (instance != null && instance.running) {
      instance.stop();
    }
  }

  public void updateConfiguration(boolean enabled, int port, String apiKey) throws IOException {
    if (!initialized) {
      // not yet initialized - nothing to do
      return;
    }

    if (!enabled && !running) {
      // not enabled and not running -> nothing to do
      return;
    }
    else if (!enabled && running) {
      // we want to shut down the running server
      stop();
      createNewServer();
      return;
    }

    if (httpServer.getAddress() == null || port != httpServer.getAddress().getPort()) {
      // change of the port - need to create a new server for that
      stop();
      createNewServer();
      bindPort(port);
      start();
    }

    this.apiKey = apiKey;
  }

  private void createNewServer() throws IOException {
    // we cannot re-use the http server once stopped
    httpServer = HttpServer.create();
    for (Map.Entry<String, HttpHandler> entry : contextMap.entrySet()) {
      httpServer.createContext(entry.getKey(), entry.getValue());
    }
  }

  /**
   * the JSON response wrapper sent to the API caller
   */
  public static class Response {
    /**
     * the human-readable result message
     */
    public String message;

    /**
     * the optional structured data of the response (e.g. the status information) - only included if present
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Object data;
  }
}
