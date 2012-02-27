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
 * @provides HtmlEmitter
 * @requires bridalMaker html html4 cajaVM parseCssStylesheet console
 * @requires cssSchema sanitizeCssProperty sanitizeCssSelectors
 */

/**
 * @param {function} makeDOMAccessible A function which will be called on base
 *     and every object retrieved from it, recursively. This hook is available
 *     in case HtmlEmitter is running in an environment such that unmodified DOM
 *     objects cannot be touched. makeDOMAccessible should be idempotent. Note
 *     that the contract here is stronger than for bridalMaker, in that
 *     this makeDOMAccessible may not return a different object.
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

  /**
   * Contiguous pairs of ex-descendants of base, and their ex-parent.
   * The detached elements (even indices) are ordered depth-first.
   */
  var detached = null;
  /** Makes sure IDs are accessible within removed detached nodes. */
  var idMap = null;

  var arraySplice = Array.prototype.splice;

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
  /**
   * Attach to the virtual document body classes that were extracted from the
   * body element.
   * @param {string} classes rewritten HTML classes.
   */
  function addBodyClasses(classes) {
    base.className += ' ' + classes;
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
  this.addBodyClasses = addBodyClasses;
  this.handleEmbed = handleEmbed;

  (function (domicile) {
    if (!domicile || domicile.writeHook) { return; }

    function concat(items) {
      return Array.prototype.join.call(items, '');
    }

    var ucase;
    if ('script'.toUpperCase() === 'SCRIPT') {
      ucase = function (s) { return s.toUpperCase(); };
    } else {
      ucase = function (s) {
        return s.replace(
            /[a-z]/g,
            function (ch) {
              return String.fromCharCode(ch.charCodeAt(0) & ~32);
            });
      };
    }

    function evaluateUntrustedScript(scriptInnerText) {
      if (!opt_guestGlobal) { return; }
      var errorMessage = "SCRIPT element evaluation failed";

      var cajaVM = opt_guestGlobal.cajaVM;
      if (cajaVM) {
        var compileModule = cajaVM.compileModule;
        if (compileModule) {
          try {
            compileModule(scriptInnerText)(opt_domicile.window);
            return;  // Do not trigger onerror below.
          } catch (ex) {
            errorMessage = (ex && (ex.message || ex.description))
                || errorMessage;
          }
        }
      }

      // Dispatch to the onerror handler.
      try {
        // TODO: Should this happen inline or be dispatched out of band?
        opt_guestGlobal.onerror(
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

    var allowed = {};
    var cssMediaTypeWhitelist = {
      'braille': allowed,
      'embossed': allowed,
      'handheld': allowed,
      'print': allowed,
      'projection': allowed,
      'screen': allowed,
      'speech': allowed,
      'tty': allowed,
      'tv': allowed
    };

    function sanitizeHistorySensitive(blockOfProperties) {
      return '{}';  // TODO: implement me.
    }

    function defineUntrustedStylesheet(cssText) {
      var safeCss = void 0;
      // A stack describing the { ... } regions.
      // Null elements indicate blocks that should not be emitted.
      var blockStack = [];
      // True when the content of the current block should be left off safeCss.
      // If we don't have a domicile then we don't have a way to sanitize CSS
      // properties.
      var elide = !domicile;
      parseCssStylesheet(
          cssText,
          {
            startStylesheet: function () {
              safeCss = [];
            },
            endStylesheet: function () {
            },
            startAtrule: function (atIdent, headerArray) {
              if (elide) {
                atIdent = null;
              } else if (atIdent === '@media') {
                headerArray = headerArray.filter(
                  function (mediaType) {
                    return cssMediaTypeWhitelist[mediaType] == allowed;
                  });
                if (headerArray.length) {
                  safeCss.push(atIdent, headerArray.join(','), '{');
                } else {
                  atIdent = null;
                }
              } else {
                if (atIdent === '@import') {
                  if ('undefined' !== typeof console) {
                    console.log('@import ' + headerArray.join(' ') + ' elided');
                  }
                }
                atIdent = null;  // Elide the block.
              }
              elide = !atIdent;
              blockStack.push(atIdent);              
            },
            endAtrule: function () {
              var atIdent = blockStack.pop();
              if (!elide) {
                safeCss.push(';');
              }
              checkElide();
            },
            startBlock: function () {
              // There are no bare blocks in CSS, so we do not change the
              // block stack here, but instead in the events that bracket
              // blocks.
              if (!elide) {
                safeCss.push('{');
              }
            },
            endBlock: function () {
              if (!elide) {
                safeCss.push('}');
                elide = true;  // skip any semicolon from endAtRule.
              }
            },
            startRuleset: function (selectorArray) {
              var historySensitiveSelectors = void 0;
              var removeHistoryInsensitiveSelectors = false;
              if (!elide) {
                var selectors = sanitizeCssSelectors(selectorArray);
                var historyInsensitiveSelectors = selectors[0];
                historySensitiveSelectors = selectors[1];
                if (!historyInsensitiveSelectors.length
                    && !historySensitiveSelectors.length) {
                  elide = true;
                } else {
                  var selector = historyInsensitiveSelectors.join(', ');
                  if (!selector) {
                    // If we have only history sensitive selectors,
                    // use an impossible rule so that we can capture the content
                    // for later processing by 
                    // history insenstive content for use below.
                    selector = 'head > html';
                    removeHistoryInsensitiveSelectors = true;
                  }
                  safeCss.push(selector);
                }
              }
              blockStack.push(
                  elide
                  ? null
                  // Sometimes a single list of selectors is split in two,
                  //   div, a:visited
                  // because we want to allow some properties for DIV that
                  // we don't want to allow for A:VISITED to avoid leaking
                  // user history.
                  // Store the history sensitive selectors and the position
                  // where the block starts so we can later create a copy
                  // of the permissive tokens, and filter it to handle the
                  // history sensitive case.
                  : {
                      historySensitiveSelectors: historySensitiveSelectors,
                      endOfSelecctors: safeCss.length,
                      removeHistoryInsensitiveSelectors:
                         removeHistoryInsensitiveSelectors
                    });
            },
            endRuleset: function () {
              var rules = blockStack.pop();
              var propertiesEnd = safeCss.length;
              if (!elide && rules) {
                var extraSelectors = rules.historySensitiveSelectors;
                if (extraSelectors.length) {
                  var propertyGroupTokens = safeCss.slice(rules.endOfSelectors);
                  safeCss.push(extraSelectors.join(', '));
                  safeCss.push.apply(
                      safeCss, sanitizeHistorySensitive(propertyGroupTokens));
                }
              }
              if (rules && rules.removeHistoryInsensitiveSelectors) {
                safeCss.splice(rules.endOfSelectors - 1, propertiesEnd);
              }
              checkElide();
            },
            declaration: function (property, valueArray) {
              if (!elide && domicile) {
                var schema = cssSchema[property];
                var sanitizeUri = void 0;  // TODO
                if (schema) {
                  sanitizeCssProperty(property, valueArray, sanitizeUri);
                  if (valueArray.length) {
                    safeCss.push(property, ':', valueArray.join(' '), ';');
                  }
                }
              }
            }
          });
      function checkElide() {
        elide = blockStack.length === 0 
            || blockStack[blockStack.length-1] !== null;
      }
      var document = insertionPoint.ownerDocument;
      var safeCssText = safeCss.join('');
      document.getElementsByTagName('head')[0].appendChild(
          bridal.createStyleSheet(document, safeCssText));
    }

    // Zero or one of the html4.eflags constants that captures the content type
    // of cdataContent.
    var cdataContentType = 0;
    // Chunks of CDATA content of the type above which need to be specially
    // processed and interpreted.
    var cdataContent = [];

    var documentWriter = {
      startTag: function (tagName, attribs) {
        var eltype = html4.ELEMENTS[tagName];
        if (!html4.ELEMENTS.hasOwnProperty(tagName)
            || (eltype & html4.eflags.UNSAFE) !== 0) {
          if (tagName === 'script') {
            var srcIndex = attribs.indexOf('src');
            while (srcIndex >= 0 && (srcIndex & 1)) {
              // Found an attribute value with value "src" not name "src".
              srcIndex = attribs.indexOf('src', srcIndex + 1);
            }
            if (srcIndex < 0) {
              cdataContentType = html4.eflags.SCRIPT;
            }
          } else if (tagName === 'style') {
            cdataContentType = html4.eflags.STYLE;
          }
          return;
        }
        domicile.sanitizeAttrs(tagName, attribs);
        var el = bridal.createElement(tagName, attribs);
        if ((eltype & html4.eflags.OPTIONAL_ENDTAG)
            && el.tagName === insertionPoint.tagName) {
          documentWriter.endTag(el.tagName, true);
        }
        insertionPoint.appendChild(el);
        if (!(eltype & html4.eflags.EMPTY)) { insertionPoint = el; }
      },
      endTag: function (tagName, optional) {
        // Close any open script or style element element.
        if (cdataContentType) {
          var isScript = cdataContentType === html4.eflags.SCRIPT;
          cdataContentType = 0;
          var content = cdataContent.join("");
          cdataContent.length = 0;
          if (isScript) {
            // TODO: create a script node that does not execute the untrusted
            // script, but that has any ID attribute properly rewritten.
            // It is not horribly uncommon for scripts to look for the last
            // script element as a proxy for the insertion cursor.
            evaluateUntrustedScript(content);
          } else {
            defineUntrustedStylesheet(content);
          }
        }
        var anc = insertionPoint;
        tagName = ucase(tagName);
        while (anc !== base && !/\bvdoc-body___\b/.test(anc.className)) {
          var p = anc.parentNode;
          if (anc.tagName === tagName) {
            insertionPoint = p;
            return;
          }
          anc = p;
        }
      },
      pcdata: function (text) {
        insertionPoint.appendChild(insertionPoint.ownerDocument.createTextNode(
            html.unescapeEntities(text)));
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
    };
    domicile.writeHook = cajaVM.def(tameDocWrite);
  })(opt_domicile);
}
