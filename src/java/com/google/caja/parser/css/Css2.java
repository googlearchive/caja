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

package com.google.caja.parser.css;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.util.Criterion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Definitions of CSS properties and symbols.
 *
 * @author mikesamuel@gmail.com
 */
public final class Css2 {
  private static final Map<String, CssPropertyInfo> PROPERTIES =
    new HashMap<String, CssPropertyInfo>();
  private static final Map<String, SymbolInfo> SYMBOLS =
    new HashMap<String, SymbolInfo>();
  private static final Set<String> KEYWORDS = new HashSet<String>();

  /**
   * Returns the signature and other information for a css property.
   * @param propertyName non null.
   * @return null if no such property.
   */
  public static CssPropertyInfo getCssProperty(String propertyName) {
    return PROPERTIES.get(propertyName);
  }

  /**
   * Returns the signature for a css symbol.
   * Not all symbols can be defined in terms of a signature.
   * @param symbolName non null.
   * @return null if no such symbol or the symbol is not defined in terms of a
   *   signature.
   */
  public static SymbolInfo getSymbol(String symbolName) {
    return SYMBOLS.get(symbolName);
  }

  /** Is the given word a css keyword? */
  public static boolean isKeyword(String name) {
    return KEYWORDS.contains(name.toLowerCase());
  }

  public static boolean isMediaType(String mediaType) {
    return ALL_MEDIA.accept(mediaType);
  }

  /**
   * Encapsulates a css property and its signatures.
   */
  public static final class CssPropertyInfo extends SymbolInfo {
    public final Criterion<String> mediaGroups;
    public final boolean inherited;
    public final Criterion<String> appliesTo;
    public final String defaultValue;

    private CssPropertyInfo(
        String name, CssPropertySignature sig, Criterion<String> mediaGroups,
        boolean inherited, Criterion<String> appliesTo, String defaultValue) {
      super(name, sig);
      this.mediaGroups = mediaGroups;
      this.inherited = inherited;
      // Not defensively copied.  This is usually an immutable AllSet.
      this.appliesTo = appliesTo;
      this.defaultValue = defaultValue;
    }
  }

  /**
   * Encapsulates a css symbol.  A symbol is a non top-level entity in a
   * declaration.  Symbols include things like &lt;color&gt;, &lt;uri&gt;,
   * &lt;integer&gt;, etc.
   */
  public static class SymbolInfo {
    public final String name;
    public final CssPropertySignature sig;

    private SymbolInfo(String name, CssPropertySignature sig) {
      this.name = name;
      this.sig = sig;
    }
  }

  // TODO(mikesamuel): Is there any value in enumerating elements?
  // Perhaps by using HTML4?
  private static final Pattern HTML_IDENTIFIER = Pattern.compile("^[\\w\\-]+$");
  private static final Pattern CSS_IDENTIFIER =
    Pattern.compile("^[a-zA-Z][\\w\\-]*$");
  private static final Criterion<String> ALL_ELEMENTS
      = new RegexpCriterion(HTML_IDENTIFIER);
  // See http://www.w3.org/TR/REC-CSS2/media.html section 7.3
  private static final Criterion<String> ALL_MEDIA = in(
      "all", "aural", "braille", "embossed", "handheld", "print", "projection",
      "screen", "tty", "tv");

  private static Criterion<String> in(String... elementGroups) {
    final Set<String> elements = new HashSet<String>();
    elements.addAll(Arrays.asList(elementGroups));
    return new Criterion<String>() {
      public boolean accept(String s) {
        return elements.contains(s);
      }
    };
  }

  private static Criterion<String> notIn(String... elementGroups) {
    final Set<String> elements = new HashSet<String>();
    elements.addAll(Arrays.asList(elementGroups));
    return new Criterion<String>() {
      public boolean accept(String s) {
        return !elements.contains(s);
      }
    };
  }

  private static void defineProperty(
      String name,
      String sig,
      String defaultValue,
      Criterion<String> appliesTo,
      boolean inherited,
      Criterion<String> mediaGroups) {
    defineProperty(new String[] { name }, sig, defaultValue, appliesTo,
                   inherited, mediaGroups);
  }

