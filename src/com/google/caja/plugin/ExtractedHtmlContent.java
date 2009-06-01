// Copyright (C) 2009 Google Inc.
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

import com.google.caja.parser.js.Block;

import org.w3c.dom.Element;

public class ExtractedHtmlContent {
  /**
   * A user data property of an Element that points to the body of an extracted
   * script tag.
   */
  private static final String EXTRACTED_SCRIPT_BODY = "caja:extractedScript";

  /**
   * The body of a script tag extracted.  Script elements are replaced with
   * place-holder span elements that have the extracted script associated with
   * them.
   */
  public static final Block getExtractedScriptFor(Element el) {
    return (Block) el.getUserData(EXTRACTED_SCRIPT_BODY);
  }

  public static void setExtractedScriptFor(Element placeholder, Block js) {
    placeholder.setUserData(EXTRACTED_SCRIPT_BODY, js, null);
  }
}
