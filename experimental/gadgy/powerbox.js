/**
 * Gadgy powerbox.
 *
 * @param valija the Valija module.
 * @param Q the promise API.
 * @param load a Cajita loader.
 * @param attachWindow privileged function that makes a new UI "window", builds
 *     a new Domita context and associates it with the supplied imports.
 * @param setTimeout privileged function with the same API as the HTML DOM
 *     setTimeout function.
 *
 * @return nothing.
 */
'use strict';
'use cajita';

var eventQueue;

var loadView = function(title, viewModule, importedVars) {
  var imports = {};
  cajita.forAllKeys(importedVars, function(k, v) { imports[k] = v; });
  imports.outers = imports;
  
  // Add an 'includeScript' allowing dynamic script inclusion
  imports.includeScript = { 
    async: function(src) {
      return Q.when(load.async(String(src)), function(module) {
        module(imports);
        // Valija modules are loaded for their side effects
        // TODO(ihab.awad): BUG Valija should explicitly return USELESS
        return cajita.USELESS;
      });
    }
  };
  
  var windowHandle = attachWindow(title, imports);
  
  // This must be the last step: create a Valija context within
  // the imports
  imports.$v = valija({ outers: imports.outers });
  viewModule(imports);
}

var loadObject = function(title, modelModuleId, viewModuleId) {

  var modelGuardianVow = load('modelGuardian')({
    Q: Q,
    moduleId: modelModuleId,
    instanceName: '<' + title + '>',
    queue: eventQueue
  });

  Q.when(modelGuardianVow, function(modelGuardian) {

    Q.when(load.async(viewModuleId), function(viewModule) {
      loadView(title, viewModule, {
        Q: Q,
        modelGuardian: modelGuardian.representativeFacet
      });
    });

    /*
    loadView(title + ' - guardian', load('modelGuardianView.html'), {
      Q: Q,
      modelGuardian: modelGuardian.diagnosticFacet
    });
    */
  });
}

Q.when(load.async('testModel'), function(modelModule) {

  eventQueue = load('eventQueue')({
    Q: Q,
    setTimeout: setTimeout,
    modelModule: modelModule
  });

  loadObject('Gadget one', 'testModel', 'testUi.html');
  loadObject('Gadget two', 'testModel', 'testUi.html');
});

