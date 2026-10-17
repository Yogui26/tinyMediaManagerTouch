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
package org.tinymediamanager.core.tvshow;

import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.CustomColumn;
import org.tinymediamanager.core.TmmResourceBundle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Class {@link TvShowCustomColumn} holds the configuration of one freely configurable column of the TV show table.
 * <p>
 * The {@link #getId()} is stable for the lifetime of this column - it never changes when columns get added, removed or reordered. Since it is used to
 * derive the identifier of the table column, the user loses neither the width nor the visibility of a column when he tinkers around with his
 * configuration.
 * <p>
 * The TV show table is hierarchical, so a column carries three patterns: one for the TV show rows, one for the season rows and one for the episode
 * rows. A pattern which is left blank simply leaves the column empty on that level.
 *
 * @author Manuel Laggner
 */
public class TvShowCustomColumn extends AbstractModelObject implements CustomColumn {

  /**
   * The prefix of the table column identifier belonging to a custom column
   */
  public static final String COLUMN_IDENTIFIER_PREFIX = "customColumn-";

  @JsonProperty
  private String             id;

  @JsonProperty
  private String             title;

  @JsonProperty
  private String             showPattern;

  @JsonProperty
  private String             seasonPattern;

  @JsonProperty
  private String             episodePattern;

  /**
   * Creates an unconfigured custom column with a fresh id. Needed for JSON unmarshalling too.
   */
  public TvShowCustomColumn() {
    id = UUID.randomUUID().toString();
  }

  /**
   * Creates a configured custom column with a fresh id.
   *
   * @param title
   *          the header title of the column
   * @param showPattern
   *          the JMTE pattern providing the content of the TV show rows
   */
  public TvShowCustomColumn(String title, String showPattern) {
    this();
    this.title = title;
    this.showPattern = showPattern;
  }

  /**
   * @return the stable id of this custom column, never null
   */
  @Override
  public String getId() {
    if (StringUtils.isBlank(id)) {
      id = UUID.randomUUID().toString();
    }
    return id;
  }

  /**
   * @param id
   *          the stable id of this custom column
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the header title of this custom column
   */
  @Override
  public String getTitle() {
    return title;
  }

  /**
   * @param title
   *          the header title of this custom column
   */
  @Override
  public void setTitle(String title) {
    String oldValue = this.title;
    this.title = title;
    firePropertyChange("title", oldValue, title);
  }

  /**
   * @return the JMTE pattern providing the content of this custom column on the TV show rows
   */
  public String getShowPattern() {
    return showPattern;
  }

  /**
   * @param showPattern
   *          the JMTE pattern providing the content of this custom column on the TV show rows
   */
  public void setShowPattern(String showPattern) {
    String oldValue = this.showPattern;
    this.showPattern = showPattern;
    firePropertyChange("showPattern", oldValue, showPattern);
  }

  /**
   * @return the JMTE pattern providing the content of this custom column on the season rows
   */
  public String getSeasonPattern() {
    return seasonPattern;
  }

  /**
   * @param seasonPattern
   *          the JMTE pattern providing the content of this custom column on the season rows
   */
  public void setSeasonPattern(String seasonPattern) {
    String oldValue = this.seasonPattern;
    this.seasonPattern = seasonPattern;
    firePropertyChange("seasonPattern", oldValue, seasonPattern);
  }

  /**
   * @return the JMTE pattern providing the content of this custom column on the episode rows
   */
  public String getEpisodePattern() {
    return episodePattern;
  }

  /**
   * @param episodePattern
   *          the JMTE pattern providing the content of this custom column on the episode rows
   */
  public void setEpisodePattern(String episodePattern) {
    String oldValue = this.episodePattern;
    this.episodePattern = episodePattern;
    firePropertyChange("episodePattern", oldValue, episodePattern);
  }

  /**
   * @return true when at least one level of the TV show table has a JMTE pattern configured
   */
  @JsonIgnore
  public boolean isConfigured() {
    return StringUtils.isNotBlank(showPattern) || StringUtils.isNotBlank(seasonPattern) || StringUtils.isNotBlank(episodePattern);
  }

  /**
   * Gets the header title of this custom column, falling back to a placeholder when the user did not set a title yet.
   *
   * @param position
   *          the 1-based position of this column within the configured custom columns
   * @return the title to show in the table header
   */
  @Override
  public String getTitleOrPlaceholder(int position) {
    return StringUtils.isBlank(title) ? TmmResourceBundle.getString("Table.columns.custom") + " " + position : title;
  }

  /**
   * Returns the identifier of the table column belonging to this custom column.
   *
   * @return the table column identifier
   */
  @Override
  @JsonIgnore
  public String getColumnIdentifier() {
    return COLUMN_IDENTIFIER_PREFIX + getId();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TvShowCustomColumn other = (TvShowCustomColumn) o;
    // the id is stable and unique, the rest is mutable - and it heals itself, so two columns never share it
    return Objects.equals(getId(), other.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
  }
}
