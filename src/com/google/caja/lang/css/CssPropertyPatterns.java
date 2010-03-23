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
import com.google.caja.config.AllowedFileResolver;
import com.google.caja.config.ConfigUtil;
import com.google.caja.config.ImportResolver;
import com.google.caja.lexer.CssLexer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssPropertySignature;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.BooleanLiteral;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.RegexpLiteral;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.tools.BuildCommand;
import com.google.caja.util.Bag;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Name;
import com.google.caja.util.Pair;
import com.google.caja.util.Sets;
import com.google.caja.util.Strings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Operates on CSS property signatures to come up with a simple regular
 * expression that validates values where possible.
 *
 * <p>
 * This class produces a javascript file like<pre>
 *   var css = {
 *       "background-image": /url("[^\"\\\(\)]+")\s+/i,
 *       clear: /(?:none|left|right|both|inherit)\s+/i,
 *       color: /(?:blue|red|green|fuschia)\s+/i
 *   };
 * </pre>
 *
 * <p>
 * The {@code css} map does not contain every property in the given
 * {@link CssSchema} since some cannot be matched efficiently by values.
 * See comments on {@code [ a || b ]*} constructs inline below.
 *
 * <h3>Differences from Server Side CSS</h3>
 * <p>
 * Nor does it contain every option matched by the server side CSS processor.
 * Specifically, it does not currently match<ul>
 *   <li>function calls, e.g. {@code rect(...)} and {@code rgb(...)}
 *   <li>the {@code unreserved-word} symbol used for font-families.
 *       Font families need to be quoted, e.g. {@code "Helvetica"}.
 *       The server side rewriter takes adjacent unreserved words and quotes
 *       them, but the goal of this pattern is to allow client-side validation,
 *       not rewriting.
 *   <li>quoted strings where URLs are allowed.  E.g. {@code background-image}
 *       does not match {@code "http://foo/myimage.com"}.
 *   <li>As noted above, string literals are not allowed as URLs, but the
 *       {@code url(...)} syntax is allowed, but only containing double-quoted
 *       strings.  E.g. {@code url(foo.gif)} is not allowed though
 *       {@code url("foo.gif")} is.
 * </ul>
 *
 * <h3>Whitespace Between Tokens</h3>
 * <p>
 * The patterns in the example above all end in {@code \s+}.  This simplifies
 * a lot of the patterns since a signature like {@code foo*} strictly translates
 * to the regular expression {@code /(foo(\s+foo)*)?/i}.  Even if we repeated
 * subexpressions, we would run into problems with the signature
 * {@code a [b | c? d]? e} which could translates to a regular expression but
 * only with non-local handling of whitespace around sub-expressions that can
 * be empty.
 *
 * <p>
 * Instead, we require every literal to be followed by one or more spaces.
 * We can then match against CSS padded with spaces, as in<pre>
 *   var isValid = css[cssPropertyName].test(cssText + ' ');
 * </pre>
 *
 * <h3>Program Flow</h3>
 * <p>
 * This class examines a schema and builds a list of all allowed CSS properties.
 * It then tries to convert each property's signature to a regular expression
 * pattern.
 * It may fail for some patterns, especially the aggregate ones like
 * {@code background} which combine
 * {@code background-image}, {@code background-style}, etc.
 * <p>
 * Next it optimizes the patterns it found.  This includes flattening
 * concatenation and union operators, and moving the {@code \s+} out of unions.
 * Optimizing {@code /((blue\s+|red\s+|green\s+)|inherit\s+)/} might yield the
 * simpler expression {@code /(blue|red|green|inherit)\s+/}.
 * <p>
 * Once it has a mapping of property names to regular expressions it builds a
 * constant pool by hashing on regular expression text.
 * This allows properties with identical patterns such as {@code border-top} and
 * {@code border-bottom} to share an instance.
 * <p>
 * Finally it builds a javascript parse tree that assigns the {@code css}
 * namespace to an object whose keys are CSS property names, and whose
 * values are regular expressions.
 *
 * <h3>Caveats</h3>
 * <p>
 * Some of the regular expressions do match URLs.  If valid css text contains
 * the string 'uri' case-insensitively, then a client may need to extract and
 * rewrite URLs.  Since all strings are double quoted, this should be doable
 * without lexing CSS.
 *
 * @author mikesamuel@gmail.com
 */
