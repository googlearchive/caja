// Copyright (C) 2009 Google Inc.
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

package com.google.caja.plugin.templates;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.parser.quasiliteral.ReservedNames;
import com.google.caja.plugin.CssRuleRewriter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Attaches CSS to either the static HTML or the uncajoled JS as appropriate.
 * <p>
 * Depends on <code>emitCss___</code> as defined in DOMita.
 *
 * @author mikesamuel@gmail.com
 */
final class SafeCssMaker {
  private final Node safeHtml;
  private final Block safeJs;
  private final List<CssTree.StyleSheet> validatedStylesheets;

  SafeCssMaker(Node safeHtml, Block safeJs,
               List<CssTree.StyleSheet> validatedStylesheets) {
    this.safeHtml = safeHtml;
    this.safeJs = safeJs;
    this.validatedStylesheets = validatedStylesheets;
  }

  void make() {
    if (validatedStylesheets.isEmpty()) { return; }

    // Accumulates dynamic CSS that will be added to the JS.
    List<Expression> cssParts = new ArrayList<Expression>();
    // Accumulate static CSS that can be embedded in the DOM.
    StringBuilder css = new StringBuilder();
    FilePosition staticPos = null, dynamicPos = null;
    for (CssTree.StyleSheet ss : validatedStylesheets) {
      ArrayConstructor ac = CssRuleRewriter.cssToJs(ss);
      List<? extends Expression> children = ac.children();
      if (children.isEmpty()) { continue; }
      FilePosition acPos = ac.getFilePosition();
      Expression child0 = children.get(0);
      // The CssRuleRewriter gets to distinguish between static and dynamic.
      // If the output is a single string, then joining it on the idClass would
      // not add any information, so we can put it in the static HTML.
      if (children.size() == 1 && child0 instanceof StringLiteral) {
        css.append('\n').append(((StringLiteral) child0).getUnquotedValue());
        staticPos = staticPos == null
            ? acPos : FilePosition.span(staticPos, acPos);
      } else {
        // Don't just push all onto the list since that would create an
        // extra, spurious separator after they're joined.
        // To avoid the spurious separator, we concatenate the last item
        // already on cssParts with child0.
        int n = cssParts.size();
        if (n == 0) {
          cssParts.addAll(children);
        } else {
          JsConcatenator cat = new JsConcatenator();
          cat.append(cssParts.get(n - 1));
          cat.append(FilePosition.startOf(child0.getFilePosition()), "\n");
          cat.append(child0);
          cssParts.set(n - 1, cat.toExpression(false));
          cssParts.addAll(children.subList(1, children.size()));
        }
        dynamicPos = dynamicPos == null
            ? acPos : FilePosition.span(dynamicPos, acPos);
      }
    }

    // Emit any dynamic CSS.
    if (!cssParts.isEmpty()) {
      Statement firstChild = safeJs.children().isEmpty()
          ? null : safeJs.children().get(0);
      // The CSS rule
      //     p { color: purple }
      // is converted to the JavaScript
      //     IMPORTS___.emitCss___(
      //         ['.', ' p { color: purple }']
      //         .join(IMPORTS___.getIdClass___()));
      //
      // If IMPORTS___.getIdClass() returns "g123___", then the resulting
      //     .g123___ p { color: purple }
      // will only make purple paragraphs that are under a node with class
      // g123__.
      safeJs.insertBefore(new ExpressionStmt(
          dynamicPos,
          (Expression) QuasiBuilder.substV(
              ReservedNames.IMPORTS
              + ".emitCss___(@cssParts./*@synthetic*/join("
              + ReservedNames.IMPORTS + ".getIdClass___()))",
              "cssParts", new ArrayConstructor(dynamicPos, cssParts))),
          firstChild);
    }

    // Emit any static CSS.
    Node safeHtml = this.safeHtml;
    if (css.length() != 0) {
      Document doc = safeHtml.getOwnerDocument();
      String nsUri = Namespaces.HTML_NAMESPACE_URI;
      Element style = doc.createElementNS(nsUri, "style");
      style.setAttributeNS(nsUri, "type", "text/css");
      style.appendChild(doc.createTextNode(css.toString()));
      Nodes.setFilePositionFor(style, dynamicPos);
      if (!(safeHtml instanceof DocumentFragment)) {
        DocumentFragment f = doc.createDocumentFragment();
        f.appendChild(safeHtml);
        safeHtml = f;
      }
      safeHtml.insertBefore(style, safeHtml.getFirstChild());
    }
  }
}
