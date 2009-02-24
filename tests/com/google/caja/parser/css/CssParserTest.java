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

package com.google.caja.parser.css;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.parser.css.CssTree.StyleSheet;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.SnippetProducer;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Join;
import com.google.caja.util.TestUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CssParserTest extends CajaTestCase {

  public void testBadHashValue() throws Exception {
    throwsParseException("h1 { color: #OOOOOO}");  // Letters instead of zeroes
  }

  public void testUnescape() throws Exception {
    FilePosition pos = FilePosition.startOfFile(is);
    assertEquals("", CssParser.unescape(
        Token.instance("", CssTokenType.IDENT, pos)));
    assertEquals("foo", CssParser.unescape(
        Token.instance("foo", CssTokenType.IDENT, pos)));
    assertEquals("foo", CssParser.unescape(
        Token.instance("f\\oo", CssTokenType.IDENT, pos)));
    assertEquals("!important", CssParser.unescape(
        Token.instance("! important", CssTokenType.IDENT, pos)));
    assertEquals("!important", CssParser.unescape(
        Token.instance("!   important", CssTokenType.IDENT, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo bar'", CssTokenType.STRING, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo\\ bar'", CssTokenType.STRING, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo\\ b\\\nar'", CssTokenType.STRING, pos)));
    assertEquals("'foo bar'", CssParser.unescape(
        Token.instance("'foo\\ b\\\rar'", CssTokenType.STRING, pos)));
    assertEquals("'ffoo bar'", CssParser.unescape(
        Token.instance("'\\66 foo bar'", CssTokenType.STRING, pos)));
    assertEquals("foo-bar", CssParser.unescape(
        Token.instance("\\66oo-ba\\0072", CssTokenType.IDENT, pos)));
    assertEquals("\\66oo-bar", CssParser.unescape(
        Token.instance("\\\\66oo-ba\\0072", CssTokenType.IDENT, pos)));
  }

  public void testCssParser1() throws Exception {
    runTestCssParser("cssparserinput1.css", "cssparsergolden1.txt", false);
  }

  public void testCssParser2() throws Exception {
    runTestCssParser("cssparserinput2.css", "cssparsergolden2.txt", false);
  }

  public void testCssParser3() throws Exception {
    runTestCssParser("cssparserinput3.css", "cssparsergolden3.txt", false);
  }

  public void testCssParser4() throws Exception {
    runTestCssParser("cssparserinput4.css", "cssparsergolden4.txt", false);
  }

  public void testTolerantParsing() throws Exception {
    String inputFile = "cssparserinput5.css";
    InputSource inputSource = new InputSource(
        TestUtil.getResource(getClass(), inputFile));
    runTestCssParser(inputFile, "cssparsergolden5.txt", true);
    assertEquals(
        TestUtil.readResource(getClass(), "csssnippets5.txt").trim(),
        renderSnippets(
            Collections.singletonMap(
                inputSource,
                TestUtil.readResource(getClass(), inputFile))));
  }

  private void runTestCssParser(
      String cssFile, String goldenFile, boolean tolerant)
      throws Exception {
    String golden = TestUtil.readResource(getClass(), goldenFile);
    CssTree.StyleSheet stylesheet;
    CharProducer cp = fromResource(cssFile);
    try {
      MessageLevel lvl = tolerant
          ? MessageLevel.WARNING : MessageLevel.FATAL_ERROR;
      stylesheet = new CssParser(
          CssParser.makeTokenQueue(cp, mq, false), mq, lvl)
          .parseStyleSheet();
    } finally {
      cp.close();
    }
    StringBuilder sb = new StringBuilder();
    stylesheet.format(new MessageContext(), sb);
    assertEquals(golden.trim(), sb.toString().trim());

    if (!tolerant) {
      // run in tolerant mode to make sure being tolerant doesn't introduce
      // failures.
      runTestCssParser(cssFile, goldenFile, true);
    }
  }

  private void throwsParseException(String fuzzString) {
    try {
      parseString(fuzzString);
    } catch (ParseException e) {
      // ParseException thrown - parser worked
      return;
    } catch (Throwable e) {
      // any other kind of exception means the parser broke
      e.printStackTrace();
      fail();
    }
  }

  private StyleSheet parseString(String fuzzString) throws Exception {
    return css(fromString(fuzzString));
  }

  private String renderSnippets(Map<InputSource, String> srcs)
       throws Exception {
    SnippetProducer sr = new SnippetProducer(srcs, mc, 80);
    List<Message> messages = new ArrayList<Message>(mq.getMessages());
    Collections.sort(messages, new Comparator<Message>() {
      public int compare(Message a, Message b) {
        FilePosition fpa = firstFilePosition(a.getMessageParts());
        FilePosition fpb = firstFilePosition(b.getMessageParts());
        int delta = fpa.startCharInFile() - fpb.startCharInFile();
        if (delta == 0) {
          delta = fpa.endCharInFile() - fpb.endCharInFile();
        }
        return delta;
      }
      private FilePosition firstFilePosition(List<MessagePart> parts) {
        if (parts.isEmpty()) { return FilePosition.UNKNOWN; }
        MessagePart p0 = parts.get(0);
        return p0 instanceof FilePosition
            ? (FilePosition) p0
            : FilePosition.UNKNOWN;
      }
    });
    List<String> snippets = new ArrayList<String>();
    for (Message msg : messages) {
      snippets.add(msg.getMessageLevel() + " : " + msg.format(mc));
      snippets.add(sr.getSnippet(msg));
    }
    return Join.join("\n", snippets);
  }
}
