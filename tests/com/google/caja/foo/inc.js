env.assertEquals(env.x, 6);
exports.inc = function(x) {
  var r = env.Q.defer();
  var m = require.async('../add.vo');
  env.Q.when(m, function(module) {
                  r.resolve(module.add(x, 1));
                },
                function(reason) { 
                  r.resolve(
                      env.Q.reject("Loading module Add failed, " + reason)); 
                });
  return r.promise;
};
