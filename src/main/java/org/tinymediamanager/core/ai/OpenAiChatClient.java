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
package org.tinymediamanager.core.ai;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ai.AiException.Authentication;
import org.tinymediamanager.core.ai.AiException.Network;
import org.tinymediamanager.core.ai.AiException.RateLimit;
import org.tinymediamanager.core.ai.AiException.Timeout;
import org.tinymediamanager.scraper.http.TmmHttpClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * The class {@link OpenAiChatClient} is an implementation of {@link AiClient} for any OpenAI-compatible chat completions API.
 *
 * @author Manuel Laggner
 */
public class OpenAiChatClient implements AiClient {
  private static final Logger    LOGGER          = LoggerFactory.getLogger(OpenAiChatClient.class);

  private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

  private final ObjectMapper     objectMapper;
  private final OkHttpClient     httpClient;

  private final String           name;
  private final String           apiUrl;
  private final String           apiKey;
  private final String           model;
  private final int              maxTokens;
  private final double           temperature;

  /**
   * Creates a new OpenAI-compatible chat client.
   *
   * @param name
   *          the provider name
   * @param apiUrl
   *          the full API URL (e.g. https://api.openai.com/v1/chat/completions)
   * @param apiKey
   *          the API key
   * @param model
   *          the model name (e.g. gpt-4o-mini)
   * @param maxTokens
   *          the maximum tokens for responses
   * @param temperature
   *          the temperature (0.0 - 2.0)
   */
  public OpenAiChatClient(String name, String apiUrl, String apiKey, String model, int maxTokens, double temperature) {
    this(name, apiUrl, apiKey, model, maxTokens, temperature,
        TmmHttpClient.newBuilder().connectTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build());
  }

  // package-private for testing - allows injecting a simpler HTTP client without the 429 retry interceptor
  OpenAiChatClient(String name, String apiUrl, String apiKey, String model, int maxTokens, double temperature, OkHttpClient httpClient) {
    this.name = name;
    this.apiUrl = apiUrl;
    this.apiKey = apiKey;
    this.model = model;
    this.maxTokens = maxTokens;
    this.temperature = temperature;

    this.objectMapper = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();

    this.httpClient = httpClient;
  }

  @Override
  public AiResponse chat(AiRequest request) throws AiException {
    return doChat(request, Duration.ofSeconds(60));
  }

  @Override
  public AiResponse chat(AiRequest request, Duration timeout) throws AiException {
    return doChat(request, timeout);
  }

  private AiResponse doChat(AiRequest request, Duration timeout) throws AiException {
    if (StringUtils.isBlank(apiKey)) {
      throw new Authentication("API key is not configured");
    }

    // apply default model/params if not set on the request
    if (request.getModel() == null) {
      request.setModel(model);
    }
    if (request.getMaxTokens() <= 0) {
      request.setMaxTokens(maxTokens);
    }
    if (request.getTemperature() <= 0) {
      request.setTemperature(temperature);
    }

    String jsonBody;
    try {
      jsonBody = objectMapper.writeValueAsString(request);
    }
    catch (Exception e) {
      throw new AiException("Failed to serialize request", e);
    }

    LOGGER.debug("Sending AI request to {} with model {}", name, request.getModel());

    OkHttpClient clientForCall = httpClient;
    if (timeout.getSeconds() != 60) {
      clientForCall = httpClient.newBuilder()
          .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
          .writeTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
          .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
          .build();
    }

    RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
    Request httpRequest = new Request.Builder().url(apiUrl)
        .addHeader("Authorization", "Bearer " + apiKey)
        .addHeader("Content-Type", "application/json")
        .post(body)
        .build();

    try (Response response = clientForCall.newCall(httpRequest).execute()) {
      int statusCode = response.code();
      String responseBody = response.body() != null ? response.body().string() : "";

      LOGGER.debug("AI response status: {} from {}", statusCode, name);

      if (statusCode == 401 || statusCode == 403) {
        throw new Authentication("Authentication failed (HTTP " + statusCode + "): " + extractErrorMessage(responseBody));
      }
      if (statusCode == 429) {
        throw new RateLimit("Rate limit exceeded (HTTP 429)");
      }
      if (statusCode >= 400) {
        throw new AiException.Api(statusCode, "API error (HTTP " + statusCode + "): " + extractErrorMessage(responseBody));
      }

      AiResponse aiResponse;
      try {
        aiResponse = objectMapper.readValue(responseBody, AiResponse.class);
      }
      catch (Exception e) {
        throw new AiException("Failed to parse API response", e);
      }

      if (aiResponse.hasError()) {
        String errorMsg = aiResponse.getError().getMessage();
        throw new AiException.Api(statusCode, "API returned error: " + errorMsg);
      }

      return aiResponse;
    }
    catch (AiException e) {
      throw e;
    }
    catch (java.net.SocketTimeoutException e) {
      throw new Timeout("Request timed out: " + e.getMessage(), e);
    }
    catch (IOException e) {
      throw new Network("Network error: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean testConnection() throws AiException {
    AiRequest request = new AiRequest(model, "You are a helpful assistant.", "Reply with only the word OK.");
    AiResponse response = doChat(request, Duration.ofSeconds(30));
    return response.getContent() != null;
  }

  @Override
  public String getProviderName() {
    return name;
  }

  private String extractErrorMessage(String responseBody) {
    if (StringUtils.isBlank(responseBody)) {
      return "unknown error";
    }
    try {
      AiResponse errorResponse = objectMapper.readValue(responseBody, AiResponse.class);
      if (errorResponse.hasError()) {
        return errorResponse.getError().getMessage();
      }
    }
    catch (Exception ignored) {
    }
    // extract first 200 chars as a fallback
    return responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody;
  }

  @Override
  public String toString() {
    return "OpenAiChatClient{" + "name='" + name + '\'' + ", model='" + model + '\'' + '}';
  }
}
