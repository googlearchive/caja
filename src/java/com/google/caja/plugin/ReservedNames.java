// Copyright (C) 2007 Google Inc.
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

import com.google.caja.lexer.Keyword;

/**
 * Caja reserved names.
 *
 * @author benl@google.com (Ben Laurie)
 */
public class ReservedNames {
  public static final String LOCAL_THIS = "t___";
  public static final String THIS = Keyword.THIS.toString();
  public static final String SUPER = "Super";
  static final String TEMP = "x___";
  public static final String ARGUMENTS = "arguments";
  public static final String LOCAL_ARGUMENTS = "a___";
  /** The current node for an event handler function. */
  public static final String THIS_NODE = "thisNode___";
  /**
   * The name of a plugin's global object in the scope in the outermost scope
   * in which that plugin's code is defined.
   */
  public static final String OUTERS = "___OUTERS___";
  /** Trademarks a string as HTML PCDATA. */
  public static final String BLESS_HTML = "blessHtml___";
  /** Trademarks a string as CSS properties. */
  public static final String BLESS_CSS = "blessCss___";
  /** Escapes a string to an HTML attribute. */
  public static final String HTML_ATTR = "htmlAttr___";
  /** Escapes a string to a HTML PCDATA. */
  public static final String HTML = "html___";
  /**
   * A property on OUTERS that resolves to an instance of of HtmlEmitter as
   * defined in html-emitter.js.
   */
  public static final String HTML_EMITTER = "htmlEmitter___";
  /**
   * Applies {@link com.google.caja.opensocial.UriCallback#rewrite} at runtime.
   */
  public static final String REWRITE_URI = "rewriteUri___";
  /** Applies the id and nmtoken policy at runtime. */
  public static final String PREFIX = "prefix___";
  /** Coerces the argument to a CSS number. */
  public static final String CSS_NUMBER = "cssNumber___";
  /** Coerces the argument to a CSS color. */
  public static final String CSS_COLOR = "cssColor___";
  /** Coerces the argument to a CSS uri by applying the URICallback policy. */
  public static final String CSS_URI = "cssUri___";
  /** An output buffer for a compiled template. */
  public static final String OUTPUT_BUFFER = "out___";

  private ReservedNames() {
  }
}
