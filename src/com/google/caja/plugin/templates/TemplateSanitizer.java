// Copyright (C) 2009 Google Inc.
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

package com.google.caja.plugin.templates;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lang.html.HTML;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Nodes;
import com.google.caja.plugin.Placeholder;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Criterion;
import com.google.caja.util.Lists;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Rewrites an IHTML DOM, removing potentially unsafe constructs that
 * can be ignored, and issuing errors if the constructs cannot be removed.
 *
 * @author mikesamuel@gmail.com
 */
public final class TemplateSanitizer {
  private final HtmlSchema schema;
  private final MessageQueue mq;
  private static String cajaPrefix = "data-caja-";

  /**
   * @param schema specifies which tags and attributes are allowed, and which
   *   attribute values are allowed.
   * @param mq a message queue that will receive errors on unsafe nodes or
   *   attributes, and warnings on removed nodes.
   */
  public TemplateSanitizer(HtmlSchema schema, MessageQueue mq) {
    this.schema = schema;
    this.mq = mq;
  }

  /**
   * @param t the node to sanitize.
   * @return true iff the htmlRoot can be safely used.  If false, explanatory
   *     messages were added to the MessageQueue passed to the constructor.
   */
  public boolean sanitize(Node t) {
    boolean valid = true;
    switch (t.getNodeType()) {
      case Node.ELEMENT_NODE:
      {
        Element el = (Element) t;
        ElKey elKey = ElKey.forElement(el);

        if (isElementIgnorable(elKey)) {
          return removeElement(el);

        } else if (schema.isElementVirtualized(elKey)) {
          valid &= virtualizeElement(elKey, el);

        } else if (!schema.isElementAllowed(elKey)) {
          // An explicitly disallowed but not virtualized element is an error.
          mq.getMessages().add(new Message(IhtmlMessageType.UNSAFE_UNVIRT_TAG,
              Nodes.getFilePositionFor(el), elKey));

          return false;
        }
        valid &= sanitizeAttrs(elKey, el, false);

        // We know by construction of org.w3c.Element that there can only be
        // one attribute with a given name.
        // If that were not the case, passes that only inspect the
        // first occurrence of an attribute could be spoofed.
        break;
      }
      case Node.DOCUMENT_FRAGMENT_NODE:
      case Node.TEXT_NODE:
      case Node.CDATA_SECTION_NODE:
      case Node.COMMENT_NODE:
        break;
      default:
        throw new SomethingWidgyHappenedError(t.getNodeName());
    }
    for (Node child : Nodes.childrenOf(t)) {
      valid &= sanitize(child);
    }
    return valid;
  }

  private boolean sanitizeAttrs(ElKey elKey, Element el, boolean ignore) {
    boolean valid = true;
    // Snapshot and then iterate so that removals/additions don't miss things.
    List<Attr> attrs = Lists.newArrayList(Nodes.attributesOf(el));
    for (Attr attr : attrs) {
      valid &= sanitizeAttr(elKey, el, attr, ignore);
    }
    return valid;
  }

  private boolean sanitizeAttr(
      ElKey elKey, Element el, Attr attrib, boolean ignore) {
    boolean valid = true;
    AttribKey attrKey = AttribKey.forAttribute(elKey, attrib);
    HTML.Attribute a = schema.lookupAttribute(attrKey);
    if (null == a) {
      if (!Placeholder.ID_ATTR.is(attrib)) {
        if (attrKey.localName.endsWith("__")) { valid = false; }
        else if (!attrKey.localName.startsWith(cajaPrefix)) {
          valid &= virtualizeAttribute(attrib, el, attrKey);
        }
      }
    } else if (!schema.isAttributeAllowed(attrKey)) {
      if (!ignore) {
        mq.addMessage(
            PluginMessageType.UNSAFE_ATTRIBUTE,
            Nodes.getFilePositionFor(attrib), attrKey, elKey);
      }
      valid &= virtualizeAttribute(attrib, el, attrKey);
    } else {
      // We do not subject "target" attributes to the value criteria
      if (a.getType() != HTML.Attribute.Type.FRAME_TARGET) {
        Criterion<? super String> criteria = a.getValueCriterion();
        if (!criteria.accept(attrib.getNodeValue())) {
          if (!ignore) {
            mq.addMessage(
                PluginMessageType.DISALLOWED_ATTRIBUTE_VALUE,
                Nodes.getFilePositionForValue(attrib),
                attrKey, MessagePart.Factory.valueOf(attrib.getNodeValue()));
          }
          valid &= removeBadAttribute(el, attrKey);
        }
      }
    }
    return valid;
  }

