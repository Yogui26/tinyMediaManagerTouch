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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;

/**
 * the class {@link AudioChannelsIcon} is used to draw the aspect ratio icon
 * 
 * @author Manuel Laggner
 */
public class AudioChannelsIcon extends MediaInfoIcon {
  private final String text;

  private String       svgContent;

  public AudioChannelsIcon(String text) throws Exception {
    super("audio/audio_channels.svg");
    this.text = text;
    setPreferredHeight(32);

  }

  @Override
  protected synchronized SVGDocument loadSVG() {
    if (StringUtils.isBlank(text)) {
      return null;
    }

    try {
      if (StringUtils.isBlank(svgContent)) {
        svgContent = IOUtils.toString(getUrl(), StandardCharsets.UTF_8);
      }

      String modifiedSvgContent = svgContent.replace("{{text}}", text);

      return SVG_LOADER.load(new ByteArrayInputStream(modifiedSvgContent.getBytes(StandardCharsets.UTF_8)), null, LoaderContext.createDefault());
    }
    catch (Exception e) {
      return null;
    }
  }
}
