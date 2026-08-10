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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.Settings;

import org.tinymediamanager.scraper.http.TmmHttpClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Tests for {@link AiService} covering prompt substitution, tagline generation, and outline generation.
 */
public class AiServiceTest {

  private MockWebServer server;
  private AiService     service;

  private String        savedAiEndpointUrl;
  private String        savedAiApiKey;
  private boolean       savedAiRateLimitEnabled;

  @Before
  public void setup() throws Exception {
    ReleaseInfo.init();

    Settings settings = Settings.getInstance();
    savedAiEndpointUrl = settings.getAiEndpointUrl();
    savedAiApiKey = settings.getAiApiKey();
    savedAiRateLimitEnabled = settings.isAiRateLimitEnabled();

    AiRateLimiter.getInstance().reset();
    AiCache.getInstance().clear();
    AiManager.getInstance().clearClientCache();
    TmmHttpClient.recreateHttpClient();

    server = new MockWebServer();
    server.start();

    settings.setAiEndpointUrl(server.url("/v1/chat/completions").toString());
    settings.setAiApiKey("sk-test-key");
    settings.setAiRateLimitEnabled(false);

    AiManager.getInstance().clearClientCache();

    service = new AiService();
  }

  @After
  public void tearDown() throws Exception {
    Settings settings = Settings.getInstance();
    settings.setAiEndpointUrl(savedAiEndpointUrl);
    settings.setAiApiKey(savedAiApiKey);
    settings.setAiRateLimitEnabled(savedAiRateLimitEnabled);
    AiManager.getInstance().clearClientCache();

    if (server != null) {
      server.shutdown();
    }
  }

  // =======================================================================
  // Prompt template content
  // =======================================================================

  @Test
  public void testTranslateSystemPromptContainsTargetLanguage() {
    assertThat(AiService.PROMPT_TRANSLATE_SYSTEM).contains("{targetLanguage}");
  }

  @Test
  public void testTranslateUserPromptContainsVariables() {
    assertThat(AiService.PROMPT_TRANSLATE_USER).contains("{targetLanguage}").contains("{plot}");
  }

  @Test
  public void testTaglineSystemPromptContainsVariables() {
    assertThat(AiService.PROMPT_TAGLINE_SYSTEM).contains("{targetLanguage}").contains("{maxWords}");
  }

  @Test
  public void testTaglineUserPromptContainsVariables() {
    assertThat(AiService.PROMPT_TAGLINE_USER).contains("{targetLanguage}").contains("{title}").contains("{plot}").contains("{year}");
  }

  @Test
  public void testOutlineSystemPromptContainsVariables() {
    assertThat(AiService.PROMPT_OUTLINE_SYSTEM).contains("{targetLanguage}").contains("{maxWords}");
  }

  @Test
  public void testOutlineUserPromptContainsVariables() {
    assertThat(AiService.PROMPT_OUTLINE_USER).contains("{targetLanguage}").contains("{title}").contains("{plot}").contains("{year}");
  }

  // =======================================================================
  // Tagline generation
  // =======================================================================

  @Test
  public void testBlankTitleAndPlotReturnsEmpty() {
    String result = service.generateTagline("", "", 0, "German", 10);
    assertThat(result).isEqualTo("");
  }

  @Test
  public void testSuccessfulTaglineGeneration() {
    server.enqueue(new MockResponse().setResponseCode(200)
        .setBody("{\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"Just do it.\"},\"finish_reason\":\"stop\"}]}"));

    String result = service.generateTagline("Test Movie", "A movie about testing.", 2024, "English", 5);
    assertThat(result).isEqualTo("Just do it.");
  }

  @Test
  public void testTaglineApiErrorReturnsEmpty() {
    server.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":{\"message\":\"Bad request\"}}"));

    String result = service.generateTagline("Test", "A plot.", 2024, "English", 5);
    assertThat(result).isEqualTo("");
  }

  @Test
  public void testTaglineServerErrorReturnsEmpty() {
    for (int i = 0; i < 4; i++) {
      server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
    }

    String result = service.generateTagline("Test", "A plot.", 2024, "English", 5);
    assertThat(result).isEqualTo("");
  }

  // =======================================================================
  // Outline generation
  // =======================================================================

  @Test
  public void testBlankTitleAndPlotReturnsEmptyOutline() {
    String result = service.generateOutline("", "", 0, "German", 50);
    assertThat(result).isEqualTo("");
  }

  @Test
  public void testSuccessfulOutlineGeneration() {
    server.enqueue(new MockResponse().setResponseCode(200)
        .setBody(
            "{\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"A hero saves the day.\"},\"finish_reason\":\"stop\"}]}"));

    String result = service.generateOutline("Test Movie", "A movie about testing.", 2024, "English", 50);
    assertThat(result).isEqualTo("A hero saves the day.");
  }

  @Test
  public void testOutlineApiErrorReturnsEmpty() {
    server.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":{\"message\":\"Bad request\"}}"));

    String result = service.generateOutline("Test", "A plot.", 2024, "English", 50);
    assertThat(result).isEqualTo("");
  }

  @Test
  public void testOutlineServerErrorReturnsEmpty() {
    for (int i = 0; i < 4; i++) {
      server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
    }

    String result = service.generateOutline("Test", "A plot.", 2024, "English", 50);
    assertThat(result).isEqualTo("");
  }
}
