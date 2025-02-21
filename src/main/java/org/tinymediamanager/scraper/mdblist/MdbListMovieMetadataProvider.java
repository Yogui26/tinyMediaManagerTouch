package org.tinymediamanager.scraper.mdblist;

import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieTmdbMetadataProvider;

public class MdbListMovieMetadataProvider extends MdbListMetadataProvider
    implements IMovieMetadataProvider, IMovieImdbMetadataProvider, IMovieTmdbMetadataProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(MdbListMovieMetadataProvider.class);

  @Override
  protected String getSubId() {
    return "movie";
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options) throws ScrapeException {
    return searchMdb(options);
  }

  @Override
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
    return getMetadata(options, true);
  }
}
