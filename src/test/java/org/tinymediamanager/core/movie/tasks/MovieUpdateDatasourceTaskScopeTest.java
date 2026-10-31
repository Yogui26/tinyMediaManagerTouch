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
import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the three update modes have distinct scopes: complete data sources, selected movies and selected paths.
 */
public class MovieUpdateDatasourceTaskScopeTest extends BasicMovieTest {

    @Before
    public void setup() throws Exception {
        super.setup();

        TmmModuleManager.getInstance().startUp();
        MovieModuleManager.getInstance().startUp();
    }

    @After
    public void tearDownAfterTest() throws Exception {
        MovieModuleManager.getInstance().shutDown();
        TmmModuleManager.getInstance().shutDown();
    }

    @Test
    public void emptyExplicitScopesAreNoOps() throws Exception {
        Path dataSource = Files.createDirectories(getWorkFolder().resolve("movies"));
        createMovie(dataSource, "Not Scanned (2020)");

        setDataSource(dataSource);

        MovieUpdateDatasourceTask.forMovies(List.of()).run();
        assertThat(getMovieList().getMovies()).isEmpty();

        MovieUpdateDatasourceTask.forPaths(List.of()).run();
        assertThat(getMovieList().getMovies()).isEmpty();

        MovieUpdateDatasourceTask.forDataSources(List.of()).run();
        assertThat(getMovieList().getMovies()).isEmpty();
    }

    @Test
    public void movieUpdateDoesNotScanSiblingFolders() throws Exception {
        Path dataSource = Files.createDirectories(getWorkFolder().resolve("movies"));
        Path category = Files.createDirectories(dataSource.resolve("scifi"));
        Path selectedMovie = createMovie(category, "Selected Movie (2020)");
        Path siblingMovie = createMovie(category, "Sibling Movie (2021)");

        setDataSource(dataSource);
        updateAllDataSources().run();

        MovieList movieList = getMovieList();
        assertThat(movieList.findFirstByPath(selectedMovie)).isNotNull();
        assertThat(movieList.findFirstByPath(siblingMovie)).isNotNull();

        // a movie added after the initial scan must not be discovered by updating another movie in the same category
        Path lateSibling = createMovie(category, "Late Sibling (2022)");

        MovieUpdateDatasourceTask.forMovies(List.of(selectedMovie)).run();

        assertThat(movieList.findFirstByPath(selectedMovie)).isNotNull();
        assertThat(movieList.findFirstByPath(siblingMovie)).isNotNull();
        assertThat(movieList.findFirstByPath(lateSibling)).isNull();
    }

    @Test
    public void movieUpdateCleansUpTheSelectedMovieOnly() throws Exception {
        Path dataSource = Files.createDirectories(getWorkFolder().resolve("movies"));
        Path category = Files.createDirectories(dataSource.resolve("scifi"));
        Path selectedMovie = createMovie(category, "Selected Movie (2020)");
        Path siblingMovie = createMovie(category, "Sibling Movie (2021)");

        setDataSource(dataSource);
        updateAllDataSources().run();

        MovieList movieList = getMovieList();
        assertThat(movieList.findFirstByPath(selectedMovie)).isNotNull();

        FileUtils.deleteDirectory(selectedMovie.toFile());
        MovieUpdateDatasourceTask.forMovies(List.of(selectedMovie)).run();

        assertThat(movieList.findFirstByPath(selectedMovie)).isNull();
        assertThat(movieList.findFirstByPath(siblingMovie)).isNotNull();
    }

    @Test
    public void pathUpdateDoesNotScanAboveTheSelectedPath() throws Exception {
        Path dataSource = Files.createDirectories(getWorkFolder().resolve("movies"));
        Path category = Files.createDirectories(dataSource.resolve("scifi"));
        Path selectedPath = Files.createDirectories(category.resolve("2020"));
        Path selectedMovie = createMovie(selectedPath, "Selected Movie (2020)");
        Path otherMovie = createMovie(Files.createDirectories(category.resolve("2021")), "Other Movie (2021)");

        setDataSource(dataSource);
        MovieUpdateDatasourceTask.forPaths(List.of(selectedPath)).run();

        MovieList movieList = getMovieList();
        assertThat(movieList.findFirstByPath(selectedMovie)).isNotNull();
        assertThat(movieList.findFirstByPath(otherMovie)).isNull();
    }

