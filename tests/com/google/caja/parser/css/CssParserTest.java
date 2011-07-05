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
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssTree.StyleSheet;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.SnippetProducer;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Join;
import com.google.caja.util.Name;
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

  public final void testBadHashValue() {
    throwsParseException("h1 { color: #OOOOOO}");  // Letters instead of zeroes
  }

  public final void testUnescape() {
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

  public final void testCssParser1() throws Exception {
    runTestCssParser("cssparserinput1.css", "cssparsergolden1.txt", false);
  }

  public final void testCssParser2() throws Exception {
    runTestCssParser("cssparserinput2.css", "cssparsergolden2.txt", false);
  }

  public final void testCssParser3() throws Exception {
    runTestCssParser("cssparserinput3.css", "cssparsergolden3.txt", false);
  }

  public final void testCssParser4() throws Exception {
    runTestCssParser("cssparserinput4.css", "cssparsergolden4.txt", false);
  }

  public final void testTolerantParsing5() throws Exception {
    runTolerantParsing(
        "cssparserinput5.css", "cssparsergolden5.txt", "csssnippets5.txt");
  }

  public final void testCssParser6() throws Exception {
    runTestCssParser("cssparserinput6.css", "cssparsergolden6.txt", false);
  }

  public final void testTolerantParsing7() throws Exception {
    runTolerantParsing(
        "cssparserinput7.css", "cssparsergolden7.txt", "csssnippets7.txt");
  }

  public final void testFilters() throws Exception {
    runTestCssParser(
        "cssparserinput-filters.css", "cssparsergolden-filters.txt", true);
    assertMessagesLessSevereThan(MessageLevel.WARNING);
  }

  public final void testUserAgentHacks() throws Exception {
    runTestCssParser(
        "cssparserinput-uahacks.css", "cssparsergolden-uahacks.txt", true);
    assertMessagesLessSevereThan(MessageLevel.WARNING);
  }

  public final void testFilterFilePositions() throws Exception {
    CssTree.DeclarationGroup ss = cssDecls(
        // Character in file indices used in assertFilePosition below.
        //          0        1         2         3         4         5
        //          1234567890123456789012345678901234567890123456789012345
        fromString("filter:progid:foo.bar.baz(a=1) progid:foo.bar.baz(a=2)"));
    assertMessagesLessSevereThan(MessageLevel.WARNING);

    assertEquals(1, ss.children().size());
    CssTree.PropertyDeclaration d
        = (CssTree.PropertyDeclaration) ss.children().get(0);
    assertFilePosition(1, 55, d);
    CssTree.Property p = d.getProperty();
    assertFilePosition(1, 7, p);
    assertEquals(Name.css("filter"), p.getPropertyName());
    CssTree.Expr e = d.getExpr();
    assertFilePosition(8, 55, e);
    assertEquals(2, e.getNTerms());
    CssTree.Term t0 = e.getNthTerm(0);
    assertFilePosition(8, 31, t0);
    CssTree.ProgId progId0 = (CssTree.ProgId) t0.getExprAtom();
    assertFilePosition(8, 31, progId0);
    assertEquals(Name.css("foo.bar.baz"), progId0.getName());
    assertEquals(1, progId0.children().size());
    CssTree.ProgIdAttribute a0 = progId0.children().get(0);
    assertFilePosition(27, 30, a0);
    assertEquals(Name.css("a"), a0.getName());
    CssTree.Term v0 = a0.getPropertyValue();
    assertFilePosition(29, 30, v0);
    CssTree.QuantityLiteral vl0 = (CssTree.QuantityLiteral) v0.getExprAtom();
    assertFilePosition(29, 30, vl0);
    assertEquals("1", vl0.getValue());
    CssTree.Term t1 = e.getNthTerm(1);
    assertFilePosition(32, 55, t1);
    CssTree.ProgId progId1 = (CssTree.ProgId) t1.getExprAtom();
    assertFilePosition(32, 55, progId1);
    assertEquals(Name.css("foo.bar.baz"), progId1.getName());
    assertEquals(1, progId1.children().size());
    CssTree.ProgIdAttribute a1 = progId1.children().get(0);
    assertFilePosition(51, 54, a1);
    assertEquals(Name.css("a"), a1.getName());
    CssTree.Term v1 = a1.getPropertyValue();
    assertFilePosition(53, 54, v1);
    CssTree.QuantityLiteral vl1 = (CssTree.QuantityLiteral) v1.getExprAtom();
    assertFilePosition(53, 54, vl1);
    assertEquals("2", vl1.getValue());
  }

  // TODO(jasvir): Should whitespace after the units in quantities be escaped
  // S 4.3.10 of CSS2 spec says no, but 4/4 browsers say yes.
  public final void testUnexpectedErrors() throws Exception {
    throwsParseException("div { top:58px\\9;}");
  }

  public final void testErrorMessages() throws Exception {
    runTestCssParser(
        fromString("p { color:e# }"),
        fromString(Join.join(
            "\n",
            "StyleSheet",
            "  RuleSet",
            "    Selector",
            "      SimpleSelector",
            "        IdentLiteral : p")),
        true);
    assertMessage(
        true, MessageType.EXPECTED_TOKEN, MessageLevel.WARNING,
        MessagePart.Factory.valueOf(";"), MessagePart.Factory.valueOf("#"));
    assertMessage(true, MessageType.SKIPPING, MessageLevel.WARNING);
    assertMessagesLessSevereThan(MessageLevel.WARNING);
  }

  private void runTolerantParsing(
      String cssFile, String goldenFile, String snippetFile) throws Exception {
    InputSource inputSource = new InputSource(
        TestUtil.getResource(getClass(), cssFile));
    runTestCssParser(cssFile, goldenFile, true);
    assertEquals(
        TestUtil.readResource(getClass(), snippetFile).trim(),
        renderSnippets(
            Collections.singletonMap(
                inputSource,
                TestUtil.readResource(getClass(), cssFile))));
  }

  private void runTestCssParser(
      String cssFile, String goldenFile, boolean tolerant)
      throws Exception {
    runTestCssParser(fromResource(cssFile), fromResource(goldenFile), tolerant);
  }

  private void runTestCssParser(
      CharProducer css, CharProducer golden, boolean tolerant)
      throws Exception {
    MessageLevel lvl = tolerant
        ? MessageLevel.WARNING : MessageLevel.FATAL_ERROR;
    CssTree.StyleSheet stylesheet = new CssParser(
        CssParser.makeTokenQueue(css.clone(), mq, false), mq, lvl)
        .parseStyleSheet();
    assertCloneable(stylesheet);
    StringBuilder sb = new StringBuilder();
    stylesheet.format(new MessageContext(), sb);
    assertEquals(golden.toString().trim(), sb.toString().trim());

    if (!tolerant) {
      // run in tolerant mode to make sure being tolerant doesn't introduce
      // failures.
      runTestCssParser(css, golden.clone(), true);
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

  private String renderSnippets(Map<InputSource, String> srcs) {
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

  private void assertFilePosition(
      int startCharInFile, int endCharInFile, ParseTreeNode n) {
    FilePosition pos = n.getFilePosition();
    assertEquals("source", is, pos.source());
    assertEquals("start of " + pos, startCharInFile, pos.startCharInFile());
    assertEquals("end of " + pos, endCharInFile, pos.endCharInFile());
  }
}
