// Copyright 2009 Google Inc. All Rights Reserved.
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

package com.google.caja.service;

/**
 * The set of arguments accepted by the Caja front end.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public enum CajaArguments {

  /**
   * The expected MIME type of an input document that Caja is being asked to
   * process. Caja may return an error if the actual MIME type of the document
   * is different from what is expected. This parameter is provided as a sanity
   * check for clients of Caja.
   */
  INPUT_MIME_TYPE("input-mime-type"),

  /**
   * Boolean indicating whether or not the cajoler should produce HTML output
   * as part of the JavaScript.
   */
  EMIT_HTML_IN_JS("emit-html-in-js"),

  /**
   * Name of the JSONP callback function desired in the output. See the
   * <a href="http://code.google.com/apis/gdata/docs/2.0/reference.html">GData
   * API</a>
   * and the guide to
   * <a href="http://code.google.com/apis/gdata/docs/json.html">using JSON</a>.
   */
  CALLBACK("callback"),

  /**
   * Alternative representation type of the desired result.
   * See the
   * <a href="http://code.google.com/apis/gdata/docs/2.0/reference.html">GData
   * API</a>.
   *
   * <p>Valid values are:
   * <ul>
   *   <li>{@code alt=json} the default.</li>
   *   <li>{@code alt=json-in-script} as described in the GData reference.</li>
   * </ul>
   */
  ALT("alt"),

  /**
   * The Caja transform that is being requested. Valid values are:
   * <ul>
   *   <li>{@code transform=cajole} the Caja language.</li>
   *   <li>{@code transform=innocent} the innocent code transform.</li>
   * </ul>
   *
   * <p>All these transforms are applicable to {@code text/javascript} input.
   * For {@code text/html} input, this argument is ignored and the
   * {@code cajole} transform is always applied.
   */
  TRANSFORM("transform"),

  /**
   * Directives which affect the chosen Caja transformation
   */
  DIRECTIVE("directive"),
  
  /**
   * The URL of the input document that Caja is being asked to process.
   */
  URL("url"),

  /**
   * Renderer for the code.
   *
   * <p>Valid values are:
   * <ul>
   *   <li>{@code renderer=minimal} the default, saves space.</li>
   *   <li>{@code renderer=pretty} pretty print for debugging.</li>
   * </ul>
   */
  RENDERER("renderer");

  private final String argKeyword;

  private CajaArguments(String argKeyword) {
    this.argKeyword = argKeyword;
  }

  /**
   * @return the keyword for this argument, as it would appear in (say) a
   * command line.
   */
  public String getArgKeyword() { return argKeyword; }

  /**
   * @see com.google.caja.service.ContentHandlerArgs#get(String)
   */
  public String get(ContentHandlerArgs args) {
    return args.get(this.getArgKeyword());
  }

  /**
   * @see com.google.caja.service.ContentHandlerArgs#get(String, boolean)
   */
  public String get(ContentHandlerArgs args, boolean required)
      throws InvalidArgumentsException {
    return args.get(this.getArgKeyword(), required);
  }
}
