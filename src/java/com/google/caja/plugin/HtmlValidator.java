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

import com.google.caja.html.HTML;
import com.google.caja.html.HTML4;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.html.DomTree;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Criterion;

/**
 * Validates an xhtml dom.
 *
 * @author mikesamuel@gmail.com
 */
public final class HtmlValidator {

  private final MessageQueue mq;

  public HtmlValidator(MessageQueue mq) {
    this.mq = mq;
  }

  public boolean validate(DomTree t, ParseTreeNode parent) {
    boolean valid = true;
    switch (t.getType()) {
    case TAGBEGIN:
      // TODO(mikesamuel): make sure that there is only one instance of an
      // attribute with a given name.  Otherwise, passes that only inspect the
      // first occurrence of an attribute could be spoofed.
      {
        String tagName = t.getValue();
        tagName = tagName.toUpperCase();
        HTML.Element e = HTML4.lookupElement(tagName);
        if (null == e) {
          mq.addMessage(PluginMessageType.UNKNOWN_TAG, t.getFilePosition(),
                        MessagePart.Factory.valueOf(t.getValue()));
          valid = false;
        } else if (!HtmlWhitelist.ALLOWED_TAGS.contains(tagName)) {
          mq.addMessage(
              PluginMessageType.UNSAFE_TAG, t.getFilePosition(),
              MessagePart.Factory.valueOf(t.getValue()));
          valid = false;
        }
      }
      break;
    case ATTRNAME:
      DomTree.Attrib attrib = (DomTree.Attrib) t;
      String attrName = attrib.getAttribName();
      String ucaseAttrName = attrName.toUpperCase();
      HTML.Attribute a = HTML4.lookupAttribute(ucaseAttrName);
      if (null == a) {
        String tagName = "{unknown}";
        if (parent instanceof DomTree.Tag) {
          tagName = ((DomTree.Tag) parent).getValue();
        }
        mq.addMessage(
            PluginMessageType.UNKNOWN_ATTRIBUTE, t.getFilePosition(),
            MessagePart.Factory.valueOf(attrName),
            MessagePart.Factory.valueOf(tagName));
        valid = false;
      }
      // TODO(mikesamuel): whitelist attributes, by tag
      Criterion<String> criteria
          = HtmlWhitelist.ATTRIBUTE_CRITERIA.get(ucaseAttrName);
      if (criteria != null && !criteria.accept(attrib.getAttribValue())) {
        mq.addMessage(
            PluginMessageType.DISALLOWED_ATTRIBUTE_VALUE,
            attrib.getAttribValueNode().getFilePosition(),
            MessagePart.Factory.valueOf(attrName),
            MessagePart.Factory.valueOf(attrib.getAttribValue()));
        valid = false;
      }
      break;
    case TEXT: case CDATA: case IGNORABLE:
    case ATTRVALUE:
    case COMMENT:
      break;
    default:
      throw new AssertionError(t.getType().toString());
    }
    for (DomTree child : t.children()) {
      valid &= validate(child, t);
    }
    return valid;
  }

  static boolean isAllowedTag(String tagName) {
    return HtmlWhitelist.ALLOWED_TAGS.contains(tagName);
  }
}
