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
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.TestUtil;

import java.io.StringReader;
import java.net.URI;

/**
 * Simple test harness for experimenting with quasiliteral rewrites. This
 * is not part of an automated test suite.
 *
 * @author ihab.awad@gmail.com
 */
public class DefaultRewriterExperiments {
  public static void main(String[] argv) throws Exception {
    showTree("var foo = (z + (x + y) * sin(0.374)) + 37;");
    showTree("var z = \"foo\" + \"bar\";");
  }

  private static void showTree(String programText) throws Exception {
    ParseTreeNode program = parse(programText);
    ParseTreeNode rewritten = new DefaultRewriter().expand(program);

    System.out.println("program = " + format(program));
    System.out.println("rewritten = " + format(rewritten));
  }

  private static String format(ParseTreeNode n) throws Exception {
    StringBuilder output = new StringBuilder();
    n.render(new RenderContext(new MessageContext(), output));
    return output.toString();
  }

  public static ParseTreeNode parse(String src) throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = TestUtil.createTestMessageQueue(mc);
    InputSource is = new InputSource(new URI("file:///no/input/source"));
    CharProducer cp = CharProducer.Factory.create(new StringReader(src), is);
    JsLexer lexer = new JsLexer(cp);
    JsTokenQueue tq = new JsTokenQueue(lexer, is, JsTokenQueue.NO_NON_DIRECTIVE_COMMENT);
    Parser p = new Parser(tq, mq);
    Statement stmt = p.parse();
    p.getTokenQueue().expectEmpty();
    return stmt;
  }
}