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

package com.google.caja.opensocial.service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import junit.framework.TestCase;

/**
 * Tests the running the cajoler as a webservice
 *
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class CajolingServiceTest extends TestCase {
  private CajolingService service;
  private Map<URI, CajolingService.FetchedData> uriContent;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    service = new CajolingService() {
      @Override
      protected CajolingService.FetchedData fetch(URI uri) throws IOException {
        if (!uriContent.containsKey(uri)) {
          throw new IOException(uri.toString());
        }
        return uriContent.get(uri);
      }
    };
    uriContent = new HashMap<URI, CajolingService.FetchedData>();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private void registerUri(String uri, String content, String contentType) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      Writer w = new OutputStreamWriter(out, "UTF-8");
      w.write(content);
      w.flush();
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    registerUri(uri, out.toByteArray(), contentType, "UTF-8");
  }

  private void registerUri(
      String uri, byte[] content, String contentType, String charset) {
    uriContent.put(
        URI.create(uri),
        new CajolingService.FetchedData(content, contentType, charset));
  }

  private Object request(String queryString) throws Exception {
    TestHttpServletRequest req = new TestHttpServletRequest(queryString);
    TestHttpServletResponse resp = new TestHttpServletResponse();
    service.doGet(req, resp);
    return resp.getOutputObject();
  }

  public void testSimpleJs() throws Exception {
    registerUri("http://foo/bar.js", "var x = y;", "text/javascript");
    assertEquals(
        "{\n  var y = ___.readImport(IMPORTS___, 'y');\n  var x = y;\n}",
        request("?url=http://foo/bar.js&mime-type=text/javascript"));
  }

  public void testVbScriptRejected() throws Exception {
    registerUri("http://foo/bar.vb", "zoicks()", "text/vbscript");
    assertEquals(
        "ERROR",
        request("?url=http://foo/bar.vb&mime-type=text/javascript"));

  }

  public void testAltJscriptMimeType() throws Exception {
    registerUri(
        "http://foo/bar.js", "f();", "application/x-javascript");
    assertEquals(
        request("?url=http://foo/bar.js&mime-type=text/javascript"),
        "{\n  var f = ___.readImport(IMPORTS___, 'f');\n"
        + "  ___.asSimpleFunc(f)();\n}");
  }

  public void testImage() throws Exception {
    byte[] byteData = { (byte) 0x47, (byte) 0x49, (byte) 0x46,
                        (byte) 0x39, (byte) 0x38, (byte) 0x61 };
    registerUri("http://foo/bar.gif", byteData, "image/gif", null);
    assertTrue(Arrays.equals(
        (byte[]) request("?url=http://foo/bar.gif&mime-type=image/*"),
        byteData));
  }

  public void testNotImage() throws Exception {
    registerUri("http://foo/bar.gif", "foo()", "text/javascript");
    assertEquals("ERROR", request("?url=http://foo/bar.gif&mime-type=image/*"));
  }

  public void testHtml() throws Exception {
    String moduleEnvelope = (
        "<Module><ModulePrefs /><Content type=\"html\">"
        + "<![CDATA[%s]]>"
        + "</Content></Module>");

    registerUri("http://foo/bar.js", "foo()", "text/javascript");
    registerUri("http://foo/bar.xml",
                String.format(
                    moduleEnvelope,
                    "<script src=bar.js></script><p>Hello, World!</p>"),
                "application/xml");
    // TODO(mikesamuel): why are scripts not fetched?
    assertEquals(
        String.format(
            moduleEnvelope,
            "<script type=\"text/javascript\">{\n"
            + "  ___.loadModule(function (___, IMPORTS___) {\n"
            + "                   IMPORTS___.htmlEmitter___"
            + ".b('p').f(false).ih('Hello, World!').e('p');\n"
            + "                 });\n"
            + "}</script>"),
        request("?url=http://foo/bar.xml&mime-type=*/*"));
  }
}

