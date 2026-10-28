package org.tinymediamanager.scraper.imdb.datasets;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Utils;

public class ImdbDatasetTitleBasics extends ImdbAbstractDataset {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImdbDatasetTitleBasics.class);

  @Override
  protected String getConfiguredDataset() {
    return IMDB_DATASET_TITLE_BASICS;
  }

  @Override
  protected int getConfiguredCacheDays() {
    return 30;
  }

  @Override
  protected String getConfiguredDbTable() {
    return "imdbTitleBasics";
  }

  @Override
  protected void initDataset() {
    Path databaseFile = getDatabaseFile();

    try {
      if (isRefreshRequired(databaseFile)) {
        LOGGER.debug("Preparing IMDB dataset {}...", getConfiguredDataset());

        Path extractedFile = downloadAndExtractDataset();
        if (extractedFile != null) {
          LOGGER.debug("Parsing dataset...");

          openWritableDatabase(databaseFile);
          dbMap = mvStore.openMap(dbTable);

          try (var stream = Files.lines(extractedFile, StandardCharsets.UTF_8)) {
            stream.skip(1).forEach(line -> {
              // tconst, titleType, primaryTitle, originalTitle, isAdult, startYear, endYear, runtimeMinutes, genres
              String[] data = line.split("\t");
              dbMap.put(data[0], data[2]);
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

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * gets the title from IMDB
   * 
   * @param imdbId
   *          the IMDb id
   * @return title or empty sting - but never NULL
   */
  public String getTitleById(String imdbId) {
    init();
    try {
      Object obj = dbMap.get(imdbId);
      if (obj instanceof String title && StringUtils.isNotBlank(title)) {
        return title;
      }
    }
    catch (Exception e) {
      LOGGER.debug("could not read the MVstore - '{}'", e.getMessage());
      shutdown();
      Utils.deleteFileSafely(getDatabaseFile());
    }
    return "";
  }
}
