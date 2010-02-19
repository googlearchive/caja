// Copyright (C) 2010 Google Inc.
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

package com.google.caja.ancillary.servlet;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.util.Sets;

import java.util.Set;

/**
 * Given a string of valid HTML 5 with well balanced start and end tags,
 * removes unnecessary tags.  Assumes that all tag names are lower-cased, as is
 * the case with the output from the HTML renderer.
 *
 * @author mikesamuel@gmail.com
 */
final class HtmlReducer {
  static void reduce(String s, StringBuilder out) throws ParseException {
    CharProducer cp = CharProducer.Factory.fromString(s, InputSource.UNKNOWN);
    HtmlLexer lexer = new HtmlLexer(cp);
    lexer.setTreatedAsXml(false);
    Token<HtmlTokenType> lookahead = null;
    String lastTagSkipped = null;
    while (lexer.hasNext() || lookahead != null) {
      Token<HtmlTokenType> t;
      if (lookahead == null) {
        t = lexer.next();
      } else {
        t = lookahead;
        lookahead = null;
      }

      if (t.type != HtmlTokenType.TAGBEGIN) {
        emitToken(t, out);
        lastTagSkipped = null;
        continue;
      }

      boolean isEndTag = t.text.startsWith("</");
      String tagName = t.text.substring(isEndTag ? 2 : 1);

      // Below comments are quoted from HTML 5 section 10.1.2.4 Optional tags

      // Certain tags can be omitted.
      boolean skip = false;

      // However, a start tag must never be omitted if it has any attributes.
      Token<HtmlTokenType> t2 = lexer.hasNext() ? lexer.next() : null;
      if (t2 == null || t2.type != HtmlTokenType.TAGEND) {
        out.append(t.text);
        lastTagSkipped = null;
        lookahead = t2;
        continue;
      }

      lookahead = lexer.hasNext() ? lexer.next() : null;

      // Omitting an element's start tag does not mean the element is not
      // present; it is implied, but it is still there. An HTML document always
      // has a root html element, even if the string <html> doesn't appear
      // anywhere in the markup.

      if ("html".equals(tagName)) {
        // An html element's start tag may be omitted if the first thing inside
        // the html element is not a comment.

        // An html element's end tag may be omitted if the html element
        // is not immediately followed by a comment.
        skip = lookahead == null || lookahead.type != HtmlTokenType.COMMENT;
      } else if ("head".equals(tagName)) {
        if (!isEndTag) {
          // A head element's start tag may be omitted if the element is
          // empty, or if the first thing inside the head element is an
          // element.
          skip = lookahead == null || lookahead.type == HtmlTokenType.TAGBEGIN;
        } else {
          // A head element's end tag may be omitted if the head element
          // is not immediately followed by a space character or a
          // comment.
          skip = lookahead == null || !isSpaceOrComment(lookahead);
        }
      } else if ("body".equals(tagName)) {
        if (!isEndTag) {
          // A body element's start tag may be omitted if the element is
          // empty, or if the first thing inside the body element is not a
          // space character or a comment, except if the first thing
          // inside the body element is a script or style element.
          skip = lookahead == null || (
              !isSpaceOrComment(lookahead)
              && !"<style".equals(lookahead.text)
              && !"<script".equals(lookahead.text));
        } else {
          // A body element's end tag may be omitted if the body element
          // is not immediately followed by a comment.
          skip = lookahead == null || lookahead.type != HtmlTokenType.COMMENT;
        }
      } else if ("li".equals(tagName)) {
        // A li element's end tag may be omitted if the li element is
        // immediately followed by another li element or if there is no
        // more content in the parent element.
        skip = isEndTag && (
            lookahead == null
            || (lookahead.type == HtmlTokenType.TAGBEGIN
                && ("<li".equals(lookahead.text)
                    || lookahead.text.startsWith("</"))));
      } else if ("dt".equals(tagName) || "dd".equals(tagName)) {
        if (isEndTag) {
          // A dt element's end tag may be omitted if the dt element is
          // immediately followed by another dt element or a dd element.

          // A dd element's end tag may be omitted if the dd element is
          // immediately followed by another dd element or a dt element,
          // or if there is no more content in the parent element.
          skip = lookahead == null
             || (lookahead.type == HtmlTokenType.TAGBEGIN
                 && (DD_DL_CLOSERS.contains(lookahead.text)
                     || lookahead.text.startsWith("</")));
        }
      } else if ("p".equals(tagName)) {
        if (isEndTag) {
          // A p element's end tag may be omitted if the p element is
          // immediately followed by an address, article, aside,
          // blockquote, dir, div, dl, fieldset, footer, form, h1, h2, h3,
          // h4, h5, h6, header, hgroup, hr, menu, nav, ol, p, pre,
          // section, table, or ul, element, or if there is no more
          // content in the parent element and the parent element is not
          // an a element.
          skip = (
              lookahead == null
              || (lookahead.type == HtmlTokenType.TAGBEGIN
                  && (P_CLOSERS.contains(lookahead.text)
                      || (lookahead.text.startsWith("</")
                          && !"</a".equals(lookahead.text)))));
        }
      } else if ("rt".equals(tagName) || "rp".equals(tagName)) {
        // An rt element's end tag may be omitted if the rt element is
        // immediately followed by an rt or rp element, or if there is
        // no more content in the parent element.

        // An rp element's end tag may be omitted if the rp element is
        // immediately followed by an rt or rp element, or if there is
        // no more content in the parent element.
        if (isEndTag) {
          skip = lookahead == null
              || (lookahead.type == HtmlTokenType.TAGBEGIN
                  && (RP_RT_CLOSERS.contains(lookahead.text)
                      || lookahead.text.startsWith("</")));
        }
      } else if ("optgroup".equals(tagName)) {
        if (isEndTag) {
          // An optgroup element's end tag may be omitted if the optgroup
          // element is immediately followed by another optgroup element,
          // or if there is no more content in the parent element.
          skip = lookahead == null
              || (lookahead.type == HtmlTokenType.TAGBEGIN
                  && ("<optgroup".equals(lookahead.text)
                      || lookahead.text.startsWith("</")));
        }
      } else if ("option".equals(tagName)) {
        if (isEndTag) {
          // An option element's end tag may be omitted if the option
          // element is immediately followed by another option element, or
          // if it is immediately followed by an optgroup element, or if
          // there is no more content in the parent element.
          skip = lookahead == null
              || (lookahead.type == HtmlTokenType.TAGBEGIN
                  && ("<optgroup".equals(lookahead.text)
                      || "<option".equals(lookahead.text)
                   || lookahead.text.startsWith("</")));
        }
      } else if ("colgroup".equals(tagName)) {
        if (!isEndTag) {
          // A colgroup element's start tag may be omitted if the first
          // thing inside the colgroup element is a col element, and if
          // the element is not immediately preceded by another colgroup
          // element whose end tag has been omitted. (It can't be omitted
          // if the element is empty.)
          skip = lookahead != null && "<colgroup".equals(lookahead.text)
              && !"colgroup".equals(lastTagSkipped);
        } else {
          // A colgroup element's end tag may be omitted if the colgroup
          // element is not immediately followed by a space character or a
          // comment.
          skip = lookahead == null || !isSpaceOrComment(lookahead);
        }
      } else if ("thead".equals(tagName)) {
        if (isEndTag) {
          // A thead element's end tag may be omitted if the thead element
          // is immediately followed by a tbody or tfoot element.
          skip = lookahead != null && THEAD_CLOSERS.contains(lookahead.text);
        }
      } else if ("tbody".equals(tagName)) {
        if (!isEndTag) {
          // A tbody element's start tag may be omitted if the first thing
          // inside the tbody element is a tr element, and if the element
          // is not immediately preceded by a tbody, thead, or tfoot
          // element whose end tag has been omitted. (It can't be omitted
          // if the element is empty.)
          skip = lookahead != null && "<tr".equals(lookahead.text)
              && !TBODY_OPENERS.contains(lastTagSkipped);
        } else {
          // A tbody element's end tag may be omitted if the tbody element
          // is immediately followed by a thead or tfoot element, or if
          // there is no more content in the parent element.
          skip = lookahead == null || TBODY_CLOSERS.contains(lookahead.text)
              || lookahead.text.startsWith("</");
        }
      } else if ("tfoot".equals(tagName)) {
        if (isEndTag) {
          // A tfoot element's end tag may be omitted if the tfoot element
          // is immediately followed by a tbody element, or if there is no
          // more content in the parent element.
          skip = lookahead == null || "<tbody".equals(lookahead.text)
              || lookahead.text.startsWith("</");
        }
      } else if ("tr".equals(tagName)) {
        if (isEndTag) {
          // A tr element's end tag may be omitted if the tr element is
          // immediately followed by another tr element, or if there is no
          // more content in the parent element.
          skip = lookahead == null || "<tr".equals(lookahead.text)
             || lookahead.text.startsWith("</");
        }
      } else if ("td".equals(tagName) || "th".equals(tagName)) {
        if (isEndTag) {
          // A td element's end tag may be omitted if the td element is
          // immediately followed by a td or th element, or if there is no
          // more content in the parent element.

          // A th element's end tag may be omitted if the th element is
          // immediately followed by a td or th element, or if there is no
          // more content in the parent element.
          skip = lookahead == null || "<td".equals(lookahead.text)
              || "<th".equals(lookahead.text)
              || lookahead.text.startsWith("</");
        }
      }

      if (!skip) {
        out.append(t.text);
        emitToken(t2, out);
        lastTagSkipped = null;
      } else {
        lastTagSkipped = tagName;
      }
    }
  }

