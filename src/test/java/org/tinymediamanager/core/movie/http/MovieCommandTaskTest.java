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
package org.tinymediamanager.core.movie.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.http.AbstractCommandHandler;
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.threading.TmmTaskHandle.TaskState;

/**
 * The class {@link MovieCommandTaskTest} checks the movie HTTP command task end-to-end with locally safe actions (the {@code cleanup} action and the
 * scope resolution) - no network / external services are involved.
 */
public class MovieCommandTaskTest extends BasicMovieTest {

  /**
   * Disables the trash folder so deleted files are really gone.
   */
  @Before
  public void setupCommandTask() {
    Settings.getInstance().setEnableTrash(false);
  }

  /**
   * Tests that a {@code cleanup} command with scope {@code all} deletes the unwanted files of all movies and keeps the media files (the
   * {@code dryRun} argument defaults to {@code false}).
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void cleanupScopeAllDeletesUnwantedFiles() throws Exception {
    Movie first = createMovieWithJunk("First Movie");
    Movie second = createMovieWithJunk("Second Movie");

    new MovieCommandTask(List.of(cleanupCommand("all"))).run();

    assertThat(Path.of(first.getPath()).resolve("leftover.txt")).doesNotExist();
    assertThat(Path.of(first.getPath()).resolve("movie.mkv")).exists();
    assertThat(Path.of(second.getPath()).resolve("leftover.txt")).doesNotExist();
    assertThat(Path.of(second.getPath()).resolve("movie.mkv")).exists();
  }

  /**
   * Tests that {@code dryRun=true} is honored by the cleanup command.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void cleanupDryRunKeepsFiles() throws Exception {
    Movie movie = createMovieWithJunk("Dry Run Movie");
    AbstractCommandHandler.Command command = cleanupCommand("all");
    command.args.put("dryRun", "true");

    new MovieCommandTask(List.of(command)).run();

    assertThat(Path.of(movie.getPath()).resolve("leftover.txt")).exists();
  }

  /**
   * Tests that the {@code path} scope only processes the movies of the given paths.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void cleanupScopePathOnlyProcessesGivenPaths() throws Exception {
    Movie selected = createMovieWithJunk("Selected Movie");
    Movie other = createMovieWithJunk("Other Movie");

    AbstractCommandHandler.Command command = cleanupCommand("path");
    command.scope.args = new String[] { Path.of(selected.getPath()).toString() };

    new MovieCommandTask(List.of(command)).run();

    assertThat(Path.of(selected.getPath()).resolve("leftover.txt")).doesNotExist();
    assertThat(Path.of(other.getPath()).resolve("leftover.txt")).exists();
  }

  /**
   * Tests that the {@code dataSource} scope processes the movies of the given data source path.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void cleanupScopeDataSourceProcessesMatchingDataSource() throws Exception {
    MovieModuleManager.getInstance().getSettings().setMovieDataSources(List.of(getWorkFolder().toString()));

    Movie inDataSource = createMovieWithJunk("In Data Source Movie");
    Movie other = createMovieWithJunk("Other Data Source Movie");
    other.setDataSource(getWorkFolder().resolve("somewhere else").toString());

    AbstractCommandHandler.Command command = cleanupCommand("dataSource");
    command.scope.args = new String[] { getWorkFolder().toString() };

    new MovieCommandTask(List.of(command)).run();

    assertThat(Path.of(inDataSource.getPath()).resolve("leftover.txt")).doesNotExist();
    assertThat(Path.of(other.getPath()).resolve("leftover.txt")).exists();
  }

  /**
   * Tests that a {@code cleanup} command without a scope name defaults to the {@code new} scope - and therefore processes nothing when no
   * {@code update} command ran before.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void cleanupWithoutUpdateAndBlankScopeProcessesNothing() throws Exception {
    Movie movie = createMovieWithJunk("Blank Scope Movie");

    new MovieCommandTask(List.of(cleanupCommand(null))).run();

    assertThat(Path.of(movie.getPath()).resolve("leftover.txt")).exists();
  }

  /**
   * Tests that locked movies are filtered out of every scope.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void cleanupSkipsLockedMovies() throws Exception {
    Movie locked = createMovieWithJunk("Locked Movie");
    locked.setLocked(true);
    Movie unlocked = createMovieWithJunk("Unlocked Movie");

    new MovieCommandTask(List.of(cleanupCommand("all"))).run();

    assertThat(Path.of(locked.getPath()).resolve("leftover.txt")).exists();
    assertThat(Path.of(unlocked.getPath()).resolve("leftover.txt")).doesNotExist();
  }

  /**
   * Tests that an unknown action is simply ignored and the task finishes without an error.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void unknownActionIsIgnored() throws Exception {
    Movie movie = createMovieWithJunk("Untouched Movie");

    AbstractCommandHandler.Command command = new AbstractCommandHandler.Command();
    command.action = "definitelyNotAnAction";
    command.scope.name = "all";

    MovieCommandTask task = new MovieCommandTask(List.of(command));
    task.run();

    assertThat(task.getState()).isEqualTo(TaskState.FINISHED);
    assertThat(Path.of(movie.getPath()).resolve("leftover.txt")).exists();
  }

  /**
   * Tests that the {@code tmdbId} scope only processes the movies with the given TMDb id.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void cleanupScopeTmdbIdOnlyProcessesMatchingMovie() throws Exception {
    Movie selected = createMovieWithJunk("Selected Tmdb Movie");
    selected.setTmdbId(12345);
    Movie other = createMovieWithJunk("Other Tmdb Movie");
    other.setTmdbId(67890);

    AbstractCommandHandler.Command command = cleanupCommand("tmdbId");
    command.scope.args = new String[] { "12345" };

    new MovieCommandTask(List.of(command)).run();

    assertThat(Path.of(selected.getPath()).resolve("leftover.txt")).doesNotExist();
    assertThat(Path.of(other.getPath()).resolve("leftover.txt")).exists();
  }

  /**
   * Tests that the {@code imdbId} scope only processes the movies with the given IMDb id.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void cleanupScopeImdbIdOnlyProcessesMatchingMovie() throws Exception {
    Movie selected = createMovieWithJunk("Selected Imdb Movie");
    selected.setImdbId("tt0103639");
    Movie other = createMovieWithJunk("Other Imdb Movie");
    other.setImdbId("tt1234567");

    AbstractCommandHandler.Command command = cleanupCommand("imdbId");
    command.scope.args = new String[] { "tt0103639" };

    new MovieCommandTask(List.of(command)).run();

    assertThat(Path.of(selected.getPath()).resolve("leftover.txt")).doesNotExist();
    assertThat(Path.of(other.getPath()).resolve("leftover.txt")).exists();
  }

  /**
   * Tests that the {@code setWatched} action marks movies as watched (with playcount 1 and a last watched date).
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void setWatchedMarksMoviesAsWatched() throws Exception {
    Movie movie = createMovieWithJunk("Watch Me");
    assertThat(movie.isWatched()).isFalse();

    new MovieCommandTask(List.of(setWatchedCommand("all", true))).run();

    assertThat(movie.isWatched()).isTrue();
    assertThat(movie.getPlaycount()).isEqualTo(1);
    assertThat(movie.getLastWatched()).isNotNull();
  }

  /**
   * Tests that the {@code playcount} argument of the {@code setWatched} action is honored.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void setWatchedHonorsPlaycountArgument() throws Exception {
    Movie movie = createMovieWithJunk("Watch Me Twice");

    AbstractCommandHandler.Command command = setWatchedCommand("all", true);
    command.args.put("playcount", "5");

    new MovieCommandTask(List.of(command)).run();

    assertThat(movie.isWatched()).isTrue();
    assertThat(movie.getPlaycount()).isEqualTo(5);
  }

  /**
   * Tests that {@code setWatched} with {@code watched=false} clears the watched state.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void setWatchedCanMarkAsUnwatched() throws Exception {
    Movie movie = createMovieWithJunk("Unwatch Me");
    movie.setWatched(true);
    movie.setPlaycount(1);
    movie.setLastWatched(new java.util.Date());

    new MovieCommandTask(List.of(setWatchedCommand("all", false))).run();

    assertThat(movie.isWatched()).isFalse();
    assertThat(movie.getPlaycount()).isZero();
    assertThat(movie.getLastWatched()).isNull();
  }

  /**
   * Tests that {@code setWatched} only processes the movies of the given id scope.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void setWatchedScopeImdbIdOnlyProcessesMatchingMovie() throws Exception {
    Movie selected = createMovieWithJunk("Watch Selected Imdb Movie");
    selected.setImdbId("tt0103639");
    Movie other = createMovieWithJunk("Watch Other Imdb Movie");
    other.setImdbId("tt1234567");

    AbstractCommandHandler.Command command = setWatchedCommand("imdbId", true);
    command.scope.args = new String[] { "tt0103639" };

    new MovieCommandTask(List.of(command)).run();

    assertThat(selected.isWatched()).isTrue();
    assertThat(other.isWatched()).isFalse();
  }

  private AbstractCommandHandler.Command cleanupCommand(String scopeName) {
    AbstractCommandHandler.Command command = new AbstractCommandHandler.Command();
    command.action = "cleanup";
    command.scope.name = scopeName;
    return command;
  }

  private AbstractCommandHandler.Command setWatchedCommand(String scopeName, boolean watched) {
    AbstractCommandHandler.Command command = new AbstractCommandHandler.Command();
    command.action = "setWatched";
    command.scope.name = scopeName;
    command.args.put("watched", Boolean.toString(watched));
    return command;
  }

  private Movie createMovieWithJunk(String title) throws Exception {
    Path folder = getWorkFolder().resolve(title);
    Files.createDirectories(folder);
    Files.createFile(folder.resolve("movie.mkv"));
    Files.createFile(folder.resolve("leftover.txt"));

    Movie movie = new Movie();
    movie.setTitle(title);
    movie.setDataSource(getWorkFolder().toString());
    movie.setPath(folder.toString());
    MovieModuleManager.getInstance().getMovieList().addMovie(movie);

    return movie;
  }
}
