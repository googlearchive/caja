var m = load.async('../c.co');
var f1 = function(module) {
  var r1 = module({x: x});
  var r2 = module({x: y});
  return r1 + r2;
};
var f2 = function(reason) {
  fail('Loading module C failed, ' + reason);
};
Q.when(m, f1, f2);
