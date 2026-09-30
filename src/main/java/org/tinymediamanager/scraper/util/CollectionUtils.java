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
package org.tinymediamanager.scraper.util;

import java.util.Collection;
import java.util.Collections;

public class CollectionUtils {

  private CollectionUtils() {
    throw new IllegalAccessError();
  }

  /**
   * Get a nullsafe Iterable. You can pass here any iterable collection and get a iterable collection back. Also works with null
   *
   * @param it
   *          the iterable collection or null
   * @return the same iterable collection (if <i>it</i> was not null) or an empty iterable collection of the same type
   */
  public static <T> Iterable<T> nullSafe(Iterable<T> it) {
    return it != null ? it : Collections.emptySet();
  }

  /**
   * checks whether a collection is null or empty
   *
   * @param collection
   *          the collection to check
   * @return true if the collection is null or empty
   */
  public static <T> boolean isEmpty(Collection<T> collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * checks whether a collection contains at least one entry
   *
   * @param collection
   *          the collection to check
   * @return true if the collection is not null and not empty
   */
  public static <T> boolean isNotEmpty(Collection<T> collection) {
    return collection != null && !collection.isEmpty();
  }

  /**
   * merges the entries from newItems into the baseCollection<br />
   * this implementation does adopt items from the baseCollection to the newItems in the same order<br />
   * - without creating a new list<br />
   * - without touching existing entries (which should not be removed)<br />
   * <br />
   * This implementation only works if .equals() of the type T is implemented in a proper way
   * 
   * @param baseCollection
   *          the base list which will be altered
   * @param newItems
   *          the new entries which should be merged into the existing list
   */
  public static <T> void merge(Collection<T> baseCollection, Collection<T> newItems) {
    // remove all items which are not in the new list
    baseCollection.removeIf(item -> !newItems.contains(item));

    // add all items which are not in the base list
    for (T item : newItems) {
      if (!baseCollection.contains(item)) {
        baseCollection.add(item);
      }
    }
  }
}
