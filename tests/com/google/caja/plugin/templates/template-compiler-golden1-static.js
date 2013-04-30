function module() {
  {
    // Set up local variables required for HTML support.
    var el___;
    var emitter___ = IMPORTS___.htmlEmitter___;
    el___ = emitter___.byId('id_4___');
    // Remove the bits the first script shouldn't see.
    emitter___.attach('id_4___');
    // Attach the onclick handler.
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
function module() {
  // The first script.
  try {
    { a(); }
  } catch (ex___) {
    ___.getNewModuleHandler().handleUncaughtException(
            ex___, onerror, 'testSafeHtmlWithStaticModuleId', '2');
  }
}
function module() {
  {
    var el___;
    var emitter___ = IMPORTS___.htmlEmitter___;
    // Attach the tail, taking the text out of the manufactured SPAN.
    emitter___.discard(emitter___.attach('id_5___'));
  }
}
function module() {
  // The second script.
 try {
   { b(); }
  } catch (ex___) {
    ___.getNewModuleHandler().handleUncaughtException(
            ex___, onerror, 'testSafeHtmlWithStaticModuleId', '4');
  }
}
function module() {
  {
    var el___;
    var emitter___ = IMPORTS___.htmlEmitter___;
    // Since the two handlers have the same text, they should share the
    // same handler function.
    el___ = emitter___.byId('id_6___');
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
    el___ = emitter___.finish();
    emitter___.signalLoaded();
  }
}
