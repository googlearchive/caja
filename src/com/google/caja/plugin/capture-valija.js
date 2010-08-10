/**
 * @fileoverview
 * Given that the next script executed is the cajoled Valija module, make
 * valijaMaker available in the global scope.
 *
 * @author jasvir@gmail.com
 * @author kpreid@switchb.org
 * @requires ___
 * @provides valijaMaker
 */

var valijaMaker = undefined;
(function(){
  // Save and restore
  var originalNewModuleHandler = ___.getNewModuleHandler();
  
  // Set up a fresh handler
  var ourHandler = ___.makeNormalNewModuleHandler();

  // ... which captures Valija
  var imports = ourHandler.getImports();
  imports.loader = {
    provide: ___.markFuncFreeze(function (v) {
      valijaMaker = v;
    })
  };
  imports.outers = imports;
  
  // ... and removes itself.
  var normalHandle = ourHandler.handle;
  ourHandler.handle = ___.markFuncFreeze(function (module) {
    ___.setNewModuleHandler(originalNewModuleHandler);
    normalHandle.call(ourHandler, module);
  });
  
  ___.setNewModuleHandler(ourHandler);
})();
