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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Tests for {@link OpenAiChatClient} using MockWebServer.
 */
public class AiClientTest extends BasicTest {

  private MockWebServer    server;
  private OpenAiChatClient client;

  @Before
  public void setup() throws Exception {
    super.setup();
    server = new MockWebServer();
    server.start();
  }

  @After
  public void shutDown() throws Exception {
    if (server != null) {
      server.shutdown();
    }
  }

  private void createClient(String apiKey) {
    OkHttpClient testClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build();
    client = new OpenAiChatClient("test", server.url("/v1/chat/completions").toString(), apiKey, "gpt-4o-mini", 2000, 0.7, testClient);
  }

  // =======================================================================
  // Successful responses
  // =======================================================================

  @Test
  public void testSuccessfulChatCompletion() throws Exception {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(200)
        .setBody("{\n" + "  \"id\": \"chatcmpl-abc123\",\n" + "  \"object\": \"chat.completion\",\n" + "  \"created\": 1677652288,\n"
            + "  \"model\": \"gpt-4o-mini\",\n" + "  \"choices\": [\n" + "    {\n" + "      \"index\": 0,\n" + "      \"message\": {\n"
            + "        \"role\": \"assistant\",\n" + "        \"content\": \"Hello! How can I help you today?\"\n" + "      },\n"
            + "      \"finish_reason\": \"stop\"\n" + "    }\n" + "  ],\n" + "  \"usage\": {\n" + "    \"prompt_tokens\": 10,\n"
            + "    \"completion_tokens\": 8,\n" + "    \"total_tokens\": 18\n" + "  }\n" + "}"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system prompt", "user prompt");
    AiResponse response = client.chat(request);

    assertThat(response).isNotNull();
    assertThat(response.getContent()).isEqualTo("Hello! How can I help you today?");
    assertThat(response.getFinishReason()).isEqualTo("stop");
    assertThat(response.getId()).isEqualTo("chatcmpl-abc123");
    assertThat(response.getModel()).isEqualTo("gpt-4o-mini");

    // verify usage
    assertThat(response.getUsage()).isNotNull();
    assertThat(response.getUsage().getPromptTokens()).isEqualTo(10);
    assertThat(response.getUsage().getCompletionTokens()).isEqualTo(8);
    assertThat(response.getUsage().getTotalTokens()).isEqualTo(18);

    // verify request was sent correctly
    RecordedRequest recorded = server.takeRequest();
    assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer sk-test123");
    assertThat(recorded.getHeader("Content-Type")).startsWith("application/json");
    assertThat(recorded.getMethod()).isEqualTo("POST");
  }

  @Test
  public void testMultipleChoices() throws Exception {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(200)
        .setBody("{\n" + "  \"id\": \"chatcmpl-abc\",\n" + "  \"object\": \"chat.completion\",\n" + "  \"created\": 1677652288,\n"
            + "  \"model\": \"gpt-4o-mini\",\n" + "  \"choices\": [\n" + "    {\n" + "      \"index\": 0,\n" + "      \"message\": {\n"
            + "        \"role\": \"assistant\",\n" + "        \"content\": \"First choice\"\n" + "      },\n" + "      \"finish_reason\": \"stop\"\n"
            + "    },\n" + "    {\n" + "      \"index\": 1,\n" + "      \"message\": {\n" + "        \"role\": \"assistant\",\n"
            + "        \"content\": \"Second choice\"\n" + "      },\n" + "      \"finish_reason\": \"stop\"\n" + "    }\n" + "  ]\n" + "}"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    AiResponse response = client.chat(request);

    // should return the first choice
    assertThat(response.getContent()).isEqualTo("First choice");
    assertThat(response.getChoices()).hasSize(2);
  }

  @Test
  public void testEmptyContent() throws Exception {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(200)
        .setBody("{\n" + "  \"id\": \"chatcmpl-abc\",\n" + "  \"object\": \"chat.completion\",\n" + "  \"created\": 1677652288,\n"
            + "  \"model\": \"gpt-4o-mini\",\n" + "  \"choices\": []\n" + "}"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    AiResponse response = client.chat(request);
    assertThat(response.getContent()).isNull();
  }

  @Test
  public void testNullContentInMessage() throws Exception {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(200)
        .setBody("{\n" + "  \"id\": \"chatcmpl-abc\",\n" + "  \"object\": \"chat.completion\",\n" + "  \"created\": 1677652288,\n"
            + "  \"model\": \"gpt-4o-mini\",\n" + "  \"choices\": [\n" + "    {\n" + "      \"index\": 0,\n" + "      \"message\": {\n"
            + "        \"role\": \"assistant\",\n" + "        \"content\": null\n" + "      },\n" + "      \"finish_reason\": \"stop\"\n" + "    }\n"
            + "  ]\n" + "}"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    AiResponse response = client.chat(request);
    assertThat(response.getContent()).isNull();
  }

  // =======================================================================
  // Error responses
  // =======================================================================

  @Test
  public void testAuthenticationError() {
    createClient("sk-bad-key");
    server.enqueue(new MockResponse().setResponseCode(401)
        .setBody("{\n" + "  \"error\": {\n" + "    \"message\": \"Incorrect API key provided\",\n" + "    \"type\": \"invalid_request_error\",\n"
            + "    \"code\": \"invalid_api_key\"\n" + "  }\n" + "}"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    assertThatThrownBy(() -> client.chat(request)).isInstanceOf(AiException.Authentication.class).hasMessageContaining("Incorrect API key");
  }

  @Test
  public void testForbiddenError() {
    createClient("sk-key");
    server.enqueue(new MockResponse().setResponseCode(403)
        .setBody("{\n" + "  \"error\": {\n" + "    \"message\": \"You are not allowed\",\n" + "    \"type\": \"access_denied\"\n" + "  }\n" + "}"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    assertThatThrownBy(() -> client.chat(request)).isInstanceOf(AiException.Authentication.class);
  }

  @Test
  public void testRateLimitError() throws Exception {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(429)
        .setBody(
            "{\n" + "  \"error\": {\n" + "    \"message\": \"Rate limit exceeded\",\n" + "    \"type\": \"rate_limit_error\"\n" + "  }\n" + "}"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    // use a short timeout to avoid long test duration
    assertThatThrownBy(() -> client.chat(request)).isInstanceOf(AiException.RateLimit.class);
  }

  @Test
  public void testBadRequestError() {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(400)
        .setBody("{\n" + "  \"error\": {\n" + "    \"message\": \"The model `gpt-5` does not exist\",\n"
            + "    \"type\": \"invalid_request_error\",\n" + "    \"code\": \"model_not_found\"\n" + "  }\n" + "}"));

    AiRequest request = new AiRequest("gpt-5", "system", "user");
    assertThatThrownBy(() -> client.chat(request)).isInstanceOf(AiException.Api.class)
        .matches(e -> ((AiException.Api) e).getStatusCode() == 400)
        .hasMessageContaining("model `gpt-5`");
  }

  @Test
  public void testServerError() {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    assertThatThrownBy(() -> client.chat(request)).isInstanceOf(AiException.Api.class).matches(e -> ((AiException.Api) e).getStatusCode() == 500);
  }

  @Test
  public void testServiceUnavailable() {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    assertThatThrownBy(() -> client.chat(request)).isInstanceOf(AiException.Api.class).matches(e -> ((AiException.Api) e).getStatusCode() == 503);
  }

  // =======================================================================
  // Response with error in body
  // =======================================================================

  @Test
  public void testApiErrorInResponseBody() {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(200)
        .setBody("{\n" + "  \"error\": {\n" + "    \"message\": \"The model is currently overloaded\",\n" + "    \"type\": \"server_error\"\n"
            + "  }\n" + "}"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    assertThatThrownBy(() -> client.chat(request)).isInstanceOf(AiException.Api.class).hasMessageContaining("currently overloaded");
  }

  // =======================================================================
  // Missing API key
  // =======================================================================

  @Test
  public void testMissingApiKey() {
    createClient("");

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    assertThatThrownBy(() -> client.chat(request)).isInstanceOf(AiException.Authentication.class).hasMessageContaining("API key is not configured");
  }

  // =======================================================================
  // Timeout
  // =======================================================================

  @Test
  public void testCustomTimeout() throws Exception {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    AiResponse response = client.chat(request, Duration.ofSeconds(5));
    assertThat(response.getContent()).isEqualTo("OK");
  }

  // =======================================================================
  // Unknown JSON fields are ignored
  // =======================================================================

  @Test
  public void testUnknownFieldsAreIgnored() throws Exception {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(200)
        .setBody("{\n" + "  \"id\": \"chatcmpl-abc\",\n" + "  \"object\": \"chat.completion\",\n" + "  \"created\": 1677652288,\n"
            + "  \"model\": \"gpt-4o-mini\",\n" + "  \"system_fingerprint\": \"fp_123\",\n" + "  \"unknown_field\": \"should not crash\",\n"
            + "  \"choices\": [\n" + "    {\n" + "      \"index\": 0,\n" + "      \"message\": {\n" + "        \"role\": \"assistant\",\n"
            + "        \"content\": \"Result\"\n" + "      },\n" + "      \"unknown_choice_field\": \"also ignored\"\n" + "    }\n" + "  ]\n" + "}"));

    AiRequest request = new AiRequest("gpt-4o-mini", "system", "user");
    AiResponse response = client.chat(request);
    assertThat(response.getContent()).isEqualTo("Result");
  }

  // =======================================================================
  // Test connection
  // =======================================================================

  @Test
  public void testConnectionSuccess() throws Exception {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}"));

    assertThat(client.testConnection()).isTrue();
  }

  @Test
  public void testConnectionFailure() {
    createClient("sk-test123");
    server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":{\"message\":\"bad key\"}}"));

    assertThatThrownBy(() -> client.testConnection()).isInstanceOf(AiException.Authentication.class);
  }
}
