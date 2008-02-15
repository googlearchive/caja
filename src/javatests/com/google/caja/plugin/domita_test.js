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

function testGetElementById() {
  assertEquals(null, document.getElementById('foo'));
  assertEquals(null, document.getElementById('bar'));
  assertEquals(null, document.getElementById('no_such_node'));
  assertTrue(document.getElementById('test-get-element-by-id') != null);
  assertEquals(null, document.getElementById('xyz-test-get-element-by-id'));
  assertTrue(document.getElementById('test-get-element-by-id-2') != null);

  pass('test-get-element-by-id');
}

function testElementId() {
  var el = document.getElementById('test-element-id');
  assertEquals('test-element-id', el.id);
  assertEquals('test-element-id', el.getAttribute('id'));

  pass('test-element-id');
}

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
  assertEquals('howdy &lt;there&gt;', newNode.innerHTML);

  pass('test-create-element');
}

function testInnerHtml() {
  var container = document.getElementById('test-inner-html');

  // Strips out non-prefixed id from link, and target=_parent.
  // Leaves id for <em> but strips the prefix.
  // Escapes trailing title, href, and > after </em>.
  assertEquals(
      '<A HREF="http://foo.com?a=b&amp;c=d" CLASS="link"'
      + ' TITLE="&lt;click me!&gt;">'
      + 'Test <EM ID="em">Not</EM>&gt; run yet.</A>\n    ',
      container.innerHTML);

  // Set innerHTML
  container.innerHTML = (
      '<a  id="foo" class="green blue" href="http://bar.com/baz"'
      + ' target="foo" title="A link" >'
      + 'A & B &amp; C<</a >');

  assertEquals(
      '<a id="xyz-foo" class="green blue" href="'
      + 'http://gadget-proxy/?url=http%3A%2F%2Fbar.com%2Fbaz&amp;mimeType=*%2F*'
      + '" title="A link" target="_blank">A &amp; B &amp; C&lt;</a>',
      directAccess.getInnerHTML(container));

  pass('test-inner-html');
}

function testForms() {
  var form = document.createElement('FORM');
  assertEquals('return false', directAccess.getAttribute(form, 'onsubmit'));

  var container = document.getElementById('test-forms');
  container.innerHTML = '<form onsubmit="foo()"><input type="submit"></form>';

  assertEquals('<form onsubmit=\''
               + 'try { plugin_dispatchEvent___'
               + '(this, event || window.event, 0, "foo");'
               + ' } finally { return false; }\'><input type="submit"></form>',
               directAccess.getInnerHTML(container));

  pass('test-forms');
}

function foo() {
  var container = document.getElementById('test-forms');
  var div = document.createElement('blockquote');
  div.innerHTML = 'event dispatched to <code>foo()</code>';
  container.appendChild(div);
}

function testCantLoadScript() {
  try {
    document.createElement('SCRIPT');
    fail('successfully created a script tag');
  } catch (e) {
    // pass
  }
  try {
    document.createElement('script');
    fail('successfully created a script tag');
  } catch (e) {
    // pass
  }
  try {
    document.createElement('scRipt');
    fail('successfully created a script tag');
  } catch (e) {
    // pass
  }
  try {
    var i = 0;
    var node = document.createElement({
      toString: function () { return (++i & 1) ? 'DIV' : 'SCRIPT'; }
    });
    assertEquals('DIV', node.tagName);
  } catch (e) {
    // pass
  }
  pass('test-no-script');
}

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
}

function testGetElementsByTagName() {
  var container = document.getElementById('test-get-elements-by-tag-name');
  var items = container.getElementsByTagName('li');
  assertEquals(5, items.length);
  for (var i = 0; i < items.length; ++i) {
    assertEquals('LI', items[i].tagName);
  }
  assertEquals('One', items[0].innerHTML.replace(/^\s+|\s+$/g, ''));
  assertEquals('Two', items[1].innerHTML.replace(/^\s+|\s+$/g, ''));
  assertEquals('Three', items[2].firstChild.data.replace(/^\s+|\s+$/g, ''));
  assertEquals('Pi', items[3].innerHTML.replace(/^\s+|\s+$/g, ''));
  assertEquals('sqrt(10)', items[4].innerHTML.replace(/^\s+|\s+$/g, ''));
  pass('test-get-elements-by-tag-name');
}
