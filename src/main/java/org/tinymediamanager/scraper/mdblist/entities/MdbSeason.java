package org.tinymediamanager.scraper.mdblist.entities;

import org.tinymediamanager.scraper.entities.BaseJsonEntity;

public class MdbSeason extends BaseJsonEntity {
  public int    tmdbid;
  public String name;
  // FIXME: public Date air_date; // Failed to parse date ["2009-01-15T00:00:00"]: No time zone indicator
  public String air_date;
  public int    episode_count;
  public int    season_number;
  public String tomatofresh;
  public String poster_path;
}