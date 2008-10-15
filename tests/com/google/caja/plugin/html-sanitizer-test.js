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

/**
 * Strips unsafe tags and attributes from html.
 * @param {string} html to sanitize
 * @param {Function} opt_urlXform : string -> string? -- a transform to apply to
 *     url attribute values.
 * @param {Function} opt_nmTokenXform : string -> string? -- a transform to
 *     apply to names, ids, and classes.
 * @return {string} html
 */
function html_sanitize(htmlText, opt_urlPolicy, opt_nmTokenPolicy) {
  var out = [];
  html.makeHtmlSanitizer(
      function sanitizeAttribs(tagName, attribs) {
        for (var i = 0; i < attribs.length; i += 2) {
          var attribName = attribs[i];
          var value = attribs[i + 1];
          if (html4.ATTRIBS.hasOwnProperty(attribName)) {
            switch (html4.ATTRIBS[attribName]) {
              case html4.atype.SCRIPT:
              case html4.atype.STYLE:
                value = null;
              case html4.atype.IDREF:
              case html4.atype.NAME:
              case html4.atype.NMTOKENS:
                value = opt_nmTokenPolicy ? opt_nmTokenPolicy(value) : value;
                break;
              case html4.atype.URI:
                value = opt_urlPolicy && opt_urlPolicy(value);
                break;
            }
          } else {
            value = null;
          }
          attribs[i + 1] = value;
        }
        return attribs;
      })(htmlText, out);
  return out.join('');
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
      '<p>Hello world</p>',
      html_sanitize('<p>Hello world</b style="width:expression(alert(1))">'));
});

jsunitRegister('testOptionalEndTags',
               function testOptionalEndTags() {
  // The difference is significant because in the first, the item contains no
  // space after 'A', but in the third, the item contains 'C' and a space.
  assertEquals(
      '<ol> <li>A</li> <li>B<li>C </ol>',
      html_sanitize('<ol> <li>A</li> <li>B<li>C </ol>'));
});

jsunitRegister('testFoldingOfHtmlAndBodyTags',
               function testFoldingOfHtmlAndBodyTags() {
  assertEquals(
      '<p>P 1</p>',
      html_sanitize('<html><head><title>Foo</title></head>'
                    + '<body><p>P 1</p></body></html>'));
  assertEquals(
      'Hello',
      html_sanitize('<body bgcolor="blue">Hello</body>'));
  assertEquals(
      '<p>Foo</p><p>One</p><p>Two</p>Three<p>Four</p>',
      html_sanitize(
          '<html>'
          + '<head>'
          + '<title>Blah</title>'
          + '<p>Foo</p>'
          + '</head>'
          + '<body>'
          + '<p>One</p>'
          + '<p>Two</p>'
          + 'Three'
          + '<p>Four</p>'
          + '</body>'
          + '</html>'));
});

jsunitRegister('testEmptyAndValuelessAttributes',
               function testEmptyAndValuelessAttributes() {
  assertEquals(
      '<input checked="checked" type="checkbox" id="" class="">',
      html_sanitize('<input checked type=checkbox id="" class=>'));
  assertEquals(
      '<input checked="checked" type="checkbox" id="" class="">',
      html_sanitize('<input checked type=checkbox id= class="">'));
  assertEquals(
      '<input checked="checked" type="checkbox" id="" class="">',
      html_sanitize('<input checked type=checkbox id= class = "">'));
});

jsunitRegister('testSgmlShortTags',
               function testSgmlShortTags() {
  // We make no attempt to correctly handle SGML short tags since they are
  // not implemented consistently across browsers, and have been removed from
  // HTML 5.
  //
  // According to http://www.w3.org/QA/2007/10/shorttags.html
  //      Shorttags - the odd side of HTML 4.01
  //      ...
  //      It uses an ill-known feature of SGML called shorthand markup, which
  //      was authorized in HTML up to HTML 4.01. But what used to be a "cool"
  //      feature for SGML experts becomes a liability in HTML, where the
  //      construct is more likely to appear as a typo than as a conscious
  //      choice.
  //
  //      All could be fine if this form typo-that-happens-to-be-legal was
  //      properly implemented in contemporary HTML user-agents. It is not.
  assertEquals('', html_sanitize('<p/b/'));  // Short-tag discarded.
  assertEquals('<p></p>', html_sanitize('<p<b>'));  // Discard <b attribute
  assertEquals(
      '<p>first part of the text&lt;/&gt; second part</p>',
      html_sanitize('<p<a href="/">first part of the text</> second part'));
});

jsunitRegister('testNul',
               function testNul() {
  // See bug 614 for details.
  assertEquals(
      '<a alt="harmless  SCRIPT&#61;javascript:alert(1) ignored&#61;ignored">'
      + '</a>',
      html_sanitize(
          '<A ALT="harmless\0  SCRIPT=javascript:alert(1) ignored=ignored">'));
});
