// Copyright (C) 2007 Google Inc.
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

import com.google.caja.lang.html.HTML;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.Token;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.DomTree;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Criterion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rewrites an xhtml or html dom, removing potentially unsafe constructs that
 * can be ignored, and issuing errors if the constructs cannot be removed.
 *
 * @author mikesamuel@gmail.com
 */
public final class HtmlSanitizer {
  private final MessageQueue mq;
  private final HtmlSchema schema;

  /**
   * @param schema specifies which tags and attributes are allowed, and which
   *   attribute values are allowed.
   * @param mq a message queue that will receive errors on unsafe nodes or
   *   attributes, and warnings on removed nodes.
   */
  public HtmlSanitizer(HtmlSchema schema, MessageQueue mq) {
    this.schema = schema;
    this.mq = mq;
  }

  /**
   * @param htmlRoot the node to sanitize.
   * @return true iff the htmlRoot can be safely used.  If false, explanatory
   *     messages were added to the MessageQueue passed to the constructor.
   */
  public boolean sanitize(AncestorChain<? extends DomTree> htmlRoot) {
    DomTree t = htmlRoot.node;

    boolean valid = true;
    switch (t.getType()) {
    case TAGBEGIN:
      {
        String tagName = t.getValue();
        if (!schema.isElementAllowed(tagName)) {
          PluginMessageType msgType = schema.lookupElement(tagName) != null
              ? PluginMessageType.UNSAFE_TAG
              : PluginMessageType.UNKNOWN_TAG;

          // Figure out what to do with the disallowed tag.  We can remove it
          // from the node, replace it with its children (fold), or error out.
          boolean ignore = false, fold = false;
          if (htmlRoot.parent != null
              && htmlRoot.parent.node instanceof MutableParseTreeNode) {
            if (isElementIgnorable(tagName)) {
              ignore = true;
            } else if (isElementFoldable(tagName)) {
              fold = true;
              msgType = PluginMessageType.FOLDING_ELEMENT;
            }
          }

          MessageLevel msgLevel
              = ignore || fold ? MessageLevel.WARNING : msgType.getLevel();
          mq.getMessages().add(new Message(
              msgType, msgLevel, t.getFilePosition(),
              MessagePart.Factory.valueOf(t.getValue())));

          if (ignore) {
            ((MutableParseTreeNode) htmlRoot.parent.node).removeChild(t);
            return valid;  // Don't recurse to children if removed.
          } else if (isElementFoldable(tagName)) {
            return valid & foldElement(htmlRoot.cast(DomTree.Tag.class));
          } else {
            valid = false;
          }

        }
        // Make sure that there is only one instance of an attribute
        // with a given name.  Otherwise, passes that only inspect the
        // first occurrence of an attribute could be spoofed.
        valid &= removeDuplicateAttributes((DomTree.Tag) t);
      }
      break;
    case ATTRNAME:
      DomTree.Tag tag = null;
      String tagName = "*";
      if (htmlRoot.parent != null
          && htmlRoot.parent.node instanceof DomTree.Tag) {
        tag = htmlRoot.parent.cast(DomTree.Tag.class).node;
        tagName = tag.getValue();
      }
      DomTree.Attrib attrib = (DomTree.Attrib) t;
      String attrName = attrib.getAttribName();
      HTML.Attribute a = schema.lookupAttribute(tagName, attrName);
      if (null == a ) {
        boolean savedValid = valid;
        valid = false;
        mq.getMessages().add(new Message(
            PluginMessageType.UNKNOWN_ATTRIBUTE, MessageLevel.WARNING,
            t.getFilePosition(), MessagePart.Factory.valueOf(attrName),
            MessagePart.Factory.valueOf(tagName)));
        valid = removeUnknownAttribute(tag, attrName) & savedValid;
        break;
      }
      if (!schema.isAttributeAllowed(tagName, attrName)) {
        mq.addMessage(
            PluginMessageType.UNSAFE_ATTRIBUTE,
            t.getFilePosition(), MessagePart.Factory.valueOf(attrName),
            MessagePart.Factory.valueOf(tagName));
        valid = false;
      }
      Criterion<? super String> criteria = schema.getAttributeCriteria(
          tagName, attrName);
      if (!criteria.accept(attrib.getAttribValue())) {
        mq.addMessage(
            PluginMessageType.DISALLOWED_ATTRIBUTE_VALUE,
            attrib.getAttribValueNode().getFilePosition(),
            MessagePart.Factory.valueOf(attrName),
            MessagePart.Factory.valueOf(attrib.getAttribValue()));
        valid = false;
      }
      break;
    case TEXT: case CDATA: case IGNORABLE:
    case ATTRVALUE: case COMMENT: case UNESCAPED:
      break;
    default:
      throw new AssertionError(t.getType().toString());
    }
    for (DomTree child : t.children()) {
      valid &= sanitize(new AncestorChain<DomTree>(htmlRoot, child));
    }
    return valid;
  }

