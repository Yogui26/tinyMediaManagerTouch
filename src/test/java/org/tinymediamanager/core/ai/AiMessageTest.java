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

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Tests for {@link AiMessage}, {@link AiRequest}, and {@link AiResponse} serialization.
 */
public class AiMessageTest {

  private final ObjectMapper mapper = JsonMapper.builder().build();

  // =======================================================================
  // AiMessage factory methods
  // =======================================================================

  @Test
  public void testSystemMessage() {
    AiMessage msg = AiMessage.system("be helpful");
    assertThat(msg.getRole()).isEqualTo("system");
    assertThat(msg.getContent()).isEqualTo("be helpful");
  }

  @Test
  public void testUserMessage() {
    AiMessage msg = AiMessage.user("hello");
    assertThat(msg.getRole()).isEqualTo("user");
    assertThat(msg.getContent()).isEqualTo("hello");
  }

  @Test
  public void testAssistantMessage() {
    AiMessage msg = AiMessage.assistant("response");
    assertThat(msg.getRole()).isEqualTo("assistant");
    assertThat(msg.getContent()).isEqualTo("response");
  }

  // =======================================================================
  // AiMessage equals/hashCode
  // =======================================================================

  @Test
  public void testEqualsSameValues() {
    AiMessage a = AiMessage.system("prompt");
    AiMessage b = AiMessage.system("prompt");
    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  public void testEqualsDifferentContent() {
    AiMessage a = AiMessage.system("a");
    AiMessage b = AiMessage.system("b");
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  public void testEqualsDifferentRole() {
    AiMessage a = AiMessage.system("x");
    AiMessage b = AiMessage.user("x");
    assertThat(a).isNotEqualTo(b);
  }

  // =======================================================================
  // AiRequest serialization
  // =======================================================================

  @Test
  public void testRequestSerialization() throws Exception {
    AiRequest request = new AiRequest("gpt-4o-mini", "be helpful", "hello");
    String json = mapper.writeValueAsString(request);

    assertThat(json).contains("\"model\":\"gpt-4o-mini\"");
    assertThat(json).contains("\"role\":\"system\"");
    assertThat(json).contains("\"content\":\"be helpful\"");
    assertThat(json).contains("\"role\":\"user\"");
    assertThat(json).contains("\"content\":\"hello\"");
    assertThat(json).contains("\"max_tokens\"");
    assertThat(json).contains("\"temperature\"");
  }

  @Test
  public void testRequestDeserialization() throws Exception {
    String json = "{" + "\"model\":\"gpt-4o\"," + "\"messages\":[{\"role\":\"system\",\"content\":\"sys\"}]," + "\"temperature\":0.5,"
        + "\"max_tokens\":100" + "}";

    AiRequest request = mapper.readValue(json, AiRequest.class);
    assertThat(request.getModel()).isEqualTo("gpt-4o");
    assertThat(request.getMessages()).hasSize(1);
    assertThat(request.getMessages().get(0).getRole()).isEqualTo("system");
    assertThat(request.getMessages().get(0).getContent()).isEqualTo("sys");
    assertThat(request.getTemperature()).isEqualTo(0.5);
    assertThat(request.getMaxTokens()).isEqualTo(100);
  }

  @Test
  public void testRequestRoundTrip() throws Exception {
    AiRequest original = new AiRequest("gpt-4o-mini", "sys", "user prompt");
    original.setTemperature(0.3);
    original.setMaxTokens(4000);

    String json = mapper.writeValueAsString(original);
    AiRequest restored = mapper.readValue(json, AiRequest.class);

    assertThat(restored.getModel()).isEqualTo(original.getModel());
    assertThat(restored.getTemperature()).isEqualTo(original.getTemperature());
    assertThat(restored.getMaxTokens()).isEqualTo(original.getMaxTokens());
    assertThat(restored.getMessages()).hasSize(original.getMessages().size());
    assertThat(restored.getMessages().get(0).getRole()).isEqualTo(original.getMessages().get(0).getRole());
    assertThat(restored.getMessages().get(0).getContent()).isEqualTo(original.getMessages().get(0).getContent());
  }

  // =======================================================================
  // AiResponse deserialization
  // =======================================================================

  @Test
  public void testResponseDeserialization() throws Exception {
    String json = "{" + "\"id\":\"chatcmpl-123\"," + "\"object\":\"chat.completion\"," + "\"created\":1677652288," + "\"model\":\"gpt-4o-mini\","
        + "\"choices\":[{" + "  \"index\":0," + "  \"message\":{\"role\":\"assistant\",\"content\":\"Hello\"}," + "  \"finish_reason\":\"stop\""
        + "}]," + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}" + "}";

    AiResponse response = mapper.readValue(json, AiResponse.class);

    assertThat(response.getId()).isEqualTo("chatcmpl-123");
    assertThat(response.getObject()).isEqualTo("chat.completion");
    assertThat(response.getCreated()).isEqualTo(1677652288L);
    assertThat(response.getModel()).isEqualTo("gpt-4o-mini");
    assertThat(response.getContent()).isEqualTo("Hello");
    assertThat(response.getFinishReason()).isEqualTo("stop");
    assertThat(response.getChoices()).hasSize(1);
    assertThat(response.getUsage()).isNotNull();
    assertThat(response.getUsage().getPromptTokens()).isEqualTo(10);
    assertThat(response.getUsage().getCompletionTokens()).isEqualTo(5);
    assertThat(response.getUsage().getTotalTokens()).isEqualTo(15);
    assertThat(response.hasError()).isFalse();
  }

  @Test
  public void testResponseWithError() throws Exception {
    String json = "{" + "\"error\":{" + "  \"message\":\"Incorrect API key\"," + "  \"type\":\"invalid_request_error\","
        + "  \"code\":\"invalid_api_key\"" + "}}";

    AiResponse response = mapper.readValue(json, AiResponse.class);

    assertThat(response.hasError()).isTrue();
    assertThat(response.getError().getMessage()).isEqualTo("Incorrect API key");
    assertThat(response.getError().getType()).isEqualTo("invalid_request_error");
    assertThat(response.getError().getCode()).isEqualTo("invalid_api_key");
  }

  @Test
  public void testResponseWithNoChoices() throws Exception {
    String json = "{\"id\":\"chatcmpl-123\",\"choices\":[]}";
    AiResponse response = mapper.readValue(json, AiResponse.class);
    assertThat(response.getContent()).isNull();
    assertThat(response.getFinishReason()).isNull();
  }

  @Test
  public void testResponseWithNullContent() throws Exception {
    String json = "{" + "\"choices\":[{" + "  \"index\":0," + "  \"message\":{\"role\":\"assistant\",\"content\":null},"
        + "  \"finish_reason\":\"stop\"" + "}]}";

    AiResponse response = mapper.readValue(json, AiResponse.class);
    assertThat(response.getContent()).isNull();
  }
}
