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

import com.google.caja.config.ConfigUtil;
import com.google.caja.config.WhiteList;
import com.google.caja.lexer.ParseException;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Name;
import com.google.caja.util.Pair;
import com.google.caja.util.Strings;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
  private static final Name WILDCARD = Name.html("*");

  private final Set<Name> allowedElements;
  private final Map<Name, HTML.Element> elementDetails;
  private final Set<Pair<Name, Name>> allowedAttributes;
  private final Map<Pair<Name, Name>, HTML.Attribute> attributeDetails;

  private static Pair<HtmlSchema, List<Message>> defaultSchema;
  /**
   * The default HTML4 whitelist.  See the JSON files in this directory for
   * the actual definitions.
   */
  public static HtmlSchema getDefault(MessageQueue mq) {
    if (defaultSchema == null) {
      SimpleMessageQueue cacheMq = new SimpleMessageQueue();
      URI elSrc = URI.create(
              "resource:///com/google/caja/lang/html/"
              + "html4-elements-extensions.json");
      URI attrSrc = URI.create(
              "resource:///com/google/caja/lang/html/"
              + "html4-attributes-extensions.json");
      try {
        defaultSchema = Pair.pair(
            new HtmlSchema(
                ConfigUtil.loadWhiteListFromJson(
                    elSrc, ConfigUtil.RESOURCE_RESOLVER, cacheMq),
                ConfigUtil.loadWhiteListFromJson(
                    attrSrc, ConfigUtil.RESOURCE_RESOLVER, cacheMq)),
            cacheMq.getMessages());
      // If the default schema is borked, there's not much we can do.
      } catch (IOException ex) {
        mq.getMessages().addAll(cacheMq.getMessages());
        throw new RuntimeException(ex);
      } catch (ParseException ex) {
        cacheMq.getMessages().add(ex.getCajaMessage());
        mq.getMessages().addAll(cacheMq.getMessages());
        throw new RuntimeException(ex);
      }
    }
    mq.getMessages().addAll(defaultSchema.b);
    return defaultSchema.a;
  }

  /**
   * Elements that can be removed from the DOM without changing behavior as long
   * as their children are folded into the element's parent.
   */
  public static boolean isElementFoldable(Name tagName) {
    String cname = tagName.getCanonicalForm();
    return "head".equals(cname) || "body".equals(cname) || "html".equals(cname);
  }

  public HtmlSchema(WhiteList tagList, WhiteList attribList) {
    this.allowedAttributes = new HashSet<Pair<Name, Name>>();
    for (String key : attribList.allowedItems()) {
      allowedAttributes.add(elAndAttrib(key));
    }
    Map<Pair<Name, Name>, RegularCriterion> criteria
        = new HashMap<Pair<Name, Name>, RegularCriterion>();
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
        String key = Strings.toLowerCase((String) def.get("key", null));
        criteria.put(elAndAttrib(key), criterion);
      }
    }

    this.attributeDetails = new HashMap<Pair<Name, Name>, HTML.Attribute>();
    Map<Name, List<HTML.Attribute>> attributeDetailsByElement
        = new HashMap<Name, List<HTML.Attribute>>();
    for (WhiteList.TypeDefinition def : attribList.typeDefinitions().values()) {
      String key = Strings.toLowerCase((String) def.get("key", null));
      Pair<Name, Name> elAndAttrib = elAndAttrib(key);
      Name element = elAndAttrib.a;
      Name attrib = elAndAttrib.b;
      HTML.Attribute.Type type = HTML.Attribute.Type.NONE;
      String typeName = (String) def.get("type", null);
      if (typeName != null) {
        // TODO(mikesamuel): divert IllegalArgumentExceptions to MessageQueue
        type = HTML.Attribute.Type.valueOf(typeName);
      }
      String mimeTypes = (String) def.get("mimeTypes", null);
      RegularCriterion elCriterion = criteria.get(elAndAttrib);
      RegularCriterion wcCriterion = criteria.get(Pair.pair(WILDCARD, attrib));
      RegularCriterion criterion;
      if (elCriterion != null) {
        if (wcCriterion != null) {
          criterion = RegularCriterion.Factory.and(elCriterion, wcCriterion);
        } else {
          criterion = elCriterion;
        }
      } else if (wcCriterion != null) {
        criterion = wcCriterion;
      } else {
        criterion = RegularCriterion.Factory.optimist();
      }
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
      HTML.Attribute a = new HTML.Attribute(
          element, attrib, type, defaultValue, safeValue, valueless, optional,
          mimeTypes, criterion);
      attributeDetails.put(elAndAttrib, a);
      List<HTML.Attribute> byElement = attributeDetailsByElement.get(element);
      if (byElement == null) {
        byElement = new ArrayList<HTML.Attribute>();
        attributeDetailsByElement.put(element, byElement);
      }
      byElement.add(a);
    }

    List<HTML.Attribute> all = attributeDetailsByElement.get(WILDCARD);
    if (all != null) {
      for (Map.Entry<Name, List<HTML.Attribute>> e
           : attributeDetailsByElement.entrySet()) {
        Name element = e.getKey();
        if (!WILDCARD.equals(element)) {
          for (HTML.Attribute ecAttr : all) {
            if (!attributeDetails.containsKey(
                    Pair.pair(element, ecAttr.getAttributeName()))) {
              e.getValue().add(ecAttr);
            }
          }
        }
        e.setValue(Collections.unmodifiableList(e.getValue()));
      }
      all = Collections.unmodifiableList(all);
    } else {
      all = Collections.<HTML.Attribute>emptyList();
    }
    this.allowedElements = new HashSet<Name>();
    for (String name : tagList.allowedItems()) {
      allowedElements.add(Name.html(name));
    }
    this.elementDetails = new HashMap<Name, HTML.Element>();
    for (WhiteList.TypeDefinition def : tagList.typeDefinitions().values()) {
      Name name = Name.html((String) def.get("key", null));
      List<HTML.Attribute> attrs = attributeDetailsByElement.get(name);
      if (attrs == null) { attrs = all; }
      boolean empty = (Boolean) def.get("empty", Boolean.FALSE);
      boolean optionalEnd = (Boolean) def.get("optionalEnd", Boolean.FALSE);
      elementDetails.put(
          name, new HTML.Element(name, attrs, empty, optionalEnd));
    }
  }

  public Set<Pair<Name, Name>> getAttributeNames() {
    return attributeDetails.keySet();
  }

  public Set<Name> getElementNames() {
    return elementDetails.keySet();
  }

  public boolean isElementAllowed(Name elementName) {
    return allowedElements.contains(elementName);
  }

  public HTML.Element lookupElement(Name elementName) {
    return elementDetails.get(elementName);
  }

  public boolean isAttributeAllowed(Name elementName, Name attribName) {
    HTML.Attribute a = lookupAttribute(elementName, attribName);
    if (a == null) { return false; }
    return allowedAttributes.contains(
        Pair.pair(a.getElementName(), a.getAttributeName()));
  }

  public HTML.Attribute lookupAttribute(Name elementName, Name attribName) {
    HTML.Attribute attr = attributeDetails.get(
        Pair.pair(elementName, attribName));
    if (attr == null) {
      attr = attributeDetails.get(Pair.pair(WILDCARD, attribName));
    }
    return attr;
  }

  private static Pair<Name, Name> elAndAttrib(String key) {
    int separator = key.indexOf("::");
    Name element = Name.html(key.substring(0, separator));
    Name attrib = Name.html(key.substring(separator + 2));
    return Pair.pair(element, attrib);
  }
}
