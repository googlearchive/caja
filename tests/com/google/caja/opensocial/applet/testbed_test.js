jsunitRegister('testIndentAndWrapCode', function () {
  assertEquals('<br>', indentAndWrapCode(''));
  assertEquals('<div class="line-of-code">foo</div>', indentAndWrapCode('foo'));
  assertEquals(
      ''
      + '<div class="indentedblock">'
        + '<div class="line-of-code">'
          + 'foo'
        + '</div>'
      + '</div>',
      indentAndWrapCode('  foo'));
  assertEquals(
      ''
      + '<div class="line-of-code">'
        + 'foo'
      + '</div>'
      + '<div class="indentedblock">'
        + '<div class="line-of-code">'
          + 'bar'
        + '</div>'
      + '</div>',
      indentAndWrapCode(
          ''
	  + 'foo\n'
	  + '  bar'));
  assertEquals(
      ''
      + '<div class="line-of-code">'
        // don't break on spaces.
        + 'function\xA0foo(<wbr>)<wbr>\xA0{<wbr>'
      + '</div>'
      + '<div class="indentedblock">'
        + '<div class="line-of-code">'
          + 'var\xA0bar\xA0=\xA0baz(<wbr>'
        + '</div>'
        + '<div class="indentedblock">'
          + '<div class="line-of-code">'
            + 'a,<wbr>\xA0b,<wbr>\xA0&quot;c,\xA0d&quot;)<wbr>;'
          + '</div>'
        + '</div>'
        + '<div class="line-of-code">'
          // ampersands excaped
          + 'return\xA0useBar(<wbr>)<wbr>\xA0&amp;&amp;\xA0bar;'
        + '</div>'
      + '</div>'
      + '<div class="line-of-code">'
        + '}<wbr>'
      + '</div>',
      indentAndWrapCode(
          ''
	  + 'function foo() {\n'
	  + '  var bar = baz(\n'
	  + '      a, b, "c, d");\n'
	  + '  return useBar() && bar;\n'
	  + '}'));
});
