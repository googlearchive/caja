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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Wraps a list of tokens and provides operations that may replace
 * whitespace tokens in that list.
 */
class Indenter {
  /** The token list.  Modified in place. */
  final List<String> tokens;
  /**
   * Maps indices of brackets ("(", ")", etc.) in tokens to the corresponding
   * open or close bracket.
   * <p>
   * For each token in tokens, the corresponding element in this list is
   * -1 if token is not a bracket, or the index of the close/open bracket token
   * that ends/starts the run of tokens started/opened by token.
   * <p>
   * For example, for the code {@code f(a[i])}, tokens is
   * {@code ["f", "(", "a", "[", "i", "]", ")"]}, and
   * {@code match[1] == 6 && match[6] == 1} as the outer parentheses match up,
   * {@code match[3] == 5 && match[5] == 3} as the inner parentheses match up.
   * and all the other elements in match are -1.
   */
  final int[] match;
  /**
   * For each token in tokens, true if the closest containing pair of brackets
   * are parentheses.  If token is a bracket, then the corresponding element
   * of parenthetical is true iff token is a parenthesis.
   * <p>
   * For example, for the code {@code f(a[i])}, tokens is
   * {@code ["f", "(", "a", "[", "i", "]", ")"]}, and
   * parenthetical is {@code [false, true, true, false, false, false, true]}
   */
  final boolean[] parenthetical;
  /**
   * Whether to break after a comment.
   */
  final boolean breakAfterComment;

  Indenter(List<String> tokens, boolean breakAfterComment) {
    this.tokens = tokens;
    this.breakAfterComment = breakAfterComment;
    match = new int[tokens.size()];
    parenthetical = new boolean[tokens.size()];
    // Keep track of the bracket blocks still open at point i.
    List<Integer> openBracketStack = new ArrayList<Integer>();
    // Whether the top element of openBracketStack is an open parenthesis.
    boolean isParenthetical = false;
    // Walk forward over tokens maintaining the open bracket stack.
    // When we see a close bracket, set the elements in match corresponding to
    // both the open bracket and the close bracket.
    for (int i = 0, n = tokens.size(); i < n; ++i) {
      parenthetical[i] = isParenthetical;
      String tok = tokens.get(i);
      if (tok.length() == 1) {
        switch (tok.charAt(0)) {
        case '(':
          openBracketStack.add(i);
          parenthetical[i] = isParenthetical = true;
          break;
        case '[': case '{':
          openBracketStack.add(i);
          isParenthetical = false;
          break;
        case '}': case ']': case ')':
          int last = openBracketStack.size() - 1;
          if (last >= 0) {
            int openIdx = openBracketStack.remove(last);
            match[openIdx] = i;
            match[i] = openIdx;
          }
          // Check whether the new topmost bracket is a parenthesis.
          isParenthetical = (
              last >= 1
              && "(".equals(tokens.get(openBracketStack.get(last - 1))));
          break;
        default:
          match[i] = -1;
          break;
        }
      }
    }
    for (int unclosedBracketIdx : openBracketStack) {
      match[unclosedBracketIdx] = tokens.size();
    }
  }

  /**
   * Change space tokens into newlines when a single line contains too many
   * tokens or when a line is normally broken at that token.
   * <p>
   * This does not need to worry about inserting newlines into restricted
   * productions since the superclass will convert to spaces any newlines that
   * would appear in a restricted context.
   */
  void breakLines() {
    for (int i = 0, last = tokens.size() - 1; i <= last; ++i) {
      String tok = tokens.get(i);
      if (!" ".equals(tok)) { continue; }
      String prev = i != 0 ? tokens.get(i - 1) : null;
      String next = i != last ? tokens.get(i + 1) : null;
      boolean isBreak;
      if ("{".equals(prev)) {
        // Break inside curly blocks that are long.
        // But not things like foo({})
        isBreak = !isShortRun(i, match[i - 1]);
      } else if ("}".equals(next)) {
        // Matches the above.
        isBreak = !isShortRun(match[i + 1], i);
      } else if (";".equals(prev)) {
        // parenthetical check distinguishes { for(;;) } from { foo(); }
        isBreak = !parenthetical[i];
      } else if ("}".equals(prev)) {
        if ("else".equals(next) || "catch".equals(next)
            || "finally".equals(next)) {
          isBreak = false;
        } else if ("while".equals(next)) {
          // Distinguish { do {} while (1); } from { {} while (1) {} }
          isBreak = true;
          int open = match[i - 1];
          if (open > 0) {
            for (int j = open; --j >= 0;) {
              String t = tokens.get(j);
              if ("do".equals(t)) {
                isBreak = false;
                break;
              }
              if (!(TokenClassification.isComment(t)
                    || Character.isWhitespace(t.charAt(0)))) {
                break;
              }
            }
          }
        } else {
          isBreak = true;
        }
      } else if (prev != null && TokenClassification.isComment(prev)) {
        isBreak = breakAfterComment;
      } else {
        isBreak = false;
      }
      if (isBreak) {
        if (next != null && TokenClassification.isComment(next)
            && i + 2 <= last) {
          String next2 = tokens.get(i + 2);
          if ("\n".equals(next2) || " ".equals(next2)) {
            tokens.set(i + 2, "\n");
            isBreak = false;
          }
        }
        if (isBreak) {
          tokens.set(i, "\n");
        }
      }
    }
  }

