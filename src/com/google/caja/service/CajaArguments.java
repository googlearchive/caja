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
   * In case the output is, or contains, a Caja module, the name of a
   * JSONP-style callback function to use for loading the module. If this
   * argument is not specified, the output looks like:
   *
   * <p>
   * <code>___.loadModule({</code><em>cajaModule</em><code>})</code>
   *
   * <p>whereas, if this argument is specified, the output looks like:
   *
   * <p>
   * <em>module-callback</em><code>({</code><em>cajaModule</em><code>})</code>
   */
  MODULE_CALLBACK("module-callback"),

  /**
   * The expected MIME type of an input document that Caja is being asked to
   * process. Caja may return an error if the actual MIME type of the document
   * is different from what is expected. This parameter is provided as a sanity
   * check for clients of Caja.
   */
  INPUT_MIME_TYPE("input-mime-type"),

  /**
   * @see #INPUT_MIME_TYPE
   */
  @Deprecated OLD_INPUT_MIME_TYPE("mime-type"),

  /**
   * The requested MIME type of the output that Caja is being asked to produce.
   * Caja will return an error if it cannot process the input into output of
   * the requested type.
   */
  OUTPUT_MIME_TYPE("output-mime-type"),

  /**
   * The Caja language transform that is being requested. Valid values are:
   * <ul>
   *   <li>{@code cajita} the Cajita language.</li>
   *   <li>{@code valija} the Valija language.</li>
   *   <li>{@code innocent} the innocent code transform.</li>
   * </ul>
   *
   * <p>All these transforms are applicable to {@code text/javascript} input.
   * For {@code text/html} input, this argument is ignored and the
   * {@code valija} transform is always applied.
   */
  TRANSFORM("transform"),

  /**
   * The URL of the input document that Caja is being asked to process.
   */
  URL("url");

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
