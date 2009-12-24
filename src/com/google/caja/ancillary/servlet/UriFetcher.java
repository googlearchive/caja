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

import com.google.caja.util.ContentType;
import com.google.caja.util.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class UriFetcher {
  static Content fetch(URI uri) throws IOException {
    String scheme = uri.getScheme();
    if (!(Strings.equalsIgnoreCase("http", scheme)
          || Strings.equalsIgnoreCase("https", scheme))) {
      throw new IOException("Bad scheme: " + uri);
    }
    URLConnection conn = uri.toURL().openConnection();
    conn.connect();
    StringBuilder text = new StringBuilder();
    InputStream in = conn.getInputStream();
    try {
      String charset = charsetFromContentType(conn.getContentType());
      if (charset == null) { charset = "UTF-8"; }
      Reader r = new InputStreamReader(in, charset);
      char[] buf = new char[4096];
      for (int n; (n = r.read(buf)) > 0;) {
        text.append(buf, 0, n);
      }
    } finally {
      in.close();
    }
    ContentType t = GuessContentType.guess(
        conn.getContentType(), uri.getPath(), text);
    return new Content(text.toString(), t);
  }

  private static final Pattern CHARSET = Pattern.compile(
      ";\\s*charset\\s*=([^;]*)", Pattern.CASE_INSENSITIVE);
  private static String charsetFromContentType(String contentTypeHeader) {
    Matcher m = CHARSET.matcher(contentTypeHeader);
    if (!m.find()) { return null; }
    String mimeTypePart = m.group(1);
    String charset = mimeTypePart.replaceAll("[\\s\"']+", "");
    return "".equals(charset) ? null : charset;
  }
}
