// html0 = html-sanitizer-legacy
// html1 = html-sanitizer (current)
// html2 = html-sanitizer-exp (might not exist)

var bench = (function() {
  var BAR_WIDTH = 50;
  var MAX_LABEL = 40;
  var MIN_MSEC = 100;
  var NUM_ALIGN_RIGHT = 4;
  var NUM_ALIGN_WIDTH = 3 + 1 + NUM_ALIGN_RIGHT;
  var REPEAT_SIZE = 30000;
  var num;
  var timer;

  var reportRows = [];

  function init() {
    if (!html2) {
      document.body.className += ' hide-t2';
    }
  }

  function clear() {
    if (timer) { stop(); }
    num = 0;
    reportRows = [];
    id('timestamp').innerHTML = '';
    clearReport();
  }

  function runSample() {
    runTest(id('sample').value);
  }

  function start() {
    num = 0;
    reportRows = [];
    id('timestamp').innerHTML = 'start ' + new Date();
    timer = window.setTimeout(step, 0);
  }

  function stop() {
    if (timer) {
      window.clearTimeout(timer);
      timer = void 0;
      id('timestamp').innerHTML += '<br>stopped ' + new Date();
      id('progress').innerHTML += ', stopped';
    }
  }

  function step() {
    id('progress').innerHTML = num + '/' + data.tests.length + ' done';
    if (data.tests.length <= num) {
      timer = void 0;
      id('timestamp').innerHTML += '<br>done ' + new Date();
      return;
    }
    var test = data.tests[num++];
    runTest(test);
    if (test.length && test.length < REPEAT_SIZE) {
      var n = Math.ceil(REPEAT_SIZE / test.length);
      test = n + data.repeat(test, n);
      runTest(test);
    }
    timer = window.setTimeout(step, 0);
  }

  function runTest(test) {
    var r0 = html0.sanitize(test);
    var r1 = html1.sanitize(test);
    if (r0 !== r1) {
      error('legacy, current disagree: ' + repr(test));
      return;
    }
    var r2 = html2 && html2.sanitize(test);
    if (html2 && r1 !== r2) {
      error('current, exp disagree: ' + repr(test));
    }
    var b0 = runEngine(html0, test);
    var b1 = runEngine(html1, test);
    var b2 = html2 && runEngine(html2, test);
    report(test, b0, b1, b2);
  }

  function runEngine(html, test, opt_minMsec) {
    if (!opt_minMsec) { opt_minMsec = MIN_MSEC; }
    var elapsed = 0;
    var loops = 1;
    for (; elapsed < opt_minMsec; loops *= 2) {
      var start = new Date();
      for (var k = 0; k < loops; k++) {
        var r = html.sanitize(test);
      }
      elapsed = new Date() - start;
    }
    return { loops: loops, elapsed: elapsed };
  }

  function labelFor(test) {
    var label = test.replace(/\n/g, '\\n');
    if (MAX_LABEL <= label.length) {
      label = label.substr(0, MAX_LABEL) + '...';
    }
    return repr(label);
  }

  function makeBar(className, t, best) {
    var bar = el('div', className);
    var px = Math.ceil(BAR_WIDTH * best / t);
    bar.setAttribute('style', 'width: ' + px + 'px');
    if (bar.style.setAttribute) {
      bar.style.setAttribute('width', px + 'px');  // IE < 8
    }
    bar.appendChild(el('b'));  // IE < 8 is weird about empty divs
    return bar;
  }

  function graph(t0, t1, t2, best) {
    var top = el('div');
    top.appendChild(makeBar('bar bar0', t0, best));
    top.appendChild(makeBar('bar bar1', t1, best));
    if (t2) {
      top.appendChild(makeBar('bar bar2', t2, best));
    }
    return top;
  }

  function report(test, b0, b1, b2) {
    var label = labelFor(test);
    var tr = el('tr');
    var t0 = b0.elapsed / b0.loops;
    var t1 = b1.elapsed / b1.loops;
    var t2 = b2 && (b2.elapsed / b2.loops);
    var best2 = t0 < t1 ? t0 : t1;
    var best3 = b2 && t2 < best2 ? t2 : best2;

    var tdg = el('td', 'graph');
    tdg.appendChild(graph(t0, t1, t2, best3));
    tr.appendChild(tdg);

    var td0 = el('td', 'number', numAlign(t0));
    var td1 = el('td', 'number', numAlign(t1));
    var td2 = el('td', 'number if-t2', b2 ? numAlign(t2) : '');

    tr.appendChild(td0);
    tr.appendChild(td1);
    tr.appendChild(td2);

    tr.appendChild(el('td', '', label));

    reportRows.push([best2 / t1, best2 / t0, tr]);
    reportRows.sort(function(a, b) {
      return (a[0] < b[0] ? -1 :
              b[0] < a[0] ? +1 :
              a[1] < b[1] ? +1 :
              b[1] < a[1] ? -1 :
              0);
    });

    clearReport();

    var table = id('report');
    var tbody = el('tbody');
    for (var k = 0; k < reportRows.length; ++k) {
      tbody.appendChild(reportRows[k][2]);
    }
    table.appendChild(tbody);
  }

  function clearReport() {
    var table = id('report');
    var tbody = oneChild(table, 'tbody');
    if (tbody) {
      table.removeChild(tbody);
    }
  }

  function error(s) {
    var e = id('errors');
    e.appendChild(text(s));
    e.appendChild(el('br'));
  }

  function id(s) {
    return document.getElementById(s);
  }

  function oneChild(el, tagName) {
    return el.getElementsByTagName(tagName)[0];
  }

  function el(tagName, className, content) {
    var e = document.createElement(tagName);
    if (className !== void 0) { e.className = className; }
    if (content !== void 0) { e.appendChild(text(content)); }
    return e;
  }

  function text(s) {
    return document.createTextNode(s);
  }

  function repr(s) {
    s = s.replace(/\\/, '\\\\')
      .replace(/\n/, '\\n')
      .replace(/\t/, '\\t')
      .replace(/\'/, "\\'");
    return "'" + s + "'";
  }

  function numAlign(n) {
    var s = Number(n).toFixed(NUM_ALIGN_RIGHT);
    s = data.repeat(' ', NUM_ALIGN_WIDTH - s.length) + s;
    s = s.replace(/[.]?0+$/, '');
    return s;
  }

  return {
    clear: clear,
    init: init,
    runEngine: runEngine,
    runSample: runSample,
    start: start,
    stop: stop
  };
})();

