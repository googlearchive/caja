(function(){
  var imports = ___.getNewModuleHandler().getImports();
  imports.loader = {provide:___.simpleFunc(function(v){valijaMaker = v;})};
  imports.outers = imports;
})();
