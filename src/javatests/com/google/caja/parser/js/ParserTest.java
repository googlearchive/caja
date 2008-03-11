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
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.TestUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class ParserTest extends TestCase {
  // TODO(mikesamuel): better comment each of the test input files.
  // What is each one supposed to test.

  private MessageContext mc;
  private MessageQueue mq;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mc = new MessageContext();
    mq = TestUtil.createTestMessageQueue(mc);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    mc = null;
    mq = null;
  }

  public void testParser() throws Exception {
    runParseTest("parsertest1.js", "parsergolden1.txt",
                 "Reserved word else used as an identifier");

    // Check warnings on message queue.
    Iterator<Message> msgs = mq.getMessages().iterator();
    assertTrue(msgs.hasNext());
    Message m1 = msgs.next();
    assertEquals(MessageType.RESERVED_WORD_USED_AS_IDENTIFIER,
                 m1.getMessageType());
    assertFilePosition("parsertest1.js:11+29 - 33",
                       (FilePosition) m1.getMessageParts().get(0), mc);
    assertEquals(Keyword.ELSE, m1.getMessageParts().get(1));
    assertTrue(msgs.hasNext());
    Message m2 = msgs.next();
    assertEquals(MessageType.NOT_IE, m2.getMessageType());
    assertFilePosition("parsertest1.js:35+7 - 8",
                       (FilePosition) m2.getMessageParts().get(0), mc);
    assertTrue(!msgs.hasNext());
  }
  public void testParser2() throws Exception {
    runParseTest("parsertest2.js", "parsergolden2.txt");

    // Check warnings on message queue.
    Iterator<Message> msgs = mq.getMessages().iterator();
    assertTrue(msgs.hasNext());
    Message m1 = msgs.next();
    assertEquals(MessageType.SEMICOLON_INSERTED, m1.getMessageType());
    assertFilePosition("parsertest2.js:4+3",
                       (FilePosition) m1.getMessageParts().get(0), mc);
    assertTrue(!msgs.hasNext());
  }
  public void testParser3() throws Exception {
    runParseTest("parsertest3.js", "parsergolden3.txt");
    assertTrue(mq.getMessages().isEmpty());
  }
  public void testParser5() throws Exception {
    runParseTest("parsertest5.js", "parsergolden5.txt");
  }
  public void testParser7() throws Exception {
    runParseTest("parsertest7.js", "parsergolden7.txt",
                 "Reserved word null used as an identifier",
                 "Reserved word false used as an identifier",
                 "Reserved word if used as an identifier",
                 "Reserved word function used as an identifier",
                 "Reserved word with used as an identifier",
                 "Reserved word debugger used as an identifier");
  }
  public void testParser8() throws Exception {
    runParseTest("parsertest8.js", "parsergolden8.txt");
  }

  public void testParseTreeRendering1() throws Exception {
    runRenderTest("parsertest1.js", "rendergolden1.txt", false);
  }
  public void testParseTreeRendering2() throws Exception {
    runRenderTest("parsertest2.js", "rendergolden2.txt", false);
  }
  public void testParseTreeRendering3() throws Exception {
    runRenderTest("parsertest3.js", "rendergolden3.txt", false);
  }
  public void testParseTreeRendering4() throws Exception {
    runRenderTest("parsertest4.js", "rendergolden4.txt", false);
  }
  public void testParseTreeRendering5() throws Exception {
    runRenderTest("parsertest5.js", "rendergolden5.txt", false);
  }
  public void testSecureParseTreeRendering6() throws Exception {
    runRenderTest("parsertest6.js", "rendergolden6.txt", true);

    // Since we're doing these checks for security, double check that someone
    // hasn't adjusted the golden file.
    String golden = TestUtil.readResource(getClass(), "rendergolden6.txt")
        .toLowerCase();
    assertFalse(golden.contains("]]>"));
    assertFalse(golden.contains("<!"));
    assertFalse(golden.contains("<script"));
    assertFalse(golden.contains("</script"));
  }
  public void testParseTreeRendering7() throws Exception {
    runRenderTest("parsertest7.js", "rendergolden7.txt", false);
  }
  public void testParseTreeRendering8() throws Exception {
    runRenderTest("parsertest8.js", "rendergolden8.txt", true);
  }

  private void runRenderTest(
      String testFile, String goldenFile, boolean paranoid)
      throws Exception {
    Statement parseTree = TestUtil.parseTree(getClass(), testFile, mq);
    TestUtil.checkFilePositionInvariants(parseTree);

    RenderContext rc = new RenderContext(mc, new StringBuilder(), paranoid);
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

  private void runParseTest(
      String testFile, String goldenFile, String ... errors)
      throws Exception {
    Statement parseTree = TestUtil.parseTree(getClass(), testFile, mq);
    TestUtil.checkFilePositionInvariants(parseTree);

    StringBuilder output = new StringBuilder();
    parseTree.format(mc, output);

    // Check that parse tree matches.
    String golden = TestUtil.readResource(getClass(), goldenFile);
    assertEquals(golden, output.toString());

    // Clone the parse tree, and check that it, too, matches.
    Statement cloneParseTree = (Statement)parseTree.clone();
    StringBuilder cloneOutput = new StringBuilder();
    cloneParseTree.format(mc, cloneOutput);
    assertEquals(golden, cloneOutput.toString());    

    Set<String> actualErrors = new LinkedHashSet<String>();
    for (Message m : mq.getMessages()) {
      if (MessageLevel.ERROR.compareTo(m.getMessageLevel()) <= 0) {
        String error = m.toString();
        actualErrors.add(error.substring(error.indexOf(": ") + 2));
      }
    }

    Set<String> expectedErrors
        = new LinkedHashSet<String>(Arrays.asList(errors));
    assertEquals(expectedErrors, actualErrors);
  }
}
