// Copyright 2008 Google Inc. All Rights Reserved
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

package com.google.caja.parser.quasiliteral;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParserBase;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.StringLiteral;

import java.util.List;
import java.util.Map;

/**
 * A quasi-node like {@code "@foo"} where matching against a string literal
 * whose content is a valid identifier, will bind the name {@code "foo"} to a
 * valid {@link Identifier} whose value is the content of the string literal.
 * Substituting into {@code "@foo"} will find a {@link Reference} or
 * {@link Identifier} binding reference for {@code "foo"} and produce
 * a string literal containing the identifier name.
 *
 * @author mikesamuel@gmail.com
 */
class StringLiteralQuasiNode extends QuasiNode {
  private final String bindingName;

  StringLiteralQuasiNode(String bindingName) {
    this.bindingName = bindingName;
  }

  @Override
  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens, Map<String, ParseTreeNode> bindings) {
    if (specimens.isEmpty()) { return false; }
    ParseTreeNode specimen = specimens.get(0);
    if (!(specimen instanceof StringLiteral)) { return false; }
    StringLiteral lit = (StringLiteral) specimen;
    String ident = lit.getUnquotedValue();
    if (!ParserBase.isJavascriptIdentifier(ident)) { return false; }

    ParseTreeNode existing = bindings.get(bindingName);
    if (existing != null
        && !(existing instanceof Identifier
             && ident.equals(existing.getValue()))) {
      return false;
    } else {
      Identifier identBinding = new Identifier(lit.getFilePosition(), ident);
      bindings.put(bindingName, identBinding);
      specimens.remove(0);
      return true;
    }
  }

  @Override
  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes, Map<String, ParseTreeNode> bindings) {
    ParseTreeNode binding = bindings.get(bindingName);
    Identifier ident;
    if (binding instanceof Identifier) {
      ident = (Identifier) binding;
    } else if (binding instanceof Reference) {
      ident = ((Reference) binding).getIdentifier();
    } else {
      return false;
    }
    if (ident.getName() == null) { return false; }
    StringLiteral sl = StringLiteral.valueOf(
        ident.getFilePosition(), ident.getName());
    substitutes.add(sl);
    return true;
  }
}
