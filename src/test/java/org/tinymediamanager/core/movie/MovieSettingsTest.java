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

package org.tinymediamanager.core.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Test;
import org.tinymediamanager.core.Utils;

public class MovieSettingsTest extends BasicMovieTest {

  @Test
  public void testMovieSettings() throws Exception {
    MovieSettings settings = MovieSettings.getInstance();
    assertThat(settings).isNotNull();
    settings.getDefaultRenamerProfile().setAsciiReplacement(true);

    // let the dirty flag set by the async propertychange listener
    Thread.sleep(100);

    settings.saveSettings();

    // cannot re-instantiate settings - need to check plain file
    String config = Utils.readFileToString(getSettingsFolder().resolve(MovieModuleManager.getInstance().getSettings().getConfigFilename()));
    assertTrue(config.contains("\"asciiReplacement\" : true"));
  }

  @Test
  public void testForceOverwriteSetting() {
    MovieSettings settings = MovieSettings.getInstance();

    settings.setForceOverwrite(true);
    assertThat(settings.isForceOverwrite()).isTrue();

    // force overwrite is only valid when overwriting is enabled
    settings.setDoNotOverwriteExistingData(true);
    assertThat(settings.isForceOverwrite()).isFalse();

    settings.setDoNotOverwriteExistingData(false);
    settings.setForceOverwrite(true);
    assertThat(settings.isForceOverwrite()).isTrue();

    settings.setForceOverwrite(false);
  }

  @Test
  public void testMovieSettingsLoading() throws Exception {
    copyResourceFolderToWorkFolder("settings/movie_renamer_loading");

    Path settingsFolder = getWorkFolder().resolve("settings/movie_renamer_loading");
    MovieSettings settings = MovieSettings.getInstance(settingsFolder.toAbsolutePath().toString());
    assertThat(settings.getMovieDataSource()).isNotEmpty();
  }

  @Test
  public void testCustomColumnsOnLegacySettings() throws Exception {
    copyResourceFolderToWorkFolder("settings/movie_renamer_loading");

    Path settingsFolder = getWorkFolder().resolve("settings/movie_renamer_loading");
    MovieSettings settings = MovieSettings.getInstance(settingsFolder.toAbsolutePath().toString());

    // a settings file which does not know anything about custom columns simply has no custom columns at all
    assertThat(settings.getCustomColumns()).isEmpty();
    assertThat(settings.getMovieSetCustomColumns()).isEmpty();
  }

  @Test
  public void testCustomColumnsAddRemoveAndSwap() {
    MovieSettings settings = MovieSettings.getInstance();
    settings.setCustomColumns(new ArrayList<>());

    MovieCustomColumn first = new MovieCustomColumn("first", "${title}");
    MovieCustomColumn second = new MovieCustomColumn("second", "${year}");
    MovieCustomColumn third = new MovieCustomColumn("third", "${rated}");

    settings.addCustomColumn(first);
    settings.addCustomColumn(second);
    settings.addCustomColumn(third);
    assertThat(settings.getCustomColumns()).containsExactly(first, second, third);

    settings.swapCustomColumns(0, 2);
    assertThat(settings.getCustomColumns()).containsExactly(third, second, first);

    // an out of range swap is simply ignored
    settings.swapCustomColumns(0, 7);
    assertThat(settings.getCustomColumns()).containsExactly(third, second, first);

    settings.removeCustomColumn(second);
    assertThat(settings.getCustomColumns()).containsExactly(third, first);

    // the ids of the surviving columns are untouched, so their table state stays intact
    assertThat(third.getColumnIdentifier()).startsWith(MovieCustomColumn.COLUMN_IDENTIFIER_PREFIX);
    assertThat(first.getColumnIdentifier()).startsWith(MovieCustomColumn.COLUMN_IDENTIFIER_PREFIX);
  }

  @Test
  public void testCustomColumnsWithoutIdAreToldApart() {
    MovieSettings settings = MovieSettings.getInstance();

    // a hand-tuned settings file can simply leave the id out - these two must never be mistaken for each other
    MovieCustomColumn first = new MovieCustomColumn("first", "${title}");
    MovieCustomColumn second = new MovieCustomColumn("second", "${year}");
    first.setId("");
    second.setId("");

    assertThat(first.getColumnIdentifier()).isNotEqualTo(second.getColumnIdentifier());
    assertThat(first).isNotEqualTo(second);

    settings.setCustomColumns(new ArrayList<>(Arrays.asList(first, second)));
    settings.removeCustomColumn(second);
    assertThat(settings.getCustomColumns()).containsExactly(first);
  }

  @Test
  public void testCustomColumnsWithDuplicatedIdGetHealedOnLoad() {
    MovieSettings settings = MovieSettings.getInstance();

    MovieCustomColumn first = new MovieCustomColumn("first", "${title}");
    MovieCustomColumn second = new MovieCustomColumn("second", "${year}");
    second.setId(first.getId());

    settings.setCustomColumns(new ArrayList<>(Arrays.asList(first, second)));
    assertThat(first.getColumnIdentifier()).isEqualTo(second.getColumnIdentifier());

    final String duplicatedId = first.getId();

    // this is what the settings loader calls right after the deserialization
    settings.afterLoading();

    // the first column keeps its id, only the duplicate gets a fresh one
    assertThat(first.getId()).isEqualTo(duplicatedId);
    assertThat(second.getId()).isNotEqualTo(duplicatedId);
    assertThat(first.getColumnIdentifier()).isNotEqualTo(second.getColumnIdentifier());
    assertThat(settings.getCustomColumns()).containsExactly(first, second);
  }

