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

package com.google.caja.ancillary.opt;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.RegexpLiteral;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Collects frequently used literals, e.g. long strings and the like, into
 * a constant pool at the top of each top level function.
 *
 * @author mikesamuel@gmail.com
 */
public class ConstantPooler {
  public static Block optimize(Block program) {
    program = (Block) program.clone();
    optimizeTopLevelFunctions(program);
    return program;
  }

  private static void optimizeTopLevelFunctions(ParseTreeNode node) {
    if (node instanceof FunctionConstructor) {
      optimizeWithin((FunctionConstructor) node);
    } else {
      for (ParseTreeNode child : node.children()) {
        optimizeTopLevelFunctions(child);
      }
    }
  }

  private static void optimizeWithin(FunctionConstructor fc) {
    final Map<LitVal, LitVal> uses = Maps.newLinkedHashMap();
    Block body = fc.getBody();
    body.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> chain) {
        if (chain.node instanceof Literal
            && !(chain.node instanceof RegexpLiteral)) {
          AncestorChain<Literal> litAc = chain.cast(Literal.class);
          LitVal key = new LitVal(litAc);
          LitVal stored = uses.get(key);
          if (stored == null) {
            uses.put(key, stored = key);
          }
          stored.uses.add(litAc);
        } else if (chain.node instanceof ObjectConstructor) {
          List<? extends ParseTreeNode> children = chain.node.children();
          for (int i = 1, n = children.size(); i < n; i += 2) {
            visit(chain.child(children.get(i)));
          }
          return false;
        }
        return true;
      }
    }, null);
    List<Declaration> decls = Lists.newArrayList();
    FilePosition pos = FilePosition.startOf(body.getFilePosition());
    for (LitVal v : uses.values()) {
      // Size now = canonLen * nUses.
      // Size after = "var aa=".length + canonLen + ";".length + "aa" * nUses
      // Switch if now > after;
      //           canonLen * nUses > 8 + canonLen + 2 * nUses
      int requiredSavings = 30;  // TUNING PARAMETER
      int canonLen = v.canonForm().length();
      int nUses = v.uses.size();
      if (canonLen * nUses > 8 + canonLen + 2 * nUses + requiredSavings) {
        // TODO(mikesamuel): choose a guaranteed non-interfering name.
        String name = "$_$__litpool__" + decls.size() + "$_$";
        decls.add(new Declaration(
            pos, new Identifier(pos, name), v.uses.get(0).node));
        for (AncestorChain<Literal> use : v.uses) {
          Reference ref = new Reference(
              new Identifier(use.node.getFilePosition(), name));
          use.parent.cast(MutableParseTreeNode.class).node
              .replaceChild(ref, use.node);
        }
      }
    }
    if (!decls.isEmpty()) {
      Statement first = body.children().get(0);
      MultiDeclaration md;
      if (first instanceof MultiDeclaration) {
        md = (MultiDeclaration) first;
      } else if (first instanceof Declaration) {
        md = new MultiDeclaration(
            FilePosition.span(pos, first.getFilePosition()),
            Collections.singletonList((Declaration) first));
        body.replaceChild(md, first);
      } else if (decls.size() == 1) {
        body.insertBefore(decls.get(0), first);
        return;
      } else {
        md = new MultiDeclaration(pos, Collections.<Declaration>emptyList());
        body.insertBefore(md, first);
      }
      MutableParseTreeNode.Mutation mut = md.createMutation();
      Declaration firstDecl = md.children().get(0);
      for (Declaration decl : decls) {
        mut = mut.insertBefore(decl, firstDecl);
      }
      mut.execute();
    }
  }

  private static class LitVal {
    final Object canonValue;
    final List<AncestorChain<Literal>> uses = Lists.newArrayList();

    LitVal(AncestorChain<Literal> useAc) {
      Literal use = useAc.node;
      canonValue = use instanceof StringLiteral
          ? ((StringLiteral) use).getUnquotedValue() : use.getValue();
    }

    String canonForm() {
      StringBuilder sb = new StringBuilder();
      Literal use = uses.get(0).node;
      RenderContext rc = new RenderContext(use.makeRenderer(sb, null));
      use.render(rc);
      rc.getOut().noMoreTokens();
      return sb.toString();
    }

    @Override
    public int hashCode() { return canonValue.hashCode(); }
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof LitVal)) { return false; }
      return this.canonValue.equals(((LitVal) o).canonValue);
    }
  }
}
