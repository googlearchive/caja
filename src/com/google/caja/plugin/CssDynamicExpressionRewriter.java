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

package com.google.caja.plugin;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Elision;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.render.CssPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Lists;
import com.google.caja.util.Strings;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

/**
 * Compiles CSS style-sheets to JavaScript which outputs the same CSS, but with
 * rules only affecting nodes that are children of a class whose name contains
 * the gadget id.
 *
 * @author mikesamuel@gmail.com
 */
public final class CssDynamicExpressionRewriter {

  private final @Nullable String gadgetNameSuffix;

  public CssDynamicExpressionRewriter(PluginMeta meta) {
    String idSuffix = meta.getIdClass();
    this.gadgetNameSuffix = idSuffix;
  }

  /**
   * @param ss modified destructively.
   */
  public void rewriteCss(CssTree.StyleSheet ss) {
    //     '#foo {}'                                        ; The original rule
    // =>  '#foo-' + IMPORTS___.getIdClass___() + ' {}'     ; Cajoled rule
    // =>  '#foo-gadget123___ {}'                           ; In the browser
    rewriteIds(ss);
    // Make sure that each selector only applies to nodes under a node
    // controlled by the gadget.
    //     'p { }'                                          ; The original rule
    // =>  '.' + IMPORTS___.getIdClass___() + '___ p { }'   ; Cajoled rule
    // =>  '.gadget123___ p { }'                            ; In the browser
    restrictRulesToSubtreeWithGadgetClass(ss);
    // Rewrite any UnsafeUriLiterals to JavaScript expressions that are
    // presented to the client-side URI policy when the content is rendered.
    //     'p { background: url(unsafe.png) }'              ; The original rule
    // =>  'p { background: url('                           ; Cajoled rule
    //       + IMPORTS___.rewriteUriInCss___('unsafe.png')  ;
    //       + ') }'                                        ;
    // =>  'p { background: url(safe.png) }'                ; In the browser
    rewriteUnsafeUriLiteralsToExpressions(ss);
  }

  private void rewriteIds(CssTree.StyleSheet ss) {
    // Rewrite IDs with the gadget suffix.
    ss.acceptPreOrder(new Visitor() {
          public boolean visit(AncestorChain<?> ancestors) {
            ParseTreeNode node = ancestors.node;
            if (!(node instanceof CssTree.SimpleSelector)) { return true; }
            CssTree.SimpleSelector ss = (CssTree.SimpleSelector) node;
            for (CssTree child : ss.children()) {
              if (child instanceof CssTree.IdLiteral) {
                CssTree.IdLiteral idLit = (CssTree.IdLiteral) child;
                if (gadgetNameSuffix != null) {
                  idLit.setValue(
                      "#" + idLit.getValue().substring(1)
                      + "-" + gadgetNameSuffix);
                } else {
                  ss.replaceChild(
                      new SuffixedClassOrIdLiteral(
                          idLit.getFilePosition(),
                          "#" + idLit.getValue().substring(1) + "-"),
                      idLit);
                }
              }
            }
            return true;
          }
        }, null);
  }
  private void restrictRulesToSubtreeWithGadgetClass(CssTree.StyleSheet ss) {
    ss.acceptPreOrder(new Visitor() {
          public boolean visit(AncestorChain<?> ancestors) {
            ParseTreeNode node = ancestors.node;
            if (!(node instanceof CssTree.Selector)) { return true; }
            CssTree.Selector sel = (CssTree.Selector) node;

            // A selector that describes an ancestor of all nodes matched
            // by this rule.
            CssTree.SimpleSelector baseSelector = (CssTree.SimpleSelector)
                sel.children().get(0);
            boolean baseIsDescendant = true;
            if (selectorMatchesElement(baseSelector, "body")) {
              CssTree.IdentLiteral elName = (CssTree.IdentLiteral)
                  baseSelector.children().get(0);
              baseSelector.replaceChild(new CssTree.ClassLiteral(
                  elName.getFilePosition(), ".vdoc-body___"), elName);
              baseIsDescendant = false;
            }

            // Use the start position of the base selector as the position of
            // the synthetic parts.
            FilePosition pos = FilePosition.endOf(
                baseSelector.getFilePosition());

            CssTree.CssLiteral restrictClass = gadgetNameSuffix != null
                ? new CssTree.ClassLiteral(pos, "." + gadgetNameSuffix)
                : new SuffixedClassOrIdLiteral(pos, ".");

            if (baseIsDescendant) {
              CssTree.Combination op = new CssTree.Combination(
                  pos, CssTree.Combinator.DESCENDANT);
              CssTree.SimpleSelector restrictSel = new CssTree.SimpleSelector(
                  pos, Collections.singletonList(restrictClass));

              sel.createMutation()
                 .insertBefore(op, baseSelector)
                 .insertBefore(restrictSel, op)
                 .execute();
            } else {
              baseSelector.appendChild(restrictClass);
            }
            return false;
          }
        }, null);
  }
  private void rewriteUnsafeUriLiteralsToExpressions(CssTree.StyleSheet ss) {
    ss.acceptPreOrder(new Visitor() {
          public boolean visit(AncestorChain<?> ancestors) {
            ParseTreeNode node = ancestors.node;
            if (node instanceof UnsafeUriLiteral) {
              UnsafeUriLiteral uul = (UnsafeUriLiteral) node;
              CssTree parent = (CssTree) ancestors.parent.node;
              assert(null != parent);
              parent.replaceChild(
                  new JsExpressionUriLiteral(
                      uul.getFilePosition(),
                      (Expression) QuasiBuilder.substV(
                          "IMPORTS___./*@synthetic*/rewriteUriInCss___(@uri)",
                          "uri", StringLiteral.valueOf(
                                     uul.getFilePosition(),
                                     uul.getValue()))),
                  uul);
            }
            return true;
          }
        }, null);
  }

