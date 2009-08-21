var result = Q.defer();
if (x <= 0) {
  result.resolve(-1);
}
else if (x == 1) {
  result.resolve(1);
}
else {
  var m = load.async('recursion.co');
  Q.when(m, function(module) {
	var r = module({x: x - 1, load: load, Q: Q});
	Q.when(r, function(r) {result.resolve(x * r); });
  });
}
result.promise;
