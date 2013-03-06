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
import com.google.caja.config.ConfigUtil;
import com.google.caja.config.WhiteList;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssPropertySignature;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Criterion;
import com.google.caja.util.Name;
import com.google.caja.util.Pair;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
  // Public strings for convenience of c.g.c.plugin.Config.
  public static final URI defaultPropWhitelistURL = URI.create(
      "resource:///com/google/caja/lang/css/css-extensions.json");
  public static final URI defaultFnWhitelistURL= URI.create(
      "resource:///com/google/caja/lang/css/css-extensions-fns.json");

  private final Map<Name, CssPropertyInfo> properties =
    new HashMap<Name, CssPropertyInfo>();
  private final Map<Name, SymbolInfo> symbols =
    new HashMap<Name, SymbolInfo>();
  private final Set<Name> keywords = new HashSet<Name>();
  private final Set<Name> functionsAllowed;
  private final Set<Name> propertiesAllowed;

  private static Pair<CssSchema, List<Message>> defaultSchema;
  public static CssSchema getDefaultCss21Schema(MessageQueue mq) {
    if (defaultSchema == null) {
      SimpleMessageQueue cacheMq = new SimpleMessageQueue();
      WhiteList propDefs, fnDefs;
      try {
        propDefs = ConfigUtil.loadWhiteListFromJson(
            defaultPropWhitelistURL, ConfigUtil.RESOURCE_RESOLVER, cacheMq);
        fnDefs = ConfigUtil.loadWhiteListFromJson(
            defaultFnWhitelistURL, ConfigUtil.RESOURCE_RESOLVER, cacheMq);
      // If the default schema is borked, there's not much we can do.
      } catch (IOException ex) {
        mq.getMessages().addAll(cacheMq.getMessages());
        throw new SomethingWidgyHappenedError("Default schema is borked", ex);
      } catch (ParseException ex) {
        ex.toMessageQueue(cacheMq);
        mq.getMessages().addAll(cacheMq.getMessages());
        throw new SomethingWidgyHappenedError("Default schema is borked", ex);
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
  public CssPropertyInfo getCssProperty(Name propertyName) {
    // http://www.w3.org/TR/CSS21/syndata.html#characters
    // All CSS style sheets are case-insensitive, except for parts
    // that are not under the control of CSS.
    return properties.get(propertyName);
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
  public SymbolInfo getSymbol(Name symbolName) {
    return symbols.get(symbolName);
  }

  /** Is the given word a css keyword? */
  public boolean isKeyword(Name name) {
    return keywords.contains(name);
  }

  /** Is the given word the name of a CSS function? */
  public boolean isFunctionAllowed(Name name) {
    return functionsAllowed.contains(name);
  }

  /** Is the given word the name of an allowed CSS property? */
  public boolean isPropertyAllowed(Name name) {
    return propertiesAllowed.contains(name);
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
    public final List<String> dom2properties;

    private CssPropertyInfo(
        Name name, CssPropertySignature sig, Criterion<String> mediaGroups,
        boolean inherited, Criterion<String> appliesTo, String defaultValue,
        List<String> dom2properties) {
      super(name, sig);
      this.mediaGroups = mediaGroups;
      this.inherited = inherited;
      // Not defensively copied.  This is usually an immutable AllSet.
      this.appliesTo = appliesTo;
      this.defaultValue = defaultValue;
      this.dom2properties = dom2properties;
    }
  }

  /**
   * Encapsulates a css symbol.  A symbol is a non top-level entity in a
   * declaration.  Symbols include things like &lt;color&gt;, &lt;uri&gt;,
   * &lt;integer&gt;, etc.
   */
  public static class SymbolInfo {
    public final Name name;
    public final CssPropertySignature sig;

    private SymbolInfo(Name name, CssPropertySignature sig) {
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
    if (type instanceof Map<?, ?>) {
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
      Name name,
      String sig,
      String defaultValue,
      Criterion<String> appliesTo,
      boolean inherited,
      Criterion<String> mediaGroups,
      List<String> dom2properties) {
    if ("".equals(defaultValue)) {
      throw new IllegalArgumentException(
          "Bad default value for symbol " + name + ", use null instead");
    }
    if (!CSS_IDENTIFIER.matcher(name.getCanonicalForm()).matches()) {
      throw new IllegalArgumentException("Bad property name: " + name);
    }
    for (String dom2property : dom2properties) {
      if (!JS_IDENTIFIER.matcher(dom2property).matches()) {
        throw new IllegalArgumentException("Bad DOM2 name: " + dom2property);
      }
    }

    CssPropertySignature csssig = parseSignature(name, sig);
    properties.put(name, new CssPropertyInfo(
        name, csssig, mediaGroups, inherited, appliesTo, defaultValue,
        dom2properties));
  }

  private void defineSymbol(Name name, String sig) {
    if (sig == null) {
      throw new NullPointerException("Null signature for symbol " + name);
    }
    if (!CSS_IDENTIFIER.matcher(name.getCanonicalForm()).matches()) {
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
            Name.css(key.substring(1, key.length() - 1)),
            (String) def.get("signature", null));
      } else {
        Criterion<String> appliesTo = criterionFromConfig(
            def.get("appliesTo", "*"), ALL_ELEMENTS);
        Criterion<String> mediaGroups = criterionFromConfig(
            def.get("mediaGroups", "*"), ALL_MEDIA);
        Object dom2property = def.get("dom2property", null);
        List<String> dom2properties;
        if (dom2property instanceof String) {
          dom2properties = Collections.singletonList((String) dom2property);
        } else if (dom2property == null) {
          dom2properties = Collections.<String>emptyList();
        } else {
          dom2properties = new ArrayList<String>();
          for (Object item : (Iterable<?>) dom2property) {
            dom2properties.add((String) item);
          }
        }
        defineProperty(
            Name.css(key),
            (String) def.get("signature", null),
            (String) def.get("default", null),
            appliesTo,
            Boolean.TRUE.equals(def.get("inherited", null)),
            mediaGroups,
            dom2properties);
      }
    }

    functionsAllowed = new LinkedHashSet<Name>();
    for (String k : functions.allowedItems()) {
      functionsAllowed.add(Name.css(k));
    }
    propertiesAllowed = new LinkedHashSet<Name>();
    for (String k : symbolsAndProperties.allowedItems()) {
      propertiesAllowed.add(Name.css(k));
    }

    // Examine the property signatures and extract a list of keywords
    for (CssPropertyInfo pi : properties.values()) {
      pi.sig.acceptPreOrder(new Visitor() {
        public boolean visit(AncestorChain<?> ancestors) {
          ParseTreeNode n = ancestors.node;
          if (n instanceof CssPropertySignature.LiteralSignature) {
            String kw = ((CssPropertySignature.LiteralSignature) n).value;
            keywords.add(Name.css(kw));
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
            keywords.add(Name.css(kw));
          }
          return true;
        }
      }, null);
    }
    keywords.add(Name.css("initial"));
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
  private static CssPropertySignature parseSignature(Name name, String sig) {
    try {
      return CssPropertySignature.Parser.parseSignature(sig);
    } catch (RuntimeException ex) {
      throw new SomethingWidgyHappenedError(
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
