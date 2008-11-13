new Test.Unit.Runner({
  testDOMFlags: function() {
    this.info('supports XPath: ' + Prototype.BrowserFeatures.XPath);
    this.info('supports SelectorsAPI: ' + Prototype.BrowserFeatures.SelectorsAPI);
    this.info('supports ElementExtensions: ' + Prototype.BrowserFeatures.ElementExtensions);
    this.info('supports SpecificElementExtensions: ' + Prototype.BrowserFeatures.SpecificElementExtensions);
  },
  
  test$: function() {
    this.assertNotNull(document.getElementById('dollar_div'));
    this.assertNotNull($('dollar_div'));
    this.assertEqual(document.getElementById('dollar_div'), $('dollar_div'));
    this.assertEqual(document.getElementById('dollar_div'), document.getElementById('dollar_div'));
  }
});