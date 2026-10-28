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
package org.tinymediamanager.scraper.imdb;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.imdb.datasets.ImdbDatasetEpisodes;
import org.tinymediamanager.scraper.imdb.datasets.ImdbDatasetTitleBasics;
import org.tinymediamanager.scraper.imdb.entities.ImdbEpisodeList;
import org.tinymediamanager.scraper.imdb.entities.ImdbIdValueType;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.JsonUtils;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The class ImdbTvShowParser is used to parse TV show site of imdb.com
 *
 * @author Manuel Laggner
 */
class ImdbTvShowParser extends ImdbParser {
  private static final Logger                                LOGGER                 = LoggerFactory.getLogger(ImdbTvShowParser.class);
  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP = new CacheMap<>(60, 10);

  ImdbTvShowParser(IMediaProvider metadataProvider, ExecutorService executor) {
    super(metadataProvider, MediaType.TV_SHOW, executor);
  }

  @Override
  protected MediaMetadata getMetadata(MediaSearchAndScrapeOptions options) throws ScrapeException {
    return switch (options.getMediaType()) {
      case TV_SHOW -> getTvShowMetadata((TvShowSearchAndScrapeOptions) options);
      case TV_EPISODE -> getEpisodeMetadata((TvShowEpisodeSearchAndScrapeOptions) options);
      default -> new MediaMetadata(ImdbMetadataProvider.ID);
    };
  }

