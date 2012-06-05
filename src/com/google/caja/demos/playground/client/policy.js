// Playground policy
(function() {
  var tamings___ = [];
  window.___ = {};

  /**
   * Expose a "recordResult" function expected by sunspider benchmarks
   */
  tamings___.push(function tameRecordResult(frameGroup, imports) {
    function recordResult(number) {
      if (!!console && !!console.log) {
        console.log("Time taken: " + number);
      }
    };
    frameGroup.markFunction(recordResult);
    imports.parent = frameGroup.tame({ recordResult: recordResult });
  });

  return tamings___;
})();
