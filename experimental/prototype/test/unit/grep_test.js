new Test.Unit.Runner({
  testGrep : function() {
    this.assertEnumEqual(['foo'], ['foo', 'bar'].grep(/^f/));
  
  }});

