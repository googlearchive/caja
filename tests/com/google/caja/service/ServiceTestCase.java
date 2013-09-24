// Copyright (C) 2008 Google Inc.
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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Charsets;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.TestBuildInfo;
import com.google.common.collect.Maps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public abstract class ServiceTestCase extends CajaTestCase {
  private CajolingServlet servlet;
  private Map<URI, FetchedData> uriContent;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    servlet = new CajolingServlet(new CajolingService(
        TestBuildInfo.getInstance(), null,
        new UriFetcher() {
          public FetchedData fetch(ExternalReference ref, String mimeType)
              throws UriFetchException {
            FetchedData data = uriContent.get(ref.getUri());
            if (data == null) {
              throw new UriFetchException(ref, mimeType);
            }
            return data;
          }
        }));
    uriContent = Maps.newHashMap();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  protected static Object json(String json) {
    return JSONValue.parse(json);
  }

  protected static void assertMessagesLessSevereThan(
      JSONArray messages, MessageLevel severity) {
    for (Object m : messages.toArray()) {
      Object level = ((JSONObject) m).get("level");
      assertTrue(((Long) level).longValue() < severity.ordinal());
    }
  }

  protected void registerUri(String uri, String content, String contentType) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      Writer w = new OutputStreamWriter(out, Charsets.UTF_8);
      w.write(content);
      w.flush();
    } catch (UnsupportedEncodingException ex) {
      throw new SomethingWidgyHappenedError(ex);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(ex);
    }
    registerUri(uri, out.toByteArray(), contentType, "UTF-8");
  }

  protected void registerUri(
      String uri, byte[] content, String contentType, String charset) {
    uriContent.put(
        URI.create(uri),
        FetchedData.fromBytes(
            content, contentType, charset, new InputSource(URI.create(uri))));
  }

  protected Object requestGet(String queryString, String expectedResponseType)
      throws Exception {
    TestHttpServletRequest req = new TestHttpServletRequest(queryString);
    TestHttpServletResponse resp = new TestHttpServletResponse();
    servlet.doGet(req, resp);
    assertResponseContentType(expectedResponseType, resp);
    return resp.getOutputObject();
  }

  protected Object requestPost(
      String queryString,
      byte[] content,
      String contentType,
      String contentEncoding,
      String expectedResponseType) throws Exception {
    TestHttpServletRequest req =
        new TestHttpServletRequest(queryString, content, contentType,
            contentEncoding);
    TestHttpServletResponse resp = new TestHttpServletResponse();
    servlet.doPost(req, resp);
    assertResponseContentType(expectedResponseType, resp);
    return resp.getOutputObject();
  }

  // TODO(ihab.awad): Change tests to use structural equality (via quasi
  // matches) rather than golden text to avoid this.
  protected static void assertEqualsIgnoreSpace(
      String expected, String actual) {
    assertEquals(
        expected.replaceAll("\\s", ""),
        actual.replaceAll("\\s", ""));
  }

  private static String normStringSpaces(String s) {
    return s.replaceAll("[ \r\n\t]+", " ")
        .replaceAll("^ | $|(?<=\\W) | (?=\\W)", "");
  }

  protected static void assertContainsIgnoreSpace(
      String full,
      String substring) {
    assertTrue(
        "Substring <" + substring + "> not part of <" + full + ">",
        normStringSpaces(full).contains(normStringSpaces(substring)));
  }

  protected static void assertSubstringsInJson(
      String emitted,
      String jsonProperty,
      String... expectedSubstrings) throws Exception {
    assertNotNull(emitted);
    JSONObject json = (JSONObject) json(emitted);
    assertNotNull(json);
    assertTrue(jsonProperty + " present", json.containsKey(jsonProperty));
    String value = (String) json.get(jsonProperty);
    for (String s : expectedSubstrings) {
      assertContainsIgnoreSpace(value, s);
    }
  }

  protected static void assertSubstringsInJsonp(
      String emitted,
      String jsonProperty,
      String... expectedSubstrings) throws Exception {
    Pattern p = Pattern.compile("(?s)^[a-zA-Z_]+\\((\\{.*\\})\\);$");
    Matcher m = p.matcher(emitted);
    assertTrue(m.matches());
    assertSubstringsInJson(m.group(1), jsonProperty, expectedSubstrings);
  }

  protected static void assertCallbackInJsonp(
      String emitted,
      String jsonpCallback) throws Exception {
    Pattern p = Pattern.compile("(?s)^" + jsonpCallback + "\\((\\{.*\\})\\);$");
    Matcher m = p.matcher(emitted);
    assertTrue(m.matches());
  }

  private static void assertResponseContentType(String expectedResponseType,
      TestHttpServletResponse resp) {
    assertEquals(expectedResponseType, resp.getContentType().split(";")[0]);
  }
}
