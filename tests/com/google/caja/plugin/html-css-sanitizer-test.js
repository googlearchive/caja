// Additional tests for inline style support.

jsunitRegister('testInlineStyle',
               function testInlineStyle() {
  assertEquals('<a style="color: red">Hello</a>',
    html.sanitize('<a style="color: red;">Hello</a>')); 
});

jsunitRegister('testInlineStyle2',
               function testInlineStyle2() {
  assertEquals('<a style="color: red ; color: blue">Hello</a>',
    html.sanitize('<a style="color: red; color: blue">Hello</a>')); 
});

jsunitRegister('testInlineStyle3',
               function testInlineStyle3() {
  assertEquals('<a>Hello</a>',
    html.sanitize('<a style="">Hello</a>')); 
});

jsunitRegister('testStyleBlock',
               function testStyleBlock() {
  assertEquals('hello world',
    html.sanitize('<style>div { color: red; }</style>hello world'));
});

jsunitRegister('testIllegalInlineStyle',
               function testIllegalInlineStyle() {
  assertEquals('<p style="width: ">Hello world</p>',
    html.sanitize('<p style="width:expression(alert(1))">Hello world</p>'));
});

jsunitRegister('testUriInlineStyle',
               function testUriInlineStyle() {
  assertEquals(
      '<div style="background: url(&#34;SAFE_URI&#34;)"></div>',
      html.sanitize('<div style="background: url(http://bar);"></div>',
                    function(uri) { return 'SAFE_URI'; }, nmTokenPolicy));
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
});
