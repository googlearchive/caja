{
  // Define handlers
  IMPORTS___.c_1___ = function (event, thisNode___) {
    wasClicked(thisNode___);
  };
  // Set up local variables required for HTML support.
  var el___;
  var emitter___ = IMPORTS___.htmlEmitter___;
  el___ = emitter___.byId('id_2___');
  // Remove the bits the first script shouldn't see.
  emitter___.attach('id_2___');
  // Attach the onclick handler.
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
      ex___, onerror, 'testSafeHtmlWithStaticModuleId', '2');
}
{
  //Attach the tail, taking the text out of the manufactured SPAN.
  emitter___.discard(emitter___.attach('id_3___'));
}
// The second script.
try {
  { b(); }
} catch (ex___) {
  ___.getNewModuleHandler().handleUncaughtException(
      ex___, onerror, 'testSafeHtmlWithStaticModuleId', '4');
}
{
  // Since the two handlers have the same text, they should share the
  // same handler function.
  el___ = emitter___.byId('id_4___');
  emitter___.setAttr(
      el___, 'onclick',
      'return plugin_dispatchEvent___(this, event, '
      + ___.getId(IMPORTS___)
      + ', \'c_1___\');');
  el___.removeAttribute('id');
  el___ = emitter___.finish();
  emitter___.signalLoaded();
}