  private static void defineProperty(
      String[] names,
      String sig,
      String defaultValue,
      Criterion<String> appliesTo,
      boolean inherited,
      Criterion<String> mediaGroups) {
    assert !"".equals(defaultValue);  // should use null

    CssPropertySignature csssig = parseSignature(names[0], sig);
    for (String name : names) {
      assert CSS_IDENTIFIER.matcher(name).matches();
      PROPERTIES.put(name, new CssPropertyInfo(
          name, csssig, mediaGroups, inherited, appliesTo, defaultValue));
    }
  }

  private static void defineSymbol(String name, String sig) {
    defineSymbol(new String[] { name }, sig);
  }

  private static void defineSymbol(String[] names, String sig) {
    CssPropertySignature csssig = parseSignature(names[0], sig);
    for (String name : names) {
      assert CSS_IDENTIFIER.matcher(name).matches();
      SYMBOLS.put(name, new SymbolInfo(name, csssig));
    }
  }

  // Common media group criteria
  private static final Criterion<String> AURAL = in("aural");
  private static final Criterion<String> VISUAL = in("visual");
  private static final Criterion<String> VISUAL_PAGED = in("visual", "pages");
  private static final Criterion<String> VISUAL_INTERACTIVE
      = in("visual", "interactive");


  static {
    defineProperty(
        "azimuth",
        "<angle> | [[ left-side | far-left | left | center-left | center"
        + " | center-right | right | far-right | right-side ] || behind ]"
        + " | leftwards | rightwards | inherit",
        "center",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "background-attachment",
        "scroll | fixed | inherit",
        "scroll",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "background-color",
        "<color> | transparent | inherit",
        "transparent",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "background-image",
        "<uri> | none | inherit",
        "none",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "background-position",
        "[ [ <percentage> | <length> | left | center | right ] [ <percentage>"
        + " | <length> | top | center | bottom ]? ]"
        + " | [ [ left | center | right ] || [ top | center | bottom ] ]"
        + " | inherit",
        "0% 0%",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "background-repeat",
        "repeat | repeat-x | repeat-y | no-repeat | inherit",
        "repeat",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "background",
        "['background-color' || 'background-image' || 'background-repeat'"
        + " || 'background-attachment' || 'background-position'] | inherit",
        "see individual properties",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "border-collapse",
        "collapse | separate | inherit",
        "separate",
        in("table", "inline-table"),
        true,
        VISUAL);
    defineProperty(
        "border-color",
        "[ <color> | transparent ]{1,4} | inherit",
        "see individual properties",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "border-spacing",
        "<length> <length>? | inherit",
        "0",
        in("table", "inline-table"),
        true,
        VISUAL);
    defineProperty(
        "border-style",
        "<border-style>{1,4} | inherit",
        "see individual properties",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        new String[] {
          "border-top",  "border-right", "border-bottom", "border-left" },
        "[ <border-width> || <border-style> || 'border-top-color' ] | inherit",
        "see individual properties",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        new String[] {
          "border-top-color", "border-right-color",
          "border-bottom-color", "border-left-color" },
        "<color> | transparent | inherit",
        "the value of the 'color' property",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        new String[] {
          "border-top-style", "border-right-style",
          "border-bottom-style", "border-left-style" },
        "<border-style> | inherit",
        "none",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        new String[] {
          "border-top-width", "border-right-width",
          "border-bottom-width", "border-left-width" },
        "<border-width> | inherit",
        "medium",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "border-width",
        "<border-width>{1,4} | inherit",
        "see individual properties",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "border",
        "[ <border-width> || <border-style> || 'border-top-color' ] | inherit",
        "see individual properties",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "bottom",
        "<length> | <percentage> | auto | inherit",
        "auto",
        in("positioned"),
        true,
        VISUAL);
    defineProperty(
        "caption-side",
        "top | bottom | inherit",
        "top",
        in("table-caption"),
        true,
        VISUAL);
    defineProperty(
        "clear",
        "none | left | right | both | inherit",
        "none",
        in("block"),
        true,
        VISUAL);
    defineProperty(
        "clip",
        "<shape> | auto | inherit",
        "auto",
        in("absolutely-positioned"),
        true,
        VISUAL);
    defineProperty(
        "color",
        "<color> | inherit",
        "depends on user agent",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "content",
        "normal | none | [ <string> | <uri> | <counter> | attr(<identifier>)"
        + " | open-quote | close-quote | no-open-quote | no-close-quote ]+"
        + " | inherit",
        "normal",
        in(":before pseudo", ":after pseudo"),
        true,
        ALL_MEDIA);
    defineProperty(
        "counter-increment",
        "[ <identifier> <integer>? ]+ | none | inherit",
        "none",
        ALL_ELEMENTS,
        true,
        ALL_MEDIA);
    defineProperty(
        "counter-reset",
        "[ <identifier> <integer>? ]+ | none | inherit",
        "none",
        ALL_ELEMENTS,
        true,
        ALL_MEDIA);
    defineProperty(
        "cue-after",
        "<uri> | none | inherit",
        "none",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "cue-before",
        "<uri> | none | inherit",
        "none",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "cue",
        "[ 'cue-before' || 'cue-after' ] | inherit",
        "see individual properties",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "cursor",
        "[ [<uri> ,]* [ auto | crosshair | default | pointer | move | e-resize"
        + " | ne-resize | nw-resize | n-resize | se-resize | sw-resize"
        + " | s-resize | w-resize | text | wait | help | progress ] ]"
        + " | inherit",
        "auto",
        ALL_ELEMENTS,
        true,
        VISUAL_INTERACTIVE);
    defineProperty(
        "direction",
        "ltr | rtl | inherit",
        "ltr",
        ALL_ELEMENTS,  // but see prose
        true,
        VISUAL);
    defineProperty(
        "display",
        "inline | block | list-item | run-in | inline-block | table"
        + " | inline-table | table-row-group | table-header-group"
        + " | table-footer-group | table-row | table-column-group"
        + " | table-column | table-cell | table-caption | none | inherit",
        "inline",
        ALL_ELEMENTS,
        true,
        ALL_MEDIA);
    defineProperty(
        "elevation",
        "<angle> | below | level | above | higher | lower | inherit",
        "level",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "empty-cells",
        "show | hide | inherit",
        "show",
        in("table-cell"),
        true,
        VISUAL);
    defineProperty(
        "float",
        "left | right | none | inherit",
        "none",
        ALL_ELEMENTS, // but see 9.7,
        true,
        VISUAL);
    defineProperty(
        "font-family",
        "[[ <family-name> | <generic-family> ]"
        + " [, <family-name>| <generic-family>]* ] | inherit",
        "depends on user agent",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "font-size",
        "<absolute-size> | <relative-size> | <length:0,> | <percentage:0,>"
        + " | inherit",
        "medium",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "font-style",
        "normal | italic | oblique | inherit",
        "normal",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "font-variant",
        "normal | small-caps | inherit",
        "normal",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "font-weight",
        "normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600"
        + " | 700 | 800 | 900 | inherit",
        "normal",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "font",
        "[ [ 'font-style' || 'font-variant' || 'font-weight' ]? 'font-size'"
        + " [ / 'line-height' ]? 'font-family' ] | caption | icon | menu"
        + " | message-box | small-caption | status-bar | inherit",
        "see individual properties",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "height",
        "<length> | <percentage> | auto | inherit",
        "auto",
        // but non-replaced inline elements, table columns, and column groups.
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "left",
        "<length> | <percentage> | auto | inherit",
        "auto",
        in("positioned"),
        true,
        VISUAL);
    defineProperty(
        "letter-spacing",
        "normal | <length:0,> | inherit",
        "normal",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "line-height",
        "normal | <number:0,> | <length:0,> | <percentage:0,> | inherit",
        "normal",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "list-style-image",
        "<uri> | none | inherit",
        "none",
        in("display: list-item"),
        true,
        VISUAL);
    defineProperty(
        "list-style-position",
        "inside | outside | inherit",
        "outside",
        in("display: list-item"),
        true,
        VISUAL);
    defineProperty(
        "list-style-type",
        "disc | circle | square | decimal | decimal-leading-zero | lower-roman"
        + " | upper-roman | lower-greek | lower-latin | upper-latin | armenian"
        + " | georgian | lower-alpha | upper-alpha | none | inherit",
        "disc",
        in("display: list-item"),
        true,
        VISUAL);
    defineProperty(
        "list-style",
        "[ 'list-style-type' || 'list-style-position' || 'list-style-image' ]"
        + " | inherit",
        "see individual properties",
        in("display: list-item"),
        true,
        VISUAL);
    defineProperty(
        new String[] { "margin-right", "margin-left" },
        "<margin-width> | inherit",
        "0",
        // All elements except elements with table display types other than
        // table-caption, table and inline-table
        // TODO(mikesamuel): is that a double negative?
        in("table-caption", "table", "inline-table"),
        true,
        VISUAL);
    defineProperty(
        new String[] { "margin-top", "margin-bottom" },
        "<margin-width> | inherit",
        "0",
        in("table-caption", "table", "inline-table"),
        true,
        VISUAL);
    defineProperty(
        "margin",
        "<margin-width>{1,4} | inherit",
        "see individual properties",
        in("table-caption", "table", "inline-table"),
        true,
        VISUAL);
    defineProperty(
        "max-height",
        "<length:0,> | <percentage:0,> | none | inherit",
        "none",
        // TODO(mikesamuel): another double negative?
        // All elements but non-replaced inline elements, table columns,
        // and column groups
        in("inline", "table column", "column group"),
        true,
        VISUAL);
    defineProperty(
        "max-width",
        "<length:0,> | <percentage:0,> | none | inherit",
        "none",
        in("inline", "table row", "row group"),
        true,
        VISUAL);
    defineProperty(
        "min-height",
        "<length:0,> | <percentage:0,> | inherit",
        "0",
        in("inline", "table column", "column group"),
        true,
        VISUAL);
    defineProperty(
        "min-width",
        "<length:0,> | <percentage:0,> | inherit",
        "0",
        in("inline", "table row", "row group"),
        true,
        VISUAL);
    defineProperty(
        "orphans",
        "<integer:0,> | inherit",
        "2",
        in("block"),
        true,
        VISUAL_PAGED);
    defineProperty(
        "outline-color",
        "<color> | invert | inherit",
        "invert",
        ALL_ELEMENTS,
        true,
        VISUAL_INTERACTIVE);
    defineProperty(
        "outline-style",
        "<border-style> | inherit",
        "none",
        ALL_ELEMENTS,
        true,
        VISUAL_INTERACTIVE);
    defineProperty(
        "outline-width",
        "<border-width> | inherit",
        "medium",
        ALL_ELEMENTS,
        true,
        VISUAL_INTERACTIVE);
    defineProperty(
        "outline",
        "[ 'outline-color' || 'outline-style' || 'outline-width' ] | inherit",
        "see individual properties",
        ALL_ELEMENTS,
        true,
        VISUAL_INTERACTIVE);
    defineProperty(
        "overflow",
        "visible | hidden | scroll | auto | inherit",
        "visible",
        in("block", "table cell", "inline block"),
        true,
        VISUAL);
    defineProperty(
        new String[] {
          "padding-top", "padding-right", "padding-bottom", "padding-left" },
        "<padding-width> | inherit",
        "0",
        notIn(
            "table-row-group", "table-header-group", "table-footer-group",
            "table-column", "table-column-group", "table-row"),
        true,
        VISUAL);
    defineProperty(
        "padding",
        "<padding-width>{1,4} | inherit",
        "see individual properties",
        notIn(
            "table-row-group", "table-header-group", "table-footer-group",
            "table-column", "table-column-group", "table-row"),
        true,
        VISUAL);
    defineProperty(
        "page-break-after",
        "auto | always | avoid | left | right | inherit",
        "auto",
        in("block"),
        true,
        VISUAL_PAGED);
    defineProperty(
        "page-break-before",
        "auto | always | avoid | left | right | inherit",
        "auto",
        in("block"),
        true,
        VISUAL_PAGED);
    defineProperty(
        "page-break-inside",
        "avoid | auto | inherit",
        "auto",
        in("block"),
        true,
        VISUAL_PAGED);
    defineProperty(
        "pause-after",
        "<time> | <percentage> | inherit",
        "0",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "pause-before",
        "<time> | <percentage> | inherit",
        "0",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "pause",
        "[ [<time> | <percentage>]{1,2} ] | inherit",
        "see individual properties",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "pitch-range",
        "<number> | inherit",
        "50",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "pitch",
        "<frequency> | x-low | low | medium | high | x-high | inherit",
        "medium",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "play-during",
        "<uri> [ mix || repeat ]? | auto | none | inherit",
        "auto",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "position",
        "static | relative | absolute | fixed | inherit",
        "static",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "quotes",
        "[<string> <string>]+ | none | inherit",
        "depends on user agent",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "richness",
        "<number> | inherit",
        "50",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "right",
        "<length> | <percentage> | auto | inherit",
        "auto",
        in("positioned"),
        true,
        VISUAL);
    defineProperty(
        "speak-header",
        "once | always | inherit",
        "once",
        ALL_ELEMENTS, // TODO: elements that have table header information,
        true,
        AURAL);
    defineProperty(
        "speak-numeral",
        "digits | continuous | inherit",
        "continuous",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "speak-punctuation",
        "code | none | inherit",
        "none",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "speak",
        "normal | none | spell-out | inherit",
        "normal",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "speech-rate",
        "<number> | x-slow | slow | medium | fast | x-fast | faster | slower"
        + " | inherit",
        "medium",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "stress",
        "<number> | inherit",
        "50",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "table-layout",
        "auto | fixed | inherit",
        "auto",
        in("table", "inline-table"),
        true,
        VISUAL);
    defineProperty(
        "text-align",
        "left | right | center | justify | inherit",
        null, // 'left' if 'direction' is 'ltr'; 'right' if 'direction' is 'rtl'
        in("block", "table cell", "inline block"),
        true,
        VISUAL);
    defineProperty(
        "text-decoration",
        "none | [ underline || overline || line-through || blink ] | inherit",
        "none",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "text-indent",
        "<length> | <percentage> | inherit",
        "0",
        in("block", "table cell", "inline block"),
        true,
        VISUAL);
    defineProperty(
        "text-transform",
        "capitalize | uppercase | lowercase | none | inherit",
        "none",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "top",
        "<length> | <percentage> | auto | inherit",
        "auto",
        in("positioned"),
        true,
        VISUAL);
    defineProperty(
        "unicode-bidi",
        "normal | embed | bidi-override | inherit",
        "normal",
        ALL_ELEMENTS,  // but see prose
        true,
        VISUAL);
    defineProperty(
        "vertical-align",
        "baseline | sub | super | top | text-top | middle | bottom"
        + " | text-bottom | <percentage> | <length> | inherit",
        "baseline",
        in("inline", "table-cell"),
        true,
        VISUAL);
    defineProperty(
        "visibility",
        "visible | hidden | collapse | inherit",
        "visible",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "voice-family",
        "[[<specific-voice> | <generic-voice> ],]*"
        + " [<specific-voice> | <generic-voice> ] | inherit",
        "depends on user agent",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "volume",
        "<number:0,> | <percentage:0,> | silent | x-soft | soft | medium"
        + " | loud | x-loud | inherit",
        "medium",
        ALL_ELEMENTS,
        true,
        AURAL);
    defineProperty(
        "white-space",
        "normal | pre | nowrap | pre-wrap | pre-line | inherit",
        "normal",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "widows",
        "<integer:0,> | inherit",
        "2",
        in("block"),
        true,
        VISUAL_PAGED);
    defineProperty(
        "width",
        "<length:0,> | <percentage:0,> | auto | inherit",
        "auto",
        notIn("inline", "table row", "row group"),
        true,
        VISUAL);
    defineProperty(
        "word-spacing",
        "normal | <length:0,> | inherit",
        "normal",
        ALL_ELEMENTS,
        true,
        VISUAL);
    defineProperty(
        "z-index",
        "auto | <integer:0,> | inherit",
        "auto",
        in("positioned"),
        true,
        VISUAL);

    // TODO(msamuel): What about http://www.w3.org/TR/REC-CSS2/descidx.html?

    // http://www.w3.org/TR/CSS21/fonts.html#propdef-font-size
    defineSymbol(
        "absolute-size",
        "xx-small|x-small|small|medium|large|x-large|xx-large");
    // http://www.w3.org/TR/REC-CSS2/box.html#value-def-border-style
    defineSymbol(
        "border-style",
        "none|hidden|dotted|dashed|solid|double|groove|ridge|inset|outset");
    defineSymbol("border-width", "thin|medium|thick|<length>");
    // http://www.w3.org/TR/REC-CSS2/syndata.html#value-def-color
    defineSymbol(
        "color",
        "<hex-color>|aqua|black|blue|fuchsia|gray|green|lime|maroon|navy|olive"
        + "|purple|red|silver|teal|white|yellow|rgb(<red>,<green>,<blue>)");
    // TODO(msamuel): http://www.w3.org/TR/REC-CSS2/ui.html#system-colors

    // http://www.w3.org/TR/CSS21/fonts.html#value-def-family-name
    defineSymbol("generic-family",
                 "serif|sans-serif|cursive|fantasy|monospace");
    // http://www.w3.org/TR/CSS21/fonts.html#propdef-font-size
    defineSymbol("relative-size", "smaller|larger");
    // http://www.w3.org/TR/CSS21/aural.html#value-def-generic-voice
    defineSymbol("generic-voice", "male|female|child");
    // http://www.w3.org/TR/CSS21/box.html#value-def-margin-width
    defineSymbol("margin-width", "<length>|<percentage>|auto");
    // http://www.w3.org/TR/CSS21/box.html#value-def-margin-width
    defineSymbol("padding-width", "<length:0,>|<percentage:0,>");
    // http://www.w3.org/TR/CSS21/syndata.html#value-def-counter
    defineSymbol(
        "counter",
        "counter(<identifier>) | counter(<identifier>, <list-style-type>)");
    // http://www.w3.org/TR/CSS21/visufx.html#value-def-shape
    defineSymbol("shape", "rect(<top>, <right>, <bottom>, <left>)");
    defineSymbol(new String[] { "top", "right", "bottom", "left" },
                 "<length>|auto");
    defineSymbol(new String[] { "red", "green", "blue" },
                 "<integer:0,255>|<percentage:0,100>");

    // Examine the property signatures and extract a list of keywords
    for (CssPropertyInfo pi : PROPERTIES.values()) {
      pi.sig.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode n = ancestors.node;
          if (n instanceof CssPropertySignature.LiteralSignature) {
            String kw = ((CssPropertySignature.LiteralSignature) n).value;
            KEYWORDS.add(kw.toLowerCase());
          }
          return true;
        }
      }, null);
    }
    for (SymbolInfo si : SYMBOLS.values()) {
      si.sig.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode n = ancestors.node;
          if (n instanceof CssPropertySignature.LiteralSignature) {
            String kw = ((CssPropertySignature.LiteralSignature) n).value;
            KEYWORDS.add(kw.toLowerCase());
          }
          return true;
        }
      }, null);
    }
    KEYWORDS.add("initial");
  }

  private static class RegexpCriterion implements Criterion<String> {
    private final Pattern p;
    RegexpCriterion(Pattern p) {
      this.p = p;
    }

    public boolean accept(String s) {
      return s != null && p.matcher(s).matches();
    }
  }

  /**
   * Parses a CssSignature according to the grammar described in
   * http://www.w3.org/TR/CSS21/about.html#property-defs
   */
  private static CssPropertySignature parseSignature(String name, String sig) {
    try {
      return CssPropertySignature.Parser.parseSignature(sig);
    } catch (RuntimeException ex) {
      throw new RuntimeException(
          "Error parsing symbol " + name + " with signature " + sig, ex);
    }
  }

  private Css2() {
    // uninstantiable
  }
}