  /**
   * Elements that can be safely removed from the DOM without changing behavior.
   */
  private static boolean isElementIgnorable(String tagName) {
    return "noscript".equals(tagName) || "noembed".equals(tagName)
        || "noframes".equals(tagName) || "title".equals(tagName);
  }

  /**
   * Elements that can be removed from the DOM without changing behavior as long
   * as their children are folded into the element's parent.
   * <p>
   * This list must be kept in sync with the foldable list in
   * <code>html4-defs.js</code>.
   */
  private static boolean isElementFoldable(String tagName) {
    return "head".equals(tagName) || "body".equals(tagName)
        || "html".equals(tagName);
  }

  /**
   * Fold the children of a {@link #isElementFoldable foldable} element into
   * that element's parent.
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
  private boolean foldElement(AncestorChain<DomTree.Tag> el) {
    boolean valid = true;

    // Recurse to children to ensure that all nodes are processed.
    for (DomTree child : el.node.children()) {
      valid &= sanitize(new AncestorChain<DomTree>(el, child));
    }

    // Pick the subset of children to fold in.
    List<DomTree> foldedChildren = new ArrayList<DomTree>();
    for (DomTree child : el.node.children()) {
      switch (child.getType()) {
        case ATTRNAME:  // Can't fold attributes cross element.
          mq.addMessage(
              PluginMessageType.CANNOT_FOLD_ATTRIBUTE, child.getFilePosition(),
              MessagePart.Factory.valueOf(child.getValue()),
              MessagePart.Factory.valueOf(el.node.getValue()));
          valid = false;
          break;
        case TAGBEGIN: case TEXT:
          foldedChildren.add(child);
          break;
        case UNESCAPED: case CDATA:  // Convert to a text node.
          foldedChildren.add(new DomTree.Text(
              Token.instance(child.getValue(), HtmlTokenType.TEXT,
                             child.getFilePosition())));
          break;
        default:
          // Ignore.
      }
    }

    // Rebuild the sibling list, substituting foldedChildren for any occurences
    // of el.node.
    List<? extends ParseTreeNode> originalSiblings = el.parent.node.children();

    MutableParseTreeNode.Mutation mut = el.parent.cast(
        MutableParseTreeNode.class).node.createMutation();
    for (ParseTreeNode sibling : originalSiblings) {
      mut.removeChild(sibling);
    }

    for (ParseTreeNode sibling : originalSiblings) {
      // Might appear more than once.
      if (sibling != el.node) {
        mut.appendChild(sibling);
      } else {
        mut.appendChildren(foldedChildren);
      }
    }
    mut.execute();

    return valid;
  }

  private boolean removeUnknownAttribute(DomTree.Tag el, String unknownAttr) {
    if ( null == el ) {
      return false;
    }
    MutableParseTreeNode.Mutation mut = ((MutableParseTreeNode)el).createMutation();
    for (DomTree child : el.children()) {
      if (!(child instanceof DomTree.Attrib)) { break; }
      DomTree.Attrib attr = (DomTree.Attrib) child;
      String name = attr.getAttribName();
      if (unknownAttr.equals(name)) {
        mut.removeChild(attr);
      }
    }
    mut.execute();
    return true;
  }

  private boolean removeDuplicateAttributes(DomTree.Tag el) {
    Map<String, DomTree.Attrib> byName = new HashMap<String, DomTree.Attrib>();
    boolean valid = true;
    for (DomTree child : el.children()) {
      if (!(child instanceof DomTree.Attrib)) { break; }
      DomTree.Attrib attr = (DomTree.Attrib) child;
      String name = attr.getAttribName();
      DomTree.Attrib orig = byName.get(name);
      if (orig == null) {
        byName.put(name, attr);
      } else {
        mq.addMessage(
            PluginMessageType.DUPLICATE_ATTRIBUTE, attr.getFilePosition(),
            MessagePart.Factory.valueOf(name), orig.getFilePosition());
        // Empirically, browsers use the first occurrence of an attribute.
        ((MutableParseTreeNode) el).removeChild(attr);
      }
    }
    return valid;
  }
}
