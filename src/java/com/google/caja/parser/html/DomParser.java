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

package com.google.caja.parser.html;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageType;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses a {@link DomTree} from a stream of xml tokens.
 * This is a tolerant, non-validating, parser, but does require that tags
 * be balanced, as in XML.  Since it's not validating, we don't bother to parse
 * DTDs, and so do not process external entities.
 *
 * @author mikesamuel@gmail.com
 */
public final class DomParser {

  public static DomTree parseDocument(TokenQueue<HtmlTokenType> tokens)
      throws ParseException {
    ignoreTopLevelIgnorables(tokens);
    DomTree doc = parseDom(tokens);
    ignoreTopLevelIgnorables(tokens);
    tokens.expectEmpty();
    return doc;
  }

  /**
   * Parses a snippet of markup.
   */
  public static DomTree.Fragment parseFragment(TokenQueue<HtmlTokenType> tokens)
      throws ParseException {
    List<DomTree> topLevelNodes = new ArrayList<DomTree>();
    do {
      topLevelNodes.add(parseDom(tokens));
    } while (!tokens.isEmpty());
    return new DomTree.Fragment(topLevelNodes);
  }

  /**
   * Creates a TokenQueue suitable for this class's parse methods.
   * @param asXml true to parse as XML, false as HTML.
   */
  public static TokenQueue<HtmlTokenType> makeTokenQueue(
      InputSource is, Reader in, boolean asXml) {
    return makeTokenQueue(FilePosition.startOfFile(is), in, asXml);
  }
  /**
   * Creates a TokenQueue suitable for this class's parse methods.
   * @param pos the position of the first character on in.
   * @param asXml true to parse as XML, false as HTML.
   */
  public static TokenQueue<HtmlTokenType> makeTokenQueue(
      FilePosition pos, Reader in, boolean asXml) {
    CharProducer cp = CharProducer.Factory.create(in, pos);
    HtmlLexer lexer = new HtmlLexer(cp);
    lexer.setTreatedAsXml(asXml);
    return new TokenQueue<HtmlTokenType>(lexer, pos.source());
  }

  /**
   * Skip over top level comments, and whitespace only text nodes.
   * Whitespace is significant for XML unless the schema specifies otherwise,
   * but whitespace outside the root element is not.  There is one exception
   * for whitespace preceding the prologue.
   */
  private static void ignoreTopLevelIgnorables(TokenQueue<HtmlTokenType> tokens)
      throws ParseException {
    while (!tokens.isEmpty()) {
      Token<HtmlTokenType> t = tokens.peek();

      if (!(HtmlTokenType.IGNORABLE == t.type || HtmlTokenType.COMMENT == t.type
            || (HtmlTokenType.TEXT == t.type && "".equals(t.text.trim())))) {
        break;
      }
      tokens.advance();
    }
  }

  /**
   * Parses a single top level construct, an element, or a text chunk from the
   * given queue.
   * @throws ParseException if elements are unbalanced -- sgml instead of xml
   *   attributes are missing values, or there is no top level construct to
   *   parse, or if there is a problem parsing the underlying stream.
   */
  private static DomTree parseDom(TokenQueue<HtmlTokenType> tokens)
      throws ParseException {
    while (true) {
      Token<HtmlTokenType> t = tokens.pop();
      Token<HtmlTokenType> end = t;
      switch (t.type) {
        case TAGBEGIN:
          if (isClose(t)) {
            throw new ParseException(new Message(
                MessageType.MALFORMED_XHTML, t.pos,
                MessagePart.Factory.valueOf(t.text)));
          }
          List<DomTree> children = new ArrayList<DomTree>();
          end = parseElement(t, tokens, children);
          children = Collections.unmodifiableList(normalize(children));
          return new DomTree.Tag(children, t, end);
        case CDATA:
          return new DomTree.CData(t);
        case TEXT:
        case UNESCAPED:
          return new DomTree.Text(t);
        case COMMENT:
          continue;
        default:
          throw new ParseException(new Message(
              MessageType.MALFORMED_XHTML, t.pos,
              MessagePart.Factory.valueOf(t.text)));
      }
    }
  }

