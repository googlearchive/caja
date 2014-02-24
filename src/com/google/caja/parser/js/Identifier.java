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

package com.google.caja.parser.js;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParserBase;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An identifier used in JavaScript source.
 *
 * @author ihab.awad@gmail.com
 */
public final class Identifier extends AbstractParseTreeNode {
  private static final long serialVersionUID = 4119434470934007127L;
  private final String name;

  @ReflectiveCtor
  public Identifier(
      FilePosition pos, String name, List<? extends ParseTreeNode> children) {
    this(pos, name);
    assert(children.isEmpty());
  }

  public Identifier(FilePosition pos, String name) {
    super(pos);
    if (!(name == null || "".equals(name) || isValid(name)) ||
        (name != null && name.length() > 1024)) {
      // Disallowed in Parser, so no code should ever produce something that
      // reaches here unless it concatenates two strings together without
      // normalizing the result.
      // This is meant to prevent rendered output from containing identifiers
      // that are not normalized as required by EcmaScript3.1 Chapter 6.
      throw new IllegalArgumentException("Invalid identifier " + name);
    }
    this.name = name;
  }

  @Override
  public String getValue() {
    return name;
  }

  public String getName() { return name; }

  public void render(RenderContext r) {
    if (name != null) {
      switch (r.jsIdentifierSyntax()) {
        case JAVASCRIPT:
          if (!isValidJs(name)) {
            throw new RuntimeException(
                "Cannot render invalid JavaScript identifier: " + name);
          }
          break;
        case QUASILITERAL:
          if (!(isValidJs(name) || isValidQuasiliteral(name))) {
            throw new RuntimeException(
                "Cannot render invalid Quasiliteral identifier: " + name);
          }
          break;
        case GWT:
          if (!(isValidJs(name) || isValidGWT(name))) {
            throw new RuntimeException(
                "Cannot render invalid GWT identifier: " + name);
          }
          break;
        default:
          throw new SomethingWidgyHappenedError(
              "Unrecognized JsIdentifierSyntax enum");
      }
      StringBuilder escapedName = new StringBuilder();
      if ("".equals(name)) {
        escapedName.append("(blank identifier)"); // break parser
      } else {
        Escaping.escapeJsIdentifier(name, true, escapedName);
        r.getOut().mark(getFilePosition());
        r.getOut().consume(escapedName.toString());
      }
    }
  }

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new JsPrettyPrinter(new Concatenator(out, exHandler));
  }

  private static boolean isValid(String name) {
    return isValidJs(name) || isValidQuasiliteral(name) || isValidGWT(name);
  }

  private static boolean isValidJs(String name) {
    return ParserBase.isJavascriptIdentifier(name);
  }

  private static boolean isValidQuasiliteral(String name) {
    return ParserBase.isQuasiIdentifier(name);
  }

  private static final Pattern GWT_IDENTIFIER_RE;

  static {
    // Basic components
    String letter = "(\\p{javaJavaIdentifierStart})";
    String letterOrDigit = "(\\p{javaJavaIdentifierPart})";
    String identifier = "(" + letter + letterOrDigit + "*" + ")";

    // Member references are like "com.foo.MyClass::myField"
    String fullClassName = "(" + identifier + "(\\." + identifier + ")*)";
    String memberReference = "(" + fullClassName + "\\:\\:" + identifier + ")";

    // Type references are like "Ljava/lang/String;" or "Z" or "[D"
    String fullClassTypeName = "(" + identifier + "(\\/" + identifier + ")*)";
    String simpleTypeName = "((L" + fullClassTypeName + ";)|Z|B|C|S|I|J|F|D)";
    String arrayTypeName = "((\\[)*" + simpleTypeName + ")";

    // Method references are like "com.foo.MyClass::myMeth(Ljava/lang/String;)"
    String methodReference =
        "(" + memberReference + "\\(" + arrayTypeName + "*\\))";

    // Identifiers start with "@" and are followed by either
    // a member or a method reference
    String gwtIdentifier =
        "^@(" + memberReference + "|" + methodReference + ")$";

    GWT_IDENTIFIER_RE = Pattern.compile(gwtIdentifier);
  }

  /* package private for testing */ static boolean isValidGWT(String name) {
    return GWT_IDENTIFIER_RE.matcher(name).matches();
  }
}