  private boolean virtualizeAttribute(
      Attr attrib, Element el, AttribKey attrKey) {
    boolean valid = true;
    // Remove this attribute and create another that
    // starts with "data-caja-".
    String localName = attrKey.localName;
    String value = attrib.getValue();
    valid &= removeBadAttribute(el, attrKey);
    if (valid) {
      el.setAttributeNS(
          attrKey.ns.uri,
          cajaPrefix + localName,
          value == null ? localName : value);
    }
    return valid;
  }

  private boolean removeBadAttribute(Element el, AttribKey attrKey) {
    el.removeAttributeNS(attrKey.ns.uri, attrKey.localName);
    return true;
  }

  /**
   * Elements that can be safely removed from the DOM without changing behavior.
   */
  private static boolean isElementIgnorable(ElKey elKey) {
    if (!elKey.isHtml()) { return false; }
    String lcName = elKey.localName;
    return "noscript".equals(lcName) || "noembed".equals(lcName)
        || "noframes".equals(lcName);
  }

  private boolean removeElement(Element el) {
    ElKey elKey = ElKey.forElement(el);
    Node p = el.getParentNode();
    if (p != null) {
      mq.getMessages().add(new Message(IhtmlMessageType.IGNORED_TAG,
          Nodes.getFilePositionFor(el), elKey));
      p.removeChild(el);
      return true;
    } else {
      mq.getMessages().add(new Message(IhtmlMessageType.UNSAFE_ROOT_TAG,
          Nodes.getFilePositionFor(el), elKey));
      return false;
    }
  }

  /**
   * Transfer the children of a
   * {@link HtmlSchema#isElementVirtualized virtualized} element into a new
   * renamed element.
   *
   * @param el a tag with a mutable parent which will be modified in place.
   * @return true iff the el's children are transitively valid.
   */
  private boolean virtualizeElement(ElKey elKey, Element el) {
    boolean valid = true;

    elKey = schema.virtualToRealElementName(elKey);

    // Note ordering: the attributes will be sanitized according to the
    // virtualized element's attribute info (which is *currently* always the
    // same).
    valid &= sanitizeAttrs(elKey, el, true);

    Element replacement = el.getOwnerDocument()
        .createElementNS(elKey.ns.uri, elKey.localName);

    // Substitute in tree
    final Node parent = el.getParentNode();
    final Node position = el.getNextSibling();
    parent.removeChild(el);
    parent.insertBefore(replacement, position);

    // Move and sanitize children
    Node child;
    while ((child = el.getFirstChild()) != null) {
      replacement.appendChild(child);
      // Sanitizing is necessary because the calling sanitize() is operating on
      // the (now nonexistent) children of the old node.
      valid &= sanitize(child);
      // Note that we must sanitize as the last step in child processing, since
      // sanitize may replace child with another node as another virtualization
      // step.
    }
    
    // Move attributes
    NamedNodeMap attrs = el.getAttributes();
    for (int i = attrs.getLength(); --i >= 0;) {
      Attr attr = (Attr) attrs.item(i);
      el.removeAttributeNode(attr);
      replacement.setAttributeNodeNS(attr);
    }

    return valid;
  }
}
