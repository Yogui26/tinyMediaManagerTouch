/*
 * Copyright 2012 - 2025 Manuel Laggner
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
package org.tinymediamanager.scraper.mdblist;

import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.mdblist.entities.MdbIdName;
import org.tinymediamanager.scraper.mdblist.entities.MdbIdTitle;
import org.tinymediamanager.scraper.mdblist.entities.MdbMediaEntity;
import org.tinymediamanager.scraper.mdblist.entities.MdbRating;
import org.tinymediamanager.scraper.mdblist.entities.MdbSearchResult;
import org.tinymediamanager.scraper.mdblist.entities.MdbSearchResults;
import org.tinymediamanager.scraper.util.ListUtils;

import retrofit2.Response;

/**
 * The class {@link MdbListMetadataProvider} is used to offer ratings from MDBList
 * 
 * @author Myron Boyle
 */
abstract class MdbListMetadataProvider implements IMediaProvider {
  public static final String        ID = "mdblist";

  protected final MediaProviderInfo providerInfo;
  protected MdbListController       controller;

  public MdbListMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  protected MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, getSubId(), "mdblist.com",
        "MDBLIst - The Ultimate Dynamic List Manager<br><br>mdblist.com is your go-to tool for creating dynamic, auto-updating movie and show lists tailored to your preferences. Seamlessly integrated with Stremio, it combines the power of multiple rating platforms like IMDb, TMDb, Letterboxd, Rotten Tomatoes, Metacritic, MyAnimeList, and RogerEbert. Whether you’re building a watchlist, tracking ratings, or syncing your library progress, mdblist.com makes it effortless.",
        MdbListMetadataProvider.class.getResource("/org/tinymediamanager/scraper/mdblist.png"));
  }

  @Override
  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(Settings.getInstance().getMdbListApiKey());
  }

  protected synchronized void initAPI() throws ScrapeException {
    if (controller == null) {
      if (!isActive()) {
        throw new ScrapeException(new FeatureNotEnabledException(this));
      }
      try {
        controller = new MdbListController();
        String apiKey = Settings.getInstance().getMdbListApiKey();
        if (apiKey.isBlank()) {
          throw new ScrapeException("apiKey missing - skipping");
        }
        controller.setApiKey(apiKey);
      }
      catch (Exception e) {
        getLogger().error("could not initialize the API: {}", e.getMessage());
        // force re-initialization the next time this will be called
        controller = null;
        throw new ScrapeException(e);
      }
    }
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  protected abstract String getSubId();

  protected abstract Logger getLogger();

  protected SortedSet<MediaSearchResult> searchMdb(MediaSearchAndScrapeOptions options) throws ScrapeException {
    getLogger().debug("search(): '{}'", options);
    initAPI();
    SortedSet<MediaSearchResult> results = new TreeSet<>();

    if (!options.getIds().isEmpty()) {
      try {
        MediaMetadata md = getMetadata(options);
        MediaSearchResult msr = md.toSearchResult(options.getMediaType());
        msr.setScore(1.0f);
        results.add(msr);
        return results;
      }
      catch (Exception e) {
        // if we got an exception, the getMetadataCall by ID did not work
      }
    }

    // so we do a search here
    String query = options.getSearchQuery();
    Response<MdbSearchResults> response = null;
    try {
      response = controller.search(options.getMediaType(), query).execute();
      if (!response.isSuccessful()) {
        String message = "";
        try {
          message = response.errorBody().string();
        }
        catch (IOException e) {
          // ignore
        }
        getLogger().warn("request was not successful: HTTP/{} - {}", response.code(), message);
        throw new HttpException(response.code(), message);
      }
    }
    catch (Exception e) {
      getLogger().error("request was not successful:{}", e);
      throw new ScrapeException(e);
    }

    MdbSearchResults result = response.body();
    if (result == null || result.search == null || result.search.isEmpty()) {
      getLogger().warn("no result found");
      throw new NothingFoundException();
    }

    for (MdbSearchResult res : result.search) {
      MediaSearchResult msr = toSearchResult(res, options.getMediaType());
      msr.calculateScore(options);
      results.add(msr);
    }

    return results;
  }

  private MediaSearchResult toSearchResult(MdbSearchResult mdb, MediaType type) {
    MediaSearchResult sr = new MediaSearchResult(getId(), type);
    for (Map.Entry<String, Object> entry : mdb.ids.entrySet()) {
      String key = entry.getKey().replaceFirst("id$", ""); // searchresult has "id" appended to all IDs
      sr.setId(key, entry.getValue());
    }
    sr.setTitle(mdb.title);
    sr.setYear(mdb.year);
    return sr;
  }

  protected MediaMetadata getMetadata(MediaSearchAndScrapeOptions options) throws ScrapeException {
    return getMetadata(options, false);
  }

  protected MediaMetadata getMetadata(MediaSearchAndScrapeOptions options, boolean keywords) throws ScrapeException {
    getLogger().debug("getMetadata(): '{}'", options);
    initAPI();

    Response<MdbMediaEntity> response = null;
    for (Map.Entry<String, Object> entry : options.getIds().entrySet()) {
      try {
        if (keywords) {
          response = controller.getMediaEntity(entry.getKey(), options.getMediaType(), entry.getValue().toString(), "keyword").execute();
        }
        else {
          response = controller.getMediaEntity(entry.getKey(), options.getMediaType(), entry.getValue().toString()).execute();
        }
        if (!response.isSuccessful()) {
          String message = "";
          try {
            message = response.errorBody().string();
          }
          catch (IOException e) {
            // ignore
          }
          getLogger().debug("request was not successful: HTTP/{} - {}", response.code(), message);
        }
        else {
          break;
        }
      }
      catch (Exception e) {
        getLogger().debug("request was not successful:{}", e);
      }
    }

    if (response == null || response.body() == null || !response.isSuccessful()) {
      throw new NothingFoundException();
    }

    MdbMediaEntity mdb = response.body();
    MediaMetadata md = new MediaMetadata(getId());
    for (Map.Entry<String, Object> entry : mdb.ids.entrySet()) {
      md.setId(entry.getKey(), entry.getValue());
    }
    md.setTitle(mdb.title);
    md.setYear(mdb.year);
    md.setPlot(mdb.description);
    md.setReleaseDate(mdb.released);
    md.setRuntime(mdb.runtime);

    for (MdbRating rating : ListUtils.nullSafe(mdb.ratings)) {
      md.addRating(getTmmRatingFrom(rating));
    }
    for (MdbIdTitle genre : ListUtils.nullSafe(mdb.genres)) {
      MediaGenres g = MediaGenres.getGenre(genre.title);
      md.addGenre(g);
    }
    for (MdbIdName prodComp : ListUtils.nullSafe(mdb.production_companies)) {
      md.addProductionCompany(prodComp.name);
    }
    for (MdbIdName keyword : ListUtils.nullSafe(mdb.keywords)) {
      md.addTag(keyword.name);
    }
    md.addSpokenLanguage(mdb.spspoken_language);
    md.setOriginalLanguage(mdb.language);
    md.addCountry(mdb.country);

    MediaCertification cert = MediaCertification.findCertification(mdb.certification);
    md.addCertification(cert);

    if (mdb.trailer != null) {
      MediaTrailer t = new MediaTrailer();
      t.setUrl(mdb.trailer);
      md.addTrailer(t);
    }

    if (mdb.poster != null) {
      MediaArtwork ma = new MediaArtwork(getId(), MediaArtworkType.POSTER);
      ma.setOriginalUrl(mdb.poster);
      ma.addImageSize(0, 0, mdb.poster, MediaArtwork.PosterSizes.LARGE.ordinal());
      md.addMediaArt(ma);
    }

    if (mdb.backdrop != null) {
      MediaArtwork ma = new MediaArtwork(getId(), MediaArtworkType.BACKGROUND);
      ma.setOriginalUrl(mdb.backdrop);
      ma.addImageSize(0, 0, mdb.backdrop, MediaArtwork.FanartSizes.LARGE.ordinal());
      md.addMediaArt(ma);
    }

    return md;
  }

  protected MediaRating getTmmRatingFrom(MdbRating mdb) {
    MediaRating rating = null;
    if (mdb.source == null || (mdb.value == 0f && !mdb.source.equals(MediaMetadata.ROGER_EBERT))) {
      return rating;
    }
    rating = new MediaRating(mdb.source, mdb.value, mdb.votes);
    switch (mdb.source) {
      case MediaMetadata.LETTERBOXD: {
        rating.setMaxValue(5);
        break;
      }

      case MediaMetadata.ROGER_EBERT: {
        rating.setMaxValue(4); // yes, 4
        break;
      }
    }
    return rating;
  }
}
