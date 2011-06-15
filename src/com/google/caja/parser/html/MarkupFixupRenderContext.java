// Copyright (C) 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.caja.parser.html;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.w3c.dom.Element;

/**
 * An interface that can be implemented by a
 * {@link com.google.caja.reporting.RenderContext render context} when the
 * rendered text contains DOM nodes whose content cannot be easily rendered in
 * a particular render mode to coerce it to semantically equivalent and
 * renderable content.
 * <p>
 * For example, the XML {@code <script>var x = 1&lt;/script&gt;/;</script>}
 * cannot be rendered as HTML without modifications because HTML CDATA cannot
 * contain entities like {@code &lt;}.
 * </p>
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
@ParametersAreNonnullByDefault
public interface MarkupFixupRenderContext {

  /**
   * May be overridden to fix invalid escaping text spans or an embedded
   * end tag in a given element's content.
   *
   * @param el an element whose content contains invalid escaping text spans.
   * @param cdataContent the concatenation of el's text nodes' node values.
   * @param problemIndex the index of the problem in cdataContent
   * @return {@code null} to indicate cannot fix or
   *     {@code textNode.getNodeValue()} transformed to fix the issue.
   */
  @Nullable String fixUnclosableCdataElement(
      Element el, String cdataContent, int problemIndex);

}