  /**
   * Parses an element from a balanced start and end tag or a unary tag.
   */
  private static Token<HtmlTokenType> parseElement(
      Token<HtmlTokenType> start, TokenQueue<HtmlTokenType> tokens,
      List<DomTree> children)
      throws ParseException {
    Token<HtmlTokenType> last;
    tokloop:
    while (true) {
      last = tokens.peek();
      switch (last.type) {
      case TAGEND:
        tokens.advance();
        break tokloop;
      case ATTRNAME:
        children.add(parseAttrib(tokens));
        break;
      default:
        throw new ParseException(new Message(
            MessageType.MALFORMED_XHTML, last.pos,
            MessagePart.Factory.valueOf(last.text)));
      }
    }
    if (">".equals(last.text)) {  // look for an end tag
      String tagName = tagName(start);
      while (true) {
        last = tokens.peek();
        if (last.type == HtmlTokenType.TAGBEGIN && isClose(last)) {
          if (!tagName.equals(tagName(last))) {
            throw new ParseException(new Message(
                MessageType.MISSING_ENDTAG, last.pos,
                MessagePart.Factory.valueOf("<" + tagName + ">"),
                MessagePart.Factory.valueOf(last.text + ">")));
          }
          tokens.advance();
          // Consume ignorable whitespace until we see a tagend
          while (tokens.peek().type == HtmlTokenType.IGNORABLE) {
            tokens.advance();
          }
          last = tokens.peek();
          tokens.expectToken(">");
          break;
        } else {
          children.add(parseDom(tokens));
        }
      }
    }
    return last;
  }

  /**
   * Parses an element from a token stream.
   * @param tokens a token queue whose head is a {HtmlTokenType#ATTRNAME}
   */
  private static DomTree parseAttrib(TokenQueue<HtmlTokenType> tokens)
      throws ParseException {
    Token<HtmlTokenType> name = tokens.pop();
    Token<HtmlTokenType> value = tokens.pop();
    if (value.type != HtmlTokenType.ATTRVALUE) {
      throw new ParseException(
          new Message(MessageType.MALFORMED_XHTML,
                      value.pos, MessagePart.Factory.valueOf(value.text)));
    }
    return new DomTree.Attrib(new DomTree.Value(value), name, name);
  }

  /**
   * True iff the given tag is an end tag.
   * @param t a token with type {@link HtmlTokenType#TAGBEGIN}
   */
  private static boolean isClose(Token<HtmlTokenType> t) {
    return t.text.startsWith("</");
  }

  /** Extracts the tag name from a {@link HtmlTokenType#TAGBEGIN} token. */
  private static String tagName(Token<HtmlTokenType> t) {
    return t.text.substring(isClose(t) ? 2 : 1);
  }

  /** Collapse adjacent text nodes. */
  private static List<DomTree> normalize(List<DomTree> nodes) {
    for (int i = 0; i < nodes.size(); ++i) {
      DomTree node = nodes.get(i);
      if (HtmlTokenType.TEXT == node.getType()) {
        int j = i + 1;
        for (int n = nodes.size(); j < n; ++j) {
          if (HtmlTokenType.TEXT != nodes.get(j).getType()) { break; }
        }
        if (j - i > 1) {
          Token<HtmlTokenType> firstToken = node.getToken(),
                                lastToken = nodes.get(j - 1).getToken();
          StringBuilder newText = new StringBuilder(firstToken.text);
          for (int k = i + 1; k < j; ++k) {
            newText.append(nodes.get(k).getToken().text);
          }
          Token<HtmlTokenType> normalToken = Token.<HtmlTokenType>instance(
              newText.toString(), HtmlTokenType.TEXT,
              FilePosition.span(firstToken.pos, lastToken.pos));
          nodes.set(i, new DomTree.Text(normalToken));
          nodes.subList(i + 1, j).clear();
          i = j - 1;
        }
      }
    }
    return nodes;
  }

  private DomParser() {
    // uninstantiable
  }
}
