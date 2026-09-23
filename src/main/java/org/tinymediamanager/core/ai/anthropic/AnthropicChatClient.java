package org.tinymediamanager.core.ai.anthropic;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ai.AiClient;
import org.tinymediamanager.core.ai.AiException;
import org.tinymediamanager.core.ai.AiRequest;
import org.tinymediamanager.core.ai.AiResponse;
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
 * The class {@link AnthropicChatClient} is an implementation of {@link AiClient} for the Anthropic (Claude) chat completions API.
 *
 * API docs: https://platform.claude.com/docs/en/api/messages
 *
 * @author Manuel Laggner
 */
public class AnthropicChatClient implements AiClient {
  private static final Logger    LOGGER          = LoggerFactory.getLogger(AnthropicChatClient.class);
  private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

  private final ObjectMapper     objectMapper;
  private final OkHttpClient     httpClient;

  private final String           name;
  private final String           apiUrl;
  private final String           baseUrl;
  private final String           apiKey;
  private final String           model;
  private final int              maxTokens;
  private final double           temperature;

  /**
   * Creates a new Anthropic chat client.
   *
   * @param name
   *          the provider name
   * @param apiUrl
   *          the full API URL (e.g. https://api.anthropic.com/v1/complete)
   * @param apiKey
   *          the API key
   * @param model
   *          the model name
   * @param maxTokens
   *          the maximum tokens for responses
   * @param temperature
   *          the temperature (0.0 - 2.0)
   */
  public AnthropicChatClient(String name, String apiUrl, String apiKey, String model, int maxTokens, double temperature) {
    this(name, apiUrl, apiKey, model, maxTokens, temperature,
        TmmHttpClient.newBuilder().connectTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build());
  }

  /**
   * Creates a new Anthropic chat client with base URL and chat endpoint.
   *
   * @param name
   *          the provider name
   * @param baseUrl
   *          the base API URL (e.g. https://api.anthropic.com/v1)
   * @param chatEndpoint
   *          the chat endpoint (e.g. /messages)
   * @param apiKey
   *          the API key
   * @param model
   *          the model name
   * @param maxTokens
   *          the maximum tokens for responses
   * @param temperature
   *          the temperature (0.0 - 2.0)
   */
  public AnthropicChatClient(String name, String baseUrl, String chatEndpoint, String apiKey, String model, int maxTokens, double temperature) {
    this(name, baseUrl, chatEndpoint, apiKey, model, maxTokens, temperature,
        TmmHttpClient.newBuilder().connectTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build());
  }

  // package-private for testing
  AnthropicChatClient(String name, String apiUrl, String apiKey, String model, int maxTokens, double temperature, OkHttpClient httpClient) {
    this.name = name;
    this.apiUrl = apiUrl;
    this.baseUrl = extractBaseUrl(apiUrl);
    this.apiKey = apiKey;
    this.model = model;
    this.maxTokens = maxTokens;
    this.temperature = temperature;

    this.objectMapper = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();

    this.httpClient = httpClient;
  }

  // package-private for testing with base URL + endpoint
  AnthropicChatClient(String name, String baseUrl, String chatEndpoint, String apiKey, String model, int maxTokens, double temperature,
      OkHttpClient httpClient) {
    this.name = name;
    this.apiUrl = buildFullUrl(baseUrl, chatEndpoint);
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.model = model;
    this.maxTokens = maxTokens;
    this.temperature = temperature;

    this.objectMapper = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();

    this.httpClient = httpClient;
  }

  private static String extractBaseUrl(String apiUrl) {
    if (apiUrl == null || apiUrl.isEmpty()) {
      return apiUrl;
    }
    int lastPathIndex = apiUrl.lastIndexOf('/');
    if (lastPathIndex > 0) {
      return apiUrl.substring(0, lastPathIndex);
    }
    return apiUrl;
  }

  private static String buildFullUrl(String baseUrl, String endpoint) {
    if (baseUrl == null || baseUrl.isEmpty()) {
      return endpoint;
    }
    if (endpoint == null || endpoint.isEmpty()) {
      return baseUrl;
    }
    String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    String end = endpoint.startsWith("/") ? endpoint.substring(1) : endpoint;
    return base + "/" + end;
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
      throw new AiException.Authentication("API key is not configured");
    }

    if (request.getMaxTokens() <= 0) {
      request.setMaxTokens(maxTokens);
    }
    if (request.getTemperature() <= 0) {
      request.setTemperature(temperature);
    }

    // Convert AiMessage list to AnthropicMessage list
    List<AnthropicMessage> anthropicMessages = AnthropicMessageConverter.toAnthropicMessages(request.getMessages());

    AnthropicRequest anthropicRequest = new AnthropicRequest(request.getModel(), anthropicMessages, request.getMaxTokens(), request.getTemperature());

    String jsonBody;
    try {
      jsonBody = objectMapper.writeValueAsString(anthropicRequest);
    }
    catch (Exception e) {
      throw new AiException("Failed to serialize request", e);
    }

    LOGGER.debug("Sending AI request to {} with model {}", name, request.getModel());

