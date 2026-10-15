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
import org.tinymediamanager.core.movie.BasicMovieTest;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;

/**
 * The class {@link MovieUpdateDatasourceTaskMessageTest} checks that every data source which gets skipped during "update data sources" is pushed to
 * the {@link MessageManager} - so that the user gets a visible notice and not just a log line.
 */
public class MovieUpdateDatasourceTaskMessageTest extends BasicMovieTest {

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

  @Test
  public void unavailableDatasourceIsReported() throws Exception {
    Path missingDatasource = getWorkFolder().resolve("not-mounted").toAbsolutePath();

    MovieSettings settings = MovieModuleManager.getInstance().getSettings();
    settings.setMovieDataSources(List.of(missingDatasource.toString()));

    new MovieUpdateDatasourceTask().run();

    List<Message> messages = findMessages("update.datasource.unavailable");
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getMessageLevel()).isEqualTo(MessageLevel.ERROR);
    assertThat(messages.get(0).getIdParams()).containsExactly(missingDatasource.toString());
  }

  @Test
  public void skipFolderDatasourceIsReported() throws Exception {
    Path skipFolderDatasource = Files.createDirectories(getWorkFolder().resolve("skipme"));

    MovieSettings settings = MovieModuleManager.getInstance().getSettings();
    settings.setSkipFolder(List.of("skipme"));
    settings.setMovieDataSources(List.of(skipFolderDatasource.toString()));

    new MovieUpdateDatasourceTask().run();

    List<Message> messages = findMessages("update.datasource.skipfolder");
    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).getMessageLevel()).isEqualTo(MessageLevel.WARN);
    assertThat(messages.get(0).getIdParams()).containsExactly(skipFolderDatasource.toString());
  }

  @Test
  public void nestedDatasourceIsNotReported() throws Exception {
    Path parentDatasource = Files.createDirectories(getWorkFolder().resolve("movies"));
    Path nestedDatasource = Files.createDirectories(parentDatasource.resolve("nested"));

    MovieSettings settings = MovieModuleManager.getInstance().getSettings();
    // nested data sources are allowed now - they must not be reported as skipped/removed
    settings.setMovieDataSources(List.of(parentDatasource.toString(), nestedDatasource.toString()));

    new MovieUpdateDatasourceTask().run();

    assertThat(findMessages("update.datasource.nested.skipped")).isEmpty();
    assertThat(findMessages("update.datasource.nested.removed")).isEmpty();
  }

  private List<Message> findMessages(String messageId) {
    return collectedMessages.stream().filter(m -> messageId.equals(m.getMessageId())).toList();
  }
}
