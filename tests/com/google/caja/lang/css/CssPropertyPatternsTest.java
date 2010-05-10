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

package com.google.caja.lang.css;

import com.google.caja.lang.css.CssPropertyPatterns;
import com.google.caja.lang.css.CssSchema;
import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.css.CssPropertySignature;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Executor;
import com.google.caja.util.Name;
import com.google.caja.util.RhinoTestBed;

import java.util.ArrayList;
import java.util.List;

public class CssPropertyPatternsTest extends CajaTestCase {
  public final void testKeywordPattern() throws Exception {
    assertPattern("zoicks", "/^\\s*zoicks\\s*$/i");
    assertMatches("zoicks", "zoicks", "  zoicks", " ZOICKS ");
    assertDoesNotMatch("zoicks", "zoick", "zzoicks", "zoicks zoicks");
  }

  public final void testUnionPattern() throws Exception {
    assertPattern("[ foo | bar ]", "/^\\s*(?:foo|bar)\\s*$/i");
    assertMatches("[ foo | bar ]", "foo", "bar", " foo ", " bar ");
    assertDoesNotMatch("[ foo | bar ]", "fo", "ar", " foo bar", " far ");
  }

  public final void testColor() throws Exception {
   assertMatches(
       "'color'", "#fff", "#aabbcc", "red", "rgb(0,0,255)",
       "rgb(0, 0, 255)");
  }

  public final void testExclusiveUnionPattern() throws Exception {
    assertPattern("[ foo | [ a || b || c || d ] | bar ]",
                  "/^\\s*(?:foo|[a-d](?:\\s+[a-d]){0,3}|bar)\\s*$/i");
  }

  public final void testReferencePattern() throws Exception {
    assertPattern(
        "'background-attachment'",
        ""
        + "/^\\s*(?:scroll|fixed|local)"
        + "(?:\\s*,\\s*(?:scroll|fixed|local))*\\s*$/i");
  }

  public final void testMultiFoo() throws Exception {
    assertPattern("foo*", "/^\\s*(?:foo(?:\\s+foo)*)?\\s*$/i");
    assertMatches("foo*", "", "foo", "foo foo");
    assertDoesNotMatch("foo*", "bar", "foo bar", "bar foo foo", "foofoo");

    assertPattern("foo+", "/^\\s*foo(?:\\s+foo)*\\s*$/i");
    assertMatches("foo+", "foo", "foo foo", "foo  foo foo");
    assertDoesNotMatch("foo+", "", "bar", "foo bar", "bar  foo foo", "foofoo");

    assertPattern("foo?", "/^\\s*(?:foo)?\\s*$/i");
    assertMatches("foo?", "", "foo");
    assertDoesNotMatch("foo?", "bar", "foo bar", "foo foo", "foofoo");
  }

  public final void testConcatenations() throws Exception {
    assertPattern("foo bar", "/^\\s*foo\\s+bar\\s*$/i");
    // Fail if cannot handle a member of a concatenation
    assertPattern("[ a b [ c || d ] ]", "/^\\s*a\\s+b(?:\\s+[cd]){1,2}\\s*$/i");
    assertMatches("foo bar", "foo bar", "foo  bar");
    assertDoesNotMatch("foo bar", "foo", "bar", "bar foo", "");
  }

  public final void testUnionsFolded() throws Exception {
    assertPattern("[ foo | [ bar bar | baz ] | boo ]",
                  "/^\\s*(?:foo|ba(?:r\\s+bar|z)|boo)\\s*$/i");
    assertMatches("[ foo | [ bar bar | baz ] | boo ]",
                  "foo", "bar bar", "baz", "boo");
    assertDoesNotMatch("[ foo | [ bar bar | baz ] | boo ]",
                       "bar", "", "faz", "fooo");
  }

  public final void testBackgroundImage() throws Exception {
    assertPattern(
        "<uri> | none | inherit",
        "/^\\s*(?:url\\(\"[^()\\\\\"\\r\\n]+\"\\)|none|inherit)\\s*$/i");
    assertMatches(
        "<uri> | none | inherit", "none", "inherit", "url(\"foo.gif\")");
    assertDoesNotMatch(
        "<uri> | none | inherit",
        "gurl(\"foo.gif\")", "\"foo.gif\"", "url(\"foo.gif)",
        "url(\"foo.gif)\")", "url(\"foo.gif\\\")", "url(\"foo.gif\"\")"
        );
  }

  public final void testFontFamilies() throws Exception {
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

  public final void testOpacity() throws Exception {
    assertMatches("<number:0,1>", "0", "0.0", ".5", "0.5", "0.707", "1", "1.0");
    assertDoesNotMatch("<number:0,1>", "1.1", "-0.5", "0px", "");
  }

  public final void testNumbers() throws Exception {
    String leftSignature = "<length> | <percentage> | auto | inherit";
    assertMatches(
        leftSignature, "0", "10px", "-10.5px", "0.125em", "+10px", "110%");
    assertDoesNotMatch(leftSignature, ".in", "-px", "em");
  }

  public final void testPropertyNameToDom2Property() {
    assertEquals(
        "color",
        CssPropertyPatterns.propertyNameToDom2Property(Name.css("color")));
    assertEquals(
        "float",
        CssPropertyPatterns.propertyNameToDom2Property(Name.css("float")));
    assertEquals(
        "listStyleImage",
        CssPropertyPatterns.propertyNameToDom2Property(
            Name.css("list-style-image")));
  }

  private void assertPattern(String sig, String golden) {
    String actual = toPattern(sig);
    assertEquals(actual, golden, actual);
  }

  private void assertMatches(String sig, String... candidates)
      throws Exception {
    RhinoTestBed.runJs(
        new Executor.Input(
            ""
            + "var pattern = " + toPattern(sig) + ";"
            + "var candidates = " + render(toArrayList(candidates)) + ";"
            + "for (var i = candidates.length; --i >= 0;) {"
            + "  if (!pattern.test(candidates[i])) {"
            + "    throw new Error('' + candidates[i]);"
            + "  }"
            + "}",
            getName()));
  }

  private void assertDoesNotMatch(String sig, String... candidates)
      throws Exception {
    RhinoTestBed.runJs(
        new Executor.Input(
            ""
            + "var pattern = " + toPattern(sig) + ";"
            + "var candidates = " + render(toArrayList(candidates)) + ";"
            + "for (var i = candidates.length; --i >= 0;) {"
            + "  if (pattern.test(candidates[i])) {"
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
      literals.add(StringLiteral.valueOf(FilePosition.UNKNOWN, value));
    }
    return new ArrayConstructor(FilePosition.UNKNOWN,literals);
  }

  private static CssPropertySignature parseSignature(String sig) {
    return CssPropertySignature.Parser.parseSignature(sig);
  }
}