public class CssPropertyPatterns {
  private final CssSchema schema;

  /**
   * Set of properties accessible on computed style of an anchor
   * (&lt;A&gt;) element or some element nested within an anchor. This
   * list is a conservative one based on the ability to do visibility,
   * containment, and layout calculations. It REQUIRES that user CSS
   * is prevented from specifying ANY of these properties in a history
   * sensitive manner (i.e., in a rule with a ":link" or ":visited"
   * predicate). Otherwise, it would allow an attacker to probe the
   * user's history as described at
   * https://bugzilla.mozilla.org/show_bug.cgi?id=147777 .
   */
  public static Set<Name> HISTORY_INSENSITIVE_STYLE_WHITELIST
      = Sets.immutableSet(
          Name.css("display"), Name.css("filter"), Name.css("float"),
          Name.css("height"), Name.css("left"), Name.css("opacity"),
          Name.css("overflow"), Name.css("position"), Name.css("right"),
          Name.css("top"), Name.css("visibility"), Name.css("width"),
          Name.css("padding-left"), Name.css("padding-right"),
          Name.css("padding-top"), Name.css("padding-bottom"));

  public CssPropertyPatterns(CssSchema schema) {
    this.schema = schema;
  }

  /**
   * Generates a regular expression for the given signature if a simple
   * regular expression exists.
   * @return null if no simple regular expression exists
   *     or the text of a Javascript regular expression like "/foo\s+/i"
   *     that matches values of the given value with one or more trailing
   *     whitespace characters.
   *     If the color property only matched the literal "blue", the resulting
   *     pattern would match "blue ".
   */
  public String cssPropertyToPattern(CssPropertySignature sig) {
    JSRE p = cssPropertyToJSRE(sig);
    if (p == null) { return null; }
    StringBuilder out = new StringBuilder();
    out.append('/');
    p.optimize().render(out);
    // Since all keywords are case insensitive and we never match text inside
    // strings.
    out.append("/i");
    return out.toString();
  }

  public Pattern cssPropertyToJavaRegex(CssPropertySignature sig) {
    JSRE p = cssPropertyToJSRE(sig);
    if (p == null) { return null; }
    StringBuilder out = new StringBuilder();
    p.render(out);
    // Since all keywords are case insensitive and we never match text inside
    // strings.
    return Pattern.compile("" + out, Pattern.CASE_INSENSITIVE);
  }

  private static class JSREBuilder {
    final JSRE p;
    final boolean identBefore;

    JSREBuilder(boolean identBefore, JSRE p) {
      this.identBefore = identBefore;
      this.p = p;
    }
  }

  private static final JSRE SPACES = JSRE.many(JSRE.raw("\\s"));
  private static final JSRE OPT_SPACES = JSRE.any(JSRE.raw("\\s"));

  private JSRE cssPropertyToJSRE(CssPropertySignature sig) {
    JSREBuilder b = sigToPattern(false, sig);
    if (b == null) { return null; }
    return JSRE.cat(JSRE.raw("^"), OPT_SPACES, b.p, OPT_SPACES, JSRE.raw("$"))
        .optimize();
  }

  private JSREBuilder sigToPattern(
      boolean identBefore, CssPropertySignature sig) {
    // Dispatch to a set of handlers that either append balanced content to
    // out, or append cruft and return null.
    if (sig instanceof CssPropertySignature.LiteralSignature) {
      return litToPattern(
          identBefore, (CssPropertySignature.LiteralSignature) sig);
    } else if (sig instanceof CssPropertySignature.RepeatedSignature) {
      return repToPattern(
          identBefore, (CssPropertySignature.RepeatedSignature) sig);
    } else if (sig instanceof CssPropertySignature.PropertyRefSignature) {
      return refToPattern(
          identBefore, (CssPropertySignature.PropertyRefSignature) sig);
    } else if (sig instanceof CssPropertySignature.SeriesSignature) {
      return seriesToPattern(
          identBefore, (CssPropertySignature.SeriesSignature) sig);
    } else if (sig instanceof CssPropertySignature.SymbolSignature) {
      return symbolToPattern(
          identBefore, (CssPropertySignature.SymbolSignature) sig);
    } else if (sig instanceof CssPropertySignature.SetSignature
               || sig instanceof CssPropertySignature.ExclusiveSetSignature) {
      return setToPattern(identBefore, sig);
    }
    return null;
  }

