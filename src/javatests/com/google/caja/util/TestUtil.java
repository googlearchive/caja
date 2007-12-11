// Copyright (C) 2005 Google Inc.
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

package com.google.caja.util;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.html.JsHtmlParser;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Utilities for junit test cases.
 *
 * @author mikesamuel@gmail.com
 */
public final class TestUtil {

  private TestUtil() {
    // uninstantiable
  }

  public static String readResource(Class<?> requestingClass, String filename)
      throws IOException {
    InputStream ins = getResourceAsStream(requestingClass, filename);
    if (null == ins) {
      throw new FileNotFoundException(
        "Failed to read " + filename + " relative to " + requestingClass);
    }
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(ins));
      StringBuilder sb = new StringBuilder();
      char[] buf = new char[1024];
      for (int n; (n = in.read(buf)) > 0;) {
        sb.append(buf, 0, n);
      }
      return sb.toString();
    } finally {
      ins.close();
    }
  }

  public static TokenQueue<HtmlTokenType> parseXml(
      Class<?> requestingClass, String testResource, MessageQueue mq)
      throws IOException {
    URI resource = getResource(requestingClass, testResource);
    if (null == resource) {
      throw new IOException("Could not resolve resource " + testResource
                            + " relative to " + requestingClass);
    }
    InputSource is = new InputSource(resource);
    useSourceToDisambiguateLocationsInMessages(is, mq);
    String content = TestUtil.readResource(requestingClass, testResource);
    CharProducer cp = CharProducer.Factory.create(
        new StringReader(content), is);

    HtmlLexer lexer = new HtmlLexer(cp);
    lexer.setTreatedAsXml(testResource.endsWith(".gxp")
                          || testResource.endsWith(".xml"));
    TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
        lexer, is, Criterion.Factory.<Token<HtmlTokenType>>optimist());
    return tq;
  }

  public static Statement parseTree(Class<?> requestingClass,
      String testResource, MessageQueue mq)
      throws IOException, ParseException {
    URI resource = getResource(requestingClass, testResource);
    if (null == resource) {
      throw new IOException("Could not resolve resource " + testResource
                            + " relative to " + requestingClass);
    }
    InputSource is = new InputSource(resource);
    useSourceToDisambiguateLocationsInMessages(is, mq);
    String content = TestUtil.readResource(requestingClass, testResource);
    CharProducer cp = CharProducer.Factory.create(
        new StringReader(content), is);
    Block fileContent;
    if (!testResource.endsWith(".html") && !testResource.endsWith(".gxp")) {
      JsLexer lexer = new JsLexer(cp);
      JsTokenQueue tq = new JsTokenQueue(
          lexer, is, JsTokenQueue.NO_NON_DIRECTIVE_COMMENT);
      Parser p = new Parser(tq, mq);
      fileContent = p.parse();
      p.getTokenQueue().expectEmpty();
    } else {
      HtmlLexer lexer = new HtmlLexer(cp);
      lexer.setTreatedAsXml(testResource.endsWith(".gxp"));
      TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
          lexer, is, Criterion.Factory.<Token<HtmlTokenType>>optimist());
      JsHtmlParser p = new JsHtmlParser(tq, mq);
      fileContent = p.parse();
      p.getTokenQueue().expectEmpty();
    }
    return fileContent;
  }

  public static MessageQueue createTestMessageQueue(MessageContext mc) {
    return new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.err)), mc);
  }

  /**
   * Wraps getResource.  This can be modified to gloss over problems with
   * build systems putting resources in the wrong place.
   */
  public static URI getResource(Class<?> cl, String resource) {
    URL url = cl.getResource(resource);
    try {
      return null != url ? url.toURI() : null;
    } catch (URISyntaxException ex) {
      throw new AssertionError("The following url is not a valid uri: " + url);
    }
  }

  /**
   * Wraps getResourceAsStream.
   * This can be modified to gloss over problems with
   * build systems putting resources in the wrong place.
   */
  public static InputStream getResourceAsStream(Class<?> cl, String resource)
      throws IOException {
    URI uri = getResource(cl, resource);
    if (null == uri) {
      throw new FileNotFoundException(
          "Resource " + resource + " relative to " + cl);
    }
    URLConnection conn = uri.toURL().openConnection();
    conn.connect();
    return conn.getInputStream();
  }

  /**
   * Make a char producer from a resource.
   */
  public static CharProducer getResourceAsProducer(Class<?> cl, String resource)
      throws IOException {
    URI uri = getResource(cl, resource);
    if (null == uri) {
      throw new FileNotFoundException(
          "Resource " + resource + " relative to " + cl);
    }
    URLConnection conn = uri.toURL().openConnection();
    conn.connect();
    return CharProducer.Factory.create(
        new InputStreamReader(conn.getInputStream()), new InputSource(uri));
  }

  /**
   * The URLs for inputs loaded from jar files are pretty ugly, so MessageQueues
   * typically use the short form which is computed by looking at all the inputs
   * so that we can come up with a short but unambiguous form.
   * This method adds the given source to that list for the MessageQueue type
   * most commonly used by unittests.
   */
  private static void useSourceToDisambiguateLocationsInMessages(
      InputSource is, MessageQueue mq) {
    if (mq instanceof EchoingMessageQueue) {
      MessageContext mc = ((EchoingMessageQueue) mq).getMessageContext();
      if (mc.inputSources.isEmpty()) {
        mc.inputSources = new ArrayList<InputSource>();
      }
      mc.inputSources.add(is);
    }
  }

}
