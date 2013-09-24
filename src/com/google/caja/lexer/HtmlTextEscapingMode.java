// Copyright (C) 2005 Google Inc.
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

package com.google.caja.lexer;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * From section 8.1.2.6 of http://www.whatwg.org/specs/web-apps/current-work/
 * <p>
 * The text in CDATA and RCDATA elements must not contain any
 * occurrences of the string "</" (U+003C LESS-THAN SIGN, U+002F
 * SOLIDUS) followed by characters that case-insensitively match the
 * tag name of the element followed by one of U+0009 CHARACTER
 * TABULATION, U+000A LINE FEED (LF), U+000B LINE TABULATION, U+000C
 * FORM FEED (FF), U+0020 SPACE, U+003E GREATER-THAN SIGN (>), or
 * U+002F SOLIDUS (/), unless that string is part of an escaping
 * text span.
 * </p>
 *
 * <p>
 * See also
 * http://www.whatwg.org/specs/web-apps/current-work/#cdata-rcdata-restrictions
 * for the elements which fall in each category.
 * </p>
 *
 * @author mikesamuel@gmail.com
 */
public enum HtmlTextEscapingMode {
  /**
   * Normally escaped character data that breaks around comments and tags.
   */
  PCDATA,
  /**
   * A span of text where HTML special characters are interpreted literally,
   * as in a SCRIPT tag.
   */
  CDATA,
  /**
   * A span of text and character entity references where HTML special
   * characters are interpreted literally, as in a TITLE tag.
   */
  RCDATA,
  /**
   * A span of text where HTML special characters are interpreted literally,
   * where there is no end tag.  PLAIN_TEXT runs until the end of the file.
   */
  PLAIN_TEXT,

  /**
   * Cannot contain data.
   */
  VOID,
  ;

  private static final Map<String, HtmlTextEscapingMode> ESCAPING_MODES
      = Maps.newHashMap();
  static {
    ESCAPING_MODES.put("iframe", CDATA);
    // HTML5 does not treat listing as CDATA, but HTML2 does
    // at http://www.w3.org/MarkUp/1995-archive/NonStandard.html
    // Listing is not supported by browsers.
    //ESCAPING_MODES.put("listing", CDATA);

    // Technically, only if embeds, frames, and scripts, respectively, are
    // enabled.
    ESCAPING_MODES.put("noembed", CDATA);
    ESCAPING_MODES.put("noframes", CDATA);
    ESCAPING_MODES.put("noscript", CDATA);

    // Runs till end of file.
    ESCAPING_MODES.put("plaintext", PLAIN_TEXT);

    ESCAPING_MODES.put("script", CDATA);
    ESCAPING_MODES.put("style", CDATA);

    // Textarea and Title are RCDATA, not CDATA, so decode entity references.
    ESCAPING_MODES.put("textarea", RCDATA);
    ESCAPING_MODES.put("title", RCDATA);

    ESCAPING_MODES.put("xmp", CDATA);

    // Nodes that can't contain content.
    // http://dev.w3.org/html5/spec/syntax.html#elements-0
    ESCAPING_MODES.put("area", VOID);
    ESCAPING_MODES.put("base", VOID);
    ESCAPING_MODES.put("br", VOID);
    ESCAPING_MODES.put("col", VOID);
    ESCAPING_MODES.put("command", VOID);
    ESCAPING_MODES.put("embed", VOID);
    ESCAPING_MODES.put("hr", VOID);
    ESCAPING_MODES.put("img", VOID);
    ESCAPING_MODES.put("input", VOID);
    ESCAPING_MODES.put("keygen", VOID);
    ESCAPING_MODES.put("link", VOID);
    ESCAPING_MODES.put("meta", VOID);
    ESCAPING_MODES.put("param", VOID);
    ESCAPING_MODES.put("source", VOID);
    ESCAPING_MODES.put("track", VOID);
    ESCAPING_MODES.put("wbr", VOID);

  }

  /**
   * The mode used for content following a start tag with the given name.
   */
  public static HtmlTextEscapingMode getModeForTag(String canonTagName) {
    HtmlTextEscapingMode mode = ESCAPING_MODES.get(canonTagName);
    return mode != null ? mode : PCDATA;
  }

  /**
   * True iff the content following the given tag allows escaping text
   * spans: {@code <!--&hellip;-->} that escape even things that might
   * be an end tag for the corresponding open tag.
   * @see <a href="http://dev.w3.org/html5/markup/aria/syntax.html#escaping-text-span">HTML 5</a>
   */
  public static boolean allowsEscapingTextSpan(String canonTagName) {
    // From the HTML5 spec:
    //    The text in style, script, title, and textarea elements must not have
    //    an escaping text span start that is not followed by an escaping text
    //    span end.
    // So the tags listed can contain HTML escaping text spans or things that
    // look like them, but <xmp> and <plaintext> do not admit escaping text
    // spans.
    //
    // From the HTML5 spec:
    //    A start tag whose tag name is "noscript", if the scripting flag is
    //    enabled
    //    A start tag whose tag name is one of: "noframes", "style"
    //      Follow the generic CDATA element parsing algorithm.
    //
    //    A start tag whose tag name is "noscript", if the scripting flag is
    //    disabled
    //      Insert an HTML element for the token.
    //      Switch the insertion mode to "in head noscript".
    // So the <noscript> element can contain HTML comments when scripting is
    // disabled, but otherwise behaves like the <style> element which can
    // contain escaping text spans.
    // This class assumes that scripting is not disabled, and that frames and
    // embeds are not disabled.
    return "style".equals(canonTagName) || "script".equals(canonTagName)
        || "title".equals(canonTagName) || "textarea".equals(canonTagName)
        || "noembed".equals(canonTagName) || "noscript".equals(canonTagName)
        || "noframes".equals(canonTagName);
  }

  /**
   * True if content immediately following the start tag must be treated as
   * special CDATA so that &lt;'s are not treated as starting tags, comments
   * or directives.
   */
  public static boolean isTagFollowedByLiteralContent(String canonTagName) {
    HtmlTextEscapingMode mode = getModeForTag(canonTagName);
    return mode != PCDATA && mode != VOID;
  }

  /**
   * True iff the tag cannot contain any content -- will an HTML parser consider
   * the element to have ended immediately after the start tag.
   */
  public static boolean isVoidElement(String canonTagName) {
    return getModeForTag(canonTagName) == VOID;
  }
}
