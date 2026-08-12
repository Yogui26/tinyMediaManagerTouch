package org.tinymediamanager.core.ai.anthropic;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The class {@link AnthropicResponse} represents a response from the Anthropic (Claude) chat completions API.
 *
 * @author Manuel Laggner
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnthropicResponse {

  @JsonProperty("id")
  private String                       id;

  @JsonProperty("type")
  private String                       type;

  @JsonProperty("role")
  private String                       role;

  @JsonProperty("content")
  private List<ContentBlock> content;

  @JsonProperty("model")
  private String                       model;

  @JsonProperty("stop_reason")
  private String                       stopReason;

  @JsonProperty("stop")
  private String                       stop;

  @JsonProperty("usage")
  private Usage                        usage;

  @JsonProperty("error")
  private ApiError                     error;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public List<ContentBlock> getContent() {
    return content;
  }

   public void setContent(List<ContentBlock> content) {
     this.content = content;
   }

  /**
   * Extracts the text content from the response. Handles both "thinking" and "text" content blocks.
   *
   * @return the text content, or null if no text block found
   */
  public String getTextContent() {
    if (content == null) {
      return null;
    }
    for (ContentBlock block : content) {
      if ("text".equals(block.getType())) {
        return block.getText();
      }
    }
    return null;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getStopReason() {
    return stopReason;
  }

  public void setStopReason(String stopReason) {
    this.stopReason = stopReason;
  }

  public String getStop() {
    return stop;
  }

  public void setStop(String stop) {
    this.stop = stop;
  }

  public Usage getUsage() {
    return usage;
  }

  public void setUsage(Usage usage) {
    this.usage = usage;
  }

  public ApiError getError() {
    return error;
  }

  public void setError(ApiError error) {
    this.error = error;
  }

  public boolean hasError() {
    return error != null;
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

    public void setMessage(String message) {
      this.message = message;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Usage {

    @JsonProperty("input_tokens")
    private int inputTokens;

    @JsonProperty("output_tokens")
    private int outputTokens;

    public int getInputTokens() {
      return inputTokens;
    }

    public void setInputTokens(int inputTokens) {
      this.inputTokens = inputTokens;
    }

    public int getOutputTokens() {
      return outputTokens;
    }

    public void setOutputTokens(int outputTokens) {
      this.outputTokens = outputTokens;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ContentBlock {

    @JsonProperty("type")
    private String type;

    @JsonProperty("text")
    private String text;

    @JsonProperty("thinking")
    private String thinking;

    @JsonProperty("signature")
    private String signature;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }

    public String getThinking() {
      return thinking;
    }

    public void setThinking(String thinking) {
      this.thinking = thinking;
    }

    public String getSignature() {
      return signature;
    }

    public void setSignature(String signature) {
      this.signature = signature;
    }
  }
}
