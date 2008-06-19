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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Lexes a comment, and extracts JSDoc/JavaDoc style directives.
 *
 * @author mikesamuel@gmail.com
 */
public final class CommentLexer {

  private CommentLexer() {
    // not instantiable
  }

  /**
   * <code>/<span>*foo*</span>/</code> -&gt; <code>foo</code>.
   * Strips the start and end markers (double slash or slash star) from
   * the string and return the rest.
   * If a slash star comment, then strips any runs of asterisks at the
   * beginning and end as well, so <code>/<span>**foo**</span>/</code>
   * -&gt; <code>foo</code>.
   * @param s non null.
   * @return a substring of s.
   */
  public static String getCommentBody(String s) {
    int start = 2;
    int end = s.length();
    boolean isStar = '*' == s.charAt(1);
    if (isStar) {
      end -= 2;
      while (start < end && '*' == s.charAt(start)) { ++start; }
      while (end > start && '*' == s.charAt(end - 1)) { --end; }
    }
    return s.substring(start, end);
  }

  /**
   * True iff the given string is a comment and is a directive: a comment
   * of the form <code>/<span>*! <i>[Directive]</i> *</span>/</code> or
   * <code>//! <i>[Directive]</i></code>.
   */
  public static boolean isDirective(String comment) {
    return getCommentBody(comment).startsWith("!");
  }

  /**
   * Whites out all the non-directive characters in a directive comment while
   * preserving the character offsets of all directive characters.
   */
  public static String sanitizeCommentBody(String s) {
    StringBuilder sb = new StringBuilder(s);

    boolean isStar = '*' == sb.charAt(1);
    sb.setCharAt(0, ' ');
    sb.setCharAt(1, ' ');
    int n = sb.length();

    int start = 2;
    int end = n;
    if (isStar) {
      sb.setCharAt(--n, ' ');
      sb.setCharAt(--n, ' ');
      while (start < end && '*' == sb.charAt(start)) {
        sb.setCharAt(start++, ' ');
      }
      while (end > start && '*' == sb.charAt(end - 1)) {
        sb.setCharAt(--end, ' ');
      }
    }

    if (start < end && sb.charAt(start) == '!') {
      sb.setCharAt(start++, ' ');
    }

    // Strip first run of asterisks as per jsdoc conventions.
    // JsDoc follows the javadoc comment format.
    // From http://java.sun.com/j2se/1.3/docs/tooldocs/win32/javadoc.html
    //      #documentationcomments
    //   The tag section starts with the first character @ that begins a line
    //   (ignoring leading asterisks, white space and comment separator).
    int state = 0; // 0 - in content, 1 - saw newline, 2 - in * run
    for (int i = start; i < end; ++i) {
      final char ch = sb.charAt(i);
      if (JsLexer.isJsLineSeparator(ch)) {
        state = 1;
      } else {
        switch (state) {
          case 1:
            if ('*' == ch) {
              sb.setCharAt(i, ' ');
              state = 2;
            } else if (!Character.isWhitespace(ch)) {
              state = 0;
            }
            break;
          case 2:
            if ('*' == ch) {
              sb.setCharAt(i, ' ');
            } else {
              state = 0;
            }
            break;
        }
      }
    }
    return sb.toString();
  }

  /**
   * Returns a tokenqueue that parses the content of comment as
   * a stream of javascript source tokens.
   * @param commentStart the start position of comment within a source
   *     file.  non null.
   * @param comment the comment text.  non null.
   * @param startOffset in [0, comment.length()]
   * @param endOffset in [startOffset, comment.length()]
   */
  public static JsTokenQueue lexComment(
      FilePosition commentStart, String comment,
      int startOffset, int endOffset) {
    comment = sanitizeCommentBody(comment);

    FilePosition chunkStart;
    if (0 == startOffset) {
      chunkStart = FilePosition.startOf(commentStart);
    } else {
      int ln = commentStart.startLineNo(),
         lln = commentStart.startLogicalLineNo(),
         cif = commentStart.startCharInFile(),
         cil = commentStart.startCharInLine();

      int last = -1;
      char ch;
      for (int i = startOffset; i < endOffset; ++i, last = ch, ++cif) {
        ch = comment.charAt(i);
        if (JsLexer.isJsLineSeparator(ch)) {
          if (last != '\r') {
            ++ln;
            ++lln;
          }
          cil = 0;
        }
      }

      chunkStart = FilePosition.instance(
          commentStart.source(), ln, lln, cif, cil);
    }

    JsLexer l = new JsLexer(new StringReader(comment), chunkStart);
    return new JsTokenQueue(
        l, chunkStart.source(), JsTokenQueue.NO_COMMENT);
  }

  /**
   * Splits a doc comment into parts, so
   * <code>"/*<span>* foo.  @param bar *</span>/"</code> - &gt;
   * <code>[ (null, "/*<span>* foo.  "),
   *   ("@param", "@param bar *</span>/") ]</code>.
   * @param commentBody a commentBody, sanitized or unsanitized.  non null.
   * @return a mutable list of DocChunks.
   */
  public static List<DocChunk> splitDocComment(String commentBody) {
    List<DocChunk> chunks = new ArrayList<DocChunk>();
    int pos = 0;
    int chunked = 0;

    int n = commentBody.length();
    for (int at; (at = commentBody.indexOf("@", pos)) >= 0;) {
      if (at + 1 < n
          && Character.isLetterOrDigit(commentBody.charAt(at + 1))) {
        chunks.add(new DocChunk(commentBody, chunked, at));
        chunked = at;
      }
      pos = at + 1;
    }
    if (chunked < n) {
      chunks.add(new DocChunk(commentBody, chunked, n));
    }
    return chunks;
  }

  /** A chunk of a documentation comment. */
  public static final class DocChunk {
    private String name;
    private int startOffset, endOffset;

    public DocChunk(String body, int startOffset, int endOffset) {
      this.startOffset = startOffset;
      this.endOffset = endOffset;

      int n = endOffset - startOffset;
      if (n >= 2 && '@' == body.charAt(startOffset)
          && Character.isLetterOrDigit(body.charAt(startOffset + 1))) {
        int end = startOffset + 2;
        while (end < endOffset
               && Character.isLetterOrDigit(body.charAt(end))) {
          ++end;
        }

        this.name = body.substring(startOffset, end);
      } else {
        this.name = null;
      }
    }

    public String getName() { return this.name; }
    public int getEndOffset() { return this.endOffset; }
    public int getStartOffset() { return this.startOffset; }

    @Override
    public String toString() {
      return "[DocChunk " + (name != null ? name + " " : "") +
        startOffset + ":" + endOffset + "]";
    }
  }
}
