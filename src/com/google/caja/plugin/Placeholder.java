// Copyright (C) 2010 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Namespaces;

/**
 * Utilities for linking scripts extracted from HTML with the points in the
 * HTML from which they were extracted.
 *
 * <p>
 * The {@link com.google.caja.plugin.stages.RewriteHtmlStage} pulls scripts and
 * styles out of HTML.
 * The processed results of some parts might be pulled from the cache, while
 * cache misses will go through a sanitizer.
 * The {@link com.google.caja.plugin.stages.CompileHtmlStage} recombines the
 * various parts, using placeholders, and does some normalization
 * (converting sanitized CSS to HTML, and possibly HTML to unsanitary JS).
 * Finally, all the unsanitary JS goes through a rewriter.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public final class Placeholder {

  /**
   * An ID used to indicate that an element is just a placeholder for extracted
   * content.
   */
  public static final AttribKey ID_ATTR = AttribKey.forAttribute(
      Namespaces.HTML_DEFAULT, ElKey.HTML_WILDCARD, "__phid__");

  private Placeholder() {
    // Not instantiable.
  }
}
