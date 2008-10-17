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
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.SimpleMessageQueue;
import com.google.caja.util.Criterion;
import com.google.caja.util.Pair;
import com.google.caja.util.Strings;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An HTML schema which defines attributes of elements and which elements are
 * allowed.
 *
 * TODO(mikesamuel): respect required attributes for tags.
 *
 * @author mikesamuel@gmail.com
 */
public final class HtmlSchema {
  private final Set<String> allowedElements;
  private final Map<String, HTML.Element> elementDetails;
  private final Set<String> allowedAttributes;
  private final Map<String, HTML.Attribute> attributeDetails;
  private final Map<String, Criterion<String>> attributeCriteria;

  private static Pair<HtmlSchema, List<Message>> defaultSchema;
  /**
   * The default HTML4 whitelist.  See the JSON files in this directory for
   * the actual definitions.
   */
  public static HtmlSchema getDefault(MessageQueue mq) {
    if (defaultSchema == null) {
      SimpleMessageQueue cacheMq = new SimpleMessageQueue();
      FilePosition elPos = FilePosition.startOfFile(
          new InputSource(URI.create(
              "resource:///com/google/caja/lang/html/html4-elements.json")));
      FilePosition attrPos = FilePosition.startOfFile(
          new InputSource(URI.create(
              "resource:///com/google/caja/lang/html/html4-attributes.json")));
      try {
        defaultSchema = Pair.pair(
            new HtmlSchema(
                ConfigUtil.loadWhiteListFromJson(ConfigUtil.openConfigResource(
                    elPos.source().getUri(), null), elPos, cacheMq),
                ConfigUtil.loadWhiteListFromJson(ConfigUtil.openConfigResource(
                   attrPos.source().getUri(), null), attrPos, cacheMq)),
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

  public HtmlSchema(WhiteList tagList, WhiteList attribList) {
    this.allowedElements = new HashSet<String>();
    for (String name : tagList.allowedItems()) {
      allowedElements.add(Strings.toLowerCase(name));
    }
    this.elementDetails = new HashMap<String, HTML.Element>();
    for (WhiteList.TypeDefinition def : tagList.typeDefinitions().values()) {
      String name = Strings.toLowerCase((String) def.get("key", null));
      elementDetails.put(
          name,
          new HTML.Element(
              name,
              (Boolean) def.get("empty", Boolean.FALSE),
              (Boolean) def.get("optionalEnd", Boolean.FALSE)));
    }
    this.allowedAttributes = new HashSet<String>();
    for (String name : attribList.allowedItems()) {
      allowedAttributes.add(Strings.toLowerCase(name));
    }
    this.attributeDetails = new HashMap<String, HTML.Attribute>();
    this.attributeCriteria = new HashMap<String, Criterion<String>>();
    for (WhiteList.TypeDefinition def : attribList.typeDefinitions().values()) {
      String key = Strings.toLowerCase((String) def.get("key", null));
      int colon = key.indexOf(':');
      String element = key.substring(0, colon),
          attrib = key.substring(colon + 1);
      HTML.Attribute.Type type = HTML.Attribute.Type.NONE;
      String typeName = (String) def.get("type", null);
      if (typeName != null) {
        // TODO(mikesamuel): divert IllegalArgumentExceptions to MessageQueue
        type = HTML.Attribute.Type.valueOf(typeName);
      }
      String mimeTypes = (String) def.get("mimeTypes", null);
      attributeDetails.put(
          key, new HTML.Attribute(element, attrib, type, mimeTypes));
      final String values = (String) def.get("values", null);
      Criterion<String> criterion = null;
      if (values != null) {
        criterion = new Criterion<String>() {
          final Set<String> valueSet = new HashSet<String>(
             Arrays.asList(Strings.toLowerCase(values).split(",")));
          public boolean accept(String s) {
            return valueSet.contains(Strings.toLowerCase(s));
          }

          @Override
          public String toString() {
            return "[Value in " + values + "]";
          }
        };
      } else {
        String pattern = (String) def.get("pattern", null);
        if (pattern != null) {
          final Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
          criterion = new Criterion<String>() {
            public boolean accept(String s) {
              return p.matcher(s).matches();
            }

            @Override
            public String toString() {
              return "[Value =~ /" + p.pattern() + "/]";
            }
          };
        }
      }
      if (criterion != null) {
        attributeCriteria.put(key, criterion);
      }
    }
  }

  public boolean isElementAllowed(String elementName) {
    assert Strings.isLowerCase(elementName);
    return allowedElements.contains(elementName);
  }

  public HTML.Element lookupElement(String elementName) {
    assert Strings.isLowerCase(elementName);
    return elementDetails.get(elementName);
  }

  public boolean isAttributeAllowed(String elementName, String attribName) {
    assert Strings.isLowerCase(elementName) && Strings.isLowerCase(attribName);
    return allowedAttributes.contains(elementName + ":" + attribName)
        || allowedAttributes.contains("*:" + attribName);
  }

  public HTML.Attribute lookupAttribute(String elementName, String attribName) {
    assert Strings.isLowerCase(elementName) && Strings.isLowerCase(attribName);
    HTML.Attribute attr = attributeDetails.get(elementName + ":" + attribName);
    if (attr == null) {
      attr = attributeDetails.get("*:" + attribName);
    }
    return attr;
  }

  /** Criteria that attribute values must satisfy. */
  public Criterion<? super String> getAttributeCriteria(
      String tagName, String attribName) {
    assert Strings.isLowerCase(tagName) && Strings.isLowerCase(attribName);
    Criterion<String> specific
        = attributeCriteria.get(tagName + ":" + attribName);
    Criterion<String> general = attributeCriteria.get("*:" + attribName);
    if (specific != null) {
      return (general != null)
          ? Criterion.Factory.and(specific, general)
          : specific;
    } else {
      return general != null ? general : Criterion.Factory.<String>optimist();
    }
  }
}
