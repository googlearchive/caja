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

  /** True iff DOM tree nodes should be rendered as XML. */
  private final MarkupRenderMode markupMode;
  /**
   * Specify how property names are quoted.
   */
  private final PropertyNameQuotingMode propertyNameQuotingMode;
  /**
   * Specify how JavaScript identifiers are handled.
   */
  private final JsIdentifierSyntax jsIdentifierSyntax;
  private final TokenConsumer out;

  public RenderContext(TokenConsumer out) {
    this(out, MarkupRenderMode.HTML, PropertyNameQuotingMode.DEFAULT,
         JsIdentifierSyntax.DEFAULT);
  }

  private RenderContext(
      TokenConsumer out, MarkupRenderMode markupMode,
      PropertyNameQuotingMode propertyNameQuotingMode,
      JsIdentifierSyntax jsIdentifierSyntax) {
    if (null == out) { throw new NullPointerException(); }
    this.markupMode = markupMode;
    this.propertyNameQuotingMode = propertyNameQuotingMode;
    this.jsIdentifierSyntax = jsIdentifierSyntax;
    this.out = out;
  }

  public final boolean asJson() {
    return propertyNameQuotingMode == PropertyNameQuotingMode.DOUBLE_QUOTES;
  }
  /** True iff DOM tree nodes should be rendered as XML. */
  public final boolean asXml() { return markupMode == MarkupRenderMode.XML; }
  public final MarkupRenderMode markupRenderMode() { return markupMode; }
  public final PropertyNameQuotingMode propertyNameQuotingMode() {
    return propertyNameQuotingMode;
  }
  public final JsIdentifierSyntax jsIdentifierSyntax() {
    return jsIdentifierSyntax;
  }

  public final TokenConsumer getOut() { return out; }

  private RenderContext derive(
      MarkupRenderMode markupMode,
      PropertyNameQuotingMode propertyNameQuotingMode,
      JsIdentifierSyntax jsIdentifierSyntax) {
    return new RenderContext(
        out, markupMode, propertyNameQuotingMode, jsIdentifierSyntax);
  }

  public RenderContext withJson(boolean b) {
    return withPropertyNameQuotingMode(
        b ? PropertyNameQuotingMode.DOUBLE_QUOTES
        : PropertyNameQuotingMode.DEFAULT);
  }
  public RenderContext withMarkupRenderMode(MarkupRenderMode markupMode) {
    return markupMode != this.markupMode
        ? derive(markupMode, propertyNameQuotingMode, jsIdentifierSyntax)
        : this;
  }
  public RenderContext withPropertyNameQuotingMode(PropertyNameQuotingMode m) {
    return this.propertyNameQuotingMode != m
        ? derive(markupMode, m, jsIdentifierSyntax)
        : this;
  }
  public RenderContext withJsIdentiferSyntax(JsIdentifierSyntax s) {
    return this.jsIdentifierSyntax != s
        ? derive(markupMode, propertyNameQuotingMode, s)
        : this;
  }

  /** Always true now. */
  @Deprecated
  @SuppressWarnings("static-method")
  public final boolean isEmbeddable() { return true; }
  /** Always true now. */
  @Deprecated
  @SuppressWarnings("static-method")
  public final boolean isAsciiOnly() { return true; }
  /** Use {@link #propertyNameQuotingMode} instead. */
  @Deprecated
  public final boolean rawObjKeys() {
    return propertyNameQuotingMode == PropertyNameQuotingMode.NO_QUOTES;
  }
  /**
   * Has no effect any more.
   * @param b Unused.  Provided for backwards compatibility.
   */
  @Deprecated
  public RenderContext withAsciiOnly(boolean b) {
    return this;
  }
  /**
   * Has no effect any more.
   * @param b Unused.  Provided for backwards compatibility.
   */
  @Deprecated
  public RenderContext withEmbeddable(boolean b) {
    return this;
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
}
