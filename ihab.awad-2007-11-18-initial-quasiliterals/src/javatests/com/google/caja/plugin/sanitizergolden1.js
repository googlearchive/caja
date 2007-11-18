{
  function foo(a, b) {
    plugin_require___(this !== window);
    if (a > 0 && b > 0) {
      return foo.call(this, b, a - 1);
    }
    try {
      bar.call(this);
    } catch (safe_ex___) {
      var e = '' + safe_ex___;
      alert.call(this, e);
    }
  }
  var a = plugin_get___(bar, baz);
  eval.call(this, 'badness()');
}