  @Test
  public void testMovieSetCustomColumnsAddRemoveAndSwap() {
    MovieSettings settings = MovieSettings.getInstance();
    settings.setMovieSetCustomColumns(new ArrayList<>());

    MovieCustomColumn first = new MovieCustomColumn("first", "${movie.title}");
    first.setSetPattern("${movieSet.title}");
    MovieCustomColumn second = new MovieCustomColumn("second", "${movie.year}");

    settings.addMovieSetCustomColumn(first);
    settings.addMovieSetCustomColumn(second);
    assertThat(settings.getMovieSetCustomColumns()).containsExactly(first, second);

    settings.swapMovieSetCustomColumns(0, 1);
    assertThat(settings.getMovieSetCustomColumns()).containsExactly(second, first);

    // an out of range swap is simply ignored
    settings.swapMovieSetCustomColumns(0, 7);
    assertThat(settings.getMovieSetCustomColumns()).containsExactly(second, first);

    settings.removeMovieSetCustomColumn(first);
    assertThat(settings.getMovieSetCustomColumns()).containsExactly(second);

    // a column which only knows the movie level is still worth showing in the hierarchical movie set table
    assertThat(second.isConfiguredForSets()).isTrue();
    second.setPattern("");
    assertThat(second.isConfiguredForSets()).isFalse();
    second.setSetPattern("${movieSet.plot}");
    assertThat(second.isConfiguredForSets()).isTrue();
  }

  @Test
  public void testCustomColumnsWithDuplicatedIdGetHealedAcrossBothTables() {
    MovieSettings settings = MovieSettings.getInstance();

    MovieCustomColumn movieColumn = new MovieCustomColumn("movie", "${movie.title}");
    MovieCustomColumn setColumn = new MovieCustomColumn("set", "");
    setColumn.setId(movieColumn.getId());

    settings.setCustomColumns(new ArrayList<>(Arrays.asList(movieColumn)));
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(setColumn)));

    // both tables derive their table column identifiers from the very same prefix, so a duplicate needs to be healed even across lists
    assertThat(movieColumn.getColumnIdentifier()).isEqualTo(setColumn.getColumnIdentifier());
    final String duplicatedId = movieColumn.getId();

    settings.afterLoading();

    assertThat(movieColumn.getId()).isEqualTo(duplicatedId);
    assertThat(setColumn.getId()).isNotEqualTo(duplicatedId);
    assertThat(movieColumn.getColumnIdentifier()).isNotEqualTo(setColumn.getColumnIdentifier());
  }

  @Test
  public void testMovieSetCustomColumnsPersistence() throws Exception {
    MovieSettings settings = MovieModuleManager.getInstance().getSettings();

    MovieCustomColumn customColumn = new MovieCustomColumn();
    customColumn.setTitle("Runtime of the set");
    customColumn.setSetPattern("${movieSet.title}");
    customColumn.setPattern("${movie.runtime}");
    settings.setMovieSetCustomColumns(new ArrayList<>(Arrays.asList(customColumn)));

    // let the dirty flag set by the async propertychange listener
    Thread.sleep(100);
    settings.saveSettings();

    String config = Utils.readFileToString(getSettingsFolder().resolve(settings.getConfigFilename()));
    assertThat(config).contains("\"movieSetCustomColumns\"");
    assertThat(config).contains("\"setPattern\" : \"${movieSet.title}\"");
  }

  @After
  public void resetCustomColumns() {
    MovieSettings settings = MovieSettings.getInstance();
    settings.setCustomColumns(new ArrayList<>());
    settings.setMovieSetCustomColumns(new ArrayList<>());
  }

  @Test
  public void testUpgradeRenamerProfile() throws Exception {
    copyResourceFolderToWorkFolder("settings/movie_renamer_upgrade");

    Path settingsFolder = getWorkFolder().resolve("settings/movie_renamer_upgrade");
    MovieSettings settings = MovieSettings.getInstance(settingsFolder.toAbsolutePath().toString());
    assertThat(settings.getRenamerProfiles()).hasSize(1);

    MovieRenamerProfile profile = settings.getDefaultRenamerProfile();
    assertNotNull(profile);
    assertThat(profile.getName()).isEqualTo(MovieRenamerProfile.DEFAULT_RENAMER_PROFILE);

    // verify all renamer fields were upgraded from the old-style JSON
    assertThat(profile.getRenamerPathname()).isEqualTo("${title} ${- ,edition,} (${year}) xxx");
    assertThat(profile.getRenamerFilename()).isEqualTo("${title} ${- ,edition,} (${year}) ${videoFormat} ${audioCodec} xxx");

    assertThat(profile.isRenamerPathnameSpaceSubstitution()).isTrue();
    assertThat(profile.getRenamerPathnameSpaceReplacement()).isEqualTo("-");
    assertThat(profile.isRenamerFilenameSpaceSubstitution()).isTrue();
    assertThat(profile.getRenamerFilenameSpaceReplacement()).isEqualTo("-");

    assertThat(profile.getRenamerColonReplacement()).isEqualTo("\u2236");
    assertThat(profile.isRenamerNfoCleanup()).isTrue();
    assertThat(profile.isRenamerCleanupUnwanted()).isTrue();
    assertThat(profile.isRenamerCreateMoviesetForSingleMovie()).isTrue();
    assertThat(profile.getRenamerFirstCharacterNumberReplacement()).isEqualTo("#");

    assertThat(profile.isAsciiReplacement()).isFalse();
    assertThat(profile.isUnicodeReplacement()).isTrue();
    assertThat(profile.isAllowMultipleMoviesInSameDir()).isTrue();
  }
}
