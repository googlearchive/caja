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

package com.google.caja.service;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FetchedData;
import com.google.caja.lexer.InputSource;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles HTTP servlet invocation of a {@link CajolingService}.
 *
 * @author jasvir@gmail.com (Jasvir Nagra)
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class CajolingServlet extends HttpServlet {
  private static final long serialVersionUID = 5055670217887121398L;

  private static class HttpContentHandlerArgs extends ContentHandlerArgs {
    private final HttpServletRequest request;

    public HttpContentHandlerArgs(HttpServletRequest request) {
      this.request = request;
    }

    @Override
    public String get(String name) {
      return request.getParameter(name);
    }
  }

  private final CajolingService service;

  //Appengine insists on a zero-argument constructor
  public CajolingServlet() {
    this(new CajolingService());
  }

  public CajolingServlet(CajolingService service) {
    this.service = service;
  }

  /**
   * Set an error status on a servlet response and close its stream cleanly.
   *
   * @param resp a servlet response.
   * @param error an error message.
   */
  private static void closeBadRequest(HttpServletResponse resp,
                                      String error)
      throws ServletException {
    try {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error);
    } catch (IOException ex) {
      throw (ServletException) new ServletException().initCause(ex);
    }
  }

  /**
   * Set an error status on a servlet response and close its stream cleanly.
   *
   * @param resp a servlet response.
   * @param mq a {@link MessageQueue} with messages to include as an error page.
   */
  private static void closeBadRequest(HttpServletResponse resp,
                                      MessageQueue mq)
      throws ServletException {
    closeBadRequest(resp, serializeMessageQueue(mq));
  }

  private static String serializeMessageQueue(MessageQueue mq) {
    StringBuilder sb = new StringBuilder();
    MessageContext mc = new MessageContext();
    for (Message m : mq.getMessages()) {
      try {
        sb.append(m.getMessageLevel()).append(": ");
        m.format(mc, sb);
        sb.append("\n");
      } catch (IOException e) {
        throw new SomethingWidgyHappenedError(e);
      }
    }
    return sb.toString();
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException {
    if (req.getContentType() == null) {
      closeBadRequest(resp, "Supplied Content-type is null");
      return;
    }

    FetchedData fetchedData;
    try {
      fetchedData = FetchedData.fromStream(
          req.getInputStream(), req.getContentType(),
          req.getCharacterEncoding(),
          InputSource.UNKNOWN);
    } catch (IOException e) {
      closeBadRequest(resp, "Error decoding POST data");
      return;
    }

    handle(resp, new HttpContentHandlerArgs(req), fetchedData);
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException {
    handle(resp, new HttpContentHandlerArgs(req), null);
  }

  private void handle(HttpServletResponse resp,
                      ContentHandlerArgs args,
                      FetchedData inputFetchedData)
      throws ServletException {
    MessageQueue mq = new SimpleMessageQueue();
    FetchedData result = service.handle(inputFetchedData, args, mq);
    if (result == null || mq.hasMessageAtLevel(MessageLevel.ERROR)) {
      closeBadRequest(resp, mq);
      return;
    }

    resp.setStatus(HttpServletResponse.SC_OK);
    String responseContentType = result.getContentType();
    if (result.getCharSet() != null) {
      responseContentType += ";charset=" + result.getCharSet();
    }
    if (containsNewline(responseContentType)) {
      throw new IllegalArgumentException(responseContentType);
    }

    try {
      byte[] content = result.getByteContent();
      resp.setContentType(responseContentType);
      resp.setContentLength(content.length);

      resp.getOutputStream().write(content);
      resp.getOutputStream().close();
    } catch (IOException ex) {
      throw (ServletException) new ServletException().initCause(ex);
    }
  }

  // Used to protect against header splitting attacks.
  private static boolean containsNewline(String s) {
    return s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
  }
}