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

/**
 * The interface {@link CustomColumn} is the common contract of the freely configurable table columns of the different tables. It only carries what
 * every configuration UI needs - the JMTE patterns themselves are accessed per level by the very implementation.
 *
 * @author Manuel Laggner
 */
public interface CustomColumn {

  /**
   * @return the stable id of this custom column, never null
   */
  String getId();

  /**
   * @return the header title of this custom column
   */
  String getTitle();

  /**
   * @param title
   *          the header title of this custom column
   */
  void setTitle(String title);

  /**
   * Returns the identifier of the table column belonging to this custom column.
   *
   * @return the table column identifier
   */
  String getColumnIdentifier();

  /**
   * Gets the header title of this custom column, falling back to a placeholder when the user did not set a title yet.
   *
   * @param position
   *          the 1-based position of this column within the configured custom columns
   * @return the title to show in the table header
   */
  String getTitleOrPlaceholder(int position);
}
