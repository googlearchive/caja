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
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssTree;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.MoreAsserts;
import com.google.caja.util.SyntheticAttributeKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class CssValidatorTest extends CajaTestCase {
  public void testValidateColor() throws Exception {
    runTest("a { color: blue }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : a\n"
            + "    PropertyDeclaration\n"
            + "      Property : color\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=color::color\n"
            + "          IdentLiteral : blue");
    runTest("a { COLOR: Blue }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : a\n"
            + "    PropertyDeclaration\n"
            + "      Property : color\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=color::color\n"
            + "          IdentLiteral : Blue");
    runTest("a { color: #00f }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : a\n"
            + "    PropertyDeclaration\n"
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
            + "    PropertyDeclaration\n"
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
            + "    PropertyDeclaration\n"
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
            + "    PropertyDeclaration\n"
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
            + "    PropertyDeclaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font\n"
            + "          IdentLiteral : caption\n"
            + "    EmptyDeclaration");
    fails("bogus, dl { font: caption; }");
    fails("p, bogus { font: caption; }");
    fails("p[bogus] { font: caption; }");
    runTest("p { font: waption; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    EmptyDeclaration\n",
            "WARNING: removing css property font with bad value:"
            + " ==>waption<==");
    runTest("p, dl { font: status-bar; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    PropertyDeclaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font\n"
            + "          IdentLiteral : status-bar\n"
            + "    EmptyDeclaration");
    runTest("p, dl { font: status-bar caption; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value: "
            + "status-bar  ==><==  caption");

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
            + "    PropertyDeclaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=font-size\n"
            + "          QuantityLiteral : 12pt\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Arial\n"
            + "    EmptyDeclaration");
    warns("p, dl { font: -12pt Arial; }");
    runTest("p, dl { font: -12pt url(Arial); }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value:"
            + " -12pt  ==>url('Arial')<==");
    runTest("p, dl { font: twelve Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value:"
            + " ==>twelve<==  Arial");
    runTest("p, dl { font: 150% Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    PropertyDeclaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=PERCENTAGE"
                        + " ; cssPropertyPart=font-size\n"
            + "          QuantityLiteral : 150%\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Arial\n"
            + "    EmptyDeclaration");
    runTest("p, dl { font: 150Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value:"
            + " ==>150Arial<==");
    runTest("p, dl { font: 150/Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value:"
            + " 150 /  ==>Arial<==");
    runTest("p, dl { font: medium Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    PropertyDeclaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-size::absolute-size\n"
            + "          IdentLiteral : medium\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Arial\n"
            + "    EmptyDeclaration");
    runTest("p, dl { font: medium; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value: medium");

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
            + "    PropertyDeclaration\n"
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
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Arial\n"
            + "    EmptyDeclaration");
    runTest("p, dl { font: italic bolderer 150% Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value:"
            + " italic  ==>bolderer<==  150% Arial");
    runTest("p, dl { font: italix bolder 150% Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value:"
            + " ==>italix<==  bolder 150% Arial");

    // font-size also matches by previous terms
    runTest("p, dl { font: inherit \"Arial\"; }",  // special
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    PropertyDeclaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-size\n"
            + "          IdentLiteral : inherit\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=STRING"
                        + " ; cssPropertyPart=font-family::family-name\n"
            + "          StringLiteral : Arial\n"
            + "    EmptyDeclaration");
    runTest("p, dl { font: inherit; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value: inherit");

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
            + "    PropertyDeclaration\n"
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
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Arial\n"
            + "    EmptyDeclaration");
    runTest("p, dl { font: 800; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value: 800");

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
            + "    PropertyDeclaration\n"
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
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Arial\n"
            + "    EmptyDeclaration");
    runTest("p, dl { font: abnormal 150% Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value:"
            + " ==>abnormal<==  150% Arial");

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
            + "    PropertyDeclaration\n"
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
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Arial\n"
            + "    EmptyDeclaration");
    runTest("p, dl { font: abnormal 150%/175% Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value:"
            + " ==>abnormal<==  150% / 175% Arial");
    runTest("p, dl { font: normal 800 150%/ Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    EmptyDeclaration",
            "WARNING: removing css property font with bad value:"
            + " normal 800 150% /  ==>Arial<==");
    runTest("p, dl { font: normal 800 150%/17.5 Arial; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : dl\n"
            + "    PropertyDeclaration\n"
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
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Arial\n"
            + "    EmptyDeclaration");
    warns("p, dl { font: normal 800 150%/-175% Arial; }");
    warns("p, dl { font: normal 800 150%/-17.5 Arial; }");

    // make sure the first three inherits match different parts
    runTest("p { font: inherit inherit inherit Arial; }",
            "StyleSheet\n" +
            "  RuleSet\n" +
            "    Selector\n" +
            "      SimpleSelector\n" +
            "        IdentLiteral : p\n" +
            "    PropertyDeclaration\n" +
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
            "        Term ; cssPropertyPartType=LOOSE_WORD"
                      + " ; cssPropertyPart=font-family::family-name"
                                         + "::loose-quotable-words\n" +
            "          IdentLiteral : Arial\n" +
            "    EmptyDeclaration");
  }

  public void testValidateUnquotedFamilyNames() throws Exception {
    runTest("p { font-family: Arial Black }",
            "StyleSheet\n" +
            "  RuleSet\n" +
            "    Selector\n" +
            "      SimpleSelector\n" +
            "        IdentLiteral : p\n" +
            "    PropertyDeclaration\n" +
            "      Property : font-family\n" +
            "      Expr\n" +
            "        Term ; cssPropertyPartType=LOOSE_WORD"
                      + " ; cssPropertyPart=font-family::family-name"
                                         + "::loose-quotable-words\n" +
            "          IdentLiteral : Arial\n" +
            "        Operation : NONE\n" +
            "        Term ; cssPropertyPartType=LOOSE_WORD"
                      + " ; cssPropertyPart=font-family::family-name"
                                         + "::loose-quotable-words\n" +
            "          IdentLiteral : Black"
            );
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
            + "    PropertyDeclaration\n"
            + "      Property : border\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=border-top-color\n"
            + "          IdentLiteral : inherit\n"
            + "    EmptyDeclaration");
    runTest("p { border: 2px }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
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
            + "    PropertyDeclaration\n"
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
            + "    PropertyDeclaration\n"
            + "      Property : border\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=border::border-style\n"
            + "          IdentLiteral : solid\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=border-top-color::color\n"
            + "          IdentLiteral : black\n"
            + "    EmptyDeclaration");
    runTest("p { border:solid black 1em}",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
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
            + "    PropertyDeclaration\n"
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
            + "    PropertyDeclaration\n"
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
    // Tests a string that is not a URL.
    runTest(""
            + "#body:before { content: ' ' }"
            + "#body:after { content: '.' }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdLiteral : #body\n"
            + "        Pseudo\n"
            + "          IdentLiteral : before\n"
            + "    PropertyDeclaration\n"
            + "      Property : content\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=STRING"
                        + " ; cssPropertyPart=content\n"
            + "          StringLiteral :  \n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdLiteral : #body\n"
            + "        Pseudo\n"
            + "          IdentLiteral : after\n"
            + "    PropertyDeclaration\n"
            + "      Property : content\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=STRING"
                        + " ; cssPropertyPart=content\n"
            + "          StringLiteral : .\n");
  }

  public void testBackground() throws Exception {
    runTest("p { background: url( /images/smiley-face.jpg ) no-repeat }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
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
            + "    PropertyDeclaration\n"
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
            + "    PropertyDeclaration\n"
            + "      Property : background-image\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=URI"
                        + " ; cssPropertyPart=background-image\n"
            + "          StringLiteral : /images/smiley-face.jpg");
    runTest("p { background:#F7F7F7 url(/images/foo.gif) no-repeat scroll"
            + " left top; }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : background\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=COLOR"
                        + " ; cssPropertyPart=background-color::color\n"
            + "          HashLiteral : #F7F7F7\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=URI"
                        + " ; cssPropertyPart=background-image\n"
            + "          UriLiteral : /images/foo.gif\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-repeat\n"
            + "          IdentLiteral : no-repeat\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-attachment\n"
            + "          IdentLiteral : scroll\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-position\n"
            + "          IdentLiteral : left\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-position\n"
            + "          IdentLiteral : top\n"
            + "    EmptyDeclaration\n"
            );
    runTest("p { background:#FFEBE8 none repeat scroll 0% }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : background\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=COLOR"
                        + " ; cssPropertyPart=background-color::color\n"
            + "          HashLiteral : #FFEBE8\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-image\n"
            + "          IdentLiteral : none\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-repeat\n"
            + "          IdentLiteral : repeat\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-attachment\n"
            + "          IdentLiteral : scroll\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=PERCENTAGE"
                        + " ; cssPropertyPart=background-position\n"
            + "          QuantityLiteral : 0%\n"
            );
    runTest("p { background: transparent url(/foo.gif) no-repeat top right }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : background\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-color\n"
            + "          IdentLiteral : transparent\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=URI"
                        + " ; cssPropertyPart=background-image\n"
            + "          UriLiteral : /foo.gif\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-repeat\n"
            + "          IdentLiteral : no-repeat\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-position\n"
            + "          IdentLiteral : top\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-position\n"
            + "          IdentLiteral : right\n"
            );
  }

  public void testBackgroundPosition() throws Exception {
    // TODO(mikesamuel): We could break the position rule into multiple
    // subrules so that the part for "right" becomes background-position::x-pos,
    // and the part for "top" becomes background-position::y-pos.
    runTest("p { background-position: right top }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : background-position\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-position\n"
            + "          IdentLiteral : right\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-position\n"
            + "          IdentLiteral : top\n"
            );
    runTest("p { background-position: top center }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : background-position\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-position\n"
            + "          IdentLiteral : top\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-position\n"
            + "          IdentLiteral : center\n"
            );
    runTest("p { background-position: center }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : background-position\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-position\n"
            + "          IdentLiteral : center\n"
            );
    runTest("p { background-position: bottom }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : background-position\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=background-position\n"
            + "          IdentLiteral : bottom\n"
            );
  }

  public void testPositionSubstitution() throws Exception {
    runTest("p { left: ${3}px }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : left\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=left\n"
            + "          Substitution : ${3}px");
  }

  public void testColorSubstitution() throws Exception {
    runTest("p { background: ${shade << 16 | shade << 8 | shade} }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : background\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=COLOR"
                        + " ; cssPropertyPart=background-color::color\n"
            + "          Substitution : ${shade << 16 | shade << 8 | shade}");
  }

  public void testUriSubstitution() throws Exception {
    runTest("p { background: ${imageName + '.png'}uri }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : background\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=URI"
                        + " ; cssPropertyPart=background-image\n"
            + "          Substitution : ${imageName + '.png'}uri");
    runTest("p { background-image: ${imageName + '.png'} }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : background-image\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=URI"
                        + " ; cssPropertyPart=background-image\n"
            + "          Substitution : ${imageName + '.png'}");
  }

  public void testFontFamily() throws Exception {
    runTest("a { font: 12pt Times New Roman, Times, 'Times Old Roman', serif }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : a\n"
            + "    PropertyDeclaration\n"
            + "      Property : font\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=font-size\n"
            + "          QuantityLiteral : 12pt\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Times\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : New\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Roman\n"
            + "        Operation : COMMA\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Times\n"
            + "        Operation : COMMA\n"
            + "        Term ; cssPropertyPartType=STRING"
                        + " ; cssPropertyPart=font-family::family-name\n"
            + "          StringLiteral : Times Old Roman\n"
            + "        Operation : COMMA\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-family::generic-family\n"
            + "          IdentLiteral : serif\n"
            );
    runTest("p { font-family: Georgia, \"Times New Roman\", Times, serif }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : font-family\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Georgia\n"
            + "        Operation : COMMA\n"
            + "        Term ; cssPropertyPartType=STRING"
                        + " ; cssPropertyPart=font-family::family-name\n"
            + "          StringLiteral : Times New Roman\n"
            + "        Operation : COMMA\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Times\n"
            + "        Operation : COMMA\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-family::generic-family\n"
            + "          IdentLiteral : serif\n"
            );
    runTest("p { font-family: Times New Roman }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : font-family\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Times\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : New\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Roman\n"
            );
    runTest("p { font-family: Heisi  Minco W3, serif }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : font-family\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Heisi\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : Minco\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LOOSE_WORD"
                        + " ; cssPropertyPart=font-family::family-name"
                                           + "::loose-quotable-words\n"
            + "          IdentLiteral : W3\n"
            + "        Operation : COMMA\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=font-family::generic-family\n"
            + "          IdentLiteral : serif\n"
            );
  }

  public void testUnitlessLengths() throws Exception {
    runTest("p { padding: 4 10 0 10 }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : padding\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=padding::padding-width\n"
            + "          QuantityLiteral : 4\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=padding::padding-width\n"
            + "          QuantityLiteral : 10\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=padding::padding-width\n"
            + "          QuantityLiteral : 0\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=padding::padding-width\n"
            + "          QuantityLiteral : 10\n"
            );
    runTest("p { border: .125in 6 }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : border\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=border::border-width\n"
            + "          QuantityLiteral : .125in\n"
            + "        Operation : NONE\n"
            + "        Term ; cssPropertyPartType=LENGTH"
                        + " ; cssPropertyPart=border::border-width\n"
            + "          QuantityLiteral : 6\n"
            );
  }

  public void testOpacity() throws Exception {
    runTest("img {\n"
            + "  opacity: 0.5;\n"
            + "  filter:alpha(opacity=50)\n"
            + "         progid:DXImageTransform.Microsoft.Alpha(opacity=50) }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : img\n"
            + "    PropertyDeclaration\n"
            + "      Property : opacity\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=NUMBER"
                        + " ; cssPropertyPart=opacity::alphavalue\n"
            + "          QuantityLiteral : 0.5\n"
            + "    PropertyDeclaration\n"
            + "      Property : filter\n"
            + "      Expr\n"
            + "        Term\n"
            + "          FunctionCall : alpha\n"
            + "            Expr\n"
            + "              Term ; cssPropertyPartType=IDENT"
                              + " ; cssPropertyPart=filter::ie-filter-opacity\n"
            + "                IdentLiteral : opacity\n"
            + "              Operation : EQUAL\n"
            + "              Term ; cssPropertyPartType=NUMBER"
                              + " ; cssPropertyPart=filter::ie-filter-opacity\n"
            + "                QuantityLiteral : 50\n"
            + "        Operation : NONE\n"
            + "        Term\n"
            + "          ProgId : dximagetransform.microsoft.alpha\n"
            + "            ProgIdAttribute : opacity\n"
            + "              Term ; cssPropertyPartType=NUMBER"
                              + " ; cssPropertyPart=filter::prog-id"
                                   + "::prog-id-alpha::filter-opacity\n"
            + "                QuantityLiteral : 50\n"
            );
  }

  public void testProgId() throws Exception {
    runTest(
        "img {\n"
        + "  filter:progid:DXImageTransform.Microsoft.AlphaImageLoader("
        + "      src='howdy', sizingMethod='scale') }",
        "StyleSheet\n"
        + "  RuleSet\n"
        + "    Selector\n"
        + "      SimpleSelector\n"
        + "        IdentLiteral : img\n"
        + "    PropertyDeclaration\n"
        + "      Property : filter\n"
        + "      Expr\n"
        + "        Term\n"
        + "          ProgId : dximagetransform.microsoft.alphaimageloader\n"
        + "            ProgIdAttribute : src\n"
        + "              Term ; cssPropertyPartType=URI"
                          + " ; cssPropertyPart=filter::prog-id"
                              + "::prog-id-alpha-image-loader::page-url\n"
        + "                StringLiteral : howdy\n"
        + "            ProgIdAttribute : sizingmethod\n"
        + "              Term ; cssPropertyPartType=STRING"
                          + " ; cssPropertyPart=filter::prog-id"
                              + "::prog-id-alpha-image-loader::sizing-method\n"
        + "                StringLiteral : scale\n"
        );
    runTest("p { filter: progid:foo.bar() }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n",
            "WARNING: removing css property filter with bad value:"
            + " ==>progid:foo.bar()<==");
    runTest("p { filter: progid:dximagetransform.microsoft.alpha(opaquity=50) }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n",
            "WARNING: removing css property filter with bad value:"
            + " ==>progid:dximagetransform.microsoft.alpha(opaquity=50)<==");
  }

  public void testStarHack() throws Exception {
    runTest("p {\n"
            + "  color: blue;\n"
            + "  *color: red }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : color\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=color::color\n"
            + "          IdentLiteral : blue\n"
            + "    UserAgentHack : [IE6, IE7]\n"
            + "      PropertyDeclaration\n"
            + "        Property : color\n"
            + "        Expr\n"
            + "          Term ; cssPropertyPartType=IDENT"
                          + " ; cssPropertyPart=color::color\n"
            + "            IdentLiteral : red\n"
            );
    runTest("p { *color: yelow }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n",
            "WARNING: removing css property color with bad value: ==>yelow<==");
  }

  public void testHtmlStarHack() throws Exception {
    runTest("* html p { color: blue }",
            "StyleSheet\n"
            + "  RuleSet\n"
            + "    Selector\n"
            + "      SimpleSelector\n"
            + "        WildcardElement\n"
            + "      Combination : DESCENDANT\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : html\n"
            + "      Combination : DESCENDANT\n"
            + "      SimpleSelector\n"
            + "        IdentLiteral : p\n"
            + "    PropertyDeclaration\n"
            + "      Property : color\n"
            + "      Expr\n"
            + "        Term ; cssPropertyPartType=IDENT"
                        + " ; cssPropertyPart=color::color\n"
            + "          IdentLiteral : blue\n"
            );
    fails("* html { color: blue }");
    fails("* html > p { color: blue }");
    fails("* html object { color: blue }");
    fails("* html#hiya p { color: blue }");
  }

  private void fails(String css) throws Exception {
    CssTree t = css(fromString(css), true);
    mq.getMessages().clear();
    CssValidator v = makeCssValidator(mq);
    assertTrue(css, !v.validateCss(ac(t)));
    MessageLevel maxLevel = MessageLevel.values()[0];
    for (Message msg : mq.getMessages()) {
      MessageLevel level = msg.getMessageLevel();
      if (level.compareTo(maxLevel) > 0) { maxLevel = level; }
    }
    // If there is a failure, there should be an error or greater on the queue.
    assertTrue(maxLevel.name(), MessageLevel.ERROR.compareTo(maxLevel) <= 0);
  }

  private void warns(String css) throws Exception {
    MessageQueue smq = new SimpleMessageQueue();
    CssTree t = css(fromString(css), true);
    CssValidator v = makeCssValidator(smq);
    boolean valid = v.validateCss(ac(t));
    mq.getMessages().addAll(smq.getMessages());
    assertTrue(css, valid);
    assertTrue(css, !mq.getMessages().isEmpty());
  }
  
  private static void removeInvalidNodes(AncestorChain<? extends CssTree> t) {
    if (t.node.getAttributes().is(CssValidator.INVALID)) {
      ((MutableParseTreeNode) t.parent.node).removeChild(t.node);
      return;
    }

    // Use a mutation to remove invalid nodes so that the sanity checks in
    // childrenChanged sees all removals at once.
    MutableParseTreeNode.Mutation mut = null;
    for (CssTree child : t.node.children()) {
      if (child.getAttributes().is(CssValidator.INVALID)) {
        if (mut == null) { mut = t.node.createMutation(); }
        mut.removeChild(child);
      } else {
        removeInvalidNodes(AncestorChain.instance(t, child));
      }
    }
    if (mut != null) { mut.execute(); }
  }


  private void runTest(String css, String golden, String... warnings)
    throws Exception {
    MessageContext mc = new MessageContext();
    mq.getMessages().clear();
    CssTree cssTree = css(fromString(css), true);
    MessageQueue smq = new SimpleMessageQueue();
    CssValidator v = makeCssValidator(smq);
    boolean valid = v.validateCss(ac(cssTree));
    mq.getMessages().addAll(smq.getMessages());
    
    // If no warnings are expected, the result should be valid
    if (warnings.length == 0) {
      if (!valid) {
        System.err.println(cssTree.toStringDeep());
      }
      assertTrue(css, valid);
    } else {
      removeInvalidNodes(AncestorChain.instance(cssTree));
    }
    
    mc.relevantKeys = new LinkedHashSet<SyntheticAttributeKey<?>>(
        Arrays.<SyntheticAttributeKey<?>>asList(
            CssValidator.CSS_PROPERTY_PART_TYPE,
            CssValidator.CSS_PROPERTY_PART));
    StringBuilder sb = new StringBuilder();
    cssTree.format(mc, sb);
    assertEquals(css, golden.trim(), sb.toString().trim());

    List<String> actualWarnings = new ArrayList<String>();
    for (Message msg : mq.getMessages()) {
      if (MessageLevel.WARNING.compareTo(msg.getMessageLevel()) <= 0) {
        String msgText = msg.format(mc);
        msgText = msgText.substring(msgText.indexOf(": ") + 1);
        actualWarnings.add(msg.getMessageLevel().name() + ":" + msgText);
      }
    }
    mq.getMessages().clear();
    MoreAsserts.assertListsEqual(Arrays.asList(warnings), actualWarnings);
  }

  private static CssValidator makeCssValidator(MessageQueue mq) {
    return new CssValidator(
        CssSchema.getDefaultCss21Schema(mq), HtmlSchema.getDefault(mq), mq);
  }

  private static <T extends ParseTreeNode> AncestorChain<T> ac(T node) {
    return new AncestorChain<T>(node);
  }
}