  /**
   * Called once all the line-breaks are present to introduce indentation by
   * looking at the stack of open bracketed blocks.
   * Turns newline tokens in tokens into newlines followed by runs of spaces.
   */
  void indent(int lineLengthLimit) {
    int charsInLine = 0;  // count of chars since last newline token
    LinkedList<Indent> indents = new LinkedList<Indent>();

    // Information about the last place at which we can insert a breakpoint
    int lastBreakPt = -1;
    LinkedList<Indent> indentsAtLastBreakPt = null;
    int charsInLineAtLastBreakPt = -1;

    indents.add(new Indent(0, false));
    for (int i = 0, n = tokens.size(); i < n; ++i) {
      String tok = tokens.get(i);
      charsInLine += tok.length();

      // If the line is too long, break it.
      if (lastBreakPt >= 0 && charsInLine >= lineLengthLimit
          && !(" ".equals(tok) || "\n".equals(tok))) {
        if (" ".equals(tokens.get(lastBreakPt))) {
          tokens.set(lastBreakPt, "\n");
          i = lastBreakPt - 1;
        } else {
          tokens.add(lastBreakPt + 1, "\n");
          i = lastBreakPt;
        }
        lastBreakPt = -1;
        indents = indentsAtLastBreakPt;
        charsInLine = charsInLineAtLastBreakPt;
        continue;
      }

      // Handle non punctuation and non-space tokens which can't break a line
      // or push/pop from the indent stack.
      if (tok.length() != 1) {
        if (!TokenClassification.isComment(tok)) {
          indents.set(0, indents.get(0).withInStatement(true));
        }
        continue;
      }

      // Maintain the indent stack, and indent as necessary.
      switch (tok.charAt(0)) {
        case '(': case '[':
          int indentLevel = Math.min(charsInLine, indents.get(0).spaces + 2);
          indents.addFirst(new Indent(indentLevel, parenthetical[i]));
          break;
        case '{':
          indents.set(0, indents.get(0).withInStatement(false));
          indents.addFirst(new Indent(indents.get(0).spaces + 2, false));
          break;
        case '}':
          if (indents.size() > 1) { indents.removeFirst(); }
          indents.set(0, indents.get(0).withInStatement(false));
          break;
        case ']': case ')':
          if (indents.size() > 1) { indents.removeFirst(); }
          break;
        case ' ':
          break;
        case '\n':
          if (i + 1 < n) {
            String nextToken = tokens.get(i + 1);
            if (!"\n".equals(nextToken)) {
              charsInLine = indents.get(0).getIndentLevel();
              if (nextToken.length() == 1) {
                switch (nextToken.charAt(0)) {
                  case '}': case ']': case ')':
                    if (indents.size() > 1) {
                      charsInLine = indents.get(1).spaces;
                    }
                    break;
                }
              } else if ("case".equals(nextToken)
                         || "default".equals(nextToken)) {
                charsInLine = Math.max(charsInLine - 2, 0);
              }
              tokens.set(i, makeIndent(charsInLine));
            }
          }
          lastBreakPt = -1;
          break;
        case ';': case ',': case ':':
          indents.set(0, indents.get(0).withInStatement(false));
          break;
        default:
          indents.set(0, indents.get(0).withInStatement(true));
          break;
      }

      // Keep track of breakpoints in case a line gets too long.
      switch (tok.charAt(0)) {
        case ' ':
        // breaking at close brackets is nice since we start off at a lower
        // indent level
        case ')': case '}': case ']':
          lastBreakPt = i;
          indentsAtLastBreakPt = new LinkedList<Indent>(indents);
          charsInLineAtLastBreakPt = charsInLine;
          break;
      }
    }
  }

  private boolean isShortRun(int start, int end) {
    if (end - start > 8) { return false; }
    int len = 0;
    for (int i = start; i < end; ++i) {
      if ((len += tokens.get(i).length()) > 20) { return false; }
    }
    return true;
  }

  private static String makeIndent(int nSpaces) {
    StringBuilder sb = new StringBuilder(nSpaces + 1);
    sb.append('\n');
    while (nSpaces >= 16) {
      sb.append("                ");
      nSpaces -= 16;
    }
    sb.append("                ", 0, nSpaces);
    return sb.toString();
  }
}
