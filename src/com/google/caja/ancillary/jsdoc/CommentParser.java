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

package com.google.caja.ancillary.jsdoc;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.TokenStream;
import com.google.caja.lexer.TokenType;

/**
 * Provides a means to decompose a structured JSDoc/Javadoc comment.
 *
 * <h3> Glossary </h3>
 * <dl>
 *   <dt>Structured Comment</dt>
 *     <dd>A JSDoc/Javadoc style comment which contains annotations meant for
 *     human consumption, and for machine consumption.</dd>
 *   <dt>Annotation</dt>
 *     <dd>A key-value pair, such as ['see', 'SomeOtherRelatedClass'].</dd>
 *   <dt>Block Annotation</dt>
 *     <dd>An annotation that runs until the next '&#64;' symbol not contained
 *     in an inline annotation.  Block annotations are discontinguous and may
 *     contain inline annotations.</dd>
 *   <dt>Inline Annotation</dt>
 *     <dd>An annotation that can appear inside a block annotation, and that
 *     is delimited by curly brackers, such as JavaDoc's 'code' annotation.
 *     Since it is a goal to allow simple javascript code, such as object
 *     literal examples of argument maps, and markup examples, an inline
 *     annotation can contain nested curly brackets.  Nesting is determined
 *     using JSON lexical conventions, but expanded to allow single quoted
 *     strings.</dd>
 *   <dt>Structured Comment Body</dt>
 *     <dd>Freeform text in a comment that is outside any annotation.</dd>
 *   <dt>Doclet</dt>
 *     <dd>Code that consumes structured comments.</dd>
 * </dl>
 *
 *
 * <h3> Goals </h3>
 * <ol>
 *   <li>Comments should be readable both in code and in rendered documentation.
 *
 *   <li>A way of parsing structured comments that is extensible -- a doclet
 *   must be able to parse a structured comment without knowing the full
 *   set of annotations that appear in those comments.
 *
 *   <li>Allow embedding of simple javascript code and XML to allow embedding
 *   of simple input and output that is readable in both code and in generated
 *   documentation, and allow embedding of extractable tests and assertions.
 * </ol>
 *
 *
 * <h3> Conventions </h3>
 * <p>An annotation name is '&#64;' followed by a letter followed by any
 * sequence of '_', and latin-1 alphanumerics.
 *
 * <p>Any '&#64;' sign followed by a unicode letter and not preceded by '{'
 * starts a block annotation, that ends at the first of (end of comment, an
 * '&#64;' outside an inline annotation that is followed by a unicode letter.
 * </p>
 *
 * <p>Any '{&#64;' followed by a unicode letter starts an inline annotation.
 * The inline annotation ends after a '}' is seen without a corresponding '{'
 * using the lexical grammar defined below.</p>
 *
 * <p>At the start of the line, a single run of whitespace followed by a single
 * run of asterisks followed by an optional single whitespace character is
 * ignored, per JavaDoc conventions.
 *
 * <p>A doclet may ignore or report errors on any annotations it does not
 * recognize.
 *
 * <p>The content of an annotation is HTML, and so can contain any of the
 * numeric entity escapes, such as &amp;#64; for '&#64;', or the basic XML
 * escapes such as &amp;lt;.  Other escapes must not be used.</p>
 *
 *
 * <h3> Known Issues </h3>
 * <p>Inline annotations differ from Javadoc 'code' annotations in that Javadoc
 * does not allow balanced curly brackets.  Since it is a goal to support
 * sample inputs, and many javascript functions take flexible parameters in
 * the form of JS object literals, support for balanced curly brackets inside
 * 'code' annotations is worth the added complexity.
 *
 * <p>Inline annotations do not allow arbitrary JS code.  JS's lexical
 * conventions are complicated by regular expression literals, and it is a non
 * goal to support comment nesting, so JSON+ seems to strike a good balance
 * between easily embedded sample code and implementation simplicity.
 *
 *
 * <h3> Grammar </h3>
 * <h4> Content Lines </h4>
 * <pre>
 * structured_comment  ::==  (content_line)+
 * content_line        ::==  (ignorable_prefix)? (content) (ignorable_suffix)?
 * ignorable_prefix    ::==  '/' '*'+ (space)?
 *                        |  (space)* '*'+ (space)?
 * ignorable_suffix    ::==  '*' '/'
 *                        |  '\r' '\n'? | '\n' | '\u2028' | '\u2029'
 * content              =~   [^\r\n\u2028\u2029]*
 * </pre>
 *
 *
 * <h4> Annotations </h4>
 * Applied to the concationation of content sections above -- the structured
 * comment with ignorable_prefixes and ignorable_suffixes removed.
 * <pre>
 * structured_comment  ::==  (body)? (block_annotation)*
 * body                ::==  (body_part)*
 * body_part           ::==  (inline_annotation)
 *                        |  simple_body_part
 * simple_body_part     =~   [^&#64;\{]
 *                      =~   \{(?=[^&#64;])
 *                      =~   \{?&#64;(?=[^a-zA-Z])
 * block_annotation    ::== '&#64;' (annotation_name) (body_part)*
 * inline_annotation   ::== '{' '&#64;' (annotation_name) (inline_body)* '}'
 * inline_body         ::== '{' (inline_body)* '}'
 *                        | (json_plus_token)
 * json_plus_token      =~  "([^\"\\]|\\.)*"
 *                      =~  '([^\'\\]|\\.)*'
 *                      =~  [^\"\'\{\}]+
 *                      =~  [\"\']           ; string literals are single-line
 * </pre>
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public final class CommentParser {
  /**
   * @param commentText a structured JSDoc style comment.
   */
  public static Comment parseStructuredComment(CharProducer commentText)
      throws ParseException {
    FilePosition pos = commentText.filePositionForOffsets(
        commentText.getOffset(), commentText.getLimit());
    TokenQueue<CommentTokenType> tq = new TokenQueue<CommentTokenType>(
        new Joiner(commentText), pos.source());
    tq.setInputRange(pos);
    CommentParser p = new CommentParser(tq);
    List<Annotation> body = new ArrayList<Annotation>();
    // A comment consists of some text,
    while (p.parseBodyPart(body)) {}
    // followed by block annotations.
    while (p.parseBlockAnnotation(body)) {}

    if (!p.tq.isEmpty()) {
      throw new IllegalStateException("Unprocessed '" + commentText + "'");
    }
    return new Comment(body, pos);
  }

  private final TokenQueue<CommentTokenType> tq;
  private CommentParser(TokenQueue<CommentTokenType> tq) {
    this.tq = tq;
  }

  /**
   * If there is a run of non annotation text at the front of commentText,
   * consume it and add a TextAnnotation to out.
   * @return true if anything was consumed.
   */
  private boolean parseBodyPart(List<Annotation> out) throws ParseException {
    if (tq.isEmpty()) { return false; }
    // A body is either text, or an inline annotation.
    Token<CommentTokenType> t = tq.peek();
    if (t.type == CommentTokenType.ANNOTATION_NAME) {
      if (t.text.charAt(0) == '{') {
        return parseInlineAnnotation(out);
      } else {
        return false;
      }
    }
    StringBuilder sb = new StringBuilder();
    FilePosition start = t.pos;
    do {
      sb.append(t.text);
      tq.advance();
    } while (!tq.isEmpty()
             && (t = tq.peek()).type != CommentTokenType.ANNOTATION_NAME);
    out.add(new TextAnnotation(
        sb.toString(), FilePosition.span(start, tq.lastPosition())));
    return true;
  }

  /**
   * Parse an inline annotation starting at a { through the corresponding }
   * and add it to out, returning true if any content was consumed.
   */
  private boolean parseInlineAnnotation(List<Annotation> out)
      throws ParseException {
    FilePosition start = tq.currentPosition();
    String name = tq.expectTokenOfType(CommentTokenType.ANNOTATION_NAME).text;
    TextAnnotation body = parseInlineBody();
    tq.expectToken("}");
    out.add(new InlineAnnotation(name.substring(2), body, posFrom(start)));
    return true;
  }

  /**
   * Parse the body of an inline annotation assuming the initial '{' has been
   * seen.
   */
  private TextAnnotation parseInlineBody() throws ParseException {
    FilePosition start = tq.currentPosition();
    StringBuilder inlineBody = new StringBuilder();
    // Append JSON tokens on inlineBody until we've seen a close '}' that
    // matches the '{' one seen in startInlineAnnotations.
    int bracketDepth = 1;  // Count of unclosed '{'
    bodyLoop:
    while (true) {
      if (tq.isEmpty()) { tq.expectToken("}"); }
      Token<CommentTokenType> t = tq.peek();
      switch (t.type) {
        case OPEN_CURLY: ++bracketDepth; break;
        case CLOSE_CURLY:
          if (--bracketDepth == 0) {
            break bodyLoop;
          }
          break;
          default: break;
      }
      inlineBody.append(t.text);
      tq.advance();
    }
    return new TextAnnotation(inlineBody.toString(), posFrom(start));
  }

  /**
   * Parse a block annotation if one is available, appending any annotation to
   * out and returning true if any content was consumed.
   */
  private boolean parseBlockAnnotation(List<Annotation> out)
      throws ParseException {
    if (tq.isEmpty() || tq.peek().type != CommentTokenType.ANNOTATION_NAME
        || tq.peek().text.startsWith("{")) {
      return false;
    }
    FilePosition start = tq.currentPosition();
    String name = tq.pop().text.substring(1);
    List<Annotation> body = new ArrayList<Annotation>();
    while (parseBodyPart(body)) {}
    out.add(new BlockAnnotation(name, body, posFrom(start)));
    return true;
  }

  private FilePosition posFrom(FilePosition start) {
    FilePosition end = tq.lastPosition();
    if (end.endCharInFile() <= start.startCharInFile()) {
      return FilePosition.startOf(start);
    }
    return FilePosition.span(start, end);
  }

  private static class Joiner implements TokenStream<CommentTokenType> {
    private final Filter ts;
    private final List<Token<CommentTokenType>> lookahead
        = new ArrayList<Token<CommentTokenType>>();
    private int bracketDepth = 0;
    Joiner(CharProducer cp) { ts = new Filter(cp); }

    private Token<CommentTokenType> pending;

    public boolean hasNext() {
      if (pending == null) { fetch(); }
      return pending != null;
    }
    public Token<CommentTokenType> next() {
      if (!hasNext()) { throw new NoSuchElementException(); }
      Token<CommentTokenType> next = pending;
      pending = null;
      return next;
    }
    private void fetch() {
      Token<CommentTokenType> t0 = peek(0);
      if (t0 == null) { return; }
      char ch0 = t0.text.charAt(0);
      if (bracketDepth != 0) {
        switch (ch0) {
          case '"': case '\'': {  // Process whole quoted string
            FilePosition start = t0.pos, end = t0.pos;
            StringBuilder sb = new StringBuilder();
            sb.append(t0.text);
            consume();
            while ((t0 = peek(0)) != null) {
              consume();
              sb.append(t0.text);
              end = t0.pos;
              if (ch0 == t0.text.charAt(0)) {
                int nEsc = 0;
                int len = sb.length() - 1;  // ignoring " just added
                while (nEsc < len && sb.charAt(len - nEsc - 1) == '\\') {
                  ++nEsc;
                }
                if ((nEsc & 1) == 0) {
                  break;
                }
              }
            }
            pending = Token.instance(
                sb.toString(), CommentTokenType.TEXT,
                FilePosition.span(start, end));
            break;
          }
          case '{':
            ++bracketDepth;
            consume();
            pending = Token.instance(
                t0.text, CommentTokenType.OPEN_CURLY, t0.pos);
            break;
          case '}':
            --bracketDepth;
            consume();
            pending = Token.instance(
                t0.text, CommentTokenType.CLOSE_CURLY, t0.pos);
            break;
          default:
            pending = t0;
            consume();
            break;
        }
      } else {
        switch (ch0) {
          case '{': {
            Token<CommentTokenType> t1 = peek(1);
            if (t1 != null && t1.text.charAt(0) == '@' && t1.text.length() > 1
                && Character.isLetter(t1.text.charAt(1))) {
              pending = Token.instance(
                  t0.text + t1.text, CommentTokenType.ANNOTATION_NAME,
                  FilePosition.span(t0.pos, t1.pos));
              consume(2);
              bracketDepth = 1;
              ignoreLeadingSpace();
            } else {
              pending = t0;
              consume();
            }
            break;
          }
          case '@': {
            if (t0.text.length() > 1 && Character.isLetter(t0.text.charAt(1))) {
              pending = Token.instance(
                  t0.text, CommentTokenType.ANNOTATION_NAME, t0.pos);
              consume();
              ignoreLeadingSpace();
            } else {
              pending = t0;
              consume();
            }
            break;
          }
          default:
            pending = t0;
            consume();
            break;
        }
      }
    }
    private Token<CommentTokenType> peek(int n) {
      while (n >= lookahead.size() && ts.hasNext()) {
        lookahead.add(ts.next());
      }
      return lookahead.size() > n ? lookahead.get(n) : null;
    }

    private void consume() {
      lookahead.remove(0);
    }

    private void consume(int n) { lookahead.subList(0, n).clear(); }

    private void ignoreLeadingSpace() {
      Token<CommentTokenType> t = peek(0);
      if (t != null) {
        char ch0 = t.text.charAt(0);
        if (ch0 == ' ' || ch0 == '\t') { consume(); }
      }
    }
  }

  private static class Filter implements TokenStream<CommentTokenType> {
    private final Splitter ts;
    Filter(CharProducer cp) { ts = new Splitter(cp); }
    private Token<CommentTokenType> pending;
    private State state = State.START_OF_FILE;

    public boolean hasNext() {
      if (pending == null) { fetch(); }
      return pending != null;
    }
    public Token<CommentTokenType> next() {
      if (!hasNext()) { throw new NoSuchElementException(); }
      Token<CommentTokenType> next = pending;
      pending = null;
      return next;
    }
    private void fetch() {
      pending = fetchNonIgnorable();
    }

    private enum State {
      START_OF_FILE,
      START_OF_LINE,
      SAW_ASTERISKS,
      IN_LINE,
      ;
    }

    private Token<CommentTokenType> fetchNonIgnorable() {
      while (ts.hasNext()) {
        Token<CommentTokenType> t = ts.next();
        char tch = t.text.charAt(0);
        if (tch == '/' && state == State.START_OF_FILE) {
          state = State.START_OF_LINE;
          continue;
        } else if (tch == '\n') {
          state = State.START_OF_LINE;
        } else if (tch == '*' && t.text.endsWith("/")) {
          continue;
        } else if (state != State.IN_LINE) {
          if (tch == '*') {
            state = State.IN_LINE;
            continue;
          } else if (tch == ' ' || tch == '*') {
            continue;
          } else {
            state = State.IN_LINE;
          }
        }
        return t;
      }
      return null;
    }
  }

  private static class Splitter implements TokenStream<CommentTokenType> {
    private final CharProducer cp;
    private Token<CommentTokenType> pending;

    Splitter(CharProducer cp) {
      this.cp = cp;
    }
    public boolean hasNext() {
      if (pending == null) { fetch(); }
      return pending != null;
    }
    public Token<CommentTokenType> next() {
      if (!hasNext()) { throw new NoSuchElementException(); }
      Token<CommentTokenType> next = pending;
      pending = null;
      return next;
    }
    private void fetch() {
      int start = cp.getOffset();
      int limit = cp.getLimit();
      if (start == limit) { return; }
      char[] buf = cp.getBuffer();
      char ch = buf[start];
      int end = start + 1;
      switch (ch) {
        case '\r':
        case '\n':
          if (ch == '\r' && end < limit && buf[end] == '\n') { ++end; }
          break;
        case '"': case '\'': case '/': case '{': case '}':
          break;
        case '@':
          while (end < limit && Character.isLetterOrDigit(buf[end])) {
            ++end;
          }
          break;
        case ' ': case '\t':
          while (end < limit && (buf[end] == ' ' || buf[end] == '\t')) {
            ++end;
          }
          break;
        case '*':
          while (end < limit && buf[end] == '*') { ++end; }
          if (end < limit) {
            switch (buf[end]) {
              case ' ': case '\t': case '/':
                ++end;
                break;
            }
          }
          break;
        default:
          tokenloop:
          while (end < limit) {
            switch (buf[end]) {
              case '{': case '}': case '*': case ' ':
              case '\t': case '\r': case '\n': case '"': case '\'':
                // allow '@' since it's not significant except after '{' or
                // space
                break tokenloop;
            }
            ++end;
          }
          break;
      }
      cp.consume(end - cp.getOffset());
      pending = Token.instance(
          normalizedCommentString(buf, start, end), CommentTokenType.TEXT,
          cp.filePositionForOffsets(start, end));
    }
  }

  private static String normalizedCommentString(char[] buf, int s, int e) {
    for (int i = s; i < e; ++i) {
      if (buf[i] == '\r') {
        StringBuilder sb = new StringBuilder(e - s);
        int pos = s;
        do {
          if ('\r' == buf[i]) {
            sb.append(buf, pos, i - pos).append('\n');
            pos = i + 1;
            if (pos < e && buf[pos] == '\n') { ++pos; }
          }
        } while (++i < e);
        sb.append(buf, pos, e - pos);
        return sb.toString();
      }
    }
    return String.valueOf(buf, s, e - s);
  }
}

enum CommentTokenType implements TokenType {
  ANNOTATION_NAME,
  OPEN_CURLY,
  CLOSE_CURLY,
  TEXT,
  ;
}
