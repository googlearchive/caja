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

import junit.framework.TestCase;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class HtmlWhitelistTest extends TestCase {

  /** blacklist the whitelist. */
  public void testWhitelist() {
    // TODO(ihab): Re-enable code or delete entirely depending on whether we
    // will allow or disallow these tests in the long term.
    if (false) {
      assertFalse(HtmlWhitelist.ALLOWED_TAGS.contains("SCRIPT"));
      assertFalse(HtmlWhitelist.ALLOWED_TAGS.contains("STYLE"));
      assertFalse(HtmlWhitelist.ALLOWED_TAGS.contains("IFRAME"));
    }
    // swapping innerHTML from an XMP or LISTING tag into another tag might
    // allow bad things to happen.
    assertFalse(HtmlWhitelist.ALLOWED_TAGS.contains("XMP"));
    assertFalse(HtmlWhitelist.ALLOWED_TAGS.contains("LISTING"));
    assertFalse(HtmlWhitelist.ALLOWED_TAGS.contains("FRAME"));
    assertFalse(HtmlWhitelist.ALLOWED_TAGS.contains("FRAMESET"));
    assertFalse(HtmlWhitelist.ALLOWED_TAGS.contains("BODY"));
    assertFalse(HtmlWhitelist.ALLOWED_TAGS.contains("HEAD"));
    assertFalse(HtmlWhitelist.ALLOWED_TAGS.contains("HTML"));
    assertFalse(HtmlWhitelist.ALLOWED_TAGS.contains("TITLE"));
  }
}