  private static JSREBuilder litToPattern(
      boolean identBefore, CssPropertySignature.LiteralSignature lit) {
    String litValue = lit.getValue();
    // Match some trailing whitespace.
    // Since some patterns can match nothing (e.g. foo*), we make sure that
    // all positive matches are followed by token-breaking space.
    // The pattern as a whole can then be matched against the value with one
    // space added at the end.
    boolean ident = isIdentChar(litValue.charAt(litValue.length() - 1));
    JSRE p = JSRE.lit(litValue);
    return new JSREBuilder(
        ident, JSRE.cat(ident && identBefore ? SPACES : OPT_SPACES, p));
  }

  private static boolean isIdentChar(char ch) {
    return CssLexer.isNmStart(ch) || ('0' <= ch && ch <= '9')
        || ch == '#' || ch == '.';
  }

  private JSREBuilder repToPattern(
      boolean identBefore, CssPropertySignature.RepeatedSignature sig) {
    CssPropertySignature rep = sig.getRepeatedSignature();
    if (rep instanceof CssPropertySignature.ExclusiveSetSignature) {
      // The spec (http://www.w3.org/TR/REC-CSS1/#css1-properties) defines
      // A double bar (A || B) means that either A or B or both must occur
      // in any order.
      // We convert [ a || b ] -> [a | b]+
      return exclusiveToPattern(identBefore, rep);
    }
    JSREBuilder repeatedPattern = sigToPattern(identBefore, rep);
    if (repeatedPattern == null) { return null; }
    int min = sig.minCount;
    int max = sig.maxCount;
    if (repeatedPattern.identBefore == identBefore || max == 1) {
      return new JSREBuilder(
          repeatedPattern.identBefore, JSRE.rep(repeatedPattern.p, min, max));
    } else {
      JSREBuilder tail = sigToPattern(repeatedPattern.identBefore, rep);
      if (min == 0) {
        if (max != Integer.MAX_VALUE) { --max; }
        return new JSREBuilder(
            tail.identBefore,
            JSRE.opt(
                JSRE.cat(repeatedPattern.p, JSRE.rep(tail.p, 0, max))));
      } else {
        return new JSREBuilder(
            tail.identBefore,
            JSRE.cat(repeatedPattern.p, JSRE.rep(tail.p, min - 1, max)));
      }
    }
  }

  private final Bag<String> refsUsed = Bag.newHashBag();

  private JSREBuilder refToPattern(
      boolean identBefore, CssPropertySignature.PropertyRefSignature sig) {
    refsUsed.incr(sig.getPropertyName().getCanonicalForm());
    CssSchema.CssPropertyInfo p = schema.getCssProperty(sig.getPropertyName());
    return p != null ? sigToPattern(identBefore, p.sig) : null;
  }

  private JSREBuilder seriesToPattern(
      boolean identBefore, CssPropertySignature.SeriesSignature sig) {
    List<JSRE> children = Lists.newArrayList();
    for (CssPropertySignature child : sig.children()) {
      JSREBuilder b = sigToPattern(identBefore, child);
      if (b == null) { return null; }
      children.add(b.p);
      identBefore = b.identBefore;
    }
    return new JSREBuilder(identBefore, JSRE.cat(children));
  }

