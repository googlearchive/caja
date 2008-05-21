// Copyright (C) 2008 Google Inc.
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

import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.quasiliteral.DefaultCajaRewriterTest;
import com.google.caja.util.CajaTestCase;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Tests the running the cajoler as a webservice
 * 
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class CajolingServiceTest extends CajaTestCase {
  
  // Port on which the test generating server listens
  // and returns test snippets for cajoling
  final private int HTTP_TEST_PORT = 18887;
  
  // Port on which the cajoling service runs
  // cajols urls which are requested via it
  final private int CAJOLING_SERVICE_PORT = 8887;
  
  private CajolingService service;
  private HttpServer httpServer;
  private TestingHttpHandler httpService;
  
  private class TestingHttpHandler implements HttpHandler {
  private String testInstance;
  private String contentType;
  
  public void setTest(String test, String contentType, String charSet) {
    this.testInstance = test;
    this.contentType = contentType;
  }

   public void handle(HttpExchange ex) throws IOException {
     Reader request = new InputStreamReader(ex.getRequestBody());
     while(request.read() != -1) {}
     ex.getResponseHeaders().set("Content-Type", contentType);
     ex.sendResponseHeaders(HttpStatus.OK.value(), 0);
     Writer response = null;
     try {
       response = new OutputStreamWriter(ex.getResponseBody(), "UTF-8");
       response.write(testInstance);
     } finally {
       response.close();
     }
   }
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    service = new CajolingService();
    httpServer = HttpServer.create(new InetSocketAddress(HTTP_TEST_PORT),0);
    httpService = new TestingHttpHandler();
    HttpContext ctx = httpServer.createContext("/cajaservicetest", httpService);
    httpServer.setExecutor(null);
    httpServer.start();
    service.start();
  }

  @Override
  public void tearDown() { 
    service.stop();
    // Stop the http server immediately
    httpServer.stop(0);
    service = null; 
    httpService = null;
  }

  public void testSimpleJs() throws Exception {
    checkJs(
        "{ var x = y; }",
        "var x0___;" +
        "{" + DefaultCajaRewriterTest.weldSetImports("x", "x0___", DefaultCajaRewriterTest.weldReadImports("y")) + "}");
  }
  
  private void checkJs(String original, String cajoled) throws IOException, ParseException {
    httpService.setTest(original, "text/javascript", "UTF-8");
    String localTestServer = "http://localhost:" + CAJOLING_SERVICE_PORT + "/cajaservice";
    String fetchUrl = "http://localhost:" + HTTP_TEST_PORT + "/cajaservicetest";
    String mimeType = "text/javascript";
    
    String request = localTestServer 
        + "?url=" + URLEncoder.encode(fetchUrl,"UTF-8")
        + "&mime-type=" + URLEncoder.encode(mimeType, "UTF-8");
    String response = getTextRequest(fetchUrl);
    ParseTreeNodes.deepEquals(js(fromString(response)), js(fromString(cajoled)));
  }

  private String getTextRequest(String testServer) 
    throws IOException {
    URL serverUrl = new URL(testServer);
    InputStream content = serverUrl.openStream();
    StringBuilder request = new StringBuilder();
    int in;
    while ((in = content.read()) != -1) { 
      request.append((char)in);
    }
    return request.toString();
  }
}
