/*
 * This tests the enforcement of the mock API policy specified in
 * taming/MyAPI.js.  Like the basic_test.js, it's a mix of cajoled
 * and uncajoled code.  Because caja.fail throws an exception that
 * stops the execution of the page, policy violations have been
 * commented out.  Uncomment the lines to see them cause failures.
 *
 * @author: Adrienne Felt (adriennefelt@gmail.com)
 */

var mainTest = new Test(21);
var argTest = new Test(24);

var tester_control = makeMembrane(mainTest);
var tester_control2 = makeMembrane(argTest);
wrapmap.mainTest = tester_control.shadow;
wrapmap.argTest = tester_control2.shadow;

{
___.loadModule(function (___, IMPORTS___) {
{

	var tester = ___.readImport(IMPORTS___, 'mainTest');
	var argtester = ___.readImport(IMPORTS___, 'argTest');
	
	// READING properties & functions
	document.write("<span class='comment'><b>Reading properties &amp;"+
			" functions</b>");

	document.write("<br /><br />When uncommented, the" +
			" following two will throw a caja.fail exception.<br/>");
	document.write("tester.name:</span> ");
	// document.write(___.readPub(tester,"name"));
	document.write("<br /><span class='comment'>tester.wave:</span> ");
	// document.write(___.readPub(tester,"wave"));
	document.write("<br /><br />");
	
	document.write("<span class='comment'>The following three" +
			" reads will succeed.<br />");
	document.write("tester.age:</span> "+ ___.readPub(tester,"age"));
	document.write("<br /><span class='comment'>tester.height:</span> "
			+___.readPub(tester,"height"));
	document.write("<br /><span class='comment'>tester.highFive:</span> "
			+___.readPub(tester,"highFive"));

	// SETTING properies & functions
	document.write("<span class='comment'><b><br /><br />Setting properties"+
			" &amp; functions</b>");
	document.write("<br /><br />When uncommented, the" +
		" following five will throw a caja.fail exception.<br/></span>");
		
	// All access to "name" is denied
	document.write("<span class='comment'>tester.name (perm:deny):<br /></span> ");
	var x0___, x1___, x2___, x3___;
	/* x3___ = IMPORTS___, 
		x0___ = (x1___ = tester, 
		x2___ = "NAME", 
		x1___.name_canSet___? (x1___.name = x2___): 
				___.setPub(x1___, 'name', x2___)); */
	
	// This property is read-only so can't be set
	document.write("<span class='comment'>tester.age (rw:read-only):<br /></span> ");
	/*x3___ = IMPORTS___, 
		x0___ = (x1___ = tester, 
		x2___ = 16, 
		x1___.age_canSet___? (x1___.age = x2___): ___.setPub(x1___, 'age', x2___)); */
	
	// Here I try to set with a value not in the array
	document.write("<span class='comment'>tester.height (RHS not in array):"+
			"<br /></span> ");
	/*x3___ = IMPORTS___, 
		x0___ = (x1___ = tester, 
		x2___ = "5'4\"", 
		x1___.height_canSet___? (x1___.height = x2___): ___.setPub(x1___,
				'height', x2___));*/

	// Setting a function is not allowed
	document.write("<span class='comment'>tester.wave() (can't set funcs):"+
			"<br /></span> ");
	/*x3___ = IMPORTS___, 
		x0___ = (x1___ = tester, 
		x2___ = "function () { }", 
		x1___.wave_canSet___? (x1___.wave = x2___): ___.setPub(x1___, 'wave', x2___));*/
	
	// Setting a function is not allowed even when "perm": "allow"
	document.write("<span class='comment'>tester.wave() (can't set funcs):"+
			"<br /></span> ");
	/*x3___ = IMPORTS___, 
		x0___ = (x1___ = tester, 
		x2___ = "function () { }", 
		x1___.highFive_canSet___? (x1___.highFive = x2___): ___.setPub(x1___, 
				'highFive', x2___));*/
	
	document.write("<br /><br /><span class='comment'>The following two"+
			" should succeed.<br/>");

	document.write("tester.height (RHS in array):<br /></span> ");
	x3___ = IMPORTS___, 
		x0___ = (x1___ = tester, 
		x2___ = "6'1\"", 
		x1___.height_canSet___? (x1___.height = x2___): 
				___.setPub(x1___, 'height', x2___));
	document.write(mainTest.height+"<br />");
	
	// CALLING properies & functions
	document.write("<span class='comment'><b><br /><br />"+
			"Calling properties &amp; functions</b>");
	
	document.write("<br /><br />When uncommented, the" +
		" following three will throw a caja.fail exception.<br/></span>");
	
	document.write("<span class='comment'>Call to tester.name (fails cause "+
			"its a prop): </span>");	
	//___.callPub(tester,"name", [ ]);
	
	document.write("<span class='comment'><br />Call to tester.wave()"+
			" (perm:deny): </span>");
	//___.callPub(tester,"wave", [ ]);

	document.write("<span class='comment'><br />Call to tester.highFive()"+
			" (wrong arg values): </span>");
	// Tests the array filtering
	//___.callPub(tester,"highFive",[2,5]);	
	// Tests the function filtering
	//___.callPub(tester,"highFive",[5,2]);
	
	document.write("<br /><br /><span class='comment'>The following should"+
			" succeed.<br />");
	document.write("Call to tester.highFive(5,5): </span>");
	document.write(___.callPub(tester,"highFive",[5,5]));
	
	// BOUNDARIES
	document.write("<br /><br /><span class='comment'>Demonstrates that "+
			"Test.privateprop is unavailable when normally accessed; if the next"+
			" line is uncommented, it will fail.");
	//___.callPub(tester,"privateprop",[]);
	document.write("<br /><br />The following will only return true if argtester."+
			"useArgs(tester) receives an unwrapped version of tester.</span><br />");
	document.write(___.callPub(argtester,"useArgs",[tester]));
	
}
});
}