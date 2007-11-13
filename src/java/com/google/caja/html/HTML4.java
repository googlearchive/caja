// Copyright (C) 2004 Google Inc.
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

package com.google.caja.html;

import java.util.HashMap;
import java.util.Map;

/**
 * HTML4 contains HTML 4.0 definitions and specifications
 *
 * @see <a href="http://www.w3.org/TR/html401/">html401</a>
 * @see <a href="http://www.w3.org/TR/html401/index/elements.html">elements</a>
 * @see <a href="http://www.w3.org/TR/html401/index/attributes.html"
 *       >attributes</a>
 *
 * @author Jing Yee Lim
 */
public final class HTML4 {

  /** Map of all elements */
  private static final Map<String, HTML.Element> elements_ =
    new HashMap<String, HTML.Element>();

  /** Map of all attributes */
  private static final Map<String, HTML.Attribute> attributes_ =
    new HashMap<String, HTML.Attribute>();

  /** Default Whitelist */
  private static final HtmlWhitelist defaultWhitelist = new HtmlWhitelist() {
    /**
     * @inheritDoc
     */
    public HTML.Element lookupElement(String name) {
      return HTML4.lookupElement(name);
    }

    /**
     * @inheritDoc
     */
    public HTML.Attribute lookupAttribute(String name) {
      return HTML4.lookupAttribute(name);
    }
  };

  /** Gets the default Whitelist */
  public static HtmlWhitelist getWhitelist() {
    return HTML4.defaultWhitelist;
  }

  /** Looks for a HTML4 element */
  public static HTML.Element lookupElement(String name) {
    return elements_.get(name.toLowerCase());
  }

  /** Looks for a HTML4 attribute */
  public static HTML.Attribute lookupAttribute(String name) {
    return attributes_.get(name.toLowerCase());
  }

  /** Creates and adds a element to the map */
  private static HTML.Element element(String name, String flags) {
    return element(name, flags, HTML.Element.Type.NONE);
  }

  private static HTML.Element element(
      String name, String flags, HTML.Element.Type type) {
    name = name.toLowerCase();

    boolean empty = false;
    boolean optionalEndTag = false;
    boolean breaksFlow = false;
    for (int i = 0; i < flags.length(); i++) {
      switch (flags.charAt(i)) {
        case 'E': empty = true; break;
        case 'O': optionalEndTag = true; break;
        case 'B': breaksFlow = true; break;
        default: throw new Error("Unknown element flag");
      }
    }
    HTML.Element element = new HTML.Element(name, type, empty, optionalEndTag,
                                            breaksFlow);
    elements_.put(name, element);
    return element;
  }

  /** Creates and add an attribute to the map */
  private static HTML.Attribute attr(String attribute) {
    return attr(attribute, HTML.Attribute.Type.NONE);
  }
  private static HTML.Attribute attr(
      String attribute, HTML.Attribute.Type type) {
    attribute = attribute.toLowerCase();
    HTML.Attribute attr = new HTML.Attribute(attribute, type);
    attributes_.put(attribute, attr);
    return attr;
  }

