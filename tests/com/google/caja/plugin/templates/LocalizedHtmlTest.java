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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Nodes;
import com.google.caja.util.Strings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class LocalizedHtmlTest extends TestCase {
  public final void testEmptyMessage() throws Exception {
    runTest("", "", "");
  }
  public final void testSimpleMessage() throws Exception {
    runTest("Hello, World!", "Hello, World!", "Hello, World!");
  }
  public final void testMessageWithPlaceholder1() throws Exception {
    runTest("Hello, PLANET!", "Hello, &lt;PLANET&gt;!",
            "Hello, <ihtml:ph name='planet'/>!");
  }
  public final void testMessageWithPlaceholder2() throws Exception {
    runTest("Hello, <b>PLANET</b>!", "Hello, <b>&lt;PLANET&gt;</b>!",
            "Hello, <ihtml:ph name='b_planet'/>!");
  }
  public final void testMessageWithPlaceholders() throws Exception {
    runTest(
        "GREETING, <b>PLANET</b>!", "&lt;GREETING&gt;, <b>&lt;PLANET&gt;</b>!",
        "<ihtml:ph name='greeting'/>, <ihtml:ph name='b_planet'/>!");
  }

  private static void runTest(String golden1, String golden2, String input)
      throws Exception {
    LocalizedHtml msg = new LocalizedHtml("test", input);
    final Document doc = DomParser.makeDocument(null, null);

    DocumentFragment f1 = msg.substitute(
        doc,
        new LocalizedHtml.PlaceholderHandler() {
          public Iterator<Token<HtmlTokenType>> substitutePlaceholder(
              String placeholderName, FilePosition placeholderLoc) {
            String uname = Strings.upper(placeholderName);
            FilePosition unk = FilePosition.UNKNOWN;
            List<Token<HtmlTokenType>> toks
                = new ArrayList<Token<HtmlTokenType>>();
            if (placeholderName.startsWith("b_")) {
              toks.add(Token.instance("<b", HtmlTokenType.TAGBEGIN, unk));
              toks.add(Token.instance(">", HtmlTokenType.TAGEND, unk));
              toks.add(Token.instance(
                  uname.substring(2), HtmlTokenType.TEXT, unk));
              toks.add(Token.instance("</b", HtmlTokenType.TAGBEGIN, unk));
              toks.add(Token.instance(">", HtmlTokenType.TAGEND, unk));
            } else {
              toks.add(Token.instance(uname, HtmlTokenType.TEXT, unk));
            }
            return toks.iterator();
          }
        });
    String actual1 = Nodes.render(f1);
    assertEquals(actual1, golden1, actual1);

    // Redo to make sure that no cached state is destructively consumed.
    DocumentFragment f2 = msg.substitute(
        doc,
        new LocalizedHtml.PlaceholderHandler() {
          public Iterator<Token<HtmlTokenType>> substitutePlaceholder(
              String placeholderName, FilePosition placeholderLoc) {
            String uname = Strings.upper(placeholderName);
            FilePosition unk = FilePosition.UNKNOWN;
            List<Token<HtmlTokenType>> toks
                = new ArrayList<Token<HtmlTokenType>>();
            if (placeholderName.startsWith("b_")) {
              toks.add(Token.instance("<b", HtmlTokenType.TAGBEGIN, unk));
              toks.add(Token.instance(">", HtmlTokenType.TAGEND, unk));
              toks.add(Token.instance(
                  "<" + uname.substring(2) + ">", HtmlTokenType.TEXT, unk));
              toks.add(Token.instance("</b", HtmlTokenType.TAGBEGIN, unk));
              toks.add(Token.instance(">", HtmlTokenType.TAGEND, unk));
            } else {
              toks.add(Token.instance(
                  "<" + uname + ">", HtmlTokenType.TEXT, unk));
            }
            return toks.iterator();
          }
        });
    String actual2 = Nodes.render(f2);
    assertEquals(actual2, golden2, actual2);
  }
}
