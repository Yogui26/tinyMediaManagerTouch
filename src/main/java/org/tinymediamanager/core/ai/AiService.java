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
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link AiService} provides AI-powered media metadata operations: plot translation, tagline generation, and outline generation. All
 * methods share the same caching, rate-limiting, and retry infrastructure.
 *
 * @author Manuel Laggner
 */
public class AiService {

  private static final Logger LOGGER                  = LoggerFactory.getLogger(AiService.class);

  private static final int    MAX_RETRIES             = 3;
  private static final long   RATE_LIMITER_TIMEOUT_MS = 30000L;

  // ---------------------------------------------------------------------------
  // Prompt templates (inlined — no separate PromptManager / PromptTemplate)
  // ---------------------------------------------------------------------------

  static final String         PROMPT_TRANSLATE_SYSTEM = "You are a professional translator for movie and TV show metadata. "
      + "Translate the following plot/overview text to {targetLanguage}. "
      + "Preserve all factual information, character names, place names, dates, and release years. "
      + "Do NOT add any explanations, commentary, or extra text. Return only the translated text.";

  static final String         PROMPT_TRANSLATE_USER   = "Translate the following plot to {targetLanguage}:\n\n{plot}";

  static final String         PROMPT_TAGLINE_SYSTEM   = "You are a marketing copywriter for movies and TV shows. "
      + "Generate a compelling, concise {targetLanguage} tagline (maximum {maxWords} words) for the given media. "
      + "The tagline should capture the essence and mood without spoilers. Return only the tagline text, no quotes, no explanations.";

  static final String         PROMPT_TAGLINE_USER     = "Generate a tagline in {targetLanguage} for:\nTitle: {title}\nPlot: {plot}\nYear: {year}";

  static final String         PROMPT_OUTLINE_SYSTEM   = "You are a professional plot writer for movies and TV shows. "
      + "Generate a brief, spoiler-free outline (maximum {maxWords} words) for the given media. "
      + "An outline is a short description of the storyline without analysis, personal opinion, or actor names. "
      + "Do NOT start with 'This film is about' or 'This is a story about'. " + "Do NOT include the title at the beginning of the outline. "
      + "Return only the outline text, no explanations. Return the outline in the same language as the given plot!";

  static final String         PROMPT_OUTLINE_USER     = "Generate an outline for:\nTitle: {title}\nPlot: {plot}";

  // ---------------------------------------------------------------------------
  // Plot translation
  // ---------------------------------------------------------------------------

  /**
   * Translates a plot to the given target language using the configured AI provider.
   *
   * @param plot
   *          the original plot text
   * @param targetLanguage
   *          the target language (e.g. "German", "French", "Bavarian")
   * @return the translated plot, or the original plot if translation fails or is disabled
   */
  public String translatePlot(String plot, String targetLanguage) {
    if (StringUtils.isBlank(plot)) {
      return plot;
    }

    if (StringUtils.isBlank(targetLanguage)) {
      return plot;
    }

    String languageName = targetLanguage;

    try {
      AiClient client = AiManager.getInstance().getDefaultClient();

      String cacheKey = AiCache.generateKey(client.getProviderName(), "", buildCacheRequest("translate", plot, languageName));
      String cached = AiCache.getInstance().get(cacheKey);
      if (cached != null) {
        LOGGER.debug("Using cached translation for plot ({})", languageName);
        return cached;
      }

      Map<String, String> variables = Map.of("targetLanguage", languageName, "plot", plot);

      List<AiMessage> messages = new ArrayList<>();
      messages.add(AiMessage.system(substitute(PROMPT_TRANSLATE_SYSTEM, variables)));
      messages.add(AiMessage.user(substitute(PROMPT_TRANSLATE_USER, variables)));

      AiRequest request = new AiRequest(null, messages);
      request.setTemperature(0.3);
      request.setMaxTokens(4000);

      String result = executeWithRetry(client, request, cacheKey, "plot translation to " + languageName);
      if (StringUtils.isNotBlank(result)) {
        AiCache.getInstance().put(cacheKey, result);
        return result;
      }
    }
    catch (AiException e) {
      LOGGER.error("Failed to translate plot to {}: {}", languageName, e.getMessage());
    }
    catch (Exception e) {
      LOGGER.error("Unexpected error during plot translation: {}", e.getMessage());
    }

    return plot;
  }

  // ---------------------------------------------------------------------------
  // Tagline generation
  // ---------------------------------------------------------------------------

