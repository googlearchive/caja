// timestamp: Sat, 29 Dec 2007 20:48:25

new function(_) { ////////////////////  BEGIN: CLOSURE  ////////////////////

// =========================================================================
// DOM/strict/package.js
// =========================================================================

// employ strict validation of DOM calls

eval(base2.namespace);
eval(DOM.namespace);

// =========================================================================
// DOM/strict/DocumentEvent.js
// =========================================================================

// http://www.w3.org/TR/DOM-Level-2-Events/events.html#Events-DocumentEvent

DocumentEvent.implement({
  createEvent: function(document, type) {
    assertArity(arguments);
    assert(Traversal.isDocument(document), "Invalid object.");
    return this.base(document, type);
  }
});

// =========================================================================
// DOM/strict/EventTarget.js
// =========================================================================

EventTarget.implement({
  addEventListener: _strictEventListener,

  dispatchEvent: function(target, event) {
    assertArity(arguments);
    _assertEventTarget(target);
    assert(event && event.type, "Invalid event object.", TypeError);
    return this.base(target, event);
  },

  removeEventListener: _strictEventListener
});

function _strictEventListener(target, type, listener, capture) {
  assertArity(arguments);
  _assertEventTarget(target);
  assertType(listener.handleEvent || listener, "function", "Invalid event listener.");
  assertType(capture, "boolean", "Invalid capture argument.");
  return this.base(target, type, listener, capture);
};

function _assertEventTarget(target) {
  assert(target == window || Traversal.isDocument(target) || Traversal.isElement(target), "Invalid event target.", TypeError);
};

// =========================================================================
// DOM/strict/NodeSelector.js
// =========================================================================

NodeSelector.implement({ 
  querySelector:    _strictNodeSelector,
  querySelectorAll: _strictNodeSelector
});

function _strictNodeSelector(node, selector) {
  assertArity(arguments);
  assert(Traversal.isDocument(node) || Traversal.isElement(node), "Invalid object.", TypeError);
  return this.base(node, selector);
};

}; ////////////////////  END: CLOSURE  /////////////////////////////////////
