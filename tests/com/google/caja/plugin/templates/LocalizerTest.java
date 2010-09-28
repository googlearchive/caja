// Copyright (C) 2009 Google Inc.
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

package com.google.caja.plugin.templates;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Nodes;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.CajaTestCase;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Element;

public class LocalizerTest extends CajaTestCase {
  private Element ihtmlRoot;
  private IhtmlL10NContext messageBundle;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    ihtmlRoot = xml(fromString(
        ""
        + "<ihtml:template name='testTemplate' formals='x'>\n"
        + "  Hello, World!\n"
        + "\n"
        + "  <ihtml:message name='m1'>\n"
        + "  We come in\n"
        + "  <ihtml:ph name='declaration'/>\n"
        + "    <ihtml:dynamic expr='peaceOrWar()'"
        + "    /><ihtml:eph/>.\n"
        + "  </ihtml:message>\n"
        + "\n"
        + "  <ihtml:message name='m2'>\n"
        + "  Take me to your <ihtml:ph name='potentate'/>LEADER<ihtml:eph/>.\n"
        + "  </ihtml:message>\n"
        + "\n"
        + "  <ihtml:message name='m3'>\n"
        + "  Do you have any more of those Earth-style\n"
        + "  <ihtml:ph name='flavor'/>cheesy<ihtml:eph/>\n"
        + "  <ihtml:ph name='snackFood'/>poofs<ihtml:eph/>?\n"
        + "  </ihtml:message>\n"
        + "\n"
        + "  <ihtml:message name='m4'>\n"
        + "  <ihtml:ph name='start1'/><a href='foo'><ihtml:eph/>\n"
        + "  Link 1\n"
        + "  <ihtml:ph name='end1'/></a><ihtml:eph/>,\n"
        + "  <ihtml:ph name='start2'/><a href='bar'><ihtml:eph/>\n"
        + "  Link 2\n"
        + "  <ihtml:ph name='end2'/></a><ihtml:eph/>\n"
        + "  </ihtml:message>\n"
        + "</ihtml:template>"));
    Map<String, LocalizedHtml> messages
        = new LinkedHashMap<String, LocalizedHtml>();
    messages.put(
        "m2",
        new LocalizedHtml(
            "m2", "akeTay emay otay ouryay <ihtml:ph name='potentate'/>."));
    messages.put(
        "m3",
        new LocalizedHtml(
            "m3",
            ""
            + "ore-May <ihtml:ph name='snackFood'/> "
            + "and-ay ake-may em-thay "
            + "<ihtml:ph name='flavor'/>, "
            + "ease-play!"));
    // Swaps the order of the links.
    messages.put(
        "m4",
        new LocalizedHtml(
            "m4",
            ""
            + "<ihtml:ph name='start2'/>inkLay2<ihtml:ph name='end2'/>"
            + " ommakay "
            + "<ihtml:ph name='start1'/>inkLay1<ihtml:ph name='end1'/>"));
    messageBundle = new IhtmlL10NContext(new Locale("la", "PI"), messages);
  }

  public final void testLocalize() {
    new IhtmlSanityChecker(mq).check(ihtmlRoot);
    assertNoErrors();
    Localizer localizer = new Localizer(mq);
    localizer.localize(ihtmlRoot, messageBundle);
    assertEquals(
        ""
        + "<ihtml:template formals=\"x\" name=\"testTemplate\">\n"
        + "  Hello, World!\n"  // Not a message.
        // Untranslated.
        + "  We come in\n"
        + "    <ihtml:dynamic expr=\"peaceOrWar()\" />.\n"
        + "  akeTay emay otay ouryay LEADER.\n"
        // Out of order substitution.
        + "  ore-May poofs and-ay ake-may em-thay cheesy, ease-play!\n"
        + "  <a href=\"bar\">inkLay2</a> ommakay <a href=\"foo\">inkLay1</a>\n"
        + "</ihtml:template>",
        Nodes.render(ihtmlRoot, MarkupRenderMode.XML)
            .replaceAll("\n[ \n]*\n", "\n"));
    assertMessage(
        true, IhtmlMessageType.UNTRANSLATED_MESSAGE, MessageLevel.WARNING,
        FilePosition.fromLinePositions(is, 4, 3, 8, 19),
        MessagePart.Factory.valueOf("m1"),
        MessagePart.Factory.valueOf("la_PI"));
    assertMessagesLessSevereThan(MessageLevel.WARNING);
  }

  public final void testExtractMessages() {
    Localizer localizer = new Localizer(mq);
    IhtmlL10NContext context = localizer.extractMessages(ihtmlRoot);
    assertEquals(Locale.ENGLISH, context.getLocale());
    assertEquals(
        ""
        + "\n  We come in\n  <ihtml:ph name=\"declaration\" />."
        + "\n  ",
        context.getMessageByName("m1").getSerializedForm());
    assertEquals(
        ""
        + "\n  Take me to your <ihtml:ph name=\"potentate\" />."
        + "\n  ",
        context.getMessageByName("m2").getSerializedForm());
    assertEquals(
        ""
        + "\n  Do you have any more of those Earth-style"
        + "\n  <ihtml:ph name=\"flavor\" />"
        + "\n  <ihtml:ph name=\"snackFood\" />?"
        + "\n  ",
        context.getMessageByName("m3").getSerializedForm());
    assertNoErrors();
  }

  public final void testExtractMessageErrors() throws Exception {
    Localizer localizer = new Localizer(mq);
    IhtmlL10NContext context = localizer.extractMessages(xml(fromString(
        ""
        + "<ihtml:template name='howdy' formals='x' xml:lang='es'>\n"
        + "  <ihtml:message name='foo'>Foo</ihtml:message>\n"
        + "  <ihtml:message name='bar'>Bar</ihtml:message>\n"
        + "  <ihtml:message name='baz'>Baz</ihtml:message>\n"
        + "  <ihtml:message name='baz'>BAZ</ihtml:message>\n"
        + "</ihtml:template>"
        )));
    assertEquals(new Locale("es"), context.getLocale());
    assertEquals("Foo", context.getMessageByName("foo").getSerializedForm());
    assertEquals("Bar", context.getMessageByName("bar").getSerializedForm());
    assertEquals("Baz", context.getMessageByName("baz").getSerializedForm());
    assertMessage(
        true, IhtmlMessageType.DUPLICATE_MESSAGE, MessageLevel.ERROR,
        FilePosition.instance(is, 5, 203, 3, 45),
        MessagePart.Factory.valueOf("baz"),
        FilePosition.instance(is, 4, 155, 3, 45));
    assertNoErrors();
  }

  public final void testNonSiblingPlaceholders() throws Exception {
    Localizer localizer = new Localizer(mq);
    IhtmlL10NContext context = localizer.extractMessages(xml(fromString(
        ""
        + "<ihtml:messages>"
          + "<ihtml:message name='notSiblings'>"
            + "<ihtml:ph name='startLink'/>"
            + "<a>"
              + "<ihtml:attribute name=href>"
                + "<ihtml:dynamic expr='href'/>"
              + "</ihtml:attribute>"
              + "<ihtml:eph/>"
              + "Click"
              + "<ihtml:ph name='endLink'/>"
            + "</a>"
            + "<ihtml:eph/>"
          + "</ihtml:message>"
        + "</ihtml:messages>")));
    LocalizedHtml msg = context.getMessageByName("notSiblings");
    assertEquals(
        Nodes.render(xmlFragment(fromString(
            "<ihtml:ph name=startLink />Click<ihtml:ph name=endLink />")),
            MarkupRenderMode.XML),
        msg.getSerializedForm());
    assertEquals(
        Nodes.render(xmlFragment(fromString(
            ""
            + "<button onclick='theyClickedMe()' name='click_me'"
            + " title='click me'>"
            + "Click"
            + "</button>"
            + "<br clear='all'/>")), MarkupRenderMode.XML),
        Nodes.render(msg.substitute(
            DomParser.makeDocument(null, null),
            new LocalizedHtml.PlaceholderHandler() {
              public Iterator<Token<HtmlTokenType>> substitutePlaceholder(
                  String placeholderName, FilePosition placeholderLoc) {
                String subst;
                if (placeholderName.equals("startLink")) {
                  subst = "<button name=click_me  title='click me' "
                      + " onclick=theyClickedMe()>";
                } else if (placeholderName.equals("endLink")) {
                  subst = "</button><br clear=\"all\" />";
                } else {
                  throw new IllegalArgumentException();
                }
                final HtmlLexer tokens = new HtmlLexer(
                    CharProducer.Factory.fromString(subst, is));
                return new Iterator<Token<HtmlTokenType>>() {
                  public boolean hasNext() {
                    try {
                      return tokens.hasNext();
                    } catch (ParseException ex) {
                      throw new SomethingWidgyHappenedError(ex);
                    }
                  }

                  public Token<HtmlTokenType> next() {
                    try {
                      return tokens.next();
                    } catch (ParseException ex) {
                      throw new SomethingWidgyHappenedError(ex);
                    }
                  }

                  public void remove() {
                    throw new UnsupportedOperationException();
                  }
                };
              }
            }), MarkupRenderMode.XML));
    assertNoErrors();
  }
}
