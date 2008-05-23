// Copyright (C) 2006 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.CajaException;
import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Statement;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Pair;
import com.google.caja.util.TestUtil;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class GxpCompilerTest extends CajaTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testCompileDocuments() throws Exception {
    String golden = TestUtil.readResource(getClass(), "gxpcompilergolden1.js");
    DomTree.Tag domTree = (DomTree.Tag)
        xml(fromResource("gxpcompilerinput1.gxp"));
    StringBuilder actual = new StringBuilder();
    assertTrue(compileAll(new DomTree.Tag[] { domTree }, false, actual));
    assertEquals(golden.trim(), actual.toString().trim());
  }

  public void testStyles() throws Exception {
    String golden = TestUtil.readResource(getClass(), "gxpcompilergolden2.js");
    DomTree.Tag domTree = (DomTree.Tag)
        xml(fromResource("gxpcompilerinput2.gxp"));
    StringBuilder actual = new StringBuilder();
    assertTrue(compileAll(new DomTree.Tag[] { domTree }, true, actual));
    assertEquals(golden.trim(), actual.toString().trim());
    List<Pair<MessageTypeInt, FilePosition>> messages =
        new ArrayList<Pair<MessageTypeInt, FilePosition>>();
    for (Message m : mq.getMessages()) {
      List<MessagePart> parts = m.getMessageParts();
      messages.add(Pair.pair(m.getMessageType(), (FilePosition) parts.get(0)));
    }
    assertEquals(
        "[[DISALLOWED_URI, gxpcompilerinput2.gxp:6+21@84 - 53@116], "
        + "[UNSAFE_CSS_PROPERTY, gxpcompilerinput2.gxp:9+9@139 - 16@146], "
        + "[MALFORMED_CSS_PROPERTY_VALUE, gxpcompilerinput2.gxp:15+71@290"
        + " - 76@295]]",
        messages.toString());
  }

  // Test the <call:templateName param0="expr"/> construct
  public void testTemplateCalls() throws Exception {
    String golden = TestUtil.readResource(getClass(), "gxpcompilergolden3.txt");
    DomTree.Tag gxp3 = (DomTree.Tag) xml(fromResource("gxpcompilerinput3.gxp"));
    DomTree.Tag gxp4 = (DomTree.Tag) xml(fromResource("gxpcompilerinput4.gxp"));

    StringBuilder out = new StringBuilder();
    assertTrue(compileAll(new DomTree.Tag[] { gxp3, gxp4 }, true, out));

    // Write out the messages with file positions
    for (Message m : mq.getMessages()) {
      out.append('\n');
      out.append(m.getMessageType().toString()).append(" : ")
          .append(m.getMessageParts().get(0).toString());
    }

    assertEquals(golden.trim(), out.toString().trim());
  }

  public void testGxpWithBadUrl() throws Exception {
    assertRejected(
        PluginMessageType.DISALLOWED_URI,
        "<gxp:template name=\"Test\">"
        + "<a href=\"http://evil.com/\">hello</a>"
        + "</gxp:template>");
  }

  public void testGxpWithMalformedUrl() throws Exception {
    assertRejected(
        PluginMessageType.MALFORMED_URL,
        "<gxp:template name=\"Test\">"
        + "<a href=\"hello world\">hello</a>"
        + "</gxp:template>");
  }

  public void testGxpWithMalformedStyle() throws Exception {
    assertRejected(
        MessageType.EXPECTED_TOKEN,
        "<gxp:template name=\"Test\">"
        + "<p style=\"happy little bunnies\">hello</p>"
        + "</gxp:template>");
  }

  public void testGxpWithInvalidStyle() throws Exception {
    assertRejected(
        PluginMessageType.UNKNOWN_CSS_PROPERTY,
        "<gxp:template name=\"Test\">"
        + "<p style=\"bogus: 0\">hello</p>"
        + "</gxp:template>");
  }

  public void testGxpWithUnsafeStyle() throws Exception {
    assertRejected(
        PluginMessageType.UNSAFE_CSS_PROPERTY,
        "<gxp:template name=\"Test\">"
        + "<p style=\"content: 'badness'\">hello</p>"
        + "</gxp:template>");
  }

  public void testGxpWithUnknownTag() throws Exception {
    assertRejected(
        PluginMessageType.UNKNOWN_TAG,
        "<gxp:template name=\"Test\">"
        + "<what/>"
        + "</gxp:template>");
  }

  public void testGxpWithUnknownAttrib() throws Exception {
    assertRejected(
        PluginMessageType.UNKNOWN_ATTRIBUTE,
        "<gxp:template name=\"Test\">"
        + "<p avast=\"ye\"/>"
        + "</gxp:template>");
  }

  public void testDynamicStyles() throws Exception {
    assertRejected(
        PluginMessageType.ATTRIBUTE_CANNOT_BE_DYNAMIC,
        "<gxp:template name=\"Test\">"
        + "<p>"
        + "<gxp:attr name=\"style\">"
        + "<gxp:eval expr=\"&quot;content: 'badness'&quot;\"/>"
        + "</gxp:attr>"
        + "</p>"
        + "</gxp:template>");
  }

  public void testCallNotAllowedInsideAttribs() throws Exception {
    assertRejected(
        PluginMessageType.TAG_NOT_ALLOWED_IN_ATTRIBUTE,
        "<gxp:template name=\"Test\">"
        + "<p>"
        + "<gxp:attr name=\"title\">"
        + "<call:other/>"
        + "</gxp:attr>"
        + "</p>"
        + "</gxp:template>");
  }

  public void testDisallowTagsInsideAttr() throws Exception {
    assertRejected(
        PluginMessageType.TAG_NOT_ALLOWED_IN_ATTRIBUTE,
        "<gxp:template name=\"Test\">"
        + "<p>"
        + "<gxp:attr name=\"title\">"
        + "<b>hello</b>"
        + "</gxp:attr>"
        + "</p>"
        + "</gxp:template>");
  }

  public void testRejectUnknownGxpTags() throws Exception {
    assertRejected(
        PluginMessageType.UNKNOWN_TAG,
        "<gxp:template name=\"Test\">"
        + "<p>"
        + "<gxp:weird/>"
        + "</p>"
        + "</gxp:template>");
  }

  public void testTargetsRewritten() throws Exception {
    assertOutput(
        "out___.push('<a href=\\\"/testplugin/foo\\\" target=\\\"_new\\\">"
        + "hello</a>');",
        true,
        "<gxp:template name=\"Test\">"
        + "<a href=\"foo\" target=\"_self\">"
        + "hello"
        + "</a>"
        + "</gxp:template>");
  }

  public void testFormRewritten1() throws Exception {
    assertOutput(
        "out___.push('<form onsubmit=\\\"return false\\\"></form>');",
        true,
        "<gxp:template name=\"Test\">"
        + "<form/>"
        + "</gxp:template>");
  }

  public void testFormRewritten2() throws Exception {
    assertOutput(
        "out___.push('<form name=\\\"hi-', IMPORTS___.getIdClass___(), '\\\""
        + " onsubmit=\\\"return false\\\"></form>');",
        true,
        "<gxp:template name=\"Test\">"
        + "<form name=\"hi\"/>"
        + "</gxp:template>");
  }

  public void testUnrewritableContent() throws Exception {
    assertRejected(
        PluginMessageType.ATTRIBUTE_CANNOT_BE_DYNAMIC,
        "<gxp:template name=\"Test\">"
        + "<form expr:style=\"badStyle()\"/>"
        + "</gxp:template>");
  }

  public void testUnrewrittenExprAttrib() throws Exception {
    assertOutput(
        "out___.push('<img src=\\\"/testplugin/blank.gif\\\" width=\\\"', "
        + "screenWidth() / 2, '\\\">');",
        true,
        "<gxp:template name=\"Test\">"
        + "<img src=\"blank.gif\" expr:width=\"screenWidth() / 2\"/>"
        + "</gxp:template>");
  }

  public void testUnrewrittenAttrAttrib() throws Exception {
    assertOutput(
        "out___.push('<img src=\\\"/testplugin/blank.gif\\\" width=\\\"', "
        + "IMPORTS___.htmlAttr___(screenWidth() / 2), '\\\">');",
        true,
        "<gxp:template name=\"Test\">"
        + "<img src=\"blank.gif\">"
        + "<gxp:attr name=\"width\">"
        + "<gxp:eval expr=\"screenWidth() / 2\"/>"
        + "</gxp:attr>"
        + "</img>"
        + "</gxp:template>");
  }

  public void testAttrVoided() throws Exception {
    assertOutput(
        "out___.push('<a href=\\\"/testplugin/blank\\\""
        + " target=\\\"_new\\\">hello</a>');",
        true,
        "<gxp:template name=\"Test\">"
        + "<a href=\"blank\">"
        + "<gxp:attr name=\"target\">"
        + "_parent"
        + "</gxp:attr>"
        + "hello"
        + "</a>"
        + "</gxp:template>");
  }

  public void testMissingAttrName() throws Exception {
    assertRejected(
        PluginMessageType.MISSING_ATTRIBUTE,
        "<gxp:template name=\"Test\">"
        + "<div>"
        + "<gxp:attr>eval('badness')</gxp:attr>"
        + "</div>"
        + "</gxp:template>");
  }

  public void testCssSubstitution() throws Exception {
    assertOutput(
        "out___.push('<div style=\\\"position: absolute;\\nleft: ', "
        + "IMPORTS___.cssNumber___(x + 10), 'px;\\nright: ', "
        + "IMPORTS___.cssNumber___(x + 50), 'px\\\""
        + " id=\\\"foo-', IMPORTS___.getIdClass___(), '\\\">\\n"
        + "Hello\\n</div>');",
        true,
        "<gxp:template name=\"Test\">\n"
        + "<div style=\n"
        +   "\"position: absolute; left: ${x + 10}px; right: ${x + 50}px\"\n"
        + " id=\"foo\">\n"
        + "Hello\n"
        + "</div>\n"
        + "</gxp:template>");
  }


  private void assertRejected(MessageTypeInt typ, String... gxps)
      throws Exception {
    DomTree.Tag[] doms = new DomTree.Tag[gxps.length];
    for (int i = 0; i < gxps.length; ++i) {
      doms[i] = (DomTree.Tag) xml(fromString(gxps[i]));
    }

    StringBuilder actualBuf = new StringBuilder();
    boolean valid = compileAll(doms, true, actualBuf);
    String actual = actualBuf.toString();
    Message actmsg = null;
    MessageTypeInt act = null;
    for (Message msg : mq.getMessages()) {
      if (msg.getMessageLevel().compareTo(MessageLevel.ERROR) >= 0) {
        act = msg.getMessageType();
        actmsg = msg;
        break;
      }
    }
    assertEquals(actmsg.format(mc), typ, act);
  }

  private void assertOutput(String golden, boolean expectValid, String... gxps)
      throws Exception {
    DomTree.Tag[] doms = new DomTree.Tag[gxps.length];
    for (int i = 0; i < gxps.length; ++i) {
      doms[i] = (DomTree.Tag) xml(fromString(gxps[i]));
    }

    StringBuilder actualBuf = new StringBuilder();
    boolean valid = compileAll(doms, true, actualBuf);
    String actual = actualBuf.toString();
    if (valid) {
      // get rid of boilerplate
      String pre = (
          "{\n"
          + "  function Test() {\n"
          + "    var out___ = [ ];\n"
          + "    c1___.call(IMPORTS___, out___);\n"
          + "    return IMPORTS___.blessHtml___(out___.join(''));\n"
          + "  }\n"
          + "  function c1___(out___) {\n"
          + "    ");
      String post = "\n  }\n}";
      assertTrue(actual, actual.startsWith(pre));
      assertTrue(actual, actual.endsWith(post));
      actual = actual.substring(pre.length(), actual.length() - post.length())
               .trim();

      assertEquals(actual, golden, actual);
    }
    assertEquals(expectValid, valid);
  }

  private boolean compileAll(
      DomTree.Tag[] doms, boolean validate, StringBuilder jsOut)
      throws Exception {
    PluginMeta meta = makeTestPluginMeta();
    GxpCompiler gxpc = new GxpCompiler(
        CssSchema.getDefaultCss21Schema(mq), HtmlSchema.getDefault(mq),
        meta, mq);

    boolean valid = true;

    List<GxpCompiler.TemplateSignature> sigs
        = new ArrayList<GxpCompiler.TemplateSignature>();
    for (DomTree.Tag dom : doms) {
      if (validate
          && !(new GxpValidator(HtmlSchema.getDefault(mq), mq)
               .validate(new AncestorChain<DomTree>(dom)))) {
        valid = false;
        break;
      }
      try {
        sigs.add(gxpc.compileTemplateSignature(dom));
      } catch (CajaException ex) {
        ex.toMessageQueue(mq);
        valid = false;
        break;
      }
    }

    List<Statement> stmts = new ArrayList<Statement>();
    for (GxpCompiler.TemplateSignature sig : sigs) {
      try {
        Pair<FunctionConstructor, FunctionConstructor> compiled
            = gxpc.compileDocument(sig);
        System.err.println("compiled=" + compiled);
        stmts.add(new FunctionDeclaration(
            compiled.a.getIdentifier(), compiled.a));
        stmts.add(new FunctionDeclaration(
            compiled.b.getIdentifier(), compiled.b));
      } catch (CajaException ex) {
        ex.toMessageQueue(mq);
        valid = false;
        break;
      }
    }
    stmts.addAll(gxpc.getEventHandlers());

    jsOut.append(render(new Block(stmts)));
    return valid;
  }

  private PluginMeta makeTestPluginMeta() {
    return new PluginMeta(
        new PluginEnvironment() {
            public CharProducer loadExternalResource(
                ExternalReference ref, String mimeType) {
              return null;
            }
            public String rewriteUri(ExternalReference ref, String mimeType) {
              URI uri = ref.getUri();

              if (uri.getScheme() == null
                  && uri.getHost() == null
                  && uri.getPath() != null) {
                try {
                  String path = uri.getPath();
                  path = (path.startsWith("/") ? "/testplugin" : "/testplugin/")
                      + path;
                  return new URI(
                      null, null, path, uri.getQuery(), uri.getFragment())
                      .toString();
                } catch (URISyntaxException ex) {
                  ex.printStackTrace();
                  return null;
                }
              } else {
                return null;
              }
            }
        });
  }
}
