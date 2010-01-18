// Copyright (C) 2010 Google Inc.
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

package com.google.caja.ancillary.servlet;

import java.util.regex.Pattern;

/**
 * Utilities for converting between globs and regexs.
 *
 * @author mikesamuel@gmail.com
 */
final class Glob {

  /**
   * A pattern that matches the given glob where ? and * match any single or
   * run of characters, respectively, that don't contain newlines, and where
   * a slash escapes the next character.
   */
  static final Pattern globToRegex(String glob) {  // Visible for testing
    int pos = 0;
    int n = glob.length();
    StringBuilder sb = new StringBuilder(n + 16);
    for (int i = 0; i < n; ++i) {
      String sub = null;
      switch (glob.charAt(i)) {
        case '*': sub = ".*"; break;
        case '?': sub = "."; break;
        case '\\':
          if (pos != i) { sb.append(Pattern.quote(glob.substring(pos, i))); }
          pos = ++i;
          continue;
      }
      if (sub != null) {
        if (pos != i) { sb.append(Pattern.quote(glob.substring(pos, i))); }
        sb.append(sub);
        pos = i + 1;
      }
    }
    if (pos != n) { sb.append(Pattern.quote(glob.substring(pos, n))); }
    return Pattern.compile(sb.toString());
  }

  static final String regexToGlob(Pattern p) {
    String re = p.pattern();
    StringBuilder sb = new StringBuilder(re.length());
    for (int i = 0, n = re.length(); i < n; ++i) {
      char ch = re.charAt(i);
      switch (ch) {
        case '.':
          if (i + 1 < n && re.charAt(i + 1) == '*') {
            sb.append('*');
            ++i;
          } else if (i + 1 < n && re.charAt(i + 1) == '+') {
            sb.append("?*");
            ++i;
          } else {
            sb.append('?');
          }
          break;
        case '[': case '{': case '(': case '^': case '$':
          throw new IllegalArgumentException(re);
        case '\\':
          if (i + 1 < n) {
            char ch1 = re.charAt(++i);
            switch (ch1) {
              case 'n': sb.append('\n'); break;
              case 'r': sb.append('\r'); break;
              case 't': sb.append('\t'); break;
              case 'Q':
                int idx = re.indexOf("\\E", i);
                for (int j = i + 1; j < idx; ++j) {
                  char chq = re.charAt(j);
                  if (chq == '\\' || chq == '*' || chq == '?') {
                    sb.append('\\');
                  }
                  sb.append(chq);
                }
                i = idx + 1;
                break;
              case '\\': case '*': case '?':
                sb.append('\\').append(ch1);
                break;
              default: sb.append(ch1);
            }
          }
          break;
        default:
          sb.append(ch);
          break;
      }
    }
    return sb.toString();
  }
}
