package org.tinymediamanager.core.ai.anthropic;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The class {@link AnthropicRequest} represents a request to the Anthropic (Claude) chat completions API.
 *
 * @author Manuel Laggner
 */
public class AnthropicRequest {

  @JsonProperty("model")
  private String                 model;

  @JsonProperty("messages")
  private List<AnthropicMessage> messages    = new ArrayList<>();

  @JsonProperty("temperature")
  private double                 temperature = 0.7;

  @JsonProperty("max_tokens")
  private int                    maxTokens   = 2000;

  @JsonProperty("stream")
  private boolean                stream      = false;

  public AnthropicRequest() {
  }

  /**
   * Creates a new Anthropic request.
   *
   * @param model
   *          the model name
   * @param messages
   *          the conversation messages
   * @param maxTokens
   *          the maximum tokens for responses
   * @param temperature
   *          the temperature (0.0 - 2.0)
   */
  public AnthropicRequest(String model, List<AnthropicMessage> messages, int maxTokens, double temperature) {
    this.model = model;
    this.messages = new ArrayList<>(messages);
    this.maxTokens = maxTokens;
    this.temperature = temperature;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public List<AnthropicMessage> getMessages() {
    return messages;
  }

  public void setMessages(List<AnthropicMessage> messages) {
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
