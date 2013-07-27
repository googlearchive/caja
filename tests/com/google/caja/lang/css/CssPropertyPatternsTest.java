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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lang.css.CssPropertyPatterns;
import com.google.caja.lang.css.CssPropertyPatterns.CssPropertyData;
import com.google.caja.lang.css.CssSchema;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.parser.css.CssPropertySignature;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.NullLiteral;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ValueProperty;
import com.google.caja.util.CajaTestCase;
import com.google.caja.util.Function;
import com.google.caja.util.Lists;
import com.google.caja.util.Name;
import com.google.caja.util.Sets;
import com.google.caja.util.Strings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CssPropertyPatternsTest extends CajaTestCase {

  CssPropertyPatterns pp;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    pp = new CssPropertyPatterns(CssSchema.getDefaultCss21Schema(mq));
  }

  @Override
  public void tearDown() throws Exception {
    pp = null;
    super.tearDown();
  }

  public final void testInvalidSymbol() {
    try {
      toDigest("<hiybbprqag>");
    } catch (SomethingWidgyHappenedError e) {
      assertContains(e.getMessage(), "unknown CSS symbol hiybbprqag");
      return;
    }
    fail("invalid symbol succeeded");
  }

  public final void testKeywordPattern() throws ParseException {
    assertDigest("zoicks", "{\n  'lits': [ 'zoicks' ]\n}");
    assertMatches("zoicks", "zoicks", "  zoicks", " ZOICKS ");
    assertDoesNotMatch("zoicks", "zoick", "zzoicks");
  }

  public final void testUnionPattern() throws ParseException {
    assertDigest("[ foo | bar ]", "{\n  'lits': [ 'bar', 'foo' ]\n}");
    assertMatches("[ foo | bar ]", "foo", "bar", " foo ", " bar ");
    assertDoesNotMatch("[ foo | bar ]", "fo", "ar", " far ");
  }

  public final void testColor() throws ParseException {
    assertDigest(
        "'color'",
        "{\n"
        + "  'props': [ HASH_VALUE ],\n"
        + "  'fns': [ 'rgb', 'rgba' ]\n"
        + "}",
        "lits"  // Ignore all the many many color names.
        );
    assertMatches(
        "'color'", "#fff", "#aabbcc", "red", "rgb(0,0,255)",
        "rgb(0, 0, 255)", "hotpink", "yellow", "black");
    assertDoesNotMatch("'color'", "infrateal", "rgbv(0,0,255)",
                       "rgb(expression(alert(1337)))", "rgba(do,evil,now)");
  }

  public final void testExclusiveUnionPattern() {
    assertDigest("[ foo | [ a || b || c || d ] | bar ]",
                 "{\n  'lits': [ 'a', 'b', 'bar', 'c', 'd', 'foo' ]\n}");
  }

  public final void testLiteralExtraction() {
    CssPropertyPatterns pp = new CssPropertyPatterns(
        CssSchema.getDefaultCss21Schema(mq));
    String text = "[ foo || bar() ]";
    CssPropertyData actual = pp.cssPropertyToData("test", parseSignature(text));
    assertEquals("test", actual.key);
    assertEquals(actual.fns.toString(), 1, actual.fns.size());
    assertEquals("bar", actual.fns.iterator().next().getName());

  }

  public final void testReferencePattern() {
    assertDigest(
        "'background-attachment'",
        "{\n"
        + "  'lits': [ ',', 'fixed', 'local', 'scroll' ]\n"
        + "}");
  }

  public final void testMultiFoo() throws ParseException {
    assertDigest("foo*", "{\n  'lits': [ 'foo' ]\n}");
    assertMatches("foo*", "", "foo", "foo foo");
    assertDoesNotMatch("foo*", "bar", "foo bar", "bar foo foo", "foofoo");

    assertDigest("foo+", "{\n  'lits': [ 'foo' ]\n}");
    assertMatches("foo+", "foo", "foo foo", "foo  foo foo");
    assertDoesNotMatch("foo+", "bar", "foo bar", "bar  foo foo", "foofoo");

    assertDigest("foo?", "{\n  'lits': [ 'foo' ]\n}");
    assertMatches("foo?", "", "foo");
    assertDoesNotMatch("foo?", "bar", "foo bar", "foofoo");
  }

  public final void testConcatenations() throws ParseException {
    assertDigest("foo bar", "{\n  'lits': [ 'bar', 'foo' ]\n}");
    assertDigest(
        "[ a b [ c || d ] ]", "{\n  'lits': [ 'a', 'b', 'c', 'd' ]\n}");
    assertMatches("foo bar", "foo bar", "foo  bar");
    assertDoesNotMatch("foo bar", "boo far");
  }

  public final void testUnionsFolded() throws ParseException {
    assertDigest("[ foo | [ bar bar | baz ] | boo ]",
                 "{\n  'lits': [ 'bar', 'baz', 'boo', 'foo' ]\n}");
    assertMatches("[ foo | [ bar bar | baz ] | boo ]",
                  "foo", "bar bar", "baz", "boo");
    assertDoesNotMatch("[ foo | [ bar bar | baz ] | boo ]",
                       "faz", "fooo");
  }

  public final void testBackgroundImage() throws ParseException {
    assertDigest(
        "<uri> | none | inherit",
        "{\n"
        + "  'lits': [ 'inherit', 'none' ],\n"
        + "  'props': [ URL ]\n"
        + "}");
    assertMatches(
        "<uri> | none | inherit", "none", "inherit", "url(\"foo.gif\")");
    assertDoesNotMatch(
        "<uri> | none | inherit",
        "gurl(\"foo.gif\")", "\"foo.gif\"", "foo.gif"
        );
  }

  public final void testFontFamilies() throws ParseException {
    String fontFamilySignature = (
        "[[ <family-name> | <generic-family> ]"
        + " [, [ <family-name> | <generic-family> ]]* ] | inherit");
    assertMatches(
        fontFamilySignature,
        "\"Helvetica\"", "\"Arial Bold\" , sans-serif",
        // Can be fixed by quoting.
        "Arial", "Arial Bold");
    assertDoesNotMatch(
        fontFamilySignature,
        "-1", "#Arial", "Arial; Bold");
  }

  public final void testOpacity() throws ParseException {
    assertMatches("<number:0,1>", "0", "0.0", ".5", "0.5", "0.707", "1", "1.0");
    assertDoesNotMatch(
        "<number:0,1>", "zero", "'0.5'", "rgba(0, 0, 0)", "-0.5");
  }

  public final void testNumbers() throws ParseException {
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

  private void assertDigest(String sig, String golden, String... ignoreKeys) {
    String actual = toDigest(sig, ignoreKeys);
    assertEquals(actual, golden, actual);
  }

  private void assertMatches(String sig, String... cssPropertyValues)
      throws ParseException {
    assertMatch(true, sig, cssPropertyValues);
  }

  private void assertDoesNotMatch(String sig, String... cssPropertyValues)
      throws ParseException {
    assertMatch(false, sig, cssPropertyValues);
  }

  private void assertMatch(
      boolean expectedMatchResult, String sig, String... cssPropertyValues)
      throws ParseException {
    CssPropertyPatterns.CssPropertyData data = pp.cssPropertyToData(
        Strings.lower(this.getName()), parseSignature(sig));

    // Collect failures altogether and report at once.
    List<String> failures = Lists.newArrayList();

    for (String cssPropertyValue : cssPropertyValues) {
      // Lex the CSS input and filter out tokens that are not significant, and
      // join signs with quantities.
      CssLexer lexer = new CssLexer(fromString(cssPropertyValue));
      List<Token<CssTokenType>> nonWhitespaceOrCommentTokens
          = Lists.newArrayList();
      boolean adjacent = false;
      while (lexer.hasNext()) {
        Token<CssTokenType> token = lexer.next();
        switch (token.type) {
          case COMMENT: case SPACE:
            adjacent = false;
            break;
          case QUANTITY:
            // Merge sign ("-") and sign-less quantity ("31") tokens.
            if (adjacent) {
              int n = nonWhitespaceOrCommentTokens.size();
              if (n >= 0) {
                Token<CssTokenType> last
                    = nonWhitespaceOrCommentTokens.get(n-1);
                if ("+".equals(last.text) || "-".equals(last.text)) {
                  Token<CssTokenType> merged = Token.instance(
                      last.text + token.text, token.type,
                      FilePosition.span(last.pos, token.pos));
                  nonWhitespaceOrCommentTokens.set(n-1, merged);
                }
              }
            }
            break;
          default:
            nonWhitespaceOrCommentTokens.add(token);
            adjacent = true;
            break;
        }
      }

      int longestMatch = match(nonWhitespaceOrCommentTokens, 0, data);
      boolean matched = longestMatch == nonWhitespaceOrCommentTokens.size();
      if (matched != expectedMatchResult) {
        String msg;
        if (matched) {
          msg = "matched unexpectedly";
        } else {
          Token<CssTokenType> failureToken
              = nonWhitespaceOrCommentTokens.get(longestMatch);
          msg = "failed at " + failureToken.text + " @ " + failureToken.pos;
        }
        failures.add(cssPropertyValue + " : " + msg);
      }
    }
    if (!failures.isEmpty()) {
      fail(failures.toString());
    }
  }

  private int match(
      List<Token<CssTokenType>> toks, int offset,
      CssPropertyPatterns.CssPropertyData data) {
    for (int i = 0, n = toks.size(), next; i < n; i = next) {
      Token<CssTokenType> tok = toks.get(i);
      boolean allowed;
      next = i + 1;
      if (data.literals.contains(Strings.lower(tok.text))) {
        allowed = true;
      } else {
        allowed = false;
        switch (tok.type) {
          case SPACE:
          case COMMENT:
            allowed = true;
            break;
          case DIRECTIVE:
          case SYMBOL:
          case SUBSTITUTION:
            allowed = false;
            break;
          case FUNCTION:
            String calleeName = tok.text.replace("(", "");  // "foo(" -> "foo"
            // Find all signatures that could match.
            List<CssPropertySignature> candidates = Lists.newArrayList();
            for (CssPropertySignature.CallSignature fn : data.fns) {
              String name = fn.getName();
              if (name.equals(calleeName)) {
                candidates.add(fn.getArgumentsSignature());
              }
            }

            CssPropertyPatterns.CssPropertyData fnData = pp.cssPropertyToData(
                calleeName + "()",
                new CssPropertySignature.SetSignature(candidates));
            int depth = 1;
            // Find the end of the call.
            while (next < n) {
              Token<CssTokenType> nextTok = toks.get(next);
              ++next;
              if (nextTok.type == CssTokenType.FUNCTION) {
                ++depth;
              } else if (nextTok.text.equals(")")) {
                --depth;
                if (depth == 0) {
                  break;
                }
              }
            }
            // Recurse.
            List<Token<CssTokenType>> actuals = toks.subList(i + 1, next - 1);
            int fnMatch = match(actuals, offset + i + 1, fnData);
            if (fnMatch != offset + next - 1) {
              // Failed to match to inside of close parenthesis.
              return fnMatch;
            }
            // If candidates is empty, then that function is not allowed at all.
            // TODO: Is this necessary?  The empty set shouldn't match the
            // empty token list.
            allowed = !candidates.isEmpty();
            break;
          case HASH:
            allowed = data.properties.contains(CssPropBit.HASH_VALUE);
            break;
          case IDENT:
            // Can we convert it to a quoted string safely.
            allowed = data.properties.contains(CssPropBit.QSTRING)
                && data.properties.contains(CssPropBit.UNRESERVED_WORD);
            break;
          case PUNCTUATION:
            allowed = false;  // Should be in literals.
            break;
          case QUANTITY:
            allowed = data.properties.contains(
                (tok.text.startsWith("-"))
                ? CssPropBit.NEGATIVE_QUANTITY
                : CssPropBit.QUANTITY);
            break;
          case STRING:
            // Is it allowed as content, or can it be converted to url("...")
            // safely.
            if (data.properties.contains(CssPropBit.QSTRING)) {
              // A quoted string might be interpreted as an external identifier
              // like a font-family name, or as a URL.
              // Fail when ambiguous.
              int possibleInterpretations =
                  (data.properties.contains(CssPropBit.UNRESERVED_WORD) ? 1 : 0)
                  + (data.properties.contains(CssPropBit.URL) ? 1 : 0);
              allowed = possibleInterpretations <= 1;
            }
            break;
          case UNICODE_RANGE:
            allowed = false;  // TODO
            break;
          case URI:
            allowed = data.properties.contains(CssPropBit.URL);
            break;
        }
      }
      if (!allowed) {
        return i + offset;
      }
    }
    return toks.size() + offset;
  }

  private String toDigest(String sig, String... ignoreKeys) {
    CssPropertyPatterns.CssPropertyData data = pp.cssPropertyToData(
        "test", parseSignature(sig));

    // CSS
    //    ( foo || bar || <number> || baz(...) )+
    // ->
    // JS
    //    {
    //      literals: ["foo", "bar"],
    //      properties: [
    //      fns: [],
    //    }
    final FilePosition unk = FilePosition.UNKNOWN;
    ObjectConstructor obj = new ObjectConstructor(unk);
    appendIfNotEmptyOrIgnored(
        obj, "lits",
        mapToJsArray(
            Sets.newTreeSet(data.literals),
            new Function<String, Expression>() {
              public Expression apply(String s) {
                return StringLiteral.valueOf(unk, s);
              }
            }),
        ignoreKeys);
    appendIfNotEmptyOrIgnored(
        obj, "props",
        mapToJsArray(
            data.properties,
            new Function<CssPropBit, Expression>() {
              public Expression apply(CssPropBit bit) {
                return new Reference(new Identifier(unk, bit.name()));
              }
            }),
        ignoreKeys);
    appendIfNotEmptyOrIgnored(
        obj, "fns",
        mapToJsArray(
            data.fns,
            new Function<CssPropertySignature.CallSignature, Expression>() {
              public Expression apply(CssPropertySignature.CallSignature fn) {
                return StringLiteral.valueOf(unk, fn.getName());
              }
            }),
        ignoreKeys);

    return render(obj);
  }

  private static void appendIfNotEmptyOrIgnored(
      ObjectConstructor obj, String key, ArrayConstructor value,
      String... ignoreKeys) {
    if (!value.children().isEmpty()
        && !Arrays.asList(ignoreKeys).contains(key)) {
      obj.appendChild(new ValueProperty(
          StringLiteral.valueOf(FilePosition.UNKNOWN, key), value));
    }
  }

  private static <T> ArrayConstructor mapToJsArray(
      Iterable<? extends T> it,
      Function<? super T, Expression> f) {
    FilePosition unk = FilePosition.UNKNOWN;
    ArrayConstructor arr = new ArrayConstructor(
        unk, Collections.<Expression>emptyList());
    for (T el : it) {
      arr.appendChild(el == null ? new NullLiteral(unk) : f.apply(el));
    }
    return arr;
  }

  private static CssPropertySignature parseSignature(String sig) {
    return CssPropertySignature.Parser.parseSignature(sig);
  }
}
