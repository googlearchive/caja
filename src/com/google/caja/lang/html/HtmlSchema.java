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

package com.google.caja.lang.html;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.config.ConfigUtil;
import com.google.caja.config.WhiteList;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.plugin.LoaderType;
import com.google.caja.plugin.UriEffect;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Lists;
import com.google.caja.util.Multimap;
import com.google.caja.util.Multimaps;
import com.google.caja.util.Pair;
import com.google.caja.util.Sets;
import com.google.caja.util.Strings;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An HTML schema which defines attributes of elements and which elements are
 * allowed.
 *
 * @author mikesamuel@gmail.com
 */
public final class HtmlSchema {
  // Public for convenience of c.g.c.plugin.Config.
  public static final URI defaultElementWhitelistURL = URI.create(
      "resource:///com/google/caja/lang/html/htmlall-elements.json");
  public static final URI defaultAttributeWhitelistURL = URI.create(
      "resource:///com/google/caja/lang/html/htmlall-attributes.json");

  private static final String VIRTUALIZATION_PREFIX = "caja-v-";
  private static final ElKey SCRIPT = ElKey.forHtmlElement("script");
  private static final ElKey STYLE = ElKey.forHtmlElement("style");
  private static final String DEFAULT_SCHEMA_INTERFACE = "HTMLElement";
  private static final String UNKNOWN_INTERFACE = "HTMLUnknownElement";

  private final Set<ElKey> allowedElements;
  private final Map<ElKey, HTML.Element> elementDetails;
  private final Set<AttribKey> allowedAttributes;
  private final Map<AttribKey, HTML.Attribute> attributeDetails;
  private final List<HTML.Attribute> attributesForUnknownHTMLElement;

  private static Pair<HtmlSchema, List<Message>> defaultSchema;
  /**
   * The default HTML4 whitelist.  See the JSON files in this directory for
   * the actual definitions.
   */
  public static HtmlSchema getDefault(MessageQueue mq) {
    if (defaultSchema == null) {
      SimpleMessageQueue cacheMq = new SimpleMessageQueue();
      try {
        defaultSchema = Pair.pair(
            new HtmlSchema(
                ConfigUtil.loadWhiteListFromJson(defaultElementWhitelistURL,
                    ConfigUtil.RESOURCE_RESOLVER, cacheMq),
                ConfigUtil.loadWhiteListFromJson(defaultAttributeWhitelistURL,
                    ConfigUtil.RESOURCE_RESOLVER, cacheMq)),
            cacheMq.getMessages());
      // If the default schema is borked, there's not much we can do.
      } catch (IOException ex) {
        mq.getMessages().addAll(cacheMq.getMessages());
        throw new SomethingWidgyHappenedError("Default schema is borked", ex);
      } catch (ParseException ex) {
        cacheMq.getMessages().add(ex.getCajaMessage());
        mq.getMessages().addAll(cacheMq.getMessages());
        throw new SomethingWidgyHappenedError("Default schema is borked", ex);
      }
    }
    mq.getMessages().addAll(defaultSchema.b);
    return defaultSchema.a;
  }

  /**
   * Elements that can and should be removed from the DOM and replaced by their
   * children with no semantic effect, when a document is being sanitized
   * without scripting support.
   */
  public static boolean isElementFoldable(ElKey el) {
    if (!el.isHtml()) { return false; }
    String cname = el.localName;
    return "head".equals(cname) || "body".equals(cname) || "html".equals(cname);
  }

