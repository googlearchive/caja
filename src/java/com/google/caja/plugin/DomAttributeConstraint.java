// Copyright (C) 2006 Google Inc.
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

import com.google.caja.parser.html.DomTree;
import com.google.caja.util.Pair;

import java.util.Collection;
import java.util.Collections;

/**
 * A constraint that must hold try for a particular tag.  This is fed attributes
 * as compiling proceeds and has the option to modify attribute values.  It is
 * also invoked at the end so that it can add values for attributes not seen.
 *
 * <p>Constraints may be stateful, so a constraint cannot be reused for multiple
 * tags.</p>
 *
 * @author mikesamuel@gmail.com
 */
interface DomAttributeConstraint {
  /** Called before any attributes are written. */
  void startTag(DomTree.Tag tag);
  /**
   * Returns the attribute prefix and suffix text, or null if the attribute
   * should not be written.
   */
  Pair<String, String> attributeValueHtml(String attribName);
  /** Called to indicate that an attribute was written. */
  void attributeDone(String attribName);
  /**
   * Called after the last attribute is written.  Returns a set of key/name
   * pairs of extra attributes to write.
   */
  Collection<Pair<String, String>> tagDone(DomTree.Tag tag);

  static final class Factory {

    static DomAttributeConstraint forTag(String tagName) {
      tagName = tagName.toUpperCase();
      // TODO(ihab): rdub disabled this condition. Am leaving it in.
      if ("FORM".equals(tagName)) {
        // We need an onsubmit handler
        return new DomAttributeConstraint() {
          boolean sawOnSubmit = false;

          public void startTag(DomTree.Tag tag) { /* noop */ }
          public Pair<String, String> attributeValueHtml(String attribName) {
            if ("ONSUBMIT".equalsIgnoreCase(attribName)) {
              return Pair.pair("try { ", " } finally { return false; }");
            }
            return Pair.pair("", "");
          }
          public void attributeDone(String attribName) {
            if ("ONSUBMIT".equalsIgnoreCase(attribName)) {
              sawOnSubmit = true;
            }
          }
          public Collection<Pair<String, String>> tagDone(DomTree.Tag tag) {
            if (!sawOnSubmit) {
              return Collections.singleton(
                  Pair.pair("onsubmit", "return false"));
            }
            return Collections.<Pair<String, String>>emptyList();
          }
        };
      } else if ("A".equals(tagName) || "AREA".equals(tagName)) {
        // We need a target if there's an href
        return new DomAttributeConstraint() {
          boolean sawHref = false;

          public void startTag(DomTree.Tag tag) { /* noop */ }
          public Pair<String, String> attributeValueHtml(String attribName) {
            if ("TARGET".equalsIgnoreCase(attribName)) { return null; }
            return Pair.pair("", "");
          }
          public void attributeDone(String attribName) {
            if ("HREF".equalsIgnoreCase(attribName)) {
              sawHref = true;
            }
          }
          public Collection<Pair<String, String>> tagDone(DomTree.Tag tag) {
            if (sawHref) {
              return Collections.singleton(Pair.pair("target", "_new"));
            }
            return Collections.<Pair<String, String>>emptyList();
          }
        };
      } else if ("SCRIPT".equals(tagName)) {
        // We disallow the src and id attributes explicity. src allows inclusion
        // of unsafe code, and id allows scripts to inject content into element.
        // TODO(ihab): Handle using HTML4, which does not allow 'id' here.
        // TODO(ihab): Review other ways SCRIPT.innerHTML may be modified.
        return new DomAttributeConstraint() {
            boolean sawSrc = false;
            boolean sawId = false;

            public void startTag(DomTree.Tag tag) { /* noop */ }
            public Pair<String, String> attributeValueHtml(String attribName) {
              if ("SRC".equalsIgnoreCase(attribName)) { return null; }
              if ("ID".equalsIgnoreCase(attribName)) { return null; }
              return Pair.pair("", "");
            }
            public void attributeDone(String attribName) {
              if ("SRC".equalsIgnoreCase(attribName)) {
                sawSrc = true;
              } else if ("ID".equalsIgnoreCase(attribName)) {
                sawId = true;
              }
            }
            public Collection<Pair<String, String>> tagDone(DomTree.Tag tag) {
              Collection<Pair<String, String>> ret = Collections.emptyList();
              if (sawSrc) {
                ret.add(Pair.pair("src", ""));
              }
              if (sawId) {
                ret.add(Pair.pair("id", ""));
              }
              return ret;
            }
          };
      } else if ("STYLE".equals(tagName)) {
        // Disallow id attribute to disallow dynamic insertion of arbitrary CSS.
        // TODO(ihab): Handle using HTML4, which does not allow 'id' here.
        return new DomAttributeConstraint() {
          boolean sawId = false;

          public void startTag(DomTree.Tag tag) { /* noop */ }
          public Pair<String, String> attributeValueHtml(String attribName) {
            if ("ID".equalsIgnoreCase(attribName)) { return null; }
            return Pair.pair("", "");
          }
          public void attributeDone(String attribName) {
            if ("ID".equalsIgnoreCase(attribName)) {
              sawId = true;
            }
          }
          public Collection<Pair<String, String>> tagDone(DomTree.Tag tag) {
            Collection<Pair<String, String>> ret = Collections.emptyList();
            if (sawId) {
              ret.add(Pair.pair("id", ""));
            }
            return ret;
          }
        };
      }
      return new DomAttributeConstraint() {
        public void startTag(DomTree.Tag tag) { /* noop */ }
        public Pair<String, String> attributeValueHtml(String attribName) {
          return Pair.pair("", "");
        }
        public void attributeDone(String attribName) { /* noop */ }
        public Collection<Pair<String, String>> tagDone(DomTree.Tag tag) {
          return Collections.<Pair<String, String>>emptyList();
        }
      };
    }

    private Factory() {
      // uninstantiable
    }
  }
}
