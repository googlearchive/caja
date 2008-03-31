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

package com.google.caja.render;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.util.Callback;

import java.io.Flushable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A formatter that indents code for a C-style language with statement delimited
 * defined by curly brackets, and expression blocks delimited by square brackets
 * and parentheses.
 *
 * @author mikesamuel@gmail.com
 */
public final class JsPrettyPrinter implements TokenConsumer {
  private final Appendable out;
  private final Callback<IOException> ioExceptionHandler;
  /**
   * Stack of indentation positions.
   * Curly brackets indent to two past the last stack position and
   * parenthetical blocks indent to the open parenthesis.
   */
  private List<Indent> indentStack = new LinkedList<Indent>(
      Arrays.asList(new Indent(0, false)));
  /** Number of characters written to out since the last linebreak. */
  private int charInLine;

  /** The end line number of the last token seen. */
  private int lastLine = 1;
  /** The last position marked. */
  private FilePosition mark;
  /** The classification of the last non-space/comment token. */
  private TokenClassification lastClass;
  /** The last non-space/comment token. */
  private String lastToken;
  /** True if the last token needs a following space. */
  private char pendingSpace = '\0';
  /** True if an IOException has been raised. */
  private boolean closed;

  /**
   * @param out receives the rendered text.
   * @param ioExceptionHandler receives exceptions thrown by out.
   */
  public JsPrettyPrinter(
      Appendable out, Callback<IOException> ioExceptionHandler) {
    this.out = out;
    this.ioExceptionHandler = ioExceptionHandler;
  }

  /** Flushes the underlying appendable. */
  public void noMoreTokens() {
    if (out instanceof Flushable) {
      try {
        ((Flushable) out).flush();
      } catch (IOException ex) {
        closed = true;
        if (!closed) {
          ioExceptionHandler.handle(ex);
        }
      }
    }
  }

  public void mark(FilePosition pos) {
    if (pos != null) {
      mark = pos;
    }
  }

  /**
   * @throws NullPointerException if out raises an IOException
   *     and ioExceptionHandler is null.
   */
  public void consume(String text) {
    if (closed) { return; }
    try {
      TokenClassification tClass = TokenClassification.classify(text);
      if (tClass == null) { return; }
      switch (tClass) {
        case LINEBREAK:
          deindentRecentlyOpenedParens();
          pendingSpace = '\0';

          // Allow external code to force linebreaks.
          // This allows us to create a composite-renderer that renders
          // original source code next to translated source code.
          emit("\n");
          return;
        case SPACE:
          pendingSpace = ' ';
          return;
        case COMMENT:
          if (mark != null && lastLine != mark.startLineNo()
              && charInLine != 0) {
            newLine();
            lastLine = mark.startLineNo();
          } else if ("/".equals(lastToken) || pendingSpace != '\0') {
            space();
          }
          indent(0);
          emit(text);
          if (text.startsWith("//")) {
            newLine();
          } else {
            pendingSpace = '\n';
          }
          return;
      }

      boolean spaceBefore = false, breakBefore = false;
      char spaceAfter = '\0';

      switch (pendingSpace) {
        case ' ': spaceBefore = true; break;
        case '\n': breakBefore = true; break;
      }
      pendingSpace = '\0';

      // Determine which pairs of tokens cannot be adjacent and put a space
      // between them.
      if (tClass == lastClass) {
        // Adjacent punctuation, strings, and words require space.
        // Numbers and words are both of type OTHER.
        // This decision may be revisited in the following to prevent
        // excessive space inside parentheses.
        spaceBefore = !"(".equals(lastToken);
      } else if (lastClass == TokenClassification.REGEX) {
        if (tClass == TokenClassification.OTHER || "/".equals(text)) {
          // Make sure words don't run into regex flags, and that / operator
          // does not combine with end of regex to make a line comment.
          spaceBefore = true;
        }
      } else if (tClass == TokenClassification.REGEX && "/".equals(lastToken)) {
        // Allowing these two tokens to run together could introduce a line
        // comment.
        spaceBefore = true;
      } else if (tClass == TokenClassification.OTHER
                 && Character.isDigit(text.charAt(0))
                 && ".".equals(lastToken)) {
        // Following a dot operator with a number is illegal syntactically, but
        // this renderer should not allow any lexical confusion.
        spaceBefore = true;
      }

      int indentOffset = 0;
      if (tClass == TokenClassification.OTHER) {
        if ("}".equals(lastToken)
            && ("else".equals(text) || "while".equals(text)
                || "catch".equals(text) || "finally".equals(text))) {
          // handle "{ } if" and "{ } label: { }" and "} else {"
          spaceBefore = true;
          breakBefore = false;
        }
        if (isKeyword(text.toString())) {
          // Put a space between if and other keywords and the parenthesis.
          spaceAfter = ' ';
          if ("default".equals(text) || "case".equals(text)) {
            indentOffset = -2;
          }
        }
      }

      // If this token is an open bracket, we want to indent, but not before
      // writing the token to avoid over-indenting the open bracket.
      if (text.length() == 1) {
        char ch0 = text.charAt(0);
        switch (ch0) {
          case '{':
            if (lastClass == TokenClassification.PUNCTUATION) {
              if (":".equals(lastToken)) {  // See JSON test.
                spaceBefore = true;
                breakBefore = false;
              } else if (!(")".equals(lastToken) || "=".equals(lastToken))) {
                // If starting a block following a parenthesized condition, or
                // an object literal assigned.
                spaceBefore = false;
              }
            }
            spaceAfter = '\n';
            break;
          case '[':
            if (")".equals(lastToken)) {
              spaceBefore = false;
            }
            spaceAfter = ' ';
            break;
          case '(':
            if (")".equals(lastToken)) {  // Calling a parenthesized value.
              spaceBefore = false;
            }
            break;
          case '}':
            breakBefore = true;
            spaceAfter = '\n';
            popIndentStack();
            setInStatement(false);
            break;
          case ')':
            spaceBefore = breakBefore = false;
            spaceAfter = ' ';
            popIndentStack();
            break;
          case ']':
            breakBefore = false;
            spaceBefore = !"}".equals(lastToken);
            spaceAfter = ' ';
            popIndentStack();
            break;
          case ',':
            spaceBefore = breakBefore = false;
            spaceAfter = ' ';
            break;
          case ';':
            spaceBefore = false;
            if (!";".equals(lastToken)) {
              breakBefore = false;
            }
            spaceAfter = indentStack.get(0).parenthetical ? ' ' : '\n';
            break;
          case ':':
            spaceBefore = ":".equals(lastToken);  // Since :: is a token in ES4
            spaceAfter = ' ';
            break;
          case '=':
            spaceBefore = true;
            spaceAfter = ' ';
            break;
          case '.':
            spaceBefore = lastToken != null
                && (TokenClassification.isNumber(lastToken)
                    || ".".equals(lastToken));
            spaceAfter = '\0';
            break;
        }
      }

      // Write any whitespace before the token.
      if (breakBefore) {
        newLine();
      } else if (spaceBefore) {
        space();
      }
      indent(indentOffset);

      // Apply any indentation from an open bracket.
      if (text.length() == 1) {
        char ch0 = text.charAt(0);
        switch (ch0) {
          case '(': case '[':
            pushIndent(new Indent(charInLine + 1, true));
            break;
          case '{':
            pushIndent(new Indent(getIndentation() + 2, false));
            break;
        }
      }

      // Actually write the token.
      emit(text);

      pendingSpace = spaceAfter;

      if (text.length() == 1) {
        switch (text.charAt(0)) {
          case '{': case '}': case ':': case ';': case ',':
            setInStatement(false);
            break;
          default:
            setInStatement(true);
        }
      } else {
        setInStatement(true);
      }

      lastClass = tClass;
      lastToken = text;
      if (mark != null) { lastLine = mark.startLineNo(); }
    } catch (IOException ex) {
      closed = true;
      ioExceptionHandler.handle(ex);
    }
  }

