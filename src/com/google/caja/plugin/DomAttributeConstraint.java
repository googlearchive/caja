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
import com.google.caja.util.Name;
import com.google.caja.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
  Pair<String, String> attributeValueHtml(Name attribName);
  /** Called to indicate that an attribute was written. */
  void attributeDone(Name attribName);
  /**
   * Called after the last attribute is written.  Returns a set of key/name
   * pairs of extra attributes to write.
   */
  Collection<Pair<Name, String>> tagDone(DomTree.Tag tag);

  static final class Factory {

    static DomAttributeConstraint forTag(Name tagName) {
      // TODO(ihab): rdub disabled this condition. Am leaving it in.
      if ("form".equals(tagName.getCanonicalForm())) {
        // We need an onsubmit handler
        return new DomAttributeConstraint() {
          boolean sawOnSubmit = false;

          public void startTag(DomTree.Tag tag) { /* noop */ }
          public Pair<String, String> attributeValueHtml(Name attribName) {
            if ("onsubmit".equals(attribName.getCanonicalForm())) {
              return Pair.pair("try { ", " } finally { return false; }");
            }
            return Pair.pair("", "");
          }
          public void attributeDone(Name attribName) {
            if ("onsubmit".equals(attribName.getCanonicalForm())) {
              sawOnSubmit = true;
            }
          }
          public Collection<Pair<Name, String>> tagDone(
              DomTree.Tag tag) {
            if (!sawOnSubmit) {
              return Collections.singleton(Pair.pair(
                  Name.html("onsubmit"), "return false"));
            }
            return Collections.<Pair<Name, String>>emptyList();
          }
        };
      } else if ("a".equals(tagName.getCanonicalForm()) || "area".equals(tagName.getCanonicalForm())) {
        // We need a target if there's an href
        return new DomAttributeConstraint() {
          boolean sawHref = false;

          public void startTag(DomTree.Tag tag) { /* noop */ }
          public Pair<String, String> attributeValueHtml(Name attribName) {
            if ("target".equals(attribName.getCanonicalForm())) { return null; }
            return Pair.pair("", "");
          }
          public void attributeDone(Name attribName) {
            if ("href".equals(attribName.getCanonicalForm())) {
              sawHref = true;
            }
          }
          public Collection<Pair<Name, String>> tagDone(DomTree.Tag tag) {
            if (sawHref) {
              return Collections.singleton(
                  Pair.pair(Name.html("target"), "_blank"));
            }
            return Collections.<Pair<Name, String>>emptyList();
          }
        };
      } else if ("script".equals(tagName.getCanonicalForm())) {
        // We disallow the src and id attributes explicity. src allows inclusion
        // of unsafe code, and id allows scripts to inject content into element.
        // TODO(ihab): Handle using HTML4, which does not allow 'id' here.
        // TODO(ihab): Review other ways SCRIPT.innerHTML may be modified.
        return new DomAttributeConstraint() {
            boolean sawSrc = false;
            boolean sawId = false;

            public void startTag(DomTree.Tag tag) { /* noop */ }
            public Pair<String, String> attributeValueHtml(
                Name attribName) {
              if ("src".equals(attribName.getCanonicalForm())) { return null; }
              if ("id".equals(attribName.getCanonicalForm())) { return null; }
              return Pair.pair("", "");
            }
            public void attributeDone(Name attribName) {
              if ("src".equals(attribName.getCanonicalForm())) {
                sawSrc = true;
              } else if ("id".equals(attribName.getCanonicalForm())) {
                sawId = true;
              }
            }
            public Collection<Pair<Name, String>> tagDone(DomTree.Tag tag) {
              Collection<Pair<Name, String>> ret
                  = new ArrayList<Pair<Name, String>>();
              if (sawSrc) {
                ret.add(Pair.pair(Name.html("src"), ""));
              }
              if (sawId) {
                ret.add(Pair.pair(Name.html("id"), ""));
              }
              return ret;
            }
          };
      } else if ("style".equals(tagName.getCanonicalForm())) {
        // Disallow id attribute to disallow dynamic insertion of arbitrary CSS.
        // TODO(ihab): Handle using HTML4, which does not allow 'id' here.
        return new DomAttributeConstraint() {
          boolean sawId = false;

          public void startTag(DomTree.Tag tag) { /* noop */ }
          public Pair<String, String> attributeValueHtml(Name attribName) {
            if ("id".equals(attribName.getCanonicalForm())) { return null; }
            return Pair.pair("", "");
          }
          public void attributeDone(Name attribName) {
            if ("id".equals(attribName.getCanonicalForm())) {
              sawId = true;
            }
          }
          public Collection<Pair<Name, String>> tagDone(DomTree.Tag tag) {
            List<Pair<Name, String>> ret = new ArrayList<Pair<Name, String>>();
            if (sawId) {
              ret.add(Pair.pair(Name.html("id"), ""));
            }
            return ret;
          }
        };
      }
      return new DomAttributeConstraint() {
        public void startTag(DomTree.Tag tag) { /* noop */ }
        public Pair<String, String> attributeValueHtml(Name attribName) {
          return Pair.pair("", "");
        }
        public void attributeDone(Name attribName) { /* noop */ }
        public Collection<Pair<Name, String>> tagDone(DomTree.Tag tag) {
          return Collections.<Pair<Name, String>>emptyList();
        }
      };
    }

    private Factory() {
      // uninstantiable
    }
  }
}
