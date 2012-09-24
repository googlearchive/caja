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
 * JavaScript support for TemplateCompiler.java and for a tamed version of
 * <code>document.write{,ln}</code>.
 * <p>
 * This handles the problem of making sure that only the bits of a Gadget's
 * static HTML which should be visible to a script are visible, and provides
 * mechanisms to reliably find elements using dynamically generated unique IDs
 * in the face of DOM modifications by untrusted scripts.
 *
 * @author mikesamuel@gmail.com
 * @author jasvir@gmail.com
 * @provides HtmlEmitter
 * @overrides window
 * @requires bridalMaker html html4 cajaVM sanitizeStylesheet URI Q
 * @overrides window
 */

// The Turkish i seems to be a non-issue, but abort in case it is.
if ('I'.toLowerCase() !== 'i') { throw 'I/i problem'; }

/**
 * @param {function} makeDOMAccessible A function which will be called on base
 *     and every object retrieved from it, recursively. This hook is available
 *     in case HtmlEmitter is running in an environment such that unmodified DOM
 *     objects cannot be touched. makeDOMAccessible should be idempotent. Note
 *     that the contract here is stronger than for bridalMaker, in that
 *     this makeDOMAccessible may not return a different object.
 *     Except, this contract may be impossible to satisfy on IE<=8.
 *     TODO(felix8a): check all the implications of violating the contract.
 * @param base a node that is the ancestor of all statically generated HTML.
 * @param opt_domicile the domado instance that will receive a load event when
 *     the html-emitter is closed, and which will have the {@code writeHook}
 *     property set to the HtmlEmitter's document.write implementation.
 * @param opt_guestGlobal the object in the guest frame that is the global scope
 *     for guest code.
 */
