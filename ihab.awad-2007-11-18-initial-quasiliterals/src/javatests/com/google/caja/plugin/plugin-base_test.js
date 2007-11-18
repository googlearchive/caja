// Copyright (C) 2006 Google Inc.
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


// in case firebug not installed
if (!('console' in this)) {
  var console = {};
  console.log = console.debug = console.info = console.warn = console.error =
    function () { };
}

// a stub plugin meta object
var meta = {
  nsPrefix: 'nsPrefix',
  plugin: {},
  name: 'testPlugin',
  pathPrefix: '/plugin-files'
};

function testCheckUriRelative() {
  assertEquals('about:blank',
               plugin_checkUriRelative___('http://www.google.com/', meta));
  assertEquals('about:blank',
               plugin_checkUriRelative___('javascript:alert()', meta));
  assertEquals('about:blank',
               plugin_checkUriRelative___('about:blank', meta));
  assertEquals('about:blank',
               plugin_checkUriRelative___('//google.com/', meta));
  assertEquals('about:blank',
               plugin_checkUriRelative___('//google.com:80/', meta));
  assertEquals('/plugin-files/foo',
               plugin_checkUriRelative___('/foo', meta));
  assertEquals('/plugin-files/./foo',
               plugin_checkUriRelative___('./foo', meta));
  assertEquals('about:blank',
               plugin_checkUriRelative___('../foo', meta));
  assertEquals('/plugin-files/bar',
               plugin_checkUriRelative___('foo/../bar', meta));
  assertEquals('about:blank',
               plugin_checkUriRelative___('%2E./foo', meta));
  assertEquals('/plugin-files/foo?bar=baz',
               plugin_checkUriRelative___('/foo?bar=baz', meta));
  assertEquals('#anchor',
               plugin_checkUriRelative___('#anchor', meta));
}

function testGet() {
  assertEquals(4, plugin_get___([4], 0));
  assertEquals(1, plugin_get___([1, 4], 0));
  assertEquals(4, plugin_get___([1, 4], 1));
  assertEquals(2, plugin_get___([1, 4], 'length'));
  assertEquals(1, plugin_get___({ x: 1, y: 2 }, 'x'));
  assertEquals(2, plugin_get___({ x: 1, y: 2 }, 'y'));
  assertEquals(undefined, plugin_get___({ x: 1, y: 2 }, 'z'));
  assertEquals(undefined, plugin_get___({ x: 1, y: 2 }, 'constructor'));
  assertEquals(undefined, plugin_get___({ x: 1, y: 2 }, 'prototype'));
  function SomeClass() { this.x = 1; this.y = 2; };
  var inst = new SomeClass;
  assertEquals(1, plugin_get___(inst, 'x'));
  assertEquals(2, plugin_get___(inst, 'y'));
  assertEquals(undefined, plugin_get___(inst, 'z'));
  assertEquals(undefined, plugin_get___(inst, 'constructor'));
  var proto = { z: 3 };
  SomeClass.prototype = proto;
  assertEquals(1, plugin_get___(inst, 'x'));
  assertEquals(2, plugin_get___(inst, 'y'));
  assertEquals(undefined, plugin_get___(inst, 'constructor'));
  assertEquals(proto, plugin_get___(SomeClass, 'prototype'));
}

function testHtml() {
  assertEquals('', plugin_html___(''));
  assertEquals('foo bar', plugin_html___('foo bar'));
  assertEquals('1 &lt; 2 &amp;&amp; 4 &gt; 3',
               plugin_html___('1 < 2 && 4 > 3'));
  // html that has been blessed is not html escaped
  assertEquals('<p>Hello</p>',
               plugin_html___(plugin_blessHtml___('<p>Hello</p>')));
}

function testHtmlAttr() {
  assertEquals('', plugin_htmlAttr___(''));
  assertEquals('foo bar', plugin_htmlAttr___('foo bar'));
  assertEquals('foo &quot;boo&quot; bar', plugin_htmlAttr___('foo "boo" bar'));
  assertEquals('Apostrophic isn&#39;t it',
               plugin_htmlAttr___('Apostrophic isn\'t it'));
  assertEquals(39, "'".charCodeAt(0));
  assertEquals('1 &lt; 2 &amp;&amp; 4 &gt; 3',
               plugin_htmlAttr___('1 < 2 && 4 > 3'));
  // html that has been blessed is not valid in an attribute value so escape
  assertEquals('&lt;p&gt;Hello&lt;/p&gt;',
               plugin_htmlAttr___(plugin_blessHtml___('<p>Hello</p>')));
}

