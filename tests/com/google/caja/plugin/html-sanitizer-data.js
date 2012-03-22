var data = (function() {
  function repeat(s, n) {
    // note, this turns NaN into 0
    if (!(0 <= n)) { n = 0; }
    return Array(n + 1).join(s);
  }

  function expand(tests) {
    var seen = {};
    var expanded = [];
    var add = function(test) {
      if (!seen.hasOwnProperty(test)) {
        seen[test] = true;
        expanded.push(test);
      }
    };
    for (var k = 0; k < tests.length; k++) {
      var test = tests[k];
      add(test);
      add(' ' + test);
      add(test + ' ');
      // \n is special because it doesn't get matched by /./
      add('\n' + test.replace(/ /g, '\n') + '\n');
      add(test.replace(/\"/g, '\''));
      add(test.replace(/\"/g, ''));
    }
    return expanded;
  }

  // html-sanitizer-bench uses tests and ignores _more
  // html-sanitizer-regress uses both tests and _more
  var tests = [];
  var _more = [];

  tests.push('<div><</div>a>b</div>c');
  _more.push('<div>></div>a>b</div>c');
  _more.push('<div>&</div>a>b</div>c');
  tests.push('<div><?</div>a>b</div>c');
  tests.push('<div><!</div>a>b</div>c');
  tests.push('<div><!--</div>a>b-->c</div>d');
  tests.push('<div></</div>a>b</div>c');

  tests.push('<script><</script>a>b</script>c');
  _more.push('<script>></script>a>b</script>c');
  _more.push('<script>&</script>a>b</script>c');
  _more.push('<script><?</script>a>b</script>c');
  _more.push('<script><!</script>a>b</script>c');
  _more.push('<script><!--</script>a>b-->c</script>d');
  _more.push('<script></</script>a>b</script>c');

  tests.push('<textarea><</textarea>a>b</textarea>c');
  _more.push('<textarea>></textarea>a>b</textarea>c');
  _more.push('<textarea>&</textarea>a>b</textarea>c');
  _more.push('<textarea><?</textarea>a>b</textarea>c');
  _more.push('<textarea><!</textarea>a>b</textarea>c');
  _more.push('<textarea><!--</textarea>a>b-->c</textarea>d');
  _more.push('<textarea></</textarea>a>b</textarea>c');

  tests.push('<script>' + repeat(' && ', 100) + '</script>');
  tests.push('<script>' + repeat(' </ ', 100) + '</script>');
  tests.push('<textarea>' + repeat(' && ', 100) + '</textarea>');

  tests.push('');

  tests.push('&');
  tests.push('&;');

  tests.push('&amp');
  tests.push('&amp;');
  tests.push('&bad;');
  _more.push('&gt;');
  _more.push('&lt;');
  _more.push('&quot;');

  tests.push('&#0');
  tests.push('&#0;');
  _more.push('&#1;');
  tests.push('&#1234567890;');
  tests.push('&#a');
  tests.push('&#a;');

  _more.push('&#x0');
  tests.push('&#x0;');
  _more.push('&#x1;');
  tests.push('&#xa');
  tests.push('&#xa;');
  tests.push('&#x9abcdef');
  tests.push('&#x9abcdef;');
  tests.push('&#xghi');
  tests.push('&#xghi;');

  tests.push('&=');
  tests.push('&>');
  _more.push('&<');
  _more.push('&&');

  tests.push('>');

  _more.push('<!--');
  _more.push('<!---->');
  _more.push('<!-- -->');
  _more.push('<!-- -- >');

  tests.push('<!-- a');
  tests.push('<!-- a -- b');
  tests.push('<!--a-->b');
  _more.push('<!-- a --> b');

  tests.push('<!-->a-->b>c');
  _more.push('<!--> a --> b > c');
  tests.push('<!--->a-->b>c');
  _more.push('<!---> a --> b > c');
  tests.push('<!---->a-->b>c');
  _more.push('<!----> a --> b > c');
  tests.push('<!-- a -- b -->c>d');
  _more.push('<!-- a -- b --> c > d');
  tests.push('<!-- a -- >b>c');
  _more.push('<!-- a -- > b > c');
  tests.push('<!- a -- >b>c');
  _more.push('<!- a -- > b > c');
  tests.push('<!--a<!--b-->c>d');
  _more.push('<!-- a <!-- b --> c > d');

  _more.push('<!doc');
  tests.push('<!doc a');
  tests.push('<!doc>a>b');
  _more.push('<!doc > a > b');
  tests.push('<! doc > a > b');
  tests.push('<!doc x>a>b');
  _more.push('<!doc x > a > b');
  tests.push('<! doc x > a > b');
  tests.push('<!doc<!-->a>b');
  _more.push('<!doc <!-- > a > b');
  tests.push('<!doc<>a>b');
  _more.push('<!doc < > a > b');

  _more.push('<!');
  tests.push('<! a');

  tests.push('<!<>a>b');
  _more.push('<!< > a > b');
  tests.push('<! < > a > b');

  _more.push('<?');
  tests.push('<? a');

  tests.push('<?<>a>b');
  _more.push('<?< > a > b');
  tests.push('<? < > a > b');

  tests.push('<?<!>a>b');
  _more.push('<?<! > a > b');
  tests.push('<? <! > a > b');

  tests.push('<?<!-->a>b');
  _more.push('<?<!-- > a > b');
  tests.push('<? <!-- > a > b');

  tests.push('<?<?>a>b');
  _more.push('<?<? > a > b');
  tests.push('<? <? > a > b');

  tests.push('<?hcf<>a');
  _more.push('<?hcf< > a');
  tests.push('<?hcf < > a');

  _more.push('</');
  tests.push('</ a');
  tests.push('</>a');
  _more.push('</ > a');
  tests.push('</ a>b');
  _more.push('</ a> b');

  tests.push('</a');
  tests.push('</a>b>');
  _more.push('</a > b >');

  tests.push('</a b>c>d');
  _more.push('</a b > c > d');
  tests.push('</a b/>c>d');
  _more.push('</a b /> c > d');

  tests.push('</a "b"/>c>d');
  _more.push('</a "b" /> c > d');

  tests.push('</a b="c">d>e');
  _more.push('</a b="c" > d > e');
  tests.push('</a b = "c" > d > e');

  tests.push('</a b="c">d>e');
  _more.push('</a b="c" > d > e');
  tests.push('</a b = "c" > d > e');

  tests.push('</a b="c>d&quot;">e>f');
  _more.push('</a b="c > d &quot; " > e > f');
  tests.push('</a b = "c > d &quot; " > e > f');

  tests.push('<');
  _more.push('<>');

  tests.push('<<a');
  tests.push('<<a>b');
  _more.push('<< a > b');

  tests.push('<&+');
  tests.push('<&+>b');
  _more.push('<& + > b');

  tests.push('< a');
  tests.push('< a>b');
  _more.push('< a > b');

  tests.push('<>a');
  tests.push('< > a');

  tests.push('<p');
  tests.push('<p>b');
  tests.push('<p > b');

  tests.push('<p title>b');
  tests.push('<p title=>b');
  tests.push('<p title="">b');
  tests.push('<p title=a>b');
  tests.push('<p title="a">b');
  tests.push('<p title =a>b');
  tests.push('<p title= a>b');
  tests.push('<p title = a>b');
  tests.push('<p title title>b');
  tests.push('<p title title=a>b');
  tests.push('<p title =title=a>b');

  tests.push('<p title=<>b');
  _more.push('<p title=<<>b');
  tests.push('<p title=' + repeat('<', 1000) + '>b');

  tests.push('<p title="<a">b');
  _more.push('<p title="<a<a">b');
  tests.push('<p title="' + repeat('<', 1000) + '">b');

  tests.push('<p title=>>b');
  _more.push('<p title=>>>b');
  tests.push('<p title=' + repeat('>', 1000) + '>b');

  tests.push('<p title=">">b');
  _more.push('<p title=">>">b');
  tests.push('<p title="' + repeat('>', 1000) + '">b');

  tests.push('<p title="&">b');
  _more.push('<p title="&#">b');
  tests.push('<p title="&#0">b');
  tests.push('<p title="&#0;">b');
  _more.push('<p title="&#33">b');
  _more.push('<p title="&#33;">b');

  tests.push('<p x title y>b');
  tests.push('<p x= title= y=>b');
  tests.push('<p x="" title="" y="">b');
  tests.push('<p x="1" title="2" y="3">b');
  tests.push('<p x="<" title="<" y="<">b');
  tests.push('<p x=">" title=">" y=">">b');

  _more.push(repeat('<p>', 100));
  _more.push(repeat('<p title=x>', 100));

  tests.push('<p' + repeat(' title=a', 100) + '>b');
  tests.push('<p' + repeat(' title="a"', 100) + '>b');
  tests.push('<p' + repeat(' title=">"', 100) + '>b');

  tests.push('<a>');
  tests.push('<a>1');
  _more.push('<a> 1 ');
  tests.push('<a></a>');
  tests.push('<a>1</a>');
  _more.push('<a> 1 </a>');
  tests.push('<a></a>2');
  tests.push('<a>1</a>2');
  _more.push('<a> 1 </a> 2');

  tests.push('<a><b><c>');
  tests.push('<a>1<b>2<c>3');
  tests.push('<a>1<b>2<c>3</a>4');
  _more.push('<a>1<b>2<c>3</b>4');
  _more.push('<a>1<b>2<c>3</c>4');

  tests.push('<a>1<b>2<c>3</a>4</a>5');
  _more.push('<a>1<b>2<c>3</a>4</b>5');
  _more.push('<a>1<b>2<c>3</a>4</c>5');

  _more.push('<a>1<b>2<c>3</b>4</a>5');
  _more.push('<a>1<b>2<c>3</b>4</b>5');
  _more.push('<a>1<b>2<c>3</b>4</c>5');

  _more.push('<a>1<b>2<c>3</c>4</a>5');
  _more.push('<a>1<b>2<c>3</c>4</b>5');
  _more.push('<a>1<b>2<c>3</c>4</c>5');

  tests.push(repeat('<a><b>', 100) + repeat('</b></a>', 100));
  tests.push(repeat('<a><b>', 100) + repeat('</c></b>', 100));

  return {
    expand: expand,
    repeat: repeat,
    tests: tests,
    _more: _more
  };
})();
