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

package com.google.caja.plugin;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.parser.css.CssPropertySignature;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.RhinoTestBed;

import java.util.ArrayList;
import java.util.List;

public class CssPropertyPatternsTest extends CajaTestCase {
  public void testKeywordPattern() throws Exception {
    assertPattern("zoicks", "/^\\s*zoicks\\s+$/i");
    assertMatches("zoicks", "zoicks", "  zoicks", " ZOICKS ");
    assertDoesNotMatch("zoicks", "zoick", "zzoicks", "zoicks zoicks");
  }

  public void testUnionPattern() throws Exception {
    assertPattern("[ foo | bar ]", "/^\\s*(?:foo|bar)\\s+$/i");
    assertMatches("[ foo | bar ]", "foo", "bar", " foo ", " bar ");
    assertDoesNotMatch("[ foo | bar ]", "fo", "ar", " foo bar", " far ");
  }


  public void testUnionsExcludeComplex() throws Exception {
    assertPattern("[ foo | [ a || b || c || d ] | bar ]",
                  "/^\\s*(?:foo|bar)\\s+$/i");
  }

  public void testReferencePattern() throws Exception {
    assertPattern("'background-attachment'",
                  "/^\\s*(?:scroll|fixed|inherit)\\s+$/i");
  }

  public void testMultiFoo() throws Exception {
    assertPattern("foo*", "/^\\s*(?:foo\\s+)*$/i");
    assertMatches("foo*", "", "foo", "foo foo");
    assertDoesNotMatch("foo*", "bar", "foo bar", "bar foo foo", "foofoo");

    assertPattern("foo+", "/^\\s*(?:foo\\s+)+$/i");
    assertMatches("foo+", "foo", "foo foo", "foo  foo foo");
    assertDoesNotMatch("foo+", "", "bar", "foo bar", "bar  foo foo", "foofoo");

    assertPattern("foo?", "/^\\s*(?:foo\\s+)?$/i");
    assertMatches("foo?", "", "foo");
    assertDoesNotMatch("foo?", "bar", "foo bar", "foo foo", "foofoo");
  }

  public void testConcatenations() throws Exception {
    assertPattern("foo bar", "/^\\s*foo\\s+bar\\s+$/i");
    // Fail if cannot handle a member of a concatenation
    assertPattern("[ a b [ c || d ] ]", null);
    assertMatches("foo bar", "foo bar", "foo  bar");
    assertDoesNotMatch("foo bar", "foo", "bar", "bar foo", "");
  }

  public void testUnionsFolded() throws Exception {
    assertPattern("[ foo | [ bar bar | baz ] | boo ]",
                  "/^\\s*(?:foo|bar\\s+bar|baz|boo)\\s+$/i");
    assertMatches("[ foo | [ bar bar | baz ] | boo ]",
                  "foo", "bar bar", "baz", "boo");
    assertDoesNotMatch("[ foo | [ bar bar | baz ] | boo ]",
                       "bar", "", "faz", "fooo");
  }

  public void testBackgroundImage() throws Exception {
    assertPattern(
        "<uri> | none | inherit",
        "/^\\s*(?:url\\(\"[^\\(\\)\\\\\\\"\\r\\n]+\"\\)|none|inherit)\\s+$/i");
    assertMatches(
        "<uri> | none | inherit", "none", "inherit", "url(\"foo.gif\")");
    assertDoesNotMatch(
        "<uri> | none | inherit",
        "gurl(\"foo.gif\")", "\"foo.gif\"", "url(\"foo.gif)",
        "url(\"foo.gif)\")", "url(\"foo.gif\\\")", "url(\"foo.gif\"\")"
        );
  }

  public void testFontFamilies() throws Exception {
    String fontFamilySignature = (
        "[[ <family-name> | <generic-family> ]"
        + " [, [ <family-name> | <generic-family> ]]* ] | inherit");
    assertMatches(
        fontFamilySignature,
        "\"Helvetica\"", "\"Arial Bold\" , sans-serif");
    assertDoesNotMatch(
        fontFamilySignature,
        // Require family names to be quoted unless we want to rewrite to
        // quote anything like "Bold" that might be a reserved word.
        "Arial Bold",
        // Quotes and escapes not allowed in strings
        "\"\"", "\"Helvetica\\\"", "\"Helvetica\\", "\"Helvetica",
        // Neither are whacky characters.
        "\"@import\"", "\"!important\"");
  }

  private void assertPattern(String sig, String golden) {
    String actual = toPattern(sig);
    assertEquals(actual, golden, actual);
  }

  private void assertMatches(String sig, String... candidates)
      throws Exception {
    String regex = "";
    RhinoTestBed.runJs(
        null,
        new RhinoTestBed.Input(
            ""
            + "var pattern = " + toPattern(sig) + ";"
            + "var candidates = " + render(toArrayList(candidates)) + ";"
            + "for (var i = candidates.length; --i >= 0;) {"
            + "  if (!pattern.test(candidates[i] + ' ')) {"
            + "    throw new Error(candidates[i]);"
            + "  }"
            + "}",
            getName()));
  }

  private void assertDoesNotMatch(String sig, String... candidates)
      throws Exception {
    String regex = "";
    RhinoTestBed.runJs(
        null,
        new RhinoTestBed.Input(
            ""
            + "var pattern = " + toPattern(sig) + ";"
            + "var candidates = " + render(toArrayList(candidates)) + ";"
            + "for (var i = candidates.length; --i >= 0;) {"
            + "  if (pattern.test(candidates[i] + ' ')) {"
            + "    throw new Error(candidates[i]);"
            + "  }"
            + "}",
            getName()));
}

  private String toPattern(String sig) {
    CssPropertyPatterns pp = new CssPropertyPatterns(
        CssSchema.getDefaultCss21Schema(mq));
    return pp.cssPropertyToPattern(parseSignature(sig));
  }

  private ArrayConstructor toArrayList(String... values) {
    List<StringLiteral> literals = new ArrayList<StringLiteral>();
    for (String value : values) {
      literals.add(new StringLiteral(StringLiteral.toQuotedValue(value)));
    }
    return new ArrayConstructor(literals);
  }

  private static CssPropertySignature parseSignature(String sig) {
    return CssPropertySignature.Parser.parseSignature(sig);
  }
}
