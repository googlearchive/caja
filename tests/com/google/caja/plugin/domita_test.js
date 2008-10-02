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

var setUp, tearDown;

/** Aim high and you might miss the moon! */
function expectFailure(shouldFail, opt_msg, opt_failFilter) {
  try {
    shouldFail();
  } catch (e) {
    if (opt_failFilter && !opt_failFilter(e)) { throw e; }
    return;
  }
  fail(opt_msg || 'Expected failure');
}

function assertFailsSafe(canFail, assertionsIfPasses) {
  try {
    canFail();
  } catch (e) {
    return;
  }
  assertionsIfPasses();
}

/**
 * Canonicalize well formed innerHTML output, by making sure that attributes
 * are ordered by name, and have quoted values.
 *
 * Without this step, it's impossible to compare innerHTML cross-browser.
 */
function canonInnerHtml(s) {
  // Sort attributes.
  var htmlAttribute = new RegExp('^\\s*(\\w+)(?:\\s*=\\s*("[^\\"]*"'
                                 + '|\'[^\\\']*\'|[^\\\'\\"\\s>]+))?');
  var quot = new RegExp('"', 'g');
  var htmlStartTag = new RegExp('(<\\w+)\\s+([^\\s>][^>]*)>', 'g');
  var htmlTag = new RegExp('(<\/?)(\\w+)(\\s+[^\\s>][^>]*)?>', 'g');
  var ignorableWhitespace = new RegExp('^[ \\t]*(\\r\\n?|\\n)|\\s+$', 'g');
  var tagEntityOrText = new RegExp(
      '(?:(</?\\w[^>]*>|&[a-zA-Z#]|[^<&>]+)|([<&>]))', 'g');
  s = s.replace(
      htmlStartTag,
      function (_, tagStart, tagBody) {
        var attrs = [];
        for (var m; (m = tagBody.match(htmlAttribute));) {
          var name = m[1];
          var value = m[2];
          var hasValue = value != null;
          if (hasValue && (new RegExp('^["\']')).test(value)) {
            value = value.substring(1, value.length - 1);
          }
          attrs.push(
              hasValue
              ? name + '="' + value.replace(quot, '&quot;') + '"'
              : name);
          tagBody = tagBody.substring(m[0].length);
        }
        attrs.sort();
        return tagStart + ' ' + attrs.join(' ') + '>';
      });
  s = s.replace(
      htmlTag,
      function (_, open, name, body) {
        return open + name.toLowerCase() + (body || '') + '>';
      });
  // Remove ignorable whitespace.
  s = s.replace(ignorableWhitespace, '');
  // Normalize escaping of text nodes since Safari doesn't escape loose >.
  s = s.replace(
      tagEntityOrText,
      function (_, good, bad) {
        return good
            ? good
            : (bad.replace(new RegExp('&', 'g'), '&amp;')
               .replace(new RegExp('>', 'g'), '&gt;'));
      });
  return s;
}

jsunitRegister('testGetElementById',
               function testGetElementById() {
  assertEquals(null, document.getElementById('foo'));
  assertEquals(null, document.getElementById('bar'));
  assertEquals(null, document.getElementById('no_such_node'));
  assertTrue(document.getElementById('test-get-element-by-id') != null);
  assertEquals(null, document.getElementById('xyz-test-get-element-by-id'));
  assertTrue(document.getElementById('test-get-element-by-id-2') != null);

  pass('test-get-element-by-id');
  pass('test-get-element-by-id-2');
});

jsunitRegister('testElementId',
               function testElementId() {
  var el = document.getElementById('test-element-id');
  assertEquals('test-element-id', el.id);
  assertEquals('test-element-id', el.getAttribute('id'));

  pass('test-element-id');
});

jsunitRegister('testCreateElement',
               function testCreateElement() {
  var newNode = document.createElement('DIV');
  assertEquals('', newNode.id);
  newNode.id = 'newNodeId';
  assertEquals('newNodeId', newNode.id);

  newNode.id = '#bog<us>';  // Not set
  assertEquals('newNodeId', newNode.id);

  newNode.id = 'not:bogus';
  assertEquals('not:bogus', newNode.id);
  assertEquals(1, newNode.nodeType);

  var el = document.getElementById('test-create-element');
  el.appendChild(newNode);

  assertEquals(document.getElementById('not:bogus').tagName,
               newNode.tagName);
  assertEquals(newNode.tagName, el.firstChild.tagName);
  assertEquals(newNode.tagName, el.lastChild.tagName);

  var text = document.createTextNode(
      { toString: function () { return 'howdy <there>'; } });
  assertEquals(3, text.nodeType);
  assertEquals('howdy <there>', text.data);
  newNode.appendChild(text);
  assertEquals(3, newNode.firstChild.nodeType);
  assertEquals('howdy &lt;there&gt;', canonInnerHtml(newNode.innerHTML));

  pass('test-create-element');
});

