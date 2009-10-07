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

import com.google.caja.util.CajaTestCase;

import java.net.URLConnection;
import java.net.URL;
import java.net.URI;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class FetchedDataTest extends CajaTestCase {
  private static class TestURLConnection extends URLConnection {
    private String data;
    private String contentType;

    public TestURLConnection(URL url, String data, String contentType) {
      super(url);
      this.data = data;
      this.contentType = contentType;
    }

    @Override
    public void connect() { }

    @Override
    public String getContentType() {
      return contentType;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(data.getBytes());
    }
  }

  private URL testUrl;

  public void setUp() throws Exception {
    testUrl = URI.create("http://www.example.com/").toURL();
  }

  public void tearDown() throws Exception { }

  public void testSimpleContent() throws Exception {
    FetchedData fd = new FetchedData(
        new TestURLConnection(testUrl, "abcdef", "text/html"));
    assertEquals("text/html", fd.getContentType());
    assertEquals("abcdef", new String(fd.getContent()));
  }

  private void assertCharSet(
      String expectedCharSet,
      String urlConnectionContentType) throws Exception {
    String testContent = "abcdef";
    FetchedData fd = new FetchedData(
        new TestURLConnection(testUrl, testContent, urlConnectionContentType));
    assertEquals(testContent, new String(fd.getContent()));
    assertEquals(urlConnectionContentType, fd.getContentType());
    assertEquals(expectedCharSet, fd.getCharSet());
  }

  public void testCharSetParsing() throws Exception {
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
