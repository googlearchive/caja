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

package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;
import java.io.Serializable;
import java.util.List;

/**
 * A regular expression literal like <code>/foo/i</code>.
 *
 * @author mikesamuel@gmail.com
 */
public final class RegexpLiteral extends Literal {
  private static final long serialVersionUID = 4158496971004173179L;
  private final RegexpWrapper value;

  /** @param children unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public RegexpLiteral(
      FilePosition pos, RegexpWrapper value,
      List<? extends ParseTreeNode> children) {
    this(pos, value);
  }

  public RegexpLiteral(FilePosition pos, RegexpWrapper value) {
    super(pos);
    this.value = value;
  }

  public RegexpLiteral(FilePosition pos, String value) {
    this(pos, new RegexpWrapper(value));
  }

  @Override
  public RegexpWrapper getValue() {
    return value;
  }

  @Override
  public void render(RenderContext rc) {
    TokenConsumer out = rc.getOut();
    out.mark(getFilePosition());

    String body = value.getMatchText();
    String mods = value.getModifiers();
    if ("".equals(body) || !areRegexpModifiersValid(mods)) {
      // (new (/./.constructor))('', 'g')
      out.consume("(");
      out.consume("new");
      out.consume("(");
      out.consume("/./");
      out.consume(".");
      out.consume("constructor");
      out.consume(")");
      out.consume("(");
      StringLiteral.renderUnquotedValue(body, rc);
      out.consume(",");
      StringLiteral.renderUnquotedValue(mods, rc);
      out.consume(")");
      out.consume(")");
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append('/');
      Escaping.normalizeRegex(body, sb);
      sb.append('/');
      sb.append(mods);
      out.consume(sb.toString());
    }
  }

  public static class RegexpWrapper implements Serializable {
    private static final long serialVersionUID = -5707593923346724185L;
    private final String regexpText;

    public RegexpWrapper(String s) {
      if (null == s) { throw new NullPointerException(); }
      this.regexpText = s;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof RegexpWrapper
          && ((RegexpWrapper) o).regexpText.equals(this.regexpText);
    }

    @Override
    public int hashCode() {
      return this.regexpText.hashCode() ^ 0x8aed26a5;
    }

    @Override
    public String toString() {
      return this.regexpText;
    }

    public static RegexpWrapper valueOf(String pattern, String modifiers) {
      StringBuilder sb = new StringBuilder();
      sb.append('/');
      Escaping.normalizeRegex(pattern, sb);
      sb.append('/').append(modifiers);
      return new RegexpWrapper(sb.toString());
    }

    public String getMatchText() {
      return regexpText.substring(1, regexpText.lastIndexOf('/'));
    }

    public String getModifiers() {
      return regexpText.substring(regexpText.lastIndexOf('/') + 1);
    }
  }

  @Override
  public boolean getValueInBooleanContext() {
    return true;
  }

  /**
   * Modifiers are the letters allowed to follow a regular expression literal.
   * Firefox does not recognize the "s" modifier, and no version of javascript
   * deals with the "x" or "e" modifiers, so this regex matches g, i, and m in
   * any order without duplicates.
   */
  public static boolean areRegexpModifiersValid(String flags) {
    final int GROUP = 1;
    final int CASE_INSENSITIVE = 2;
    final int MULTILINE = 4;
    int seen = 0;
    for (int i = 0, n = flags.length(); i < n; ++i) {
      char flag = flags.charAt(i);
      int flagMask;
      switch (flag) {
        case 'g':
          flagMask = GROUP;
          break;
        case 'i':
          flagMask = CASE_INSENSITIVE;
          break;
        case 'm':
          flagMask = MULTILINE;
          break;
        default:
          return false;
      }
      if ((seen & flagMask) != 0) {
        return false;
      }
      seen = seen | flagMask;
    }
    return true;
  }

  // "function" on some interpreters, "object" on others.
  public String typeOf() { return null; }
}