function HtmlEmitter(makeDOMAccessible, base, opt_domicile, opt_guestGlobal) {
  if (!base) {
    throw new Error(
        'Host page error: Virtual document element was not provided');
  }
  base = makeDOMAccessible(base);
  var insertionPoint = base;
  var bridal = bridalMaker(makeDOMAccessible, base.ownerDocument);

  // TODO: Take into account <base> elements.

  /**
   * Contiguous pairs of ex-descendants of base, and their ex-parent.
   * The detached elements (even indices) are ordered depth-first.
   */
  var detached = null;
  /** Makes sure IDs are accessible within removed detached nodes. */
  var idMap = null;
  
  /** Hook from attach/detach to document.write logic. */
  var updateInsertionMode;

  var arraySplice = Array.prototype.splice;
  
  var HTML5_WHITESPACE_RE = /^[\u0009\u000a\u000c\u000d\u0020]*$/;

  function buildIdMap() {
    idMap = {};
    var descs = base.getElementsByTagName('*');
    for (var i = 0, desc; (desc = descs[i]); ++i) {
      desc = makeDOMAccessible(desc);
      var id = desc.getAttributeNode('id');
      id = makeDOMAccessible(id);
      // The key is decorated to avoid name conflicts and restrictions.
      if (id && id.value) { idMap[id.value + " map entry"] = desc; }
    }
  }
  /**
   * Returns the element with the given ID under the base node.
   * @param id an auto-generated ID since we cannot rely on user supplied IDs
   *     to be unique.
   * @return {Element|null} null if no such element exists.
   */
  function byId(id) {
    if (!idMap) { buildIdMap(); }
    var node = idMap[id + " map entry"];
    if (node) { return node; }
    for (; (node = base.ownerDocument.getElementById(id));) {
      if (base.contains
          ? base.contains(node)
          : (base.compareDocumentPosition(node) & 0x10)) {
        idMap[id + " map entry"] = node;
        return node;
      } else {
        node.id = '';
      }
    }
    return null;
  }

  /**
   * emitStatic allows the caller to inject the static HTML from JavaScript,
   * if the gadget host page's usage pattern requires it.
   */
  function emitStatic(htmlString) {
    if (!base) {
      throw new Error('Host page error: HtmlEmitter.emitStatic called after' +
          ' document finish()ed');
    }
    base.innerHTML += htmlString;
  }

  // Below we define the attach, detach, and finish operations.
  // These obey the conventions that:
  //   (1) All detached nodes, along with their ex-parents are in detached,
  //       and they are ordered depth-first.
  //   (2) When a node is specified by an ID, after the operation is performed,
  //       it is in the tree.
  //   (3) Each node is attached to the same parent regardless of what the
  //       script does.  Even if a node is removed from the DOM by a script,
  //       any of its children that appear after the script, will be added.
  // As an example, consider this HTML which has the end-tags removed since
  // they don't correspond to actual nodes.
  //   <table>
  //     <script>
  //     <tr>
  //       <td>Foo<script>Bar
  //       <th>Baz
  //   <script>
  //   <p>The-End
  // There are two script elements, and we need to make sure that each only
  // sees the bits of the DOM that it is supposed to be aware of.
  //
  // To make sure that things work when javascript is off, we emit the whole
  // HTML tree, and then detach everything that shouldn't be present.
  // We represent the removed bits as pairs of (removedNode, parentItWasPartOf).
  // Including both makes us robust against changes scripts make to the DOM.
  // In this case, the detach operation results in the tree
  //   <table>
  // and the detached list
  //   [<tr><td>FooBar<th>Baz in <table>, <p>The-End in (base)]

  // After the first script executes, we reattach the bits needed by the second
  // script, which gives us the DOM
  //   <table><tr><td>Foo
  // and the detached list
  //   ['Bar' in <td>, <th>Baz in <tr>, <p>The-End in (base)]
  // Note that we did not simply remove items from the old detached list.  Since
  // the second script was deeper than the first, we had to add only a portion
  // of the <tr>'s content which required doing a separate mini-detach operation
  // and push its operation on to the front of the detached list.

  // After the second script executes, we reattach the bits needed by the third
  // script, which gives us the DOM
  //   <table><tr><td>FooBar<th>Baz
  // and the detached list
  //   [<p>The-End in (base)]

  // After the third script executes, we reattached the rest of the detached
  // nodes, and we're done.

  // To perform a detach or reattach operation, we impose a depth-first ordering
  // on HTML start tags, and text nodes:
  //   [0: <table>, 1: <tr>, 2: <td>, 3: 'Foo', 4: 'Bar', 5: <th>, 6: 'Baz',
  //    7: <p>, 8: 'The-End']
  // Then the detach operation simply removes the minimal number of nodes from
  // the DOM to make sure that only a prefix of those nodes are present.
  // In the case above, we are detaching everything after item 0.
  // Then the reattach operation advances the number.  In the example above, we
  // advance the index from 0 to 3, and then from 3 to 6.
  // The finish operation simply reattaches the rest, advancing the counter from
  // 6 to the end.

  // The minimal detached list from the node with DFS index I is the ordered
  // list such that a (node, parent) pair (N, P) is on the list if
  // dfs-index(N) > I and there is no pair (P, GP) on the list.

  // To calculate the minimal detached list given a node representing a point in
  // that ordering, we rely on the following observations:
  //    The minimal detached list after a node, is the concatenation of
  //    (1) that node's children in order
  //    (2) the next sibling of that node and its later siblings,
  //        the next sibling of that node's parent and its later siblings,
  //        the next sibling of that node's grandparent and its later siblings,
  //        etc., until base is reached.

  function detachOnto(limit, out) {
    // Set detached to be the minimal set of nodes that have to be removed
    // to make sure that limit is the last attached node in DFS order as
    // specified above.

    // First, store all the children.
    for (var child = limit.firstChild, next; child; child = next) {
      child = makeDOMAccessible(child);
      next = child.nextSibling;  // removeChild kills nextSibling.
      out.push(child, limit);
      limit.removeChild(child);
    }

    // Second, store your ancestor's next siblings and recurse.
    for (var anc = limit, greatAnc; anc && anc !== base; anc = greatAnc) {
      greatAnc = anc.parentNode;
      greatAnc = makeDOMAccessible(greatAnc);
      for (var sibling = anc.nextSibling, next; sibling; sibling = next) {
        sibling = makeDOMAccessible(sibling);
        next = sibling.nextSibling;
        out.push(sibling, greatAnc);
        greatAnc.removeChild(sibling);
      }
    }
  }
  /**
   * Make sure that everything up to and including the node with the given ID
   * is attached, and that nothing that follows the node is attached.
   */
  function attach(id) {
    var limit = byId(id);
    if (detached) {
      // Build an array of arguments to splice so we can replace the reattached
      // nodes with the nodes detached from limit.
      var newDetached = [0, 0];
      // Since limit has no parent, detachOnto will bottom out at its sibling.
      detachOnto(limit, newDetached);
      // Find the node containing limit that appears on detached.
      var limitAnc = limit;
      for (var parent; (parent = limitAnc.parentNode);) {
        limitAnc = parent;
      }
      // Reattach up to and including limit ancestor.
      // If some browser quirk causes us to miss limit in detached, we'll
      // reattach everything and try to continue.
      var nConsumed = 0;
      while (nConsumed < detached.length) {
        // in IE, some types of nodes can't be standalone, and detaching
        // one will create new parentNodes for them.  so at this point,
        // limitAnc might be an ancestor of the node on detached.
        var reattach = detached[nConsumed];
        var reattAnc = reattach;
        for (; reattAnc.parentNode; reattAnc = reattAnc.parentNode) {}
        (detached[nConsumed + 1] /* the parent */).appendChild(reattach);
        nConsumed += 2;
        if (reattAnc === limitAnc) { break; }
      }
      // Replace the reattached bits with the ones detached from limit.
      newDetached[1] = nConsumed;  // splice's second arg is the number removed
      arraySplice.apply(detached, newDetached);
    } else {
      // The first time attach is called, the limit is actually part of the DOM.
      // There's no point removing anything when all scripts are deferred.
      detached = [];
      detachOnto(limit, detached);
    }
    // Keep track of the insertion point for document.write.
    // The tag was closed if there is no child waiting to be added.
    // FIXME(mikesamuel): This is not technically correct, since the script
    // element could have been the only child.
    var isLimitClosed = detached[1] !== limit;
    insertionPoint = isLimitClosed ? limit.parentNode : limit;
    updateInsertionMode();
    return limit;
  }
  /**
   * Removes a script place-holder.
   * When a text node immediately precedes a script block, the limit will be
   * a text node.  Text nodes can't be addressed by ID, so the TemplateCompiler
   * follows them with a {@code <span>} which must be removed to be semantics
   * preserving.
   */
  function discard(placeholder) {
    // An untrusted script block should not be able to access the wrapper before
    // it's removed since it won't be part of the DOM so there should be a
    // parentNode.
    placeholder.parentNode.removeChild(placeholder);
  }
  /**
   * Reattach any remaining detached bits, free resources.
   */
  function finish() {
    insertionPoint = null;
    if (detached) {
      for (var i = 0, n = detached.length; i < n; i += 2) {
        detached[i + 1].appendChild(detached[i]);
      }
    }
    // Release references so nodes can be garbage collected.
    idMap = detached = base = null;
    return this;
  }

  function signalLoaded() {
    // Signals the close of the document and fires any window.onload event
    // handlers.
    var domicile = opt_domicile;
    if (domicile) { domicile.signalLoaded(); }
    return this;
  }

  function handleEmbed(params) {
    if (!opt_guestGlobal) { return; }
    if (!opt_guestGlobal.cajaHandleEmbed) { return; }
    opt_guestGlobal.cajaHandleEmbed(params);
  }

  this.byId = byId;
  this.attach = attach;
  this.discard = discard;
  this.emitStatic = emitStatic;
  this.finish = finish;
  this.signalLoaded = signalLoaded;
  this.setAttr = bridal.setAttribute;
  this.rmAttr = function(el, attr) { return el.removeAttribute(attr); };
  this.handleEmbed = handleEmbed;

  (function (domicile) {
    if (!domicile || domicile.writeHook) {
      updateInsertionMode = function () {};
      return;
    }

    function concat(items) {
      return Array.prototype.join.call(items, '');
    }

    function evaluateUntrustedScript(scriptBaseUri, scriptInnerText) {
      if (!opt_guestGlobal) { return; }
      var errorMessage = "SCRIPT element evaluation failed";

      var cajaVM = opt_guestGlobal.cajaVM;
      if (cajaVM) {
        var compileModule = cajaVM.compileModule;
        if (compileModule) {
          try {
            var compiledModule = compileModule(scriptInnerText);
            try {
              compiledModule(opt_domicile.window);
              return;  // Do not trigger onerror below.
            } catch (runningEx) {
              errorMessage = String(runningEx);
            }
          } catch (compileEx) {
            errorMessage =
              String(compileEx && (compileEx.message || compileEx.description))
                || errorMessage;
          }
        }
      }

      // Dispatch to the onerror handler.
      try {
        // TODO: Should this happen inline or be dispatched out of band?
        opt_domicile.window.onerror(
            errorMessage,
            // URL where error was raised.
            // TODO: Is this leaking?  Do we need to maintain an illusion here?
            opt_guestGlobal ? opt_guestGlobal.location.href : '',
            1  // Line number where error was raised.
            );
      } catch (_) {
        // Ignore problems dispatching error.
      }
    }

    function makeCssUriSanitizer(baseUri) {
      return function(uri, prop) {
          return (domicile && domicile.cssUri)
              ? domicile.cssUri(URI.utils.resolve(baseUri, uri), 'image/*', prop)
              : void 0;
      };
    }

    function defineUntrustedStylesheet(styleBaseUri, cssText) {
      if (domicile && domicile.emitCss) {
        domicile.emitCss(sanitizeStylesheet(styleBaseUri,
            cssText, domicile.suffixStr.replace(/^-/, ''), 
            makeCssUriSanitizer(styleBaseUri)));
      }
    }

    function resolveUntrustedExternal(func, url, mime, marker, continuation) {
      if (domicile && domicile.fetchUri) {
        domicile.fetchUri(url, mime,
          function (result) {
            if (result && result.html) {
              func(url, result.html);
            } else {
              // TODO(jasvir): Thread logger and log the failure to fetch
            }
            if (continuation) {
              continuation();
            }
          });
        if (marker) {
          throw marker;
        }
      }
    }

    function defineUntrustedExternalStylesheet(url, marker, continuation) {
      resolveUntrustedExternal(
        defineUntrustedStylesheet, url, 'text/css', marker, continuation);
    }

    function evaluateUntrustedExternalScript(url, marker, continuation) {
      resolveUntrustedExternal(
        evaluateUntrustedScript, url, 'text/javascript', marker, continuation);
    }

    function lookupAttr(attribs, attr) {
      var srcIndex = 0;
      do {
        srcIndex = attribs.indexOf(attr, srcIndex) + 1;
      } while (srcIndex && !(srcIndex & 1));
      return srcIndex ? attribs[srcIndex] : undefined;
    }

    function resolveUriRelativeToDocument(href) {
      if (domicile && domicile.pseudoLocation && domicile.pseudoLocation.href) {
        return URI.utils.resolve(domicile.pseudoLocation.href, href);
      }
      return href;
    }

    // Zero or one of the html4.eflags constants that captures the content type
    // of cdataContent.
    var cdataContentType = 0;
    // Chunks of CDATA content of the type above which need to be specially
    // processed and interpreted.
    var cdataContent = [];
    var pendingExternal = undefined;
    var documentLoaded = undefined;
    var depth = 0;

    function normalInsert(virtualTagName, attribs) {
      var realTagName = html.virtualToRealElementName(virtualTagName);

      // Extract attributes which we need to invoke side-effects on rather
      // than just sanitization; currently <body> event handlers.
      var slowPathAttribs = [];
      if (opt_domicile && virtualTagName === 'body') {
        for (var i = attribs.length - 2; i >= 0; i -= 2) {
          if (/^on/i.test(attribs[i])) {
            slowPathAttribs.push.apply(slowPathAttribs, attribs.splice(i, 2));
          }
        }
      }

      var eltype = html4.ELEMENTS[realTagName];
      domicile.sanitizeAttrs(realTagName, attribs);

      if ((eltype & html4.eflags.UNSAFE) !== 0) {
        // TODO(kpreid): Shouldn't happen (for now)
        return;
      }

      var el = bridal.createElement(realTagName, attribs);
      if ((eltype & html4.eflags.OPTIONAL_ENDTAG)
          && el.tagName === insertionPoint.tagName) {
        documentWriter.endTag(el.tagName, true);
        // TODO(kpreid): Replace this with HTML5 parsing model
      }
      insertionPoint.appendChild(el);
      if (!(eltype & html4.eflags.EMPTY)) { insertionPoint = el; }
      
      for (var i = slowPathAttribs.length - 2; i >= 0; i -= 2) {
        opt_domicile.tameNode(el, true).setAttribute(
          slowPathAttribs[i], slowPathAttribs[i+1]);
      }
    }

    function normalEndTag(tagName) {
      tagName = html.virtualToRealElementName(tagName).toUpperCase();

      var anc = insertionPoint;
      while (anc !== base && !/\bvdoc-container___\b/.test(anc.className)) {
        var p = anc.parentNode;
        if (anc.tagName === tagName) {
          insertionPoint = p;
          return;
        }
        anc = p;
      }
    }

    function normalText(text) {
      insertionPoint.appendChild(insertionPoint.ownerDocument.createTextNode(
          html.unescapeEntities(text)));
    }

    // Per HTML5 spec
    function isHtml5NonWhitespace(text) {
      return !HTML5_WHITESPACE_RE.test(text);
    }
    var insertionModes = {
      initial: {
        toString: function () { return "initial"; },
        startTag: function (tagName, attribs) {
          insertionMode = insertionModes.beforeHtml;
          insertionMode.startTag.apply(undefined, arguments);
        },
        endTag: function (tagName) {
          insertionMode = insertionModes.beforeHtml;
          insertionMode.endTag.apply(undefined, arguments);
        },
        text: function (text) {
          if (isHtml5NonWhitespace(text)) {
            insertionMode = insertionModes.beforeHtml;
            insertionMode.text.apply(undefined, arguments);
          }
        }
      },
      beforeHtml: {
        toString: function () { return "before html"; },
        startTag: function (tagName, attribs) {
          if (tagName === 'html') {
            normalInsert(tagName, attribs);
            insertionMode = insertionModes.beforeHead;
          } else {
            normalInsert('html', []);
            insertionMode = insertionModes.beforeHead;
            insertionMode.startTag.apply(undefined, arguments);
          }
        },
        endTag: function (tagName) {
          if (tagName === 'head' || tagName === 'body' || tagName === 'html' ||
              tagName === 'br') {
            normalInsert('html', []);
            insertionMode = insertionModes.beforeHead;
            insertionMode.endTag.apply(undefined, arguments);
          } else {
            // "Parse error. Ignore the token."
          }
        },
        text: function (text) {
          if (isHtml5NonWhitespace(text)) {
            normalInsert('html', []);
            insertionMode = insertionModes.beforeHead;
            insertionMode.text.apply(undefined, arguments);
          }
        }
      },
      beforeHead: {
        toString: function () { return "before head"; },
        startTag: function (tagName, attribs) {
          if (tagName === 'html') {
            insertionModes.inBody.startTag.apply(undefined, arguments);
          } else if (tagName === 'head') {
            normalInsert(tagName, attribs);
            insertionMode = insertionModes.inHead;
          } else {
            insertionMode.startTag('head', []);
            insertionMode.startTag.apply(undefined, arguments);
          }
        },
        endTag: function (tagName) {
          if (tagName === 'head' || tagName === 'body' || tagName === 'html' ||
              tagName === 'br') {
            insertionMode.startTag('head', []);
            insertionMode.endTag.apply(undefined, arguments);
          } else {
            // "Parse error. Ignore the token."
          }
        },
        text: function (text) {
          if (isHtml5NonWhitespace(text)) {
            insertionMode.startTag('head', []);
            insertionMode.text.apply(undefined, arguments);
          }
        }
      },
      inHead: {
        toString: function () { return "in head"; },
        startTag: function (tagName, attribs) {
          if (tagName === 'html') {
            insertionModes.inBody.startTag.apply(undefined, arguments);
          } else if (tagName === 'base' || tagName === 'basefont' ||
              tagName === 'bgsound'  || tagName === 'command' ||
              tagName === 'link'     || tagName === 'meta' ||
              tagName === 'noscript' || tagName === 'noframes' ||
              tagName === 'style'    || tagName === 'script') {
            normalInsert(tagName, attribs);
          } else if (tagName === 'title') {
            normalInsert(tagName, attribs);
            originalInsertionMode = insertionMode;
            insertionMode = insertionModes.text;
          } else if (tagName === 'head') {
            // "Parse error. Ignore the token."
          } else {
            insertionMode.endTag('head');
            insertionMode.startTag.apply(undefined, arguments);
          }
        },
        endTag: function (tagName) {
          if (tagName === 'head') {
            insertionPoint = insertionPoint.parentElement;
            insertionMode = insertionModes.afterHead;
          } else if (tagName === 'body' || tagName === 'html' ||
              tagName === 'br') {
            insertionMode.endTag('head');
            insertionMode.endTag.apply(undefined, arguments);
          } else {
            // "Parse error. Ignore the token."
          }
        },
        text: function (text) {
          if (isHtml5NonWhitespace(text)) {
            insertionMode.endTag('head');
            insertionMode.text.apply(undefined, arguments);
          }
        }
      },
      afterHead: {
        toString: function () { return "after head"; },
        startTag: function (tagName, attribs) {
          if (tagName === 'html') {
            insertionModes.inBody.startTag.apply(undefined, arguments);
          } else if (tagName === 'body') {
            normalInsert(tagName, attribs);
            insertionMode = insertionModes.inBody;
          // TODO(kpreid): Implement the "stuff that should be in head" case.
          } else if (tagName === 'head') {
            // "Parse error. Ignore the token."
          } else {
            insertionMode.startTag('body', []);
            insertionMode.startTag.apply(undefined, arguments);
          }
        },
        endTag: function (tagName) {
          if (tagName === 'body' || tagName === 'html' || tagName === 'br') {
            insertionMode.startTag('body', []);
            insertionMode.endTag.apply(undefined, arguments);
          } else {
            // "Parse error. Ignore the token."
          }
        },
        text: function (text) {
          if (isHtml5NonWhitespace(text)) {
            insertionMode.startTag('body', []);
            insertionMode.text.apply(undefined, arguments);
          } else {
            normalText(text);
          }
        }
      },
      inBody: {
        toString: function () { return "in body"; },
        startTag: function (tagName, attribs) {
          if (tagName === 'html') {
            // TODO(kpreid): Implement
            // "Parse error. For each attribute on the token, check to see if
            //  the attribute is already present on the top element of the stack
            //  of open elements. If it is not, add the attribute and its
            //  corresponding value to that element."
          } else if (tagName === 'base' || tagName === 'basefont' ||
              tagName === 'bgsound'     || tagName === 'command' ||
              tagName === 'link'        || tagName === 'meta' || 
              tagName === 'noframes'    || tagName === 'script' ||
              tagName === 'style'       || tagName === 'title') {
            insertionModes.inHead.startTag.apply(undefined, arguments);
          } else if (tagName === 'body') {
            // "Parse error."
            // TODO(kpreid): Implement attribute merging etc.
          } else {
            normalInsert(tagName, attribs);
          }
        },
        endTag: function (tagName) {
          if (tagName === 'body') {
            // Yes, we really aren't moving the insertion point.
            insertionMode = insertionModes.afterBody;
          } else if (tagName === 'html') {
            insertionMode.endTag('body');
            insertionMode.endTag.apply(undefined, arguments);
          } else {
            // TODO(kpreid): Confirm vs spec'd "Any other end tag" handling
            normalEndTag(tagName);
          }
        },
        text: function (text) {
          normalText(text);
        }
      },
      text: {
        toString: function () { return "text"; },
        startTag: function (tagName, attribs) {
          throw new Error("shouldn't happen: start tag <" + tagName +
              "...> while in text insertion mode for " +
              insertionPoint.tagName);
        },
        endTag: function (tagName) {
          normalEndTag(tagName);
          insertionMode = originalInsertionMode;
        },
        text: function (text) {
          normalText(text);
        }
      },
      afterBody: {
        toString: function () { return "after body"; },
        startTag: function (tagName, attribs) {
          if (tagName === 'html') {
            insertionModes.inBody.startTag.apply(undefined, arguments);
          } else {
            // "Parse error."
            insertionMode = insertionModes.inBody;
            insertionMode.startTag.apply(undefined, arguments);
          }
        },
        endTag: function (tagName) {
          if (tagName === 'html') {
            insertionMode = insertionModes.afterAfterBody;
          } else {
            insertionMode = insertionModes.inBody;
            insertionMode.endTag.apply(undefined, arguments);
          }
        },
        text: function (text) {
          if (isHtml5NonWhitespace(text)) {
            // "Parse error."
            insertionMode = insertionModes.inBody;
          }
          insertionModes.inBody.text.apply(undefined, arguments);
        }
      },
      afterAfterBody: {
        toString: function () { return "after after body"; },
        startTag: function (tagName, attribs) {
          if (tagName === 'html') {
            insertionModes.inBody.startTag.apply(undefined, arguments);
          } else {
            // "Parse error."
            insertionMode = insertionModes.inBody;
            insertionMode.startTag.apply(undefined, arguments);
          }
        },
        endTag: function (tagName) {
          // "Parse error."
          insertionMode = insertionModes.inBody;
          insertionMode.endTag.apply(undefined, arguments);
        },
        text: function (text) {
          if (isHtml5NonWhitespace(text)) {
            // "Parse error."
            insertionMode = insertionModes.inBody;
            insertionMode.text.apply(undefined, arguments);
          } else {
            insertionModes.inBody.text.apply(undefined, arguments);
          }
        }
      }
    };
    var insertionMode = insertionModes.initial;
    var originalInsertionMode = null;

    /**
     * Given that attach() has updated the insertionPoint, change the
     * insertionMode to a suitable value.
     */
    updateInsertionMode = function updateInsertionMode() {
      // Note: This algorithm was made from scratch and does NOT reflect the
      // HTML5 specification.
      if (insertionPoint === base) {
        if (insertionPoint.lastChild) {
          insertionMode = insertionModes.afterAfterBody;
        } else {
          insertionMode = insertionModes.beforeHtml;
        }
      } else {
        for (var anc = insertionPoint; anc !== base; anc = anc.parentNode) {
          var tn = html.realToVirtualElementName(anc.tagName).toLowerCase();
          switch (tn) {
            case 'head': insertionMode = insertionModes.inHead; break;
            case 'body': insertionMode = insertionModes.inBody; break;
            case 'html':
              var prevtn = html.realToVirtualElementName(
                  (anc.lastChild || {}).tagName).toLowerCase();
              if (prevtn === undefined) {
                insertionMode = insertionModes.beforeHead;
              } else {
                switch (prevtn) {
                  case 'head': insertionMode = insertionModes.afterHead; break;
                  case 'body': insertionMode = insertionModes.afterBody; break;
                }
              }
              break;
            default: break;
          }
        }
      }
    };

    var documentWriter = {
      startDoc: function() {
        // TODO(jasvir): Fix recursive document.write
        if (depth == 0) {
          documentLoaded = Q.defer();
        }
        depth++;
      },
      endDoc: function () {
        depth--;
        if (depth == 0) {
          documentLoaded.resolve(true);
        }
      },
      startTag: function (tagName, attribs, params, marker, continuation) {
        var eltype = html4.ELEMENTS[tagName];
        if (!html4.ELEMENTS.hasOwnProperty(tagName)
            || (eltype & html4.eflags.UNSAFE) !== 0 &&
                !(eltype & html4.eflags.VIRTUALIZED)) {
          if (tagName === 'script') {
            var scriptSrc = lookupAttr(attribs, 'src');
            if (!scriptSrc) {
              // A script tag without a script src - use child node for source
              cdataContentType = html4.eflags.SCRIPT;
              pendingExternal = undefined;
            } else {
              cdataContentType = html4.eflags.SCRIPT;
              pendingExternal = scriptSrc;
            }
          } else if (tagName === 'style') {
            cdataContentType = html4.eflags.STYLE;
            pendingExternal = undefined;
          } else if ('link' === tagName) {
            // Link types are case insensitive
            var rel = lookupAttr(attribs, 'rel');
            var href = lookupAttr(attribs, 'href');
            var rels = rel ? String(rel).toLowerCase().split(' ') : [];
            if (href && rels.indexOf('stylesheet') >= 0) {
              var res = resolveUriRelativeToDocument(href);
              defineUntrustedExternalStylesheet(res, marker, continuation);
            }
          } else if (tagName === 'base') {
            var baseHref = lookupAttr(attribs, 'href');
            if (baseHref && domicile) {
              domicile.setBaseUri(resolveUriRelativeToDocument(baseHref));
            }
          }
          return;
        }
        insertionMode.startTag(tagName, attribs);
      },
      endTag: function (tagName, optional, marker, continuation) {
        var eltype = html4.ELEMENTS[tagName];
        // Close any open script or style element element.
        // TODO(kpreid): Move this stuff into the insertion mode logic
        if (cdataContentType) {
          var isScript = cdataContentType === html4.eflags.SCRIPT;
          cdataContentType = 0;
          if (pendingExternal) {
            if (isScript) {
              var res = resolveUriRelativeToDocument(pendingExternal);
              evaluateUntrustedExternalScript(
                res, marker, continuation);
            }
            pendingExternal = undefined;
          } else {
            var content = cdataContent.join("");
            cdataContent.length = 0;
            if (isScript) {
              // TODO: create a script node that does not execute the untrusted
              // script, but that has any ID attribute properly rewritten.
              // It is not horribly uncommon for scripts to look for the last
              // script element as a proxy for the insertion cursor.
              evaluateUntrustedScript(domicile.pseudoLocation.href, content);
            } else {
              defineUntrustedStylesheet(domicile.pseudoLocation.href, content);
            }
          }
        }
        insertionMode.endTag(tagName);
      },
      pcdata: function (text) {
        insertionMode.text(text);
      },
      cdata: function (text) {
        if (cdataContentType) {
          cdataContent.push(text);
        } else {
          documentWriter.pcdata(text);
        }
      }
    };
    documentWriter.rcdata = documentWriter.pcdata;

    var htmlParser = html.makeSaxParser(documentWriter);

    // Document.write and document.writeln behave as described at
    // http://www.w3.org/TR/2009/WD-html5-20090825/embedded-content-0.html#dom-document-write
    // but with a few differences:
    // (1) all HTML written is sanitized per the opt_domicile's HTML
    //     sanitizer
    // (2) HTML written cannot change where subsequent static HTML is emitted.
    // (3) if the document has been closed (insertion point is undefined) then
    //     the window will not be reopened.  Instead, execution will proceed at
    //     the end of the virtual document.  This is allowed by the spec but
    //     only if the onunload refuses to allow an unload, so we treat the
    //     virtual document as un-unloadable by document.write.
    // (4) document.write cannot be used to inject scripts, so the
    //     "if there is a pending external script" does not apply.
    //     TODO(kpreid): This is going to change in the SES/client-side case.
    /**
     * A tame version of document.write.
     * @param html_varargs according to HTML5, the input to document.write is
     *     varargs, and the HTML is the concatenation of all the arguments.
     */
    var tameDocWrite = function write(html_varargs) {
      var htmlText = concat(arguments);
      if (!insertionPoint) {
        // Handles case 3 where the document has been closed.
        insertionPoint = base;
      }
      if (cdataContentType) {
        // A <script> or <style> element started in one document.write and
        // continues in this one as in
        //   document.write('<script>foo');
        //   document.write('(bar)</script>');
        // so we need to trick the SAX parser into a CDATA context.
        htmlText = (cdataContentType === html4.eflags.SCRIPT
                    ? '<script>' : '<style>') + htmlText;
      }
      htmlParser(htmlText);
      return documentLoaded.promise;
    };
    domicile.writeHook = cajaVM.def(tameDocWrite);
    domicile.evaluateUntrustedExternalScript =
      cajaVM.def(evaluateUntrustedExternalScript);
  })(opt_domicile);
}

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['HtmlEmitter'] = HtmlEmitter;
}