  private static final Name COLOR = Name.css("color");
  private static final Name STANDARD_COLOR = Name.css("color-standard");
  private JSREBuilder symbolToPattern(
      boolean identBefore, CssPropertySignature.SymbolSignature sig) {
    Name symbolName = sig.getValue();
    refsUsed.incr(symbolName.getCanonicalForm());
    JSRE builtinMatch = builtinToPattern(symbolName);
    if (builtinMatch != null) {
      String re = builtinMatch.toString();
      boolean ident = isIdentChar(re.charAt(0));
      boolean identAfter = isIdentChar(re.charAt(re.length() - 1));
      return new JSREBuilder(
          identAfter,
          JSRE.cat(identBefore && ident ? SPACES : OPT_SPACES, builtinMatch));
    }
    CssSchema.SymbolInfo s = schema.getSymbol(symbolName);
    if (COLOR.equals(symbolName)) {
      // Don't blow up the regexs by including the entire X11 color set over
      // and over.
      CssSchema.SymbolInfo standard = schema.getSymbol(STANDARD_COLOR);
      if (standard != null) { s = standard; }
    }
    return s != null ? sigToPattern(identBefore, s.sig) : null;
  }

  private JSREBuilder setToPattern(
      boolean identBefore, CssPropertySignature sig) {
    if (sig.children().isEmpty()) { return null; }
    List<JSRE> children = Lists.newArrayList();
    boolean identAfter = false;
    for (CssPropertySignature child : sig.children()) {
      JSREBuilder b = sigToPattern(identBefore, child);
      if (b != null) {
        children.add(b.p);
        identAfter |= b.identBefore;
      }
    }
    if (children.isEmpty()) { return null; }
    return new JSREBuilder(identAfter, JSRE.alt(children));
  }

  // TODO(jasvir): Clarify the meaning of (a||b)* and modify this function
  // if necessary to reflect it.
  private JSREBuilder exclusiveToPattern(
      boolean identBefore, CssPropertySignature sig) {
    if (sig.children().isEmpty()) { return null; }
    List<JSRE> head = Lists.newArrayList();
    boolean identAfterHead = false;
    for (CssPropertySignature child : sig.children()) {
      JSREBuilder b = sigToPattern(identBefore, child);
      if (b != null) {
        head.add(b.p);
        identAfterHead = b.identBefore;
      }
    }
    List<JSRE> tail = Lists.newArrayList();
    boolean identAfterTail = false;
    for (CssPropertySignature child : sig.children()) {
      JSREBuilder b = sigToPattern(identAfterHead, child);
      if (b != null) {
        tail.add(b.p);
        identAfterTail = b.identBefore;
      }
    }
    if (tail.isEmpty()) { return null; }
    return new JSREBuilder(
        identAfterTail,
        JSRE.cat(JSRE.alt(head), JSRE.rep(JSRE.alt(tail), 0, tail.size() - 1)));
  }

