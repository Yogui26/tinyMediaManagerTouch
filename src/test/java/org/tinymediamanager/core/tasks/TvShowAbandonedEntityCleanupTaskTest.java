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
import org.tinymediamanager.core.tvshow.BasicTvShowTest;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;

/**
 * The class {@link TvShowAbandonedEntityCleanupTaskTest} verifies that the abandoned entity cleanup removes TV shows whose data source is no longer
 * configured - and that empty TV shows as well as all files are left untouched.
 */
public class TvShowAbandonedEntityCleanupTaskTest extends BasicTvShowTest {

  private final List<Message>    collectedMessages = Collections.synchronizedList(new ArrayList<>());
  private final IMessageListener messageListener   = collectedMessages::add;

  @Before
  public void setup() throws Exception {
    super.setup();

    TmmModuleManager.getInstance().startUp();
    TvShowModuleManager.getInstance().startUp();

    MessageManager.getInstance().addListener(messageListener);
  }

  @After
  public void tearDownAfterTest() throws Exception {
    MessageManager.getInstance().removeListener(messageListener);

    TvShowModuleManager.getInstance().shutDown();
    TmmModuleManager.getInstance().shutDown();
  }

  /**
   * Tests that a TV show of a data source which has been removed from the settings is taken out of the database together with its seasons and
   * episodes.
   */
  @Test
  public void removesShowWithoutConfiguredDatasource() {
    TvShow tvShow = addShow("Gone Show", getWorkFolder().resolve("gone"), 2);

    configureDatasources(getWorkFolder().resolve("stillthere"));

    new AbandonedEntityCleanupTask().run();

    assertThat(getTvShowList().getTvShows()).doesNotContain(tvShow);
  }

  /**
   * Tests that a locked TV show does not survive the cleanup of its data source.
   */
  @Test
  public void removesLockedShowAsWell() {
    TvShow tvShow = addShow("Locked Gone Show", getWorkFolder().resolve("gone"), 1);
    tvShow.setLocked(true);

    configureDatasources(getWorkFolder().resolve("stillthere"));

    new AbandonedEntityCleanupTask().run();

    assertThat(getTvShowList().getTvShows()).doesNotContain(tvShow);
  }

  /**
   * Tests that a TV show of a configured data source is kept while the show of a removed one is not.
   */
  @Test
  public void keepsShowOfConfiguredDatasource() {
    Path datasource = getWorkFolder().resolve("stillthere");
    TvShow keptShow = addShow("Kept Show", datasource, 1);
    TvShow goneShow = addShow("Gone Show", getWorkFolder().resolve("gone"), 1);

    configureDatasources(datasource);

    new AbandonedEntityCleanupTask().run();

    assertThat(getTvShowList().getTvShows()).containsExactly(keptShow);
  }

  /**
   * Tests that an empty TV show is not removed - emptiness is not the criterion of this cleanup.
   */
  @Test
  public void keepsEmptyShow() {
    TvShow emptyShow = addShow("Empty Show", getWorkFolder().resolve("stillthere"), 0);

    configureDatasources(getWorkFolder().resolve("stillthere"));

    new AbandonedEntityCleanupTask().run();

    assertThat(emptyShow.getEpisodeCount()).isZero();
    assertThat(getTvShowList().getTvShows()).containsExactly(emptyShow);
  }

  /**
   * Tests that a temporarily unavailable data source is no reason to remove anything - the check never looks at the filesystem.
   */
  @Test
  public void keepsShowsOfUnavailableButConfiguredDatasource() {
    Path notMounted = getWorkFolder().resolve("not-mounted");
    TvShow tvShow = addShow("Offline Nas Show", notMounted, 1);

    configureDatasources(notMounted);

    new AbandonedEntityCleanupTask().run();

    assertThat(getTvShowList().getTvShows()).containsExactly(tvShow);
  }

