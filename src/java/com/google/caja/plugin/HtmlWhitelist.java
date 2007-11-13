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

package com.google.caja.plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author mikesamuel@gmail.com
 */
final class HtmlWhitelist {

  static final Set<String> ALLOWED_TAGS;

  static {
    Set<String> allowedTags = new HashSet<String>();
    allowedTags.addAll(Arrays.asList(
      "A",
      "ABBR",
      "ACRONYM",
      "ADDRESS",
      //"APPLET",  // disallow because allows scripting
      "AREA",
      "B",
      //"BASE",  // affects global state and could be used to redirect requests
      //"BASEFONT",  // affects global state
      "BDO",
      "BIG",
      "BLOCKQUOTE",
      //"BODY",  // a global level tag
      "BR",
      "BUTTON",
      "CAPTION",
      "CENTER",
      "CITE",
      "CODE",
      "COL",
      "COLGROUP",
      "DD",
      "DEL",
      "DFN",
      "DIR",
      "DIV",
      "DL",
      "DT",
      "EM",
      "FIELDSET",
      "FONT",
      "FORM",
      //"FRAME",  // can be used to cause javascript execution
      //"FRAMESET",  // only useful with banned elements
      "H1",
      "H2",
      "H3",
      "H4",
      "H5",
      "H6",
      //"HEAD",  // a global level tag
      "HR",
      //"HTML",  // a global level tag
      "I",
       // TODO(ihab): Remove this and allow client custom whitelists.
      "IFRAME",  // can be used to cause javascript execution
      "IMG",
      "INPUT",
      "INS",
      //"ISINDEX",  // can be used to change page location
      "KBD",
      "LABEL",
      "LEGEND",
      "LI",
      //"LINK",  // can be used to load other javascript, e.g. on print
      "MAP",
      "MENU",
      //"META",  // can be used to cause page reloads
      //"NOFRAMES",  // useless since frames can't be used
      //"NOSCRIPT",  // useless since javascript must be loaded
      //"OBJECT",  // allows scripting
      "OL",
      "OPTGROUP",
      "OPTION",
      "P",
      //"PARAM",  // useless since applet and object banned
      "PRE",
      "Q",
      "S",
      "SAMP",
       // TODO(ihab): Remove this and allow client custom whitelists.
      "SCRIPT",  // allows execution of arbitrary script
      "SELECT",
      "SMALL",
      "SPAN",
      "STRIKE",
      "STRONG",
       // TODO(ihab): Remove this and allow client custom whitelists.
      "STYLE",  // allows global definition of styles.
      "SUB",
      "SUP",
      "TABLE",
      "TBODY",
      "TD",
      "TEXTAREA",
      "TFOOT",
      "TH",
      "THEAD",
      //"TITLE",  // a global level tag
      "TR",
      "TT",
      "U",
      "UL",
      "VAR"
      ));
    ALLOWED_TAGS = Collections.unmodifiableSet(allowedTags);
  }
}
