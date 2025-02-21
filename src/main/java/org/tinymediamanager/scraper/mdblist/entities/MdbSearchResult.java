package org.tinymediamanager.scraper.mdblist.entities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.tinymediamanager.scraper.entities.BaseJsonEntity;

public class MdbSearchResult extends BaseJsonEntity {
  public String              title;
  public int                 year;
  public int                 score;
  public int                 score_average;
  public String              type;
  /**
   * CAUTION: remove the word "id" from key - search responds in format "imdbid"
   */
  public Map<String, Object> ids = new ConcurrentHashMap<>(0);
}