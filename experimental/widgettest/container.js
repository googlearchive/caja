function cajolerFinder(moduleURL) {
  return moduleURL + '.out.js';
}

var scriptModuleLoad =
    scriptModuleLoadMaker(document.location.toString(),
                          undefined, cajolerFinder);
/**
 * Load the Valija module.
 */

var valijaVow = scriptModuleLoad.async(
    '../../ant-lib/com/google/caja/plugin/valija');

/**
 * Load the 'libSquare' module, which our gadgets expect to have access
 * to without any special effort.
 */

var libSquareVow = scriptModuleLoad.async('libSquare');

/**
 * ID counter for gadget <DIV> IDs.
 */
var gadgetIdCounter = 0;

/**
 * Creates a <DIV> for a new gadget.
 *
 * @return the ID of the newly created <DIV>.
 */
function createGadgetContainer() {
  var id = 'gadget' + gadgetIdCounter++;
  var div = document.createElement('DIV');
  div.setAttribute('class', 'gadgetContainer');
  div.setAttribute('id', id);
  div.innerHTML = 'This is some content';
  document.getElementById('gadgets').appendChild(div);
  return id;
}

/**
 * Loads an arbitrary gadget with the proper Valija context.
 *
 * @param src a URL to the source of the gadget.
 */
function loadGadget(src) {
  Q.when(valijaVow, function(valija) {
    Q.when(libSquareVow, function(libSquare) {
      Q.when(scriptModuleLoad.async(src), function(gadgetModule) {

        // Create a container for our gadget to live in
        var gadgetContainerId = createGadgetContainer();

        // Create a private imports object for the gadget
        var imports = ___.copy(___.sharedImports);

        // Establish Valija "outers"
        imports.outers = imports;

        // Provide the promise API to the gadget
        imports.Q = Q;

        // Add an 'includeScript' allowing dynamic script inclusion
        imports.includeScript = ___.primFreeze({ 
            async: ___.markFuncFreeze(function(src) {
              return Q.when(
                scriptModuleLoad.async(String(src)),
                function(module) {
                  module(imports);
                  return cajita.USELESS;
                });
            })
        });

        // This is where we attach a Domita instance to the DIV
        // in which the untrusted code is sandboxed
        attachDocumentStub(
            '-' + gadgetContainerId,
            { rewrite: function () { return null; } },
            imports,
            document.getElementById(gadgetContainerId));

        // We attach an HTMLEmitter so that HTML embedded in the
        // untrusted code is also emitted into the sandbox DIV
        imports.htmlEmitter___ = new HtmlEmitter(
            document.getElementById(gadgetContainerId));

        // This must be the last step: create a Valija context within
        // the imports
        imports.$v = valija.CALL___({ outers: imports.outers });

        // Invoke the actual libSquare and gadget modules with the imports
        libSquare(imports);
        gadgetModule(imports);
      });
    });
  });
}

/**
 *  Load some gadgets.
 */
loadGadget('widget');
loadGadget('widget');
