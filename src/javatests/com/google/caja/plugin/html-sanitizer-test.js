function nmTokenPrefixer(prefix) {
  return function (nmTokens) {
        var names = nmTokens.split(/\s+/);
        var validNames;
        for (var i = names.length; --i >= 0;) {
          // See http://www.w3.org/TR/1998/REC-xml-19980210#NT-NameChar
          // for the regex below.
          if (names[i] && !/[^\-\.0-9:A-Z_a-z]/.test(names[i])) {
            names[i] = prefix + names[i];
            validNames = true;
          } else {
            names[i] = '';
          }
        }
        return validNames ? names.join(' ') : null;
      };
}


jsunitRegister('testEmpty',
               function testEmpty() { assertEquals('', html_sanitize('')); });

jsunitRegister('testSimpleText',
               function testSimpleText() {
  assertEquals('hello world', html_sanitize('hello world'));
});

jsunitRegister('testEntities1',
               function testEntities() {
  assertEquals('&lt;hello world&gt;', html_sanitize('&lt;hello world&gt;'));
});

jsunitRegister('testEntities2',
               function testEntities() {
  assertEquals('<b>hello <i>world</i></b>',
               html_sanitize('<b>hello <i>world</i></b>'));
});

jsunitRegister('testUnknownTagsRemoved',
               function testUnknownTagsRemoved() {
  assertEquals('<b>hello <i>world</i></b>',
               html_sanitize('<b>hello <bogus><i>world</i></bogus></b>'));
});

jsunitRegister('testUnsafeTagsRemoved',
               function testUnsafeTagsRemoved() {
  assertEquals('<b>hello <i>world</i></b>',
               html_sanitize('<b>hello <i>world</i>' +
                             '<script src=foo.js></script></b>'));
});

jsunitRegister('testUnsafeAttributesRemoved',
               function testUnsafeAttributesRemoved() {
  assertEquals('<b>hello <i>world</i></b>',
               html_sanitize(
                   '<b>hello <i onclick="takeOverWorld(this)">world</i></b>'));
});

jsunitRegister('testCruftEscaped',
               function testCruftEscaped() {
  assertEquals('<b>hello <i>world&lt;</i></b> &amp; tomorrow the universe',
               html_sanitize(
                   '<b>hello <i>world<</i></b> & tomorrow the universe'));
});

jsunitRegister('testTagCruftRemoved',
               function testTagCruftRemoved() {
  assertEquals('<b id="foo">hello <i>world&lt;</i></b>',
               html_sanitize('<b id="foo" / -->hello <i>world<</i></b>'));
});

jsunitRegister('testIdsAndClassesPrefixed',
               function testIdsAndClassesPrefixed() {
  assertEquals(
      '<b id="p-foo" class="p-boo p-bar p-baz">hello <i>world&lt;</i></b>',
      html_sanitize(
          '<b id="foo" class="boo bar baz">hello <i>world<</i></b>',
          undefined, nmTokenPrefixer('p-')));
});

jsunitRegister('testInvalidIdsAndClassesRemoved',
               function testInvalidIdsAndClassesRemoved() {
  assertEquals(
      '<b class="p-boo  p-baz">hello <i>world&lt;</i></b>',
      html_sanitize(
          ('<b id="fo,o" class="boo bar/bar baz">'
           + 'hello <i class="i*j">world<</i></b>'),
          undefined, nmTokenPrefixer('p-')));
});

jsunitRegister('testNonStringInput',
               function testNonStringInput() {
  var badHtml = '<b whacky=foo><script src=badness.js></script>bar</b id=foo>';
  assertEquals(
      '<b>bar</b>',
      html_sanitize({ toString: function () { return badHtml; } }));
});

jsunitRegister('testSpecialCharsInAttributes',
               function testSpecialCharsInAttributes() {
  assertEquals(
      '<b title="a&lt;b &amp;&amp; c&gt;b">bar</b>',
      html_sanitize('<b title="a<b && c>b">bar</b>'));
});

jsunitRegister('testUnclosedTags',
               function testUnclosedTags() {
  assertEquals('<div id="foo">Bar<br>Baz</div>',
               html_sanitize('<div id="foo">Bar<br>Baz'));
});

jsunitRegister('testUnopenedTags',
               function testUnopenedTags() {
  assertEquals('Foo<b>Bar</b>Baz',
               html_sanitize('Foo<b></select>Bar</b></b>Baz</select>'));
});

jsunitRegister('testUnsafeEndTags',
               function testUnsafeEndTags() {
  assertEquals(
      '',
      html_sanitize(
          '</meta http-equiv="refesh" content="1;URL=http://evilgadget.com">'));
});

jsunitRegister('testEmptyEndTags',
               function testEmptyEndTags() {
  assertEquals('<input>', html_sanitize('<input></input>'));
});

jsunitRegister('testOnLoadStripped',
               function testOnLoadStripped() {
  assertEquals(
      '<img>',
      html_sanitize('<img src=http://foo.com/bar ONLOAD=alert(1)>'));
});

jsunitRegister('testClosingTagParameters',
               function testClosingTagParameters() {
  assertEquals(
      '<p>Hello world',
      html_sanitize('<p>Hello world</b style="width:expression(alert(1))">'));
});
