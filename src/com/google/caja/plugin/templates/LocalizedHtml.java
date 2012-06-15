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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.TokenStream;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Nodes;
import com.google.caja.reporting.DevNullMessageQueue;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

/**
 * A snippet of Localized HTML containing human language text with placeholders.
 * Two strings with different content but different placeholders can be swapped,
 * e.g. {@code Number <ph name="index"/> of <ph name="total"/>} and
 * {@code El numero <ph name="index/> de <ph name="total"/>}.  The ordering of
 * placeholders is irrelevant: {@code <ph name="total"/> @ <ph name="index"/>}.
 *
 * @author mikesamuel@gmail.com
 */
public final class LocalizedHtml {
  private final String name;
  private final String xhtml;
  private final FilePosition pos;
  private List<Token<HtmlTokenType>> tokens;

  private static InputSource makeInputSource(String name) {
    try {
      return new InputSource(new URI("message", null, "/" + name, null, null));
    } catch (URISyntaxException ex) {
      // Authority can't be bad since there isn't one, and the path is not
      // relative, so should not throw.
      throw new SomethingWidgyHappenedError(ex);
    }
  }

  /**
   * @param name a safe identifier.
   * @param html a string of XHTML with {@code <ihtml:ph name="..."/>} marking
   *     placeholders.
   */
  public LocalizedHtml(String name, String html) {
    this(name, html, FilePosition.startOfFile(makeInputSource(name)));
  }

  public LocalizedHtml(String name, String html, FilePosition pos) {
    this.name = name;
    this.xhtml = html;
    this.pos = pos;
  }

  /**
   * The value of the "name" attribute of an <code>ihtml:message</code> element.
   */
  public String getName() { return name; }

  /**
   * A string of XHTML with placeholders marked by
   * {@code <ihtml:ph name="..."/>}.
   */
  public String getSerializedForm() { return xhtml; }

  /**
   * @param sourceDoc the document in which to create the result.
   * @return an IHTML DOM subtree where placeholders have been handled by
   *    the given handler.
   */
  public DocumentFragment substitute(
      Document sourceDoc, final PlaceholderHandler handler)
      throws ParseException {
    if (this.tokens == null) {
      List<Token<HtmlTokenType>> toks = new ArrayList<Token<HtmlTokenType>>();
      HtmlLexer lexer = new HtmlLexer(CharProducer.Factory.create(
          new StringReader(xhtml), pos));
      lexer.setTreatedAsXml(true);
      while (lexer.hasNext()) {
        toks.add(lexer.next());
      }
      this.tokens = toks;
    }
    // stitch together tokens from this.tokens with those from placeholders.
    TokenStream<HtmlTokenType> str = new TokenStream<HtmlTokenType>() {
      Iterator<Token<HtmlTokenType>> mainIt = tokens.iterator();
      Iterator<Token<HtmlTokenType>> placeholderIt;
      Token<HtmlTokenType> pending;

      public boolean hasNext() {
        return fetch();
      }

      public Token<HtmlTokenType> next() {
        if (!fetch()) { throw new NoSuchElementException(); }
        Token<HtmlTokenType> t = pending;
        pending = null;
        return t;
      }

      private boolean fetch() {
        if (pending != null) { return true; }
        do {
          if (placeholderIt != null) {
            if (placeholderIt.hasNext()) {
              pending = placeholderIt.next();
              return true;
            } else {
              placeholderIt = null;
            }
          }
          if (!mainIt.hasNext()) { return false; }
          Token<HtmlTokenType> t = mainIt.next();
          if (t.type != HtmlTokenType.TAGBEGIN) {
            pending = t;
            return true;
          }
          if ("</ihtml:ph".equals(t.text)) {
            while (mainIt.hasNext()) {
              t = mainIt.next();
              if (t.type == HtmlTokenType.TAGEND) { break; }
            }
            continue;
          }
          if (!"<ihtml:ph".equals(t.text)) {
            pending = t;
            return true;
          }
          // Consume the <ihtml:ph/> tag, and substitute the results of the
          // PlaceholderHandler in its place.
          String name = null;
          FilePosition placeholderPos = null;
          while (mainIt.hasNext()) {
            t = mainIt.next();
            if (t.type == HtmlTokenType.ATTRNAME && "name".equals(t.text)
                && mainIt.hasNext()) {
              t = mainIt.next();
              if (t.type == HtmlTokenType.ATTRVALUE) {
                name = decodeAttrValue(t.text);
                placeholderPos = t.pos;
              }
            }
            if (t.type == HtmlTokenType.TAGEND) { break; }
          }
          if (name != null) {
            placeholderIt = handler.substitutePlaceholder(name, placeholderPos);
          }
        } while (true);
      }
    };
    TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
        str, pos.source(), DomParser.SKIP_COMMENTS);
    tq.setInputRange(pos);
    return new DomParser(
        tq, true, DevNullMessageQueue.singleton()).parseFragment(sourceDoc);
  }
  private String decodeAttrValue(String tokenText) {
    int len = tokenText.length();
    if (len >= 2) {
      char ch0 = tokenText.charAt(0);
      if (ch0 == '"' || ch0 == '\'' || ch0 == tokenText.charAt(len - 1)) {
        tokenText = tokenText.substring(1, len - 1);
      }
    }
    return Nodes.decode(tokenText);
  }

  @Override
  public String toString() { return xhtml; }

  public static interface PlaceholderHandler {
    public Iterator<Token<HtmlTokenType>> substitutePlaceholder(
        String placeholderName, FilePosition placeholderLoc);
  }
}
