package org.tinymediamanager.scraper.mdblist;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IRatingProvider;

public class MdbListRatingProvider extends MdbListMetadataProvider implements IRatingProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(MdbListRatingProvider.class);

  @Override
  protected String getSubId() {
    return "";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public List<MediaRating> getRatings(Map<String, Object> ids, MediaType mediaType) throws ScrapeException {
    getLogger().debug("getRatings(): '{}'", ids);
    initAPI();

    // MDBList does only carry show (and movie) ratings, so skip for episodes
    if (mediaType == MediaType.TV_EPISODE) {
      return Collections.emptyList();
    }

    // create searchOptions for getMetadata call
    MediaSearchAndScrapeOptions options = new MovieSearchAndScrapeOptions();
    if (mediaType == MediaType.TV_SHOW) {
      options = new TvShowSearchAndScrapeOptions();
    }
    options.setIds(ids);

    MediaMetadata md = getMetadata(options);
    return md.getRatings();
  }
}
