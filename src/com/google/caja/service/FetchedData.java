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

package com.google.caja.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

/**
 * Encapsulates a unit of content fetched from some remote location, including
 * some basic metadata about the content.
 *
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public final class FetchedData {
  private final byte[] content;
  private final String contentType;
  private final String charSet;

  public FetchedData(byte[] content, String contentType, String charSet) {
    this.content = content;
    this.contentType = contentType;
    this.charSet = charSet;
  }

  public FetchedData(InputStream is, String contentType, String charSet)
      throws IOException {
    this.content = readStream(is);
    this.contentType = contentType;
    this.charSet = charSet;
  }

  public FetchedData(URI uri) throws IOException {
    this(uri.toURL().openConnection());
  }

  public FetchedData(URLConnection connection) throws IOException {
    connection.connect();
    this.content = readStream(connection.getInputStream());
    this.contentType = connection.getContentType();
    this.charSet = getCharSet(connection);
  }

  /**
   * @return the actual content bytes.
   */
  public byte[] getContent() { return content; }

  /**
   * @return the MIME type of the content.
   */
  public String getContentType() { return contentType; }

  /**
   * @return the character set of the content. This is only meaningful if the
   * MIME type of the content is textual and if the character set is provided
   * by the source. If neither case is true, the return value of this method
   * will be an empty string.
   */
  public String getCharSet() { return charSet; }

  private static int MAX_RESPONSE_SIZE_BYTES = 1 << 18;  // 256kB
  protected static byte[] readStream(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      byte[] barr = new byte[4096];
      int totalLen = 0;
      for (int n; (n = is.read(barr)) > 0;) {
        if ((totalLen += n) > MAX_RESPONSE_SIZE_BYTES) {
          throw new IOException("Response too large");
        }
        buffer.write(barr, 0, n);
      }
    } finally {
      is.close();
    }
    return buffer.toByteArray();
  }

  private static String getCharSet(URLConnection conn) {
    try {
      String charset = new ContentType(conn.getContentType())
          .getParameter("charset");
      return (charset == null) ? "" : charset;
    } catch (ParseException e) {
      return "";
    }
  }
}

