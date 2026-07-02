package org.tinymediamanager.scraper.imdb.datasets;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;

public abstract class ImdbAbstractDataset {

  protected static final String   IMDB_DATASET_NAME_BASICS      = "name.basics.tsv";
  protected static final String   IMDB_DATASET_TITLE_AKAS       = "title.akas.tsv";
  protected static final String   IMDB_DATASET_TITLE_BASICS     = "title.basics.tsv";
  protected static final String   IMDB_DATASET_TITLE_CREW       = "title.crew.tsv";
  protected static final String   IMDB_DATASET_TITLE_EPISODE    = "title.episode.tsv";
  protected static final String   IMDB_DATASET_TITLE_PRINCIPALS = "title.principals.tsv";
  protected static final String   IMDB_DATASET_TITLE_RATINGS    = "title.ratings.tsv";

  /**
   * the DB storage engine
   */
  protected MVStore               mvStore;
  /**
   * the opened DB map
   */
  protected MVMap<String, String> dbMap;
  /**
   * java shadow map for adding/updating <br>
   * doing that directly inside M2 map is waaay to slow...
   */
  protected Map<String, String>   javaMap;

  /**
   * the DB table name
   */
  protected String                dbTable;

  public ImdbAbstractDataset() {
    this.dbTable = getConfiguredDbTable();
  }

  protected synchronized void init() {
    if (mvStore == null) {
      initDataset();
    }
  }

  protected void initDataset() {
    // Placeholder implementation while datasets are migrated one by one.
    getLogger().debug("Dataset initialization is not yet implemented for {}", getConfiguredDataset());
  }

  protected void shutdown() {
    try {
      if (mvStore != null && !mvStore.isClosed()) {
        mvStore.close();
      }
    }
    catch (Exception e) {
      getLogger().debug("Could not close MVstore - deleting the cache");
    }
    finally {
      mvStore = null;
      javaMap = null;
      dbMap = null;
    }
  }

  abstract protected Logger getLogger();

  protected abstract String getConfiguredDataset();

  protected abstract int getConfiguredCacheDays();

  protected abstract String getConfiguredDbTable();

  protected Path getDatabaseFile() {
    return Paths.get(Globals.CACHE_FOLDER, getConfiguredDataset() + ".db");
  }

  protected boolean isRefreshRequired(Path databaseFile) throws IOException {
    if (!Files.exists(databaseFile)) {
      return true;
    }

    long cut = LocalDateTime.now().minusDays(getConfiguredCacheDays()).toEpochSecond(ZoneOffset.UTC);
    return Files.getLastModifiedTime(databaseFile).to(TimeUnit.SECONDS) < cut;
  }

  /**
   * Downloads and extracts dataset (d'oh)
   * 
   * @return path of extracted file, or NULL when download failed
   * @throws IOException
   *           on any error
   */
  protected Path downloadAndExtractDataset() throws IOException {
    boolean ok = downloadDataset();
    if (!ok) {
      return null;
    }

    return unpackDataset();
  }

  protected void openWritableDatabase(Path databaseFile) {
    try {
      mvStore = new MVStore.Builder().fileName(databaseFile.toString()).compress().autoCommitDisabled().open();
    }
    catch (Exception e) {
      getLogger().debug("Could not open IMDB database {} - '{}'", getConfiguredDataset(), e.getMessage());
      Utils.deleteFileSafely(databaseFile);
      mvStore = new MVStore.Builder().fileName(databaseFile.toString()).compress().autoCommitDisabled().open();
    }
  }

  protected void openReadOnlyDatabase(Path databaseFile) {
    mvStore = new MVStore.Builder().fileName(databaseFile.toString()).readOnly().open();
  }

  protected boolean downloadDataset() {
    String dataset = getConfiguredDataset();
    getLogger().debug("Downloading dataset {}", dataset);
    Path downloadedFile = Paths.get(Globals.CACHE_FOLDER, dataset + ".gz");
    Utils.deleteFileSafely(downloadedFile);
    try {
      Url imdbUrl = new OnDiskCachedUrl("https://datasets.imdbws.com/" + dataset + ".gz", getConfiguredCacheDays(), TimeUnit.DAYS);
      return imdbUrl.download(downloadedFile);
    }
    catch (Exception e) {
      getLogger().debug("Could not create dataset URL for {} - '{}'", dataset, e.getMessage());
      return false;
    }
  }

  protected Path unpackDataset() throws FileNotFoundException, IOException {
    String dataset = getConfiguredDataset();
    getLogger().debug("Unpacking dataset {}", dataset);
    Path downloadedFile = Paths.get(Globals.CACHE_FOLDER, dataset + ".gz");
    Path extractedTempFile = Paths.get(Globals.CACHE_FOLDER, dataset + ".tmp");
    Path extractedFile = Paths.get(Globals.CACHE_FOLDER, dataset);
    Utils.deleteFileSafely(extractedTempFile);
    try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(downloadedFile.toFile()))) {
      Files.copy(gis, extractedTempFile);
    }
    // DL+extraction worked w/o error, NOW its the time to kill the old file
    Utils.deleteFileSafely(downloadedFile);
    Utils.deleteFileSafely(extractedFile);
    Utils.moveFileSafe(extractedTempFile, extractedFile);

    return extractedFile;
  }

}
