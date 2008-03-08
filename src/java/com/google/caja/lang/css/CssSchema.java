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
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Criterion;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
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

  public static CssSchema getDefaultCss21Schema(MessageQueue mq) {
    FilePosition propPos = FilePosition.startOfFile(new InputSource(URI.create(
        "resource:///com/google/caja/lang/css/css21.json")));
    FilePosition fnPos = FilePosition.startOfFile(new InputSource(URI.create(
        "resource:///com/google/caja/lang/css/css21-fns.json")));
    WhiteList propDefs, fnDefs;
    try {
      propDefs = ConfigUtil.loadWhiteListFromJson(
          ConfigUtil.openConfigResource(propPos.source().getUri(), null),
          propPos, mq);
      fnDefs = ConfigUtil.loadWhiteListFromJson(
          ConfigUtil.openConfigResource(fnPos.source().getUri(), null),
          fnPos, mq);
    // If the default schema is borked, there's not much we can do.
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
      throw new RuntimeException(ex);
    }
    return new CssSchema(propDefs, fnDefs);
  }

  /**
   * Returns the signature and other information for a css property.
   * @param propertyName non null.
   * @return null if no such property.
   */
  public CssPropertyInfo getCssProperty(String propertyName) {
    return properties.get(propertyName);
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
    return keywords.contains(name.toLowerCase());
  }

  /** Is the given word the name of a CSS function? */
  public boolean isFunctionAllowed(String name) {
    return functionsAllowed.contains(name.toLowerCase());
  }

  /** Is the given word the name of an allowed CSS property? */
  public boolean isPropertyAllowed(String name) {
    return propertiesAllowed.contains(name.toLowerCase());
  }

  public static boolean isMediaType(String mediaType) {
    return ALL_MEDIA.accept(mediaType);
  }

  /**
   * Encapsulates a css property and its signatures.
   */
  public static final class CssPropertyInfo extends SymbolInfo {
    public final Criterion<String> mediaGroups;
    public final boolean inherited;
    public final Criterion<String> appliesTo;
    public final String defaultValue;

    private CssPropertyInfo(
        String name, CssPropertySignature sig, Criterion<String> mediaGroups,
        boolean inherited, Criterion<String> appliesTo, String defaultValue) {
      super(name, sig);
      this.mediaGroups = mediaGroups;
      this.inherited = inherited;
      // Not defensively copied.  This is usually an immutable AllSet.
      this.appliesTo = appliesTo;
      this.defaultValue = defaultValue;
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
  // Perhaps by using HTML4?
  private static final Pattern HTML_IDENTIFIER = Pattern.compile("^[\\w\\-]+$");
  private static final Pattern CSS_IDENTIFIER =
    Pattern.compile("^[a-zA-Z][\\w\\-]*$");
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
      // Exclusions in the definitions specify things like
      //     All elements except TABLE, TBODY, TFOOT, CAPTION, TR
      // and inclusions specify things like
      //     BUTTON, INPUT, SELECT, TEXTAREA

      // The permissiveCriterion defines the universal set, so an
      // exclude cannot be larger than "All".
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
      Criterion<String> mediaGroups) {
    if ("".equals(defaultValue)) {
      throw new IllegalArgumentException(
          "Bad default value for symbol " + name + ", use null instead");
    }
    if (!CSS_IDENTIFIER.matcher(name).matches()) {
      throw new IllegalArgumentException("Bad property name: " + name);
    }

    CssPropertySignature csssig = parseSignature(name, sig);
    assert CSS_IDENTIFIER.matcher(name).matches();
    properties.put(name, new CssPropertyInfo(
        name, csssig, mediaGroups, inherited, appliesTo, defaultValue));
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
        def = symbolsAndProperties.typeDefinitions().get(aliasKey);
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
        defineProperty(
            key,
            (String) def.get("signature", null),
            (String) def.get("default", null),
            appliesTo,
            Boolean.TRUE.equals(def.get("inherited", null)),
            mediaGroups);
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
            keywords.add(kw.toLowerCase());
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
            keywords.add(kw.toLowerCase());
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
}
