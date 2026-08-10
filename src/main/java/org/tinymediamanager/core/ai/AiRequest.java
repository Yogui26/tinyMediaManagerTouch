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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The class {@link AiRequest} represents a request to an OpenAI-compatible chat completions API.
 *
 * @author Manuel Laggner
 */
public class AiRequest {

  @JsonProperty("model")
  private String          model;

  @JsonProperty("messages")
  private List<AiMessage> messages    = new ArrayList<>();

  @JsonProperty("temperature")
  private double          temperature = 0.7;

  @JsonProperty("max_tokens")
  private int             maxTokens   = 2000;

  @JsonProperty("stream")
  private boolean         stream      = false;

  AiRequest() {
  }

  /**
   * Creates a new AI request.
   *
   * @param model
   *          the model name
   * @param messages
   *          the conversation messages
   */
  public AiRequest(String model, List<AiMessage> messages) {
    this.model = model;
    this.messages = new ArrayList<>(messages);
  }

  /**
   * Creates a new AI request with default parameters.
   *
   * @param model
   *          the model name
   * @param systemPrompt
   *          the system instruction
   * @param userPrompt
   *          the user input
   */
  public AiRequest(String model, String systemPrompt, String userPrompt) {
    this.model = model;
    this.messages.add(AiMessage.system(systemPrompt));
    this.messages.add(AiMessage.user(userPrompt));
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public List<AiMessage> getMessages() {
    return messages;
  }

  public void setMessages(List<AiMessage> messages) {
    this.messages = messages;
  }

  public double getTemperature() {
    return temperature;
  }

  public void setTemperature(double temperature) {
    this.temperature = temperature;
  }

  public int getMaxTokens() {
    return maxTokens;
  }

  public void setMaxTokens(int maxTokens) {
    this.maxTokens = maxTokens;
  }

  public boolean isStream() {
    return stream;
  }

  public void setStream(boolean stream) {
    this.stream = stream;
  }
}
