package org.tinymediamanager.core.movie;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.filenaming.MoviePosterNaming;
import org.tinymediamanager.core.movie.tasks.MovieUpdateDatasourceTask;

public class MovieRenamerTest extends BasicMovieTest {

  private void initTest() throws Exception {
    // setup
    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();

    // just a copy; we might have another movie test which uses these files
    copyResourceFolderToWorkFolder("testmovies_rename");
    Path datasource = getWorkFolder().resolve("testmovies_rename").toAbsolutePath();
    MovieModuleManager.getInstance().getSettings().addMovieDataSources(datasource.toString());

    // read all movies via UDS - all needed infos are embedded in NFO
    MovieUpdateDatasourceTask task = new MovieUpdateDatasourceTask();
    task.run();

    MovieList movieList = MovieList.getInstance();
    assertThat(movieList.getMovies().size()).isEqualTo(8);

    MovieSettingsDefaults.setDefaultSettingsForKodi();
  }

  private void finishTest() throws Exception {
    MovieModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  @Test
  public void testRenameSingleMovieFolder() throws Exception {
    try {
      initTest();

      MovieModuleManager.getInstance()
          .getSettings()
          .getDefaultRenamerProfile()
          .setRenamerPathname("${titleSortable;first}/${titleSortable} ${- ,edition,} (${year}) [${videoFormat}]");
      MovieModuleManager.getInstance()
          .getSettings()
          .getDefaultRenamerProfile()
          .setRenamerFilename("${title} ${- ,edition,} (${year}) [${videoFormat}] [${videoCodec}] [${audioCodec}]");

      UdsMiRenamerExample example = new UdsMiRenamerExample("Single", "Aladdin", 1992, "singlefile.avi", "1080p", "h264", "A/Aladdin (1992) [1080p]");
      example.oldFiles = new String[] { "singlefile.avi", "fanart.png", "movie.nfo", "singlefile-poster.png", "trailer.mp4", "extras/cut-scenes.mkv",
          "trailer/trailer2.mp4" };
      String movieBasename = "Aladdin (1992) [1080p] [h264] [DTS]";
      example.newFiles = new String[] { movieBasename + ".avi", movieBasename + "-fanart.png", movieBasename + ".nfo", movieBasename + "-poster.png",
          movieBasename + "-trailer.mp4", "extras/cut-scenes.mkv", "trailer/trailer2.mp4" };
      checkExample(example);
    }
    finally {
      finishTest();
    }
  }

  @Test
  public void testRenameSingleMovieFolderOnlyCleanup() throws Exception {
    try {
      initTest();

      MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile().setRenamerPathname("");
      MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile().setRenamerFilename("");

      UdsMiRenamerExample example = new UdsMiRenamerExample("Single", "Aladdin", 1992, "singlefile.avi", "1080p", "h264", "Single");
      example.oldFiles = new String[] { "singlefile.avi", "fanart.png", "movie.nfo", "singlefile-poster.png", "trailer.mp4", "extras/cut-scenes.mkv",
          "trailer/trailer2.mp4" };
      String movieBasename = "singlefile";
      example.newFiles = new String[] { movieBasename + ".avi", movieBasename + "-fanart.png", movieBasename + ".nfo", movieBasename + "-poster.png",
          movieBasename + "-trailer.mp4", "extras/cut-scenes.mkv", "trailer/trailer2.mp4" };
      checkExample(example);
    }
    finally {
      finishTest();
    }
  }

  @Test
  public void testRenameSingleMovieFolderMultipleArtwork() throws Exception {
    try {
      initTest();

      MovieModuleManager.getInstance()
          .getSettings()
          .getDefaultRenamerProfile()
          .setRenamerPathname("${titleSortable;first}/${titleSortable} ${- ,edition,} (${year}) [${videoFormat}]");
      MovieModuleManager.getInstance()
          .getSettings()
          .getDefaultRenamerProfile()
          .setRenamerFilename("${title} ${- ,edition,} (${year}) [${videoFormat}] [${videoCodec}] [${audioCodec}]");

      MovieModuleManager.getInstance().getSettings().addPosterFilename(MoviePosterNaming.FOLDER);

      UdsMiRenamerExample example = new UdsMiRenamerExample("Single", "Aladdin", 1992, "singlefile.avi", "1080p", "h264", "A/Aladdin (1992) [1080p]");
      example.oldFiles = new String[] { "singlefile.avi", "fanart.png", "movie.nfo", "singlefile-poster.png", "trailer.mp4", "extras/cut-scenes.mkv",
          "trailer/trailer2.mp4" };
      String movieBasename = "Aladdin (1992) [1080p] [h264] [DTS]";
      example.newFiles = new String[] { movieBasename + ".avi", movieBasename + "-fanart.png", movieBasename + ".nfo", movieBasename + "-poster.png",
          "folder.png", movieBasename + "-trailer.mp4", "extras/cut-scenes.mkv", "trailer/trailer2.mp4" };
      checkExample(example);
    }
    finally {
      finishTest();
    }
  }

  @Test
  public void testRenameMultiMovieFolderToSingleMovieFolder() throws Exception {
    try {
      initTest();

      MovieModuleManager.getInstance()
          .getSettings()
          .getDefaultRenamerProfile()
          .setRenamerPathname("${titleSortable} ${- ,edition,} (${year}) [${videoFormat}]");
      MovieModuleManager.getInstance()
          .getSettings()
          .getDefaultRenamerProfile()
          .setRenamerFilename("${title} ${- ,edition,} (${year}) [${videoFormat}] [${videoCodec}] [${audioCodec}]");

      UdsMiRenamerExample example = new UdsMiRenamerExample("MultipleVideosMMD", "Aladdin", 1992, "multiv-movie1.avi", "1080p", "h264",
          "Aladdin (1992) [1080p]");
      example.oldFiles = new String[] { "multiv-movie1.avi", "multiv-movie1.nfo", "multiv-movie1-poster.png" };
      String movieBasename = "Aladdin (1992) [1080p] [h264] [DTS]";
      example.newFiles = new String[] { movieBasename + ".avi", movieBasename + ".nfo", movieBasename + "-poster.png" };
      checkExample(example);
    }
    finally {
      finishTest();
    }
  }

  @Test
  public void testMMDReevaluationAfterMergingIntoSingleMovieFolder() throws Exception {
    try {
      initTest();

      // a NON-unique folder pattern will merge the movie into the existing folder -> MMD
      MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile().setRenamerPathname("Single");
      MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile().setRenamerFilename("");

      MovieList movieList = MovieList.getInstance();
      Path datasource = Paths.get(MovieModuleManager.getInstance().getSettings().getMovieDataSource().get(0));

      Movie singleMovie = getMovieByMainVideo(movieList, datasource, "Single", "singlefile.avi");
      Movie movieToMerge = getMovieByMainVideo(movieList, datasource, "MultipleVideosMMD", "multiv-movie2.avi");
      assertThat(singleMovie).isNotNull();
      assertThat(movieToMerge).isNotNull();
      assertThat(singleMovie.isMultiMovieDir()).isFalse();

      Set<Path> touchedFolders = new LinkedHashSet<>();
      touchedFolders.add(movieToMerge.getPathNIO().toAbsolutePath());
      MovieRenamer.renameMovie(movieToMerge, MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile());
      touchedFolders.add(movieToMerge.getPathNIO().toAbsolutePath());

      // the renamer only updates the flag of the renamed movie itself -> still stale at this point
      assertThat(singleMovie.isMultiMovieDir()).isFalse();

      // simulate the post-rename re-check done by the rename task
      movieList.reevaluateMMDForFolders(touchedFolders);

      // existing single movie now shares the folder -> must be treated as MMD
      assertThat(singleMovie.isMultiMovieDir()).isTrue();
    }
    finally {
      finishTest();
    }
  }

  @Test
  public void testMMDReevaluationAfterMovingMovieOutOfSharedFolder() throws Exception {
    try {
      initTest();

      MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile().setRenamerPathname("${titleSortable} (${year})");
      MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile().setRenamerFilename("");

      MovieList movieList = MovieList.getInstance();
      Path datasource = Paths.get(MovieModuleManager.getInstance().getSettings().getMovieDataSource().get(0));

      Movie movie1 = getMovieByMainVideo(movieList, datasource, "MultipleVideosMMD", "multiv-movie1.avi");
      Movie movie2 = getMovieByMainVideo(movieList, datasource, "MultipleVideosMMD", "multiv-movie2.avi");
      Movie movie3 = getMovieByMainVideo(movieList, datasource, "MultipleVideosMMD", "multiv-movie3.avi");
      assertThat(movie1).isNotNull();
      assertThat(movie2).isNotNull();
      assertThat(movie3).isNotNull();

      Set<Path> touchedFolders = new LinkedHashSet<>();
      touchedFolders.add(movie1.getPathNIO().toAbsolutePath());

      // move 2 of the 3 movies out of the shared folder
      touchedFolders.add(movie2.getPathNIO().toAbsolutePath());
      MovieRenamer.renameMovie(movie2, MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile());
      touchedFolders.add(movie2.getPathNIO().toAbsolutePath());

      // still 2 movies left -> MMD is still correct
      movieList.reevaluateMMDForFolders(touchedFolders);
      assertThat(movie1.isMultiMovieDir()).isTrue();

      touchedFolders.add(movie3.getPathNIO().toAbsolutePath());
      MovieRenamer.renameMovie(movie3, MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile());
      touchedFolders.add(movie3.getPathNIO().toAbsolutePath());

      // only one movie left, but the stale flag is not touched by the renamer
      assertThat(movie1.isMultiMovieDir()).isTrue();

      // simulate the post-rename re-check done by the rename task
      movieList.reevaluateMMDForFolders(touchedFolders);

      // the last movie left behind can be upgraded to a single movie folder
      assertThat(movie1.isMultiMovieDir()).isFalse();
    }
    finally {
      finishTest();
    }
  }

  @Test
  public void testRenameIntoNestedDatasource() throws Exception {
    try {
      initTest();

      MovieList movieList = MovieList.getInstance();
      Path parentDs = getWorkFolder().resolve("testmovies_rename").toAbsolutePath();
      Path nestedDs = parentDs.resolve("archive");
      Files.createDirectories(nestedDs);

      // a data source nested inside the parent one is allowed now
      assertThat(MovieModuleManager.getInstance().getSettings().addMovieDataSources(nestedDs.toString())).isTrue();

      Movie movie = movieList.findFirstByPath(parentDs.resolve("Single"));
      assertThat(movie).isNotNull();
      assertThat(movie.getDataSource()).isEqualTo(parentDs.toString());

      // rename/move the movie into the nested data source
      MovieRenamerProfile profile = MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile();
      profile.setRenamerPathname("archive/${titleSortable} (${year})");
      profile.setRenamerFilename("${title} (${year})");

      MovieRenamer.renameMovie(movie, profile);

      // the movie is now located inside the nested data source - the data source field must follow
      assertThat(movie.getPathNIO().startsWith(nestedDs)).isTrue();
      assertThat(Paths.get(movie.getDataSource())).isEqualTo(nestedDs);
    }
    finally {
      finishTest();
    }
  }

  @Test
  public void testMMDReevaluationForNestedMovieFolders() throws Exception {
    try {
      initTest();

      MovieList movieList = MovieList.getInstance();
      String datasource = Paths.get(MovieModuleManager.getInstance().getSettings().getMovieDataSource().get(0)).toString();

      // imagine a structure like
      // ds/Nested/parent.mkv
      // ds/Nested/child/child.mkv
      // the parent MUST be treated as MMD, as long as the child lives below it
      Movie parent = new Movie();
      parent.setTitle("Nested Parent");
      parent.setDataSource(datasource);
      parent.setPath(Paths.get(datasource, "Nested").toString());
      parent.setMultiMovieDir(true);
      Movie child = new Movie();
      child.setTitle("Nested Child");
      child.setDataSource(datasource);
      child.setPath(Paths.get(datasource, "Nested", "Child").toString());
      child.setMultiMovieDir(false);
      movieList.addMovie(parent);
      movieList.addMovie(child);

      // sanity: full sweep keeps the parent as MMD because of the nested child
      movieList.reevaluateMMD();
      assertThat(parent.isMultiMovieDir()).isTrue();
      assertThat(child.isMultiMovieDir()).isFalse();

      // simulate the child moving out of the nested folder; only its old folder is touched
      Path childOldFolder = Paths.get(datasource, "Nested", "Child");
      child.setPath(Paths.get(datasource, "Child").toString());
      movieList.reevaluateMMDForFolders(Set.of(childOldFolder));

      // no movies below the parent folder anymore -> can be downgraded to a single movie folder
      assertThat(parent.isMultiMovieDir()).isFalse();
    }
    finally {
      finishTest();
    }
  }

  private Movie getMovieByMainVideo(MovieList movieList, Path datasource, String folder, String mainVideoFilename) {
    for (Movie m : movieList.findByPath(datasource.resolve(folder))) {
      if (m.getMainVideoFile() != null && m.getMainVideoFile().getFilename().equals(mainVideoFilename)) {
        return m;
      }
    }
    return null;
  }

  @Test
  public void testRenameBdmv() throws Exception {
    try {
      initTest();

      MovieModuleManager.getInstance()
          .getSettings()
          .getDefaultRenamerProfile()
          .setRenamerPathname("${titleSortable;first}/${titleSortable} ${- ,edition,} (${year}) [${videoFormat}]");
      MovieModuleManager.getInstance()
          .getSettings()
          .getDefaultRenamerProfile()
          .setRenamerFilename("${title} ${- ,edition,} (${year}) [${videoFormat}] [${videoCodec}] [${audioCodec}]");

      UdsMiRenamerExample example = new UdsMiRenamerExample("BluRay", "Brave", 2012, "BDMV", "2160p", "h265", "B/Brave (2012) [2160p]");
      example.oldFiles = new String[] { "BDMV", "BDMV/index.nfo", "fanart.png", "poster.png", "trailer.mp4", "extras/cut-scenes.mkv",
          "trailer/trailer2.mp4" };
      example.newFiles = new String[] { "BDMV", "BDMV/index.nfo", "fanart.png", "poster.png", "BDMV/index-trailer.mp4", "extras/cut-scenes.mkv",
          "trailer/trailer2.mp4" };
      checkExample(example);
    }
    finally {
      finishTest();
    }
  }

  @Test
  public void testRenameVideoTS() throws Exception {
    try {
      initTest();

      MovieModuleManager.getInstance()
          .getSettings()
          .getDefaultRenamerProfile()
          .setRenamerPathname("${titleSortable;first}/${titleSortable} ${- ,edition,} (${year}) [${videoFormat}]");
      MovieModuleManager.getInstance()
          .getSettings()
          .getDefaultRenamerProfile()
          .setRenamerFilename("${title} ${- ,edition,} (${year}) [${videoFormat}] [${videoCodec}] [${audioCodec}]");

      UdsMiRenamerExample example = new UdsMiRenamerExample("DVDfolder", "Cars", 2006, "VIDEO_TS", "576p", "MPEG-2", "C/Cars (2006) [576p]");
      example.oldFiles = new String[] { "VIDEO_TS", "movie.nfo", "fanart.png", "poster.png", "trailer.mp4", "extras/cut-scenes.mkv",
          "trailer/trailer2.mp4" };
      example.newFiles = new String[] { "VIDEO_TS", "VIDEO_TS/VIDEO_TS.nfo", "fanart.png", "poster.png", "VIDEO_TS/VIDEO_TS-trailer.mp4",
          "extras/cut-scenes.mkv", "trailer/trailer2.mp4" };
      checkExample(example);
    }
    finally {
      finishTest();
    }
  }

  private void checkExample(UdsMiRenamerExample example) {
    MovieRenamerProfile defaultRenamerProfile = MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile();

    MovieList movieList = MovieList.getInstance();
    Path datasource = Paths.get(MovieModuleManager.getInstance().getSettings().getMovieDataSource().get(0));

    Movie movie = null;
    for (Movie m : movieList.findByPath(datasource.resolve(example.oldFolder))) {
      if (m.getMainVideoFile().getFilename().equals(example.mainVideoFilename)) {
        movie = m;
        break;
      }
    }

    assertThat(movie).isNotNull();
    assertThat(movie.getTitle()).isEqualTo(example.title);
    assertThat(movie.getYear()).isEqualTo(example.year);

    // test UDS
    Path moviePath = datasource.resolve(example.oldFolder);
    checkFiles(moviePath, example.oldFiles);

    // test MI
    MediaFile mainVideoFile = movie.getMainVideoFile();
    assertThat(mainVideoFile.getFile()).isEqualTo(moviePath.resolve(example.mainVideoFilename));
    assertThat(mainVideoFile.getVideoFormat()).isEqualTo(example.videoFormat);
    assertThat(mainVideoFile.getVideoCodec()).isEqualTo(example.videoCodec);

    // the main video must never disappear from the movie while the media files are being replaced
    Movie movieToRename = movie;
    List<MediaFile> mainVideoFilesDuringRename = new ArrayList<>();
    movieToRename.addPropertyChangeListener(evt -> {
      if (Constants.MEDIA_FILES.equals(evt.getPropertyName())) {
        mainVideoFilesDuringRename.add(movieToRename.getMainVideoFile());
      }
    });

    // rename the movie
    MovieRenamer.renameMovie(movie, defaultRenamerProfile);
    assertThat(mainVideoFilesDuringRename).isNotEmpty().noneMatch(mediaFile -> mediaFile == MediaFile.EMPTY_MEDIAFILE);
    moviePath = datasource.resolve(example.newFolder);
    assertThat(movie.getPathNIO()).isEqualTo(moviePath);
    checkFiles(moviePath, example.newFiles);

    // undo rename (if changes were made)
    MovieRenamer.undoRename(movie);

    if (!example.oldFolder.equals(example.newFolder)) {
      assertThat(moviePath).doesNotExist();
      moviePath = datasource.resolve(example.oldFolder);
      assertThat(movie.getPathNIO()).isEqualTo(moviePath);
    }

    checkFiles(moviePath, example.oldFiles);
  }

  private void checkFiles(Path moviePath, String... filenames) {
    for (String filename : filenames) {
      Path filePath = moviePath.resolve(filename);
      assertThat(filePath).exists();
    }
  }

  @Test
  public void testSpecialCases() {
    MovieRenamerProfile defaultRenamerProfile = MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile();

    assertEqual("jb - the bla", MovieRenamer.replaceInvalidCharacters("jb: the bla", defaultRenamerProfile));
    assertEqual("jb  - the bla", MovieRenamer.replaceInvalidCharacters("jb : the bla", defaultRenamerProfile));
    assertEqual("2-22", MovieRenamer.replaceInvalidCharacters("2:22", defaultRenamerProfile));
    assertEqual("2 -22", MovieRenamer.replaceInvalidCharacters("2 :22", defaultRenamerProfile));

    // we do not strip path separators here
    assertEqual("weird \\\\-/ movie", MovieRenamer.replaceInvalidCharacters("weird \"\\\\:<>|/?* movie", defaultRenamerProfile));
  }

  @Test
  public void testRenamerToken() throws Exception {
    MovieRenamerProfile defaultRenamerProfile = MovieModuleManager.getInstance().getSettings().getDefaultRenamerProfile();

    // MediaInfoUtils.loadMediaInfo(); // no MI on buildserver
    copyResourceFolderToWorkFolder("samples");

    Movie m = new Movie();
    m.setTitle("The Dish");
    m.setOriginalTitle("The Original Dish");
    m.setEnglishTitle("The English Dish");
    m.setYear(2000);
    MediaFile mf = new MediaFile(getWorkFolder().resolve("samples").resolve("thx_scarface-DWEU.vob"));

    // mf.gatherMediaInformation();
    mf.setVideoCodec("MPEG");
    mf.setVideoHeight(480);
    mf.setVideoWidth(720);
    ArrayList<MediaFileAudioStream> audl = new ArrayList<>();
    MediaFileAudioStream aud = new MediaFileAudioStream();
    aud.setAudioChannels(6);
    aud.setCodec("AC3");
    aud.setLanguage("en");
    audl.add(aud);
    mf.setAudioStreams(audl);

    aud = new MediaFileAudioStream();
    aud.setAudioChannels(2);
    aud.setCodec("MP3");
    aud.setLanguage("de");
    audl.add(aud);
    mf.setAudioStreams(audl);

    m.addToMediaFiles(mf);

    assertEqual("The Dish (2000) MPEG-480p AC3-6ch", MovieRenamer
        .createDestinationForFilename("${title} (${year}) ${videoCodec}-${videoFormat} ${audioCodec}-${audioChannels}", defaultRenamerProfile, m));
    assertEqual("The Dish (2000)", MovieRenamer.createDestinationForFoldername("${title} (${year})", defaultRenamerProfile, m));
    assertEqual("_The Dish (2000)", MovieRenamer.createDestinationForFoldername("${_,title,} (${year})", defaultRenamerProfile, m));
    assertEqual("The Dish (2000)", MovieRenamer.createDestinationForFoldername(".${title} (${year})", defaultRenamerProfile, m));
    assertEqual("The Dish (2000)", MovieRenamer.createDestinationForFoldername("-${title} (${year})-", defaultRenamerProfile, m));
    assertEqual("2000-2009", MovieRenamer.createDestinationForFoldername("${decadeLong}", defaultRenamerProfile, m));
    assertEqual("2000s", MovieRenamer.createDestinationForFoldername("${decadeShort}", defaultRenamerProfile, m));

    // IF with 2 variables
    assertEqual("The Original Dish - The English Dish", MovieRenamer.createDestinationForFoldername(
        "${originalTitle}${if englishTitle}${if englishTitle = originalTitle}${else} - ${englishTitle}${end}${end}", defaultRenamerProfile, m));
    m.setEnglishTitle(m.getOriginalTitle());
    assertEqual("The Original Dish", MovieRenamer.createDestinationForFoldername(
        "${originalTitle}${if englishTitle}${if englishTitle = originalTitle}${else} - ${englishTitle}${end}${end}", defaultRenamerProfile, m));
  }

  /**
   * A skiptro sidecar must be renamed to "&lt;new video basename&gt;.skiptro.json" (without the videos container extension) - that is the naming
   * convention of the Kodi addon "service.skiptro"
   */
  @Test
  public void testGenerateFilenameForSkiptroSidecar() {
    Path movieDir = getWorkFolder().resolve("Old Movie");

    Movie movie = new Movie();
    movie.setDataSource(getWorkFolder().toString());
    movie.setPath(movieDir.toString());
    movie.setTitle("New Movie");
    movie.setYear(2020);

    MediaFile skiptro = new MediaFile();
    skiptro.setType(MediaFileType.SKIPTRO);
    skiptro.setPath(movieDir.toString());
    skiptro.setFilename("oldmovie.skiptro.json");

    List<MediaFile> renamed = MovieRenamer.generateFilename(movie, skiptro, "New Movie (2020)", new MovieRenamerProfile());

    assertThat(renamed).hasSize(1);
    assertThat(renamed.get(0).getFilename()).isEqualTo("New Movie (2020).skiptro.json");
  }

  /**
   * ScenePass sidecars must be renamed to "&lt;new video basename&gt;&lt;suffix&gt;" (without the video container extension) and keep their specific
   * suffix type.
   */
  @Test
  public void testGenerateFilenameForScenePassSidecar() {
    Path movieDir = getWorkFolder().resolve("Old Movie");

    Movie movie = new Movie();
    movie.setDataSource(getWorkFolder().toString());
    movie.setPath(movieDir.toString());
    movie.setTitle("New Movie");
    movie.setYear(2020);

    for (String filename : List.of("oldmovie.sp.json", "oldmovie.sp.hash.json")) {
      MediaFile scenePass = new MediaFile();
      scenePass.setType(MediaFileType.SCENEPASS);
      scenePass.setPath(movieDir.toString());
      scenePass.setFilename(filename);

      List<MediaFile> renamed = MovieRenamer.generateFilename(movie, scenePass, "New Movie (2020)", new MovieRenamerProfile());

      assertThat(renamed).hasSize(1);
      assertThat(renamed.get(0).getFilename()).isEqualTo("New Movie (2020)" + MediaFileHelper.getScenePassSuffix(filename));
    }
  }

  private static class UdsMiRenamerExample {
    // old locations (UDS & undo)
    String   oldFolder;
    String[] oldFiles;

    String   title;
    int      year;

    // MI values
    String   mainVideoFilename;
    String   videoFormat;
    String   videoCodec;

    // new locations (renamer)
    String   newFolder;
    String[] newFiles;

    public UdsMiRenamerExample(String oldFolder, String title, int year, String mainVideoFilename, String videoFormat, String videoCodec,
        String newFolder) {
      this.oldFolder = oldFolder;
      this.title = title;
      this.year = year;
      this.mainVideoFilename = mainVideoFilename;
      this.videoFormat = videoFormat;
      this.videoCodec = videoCodec;
      this.newFolder = newFolder;
    }
  }
}
