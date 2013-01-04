// Copyright (C) 2012 Google Inc.
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

var reporter = window.reporter = (function() {
  var leafTable = [];
  [
    // JS standard stuff
    'Array',
    'Boolean',
    'Date',
    'Error',
    'EvalError',
    'JSON',
    'Math',
    'Number',
    'Object',
    'RangeError',
    'ReferenceError',
    'RegExp',
    'String',
    'SyntaxError',
    'TypeError',
    'URIError',

    // varies on clients
    'window.navigator',
    'window.location',
  ].forEach(function(expr) {
    leafTable.push([expr, typeof cajaVM !== 'undefined'
        ? cajaVM.compileExpr(expr)(window) : eval(expr)]);
  });

  var tamingFrameObjectProto = setTimeout.prototype  // not present on FF host
      ? Object.getPrototypeOf(setTimeout.prototype) : Object.prototype;
  var arbitraryElementSample = document.createElement('ARBITRARYELEMENT');

  // ---

  function prototypeIsInteresting(constructor) {
    var prototype = constructor.prototype;
    if (typeof prototype !== 'object' || prototype === null) {
      return true;
    }
    if (Object.getPrototypeOf(prototype) !== Object.prototype
        && Object.getPrototypeOf(prototype) !== tamingFrameObjectProto) {
      return true;
    }
    var interesting;
    return Object.getOwnPropertyNames(prototype).some(function(n) {
      return n !== 'constructor' || prototype.constructor !== constructor;
    });
  }

  /**
   * Return a first-cut list of properties of interest on the object,
   * including prototypes.
   */
  function getInterestingProperties(object) {
    var names = Object.create(null);
    function recur(o) {
      if (!o || o === Object.prototype || o === Function.prototype) {
        return;
      }
      recur(Object.getPrototypeOf(o));
      // iteration occurs second so as to override
      Object.getOwnPropertyNames(o).forEach(function (k) {
        names[k] = o;
      });
    }
    recur(object);
    var result = [];
    for (var k in names) {
      result.push([k, names[k]]);
    }
    return result.sort(function(a,b) {
      // compare by property name
      return a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0;
    });
  }
  
  function mayReadThroughAccessor(object, name, desc) {
    return (
        // work around SES patching frozen value properties into accessors
        /defProp\(this, name, \{/.exec(String(desc.set)) ||
        // interesting known-accessors
        object instanceof Node && (
          name === 'attributes' ||
          name === 'childNodes' ||
          name === 'children' ||
          name === 'rows' ||
          name === 'cells' ||
          name === 'tBodies'));
  }

  function mkel(name, inners) {
    var e = document.createElement(name);
    for (var i = 1; i < arguments.length; i++) {
      e.appendChild(mkhtml(arguments[i]));
    }
    return e;
  }
  function mkhtml(value) {
    if (value instanceof Node) {
      return value;
    } else {
      return document.createTextNode(String(value));
    }
  }

  function generateList(object, seen, explicitlyThis, options) {
    var box = document.createElement('div');
    var leafRec, cycleIndex;
    if (!((typeof object === 'object' && object !== null) ||
        typeof object === 'function')) {
      if (options.showValues) {
        try {
          box.textContent = JSON.stringify(object);
        } catch (e) {
          box.textContent = String(object);
        }
      }
    } else if ((cycleIndex = seen.indexOf(object)) !== -1) {
      box.textContent = '(cycle ' + (seen.length - cycleIndex) + ')';
    } else if (seen.length > 4) {
      box.textContent = '(too deep)';
    } else if ((leafRec = leafTable.filter(function(r) {
          return object === r[1];
        })).length > 0) {
      box.textContent = '(' + leafRec[0][0] + ')';
    //} else if (!(object instanceof Object)) {
    //  box.textContent = '(an other-frame object)';
    } else if (window.parent && window.parent !== window &&
               object instanceof window.parent.Object) {
      box.textContent = '(skipping containing frame)';
    } else if (object instanceof Node && !explicitlyThis) {
      box.textContent = '(a Node)';
    } else if (object instanceof (window.CSSStyleDeclaration ||
          function(){} /* TODO caja lacks CSSStyleDeclaration */)) {
      box.textContent = '(a CSSStyleDeclaration)';
    } else if (object instanceof Window && object !== window) {
      box.textContent = '(a Window)';
    } else {
      var props = getInterestingProperties(object);
      // hide various uninteresting properties
      props = props.filter(function(record) {
        var p = record[0];
        return (
            // standard JS properties
            !(p in Object.prototype) &&
            // vendor-prefixed properties
            !/^(webkit|WebKit|moz|Moz|ms|o|v8)(?=[A-Z])/.test(p) &&
            !/^on(webkit|moz|ms)[a-z]/.test(p) &&
            !/^initWebKit\w+Event$/.test(p) &&
            !/^(WEBKIT|MOZ|MS)_/.test(p) &&
            // numeric properties
            !/^[0-9]+$/.test(p));
      });
      if (typeof object === 'function') {
        // hide prototype if it is not interesting
        props = props.filter(function(record) {
          var p = record[0];
          if (p === 'prototype') {
            return prototypeIsInteresting(object);
          } else {
            return !(p in Function.prototype);
          }
        });
        if (options.showValues) {
          box.appendChild(mkel('span',
              '(function ', mkel('span', object.name),
              '(', mkel('span', object.length), '))'));
        } else {
          box.appendChild(mkel('span', '(function)'));
        }
      } else if (object instanceof Node &&
            object !== arbitraryElementSample) {
        // hide properties common to all elements
        props = props.filter(function(record) {
          var p = record[0];
          return !(p in arbitraryElementSample);
        });
      }

      var list = document.createElement('dl');

      props.forEach(function (record) {
        var key = record[0];
        var bearer = record[1];
        var dt = document.createElement('dt');
        var dd = document.createElement('dd');
        dt.textContent = key;
        var desc, error;
        try {
          desc = Object.getOwnPropertyDescriptor(bearer, key);
        } catch (e) {
          error = e;
        }

        if (desc && !('value' in desc) &&
            mayReadThroughAccessor(object, key, desc)) {
          desc.value = desc.get.call(object);
        }

        if (!desc) {
          if (error) {
            dd.textContent =
                '(getOwnPropertyDescriptor crashed ' + error + ')';
          } else {
            dd.textContent = '(??? desc missing)';
          }
        } else if ('value' in desc) {
          dd.appendChild(generateList(object[key], seen.concat([object]), false,
              options));
        } else {
          if (options.showValues) { dd.textContent = '(accessor)'; }
        }
        list.appendChild(dt);
        list.appendChild(dd);
      });

      if (list.childNodes.length) { box.appendChild(list); }
    }
    return box;
  }

  return {
    report: function(targetDL, options) {
      function scanRoot(name, value) {
        targetDL.appendChild(mkel('dt', name));
        targetDL.appendChild(mkel('dd', generateList(value, [], true, options)));
      }

      scanRoot('arbitrary element', arbitraryElementSample);
      [
        // TODO(kpreid): Cite source for this list
        'a','abbr','acronym','address','applet','area','article','aside',
        'audio','b','base','basefont','bdi','bdo','big','blockquote','body',
        'br','button','canvas','caption','center','cite','code','col',
        'colgroup','command','data','datalist','dd','del','details','dfn',
        'dialog','dir','div','dl','dt','em','fieldset','figcaption','figure',
        'font','footer','form','frame','frameset','h1','h2','h3','h4','h5','h6',
        'head','header','hgroup','hr','html','i','iframe','img','input','ins',
        'isindex','kbd','keygen','label','legend','li','link','map','mark',
        'menu','meta','meter','nav','nobr','noembed','noframes','noscript',
        'object','ol','optgroup','option','output','p','param','pre','progress',
        'q','s','samp','script','section','select','small','source','span',
        'strike','strong','style','sub','summary','sup','table','tbody','td',
        'textarea','tfoot','th','thead','time','title','tr','track','tt','u',
        'ul','var','video','wbr'
      ].forEach(function(name) {
        var el = document.createElement(name);
        scanRoot('<' + name + '>', el);
      })
      scanRoot('window', window);
    }
  };
}());