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

import com.google.caja.config.ConfigUtil;
import com.google.caja.config.WhiteList;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssPropertySignature;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pair;
import com.google.caja.util.Strings;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Definitions of CSS properties and symbols.
 *
 * @author mikesamuel@gmail.com
 */
public final class CssSchema {
  private final Map<String, CssPropertyInfo> properties =
    new HashMap<String, CssPropertyInfo>();
  private final Map<String, SymbolInfo> symbols =
    new HashMap<String, SymbolInfo>();
  private final Set<String> keywords = new HashSet<String>();
  private final Set<String> functionsAllowed;
  private final Set<String> propertiesAllowed;

  private static Pair<CssSchema, List<Message>> defaultSchema;
  public static CssSchema getDefaultCss21Schema(MessageQueue mq) {
    if (defaultSchema == null) {
      SimpleMessageQueue cacheMq = new SimpleMessageQueue();
      FilePosition fnPos = FilePosition.startOfFile(new InputSource(URI.create(
          "resource:///com/google/caja/lang/css/css21-fns.json"))),
          propPos = FilePosition.startOfFile(new InputSource(URI.create(
          "resource:///com/google/caja/lang/css/css21.json")));
      WhiteList propDefs, fnDefs;
      try {
        propDefs = ConfigUtil.loadWhiteListFromJson(
            ConfigUtil.openConfigResource(propPos.source().getUri(), null),
            propPos, cacheMq);
        fnDefs = ConfigUtil.loadWhiteListFromJson(
            ConfigUtil.openConfigResource(fnPos.source().getUri(), null),
            fnPos, cacheMq);
      // If the default schema is borked, there's not much we can do.
      } catch (IOException ex) {
        mq.getMessages().addAll(cacheMq.getMessages());
        throw new RuntimeException(ex);
      } catch (ParseException ex) {
        ex.toMessageQueue(cacheMq);
        mq.getMessages().addAll(cacheMq.getMessages());
        throw new RuntimeException(ex);
      }
      defaultSchema = Pair.pair(
          new CssSchema(propDefs, fnDefs), cacheMq.getMessages());
    }
    mq.getMessages().addAll(defaultSchema.b);
    return defaultSchema.a;
  }

  /**
   * Returns the signature and other information for a css property.
   * @param propertyName non null.
   * @return null if no such property.
   */
  public CssPropertyInfo getCssProperty(String propertyName) {
    // http://www.w3.org/TR/CSS21/syndata.html#characters
    // All CSS style sheets are case-insensitive, except for parts
    // that are not under the control of CSS.
    return properties.get(Strings.toLowerCase(propertyName));
  }

  /** All defined properties including disallowed ones. */
  public Collection<CssPropertyInfo> getCssProperties() {
    return Collections.unmodifiableCollection(properties.values());
  }

  /**
   * Returns the signature for a css symbol.
   * Not all symbols can be defined in terms of a signature.
   * @param symbolName non null.
   * @return null if no such symbol or the symbol is not defined in terms of a
   *   signature.
   */
  public SymbolInfo getSymbol(String symbolName) {
    return symbols.get(symbolName);
  }

  /** Is the given word a css keyword? */
  public boolean isKeyword(String name) {
    return keywords.contains(Strings.toLowerCase(name));
  }

  /** Is the given word the name of a CSS function? */
  public boolean isFunctionAllowed(String name) {
    return functionsAllowed.contains(Strings.toLowerCase(name));
  }

  /** Is the given word the name of an allowed CSS property? */
  public boolean isPropertyAllowed(String name) {
    return propertiesAllowed.contains(Strings.toLowerCase(name));
  }

  public static boolean isMediaType(String mediaType) {
    return ALL_MEDIA.accept(mediaType);
  }

