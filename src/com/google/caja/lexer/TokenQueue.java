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

import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.util.Criterion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A queue of tokens extracted from a Lexer and a bunch of convenience
 * methods for parsing.
 *
 * @author mikesamuel@gmail.com
 */
public class TokenQueue<T extends TokenType> {
  private final TokenStream<T> tstream;
  private final InputSource file;
  /** Null or the range of file that is being parsed. */
  private FilePosition inputRange;
  private final Criterion<Token<T>> tokenFilter;

  private TokenList<T> current, prev;

  private boolean eof = false;

  public TokenQueue(TokenStream<T> tokenStream, InputSource file,
                    Criterion<Token<T>> tokenFilter) {
    this.tstream = tokenStream;
    this.file = file;
    this.tokenFilter = tokenFilter;
  }

  public TokenQueue(TokenStream<T> tokenStream, InputSource file) {
    this(tokenStream, file, Criterion.Factory.<Token<T>>optimist());
  }

  public InputSource getInputSource() { return this.file; }

  /** Null or the range within the file which is being parsed. */
  public FilePosition getInputRange() { return this.inputRange; }
  public void setInputRange(FilePosition range) { this.inputRange = range; }
  public Criterion<Token<T>> getTokenFilter() { return this.tokenFilter; }

  /** True iff there are no more tokens on the queue. */
  public boolean isEmpty() throws ParseException {
    fetch(false);
    return null == current;
  }

  /** Throws a ParseException if the queue is not empty. */
  public void expectEmpty() throws ParseException {
    if (!isEmpty()) {
      throw new ParseException(
          new Message(MessageType.UNUSED_TOKENS,
              currentPosition(), MessagePart.Factory.valueOf(peek().text)));
    }
  }

  /**
   * Ensures that there is a token on the queue ready for fetching.
   *
   * @throws ParseException if there is an error parsing a token or if the
   *   end of file has been reached.
   * @see MessageType#END_OF_FILE
   */
  private void fetch(boolean failOnEof) throws ParseException {
    if (null != current) { return; }

    List<Token<T>> filtered = null;
    Token<T> t = null;

    if (!eof) {
      while (tstream.hasNext()) {
        t = tstream.next();
        if (tokenFilter.accept(t)) { break; }
        if (null == filtered) { filtered = new ArrayList<Token<T>>(); }
        filtered.add(t);
        t = null;
      }
    }

    if (null == t) {
      eof = true;
      if (failOnEof) {
        throw new ParseException(
            new Message(MessageType.END_OF_FILE,
                        (null != inputRange ? this.inputRange : this.file)));
      }
      return;
    }

    TokenList<T> tl = new TokenList<T>();
    tl.t = t;
    tl.filteredTokens = null != filtered
        ? Collections.<Token<T>>unmodifiableList(filtered)
        : Collections.<Token<T>>emptyList();
    current = tl;
    if (null != prev) { prev.next = tl; }
  }

  /** Advance to the next token. */
  public void advance() throws ParseException {
    fetch(true);
    prev = current;
    current = current.next;
  }

  /** Fetch the current token. */
  public Token<T> peek() throws ParseException {
    fetch(true);
    return current.t;
  }

  /** Fetch the current token and advance to the next token. */
  public Token<T> pop() throws ParseException {
    Token<T> t = peek();
    advance();
    return t;
  }

  /**
   * The list of filtered between the current token and the last non filtered
   * token.
   * For example, if using the {@link JsTokenQueue#NO_COMMENT} filter,
   * this method will return a list of
   * {@link JsTokenType#COMMENT comment tokens}.
   */
  public List<Token<T>> filteredTokens() throws ParseException {
    fetch(true);
    return current.filteredTokens;
  }

  /**
   * Fetches an object that represents a token position, so that this token
   * queue can be "rewound" to the current position later on.
   */
  public Mark mark() throws ParseException {
    if (null == this.current && null == this.prev) {
      fetch(true);
    }
    // tokens from prev on will not be garbage collectible until the Mark
    // object is garbage collectible.
    return new Mark(this);
  }

