// Copyright (C) 2008 Google Inc.
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


/**
 * @fileoverview
 * Whitelists of HTML elements and attributes.
 * 
 * @author mikesamuel@gmail.com
 */


/** @namespace */
var html4 = {};

/**
 * HTML element flags.
 * @enum {number}
 */
html4.eflags = {
  OPTIONAL_ENDTAG: 1,
  BREAKS_FLOW: 2,
  EMPTY: 4,
  NAVIGATES: 8,
  CDATA: 0x10,
  RCDATA: 0x20,
  UNSAFE: 0x40
};

/**
 * HTML attribute flags.
 * @enum {number}
 */
html4.atype = {
  SCRIPT: 1,
  STYLE: 2,
  IDREF: 3,
  NAME: 4,
  NMTOKENS: 5,
  URI: 6,
  FRAME: 7
};

/**
 * Maps HTML4 element names to flag bitsets.
 */
html4.ELEMENTS = {
  A          : html4.eflags.NAVIGATES,
  ABBR       : 0,
  ACRONYM    : 0,
  ADDRESS    : 0,
  APPLET     : html4.eflags.UNSAFE,
  AREA       : html4.eflags.EMPTY | html4.eflags.NAVIGATES,
  B          : 0,
  // Changes the meaning of URIs
  BASE       : html4.eflags.UNSAFE | html4.eflags.EMPTY,
  // Affects global styles.
  BASEFONT   : html4.eflags.UNSAFE | html4.eflags.EMPTY,
  BDO        : 0,
  BIG        : 0,
  BLOCKQUOTE : html4.eflags.BREAKS_FLOW,
  // Attributes merged into global body.
  BODY       : html4.eflags.UNSAFE | html4.eflags.OPTIONAL_ENDTAG,
  BR         : html4.eflags.EMPTY | html4.eflags.BREAKS_FLOW,
  BUTTON     : 0,
  CAPTION    : 0,
  CENTER     : html4.eflags.BREAKS_FLOW,
  CITE       : 0,
  CODE       : 0,
  COL        : html4.eflags.EMPTY,
  COLGROUP   : html4.eflags.OPTIONAL_ENDTAG,
  DD         : html4.eflags.OPTIONAL_ENDTAG | html4.eflags.BREAKS_FLOW,
  DEL        : 0,
  DFN        : 0,
  DIR        : html4.eflags.BREAKS_FLOW,
  DIV        : html4.eflags.BREAKS_FLOW,
  DL         : html4.eflags.BREAKS_FLOW,
  DT         : html4.eflags.OPTIONAL_ENDTAG | html4.eflags.BREAKS_FLOW,
  EM         : 0,
  FIELDSET   : 0,
  FONT       : 0,
  FORM       : html4.eflags.BREAKS_FLOW | html4.eflags.NAVIGATES,
  FRAME      : html4.eflags.UNSAFE | html4.eflags.EMPTY,
  // Attributes merged into global frameset.
  FRAMESET   : html4.eflags.UNSAFE,
  H1         : html4.eflags.BREAKS_FLOW,
  H2         : html4.eflags.BREAKS_FLOW,
  H3         : html4.eflags.BREAKS_FLOW,
  H4         : html4.eflags.BREAKS_FLOW,
  H5         : html4.eflags.BREAKS_FLOW,
  H6         : html4.eflags.BREAKS_FLOW,
  HEAD       : (html4.eflags.UNSAFE | html4.eflags.OPTIONAL_ENDTAG
                | html4.eflags.BREAKS_FLOW),
  HR         : html4.eflags.EMPTY | html4.eflags.BREAKS_FLOW,
  HTML       : (html4.eflags.UNSAFE | html4.eflags.OPTIONAL_ENDTAG
                | html4.eflags.BREAKS_FLOW),
  I          : 0,
  IFRAME     : html4.eflags.UNSAFE,
  IMG        : html4.eflags.EMPTY,
  INPUT      : html4.eflags.EMPTY,
  INS        : 0,
  ISINDEX    : (html4.eflags.UNSAFE | html4.eflags.EMPTY
                | html4.eflags.BREAKS_FLOW | html4.eflags.NAVIGATES),
  KBD        : 0,
  LABEL      : 0,
  LEGEND     : 0,
  LI         : html4.eflags.OPTIONAL_ENDTAG | html4.eflags.BREAKS_FLOW,
  // Can load global styles.
  LINK       : html4.eflags.UNSAFE | html4.eflags.EMPTY,
  MAP        : 0,
  MENU       : html4.eflags.BREAKS_FLOW,
  // Can override document headers and encoding, or cause navigation.
  META       : html4.eflags.UNSAFE | html4.eflags.EMPTY,
  // Ambiguous tokenization.  Content is CDATA/PCDATA depending on browser.
  NOFRAMES   : html4.eflags.UNSAFE | html4.eflags.BREAKS_FLOW,
  // Ambiguous tokenization.  Content is CDATA/PCDATA depending on browser.
  NOSCRIPT   : html4.eflags.UNSAFE,
  OBJECT     : html4.eflags.UNSAFE,
  OL         : html4.eflags.BREAKS_FLOW,
  OPTGROUP   : 0,
  OPTION     : html4.eflags.OPTIONAL_ENDTAG,
  P          : html4.eflags.OPTIONAL_ENDTAG | html4.eflags.BREAKS_FLOW,
  PARAM      : html4.eflags.UNSAFE | html4.eflags.EMPTY,
  PLAINTEXT  : (html4.eflags.OPTIONAL_ENDTAG | html4.eflags.UNSAFE
                | html4.eflags.CDATA),
  PRE        : html4.eflags.BREAKS_FLOW,
  Q          : 0,
  S          : 0,
  SAMP       : 0,
  SCRIPT     : html4.eflags.UNSAFE | html4.eflags.CDATA,
  SELECT     : 0,
  SMALL      : 0,
  SPAN       : 0,
  STRIKE     : 0,
  STRONG     : 0,
  STYLE      : html4.eflags.UNSAFE | html4.eflags.CDATA,
  SUB        : 0,
  SUP        : 0,
  TABLE      : html4.eflags.BREAKS_FLOW,
  TBODY      : html4.eflags.OPTIONAL_ENDTAG,
  TD         : html4.eflags.OPTIONAL_ENDTAG | html4.eflags.BREAKS_FLOW,
  TEXTAREA   : html4.eflags.RCDATA,
  TFOOT      : html4.eflags.OPTIONAL_ENDTAG,
  TH         : html4.eflags.OPTIONAL_ENDTAG | html4.eflags.BREAKS_FLOW,
  THEAD      : html4.eflags.OPTIONAL_ENDTAG,
  TITLE      : (html4.eflags.UNSAFE | html4.eflags.BREAKS_FLOW
                | html4.eflags.RCDATA),
  TR         : html4.eflags.OPTIONAL_ENDTAG | html4.eflags.BREAKS_FLOW,
  TT         : 0,
  U          : 0,
  UL         : html4.eflags.BREAKS_FLOW,
  VAR        : 0,
  XMP        : html4.eflags.CDATA
};

