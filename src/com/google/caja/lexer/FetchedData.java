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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.util.Charsets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;

import javax.annotation.WillClose;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

/**
 * Encapsulates a unit of content fetched from some remote location, including
 * some basic metadata about the content.
 *
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public abstract class FetchedData {
  protected final String contentType;
  protected final String charSet;

  protected FetchedData(String contentType, String charSet) {
    this.contentType = contentType;
    this.charSet = charSet != null ? charSet : "";
  }

  private static class BinaryFetchedData extends FetchedData {
    private final byte[] content;
    private final InputSource src;

    BinaryFetchedData(
        byte[] content, String contentType, String charSet, InputSource src) {
      super(contentType, charSet);
      this.content = content;
      this.src = src;
    }

    @Override
    public CharProducer getTextualContent()
        throws UnsupportedEncodingException {
      return CharProducer.Factory.fromString(
          new String(content, "".equals(charSet)
              ? Charsets.UTF_8.name() : charSet), src);
    }

    @Override
    public InputStream getBinaryContent() {
      return new ByteArrayInputStream(content);
    }

    @Override
    public byte[] getByteContent() { return content.clone(); }
  }

  private static class TextualFetchedData extends FetchedData {
    private final CharProducer cp;

    public TextualFetchedData(
        CharProducer cp, String contentType, String charSet) {
      super(contentType, charSet);
      this.cp = cp;
    }

    @Override
    public CharProducer getTextualContent() {
      return cp.clone();
    }

    @Override
    public InputStream getBinaryContent() throws UnsupportedEncodingException {
      return new ByteArrayInputStream(getByteContent());
    }

    @Override
    public byte[] getByteContent() throws UnsupportedEncodingException {
      return cp.toString().getBytes(charSet);
    }
  }

  public static FetchedData fromBytes(
      byte[] content, String contentType, String charSet, InputSource src) {
    return new BinaryFetchedData(content, contentType, charSet, src);
  }

  public static FetchedData fromStream(
      @WillClose InputStream is, String contentType, String charSet,
      InputSource src)
      throws IOException {
    return fromBytes(readStream(is), contentType, charSet, src);
  }

  public static FetchedData fromConnection(URLConnection connection)
      throws IOException {
    connection.connect();
    URI uri;
    try {
      uri = connection.getURL().toURI();
    } catch (URISyntaxException ex) {
      throw new SomethingWidgyHappenedError(ex);
    }
    return fromBytes(
        readStream(connection.getInputStream()),
        connection.getContentType(),
        getCharSet(connection),
        new InputSource(uri));
  }

  public static FetchedData fromCharProducer(
      CharProducer cp, String contentType, String charSet) {
    return new TextualFetchedData(cp, contentType, charSet);
  }

  public static FetchedData fromReader(
      Reader in, InputSource is, String contentType, String charSet)
      throws IOException {
    return fromCharProducer(
        CharProducer.Factory.create(in, is), contentType, charSet);
  }

  public static FetchedData fromReader(
      Reader in, FilePosition pos, String contentType, String charSet)
      throws IOException {
    return fromCharProducer(
        CharProducer.Factory.create(in, pos), contentType, charSet);
  }

  public abstract CharProducer getTextualContent()
      throws UnsupportedEncodingException;

  public abstract InputStream getBinaryContent()
      throws UnsupportedEncodingException;

  public abstract byte[] getByteContent()
      throws UnsupportedEncodingException;

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

  private static int MAX_RESPONSE_SIZE_BYTES = 1 << 24;  // 16MB
  protected static byte[] readStream(@WillClose InputStream is)
      throws IOException {
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] barr = new byte[4096];
      int totalLen = 0;
      for (int n; (n = is.read(barr)) > 0;) {
        if ((totalLen += n) > MAX_RESPONSE_SIZE_BYTES) {
          throw new IOException("Response too large");
        }
        buffer.write(barr, 0, n);
      }
      return buffer.toByteArray();
    } finally {
      is.close();
    }
  }

  private static String getCharSet(URLConnection conn) {
    try {
      String contentType = conn.getContentType();
      if (contentType == null) { return ""; }
      String charset = new ContentType(contentType).getParameter("charset");
      return (charset == null) ? "" : charset;
    } catch (ParseException e) {
      return "";
    }
  }
}

