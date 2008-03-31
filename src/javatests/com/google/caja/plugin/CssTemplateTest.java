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
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Criterion;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class CssTemplateTest extends TestCase {
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

  private CssTree.DeclarationGroup parseTemplate(String css) throws Exception {
    InputSource is = new InputSource(new URI("test://" + getClass().getName()));
    CharProducer cp = CharProducer.Factory.create(new StringReader(css), is);
    try {
      CssLexer lexer = new CssLexer(cp, true);
      TokenQueue<CssTokenType> tq = new TokenQueue<CssTokenType>(
          lexer, cp.getCurrentPosition().source(),
          new Criterion<Token<CssTokenType>>() {
            public boolean accept(Token<CssTokenType> t) {
              return CssTokenType.SPACE != t.type
                  && CssTokenType.COMMENT != t.type;
            }
          });
      CssParser p = new CssParser(tq);
      CssTree.DeclarationGroup t = p.parseDeclarationGroup();
      tq.expectEmpty();
      return t;
    } finally {
      cp.close();
    }
  }

  private void runTest(String css, String golden) throws Exception {
    InputSource is = new InputSource(URI.create("test:///" + getName()));
    FilePosition pos = FilePosition.startOfFile(is);
    Identifier name = new Identifier(getName());
    name.setFilePosition(pos);
    CssTemplate tmpl = new CssTemplate(
        pos, name, Collections.<Identifier>emptyList(), parseTemplate(css));

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

    MessageQueue mq = new SimpleMessageQueue();
    MessageContext mc = new MessageContext();
    if (cssSchema == null) {
      cssSchema = CssSchema.getDefaultCss21Schema(mq);
      htmlSchema = HtmlSchema.getDefault(mq);
    }

    CssValidator cssv = new CssValidator(cssSchema, htmlSchema, mq);
    CssRewriter cssr = new CssRewriter(meta, mq);
    if (!(cssv.validateCss(
            new AncestorChain<CssTree.DeclarationGroup>(tmpl.getCss()))
          && cssr.rewrite(
              new AncestorChain<CssTree.DeclarationGroup>(tmpl.getCss())))) {
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
