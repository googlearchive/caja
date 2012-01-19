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
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.RegexpLiteral;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ValueProperty;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.tools.BuildCommand;
import com.google.caja.util.Bag;
import com.google.caja.util.Charsets;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Name;
import com.google.caja.util.Pair;
import com.google.caja.util.Sets;
import com.google.caja.util.Strings;
import com.google.common.collect.ImmutableMap;

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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Operates on CSS property signatures to come up with a simple regular
 * expression that validates values where possible.
 *
 * <p>
 * This class produces a javascript file like<pre>
 *   var CSS_PROP_BIT_x = ...;
 *   // Sets of allowed literal tokens.
 *   var CSS_LIT_GROUP = [["auto",...],...];
 *   var CSS_REGEX = [/^...$/];
 *   var cssSchema = {
 *     "float": {
 *       // Describe the kinds of tokens that can appear in the named
 *       // property's value and any additional restrictions.
 *       cssPropBits: CSS_PROP_BIT_x | CSS_PROP_BIT_y | ...,
 *       // Aliases for the named property.
 *       cssAlternates: ["cssFloat", "styleFloat"],
 *       // Groups of literal values allowed.
 *       cssLitGroup: [CSS_LIT_GROUP[1],CSS_LIT_GROUP[3],CSS_LIT_GROUP[16]],
 *       // Matches any other tokens that can be part of the value including
 *       // function calls like rgba(...).
 *       cssExtra: CSS_REGEX[2]
 *     },
 *     ...
 *   };
 * </pre>
 *
 * <p>
 * The {@code css} map does not contain every property in the given
 * {@link CssSchema} since some cannot be matched efficiently by values.
 * See comments on {@code [ a || b ]*} constructs inline below.
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
  // TODO: Why is padding not included?  How was this derived?

  public CssPropertyPatterns(CssSchema schema) {
    this.schema = schema;
  }

  static class CssPropertyData {
    final @Nullable String regex;
    final EnumSet<CssPropBit> properties;
    final Set<String> literals;

    CssPropertyData(
        @Nullable String regex, EnumSet<CssPropBit> properties,
        Set<String> literals) {
      this.regex = regex;
      this.properties = properties;
      this.literals = literals;
    }
  }

  /**
   * Generates a regular expression for the given signature if a simple
   * regular expression exists.
   * @return null if no simple regular expression exists
   *     or the text of a JavaScript regular expression like "/foo\s+/i"
   *     that matches values of the given value with one or more trailing
   *     whitespace characters.
   *     If the color property only matched the literal "blue", the resulting
   *     pattern would match "blue ".
   */
  public CssPropertyData cssPropertyToPattern(
      CssPropertySignature sig, boolean complete) {
    return new Inspector(complete).cssPropertyToPattern(sig);
  }

  public Pattern cssPropertyToJavaRegex(CssPropertySignature sig) {
    JSRE p = new Inspector(true).inspect(sig);
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
      assert p != null;
      this.identBefore = identBefore;
      this.p = p;
    }
  }

  private static final JSRE SPACES = JSRE.many(JSRE.raw("\\s"));
  private static final JSRE OPT_SPACES = JSRE.any(JSRE.raw("\\s"));
  // When rendering to be used as part of the client side JS parser, we can
  // rely on all CSS comments and space tokens to have been reduced to a single
  // space character.
  private static final JSRE SPACES_NORMALIZED = JSRE.many(JSRE.raw(" "));
  private static final JSRE OPT_SPACES_NORMALIZED = JSRE.any(SPACES_NORMALIZED);

  private static boolean isIdentChar(char ch) {
    return CssLexer.isNmStart(ch) || ('0' <= ch && ch <= '9')
        || ch == '#' || ch == '.';
  }

  private static final Name COLOR = Name.css("color");
  private static final Name STANDARD_COLOR = Name.css("color-standard");

  private class Inspector {
    final EnumSet<CssPropBit> props;
    final Set<String> literals;
    /**
     * True to include literals and patterns inferrable from props in the regex
     * so as to produce a complete regular expression instead of one focused
     * on just matching the portions of a normalized tokens set not matched
     * by property bits, or literal groups.
     */
    final boolean complete;
    private final Bag<String> refsUsed;

    final JSRE spaces, optSpaces;

    Inspector(boolean complete) {
      this(complete, Bag.<String>newHashBag());
    }

    private Inspector(boolean complete, Bag<String> refsUsed) {
      this.props = EnumSet.noneOf(CssPropBit.class);
      this.literals = Sets.newHashSet();
      this.complete = complete;
      this.refsUsed = refsUsed;
      this.spaces = complete ? SPACES : SPACES_NORMALIZED;
      this.optSpaces = complete ? OPT_SPACES : OPT_SPACES_NORMALIZED;
    }

    /**
     * Generates a regular expression for the given signature if a simple
     * regular expression exists.
     * @return null if no simple regular expression exists
     *     or the text of a JavaScript regular expression like "/^foo$/i"
     *     that matches values of the given value.
     *     If the color property only matched the literal "blue", the resulting
     *     pattern would match "blue ".
     */
    CssPropertyData cssPropertyToPattern(CssPropertySignature sig) {
      JSRE p = inspect(sig);
      String regex = null;
      if (p != null) {
        StringBuilder out = new StringBuilder();
        out.append('/');
        p.render(out);
        // Since all keywords are case insensitive and we never match text
        // inside strings.
        out.append("/i");
        regex = out.toString();
      }
      return new CssPropertyData(
          regex, EnumSet.copyOf(props), Sets.newHashSet(literals));
    }

    JSRE inspect(CssPropertySignature sig) {
      this.literals.clear();
      this.props.clear();
      JSREBuilder b = sigToPattern(false, sig);
      if (b == null) { return null; }
      // TODO(mikesamuel): are the optSpaces really necessary?
      return JSRE.cat(JSRE.raw("^"), optSpaces, b.p, optSpaces, JSRE.raw("$"))
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
      } else if (sig instanceof CssPropertySignature.CallSignature) {
        return callToPattern(
            identBefore, (CssPropertySignature.CallSignature) sig);
      }
      return null;
    }

    private JSREBuilder litToPattern(
        boolean identBefore, CssPropertySignature.LiteralSignature lit) {
      String litValue = lit.getValue();
      // Match some trailing whitespace.
      // Since some patterns can match nothing (e.g. foo*), we make sure that
      // all positive matches are followed by token-breaking space.
      // The pattern as a whole can then be matched against the value with one
      // space added at the end.
      literals.add(litValue);
      if (!complete) { return null; }
      boolean ident = isIdentChar(litValue.charAt(litValue.length() - 1));
      JSRE p = JSRE.lit(litValue);
      if (p == null) { return null; }
      return new JSREBuilder(
          ident, JSRE.cat(ident && identBefore ? spaces : optSpaces, p));
    }

    private JSREBuilder repToPattern(
        boolean identBefore, CssPropertySignature.RepeatedSignature sig) {
      CssPropertySignature rep = sig.getRepeatedSignature();
      if (rep instanceof CssPropertySignature.ExclusiveSetSignature) {
        // The spec (http://www.w3.org/TR/REC-CSS1/#css1-properties) defines
        // A double bar (A || B) means that either A or B or both must occur
        // in any order.
        // We convert [ a || b ] -> [a | b]+
        // So according to the spec,
        //    a
        //    a b
        //    b
        //    b a
        // are all ok, but
        //    a a
        // is not, though it is allowed by our regular expression to avoid
        // exploding the size of the regular expression or requiring complicated
        // backtracking.
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
        if (max != Integer.MAX_VALUE) { --max; }
        JSRE spaceSeparated = JSRE.cat(
            repeatedPattern.p, JSRE.rep(tail.p, min != 0 ? min - 1 : 0, max));
        if (min == 0) {
          spaceSeparated = JSRE.opt(spaceSeparated);
        }
        return new JSREBuilder(tail.identBefore, spaceSeparated);
      }
    }

    private JSREBuilder refToPattern(
        boolean identBefore, CssPropertySignature.PropertyRefSignature sig) {
      refsUsed.incr(sig.getPropertyName().getCanonicalForm());
      CssSchema.CssPropertyInfo p = schema.getCssProperty(
          sig.getPropertyName());
      return p != null ? sigToPattern(identBefore, p.sig) : null;
    }

    private JSREBuilder seriesToPattern(
        boolean identBefore, CssPropertySignature.SeriesSignature sig) {
      List<JSRE> children = Lists.newArrayList();
      for (CssPropertySignature child : sig.children()) {
        JSREBuilder b = sigToPattern(identBefore, child);
        if (children != null) {
          if (b != null) {
            children.add(b.p);
            identBefore = b.identBefore;
          } else if (complete) {
            children = null;
          }
        }
      }
      if (children == null) { return null; }
      return new JSREBuilder(identBefore, JSRE.cat(children));
    }

    private JSREBuilder symbolToPattern(
        boolean identBefore, CssPropertySignature.SymbolSignature sig) {
      Name symbolName = sig.getValue();
      refsUsed.incr(symbolName.getCanonicalForm());
      JSRE builtinMatch = builtinToPattern(symbolName);
      if (builtinMatch != null) {
        String re = builtinMatch.toString();
        boolean ident = isIdentChar(re.charAt(0));
        boolean identAfter = isIdentChar(re.charAt(re.length() - 1));
        // TODO(mikesamuel):
        // This whole way of computing identAfter for regular expressions is
        // horribly broken, and the lookup table should just have a bit along
        // with the regex.
        //
        // Felix points out:
        // Aren't ident and identAfter going to be wrong for builtins like
        // <integer>?
        //
        // example, the spec for text-shadow has
        //   <length>{2,4}
        // which gets translated to
        //   <length> (?: \s* <length>){1,4}
        //
        // which means "00" with no spaces will count as two matches of
        // <length>.
        //
        // This seems harmless, so I'm not sure it's worth fixing, but maybe
        // add a note.
        return new JSREBuilder(
            identAfter,
            JSRE.cat(identBefore && ident ? spaces : optSpaces, builtinMatch));
      }
      CssSchema.SymbolInfo s = schema.getSymbol(symbolName);
      if (COLOR.equals(symbolName) && complete) {
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

    private JSREBuilder callToPattern(
        boolean identBefore, CssPropertySignature.CallSignature sig) {
      Iterator<? extends CssPropertySignature> sigs = sig.children().iterator();
      List<JSRE> children = Lists.newArrayList();
      // Use a complete inspector so we get regular expressions for calls in
      // the output because they cannot be matched by other means.  Do not
      // record as literals literals appearing in the call body.
      Inspector inspector = new Inspector(true, refsUsed);
      JSREBuilder fnPattern = inspector.sigToPattern(identBefore, sigs.next());
      if (fnPattern == null) { return null; }
      children.add(fnPattern.p);
      children.add(JSRE.lit("("));
      boolean first = true;
      while (sigs.hasNext()) {
        if (first) {
          first = false;
        } else {
          children.add(JSRE.lit(","));
        }
        JSREBuilder actual = inspector.sigToPattern(false, sigs.next());
        if (actual == null) { return null; }
        children.add(actual.p);
      }
      children.add(optSpaces);
      children.add(JSRE.lit(")"));
      return new JSREBuilder(false, JSRE.cat(children));
    }

    /**
     * (a||b) means (a | b | a b | b a) but we implement it as (a | b){1,2}
     * which admits (a a | b b) too.
     */
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
          JSRE.cat(JSRE.alt(head),
                   JSRE.rep(JSRE.alt(tail), 0, tail.size() - 1)));
    }

    private JSRE builtinToPattern(Name name) {
      String key = name.getCanonicalForm();
      int colon = key.lastIndexOf(':');
      String baseKey = colon >= 0 ? key.substring(0, colon) : key;
      CssPropBit b = BUILTIN_PROP_BITS.get(baseKey);
      if (b != null) {
        this.props.add(b);
        // The negative bit allows for some schemas to reject positioning
        // outside the parents bounding boxes, and negative offsets for clip
        // regions.
        if (b == CssPropBit.QUANTITY && colon < 0) {
          // TODO: maybe tighten this condition
          this.props.add(CssPropBit.NEGATIVE_QUANTITY);
        }
        if (!complete) { return null; }
      }
      JSRE p = BUILTINS.get(key);
      if (p == null && key != baseKey) {
        p = BUILTINS.get(baseKey);
      }
      return p;
    }
  }

  private static final Map<String, CssPropBit> BUILTIN_PROP_BITS
      = ImmutableMap.<String, CssPropBit>builder()
        .put("number", CssPropBit.QUANTITY)
        .put("percentage", CssPropBit.QUANTITY)
        .put("angle", CssPropBit.QUANTITY)
        .put("frequency", CssPropBit.QUANTITY)
        .put("length", CssPropBit.QUANTITY)
        .put("time", CssPropBit.QUANTITY)
        .put("integer", CssPropBit.QUANTITY)
        .put("hex-color", CssPropBit.HASH_VALUE)
        .put("specific-voice", CssPropBit.QSTRING_CONTENT)
        .put("family-name", CssPropBit.QSTRING_CONTENT)
        .put("uri", CssPropBit.QSTRING_URL)
        .build();

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
        .put("percentage:0,100", JSRE.alt(zero, JSRE.cat(unsignedNum, pct))
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
        .put("integer:0,255", digits)
        .put("hex-color", JSRE.cat(hash, JSRE.rep(JSRE.rep(hex, 3, 3), 1, 2)))
        .put("specific-voice", quotedIdentifiers)
        .put("family-name", quotedIdentifiers)
        .put("uri", JSRE.cat(
            JSRE.lit("url(\""),
            JSRE.many(JSRE.raw("[^()\\\\\"\\r\\n]")),
            JSRE.lit("\")")))
        .create();
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
    Inspector inspector = pp.new Inspector(false);
    // Seed with some known constant strings.
    for (Name commonSymbol : new Name[] {
        // Derived by counting symbol names into a bag in symbolToPattern above.
        COLOR, Name.css("standard-color"),
        Name.css("length"), Name.css("length:0,"),
        Name.css("border-style"), Name.css("border-width"),
        Name.css("bg-position"), Name.css("bg-size"),
        Name.css("percentage"), Name.css("percentage:0,"),
        Name.css("uri"), Name.css("repeat-style") }) {
      CssPropertySignature.SymbolSignature sig
          = (CssPropertySignature.SymbolSignature)
            CssPropertySignature.Parser.parseSignature(
                "<" + commonSymbol + ">");
      JSREBuilder p = inspector.symbolToPattern(false, sig);
      if (p != null) {
        String commonSubstring = withoutSpacesOrZero(p.p.optimize()).toString();
        if (commonSubstring.length() != 0) {
          commonSubstrings.add(commonSubstring);
        }
      }
    }
    Map<String, int[]> regexPoolMap = Maps.newHashMap();
    Map<String, Integer> commonSubstringMap = Maps.newLinkedHashMap();
    List<Pair<CssSchema.CssPropertyInfo, CssPropertyData>> propData
        = Lists.newArrayList();
    List<Expression> stringPool = Lists.newArrayList();
    List<Expression> regexPool = Lists.newArrayList();

    for (CssSchema.CssPropertyInfo prop : props) {
      if (!schema.isPropertyAllowed(prop.name)) { continue; }
      propData.add(Pair.pair(prop, inspector.cssPropertyToPattern(prop.sig)));
    }

    for (String s : commonSubstrings) {
      int n = 0;
      for (Pair<CssSchema.CssPropertyInfo, CssPropertyData> p : propData) {
        String pattern = p.b.regex;
        if (pattern == null) { continue; }
        for (int index = -1; (index = pattern.indexOf(s, index + s.length())) >= 0;) {
          ++n;
        }
      }
      if (n > 1) {
        int poolIndx = stringPool.size();
        stringPool.add(StringLiteral.valueOf(unk, s));
        commonSubstringMap.put(s, poolIndx);
      }
    }

    for (Pair<CssSchema.CssPropertyInfo, CssPropertyData> p : propData) {
      String pattern = p.b.regex;
      if (pattern == null) { continue; }
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
      poolDecls = joinDeclarations(
          poolDecls,
          new Declaration(unk, new Identifier(unk, "s"),
              new ArrayConstructor(unk, stringPool)));
    }
    if (!regexPool.isEmpty()) {
      poolDecls = joinDeclarations(
          poolDecls,
          new Declaration(unk, new Identifier(unk, "c"),
              new ArrayConstructor(unk, regexPool)));
    }

    // Given keyword sets like
    // [['red','blue','green','transparent','inherit',;none'],
    //  ['red','blue','green'],
    //  ['inherit','none','bold','bolder']]
    // recognize that ['red','blue','green'] probably occurs frequently and
    // create a partition like
    // [['red','blue','green'],['bold','bolder'],['inherit',none'],
    //  ['transparent']]
    // and then store indices into the array of partition elements with
    // CSS property names so they can be unioned as needed.
    List<Set<String>> literalSets = Lists.newArrayList();
    for (Pair<CssSchema.CssPropertyInfo, CssPropertyData> p : propData) {
      literalSets.add(p.b.literals);
    }
    Partitions.Partition<String> litPartition = Partitions.partition(
        literalSets, String.class, null);
    List<ArrayConstructor> literalSetArrs = Lists.newArrayList();
    for (int[] literalIndices : litPartition.partition) {
      List<StringLiteral> literalArr = Lists.newArrayList();
      for (int litIndex : literalIndices) {
        literalArr.add(StringLiteral.valueOf(
            unk, litPartition.universe[litIndex]));
      }
      literalSetArrs.add(new ArrayConstructor(unk, literalArr));
    }
    if (!literalSetArrs.isEmpty()) {
      poolDecls = joinDeclarations(
          poolDecls,
          new Declaration(unk, new Identifier(unk, "L"),
              new ArrayConstructor(unk, literalSetArrs)));
    }

    List<ValueProperty> cssSchemaProps = Lists.newArrayList();
    StringLiteral regexObjKey = new StringLiteral(unk, "cssExtra");
    StringLiteral alternatesObjKey = new StringLiteral(unk, "cssAlternates");
    StringLiteral propbitsObjKey = new StringLiteral(unk, "cssPropBits");
    StringLiteral litgroupObjKey = new StringLiteral(unk, "cssLitGroup");

    for (int propIndex = 0, n = propData.size(); propIndex < n; ++propIndex) {
      Pair<CssSchema.CssPropertyInfo, CssPropertyData> d
          = propData.get(propIndex);
      CssSchema.CssPropertyInfo prop = d.a;
      CssPropertyData data = d.b;

      ObjectConstructor dataObj = new ObjectConstructor(unk);

      String regex = data.regex;
      if (regex != null) {
        int poolIndex = regexPoolMap.get(regex)[0];
        Expression re = poolIndex < 0
            ? makeRegexp(commonSubstringMap, regex)
            : (Expression) QuasiBuilder.substV(
                "c[@i]", "i", new IntegerLiteral(unk, poolIndex));
        dataObj.appendChild(new ValueProperty(regexObjKey, re));
      }

      String dom2property = propertyNameToDom2Property(prop.name);
      ArrayConstructor altNames = null;
      for (String altDom2Property : prop.dom2properties) {
        if (altDom2Property.equals(dom2property)) { continue; }
        if (altNames == null) {
          altNames = new ArrayConstructor(
              unk, Collections.<Expression>emptyList());
        }
        altNames.appendChild(StringLiteral.valueOf(unk, altDom2Property));
      }
      if (altNames != null) {
        dataObj.appendChild(new ValueProperty(alternatesObjKey, altNames));
      }

      cssSchemaProps.add(new ValueProperty(
          unk, StringLiteral.valueOf(unk, prop.name.getCanonicalForm()),
          dataObj));

      int propBits = 0;
      for (CssPropBit b : data.properties) {
        propBits |= b.jsValue;
      }
      if (HISTORY_INSENSITIVE_STYLE_WHITELIST.contains(prop.name)) {
        propBits |= CssPropBit.HISTORY_INSENSITIVE.jsValue;
      }
      dataObj.appendChild(
          new ValueProperty(propbitsObjKey, new IntegerLiteral(unk, propBits)));

      List<Expression> litGroups = Lists.newArrayList();
      for (int groupIndex : litPartition.unions[propIndex]) {
        litGroups.add((Expression) QuasiBuilder.substV(
            "L[@i]", "i", new IntegerLiteral(unk, groupIndex)));
      }
      if (!litGroups.isEmpty()) {
        dataObj.appendChild(new ValueProperty(
            litgroupObjKey, new ArrayConstructor(unk, litGroups)));
      }
    }

    ObjectConstructor cssSchema = new ObjectConstructor(unk, cssSchemaProps);

    ParseTreeNode js = QuasiBuilder.substV(
        ""
        + "var cssSchema = (function () {"
        + "  @poolDecls?;"
        + "  return @cssSchema;"
        + "})();",
        "poolDecls", poolDecls,
        "cssSchema", cssSchema);
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

  private static Statement joinDeclarations(
      @Nullable Statement decl, Declaration d) {
    if (decl == null) { return d; }
    if (decl instanceof Declaration) {
      decl = new MultiDeclaration(
          FilePosition.UNKNOWN, Arrays.asList((Declaration) decl));
    }
    ((MultiDeclaration) decl).appendChild(d);
    return decl;
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
    public boolean build(List<File> inputs, List<File> deps,
                         Map<String, Object> options, File output)
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
          new FileOutputStream(output), Charsets.UTF_8.name());
      try {
        String currentDate = "" + new Date();
        if (currentDate.indexOf("*/") >= 0) {
          throw new SomethingWidgyHappenedError("Date should not contain '*/'");
        }
        out.write("/* Copyright Google Inc.\n");
        out.write(" * Licensed under the Apache Licence Version 2.0\n");
        out.write(" * Autogenerated at " + currentDate + "\n");
        out.write(" * \\@provides cssSchema");
        for (CssPropBit b : CssPropBit.values()) {
          out.write(", CSS_PROP_BIT_");
          out.write(Strings.toUpperCase(b.name()));
        }
        out.write(" */\n");
        for (CssPropBit b : CssPropBit.values()) {
          out.write("/**\n * @const\n * @type {number}\n */\n");
          out.write("var CSS_PROP_BIT_");
          out.write(Strings.toUpperCase(b.name()));
          out.write(" = ");
          out.write(String.valueOf(b.jsValue));
          out.write(";\n");
        }
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
