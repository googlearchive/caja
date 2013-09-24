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
import com.google.caja.parser.js.MultiDeclaration;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ValueProperty;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.plugin.LinkStyleWhitelist;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.tools.BuildCommand;
import com.google.caja.util.Bag;
import com.google.caja.util.Charsets;
import com.google.caja.util.Lists;
import com.google.caja.util.Name;
import com.google.caja.util.Pair;
import com.google.caja.util.Sets;
import com.google.caja.util.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Operates on CSS property signatures to come up with a schema that can be
 * used to validate properties.
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
 *       // Groups of literal values allowed including keywords and specific
 *       // numeric values like font-weight:300
 *       cssLitGroup: [CSS_LIT_GROUP[1],CSS_LIT_GROUP[3],CSS_LIT_GROUP[16]],
 *       // Schema keys for functions that are allowed (non-transitively).
 *       cssFns: []
 *     },
 *     ...
 *     // Functions are top-level constructs that have their own filters which
 *     // can be applied to their actuals.
 *     "rgba()": { ... },
 *     ...
 *   };
 * </pre>
 *
 * <h3>Program Flow</h3>
 * <p>
 * This class examines a schema and builds a list of all allowed CSS properties, * and each function.
 * It then tries to deduce for each property value and function the set of
 * keywords/literal token values, how to interpret quoted strings, and what to
 * do with loose identifiers that do not match a known keyword.
 * <p>
 * Once it has collections of keywords/literal-tokens, it tries to group
 * commonly co-occuring literal-tokens together to reduce download size.
 * Finally, it identifies patterns like {@code border-top} and
 * {@code border-bottom} which have identical results.
 * <p>
 * Finally it builds a javascript parse tree that assigns the {@code css}
 * namespace to an object whose keys are CSS property names, and whose
 * values are data maps similar to the example code above.
 *
 * <p>
 * "sanitize-css.js" uses this map extensively to sanitize &amp; normalize CSS
 * properties, rewriting URIs as needed.
 *
 * @author mikesamuel@gmail.com
 */
public class CssPropertyPatterns {
  private final CssSchema schema;

  public CssPropertyPatterns(CssSchema schema) {
    this.schema = schema;
  }

  static final class CssPropertyData {
    final String key;
    final CssPropertySignature sig;
    final EnumSet<CssPropBit> properties;
    final Set<String> literals;
    final Set<CssPropertySignature.CallSignature> fns;

    CssPropertyData(String key, CssPropertySignature sig) {
      assert key.equals(Strings.lower(key)) : key;
      this.key = key;
      this.sig = sig;
      this.properties = EnumSet.noneOf(CssPropBit.class);
      this.literals = Sets.newHashSet();
      this.fns = Sets.newTreeSet(SignatureComparator.SINGLETON);
    }
  }

  /**
   * Generates a data map for the given signature.
   */
  public CssPropertyData cssPropertyToData(
      String key, CssPropertySignature sig) {
    CssPropertyData data = new CssPropertyData(key, sig);
    new Inspector(data).inspect();
    return data;
  }

  private static final Set<String> KNOWN_VENDOR_PREFIXES = Sets.immutableSet(
      "apple",
      "css",
      "epub",
      "khtml",
      "moz",
      "ms",
      "mso",
      "o",
      "rim",
      "wap",
      "webkit",
      "xv"
  );

  public static String withoutVendorPrefix(String cssIdentifier) {
    if (cssIdentifier.startsWith("-")) {
      int dash = cssIdentifier.indexOf('-', 1);
      if (dash >= 0) {
        String possiblePrefix = cssIdentifier.substring(1, dash);
        if (KNOWN_VENDOR_PREFIXES.contains(possiblePrefix)) {
          return cssIdentifier.substring(dash + 1);
        }
      }
    }
    return cssIdentifier;
  }

  public static boolean hasVendorPrefix(String cssIdentifier) {
    return !cssIdentifier.equals(withoutVendorPrefix(cssIdentifier));
  }

  /**
   * Walks a property signature to figure out what tokens can comprise its
   * value and how non-symbolic tokens like quoted strings, and non-keyword
   * identifiers are used.
   */
  private class Inspector {
    /** Modified in-place as the inspector encounters symbols. */
    final CssPropertyData data;
    /** Avoid infinitely recursing on symbol cycles. */
    private final Bag<String> refsUsed;

    private Inspector(CssPropertyData data) {
      this.data = data;
      this.refsUsed = Bag.newHashBag();
    }

    void inspect() {
      this.data.literals.clear();
      this.data.properties.clear();
      this.data.fns.clear();
      inspectSig(data.sig);
    }