  /**
   * All the HTML4 elements
   */
  public static final HTML.Element
    A_ELEMENT          = element("A", ""),
    ABBR_ELEMENT       = element("ABBR", ""),
    ACRONYM_ELEMENT    = element("ACRONYM", ""),
    ADDRESS_ELEMENT    = element("ADDRESS", ""),
    APPLET_ELEMENT     = element("APPLET", ""),
    AREA_ELEMENT       = element("AREA", "E"),
    B_ELEMENT          = element("B", ""),
    BASE_ELEMENT       = element("BASE", "E"),
    BASEFONT_ELEMENT   = element("BASEFONT", "E"),
    BDO_ELEMENT        = element("BDO", ""),
    BIG_ELEMENT        = element("BIG", ""),
    BLOCKQUOTE_ELEMENT = element("BLOCKQUOTE", "B"),
    BODY_ELEMENT       = element("BODY", "O"),
    BR_ELEMENT         = element("BR", "EB"),
    BUTTON_ELEMENT     = element("BUTTON", ""),
    CAPTION_ELEMENT    = element("CAPTION", "", HTML.Element.Type.TABLE),
    CENTER_ELEMENT     = element("CENTER", "B"),
    CITE_ELEMENT       = element("CITE", ""),
    CODE_ELEMENT       = element("CODE", ""),
    COL_ELEMENT        = element("COL", "E", HTML.Element.Type.TABLE),
    COLGROUP_ELEMENT   = element("COLGROUP", "O", HTML.Element.Type.TABLE),
    DD_ELEMENT         = element("DD", "OB"),
    DEL_ELEMENT        = element("DEL", ""),
    DFN_ELEMENT        = element("DFN", ""),
    DIR_ELEMENT        = element("DIR", "B"),
    DIV_ELEMENT        = element("DIV", "B"),
    DL_ELEMENT         = element("DL", "B"),
    DT_ELEMENT         = element("DT", "OB"),
    EM_ELEMENT         = element("EM", ""),
    FIELDSET_ELEMENT   = element("FIELDSET", ""),
    FONT_ELEMENT       = element("FONT", ""),
    FORM_ELEMENT       = element("FORM", "B"),
    FRAME_ELEMENT      = element("FRAME", "E"),
    FRAMESET_ELEMENT   = element("FRAMESET", ""),
    H1_ELEMENT         = element("H1", "B"),
    H2_ELEMENT         = element("H2", "B"),
    H3_ELEMENT         = element("H3", "B"),
    H4_ELEMENT         = element("H4", "B"),
    H5_ELEMENT         = element("H5", "B"),
    H6_ELEMENT         = element("H6", "B"),
    HEAD_ELEMENT       = element("HEAD", "OB"),
    HR_ELEMENT         = element("HR", "EB"),
    HTML_ELEMENT       = element("HTML", "OB"),
    I_ELEMENT          = element("I", ""),
    IFRAME_ELEMENT     = element("IFRAME", ""),
    IMG_ELEMENT        = element("IMG", "E"),
    INPUT_ELEMENT      = element("INPUT", "E"),
    INS_ELEMENT        = element("INS", ""),
    ISINDEX_ELEMENT    = element("ISINDEX", "EB"),
    KBD_ELEMENT        = element("KBD", ""),
    LABEL_ELEMENT      = element("LABEL", ""),
    LEGEND_ELEMENT     = element("LEGEND", ""),
    LI_ELEMENT         = element("LI", "OB"),
    LINK_ELEMENT       = element("LINK", "E"),
    MAP_ELEMENT        = element("MAP", ""),
    MENU_ELEMENT       = element("MENU", "B"),
    META_ELEMENT       = element("META", "E"),
    NOFRAMES_ELEMENT   = element("NOFRAMES", "B"),
    NOSCRIPT_ELEMENT   = element("NOSCRIPT", ""),
    OBJECT_ELEMENT     = element("OBJECT", ""),
    OL_ELEMENT         = element("OL", "B"),
    OPTGROUP_ELEMENT   = element("OPTGROUP", ""),
    OPTION_ELEMENT     = element("OPTION", "O"),
    P_ELEMENT          = element("P", "OB"),
    PARAM_ELEMENT      = element("PARAM", "E"),
    PRE_ELEMENT        = element("PRE", "B"),
    Q_ELEMENT          = element("Q", ""),
    S_ELEMENT          = element("S", ""),
    SAMP_ELEMENT       = element("SAMP", ""),
    SCRIPT_ELEMENT     = element("SCRIPT", ""),
    SELECT_ELEMENT     = element("SELECT", ""),
    SMALL_ELEMENT      = element("SMALL", ""),
    SPAN_ELEMENT       = element("SPAN", ""),
    STRIKE_ELEMENT     = element("STRIKE", ""),
    STRONG_ELEMENT     = element("STRONG", ""),
    STYLE_ELEMENT      = element("STYLE", ""),
    SUB_ELEMENT        = element("SUB", ""),
    SUP_ELEMENT        = element("SUP", ""),
    TABLE_ELEMENT      = element("TABLE", "B", HTML.Element.Type.TABLE),
    TBODY_ELEMENT      = element("TBODY", "O", HTML.Element.Type.TABLE),
    TD_ELEMENT         = element("TD", "OB", HTML.Element.Type.TABLE),
    TEXTAREA_ELEMENT   = element("TEXTAREA", ""),
    TFOOT_ELEMENT      = element("TFOOT", "O", HTML.Element.Type.TABLE),
    TH_ELEMENT         = element("TH", "OB", HTML.Element.Type.TABLE),
    THEAD_ELEMENT      = element("THEAD", "O", HTML.Element.Type.TABLE),
    TITLE_ELEMENT      = element("TITLE", "B"),
    TR_ELEMENT         = element("TR", "OB", HTML.Element.Type.TABLE),
    TT_ELEMENT         = element("TT", ""),
    U_ELEMENT          = element("U", ""),
    UL_ELEMENT         = element("UL", "B"),
    VAR_ELEMENT        = element("VAR", "");

