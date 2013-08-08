// Copyright (C) 2011 Google Inc.
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

package com.google.caja.parser;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import com.google.caja.lexer.InputSource;
import com.google.caja.parser.css.CssParserTest;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.Dom;
import com.google.caja.parser.html.DomParserTest;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.ParserTest;
import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.ContentType;
import com.google.caja.util.Maps;
import com.google.caja.util.TestUtil;

/**
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
@SuppressWarnings("static-method")
public class ParserContextTest extends CajaTestCase {

  public final void testNotEnoughInput() {
    try {
      @SuppressWarnings("unused")
      ParseTreeNode node = new ParserContext(DevNullMessageQueue.singleton())
        .build();
      fail();
    } catch (Throwable e) {
      assertTrue(e instanceof IllegalStateException);
    }
  }

  public final void testContentCache() throws Exception {
    InputSource is = new InputSource(URI.create("test:///foo.js"));
    Map<InputSource, CharSequence> sourceMap = Maps.newHashMap();
    @SuppressWarnings("unused")
    ParseTreeNode node = new ParserContext(DevNullMessageQueue.singleton())
      .withSourceMap(sourceMap)
      .withInput(is)
      .withInput("var j = 1;")
      .build();
    assertTrue(sourceMap.containsKey(is));
    assertEquals("var j = 1;", sourceMap.get(is));
  }

  public final void testMinimalGuessHtmlContentType() throws Exception {
    ParseTreeNode node = new ParserContext(DevNullMessageQueue.singleton())
      .withInput("<b>hi mom</b>")
      .build();
    assertEquals(node.getFilePosition().source(), InputSource.UNKNOWN);
    assertTrue(node instanceof Dom);
  }

  public final void testMinimalGuessJsContentType() throws Exception {
    ParseTreeNode node = new ParserContext(DevNullMessageQueue.singleton())
      .withInput("var x = 1;")
      .build();
    assertEquals(node.getFilePosition().source(), InputSource.UNKNOWN);
    assertTrue(node instanceof Block);
  }

  public final void testMinimalGuessCssContentType() throws Exception {
    ParseTreeNode node = new ParserContext(DevNullMessageQueue.singleton())
      .withInput("div { color: red; }")
      .build();
    assertEquals(node.getFilePosition().source(), InputSource.UNKNOWN);
    assertTrue(node instanceof CssTree.StyleSheet);
  }

  public final void testGuessCharProducer() throws Exception {
    InputStream css = TestUtil.getResource(CssParserTest.class,
        "cssparserinput1.css").toURL().openStream();
    InputStream html = TestUtil.getResource(DomParserTest.class,
        "amazon.com.html").toURL().openStream();
    InputStream js  = TestUtil.getResource(ParserTest.class,
        "parsertest1.js").toURL().openStream();

    ParserContext ctx = new ParserContext(DevNullMessageQueue.singleton());

    ParseTreeNode cssNode = ctx.withInput(css).withInput(ContentType.CSS).build();
    assertTrue(cssNode instanceof CssTree.StyleSheet);

    ParseTreeNode htmlNode = ctx.withInput(html).withInput(ContentType.HTML).build();
    assertTrue(htmlNode instanceof Dom);

    ParseTreeNode jsNode = ctx.withInput(js).withInput(ContentType.JS).build();
    assertTrue(jsNode instanceof Block);
  }
}
