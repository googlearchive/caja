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

package com.google.caja.lexer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;

import com.google.caja.util.Pair;

import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class ChardetTest extends TestCase {
  public final void testEmptyDocument() throws IOException {
    assertCharset("", new byte[0], "UTF-8");
  }

  public final void testMetaHttpEquiv() throws IOException {
    String metaInputUtf8 = (
        ""
        + "<html>"
        + "<head>"
        + "<meta http-equiv=\"Content-type\" value=\"text/html;charset=UTF-8\">"
        + "</head>"
        + "<body>Hello, World!</body>"
        + "</html>");
    assertCharset(metaInputUtf8, metaInputUtf8.getBytes("UTF-8"), "UTF-8");
    String metaInputUtf16BE = (
        ""
        + "<html>"
        + "<head>"
        + "<meta http-equiv=\"Content-type\""
        + " value=\"text/html;charset =UTF-16BE\">"
        + "</head>"
        + "<body>Hello, World!</body>"
        + "</html>");
    assertCharset(
        metaInputUtf16BE, metaInputUtf16BE.getBytes("UTF-16BE"), "UTF-16BE");
    String metaInputUtf16LE = (
        ""
        + "<html>"
        + "<head>"
        + "<meta http-equiv=\"Content-type\""
        + " value=\"text/html;charset= 'UTF-16LE\">"
        + "</head>"
        + "<body>Hello, World!</body>"
        + "</html>");
    assertCharset(
        metaInputUtf16LE, metaInputUtf16LE.getBytes("UTF-16LE"), "UTF-16LE");
  }

  public final void testBOM() throws IOException {
    String html = "<html>Hello, World!</html>";
    String htmlWithBom = "\ufeff" + html;
    assertCharset(html, htmlWithBom.getBytes("UTF-8"), "UTF-8");
    assertCharset(html, htmlWithBom.getBytes("UTF-16LE"), "UTF-16LE");
    assertCharset(html, htmlWithBom.getBytes("UTF-16BE"), "UTF-16BE");
    if (Chardet.supportedCharsetName("UTF-32LE") != null) {
      assertCharset(html, htmlWithBom.getBytes("UTF-32LE"), "UTF-32LE");
      assertCharset(html, htmlWithBom.getBytes("UTF-32BE"), "UTF-32BE");
    }
    if (Chardet.supportedCharsetName("UTF-7") != null) {
      assertCharset(html, htmlWithBom.getBytes("UTF-7"), "UTF-7");
    }
    if (Chardet.supportedCharsetName("UTF-1") != null) {
      assertCharset(html, htmlWithBom.getBytes("UTF-1"), "UTF-1");
    }
  }

  public final void testXmlPrologue() throws IOException {
    String html = "<html>Hello, World!</html>";
    String xmlUtf8 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + html;
    assertCharset(xmlUtf8, xmlUtf8.getBytes("UTF-8"), "UTF-8");
    String xmlUtf16BE = "<?xml version=\"1.0\" encoding=\"UTF-16BE\"?>" + html;
    assertCharset(xmlUtf16BE, xmlUtf16BE.getBytes("UTF-16BE"), "UTF-16BE");
    String xmlUtf16LE = "<?xml version=\"1.0\" encoding=\"UTF-16LE\"?>" + html;
    assertCharset(xmlUtf16LE, xmlUtf16LE.getBytes("UTF-16LE"), "UTF-16LE");
  }

  public final void testCharsetInText() throws IOException {
    String html = (
        ""
        + "<html>"
        + "<head>"
        + "<title>charset=UTF-16LE</title>"
        + "</head>"
        + "<body>"
        + "Hello, World!"
        + "</body>"
        + "</html>");
    for (String encoding : new String[] { "UTF-8", "UTF-16LE", "UTF-16BE" }) {
      assertCharset(html, ("\ufeff" + html).getBytes(encoding), encoding);
    }
  }

  private static void assertCharset(
      String golden, byte[] bytes, String expectedCharset)
      throws IOException {
    Pair<Reader, String> p = Chardet.guessCharset(
        new ByteArrayInputStream(bytes));
    assertEquals(expectedCharset, p.b);
    StringBuilder sb = new StringBuilder();
    char[] buf = new char[1024];
    for (int n; (n = p.a.read(buf)) > 0;) { sb.append(buf, 0, n); }
    assertEquals(golden, sb.toString());
  }
}