  private static final Map<String, JSRE> BUILTINS;
  static {
    // http://www.w3.org/TR/REC-CSS2/syndata.html
    JSRE zero = JSRE.lit("0");
    JSRE one = JSRE.lit("1");
    JSRE digit = JSRE.raw("\\d");
    JSRE digits = JSRE.many(digit);
    JSRE dot = JSRE.raw("\\.");
    JSRE fraction = JSRE.cat(dot, digits);
    JSRE pct = JSRE.lit("%");
    JSRE minus = JSRE.lit("-");
    JSRE sign = JSRE.alt(JSRE.lit("+"), minus);
    JSRE unsignedNum = JSRE.cat(digits, JSRE.opt(fraction));
    JSRE signedNum = JSRE.cat(JSRE.opt(sign), unsignedNum);
    JSRE angleUnits = JSRE.alt(
        JSRE.alt(JSRE.lit("rad"), JSRE.lit("grad")),
        JSRE.lit("deg"));
    JSRE freqUnits = JSRE.alt(JSRE.lit("Hz"), JSRE.lit("kHz"));
    JSRE lengthUnits = JSRE.alt(
        JSRE.alt(JSRE.lit("cm"), JSRE.lit("mm"), JSRE.lit("em")),
        JSRE.lit("ex"), JSRE.lit("in"),
        JSRE.alt(JSRE.lit("pc"), JSRE.lit("pt"), JSRE.lit("px")));
    JSRE timeUnits = JSRE.alt(JSRE.lit("ms"), JSRE.lit("s"));
    JSRE quotedIdentifiers = JSRE.raw(
        "\"\\w(?:[\\w-]*\\w)(?:\\s+\\w([\\w-]*\\w))*\"");
    JSRE hash = JSRE.lit("#");
    JSRE hex = JSRE.alt(digit, JSRE.lit("a"), JSRE.lit("b"), JSRE.lit("c"),
                        JSRE.lit("d"), JSRE.lit("e"), JSRE.lit("f"));
    BUILTINS = Maps.<String, JSRE>immutableMap()
        .put("number:0,", JSRE.alt(zero, unsignedNum).optimize())
        .put("number:0,1", JSRE.alt(
            JSRE.cat(zero, JSRE.opt(fraction)),
            fraction,
            JSRE.cat(one, JSRE.opt(JSRE.cat(dot, JSRE.many(zero)))))
            .optimize())
        .put("number", JSRE.alt(zero, signedNum).optimize())
        .put("percentage:0,", JSRE.alt(zero, JSRE.cat(unsignedNum, pct))
             .optimize())
        .put("percentage", JSRE.alt(zero, JSRE.cat(signedNum, pct)).optimize())
        .put("angle:0", JSRE.alt(zero, JSRE.cat(unsignedNum, angleUnits)))
        .put("angle", JSRE.alt(zero, JSRE.cat(signedNum, angleUnits)))
        .put("frequency", JSRE.alt(zero, JSRE.cat(unsignedNum, freqUnits)))
        .put("length:0,", JSRE.alt(zero, JSRE.cat(unsignedNum, lengthUnits)))
        .put("length", JSRE.alt(zero, JSRE.cat(signedNum, lengthUnits)))
        .put("time:0,", JSRE.alt(zero, JSRE.cat(unsignedNum, timeUnits)))
        .put("time", JSRE.alt(zero, JSRE.cat(signedNum, timeUnits)))
        .put("integer", JSRE.cat(JSRE.opt(minus), digits))
        .put("integer:0,", digits)
        .put("hex-color", JSRE.cat(hash, JSRE.rep(JSRE.rep(hex, 3, 3), 1, 2)))
        .put("specific-voice", quotedIdentifiers)
        .put("family-name", quotedIdentifiers)
        .put("uri", JSRE.cat(
            JSRE.lit("url(\""),
            JSRE.many(JSRE.raw("[^()\\\\\"\\r\\n]")),
            JSRE.lit("\")")))
        .create();
  }
  private JSRE builtinToPattern(Name name) {
    String key = name.getCanonicalForm();
    JSRE p = BUILTINS.get(key);
    if (p == null) {
      int comma = key.lastIndexOf(':');
      if (comma >= 0) {
        p = BUILTINS.get(key.substring(0, comma));
      }
    }
    return p;
  }

