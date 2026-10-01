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
package org.tinymediamanager.ui.images;

import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

public class TmmAwesomeIcon extends TmmSvgIcon {

  public TmmAwesomeIcon(URI uri, Color color) {
    super(uri);
    setColor(color);
    setAutosize(AUTOSIZE_BESTFIT);
  }

  /**
   * Loads the SVG document from the given {@link URL}, see {@link TmmSvgIcon#TmmSvgIcon(URL, String)}
   * 
   * @param url
   *          the {@link URL} pointing to the SVG document
   * @param name
   *          the name used to reference the document if it has to be loaded from the stream
   * @param color
   *          the {@link Color} to set
   * @throws IOException
   *           if the document could not be loaded
   */
  public TmmAwesomeIcon(URL url, String name, Color color) throws IOException {
    super(url, name);
    setColor(color);
    setAutosize(AUTOSIZE_BESTFIT);
  }
}