    private void inspectSig(CssPropertySignature sig) {
      // Dispatch to a set of handlers that either append balanced content to
      // out, or append cruft and return null.
      if (sig instanceof CssPropertySignature.LiteralSignature) {
        inspectLit((CssPropertySignature.LiteralSignature) sig);
      } else if (sig instanceof CssPropertySignature.RepeatedSignature) {
        inspectRep((CssPropertySignature.RepeatedSignature) sig);
      } else if (sig instanceof CssPropertySignature.PropertyRefSignature) {
        inspectRef((CssPropertySignature.PropertyRefSignature) sig);
      } else if (sig instanceof CssPropertySignature.SeriesSignature) {
        inspectSeries((CssPropertySignature.SeriesSignature) sig);
      } else if (sig instanceof CssPropertySignature.SymbolSignature) {
        inspectSymbol((CssPropertySignature.SymbolSignature) sig);
      } else if (sig instanceof CssPropertySignature.SetSignature
                 || sig instanceof CssPropertySignature.ExclusiveSetSignature) {
        inspectSet(sig);
      } else if (sig instanceof CssPropertySignature.CallSignature) {
        inspectCall((CssPropertySignature.CallSignature) sig);
      } else if (sig instanceof CssPropertySignature.ProgIdSignature) {
        // Ignore.  progid is of interest for old versions of IE and should
        // probably be obsoleted.
      } else {
        throw new SomethingWidgyHappenedError(
            sig + " : " + sig.getClass().getSimpleName());
      }
    }

    private void inspectLit(CssPropertySignature.LiteralSignature lit) {
      String litValue = lit.getValue();
      // Match some trailing whitespace.
      // Since some patterns can match nothing (e.g. foo*), we make sure that
      // all positive matches are followed by token-breaking space.
      // The pattern as a whole can then be matched against the value with one
      // space added at the end.
      data.literals.add(withoutVendorPrefix(litValue));
    }

    private void inspectRep(CssPropertySignature.RepeatedSignature sig) {
      CssPropertySignature rep = sig.getRepeatedSignature();
      inspectSig(rep);
    }

    private void inspectRef(CssPropertySignature.PropertyRefSignature sig) {
      Name propertyName = sig.getPropertyName();
      if (refsUsed.incr(propertyName.getCanonicalForm()) == 0) {
        CssSchema.CssPropertyInfo p = schema.getCssProperty(propertyName);
        if (p == null) {
          throw new SomethingWidgyHappenedError(
              "Unsatisfied reference " + propertyName);
        }
        inspectSig(p.sig);
      }
    }

    private void inspectSeries(CssPropertySignature.SeriesSignature sig) {
      for (CssPropertySignature child : sig.children()) {
        inspectSig(child);
      }
    }

    private void inspectSymbol(CssPropertySignature.SymbolSignature sig) {
      Name symbolName = sig.getValue();
      CssSchema.SymbolInfo s = schema.getSymbol(symbolName);
      if (s != null) {
        inspectSig(s.sig);
      } else if (!inspectBuiltin(symbolName)) {
        throw new SomethingWidgyHappenedError(
            "unknown CSS symbol " + symbolName);
      }
    }

    private void inspectSet(CssPropertySignature sig) {
      for (CssPropertySignature child : sig.children()) {
        inspectSig(child);
      }
    }

    private void inspectCall(CssPropertySignature.CallSignature sig) {
      data.fns.add(sig);
    }

    private boolean inspectBuiltin(Name name) {
      String key = name.getCanonicalForm();
      int colon = key.lastIndexOf(':');
      boolean negative = key.lastIndexOf('-') > colon;
      String baseKey = colon >= 0 ? key.substring(0, colon) : key;
      CssPropBit b = BUILTIN_PROP_BITS.get(baseKey);
      if (b == null) {
        return false;
      }
      data.properties.add(b);
      // The negative bit allows for some schemas to reject positioning
      // outside the parents' bounding boxes, and negative offsets for clip
      // regions.
      if (b == CssPropBit.QUANTITY && (colon < 0 || negative)) {
        // TODO: maybe tighten this condition
        data.properties.add(CssPropBit.NEGATIVE_QUANTITY);
      }
      return true;
    }
  }

