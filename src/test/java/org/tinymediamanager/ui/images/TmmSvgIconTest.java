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

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.kitfox.svg.SVGDiagram;

/**
 * The class {@link TmmSvgIconTest} tests the loading of SVG documents from URLs which contain characters that are illegal in a URI (e.g. square
 * brackets from a Windows installation path built by a launcher without percent-encoding). Such documents are loaded from the stream to work around
 * svgSalamanders broken reference resolution with those base URIs
 */
public class TmmSvgIconTest {
  @Rule
  public TemporaryFolder      tempFolder          = new TemporaryFolder();

  private static final String SVG_WITH_REFERENCES = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\">"
      + "<defs><linearGradient id=\"g\"><stop offset=\"0\" stop-color=\"#000000\"/><stop offset=\"1\" stop-color=\"#ffffff\"/></linearGradient></defs>"
      + "<rect width=\"24\" height=\"24\" fill=\"url(#g)\"/></svg>";

  @Test
  public void loadsSimpleSvgFromPlainPath() throws Exception {
    Path svgFile = Files.createDirectories(tempFolder.getRoot().toPath().resolve("plain")).resolve("t.svg");
    Files.write(svgFile, SVG_WITH_REFERENCES.getBytes(StandardCharsets.UTF_8));

    TmmSvgIcon icon = new TmmSvgIcon(svgFile.toUri().toURL(), "t.svg");

    assertRenders(icon);
  }

  @Test
  public void loadsSvgFromPathWithBrackets() throws Exception {
    // simulate a launcher created file: URL with literal [ ] and $ - and a proper percent-encoded one to compare with
    Path svgFile = Files.createDirectories(tempFolder.getRoot().toPath().resolve("[Portable] $Multimedia")).resolve("t.svg");
    Files.write(svgFile, SVG_WITH_REFERENCES.getBytes(StandardCharsets.UTF_8));

    TmmSvgIcon icon = new TmmSvgIcon(new URL("file:" + svgFile.toAbsolutePath()), "t.svg");

    assertRenders(icon);
  }

  @Test
  public void rendersIdenticalToWellKnownPath() throws Exception {
    // the reference resolution must not be degraded when the document had to be loaded from the stream
    String svg = readBundledIcon("icn_movies.svg"); // contains url(#linearGradient..) references

    Path plain = Files.createDirectories(tempFolder.getRoot().toPath().resolve("plain")).resolve("icn_movies.svg");
    Files.write(plain, svg.getBytes(StandardCharsets.UTF_8));

    Path bracketed = Files.createDirectories(tempFolder.getRoot().toPath().resolve("[Portable] $Multimedia")).resolve("icn_movies.svg");
    Files.write(bracketed, svg.getBytes(StandardCharsets.UTF_8));

    TmmSvgIcon reference = new TmmSvgIcon(plain.toUri().toURL(), "icn_movies.svg");
    TmmSvgIcon fromBrackets = new TmmSvgIcon(new URL("file:" + bracketed.toAbsolutePath()), "icn_movies.svg");

    assertThat(rasterize(fromBrackets)).isEqualTo(rasterize(reference));
  }

  @Test
  public void loadsSvgFromJarInPathWithBrackets() throws Exception {
    Path jarFile = Files.createDirectories(tempFolder.getRoot().toPath().resolve("[Portable] $Multimedia")).resolve("icons.jar");
    try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
      jos.putNextEntry(new JarEntry("svg/t.svg"));
      jos.write(SVG_WITH_REFERENCES.getBytes(StandardCharsets.UTF_8));
      jos.closeEntry();
    }

    TmmSvgIcon icon = new TmmSvgIcon(new URL("jar:file:" + jarFile.toAbsolutePath() + "!/svg/t.svg"), "t.svg");

    assertRenders(icon);
  }

  @Test
  public void reloadDiagramReloadsStreamLoadedDocument() throws Exception {
    // the MediaInfoIcon.setText pattern: clear the universe and get a fresh copy of the document to modify it
    Path svgFile = Files.createDirectories(tempFolder.getRoot().toPath().resolve("[x] $y")).resolve("t.svg");
    Files.write(svgFile, SVG_WITH_REFERENCES.getBytes(StandardCharsets.UTF_8));

    TmmSvgIcon icon = new TmmSvgIcon(new URL("file:" + svgFile.toAbsolutePath()), "t.svg");

    SVGDiagram reloaded = icon.reloadDiagram();
    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getElement("g")).isNotNull();

    // and the URI based loading reloads via the universe as before
    TmmSvgIcon fromPlain = new TmmSvgIcon(svgFile.toUri().toURL(), "t.svg");
    assertThat(fromPlain.reloadDiagram()).isNotNull();
  }

  private String readBundledIcon(String name) throws Exception {
    try (InputStream is = TmmSvgIconTest.class.getResourceAsStream("/org/tinymediamanager/ui/images/svg/" + name)) {
      assertThat(is).as("bundled icon " + name).isNotNull();
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private void assertRenders(TmmSvgIcon icon) {
    assertThat(icon.getIconWidth()).isGreaterThan(0);
    assertThat(icon.getIconHeight()).isGreaterThan(0);
    assertThat(rasterize(icon)).as("painted pixels").isNotZero();
  }

  private int rasterize(TmmSvgIcon icon) {
    icon.setPreferredSize(new Dimension(24, 24));
    icon.setAutosize(com.kitfox.svg.app.beans.SVGIcon.AUTOSIZE_STRETCH);

    BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
    icon.paintIcon(null, img.createGraphics(), 0, 0);

    int painted = 0;
    for (int y = 0; y < 24; y++) {
      for (int x = 0; x < 24; x++) {
        if ((img.getRGB(x, y) >>> 24) != 0) {
          painted++;
        }
      }
    }
    return painted;
  }
}
