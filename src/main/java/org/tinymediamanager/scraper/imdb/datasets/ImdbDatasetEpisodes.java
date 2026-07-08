package org.tinymediamanager.scraper.imdb.datasets;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;

public class ImdbDatasetEpisodes extends ImdbAbstractDataset {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImdbDatasetEpisodes.class);

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected String getConfiguredDataset() {
    return IMDB_DATASET_TITLE_EPISODE;
  }

  @Override
  protected int getConfiguredCacheDays() {
    return 30;
  }

  @Override
  protected String getConfiguredDbTable() {
    return "imdbEpisodes";
  }

  @Override
  protected void initDataset() {
    Path databaseFile = getDatabaseFile();

    try {
      if (isRefreshRequired(databaseFile)) {
        LOGGER.debug("Preparing IMDB episode dataset...");

        Path extractedFile = downloadAndExtractDataset();
        if (extractedFile != null) {
          LOGGER.debug("Parsing dataset...");

          openWritableDatabase(databaseFile);
          dbMap = mvStore.openMap(dbTable);
          dbMap.clear();

          try (var stream = Files.lines(extractedFile, StandardCharsets.UTF_8)) {
            stream.skip(1).forEach(line -> {
              // EP, show, S, E
              String[] data = line.split("\t");
              List<String> episodes = (List<String>) dbMap.computeIfAbsent(data[1], k -> new ArrayList<>());
              episodes.add(line);
            });
          }

          LOGGER.debug("Copying map to database...");
          mvStore.commit();

          Files.delete(extractedFile);

          LOGGER.debug("Done!");
        }
        else if (Files.exists(databaseFile)) {
          LOGGER.debug("download failed - using old copy");
          openReadOnlyDatabase(databaseFile);
          dbMap = mvStore.openMap(dbTable);
        }
      }
      else {
        LOGGER.debug("Dataset young enough, no need to download...");
        openReadOnlyDatabase(databaseFile);
        dbMap = mvStore.openMap(dbTable);
      }
    }
    catch (Exception e) {
      LOGGER.debug("Error initializing dataset: {} {}", getConfiguredDataset(), e.getMessage());
      Utils.deleteFileSafely(databaseFile);
    }
  }

  /**
   * Returns a List of all known IMDB episode Ids.<br>
   * Might be outdated
   * 
   * @param showId
   * @return empty list or list of episodeIds - but never NULL
   */
  public List<MediaMetadata> getEpisodesByShowId(String showId) {
    init();
    try {
      Object obj = dbMap.get(showId);
      if (obj instanceof List<?> list) {
        if (list.isEmpty()) {
          return Collections.emptyList();
        }

        List<String> epsTsv = (List<String>) list;
        List<MediaMetadata> ret = new ArrayList<>();
        epsTsv.forEach(ep -> {
          // EP, show, S, E
          String[] tsv = ep.split("\t");
          DatasetEpisode data = new DatasetEpisode(tsv[0], tsv[2], tsv[3]);
          MediaMetadata md = new MediaMetadata(MediaMetadata.IMDB);
          md.setId(MediaMetadata.IMDB, data.id);
          MediaEpisodeNumber see = new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, data.s, data.e);
          md.setEpisodeNumber(see);
          md.setTitle("found S" + see.season() + " E" + see.episode() + " - title will be scraped later");
          ret.add(md);
        });
        return ret;
      }
    }
    catch (Exception e) {
      LOGGER.debug("could not read the MVstore - '{}'", e.getMessage());
      shutdown();
      Utils.deleteFileSafely(getDatabaseFile());
    }
    return Collections.emptyList();
  }

  static class DatasetEpisode implements Serializable {
    String id;
    int    s = -1;
    int    e = -1;

    public DatasetEpisode(String episodeId, String seasonNr, String episodeNr) {
      id = episodeId;
      try {
        s = Integer.parseInt(seasonNr); // do not use our utils method - too slow
      }
      catch (NumberFormatException e) {
        // ignored
      }
      try {
        e = Integer.parseInt(episodeNr);
      }
      catch (NumberFormatException e) {
        // ignored
      }
    }
  }
}
