// Copyright (C) 2007 Google Inc.
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

package com.google.caja.parser.html;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.MessageQueue;
import java.util.List;

/**
 * Consumes SAX style events (tag name and attributes) from the
 * {@link DomParser} to build a DomTree.
 *
 * <p>
 * Instances of this class are not reusable over multiple parses.
 *
 * <p>
 * The {@link OpenElementStack.Factory Factory} class has implementations of
 * this interface for both
 * {@link OpenElementStack.Factory#createHtml5ElementStack(MessageQueue) HTML}
 *  and a trivial one for all
 * {@link OpenElementStack.Factory#createXmlElementStack() XML} including XHTML.
 *
 * @see <a href="http://www.whatwg.org/specs/web-apps/current-work/">HTML5</a>
 * @see <a href="http://www.w3.org/TR/REC-xml/">XML</a>
 * @see <a href="http://james.html5.org/parsetree.html">HTML5 Validator</a>
 * @see <a href="http://html5lib.googlecode.com/svn/trunk/testdata/">Tests</a>
 * @see <a href="http://wiki.whatwg.org/wiki/Parser_tests">More Tests</a>
 *
 * @author mikesamuel@gmail.com
 */
public interface OpenElementStack {

  /**
   * The root element.
   */
  DomTree.Fragment getRootElement();

  /**
   * Given an element name, return a canonical element name.
   * <p>
   * This API does not currently handle namespace-aware XML.
   * <p>
   * Since this method canonicalizes, it is idempotent.  It must be idempotent
   * even if the input is not canonicalizable to a name in an HTML or XML
   * schema.
   */
  String canonicalizeElementName(String elementName);

  /**
   * Given an element name, return a canonical attribute name.
   * <p>
   * This API does not currently handle namespace-aware XML.
   * <p>
   * Since this method canonicalizes, it is idempotent.  It must be idempotent
   * even if the input is not canonicalizable to a name in an HTML or XML
   * schema.
   */
  String canonicalizeAttributeName(String attributeName);

  /**
   * Records the fact that a tag has been seen, updating internal state
   *
   * @param start the token of the beginning of the tag, so {@code "<p"} for a
   *   paragraph start, {@code </p} for an end tag.
   * @param end the token of the beginning of the tag, so {@code ">"} for a
   *   paragraph start, {@code />} for an unary break tag.
   * @param attrs the attributes for the element.  This will be empty
   *   for end tags.
   */
  void processTag(Token<HtmlTokenType> start, Token<HtmlTokenType> end,
                  List<DomTree.Attrib> attrs)
      throws IllegalDocumentStateException;

  /**
   * Adds the given text node to the DOM.
   */
  void processText(Token<HtmlTokenType> text);

  /**
   * Called before parsing starts.
   *
   * @param isFragment true to parse a fragment, not a full html document.
   */
  void open(boolean isFragment);

  /**
   * Check that the document is in a consistent state, by checking that all
   * elements that need to be closed, have been properly closed.
   *
   * This method may modify the DOM, e.g. by removing ignorable text nodes from
   * the root to ensure a single document element.
   *
   * @param endOfFile position at which parsing ends.
   */
  void finish(FilePosition endOfFile)
      throws IllegalDocumentStateException;

  /**
   * Constructors.
   */
  public static final class Factory {
    public static OpenElementStack createHtml5ElementStack(MessageQueue mq) {
      return new Html5ElementStack(mq);
    }

    public static OpenElementStack createXmlElementStack() {
      return new XmlElementStack();
    }

    private Factory() {}
  }
}