    OkHttpClient clientForCall = httpClient;
    if (timeout.toMillis() != 60000) {
      clientForCall = httpClient.newBuilder()
          .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
          .writeTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
          .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
          .build();
    }

    RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
    Request httpRequest = new Request.Builder().url(apiUrl)
        .addHeader("x-api-key", apiKey)
        .addHeader("Content-Type", "application/json")
        .post(body)
        .build();

    try (Response response = clientForCall.newCall(httpRequest).execute()) {
      int statusCode = response.code();
      String responseBody = response.body() != null ? response.body().string() : "";

      LOGGER.debug("AI response status: {} from {}", statusCode, name);

      if (statusCode == 401 || statusCode == 403) {
        throw new AiException.Authentication("Authentication failed (HTTP " + statusCode + "): " + extractErrorMessage(responseBody));
      }
      if (statusCode == 429) {
        throw new AiException.RateLimit("Rate limit exceeded (HTTP 429)");
      }
      if (statusCode >= 400) {
        throw new AiException.Api(statusCode, "API error (HTTP " + statusCode + "): " + extractErrorMessage(responseBody));
      }

      AnthropicResponse anthropicResponse;
      try {
        anthropicResponse = objectMapper.readValue(responseBody, AnthropicResponse.class);
      }
      catch (Exception e) {
        throw new AiException("Failed to parse API response", e);
      }

      if (anthropicResponse.hasError()) {
        throw new AiException.Api(statusCode, "API returned error: " + anthropicResponse.getError().getMessage());
      }

      // Build AiResponse from AnthropicResponse
      AiResponse aiResponse = new AiResponse();
      aiResponse.setId(anthropicResponse.getId());
      aiResponse.setModel(anthropicResponse.getModel());
      aiResponse.setObject(anthropicResponse.getType());
      aiResponse.setCreated(0);

      if (anthropicResponse.getUsage() != null) {
        AiResponse.Usage usage = new AiResponse.Usage();
        usage.setPromptTokens(anthropicResponse.getUsage().getInputTokens());
        usage.setCompletionTokens(anthropicResponse.getUsage().getOutputTokens());
        usage.setTotalTokens(anthropicResponse.getUsage().getInputTokens() + anthropicResponse.getUsage().getOutputTokens());
        aiResponse.setUsage(usage);
      }

      if (anthropicResponse.getTextContent() != null) {
        AiResponse.Message message = new AiResponse.Message();
        message.setRole("assistant");
        message.setContent(anthropicResponse.getTextContent());
        AiResponse.Choice choice = new AiResponse.Choice();
        choice.setMessage(message);
        choice.setFinishReason(anthropicResponse.getStopReason());
        aiResponse.setChoices(Collections.singletonList(choice));
      }

      return aiResponse;
    }
    catch (AiException e) {
      throw e;
    }
    catch (java.net.SocketTimeoutException e) {
      throw new AiException.Timeout("Request timed out: " + e.getMessage(), e);
    }
    catch (IOException e) {
      throw new AiException.Network("Network error: " + e.getMessage(), e);
    }
  }

  private String extractErrorMessage(String responseBody) {
    if (StringUtils.isBlank(responseBody)) {
      return "unknown error";
    }
    try {
      AnthropicResponse errorResponse = objectMapper.readValue(responseBody, AnthropicResponse.class);
      if (errorResponse.hasError()) {
        return errorResponse.getError().getMessage();
      }
    }
    catch (Exception ignored) {
    }
    return responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody;
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

  @Override
  public String toString() {
    return "AnthropicChatClient{" + "name='" + name + '\'' + ", model='" + model + '\'' + '}';
  }

  @Override
  public List<String> listModels() throws AiException {
    String modelsUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + "/models" : baseUrl + "/models";

    Request httpRequest = new Request.Builder().url(modelsUrl)
        .addHeader("x-api-key", apiKey)
        .addHeader("Content-Type", "application/json")
        .get()
        .build();

    try (Response response = httpClient.newCall(httpRequest).execute()) {
      int statusCode = response.code();
      String responseBody = response.body() != null ? response.body().string() : "";

      if (statusCode == 401 || statusCode == 403) {
        throw new AiException.Authentication("Authentication failed (HTTP " + statusCode + "): " + extractErrorMessage(responseBody));
      }
      if (statusCode >= 400) {
        throw new AiException.Api(statusCode, "Failed to fetch models (HTTP " + statusCode + "): " + extractErrorMessage(responseBody));
      }

      AnthropicModelsResponse modelsResponse;
      try {
        modelsResponse = objectMapper.readValue(responseBody, AnthropicModelsResponse.class);
      }
      catch (Exception e) {
        throw new AiException("Failed to parse models response", e);
      }

      if (modelsResponse.getModels() != null) {
        return modelsResponse.getModels().stream().map(m -> m.getId()).collect(Collectors.toList());
      }

      return new ArrayList<>();
    }
    catch (AiException e) {
      throw e;
    }
    catch (IOException e) {
      throw new AiException.Network("Network error: " + e.getMessage(), e);
    }
  }
}
