{
  ___OUTERS___.foo = ___.simpleFunc(function foo(a, b) {
      if (a > 0 && b > 0) {
        return ___.asSimpleFunc(___.primFreeze(foo))(b, a - 1);
      }
      try {
        ___.asSimpleFunc(___OUTERS___.bar)();
      } catch (e) {
        ___.asSimpleFunc(___OUTERS___.alert)(e);
      }
    });
  ___OUTERS___.a = ___.readPub(___OUTERS___.bar, ___OUTERS___.baz);
  ___.asSimpleFunc(___OUTERS___.eval)('badness()');
}
