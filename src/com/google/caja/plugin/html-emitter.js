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
  if (!base) { throw new Error(); }
  this.cursor_ = [base];
}
HtmlEmitter.prototype = {
  top_: function () { return this.cursor_[this.cursor_.length - 1]; },
  doc_: function () { return this.cursor_[0].ownerDocument || document; },
  /** emits a start tag: {@code <foo}. */
  b: function (tagName, unary) {
    this.cursor_.push(this.doc_().createElement(tagName));
    // We delay appending the child until after attributes have been set
    // to avoid problems with side-effects caused by attribute values.
    // See bug 731 for details, and see HtmlEmitter.prototype.f for the
    // implementation.
    return this;
  },
  /** emits an end tag: {@code </foo>}. */
  e: function (tagName) {
    --this.cursor_.length;
    return this;
  },
  /**
   * emits an end to a start tag: {@code >} or {@code />}.
   * @param {boolean} unary true if there will be no end tag, i.e. no
   *   corresponding {@code e()} call for the closest preceding {@code b()}
   *   call.  In XML, a tag is unary if it ends with "/>", and in HTML, if the
   *   schema says so.
   */
  f: function (unary) {
    if (unary) {
      var child = this.cursor_.pop();
      this.top_().appendChild(child);
    } else {
      // This branch could be implemented in e() except for the fact that
      // everything on the stack must be reachable from base before an
      // interleaved script tag is executed.
      // Interleaved script tags can't be executed while a begin tag is open
      // so we do it on the end of the attribute list.
      var topIdx = this.cursor_.length - 1;
      this.cursor_[topIdx - 1].appendChild(this.cursor_[topIdx]);
    }
    return this;
  },
  /** emits an attribute: {@code key="value"}. */
  a: function (name, value) {
    var node = this.top_();
    // The third parameter causes IE to not treat name as case-sensitive.
    // See bug 781 for details.
    bridal.setAttribute(node, name, value);

    // Set the default state for FORM elements, so that
    // HTMLFormElement.reset() properly resets the form.  We do
    // this here instead of in the browser abstraction layer,
    // because this code should emulate a DOM being built for
    // the first time, and setting an attribute for a newly
    // created node is different from subsequent changes which
    // should not update the default value.  See bugs 850 and 855.
    switch (name) {
      case 'value':
        if ('INPUT' === node.tagName || 'TEXTAREA' == node.tagName) {
          node.defaultValue = value;
        }
        break;
      case 'checked':
        if ('INPUT' === node.tagName) {
          // http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-20509171
          node.defaultChecked = !!value;
        }
        break;
      case 'selected':
        if ('OPTION' === node.tagName) {
          node.defaultSelected = !!value;
        }
        break;
    }
    return this;
  },
  /**
   * emits an event handler given the name of a function
   * defined on the containing module's IMPORTS___ object.
   * @param {string} handlerBody a JavaScript statement that dispatches the
   *     event to a DOMita handling function.  This statement may contain
   *     free variables "event" and globals defined in supporting JS files
   *     including domita.js and cajita.js.
   */
  h: function (handlerName, handlerBody) {
    var node = this.top_();
    bridal.setAttribute(node, handlerName, handlerBody);
    // Make IE happy
    node[handlerName] = new Function('event', handlerBody);
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
