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
 * A partially tamed browser object model.
 *
 * @author mikesamuel@gmail.com
 */


(function () {
  var currentGadgetRoot = null;
  var htmlBuf = null;

  function setGadgetRoot(root) {
    currentGadgetRoot = root;
    htmlBuf = [];
  }

  function closeGadgetRoot() {
    console.log('closing ' + htmlBuf + ' for ' + currentGadgetRoot.id);
    currentGadgetRoot.innerHTML = htmlBuf.join('');
    htmlBuf = currentGadgetRoot = null;
  }

  function htmlEmitter() {
    var buffer = htmlBuf;
    return function (var_args) {
      for (var i = 0, n = arguments.length; i < n; ++i) {
        buffer.push(arguments[i]);
      }
    };
  }

  this.setGadgetRoot = setGadgetRoot;
  this.closeGadgetRoot = closeGadgetRoot;
  this.htmlEmitter = htmlEmitter;
})();

var tameNodeSecret = {};

function attachDocumentStubs(domPrefix, outers) {

  function tameNode(node) {
    if (node == null) { return node; }
    var tamed;
    switch (node.tagName.toUpperCase()) {
      case 'FORM':
        tamed = new TameFormNode(node);
        break;
      case 'INPUT':
        tamed = new TameInputNode(node);
        break;
      default:
        tamed = new TameNode(node);
        break;
    }
    return tamed;
  }

  function TameNode(node) {
    this.node_ = node;
  }
  TameNode.prototype.secret___ = tameNodeSecret;
  TameNode.prototype.getId = function () {
    var id = this.node_.id;
    return id != null ? domPrefix + id : id;
  };
  TameNode.prototype.setInnerHTML = function (html) {
    this.node_.innerHTML = safeHtml(html);
  };
  TameNode.prototype.appendChild = function (child) {
    if (child.secret___ !== tameNodeSecret) { throw new Error(); }
    this.node_.appendChild(child.node_);
  };
  TameNode.prototype.removeChild = function (child) {
    if (child.secret___ !== tameNodeSecret) { throw new Error(); }
    this.node_.removeChild(child.node_);
  };
  TameNode.prototype.getFirstChild = function () {
    return tameNode(this.node_.firstChild);
  };
  TameNode.prototype.getLastChild = function () {
    return tameNode(this.node_.lastChild);
  };
  TameNode.prototype.getNextSibling = function () {
    return tameNode(this.node_.nextSibling);
  };
  TameNode.prototype.getPrevSibling = function () {
    return tameNode(this.node_.prevSibling);
  };
  TameNode.prototype.addEventListener = function (name, listener, bubble) {
    if (this.node_.addEventListener) {
      this.node_.addEventListener(
          name,
          function (event) {
            var thisNode = tameNode(this);
            if (!thisNode) { throw new Error(); }
            return listener.call(
                thisNode, new TameEvent(event || window.event));
          }, bubble);
    } else {
      var thisNode = this;
      this.node_.attachEvent(
          'on' + name,
          function (event) {
            return listener.call(
                thisNode, new TameEvent(event || window.event));
          });
    }
  };
  TameNode.prototype.toString = function () {
    return '<' + this.node_.tagName + '>';
  };
  ___.ctor(TameNode, undefined, 'TameNode');
  ___.all2(___.allowMethod, TameNode, ['addEventListener', 'appendChild',
                                       'getFirstChild', 'getId', 'getLastChild',
                                       'getNextSibling', 'getPrevSibling',
                                       'removeChild', 'setInnerHTML']);

  function TameFormNode(node) {
    TameNode.call(this, node);
  }
  TameFormNode.prototype = new TameNode();
  TameFormNode.prototype.constructor = TameFormNode;
  TameFormNode.prototype.getElements = function () {
    var realEls = this.node_.elements;
    var tameEls = [];
    for (var i = realEls.length; --i >= 0;) {
      var el = realEls[i];
      var tameEl = tameNode(el);
      tameEls[i] = tameEl;
      if (el.name) { tameEls[el.name] = tameEl; }
    }
    return tameEls;
  };
  ___.ctor(TameFormNode, TameNode, 'TameFormNode');
  ___.all2(___.allowMethod, TameFormNode, ['getElements']);

  function TameInputNode(node) {
    TameNode.call(this, node);
  }
  TameInputNode.prototype = new TameNode();
  TameInputNode.prototype.constructor = TameInputNode;
  TameInputNode.prototype.getValue = function () {
    var value = this.node_.value;
    return value == null ? value : String(value);
  };
  TameInputNode.prototype.focus = function () {
    this.node_.focus();
  };
  TameInputNode.prototype.getForm = function () {
    return tameNode(this.node_.form);
  };
  ___.ctor(TameInputNode, TameNode, 'TameInputNode');
  ___.all2(___.allowMethod, TameInputNode, ['getValue', 'focus', 'getForm']);

  function TameEvent(event) {
    this.event_ = event;
  }
  TameEvent.prototype.toString = function () { return 'Not a real event'; };

  function TameDocument() {
  }
  TameDocument.prototype.createElement = function (tagName) {
    return tameNode(document.createElement(tagName));
  };
  TameDocument.prototype.getElementById = function (id) {
    id = domPrefix + id;
    var node = document.getElementById(id);
    return tameNode(node);
  };
  ___.ctor(TameDocument, undefined, 'TameDocument');
  ___.all2(___.allowMethod, TameDocument, ['createElement', 'getElementById']);

  outers.tameNode___ = tameNode;
  outers.tameEvent___ = function (event) { return new TameEvent(event); };
  outers.document = new TameDocument();
  outers.StringInterpolation = StringInterpolation;

  // HACK: provide logging to gadgets
  ___.simpleFunc(
      outers.log = function () {
            arguments[0] = 'gadget:' + arguments[0];
            console.log.apply(console, arguments);
          });
}

function plugin_dispatchEvent___(thisNode, event, pluginId, handlerName) {
  console.log('thisNode=%o, event=%o, pluginId=%o, handlerName=%o',
              thisNode, event, pluginId, handlerName);
  var outers = ___.getOuters(pluginId);
  return outers[handlerName](
      outers.tameNode___(thisNode), outers.tameEvent___(event));
}

___.ctor(StringInterpolation, undefined, 'StringInterpolation');
