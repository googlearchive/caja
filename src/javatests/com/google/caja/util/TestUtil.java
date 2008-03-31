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
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.JsHtmlParser;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.reporting.RenderContext;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.Assert;

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

  public static DomParser parseXml(
      Class<?> requestingClass, String testResource, MessageQueue mq)
      throws IOException, ParseException {
    URI resource = getResource(requestingClass, testResource);
    if (null == resource) {
      throw new IOException("Could not resolve resource " + testResource
                            + " relative to " + requestingClass);
    }
    InputSource is = new InputSource(resource);
    useSourceToDisambiguateLocationsInMessages(is, mq);
    HtmlLexer lexer = new HtmlLexer(
        getResourceAsProducer(requestingClass, testResource));
    return new DomParser(lexer, is, mq);
  }

  public static Statement parseTree(
      Class<?> requestingClass, String testResource, MessageQueue mq)
      throws IOException, ParseException {
    URI resource = getResource(requestingClass, testResource);
    if (null == resource) {
      throw new IOException("Could not resolve resource " + testResource
                            + " relative to " + requestingClass);
    }
    InputSource is = new InputSource(resource);
    useSourceToDisambiguateLocationsInMessages(is, mq);
    CharProducer cp = getResourceAsProducer(requestingClass, testResource);
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

  public static void checkFilePositionInvariants(ParseTreeNode root) {
    checkFilePositionInvariants(new AncestorChain<ParseTreeNode>(root));
  }

  public static Block parse(String src) throws ParseException {
    InputSource inputSource
        = new InputSource(URI.create("built-in:///js-test"));
    Parser parser = new Parser(
        new JsTokenQueue(
            new JsLexer(
                CharProducer.Factory.create(
                    new StringReader(src),
                    inputSource)),
            inputSource,
            JsTokenQueue.NO_NON_DIRECTIVE_COMMENT),
        DevNullMessageQueue.singleton());

    Statement topLevelStatement = parser.parse();
    parser.getTokenQueue().expectEmpty();
    return (Block)topLevelStatement;
  }

  public static String format(ParseTreeNode n) {
    StringBuilder output = new StringBuilder();
    try {
      n.format(new MessageContext(), output);
    } catch (IOException ex) {
      throw new RuntimeException(ex);  // StringBuilder should not throw.
    }
    return output.toString();
  }

  public static String render(ParseTreeNode n) {
    StringBuilder output = new StringBuilder();
    TokenConsumer tc = n.makeRenderer(output, null);
    n.render(new RenderContext(new MessageContext(), tc));
    tc.noMoreTokens();
    return output.toString();
  }
  
  private static void checkFilePositionInvariants(AncestorChain<?> nChain) {
    ParseTreeNode n = nChain.node;
    String msg = n + " : " + n.getFilePosition();
    try {
      // require that n start on or after its previous sibling
      ParseTreeNode prev = nChain.getPrevSibling();
      Assert.assertTrue(msg, null == prev
                        || (prev.getFilePosition().endCharInFile()
                            <= n.getFilePosition().startCharInFile()));

      // require that n end on or before its next sibling
      ParseTreeNode next = nChain.getNextSibling();
      Assert.assertTrue(msg, null == next
                        || (next.getFilePosition().startCharInFile()
                            >= n.getFilePosition().endCharInFile()));

      // require that n encompass its children
      List<? extends ParseTreeNode> children = n.children();
      if (!children.isEmpty()) {
        ParseTreeNode first = children.get(0),
                       last = children.get(children.size() - 1);
        Assert.assertTrue(msg, first.getFilePosition().startCharInFile()
                          >= n.getFilePosition().startCharInFile());
        Assert.assertTrue(msg, last.getFilePosition().endCharInFile()
                          <= n.getFilePosition().endCharInFile());
      }

      for (ParseTreeNode c : children) {
        checkFilePositionInvariants(
            new AncestorChain<ParseTreeNode>(nChain, c));
      }
    } catch (RuntimeException ex) {
      throw new RuntimeException(msg, ex);
    }
  }

  /**
   * Allow parsing of content: URLs which can be useful for the browser mocks
   * since it allows us to specify HTML in a string.
   * <p>
   * This registers a handler for the <code>content</code> protocol so that
   * {@code content:foo-bar} when loaded via {@code java.net.URL} will yield an
   * {@code InputStream} containing the UTF-8 encoding of the string
   * {@code "foo-bar"}.
   */
  public static void enableContentUrls() {
    // Force loading of class that registers a handler for content: URLs.
    SetupUrlHandlers.init();
  }

  public static String makeContentUrl(String content) {
    try {
      return "content:"
          + URLEncoder.encode(content, "UTF-8").replace("+", "%20");
    } catch (UnsupportedEncodingException ex) {
      throw (AssertionError) new AssertionError(
          "UTF-8 not supported").initCause(ex);
    }
  }
}

class SetupUrlHandlers {
  static {
    URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
        private Map<String, URLStreamHandler> handlers
            = new HashMap<String, URLStreamHandler>();

        // The below scheme for extending URL handlers is written according to
        // examples at:
        // http://www.webbasedprogramming.com
        // /Tricks-of-the-Java-Programming-Gurus/ch17.htm

        public URLStreamHandler createURLStreamHandler(String protocol) {
          protocol = protocol.toLowerCase(Locale.ENGLISH);

          URLStreamHandler handler;
          synchronized (handlers) {
            handler = handlers.get(protocol);
            if (handler == null) {
              handler = createHandler(protocol);
            }
          }
          return handler;
        }

        private URLStreamHandler createHandler(String protocol) {
          if ("content".equalsIgnoreCase(protocol)) {
            return new ContentUrlHandler();
          } else if ("http".equals(protocol) || "https".equals(protocol)) {
            // We could allow tests to stub out the internet, but
            // we definitely don't want unittests loading arbitrary URIs.
          } else {
            // Make sure that we support file: and jar: URIs so that
            // classloaders continue to work.
            try {
              String clname = "sun.net.www.protocol." + protocol + ".Handler";
              return (URLStreamHandler) Class.forName(clname).newInstance();
            } catch (Exception e) {
              System.err.println("No URL Handler for protocol " + protocol);
            }
          }
          return null;
        }
      });
  }

  public static void init() {}
}

class ContentUrlHandler extends URLStreamHandler {
  @Override
  protected URLConnection openConnection(URL url) {
    return new URLConnection(url) {
        private InputStream instream;

        @Override
        public void connect() {
          if (connected) { return; }
          connected = true;
          URI uri;
          try {
            uri = url.toURI();
          } catch (URISyntaxException ex) {
            ex.printStackTrace();
            return;
          }
          assert uri.isOpaque();
          try {
            instream = new ByteArrayInputStream(
                uri.getSchemeSpecificPart().getBytes("UTF-8"));
          } catch (UnsupportedEncodingException ex) {
            throw (AssertionError) new AssertionError(
                "UTF-8 not supported").initCause(ex);
          }
        }

        @Override
        public InputStream getInputStream() {
          if (instream == null) { throw new IllegalStateException(); }
          return instream;
        }
      };
  }
}
