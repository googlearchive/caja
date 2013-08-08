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

import com.google.caja.util.CajaTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import javax.annotation.Nullable;

/**
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class FetchedDataTest extends CajaTestCase {
  private static class TestURLConnection extends URLConnection {
    private String data;
    private String contentType;
    private boolean isConnected;

    public TestURLConnection(
        URL url, String data, @Nullable String contentType) {
      super(url);
      this.data = data;
      this.contentType = contentType;
    }

    @Override
    public void connect() {
      assertFalse(isConnected);
      isConnected = true;
    }

    @Override
    public @Nullable String getContentType() {
      return contentType;
    }

    @Override
    public InputStream getInputStream() {
      assertTrue(isConnected);
      return new ByteArrayInputStream(data.getBytes());
    }
  }

  private URL testUrl;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    testUrl = URI.create("http://www.example.com/").toURL();
  }

  public final void testSimpleContent() throws Exception {
    FetchedData fd = FetchedData.fromConnection(
        new TestURLConnection(testUrl, "abcdef", "text/html"));
    assertEquals("text/html", fd.getContentType());
    assertEquals("abcdef", new String(fd.getByteContent()));
  }

  private void assertCharSet(
      String expectedCharSet, @Nullable String urlConnectionContentType)
      throws Exception {
    String testContent = "abcdef";
    FetchedData fd = FetchedData.fromConnection(
        new TestURLConnection(testUrl, testContent, urlConnectionContentType));
    assertEquals(testContent, new String(fd.getByteContent()));
    assertEquals(urlConnectionContentType, fd.getContentType());
    assertEquals(expectedCharSet, fd.getCharSet());
  }

  public final void testCharSetParsing() throws Exception {
    assertCharSet(
        "iso-8859-1",
        "text/html;charset=iso-8859-1");
    assertCharSet(
        "iso-8859-1",
        "  text/html;  boo=baz;  charset=iso-8859-1  ");
    assertCharSet(
        "iso-8859-1",
        "text/html;boo=\"baz\";charset=\"iso\\-8859\\-1\";bar=\"b\\\"ip\"");
    assertCharSet(
        "",
        "text/html;boo=baz; charset = ;bar=bip");
    assertCharSet(
        "",
        "text/html;boo=baz;charset;bar=bip");
    assertCharSet(
        "",
        "");
    assertCharSet(
        "",
        null);
  }
}