  /**
   * Restores the current token position to the position when the given mark
   * was created.
   * @see #mark
   */
  @SuppressWarnings("unchecked")
  public void rewind(Mark m) {
    if (m.tq != this) { throw new IllegalStateException(); }
    // Since Mark was created from this tokenqueue, we know that it is typesafe
    // to assign savedPrev and savedCurrent, which warrants
    // @SuppressWarnings above.
    this.prev = (TokenList<T>) m.savedPrev;
    if (null != this.prev) {
      this.current = this.prev.next;
    } else {
      this.current = (TokenList<T>) m.savedCurrent;
    }
  }

  /**
   * Allows rewinding to a known position in the token queue.
   *
   * @see TokenQueue#mark
   */
  public static class Mark {
    final TokenList<?> savedCurrent, savedPrev;
    final TokenQueue<?> tq;

    Mark(TokenQueue<?> tq) {
      this.tq = tq;
      this.savedCurrent = tq.current;
      this.savedPrev = tq.prev;
     }

    public FilePosition getFilePosition() throws ParseException {
      Mark endMark = tq.mark();
      tq.rewind(this);
      try {
        if (tq.isEmpty()) {
          return FilePosition.endOf(tq.lastPosition());
        } else {
          return tq.currentPosition();
        }
      } finally {
        tq.rewind(endMark);
      }
    }
  }

  public FilePosition currentPosition() throws ParseException {
    return peek().pos;
  }

  public FilePosition lastPosition() {
    return prev != null ? prev.t.pos : null;
  }

  /**
   * Pops the current token iff it matches the given text.
   * @return true iff the current token matched text.
   */
  public boolean checkToken(String text) throws ParseException {
    if (isEmpty()) { return false; }
    if (peek().text.equals(text)) {
      advance();
      return true;
    }
    return false;
  }

  /**
   * Pops the current token if it matches the given text, but
   * raises a ParseException otherwise.
   */
  public void expectToken(String text) throws ParseException {
    Token<T> t;
    try {
      t = peek();
    } catch (ParseException ex) {
      if (prev != null
          && ex.getCajaMessage().getMessageType() == MessageType.END_OF_FILE) {
        throw new ParseException(
            new Message(MessageType.EXPECTED_TOKEN,
                        FilePosition.endOf(prev.t.pos),
                        MessagePart.Factory.valueOf(text),
                        MessagePart.Factory.valueOf("EOF")));
      }
      throw ex;
    }
    if (t.text.equals(text)) {
      advance();
      return;
    }

    throw new ParseException(
        new Message(MessageType.EXPECTED_TOKEN, t.pos,
                    MessagePart.Factory.valueOf(text),
                    MessagePart.Factory.valueOf(t.text)));
  }

  /**
   * Returns true iff the current token matches the given text.
   * @return true iff the current token matched text.
   */
  public boolean lookaheadToken(String text) throws ParseException {
    return !isEmpty() && peek().text.equals(text);
  }

  /**
   * Pops and returns the current token if it has the given type.
   * @throws ParseException if the current token doesn't have the given type
   *     or if there was an error fetching a token.
   */
  public Token<T> expectTokenOfType(T tt) throws ParseException {
    Token<T> t = peek();
    if (t.type == tt) {
      advance();
      return t;
    }
    throw new ParseException(
        new Message(MessageType.EXPECTED_TOKEN, t.pos,
                    MessagePart.Factory.valueOf(tt.toString()),
                    MessagePart.Factory.valueOf(t.text)));
  }

  /**
   * A singly linked list of tokens.
   * The elements of this list will become garbage colltible as they are
   * skipped past and the Marks that allow rewinding to earlier positions
   * are discarded.
   */
  private static class TokenList<TT extends TokenType> {
    Token<TT> t;
    TokenList<TT> next;
    List<Token<TT>> filteredTokens;
  }
}
