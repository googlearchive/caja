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

package com.google.caja.lexer;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.util.ContentType;

/**
 * Guesses content type based on reported mime-type, file name, content of file.
 *
 * @author mikesamuel@gmail.com
 */
public final class GuessContentType {

  /**
   * Tries a number of heuristics to determine the type of content.
   * <ol>
   *   <li>if mimeType is provided and is recognized, uses it.
   *   <li>if the file path is available and has a recognized extension, uses
   *   it.
   *   <li>if the code is provided then applies some further heuristics;
   *     anything where the first non-whitespace char is {@code '<'} is markup.
   * </ol>
   *
   * @param mimeType null or a string like "text/plain" or
   *    "text/javascript; param=value"
   * @param path null or the path to the content.  Either a URL path
   *     (no trailing query or fragment) or a file system path will work.
   * @param code null or a representative prefix of the content.
   * @return a known {@link ContentType} or null if none could be found.
   */
  public static ContentType guess(
      String mimeType, String path, CharSequence code) {
    ContentType contentType = null;
    if (mimeType != null) { contentType = ContentType.fromMimeType(mimeType); }
    if (contentType == null && path != null) {
      int dot = path.lastIndexOf('.');
      if (dot >= 0) {
        String ext = path.substring(dot + 1);
        for (ContentType candidate : ContentType.values()) {
          if (ext.equals(candidate.ext)) {
            contentType = candidate;
            break;
          }
        }
      }
    }
    if (contentType == null && code != null) {
      char ch = '\0';
      for (int i = 0, n = code.length(); i < n; ++i) {
        ch = code.charAt(i);
        if (!Character.isWhitespace(ch)) { break; }
      }
      switch (ch) {
        case '<':
          contentType = ContentType.HTML;
          break;
        case '@': case '.': case '#':
          contentType = ContentType.CSS;
          break;
      }
    }
    if (contentType == null && code != null) {
      // Try and lex and see what happens.
      CharProducer cp = CharProducer.Factory.fromString(
          code, FilePosition.UNKNOWN);
      boolean sawColon = false;
      try {
        CssLexer cssLexer = new CssLexer(
            cp, DevNullMessageQueue.singleton(), false);
        contentType = ContentType.CSS;
        while (cssLexer.hasNext()) {
          Token<CssTokenType> t = cssLexer.next();
          if ("if".equals(t.text) || "while".equals(t.text)
              || "for".equals(t.text) || "return".equals(t.text)
              || "?".equals(t.text)) {
            contentType = ContentType.JS;
            break;
          }
          if (!sawColon && ":".equals(t.text)) { sawColon = true; }
        }
        if (!sawColon) { contentType = ContentType.JS; }
      } catch (ParseException ex) {
        contentType = ContentType.JS;
      }
    }
    return contentType;
  }
}
