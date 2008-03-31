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

/**
 * Quickly classifies JS and CSS tokens so they can be rendered to an output
 * stream.
 *
 * @author mikesamuel@gmail.com
 */
enum TokenClassification {
  LINEBREAK,
  SPACE,
  STRING,
  REGEX,
  COMMENT,
  PUNCTUATION,
  OTHER,
  ;

  static TokenClassification classify(CharSequence text) {
    if ("".equals(text)) { return null; }

    char ch0 = text.charAt(0);
    if (ch0 == '\n') { return LINEBREAK; }
    if (ch0 == ' ') { return SPACE; }

    int n = text.length();
    if (n >= 2) {
      char ch1 = text.charAt(1);
      char chLast = text.charAt(n - 1);
      switch (ch0) {
        case '/':
          if (ch1 == '*') {
            if (chLast != '/' || text.charAt(n - 2) != '*') {
              // Prevent any tricky regex that looks like a comment.
              throw new IllegalArgumentException();
            }
            return COMMENT;
          }
          if (ch1 == '/') {
            // This would escape the following newline.
            if (chLast == '\\') { throw new IllegalArgumentException(); }
            return COMMENT;
          }
          if (n > 2) {  // /= is 2 characters and / is 1
            return REGEX;
          }
          break;
        case '.': case '-': case '+':
          if (Character.isLetterOrDigit(ch1)) {
            return OTHER;
          }
          break;
        case '"': case '\'':
          return STRING;
      }
    }
    if (Character.isLetterOrDigit(ch0) || ch0 == '$' || ch0 == '_') {
      return OTHER;
    }
    return PUNCTUATION;
  }

  static boolean isNumber(String s) {
    int n = s.length();
    if (n == 0) { return false; }
    char ch = s.charAt(0);
    if (ch == '+' || ch == '-') {
      if (n == 1) { return false; }
      ch = s.charAt(1);
    }
    return ch >= '0' && ch <= '9';
  }
}
