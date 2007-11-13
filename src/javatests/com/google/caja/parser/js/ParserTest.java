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

package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Keyword;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.TestUtil;

import java.io.IOException;
import java.util.Iterator;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class ParserTest extends TestCase {
  // TODO(mikesamuel): better comment each of the test input files.
  // What is each one supposed to test.

  public void testParser() throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = TestUtil.createTestMessageQueue(mc);
    Statement parseTree = TestUtil.parseTree(getClass(), "parsertest1.js", mq);

    StringBuilder output = new StringBuilder();
    parseTree.format(mc, output);

    // check that parse tree matches
    String golden = TestUtil.readResource(getClass(), "parsergolden1.txt");
    assertEquals(golden, output.toString());

    // check warnings on message queue
    Iterator<Message> msgs = mq.getMessages().iterator();
    assertTrue(msgs.hasNext());
    Message m1 = msgs.next();
    assertEquals(MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
                 m1.getMessageType());
    assertEquals(Keyword.ELSE, m1.getMessageParts().get(0));
    assertFilePosition("parsertest1.js:11+29 - 33",
        (FilePosition) m1.getMessageParts().get(1), mc);
    assertTrue(msgs.hasNext());
    Message m2 = msgs.next();
    assertEquals(MessageType.NOT_IE, m2.getMessageType());
    assertFilePosition("parsertest1.js:35+7 - 8",
                       (FilePosition) m2.getMessageParts().get(0), mc);
    assertTrue(!msgs.hasNext());
  }

  public void testParser2() throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = TestUtil.createTestMessageQueue(mc);
    Statement parseTree = TestUtil.parseTree(getClass(), "parsertest2.js", mq);

    StringBuilder output = new StringBuilder();
    parseTree.format(mc, output);

    // check that parse tree matches
    String golden = TestUtil.readResource(getClass(), "parsergolden2.txt");
    assertEquals(golden, output.toString());

    // check warnings on message queue
    Iterator<Message> msgs = mq.getMessages().iterator();
    assertTrue(msgs.hasNext());
    Message m1 = msgs.next();
    assertEquals(MessageType.SEMICOLON_INSERTED,
        m1.getMessageType());
    assertFilePosition("parsertest2.js:3+3",
        (FilePosition) m1.getMessageParts().get(0), mc);
    assertTrue(!msgs.hasNext());
  }

  public void testParser3() throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = TestUtil.createTestMessageQueue(mc);
    Statement parseTree = TestUtil.parseTree(getClass(), "parsertest3.js", mq);

    StringBuilder output = new StringBuilder();
    parseTree.format(mc, output);

    // check that parse tree matches
    String golden = TestUtil.readResource(getClass(), "parsergolden3.txt");
    assertEquals(golden, output.toString());
  }

  public void testParser5() throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = TestUtil.createTestMessageQueue(mc);
    Statement parseTree = TestUtil.parseTree(getClass(), "parsertest5.js", mq);

    StringBuilder output = new StringBuilder();
    parseTree.format(mc, output);

    // check that parse tree matches
    String golden = TestUtil.readResource(getClass(), "parsergolden5.txt");
    assertEquals(golden, output.toString());
  }

  public void testParseTreeRendering1() throws Exception {
    runRenderTest("parsertest1.js", "rendergolden1.txt");
  }
  public void testParseTreeRendering2() throws Exception {
    runRenderTest("parsertest2.js", "rendergolden2.txt");
  }
  public void testParseTreeRendering3() throws Exception {
    runRenderTest("parsertest3.js", "rendergolden3.txt");
  }
  public void testParseTreeRendering4() throws Exception {
    runRenderTest("parsertest4.js", "rendergolden4.txt");
  }
  public void testParseTreeRendering5() throws Exception {
    runRenderTest("parsertest5.js", "rendergolden5.txt");
  }

  private void runRenderTest(String testFile, String goldenFile)
      throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = TestUtil.createTestMessageQueue(mc);
    Statement parseTree = TestUtil.parseTree(getClass(), testFile, mq);

    RenderContext rc = new RenderContext(mc, new StringBuilder());
    parseTree.render(rc);
    rc.newLine();

    String golden = TestUtil.readResource(getClass(), goldenFile);
    String actual = rc.out.toString();
    assertEquals(actual, golden, actual);
  }

  private void assertFilePosition(
      String golden, FilePosition actual, MessageContext mc)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    actual.format(mc, sb);
    assertEquals(golden, sb.toString());
  }
}
