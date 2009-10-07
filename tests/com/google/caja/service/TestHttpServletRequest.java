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

import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
final class TestHttpServletRequest implements HttpServletRequest {
  private final String queryString;
  private final Hashtable<String, List<String>> params
      = new Hashtable<String, List<String>>();
  private final byte[] content;
  private final String contentType;
  private final String characterEncoding;

  TestHttpServletRequest(String queryString) {
    this.queryString = queryString;
    this.content = new byte[0];
    this.contentType = null;
    this.characterEncoding = null;
    decodeParams();
  }

  TestHttpServletRequest(String queryString,
                         byte[] content,
                         String contentType,
                         String characterEncoding) {
    this.queryString = queryString;
    this.content = content;
    this.contentType = contentType;
    this.characterEncoding = characterEncoding;
    decodeParams();
  }

  private void decodeParams() {
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
  public String getMethod() { throw new UnsupportedOperationException(); }
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
  @Deprecated
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
  public String getCharacterEncoding() { return characterEncoding; }
  public int getContentLength() { return content.length; }
  public String getContentType() { return contentType; }
  public ServletInputStream getInputStream() {
    final ByteArrayInputStream bais = new ByteArrayInputStream(content);
    return new ServletInputStream() {
      @Override
      public int read() throws IOException {
        return bais.read();
      }
      @Override
      public void close() throws IOException {
        bais.close();
      }
    };
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
  @Deprecated
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