  public HtmlSchema(WhiteList tagList, WhiteList attribList) {
    this.allowedAttributes = Sets.newHashSet();
    for (String key : attribList.allowedItems()) {
      AttribKey attribKey = attribKey(key);
      allowedAttributes.add(attribKey);
    }
    Map<AttribKey, RegularCriterion> criteria = Maps.newHashMap();
    for (WhiteList.TypeDefinition def : attribList.typeDefinitions().values()) {
      final String values = (String) def.get("values", null);
      RegularCriterion criterion = null;
      if (values != null) {
        criterion = RegularCriterion.Factory.fromValueSet(
            Arrays.asList(values.split(",")));
      } else {
        String pattern = (String) def.get("pattern", null);
        if (pattern != null) {
          criterion = RegularCriterion.Factory.fromPattern(
              "(?i:" + pattern + ")");
        }
      }
      if (criterion != null) {
        String key = Strings.lower((String) def.get("key", null));
        criteria.put(attribKey(key), criterion);
      }
    }

    this.attributeDetails = Maps.newHashMap();
    Multimap<ElKey, HTML.Attribute> attributeDetailsByElement
        = Multimaps.newListHashMultimap();
    for (WhiteList.TypeDefinition def : attribList.typeDefinitions().values()) {
      String key = Strings.lower((String) def.get("key", null));
      AttribKey elAndAttrib = attribKey(key);
      if (elAndAttrib == null) { throw new NullPointerException(key); }
      ElKey element = elAndAttrib.el;
      HTML.Attribute.Type type = HTML.Attribute.Type.NONE;
      String typeName = (String) def.get("type", null);
      if (typeName != null) {
        // TODO(mikesamuel): divert IllegalArgumentExceptions to MessageQueue
        type = HTML.Attribute.Type.valueOf(typeName);
      }
      String loaderTypeStr = (String) def.get("loaderType", null);
      // TODO(mikesamuel): divert IllegalArgumentExceptions to MessageQueue
      LoaderType loaderType = loaderTypeStr != null
          ? LoaderType.valueOf(loaderTypeStr) : null;
      String uriEffectStr = (String) def.get("uriEffect", null);
      // TODO(mikesamuel): divert IllegalArgumentExceptions to MessageQueue
      UriEffect uriEffect = uriEffectStr != null
          ? UriEffect.valueOf(uriEffectStr) : null;
      RegularCriterion elCriterion = criteria.get(elAndAttrib);
      RegularCriterion wcCriterion = criteria.get(elAndAttrib.onAnyElement());
      RegularCriterion criterion = conjunction(elCriterion, wcCriterion);
      String defaultValue = (String) def.get("default", null);
      boolean optional = Boolean.TRUE.equals(def.get("optional", true));
      String safeValue = (String) def.get("safeValue", null);
      if (safeValue == null) {
        String candidate = defaultValue != null ? defaultValue : "";
        if (criterion.accept(candidate)) {
          safeValue = candidate;
        } else {
          String values = (String) def.get("values", null);
          if (values != null) {
            safeValue = values.split(",")[0];
          }
        }
      }
      boolean valueless = Boolean.TRUE.equals(def.get("valueless", false));
      // For valueless attributes, like checked, we allow the blank value.
      if (valueless && !criterion.accept("")) {
        criterion = RegularCriterion.Factory.or(
            criterion,
            RegularCriterion.Factory.fromValueSet(Collections.singleton("")));
      }

      HTML.Attribute a = new HTML.Attribute(
          elAndAttrib, type, defaultValue, safeValue, valueless, optional,
          loaderType, uriEffect, criterion);
      attributeDetails.put(elAndAttrib, a);
      attributeDetailsByElement.put(element, a);
    }

    this.attributesForUnknownHTMLElement = Collections.unmodifiableList(
        Lists.newArrayList(attributeDetailsByElement.get(ElKey.HTML_WILDCARD)));

    this.allowedElements = Sets.newHashSet();
    for (String qualifiedName : tagList.allowedItems()) {
      allowedElements.add(
          ElKey.forElement(Namespaces.HTML_DEFAULT, qualifiedName));
    }
    this.elementDetails = Maps.newHashMap();
    for (WhiteList.TypeDefinition def : tagList.typeDefinitions().values()) {
      String qualifiedTagName = (String) def.get("key", null);
      ElKey key = ElKey.forElement(Namespaces.HTML_DEFAULT, qualifiedTagName);
      Collection<HTML.Attribute> specific = attributeDetailsByElement.get(key);
      ElKey wc = ElKey.wildcard(key.ns);
      Collection<HTML.Attribute> general = attributeDetailsByElement.get(wc);
      List<HTML.Attribute> attrs = Lists.newArrayList(specific);
      if (!general.isEmpty()) {
        Set<AttribKey> present = Sets.newHashSet();
        for (HTML.Attribute a : attrs) {
          present.add(a.getKey().onElement(wc));
        }
        for (HTML.Attribute a : general) {
          if (!present.contains(a.getKey())) { attrs.add(a); }
        }
      }
      boolean empty = (Boolean) def.get("empty", Boolean.FALSE);
      boolean optionalEnd = (Boolean) def.get("optionalEnd", Boolean.FALSE);
      boolean containsText = (Boolean) def.get("textContent", Boolean.TRUE)
          && !empty;
      String domInterface = (String) def.get("interface",
          DEFAULT_SCHEMA_INTERFACE);
      elementDetails.put(
          key, new HTML.Element(key, attrs, empty, optionalEnd, containsText,
              domInterface));
    }
  }

