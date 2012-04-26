/**
 * @fileoverview
 * Given that the next script executed is the cajoled Domado module, make
 * the Domado entry points available in the global scope.
 *
 * @author ihab.awad@gmail.com
 * @requires CaptureCajoledModule
 * @overrides window
 * @provides Domado, HtmlEmitter
 */

var Domado = undefined;
var HtmlEmitter = undefined;

CaptureCajoledModule(function(imports) {
  Domado = imports.Domado;
  HtmlEmitter = imports.HtmlEmitter;
});

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['Domado'] = Domado;
  window['HtmlEmitter'] = HtmlEmitter;
}