  /**
   * Returns an array containing chunks of CSS text that can be joined on a
   * CSS identifier to yield sandboxed CSS.
   * This can be used client side with the {@code emitCss___} method defined in
   * "domita.js".
   * @param ss a rewritten stylesheet.
   */
  public static ArrayConstructor cssToJs(CssTree.StyleSheet ss) {
    // Render the CSS to a string, split it (effectively) on the
    // GADGET_ID_PLACEHOLDER to get an array of strings, and produce JavaScript
    // which joins it on the actual gadget id which is chosen at runtime.

    // The below will, if GADGET_ID_PLACEHOLDER where "X", given the sequence
    // of calls
    //    call            sb        cssParts
    //    consume("a")    "a"       []
    //    consume("bX")   ""        ["ab"]
    //    consume("cX")   ""        ["ab", "c"]
    //    consume("d")    "d"       ["ab", "c"]
    //    noMoreTokens()  ""        ["ab", "c", "d"]
    // Which has he property that the output list joined with the placeholder
    // produces the concatenation of the original string.
    EmbeddedJsExpressionTokenConsumer cssToJsArrayElements
        = new EmbeddedJsExpressionTokenConsumer();
    ss.render(new RenderContext(cssToJsArrayElements));
    cssToJsArrayElements.noMoreTokens();

    return new ArrayConstructor(
        ss.getFilePosition(), cssToJsArrayElements.getArrayMembers());
  }


  private static boolean selectorMatchesElement(
      CssTree.SimpleSelector t, String elementName) {
    return Strings.equalsIgnoreCase(elementName, t.getElementName());
  }
}


/**
 * An ID literal that should have the gadget ID added as a suffix to prevent
 * collision with other IDs.
 */
class SuffixedClassOrIdLiteral extends CssTree.CssLiteral {

  SuffixedClassOrIdLiteral(FilePosition pos, String value) {
    super(pos, value);
  }

  @Override
  public void render(RenderContext r) {
    TokenConsumer tc = r.getOut();
    tc.mark(getFilePosition());
    tc.consume(getValue());
    if (tc instanceof EmbeddedJsExpressionTokenConsumer) {
      ((EmbeddedJsExpressionTokenConsumer) tc).endArrayElement();
    } else {
      // Emit a buster that will cause the CSS parser to error out but not out
      // of the current block.
      // This allows us to debug the output of this stage, but does not
      // compromise security if the CSS is rendered by naive code.
      tc.consume("UNSAFE_UNTRANSLATED_SUFFIX:;");
    }
  }

  @Override
  protected boolean checkValue(String value) {
    return (value.startsWith("#") && value.endsWith("-"))
        || value.startsWith(".");
  }

}


/**
 * A Uri literal evaluated by calling a JavaScript Expression at run time.
 */
class JsExpressionUriLiteral extends CssTree.CssLiteral {
  private final Expression expr;

  JsExpressionUriLiteral(FilePosition pos, Expression expr) {
    super(pos, null);
    this.expr = expr;
  }

