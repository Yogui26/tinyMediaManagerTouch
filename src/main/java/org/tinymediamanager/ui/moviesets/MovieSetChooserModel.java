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
package org.tinymediamanager.ui.moviesets;

import static org.tinymediamanager.scraper.util.MediaIdUtil.getMovieImdbIdViaTmdbId;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.observablecollections.ObservableCollections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetArtworkHelper;
import org.tinymediamanager.core.movie.MovieSetScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.tasks.QueueTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSetMetadataProvider;
import org.tinymediamanager.scraper.util.MediaIdUtil;

/**
 * The Class MovieSetChooserModel.
 */
public class MovieSetChooserModel extends AbstractModelObject {

  private static final Logger                LOGGER         = LoggerFactory.getLogger(MovieSetChooserModel.class);
  public static final MovieSetChooserModel   EMPTY_RESULT   = new MovieSetChooserModel();

  private final List<MovieInSet>             movies         = ObservableCollections.observableList(new ArrayList<>());
  private final List<MovieSet.MovieSetMovie> movieSetMovies = new ArrayList<>();

  private MediaScraper                       scraper;
  private MediaSearchResult                  result         = null;
  private MediaMetadata                      metadata       = null;
  private String                             name           = "";
  private String                             posterUrl      = "";
  private String                             fanartUrl      = "";
  private String                             overview       = "";
  private int                                tmdbId         = 0;
  private boolean                            scraped;
  private QueueTask                          queueTask;

  public MovieSetChooserModel(MediaSearchResult result) {
    this.result = result;

    setName(result.getTitle());
    setTmdbId(result.getIdAsInt(result.getProviderId()));
    setPosterUrl(result.getPosterUrl());

    try {
      List<MediaScraper> sets = MediaScraper.getMediaScrapers(ScraperType.MOVIE_SET);
      if (!sets.isEmpty()) {
        scraper = sets.get(0); // just get first
      }
    }
    catch (Exception e) {
      scraper = null;
    }
  }

  /**
   * create the empty search result.
   */
  private MovieSetChooserModel() {
    setName(TmmResourceBundle.getString("chooser.nothingfound"));
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
    firePropertyChange("name", "", name);
  }

  public void setOverview(String overview) {
    this.overview = overview;
    firePropertyChange("overview", "", overview);
  }

  public int getTmdbId() {
    return tmdbId;
  }

  public void setTmdbId(int tmdbId) {
    this.tmdbId = tmdbId;
  }

  public void setPosterUrl(String posterUrl) {
    this.posterUrl = posterUrl;
    firePropertyChange("posterUrl", "", posterUrl);
  }

  public void setFanartUrl(String fanartUrl) {
    this.fanartUrl = fanartUrl;
    firePropertyChange("fanartUrl", "", fanartUrl);
  }

  private void setScraped(boolean newValue) {
    boolean oldValue = scraped;
    scraped = newValue;
    firePropertyChange("scraped", oldValue, newValue);
  }

  public boolean isScraped() {
    return scraped;
  }

  public String getPosterUrl() {
    return posterUrl;
  }

  public String getFanartUrl() {
    return fanartUrl;
  }

  /**
   * Match with existing movies.
   */
  public void matchWithExistingMovies() {
    List<Movie> moviesFromMovieList = MovieModuleManager.getInstance().getMovieList().getMovies();
    for (MovieInSet mis : movies) {
      // try to match via tmdbid
      if (mis.tmdbId > 0) {
        for (Movie movie : moviesFromMovieList) {
          if (movie.getTmdbId() == mis.tmdbId) {
            mis.setMovie(movie);
            break;
          }
        }
      }

      // try to match via imdbid if nothing has been found
      if (mis.getMovie() == null) {
        if (!MediaIdUtil.isValidImdbId(mis.imdbId) && mis.tmdbId > 0) {
          // get imdbid for this tmdbid
          String imdbId = getMovieImdbIdViaTmdbId(mis.tmdbId);
          if (MediaIdUtil.isValidImdbId(imdbId)) {
            mis.imdbId = imdbId;
          }
        }

        if (MediaIdUtil.isValidImdbId(mis.imdbId)) {
          for (Movie movie : moviesFromMovieList) {
            if (mis.imdbId.equals(movie.getImdbId())) {
              mis.setMovie(movie);
              break;
            }
          }
        }
      }
    }
  }

