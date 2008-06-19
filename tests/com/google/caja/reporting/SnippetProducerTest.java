// Copyright (C) 2008 Google Inc.
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

package com.google.caja.reporting;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class SnippetProducerTest extends TestCase {
  static final String F1_TEXT = (
      "f1 line 1\n"
      + "\r\n"
      + "f1 line 3\n");

  static final String F2_TEXT = "f2 line 1";

  
  SnippetProducer s;
  final InputSource f1 = new InputSource(URI.create("file:///f1"));
  final InputSource f2 = new InputSource(URI.create("file:///f2"));

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Map<InputSource, String> originalSource
        = new HashMap<InputSource, String>();
    originalSource.put(f1, F1_TEXT);
    originalSource.put(f2, F2_TEXT);
    s = new SnippetProducer(originalSource, new MessageContext());
  }

  public enum TestMessageType implements MessageTypeInt {
    ZERO("ZERO", MessageLevel.LOG),
    ONE("%s: ONE", MessageLevel.WARNING),
    TWO("%s: TWO %s", MessageLevel.ERROR),
    THREE("%s: THREE %s %s", MessageLevel.FATAL_ERROR),
    ;

    private final String formatString;
    private final MessageLevel level;
    private final int paramCount;

    TestMessageType(String formatString, MessageLevel level) {
      this.formatString = formatString;
      this.level = level;
      this.paramCount = MessageType.formatStringArity(formatString);
    }

    public void format(MessagePart[] parts, MessageContext context,
                       Appendable out)
        throws IOException {
      MessageType.formatMessage(formatString, parts, context, out);
    }

    public MessageLevel getLevel() { return level; }

    public int getParamCount() { return paramCount; }
  }

  public void testGetSnippetNoPos() {
    Message msg = new Message(TestMessageType.ZERO);
    assertEquals("", s.getSnippet(msg));
  }

  public void testGetSnippetOnePartNoPos() {
    Message msg = new Message(
        TestMessageType.ONE, MessagePart.Factory.valueOf("hi"));
    assertEquals("", s.getSnippet(msg));
  }

  public void testGetSnippetOnePos() {
    Message msg = new Message(
        TestMessageType.ONE, FilePosition.instance(f2, 1, 1, 4, 4, 1, 1, 8, 8));
    assertEquals(
        ("f2:1: f2 line 1\n" +
         "         ^^^^"),
        s.getSnippet(msg));
  }

  public void testGetSnippetTwoPos() {
    Message msg = new Message(
        TestMessageType.TWO,
        FilePosition.instance(f2, 1, 1, 1, 1, 1, 1, 3, 3),
        // Starts on a newline to test that we use the line with text.
        FilePosition.instance(f1, 2, 2, 1 + F1_TEXT.indexOf("\r\nf1 line 3"), 1,
                              3, 3, 1 + F1_TEXT.indexOf(" line 3"), 3)
        );
    assertEquals(
        ("f2:1: f2 line 1\n" +
         "      ^^\n" +
         "f1:3: f1 line 3\n" +
         "      ^^"),
        s.getSnippet(msg));
  }

  public void testZeroLengthRegion() {
    Message msg = new Message(
        TestMessageType.ONE, FilePosition.instance(f2, 1, 1, 3, 3, 1, 1, 3, 3));
    assertEquals(
        ("f2:1: f2 line 1\n" +
         "        ^"),
        s.getSnippet(msg));
  }

  public void testZeroLengthAtEndOfLine() {
    int nlPos = 1 + F1_TEXT.indexOf('\n');
    Message msg = new Message(
        TestMessageType.ONE,
        FilePosition.instance(f1, 1, 1, nlPos, nlPos, 1, 1, nlPos, nlPos));
    assertEquals(
        ("f1:1: f1 line 1\n" +
         "               ^"),
        s.getSnippet(msg));
  }

  public void testZeroLengthAtEndOfFile() {
    int endPos = 1 + F2_TEXT.length();
    Message msg = new Message(
        TestMessageType.ONE,
        FilePosition.instance(f2, 1, 1, endPos, endPos, 1, 1, endPos, endPos));
    assertEquals(
        ("f2:1: f2 line 1\n" +
         "               ^"),
        s.getSnippet(msg));
  }
}
