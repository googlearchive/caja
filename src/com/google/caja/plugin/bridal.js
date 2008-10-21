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
 * A set of utility functions that implement browser feature testing to unify
 * certain DOM behaviors, and a set of recommendations about when to use these
 * functions as opposed to the native DOM functions.
 *
 * @author ihab.awad@gmail.com
 */

var bridal = (function() {

  ////////////////////////////////////////////////////////////////////////////
  // Private section
  ////////////////////////////////////////////////////////////////////////////

  var features = {
    attachEvent:
        !!(document.createElement('div').attachEvent),
    setAttributeExtraParam:
        new RegExp('Internet Explorer').test(navigator.appName)
  };

  ////////////////////////////////////////////////////////////////////////////
  // Public section
  ////////////////////////////////////////////////////////////////////////////

  /**
   * Add an event listener function to an element.
   *
   * <p>Replaces
   * W3C <code>Element::addEventListener</code> and
   * IE <code>Element::attachEvent</code>.
   *
   * @param element a native DOM element.
   * @param type a string identifying the event type.
   * @param handler a function acting as an event handler.
   * @param useCapture whether the user wishes to initiate capture.
   */
  function addEventListener(element, type, handler, useCapture) {
    if (features.attachEvent) {
      // TODO(ihab.awad): How do we emulate 'useCapture' here?
      element.attachEvent('on' + type, handler);
    } else {
      element.addEventListener(
          type, handler,
          useCapture === void 0 ? void 0 : Boolean(useCapture));
    }
  }

  /**
   * Remove an event listener function from an element.
   *
   * <p>Replaces
   * W3C <code>Element::removeEventListener</code> and
   * IE <code>Element::detachEvent</code>.
   *
   * @param element a native DOM element.
   * @param type a string identifying the event type.
   * @param handler a function acting as an event handler.
   * @param useCapture whether the user wishes to initiate capture.
   */
  function removeEventListener(element, type, handler, useCapture) {
    if (features.attachEvent) {
      // TODO(ihab.awad): How do we emulate 'useCapture' here?
      element.detachEvent('on' + type, handler);
    } else {
      element.removeEventListener(type, handler, useCapture);
    }
  }

  /**
   * Create a <code>style</code> element for a document containing some
   * specified CSS text. Does not add the element to the document: the client
   * may do this separately if desired.
   *
   * <p>Replaces directly creating the <code>style</code> element and
   * populating its contents.
   *
   * @param document a DOM document.
   * @param cssText a string containing a well-formed stylesheet production.
   * @return a <code>style</code> element for the specified document.
   */
  function createStylesheet(document, cssText) {
    // Courtesy Stoyan Stefanov who documents the derivation of this at
    // http://www.phpied.com/dynamic-script-and-style-elements-in-ie/ and
    // http://yuiblog.com/blog/2007/06/07/style/
    var styleSheet = document.createElement('style');
    styleSheet.setAttribute('type', 'text/css');
    if (styleSheet.styleSheet) {   // IE
      styleSheet.styleSheet.cssText = cssText;
    } else {                // the world
      styleSheet.appendChild(document.createTextNode(cssText));
    }
    return styleSheet;
  }

  /**
   * Set an attribute on a DOM node.
   *
   * <p>Replaces DOM <code>Node::setAttribute</code>.
   *
   * @param node a DOM document.
   * @param name a string containing the name of an attribute.
   * @param value a string containing the value of an attribute.
   */
  function setAttribute(node, name, value) {
    if (name === 'style'
        && (typeof node.style.cssText) === 'string') {
      // Setting the 'style' attribute does not work for IE, but
      // setting cssText works on IE 6, Firefox, and IE 7.
      node.style.cssText = value;
    } else if (features.setAttributeExtraParam) {
      node.setAttribute(name, value, 0);
    } else {
      node.setAttribute(name, value);
    }
    return value;
  }

  return {
    addEventListener: addEventListener,
    removeEventListener: removeEventListener,
    createStylesheet: createStylesheet,
    setAttribute: setAttribute
  };
})();
