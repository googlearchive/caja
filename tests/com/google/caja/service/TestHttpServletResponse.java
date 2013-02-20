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
import com.google.caja.util.Maps;
import com.google.caja.util.Strings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public final class TestHttpServletResponse implements HttpServletResponse {
  private int status = 200;
  private Map<String, String> headers = Maps.newLinkedHashMap();
  private Object output;
  public void addCookie(Cookie a) { throw new UnsupportedOperationException(); }
  public boolean containsHeader(String n) { return headers.containsKey(n); }
  public String encodeRedirectURL(String arg0) {
    throw new UnsupportedOperationException();
  }
  @Deprecated
  public String encodeRedirectUrl(String url) { return encodeRedirectURL(url); }
  public String encodeURL(String arg0) {
    throw new UnsupportedOperationException();
  }
  @Deprecated
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
    setHeader(arg0, new Date(arg1).toString());
  }
  public void setHeader(String k, String v) {
    if (output != null) { throw new IllegalStateException(); }
    headers.put(Strings.lower(k), v);
  }
  public void setIntHeader(String arg0, int arg1) {
    setHeader(arg0, "" + arg1);
  }
  public void setStatus(int status) {
    if (output != null) { throw new IllegalStateException(); }
    this.status = status;
  }
  @Deprecated
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
  public String getContentType() {
    return headers.get("content-type");
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
          throw new SomethingWidgyHappenedError(ex);
        }
      }
      return bytes;
    }
    return ((StringWriter) output).toString();
  }
  public void addDateHeader(String arg0, long arg1) {
    addHeader(arg0, "" + new Date(arg1));
  }
  public void addHeader(String arg0, String arg1) {
    headers.put(arg0, arg1);
  }
  public void addIntHeader(String arg0, int arg1) {
    addHeader(arg0, "" + arg1);
  }
  public void flushBuffer() {
    throw new UnsupportedOperationException();
  }
  public int getBufferSize() {
    throw new UnsupportedOperationException();
  }
  public Locale getLocale() {
    throw new UnsupportedOperationException();
  }
  public boolean isCommitted() {
    throw new UnsupportedOperationException();
  }
  public void reset() {
    throw new UnsupportedOperationException();
  }
  public void resetBuffer() {
    throw new UnsupportedOperationException();
  }
  public void setBufferSize(int arg0) {
    throw new UnsupportedOperationException();
  }
  public void setLocale(Locale arg0) {
    throw new UnsupportedOperationException();
  }
  public void setCharacterEncoding(String charset) {
    throw new UnsupportedOperationException();
  }

  public Map<String, String> getHeaders() { return headers; }
}