function testBlessHtml() {
  assertEquals('', plugin_blessHtml___('').toString());
  assertEquals('foo bar', plugin_blessHtml___('foo bar').toString());
  assertEquals('<p>foo bar', plugin_blessHtml___('<p>foo bar').toString());
}

function testCss() {
  assertEquals(
      'blank', '', plugin_blessCss___('').toString());
  assertEquals(
      'not-blank', 'left: 40px', plugin_blessCss___('left: 40px').toString());
  assertEquals(
      'is object', 'object', typeof plugin_blessCss___('left: 40px'));
}

function testDomNode() {
  var is_ie = 'navigator' in window && /\bmsie\b/i.test(navigator.userAgent);

  var styleNode = document.getElementById('styles');
  var wrappedStyleNode = plugin_domNode___(styleNode, meta.nsPrefix);

  assertEquals('style node type', 1, wrappedStyleNode.getNodeType());
  assertEquals('style node name', 'STYLE', wrappedStyleNode.getNodeName());
  assertEquals(null, wrappedStyleNode.getId());  // wrong prefix
  assertEquals('', wrappedStyleNode.getClass());  // no class
  wrappedStyleNode.setClass('foo');
  assertEquals('nsPrefix-foo', trim(styleNode.className));
  assertEquals('foo', wrappedStyleNode.getClass());
  assertEquals('#hello-world > div { color: blue }',
               trim(wrappedStyleNode.getInnerHTML().toString()));
  // a style node's content is not html so if it's to be treated as html it must
  // be escaped
  assertEquals('#hello-world &gt; div { color: blue }',
               trim(plugin_html___(wrappedStyleNode.getInnerHTML())));
  if (!is_ie) {  // styles don't contain text nodes in IE's DOM
    // the style node contains some text
    assertEquals(3, wrappedStyleNode.getFirstChild().getNodeType());
    assertEquals(null, wrappedStyleNode.getFirstChild().getNextSibling());
  }
  assertEquals('text/css', wrappedStyleNode.getAttribute('type'));

  var header = document.getElementById('nsPrefix-header');
  var wrappedHeader = plugin_domNode___(header, meta.nsPrefix);

  assertEquals(1, wrappedHeader.getNodeType());
  assertEquals('H1', wrappedHeader.getNodeName());
  assertEquals('header', wrappedHeader.getId());  // prefix is stripped
  // two classes w/ right prefix
  assertEquals('foo baz', wrappedHeader.getClass());
  // removes nsPrefix-baz and leaves the unprefixed one alone
  wrappedHeader.setClass('foo');
  assertEquals('bar nsPrefix-foo', trim(header.className));
  assertEquals('foo', wrappedHeader.getClass());
  assertEquals('header <b>text</b>',
               wrappedHeader.getInnerHTML().toString().toLowerCase());
  // the content is safe so escaping it does nothing.
  assertEquals('header <b>text</b>',
               plugin_html___(wrappedHeader.getInnerHTML()).toLowerCase());
  // the style node contains some text and a <b> node
  assertEquals(3, wrappedHeader.getFirstChild().getNodeType());
  assertEquals(1, wrappedHeader.getFirstChild().getNextSibling().getNodeType());
  assertEquals(
      'B', wrappedHeader.getFirstChild().getNextSibling().getNodeName());
  assertEquals(null, wrappedHeader.getFirstChild()
               .getNextSibling().getNextSibling());
  var xmp = document.getElementById('foo-unsafe');
  var wrappedXmp = plugin_domNode___(xmp, meta.nsPrefix);

  assertEquals(1, wrappedXmp.getNodeType());
  assertEquals('XMP', wrappedXmp.getNodeName());
  assertEquals(null, wrappedXmp.getId());  // wrong prefix
  // no classes
  assertEquals('', wrappedXmp.getClass());
  wrappedXmp.setClass('ahoy');
  assertEquals('nsPrefix-ahoy', trim(xmp.className));
  assertEquals('ahoy', wrappedXmp.getClass());
  assertEquals('Not html in <here>',
               wrappedXmp.getInnerHTML().toString());
  // the content is unsafe so escaping it has an effect
  assertEquals('Not html in &lt;here&gt;',
               plugin_html___(wrappedXmp.getInnerHTML()));
  // the style node contains one text node
  assertEquals(3, wrappedXmp.getFirstChild().getNodeType());
  assertEquals(null, wrappedXmp.getFirstChild().getNextSibling());
  assertEquals('[DomNode XMP]', wrappedXmp.toString());

  var divvy = document.getElementById('divvy');
  var wrappedDivvy = plugin_domNode___(divvy, meta.nsPrefix);
  assertEquals('background-color: red',
               wrappedDivvy.getStyle().toString().toLowerCase().replace(/;$/, ''));
}

