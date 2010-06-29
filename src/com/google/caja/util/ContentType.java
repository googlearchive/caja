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

import com.google.caja.util.Maps;
import com.google.caja.util.Strings;

import java.util.Map;

/**
 * A file type.
 *
 * @author mikesamuel@gmail.com
 */
public enum ContentType {
  CSS("text/css", "css", true),
  JS("text/javascript", "js", true),
  JSON("application/json", "json", true),
  HTML("text/html", "html", true),
  XML("application/xhtml+xml", "xhtml", true),
  ZIP("application/zip", "zip", false),
  ;

  public final String mimeType;
  public final String ext;
  public final boolean isText;

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
      .put("application/json", ContentType.JSON)
      .put("text/css", ContentType.CSS)
      .put("text/html", ContentType.HTML)
      .put("application/xml", ContentType.XML)
      .put("application/xhtml+xml", ContentType.XML)
      .put("application/x-winzip", ContentType.ZIP)
      .put("application/zip", ContentType.ZIP)
      .put("application/x-java-archive", ContentType.ZIP)
      .create();

  public static ContentType fromMimeType(String mimeType) {
    int end = mimeType.indexOf(';');
    if (end >= 0) {
      while (end > 0 && Character.isWhitespace(mimeType.charAt(end))) { --end; }
    } else {
      end = mimeType.length();
    }
    return MIME_TYPES.get(Strings.toLowerCase(mimeType.substring(0, end)));
  }
}