  /**
   * Tests that the whole check is skipped when no data source is configured at all, since that would empty the complete database.
   */
  @Test
  public void skipsCheckWithoutAnyConfiguredDatasource() {
    TvShow tvShow = addShow("Show Of The Last Datasource", getWorkFolder().resolve("gone"), 1);

    TvShowModuleManager.getInstance().getSettings().setTvShowDataSources(Collections.emptyList());

    new AbandonedEntityCleanupTask().run();

    assertThat(getTvShowList().getTvShows()).containsExactly(tvShow);

    List<Message> messages = findMessages("tmm.cleanupabandoned.skipped");
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getMessageLevel()).isEqualTo(MessageLevel.WARN);
  }

  /**
   * Tests that the cleanup only removes database entries and leaves all files of the abandoned TV show untouched.
   *
   * @throws Exception
   *           thrown on I/O errors
   */
  @Test
  public void neverDeletesAnyFile() throws Exception {
    Path datasource = getWorkFolder().resolve("gone");
    Path showFolder = datasource.resolve("Gone Show");
    Files.createDirectories(showFolder);
    Files.createFile(showFolder.resolve("tvshow.nfo"));
    Files.createFile(showFolder.resolve("poster.jpg"));

    addShow("Gone Show", datasource, 1);

    configureDatasources(getWorkFolder().resolve("stillthere"));

    new AbandonedEntityCleanupTask().run();

    assertThat(getTvShowList().getTvShows()).isEmpty();
    assertThat(showFolder.resolve("tvshow.nfo")).exists();
    assertThat(showFolder.resolve("poster.jpg")).exists();
  }

  /**
   * Tests that the summary of the cleanup is pushed as an info message.
   */
  @Test
  public void reportsTheRemovedEntries() {
    addShow("Gone Show", getWorkFolder().resolve("gone"), 1);
    TvShow keptShow = addShow("Kept Show", getWorkFolder().resolve("stillthere"), 1);

    configureDatasources(getWorkFolder().resolve("stillthere"));

    new AbandonedEntityCleanupTask().run();

    assertThat(getTvShowList().getTvShows()).containsExactly(keptShow);

    List<Message> messages = findMessages("tmm.cleanupabandoned.result");
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getMessageLevel()).isEqualTo(MessageLevel.INFO);
    assertThat(messages.get(0).getIdParams()).containsExactly("0", "1");
  }

  private TvShowList getTvShowList() {
    return TvShowModuleManager.getInstance().getTvShowList();
  }

  private void configureDatasources(Path... dataSources) {
    List<String> datasourcePaths = new ArrayList<>();
    for (Path dataSource : dataSources) {
      datasourcePaths.add(dataSource.toAbsolutePath().toString());
    }

    TvShowModuleManager.getInstance().getSettings().setTvShowDataSources(datasourcePaths);
  }

  private TvShow addShow(String title, Path datasource, int episodeCount) {
    TvShow tvShow = new TvShow();
    tvShow.setTitle(title);
    tvShow.setDataSource(datasource.toAbsolutePath().toString());
    tvShow.setPath(datasource.resolve(title).toAbsolutePath().toString());

    for (int i = 1; i <= episodeCount; i++) {
      TvShowEpisode episode = new TvShowEpisode();
      episode.setTvShow(tvShow);
      episode.setTitle("Episode " + i);
      episode.setEpisode(new MediaEpisodeNumber(MediaEpisodeGroup.DEFAULT_AIRED, 1, i));

      MediaFile mf = new MediaFile();
      mf.setType(MediaFileType.VIDEO);
      mf.setPath(tvShow.getPath());
      mf.setFilename(String.format("episode-%02d.mkv", i));
      episode.addToMediaFiles(mf);

      tvShow.addEpisode(episode);
    }

    getTvShowList().addTvShow(tvShow);

    return tvShow;
  }

  private List<Message> findMessages(String messageId) {
    return collectedMessages.stream().filter(message -> messageId.equals(message.getMessageId())).toList();
  }
}
