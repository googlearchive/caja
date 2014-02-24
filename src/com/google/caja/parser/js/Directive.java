// Copyright (C) 2008 Google Inc.
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

package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import java.io.IOException;
import java.util.List;

/**
 * An element of a {@code DirectivePrologue}.
 *
 * @author mikesamuel@gmail.com
 * @see DirectivePrologue
 */
public final class Directive extends AbstractParseTreeNode {
  private static final long serialVersionUID = 946831596035589021L;

  /**
   * The directive strings recognized by Caja.
   */
  public enum RecognizedValue {

    /**
     * Directive invoking ES5 "strict" mode.
     */
    USE_STRICT("use strict");

    private final String directiveString;

    RecognizedValue(String directiveString) {
      this.directiveString = directiveString;
    }

    /**
     * @return the representation of this directive in source code.
     */
    public String getDirectiveString() { return directiveString; }

    public static boolean isDirectiveStringRecognized(String directiveString) {
      for (RecognizedValue v : values()) {
        if (v.directiveString.equals(directiveString)) { return true; }
      }
      return false;
    }
  }

  private final String directiveString;

  /** @param children unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public Directive(
      FilePosition pos, String directiveString, List<NoChildren> children) {
    this(pos, directiveString);
  }

  public Directive(FilePosition pos, String directiveString) {
    super(pos);
    if (directiveString == null) { throw new NullPointerException(); }
    this.directiveString = directiveString;
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (!children().isEmpty()) { throw new IndexOutOfBoundsException(); }
  }

  @Override
  public String getValue() { return directiveString; }

  public String getDirectiveString() { return directiveString; }

  public void render(RenderContext rc) {
    StringBuilder escaped = new StringBuilder();
    escaped.append('\'');  // Not allowed in JSON so always use single quotes.
    Escaping.escapeJsString(getValue(), true, true, escaped);
    escaped.append('\'');
    String escapedString = escaped.toString();
    if (!escapedString.contains(directiveString)) {
      // Escaping has modified the directive. Render nothing.
      // See http://code.google.com/p/google-caja/issues/detail?id=1111
      return;
    }
    rc.getOut().consume(escapedString);
    rc.getOut().consume(";");
  }

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new JsPrettyPrinter(new Concatenator(out, exHandler));
  }
}
