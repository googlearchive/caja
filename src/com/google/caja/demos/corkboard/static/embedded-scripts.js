// See the file LICENSE.txt in this directory for license information for the
// Caja Corkboard demo.

// Tools for handling the case where a web page contains many cajoled gadgets
// included inline, which should be executed only after the Caja runtime has
// been asynchronously loaded.
//
// First, call registerForScript(vdocId, moduleText) for each gadget, where
// vdocId is the id (string) of the DOM element to attach the gadget to, and
// moduleText is the JS source of a *cajoled* module. DO NOT PASS USER-SUPPLIED
// SCRIPT as moduleText, or you lose all security because it will just be
// eval()ed.
//
// After all scripts have been registered and the page has loaded (say, in
// <body onload="...">), call loadScripts(), which will load the Caja runtime
// and attach each script.
//
// @author kpreid@switchb.org

var registerForScript, loadScripts;
(function () {
  var scriptHooks = [];

  var uriPolicy = {
    // TODO(kpreid): have a sensible default instead of this app needing it
    // In particular, in cajole.py we specify "sext=false" (which is itself a
    // temporary kludge); there should be a single switch, or at least two
    // similarly-exzpressed ones, which do that and also change the client-side
    // policy.
    rewrite: function (uri, mimeType) {
      if ((/^https?:/i).test(uri)) {
        // TODO: unsafe, need to check mimeType but that's not sufficient
        return uri;
      } else {
        return "data:,URI%20rejected";
      }
    }
  };

  registerForScript = function (vdocId, moduleText) {
    scriptHooks.push([vdocId, moduleText]);
  }

  loadScripts = function (server) {
    caja.initialize({
      cajaServer: server,
      debug: true
    });
    for (var i = 0; i < scriptHooks.length; i++) {
      (function (id, moduleText) {
        caja.load(document.getElementById(id), uriPolicy, function (frame) {
          frame.cajoled(undefined, moduleText, undefined).run();
        });
      }).apply(undefined, scriptHooks[i]);
    }
    scriptHooks = [];
  }
})();
