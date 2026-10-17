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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.tinymediamanager.core.movie.BasicMovieTest;

/**
 * Verifies the lazy per-entity value cache of the table format: values are only computed when they are asked for, kept until the entity is
 * invalidated and can be dropped completely.
 */
public class TmmTableFormatValueCacheTest extends BasicMovieTest {

  private static final class Element {
    private final String key;

    private Element(String key) {
      this.key = key;
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Element other && key.equals(other.key);
    }
  }

  private static class CountingFormat extends TmmTableFormat<Element> {
    private final AtomicInteger valueCalls   = new AtomicInteger();
    private final AtomicInteger nullCalls    = new AtomicInteger();
    private final AtomicInteger tooltipCalls = new AtomicInteger();

    private CountingFormat() {
      this(true);
    }

    private CountingFormat(boolean cache) {
      if (cache) {
        enableValueCaching();
      }

      Column col = new Column("value", "value", element -> {
        valueCalls.incrementAndGet();
        return element.key;
      }, String.class);
      col.setCellTooltip(element -> {
        tooltipCalls.incrementAndGet();
        return "tooltip " + element.key;
      });
      addColumn(col);

      addColumn(new Column("null", "null", element -> {
        nullCalls.incrementAndGet();
        return null;
      }, String.class));
    }
  }

  @Test
  public void valuesAreLoadedLazilyAndCached() {
    CountingFormat format = new CountingFormat();
    Element element = new Element("a");

    // nothing is computed before the very first access
    assertThat(format.valueCalls.get()).isZero();

    assertThat(format.getColumnValue(element, 0)).isEqualTo("a");
    assertThat(format.getColumnValue(element, 1)).isNull();

    // both values are remembered, the second access does not compute them again
    assertThat(format.getColumnValue(element, 0)).isEqualTo("a");
    assertThat(format.getColumnValue(element, 1)).isNull();

    assertThat(format.valueCalls.get()).isEqualTo(1);
    assertThat(format.nullCalls.get()).isEqualTo(1);
  }

  @Test
  public void valuesAreCachedPerEntity() {
    CountingFormat format = new CountingFormat();

    assertThat(format.getColumnValue(new Element("a"), 0)).isEqualTo("a");
    assertThat(format.getColumnValue(new Element("b"), 0)).isEqualTo("b");
    assertThat(format.getColumnValue(new Element("a"), 0)).isEqualTo("a");

    assertThat(format.valueCalls.get()).isEqualTo(2);
  }

  @Test
  public void invalidateDropsOnlyTheGivenEntity() {
    CountingFormat format = new CountingFormat();
    Element first = new Element("a");
    Element second = new Element("b");

    format.getColumnValue(first, 0);
    format.getColumnValue(second, 0);
    assertThat(format.valueCalls.get()).isEqualTo(2);

    format.invalidateCache(first);

    assertThat(format.getColumnValue(second, 0)).isEqualTo("b");
    assertThat(format.valueCalls.get()).isEqualTo(2);

    assertThat(format.getColumnValue(first, 0)).isEqualTo("a");
    assertThat(format.valueCalls.get()).isEqualTo(3);
  }

  @Test
  public void clearDropsAllEntities() {
    CountingFormat format = new CountingFormat();

    format.getColumnValue(new Element("a"), 0);
    format.getColumnValue(new Element("b"), 0);
    assertThat(format.valueCalls.get()).isEqualTo(2);

    format.clearCache();

    format.getColumnValue(new Element("a"), 0);
    assertThat(format.valueCalls.get()).isEqualTo(3);
  }

  @Test
  public void tooltipsAreCachedToo() {
    CountingFormat format = new CountingFormat();
    Element element = new Element("a");

    assertThat(format.getColumnTooltip(element, 0)).isEqualTo("tooltip a");
    assertThat(format.getColumnTooltip(element, 0)).isEqualTo("tooltip a");
    assertThat(format.tooltipCalls.get()).isEqualTo(1);

    format.invalidateCache(element);
    assertThat(format.getColumnTooltip(element, 0)).isEqualTo("tooltip a");
    assertThat(format.tooltipCalls.get()).isEqualTo(2);
  }

  @Test
  public void cachingIsOffByDefault() {
    CountingFormat format = new CountingFormat(false);
    Element element = new Element("a");

    format.getColumnValue(element, 0);
    format.getColumnValue(element, 0);

    assertThat(format.valueCalls.get()).isEqualTo(2);
  }

  @Test
  public void aNullCacheKeyBypassesTheCache() {
    CountingFormat format = new CountingFormat() {
      @Override
      protected Object getCacheKey(Element element) {
        return null;
      }
    };
    Element element = new Element("a");

    format.getColumnValue(element, 0);
    format.getColumnValue(element, 0);

    assertThat(format.valueCalls.get()).isEqualTo(2);
  }
}
