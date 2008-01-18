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

// mikesamuel@gmail.com

/**
 * Strips unsafe tags and attributes from html.
 * @param {String} html to sanitize
 * @param {Function} opt_urlXform : String -> String -- a transform to apply to
 *   url attribute values.
 * @param {Function} opt_nmTokenXform : String -> String -- a transform to apply
 *   to name attribute values.
 * @return {String} html
 */
var html_sanitize = (function () {

  // hide all the whitelists and other state so they can't be interfered with

  /** token definitions. */
  var TOK_ENTITY = /^&(?:\#[0-9]+|\#x[0-9a-f]+|\w+);/i;
  var TOK_COMMENT = /^<!--[\s\S]*?-->/;
  var TOK_TAG_BEGIN = /^<\/?[a-z][a-z0-9]*/i;
  var TOK_TAG_END = /^\/?>/;
  var TOK_ATTRIB = /^\w+(\s*=\s*(?:\"[^\"]*\"|\'[^\']*\'|[^>\"\'\s]*))?/;
  var TOK_SPACE = /^\s+/;
  var TOK_OTHER = /^[^&<]+/;
  var TOK_CRUFT = /^[<&]/;
  var TOK_IGNORABLE_CRUFT = /^[^\w\s>]+/;

  /** the token definitions used when we are outside a tag. */
  var TOKS_NOTTAG = [
      TOK_ENTITY,
      TOK_COMMENT,
      TOK_TAG_BEGIN,
      TOK_OTHER,
      TOK_CRUFT
  ];
  /** token definitions used inside a tag. */
  var TOKS_INTAG = [
      TOK_SPACE,
      TOK_ATTRIB,
      TOK_TAG_END,
      TOK_IGNORABLE_CRUFT
  ];

  /**
   * split html into tokens using the regexps above.
   * This also does some normalization of tokens, escaping specials that don't
   * appear to be part of a larger token.
   */
  var lex = function (html) {
    html = String(html);
    var tokens = [];
    var inTag = false; // 0 outside tag, 1 inside tag
    while (html) {
      var m = null;
      var tok = null;
      if (!inTag) {
        for (var i = 0; i < TOKS_NOTTAG.length; ++i) {
          m = html.match(TOKS_NOTTAG[i]);
          if (m) {
            tok = TOKS_NOTTAG[i];
            break;
          }
        }
        if (tok === TOK_TAG_BEGIN) { inTag = true; }
      } else {
        for (var i = 0; i < TOKS_INTAG.length; ++i) {
          m = html.match(TOKS_INTAG[i]);
          if (m) {
            tok = TOKS_INTAG[i];
            break;
          }
        }
        if (tok === TOK_TAG_END) { inTag = false; }
      }
      var tokstr = m[0];
      html = html.substring(tokstr.length);
      if (tok === TOK_CRUFT) {
        tokstr = tokstr == '<' ? '&lt;' : '&amp;';
      } else if (tok === TOK_OTHER) {
        tokstr = tokstr.replace(/>/g, '&gt;');
      } else if (tok == TOK_SPACE) {
        tokstr = ' ';
      } else if (tok === TOK_ATTRIB) {
        tokstr = tokstr.replace(/^(\w+)\s*=\s*/, '$1=');
      }
      if (tok !== TOK_IGNORABLE_CRUFT && tok != TOK_COMMENT) {
        tokens.push(tokstr, tok);
      }
    }
    return tokens;
  };

  // whitelists of elements and attributes

  /** element flags. */
  var OPTIONAL_ENDTAG = 1,
          BREAKS_FLOW = 2,
                EMPTY = 4,
               UNSAFE = 8;

  /** attribute flags */
  var SCRIPT_TYPE = 1,
       STYLE_TYPE = 2,
     NMTOKEN_TYPE = 4,
         URI_TYPE = 8;

  /**
   * All the HTML4 elements.
   * U - unsafe, E - empty, B - breaks flow, O - optional endtag
   */
  var ELEMENTS = {
    A          : 0,
    ABBR       : 0,
    ACRONYM    : 0,
    ADDRESS    : 0,
    APPLET     : UNSAFE,
    AREA       : EMPTY,
    B          : 0,
    BASE       : UNSAFE|EMPTY,
    BASEFONT   : UNSAFE|EMPTY,
    BDO        : 0,
    BIG        : 0,
    BLOCKQUOTE : BREAKS_FLOW,
    BODY       : UNSAFE|OPTIONAL_ENDTAG,
    BR         : EMPTY|BREAKS_FLOW,
    BUTTON     : 0,
    CAPTION    : 0,
    CENTER     : BREAKS_FLOW,
    CITE       : 0,
    CODE       : 0,
    COL        : EMPTY,
    COLGROUP   : OPTIONAL_ENDTAG,
    DD         : OPTIONAL_ENDTAG|BREAKS_FLOW,
    DEL        : 0,
    DFN        : 0,
    DIR        : BREAKS_FLOW,
    DIV        : BREAKS_FLOW,
    DL         : BREAKS_FLOW,
    DT         : OPTIONAL_ENDTAG|BREAKS_FLOW,
    EM         : 0,
    FIELDSET   : 0,
    FONT       : 0,
    FORM       : BREAKS_FLOW,
    FRAME      : UNSAFE|EMPTY,
    FRAMESET   : UNSAFE,
    H1         : BREAKS_FLOW,
    H2         : BREAKS_FLOW,
    H3         : BREAKS_FLOW,
    H4         : BREAKS_FLOW,
    H5         : BREAKS_FLOW,
    H6         : BREAKS_FLOW,
    HEAD       : UNSAFE|OPTIONAL_ENDTAG|BREAKS_FLOW,
    HR         : EMPTY|BREAKS_FLOW,
    HTML       : UNSAFE|OPTIONAL_ENDTAG|BREAKS_FLOW,
    I          : 0,
    IFRAME     : UNSAFE,
    IMG        : EMPTY,
    INPUT      : EMPTY,
    INS        : 0,
    ISINDEX    : UNSAFE|EMPTY|BREAKS_FLOW,
    KBD        : 0,
    LABEL      : 0,
    LEGEND     : 0,
    LI         : OPTIONAL_ENDTAG|BREAKS_FLOW,
    LINK       : UNSAFE|EMPTY,
    MAP        : 0,
    MENU       : BREAKS_FLOW,
    META       : UNSAFE|EMPTY,
    NOFRAMES   : UNSAFE|BREAKS_FLOW,
    NOSCRIPT   : UNSAFE,
    OBJECT     : UNSAFE,
    OL         : BREAKS_FLOW,
    OPTGROUP   : 0,
    OPTION     : OPTIONAL_ENDTAG,
    P          : OPTIONAL_ENDTAG|BREAKS_FLOW,
    PARAM      : UNSAFE|EMPTY,
    PRE        : BREAKS_FLOW,
    Q          : 0,
    S          : 0,
    SAMP       : 0,
    SCRIPT     : UNSAFE,
    SELECT     : 0,
    SMALL      : 0,
    SPAN       : 0,
    STRIKE     : 0,
    STRONG     : 0,
    STYLE      : UNSAFE,
    SUB        : 0,
    SUP        : 0,
    TABLE      : BREAKS_FLOW,
    TBODY      : OPTIONAL_ENDTAG,
    TD         : OPTIONAL_ENDTAG|BREAKS_FLOW,
    TEXTAREA   : 0,
    TFOOT      : OPTIONAL_ENDTAG,
    TH         : OPTIONAL_ENDTAG|BREAKS_FLOW,
    THEAD      : OPTIONAL_ENDTAG,
    TITLE      : UNSAFE|BREAKS_FLOW,
    TR         : OPTIONAL_ENDTAG|BREAKS_FLOW,
    TT         : 0,
    U          : 0,
    UL         : BREAKS_FLOW,
    VAR        : 0
  };

  /**
   * All the HTML4 attributes
   */
  var ATTRIBS = {
    ABBR            : 0,
    ACCEPT          : 0,
    'ACCEPT-CHARSET': 0,
    ACCESSKEY       : 0,
    ACTION          : URI_TYPE,
    ALIGN           : 0,
    ALINK           : 0,
    ALT             : 0,
    ARCHIVE         : URI_TYPE,
    AXIS            : 0,
    BACKGROUND      : URI_TYPE,
    BGCOLOR         : 0,
    BORDER          : 0,
    CELLPADDING     : 0,
    CELLSPACING     : 0,
    CHAR            : 0,
    CHAROFF         : 0,
    CHARSET         : 0,
    CHECKED         : 0,
    CITE            : URI_TYPE,
    CLASS           : NMTOKEN_TYPE,
    CLASSID         : URI_TYPE,
    CLEAR           : 0,
    CODE            : 0,
    CODEBASE        : URI_TYPE,
    CODETYPE        : 0,
    COLOR           : 0,
    COLS            : 0,
    COLSPAN         : 0,
    COMPACT         : 0,
    CONTENT         : 0,
    COORDS          : 0,
    DATA            : URI_TYPE,
    DATETIME        : 0,
    DECLARE         : 0,
    DEFER           : 0,
    DIR             : 0,
    DISABLED        : 0,
    ENCTYPE         : 0,
    FACE            : 0,
    FOR             : NMTOKEN_TYPE,
    FRAME           : 0,
    FRAMEBORDER     : 0,
    HEADERS         : 0,
    HEIGHT          : 0,
    HREF            : URI_TYPE,
    HREFLANG        : 0,
    HSPACE          : 0,
    'HTTP-EQUIV'    : 0,
    ID              : NMTOKEN_TYPE,
    ISMAP           : 0,
    LABEL           : 0,
    LANG            : 0,
    LANGUAGE        : 0,
    LINK            : 0,
    LONGDESC        : URI_TYPE,
    MARGINHEIGHT    : 0,
    MARGINWIDTH     : 0,
    MAXLENGTH       : 0,
    MEDIA           : 0,
    METHOD          : 0,
    MULTIPLE        : 0,
    NAME            : NMTOKEN_TYPE,  // but not really for inputs
    NOHREF          : 0,
    NORESIZE        : 0,
    NOSHADE         : 0,
    NOWRAP          : 0,
    OBJECT          : 0,
    ONBLUR          : SCRIPT_TYPE,
    ONCHANGE        : SCRIPT_TYPE,
    ONCLICK         : SCRIPT_TYPE,
    ONDBLCLICK      : SCRIPT_TYPE,
    ONFOCUS         : SCRIPT_TYPE,
    ONKEYDOWN       : SCRIPT_TYPE,
    ONKEYPRESS      : SCRIPT_TYPE,
    ONKEYUP         : SCRIPT_TYPE,
    ONLOAD          : SCRIPT_TYPE,
    ONMOUSEDOWN     : SCRIPT_TYPE,
    ONMOUSEMOVE     : SCRIPT_TYPE,
    ONMOUSEOUT      : SCRIPT_TYPE,
    ONMOUSEOVER     : SCRIPT_TYPE,
    ONMOUSEUP       : SCRIPT_TYPE,
    ONRESET         : SCRIPT_TYPE,
    ONSELECT        : SCRIPT_TYPE,
    ONSUBMIT        : SCRIPT_TYPE,
    ONUNLOAD        : SCRIPT_TYPE,
    PROFILE         : URI_TYPE,
    PROMPT          : 0,
    READONLY        : 0,
    REL             : 0,
    REV             : 0,
    ROWS            : 0,
    ROWSPAN         : 0,
    RULES           : 0,
    SCHEME          : 0,
    SCOPE           : 0,
    SCROLLING       : 0,
    SELECTED        : 0,
    SHAPE           : 0,
    SIZE            : 0,
    SPAN            : 0,
    SRC             : URI_TYPE,
    STANDBY         : 0,
    START           : 0,
    STYLE           : STYLE_TYPE,
    SUMMARY         : 0,
    TABINDEX        : 0,
    TARGET          : 0,
    TEXT            : 0,
    TITLE           : 0,
    TYPE            : 0,
    USEMAP          : URI_TYPE,
    VALIGN          : 0,
    VALUE           : 0,
    VALUETYPE       : 0,
    VERSION         : 0,
    VLINK           : 0,
    VSPACE          : 0,
    WIDTH           : 0
  };

  var ENTITIES = {
    LT   : '<',
    GT   : '>',
    AMP  : '&',
    NBSP : '\240',
    QUOT : '"',
    APOS : '\''
  };

  function escapeOneEntity(m) {
    var name = m[1].toUpperCase();
    if (ENTITIES.hasOwnProperty(s)) { return ENTITIES[name]; }
    m = name.match(/^#(\d+)$/);
    if (m) {
      return String.fromCharCode(parseInt(m[1], 10));
    } else if (!!(m = name.match(/^#x([0-9A-F]+)$/))) {
      return String.fromCharCode(parseInt(m[1], 16));
    }
    return '';
  }

  function unescapeEntities(s) {
    return s.replace(/&(#\d+|#x[\da-f]+|\w+);/g, escapeOneEntity);
  }

  function unescapedValueForAttrib(s) {
    var m = s.match(/=\s*([\"\']?)?(.*)\1/);
    if (m) {
      return unescapeEntities(m[2]);
    } else {
      return null;
    }
  }

  function escapeAttrib(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/&/g, '&gt;')
      .replace(/\"/g, '&quot;');
  }

  /** actually does the sanitizing. */
  return function html_sanitize(html, opt_urlXform, opt_nmTokenXform) {
    var toks = lex(html);
    var out = [];

    var ignoring = false;
    for (var i = 0; i < toks.length; ++i) {
      var tok = toks[i], type = toks[++i];
      //alert('tok=' + tok + ', type=' + type + ', ignoring=' + ignoring);
      if (TOK_TAG_BEGIN === type) {
        var name = tok.replace(/^[<\/]+/, '').toUpperCase();
        ignoring = !ELEMENTS.hasOwnProperty(name) || (ELEMENTS[name] & UNSAFE);
      } else if (TOK_ATTRIB === type && !ignoring) {
        var name = tok.match(/\w+/)[0].toUpperCase();
        if (!ATTRIBS.hasOwnProperty(name)) { continue; }
        var flags = ATTRIBS[name];
        if (flags & (SCRIPT_TYPE | STYLE_TYPE)) { continue; }
        if (flags) {
          // apply transforms
          // unescape value, transform it.  skip if null, otherwise reescape.
          var value = unescapedValueForAttrib(tok);
          if (null == value) { continue; }
          if ((flags & URI_TYPE) && opt_urlXform) {
            value = opt_urlXform(value);
          }
          if ((flags & NMTOKEN_TYPE) && opt_nmTokenXform) {
            value = opt_nmTokenXForm(value);
          }
          if (null == value) { continue; }
          tok = name + '="' + escapeAttrib(value) + '"';
        }
      }
      if (!ignoring) { out.push(tok); }
      // TODO: some way of enforcing attribute constraints
      if (TOK_TAG_END === type) { ignoring = false; }
    }
    return out.join('');
  };

})();
