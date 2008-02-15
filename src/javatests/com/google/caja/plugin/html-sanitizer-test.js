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


function testEmpty() { assertEquals('', html_sanitize('')); }

function testSimpleText() {
  assertEquals('hello world', html_sanitize('hello world'));
}

function testEntities() {
  assertEquals('&lt;hello world&gt;', html_sanitize('&lt;hello world&gt;'));
}

function testEntities() {
  assertEquals('<B>hello <I>world</I></B>',
               html_sanitize('<b>hello <i>world</i></b>'));
}

function testUnknownTagsRemoved() {
  assertEquals('<B>hello <I>world</I></B>',
               html_sanitize('<b>hello <bogus><i>world</i></bogus></b>'));
}

function testUnsafeTagsRemoved() {
  assertEquals('<B>hello <I>world</I></B>',
               html_sanitize('<b>hello <i>world</i>' +
                             '<script src=foo.js></script></b>'));
}

function testUnsafeAttributesRemoved() {
  assertEquals('<B>hello <I>world</I></B>',
               html_sanitize(
                   '<b>hello <i onclick="takeOverWorld(this)">world</i></b>'));
}

function testCruftEscaped() {
  assertEquals('<B>hello <I>world&lt;</I></B> &amp; tomorrow the universe',
               html_sanitize(
                   '<b>hello <i>world<</i></b> & tomorrow the universe'));
}

function testTagCruftRemoved() {
  assertEquals('<B ID="foo">hello <I>world&lt;</I></B>',
               html_sanitize('<b id="foo" / -->hello <i>world<</i></b>'));
}

function testIdsAndClassesPrefixed() {
  assertEquals(
      '<B ID="p-foo" CLASS="p-boo p-bar p-baz">hello <I>world&lt;</I></B>',
      html_sanitize(
          '<b id="foo" class="boo bar baz">hello <i>world<</i></b>',
          undefined, nmTokenPrefixer('p-')));
}

function testInvalidIdsAndClassesRemoved() {
  assertEquals(
      '<B CLASS="p-boo  p-baz">hello <I>world&lt;</I></B>',
      html_sanitize(
          ('<b id="fo,o" class="boo bar/bar baz">'
           + 'hello <i class="i*j">world<</i></b>'),
          undefined, nmTokenPrefixer('p-')));
}

function testNonStringInput() {
  var badHtml = '<b whacky=foo><script src=badness.js></script>bar</b id=foo>';
  assertEquals(
      '<B>bar</B>',
      html_sanitize({ toString: function () { return badHtml; } }));
}

function testSpecialCharsInAttributes() {
  assertEquals(
      '<B TITLE="a&lt;b &amp;&amp; c&gt;b">bar</B>',
      html_sanitize('<b title="a<b && c>b">bar</b>'));
}

function testUnclosedTags() {
  assertEquals('<DIV ID="foo">Bar<BR>Baz</DIV>',
               html_sanitize('<div id="foo">Bar<br>Baz'));
}

function testUnopenedTags() {
  assertEquals('Foo<B>Bar</B>Baz',
               html_sanitize('Foo<b></select>Bar</b></b>Baz</select>'));
}

function testUnsafeEndTags() {
  assertEquals(
      '',
      html_sanitize(
          '</meta http-equiv="refesh" content="1;URL=http://evilgadget.com">'));
}

function testEmptyEndTags() {
  assertEquals('<INPUT>', html_sanitize('<input></input>'));
}

function testOnLoadStripped() {
  assertEquals(
      '<IMG>',
      html_sanitize('<img src=http://foo.com/bar ONLOAD=alert(1)>'));
}

function testClosingTagParameters() {
  assertEquals(
      '<P>Hello world',
      html_sanitize('<p>Hello world</b style="width:expression(alert(1))">'));
}
