package org.tinymediamanager.scraper.imdb.entities;

import java.util.List;

import org.tinymediamanager.scraper.entities.BaseJsonEntity;

public class ImdbEpisodeListEpisodes extends BaseJsonEntity {
  public int                          total       = 0;
  public List<ImdbEpisodeListEpisode> items       = null;
  /**
   * IMDB only displays the first 50 episodes - that flag indicates, that there are more...
   */
  public boolean                      hasNextPage = false;
}
