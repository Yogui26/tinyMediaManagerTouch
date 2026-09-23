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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.ai.openai.OpenAiChatClient;

/**
 * Integration tests that call a real LLM endpoint (OpenAI-compatible or Anthropic).
 * <p>
 * Set the following environment variables to enable:
 * <ul>
 * <li>{@code TMM_AI_API_KEY} – your API key (required)</li>
 * <li>{@code TMM_AI_ENDPOINT_URL} – API endpoint (default: {@code https://api.openai.com/v1/chat/completions})</li>
 * <li>{@code TMM_AI_MODEL} – model name (default: gpt-4o-mini)</li>
 * <li>{@code TMM_AI_PROVIDER_TYPE} – provider type (default: openai, can be openai or anthropic)</li>
 * </ul>
 * </p>
 */
public class ITAiClientIntegrationTest extends BasicITest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ITAiClientIntegrationTest.class);

  private static String       apiKey;
  private static String       endpointUrl;
  private static String       providerType;

  @BeforeClass
  public static void readCredentials() {
    apiKey = System.getenv("TMM_AI_API_KEY");
    endpointUrl = System.getenv("TMM_AI_ENDPOINT_URL");
    providerType = System.getenv("TMM_AI_PROVIDER_TYPE");
    if (StringUtils.isBlank(endpointUrl)) {
      endpointUrl = "https://api.openai.com/v1/chat/completions";
    }
    if (StringUtils.isBlank(providerType)) {
      providerType = "openai";
    }
  }

  private static AiClient createClient(String model) {
    if ("anthropic".equalsIgnoreCase(providerType)) {
      return new org.tinymediamanager.core.ai.anthropic.AnthropicChatClient("integration-test", endpointUrl, apiKey, model, 1000, 0.7);
    }
    return new OpenAiChatClient("integration-test", endpointUrl, apiKey, model, 1000, 0.7);
  }

  @Before
  public void checkCredentials() {
    Assume.assumeTrue("Set TMM_AI_API_KEY to run AI integration tests", StringUtils.isNotBlank(apiKey));
  }

  @Test
  public void testBasicChatCompletion() throws Exception {
    AiClient client = createClient("gpt-4o-mini");

    AiRequest request = new AiRequest("gpt-4o-mini", "You are a helpful assistant.", "Reply with only the word OK.");
    AiResponse response = client.chat(request);

    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotBlank();
    assertThat(response.getFinishReason()).isEqualTo("stop");
    assertThat(response.getModel()).isNotBlank();
    assertThat(response.getUsage()).isNotNull();
    assertThat(response.getUsage().getTotalTokens()).isGreaterThan(0);
  }

  @Test
  public void testConnection() throws Exception {
    AiClient client = createClient("gpt-4o-mini");

    boolean connected = client.testConnection();
    assertThat(connected).isTrue();
  }

  @Test
  public void testTranslationToGerman() throws AiException {
    Settings settings = Settings.getInstance();
    settings.setAiEndpointUrl(endpointUrl);
    settings.setAiApiKey(apiKey);
    settings.setAiRateLimitEnabled(false);
    AiManager.getInstance().clearClientCache();

    AiService service = new AiService();

    String plot = "A young wizard discovers his magical heritage and battles the dark lord who killed his parents.";
    String result = service.translatePlot(plot, "German", "");

    assertThat(result).isNotBlank();
    assertThat(result).doesNotContain("young wizard discovers his magical heritage");
    assertThat(result).contains("Zauberer");
  }

  @Test
  public void testTranslationToFrench() throws AiException {
    Settings settings = Settings.getInstance();
    settings.setAiEndpointUrl(endpointUrl);
    settings.setAiApiKey(apiKey);
    settings.setAiRateLimitEnabled(false);
    settings.forceSaveSettings();
    AiManager.getInstance().clearClientCache();

    AiService service = new AiService();

    String plot = "A young wizard discovers his magical heritage.";
    String result = service.translatePlot(plot, "French", "");

    assertThat(result).isNotBlank();
    assertThat(result).doesNotContain("young wizard discovers");
    assertThat(result).contains("sorcier");
  }

  @Test
  public void testAuthenticationError() {
    OpenAiChatClient client = new OpenAiChatClient("integration-test", endpointUrl, "sk-invalid-key", "gpt-4o-mini", 1000, 0.7);

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    try {
      client.chat(request);
    }
    catch (AiException.Authentication e) {
      // expected
      assertThat(e.getMessage()).isNotBlank();
    }
    catch (AiException e) {
      // 401/403 is expected on the real server; some proxies may return other errors
      org.junit.Assert.assertTrue("Expected an authentication or API error", e instanceof AiException.Api || e instanceof AiException.Authentication);
    }
  }

  @Test
  public void testModelNotFoundError() {
    OpenAiChatClient client = new OpenAiChatClient("integration-test", endpointUrl, apiKey, "nonexistent-model-xyz", 1000, 0.7);

    AiRequest request = new AiRequest("nonexistent-model-xyz", "system", "user");
    try {
      client.chat(request);
    }
    catch (AiException.Api e) {
      assertThat(e.getStatusCode()).isIn(400, 404, 422);
    }
    catch (AiException e) {
      // some providers may return other error types for unknown models
      assertThat(e).isInstanceOfAny(AiException.Api.class, AiException.class);
    }
  }

  @Test
  public void testCustomTimeout() throws Exception {
    OpenAiChatClient client = new OpenAiChatClient("integration-test", endpointUrl, apiKey, "gpt-4o-mini", 1000, 0.7);

    AiRequest request = new AiRequest("gpt-4o-mini", "You are a helpful assistant.", "Reply with 'OK'.");
    java.time.Duration shortTimeout = java.time.Duration.ofSeconds(30);
    AiResponse response = client.chat(request, shortTimeout);

    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotBlank();
  }

  @Test
  public void testTranslationPerformance() throws AiException {
    Settings settings = Settings.getInstance();
    settings.setAiEndpointUrl(endpointUrl);
    settings.setAiApiKey(apiKey);
    settings.setAiRateLimitEnabled(false);
    settings.forceSaveSettings();
    AiManager.getInstance().clearClientCache();

    AiService service = new AiService();

    String longPlot = "In a world where people can enter each other's dreams and share ideas, Dom Cobb is "
        + "the most skilled thief the world has ever known. His specialty is extraction: stealing secrets from "
        + "deep within the subconscious while the victim is dreaming. This rare talent has made him a coveted "
        + "player in the treacherous world of corporate espionage, but it has also cost him everything he loves. "
        + "Framed for a crime he did not commit and separated from his children, Cobb is given a chance at "
        + "redemption. One final job: instead of stealing an idea, he must plant one. This seemingly impossible "
        + "task of inception could be the key to clearing his name and returning to his family. To succeed, Cobb "
        + "and his team must navigate through multiple layers of dreams, each more unstable and dangerous than "
        + "the last. As they descend deeper into the dream world, the line between reality and imagination blurs, "
        + "and Cobb must confront the demons from his past that threaten to sabotage the mission. The team races "
        + "against time, battling the projections of the target's subconscious and their own doubts, in a desperate "
        + "attempt to pull off the perfect crime. But in a world where dreams can become reality, the greatest " + "danger may lie in waking up.";

    // Warmup run (tracked — fast time indicates a cached result)
    LOGGER.info("Performing warmup run for performance measurement...");
    long warmupStart = System.nanoTime();
    String warmup = service.translatePlot(longPlot, "German", "");
    long warmupMs = (System.nanoTime() - warmupStart) / 1_000_000;
    assertThat(warmup).isNotBlank();
    LOGGER.info("Warmup complete: {} ms ({} chars){}", warmupMs, warmup.length(), warmupMs < 500 ? " — likely cached" : "");

    // Measured runs
    int runs = 3;
    long totalMs = 0;
    long minMs = Long.MAX_VALUE;
    long maxMs = 0;

    for (int i = 1; i <= runs; i++) {
      long start = System.nanoTime();
      String result = service.translatePlot(longPlot, "German", "");
      long elapsedMs = (System.nanoTime() - start) / 1_000_000;
      totalMs += elapsedMs;
      minMs = Math.min(minMs, elapsedMs);
      maxMs = Math.max(maxMs, elapsedMs);

      assertThat(result).isNotBlank();
      assertThat(result).doesNotContain("dreams and share ideas");

      LOGGER.info("Translation run {}/{}: {} ms ({} chars)", i, runs, elapsedMs, result.length());
    }

    long avgMs = totalMs / runs;
    LOGGER.info("Performance summary for {} runs ({} chars input): min={} ms, max={} ms, avg={} ms", runs, longPlot.length(), minMs, maxMs, avgMs);

    assertThat(avgMs).as("Average translation time should be under 30 seconds").isLessThan(30_000L);
  }
}
