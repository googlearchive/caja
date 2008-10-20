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

import com.google.caja.config.ConfigUtil;
import com.google.caja.lang.css.CssSchema;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssPropertySignature;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.RegexpLiteral;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.tools.BuildCommand;
import com.google.caja.util.Name;
import com.google.caja.util.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    Pattern p = sigToPattern(sig);
    if (p == null) { return null; }
    p = new Concatenation(
        Arrays.asList(new Snippet("^\\s*"), p, new Snippet("$"))).optimize();
    StringBuilder out = new StringBuilder();
    out.append('/');
    p.render(out);
    // Since all keywords are case insensitive and we never match text inside
    // strings.
    out.append("/i");
    return out.toString();
  }

  private Pattern sigToPattern(CssPropertySignature sig) {
    // Dispatch to a set of handlers that either append balanced content to
    // out, or append cruft and return null.
    if (sig instanceof CssPropertySignature.LiteralSignature) {
      return litToPattern((CssPropertySignature.LiteralSignature) sig);
    } else if (sig instanceof CssPropertySignature.RepeatedSignature) {
      return repToPattern((CssPropertySignature.RepeatedSignature) sig);
    } else if (sig instanceof CssPropertySignature.PropertyRefSignature) {
      return refToPattern((CssPropertySignature.PropertyRefSignature) sig);
    } else if (sig instanceof CssPropertySignature.SeriesSignature) {
      return seriesToPattern((CssPropertySignature.SeriesSignature) sig);
    } else if (sig instanceof CssPropertySignature.SymbolSignature) {
      return symbolToPattern((CssPropertySignature.SymbolSignature) sig);
    } else if (sig instanceof CssPropertySignature.SetSignature
               || sig instanceof CssPropertySignature.ExclusiveSetSignature) {
      return setToPattern(sig);
    }
    return null;
  }

  private static Pattern litToPattern(
      CssPropertySignature.LiteralSignature lit) {
    StringBuilder regex = new StringBuilder();
    Escaping.escapeRegex(lit.getValue(), false, false, regex);
    // Match some trailing whitespace.
    // Since some patterns can match nothing (e.g. foo*), we make sure that
    // all positive matches are followed by token-breaking space.
    // The pattern as a whole can then be matched against the value with one
    // space added at the end.
    regex.append("\\s+");
    return new Snippet(regex.toString());
  }

  private Pattern repToPattern(CssPropertySignature.RepeatedSignature sig) {
    CssPropertySignature rep = sig.getRepeatedSignature();
    if (rep instanceof CssPropertySignature.ExclusiveSetSignature) {
      // Can't easily handle the special semantics of [ a || b ]* allowing
      // "a b", "b a", "a", "b", but not "a a", "b b".
      return null;
    }
    Pattern repeatedPattern = sigToPattern(rep);
    if (repeatedPattern == null) { return null; }
    return new Repetition(repeatedPattern, sig.minCount, sig.maxCount);
  }

  private Pattern refToPattern(CssPropertySignature.PropertyRefSignature sig) {
    CssSchema.CssPropertyInfo p = schema.getCssProperty(sig.getPropertyName());
    return p != null ? sigToPattern(p.sig) : null;
  }

  private Pattern seriesToPattern(CssPropertySignature.SeriesSignature sig) {
    List<Pattern> children = new ArrayList<Pattern>();
    for (CssPropertySignature child : sig.children()) {
      Pattern childP = sigToPattern(child);
      if (childP == null) { return null; }
      children.add(childP);
    }
    return new Concatenation(children);
  }

  private Pattern symbolToPattern(CssPropertySignature.SymbolSignature sig) {
    Name symbolName = sig.getValue();
    Pattern builtinMatch = builtinToPattern(symbolName);
    if (builtinMatch != null) { return builtinMatch; }
    CssSchema.SymbolInfo s = schema.getSymbol(symbolName);
    return s != null ? sigToPattern(s.sig) : null;
  }

  private Pattern setToPattern(CssPropertySignature sig) {
    if (sig.children().isEmpty()) { return null; }
    List<Pattern> children = new ArrayList<Pattern>();
    for (CssPropertySignature child : sig.children()) {
      Pattern childP = sigToPattern(child);
      if (childP != null) { children.add(childP); }
    }
    if (children.isEmpty()) { return null; }
    return new Union(children);
  }

  private static final Map<String, String> BUILTINS
      = new HashMap<String, String>();
  static {
    // http://www.w3.org/TR/REC-CSS2/syndata.html
    String unsignedNum = "(?:\\d+(?:\\.\\d+)?)";
    String signedNum = "[+-]?\\d+(?:\\.\\d+)?";
    String angleUnits = "(?:deg|g?rad)";
    String freqUnits = "k?Hz";
    String lengthUnits = "(?:em|ex|px|in|cm|mm|pt|pc)";
    String timeUnits = "m?s";
    String quotedIdentifiers = "\"\\w(?:[\\w-]*\\w)(?:\\s+\\w([\\w-]*\\w))*\"";
    BUILTINS.put("number:0,", "0|" + unsignedNum);
    BUILTINS.put("number", "0|" + signedNum);
    BUILTINS.put("percentage", "0|" + unsignedNum + "%");
    BUILTINS.put("percentage:0,", "0|" + signedNum + "%");
    BUILTINS.put("angle:0,", "0|" + unsignedNum + angleUnits);
    BUILTINS.put("angle", "0|" + signedNum + angleUnits);
    BUILTINS.put("frequency", "0|" + unsignedNum + freqUnits);
    BUILTINS.put("length:0,", "0|" + unsignedNum + lengthUnits);
    BUILTINS.put("length", "0|" + signedNum + lengthUnits);
    BUILTINS.put("time:0,", "0|" + unsignedNum + timeUnits);
    BUILTINS.put("time", "0|" + signedNum + timeUnits);
    BUILTINS.put("integer", "-?\\d+");
    BUILTINS.put("integer:0,", "\\d+");
    BUILTINS.put("hex-color", "#(?:[0-9a-f]{3}){1,2}");
    BUILTINS.put("specific-voice", quotedIdentifiers);
    BUILTINS.put("family-name", quotedIdentifiers);
    BUILTINS.put("uri", "url\\(\"[^\\(\\)\\\\\\\"\\r\\n]+\"\\)");
  }
  private Pattern builtinToPattern(Name name) {
    String pattern = BUILTINS.get(name.getCanonicalForm());
    return pattern != null ? new Snippet(pattern + "\\s+") : null;
  }

  private static interface Pattern {
    /** Returns a simpler but equivalent Pattern. */
    Pattern optimize();
    /** Appends javascript regular expression to the given buffer. */
    void render(StringBuilder out);
    /** A Pattern suffix that matches all strings matched by this pattern. */
    String tail();
    /**
     * A pattern that matches the same content as this pattern but without
     * matching the last n characters.
     */
    Pattern subtractTail(int n);
  }

  private static class Snippet implements Pattern {
    final String patternSnippet;
    Snippet(String patternSnippet) {
      this.patternSnippet = patternSnippet;
    }
    public Pattern optimize() {
      return this;
    }
    public void render(StringBuilder out) { out.append(patternSnippet); }
    public String tail() { return patternSnippet; }
    public Pattern subtractTail(int n) {
      return new Snippet(
          patternSnippet.substring(0, patternSnippet.length() - n));
    }
  }

  private static class Concatenation implements Pattern {
    final List<Pattern> children;
    Concatenation(List<Pattern> children) { this.children = children; }
    public Pattern optimize() {
      List<Pattern> newChildren = new ArrayList<Pattern>();
      for (Pattern child : children) {
        child = child.optimize();
        if (child instanceof Concatenation) {
          Concatenation childCat = (Concatenation) child;
          newChildren.addAll(childCat.children);
        } else if (!(child instanceof Snippet
                     && "".equals(((Snippet) child).patternSnippet))) {
          newChildren.add(child);
        }
      }
      if (newChildren.size() == 1) { return newChildren.get(0); }
      return new Concatenation(newChildren);
    }
    public void render(StringBuilder out) {
      for (Pattern n : children) { n.render(out); }
    }
    public String tail() {
      return children.get(children.size() - 1).tail();
    }
    public Pattern subtractTail(int n) {
      List<Pattern> newChildren = new ArrayList<Pattern>();
      int last = children.size() - 1;
      newChildren.addAll(children.subList(0, last));
      newChildren.add(children.get(last).subtractTail(n));
      return new Concatenation(newChildren);
    }
  }

  private static class Union implements Pattern {
    final List<Pattern> children;
    Union(List<Pattern> children) { this.children = children; }
    public Pattern optimize() {
      List<Pattern> newChildren = new ArrayList<Pattern>();
      for (Pattern child : children) {
        child = child.optimize();
        if (child instanceof Union) {
          Union childUnion = (Union) child;
          newChildren.addAll(childUnion.children);
        } else {
          newChildren.add(child);
        }
      }
      int n = newChildren.size();
      if (n == 1) { return newChildren.get(0); }
      if (n != 0) {  // a\\s+|b\\s+|c\\s+  -> (a|b|c)\\s+
        // Tail optimize since most patterns will end with \\s+.
        Pattern child0 = newChildren.get(0);
        String tail = child0.tail();
        for (Pattern child : newChildren.subList(1, n)) {
          if ("".equals(tail)) { break; }
          tail = commonSuffix(tail, child.tail());
        }
        if (!"".equals(tail)) {
          for (int i = 0; i < n; ++i) {
            newChildren.set(i, newChildren.get(i).subtractTail(tail.length()));
          }
          Pattern opt = new Concatenation(
              Arrays.asList(new Union(newChildren), new Snippet(tail)))
              .optimize();
          return opt;
        }
      }
      return new Union(newChildren);
    }
    public void render(StringBuilder out) {
      out.append("(?:");
      for (int i = 0, n = children.size(); i < n; ++i) {
        if (i != 0) { out.append('|'); }
        children.get(i).render(out);
      }
      out.append(')');
    }
    public String tail() { return ""; }
    public Pattern subtractTail(int n) {
      throw new UnsupportedOperationException();
    }
  }

  private static class Repetition implements Pattern {
    final Pattern repeated;
    final int min, max;
    Repetition(Pattern repeated, int min, int max) {
      this.repeated = repeated;
      this.min = min;
      this.max = max;
    }
    public Pattern optimize() {
      return new Repetition(repeated.optimize(), min, max);
    }
    public void render(StringBuilder out) {
      out.append("(?:");
      repeated.render(out);
      out.append(')');
      if (max == Integer.MAX_VALUE) {
        switch (min) {
          case 0: out.append('*'); return;
          case 1: out.append('+'); return;
        }
      } else if (max == 1 && min == 0) {
        out.append('?');
        return;
      }
      out.append('{').append(min).append(',').append(max).append('}');
    }
    public String tail() { return ""; }
    public Pattern subtractTail(int n) {
      throw new UnsupportedOperationException();
    }
  }

  public static String commonSuffix(String a, String b) {
    int m = a.length(), n = b.length();
    int k = Math.min(m, n);
    int i = 0;
    while (i < k && a.charAt(m - i - 1) == b.charAt(n - i - 1)) { ++i; }
    return a.substring(m - i, m);
  }

  public static void generatePatterns(CssSchema schema, Appendable out)
      throws IOException {
    CssPropertyPatterns pp = new CssPropertyPatterns(schema);
    List<CssSchema.CssPropertyInfo> props
        = new ArrayList<CssSchema.CssPropertyInfo>(schema.getCssProperties());
    Collections.sort(
        props, new Comparator<CssSchema.CssPropertyInfo>() {
          public int compare(CssSchema.CssPropertyInfo a,
                             CssSchema.CssPropertyInfo b) {
            return a.dom2property.compareTo(b.dom2property);
          }
        });
    Map<String, int[]> constantPoolMap = new HashMap<String, int[]>();
    List<Pair<CssSchema.CssPropertyInfo, String>> patterns
        = new ArrayList<Pair<CssSchema.CssPropertyInfo, String>>();
    List<RegexpLiteral> constantPool = new ArrayList<RegexpLiteral>();

    for (CssSchema.CssPropertyInfo prop : props) {
      String pattern = pp.cssPropertyToPattern(prop.sig);
      if (!schema.isPropertyAllowed(prop.name)) { continue; }
      if (pattern != null && !"(?:inherit\\s+)".equals(pattern)) {
        patterns.add(Pair.pair(prop, pattern));
        // Keep track of which patterns appear more than once so we can use
        // a constant pool.
        int[] pool = constantPoolMap.get(pattern);
        if (pool == null) {
          constantPoolMap.put(pattern, new int[] { -1 });
        } else if (pool[0] == -1) {
          pool[0] = constantPool.size();
          constantPool.add(new RegexpLiteral(pattern));
        }
      }
    }

    Declaration constantPoolDecl = null;
    if (!constantPool.isEmpty()) {
      constantPoolDecl = (Declaration) QuasiBuilder.substV(
          "var c = @constantPool;",
          "constantPool", new ArrayConstructor(constantPool));
    }
    List<Pair<Literal, Expression>> members
        = new ArrayList<Pair<Literal, Expression>>();
    for (Pair<CssSchema.CssPropertyInfo, String> p : patterns) {
      Literal name = StringLiteral.valueOf(p.a.dom2property);
      int poolIndex = constantPoolMap.get(p.b)[0];
      Expression re = poolIndex < 0
          ? new RegexpLiteral(p.b)
          : (Expression) QuasiBuilder.substV(
              "c[@i]", "i", new IntegerLiteral(poolIndex));
      members.add(Pair.pair(name, re));
    }
    ObjectConstructor cssPropConstructor = new ObjectConstructor(members);

    ParseTreeNode js = QuasiBuilder.substV(
        "var css = { properties: (function () {"
        + "  @constantPoolDecl?;"
        + "  return @cssPropConstructor;"
        + "})() };",
        "constantPoolDecl", constantPoolDecl,
        "cssPropConstructor", cssPropConstructor);
    TokenConsumer tc = js.makeRenderer(out, null);
    js.render(new RenderContext(new MessageContext(), tc));
    tc.consume(";");
    tc.noMoreTokens();
    out.append("\n");
  }

  public static class Builder implements BuildCommand {
    public void build(List<File> inputs, List<File> deps, File output)
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
      mc.inputSources = Arrays.asList(sps.source(), fns.source());
      MessageQueue mq = new EchoingMessageQueue(
          new PrintWriter(new OutputStreamWriter(System.err), true), mc, false);

      CssSchema schema;
      try {
        Reader spsIn = new InputStreamReader(
            new FileInputStream(symbolsAndPropertiesFile), "UTF-8");
        try {
          Reader fnsIn = new InputStreamReader(
              new FileInputStream(functionsFile), "UTF-8");
          try {
            schema = new CssSchema(
                ConfigUtil.loadWhiteListFromJson(spsIn, sps, mq),
                ConfigUtil.loadWhiteListFromJson(fnsIn, fns, mq));
          } finally {
            fnsIn.close();
          }
        } finally {
          spsIn.close();
        }
      } catch (ParseException ex) {
        ex.toMessageQueue(mq);
        throw (IOException) new IOException("Failed to parse schema")
            .initCause(ex);
      }

      Writer out = new OutputStreamWriter(
          new FileOutputStream(output), "UTF-8");
      String currentDate = "" + new Date();
      if (currentDate.indexOf("*/") >= 0) { throw new RuntimeException(); }
      out.write("/* Copyright Google Inc.\n");
      out.write(" * Licensed under the Apache Licence Version 2.0\n");
      out.write(" * Autogenerated at " + currentDate + "\n");
      out.write(" */\n");
      try {
        generatePatterns(schema, out);
      } finally {
        out.close();
      }
    }
  }

  public static void main(String[] args) throws IOException {
    CssSchema schema = CssSchema.getDefaultCss21Schema(
        new SimpleMessageQueue());
    generatePatterns(schema, System.out);
  }
}
