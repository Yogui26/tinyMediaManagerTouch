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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.Settings;

import org.tinymediamanager.scraper.http.TmmHttpClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Integration tests for {@link AiService#translatePlot(String, String)} with a mocked HTTP endpoint.
 */
public class PlotTranslationServiceTest {

  private MockWebServer server;
  private AiService     service;

  private String        savedAiEndpointUrl;
  private String        savedAiApiKey;
  private boolean       savedAiRateLimitEnabled;

  @Before
  public void setup() throws Exception {
    ReleaseInfo.init();

    // save current settings to restore in teardown
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
    // restore saved settings
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
  // Configuration guards
  // =======================================================================

  @Test
  public void testBlankPlotReturnsOriginal() throws AiException {
    String result = service.translatePlot("", "German", "");
    assertThat(result).isEqualTo("");
  }

  @Test
  public void testNullLanguageReturnsOriginal() throws AiException {
    String plot = "Some plot";
    String result = service.translatePlot(plot, null, "");
    assertThat(result).isEqualTo(plot);
  }

  // =======================================================================
  // Successful translation
  // =======================================================================

  @Test
  public void testSuccessfulTranslation() throws AiException {
    server.enqueue(new MockResponse().setResponseCode(200)
        .setBody("{\n" + "  \"id\": \"chatcmpl-abc\",\n" + "  \"object\": \"chat.completion\",\n" + "  \"created\": 1677652288,\n"
            + "  \"model\": \"gpt-4o-mini\",\n" + "  \"choices\": [\n" + "    {\n" + "      \"index\": 0,\n" + "      \"message\": {\n"
            + "        \"role\": \"assistant\",\n" + "        \"content\": \"Dies ist ein übersetzter Handlungstext.\"\n" + "      },\n"
            + "      \"finish_reason\": \"stop\"\n" + "    }\n" + "  ]\n" + "}"));

    String result = service.translatePlot("This is a plot text.", "German", "");
    assertThat(result).isEqualTo("Dies ist ein übersetzter Handlungstext.");
  }

  @Test
  public void testTranslationToFrench() throws AiException {
    server.enqueue(new MockResponse().setResponseCode(200)
        .setBody("{\n" + "  \"id\": \"chatcmpl-def\",\n" + "  \"object\": \"chat.completion\",\n" + "  \"created\": 1677652288,\n"
            + "  \"model\": \"gpt-4o-mini\",\n" + "  \"choices\": [\n" + "    {\n" + "      \"index\": 0,\n" + "      \"message\": {\n"
            + "        \"role\": \"assistant\",\n" + "        \"content\": \"Ceci est un texte traduit en français.\"\n" + "      },\n"
            + "      \"finish_reason\": \"stop\"\n" + "    }\n" + "  ]\n" + "}"));

    String result = service.translatePlot("This is a plot text.", "French", "");
    assertThat(result).isEqualTo("Ceci est un texte traduit en français.");
  }

  @Test
  public void testTranslationResultIsTrimmed() throws AiException {
    server.enqueue(new MockResponse().setResponseCode(200)
        .setBody("{\n" + "  \"choices\": [\n" + "    {\n" + "      \"message\": {\n" + "        \"content\": \"  Trimmed result.  \"\n" + "      },\n"
            + "      \"finish_reason\": \"stop\"\n" + "    }\n" + "  ]\n" + "}"));

    String result = service.translatePlot("Some plot", "English", "");
    assertThat(result).isEqualTo("Trimmed result.");
  }

  // =======================================================================
  // Error handling - throws AiException
  // =======================================================================

  @Test
  public void testApiErrorThrowsException() {
    server.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":{\"message\":\"Bad request\"}}"));

    assertThatThrownBy(() -> service.translatePlot("Original plot text", "German", "")).isInstanceOf(AiException.class);
  }

  @Test
  public void testServerErrorThrowsException() {
    // enqueue enough responses for all retry attempts (AiService uses 3 retries)
    for (int i = 0; i < 4; i++) {
      server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
    }

    assertThatThrownBy(() -> service.translatePlot("Some plot", "German", "")).isInstanceOf(AiException.class);
  }

  @Test
  public void testMalformedResponseThrowsException() {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("not json at all"));

    assertThatThrownBy(() -> service.translatePlot("Some plot", "German", "")).isInstanceOf(AiException.class);
  }
}
