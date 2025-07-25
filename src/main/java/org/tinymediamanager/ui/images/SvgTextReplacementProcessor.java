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

import org.jetbrains.annotations.NotNull;

import com.github.weisj.jsvg.parser.DomElement;
import com.github.weisj.jsvg.parser.DomProcessor;

public class SvgTextReplacementProcessor implements DomProcessor {

  private final String replacementText;
  private final int    offsetX;

  public SvgTextReplacementProcessor(String replacementText, int offsetX) {
    this.replacementText = replacementText;
    this.offsetX = offsetX;
  }

  @Override
  public void process(@NotNull DomElement root) {
    processImpl(root);
    root.children().forEach(this::process);
  }

  private void processImpl(DomElement element) {
    // Obtain the id of the element
    // Note: There that Element also has a node() method to obtain the SVGNode. However during the pre-processing
    // phase the SVGNode is not yet fully parsed and doesn't contain any non-defaulted information.
    String nodeId = element.id();

    if (!"text".equals(nodeId)) {
      return; // we only want to process text nodes
    }

    element.setAttribute("x", String.valueOf(offsetX));
  }
}
