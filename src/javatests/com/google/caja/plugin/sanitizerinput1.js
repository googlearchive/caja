
function foo(a, b) {
  if (a > 0 && b > 0) {
    return foo(b, a - 1);
  }
  try {
    bar();
  } catch (e) {
    alert(e);
  }
}

var a = bar[baz];


eval('badness()');
