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

var analyzer = (function() {
  'use strict';
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

  /**
   * Clone a DOM subtree, which may be from another document or another DOM
   * implementation (e.g. Domado).
   */
  function clone(node) {
    switch (node.nodeType) {
      case Node.ELEMENT_NODE:
        var c = document.createElement(node.tagName);
        for (var child = node.firstChild;
             child !== null;
             child = child.nextSibling) {
          c.appendChild(clone(child));
        }
        return c;
      case Node.TEXT_NODE:
        return document.createTextNode(node.data);
      default:
        throw new Error('Don\'t know how to clone ' + node.nodeName + ' ' +
            node);
    }
  }

  /**
   * As a diffTree handler, return a HTML tree indicating differences with
   * <INS> and <DEL> elements. The structure should be primarily <DL> lists
   * with exactly one <DD> per <DT>.
   */
  var htmlDiffer = {
    different: function(a, b) {
      return mkel('span', mkel('del', clone(a)), mkel('ins', clone(b)));
    },
    same: function(a, b) {
      return clone(a);
    },
    diffChildren: function(a, b) {
      var result = document.createElement(a.tagName);
      var acs = a.childNodes;
      var bcs = b.childNodes;
      if (a.tagName === 'DL') {
        var am = mapify(a);
        var bm = mapify(b);
        var hop = Object.prototype.hasOwnProperty;
        var lastKey = null;
        Object.getOwnPropertyNames(am)
            .concat(Object.getOwnPropertyNames(bm)).sort()
            .forEach(function(key) {
          if (lastKey === key) { return; }
          lastKey = key;
          var ac = hop.call(am, key) ? am[key] : null;
          var bc = hop.call(bm, key) ? bm[key] : null;
          var dt = mkel('dt', document.createTextNode(key));
          if (!ac) {
            result.appendChild(mkel('ins', dt, elideDD(bc)));
          } else if (!bc) {
            result.appendChild(mkel('del', dt, elideDD(ac)));
          } else {
            result.appendChild(dt);
            result.appendChild(diffOrOmit(ac, bc));
          }
        });
      } else {
        for (var ai = 0, bi = 0;
             ai < acs.length || bi < bcs.length;
             ai++, bi++) {
          var ac = acs[ai];
          var bc = bcs[bi];
          if (!ac) {
            result.appendChild(mkel('ins', clone(bc)));
          } else if (!bc) {
            result.appendChild(mkel('del', clone(ac)));
          } else {
            result.appendChild(diffOrOmit(ac, bc));
          }
        }
      }
      return result;
    },
    join: function(first, second) {
      return mkel('span', first, second);
    }
  };

  /**
   * Internal for htmlDiffer.
   */
  function elideDD(content) {
    return content.textContent
        ? mkel('dd', document.createTextNode('(...)'))
        : clone(content);
  }

  /**
   * Internal for htmlDiffer.
   */
  function diffOrOmit(a, b) {
    if (diffTree(a, b, equalTester)) {
      // NOTE: double recursion is inefficient
      return diffTree(a, b, htmlDiffer);
    } else {
      var tc = a.textContent;
      if (false && tc.length > 10) {
        var content = a.textContent.substring(0, 10) + '...';
        if (a.nodeType === Node.ELEMENT_NODE) {
          var el = document.createElement(a.tagName);
          el.textContent = content;
          return el;
        } else {
          return document.createTextNode(content);
        }
      } else {
        return clone(a);
      }
    }
  }

  /**
   * As a diffTree handler, return whether the trees differ.
   */
  var equalTester = {
    different: function(a, b) { return true; },
    same: function(a, b) { return false; },
    diffChildren: function(a, b) {
      var result = false;
      var acs = a.childNodes;
      var bcs = b.childNodes;
      var limit = Math.max(acs.length, bcs.length);
      for (var i = 0; i < limit; i++) {
        var ac = acs[i];
        var bc = bcs[i];
        if (!ac || !bc || diffTree(ac, bc, equalTester)) {
          result = true;
          break;
        }
      }
      return result;
    },
    join: function(first, second) {
      return first || second;
    }
  };

  /**
   * Compute the difference of DOM trees a and b in the output format
   * defined by the handler.
   */
  function diffTree(a, b, handler) {
    if (a.nodeType !== b.nodeType) {
      return handler.different(a, b);
    }
    switch (a.nodeType) {
      case Node.ELEMENT_NODE:
        if (a.tagName !== b.tagName) {
          return handler.different(a, b);
        } else {
          return handler.diffChildren(a, b);
        }
      case Node.TEXT_NODE:
        if (a.data !== b.data) {
          return handler.different(a, b);
        } else {
          return handler.same(a, b);
        }
      default:
        throw new Error('Don\'t know how to diff ' + a.nodeName + ' ' + a);
    }
  }

  /**
   * Turn a <DL> into a map object.
   */
  function mapify(dl) {
    var map = {};
    for (var child = dl.firstChild; child; child = child.nextSibling) {
      while (child.nodeType !== Node.ELEMENT_NODE) {
        child = child.nextSibling;
      }
      map[child.innerText] = (child = child.nextSibling);
    }
    return map;
  }

  /**
   * From a diffed DOM tree, compute a plaintext unified diff of the <DT>
   * paths only.
   * 
   * @param diffhtml input DOM tree with INS/DEL
   * @param output array to write output strings to be concatenated into
   * @param context internal
   * @param marker internal
   */
  function propertyDiff(diffhtml, output, context, marker) {
    var moreContext = '?';
    if (diffhtml.nodeName === 'DEL') {
      marker = '- ';
    } else if (diffhtml.nodeName === 'INS') {
      marker = '+ ';
    }
    for (var c = diffhtml.firstChild; c; c = c.nextSibling) {
      if (c.nodeName === 'DT') {
        moreContext = (context ? context + '.' : '') + c.textContent;
        if (marker) {
          output.push(marker + moreContext);
        }
      } else if (c.nodeName === 'DD') {
        propertyDiff(c, output, moreContext, marker);
      } else {
        propertyDiff(c, output, context, marker);
      }
    }
  }

  return {
    diffHTML: function(domA, domB) {
      return diffTree(domA, domB, htmlDiffer);
    },
    diffToPlainTextKeysOnly: function(diffDom) {
      var pd = [];
      propertyDiff(diffDom, pd, '');
      return pd.join('\n');
    }
  };
}());