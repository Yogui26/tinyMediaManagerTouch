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
package org.tinymediamanager.core.tvshow.tasks;

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
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.tvshow.BasicTvShowTest;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowSettings;

/**
 * The class {@link TvShowUpdateDatasourceTaskMessageTest} checks that every data source which gets skipped during "update data sources" is pushed to
 * the {@link MessageManager} - so that the user gets a visible notice and not just a log line.
 */
public class TvShowUpdateDatasourceTaskMessageTest extends BasicTvShowTest {

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

  @Test
  public void unavailableDatasourceIsReported() throws Exception {
    Path missingDatasource = getWorkFolder().resolve("not-mounted").toAbsolutePath();

    TvShowSettings settings = TvShowModuleManager.getInstance().getSettings();
    settings.setTvShowDataSources(List.of(missingDatasource.toString()));

    new TvShowUpdateDatasourceTask().run();

    List<Message> messages = findMessages("update.datasource.unavailable");
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getMessageLevel()).isEqualTo(MessageLevel.ERROR);
    assertThat(messages.get(0).getIdParams()).containsExactly(missingDatasource.toString());
  }

  @Test
  public void skipFolderDatasourceIsReported() throws Exception {
    Path skipFolderDatasource = Files.createDirectories(getWorkFolder().resolve("skipme"));

    TvShowSettings settings = TvShowModuleManager.getInstance().getSettings();
    settings.setSkipFolder(List.of("skipme"));
    settings.setTvShowDataSources(List.of(skipFolderDatasource.toString()));

    new TvShowUpdateDatasourceTask().run();

    List<Message> messages = findMessages("update.datasource.skipfolder");
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getMessageLevel()).isEqualTo(MessageLevel.WARN);
    assertThat(messages.get(0).getIdParams()).containsExactly(skipFolderDatasource.toString());
  }

  @Test
  public void nestedDatasourceIsReported() throws Exception {
    Path parentDatasource = Files.createDirectories(getWorkFolder().resolve("tvshows"));
    Path nestedDatasource = Files.createDirectories(parentDatasource.resolve("nested"));

    TvShowSettings settings = TvShowModuleManager.getInstance().getSettings();
    // simulate an already broken setup (nested data source added before the guard existed)
    settings.setTvShowDataSources(List.of(parentDatasource.toString(), nestedDatasource.toString()));

    // the nested data source is detected while preparing the task
    new TvShowUpdateDatasourceTask();

    List<Message> messages = findMessages("update.datasource.nested.skipped");
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getMessageLevel()).isEqualTo(MessageLevel.WARN);
    assertThat(messages.get(0).getIdParams()).containsExactly(nestedDatasource.toString());
  }

  private List<Message> findMessages(String messageId) {
    return collectedMessages.stream().filter(m -> messageId.equals(m.getMessageId())).toList();
  }
}
