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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.render.SourceSpansRenderer;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;
import com.google.caja.util.Lists;
import com.google.javascript.jscomp.jsonml.JsonML;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The result of running the cajoler over some content.
 *
 * <p>TODO(ihab.awad): This class relies on
 * {@link com.google.caja.parser.quasiliteral.QuasiBuilder}
 * which makes our dependencies messy. Refactor {@code QuasiBuilder} so that
 * parser and parse tree node components can rely on it.
 *
 * @author ihab.awad@gmail.com
 */
public final class CajoledModule extends AbstractParseTreeNode
    implements JsonMLCompatible {
  // A stub file name by which to call the (otherwise anonymous) cajoled output.
  private static final InputSource CAJOLED_OUTPUT_FILE_NAME =
      new InputSource(URI.create("file:///CAJOLED-OUTPUT"));

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

  public final TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> exHandler) {
    return new JsPrettyPrinter(new Concatenator(out, exHandler));
  }

  /**
   * Render the cajoled module in default form. The result is text representing
   * a block containing a statement that calls the new module handler as defined
   * using {@code cajita.js}. The result looks like:
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

  private void renderModuleExpression(Expression expr, RenderContext rc) {
    // Note that we deliberately add an enclosing block. See:
    // http://code.google.com/p/google-caja/issues/detail?id=1000
    Block block = new Block(
        FilePosition.UNKNOWN,
        Arrays.asList(new ExpressionStmt(FilePosition.UNKNOWN, expr)));
    block.render(rc);
  }

  /**
   * Render this {@code CajoledModule} with debugging information.
   *
   * <p>The Caja compiler keeps track of the originating {@code InputSource} of
   * each piece of compiled material. This method can therefore use the
   * {@code originalSources} argument to embed, in the rendered output, snippets
   * of the original source linked to the compiled code. This in turn allows a
   * compatible debugger to provide debugging based on the original source.
   *
   * <p>See description of the <a
   * href="http://google-caja.googlecode.com/svn/trunk/doc/html/compiledModuleFormat/index.html">compiled
   * module format</a>.
   *
   * @param originalSources a map from {@code InputSource}s to the literal text
   *     of the code found in each {@code InputSource}. This map is expected to
   *     contain an entry for each of the original {@code InputSource}s from
   *     which this {@code CajoledModule} was compiled.
   * @param rc contains a {@link Concatenator} to which rendered text
   *      will be written.
   */
  public void renderWithDebugSymbols(
      Map<InputSource, CharSequence> originalSources, RenderContext rc) {
    TokenConsumer out = rc.getOut();
    // Note that we deliberately add an enclosing block. See:
    // http://code.google.com/p/google-caja/issues/detail?id=1000
    out.consume("{ ___.loadModule({\n");

    renderModuleBodyWithDebugSymbols(originalSources, rc);

    out.consume("}); }\n");
  }

  /**
   * Render with debugging symbols, specifying a callback expression.
   *
   * @param callbackExpression see
   *     {@link #render(Expression, RenderContext)}.
   * @param originalSources see
   *     {@link #renderWithDebugSymbols(Map, RenderContext)}.
   * @param rc see
   *     {@link #renderWithDebugSymbols(Map, RenderContext)}.
   */
  public void renderWithDebugSymbols(
      Expression callbackExpression,
      Map<InputSource, CharSequence> originalSources,
      RenderContext rc) {
    TokenConsumer out = rc.getOut();

    // Note that we deliberately add an enclosing block. See:
    // http://code.google.com/p/google-caja/issues/detail?id=1000
    out.consume("{");
    renderNode(callbackExpression, rc);
    out.consume("(___.prepareModule({\n");

    renderModuleBodyWithDebugSymbols(originalSources, rc);

    out.consume("}));}\n");
  }

  private void renderModuleBodyWithDebugSymbols(
      Map<InputSource, CharSequence> originalSources, RenderContext rc) {
    // Render the module function. With this, the SourceSpansRenderer captures
    // the rendered form of the function, and also builds the debug information.
    SourceSpansRenderer ssr = new SourceSpansRenderer(
        CAJOLED_OUTPUT_FILE_NAME, rc);
    RenderContext ssrrc = new RenderContext(ssr)
        .withAsciiOnly(rc.isAsciiOnly())
        .withEmbeddable(rc.isEmbeddable());

    ((ValueProperty) getModuleBody().propertyWithName("instantiate"))
        .getValueExpr().render(ssrrc);
    ssr.noMoreTokens();

    // Build the abbreviated original file names and their contents.
    List<String> abbreviatedOriginalFileNames = Lists.newArrayList();
    List<List<String>> originalFileContents = Lists.newArrayList();

    for (InputSource is : ssr.getMessageContext().getInputSources()) {
      String sourceString = charSequenceToString(originalSources.get(is));
      List<String> lines = Arrays.asList(sourceString.split("\r\n?|\n"));
      abbreviatedOriginalFileNames.add(ssr.getMessageContext().abbreviate(is));
      originalFileContents.add(lines);
    }

    // Now render the actual text to 'out'.
    renderText(
        getModuleBody(),
        ssr.getProgramText(),
        ssr.getSourceLocationMap(),
        abbreviatedOriginalFileNames,
        originalFileContents,
        rc);
  }

  /**
   * Renders the text of the module literal. This is the only place where we
   * break the rendering abstraction by printing plain text directly to the
   * output stream.
   */
  private static void renderText(ObjectConstructor moduleBody,
                                 String instantiateFunctionText,
                                 List<String> sourceLocationMap,
                                 List<String> abbreviatedOriginalFileNames,
                                 List<List<String>> originalFileContents,
                                 RenderContext rc) {
    TokenConsumer out = rc.getOut();
    // Render the cajoled code
    renderNode(stringToStringLiteral("instantiate"), rc);
    out.consume(":\n");
    out.consume(instantiateFunctionText);
    out.consume(",\n");

    List<? extends ObjProperty> moduleBodyProps = moduleBody.children();
    for (ObjProperty moduleBodyProp : moduleBodyProps) {
      if ("instantiate".equals(moduleBodyProp.getPropertyName())) { continue; }

      // Render remaining key/value pairs in the module body
      renderNode(moduleBodyProp.getPropertyNameNode(), rc);
      out.consume(": ");
      Expression valueExpr = ((ValueProperty) moduleBodyProp).getValueExpr();
      if (Operation.is(valueExpr, Operator.COMMA)) {
        out.consume("(");
        renderNode(valueExpr, rc);
        out.consume(")");
      } else {
        renderNode(valueExpr, rc);
      }
      out.consume(",\n");
    }

    // Render source location map
    renderNode(stringToStringLiteral("sourceLocationMap"), rc);
    out.consume(": ");
    renderNode(stringListToContentNode(sourceLocationMap), rc);
    out.consume(",\n");

    // Render original source
    renderNode(stringToStringLiteral("originalSource"), rc);
    out.consume(": ");
    renderNode(
        buildOriginalSourceNode(
            abbreviatedOriginalFileNames, originalFileContents),
        rc);
    out.consume("\n");
  }

  private static ParseTreeNode buildOriginalSourceNode(
      List<String> abbreviatedOriginalFileNames,
      List<List<String>> originalFileContents) {
    return QuasiBuilder.substV(
        "({ @keys*: @values* })",
        "keys", stringListToStringLiterals(abbreviatedOriginalFileNames),
        "values", stringListListToMultipleContentNodes(originalFileContents));
  }

  private static ParseTreeNode stringListListToMultipleContentNodes(
      List<List<String>> contents) {
    List<ParseTreeNode> multipleContents = Lists.newArrayList(contents.size());

    for (List<String> c : contents) {
      multipleContents.add(stringListToContentNode(c));
    }

    return new ParseTreeNodeContainer(multipleContents);
  }

  private static ParseTreeNode stringListToContentNode(List<String> lines) {
    return QuasiBuilder.substV(
        "  ({"
        + "  type: 'content',"
        + "  content: [ @stringLiterals* ]"
        + "})",
        "stringLiterals", stringListToStringLiterals(lines));
  }

  private static ParseTreeNode stringListToStringLiterals(
      List<String> strings) {
    List<ParseTreeNode> stringLiterals = Lists.newArrayList(strings.size());

    for (String s : strings) {
      stringLiterals.add(stringToStringLiteral(s));
    }

    return new ParseTreeNodeContainer(stringLiterals);
  }

  private static ParseTreeNode stringToStringLiteral(String s) {
    return StringLiteral.valueOf(FilePosition.UNKNOWN, s);
  }

  private static String charSequenceToString(CharSequence cs) {
    return cs instanceof String ?
        (String) cs :
        new StringBuilder().append(cs).toString();
  }

  private static void renderNode(ParseTreeNode node, RenderContext rc) {
    TokenConsumer tc = new JsPrettyPrinter((Concatenator) rc.getOut());
    node.render(new RenderContext(tc).withAsciiOnly(rc.isAsciiOnly())
                .withEmbeddable(rc.isEmbeddable()));
    tc.noMoreTokens();
  }
}
