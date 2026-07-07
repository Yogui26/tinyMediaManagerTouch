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

import org.tinymediamanager.scraper.entities.BaseJsonEntity;

import com.google.gson.annotations.SerializedName;

/**
 * the extended people record
 */
public class PeopleExtendedRecord extends BaseJsonEntity {
  @SerializedName("aliases")
  public List<Alias>           aliases              = null;

  @SerializedName("awards")
  public List<AwardBaseRecord> awards               = null;

  @SerializedName("biographies")
  public List<Biography>       biographies          = null;

  @SerializedName("birth")
  public String                birth                = null;

  @SerializedName("birthPlace")
  public String                birthPlace           = null;

  @SerializedName("characters")
  public List<Character>       characters           = null;

  @SerializedName("death")
  public String                death                = null;

  @SerializedName("gender")
  public Integer               gender               = null;

  @SerializedName("id")
  public Integer               id                   = null;

  @SerializedName("image")
  public String                image                = null;

  @SerializedName("lastUpdated")
  public String                lastUpdated          = null;

  @SerializedName("name")
  public String                name                 = null;

  @SerializedName("nameTranslations")
  public List<String>          nameTranslations     = null;

  @SerializedName("overviewTranslations")
  public List<String>          overviewTranslations = null;

  @SerializedName("remoteIds")
  public List<RemoteID>        remoteIds            = null;

  @SerializedName("score")
  public Integer               score                = null;

  @SerializedName("slug")
  public String                slug                 = null;

  @SerializedName("tagOptions")
  public List<TagOption>       tagOptions           = null;

  @SerializedName("translations")
  public TranslationExtended   translations         = null;
}
