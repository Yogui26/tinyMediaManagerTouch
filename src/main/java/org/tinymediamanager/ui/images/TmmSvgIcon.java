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

import static org.tinymediamanager.ui.images.SvgIconHelper.getHexString;
import static org.tinymediamanager.ui.images.SvgIconHelper.setFill;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;

/**
 * basically a clone of {@link SVGIcon}, but with support of loading the same SVG several times with different settings (color, sizes, ...)
 * 
 * @author Manuel Laggner
 */
public class TmmSvgIcon extends SVGIcon {
  private static final Logger LOGGER = LoggerFactory.getLogger(TmmSvgIcon.class);

  // only set when the document was loaded from a stream because the URL could not be used as URI base
  private URL                 sourceUrl;
  private String              documentName;

  /**
   * Convenience-Constructor to set the {@link URI} afterward
   */
  protected TmmSvgIcon() {
    // create new SVG universe to suppress caching of loaded SVGs
    setSvgUniverse(new SVGUniverse());
    setAutosize(AUTOSIZE_BESTFIT);
    setAntiAlias(true);
    setInterpolation(INTERP_BICUBIC);
    setClipToViewbox(false);
  }

  public TmmSvgIcon(URI uri) {
    this();
    Objects.requireNonNull(uri, "uri must not be null");
    setSvgURI(uri);
  }

  /**
   * Loads the SVG document from the given {@link URL}.<br>
   * First a regular load via {@link URI} is tried, so relative references inside the document keep working. But svgSalamander percent-decodes the
   * base URI and reparses it strictly whenever it resolves a reference like {@code url(#id)} or {@code xlink:href="#id"} - which blows up for
   * characters that stay illegal in a URI even after encoding (e.g. square brackets from a Windows installation path built by a launcher without
   * percent-encoding).<br>
   * For those documents the SVG is loaded from the stream with a clean internal document name instead, which makes icon loading independent of the
   * encoding of the installation path.
   * 
   * @param url
   *          the {@link URL} pointing to the SVG document
   * @param name
   *          the name used to reference the document if it has to be loaded from the stream
   * @throws IOException
   *           if the document could not be loaded
   */
  public TmmSvgIcon(URL url, String name) throws IOException {
    this();
    Objects.requireNonNull(url, "url must not be null");
    sourceUrl = url;
    documentName = name;

    URI uri = null;
    try {
      uri = url.toURI();
    }
    catch (URISyntaxException e) {
      LOGGER.debug("Could not create a valid URI from '{}' - loading the SVG from stream", url);
    }

    if (uri != null && isSalamanderCompatibleUri(uri)) {
      // regular URI based loading
      sourceUrl = null;
      setSvgURI(uri);
      return;
    }

    loadFromStream();
  }

  /**
   * Checks if svgSalamander can resolve references within a document loaded from this URI. Mirrors what
   * {@code com.kitfox.svg.xml.StyleAttribute#getURIValue(URI)} does internally: it takes the (percent-decoded) scheme specific part and reparses it
   * as URI.
   */
  private static boolean isSalamanderCompatibleUri(URI uri) {
    try {
      new URI(null, uri.getSchemeSpecificPart(), null);
      return true;
    }
    catch (URISyntaxException e) {
      return false;
    }
  }

  /**
   * Loads the SVG document from the stream of the source URL and sets the URI to a clean internal name. Needed for documents that cannot be loaded
   * from a URI because of illegal characters in the path.
   */
  private void loadFromStream() throws IOException {
    try (InputStream is = sourceUrl.openStream()) {
      URI uri = getSvgUniverse().loadSVG(is, documentName);
      if (uri == null) {
        throw new IOException("Could not parse the SVG document '" + documentName + "'");
      }
      setSvgURI(uri);
    }
  }

  /**
   * Clears the {@link SVGUniverse} and loads the document again. Needed when you want to modify the parsed source of a document which is cached in
   * the universe.<br>
   * Documents loaded from a stream (see {@link #TmmSvgIcon(URL, String)}) are reloaded from the original {@link URL}, documents loaded via
   * {@link URI} are reloaded by the {@link SVGUniverse} itself.
   * 
   * @return the freshly loaded {@link SVGDiagram} or null if the document could not be reloaded
   */
  protected SVGDiagram reloadDiagram() {
    getSvgUniverse().clear();

    if (sourceUrl != null) {
      try {
        loadFromStream();
      }
      catch (IOException e) {
        LOGGER.warn("Could not reload the SVG document '{}'", documentName, e);
        return null;
      }
    }

    return getSvgUniverse().getDiagram(getSvgURI());
  }

  public void setPreferredHeight(int height) {
    SVGDiagram diagram = getSvgUniverse().getDiagram(getSvgURI());
    if (diagram != null) {
      setPreferredSize(new Dimension((int) (diagram.getWidth() * height / diagram.getHeight()), height));
    }
  }

  /**
   * Gets a SVGElement out of parsed XML structure<br>
   * Impl takes it out of the URI #fragment, which is the ID.
   * 
   * @param id
   *          the element ID to get
   * @return SVGElement or null
   */
  public SVGElement getElement(String id) {
    return getSvgUniverse().getElement(getSvgURI().resolve("#" + id));
  }

  /**
   * set the color of the whole icon (set the fill on all nodes).
   * 
   * @param color
   *          the {@link Color} to set
   */
  public void setColor(Color color) {
    if (color != null) {
      try {
        SVGDiagram diagram = getSvgUniverse().getDiagram(getSvgURI());
        setFill(getHexString(color), diagram.getRoot());
      }
      catch (Exception ex) {
        // ignored
      }
    }
  }

  /**
   * set the color of the whole icon (set the fill on all nodes, where the original color matches).
   * 
   * @param color
   *          the {@link Color} to set
   * @param originalToReplace
   *          the original color in the SVG to replace
   * 
   */
  public void setColor(Color color, String originalToReplace) {
    if (color != null) {
      try {
        SVGDiagram diagram = getSvgUniverse().getDiagram(getSvgURI());
        setFill(getHexString(color), originalToReplace, diagram.getRoot());
      }
      catch (Exception ex) {
        // ignored
      }
    }
  }
}
