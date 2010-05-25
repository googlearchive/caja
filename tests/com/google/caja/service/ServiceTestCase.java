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
import com.google.caja.util.Maps;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.TestBuildInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.Map;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public abstract class ServiceTestCase extends CajaTestCase {
  private CajolingServlet servlet;
  private Map<URI, FetchedData> uriContent;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    uriContent = Maps.newHashMap();
    servlet = new CajolingServlet(new CajolingService(
        new TestBuildInfo(), null,
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
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
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

  protected Object requestGet(String queryString) throws Exception {
    TestHttpServletRequest req = new TestHttpServletRequest(queryString);
    TestHttpServletResponse resp = new TestHttpServletResponse();
    servlet.doGet(req, resp);
    return resp.getOutputObject();
  }

  protected Object requestPost(
      String queryString,
      byte[] content,
      String contentType,
      String contentEncoding) throws Exception {
    TestHttpServletRequest req =
        new TestHttpServletRequest(queryString, content, contentType,
            contentEncoding);
    TestHttpServletResponse resp = new TestHttpServletResponse();
    servlet.doPost(req, resp);
    return resp.getOutputObject();
  }

  protected static String valijaModule(String... lines) {
    return moduleInternal(true /* valija */,
        "___.loadModule(", ")",
        lines);
  }

  protected static String cajitaModule(String... lines) {
    return moduleInternal(false /* valija */,
        "___.loadModule(", ")",
        lines);
  }

  protected static String valijaModuleWithCallback(String callback,
                                                   String... lines) {
    return moduleInternal(true /* valija */,
        callback + "(___.prepareModule(", "))",
        lines);
  }

  private static String moduleInternal(boolean valija,
                                       String modulePrefix,
                                       String moduleSuffix,
                                       String... lines) {
    String prefix = (
        ""
        + "{\n"
        + "  " + modulePrefix + "{\n"
        + "      'instantiate': function (___, IMPORTS___) {\n"
        + "        var moduleResult___ = ___.NO_RESULT;\n");
    String valijaPrefix = (
        ""
        + "        var $v = ___.readImport(IMPORTS___, '$v', {\n"
        + "            'getOuters': { '()': {} },\n"
        + "            'initOuter': { '()': {} },\n"
        + "            'cf': { '()': {} },\n"
        + "            'ro': { '()': {} }\n"
        + "          });\n"
        + "        var $dis;\n"
        + "        $dis = $v.getOuters();\n"
        + "        $v.initOuter('onerror');\n"
        );
    String suffix = (
        ""
        + "        return moduleResult___;\n"
        + "      },\n"
        + "      'includedModules': [ ],\n"
        + "      'cajolerName': 'com.google.caja',\n"
        + "      'cajolerVersion': 'testBuildVersion',\n"
        + "      'cajoledDate': 0\n"
        + "    }" + moduleSuffix + ";\n"
        + "}"
        );
    StringBuilder sb = new StringBuilder();
    sb.append(prefix);
    if (valija) {
      sb.append(valijaPrefix);
    }
    for (String line : lines) {
      sb.append("        ").append(line).append('\n');
    }
    sb.append(suffix);
    return sb.toString();
  }
}
