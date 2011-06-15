// Copyright (C) 2011 Google Inc.
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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenType;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.util.ContentType;
import com.google.caja.util.Strings;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.w3c.dom.Element;


/**
 * A strategy for semantics preserving changes to a block of JavaScript to allow
 * it to be rendered in {@link MarkupRenderMode#HTML HTML mode}.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
@ParametersAreNonnullByDefault
public final class CdataScriptContentFixer {

  /**
   * HTML 5 requires that {@code <script>} elements' bodies have balanced
   * escaping text spans.  There is valid JavaScript content that does not.
   * This strategy can fix some of these.
   *
   * @param el an element whose content contains invalid escaping text spans.
   * @param cdataContent the concatenation of el's text nodes' node values.
   * @return {@code null} to indicate cannot fix or
   *     {@code textNode.getNodeValue()} transformed to fix the escaping text
   *     spans.
   * @see MarkupFixupRenderContext
   */
  public static @Nullable String fixUnclosableCdataElement(
      Element el, String cdataContent) {
    // Only muck with script elements.
    // Assume HTML.
    if (!Strings.equalsIgnoreCase("script", el.getLocalName())) {
      return null;
    }
    // Only muck with Javascript script elements.
    String type = el.getAttributeNS(Namespaces.HTML_NAMESPACE_URI, "type");
    if ("".equals(type)) { type = el.getAttribute("type"); }
    if (!"".equals(type)
        && ContentType.JS != ContentType.fromMimeType(type)) {
      return null;
    }
    // Lex the body, making three changes.
    // (1) We insert spaces between tokens so that <!-- becomes
    //     < ! -- and --> becomes -- >.
    // (2) We drop comments such as those in
    //     <script>...//--></script>
    //     We take care not to muck with comments that could include
    //     conditional compilation directives.
    // (3) We rework string bodies so "</script>" becomes "<\/script>",
    //     "<!--" becomes "<\!--", and similarly for "-->".
    // TODO: regular expression literal bodies.
    StringBuilder sb = new StringBuilder(cdataContent.length() + 16);
    JsLexer lexer = new JsLexer(CharProducer.Factory.fromString(
        cdataContent, InputSource.UNKNOWN), false);
    try {
      int lastLine = 0;
      while (lexer.hasNext()) {
        Token<JsTokenType> tok = lexer.next();
        String text = tok.text;
        if (sb.length() != 0) {
          sb.append(lastLine != tok.pos.startLineNo() ? '\n' : ' ');
        }
        lastLine = tok.pos.endLineNo();
        if (tok.type == JsTokenType.COMMENT) {
          if (tok.text.indexOf('@') < 0) {
            // Skip comments that don't contain conditional compilation.
            // The lastLine check above will turn multiline block comments
            // into newlines.
            continue;
          }
        } else if (tok.type == JsTokenType.STRING) {
          int pos = 0;
          int n = text.length();
          char last = text.charAt(0);
          for (int i = 1; i < n; ++i) {
            char ch = text.charAt(i);
            if ((last == '<' && (ch == '!' || ch == '/'))
                || (last == '-' && ch == '>')) {
              sb.append(text, 0, i).append('\\');
              pos = i;
            }
            last = ch;
          }
          sb.append(text, pos, n);
          continue;
        }
        sb.append(tok.text);
      }
    } catch (ParseException ex) {
      return null;  // Invalid JS
    }
    return sb.toString();
  }

  private CdataScriptContentFixer() { /* uninstantiable */ }

}
