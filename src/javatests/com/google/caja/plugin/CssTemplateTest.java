// Copyright (C) 2006 Google Inc.
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
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.css.CssTree;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.TestUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class CssTemplateTest extends CajaTestCase {
  private CssSchema cssSchema;
  private HtmlSchema htmlSchema;

  public void testEmptyDeclaration() throws Exception {
    runTest(";",
            "function testEmptyDeclaration() {\n"
            + "  return ___OUTERS___.blessCss___();\n"
            + "}");
  }

  public void testOneDecl() throws Exception {
    runTest("background-color: blue",
            "function testOneDecl() {\n"
            + "  return ___OUTERS___.blessCss___("
            + "'background-color;backgroundColor', 'blue');\n"
            + "}");
  }

  public void testAmbiguousDecl() throws Exception {
    runTest("float: left",
            "function testAmbiguousDecl() {\n"
            + "  return ___OUTERS___.blessCss___('float;cssFloat', 'left');\n"
            + "}");
  }

  public void testDynamicLengthStyle() throws Exception {
    runTest("left: ${x}px",
            "function testDynamicLengthStyle() {\n"
            + "  return ___OUTERS___.blessCss___("
            + "'left', ___OUTERS___.cssNumber___(x) + 'px');\n"
            + "}");
  }

  public void testColorRewriting() throws Exception {
    runTest("color: ${x}",
            "function testColorRewriting() {\n"
            + "  return ___OUTERS___.blessCss___("
            + "'color', ___OUTERS___.cssColor___(x));\n"
            + "}");
  }

  public void testUriRewriting() throws Exception {
    runTest("list-style-image: ${x}; background: 'foo.png';",
            "function testUriRewriting() {\n"
            + "  return ___OUTERS___.blessCss___("
            + "'list-style-image;listStyleImage',"
            + " ___OUTERS___.cssUri___(x),"
            + " 'background',"
            + " '\\'http://proxy/?url=foo.png"
            + "&base=http%3A%2F%2Fgadget.com%2Ffoo&mt=image%2F*\\'');\n"
            + "}");
  }

  public void testNumericValues() throws Exception {
    runTest("line-height: ${n}; width: ${p}%",
            "function testNumericValues() {\n"
            + "  return ___OUTERS___.blessCss___("
            + "'line-height;lineHeight', ___OUTERS___.cssNumber___(n),"
            + " 'width', ___OUTERS___.cssNumber___(p) + '%');\n"
            + "}");
  }

  private void runTest(String css, String golden) throws Exception {
    FilePosition pos = FilePosition.startOfFile(is);
    Identifier name = new Identifier(getName());
    name.setFilePosition(pos);
    CssTemplate tmpl = new CssTemplate(
        pos, name, Collections.<Identifier>emptyList(),
        cssDecls(fromString(css), true));

    PluginMeta meta = new PluginMeta(":", new PluginEnvironment() {
        public CharProducer loadExternalResource(
            ExternalReference ref, String mimeType) {
          return null;
        }

        public String rewriteUri(ExternalReference ref, String mimeType) {
          try {
            return "http://proxy/?url="
                + URLEncoder.encode(ref.getUri().toString(), "UTF-8")
                + "&base=" + URLEncoder.encode("http://gadget.com/foo", "UTF-8")
                + "&mt=" + URLEncoder.encode(mimeType, "UTF-8");
          } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported");
          }
        }
      });

    if (cssSchema == null) {
      cssSchema = CssSchema.getDefaultCss21Schema(mq);
      htmlSchema = HtmlSchema.getDefault(mq);
    }

    AncestorChain<CssTree.DeclarationGroup> declAc
        = new AncestorChain<CssTree.DeclarationGroup>(tmpl.getCss());
    new CssValidator(cssSchema, htmlSchema, mq).validateCss(declAc);
    new CssRewriter(meta, mq).rewrite(declAc);
    if (TestUtil.hasErrorsOrWarnings(mq)) {
      for (com.google.caja.reporting.Message msg : mq.getMessages()) {
        System.err.println(msg.format(mc));
      }
      fail();
    }

    FunctionConstructor fc = tmpl.toFunction(cssSchema, mq);
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = fc.makeRenderer(sb, null);
    fc.render(new RenderContext(mc, tc));
    assertEquals(css, golden.trim(), sb.toString().trim());
    assertTrue(css, mq.getMessages().isEmpty());
  }
}