  /**
   * Encapsulates a css property and its signatures.
   */
  public static final class CssPropertyInfo extends SymbolInfo {
    /**
     * The set of media types for which this property is applicable.
     * "pitch" is only applicable if the page is being spoken by a
     * screen reader.
     */
    public final Criterion<String> mediaGroups;
    public final boolean inherited;
    /** Set of elements the property applies to. */
    public final Criterion<String> appliesTo;
    /** May be null. */
    public final String defaultValue;
    /**
     * The name of the property in {@code htmlElement.style}.
     * See <a href="http://developer.mozilla.org/en/docs/DOM:CSS">mozilla's
     *     list</a>.
     */
    public final String dom2property;

    private CssPropertyInfo(
        String name, CssPropertySignature sig, Criterion<String> mediaGroups,
        boolean inherited, Criterion<String> appliesTo, String defaultValue,
        String dom2property) {
      super(name, sig);
      this.mediaGroups = mediaGroups;
      this.inherited = inherited;
      // Not defensively copied.  This is usually an immutable AllSet.
      this.appliesTo = appliesTo;
      this.defaultValue = defaultValue;
      this.dom2property = dom2property;
    }
  }

  /**
   * Encapsulates a css symbol.  A symbol is a non top-level entity in a
   * declaration.  Symbols include things like &lt;color&gt;, &lt;uri&gt;,
   * &lt;integer&gt;, etc.
   */
  public static class SymbolInfo {
    public final String name;
    public final CssPropertySignature sig;

    private SymbolInfo(String name, CssPropertySignature sig) {
      this.name = name;
      this.sig = sig;
    }
  }

  // TODO(mikesamuel): Is there any value in enumerating elements?
  // Perhaps by using HTMLSchema?
  private static final Pattern HTML_IDENTIFIER = Pattern.compile("^[\\w\\-]+$");
  private static final Pattern CSS_IDENTIFIER =
    Pattern.compile("^[a-zA-Z\\-][\\w\\-]*$");
  private static final Pattern JS_IDENTIFIER =
    Pattern.compile("^[a-zA-Z_][\\w_]*$");
  private static final Criterion<String> ALL_ELEMENTS
      = new RegexpCriterion(HTML_IDENTIFIER);
  // See http://www.w3.org/TR/REC-CSS2/media.html section 7.3
  private static final Criterion<String> ALL_MEDIA = in(
      "all", "aural", "braille", "embossed", "handheld", "print", "projection",
      "screen", "tty", "tv");

  private static Criterion<String> in(String... elementGroups) {
    final Set<String> elements = new HashSet<String>();
    elements.addAll(Arrays.asList(elementGroups));
    return new Criterion<String>() {
      public boolean accept(String s) {
        return elements.contains(s);
      }
    };
  }

