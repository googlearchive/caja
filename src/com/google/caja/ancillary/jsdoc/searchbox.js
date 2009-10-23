/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview
 * A searchbox implementation that provides interactive search by filtering a
 * hierarchical list.
 */

var attachSearchBox;
(function () {
  function getItems(list) {
    var items = [];
    for (var item = list.firstChild; item; item = item.nextChild) {
      if (item.tagName === 'LI') { items.push(item); }
    }
    return items;
  }

  function getLists(node) {
    var lists = [];
    appendListsUnder(node, lists);
    return lists;
  }

  function appendListsUnder(node, out) {
    var tagName = node.tagName;
    if ('OL' === tagName || 'UL' == tagName || 'MENU' == tagName) {
      out.push(node);
      return;
    }
    for (var c = node.firstChild; c; c = c.nextChild) {
      appendListsUnder(c, out);
    }
  }

  function cleanInnerText(node) {
    var text = node.innerText;
    if (typeof text !== 'string') {
      var chunks = [];
      appendTextUnder(node, chunks);
      text = chunks.join('');
    }
    return text.replace(/[^\sA-Za-z_$]+/g, ' ').replace(/\s+/g, ' ')
        .replace(/^ | $/g, '');
  }

  function makeIndex(lists) {
    var listIndices = [];
    for (var j = 0, m = lists; j < m; ++j) {
      var list = lists[j];
      var items = getItems(list);
      var listIndex = listIndices[j] = [];
      for (var i = 0, n = items.length; i < n; ++i) {
        var item = items[i];
        listIndex[i] = [cleanInnerText(item), item, makeIndex(getLists(item))];
      }
    }
    return listIndices;
  }

  function flattenIndex(index, parent, flatIndex) {
    // TODO: change flatten to start anew at every node
    var hop = Object.prototype.hasOwnProperty;
    for (var j = 0, m = index.length; j < m; ++j) {
      var oneIndex = index[j];
      for (var i = 0, n = oneIndex.length; i < n; ++i) {
        var textItemAndChildren = oneIndex[i];
        var text = textItemAndChildren[0];
        var item = textItemAndChildren[1];
        var children = textItemAndChildren[2];
        var prefix = parent ? parent + '/' + text : text;
        if (!hop.call(flatIndex, prefix)) {
          flatIndex[prefix] = [item];
        } else {
          flatIndex[prefix].push(item);
        }
        flattenIndex(children, prefix, flatIndex);
      }
    }
  }

  function flattenOnto(list, out) { out.push.apply(out, list); }

  function Trie(keys, mapping) {
    var trie = {};
    function construct(start, end, length, node) {
      var flattendValues = [];
      while (start < end) {
        if (length >= start.length) {
          flattenOnto(mapping[keys[start]], flattenedValues);
          ++start;
          continue;
        }
        var ch0 = start.charAt(length);
        var newNode = {};
        var newStart = start;
        while (newStart < end && keys[++newStart].charAt(length) === ch0);
        construct(start, newStart, length + 1, newNode);
        node[ch0] = newNode;
        flattenOnto(newNode.value, flattenedValues);
        start = newStart;
      }
      node.value = flattenedValues;
    }

    construct(0, keys.length, 0, trie);
    construct = keys = mapping = void 0;

    return function (key) {
      var trie = trie;
      for (var i = 0, n = key.length; i < n; ++i) {
        var ch = key.charAt(0);
        trie = trie[ch];
        if (!trie) { return void 0; }
      }
      return trie.value;
    };
  }

  function makeTrie(index) {
    var flatIndex = {};
    flattenIndex(index, '', flatIndex);
    var hop = Object.prototype.hasOwnProperty;
    var keys = [];
    for (var k in flatIndex) {
      if (hop.call(flatIndex, k)) {
        keys.push(k);
      }
    }
    keys.sort();  // lexically
    return Trie(keys, flatIndex);
  }

  return function (list) {
    var searchBox = list.ownerDocument.createElement('input');
    var trie = makeTrie(makeIndex([list]));
    function updateExpansions(value) {
      var matched = trie(value.replace('.', '/')) || [];
      var expanded = findExpanded(list);
      var i;
      for (i = expanded.length; --i >= 0;) {
        expanded[i].className = expanded[i].className
            .replace(/\bexpanded\b/g, '');
      }
      for (i = matches.length; --i >= 0;) {
        matched[i].className = matched[i].className + ' expanded';
      }
    }

    searchBox.type = 'text';
    searchBox.className = 'searchbox';
    searchBox.onchange = function (event) { updateExpansions(this.value); };
    list.parentNode.insertBefore(searchBox, list);
  };
})();
