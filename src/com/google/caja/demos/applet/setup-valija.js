(function(){
  var imports = ___.getNewModuleHandler().getImports();
  imports.loader = {provide:___.markFuncFreeze(function(v){valijaMaker = v;})};
  imports.outers = imports;
})();
