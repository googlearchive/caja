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
 * Runtime support for JsdocRewriter.java.  This is meant to run in rhino and
 * defines the operators added by the rewriter.
 *
 * @author mikesamuel@gmail.com
 * @namespace
 */
var jsdoc___ = (function () {
  var hasOwnProperty = ({}).hasOwnProperty;
  var lookupGetter = ({}).__lookupGetter__;

  /** Called to check {@code @return} annotations. */
  function requireFunction(apiElement, apiPath) {
    if ('function' !== typeof apiElement) {
      error('Expected a function for ' + apiPath
            + ', not ' + typeof apiElement);
    }
  }

  /** Called in response to {@code @param} annotations. */
  function requireParam(apiElement, apiPath, paramName) {
    requireFunction(apiElement, apiPath);
    var paramNames = ('' + apiElement).match(
        /^\s*function(?:\s+\w+)?\s*\(\s*((?:\w+(?:, \w+)*)?)\s*\)/);
    if (!paramNames || paramNames[1].split(/[\s,]+/g).indexOf(paramName) < 0) {
      error('Parameter ' + paramName + ' not defined on function ' + apiPath);
    }
    return paramName;
  }

  /** Called to check symbols in {@code @type} annotations. */
  function requireTypeAtoms(pos, symbolsAndNames) {
    for (var i = 0, n = symbolsAndNames.length; i < n; i += 2) {
      var symbol = symbolsAndNames[i], name = symbolsAndNames[i + 1];
      try {
        if ('function' === typeof symbol()) { continue; }
      } catch (e) {
        // A ReferenceError from trying to look up the symbol
      }
      warn(pos + ' : ' + name + ' is not a type');
    }
  }

  function document(value, doc) {
    value = wrap(value);
    attachMetaData(value, doc);
    return value;
  }

  var fileOverviews = {};
  function documentFile(fileName, overview) {
    fileOverviews[fileName] = overview;
  }

  function documentFunction(fn, doc, fields) {
    fn = document(fn, doc);
    // Attach information about the members of `this` used in the function body
    var metadata = getMetaData(fn);
    if ('function' === typeof fn) {
      metadata['@field'] = fields;
    }
    // Infer superclass info.
    if (!metadata['@extends']) {
      metadata['@extends'] = function () {
        var superClass = fn.prototype.__proto__.constructor;
        if ('function' === typeof superClass) {
          return [nameOf(superClass)];
        } else {
          return [];
        }
      };
    }
    return fn;
  }

  function structurallyEquivalent(a, b) {
    if (a === b) { return true; }
    if (typeof a !== typeof b) { return false; }
    switch (typeof a) {
      case 'number':
        return isNaN(a) && isNaN(b);
      case 'object':
        if (a === null || b === null) { return false; }
        if (!('toSource' in a) && typeof a.toSource !== 'function'
            && !('toSource' in b) && typeof b.toSource !== 'function') {
          return false;
        }
        return a.toSource() === b.toSource();
    }
  }

  function updoc(runs) {
    var htmlBuf = [];
    htmlBuf.push('<pre class="prettyprint lang-js updoc">');
    for (var i = 0, n = runs.length; i < n; ++i) {
      var run = runs[i];
      var status = 'running';
      var reason = '';
      try {
        var actual = run.input(), expected = run.result();
        if (structurallyEquivalent(actual, expected)) {
          status = 'pass';
        } else {
          status = 'fail';
          try {
            reason = '  // Was ' + uneval(actual) + ' : ' + typeNameOf(actual);
          } catch (ex) {
            reason = '  // Mismatch';
          }
        }
      } catch (ex) {
        status = 'fail';
        try {
          reason = '  // threw ' + uneval(ex) + ' : ' + typeNameOf(ex);
        } catch (ex2) {
          reason = '  // threw Error';
        }
      }
      htmlBuf.push(
          '<div class=\"', status, '\">', html(run.doc), html(reason),
          '</div>\n'
          );
      if (status !== 'pass') {
        error('updoc test at ' + run.pos + ' failed: ' + reason);
      }
    }
    htmlBuf.push('</pre>');
    return htmlBuf.join('');
  }

  function error(msg) { runtimeMessage(msg, 'ERROR'); }
  function warn(msg) { runtimeMessage(msg, 'WARNING'); }
  function runtimeMessage(msg, level) {
    jsdocPowerBox___.addMessage('RUNTIME_MESSAGE', level, '' + msg);
  }

  function beget(o) {
    function C() {}
    C.prototype = o;
    return new C;
  }

  function setNoEnumProp(obj, propertyName, value) {
    obj[propertyName] = value;
    scriptEngine___.dontEnum(obj, propertyName);
  }

  function wrap(o) {
    switch (typeof o) {
      case 'function':
        return o;
        break;
      case 'object':
        if (null !== o) {
          return o;  //!hasOwnProperty.call(o, 'doc___') ? o : beget(o);
        }
        break;
    }
    var wrapper = Object(o);
    setNoEnumProp(wrapper, 'wrapped___', o);
    return wrapper;
  }

  function unwrap(o) {
    return o && hasOwnProperty.call(o, 'wrapped___') ? o.wrapped___ : o;
  }

  function attachMetaData(obj, metadata) {
    obj.doc___ = metadata;
  }

  function getMetaData(obj) {
    if (!hasOwnProperty.call(obj, 'doc___')) {
      setNoEnumProp(obj, 'doc___', {});
    }
    return obj.doc___;
  }

  function hasGetter(o, k) {
    return !!lookupGetter.call(o, k);
  }

  function getGlobalScope() {
    return (function() { return this; })();
  }

  /**
   * Should the property k in o be listed as an API element.
   * Prototype properties are enumerated separately, so this distinguishes
   * inherited from defined properties, and filters out book-keeping properties
   * defined by the Jsdoc rewriter, and this file's functions.
   * @param {Object} o
   * @param {string} k
   */
  function shouldEnum(o, k) {
    return (k.length <= 3 || k.slice(-3) !== '___') && k.charAt(0) !== '@'
        && hasOwnProperty.call(o, k) && !hasGetter(o, k);
  }

  /**
   * Compare two object keys.  If both describe valid array indices, they are
   * compared numerically.  If both are not, they are compared lexically.
   * Array indices compare before string keys.
   */
  function keyComparator(a, b) {
    var na = +a, nb = +b;
    if (na === (a & 0x7fffffff)) {
      if (nb === (b & 0x7fffffff)) {
        a = na, b = nb;
      } else {
        return -1;
      }
    } else if (nb === (b & 0x7fffffff)) {
      return 1;
    }
    return a < b ? -1 : a === b ? 0 : 1;
  }

  function walkObjectGraph() {
    function visit(obj, docs, nameParts) {
      switch (typeof obj) {
        case 'object':
          if (obj !== null) { break; }
          // fallthru
        case 'boolean': case 'number': case 'string': case 'undefined':
          visitDocs(obj, docs, nameParts);
          return;
      }

      var metadata = getMetaData(obj);
      if (hasOwnProperty.call(metadata, 'VISITED_NAME')) {
        docs['@aliases'] = metadata.VISITED_NAME;
      } else {
        visitDocs(obj, docs, nameParts);
        visitChildren(obj, docs, nameParts);
      }
    }

    function visitChildren(obj, docs, nameParts) {
      var metadata = getMetaData(obj);
      metadata.VISITED_NAME = nameParts.join('.');
      visited.push(metadata);
      var depth = nameParts.length;
      if (depth > 20) { throw new Error(); }
      for (var k in obj) {
        if (!shouldEnum(obj, k)) { continue; }
        var v = obj[k];
        var doc = docs[k] = {};
        nameParts[depth] = k;
        visit(v, doc, nameParts);
      }
      nameParts.length = depth;
    }

    /** Copies documentation stored with a value into the doc tree. */
    function visitDocs(obj, docs, nameParts) {
      var underlying = unwrap(obj);
      var type = typeof underlying;
      switch (typeof underlying) {
        case 'object': case 'function':
          if (underlying !== null) {
            type = (function () {
                      return typeNameOf(underlying);
                    })();
          }
          break;
      }
      docs['@type'] = [type];
      switch (typeof obj) {
        case 'object':
          if (obj === null) { break; }
          // fallthru
        case 'function':
          var objdoc = getMetaData(obj);
          if (objdoc) {
            copyAnnotations(obj, objdoc, docs, nameParts);
          }
          break;
      }
    }

    var docRoot = {}, globals = getGlobalScope();
    var visited = [];  // So we can remove breadcrumbs that prevent rewalking
    var INTRINSICS = [
        'Array', 'Boolean', 'Date', 'Error', 'EvalError', 'Function',
        'Math', 'Number', 'Object', 'RangeError', 'ReferenceError',
        'RegExp', 'String', 'SyntaxError', 'TypeError', 'URIError'];
    for (var i = 0; i < INTRINSICS.length; ++i) {
      var k = INTRINSICS[i];
      if (!hasOwnProperty.call(globals, k)) { continue; }
      visit(globals[k], docRoot[k] = { '@intrinsic': [true] }, [k]);
    }
    visit(globals, docRoot, []);
    resolvePromises(docRoot, [docRoot]);
    for (var i = visited.length; --i >= 0;) { delete visited[i].VISITED_NAME; }
    return docRoot;
  }

  function bindAnnotations(self, doc, apiElementName) {
    switch (typeof doc) {
      case 'function':
        return function (docRoot) {
          return doc.call(self, docRoot, apiElementName);
        };
        break;
      case 'object':
        if (doc == null) { return null; }
        if (doc instanceof Array) {
          var bound = [];
          for (var i = doc.length; --i >= 0;) {
            bound[i] = bindAnnotations(self, doc[i], apiElementName);
          }
          return bound;
        } else {
          var bound = {};
          for (var k in doc) {
            if (!hasOwnProperty.call(doc, k)) { continue; }
            bound[k] = bindAnnotations(self, doc[k], apiElementName);
          }
          return bound;
        }
        break;
      default:
        return doc;
    }
  }

  function copyAnnotations(obj, sourceDoc, targetDocs, nameParts) {
    for (var k in sourceDoc) {
      if (!hasOwnProperty.call(sourceDoc, k)) { continue; }
      targetDocs[k] = bindAnnotations(
          obj, sourceDoc[k], nameParts.join('.'));
    }
  }

  function resolvePromises(o, args) {
    for (var k in o) {
      if (!hasOwnProperty.call(o, k)) { continue; }
      var v = o[k];
      while ('function' === typeof v) {
        try {
          v = v.apply({}, args);
        } catch (ex) {
          var message;
          if (o['@pos']) {  // TODO: pull out error handling
            message = o['@pos'] + ': ' + ex.message;
          } else {
            message = ex.message;
          }
          v = 'ERROR ' + message;
          break;
        }
      }
      if (v !== null && 'object' === typeof v) {
        resolvePromises(v, args);
      }
      o[k] = v;
    }
  }

  function resolvePromise(v, docRoot, apiElementName, apiElement) {
    while (typeof v === 'function') {
      v = v.apply(apiElement, docRoot, apiElementName);
    }
    return v;
  }

  /**
   * An object tree that includes edges only corresponding to a.  If a leaf
   * node is in both, then it is skipped, and the edge to it is skipped.
   * If all the edges from a non-leaf object are skipped, then the object, and
   * the edge to it are skipped.
   * @param a a JSON value.  An acyclic object tree.
   * @param b a JSON value like 'a'.
   * @return null if skipped.
   */
  function subtractIntersection(a, b) {
    if ('object' !== typeof a) {
      if (a === b) { return null; }
      if (a !== a && b !== b) { return null; }  // Treat NaN as equal to itself
      return a;
    }
    if ('object' !== typeof b) { return a; }
    if (a === null || b === null) { return a; }
    var diff = a instanceof Array ? [] : {};
    var hasEdges = false;
    for (var k in a) {
      if (!shouldEnum(a, k)) { continue; }
      var deltaK = subtractIntersection(
          a[k], hasOwnProperty.call(b, k) ? b[k] : null);
      if (deltaK !== null) {
        diff[k] = deltaK;
        hasEdges = true;
      }
    }
    return hasEdges ? diff : null;
  }

  var snapshot = walkObjectGraph();
  function extractDocs() {
    var docs = subtractIntersection(walkObjectGraph(), snapshot) || {};
    var fileOverviewDocs = docs['@fileoverview'] = {};
    for (var k in fileOverviews) {
      if (!hasOwnProperty.call(fileOverviews, k)) { continue; }
      var path = k.replace(/^[\/\\]+|[\/\\]+$/g, '');
      var fileNames = path.split(/[\/\\]+/g);
      var fileDocs = fileOverviewDocs;
      for (var i = 0, n = fileNames.length; i < n; ++i) {
        var fileName = fileNames[i];
        if (!hasOwnProperty.call(fileDocs, fileName)) {
          fileDocs[fileName] = {};
        }
        fileDocs = fileDocs[fileName];
      }
      copyAnnotations({}, fileOverviews[k], fileDocs, [k]);
    }
    resolvePromises(fileOverviewDocs, [docs]);
    return docs;
  }

  /**
   * Given an object or other value, format it as a JSON string.
   * This produces a canonical value, so can be used as a regression test
   * golden.
   */
  function formatJson(o) {
    var buf = [];
    formatJsonOnto(o, buf);
    return buf.join('');
  }
  function formatJsonOnto(o, buf) {
    switch (typeof o) {
      case 'object':
        if (o instanceof Array) {
          buf.push('[');
          for (var i = 0, n = o.length; i < n; ++i) {
            if (i) { buf.push(','); }
            formatJsonOnto(o[i], buf);
          }
          buf.push(']');
        } else {
          var keys = [];
          for (var k in o) {
            if (hasOwnProperty.call(o, k)) { keys.push(k); }
          }
          keys.sort(keyComparator);
          buf.push('{');
          for (var i = 0, n = keys.length; i < n; ++i) {
            if (i) { buf.push(','); }
            var k = keys[i];
            formatJsonOnto(String(k), buf);
            buf.push(':');
            formatJsonOnto(o[k], buf);
          }
          buf.push('}');
        }
        break;
      case 'string':
        buf.push(
            '"',
            o.replace(
                /[^\x20\x21\x23-\x5b\x5d-\x7e]/g,
                function (m) {
                  var codeUnit = m.charCodeAt(0);
                  return ((codeUnit < 0x100
                           ? (codeUnit < 0x10 ? '\\x0' : '\\x')
                           : (codeUnit < 0x1000 ? '\\u0' : '\\u'))
                          + codeUnit.toString(16));
                }),
            '"');
        break;
      default:
        buf.push(o);
        break;
    }
  }

  /**
   * Converts a plain text string to a string of HTML with equivalent meaning
   * that can be safely embedded in PCDATA, RCDATA, and attributes.
   */
  function html(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/\"/g, '&quot;');
  }

  function nameOf(obj) {
    var name = getMetaData(obj).VISITED_NAME;
    if (!name) {
      if ('function' === typeof obj && 'string' == typeof obj.name
          && getGlobalScope()[obj.name] === obj) {
        name = obj.name;
      }
    }
    return name ? String(name) : undefined;
  }

  function typeNameOf(obj) {
    switch (typeof obj) {
      case 'object': case 'function':
        if (obj !== null) {
          var ctor = obj.constructor;
          if ('function' === typeof ctor) {
            return nameOf(ctor) || typeof obj;
          }
        }
        break;
    }
    return typeof obj;
  }

  function linkTo(apiElement, apiElementName, linkTarget, givenName, opt_pos) {
    var targetName = nameOf(linkTarget);
    if (targetName) {
      return (
          'javascript:'
          + encodeURIComponent(
              'navigateToApiElement(' + formatJson(targetName) + ')'));
    } else {
      error((opt_pos ? opt_pos + ': ' : '') + 'Cannot resolve ' + givenName
            + ' in context of ' + apiElementName);
      return '#broken-' + encodeURIComponent(givenName);
    }
  }

  return {
    // Report errors that occur during doc generation
    error: error,
    warn: warn,
    // Assertions on API structure
    requireFunction: requireFunction,
    requireParam: requireParam,
    requireTypeAtoms: requireTypeAtoms,
    // Utilities that help associate documentation with values
    document: document,
    documentFile: documentFile,
    documentFunction: documentFunction,
    resolvePromise: resolvePromise,
    wrap: wrap,
    unwrap: unwrap,
    // Utilities called to generate values for annotations
    formatJson: formatJson,
    html: html,
    linkTo: linkTo,
    nameOf: nameOf,
    // Inline regression testing
    updoc: updoc,
    // Produces the documentation tree.
    extractDocs: extractDocs
  };
})();
