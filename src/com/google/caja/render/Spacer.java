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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Accumulates tokens inserting whitespace tokens where appropriate for
 * aesthetic reasons.  These whitespace tokens may later be turned into
 * newlines by an {@link Indenter}.
 *
 * @author mikesamuel@gmail.com
 */
class Spacer {
  /** The last position marked. */
  private FilePosition mark;
  /** The classification of the last non-space/comment token. */
  private TokenClassification lastClass;
  /** The last non-space/comment token. */
  private String lastToken;
  /** True if the last token needs a following space. */
  private boolean pendingSpace = false;
  /** The end line number of the last token seen. */
  private int lastLine = 1;

  private final List<String> outputTokens = new ArrayList<String>();

  List<String> getOutputTokens() { return outputTokens; }

  void processMark(FilePosition mark) { this.mark = mark; }

  void processToken(String text) {
    TokenClassification tClass = TokenClassification.classify(text);
    if (tClass == null) { return; }
    switch (tClass) {
      case LINEBREAK:
        // Allow external code to force line-breaks.
        // This allows us to create a composite-renderer that renders
        // original source code next to translated source code.
        emit("\n");
        return;
      case SPACE:
        pendingSpace = true;
        return;
      case COMMENT:
        if (mark != null && lastLine != mark.startLineNo()) {
          newline();
          lastLine = mark.startLineNo();
        } else if ("/".equals(lastToken) || pendingSpace) {
          space();
        }
        pendingSpace = false;
        emit(text);
        if (text.startsWith("//")) {
          newline();
          pendingSpace = false;
        } else {
          pendingSpace = true;
        }
        return;
      default: break;
    }

    boolean spaceBefore = pendingSpace;
    pendingSpace = false;
    boolean spaceAfter = false;

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

    if (tClass == TokenClassification.OTHER) {
      if ("}".equals(lastToken)) {
        spaceBefore = true;
      }
      if (isKeyword(text.toString())) {
        // Put a space between if and other keywords and the parenthesis.
        spaceAfter = true;
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
            } else if (!(")".equals(lastToken) || "=".equals(lastToken))) {
              // If starting a block following a parenthesized condition, or
              // an object literal assigned.
              spaceBefore = !("(".equals(lastToken) || "[".equals(lastToken));
            }
          }
          spaceAfter = true;
          break;
        case '[':
          if (")".equals(lastToken)) {
            spaceBefore = false;
          }
          spaceAfter = true;
          break;
        case '(':
          if (")".equals(lastToken)) {  // Calling a parenthesized value.
            spaceBefore = false;
          }
          break;
        case '}':
          spaceBefore = !"{".equals(lastToken);
          spaceAfter = true;
          break;
        case ')':
          spaceBefore = false;
          spaceAfter = true;
          break;
        case ']':
          spaceBefore = !"}".equals(lastToken);
          spaceAfter = true;
          break;
        case ',':
          spaceBefore = false;
          spaceAfter = true;
          break;
        case ';':
          spaceBefore = false;
          spaceAfter = true;
          break;
        case ':':
          spaceBefore = ":".equals(lastToken);  // Since :: is a token in ES4
          spaceAfter = true;
          break;
        case '=':
          spaceBefore = true;
          spaceAfter = true;
          break;
        case '.':
          spaceBefore = lastToken != null
              && (TokenClassification.isNumber(lastToken)
                  || ".".equals(lastToken));
          spaceAfter = false;
          break;
      }
    }

    // Write any whitespace before the token.
    if (spaceBefore) { space(); }

    // Actually write the token.
    emit(text);

    pendingSpace = spaceAfter;

    lastClass = tClass;
    lastToken = text;
    if (mark != null) { lastLine = mark.startLineNo(); }
  }

  private static final Set<String> KEYWORDS = new HashSet<String>();
  static {
    for (Keyword kw : Keyword.values()) {
      KEYWORDS.add(kw.toString());
    }
  }

  private static boolean isKeyword(String s) {
    return KEYWORDS.contains(s);
  }

  private void emit(String s) {
    outputTokens.add(s);
  }

  private void newline() {
    if (outputTokens.isEmpty()) { return; }
    int lastIdx = outputTokens.size() - 1;
    String last = outputTokens.get(lastIdx);
    if (" ".equals(last)) {
      outputTokens.set(lastIdx, "\n");
    } else if (!"\n".equals(last)) {
      emit("\n");
    }
  }

  private void space() {
    if (outputTokens.isEmpty()) { return; }
    String last = outputTokens.get(outputTokens.size() - 1);
    if (!("\n".equals(last) || " ".equals(last))) { emit(" "); }
  }
}