jsunitRegister('testInnerHtml',
               function testInnerHtml() {
  var container = document.getElementById('test-inner-html');

  // Strips out non-prefixed id from link, and target=_parent.
  // Leaves id for <em> but strips the prefix.
  // Escapes trailing title, href, and > after </em>.
  assertEquals(
      '<a class="link" href="http://foo.com?a=b&amp;c=d"'
      + ' title="&lt;click me!&gt;">'
      + 'Test <em id="em">Not</em>&gt; run yet.</a>',
      canonInnerHtml(container.innerHTML));

  // Set innerHTML
  container.innerHTML = (
      '<a  id="foo" class="green blue" href="http://bar.com/baz"'
      + ' target="foo" title="A link" >'
      + 'A & B &amp; C<</a >');

  assertEquals(
      '<a class="green blue" href="http://gadget-proxy/'
      + '?url=http%3A%2F%2Fbar.com%2Fbaz&amp;mimeType=*%2F*" id="foo-xyz___"'
      + ' target="_blank" title="A link">A &amp; B &amp; C&lt;</a>',
      canonInnerHtml(directAccess.getInnerHTML(container)));

  pass('test-inner-html');
});

jsunitRegister('testForms',
               function testForms() {
  var form = document.createElement('FORM');
  assertEquals('return false', directAccess.getAttribute(form, 'onsubmit'));

  var container = document.getElementById('test-forms');
  container.innerHTML = '<form onsubmit="foo()">'
      + '<input type="submit" value="Submit"></form>';

  assertEquals('<form onsubmit="'
               + 'try { plugin_dispatchEvent___'
               + '(this, event || window.event, 0, &quot;foo&quot;);'
               + ' } finally { return false; }">'
               + '<input type="submit" value="Submit"></form>',
               canonInnerHtml(directAccess.getInnerHTML(container)));

  pass('test-forms');
});

function foo() {
  var container = document.getElementById('test-forms');
  var div = document.createElement('blockquote');
  div.innerHTML = 'event dispatched to <code>foo()</code>';
  container.appendChild(div);
}

jsunitRegister('testCantLoadScript',
               function testCantLoadScript() {
  expectFailure(
      function () {
        document.createElement('SCRIPT');
      },
      'successfully created a script tag via SCRIPT');
  expectFailure(
      function () {
        document.createElement('script');
      },
      'successfully created a script tag via script');
  expectFailure(
      function () {
        document.createElement('scRipt');
      },
      'successfully created a script tag via scRipt');

  var node;
  assertFailsSafe(
      function () {
        var i = 0;
        node = document.createElement({
          toString: function () { return (++i & 1) ? 'DIV' : 'SCRIPT'; }
        });
      },
      function () {
        assertEquals('DIV', node.tagName);
      });

  pass('test-no-script');
});

jsunitRegister('testAddEventListener',
               function testAddEventListener() {
  var container = document.getElementById('test-add-event-listener');
  container.addEventListener(
      'click',
      function (node, event) {
        console.log('received event');
        assertEquals('P', node.tagName);
        assertEquals('click', event.type);
        pass('test-add-event-listener');
      });
});

jsunitRegister('testGetElementsByTagName',
               function testGetElementsByTagName() {
  var container = document.getElementById('test-get-elements-by-tag-name');
  var items = container.getElementsByTagName('li');
  assertEquals(5, items.length);
  for (var i = 0; i < items.length; ++i) {
    assertEquals('LI', items[i].tagName);
  }
  assertEquals('One', canonInnerHtml(items[0].innerHTML));
  assertEquals('Two', canonInnerHtml(items[1].innerHTML));
  assertEquals('Three', canonInnerHtml(items[2].firstChild.data));
  assertEquals('Pi', canonInnerHtml(items[3].innerHTML));
  assertEquals('sqrt(10)', canonInnerHtml(items[4].innerHTML));
  pass('test-get-elements-by-tag-name');
});

jsunitRegister('testDynamicStyle',
               function testDynamicStyle() {
  function $(id) { return document.getElementById(id); }
  $('test-dynamic-styles1').style.fontWeight = 'bold';
  $('test-dynamic-styles2').style.fontWeight = '';  // Can unset a style.
  expectFailure(
      function () {
        $('test-dynamic-styles3').style.fontWeight = 'super-bold';
      },
      'set to super-bold');
  pass('test-dynamic-styles');
});

