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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.TranslatedCode;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Utilities for junit test cases.
 *
 * @author mikesamuel@gmail.com
 */
public final class TestUtil {

  private TestUtil() {
    // uninstantiable
  }

  /**
   * Java 1.5 is missing some core libraries so we are more restrictive under
   * 1.5 than we might otherwise be.  This means we need to disable certain
   * tests.
   */
  public static boolean isJava15() {
    String version = System.getProperty("java.version");
    return version != null && version.startsWith("1.5.");
  }

  public static String readResource(Class<?> requestingClass, String filename)
      throws IOException {
    InputStream ins = getResourceAsStream(requestingClass, filename);
    if (null == ins) {
      throw new FileNotFoundException(
        "Failed to read " + filename + " relative to " + requestingClass);
    }
    try {
      BufferedReader in = new BufferedReader(
          new InputStreamReader(ins, "UTF-8"));
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

  public static MessageQueue createTestMessageQueue(MessageContext mc) {
    // Tests can be run with
    //     ant -Djunit.verbose=true runtests
    // to dump stacktraces with messages in the log.
    boolean verbose = "true".equals(System.getProperty("junit.verbose"));
    return new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.err)), mc, verbose);
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
      throw new SomethingWidgyHappenedError(
          "The following url is not a valid uri: " + url);
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
        new InputStreamReader(conn.getInputStream(), "UTF-8"),
        new InputSource(uri));
  }

  public static String format(ParseTreeNode n) {
    StringBuilder output = new StringBuilder();
    try {
      n.format(new MessageContext(), output);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "StringBuilder does not throw IOException", ex);
    }
    return output.toString();
  }

  public static MessageLevel maxMessageLevel(MessageQueue mq) {
    MessageLevel max = MessageLevel.values()[0];
    for (Message msg : mq.getMessages()) {
      MessageLevel lvl = msg.getMessageLevel();
      if (max.compareTo(lvl) < 0) { max = lvl; }
    }
    return max;
  }

  public static boolean hasErrors(MessageQueue mq) {
    return MessageLevel.ERROR.compareTo(maxMessageLevel(mq)) <= 0;
  }

  public static boolean hasErrorsOrWarnings(MessageQueue mq) {
    return MessageLevel.WARNING.compareTo(maxMessageLevel(mq)) <= 0;
  }

  public static void enableContentUrls() {
    RhinoExecutor.enableContentUrls();
  }

  public static String makeContentUrl(String content) {
    return "content:" + UriUtil.encode(content).replace("+", "%20");
  }

  public static void removePseudoNodes(ParseTreeNode node) {
    assert !(node instanceof TranslatedCode);
    node.acceptPostOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ac) {
          if (ac.node instanceof TranslatedCode) {
            ((MutableParseTreeNode) ac.parent.node).replaceChild(
                ((TranslatedCode) ac.node).getTranslation(), ac.node);
          }
          return true;
        }
      }, null);
  }
}
