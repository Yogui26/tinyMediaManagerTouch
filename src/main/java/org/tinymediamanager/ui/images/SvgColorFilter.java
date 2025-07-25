/*
 * Copyright 2012 - 2025 Manuel Laggner
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

package org.tinymediamanager.ui.images;

import java.awt.Color;
import java.awt.Component;
import java.awt.image.RGBImageFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatIconColors;

public class SvgColorFilter {
  private static SvgColorFilter               instance;
  private Map<Integer, String>                rgb2keyMap;
  private Map<Color, Color>                   colorMap;
  private Map<Color, Color>                   darkColorMap;
  private Function<Color, Color>              mapper;
  private BiFunction<Component, Color, Color> mapperEx;

  public Component                            c;

  public static SvgColorFilter getInstance() {
    if (instance == null) {
      instance = new SvgColorFilter();
      instance.rgb2keyMap = new HashMap<>();

      for (FlatIconColors c : FlatIconColors.values()) {
        instance.rgb2keyMap.put(c.rgb, c.key);
      }
    }

    return instance;
  }

  public SvgColorFilter() {
  }

  public SvgColorFilter(Function<Color, Color> mapper) {
    this.setMapper(mapper);
  }

  public SvgColorFilter(BiFunction<Component, Color, Color> mapperEx) {
    this.setMapperEx(mapperEx);
  }

  public Function<Color, Color> getMapper() {
    return this.mapper;
  }

  public void setMapper(Function<Color, Color> mapper) {
    this.mapper = mapper;
  }

  public BiFunction<Component, Color, Color> getMapperEx() {
    return this.mapperEx;
  }

  public void setMapperEx(BiFunction<Component, Color, Color> mapperEx) {
    this.mapperEx = mapperEx;
  }

  public Map<Color, Color> getLightColorMap() {
    return this.colorMap != null ? Collections.unmodifiableMap(this.colorMap) : Collections.emptyMap();
  }

  public Map<Color, Color> getDarkColorMap() {
    return this.darkColorMap != null ? Collections.unmodifiableMap(this.darkColorMap) : this.getLightColorMap();
  }

  public SvgColorFilter addAll(Map<Color, Color> from2toMap) {
    this.ensureColorMap();
    this.colorMap.putAll(from2toMap);
    if (this.darkColorMap != null) {
      this.darkColorMap.putAll(from2toMap);
    }

    return this;
  }

  public SvgColorFilter addAll(Map<Color, Color> from2toLightMap, Map<Color, Color> from2toDarkMap) {
    this.ensureColorMap();
    this.ensureDarkColorMap();
    this.colorMap.putAll(from2toLightMap);
    this.darkColorMap.putAll(from2toDarkMap);
    return this;
  }

  public SvgColorFilter add(Color from, Color to) {
    this.ensureColorMap();
    this.colorMap.put(from, to);
    if (this.darkColorMap != null) {
      this.darkColorMap.put(from, to);
    }

    return this;
  }

  public SvgColorFilter add(Color from, Color toLight, Color toDark) {
    this.ensureColorMap();
    this.ensureDarkColorMap();
    if (toLight != null) {
      this.colorMap.put(from, toLight);
    }

    if (toDark != null) {
      this.darkColorMap.put(from, toDark);
    }

    return this;
  }

  public SvgColorFilter remove(Color from) {
    if (this.colorMap != null) {
      this.colorMap.remove(from);
    }

    if (this.darkColorMap != null) {
      this.darkColorMap.remove(from);
    }

    return this;
  }

  public SvgColorFilter removeAll() {
    this.colorMap = null;
    this.darkColorMap = null;
    return this;
  }

  private void ensureColorMap() {
    if (this.colorMap == null) {
      this.colorMap = new HashMap<>();
    }

  }

  private void ensureDarkColorMap() {
    if (this.darkColorMap == null) {
      this.darkColorMap = new HashMap<>(this.colorMap);
    }

  }

  public Color filter(Color color) {
    return this.filter(this.c, color);
  }

  public Color filter(Component c, Color color) {
    color = this.applyMappings(color);
    if (this.mapper != null) {
      color = this.mapper.apply(color);
    }

    if (this.mapperEx != null) {
      color = this.mapperEx.apply(c, color);
    }

    return color;
  }

  private Color applyMappings(Color color) {
    if (this.colorMap != null) {
      Map<Color, Color> map = this.colorMap;
      Color newColor = map.get(color);
      if (newColor != null) {
        return newColor;
      }
    }

    if (this.rgb2keyMap != null) {
      String colorKey = this.rgb2keyMap.get(color.getRGB() & 16777215);
      if (colorKey == null) {
        return color;
      }
      else {
        Color newColor = UIManager.getColor(colorKey);
        if (newColor == null) {
          return color;
        }
        else {
          return newColor.getAlpha() != color.getAlpha() ? new Color(newColor.getRGB() & 16777215 | color.getRGB() & -16777216) : newColor;
        }
      }
    }
    else {
      return color;
    }
  }

  public Component getPaintingComponent() {
    return this.c;
  }

  public static Function<Color, Color> createRGBImageFilterFunction(RGBImageFilter rgbImageFilter) {
    return (color) -> {
      int oldRGB = color.getRGB();
      int newRGB = rgbImageFilter.filterRGB(0, 0, oldRGB);
      return newRGB != oldRGB ? new Color(newRGB, true) : color;
    };
  }
}
