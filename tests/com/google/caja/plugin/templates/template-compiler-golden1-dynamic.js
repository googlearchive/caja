// Output CSS first
IMPORTS___.emitCss___(['.', ' p {\n  color: purple\n}\n.',
                       ' p {\n  color: pink\n}']
                       .join(IMPORTS___.getIdClass___()));
//Set up local variables required for HTML support.
{
  // Define handlers
  IMPORTS___.c_1___ = function (event, thisNode___) {
    wasClicked(thisNode___);
  };
  // Attach the onclick handler.
  var el___;
  var emitter___ = IMPORTS___.htmlEmitter___;
  el___ = emitter___.byId('id_2___');
  // Remove the bits the first script shouldn't see.
  emitter___.attach('id_2___');
  emitter___.setAttr(el___, 'onclick',
      'return plugin_dispatchEvent___(this, event, '
      + ___.getId(IMPORTS___) + ', \'c_1___\');');
  // Remove the manufactured ID
  el___.removeAttribute('id');
}
// The first script.
try {
  { a(); }
} catch (ex___) {
  ___.getNewModuleHandler().handleUncaughtException(
      ex___, onerror, 'testSafeHtmlWithDynamicModuleId', '2');
}
{
  el___ = emitter___.byId('id_3___');
  emitter___.setAttr(el___, 'id', 'yo-' + IMPORTS___.getIdClass___());
  // Reattach the bits the second script should see.
  emitter___.discard(emitter___.attach('id_4___'));
}
try {
  { b(); }
} catch (ex___) {
  ___.getNewModuleHandler().handleUncaughtException(
      ex___, onerror, 'testSafeHtmlWithDynamicModuleId', '4');
}
{
  el___ = emitter___.byId('id_5___');
  emitter___.setAttr(
      el___, 'onclick',
      'return plugin_dispatchEvent___(this, event, '
      + ___.getId(IMPORTS___)
      + ', \'c_1___\');');
  el___.removeAttribute('id');
  el___ = emitter___.byId('id_6___');
  emitter___.setAttr(el___, 'id', 'zag-' + IMPORTS___.getIdClass___());
  el___ = emitter___.finish();
  emitter___.signalLoaded();
}
