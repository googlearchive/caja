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

package com.google.caja.parser.quasiliteral;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Callback;
import com.google.caja.util.Maps;
import com.google.caja.util.TestUtil;

import java.io.IOException;
import java.net.URI;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


/**
 * This test ensures that the module format, including debugging information,
 * is correct. Some of this material essentially tests the rendering in class
 * CajoledModule and its dependencies, but the tests are inextricably tied to
 * the cajoling process and therefore arguably belongs in this package.
 *
 * @author ihab.awad@gmail.com
 */
public class ModuleFormatTest extends CajaTestCase {
  private final Rewriter makeRewriter() {
    return new CajitaRewriter(TestBuildInfo.getInstance(), mq, false);
  }

  private final Callback<IOException> exHandler = new Callback<IOException>() {
    public void handle(IOException e) {
      throw new SomethingWidgyHappenedError(e);
    }
  };

  public final void testCajoledModuleContents() {
    CajoledModule trivialCajoledModule = (CajoledModule) makeRewriter().expand(
        new UncajoledModule(new Block()));
    assertNoErrors();

    Map<String, ParseTreeNode> bindings = Maps.newHashMap();

    assertTrue(QuasiBuilder.match(
        "  ({"
        + "  instantiate: @instantiate,"
        + "  cajolerName: @cajolerName,"
        + "  cajolerVersion: @cajolerVersion,"
        + "  cajoledDate: @cajoledDate"
        + "})",
        trivialCajoledModule.getModuleBody(),
        bindings));

    assertTrue(bindings.get("instantiate") instanceof FunctionConstructor);

    assertTrue(bindings.get("cajolerName") instanceof StringLiteral);
    assertEquals(
        "com.google.caja",
        bindings.get("cajolerName").getValue());

    assertTrue(bindings.get("cajolerVersion") instanceof StringLiteral);
    assertEquals(
        new TestBuildInfo().getBuildVersion(),
        bindings.get("cajolerVersion").getValue());

    assertTrue(bindings.get("cajoledDate") instanceof IntegerLiteral);
    assertEquals(
        new Long(new TestBuildInfo().getCurrentTime()),
        bindings.get("cajoledDate").getValue());
  }

  public final void testCajoledModuleDebugRendering() throws Exception {
    CajoledModule cajoledModule = (CajoledModule) makeRewriter().expand(
        new UncajoledModule(js(fromResource("testModule.js"))));
    assertNoErrors();

    Map<InputSource, CharSequence> originalSource = Collections.singletonMap(
        new InputSource(
            new URI(getClass().getResource("testModule.js").toExternalForm())),
        (CharSequence) TestUtil.readResource(getClass(), "testModule.js"));

    StringBuilder sb = new StringBuilder();
    RenderContext rc = new RenderContext(new Concatenator(sb));
    cajoledModule.renderWithDebugSymbols(originalSource, rc);
    rc.getOut().noMoreTokens();

    assertEquals(
        TestUtil.readResource(getClass(), "testModule.out.js"),
        sb.toString());
  }

  private CajoledModule makeTestCajoledModule() {
    ObjectConstructor oc = (ObjectConstructor) QuasiBuilder.substV(
        "  ({"
        + "  instantiate: function() {},"
        + "  foo: 42"
        + "})");
    return new CajoledModule(
        FilePosition.UNKNOWN,
        null,
        Arrays.asList(oc));
  }

  private String render(CajoledModule module,
                        Expression callbackExpression) {
    StringBuilder out = new StringBuilder();
    TokenConsumer tc = new JsPrettyPrinter(new Concatenator(out, exHandler));
    module.render(callbackExpression, new RenderContext(tc));
    tc.noMoreTokens();
    return out.toString();
  }

  private String renderWithDebugSymbols(CajoledModule module,
                                        Expression callbackExpression) {
    StringBuilder out = new StringBuilder();
    TokenConsumer tc = new Concatenator(out, exHandler);
    module.renderWithDebugSymbols(
        callbackExpression, Maps.<InputSource, CharSequence>newHashMap(),
        new RenderContext(tc));
    tc.noMoreTokens();
    return out.toString();
  }

  public final void testCajoledModuleRenderingWithCallback() throws Exception {
    // Ensure that the rendered form of a cajoled module with a callback
    // expression fits the expected format.

    // Create a cajoled module and render it with a callback.
    String renderedModule = render(
        makeTestCajoledModule(),
        jsExpr(fromString("foo.bar.baz")));

    // Re-parse the rendered output so we can apply quasi matches to it.
    Expression reparsedModule = (Expression)
        js(fromString(renderedModule))
        // Extract the innermost Expression since the quasi will match that.
        .children().get(0).children().get(0).children().get(0);

    // Check that the reparsed structure matches what we expect.
    assertEquals(
        render(jsExpr(fromString(
            ""
            + "foo.bar.baz(___.prepareModule({"
            + "  instantiate: function() {},"
            + "  foo: 42"
            + "}))"))),
        render(reparsedModule));
  }

  public final void testCajoledModuleDebugRenderingWithCallback()
      throws Exception {
    // Ensure that the rendered form of a cajoled module with a callback
    // expression and debugging information fits the expected format.

    // Create a cajoled module and render it with a callback and
    // debugging information.
    String renderedModule = renderWithDebugSymbols(
        makeTestCajoledModule(),
        jsExpr(fromString("foo.bar.baz")));

    // Re-parse the rendered output so we can apply quasi matches to it.
    Expression reparsedModule = (Expression)
        js(fromString(renderedModule))
        // Extract the innermost Expression since the quasi will match that.
        .children().get(0).children().get(0).children().get(0);

    // Check that the reparsed structure matches what we expect.
   Map<String, ParseTreeNode> bindings = Maps.newHashMap();
    assertTrue(
        render(reparsedModule),
        QuasiBuilder.match(
            ""
            + "foo.bar.baz(___.prepareModule({"
            + "  instantiate: function() {},"
            + "  foo: 42,"
            + "  sourceLocationMap: @sourceLocationMap,"
            + "  originalSource: @originalSource"
            + "}));",
            reparsedModule,
            bindings));
    // Other tests verify the exact details of "sourceLocationMap" and
    // "originalSource". In this test, we are checking for the correct callback
    // expression "foo.bar.baz", so we apply only a very weak sanity check on
    // the remainder.
    assertTrue(bindings.get("sourceLocationMap") instanceof ObjectConstructor);
    assertTrue(bindings.get("originalSource") instanceof ObjectConstructor);
  }
}