function testPrefix() {
  assertEquals('', plugin_prefix___('', meta));
  assertEquals('nsPrefix-foo', plugin_prefix___('foo', meta));
  assertEquals('nsPrefix-foo nsPrefix-bar',
               plugin_prefix___('foo bar', meta));
  assertEquals('nsPrefix-foo nsPrefix-bar',
               plugin_prefix___('foo ### bar', meta));
  assertEquals('nsPrefix-foo nsPrefix-bar',
               plugin_prefix___(' foo  bar ', meta));
}

function testRequire() {
  plugin_require___(true);
  try {
    plugin_require___(false);
    fail('require exited normally on false input');
  } catch (e) {
    // happy
  }
}

function testWrap() {
  assertEquals(null, plugin_wrap___(null));
  assertEquals(undefined, plugin_wrap___(undefined));
  assertEquals('', plugin_wrap___(''));
  assertEquals('foo', plugin_wrap___('foo'));
  assertEquals(4, plugin_wrap___(4));

  var obj = { x: 4, y: 5 };
  var wrapped = plugin_wrap___(obj);
  assertEquals(4, wrapped.x);
  assertEquals(5, wrapped.y);
  wrapped.x = 3;
  assertEquals(3, wrapped.x);
  assertEquals(4, obj.x);
  obj.y = 2;
  assertEquals(2, wrapped.y);
  assertEquals(2, obj.y);

  function f() { }
  var proto = {};
  f.prototype = proto;
  var wrappedFn = plugin_wrap___(f);
  assertEquals(wrappedFn.prototype, f.prototype);
  wrappedFn.x = 4;
  assertEquals(4, wrappedFn.x);
  assertEquals(undefined, f.x);

  var re = /a\b/;
  var wrappedRe = plugin_wrap___(re);
  assertTrue(wrappedRe.test('a'));
  assertTrue(wrappedRe.test('ba'));
  assertFalse(wrappedRe.test('ab'));
}

function testCssNumber() {
  assertEquals('0', plugin_cssNumber___(0));
  assertEquals('1', plugin_cssNumber___(1));
  assertEquals('-1', plugin_cssNumber___(-1));
  assertEquals('1.5', plugin_cssNumber___(1.5));
  assertEquals('0', plugin_cssNumber___(Infinity));
  assertEquals('0', plugin_cssNumber___(-Infinity));
  assertEquals('0', plugin_cssNumber___(NaN));
  assertEquals('0', plugin_cssNumber___(null));
  assertEquals('0', plugin_cssNumber___(''));
  assertEquals('0', plugin_cssNumber___('foo'));
}

function testCssColor() {
  assertEquals('#000', plugin_cssColor___(0));
  assertEquals('#abcdef', plugin_cssColor___(0xabcdef));
  assertEquals('#ff0000', plugin_cssColor___(0xff0000));
  assertEquals('#00ff00', plugin_cssColor___(0x00ff00));
  assertEquals('#0000ff', plugin_cssColor___(0x0000ff));
  assertEquals('#ffffff', plugin_cssColor___(-1));
  assertEquals('#000001', plugin_cssColor___(1.5));
  assertEquals('#000000', plugin_cssColor___(Infinity));
  assertEquals('#000000', plugin_cssColor___(-Infinity));
  assertEquals('#000', plugin_cssColor___(NaN));
  assertEquals('#000', plugin_cssColor___(null));
  assertEquals('#000', plugin_cssColor___(''));
  assertEquals('#000', plugin_cssColor___('foo'));
}

function testCssUri() {
  assertEquals("url('/plugin-files/foo.html')",
               plugin_cssUri___('foo.html', meta));
  assertEquals("url('about:blank')",
               plugin_cssUri___("foo.html');background:uri('//evil.com/",
               meta));
}

function trim(s) {
  return s.replace(/^[ \r\n]+|[ \r\n]+$/g, '');
}
