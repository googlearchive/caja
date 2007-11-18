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
