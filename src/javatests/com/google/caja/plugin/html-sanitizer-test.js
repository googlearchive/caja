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
  assertEquals('<b>hello <i>world</i></b>',
               html_sanitize('<b>hello <i>world</i></b>'));
}

function testUnknownTagsRemoved() {
  assertEquals('<b>hello <i>world</i></b>',
               html_sanitize('<b>hello <bogus><i>world</i></bogus></b>'));
}

function testUnsafeTagsRemoved() {
  assertEquals('<b>hello <i>world</i></b>',
               html_sanitize('<b>hello <i>world</i>' +
                             '<script src=foo.js></script></b>'));
}

function testUnsafeAttributesRemoved() {
  assertEquals('<b>hello <i >world</i></b>',
               html_sanitize(
                   '<b>hello <i onclick="takeOverWorld(this)">world</i></b>'));
}

function testCruftEscaped() {
  assertEquals('<b>hello <i>world&lt;</i></b> &amp; tomorrow the universe',
               html_sanitize(
                   '<b>hello <i>world<</i></b> & tomorrow the universe'));
}

function testTagCruftRemoved() {
  assertEquals('<b ID="foo"  >hello <i>world&lt;</i></b>',
               html_sanitize('<b id="foo" / -->hello <i>world<</i></b>'));
}

function testIdsAndClassesPrefixed() {
  assertEquals(
      '<b ID="p-foo" CLASS="p-boo p-bar p-baz">hello <i>world&lt;</i></b>',
      html_sanitize(
          '<b id="foo" class="boo bar baz">hello <i>world<</i></b>',
          undefined, nmTokenPrefixer('p-')));
}

function testInvalidIdsAndClassesRemoved() {
  assertEquals(
      '<b  CLASS="p-boo  p-baz">hello <i >world&lt;</i></b>',
      html_sanitize(
          ('<b id="fo,o" class="boo bar/bar baz">'
           + 'hello <i class="i*j">world<</i></b>'),
          undefined, nmTokenPrefixer('p-')));
}

function testNonStringInput() {
  var badHtml = '<b whacky=foo><script src=badness.js></script>bar</b id=foo>';
  assertEquals(
      '<b >bar</b >',
      html_sanitize({ toString: function () { return badHtml; } }));
}

function testSpecialCharsInAttributes() {
  assertEquals(
      '<b TITLE="a&lt;b &amp;&amp; c&gt;b">bar</b>',
      html_sanitize('<b title="a<b && c>b">bar</b>'));
}