  private int getIndentation() {
    return indentStack.get(0).spaces;
  }

  private void pushIndent(Indent sframe) {
    indentStack.add(0, sframe);
  }

  private void popIndentStack() {
    if (indentStack.size() > 1) {
      indentStack.remove(0);
    }
  }

  private void deindentRecentlyOpenedParens() {
    // When a newline occurs immediately after an open parenthesis, don't
    // indent to the parenthesis, but indent 4.
    // Instead of
    //     var myVar = myObject.functionName(
    //                                       param1, param2, param3, param4)
    // indent as
    //     var myVar = myObject.functionName(
    //         param1, param2, param3, param4)
    int pos = charInLine;
    int nFrames = 0;
    while (pos > 0 && indentStack.size() > nFrames + 1) {
      Indent top = indentStack.get(nFrames);
      if (!(top.parenthetical && top.spaces == pos)) {
        break;
      }
      --pos;  // Consider the parenthesis to the left next.
      ++nFrames;
    }
    int spaces = indentStack.get(nFrames).spaces + 4;
    for (int i = 0; i < nFrames; ++i) {
      indentStack.get(i).spaces = spaces;
    }
  }

  private void setInStatement(boolean inStatement) {
    indentStack.get(0).inStatement = inStatement;
  }

  private static final Set<String> KEYWORDS = new HashSet<String>();
  static {
    for (Keyword kw : Keyword.values()) {
      KEYWORDS.add(kw.toString());
    }
  }

  private boolean isKeyword(String s) {
    return KEYWORDS.contains(s);
  }

  private void indent(int offset) throws IOException {
    if (charInLine != 0) { return; }
    Indent sframe = indentStack.get(0);
    int nSpaces = Math.max(0, sframe.spaces + offset);
    if (!sframe.parenthetical && sframe.inStatement) { nSpaces += 4; }

    charInLine += nSpaces;
    String spaces = "                ";
    while (nSpaces >= spaces.length()) {
      out.append(spaces);
      nSpaces -= spaces.length();
    }
    out.append(spaces, 0, nSpaces);
  }

  private void newLine() throws IOException {
    if (charInLine == 0) { return; }
    charInLine = 0;
    out.append("\n");
  }

  private void space() throws IOException {
    if (charInLine != 0) {
      out.append(" ");
      ++charInLine;
    }
  }

  private void emit(CharSequence s) throws IOException {
    out.append(s);
    int n = s.length();
    // Look backwards for a linebreak so we can keep charInLine up-to-date.
    for (int i = n; --i >= 0;) {
      char ch = s.charAt(i);
      if (ch == '\r' || ch == '\n') {  // Using CharProducer's linebreak def.
        charInLine = n - i - 1;
        return;
      }
    }
    charInLine += n;
  }

  private static class Indent {
    int spaces;
    /**
     * Are we in a () or [] block.
     * Semicolons are not treated as statement separators in that context.
     */
    final boolean parenthetical;
    /**
     * True iff we are in a statement.
     * E.g. in {@code
     *    var foo = x
     *        + y;
     * }
     * the <code>+</code> is indented past the beginning of the previous line
     * because we have not yet seen a semicolon.
     */
    boolean inStatement;

    Indent(int spaces, boolean parenthetical) {
      this.spaces = spaces;
      this.parenthetical = parenthetical;
    }
  }
}
