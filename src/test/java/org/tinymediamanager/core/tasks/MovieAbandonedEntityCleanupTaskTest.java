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
package org.tinymediamanager.core.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.IMessageListener;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;

/**
 * The class {@link MovieAbandonedEntityCleanupTaskTest} verifies that the abandoned entity cleanup removes movies whose data source is no longer
 * configured - and that it never removes more than that, especially no files and no movie sets.
 */
public class MovieAbandonedEntityCleanupTaskTest extends BasicMovieTest {

  private final List<Message>    collectedMessages = Collections.synchronizedList(new ArrayList<>());
  private final IMessageListener messageListener   = collectedMessages::add;

  @Before
  public void setup() throws Exception {
    super.setup();

    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();

    MessageManager.getInstance().addListener(messageListener);
  }

  @After
  public void tearDownAfterTest() throws Exception {
    MessageManager.getInstance().removeListener(messageListener);

    MovieModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  /**
   * Tests that a movie of a data source which has been removed from the settings is taken out of the database.
   */
  @Test
  public void removesMovieWithoutConfiguredDatasource() {
    Movie movie = addMovie("Gone Movie", getWorkFolder().resolve("gone"));

    configureDatasources(getWorkFolder().resolve("stillthere"));

    new AbandonedEntityCleanupTask().run();

    assertThat(getMovieList().getMovies()).doesNotContain(movie);
  }

  /**
   * Tests that a locked movie does not survive the cleanup of its data source.
   */
  @Test
  public void removesLockedMovieAsWell() {
    Movie movie = addMovie("Locked Gone Movie", getWorkFolder().resolve("gone"));
    movie.setLocked(true);

    configureDatasources(getWorkFolder().resolve("stillthere"));

    new AbandonedEntityCleanupTask().run();

    assertThat(movie.isLocked()).isTrue();
    assertThat(getMovieList().getMovies()).doesNotContain(movie);
  }

  /**
   * Tests that movies of a configured data source are kept, no matter whether they are stored directly on the data source root or in one of its
   * subfolders.
   */
  @Test
  public void keepsMoviesOfConfiguredDatasources() {
    Path datasource = getWorkFolder().resolve("stillthere");
    Movie rootMovie = addMovie("Root Movie", datasource);
    // an entity which has been created by a nested data source is still inside the configured root
    Movie nestedMovie = addMovie("Nested Movie", datasource.resolve("nested"));

    configureDatasources(datasource);

    new AbandonedEntityCleanupTask().run();

    assertThat(getMovieList().getMovies()).containsExactlyInAnyOrder(rootMovie, nestedMovie);
  }

  /**
   * Tests that a movie is kept when only its stored data source is outdated, but the movie itself still lives inside a configured data source.
   */
  @Test
  public void keepsMovieWithOutdatedDatasourceButInsideAConfiguredOne() {
    Path datasource = getWorkFolder().resolve("stillthere");
    Movie movie = addMovie("Outdated Datasource Movie", datasource.resolve("subfolder"));
    movie.setDataSource(getWorkFolder().resolve("gone").toString());

    configureDatasources(datasource);

    new AbandonedEntityCleanupTask().run();

    assertThat(getMovieList().getMovies()).containsExactly(movie);
  }

  /**
   * Tests that a temporarily unavailable data source is no reason to remove anything - the check never looks at the filesystem.
   */
  @Test
  public void keepsMoviesOfUnavailableButConfiguredDatasource() {
    Path notMounted = getWorkFolder().resolve("not-mounted");
    Movie movie = addMovie("Offline Nas Movie", notMounted);

    configureDatasources(notMounted);

    new AbandonedEntityCleanupTask().run();

    assertThat(getMovieList().getMovies()).containsExactly(movie);
  }

  /**
   * Tests that the whole check is skipped when no data source is configured at all, since that would empty the complete database.
   */
  @Test
  public void skipsCheckWithoutAnyConfiguredDatasource() {
    Movie movie = addMovie("Movie Of The Last Datasource", getWorkFolder().resolve("gone"));

    MovieModuleManager.getInstance().getSettings().setMovieDataSources(Collections.emptyList());

    new AbandonedEntityCleanupTask().run();

    assertThat(getMovieList().getMovies()).containsExactly(movie);

    List<Message> messages = findMessages("tmm.cleanupabandoned.skipped");
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getMessageLevel()).isEqualTo(MessageLevel.WARN);
  }