  MediaMetadata getTvShowMetadata(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    MediaMetadata md = new MediaMetadata(ImdbMetadataProvider.ID);
    md.setScrapeOptions(options);

    String imdbId = "";

    // imdbId from searchResult
    if (options.getSearchResult() != null) {
      imdbId = options.getSearchResult().getIMDBId();
    }

    // imdbid from scraper option
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      imdbId = options.getImdbId();
    }

    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.debug("not possible to scrape from IMDB - no imdbId found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    LOGGER.debug("IMDB: getMetadata(imdbId): {}", imdbId);
    md.setId(ImdbMetadataProvider.ID, imdbId);
    md.addEpisodeGroup(MediaEpisodeGroup.DEFAULT_AIRED);

    // default workers which always run
    Document doc = null;
    boolean json = false;
    Callable<Document> worker = createImdbWorker(constructUrl("title/", imdbId), options.getLanguage().getLanguage(),
        options.getCertificationCountry().getAlpha2());
    Future<Document> futureDetail = executor.submit(worker);

    // check if the detail page is scrapable
    try {
      doc = futureDetail.get();
      parseDetailPageJson(doc, options, md);
      json = true;
    }
    catch (ScrapeException e) {
      throw e;
    }
    catch (ExecutionException e) {
      // any exception thrown in the future worker is wrapped by this one
      if (e.getCause() instanceof HttpException httpException && httpException.getStatusCode() == 202) {
        // just continue - maybe the old logic works
        json = false;
      }
      else {
        throw new ScrapeException(e.getCause());
      }
    }
    catch (Exception e) {
      LOGGER.debug("Could not get detailpage for id '{}' - '{}'", imdbId, e.getMessage());
      throw new ScrapeException(e);
    }

    // start other workers afterward
    worker = createImdbWorker(constructUrl("title/", imdbId, decode("L3JlZmVyZW5jZQ==")), options.getLanguage().getLanguage(),
        options.getCertificationCountry().getAlpha2());
    Future<Document> futureReference = executor.submit(worker);

    // we must parse this as fixed language, since the IDs seem not to be fixated yet...?
    worker = createImdbWorker(constructUrl("title/", imdbId, decode("L2Z1bGxjcmVkaXRz")), "en", "US");
    Future<Document> futureCredits = executor.submit(worker);

    Future<Document> futureKeywords = null;
    if (isScrapeKeywordsPage() && getMaxKeywordCount() > 5) {
      worker = createImdbWorker(constructUrl("title/", imdbId, decode("L2tleXdvcmRz")), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2());
      futureKeywords = executor.submit(worker);
    }

    Future<Document> futureReleaseInfo = null;
    if (!isScrapeLocalReleaseDate()) {
      worker = createImdbWorker(constructUrl("title/", imdbId, decode("L3JlbGVhc2VpbmZv")), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2());
      futureReleaseInfo = executor.submit(worker);
    }

    if (json) {
      // detail page worked, mix-in missing
      try {
        MediaMetadata md2 = new MediaMetadata(ImdbMetadataProvider.ID);
        doc = futureReference.get();
        if (doc != null) {
          parseReferencePage(doc, options, md2);
          // md.setTagline(md2.getTagline()); // looks like an English tagline from the first season is returned in the JSON
          md.setCastMembers(md2.getCastMembers()); // overwrite all
          md.setTop250(md2.getTop250());
          md2.getCertifications().forEach(md::addCertification); // reference page has more certifications
        }

        // no cast? parse full credits
        if (md.getCastMembers().isEmpty()) {
          doc = futureCredits.get();
          if (doc != null) {
            parseFullcreditsPage(doc, options, md);
          }
        }

        if (isScrapeKeywordsPage() && getMaxKeywordCount() > 5) {
          if (futureKeywords != null) {
            doc = futureKeywords.get();
            if (doc != null) {
              parseKeywordsPage(doc, options, md2);
              md.setTags(md2.getTags());// overwrite all
            }
          }
        }

        // if we want to scrape NOT the local release date, we take the FIRST from releaseinfo page
        if (!isScrapeLocalReleaseDate()) {
          if (futureReleaseInfo != null) {
            doc = futureReleaseInfo.get();
            if (doc != null) {
              parseReleaseinfoPageJson(doc, options, md2);
              md.setReleaseDate(md2.getReleaseDate());
            }
          }
        }

      }
      catch (Exception e) {
        LOGGER.debug("Could not parse page: {}", e.getMessage());
      }
    }
    else {
      // fallback old style, when json parsing was not ok
      Future<Document> futurePlotsummary;
      worker = createImdbWorker(constructUrl("title/", imdbId, decode("L3Bsb3RzdW1tYXJ5")), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2());
      futurePlotsummary = executor.submit(worker);

      Future<Document> futureReleaseinfo;
      worker = createImdbWorker(constructUrl("title/", imdbId, decode("L3JlbGVhc2VpbmZv")), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2());
      futureReleaseinfo = executor.submit(worker);

      try {
        // parse reference - this must not fail
        doc = futureReference.get();
        if (doc != null) {
          parseReferencePage(doc, options, md);
        }
      }
      catch (ExecutionException e) {
        throw new ScrapeException(e.getCause());
      }
      catch (Exception e) {
        throw new ScrapeException(e);
      }

      try {
        // parse others - this may fail
        doc = futurePlotsummary.get();
        if (doc != null) {
          parsePlotsummaryPage(doc, options, md);
        }

        // get the release info page
        Document releaseinfoDoc = futureReleaseinfo.get();
        if (releaseinfoDoc != null) {
          // get the date from the releaseinfo page
          parseReleaseinfoPage(releaseinfoDoc, options, md);
        }

        if (futureKeywords != null) {
          doc = futureKeywords.get();
          if (doc != null) {
            parseKeywordsPage(doc, options, md);
          }
        }
      }
      catch (ExecutionException e) {
        // just log
        LOGGER.debug("problem while scraping: {}", e.getCause().getMessage());
      }
      catch (Exception e) {
        LOGGER.debug("problem while scraping: {}", e.getMessage());
      }

      if (md.getIds().isEmpty() || StringUtils.isBlank(md.getTitle())) {
        LOGGER.debug("nothing found");
        throw new NothingFoundException();
      }
    }

    // populate id
    md.setId(ImdbMetadataProvider.ID, imdbId);

    return md;
  }

  MediaMetadata getEpisodeMetadata(TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeMetadata(): {}", options);

    MediaMetadata md = new MediaMetadata(ImdbMetadataProvider.ID);
    md.setScrapeOptions(options);
    String showId = "" + options.getTvShowIds().get(MediaMetadata.IMDB);

    String episodeId = options.getIdAsString(MediaMetadata.IMDB);
    if (!MediaIdUtil.isValidImdbId(episodeId)) {
      episodeId = "";
    }

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    if ((seasonNr == -1 || episodeNr == -1) && StringUtils.isBlank(episodeId)) {
      throw new MissingIdException(MediaMetadata.EPISODE_NR, MediaMetadata.SEASON_NR);
    }
    // we want this, we should set this (in case of json error)
    md.setEpisodeNumber(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, seasonNr, episodeNr));