  public static void generatePatterns(CssSchema schema, Appendable out)
      throws IOException {
    FilePosition unk = FilePosition.UNKNOWN;
    CssPropertyPatterns pp = new CssPropertyPatterns(schema);
    List<CssSchema.CssPropertyInfo> props
        = Lists.newArrayList(schema.getCssProperties());
    Collections.sort(
        props, new Comparator<CssSchema.CssPropertyInfo>() {
          public int compare(CssSchema.CssPropertyInfo a,
                             CssSchema.CssPropertyInfo b) {
            return a.name.compareTo(b.name);
          }
        });
    Set<String> commonSubstrings = Sets.newLinkedHashSet();
    commonSubstrings.add("|left|center|right");
    commonSubstrings.add("|top|center|bottom");
    // Seed with some known constant strings.
    for (Name commonSymbol : new Name[] {
        // Derived by counting symbol names into a bag in symbolToPattern above.
        COLOR, Name.css("length"), Name.css("length:0,"),
        Name.css("border-style"), Name.css("border-width"),
        Name.css("bg-position"), Name.css("bg-size"),
        Name.css("percentage"), Name.css("percentage:0,"), Name.css("uri"),
        Name.css("repeat-style") }) {
      CssPropertySignature.SymbolSignature sig
          = (CssPropertySignature.SymbolSignature)
            CssPropertySignature.Parser.parseSignature(
                "<" + commonSymbol + ">");
      JSREBuilder p = pp.symbolToPattern(false, sig);
      if (p != null) {
        commonSubstrings.add("" + withoutSpacesOrZero(p.p.optimize()));
      }
    }
    Map<String, int[]> regexPoolMap = Maps.newHashMap();
    Map<String, Integer> commonSubstringMap = Maps.newLinkedHashMap();
    List<Pair<CssSchema.CssPropertyInfo, String>> patterns
        = Lists.newArrayList();
    List<Expression> stringPool = Lists.newArrayList();
    List<Expression> regexPool = Lists.newArrayList();

    for (CssSchema.CssPropertyInfo prop : props) {
      String pattern = pp.cssPropertyToPattern(prop.sig);
      if (!schema.isPropertyAllowed(prop.name)) { continue; }
      if (pattern != null && !"(?:inherit\\s)".equals(pattern)) {
        patterns.add(Pair.pair(prop, pattern));
      }
    }

    for (String s : commonSubstrings) {
      int n = 0;
      for (Pair<CssSchema.CssPropertyInfo, String> p : patterns) {
        String pattern = p.b;
        for (int index = -1; (index = pattern.indexOf(s, index + 1)) >= 0;) {
          ++n;
        }
      }
      if (n > 1) {
        int poolIndx = stringPool.size();
        stringPool.add(StringLiteral.valueOf(unk, s));
        commonSubstringMap.put(s, poolIndx);
      }
    }

    for (Pair<CssSchema.CssPropertyInfo, String> p : patterns) {
      String pattern = p.b;
      // Keep track of which patterns appear more than once so we can use
      // a constant pool.
      int[] pool = regexPoolMap.get(pattern);
      if (pool == null) {
        regexPoolMap.put(pattern, new int[] { -1 });
      } else if (pool[0] == -1) {
        pool[0] = regexPool.size();
        regexPool.add(makeRegexp(commonSubstringMap, pattern));
      }
    }

    Statement poolDecls = null;
    if (!stringPool.isEmpty()) {
      poolDecls = new Declaration(
          unk, new Identifier(unk, "s"), new ArrayConstructor(unk, stringPool));
    }
    if (!regexPool.isEmpty()) {
      Declaration d = new Declaration(
          unk, new Identifier(unk, "c"), new ArrayConstructor(unk, regexPool));
      poolDecls = (poolDecls == null
          ? d
          : new MultiDeclaration(
              unk, Arrays.asList((Declaration) poolDecls, d)));
    }
    List<Pair<Literal, Expression>> members = Lists.newArrayList();
    List<Pair<Literal, Expression>> alternates = Lists.newArrayList();
    for (Pair<CssSchema.CssPropertyInfo, String> p : patterns) {
      int poolIndex = regexPoolMap.get(p.b)[0];
      Expression re = poolIndex < 0
          ? makeRegexp(commonSubstringMap, p.b)
          : (Expression) QuasiBuilder.substV(
              "c[@i]", "i", new IntegerLiteral(unk, poolIndex));
      Literal name = StringLiteral.valueOf(unk, p.a.name.getCanonicalForm());
      members.add(Pair.pair(name, re));

      String dom2property = propertyNameToDom2Property(p.a.name);
      ArrayConstructor altNames = null;
      for (String altDom2Property : p.a.dom2properties) {
        if (altDom2Property.equals(dom2property)) { continue; }
        if (altNames == null) {
          altNames = new ArrayConstructor(
              unk, Collections.<Expression>emptyList());
          alternates.add(Pair.pair(
              (Literal) StringLiteral.valueOf(unk, dom2property),
              (Expression) altNames));
        }
        altNames.appendChild(StringLiteral.valueOf(unk, altDom2Property));
      }
    }

    List<Pair<Literal, Expression>> historyInsensitiveStyleWhitelistEls
        = Lists.newArrayList();
    for (Name propertyName : HISTORY_INSENSITIVE_STYLE_WHITELIST) {
      historyInsensitiveStyleWhitelistEls.add(Pair.<Literal, Expression>pair(
          StringLiteral.valueOf(unk, propertyName.getCanonicalForm()),
          new BooleanLiteral(unk, true)));
    }

    ObjectConstructor cssPropConstructor = new ObjectConstructor(unk, members);
    ObjectConstructor alternateNames = new ObjectConstructor(unk, alternates);
    ObjectConstructor historyInsensitiveStyleWhitelist
        = new ObjectConstructor(unk, historyInsensitiveStyleWhitelistEls);

    ParseTreeNode js = QuasiBuilder.substV(
        ""
        + "var css = {"
        + "  properties: (function () {"
        + "    @poolDecls?;"
        + "    return @cssPropConstructor;"
        + "  })(),"
        + "  alternates: @alternates,"
        + "  HISTORY_INSENSITIVE_STYLE_WHITELIST: "
        + "      @historyInsensitiveStyleWhitelist"
        + "};",
        "poolDecls", poolDecls,
        "cssPropConstructor", cssPropConstructor,
        "alternates", alternateNames,
        "historyInsensitiveStyleWhitelist", historyInsensitiveStyleWhitelist);
    TokenConsumer tc = js.makeRenderer(out, null);
    js.render(new RenderContext(tc));
    tc.noMoreTokens();
    out.append("\n");
  }