  /**
   * Scrape metadata.
   */
  public void scrapeMetadata() {
    try {
      if (scraper.getMediaProvider() != null) {
        MovieSetSearchAndScrapeOptions options = new MovieSetSearchAndScrapeOptions();
        options.setSearchResult(result);
        options.setTmdbId(result.getIdAsInt(result.getProviderId()));
        options.setLanguage(MovieModuleManager.getInstance().getSettings().getScraperLanguage());

        MediaMetadata info;

        try {
          info = ((IMovieSetMetadataProvider) scraper.getMediaProvider()).getMetadata(options);
        }
        catch (MissingIdException e) {
          LOGGER.warn("Missing IDs for scraping movie set with '{}'", scraper.getId());
          MessageManager.getInstance().pushMessage(new Message(Message.MessageLevel.ERROR, "MovieSetChooser", "scraper.error.missingid"));
          return;
        }
        catch (NothingFoundException e) {
          LOGGER.debug("nothing found");
          return;
        }
        catch (ScrapeException e) {
          LOGGER.error("Could not scrape movie set with '{}' - '{}'", scraper.getId(), e.getMessage());
          MessageManager.getInstance()
              .pushMessage(new Message(Message.MessageLevel.ERROR, "MovieSetChooser", "message.scrape.metadatamoviesetfailed",
                  new String[] { ":", e.getLocalizedMessage() }));
          return;
        }
        catch (Exception e) {
          LOGGER.error("Unforeseen error in movie set scrape", e);
          return;
        }

        if (info != null) {
          this.metadata = info;
          if (!info.getMediaArt(MediaArtworkType.BACKGROUND).isEmpty()) {
            setFanartUrl(info.getMediaArt(MediaArtworkType.BACKGROUND).get(0).getOriginalUrl());
          }

          setName(info.getTitle());
          setOverview(info.getPlot());

          for (MediaMetadata item : info.getSubItems()) {
            MovieInSet movie = new MovieInSet(item.getTitle());
            movie.setYear(item.getYear());
            try {
              movie.setTmdbId(Integer.parseInt(item.getId(MediaMetadata.TMDB).toString()));
            }
            catch (NumberFormatException ignored) {
            }
            if (item.getReleaseDate() != null) {
              movie.setReleaseDate(new SimpleDateFormat("yyyy-MM-dd").format(item.getReleaseDate()));
            }
            movies.add(movie);

            // mix in the dummy movie
            MovieSet.MovieSetMovie movieSetMovie = new MovieSet.MovieSetMovie();
            movieSetMovie.setMetadata(item, new ArrayList<>(Arrays.asList(MovieScraperMetadataConfig.values())), true, false);
            movieSetMovie.setLastScraperId(scraper.getMediaProvider().getId());
            movieSetMovie.setLastScrapeLanguage(options.getLanguage().name());

            // POSTER
            if (!item.getMediaArt(MediaArtworkType.POSTER).isEmpty()) {
              movieSetMovie.setArtworkUrl(item.getMediaArt(MediaArtworkType.POSTER).get(0).getOriginalUrl(), MediaFileType.POSTER);
            }

            // FANART
            if (!item.getMediaArt(MediaArtworkType.BACKGROUND).isEmpty()) {
              movieSetMovie.setArtworkUrl(item.getMediaArt(MediaArtworkType.BACKGROUND).get(0).getOriginalUrl(), MediaFileType.FANART);
            }

            movieSetMovies.add(movieSetMovie);
          }

          Collections.sort(movies);

          // try to match movies
          matchWithExistingMovies();

          setScraped(true);
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("scrapeMedia", e);
      MessageManager.getInstance()
          .pushMessage(new Message(Message.MessageLevel.ERROR, "MovieSetChooser", "message.scrape.threadcrashed",
              new String[] { ":", e.getLocalizedMessage() }));
    }
  }

  public String getOverview() {
    return overview;
  }

  public List<MovieInSet> getMovies() {
    return movies;
  }

  public List<MovieSet.MovieSetMovie> getMovieSetMovies() {
    return movieSetMovies;
  }

  public void startArtworkScrapeTask(MovieSet movieSet, List<MovieSetScraperMetadataConfig> config) {
    startArtworkScrapeTask(movieSet, config, true);
  }

  public void startArtworkScrapeTask(MovieSet movieSet, List<MovieSetScraperMetadataConfig> config, boolean overwrite) {
    addTask(new ArtworkScrapeTask(movieSet, config, overwrite));
  }

  /**
   * Adds a {@link TmmTask} to the internal {@link QueueTask} for deferred sequential execution.
   * <p>
   * All tasks added via this method will be executed in order when {@link #startTasks()} is called.
   * </p>
   *
   * @param task
   *          the task to add to the queue
   */
  public void addTask(TmmTask task) {
    if (queueTask == null) {
      queueTask = new QueueTask(TmmResourceBundle.getString("movieset.scraping"));
    }
    queueTask.addTask(task);
  }

  /**
   * Submits the accumulated queue of tasks to the {@link TmmTaskManager} for background execution.
   * <p>
   * If no tasks have been added via {@link #addTask(TmmTask)}, this method does nothing.
   * </p>
   */
  public void startTasks() {
    if (queueTask != null) {
      TmmTaskManager.getInstance().addUnnamedTask(queueTask);
      queueTask = null;
    }
  }

  /**
   * Queues a background task that assigns matched movies to the given movie set.
   * <p>
   * The task handles removing each movie from its previous set (if any), linking it to the new set, writing its NFO and persisting it to the
   * database. The movie set itself is also saved at the end.
   * </p>
   *
   * @param movieSet
   *          the movie set to assign movies to
   * @param moviesToAssign
   *          the list of {@link MovieInSet} entries whose matched {@link Movie} instances are to be assigned
   */
  public void startAssignMoviesTask(MovieSet movieSet, List<MovieInSet> moviesToAssign) {
    addTask(new AssignMoviesTask(movieSet, moviesToAssign));
  }

  /**
   * Queues a background task that writes the NFO file for the given movie set and persists it to the database.
   * <p>
   * This should be called after artwork URLs have been selected so that they are written into the NFO.
   * </p>
   *
   * @param movieSet
   *          the movie set whose NFO should be rewritten
   */
  public void startWriteNfoTask(MovieSet movieSet) {
    addTask(new WriteNfoTask(movieSet));
  }

  private class ArtworkScrapeTask extends TmmTask {
    private final MovieSet                            movieSetToScrape;
    private final List<MovieSetScraperMetadataConfig> config;
    private final boolean                             overwrite;

    public ArtworkScrapeTask(MovieSet movieSet, List<MovieSetScraperMetadataConfig> config, boolean overwrite) {
      super(TmmResourceBundle.getString("message.scrape.artwork") + " " + movieSet.getTitle(), 0, TaskType.BACKGROUND_TASK);
      this.movieSetToScrape = movieSet;
      this.config = config;
      this.overwrite = overwrite;
    }

    @Override
    protected void doInBackground() {
      if (!scraped) {
        return;
      }

      List<MediaArtwork> artwork = new ArrayList<>();

      ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(MediaType.MOVIE_SET);
      options.setArtworkType(MediaArtwork.MediaArtworkType.ALL);
      options.setMetadata(metadata);
      options.setId(MediaMetadata.IMDB, String.valueOf(metadata.getId(MediaMetadata.IMDB)));
      try {
        options.setTmdbId(Integer.parseInt(String.valueOf(metadata.getId(MediaMetadata.TMDB_SET))));
      }
      catch (Exception e) {
        options.setTmdbId(0);
      }
      options.setLanguage(MovieModuleManager.getInstance().getSettings().getDefaultImageScraperLanguage());
      options.setFanartSize(MovieModuleManager.getInstance().getSettings().getImageFanartSize());
      options.setPosterSize(MovieModuleManager.getInstance().getSettings().getImagePosterSize());

      // scrape providers till one artwork has been found
      for (MediaScraper artworkScraper : MovieModuleManager.getInstance().getMovieList().getDefaultArtworkScrapers()) {
        IMovieArtworkProvider artworkProvider = (IMovieArtworkProvider) artworkScraper.getMediaProvider();
        try {
          artwork.addAll(artworkProvider.getArtwork(options));
        }
        catch (MissingIdException e) {
          LOGGER.info("Missing IDs for scraping movie set artwork of '{}' with '{}'", movieSetToScrape.getTitle(), artworkScraper.getId());
        }
        catch (ScrapeException e) {
          LOGGER.error("Could not scrape movie set artwork of '{}' with '{}' - '{}'", movieSetToScrape.getTitle(), artworkScraper.getId(),
              e.getMessage());
          MessageManager.getInstance()
              .pushMessage(new Message(Message.MessageLevel.ERROR, movieSetToScrape, "message.scrape.moviesetartworkfailed",
                  new String[] { ":", e.getLocalizedMessage() }));
        }
        catch (Error e) {
          LOGGER.error("Unforeseen error in movie artwork scrape for '{}'", movieSetToScrape.getTitle(), e);
        }
      }

      // at last take the poster from the result
      if (StringUtils.isNotBlank(getPosterUrl())) {
        MediaArtwork ma = new MediaArtwork(result.getProviderId(), MediaArtwork.MediaArtworkType.POSTER);
        ma.setOriginalUrl(getPosterUrl());
        ma.setPreviewUrl(getPosterUrl());
        ma.addImageSize(0, 0, getPosterUrl(), 0);
        artwork.add(ma);
      }

      movieSetToScrape.setArtwork(artwork, getArtworkConfigToSet());
    }

    private List<MovieSetScraperMetadataConfig> getArtworkConfigToSet() {
      if (overwrite) {
        return config;
      }

      List<MovieSetScraperMetadataConfig> filteredConfig = new ArrayList<>();
      for (MovieSetScraperMetadataConfig metadataConfig : config) {
        MediaFileType type = switch (metadataConfig) {
          case POSTER -> MediaFileType.POSTER;
          case FANART -> MediaFileType.FANART;
          case BANNER -> MediaFileType.BANNER;
          case CLEARLOGO -> MediaFileType.CLEARLOGO;
          case CLEARART -> MediaFileType.CLEARART;
          case DISCART -> MediaFileType.DISC;
          case THUMB -> MediaFileType.THUMB;
          default -> null;
        };

        if (type == null || !MovieSetArtworkHelper.hasArtwork(movieSetToScrape, type)) {
          filteredConfig.add(metadataConfig);
        }
      }

      return filteredConfig;
    }
  }

  public MediaMetadata getMetadata() {
    return metadata;
  }

  /**
   * The class {@link AssignMoviesTask} assigns matched movies from the chooser result to the movie set in a background thread.
   * <p>
   * This avoids blocking the EDT with file I/O (NFO writes) and database operations while scraping multiple movie sets in a queue.
   * </p>
   */
  private static class AssignMoviesTask extends TmmTask {
    private final MovieSet         movieSetToScrape;
    private final List<MovieInSet> moviesToAssign;

    /**
     * Creates a new {@link AssignMoviesTask}.
     *
     * @param movieSet
     *          the movie set to assign movies to
     * @param moviesToAssign
     *          the list of {@link MovieInSet} entries to process; a defensive copy is taken
     */
    AssignMoviesTask(MovieSet movieSet, List<MovieInSet> moviesToAssign) {
      super(TmmResourceBundle.getString("movieset.movie.assign") + " " + movieSet.getTitle(), 0, TaskType.BACKGROUND_TASK);
      this.movieSetToScrape = movieSet;
      // defensive copy to avoid ConcurrentModificationException on the observable list
      this.moviesToAssign = new ArrayList<>(moviesToAssign);
    }

    @Override
    protected void doInBackground() {
      movieSetToScrape.removeAllMovies();

      for (MovieInSet movieInSet : moviesToAssign) {
        Movie movie = movieInSet.getMovie();
        if (movie == null) {
          continue;
        }

        // check if the found movie already belongs to a different set
        if (movie.getMovieSet() != null) {
          // unassign movie from its current set first
          MovieSet mSet = movie.getMovieSet();
          mSet.removeMovie(movie, true);
        }

        movie.setMovieSet(movieSetToScrape);
        movie.writeNFO();
        movie.saveToDb();
        movieSetToScrape.insertMovie(movie);
      }

      // persist the final state of the movie set
      movieSetToScrape.saveToDb();
    }
  }

  /**
   * The class {@link WriteNfoTask} rewrites the NFO file for a movie set in a background thread.
   * <p>
   * This is used after the user has manually selected artwork URLs so that those URLs are flushed to disk without blocking the EDT.
   * </p>
   */
  private static class WriteNfoTask extends TmmTask {
    private final MovieSet movieSetToScrape;

    /**
     * Creates a new {@link WriteNfoTask}.
     *
     * @param movieSet
     *          the movie set whose NFO should be rewritten
     */
    WriteNfoTask(MovieSet movieSet) {
      super(TmmResourceBundle.getString("movieset.rewritenfo") + " " + movieSet.getTitle(), 0, TaskType.BACKGROUND_TASK);
      this.movieSetToScrape = movieSet;
    }

    @Override
    protected void doInBackground() {
      movieSetToScrape.writeNFO();
      movieSetToScrape.saveToDb();
    }
  }

  public static class MovieInSet extends AbstractModelObject implements Comparable<MovieInSet> {
    private final String name;

    private int          tmdbId      = 0;
    private String       imdbId      = "";
    private String       releaseDate = "";
    private int          year        = 0;
    private Movie        movie       = null;

    public MovieInSet(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public int getYear() {
      return year;
    }

    public int getTmdbId() {
      return tmdbId;
    }

    public String getImdbId() {
      return imdbId;
    }

    public String getReleaseDate() {
      return releaseDate;
    }

    public Movie getMovie() {
      return movie;
    }

    public void setYear(int year) {
      this.year = year;
    }

    public void setTmdbId(int tmdbId) {
      this.tmdbId = tmdbId;
    }

    public void setImdbId(String imdbId) {
      this.imdbId = imdbId;
    }

    public void setReleaseDate(String releaseDate) {
      this.releaseDate = releaseDate;
    }

    public void setMovie(Movie movie) {
      this.movie = movie;
      firePropertyChange("movie", null, movie);
    }

    @Override
    public int compareTo(MovieInSet o) {
      return releaseDate.compareTo(o.releaseDate);
    }
  }
}
