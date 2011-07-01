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
  /** True iff DOM tree nodes should be rendered as XML. */
  private final MarkupRenderMode markupMode;
  /**
   * Specify how property names are quoted.
   */
  private final PropertyNameQuotingMode propertyNameQuotingMode;
  private final TokenConsumer out;

  public RenderContext(TokenConsumer out) {
    this(true, false, MarkupRenderMode.HTML, PropertyNameQuotingMode.DEFAULT,
         out);
  }

  protected RenderContext(
      boolean asciiOnly, boolean embeddable, MarkupRenderMode markupMode,
      PropertyNameQuotingMode propertyNameQuotingMode, TokenConsumer out) {
    if (null == out) { throw new NullPointerException(); }
    this.embeddable = embeddable;
    this.asciiOnly = asciiOnly;
    this.markupMode = markupMode;
    this.propertyNameQuotingMode = propertyNameQuotingMode;
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
  public final boolean asJson() {
    return propertyNameQuotingMode == PropertyNameQuotingMode.DOUBLE_QUOTES;
  }
  /** True iff DOM tree nodes should be rendered as XML. */
  public final boolean asXml() { return markupMode == MarkupRenderMode.XML; }
  public final MarkupRenderMode markupRenderMode() { return markupMode; }
  public final PropertyNameQuotingMode propertyNameQuotingMode() {
    return propertyNameQuotingMode;
  }
  /** Use {@link #propertyNameQuotingMode} instead. */
  @Deprecated
  public final boolean rawObjKeys() {
    return propertyNameQuotingMode == PropertyNameQuotingMode.NO_QUOTES;
  }
  public final TokenConsumer getOut() { return out; }

  /** Must be overridden by subclasses to return an instance of the subclass. */
  protected RenderContext derive(
      boolean asciiOnly, boolean embeddable, MarkupRenderMode markupMode,
      PropertyNameQuotingMode propertyNameQuotingMode) {
    return new RenderContext(
        asciiOnly, embeddable, markupMode, propertyNameQuotingMode, out);
  }

  private RenderContext deriveChecked(
      boolean asciiOnly, boolean embeddable, MarkupRenderMode markupMode,
      PropertyNameQuotingMode propertyNameQuotingMode) {
    RenderContext derived = derive(
        asciiOnly, embeddable, markupMode, propertyNameQuotingMode);
    // Enforce that derive has been overridden.
    assert derived.getClass() == getClass();
    return derived;
  }

  public RenderContext withAsciiOnly(boolean b) {
    return b != asciiOnly
        ? deriveChecked(b, embeddable, markupMode, propertyNameQuotingMode)
        : this;
  }
  public RenderContext withEmbeddable(boolean b) {
    return b != embeddable
        ? deriveChecked(asciiOnly, b, markupMode, propertyNameQuotingMode)
        : this;
  }
  public RenderContext withJson(boolean b) {
    return withPropertyNameQuotingMode(
        b ? PropertyNameQuotingMode.DOUBLE_QUOTES
        : PropertyNameQuotingMode.DEFAULT);
  }
  public RenderContext withMarkupRenderMode(MarkupRenderMode markupMode) {
    return markupMode != this.markupMode
        ? deriveChecked(asciiOnly, embeddable, markupMode,
                        propertyNameQuotingMode)
        : this;
  }
  /** Use {@link #withMarkupRenderMode} instead. */
  @Deprecated
  public RenderContext withAsXml(boolean b) {
    return withMarkupRenderMode(
        b ? MarkupRenderMode.XML : MarkupRenderMode.HTML);
  }
  /** Use {@link #withPropertyNameQuotingMode} instead. */
  @Deprecated
  public RenderContext withRawObjKeys(boolean b) {
    return b ? withPropertyNameQuotingMode(PropertyNameQuotingMode.NO_QUOTES)
        : withPropertyNameQuotingMode(PropertyNameQuotingMode.DEFAULT);
  }
  public RenderContext withPropertyNameQuotingMode(PropertyNameQuotingMode m) {
    return this.propertyNameQuotingMode != m
        ? deriveChecked(asciiOnly, embeddable, markupMode, m)
        : this;
  }
}
