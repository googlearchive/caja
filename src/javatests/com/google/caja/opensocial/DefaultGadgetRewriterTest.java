// Copyright (C) 2007 Google Inc.
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

package com.google.caja.opensocial;

import com.google.caja.lexer.ExternalReference;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.TestUtil;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class DefaultGadgetRewriterTest extends TestCase {

  private static final UriCallback uriCallback = new UriCallback() {
    public UriCallbackOption getOption(
        ExternalReference extref, String mimeType) {
      return UriCallbackOption.RETRIEVE;
    }

    public Reader retrieve(ExternalReference extref, String mimeType)
        throws UriCallbackException {
      if ("file".equals(extref.getUri().getScheme())) {
        if (extref.getUri().toString().startsWith(
                TestUtil.getResource(getClass(), "").toString())) {
          try {
            return new FileReader(extref.getUri().getPath());
          } catch (FileNotFoundException ex) {
            throw new UriCallbackException(extref, ex);
          }
        }
      }
      throw new UriCallbackException(extref);
    }

    public URI rewrite(ExternalReference extref, String mimeType) {
      try {
        return URI.create(
            "http://url-proxy.test.google.com/"
            + "?url=" + URLEncoder.encode(extref.getUri().toString(), "UTF-8")
            + "&mime-type=" + URLEncoder.encode(mimeType, "UTF-8"));
      } catch (UnsupportedEncodingException ex) {
        // If we don't support UTF-8 we're in trouble
        throw new RuntimeException(ex);
      }
    }
  };

  private DefaultGadgetRewriter rewriter;

  @Override
  public void setUp() {
    rewriter = new DefaultGadgetRewriter(
        new EchoingMessageQueue(
            new PrintWriter(System.err), new MessageContext(), false)) {
          @Override
          protected RenderContext createRenderContext(
              Appendable out, MessageContext mc) {
            return new RenderContext(mc, out, false);
          }
        };
  }

  @Override
  public void tearDown() { rewriter = null; }

  // Test Gadget parsing
  public void testInlineGadget() throws Exception {
    assertRewritePasses("listfriends-inline.xml", MessageLevel.WARNING);
  }

  public void testParsing() throws Exception {
    assertRewritePasses("test-parsing.xml", MessageLevel.WARNING);
  }

  public void testSourcedGadget() throws Exception {
    assertRewritePasses("listfriends.xml", MessageLevel.WARNING);
  }

  // Test Gadget rewriting
  public void testExampleGadget() throws Exception {
    assertRewriteMatches("example.xml", "example-rewritten.xml",
                       MessageLevel.ERROR);
  }

  // Check that the validating and rewriting passes are hooked up.
  public void testTargetsDisallowed() throws Exception {
    assertRewriteFailsWithMessage(
        "<a target=\"_top\">Redirect window</a>",
        "attribute target cannot have value _top");
  }

  public void testMetaRefreshDisallowed() throws Exception {
    assertRewriteFailsWithMessage(
        "<meta http-equiv=\"refresh\" content=\"5;http://foo.com\"/>",
        "tag meta is not allowed");
  }

  public void testStylesSanitized() throws Exception {
    assertRewriteFailsWithMessage(
        "<p style=\"color: expression(foo)\">Bar</p>",
        "css property color has bad value: ==>expression(foo)<==");
  }

  private void assertRewritePasses(String file, MessageLevel failLevel)
      throws Exception {
    Reader input = new StringReader(TestUtil.readResource(getClass(), file));
    URI baseUri = TestUtil.getResource(getClass(), file);
    rewriter.rewrite(baseUri, input, uriCallback, System.out);

    checkMessages(failLevel);
  }

  private void assertRewriteMatches(
      String file, String goldenFile, MessageLevel failLevel)
      throws Exception {
    Reader input = new StringReader(TestUtil.readResource(getClass(), file));
    URI baseUri = TestUtil.getResource(getClass(), file);

    StringBuilder sb = new StringBuilder();
    rewriter.rewrite(baseUri, input, uriCallback, sb);
    String actual = normalXml(sb.toString()).trim();

    checkMessages(failLevel);

    String expected
        = normalXml(TestUtil.readResource(getClass(), goldenFile)).trim();
    if (!expected.equals(actual)) {
      System.err.println(actual);
      assertEquals(expected, actual);
    }
  }

  private static String normalXml(String xml) {
    return xml.replaceFirst("^<\\?xml[^>]*>", "");
  }

  private void assertRewriteFailsWithMessage(String htmlContent, String msg)
      throws Exception {
    Reader input = new StringReader(
       "<?xml version=\"1.0\"?>"
       + "<Module>"
       + "<ModulePrefs title=\"Example Gadget\">"
       + "<Require feature=\"opensocial-0.5\"/>"
       + "</ModulePrefs>"
       + "<Content type=\"html\">"
       + "<![CDATA[" + htmlContent + "]]>"
       + "</Content>"
       + "</Module>");
    URI baseUri = URI.create("http://unittest.google.com/foo/bar/");
    try {
      rewriter.rewrite(baseUri, input, uriCallback, System.out);
      fail("rewrite should have failed with message " + msg);
    } catch (GadgetRewriteException ex) {
      // pass
    }

    List<Message> errors = getMessagesExceedingLevel(MessageLevel.ERROR);

    assertFalse("Expected error msg: " + msg, errors.isEmpty());
    String actualMsg = errors.get(0).format(new MessageContext());
    // strip off the file position since that's tested in the modules that
    // generate the errors.
    actualMsg = actualMsg.substring(actualMsg.indexOf(": ") + 2).trim();
    assertEquals(msg, actualMsg);
  }

  private List<Message> getMessagesExceedingLevel(MessageLevel limit) {
    List<Message> matches = new ArrayList<Message>();
    for (Message msg : rewriter.getMessageQueue().getMessages()) {
      if (msg.getMessageLevel().compareTo(limit) >= 0) {
        matches.add(msg);
      }
    }
    return matches;
  }

  private void checkMessages(MessageLevel failLevel) {
    List<Message> failures = getMessagesExceedingLevel(failLevel);
    MessageContext mc = new MessageContext();
    if (!failures.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Message failure : failures) {
        sb.append(failure.getMessageLevel())
            .append(" : ")
            .append(failure.format(mc))
            .append('\n');
      }
      fail(sb.toString().trim());
    }
  }
}
