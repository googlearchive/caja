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

import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenType;
import com.google.caja.lexer.TokenQueue.Mark;
import com.google.caja.plugin.HtmlPluginCompiler;
import com.google.caja.plugin.GxpCompiler;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageType;
import com.google.caja.parser.ParseTreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * parses a {@link DomTree} from a stream of xml tokens.
 *
 * @author mikesamuel@gmail.com
 */
public final class HtmlDomParser {

  public static DomTree parseDocument(
      TokenQueue<HtmlTokenType> tokens, HtmlPluginCompiler c)
      throws ParseException {
    ignoreTopLevelIgnorables(tokens);
    DomTree doc = parseDom(tokens, c);
    ignoreTopLevelIgnorables(tokens);
    tokens.expectEmpty();
    return doc;
  }

  private static void ignoreTopLevelIgnorables(TokenQueue<HtmlTokenType> tokens)
      throws ParseException {
    while (!tokens.isEmpty()) {
      Token<HtmlTokenType> t = tokens.peek();

      if (HtmlTokenType.IGNORABLE != t.type && HtmlTokenType.COMMENT != t.type
          && (HtmlTokenType.TEXT != t.type || !"".equals(t.text.trim()))) {
        break;
      }
      tokens.advance();
    }
  }

  private static DomTree parseDom(TokenQueue<HtmlTokenType> tokens,
                                  HtmlPluginCompiler c)
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
        end = parseElement(t, tokens, children, c);
        children = Collections.unmodifiableList(normalize(children));
        return new DomTree.Tag(children, t, end);
      case CDATA:
        return new DomTree.CData(t);
      case TEXT:
        return new DomTree.Text(t);
      case COMMENT:
        //continue;
        // There were nasty issues when the contents of a script tag were nested
        // within html comments <!-- ... -->.  Returning null here resolves them
        // by counting the comment token as a complete tag rather than just an
        // open tag.
        // TODO(ihab): Modify the markup lexer to take a set of tag names
        // whose content is CDATA, and for html, initialize it with
        // (script|style|xmp|listing).
        return null;
      default:
        throw new ParseException(new Message(
            MessageType.MALFORMED_XHTML, t.pos,
            MessagePart.Factory.valueOf(t.text)));
      }
    }
  }

  private static Token<HtmlTokenType> parseElement(
      Token<HtmlTokenType> start, TokenQueue<HtmlTokenType> tokens,
      List<DomTree> children, HtmlPluginCompiler c)
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
      if (tagName.equals("script") || tagName.equals("style")) {
        return extractTagContents(tagName, last, tokens, c);
      }
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
          // consume ignorable whitespace until we see a tagend
          while (tokens.peek().type == HtmlTokenType.IGNORABLE) {
            tokens.advance();
          }
          last = tokens.peek();
          tokens.expectToken(">");
          break;
        } else {
          DomTree dom = parseDom(tokens, c);
          if (dom != null) {
            children.add(dom);
          }
        }
      }
    }
    return last;
  }

  /**
   * When we see a <script> or <style> tag, we want to read its entire contents
   * and then route them back through the compiler appropriately before resuming
   * html parsing as normal.
   */
  private static Token<HtmlTokenType> extractTagContents(
      String tagName, Token<HtmlTokenType> last,
      TokenQueue<HtmlTokenType> tokens, HtmlPluginCompiler c)
      throws ParseException {
    FilePosition posBegin = last.pos;
    FilePosition posEnd = last.pos;
    Mark start = tokens.mark();
    Mark end = start;
    while (true) {
      last = tokens.peek();
      if (last.type == HtmlTokenType.TAGBEGIN && isClose(last) &&
          tagName.equalsIgnoreCase(tagName(last))) {
        // consume ignorable whitespace until we see a tagend
        while (true) {
          posEnd = last.pos;
          tokens.advance();
          if (tokens.peek().type != HtmlTokenType.IGNORABLE) {
            break;
          }
        }
        last = tokens.peek();
        tokens.expectToken(">");
        break;
      }
      end = tokens.mark();
      tokens.advance();
    }

    // the TokenQueue skips over some things (e.g. whitespace) so it's important
    // that we go back and extract the tag's contents from the input
    // source itself.
    FilePosition pos = FilePosition.between(posBegin, posEnd);
    String tagContent = contentBetween(start, end, tokens);

    // Remove HTML comments in the SCRIPT tag that HTML authors use to cloak
    // JavaScript from older browsers.
    tagContent = tagContent
        .replaceFirst("^(\\s*)<!--", "$1 ")
        .replaceFirst("-->(\\s*)", " $1");

    String canonicalTagName = tagName.toLowerCase();
    try {
      ParseTreeNode node = null;
      if (canonicalTagName.equals("script")) {
        node = c.parseJsString(tagContent, pos);
      } else if (canonicalTagName.equals("style")) {
        node = c.parseCssString(tagContent, pos);
      }
      c.addInput(node);
    } catch (GxpCompiler.BadContentException ex) {
      throw new ParseException(
          new Message(
              MessageType.PARSE_ERROR,
              pos,
              MessagePart.Factory.valueOf(("<" + tagName + "> tag."))),
          ex);
    }
    return last;
  }

  /**
   * Reconstructs the input text between a range of marks from a token queue.
   * @param start inclusive
   * @param end exclusive
   */
  private static <T extends TokenType> 
      String contentBetween(Mark start, Mark end, TokenQueue<T> q)
      throws ParseException {
    Mark orig = q.mark();
    q.rewind(end);
    int endChar = q.currentPosition().endCharInFile();
    q.rewind(start);
    int startChar = q.currentPosition().startCharInFile();
    
    StringBuilder sb = new StringBuilder(endChar - startChar);
    while (q.currentPosition().startCharInFile() < endChar) {
      Token t = q.pop();
      sb.append(t.text);
    }
    
    q.rewind(orig);
    return sb.toString();
  }

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

  private static boolean isClose(Token<HtmlTokenType> t) {
    return t.text.startsWith("</");
  }

  private static String tagName(Token<HtmlTokenType> t) {
    return t.text.substring(isClose(t) ? 2 : 1);
  }

  /** collapse adjacent text nodes. */
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

  private HtmlDomParser() {
    // uninstantiable
  }
}