  private static Criterion<String> criterionFromConfig(
      Object type, Criterion<String> permissiveCriterion) {
    if ("*".equals(type)) { return permissiveCriterion; }
    boolean invert = false;
    if (type instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) type;
      if (map.containsKey("exclude")) {
        invert = true;
        type = map.get("exclude");
      } else {
        type = map.get("include");
      }
    }
    List<String> members = new ArrayList<String>();
    for (Object member : (List<?>) type) {
      if (member instanceof String) {
        members.add((String) member);
      } else {
        members.add((String) ((Map<?, ?>) member).get("key"));
      }
    }
    Criterion<String> set = in(members.toArray(new String[0]));
    if (invert) {
      return Criterion.Factory.and(
          permissiveCriterion, Criterion.Factory.not(set));
    } else {
      return set;
    }
  }

  private void defineProperty(
      String name,
      String sig,
      String defaultValue,
      Criterion<String> appliesTo,
      boolean inherited,
      Criterion<String> mediaGroups,
      String dom2property) {
    if ("".equals(defaultValue)) {
      throw new IllegalArgumentException(
          "Bad default value for symbol " + name + ", use null instead");
    }
    if (!CSS_IDENTIFIER.matcher(name).matches()) {
      throw new IllegalArgumentException("Bad property name: " + name);
    }
    if (!JS_IDENTIFIER.matcher(dom2property).matches()) {
      throw new IllegalArgumentException("Bad DOM2 name: " + name);
    }

    CssPropertySignature csssig = parseSignature(name, sig);
    properties.put(name, new CssPropertyInfo(
        name, csssig, mediaGroups, inherited, appliesTo, defaultValue,
        dom2property));
  }

  private void defineSymbol(String name, String sig) {
    if (sig == null) {
      throw new NullPointerException("Null signature for symbol " + name);
    }
    if (!CSS_IDENTIFIER.matcher(name).matches()) {
      throw new IllegalArgumentException("Bad symbol name: " + name);
    }
    CssPropertySignature csssig = parseSignature(name, sig);
    symbols.put(name, new SymbolInfo(name, csssig));
  }

  public CssSchema(WhiteList symbolsAndProperties, WhiteList functions) {
    for (WhiteList.TypeDefinition def
           : symbolsAndProperties.typeDefinitions().values()) {
      String key = (String) def.get("key", null);
      String aliasKey = (String) def.get("as", null);
      if (aliasKey != null) {
        // "as" aliases definition.
        def = merge(def, symbolsAndProperties.typeDefinitions().get(aliasKey));
      }
      if (key.startsWith("<") && key.endsWith(">")) {
        defineSymbol(
            key.substring(1, key.length() - 1),
            (String) def.get("signature", null));
      } else {
        Criterion<String> appliesTo = criterionFromConfig(
            def.get("appliesTo", "*"), ALL_ELEMENTS);
        Criterion<String> mediaGroups = criterionFromConfig(
            def.get("mediaGroups", "*"), ALL_MEDIA);
        String dom2property = (String) def.get("dom2property", null);
        defineProperty(
            key,
            (String) def.get("signature", null),
            (String) def.get("default", null),
            appliesTo,
            Boolean.TRUE.equals(def.get("inherited", null)),
            mediaGroups,
            dom2property);
      }
    }

    functionsAllowed = functions.allowedItems();
    propertiesAllowed = symbolsAndProperties.allowedItems();

    // Examine the property signatures and extract a list of keywords
    for (CssPropertyInfo pi : properties.values()) {
      pi.sig.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode n = ancestors.node;
          if (n instanceof CssPropertySignature.LiteralSignature) {
            String kw = ((CssPropertySignature.LiteralSignature) n).value;
            keywords.add(Strings.toLowerCase(kw));
          }
          return true;
        }
      }, null);
    }
    for (SymbolInfo si : symbols.values()) {
      si.sig.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode n = ancestors.node;
          if (n instanceof CssPropertySignature.LiteralSignature) {
            String kw = ((CssPropertySignature.LiteralSignature) n).value;
            keywords.add(Strings.toLowerCase(kw));
          }
          return true;
        }
      }, null);
    }
    keywords.add("initial");
  }

  private static class RegexpCriterion implements Criterion<String> {
    private final Pattern p;
    RegexpCriterion(Pattern p) {
      this.p = p;
    }

    public boolean accept(String s) {
      return s != null && p.matcher(s).matches();
    }
  }

  /**
   * Parses a CssSignature according to the grammar described in
   * http://www.w3.org/TR/CSS21/about.html#property-defs
   */
  private static CssPropertySignature parseSignature(String name, String sig) {
    try {
      return CssPropertySignature.Parser.parseSignature(sig);
    } catch (RuntimeException ex) {
      throw new RuntimeException(
          "Error parsing symbol " + name + " with signature " + sig, ex);
    }
  }

  private static WhiteList.TypeDefinition merge(
      final WhiteList.TypeDefinition wl1, final WhiteList.TypeDefinition wl2) {
    return new WhiteList.TypeDefinition() {
        public Object get(String key, Object defaultValue) {
          return wl1.get(key, wl2.get(key, defaultValue));
        }
      };
  }
}