  /**
   * Tests that a movie set survives even when its last movie has been removed as abandoned.
   */
  @Test
  public void keepsTheMovieSetOfAnAbandonedMovie() {
    MovieSet movieSet = new MovieSet();
    movieSet.setTitle("Gone Collection");
    getMovieList().addMovieSet(movieSet);

    Movie movie = addMovie("Set Movie", getWorkFolder().resolve("gone"));
    movie.setMovieSet(movieSet);

    configureDatasources(getWorkFolder().resolve("stillthere"));

    new AbandonedEntityCleanupTask().run();

    assertThat(getMovieList().getMovies()).doesNotContain(movie);
    assertThat(getMovieList().getMovieSetList()).containsExactly(movieSet);
    assertThat(movieSet.getMovies()).isEmpty();
  }

  /**
   * Tests that the cleanup only removes database entries and leaves all files of the abandoned movie untouched.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void neverDeletesAnyFile() throws Exception {
    Path datasource = getWorkFolder().resolve("gone");
    Path movieFolder = datasource.resolve("Movie On A Gone Datasource");
    Files.createDirectories(movieFolder);
    Files.createFile(movieFolder.resolve("movie.mkv"));
    Files.createFile(movieFolder.resolve("movie.nfo"));
    Files.createFile(movieFolder.resolve("poster.jpg"));

    addMovie("Movie On A Gone Datasource", datasource);

    configureDatasources(getWorkFolder().resolve("stillthere"));

    new AbandonedEntityCleanupTask().run();

    assertThat(getMovieList().getMovies()).isEmpty();
    assertThat(movieFolder.resolve("movie.mkv")).exists();
    assertThat(movieFolder.resolve("movie.nfo")).exists();
    assertThat(movieFolder.resolve("poster.jpg")).exists();
  }

  /**
   * Tests that the summary of the cleanup is pushed as an info message.
   */
  @Test
  public void reportsTheRemovedEntries() {
    addMovie("Gone Movie", getWorkFolder().resolve("gone"));
    Movie keptMovie = addMovie("Kept Movie", getWorkFolder().resolve("stillthere"));

    configureDatasources(getWorkFolder().resolve("stillthere"));

    new AbandonedEntityCleanupTask().run();

    assertThat(getMovieList().getMovies()).containsExactly(keptMovie);

    List<Message> messages = findMessages("tmm.cleanupabandoned.result");
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getMessageLevel()).isEqualTo(MessageLevel.INFO);
    assertThat(messages.get(0).getIdParams()).containsExactly("1", "0");
  }

  private MovieList getMovieList() {
    return MovieModuleManager.getInstance().getMovieList();
  }

  private void configureDatasources(Path... dataSources) {
    List<String> datasourcePaths = new ArrayList<>();
    for (Path dataSource : dataSources) {
      datasourcePaths.add(dataSource.toAbsolutePath().toString());
    }

    MovieModuleManager.getInstance().getSettings().setMovieDataSources(datasourcePaths);
  }

  private Movie addMovie(String title, Path datasource) {
    Movie movie = new Movie();
    movie.setTitle(title);
    movie.setDataSource(datasource.toAbsolutePath().toString());
    movie.setPath(datasource.resolve(title).toAbsolutePath().toString());

    MediaFile mf = new MediaFile();
    mf.setType(MediaFileType.VIDEO);
    mf.setPath(movie.getPath());
    mf.setFilename(title + ".mkv");
    movie.addToMediaFiles(mf);

    getMovieList().addMovie(movie);

    return movie;
  }

  private List<Message> findMessages(String messageId) {
    return collectedMessages.stream().filter(message -> messageId.equals(message.getMessageId())).toList();
  }
}
