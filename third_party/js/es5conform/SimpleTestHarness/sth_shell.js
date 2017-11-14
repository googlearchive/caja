sth.prototype.prepareToTest = function (filter) {
  requestedTests;
  for (var i = 0; i < aryTestCasePaths.length; i++) {
    var path = aryTestCasePaths[i];
    try {
      load(path)
      requestedTests++
    } catch (e) {
      print("ERROR! Could not load "+path+" because of "+e)
    }
  }
}

sth.prototype.println = print
sth.prototype.flush = function() {}

sth.prototype.testline = function(ut) {
    ut.printed = true;
    if (ut.testObj.id) utId=ut.testObj.id;
    else utId=ut.testObj.path.slice(ut.testObj.path.lastIndexOf('/')+1,-3);

    this.println(ut.res.toUpperCase()+': '+ut.testObj.id+' '+(ut.description));
    }

sth.prototype.startingTest = function(test) {}
