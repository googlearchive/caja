//Copyright (C) 2008 Google Inc.
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

import com.google.caja.lexer.InputSource;
import com.google.caja.util.Pair;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

/**
 * A cajoling service which proxies connections:
 *      - cajole any javascript
 *      - cajoles any gadgets
 *      - checks requested and retrieved mime-types  
 *      
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public class CajolingService implements HttpHandler {
  private Map<InputSource, CharSequence> originalSources
    = new HashMap<InputSource, CharSequence>();
  private List<ContentHandler> handlers = new Vector<ContentHandler>();
  private ContentTypeCheck typeCheck = new LooseContentTypeCheck();
  private HttpServer server;
  
  public CajolingService() {
    registerHandlers();
  }
  
  public void start() {
    try{
      // TODO(jas): Use Config to config port
      server = HttpServer.create(new InetSocketAddress(8887),0);
      HttpContext ctx = server.createContext("/cajaservice",this);
      server.setExecutor(null);
      server.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void stop() {
    server.stop(0);
  }

  private Map<String, String> parseQuery(String query) throws UnsupportedEncodingException {
    String[] params = query.split("&");
    Map<String, String> map = new HashMap<String, String>();
    
    for (String param : params) {
      String[] result = param.split("=");
      String name = result[0];
      String value = URLDecoder.decode(result[1], "UTF-8");
      map.put(name, value);
    }
    return map;
  }
  
  /**
   * Read the remainder of the input request, send a BAD_REQUEST http status
   * to browser and close the connection
   * @param ex
   * @throws IOException 
   */
  private void closeBadRequest(HttpExchange ex) throws IOException {
    ex.sendResponseHeaders(HttpStatus.INTERNAL_SERVER_ERROR.value(),0);
    ex.getResponseBody().close();    
  }
  
  public void handle(HttpExchange ex) throws IOException {
    try {
      String requestMethod = ex.getRequestMethod();
      if (requestMethod.equalsIgnoreCase("GET")) {
        Map<String,String> urlMap = parseQuery(ex.getRequestURI().getQuery());

        String gadgetUrlString = urlMap.get("url");
        if (gadgetUrlString == null)
          throw new URISyntaxException(ex.getRequestURI().toString(), "Missing parameter \"url\" is required");
        URL gadgetUrl = new URL(gadgetUrlString);
        
        String expectedMimeType = urlMap.get("mime-type");
        if (expectedMimeType == null)
          throw new URISyntaxException(ex.getRequestURI().toString(), "Missing parameter \"mime-type\" is required");
        
        URLConnection urlConnect = gadgetUrl.openConnection();
        urlConnect.connect();
        InputStream stream = urlConnect.getInputStream();
        
        String contentEncoding = urlConnect.getContentEncoding();
        ContentType contentType = new ContentType(urlConnect.getContentType());
        String contentCharSet = contentType.getParameter("charset");

        Headers responseHeaders = ex.getResponseHeaders();
        
        if (!typeCheck.check(expectedMimeType, urlConnect.getContentType())) {
          closeBadRequest(ex);
          return;
        }

        try {
          ByteArrayOutputStream intermediateResponse = new ByteArrayOutputStream();
          Pair<String,String> contentInfo = 
            applyHandler(gadgetUrl.toURI(), urlConnect.getContentType(), 
                contentEncoding, contentCharSet, stream, intermediateResponse);

          responseHeaders.set("Content-Type", contentInfo.a);
          responseHeaders.set("Content-Encoding", contentInfo.b);
          
          byte[] response = intermediateResponse.toByteArray();
          int responseLength = response.length;
          
          ex.sendResponseHeaders(HttpStatus.ACCEPTED.value(), responseLength);
          ex.getResponseBody().write(response);
          ex.close();
        } catch (UnsupportedContentTypeException e) {
          closeBadRequest(ex);
          e.printStackTrace();
        }
      }
    } catch (URISyntaxException e) {
      closeBadRequest(ex);
      e.printStackTrace();
    } catch (ParseException e) {
      closeBadRequest(ex);
      e.printStackTrace();
    }
  }
   
  public void registerHandlers() {
    handlers.add(new JsHandler());
    handlers.add(new ImageHandler());
    handlers.add(new GadgetHandler());    
  }
  
  private Pair<String, String> applyHandler(URI uri, 
      String contentType, String contentEncoding, String charSet,
      InputStream stream, OutputStream response) 
      throws UnsupportedContentTypeException {
    for (ContentHandler handler : handlers) {
      if ( handler.canHandle(uri, contentType, typeCheck) ) {
        return handler.apply(uri, contentType, contentEncoding, charSet, stream, response);
      }
    }
    throw new UnsupportedContentTypeException();
  }
}
