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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The class {@link AiResponse} represents a response from an OpenAI-compatible chat completions API.
 *
 * @author Manuel Laggner
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiResponse {

  @JsonProperty("id")
  private String       id;

  @JsonProperty("object")
  private String       object;

  @JsonProperty("created")
  private long         created;

  @JsonProperty("model")
  private String       model;

  @JsonProperty("choices")
  private List<Choice> choices;

  @JsonProperty("usage")
  private Usage        usage;

  @JsonProperty("error")
  private ApiError     error;

  AiResponse() {
  }

  /**
   * Gets the content of the first choice message.
   *
   * @return the content text, or null if no choices available
   */
  public String getContent() {
    if (choices != null && !choices.isEmpty() && choices.get(0).message != null) {
      return choices.get(0).message.content;
    }
    return null;
  }

  /**
   * Gets the finish reason of the first choice.
   *
   * @return the finish reason (stop, length, content_filter, null)
   */
  public String getFinishReason() {
    if (choices != null && !choices.isEmpty()) {
      return choices.get(0).finishReason;
    }
    return null;
  }

  public String getId() {
    return id;
  }

  public String getObject() {
    return object;
  }

  public long getCreated() {
    return created;
  }

  public String getModel() {
    return model;
  }

  public List<Choice> getChoices() {
    return choices;
  }

  public Usage getUsage() {
    return usage;
  }

  public ApiError getError() {
    return error;
  }

  public boolean hasError() {
    return error != null;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Choice {

    @JsonProperty("index")
    private int     index;

    @JsonProperty("message")
    private Message message;

    @JsonProperty("finish_reason")
    private String  finishReason;

    public int getIndex() {
      return index;
    }

    public Message getMessage() {
      return message;
    }

    public String getFinishReason() {
      return finishReason;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Message {

    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String content;

    public String getRole() {
      return role;
    }

    public String getContent() {
      return content;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Usage {

    @JsonProperty("prompt_tokens")
    private int promptTokens;

    @JsonProperty("completion_tokens")
    private int completionTokens;

    @JsonProperty("total_tokens")
    private int totalTokens;

    public int getPromptTokens() {
      return promptTokens;
    }

    public int getCompletionTokens() {
      return completionTokens;
    }

    public int getTotalTokens() {
      return totalTokens;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ApiError {

    @JsonProperty("message")
    private String message;

    @JsonProperty("type")
    private String type;

    @JsonProperty("code")
    private String code;

    public String getMessage() {
      return message;
    }

    public String getType() {
      return type;
    }

    public String getCode() {
      return code;
    }
  }
}