  private static final Set<String> DD_DL_CLOSERS = Sets.immutableSet(
      "<dd", "<dt");

  private static final Set<String> RP_RT_CLOSERS = Sets.immutableSet(
      "<rp", "<rt");

  private static final Set<String> P_CLOSERS = Sets.immutableSet(
      "<address", "<article", "<aside",
      "<blockquote", "<dir", "<div", "<dl", "<fieldset", "<footer", "<form", "<h1", "<h2", "<h3",
      "<h4", "<h5", "<h6", "<header", "<hgroup", "<hr", "<menu", "<nav", "<ol", "<p", "<pre",
      "<section", "<table", "<ul");
  private static final Set<String> THEAD_CLOSERS = Sets.immutableSet(
      "<tfoot", "<tbody");
  private static final Set<String> TBODY_OPENERS = Sets.immutableSet(
      "tbody", "thead", "tfoot");
  private static final Set<String> TBODY_CLOSERS = Sets.immutableSet(
      "<tfoot", "<thead");

  private static boolean isSpaceOrComment(Token<HtmlTokenType> t) {
    if (t.type == HtmlTokenType.COMMENT) { return true; }
    if (t.type == HtmlTokenType.TEXT) {
      return Character.isWhitespace(t.text.charAt(0));
    }
    return false;
  }

  private static void emitToken(Token<HtmlTokenType> t, StringBuilder out) {
    switch (t.type) {
      case ATTRNAME: out.append(' '); break;
      case ATTRVALUE: out.append('='); break;
      case TAGEND:
        if (t.text.charAt(0) == '/') { out.append(' '); }
        break;
      default: break;
    }
    out.append(t.text);
  }
}
