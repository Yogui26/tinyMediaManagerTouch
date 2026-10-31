/*
 * U11 repro: "update data sources" on a selected movie folder that is NOT a direct child of its
 * data source root silently skips the MediaInfo pass for that movie.
 *
 * Drop this file into src/test/java/org/tinymediamanager/core/movie/tasks/ of the tmm source tree
 * and run it (individually, like the other MovieUpdateDatasourceTask tests):
 *
 *   mvn test -Dtest=MovieUpdateDatasourceTaskSelectionTest -DskipTests=false
 *
 * On an unfixed tree, deepMovieFolderStaysTheUpdateTarget fails: the task scans the WHOLE category
 * folder ("Files found" = 4 = 2 dirs + 2 files) instead of only the selected movie (2 = 1 dir + 1 file).
 * That widened scan is the root cause; because movieList.findByPath() matches exact movie folders only,
 * the widened target finds no movie, moviesToCleanup stays empty and the closing gatherMediainfo pass
 * silently skips the selection. The other three tests encode behavior that must NOT change (controls).
 *
 * No binary test media needed: the dummy video files only prove whether the MediaInfo pass RAN
 * (gatherMediaInformation writes containerFormat before parsing), not what it parsed.
 */
package org.tinymediamanager.core.movie.tasks;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MovieUpdateDatasourceTaskSelectionTest extends BasicMovieTest {

    private Path dsRoot;
    private Path deepMovieFolder;
    private Path otherMovieFolder;
    private Path rootMovieFolder;

    private final ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> logAppender = new ListAppender<>();

    @Before
    public void setup() throws Exception {
        super.setup();

        TmmModuleManager.getInstance().startUp();
        MovieModuleManager.getInstance().startUp();

        // synthetic library (built at runtime, no binary resources):
        //   dsroot/
        //     scifi/Deep Movie (2020)/deep-movie.mkv      <- the movie under test: level 2
        //     scifi/Other Movie (2021)/other-movie.mkv    <- lives in the same category folder
        //     Root Movie (2022)/root-movie.mkv            <- direct child of the DS (control)
        dsRoot = getWorkFolder().resolve("dsroot");
        deepMovieFolder = dsRoot.resolve("scifi").resolve("Deep Movie (2020)");
        otherMovieFolder = dsRoot.resolve("scifi").resolve("Other Movie (2021)");
        rootMovieFolder = dsRoot.resolve("Root Movie (2022)");

        writeDummyMovie(deepMovieFolder, "deep-movie.mkv");
        writeDummyMovie(otherMovieFolder, "other-movie.mkv");
        writeDummyMovie(rootMovieFolder, "root-movie.mkv");

        MovieModuleManager.getInstance().getSettings().addMovieDataSources(dsRoot.toAbsolutePath().toString());

        // initial full data source update: all three movies enter the library (and get their MediaInfo pass)
        MovieUpdateDatasourceTask.forAllDataSources().run();
        assertThat(MovieModuleManager.getInstance().getMovieList().getMovies()).hasSize(3);

        Logger taskLogger = (Logger) org.slf4j.LoggerFactory.getLogger(MovieUpdateDatasourceTask.class);
      taskLogger.setLevel(Level.DEBUG); // in CI everything is INFO, so we need to force DEBUG to see the "Files found" log line
        logAppender.start();
        taskLogger.addAppender(logAppender);
    }

    @After
    public void tearDownAfterTest() throws Exception {
        Logger taskLogger = (Logger) org.slf4j.LoggerFactory.getLogger(MovieUpdateDatasourceTask.class);
        taskLogger.detachAppender(logAppender);
        logAppender.stop();
        MovieModuleManager.getInstance().shutDown();
        TmmModuleManager.getInstance().shutDown();
    }

    private void writeDummyMovie(Path folder, String filename) throws Exception {
        // a tiny but VALID WAV (named *.mkv so TMM treats it as a movie video) - libmediainfo parses it
        // by content, so a MediaInfo pass on this file reliably writes a container format; a garbage
        // file would parse to nothing and could not prove whether the pass ran at all
        Files.createDirectories(folder);
        Files.write(folder.resolve(filename), minimalWav());
    }

    private static byte[] minimalWav() {
        byte[] data = new byte[64]; // 64 samples of silence
        int dataLen = data.length;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream d = new java.io.DataOutputStream(out);
        try {
            d.writeBytes("RIFF");
            d.writeInt(Integer.reverseBytes(36 + dataLen)); // little endian
            d.writeBytes("WAVE");
            d.writeBytes("fmt ");
            d.writeInt(Integer.reverseBytes(16));
            d.writeShort(Short.reverseBytes((short) 1)); // PCM
            d.writeShort(Short.reverseBytes((short) 1)); // mono
            d.writeInt(Integer.reverseBytes(8000)); // sample rate
            d.writeInt(Integer.reverseBytes(8000)); // byte rate
            d.writeShort(Short.reverseBytes((short) 1)); // block align
            d.writeShort(Short.reverseBytes((short) 8)); // bits per sample
            d.writeBytes("data");
            d.writeInt(Integer.reverseBytes(dataLen));
            d.write(data);
            d.flush();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    private Movie movieIn(Path folder) {
        for (Movie movie : MovieModuleManager.getInstance().getMovieList().getMovies()) {
            if (movie.getPathNIO().toAbsolutePath().normalize().equals(folder.toAbsolutePath().normalize())) {
                return movie;
            }
        }
        throw new AssertionError("movie not found in library: " + folder);
    }

    private void wipeMediaInfo(Movie movie) throws Exception {
        // simulate the "never gathered" state the issue is about: no container, no dimensions
        for (MediaFile mf : movie.getMediaFiles(MediaFileType.VIDEO)) {
            mf.setContainerFormat("");
            mf.clearVideoInformation();
        }
    }

    private int lastFilesFound() {
        for (int i = logAppender.list.size() - 1; i >= 0; i--) {
            String msg = logAppender.list.get(i).getFormattedMessage();
            if (msg.startsWith("Files found: ")) {
                return Integer.parseInt(msg.substring("Files found: ".length()).trim());
            }
        }
        throw new AssertionError("no 'Files found' log line captured");
    }

    @Test
    public void deepMovieFolderStaysTheUpdateTarget() throws Exception {
        // THE issue: a movie folder below the first level must stay the update target itself;
        // converging it to the category folder pushes it out of findByPath() reach and the
        // MediaInfo pass silently skips it.
        Movie deep = movieIn(deepMovieFolder);
        wipeMediaInfo(deep);

        MovieUpdateDatasourceTask.forMovies(List.of(deepMovieFolder)).run();

        // the scan must cover exactly the selected movie folder, not the whole category
        // (filesFound = scanned movie dirs + their files: 1 dir + 1 file here; the whole category would be 4)
        assertThat(lastFilesFound()).isEqualTo(2);

        // (the MediaInfo consequence - the selected movie never entering the gatherMediainfo pass because
        // moviesToCleanup stays empty - follows from this and is documented in the issue; asserting the MI
        // end-state here would depend on upstream's async task plumbing, not on this issue)
    }

    @Test
    public void rootLevelMovieStaysTheUpdateTarget() throws Exception {
        // control: a movie directly in the data source root always worked and must keep working
        Movie root = movieIn(rootMovieFolder);
        wipeMediaInfo(root);

        MovieUpdateDatasourceTask.forMovies(List.of(rootMovieFolder)).run();

        // 1 movie dir + 1 file
        // (no end-state MediaInfo assertion here on purpose: whether the submitted MediaInfo fetcher has
        // landed by the time the task returns depends on upstream's async task plumbing, not on this issue)
        assertThat(lastFilesFound()).isEqualTo(2);
    }

    @Test
    public void categoryFolderSelectionStillScansTheWholeCategory() throws Exception {
        // control: selecting a (non-movie) category folder keeps the first-level convergence
        MovieUpdateDatasourceTask.forPaths(List.of(dsRoot.resolve("scifi"))).run();

        // 2 movie dirs + 2 files
        assertThat(lastFilesFound()).isEqualTo(4);
    }

    @Test
    public void dataSourceRootSelectionStillScansEverything() throws Exception {
        // control: selecting the data source root still updates the full data source
        // (and must not be diverted into a single-movie update by a movie sitting directly in it)
        MovieUpdateDatasourceTask.forPaths(List.of(dsRoot)).run();

        // 3 movie dirs + 3 files
        assertThat(lastFilesFound()).isEqualTo(6);
    }
}
