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
 * @requires bridalMaker html html4 ___
 */

/**
 * @param base a node that is the ancestor of all statically generated HTML.
 * @param opt_tameDocument a tame document that will receive a load event
 *    when the html-emitter is closed, and which will have {@code write} and
 *    {@code writeln} members attached.
 */
function HtmlEmitter(base, opt_tameDocument) {
  if (!base) { throw new Error(); }
  var insertionPoint = base;
  var bridal = bridalMaker(base.ownerDocument);

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
      var id = desc.getAttributeNode('id');
      if (id && id.value) { idMap[id.value] = desc; }
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
    var node = idMap[id];
    if (node) { return node; }
    for (; (node = base.ownerDocument.getElementById(id));) {
      if (base.contains
          ? base.contains(node)
          : (base.compareDocumentPosition(node) & 0x10)) {
        idMap[id] = node;
        return node;
      } else {
        node.id = '';
      }
    }
    return null;
  }

  /**
   * emitStatic allows the caller to inject the static HTML from JavaScript,
   * if the gadget container's usage pattern requires it.
   */
  function emitStatic(htmlString) {
    // TODO: We could append the cajoled HTML to existing contents of the
    // 'base' element, thus allowing the container to pre-populate it prior to
    // adding cajoled content. However, no clients need that yet.
    if (base.firstChild) {
      throw new Error('Container error: Virtual document element is not empty');
    }
    base.innerHTML = htmlString;
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
      next = child.nextSibling;  // removeChild kills nextSibling.
      out.push(child, limit);
      limit.removeChild(child);
    }

    // Second, store your ancestor's next siblings and recurse.
    for (var anc = limit, greatAnc; anc && anc !== base; anc = greatAnc) {
      greatAnc = anc.parentNode;
      for (var sibling = anc.nextSibling, next; sibling; sibling = next) {
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
   * Reattach any remaining detached bits, free resources, and fire a document
   * loaded event.
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
    var doc = opt_tameDocument;
    if (doc) { doc.signalLoaded___(); }
    return this;
  }

  this.byId = byId;
  this.attach = attach;
  this.discard = discard;
  this.emitStatic = emitStatic;
  this.finish = finish;
  this.signalLoaded = signalLoaded;
  this.setAttr = bridal.setAttribute;

  (function (tameDoc) {
    if (!tameDoc || tameDoc.write) { return; }

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

    var documentWriter = {
      startTag: function (tagName, attribs) {
        var eltype = html4.ELEMENTS[tagName];
        if (!html4.ELEMENTS.hasOwnProperty(tagName)
            || (eltype & html4.eflags.UNSAFE) !== 0) {
          return;
        }
        tameDoc.sanitizeAttrs___(tagName, attribs);
        var el = bridal.createElement(tagName, attribs);
        if ((eltype & html4.eflags.OPTIONAL_ENDTAG)
            && el.tagName === insertionPoint.tagName) {
          documentWriter.endTag(el.tagName, true);
        }
        insertionPoint.appendChild(el);
        if (!(eltype & html4.eflags.EMPTY)) { insertionPoint = el; }
      },
      endTag: function (tagName, optional) {
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
        insertionPoint.appendChild(
            insertionPoint.ownerDocument.createTextNode(text));
      }
    };
    documentWriter.rcdata = documentWriter.pcdata;

    // Document.write and document.writeln behave as described at
    // http://www.w3.org/TR/2009/WD-html5-20090825/embedded-content-0.html#dom-document-write
    // but with a few differences:
    // (1) all HTML written is sanitized per the opt_tameDocument's HTML
    //     sanitizer
    // (2) HTML written cannot change where subsequent static HTML is emitted.
    // (3) if the document has been closed (insertion point is undefined) then
    //     the window will not be reopened.  Instead, execution will proceed at
    //     the end of the virtual document.  This is allowed by the spec but
    //     only if the onunload refuses to allow an unload, so we treat the
    //     virtual document as un-unloadable by document.write.
    // (4) document.write cannot be used to inject scripts, so the
    //     "if there is a pending external script" does not apply.
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
      var lexer = html.makeSaxParser(documentWriter);
      lexer(htmlText);
    };
    tameDoc.write = ___.markFuncFreeze(tameDocWrite, 'write');
    tameDoc.writeln = ___.markFuncFreeze(function writeln(html) {
      tameDocWrite(concat(arguments), '\n');
    }, 'writeln');
    ___.grantFunc(tameDoc, 'write');
    ___.grantFunc(tameDoc, 'writeln');
  })(opt_tameDocument);
}
