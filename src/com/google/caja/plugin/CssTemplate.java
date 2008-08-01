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

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.ReservedNames;
import com.google.caja.plugin.GxpCompiler.BadContentException;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a CSS template that can be compiled to a javascript function.
 *
 * @author mikesamuel@gmail.com
 */
public final class CssTemplate extends AbstractParseTreeNode<ParseTreeNode> {
  CssTemplate(FilePosition pos, Identifier name,
              List<? extends Identifier> formalNames,
              CssTree.DeclarationGroup css) {
    this.setFilePosition(pos);
    createMutation()
        .appendChild(name)
        .appendChildren(formalNames)
        .appendChild(css)
        .execute();
  }

  public CssTemplate(FilePosition pos, CssTree.DeclarationGroup css) {
    this(pos, blankIdentifier(FilePosition.startOf(pos)),
         Collections.<Identifier>emptyList(), css);
  }

  private static Identifier blankIdentifier(FilePosition pos) {
    Identifier blank = new Identifier(null);
    blank.setFilePosition(pos);
    return blank;
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
    return getTemplateNameIdentifier().getName();
  }

  public Identifier getTemplateNameIdentifier() {
    return ((Identifier) children().get(0));
  }

  @SuppressWarnings("unchecked")
  public Iterable<? extends Identifier> getTemplateParamDeclarations() {
    return (List<Identifier>) children().subList(1, children().size() - 1);
  }

  public Iterable<String> getTemplateParamNames() {
    return new Iterable<String>() {
      public Iterator<String> iterator() {
        final Iterator<? extends Identifier> paramNames =
            getTemplateParamDeclarations().iterator();
        return new Iterator<String>() {
          public boolean hasNext() { return paramNames.hasNext(); }
          public String next() {
            return paramNames.next().getName();
          }
          public void remove() { throw new UnsupportedOperationException(); }
        };
      }
    };
  }

  public CssTree.DeclarationGroup getCss() {
    return (CssTree.DeclarationGroup) children().get(children().size() - 1);
  }

  public void render(RenderContext r) {
    // TODO(mikesamuel): implement me
    throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
  }

