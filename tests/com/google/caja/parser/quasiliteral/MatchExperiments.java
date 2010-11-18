// Copyright (C) 2007 Google Inc.
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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Statement;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.TestUtil;

import java.io.StringReader;
import java.net.URI;
import java.util.Map;

/**
 * Simple test harness for experimenting with quasiliteral matches during
 * development. This is not part of an automated test suite.
 *
 * @author ihab.awad@gmail.com
 */
public class MatchExperiments {
  public static void main(String[] argv) throws Exception {
    showTree(
        "var foo = (x + y) * sin(0.374);",
        "var foo = @bar;",
        "var zee = @bar * @bar;");
    showTree(
        "function foo() { };",
        "function @f() {};",
        "@f");
  }

  private static void showTree(
      String specimenText,
      String matchPatternText,
      String substPatternText) throws Exception {
    ParseTreeNode specimen = parse(specimenText);
    QuasiNode matchPattern = QuasiBuilder.parseQuasiNode(
        new InputSource(URI.create("built-in:///js-quasi-literals")),
        matchPatternText);
    QuasiNode substPattern = QuasiBuilder.parseQuasiNode(
        new InputSource(URI.create("built-in:///js-quasi-literals")),
        substPatternText);

    System.out.println("specimen = " + format(specimen));
    System.out.println("matchPattern = " + format(matchPattern));
    System.out.println("substPattern = " + format(substPattern));

    if (specimen.children().size() != 1)
      throw new Exception("Top level of specimen does not have exactly 1 child");

    Map<String, ParseTreeNode> matchResult =
      matchPattern.match(specimen.children().get(0));

    System.out.println(
        (matchResult == null) ?
        "match failed" :
        "matchResult = " + format(matchResult));

    if (matchResult == null) return;

    ParseTreeNode substResult = substPattern.substitute(matchResult);

    System.out.println(
        (substResult == null) ?
        "subst failed" :
        "substResult = " + format(substResult));
  }

  private static String format(QuasiNode n) {
    return n.render();
  }

  private static String format(ParseTreeNode n) throws Exception {
    MessageContext mc = new MessageContext();
    StringBuilder output = new StringBuilder();
    n.format(mc, output);
    return output.toString();
  }

  private static String format(Map<String, ParseTreeNode> map)
      throws Exception {
    StringBuilder sb = new StringBuilder("{\n");
    for (Map.Entry<String, ParseTreeNode> k : map.entrySet()) {
      sb.append(k.getKey()).append(" = ").append(format(k.getValue()))
          .append('\n');
    }
    return sb.append('}').toString();
  }

  public static ParseTreeNode parse(String src) throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = TestUtil.createTestMessageQueue(mc);
    InputSource is = new InputSource(new URI("file:///no/input/source"));
    CharProducer cp = CharProducer.Factory.create(new StringReader(src), is);
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, is, JsTokenQueue.NO_COMMENT);
    Parser p = new Parser(tq, mq);
    Statement stmt = p.parse();
    p.getTokenQueue().expectEmpty();
    return stmt;
  }
}
