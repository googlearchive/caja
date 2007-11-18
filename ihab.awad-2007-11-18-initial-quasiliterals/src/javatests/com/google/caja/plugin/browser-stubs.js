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

/**
 * @fileoverview recreates enough of the browser context to let us run very
 * simple plugins in Rhino.
 */

var window = this;

//// Firebug Debugging stubs ////
var console = {};
// Console methods treat formatString like printf, except that all % expansions
// are treated as %s.
// See http://www.getfirebug.com/console.html for a description of the API
// this is emulating.
console.log = console.debug = console.info = console.warn = console.error =
function (printfFormatString, var_args) {
  var msg;
  try {
    var index = 0;
    var args = arguments;
    msg = printfFormatString.replace(
        /%(\w+|%)/g, function (x) { return x != '%' ? args[++index] : '%'; });
  } catch (e) {
    msg = printfFormatString;
  }
  stderr.println(msg);
};


//// Basic definitions ////
var alert = console.log;
var confirm = function (msg) {
  console.log.apply('confirm("%s")', msg);
  return false;
}
// Firefox defines nodeType constants DOM_*_NODE and Node.*_NODE as speced in
// http://www.w3.org/TR/2002/WD-DOM-Level-3-Core-20020409/ecma-script-binding.html
var DOM_ELEMENT_NODE = 1;


//// DOM Stubs ////
var stubDom_ = [];

function StubDomNode_(type, name, value) {
  this.nodeType = type;
  this.nodeName = name;
  this.nodeValue = value;
  if (type === DOM_ELEMENT_NODE) {
    this.innerHTML = '';
    this.children___ = [];
    this.className = '';
    this.id = null;
    this.style = { cssText: '' };
  } else {
    this.children___ = null;
  }
  this.firstChild = this.nextSibling = null;
}
StubDomNode_.prototype.appendChild = function (child) {
  assertTrue(child instanceof StubDomNode_);
  this.children___.push(child);
  this.recomputeChildren___();
};
/** Fix the sibling chains after the children array is modified. */
StubDomNode_.prototype.recomputeChildren___ = function () {
  var last = this.firstChild = this.children___[0];
  for (var i = 0; i < this.children___.length; ++i) {
    last = (last.nextSibling = this.children___[i]);
  }
  if (this.children___.length) {
    this.children___[this.children___.length - 1].nextSibling = null;
  }
};
StubDomNode_.prototype.setAttribute = function (name, value) {
  switch (name.toLowerCase()) {
    case 'style':
      this.style.cssText = value;
      break;
    case 'class':
      this.className = value;
      break;
    case 'id':
      this.id = value;
      break;
  }
};

/** Update the map of ids used by getElementById when the DOM is modified. */
function recomputeIds___(node) {
  if (node.id) { stubDom_[node.id] = node; }
  for (var child = node.firstChild; child; child = child.nextSibling) {
    recomputeIds___(child);
  }
}

var document = {
  getElementById: function (id) {
    if ('string' !== typeof id) { throw 'bad id ' + id; }
    var node = stubDom_[id];
    if (!node || node.id !== id) {
      stubDom_ = {};
      recomputeIds___(this.body);
      node = stubDom_[id];
    }
    if (!node) { console.log('no DOM node for id ' + id); }
    return node;
  },
  createElement: function (name) {
    return new StubDomNode_(1, name.toUpperCase(), null);
  },
  createTextNode: function (text) {
    return new StubDomNode_(3, '#text', text);
  },
  body: new StubDomNode_(1, 'BODY', null)
};


//// Stubs for {clear,set}{Interval,Timeout} ////
// The advanceTime function can be called to cause timeouts to fire
var stubCurrentTime_ = (new Date).getTime();

var stubTimeoutCtr_ = 0;
var stubTimeouts_ = {};
function setTimeout(continuation, delay) {
  if ('function' !== typeof continuation) {
    throw 'bad continuation: ' + continuation;
  }
  var id = ++stubTimeoutCtr_;
  stubTimeouts_[id] = { time: stubCurrentTime_ + delay, op: continuation };
  return id;
}
function clearTimeout(id) {
  delete stubTimeouts_[id];
}

var stubIntervalCtr_ = 0;
var stubIntervals_ = {};
function setInterval(continuation, period) {
  if ('number' !== typeof period || !(period > 0)) {
    throw 'bad period ' + period;
  }
  if ('function' !== typeof continuation) {
    throw 'bad continuation: ' + continuation;
  }
  var id = ++stubIntervalCtr_;
  stubIntervals_[id] = {
    time: stubCurrentTime_ + period, period: period, op: continuation
   };
  return id;
}
function clearInterval(id) {
  delete stubIntervals_[id];
}


function advanceTime(millis) {
  assertTrue('number' == typeof millis && millis >= 0);
  var oldTime = stubCurrentTime_;
  stubCurrentTime_ += millis;

  var ops = [];
  for (var k in stubTimeouts_) {
    var to = stubTimeouts_[k];
    if (to.time <= currentTime) {
      delete stubTimeouts_[k];
      ops.push(to);
    }
  }
  for (var k in stubIntervals_) {
    var it = stubIntervals_[k];
    while (it.time <= currentTime) {
      ops.push({ time: it.time, op: it.op });
      it.time += it.period;
    }
  }
  ops.sort(function (a, b) { return a.time - b.time; });
  for (var i = 0; i < ops.length; ++i) {
    var op = ops[i].op;
    op();
  }
}