final class TestHttpServletRequest implements HttpServletRequest {
  private final String queryString;
  private final Hashtable<String, List<String>> params
      = new Hashtable<String, List<String>>();
  TestHttpServletRequest(String queryString) {
    this.queryString = queryString;
    for (String pair : queryString.split("[?&]")) {
      int eq = pair.indexOf('=');
      String k = decode(eq >= 0 ? pair.substring(0, eq) : pair);
      String v = decode(eq >= 0 ? pair.substring(eq + 1) : "");
      if (!params.containsKey(k)) {
        params.put(k, new ArrayList<String>());
      }
      params.get(k).add(v);
    }
  }
  public String getAuthType() { throw new UnsupportedOperationException(); }
  public Cookie[] getCookies() { throw new UnsupportedOperationException(); }
  public long getDateHeader(String a) {
    throw new UnsupportedOperationException();
  }
  public String getHeader(String a) {
    throw new UnsupportedOperationException();
  }
  public Enumeration<String> getHeaderNames() {
    throw new UnsupportedOperationException();
  }
  public int getIntHeader(String arg0) {
    throw new UnsupportedOperationException();
  }
  public String getMethod() { return "GET"; }
  public String getPathInfo() { throw new UnsupportedOperationException(); }
  public String getPathTranslated() {
    throw new UnsupportedOperationException();
  }
  public String getQueryString() { return queryString; }
  public String getRemoteUser() { throw new UnsupportedOperationException(); }
  public String getRequestURI() { return "/proxy"; }
  public String getRequestedSessionId() {
    throw new UnsupportedOperationException();
  }
  public String getServletPath() { throw new UnsupportedOperationException(); }
  public HttpSession getSession() { throw new UnsupportedOperationException(); }
  public HttpSession getSession(boolean a) {
    throw new UnsupportedOperationException();
  }
  public boolean isRequestedSessionIdFromCookie() {
    throw new UnsupportedOperationException();
  }
  public boolean isRequestedSessionIdFromURL() {
    throw new UnsupportedOperationException();
  }
  public boolean isRequestedSessionIdFromUrl() {
    throw new UnsupportedOperationException();
  }
  public boolean isRequestedSessionIdValid() {
    throw new UnsupportedOperationException();
  }
  public Object getAttribute(String arg0) {
    throw new UnsupportedOperationException();
  }
  public Enumeration<String> getAttributeNames() {
    throw new UnsupportedOperationException();
  }
  public String getCharacterEncoding() { return null; }
  public int getContentLength() { return 0; }
  public String getContentType() { return null; }
  public ServletInputStream getInputStream() {
    throw new UnsupportedOperationException();
  }
  public String getParameter(String k) {
    return params.containsKey(k) ? params.get(k).get(0) : null;
  }
  public Enumeration<?> getParameterNames() { return params.keys(); }
  public String[] getParameterValues(String k) {
    List<String> vals = params.get(k);
    return vals != null ? vals.toArray(new String[0]) : null;
  }
  public String getProtocol() { return "http"; }
  public BufferedReader getReader() {
    throw new UnsupportedOperationException();
  }
  public String getRealPath(String arg0) {
    throw new UnsupportedOperationException();
  }
  public String getRemoteAddr() { throw new UnsupportedOperationException(); }
  public String getRemoteHost() { throw new UnsupportedOperationException(); }
  public String getScheme() { return getProtocol(); }
  public String getServerName() { return "test.proxy"; }
  public int getServerPort() { return -1; }
  public void setAttribute(String arg0, Object arg1) {
    throw new UnsupportedOperationException();
  }

  private static String decode(String mimeEncoded) {
    try {
      return URLDecoder.decode(mimeEncoded, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
  }
}

final class TestHttpServletResponse implements HttpServletResponse {
  private int status = 200;
  private Hashtable<String, String> headers = new Hashtable<String, String>();
  private Object output;
  public void addCookie(Cookie a) { throw new UnsupportedOperationException(); }
  public boolean containsHeader(String n) { return headers.containsKey(n); }
  public String encodeRedirectURL(String arg0) {
    throw new UnsupportedOperationException();
  }
  public String encodeRedirectUrl(String url) { return encodeRedirectURL(url); }
  public String encodeURL(String arg0) {
    throw new UnsupportedOperationException();
  }
  public String encodeUrl(String url) { return encodeURL(url); }
  public void sendError(int code) {
    setStatus(code);
    setContentType("text/html");
    getWriter().write("ERROR");
  }
  public void sendError(int code, String desc) {
    setStatus(code, desc);
    setContentType("text/html");
    getWriter().write("ERROR");
  }
  public void sendRedirect(String arg0) {
    throw new UnsupportedOperationException();
  }
  public void setDateHeader(String arg0, long arg1) {
    throw new UnsupportedOperationException();
  }
  public void setHeader(String k, String v) {
    if (output != null) { throw new IllegalStateException(); }
    headers.put(k.toLowerCase(Locale.ENGLISH), v);
  }
  public void setIntHeader(String arg0, int arg1) {
    throw new UnsupportedOperationException();
  }
  public void setStatus(int status) {
    if (output != null) { throw new IllegalStateException(); }
    this.status = status;
  }
  public void setStatus(int status, String desc) {
    assert !desc.matches("\\s");
    setStatus(status);
  }
  public int getStatus() { return status; }
  private String getSpecifiedCharacterEncoding() {
    String contentType = headers.get("content-type");
    if (contentType != null) {
      Matcher m = Pattern.compile(";\\s*charset=(\\S+)").matcher(contentType);
      if (m.find()) {
        return m.group(1);
      }
    }
    return null;
  }
  public String getCharacterEncoding() {
    String enc = getSpecifiedCharacterEncoding();
    return enc != null ? enc : "UTF-8";
  }
  public ServletOutputStream getOutputStream() {
    if (output == null) { output = new ByteArrayOutputStream(); }
    final OutputStream out = (OutputStream) output;
    return new ServletOutputStream() {
      @Override
      public void write(int arg0) throws IOException { out.write(arg0); }
    };
  }
  public PrintWriter getWriter() {
    if (output == null) { output = new StringWriter(); }
    return new PrintWriter((Writer) output);
  }
  public void setContentLength(int arg0) {
    assert arg0 >= 0;
    setHeader("Content-length", "" + arg0);
  }
  public void setContentType(String arg0) {
    setHeader("Content-type", arg0);
  }
  public Object getOutputObject() {
    if (output == null) { return null; }
    if (output instanceof ByteArrayOutputStream) {
      String enc = getSpecifiedCharacterEncoding();
      byte[] bytes = ((ByteArrayOutputStream) output).toByteArray();
      if (enc != null) {
        try {
          return new String(bytes, enc);
        } catch (UnsupportedEncodingException ex) {
          throw new RuntimeException(ex);
        }
      }
      return bytes;
    }
    return ((StringWriter) output).toString();
  }
}