  private static Expression makeRegexp(
      Map<String, Integer> commonSubstrings, String regex) {
    FilePosition unk = FilePosition.UNKNOWN;
    List<Pair<String, Expression>> substrings = Lists.newArrayList();
    for (Map.Entry<String, Integer> e : commonSubstrings.entrySet()) {
      substrings.add(Pair.pair(e.getKey(), (Expression) QuasiBuilder.substV(
          "s[@i]", "i", new IntegerLiteral(unk, e.getValue()))));
    }
    List<Expression> parts = Lists.newArrayList();
    assert regex.startsWith("/") && regex.endsWith("/i");
    String pattern = regex.substring(1, regex.length() - 2);
    makeRegexpOnto(substrings, pattern, 0, parts);
    if (parts.size() == 1 && parts.get(0) instanceof StringLiteral) {
      return new RegexpLiteral(unk, regex);
    } else {
      Expression e = parts.get(0);
      for (int i = 1, n = parts.size(); i < n; ++i) {
        e = Operation.createInfix(Operator.ADDITION, e, parts.get(i));
      }
      return (Expression) QuasiBuilder.substV(
          "RegExp(@pattern, 'i')", "pattern", e);
    }
  }

  private static void makeRegexpOnto(
      List<Pair<String, Expression>> strs, String pattern, int index,
      List<Expression> parts) {
    if ("".equals(pattern)) { return; }
    for (int n = strs.size(); index < n; ++index) {
      Pair<String, Expression> commonString = strs.get(index);
      String s = commonString.a;
      int pos = pattern.indexOf(s);
      if (pos >= 0) {
        makeRegexpOnto(strs, pattern.substring(0, pos), index + 1, parts);
        parts.add(commonString.b);
        makeRegexpOnto(strs, pattern.substring(pos + s.length()), index, parts);
        return;
      }
    }
    parts.add(StringLiteral.valueOf(FilePosition.UNKNOWN, pattern));
  }

  /**
   * Spaces and zero tend to get moved/merged frequently during
   * regex optimization so don't consider them when doing common substring
   * matching.
   */
  private static JSRE withoutSpacesOrZero(JSRE p) {
    if (p instanceof JSRE.Atom) {
      String s = ((JSRE.Atom) p).atom;
      return ("0".equals(s) || "\\s".equals(s)) ? null : p;
    } else if (p instanceof JSRE.Concatenation) {
      JSRE.Concatenation c = (JSRE.Concatenation) p;
      List<JSRE> children = Lists.newArrayList(c.children);
      while (!children.isEmpty()) {
        JSRE p0 = children.get(0);
        JSRE p0w = withoutSpacesOrZero(p0);
        if (p0w == null) {
          children.remove(0);
        } else {
          children.set(0, p0w);
          break;
        }
      }
      if (children.isEmpty()) { return null; }
      return JSRE.cat(children).optimize();
    } else if (p instanceof JSRE.Alternation) {
      List<JSRE> children = ((JSRE.Alternation) p).children;
      List<JSRE> childrenNew = Lists.newArrayList();
      for (JSRE child : children) {
        JSRE wo = withoutSpacesOrZero(child);
        if (wo != null) { childrenNew.add(wo); }
      }
      if (childrenNew.isEmpty()) { return null; }
      return JSRE.alt(childrenNew);
    } else if (p instanceof JSRE.Repetition) {
      JSRE.Repetition rep = (JSRE.Repetition) p;
      JSRE body = withoutSpacesOrZero(rep.body);
      return body != null
          ? JSRE.rep(body, rep.min, rep.max).optimize()
          : null;
    }
    return p;
  }

