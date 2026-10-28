package org.tinymediamanager.scraper.imdb.datasets;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Utils;

public class ImdbDatasetTitlePrincipals extends ImdbAbstractDataset {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImdbDatasetTitlePrincipals.class);

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected String getConfiguredDataset() {
    return IMDB_DATASET_TITLE_PRINCIPALS;
  }

  @Override
  protected int getConfiguredCacheDays() {
    return 30;
  }

  @Override
  protected String getConfiguredDbTable() {
    return "imdbTitlePrincipals";
  }

  @Override
  protected void initDataset() {
    Path databaseFile = getDatabaseFile();

    try {
      if (isRefreshRequired(databaseFile)) {
        LOGGER.debug("Preparing IMDB dataset {}...", getConfiguredDataset());

        Path extractedFile = downloadAndExtractDataset();
        if (extractedFile != null) {
          // ***************************************
          // ***************************** DUMMY IMPL
          // ***************************************
          LOGGER.debug("Parsing dataset...");

          openWritableDatabase(databaseFile);
          dbMap = mvStore.openMap(dbTable);

          try (var stream = Files.lines(extractedFile, StandardCharsets.UTF_8)) {
            stream.skip(1).forEach(line -> {
              // TODO: tconst
              String[] data = line.split("\t");
              dbMap.put(data[0], data[2]);
            });
          }

          LOGGER.debug("Copying map to database...");
          mvStore.commit();

          Files.delete(extractedFile);

          LOGGER.debug("Done!");
          // ***************************************
          // ***************************************
          // ***************************************
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

  public List<String> getPrincipalsByTitleId(String imdbId) {
    init();
    LOGGER.debug("Dummy lookup for imdbId '{}' in {}", imdbId, getConfiguredDataset());
    return Collections.emptyList();
  }
}
