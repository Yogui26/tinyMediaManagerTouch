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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The class {@link AiMessage} represents a single message in an AI chat conversation.
 *
 * @author Manuel Laggner
 */
public class AiMessage {

  @JsonProperty("role")
  private String role;

  @JsonProperty("content")
  private String content;

  AiMessage() {
  }

  /**
   * Creates a new message.
   *
   * @param role
   *          the role (system, user, assistant)
   * @param content
   *          the message content
   */
  public AiMessage(String role, String content) {
    this.role = role;
    this.content = content;
  }

  /**
   * Creates a system message.
   *
   * @param content
   *          the system instruction
   * @return the message
   */
  public static AiMessage system(String content) {
    return new AiMessage("system", content);
  }

  /**
   * Creates a user message.
   *
   * @param content
   *          the user input
   * @return the message
   */
  public static AiMessage user(String content) {
    return new AiMessage("user", content);
  }

  /**
   * Creates an assistant message.
   *
   * @param content
   *          the assistant response
   * @return the message
   */
  public static AiMessage assistant(String content) {
    return new AiMessage("assistant", content);
  }

  public String getRole() {
    return role;
  }

  public String getContent() {
    return content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AiMessage aiMessage = (AiMessage) o;
    return Objects.equals(role, aiMessage.role) && Objects.equals(content, aiMessage.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(role, content);
  }

  @Override
  public String toString() {
    return "AiMessage{" + "role='" + role + '\'' + '}';
  }
}
