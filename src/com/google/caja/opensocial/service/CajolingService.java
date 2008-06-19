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

import com.google.caja.util.Pair;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.List;
import java.util.Vector;

/**
 * A cajoling service which proxies connections:<ul>
 *   <li> cajole any javascript
 *   <li> cajoles any gadgets
 *   <li> checks requested and retrieved mime-types
 * </ul>
 *
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public class CajolingService extends HttpServlet {
  private List<ContentHandler> handlers = new Vector<ContentHandler>();
  private ContentTypeCheck typeCheck = new LooseContentTypeCheck();

  public CajolingService() {
    registerHandlers();
  }

  /**
   * Read the remainder of the input request, send a BAD_REQUEST http status
   * to browser and close the connection
   */
  private static void closeBadRequest(HttpServletResponse resp)
      throws ServletException {
    try {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      resp.getWriter().close();
    } catch (IOException ex) {
      throw (ServletException) new ServletException().initCause(ex);
    }
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException {

    String gadgetUrlString = req.getParameter("url");
    if (gadgetUrlString == null) {
      throw new ServletException(
          "Missing parameter \"url\" is required: " + req.getRequestURI());
    }
    URI gadgetUrl;
    try {
      gadgetUrl = new URI(gadgetUrlString);
    } catch (URISyntaxException ex) {
      throw (ServletException) new ServletException().initCause(ex);
    }

    String expectedMimeType = req.getParameter("mime-type");
    if (expectedMimeType == null) {
      throw new ServletException(
          "Missing parameter \"mime-type\" is required: "
          + req.getRequestURI());
    }

    String contentType, contentCharSet;
    byte[] content;

    try {
      FetchedData fetched = fetch(gadgetUrl);
      contentType = fetched.contentType;
      content = fetched.content;
      contentCharSet = fetched.charSet;
    } catch (IOException ex) {
      closeBadRequest(resp);
      return;
    }

    if (!typeCheck.check(expectedMimeType, contentType)) {
      closeBadRequest(resp);
      return;
    }

    ByteArrayOutputStream intermediateResponse = new ByteArrayOutputStream();
    Pair<String,String> contentInfo;
    try {
      contentInfo = applyHandler(
          URI.create(gadgetUrl.toString()), contentType, contentCharSet,
          content, intermediateResponse);
    } catch (UnsupportedContentTypeException e) {
      closeBadRequest(resp);
      return;
    }

    byte[] response = intermediateResponse.toByteArray();
    int responseLength = response.length;

    resp.setStatus(HttpServletResponse.SC_OK);
    String responseContentType = contentInfo.a;
    if (contentInfo.b != null) {
      responseContentType += ";charset=" + contentInfo.b;
    }
    resp.setHeader("Content-Type", responseContentType);
    resp.setContentLength(responseLength);

    try {
      resp.getOutputStream().write(response);
      resp.getOutputStream().close();
    } catch (IOException ex) {
      throw (ServletException) new ServletException().initCause(ex);
    }
  }

  private static int MAX_RESPONSE_SIZE_BYTES = 1 << 18;  // 256kB
  protected FetchedData fetch(URI uri) throws IOException {
    URLConnection urlConnect = uri.toURL().openConnection();
    urlConnect.connect();
    String contentType = urlConnect.getContentType();
    String contentCharSet = urlConnect.getContentEncoding();

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    InputStream stream = urlConnect.getInputStream();
    try {
      byte[] barr = new byte[4096];
      int totalLen = 0;
      for (int n; (n = stream.read(barr)) > 0;) {
        if ((totalLen += n) > MAX_RESPONSE_SIZE_BYTES) {
          throw new IOException("Response too large");
        }
        buffer.write(barr, 0, n);
      }
    } finally {
      stream.close();
    }
    byte[] content = buffer.toByteArray();
    return new FetchedData(content, contentType, contentCharSet);
  }

  public void registerHandlers() {
    handlers.add(new JsHandler());
    handlers.add(new ImageHandler());
    handlers.add(new GadgetHandler());
  }

  private Pair<String, String> applyHandler(URI uri,
      String contentType, String charSet,
      byte[] content, OutputStream response)
      throws UnsupportedContentTypeException {
    for (ContentHandler handler : handlers) {
      if (handler.canHandle(uri, contentType, typeCheck)) {
        return handler.apply(uri, contentType, charSet, content, response);
      }
    }
    throw new UnsupportedContentTypeException();
  }

  public static final class FetchedData {
    public final byte[] content;
    public final String contentType;
    public final String charSet;
    FetchedData(byte[] content, String contentType, String charSet) {
      this.content = content;
      this.contentType = contentType;
      this.charSet = charSet;
    }
  }
}
