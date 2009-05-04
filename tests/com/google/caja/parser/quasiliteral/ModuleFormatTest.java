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

import com.google.caja.lexer.InputSource;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.reporting.TestBuildInfo;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Callback;
import com.google.caja.util.TestUtil;

import java.io.IOException;
import java.net.URI;

import java.util.Collections;
import java.util.HashMap;
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
  private final Rewriter rewriter =
      new CajitaRewriter(new TestBuildInfo(), false);

  private final Callback<IOException> exHandler = new Callback<IOException>() {
    public void handle(IOException e) { throw new RuntimeException(e); }
  };

  public void testCajoledModuleContents() throws Exception {
    CajoledModule trivialCajoledModule = (CajoledModule)
        rewriter.expand(new UncajoledModule(new Block()), mq);
    assertNoErrors();

    Map<String, ParseTreeNode> bindings = new HashMap<String, ParseTreeNode>();

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

  public void testCajoledModuleDebugRendering() throws Exception {
    CajoledModule cajoledModule = (CajoledModule)
        rewriter.expand(
            new UncajoledModule(js(fromResource("testModule.js"))),
            mq);
    assertNoErrors();

    Map<InputSource, CharSequence> originalSource = Collections.singletonMap(
        new InputSource(
            new URI(getClass().getResource("testModule.js").toExternalForm())),
        (CharSequence) TestUtil.readResource(getClass(), "testModule.js"));

    StringBuilder sb = new StringBuilder();
    cajoledModule.renderWithDebugSymbols(originalSource, sb, exHandler);

    assertEquals(
        TestUtil.readResource(getClass(), "testModule.co.js"),
        sb.toString());
  }
}