  public TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    throw new UnsupportedOperationException();
  }

  /**
   * Builds a function that takes parameters and returns blessed CSS.
   */
  public FunctionConstructor toFunction(CssSchema cssSchema, MessageQueue mq)
      throws BadContentException {
    List<FormalParam> params = new ArrayList<FormalParam>();
    for (Identifier formal : getTemplateParamDeclarations()) {
      FormalParam p = new FormalParam(formal);
      p.setFilePosition(formal.getFilePosition());
      params.add(p);
    }

    ReturnStmt result = new ReturnStmt(toPropertyValueList(cssSchema, mq));
    result.setFilePosition(getCss().getFilePosition());

    // function (<formals>) {
    //   return IMPORTS___.blessCss___(...);
    // }
    FunctionConstructor fn = new FunctionConstructor(
         getTemplateNameIdentifier(), params,
         new Block(Collections.singletonList(result)));
    fn.setFilePosition(this.getFilePosition());
    return fn;
  }

  /**
   * Returns a call to {@code IMPORTS___.blessCss__} with alternating
   * property names and expressions.
   *
   * Since the DOM2 Style object uses different names than the CSS2 standard,
   * the property names may have a comma in which case they have the form
   * {@code <css-name>;<dom-name>}.
   */
  public Expression toPropertyValueList(CssSchema cssSchema, MessageQueue mq)
      throws BadContentException {
    List<Expression> parts = new ArrayList<Expression>();

    declGroupToDeltaArray(getCss(), cssSchema, parts, mq);

    // IMPORTS___.blessCss___('property-name', <propertyValue>, ...);
    List<Expression> blessCallOperands = new ArrayList<Expression>();
    blessCallOperands.add(
        TreeConstruction.memberAccess(
            ReservedNames.IMPORTS, ReservedNames.BLESS_CSS));
    blessCallOperands.addAll(parts);

    // return IMPORTS___.blessCss___(...);
    Operation blessCall = TreeConstruction.call(
        blessCallOperands.toArray(new Expression[0]));
    blessCall.setFilePosition(getCss().getFilePosition());

    return blessCall;
  }

  private static void declGroupToDeltaArray(
      CssTree.DeclarationGroup declGroup, final CssSchema cssSchema,
      final List<Expression> out, MessageQueue mq)
      throws BadContentException {

    declarationsToJavascript(
        declGroup, JsWriter.Esc.NONE, mq, new DynamicCssReceiver() {
            List<Expression> valueParts;

            public void startDeclaration() {
              valueParts = new ArrayList<Expression>();
            }

            public void property(CssTree.Property p) {
              String pn = p.getPropertyName();
              CssSchema.CssPropertyInfo propInfo = cssSchema.getCssProperty(pn);

              // Encode enough information so that both the partial style setter
              // which uses (htmlElement.style.foo = ...) and the style attrib
              // replacer which uses (htmlElement.setAttribute('style', ...)
              // can choose appropriate property names.
              if (!pn.equals(propInfo.dom2property)) {
                pn = pn + ";" + propInfo.dom2property;
              }
              StringLiteral nameLit = new StringLiteral(
                  StringLiteral.toQuotedValue(pn));
              nameLit.setFilePosition(p.getFilePosition());
              out.add(nameLit);
            }

            public void expr(Expression e) {
              valueParts.add(e);
            }

            public void rawCss(String rawCss) {
              if ("".equals(rawCss)) { return; }
              valueParts.add(
                  new StringLiteral(StringLiteral.toQuotedValue(rawCss)));
            }

            public void priority(CssTree.Prio p) {}

            public void endDeclaration() {
              Expression e = valueParts.get(0);
              for (int i = 1, n = valueParts.size(); i < n; ++i) {
                e = Operation.create(
                    Operator.ADDITION, e, valueParts.get(i));
              }
              valueParts = null;
              out.add(e);
            }
          });
  }

  static void declGroupToStyleValue(
      CssTree.DeclarationGroup cssTree, final List<String> tgtChain,
      final Block b, final JsWriter.Esc esc, MessageQueue mq)
      throws BadContentException {


    declarationsToJavascript(cssTree, esc, mq, new DynamicCssReceiver() {
        boolean first = true;

        public void startDeclaration() {}

        public void property(CssTree.Property p) {
          StringBuilder out = new StringBuilder();
          TokenConsumer tc = p.makeRenderer(out, null);
          if (first) {
            first = false;
          } else {
            tc.consume(";");
          }
          p.render(new RenderContext(new MessageContext(), tc));
          tc.consume(":");
          out.append(" ");
          rawCss(out.toString());
        }

        public void expr(Expression dynamic) {
          JsWriter.append(dynamic, tgtChain, b);
        }

        public void rawCss(String rawCss) {
          JsWriter.appendText(rawCss, esc, tgtChain, b);
        }

        public void priority(CssTree.Prio p) {
          StringBuilder out = new StringBuilder();
          out.append(" ");
          TokenConsumer tc = p.makeRenderer(out, null);
          p.render(new RenderContext(new MessageContext(), tc));
          rawCss(out.toString());
        }

        public void endDeclaration() {}
      });
  }

  private static interface DynamicCssReceiver {
    void startDeclaration();

    void property(CssTree.Property p);

    void expr(Expression dynamic);

    void rawCss(String rawCss);

    void priority(CssTree.Prio p);

    void endDeclaration();
  }

  private static void declarationsToJavascript(
      CssTree.DeclarationGroup decls, JsWriter.Esc esc, MessageQueue mq,
      DynamicCssReceiver out)
      throws BadContentException {
    assert esc == JsWriter.Esc.NONE || esc == JsWriter.Esc.HTML_ATTRIB : esc;

    // Replace any substitutions with placeholders.
    // The below puts finds all substitutions and replaces them with the
    // substitution $(index) so that the output text can be html escaped, and
    // then turned into a sequence of string concatenations
    final List<CssTree.Substitution> substitutions =
        new ArrayList<CssTree.Substitution>();
    decls.acceptPreOrder(new Visitor() {
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

        CssTree.Substitution placeholder = new CssTree.Substitution(
            node.getFilePosition(), "${\0" + index + "\0}");
        term.replaceChild(placeholder, node);

        substitutions.add(sub);
        return false;
      }
    }, null);

    for (CssTree child : decls.children()) {
      CssTree.Declaration decl = (CssTree.Declaration) child;
      // Render the style to a canonical form with consistent escaping
      // conventions, so that we can avoid browser bugs.
      String css;
      {
        StringBuilder cssBuf = new StringBuilder();
        TokenConsumer tc = decl.makeRenderer(cssBuf, null);
        decl.getExpr().render(new RenderContext(new MessageContext(), tc));

        // Contains the rendered CSS with ${\0###\0} placeholders.
        // Split around the placeholders, parse the javascript, escape the
        // literal text, and emit the appropriate javascript.
        css = cssBuf.toString();
      }

      out.startDeclaration();
      out.property(decl.getProperty());

      int pos = 0;
      while (pos < css.length()) {
        int start = css.indexOf("${\0", pos);
        if (start < 0) { break; }
        int end = css.indexOf("\0}", start + 3);
        int index = Integer.valueOf(css.substring(start + 3, end));

        out.rawCss(css.substring(pos, start));
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
            // IMPORTS___.cssNumber___(...)
            e = TreeConstruction.call(
                TreeConstruction.memberAccess(
                    ReservedNames.IMPORTS, ReservedNames.CSS_NUMBER),
                e);
            suffix = sub.getSuffix();
            break;
          case URI:
            // IMPORTS___.cssUri___(...)
            e = TreeConstruction.call(
                TreeConstruction.memberAccess(
                    ReservedNames.IMPORTS, ReservedNames.CSS_URI),
                e);
            if (esc == JsWriter.Esc.HTML_ATTRIB) {
              e = TreeConstruction.call(
                  TreeConstruction.memberAccess(
                      ReservedNames.IMPORTS, ReservedNames.HTML_ATTR),
                  e);
            }
            suffix = "";
            break;
          case COLOR:
            // IMPORTS___.cssColor___(...)
            e = TreeConstruction.call(
                TreeConstruction.memberAccess(
                    ReservedNames.IMPORTS, ReservedNames.CSS_COLOR),
                e);
            suffix = "";
            break;
          default:
            throw new BadContentException(
                new Message(PluginMessageType.CSS_SUBSTITUTION_NOT_ALLOWED_HERE,
                            sub.getFilePosition(),
                            MessagePart.Factory.valueOf(t.name())));
        }
        out.expr(e);
        out.rawCss(suffix);
      }
      out.rawCss(css.substring(pos));

      if (decl.getPrio() != null) { out.priority(decl.getPrio()); }
      out.endDeclaration();
    }
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