    // first get the base episode metadata which can be gathered via getEpisodeList()
    // only if we get a S/E number
    MediaMetadata wantedEpisode = null;
    if (episodeId.isEmpty() && seasonNr >= 0 && episodeNr > 0) {
      if (!MediaIdUtil.isValidImdbId(showId)) {
        LOGGER.debug("not possible to scrape from IMDB - no imdbId found");
        throw new MissingIdException(MediaMetadata.IMDB);
      }

      List<MediaMetadata> episodes = getEpisodeList(options.createTvShowSearchAndScrapeOptions());

      // search by ID
      if (StringUtils.isNotBlank(episodeId)) {
        for (MediaMetadata episode : episodes) {
          if (episodeId.equals(episode.getId(MediaMetadata.IMDB))) {
            wantedEpisode = episode;
            break;
          }
        }
      }

      // search by S/E
      if (wantedEpisode == null) {
        for (MediaMetadata episode : episodes) {
          MediaEpisodeNumber episodeNumber = episode.getEpisodeNumber(MediaEpisodeGroup.EpisodeGroupType.AIRED);

          if (episodeNumber != null && episodeNumber.season() == seasonNr && episodeNumber.episode() == episodeNr) {
            // search via season/episode number
            wantedEpisode = episode;
            break;
          }
        }
      }
    }

    // we did not find the episode; return
    if (wantedEpisode == null && StringUtils.isBlank(episodeId)) {
      LOGGER.debug("episode not found");
      throw new NothingFoundException();
    }

    // match via episodelist found
    if (wantedEpisode != null && wantedEpisode.getId(ImdbMetadataProvider.ID) instanceof String) {
      episodeId = (String) wantedEpisode.getId(ImdbMetadataProvider.ID);
      md.setEpisodeNumbers(wantedEpisode.getEpisodeNumbers());
      md.setTitle(wantedEpisode.getTitle());
      md.setPlot(wantedEpisode.getPlot());
      md.setRatings(wantedEpisode.getRatings());
      md.setReleaseDate(wantedEpisode.getReleaseDate());
      md.setRuntime(wantedEpisode.getRuntime());
    }