  /**
   * Generates a tagline for the given media using the configured AI provider.
   *
   * @param title
   *          the media title
   * @param plot
   *          the plot/overview text
   * @param year
   *          the release year (may be 0)
   * @param targetLanguage
   *          the desired language (e.g. "German", "Bavarian", "English")
   * @param maxWords
   *          the maximum word count for the generated tagline
   * @return the generated tagline, or the empty string if generation fails
   */
  public String generateTagline(String title, String plot, int year, String targetLanguage, int maxWords) {
    if (StringUtils.isBlank(title) && StringUtils.isBlank(plot)) {
      return "";
    }

    String lang = Objects.toString(targetLanguage, "English");

    try {
      AiClient client = AiManager.getInstance().getDefaultClient();

      String cacheKey = AiCache.generateKey(client.getProviderName(), "",
          buildCacheRequest("tagline", title + "|" + plot + "|" + year + "|" + lang, "tagline"));
      String cached = AiCache.getInstance().get(cacheKey);
      if (cached != null) {
        LOGGER.debug("Using cached tagline for '{}'", title);
        return cached;
      }

      Map<String, String> variables = Map.of("title", StringUtils.defaultString(title), "plot", StringUtils.defaultString(plot), "year",
          String.valueOf(year), "targetLanguage", lang, "maxWords", String.valueOf(Math.max(maxWords, 10)));

      List<AiMessage> messages = new ArrayList<>();
      messages.add(AiMessage.system(substitute(PROMPT_TAGLINE_SYSTEM, variables)));
      messages.add(AiMessage.user(substitute(PROMPT_TAGLINE_USER, variables)));

      AiRequest request = new AiRequest(null, messages);
      request.setTemperature(0.8);

      String result = executeWithRetry(client, request, cacheKey, "tagline generation for '" + title + "'");
      if (StringUtils.isNotBlank(result)) {
        AiCache.getInstance().put(cacheKey, result);
        return result;
      }
    }
    catch (AiException e) {
      LOGGER.error("Failed to generate tagline for '{}': {}", title, e.getMessage());
    }
    catch (Exception e) {
      LOGGER.error("Unexpected error during tagline generation: {}", e.getMessage());
    }

    return "";
  }

  // ---------------------------------------------------------------------------
  // Outline generation
  // ---------------------------------------------------------------------------

  /**
   * Generates a brief plot outline for the given media using the configured AI provider.
   * <p>
   * An outline is a short (typically &le; 240 characters), spoiler-free description of the storyline following IMDB guidelines: no actor names, no
   * analysis, no "This is a story about" preamble.
   *
   * @param title
   *          the media title
   * @param plot
   *          the full plot/overview text
   * @param maxWords
   *          the maximum word count for the generated outline
   * @return the generated outline, or the empty string if generation fails
   */
  public String generateOutline(String title, String plot, int maxWords) {
    if (StringUtils.isBlank(title) && StringUtils.isBlank(plot)) {
      return "";
    }

    try {
      AiClient client = AiManager.getInstance().getDefaultClient();

      String cacheKey = AiCache.generateKey(client.getProviderName(), "", buildCacheRequest("outline", title + "|" + plot, "outline"));
      String cached = AiCache.getInstance().get(cacheKey);
      if (cached != null) {
        LOGGER.debug("Using cached outline for '{}'", title);
        return cached;
      }

      Map<String, String> variables = Map.of("title", StringUtils.defaultString(title), "plot", StringUtils.defaultString(plot), "maxWords",
          String.valueOf(Math.max(maxWords, 10)));

      List<AiMessage> messages = new ArrayList<>();
      messages.add(AiMessage.system(substitute(PROMPT_OUTLINE_SYSTEM, variables)));
      messages.add(AiMessage.user(substitute(PROMPT_OUTLINE_USER, variables)));

      AiRequest request = new AiRequest(null, messages);
      request.setTemperature(0.5);

      String result = executeWithRetry(client, request, cacheKey, "outline generation for '" + title + "'");
      if (StringUtils.isNotBlank(result)) {
        AiCache.getInstance().put(cacheKey, result);
        return result;
      }
    }
    catch (AiException e) {
      LOGGER.error("Failed to generate outline for '{}': {}", title, e.getMessage());
    }
    catch (Exception e) {
      LOGGER.error("Unexpected error during outline generation: {}", e.getMessage());
    }

    return "";
  }

  // ---------------------------------------------------------------------------
  // Shared retry loop
  // ---------------------------------------------------------------------------

  private String executeWithRetry(AiClient client, AiRequest request, String cacheKey, String context) throws AiException {
    String providerName = client.getProviderName();
    AiRateLimiter rateLimiter = AiRateLimiter.getInstance();

    for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      try {
        if (!rateLimiter.waitForPermission(providerName, RATE_LIMITER_TIMEOUT_MS)) {
          LOGGER.warn("Rate limit timeout for {} ({})", context, cacheKey);
          return null;
        }

        AiResponse response = client.chat(request);
        rateLimiter.recordSuccessfulCall(providerName);

        String content = response.getContent();
        if (StringUtils.isNotBlank(content)) {
          return content.trim();
        }
        return null;
      }
      catch (AiException.RateLimit e) {
        rateLimiter.record429Error(providerName);
        if (!RetryUtils.shouldRetry(attempt, MAX_RETRIES)) {
          throw e;
        }
        RetryUtils.waitBeforeRetry(attempt + 1, context);
      }
      catch (AiException e) {
        rateLimiter.recordFailedCall(providerName);
        if (!RetryUtils.shouldRetry(attempt, MAX_RETRIES) || !RetryUtils.isRetryable(e)) {
          throw e;
        }
        RetryUtils.waitBeforeRetry(attempt + 1, context);
      }
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static AiRequest buildCacheRequest(String prefix, String payload, String cacheHint) {
    return new AiRequest("", prefix, cacheHint + ": " + payload);
  }

  private static String substitute(String template, Map<String, String> variables) {
    String result = template;
    if (variables != null) {
      for (Map.Entry<String, String> entry : variables.entrySet()) {
        result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
      }
    }
    return result;
  }
}