  public static class Builder implements BuildCommand {
    public boolean build(List<File> inputs, List<File> deps, File output)
        throws IOException {
      File symbolsAndPropertiesFile = null;
      File functionsFile = null;
      for (File input : inputs) {
        if (input.getName().endsWith(".json")) {
          if (symbolsAndPropertiesFile == null) {
            symbolsAndPropertiesFile = input;
          } else if (functionsFile == null) {
            functionsFile = input;
          } else {
            throw new IOException("Unused input " + input);
          }
        }
      }
      if (symbolsAndPropertiesFile == null) {
        throw new IOException("No JSON whitelist for CSS Symbols + Properties");
      }
      if (functionsFile == null) {
        throw new IOException("No JSON whitelist for CSS Functions");
      }

      FilePosition sps = FilePosition.startOfFile(new InputSource(
          symbolsAndPropertiesFile.getAbsoluteFile().toURI()));
      FilePosition fns = FilePosition.startOfFile(new InputSource(
          functionsFile.getAbsoluteFile().toURI()));

      MessageContext mc = new MessageContext();
      mc.addInputSource(sps.source());
      mc.addInputSource(fns.source());
      MessageQueue mq = new EchoingMessageQueue(
          new PrintWriter(new OutputStreamWriter(System.err), true), mc, false);

      Set<File> inputsAndDeps = Sets.newHashSet();
      for (File f : inputs) { inputsAndDeps.add(f.getAbsoluteFile()); }
      for (File f : deps) { inputsAndDeps.add(f.getAbsoluteFile()); }

      ImportResolver resolver = new AllowedFileResolver(inputsAndDeps);

      CssSchema schema;
      try {
        schema = new CssSchema(
            ConfigUtil.loadWhiteListFromJson(
                sps.source().getUri(), resolver, mq),
            ConfigUtil.loadWhiteListFromJson(
                fns.source().getUri(), resolver, mq));
      } catch (ParseException ex) {
        ex.toMessageQueue(mq);
        throw (IOException) new IOException("Failed to parse schema")
            .initCause(ex);
      }

      Writer out = new OutputStreamWriter(
          new FileOutputStream(output), "UTF-8");
      String currentDate = "" + new Date();
      if (currentDate.indexOf("*/") >= 0) {
        throw new SomethingWidgyHappenedError("Date should not contain '*/'");
      }
      out.write("/* Copyright Google Inc.\n");
      out.write(" * Licensed under the Apache Licence Version 2.0\n");
      out.write(" * Autogenerated at " + currentDate + "\n");
      out.write(" * @provides css\n");
      out.write(" */\n");
      try {
        generatePatterns(schema, out);
      } finally {
        out.close();
      }
      return true;
    }
  }

  /**
   * Converts a css property name to a javascript identifier, e.g.
   * {@code background-color} => {@code backgroundColor}.
   */
  static String propertyNameToDom2Property(Name cssPropertyName) {
    String lcaseDashed = cssPropertyName.getCanonicalForm();
    int dash = lcaseDashed.indexOf('-');
    if (dash < 0) { return lcaseDashed; }
    StringBuilder sb = new StringBuilder(lcaseDashed.length());
    int written = 0;
    do {
      sb.append(lcaseDashed, written, dash);
      written = dash + 1;
      if (written < lcaseDashed.length()) {
        sb.append(Strings.toUpperCase(
            lcaseDashed.substring(written, written + 1)));
        ++written;
      }
      dash = lcaseDashed.indexOf('-', written);
    } while (dash >= 0);
    sb.append(lcaseDashed, written, lcaseDashed.length());
    return sb.toString();
  }

  public static void main(String[] args) throws IOException {
    CssSchema schema = CssSchema.getDefaultCss21Schema(
        new SimpleMessageQueue());
    generatePatterns(schema, System.out);
  }
}
