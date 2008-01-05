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

package com.google.caja.plugin;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.plugin.GxpCompiler.BadContentException;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import static com.google.caja.plugin.SyntheticNodes.s;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a CSS template that can be compiled to a javascript function.
 *
 * @author mikesamuel@gmail.com
 */
final class CssTemplate extends AbstractParseTreeNode<CssTree> {
  CssTemplate(FilePosition pos, CssTree.FunctionCall name,
              List<? extends CssTree.FunctionCall> params,
              CssTree css) {
    this.setFilePosition(pos);
    createMutation()
        .appendChild(name)
        .appendChildren(params)
        .appendChild(css)
        .execute();
  }

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public void childrenChanged() {
    super.childrenChanged();
    getCss();
    getTemplateName();
    for (String name : getTemplateParamNames()) {
      if (name == null) {
        throw new NullPointerException("Missing parameter name");
      }
    }
  }

  public String getTemplateName() {
    CssTree.Term nameTerm =
      getTemplateDeclaration().getArguments().getNthTerm(0);
    return ((CssTree.StringLiteral) nameTerm.getExprAtom()).getValue();
  }

  public CssTree.FunctionCall getTemplateDeclaration() {
    return (CssTree.FunctionCall) children().get(0);
  }

  public Iterable<? extends CssTree.FunctionCall>
    getTemplateParamDeclarations() {
    return new Iterable<CssTree.FunctionCall>() {
      public Iterator<CssTree.FunctionCall> iterator() {
        final Iterator<? extends CssTree> paramDecls =
            children().subList(1, children().size() - 1).iterator();
        return new Iterator<CssTree.FunctionCall>() {
          public boolean hasNext() { return paramDecls.hasNext(); }
          public CssTree.FunctionCall next() {
            return (CssTree.FunctionCall) paramDecls.next();
          }
          public void remove() { throw new UnsupportedOperationException(); }
        };
      }
    };
  }

  public Iterable<String> getTemplateParamNames() {
    return new Iterable<String>() {
      public Iterator<String> iterator() {
        final Iterator<? extends CssTree> paramDecls =
            children().subList(1, children().size() - 1).iterator();
        return new Iterator<String>() {
          public boolean hasNext() { return paramDecls.hasNext(); }
          public String next() {
            CssTree.CssExprAtom atom =
               ((CssTree.FunctionCall) paramDecls.next())
              .getArguments().getNthTerm(0).getExprAtom();
            return ((CssTree.StringLiteral) atom).getValue();
          }
          public void remove() { throw new UnsupportedOperationException(); }
        };
      }
    };
  }

  public CssTree getCss() {
    return children().get(children().size() - 1);
  }

  public void render(RenderContext r) {
    throw new UnsupportedOperationException("NOT IMPLEMENTED YET");  // TODO
  }

  public FunctionConstructor toJavascript(PluginMeta meta, MessageQueue mq)
      throws BadContentException {
    String identifier = getTemplateName();
    List<FormalParam> params = new ArrayList<FormalParam>();
    for (CssTree.FunctionCall paramDecl : getTemplateParamDeclarations()) {
      String paramName =
        (String) paramDecl.getArguments().getNthTerm(0).getExprAtom()
        .getValue();
      FormalParam formal = new FormalParam(new Identifier(paramName));
      formal.setFilePosition(paramDecl.getFilePosition());
      params.add(formal);
    }

    Block body = new Block(Collections.<Statement>emptyList());

    List<String> tgtChain = Arrays.asList("___out___", "push");
    body.insertBefore(
        s(new Declaration(
              new Identifier(tgtChain.get(0)),
              s(new ArrayConstructor(Collections.<Expression>emptyList())))),
        null);

    bodyToJavascript(getCss(), meta, tgtChain, body, JsWriter.Esc.NONE, mq);

    body.insertBefore(
        s(new ReturnStmt(
              s(new Operation(
                    Operator.FUNCTION_CALL,
                    s(new Reference(new Identifier("plugin_blessCss___"))),
                    s(new Operation(
                          Operator.FUNCTION_CALL,
                          s(new Operation(
                                Operator.MEMBER_ACCESS,
                                s(new Reference(new Identifier(tgtChain.get(0)))),
                                s(new Reference(new Identifier("join"))))),
                          s(new StringLiteral("''"))
                    ))
               ))
         )), null);
    body.setFilePosition(getCss().getFilePosition());
    FunctionConstructor fn = new FunctionConstructor(new Identifier(identifier), params, body);
    fn.setFilePosition(this.getFilePosition());
    return fn;
  }

