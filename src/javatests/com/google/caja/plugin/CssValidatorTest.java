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
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Criterion;
import com.google.caja.util.SyntheticAttributeKey;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;

import java.util.Arrays;
import java.util.LinkedHashSet;

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class CssValidatorTest extends TestCase {
  private MessageQueue mq;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    if (null == mq) {
      mq = new EchoingMessageQueue(
          new PrintWriter(new OutputStreamWriter(System.err)),
          new MessageContext(), false);
    }
  }

  public void testValidateColor() throws Exception {
    runTest("a { color: blue }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : a\n"
            + "    Declaration\n"
            + "      Property : color\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=color::color\n"
            + "          IdentLiteral : blue");
    runTest("a { color: #00f }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : a\n"
            + "    Declaration\n"
            + "      Property : color\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=COLOR"
                        + " ; cssPropertyPart=color::color\n"
            + "          HashLiteral : #00f");
    runTest("a { color: #0000ff }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : a\n"
            + "    Declaration\n"
            + "      Property : color\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=COLOR"
                        + " ; cssPropertyPart=color::color\n"
            + "          HashLiteral : #0000ff");
    runTest("a { color: rgb(0, 0, 255) }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : a\n"
            + "    Declaration\n"
            + "      Property : color\n"
            + "      Expr\n"
            + "        Term\n"
            + "          FunctionCall : rgb\n"
            + "            Expr\n"
            + "              Term ; cssPropertyPartType=INTEGER"
                              + " ; cssPropertyPart=color::color::red\n"
            + "                QuantityLiteral : 0\n"
            + "              Operation : COMMA\n"
            + "              Term ; cssPropertyPartType=INTEGER"
                              + " ; cssPropertyPart=color::color::green\n"
            + "                QuantityLiteral : 0\n"
            + "              Operation : COMMA\n"
            + "              Term ; cssPropertyPartType=INTEGER"
                              + " ; cssPropertyPart=color::color::blue\n"
            + "                QuantityLiteral : 255");
    runTest("a { color: rgb(0%, 0%, 100%) }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : a\n"
            + "    Declaration\n"
            + "      Property : color\n"
            + "      Expr\n"
            + "        Term\n"
            + "          FunctionCall : rgb\n"
            + "            Expr\n"
            + "              Term ; cssPropertyPartType=PERCENTAGE"
                              + " ; cssPropertyPart=color::color::red\n"
            + "                QuantityLiteral : 0%\n"
            + "              Operation : COMMA\n"
            + "              Term ; cssPropertyPartType=PERCENTAGE"
                              + " ; cssPropertyPart=color::color::green\n"
            + "                QuantityLiteral : 0%\n"
            + "              Operation : COMMA\n"
            + "              Term ; cssPropertyPartType=PERCENTAGE"
                              + " ; cssPropertyPart=color::color::blue\n"
            + "                QuantityLiteral : 100%");
  }

  public void testValidateFont() throws Exception {
    // special names
    runTest("p, dl { font: caption; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font\n"
            + "          IdentLiteral : caption\n"
            + "    Declaration");
    fails("bogus, dl { font: caption; }");
    fails("p, bogus { font: caption; }");
    fails("p[bogus] { font: caption; }");
    fails("p { font: waption; }");

    runTest("p, dl { font: status-bar; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font\n"
            + "          IdentLiteral : status-bar\n"
            + "    Declaration");
    fails("p, dl { font: status-bar caption; }");

    // size and family
    runTest("p, dl { font: 12pt Arial; }",  // absolute
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=font-size\n"
            + "          QuantityLiteral : 12pt\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=FAMILY_NAME"
                        + " ; cssPropertyPart=font-family\n"
            + "          IdentLiteral : Arial\n"
            + "    Declaration");
    warns("p, dl { font: -12pt Arial; }");
    fails("p, dl { font: -12pt url(Arial); }");
    fails("p, dl { font: 12 Arial; }");
    fails("p, dl { font: twelve Arial; }");
    runTest("p, dl { font: 150% Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=PERCENTAGE"
                        + " ; cssPropertyPart=font-size\n"
            + "          QuantityLiteral : 150%\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=FAMILY_NAME"
                        + " ; cssPropertyPart=font-family\n"
            + "          IdentLiteral : Arial\n"
            + "    Declaration");
    fails("p, dl { font: 150 Arial; }");
    fails("p, dl { font: 150Arial; }");
    fails("p, dl { font: 150/Arial; }");
    runTest("p, dl { font: medium Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-size::absolute-size\n"
            + "          IdentLiteral : medium\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=FAMILY_NAME"
                        + " ; cssPropertyPart=font-family\n"
            + "          IdentLiteral : Arial\n"
            + "    Declaration");
    fails("p, dl { font: medium; }");

    // style weight size family
    runTest("p, dl { font: italic bolder 150% Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-style\n"
            + "          IdentLiteral : italic\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-weight\n"
            + "          IdentLiteral : bolder\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=PERCENTAGE"
                        + " ; cssPropertyPart=font-size\n"
            + "          QuantityLiteral : 150%\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=FAMILY_NAME"
                        + " ; cssPropertyPart=font-family\n"
            + "          IdentLiteral : Arial\n"
            + "    Declaration");
    fails("p, dl { font: italic bolderer 150% Arial; }");
    fails("p, dl { font: italix bolder 150% Arial; }");

    // font-size also matches by previous terms
    runTest("p, dl { font: inherit Arial; }",  // special
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-size\n"
            + "          IdentLiteral : inherit\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=FAMILY_NAME"
                        + " ; cssPropertyPart=font-family\n"
            + "          IdentLiteral : Arial\n"
            + "    Declaration");
    fails("p, dl { font: inherit; }");

    // weight size family
    runTest("p, dl { font: 800 150% Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-weight\n"
            + "          QuantityLiteral : 800\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=PERCENTAGE"
                        + " ; cssPropertyPart=font-size\n"
            + "          QuantityLiteral : 150%\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=FAMILY_NAME"
                        + " ; cssPropertyPart=font-family\n"
            + "          IdentLiteral : Arial\n"
            + "    Declaration");
    fails("p, dl { font: 800; }");
    fails("p, dl { font: 800 Arial; }");

    // variant weight family
    runTest("p, dl { font: normal 800 150% Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-style\n"
            + "          IdentLiteral : normal\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-weight\n"
            + "          QuantityLiteral : 800\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=PERCENTAGE"
                        + " ; cssPropertyPart=font-size\n"
            + "          QuantityLiteral : 150%\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=FAMILY_NAME"
                        + " ; cssPropertyPart=font-family\n"
            + "          IdentLiteral : Arial\n"
            + "    Declaration");
    fails("p, dl { font: normal 800 Arial; }");
    fails("p, dl { font: abnormal 150% Arial; }");

    // with line-height following /
    runTest("p, dl { font: normal 800 150%/175% Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-style\n"
            + "          IdentLiteral : normal\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-weight\n"
            + "          QuantityLiteral : 800\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=PERCENTAGE"
                        + " ; cssPropertyPart=font-size\n"
            + "          QuantityLiteral : 150%\n"
            + "        Operation : DIV\n"
            + "        Term ; cssPropertyPartType=PERCENTAGE"
                        + " ; cssPropertyPart=line-height\n"
            + "          QuantityLiteral : 175%\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=FAMILY_NAME"
                        + " ; cssPropertyPart=font-family\n"
            + "          IdentLiteral : Arial\n"
            + "    Declaration");
    fails("p, dl { font: abnormal 150%/175% Arial; }");
    fails("p, dl { font: normal 800 150%/ Arial; }");
    runTest("p, dl { font: normal 800 150%/17.5 Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-style\n"
            + "          IdentLiteral : normal\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-weight\n"
            + "          QuantityLiteral : 800\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=PERCENTAGE"
                        + " ; cssPropertyPart=font-size\n"
            + "          QuantityLiteral : 150%\n"
            + "        Operation : DIV\n"
            + "        Term ; cssPropertyPartType=NUMBER"
                        + " ; cssPropertyPart=line-height\n"
            + "          QuantityLiteral : 17.5\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=FAMILY_NAME"
                        + " ; cssPropertyPart=font-family\n"
            + "          IdentLiteral : Arial\n"
            + "    Declaration");
    warns("p, dl { font: normal 800 150%/-175% Arial; }");
    warns("p, dl { font: normal 800 150%/-17.5 Arial; }");

    // make sure the first three inherits match different parts
    runTest("p { font: inherit inherit inherit Arial; }",
            "StyleSheet\n" +
            "  RuleSet\n" +
            "    Selector\n" +
            "      SimpleSelector\n" +
            "        IdentLiteral : p\n" +
            "    Declaration\n" +
            "      Property : font\n" +
            "      Expr\n" +
            "        Term ; cssPropertyPartType=IDENT"
                      + " ; cssPropertyPart=font-style\n" +
            "          IdentLiteral : inherit\n" +
            "        Operation : NONE\n" +
            "        Term ; cssPropertyPartType=IDENT"
                      + " ; cssPropertyPart=font-variant\n" +
            "          IdentLiteral : inherit\n" +
            "        Operation : NONE\n" +
            "        Term ; cssPropertyPartType=IDENT"
                      + " ; cssPropertyPart=font-size\n" +
            "          IdentLiteral : inherit\n" +
            "        Operation : NONE\n" +
            "        Term ; cssPropertyPartType=FAMILY_NAME"
                      + " ; cssPropertyPart=font-family\n" +
            "          IdentLiteral : Arial\n" +
            "    Declaration");
  }

  public void testValidateBorder() throws Exception {
    runTest("p, dl { border: inherit; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    Declaration\n"
            + "      Property : border\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=border-top-color\n"
            + "          IdentLiteral : inherit\n"
            + "    Declaration");
    runTest("p { border: 2px }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : border\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=border::border-width\n"
            + "          QuantityLiteral : 2px");
    runTest("p { border: 2px solid black}",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : border\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=border::border-width\n"
            + "          QuantityLiteral : 2px\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=border::border-style\n"
            + "          IdentLiteral : solid\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=border-top-color::color\n"
            + "          IdentLiteral : black");
    runTest("p {border: solid black; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : border\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=border::border-style\n"
            + "          IdentLiteral : solid\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=border-top-color::color\n"
            + "          IdentLiteral : black\n"
            + "    Declaration");
    runTest("p { border:solid black 1em}",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : border\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=border::border-style\n"
            + "          IdentLiteral : solid\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=border-top-color::color\n"
            + "          IdentLiteral : black\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=border::border-width\n"
            + "          QuantityLiteral : 1em");
    runTest("p { border: 14px transparent }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : border\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=border::border-width\n"
            + "          QuantityLiteral : 14px\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=border-top-color\n"
            + "          IdentLiteral : transparent");
  }

  public void testClip() throws Exception {
    runTest("p { clip: rect(10px, 10px, 10px, auto) }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : clip\n"
            + "      Expr\n"
            + "        Term\n"
            + "          FunctionCall : rect\n"
            + "            Expr\n"
            + "              Term ; cssPropertyPartType=LENGTH"
                              + " ; cssPropertyPart=clip::shape::top\n"
            + "                QuantityLiteral : 10px\n"
            + "              Operation : COMMA\n"
            + "              Term ; cssPropertyPartType=LENGTH"
                              + " ; cssPropertyPart=clip::shape::right\n"
            + "                QuantityLiteral : 10px\n"
            + "              Operation : COMMA\n"
            + "              Term ; cssPropertyPartType=LENGTH"
                              + " ; cssPropertyPart=clip::shape::bottom\n"
            + "                QuantityLiteral : 10px\n"
            + "              Operation : COMMA\n"
            + "              Term ; cssPropertyPartType=IDENT"
                              + " ; cssPropertyPart=clip::shape::left\n"
            + "                IdentLiteral : auto");
  }

  public void testContent() throws Exception {
    // TODO
  }

  public void testBackground() throws Exception {
    runTest("p { background: url( /images/smiley-face.jpg ) no-repeat }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : background\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=URI"
                        + " ; cssPropertyPart=background-image\n"
            + "          UriLiteral : /images/smiley-face.jpg\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-repeat\n"
            + "          IdentLiteral : no-repeat");
    runTest("p { background: url( /images/smiley-face.jpg ) no-repeat }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : background\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=URI"
                        + " ; cssPropertyPart=background-image\n"
            + "          UriLiteral : /images/smiley-face.jpg\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-repeat\n"
            + "          IdentLiteral : no-repeat");
    runTest("p { background-image: '/images/smiley-face.jpg' }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : background-image\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=URI"
                        + " ; cssPropertyPart=background-image\n"
            + "          StringLiteral : /images/smiley-face.jpg");
  }

  public void testPositionSubstitution() throws Exception {
    runTest("p { left: $(3)px }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : left\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=left\n"
            + "          Substitution : $(3)px");
  }

  public void testColorSubstitution() throws Exception {
    runTest("p { background: $(shade << 16 | shade << 8 | shade) }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : background\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=COLOR"
                        + " ; cssPropertyPart=background-color::color\n"
            + "          Substitution : $(shade << 16 | shade << 8 | shade)");
  }

  public void testUriSubstitution() throws Exception {
    runTest("p { background: $(imageName + '.png')uri }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : background\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=URI"
                        + " ; cssPropertyPart=background-image\n"
            + "          Substitution : $(imageName + '.png')uri");
    runTest("p { background-image: $(imageName + '.png') }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Declaration\n"
            + "      Property : background-image\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=URI"
                        + " ; cssPropertyPart=background-image\n"
            + "          Substitution : $(imageName + '.png')");
  }

  private CssTree parseCss(String css) throws Exception {
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
      CssTree t = p.parseStyleSheet();
      tq.expectEmpty();
      return t;
    } finally {
      cp.close();
    }
  }

  private void fails(String css) throws Exception {
    CssTree t = parseCss(css);
    CssValidator v = makeCssValidator(mq);
    assertTrue(css, !v.validateCss(ac(t)));
  }

  private void warns(String css) throws Exception {
    CssTree t = parseCss(css);
    MessageQueue smq = new SimpleMessageQueue();
    CssValidator v = makeCssValidator(smq);
    boolean valid = v.validateCss(ac(t));
    mq.getMessages().addAll(smq.getMessages());
    assertTrue(css, valid);
    assertTrue(css, !smq.getMessages().isEmpty());
  }

  private void runTest(String css, String golden) throws Exception {
    MessageContext mc = new MessageContext();
    CssTree cssTree = parseCss(css);
    MessageQueue smq = new SimpleMessageQueue();
    CssValidator v = makeCssValidator(smq);
    boolean valid = v.validateCss(ac(cssTree));
    mq.getMessages().addAll(smq.getMessages());
    assertTrue(css, valid);

    mc.relevantKeys = new LinkedHashSet<SyntheticAttributeKey<?>>(
        Arrays.<SyntheticAttributeKey<?>>asList(
            CssValidator.CSS_PROPERTY_PART_TYPE,
            CssValidator.CSS_PROPERTY_PART));
    StringBuilder sb = new StringBuilder();
    cssTree.format(mc, sb);
    assertEquals(css, golden.trim(), sb.toString().trim());
    assertTrue(css, smq.getMessages().isEmpty());
  }

  private static CssValidator makeCssValidator(MessageQueue mq) {
    return new CssValidator(CssSchema.getDefaultCss21Schema(mq), mq);
  }

  private static <T extends ParseTreeNode> AncestorChain<T> ac(T node) {
    return new AncestorChain<T>(node);
  }
}
