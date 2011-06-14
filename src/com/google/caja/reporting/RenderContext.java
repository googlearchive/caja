// Copyright (C) 2005 Google Inc.
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

package com.google.caja.reporting;

import com.google.caja.lexer.TokenConsumer;

/**
 * @see Renderable
 * @author mikesamuel@gmail.com
 */
public class RenderContext {

  /** Produce output that can be safely embedded. */
  private final boolean embeddable;
  /** Produce output that only contains lower 7-bit characters. */
  private final boolean asciiOnly;
  /** Should javascript output be rendered using JSON conventions. */
  private final boolean json;
  /** True iff DOM tree nodes should be rendered as XML. */
  private final MarkupRenderMode markupMode;
  /**
   * True iff object ctor keys that are JS identifiers can be rendered without
   * quotes.
   */
  private final boolean rawObjKeys;
  private final TokenConsumer out;

  public RenderContext(TokenConsumer out) {
    this(true, false, false, MarkupRenderMode.HTML, false, out);
  }

  protected RenderContext(
      boolean asciiOnly, boolean embeddable, boolean json,
      MarkupRenderMode markupMode, boolean rawObjKeys, TokenConsumer out) {
    if (null == out) { throw new NullPointerException(); }
    this.embeddable = embeddable;
    this.asciiOnly = asciiOnly;
    this.json = json;
    this.markupMode = markupMode;
    this.rawObjKeys = rawObjKeys;
    this.out = out;
  }

  /**
   * True if the renderer produces output that can be embedded inside a CDATA
   * section, or {@code script} element without further escaping?
   */
  public final boolean isEmbeddable() { return embeddable; }
  /**
   * True if the renderer produces output that only contains characters in
   * {@code [\1-\x7f]}.
   */
  public final boolean isAsciiOnly() { return asciiOnly; }
  public final boolean asJson() { return json; }
  /** True iff DOM tree nodes should be rendered as XML. */
  public final boolean asXml() { return markupMode == MarkupRenderMode.XML; }
  public final MarkupRenderMode markupRenderMode() { return markupMode; }
  public final boolean rawObjKeys() { return rawObjKeys; }
  public final TokenConsumer getOut() { return out; }

  /** Must be overridden by subclasses to return an instance of the subclass. */
  protected RenderContext derive(
      boolean asciiOnly, boolean embeddable, boolean json,
      MarkupRenderMode markupMode, boolean rawObjKeys) {
    return new RenderContext(
        asciiOnly, embeddable, json, markupMode, rawObjKeys, out);
  }

  private RenderContext deriveChecked(
      boolean asciiOnly, boolean embeddable, boolean json,
      MarkupRenderMode markupMode, boolean rawObjKeys) {
    RenderContext derived = derive(
        asciiOnly, embeddable, json, markupMode, rawObjKeys);
    // Enforce that derive has been overridden.
    assert derived.getClass() == getClass();
    return derived;
  }

  public RenderContext withAsciiOnly(boolean b) {
    return b != asciiOnly
        ? deriveChecked(b, embeddable, json, markupMode, rawObjKeys)
        : this;
  }
  public RenderContext withEmbeddable(boolean b) {
    return b != embeddable
        ? deriveChecked(asciiOnly, b, json, markupMode, rawObjKeys)
        : this;
  }
  public RenderContext withJson(boolean b) {
    return b != json
        ? deriveChecked(asciiOnly, embeddable, b, markupMode, rawObjKeys)
        : this;
  }
  public RenderContext withMarkupRenderMode(MarkupRenderMode markupMode) {
    return markupMode != this.markupMode
        ? deriveChecked(asciiOnly, embeddable, json, markupMode, rawObjKeys)
        : this;
  }
  @Deprecated
  public RenderContext withAsXml(boolean b) {
    return withMarkupRenderMode(
        b ? MarkupRenderMode.XML : MarkupRenderMode.HTML);
  }
  public RenderContext withRawObjKeys(boolean b) {
    return b != this.rawObjKeys
        ? deriveChecked(asciiOnly, embeddable, json, markupMode, b)
        : this;
  }
}