  static void bodyToJavascript(
      CssTree cssTree, PluginMeta meta, List<String> tgtChain, Block b,
      JsWriter.Esc esc, MessageQueue mq)
      throws BadContentException {
    assert esc == JsWriter.Esc.NONE || esc == JsWriter.Esc.HTML_ATTRIB : esc;

    // Replace any substitutions with placeholders.
    // The below puts finds all substitutions and replaces them with the
    // substitution $(index) so that the output text can be html escaped, and
    // then turned into a sequence of string concatenations
    final List<CssTree.Substitution> substitutions =
      new ArrayList<CssTree.Substitution>();
    cssTree.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ancestors) {
        ParseTreeNode node = ancestors.node;
        if (!(node instanceof CssTree.Substitution)) { return true; }
        CssTree.Substitution sub = (CssTree.Substitution) node;
        CssTree.Term term = (CssTree.Term) ancestors.getParentNode();
        // Store the part type so we can get at it once sub has been removed
        // from the parse tree.
        sub.getAttributes().set(
            CssValidator.CSS_PROPERTY_PART_TYPE,
            term.getAttributes().get(CssValidator.CSS_PROPERTY_PART_TYPE));

        int index = substitutions.size();

        CssTree.Substitution placeholder =
          new CssTree.Substitution(node.getFilePosition(),
                                   "$(\0" + index + "\0)");
        term.replaceChild(placeholder, node);

        substitutions.add(sub);
        return false;
      }
    }, null);

    // Render the style to a canonical form with consistent escaping
    // conventions, so that we can avoid browser bugs.
    StringBuilder out = new StringBuilder();
    RenderContext rc = new RenderContext(new MessageContext(), out);
    try {
      cssTree.render(rc);
    } catch (IOException ex) {
      throw (AssertionError) new AssertionError(
          "IOException writing to StringBuilder").initCause(ex);
    }

    // Contains the rendered CSS with $(\0###\0) placeholders.
    // Split around the placeholders, parse the javascript, escape the literal
    // text, and emit the appropriate javascript.
    String css = out.toString();
    int pos = 0;
    while (pos < css.length()) {
      int start = css.indexOf("$(\0", pos);
      if (start < 0) { break; }
      int end = css.indexOf("\0)", start + 3);
      int index = Integer.valueOf(css.substring(start + 3, end));

      JsWriter.appendText(css.substring(pos, start), esc, tgtChain, b);
      pos = end + 2;

      CssTree.Substitution sub = substitutions.get(index);
      Expression e = asExpression(sub, mq);
      String suffix;
      CssPropertyPartType t = sub.getAttributes().get(
          CssValidator.CSS_PROPERTY_PART_TYPE);
      switch (t) {
        case ANGLE:
        case FREQUENCY:
        case INTEGER:
        case LENGTH:
        case NUMBER:
        case PERCENTAGE:
        case TIME:
          // plugin_cssNumber___(...)
          e = s(new Operation(Operator.FUNCTION_CALL,
                              s(new Reference(new Identifier("plugin_cssNumber___"))), e));
          suffix = sub.getSuffix();
          break;
        case URI:
          // plugin_cssUri___(..., PLUGIN)
          e = s(new Operation(Operator.FUNCTION_CALL,
                              s(new Reference(new Identifier("plugin_cssUri___"))), e,
                              s(new Reference(new Identifier(meta.namespacePrivateName)))));
          if (esc == JsWriter.Esc.HTML_ATTRIB) {
            e = s(new Operation(Operator.FUNCTION_CALL,
                                s(new Reference(new Identifier("plugin_htmlAttr___"))), e));
          }
          suffix = "";
          break;
        case COLOR:
          // plugin_cssHexColor___(...)
          e = s(new Operation(Operator.FUNCTION_CALL,
                              s(new Reference(new Identifier("plugin_cssColor___"))), e));
          suffix = "";
          break;
        default:
          throw new BadContentException(
              new Message(PluginMessageType.CSS_SUBSTITUTION_NOT_ALLOWED_HERE,
                          sub.getFilePosition(),
                          MessagePart.Factory.valueOf(t.name())));
      }
      JsWriter.append(e, tgtChain, b);
      JsWriter.appendText(suffix, esc, tgtChain, b);
    }

    JsWriter.appendText(css.substring(pos), esc, tgtChain, b);
  }

  private static Expression asExpression(
      CssTree.Substitution sub, MessageQueue mq) {
    FilePosition pos = sub.getFilePosition();
    CharProducer cp =
      CharProducer.Factory.fromHtmlAttribute(CharProducer.Factory.create(
          new StringReader("  " + sub.getBody()), pos));
    return JsWriter.asExpression(cp, pos, mq);
  }
}
