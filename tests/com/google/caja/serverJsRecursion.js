exports.isNegative = function(a) {
  if (a < 0) { return true; } else { return false; }
};
exports.isNonNegative = function (a) {
  var m = require.async('./serverJsRecursion.vo');
  var r = env.Q.defer();
  env.Q.when(m, function(module) { r.resolve(!module.isNegative(a)); },
                function(reason) { r.resolve(env.Q.reject(reason)); });
  return r.promise;
};