jsunitRegister('testReadOnlyNotEditable',
               function testReadOnlyNotEditable() {
  function $(id) { return documentRO.getElementById(id); }
  expectFailure(
      function () {
        documentRO.createElement('SPAN');
      },
      'successfully created element');
  expectFailure(
      function () {
        var el = document.createElement('SPAN');
        el.id = 'test-read-only-1';
        $('test-read-only').appendChild(el);
        assertEquals(null, $('test-read-only-1'));
      },
      'successfully appended element');
  expectFailure(
      function () {
        assertTrue($('indelible') != null);
        $('test-read-only').removeChild($('indelible'));
        assertEquals(null, $('test-read-only-1'));
      },
      'successfully removed element');
  expectFailure(
      function () {
        var el = document.createElement('SPAN');
        el.id = 'test-read-only-2';
        $('test-read-only').replaceChild($('indelible'), el);
        assertEquals(null, $('test-read-only-2'));
        assertTrue($('indelible') != null);
      },
      'successfully replaced element');
  pass('test-read-only');
});

jsunitRegister('testInsertBefore',
               function testInsertBefore() {
  var el = document.getElementById('test-insert-before');
  function assertChildren(var_args) {
    var children = [];
    for (var child = el.firstChild; child; child = child.nextSibling) {
      children.push(child.nodeValue);
    }
    assertEquals([].join.call(arguments, ','), children.join(','));
  }
  var one = document.createTextNode('one');
  var two = document.createTextNode('two');
  var three = document.createTextNode('three');
  var four = document.createTextNode('four');
  el.insertBefore(three, null);
  assertChildren('zero', 'three');
  el.insertBefore(one, three);
  assertChildren('zero', 'one', 'three');
  el.insertBefore(two, null);
  assertChildren('zero', 'one', 'three', 'two');
  el.insertBefore(four, two);
  assertChildren('zero', 'one', 'three', 'four', 'two');
  el.insertBefore(two, one);
  assertChildren('zero', 'two', 'one', 'three', 'four');
  el.insertBefore(one, two);
  assertChildren('zero', 'one', 'two', 'three', 'four');
  el.insertBefore(four, void 0);
  assertChildren('zero', 'one', 'two', 'three', 'four');
  pass('test-insert-before');
});

jsunitRegister('testNodeLists',
               function testNodeLists() {
  function $(id) { return document.getElementById(id); }
  var descendants = $('test-node-lists').getElementsByTagName('*');
  assertEquals(4, descendants.length);

  assertEquals('LI', descendants[0].tagName);
  assertEquals('LI', descendants[1].tagName);
  assertEquals('B', descendants[2].tagName);
  assertEquals('LI', descendants[3].tagName);

  assertEquals('LI', descendants.item(0).tagName);
  assertEquals('LI', descendants.item(1).tagName);
  assertEquals('B', descendants.item(2).tagName);
  assertEquals('LI', descendants.item(3).tagName);

  var item = descendants.item;
  // Check does not access properties of global.
  assertEquals('LI', item(0).tagName);

  pass('test-node-lists');
});

jsunitRegister('testNameAttr',
               function testNameAttr() {
  var testDiv = document.getElementById('test-name-attr');
  testDiv.innerHTML = '<span name="test-span">text</span>';
  var nameAttr = directAccess.getAttribute(testDiv.firstChild, 'name');
  assertFalse('test-span' === nameAttr); // Should have been renamed
  pass('test-name-attr');
});

jsunitRegister('testTargetAttr',
               function testTargetAttr() {
  var testDiv = document.getElementById('test-target-attr');
  testDiv.innerHTML = '<a target="foo">text1</a><a>text2</a>';
  var node = testDiv.firstChild;
  assertEquals('_blank', directAccess.getAttribute(node, 'target'));
  node = node.nextSibling;
  assertEquals('_blank', directAccess.getAttribute(node, 'target'));
  pass('test-target-attr');
});

jsunitRegister('testLocation',
               function testLocation() {
  assertEquals(
      window.location.href, window.location.protocol + '//'
      + window.location.host + window.location.port + window.location.pathname
      + window.location.search + window.location.hash);
  pass('test-location');
});

jsunitRegister('testNavigator',
               function testNavigator() {
  assertEquals(
      window.navigator.userAgent,
      window.navigator.appCodeName + '/' + window.navigator.appVersion);
  pass('test-navigator');
});

jsunitRegister('testEmitCss',
               function testCss() {
  directAccess.emitCssHook(['.', ' a { color: #00ff00 }']);
  var computedColor = directAccess.getComputedStyle(
      document.getElementById('not-blue'), 'color').toLowerCase();
  if (!(computedColor === 'green' || computedColor === '#00ff00'
        || computedColor === '#0f0' || computedColor === 'rgb(0, 255, 0)'
        || computedColor === 'rgb(0, 100%, 0)')) {
    fail(computedColor + ' is not green');
  } else {
    pass('test-emit-css');
  }
});
