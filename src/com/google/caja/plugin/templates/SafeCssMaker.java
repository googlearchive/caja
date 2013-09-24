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
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.parser.quasiliteral.ReservedNames;
import com.google.caja.plugin.CssDynamicExpressionRewriter;
import com.google.caja.plugin.JobEnvelope;
import com.google.common.collect.Lists;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Attaches CSS to either the static HTML or the uncajoled JS as appropriate.
 * <p>
 * Depends on <code>emitCss___</code> as defined in Domado.
 *
 * @author mikesamuel@gmail.com
 */
final class SafeCssMaker {
  private final List<? extends ValidatedStylesheet> validatedStylesheets;
  private final Document doc;

  SafeCssMaker(
      List<? extends ValidatedStylesheet> validatedStylesheets,
      Document doc) {
    this.validatedStylesheets = validatedStylesheets;
    this.doc = doc;
  }

  List<SafeStylesheet> make() {
    if (validatedStylesheets.isEmpty()) { return Collections.emptyList(); }

    List<SafeStylesheet> out = Lists.newArrayList();

    JobEnvelope currentSource = validatedStylesheets.get(0).source;
    URI currentUri = validatedStylesheets.get(0).baseUri;
    int pos = 0;
    int n = validatedStylesheets.size();
    for (int i = 0; i < n; ++i) {
      ValidatedStylesheet vss = validatedStylesheets.get(i);
      if (!(vss.source.areFromSameSource(currentSource)
            && vss.baseUri.equals(currentUri))) {
        out.add(make(
            validatedStylesheets.subList(pos, i), currentSource, currentUri));
        currentSource = vss.source;
        currentUri = vss.baseUri;
        pos = i;
      }
    }
    out.add(make(
        validatedStylesheets.subList(pos, n), currentSource, currentUri));

    return out;
  }

  private SafeStylesheet make(
      List<? extends ValidatedStylesheet> validatedStylesheets,
      JobEnvelope source, URI baseUri) {

    // Accumulates dynamic CSS that will be added to the JS.
    List<Expression> cssParts = Lists.newArrayList();
    // Accumulate static CSS that can be embedded in the DOM.
    StringBuilder css = new StringBuilder();
    FilePosition staticPos = null, dynamicPos = null;
    for (ValidatedStylesheet ss : validatedStylesheets) {
      ArrayConstructor ac = CssDynamicExpressionRewriter.cssToJs(ss.ss);
      List<? extends Expression> children = ac.children();
      if (children.isEmpty()) { continue; }
      FilePosition acPos = ac.getFilePosition();
      Expression child0 = children.get(0);
      // The CssDynamicExpressionRewriter gets to distinguish between static and
      // dynamic. If the output is a single string, then joining it on the
      // idClass would not add any information, so we can put it in the static
      // HTML.
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

    ExpressionStmt dynamicCss = null;
    Element staticCss = null;
    // Emit any dynamic CSS.
    if (!cssParts.isEmpty()) {
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
      dynamicCss = new ExpressionStmt(
          dynamicPos,
          (Expression) QuasiBuilder.substV(
              ReservedNames.IMPORTS
              + ".emitCss___(@cssParts./*@synthetic*/join("
              + ReservedNames.IMPORTS + ".getIdClass___()))",
              "cssParts", new ArrayConstructor(dynamicPos, cssParts)));
    }

    // Emit any static CSS.
    if (css.length() != 0) {
      String nsUri = Namespaces.HTML_NAMESPACE_URI;
      Element style = doc.createElementNS(nsUri, "style");
      style.setAttributeNS(nsUri, "type", "text/css");
      style.appendChild(doc.createTextNode(css.toString()));
      Nodes.setFilePositionFor(style, dynamicPos);
      staticCss = style;
    }

    return staticCss != null
        ? new SafeStylesheet(source, staticCss, baseUri)
        : new SafeStylesheet(source, dynamicCss, baseUri);
  }
}
