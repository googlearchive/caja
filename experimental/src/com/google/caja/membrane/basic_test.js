/*
 *  The tests in this file check basic getting and setting of wrappers.
 *  This JavaScript is a mix of cajoled code (to show its behavior)
 *  and uncajoled code (to print out and manipulate the cajoled code).
 *  
 *  @author: Adrienne Felt (adriennefelt@gmail.com)
 */

{
___.loadModule(function (___, IMPORTS___) {
{

	/* Recursively prints out the values in a wrapped object.
   * Notably, it fakes how the keys for the for-in enumerating loop are
   * obtained.  Since a shadow object doesn't actually have any properties,
	 * obj.getKeys___() needs to be called to obtain a list of keys to loop 
   * over.  This is not actually in the current rewriting rules. !!!
   */
	var printPropsGetter;
  printPropsGetter = ___.simpleFunc(function (obj, ind) {
    var a, x0___, x1___;
		var indent = "";
		for (var i = 0; i < ind; i++) {
			indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;";
		}
		
		/* FAKE CAJOLING of the following code:
		 * var keys = obj.getKeys___(); 
		 * for (var i = 0; i < keys.length; i++) {
		 *  var a = keys[i]; ...} */
		x0___ = ___.callPub(obj, 'getKeys___', []);
		var len = ___.readPub(x0___, 'length');
		
		for (x1___ = 0; x1___ < len; x1___++) {
			a = ___.readPub(x0___, x1___);
			if (typeof ___.readPub(obj, a) != "object") {
				document.write(indent + "<b>" + a + ":</b> " + ___.readPub(obj, a) + 
						"<br />");
				} else {
					document.write(indent + "<b><i>" + a + "</i></b>, object<br />");
					___.asSimpleFunc(___.primFreeze(printPropsGetter))(___.readPub(obj, a), 
							ind + 1);
				}
		}
  }, 'printPropsGetter');
  
  // Note that the ALICE referenced in this code is the imported
  // shadow version of the real alice
  var Alice = ___.readImport(IMPORTS___, 'Alice');
  var x0___, x1___, x2___, x3___, x4___, x5___, x6___, x7___;
  var x8___, x9___, x10___, x11___, x12___, x13___, x14___, x15___, x16___; 
	var x17___;

  // Shows what's inside Alice using printPropsGetter
  document.write("<span class=\"comment\">Recursively prints out the values in"+
			" a wrapped object (as obj[prop]).<br />  If gate is disabled, nothing"+
			" will appear since the prop list is hidden.</span><br />");
  printPropsGetter(Alice,0);

	// Checks reading chained references (e.g., Alice.pet.petpet.species).
	document.write("<br /><span class=\"comment\">Checks reading chained"+
			" references (e.g. Alice.pet.petpet.species).<br />" +
			"Enabled: \"Kitten\".  Disabled: \"fail\".<br />");
	var chain = "fail";
	if (___.readPub(Alice, 'pet')) {
		chain = (x0___ = (x1___ = (x2___ = Alice, 
				x2___.pet_canRead___? x2___.pet: ___.readPub(x2___, 'pet')), 
				x1___.petpet_canRead___? x1___.petpet: ___.readPub(x1___, 'petpet')), 
				x0___.species_canRead___? x0___.species: ___.readPub(x0___, 'species'));
	}
	document.write("<b>alice.pet.petpet.species:</b></span> " + chain + 
			"<br /><br />");

	/* Calls Alice.dance() through a wrapper.  
	 * Alice.dance() should print "Alice dances"; it checks that "this" is binding
	 * properly.  If only the "Alice" is missing, something is wrong with how 
	 * "this" is being passed around.  */
	document.write("<span class=\"comment\">Alice.dance() checks the binding of"+
			" \"this\".<br />" + 
			"Enabled: \"Alice dances\".  Disabled: blank.</span><br />");
	x3___ = Alice, undefined, 
			x3___.dance_canCall___? x3___.dance(): ___.callPub(x3___, 'dance', [ ]);
	
	/* Calls Alice.giveNumber(1,2,3) through a wrapper.
	 * It should return 17.  It checks that arguments are being passed through 
	 * wrapper code adequately.  */
	document.write("<br /><br /><span class=\"comment\">Checks that arguments"+
			" are being passed through wrapper code.<br />" +
			"Enabled: \"17\".  Disabled: false.</span>");
	var givenum = (x7___ = Alice, (x4___ = 1, x5___ = 2, x6___ = 3), 
			x7___.giveNumber_canCall___? x7___.giveNumber(x4___, x5___, x6___): 
			___.callPub(x7___, 'giveNumber', [ x4___, x5___, x6___ ]));
	document.write("<br /><span class=\"comment\">give num: </span>" + givenum 
			+ "<br /><br />");

	// Checks setting of a property with a simple RHS.
	document.write("<span class=\"comment\">Checks setting of a property"+
			" with a simple RHS.<br />Enabled: \"Wonderland\"," +
			" \"Sunnyvale\".  Disabled: false, false.</span><br />");
	document.write("<span class=\"comment\"><b>old loc:</b> </span>" +
			___.readPub(Alice, "location") + "<br />");
	x8___ = Alice, x9___ = "Sunnyvale", 
			x8___.location_canSet___? (x8___.location = x9___):
			___.setPub(x8___, 'location', x9___);
	document.write("<span class=\"comment\"><b>new loc:</b> </span>" +
			___.readPub(Alice, "location") + "<br /><br />");
	
	// Checks setting of a property with a function as the RHS.
	document.write("<span class=\"comment\">Checks setting of a property with a"+
			" function as the RHS.<br />" +
			"Enabled: \"16\", \"17\".  Disabled: false, false.</span><br />");
	document.write("<span class=\"comment\"><b>old age:</b> </span>" + 
			___.readPub(Alice, "age") + "<br />");
	x17___ = IMPORTS___, x10___ = (x11___ = Alice, x12___ = (x16___ = Alice, 
			(x13___ = 1, x14___ = 2, x15___ = 3), x16___.giveNumber_canCall___? 
			x16___.giveNumber(x13___, x14___, x15___): ___.callPub(x16___, 'giveNumber', 
			[ x13___, x14___, x15___ ])), x11___.age_canSet___? (x11___.age = x12___): 
			___.setPub(x11___, 'age', x12___));
	document.write("<span class=\"comment\"><b>new age:</b> </span>" + 
			___.readPub(Alice, "age"));

}
});
}