    // and finally the cast which needed to be fetched from the reference page (or json detail page)
    if (MediaIdUtil.isValidImdbId(episodeId)) {
      md.setId(ImdbMetadataProvider.ID, episodeId);

      // default workers which always run
      Document doc = null;
      boolean json = false;
      Callable<Document> worker = createImdbWorker(constructUrl("title/", episodeId), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2());
      Future<Document> futureDetail = executor.submit(worker);

      worker = createImdbWorker(constructUrl("title/", episodeId, decode("L3JlZmVyZW5jZQ==")), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2());
      Future<Document> futureReference = executor.submit(worker);

      // we must parse this as fixed language, since the IDs seem not to be fixated yet...?
      worker = createImdbWorker(constructUrl("title/", episodeId, decode("L2Z1bGxjcmVkaXRz")), "en", "US");
      Future<Document> futureCredits = executor.submit(worker);

      Future<Document> futureKeywords = null;
      if (isScrapeKeywordsPage() && getMaxKeywordCount() > 5) {
        worker = createImdbWorker(constructUrl("title/", episodeId, decode("L2tleXdvcmRz")), options.getLanguage().getLanguage(),
            options.getCertificationCountry().getAlpha2());
        futureKeywords = executor.submit(worker);
      }

      Future<Document> futureReleaseInfo = null;
      if (!isScrapeLocalReleaseDate()) {
        worker = createImdbWorker(constructUrl("title/", episodeId, decode("L3JlbGVhc2VpbmZv")), options.getLanguage().getLanguage(),
            options.getCertificationCountry().getAlpha2());
        futureReleaseInfo = executor.submit(worker);
      }

      try {
        doc = futureDetail.get();
        parseDetailPageJson(doc, options, md);
        json = true;
      }
      catch (ExecutionException e) {
        // any exception thrown in the future worker is wrapped by this one
        if (e.getCause() instanceof HttpException httpException && httpException.getStatusCode() == 202) {
          // just continue - maybe the old logic works
          json = false;
        }
        else {
          throw new ScrapeException(e.getCause());
        }
      }
      catch (ScrapeException e) {
        throw e;
      }
      catch (Exception e) {
        throw new ScrapeException(e);
      }

      if (json) {
        // detail page worked, mix-in missing
        try {
          MediaMetadata md2 = new MediaMetadata(ImdbMetadataProvider.ID);
          doc = futureReference.get();
          if (doc != null) {
            parseReferencePage(doc, options, md2);
            md.setTagline(md2.getTagline());
            md.setCastMembers(md2.getCastMembers()); // overwrite all
            md.setTop250(md2.getTop250());
            md2.getCertifications().forEach(md::addCertification); // reference page has more certifications
          }

          // no cast? parse full credits
          if (md.getCastMembers().isEmpty()) {
            doc = futureCredits.get();
            if (doc != null) {
              parseFullcreditsPage(doc, options, md);
            }
          }

          if (isScrapeKeywordsPage() && getMaxKeywordCount() > 5) {
            if (futureKeywords != null) {
              doc = futureKeywords.get();
              if (doc != null) {
                parseKeywordsPage(doc, options, md2);
                md.setTags(md2.getTags());// overwrite all
              }
            }
          }

          // if we want to scrape NOT the local release date, we take the FIRST from releaseinfo page
          if (!isScrapeLocalReleaseDate()) {
            if (futureReleaseInfo != null) {
              doc = futureReleaseInfo.get();
              if (doc != null) {
                parseReleaseinfoPageJson(doc, options, md2);
                md.setReleaseDate(md2.getReleaseDate());
              }
            }
          }
        }
        catch (Exception e) {
          LOGGER.debug("Could not parse page: {}", e.getMessage());
        }
      }
      else {
        try {
          // parse reference - this must not fail
          Document docReference = futureReference.get();
          if (docReference != null) {
            parseEpisodeReference(docReference, md, episodeId);
          }
        }
        catch (ExecutionException e) {
          throw new ScrapeException(e.getCause());
        }
        catch (Exception e) {
          throw new ScrapeException(e);
        }

        try {
          // parse others - this may fail
          if (futureKeywords != null) {
            Document docKeywords = futureKeywords.get();
            if (docKeywords != null) {
              parseKeywordsPage(docKeywords, options, md);
            }
          }
        }
        catch (ExecutionException e) {
          // just log
          LOGGER.debug("problem while scraping: {}", e.getCause().getMessage());
        }
        catch (Exception e) {
          LOGGER.debug("problem while scraping: {}", e.getMessage());
        }

        if (md.getIds().isEmpty() || StringUtils.isBlank(md.getTitle())) {
          LOGGER.debug("nothing found");
          throw new NothingFoundException();
        }
      }
    }

