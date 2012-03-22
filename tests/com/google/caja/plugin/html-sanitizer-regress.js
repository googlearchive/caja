
// Returns a function that parses HTML and returns a normalized
// serialization of the SAX events.  In particular, sequences of adjacent
// cdata or pcdata or rcdata events get combined into a single event.

function makeParser(htmlModule) {
  var out = [];
  var handler = {
    startDoc: function() {
      out.push('+doc');
    },
    endDoc: function() {
      out.push('-doc');
    },
    startTag: function(t, a) {
      out.push('+tag', t + '[' + a.join(';') + ']');
    },
    endTag: function(t) {
      out.push('-tag', t);
    },
    pcdata: function(t) {
      var n = out.length - 2;
      if (out[n] === 'PC') { out[n + 1] += t; } else { out.push('PC', t); }
    },
    rcdata: function(t) {
      var n = out.length - 2;
      if (out[n] === 'RC') { out[n + 1] += t; } else { out.push('RC', t); }
    },
    cdata: function(t) {
      var n = out.length - 2;
      if (out[n] === 'C') { out[n + 1] += t; } else { out.push('C', t); }
    }
  };
  var parser = htmlModule.makeSaxParser(handler);
  return function(input) {
    out = [];
    parser(input);
    return out.join(' ');
  };
}

var parser0 = makeParser(html0);
var parser1 = makeParser(html1);
var parser2 = typeof html2 != 'undefined' && makeParser(html2);

function regress(test) {
  var p0 = parser0(test);
  var p1 = parser1(test);
  assertEquals(p0, p1);
  var s0 = html0.sanitize(test);
  var s1 = html1.sanitize(test);
  assertEquals(s0, s1);
  if (parser2) {
    var p2 = parser2(test);
    assertEquals(p1, p2);
    var s2 = html2.sanitize(test);
    assertEquals(s1, s2);
  }
}

jsunitRegister('testSanitizerRegress', function() {
  var tests = data.expand(data.tests.concat(data._more));
  for (var k = 0; k < tests.length; k++) {
    regress(tests[k]);
  }
  jsunitPass('testSanitizerRegress');
});
