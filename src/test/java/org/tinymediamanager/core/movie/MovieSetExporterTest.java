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
import java.util.Arrays;
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

      MovieSet colonSet = new MovieSet();
      colonSet.setDbId(UUID.nameUUIDFromBytes("Alien: Romulus Collection".getBytes()));
      colonSet.setTitle("Alien: Romulus Collection");

      // a set title containing a path separator used to be resolved as a nested path, which made
      // 'Files.createDirectory' throw NoSuchFileException and silently abort the whole export
      MovieSet slashSet = new MovieSet();
      slashSet.setDbId(UUID.nameUUIDFromBytes("Superman / Batman (Animated) Collection".getBytes()));
      slashSet.setTitle("Superman / Batman (Animated) Collection");

      MovieSet backslashSet = new MovieSet();
      backslashSet.setDbId(UUID.nameUUIDFromBytes("Superman \\ Batman (Animated) Collection".getBytes()));
      backslashSet.setTitle("Superman \\ Batman (Animated) Collection");

      Path exportDir = getWorkFolder().resolve("export");
      new MovieSetExporter(templateDir).export(Arrays.asList(colonSet, slashSet, backslashSet), exportDir);

      List<String> subdirectories;
      try (Stream<Path> entries = Files.list(exportDir)) {
        subdirectories = entries.filter(Files::isDirectory).map(p -> p.getFileName().toString()).collect(Collectors.toList());
      }

      assertThat(subdirectories).containsExactlyInAnyOrder("Alien - Romulus Collection", "Superman Batman (Animated) Collection");
      assertThat(subdirectories).noneMatch(name -> name.contains(":") || name.contains("/") || name.contains("\\"));
      // the list file and all detail files must still be present after the loop
      assertThat(exportDir.resolve("moviesets.html")).exists();
    }
    finally {
      MovieModuleManager.getInstance().shutDown();
      TmmModuleManager.getInstance().shutDown();
    }
  }
}
