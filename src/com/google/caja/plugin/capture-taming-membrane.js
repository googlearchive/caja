/**
 * @fileoverview
 * Given that the next script executed is the cajoled TamingMembrane module,
 * make the TamingMembrane entry points available in the global scope.
 *
 * @author ihab.awad@gmail.com
 * @requires CaptureCajoledModule
 * @provides TamingMembrane
 */

var TamingMembrane = undefined;

CaptureCajoledModule(function(imports) {
  TamingMembrane = imports.TamingMembrane;
});
