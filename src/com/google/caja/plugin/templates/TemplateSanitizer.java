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

import com.google.caja.lang.html.HTML;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.Nodes;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Criterion;

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
      case Node.DOCUMENT_FRAGMENT_NODE:
        for (Node child : Nodes.childrenOf(t)) {
          sanitize(child);
        }
        break;
      case Node.ELEMENT_NODE:
      {
        Element el = (Element) t;
        ElKey elKey = ElKey.forElement(el);
        {
          if (!schema.isElementAllowed(elKey)) {
            IhtmlMessageType msgType = schema.lookupElement(elKey) != null
                ? IhtmlMessageType.UNSAFE_TAG
                : IhtmlMessageType.UNKNOWN_TAG;

            // Figure out what to do with the disallowed tag.  We can remove it
            // from the node, replace it with its children (fold), or error out.
            boolean ignore = false, fold = false;
            Node p = el.getParentNode();
            if (p != null) {
              if (isElementIgnorable(elKey)) {
                ignore = true;
              } else if (HtmlSchema.isElementFoldable(elKey)) {
                fold = true;
                msgType = IhtmlMessageType.FOLDING_ELEMENT;
              }
            }

            MessageLevel msgLevel
                = ignore || fold ? MessageLevel.WARNING : msgType.getLevel();
            mq.getMessages().add(new Message(
                msgType, msgLevel, Nodes.getFilePositionFor(el), elKey));

            if (ignore) {
              p.removeChild(el);
              return valid;  // Don't recurse to children if removed.
            } else {
              // According to http://www.w3.org/TR/html401/appendix/notes.html
              // the recommended behavior is to try to render an unrecognized
              // element's contents
              return valid & foldElement(elKey, el);
            }
          }
          valid &= sanitizeAttrs(elKey, el);
        }
        // We know by construction of org.w3c.Element that there can only be
        // one attribute with a given name.
        // If that were not the case, passes that only inspect the
        // first occurrence of an attribute could be spoofed.
        break;
      }
      case Node.TEXT_NODE:
      case Node.CDATA_SECTION_NODE:
      case Node.COMMENT_NODE:
        break;
      default:
        throw new AssertionError(t.getNodeName());
    }
    for (Node child : Nodes.childrenOf(t)) {
      valid &= sanitize(child);
    }
    return valid;
  }

  private boolean sanitizeAttrs(ElKey elKey, Element el) {
    boolean valid = true;
    // Iterate in reverse so that removed attributes don't break iteration.
    NamedNodeMap attrs = el.getAttributes();
    for (int i = attrs.getLength(); --i >= 0;) {
      valid &= sanitizeAttr(elKey, el, (Attr) attrs.item(i));
    }
    return valid;
  }

  private boolean sanitizeAttr(ElKey elKey, Element el, Attr attrib) {
    boolean valid = true;
    AttribKey attrKey = AttribKey.forAttribute(elKey, attrib);
    HTML.Attribute a = schema.lookupAttribute(attrKey);
    if (null == a) {
      mq.getMessages().add(new Message(
          PluginMessageType.UNKNOWN_ATTRIBUTE, MessageLevel.WARNING,
          Nodes.getFilePositionFor(attrib), attrKey, elKey));
      valid &= removeBadAttribute(el, attrKey);
    } else if (!schema.isAttributeAllowed(attrKey)) {
      mq.addMessage(
          PluginMessageType.UNSAFE_ATTRIBUTE,
          Nodes.getFilePositionFor(attrib), attrKey, elKey);
      valid &= removeBadAttribute(el, attrKey);
    } else {
      Criterion<? super String> criteria = a.getValueCriterion();
      if (!criteria.accept(attrib.getNodeValue())) {
        mq.addMessage(
            PluginMessageType.DISALLOWED_ATTRIBUTE_VALUE,
            Nodes.getFilePositionForValue(attrib),
            attrKey, MessagePart.Factory.valueOf(attrib.getNodeValue()));
        valid &= removeBadAttribute(el, attrKey);
      }
    }
    return valid;
  }

  /**
   * Elements that can be safely removed from the DOM without changing behavior.
   */
  private static boolean isElementIgnorable(ElKey elKey) {
    if (!elKey.isHtml()) { return false; }
    String lcName = elKey.localName;
    return "noscript".equals(lcName) || "noembed".equals(lcName)
        || "noframes".equals(lcName) || "title".equals(lcName);
  }

  /**
   * Fold the children of a {@link HtmlSchema#isElementFoldable foldable}
   * element into that element's parent.
   *
   * <p>
   * This should have the property that:<ul>
   * <li>Every element is processed
   * <li>Elements can recursively fold
   * <li>Folded elements that are implied (such as head when a title
   *     is present) don't break cajoling.
   * <li>We don't fold elements that are explicitly allowed by the whitelist.
   * <li>Nothing is removed from the parse tree without a notification
   *     to the user.
   * </ul>
   *
   * @param el a tag with a mutable parent which will be modified in place.
   * @return true iff the el's children are transitively valid, and if they
   *     could all be folded into the parent.
   */
  private boolean foldElement(ElKey elKey, Element el) {
    boolean valid = true;

    // Recurse to children to ensure that all nodes are processed.
    valid &= sanitizeAttrs(elKey, el);
    for (Node child : Nodes.childrenOf(el)) { valid &= sanitize(child); }

    for (Attr a : Nodes.attributesOf(el)) {
      mq.addMessage(
          PluginMessageType.CANNOT_FOLD_ATTRIBUTE, Nodes.getFilePositionFor(a),
          MessagePart.Factory.valueOf(a.getNodeName()),
          MessagePart.Factory.valueOf(el.getLocalName()));
    }

    // Pick the subset of children to fold in.
    List<Node> foldedChildren = new ArrayList<Node>();
    for (Node child : Nodes.childrenOf(el)) {
      switch (child.getNodeType()) {
        case Node.ELEMENT_NODE: case Node.TEXT_NODE:
        case Node.CDATA_SECTION_NODE:
          foldedChildren.add(child);
          break;
        default:
          // Ignore.
      }
    }

    // Rebuild the sibling list, substituting foldedChildren for any occurrences
    // of el.node.
    Node next = el.getNextSibling();
    Node parent = el.getParentNode();
    parent.removeChild(el);
    for (Node n : foldedChildren) { parent.insertBefore(n, next); }

    return valid;
  }

  private boolean removeBadAttribute(Element el, AttribKey attrKey) {
    el.removeAttributeNS(attrKey.ns.uri, attrKey.localName);
    return true;
  }
}
