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
package org.tinymediamanager.ui.components.table;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.tinymediamanager.core.CustomColumn;
import org.tinymediamanager.core.TmmResourceBundle;

/**
 * The class {@link CustomColumnLevel} describes one hierarchy level a freely configurable column can carry a JMTE pattern for, along with the
 * accessors to read/write that very pattern on the concrete column bean.
 *
 * @param <C>
 *          the concrete type of the custom column bean
 * @author Manuel Laggner
 */
public class CustomColumnLevel<C extends CustomColumn> {
  private final String                labelKey;
  private final String                hintKey;
  private final Function<C, String>   getter;
  private final BiConsumer<C, String> setter;

  /**
   * @param labelKey
   *          the resource bundle key of the label shown for this level
   * @param hintKey
   *          the resource bundle key of the tooltip shown for this level
   * @param getter
   *          reads the JMTE pattern of this level
   * @param setter
   *          writes the JMTE pattern of this level
   */
  public CustomColumnLevel(String labelKey, String hintKey, Function<C, String> getter, BiConsumer<C, String> setter) {
    this.labelKey = labelKey;
    this.hintKey = hintKey;
    this.getter = getter;
    this.setter = setter;
  }

  /**
   * @return the label to show for this level
   */
  public String getLabel() {
    return TmmResourceBundle.getString(labelKey);
  }

  /**
   * @return the tooltip to show for this level
   */
  public String getHint() {
    return TmmResourceBundle.getString(hintKey);
  }

  /**
   * @return the JMTE pattern of this level of the given column, may be null
   */
  public String getPattern(C customColumn) {
    return getter.apply(customColumn);
  }

  /**
   * @param customColumn
   *          the column to set the pattern for
   * @param pattern
   *          the JMTE pattern of this level
   */
  public void setPattern(C customColumn, String pattern) {
    setter.accept(customColumn, pattern);
  }
}
