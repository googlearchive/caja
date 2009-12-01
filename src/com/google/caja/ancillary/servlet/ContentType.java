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

package com.google.caja.ancillary.servlet;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.util.Maps;
import com.google.caja.util.Strings;

import java.util.Map;

/**
 * A file type.
 *
 * @author mikesamuel@gmail.com
 */
enum ContentType {
  CSS("text/css", "css", true),
  JS("text/javascript", "js", true),
  JSON("application/json", "json", true),
  HTML("text/html", "html", true),
  XML("application/xhtml+xml", "xhtml", true),
  ZIP("application/zip", "zip", false),
  ;

  final String mimeType;
  final String ext;
  final boolean isText;

  ContentType(String contentType, String ext, boolean isText) {
    this.mimeType = contentType;
    this.ext = ext;
    this.isText = isText;
  }

  private static final Map<String, ContentType> MIME_TYPES
      = Maps.<String, ContentType>immutableMap()
      // From http://krijnhoetmer.nl/stuff/javascript/mime-types/ and others.
      .put("text/javascript", ContentType.JS)
      .put("application/x-javascript", ContentType.JS)
      .put("application/javascript", ContentType.JS)
      .put("text/ecmascript", ContentType.JS)
      .put("application/ecmascript", ContentType.JS)
      .put("text/jscript", ContentType.JS)
      .put("text/css", ContentType.CSS)
      .put("text/html", ContentType.HTML)
      .put("application/xml", ContentType.XML)
      .put("application/xhtml+xml", ContentType.XML)
      .put("application/x-winzip", ContentType.ZIP)
      .put("application/zip", ContentType.ZIP)
      .put("application/x-java-archive", ContentType.ZIP)
      .create();

  static ContentType guess(String mimeType, String path, CharSequence code) {
    ContentType contentType = null;
    if (mimeType != null) {
      int semi = mimeType.indexOf(';');
      String baseType = semi < 0 ? mimeType : mimeType.substring(0, semi);
      contentType = MIME_TYPES.get(Strings.toLowerCase(baseType).trim());
    }
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
      try {
        CssLexer cssLexer = new CssLexer(
            cp, DevNullMessageQueue.singleton(), false);
        contentType = ContentType.CSS;
        while (cssLexer.hasNext()) {
          Token<CssTokenType> t = cssLexer.next();
          if ("if".equals(t.text) || "while".equals(t.text)
              || "for".equals(t.text) || "return".equals(t.text)) {
            contentType = ContentType.JS;
            break;
          }
        }
      } catch (ParseException ex) {
        contentType = ContentType.JS;
      }
    }
    return contentType;
  }
}
