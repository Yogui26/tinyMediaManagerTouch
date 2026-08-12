package org.tinymediamanager.core.ai.anthropic;

import org.tinymediamanager.core.ai.AiMessage;

/**
 * Utility class to convert between {@link AiMessage} and {@link AnthropicMessage}.
 *
 * @author Manuel Laggner
 */
public class AnthropicMessageConverter {

  /**
   * Converts a list of AiMessage to AnthropicMessage.
   *
   * @param messages
   *          the messages to convert
   * @return the converted messages
   */
  public static java.util.List<AnthropicMessage> toAnthropicMessages(java.util.List<AiMessage> messages) {
    java.util.List<AnthropicMessage> anthropicMessages = new java.util.ArrayList<>();
    for (AiMessage msg : messages) {
      String role = "user"; // default
      String roleStr = msg.getRole();
      if ("system".equalsIgnoreCase(roleStr)) {
        role = "system";
      }
      else if ("assistant".equalsIgnoreCase(roleStr)) {
        role = "assistant";
      }
      anthropicMessages.add(new AnthropicMessage(role, msg.getContent()));
    }
    return anthropicMessages;
  }

  /**
   * Converts a single AiMessage to AnthropicMessage.
   *
   * @param message
   *          the message to convert
   * @return the converted message
   */
  public static AnthropicMessage toAnthropicMessage(AiMessage message) {
    String role = "user"; // default
    String roleStr = message.getRole();
    if ("system".equalsIgnoreCase(roleStr)) {
      role = "system";
    }
    else if ("assistant".equalsIgnoreCase(roleStr)) {
      role = "assistant";
    }
    return new AnthropicMessage(role, message.getContent());
  }
}
