// Copyright (C) 2009 Google Inc.
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

package com.google.caja.util;

import com.google.caja.lexer.Keyword;
import com.google.caja.parser.ParserBase;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Produces a sequence of strings like "a", "b", "c", ... "aa", "ab", "ac", ...
 * skipping JS keywords and other
 * {@link SafeIdentifierMaker#isSafeIdentifier unsafe}
 * identifiers.
 *
 * @author mikesamuel@gmail.com
 */
public final class SafeIdentifierMaker implements Iterator<String> {
  private final char[] alphabet;

  /** Visible for testing. */
  SafeIdentifierMaker(char[] alphabet) {
    this.alphabet = alphabet;
  }
  public SafeIdentifierMaker() {
    this(charRanges('a', 'z', 'A', 'Z'));
  }

  private int counter = -1;
  public String next() {
    int nLetters = alphabet.length;
    while (true) {
      if (counter + 1 == Integer.MAX_VALUE) {
        throw new NoSuchElementException();
      }
      ++counter;
      StringBuilder sb = new StringBuilder();
      int n = counter;
      while (n >= 0) {
        sb.append(alphabet[n % nLetters]);
        // Subtract 1 for the fake "end of string" character.
        n = (n / nLetters) - 1;
      }
      String word = sb.reverse().toString();
      // Disallow keywords and other words that are not safe identifiers..
      if (isSafeIdentifier(word)) { return word; }
    }
  }
  public boolean hasNext() { return counter < Integer.MAX_VALUE; }
  public void remove() { throw new UnsupportedOperationException(); }

  public static boolean isSafeIdentifier(String ident) {
    return !ident.endsWith("__")
        && ParserBase.isJavascriptIdentifier(ident)
        && !Keyword.isKeyword(ident) && !"arguments".equals(ident)
        // "eval" has special significance in the ES262 spec, so do not output
        // it.  From section 15.1.2.1:
        // If value of the eval property is used in any way other than a direct
        // call (that is, other than by the explicit use of its name as an
        // Identifier which is the MemberExpression in a Call Expression), or if
        // the eval property is assigned to, an Eval Error exception may be
        // thrown.
        && !"eval".equals(ident);
  }

  /**
   * @param startsAndEnds adjacent start and end characters, inclusive.
   *   To output all the characters in the regexp charset {@code [0-9a-f]},
   *   use the input {@code '0', '9', 'a', 'f'}.
   */
  private static char[] charRanges(char... startsAndEnds) {
    assert (startsAndEnds.length % 2) == 0;
    int len = 0;
    for (int i = 0; i < startsAndEnds.length; i += 2) {
      len += startsAndEnds[i + 1] + 1 - startsAndEnds[i];
    }
    char[] ranges = new char[len];
    int k = 0;
    for (int i = 0; i < startsAndEnds.length; i += 2) {
      char j = startsAndEnds[i];
      char e = startsAndEnds[i + 1];
      assert j <= e;
      while (j <= e) {
        ranges[k++] = j++;
      }
    }
    assert k == ranges.length;
    return ranges;
  }
}
