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
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pair;
import com.google.caja.util.TestUtil;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class GxpCompilerTest extends TestCase {

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
    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.out)), mc);
    TokenQueue<HtmlTokenType> tq = TestUtil.parseXml(
        getClass(), "gxpcompilerinput1.gxp", mq);
    DomTree.Tag domTree = (DomTree.Tag) DomParser.parseDocument(tq);
    GxpCompiler gxpc = new GxpCompiler(
        mq,
        new PluginMeta("TestPlugin", "pre", "/testplugin", "rootDiv", false));
    GxpCompiler.TemplateSignature sig = gxpc.compileTemplateSignature(domTree);
    ParseTreeNode compiled = gxpc.compileDocument(sig);

    StringBuilder out = new StringBuilder();
    RenderContext rc = new RenderContext(mc, out);
    compiled.render(rc);
    for (FunctionDeclaration handler : gxpc.getEventHandlers()) {
      rc.newLine();
      handler.render(rc);
    }
    assertEquals(golden.trim(), out.toString().trim());
  }

  public void testStyles() throws Exception {
    String golden = TestUtil.readResource(getClass(), "gxpcompilergolden2.js");
    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.out)), mc);
    TokenQueue<HtmlTokenType> tq = TestUtil.parseXml(
        getClass(), "gxpcompilerinput2.gxp", mq);
    DomTree.Tag domTree = (DomTree.Tag) DomParser.parseDocument(tq);
    GxpCompiler gxpc = new GxpCompiler(
        mq,
        new PluginMeta("TestPlugin", "pre", "/testplugin", "rootDiv", false));
    GxpCompiler.TemplateSignature sig = gxpc.compileTemplateSignature(domTree);
    ParseTreeNode compiled = gxpc.compileDocument(sig);

    StringBuilder out = new StringBuilder();
    RenderContext rc = new RenderContext(mc, out);
    compiled.render(rc);
    for (FunctionDeclaration handler : gxpc.getEventHandlers()) {
      rc.newLine();
      handler.render(rc);
    }
    assertEquals(golden.trim(), out.toString().trim());
    List<Pair<MessageTypeInt, FilePosition>> messages =
      new ArrayList<Pair<MessageTypeInt, FilePosition>>();
    for (Message m : mq.getMessages()) {
      List<MessagePart> parts = m.getMessageParts();
      messages.add(Pair.pair(m.getMessageType(), (FilePosition) parts.get(0)));
    }
    assertEquals(
        "[[DISALLOWED_URI, gxpcompilerinput2.gxp:6+21@84 - 53@116], "
        + "[REWROTE_STYLE, gxpcompilerinput2.gxp:6+8@71 - 54@117], "
        + "[UNSAFE_CSS_PROPERTY, gxpcompilerinput2.gxp:9+9@139 - 16@146], "
        + "[REWROTE_STYLE, gxpcompilerinput2.gxp:9+8@138 - 71@201], "
        + "[MALFORMED_CSS_PROPERTY_VALUE, gxpcompilerinput2.gxp:15+71@290"
          + " - 76@295], "
        + "[REWROTE_STYLE, gxpcompilerinput2.gxp:15+12@231 - 106@325]]",
        messages.toString());
  }

  /** Test the <call:templateName param0="expr"/> construct */
  public void testTemplateCalls() throws Exception {
    String golden = TestUtil.readResource(getClass(), "gxpcompilergolden3.txt");
    MessageContext mc = new MessageContext();
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(new OutputStreamWriter(System.out)), mc);

    DomTree.Tag gxp2 = (DomTree.Tag) DomParser.parseDocument(
        TestUtil.parseXml(getClass(), "gxpcompilerinput3.gxp", mq));
    DomTree.Tag gxp3 = (DomTree.Tag) DomParser.parseDocument(
        TestUtil.parseXml(getClass(), "gxpcompilerinput4.gxp", mq));
    GxpCompiler gxpc = new GxpCompiler(
        mq,
        new PluginMeta("TestPlugin", "pre", "/testplugin", "rootDiv", false));
    GxpCompiler.TemplateSignature sig2 = gxpc.compileTemplateSignature(gxp2),
                                  sig3 = gxpc.compileTemplateSignature(gxp3);

    ParseTreeNode compiled2 = gxpc.compileDocument(sig2),
                  compiled3 = gxpc.compileDocument(sig3);

    StringBuilder out = new StringBuilder();
    RenderContext rc = new RenderContext(mc, out);

    // write out the compiled gxps
    compiled2.render(rc);
    rc.newLine();
    rc.newLine();
    compiled3.render(rc);

    // write out the handler functions
    for (FunctionDeclaration handler : gxpc.getEventHandlers()) {
      rc.newLine();
      rc.newLine();
      handler.render(rc);
    }

    // write out the messages with file positions
    for (Message m : mq.getMessages()) {
      rc.newLine();
      rc.out.append(m.getMessageType().toString()).append(" : ")
        .append(m.getMessageParts().get(0).toString());
    }

    // test that they match
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
        PluginMessageType.REWROTE_STYLE,
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
        "out___.push('<form name=\\\"pre-hi\\\""
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
        + "plugin_htmlAttr___(screenWidth() / 2), '\\\">');",
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
        + "plugin_cssNumber___(x + 10), 'px;\\nright: ', "
        + "plugin_cssNumber___(x + 50), 'px\\\" id=\\\"pre-foo\\\">\\n"
        + "Hello\\n</div>');",
        true,
        "<gxp:template name=\"Test\">\n"
        + "<div style=\n"
        +   "\"position: absolute; left: $(x + 10)px; right: $(x + 50)px\"\n"
        + " id=\"foo\">\n"
        + "Hello\n"
        + "</div>\n"
        + "</gxp:template>");
  }


  private void assertRejected(MessageTypeInt typ, String... gxps)
      throws Exception {
    MessageContext mc = new MessageContext();
    MessageQueue mq = //new SimpleMessageQueue();
      new EchoingMessageQueue(
          new PrintWriter(new OutputStreamWriter(System.out)), mc);
    PluginMeta meta = new PluginMeta("TestPlugin", "pre",
        "/testplugin", "rootDiv", false);


    DomTree.Tag[] doms = new DomTree.Tag[gxps.length];
    for (int i = 0; i < gxps.length; ++i) {
      String gxp = gxps[i];
      InputSource is = new InputSource(
          new URI("test:///" + i));
      CharProducer cp = CharProducer.Factory.create(new StringReader(gxp), is);
      HtmlLexer lexer = new HtmlLexer(cp);
      lexer.setTreatedAsXml(true);
      TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
          lexer, is, Criterion.Factory.<Token<HtmlTokenType>>optimist());
      doms[i] = (DomTree.Tag) DomParser.parseDocument(tq);
      tq.expectEmpty();
    }

    GxpCompiler.TemplateSignature[] sigs =
      new GxpCompiler.TemplateSignature[doms.length];
    GxpCompiler gxpc = new GxpCompiler(mq, meta);
    boolean valid = true;
    for (int i = 0; i < doms.length; ++i) {
      DomTree.Tag dom = doms[i];
      if (!new GxpValidator(mq).validate(new AncestorChain<DomTree>(dom))) {
        valid = false;
        break;
      }
      try {
        sigs[i] = gxpc.compileTemplateSignature(dom);
      } catch (CajaException ex) {
        ex.toMessageQueue(mq);
        valid = false;
        break;
      }
    }
    if (valid) {
      ParseTreeNode[] javascripts = new ParseTreeNode[sigs.length];
      for (int i = 0; i < sigs.length; ++i) {
        GxpCompiler.TemplateSignature sig = sigs[i];
        try {
          javascripts[i] = gxpc.compileDocument(sig);
        } catch (CajaException ex) {
          ex.toMessageQueue(mq);
        ex.printStackTrace();
          valid = false;
          break;
        }
      }
      if (valid) {
        MessageLevel max = null;
        for (Message msg : mq.getMessages()) {
          if (null == max || msg.getMessageLevel().compareTo(max) > 0) {
            max = msg.getMessageLevel();
          }
        }
        if (null == max || MessageLevel.ERROR.compareTo(max) > 0) {
          for (ParseTreeNode javascript : javascripts) {
            javascript.formatTree(mc, 0, System.err);
          }
          fail("did not reject " + gxps[0]);
        }
      }
    }
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
    MessageContext mc = new MessageContext();
    MessageQueue mq = //new SimpleMessageQueue();
      new EchoingMessageQueue(
          new PrintWriter(new OutputStreamWriter(System.out)), mc);
    PluginMeta meta = new PluginMeta("TestPlugin", "pre",
        "/testplugin", "rootDiv", false);

    DomTree.Tag[] doms = new DomTree.Tag[gxps.length];
    for (int i = 0; i < gxps.length; ++i) {
      String gxp = gxps[i];
      InputSource is = new InputSource(
          new URI("test:///" + i));
      CharProducer cp = CharProducer.Factory.create(new StringReader(gxp), is);
      HtmlLexer lexer = new HtmlLexer(cp);
      lexer.setTreatedAsXml(true);
      TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
          lexer, is, Criterion.Factory.<Token<HtmlTokenType>>optimist());
      doms[i] = (DomTree.Tag) DomParser.parseDocument(tq);
      tq.expectEmpty();
    }

    GxpCompiler.TemplateSignature[] sigs =
      new GxpCompiler.TemplateSignature[doms.length];
    GxpCompiler gxpc = new GxpCompiler(mq, meta);
    boolean valid = true;
    for (int i = 0; i < doms.length; ++i) {
      DomTree.Tag dom = doms[i];
      if (!new GxpValidator(mq).validate(new AncestorChain<DomTree>(dom))) {
        valid = false;
        break;
      }
      try {
        sigs[i] = gxpc.compileTemplateSignature(dom);
      } catch (CajaException ex) {
        ex.toMessageQueue(mq);
        valid = false;
        break;
      }
    }
    if (valid) {
      ParseTreeNode[] javascripts = new ParseTreeNode[sigs.length];
      for (int i = 0; i < sigs.length; ++i) {
        GxpCompiler.TemplateSignature sig = sigs[i];
        try {
          javascripts[i] = gxpc.compileDocument(sig);
        } catch (CajaException ex) {
          ex.toMessageQueue(mq);
          ex.printStackTrace();
          valid = false;
          break;
        }
      }

      StringBuilder actualBuf = new StringBuilder();
      RenderContext rc = new RenderContext(mc, actualBuf);
      for (ParseTreeNode javascript : javascripts) {
        javascript.render(rc);
        rc.newLine();
      }

      String actual = actualBuf.toString().trim();
      // get rid of boilerplate
      String pre = "function Test() {\n  var out___ = [];\n  ",
            post = "\n  return plugin_blessHtml___(out___.join(''));\n}";
      assertTrue(actual, actual.startsWith(pre));
      assertTrue(actual, actual.endsWith(post));
      actual = actual.substring(pre.length(), actual.length() - post.length())
               .trim();

      assertEquals(actual, golden, actual);
    }
    assertEquals(expectValid, valid);
  }
}