  private static final Map<String, CssPropBit> BUILTIN_PROP_BITS
      = new ImmutableMap.Builder<String, CssPropBit>()
        .put("angle", CssPropBit.QUANTITY)
        .put("frequency", CssPropBit.QUANTITY)
        .put("global-name", CssPropBit.GLOBAL_NAME)
        .put("hex-color", CssPropBit.HASH_VALUE)
        .put("integer", CssPropBit.QUANTITY)
        .put("length", CssPropBit.QUANTITY)
        .put("number", CssPropBit.QUANTITY)
        .put("percentage", CssPropBit.QUANTITY)
        .put("property-name", CssPropBit.PROPERTY_NAME)
        .put("quotable-word", CssPropBit.UNRESERVED_WORD)
        .put("specific-voice", CssPropBit.QSTRING)
        .put("string", CssPropBit.QSTRING)
        .put("time", CssPropBit.QUANTITY)
        .put("unicode-range", CssPropBit.UNICODE_RANGE)
        .put("unreserved-word", CssPropBit.UNRESERVED_WORD)
        .put("uri", CssPropBit.URL)
        .put("z-index", CssPropBit.QUANTITY)
        .build();

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
    List<Pair<CssSchema.CssPropertyInfo, CssPropertyData>> propData
        = Lists.newArrayList();
    List<Expression> stringPool = Lists.newArrayList();
    List<Expression> regexPool = Lists.newArrayList();

    // Inspect each property's signature in the schema.
    Set<String> keys = Sets.newHashSet();
    for (CssSchema.CssPropertyInfo prop : props) {
      if (!schema.isPropertyAllowed(prop.name)) { continue; }
      String key = prop.name.getCanonicalForm();
      if (hasVendorPrefix(key)) { continue; }
      CssPropertyData data = new CssPropertyData(key, prop.sig);
      pp.new Inspector(data).inspect();
      propData.add(Pair.pair(prop, data));
      keys.add(data.key);
    }

    // Now, rewalk the list, and add an entry for each unique function signature
    // seen, and allocate names for the functions.
    Map<CssPropertySignature, CssPropertyData> fnSigToData
        = Maps.newTreeMap(SignatureComparator.SINGLETON);
    for (int i = 0; i < propData.size() /* Walks over fns as added */; ++i) {
      for (CssPropertySignature.CallSignature fn : propData.get(i).b.fns) {
        if (!fnSigToData.containsKey(fn)) {
          String fnName = fn.getName();
          if (fnName == null) { continue; }
          String fnKey = allocateKey(fnName + "()", keys);
          CssPropertyData fnData = new CssPropertyData(
              fnKey, fn.getArgumentsSignature());
          pp.new Inspector(fnData).inspect();
          fnSigToData.put(fn, fnData);
          keys.add(fnKey);
          propData.add(Pair.pair((CssSchema.CssPropertyInfo) null, fnData));
        }
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
    StringLiteral propbitsObjKey = new StringLiteral(unk, "cssPropBits");
    StringLiteral litgroupObjKey = new StringLiteral(unk, "cssLitGroup");
    StringLiteral fnsObjKey = new StringLiteral(unk, "cssFns");

    // Keep track of the JS we generate so we can reuse data-objects for
    // CSS properties whose filtering schemes are functionally equivalent.
    Map<String, String> dataJsToKey = Maps.newHashMap();
    boolean hasAliases = false;

    for (int propIndex = 0, n = propData.size(); propIndex < n; ++propIndex) {
      Pair<CssSchema.CssPropertyInfo, CssPropertyData> d
          = propData.get(propIndex);
      CssSchema.CssPropertyInfo prop = d.a;
      CssPropertyData data = d.b;

      ObjectConstructor dataObj = new ObjectConstructor(unk);

      int propBits = 0;
      for (CssPropBit b : data.properties) {
        propBits |= b.jsValue;
      }

      if (prop != null) {
        if (LinkStyleWhitelist.HISTORY_INSENSITIVE_STYLE_WHITELIST
            .contains(prop.name)) {
          propBits |= CssPropBit.HISTORY_INSENSITIVE.jsValue;
        } else if (LinkStyleWhitelist.PROPERTIES_ALLOWED_IN_LINK_CLASSES
                   .contains(prop.name)) {
          propBits |= CssPropBit.ALLOWED_IN_LINK.jsValue;
        }
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

      List<Expression> fnKeyStrs = Lists.newArrayList();
      for (CssPropertySignature.CallSignature fn : data.fns) {
        String fnKey = fnSigToData.get(fn).key;
        fnKeyStrs.add(StringLiteral.valueOf(unk, fnKey));
      }
      ArrayConstructor fnKeyArray = new ArrayConstructor(unk, fnKeyStrs);
      dataObj.appendChild(new ValueProperty(fnsObjKey, fnKeyArray));

      String dataJs;
      {
        StringBuilder js = new StringBuilder();
        JsMinimalPrinter tokenConsumer = new JsMinimalPrinter(js);
        dataObj.render(new RenderContext(tokenConsumer));
        tokenConsumer.noMoreTokens();
        dataJs = js.toString();
      }

      String equivKey = dataJsToKey.get(dataJs);
      Expression value = dataObj;
      if (equivKey == null) {
        dataJsToKey.put(dataJs, data.key);
      } else {
        value = StringLiteral.valueOf(unk, equivKey);
        hasAliases = true;
      }
      cssSchemaProps.add(new ValueProperty(
          unk, StringLiteral.valueOf(unk, data.key), value));
    }

    ObjectConstructor cssSchema = new ObjectConstructor(unk, cssSchemaProps);

    ParseTreeNode js = QuasiBuilder.substV(
        ""
        + "var cssSchema = (function () {"
        + "  @poolDecls?;"
        + "  var schema = @cssSchema;"
        + "  if (@hasAliases) {"
        + "    for (var key in schema) {"
        + "      if ('string' === typeof schema[key]"
        + "          && Object.hasOwnProperty.call(schema, key)) {"
        + "        schema[key] = schema[schema[key]];"
        + "      }"
        + "    }"
        + "  }"
        + "  return schema;"
        + "})();",
        "poolDecls", poolDecls,
        "cssSchema", cssSchema,
        "hasAliases", new BooleanLiteral(unk, hasAliases));
    TokenConsumer tc = js.makeRenderer(out, null);
    js.render(new RenderContext(tc));
    tc.noMoreTokens();
    out.append(";\n");
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
        out.write(" * \\@overrides window\n");
        out.write(" * \\@provides cssSchema");
        for (CssPropBit b : CssPropBit.values()) {
          out.write(", CSS_PROP_BIT_");
          out.write(Strings.upper(b.name()));
        }
        out.write(" */\n");
        for (CssPropBit b : CssPropBit.values()) {
          out.write("/**\n * @const\n * @type {number}\n */\n");
          out.write("var CSS_PROP_BIT_");
          out.write(Strings.upper(b.name()));
          out.write(" = ");
          out.write(String.valueOf(b.jsValue));
          out.write(";\n");
        }
        generatePatterns(schema, out);
        out.write("if (typeof window !== 'undefined') {\n");
        out.write("  window['cssSchema'] = cssSchema;\n");
        out.write("}\n");
      } finally {
        out.close();
      }
      return true;
    }
  }