  public Set<AttribKey> getAttributeNames() {
    return attributeDetails.keySet();
  }

  // TODO(kpreid): Virtualization makes this set arguably unbounded; review
  // whether any code is affected.
  public Set<ElKey> getElementNames() {
    return elementDetails.keySet();
  }

  public boolean isElementAllowed(ElKey elementName) {
    return allowedElements.contains(elementName) ||
        elementName.localName.startsWith(VIRTUALIZATION_PREFIX);
  }

  public HTML.Element lookupElement(ElKey elementName) {
    HTML.Element details = elementDetails.get(elementName);
    if (details != null) {
      return details;
    } else {
      // May be a virtualized form of an unknown element, but we don't care --
      // just virtualize all non-global attributes on it.
      return new HTML.Element(elementName, attributesForUnknownHTMLElement,
        false, false, false, UNKNOWN_INTERFACE);
    }
  }

  /**
   * Elements that are to be rewritten with different tag names to avoid the
   * known or unknown browser semantics of them.
   */
  public boolean isElementVirtualized(ElKey el) {
    // <script> and <style> are excluded because we want to rewrite them,
    // not virtualize them, but they are considered unsafe because their
    // text contents have meaning.
    return !isElementAllowed(el) && !SCRIPT.equals(el) && !STYLE.equals(el);
  }

  public ElKey virtualToRealElementName(ElKey virtual) {
    if (isElementVirtualized(virtual)) {
      // TODO(kpreid): Better to modify the NS instead of the local name if the
      // input is not in HTML NS (i.e. we are in XML rather than HTML).
      // Currently can't happen as only HTML elements are virtualized.
      return ElKey.forElement(virtual.ns,
                              VIRTUALIZATION_PREFIX + virtual.localName);
    } else {
      return virtual;
    }
  }

  public boolean isAttributeAllowed(AttribKey k) {
    HTML.Attribute a = lookupAttribute(k);
    if (a != null && allowedAttributes.contains(a.getKey())) { return true; }
    String keyName = k.localName;
    return keyName.startsWith("data-caja-") && !keyName.endsWith("___");
  }

  public HTML.Attribute lookupAttribute(AttribKey k) {
    if (isElementVirtualized(k.el)) {
      k = k.onElement(virtualToRealElementName(k.el));
    }
    HTML.Attribute attr = attributeDetails.get(k);
    if (attr == null) {
      attr = attributeDetails.get(k.onAnyElement());
    }
    return attr;
  }

  private static AttribKey attribKey(String key) {
    int separator = key.indexOf("::");
    if (separator == -1) {
      throw new IllegalArgumentException("no :: in: " + key);
    }
    String elQName = key.substring(0, separator);
    String attrQName = key.substring(separator + 2);
    ElKey el = ElKey.forElement(Namespaces.HTML_DEFAULT, elQName);
    if (el == null) { throw new NullPointerException(elQName); }
    AttribKey a = AttribKey.forAttribute(
        Namespaces.HTML_DEFAULT, el, attrQName);
    if (a == null) { throw new NullPointerException(attrQName); }
    return a;
  }

  private static RegularCriterion conjunction(
      RegularCriterion a, RegularCriterion b) {
    RegularCriterion c = a;
    if (b != null) { c = c == null ? b : RegularCriterion.Factory.and(c, b); }
    return c != null ? c : RegularCriterion.Factory.optimist();
  }
}
