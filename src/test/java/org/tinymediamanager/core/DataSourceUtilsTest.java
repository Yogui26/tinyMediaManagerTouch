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
package org.tinymediamanager.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

public class DataSourceUtilsTest {

  @Test
  public void detectsNestedDataSources() {
    Path parent = Paths.get("/media/movies").toAbsolutePath();
    Path nested = parent.resolve("archive");
    Path grandChild = nested.resolve("deep");
    Path unrelated = Paths.get("/media/series").toAbsolutePath();

    assertThat(DataSourceUtils.getNestedDataSources(List.of(parent.toString(), nested.toString(), grandChild.toString(), unrelated.toString())))
        .containsExactlyInAnyOrder(nested, grandChild);
  }

  @Test
  public void noNestedDataSourcesForFlatSetup() {
    Path a = Paths.get("/media/movies").toAbsolutePath();
    Path b = Paths.get("/media/series").toAbsolutePath();

    assertThat(DataSourceUtils.getNestedDataSources(List.of(a.toString(), b.toString()))).isEmpty();
  }

  @Test
  public void findsDeepestDataSourceForPath() {
    Path parent = Paths.get("/media/movies").toAbsolutePath();
    Path nested = parent.resolve("archive");

    List<String> dataSources = List.of(parent.toString(), nested.toString());

    assertThat(DataSourceUtils.getDataSourceForPath(dataSources, nested.resolve("Movie"))).isEqualTo(nested.toString());
    assertThat(DataSourceUtils.getDataSourceForPath(dataSources, parent.resolve("Movie"))).isEqualTo(parent.toString());
    assertThat(DataSourceUtils.getDataSourceForPath(dataSources, parent)).isEqualTo(parent.toString());
    assertThat(DataSourceUtils.getDataSourceForPath(dataSources, Paths.get("/somewhere/else"))).isNull();
  }

  @Test
  public void ignoresBlankAndInvalidDataSources() {
    Path parent = Paths.get("/media/movies").toAbsolutePath();

    assertThat(DataSourceUtils.getNestedDataSources(List.of("", "  ", parent.toString()))).isEmpty();
    assertThat(DataSourceUtils.getDataSourceForPath(List.of("", parent.toString()), parent.resolve("Movie"))).isEqualTo(parent.toString());
  }
}
