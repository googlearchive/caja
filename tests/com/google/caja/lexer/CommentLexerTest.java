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

package com.google.caja.lexer;

import java.net.URI;
import java.util.List;

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class CommentLexerTest extends TestCase {

  public void testGetCommentBody() {
    assertEquals("foo", CommentLexer.getCommentBody("//foo"));
    assertEquals("foo", CommentLexer.getCommentBody("/*foo*/"));
    assertEquals("foo", CommentLexer.getCommentBody("/**foo*/"));
    assertEquals("foo", CommentLexer.getCommentBody("/*foo**/"));
    assertEquals("", CommentLexer.getCommentBody("//"));
    assertEquals("", CommentLexer.getCommentBody("/**/"));
    assertEquals("", CommentLexer.getCommentBody("/***/"));
    assertEquals(" * ", CommentLexer.getCommentBody("/** * */"));
  }

  public void testIsDirective() {
    assertTrue(CommentLexer.isDirective("/*!*/"));
    assertTrue(CommentLexer.isDirective("/*!foo*/"));
    assertTrue(CommentLexer.isDirective("/***!foo*/"));
    assertTrue(!CommentLexer.isDirective("/**/"));
    assertTrue(!CommentLexer.isDirective("/* */"));
    assertTrue(!CommentLexer.isDirective("/*foo*/"));
    assertTrue(!CommentLexer.isDirective("//foo/"));
    assertTrue(!CommentLexer.isDirective("// !foo/"));
    assertTrue(!CommentLexer.isDirective("/*** !foo*/"));
  }

  public void testSanitizeCommentBody() {
    assertEquals("  ", CommentLexer.sanitizeCommentBody("//"));
    assertEquals("  foo", CommentLexer.sanitizeCommentBody("//foo"));
    assertEquals("   foo", CommentLexer.sanitizeCommentBody("//!foo"));
    assertEquals("    ", CommentLexer.sanitizeCommentBody("/**/"));
    assertEquals("  foo  ", CommentLexer.sanitizeCommentBody("/*foo*/"));
    assertEquals("    foo  ", CommentLexer.sanitizeCommentBody("/**!foo*/"));
    assertEquals(
        "     foo bar\n" +
        "    bar / baz\n" +
        "    * 4\n" +
        "    ",
        CommentLexer.sanitizeCommentBody(
            "/**! foo bar\n" +
            " ** bar / baz\n" +
            " ** * 4\n" +
            " **/"));
  }

  public void testLexComment() throws Exception {
    String comment = (
        "/**! foo bar\n" +
        " ** bar / baz\n" +
        " ** * 4\n" +
        " **/");
    FilePosition pos = FilePosition.instance(
        new InputSource(URI.create("test:///CommentLexer")), 2, 2, 10, 0);
    JsTokenQueue tq =
      CommentLexer.lexComment(pos, comment, 0, comment.length());
    Token<JsTokenType> t;
    t = tq.pop();
    assertEquals("foo", t.text);
    assertEquals(2, t.pos.startLineNo());
    assertEquals(2, t.pos.startLogicalLineNo());
    assertEquals(15, t.pos.startCharInFile());
    assertEquals(5, t.pos.startCharInLine());
    assertTrue(tq.checkToken("bar"));
    assertTrue(tq.checkToken("bar"));
    assertTrue(tq.checkToken(Punctuation.SLASH));
    assertTrue(tq.checkToken("baz"));

    t = tq.peek();
    assertEquals("*", t.text);
    assertEquals(4, t.pos.startLineNo());
    assertEquals(4, t.pos.startLogicalLineNo());
    assertEquals(41, t.pos.startCharInFile());
    assertEquals(5, t.pos.startCharInLine());

    assertTrue(tq.checkToken(Punctuation.AST));
    assertTrue(tq.checkToken("4"));
    assertTrue(tq.isEmpty());
  }

  public void testSplitDocComment() {
    String docComment = (
        "/** hi there. \n" +
        "  * @param foo blah blah\n" +
        "  * @type blah blah @ 2 @ 3\n" +
        "  * @foo bar\n" +
        "  */");
    List<CommentLexer.DocChunk> chunks =
      CommentLexer.splitDocComment(docComment);
    CommentLexer.DocChunk dc;

    dc = chunks.get(0);
    assertSame(null, dc.getName());
    assertEquals("/** hi there. \n  * ",
        docComment.substring(dc.getStartOffset(), dc.getEndOffset()));
    dc = chunks.get(1);
    assertEquals("@param", dc.getName());
    assertEquals("@param foo blah blah\n  * ",
        docComment.substring(dc.getStartOffset(), dc.getEndOffset()));
    dc = chunks.get(2);
    assertEquals("@type", dc.getName());
    assertEquals("@type blah blah @ 2 @ 3\n  * ",
        docComment.substring(dc.getStartOffset(), dc.getEndOffset()));
    dc = chunks.get(3);
    assertEquals("@foo", dc.getName());
    assertEquals("@foo bar\n  */",
        docComment.substring(dc.getStartOffset(), dc.getEndOffset()));

    assertEquals(4, chunks.size());
  }
}
