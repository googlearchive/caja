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

/**
 * @fileoverview
 * Javascript support for DomProcessingEvents.java
 *
 * @author mikesamuel@gmail.com
 */


function HtmlEmitter(base) {
  this.cursor_ = [base];
}
HtmlEmitter.prototype = {
  top_: function () { return this.cursor_[this.cursor_.length - 1]; },
  doc_: function () { return this.cursor_[0].ownerDocument || document; },
  /** emits a start tag: {@code <foo}. */
  b: function (tagName, unary) {
    var el = this.doc_().createElement(tagName);
    this.top_().appendChild(el);
    this.cursor_.push(el);
    return this;
  },
  /** emits an end tag: {@code </foo>}. */
  e: function (tagName) {
    --this.cursor_.length;
    return this;
  },
  /** emits an end to a start tag: {@code >} or {@code />}. */
  f: function (unary) {
    if (unary) { --this.cursor_.length; }
    return this;
  },
  /** emits an attribute: {@code key="value"}. */
  a: function (name, value) {
    this.top_().setAttribute(name, value);
    return this;
  },
  /** emits PCDATA text. */
  pc: function (text) {
    this.top_().appendChild(this.doc_().createTextNode(text));
    return this;
  },
  /** emits CDATA text. */
  cd: function (text) {
    this.top_().appendChild(this.doc_().createTextNode(text));
    return this;
  },
  /** emits a chunk of preprocessed HTML. */
  ih: function (html) {
    var top = this.top_();
    if (top.firstChild) {
      // If top already contains children, we can't just += to top.innerHTML
      // since that would invalidate existing node references.
      var container = this.doc_().createElement(top.nodeName);
      container.innerHTML = html;
      while (container.firstChild) {
        top.appendChild(container.firstChild);
      }
    } else {
      top.innerHTML = html;
    }
    return this;
  }
};
