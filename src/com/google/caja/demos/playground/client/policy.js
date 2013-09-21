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

    // TODO(ihab.awad): Consolidate code that tames common browser utilities
    // such as this one.
    imports.console = frameGroup.tame(frameGroup.markReadOnlyRecord({
      log: frameGroup.markFunction(function () {
        console.log.apply(console, arguments);
      }),
      warn: frameGroup.markFunction(function () {
        console.warn.apply(console, arguments);
      }),
      error: frameGroup.markFunction(function () {
        console.error.apply(console, arguments);
      }),
      info: frameGroup.markFunction(function () {
        console.info.apply(console, arguments);
      }),
      profile: frameGroup.markFunction(function () {
        console.profile.apply(console, arguments);
      }),
      profileEnd: frameGroup.markFunction(function () {
        console.profileEnd.apply(console, arguments);
      })
    }));
  });

  return tamings___;
})();
