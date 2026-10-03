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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.movie.entities.MovieSet;

public class MovieSetExporterTest extends BasicMovieTest {

  @Test
  public void exportIllegalCharactersInSetTitle() throws Exception {
    TmmModuleManager.getInstance().startUp();
    MovieModuleManager.getInstance().startUp();
    try {
      Path templateDir = getWorkFolder().resolve("template");
      Files.createDirectories(templateDir);
      Files.writeString(templateDir.resolve("template.conf"), "name=Test\ntype=MOVIE_SET\nlist=list.jmte\ndetail=detail.jmte\nextension=html\n");
      Files.writeString(templateDir.resolve("list.jmte"), "list");
      Files.writeString(templateDir.resolve("detail.jmte"), "detail");

      MovieSet movieSet = new MovieSet();
      movieSet.setDbId(UUID.nameUUIDFromBytes("Alien: Romulus Collection".getBytes()));
      movieSet.setTitle("Alien: Romulus Collection");

      Path exportDir = getWorkFolder().resolve("export");
      new MovieSetExporter(templateDir).export(Collections.singletonList(movieSet), exportDir);

      List<String> subdirectories;
      try (Stream<Path> entries = Files.list(exportDir)) {
        subdirectories = entries.filter(Files::isDirectory).map(p -> p.getFileName().toString()).collect(Collectors.toList());
      }

      assertThat(subdirectories).containsExactly("Alien - Romulus Collection");
      assertThat(subdirectories).noneMatch(name -> name.contains(":"));
    }
    finally {
      MovieModuleManager.getInstance().shutDown();
      TmmModuleManager.getInstance().shutDown();
    }
  }
}