  /**
   * All the HTML4 attributes
   */
  public static final HTML.Attribute
    ABBR_ATTRIBUTE           = attr("ABBR"),
    ACCEPT_ATTRIBUTE         = attr("ACCEPT"),
    ACCEPT_CHARSET_ATTRIBUTE = attr("ACCEPT-CHARSET"),
    ACCESSKEY_ATTRIBUTE      = attr("ACCESSKEY"),
    ACTION_ATTRIBUTE         = attr("ACTION", HTML.Attribute.Type.URI),
    ALIGN_ATTRIBUTE          = attr("ALIGN"),
    ALINK_ATTRIBUTE          = attr("ALINK"),
    ALT_ATTRIBUTE            = attr("ALT"),
    ARCHIVE_ATTRIBUTE        = attr("ARCHIVE", HTML.Attribute.Type.URI),
    AXIS_ATTRIBUTE           = attr("AXIS"),
    BACKGROUND_ATTRIBUTE     = attr("BACKGROUND", HTML.Attribute.Type.URI),
    BGCOLOR_ATTRIBUTE        = attr("BGCOLOR"),
    BORDER_ATTRIBUTE         = attr("BORDER"),
    CELLPADDING_ATTRIBUTE    = attr("CELLPADDING"),
    CELLSPACING_ATTRIBUTE    = attr("CELLSPACING"),
    CHAR_ATTRIBUTE           = attr("CHAR"),
    CHAROFF_ATTRIBUTE        = attr("CHAROFF"),
    CHARSET_ATTRIBUTE        = attr("CHARSET"),
    CHECKED_ATTRIBUTE        = attr("CHECKED"),
    CITE_ATTRIBUTE           = attr("CITE", HTML.Attribute.Type.URI),
    CLASS_ATTRIBUTE          = attr("CLASS"),
    CLASSID_ATTRIBUTE        = attr("CLASSID", HTML.Attribute.Type.URI),
    CLEAR_ATTRIBUTE          = attr("CLEAR"),
    CODE_ATTRIBUTE           = attr("CODE"),
    CODEBASE_ATTRIBUTE       = attr("CODEBASE", HTML.Attribute.Type.URI),
    CODETYPE_ATTRIBUTE       = attr("CODETYPE"),
    COLOR_ATTRIBUTE          = attr("COLOR"),
    COLS_ATTRIBUTE           = attr("COLS"),
    COLSPAN_ATTRIBUTE        = attr("COLSPAN"),
    COMPACT_ATTRIBUTE        = attr("COMPACT"),
    CONTENT_ATTRIBUTE        = attr("CONTENT"),
    COORDS_ATTRIBUTE         = attr("COORDS"),
    DATA_ATTRIBUTE           = attr("DATA", HTML.Attribute.Type.URI),
    DATETIME_ATTRIBUTE       = attr("DATETIME"),
    DECLARE_ATTRIBUTE        = attr("DECLARE"),
    DEFER_ATTRIBUTE          = attr("DEFER"),
    DIR_ATTRIBUTE            = attr("DIR"),
    DISABLED_ATTRIBUTE       = attr("DISABLED"),
    ENCTYPE_ATTRIBUTE        = attr("ENCTYPE"),
    FACE_ATTRIBUTE           = attr("FACE"),
    FOR_ATTRIBUTE            = attr("FOR"),
    FRAME_ATTRIBUTE          = attr("FRAME"),
    FRAMEBORDER_ATTRIBUTE    = attr("FRAMEBORDER"),
    HEADERS_ATTRIBUTE        = attr("HEADERS"),
    HEIGHT_ATTRIBUTE         = attr("HEIGHT"),
    HREF_ATTRIBUTE           = attr("HREF", HTML.Attribute.Type.URI),
    HREFLANG_ATTRIBUTE       = attr("HREFLANG"),
    HSPACE_ATTRIBUTE         = attr("HSPACE"),
    HTTP_EQUIV_ATTRIBUTE     = attr("HTTP-EQUIV"),
    ID_ATTRIBUTE             = attr("ID"),
    ISMAP_ATTRIBUTE          = attr("ISMAP"),
    LABEL_ATTRIBUTE          = attr("LABEL"),
    LANG_ATTRIBUTE           = attr("LANG"),
    LANGUAGE_ATTRIBUTE       = attr("LANGUAGE"),
    LINK_ATTRIBUTE           = attr("LINK"),
    LONGDESC_ATTRIBUTE       = attr("LONGDESC", HTML.Attribute.Type.URI),
    MARGINHEIGHT_ATTRIBUTE   = attr("MARGINHEIGHT"),
    MARGINWIDTH_ATTRIBUTE    = attr("MARGINWIDTH"),
    MAXLENGTH_ATTRIBUTE      = attr("MAXLENGTH"),
    MEDIA_ATTRIBUTE          = attr("MEDIA"),
    METHOD_ATTRIBUTE         = attr("METHOD"),
    MULTIPLE_ATTRIBUTE       = attr("MULTIPLE"),
    NAME_ATTRIBUTE           = attr("NAME"),
    NOHREF_ATTRIBUTE         = attr("NOHREF"),
    NORESIZE_ATTRIBUTE       = attr("NORESIZE"),
    NOSHADE_ATTRIBUTE        = attr("NOSHADE"),
    NOWRAP_ATTRIBUTE         = attr("NOWRAP"),
    OBJECT_ATTRIBUTE         = attr("OBJECT"),
    ONBLUR_ATTRIBUTE         = attr("ONBLUR", HTML.Attribute.Type.SCRIPT),
    ONCHANGE_ATTRIBUTE       = attr("ONCHANGE", HTML.Attribute.Type.SCRIPT),
    ONCLICK_ATTRIBUTE        = attr("ONCLICK", HTML.Attribute.Type.SCRIPT),
    ONDBLCLICK_ATTRIBUTE     = attr("ONDBLCLICK", HTML.Attribute.Type.SCRIPT),
    ONFOCUS_ATTRIBUTE        = attr("ONFOCUS", HTML.Attribute.Type.SCRIPT),
    ONKEYDOWN_ATTRIBUTE      = attr("ONKEYDOWN", HTML.Attribute.Type.SCRIPT),
    ONKEYPRESS_ATTRIBUTE     = attr("ONKEYPRESS", HTML.Attribute.Type.SCRIPT),
    ONKEYUP_ATTRIBUTE        = attr("ONKEYUP", HTML.Attribute.Type.SCRIPT),
    ONLOAD_ATTRIBUTE         = attr("ONLOAD", HTML.Attribute.Type.SCRIPT),
    ONMOUSEDOWN_ATTRIBUTE    = attr("ONMOUSEDOWN", HTML.Attribute.Type.SCRIPT),
    ONMOUSEMOVE_ATTRIBUTE    = attr("ONMOUSEMOVE", HTML.Attribute.Type.SCRIPT),
    ONMOUSEOUT_ATTRIBUTE     = attr("ONMOUSEOUT", HTML.Attribute.Type.SCRIPT),
    ONMOUSEOVER_ATTRIBUTE    = attr("ONMOUSEOVER", HTML.Attribute.Type.SCRIPT),
    ONMOUSEUP_ATTRIBUTE      = attr("ONMOUSEUP", HTML.Attribute.Type.SCRIPT),
    ONRESET_ATTRIBUTE        = attr("ONRESET", HTML.Attribute.Type.SCRIPT),
    ONSELECT_ATTRIBUTE       = attr("ONSELECT", HTML.Attribute.Type.SCRIPT),
    ONSUBMIT_ATTRIBUTE       = attr("ONSUBMIT", HTML.Attribute.Type.SCRIPT),
    ONUNLOAD_ATTRIBUTE       = attr("ONUNLOAD", HTML.Attribute.Type.SCRIPT),
    PROFILE_ATTRIBUTE        = attr("PROFILE", HTML.Attribute.Type.URI),
    PROMPT_ATTRIBUTE         = attr("PROMPT"),
    READONLY_ATTRIBUTE       = attr("READONLY"),
    REL_ATTRIBUTE            = attr("REL"),
    REV_ATTRIBUTE            = attr("REV"),
    ROWS_ATTRIBUTE           = attr("ROWS"),
    ROWSPAN_ATTRIBUTE        = attr("ROWSPAN"),
    RULES_ATTRIBUTE          = attr("RULES"),
    SCHEME_ATTRIBUTE         = attr("SCHEME"),
    SCOPE_ATTRIBUTE          = attr("SCOPE"),
    SCROLLING_ATTRIBUTE      = attr("SCROLLING"),
    SELECTED_ATTRIBUTE       = attr("SELECTED"),
    SHAPE_ATTRIBUTE          = attr("SHAPE"),
    SIZE_ATTRIBUTE           = attr("SIZE"),
    SPAN_ATTRIBUTE           = attr("SPAN"),
    SRC_ATTRIBUTE            = attr("SRC", HTML.Attribute.Type.URI),
    STANDBY_ATTRIBUTE        = attr("STANDBY"),
    START_ATTRIBUTE          = attr("START"),
    STYLE_ATTRIBUTE          = attr("STYLE"),
    SUMMARY_ATTRIBUTE        = attr("SUMMARY"),
    TABINDEX_ATTRIBUTE       = attr("TABINDEX"),
    TARGET_ATTRIBUTE         = attr("TARGET"),
    TEXT_ATTRIBUTE           = attr("TEXT"),
    TITLE_ATTRIBUTE          = attr("TITLE"),
    TYPE_ATTRIBUTE           = attr("TYPE"),
    USEMAP_ATTRIBUTE         = attr("USEMAP", HTML.Attribute.Type.URI),
    VALIGN_ATTRIBUTE         = attr("VALIGN"),
    VALUE_ATTRIBUTE          = attr("VALUE"),
    VALUETYPE_ATTRIBUTE      = attr("VALUETYPE"),
    VERSION_ATTRIBUTE        = attr("VERSION"),
    VLINK_ATTRIBUTE          = attr("VLINK"),
    VSPACE_ATTRIBUTE         = attr("VSPACE"),
    WIDTH_ATTRIBUTE          = attr("WIDTH");
}
