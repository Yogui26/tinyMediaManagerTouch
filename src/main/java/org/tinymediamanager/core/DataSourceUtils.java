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

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods for working with the configured data sources.
 * <p>
 * Data sources may be nested (a data source located inside another data source). Every data source owns its subtree: a scan of a data source must
 * never descend into a nested data source, and every entity belongs to the deepest matching data source.
 * </p>
 * <p>
 * <b>Note on symlinks:</b> all comparisons are done on lexically normalized absolute paths ({@link Path#normalize normalize()}), not on
 * {@link Path#toRealPath real paths}. Therefore, a data source which is only reachable through a symbolic link is not recognized as nested, and a
 * scan following that link may still produce duplicate entities. Keep data source folders free of symbolic links to other data sources.
 * </p>
 *
 * @author Manuel Laggner
 */
public final class DataSourceUtils {

  private DataSourceUtils() {
    // static utility only
    throw new IllegalAccessError();
  }

  /**
   * Normalizes a configured data source string into an absolute path.
   *
   * @param dataSource
   *          the data source as configured in the settings
   * @return the normalized absolute path, or null if the data source is blank or not a valid path
   */
  public static Path toPath(String dataSource) {
    if (StringUtils.isBlank(dataSource)) {
      return null;
    }

    try {
      return Paths.get(dataSource).normalize().toAbsolutePath();
    }
    catch (InvalidPathException e) {
      return null;
    }
  }

  /**
   * Determines all data sources which are nested inside another data source. Those data sources must be skipped when their parent data source is
   * scanned, since they own their subtree on their own.
   *
   * @param dataSources
   *          the configured data sources
   * @return the normalized absolute paths of all nested data sources
   */
  public static Set<Path> getNestedDataSources(Collection<String> dataSources) {
    Set<Path> normalized = normalize(dataSources);
    Set<Path> nested = new LinkedHashSet<>();

    for (Path dataSource : normalized) {
      for (Path other : normalized) {
        if (!dataSource.equals(other) && dataSource.startsWith(other)) {
          nested.add(dataSource);
          break;
        }
      }
    }

    return nested;
  }

  /**
   * Finds the deepest data source containing the given path.
   *
   * @param dataSources
   *          the (already normalized) data sources to check
   * @param path
   *          the path to find the data source for
   * @return the deepest matching data source, or null if the path is not inside any data source
   */
  public static Path getDeepestDataSource(Collection<Path> dataSources, Path path) {
    if (dataSources == null || path == null) {
      return null;
    }

    Path normalizedPath = path.normalize().toAbsolutePath();
    Path best = null;

    for (Path dataSource : dataSources) {
      if (dataSource != null && normalizedPath.startsWith(dataSource) && (best == null || dataSource.getNameCount() > best.getNameCount())) {
        best = dataSource;
      }
    }

    return best;
  }

  /**
   * Finds the deepest configured data source containing the given path.
   *
   * @param dataSources
   *          the configured data sources
   * @param path
   *          the path to find the data source for
   * @return the deepest matching data source as a normalized absolute path string, or null if the path is not inside any data source
   */
  public static String getDataSourceForPath(Collection<String> dataSources, Path path) {
    Path best = getDeepestDataSource(normalize(dataSources), path);
    return best == null ? null : best.toString();
  }

  /**
   * Normalizes the given configured data sources, ignoring blank and invalid ones.
   *
   * @param dataSources
   *          the configured data sources
   * @return the normalized absolute paths
   */
  private static Set<Path> normalize(Collection<String> dataSources) {
    Set<Path> normalized = new LinkedHashSet<>();
    if (dataSources == null) {
      return normalized;
    }

    for (String dataSource : dataSources) {
      Path path = toPath(dataSource);
      if (path != null) {
        normalized.add(path);
      }
    }

    return normalized;
  }
}
