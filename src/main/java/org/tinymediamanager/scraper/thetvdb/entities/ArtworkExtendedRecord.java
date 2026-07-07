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
package org.tinymediamanager.scraper.thetvdb.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * the extended artwork record
 */
public class ArtworkExtendedRecord {
  @SerializedName("episodeId")
  public Integer           episodeId        = null;

  @SerializedName("height")
  public Integer           height           = null;

  @SerializedName("id")
  public Integer           id               = null;

  @SerializedName("image")
  public String            image            = null;

  @SerializedName("includesText")
  public Boolean           includesText     = null;

  @SerializedName("language")
  public String            language         = null;

  @SerializedName("movieId")
  public Integer           movieId          = null;

  @SerializedName("networkId")
  public Integer           networkId        = null;

  @SerializedName("peopleId")
  public Integer           peopleId         = null;

  @SerializedName("score")
  public Double            score            = null;

  @SerializedName("seasonId")
  public Integer           seasonId         = null;

  @SerializedName("seriesId")
  public Integer           seriesId         = null;

  @SerializedName("seriesPeopleId")
  public Integer           seriesPeopleId   = null;

  @SerializedName("status")
  public ArtworkStatus     status           = null;

  @SerializedName("tagOptions")
  public List<TagOption>   tagOptions       = null;

  @SerializedName("thumbnail")
  public String            thumbnail        = null;

  @SerializedName("thumbnailHeight")
  public Integer           thumbnailHeight  = null;

  @SerializedName("thumbnailWidth")
  public Integer           thumbnailWidth   = null;

  @SerializedName("type")
  public Integer           type             = null;

  @SerializedName("updatedAt")
  public Long              updatedAt        = null;

  @SerializedName("width")
  public Integer           width            = null;
}