/**
 * Maps HTML4 attribute names to flag bitsets.
 */
html4.ATTRIBS = {
  ABBR            : 0,
  ACCEPT          : 0,
  'ACCEPT-CHARSET': 0,
  ACCESSKEY       : 0,
  ACTION          : html4.atype.URI,
  ALIGN           : 0,
  ALINK           : 0,
  ALT             : 0,
  ARCHIVE         : html4.atype.URI,
  AXIS            : 0,
  BACKGROUND      : html4.atype.URI,
  BGCOLOR         : 0,
  BORDER          : 0,
  CELLPADDING     : 0,
  CELLSPACING     : 0,
  CHAR            : 0,
  CHAROFF         : 0,
  CHARSET         : 0,
  CHECKED         : 0,
  CITE            : html4.atype.URI,
  CLASS           : html4.atype.NMTOKENS,
  CLASSID         : html4.atype.URI,
  CLEAR           : 0,
  CODE            : 0,
  CODEBASE        : html4.atype.URI,
  CODETYPE        : 0,
  COLOR           : 0,
  COLS            : 0,
  COLSPAN         : 0,
  COMPACT         : 0,
  CONTENT         : 0,
  COORDS          : 0,
  DATA            : html4.atype.URI,
  DATETIME        : 0,
  DECLARE         : 0,
  DEFER           : 0,
  DIR             : 0,
  DISABLED        : 0,
  ENCTYPE         : 0,
  FACE            : 0,
  FOR             : html4.atype.IDREF,
  FRAME           : 0,
  FRAMEBORDER     : 0,
  HEADERS         : 0,
  HEIGHT          : 0,
  HREF            : html4.atype.URI,
  HREFLANG        : 0,
  HSPACE          : 0,
  //'HTTP-EQUIV'    : 0,   // unsafe
  ID              : html4.atype.IDREF,
  ISMAP           : 0,
  LABEL           : 0,
  LANG            : 0,
  LANGUAGE        : 0,
  LINK            : 0,
  LONGDESC        : html4.atype.URI,
  MARGINHEIGHT    : 0,
  MARGINWIDTH     : 0,
  MAXLENGTH       : 0,
  MEDIA           : 0,
  METHOD          : 0,
  MULTIPLE        : 0,
  NAME            : html4.atype.NAME,
  NOHREF          : 0,
  NORESIZE        : 0,
  NOSHADE         : 0,
  NOWRAP          : 0,
  OBJECT          : 0,
  ONBLUR          : html4.atype.SCRIPT,
  ONCHANGE        : html4.atype.SCRIPT,
  ONCLICK         : html4.atype.SCRIPT,
  ONDBLCLICK      : html4.atype.SCRIPT,
  ONFOCUS         : html4.atype.SCRIPT,
  ONKEYDOWN       : html4.atype.SCRIPT,
  ONKEYPRESS      : html4.atype.SCRIPT,
  ONKEYUP         : html4.atype.SCRIPT,
  ONLOAD          : html4.atype.SCRIPT,
  ONMOUSEDOWN     : html4.atype.SCRIPT,
  ONMOUSEMOVE     : html4.atype.SCRIPT,
  ONMOUSEOUT      : html4.atype.SCRIPT,
  ONMOUSEOVER     : html4.atype.SCRIPT,
  ONMOUSEUP       : html4.atype.SCRIPT,
  ONRESET         : html4.atype.SCRIPT,
  ONSELECT        : html4.atype.SCRIPT,
  ONSUBMIT        : html4.atype.SCRIPT,
  ONUNLOAD        : html4.atype.SCRIPT,
  PROFILE         : html4.atype.URI,
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
  SRC             : html4.atype.URI,
  STANDBY         : 0,
  START           : 0,
  STYLE           : html4.atype.STYLE,
  SUMMARY         : 0,
  TABINDEX        : 0,
  TARGET          : html4.atype.FRAME,
  TEXT            : 0,
  TITLE           : 0,
  TYPE            : 0,
  USEMAP          : html4.atype.URI,
  VALIGN          : 0,
  VALUE           : 0,
  VALUETYPE       : 0,
  VERSION         : 0,
  VLINK           : 0,
  VSPACE          : 0,
  WIDTH           : 0
};
