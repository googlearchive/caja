function module() {
  {
    // Output CSS before any HTML that might be styled.
    IMPORTS___.emitCss___(['.', ' p {\n  color: purple\n}\n.',
                           ' p {\n  color: pink\n}']
                           .join(IMPORTS___.getIdClass___()));
    // Set up local variables required for HTML support.
    // Attach the onclick handler.
    var el___;
    var emitter___ = IMPORTS___.htmlEmitter___;
    el___ = emitter___.byId('id_4___');
    // Remove the bits the first script shouldn't see.
    emitter___.attach('id_4___');
    // Define handlers as needed.
    var c_3___ = ___.markFuncFreeze(function (event, thisNode___) {
      wasClicked(thisNode___);
    });
    el___.onclick = function (event) {
      return ___.plugin_dispatchEvent___(this, event, ___.getId(IMPORTS___), c_3___, 2);
    };
    // Remove the manufactured ID
    emitter___.rmAttr(el___, 'id');
  }
}
function module() { // The first script.
  try {
    { a(); }
  } catch (ex___) {
    ___.getNewModuleHandler().handleUncaughtException(
        ex___, onerror, 'testSafeHtmlWithNullUriPolicy', '2');
  }
}
function module() {
  {
    var el___;
    var emitter___ = IMPORTS___.htmlEmitter___;
    el___ = emitter___.byId('id_5___');
    emitter___.setAttr(el___, 'id', 'yo-' + IMPORTS___.getIdClass___());
    // Reattach the bits the second script should see.
    emitter___.discard(emitter___.attach('id_6___'));
  }
}
function module() {
  try {
    { b(); }
  } catch (ex___) {
    ___.getNewModuleHandler().handleUncaughtException(
        ex___, onerror, 'testSafeHtmlWithNullUriPolicy', '4');
  }
}
function module() {
  {
    var el___;
    var emitter___ = IMPORTS___.htmlEmitter___;
    el___ = emitter___.byId('id_7___');
    emitter___.setAttr(el___, 'href',
      IMPORTS___.rewriteUriInAttribute___('foo%3dbar?baz%28%29', 'a', 'href'));
    var c_3___ = ___.markFuncFreeze(function (event, thisNode___) {
      wasClicked(thisNode___);
    });
    el___.onclick = function (event) {
      return ___.plugin_dispatchEvent___(this, event, ___.getId(IMPORTS___), c_3___, 2);
    };
    // Pass the 'target' attribute of the <a> through the client side policy
    emitter___.setAttr(
        el___,
        'target',
        IMPORTS___.rewriteTargetAttribute___(null, 'a', 'target'));
    emitter___.rmAttr(el___, 'id');
    el___ = emitter___.byId('id_8___');
    emitter___.setAttr(el___, 'style', 'color: red; background-image: url(' +
      IMPORTS___.rewriteUriInCss___(
          'http://example.org/foo', 'background-image')
      + ')');
    emitter___.rmAttr(el___, 'id');
    el___ = emitter___.byId('id_9___');
    emitter___.setAttr(el___, 'id', 'zag-' + IMPORTS___.getIdClass___());
    el___ = emitter___.finish();
    emitter___.signalLoaded();
  }
}
