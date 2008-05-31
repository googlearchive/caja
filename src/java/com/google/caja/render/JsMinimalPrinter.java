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
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.PunctuationTrie;
import com.google.caja.util.Callback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A compact javascript renderer.
 *
 * @author mikesamuel@gmail.com
 */
public final class JsMinimalPrinter extends AbstractRenderer {
  /** Number of characters written to out since the last linebreak. */
  private int charInLine;
  /** The classification of the last non-space/comment token. */
  private TokenClassification lastClass;
  /** The last non-space/comment token. */
  private String lastToken;
  /** Keeps track of our position in a run of punctuation tokens. */
  private PunctuationTrie trie;
  /**
   * Line length below which the printer will not wrap lines.
   * At or above this limit, the printer will try to replace a space with
   * a line-break.
   */
  private int lineLengthLimit = 500;

  /**
   * @param out receives the rendered text.
   * @param ioExceptionHandler receives exceptions thrown by out.
   */
  public JsMinimalPrinter(
      Appendable out, Callback<IOException> ioExceptionHandler) {
    super(out, ioExceptionHandler);
  }

  public void mark(FilePosition pos) {}

  /** Visible for testing.  Should not be used by clients. */
  void setLineLengthLimit(int lineLengthLimit) {
    this.lineLengthLimit = lineLengthLimit;
  }

  @Override
  protected void append(String text) throws IOException {
    TokenClassification tClass = TokenClassification.classify(text);
    if (tClass == null) { return; }
    switch (tClass) {
      case LINEBREAK:
      case SPACE:
      case COMMENT:
        return;
    }

    boolean spaceBefore = false;

    // Determine which pairs of tokens cannot be adjacent and put a space
    // between them.
    if (tClass == TokenClassification.PUNCTUATION) {
      if (lastClass == TokenClassification.PUNCTUATION) {
        if (trie == null) {
          // If the last punctuation token was not in the punctuation set,
          // we don't need a space, but we do need to update the trie for the
          // next match.
          trie = START_TRIE.lookup(text);
        } else {
          // Otherwise, the last token might combine with this one to form
          // a larger one, such as '<' and '=' forming '<='.
          boolean ambiguous = false;
          for (int i = 0, n = text.length(); i < n; ++i) {
            trie = trie.lookup(text.charAt(i));
            if (trie == null) { break; }
            if (trie.isTerminal()) {
              // Putting text and lastToken adjacent would lead to incorrect
              // tokenization.
              spaceBefore = true;
              break;
            }
          }
          if (spaceBefore || trie == null) {  // Set up trie for next token.
            trie = START_TRIE.lookup(text);
          }
        }
      } else {
        if (lastClass == TokenClassification.REGEX && lastToken.endsWith("/")
            && (text.startsWith("*") || text.startsWith("/"))) {
          // Regular expression followed by mul or div could cause confusion.
          // /./* and /.//
          spaceBefore = true;
        } else if (lastClass == TokenClassification.OTHER
                   && text.startsWith(".")
                   && Character.isDigit(
                         lastToken.charAt(lastToken.length() - 1))) {
          // Separate numbers from . and similar operators.
          spaceBefore = true;
        }
        // Initialize the Trie in case the next token is punctuation.
        trie = START_TRIE.lookup(text);
      }
    } else if (tClass == lastClass) {
      spaceBefore = true;
    } else if (lastClass == TokenClassification.REGEX) {
      if (tClass == TokenClassification.OTHER || "/".equals(text)) {
        // Make sure words don't run into regex flags, and that / operator
        // does not combine with end of regex to make a line comment.
        spaceBefore = true;
      }
    } else if (tClass == TokenClassification.REGEX && lastToken != null
               && ("/".equals(lastToken) || lastToken.endsWith("<"))) {
      // Allowing / and a regex to run together.
      spaceBefore = true;
    } else if (tClass == TokenClassification.OTHER
               && Character.isDigit(text.charAt(0))
               && lastToken != null && lastToken.endsWith(".")) {
      // Following a dot operator with a number is illegal syntactically, but
      // this renderer should not allow any lexical confusion.
      spaceBefore = true;
    }

    // Write any whitespace before the token.
    if (spaceBefore) {
      // Some security tools/proxies/firewalls break on really long javascript
      // lines.
      if (charInLine >= lineLengthLimit && canBreakBetween(lastToken, text)) {
        newLine();
      } else {
        space();
      }
    }

    // Actually write the token.
    emit(text);

    lastClass = tClass;
    lastToken = text;
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

  private static boolean canBreakBetween(String before, String after) {
    // According to semicolon insertion rules in ES262 Section 7.9.1
    //     When, as the program is parsed from left to right, a token
    //     is encountered that is allowed by some production of the
    //     grammar, but the production is a restricted production and
    //     the token would be the first token for a terminal or
    //     nonterminal immediately following the annotation "[no
    //     LineTerminator here]" within the restricted production (and
    //     therefore such a token is called a restricted token), and
    //     the restricted token is separated from the previous token
    //     by at least one LineTerminator, then a semicolon is
    //     automatically inserted before the restricted token.

    //     These are the only restricted productions in the grammar:
    //     PostfixExpression :
    //         LeftHandSideExpression [no LineTerminator here] ++
    //         LeftHandSideExpression [no LineTerminator here] --
    //     ContinueStatement :
    //         continue [no LineTerminator here] Identifieropt ;
    //     BreakStatement :
    //         break [no LineTerminator here] Identifieropt ;
    //     ReturnStatement :
    //         return [no LineTerminator here] Expressionopt ;
    //     ThrowStatement :
    //         throw [no LineTerminator here] Expression ;
    return !("++".equals(after)
             || "--".equals(after)
             || "continue".equals(before)
             || "break".equals(before)
             || "return".equals(before)
             || "throw".equals(before));
  }

  private void emit(CharSequence s) throws IOException {
    out.append(s);
    charInLine += s.length();  // Comments skipped, so no multiline tokens.
  }

  private static final PunctuationTrie START_TRIE;
  static {
    List<String> punctuationStrings = new ArrayList<String>();
    JsLexer.getPunctuationTrie().toStringList(punctuationStrings);
    // Make sure the output can be embedded in HTML and XML.
    // All non-empty prefixes of these strings must be valid punctuation
    // sequences.  If we added <!--, then since <!- is not a valid sequence,
    // we would have to keep multiple tries above, one for every space we didn't
    // insert.
    punctuationStrings.add("<!");  // Prefix for <!-- and <![CDATA[
    punctuationStrings.add("-->");
    punctuationStrings.add("]>");  // Suffix of ]]>
    punctuationStrings.add("//");
    punctuationStrings.add("/*");
    START_TRIE = new PunctuationTrie(punctuationStrings.toArray(new String[0]));
  }
}
