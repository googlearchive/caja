// Additional tests for inline style support.

jsunitRegister('testInlineStyle',
               function testInlineStyle() {
  assertEquals('<a style="color: red">Hello</a>',
    html.sanitize('<a style="color: red;">Hello</a>')); 
  jsunit.pass();
});

jsunitRegister('testInlineStyle2',
               function testInlineStyle2() {
  assertEquals('<a style="color: red ; color: blue">Hello</a>',
    html.sanitize('<a style="color: red; color: blue">Hello</a>')); 
  jsunit.pass();
});

jsunitRegister('testInlineStyle3',
               function testInlineStyle3() {
  assertEquals('<a>Hello</a>',
    html.sanitize('<a style="">Hello</a>')); 
  jsunit.pass();
});

jsunitRegister('testStyleBlock',
               function testStyleBlock() {
  assertEquals('hello world',
    html.sanitize('<style>div { color: red; }</style>hello world'));
  jsunit.pass();
});

jsunitRegister('testIllegalInlineStyle',
               function testIllegalInlineStyle() {
  assertEquals('<p style="width: ">Hello world</p>',
    html.sanitize('<p style="width:expression(alert(1))">Hello world</p>'));
  jsunit.pass();
});

jsunitRegister('testUriInlineStyle',
               function testUriInlineStyle() {
  assertEquals(
      '<div style="background: url(&#34;SAFE_URI&#34;)"></div>',
      html.sanitize('<div style="background: url(http://bar);"></div>',
                    function(uri) { return 'SAFE_URI'; }, nmTokenPolicy));
  jsunit.pass();
});

jsunitRegister('testWeakUriRewriter',
               function testWeakUriRewriter() {
  assertEquals(
      '<div style="background: "></div>',
      html_sanitize(
          '<div style="background: url(javascript:1)"></div>',
          function (uri) { return uri; }));
  assertEquals(
      '<div style="background: "></div>',
      html_sanitize(
          '<div style="background: url(invalid:1)"></div>',
          function (uri) { return uri; }));
  jsunit.pass();
});

jsunitRegister('testUriHints',
               function testUriHints() {
  assertEquals('<div style="background: url(&#34;img.jpg&#34;)">test</div>',
      html.sanitize('<div style="background: url(img.jpg)">test</div>',
        function(uri, effect, ltype, hints) {
      assertEquals("CSS", hints.TYPE);
      assertEquals("background", hints.CSS_PROP);
      return uri;
  }));
  jsunit.pass();
});