    return md;
  }

  List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeList(): {}", options);

    // parse the episodes from the ratings overview page (e.g.
    // https://www.imdb.com/title/tt0491738/episodes )
    String imdbId = options.getImdbId();
    if (StringUtils.isBlank(imdbId)) {
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    // look in the cache map if there is an entry
    List<MediaMetadata> episodes = EPISODE_LIST_CACHE_MAP.get(imdbId + "_" + options.getLanguage().getLanguage());
    if (ListUtils.isNotEmpty(episodes)) {
      // cache hit!
      return episodes;
    }

    episodes = new ArrayList<>();

    // NEW WORKFLOW:
    // 1.) use dataset for getting them fast, and also for more-than-fifty-per-season
    Map<String, MediaMetadata> datasetMetadata = new HashMap<>();
    if (isUsingLocalDatasets()) {
      ImdbDatasetEpisodes de = new ImdbDatasetEpisodes();
      List<MediaMetadata> datasetEPs = de.getEpisodesByShowId(imdbId);

      // dataset EPs have no title - but we might need one for EP search/groups
      ImdbDatasetTitleBasics basic = new ImdbDatasetTitleBasics();
      for (MediaMetadata md : datasetEPs) {
        String eptitle = basic.getTitleById(md.getIdAsString(MediaMetadata.IMDB));
        if (!eptitle.isEmpty()) {
          md.setTitle(eptitle);
        }
      }

      if (datasetEPs != null && datasetEPs.size() > 0) {
        datasetEPs.stream().forEach(ep -> datasetMetadata.put(ep.getIdAsString(MediaMetadata.IMDB), ep));
        datasetEPs.clear();
        episodes.addAll(datasetMetadata.values());
        // id, s, ep, title - we have the minimum; lets return
        return episodes;
      }
    }

    List<Integer> availableSeasons = new ArrayList<>();
    List<Integer> availableYears = new ArrayList<>();

    // 2.) get episodes amount from show DETAIL page )
    MediaMetadata show = options.getMetadata();
    if (show != null) {
      // 2a.) either from a previously scraped show
      if (show.getExtraData("seasons") != null) {
        availableSeasons.addAll((List<Integer>) show.getExtraData("seasons"));
        Collections.sort(availableSeasons);
      }
      if (show.getExtraData("years") != null) {
        availableYears.addAll((List<Integer>) show.getExtraData("years"));
        Collections.sort(availableYears);
      }
    }

    // 2b.) or - oldstyle - from webpage
    if (availableSeasons.isEmpty() && availableYears.isEmpty()) {
      try {
        Callable<Document> worker = createImdbWorker(constructUrl("/title/", imdbId, "/episodes"), options.getLanguage().getLanguage(),
            options.getCertificationCountry().getAlpha2());
        Document doc = worker.call();
        if (doc != null) {
          ImdbEpisodeList epList = parseEpisodeListJSON(doc);
          if (epList != null) {
            // JSON parsing worked
            episodes.addAll(epList.getEpisodes());
            for (ImdbIdValueType season : ListUtils.nullSafe(epList.seasons)) {
              availableSeasons.add(MetadataUtil.parseInt(season.value));
            }
          }

          // no results via JSON? use old style...
          if (availableSeasons.isEmpty() || episodes.isEmpty()) {
            parseEpisodeList(1, episodes, doc);
            // get the other seasons out of the select option
            Element select = doc.getElementById("bySeason");
            if (select != null) {
              for (Element option : select.getElementsByTag("option")) {
                String value = option.attr("value");
                if (StringUtils.isNotBlank(value)) {
                  availableSeasons.add(MetadataUtil.parseInt(value));
                }
              }
            }
          }
        }
      }
      catch (InterruptedException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (ScrapeException e) {
        throw e;
      }
      catch (Exception e) {
        LOGGER.debug("problem scraping: {}", e.getMessage());
        throw new ScrapeException(e);
      }
    }

    // 3a.) parse every season
    Document doc = null;
    for (int season : availableSeasons) {
      if (season < 0) {
        LOGGER.debug("could not parse season number - {}", season);
        continue;
      }

      try {
        Callable<Document> worker = createImdbWorker(constructUrl("/title/", imdbId, "/episodes?season=" + season),
            options.getLanguage().getLanguage(), options.getCertificationCountry().getAlpha2());
        doc = worker.call();
        ImdbEpisodeList epList = parseEpisodeListJSON(doc);
        if (epList != null && !epList.getEpisodes().isEmpty()) {
          episodes.addAll(epList.getEpisodes());
        }
        else {
          // if the given season number and the parsed one does not match, break here
          if (!parseEpisodeList(season, episodes, doc)) {
            break;
          }
        }
      }
      catch (InterruptedException | InterruptedIOException e) {
        break; // pressed 'abort' while scraping - no need to fetch the rest!
      }
      catch (Exception e) {
        // ignore?
      }
    }

    // 3b.) if there were no season (like tt13158394) do the same with years
    if (episodes.isEmpty() && !availableYears.isEmpty()) {
      for (int year : availableYears) {
        if (year < 0) {
          LOGGER.debug("could not parse season number - {}", year);
          continue;
        }

        try {
          Callable<Document> worker = createImdbWorker(constructUrl("/title/", imdbId, "/episodes?year=" + year), options.getLanguage().getLanguage(),
              options.getCertificationCountry().getAlpha2());
          doc = worker.call();
          ImdbEpisodeList epList = parseEpisodeListJSON(doc);
          if (epList != null && !epList.getEpisodes().isEmpty()) {
            episodes.addAll(epList.getEpisodes());
          }
          else {
            // if the given season number and the parsed one does not match, break here
            if (!parseEpisodeList(year, episodes, doc)) {
              break;
            }
          }
        }
        catch (InterruptedException | InterruptedIOException e) {
          break; // pressed 'abort' while scraping - no need to fetch the rest!
        }
        catch (Exception e) {
          // ignore?
        }
      }
    }

    // cache for further fast access
    if (!episodes.isEmpty()) {
      // 4. since IMDB lists only first 50 EPs per season, add all the remaining ones from dataset
      // remove all scraped EPs from dataset cache
      for (MediaMetadata ep : episodes) {
        datasetMetadata.remove(ep.getIdAsString(MediaMetadata.IMDB));
      }
      // add few remainders
      episodes.addAll(datasetMetadata.values());

      EPISODE_LIST_CACHE_MAP.put(imdbId + "_" + options.getLanguage().getLanguage(), episodes);
    }

    return episodes;
  }

  protected ImdbEpisodeList parseEpisodeListJSON(Document doc) {
    try {
      Element jsonNode = doc.getElementById("__NEXT_DATA__");
      if (jsonNode == null) {
        return null;
      }
      String json = jsonNode.data();
      // System.out.println(json);
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(json);

      JsonNode epListNode = JsonUtils.at(node, "/props/pageProps/contentData/section");
      ImdbEpisodeList epList = JsonUtils.parseObject(mapper, epListNode, ImdbEpisodeList.class);
      return epList;
    }
    catch (Exception e) {
      LOGGER.debug("Error parsing JSON: '{}'", e.getMessage(), e);
    }
    return null;
  }

  protected boolean parseEpisodeList(int season, List<MediaMetadata> episodes, Document doc) {
    Pattern unknownPattern = Pattern.compile("Unknown");
    Pattern seasonEpisodePattern = Pattern.compile("S([0-9]*), Ep([0-9]*)");
    int specialEpisodeCounter = 0;

    // parse episodes
    Elements tables = doc.getElementsByClass("eplist");
    if (tables.isEmpty()) {
      // no episodes here? break
      return false;
    }

    for (Element table : tables) {
      Elements rows = table.getElementsByClass("list_item");
      for (Element row : rows) {
        Matcher matcher = season <= 0 ? unknownPattern.matcher(row.text()) : seasonEpisodePattern.matcher(row.text());
        if (matcher.find() && (season <= 0 || matcher.groupCount() >= 2)) {
          try {
            // we found a row containing episode data
            MediaMetadata ep = new MediaMetadata(ImdbMetadataProvider.ID);

            // parse season and ep number
            if (season <= 0) {
              ep.setEpisodeNumber(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 0, ++specialEpisodeCounter));
            }
            else {
              int s = Integer.parseInt(matcher.group(1));
              int e = Integer.parseInt(matcher.group(2));

              // check if we have still valid data
              if (season != s) {
                return false;
              }

              ep.setEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, s, e);
            }

            // get ep title and id
            Elements anchors = row.getElementsByAttributeValueStarting("href", "/title/tt");
            for (Element anchor : anchors) {
              if ("name".equals(anchor.attr("itemprop"))) {
                ep.setTitle(anchor.text());
                break;
              }
            }

            String id = "";
            Matcher idMatcher = IMDB_ID_PATTERN.matcher(anchors.get(0).attr("href"));
            while (idMatcher.find()) {
              if (idMatcher.group(1) != null) {
                id = idMatcher.group(1);
              }
            }

            if (StringUtils.isNotBlank(id)) {
              ep.setId(ImdbMetadataProvider.ID, id);
            }

            // plot
            Element plot = row.getElementsByClass("item_description").first();
            if (plot != null) {
              ep.setPlot(plot.ownText());
            }

            // rating and rating count
            Element ratingElement = row.getElementsByClass("ipl-rating-star__rating").first();
            if (ratingElement != null) {
              String ratingAsString = ratingElement.ownText().replace(",", ".");

              Element votesElement = row.getElementsByClass("ipl-rating-star__total-votes").first();
              if (votesElement != null) {
                String countAsString = votesElement.ownText().replaceAll("[.,()]", "").strip();
                try {
                  MediaRating rating = new MediaRating(ImdbMetadataProvider.ID);
                  rating.setRating(Float.parseFloat(ratingAsString));
                  rating.setVotes(MetadataUtil.parseInt(countAsString));
                  ep.addRating(rating);
                }
                catch (Exception e) {
                  LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
                }
              }
            }

            // release date
            Element releaseDate = row.getElementsByClass("airdate").first();
            if (releaseDate != null) {
              ep.setReleaseDate(parseDate(releaseDate.ownText()));
            }

            // poster
            Element image = row.getElementsByTag("img").first();
            if (image != null) {
              String posterUrl = image.attr("src");
              String thumb = posterUrl; // unmodified, small
              posterUrl = posterUrl.replaceAll("UX[0-9]{2,4}_", "");
              posterUrl = posterUrl.replaceAll("UY[0-9]{2,4}_", "");
              posterUrl = posterUrl.replaceAll("CR[0-9]{1,3},[0-9]{1,3},[0-9]{1,3},[0-9]{1,3}_", "");

              if (StringUtils.isNotBlank(posterUrl)) {
                MediaArtwork ma = new MediaArtwork(ImdbMetadataProvider.ID, THUMB);
                ma.setPreviewUrl(thumb);
                ma.setOriginalUrl(posterUrl);
                ep.addMediaArt(ma);
              }
            }

            episodes.add(ep);
          }
          catch (Exception e) {
            LOGGER.debug("failed parsing: {} for ep data - {}", row.text(), e.getMessage());
          }
        }
      }
    }
    return true;
  }

  protected void parseEpisodeReference(Document doc, MediaMetadata md, String imdbId) {
    // title (h3 itemprop=name)
    Element header = doc.getElementsByClass("titlereference-header").first();
    if (header != null) {
      Element year = header.getElementsByClass("titlereference-title-year").first();
      if (year != null) {
        String episodeTitle = cleanString(year.parent().ownText());
        if (StringUtils.isNotBlank(episodeTitle)) {
          md.setTitle(episodeTitle);
        }
      }
    }

    // title: fallback to title from meta
    if (StringUtils.isBlank(md.getTitle())) {
      Element title = doc.getElementsByAttributeValue("name", "title").first();
      if (title != null) {
        String episodeTitle = cleanString(title.attr("content"));

        int yearStart = episodeTitle.lastIndexOf('(');
        if (yearStart > 0) {
          episodeTitle = episodeTitle.substring(0, yearStart - 1).strip();
          md.setTitle(episodeTitle);
        }
      }
    }

    // plot
    // class titlereference-section-overview -> first div without class
    Element titlereference = doc.getElementsByClass("titlereference-section-overview").first();
    if (titlereference != null) {
      for (Element child : titlereference.children()) {
        if ("div".equals(child.tagName()) && child.classNames().isEmpty()) {
          String plot = child.text();
          if (StringUtils.isBlank(md.getPlot()) && StringUtils.isNotBlank(plot)) {
            md.setPlot(plot);
            break;
          }
        }
      }
    }

    // releasedate
    Element releaseDateElement = doc.getElementsByAttributeValue("href", "/title/" + imdbId + "/releaseinfo").first();
    if (releaseDateElement != null) {
      String releaseDateText = releaseDateElement.ownText();
      int startOfCountry = releaseDateText.indexOf('(');
      if (startOfCountry > 0) {
        releaseDateText = releaseDateText.substring(0, startOfCountry - 1).strip();
      }
      md.setReleaseDate(parseDate(releaseDateText));
    }

    // poster
    Element poster = doc.getElementsByAttributeValue("property", "og:image").first();
    if (poster != null) {
      String posterUrl = poster.attr("content");

      int fileStart = posterUrl.lastIndexOf('/');
      if (fileStart > 0) {
        int parameterStart = posterUrl.indexOf('_', fileStart);
        if (parameterStart > 0) {
          int startOfExtension = posterUrl.lastIndexOf('.');
          if (startOfExtension > parameterStart) {
            posterUrl = posterUrl.substring(0, parameterStart) + posterUrl.substring(startOfExtension);
          }
        }
      }
      processMediaArt(md, THUMB, posterUrl);
    }

    // rating and rating count
    Element ratingElement = doc.getElementsByClass("ipl-rating-star__rating").first();
    if (ratingElement != null) {
      String ratingAsString = ratingElement.ownText().replace(",", ".");
      Element votesElement = doc.getElementsByClass("ipl-rating-star__total-votes").first();
      if (votesElement != null) {
        String countAsString = votesElement.ownText().replaceAll("[.,()]", "").strip();
        try {
          MediaRating rating = new MediaRating(MediaMetadata.IMDB);
          rating.setRating(Float.parseFloat(ratingAsString));
          rating.setVotes(MetadataUtil.parseInt(countAsString));
          md.addRating(rating);
        }
        catch (Exception e) {
          LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
        }
      }
    }

    // director
    Element directorsElement = doc.getElementById("directors");
    while (directorsElement != null && !"header".equals(directorsElement.tag().getName())) {
      directorsElement = directorsElement.parent();
    }
    if (directorsElement != null) {
      directorsElement = directorsElement.nextElementSibling();
    }
    if (directorsElement != null) {
      for (Element directorElement : directorsElement.getElementsByClass("name")) {
        String director = directorElement.text().strip();

        Person cm = new Person(Person.Type.DIRECTOR, director);
        // profile path
        Element anchor = directorElement.getElementsByAttributeValueStarting("href", "/name/").first();
        if (anchor != null) {
          Matcher matcher = PERSON_ID_PATTERN.matcher(anchor.attr("href"));
          if (matcher.find()) {
            if (matcher.group(0) != null) {
              cm.setProfileUrl("https://www.imdb.com" + matcher.group(0));
            }
            if (matcher.group(1) != null) {
              cm.setId(ImdbMetadataProvider.ID, matcher.group(1));
            }
          }
        }
        md.addCastMember(cm);
      }
    }

    // actors
    Element castTableElement = doc.getElementsByClass("cast_list").first();
    if (castTableElement != null) {
      Elements castListLabel = castTableElement.getElementsByClass("castlist_label");
      Elements tr = castTableElement.getElementsByTag("tr");
      for (Element row : tr) {
        // check if we're at the uncredited cast members
        if (!isScrapeUncreditedActors() && castListLabel.size() > 1 && row.children().contains(castListLabel.get(1))) {
          break;
        }

        Person cm = parseCastMember(row);
        if (cm != null && StringUtils.isNotEmpty(cm.getName())) {
          cm.setType(ACTOR);
          md.addCastMember(cm);
        }
      }
    }

    // writers
    Element writersElement = doc.getElementById("writers");
    while (writersElement != null && !"header".equals(writersElement.tag().getName())) {
      writersElement = writersElement.parent();
    }
    if (writersElement != null) {
      writersElement = writersElement.nextElementSibling();
    }
    if (writersElement != null) {
      Elements writersElements = writersElement.getElementsByAttributeValueStarting("href", "/name/");

      for (Element writerElement : writersElements) {
        String writer = cleanString(writerElement.ownText());
        Person cm = new Person(WRITER, writer);
        // profile path
        Element anchor = writerElement.getElementsByAttributeValueStarting("href", "/name/").first();
        if (anchor != null) {
          Matcher matcher = PERSON_ID_PATTERN.matcher(anchor.attr("href"));
          if (matcher.find()) {
            if (matcher.group(0) != null) {
              cm.setProfileUrl("https://www.imdb.com" + matcher.group(0));
            }
            if (matcher.group(1) != null) {
              cm.setId(ImdbMetadataProvider.ID, matcher.group(1));
            }
          }
        }
        md.addCastMember(cm);
      }
    }
  }

  public List<MediaArtwork> getTvShowArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {
    String imdbId = options.getImdbId();

    // imdbid via tmdbid
    if (!MediaIdUtil.isValidImdbId(imdbId) && options.getTmdbId() > 0) {
      imdbId = MediaIdUtil.getTvShowImdbIdViaTmdbId(options.getTmdbId());
    }

    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      return Collections.emptyList();
    }

    // just get the MediaMetadata via normal scrape and pick the poster from the result
    try {
      if (options.getMediaType() == MediaType.TV_EPISODE) {
        TvShowEpisodeSearchAndScrapeOptions op = new TvShowEpisodeSearchAndScrapeOptions();
        op.setDataFromOtherOptions(options);
        if (options.getIds().get(MediaMetadata.TVSHOW_IDS) instanceof Map) {
          Map<String, Object> tvShowIds = (Map<String, Object>) options.getIds().get(MediaMetadata.TVSHOW_IDS);
          op.setTvShowIds(tvShowIds);
        }
        // Episode gets its one and only artwork via MetaData call
        List<MediaArtwork> artworks = getMetadata(op).getMediaArt(options.getArtworkType());
        return artworks;
      }
      else {
        // whereas show artwork has i new dedicated method
        List<MediaArtwork> artworks = getMediaArt(options);
        if (options.getArtworkType() == MediaArtworkType.ALL) {
          return artworks;
        }
        return artworks.stream().filter(ma -> ma.getType() == options.getArtworkType()).toList();
      }
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }
  }

  public Map<String, Integer> getTvShowTop250() throws ScrapeException {
    return parseTop250("/chart/toptv/");
  }
}
