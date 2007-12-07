// Copyright (C) 2005 Google Inc.
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

package com.google.caja;

import com.google.caja.lexer.CharProducerTest;
import com.google.caja.lexer.CommentLexerTest;
import com.google.caja.lexer.CssLexerTest;
import com.google.caja.lexer.HtmlLexerTest;
import com.google.caja.lexer.JsLexerTest;
import com.google.caja.lexer.LookaheadCharProducerTest;
import com.google.caja.lexer.PunctuationTrieTest;
import com.google.caja.opensocial.DefaultGadgetRewriterTest;
import com.google.caja.opensocial.GadgetParserTest;
import com.google.caja.parser.ParseTreeNodeTest;
import com.google.caja.parser.css.CssParserTest;
import com.google.caja.parser.css.CssTreeTest;
import com.google.caja.parser.css.Css2Test;
import com.google.caja.parser.html.DomParserTest;
import com.google.caja.parser.html.JsHtmlParserTest;
import com.google.caja.parser.js.ParserTest;
import com.google.caja.parser.js.StringLiteralTest;
import com.google.caja.plugin.CompiledPluginTest;
import com.google.caja.plugin.CssRewriterTest;
import com.google.caja.plugin.CssValidatorTest;
import com.google.caja.plugin.ExpressionSanitizerTest;
import com.google.caja.plugin.GxpCompilerTest;
import com.google.caja.plugin.GxpValidatorTest;
import com.google.caja.plugin.HtmlCompiledPluginTest;
import com.google.caja.plugin.HtmlWhitelistTest;
import com.google.caja.plugin.PluginCompilerTest;
import com.google.caja.plugin.UrlUtilTest;
import com.google.caja.plugin.caps.CapabilityRewriterTest;

import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author mikesamuel@gmail.com
 */
public class AllTests {

  @SuppressWarnings("unchecked")
  public static Test suite() {
    TestSuite suite = new TestSuite("Caja Tests");
    Class<? extends TestCase>[] testClasses = new Class[] {
          CapabilityRewriterTest.class,
          CharProducerTest.class,
          CommentLexerTest.class,
          CompiledPluginTest.class,
          Css2Test.class,
          CssLexerTest.class,
          CssParserTest.class,
          CssRewriterTest.class,
          CssTreeTest.class,
          CssValidatorTest.class,
          GadgetParserTest.class,
          DefaultGadgetRewriterTest.class,
          DomParserTest.class,
          ExpressionSanitizerTest.class,
          GxpCompilerTest.class,
          GxpValidatorTest.class,
          HtmlCompiledPluginTest.class,
          HtmlLexerTest.class,
          HtmlWhitelistTest.class,
          JsHtmlParserTest.class,
          JsLexerTest.class,
          LookaheadCharProducerTest.class,
          ParseTreeNodeTest.class,
          ParserTest.class,
          PluginCompilerTest.class,
          PunctuationTrieTest.class,
          StringLiteralTest.class,
          UrlUtilTest.class,
        };
    Pattern testFilter = Pattern.compile(System.getProperty("test.filter", ""));
    for (Class<? extends TestCase> testClass : testClasses) {
      if (testFilter.matcher(testClass.getName()).find()) {
        suite.addTestSuite(testClass);
      }
    }
    return suite;
  }
}