  @Override
  public void render(RenderContext r) {
    TokenConsumer tc = r.getOut();
    tc.mark(getFilePosition());
    tc.consume("url(");
    if (tc instanceof EmbeddedJsExpressionTokenConsumer) {
      ((EmbeddedJsExpressionTokenConsumer) tc).consume(expr);
    } else {
      // Emit a buster that will cause the CSS parser to error out but not out
      // of the current block.
      // This allows us to debug the output of this stage, but does not
      // compromise security if the CSS is rendered by naive code.
      tc.consume("UNSAFE_JS_EXPRESSION_LITERAL:;");
    }
    tc.consume(")");
  }

  @Override
  protected boolean checkValue(String value) { return true; }
}


/**
 * Consumes CSS tokens and calls to substitute JS expressions to produce a JS
 * array constructor that can be joined on a gadget ID to produce a string of
 * name-spaced CSS.
 */
class EmbeddedJsExpressionTokenConsumer implements TokenConsumer {
  private final StringBuilder partialJsStringLiteral = new StringBuilder();
  private boolean inJsString;
  private FilePosition positionAtStartOfStringLiteral, last;
  private final CssPrettyPrinter cssTokenConsumer =
      new CssPrettyPrinter(partialJsStringLiteral);
  private Expression pendingExpression;
  private final List<Expression> arrayElements = Lists.newArrayList();


  /**
   * Appends the given JS expression to the current array element.
   */
  public void consume(Expression jsExpression) {
    if (jsExpression instanceof StringLiteral) {
      if (!inJsString) {
        startPartialJsStringLiteral();
      }
      mark(jsExpression.getFilePosition());
      partialJsStringLiteral.append(
          ((StringLiteral) jsExpression).getUnquotedValue());
    } else {
      if (inJsString) {
        pendingExpression = combine(
            pendingExpression, endPartialJsStringLiteral());
      }
      pendingExpression = combine(pendingExpression, jsExpression);
    }
  }

  /**
   * Introduces a break between array elements.
   * So if the following calls happen in-sequence:
   * {@code this.consume("foo"); this.endArrayElement(); this.consume("bar");}
   * then the array elements will be {@code ['foo', 'bar']}.
   */
  public void endArrayElement() {
    endArrayElement(true);
  }

  private void endArrayElement(boolean requireHole) {
    if (inJsString) {
      pendingExpression = combine(
          pendingExpression, endPartialJsStringLiteral());
    }
    if (pendingExpression != null) {
      arrayElements.add(pendingExpression);
      pendingExpression = null;
    } else if (requireHole) {
      // If there are no calls to consume or
      arrayElements.add(
          new Elision(last != null ? last : FilePosition.UNKNOWN));
    }
  }

  /**
   * The members of the array of JS string expressions built by prior calls to
   * {@link #consume(String)}, {@link #consume(Expression)}, and
   * {@link #endArrayElement}.
   */
  public List<Expression> getArrayMembers() {
    if (inJsString || pendingExpression != null) {
      // Call noMoreTokens() before sampling the array members.
      throw new IllegalStateException();
    }
    return arrayElements;
  }

  @Override
  public void mark(FilePosition pos) {
    cssTokenConsumer.mark(pos);
    if (inJsString && positionAtStartOfStringLiteral == null) {
      positionAtStartOfStringLiteral = pos;
    }
    last = pos;
  }

  @Override
  public void consume(String text) {
    if (!inJsString) {
      startPartialJsStringLiteral();
    }
    cssTokenConsumer.consume(text);
  }

  /** May be overridden to do something before starting a string literal. */
  @OverridingMethodsMustInvokeSuper
  protected void startPartialJsStringLiteral() {
    inJsString = true;
  }

  @Override
  public void noMoreTokens() {
    cssTokenConsumer.noMoreTokens();
    endArrayElement(false);
  }

  /** Constructs a string expression. */
  @OverridingMethodsMustInvokeSuper
  protected Expression endPartialJsStringLiteral() {
    String s = partialJsStringLiteral.toString();
    partialJsStringLiteral.setLength(0);
    FilePosition pos = positionAtStartOfStringLiteral != null
        ? FilePosition.span(positionAtStartOfStringLiteral, last)
        : FilePosition.UNKNOWN;
    positionAtStartOfStringLiteral = null;
    inJsString = false;
    return StringLiteral.valueOf(pos, s);
  }

  /** (null, a) -> a, but (a, b) -> (a + b) */
  protected Expression combine(
      @Nullable Expression prefixExpression, Expression suffixExpression) {
    if (prefixExpression != null) {
      return Operation.createInfix(
          Operator.ADDITION, prefixExpression, suffixExpression);
    }
    return suffixExpression;
  }
}
