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

package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import com.google.javascript.jscomp.jsonml.JsonML;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The result of running the cajoler over some content.
 *
 * @author ihab.awad@gmail.com
 */
public final class CajoledModule extends AbstractParseTreeNode
    implements JsonMLCompatible {
  private static final long serialVersionUID = -2499144011243193616L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public CajoledModule(FilePosition pos,
                       Void value,
                       List<? extends ObjectConstructor> children) {
    this(pos, children.get(0));
    assert children.size() == 1;
  }

  /**
   * Creates a CajoledModule.
   *
   * @param pos a file position.
   * @param body an object constructor representing the module.
   */
  public CajoledModule(FilePosition pos, ObjectConstructor body) {
    super(pos, ObjectConstructor.class);
    createMutation().appendChild(body).execute();
  }

  /**
   * Creates a CajoledModule.
   *
   * @param body an object constructor representing the module.
   */
  public CajoledModule(ObjectConstructor body) {
    this(body.getFilePosition(), body);
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (children().size() != 1 && getModuleBody() != null) {
      throw new IllegalStateException(
          "A CajoledModule may only have one child");
    }
  }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<? extends ObjectConstructor> children() {
    return childrenAs(ObjectConstructor.class);
  }

  public ObjectConstructor getModuleBody() {
    return children().get(0);
  }

  /** The URI from which the module was loaded. */
  public String getSrc() {
    ValueProperty p = (ValueProperty) getModuleBody().propertyWithName("src");
    if (p == null) { return null; }
    return ((StringLiteral) p.getValueExpr()).getUnquotedValue();
  }

  /** The URIs of modules needed by this module. */
  public ArrayConstructor getIncludedModules() {
    ValueProperty p = (ValueProperty) getModuleBody().propertyWithName(
        "includedModules");
    return p != null ? (ArrayConstructor) p.getValueExpr() : null;
  }

  /** The URIs of modules inlined by this module. */
  public ArrayConstructor getInlinedModules() {
    ValueProperty p = (ValueProperty) getModuleBody().propertyWithName(
        "inlinedModules");
    return p != null ? (ArrayConstructor) p.getValueExpr() : null;
  }

  public FunctionConstructor getInstantiateMethod() {
    return (FunctionConstructor) ((ValueProperty)
        getModuleBody().propertyWithName("instantiate")).getValueExpr();
  }

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new JsPrettyPrinter(new Concatenator(out, exHandler));
  }

  /**
   * Render the cajoled module in default form. The result is text representing
   * a block containing a statement that calls the new module handler as defined
   * using {@code es53.js}. The result looks like:
   *
   * <p><pre>
   * {
   *   ___.loadModule({
   *     <em>contents of module object literal</em>
   *   });
   * }
   * </pre>
   *
   * @param rc a {@code RenderContext}.
   */
  public void render(RenderContext rc) {
    renderModuleExpression(
        (Expression) QuasiBuilder.substV(
            "___.loadModule(@body)",
            "body", getModuleBody()),
        rc);
  }

  public JsonML toJsonML() {
    Expression loadModuleCall = (Expression) QuasiBuilder.substV(
        "___.loadModule(@body)",
        "body", getModuleBody());
    Block program = new Block(
        loadModuleCall.getFilePosition(),
        Collections.singletonList(new ExpressionStmt(loadModuleCall)));
    return program.toJsonMLAsProgram();
  }

  /**
   * Render the cajoled module with a custom callback expression. This is used
   * when the receiving JavaScript runtime wishes to use a different scheme for
   * handling new modules (for example, to implement a cajoling Web service
   * using a JSONP-style protocol for returning content to the client). The
   * result looks like:
   *
   * <p><pre>
   * {
   *   <em>callbackExpression</em>(___.prepareModule({
   *     <em>contents of module object literal</em>
   *   }));
   * }
   * </pre>
   *
   * @param callbackExpression an {@code Expression} that will be called, in the
   *     rendered output, as a single-argument function passing the module
   *     object. If this argument is {@code null}, the behavior of this method
   *     is the same as {@link #render(RenderContext)}.
   * @param rc a {@code RenderContext}.
   */
  public void render(Expression callbackExpression,
                     RenderContext rc) {
    if (callbackExpression == null) {
      render(rc);
    } else {
      renderModuleExpression(
          (Expression) QuasiBuilder.substV(
              "@callbackExpression(___.prepareModule(@body))",
              "callbackExpression", callbackExpression,
              "body", getModuleBody()),
          rc);
    }
  }

  private static void renderModuleExpression(
      Expression expr, RenderContext rc) {
    // Note that we deliberately add an enclosing block. See:
    // http://code.google.com/p/google-caja/issues/detail?id=1000
    Block block = new Block(
        FilePosition.UNKNOWN,
        Arrays.asList(new ExpressionStmt(FilePosition.UNKNOWN, expr)));
    block.render(rc);
  }

  /**
   * Returns a flattened copy of this CajoledModule.  The flattened copy
   * is cheaper to clone and/or cache.
   * <p>
   * Moderately-large JS (such as jquery.js) becomes a large ParseTreeNode
   * structure that takes a nontrivial amount of time to clone or
   * deserialize.  However, once we've finished cajoling a module,
   * the tree structure becomes irrelevant, so we can render the
   * module body into a single string for efficiency.
   * <p>
   * This requires making an early decision on what renderer to use.
   */
  public CajoledModule flatten(boolean minify) {
    ObjectConstructor oc = new ObjectConstructor(FilePosition.UNKNOWN);
    for (ObjProperty p : getModuleBody().children()) {
      oc.appendChild(flattenProperty(p, minify));
    }
    return new CajoledModule(oc);
  }

  private static ObjProperty flattenProperty(ObjProperty op, boolean minify) {
    if (op instanceof ValueProperty) {
      ValueProperty vp = (ValueProperty) op;
      String name = vp.getPropertyName();
      if ("instantiate".equals(name)) {
        return new ValueProperty(
            vp.getPropertyNameNode(),
            new RenderedExpression(vp.getValueExpr(), minify));
      }
    }
    return op;
  }

}