    @Test
    public void pathUpdateCleansUpMoviesBelowTheSelectedPath() throws Exception {
        Path dataSource = Files.createDirectories(getWorkFolder().resolve("movies"));
        Path category = Files.createDirectories(dataSource.resolve("scifi"));
        Path movie = createMovie(category, "Old Movie (2020)");
        Path siblingMovie = createMovie(category, "Sibling Movie (2021)");

        setDataSource(dataSource);
        updateAllDataSources().run();

        MovieList movieList = getMovieList();
        assertThat(movieList.findFirstByPath(movie)).isNotNull();

        FileUtils.deleteDirectory(movie.toFile());
        MovieUpdateDatasourceTask.forPaths(List.of(category)).run();

        assertThat(movieList.findFirstByPath(movie)).isNull();
        assertThat(movieList.findFirstByPath(siblingMovie)).isNotNull();
    }

    @Test
    public void pathUpdateCleansUpMissingPath() throws Exception {
        Path dataSource = Files.createDirectories(getWorkFolder().resolve("movies"));
        Path selectedPath = Files.createDirectories(dataSource.resolve("scifi"));
        Path movie = createMovie(selectedPath, "Deleted Movie (2020)");
        createMovie(Files.createDirectories(dataSource.resolve("other")), "Online Movie (2021)");

        setDataSource(dataSource);
        updateAllDataSources().run();

        MovieList movieList = getMovieList();
        assertThat(movieList.findFirstByPath(movie)).isNotNull();

        // the data source is still available - the deleted path itself must be cleaned up
        FileUtils.deleteDirectory(selectedPath.toFile());
        MovieUpdateDatasourceTask.forPaths(List.of(selectedPath)).run();

        assertThat(movieList.findFirstByPath(movie)).isNull();
    }

    @Test
    public void pathUpdateSkipsWhenDataSourceIsMissing() throws Exception {
        Path dataSource = Files.createDirectories(getWorkFolder().resolve("movies"));
        Path selectedPath = Files.createDirectories(dataSource.resolve("scifi"));
        Path movie = createMovie(selectedPath, "Offline Movie (2020)");

        setDataSource(dataSource);
        updateAllDataSources().run();

        MovieList movieList = getMovieList();
        assertThat(movieList.findFirstByPath(movie)).isNotNull();

        // a missing data source root is treated as an unavailable storage
        FileUtils.deleteDirectory(dataSource.toFile());
        MovieUpdateDatasourceTask.forPaths(List.of(selectedPath)).run();

        assertThat(movieList.findFirstByPath(movie)).isNotNull();
    }

    @Test
    public void pathUpdateSkipsWhenDataSourceIsEmpty() throws Exception {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        Path dataSource = Files.createDirectories(getWorkFolder().resolve("movies"));
        Path selectedPath = Files.createDirectories(dataSource.resolve("scifi"));
        Path movie = createMovie(selectedPath, "Offline Movie (2020)");

        setDataSource(dataSource);
        updateAllDataSources().run();

        MovieList movieList = getMovieList();
        assertThat(movieList.findFirstByPath(movie)).isNotNull();

        // an empty data source root is treated as an unavailable storage
        FileUtils.deleteDirectory(selectedPath.toFile());
        MovieUpdateDatasourceTask.forPaths(List.of(selectedPath)).run();

        assertThat(movieList.findFirstByPath(movie)).isNotNull();
    }

    private MovieUpdateDatasourceTask updateAllDataSources() {
        return MovieUpdateDatasourceTask.forAllDataSources();
    }

    private void setDataSource(Path dataSource) {
        MovieSettings settings = MovieModuleManager.getInstance().getSettings();
        settings.setMovieDataSources(List.of(dataSource.toString()));
    }

    private MovieList getMovieList() {
        return MovieModuleManager.getInstance().getMovieList();
    }

    private Path createMovie(Path parent, String folderName) throws Exception {
        Path movieFolder = Files.createDirectories(parent.resolve(folderName));
        Files.createFile(movieFolder.resolve("movie.mkv"));
        return movieFolder;
    }
}
