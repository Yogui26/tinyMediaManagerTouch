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

package org.tinymediamanager.core.movie.tasks;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.MovieComparator;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class cannot run, since Settings() is STATIC<br>
 * run these test individually (for now)
 * 
 * @author Myron Boyle
 *
 */
public class MovieUpdateDatasourceTaskTest extends BasicMovieTest {

  private static final int NUMBER_OF_EXPECTED_MOVIES     = 103;
  private static final int NUMBER_OF_STACKED_MOVIES      = 14;
  private static final int NUMBER_OF_DISC_MOVIES         = 7;
  private static final int NUMBER_OF_MOVIES_IN_MMD       = 59;
  private static final int NUMBER_OF_EXPECTED_MEDIAFILES = 233;

  @Before
  public void setup() throws Exception {
    super.setup();

    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();

    // just a copy; we might have another movie test which uses these files
    copyResourceFolderToWorkFolder("testmovies");
    MovieModuleManager.getInstance().getSettings().addMovieDataSources(getWorkFolder().resolve("testmovies").toAbsolutePath().toString());
  }

  @After
  public void tearDownAfterTest() throws Exception {
    MovieModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  @Test
  public void udsNew() throws Exception {
    MovieUpdateDatasourceTask task = updateAllDataSources();
    task.run();

    showEntries();
  }

  @Test
  public void udsFolder() throws Exception {
    MovieUpdateDatasourceTask task = MovieUpdateDatasourceTask.forPaths(List.of(getWorkFolder().resolve("testmovies").resolve("Harry Potter")));
    task.run();

    MovieList movieList = MovieModuleManager.getInstance().getMovieList();
    assertThat(movieList.getMovies()).hasSize(4);
  }

  @Test
  public void udsNestedDatasource() throws Exception {
    MovieSettings settings = MovieModuleManager.getInstance().getSettings();
    MovieList movieList = MovieModuleManager.getInstance().getMovieList();

    Path parentDs = getWorkFolder().resolve("testmovies").toAbsolutePath();
    Path nestedDs = parentDs.resolve("Harry Potter");

    // adding a data source which is nested inside another one is allowed now
    assertThat(settings.addMovieDataSources(nestedDs.toString())).isTrue();
    assertThat(settings.getMovieDataSource()).contains(nestedDs.toString());

    MovieUpdateDatasourceTask task = updateAllDataSources();
    task.run();

    // the movies inside the nested data source must belong to the nested data source
    Movie hp1 = movieList.findFirstByPath(nestedDs.resolve("HP7 Deathly Hallows Part 1 (2010)"));
    assertThat(hp1).isNotNull();
    assertThat(Paths.get(hp1.getDataSource())).isEqualTo(nestedDs);

    Movie hp2 = movieList.findFirstByPath(nestedDs.resolve("HP7 Deathly Hallows Part 2"));
    assertThat(hp2).isNotNull();
    assertThat(Paths.get(hp2.getDataSource())).isEqualTo(nestedDs);

    // ... and each of them must exist exactly once (no duplicates from the parent scan)
    assertThat(movieList.findByPath(nestedDs.resolve("HP7 Deathly Hallows Part 1 (2010)"))).hasSize(1);
    assertThat(movieList.findByPath(nestedDs.resolve("HP7 Deathly Hallows Part 2"))).hasSize(1);

    // movies outside the nested data source still belong to the parent data source
    Movie parentMovie = movieList.getMovies().stream().filter(movie -> !movie.getPathNIO().startsWith(nestedDs)).findFirst().orElse(null);
    assertThat(parentMovie).isNotNull();
    assertThat(Paths.get(parentMovie.getDataSource())).isEqualTo(parentDs);
  }

  @Test
  public void udsNestedDatasourceCleanup() throws Exception {
    MovieSettings settings = MovieModuleManager.getInstance().getSettings();
    MovieList movieList = MovieModuleManager.getInstance().getMovieList();

    Path parentDs = getWorkFolder().resolve("testmovies").toAbsolutePath();
    Path nestedDs = parentDs.resolve("Harry Potter");
    assertThat(settings.addMovieDataSources(nestedDs.toString())).isTrue();

    updateAllDataSources().run();

    Path removedMovie = nestedDs.resolve("HP7 Deathly Hallows Part 2");
    assertThat(movieList.findFirstByPath(removedMovie)).isNotNull();

    // remove the movie folder from disk and re-run -> the nested data source must clean up its own entry
    FileUtils.deleteDirectory(removedMovie.toFile());

    updateAllDataSources().run();

    assertThat(movieList.findFirstByPath(removedMovie)).isNull();
    assertThat(movieList.getMovies()).noneMatch(movie -> movie.getPathNIO().equals(removedMovie));
  }

  @Test
  public void udsNestedDatasourceReassignsStaleMovie() throws Exception {
    MovieSettings settings = MovieModuleManager.getInstance().getSettings();
    MovieList movieList = MovieModuleManager.getInstance().getMovieList();

    Path parentDs = getWorkFolder().resolve("testmovies").toAbsolutePath();
    Path nestedDs = parentDs.resolve("Harry Potter");

    // first scan with only the parent data source - the HP movies are stored with the parent data source
    updateAllDataSources().run();

    Path hpMovie = nestedDs.resolve("HP7 Deathly Hallows Part 1 (2010)");
    Movie hp1 = movieList.findFirstByPath(hpMovie);
    assertThat(hp1).isNotNull();
    assertThat(Paths.get(hp1.getDataSource())).isEqualTo(parentDs);

    // add the parent folder of the movie as a data source afterwards
    assertThat(settings.addMovieDataSources(nestedDs.toString())).isTrue();

    // update only the parent data source - the cleanup must update the data source field and keep the movie
    MovieUpdateDatasourceTask.forDataSources(List.of(parentDs)).run();

    hp1 = movieList.findFirstByPath(hpMovie);
    assertThat(hp1).isNotNull();
    assertThat(Paths.get(hp1.getDataSource())).isEqualTo(nestedDs);
  }

  private MovieUpdateDatasourceTask updateAllDataSources() {
    return MovieUpdateDatasourceTask.forAllDataSources();
  }

  private void showEntries() throws Exception {
    // wait until all movies have been added (let propertychanges finish)
    for (int i = 0; i < 3; i++) {
      if (MovieModuleManager.getInstance().getMovieList().getMovieCount() == NUMBER_OF_EXPECTED_MOVIES) {
        break;
      }

      // not all here yet? wait for a second
      System.out.println("waiting for 1000 ms");
      Thread.sleep(1000);
    }

    int stack = 0;
    int disc = 0;
    int mmd = 0;
    int mfCnt = 0;
    List<Movie> movies = MovieModuleManager.getInstance().getMovieList().getMovies();
    movies.sort(new MovieComparator());
    for (Movie m : movies) {
      System.out.println(rpad(m.getTitle(), 30) + "(Disc:" + rpad(m.isDisc(), 5) + " Stack:" + rpad(m.isStacked(), 5) + " Multi:"
          + rpad(m.isMultiMovieDir(), 5) + ")\t" + m.getPathNIO());
      if (m.isStacked()) {
        stack++;
      }
      if (m.isDisc()) {
        disc++;
      }
      if (m.isMultiMovieDir()) {
        mmd++;
      }
      mfCnt += m.getMediaFiles().size();
    }

    assertEqual("Amount of movies does not match!", NUMBER_OF_EXPECTED_MOVIES, MovieModuleManager.getInstance().getMovieList().getMovieCount());
    assertEqual("Amount of stacked movies does not match!", NUMBER_OF_STACKED_MOVIES, stack);
    assertEqual("Amount of disc folders does not match!", NUMBER_OF_DISC_MOVIES, disc);
    assertEqual("Amount of movies in multimoviedirs does not match!", NUMBER_OF_MOVIES_IN_MMD, mmd);
    assertEqual("Amount of mediafiles does not match!", NUMBER_OF_EXPECTED_MEDIAFILES, mfCnt);

    // check the found files
    Path testMoviesFolder = getWorkFolder().resolve("testmovies");

    Movie hp7Movie = getMovieByFolder(testMoviesFolder, "Harry Potter/HP7 Deathly Hallows Part 1 (2010)");
    assertMediaFileCount(hp7Movie, "Harry Potter/HP7 Deathly Hallows Part 1 (2010)", 7);
    assertMediaFileType(hp7Movie, "HP7 DH Part 1 (2010) [x265].avi", MediaFileType.VIDEO);
    assertMediaFileType(hp7Movie, "HP7 DH Part 1 (2010) [x265]-poster.jpg", MediaFileType.POSTER);
    assertMediaFileType(hp7Movie, "HP7 DH Part 1 (2010) [x265]-fanart.jpg", MediaFileType.FANART);
    assertMediaFileType(hp7Movie, "HP7 DH Part 1 (2010) [x265]-banner.jpg", MediaFileType.BANNER);
    assertMediaFileType(hp7Movie, "HP7 DH Part 1 (2010) [x265]-clearart.jpg", MediaFileType.CLEARART);
    assertMediaFileType(hp7Movie, "HP7 DH Part 1 (2010) [x265]-clearlogo.jpg", MediaFileType.CLEARLOGO);
    assertMediaFileType(hp7Movie, "HP7 DH Part 1 (2010) [x265]-logo.jpg", MediaFileType.CLEARLOGO);

    Movie singleMovie = getMovieByFolder(testMoviesFolder, "Single");
    assertMediaFileCount(singleMovie, "Single", 3);
    assertMediaFileType(singleMovie, "singlefile.avi", MediaFileType.VIDEO);
    assertMediaFileType(singleMovie, "singlefile-poster.png", MediaFileType.POSTER);
    assertMediaFileType(singleMovie, "singlefile-fanart.png", MediaFileType.FANART);

    Movie synologyMovie = getMovieByFolder(testMoviesFolder, "Synology/101 Dalmatiner");
    assertMediaFileCount(synologyMovie, "Synology/101 Dalmatiner", 2);
    assertMediaFileType(synologyMovie, "101 Dalmatiner.avi", MediaFileType.VIDEO);
    assertMediaFileType(synologyMovie, "101 Dalmatiner.avi.vsmeta", MediaFileType.VSMETA);

    Movie partMovie = getMovieByFolder(testMoviesFolder, "Part X/Movie Part I");
    assertMediaFileCount(partMovie, "Part X/Movie Part I", 2);
    assertMediaFileType(partMovie, "moviefile part I.avi", MediaFileType.VIDEO);
    assertMediaFileType(partMovie, "moviefile part I.srt", MediaFileType.SUBTITLE);

    Movie trailerFolderMovie = getMovieByFolder(testMoviesFolder, "MovieWithTrailersInFolder");
    assertMediaFileCount(trailerFolderMovie, "MovieWithTrailersInFolder", 5);
    assertMediaFileType(trailerFolderMovie, "MovieWithTrailersInFolder.avi", MediaFileType.VIDEO);
    assertMediaFileType(trailerFolderMovie, "cool.avi", MediaFileType.TRAILER);
    assertMediaFileType(trailerFolderMovie, "another.avi", MediaFileType.TRAILER);
    assertMediaFileType(trailerFolderMovie, "cools.avi", MediaFileType.TRAILER);
    assertMediaFileType(trailerFolderMovie, "anothers.avi", MediaFileType.TRAILER);

    Movie extrasFolderMovie = getMovieByFolder(testMoviesFolder, "MovieWithExtrasInFolder");
    assertMediaFileCount(extrasFolderMovie, "MovieWithExtrasInFolder", 9);
    assertMediaFileType(extrasFolderMovie, "MovieWithExtrasInFolderMovieWithExtrasInFolder.avi", MediaFileType.VIDEO);
    assertMediaFileType(extrasFolderMovie, "MovieWithExtrasInFolderMovieWithExtrasInFolder-poster.jpg", MediaFileType.POSTER);
    assertMediaFileType(extrasFolderMovie, "stuff.avi", MediaFileType.EXTRA);
    assertMediaFileType(extrasFolderMovie, "stuff.jpg", MediaFileType.EXTRA);
    assertMediaFileType(extrasFolderMovie, "another_extra.avi", MediaFileType.EXTRA);
    assertMediaFileType(extrasFolderMovie, "another_extra.nfo", MediaFileType.EXTRA);
    assertMediaFileType(extrasFolderMovie, "another_extra-poster.jpg", MediaFileType.EXTRA);
    assertMediaFileType(extrasFolderMovie, "cool.avi", MediaFileType.EXTRA);
    assertMediaFileType(extrasFolderMovie, "another_extras.avi", MediaFileType.EXTRA);

    Movie subtitleFolderMovie = getMovieByFolder(testMoviesFolder, "SubtitleFolder");
    assertMediaFileCount(subtitleFolderMovie, "SubtitleFolder", 6);
    assertMediaFileType(subtitleFolderMovie, "SubtitleFolder.mkv", MediaFileType.VIDEO);
    assertMediaFileType(subtitleFolderMovie, "01_english-eng_sdh.srt", MediaFileType.SUBTITLE);
    assertMediaFileType(subtitleFolderMovie, "02_german.de.srt", MediaFileType.SUBTITLE);
    assertMediaFileType(subtitleFolderMovie, "23_Romanian.srt", MediaFileType.SUBTITLE);
    assertMediaFileType(subtitleFolderMovie, "english.srt", MediaFileType.SUBTITLE);
    assertMediaFileType(subtitleFolderMovie, "german.srt", MediaFileType.SUBTITLE);

    Movie themeMovie = getMovieByFolder(testMoviesFolder, "MovieWithTheme");
    assertMediaFileCount(themeMovie, "MovieWithTheme", 5);
    assertMediaFileType(themeMovie, "TMovie.avi", MediaFileType.VIDEO);
    assertMediaFileType(themeMovie, "TMovie-theme.mp3", MediaFileType.THEME);
    assertMediaFileType(themeMovie, "theme.mp3", MediaFileType.THEME);
    assertMediaFileType(themeMovie, "theme2.mp3", MediaFileType.THEME);
    assertMediaFileType(themeMovie, "theme.mp4", MediaFileType.THEME);
  }

  /**
   * Gets the first movie from the movie list for the given relative folder inside testmovies.
   *
   * @param testMoviesFolder
   *          the copied testmovies root in the work folder
   * @param relativeMovieFolder
   *          the movie folder relative to testmovies
   * @return the detected movie
   */
  private Movie getMovieByFolder(Path testMoviesFolder, String relativeMovieFolder) {
    Movie movie = MovieModuleManager.getInstance().getMovieList().findFirstByPath(testMoviesFolder.resolve(relativeMovieFolder));
    assertEqual("Movie not found for folder: " + relativeMovieFolder, false, movie == null);
    return movie;
  }

  /**
   * A skiptro sidecar ("&lt;video basename&gt;.skiptro.json") must be detected as {@link MediaFileType#SKIPTRO} and associated to the movie,
   * otherwise it would neither show up in the GUI nor be moved along with the video when renaming.
   */
  @Test
  public void udsSkiptroSidecarIsAttached() throws Exception {
    Path skiptroDs = getWorkFolder().resolve("skiptrods").toAbsolutePath();
    Path movieFolder = skiptroDs.resolve("Test Movie (2020)");
    Files.createDirectories(movieFolder);
    Files.createFile(movieFolder.resolve("testmovie.mkv"));
    Files.writeString(movieFolder.resolve("testmovie.skiptro.json"), "{ \"intro\": { \"start\": 0, \"end\": 87.5 } }");
    Files.writeString(movieFolder.resolve("testmovie.mkv.skiptro.json"), "{ \"intro\": { \"start\": 0, \"end\": 87.5 } }");
    Files.writeString(movieFolder.resolve("other.skiptro.json"), "{ \"intro\": { \"start\": 0, \"end\": 87.5 } }");

    MovieModuleManager.getInstance().getSettings().addMovieDataSources(skiptroDs.toString());

    MovieUpdateDatasourceTask task = updateAllDataSources();
    task.run();

    Movie movie = getMovieByFolder(skiptroDs, "Test Movie (2020)");

    List<MediaFile> skiptroFiles = movie.getMediaFiles(MediaFileType.SKIPTRO);
    assertThat(skiptroFiles).hasSize(2);
    assertThat(skiptroFiles).extracting(MediaFile::getFilename).containsExactlyInAnyOrder("testmovie.skiptro.json", "testmovie.mkv.skiptro.json");
  }

  /**
   * ScenePass sidecars ("&lt;video basename&gt;.sp.json" and "&lt;video basename&gt;.sp.hash.json") must be detected and associated to the matching
   * movie. Sidecars with a non-matching basename must not be attached.
   */
  @Test
  public void udsScenePassSidecarsAreAttached() throws Exception {
    Path scenePassDs = getWorkFolder().resolve("scenepassds").toAbsolutePath();
    Path movieFolder = scenePassDs.resolve("Test Movie (2020)");
    Files.createDirectories(movieFolder);
    Files.createFile(movieFolder.resolve("testmovie.mkv"));
    Files.writeString(movieFolder.resolve("testmovie.sp.json"), "{}");
    Files.writeString(movieFolder.resolve("testmovie.sp.hash.json"), "{}");
    Files.writeString(movieFolder.resolve("other.sp.json"), "{}");

    MovieModuleManager.getInstance().getSettings().addMovieDataSources(scenePassDs.toString());

    MovieUpdateDatasourceTask task = updateAllDataSources();
    task.run();

    Movie movie = getMovieByFolder(scenePassDs, "Test Movie (2020)");

    List<MediaFile> scenePassFiles = movie.getMediaFiles(MediaFileType.SCENEPASS);
    assertThat(scenePassFiles).hasSize(2);
    assertThat(scenePassFiles).extracting(MediaFile::getFilename).containsExactlyInAnyOrder("testmovie.sp.json", "testmovie.sp.hash.json");
  }

  /**
   * Asserts that all expected media files from the folder have been associated to the movie.
   *
   * @param movie
   *          the movie to test
   * @param movieFolder
   *          the tested folder (for assertion messages)
   * @param expectedCount
   *          the expected amount of associated media files
   */
  private void assertMediaFileCount(Movie movie, String movieFolder, int expectedCount) {
    assertEqual("Unexpected amount of found media files for folder: " + movieFolder, expectedCount, movie.getMediaFiles().size());
  }

  /**
   * Asserts that a specific media file has been found and assigned to the expected media file type.
   *
   * @param movie
   *          the movie to test
   * @param filename
   *          the media file name to search for
   * @param expectedType
   *          the expected media file type
   */
  private void assertMediaFileType(Movie movie, String filename, MediaFileType expectedType) {
    MediaFile mediaFile = null;
    for (MediaFile mf : movie.getMediaFiles()) {
      if (filename.equalsIgnoreCase(mf.getFilename())) {
        mediaFile = mf;
        break;
      }
    }

    if (mediaFile == null) {
      assertEqual("Media file not found: " + filename + " for movie " + movie.getPathNIO(), false, true);
      return;
    }

    assertEqual("Unexpected media file type for file " + filename, expectedType, mediaFile.getType());
  }

  public static String rpad(Object s, int n) {
    return String.format("%1$-" + n + "s", s);
  }
}
