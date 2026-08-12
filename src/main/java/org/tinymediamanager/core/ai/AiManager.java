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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.ai.AiException.Configuration;
import org.tinymediamanager.core.ai.anthropic.AnthropicChatClient;
import org.tinymediamanager.core.ai.openai.OpenAiChatClient;
import org.tinymediamanager.license.TmmFeature;

/**
 * The class {@link AiManager} is the central facade for AI operations.
 * <p>
 * It manages the lifecycle of the {@link AiClient} and provides convenience methods for common tasks.
 * </p>
 *
 * @author Manuel Laggner
 */
public class AiManager implements TmmFeature {
  private static final Logger LOGGER              = LoggerFactory.getLogger(AiManager.class);

  /** Default model used when no per-feature model is configured. */
  private static final int    DEFAULT_MAX_TOKENS  = 2000;
  private static final double DEFAULT_TEMPERATURE = 0.7;

  private static AiManager    instance;
  private AiClient            defaultClient;

  private AiManager() {
    // private constructor
  }

  /**
   * Gets the singleton instance.
   *
   * @return the AiManager instance
   */
  public static synchronized AiManager getInstance() {
    if (instance == null) {
      instance = new AiManager();
    }
    return instance;
  }

  /**
   * Gets the {@link AiClient} configured from {@link Settings}.
   *
   * @return the client
   * @throws AiException.Configuration
   *           if the provider is not configured
   */
  public AiClient getDefaultClient() throws Configuration {
    if (!isFeatureEnabled()) {
      throw new Configuration("AI service is not enabled with your current license");
    }
    if (defaultClient == null) {
      resetClient();
    }
    return defaultClient;
  }

  /**
   * Gets the {@link AiClient} configured from {@link Settings} with the given model.
   *
   * @param model
   *          the model to use (falls back to the default model from the settings if blank)
   * @return the client
   */
  public AiClient getClient(String model) {
    Settings settings = Settings.getInstance();
    String effectiveModel = StringUtils.isNotBlank(model) ? model : settings.getAiModel();
    return createClient(settings, effectiveModel);
  }

  /**
   * Rebuilds the client from current settings (e.g. after settings changed).
   */
  public void resetClient() {
    Settings settings = Settings.getInstance();
    defaultClient = createClient(settings, settings.getAiModel());
  }

  private AiClient createClient(Settings settings, String model) {
    String providerType = settings.getAiProviderType();
    String endpointUrl = settings.getAiEndpointUrl();
    String apiKey = settings.getAiApiKey();

    LOGGER.debug("Creating AI client for provider: {} at endpoint: {} (model: {})", providerType, endpointUrl, model);

    if ("anthropic".equalsIgnoreCase(providerType)) {
      return new AnthropicChatClient(providerType, endpointUrl, apiKey, model, DEFAULT_MAX_TOKENS, DEFAULT_TEMPERATURE);
    }

    // Default to OpenAI-compatible endpoint
    return new OpenAiChatClient(providerType, endpointUrl, apiKey, model, DEFAULT_MAX_TOKENS, DEFAULT_TEMPERATURE);
  }

  /**
   * Tests the connection to the configured provider.
   *
   * @return true if the connection succeeds
   * @throws AiException
   *           if the connection fails
   */
  public boolean testConnection() throws AiException {
    if (!isFeatureEnabled()) {
      return false;
    }
    return getDefaultClient().testConnection();
  }

  /**
   * Clears the client cache (e.g., after settings changed).
   */
  public void clearClientCache() {
    defaultClient = null;
  }

  /**
   * Clears the AI response cache.
   */
  public void clearCache() {
    AiCache.getInstance().clear();
  }

  /**
   * Gets statistics from the rate limiter.
   *
   * @return statistics string
   */
  public String getStatistics() {
    return AiRateLimiter.getInstance().getStatistics();
  }
}