  /**
   * Adds a key that is not in allocated to it and returns the result.
   * The result will have base as a prefix.
   */
  private static final String allocateKey(String base, Set<String> allocated) {
    base = Strings.lower(base);
    int counter = 0;
    String candidate = base;
    while (!allocated.add(candidate)) {
      candidate = base + "#" + counter;
      ++counter;
    }
    return candidate;
  }

  public static void main(String[] args) throws IOException {
    CssSchema schema = CssSchema.getDefaultCss21Schema(
        new SimpleMessageQueue());
    generatePatterns(schema, System.out);
  }

  /**
   * Compares two CSS signatures by type (concrete class), value, and
   * recursively by child list.
   * The ordering is suitable for use in an Ordered{Set,Map} but has no greater
   * significance.
   */
  private static final class SignatureComparator
      implements Comparator<CssPropertySignature> {
    private SignatureComparator() {}
    static final SignatureComparator SINGLETON = new SignatureComparator();

    @SuppressWarnings("unchecked")
    public int compare(CssPropertySignature a, CssPropertySignature b) {
      if (a == b) {
        return 0;
      }
      Class<?> aClass = a.getClass();
      Class<?> bClass = b.getClass();
      if (aClass != bClass) {
        return aClass.getName().compareTo(bClass.getName());
      }

      Object aValue = a.getValue();
      Object bValue = b.getValue();
      if (aValue != bValue) {
        if (aValue == null) {
          return -1;
        }
        if (bValue == null) {
          return 1;
        }
        // Works for the Number and String types typically used as ParseTreeNode
        // values, but is not strictly type safe.
        @SuppressWarnings("rawtypes")
        Comparable aValueCmp = (Comparable) aValue;
        @SuppressWarnings("rawtypes")
        Comparable bValueCmp = (Comparable) bValue;
        return aValueCmp.compareTo(bValueCmp);
      }

      List<? extends CssPropertySignature> aChildren = a.children();
      List<? extends CssPropertySignature> bChildren = b.children();
      int size = aChildren.size();
      int sizeDelta = size - bChildren.size();
      if (sizeDelta != 0) {
        return sizeDelta;
      }
      for (int i = 0; i < size; ++i) {
        int childDelta = compare(aChildren.get(i), bChildren.get(i));
        if (childDelta != 0) {
          return childDelta;
        }
      }
      return 0;
    }
  }
}
