
// *** unit/ajax.js ***
//   827: 
//   828: //}


// *** index2.html ***
//     1: <html>
//     2: <head>
//     3: 	<title>jQuery Test Suite<�title>
//     4: 	<link rel="Stylesheet" media="screen" href="data/testsuite.css" />
//     5: 	<script>
//    26:     <�script>
//    27: 	<h1 id="header">jQuery Test Suite<�h1>
//    28: 	<h2 id="banner"><�h2>
//    29: 	<h2 id="userAgent"><�h2>
//    30: 	
//    31: 	<�-- Test HTML -�>
//    32: 	<div id="nothiddendiv" style="height:1px;background:white;">
//    33: 		<div id="nothiddendivchild"><�div>
//    34: 	<�div>
//    35: 	<�-- this iframe is outside the #main so it won't reload constantly wasting time, but it means the tests must be "safe" and clean up after themselves -�>
//    36: 	<�-- iframe id="loadediframe" name="loadediframe" style="display:none;" src="data/iframe.html"><�iframe -�>
//    37: 	<dl id="dl" style="display:none;">
//    38: 	<div id="main" style="display: none;">
//    39: 		<p id="firstp">See <a id="simon1" href="http://simon.incutio.com/archive/2003/03/25/#getElementsBySelector" rel="bookmark">this 
//    40: 		  blog entry<�a> for more information.<�p>
//    41: 		<p id="ap">
//    42: 			Here are some links in a normal paragraph: <a id="google" href="http://www.google.com/" title="Google!">Google<�a>, 
//    43: 			<a id="groups" href="http://groups.google.com/">Google Groups<�a>. 
//    44: 			This link has <code><a href="http://smin" id="anchor1">class="blog"<�a><�code>: 
//    45: 			<a href="http://diveintomark.org/" class="blog" hreflang="en" id="mark">diveintomark<�a>
//    46: 
//    47: 		<�p>
//    48: 		<div id="foo">
//    49: 			<p id="sndp">Everything inside the red border is inside a div with <code>id="foo"<�code>.<�p>
//    50: 			<p lang="en" id="en">This is a normal link: <a id="yahoo" href="http://www.yahoo.com/" class="blogTest">Yahoo<�a><�p>
//    51: 			<p id="sap">This link has <code><a href="#2" id="anchor2">class="blog"<�a><�code>: <a href="http://simon.incutio.com/" class="blog link" id="simon">Simon Willison's Weblog<�a><�p>
//    52: 
//    53: 		<�div>
//    54: 		<p id="first">Try them out:<�p>
//    55: 		<ul id="firstUL"><�ul>
//    56: 		<ol id="empty"><�ol>
//    57: 		<form id="form" action="formaction">
//    58: 			<input type="text" name="action" value="Test" id="text1" maxlength="30"/>
//    59: 			<input type="text" name="text2" value="Test" id="text2" disabled="disabled"/>
//    60: 			<input type="radio" name="radio1" id="radio1" value="on"/>
//    61: 
//    62: 			<input type="radio" name="radio2" id="radio2" checked="checked"/>
//    63: 			<input type="checkbox" name="check" id="check1" checked="checked"/>
//    64: 			<input type="checkbox" id="check2" value="on"/>
//    65: 
//    66: 			<input type="hidden" name="hidden" id="hidden1"/>
//    67: 			<input type="text" style="display:none;" name="foo[bar]" id="hidden2"/>
//    68: 			
//    69: 			<input type="text" id="name" name="name" value="name" />
//    70: 			
//    71: 			<button id="button" name="button">Button<�button>
//    72: 			
//    73: 			<textarea id="area1" maxlength="30">foobar<�textarea>
//    74: 			
//    75: 			<select name="select1" id="select1">
//    76: 				<option id="option1a" class="emptyopt" value="">Nothing<�option>
//    77: 				<option id="option1b" value="1">1<�option>
//    78: 				<option id="option1c" value="2">2<�option>
//    79: 				<option id="option1d" value="3">3<�option>
//    80: 			<�select>
//    81: 			<select name="select2" id="select2">
//    82: 				<option id="option2a" class="emptyopt" value="">Nothing<�option>
//    83: 				<option id="option2b" value="1">1<�option>
//    84: 				<option id="option2c" value="2">2<�option>
//    85: 				<option id="option2d" selected="selected" value="3">3<�option>
//    86: 			<�select>
//    87: 			<select name="select3" id="select3" multiple="multiple">
//    88: 				<option id="option3a" class="emptyopt" value="">Nothing<�option>
//    89: 				<option id="option3b" selected="selected" value="1">1<�option>
//    90: 				<option id="option3c" selected="selected" value="2">2<�option>
//    91: 				<option id="option3d" value="3">3<�option>
//    92: 			<�select>
//    93: 			
//    94: 			<�-- object id="object1" codebase="stupid">
//    95: 				<param name="p1" value="x1" />
//    96: 				<param name="p2" value="x2" />
//    97: 			<�object -�>
//    98: 			
//    99: 			<span id="台北Táiběi"><�span>
//   100: 			<span id="台北" lang="中文"><�span>
//   101: 			<span id="utf8class1" class="台北Táiběi 台北"><�span>
//   102: 			<span id="utf8class2" class="台北"><�span>
//   103: 			<span id="foo:bar" class="foo:bar"><�span>
//   104: 			<span id="test.foo[5]bar" class="test.foo[5]bar"><�span>
//   105: 			
//   106: 			<�-- foo_bar id="foobar">test element<�foo_bar -�>
//   107: 		<�form>
//   108: 		<b id="floatTest">Float test.<�b>
//   109: 		<�-- iframe id="iframe" name="iframe"><�iframe -�>
//   110: 		<form id="lengthtest">
//   111: 			<input type="text" id="length" name="test"/>
//   112: 			<input type="text" id="idTest" name="id"/>
//   113: 		<�form>
//   114: 		<table id="table"><�table>
//   115: 		
//   116: 		<div id="fx-queue">
//   117: 			<div id="fadein" class='chain test'>fadeIn<div>fadeIn<�div><�div>
//   118: 			<div id="fadeout" class='chain test out'>fadeOut<div>fadeOut<�div><�div>
//   119: 			
//   120: 			<div id="show" class='chain test'>show<div>show<�div><�div>
//   121: 			<div id="hide" class='chain test out'>hide<div>hide<�div><�div>
//   122: 			
//   123: 			<div id="togglein" class='chain test'>togglein<div>togglein<�div><�div>
//   124: 			<div id="toggleout" class='chain test out'>toggleout<div>toggleout<�div><�div>
//   125: 		
//   126: 			
//   127: 			<div id="slideup" class='chain test'>slideUp<div>slideUp<�div><�div>
//   128: 			<div id="slidedown" class='chain test out'>slideDown<div>slideDown<�div><�div>
//   129: 			
//   130: 			<div id="slidetogglein" class='chain test'>slideToggleIn<div>slideToggleIn<�div><�div>
//   131: 			<div id="slidetoggleout" class='chain test out'>slideToggleOut<div>slideToggleOut<�div><�div>
//   132: 		<�div>
//   133: 		
//   134: 		<div id="fx-tests"><�div>
//   135: 
//   136: 		<form id="testForm" method="get">
//   137: 			<textarea name="T3" rows="2" cols="15">?
//   138: Z<�textarea>
//   139: 			<input type="hidden" name="H1" value="x" />
//   140: 			<input type="hidden" name="H2" />
//   141: 			<input name="PWD" value="" />
//   142: 			<input name="T1" type="text" />
//   143: 			<input name="T2" type="text" value="YES" readonly="readonly" />
//   144: 			<input type="checkbox" name="C1" value="1" />
//   145: 			<input type="checkbox" name="C2" />
//   146: 			<input type="radio" name="R1" value="1" />
//   147: 			<input type="radio" name="R1" value="2" />
//   148: 			<input type="text" name="My Name" value="me" />
//   149: 			<input type="reset" name="reset" value="NO" />
//   150: 			<select name="S1">
//   151: 				<option value="abc">ABC<�option>
//   152: 				<option value="abc">ABC<�option>
//   153: 				<option value="abc">ABC<�option>
//   154: 			<�select>
//   155: 			<select name="S2" multiple="multiple" size="3">
//   156: 				<option value="abc">ABC<�option>
//   157: 				<option value="abc">ABC<�option>
//   158: 				<option value="abc">ABC<�option>
//   159: 			<�select>
//   160: 			<select name="S3">
//   161: 				<option selected="selected">YES<�option>
//   162: 			<�select>
//   163: 			<select name="S4">
//   164: 				<option value="" selected="selected">NO<�option>
//   165: 			<�select>
//   166: 			<input type="submit" name="sub1" value="NO" />
//   167: 			<input type="submit" name="sub2" value="NO" />
//   168: 			<input type="image" name="sub3" value="NO" />
//   169: 			<button name="sub4" type="submit" value="NO">NO<�button>
//   170: 			<input name="D1" type="text" value="NO" disabled="disabled" />
//   171: 			<input type="checkbox" checked="checked" disabled="disabled" name="D2" value="NO" />
//   172: 			<input type="radio" name="D3" value="NO" checked="checked" disabled="disabled" />
//   173: 			<select name="D4" disabled="disabled">
//   174: 				<option selected="selected" value="NO">NO<�option>
//   175: 			<�select>
//   176: 		<�form>
//   177: 		<div id="moretests">
//   178: 			<form>
//   179: 				<div id="checkedtest" style="display:none;">
//   180: 					<input type="radio" name="checkedtestradios" checked="checked"/>
//   181: 					<input type="radio" name="checkedtestradios" value="on"/>
//   182: 					<input type="checkbox" name="checkedtestcheckboxes" checked="checked"/>
//   183: 					<input type="checkbox" name="checkedtestcheckboxes" />
//   184: 				<�div>
//   185: 			<�form>
//   186: 			<div id="nonnodes"><span>hi<�span> there <�-- mon ami -�><�div>
//   187: 			<div id="t2037">
//   188: 				<div><div class="hidden">hidden<�div><�div>
//   189: 			<�div>
//   190: 		<�div>
//   191: 	<�div>
//   192: 	<�dl>
//   193: 	
//   194: 	<ol id="tests"><�ol>
//   195: <�body>
//   196: <�html>

{

// *** index2.html ***
//     8: 	<�script>
//     9: 	<script type="text/javascript" src="jquery.js"><�script>
//    10: 	<script type="text/javascript" src="data/testrunner.js"><�script>
//    11: 	<script type="text/javascript" src="unit/core.js"><�script>
//    12: 	<script type="text/javascript" src="unit/dimensions.js"><�script>
//    13: 	<script type="text/javascript" src="unit/selector.js"><�script>
//    14: 	<script type="text/javascript" src="unit/event.js"><�script>
//    15: 	<script type="text/javascript" src="unit/ajax.js"><�script>
//    16: 	<script type="text/javascript" src="unit/fx.js"><�script>
//    17: <�head>
//    18: 
//    19: <body onload="go()">
//    20:     <script>
//    25:     }

___.loadModule(function moduleFunc___(___, IMPORTS___) {
var moduleResult___ = ___.NO_RESULT;
var Array = ___.readImport(IMPORTS___, 'Array');
var $v = ___.readImport(IMPORTS___, '$v',{
'ros': {
'()': {
}
},                                             'ro': {
'()': {
}
},                                             'remove': {
'()': {
}
},                                             'so': {
'()': {
}
},                                             'keys': {
'()': {
}
},                                             'typeOf': {
'()': {
}
},                                             's': {
'()': {
}
},                                             'cm': {
'()': {
}
},                                             'r': {
'()': {
}
},                                             'dis': {
'()': {
}
},                                             'initOuter': {
'()': {
}
},                                             'construct': {
'()': {
}
},                                             'getOuters': {
'()': {
}
},                                             'cf': {
'()': {
}
},                                             'canReadRev': {
'()': {
}
}
});
var RegExp = ___.readImport(IMPORTS___, 'RegExp',{
});
var $dis = $v.getOuters();
$v.initOuter('onerror');

// *** data/testsuite.css ***
//     1: body, div, h1 { font-family: 'trebuchet ms', verdana, arial; margin: 0; padding: 0 }
//     2: body {font-size: 10pt; }
//     3: h1 { padding: 15px; font-size: large; background-color: #06b; color: white; }
//     4: h2 { padding: 10px; background-color: #eee; color: black; margin: 0; font-size: small; font-weight: normal }
//     5: 
//     6: .pass { color: green; } 
//     7: .fail { color: red; } 
//     8: p.result { margin-left: 1em; }
//     9: 
//    10: #banner { height: 2em; border-bottom: 1px solid white; }
//    11: h2.pass { background-color: green; }
//    12: h2.fail { background-color: red; }
//    13: 
//    14: ol#tests > li > strong { cursor:pointer; }
//    15: 
//    16: div#fx-tests h4 {
//    17: 	background: red;
//    18: }
//    19: 
//    20: div#fx-tests h4.pass {
//    21: 	background: green;
//    22: }
//    23: 
//    24: div#fx-tests div.box {
//    25: 	background: red url(data/cow.jpg) no-repeat;
//    26: 	overflow: hidden;
//    27: 	border: 2px solid #000;
//    28: }
//    29: 
//    30: div#fx-tests div.overflow {
//    31: 	overflow: visible;
//    32: }
//    33: 
//    34: div.inline {
//    35: 	display: inline;
//    36: }
//    37: 
//    38: div.autoheight {
//    39: 	height: auto;
//    40: }
//    41: 
//    42: div.autowidth {
//    43: 	width: auto;
//    44: }
//    45: 
//    46: div.largewidth {
//    47: 	width: 100px;
//    48: }
//    49: 
//    50: div.largeheight {
//    51: 	height: 100px;
//    52: }
//    53: 
//    54: div.medwidth {
//    55: 	width: 50px;
//    56: }
//    57: 
//    58: div.medheight {
//    59: 	height: 50px;
//    60: }
//    61: 
//    62: div.nowidth {
//    63: 	width: 0px;
//    64: }
//    65: 
//    66: div.noheight {
//    67: 	height: 0px;
//    68: }
//    69: 
//    70: div.hidden {
//    71: 	display: none;
//    72: }
//    73: 
//    74: div#fx-tests div.widewidth {
//    75: 	background-repeat: repeat-x;
//    76: }
//    77: 
//    78: div#fx-tests div.wideheight {
//    79: 	background-repeat: repeat-y;
//    80: }
//    81: 
//    82: div#fx-tests div.widewidth.wideheight {
//    83: 	background-repeat: repeat;
//    84: }
//    85: 
//    86: div#fx-tests div.noback {
//    87: 	background-image: none;
//    88: }
//    89: 
//    90: div.chain, div.chain div { width: 100px; height: 20px; position: relative; float: left; }
//    91: div.chain div { position: absolute; top: 0px; left: 0px; }
//    92: 
//    93: div.chain.test { background: red; }
//    94: div.chain.test div { background: green; }
//    95: 
//    96: div.chain.out { background: green; }
//    97: div.chain.out div { background: red; display: none; }
//    98: 
//    99: div#show-tests * { display: none; }

IMPORTS___.emitCss___([ '@media screen {\n  .', ' body, .', ' div, .', ' h1 {\n    font-family: \'trebuchet ms\', \'verdana\', \'arial\';\n    margin: 0;\n    padding: 0\n  }\n  .', ' body {\n    font-size: 10pt\n  }\n  .', ' h1 {\n    padding: 15px;\n    font-size: large;\n    background-color: #06b;\n    color: white\n  }\n  .', ' h2 {\n    padding: 10px;\n    background-color: #eee;\n    color: black;\n    margin: 0;\n    font-size: small;\n    font-weight: normal\n  }\n  .', ' .pass {\n    color: green\n  }\n  .', ' .fail {\n    color: red\n  }\n  .', ' p.result {\n    margin-left: 1em\n  }\n  .', ' #banner-', ' {\n    height: 2em;\n    border-bottom: 1px solid white\n  }\n  .', ' h2.pass {\n    background-color: green\n  }\n  .', ' h2.fail {\n    background-color: red\n  }\n  .', ' ol#tests-', ' \x3e li \x3e strong {\n    cursor: pointer\n  }\n  .', ' div#fx-tests-', ' h4 {\n    background: red\n  }\n  .', ' div#fx-tests-', ' h4.pass {\n    background: green\n  }\n  .', ' div#fx-tests-', ' div.box {\n    background: red url(\'data/cow.jpg\') no-repeat;\n    overflow: hidden;\n    border: 2px solid #000\n  }\n  .', ' div#fx-tests-', ' div.overflow {\n    overflow: visible\n  }\n  .', ' div.inline {\n    display: inline\n  }\n  .', ' div.autoheight {\n    height: auto\n  }\n  .', ' div.autowidth {\n    width: auto\n  }\n  .', ' div.largewidth {\n    width: 100px\n  }\n  .', ' div.largeheight {\n    height: 100px\n  }\n  .', ' div.medwidth {\n    width: 50px\n  }\n  .', ' div.medheight {\n    height: 50px\n  }\n  .', ' div.nowidth {\n    width: 0px\n  }\n  .', ' div.noheight {\n    height: 0px\n  }\n  .', ' div.hidden {\n    display: none\n  }\n  .', ' div#fx-tests-', ' div.widewidth {\n    background-repeat: repeat-x\n  }\n  .', ' div#fx-tests-', ' div.wideheight {\n    background-repeat: repeat-y\n  }\n  .', ' div#fx-tests-', ' div.widewidth.wideheight {\n    background-repeat: repeat\n  }\n  .', ' div#fx-tests-', ' div.noback {\n    background-image: none\n  }\n  .', ' div.chain, .', ' div.chain div {\n    width: 100px;\n    height: 20px;\n    position: relative;\n    float: left\n  }\n  .', ' div.chain div {\n    position: absolute;\n    top: 0px;\n    left: 0px\n  }\n  .', ' div.chain.test {\n    background: red\n  }\n  .', ' div.chain.test div {\n    background: green\n  }\n  .', ' div.chain.out {\n    background: green\n  }\n  .', ' div.chain.out div {\n    background: red;\n    display: none\n  }\n  .', ' div#show-tests-', ' * {\n    display: none\n  }\n}' ].join(IMPORTS___.getIdClass___()));
IMPORTS___.htmlEmitter___.pc('\n\n\t\n\t\n\t');
try {
{

// *** index2.html ***
//     6: 	   var jQuery = "jQuery", $ = "$"; // For testing .noConflict()

$v.so('jQuery', 'jQuery'), $v.so('$', '$');
$v.s($v.ro('window'), 'location', ___.initializeMap([ 'protocol', 'file:', 'toString', ___.frozenFunc(function () {
function toString$_lit$($dis) {

// *** index2.html ***
//     7: 	   window.location = {protocol:'file:', toString: function(){return "file:///"}};

return 'file:///';
}
___.func(toString$_lit$, 'toString$_lit$');
;
var toString$_lit = $v.dis(___.primFreeze(toString$_lit$), 'toString$_lit');
return toString$_lit;
}).CALL___() ]));
}
} catch (ex___) {
___.getNewModuleHandler().handleUncaughtException(ex___, $v.ro('onerror'), 'index2.html', '6');
}
IMPORTS___.htmlEmitter___.pc('\n\t');

// *** jquery.js ***
//     1: (function(){
//     2: /*
//     3:  * jQuery 1.2.6 - New Wave Javascript
//     4:  *
//     5:  * Copyright (c) 2008 John Resig (jquery.com)
//     6:  * Dual licensed under the MIT (MIT-LICENSE.txt)
//     7:  * and GPL (GPL-LICENSE.txt) licenses.
//     8:  *
//     9:  * $Date: 2008-05-24 14:22:17 -0400 (Sat, 24 May 2008) $
//    10:  * $Rev: 5685 $
//    11:  �/
//    12: 
//    13: // Map over jQuery in case of overwrite
//    17: 
//    22: 
//    23: // A simple way to check for HTML strings or ID strings
//    24: // (both of which we optimize for)
//    29: 
//    81:   },
//    82: 
//    83:   // The current version of jQuery being used
//    84:   jquery: "1.2.6",
//    85: 
//    86:   // The number of elements contained in the matched element set
//    87:   size: function() {
//    89:   },
//    90: 
//    91:   // The number of elements contained in the matched element set
//    92:   length: 0,
//    93: 
//    94:   // Get the Nth element in the matched element set OR
//    95:   // Get the whole matched element set as a clean array
//   104:   },
//   105: 
//   106:   // Take an array of elements and push it onto the stack
//   107:   // (returning the new matched element set)
//   117:   },
//   118: 
//   119:   // Force the current matched set of elements to become
//   120:   // the specified array of elements (destroying the stack in the process)
//   121:   // You should use pushStack() in order to do this, but maintain the stack
//   129:   },
//   130: 
//   131:   // Execute a callback for every element in the matched set.
//   132:   // (You can seed the arguments with an array of args, but this is
//   133:   // only used internally.)
//   136:   },
//   137: 
//   138:   // Determine the position of an element within
//   139:   // the matched set of elements
//   148:   },
//   149: 
//   174:   },
//   175: 
//   181:   },
//   182: 
//   199:   },
//   200: 
//   218:   },
//   219: 
//   224:   },
//   225: 
//   230:   },
//   231: 
//   232:   append: function() {
//   237:   },
//   238: 
//   239:   prepend: function() {
//   244:   },
//   245: 
//   246:   before: function() {
//   250:   },
//   251: 
//   252:   after: function() {
//   256:   },
//   257: 
//   258:   end: function() {
//   260:   },
//   261: 
//   270:   },
//   271: 
//   314:   },
//   315: 
//   324:   },
//   325: 
//   338:   },
//   339: 
//   347:   },
//   348: 
//   351:   },
//   352: 
//   355:   },
//   356: 
//   427:   },
//   428: 
//   435:   },
//   436: 
//   439:   },
//   440: 
//   443:   },
//   444: 
//   445:   slice: function() {
//   447:   },
//   448: 
//   453:   },
//   454: 
//   455:   andSelf: function() {
//   457:   },
//   458: 
//   476:   },
//   477: 
//   482:   },
//   483: 
//   522:   }
//   523: };
//   524: 
//   525: // Give the init function the jQuery prototype for later instantiation
//   527: 
//   542: 
//   546: 
//   548:   // copy reference to target object
//   595: };
//   596: 
//   602: 
//   603: jQuery.extend({
//   611:   },
//   612: 
//   613:   // See test/unit/core.js for details concerning this function.
//   617:   },
//   618: 
//   619:   // check if an element is in a (or is an) XML document
//   623:   },
//   624: 
//   625:   // Evalulates a script in a global context
//   646:   },
//   647: 
//   650:   },
//   651: 
//   652:   cache: {},
//   653: 
//   678:   },
//   679: 
//   718:   },
//   719: 
//   720:   // args is for internal usage only
//   746:   },
//   747: 
//   757:   },
//   758: 
//   759:   className: {
//   760:     // internal only, use addClass("class")
//   766:     },
//   767: 
//   768:     // internal only, use removeClass("class")
//   776:     },
//   777: 
//   778:     // internal only, use hasClass("class")
//   781:     }
//   782:   },
//   783: 
//   784:   // A method for quickly swapping in/out CSS properties to get correct calculations
//   798:   },
//   799: 
//   823:   },
//   824: 
//   934:   },
//   935: 
//  1033:   },
//  1034: 
//  1122:   },
//  1123: 
//  1126:   },
//  1127: 
//  1142:   },
//  1143: 
//  1151:   },
//  1152: 
//  1169:   },
//  1170: 
//  1190:   },
//  1191: 
//  1202:   },
//  1203: 
//  1217:   }
//  1218: });
//  1219: 
//  1221: 
//  1222: // Figure out what browser is being used
//  1223: jQuery.browser = {
//  1229: };
//  1230: 
//  1234: 
//  1235: jQuery.extend({
//  1236:   // Check to see if the W3C box model is being used
//  1238: 
//  1239:   props: {
//  1240:     "for": "htmlFor",
//  1248:   }
//  1249: });
//  1250: 
//  1251: jQuery.each({
//  1261: }, function(name, fn){
//  1264: 
//  1267: 
//  1270: });
//  1271: 
//  1272: jQuery.each({
//  1279:   jQuery.fn[ name ] = function() {
//  1281: 
//  1287: });
//  1288: 
//  1289: jQuery.each({
//  1294:   },
//  1295: 
//  1298:   },
//  1299: 
//  1302:   },
//  1303: 
//  1306:   },
//  1307: 
//  1318:   },
//  1319: 
//  1320:   empty: function() {
//  1321:     // Remove element nodes and prevent memory leaks
//  1327:   }
//  1328: }, function(name, fn){
//  1329:   jQuery.fn[ name ] = function(){
//  1332: });
//  1333: 
//  1336: 
//  1338:     // Get window width or height
//  1365: });
//  1366: 
//  1367: // Helper function used by the dimensions and offset modules
//  1376: 
//  1377: jQuery.extend({
//  1378:   expr: {
//  1381:     ":": {
//  1382:       // Position Checks
//  1391: 
//  1392:       // Child Checks
//  1396: 
//  1397:       // Parent Checks
//  1400: 
//  1401:       // Text Check
//  1403: 
//  1404:       // Visibility
//  1407: 
//  1408:       // Form attributes
//  1413: 
//  1414:       // Form elements
//  1425: 
//  1426:       // :has()
//  1428: 
//  1429:       // :header
//  1431: 
//  1432:       // :animated
//  1434:     }
//  1435:   },
//  1436: 
//  1437:   // The regular expressions that power the parsing engine
//  1438:   parse: [
//  1439:     // Match: [�value='test'], [�foo]
//  1440:     /^(\[) *�?([\w-]+) *([!*$^~=]*) *('?"?)(.*?)\4 *\]/,
//  1441: 
//  1442:     // Match: :contains('foo')
//  1443:     /^(:)([\w-]+)\("?'?(.*?(\(.*?\))?[^(]*?)"?'?\)/,
//  1444: 
//  1445:     // Match: :even, :last-child, #id, .class
//  1447:   ],
//  1448: 
//  1460:   },
//  1461: 
//  1653:   },
//  1654: 
//  1664:   },
//  1665: 
//  1779:   },
//  1780: 
//  1790:   },
//  1791: 
//  1801:   },
//  1802: 
//  1812:   }
//  1813: });
//  1814: /*
//  1815:  * A number of helper functions used for managing events.
//  1816:  * Many of the ideas behind this code orignated from
//  1817:  * Dean Edwards' addEvent library.
//  1818:  �/
//  1819: jQuery.event = {
//  1820: 
//  1821:   // Bind an event to an element
//  1822:   // Original by Dean Edwards
//  1900:   },
//  1901: 
//  1902:   guid: 1,
//  1903:   global: {},
//  1904: 
//  1905:   // Detach an event or set of events from an element
//  1969:   },
//  1970: 
//  2048:   },
//  2049: 
//  2088:   },
//  2089: 
//  2158:   },
//  2159: 
//  2165:   },
//  2166: 
//  2167:   special: {
//  2168:     ready: {
//  2169:       setup: function() {
//  2170:         // Make sure the ready event is setup
//  2173:       },
//  2174: 
//  2176:     },
//  2177: 
//  2178:     mouseenter: {
//  2179:       setup: function() {
//  2183:       },
//  2184: 
//  2185:       teardown: function() {
//  2189:       },
//  2190: 
//  2197:       }
//  2198:     },
//  2199: 
//  2200:     mouseleave: {
//  2201:       setup: function() {
//  2205:       },
//  2206: 
//  2207:       teardown: function() {
//  2211:       },
//  2212: 
//  2219:       }
//  2220:     }
//  2221:   }
//  2222: };
//  2223: 
//  2229:   },
//  2230: 
//  2239:   },
//  2240: 
//  2245:   },
//  2246: 
//  2251:   },
//  2252: 
//  2255:   },
//  2256: 
//  2275:   },
//  2276: 
//  2279:   },
//  2280: 
//  2296:   }
//  2297: });
//  2298: 
//  2299: jQuery.extend({
//  2300:   isReady: false,
//  2301:   readyList: [],
//  2302:   // Handle when the DOM is ready
//  2303:   ready: function() {
//  2304:     // Make sure that the DOM is not already loaded
//  2323:   }
//  2324: });
//  2325: 
//  2327: 
//  2387: 
//  2391: 
//  2392:   // Handle event binding
//  2396: });
//  2397: 
//  2398: // Checks if an event happened on an element within another element
//  2399: // Used in jQuery.event.special.mouseenter and mouseleave handlers
//  2408: 
//  2409: // Prevent memory leaks in IE
//  2410: // And prevent errors on refresh with events like mouseover in other browsers
//  2411: // Window isn't included so as not to unbind existing unload events
//  2412: 
//  2413: // jQuery(window).bind("unload", function() {
//  2414: //   jQuery("*").add(document).unbind();
//  2415: // });
//  2416: 
//  2418:   // Keep a copy of the old load
//  2420: 
//  2479:   },
//  2480: 
//  2481:   serialize: function() {
//  2483:   },
//  2484:   serializeArray: function() {
//  2503:   }
//  2504: });
//  2505: 
//  2506: // Attach a bunch of functions for handling common AJAX events
//  2511: });
//  2512: 
//  2514: 
//  2515: jQuery.extend({
//  2530:   },
//  2531: 
//  2534:   },
//  2535: 
//  2538:   },
//  2539: 
//  2553:   },
//  2554: 
//  2557:   },
//  2558: 
//  2559:   ajaxSettings: {
//  2561:     global: true,
//  2562:     type: "GET",
//  2563:     timeout: 0,
//  2565:     processData: true,
//  2566:     async: true,
//  2567:     data: null,
//  2568:     username: null,
//  2569:     password: null,
//  2570:     accepts: {
//  2572:       html: "text/html",
//  2576:       _default: "�/*"
//  2577:     }
//  2578:   },
//  2579: 
//  2580:   // Last-Modified header cache for next request
//  2581:   lastModified: {},
//  2582: 
//  2842:   },
//  2843: 
//  2851:   },
//  2852: 
//  2853:   // Counter for holding the number of active queries
//  2854:   active: 0,
//  2855: 
//  2856:   // Determines if an XMLHttpRequest was successful or not
//  2865:   },
//  2866: 
//  2867:   // Determines if an XMLHttpRequest returns NotModified
//  2877:   },
//  2878: 
//  2900:   },
//  2901: 
//  2902:   // Serialize an array of form elements or a set of
//  2903:   // key/values into a query string
//  2929:   }
//  2930: 
//  2931: });
//  2950:   },
//  2951: 
//  2962:   },
//  2963: 
//  2964:   // Save the old toggle function
//  2966: 
//  2977:   },
//  2978: 
//  2981:   },
//  2982: 
//  2985:   },
//  2986: 
//  2989:   },
//  2990: 
//  2993:   },
//  2994: 
//  2997:   },
//  2998: 
//  3001:   },
//  3002: 
//  3064:   },
//  3065: 
//  3085:   },
//  3086: 
//  3109:   }
//  3110: 
//  3111: });
//  3112: 
//  3126: 
//  3138: };
//  3139: 
//  3140: jQuery.extend({
//  3141: 
//  3164:   },
//  3165: 
//  3166:   easing: {
//  3169:     },
//  3172:     }
//  3173:   },
//  3174: 
//  3175:   timers: [],
//  3176:   timerId: null,
//  3177: 
//  3185:   }
//  3186: 
//  3187: });
//  3188: 
//  3190: 
//  3191:   // Simple function for setting a style value
//  3192:   update: function(){
//  3201:   },
//  3202: 
//  3203:   // Get the current size
//  3210:   },
//  3211: 
//  3212:   // Start an animation from one number to another
//  3245:   },
//  3246: 
//  3247:   // Simple 'show' function
//  3248:   show: function(){
//  3249:     // Remember where we started, so that we can go back to it later
//  3263:   },
//  3264: 
//  3265:   // Simple 'hide' function
//  3266:   hide: function(){
//  3267:     // Remember where we started, so that we can go back to it later
//  3273:   },
//  3274: 
//  3275:   // Each step of an animation
//  3330:   }
//  3331: 
//  3332: };
//  3333: 
//  3335:   speeds:{
//  3336:     slow: 600,
//  3337:      fast: 200,
//  3338:      // Default speed
//  3339:      def: 400
//  3340:   },
//  3341:   step: {
//  3344:     },
//  3345: 
//  3348:     },
//  3349: 
//  3352:     },
//  3353: 
//  3356:     }
//  3357:   }
//  3358: });
//  3359: // The Offset Method
//  3360: // Originally By Brandon Aaron, part of the Dimension Plugin
//  3361: // http://jquery.com/plugins/project/dimensions
//  3463: };
//  3464: 
//  3465: 
//  3467:   position: function() {
//  3496:   },
//  3497: 
//  3498:   offsetParent: function() {
//  3503:   }
//  3504: });
//  3505: 
//  3506: 
//  3507: // Create scrollLeft and scrollTop methods
//  3510:   
//  3513: 
//  3533: });
//  3534: // Create innerHeight, innerWidth, outerHeight and outerWidth methods
//  3536: 
//  3539: 
//  3540:   // innerHeight and innerWidth
//  3545:   };
//  3546: 
//  3547:   // outerHeight and outerWidth
//  3555: 

try {
{
$v.cf($v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//   535: 
//   536:   else
//   538: 

function evalScript$_caller($dis, i, elem) {

// *** jquery.js ***
//   529:   if ( elem.src )
//   530:     jQuery.ajax({
//   531:       url: elem.src,
//   532:       async: false,
//   533:       dataType: "script"
//   534:     });

if ($v.r(elem, 'src')) $v.cm(jQuery, 'ajax', [ ___.initializeMap([ 'url', $v.r(elem, 'src'), 'async', false, 'dataType', 'script' ]) ]);

// *** jquery.js ***
//   537:     jQuery.globalEval( elem.text || elem.textContent || elem.innerHTML || "" );

else $v.cm(jQuery, 'globalEval', [ $v.r(elem, 'text') || $v.r(elem, 'textContent') || $v.r(elem, 'innerHTML') || '' ]);

// *** jquery.js ***
//   539:   if ( elem.parentNode )
//   540:     elem.parentNode.removeChild( elem );

if ($v.r(elem, 'parentNode')) $v.cm($v.r(elem, 'parentNode'), 'removeChild', [ elem ]);
}
___.func(evalScript$_caller, 'evalScript$_caller');
function now$_caller($dis) {

// *** jquery.js ***
//   544:   return +new Date;

return +$v.construct($v.ro('Date'), [ ]);
}
___.func(now$_caller, 'now$_caller');

// *** jquery.js ***
//  1368: function num(elem, prop) {

function num$_caller($dis, elem, prop) {

// *** jquery.js ***
//  1369:   return elem[0] && parseInt( jQuery.curCSS(elem[0], prop, true), 10 ) || 0;

return $v.r(elem, 0) && $v.cf($v.ro('parseInt'), [ $v.cm(jQuery, 'curCSS', [ $v.r(elem, 0), prop, true ]), 10 ]) || 0;
}
___.func(num$_caller, 'num$_caller');

// *** jquery.js ***
//  2331: 
//  2332:   // Mozilla, Opera (see further below for it) and webkit nightlies currently support this event
//  2334:     // Use the handy event callback
//  2336: 
//  2337:   // If IE is used and is not in a frame
//  2338:   // Continually check to see if the document is ready
//  2351:   })();
//  2352: 
//  2363:     }, false);
//  2364: 
//  2367:     (function(){
//  2379:       // and execute any waiting functions
//  2381:     })();
//  2382:   }
//  2383: 
//  2384:   // A fallback to window.onload, that will always work

function bindReady$_caller($dis) {

// *** jquery.js ***
//  2329:   if ( readyBound ) return;

if (readyBound) return;

// *** jquery.js ***
//  2330:   readyBound = true;

readyBound = true;

// *** jquery.js ***
//  2333:   if ( document.addEventListener && !jQuery.browser.opera)
//  2335:     document.addEventListener( "DOMContentLoaded", jQuery.ready, false );

if ($v.r($v.ro('document'), 'addEventListener') && !$v.r($v.r(jQuery, 'browser'), 'opera')) $v.cm($v.ro('document'), 'addEventListener', [ 'DOMContentLoaded', $v.r(jQuery, 'ready'), false ]);

// *** jquery.js ***
//  2339:   if ( jQuery.browser.msie && window == top ) (function(){
//  2349:     // and execute any waiting functions

if ($v.r($v.r(jQuery, 'browser'), 'msie') && $v.ro('window') == $v.ro('top')) $v.cf($v.dis(___.frozenFunc(function ($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//  2340:     if (jQuery.isReady) return;

if ($v.r(jQuery, 'isReady')) return;

// *** jquery.js ***
//  2341:     try {
//  2342:       // If IE is used, use the trick by Diego Perini
//  2343:       // http://javascript.nwbox.com/IEContentLoaded/
//  2344:       document.documentElement.doScroll("left");
//  2348:     }

try {
$v.cm($v.r($v.ro('document'), 'documentElement'), 'doScroll', [ 'left' ]);
} catch (ex___) {

// *** jquery.js ***
//  2345:     } catch( error ) {
//  2346:       setTimeout( arguments.callee, 0 );

try {
throw ___.tameException(ex___);
} catch (error) {
$v.cf($v.ro('setTimeout'), [ $v.r(Array.slice(a___, 1), 'callee'), 0 ]);

// *** jquery.js ***
//  2347:       return;

return;
}
}
$v.cm(jQuery, 'ready', [ ]);

// *** jquery.js ***
//  2350:     jQuery.ready();

})), [ ]);

// *** jquery.js ***
//  2353:   if ( jQuery.browser.opera )
//  2354:     document.addEventListener( "DOMContentLoaded", function () {
//  2361:       // and execute any waiting functions

if ($v.r($v.r(jQuery, 'browser'), 'opera')) $v.cm($v.ro('document'), 'addEventListener', [ 'DOMContentLoaded', $v.dis(___.frozenFunc(function ($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//  2355:       if (jQuery.isReady) return;

if ($v.r(jQuery, 'isReady')) return;

// *** jquery.js ***
//  2356:       for (var i = 0; i < document.styleSheets.length; i++)
//  2357:         if (document.styleSheets[i].disabled) {
//  2358:           setTimeout( arguments.callee, 0 );
//  2359:           return;
//  2360:         }

for (var i = 0; i < $v.r($v.r($v.ro('document'), 'styleSheets'), 'length'); i++) if ($v.r($v.r($v.r($v.ro('document'), 'styleSheets'), i), 'disabled')) {
$v.cf($v.ro('setTimeout'), [ $v.r(Array.slice(a___, 1), 'callee'), 0 ]);
return;
}
$v.cm(jQuery, 'ready', [ ]);

// *** jquery.js ***
//  2362:       jQuery.ready();

})), false ]);

// *** jquery.js ***
//  2365:   if ( jQuery.browser.safari ) {

if ($v.r($v.r(jQuery, 'browser'), 'safari')) {

// *** jquery.js ***
//  2366:     var numStyles;

var numStyles;
$v.cf($v.dis(___.frozenFunc(function ($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//  2368:       if (jQuery.isReady) return;

if ($v.r(jQuery, 'isReady')) return;

// *** jquery.js ***
//  2369:       if ( document.readyState != "loaded" && document.readyState != "complete" ) {
//  2370:         setTimeout( arguments.callee, 0 );
//  2372:       }

if ($v.r($v.ro('document'), 'readyState') != 'loaded' && $v.r($v.ro('document'), 'readyState') != 'complete') {
$v.cf($v.ro('setTimeout'), [ $v.r(Array.slice(a___, 1), 'callee'), 0 ]);

// *** jquery.js ***
//  2371:         return;

return;
}

// *** jquery.js ***
//  2373:       if ( numStyles === undefined )
//  2374:         numStyles = jQuery("style, link[rel=stylesheet]").length;

if (numStyles === $v.ro('undefined')) numStyles = $v.r($v.cf(jQuery, [ 'style, link[rel=stylesheet]' ]), 'length');

// *** jquery.js ***
//  2375:       if ( document.styleSheets.length != numStyles ) {
//  2376:         setTimeout( arguments.callee, 0 );
//  2378:       }

if ($v.r($v.r($v.ro('document'), 'styleSheets'), 'length') != numStyles) {
$v.cf($v.ro('setTimeout'), [ $v.r(Array.slice(a___, 1), 'callee'), 0 ]);

// *** jquery.js ***
//  2377:         return;

return;
}
$v.cm(jQuery, 'ready', [ ]);

// *** jquery.js ***
//  2380:       jQuery.ready();

})), [ ]);
}

// *** jquery.js ***
//  2385:   jQuery.event.add( window, "load", jQuery.ready );

$v.cm($v.r(jQuery, 'event'), 'add', [ $v.ro('window'), 'load', $v.r(jQuery, 'ready') ]);
}
___.func(bindReady$_caller, 'bindReady$_caller');
var evalScript;
;

// *** jquery.js ***
//   528: function evalScript( i, elem ) {

evalScript = $v.dis(___.primFreeze(evalScript$_caller), 'evalScript');
var now;
;

// *** jquery.js ***
//   543: function now(){

now = $v.dis(___.primFreeze(now$_caller), 'now');
var num;
;
num = $v.dis(___.primFreeze(num$_caller), 'num');
var bindReady;
;

// *** jquery.js ***
//  2328: function bindReady(){

bindReady = $v.dis(___.primFreeze(bindReady$_caller), 'bindReady');

// *** jquery.js ***
//    14: var _jQuery = window.jQuery,
//    15: // Map over the $ in case of overwrite
//    16:   _$ = window.$;

var _jQuery = $v.r($v.ro('window'), 'jQuery'), _$ = $v.r($v.ro('window'), '$');

// *** jquery.js ***
//    21: };

var jQuery = $v.s($v.ro('window'), 'jQuery', $v.s($v.ro('window'), '$', ___.frozenFunc(function () {

// *** jquery.js ***
//    18: var jQuery = window.jQuery = window.$ = function( selector, context ) {
//    19:   // The jQuery object is actually just the init constructor 'enhanced'

function $$_meth$($dis, selector, context) {

// *** jquery.js ***
//    20:   return new jQuery.fn.init( selector, context );

return $v.construct($v.r($v.r(jQuery, 'fn'), 'init'), [ selector, context ]);
}
___.func($$_meth$, '$$_meth$');
;
var $$_meth = $v.dis(___.primFreeze($$_meth$), '$$_meth');
return $$_meth;
}).CALL___()));

// *** jquery.js ***
//    25: var quickExpr = /^[^<]*(<(.|\s)+>)[^>]*$|^#(\w+)$/,
//    26: 
//    27: // Is it a simple selector
//    28:   isSimple = /^.[^:#\[\.]*$/;

var quickExpr = $v.construct(RegExp, [ '^[^\x3c]*(\x3c(.|\\s)+\x3e)[^\x3e]*$|^#(\\w+)$' ]), isSimple = $v.construct(RegExp, [ '^.[^:#\\[\\.]*$' ]);

// *** jquery.js ***
//    30: jQuery.fn = jQuery.prototype = {

$v.s(jQuery, 'fn', $v.s(jQuery, 'prototype', ___.initializeMap([ 'init', ___.frozenFunc(function () {

// *** jquery.js ***
//    31:   init: function( selector, context ) {
//    32:     // Make sure that a selection was provided
//    34: 
//    35:     // Handle $(DOMElement)
//    37:       this[0] = selector;
//    38:       this.length = 1;
//    40:     }
//    41:     // Handle HTML strings
//    43:       // Are we dealing with HTML string or an ID?
//    45: 
//    46:       // Verify a match, and that no context was specified for #id
//    73:         return jQuery( context ).find( selector );
//    74: 
//    75:     // HANDLE: $(function)
//    76:     // Shortcut for document ready
//    78:       return jQuery( document )[ jQuery.fn.ready ? "ready" : "load" ]( selector );
//    79: 

function init$_lit$($dis, selector, context) {

// *** jquery.js ***
//    33:     selector = selector || document;

selector = selector || $v.ro('document');

// *** jquery.js ***
//    36:     if ( selector.nodeType ) {

if ($v.r(selector, 'nodeType')) {
$v.s($dis, 0, selector);
$v.s($dis, 'length', 1);

// *** jquery.js ***
//    39:       return this;

return $dis;
}

// *** jquery.js ***
//    42:     if ( typeof selector == "string" ) {

if ($v.typeOf(selector) == 'string') {

// *** jquery.js ***
//    44:       var match = quickExpr.exec( selector );

var match = $v.cm(quickExpr, 'exec', [ selector ]);

// *** jquery.js ***
//    47:       if ( match && (match[1] || !context) ) {
//    48: 
//    49:         // HANDLE: $(html) -> $(array)
//    69: 
//    70:       // HANDLE: $(expr, [context])
//    71:       // (which is just equivalent to: $(content).find(expr)
//    72:       } else

if (match && ($v.r(match, 1) || !context)) {

// *** jquery.js ***
//    50:         if ( match[1] )
//    51:           selector = jQuery.clean( [ match[1] ], context );
//    52: 
//    53:         // HANDLE: $("#id")
//    54:         else {
//    56: 
//    57:           // Make sure an element was located
//    68:         }

if ($v.r(match, 1)) selector = $v.cm(jQuery, 'clean', [ [ $v.r(match, 1) ], context ]);
else {

// *** jquery.js ***
//    55:           var elem = document.getElementById( match[3] );

var elem = $v.cm($v.ro('document'), 'getElementById', [ $v.r(match, 3) ]);

// *** jquery.js ***
//    58:           if ( elem ){
//    59:             // Handle the case where IE and Opera return items
//    60:             // by name instead of ID
//    63: 
//    64:             // Otherwise, we inject the element directly into the jQuery object
//    66:           }

if (elem) {

// *** jquery.js ***
//    61:             if ( elem.id != match[3] )
//    62:               return jQuery().find( selector );

if ($v.r(elem, 'id') != $v.r(match, 3)) return $v.cm($v.cf(jQuery, [ ]), 'find', [ selector ]);

// *** jquery.js ***
//    65:             return jQuery( elem );

return $v.cf(jQuery, [ elem ]);
}

// *** jquery.js ***
//    67:           selector = [];

selector = [ ];
}
} else return $v.cm($v.cf(jQuery, [ context ]), 'find', [ selector ]);

// *** jquery.js ***
//    77:     } else if ( jQuery.isFunction( selector ) )

} else if ($v.cm(jQuery, 'isFunction', [ selector ])) return $v.cm($v.cf(jQuery, [ $v.ro('document') ]), $v.r($v.r(jQuery, 'fn'), 'ready') ? 'ready': 'load', [ selector ]);

// *** jquery.js ***
//    80:     return this.setArray(jQuery.makeArray(selector));

return $v.cm($dis, 'setArray', [ $v.cm(jQuery, 'makeArray', [ selector ]) ]);
}
___.func(init$_lit$, 'init$_lit$');
;
var init$_lit = $v.dis(___.primFreeze(init$_lit$), 'init$_lit');
return init$_lit;
}).CALL___(), 'jquery', '1.2.6', 'size', ___.frozenFunc(function () {
function size$_lit$($dis) {

// *** jquery.js ***
//    88:     return this.length;

return $v.r($dis, 'length');
}
___.func(size$_lit$, 'size$_lit$');
;
var size$_lit = $v.dis(___.primFreeze(size$_lit$), 'size$_lit');
return size$_lit;
}).CALL___(), 'length', 0, 'get', ___.frozenFunc(function () {

// *** jquery.js ***
//    96:   get: function( num ) {
//    98: 
//    99:       // Return a 'clean' array
//   101: 
//   102:       // Return just the object

function get$_lit$($dis, num) {

// *** jquery.js ***
//    97:     return num == undefined ?
//   100:       jQuery.makeArray( this ) :
//   103:       this[ num ];

return num == $v.ro('undefined') ? $v.cm(jQuery, 'makeArray', [ $dis ]): $v.r($dis, num);
}
___.func(get$_lit$, 'get$_lit$');
;
var get$_lit = $v.dis(___.primFreeze(get$_lit$), 'get$_lit');
return get$_lit;
}).CALL___(), 'pushStack', ___.frozenFunc(function () {

// *** jquery.js ***
//   108:   pushStack: function( elems ) {
//   109:     // Build a new jQuery matched element set
//   111: 
//   112:     // Add the old object onto the stack (as a reference)
//   113:     ret.prevObject = this;
//   114: 
//   115:     // Return the newly-formed element set

function pushStack$_lit$($dis, elems) {

// *** jquery.js ***
//   110:     var ret = jQuery( elems );

var ret = $v.cf(jQuery, [ elems ]);
$v.s(ret, 'prevObject', $dis);

// *** jquery.js ***
//   116:     return ret;

return ret;
}
___.func(pushStack$_lit$, 'pushStack$_lit$');
;
var pushStack$_lit = $v.dis(___.primFreeze(pushStack$_lit$), 'pushStack$_lit');
return pushStack$_lit;
}).CALL___(), 'setArray', ___.frozenFunc(function () {

// *** jquery.js ***
//   122:   setArray: function( elems ) {
//   123:     // Resetting the length to 0, then using the native Array push
//   124:     // is a super-fast way to populate an object with array-like properties
//   125:     this.length = 0;
//   126:     Array.prototype.push.apply( this, elems );
//   127: 
//   128:     return this;

function setArray$_lit$($dis, elems) {
$v.s($dis, 'length', 0);
$v.cm($v.r($v.r($v.ro('Array'), 'prototype'), 'push'), 'apply', [ $dis, elems ]);
return $dis;
}
___.func(setArray$_lit$, 'setArray$_lit$');
;
var setArray$_lit = $v.dis(___.primFreeze(setArray$_lit$), 'setArray$_lit');
return setArray$_lit;
}).CALL___(), 'each', ___.frozenFunc(function () {

// *** jquery.js ***
//   134:   each: function( callback, args ) {

function each$_lit$($dis, callback, args) {

// *** jquery.js ***
//   135:     return jQuery.each( this, callback, args );

return $v.cm(jQuery, 'each', [ $dis, callback, args ]);
}
___.func(each$_lit$, 'each$_lit$');
;
var each$_lit = $v.dis(___.primFreeze(each$_lit$), 'each$_lit');
return each$_lit;
}).CALL___(), 'index', ___.frozenFunc(function () {

// *** jquery.js ***
//   140:   index: function( elem ) {
//   142: 
//   143:     // Locate the position of the desired element
//   145:       // If it receives a jQuery object, the first element is used

function index$_lit$($dis, elem) {

// *** jquery.js ***
//   141:     var ret = -1;

var ret = -1;

// *** jquery.js ***
//   144:     return jQuery.inArray(
//   146:       elem && elem.jquery ? elem[0] : elem
//   147:     , this );

return $v.cm(jQuery, 'inArray', [ elem && $v.r(elem, 'jquery') ? $v.r(elem, 0): elem, $dis ]);
}
___.func(index$_lit$, 'index$_lit$');
;
var index$_lit = $v.dis(___.primFreeze(index$_lit$), 'index$_lit');
return index$_lit;
}).CALL___(), 'attr', ___.frozenFunc(function () {

// *** jquery.js ***
//   150:   attr: function( name, value, type ) {
//   152: 
//   153:     // Look for the case where we're accessing a style value
//   162: 
//   163:     // Check to see if we're setting style values

function attr$_lit$($dis, name, value, type) {

// *** jquery.js ***
//   151:     var options = name;

var options = name;

// *** jquery.js ***
//   154:     if ( name.constructor == String )
//   155:       if ( value === undefined )
//   156:         return this[0] && jQuery[ type || "attr" ]( this[0], name );
//   157: 
//   158:       else {
//   160:         options[ name ] = value;
//   161:       }

if ($v.r(name, 'constructor') == $v.ro('String')) if (value === $v.ro('undefined')) return $v.r($dis, 0) && $v.cm(jQuery, type || 'attr', [ $v.r($dis, 0), name ]);
else {

// *** jquery.js ***
//   159:         options = {};

options = ___.initializeMap([ ]);
$v.s(options, name, value);
}

// *** jquery.js ***
//   164:     return this.each(function(i){
//   165:       // Set all the styles
//   173:     });

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis, i) {
var $caja$1;
var $caja$2;
{
$caja$1 = $v.keys(options);

// *** jquery.js ***
//   167:         jQuery.attr(
//   168:           type ?
//   169:             this.style :
//   170:             this,
//   171:           name, jQuery.prop( this, options[ name ], type, i, name )
//   172:         );

for ($caja$2 = 0; $caja$2 < ($caja$1.length_canRead___? $caja$1.length: ___.readPub($caja$1, 'length')); ++$caja$2) {

// *** jquery.js ***
//   166:       for ( name in options )

name = ___.readPub($caja$1, $caja$2);
$v.cm(jQuery, 'attr', [ type? $v.r($dis, 'style'): $dis, name, $v.cm(jQuery, 'prop', [ $dis, $v.r(options, name), type, i, name ]) ]);
}
}
})) ]);
}
___.func(attr$_lit$, 'attr$_lit$');
;
var attr$_lit = $v.dis(___.primFreeze(attr$_lit$), 'attr$_lit');
return attr$_lit;
}).CALL___(), 'css', ___.frozenFunc(function () {

// *** jquery.js ***
//   176:   css: function( key, value ) {
//   177:     // ignore negative width and height values

function css$_lit$($dis, key, value) {

// *** jquery.js ***
//   178:     if ( (key == 'width' || key == 'height') && parseFloat(value) < 0 )
//   179:       value = undefined;

if ((key == 'width' || key == 'height') && $v.cf($v.ro('parseFloat'), [ value ]) < 0) value = $v.ro('undefined');

// *** jquery.js ***
//   180:     return this.attr( key, value, "curCSS" );

return $v.cm($dis, 'attr', [ key, value, 'curCSS' ]);
}
___.func(css$_lit$, 'css$_lit$');
;
var css$_lit = $v.dis(___.primFreeze(css$_lit$), 'css$_lit');
return css$_lit;
}).CALL___(), 'text', ___.frozenFunc(function () {

// *** jquery.js ***
//   183:   text: function( text ) {
//   186: 
//   188: 
//   189:     jQuery.each( text || this, function(){
//   190:       jQuery.each( this.childNodes, function(){
//   196:     });
//   197: 

function text$_lit$($dis, text) {

// *** jquery.js ***
//   184:     if ( typeof text != "object" && text != null )
//   185:       return this.empty().append( (this[0] && this[0].ownerDocument || document).createTextNode( text ) );

if ($v.typeOf(text) != 'object' && text != null) return $v.cm($v.cm($dis, 'empty', [ ]), 'append', [ $v.cm($v.r($dis, 0) && $v.r($v.r($dis, 0), 'ownerDocument') || $v.ro('document'), 'createTextNode', [ text ]) ]);

// *** jquery.js ***
//   187:     var ret = "";

var ret = '';
$v.cm(jQuery, 'each', [ text || $dis, $v.dis(___.frozenFunc(function ($dis) {
$v.cm(jQuery, 'each', [ $v.r($dis, 'childNodes'), $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//   191:         if ( this.nodeType != 8 )
//   192:           ret += this.nodeType != 1 ?
//   193:             this.nodeValue :
//   194:             jQuery.fn.text( [ this ] );

if ($v.r($dis, 'nodeType') != 8) ret = ret + ($v.r($dis, 'nodeType') != 1? $v.r($dis, 'nodeValue'): $v.cm($v.r(jQuery, 'fn'), 'text', [ [ $dis ] ]));
})) ]);

// *** jquery.js ***
//   195:       });

})) ]);

// *** jquery.js ***
//   198:     return ret;

return ret;
}
___.func(text$_lit$, 'text$_lit$');
;
var text$_lit = $v.dis(___.primFreeze(text$_lit$), 'text$_lit');
return text$_lit;
}).CALL___(), 'wrapAll', ___.frozenFunc(function () {

// *** jquery.js ***
//   201:   wrapAll: function( html ) {
//   202:     if ( this[0] )
//   203:       // The elements to wrap the target around
//   214:         })
//   215:         .append(this);
//   216: 
//   217:     return this;

function wrapAll$_lit$($dis, html) {

// *** jquery.js ***
//   204:       jQuery( html, this[0].ownerDocument )
//   205:         .clone()
//   206:         .insertBefore( this[0] )
//   207:         .map(function(){
//   209: 
//   212: 

if ($v.r($dis, 0)) $v.cm($v.cm($v.cm($v.cm($v.cf(jQuery, [ html, $v.r($v.r($dis, 0), 'ownerDocument') ]), 'clone', [ ]), 'insertBefore', [ $v.r($dis, 0) ]), 'map', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//   208:           var elem = this;

var elem = $dis;

// *** jquery.js ***
//   210:           while ( elem.firstChild )
//   211:             elem = elem.firstChild;

while ($v.r(elem, 'firstChild')) elem = $v.r(elem, 'firstChild');

// *** jquery.js ***
//   213:           return elem;

return elem;
})) ]), 'append', [ $dis ]);
return $dis;
}
___.func(wrapAll$_lit$, 'wrapAll$_lit$');
;
var wrapAll$_lit = $v.dis(___.primFreeze(wrapAll$_lit$), 'wrapAll$_lit');
return wrapAll$_lit;
}).CALL___(), 'wrapInner', ___.frozenFunc(function () {

// *** jquery.js ***
//   220:   wrapInner: function( html ) {

function wrapInner$_lit$($dis, html) {

// *** jquery.js ***
//   221:     return this.each(function(){
//   223:     });

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm($v.cm($v.cf(jQuery, [ $dis ]), 'contents', [ ]), 'wrapAll', [ html ]);

// *** jquery.js ***
//   222:       jQuery( this ).contents().wrapAll( html );

})) ]);
}
___.func(wrapInner$_lit$, 'wrapInner$_lit$');
;
var wrapInner$_lit = $v.dis(___.primFreeze(wrapInner$_lit$), 'wrapInner$_lit');
return wrapInner$_lit;
}).CALL___(), 'wrap', ___.frozenFunc(function () {

// *** jquery.js ***
//   226:   wrap: function( html ) {

function wrap$_lit$($dis, html) {

// *** jquery.js ***
//   227:     return this.each(function(){
//   229:     });

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm($v.cf(jQuery, [ $dis ]), 'wrapAll', [ html ]);

// *** jquery.js ***
//   228:       jQuery( this ).wrapAll( html );

})) ]);
}
___.func(wrap$_lit$, 'wrap$_lit$');
;
var wrap$_lit = $v.dis(___.primFreeze(wrap$_lit$), 'wrap$_lit');
return wrap$_lit;
}).CALL___(), 'append', ___.frozenFunc(function () {

// *** jquery.js ***
//   236:     });

function append$_lit$($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//   233:     return this.domManip(arguments, true, false, function(elem){

return $v.cm($dis, 'domManip', [ Array.slice(a___, 1), true, false, $v.dis(___.frozenFunc(function ($dis, elem) {

// *** jquery.js ***
//   234:       if (this.nodeType == 1)
//   235:         this.appendChild( elem );

if ($v.r($dis, 'nodeType') == 1) $v.cm($dis, 'appendChild', [ elem ]);
})) ]);
}
___.func(append$_lit$, 'append$_lit$');
;
var append$_lit = $v.dis(___.primFreeze(append$_lit$), 'append$_lit');
return append$_lit;
}).CALL___(), 'prepend', ___.frozenFunc(function () {
function prepend$_lit$($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//   240:     return this.domManip(arguments, true, true, function(elem){
//   243:     });

return $v.cm($dis, 'domManip', [ Array.slice(a___, 1), true, true, $v.dis(___.frozenFunc(function ($dis, elem) {

// *** jquery.js ***
//   241:       if (this.nodeType == 1)
//   242:         this.insertBefore( elem, this.firstChild );

if ($v.r($dis, 'nodeType') == 1) $v.cm($dis, 'insertBefore', [ elem, $v.r($dis, 'firstChild') ]);
})) ]);
}
___.func(prepend$_lit$, 'prepend$_lit$');
;
var prepend$_lit = $v.dis(___.primFreeze(prepend$_lit$), 'prepend$_lit');
return prepend$_lit;
}).CALL___(), 'before', ___.frozenFunc(function () {
function before$_lit$($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//   247:     return this.domManip(arguments, false, false, function(elem){
//   249:     });

return $v.cm($dis, 'domManip', [ Array.slice(a___, 1), false, false, $v.dis(___.frozenFunc(function ($dis, elem) {
$v.cm($v.r($dis, 'parentNode'), 'insertBefore', [ elem, $dis ]);

// *** jquery.js ***
//   248:       this.parentNode.insertBefore( elem, this );

})) ]);
}
___.func(before$_lit$, 'before$_lit$');
;
var before$_lit = $v.dis(___.primFreeze(before$_lit$), 'before$_lit');
return before$_lit;
}).CALL___(), 'after', ___.frozenFunc(function () {

// *** jquery.js ***
//   255:     });

function after$_lit$($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//   253:     return this.domManip(arguments, false, true, function(elem){

return $v.cm($dis, 'domManip', [ Array.slice(a___, 1), false, true, $v.dis(___.frozenFunc(function ($dis, elem) {
$v.cm($v.r($dis, 'parentNode'), 'insertBefore', [ elem, $v.r($dis, 'nextSibling') ]);

// *** jquery.js ***
//   254:       this.parentNode.insertBefore( elem, this.nextSibling );

})) ]);
}
___.func(after$_lit$, 'after$_lit$');
;
var after$_lit = $v.dis(___.primFreeze(after$_lit$), 'after$_lit');
return after$_lit;
}).CALL___(), 'end', ___.frozenFunc(function () {
function end$_lit$($dis) {

// *** jquery.js ***
//   259:     return this.prevObject || jQuery( [] );

return $v.r($dis, 'prevObject') || $v.cf(jQuery, [ [ ] ]);
}
___.func(end$_lit$, 'end$_lit$');
;
var end$_lit = $v.dis(___.primFreeze(end$_lit$), 'end$_lit');
return end$_lit;
}).CALL___(), 'find', ___.frozenFunc(function () {

// *** jquery.js ***
//   262:   find: function( selector ) {
//   265:     });
//   266: 

function find$_lit$($dis, selector) {

// *** jquery.js ***
//   263:     var elems = jQuery.map(this, function(elem){

var elems = $v.cm(jQuery, 'map', [ $dis, $v.dis(___.frozenFunc(function ($dis, elem) {

// *** jquery.js ***
//   264:       return jQuery.find( selector, elem );

return $v.cm(jQuery, 'find', [ selector, elem ]);
})) ]);

// *** jquery.js ***
//   267:     return this.pushStack( /[^+>] [^+>]/.test( selector ) || selector.indexOf("..") > -1 ?
//   268:       jQuery.unique( elems ) :
//   269:       elems );

return $v.cm($dis, 'pushStack', [ $v.cm($v.construct(RegExp, [ '[^+\x3e] [^+\x3e]' ]), 'test', [ selector ]) || $v.cm(selector, 'indexOf', [ '..' ]) > -1? $v.cm(jQuery, 'unique', [ elems ]): elems ]);
}
___.func(find$_lit$, 'find$_lit$');
;
var find$_lit = $v.dis(___.primFreeze(find$_lit$), 'find$_lit');
return find$_lit;
}).CALL___(), 'clone', ___.frozenFunc(function () {

// *** jquery.js ***
//   272:   clone: function( events ) {
//   273:     // Do the clone
//   290:     });
//   291: 
//   292:     // Need to set the expando to null on the cloned set if it exists
//   293:     // removeData doesn't work here, IE removes it from the original as well
//   294:     // this is primarily for IE but the data expando shouldn't be copied over in any browser
//   298:     });
//   299: 
//   300:     // Copy the events from the original to the clone
//   310:       });
//   311: 
//   312:     // Return the cloned set

function clone$_lit$($dis, events) {

// *** jquery.js ***
//   274:     var ret = this.map(function(){
var ret = $v.cm($dis, 'map', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//   275:       if ( jQuery.browser.msie && !jQuery.isXMLDoc(this) ) {
//   276:         // IE copies events bound via attachEvent when
//   277:         // using cloneNode. Calling detachEvent on the
//   278:         // clone will also remove the events from the orignal
//   279:         // In order to get around this, we use innerHTML.
//   280:         // Unfortunately, this means some modifications to
//   281:         // attributes in IE that are actually only stored
//   282:         // as properties will not be copied (such as the
//   283:         // the name attribute on an input).
//   286:         container.appendChild(clone);
//   289:         return this.cloneNode(true);

if ($v.r($v.r(jQuery, 'browser'), 'msie') && !$v.cm(jQuery, 'isXMLDoc', [ $dis ])) {

// *** jquery.js ***
//   284:         var clone = this.cloneNode(true),
//   285:           container = document.createElement("div");

var clone = $v.cm($dis, 'cloneNode', [ true ]), container = $v.cm($v.ro('document'), 'createElement', [ 'div' ]);
$v.cm(container, 'appendChild', [ clone ]);

// *** jquery.js ***
//   287:         return jQuery.clean([container.innerHTML])[0];

return $v.r($v.cm(jQuery, 'clean', [ [ $v.r(container, 'innerHTML') ] ]), 0);

// *** jquery.js ***
//   288:       } else

} else return $v.cm($dis, 'cloneNode', [ true ]);
})) ]);

// *** jquery.js ***
//   295:     var clone = ret.find("*").andSelf().each(function(){

var clone = $v.cm($v.cm($v.cm(ret, 'find', [ '*' ]), 'andSelf', [ ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//   296:       if ( this[ expando ] != undefined )
//   297:         this[ expando ] = null;

if ($v.r($dis, expando) != $v.ro('undefined')) $v.s($dis, expando, null);
})) ]);

// *** jquery.js ***
//   301:     if ( events === true )
//   302:       this.find("*").andSelf().each(function(i){
//   306: 

if (events === true) $v.cm($v.cm($v.cm($dis, 'find', [ '*' ]), 'andSelf', [ ]), 'each', [ $v.dis(___.frozenFunc(function ($dis, i) {
var $caja$3;
var $caja$4;
var $caja$5;
var $caja$6;

// *** jquery.js ***
//   303:         if (this.nodeType == 3)
//   304:           return;

if ($v.r($dis, 'nodeType') == 3) return;

// *** jquery.js ***
//   305:         var events = jQuery.data( this, "events" );

var events = $v.cm(jQuery, 'data', [ $dis, 'events' ]);
{
$caja$3 = $v.keys(events);
for ($caja$4 = 0; $caja$4 < ($caja$3.length_canRead___? $caja$3.length: ___.readPub($caja$3, 'length')); ++$caja$4) {

// *** jquery.js ***
//   307:         for ( var type in events )

var type = ___.readPub($caja$3, $caja$4);
{
$caja$5 = $v.keys($v.r(events, type));

// *** jquery.js ***
//   309:             jQuery.event.add( clone[ i ], type, events[ type ][ handler ], events[ type ][ handler ].data );

for ($caja$6 = 0; $caja$6 < ($caja$5.length_canRead___? $caja$5.length: ___.readPub($caja$5, 'length')); ++$caja$6) {

// *** jquery.js ***
//   308:           for ( var handler in events[ type ] )

var handler = ___.readPub($caja$5, $caja$6);
$v.cm($v.r(jQuery, 'event'), 'add', [ $v.r(clone, i), type, $v.r($v.r(events, type), handler), $v.r($v.r($v.r(events, type), handler), 'data') ]);
}
}
}
}
})) ]);

// *** jquery.js ***
//   313:     return ret;

return ret;
}
___.func(clone$_lit$, 'clone$_lit$');
;
var clone$_lit = $v.dis(___.primFreeze(clone$_lit$), 'clone$_lit');
return clone$_lit;
}).CALL___(), 'filter', ___.frozenFunc(function () {

// *** jquery.js ***
//   316:   filter: function( selector ) {

function filter$_lit$($dis, selector) {

// *** jquery.js ***
//   317:     return this.pushStack(
//   318:       jQuery.isFunction( selector ) &&
//   319:       jQuery.grep(this, function(elem, i){
//   321:       }) ||
//   322: 
//   323:       jQuery.multiFilter( selector, this ) );

return $v.cm($dis, 'pushStack', [ $v.cm(jQuery, 'isFunction', [ selector ]) && $v.cm(jQuery, 'grep', [ $dis, $v.dis(___.frozenFunc(function ($dis, elem, i) {

// *** jquery.js ***
//   320:         return selector.call( elem, i );

return $v.cm(selector, 'call', [ elem, i ]);
})) ]) || $v.cm(jQuery, 'multiFilter', [ selector, $dis ]) ]);
}
___.func(filter$_lit$, 'filter$_lit$');
;
var filter$_lit = $v.dis(___.primFreeze(filter$_lit$), 'filter$_lit');
return filter$_lit;
}).CALL___(), 'not', ___.frozenFunc(function () {

// *** jquery.js ***
//   326:   not: function( selector ) {
//   328:       // test special case where just one selector is passed in
//   333: 

function not$_lit$($dis, selector) {

// *** jquery.js ***
//   327:     if ( selector.constructor == String )
//   329:       if ( isSimple.test( selector ) )
//   330:         return this.pushStack( jQuery.multiFilter( selector, this, true ) );
//   331:       else
//   332:         selector = jQuery.multiFilter( selector, this );

if ($v.r(selector, 'constructor') == $v.ro('String')) if ($v.cm(isSimple, 'test', [ selector ])) return $v.cm($dis, 'pushStack', [ $v.cm(jQuery, 'multiFilter', [ selector, $dis, true ]) ]);
else selector = $v.cm(jQuery, 'multiFilter', [ selector, $dis ]);

// *** jquery.js ***
//   334:     var isArrayLike = selector.length && selector[selector.length - 1] !== undefined && !selector.nodeType;

var isArrayLike = $v.r(selector, 'length') && $v.r(selector, $v.r(selector, 'length') - 1) !== $v.ro('undefined') && !$v.r(selector, 'nodeType');

// *** jquery.js ***
//   335:     return this.filter(function() {
//   337:     });

return $v.cm($dis, 'filter', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//   336:       return isArrayLike ? jQuery.inArray( this, selector ) < 0 : this != selector;

return isArrayLike? $v.cm(jQuery, 'inArray', [ $dis, selector ]) < 0: $dis != selector;
})) ]);
}
___.func(not$_lit$, 'not$_lit$');
;
var not$_lit = $v.dis(___.primFreeze(not$_lit$), 'not$_lit');
return not$_lit;
}).CALL___(), 'add', ___.frozenFunc(function () {

// *** jquery.js ***
//   340:   add: function( selector ) {

function add$_lit$($dis, selector) {

// *** jquery.js ***
//   341:     return this.pushStack( jQuery.unique( jQuery.merge(
//   342:       this.get(),
//   343:       typeof selector == 'string' ?
//   344:         jQuery( selector ) :
//   345:         jQuery.makeArray( selector )
//   346:     )));

return $v.cm($dis, 'pushStack', [ $v.cm(jQuery, 'unique', [ $v.cm(jQuery, 'merge', [ $v.cm($dis, 'get', [ ]), $v.typeOf(selector) == 'string'? $v.cf(jQuery, [ selector ]): $v.cm(jQuery, 'makeArray', [ selector ]) ]) ]) ]);
}
___.func(add$_lit$, 'add$_lit$');
;
var add$_lit = $v.dis(___.primFreeze(add$_lit$), 'add$_lit');
return add$_lit;
}).CALL___(), 'is', ___.frozenFunc(function () {

// *** jquery.js ***
//   349:   is: function( selector ) {

function is$_lit$($dis, selector) {

// *** jquery.js ***
//   350:     return !!selector && jQuery.multiFilter( selector, this ).length > 0;

return ! (!selector) && $v.r($v.cm(jQuery, 'multiFilter', [ selector, $dis ]), 'length') > 0;
}
___.func(is$_lit$, 'is$_lit$');
;
var is$_lit = $v.dis(___.primFreeze(is$_lit$), 'is$_lit');
return is$_lit;
}).CALL___(), 'hasClass', ___.frozenFunc(function () {

// *** jquery.js ***
//   353:   hasClass: function( selector ) {

function hasClass$_lit$($dis, selector) {

// *** jquery.js ***
//   354:     return this.is( "." + selector );

return $v.cm($dis, 'is', [ '.' + selector ]);
}
___.func(hasClass$_lit$, 'hasClass$_lit$');
;
var hasClass$_lit = $v.dis(___.primFreeze(hasClass$_lit$), 'hasClass$_lit');
return hasClass$_lit;
}).CALL___(), 'val', ___.frozenFunc(function () {

// *** jquery.js ***
//   357:   val: function( value ) {
//   359: 
//   398: 
//   400:     }
//   401: 
//   404: 
//   426:     });

function val$_lit$($dis, value) {

// *** jquery.js ***
//   358:     if ( value == undefined ) {

if (value == $v.ro('undefined')) {

// *** jquery.js ***
//   360:       if ( this.length ) {
//   362: 
//   363:         // We need to handle select boxes special
//   396: 
//   397:       }

if ($v.r($dis, 'length')) {

// *** jquery.js ***
//   361:         var elem = this[0];

var elem = $v.r($dis, 0);

// *** jquery.js ***
//   364:         if ( jQuery.nodeName( elem, "select" ) ) {
//   369: 
//   370:           // Nothing was selected
//   373: 
//   374:           // Loop through all the selected options
//   390: 
//   392: 
//   393:         // Everything else, we just grab the value
//   394:         } else
//   395:           return (this[0].value || "").replace(/\r/g, "");

if ($v.cm(jQuery, 'nodeName', [ elem, 'select' ])) {

// *** jquery.js ***
//   365:           var index = elem.selectedIndex,
//   366:             values = [],
//   367:             options = elem.options,
//   368:             one = elem.type == "select-one";

var index = $v.r(elem, 'selectedIndex'), values = [ ], options = $v.r(elem, 'options'), one = $v.r(elem, 'type') == 'select-one';

// *** jquery.js ***
//   371:           if ( index < 0 )
//   372:             return null;

if (index < 0) return null;

// *** jquery.js ***
//   375:           for ( var i = one ? index : 0, max = one ? index + 1 : options.length; i < max; i++ ) {
//   377: 
//   389:           }

for (var i = one? index: 0, max = one? index + 1: $v.r(options, 'length'); i < max; i++) {

// *** jquery.js ***
//   376:             var option = options[ i ];

var option = $v.r(options, i);

// *** jquery.js ***
//   378:             if ( option.selected ) {
//   379:               // Get the specifc value for the option
//   381: 
//   382:               // We don't need an array for one selects
//   385: 
//   386:               // Multi-Selects return an array
//   387:               values.push( value );
//   388:             }

if ($v.r(option, 'selected')) {

// *** jquery.js ***
//   380:               value = jQuery.browser.msie && !option.attributes.value.specified ? option.text : option.value;

value = $v.r($v.r(jQuery, 'browser'), 'msie') && !$v.r($v.r($v.r(option, 'attributes'), 'value'), 'specified') ? $v.r(option, 'text'): $v.r(option, 'value');

// *** jquery.js ***
//   383:               if ( one )
//   384:                 return value;

if (one) return value;
$v.cm(values, 'push', [ value ]);
}
}

// *** jquery.js ***
//   391:           return values;

return values;
} else return $v.cm($v.r($v.r($dis, 0), 'value') || '', 'replace', [ $v.construct(RegExp, [ '\\r', 'g' ]), '' ]);
}

// *** jquery.js ***
//   399:       return undefined;

return $v.ro('undefined');
}

// *** jquery.js ***
//   402:     if( value.constructor == Number )
//   403:       value += '';

if ($v.r(value, 'constructor') == $v.ro('Number')) value = value + '';

// *** jquery.js ***
//   405:     return this.each(function(){
//   408: 

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//   406:       if ( this.nodeType != 1 )
//   407:         return;

if ($v.r($dis, 'nodeType') != 1) return;

// *** jquery.js ***
//   409:       if ( value.constructor == Array && /radio|checkbox/.test( this.type ) )
//   410:         this.checked = (jQuery.inArray(this.value, value) >= 0 ||
//   411:           jQuery.inArray(this.name, value) >= 0);
//   412: 
//   413:       else if ( jQuery.nodeName( this, "select" ) ) {
//   415: 
//   416:         jQuery( "option", this ).each(function(){
//   417:           this.selected = (jQuery.inArray( this.value, values ) >= 0 ||
//   419:         });
//   420: 
//   423: 
//   425:         this.value = value;

if ($v.r(value, 'constructor') == $v.ro('Array') && $v.cm($v.construct(RegExp, [ 'radio|checkbox' ]), 'test', [ $v.r($dis, 'type') ])) $v.s($dis, 'checked', $v.cm(jQuery, 'inArray', [ $v.r($dis, 'value'), value ]) >= 0 || $v.cm(jQuery, 'inArray', [ $v.r($dis, 'name'), value ]) >= 0);
else if ($v.cm(jQuery, 'nodeName', [ $dis, 'select' ])) {

// *** jquery.js ***
//   414:         var values = jQuery.makeArray(value);

var values = $v.cm(jQuery, 'makeArray', [ value ]);
$v.cm($v.cf(jQuery, [ 'option', $dis ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.s($dis, 'selected', $v.cm(jQuery, 'inArray', [ $v.r($dis, 'value'), values ]) >= 0 || $v.cm(jQuery, 'inArray', [ $v.r($dis, 'text'), values ]) >= 0);

// *** jquery.js ***
//   418:             jQuery.inArray( this.text, values ) >= 0);

})) ]);

// *** jquery.js ***
//   421:         if ( !values.length )
//   422:           this.selectedIndex = -1;

if (!$v.r(values, 'length')) $v.s($dis, 'selectedIndex', -1);

// *** jquery.js ***
//   424:       } else

} else $v.s($dis, 'value', value);
})) ]);
}
___.func(val$_lit$, 'val$_lit$');
;
var val$_lit = $v.dis(___.primFreeze(val$_lit$), 'val$_lit');
return val$_lit;
}).CALL___(), 'html', ___.frozenFunc(function () {

// *** jquery.js ***
//   429:   html: function( value ) {

function html$_lit$($dis, value) {

// *** jquery.js ***
//   430:     return value == undefined ?
//   431:       (this[0] ?
//   432:         this[0].innerHTML :
//   433:         null) :
//   434:       this.empty().append( value );

return value == $v.ro('undefined') ? $v.r($dis, 0) ? $v.r($v.r($dis, 0), 'innerHTML'): null: $v.cm($v.cm($dis, 'empty', [ ]), 'append', [ value ]);
}
___.func(html$_lit$, 'html$_lit$');
;
var html$_lit = $v.dis(___.primFreeze(html$_lit$), 'html$_lit');
return html$_lit;
}).CALL___(), 'replaceWith', ___.frozenFunc(function () {

// *** jquery.js ***
//   437:   replaceWith: function( value ) {

function replaceWith$_lit$($dis, value) {

// *** jquery.js ***
//   438:     return this.after( value ).remove();

return $v.cm($v.cm($dis, 'after', [ value ]), 'remove', [ ]);
}
___.func(replaceWith$_lit$, 'replaceWith$_lit$');
;
var replaceWith$_lit = $v.dis(___.primFreeze(replaceWith$_lit$), 'replaceWith$_lit');
return replaceWith$_lit;
}).CALL___(), 'eq', ___.frozenFunc(function () {

// *** jquery.js ***
//   441:   eq: function( i ) {

function eq$_lit$($dis, i) {

// *** jquery.js ***
//   442:     return this.slice( i, i + 1 );

return $v.cm($dis, 'slice', [ i, i + 1 ]);
}
___.func(eq$_lit$, 'eq$_lit$');
;
var eq$_lit = $v.dis(___.primFreeze(eq$_lit$), 'eq$_lit');
return eq$_lit;
}).CALL___(), 'slice', ___.frozenFunc(function () {
function slice$_lit$($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//   446:     return this.pushStack( Array.prototype.slice.apply( this, arguments ) );

return $v.cm($dis, 'pushStack', [ $v.cm($v.r($v.r($v.ro('Array'), 'prototype'), 'slice'), 'apply', [ $dis, Array.slice(a___, 1) ]) ]);
}
___.func(slice$_lit$, 'slice$_lit$');
;
var slice$_lit = $v.dis(___.primFreeze(slice$_lit$), 'slice$_lit');
return slice$_lit;
}).CALL___(), 'map', ___.frozenFunc(function () {

// *** jquery.js ***
//   449:   map: function( callback ) {

function map$_lit$($dis, callback) {

// *** jquery.js ***
//   450:     return this.pushStack( jQuery.map(this, function(elem, i){
//   452:     }));

return $v.cm($dis, 'pushStack', [ $v.cm(jQuery, 'map', [ $dis, $v.dis(___.frozenFunc(function ($dis, elem, i) {

// *** jquery.js ***
//   451:       return callback.call( elem, i, elem );

return $v.cm(callback, 'call', [ elem, i, elem ]);
})) ]) ]);
}
___.func(map$_lit$, 'map$_lit$');
;
var map$_lit = $v.dis(___.primFreeze(map$_lit$), 'map$_lit');
return map$_lit;
}).CALL___(), 'andSelf', ___.frozenFunc(function () {
function andSelf$_lit$($dis) {

// *** jquery.js ***
//   456:     return this.add( this.prevObject );

return $v.cm($dis, 'add', [ $v.r($dis, 'prevObject') ]);
}
___.func(andSelf$_lit$, 'andSelf$_lit$');
;
var andSelf$_lit = $v.dis(___.primFreeze(andSelf$_lit$), 'andSelf$_lit');
return andSelf$_lit;
}).CALL___(), 'data', ___.frozenFunc(function () {

// *** jquery.js ***
//   459:   data: function( key, value ){
//   461:     parts[1] = parts[1] ? "." + parts[1] : "";
//   462: 
//   465: 
//   468: 
//   472:     } else
//   473:       return this.trigger("setData" + parts[1] + "!", [parts[0], value]).each(function(){
//   475:       });

function data$_lit$($dis, key, value) {

// *** jquery.js ***
//   460:     var parts = key.split(".");

var parts = $v.cm(key, 'split', [ '.' ]);
$v.s(parts, 1, $v.r(parts, 1) ? '.' + $v.r(parts, 1): '');

// *** jquery.js ***
//   463:     if ( value === undefined ) {

if (value === $v.ro('undefined')) {

// *** jquery.js ***
//   464:       var data = this.triggerHandler("getData" + parts[1] + "!", [parts[0]]);

var data = $v.cm($dis, 'triggerHandler', [ 'getData' + $v.r(parts, 1) + '!', [ $v.r(parts, 0) ] ]);

// *** jquery.js ***
//   466:       if ( data === undefined && this.length )
//   467:         data = jQuery.data( this[0], key );

if (data === $v.ro('undefined') && $v.r($dis, 'length')) data = $v.cm(jQuery, 'data', [ $v.r($dis, 0), key ]);

// *** jquery.js ***
//   469:       return data === undefined && parts[1] ?
//   470:         this.data( parts[0] ) :
//   471:         data;

return data === $v.ro('undefined') && $v.r(parts, 1) ? $v.cm($dis, 'data', [ $v.r(parts, 0) ]): data;
} else return $v.cm($v.cm($dis, 'trigger', [ 'setData' + $v.r(parts, 1) + '!', [ $v.r(parts, 0), value ] ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm(jQuery, 'data', [ $dis, key, value ]);

// *** jquery.js ***
//   474:         jQuery.data( this, key, value );

})) ]);
}
___.func(data$_lit$, 'data$_lit$');
;
var data$_lit = $v.dis(___.primFreeze(data$_lit$), 'data$_lit');
return data$_lit;
}).CALL___(), 'removeData', ___.frozenFunc(function () {

// *** jquery.js ***
//   478:   removeData: function( key ){
//   481:     });

function removeData$_lit$($dis, key) {

// *** jquery.js ***
//   479:     return this.each(function(){

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm(jQuery, 'removeData', [ $dis, key ]);

// *** jquery.js ***
//   480:       jQuery.removeData( this, key );

})) ]);
}
___.func(removeData$_lit$, 'removeData$_lit$');
;
var removeData$_lit = $v.dis(___.primFreeze(removeData$_lit$), 'removeData$_lit');
return removeData$_lit;
}).CALL___(), 'domManip', ___.frozenFunc(function () {

// *** jquery.js ***
//   484:   domManip: function( args, table, reverse, callback ) {
//   486: 
//   521:     });

function domManip$_lit$($dis, args, table, reverse, callback) {

// *** jquery.js ***
//   485:     var clone = this.length > 1, elems;

var clone = $v.r($dis, 'length') > 1, elems;

// *** jquery.js ***
//   487:     return this.each(function(){
//   494: 
//   496: 
//   499: 
//   501: 
//   502:       jQuery.each(elems, function(){
//   506: 
//   507:         // execute all scripts after the elements have been injected
//   518:       });
//   519: 

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//   488:       if ( !elems ) {
//   490: 
//   493:       }

if (!elems) {

// *** jquery.js ***
//   489:         elems = jQuery.clean( args, this.ownerDocument );

elems = $v.cm(jQuery, 'clean', [ args, $v.r($dis, 'ownerDocument') ]);

// *** jquery.js ***
//   491:         if ( reverse )
//   492:           elems.reverse();

if (reverse) $v.cm(elems, 'reverse', [ ]);
}

// *** jquery.js ***
//   495:       var obj = this;

var obj = $dis;

// *** jquery.js ***
//   497:       if ( table && jQuery.nodeName( this, "table" ) && jQuery.nodeName( elems[0], "tr" ) )
//   498:         obj = this.getElementsByTagName("tbody")[0] || this.appendChild( this.ownerDocument.createElement("tbody") );

if (table && $v.cm(jQuery, 'nodeName', [ $dis, 'table' ]) && $v.cm(jQuery, 'nodeName', [ $v.r(elems, 0), 'tr' ])) obj = $v.r($v.cm($dis, 'getElementsByTagName', [ 'tbody' ]), 0) || $v.cm($dis, 'appendChild', [ $v.cm($v.r($dis, 'ownerDocument'), 'createElement', [ 'tbody' ]) ]);

// *** jquery.js ***
//   500:       var scripts = jQuery( [] );

var scripts = $v.cf(jQuery, [ [ ] ]);
$v.cm(jQuery, 'each', [ elems, $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//   503:         var elem = clone ?
//   504:           jQuery( this ).clone( true )[0] :
//   505:           this;

var elem = clone? $v.r($v.cm($v.cf(jQuery, [ $dis ]), 'clone', [ true ]), 0): $dis;

// *** jquery.js ***
//   508:         if ( jQuery.nodeName( elem, "script" ) )
//   509:           scripts = scripts.add( elem );
//   510:         else {
//   511:           // Remove any inner scripts for later evaluation
//   514: 
//   515:           // Inject the elements into the document
//   516:           callback.call( obj, elem );
//   517:         }

if ($v.cm(jQuery, 'nodeName', [ elem, 'script' ])) scripts = $v.cm(scripts, 'add', [ elem ]);
else {

// *** jquery.js ***
//   512:           if ( elem.nodeType == 1 )
//   513:             scripts = scripts.add( jQuery( "script", elem ).remove() );

if ($v.r(elem, 'nodeType') == 1) scripts = $v.cm(scripts, 'add', [ $v.cm($v.cf(jQuery, [ 'script', elem ]), 'remove', [ ]) ]);
$v.cm(callback, 'call', [ obj, elem ]);
}
})) ]);
$v.cm(scripts, 'each', [ evalScript ]);

// *** jquery.js ***
//   520:       scripts.each( evalScript );

})) ]);
}
___.func(domManip$_lit$, 'domManip$_lit$');
;
var domManip$_lit = $v.dis(___.primFreeze(domManip$_lit$), 'domManip$_lit');
return domManip$_lit;
}).CALL___() ])));

// *** jquery.js ***
//   526: jQuery.fn.init.prototype = jQuery.fn;

$v.s($v.r($v.r(jQuery, 'fn'), 'init'), 'prototype', $v.r(jQuery, 'fn'));

// *** jquery.js ***
//   541: }

;

// *** jquery.js ***
//   545: }

;

// *** jquery.js ***
//   547: jQuery.extend = jQuery.fn.extend = function() {

$v.s(jQuery, 'extend', $v.s($v.r(jQuery, 'fn'), 'extend', ___.frozenFunc(function () {

// *** jquery.js ***
//   550: 
//   551:   // Handle a deep copy situation
//   555:     // skip the boolean and the target
//   557:   }
//   558: 
//   559:   // Handle case when target is a string or something (possible in deep copy)
//   562: 
//   563:   // extend jQuery itself if only one argument is passed
//   566:     --i;
//   567:   }
//   568: 
//   570:     // Only deal with non-null/undefined values
//   592: 
//   593:   // Return the modified object

function extend$_meth$($dis) {
var a___ = ___.args(arguments);
var $caja$7;
var $caja$8;

// *** jquery.js ***
//   549:   var target = arguments[0] || {}, i = 1, length = arguments.length, deep = false, options;

var target = $v.r(Array.slice(a___, 1), 0) || ___.initializeMap([ ]), i = 1, length = $v.r(Array.slice(a___, 1), 'length'), deep = false, options;

// *** jquery.js ***
//   552:   if ( target.constructor == Boolean ) {

if ($v.r(target, 'constructor') == $v.ro('Boolean')) {

// *** jquery.js ***
//   553:     deep = target;

deep = target;

// *** jquery.js ***
//   554:     target = arguments[1] || {};

target = $v.r(Array.slice(a___, 1), 1) || ___.initializeMap([ ]);

// *** jquery.js ***
//   556:     i = 2;

i = 2;
}

// *** jquery.js ***
//   560:   if ( typeof target != "object" && typeof target != "function" )
//   561:     target = {};

if ($v.typeOf(target) != 'object' && $v.typeOf(target) != 'function') target = ___.initializeMap([ ]);

// *** jquery.js ***
//   564:   if ( length == i ) {

if (length == i) {

// *** jquery.js ***
//   565:     target = this;

target = $dis;
--i;
}

// *** jquery.js ***
//   569:   for ( ; i < length; i++ )
//   571:     if ( (options = arguments[ i ]) != null )
//   572:       // Extend the base object
//   575: 
//   576:         // Prevent never-ending loop
//   579: 
//   580:         // Recurse if we're merging object values
//   583:             // Never move original objects, clone them
//   586: 
//   587:         // Don't bring in undefined values
//   588:         else if ( copy !== undefined )
//   589:           target[ name ] = copy;
//   590: 
//   591:       }

for (; i < length; i++) if ((options = $v.r(Array.slice(a___, 1), i)) != null) {
$caja$7 = $v.keys(options);
for ($caja$8 = 0; $caja$8 < ($caja$7.length_canRead___? $caja$7.length: ___.readPub($caja$7, 'length')); ++$caja$8) {

// *** jquery.js ***
//   573:       for ( var name in options ) {

var name = ___.readPub($caja$7, $caja$8);
{

// *** jquery.js ***
//   574:         var src = target[ name ], copy = options[ name ];

var src = $v.r(target, name), copy = $v.r(options, name);

// *** jquery.js ***
//   577:         if ( target === copy )
//   578:           continue;

if (target === copy) continue;

// *** jquery.js ***
//   581:         if ( deep && copy && typeof copy == "object" && !copy.nodeType )
//   582:           target[ name ] = jQuery.extend( deep, 
//   584:             src || ( copy.length != null ? [ ] : { } )
//   585:           , copy );

if (deep && copy && $v.typeOf(copy) == 'object' && !$v.r(copy, 'nodeType')) $v.s(target, name, $v.cm(jQuery, 'extend', [ deep, src || ($v.r(copy, 'length') != null ? [ ]: ___.initializeMap([ ])), copy ]));
else if (copy !== $v.ro('undefined')) $v.s(target, name, copy);
}
}
}

// *** jquery.js ***
//   594:   return target;

return target;
}
___.func(extend$_meth$, 'extend$_meth$');
;
var extend$_meth = $v.dis(___.primFreeze(extend$_meth$), 'extend$_meth');
return extend$_meth;
}).CALL___()));

// *** jquery.js ***
//   597: var expando = "jQuery" + now(), uuid = 0, windowData = {},
//   598:   // exclude the following css properties to add px
//   599:   exclude = /z-?index|font-?weight|opacity|zoom|line-?height/i,
//   600:   // cache defaultView
//   601:   defaultView = document.defaultView || {};

var expando = 'jQuery' + $v.cf(now, [ ]), uuid = 0, windowData = ___.initializeMap([ ]), exclude = $v.construct(RegExp, [ 'z-?index|font-?weight|opacity|zoom|line-?height', 'i' ]), defaultView = $v.r($v.ro('document'), 'defaultView') || ___.initializeMap([ ]);
$v.cm(jQuery, 'extend', [ ___.initializeMap([ 'noConflict', ___.frozenFunc(function () {

// *** jquery.js ***
//   604:   noConflict: function( deep ) {
//   605:     window.$ = _$;
//   606: 
//   609: 

function noConflict$_lit$($dis, deep) {
$v.s($v.ro('window'), '$', _$);

// *** jquery.js ***
//   607:     if ( deep )
//   608:       window.jQuery = _jQuery;

if (deep) $v.s($v.ro('window'), 'jQuery', _jQuery);

// *** jquery.js ***
//   610:     return jQuery;

return jQuery;
}
___.func(noConflict$_lit$, 'noConflict$_lit$');
;
var noConflict$_lit = $v.dis(___.primFreeze(noConflict$_lit$), 'noConflict$_lit');
return noConflict$_lit;
}).CALL___(), 'isFunction', ___.frozenFunc(function () {

// *** jquery.js ***
//   614:   isFunction: function( fn ) {

function isFunction$_lit$($dis, fn) {

// *** jquery.js ***
//   615:     return !!fn && typeof fn != "string" && !fn.nodeName &&
//   616:       fn.constructor != Array && /^[\s[]?function/.test( fn + "" );

return ! (!fn) && $v.typeOf(fn) != 'string' && !$v.r(fn, 'nodeName') && $v.r(fn, 'constructor') != $v.ro('Array') && $v.cm($v.construct(RegExp, [ '^[\\s[]?function' ]), 'test', [ fn + '' ]);
}
___.func(isFunction$_lit$, 'isFunction$_lit$');
;
var isFunction$_lit = $v.dis(___.primFreeze(isFunction$_lit$), 'isFunction$_lit');
return isFunction$_lit;
}).CALL___(), 'isXMLDoc', ___.frozenFunc(function () {

// *** jquery.js ***
//   620:   isXMLDoc: function( elem ) {

function isXMLDoc$_lit$($dis, elem) {

// *** jquery.js ***
//   621:     return elem.documentElement && !elem.body ||
//   622:       elem.tagName && elem.ownerDocument && !elem.ownerDocument.body;

return $v.r(elem, 'documentElement') && !$v.r(elem, 'body') || $v.r(elem, 'tagName') && $v.r(elem, 'ownerDocument') && !$v.r($v.r(elem, 'ownerDocument'), 'body');
}
___.func(isXMLDoc$_lit$, 'isXMLDoc$_lit$');
;
var isXMLDoc$_lit = $v.dis(___.primFreeze(isXMLDoc$_lit$), 'isXMLDoc$_lit');
return isXMLDoc$_lit;
}).CALL___(), 'globalEval', ___.frozenFunc(function () {

// *** jquery.js ***
//   626:   globalEval: function( data ) {
//   628: 
//   630:       // Inspired by code by Andrea Giammarchi
//   631:       // http://webreflection.blogspot.com/2007/08/global-scope-evaluation-and-dom.html
//   634: 
//   635:       script.type = "text/javascript";
//   638:       else
//   639:         script.appendChild( document.createTextNode( data ) );
//   640: 
//   641:       // Use insertBefore instead of appendChild  to circumvent an IE6 bug.
//   642:       // This arises when a base node is used (#2709).
//   643:       head.insertBefore( script, head.firstChild );
//   644:       head.removeChild( script );
//   645:     }

function globalEval$_lit$($dis, data) {

// *** jquery.js ***
//   627:     data = jQuery.trim( data );

data = $v.cm(jQuery, 'trim', [ data ]);

// *** jquery.js ***
//   629:     if ( data ) {

if (data) {

// *** jquery.js ***
//   632:       var head = document.getElementsByTagName("head")[0] || document.documentElement,
//   633:         script = document.createElement("script");

var head = $v.r($v.cm($v.ro('document'), 'getElementsByTagName', [ 'head' ]), 0) || $v.r($v.ro('document'), 'documentElement'), script = $v.cm($v.ro('document'), 'createElement', [ 'script' ]);
$v.s(script, 'type', 'text/javascript');

// *** jquery.js ***
//   636:       if ( jQuery.browser.msie )
//   637:         script.text = data;

if ($v.r($v.r(jQuery, 'browser'), 'msie')) $v.s(script, 'text', data);
else $v.cm(script, 'appendChild', [ $v.cm($v.ro('document'), 'createTextNode', [ data ]) ]);
$v.cm(head, 'insertBefore', [ script, $v.r(head, 'firstChild') ]);
$v.cm(head, 'removeChild', [ script ]);
}
}
___.func(globalEval$_lit$, 'globalEval$_lit$');
;
var globalEval$_lit = $v.dis(___.primFreeze(globalEval$_lit$), 'globalEval$_lit');
return globalEval$_lit;
}).CALL___(), 'nodeName', ___.frozenFunc(function () {

// *** jquery.js ***
//   648:   nodeName: function( elem, name ) {

function nodeName$_lit$($dis, elem, name) {

// *** jquery.js ***
//   649:     return elem.nodeName && elem.nodeName.toUpperCase() == name.toUpperCase();

return $v.r(elem, 'nodeName') && $v.cm($v.r(elem, 'nodeName'), 'toUpperCase', [ ]) == $v.cm(name, 'toUpperCase', [ ]);
}
___.func(nodeName$_lit$, 'nodeName$_lit$');
;
var nodeName$_lit = $v.dis(___.primFreeze(nodeName$_lit$), 'nodeName$_lit');
return nodeName$_lit;
}).CALL___(), 'cache', ___.initializeMap([ ]), 'data', ___.frozenFunc(function () {

// *** jquery.js ***
//   654:   data: function( elem, name, data ) {
//   656:       windowData :
//   657:       elem;
//   658: 
//   660: 
//   661:     // Compute a unique ID for the element
//   662:     if ( !id )
//   664: 
//   665:     // Only generate the data cache if we're
//   666:     // trying to access or manipulate it
//   669: 
//   670:     // Prevent overriding the named cache with undefined values
//   673: 
//   674:     // Return the named cache data, or the ID for the element

function data$_lit$($dis, elem, name, data) {

// *** jquery.js ***
//   655:     elem = elem == window ?

elem = elem == $v.ro('window') ? windowData: elem;

// *** jquery.js ***
//   659:     var id = elem[ expando ];

var id = $v.r(elem, expando);

// *** jquery.js ***
//   663:       id = elem[ expando ] = ++uuid;

if (!id) id = $v.s(elem, expando, ++uuid);

// *** jquery.js ***
//   667:     if ( name && !jQuery.cache[ id ] )
//   668:       jQuery.cache[ id ] = {};

if (name && !$v.r($v.r(jQuery, 'cache'), id)) $v.s($v.r(jQuery, 'cache'), id, ___.initializeMap([ ]));

// *** jquery.js ***
//   671:     if ( data !== undefined )
//   672:       jQuery.cache[ id ][ name ] = data;

if (data !== $v.ro('undefined')) $v.s($v.r($v.r(jQuery, 'cache'), id), name, data);

// *** jquery.js ***
//   675:     return name ?
//   676:       jQuery.cache[ id ][ name ] :
//   677:       id;

return name? $v.r($v.r($v.r(jQuery, 'cache'), id), name): id;
}
___.func(data$_lit$, 'data$_lit$');
;
var data$_lit = $v.dis(___.primFreeze(data$_lit$), 'data$_lit');
return data$_lit;
}).CALL___(), 'removeData', ___.frozenFunc(function () {

// *** jquery.js ***
//   680:   removeData: function( elem, name ) {
//   682:       windowData :
//   683:       elem;
//   684: 
//   686: 
//   687:     // If we want to remove a specific section of the element's data
//   690:         // Remove the section of cache data
//   691:         delete jQuery.cache[ id ][ name ];
//   692: 
//   693:         // If we've removed all the data, remove the element's cache
//   695: 
//   698: 
//   701:       }
//   702: 
//   703:     // Otherwise, we want to remove all of the element's data
//   704:     } else {
//   705:       // Clean up the element expando
//   706:       try {
//   707:         delete elem[ expando ];
//   708:       } catch(e){
//   709:         // IE has trouble directly removing the expando
//   710:         // but it's ok with using removeAttribute
//   713:       }
//   714: 
//   715:       // Completely remove the data cache
//   716:       delete jQuery.cache[ id ];
//   717:     }

function removeData$_lit$($dis, elem, name) {
var $caja$9;
var $caja$10;

// *** jquery.js ***
//   681:     elem = elem == window ?

elem = elem == $v.ro('window') ? windowData: elem;

// *** jquery.js ***
//   685:     var id = elem[ expando ];

var id = $v.r(elem, expando);

// *** jquery.js ***
//   688:     if ( name ) {

if (name) {

// *** jquery.js ***
//   689:       if ( jQuery.cache[ id ] ) {

if ($v.r($v.r(jQuery, 'cache'), id)) {
$v.remove($v.r($v.r(jQuery, 'cache'), id), name);

// *** jquery.js ***
//   694:         name = "";

name = '';
{
$caja$9 = $v.keys($v.r($v.r(jQuery, 'cache'), id));
for ($caja$10 = 0; $caja$10 < ($caja$9.length_canRead___? $caja$9.length: ___.readPub($caja$9, 'length')); ++$caja$10) {

// *** jquery.js ***
//   696:         for ( name in jQuery.cache[ id ] )

name = ___.readPub($caja$9, $caja$10);

// *** jquery.js ***
//   697:           break;

break;
}
}

// *** jquery.js ***
//   699:         if ( !name )
//   700:           jQuery.removeData( elem );

if (!name) $v.cm(jQuery, 'removeData', [ elem ]);
}
} else {
try {
$v.remove(elem, expando);
} catch (ex___) {
try {
throw ___.tameException(ex___);
} catch (e) {

// *** jquery.js ***
//   711:         if ( elem.removeAttribute )
//   712:           elem.removeAttribute( expando );

if ($v.r(elem, 'removeAttribute')) $v.cm(elem, 'removeAttribute', [ expando ]);
}
}
$v.remove($v.r(jQuery, 'cache'), id);
}
}
___.func(removeData$_lit$, 'removeData$_lit$');
;
var removeData$_lit = $v.dis(___.primFreeze(removeData$_lit$), 'removeData$_lit');
return removeData$_lit;
}).CALL___(), 'each', ___.frozenFunc(function () {

// *** jquery.js ***
//   721:   each: function( object, callback, args ) {
//   723: 
//   730:         for ( ; i < length; )
//   731:           if ( callback.apply( object[ i++ ], args ) === false )
//   732:             break;
//   733: 
//   734:     // A special, fast, case for the most common use of each
//   741:         for ( var value = object[0];
//   742:           i < length && callback.call( value, i, value ) !== false; value = object[++i] ){}
//   743:     }
//   744: 

function each$_lit$($dis, object, callback, args) {
var $caja$11;
var $caja$12;
var $caja$13;
var $caja$14;

// *** jquery.js ***
//   722:     var name, i = 0, length = object.length;

var name, i = 0, length = $v.r(object, 'length');

// *** jquery.js ***
//   724:     if ( args ) {

if (args) {

// *** jquery.js ***
//   725:       if ( length == undefined ) {
//   729:       } else

if (length == $v.ro('undefined')) {
{
$caja$11 = $v.keys(object);
for ($caja$12 = 0; $caja$12 < ($caja$11.length_canRead___? $caja$11.length: ___.readPub($caja$11, 'length')); ++$caja$12) {

// *** jquery.js ***
//   726:         for ( name in object )

name = ___.readPub($caja$11, $caja$12);

// *** jquery.js ***
//   727:           if ( callback.apply( object[ name ], args ) === false )
//   728:             break;

if ($v.cm(callback, 'apply', [ $v.r(object, name), args ]) === false) break;
}
}
} else for (; i < length;) if ($v.cm(callback, 'apply', [ $v.r(object, i++), args ]) === false) break;

// *** jquery.js ***
//   735:     } else {

} else {

// *** jquery.js ***
//   736:       if ( length == undefined ) {
//   740:       } else

if (length == $v.ro('undefined')) {
{
$caja$13 = $v.keys(object);
for ($caja$14 = 0; $caja$14 < ($caja$13.length_canRead___? $caja$13.length: ___.readPub($caja$13, 'length')); ++$caja$14) {

// *** jquery.js ***
//   737:         for ( name in object )

name = ___.readPub($caja$13, $caja$14);

// *** jquery.js ***
//   738:           if ( callback.call( object[ name ], name, object[ name ] ) === false )
//   739:             break;

if ($v.cm(callback, 'call', [ $v.r(object, name), name, $v.r(object, name) ]) === false) break;
}
}
} else for (var value = $v.r(object, 0); i < length && $v.cm(callback, 'call', [ value, i, value ]) !== false; value = $v.r(object, ++i)) {
}
}

// *** jquery.js ***
//   745:     return object;

return object;
}
___.func(each$_lit$, 'each$_lit$');
;
var each$_lit = $v.dis(___.primFreeze(each$_lit$), 'each$_lit');
return each$_lit;
}).CALL___(), 'prop', ___.frozenFunc(function () {

// *** jquery.js ***
//   748:   prop: function( elem, value, type, i, name ) {
//   749:     // Handle executable functions
//   752: 
//   753:     // Handle passing in a number to a CSS property

function prop$_lit$($dis, elem, value, type, i, name) {

// *** jquery.js ***
//   750:     if ( jQuery.isFunction( value ) )
//   751:       value = value.call( elem, i );

if ($v.cm(jQuery, 'isFunction', [ value ])) value = $v.cm(value, 'call', [ elem, i ]);

// *** jquery.js ***
//   754:     return value && value.constructor == Number && type == "curCSS" && !exclude.test( name ) ?
//   755:       value + "px" :
//   756:       value;

return value && $v.r(value, 'constructor') == $v.ro('Number') && type == 'curCSS' && !$v.cm(exclude, 'test', [ name ]) ? value + 'px': value;
}
___.func(prop$_lit$, 'prop$_lit$');
;
var prop$_lit = $v.dis(___.primFreeze(prop$_lit$), 'prop$_lit');
return prop$_lit;
}).CALL___(), 'className', ___.initializeMap([ 'add', ___.frozenFunc(function () {

// *** jquery.js ***
//   761:     add: function( elem, classNames ) {
//   762:       jQuery.each((classNames || "").split(/\s+/), function(i, className){
//   765:       });

function add$_lit$($dis, elem, classNames) {
$v.cm(jQuery, 'each', [ $v.cm(classNames || '', 'split', [ $v.construct(RegExp, [ '\\s+' ]) ]), $v.dis(___.frozenFunc(function ($dis, i, className) {

// *** jquery.js ***
//   763:         if ( elem.nodeType == 1 && !jQuery.className.has( elem.className, className ) )
//   764:           elem.className += (elem.className ? " " : "") + className;

if ($v.r(elem, 'nodeType') == 1 && !$v.cm($v.r(jQuery, 'className'), 'has', [ $v.r(elem, 'className'), className ])) $v.s(elem, 'className', $v.r(elem, 'className') + (($v.r(elem, 'className') ? ' ': '') + className));
})) ]);
}
___.func(add$_lit$, 'add$_lit$');
;
var add$_lit = $v.dis(___.primFreeze(add$_lit$), 'add$_lit');
return add$_lit;
}).CALL___(), 'remove', ___.frozenFunc(function () {

// *** jquery.js ***
//   769:     remove: function( elem, classNames ) {
//   774:           }).join(" ") :
//   775:           "";

function remove$_lit$($dis, elem, classNames) {

// *** jquery.js ***
//   770:       if (elem.nodeType == 1)
//   771:         elem.className = classNames != undefined ?
//   772:           jQuery.grep(elem.className.split(/\s+/), function(className){

if ($v.r(elem, 'nodeType') == 1) $v.s(elem, 'className', classNames != $v.ro('undefined') ? $v.cm($v.cm(jQuery, 'grep', [ $v.cm($v.r(elem, 'className'), 'split', [ $v.construct(RegExp, [ '\\s+' ]) ]), $v.dis(___.frozenFunc(function ($dis, className) {

// *** jquery.js ***
//   773:             return !jQuery.className.has( classNames, className );

return !$v.cm($v.r(jQuery, 'className'), 'has', [ classNames, className ]);
})) ]), 'join', [ ' ' ]): '');
}
___.func(remove$_lit$, 'remove$_lit$');
;
var remove$_lit = $v.dis(___.primFreeze(remove$_lit$), 'remove$_lit');
return remove$_lit;
}).CALL___(), 'has', ___.frozenFunc(function () {

// *** jquery.js ***
//   779:     has: function( elem, className ) {

function has$_lit$($dis, elem, className) {

// *** jquery.js ***
//   780:       return jQuery.inArray( className, (elem.className || elem).toString().split(/\s+/) ) > -1;

return $v.cm(jQuery, 'inArray', [ className, $v.cm($v.cm($v.r(elem, 'className') || elem, 'toString', [ ]), 'split', [ $v.construct(RegExp, [ '\\s+' ]) ]) ]) > -1;
}
___.func(has$_lit$, 'has$_lit$');
;
var has$_lit = $v.dis(___.primFreeze(has$_lit$), 'has$_lit');
return has$_lit;
}).CALL___() ]), 'swap', ___.frozenFunc(function () {

// *** jquery.js ***
//   785:   swap: function( elem, options, callback ) {
//   787:     // Remember the old values, and insert the new ones
//   792: 
//   793:     callback.call( elem );
//   794: 
//   795:     // Revert the old values

function swap$_lit$($dis, elem, options, callback) {
var $caja$15;
var $caja$16;
var $caja$17;
var $caja$18;

// *** jquery.js ***
//   786:     var old = {};

var old = ___.initializeMap([ ]);
{
$caja$15 = $v.keys(options);

// *** jquery.js ***
//   789:       old[ name ] = elem.style[ name ];
//   790:       elem.style[ name ] = options[ name ];
//   791:     }

for ($caja$16 = 0; $caja$16 < ($caja$15.length_canRead___? $caja$15.length: ___.readPub($caja$15, 'length')); ++$caja$16) {

// *** jquery.js ***
//   788:     for ( var name in options ) {

var name = ___.readPub($caja$15, $caja$16);
{
$v.s(old, name, $v.r($v.r(elem, 'style'), name));
$v.s($v.r(elem, 'style'), name, $v.r(options, name));
}
}
}
$v.cm(callback, 'call', [ elem ]);
{
$caja$17 = $v.keys(options);

// *** jquery.js ***
//   797:       elem.style[ name ] = old[ name ];

for ($caja$18 = 0; $caja$18 < ($caja$17.length_canRead___? $caja$17.length: ___.readPub($caja$17, 'length')); ++$caja$18) {

// *** jquery.js ***
//   796:     for ( var name in options )

var name = ___.readPub($caja$17, $caja$18);
$v.s($v.r(elem, 'style'), name, $v.r(old, name));
}
}
}
___.func(swap$_lit$, 'swap$_lit$');
;
var swap$_lit = $v.dis(___.primFreeze(swap$_lit$), 'swap$_lit');
return swap$_lit;
}).CALL___(), 'css', ___.frozenFunc(function () {

// *** jquery.js ***
//   800:   css: function( elem, name, force ) {
//   803: 
//   812:       }
//   813: 
//   816:       else
//   817:         jQuery.swap( elem, props, getWH );
//   818: 
//   820:     }
//   821: 

function css$_lit$($dis, elem, name, force) {
var getWH$_caller;
var getWH;

// *** jquery.js ***
//   801:     if ( name == "width" || name == "height" ) {

if (name == 'width' || name == 'height') {
getWH$_caller = (function () {

// *** jquery.js ***
//   807:         jQuery.each( which, function() {
//   810:         });

function getWH$_caller$($dis) {

// *** jquery.js ***
//   805:         val = name == "width" ? elem.offsetWidth : elem.offsetHeight;

val = name == 'width'? $v.r(elem, 'offsetWidth'): $v.r(elem, 'offsetHeight');

// *** jquery.js ***
//   806:         var padding = 0, border = 0;

var padding = 0, border = 0;
$v.cm(jQuery, 'each', [ which, $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//   808:           padding += parseFloat(jQuery.curCSS( elem, "padding" + this, true)) || 0;

padding = padding + ($v.cf($v.ro('parseFloat'), [ $v.cm(jQuery, 'curCSS', [ elem, 'padding' + $dis, true ]) ]) || 0);

// *** jquery.js ***
//   809:           border += parseFloat(jQuery.curCSS( elem, "border" + this + "Width", true)) || 0;

border = border + ($v.cf($v.ro('parseFloat'), [ $v.cm(jQuery, 'curCSS', [ elem, 'border' + $dis + 'Width', true ]) ]) || 0);
})) ]);

// *** jquery.js ***
//   811:         val -= Math.round(padding + border);

val = val - $v.cm($v.ro('Math'), 'round', [ padding + border ]);
}
return ___.func(getWH$_caller$, 'getWH$_caller');
})();
;

// *** jquery.js ***
//   804:       function getWH() {

getWH = $v.dis(___.primFreeze(getWH$_caller), 'getWH');

// *** jquery.js ***
//   802:       var val, props = { position: "absolute", visibility: "hidden", display:"block" }, which = name == "width" ? [ "Left", "Right" ] : [ "Top", "Bottom" ];

var val, props = ___.initializeMap([ 'position', 'absolute', 'visibility', 'hidden', 'display', 'block' ]), which = name == 'width'? [ 'Left', 'Right' ]: [ 'Top', 'Bottom' ];
;

// *** jquery.js ***
//   814:       if ( jQuery(elem).is(":visible") )
//   815:         getWH();

if ($v.cm($v.cf(jQuery, [ elem ]), 'is', [ ':visible' ])) $v.cf(getWH, [ ]);
else $v.cm(jQuery, 'swap', [ elem, props, getWH ]);

// *** jquery.js ***
//   819:       return Math.max(0, val);

return $v.cm($v.ro('Math'), 'max', [ 0, val ]);
}

// *** jquery.js ***
//   822:     return jQuery.curCSS( elem, name, force );

return $v.cm(jQuery, 'curCSS', [ elem, name, force ]);
}
___.func(css$_lit$, 'css$_lit$');
;
var css$_lit = $v.dis(___.primFreeze(css$_lit$), 'css$_lit');
return css$_lit;
}).CALL___(), 'curCSS', ___.frozenFunc(function () {

// *** jquery.js ***
//   825:   curCSS: function( elem, name, force ) {
//   827: 
//   828:     // A helper method for determining if an element's values are broken
//   836:     }
//   837: 
//   838:     // We need to handle opacity special in IE
//   841: 
//   845:     }
//   846:     // Opera sometimes will give the wrong display answer, this fixes it, see #2037
//   849:       style.outline = "0 solid black";
//   850:       style.outline = save;
//   851:     }
//   852: 
//   853:     // Make sure we're using the right name for getting the float value
//   856: 
//   859: 
//   860:     else if ( defaultView.getComputedStyle ) {
//   861: 
//   862:       // Only "float" is needed here
//   865: 
//   867: 
//   869: 
//   872: 
//   873:       // If the element isn't reporting its values properly in Safari
//   874:       // then some display: none elements are involved
//   875:       else {
//   877: 
//   878:         // Locate all of the parent display: none elements
//   881: 
//   882:         // Go through and make them visible, but in reverse
//   883:         // (It would be better if we knew the exact display type that they had)
//   889: 
//   890:         // Since we flip the display style, we have to handle that
//   891:         // one special, otherwise get the value
//   893:           "none" :
//   894:           ( computedStyle && computedStyle.getPropertyValue( name ) ) || "";
//   895: 
//   896:         // Finally, revert the display styles back
//   900:       }
//   901: 
//   902:       // We should always get a number back from opacity
//   905: 
//   909:       });
//   910: 
//   912: 
//   913:       // From the awesome hack by Dean Edwards
//   914:       // http://erik.eae.net/archives/2007/07/27/18.54.15/#comment-102291
//   915: 
//   916:       // If we're not dealing with a regular pixel number
//   917:       // but a number that has a weird ending, we need to convert it to pixels
//   919:         // Remember the original values
//   921: 
//   922:         // Put in the new values to get a computed value out
//   923:         elem.runtimeStyle.left = elem.currentStyle.left;
//   924:         style.left = ret || 0;
//   926: 
//   927:         // Revert the changed values
//   928:         style.left = left;
//   929:         elem.runtimeStyle.left = rsLeft;
//   930:       }
//   931:     }
//   932: 

function curCSS$_lit$($dis, elem, name, force) {

// *** jquery.js ***
//   832: 
//   833:       // defaultView is cached

function color$_caller($dis, elem) {

// *** jquery.js ***
//   830:       if ( !jQuery.browser.safari )
//   831:         return false;

if (!$v.r($v.r(jQuery, 'browser'), 'safari')) return false;

// *** jquery.js ***
//   834:       var ret = defaultView.getComputedStyle( elem, null );

var ret = $v.cm(defaultView, 'getComputedStyle', [ elem, null ]);

// *** jquery.js ***
//   835:       return !ret || ret.getPropertyValue("color") == "";

return !ret || $v.cm(ret, 'getPropertyValue', [ 'color' ]) == '';
}
___.func(color$_caller, 'color$_caller');
var color;
;

// *** jquery.js ***
//   829:     function color( elem ) {

color = $v.dis(___.primFreeze(color$_caller), 'color');

// *** jquery.js ***
//   826:     var ret, style = elem.style;

var ret, style = $v.r(elem, 'style');
;

// *** jquery.js ***
//   839:     if ( name == "opacity" && jQuery.browser.msie ) {

if (name == 'opacity' && $v.r($v.r(jQuery, 'browser'), 'msie')) {

// *** jquery.js ***
//   840:       ret = jQuery.attr( style, "opacity" );

ret = $v.cm(jQuery, 'attr', [ style, 'opacity' ]);

// *** jquery.js ***
//   842:       return ret == "" ?
//   843:         "1" :
//   844:         ret;

return ret == ''? '1': ret;
}

// *** jquery.js ***
//   847:     if ( jQuery.browser.opera && name == "display" ) {

if ($v.r($v.r(jQuery, 'browser'), 'opera') && name == 'display') {

// *** jquery.js ***
//   848:       var save = style.outline;

var save = $v.r(style, 'outline');
$v.s(style, 'outline', '0 solid black');
$v.s(style, 'outline', save);
}

// *** jquery.js ***
//   854:     if ( name.match( /float/i ) )
//   855:       name = styleFloat;

if ($v.cm(name, 'match', [ $v.construct(RegExp, [ 'float', 'i' ]) ])) name = styleFloat;

// *** jquery.js ***
//   857:     if ( !force && style && style[ name ] )
//   858:       ret = style[ name ];

if (!force && style && $v.r(style, name)) ret = $v.r(style, name);
else if ($v.r(defaultView, 'getComputedStyle')) {

// *** jquery.js ***
//   863:       if ( name.match( /float/i ) )
//   864:         name = "float";

if ($v.cm(name, 'match', [ $v.construct(RegExp, [ 'float', 'i' ]) ])) name = 'float';

// *** jquery.js ***
//   866:       name = name.replace( /([A-Z])/g, "-$1" ).toLowerCase();

name = $v.cm($v.cm(name, 'replace', [ $v.construct(RegExp, [ '([A-Z])', 'g' ]), '-$1' ]), 'toLowerCase', [ ]);

// *** jquery.js ***
//   868:       var computedStyle = defaultView.getComputedStyle( elem, null );

var computedStyle = $v.cm(defaultView, 'getComputedStyle', [ elem, null ]);

// *** jquery.js ***
//   870:       if ( computedStyle && !color( elem ) )
//   871:         ret = computedStyle.getPropertyValue( name );

if (computedStyle && !$v.cf(color, [ elem ])) ret = $v.cm(computedStyle, 'getPropertyValue', [ name ]);
else {

// *** jquery.js ***
//   876:         var swap = [], stack = [], a = elem, i = 0;

var swap = [ ], stack = [ ], a = elem, i = 0;

// *** jquery.js ***
//   879:         for ( ; a && color(a); a = a.parentNode )
//   880:           stack.unshift(a);

for (; a && $v.cf(color, [ a ]); a = $v.r(a, 'parentNode')) $v.cm(stack, 'unshift', [ a ]);

// *** jquery.js ***
//   884:         for ( ; i < stack.length; i++ )
//   885:           if ( color( stack[ i ] ) ) {
//   886:             swap[ i ] = stack[ i ].style.display;
//   887:             stack[ i ].style.display = "block";
//   888:           }

for (; i < $v.r(stack, 'length'); i++) if ($v.cf(color, [ $v.r(stack, i) ])) {
$v.s(swap, i, $v.r($v.r($v.r(stack, i), 'style'), 'display'));
$v.s($v.r($v.r(stack, i), 'style'), 'display', 'block');
}

// *** jquery.js ***
//   892:         ret = name == "display" && swap[ stack.length - 1 ] != null ?

ret = name == 'display' && $v.r(swap, $v.r(stack, 'length') - 1) != null ? 'none': computedStyle && $v.cm(computedStyle, 'getPropertyValue', [ name ]) || '';

// *** jquery.js ***
//   897:         for ( i = 0; i < swap.length; i++ )
//   898:           if ( swap[ i ] != null )
//   899:             stack[ i ].style.display = swap[ i ];

for (i = 0; i < $v.r(swap, 'length'); i++) if ($v.r(swap, i) != null) $v.s($v.r($v.r(stack, i), 'style'), 'display', $v.r(swap, i));
}

// *** jquery.js ***
//   903:       if ( name == "opacity" && ret == "" )
//   904:         ret = "1";

if (name == 'opacity' && ret == '') ret = '1';

// *** jquery.js ***
//   906:     } else if ( elem.currentStyle ) {

} else if ($v.r(elem, 'currentStyle')) {

// *** jquery.js ***
//   907:       var camelCase = name.replace(/\-(\w)/g, function(all, letter){

var camelCase = $v.cm(name, 'replace', [ $v.construct(RegExp, [ '\\-(\\w)', 'g' ]), $v.dis(___.frozenFunc(function ($dis, all, letter) {

// *** jquery.js ***
//   908:         return letter.toUpperCase();

return $v.cm(letter, 'toUpperCase', [ ]);
})) ]);

// *** jquery.js ***
//   911:       ret = elem.currentStyle[ name ] || elem.currentStyle[ camelCase ];

ret = $v.r($v.r(elem, 'currentStyle'), name) || $v.r($v.r(elem, 'currentStyle'), camelCase);

// *** jquery.js ***
//   918:       if ( !/^\d+(px)?$/i.test( ret ) && /^\d/.test( ret ) ) {

if (!$v.cm($v.construct(RegExp, [ '^\\d+(px)?$', 'i' ]), 'test', [ ret ]) && $v.cm($v.construct(RegExp, [ '^\\d' ]), 'test', [ ret ])) {

// *** jquery.js ***
//   920:         var left = style.left, rsLeft = elem.runtimeStyle.left;

var left = $v.r(style, 'left'), rsLeft = $v.r($v.r(elem, 'runtimeStyle'), 'left');
$v.s($v.r(elem, 'runtimeStyle'), 'left', $v.r($v.r(elem, 'currentStyle'), 'left'));
$v.s(style, 'left', ret || 0);

// *** jquery.js ***
//   925:         ret = style.pixelLeft + "px";

ret = $v.r(style, 'pixelLeft') + 'px';
$v.s(style, 'left', left);
$v.s($v.r(elem, 'runtimeStyle'), 'left', rsLeft);
}
}

// *** jquery.js ***
//   933:     return ret;

return ret;
}
___.func(curCSS$_lit$, 'curCSS$_lit$');
;
var curCSS$_lit = $v.dis(___.primFreeze(curCSS$_lit$), 'curCSS$_lit');
return curCSS$_lit;
}).CALL___(), 'clean', ___.frozenFunc(function () {

// *** jquery.js ***
//   936:   clean: function( elems, context ) {
//   939:     // !context.createElement fails in IE with an error but returns typeof 'object'
//   942: 
//   943:     jQuery.each(elems, function(i, elem){
//   946: 
//   949: 
//   950:       // Convert html string into DOM nodes
//  1020: 
//  1023: 
//  1029: 
//  1030:     });
//  1031: 

function clean$_lit$($dis, elems, context) {

// *** jquery.js ***
//   937:     var ret = [];

var ret = [ ];

// *** jquery.js ***
//   938:     context = context || document;

context = context || $v.ro('document');

// *** jquery.js ***
//   940:     if (typeof context.createElement == 'undefined')
//   941:       context = context.ownerDocument || context[0] && context[0].ownerDocument || document;

if ($v.typeOf($v.r(context, 'createElement')) == 'undefined') context = $v.r(context, 'ownerDocument') || $v.r(context, 0) && $v.r($v.r(context, 0), 'ownerDocument') || $v.ro('document');
$v.cm(jQuery, 'each', [ elems, $v.dis(___.frozenFunc(function ($dis, i, elem) {
var x0___;

// *** jquery.js ***
//   944:       if ( !elem )
//   945:         return;

if (!elem) return;

// *** jquery.js ***
//   947:       if ( elem.constructor == Number )
//   948:         elem += '';

if ($v.r(elem, 'constructor') == $v.ro('Number')) elem = elem + '';

// *** jquery.js ***
//   951:       if ( typeof elem == "string" ) {
//   952:         // Fix "XHTML"-style tags in all browsers
//   957:         });
//   958: 
//   959:         // Trim whitespace, otherwise indexOf won't work as expected
//   961: 
//   963:           // option or optgroup
//   966: 
//   969: 
//   972: 
//   975: 
//   976:            // <thead> matched above
//   979: 
//   982: 
//   983:           // IE can't serialize <link> and <script> tags normally
//   986: 
//   988: 
//   989:         // Go to html and back, then peel off extra wrappers
//   990:         div.innerHTML = wrap[1] + elem + wrap[2];
//   991: 
//   992:         // Move to the right depth
//   995: 
//   996:         // Remove IE's autoinserted <tbody> from table fragments
//  1017: 
//  1019:       }

if ($v.typeOf(elem) == 'string') {

// *** jquery.js ***
//   953:         elem = elem.replace(/(<(\w+)[^>]*?)\/>/g, function(all, front, tag){

elem = $v.cm(elem, 'replace', [ $v.construct(RegExp, [ '(\x3c(\\w+)[^\x3e]*?)\\/\x3e', 'g' ]), $v.dis(___.frozenFunc(function ($dis, all, front, tag) {

// *** jquery.js ***
//   954:           return tag.match(/^(abbr|br|col|img|input|link|meta|param|hr|area|embed)$/i) ?
//   955:             all :
//   956:             front + "><�" + tag + ">";

return $v.cm(tag, 'match', [ $v.construct(RegExp, [ '^(abbr|br|col|img|input|link|meta|param|hr|area|embed)$', 'i' ]) ]) ? all: front + '\x3e\x3c/' + tag + '\x3e';
})) ]);

// *** jquery.js ***
//   960:         var tags = jQuery.trim( elem ).toLowerCase(), div = context.createElement("div");

var tags = $v.cm($v.cm(jQuery, 'trim', [ elem ]), 'toLowerCase', [ ]), div = $v.cm(context, 'createElement', [ 'div' ]);

// *** jquery.js ***
//   962:         var wrap =
//   964:           !tags.indexOf("<opt") &&
//   965:           [ 1, "<select multiple='multiple'>", "<�select>" ] ||
//   967:           !tags.indexOf("<leg") &&
//   968:           [ 1, "<fieldset>", "<�fieldset>" ] ||
//   970:           tags.match(/^<(thead|tbody|tfoot|colg|cap)/) &&
//   971:           [ 1, "<table>", "<�table>" ] ||
//   973:           !tags.indexOf("<tr") &&
//   974:           [ 2, "<table><tbody>", "<�tbody><�table>" ] ||
//   977:           (!tags.indexOf("<td") || !tags.indexOf("<th")) &&
//   978:           [ 3, "<table><tbody><tr>", "<�tr><�tbody><�table>" ] ||
//   980:           !tags.indexOf("<col") &&
//   981:           [ 2, "<table><tbody><�tbody><colgroup>", "<�colgroup><�table>" ] ||
//   984:           jQuery.browser.msie &&
//   985:           [ 1, "div<div>", "<�div>" ] ||
//   987:           [ 0, "", "" ];

var wrap = !$v.cm(tags, 'indexOf', [ '\x3copt' ]) && [ 1, '\x3cselect multiple=\'multiple\'\x3e', '\x3c/select\x3e' ] || !$v.cm(tags, 'indexOf', [ '\x3cleg' ]) && [ 1, '\x3cfieldset\x3e', '\x3c/fieldset\x3e' ] || $v.cm(tags, 'match', [ $v.construct(RegExp, [ '^\x3c(thead|tbody|tfoot|colg|cap)' ]) ]) && [ 1, '\x3ctable\x3e', '\x3c/table\x3e' ] || !$v.cm(tags, 'indexOf', [ '\x3ctr' ]) && [ 2, '\x3ctable\x3e\x3ctbody\x3e', '\x3c/tbody\x3e\x3c/table\x3e' ] || (!$v.cm(tags, 'indexOf', [ '\x3ctd' ]) || !$v.cm(tags, 'indexOf', [ '\x3cth' ])) && [ 3, '\x3ctable\x3e\x3ctbody\x3e\x3ctr\x3e', '\x3c/tr\x3e\x3c/tbody\x3e\x3c/table\x3e' ] || !$v.cm(tags, 'indexOf', [ '\x3ccol' ]) && [ 2, '\x3ctable\x3e\x3ctbody\x3e\x3c/tbody\x3e\x3ccolgroup\x3e', '\x3c/colgroup\x3e\x3c/table\x3e' ] || $v.r($v.r(jQuery, 'browser'), 'msie') && [ 1, 'div\x3cdiv\x3e', '\x3c/div\x3e' ] || [ 0, '', '' ];
$v.s(div, 'innerHTML', $v.r(wrap, 1) + elem + $v.r(wrap, 2));

// *** jquery.js ***
//   993:         while ( wrap[0]-- )
//   994:           div = div.lastChild;

while (void 0, x0___ = +$v.r(wrap, 0), $v.s(wrap, 0, x0___ - 1), x0___) div = $v.r(div, 'lastChild');

// *** jquery.js ***
//   997:         if ( jQuery.browser.msie ) {
//   998: 
//   999:           // String was a <table>, *may* have spurious <tbody>
//  1002: 
//  1003:             // String was a bare <thead> or <tfoot>
//  1007: 
//  1011: 
//  1012:           // IE completely kills leading whitespace when innerHTML is used
//  1015: 
//  1016:         }

if ($v.r($v.r(jQuery, 'browser'), 'msie')) {

// *** jquery.js ***
//  1000:           var tbody = !tags.indexOf("<table") && tags.indexOf("<tbody") < 0 ?
//  1001:             div.firstChild && div.firstChild.childNodes :
//  1004:             wrap[1] == "<table>" && tags.indexOf("<tbody") < 0 ?
//  1005:               div.childNodes :
//  1006:               [];

var tbody = !$v.cm(tags, 'indexOf', [ '\x3ctable' ]) && $v.cm(tags, 'indexOf', [ '\x3ctbody' ]) < 0? $v.r(div, 'firstChild') && $v.r($v.r(div, 'firstChild'), 'childNodes'): $v.r(wrap, 1) == '\x3ctable\x3e' && $v.cm(tags, 'indexOf', [ '\x3ctbody' ]) < 0? $v.r(div, 'childNodes'): [ ];

// *** jquery.js ***
//  1008:           for ( var j = tbody.length - 1; j >= 0 ; --j )
//  1009:             if ( jQuery.nodeName( tbody[ j ], "tbody" ) && !tbody[ j ].childNodes.length )
//  1010:               tbody[ j ].parentNode.removeChild( tbody[ j ] );

for (var j = $v.r(tbody, 'length') - 1; j >= 0; --j) if ($v.cm(jQuery, 'nodeName', [ $v.r(tbody, j), 'tbody' ]) && !$v.r($v.r($v.r(tbody, j), 'childNodes'), 'length')) $v.cm($v.r($v.r(tbody, j), 'parentNode'), 'removeChild', [ $v.r(tbody, j) ]);

// *** jquery.js ***
//  1013:           if ( /^\s/.test( elem ) )
//  1014:             div.insertBefore( context.createTextNode( elem.match(/^\s�/)[0] ), div.firstChild );

if ($v.cm($v.construct(RegExp, [ '^\\s' ]), 'test', [ elem ])) $v.cm(div, 'insertBefore', [ $v.cm(context, 'createTextNode', [ $v.r($v.cm(elem, 'match', [ $v.construct(RegExp, [ '^\\s*' ]) ]), 0) ]), $v.r(div, 'firstChild') ]);
}

// *** jquery.js ***
//  1018:         elem = jQuery.makeArray( div.childNodes );

elem = $v.cm(jQuery, 'makeArray', [ $v.r(div, 'childNodes') ]);
}

// *** jquery.js ***
//  1021:       if ( elem.length === 0 && (!jQuery.nodeName( elem, "form" ) && !jQuery.nodeName( elem, "select" )) )
//  1022:         return;

if ($v.r(elem, 'length') === 0 && (!$v.cm(jQuery, 'nodeName', [ elem, 'form' ]) && !$v.cm(jQuery, 'nodeName', [ elem, 'select' ]))) return;

// *** jquery.js ***
//  1024:       if ( elem[0] == undefined || jQuery.nodeName( elem, "form" ) || elem.options )
//  1025:         ret.push( elem );
//  1026: 
//  1027:       else
//  1028:         ret = jQuery.merge( ret, elem );

if ($v.r(elem, 0) == $v.ro('undefined') || $v.cm(jQuery, 'nodeName', [ elem, 'form' ]) || $v.r(elem, 'options')) $v.cm(ret, 'push', [ elem ]);
else ret = $v.cm(jQuery, 'merge', [ ret, elem ]);
})) ]);

// *** jquery.js ***
//  1032:     return ret;

return ret;
}
___.func(clean$_lit$, 'clean$_lit$');
;
var clean$_lit = $v.dis(___.primFreeze(clean$_lit$), 'clean$_lit');
return clean$_lit;
}).CALL___(), 'attr', ___.frozenFunc(function () {

// *** jquery.js ***
//  1035:   attr: function( elem, name, value ) {
//  1036:     // don't set attributes on text and comment nodes
//  1039: 
//  1041:       // Whether we are setting (or getting)
//  1044: 
//  1045:     // Try to normalize/fix the name
//  1047: 
//  1048:     // Only do all the following if this is a node (faster for style)
//  1049:     // IE elem.getAttribute passes even for style
//  1051: 
//  1052:       // These attributes require special treatment
//  1054: 
//  1055:       // Safari mis-reports the default selected property of a hidden option
//  1056:       // Accessing the parent's selectedIndex property fixes it
//  1059: 
//  1060:       // If applicable, access the attribute via the DOM 0 way
//  1078: 
//  1081: 
//  1083:         // convert the value to a string (all browsers do this but IE) see #1070
//  1085: 
//  1087:           // Some attributes require a special call on IE
//  1090: 
//  1091:       // Non-existent attributes return null, we normalize to undefined
//  1093:     }
//  1094: 
//  1095:     // elem is actually elem.style ... set the style
//  1096: 
//  1097:     // IE uses filters for opacity
//  1105:         elem.filter = (elem.filter || "").replace( /alpha\([^)]*\)/, "" ) +
//  1106:           (parseInt( value ) + '' == "NaN" ? "" : "alpha(opacity=" + value * 100 + ")");
//  1107:       }
//  1108: 
//  1112:     }
//  1113: 
//  1116:     });
//  1117: 
//  1120: 

function attr$_lit$($dis, elem, name, value) {

// *** jquery.js ***
//  1037:     if (!elem || elem.nodeType == 3 || elem.nodeType == 8)
//  1038:       return undefined;

if (!elem || $v.r(elem, 'nodeType') == 3 || $v.r(elem, 'nodeType') == 8) return $v.ro('undefined');

// *** jquery.js ***
//  1040:     var notxml = !jQuery.isXMLDoc( elem ),
//  1042:       set = value !== undefined,
//  1043:       msie = jQuery.browser.msie;

var notxml = !$v.cm(jQuery, 'isXMLDoc', [ elem ]), set = value !== $v.ro('undefined'), msie = $v.r($v.r(jQuery, 'browser'), 'msie');

// *** jquery.js ***
//  1046:     name = notxml && jQuery.props[ name ] || name;

name = notxml && $v.r($v.r(jQuery, 'props'), name) || name;

// *** jquery.js ***
//  1050:     if ( elem.tagName ) {

if ($v.r(elem, 'tagName')) {

// *** jquery.js ***
//  1053:       var special = /href|src|style/.test( name );

var special = $v.cm($v.construct(RegExp, [ 'href|src|style' ]), 'test', [ name ]);

// *** jquery.js ***
//  1057:       if ( name == "selected" && jQuery.browser.safari )
//  1058:         elem.parentNode.selectedIndex;

if (name == 'selected' && $v.r($v.r(jQuery, 'browser'), 'safari')) $v.r($v.r(elem, 'parentNode'), 'selectedIndex');

// *** jquery.js ***
//  1061:       if ( name in elem && notxml && !special ) {
//  1069: 
//  1070:         // browsers index elements by id/name on forms, give priority to attributes.
//  1075: 
//  1077:       }

if ($v.canReadRev(name, elem) && notxml && !special) {

// *** jquery.js ***
//  1062:         if ( set ){
//  1063:           // We can't allow the type property to be changed (since it causes problems in IE)
//  1066: 
//  1067:           elem[ name ] = value;
//  1068:         }

if (set) {

// *** jquery.js ***
//  1064:           if ( name == "type" && jQuery.nodeName( elem, "input" ) && elem.parentNode )
//  1065:             throw "type property can't be changed";

if (name == 'type' && $v.cm(jQuery, 'nodeName', [ elem, 'input' ]) && $v.r(elem, 'parentNode')) throw 'type property can\'t be changed';
$v.s(elem, name, value);
}

// *** jquery.js ***
//  1071:         if( jQuery.nodeName( elem, "form" ) && 
//  1072:             elem.getAttributeNode && 
//  1073:             elem.getAttributeNode(name) )
//  1074:           return elem.getAttributeNode( name ).nodeValue;

if ($v.cm(jQuery, 'nodeName', [ elem, 'form' ]) && $v.r(elem, 'getAttributeNode') && $v.cm(elem, 'getAttributeNode', [ name ])) return $v.r($v.cm(elem, 'getAttributeNode', [ name ]), 'nodeValue');

// *** jquery.js ***
//  1076:         return elem[ name ];

return $v.r(elem, name);
}

// *** jquery.js ***
//  1079:       if ( msie && notxml &&  name == "style" )
//  1080:         return jQuery.attr( elem.style, "cssText", value );

if (msie && notxml && name == 'style') return $v.cm(jQuery, 'attr', [ $v.r(elem, 'style'), 'cssText', value ]);

// *** jquery.js ***
//  1082:       if ( set )
//  1084:         elem.setAttribute( name, "" + value );

if (set) $v.cm(elem, 'setAttribute', [ name, '' + value ]);

// *** jquery.js ***
//  1086:       var attr = msie && notxml && special
//  1088:           ? elem.getAttribute( name, 2 )
//  1089:           : elem.getAttribute( name );

var attr = msie && notxml && special? $v.cm(elem, 'getAttribute', [ name, 2 ]): $v.cm(elem, 'getAttribute', [ name ]);

// *** jquery.js ***
//  1092:       return attr === null ? undefined : attr;

return attr === null ? $v.ro('undefined'): attr;
}

// *** jquery.js ***
//  1098:     if ( msie && name == "opacity" ) {

if (msie && name == 'opacity') {

// *** jquery.js ***
//  1099:       if ( set ) {
//  1100:         // IE has trouble with opacity if it does not have layout
//  1101:         // Force it by setting the zoom level
//  1102:         elem.zoom = 1;
//  1103: 
//  1104:         // Set the alpha filter to set the opacity

if (set) {
$v.s(elem, 'zoom', 1);
$v.s(elem, 'filter', $v.cm($v.r(elem, 'filter') || '', 'replace', [ $v.construct(RegExp, [ 'alpha\\([^)]*\\)' ]), '' ]) + ($v.cf($v.ro('parseInt'), [ value ]) + '' == 'NaN'? '': 'alpha(opacity=' + value * 100 + ')'));
}

// *** jquery.js ***
//  1109:       return elem.filter && elem.filter.indexOf("opacity=") >= 0 ?
//  1110:         (parseFloat( elem.filter.match(/opacity=([^)]*)/)[1] ) / 100) + '':
//  1111:         "";

return $v.r(elem, 'filter') && $v.cm($v.r(elem, 'filter'), 'indexOf', [ 'opacity=' ]) >= 0? $v.cf($v.ro('parseFloat'), [ $v.r($v.cm($v.r(elem, 'filter'), 'match', [ $v.construct(RegExp, [ 'opacity=([^)]*)' ]) ]), 1) ]) / 100 + '': '';
}

// *** jquery.js ***
//  1114:     name = name.replace(/-([a-z])/ig, function(all, letter){

name = $v.cm(name, 'replace', [ $v.construct(RegExp, [ '-([a-z])', 'ig' ]), $v.dis(___.frozenFunc(function ($dis, all, letter) {

// *** jquery.js ***
//  1115:       return letter.toUpperCase();

return $v.cm(letter, 'toUpperCase', [ ]);
})) ]);

// *** jquery.js ***
//  1118:     if ( set )
//  1119:       elem[ name ] = value;

if (set) $v.s(elem, name, value);

// *** jquery.js ***
//  1121:     return elem[ name ];

return $v.r(elem, name);
}
___.func(attr$_lit$, 'attr$_lit$');
;
var attr$_lit = $v.dis(___.primFreeze(attr$_lit$), 'attr$_lit');
return attr$_lit;
}).CALL___(), 'trim', ___.frozenFunc(function () {

// *** jquery.js ***
//  1124:   trim: function( text ) {

function trim$_lit$($dis, text) {

// *** jquery.js ***
//  1125:     return (text || "").replace( /^\s+|\s+$/g, "" );

return $v.cm(text || '', 'replace', [ $v.construct(RegExp, [ '^\\s+|\\s+$', 'g' ]), '' ]);
}
___.func(trim$_lit$, 'trim$_lit$');
;
var trim$_lit = $v.dis(___.primFreeze(trim$_lit$), 'trim$_lit');
return trim$_lit;
}).CALL___(), 'makeArray', ___.frozenFunc(function () {

// *** jquery.js ***
//  1128:   makeArray: function( array ) {
//  1130: 
//  1133:       //the window, strings and functions also have 'length'
//  1136:       else
//  1137:         while( i )
//  1138:           ret[--i] = array[i];
//  1139:     }
//  1140: 

function makeArray$_lit$($dis, array) {

// *** jquery.js ***
//  1129:     var ret = [];

var ret = [ ];

// *** jquery.js ***
//  1131:     if( array != null ){

if (array != null) {

// *** jquery.js ***
//  1132:       var i = array.length;

var i = $v.r(array, 'length');

// *** jquery.js ***
//  1134:       if( i == null || array.split || array.setInterval || array.call )
//  1135:         ret[0] = array;

if (i == null || $v.r(array, 'split') || $v.r(array, 'setInterval') || $v.r(array, 'call')) $v.s(ret, 0, array);
else while (i) $v.s(ret, --i, $v.r(array, i));
}

// *** jquery.js ***
//  1141:     return ret;

return ret;
}
___.func(makeArray$_lit$, 'makeArray$_lit$');
;
var makeArray$_lit = $v.dis(___.primFreeze(makeArray$_lit$), 'makeArray$_lit');
return makeArray$_lit;
}).CALL___(), 'inArray', ___.frozenFunc(function () {

// *** jquery.js ***
//  1144:   inArray: function( elem, array ) {
//  1146:     // Use === because on IE, window == document
//  1149: 

function inArray$_lit$($dis, elem, array) {

// *** jquery.js ***
//  1145:     for ( var i = 0, length = array.length; i < length; i++ )
//  1147:       if ( array[ i ] === elem )
//  1148:         return i;

for (var i = 0, length = $v.r(array, 'length'); i < length; i++) if ($v.r(array, i) === elem) return i;

// *** jquery.js ***
//  1150:     return -1;

return -1;
}
___.func(inArray$_lit$, 'inArray$_lit$');
;
var inArray$_lit = $v.dis(___.primFreeze(inArray$_lit$), 'inArray$_lit');
return inArray$_lit;
}).CALL___(), 'merge', ___.frozenFunc(function () {

// *** jquery.js ***
//  1153:   merge: function( first, second ) {
//  1154:     // We have to loop this way because IE & Opera overwrite the length
//  1155:     // expando of getElementsByTagName
//  1157:     // Also, we need to make sure that the correct elements are being returned
//  1158:     // (IE returns comment nodes in a '*' query)
//  1163: 
//  1164:     } else
//  1165:       while ( elem = second[ i++ ] )
//  1166:         first[ pos++ ] = elem;
//  1167: 

function merge$_lit$($dis, first, second) {

// *** jquery.js ***
//  1156:     var i = 0, elem, pos = first.length;

var i = 0, elem, pos = $v.r(first, 'length');

// *** jquery.js ***
//  1159:     if ( jQuery.browser.msie ) {

if ($v.r($v.r(jQuery, 'browser'), 'msie')) {

// *** jquery.js ***
//  1160:       while ( elem = second[ i++ ] )
//  1161:         if ( elem.nodeType != 8 )
//  1162:           first[ pos++ ] = elem;

while (elem = $v.r(second, i++)) if ($v.r(elem, 'nodeType') != 8) $v.s(first, pos++, elem);
} else while (elem = $v.r(second, i++)) $v.s(first, pos++, elem);

// *** jquery.js ***
//  1168:     return first;

return first;
}
___.func(merge$_lit$, 'merge$_lit$');
;
var merge$_lit = $v.dis(___.primFreeze(merge$_lit$), 'merge$_lit');
return merge$_lit;
}).CALL___(), 'unique', ___.frozenFunc(function () {

// *** jquery.js ***
//  1171:   unique: function( array ) {
//  1173: 
//  1174:     try {
//  1175: 
//  1184: 
//  1185:     } catch( e ) {
//  1187:     }
//  1188: 

function unique$_lit$($dis, array) {

// *** jquery.js ***
//  1172:     var ret = [], done = {};

var ret = [ ], done = ___.initializeMap([ ]);
try {

// *** jquery.js ***
//  1176:       for ( var i = 0, length = array.length; i < length; i++ ) {
//  1178: 
//  1183:       }

for (var i = 0, length = $v.r(array, 'length'); i < length; i++) {

// *** jquery.js ***
//  1177:         var id = jQuery.data( array[ i ] );

var id = $v.cm(jQuery, 'data', [ $v.r(array, i) ]);

// *** jquery.js ***
//  1179:         if ( !done[ id ] ) {
//  1180:           done[ id ] = true;
//  1181:           ret.push( array[ i ] );
//  1182:         }

if (!$v.r(done, id)) {
$v.s(done, id, true);
$v.cm(ret, 'push', [ $v.r(array, i) ]);
}
}
} catch (ex___) {
try {
throw ___.tameException(ex___);
} catch (e) {

// *** jquery.js ***
//  1186:       ret = array;

ret = array;
}
}

// *** jquery.js ***
//  1189:     return ret;

return ret;
}
___.func(unique$_lit$, 'unique$_lit$');
;
var unique$_lit = $v.dis(___.primFreeze(unique$_lit$), 'unique$_lit');
return unique$_lit;
}).CALL___(), 'grep', ___.frozenFunc(function () {

// *** jquery.js ***
//  1192:   grep: function( elems, callback, inv ) {
//  1194: 
//  1195:     // Go through the array, only saving the items
//  1196:     // that pass the validator function
//  1200: 

function grep$_lit$($dis, elems, callback, inv) {

// *** jquery.js ***
//  1193:     var ret = [];

var ret = [ ];

// *** jquery.js ***
//  1197:     for ( var i = 0, length = elems.length; i < length; i++ )
//  1198:       if ( !inv != !callback( elems[ i ], i ) )
//  1199:         ret.push( elems[ i ] );

for (var i = 0, length = $v.r(elems, 'length'); i < length; i++) if (!inv != !$v.cf(callback, [ $v.r(elems, i), i ])) $v.cm(ret, 'push', [ $v.r(elems, i) ]);

// *** jquery.js ***
//  1201:     return ret;

return ret;
}
___.func(grep$_lit$, 'grep$_lit$');
;
var grep$_lit = $v.dis(___.primFreeze(grep$_lit$), 'grep$_lit');
return grep$_lit;
}).CALL___(), 'map', ___.frozenFunc(function () {

// *** jquery.js ***
//  1204:   map: function( elems, callback ) {
//  1206: 
//  1207:     // Go through the array, translating each of the items to their
//  1208:     // new value (or values).
//  1211: 
//  1214:     }
//  1215: 

function map$_lit$($dis, elems, callback) {

// *** jquery.js ***
//  1205:     var ret = [];

var ret = [ ];

// *** jquery.js ***
//  1209:     for ( var i = 0, length = elems.length; i < length; i++ ) {

for (var i = 0, length = $v.r(elems, 'length'); i < length; i++) {

// *** jquery.js ***
//  1210:       var value = callback( elems[ i ], i );

var value = $v.cf(callback, [ $v.r(elems, i), i ]);

// *** jquery.js ***
//  1212:       if ( value != null )
//  1213:         ret[ ret.length ] = value;

if (value != null) $v.s(ret, $v.r(ret, 'length'), value);
}

// *** jquery.js ***
//  1216:     return ret.concat.apply( [], ret );

return $v.cm($v.r(ret, 'concat'), 'apply', [ [ ], ret ]);
}
___.func(map$_lit$, 'map$_lit$');
;
var map$_lit = $v.dis(___.primFreeze(map$_lit$), 'map$_lit');
return map$_lit;
}).CALL___() ]) ]);

// *** jquery.js ***
//  1220: var userAgent = navigator.userAgent.toLowerCase();

var userAgent = $v.cm($v.r($v.ro('navigator'), 'userAgent'), 'toLowerCase', [ ]);

// *** jquery.js ***
//  1224:   version: (userAgent.match( /.+(?:rv|it|ra|ie)[\/: ]([\d.]+)/ ) || [])[1],
//  1225:   safari: /webkit/.test( userAgent ),
//  1226:   opera: /opera/.test( userAgent ),
//  1227:   msie: /msie/.test( userAgent ) && !/opera/.test( userAgent ),
//  1228:   mozilla: /mozilla/.test( userAgent ) && !/(compatible|webkit)/.test( userAgent )

$v.s(jQuery, 'browser', ___.initializeMap([ 'version', $v.r($v.cm(userAgent, 'match', [ $v.construct(RegExp, [ '.+(?:rv|it|ra|ie)[\\/: ]([\\d.]+)' ]) ]) || [ ], 1), 'safari', $v.cm($v.construct(RegExp, [ 'webkit' ]), 'test', [ userAgent ]), 'opera', $v.cm($v.construct(RegExp, [ 'opera' ]), 'test', [ userAgent ]), 'msie', $v.cm($v.construct(RegExp, [ 'msie' ]), 'test', [ userAgent ]) && !$v.cm($v.construct(RegExp, [ 'opera' ]), 'test', [ userAgent ]), 'mozilla', $v.cm($v.construct(RegExp, [ 'mozilla' ]), 'test', [ userAgent ]) && !$v.cm($v.construct(RegExp, [ '(compatible|webkit)' ]), 'test', [ userAgent ]) ]));

// *** jquery.js ***
//  1231: var styleFloat = jQuery.browser.msie ?
//  1232:   "styleFloat" :
//  1233:   "cssFloat";

var styleFloat = $v.r($v.r(jQuery, 'browser'), 'msie') ? 'styleFloat': 'cssFloat';

// *** jquery.js ***
//  1237:   boxModel: !jQuery.browser.msie || document.compatMode == "CSS1Compat",
//  1241:     "class": "className",
//  1242:     "float": styleFloat,
//  1243:     cssFloat: styleFloat,
//  1244:     styleFloat: styleFloat,
//  1245:     readonly: "readOnly",
//  1246:     maxlength: "maxLength",
//  1247:     cellspacing: "cellSpacing"

$v.cm(jQuery, 'extend', [ ___.initializeMap([ 'boxModel', !$v.r($v.r(jQuery, 'browser'), 'msie') || $v.r($v.ro('document'), 'compatMode') == 'CSS1Compat', 'props', ___.initializeMap([ 'for', 'htmlFor', 'class', 'className', 'float', styleFloat, 'cssFloat', styleFloat, 'styleFloat', styleFloat, 'readonly', 'readOnly', 'maxlength', 'maxLength', 'cellspacing', 'cellSpacing' ]) ]) ]);
$v.cm(jQuery, 'each', [ ___.initializeMap([ 'parent', ___.frozenFunc(function () {
function parent$_lit$($dis, elem) {

// *** jquery.js ***
//  1252:   parent: function(elem){return elem.parentNode;},

return $v.r(elem, 'parentNode');
}
___.func(parent$_lit$, 'parent$_lit$');
;
var parent$_lit = $v.dis(___.primFreeze(parent$_lit$), 'parent$_lit');
return parent$_lit;
}).CALL___(), 'parents', ___.frozenFunc(function () {
function parents$_lit$($dis, elem) {

// *** jquery.js ***
//  1253:   parents: function(elem){return jQuery.dir(elem,"parentNode");},

return $v.cm(jQuery, 'dir', [ elem, 'parentNode' ]);
}
___.func(parents$_lit$, 'parents$_lit$');
;
var parents$_lit = $v.dis(___.primFreeze(parents$_lit$), 'parents$_lit');
return parents$_lit;
}).CALL___(), 'next', ___.frozenFunc(function () {
function next$_lit$($dis, elem) {

// *** jquery.js ***
//  1254:   next: function(elem){return jQuery.nth(elem,2,"nextSibling");},

return $v.cm(jQuery, 'nth', [ elem, 2, 'nextSibling' ]);
}
___.func(next$_lit$, 'next$_lit$');
;
var next$_lit = $v.dis(___.primFreeze(next$_lit$), 'next$_lit');
return next$_lit;
}).CALL___(), 'prev', ___.frozenFunc(function () {
function prev$_lit$($dis, elem) {

// *** jquery.js ***
//  1255:   prev: function(elem){return jQuery.nth(elem,2,"previousSibling");},

return $v.cm(jQuery, 'nth', [ elem, 2, 'previousSibling' ]);
}
___.func(prev$_lit$, 'prev$_lit$');
;
var prev$_lit = $v.dis(___.primFreeze(prev$_lit$), 'prev$_lit');
return prev$_lit;
}).CALL___(), 'nextAll', ___.frozenFunc(function () {
function nextAll$_lit$($dis, elem) {

// *** jquery.js ***
//  1256:   nextAll: function(elem){return jQuery.dir(elem,"nextSibling");},

return $v.cm(jQuery, 'dir', [ elem, 'nextSibling' ]);
}
___.func(nextAll$_lit$, 'nextAll$_lit$');
;
var nextAll$_lit = $v.dis(___.primFreeze(nextAll$_lit$), 'nextAll$_lit');
return nextAll$_lit;
}).CALL___(), 'prevAll', ___.frozenFunc(function () {
function prevAll$_lit$($dis, elem) {

// *** jquery.js ***
//  1257:   prevAll: function(elem){return jQuery.dir(elem,"previousSibling");},

return $v.cm(jQuery, 'dir', [ elem, 'previousSibling' ]);
}
___.func(prevAll$_lit$, 'prevAll$_lit$');
;
var prevAll$_lit = $v.dis(___.primFreeze(prevAll$_lit$), 'prevAll$_lit');
return prevAll$_lit;
}).CALL___(), 'siblings', ___.frozenFunc(function () {
function siblings$_lit$($dis, elem) {

// *** jquery.js ***
//  1258:   siblings: function(elem){return jQuery.sibling(elem.parentNode.firstChild,elem);},

return $v.cm(jQuery, 'sibling', [ $v.r($v.r(elem, 'parentNode'), 'firstChild'), elem ]);
}
___.func(siblings$_lit$, 'siblings$_lit$');
;
var siblings$_lit = $v.dis(___.primFreeze(siblings$_lit$), 'siblings$_lit');
return siblings$_lit;
}).CALL___(), 'children', ___.frozenFunc(function () {
function children$_lit$($dis, elem) {

// *** jquery.js ***
//  1259:   children: function(elem){return jQuery.sibling(elem.firstChild);},

return $v.cm(jQuery, 'sibling', [ $v.r(elem, 'firstChild') ]);
}
___.func(children$_lit$, 'children$_lit$');
;
var children$_lit = $v.dis(___.primFreeze(children$_lit$), 'children$_lit');
return children$_lit;
}).CALL___(), 'contents', ___.frozenFunc(function () {
function contents$_lit$($dis, elem) {

// *** jquery.js ***
//  1260:   contents: function(elem){return jQuery.nodeName(elem,"iframe")?elem.contentDocument||elem.contentWindow.document:jQuery.makeArray(elem.childNodes);}

return $v.cm(jQuery, 'nodeName', [ elem, 'iframe' ]) ? $v.r(elem, 'contentDocument') || $v.r($v.r(elem, 'contentWindow'), 'document'): $v.cm(jQuery, 'makeArray', [ $v.r(elem, 'childNodes') ]);
}
___.func(contents$_lit$, 'contents$_lit$');
;
var contents$_lit = $v.dis(___.primFreeze(contents$_lit$), 'contents$_lit');
return contents$_lit;
}).CALL___() ]), $v.dis(___.frozenFunc(function ($dis, name, fn) {

// *** jquery.js ***
//  1262:   jQuery.fn[ name ] = function( selector ) {

$v.s($v.r(jQuery, 'fn'), name, $v.dis(___.frozenFunc(function ($dis, selector) {

// *** jquery.js ***
//  1263:     var ret = jQuery.map( this, fn );

var ret = $v.cm(jQuery, 'map', [ $dis, fn ]);

// *** jquery.js ***
//  1265:     if ( selector && typeof selector == "string" )
//  1266:       ret = jQuery.multiFilter( selector, ret );

if (selector && $v.typeOf(selector) == 'string') ret = $v.cm(jQuery, 'multiFilter', [ selector, ret ]);

// *** jquery.js ***
//  1268:     return this.pushStack( jQuery.unique( ret ) );

return $v.cm($dis, 'pushStack', [ $v.cm(jQuery, 'unique', [ ret ]) ]);
})));

// *** jquery.js ***
//  1269:   };

})) ]);

// *** jquery.js ***
//  1273:   appendTo: "append",
//  1274:   prependTo: "prepend",
//  1275:   insertBefore: "before",
//  1276:   insertAfter: "after",
//  1277:   replaceAll: "replaceWith"
//  1278: }, function(name, original){

$v.cm(jQuery, 'each', [ ___.initializeMap([ 'appendTo', 'append', 'prependTo', 'prepend', 'insertBefore', 'before', 'insertAfter', 'after', 'replaceAll', 'replaceWith' ]), $v.dis(___.frozenFunc(function ($dis, name, original) {
$v.s($v.r(jQuery, 'fn'), name, $v.dis(___.frozenFunc(function ($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//  1280:     var args = arguments;

var args = Array.slice(a___, 1);

// *** jquery.js ***
//  1282:     return this.each(function(){
//  1285:     });

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//  1283:       for ( var i = 0, length = args.length; i < length; i++ )
//  1284:         jQuery( args[ i ] )[ original ]( this );

for (var i = 0, length = $v.r(args, 'length'); i < length; i++) $v.cm($v.cf(jQuery, [ $v.r(args, i) ]), original, [ $dis ]);
})) ]);
})));

// *** jquery.js ***
//  1286:   };

})) ]);
$v.cm(jQuery, 'each', [ ___.initializeMap([ 'removeAttr', ___.frozenFunc(function () {

// *** jquery.js ***
//  1290:   removeAttr: function( name ) {
//  1291:     jQuery.attr( this, name, "" );

function removeAttr$_lit$($dis, name) {
$v.cm(jQuery, 'attr', [ $dis, name, '' ]);

// *** jquery.js ***
//  1292:     if (this.nodeType == 1)
//  1293:       this.removeAttribute( name );

if ($v.r($dis, 'nodeType') == 1) $v.cm($dis, 'removeAttribute', [ name ]);
}
___.func(removeAttr$_lit$, 'removeAttr$_lit$');
;
var removeAttr$_lit = $v.dis(___.primFreeze(removeAttr$_lit$), 'removeAttr$_lit');
return removeAttr$_lit;
}).CALL___(), 'addClass', ___.frozenFunc(function () {

// *** jquery.js ***
//  1296:   addClass: function( classNames ) {
//  1297:     jQuery.className.add( this, classNames );

function addClass$_lit$($dis, classNames) {
$v.cm($v.r(jQuery, 'className'), 'add', [ $dis, classNames ]);
}
___.func(addClass$_lit$, 'addClass$_lit$');
;
var addClass$_lit = $v.dis(___.primFreeze(addClass$_lit$), 'addClass$_lit');
return addClass$_lit;
}).CALL___(), 'removeClass', ___.frozenFunc(function () {

// *** jquery.js ***
//  1300:   removeClass: function( classNames ) {
//  1301:     jQuery.className.remove( this, classNames );

function removeClass$_lit$($dis, classNames) {
$v.cm($v.r(jQuery, 'className'), 'remove', [ $dis, classNames ]);
}
___.func(removeClass$_lit$, 'removeClass$_lit$');
;
var removeClass$_lit = $v.dis(___.primFreeze(removeClass$_lit$), 'removeClass$_lit');
return removeClass$_lit;
}).CALL___(), 'toggleClass', ___.frozenFunc(function () {

// *** jquery.js ***
//  1304:   toggleClass: function( classNames ) {
//  1305:     jQuery.className[ jQuery.className.has( this, classNames ) ? "remove" : "add" ]( this, classNames );

function toggleClass$_lit$($dis, classNames) {
$v.cm($v.r(jQuery, 'className'), $v.cm($v.r(jQuery, 'className'), 'has', [ $dis, classNames ]) ? 'remove': 'add', [ $dis, classNames ]);
}
___.func(toggleClass$_lit$, 'toggleClass$_lit$');
;
var toggleClass$_lit = $v.dis(___.primFreeze(toggleClass$_lit$), 'toggleClass$_lit');
return toggleClass$_lit;
}).CALL___(), 'remove', ___.frozenFunc(function () {

// *** jquery.js ***
//  1308:   remove: function( selector ) {
//  1310:       // Prevent memory leaks
//  1311:       jQuery( "*", this ).add(this).each(function(){
//  1312:         jQuery.event.remove(this);
//  1314:       });
//  1317:     }

function remove$_lit$($dis, selector) {

// *** jquery.js ***
//  1309:     if ( !selector || jQuery.filter( selector, [ this ] ).r.length ) {

if (!selector || $v.r($v.r($v.cm(jQuery, 'filter', [ selector, [ $dis ] ]), 'r'), 'length')) {
$v.cm($v.cm($v.cf(jQuery, [ '*', $dis ]), 'add', [ $dis ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm($v.r(jQuery, 'event'), 'remove', [ $dis ]);
$v.cm(jQuery, 'removeData', [ $dis ]);

// *** jquery.js ***
//  1313:         jQuery.removeData(this);

})) ]);

// *** jquery.js ***
//  1315:       if (this.parentNode)
//  1316:         this.parentNode.removeChild( this );

if ($v.r($dis, 'parentNode')) $v.cm($v.r($dis, 'parentNode'), 'removeChild', [ $dis ]);
}
}
___.func(remove$_lit$, 'remove$_lit$');
;
var remove$_lit = $v.dis(___.primFreeze(remove$_lit$), 'remove$_lit');
return remove$_lit;
}).CALL___(), 'empty', ___.frozenFunc(function () {

// *** jquery.js ***
//  1322:     jQuery( ">*", this ).remove();
//  1323: 
//  1324:     // Remove any remaining nodes

function empty$_lit$($dis) {
$v.cm($v.cf(jQuery, [ '\x3e*', $dis ]), 'remove', [ ]);

// *** jquery.js ***
//  1325:     while ( this.firstChild )
//  1326:       this.removeChild( this.firstChild );

while ($v.r($dis, 'firstChild')) $v.cm($dis, 'removeChild', [ $v.r($dis, 'firstChild') ]);
}
___.func(empty$_lit$, 'empty$_lit$');
;
var empty$_lit = $v.dis(___.primFreeze(empty$_lit$), 'empty$_lit');
return empty$_lit;
}).CALL___() ]), $v.dis(___.frozenFunc(function ($dis, name, fn) {
$v.s($v.r(jQuery, 'fn'), name, $v.dis(___.frozenFunc(function ($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//  1330:     return this.each( fn, arguments );

return $v.cm($dis, 'each', [ fn, Array.slice(a___, 1) ]);
})));

// *** jquery.js ***
//  1331:   };

})) ]);

// *** jquery.js ***
//  1334: jQuery.each([ "Height", "Width" ], function(i, name){

$v.cm(jQuery, 'each', [ [ 'Height', 'Width' ], $v.dis(___.frozenFunc(function ($dis, i, name) {

// *** jquery.js ***
//  1335:   var type = name.toLowerCase();

var type = $v.cm(name, 'toLowerCase', [ ]);

// *** jquery.js ***
//  1337:   jQuery.fn[ type ] = function( size ) {

$v.s($v.r(jQuery, 'fn'), type, $v.dis(___.frozenFunc(function ($dis, size) {

// *** jquery.js ***
//  1339:     return this[0] == window ?
//  1340:       // Opera reports document.body.client[Width/Height] properly in both quirks and standards
//  1341:       jQuery.browser.opera && document.body[ "client" + name ] ||
//  1342: 
//  1343:       // Safari reports inner[Width/Height] just fine (Mozilla and Opera include scroll bar widths)
//  1344:       jQuery.browser.safari && window[ "inner" + name ] ||
//  1345: 
//  1346:       // Everyone else use document.documentElement or document.body depending on Quirks vs Standards mode
//  1347:       document.compatMode == "CSS1Compat" && document.documentElement[ "client" + name ] || document.body[ "client" + name ] :
//  1348: 
//  1349:       // Get document width or height
//  1350:       this[0] == document ?
//  1351:         // Either scroll[Width/Height] or offset[Width/Height], whichever is greater
//  1352:         Math.max(
//  1353:           Math.max(document.body["scroll" + name], document.documentElement["scroll" + name]),
//  1354:           Math.max(document.body["offset" + name], document.documentElement["offset" + name])
//  1355:         ) :
//  1356: 
//  1357:         // Get or set width or height on the element
//  1358:         size == undefined ?
//  1359:           // Get width or height on the element
//  1360:           (this.length ? jQuery.css( this[0], type ) : null) :
//  1361: 
//  1362:           // Set the width or height on the element (default to pixels if value is unitless)
//  1363:           this.css( type, size.constructor == String ? size : size + "px" );

return $v.r($dis, 0) == $v.ro('window') ? $v.r($v.r(jQuery, 'browser'), 'opera') && $v.r($v.r($v.ro('document'), 'body'), 'client' + name) || $v.r($v.r(jQuery, 'browser'), 'safari') && $v.r($v.ro('window'), 'inner' + name) || $v.r($v.ro('document'), 'compatMode') == 'CSS1Compat' && $v.r($v.r($v.ro('document'), 'documentElement'), 'client' + name) || $v.r($v.r($v.ro('document'), 'body'), 'client' + name): $v.r($dis, 0) == $v.ro('document') ? $v.cm($v.ro('Math'), 'max', [ $v.cm($v.ro('Math'), 'max', [ $v.r($v.r($v.ro('document'), 'body'), 'scroll' + name), $v.r($v.r($v.ro('document'), 'documentElement'), 'scroll' + name) ]), $v.cm($v.ro('Math'), 'max', [ $v.r($v.r($v.ro('document'), 'body'), 'offset' + name), $v.r($v.r($v.ro('document'), 'documentElement'), 'offset' + name) ]) ]): size == $v.ro('undefined') ? $v.r($dis, 'length') ? $v.cm(jQuery, 'css', [ $v.r($dis, 0), type ]): null: $v.cm($dis, 'css', [ type, $v.r(size, 'constructor') == $v.ro('String') ? size: size + 'px' ]);
})));

// *** jquery.js ***
//  1364:   };

})) ]);
;

// *** jquery.js ***
//  1370: }var chars = jQuery.browser.safari && parseInt(jQuery.browser.version) < 417 ?
//  1371:     "(?:[\\w*_-]|\\\\.)" :
//  1372:     "(?:[\\w\u0128-\uFFFF*_-]|\\\\.)",
//  1373:   quickChild = new RegExp("^>\\s*(" + chars + "+)"),
//  1374:   quickID = new RegExp("^(" + chars + "+)(#)(" + chars + "+)"),
//  1375:   quickClass = new RegExp("^([#.]?)(" + chars + "*)");

var chars = $v.r($v.r(jQuery, 'browser'), 'safari') && $v.cf($v.ro('parseInt'), [ $v.r($v.r(jQuery, 'browser'), 'version') ]) < 417? '(?:[\\w*_-]|\\\\.)': '(?:[\\w\u0128-\uffff*_-]|\\\\.)', quickChild = $v.construct($v.ro('RegExp'), [ '^\x3e\\s*(' + chars + '+)' ]), quickID = $v.construct($v.ro('RegExp'), [ '^(' + chars + '+)(#)(' + chars + '+)' ]), quickClass = $v.construct($v.ro('RegExp'), [ '^([#.]?)(' + chars + '*)' ]);
$v.cm(jQuery, 'extend', [ ___.initializeMap([ 'expr', ___.initializeMap([ '', ___.frozenFunc(function () {
function $_lit$($dis, a, i, m) {

// *** jquery.js ***
//  1379:     "": function(a,i,m){return m[2]=="*"||jQuery.nodeName(a,m[2]);},

return $v.r(m, 2) == '*' || $v.cm(jQuery, 'nodeName', [ a, $v.r(m, 2) ]);
}
___.func($_lit$, '$_lit$');
;
var $_lit = $v.dis(___.primFreeze($_lit$), '$_lit');
return $_lit;
}).CALL___(), '#', ___.frozenFunc(function () {
function badName$_lit$($dis, a, i, m) {

// *** jquery.js ***
//  1380:     "#": function(a,i,m){return a.getAttribute("id")==m[2];},

return $v.cm(a, 'getAttribute', [ 'id' ]) == $v.r(m, 2);
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), ':', ___.initializeMap([ 'lt', ___.frozenFunc(function () {
function lt$_lit$($dis, a, i, m) {

// *** jquery.js ***
//  1383:       lt: function(a,i,m){return i<m[3]-0;},

return i < $v.r(m, 3) - 0;
}
___.func(lt$_lit$, 'lt$_lit$');
;
var lt$_lit = $v.dis(___.primFreeze(lt$_lit$), 'lt$_lit');
return lt$_lit;
}).CALL___(), 'gt', ___.frozenFunc(function () {
function gt$_lit$($dis, a, i, m) {

// *** jquery.js ***
//  1384:       gt: function(a,i,m){return i>m[3]-0;},

return i > $v.r(m, 3) - 0;
}
___.func(gt$_lit$, 'gt$_lit$');
;
var gt$_lit = $v.dis(___.primFreeze(gt$_lit$), 'gt$_lit');
return gt$_lit;
}).CALL___(), 'nth', ___.frozenFunc(function () {
function nth$_lit$($dis, a, i, m) {

// *** jquery.js ***
//  1385:       nth: function(a,i,m){return m[3]-0==i;},

return $v.r(m, 3) - 0 == i;
}
___.func(nth$_lit$, 'nth$_lit$');
;
var nth$_lit = $v.dis(___.primFreeze(nth$_lit$), 'nth$_lit');
return nth$_lit;
}).CALL___(), 'eq', ___.frozenFunc(function () {
function eq$_lit$($dis, a, i, m) {

// *** jquery.js ***
//  1386:       eq: function(a,i,m){return m[3]-0==i;},

return $v.r(m, 3) - 0 == i;
}
___.func(eq$_lit$, 'eq$_lit$');
;
var eq$_lit = $v.dis(___.primFreeze(eq$_lit$), 'eq$_lit');
return eq$_lit;
}).CALL___(), 'first', ___.frozenFunc(function () {

// *** jquery.js ***
//  1387:       first: function(a,i){return i==0;},

function first$_lit$($dis, a, i) {
return i == 0;
}
___.func(first$_lit$, 'first$_lit$');
;
var first$_lit = $v.dis(___.primFreeze(first$_lit$), 'first$_lit');
return first$_lit;
}).CALL___(), 'last', ___.frozenFunc(function () {
function last$_lit$($dis, a, i, m, r) {

// *** jquery.js ***
//  1388:       last: function(a,i,m,r){return i==r.length-1;},

return i == $v.r(r, 'length') - 1;
}
___.func(last$_lit$, 'last$_lit$');
;
var last$_lit = $v.dis(___.primFreeze(last$_lit$), 'last$_lit');
return last$_lit;
}).CALL___(), 'even', ___.frozenFunc(function () {
function even$_lit$($dis, a, i) {

// *** jquery.js ***
//  1389:       even: function(a,i){return i%2==0;},

return i % 2 == 0;
}
___.func(even$_lit$, 'even$_lit$');
;
var even$_lit = $v.dis(___.primFreeze(even$_lit$), 'even$_lit');
return even$_lit;
}).CALL___(), 'odd', ___.frozenFunc(function () {

// *** jquery.js ***
//  1390:       odd: function(a,i){return i%2;},

function odd$_lit$($dis, a, i) {
return i % 2;
}
___.func(odd$_lit$, 'odd$_lit$');
;
var odd$_lit = $v.dis(___.primFreeze(odd$_lit$), 'odd$_lit');
return odd$_lit;
}).CALL___(), 'first-child', ___.frozenFunc(function () {
function badName$_lit$($dis, a) {

// *** jquery.js ***
//  1393:       "first-child": function(a){return a.parentNode.getElementsByTagName("*")[0]==a;},

return $v.r($v.cm($v.r(a, 'parentNode'), 'getElementsByTagName', [ '*' ]), 0) == a;
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), 'last-child', ___.frozenFunc(function () {
function badName$_lit$($dis, a) {

// *** jquery.js ***
//  1394:       "last-child": function(a){return jQuery.nth(a.parentNode.lastChild,1,"previousSibling")==a;},

return $v.cm(jQuery, 'nth', [ $v.r($v.r(a, 'parentNode'), 'lastChild'), 1, 'previousSibling' ]) == a;
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), 'only-child', ___.frozenFunc(function () {
function badName$_lit$($dis, a) {

// *** jquery.js ***
//  1395:       "only-child": function(a){return !jQuery.nth(a.parentNode.lastChild,2,"previousSibling");},

return !$v.cm(jQuery, 'nth', [ $v.r($v.r(a, 'parentNode'), 'lastChild'), 2, 'previousSibling' ]);
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), 'parent', ___.frozenFunc(function () {
function parent$_lit$($dis, a) {

// *** jquery.js ***
//  1398:       parent: function(a){return a.firstChild;},

return $v.r(a, 'firstChild');
}
___.func(parent$_lit$, 'parent$_lit$');
;
var parent$_lit = $v.dis(___.primFreeze(parent$_lit$), 'parent$_lit');
return parent$_lit;
}).CALL___(), 'empty', ___.frozenFunc(function () {
function empty$_lit$($dis, a) {

// *** jquery.js ***
//  1399:       empty: function(a){return !a.firstChild;},

return !$v.r(a, 'firstChild');
}
___.func(empty$_lit$, 'empty$_lit$');
;
var empty$_lit = $v.dis(___.primFreeze(empty$_lit$), 'empty$_lit');
return empty$_lit;
}).CALL___(), 'contains', ___.frozenFunc(function () {
function contains$_lit$($dis, a, i, m) {

// *** jquery.js ***
//  1402:       contains: function(a,i,m){return (a.textContent||a.innerText||jQuery(a).text()||"").indexOf(m[3])>=0;},

return $v.cm($v.r(a, 'textContent') || $v.r(a, 'innerText') || $v.cm($v.cf(jQuery, [ a ]), 'text', [ ]) || '', 'indexOf', [ $v.r(m, 3) ]) >= 0;
}
___.func(contains$_lit$, 'contains$_lit$');
;
var contains$_lit = $v.dis(___.primFreeze(contains$_lit$), 'contains$_lit');
return contains$_lit;
}).CALL___(), 'visible', ___.frozenFunc(function () {
function visible$_lit$($dis, a) {

// *** jquery.js ***
//  1405:       visible: function(a){return "hidden"!=a.type&&jQuery.css(a,"display")!="none"&&jQuery.css(a,"visibility")!="hidden";},

return 'hidden' != $v.r(a, 'type') && $v.cm(jQuery, 'css', [ a, 'display' ]) != 'none' && $v.cm(jQuery, 'css', [ a, 'visibility' ]) != 'hidden';
}
___.func(visible$_lit$, 'visible$_lit$');
;
var visible$_lit = $v.dis(___.primFreeze(visible$_lit$), 'visible$_lit');
return visible$_lit;
}).CALL___(), 'hidden', ___.frozenFunc(function () {
function hidden$_lit$($dis, a) {

// *** jquery.js ***
//  1406:       hidden: function(a){return "hidden"==a.type||jQuery.css(a,"display")=="none"||jQuery.css(a,"visibility")=="hidden";},

return 'hidden' == $v.r(a, 'type') || $v.cm(jQuery, 'css', [ a, 'display' ]) == 'none' || $v.cm(jQuery, 'css', [ a, 'visibility' ]) == 'hidden';
}
___.func(hidden$_lit$, 'hidden$_lit$');
;
var hidden$_lit = $v.dis(___.primFreeze(hidden$_lit$), 'hidden$_lit');
return hidden$_lit;
}).CALL___(), 'enabled', ___.frozenFunc(function () {
function enabled$_lit$($dis, a) {

// *** jquery.js ***
//  1409:       enabled: function(a){return !a.disabled;},

return !$v.r(a, 'disabled');
}
___.func(enabled$_lit$, 'enabled$_lit$');
;
var enabled$_lit = $v.dis(___.primFreeze(enabled$_lit$), 'enabled$_lit');
return enabled$_lit;
}).CALL___(), 'disabled', ___.frozenFunc(function () {
function disabled$_lit$($dis, a) {

// *** jquery.js ***
//  1410:       disabled: function(a){return a.disabled;},

return $v.r(a, 'disabled');
}
___.func(disabled$_lit$, 'disabled$_lit$');
;
var disabled$_lit = $v.dis(___.primFreeze(disabled$_lit$), 'disabled$_lit');
return disabled$_lit;
}).CALL___(), 'checked', ___.frozenFunc(function () {
function checked$_lit$($dis, a) {

// *** jquery.js ***
//  1411:       checked: function(a){return a.checked;},

return $v.r(a, 'checked');
}
___.func(checked$_lit$, 'checked$_lit$');
;
var checked$_lit = $v.dis(___.primFreeze(checked$_lit$), 'checked$_lit');
return checked$_lit;
}).CALL___(), 'selected', ___.frozenFunc(function () {
function selected$_lit$($dis, a) {

// *** jquery.js ***
//  1412:       selected: function(a){return a.selected||jQuery.attr(a,"selected");},

return $v.r(a, 'selected') || $v.cm(jQuery, 'attr', [ a, 'selected' ]);
}
___.func(selected$_lit$, 'selected$_lit$');
;
var selected$_lit = $v.dis(___.primFreeze(selected$_lit$), 'selected$_lit');
return selected$_lit;
}).CALL___(), 'text', ___.frozenFunc(function () {
function text$_lit$($dis, a) {

// *** jquery.js ***
//  1415:       text: function(a){return "text"==a.type;},

return 'text' == $v.r(a, 'type');
}
___.func(text$_lit$, 'text$_lit$');
;
var text$_lit = $v.dis(___.primFreeze(text$_lit$), 'text$_lit');
return text$_lit;
}).CALL___(), 'radio', ___.frozenFunc(function () {
function radio$_lit$($dis, a) {

// *** jquery.js ***
//  1416:       radio: function(a){return "radio"==a.type;},

return 'radio' == $v.r(a, 'type');
}
___.func(radio$_lit$, 'radio$_lit$');
;
var radio$_lit = $v.dis(___.primFreeze(radio$_lit$), 'radio$_lit');
return radio$_lit;
}).CALL___(), 'checkbox', ___.frozenFunc(function () {
function checkbox$_lit$($dis, a) {

// *** jquery.js ***
//  1417:       checkbox: function(a){return "checkbox"==a.type;},

return 'checkbox' == $v.r(a, 'type');
}
___.func(checkbox$_lit$, 'checkbox$_lit$');
;
var checkbox$_lit = $v.dis(___.primFreeze(checkbox$_lit$), 'checkbox$_lit');
return checkbox$_lit;
}).CALL___(), 'file', ___.frozenFunc(function () {
function file$_lit$($dis, a) {

// *** jquery.js ***
//  1418:       file: function(a){return "file"==a.type;},

return 'file' == $v.r(a, 'type');
}
___.func(file$_lit$, 'file$_lit$');
;
var file$_lit = $v.dis(___.primFreeze(file$_lit$), 'file$_lit');
return file$_lit;
}).CALL___(), 'password', ___.frozenFunc(function () {
function password$_lit$($dis, a) {

// *** jquery.js ***
//  1419:       password: function(a){return "password"==a.type;},

return 'password' == $v.r(a, 'type');
}
___.func(password$_lit$, 'password$_lit$');
;
var password$_lit = $v.dis(___.primFreeze(password$_lit$), 'password$_lit');
return password$_lit;
}).CALL___(), 'submit', ___.frozenFunc(function () {
function submit$_lit$($dis, a) {

// *** jquery.js ***
//  1420:       submit: function(a){return "submit"==a.type;},

return 'submit' == $v.r(a, 'type');
}
___.func(submit$_lit$, 'submit$_lit$');
;
var submit$_lit = $v.dis(___.primFreeze(submit$_lit$), 'submit$_lit');
return submit$_lit;
}).CALL___(), 'image', ___.frozenFunc(function () {
function image$_lit$($dis, a) {

// *** jquery.js ***
//  1421:       image: function(a){return "image"==a.type;},

return 'image' == $v.r(a, 'type');
}
___.func(image$_lit$, 'image$_lit$');
;
var image$_lit = $v.dis(___.primFreeze(image$_lit$), 'image$_lit');
return image$_lit;
}).CALL___(), 'reset', ___.frozenFunc(function () {
function reset$_lit$($dis, a) {

// *** jquery.js ***
//  1422:       reset: function(a){return "reset"==a.type;},

return 'reset' == $v.r(a, 'type');
}
___.func(reset$_lit$, 'reset$_lit$');
;
var reset$_lit = $v.dis(___.primFreeze(reset$_lit$), 'reset$_lit');
return reset$_lit;
}).CALL___(), 'button', ___.frozenFunc(function () {
function button$_lit$($dis, a) {

// *** jquery.js ***
//  1423:       button: function(a){return "button"==a.type||jQuery.nodeName(a,"button");},

return 'button' == $v.r(a, 'type') || $v.cm(jQuery, 'nodeName', [ a, 'button' ]);
}
___.func(button$_lit$, 'button$_lit$');
;
var button$_lit = $v.dis(___.primFreeze(button$_lit$), 'button$_lit');
return button$_lit;
}).CALL___(), 'input', ___.frozenFunc(function () {
function input$_lit$($dis, a) {

// *** jquery.js ***
//  1424:       input: function(a){return /input|select|textarea|button/i.test(a.nodeName);},

return $v.cm($v.construct(RegExp, [ 'input|select|textarea|button', 'i' ]), 'test', [ $v.r(a, 'nodeName') ]);
}
___.func(input$_lit$, 'input$_lit$');
;
var input$_lit = $v.dis(___.primFreeze(input$_lit$), 'input$_lit');
return input$_lit;
}).CALL___(), 'has', ___.frozenFunc(function () {
function has$_lit$($dis, a, i, m) {

// *** jquery.js ***
//  1427:       has: function(a,i,m){return jQuery.find(m[3],a).length;},

return $v.r($v.cm(jQuery, 'find', [ $v.r(m, 3), a ]), 'length');
}
___.func(has$_lit$, 'has$_lit$');
;
var has$_lit = $v.dis(___.primFreeze(has$_lit$), 'has$_lit');
return has$_lit;
}).CALL___(), 'header', ___.frozenFunc(function () {
function header$_lit$($dis, a) {

// *** jquery.js ***
//  1430:       header: function(a){return /h\d/i.test(a.nodeName);},

return $v.cm($v.construct(RegExp, [ 'h\\d', 'i' ]), 'test', [ $v.r(a, 'nodeName') ]);
}
___.func(header$_lit$, 'header$_lit$');
;
var header$_lit = $v.dis(___.primFreeze(header$_lit$), 'header$_lit');
return header$_lit;
}).CALL___(), 'animated', ___.frozenFunc(function () {
function animated$_lit$($dis, a) {
return $v.r($v.cm(jQuery, 'grep', [ $v.r(jQuery, 'timers'), $v.dis(___.frozenFunc(function ($dis, fn) {

// *** jquery.js ***
//  1433:       animated: function(a){return jQuery.grep(jQuery.timers,function(fn){return a==fn.elem;}).length;}

return a == $v.r(fn, 'elem');
})) ]), 'length');
}
___.func(animated$_lit$, 'animated$_lit$');
;
var animated$_lit = $v.dis(___.primFreeze(animated$_lit$), 'animated$_lit');
return animated$_lit;

// *** jquery.js ***
//  1446:     new RegExp("^([:.#]*)(" + chars + "+)")

}).CALL___() ]) ]), 'parse', [ $v.construct(RegExp, [ '^(\\[) *@?([\\w-]+) *([!*$^~=]*) *(\'?\"?)(.*?)\\4 *\\]' ]), $v.construct(RegExp, [ '^(:)([\\w-]+)\\(\"?\'?(.*?(\\(.*?\\))?[^(]*?)\"?\'?\\)' ]), $v.construct($v.ro('RegExp'), [ '^([:.#]*)(' + chars + '+)' ]) ], 'multiFilter', ___.frozenFunc(function () {

// *** jquery.js ***
//  1449:   multiFilter: function( expr, elems, not ) {
//  1451: 
//  1458: 

function multiFilter$_lit$($dis, expr, elems, not) {

// *** jquery.js ***
//  1450:     var old, cur = [];

var old, cur = [ ];

// *** jquery.js ***
//  1452:     while ( expr && expr != old ) {
//  1457:     }

while (expr && expr != old) {

// *** jquery.js ***
//  1453:       old = expr;

old = expr;

// *** jquery.js ***
//  1454:       var f = jQuery.filter( expr, elems, not );

var f = $v.cm(jQuery, 'filter', [ expr, elems, not ]);

// *** jquery.js ***
//  1455:       expr = f.t.replace(/^\s*,\s�/, "" );

expr = $v.cm($v.r(f, 't'), 'replace', [ $v.construct(RegExp, [ '^\\s*,\\s*' ]), '' ]);

// *** jquery.js ***
//  1456:       cur = not ? elems = f.r : jQuery.merge( cur, f.r );

cur = not? (elems = $v.r(f, 'r')): $v.cm(jQuery, 'merge', [ cur, $v.r(f, 'r') ]);
}

// *** jquery.js ***
//  1459:     return cur;

return cur;
}
___.func(multiFilter$_lit$, 'multiFilter$_lit$');
;
var multiFilter$_lit = $v.dis(___.primFreeze(multiFilter$_lit$), 'multiFilter$_lit');
return multiFilter$_lit;
}).CALL___(), 'find', ___.frozenFunc(function () {

// *** jquery.js ***
//  1462:   find: function( t, context ) {
//  1463:     // Quickly handle non-string expressions
//  1466: 
//  1467:     // check to make sure context is a DOM element or a document
//  1470: 
//  1471:     // Set the correct context (if none is provided)
//  1473: 
//  1474:     // Initialize the search
//  1476: 
//  1477:     // Continue while a selector expression exists, and while
//  1478:     // we're no longer looping upon ourselves
//  1639: 
//  1640:     // An error occurred with the selector;
//  1641:     // just return an empty set instead
//  1642:     if ( t )
//  1644: 
//  1645:     // Remove the root context
//  1648: 
//  1649:     // And combine the results
//  1651: 

function find$_lit$($dis, t, context) {

// *** jquery.js ***
//  1464:     if ( typeof t != "string" )
//  1465:       return [ t ];

if ($v.typeOf(t) != 'string') return [ t ];

// *** jquery.js ***
//  1468:     if ( context && context.nodeType != 1 && context.nodeType != 9)
//  1469:       return [ ];

if (context && $v.r(context, 'nodeType') != 1 && $v.r(context, 'nodeType') != 9) return [ ];

// *** jquery.js ***
//  1472:     context = context || document;

context = context || $v.ro('document');

// *** jquery.js ***
//  1475:     var ret = [context], done = [], last, nodeName;

var ret = [ context ], done = [ ], last, nodeName;

// *** jquery.js ***
//  1479:     while ( t && last != t ) {
//  1482: 
//  1484: 
//  1486: 
//  1487:       // An attempt at speeding up child selectors that
//  1488:       // point to a specific element tag
//  1490: 
//  1492: 
//  1495: 
//  1496:         // Perform our own iteration and filter
//  1501: 
//  1508: 
//  1539:       }
//  1540: 
//  1541:       // See if there's still an expression, and that we haven't already
//  1542:       // matched a token
//  1544:         // Handle multiple expressions
//  1559:           // Optimize for the case nodeName#idName
//  1562: 
//  1563:           // Re-organize the results, so that they're consistent
//  1573: 
//  1574:           m[2] = m[2].replace(/\\/g, "");
//  1575: 
//  1577: 
//  1578:           // Try to do a global search by ID, where we can
//  1625: 
//  1627:         }
//  1628: 
//  1629:       }
//  1630: 
//  1631:       // If a selector string still exists
//  1633:         // Attempt to filter it
//  1637:       }
//  1638:     }

while (t && last != t) {

// *** jquery.js ***
//  1480:       var r = [];

var r = [ ];

// *** jquery.js ***
//  1481:       last = t;

last = t;

// *** jquery.js ***
//  1483:       t = jQuery.trim(t);

t = $v.cm(jQuery, 'trim', [ t ]);

// *** jquery.js ***
//  1485:       var foundToken = false,
//  1489:         re = quickChild,
//  1491:         m = re.exec(t);

var foundToken = false, re = quickChild, m = $v.cm(re, 'exec', [ t ]);

// *** jquery.js ***
//  1493:       if ( m ) {

if (m) {

// *** jquery.js ***
//  1494:         nodeName = m[1].toUpperCase();

nodeName = $v.cm($v.r(m, 1), 'toUpperCase', [ ]);

// *** jquery.js ***
//  1497:         for ( var i = 0; ret[i]; i++ )
//  1498:           for ( var c = ret[i].firstChild; c; c = c.nextSibling )
//  1499:             if ( c.nodeType == 1 && (nodeName == "*" || c.nodeName.toUpperCase() == nodeName) )
//  1500:               r.push( c );

for (var i = 0; $v.r(ret, i); i++) for (var c = $v.r($v.r(ret, i), 'firstChild'); c; c = $v.r(c, 'nextSibling')) if ($v.r(c, 'nodeType') == 1 && (nodeName == '*' || $v.cm($v.r(c, 'nodeName'), 'toUpperCase', [ ]) == nodeName)) $v.cm(r, 'push', [ c ]);

// *** jquery.js ***
//  1502:         ret = r;

ret = r;

// *** jquery.js ***
//  1503:         t = t.replace( re, "" );

t = $v.cm(t, 'replace', [ re, '' ]);

// *** jquery.js ***
//  1504:         if ( t.indexOf(" ") == 0 ) continue;

if ($v.cm(t, 'indexOf', [ ' ' ]) == 0) continue;

// *** jquery.js ***
//  1505:         foundToken = true;

foundToken = true;

// *** jquery.js ***
//  1506:       } else {

} else {

// *** jquery.js ***
//  1507:         re = /^([>+~])\s*(\w*)/i;

re = $v.construct(RegExp, [ '^([\x3e+~])\\s*(\\w*)', 'i' ]);

// *** jquery.js ***
//  1509:         if ( (m = re.exec(t)) != null ) {
//  1511: 
//  1515: 
//  1532: 
//  1534: 
//  1535:           // And remove the token
//  1538:         }

if ((m = $v.cm(re, 'exec', [ t ])) != null) {

// *** jquery.js ***
//  1510:           r = [];

r = [ ];

// *** jquery.js ***
//  1512:           var merge = {};

var merge = ___.initializeMap([ ]);

// *** jquery.js ***
//  1513:           nodeName = m[2].toUpperCase();

nodeName = $v.cm($v.r(m, 2), 'toUpperCase', [ ]);

// *** jquery.js ***
//  1514:           m = m[1];

m = $v.r(m, 1);

// *** jquery.js ***
//  1516:           for ( var j = 0, rl = ret.length; j < rl; j++ ) {
//  1531:           }

for (var j = 0, rl = $v.r(ret, 'length'); j < rl; j++) {

// *** jquery.js ***
//  1517:             var n = m == "~" || m == "+" ? ret[j].nextSibling : ret[j].firstChild;

var n = m == '~' || m == '+'? $v.r($v.r(ret, j), 'nextSibling'): $v.r($v.r(ret, j), 'firstChild');

// *** jquery.js ***
//  1518:             for ( ; n; n = n.nextSibling )
//  1519:               if ( n.nodeType == 1 ) {
//  1521: 
//  1523: 
//  1526:                   r.push( n );
//  1527:                 }
//  1528: 
//  1530:               }

for (; n; n = $v.r(n, 'nextSibling')) if ($v.r(n, 'nodeType') == 1) {

// *** jquery.js ***
//  1520:                 var id = jQuery.data(n);

var id = $v.cm(jQuery, 'data', [ n ]);

// *** jquery.js ***
//  1522:                 if ( m == "~" && merge[id] ) break;

if (m == '~' && $v.r(merge, id)) break;

// *** jquery.js ***
//  1524:                 if (!nodeName || n.nodeName.toUpperCase() == nodeName ) {

if (!nodeName || $v.cm($v.r(n, 'nodeName'), 'toUpperCase', [ ]) == nodeName) {

// *** jquery.js ***
//  1525:                   if ( m == "~" ) merge[id] = true;

if (m == '~') $v.s(merge, id, true);
$v.cm(r, 'push', [ n ]);
}

// *** jquery.js ***
//  1529:                 if ( m == "+" ) break;

if (m == '+') break;
}
}

// *** jquery.js ***
//  1533:           ret = r;

ret = r;

// *** jquery.js ***
//  1536:           t = jQuery.trim( t.replace( re, "" ) );

t = $v.cm(jQuery, 'trim', [ $v.cm(t, 'replace', [ re, '' ]) ]);

// *** jquery.js ***
//  1537:           foundToken = true;

foundToken = true;
}
}

// *** jquery.js ***
//  1543:       if ( t && !foundToken ) {

if (t && !foundToken) {

// *** jquery.js ***
//  1545:         if ( !t.indexOf(",") ) {
//  1546:           // Clean the result set
//  1548: 
//  1549:           // Merge the result sets
//  1551: 
//  1552:           // Reset the context
//  1554: 
//  1555:           // Touch up the selector string
//  1557: 

if (!$v.cm(t, 'indexOf', [ ',' ])) {

// *** jquery.js ***
//  1547:           if ( context == ret[0] ) ret.shift();

if (context == $v.r(ret, 0)) $v.cm(ret, 'shift', [ ]);

// *** jquery.js ***
//  1550:           done = jQuery.merge( done, ret );

done = $v.cm(jQuery, 'merge', [ done, ret ]);

// *** jquery.js ***
//  1553:           r = ret = [context];

r = ret = [ context ];

// *** jquery.js ***
//  1556:           t = " " + t.substr(1,t.length);

t = ' ' + $v.cm(t, 'substr', [ 1, $v.r(t, 'length') ]);

// *** jquery.js ***
//  1558:         } else {

} else {

// *** jquery.js ***
//  1560:           var re2 = quickID;

var re2 = quickID;

// *** jquery.js ***
//  1561:           var m = re2.exec(t);

var m = $v.cm(re2, 'exec', [ t ]);

// *** jquery.js ***
//  1564:           if ( m ) {
//  1566: 
//  1568:             // Otherwise, do a traditional filter check for
//  1569:             // ID, class, and element selectors
//  1572:           }

if (m) {

// *** jquery.js ***
//  1565:             m = [ 0, m[2], m[3], m[1] ];

m = [ 0, $v.r(m, 2), $v.r(m, 3), $v.r(m, 1) ];

// *** jquery.js ***
//  1567:           } else {

} else {

// *** jquery.js ***
//  1570:             re2 = quickClass;

re2 = quickClass;

// *** jquery.js ***
//  1571:             m = re2.exec(t);

m = $v.cm(re2, 'exec', [ t ]);
}
$v.s(m, 2, $v.cm($v.r(m, 2), 'replace', [ $v.construct(RegExp, [ '\\\\', 'g' ]), '' ]));

// *** jquery.js ***
//  1576:           var elem = ret[ret.length-1];

var elem = $v.r(ret, $v.r(ret, 'length') - 1);

// *** jquery.js ***
//  1579:           if ( m[1] == "#" && elem && elem.getElementById && !jQuery.isXMLDoc(elem) ) {
//  1580:             // Optimization for HTML document case
//  1582: 
//  1583:             // Do a quick check for the existence of the actual ID attribute
//  1584:             // to avoid selecting by the name attribute in IE
//  1585:             // also check to insure id is a string to avoid selecting an element with the name of 'id' inside a form
//  1588: 
//  1589:             // Do a quick check for node name (where applicable) so
//  1590:             // that div#foo searches will be really fast
//  1593:             // We need to find all descendant elements
//  1604: 
//  1605:             // It's faster to filter by class and be done with it
//  1608: 
//  1609:             // Same with ID filtering
//  1622: 
//  1624:           }

if ($v.r(m, 1) == '#' && elem && $v.r(elem, 'getElementById') && !$v.cm(jQuery, 'isXMLDoc', [ elem ])) {

// *** jquery.js ***
//  1581:             var oid = elem.getElementById(m[2]);

var oid = $v.cm(elem, 'getElementById', [ $v.r(m, 2) ]);

// *** jquery.js ***
//  1586:             if ( (jQuery.browser.msie||jQuery.browser.opera) && oid && typeof oid.id == "string" && oid.id != m[2] )
//  1587:               oid = jQuery('[�id="'+m[2]+'"]', elem)[0];

if (($v.r($v.r(jQuery, 'browser'), 'msie') || $v.r($v.r(jQuery, 'browser'), 'opera')) && oid && $v.typeOf($v.r(oid, 'id')) == 'string' && $v.r(oid, 'id') != $v.r(m, 2)) oid = $v.r($v.cf(jQuery, [ '[@id=\"' + $v.r(m, 2) + '\"]', elem ]), 0);

// *** jquery.js ***
//  1591:             ret = r = oid && (!m[3] || jQuery.nodeName(oid, m[3])) ? [oid] : [];

ret = r = oid && (!$v.r(m, 3) || $v.cm(jQuery, 'nodeName', [ oid, $v.r(m, 3) ])) ? [ oid ]: [ ];

// *** jquery.js ***
//  1592:           } else {

} else {

// *** jquery.js ***
//  1594:             for ( var i = 0; ret[i]; i++ ) {
//  1595:               // Grab the tag name being searched for
//  1597: 
//  1598:               // Handle IE7 being really dumb about <object>s
//  1601: 
//  1603:             }

for (var i = 0; $v.r(ret, i); i++) {

// *** jquery.js ***
//  1596:               var tag = m[1] == "#" && m[3] ? m[3] : m[1] != "" || m[0] == "" ? "*" : m[2];

var tag = $v.r(m, 1) == '#' && $v.r(m, 3) ? $v.r(m, 3): $v.r(m, 1) != '' || $v.r(m, 0) == ''? '*': $v.r(m, 2);

// *** jquery.js ***
//  1599:               if ( tag == "*" && ret[i].nodeName.toLowerCase() == "object" )
//  1600:                 tag = "param";

if (tag == '*' && $v.cm($v.r($v.r(ret, i), 'nodeName'), 'toLowerCase', [ ]) == 'object') tag = 'param';

// *** jquery.js ***
//  1602:               r = jQuery.merge( r, ret[i].getElementsByTagName( tag ));

r = $v.cm(jQuery, 'merge', [ r, $v.cm($v.r(ret, i), 'getElementsByTagName', [ tag ]) ]);
}

// *** jquery.js ***
//  1606:             if ( m[1] == "." )
//  1607:               r = jQuery.classFilter( r, m[2] );

if ($v.r(m, 1) == '.') r = $v.cm(jQuery, 'classFilter', [ r, $v.r(m, 2) ]);

// *** jquery.js ***
//  1610:             if ( m[1] == "#" ) {
//  1612: 
//  1613:               // Try to find the element with the ID
//  1619: 
//  1621:             }

if ($v.r(m, 1) == '#') {

// *** jquery.js ***
//  1611:               var tmp = [];

var tmp = [ ];

// *** jquery.js ***
//  1614:               for ( var i = 0; r[i]; i++ )
//  1615:                 if ( r[i].getAttribute("id") == m[2] ) {
//  1617:                   break;
//  1618:                 }

for (var i = 0; $v.r(r, i); i++) if ($v.cm($v.r(r, i), 'getAttribute', [ 'id' ]) == $v.r(m, 2)) {

// *** jquery.js ***
//  1616:                   tmp = [ r[i] ];

tmp = [ $v.r(r, i) ];
break;
}

// *** jquery.js ***
//  1620:               r = tmp;

r = tmp;
}

// *** jquery.js ***
//  1623:             ret = r;

ret = r;
}

// *** jquery.js ***
//  1626:           t = t.replace( re2, "" );

t = $v.cm(t, 'replace', [ re2, '' ]);
}
}

// *** jquery.js ***
//  1632:       if ( t ) {

if (t) {

// *** jquery.js ***
//  1634:         var val = jQuery.filter(t,r);

var val = $v.cm(jQuery, 'filter', [ t, r ]);

// *** jquery.js ***
//  1635:         ret = r = val.r;

ret = r = $v.r(val, 'r');

// *** jquery.js ***
//  1636:         t = jQuery.trim(val.t);

t = $v.cm(jQuery, 'trim', [ $v.r(val, 't') ]);
}
}

// *** jquery.js ***
//  1643:       ret = [];

if (t) ret = [ ];

// *** jquery.js ***
//  1646:     if ( ret && context == ret[0] )
//  1647:       ret.shift();

if (ret && context == $v.r(ret, 0)) $v.cm(ret, 'shift', [ ]);

// *** jquery.js ***
//  1650:     done = jQuery.merge( done, ret );

done = $v.cm(jQuery, 'merge', [ done, ret ]);

// *** jquery.js ***
//  1652:     return done;

return done;
}
___.func(find$_lit$, 'find$_lit$');
;
var find$_lit = $v.dis(___.primFreeze(find$_lit$), 'find$_lit');
return find$_lit;
}).CALL___(), 'classFilter', ___.frozenFunc(function () {

// *** jquery.js ***
//  1655:   classFilter: function(r,m,not){
//  1662:     }

function classFilter$_lit$($dis, r, m, not) {

// *** jquery.js ***
//  1656:     m = " " + m + " ";

m = ' ' + m + ' ';

// *** jquery.js ***
//  1657:     var tmp = [];

var tmp = [ ];

// *** jquery.js ***
//  1658:     for ( var i = 0; r[i]; i++ ) {

for (var i = 0; $v.r(r, i); i++) {

// *** jquery.js ***
//  1659:       var pass = (" " + r[i].className + " ").indexOf( m ) >= 0;

var pass = $v.cm(' ' + $v.r($v.r(r, i), 'className') + ' ', 'indexOf', [ m ]) >= 0;

// *** jquery.js ***
//  1660:       if ( !not && pass || not && !pass )
//  1661:         tmp.push( r[i] );

if (!not && pass || not && !pass) $v.cm(tmp, 'push', [ $v.r(r, i) ]);
}

// *** jquery.js ***
//  1663:     return tmp;

return tmp;
}
___.func(classFilter$_lit$, 'classFilter$_lit$');
;
var classFilter$_lit = $v.dis(___.primFreeze(classFilter$_lit$), 'classFilter$_lit');
return classFilter$_lit;
}).CALL___(), 'filter', ___.frozenFunc(function () {

// *** jquery.js ***
//  1666:   filter: function(t,r,not) {
//  1668: 
//  1669:     // Look for common filter expressions
//  1775: 
//  1776:     // Return an array of filtered elements (r)
//  1777:     // and the modified expression string (t)

function filter$_lit$($dis, t, r, not) {

// *** jquery.js ***
//  1667:     var last;

var last;

// *** jquery.js ***
//  1670:     while ( t && t != last ) {
//  1672: 
//  1674: 
//  1677: 
//  1685:       }
//  1686: 
//  1687:       if ( !m )
//  1689: 
//  1690:       // :not() is a special case that can be optimized by
//  1691:       // keeping it out of the expression list
//  1693:         // optimize if only one selector found (most common case)
//  1697: 
//  1698:       // We can get a big speed boost by filtering by class here
//  1699:       else if ( m[1] == "." )
//  1701: 
//  1702:       else if ( m[1] == "[" ) {
//  1704: 
//  1719: 
//  1721: 
//  1722:       // We can get a speed boost by handling nth-child here
//  1725:           // parse equations like 'even', 'odd', '5', '2n', '3n+2', '4n-1', '-n+6'
//  1729:           // calculate the numbers (first)n+(last) including if they are negative
//  1731: 
//  1732:         // loop through all the elements left in the jQuery object
//  1757: 
//  1759: 
//  1760:       // Otherwise, find the expression to execute
//  1765: 
//  1768: 
//  1769:         // Execute it against the current filter
//  1772:         }, not );
//  1773:       }
//  1774:     }

while (t && t != last) {

// *** jquery.js ***
//  1671:       last = t;

last = t;

// *** jquery.js ***
//  1673:       var p = jQuery.parse, m;

var p = $v.r(jQuery, 'parse'), m;

// *** jquery.js ***
//  1675:       for ( var i = 0; p[i]; i++ ) {

for (var i = 0; $v.r(p, i); i++) {

// *** jquery.js ***
//  1676:         m = p[i].exec( t );

m = $v.cm($v.r(p, i), 'exec', [ t ]);

// *** jquery.js ***
//  1678:         if ( m ) {
//  1679:           // Remove what we just matched
//  1681: 
//  1682:           m[2] = m[2].replace(/\\/g, "");
//  1684:         }

if (m) {

// *** jquery.js ***
//  1680:           t = t.substring( m[0].length );

t = $v.cm(t, 'substring', [ $v.r($v.r(m, 0), 'length') ]);
$v.s(m, 2, $v.cm($v.r(m, 2), 'replace', [ $v.construct(RegExp, [ '\\\\', 'g' ]), '' ]));

// *** jquery.js ***
//  1683:           break;

break;
}
}

// *** jquery.js ***
//  1688:         break;

if (!m) break;

// *** jquery.js ***
//  1692:       if ( m[1] == ":" && m[2] == "not" )
//  1694:         r = isSimple.test( m[3] ) ?
//  1695:           jQuery.filter(m[3], r, true).r :
//  1696:           jQuery( r ).not( m[3] );

if ($v.r(m, 1) == ':' && $v.r(m, 2) == 'not') r = $v.cm(isSimple, 'test', [ $v.r(m, 3) ]) ? $v.r($v.cm(jQuery, 'filter', [ $v.r(m, 3), r, true ]), 'r'): $v.cm($v.cf(jQuery, [ r ]), 'not', [ $v.r(m, 3) ]);
else if ($v.r(m, 1) == '.') r = $v.cm(jQuery, 'classFilter', [ r, $v.r(m, 2), not ]);

// *** jquery.js ***
//  1700:         r = jQuery.classFilter(r, m[2], not);

else if ($v.r(m, 1) == '[') {

// *** jquery.js ***
//  1703:         var tmp = [], type = m[3];

var tmp = [ ], type = $v.r(m, 3);

// *** jquery.js ***
//  1705:         for ( var i = 0, rl = r.length; i < rl; i++ ) {
//  1707: 
//  1710: 
//  1718:         }

for (var i = 0, rl = $v.r(r, 'length'); i < rl; i++) {

// *** jquery.js ***
//  1706:           var a = r[i], z = a[ jQuery.props[m[2]] || m[2] ];

var a = $v.r(r, i), z = $v.r(a, $v.r($v.r(jQuery, 'props'), $v.r(m, 2)) || $v.r(m, 2));

// *** jquery.js ***
//  1708:           if ( z == null || /href|src|selected/.test(m[2]) )
//  1709:             z = jQuery.attr(a,m[2]) || '';

if (z == null || $v.cm($v.construct(RegExp, [ 'href|src|selected' ]), 'test', [ $v.r(m, 2) ])) z = $v.cm(jQuery, 'attr', [ a, $v.r(m, 2) ]) || '';

// *** jquery.js ***
//  1711:           if ( (type == "" && !!z ||
//  1712:              type == "=" && z == m[5] ||
//  1713:              type == "!=" && z != m[5] ||
//  1714:              type == "^=" && z && !z.indexOf(m[5]) ||
//  1715:              type == "$=" && z.substr(z.length - m[5].length) == m[5] ||
//  1716:              (type == "*=" || type == "~=") && z.indexOf(m[5]) >= 0) ^ not )
//  1717:               tmp.push( a );

if ((type == '' && ! (!z) || type == '=' && z == $v.r(m, 5) || type == '!=' && z != $v.r(m, 5) || type == '^=' && z && !$v.cm(z, 'indexOf', [ $v.r(m, 5) ]) || type == '$=' && $v.cm(z, 'substr', [ $v.r(z, 'length') - $v.r($v.r(m, 5), 'length') ]) == $v.r(m, 5) || (type == '*=' || type == '~=') && $v.cm(z, 'indexOf', [ $v.r(m, 5) ]) >= 0) ^ not) $v.cm(tmp, 'push', [ a ]);
}

// *** jquery.js ***
//  1720:         r = tmp;

r = tmp;

// *** jquery.js ***
//  1723:       } else if ( m[1] == ":" && m[2] == "nth-child" ) {

} else if ($v.r(m, 1) == ':' && $v.r(m, 2) == 'nth-child') {

// *** jquery.js ***
//  1724:         var merge = {}, tmp = [],
//  1726:           test = /(-?)(\d*)n((?:\+|-)?\d*)/.exec(
//  1727:             m[3] == "even" && "2n" || m[3] == "odd" && "2n+1" ||
//  1728:             !/\D/.test(m[3]) && "0n+" + m[3] || m[3]),
//  1730:           first = (test[1] + (test[2] || 1)) - 0, last = test[3] - 0;

var merge = ___.initializeMap([ ]), tmp = [ ], test = $v.cm($v.construct(RegExp, [ '(-?)(\\d*)n((?:\\+|-)?\\d*)' ]), 'exec', [ $v.r(m, 3) == 'even' && '2n' || $v.r(m, 3) == 'odd' && '2n+1' || !$v.cm($v.construct(RegExp, [ '\\D' ]), 'test', [ $v.r(m, 3) ]) && '0n+' + $v.r(m, 3) || $v.r(m, 3) ]), first = $v.r(test, 1) + ($v.r(test, 2) || 1) - 0, last = $v.r(test, 3) - 0;

// *** jquery.js ***
//  1733:         for ( var i = 0, rl = r.length; i < rl; i++ ) {
//  1735: 
//  1745: 
//  1747: 
//  1752:             add = true;
//  1753: 
//  1756:         }

for (var i = 0, rl = $v.r(r, 'length'); i < rl; i++) {

// *** jquery.js ***
//  1734:           var node = r[i], parentNode = node.parentNode, id = jQuery.data(parentNode);

var node = $v.r(r, i), parentNode = $v.r(node, 'parentNode'), id = $v.cm(jQuery, 'data', [ parentNode ]);

// *** jquery.js ***
//  1736:           if ( !merge[id] ) {
//  1738: 
//  1742: 
//  1743:             merge[id] = true;
//  1744:           }

if (!$v.r(merge, id)) {

// *** jquery.js ***
//  1737:             var c = 1;

var c = 1;

// *** jquery.js ***
//  1739:             for ( var n = parentNode.firstChild; n; n = n.nextSibling )
//  1740:               if ( n.nodeType == 1 )
//  1741:                 n.nodeIndex = c++;

for (var n = $v.r(parentNode, 'firstChild'); n; n = $v.r(n, 'nextSibling')) if ($v.r(n, 'nodeType') == 1) $v.s(n, 'nodeIndex', c++);
$v.s(merge, id, true);
}

// *** jquery.js ***
//  1746:           var add = false;

var add = false;

// *** jquery.js ***
//  1748:           if ( first == 0 ) {

if (first == 0) {

// *** jquery.js ***
//  1749:             if ( node.nodeIndex == last )
//  1750:               add = true;

if ($v.r(node, 'nodeIndex') == last) add = true;

// *** jquery.js ***
//  1751:           } else if ( (node.nodeIndex - last) % first == 0 && (node.nodeIndex - last) / first >= 0 )

} else if (($v.r(node, 'nodeIndex') - last) % first == 0 && ($v.r(node, 'nodeIndex') - last) / first >= 0) add = true;

// *** jquery.js ***
//  1754:           if ( add ^ not )
//  1755:             tmp.push( node );

if (add ^ not) $v.cm(tmp, 'push', [ node ]);
}

// *** jquery.js ***
//  1758:         r = tmp;

r = tmp;

// *** jquery.js ***
//  1761:       } else {

} else {

// *** jquery.js ***
//  1762:         var fn = jQuery.expr[ m[1] ];

var fn = $v.r($v.r(jQuery, 'expr'), $v.r(m, 1));

// *** jquery.js ***
//  1763:         if ( typeof fn == "object" )
//  1764:           fn = fn[ m[2] ];

if ($v.typeOf(fn) == 'object') fn = $v.r(fn, $v.r(m, 2));

// *** jquery.js ***
//  1766:         if ( typeof fn == "string" )
//  1767:           fn = eval("false||function(a,i){return " + fn + ";}");

if ($v.typeOf(fn) == 'string') fn = $v.cf($v.ro('eval'), [ 'false||function(a,i){return ' + fn + ';}' ]);

// *** jquery.js ***
//  1770:         r = jQuery.grep( r, function(elem, i){

r = $v.cm(jQuery, 'grep', [ r, $v.dis(___.frozenFunc(function ($dis, elem, i) {

// *** jquery.js ***
//  1771:           return fn(elem, i, m, r);

return $v.cf(fn, [ elem, i, m, r ]);
})), not ]);
}
}

// *** jquery.js ***
//  1778:     return { r: r, t: t };

return ___.initializeMap([ 'r', r, 't', t ]);
}
___.func(filter$_lit$, 'filter$_lit$');
;
var filter$_lit = $v.dis(___.primFreeze(filter$_lit$), 'filter$_lit');
return filter$_lit;
}).CALL___(), 'dir', ___.frozenFunc(function () {

// *** jquery.js ***
//  1781:   dir: function( elem, dir ){

function dir$_lit$($dis, elem, dir) {

// *** jquery.js ***
//  1782:     var matched = [],
//  1783:       cur = elem[dir];

var matched = [ ], cur = $v.r(elem, dir);

// *** jquery.js ***
//  1784:     while ( cur && cur != document ) {
//  1788:     }

while (cur && cur != $v.ro('document')) {

// *** jquery.js ***
//  1785:       if ( cur.nodeType == 1 )
//  1786:         matched.push( cur );

if ($v.r(cur, 'nodeType') == 1) $v.cm(matched, 'push', [ cur ]);

// *** jquery.js ***
//  1787:       cur = cur[dir];

cur = $v.r(cur, dir);
}

// *** jquery.js ***
//  1789:     return matched;

return matched;
}
___.func(dir$_lit$, 'dir$_lit$');
;
var dir$_lit = $v.dis(___.primFreeze(dir$_lit$), 'dir$_lit');
return dir$_lit;
}).CALL___(), 'nth', ___.frozenFunc(function () {

// *** jquery.js ***
//  1792:   nth: function(cur,result,dir,elem){
//  1795: 
//  1799: 

function nth$_lit$($dis, cur, result, dir, elem) {

// *** jquery.js ***
//  1793:     result = result || 1;

result = result || 1;

// *** jquery.js ***
//  1794:     var num = 0;

var num = 0;

// *** jquery.js ***
//  1796:     for ( ; cur; cur = cur[dir] )
//  1797:       if ( cur.nodeType == 1 && ++num == result )
//  1798:         break;

for (; cur; cur = $v.r(cur, dir)) if ($v.r(cur, 'nodeType') == 1 && ++num == result) break;

// *** jquery.js ***
//  1800:     return cur;

return cur;
}
___.func(nth$_lit$, 'nth$_lit$');
;
var nth$_lit = $v.dis(___.primFreeze(nth$_lit$), 'nth$_lit');
return nth$_lit;
}).CALL___(), 'sibling', ___.frozenFunc(function () {

// *** jquery.js ***
//  1803:   sibling: function( n, elem ) {
//  1805: 
//  1809:     }
//  1810: 

function sibling$_lit$($dis, n, elem) {

// *** jquery.js ***
//  1804:     var r = [];

var r = [ ];

// *** jquery.js ***
//  1806:     for ( ; n; n = n.nextSibling ) {

for (; n; n = $v.r(n, 'nextSibling')) {

// *** jquery.js ***
//  1807:       if ( n.nodeType == 1 && n != elem )
//  1808:         r.push( n );

if ($v.r(n, 'nodeType') == 1 && n != elem) $v.cm(r, 'push', [ n ]);
}

// *** jquery.js ***
//  1811:     return r;

return r;
}
___.func(sibling$_lit$, 'sibling$_lit$');
;
var sibling$_lit = $v.dis(___.primFreeze(sibling$_lit$), 'sibling$_lit');
return sibling$_lit;
}).CALL___() ]) ]);
$v.s(jQuery, 'event', ___.initializeMap([ 'add', ___.frozenFunc(function () {

// *** jquery.js ***
//  1823:   add: function(elem, types, handler, data) {
//  1826: 
//  1827:     // For whatever reason, IE has trouble passing the window object
//  1828:     // around, causing it to be cloned in the process
//  1831: 
//  1832:     // Make sure that the function being executed has a unique ID
//  1835: 
//  1836:     // if data is passed, bind to handler
//  1838:       // Create temporary function pointer to original handler
//  1840: 
//  1841:       // Create unique handler function, wrapped around original handler
//  1843:         // Pass arguments and context to original handler
//  1845:       });
//  1846: 
//  1847:       // Store data in unique handler
//  1848:       handler.data = data;
//  1849:     }
//  1850: 
//  1851:     // Init the element's event structure
//  1854:         // Handle the second event of a trigger and when
//  1855:         // an event is called after a page has unloaded
//  1858:       });
//  1859:     // Add elem as a property of the handle function
//  1860:     // This is to prevent a memory leak with non-native
//  1861:     // event in IE.
//  1862:     handle.elem = elem;
//  1863: 
//  1864:     // Handle multiple events separated by a space
//  1865:     // jQuery(...).bind("mouseover mouseout", fn);
//  1866:     jQuery.each(types.split(/\s+/), function(index, type) {
//  1867:       // Namespaced event handlers
//  1870:       handler.type = parts[1];
//  1871: 
//  1872:       // Get the current list of functions bound to this event
//  1874: 
//  1875:       // Init the event handler queue
//  1890: 
//  1891:       // Add the function to the element's handler list
//  1892:       handlers[handler.guid] = handler;
//  1893: 
//  1894:       // Keep track of which events have been used, for global triggering
//  1896:     });
//  1897: 
//  1898:     // Nullify elem to prevent memory leaks in IE

function add$_lit$($dis, elem, types, handler, data) {
var x0___;

// *** jquery.js ***
//  1824:     if ( elem.nodeType == 3 || elem.nodeType == 8 )
//  1825:       return;

if ($v.r(elem, 'nodeType') == 3 || $v.r(elem, 'nodeType') == 8) return;

// *** jquery.js ***
//  1829:     if ( jQuery.browser.msie && elem.setInterval )
//  1830:       elem = window;

if ($v.r($v.r(jQuery, 'browser'), 'msie') && $v.r(elem, 'setInterval')) elem = $v.ro('window');

// *** jquery.js ***
//  1833:     if ( !handler.guid )
//  1834:       handler.guid = this.guid++;

if (!$v.r(handler, 'guid')) $v.s(handler, 'guid', (void 0, x0___ = +$v.r($dis, 'guid'), $v.s($dis, 'guid', x0___ + 1), x0___));

// *** jquery.js ***
//  1837:     if( data != undefined ) {

if (data != $v.ro('undefined')) {

// *** jquery.js ***
//  1839:       var fn = handler;

var fn = handler;

// *** jquery.js ***
//  1842:       handler = this.proxy( fn, function() {

handler = $v.cm($dis, 'proxy', [ fn, $v.dis(___.frozenFunc(function ($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//  1844:         return fn.apply(this, arguments);

return $v.cm(fn, 'apply', [ $dis, Array.slice(a___, 1) ]);
})) ]);
$v.s(handler, 'data', data);
}

// *** jquery.js ***
//  1852:     var events = jQuery.data(elem, "events") || jQuery.data(elem, "events", {}),
//  1853:       handle = jQuery.data(elem, "handle") || jQuery.data(elem, "handle", function(){

var events = $v.cm(jQuery, 'data', [ elem, 'events' ]) || $v.cm(jQuery, 'data', [ elem, 'events', ___.initializeMap([ ]) ]), handle = $v.cm(jQuery, 'data', [ elem, 'handle' ]) || $v.cm(jQuery, 'data', [ elem, 'handle', $v.dis(___.frozenFunc(function ($dis) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//  1856:         if ( typeof jQuery != "undefined" && !jQuery.event.triggered )
//  1857:           return jQuery.event.handle.apply(arguments.callee.elem, arguments);

if ($v.typeOf(jQuery) != 'undefined' && !$v.r($v.r(jQuery, 'event'), 'triggered')) return $v.cm($v.r($v.r(jQuery, 'event'), 'handle'), 'apply', [ $v.r($v.r(Array.slice(a___, 1), 'callee'), 'elem'), Array.slice(a___, 1) ]);
})) ]);
$v.s(handle, 'elem', elem);
$v.cm(jQuery, 'each', [ $v.cm(types, 'split', [ $v.construct(RegExp, [ '\\s+' ]) ]), $v.dis(___.frozenFunc(function ($dis, index, type) {

// *** jquery.js ***
//  1868:       var parts = type.split(".");

var parts = $v.cm(type, 'split', [ '.' ]);

// *** jquery.js ***
//  1869:       type = parts[0];

type = $v.r(parts, 0);
$v.s(handler, 'type', $v.r(parts, 1));

// *** jquery.js ***
//  1873:       var handlers = events[type];

var handlers = $v.r(events, type);

// *** jquery.js ***
//  1876:       if (!handlers) {
//  1878: 
//  1879:         // Check for a special event handler
//  1880:         // Only use addEventListener/attachEvent if the special
//  1881:         // events handler returns false
//  1889:       }

if (!handlers) {

// *** jquery.js ***
//  1877:         handlers = events[type] = {};

handlers = $v.s(events, type, ___.initializeMap([ ]));

// *** jquery.js ***
//  1882:         if ( !jQuery.event.special[type] || jQuery.event.special[type].setup.call(elem) === false ) {
//  1883:           // Bind the global event handler to the element
//  1886:           else if (elem.attachEvent)
//  1887:             elem.attachEvent("on" + type, handle);
//  1888:         }

if (!$v.r($v.r($v.r(jQuery, 'event'), 'special'), type) || $v.cm($v.r($v.r($v.r($v.r(jQuery, 'event'), 'special'), type), 'setup'), 'call', [ elem ]) === false) {

// *** jquery.js ***
//  1884:           if (elem.addEventListener)
//  1885:             elem.addEventListener(type, handle, false);

if ($v.r(elem, 'addEventListener')) $v.cm(elem, 'addEventListener', [ type, handle, false ]);
else if ($v.r(elem, 'attachEvent')) $v.cm(elem, 'attachEvent', [ 'on' + type, handle ]);
}
}
$v.s(handlers, $v.r(handler, 'guid'), handler);
$v.s($v.r($v.r(jQuery, 'event'), 'global'), type, true);

// *** jquery.js ***
//  1895:       jQuery.event.global[type] = true;

})) ]);

// *** jquery.js ***
//  1899:     elem = null;

elem = null;
}
___.func(add$_lit$, 'add$_lit$');
;
var add$_lit = $v.dis(___.primFreeze(add$_lit$), 'add$_lit');
return add$_lit;
}).CALL___(), 'guid', 1, 'global', ___.initializeMap([ ]), 'remove', ___.frozenFunc(function () {

// *** jquery.js ***
//  1906:   remove: function(elem, types, handler) {
//  1907:     // don't do events on text and comment nodes
//  1910: 
//  1912: 
//  1914:       // Unbind all events for the element
//  1918:       else {
//  1919:         // types is actually an event object here
//  1924: 
//  1925:         // Handle multiple events seperated by a space
//  1926:         // jQuery(...).unbind("mouseover mouseout", fn);
//  1927:         jQuery.each(types.split(/\s+/), function(index, type){
//  1928:           // Namespaced event handlers
//  1931: 
//  1957:         });
//  1958:       }
//  1959: 
//  1960:       // Remove the expando if it's no longer used
//  1965:         jQuery.removeData( elem, "events" );
//  1966:         jQuery.removeData( elem, "handle" );
//  1967:       }
//  1968:     }

function remove$_lit$($dis, elem, types, handler) {
var $caja$19;
var $caja$20;
var $caja$25;
var $caja$26;

// *** jquery.js ***
//  1908:     if ( elem.nodeType == 3 || elem.nodeType == 8 )
//  1909:       return;

if ($v.r(elem, 'nodeType') == 3 || $v.r(elem, 'nodeType') == 8) return;

// *** jquery.js ***
//  1911:     var events = jQuery.data(elem, "events"), ret, index;

var events = $v.cm(jQuery, 'data', [ elem, 'events' ]), ret, index;

// *** jquery.js ***
//  1913:     if ( events ) {

if (events) {

// *** jquery.js ***
//  1915:       if ( types == undefined || (typeof types == "string" && types.charAt(0) == ".") )

if (types == $v.ro('undefined') || $v.typeOf(types) == 'string' && $v.cm(types, 'charAt', [ 0 ]) == '.') {
$caja$19 = $v.keys(events);

// *** jquery.js ***
//  1917:           this.remove( elem, type + (types || "") );

for ($caja$20 = 0; $caja$20 < ($caja$19.length_canRead___? $caja$19.length: ___.readPub($caja$19, 'length')); ++$caja$20) {

// *** jquery.js ***
//  1916:         for ( var type in events )

var type = ___.readPub($caja$19, $caja$20);
$v.cm($dis, 'remove', [ elem, type + (types || '') ]);
}
} else {

// *** jquery.js ***
//  1920:         if ( types.type ) {
//  1923:         }

if ($v.r(types, 'type')) {

// *** jquery.js ***
//  1921:           handler = types.handler;

handler = $v.r(types, 'handler');

// *** jquery.js ***
//  1922:           types = types.type;

types = $v.r(types, 'type');
}
$v.cm(jQuery, 'each', [ $v.cm(types, 'split', [ $v.construct(RegExp, [ '\\s+' ]) ]), $v.dis(___.frozenFunc(function ($dis, index, type) {
var $caja$21;
var $caja$22;
var $caja$23;
var $caja$24;

// *** jquery.js ***
//  1929:           var parts = type.split(".");

var parts = $v.cm(type, 'split', [ '.' ]);

// *** jquery.js ***
//  1930:           type = parts[0];

type = $v.r(parts, 0);

// *** jquery.js ***
//  1932:           if ( events[type] ) {
//  1933:             // remove the given handler for the given type
//  1936: 
//  1937:             // remove all handlers for the given type
//  1938:             else
//  1943: 
//  1944:             // remove generic event handler if no more handlers exist
//  1956:           }

if ($v.r(events, type)) {

// *** jquery.js ***
//  1934:             if ( handler )
//  1935:               delete events[type][handler.guid];

if (handler) $v.remove($v.r(events, type), $v.r(handler, 'guid'));
else {
$caja$21 = $v.keys($v.r(events, type));

// *** jquery.js ***
//  1940:                 // Handle the removal of jqnsd events

for ($caja$22 = 0; $caja$22 < ($caja$21.length_canRead___? $caja$21.length: ___.readPub($caja$21, 'length')); ++$caja$22) {

// *** jquery.js ***
//  1939:               for ( handler in events[type] )

handler = ___.readPub($caja$21, $caja$22);

// *** jquery.js ***
//  1941:                 if ( !parts[1] || events[type][handler].type == parts[1] )
//  1942:                   delete events[type][handler];

if (!$v.r(parts, 1) || $v.r($v.r($v.r(events, type), handler), 'type') == $v.r(parts, 1)) $v.remove($v.r(events, type), handler);
}
}
{
$caja$23 = $v.keys($v.r(events, type));
for ($caja$24 = 0; $caja$24 < ($caja$23.length_canRead___? $caja$23.length: ___.readPub($caja$23, 'length')); ++$caja$24) {

// *** jquery.js ***
//  1945:             for ( ret in events[type] ) break;

ret = ___.readPub($caja$23, $caja$24);
break;
}
}

// *** jquery.js ***
//  1946:             if ( !ret ) {
//  1954:               delete events[type];
//  1955:             }

if (!ret) {

// *** jquery.js ***
//  1947:               if ( !jQuery.event.special[type] || jQuery.event.special[type].teardown.call(elem) === false ) {
//  1950:                 else if (elem.detachEvent)
//  1951:                   elem.detachEvent("on" + type, jQuery.data(elem, "handle"));
//  1952:               }

if (!$v.r($v.r($v.r(jQuery, 'event'), 'special'), type) || $v.cm($v.r($v.r($v.r($v.r(jQuery, 'event'), 'special'), type), 'teardown'), 'call', [ elem ]) === false) {

// *** jquery.js ***
//  1948:                 if (elem.removeEventListener)
//  1949:                   elem.removeEventListener(type, jQuery.data(elem, "handle"), false);

if ($v.r(elem, 'removeEventListener')) $v.cm(elem, 'removeEventListener', [ type, $v.cm(jQuery, 'data', [ elem, 'handle' ]), false ]);
else if ($v.r(elem, 'detachEvent')) $v.cm(elem, 'detachEvent', [ 'on' + type, $v.cm(jQuery, 'data', [ elem, 'handle' ]) ]);
}

// *** jquery.js ***
//  1953:               ret = null;

ret = null;
$v.remove(events, type);
}
}
})) ]);
}
{
$caja$25 = $v.keys(events);
for ($caja$26 = 0; $caja$26 < ($caja$25.length_canRead___? $caja$25.length: ___.readPub($caja$25, 'length')); ++$caja$26) {
ret = ___.readPub($caja$25, $caja$26);

// *** jquery.js ***
//  1961:       for ( ret in events ) break;

break;
}
}

// *** jquery.js ***
//  1962:       if ( !ret ) {

if (!ret) {

// *** jquery.js ***
//  1963:         var handle = jQuery.data( elem, "handle" );

var handle = $v.cm(jQuery, 'data', [ elem, 'handle' ]);

// *** jquery.js ***
//  1964:         if ( handle ) handle.elem = null;

if (handle) $v.s(handle, 'elem', null);
$v.cm(jQuery, 'removeData', [ elem, 'events' ]);
$v.cm(jQuery, 'removeData', [ elem, 'handle' ]);
}
}
}
___.func(remove$_lit$, 'remove$_lit$');
;
var remove$_lit = $v.dis(___.primFreeze(remove$_lit$), 'remove$_lit');
return remove$_lit;
}).CALL___(), 'trigger', ___.frozenFunc(function () {

// *** jquery.js ***
//  1971:   trigger: function(type, data, elem, donative, extra) {
//  1972:     // Clone the incoming data, if any
//  1974: 
//  1978:     }
//  1979: 
//  1980:     // Handle a global trigger
//  1982:       // Only trigger if we've ever bound an event for it
//  1985: 
//  1986:     // Handle triggering a single element
//  1988:       // don't do events on text and comment nodes
//  1991: 
//  1993:         // Check to see if we need to provide a fake event, or not
//  1995: 
//  1996:       // Pass along a fake event
//  2007: 
//  2008:       // Enforce the right trigger type
//  2009:       data[0].type = type;
//  2012: 
//  2013:       // Trigger the event, it is assumed that "handle" is a function
//  2017: 
//  2018:       // Handle triggering native .onfoo handlers (and on links since we don't call .click() for links)
//  2021: 
//  2022:       // Extra functions don't get the custom event object
//  2025: 
//  2026:       // Handle triggering of extra function
//  2034: 
//  2035:       // Trigger the native events (except for clicks on links)
//  2043: 
//  2044:       this.triggered = false;
//  2045:     }
//  2046: 

function trigger$_lit$($dis, type, data, elem, donative, extra) {

// *** jquery.js ***
//  1973:     data = jQuery.makeArray(data);

data = $v.cm(jQuery, 'makeArray', [ data ]);

// *** jquery.js ***
//  1975:     if ( type.indexOf("!") >= 0 ) {

if ($v.cm(type, 'indexOf', [ '!' ]) >= 0) {

// *** jquery.js ***
//  1976:       type = type.slice(0, -1);

type = $v.cm(type, 'slice', [ 0, -1 ]);

// *** jquery.js ***
//  1977:       var exclusive = true;

var exclusive = true;
}

// *** jquery.js ***
//  1981:     if ( !elem ) {

if (!elem) {

// *** jquery.js ***
//  1983:       if ( this.global[type] )
//  1984:         jQuery("*").add([window, document]).trigger(type, data);

if ($v.r($v.r($dis, 'global'), type)) $v.cm($v.cm($v.cf(jQuery, [ '*' ]), 'add', [ [ $v.ro('window'), $v.ro('document') ] ]), 'trigger', [ type, data ]);

// *** jquery.js ***
//  1987:     } else {

} else {

// *** jquery.js ***
//  1989:       if ( elem.nodeType == 3 || elem.nodeType == 8 )
//  1990:         return undefined;

if ($v.r(elem, 'nodeType') == 3 || $v.r(elem, 'nodeType') == 8) return $v.ro('undefined');

// *** jquery.js ***
//  1992:       var val, ret, fn = jQuery.isFunction( elem[ type ] || null ),
//  1994:         event = !data[0] || !data[0].preventDefault;

var val, ret, fn = $v.cm(jQuery, 'isFunction', [ $v.r(elem, type) || null ]), event = !$v.r(data, 0) || !$v.r($v.r(data, 0), 'preventDefault');

// *** jquery.js ***
//  1997:       if ( event ) {
//  1998:         data.unshift({
//  1999:           type: type,
//  2000:           target: elem,
//  2001:           preventDefault: function(){},
//  2002:           stopPropagation: function(){},
//  2003:           timeStamp: now()
//  2004:         });
//  2005:         data[0][expando] = true; // no need to fix fake event
//  2006:       }

if (event) {
$v.cm(data, 'unshift', [ ___.initializeMap([ 'type', type, 'target', elem, 'preventDefault', ___.frozenFunc(function () {
function preventDefault$_lit$($dis) {
}
___.func(preventDefault$_lit$, 'preventDefault$_lit$');
;
var preventDefault$_lit = $v.dis(___.primFreeze(preventDefault$_lit$), 'preventDefault$_lit');
return preventDefault$_lit;
}).CALL___(), 'stopPropagation', ___.frozenFunc(function () {
function stopPropagation$_lit$($dis) {
}
___.func(stopPropagation$_lit$, 'stopPropagation$_lit$');
;
var stopPropagation$_lit = $v.dis(___.primFreeze(stopPropagation$_lit$), 'stopPropagation$_lit');
return stopPropagation$_lit;
}).CALL___(), 'timeStamp', $v.cf(now, [ ]) ]) ]);
$v.s($v.r(data, 0), expando, true);
}
$v.s($v.r(data, 0), 'type', type);

// *** jquery.js ***
//  2010:       if ( exclusive )
//  2011:         data[0].exclusive = true;

if (exclusive) $v.s($v.r(data, 0), 'exclusive', true);

// *** jquery.js ***
//  2014:       var handle = jQuery.data(elem, "handle");

var handle = $v.cm(jQuery, 'data', [ elem, 'handle' ]);

// *** jquery.js ***
//  2015:       if ( handle )
//  2016:         val = handle.apply( elem, data );

if (handle) val = $v.cm(handle, 'apply', [ elem, data ]);

// *** jquery.js ***
//  2019:       if ( (!fn || (jQuery.nodeName(elem, 'a') && type == "click")) && elem["on"+type] && elem["on"+type].apply( elem, data ) === false )
//  2020:         val = false;

if ((!fn || $v.cm(jQuery, 'nodeName', [ elem, 'a' ]) && type == 'click') && $v.r(elem, 'on' + type) && $v.cm($v.r(elem, 'on' + type), 'apply', [ elem, data ]) === false) val = false;

// *** jquery.js ***
//  2023:       if ( event )
//  2024:         data.shift();

if (event) $v.cm(data, 'shift', [ ]);

// *** jquery.js ***
//  2027:       if ( extra && jQuery.isFunction( extra ) ) {
//  2028:         // call the extra function and tack the current return value on the end for possible inspection
//  2030:         // if anything is returned, give it precedence and have it overwrite the previous value
//  2033:       }

if (extra && $v.cm(jQuery, 'isFunction', [ extra ])) {

// *** jquery.js ***
//  2029:         ret = extra.apply( elem, val == null ? data : data.concat( val ) );

ret = $v.cm(extra, 'apply', [ elem, val == null ? data: $v.cm(data, 'concat', [ val ]) ]);

// *** jquery.js ***
//  2031:         if (ret !== undefined)
//  2032:           val = ret;

if (ret !== $v.ro('undefined')) val = ret;
}

// *** jquery.js ***
//  2036:       if ( fn && donative !== false && val !== false && !(jQuery.nodeName(elem, 'a') && type == "click") ) {
//  2037:         this.triggered = true;
//  2038:         try {
//  2040:         // prevent IE from throwing an error for some hidden elements
//  2042:       }

if (fn && donative !== false && val !== false && ! ($v.cm(jQuery, 'nodeName', [ elem, 'a' ]) && type == 'click')) {
$v.s($dis, 'triggered', true);

// *** jquery.js ***
//  2039:           elem[ type ]();

try {
$v.cm(elem, type, [ ]);
} catch (ex___) {

// *** jquery.js ***
//  2041:         } catch (e) {}

try {
throw ___.tameException(ex___);
} catch (e) {
}
}
}
$v.s($dis, 'triggered', false);
}

// *** jquery.js ***
//  2047:     return val;

return val;
}
___.func(trigger$_lit$, 'trigger$_lit$');
;
var trigger$_lit = $v.dis(___.primFreeze(trigger$_lit$), 'trigger$_lit');
return trigger$_lit;
}).CALL___(), 'handle', ___.frozenFunc(function () {

// *** jquery.js ***
//  2050:   handle: function(event) {
//  2051:     // returned undefined or false
//  2053: 
//  2055: 
//  2056:     // Namespaced event handlers
//  2058:     event.type = jqns[0];
//  2060:     // Cache this now, all = true means, any handler
//  2062: 
//  2064: 
//  2086: 

function handle$_lit$($dis, event) {
var a___ = ___.args(arguments);
var $caja$27;
var $caja$28;

// *** jquery.js ***
//  2052:     var val, ret, jqns, all, handlers;

var val, ret, jqns, all, handlers;

// *** jquery.js ***
//  2054:     event = arguments[0] = jQuery.event.fix( event || window.event );

event = $v.s(Array.slice(a___, 1), 0, $v.cm($v.r(jQuery, 'event'), 'fix', [ event || $v.r($v.ro('window'), 'event') ]));

// *** jquery.js ***
//  2057:     jqns = event.type.split(".");

jqns = $v.cm($v.r(event, 'type'), 'split', [ '.' ]);
$v.s(event, 'type', $v.r(jqns, 0));

// *** jquery.js ***
//  2059:     jqns = jqns[1];

jqns = $v.r(jqns, 1);

// *** jquery.js ***
//  2061:     all = !jqns && !event.exclusive;

all = !jqns && !$v.r(event, 'exclusive');

// *** jquery.js ***
//  2063:     handlers = ( jQuery.data(this, "events") || {} )[event.type];

handlers = $v.r($v.cm(jQuery, 'data', [ $dis, 'events' ]) || ___.initializeMap([ ]), $v.r(event, 'type'));
{
$caja$27 = $v.keys(handlers);

// *** jquery.js ***
//  2067: 
//  2068:       // Filter the functions by class
//  2085:     }

for ($caja$28 = 0; $caja$28 < ($caja$27.length_canRead___? $caja$27.length: ___.readPub($caja$27, 'length')); ++$caja$28) {

// *** jquery.js ***
//  2065:     for ( var j in handlers ) {

var j = ___.readPub($caja$27, $caja$28);
{

// *** jquery.js ***
//  2066:       var handler = handlers[j];

var handler = $v.r(handlers, j);

// *** jquery.js ***
//  2069:       if ( all || handler.type == jqns ) {
//  2070:         // Pass in a reference to the handler function itself
//  2071:         // So that we can later remove it
//  2072:         event.handler = handler;
//  2073:         event.data = handler.data;
//  2074: 
//  2076: 
//  2079: 
//  2084:       }

if (all || $v.r(handler, 'type') == jqns) {
$v.s(event, 'handler', handler);
$v.s(event, 'data', $v.r(handler, 'data'));

// *** jquery.js ***
//  2075:         ret = handler.apply( this, arguments );

ret = $v.cm(handler, 'apply', [ $dis, Array.slice(a___, 1) ]);

// *** jquery.js ***
//  2077:         if ( val !== false )
//  2078:           val = ret;

if (val !== false) val = ret;

// *** jquery.js ***
//  2080:         if ( ret === false ) {
//  2081:           event.preventDefault();
//  2082:           event.stopPropagation();
//  2083:         }

if (ret === false) {
$v.cm(event, 'preventDefault', [ ]);
$v.cm(event, 'stopPropagation', [ ]);
}
}
}
}
}

// *** jquery.js ***
//  2087:     return val;

return val;
}
___.func(handle$_lit$, 'handle$_lit$');
;
var handle$_lit = $v.dis(___.primFreeze(handle$_lit$), 'handle$_lit');
return handle$_lit;
}).CALL___(), 'fix', ___.frozenFunc(function () {

// *** jquery.js ***
//  2090:   fix: function(event) {
//  2093: 
//  2094:     // store a copy of the original event object
//  2095:     // and "clone" to set read-only properties
//  2101: 
//  2102:     // Mark it as fixed
//  2103:     event[expando] = true;
//  2104: 
//  2105:     // add preventDefault and stopPropagation since
//  2106:     // they will not work on the clone
//  2107:     event.preventDefault = function() {
//  2108:       // if preventDefault exists run it on the original event
//  2113:     };
//  2114:     event.stopPropagation = function() {
//  2115:       // if stopPropagation exists run it on the original event
//  2120:     };
//  2121: 
//  2122:     // Fix timeStamp
//  2123:     event.timeStamp = event.timeStamp || now();
//  2124: 
//  2125:     // Fix target property, if necessary
//  2128: 
//  2129:     // check if target is a textnode (safari)
//  2132: 
//  2133:     // Add relatedTarget, if necessary
//  2136: 
//  2137:     // Calculate pageX/Y if missing and clientX/Y available
//  2140:       event.pageX = event.clientX + (doc && doc.scrollLeft || body && body.scrollLeft || 0) - (doc.clientLeft || 0);
//  2141:       event.pageY = event.clientY + (doc && doc.scrollTop || body && body.scrollTop || 0) - (doc.clientTop || 0);
//  2142:     }
//  2143: 
//  2144:     // Add which for key events
//  2147: 
//  2148:     // Add metaKey to non-Mac browsers (use ctrl for PC's and Meta for Macs)
//  2151: 
//  2152:     // Add which for click: 1 == left; 2 == middle; 3 == right
//  2153:     // Note: button is not normalized, so don't use it
//  2156: 

function fix$_lit$($dis, event) {

// *** jquery.js ***
//  2091:     if ( event[expando] == true )
//  2092:       return event;

if ($v.r(event, expando) == true) return event;

// *** jquery.js ***
//  2096:     var originalEvent = event;

var originalEvent = event;

// *** jquery.js ***
//  2097:     event = { originalEvent: originalEvent };

event = ___.initializeMap([ 'originalEvent', originalEvent ]);

// *** jquery.js ***
//  2098:     var props = "altKey attrChange attrName bubbles button cancelable charCode clientX clientY ctrlKey currentTarget data detail eventPhase fromElement handler keyCode metaKey newValue originalTarget pageX pageY prevValue relatedNode relatedTarget screenX screenY shiftKey srcElement target timeStamp toElement type view wheelDelta which".split(" ");

var props = $v.cm('altKey attrChange attrName bubbles button cancelable charCode clientX clientY ctrlKey currentTarget data detail eventPhase fromElement handler keyCode metaKey newValue originalTarget pageX pageY prevValue relatedNode relatedTarget screenX screenY shiftKey srcElement target timeStamp toElement type view wheelDelta which', 'split', [ ' ' ]);

// *** jquery.js ***
//  2099:     for ( var i=props.length; i; i-- )
//  2100:       event[ props[i] ] = originalEvent[ props[i] ];

for (var i = $v.r(props, 'length'); i; i--) $v.s(event, $v.r(props, i), $v.r(originalEvent, $v.r(props, i)));
$v.s(event, expando, true);
$v.s(event, 'preventDefault', ___.frozenFunc(function () {

// *** jquery.js ***
//  2111:       // otherwise set the returnValue property of the original event to false (IE)
//  2112:       originalEvent.returnValue = false;

function preventDefault$_meth$($dis) {

// *** jquery.js ***
//  2109:       if (originalEvent.preventDefault)
//  2110:         originalEvent.preventDefault();

if ($v.r(originalEvent, 'preventDefault')) $v.cm(originalEvent, 'preventDefault', [ ]);
$v.s(originalEvent, 'returnValue', false);
}
___.func(preventDefault$_meth$, 'preventDefault$_meth$');
;
var preventDefault$_meth = $v.dis(___.primFreeze(preventDefault$_meth$), 'preventDefault$_meth');
return preventDefault$_meth;
}).CALL___());
$v.s(event, 'stopPropagation', ___.frozenFunc(function () {

// *** jquery.js ***
//  2118:       // otherwise set the cancelBubble property of the original event to true (IE)
//  2119:       originalEvent.cancelBubble = true;

function stopPropagation$_meth$($dis) {

// *** jquery.js ***
//  2116:       if (originalEvent.stopPropagation)
//  2117:         originalEvent.stopPropagation();

if ($v.r(originalEvent, 'stopPropagation')) $v.cm(originalEvent, 'stopPropagation', [ ]);
$v.s(originalEvent, 'cancelBubble', true);
}
___.func(stopPropagation$_meth$, 'stopPropagation$_meth$');
;
var stopPropagation$_meth = $v.dis(___.primFreeze(stopPropagation$_meth$), 'stopPropagation$_meth');
return stopPropagation$_meth;
}).CALL___());
$v.s(event, 'timeStamp', $v.r(event, 'timeStamp') || $v.cf(now, [ ]));

// *** jquery.js ***
//  2126:     if ( !event.target )
//  2127:       event.target = event.srcElement || document; // Fixes #1925 where srcElement might not be defined either

if (!$v.r(event, 'target')) $v.s(event, 'target', $v.r(event, 'srcElement') || $v.ro('document'));

// *** jquery.js ***
//  2130:     if ( event.target.nodeType == 3 )
//  2131:       event.target = event.target.parentNode;

if ($v.r($v.r(event, 'target'), 'nodeType') == 3) $v.s(event, 'target', $v.r($v.r(event, 'target'), 'parentNode'));

// *** jquery.js ***
//  2134:     if ( !event.relatedTarget && event.fromElement )
//  2135:       event.relatedTarget = event.fromElement == event.target ? event.toElement : event.fromElement;

if (!$v.r(event, 'relatedTarget') && $v.r(event, 'fromElement')) $v.s(event, 'relatedTarget', $v.r(event, 'fromElement') == $v.r(event, 'target') ? $v.r(event, 'toElement'): $v.r(event, 'fromElement'));

// *** jquery.js ***
//  2138:     if ( event.pageX == null && event.clientX != null ) {

if ($v.r(event, 'pageX') == null && $v.r(event, 'clientX') != null) {

// *** jquery.js ***
//  2139:       var doc = document.documentElement, body = document.body;

var doc = $v.r($v.ro('document'), 'documentElement'), body = $v.r($v.ro('document'), 'body');
$v.s(event, 'pageX', $v.r(event, 'clientX') + (doc && $v.r(doc, 'scrollLeft') || body && $v.r(body, 'scrollLeft') || 0) - ($v.r(doc, 'clientLeft') || 0));
$v.s(event, 'pageY', $v.r(event, 'clientY') + (doc && $v.r(doc, 'scrollTop') || body && $v.r(body, 'scrollTop') || 0) - ($v.r(doc, 'clientTop') || 0));
}

// *** jquery.js ***
//  2145:     if ( !event.which && ((event.charCode || event.charCode === 0) ? event.charCode : event.keyCode) )
//  2146:       event.which = event.charCode || event.keyCode;

if (!$v.r(event, 'which') && ($v.r(event, 'charCode') || $v.r(event, 'charCode') === 0? $v.r(event, 'charCode'): $v.r(event, 'keyCode'))) $v.s(event, 'which', $v.r(event, 'charCode') || $v.r(event, 'keyCode'));

// *** jquery.js ***
//  2149:     if ( !event.metaKey && event.ctrlKey )
//  2150:       event.metaKey = event.ctrlKey;

if (!$v.r(event, 'metaKey') && $v.r(event, 'ctrlKey')) $v.s(event, 'metaKey', $v.r(event, 'ctrlKey'));

// *** jquery.js ***
//  2154:     if ( !event.which && event.button )
//  2155:       event.which = (event.button & 1 ? 1 : ( event.button & 2 ? 3 : ( event.button & 4 ? 2 : 0 ) ));

if (!$v.r(event, 'which') && $v.r(event, 'button')) $v.s(event, 'which', $v.r(event, 'button') & 1? 1: $v.r(event, 'button') & 2? 3: $v.r(event, 'button') & 4? 2: 0);

// *** jquery.js ***
//  2157:     return event;

return event;
}
___.func(fix$_lit$, 'fix$_lit$');
;
var fix$_lit = $v.dis(___.primFreeze(fix$_lit$), 'fix$_lit');
return fix$_lit;
}).CALL___(), 'proxy', ___.frozenFunc(function () {

// *** jquery.js ***
//  2160:   proxy: function( fn, proxy ){
//  2161:     // Set the guid of unique handler to the same of original handler, so it can be removed
//  2162:     proxy.guid = fn.guid = fn.guid || proxy.guid || this.guid++;
//  2163:     // So proxy can be declared as an argument

function proxy$_lit$($dis, fn, proxy) {
var x0___;
$v.s(proxy, 'guid', $v.s(fn, 'guid', $v.r(fn, 'guid') || $v.r(proxy, 'guid') || (void 0, x0___ = +$v.r($dis, 'guid'), $v.s($dis, 'guid', x0___ + 1), x0___)));

// *** jquery.js ***
//  2164:     return proxy;

return proxy;
}
___.func(proxy$_lit$, 'proxy$_lit$');
;
var proxy$_lit = $v.dis(___.primFreeze(proxy$_lit$), 'proxy$_lit');
return proxy$_lit;
}).CALL___(), 'special', ___.initializeMap([ 'ready', ___.initializeMap([ 'setup', ___.frozenFunc(function () {

// *** jquery.js ***
//  2171:         bindReady();
//  2172:         return;

function setup$_lit$($dis) {
$v.cf(bindReady, [ ]);
return;
}
___.func(setup$_lit$, 'setup$_lit$');
;
var setup$_lit = $v.dis(___.primFreeze(setup$_lit$), 'setup$_lit');
return setup$_lit;
}).CALL___(), 'teardown', ___.frozenFunc(function () {

// *** jquery.js ***
//  2175:       teardown: function() { return; }

function teardown$_lit$($dis) {
return;
}
___.func(teardown$_lit$, 'teardown$_lit$');
;
var teardown$_lit = $v.dis(___.primFreeze(teardown$_lit$), 'teardown$_lit');
return teardown$_lit;
}).CALL___() ]), 'mouseenter', ___.initializeMap([ 'setup', ___.frozenFunc(function () {

// *** jquery.js ***
//  2181:         jQuery(this).bind("mouseover", jQuery.event.special.mouseenter.handler);

function setup$_lit$($dis) {

// *** jquery.js ***
//  2180:         if ( jQuery.browser.msie ) return false;

if ($v.r($v.r(jQuery, 'browser'), 'msie')) return false;
$v.cm($v.cf(jQuery, [ $dis ]), 'bind', [ 'mouseover', $v.r($v.r($v.r($v.r(jQuery, 'event'), 'special'), 'mouseenter'), 'handler') ]);

// *** jquery.js ***
//  2182:         return true;

return true;
}
___.func(setup$_lit$, 'setup$_lit$');
;
var setup$_lit = $v.dis(___.primFreeze(setup$_lit$), 'setup$_lit');
return setup$_lit;
}).CALL___(), 'teardown', ___.frozenFunc(function () {

// *** jquery.js ***
//  2187:         jQuery(this).unbind("mouseover", jQuery.event.special.mouseenter.handler);

function teardown$_lit$($dis) {

// *** jquery.js ***
//  2186:         if ( jQuery.browser.msie ) return false;

if ($v.r($v.r(jQuery, 'browser'), 'msie')) return false;
$v.cm($v.cf(jQuery, [ $dis ]), 'unbind', [ 'mouseover', $v.r($v.r($v.r($v.r(jQuery, 'event'), 'special'), 'mouseenter'), 'handler') ]);

// *** jquery.js ***
//  2188:         return true;

return true;
}
___.func(teardown$_lit$, 'teardown$_lit$');
;
var teardown$_lit = $v.dis(___.primFreeze(teardown$_lit$), 'teardown$_lit');
return teardown$_lit;
}).CALL___(), 'handler', ___.frozenFunc(function () {

// *** jquery.js ***
//  2191:       handler: function(event) {
//  2192:         // If we actually just moused on to a sub-element, ignore it
//  2194:         // Execute the right handlers by setting the event type to mouseenter
//  2195:         event.type = "mouseenter";

function handler$_lit$($dis, event) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//  2193:         if ( withinElement(event, this) ) return true;

if ($v.cf(withinElement, [ event, $dis ])) return true;
$v.s(event, 'type', 'mouseenter');

// *** jquery.js ***
//  2196:         return jQuery.event.handle.apply(this, arguments);

return $v.cm($v.r($v.r(jQuery, 'event'), 'handle'), 'apply', [ $dis, Array.slice(a___, 1) ]);
}
___.func(handler$_lit$, 'handler$_lit$');
;
var handler$_lit = $v.dis(___.primFreeze(handler$_lit$), 'handler$_lit');
return handler$_lit;
}).CALL___() ]), 'mouseleave', ___.initializeMap([ 'setup', ___.frozenFunc(function () {

// *** jquery.js ***
//  2203:         jQuery(this).bind("mouseout", jQuery.event.special.mouseleave.handler);

function setup$_lit$($dis) {

// *** jquery.js ***
//  2202:         if ( jQuery.browser.msie ) return false;

if ($v.r($v.r(jQuery, 'browser'), 'msie')) return false;
$v.cm($v.cf(jQuery, [ $dis ]), 'bind', [ 'mouseout', $v.r($v.r($v.r($v.r(jQuery, 'event'), 'special'), 'mouseleave'), 'handler') ]);

// *** jquery.js ***
//  2204:         return true;

return true;
}
___.func(setup$_lit$, 'setup$_lit$');
;
var setup$_lit = $v.dis(___.primFreeze(setup$_lit$), 'setup$_lit');
return setup$_lit;
}).CALL___(), 'teardown', ___.frozenFunc(function () {

// *** jquery.js ***
//  2209:         jQuery(this).unbind("mouseout", jQuery.event.special.mouseleave.handler);

function teardown$_lit$($dis) {

// *** jquery.js ***
//  2208:         if ( jQuery.browser.msie ) return false;

if ($v.r($v.r(jQuery, 'browser'), 'msie')) return false;
$v.cm($v.cf(jQuery, [ $dis ]), 'unbind', [ 'mouseout', $v.r($v.r($v.r($v.r(jQuery, 'event'), 'special'), 'mouseleave'), 'handler') ]);

// *** jquery.js ***
//  2210:         return true;

return true;
}
___.func(teardown$_lit$, 'teardown$_lit$');
;
var teardown$_lit = $v.dis(___.primFreeze(teardown$_lit$), 'teardown$_lit');
return teardown$_lit;
}).CALL___(), 'handler', ___.frozenFunc(function () {

// *** jquery.js ***
//  2213:       handler: function(event) {
//  2214:         // If we actually just moused on to a sub-element, ignore it
//  2216:         // Execute the right handlers by setting the event type to mouseleave
//  2217:         event.type = "mouseleave";

function handler$_lit$($dis, event) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//  2215:         if ( withinElement(event, this) ) return true;

if ($v.cf(withinElement, [ event, $dis ])) return true;
$v.s(event, 'type', 'mouseleave');

// *** jquery.js ***
//  2218:         return jQuery.event.handle.apply(this, arguments);

return $v.cm($v.r($v.r(jQuery, 'event'), 'handle'), 'apply', [ $dis, Array.slice(a___, 1) ]);
}
___.func(handler$_lit$, 'handler$_lit$');
;
var handler$_lit = $v.dis(___.primFreeze(handler$_lit$), 'handler$_lit');
return handler$_lit;
}).CALL___() ]) ]) ]));

// *** jquery.js ***
//  2224: jQuery.fn.extend({

$v.cm($v.r(jQuery, 'fn'), 'extend', [ ___.initializeMap([ 'bind', ___.frozenFunc(function () {

// *** jquery.js ***
//  2225:   bind: function( type, data, fn ) {
//  2228:     });

function bind$_lit$($dis, type, data, fn) {

// *** jquery.js ***
//  2226:     return type == "unload" ? this.one(type, data, fn) : this.each(function(){

return type == 'unload'? $v.cm($dis, 'one', [ type, data, fn ]): $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm($v.r(jQuery, 'event'), 'add', [ $dis, type, fn || data, fn && data ]);

// *** jquery.js ***
//  2227:       jQuery.event.add( this, type, fn || data, fn && data );

})) ]);
}
___.func(bind$_lit$, 'bind$_lit$');
;
var bind$_lit = $v.dis(___.primFreeze(bind$_lit$), 'bind$_lit');
return bind$_lit;
}).CALL___(), 'one', ___.frozenFunc(function () {

// *** jquery.js ***
//  2231:   one: function( type, data, fn ) {
//  2235:     });
//  2238:     });

function one$_lit$($dis, type, data, fn) {

// *** jquery.js ***
//  2232:     var one = jQuery.event.proxy( fn || data, function(event) {
//  2233:       jQuery(this).unbind(event, one);

var one = $v.cm($v.r(jQuery, 'event'), 'proxy', [ fn || data, $v.dis(___.frozenFunc(function ($dis, event) {
var a___ = ___.args(arguments);
$v.cm($v.cf(jQuery, [ $dis ]), 'unbind', [ event, one ]);

// *** jquery.js ***
//  2234:       return (fn || data).apply( this, arguments );

return $v.cm(fn || data, 'apply', [ $dis, Array.slice(a___, 1) ]);
})) ]);

// *** jquery.js ***
//  2236:     return this.each(function(){

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm($v.r(jQuery, 'event'), 'add', [ $dis, type, one, fn && data ]);

// *** jquery.js ***
//  2237:       jQuery.event.add( this, type, one, fn && data);

})) ]);
}
___.func(one$_lit$, 'one$_lit$');
;
var one$_lit = $v.dis(___.primFreeze(one$_lit$), 'one$_lit');
return one$_lit;
}).CALL___(), 'unbind', ___.frozenFunc(function () {

// *** jquery.js ***
//  2241:   unbind: function( type, fn ) {

function unbind$_lit$($dis, type, fn) {

// *** jquery.js ***
//  2242:     return this.each(function(){
//  2244:     });

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm($v.r(jQuery, 'event'), 'remove', [ $dis, type, fn ]);

// *** jquery.js ***
//  2243:       jQuery.event.remove( this, type, fn );

})) ]);
}
___.func(unbind$_lit$, 'unbind$_lit$');
;
var unbind$_lit = $v.dis(___.primFreeze(unbind$_lit$), 'unbind$_lit');
return unbind$_lit;
}).CALL___(), 'trigger', ___.frozenFunc(function () {

// *** jquery.js ***
//  2247:   trigger: function( type, data, fn ) {

function trigger$_lit$($dis, type, data, fn) {

// *** jquery.js ***
//  2248:     return this.each(function(){
//  2250:     });

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm($v.r(jQuery, 'event'), 'trigger', [ type, data, $dis, true, fn ]);

// *** jquery.js ***
//  2249:       jQuery.event.trigger( type, data, this, true, fn );

})) ]);
}
___.func(trigger$_lit$, 'trigger$_lit$');
;
var trigger$_lit = $v.dis(___.primFreeze(trigger$_lit$), 'trigger$_lit');
return trigger$_lit;
}).CALL___(), 'triggerHandler', ___.frozenFunc(function () {

// *** jquery.js ***
//  2253:   triggerHandler: function( type, data, fn ) {

function triggerHandler$_lit$($dis, type, data, fn) {

// *** jquery.js ***
//  2254:     return this[0] && jQuery.event.trigger( type, data, this[0], false, fn );

return $v.r($dis, 0) && $v.cm($v.r(jQuery, 'event'), 'trigger', [ type, data, $v.r($dis, 0), false, fn ]);
}
___.func(triggerHandler$_lit$, 'triggerHandler$_lit$');
;
var triggerHandler$_lit = $v.dis(___.primFreeze(triggerHandler$_lit$), 'triggerHandler$_lit');
return triggerHandler$_lit;
}).CALL___(), 'toggle', ___.frozenFunc(function () {

// *** jquery.js ***
//  2257:   toggle: function( fn ) {
//  2258:     // Save reference to arguments for access in closure
//  2260: 
//  2261:     // link all the functions, so any of them can unbind this click handler
//  2264: 

function toggle$_lit$($dis, fn) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//  2259:     var args = arguments, i = 1;

var args = Array.slice(a___, 1), i = 1;

// *** jquery.js ***
//  2262:     while( i < args.length )
//  2263:       jQuery.event.proxy( fn, args[i++] );

while (i < $v.r(args, 'length')) $v.cm($v.r(jQuery, 'event'), 'proxy', [ fn, $v.r(args, i++) ]);

// *** jquery.js ***
//  2265:     return this.click( jQuery.event.proxy( fn, function(event) {
//  2266:       // Figure out which function to execute
//  2267:       this.lastToggle = ( this.lastToggle || 0 ) % i;
//  2268: 
//  2269:       // Make sure that clicks stop
//  2270:       event.preventDefault();
//  2271: 
//  2272:       // and execute the function
//  2274:     }));

return $v.cm($dis, 'click', [ $v.cm($v.r(jQuery, 'event'), 'proxy', [ fn, $v.dis(___.frozenFunc(function ($dis, event) {
var a___ = ___.args(arguments);
var x0___;
$v.s($dis, 'lastToggle', ($v.r($dis, 'lastToggle') || 0) % i);
$v.cm(event, 'preventDefault', [ ]);

// *** jquery.js ***
//  2273:       return args[ this.lastToggle++ ].apply( this, arguments ) || false;

return $v.cm($v.r(args, (void 0, x0___ = +$v.r($dis, 'lastToggle'), $v.s($dis, 'lastToggle', x0___ + 1), x0___)), 'apply', [ $dis, Array.slice(a___, 1) ]) || false;
})) ]) ]);
}
___.func(toggle$_lit$, 'toggle$_lit$');
;
var toggle$_lit = $v.dis(___.primFreeze(toggle$_lit$), 'toggle$_lit');
return toggle$_lit;
}).CALL___(), 'hover', ___.frozenFunc(function () {

// *** jquery.js ***
//  2277:   hover: function(fnOver, fnOut) {

function hover$_lit$($dis, fnOver, fnOut) {

// *** jquery.js ***
//  2278:     return this.bind('mouseenter', fnOver).bind('mouseleave', fnOut);

return $v.cm($v.cm($dis, 'bind', [ 'mouseenter', fnOver ]), 'bind', [ 'mouseleave', fnOut ]);
}
___.func(hover$_lit$, 'hover$_lit$');
;
var hover$_lit = $v.dis(___.primFreeze(hover$_lit$), 'hover$_lit');
return hover$_lit;
}).CALL___(), 'ready', ___.frozenFunc(function () {

// *** jquery.js ***
//  2281:   ready: function(fn) {
//  2282:     // Attach the listeners
//  2283:     bindReady();
//  2284: 
//  2285:     // If the DOM is already ready
//  2287:       // Execute the function immediately
//  2289: 
//  2290:     // Otherwise, remember the function for later
//  2291:     else
//  2292:       // Add the function to the wait list
//  2294: 
//  2295:     return this;

function ready$_lit$($dis, fn) {
$v.cf(bindReady, [ ]);

// *** jquery.js ***
//  2286:     if ( jQuery.isReady )
//  2288:       fn.call( document, jQuery );

if ($v.r(jQuery, 'isReady')) $v.cm(fn, 'call', [ $v.ro('document'), jQuery ]);
else $v.cm($v.r(jQuery, 'readyList'), 'push', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//  2293:       jQuery.readyList.push( function() { return fn.call(this, jQuery); } );

return $v.cm(fn, 'call', [ $dis, jQuery ]);
})) ]);
return $dis;
}
___.func(ready$_lit$, 'ready$_lit$');
;
var ready$_lit = $v.dis(___.primFreeze(ready$_lit$), 'ready$_lit');
return ready$_lit;
}).CALL___() ]) ]);
$v.cm(jQuery, 'extend', [ ___.initializeMap([ 'isReady', false, 'readyList', [ ], 'ready', ___.frozenFunc(function () {

// *** jquery.js ***
//  2306:       // Remember that the DOM is ready
//  2307:       jQuery.isReady = true;
//  2308: 
//  2309:       // If there are functions bound, to execute
//  2319: 
//  2320:       // Trigger any bound ready events
//  2321:       jQuery(document).triggerHandler("ready");
//  2322:     }

function ready$_lit$($dis) {

// *** jquery.js ***
//  2305:     if ( !jQuery.isReady ) {

if (!$v.r(jQuery, 'isReady')) {
$v.s(jQuery, 'isReady', true);

// *** jquery.js ***
//  2310:       if ( jQuery.readyList ) {
//  2311:         // Execute all of them
//  2312:         jQuery.each( jQuery.readyList, function(){
//  2314:         });
//  2315: 
//  2316:         // Reset the list of functions
//  2317:         jQuery.readyList = null;
//  2318:       }

if ($v.r(jQuery, 'readyList')) {
$v.cm(jQuery, 'each', [ $v.r(jQuery, 'readyList'), $v.dis(___.frozenFunc(function ($dis) {
$v.cm($dis, 'call', [ $v.ro('document') ]);

// *** jquery.js ***
//  2313:           this.call( document );

})) ]);
$v.s(jQuery, 'readyList', null);
}
$v.cm($v.cf(jQuery, [ $v.ro('document') ]), 'triggerHandler', [ 'ready' ]);
}
}
___.func(ready$_lit$, 'ready$_lit$');
;
var ready$_lit = $v.dis(___.primFreeze(ready$_lit$), 'ready$_lit');
return ready$_lit;
}).CALL___() ]) ]);

// *** jquery.js ***
//  2326: var readyBound = false;

var readyBound = false;

// *** jquery.js ***
//  2386: }

;

// *** jquery.js ***
//  2388: jQuery.each( ("blur,focus,load,resize,scroll,unload,click,dblclick," +
//  2389:   "mousedown,mouseup,mousemove,mouseover,mouseout,change,select," +
//  2390:   "submit,keydown,keypress,keyup,error").split(","), function(i, name){

$v.cm(jQuery, 'each', [ $v.cm('blur,focus,load,resize,scroll,unload,click,dblclick,' + 'mousedown,mouseup,mousemove,mouseover,mouseout,change,select,' + 'submit,keydown,keypress,keyup,error', 'split', [ ',' ]), $v.dis(___.frozenFunc(function ($dis, i, name) {

// *** jquery.js ***
//  2393:   jQuery.fn[name] = function(fn){

$v.s($v.r(jQuery, 'fn'), name, $v.dis(___.frozenFunc(function ($dis, fn) {

// *** jquery.js ***
//  2394:     return fn ? this.bind(name, fn) : this.trigger(name);

return fn? $v.cm($dis, 'bind', [ name, fn ]): $v.cm($dis, 'trigger', [ name ]);
})));

// *** jquery.js ***
//  2395:   };

})) ]);

// *** jquery.js ***
//  2407: };

var withinElement = ___.frozenFunc(function () {

// *** jquery.js ***
//  2400: var withinElement = function(event, elem) {
//  2401:   // Check if mouse(over|out) are still within the same parent element
//  2403:   // Traverse up the tree
//  2405:   // Return true if we actually just moused on to a sub-element

function withinElement$_var$($dis, event, elem) {

// *** jquery.js ***
//  2402:   var parent = event.relatedTarget;

var parent = $v.r(event, 'relatedTarget');

// *** jquery.js ***
//  2404:   while ( parent && parent != elem ) try { parent = parent.parentNode; } catch(error) { parent = elem; }

while (parent && parent != elem) try {
parent = $v.r(parent, 'parentNode');
} catch (ex___) {
try {
throw ___.tameException(ex___);
} catch (error) {
parent = elem;
}
}

// *** jquery.js ***
//  2406:   return parent == elem;

return parent == elem;
}
___.func(withinElement$_var$, 'withinElement$_var$');
;
var withinElement$_var = $v.dis(___.primFreeze(withinElement$_var$), 'withinElement$_var');
return withinElement$_var;
}).CALL___();

// *** jquery.js ***
//  2417: jQuery.fn.extend({
//  2419:   _load: jQuery.fn.load,

$v.cm($v.r(jQuery, 'fn'), 'extend', [ ___.initializeMap([ '_load', $v.r($v.r(jQuery, 'fn'), 'load'), 'load', ___.frozenFunc(function () {

// *** jquery.js ***
//  2421:   load: function( url, params, callback ) {
//  2424: 
//  2429:     }
//  2430: 
//  2432: 
//  2433:     // Default to a GET request
//  2435: 
//  2436:     // If the second parameter was provided
//  2438:       // If it's a function
//  2449: 
//  2451: 
//  2452:     // Request the remote document
//  2453:     jQuery.ajax({
//  2454:       url: url,
//  2455:       type: type,
//  2456:       dataType: "html",
//  2457:       data: params,
//  2476:       }
//  2477:     });
//  2478:     return this;

function load$_lit$($dis, url, params, callback) {

// *** jquery.js ***
//  2422:     if ( typeof url != 'string' )
//  2423:       return this._load( url );

if ($v.typeOf(url) != 'string') return $v.cm($dis, '_load', [ url ]);

// *** jquery.js ***
//  2425:     var off = url.indexOf(" ");

var off = $v.cm(url, 'indexOf', [ ' ' ]);

// *** jquery.js ***
//  2426:     if ( off >= 0 ) {

if (off >= 0) {

// *** jquery.js ***
//  2427:       var selector = url.slice(off, url.length);

var selector = $v.cm(url, 'slice', [ off, $v.r(url, 'length') ]);

// *** jquery.js ***
//  2428:       url = url.slice(0, off);

url = $v.cm(url, 'slice', [ 0, off ]);
}

// *** jquery.js ***
//  2431:     callback = callback || function(){};

callback = callback || $v.dis(___.frozenFunc(function ($dis) {
}));

// *** jquery.js ***
//  2434:     var type = "GET";

var type = 'GET';

// *** jquery.js ***
//  2437:     if ( params )
//  2439:       if ( jQuery.isFunction( params ) ) {
//  2440:         // We assume that it's the callback
//  2443: 
//  2444:       // Otherwise, build a param string
//  2445:       } else {
//  2448:       }

if (params) if ($v.cm(jQuery, 'isFunction', [ params ])) {

// *** jquery.js ***
//  2441:         callback = params;

callback = params;

// *** jquery.js ***
//  2442:         params = null;

params = null;
} else {

// *** jquery.js ***
//  2446:         params = jQuery.param( params );

params = $v.cm(jQuery, 'param', [ params ]);

// *** jquery.js ***
//  2447:         type = "POST";

type = 'POST';
}

// *** jquery.js ***
//  2450:     var self = this;

var self = $dis;
$v.cm(jQuery, 'ajax', [ ___.initializeMap([ 'url', url, 'type', type, 'dataType', 'html', 'data', params, 'complete', ___.frozenFunc(function () {

// *** jquery.js ***
//  2458:       complete: function(res, status){
//  2459:         // If successful, inject the HTML into all the matched elements
//  2461:           // See if a selector was specified
//  2463:             // Create a dummy div to hold the results
//  2465:               // inject the contents of the document in, removing the scripts
//  2466:               // to avoid any 'Permission Denied' errors in IE
//  2468: 
//  2469:               // Locate the specified elements
//  2471: 
//  2472:             // If not, just inject the full result
//  2474: 
//  2475:         self.each( callback, [res.responseText, status, res] );

function complete$_lit$($dis, res, status) {

// *** jquery.js ***
//  2460:         if ( status == "success" || status == "notmodified" )
//  2462:           self.html( selector ?
//  2464:             jQuery("<div/>")
//  2467:               .append(res.responseText.replace(/<script(.|\s)*?\/script>/g, ""))
//  2470:               .find(selector) :
//  2473:             res.responseText );

if (status == 'success' || status == 'notmodified') $v.cm(self, 'html', [ selector? $v.cm($v.cm($v.cf(jQuery, [ '\x3cdiv/\x3e' ]), 'append', [ $v.cm($v.r(res, 'responseText'), 'replace', [ $v.construct(RegExp, [ '\x3cscript(.|\\s)*?\\/script\x3e', 'g' ]), '' ]) ]), 'find', [ selector ]): $v.r(res, 'responseText') ]);
$v.cm(self, 'each', [ callback, [ $v.r(res, 'responseText'), status, res ] ]);
}
___.func(complete$_lit$, 'complete$_lit$');
;
var complete$_lit = $v.dis(___.primFreeze(complete$_lit$), 'complete$_lit');
return complete$_lit;
}).CALL___() ]) ]);
return $dis;
}
___.func(load$_lit$, 'load$_lit$');
;
var load$_lit = $v.dis(___.primFreeze(load$_lit$), 'load$_lit');
return load$_lit;
}).CALL___(), 'serialize', ___.frozenFunc(function () {
function serialize$_lit$($dis) {

// *** jquery.js ***
//  2482:     return jQuery.param(this.serializeArray());

return $v.cm(jQuery, 'param', [ $v.cm($dis, 'serializeArray', [ ]) ]);
}
___.func(serialize$_lit$, 'serialize$_lit$');
;
var serialize$_lit = $v.dis(___.primFreeze(serialize$_lit$), 'serialize$_lit');
return serialize$_lit;
}).CALL___(), 'serializeArray', ___.frozenFunc(function () {

// *** jquery.js ***
//  2489:     .filter(function(){
//  2493:     })
//  2494:     .map(function(i, elem){
//  2502:     }).get();

function serializeArray$_lit$($dis) {

// *** jquery.js ***
//  2485:     return this.map(function(){
//  2488:     })

return $v.cm($v.cm($v.cm($v.cm($dis, 'map', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//  2486:       return jQuery.nodeName(this, "form") ?
//  2487:         jQuery.makeArray(this.elements) : this;

return $v.cm(jQuery, 'nodeName', [ $dis, 'form' ]) ? $v.cm(jQuery, 'makeArray', [ $v.r($dis, 'elements') ]): $dis;
})) ]), 'filter', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//  2490:       return this.name && !this.disabled &&
//  2491:         (this.checked || /select|textarea/i.test(this.nodeName) ||
//  2492:           /text|hidden|password/i.test(this.type));

return $v.r($dis, 'name') && !$v.r($dis, 'disabled') && ($v.r($dis, 'checked') || $v.cm($v.construct(RegExp, [ 'select|textarea', 'i' ]), 'test', [ $v.r($dis, 'nodeName') ]) || $v.cm($v.construct(RegExp, [ 'text|hidden|password', 'i' ]), 'test', [ $v.r($dis, 'type') ]));
})) ]), 'map', [ $v.dis(___.frozenFunc(function ($dis, i, elem) {

// *** jquery.js ***
//  2495:       var val = jQuery(this).val();

var val = $v.cm($v.cf(jQuery, [ $dis ]), 'val', [ ]);

// *** jquery.js ***
//  2496:       return val == null ? null :
//  2497:         val.constructor == Array ?
//  2498:           jQuery.map( val, function(val, i){
//  2500:           }) :
//  2501:           {name: elem.name, value: val};

return val == null ? null: $v.r(val, 'constructor') == $v.ro('Array') ? $v.cm(jQuery, 'map', [ val, $v.dis(___.frozenFunc(function ($dis, val, i) {

// *** jquery.js ***
//  2499:             return {name: elem.name, value: val};

return ___.initializeMap([ 'name', $v.r(elem, 'name'), 'value', val ]);
})) ]): ___.initializeMap([ 'name', $v.r(elem, 'name'), 'value', val ]);
})) ]), 'get', [ ]);
}
___.func(serializeArray$_lit$, 'serializeArray$_lit$');
;
var serializeArray$_lit = $v.dis(___.primFreeze(serializeArray$_lit$), 'serializeArray$_lit');
return serializeArray$_lit;
}).CALL___() ]) ]);

// *** jquery.js ***
//  2507: jQuery.each( "ajaxStart,ajaxStop,ajaxComplete,ajaxError,ajaxSuccess,ajaxSend".split(","), function(i,o){

$v.cm(jQuery, 'each', [ $v.cm('ajaxStart,ajaxStop,ajaxComplete,ajaxError,ajaxSuccess,ajaxSend', 'split', [ ',' ]), $v.dis(___.frozenFunc(function ($dis, i, o) {

// *** jquery.js ***
//  2508:   jQuery.fn[o] = function(f){

$v.s($v.r(jQuery, 'fn'), o, $v.dis(___.frozenFunc(function ($dis, f) {

// *** jquery.js ***
//  2509:     return this.bind(o, f);

return $v.cm($dis, 'bind', [ o, f ]);
})));

// *** jquery.js ***
//  2510:   };

})) ]);

// *** jquery.js ***
//  2513: var jsc = now();

var jsc = $v.cf(now, [ ]);
$v.cm(jQuery, 'extend', [ ___.initializeMap([ 'get', ___.frozenFunc(function () {

// *** jquery.js ***
//  2516:   get: function( url, data, callback, type ) {
//  2517:     // shift arguments if data argument was ommited
//  2521:     }
//  2522: 

function get$_lit$($dis, url, data, callback, type) {

// *** jquery.js ***
//  2518:     if ( jQuery.isFunction( data ) ) {

if ($v.cm(jQuery, 'isFunction', [ data ])) {

// *** jquery.js ***
//  2519:       callback = data;

callback = data;

// *** jquery.js ***
//  2520:       data = null;

data = null;
}

// *** jquery.js ***
//  2523:     return jQuery.ajax({
//  2524:       type: "GET",
//  2525:       url: url,
//  2526:       data: data,
//  2527:       success: callback,
//  2528:       dataType: type
//  2529:     });

return $v.cm(jQuery, 'ajax', [ ___.initializeMap([ 'type', 'GET', 'url', url, 'data', data, 'success', callback, 'dataType', type ]) ]);
}
___.func(get$_lit$, 'get$_lit$');
;
var get$_lit = $v.dis(___.primFreeze(get$_lit$), 'get$_lit');
return get$_lit;
}).CALL___(), 'getScript', ___.frozenFunc(function () {

// *** jquery.js ***
//  2532:   getScript: function( url, callback ) {

function getScript$_lit$($dis, url, callback) {

// *** jquery.js ***
//  2533:     return jQuery.get(url, null, callback, "script");

return $v.cm(jQuery, 'get', [ url, null, callback, 'script' ]);
}
___.func(getScript$_lit$, 'getScript$_lit$');
;
var getScript$_lit = $v.dis(___.primFreeze(getScript$_lit$), 'getScript$_lit');
return getScript$_lit;
}).CALL___(), 'getJSON', ___.frozenFunc(function () {

// *** jquery.js ***
//  2536:   getJSON: function( url, data, callback ) {

function getJSON$_lit$($dis, url, data, callback) {

// *** jquery.js ***
//  2537:     return jQuery.get(url, data, callback, "json");

return $v.cm(jQuery, 'get', [ url, data, callback, 'json' ]);
}
___.func(getJSON$_lit$, 'getJSON$_lit$');
;
var getJSON$_lit = $v.dis(___.primFreeze(getJSON$_lit$), 'getJSON$_lit');
return getJSON$_lit;
}).CALL___(), 'post', ___.frozenFunc(function () {

// *** jquery.js ***
//  2540:   post: function( url, data, callback, type ) {
//  2544:     }
//  2545: 

function post$_lit$($dis, url, data, callback, type) {

// *** jquery.js ***
//  2541:     if ( jQuery.isFunction( data ) ) {

if ($v.cm(jQuery, 'isFunction', [ data ])) {

// *** jquery.js ***
//  2542:       callback = data;

callback = data;

// *** jquery.js ***
//  2543:       data = {};

data = ___.initializeMap([ ]);
}

// *** jquery.js ***
//  2546:     return jQuery.ajax({
//  2547:       type: "POST",
//  2548:       url: url,
//  2549:       data: data,
//  2550:       success: callback,
//  2551:       dataType: type
//  2552:     });

return $v.cm(jQuery, 'ajax', [ ___.initializeMap([ 'type', 'POST', 'url', url, 'data', data, 'success', callback, 'dataType', type ]) ]);
}
___.func(post$_lit$, 'post$_lit$');
;
var post$_lit = $v.dis(___.primFreeze(post$_lit$), 'post$_lit');
return post$_lit;
}).CALL___(), 'ajaxSetup', ___.frozenFunc(function () {

// *** jquery.js ***
//  2555:   ajaxSetup: function( settings ) {
//  2556:     jQuery.extend( jQuery.ajaxSettings, settings );

function ajaxSetup$_lit$($dis, settings) {
$v.cm(jQuery, 'extend', [ $v.r(jQuery, 'ajaxSettings'), settings ]);
}
___.func(ajaxSetup$_lit$, 'ajaxSetup$_lit$');
;
var ajaxSetup$_lit = $v.dis(___.primFreeze(ajaxSetup$_lit$), 'ajaxSetup$_lit');
return ajaxSetup$_lit;

// *** jquery.js ***
//  2560:     url: location.href,
//  2564:     contentType: "application/x-www-form-urlencoded",
//  2571:       xml: "application/xml, text/xml",
//  2573:       script: "text/javascript, application/javascript",
//  2574:       json: "application/json, text/javascript",
//  2575:       text: "text/plain",

}).CALL___(), 'ajaxSettings', ___.initializeMap([ 'url', $v.r($v.ro('location'), 'href'), 'global', true, 'type', 'GET', 'timeout', 0, 'contentType', 'application/x-www-form-urlencoded', 'processData', true, 'async', true, 'data', null, 'username', null, 'password', null, 'accepts', ___.initializeMap([ 'xml', 'application/xml, text/xml', 'html', 'text/html', 'script', 'text/javascript, application/javascript', 'json', 'application/json, text/javascript', 'text', 'text/plain', '_default', '*/*' ]) ]), 'lastModified', ___.initializeMap([ ]), 'ajax', ___.frozenFunc(function () {

// *** jquery.js ***
//  2583:   ajax: function( s ) {
//  2584:     // Extend the settings, but re-extend 's' so that it can be
//  2585:     // checked again later (in the test suite, specifically)
//  2587: 
//  2590: 
//  2591:     // convert data if not already a string
//  2594: 
//  2595:     // Handle JSONP Parameter Callbacks
//  2601:         s.data = (s.data ? s.data + "&" : "") + (s.jsonp || "callback") + "=?";
//  2602:       s.dataType = "json";
//  2603:     }
//  2604: 
//  2605:     // Build temporary JSONP function
//  2608: 
//  2609:       // Replace the =? sequence both in the query string and the data
//  2612:       s.url = s.url.replace(jsre, "=" + jsonp + "$1");
//  2613: 
//  2614:       // We need to make sure
//  2615:       // that a JSONP style response is executed properly
//  2616:       s.dataType = "script";
//  2617: 
//  2618:       // Handle JSONP-style loading
//  2619:       window[ jsonp ] = function(tmp){
//  2621:         success();
//  2622:         complete();
//  2623:         // Garbage collect
//  2624:         window[ jsonp ] = undefined;
//  2628:       };
//  2629:     }
//  2630: 
//  2633: 
//  2636:       // try replacing _= if it is there
//  2638:       // if nothing was replaced, add timestamp to the end
//  2639:       s.url = ret + ((ret == s.url) ? (s.url.match(/\?/) ? "&" : "?") + "_=" + ts : "");
//  2640:     }
//  2641: 
//  2642:     // If data is available, append data to url for get requests
//  2644:       s.url += (s.url.match(/\?/) ? "&" : "?") + s.data;
//  2645: 
//  2646:       // IE likes to send both get and post data, prevent this
//  2647:       s.data = null;
//  2648:     }
//  2649: 
//  2650:     // Watch for a new set of requests
//  2653: 
//  2654:     // Matches an absolute URL, and saves the domain
//  2656: 
//  2657:     // If we're requesting a remote document
//  2658:     // and trying to load JSON or Script with a GET
//  2663:       script.src = s.url;
//  2666: 
//  2667:       // Handle Script loading
//  2670: 
//  2671:         // Attach handlers for all browsers
//  2672:         script.onload = script.onreadystatechange = function(){
//  2680:         };
//  2681:       }
//  2682: 
//  2683:       head.appendChild(script);
//  2684: 
//  2685:       // We handle everything using the script element injection
//  2687:     }
//  2688: 
//  2690: 
//  2691:     // Create the request object; Microsoft failed to properly
//  2692:     // implement the XMLHttpRequest in IE7, so we use the ActiveXObject when it is available
//  2694: 
//  2695:     // Open the socket
//  2696:     // Passing null username, generates a login popup on Opera (#2865)
//  2699:     else
//  2700:       xhr.open(type, s.url, s.async);
//  2701: 
//  2702:     // Need an extra try/catch for cross domain requests in Firefox 3
//  2703:     try {
//  2704:       // Set the correct header, if data is being sent
//  2707: 
//  2708:       // Set the If-Modified-Since header, if ifModified mode.
//  2712: 
//  2713:       // Set header so the called script knows that it's an XMLHttpRequest
//  2714:       xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
//  2715: 
//  2716:       // Set the Accepts header for the server, depending on the dataType
//  2717:       xhr.setRequestHeader("Accept", s.dataType && s.accepts[ s.dataType ] ?
//  2718:         s.accepts[ s.dataType ] + ", �/*" :
//  2719:         s.accepts._default );
//  2720:     } catch(e){}
//  2721: 
//  2722:     // Allow custom headers/mimetypes
//  2724:       // cleanup active request counter
//  2725:       s.global && jQuery.active--;
//  2726:       // close opended socket
//  2727:       xhr.abort();
//  2729:     }
//  2730: 
//  2733: 
//  2734:     // Wait for a response to come back
//  2785:     };
//  2786: 
//  2788:       // don't attach the handler to the request, just poll it instead
//  2790: 
//  2791:       // Timeout checker
//  2802:         }, s.timeout);
//  2803:     }
//  2804: 
//  2805:     // Send the data
//  2806:     try {
//  2807:       xhr.send(s.data);
//  2808:     } catch(e) {
//  2809:       jQuery.handleError(s, xhr, null, e);
//  2810:     }
//  2811: 
//  2812:     // firefox 1.5 doesn't fire statechange for sync requests
//  2815: 
//  2817:       // If a local callback was specified, fire it and pass it the data
//  2824:     }
//  2825: 
//  2827:       // Process result
//  2838:     }
//  2839: 
//  2840:     // return XMLHttpRequest to allow aborting the request etc.

function ajax$_lit$($dis, s) {

// *** jquery.js ***
//  2820: 
//  2821:       // Fire the global callback

function success$_caller($dis) {

// *** jquery.js ***
//  2818:       if ( s.success )
//  2819:         s.success( data, status );

if ($v.r(s, 'success')) $v.cm(s, 'success', [ data, status ]);

// *** jquery.js ***
//  2822:       if ( s.global )
//  2823:         jQuery.event.trigger( "ajaxSuccess", [xhr, s] );

if ($v.r(s, 'global')) $v.cm($v.r(jQuery, 'event'), 'trigger', [ 'ajaxSuccess', [ xhr, s ] ]);
}
___.func(success$_caller, 'success$_caller');

// *** jquery.js ***
//  2830: 
//  2831:       // The request was completed
//  2834: 
//  2835:       // Handle the global AJAX counter

function complete$_caller($dis) {

// *** jquery.js ***
//  2828:       if ( s.complete )
//  2829:         s.complete(xhr, status);

if ($v.r(s, 'complete')) $v.cm(s, 'complete', [ xhr, status ]);

// *** jquery.js ***
//  2832:       if ( s.global )
//  2833:         jQuery.event.trigger( "ajaxComplete", [xhr, s] );

if ($v.r(s, 'global')) $v.cm($v.r(jQuery, 'event'), 'trigger', [ 'ajaxComplete', [ xhr, s ] ]);

// *** jquery.js ***
//  2836:       if ( s.global && ! --jQuery.active )
//  2837:         jQuery.event.trigger( "ajaxStop" );

if ($v.r(s, 'global') && !$v.s(jQuery, 'active', $v.r(jQuery, 'active') - 1)) $v.cm($v.r(jQuery, 'event'), 'trigger', [ 'ajaxStop' ]);
}
___.func(complete$_caller, 'complete$_caller');
var x0___;
var x1___;
var success;
;

// *** jquery.js ***
//  2816:     function success(){

success = $v.dis(___.primFreeze(success$_caller), 'success');
var complete;
;

// *** jquery.js ***
//  2826:     function complete(){

complete = $v.dis(___.primFreeze(complete$_caller), 'complete');

// *** jquery.js ***
//  2586:     s = jQuery.extend(true, s, jQuery.extend(true, {}, jQuery.ajaxSettings, s));

s = $v.cm(jQuery, 'extend', [ true, s, $v.cm(jQuery, 'extend', [ true, ___.initializeMap([ ]), $v.r(jQuery, 'ajaxSettings'), s ]) ]);

// *** jquery.js ***
//  2588:     var jsonp, jsre = /=\?(&|$)/g, status, data,
//  2589:       type = s.type.toUpperCase();

var jsonp, jsre = $v.construct(RegExp, [ '=\\?(\x26|$)', 'g' ]), status, data, type = $v.cm($v.r(s, 'type'), 'toUpperCase', [ ]);

// *** jquery.js ***
//  2592:     if ( s.data && s.processData && typeof s.data != "string" )
//  2593:       s.data = jQuery.param(s.data);

if ($v.r(s, 'data') && $v.r(s, 'processData') && $v.typeOf($v.r(s, 'data')) != 'string') $v.s(s, 'data', $v.cm(jQuery, 'param', [ $v.r(s, 'data') ]));

// *** jquery.js ***
//  2596:     if ( s.dataType == "jsonp" ) {

if ($v.r(s, 'dataType') == 'jsonp') {

// *** jquery.js ***
//  2597:       if ( type == "GET" ) {

if (type == 'GET') {

// *** jquery.js ***
//  2598:         if ( !s.url.match(jsre) )
//  2599:           s.url += (s.url.match(/\?/) ? "&" : "?") + (s.jsonp || "callback") + "=?";

if (!$v.cm($v.r(s, 'url'), 'match', [ jsre ])) $v.s(s, 'url', $v.r(s, 'url') + (($v.cm($v.r(s, 'url'), 'match', [ $v.construct(RegExp, [ '\\?' ]) ]) ? '\x26': '?') + ($v.r(s, 'jsonp') || 'callback') + '=?'));

// *** jquery.js ***
//  2600:       } else if ( !s.data || !s.data.match(jsre) )

} else if (!$v.r(s, 'data') || !$v.cm($v.r(s, 'data'), 'match', [ jsre ])) $v.s(s, 'data', ($v.r(s, 'data') ? $v.r(s, 'data') + '\x26': '') + ($v.r(s, 'jsonp') || 'callback') + '=?');
$v.s(s, 'dataType', 'json');
}

// *** jquery.js ***
//  2606:     if ( s.dataType == "json" && (s.data && s.data.match(jsre) || s.url.match(jsre)) ) {

if ($v.r(s, 'dataType') == 'json' && ($v.r(s, 'data') && $v.cm($v.r(s, 'data'), 'match', [ jsre ]) || $v.cm($v.r(s, 'url'), 'match', [ jsre ]))) {

// *** jquery.js ***
//  2607:       jsonp = "jsonp" + jsc++;

jsonp = 'jsonp' + jsc++;

// *** jquery.js ***
//  2610:       if ( s.data )
//  2611:         s.data = (s.data + "").replace(jsre, "=" + jsonp + "$1");

if ($v.r(s, 'data')) $v.s(s, 'data', $v.cm($v.r(s, 'data') + '', 'replace', [ jsre, '=' + jsonp + '$1' ]));
$v.s(s, 'url', $v.cm($v.r(s, 'url'), 'replace', [ jsre, '=' + jsonp + '$1' ]));
$v.s(s, 'dataType', 'script');
$v.s($v.ro('window'), jsonp, $v.dis(___.frozenFunc(function ($dis, tmp) {

// *** jquery.js ***
//  2620:         data = tmp;

data = tmp;
$v.cf(success, [ ]);
$v.cf(complete, [ ]);
$v.s($v.ro('window'), jsonp, $v.ro('undefined'));
try {
$v.remove($v.ro('window'), jsonp);

// *** jquery.js ***
//  2625:         try{ delete window[ jsonp ]; } catch(e){}

} catch (ex___) {
try {
throw ___.tameException(ex___);
} catch (e) {
}
}

// *** jquery.js ***
//  2626:         if ( head )
//  2627:           head.removeChild( script );

if (head) $v.cm(head, 'removeChild', [ script ]);
})));
}

// *** jquery.js ***
//  2631:     if ( s.dataType == "script" && s.cache == null )
//  2632:       s.cache = false;

if ($v.r(s, 'dataType') == 'script' && $v.r(s, 'cache') == null) $v.s(s, 'cache', false);

// *** jquery.js ***
//  2634:     if ( s.cache === false && type == "GET" ) {

if ($v.r(s, 'cache') === false && type == 'GET') {

// *** jquery.js ***
//  2635:       var ts = now();

var ts = $v.cf(now, [ ]);

// *** jquery.js ***
//  2637:       var ret = s.url.replace(/(\?|&)_=.*?(&|$)/, "$1_=" + ts + "$2");

var ret = $v.cm($v.r(s, 'url'), 'replace', [ $v.construct(RegExp, [ '(\\?|\x26)_=.*?(\x26|$)' ]), '$1_=' + ts + '$2' ]);
$v.s(s, 'url', ret + (ret == $v.r(s, 'url') ? ($v.cm($v.r(s, 'url'), 'match', [ $v.construct(RegExp, [ '\\?' ]) ]) ? '\x26': '?') + '_=' + ts: ''));
}

// *** jquery.js ***
//  2643:     if ( s.data && type == "GET" ) {

if ($v.r(s, 'data') && type == 'GET') {
$v.s(s, 'url', $v.r(s, 'url') + (($v.cm($v.r(s, 'url'), 'match', [ $v.construct(RegExp, [ '\\?' ]) ]) ? '\x26': '?') + $v.r(s, 'data')));
$v.s(s, 'data', null);
}

// *** jquery.js ***
//  2651:     if ( s.global && ! jQuery.active++ )
//  2652:       jQuery.event.trigger( "ajaxStart" );

if ($v.r(s, 'global') && ! (void 0, x0___ = +$v.r(jQuery, 'active'), $v.s(jQuery, 'active', x0___ + 1), x0___)) $v.cm($v.r(jQuery, 'event'), 'trigger', [ 'ajaxStart' ]);

// *** jquery.js ***
//  2655:     var remote = /^(?:\w+:)?\/\/([^\/?#]+)/;

var remote = $v.construct(RegExp, [ '^(?:\\w+:)?\\/\\/([^\\/?#]+)' ]);

// *** jquery.js ***
//  2659:     if ( s.dataType == "script" && type == "GET"
//  2660:         && remote.test(s.url) && remote.exec(s.url)[1] != location.host ){

if ($v.r(s, 'dataType') == 'script' && type == 'GET' && $v.cm(remote, 'test', [ $v.r(s, 'url') ]) && $v.r($v.cm(remote, 'exec', [ $v.r(s, 'url') ]), 1) != $v.r($v.ro('location'), 'host')) {

// *** jquery.js ***
//  2661:       var head = document.getElementsByTagName("head")[0];

var head = $v.r($v.cm($v.ro('document'), 'getElementsByTagName', [ 'head' ]), 0);

// *** jquery.js ***
//  2662:       var script = document.createElement("script");

var script = $v.cm($v.ro('document'), 'createElement', [ 'script' ]);
$v.s(script, 'src', $v.r(s, 'url'));

// *** jquery.js ***
//  2664:       if (s.scriptCharset)
//  2665:         script.charset = s.scriptCharset;

if ($v.r(s, 'scriptCharset')) $v.s(script, 'charset', $v.r(s, 'scriptCharset'));

// *** jquery.js ***
//  2668:       if ( !jsonp ) {

if (!jsonp) {

// *** jquery.js ***
//  2669:         var done = false;

var done = false;
$v.s(script, 'onload', $v.s(script, 'onreadystatechange', ___.frozenFunc(function () {

// *** jquery.js ***
//  2676:             success();
//  2677:             complete();
//  2678:             head.removeChild( script );
//  2679:           }

function onreadystatechange$_meth$($dis) {

// *** jquery.js ***
//  2673:           if ( !done && (!this.readyState ||
//  2674:               this.readyState == "loaded" || this.readyState == "complete") ) {

if (!done && (!$v.r($dis, 'readyState') || $v.r($dis, 'readyState') == 'loaded' || $v.r($dis, 'readyState') == 'complete')) {

// *** jquery.js ***
//  2675:             done = true;

done = true;
$v.cf(success, [ ]);
$v.cf(complete, [ ]);
$v.cm(head, 'removeChild', [ script ]);
}
}
___.func(onreadystatechange$_meth$, 'onreadystatechange$_meth$');
;
var onreadystatechange$_meth = $v.dis(___.primFreeze(onreadystatechange$_meth$), 'onreadystatechange$_meth');
return onreadystatechange$_meth;
}).CALL___()));
}
$v.cm(head, 'appendChild', [ script ]);

// *** jquery.js ***
//  2686:       return undefined;

return $v.ro('undefined');
}

// *** jquery.js ***
//  2689:     var requestDone = false;

var requestDone = false;

// *** jquery.js ***
//  2693:     var xhr = window.ActiveXObject ? new ActiveXObject("Microsoft.XMLHTTP") : new XMLHttpRequest();

var xhr = $v.r($v.ro('window'), 'ActiveXObject') ? $v.construct($v.ro('ActiveXObject'), [ 'Microsoft.XMLHTTP' ]): $v.construct($v.ro('XMLHttpRequest'), [ ]);

// *** jquery.js ***
//  2697:     if( s.username )
//  2698:       xhr.open(type, s.url, s.async, s.username, s.password);

if ($v.r(s, 'username')) $v.cm(xhr, 'open', [ type, $v.r(s, 'url'), $v.r(s, 'async'), $v.r(s, 'username'), $v.r(s, 'password') ]);
else $v.cm(xhr, 'open', [ type, $v.r(s, 'url'), $v.r(s, 'async') ]);
try {

// *** jquery.js ***
//  2705:       if ( s.data )
//  2706:         xhr.setRequestHeader("Content-Type", s.contentType);

if ($v.r(s, 'data')) $v.cm(xhr, 'setRequestHeader', [ 'Content-Type', $v.r(s, 'contentType') ]);

// *** jquery.js ***
//  2709:       if ( s.ifModified )
//  2710:         xhr.setRequestHeader("If-Modified-Since",
//  2711:           jQuery.lastModified[s.url] || "Thu, 01 Jan 1970 00:00:00 GMT" );

if ($v.r(s, 'ifModified')) $v.cm(xhr, 'setRequestHeader', [ 'If-Modified-Since', $v.r($v.r(jQuery, 'lastModified'), $v.r(s, 'url')) || 'Thu, 01 Jan 1970 00:00:00 GMT' ]);
$v.cm(xhr, 'setRequestHeader', [ 'X-Requested-With', 'XMLHttpRequest' ]);
$v.cm(xhr, 'setRequestHeader', [ 'Accept', $v.r(s, 'dataType') && $v.r($v.r(s, 'accepts'), $v.r(s, 'dataType')) ? $v.r($v.r(s, 'accepts'), $v.r(s, 'dataType')) + ', */*': $v.r($v.r(s, 'accepts'), '_default') ]);
} catch (ex___) {
try {
throw ___.tameException(ex___);
} catch (e) {
}
}

// *** jquery.js ***
//  2723:     if ( s.beforeSend && s.beforeSend(xhr, s) === false ) {

if ($v.r(s, 'beforeSend') && $v.cm(s, 'beforeSend', [ xhr, s ]) === false) {
$v.r(s, 'global') && (void 0, x1___ = +$v.r(jQuery, 'active'), $v.s(jQuery, 'active', x1___ - 1), x1___);
$v.cm(xhr, 'abort', [ ]);

// *** jquery.js ***
//  2728:       return false;

return false;
}

// *** jquery.js ***
//  2731:     if ( s.global )
//  2732:       jQuery.event.trigger("ajaxSend", [xhr, s]);

if ($v.r(s, 'global')) $v.cm($v.r(jQuery, 'event'), 'trigger', [ 'ajaxSend', [ xhr, s ] ]);
var onreadystatechange = ___.frozenFunc(function () {

// *** jquery.js ***
//  2735:     var onreadystatechange = function(isTimeout){
//  2736:       // The transfer is complete and the data is available, or the request timed out
//  2739: 
//  2740:         // clear poll interval
//  2742:           clearInterval(ival);
//  2744:         }
//  2745: 
//  2747:           !jQuery.httpSuccess( xhr ) && "error" ||
//  2748:           s.ifModified && jQuery.httpNotModified( xhr, s.url ) && "notmodified" ||
//  2749:           "success";
//  2750: 
//  2760: 
//  2761:         // Make sure that the request was successful or notmodified
//  2776:           jQuery.handleError(s, xhr, status);
//  2777: 
//  2778:         // Fire the complete handlers
//  2779:         complete();
//  2780: 
//  2781:         // Stop memory leaks
//  2784:       }

function onreadystatechange$_var$($dis, isTimeout) {

// *** jquery.js ***
//  2737:       if ( !requestDone && xhr && (xhr.readyState == 4 || isTimeout == "timeout") ) {

if (!requestDone && xhr && ($v.r(xhr, 'readyState') == 4 || isTimeout == 'timeout')) {

// *** jquery.js ***
//  2738:         requestDone = true;

requestDone = true;

// *** jquery.js ***
//  2741:         if (ival) {

if (ival) {
$v.cf($v.ro('clearInterval'), [ ival ]);

// *** jquery.js ***
//  2743:           ival = null;

ival = null;
}

// *** jquery.js ***
//  2746:         status = isTimeout == "timeout" && "timeout" ||

status = isTimeout == 'timeout' && 'timeout' || !$v.cm(jQuery, 'httpSuccess', [ xhr ]) && 'error' || $v.r(s, 'ifModified') && $v.cm(jQuery, 'httpNotModified', [ xhr, $v.r(s, 'url') ]) && 'notmodified' || 'success';

// *** jquery.js ***
//  2751:         if ( status == "success" ) {
//  2752:           // Watch for, and catch, XML document parse errors
//  2753:           try {
//  2754:             // process the data (runs the xml through httpData regardless of callback)
//  2758:           }
//  2759:         }

if (status == 'success') {
try {

// *** jquery.js ***
//  2755:             data = jQuery.httpData( xhr, s.dataType, s.dataFilter );

data = $v.cm(jQuery, 'httpData', [ xhr, $v.r(s, 'dataType'), $v.r(s, 'dataFilter') ]);
} catch (ex___) {

// *** jquery.js ***
//  2756:           } catch(e) {

try {
throw ___.tameException(ex___);
} catch (e) {

// *** jquery.js ***
//  2757:             status = "parsererror";

status = 'parsererror';
}
}
}

// *** jquery.js ***
//  2762:         if ( status == "success" ) {
//  2763:           // Cache Last-Modified header, if ifModified mode.
//  2765:           try {
//  2768: 
//  2771: 
//  2772:           // JSONP handles its own success callback

if (status == 'success') {

// *** jquery.js ***
//  2764:           var modRes;

var modRes;
try {

// *** jquery.js ***
//  2766:             modRes = xhr.getResponseHeader("Last-Modified");

modRes = $v.cm(xhr, 'getResponseHeader', [ 'Last-Modified' ]);
} catch (ex___) {

// *** jquery.js ***
//  2767:           } catch(e) {} // swallow exception thrown by FF if header is not available

try {
throw ___.tameException(ex___);
} catch (e) {
}
}

// *** jquery.js ***
//  2769:           if ( s.ifModified && modRes )
//  2770:             jQuery.lastModified[s.url] = modRes;

if ($v.r(s, 'ifModified') && modRes) $v.s($v.r(jQuery, 'lastModified'), $v.r(s, 'url'), modRes);

// *** jquery.js ***
//  2773:           if ( !jsonp )
//  2774:             success();

if (!jsonp) $v.cf(success, [ ]);

// *** jquery.js ***
//  2775:         } else

} else $v.cm(jQuery, 'handleError', [ s, xhr, status ]);
$v.cf(complete, [ ]);

// *** jquery.js ***
//  2782:         if ( s.async )
//  2783:           xhr = null;

if ($v.r(s, 'async')) xhr = null;
}
}
___.func(onreadystatechange$_var$, 'onreadystatechange$_var$');
;
var onreadystatechange$_var = $v.dis(___.primFreeze(onreadystatechange$_var$), 'onreadystatechange$_var');
return onreadystatechange$_var;
}).CALL___();

// *** jquery.js ***
//  2787:     if ( s.async ) {

if ($v.r(s, 'async')) {

// *** jquery.js ***
//  2789:       var ival = setInterval(onreadystatechange, 13);

var ival = $v.cf($v.ro('setInterval'), [ onreadystatechange, 13 ]);

// *** jquery.js ***
//  2792:       if ( s.timeout > 0 )
//  2793:         setTimeout(function(){
//  2794:           // Check to see if the request is still happening

if ($v.r(s, 'timeout') > 0) $v.cf($v.ro('setTimeout'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//  2795:           if ( xhr ) {
//  2796:             // Cancel the request
//  2797:             xhr.abort();
//  2798: 
//  2801:           }

if (xhr) {
$v.cm(xhr, 'abort', [ ]);

// *** jquery.js ***
//  2799:             if( !requestDone )
//  2800:               onreadystatechange( "timeout" );

if (!requestDone) $v.cf(onreadystatechange, [ 'timeout' ]);
}
})), $v.r(s, 'timeout') ]);
}
try {
$v.cm(xhr, 'send', [ $v.r(s, 'data') ]);
} catch (ex___) {
try {
throw ___.tameException(ex___);
} catch (e) {
$v.cm(jQuery, 'handleError', [ s, xhr, null, e ]);
}
}

// *** jquery.js ***
//  2813:     if ( !s.async )
//  2814:       onreadystatechange();

if (!$v.r(s, 'async')) $v.cf(onreadystatechange, [ ]);
;
;

// *** jquery.js ***
//  2841:     return xhr;

return xhr;
}
___.func(ajax$_lit$, 'ajax$_lit$');
;
var ajax$_lit = $v.dis(___.primFreeze(ajax$_lit$), 'ajax$_lit');
return ajax$_lit;
}).CALL___(), 'handleError', ___.frozenFunc(function () {

// *** jquery.js ***
//  2844:   handleError: function( s, xhr, status, e ) {
//  2845:     // If a local callback was specified, fire it
//  2847: 
//  2848:     // Fire the global callback

function handleError$_lit$($dis, s, xhr, status, e) {

// *** jquery.js ***
//  2846:     if ( s.error ) s.error( xhr, status, e );

if ($v.r(s, 'error')) $v.cm(s, 'error', [ xhr, status, e ]);

// *** jquery.js ***
//  2849:     if ( s.global )
//  2850:       jQuery.event.trigger( "ajaxError", [xhr, s, e] );

if ($v.r(s, 'global')) $v.cm($v.r(jQuery, 'event'), 'trigger', [ 'ajaxError', [ xhr, s, e ] ]);
}
___.func(handleError$_lit$, 'handleError$_lit$');
;
var handleError$_lit = $v.dis(___.primFreeze(handleError$_lit$), 'handleError$_lit');
return handleError$_lit;
}).CALL___(), 'active', 0, 'httpSuccess', ___.frozenFunc(function () {

// *** jquery.js ***
//  2857:   httpSuccess: function( xhr ) {
//  2858:     try {
//  2859:       // IE error sometimes returns 1223 when it should be 204 so treat it as success, see #1450
//  2863:     } catch(e){}

function httpSuccess$_lit$($dis, xhr) {
try {

// *** jquery.js ***
//  2860:       return !xhr.status && location.protocol == "file:" ||
//  2861:         ( xhr.status >= 200 && xhr.status < 300 ) || xhr.status == 304 || xhr.status == 1223 ||
//  2862:         jQuery.browser.safari && xhr.status == undefined;

return !$v.r(xhr, 'status') && $v.r($v.ro('location'), 'protocol') == 'file:' || $v.r(xhr, 'status') >= 200 && $v.r(xhr, 'status') < 300 || $v.r(xhr, 'status') == 304 || $v.r(xhr, 'status') == 1223 || $v.r($v.r(jQuery, 'browser'), 'safari') && $v.r(xhr, 'status') == $v.ro('undefined');
} catch (ex___) {
try {
throw ___.tameException(ex___);
} catch (e) {
}
}

// *** jquery.js ***
//  2864:     return false;

return false;
}
___.func(httpSuccess$_lit$, 'httpSuccess$_lit$');
;
var httpSuccess$_lit = $v.dis(___.primFreeze(httpSuccess$_lit$), 'httpSuccess$_lit');
return httpSuccess$_lit;
}).CALL___(), 'httpNotModified', ___.frozenFunc(function () {

// *** jquery.js ***
//  2868:   httpNotModified: function( xhr, url ) {
//  2869:     try {
//  2871: 
//  2872:       // Firefox always returns 200. check Last-Modified date
//  2875:     } catch(e){}

function httpNotModified$_lit$($dis, xhr, url) {
try {

// *** jquery.js ***
//  2870:       var xhrRes = xhr.getResponseHeader("Last-Modified");

var xhrRes = $v.cm(xhr, 'getResponseHeader', [ 'Last-Modified' ]);

// *** jquery.js ***
//  2873:       return xhr.status == 304 || xhrRes == jQuery.lastModified[url] ||
//  2874:         jQuery.browser.safari && xhr.status == undefined;

return $v.r(xhr, 'status') == 304 || xhrRes == $v.r($v.r(jQuery, 'lastModified'), url) || $v.r($v.r(jQuery, 'browser'), 'safari') && $v.r(xhr, 'status') == $v.ro('undefined');
} catch (ex___) {
try {
throw ___.tameException(ex___);
} catch (e) {
}
}

// *** jquery.js ***
//  2876:     return false;

return false;
}
___.func(httpNotModified$_lit$, 'httpNotModified$_lit$');
;
var httpNotModified$_lit = $v.dis(___.primFreeze(httpNotModified$_lit$), 'httpNotModified$_lit');
return httpNotModified$_lit;
}).CALL___(), 'httpData', ___.frozenFunc(function () {

// *** jquery.js ***
//  2879:   httpData: function( xhr, type, filter ) {
//  2883: 
//  2886:       
//  2887:     // Allow a pre-filtering function to sanitize the response
//  2890: 
//  2891:     // If the type is "script", eval it in global context
//  2894: 
//  2895:     // Get the JavaScript object, if JSON is used.
//  2898: 

function httpData$_lit$($dis, xhr, type, filter) {

// *** jquery.js ***
//  2880:     var ct = xhr.getResponseHeader("content-type"),
//  2881:       xml = type == "xml" || !type && ct && ct.indexOf("xml") >= 0,
//  2882:       data = xml ? xhr.responseXML : xhr.responseText;

var ct = $v.cm(xhr, 'getResponseHeader', [ 'content-type' ]), xml = type == 'xml' || !type && ct && $v.cm(ct, 'indexOf', [ 'xml' ]) >= 0, data = xml? $v.r(xhr, 'responseXML'): $v.r(xhr, 'responseText');

// *** jquery.js ***
//  2884:     if ( xml && data.documentElement.tagName == "parsererror" )
//  2885:       throw "parsererror";

if (xml && $v.r($v.r(data, 'documentElement'), 'tagName') == 'parsererror') throw 'parsererror';

// *** jquery.js ***
//  2888:     if( filter )
//  2889:       data = filter( data, type );

if (filter) data = $v.cf(filter, [ data, type ]);

// *** jquery.js ***
//  2892:     if ( type == "script" )
//  2893:       jQuery.globalEval( data );

if (type == 'script') $v.cm(jQuery, 'globalEval', [ data ]);

// *** jquery.js ***
//  2896:     if ( type == "json" )
//  2897:       data = eval("(" + data + ")");

if (type == 'json') data = $v.cf($v.ro('eval'), [ '(' + data + ')' ]);

// *** jquery.js ***
//  2899:     return data;

return data;
}
___.func(httpData$_lit$, 'httpData$_lit$');
;
var httpData$_lit = $v.dis(___.primFreeze(httpData$_lit$), 'httpData$_lit');
return httpData$_lit;
}).CALL___(), 'param', ___.frozenFunc(function () {

// *** jquery.js ***
//  2904:   param: function( a ) {
//  2906: 
//  2907:     // If an array was passed in, assume that it is an array
//  2908:     // of form elements
//  2910:       // Serialize the form elements
//  2913:       });
//  2914: 
//  2915:     // Otherwise, assume that it's an object of key/value pairs
//  2916:     else
//  2917:       // Serialize the key/values
//  2926: 
//  2927:     // Return the resulting serialization

function param$_lit$($dis, a) {
var $caja$29;
var $caja$30;

// *** jquery.js ***
//  2905:     var s = [];

var s = [ ];

// *** jquery.js ***
//  2909:     if ( a.constructor == Array || a.jquery )
//  2911:       jQuery.each( a, function(){

if ($v.r(a, 'constructor') == $v.ro('Array') || $v.r(a, 'jquery')) $v.cm(jQuery, 'each', [ a, $v.dis(___.frozenFunc(function ($dis) {
$v.cm(s, 'push', [ $v.cf($v.ro('encodeURIComponent'), [ $v.r($dis, 'name') ]) + '=' + $v.cf($v.ro('encodeURIComponent'), [ $v.r($dis, 'value') ]) ]);

// *** jquery.js ***
//  2912:         s.push( encodeURIComponent(this.name) + "=" + encodeURIComponent( this.value ) );

})) ]);
else {
$caja$29 = $v.keys(a);

// *** jquery.js ***
//  2919:         // If the value is an array then the key names need to be repeated
//  2924:         else
//  2925:           s.push( encodeURIComponent(j) + "=" + encodeURIComponent( jQuery.isFunction(a[j]) ? a[j]() : a[j] ) );

for ($caja$30 = 0; $caja$30 < ($caja$29.length_canRead___? $caja$29.length: ___.readPub($caja$29, 'length')); ++$caja$30) {

// *** jquery.js ***
//  2918:       for ( var j in a )

var j = ___.readPub($caja$29, $caja$30);

// *** jquery.js ***
//  2920:         if ( a[j] && a[j].constructor == Array )
//  2921:           jQuery.each( a[j], function(){
//  2923:           });

if ($v.r(a, j) && $v.r($v.r(a, j), 'constructor') == $v.ro('Array')) $v.cm(jQuery, 'each', [ $v.r(a, j), $v.dis(___.frozenFunc(function ($dis) {
$v.cm(s, 'push', [ $v.cf($v.ro('encodeURIComponent'), [ j ]) + '=' + $v.cf($v.ro('encodeURIComponent'), [ $dis ]) ]);

// *** jquery.js ***
//  2922:             s.push( encodeURIComponent(j) + "=" + encodeURIComponent( this ) );

})) ]);
else $v.cm(s, 'push', [ $v.cf($v.ro('encodeURIComponent'), [ j ]) + '=' + $v.cf($v.ro('encodeURIComponent'), [ $v.cm(jQuery, 'isFunction', [ $v.r(a, j) ]) ? $v.cm(a, j, [ ]): $v.r(a, j) ]) ]);
}
}

// *** jquery.js ***
//  2928:     return s.join("&").replace(/%20/g, "+");

return $v.cm($v.cm(s, 'join', [ '\x26' ]), 'replace', [ $v.construct(RegExp, [ '%20', 'g' ]), '+' ]);
}
___.func(param$_lit$, 'param$_lit$');
;
var param$_lit = $v.dis(___.primFreeze(param$_lit$), 'param$_lit');
return param$_lit;
}).CALL___() ]) ]);

// *** jquery.js ***
//  2932: jQuery.fn.extend({

$v.cm($v.r(jQuery, 'fn'), 'extend', [ ___.initializeMap([ 'show', ___.frozenFunc(function () {

// *** jquery.js ***
//  2933:   show: function(speed,callback){
//  2938: 
//  2949:       }).end();

function show$_lit$($dis, speed, callback) {

// *** jquery.js ***
//  2934:     return speed ?
//  2935:       this.animate({
//  2936:         height: "show", width: "show", opacity: "show"
//  2937:       }, speed, callback) :
//  2939:       this.filter(":hidden").each(function(){
//  2940:         this.style.display = this.oldblock || "";

return speed? $v.cm($dis, 'animate', [ ___.initializeMap([ 'height', 'show', 'width', 'show', 'opacity', 'show' ]), speed, callback ]): $v.cm($v.cm($v.cm($dis, 'filter', [ ':hidden' ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.s($v.r($dis, 'style'), 'display', $v.r($dis, 'oldblock') || '');

// *** jquery.js ***
//  2941:         if ( jQuery.css(this,"display") == "none" ) {
//  2943:           this.style.display = elem.css("display");
//  2944:           // handle an edge condition where css is - div { display:none; } or similar
//  2947:           elem.remove();
//  2948:         }

if ($v.cm(jQuery, 'css', [ $dis, 'display' ]) == 'none') {

// *** jquery.js ***
//  2942:           var elem = jQuery("<" + this.tagName + " />").appendTo("body");

var elem = $v.cm($v.cf(jQuery, [ '\x3c' + $v.r($dis, 'tagName') + ' /\x3e' ]), 'appendTo', [ 'body' ]);
$v.s($v.r($dis, 'style'), 'display', $v.cm(elem, 'css', [ 'display' ]));

// *** jquery.js ***
//  2945:           if (this.style.display == "none")
//  2946:             this.style.display = "block";

if ($v.r($v.r($dis, 'style'), 'display') == 'none') $v.s($v.r($dis, 'style'), 'display', 'block');
$v.cm(elem, 'remove', [ ]);
}
})) ]), 'end', [ ]);
}
___.func(show$_lit$, 'show$_lit$');
;
var show$_lit = $v.dis(___.primFreeze(show$_lit$), 'show$_lit');
return show$_lit;
}).CALL___(), 'hide', ___.frozenFunc(function () {

// *** jquery.js ***
//  2952:   hide: function(speed,callback){
//  2957: 
//  2961:       }).end();

function hide$_lit$($dis, speed, callback) {

// *** jquery.js ***
//  2953:     return speed ?
//  2954:       this.animate({
//  2955:         height: "hide", width: "hide", opacity: "hide"
//  2956:       }, speed, callback) :
//  2958:       this.filter(":visible").each(function(){
//  2959:         this.oldblock = this.oldblock || jQuery.css(this,"display");

return speed? $v.cm($dis, 'animate', [ ___.initializeMap([ 'height', 'hide', 'width', 'hide', 'opacity', 'hide' ]), speed, callback ]): $v.cm($v.cm($v.cm($dis, 'filter', [ ':visible' ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.s($dis, 'oldblock', $v.r($dis, 'oldblock') || $v.cm(jQuery, 'css', [ $dis, 'display' ]));
$v.s($v.r($dis, 'style'), 'display', 'none');

// *** jquery.js ***
//  2960:         this.style.display = "none";

})) ]), 'end', [ ]);
}
___.func(hide$_lit$, 'hide$_lit$');
;
var hide$_lit = $v.dis(___.primFreeze(hide$_lit$), 'hide$_lit');
return hide$_lit;

// *** jquery.js ***
//  2965:   _toggle: jQuery.fn.toggle,

}).CALL___(), '_toggle', $v.r($v.r(jQuery, 'fn'), 'toggle'), 'toggle', ___.frozenFunc(function () {

// *** jquery.js ***
//  2967:   toggle: function( fn, fn2 ){

function toggle$_lit$($dis, fn, fn2) {
var a___ = ___.args(arguments);

// *** jquery.js ***
//  2968:     return jQuery.isFunction(fn) && jQuery.isFunction(fn2) ?
//  2969:       this._toggle.apply( this, arguments ) :
//  2970:       fn ?
//  2971:         this.animate({
//  2972:           height: "toggle", width: "toggle", opacity: "toggle"
//  2973:         }, fn, fn2) :
//  2974:         this.each(function(){
//  2976:         });

return $v.cm(jQuery, 'isFunction', [ fn ]) && $v.cm(jQuery, 'isFunction', [ fn2 ]) ? $v.cm($v.r($dis, '_toggle'), 'apply', [ $dis, Array.slice(a___, 1) ]): fn? $v.cm($dis, 'animate', [ ___.initializeMap([ 'height', 'toggle', 'width', 'toggle', 'opacity', 'toggle' ]), fn, fn2 ]): $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm($v.cf(jQuery, [ $dis ]), $v.cm($v.cf(jQuery, [ $dis ]), 'is', [ ':hidden' ]) ? 'show': 'hide', [ ]);

// *** jquery.js ***
//  2975:           jQuery(this)[ jQuery(this).is(":hidden") ? "show" : "hide" ]();

})) ]);
}
___.func(toggle$_lit$, 'toggle$_lit$');
;
var toggle$_lit = $v.dis(___.primFreeze(toggle$_lit$), 'toggle$_lit');
return toggle$_lit;
}).CALL___(), 'slideDown', ___.frozenFunc(function () {

// *** jquery.js ***
//  2979:   slideDown: function(speed,callback){

function slideDown$_lit$($dis, speed, callback) {

// *** jquery.js ***
//  2980:     return this.animate({height: "show"}, speed, callback);

return $v.cm($dis, 'animate', [ ___.initializeMap([ 'height', 'show' ]), speed, callback ]);
}
___.func(slideDown$_lit$, 'slideDown$_lit$');
;
var slideDown$_lit = $v.dis(___.primFreeze(slideDown$_lit$), 'slideDown$_lit');
return slideDown$_lit;
}).CALL___(), 'slideUp', ___.frozenFunc(function () {

// *** jquery.js ***
//  2983:   slideUp: function(speed,callback){

function slideUp$_lit$($dis, speed, callback) {

// *** jquery.js ***
//  2984:     return this.animate({height: "hide"}, speed, callback);

return $v.cm($dis, 'animate', [ ___.initializeMap([ 'height', 'hide' ]), speed, callback ]);
}
___.func(slideUp$_lit$, 'slideUp$_lit$');
;
var slideUp$_lit = $v.dis(___.primFreeze(slideUp$_lit$), 'slideUp$_lit');
return slideUp$_lit;
}).CALL___(), 'slideToggle', ___.frozenFunc(function () {

// *** jquery.js ***
//  2987:   slideToggle: function(speed, callback){

function slideToggle$_lit$($dis, speed, callback) {

// *** jquery.js ***
//  2988:     return this.animate({height: "toggle"}, speed, callback);

return $v.cm($dis, 'animate', [ ___.initializeMap([ 'height', 'toggle' ]), speed, callback ]);
}
___.func(slideToggle$_lit$, 'slideToggle$_lit$');
;
var slideToggle$_lit = $v.dis(___.primFreeze(slideToggle$_lit$), 'slideToggle$_lit');
return slideToggle$_lit;
}).CALL___(), 'fadeIn', ___.frozenFunc(function () {

// *** jquery.js ***
//  2991:   fadeIn: function(speed, callback){

function fadeIn$_lit$($dis, speed, callback) {

// *** jquery.js ***
//  2992:     return this.animate({opacity: "show"}, speed, callback);

return $v.cm($dis, 'animate', [ ___.initializeMap([ 'opacity', 'show' ]), speed, callback ]);
}
___.func(fadeIn$_lit$, 'fadeIn$_lit$');
;
var fadeIn$_lit = $v.dis(___.primFreeze(fadeIn$_lit$), 'fadeIn$_lit');
return fadeIn$_lit;
}).CALL___(), 'fadeOut', ___.frozenFunc(function () {

// *** jquery.js ***
//  2995:   fadeOut: function(speed, callback){

function fadeOut$_lit$($dis, speed, callback) {

// *** jquery.js ***
//  2996:     return this.animate({opacity: "hide"}, speed, callback);

return $v.cm($dis, 'animate', [ ___.initializeMap([ 'opacity', 'hide' ]), speed, callback ]);
}
___.func(fadeOut$_lit$, 'fadeOut$_lit$');
;
var fadeOut$_lit = $v.dis(___.primFreeze(fadeOut$_lit$), 'fadeOut$_lit');
return fadeOut$_lit;
}).CALL___(), 'fadeTo', ___.frozenFunc(function () {

// *** jquery.js ***
//  2999:   fadeTo: function(speed,to,callback){

function fadeTo$_lit$($dis, speed, to, callback) {

// *** jquery.js ***
//  3000:     return this.animate({opacity: to}, speed, callback);

return $v.cm($dis, 'animate', [ ___.initializeMap([ 'opacity', to ]), speed, callback ]);
}
___.func(fadeTo$_lit$, 'fadeTo$_lit$');
;
var fadeTo$_lit = $v.dis(___.primFreeze(fadeTo$_lit$), 'fadeTo$_lit');
return fadeTo$_lit;
}).CALL___(), 'animate', ___.frozenFunc(function () {

// *** jquery.js ***
//  3003:   animate: function( prop, speed, easing, callback ) {
//  3005: 

function animate$_lit$($dis, prop, speed, easing, callback) {

// *** jquery.js ***
//  3004:     var optall = jQuery.speed(speed, easing, callback);

var optall = $v.cm(jQuery, 'speed', [ speed, easing, callback ]);

// *** jquery.js ***
//  3006:     return this[ optall.queue === false ? "each" : "queue" ](function(){
//  3009: 
//  3012: 
//  3025: 
//  3028: 
//  3029:       opt.curAnim = jQuery.extend({}, prop);
//  3030: 
//  3031:       jQuery.each( prop, function(name, val){
//  3033: 
//  3059:       });
//  3060: 
//  3061:       // For JS strict compliance
//  3063:     });

return $v.cm($dis, $v.r(optall, 'queue') === false ? 'each': 'queue', [ $v.dis(___.frozenFunc(function ($dis) {
var $caja$31;
var $caja$32;

// *** jquery.js ***
//  3007:       if ( this.nodeType != 1)
//  3008:         return false;

if ($v.r($dis, 'nodeType') != 1) return false;

// *** jquery.js ***
//  3010:       var opt = jQuery.extend({}, optall), p,
//  3011:         hidden = jQuery(this).is(":hidden"), self = this;

var opt = $v.cm(jQuery, 'extend', [ ___.initializeMap([ ]), optall ]), p, hidden = $v.cm($v.cf(jQuery, [ $dis ]), 'is', [ ':hidden' ]), self = $dis;
{
$caja$31 = $v.keys(prop);

// *** jquery.js ***
//  3013:       for ( p in prop ) {
//  3016: 
//  3024:       }

for ($caja$32 = 0; $caja$32 < ($caja$31.length_canRead___? $caja$31.length: ___.readPub($caja$31, 'length')); ++$caja$32) {
p = ___.readPub($caja$31, $caja$32);
{

// *** jquery.js ***
//  3014:         if ( prop[p] == "hide" && hidden || prop[p] == "show" && !hidden )
//  3015:           return opt.complete.call(this);

if ($v.r(prop, p) == 'hide' && hidden || $v.r(prop, p) == 'show' && !hidden) return $v.cm($v.r(opt, 'complete'), 'call', [ $dis ]);

// *** jquery.js ***
//  3017:         if ( p == "height" || p == "width" ) {
//  3018:           // Store display property
//  3019:           opt.display = jQuery.css(this, "display");
//  3020: 
//  3021:           // Make sure that nothing sneaks out
//  3022:           opt.overflow = this.style.overflow;
//  3023:         }

if (p == 'height' || p == 'width') {
$v.s(opt, 'display', $v.cm(jQuery, 'css', [ $dis, 'display' ]));
$v.s(opt, 'overflow', $v.r($v.r($dis, 'style'), 'overflow'));
}
}
}
}

// *** jquery.js ***
//  3026:       if ( opt.overflow != null )
//  3027:         this.style.overflow = "hidden";

if ($v.r(opt, 'overflow') != null) $v.s($v.r($dis, 'style'), 'overflow', 'hidden');
$v.s(opt, 'curAnim', $v.cm(jQuery, 'extend', [ ___.initializeMap([ ]), prop ]));
$v.cm(jQuery, 'each', [ prop, $v.dis(___.frozenFunc(function ($dis, name, val) {

// *** jquery.js ***
//  3032:         var e = new jQuery.fx( self, opt, name );

var e = $v.construct($v.r(jQuery, 'fx'), [ self, opt, name ]);

// *** jquery.js ***
//  3034:         if ( /toggle|show|hide/.test(val) )
//  3035:           e[ val == "toggle" ? hidden ? "show" : "hide" : val ]( prop );
//  3036:         else {
//  3039: 
//  3058:         }

if ($v.cm($v.construct(RegExp, [ 'toggle|show|hide' ]), 'test', [ val ])) $v.cm(e, val == 'toggle'? hidden? 'show': 'hide': val, [ prop ]);
else {

// *** jquery.js ***
//  3037:           var parts = val.toString().match(/^([+-]=)?([\d+-.]+)(.*)$/),
//  3038:             start = e.cur(true) || 0;

var parts = $v.cm($v.cm(val, 'toString', [ ]), 'match', [ $v.construct(RegExp, [ '^([+-]=)?([\\d+-.]+)(.*)$' ]) ]), start = $v.cm(e, 'cur', [ true ]) || 0;

// *** jquery.js ***
//  3040:           if ( parts ) {
//  3043: 
//  3044:             // We need to compute starting value
//  3050: 
//  3051:             // If a +=/-= token was provided, we're doing a relative animation
//  3054: 
//  3055:             e.custom( start, end, unit );
//  3057:             e.custom( start, val, "" );

if (parts) {

// *** jquery.js ***
//  3041:             var end = parseFloat(parts[2]),
//  3042:               unit = parts[3] || "px";

var end = $v.cf($v.ro('parseFloat'), [ $v.r(parts, 2) ]), unit = $v.r(parts, 3) || 'px';

// *** jquery.js ***
//  3045:             if ( unit != "px" ) {
//  3046:               self.style[ name ] = (end || 1) + unit;
//  3048:               self.style[ name ] = start + unit;
//  3049:             }

if (unit != 'px') {
$v.s($v.r(self, 'style'), name, (end || 1) + unit);

// *** jquery.js ***
//  3047:               start = ((end || 1) / e.cur(true)) * start;

start = (end || 1) / $v.cm(e, 'cur', [ true ]) * start;
$v.s($v.r(self, 'style'), name, start + unit);
}

// *** jquery.js ***
//  3052:             if ( parts[1] )
//  3053:               end = ((parts[1] == "-=" ? -1 : 1) * end) + start;

if ($v.r(parts, 1)) end = ($v.r(parts, 1) == '-='? -1: 1) * end + start;
$v.cm(e, 'custom', [ start, end, unit ]);

// *** jquery.js ***
//  3056:           } else

} else $v.cm(e, 'custom', [ start, val, '' ]);
}
})) ]);

// *** jquery.js ***
//  3062:       return true;

return true;
})) ]);
}
___.func(animate$_lit$, 'animate$_lit$');
;
var animate$_lit = $v.dis(___.primFreeze(animate$_lit$), 'animate$_lit');
return animate$_lit;
}).CALL___(), 'queue', ___.frozenFunc(function () {

// *** jquery.js ***
//  3066:   queue: function(type, fn){
//  3070:     }
//  3071: 
//  3074: 

function queue$_lit$($dis, type, fn) {

// *** jquery.js ***
//  3067:     if ( jQuery.isFunction(type) || ( type && type.constructor == Array )) {

if ($v.cm(jQuery, 'isFunction', [ type ]) || type && $v.r(type, 'constructor') == $v.ro('Array')) {

// *** jquery.js ***
//  3068:       fn = type;

fn = type;

// *** jquery.js ***
//  3069:       type = "fx";

type = 'fx';
}

// *** jquery.js ***
//  3072:     if ( !type || (typeof type == "string" && !fn) )
//  3073:       return queue( this[0], type );

if (!type || $v.typeOf(type) == 'string' && !fn) return $v.cf(queue, [ $v.r($dis, 0), type ]);

// *** jquery.js ***
//  3075:     return this.each(function(){
//  3084:     });

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//  3076:       if ( fn.constructor == Array )
//  3077:         queue(this, type, fn);
//  3078:       else {
//  3079:         queue(this, type).push( fn );
//  3080: 
//  3083:       }

if ($v.r(fn, 'constructor') == $v.ro('Array')) $v.cf(queue, [ $dis, type, fn ]);
else {
$v.cm($v.cf(queue, [ $dis, type ]), 'push', [ fn ]);

// *** jquery.js ***
//  3081:         if ( queue(this, type).length == 1 )
//  3082:           fn.call(this);

if ($v.r($v.cf(queue, [ $dis, type ]), 'length') == 1) $v.cm(fn, 'call', [ $dis ]);
}
})) ]);
}
___.func(queue$_lit$, 'queue$_lit$');
;
var queue$_lit = $v.dis(___.primFreeze(queue$_lit$), 'queue$_lit');
return queue$_lit;
}).CALL___(), 'stop', ___.frozenFunc(function () {

// *** jquery.js ***
//  3087:   stop: function(clearQueue, gotoEnd){
//  3089: 
//  3092: 
//  3093:     this.each(function(){
//  3094:       // go in reverse order so anything added to the queue during the loop is ignored
//  3102:     });
//  3103: 
//  3104:     // start the next in the queue if the last step wasn't forced
//  3107: 
//  3108:     return this;

function stop$_lit$($dis, clearQueue, gotoEnd) {

// *** jquery.js ***
//  3088:     var timers = jQuery.timers;

var timers = $v.r(jQuery, 'timers');

// *** jquery.js ***
//  3090:     if (clearQueue)
//  3091:       this.queue([]);

if (clearQueue) $v.cm($dis, 'queue', [ [ ] ]);
$v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//  3095:       for ( var i = timers.length - 1; i >= 0; i-- )
//  3096:         if ( timers[i].elem == this ) {
//  3098:             // force the next step to be the last
//  3100:           timers.splice(i, 1);
//  3101:         }

for (var i = $v.r(timers, 'length') - 1; i >= 0; i--) if ($v.r($v.r(timers, i), 'elem') == $dis) {

// *** jquery.js ***
//  3097:           if (gotoEnd)
//  3099:             timers[i](true);

if (gotoEnd) $v.cm(timers, i, [ true ]);
$v.cm(timers, 'splice', [ i, 1 ]);
}
})) ]);

// *** jquery.js ***
//  3105:     if (!gotoEnd)
//  3106:       this.dequeue();

if (!gotoEnd) $v.cm($dis, 'dequeue', [ ]);
return $dis;
}
___.func(stop$_lit$, 'stop$_lit$');
;
var stop$_lit = $v.dis(___.primFreeze(stop$_lit$), 'stop$_lit');
return stop$_lit;
}).CALL___() ]) ]);

// *** jquery.js ***
//  3125: };

var queue = ___.frozenFunc(function () {

// *** jquery.js ***
//  3113: var queue = function( elem, type, array ) {
//  3115: 
//  3117: 
//  3119: 
//  3122: 
//  3123:   }

function queue$_var$($dis, elem, type, array) {

// *** jquery.js ***
//  3114:   if ( elem ){

if (elem) {

// *** jquery.js ***
//  3116:     type = type || "fx";

type = type || 'fx';

// *** jquery.js ***
//  3118:     var q = jQuery.data( elem, type + "queue" );

var q = $v.cm(jQuery, 'data', [ elem, type + 'queue' ]);

// *** jquery.js ***
//  3120:     if ( !q || array )
//  3121:       q = jQuery.data( elem, type + "queue", jQuery.makeArray(array) );

if (!q || array) q = $v.cm(jQuery, 'data', [ elem, type + 'queue', $v.cm(jQuery, 'makeArray', [ array ]) ]);
}

// *** jquery.js ***
//  3124:   return q;

return q;
}
___.func(queue$_var$, 'queue$_var$');
;
var queue$_var = $v.dis(___.primFreeze(queue$_var$), 'queue$_var');
return queue$_var;
}).CALL___();
$v.s($v.r(jQuery, 'fn'), 'dequeue', ___.frozenFunc(function () {

// *** jquery.js ***
//  3127: jQuery.fn.dequeue = function(type){
//  3129: 

function dequeue$_meth$($dis, type) {

// *** jquery.js ***
//  3128:   type = type || "fx";

type = type || 'fx';

// *** jquery.js ***
//  3130:   return this.each(function(){
//  3132: 
//  3133:     q.shift();
//  3134: 
//  3137:   });

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//  3131:     var q = queue(this, type);

var q = $v.cf(queue, [ $dis, type ]);
$v.cm(q, 'shift', [ ]);

// *** jquery.js ***
//  3135:     if ( q.length )
//  3136:       q[0].call( this );

if ($v.r(q, 'length')) $v.cm($v.r(q, 0), 'call', [ $dis ]);
})) ]);
}
___.func(dequeue$_meth$, 'dequeue$_meth$');
;
var dequeue$_meth = $v.dis(___.primFreeze(dequeue$_meth$), 'dequeue$_meth');
return dequeue$_meth;
}).CALL___());
$v.cm(jQuery, 'extend', [ ___.initializeMap([ 'speed', ___.frozenFunc(function () {

// *** jquery.js ***
//  3142:   speed: function(speed, easing, fn) {
//  3148:     };
//  3149: 
//  3150:     opt.duration = (opt.duration && opt.duration.constructor == Number ?
//  3151:       opt.duration :
//  3152:       jQuery.fx.speeds[opt.duration]) || jQuery.fx.speeds.def;
//  3153: 
//  3154:     // Queueing
//  3155:     opt.old = opt.complete;
//  3156:     opt.complete = function(){
//  3161:     };
//  3162: 

function speed$_lit$($dis, speed, easing, fn) {

// *** jquery.js ***
//  3143:     var opt = speed && speed.constructor == Object ? speed : {
//  3144:       complete: fn || !fn && easing ||
//  3145:         jQuery.isFunction( speed ) && speed,
//  3146:       duration: speed,
//  3147:       easing: fn && easing || easing && easing.constructor != Function && easing

var opt = speed && $v.r(speed, 'constructor') == $v.ro('Object') ? speed: ___.initializeMap([ 'complete', fn || !fn && easing || $v.cm(jQuery, 'isFunction', [ speed ]) && speed, 'duration', speed, 'easing', fn && easing || easing && $v.r(easing, 'constructor') != $v.ro('Function') && easing ]);
$v.s(opt, 'duration', ($v.r(opt, 'duration') && $v.r($v.r(opt, 'duration'), 'constructor') == $v.ro('Number') ? $v.r(opt, 'duration'): $v.r($v.r($v.r(jQuery, 'fx'), 'speeds'), $v.r(opt, 'duration'))) || $v.r($v.r($v.r(jQuery, 'fx'), 'speeds'), 'def'));
$v.s(opt, 'old', $v.r(opt, 'complete'));
$v.s(opt, 'complete', ___.frozenFunc(function () {
function complete$_meth$($dis) {

// *** jquery.js ***
//  3157:       if ( opt.queue !== false )
//  3158:         jQuery(this).dequeue();

if ($v.r(opt, 'queue') !== false) $v.cm($v.cf(jQuery, [ $dis ]), 'dequeue', [ ]);

// *** jquery.js ***
//  3159:       if ( jQuery.isFunction( opt.old ) )
//  3160:         opt.old.call( this );

if ($v.cm(jQuery, 'isFunction', [ $v.r(opt, 'old') ])) $v.cm($v.r(opt, 'old'), 'call', [ $dis ]);
}
___.func(complete$_meth$, 'complete$_meth$');
;
var complete$_meth = $v.dis(___.primFreeze(complete$_meth$), 'complete$_meth');
return complete$_meth;
}).CALL___());

// *** jquery.js ***
//  3163:     return opt;

return opt;
}
___.func(speed$_lit$, 'speed$_lit$');
;
var speed$_lit = $v.dis(___.primFreeze(speed$_lit$), 'speed$_lit');
return speed$_lit;
}).CALL___(), 'easing', ___.initializeMap([ 'linear', ___.frozenFunc(function () {

// *** jquery.js ***
//  3167:     linear: function( p, n, firstNum, diff ) {

function linear$_lit$($dis, p, n, firstNum, diff) {

// *** jquery.js ***
//  3168:       return firstNum + diff * p;

return firstNum + diff * p;
}
___.func(linear$_lit$, 'linear$_lit$');
;
var linear$_lit = $v.dis(___.primFreeze(linear$_lit$), 'linear$_lit');
return linear$_lit;
}).CALL___(), 'swing', ___.frozenFunc(function () {

// *** jquery.js ***
//  3170:     swing: function( p, n, firstNum, diff ) {

function swing$_lit$($dis, p, n, firstNum, diff) {

// *** jquery.js ***
//  3171:       return ((-Math.cos(p*Math.PI)/2) + 0.5) * diff + firstNum;

return ((-$v.cm($v.ro('Math'), 'cos', [ p * $v.r($v.ro('Math'), 'PI') ])) / 2 + 0.5) * diff + firstNum;
}
___.func(swing$_lit$, 'swing$_lit$');
;
var swing$_lit = $v.dis(___.primFreeze(swing$_lit$), 'swing$_lit');
return swing$_lit;
}).CALL___() ]), 'timers', [ ], 'timerId', null, 'fx', ___.frozenFunc(function () {

// *** jquery.js ***
//  3178:   fx: function( elem, options, prop ){
//  3179:     this.options = options;
//  3180:     this.elem = elem;
//  3181:     this.prop = prop;
//  3182: 

function fx$_lit$($dis, elem, options, prop) {
$v.s($dis, 'options', options);
$v.s($dis, 'elem', elem);
$v.s($dis, 'prop', prop);

// *** jquery.js ***
//  3183:     if ( !options.orig )
//  3184:       options.orig = {};

if (!$v.r(options, 'orig')) $v.s(options, 'orig', ___.initializeMap([ ]));
}
___.func(fx$_lit$, 'fx$_lit$');
;
var fx$_lit = $v.dis(___.primFreeze(fx$_lit$), 'fx$_lit');
return fx$_lit;
}).CALL___() ]) ]);

// *** jquery.js ***
//  3189: jQuery.fx.prototype = {

$v.s($v.r(jQuery, 'fx'), 'prototype', ___.initializeMap([ 'update', ___.frozenFunc(function () {

// *** jquery.js ***
//  3195: 
//  3196:     (jQuery.fx.step[this.prop] || jQuery.fx.step._default)( this );
//  3197: 
//  3198:     // Set display property to block for height/width animations

function update$_lit$($dis) {

// *** jquery.js ***
//  3193:     if ( this.options.step )
//  3194:       this.options.step.call( this.elem, this.now, this );

if ($v.r($v.r($dis, 'options'), 'step')) $v.cm($v.r($v.r($dis, 'options'), 'step'), 'call', [ $v.r($dis, 'elem'), $v.r($dis, 'now'), $dis ]);
$v.cf($v.r($v.r($v.r(jQuery, 'fx'), 'step'), $v.r($dis, 'prop')) || $v.r($v.r($v.r(jQuery, 'fx'), 'step'), '_default'), [ $dis ]);

// *** jquery.js ***
//  3199:     if ( this.prop == "height" || this.prop == "width" )
//  3200:       this.elem.style.display = "block";

if ($v.r($dis, 'prop') == 'height' || $v.r($dis, 'prop') == 'width') $v.s($v.r($v.r($dis, 'elem'), 'style'), 'display', 'block');
}
___.func(update$_lit$, 'update$_lit$');
;
var update$_lit = $v.dis(___.primFreeze(update$_lit$), 'update$_lit');
return update$_lit;
}).CALL___(), 'cur', ___.frozenFunc(function () {

// *** jquery.js ***
//  3204:   cur: function(force){
//  3207: 

function cur$_lit$($dis, force) {

// *** jquery.js ***
//  3205:     if ( this.elem[this.prop] != null && this.elem.style[this.prop] == null )
//  3206:       return this.elem[ this.prop ];

if ($v.r($v.r($dis, 'elem'), $v.r($dis, 'prop')) != null && $v.r($v.r($v.r($dis, 'elem'), 'style'), $v.r($dis, 'prop')) == null) return $v.r($v.r($dis, 'elem'), $v.r($dis, 'prop'));

// *** jquery.js ***
//  3208:     var r = parseFloat(jQuery.css(this.elem, this.prop, force));

var r = $v.cf($v.ro('parseFloat'), [ $v.cm(jQuery, 'css', [ $v.r($dis, 'elem'), $v.r($dis, 'prop'), force ]) ]);

// *** jquery.js ***
//  3209:     return r && r > -10000 ? r : parseFloat(jQuery.curCSS(this.elem, this.prop)) || 0;

return r && r > -10000? r: $v.cf($v.ro('parseFloat'), [ $v.cm(jQuery, 'curCSS', [ $v.r($dis, 'elem'), $v.r($dis, 'prop') ]) ]) || 0;
}
___.func(cur$_lit$, 'cur$_lit$');
;
var cur$_lit = $v.dis(___.primFreeze(cur$_lit$), 'cur$_lit');
return cur$_lit;
}).CALL___(), 'custom', ___.frozenFunc(function () {

// *** jquery.js ***
//  3213:   custom: function(from, to, unit){
//  3214:     this.startTime = now();
//  3215:     this.start = from;
//  3216:     this.end = to;
//  3217:     this.unit = unit || this.unit || "px";
//  3218:     this.now = this.start;
//  3219:     this.pos = this.state = 0;
//  3220:     this.update();
//  3221: 
//  3225:     }
//  3226: 
//  3227:     t.elem = this.elem;
//  3228: 
//  3229:     jQuery.timers.push(t);
//  3230: 
//  3232:       jQuery.timerId = setInterval(function(){
//  3234: 
//  3238: 
//  3243:       }, 13);
//  3244:     }

function custom$_lit$($dis, from, to, unit) {

// *** jquery.js ***
//  3223:     function t(gotoEnd){

function t$_caller($dis, gotoEnd) {

// *** jquery.js ***
//  3224:       return self.step(gotoEnd);

return $v.cm(self, 'step', [ gotoEnd ]);
}
___.func(t$_caller, 't$_caller');
var t;
;
t = $v.dis(___.primFreeze(t$_caller), 't');
$v.s($dis, 'startTime', $v.cf(now, [ ]));
$v.s($dis, 'start', from);
$v.s($dis, 'end', to);
$v.s($dis, 'unit', unit || $v.r($dis, 'unit') || 'px');
$v.s($dis, 'now', $v.r($dis, 'start'));
$v.s($dis, 'pos', $v.s($dis, 'state', 0));
$v.cm($dis, 'update', [ ]);

// *** jquery.js ***
//  3222:     var self = this;

var self = $dis;
;
$v.s(t, 'elem', $v.r($dis, 'elem'));
$v.cm($v.r(jQuery, 'timers'), 'push', [ t ]);

// *** jquery.js ***
//  3231:     if ( jQuery.timerId == null ) {

if ($v.r(jQuery, 'timerId') == null) {
$v.s(jQuery, 'timerId', $v.cf($v.ro('setInterval'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//  3233:         var timers = jQuery.timers;

var timers = $v.r(jQuery, 'timers');

// *** jquery.js ***
//  3235:         for ( var i = 0; i < timers.length; i++ )
//  3236:           if ( !timers[i]() )
//  3237:             timers.splice(i--, 1);

for (var i = 0; i < $v.r(timers, 'length'); i++) if (!$v.cm(timers, i, [ ])) $v.cm(timers, 'splice', [ i--, 1 ]);

// *** jquery.js ***
//  3239:         if ( !timers.length ) {
//  3240:           clearInterval( jQuery.timerId );
//  3241:           jQuery.timerId = null;
//  3242:         }

if (!$v.r(timers, 'length')) {
$v.cf($v.ro('clearInterval'), [ $v.r(jQuery, 'timerId') ]);
$v.s(jQuery, 'timerId', null);
}
})), 13 ]));
}
}
___.func(custom$_lit$, 'custom$_lit$');
;
var custom$_lit = $v.dis(___.primFreeze(custom$_lit$), 'custom$_lit');
return custom$_lit;
}).CALL___(), 'show', ___.frozenFunc(function () {

// *** jquery.js ***
//  3250:     this.options.orig[this.prop] = jQuery.attr( this.elem.style, this.prop );
//  3251:     this.options.show = true;
//  3252: 
//  3253:     // Begin the animation
//  3254:     this.custom(0, this.cur());
//  3255: 
//  3256:     // Make sure that we start at a small width/height to avoid any
//  3257:     // flash of content
//  3260: 
//  3261:     // Start by showing the element
//  3262:     jQuery(this.elem).show();

function show$_lit$($dis) {
$v.s($v.r($v.r($dis, 'options'), 'orig'), $v.r($dis, 'prop'), $v.cm(jQuery, 'attr', [ $v.r($v.r($dis, 'elem'), 'style'), $v.r($dis, 'prop') ]));
$v.s($v.r($dis, 'options'), 'show', true);
$v.cm($dis, 'custom', [ 0, $v.cm($dis, 'cur', [ ]) ]);

// *** jquery.js ***
//  3258:     if ( this.prop == "width" || this.prop == "height" )
//  3259:       this.elem.style[this.prop] = "1px";

if ($v.r($dis, 'prop') == 'width' || $v.r($dis, 'prop') == 'height') $v.s($v.r($v.r($dis, 'elem'), 'style'), $v.r($dis, 'prop'), '1px');
$v.cm($v.cf(jQuery, [ $v.r($dis, 'elem') ]), 'show', [ ]);
}
___.func(show$_lit$, 'show$_lit$');
;
var show$_lit = $v.dis(___.primFreeze(show$_lit$), 'show$_lit');
return show$_lit;
}).CALL___(), 'hide', ___.frozenFunc(function () {

// *** jquery.js ***
//  3268:     this.options.orig[this.prop] = jQuery.attr( this.elem.style, this.prop );
//  3269:     this.options.hide = true;
//  3270: 
//  3271:     // Begin the animation
//  3272:     this.custom(this.cur(), 0);

function hide$_lit$($dis) {
$v.s($v.r($v.r($dis, 'options'), 'orig'), $v.r($dis, 'prop'), $v.cm(jQuery, 'attr', [ $v.r($v.r($dis, 'elem'), 'style'), $v.r($dis, 'prop') ]));
$v.s($v.r($dis, 'options'), 'hide', true);
$v.cm($dis, 'custom', [ $v.cm($dis, 'cur', [ ]), 0 ]);
}
___.func(hide$_lit$, 'hide$_lit$');
;
var hide$_lit = $v.dis(___.primFreeze(hide$_lit$), 'hide$_lit');
return hide$_lit;
}).CALL___(), 'step', ___.frozenFunc(function () {

// *** jquery.js ***
//  3276:   step: function(gotoEnd){
//  3278: 
//  3280:       this.now = this.end;
//  3281:       this.pos = this.state = 1;
//  3282:       this.update();
//  3283: 
//  3284:       this.options.curAnim[ this.prop ] = true;
//  3285: 
//  3290: 
//  3310:       }
//  3311: 
//  3313:         // Execute the complete function
//  3315: 
//  3319:       this.state = n / this.options.duration;
//  3320: 
//  3321:       // Perform the easing function, defaults to swing
//  3322:       this.pos = jQuery.easing[this.options.easing || (jQuery.easing.swing ? "swing" : "linear")](this.state, n, 0, 1, this.options.duration);
//  3323:       this.now = this.start + ((this.end - this.start) * this.pos);
//  3324: 
//  3325:       // Perform the next step of the animation
//  3326:       this.update();
//  3327:     }
//  3328: 

function step$_lit$($dis, gotoEnd) {
var $caja$33;
var $caja$34;
var $caja$35;
var $caja$36;

// *** jquery.js ***
//  3277:     var t = now();

var t = $v.cf(now, [ ]);

// *** jquery.js ***
//  3279:     if ( gotoEnd || t > this.options.duration + this.startTime ) {

if (gotoEnd || t > $v.r($v.r($dis, 'options'), 'duration') + $v.r($dis, 'startTime')) {
$v.s($dis, 'now', $v.r($dis, 'end'));
$v.s($dis, 'pos', $v.s($dis, 'state', 1));
$v.cm($dis, 'update', [ ]);
$v.s($v.r($v.r($dis, 'options'), 'curAnim'), $v.r($dis, 'prop'), true);

// *** jquery.js ***
//  3286:       var done = true;

var done = true;
{
$caja$33 = $v.keys($v.r($v.r($dis, 'options'), 'curAnim'));
for ($caja$34 = 0; $caja$34 < ($caja$33.length_canRead___? $caja$33.length: ___.readPub($caja$33, 'length')); ++$caja$34) {

// *** jquery.js ***
//  3287:       for ( var i in this.options.curAnim )

var i = ___.readPub($caja$33, $caja$34);

// *** jquery.js ***
//  3288:         if ( this.options.curAnim[i] !== true )
//  3289:           done = false;

if ($v.r($v.r($v.r($dis, 'options'), 'curAnim'), i) !== true) done = false;
}
}

// *** jquery.js ***
//  3291:       if ( done ) {
//  3301: 
//  3302:         // Hide the element if the "hide" operation was done
//  3305: 
//  3306:         // Reset the properties, if the item has been hidden or shown

if (done) {

// *** jquery.js ***
//  3292:         if ( this.options.display != null ) {
//  3293:           // Reset the overflow
//  3294:           this.elem.style.overflow = this.options.overflow;
//  3295: 
//  3296:           // Reset the display
//  3297:           this.elem.style.display = this.options.display;
//  3300:         }

if ($v.r($v.r($dis, 'options'), 'display') != null) {
$v.s($v.r($v.r($dis, 'elem'), 'style'), 'overflow', $v.r($v.r($dis, 'options'), 'overflow'));
$v.s($v.r($v.r($dis, 'elem'), 'style'), 'display', $v.r($v.r($dis, 'options'), 'display'));

// *** jquery.js ***
//  3298:           if ( jQuery.css(this.elem, "display") == "none" )
//  3299:             this.elem.style.display = "block";

if ($v.cm(jQuery, 'css', [ $v.r($dis, 'elem'), 'display' ]) == 'none') $v.s($v.r($v.r($dis, 'elem'), 'style'), 'display', 'block');
}

// *** jquery.js ***
//  3303:         if ( this.options.hide )
//  3304:           this.elem.style.display = "none";

if ($v.r($v.r($dis, 'options'), 'hide')) $v.s($v.r($v.r($dis, 'elem'), 'style'), 'display', 'none');

// *** jquery.js ***
//  3307:         if ( this.options.hide || this.options.show )

if ($v.r($v.r($dis, 'options'), 'hide') || $v.r($v.r($dis, 'options'), 'show')) {
$caja$35 = $v.keys($v.r($v.r($dis, 'options'), 'curAnim'));

// *** jquery.js ***
//  3309:             jQuery.attr(this.elem.style, p, this.options.orig[p]);

for ($caja$36 = 0; $caja$36 < ($caja$35.length_canRead___? $caja$35.length: ___.readPub($caja$35, 'length')); ++$caja$36) {

// *** jquery.js ***
//  3308:           for ( var p in this.options.curAnim )

var p = ___.readPub($caja$35, $caja$36);
$v.cm(jQuery, 'attr', [ $v.r($v.r($dis, 'elem'), 'style'), p, $v.r($v.r($v.r($dis, 'options'), 'orig'), p) ]);
}
}
}

// *** jquery.js ***
//  3312:       if ( done )
//  3314:         this.options.complete.call( this.elem );

if (done) $v.cm($v.r($v.r($dis, 'options'), 'complete'), 'call', [ $v.r($dis, 'elem') ]);

// *** jquery.js ***
//  3316:       return false;

return false;

// *** jquery.js ***
//  3317:     } else {

} else {

// *** jquery.js ***
//  3318:       var n = t - this.startTime;

var n = t - $v.r($dis, 'startTime');
$v.s($dis, 'state', n / $v.r($v.r($dis, 'options'), 'duration'));
$v.s($dis, 'pos', $v.cm($v.r(jQuery, 'easing'), $v.r($v.r($dis, 'options'), 'easing') || ($v.r($v.r(jQuery, 'easing'), 'swing') ? 'swing': 'linear'), [ $v.r($dis, 'state'), n, 0, 1, $v.r($v.r($dis, 'options'), 'duration') ]));
$v.s($dis, 'now', $v.r($dis, 'start') + ($v.r($dis, 'end') - $v.r($dis, 'start')) * $v.r($dis, 'pos'));
$v.cm($dis, 'update', [ ]);
}

// *** jquery.js ***
//  3329:     return true;

return true;
}
___.func(step$_lit$, 'step$_lit$');
;
var step$_lit = $v.dis(___.primFreeze(step$_lit$), 'step$_lit');
return step$_lit;
}).CALL___() ]));

// *** jquery.js ***
//  3334: jQuery.extend( jQuery.fx, {

$v.cm(jQuery, 'extend', [ $v.r(jQuery, 'fx'), ___.initializeMap([ 'speeds', ___.initializeMap([ 'slow', 600, 'fast', 200, 'def', 400 ]), 'step', ___.initializeMap([ 'scrollLeft', ___.frozenFunc(function () {

// *** jquery.js ***
//  3342:     scrollLeft: function(fx){
//  3343:       fx.elem.scrollLeft = fx.now;

function scrollLeft$_lit$($dis, fx) {
$v.s($v.r(fx, 'elem'), 'scrollLeft', $v.r(fx, 'now'));
}
___.func(scrollLeft$_lit$, 'scrollLeft$_lit$');
;
var scrollLeft$_lit = $v.dis(___.primFreeze(scrollLeft$_lit$), 'scrollLeft$_lit');
return scrollLeft$_lit;
}).CALL___(), 'scrollTop', ___.frozenFunc(function () {

// *** jquery.js ***
//  3346:     scrollTop: function(fx){
//  3347:       fx.elem.scrollTop = fx.now;

function scrollTop$_lit$($dis, fx) {
$v.s($v.r(fx, 'elem'), 'scrollTop', $v.r(fx, 'now'));
}
___.func(scrollTop$_lit$, 'scrollTop$_lit$');
;
var scrollTop$_lit = $v.dis(___.primFreeze(scrollTop$_lit$), 'scrollTop$_lit');
return scrollTop$_lit;
}).CALL___(), 'opacity', ___.frozenFunc(function () {

// *** jquery.js ***
//  3350:     opacity: function(fx){
//  3351:       jQuery.attr(fx.elem.style, "opacity", fx.now);

function opacity$_lit$($dis, fx) {
$v.cm(jQuery, 'attr', [ $v.r($v.r(fx, 'elem'), 'style'), 'opacity', $v.r(fx, 'now') ]);
}
___.func(opacity$_lit$, 'opacity$_lit$');
;
var opacity$_lit = $v.dis(___.primFreeze(opacity$_lit$), 'opacity$_lit');
return opacity$_lit;
}).CALL___(), '_default', ___.frozenFunc(function () {

// *** jquery.js ***
//  3354:     _default: function(fx){
//  3355:       fx.elem.style[ fx.prop ] = fx.now + fx.unit;

function _default$_lit$($dis, fx) {
$v.s($v.r($v.r(fx, 'elem'), 'style'), $v.r(fx, 'prop'), $v.r(fx, 'now') + $v.r(fx, 'unit'));
}
___.func(_default$_lit$, '_default$_lit$');
;
var _default$_lit = $v.dis(___.primFreeze(_default$_lit$), '_default$_lit');
return _default$_lit;
}).CALL___() ]) ]) ]);

// *** jquery.js ***
//  3362: jQuery.fn.offset = function() {

$v.s($v.r(jQuery, 'fn'), 'offset', ___.frozenFunc(function () {

// *** jquery.js ***
//  3364: 
//  3371:     
//  3379: 
//  3380:     // Use getBoundingClientRect if available
//  3383: 
//  3384:       // Add the document scroll offsets
//  3385:       add(box.left + Math.max(doc.documentElement.scrollLeft, doc.body.scrollLeft),
//  3386:         box.top  + Math.max(doc.documentElement.scrollTop,  doc.body.scrollTop));
//  3387: 
//  3388:       // IE adds the HTML element's border, by default it is medium which is 2px
//  3389:       // IE 6 and 7 quirks mode the border width is overwritable by the following css html { border: 0; }
//  3390:       // IE 7 standards mode, the border is always 2px
//  3391:       // This border/offset is typically represented by the clientLeft and clientTop properties
//  3392:       // However, in IE6 and 7 quirks mode the clientLeft and clientTop properties are not updated when overwriting it via CSS
//  3393:       // Therefore this method will be off by 2px in IE while in quirksmode
//  3394:       add( -doc.documentElement.clientLeft, -doc.documentElement.clientTop );
//  3395: 
//  3396:     // Otherwise loop through the offsetParents and parentNodes
//  3398: 
//  3399:       // Initial element offsets
//  3400:       add( elem.offsetLeft, elem.offsetTop );
//  3401: 
//  3402:       // Get parent offsets
//  3421: 
//  3422:       // Get parent scroll offsets
//  3423:       while ( parent && parent.tagName && !/^body|html$/i.test(parent.tagName) ) {
//  3424:         // Remove parent scroll UNLESS that parent is inline or a table to work around Opera inline/table scrollLeft/Top bug
//  3428: 
//  3429:         // Mozilla does not add the border for a parent that has overflow != visible
//  3432: 
//  3433:         // Get next parent
//  3435:       }
//  3436: 
//  3437:       // Safari <= 2 doubles body offsets with a fixed position element/offsetParent or absolutely positioned offsetChild
//  3438:       // Mozilla doubles body offsets with a non-absolutely positioned offsetChild
//  3442: 
//  3443:       // Add the document scroll offsets if position is fixed
//  3447:     }
//  3448: 
//  3449:     // Return an object with top and left properties
//  3451:   }
//  3452: 
//  3455:   }
//  3456: 
//  3460:   }
//  3461: 

function offset$_meth$($dis) {

// *** jquery.js ***
//  3454:     add( jQuery.curCSS(elem, "borderLeftWidth", true), jQuery.curCSS(elem, "borderTopWidth", true) );

function border$_caller($dis, elem) {
$v.cf(add, [ $v.cm(jQuery, 'curCSS', [ elem, 'borderLeftWidth', true ]), $v.cm(jQuery, 'curCSS', [ elem, 'borderTopWidth', true ]) ]);
}
___.func(border$_caller, 'border$_caller');

// *** jquery.js ***
//  3457:   function add(l, t) {

function add$_caller($dis, l, t) {

// *** jquery.js ***
//  3458:     left += parseInt(l, 10) || 0;

left = left + ($v.cf($v.ro('parseInt'), [ l, 10 ]) || 0);

// *** jquery.js ***
//  3459:     top += parseInt(t, 10) || 0;

top = top + ($v.cf($v.ro('parseInt'), [ t, 10 ]) || 0);
}
___.func(add$_caller, 'add$_caller');
var border;
;

// *** jquery.js ***
//  3453:   function border(elem) {

border = $v.dis(___.primFreeze(border$_caller), 'border');
var add;
;
add = $v.dis(___.primFreeze(add$_caller), 'add');

// *** jquery.js ***
//  3363:   var left = 0, top = 0, elem = this[0], results;

var left = 0, top = 0, elem = $v.r($dis, 0), results;

// *** jquery.js ***
//  3365:   if ( elem ) {

if (elem) {

// *** jquery.js ***
//  3366:     var version = jQuery.browser.version;

var version = $v.r($v.r(jQuery, 'browser'), 'version');

// *** jquery.js ***
//  3367:     var safari = jQuery.browser.safari;

var safari = $v.r($v.r(jQuery, 'browser'), 'safari');

// *** jquery.js ***
//  3368:     var opera = jQuery.browser.opera;

var opera = $v.r($v.r(jQuery, 'browser'), 'opera');

// *** jquery.js ***
//  3369:     var msie = jQuery.browser.msie;

var msie = $v.r($v.r(jQuery, 'browser'), 'msie');

// *** jquery.js ***
//  3370:     var mozilla = jQuery.browser.mozilla;

var mozilla = $v.r($v.r(jQuery, 'browser'), 'mozilla');

// *** jquery.js ***
//  3372:     var parent       = elem.parentNode,
//  3373:         offsetChild  = elem,
//  3374:         offsetParent = elem.offsetParent,
//  3375:         doc          = elem.ownerDocument,
//  3376:         safari2      = safari && parseInt(version) < 522 && !/adobeair/i.test(userAgent),
//  3377:         css          = jQuery.curCSS,
//  3378:         fixed        = css(elem, "position") == "fixed";

var parent = $v.r(elem, 'parentNode'), offsetChild = elem, offsetParent = $v.r(elem, 'offsetParent'), doc = $v.r(elem, 'ownerDocument'), safari2 = safari && $v.cf($v.ro('parseInt'), [ version ]) < 522 && !$v.cm($v.construct(RegExp, [ 'adobeair', 'i' ]), 'test', [ userAgent ]), css = $v.r(jQuery, 'curCSS'), fixed = $v.cf(css, [ elem, 'position' ]) == 'fixed';

// *** jquery.js ***
//  3381:     if ( elem.getBoundingClientRect ) {

if ($v.r(elem, 'getBoundingClientRect')) {

// *** jquery.js ***
//  3382:       var box = elem.getBoundingClientRect();

var box = $v.cm(elem, 'getBoundingClientRect', [ ]);
$v.cf(add, [ $v.r(box, 'left') + $v.cm($v.ro('Math'), 'max', [ $v.r($v.r(doc, 'documentElement'), 'scrollLeft'), $v.r($v.r(doc, 'body'), 'scrollLeft') ]), $v.r(box, 'top') + $v.cm($v.ro('Math'), 'max', [ $v.r($v.r(doc, 'documentElement'), 'scrollTop'), $v.r($v.r(doc, 'body'), 'scrollTop') ]) ]);
$v.cf(add, [ -$v.r($v.r(doc, 'documentElement'), 'clientLeft'), -$v.r($v.r(doc, 'documentElement'), 'clientTop') ]);

// *** jquery.js ***
//  3397:     } else {

} else {
$v.cf(add, [ $v.r(elem, 'offsetLeft'), $v.r(elem, 'offsetTop') ]);

// *** jquery.js ***
//  3403:       while ( offsetParent ) {
//  3404:         // Add offsetParent offsets
//  3405:         add( offsetParent.offsetLeft, offsetParent.offsetTop );
//  3406: 
//  3407:         // Mozilla and Safari > 2 does not include the border on offset parents
//  3408:         // However Mozilla adds the border for table or table cells
//  3411: 
//  3412:         // Add the document scroll offsets if position is fixed on any offsetParent
//  3415: 
//  3416:         // Set offsetChild to previous offsetParent unless it is the body element
//  3418:         // Get next offsetParent
//  3420:       }

while (offsetParent) {
$v.cf(add, [ $v.r(offsetParent, 'offsetLeft'), $v.r(offsetParent, 'offsetTop') ]);

// *** jquery.js ***
//  3409:         if ( mozilla && !/^t(able|d|h)$/i.test(offsetParent.tagName) || safari && !safari2 )
//  3410:           border( offsetParent );

if (mozilla && !$v.cm($v.construct(RegExp, [ '^t(able|d|h)$', 'i' ]), 'test', [ $v.r(offsetParent, 'tagName') ]) || safari && !safari2) $v.cf(border, [ offsetParent ]);

// *** jquery.js ***
//  3413:         if ( !fixed && css(offsetParent, "position") == "fixed" )
//  3414:           fixed = true;

if (!fixed && $v.cf(css, [ offsetParent, 'position' ]) == 'fixed') fixed = true;

// *** jquery.js ***
//  3417:         offsetChild  = /^body$/i.test(offsetParent.tagName) ? offsetChild : offsetParent;

offsetChild = $v.cm($v.construct(RegExp, [ '^body$', 'i' ]), 'test', [ $v.r(offsetParent, 'tagName') ]) ? offsetChild: offsetParent;

// *** jquery.js ***
//  3419:         offsetParent = offsetParent.offsetParent;

offsetParent = $v.r(offsetParent, 'offsetParent');
} while (parent && $v.r(parent, 'tagName') && !$v.cm($v.construct(RegExp, [ '^body|html$', 'i' ]), 'test', [ $v.r(parent, 'tagName') ])) {

// *** jquery.js ***
//  3425:         if ( !/^inline|table.*$/i.test(css(parent, "display")) )
//  3426:           // Subtract parent scroll offsets
//  3427:           add( -parent.scrollLeft, -parent.scrollTop );

if (!$v.cm($v.construct(RegExp, [ '^inline|table.*$', 'i' ]), 'test', [ $v.cf(css, [ parent, 'display' ]) ])) $v.cf(add, [ -$v.r(parent, 'scrollLeft'), -$v.r(parent, 'scrollTop') ]);

// *** jquery.js ***
//  3430:         if ( mozilla && css(parent, "overflow") != "visible" )
//  3431:           border( parent );

if (mozilla && $v.cf(css, [ parent, 'overflow' ]) != 'visible') $v.cf(border, [ parent ]);

// *** jquery.js ***
//  3434:         parent = parent.parentNode;

parent = $v.r(parent, 'parentNode');
}

// *** jquery.js ***
//  3439:       if ( (safari2 && (fixed || css(offsetChild, "position") == "absolute")) ||
//  3440:         (mozilla && css(offsetChild, "position") != "absolute") )
//  3441:           add( -doc.body.offsetLeft, -doc.body.offsetTop );

if (safari2 && (fixed || $v.cf(css, [ offsetChild, 'position' ]) == 'absolute') || mozilla && $v.cf(css, [ offsetChild, 'position' ]) != 'absolute') $v.cf(add, [ -$v.r($v.r(doc, 'body'), 'offsetLeft'), -$v.r($v.r(doc, 'body'), 'offsetTop') ]);

// *** jquery.js ***
//  3444:       if ( fixed )
//  3445:         add(Math.max(doc.documentElement.scrollLeft, doc.body.scrollLeft),
//  3446:           Math.max(doc.documentElement.scrollTop,  doc.body.scrollTop));

if (fixed) $v.cf(add, [ $v.cm($v.ro('Math'), 'max', [ $v.r($v.r(doc, 'documentElement'), 'scrollLeft'), $v.r($v.r(doc, 'body'), 'scrollLeft') ]), $v.cm($v.ro('Math'), 'max', [ $v.r($v.r(doc, 'documentElement'), 'scrollTop'), $v.r($v.r(doc, 'body'), 'scrollTop') ]) ]);
}

// *** jquery.js ***
//  3450:     results = { top: top, left: left };

results = ___.initializeMap([ 'top', top, 'left', left ]);
};
;

// *** jquery.js ***
//  3462:   return results;

return results;
}
___.func(offset$_meth$, 'offset$_meth$');
;
var offset$_meth = $v.dis(___.primFreeze(offset$_meth$), 'offset$_meth');
return offset$_meth;
}).CALL___());

// *** jquery.js ***
//  3466: jQuery.fn.extend({

$v.cm($v.r(jQuery, 'fn'), 'extend', [ ___.initializeMap([ 'position', ___.frozenFunc(function () {

// *** jquery.js ***
//  3469: 
//  3470:     if ( this[0] ) {
//  3471:       // Get *real* offsetParent
//  3473: 
//  3474:       // Get correct offsets
//  3477: 
//  3478:       // Subtract element margins
//  3479:       // note: when an element has margin: auto the offsetLeft and marginLeft 
//  3480:       // are the same in Safari causing offset.left to incorrectly be 0
//  3481:       offset.top  -= num( this, 'marginTop' );
//  3482:       offset.left -= num( this, 'marginLeft' );
//  3483: 
//  3484:       // Add offsetParent borders
//  3485:       parentOffset.top  += num( offsetParent, 'borderTopWidth' );
//  3486:       parentOffset.left += num( offsetParent, 'borderLeftWidth' );
//  3487: 
//  3488:       // Subtract the two offsets
//  3490:         top:  offset.top  - parentOffset.top,
//  3491:         left: offset.left - parentOffset.left
//  3492:       };
//  3493:     }
//  3494: 

function position$_lit$($dis) {

// *** jquery.js ***
//  3468:     var left = 0, top = 0, results;

var left = 0, top = 0, results;
if ($v.r($dis, 0)) {

// *** jquery.js ***
//  3472:       var offsetParent = this.offsetParent(),
//  3475:       offset       = this.offset(),
//  3476:       parentOffset = /^body|html$/i.test(offsetParent[0].tagName) ? { top: 0, left: 0 } : offsetParent.offset();

var offsetParent = $v.cm($dis, 'offsetParent', [ ]), offset = $v.cm($dis, 'offset', [ ]), parentOffset = $v.cm($v.construct(RegExp, [ '^body|html$', 'i' ]), 'test', [ $v.r($v.r(offsetParent, 0), 'tagName') ]) ? ___.initializeMap([ 'top', 0, 'left', 0 ]): $v.cm(offsetParent, 'offset', [ ]);
$v.s(offset, 'top', $v.r(offset, 'top') - $v.cf(num, [ $dis, 'marginTop' ]));
$v.s(offset, 'left', $v.r(offset, 'left') - $v.cf(num, [ $dis, 'marginLeft' ]));
$v.s(parentOffset, 'top', $v.r(parentOffset, 'top') + $v.cf(num, [ offsetParent, 'borderTopWidth' ]));
$v.s(parentOffset, 'left', $v.r(parentOffset, 'left') + $v.cf(num, [ offsetParent, 'borderLeftWidth' ]));

// *** jquery.js ***
//  3489:       results = {

results = ___.initializeMap([ 'top', $v.r(offset, 'top') - $v.r(parentOffset, 'top'), 'left', $v.r(offset, 'left') - $v.r(parentOffset, 'left') ]);
}

// *** jquery.js ***
//  3495:     return results;

return results;
}
___.func(position$_lit$, 'position$_lit$');
;
var position$_lit = $v.dis(___.primFreeze(position$_lit$), 'position$_lit');
return position$_lit;
}).CALL___(), 'offsetParent', ___.frozenFunc(function () {
function offsetParent$_lit$($dis) {

// *** jquery.js ***
//  3499:     var offsetParent = this[0].offsetParent;

var offsetParent = $v.r($v.r($dis, 0), 'offsetParent');

// *** jquery.js ***
//  3500:     while ( offsetParent && (!/^body|html$/i.test(offsetParent.tagName) && jQuery.css(offsetParent, 'position') == 'static') )
//  3501:       offsetParent = offsetParent.offsetParent;

while (offsetParent && (!$v.cm($v.construct(RegExp, [ '^body|html$', 'i' ]), 'test', [ $v.r(offsetParent, 'tagName') ]) && $v.cm(jQuery, 'css', [ offsetParent, 'position' ]) == 'static')) offsetParent = $v.r(offsetParent, 'offsetParent');

// *** jquery.js ***
//  3502:     return jQuery(offsetParent);

return $v.cf(jQuery, [ offsetParent ]);
}
___.func(offsetParent$_lit$, 'offsetParent$_lit$');
;
var offsetParent$_lit = $v.dis(___.primFreeze(offsetParent$_lit$), 'offsetParent$_lit');
return offsetParent$_lit;
}).CALL___() ]) ]);

// *** jquery.js ***
//  3508: jQuery.each( ['Left', 'Top'], function(i, name) {

$v.cm(jQuery, 'each', [ [ 'Left', 'Top' ], $v.dis(___.frozenFunc(function ($dis, i, name) {

// *** jquery.js ***
//  3509:   var method = 'scroll' + name;

var method = 'scroll' + name;

// *** jquery.js ***
//  3511:   jQuery.fn[ method ] = function(val) {

$v.s($v.r(jQuery, 'fn'), method, $v.dis(___.frozenFunc(function ($dis, val) {

// *** jquery.js ***
//  3512:     if (!this[0]) return;

if (!$v.r($dis, 0)) return;

// *** jquery.js ***
//  3514:     return val != undefined ?
//  3515: 
//  3516:       // Set the scroll offset
//  3517:       this.each(function() {
//  3518:         this == window || this == document ?
//  3519:           window.scrollTo(
//  3520:             !i ? val : jQuery(window).scrollLeft(),
//  3521:              i ? val : jQuery(window).scrollTop()
//  3522:           ) :
//  3524:       }) :
//  3525: 
//  3526:       // Return the scroll offset
//  3527:       this[0] == window || this[0] == document ?
//  3528:         self[ i ? 'pageYOffset' : 'pageXOffset' ] ||
//  3529:           jQuery.boxModel && document.documentElement[ method ] ||
//  3530:           document.body[ method ] :
//  3531:         this[0][ method ];

return val != $v.ro('undefined') ? $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$dis == $v.ro('window') || $dis == $v.ro('document') ? $v.cm($v.ro('window'), 'scrollTo', [ !i? val: $v.cm($v.cf(jQuery, [ $v.ro('window') ]), 'scrollLeft', [ ]), i? val: $v.cm($v.cf(jQuery, [ $v.ro('window') ]), 'scrollTop', [ ]) ]): $v.s($dis, method, val);

// *** jquery.js ***
//  3523:           this[ method ] = val;

})) ]): $v.r($dis, 0) == $v.ro('window') || $v.r($dis, 0) == $v.ro('document') ? $v.r($v.ro('self'), i? 'pageYOffset': 'pageXOffset') || $v.r(jQuery, 'boxModel') && $v.r($v.r($v.ro('document'), 'documentElement'), method) || $v.r($v.r($v.ro('document'), 'body'), method): $v.r($v.r($dis, 0), method);
})));

// *** jquery.js ***
//  3532:   };

})) ]);

// *** jquery.js ***
//  3535: jQuery.each([ "Height", "Width" ], function(i, name){

$v.cm(jQuery, 'each', [ [ 'Height', 'Width' ], $v.dis(___.frozenFunc(function ($dis, i, name) {

// *** jquery.js ***
//  3537:   var tl = i ? "Left"  : "Top",  // top or left
//  3538:     br = i ? "Right" : "Bottom"; // bottom or right

var tl = i? 'Left': 'Top', br = i? 'Right': 'Bottom';

// *** jquery.js ***
//  3541:   jQuery.fn["inner" + name] = function(){

$v.s($v.r(jQuery, 'fn'), 'inner' + name, $v.dis(___.frozenFunc(function ($dis) {

// *** jquery.js ***
//  3542:     return this[ name.toLowerCase() ]() +
//  3543:       num(this, "padding" + tl) +
//  3544:       num(this, "padding" + br);

return $v.cm($dis, $v.cm(name, 'toLowerCase', [ ]), [ ]) + $v.cf(num, [ $dis, 'padding' + tl ]) + $v.cf(num, [ $dis, 'padding' + br ]);
})));

// *** jquery.js ***
//  3548:   jQuery.fn["outer" + name] = function(margin) {

$v.s($v.r(jQuery, 'fn'), 'outer' + name, $v.dis(___.frozenFunc(function ($dis, margin) {

// *** jquery.js ***
//  3549:     return this["inner" + name]() +
//  3550:       num(this, "border" + tl + "Width") +
//  3551:       num(this, "border" + br + "Width") +
//  3552:       (margin ?
//  3553:         num(this, "margin" + tl) + num(this, "margin" + br) : 0);

return $v.cm($dis, 'inner' + name, [ ]) + $v.cf(num, [ $dis, 'border' + tl + 'Width' ]) + $v.cf(num, [ $dis, 'border' + br + 'Width' ]) + (margin? $v.cf(num, [ $dis, 'margin' + tl ]) + $v.cf(num, [ $dis, 'margin' + br ]): 0);
})));

// *** jquery.js ***
//  3554:   };

})) ]);

// *** jquery.js ***
//  3556: });})();

})), [ ]);
}
} catch (ex___) {
___.getNewModuleHandler().handleUncaughtException(ex___, $v.ro('onerror'), 'jquery.js', '1');
}
IMPORTS___.htmlEmitter___.pc('\n\t');

// *** data/testrunner.js ***
//     1: var _config = {
//     2: 	fixture: null,
//     3: 	Test: [],
//     4: 	stats: {
//     5: 		all: 0,
//     6: 		bad: 0
//     7: 	},
//     8: 	queue: [],
//     9: 	blocking: true,
//    10: 	timeout: null,
//    11: 	expected: null,
//    13: 	asyncTimeout: 2 // seconds for async timeout
//    14: };
//    15: 
//    16: //restrict modules/tests by get parameters
//    19: 
//    21: 
//    28: 
//    36: 
//    43: 	// Disabled, caused too many random errors
//    44: 	//_config.timeout = setTimeout(handler, _config.asyncTimeout * 1000);
//    47: 	// A slight delay, to avoid any current callbacks
//    55: 
//    75: 
//    91: 
//   166: 
//   167: // call on start of module test to prepend name to all tests
//   171: 
//   172: /**
//   173:  * Specify the number of expected assertions to gurantee that failed test (no assertions are run at all) don't slip through.
//   174:  �/
//   177: }
//   178: 
//   179: /**
//   180:  * Resets the test setup. Useful for tests that modify the DOM.
//   181:  �/
//   186: }
//   187: 
//   188: /**
//   189:  * Asserts true.
//   190:  * �example ok( $("a").size() > 5, "There must be at least 5 anchors" );
//   191:  �/
//   194: }
//   195: 
//   196: /**
//   197:  * Asserts that two arrays are the same
//   198:  �/
//   211: }
//   212: 
//   213: /**
//   214:  * Asserts that two objects are equivalent
//   215:  �/
//   232: 
//   250: 
//   251: /**
//   252:  * Returns an array of elements with the given IDs, eg.
//   253:  * �example q("main", "foo", "bar")
//   254:  * �result [<div id="main">, <span id="foo">, <input id="bar">]
//   255:  �/
//   262: 
//   263: /**
//   264:  * Asserts that a select matches the given IDs
//   265:  * �example t("Check for something", "//[a]", ["foo", "baar"]);
//   266:  * �result returns true if "//[a]" return two elements with the IDs 'foo' and 'baar'
//   267:  �/
//   275: 
//   276: /**
//   277:  * Add random number to url to stop IE from caching
//   278:  *
//   279:  * �example url("data/test.html")
//   280:  * �result "data/test.html?10538358428943"
//   281:  *
//   282:  * �example url("data/test.php?foo=bar")
//   283:  * �result "data/test.php?foo=bar&10538358345554"
//   284:  �/
//   288: 
//   289: /**
//   290:  * Checks that the first two arguments are equal, with an optional message.
//   291:  * Prints out both expected and actual values on failure.
//   292:  *
//   293:  * Prefered to ok( expected == actual, message )
//   294:  *
//   295:  * �example equals( "Expected 2 characters.", v.formatMessage("Expected {0} characters.", 2) );
//   296:  *
//   297:  * �param Object actual
//   298:  * �param Object expected
//   299:  * �param String message (optional)
//   300:  �/
//   306: 
//   307: /**
//   308:  * Trigger an event on an element.
//   309:  *
//   310:  * �example triggerEvent( document.body, "click" );
//   311:  *
//   312:  * �param DOMElement elem
//   313:  * �param String type
//   314:  �/

try {
{
$v.so('synchronize', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//    23: 	_config.queue[_config.queue.length] = callback;
//    25: 		process();
//    26: 	}

function synchronize$_caller($dis, callback) {
$v.s($v.r($v.ro('_config'), 'queue'), $v.r($v.r($v.ro('_config'), 'queue'), 'length'), callback);

// *** data/testrunner.js ***
//    24: 	if(!_config.blocking) {

if (!$v.r($v.ro('_config'), 'blocking')) {
$v.cf($v.ro('process'), [ ]);
}
}
___.func(synchronize$_caller, 'synchronize$_caller');
var synchronize;
;

// *** data/testrunner.js ***
//    22: function synchronize(callback) {

synchronize = $v.dis(___.primFreeze(synchronize$_caller), 'synchronize');
return synchronize;
}).CALL___());
$v.so('process', ___.frozenFunc(function () {
function process$_caller($dis) {

// *** data/testrunner.js ***
//    30: 	while(_config.queue.length && !_config.blocking) {
//    32: 		_config.queue = _config.queue.slice(1);
//    33: 		call();
//    34: 	}

while ($v.r($v.r($v.ro('_config'), 'queue'), 'length') && !$v.r($v.ro('_config'), 'blocking')) {

// *** data/testrunner.js ***
//    31: 		var call = _config.queue[0];

var call = $v.r($v.r($v.ro('_config'), 'queue'), 0);
$v.s($v.ro('_config'), 'queue', $v.cm($v.r($v.ro('_config'), 'queue'), 'slice', [ 1 ]));
$v.cf(call, [ ]);
}
}
___.func(process$_caller, 'process$_caller');
var process;
;

// *** data/testrunner.js ***
//    29: function process() {

process = $v.dis(___.primFreeze(process$_caller), 'process');
return process;
}).CALL___());
$v.so('stop', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//    37: function stop(allowFailure) {
//    38: 	_config.blocking = true;
//    42: 	};

function stop$_caller($dis, allowFailure) {
$v.s($v.ro('_config'), 'blocking', true);

// *** data/testrunner.js ***
//    39: 	var handler = allowFailure ? start : function() {
//    40: 		ok( false, "Test timed out" );

var handler = allowFailure? $v.ro('start'): $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ false, 'Test timed out' ]);
$v.cf($v.ro('start'), [ ]);

// *** data/testrunner.js ***
//    41: 		start();

}));
}
___.func(stop$_caller, 'stop$_caller');
var stop;
;
stop = $v.dis(___.primFreeze(stop$_caller), 'stop');
return stop;
}).CALL___());
$v.so('start', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//    48: 	setTimeout(function(){
//    51: 		_config.blocking = false;
//    53: 	}, 13);

function start$_caller($dis) {
$v.cf($v.ro('setTimeout'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** data/testrunner.js ***
//    49: 		if(_config.timeout)
//    50: 			clearTimeout(_config.timeout);

if ($v.r($v.ro('_config'), 'timeout')) $v.cf($v.ro('clearTimeout'), [ $v.r($v.ro('_config'), 'timeout') ]);
$v.s($v.ro('_config'), 'blocking', false);
$v.cf($v.ro('process'), [ ]);

// *** data/testrunner.js ***
//    52: 		process();

})), 13 ]);
}
___.func(start$_caller, 'start$_caller');
var start;
;

// *** data/testrunner.js ***
//    46: function start() {

start = $v.dis(___.primFreeze(start$_caller), 'start');
return start;
}).CALL___());
$v.so('validTest', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//    60: 

function validTest$_caller($dis, name) {

// *** data/testrunner.js ***
//    57: 	var filters = _config.filters;

var filters = $v.r($v.ro('_config'), 'filters');

// *** data/testrunner.js ***
//    58: 	if( !filters )
//    59: 		return true;

if (!filters) return true;

// *** data/testrunner.js ***
//    61: 	var i = filters.length,
//    62: 		run = false;

var i = $v.r(filters, 'length'), run = false;

// *** data/testrunner.js ***
//    63: 	while( i-- ){
//    72: 	}

while (i--) {

// *** data/testrunner.js ***
//    64: 		var filter = filters[i],
//    65: 			not = filter.charAt(0) == '!';

var filter = $v.r(filters, i), not = $v.cm(filter, 'charAt', [ 0 ]) == '!';

// *** data/testrunner.js ***
//    66: 		if( not ) 
//    67: 			filter = filter.slice(1);

if (not) filter = $v.cm(filter, 'slice', [ 1 ]);

// *** data/testrunner.js ***
//    68: 		if( name.indexOf(filter) != -1 )
//    69: 			return !not;

if ($v.cm(name, 'indexOf', [ filter ]) != -1) return !not;

// *** data/testrunner.js ***
//    70: 		if( not )
//    71: 			run = true;

if (not) run = true;
}

// *** data/testrunner.js ***
//    73: 	return run;

return run;
}
___.func(validTest$_caller, 'validTest$_caller');
var validTest;
;

// *** data/testrunner.js ***
//    56: function validTest( name ) {

validTest = $v.dis(___.primFreeze(validTest$_caller), 'validTest');
return validTest;
}).CALL___());
$v.so('runTest', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//    77: 	_config.blocking = false;
//    80: 	_config.ajaxSettings = $.ajaxSettings;
//    81: 	synchronize(function() {
//    84: 			time, ' milliseconds.<br/>',
//    86: 			.join(''))
//    87: 			.appendTo("body");
//    89: 	});

function runTest$_caller($dis) {
$v.s($v.ro('_config'), 'blocking', false);

// *** data/testrunner.js ***
//    78: 	var time = new Date();

var time = $v.construct($v.ro('Date'), [ ]);

// *** data/testrunner.js ***
//    79: 	_config.fixture = document.getElementById('main').innerHTML;

$v.s($v.ro('_config'), 'fixture', $v.r($v.cm($v.ro('document'), 'getElementById', [ 'main' ]), 'innerHTML'));
$v.s($v.ro('_config'), 'ajaxSettings', $v.r($v.ro('$'), 'ajaxSettings'));
$v.cf($v.ro('synchronize'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** data/testrunner.js ***
//    82: 		time = new Date() - time;

time = $v.construct($v.ro('Date'), [ ]) - time;

// *** data/testrunner.js ***
//    83: 		$("<div>").html(['<p class="result">Tests completed in ',
//    85: 			_config.stats.bad, ' tests of ', _config.stats.all, ' failed.<�p>']

$v.cm($v.cm($v.cf($v.ro('$'), [ '\x3cdiv\x3e' ]), 'html', [ $v.cm([ '\x3cp class=\"result\"\x3eTests completed in ', time, ' milliseconds.\x3cbr/\x3e', $v.r($v.r($v.ro('_config'), 'stats'), 'bad'), ' tests of ', $v.r($v.r($v.ro('_config'), 'stats'), 'all'), ' failed.\x3c/p\x3e' ], 'join', [ '' ]) ]), 'appendTo', [ 'body' ]);
$v.cm($v.cf($v.ro('$'), [ '#banner' ]), 'addClass', [ $v.r($v.r($v.ro('_config'), 'stats'), 'bad') ? 'fail': 'pass' ]);

// *** data/testrunner.js ***
//    88: 		$("#banner").addClass(_config.stats.bad ? "fail" : "pass");

})) ]);
}
___.func(runTest$_caller, 'runTest$_caller');
var runTest;
;

// *** data/testrunner.js ***
//    76: function runTest() {

runTest = $v.dis(___.primFreeze(runTest$_caller), 'runTest');
return runTest;
}).CALL___());
$v.so('test', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//    92: function test(name, callback, nowait) {
//    95: 		
//    98: 		
//    99: 	synchronize(function() {
//   100: 		_config.Test = [];
//   111: 	});
//   112: 	synchronize(function() {
//   113: 		reset();
//   114: 		
//   115: 		// don't output pause tests
//   117: 		
//   121: 		_config.expected = null;
//   122: 		
//   125: 		ol.style.display = "none";
//   140: 	
//   142: 		li.className = state;
//   143: 	
//   146: 		b.onclick = function(){
//   152: 		};
//   153: 		$(b).dblclick(function(event) {
//   159: 		});
//   160: 		li.appendChild( b );
//   161: 		li.appendChild( ol );
//   162: 	
//   164: 	});

function test$_caller($dis, name, callback, nowait) {

// *** data/testrunner.js ***
//    93: 	if(_config.currentModule)
//    94: 		name = _config.currentModule + " module: " + name;

if ($v.r($v.ro('_config'), 'currentModule')) name = $v.r($v.ro('_config'), 'currentModule') + ' module: ' + name;

// *** data/testrunner.js ***
//    96: 	if ( !validTest(name) )
//    97: 		return;

if (!$v.cf($v.ro('validTest'), [ name ])) return;
$v.cf($v.ro('synchronize'), [ $v.dis(___.frozenFunc(function ($dis) {
$v.s($v.ro('_config'), 'Test', [ ]);

// *** data/testrunner.js ***
//   101: 		try {
//   102: 			callback();
//   110: 		}

try {
$v.cf(callback, [ ]);
} catch (ex___) {

// *** data/testrunner.js ***
//   103: 		} catch(e) {

try {
throw ___.tameException(ex___);
} catch (e) {

// *** data/testrunner.js ***
//   104: 			if( typeof console != "undefined" && console.error && console.warn ) {
//   105: 				console.error("Test " + name + " died, exception and test follows");
//   106: 				console.error(e);
//   107: 				console.warn(callback.toString());
//   108: 			}

if ($v.typeOf($v.ros('console')) != 'undefined' && $v.r($v.ro('console'), 'error') && $v.r($v.ro('console'), 'warn')) {
$v.cm($v.ro('console'), 'error', [ 'Test ' + name + ' died, exception and test follows' ]);
$v.cm($v.ro('console'), 'error', [ e ]);
$v.cm($v.ro('console'), 'warn', [ $v.cm(callback, 'toString', [ ]) ]);
}

// *** data/testrunner.js ***
//   109: 			_config.Test.push( [ false, "Died on test #" + (_config.Test.length+1) + ": " + e.message ] );

$v.cm($v.r($v.ro('_config'), 'Test'), 'push', [ [ false, 'Died on test #' + ($v.r($v.r($v.ro('_config'), 'Test'), 'length') + 1) + ': ' + $v.r(e, 'message') ] ]);
}
}
})) ]);
$v.cf($v.ro('synchronize'), [ $v.dis(___.frozenFunc(function ($dis) {
var x0___;
var x1___;
var x2___;
var x3___;
$v.cf($v.ro('reset'), [ ]);

// *** data/testrunner.js ***
//   116: 		if(nowait) return;

if (nowait) return;

// *** data/testrunner.js ***
//   118: 		if(_config.expected && _config.expected != _config.Test.length) {
//   120: 		}

if ($v.r($v.ro('_config'), 'expected') && $v.r($v.ro('_config'), 'expected') != $v.r($v.r($v.ro('_config'), 'Test'), 'length')) {

// *** data/testrunner.js ***
//   119: 			_config.Test.push( [ false, "Expected " + _config.expected + " assertions, but " + _config.Test.length + " were run" ] );

$v.cm($v.r($v.ro('_config'), 'Test'), 'push', [ [ false, 'Expected ' + $v.r($v.ro('_config'), 'expected') + ' assertions, but ' + $v.r($v.r($v.ro('_config'), 'Test'), 'length') + ' were run' ] ]);
}
$v.s($v.ro('_config'), 'expected', null);

// *** data/testrunner.js ***
//   123: 		var good = 0, bad = 0;

var good = 0, bad = 0;

// *** data/testrunner.js ***
//   124: 		var ol = document.createElement("ol");

var ol = $v.cm($v.ro('document'), 'createElement', [ 'ol' ]);
$v.s($v.r(ol, 'style'), 'display', 'none');

// *** data/testrunner.js ***
//   126: 		var li = "", state = "pass";

var li = '', state = 'pass';

// *** data/testrunner.js ***
//   127: 		for ( var i = 0; i < _config.Test.length; i++ ) {
//   129: 			li.className = _config.Test[i][0] ? "pass" : "fail";
//   130: 			li.innerHTML = _config.Test[i][1];
//   131: 			ol.appendChild( li );
//   132: 			
//   133: 			_config.stats.all++;
//   139: 		}

for (var i = 0; i < $v.r($v.r($v.ro('_config'), 'Test'), 'length'); i++) {

// *** data/testrunner.js ***
//   128: 			var li = document.createElement("li");

var li = $v.cm($v.ro('document'), 'createElement', [ 'li' ]);
$v.s(li, 'className', $v.r($v.r($v.r($v.ro('_config'), 'Test'), i), 0) ? 'pass': 'fail');
$v.s(li, 'innerHTML', $v.cf($v.ro('escape'), [$v.r($v.r($v.r($v.ro('_config'), 'Test'), i), 1)]));
$v.cm(ol, 'appendChild', [ li ]);
x0___ = $v.r($v.ro('_config'), 'stats'), x1___ = +$v.r(x0___, 'all'), $v.s(x0___, 'all', x1___ + 1), x1___;

// *** data/testrunner.js ***
//   134: 			if ( !_config.Test[i][0] ) {
//   137: 				_config.stats.bad++;

if (!$v.r($v.r($v.r($v.ro('_config'), 'Test'), i), 0)) {

// *** data/testrunner.js ***
//   135: 				state = "fail";

state = 'fail';

// *** data/testrunner.js ***
//   136: 				bad++;

bad++;
x2___ = $v.r($v.ro('_config'), 'stats'), x3___ = +$v.r(x2___, 'bad'), $v.s(x2___, 'bad', x3___ + 1), x3___;

// *** data/testrunner.js ***
//   138: 			} else good++;

} else good++;
}

// *** data/testrunner.js ***
//   141: 		var li = document.createElement("li");

var li = $v.cm($v.ro('document'), 'createElement', [ 'li' ]);
$v.s(li, 'className', state);

// *** data/testrunner.js ***
//   144: 		var b = document.createElement("strong");

var b = $v.cm($v.ro('document'), 'createElement', [ 'strong' ]);

// *** data/testrunner.js ***
//   145: 		b.innerHTML = name + " <b style='color:black;'>(<b class='fail'>" + bad + "<�b>, <b class='pass'>" + good + "<�b>, " + _config.Test.length + ")<�b>";

$v.s(b, 'innerHTML', name + ' \x3cb style=\'color:black;\'\x3e(\x3cb class=\'fail\'\x3e' + bad + '\x3c/b\x3e, \x3cb class=\'pass\'\x3e' + good + '\x3c/b\x3e, ' + $v.r($v.r($v.ro('_config'), 'Test'), 'length') + ')\x3c/b\x3e');
$v.s(b, 'onclick', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   150: 			else
//   151: 				n.style.display = "none";

function onclick$_meth$($dis) {

// *** data/testrunner.js ***
//   147: 			var n = this.nextSibling;

var n = $v.r($dis, 'nextSibling');

// *** data/testrunner.js ***
//   148: 			if ( jQuery.css( n, "display" ) == "none" )
//   149: 				n.style.display = "block";

if ($v.cm($v.ro('jQuery'), 'css', [ n, 'display' ]) == 'none') $v.s($v.r(n, 'style'), 'display', 'block');
else $v.s($v.r(n, 'style'), 'display', 'none');
}
___.func(onclick$_meth$, 'onclick$_meth$');
;
var onclick$_meth = $v.dis(___.primFreeze(onclick$_meth$), 'onclick$_meth');
return onclick$_meth;
}).CALL___());
$v.cm($v.cf($v.ro('$'), [ b ]), 'dblclick', [ $v.dis(___.frozenFunc(function ($dis, event) {

// *** data/testrunner.js ***
//   154: 			var target = jQuery(event.target).filter("strong").clone();

var target = $v.cm($v.cm($v.cf($v.ro('jQuery'), [ $v.r(event, 'target') ]), 'filter', [ 'strong' ]), 'clone', [ ]);

// *** data/testrunner.js ***
//   155: 			if ( target.length ) {
//   156: 				target.children().remove();
//   157: 				location.href = location.href.match(/^(.+?)(\?.*)?$/)[1] + "?" + encodeURIComponent($.trim(target.text()));
//   158: 			}

if ($v.r(target, 'length')) {
$v.cm($v.cm(target, 'children', [ ]), 'remove', [ ]);
$v.s($v.ro('location'), 'href', $v.r($v.cm($v.r($v.ro('location'), 'href'), 'match', [ $v.construct(RegExp, [ '^(.+?)(\\?.*)?$' ]) ]), 1) + '?' + $v.cf($v.ro('encodeURIComponent'), [ $v.cm($v.ro('$'), 'trim', [ $v.cm(target, 'text', [ ]) ]) ]));
}
})) ]);
$v.cm(li, 'appendChild', [ b ]);
$v.cm(li, 'appendChild', [ ol ]);
$v.cm($v.cm($v.ro('document'), 'getElementById', [ 'tests' ]), 'appendChild', [ li ]);

// *** data/testrunner.js ***
//   163: 		document.getElementById("tests").appendChild( li );		

})) ]);
}
___.func(test$_caller, 'test$_caller');
var test;
;
test = $v.dis(___.primFreeze(test$_caller), 'test');
return test;
}).CALL___());
$v.so('module', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   168: function module(moduleName) {
//   169: 	_config.currentModule = moduleName;

function module$_caller($dis, moduleName) {
$v.s($v.ro('_config'), 'currentModule', moduleName);
}
___.func(module$_caller, 'module$_caller');
var module;
;
module = $v.dis(___.primFreeze(module$_caller), 'module');
return module;
}).CALL___());
$v.so('expect', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   176: 	_config.expected = asserts;

function expect$_caller($dis, asserts) {
$v.s($v.ro('_config'), 'expected', asserts);
}
___.func(expect$_caller, 'expect$_caller');
var expect;
;

// *** data/testrunner.js ***
//   175: function expect(asserts) {

expect = $v.dis(___.primFreeze(expect$_caller), 'expect');
return expect;
}).CALL___());
$v.so('reset', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   183: 	$("#main").html( _config.fixture );
//   184: 	$.event.global = {};

function reset$_caller($dis) {
$v.cm($v.cf($v.ro('$'), [ '#main' ]), 'html', [ $v.r($v.ro('_config'), 'fixture') ]);
$v.s($v.r($v.ro('$'), 'event'), 'global', ___.initializeMap([ ]));

// *** data/testrunner.js ***
//   185: 	$.ajaxSettings = $.extend({}, _config.ajaxSettings);

$v.s($v.ro('$'), 'ajaxSettings', $v.cm($v.ro('$'), 'extend', [ ___.initializeMap([ ]), $v.r($v.ro('_config'), 'ajaxSettings') ]));
}
___.func(reset$_caller, 'reset$_caller');
var reset;
;

// *** data/testrunner.js ***
//   182: function reset() {

reset = $v.dis(___.primFreeze(reset$_caller), 'reset');
return reset;
}).CALL___());
$v.so('ok', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   192: function ok(a, msg) {
//   193: 	_config.Test.push( [ !!a, msg ] );

function ok$_caller($dis, a, msg) {
$v.cm($v.r($v.ro('_config'), 'Test'), 'push', [ [ ! (!a), msg ] ]);
}
___.func(ok$_caller, 'ok$_caller');
var ok;
;
ok = $v.dis(___.primFreeze(ok$_caller), 'ok');
return ok;
}).CALL___());
$v.so('isSet', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   199: function isSet(a, b, msg) {
//   205: 	} else
//   206: 		ret = false;
//   209: 	else 
//   210: 		_config.Test.push( [ ret, msg ] );

function isSet$_caller($dis, a, b, msg) {

// *** data/testrunner.js ***
//   200: 	var ret = true;

var ret = true;

// *** data/testrunner.js ***
//   201: 	if ( a && b && a.length != undefined && a.length == b.length ) {

if (a && b && $v.r(a, 'length') != $v.ro('undefined') && $v.r(a, 'length') == $v.r(b, 'length')) {

// *** data/testrunner.js ***
//   202: 		for ( var i = 0; i < a.length; i++ )
//   203: 			if ( a[i] != b[i] )
//   204: 				ret = false;

for (var i = 0; i < $v.r(a, 'length'); i++) if ($v.r(a, i) != $v.r(b, i)) ret = false;
} else ret = false;

// *** data/testrunner.js ***
//   207: 	if ( !ret )
//   208: 		_config.Test.push( [ ret, msg + " expected: " + serialArray(b) + " result: " + serialArray(a) ] );

if (!ret) $v.cm($v.r($v.ro('_config'), 'Test'), 'push', [ [ ret, msg + ' expected: ' + $v.cf($v.ro('serialArray'), [ b ]) + ' result: ' + $v.cf($v.ro('serialArray'), [ a ]) ] ]);
else $v.cm($v.r($v.ro('_config'), 'Test'), 'push', [ [ ret, msg ] ]);
}
___.func(isSet$_caller, 'isSet$_caller');
var isSet;
;
isSet = $v.dis(___.primFreeze(isSet$_caller), 'isSet');
return isSet;
}).CALL___());
$v.so('isObj', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   216: function isObj(a, b, msg) {
//   218: 	
//   223: 
//   227: 	} else
//   228: 		ret = false;
//   229: 
//   230:     _config.Test.push( [ ret, msg ] );

function isObj$_caller($dis, a, b, msg) {
var $caja$37;
var $caja$38;
var $caja$39;
var $caja$40;

// *** data/testrunner.js ***
//   217: 	var ret = true;

var ret = true;

// *** data/testrunner.js ***
//   219: 	if ( a && b ) {

if (a && b) {
{
$caja$37 = $v.keys(a);
for ($caja$38 = 0; $caja$38 < ($caja$37.length_canRead___? $caja$37.length: ___.readPub($caja$37, 'length')); ++$caja$38) {

// *** data/testrunner.js ***
//   220: 		for ( var i in a )

var i = ___.readPub($caja$37, $caja$38);

// *** data/testrunner.js ***
//   221: 			if ( a[i] != b[i] )
//   222: 				ret = false;

if ($v.r(a, i) != $v.r(b, i)) ret = false;
}
}
{
$caja$39 = $v.keys(b);

// *** data/testrunner.js ***
//   224: 		for ( i in b )

for ($caja$40 = 0; $caja$40 < ($caja$39.length_canRead___? $caja$39.length: ___.readPub($caja$39, 'length')); ++$caja$40) {
i = ___.readPub($caja$39, $caja$40);

// *** data/testrunner.js ***
//   225: 			if ( a[i] != b[i] )
//   226: 				ret = false;

if ($v.r(a, i) != $v.r(b, i)) ret = false;
}
}
} else ret = false;
$v.cm($v.r($v.ro('_config'), 'Test'), 'push', [ [ ret, msg ] ]);
}
___.func(isObj$_caller, 'isObj$_caller');
var isObj;
;
isObj = $v.dis(___.primFreeze(isObj$_caller), 'isObj');
return isObj;
}).CALL___());
$v.so('serialArray', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   235: 	
//   247: 

function serialArray$_caller($dis, a) {

// *** data/testrunner.js ***
//   234: 	var r = [];

var r = [ ];

// *** data/testrunner.js ***
//   236: 	if ( a && a.length )
//   237:         for ( var i = 0; i < a.length; i++ ) {
//   243:             } else
//   244:                 str = a[i];
//   245:             r.push( str );
//   246:         }

if (a && $v.r(a, 'length')) for (var i = 0; i < $v.r(a, 'length'); i++) {

// *** data/testrunner.js ***
//   238:             var str = a[i].nodeName;

var str = $v.r($v.r(a, i), 'nodeName');

// *** data/testrunner.js ***
//   239:             if ( str ) {

if (str) {

// *** data/testrunner.js ***
//   240:                 str = str.toLowerCase();

str = $v.cm(str, 'toLowerCase', [ ]);

// *** data/testrunner.js ***
//   241:                 if ( a[i].id )
//   242:                     str += "#" + a[i].id;

if ($v.r($v.r(a, i), 'id')) str = str + ('#' + $v.r($v.r(a, i), 'id'));
} else str = $v.r(a, i);
$v.cm(r, 'push', [ str ]);
}

// *** data/testrunner.js ***
//   248: 	return "[ " + r.join(", ") + " ]";

return '[ ' + $v.cm(r, 'join', [ ', ' ]) + ' ]';
}
___.func(serialArray$_caller, 'serialArray$_caller');
var serialArray;
;

// *** data/testrunner.js ***
//   233: function serialArray( a ) {

serialArray = $v.dis(___.primFreeze(serialArray$_caller), 'serialArray');
return serialArray;
}).CALL___());
$v.so('q', ___.frozenFunc(function () {
function q$_caller($dis) {
var a___ = ___.args(arguments);

// *** data/testrunner.js ***
//   257: 	var r = [];

var r = [ ];

// *** data/testrunner.js ***
//   258: 	for ( var i = 0; i < arguments.length; i++ )
//   259: 		r.push( document.getElementById( arguments[i] ) );

for (var i = 0; i < $v.r(Array.slice(a___, 1), 'length'); i++) $v.cm(r, 'push', [ $v.cm($v.ro('document'), 'getElementById', [ $v.r(Array.slice(a___, 1), i) ]) ]);

// *** data/testrunner.js ***
//   260: 	return r;

return r;
}
___.func(q$_caller, 'q$_caller');
var q;
;
q = $v.dis(___.primFreeze(q$_caller), 'q');

// *** data/testrunner.js ***
//   256: function q() {

return q;
}).CALL___());
$v.so('t', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   268: function t(a,b,c) {

function t$_caller($dis, a, b, c) {

// *** data/testrunner.js ***
//   269: 	var f = jQuery(b);

var f = $v.cf($v.ro('jQuery'), [ b ]);

// *** data/testrunner.js ***
//   270: 	var s = "";

var s = '';

// *** data/testrunner.js ***
//   271: 	for ( var i = 0; i < f.length; i++ )
//   272: 		s += (s && ",") + '"' + f[i].id + '"';

for (var i = 0; i < $v.r(f, 'length'); i++) s = s + ((s && ',') + '\"' + $v.r($v.r(f, i), 'id') + '\"');

// *** data/testrunner.js ***
//   273: 	isSet(f, q.apply(q,c), a + " (" + b + ")");

$v.cf($v.ro('isSet'), [ f, $v.cm($v.ro('q'), 'apply', [ $v.ro('q'), c ]), a + ' (' + b + ')' ]);
}
___.func(t$_caller, 't$_caller');
var t;
;
t = $v.dis(___.primFreeze(t$_caller), 't');
return t;
}).CALL___());
$v.so('url', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   285: function url(value) {

function url$_caller($dis, value) {

// *** data/testrunner.js ***
//   286: 	return value + (/\?/.test(value) ? "&" : "?") + new Date().getTime() + "" + parseInt(Math.random()*100000);

return value + ($v.cm($v.construct(RegExp, [ '\\?' ]), 'test', [ value ]) ? '\x26': '?') + $v.cm($v.construct($v.ro('Date'), [ ]), 'getTime', [ ]) + '' + $v.cf($v.ro('parseInt'), [ $v.cm($v.ro('Math'), 'random', [ ]) * 100000 ]);
}
___.func(url$_caller, 'url$_caller');
var url;
;
url = $v.dis(___.primFreeze(url$_caller), 'url');
return url;
}).CALL___());
$v.so('equals', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   301: function equals(actual, expected, message) {

function equals$_caller($dis, actual, expected, message) {

// *** data/testrunner.js ***
//   302: 	var result = expected == actual;

var result = expected == actual;

// *** data/testrunner.js ***
//   303: 	message = message || (result ? "okay" : "failed");

message = message || (result? 'okay': 'failed');

// *** data/testrunner.js ***
//   304: 	_config.Test.push( [ result, result ? message + ": " + expected : message + " expected: " + expected + " actual: " + actual ] );

$v.cm($v.r($v.ro('_config'), 'Test'), 'push', [ [ result, result? message + ': ' + expected: message + ' expected: ' + expected + ' actual: ' + actual ] ]);
}
___.func(equals$_caller, 'equals$_caller');
var equals;
;
equals = $v.dis(___.primFreeze(equals$_caller), 'equals');
return equals;
}).CALL___());
$v.so('triggerEvent', ___.frozenFunc(function () {

// *** data/testrunner.js ***
//   319: 			0, 0, 0, 0, 0, false, false, false, false, 0, null);
//   320: 		elem.dispatchEvent( event );
//   322: 		elem.fireEvent("on"+type);
//   323: 	}

function triggerEvent$_caller($dis, elem, type, event) {

// *** data/testrunner.js ***
//   316: 	if ( jQuery.browser.mozilla || jQuery.browser.opera ) {

if ($v.r($v.r($v.ro('jQuery'), 'browser'), 'mozilla') || $v.r($v.r($v.ro('jQuery'), 'browser'), 'opera')) {

// *** data/testrunner.js ***
//   317: 		event = document.createEvent("MouseEvents");

event = $v.cm($v.ro('document'), 'createEvent', [ 'MouseEvents' ]);

// *** data/testrunner.js ***
//   318: 		event.initMouseEvent(type, true, true, elem.ownerDocument.defaultView,

$v.cm(event, 'initMouseEvent', [ type, true, true, $v.r($v.r(elem, 'ownerDocument'), 'defaultView'), 0, 0, 0, 0, 0, false, false, false, false, 0, null ]);
$v.cm(elem, 'dispatchEvent', [ event ]);

// *** data/testrunner.js ***
//   321: 	} else if ( jQuery.browser.msie ) {

} else if ($v.r($v.r($v.ro('jQuery'), 'browser'), 'msie')) {
$v.cm(elem, 'fireEvent', [ 'on' + type ]);
}
}
___.func(triggerEvent$_caller, 'triggerEvent$_caller');
var triggerEvent;
;

// *** data/testrunner.js ***
//   315: function triggerEvent( elem, type, event ) {

triggerEvent = $v.dis(___.primFreeze(triggerEvent$_caller), 'triggerEvent');
return triggerEvent;
}).CALL___());

// *** data/testrunner.js ***
//    12: 	currentModule: null,

$v.so('_config', ___.initializeMap([ 'fixture', null, 'Test', [ ], 'stats', ___.initializeMap([ 'all', 0, 'bad', 0 ]), 'queue', [ ], 'blocking', true, 'timeout', null, 'expected', null, 'currentModule', null, 'asyncTimeout', 2 ]));

// *** data/testrunner.js ***
//    17: _config.filters = location.search && location.search.length > 1 && 
//    18: 		$.map( location.search.slice(1).split('&'), decodeURIComponent );

$v.s($v.ro('_config'), 'filters', $v.r($v.ro('location'), 'search') && $v.r($v.r($v.ro('location'), 'search'), 'length') > 1 && $v.cm($v.ro('$'), 'map', [ $v.cm($v.cm($v.r($v.ro('location'), 'search'), 'slice', [ 1 ]), 'split', [ '\x26' ]), $v.ro('decodeURIComponent') ]));

// *** data/testrunner.js ***
//    20: var isLocal = !!(window.location.protocol == 'file:');

$v.so('isLocal', ! (! ($v.r($v.r($v.ro('window'), 'location'), 'protocol') == 'file:')));

// *** data/testrunner.js ***
//    27: }

;

// *** data/testrunner.js ***
//    35: }

;

// *** data/testrunner.js ***
//    45: }

;

// *** data/testrunner.js ***
//    54: }

;

// *** data/testrunner.js ***
//    74: }

;

// *** data/testrunner.js ***
//    90: }

;

// *** data/testrunner.js ***
//   165: }

;

// *** data/testrunner.js ***
//   170: }

;
;
;
;
;

// *** data/testrunner.js ***
//   231: }

;

// *** data/testrunner.js ***
//   249: }

;

// *** data/testrunner.js ***
//   261: }

;

// *** data/testrunner.js ***
//   274: }

;

// *** data/testrunner.js ***
//   287: }

;

// *** data/testrunner.js ***
//   305: }

;

// *** data/testrunner.js ***
//   324: }

;
}
} catch (ex___) {
___.getNewModuleHandler().handleUncaughtException(ex___, $v.ro('onerror'), 'data/testrunner.js', '1');
}
IMPORTS___.htmlEmitter___.pc('\n\t');

// *** unit/core.js ***
//     1: module("core");
//     2: 
//     4: 	expect(7);
//    12: });
//    13: 
//    14: test("$()", function() {
//    15: 	expect(8);
//    16: 
//    19: 
//    20: /*
//    21: 	// disabled since this test was doing nothing. i tried to fix it but i'm not sure
//    22: 	// what the expected behavior should even be. FF returns "\n" for the text node
//    23: 	// make sure this is handled
//    24: 	var crlfContainer = $('<p>\r\n<�p>');
//    25: 	var x = crlfContainer.contents().get(0).nodeValue;
//    26: 	equals( x, what???, "Check for \\r and \\n in jQuery()" );
//    27: �/
//    28: 
//    29: 	/* // Disabled until we add this functionality in
//    30: 	var pass = true;
//    31: 	try {
//    32: 		$("<div>Testing<�div>").appendTo(document.getElementById("iframe").contentDocument.body);
//    33: 	} catch(e){
//    34: 		pass = false;
//    35: 	}
//    36: 	ok( pass, "$('&lt;tag&gt;') needs optional document parameter to ease cross-frame DOM wrangling, see #968" );�/
//    37: 
//    44: 
//    45: 	// can actually yield more than one, when iframes are included, the window is an array as well
//    47: 
//    49: 
//    52: });
//    53: 
//    54: test("browser", function() {
//    55: 	expect(13);
//    88: });
//    89: 
//    91: 	expect(6);
//    92: 
//    95: 
//    99: 
//   100: 	jQuery = $ = old;
//   101: 
//   103: 
//   107: 
//   109: });
//   110: 
//   112: 	expect(21);
//   113: 
//   114: 	// Make sure that false values return false
//   120: 
//   121: 	// Check built-ins
//   122: 	// Safari uses "(Internal Function)"
//   127: 
//   128: 	// When stringified, this could be misinterpreted
//   131: 
//   132: 	// When stringified, this could be misinterpreted
//   135: 
//   136: 	// When stringified, this could be misinterpreted
//   139: 
//   140: 	// Make sure normal functions still work
//   143: 
//   144: //	var obj = document.createElement("object");
//   145: 
//   146: 	// Firefox says this is a function
//   147: 	//ok( !jQuery.isFunction(obj), "Object Element" );
//   149: 
//   150: 	// IE says this is an object
//   152: 
//   154: 
//   155: 	// Safari says this is a function
//   157: 
//   159: 
//   160: 	// Normal elements are reported ok everywhere
//   162: 
//   164: 	input.type = "text";
//   166: 
//   167: 	// IE says this is an object
//   169: 
//   171: 
//   175: 
//   176: 	// This serializes with the word 'function' in it
//   178: 
//   180: 
//   181: 	// Recursive function calls have lengths and array-like properties
//   191: 
//   192: 	callme(function(){
//   195: });
//   196: 
//   197: var foo = false;
//   198: 
//   200: 	expect(6);
//   201: 
//   202: 	reset();
//   203: 	foo = false;
//   209: 
//   210: 	reset();
//   212: 
//   213: 	reset();
//   214: 
//   217: 
//   219: });
//   220: 
//   222: 	expect(1);
//   223: 
//   227: });
//   228: 
//   242: 
//   243: test("length", function() {
//   244: 	expect(1);
//   246: });
//   247: 
//   248: test("size()", function() {
//   249: 	expect(1);
//   251: });
//   252: 
//   253: test("get()", function() {
//   254: 	expect(1);
//   256: });
//   257: 
//   259: 	expect(1);
//   261: });
//   262: 
//   264: 	expect(12);
//   268: 
//   269: 	// For the time being, we're discontinuing support for $(form.elements) since it's ambiguous in IE
//   270: 	// use $([]).add(form.elements) instead.
//   271: 	//equals( $([]).add($("#form")[0].elements).length, $($("#form")[0].elements).length, "Array in constructor must equals array in add()" );
//   272: 
//   276: 
//   280: 
//   283: 
//   284: 	// Added after #2811
//   289: });
//   290: 
//   292: 	expect(1);
//   300: });
//   301: 
//   303: 	expect(10);
//   304: 
//   307: 
//   316: 
//   317: 	// enabled since [5500]
//   320: });
//   321: 
//   323: 	expect(26);
//   343: 
//   346: 
//   347: 
//   348: 	// Related to [5574] and [5683]
//   350: 
//   353: 	
//   354: 	// body.setAttribute('foo', 'baz');
//   355: 	// equals( $body.attr('foo'), 'baz', 'Make sure the dom attribute is retrieved when no expando is found' );
//   357: 	
//   358: 	body.foo = 'bar';
//   360: 	
//   361: 	// $body.attr('foo','cool');
//   362: 	// equals( $body.attr('foo'), 'cool', 'Make sure that setting works well when both expando and dom attribute are available' );
//   364: 	
//   367: 	
//   369: });
//   370: 
//   382: 
//   384: 	expect(2);
//   387: });
//   388: 
//   390: 	expect(1);
//   394: 	});
//   396: });
//   397: 
//   399: 	expect(17);
//   401: 		fail = false;
//   409: 
//   410: 	// ok( $("#foo").attr({"width": null}), "Try to set an attribute to nothing" );
//   412: 	
//   427: 
//   428: 	// for #1070
//   429: 	// $("#name").attr('someAttr', '0');
//   430: 	// equals( $("#name").attr('someAttr'), '0', 'Set attribute to a string of "0"' );
//   432: 	// $("#name").attr('someAttr', 0);
//   433: 	// equals( $("#name").attr('someAttr'), 0, 'Set attribute to the number 0' );
//   435: 	// $("#name").attr('someAttr', 1);
//   436: 	// equals( $("#name").attr('someAttr'), 1, 'Set attribute to the number 1' );
//   438: 
//   439: 	// using contents will get comments regular, text, and comment nodes
//   441: 
//   442: 	// j.attr("name", "attrvalue");
//   443: 	// equals( j.attr("name"), "attrvalue", "Check node,textnode,comment for attr" );
//   446: 
//   447: 	reset();
//   448: 
//   458: 
//   468: });
//   469: 
//   485: 
//   487: 	expect(19);
//   488: 
//   490: 
//   496: 
//   505: 
//   511: 	});
//   514: });
//   515: 
//   517: 	expect(21);
//   523: 
//   532: 
//   538: 	});
//   541: 	// for #1438, IE throws JS error when filter exists but doesn't have opacity in it
//   546: 
//   547: 	// using contents will get comments regular, text, and comment nodes
//   551: 
//   552: 	// opera sometimes doesn't update 'display' correctly, see #2037
//   555: });
//   556: 
//   558: 	expect(4);
//   559: 
//   561: 	// IE6 was clearing "checked" in jQuery.css(elem, "height");
//   567: });
//   568: 
//   569: test("width()", function() {
//   570: 	expect(9);
//   571: 
//   573: 	$div.width(30);
//   575: 	$div.width(-1); // handle negative numbers by ignoring #1599
//   587: 	$div.hide();
//   589: 
//   591: 
//   595: });
//   596: 
//   597: test("height()", function() {
//   598: 	expect(8);
//   599: 
//   615: 	$div.hide();
//   617: 
//   619: });
//   620: 
//   621: test("text()", function() {
//   622: 	expect(1);
//   625: });
//   626: 
//   628: 	expect(8);
//   633: 
//   634: 	reset();
//   639: 
//   640: 	reset();
//   646: 	}).click();
//   647: 
//   648: 	// using contents will get comments regular, text, and comment nodes
//   653: });
//   654: 
//   656: 	expect(8);
//   665: 
//   666: 	reset();
//   673: });
//   674: 
//   676: 	expect(6);
//   682: 
//   683: 	reset();
//   689: });
//   690: 
//   692: 	expect(21);
//   697: 
//   698: 	reset();
//   702: 
//   703: 	reset();
//   707: 
//   708: 	reset();
//   712: 
//   713: 	reset();
//   716: 
//   717: 	reset();
//   720: 
//   721: 	reset();
//   725: 
//   726: 	reset();
//   729: 
//   730: 	reset();
//   737: 
//   739: 
//   740: 	reset();
//   743: 
//   744: 	reset();
//   747: 
//   750: 
//   753: 
//   754: 	reset();
//   757: 
//   758: 	reset();
//   759: 	$('form:last')
//   762: 
//   764: 
//   765: 	// using contents will get comments regular, text, and comment nodes
//   771: 	d.remove();
//   773: });
//   774: 
//   776: 	expect(6);
//   781: 
//   782: 	reset();
//   786: 
//   787: 	reset();
//   791: 
//   792: 	reset();
//   796: 
//   797: 	reset();
//   800: });
//   801: 
//   803: 	expect(5);
//   808: 
//   809: 	reset();
//   813: 
//   814: 	reset();
//   818: 
//   819: 	reset();
//   823: });
//   824: 
//   826: 	expect(6);
//   831: 
//   832: 	reset();
//   836: 
//   837: 	reset();
//   841: 
//   842: 	reset();
//   846: 
//   847: 	reset();
//   850: 
//   852: });
//   853: 
//   855: 	expect(4);
//   859: 
//   860: 	reset();
//   864: 
//   865: 	reset();
//   869: 
//   870: 	reset();
//   874: });
//   875: 
//   877: 	expect(4);
//   881: 
//   882: 	reset();
//   886: 
//   887: 	reset();
//   891: 
//   892: 	reset();
//   896: });
//   897: 
//   899: 	expect(4);
//   903: 
//   904: 	reset();
//   908: 
//   909: 	reset();
//   913: 
//   914: 	reset();
//   918: });
//   919: 
//   921: 	expect(4);
//   925: 
//   926: 	reset();
//   930: 
//   931: 	reset();
//   935: 
//   936: 	reset();
//   940: });
//   941: 
//   943: 	expect(10);
//   947: 
//   948: 	reset();
//   952: 
//   953: 	reset();
//   958: 
//   959: 	reset();
//   964: });
//   965: 
//   967: 	expect(10);
//   971: 
//   972: 	reset();
//   976: 
//   977: 	reset();
//   982: 
//   983: 	reset();
//   988: });
//   989: 
//   990: test("end()", function() {
//   991: 	expect(3);
//   994: 
//   996: 	x.parent();
//   998: });
//   999: 
//  1001: 	expect(2);
//  1003: 
//  1004: 	// using contents will get comments regular, text, and comment nodes
//  1007: });
//  1008: 
//  1009: test("clone()", function() {
//  1010: 	expect(20);
//  1015: 
//  1026: 
//  1027: 	// using contents will get comments regular, text, and comment nodes
//  1030: });
//  1031: 
//  1046: 
//  1048: 	expect(26);
//  1071: 
//  1072: 	// test is() with comma-seperated expressions
//  1077: });
//  1078: 
//  1080: 	expect(20);
//  1081: 
//  1091: 
//  1095: 
//  1099: 
//  1104: 
//  1108: 
//  1111: 
//  1114: 
//  1119: 
//  1122: 
//  1125: 
//  1128: 
//  1132: 
//  1136: 
//  1144: 
//  1150: });
//  1151: 
//  1152: test("val()", function() {
//  1153: 	expect(4);
//  1156: 	// ticket #1714 this caused a JS error in IE
//  1159: });
//  1160: 
//  1162: 	expect(4);
//  1167: 
//  1170: 
//  1171: 	// using contents will get comments regular, text, and comment nodes
//  1173: 	j.val("asdf");
//  1176: });
//  1177: 
//  1178: var scriptorder = 0;
//  1179: 
//  1181: 	expect(11);
//  1189: 
//  1190: 	reset();
//  1191: 	// using contents will get comments regular, text, and comment nodes
//  1194: 
//  1195: 	// this is needed, or the expando added by jQuery unique will yield a different html
//  1198: 
//  1202: 
//  1203: 	stop();
//  1204: 
//  1206: 
//  1208: 
//  1209: 	// it was decided that waiting to execute ALL scripts makes sense since nested ones have to wait anyway so this test case is changed, see #1959
//  1211: 
//  1213: });
//  1214: 
//  1215: test("filter()", function() {
//  1216: 	expect(6);
//  1221: 
//  1222: 	// using contents will get comments regular, text, and comment nodes
//  1226: });
//  1227: 
//  1228: test("not()", function() {
//  1229: 	expect(8);
//  1237: 
//  1240: });
//  1241: 
//  1242: test("andSelf()", function() {
//  1243: 	expect(4);
//  1248: });
//  1249: 
//  1251: 	expect(5);
//  1257: });
//  1258: 
//  1260: 	expect(3);
//  1264: });
//  1265: 
//  1267: 	expect(5);
//  1273: });
//  1274: 
//  1276: 	expect(5);
//  1282: });
//  1283: 
//  1285: 	expect(4);
//  1290: });
//  1291: 
//  1293: 	expect(4);
//  1298: });
//  1299: 
//  1300: test("show()", function() {
//  1301: 	expect(15);
//  1305: 	});
//  1307: 
//  1325: 
//  1330: });
//  1331: 
//  1333: 	expect(2);
//  1341: 
//  1342: 	// using contents will get regular, text, and comment nodes
//  1346: });
//  1347: 
//  1349: 	expect(4);
//  1356: 
//  1357: 	reset();
//  1365: 
//  1366: 	reset();
//  1369: 
//  1370: 	// using contents will get regular, text, and comment nodes
//  1374: });
//  1375: 
//  1377: 	expect(3);
//  1384: });
//  1385: 
//  1387: 	expect(1);
//  1389: });
//  1390: 
//  1392: 	expect(4);
//  1394: 
//  1395: 	// using contents will get comments regular, text, and comment nodes
//  1397: 	j.text("hi!");
//  1401: });
//  1402: 
//  1404: 	expect(12);
//  1407: 	});
//  1408: 
//  1411: 	});
//  1412: 
//  1415: 	});
//  1416: 
//  1429: });
//  1430: 
//  1431: test("$.prop", function() {
//  1432: 	expect(2);
//  1436: });
//  1437: 
//  1439: 	expect(6);
//  1442: 	c.add(x, "hi");
//  1446: 	c.remove(x);
//  1453: });
//  1454: 
//  1455: test("$.data", function() {
//  1456: 	expect(5);
//  1467: });
//  1468: 
//  1469: test(".data()", function() {
//  1470: 	expect(18);
//  1481: 
//  1484: 
//  1485: 	div
//  1490: 
//  1497: 
//  1498: 	hits.test = 0;
//  1499: 	gets.test = 0;
//  1500: 
//  1507: 
//  1508: 	hits.test = 0;
//  1509: 	gets.test = 0;
//  1510: 
//  1511: 	div
//  1514: 
//  1518: });
//  1519: 
//  1521: 	expect(1);
//  1526: });
//  1527: 
//  1529: 	expect(6);
//  1534: 
//  1540: 
//  1544: 
//  1547: });
//  1548: 
//  1549: test("remove()", function() {
//  1550: 	expect(6);
//  1554: 
//  1555: 	reset();
//  1559: 
//  1560: 	// using contents will get comments regular, text, and comment nodes
//  1564: });
//  1565: 
//  1566: test("empty()", function() {
//  1567: 	expect(3);
//  1570: 
//  1571: 	// using contents will get comments regular, text, and comment nodes
//  1573: 	j.empty();
//  1575: });
//  1576: 
//  1577: test("slice()", function() {
//  1578: 	expect(5);
//  1583: 
//  1585: });
//  1586: 
//  1587: test("map()", function() {
//  1588: 	expect(2);//expect(6);
//  1589: 
//  1590: 	isSet(
//  1593: 		}),
//  1595: 		"Array Map"
//  1596: 	);
//  1597: 
//  1598: 	isSet(
//  1601: 		}),
//  1603: 		"Single Map"
//  1604: 	);
//  1605: 
//  1607: 
//  1608: 	//for #2616
//  1612: 
//  1614: 
//  1618: 
//  1620: 
//  1625: 
//  1627: 
//  1631: 
//  1633: });
//  1634: 
//  1636: 	expect(12);
//  1641: 
//  1643: 
//  1646: 
//  1648: 
//  1651: 
//  1654: 
//  1656: 
//  1660: 
//  1661: 	// using contents will get comments regular, text, and comment nodes
//  1665: });
//  1666: 
//  1668: 	expect(15);
//  1669: 
//  1671: 
//  1673: 
//  1675: 
//  1677: 
//  1679: 
//  1681: 
//  1683: 
//  1685: 
//  1687: 
//  1689: 
//  1691: 
//  1692: 	//function, is tricky as it has length
//  1694: 	//window, also has length
//  1696: 
//  1698: 
//  1700: });

try {
{
$v.cf($v.ro('module'), [ 'core' ]);

// *** unit/core.js ***
//     3: test("Basic requirements", function() {

$v.cf($v.ro('test'), [ 'Basic requirements', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 7 ]);

// *** unit/core.js ***
//     5: 	ok( Array.prototype.push, "Array.push()" );

$v.cf($v.ro('ok'), [ $v.r($v.r($v.ro('Array'), 'prototype'), 'push'), 'Array.push()' ]);

// *** unit/core.js ***
//     6: 	ok( Function.prototype.apply, "Function.apply()" );

$v.cf($v.ro('ok'), [ $v.r($v.r($v.ro('Function'), 'prototype'), 'apply'), 'Function.apply()' ]);

// *** unit/core.js ***
//     7: 	ok( document.getElementById, "getElementById" );

$v.cf($v.ro('ok'), [ $v.r($v.ro('document'), 'getElementById'), 'getElementById' ]);

// *** unit/core.js ***
//     8: 	ok( document.getElementsByTagName, "getElementsByTagName" );

$v.cf($v.ro('ok'), [ $v.r($v.ro('document'), 'getElementsByTagName'), 'getElementsByTagName' ]);

// *** unit/core.js ***
//     9: 	ok( RegExp, "RegExp" );

$v.cf($v.ro('ok'), [ $v.ro('RegExp'), 'RegExp' ]);

// *** unit/core.js ***
//    10: 	ok( jQuery, "jQuery" );

$v.cf($v.ro('ok'), [ $v.ro('jQuery'), 'jQuery' ]);
$v.cf($v.ro('ok'), [ $v.ro('$'), '$()' ]);

// *** unit/core.js ***
//    11: 	ok( $, "$()" );

})) ]);
$v.cf($v.ro('test'), [ '$()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 8 ]);

// *** unit/core.js ***
//    17: 	var main = $("#main");

var main = $v.cf($v.ro('$'), [ '#main' ]);

// *** unit/core.js ***
//    18: 	isSet( $("div p", main).get(), q("sndp", "en", "sap"), "Basic selector with jQuery object as context" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cf($v.ro('$'), [ 'div p', main ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'sndp', 'en', 'sap' ]), 'Basic selector with jQuery object as context' ]);

// *** unit/core.js ***
//    38: 	var code = $("<code/>");

var code = $v.cf($v.ro('$'), [ '\x3ccode/\x3e' ]);

// *** unit/core.js ***
//    39: 	equals( code.length, 1, "Correct number of elements generated for code" );

$v.cf($v.ro('equals'), [ $v.r(code, 'length'), 1, 'Correct number of elements generated for code' ]);

// *** unit/core.js ***
//    40: 	var img = $("<img/>");

var img = $v.cf($v.ro('$'), [ '\x3cimg/\x3e' ]);

// *** unit/core.js ***
//    41: 	equals( img.length, 1, "Correct number of elements generated for img" );

$v.cf($v.ro('equals'), [ $v.r(img, 'length'), 1, 'Correct number of elements generated for img' ]);

// *** unit/core.js ***
//    42: 	var div = $("<div/><hr/><code/><b/>");

var div = $v.cf($v.ro('$'), [ '\x3cdiv/\x3e\x3chr/\x3e\x3ccode/\x3e\x3cb/\x3e' ]);

// *** unit/core.js ***
//    43: 	equals( div.length, 4, "Correct number of elements generated for div hr code b" );

$v.cf($v.ro('equals'), [ $v.r(div, 'length'), 4, 'Correct number of elements generated for div hr code b' ]);

// *** unit/core.js ***
//    46: 	equals( $(window).length, 1, "Correct number of elements generated for window" );

$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ $v.ro('window') ]), 'length'), 1, 'Correct number of elements generated for window' ]);

// *** unit/core.js ***
//    48: 	equals( $(document).length, 1, "Correct number of elements generated for document" );

$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ $v.ro('document') ]), 'length'), 1, 'Correct number of elements generated for document' ]);

// *** unit/core.js ***
//    50: 	equals( $([1,2,3]).get(1), 2, "Test passing an array to the factory" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ [ 1, 2, 3 ] ]), 'get', [ 1 ]), 2, 'Test passing an array to the factory' ]);

// *** unit/core.js ***
//    51: 	equals( $(document.body).get(0), $('body').get(0), "Test passing an html node to the factory" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ $v.r($v.ro('document'), 'body') ]), 'get', [ 0 ]), $v.cm($v.cf($v.ro('$'), [ 'body' ]), 'get', [ 0 ]), 'Test passing an html node to the factory' ]);
})) ]);
$v.cf($v.ro('test'), [ 'browser', $v.dis(___.frozenFunc(function ($dis) {
var $caja$41;
var $caja$42;
$v.cf($v.ro('expect'), [ 13 ]);

// *** unit/core.js ***
//    56: 	var browsers = {
//    57: 		//Internet Explorer
//    58: 		"Mozilla/5.0 (Windows; U; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)": "6.0",
//    59: 		"Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 5.1; .NET CLR 1.1.4322; InfoPath.1; .NET CLR 2.0.50727)": "7.0",
//    60: 		/** Failing #1876
//    61: 		 * "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1) ; .NET CLR 2.0.50727; .NET CLR 1.1.4322; .NET CLR 3.0.04506.30)": "7.0",
//    62: 		 �/
//    63: 		//Browsers with Gecko engine
//    64: 		//Mozilla
//    65: 		"Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.12) Gecko/20050915" : "1.7.12",
//    66: 		//Firefox
//    67: 		"Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en-US; rv:1.8.1.3) Gecko/20070309 Firefox/2.0.0.3": "1.8.1.3",
//    68: 		//Netscape
//    69: 		"Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.5) Gecko/20070321 Netscape/8.1.3" : "1.7.5",
//    70: 		//Flock
//    71: 		"Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.0.11) Gecko/20070321 Firefox/1.5.0.11 Flock/0.7.12" : "1.8.0.11",
//    72: 		//Opera browser
//    73: 		"Opera/9.20 (X11; Linux x86_64; U; en)": "9.20",
//    74: 		"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; en) Opera 9.20" : "9.20",
//    75: 		"Mozilla/5.0 (Windows NT 5.1; U; pl; rv:1.8.0) Gecko/20060728 Firefox/1.5.0 Opera 9.20": "9.20",
//    76: 		//WebKit engine
//    77: 		"Mozilla/5.0 (Macintosh; U; PPC Mac OS X; sv-se) AppleWebKit/418.9 (KHTML, like Gecko) Safari/419.3": "418.9",
//    78: 		"Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/418.8 (KHTML, like Gecko) Safari/419.3" : "418.8",
//    79: 		"Mozilla/5.0 (Macintosh; U; PPC Mac OS X; sv-se) AppleWebKit/312.8 (KHTML, like Gecko) Safari/312.5": "312.8",
//    80: 		//Other user agent string
//    81: 		"Other browser's user agent 1.0":null
//    82: 	};

var browsers = ___.initializeMap([ 'Mozilla/5.0 (Windows; U; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)', '6.0', 'Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 5.1; .NET CLR 1.1.4322; InfoPath.1; .NET CLR 2.0.50727)', '7.0', 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.12) Gecko/20050915', '1.7.12', 'Mozilla/5.0 (Macintosh; U; Intel Mac OS X; en-US; rv:1.8.1.3) Gecko/20070309 Firefox/2.0.0.3', '1.8.1.3', 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.7.5) Gecko/20070321 Netscape/8.1.3', '1.7.5', 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.0.11) Gecko/20070321 Firefox/1.5.0.11 Flock/0.7.12', '1.8.0.11', 'Opera/9.20 (X11; Linux x86_64; U; en)', '9.20', 'Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; en) Opera 9.20', '9.20', 'Mozilla/5.0 (Windows NT 5.1; U; pl; rv:1.8.0) Gecko/20060728 Firefox/1.5.0 Opera 9.20', '9.20', 'Mozilla/5.0 (Macintosh; U; PPC Mac OS X; sv-se) AppleWebKit/418.9 (KHTML, like Gecko) Safari/419.3', '418.9', 'Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/418.8 (KHTML, like Gecko) Safari/419.3', '418.8', 'Mozilla/5.0 (Macintosh; U; PPC Mac OS X; sv-se) AppleWebKit/312.8 (KHTML, like Gecko) Safari/312.5', '312.8', 'Other browser\'s user agent 1.0', null ]);
{
$caja$41 = $v.keys(browsers);

// *** unit/core.js ***
//    85: 		version = v ? v[1] : null;
//    86: 		equals( version, browsers[i], "Checking UA string" );
//    87: 	}

for ($caja$42 = 0; $caja$42 < ($caja$41.length_canRead___? $caja$41.length: ___.readPub($caja$41, 'length')); ++$caja$42) {

// *** unit/core.js ***
//    83: 	for (var i in browsers) {

var i = ___.readPub($caja$41, $caja$42);
{

// *** unit/core.js ***
//    84: 		var v = i.toLowerCase().match( /.+(?:rv|it|ra|ie)[\/: ]([\d.]+)/ ); // RegEx from Core jQuery.browser.version check

var v = $v.cm($v.cm(i, 'toLowerCase', [ ]), 'match', [ $v.construct(RegExp, [ '.+(?:rv|it|ra|ie)[\\/: ]([\\d.]+)' ]) ]);
$v.so('version', v? $v.r(v, 1): null);
$v.cf($v.ro('equals'), [ $v.ro('version'), $v.r(browsers, i), 'Checking UA string' ]);
}
}
}
})) ]);

// *** unit/core.js ***
//    90: test("noConflict", function() {

$v.cf($v.ro('test'), [ 'noConflict', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/core.js ***
//    93: 	var old = jQuery;

var old = $v.ro('jQuery');

// *** unit/core.js ***
//    94: 	var newjQuery = jQuery.noConflict();

var newjQuery = $v.cm($v.ro('jQuery'), 'noConflict', [ ]);

// *** unit/core.js ***
//    96: 	equals( newjQuery, old, "noConflict returned the jQuery object" );

$v.cf($v.ro('equals'), [ newjQuery, old, 'noConflict returned the jQuery object' ]);

// *** unit/core.js ***
//    97: 	equals( jQuery, old, "Make sure jQuery wasn't touched." );

$v.cf($v.ro('equals'), [ $v.ro('jQuery'), old, 'Make sure jQuery wasn\'t touched.' ]);

// *** unit/core.js ***
//    98: 	equals( $, "$", "Make sure $ was reverted." );

$v.cf($v.ro('equals'), [ $v.ro('$'), '$', 'Make sure $ was reverted.' ]);
$v.so('jQuery', $v.so('$', old));

// *** unit/core.js ***
//   102: 	newjQuery = jQuery.noConflict(true);

newjQuery = $v.cm($v.ro('jQuery'), 'noConflict', [ true ]);

// *** unit/core.js ***
//   104: 	equals( newjQuery, old, "noConflict returned the jQuery object" );

$v.cf($v.ro('equals'), [ newjQuery, old, 'noConflict returned the jQuery object' ]);

// *** unit/core.js ***
//   105: 	equals( jQuery, "jQuery", "Make sure jQuery was reverted." );

$v.cf($v.ro('equals'), [ $v.ro('jQuery'), 'jQuery', 'Make sure jQuery was reverted.' ]);

// *** unit/core.js ***
//   106: 	equals( $, "$", "Make sure $ was reverted." );

$v.cf($v.ro('equals'), [ $v.ro('$'), '$', 'Make sure $ was reverted.' ]);
$v.so('jQuery', $v.so('$', old));

// *** unit/core.js ***
//   108: 	jQuery = $ = old;

})) ]);

// *** unit/core.js ***
//   111: test("isFunction", function() {

$v.cf($v.ro('test'), [ 'isFunction', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/core.js ***
//   185: 		}
//   186: 
//   187: 		ok( jQuery.isFunction(fn), "Recursive Function Call" );
//   188: 
//   189: 		fn({ some: "data" });

function callme$_caller($dis, callback) {

// *** unit/core.js ***
//   183: 		function fn(response){
//   184: 			callback(response);

function fn$_caller($dis, response) {
$v.cf(callback, [ response ]);
}
___.func(fn$_caller, 'fn$_caller');
var fn;
;
fn = $v.dis(___.primFreeze(fn$_caller), 'fn');
;
$v.cf($v.ro('ok'), [ $v.cm($v.ro('jQuery'), 'isFunction', [ fn ]), 'Recursive Function Call' ]);
$v.cf(fn, [ ___.initializeMap([ 'some', 'data' ]) ]);
}
___.func(callme$_caller, 'callme$_caller');
var callme;
;

// *** unit/core.js ***
//   182: 	function callme(callback){

callme = $v.dis(___.primFreeze(callme$_caller), 'callme');
$v.cf($v.ro('expect'), [ 21 ]);

// *** unit/core.js ***
//   115: 	ok( !jQuery.isFunction(), "No Value" );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'isFunction', [ ]), 'No Value' ]);

// *** unit/core.js ***
//   116: 	ok( !jQuery.isFunction( null ), "null Value" );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'isFunction', [ null ]), 'null Value' ]);

// *** unit/core.js ***
//   117: 	ok( !jQuery.isFunction( undefined ), "undefined Value" );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'isFunction', [ $v.ro('undefined') ]), 'undefined Value' ]);

// *** unit/core.js ***
//   118: 	ok( !jQuery.isFunction( "" ), "Empty String Value" );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'isFunction', [ '' ]), 'Empty String Value' ]);

// *** unit/core.js ***
//   119: 	ok( !jQuery.isFunction( 0 ), "0 Value" );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'isFunction', [ 0 ]), '0 Value' ]);

// *** unit/core.js ***
//   123: 	ok( jQuery.isFunction(String), "String Function("+String+")" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('jQuery'), 'isFunction', [ $v.ro('String') ]), 'String Function(' + $v.ro('String') + ')' ]);

// *** unit/core.js ***
//   124: 	ok( jQuery.isFunction(Array), "Array Function("+Array+")" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('jQuery'), 'isFunction', [ $v.ro('Array') ]), 'Array Function(' + $v.ro('Array') + ')' ]);

// *** unit/core.js ***
//   125: 	ok( jQuery.isFunction(Object), "Object Function("+Object+")" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('jQuery'), 'isFunction', [ $v.ro('Object') ]), 'Object Function(' + $v.ro('Object') + ')' ]);

// *** unit/core.js ***
//   126: 	ok( jQuery.isFunction(Function), "Function Function("+Function+")" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('jQuery'), 'isFunction', [ $v.ro('Function') ]), 'Function Function(' + $v.ro('Function') + ')' ]);

// *** unit/core.js ***
//   129: 	var mystr = "function";

var mystr = 'function';

// *** unit/core.js ***
//   130: 	ok( !jQuery.isFunction(mystr), "Function String" );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'isFunction', [ mystr ]), 'Function String' ]);

// *** unit/core.js ***
//   133: 	var myarr = [ "function" ];

var myarr = [ 'function' ];

// *** unit/core.js ***
//   134: 	ok( !jQuery.isFunction(myarr), "Function Array" );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'isFunction', [ myarr ]), 'Function Array' ]);

// *** unit/core.js ***
//   137: 	var myfunction = { "function": "test" };

var myfunction = ___.initializeMap([ 'function', 'test' ]);

// *** unit/core.js ***
//   138: 	ok( !jQuery.isFunction(myfunction), "Function Object" );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'isFunction', [ myfunction ]), 'Function Object' ]);

// *** unit/core.js ***
//   141: 	var fn = function(){};

var fn = ___.frozenFunc(function () {
function fn$_var$($dis) {
}
___.func(fn$_var$, 'fn$_var$');
;
var fn$_var = $v.dis(___.primFreeze(fn$_var$), 'fn$_var');
return fn$_var;
}).CALL___();

// *** unit/core.js ***
//   142: 	ok( jQuery.isFunction(fn), "Normal Function" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('jQuery'), 'isFunction', [ fn ]), 'Normal Function' ]);

// *** unit/core.js ***
//   148: 	ok(true, "skipped test")

$v.cf($v.ro('ok'), [ true, 'skipped test' ]);

// *** unit/core.js ***
//   151:   ok( jQuery.isFunction(obj.getAttribute), "getAttribute Function" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('jQuery'), 'isFunction', [ $v.r($v.ro('obj'), 'getAttribute') ]), 'getAttribute Function' ]);

// *** unit/core.js ***
//   153: 	var nodes = document.body.childNodes;

var nodes = $v.r($v.r($v.ro('document'), 'body'), 'childNodes');

// *** unit/core.js ***
//   156: 	ok( !jQuery.isFunction(nodes), "childNodes Property" );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'isFunction', [ nodes ]), 'childNodes Property' ]);

// *** unit/core.js ***
//   158: 	var first = document.body.firstChild;

var first = $v.r($v.r($v.ro('document'), 'body'), 'firstChild');

// *** unit/core.js ***
//   161: 	ok( !jQuery.isFunction(first), "A normal DOM Element" );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'isFunction', [ first ]), 'A normal DOM Element' ]);

// *** unit/core.js ***
//   163: 	var input = document.createElement("input");

var input = $v.cm($v.ro('document'), 'createElement', [ 'input' ]);
$v.s(input, 'type', 'text');

// *** unit/core.js ***
//   165: 	document.body.appendChild( input );

$v.cm($v.r($v.ro('document'), 'body'), 'appendChild', [ input ]);

// *** unit/core.js ***
//   168: 	ok( jQuery.isFunction(input.focus), "A default function property" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('jQuery'), 'isFunction', [ $v.r(input, 'focus') ]), 'A default function property' ]);

// *** unit/core.js ***
//   170: 	document.body.removeChild( input );

$v.cm($v.r($v.ro('document'), 'body'), 'removeChild', [ input ]);

// *** unit/core.js ***
//   172: 	var a = document.createElement("a");

var a = $v.cm($v.ro('document'), 'createElement', [ 'a' ]);

// *** unit/core.js ***
//   173: 	a.href = "some-function";

$v.s(a, 'href', 'some-function');

// *** unit/core.js ***
//   174: 	document.body.appendChild( a );

$v.cm($v.r($v.ro('document'), 'body'), 'appendChild', [ a ]);

// *** unit/core.js ***
//   177: 	ok( !jQuery.isFunction(a), "Anchor Element" );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'isFunction', [ a ]), 'Anchor Element' ]);

// *** unit/core.js ***
//   179: 	document.body.removeChild( a );

$v.cm($v.r($v.ro('document'), 'body'), 'removeChild', [ a ]);
;

// *** unit/core.js ***
//   190: 	};

;
$v.cf(callme, [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf(callme, [ $v.dis(___.frozenFunc(function ($dis) {
})) ]);

// *** unit/core.js ***
//   193: 		callme(function(){});

})) ]);

// *** unit/core.js ***
//   194: 	});

})) ]);
$v.so('foo', false);

// *** unit/core.js ***
//   199: test("$('html')", function() {

$v.cf($v.ro('test'), [ '$(\'html\')', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);
$v.cf($v.ro('reset'), [ ]);
$v.so('foo', false);

// *** unit/core.js ***
//   204: 	var s = $("<script>var foo='test';<�script>")[0];

var s = $v.r($v.cf($v.ro('$'), [ '\x3cscript\x3evar foo=\'test\';\x3c/script\x3e' ]), 0);

// *** unit/core.js ***
//   205: 	ok( s, "Creating a script" );

$v.cf($v.ro('ok'), [ s, 'Creating a script' ]);

// *** unit/core.js ***
//   206: 	ok( !foo, "Make sure the script wasn't executed prematurely" );

$v.cf($v.ro('ok'), [ !$v.ro('foo'), 'Make sure the script wasn\'t executed prematurely' ]);

// *** unit/core.js ***
//   207: 	$("body").append(s);

$v.cm($v.cf($v.ro('$'), [ 'body' ]), 'append', [ s ]);

// *** unit/core.js ***
//   208: 	ok( foo, "Executing a scripts contents in the right context" );

$v.cf($v.ro('ok'), [ $v.ro('foo'), 'Executing a scripts contents in the right context' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   211: 	ok( $("<link rel='stylesheet'/>")[0], "Creating a link" );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '\x3clink rel=\'stylesheet\'/\x3e' ]), 0), 'Creating a link' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   215: 	var j = $("<span>hi<�span> there <�-- mon ami -�>");

var j = $v.cf($v.ro('$'), [ '\x3cspan\x3ehi\x3c/span\x3e there \x3c!-- mon ami --\x3e' ]);

// *** unit/core.js ***
//   216: 	ok( j.length >= 2, "Check node,textnode,comment creation (some browsers delete comments)" );

$v.cf($v.ro('ok'), [ $v.r(j, 'length') >= 2, 'Check node,textnode,comment creation (some browsers delete comments)' ]);

// *** unit/core.js ***
//   218: 	ok( !$("<option>test<�option>")[0].selected, "Make sure that options are auto-selected #2050" );

$v.cf($v.ro('ok'), [ !$v.r($v.r($v.cf($v.ro('$'), [ '\x3coption\x3etest\x3c/option\x3e' ]), 0), 'selected'), 'Make sure that options are auto-selected #2050' ]);
})) ]);

// *** unit/core.js ***
//   221: test("$('html', context)", function() {

$v.cf($v.ro('test'), [ '$(\'html\', context)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);

// *** unit/core.js ***
//   224: 	var $div = $("<div/>");

var $div = $v.cf($v.ro('$'), [ '\x3cdiv/\x3e' ]);

// *** unit/core.js ***
//   225: 	var $span = $("<span/>", $div);

var $span = $v.cf($v.ro('$'), [ '\x3cspan/\x3e', $div ]);
$v.cf($v.ro('equals'), [ $v.r($span, 'length'), 1, 'Verify a span created with a div context works, #1763' ]);

// *** unit/core.js ***
//   226: 	equals($span.length, 1, "Verify a span created with a div context works, #1763");

})) ]);

// *** unit/core.js ***
//   229: if ( !isLocal ) {
//   231: 	expect(2);
//   232: 	stop();
//   234: 		// tests for #1419 where IE was a problem
//   240: });
//   241: }

if (!$v.ro('isLocal')) {

// *** unit/core.js ***
//   230: test("$(selector, xml).text(str) - Loaded via XML document", function() {

$v.cf($v.ro('test'), [ '$(selector, xml).text(str) - Loaded via XML document', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/core.js ***
//   233: 	$.get('data/dashboard.xml', function(xml) {

$v.cm($v.ro('$'), 'get', [ 'data/dashboard.xml', $v.dis(___.frozenFunc(function ($dis, xml) {

// *** unit/core.js ***
//   235: 		equals( $("tab:first", xml).text(), "blabla", "Verify initial text correct" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'tab:first', xml ]), 'text', [ ]), 'blabla', 'Verify initial text correct' ]);

// *** unit/core.js ***
//   236: 		$("tab:first", xml).text("newtext");

$v.cm($v.cf($v.ro('$'), [ 'tab:first', xml ]), 'text', [ 'newtext' ]);

// *** unit/core.js ***
//   237: 		equals( $("tab:first", xml).text(), "newtext", "Verify new text correct" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'tab:first', xml ]), 'text', [ ]), 'newtext', 'Verify new text correct' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/core.js ***
//   238: 		start();

})) ]);

// *** unit/core.js ***
//   239: 	});

})) ]);
}
$v.cf($v.ro('test'), [ 'length', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ 'p' ]), 'length'), 6, 'Get Number of Elements Found' ]);

// *** unit/core.js ***
//   245: 	equals( $("p").length, 6, "Get Number of Elements Found" );

})) ]);
$v.cf($v.ro('test'), [ 'size()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'p' ]), 'size', [ ]), 6, 'Get Number of Elements Found' ]);

// *** unit/core.js ***
//   250: 	equals( $("p").size(), 6, "Get Number of Elements Found" );

})) ]);
$v.cf($v.ro('test'), [ 'get()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);

// *** unit/core.js ***
//   255: 	isSet( $("p").get(), q("firstp","ap","sndp","en","sap","first"), "Get All Elements" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cf($v.ro('$'), [ 'p' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'firstp', 'ap', 'sndp', 'en', 'sap', 'first' ]), 'Get All Elements' ]);
})) ]);

// *** unit/core.js ***
//   258: test("get(Number)", function() {

$v.cf($v.ro('test'), [ 'get(Number)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);

// *** unit/core.js ***
//   260: 	equals( $("p").get(0), document.getElementById("firstp"), "Get A Single Element" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'p' ]), 'get', [ 0 ]), $v.cm($v.ro('document'), 'getElementById', [ 'firstp' ]), 'Get A Single Element' ]);
})) ]);

// *** unit/core.js ***
//   263: test("add(String|Element|Array|undefined)", function() {

$v.cf($v.ro('test'), [ 'add(String|Element|Array|undefined)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 12 ]);

// *** unit/core.js ***
//   265: 	isSet( $("#sndp").add("#en").add("#sap").get(), q("sndp", "en", "sap"), "Check elements from document" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#sndp' ]), 'add', [ '#en' ]), 'add', [ '#sap' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'sndp', 'en', 'sap' ]), 'Check elements from document' ]);

// *** unit/core.js ***
//   266: 	isSet( $("#sndp").add( $("#en")[0] ).add( $("#sap") ).get(), q("sndp", "en", "sap"), "Check elements from document" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#sndp' ]), 'add', [ $v.r($v.cf($v.ro('$'), [ '#en' ]), 0) ]), 'add', [ $v.cf($v.ro('$'), [ '#sap' ]) ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'sndp', 'en', 'sap' ]), 'Check elements from document' ]);

// *** unit/core.js ***
//   267: 	ok( $([]).add($("#form")[0].elements).length >= 13, "Check elements from array" );

$v.cf($v.ro('ok'), [ $v.r($v.cm($v.cf($v.ro('$'), [ [ ] ]), 'add', [ $v.r($v.r($v.cf($v.ro('$'), [ '#form' ]), 0), 'elements') ]), 'length') >= 13, 'Check elements from array' ]);

// *** unit/core.js ***
//   273: 	var x = $([]).add($("<p id='x1'>xxx<�p>")).add($("<p id='x2'>xxx<�p>"));

var x = $v.cm($v.cm($v.cf($v.ro('$'), [ [ ] ]), 'add', [ $v.cf($v.ro('$'), [ '\x3cp id=\'x1\'\x3exxx\x3c/p\x3e' ]) ]), 'add', [ $v.cf($v.ro('$'), [ '\x3cp id=\'x2\'\x3exxx\x3c/p\x3e' ]) ]);

// *** unit/core.js ***
//   274: 	equals( x[0].id, "x1", "Check on-the-fly element1" );

$v.cf($v.ro('equals'), [ $v.r($v.r(x, 0), 'id'), 'x1', 'Check on-the-fly element1' ]);

// *** unit/core.js ***
//   275: 	equals( x[1].id, "x2", "Check on-the-fly element2" );

$v.cf($v.ro('equals'), [ $v.r($v.r(x, 1), 'id'), 'x2', 'Check on-the-fly element2' ]);

// *** unit/core.js ***
//   277: 	var x = $([]).add("<p id='x1'>xxx<�p>").add("<p id='x2'>xxx<�p>");

var x = $v.cm($v.cm($v.cf($v.ro('$'), [ [ ] ]), 'add', [ '\x3cp id=\'x1\'\x3exxx\x3c/p\x3e' ]), 'add', [ '\x3cp id=\'x2\'\x3exxx\x3c/p\x3e' ]);

// *** unit/core.js ***
//   278: 	equals( x[0].id, "x1", "Check on-the-fly element1" );

$v.cf($v.ro('equals'), [ $v.r($v.r(x, 0), 'id'), 'x1', 'Check on-the-fly element1' ]);

// *** unit/core.js ***
//   279: 	equals( x[1].id, "x2", "Check on-the-fly element2" );

$v.cf($v.ro('equals'), [ $v.r($v.r(x, 1), 'id'), 'x2', 'Check on-the-fly element2' ]);

// *** unit/core.js ***
//   281: 	var notDefined;

var notDefined;

// *** unit/core.js ***
//   282: 	equals( $([]).add(notDefined).length, 0, "Check that undefined adds nothing" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ [ ] ]), 'add', [ notDefined ]), 'length'), 0, 'Check that undefined adds nothing' ]);

// *** unit/core.js ***
//   285: 	equals( $([]).add([window,document,document.body,document]).length, 3, "Pass an array" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ [ ] ]), 'add', [ [ $v.ro('window'), $v.ro('document'), $v.r($v.ro('document'), 'body'), $v.ro('document') ] ]), 'length'), 3, 'Pass an array' ]);

// *** unit/core.js ***
//   286: 	equals( $(document).add(document).length, 1, "Check duplicated elements" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ $v.ro('document') ]), 'add', [ $v.ro('document') ]), 'length'), 1, 'Check duplicated elements' ]);

// *** unit/core.js ***
//   287: 	equals( $(window).add(window).length, 1, "Check duplicated elements using the window" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ $v.ro('window') ]), 'add', [ $v.ro('window') ]), 'length'), 1, 'Check duplicated elements using the window' ]);

// *** unit/core.js ***
//   288: 	ok( $([]).add( document.getElementById('form') ).length >= 13, "Add a form (adds the elements)" );

$v.cf($v.ro('ok'), [ $v.r($v.cm($v.cf($v.ro('$'), [ [ ] ]), 'add', [ $v.cm($v.ro('document'), 'getElementById', [ 'form' ]) ]), 'length') >= 13, 'Add a form (adds the elements)' ]);
})) ]);

// *** unit/core.js ***
//   291: test("each(Function)", function() {

$v.cf($v.ro('test'), [ 'each(Function)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);

// *** unit/core.js ***
//   293: 	var div = $("div");

var div = $v.cf($v.ro('$'), [ 'div' ]);
$v.cm(div, 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.s($dis, 'foo', 'zoo');

// *** unit/core.js ***
//   294: 	div.each(function(){this.foo = 'zoo';});

})) ]);

// *** unit/core.js ***
//   295: 	var pass = true;

var pass = true;

// *** unit/core.js ***
//   296: 	for ( var i = 0; i < div.size(); i++ ) {
//   298: 	}

for (var i = 0; i < $v.cm(div, 'size', [ ]); i++) {

// *** unit/core.js ***
//   297: 		if ( div.get(i).foo != "zoo" ) pass = false;

if ($v.r($v.cm(div, 'get', [ i ]), 'foo') != 'zoo') pass = false;
}
$v.cf($v.ro('ok'), [ pass, 'Execute a function, Relative' ]);

// *** unit/core.js ***
//   299: 	ok( pass, "Execute a function, Relative" );

})) ]);

// *** unit/core.js ***
//   302: test("index(Object)", function() {

$v.cf($v.ro('test'), [ 'index(Object)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 10 ]);

// *** unit/core.js ***
//   305: 	var elements = $([window, document]),
//   306: 		inputElements = $('#radio1,#radio2,#check1,#check2');

var elements = $v.cf($v.ro('$'), [ [ $v.ro('window'), $v.ro('document') ] ]), inputElements = $v.cf($v.ro('$'), [ '#radio1,#radio2,#check1,#check2' ]);

// *** unit/core.js ***
//   308: 	equals( elements.index(window), 0, "Check for index of elements" );

$v.cf($v.ro('equals'), [ $v.cm(elements, 'index', [ $v.ro('window') ]), 0, 'Check for index of elements' ]);

// *** unit/core.js ***
//   309: 	equals( elements.index(document), 1, "Check for index of elements" );

$v.cf($v.ro('equals'), [ $v.cm(elements, 'index', [ $v.ro('document') ]), 1, 'Check for index of elements' ]);

// *** unit/core.js ***
//   310: 	equals( inputElements.index(document.getElementById('radio1')), 0, "Check for index of elements" );

$v.cf($v.ro('equals'), [ $v.cm(inputElements, 'index', [ $v.cm($v.ro('document'), 'getElementById', [ 'radio1' ]) ]), 0, 'Check for index of elements' ]);

// *** unit/core.js ***
//   311: 	equals( inputElements.index(document.getElementById('radio2')), 1, "Check for index of elements" );

$v.cf($v.ro('equals'), [ $v.cm(inputElements, 'index', [ $v.cm($v.ro('document'), 'getElementById', [ 'radio2' ]) ]), 1, 'Check for index of elements' ]);

// *** unit/core.js ***
//   312: 	equals( inputElements.index(document.getElementById('check1')), 2, "Check for index of elements" );

$v.cf($v.ro('equals'), [ $v.cm(inputElements, 'index', [ $v.cm($v.ro('document'), 'getElementById', [ 'check1' ]) ]), 2, 'Check for index of elements' ]);

// *** unit/core.js ***
//   313: 	equals( inputElements.index(document.getElementById('check2')), 3, "Check for index of elements" );

$v.cf($v.ro('equals'), [ $v.cm(inputElements, 'index', [ $v.cm($v.ro('document'), 'getElementById', [ 'check2' ]) ]), 3, 'Check for index of elements' ]);

// *** unit/core.js ***
//   314: 	equals( inputElements.index(window), -1, "Check for not found index" );

$v.cf($v.ro('equals'), [ $v.cm(inputElements, 'index', [ $v.ro('window') ]), -1, 'Check for not found index' ]);

// *** unit/core.js ***
//   315: 	equals( inputElements.index(document), -1, "Check for not found index" );

$v.cf($v.ro('equals'), [ $v.cm(inputElements, 'index', [ $v.ro('document') ]), -1, 'Check for not found index' ]);

// *** unit/core.js ***
//   318: 	equals( elements.index( elements ), 0, "Pass in a jQuery object" );

$v.cf($v.ro('equals'), [ $v.cm(elements, 'index', [ elements ]), 0, 'Pass in a jQuery object' ]);

// *** unit/core.js ***
//   319: 	equals( elements.index( elements.eq(1) ), 1, "Pass in a jQuery object" );

$v.cf($v.ro('equals'), [ $v.cm(elements, 'index', [ $v.cm(elements, 'eq', [ 1 ]) ]), 1, 'Pass in a jQuery object' ]);
})) ]);

// *** unit/core.js ***
//   322: test("attr(String)", function() {

$v.cf($v.ro('test'), [ 'attr(String)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 26 ]);

// *** unit/core.js ***
//   324: 	equals( $('#text1').attr('value'), "Test", 'Check for value attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'attr', [ 'value' ]), 'Test', 'Check for value attribute' ]);

// *** unit/core.js ***
//   325: 	equals( $('#text1').attr('value', "Test2").attr('defaultValue'), "Test", 'Check for defaultValue attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'attr', [ 'value', 'Test2' ]), 'attr', [ 'defaultValue' ]), 'Test', 'Check for defaultValue attribute' ]);

// *** unit/core.js ***
//   326: 	equals( $('#text1').attr('type'), "text", 'Check for type attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'attr', [ 'type' ]), 'text', 'Check for type attribute' ]);

// *** unit/core.js ***
//   327: 	equals( $('#radio1').attr('type'), "radio", 'Check for type attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#radio1' ]), 'attr', [ 'type' ]), 'radio', 'Check for type attribute' ]);

// *** unit/core.js ***
//   328: 	equals( $('#check1').attr('type'), "checkbox", 'Check for type attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#check1' ]), 'attr', [ 'type' ]), 'checkbox', 'Check for type attribute' ]);

// *** unit/core.js ***
//   329: 	equals( $('#simon1').attr('rel'), "bookmark", 'Check for rel attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#simon1' ]), 'attr', [ 'rel' ]), 'bookmark', 'Check for rel attribute' ]);

// *** unit/core.js ***
//   330: 	equals( $('#google').attr('title'), "Google!", 'Check for title attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#google' ]), 'attr', [ 'title' ]), 'Google!', 'Check for title attribute' ]);

// *** unit/core.js ***
//   331: 	equals( $('#mark').attr('hreflang'), "en", 'Check for hreflang attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#mark' ]), 'attr', [ 'hreflang' ]), 'en', 'Check for hreflang attribute' ]);

// *** unit/core.js ***
//   332: 	equals( $('#en').attr('lang'), "en", 'Check for lang attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'attr', [ 'lang' ]), 'en', 'Check for lang attribute' ]);

// *** unit/core.js ***
//   333: 	equals( $('#simon').attr('class'), "blog link", 'Check for class attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#simon' ]), 'attr', [ 'class' ]), 'blog link', 'Check for class attribute' ]);

// *** unit/core.js ***
//   334: 	equals( $('#name').attr('name'), "name", 'Check for name attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#name' ]), 'attr', [ 'name' ]), 'name', 'Check for name attribute' ]);

// *** unit/core.js ***
//   335: 	equals( $('#text1').attr('name'), "action", 'Check for name attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'attr', [ 'name' ]), 'action', 'Check for name attribute' ]);

// *** unit/core.js ***
//   336: 	ok( $('#form').attr('action').indexOf("formaction") >= 0, 'Check for action attribute' );

$v.cf($v.ro('ok'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#form' ]), 'attr', [ 'action' ]), 'indexOf', [ 'formaction' ]) >= 0, 'Check for action attribute' ]);

// *** unit/core.js ***
//   337: 	equals( $('#text1').attr('maxlength'), '30', 'Check for maxlength attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'attr', [ 'maxlength' ]), '30', 'Check for maxlength attribute' ]);

// *** unit/core.js ***
//   338: 	equals( $('#text1').attr('maxLength'), '30', 'Check for maxLength attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'attr', [ 'maxLength' ]), '30', 'Check for maxLength attribute' ]);

// *** unit/core.js ***
//   339: 	equals( $('#area1').attr('maxLength'), '30', 'Check for maxLength attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#area1' ]), 'attr', [ 'maxLength' ]), '30', 'Check for maxLength attribute' ]);

// *** unit/core.js ***
//   340: 	equals( $('#select2').attr('selectedIndex'), 3, 'Check for selectedIndex attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#select2' ]), 'attr', [ 'selectedIndex' ]), 3, 'Check for selectedIndex attribute' ]);

// *** unit/core.js ***
//   341: 	equals( $('#foo').attr('nodeName'), 'DIV', 'Check for nodeName attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'attr', [ 'nodeName' ]), 'DIV', 'Check for nodeName attribute' ]);

// *** unit/core.js ***
//   342: 	equals( $('#foo').attr('tagName'), 'DIV', 'Check for tagName attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'attr', [ 'tagName' ]), 'DIV', 'Check for tagName attribute' ]);

// *** unit/core.js ***
//   344: 	$('<a id="tAnchor5"><�a>').attr('href', '#5').appendTo('#main'); // using innerHTML in IE causes href attribute to be serialized to the full path

$v.cm($v.cm($v.cf($v.ro('$'), [ '\x3ca id=\"tAnchor5\"\x3e\x3c/a\x3e' ]), 'attr', [ 'href', '#5' ]), 'appendTo', [ '#main' ]);

// *** unit/core.js ***
//   345: 	equals( $('#tAnchor5').attr('href'), "#5", 'Check for non-absolute href (an anchor)' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#tAnchor5' ]), 'attr', [ 'href' ]), '#5', 'Check for non-absolute href (an anchor)' ]);

// *** unit/core.js ***
//   349: 	var body = document.body, $body = $(body);

var body = $v.r($v.ro('document'), 'body'), $body = $v.cf($v.ro('$'), [ body ]);

// *** unit/core.js ***
//   351: 	ok( $body.attr('foo') === undefined, 'Make sure that a non existent attribute returns undefined' );

$v.cf($v.ro('ok'), [ $v.cm($body, 'attr', [ 'foo' ]) === $v.ro('undefined'), 'Make sure that a non existent attribute returns undefined' ]);

// *** unit/core.js ***
//   352: 	ok( $body.attr('nextSibling') === null, 'Make sure a null expando returns null' );

$v.cf($v.ro('ok'), [ $v.cm($body, 'attr', [ 'nextSibling' ]) === null, 'Make sure a null expando returns null' ]);

// *** unit/core.js ***
//   356: 	ok(true, "skipped test")

$v.cf($v.ro('ok'), [ true, 'skipped test' ]);
$v.s(body, 'foo', 'bar');

// *** unit/core.js ***
//   359: 	equals( $body.attr('foo'), 'bar', 'Make sure the expando is preferred over the dom attribute' );

$v.cf($v.ro('equals'), [ $v.cm($body, 'attr', [ 'foo' ]), 'bar', 'Make sure the expando is preferred over the dom attribute' ]);

// *** unit/core.js ***
//   363:   ok(true, "skipped test")

$v.cf($v.ro('ok'), [ true, 'skipped test' ]);

// *** unit/core.js ***
//   365: 	body.foo = undefined;

$v.s(body, 'foo', $v.ro('undefined'));

// *** unit/core.js ***
//   366: 	ok( $body.attr('foo') === undefined, 'Make sure the expando is preferred over the dom attribute, even if undefined' );

$v.cf($v.ro('ok'), [ $v.cm($body, 'attr', [ 'foo' ]) === $v.ro('undefined'), 'Make sure the expando is preferred over the dom attribute, even if undefined' ]);
$v.cm(body, 'removeAttribute', [ 'foo' ]);

// *** unit/core.js ***
//   368: 	body.removeAttribute('foo'); // Cleanup

})) ]);

// *** unit/core.js ***
//   371: if ( !isLocal ) {
//   373: 		expect(2);
//   374: 		stop();
//   380: 	});
//   381: }

if (!$v.ro('isLocal')) {

// *** unit/core.js ***
//   372: 	test("attr(String) in XML Files", function() {

$v.cf($v.ro('test'), [ 'attr(String) in XML Files', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/core.js ***
//   375: 		$.get("data/dashboard.xml", function(xml) {

$v.cm($v.ro('$'), 'get', [ 'data/dashboard.xml', $v.dis(___.frozenFunc(function ($dis, xml) {

// *** unit/core.js ***
//   376: 			equals( $("locations", xml).attr("class"), "foo", "Check class attribute in XML document" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'locations', xml ]), 'attr', [ 'class' ]), 'foo', 'Check class attribute in XML document' ]);

// *** unit/core.js ***
//   377: 			equals( $("location", xml).attr("for"), "bar", "Check for attribute in XML document" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'location', xml ]), 'attr', [ 'for' ]), 'bar', 'Check for attribute in XML document' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/core.js ***
//   378: 			start();

})) ]);

// *** unit/core.js ***
//   379: 		});

})) ]);
}

// *** unit/core.js ***
//   383: test("attr(String, Function)", function() {

$v.cf($v.ro('test'), [ 'attr(String, Function)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'attr', [ 'value', $v.dis(___.frozenFunc(function ($dis) {
return $v.r($dis, 'id');

// *** unit/core.js ***
//   385: 	equals( $('#text1').attr('value', function() { return this.id })[0].value, "text1", "Set value from id" );

})) ]), 0), 'value'), 'text1', 'Set value from id' ]);
$v.cf($v.ro('equals'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'attr', [ 'title', $v.dis(___.frozenFunc(function ($dis, i) {
return i;

// *** unit/core.js ***
//   386: 	equals( $('#text1').attr('title', function(i) { return i }).attr('title'), "0", "Set value with an index");

})) ]), 'attr', [ 'title' ]), '0', 'Set value with an index' ]);
})) ]);

// *** unit/core.js ***
//   389: test("attr(Hash)", function() {

$v.cf($v.ro('test'), [ 'attr(Hash)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);

// *** unit/core.js ***
//   391: 	var pass = true;

var pass = true;

// *** unit/core.js ***
//   392: 	$("div").attr({foo: 'baz', zoo: 'ping'}).each(function(){

$v.cm($v.cm($v.cf($v.ro('$'), [ 'div' ]), 'attr', [ ___.initializeMap([ 'foo', 'baz', 'zoo', 'ping' ]) ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/core.js ***
//   393: 		if ( this.getAttribute('foo') != "baz" && this.getAttribute('zoo') != "ping" ) pass = false;

if ($v.cm($dis, 'getAttribute', [ 'foo' ]) != 'baz' && $v.cm($dis, 'getAttribute', [ 'zoo' ]) != 'ping') pass = false;
})) ]);
$v.cf($v.ro('ok'), [ pass, 'Set Multiple Attributes' ]);

// *** unit/core.js ***
//   395: 	ok( pass, "Set Multiple Attributes" );

})) ]);

// *** unit/core.js ***
//   398: test("attr(String, Object)", function() {

$v.cf($v.ro('test'), [ 'attr(String, Object)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 17 ]);

// *** unit/core.js ***
//   400: 	var div = $("div").attr("foo", "bar");

var div = $v.cm($v.cf($v.ro('$'), [ 'div' ]), 'attr', [ 'foo', 'bar' ]);
$v.so('fail', false);

// *** unit/core.js ***
//   402: 	for ( var i = 0; i < div.size(); i++ ) {
//   407: 	}

for (var i = 0; i < $v.cm(div, 'size', [ ]); i++) {

// *** unit/core.js ***
//   403: 		if ( div.get(i).getAttribute('foo') != "bar" ){
//   404: 			fail = i;
//   406: 		}

if ($v.cm($v.cm(div, 'get', [ i ]), 'getAttribute', [ 'foo' ]) != 'bar') {
$v.so('fail', i);

// *** unit/core.js ***
//   405: 			break;

break;
}
}

// *** unit/core.js ***
//   408: 	equals( fail, false, "Set Attribute, the #"+fail+" element didn't get the attribute 'foo'" );

$v.cf($v.ro('equals'), [ $v.ro('fail'), false, 'Set Attribute, the #' + $v.ro('fail') + ' element didn\'t get the attribute \'foo\'' ]);

// *** unit/core.js ***
//   411: 	ok(true, "skipped test")

$v.cf($v.ro('ok'), [ true, 'skipped test' ]);

// *** unit/core.js ***
//   413: 	$("#name").attr('name', 'something');

$v.cm($v.cf($v.ro('$'), [ '#name' ]), 'attr', [ 'name', 'something' ]);

// *** unit/core.js ***
//   414: 	equals( $("#name").attr('name'), 'something', 'Set name attribute' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#name' ]), 'attr', [ 'name' ]), 'something', 'Set name attribute' ]);

// *** unit/core.js ***
//   415: 	$("#check2").attr('checked', true);

$v.cm($v.cf($v.ro('$'), [ '#check2' ]), 'attr', [ 'checked', true ]);

// *** unit/core.js ***
//   416: 	equals( document.getElementById('check2').checked, true, 'Set checked attribute' );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.ro('document'), 'getElementById', [ 'check2' ]), 'checked'), true, 'Set checked attribute' ]);

// *** unit/core.js ***
//   417: 	$("#check2").attr('checked', false);

$v.cm($v.cf($v.ro('$'), [ '#check2' ]), 'attr', [ 'checked', false ]);

// *** unit/core.js ***
//   418: 	equals( document.getElementById('check2').checked, false, 'Set checked attribute' );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.ro('document'), 'getElementById', [ 'check2' ]), 'checked'), false, 'Set checked attribute' ]);

// *** unit/core.js ***
//   419: 	$("#text1").attr('readonly', true);

$v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'attr', [ 'readonly', true ]);

// *** unit/core.js ***
//   420: 	equals( document.getElementById('text1').readOnly, true, 'Set readonly attribute' );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.ro('document'), 'getElementById', [ 'text1' ]), 'readOnly'), true, 'Set readonly attribute' ]);

// *** unit/core.js ***
//   421: 	$("#text1").attr('readonly', false);

$v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'attr', [ 'readonly', false ]);

// *** unit/core.js ***
//   422: 	equals( document.getElementById('text1').readOnly, false, 'Set readonly attribute' );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.ro('document'), 'getElementById', [ 'text1' ]), 'readOnly'), false, 'Set readonly attribute' ]);

// *** unit/core.js ***
//   423: 	$("#name").attr('maxlength', '5');

$v.cm($v.cf($v.ro('$'), [ '#name' ]), 'attr', [ 'maxlength', '5' ]);

// *** unit/core.js ***
//   424: 	equals( document.getElementById('name').maxLength, '5', 'Set maxlength attribute' );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.ro('document'), 'getElementById', [ 'name' ]), 'maxLength'), '5', 'Set maxlength attribute' ]);

// *** unit/core.js ***
//   425: 	$("#name").attr('maxLength', '10');

$v.cm($v.cf($v.ro('$'), [ '#name' ]), 'attr', [ 'maxLength', '10' ]);

// *** unit/core.js ***
//   426: 	equals( document.getElementById('name').maxLength, '10', 'Set maxlength attribute' );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.ro('document'), 'getElementById', [ 'name' ]), 'maxLength'), '10', 'Set maxlength attribute' ]);

// *** unit/core.js ***
//   431: 	ok(true, "skipped test");

$v.cf($v.ro('ok'), [ true, 'skipped test' ]);

// *** unit/core.js ***
//   434:   ok(true, "skipped test");

$v.cf($v.ro('ok'), [ true, 'skipped test' ]);

// *** unit/core.js ***
//   437:   ok(true, "skipped test");

$v.cf($v.ro('ok'), [ true, 'skipped test' ]);

// *** unit/core.js ***
//   440: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);

// *** unit/core.js ***
//   444: 	ok(true, "skipped test");

$v.cf($v.ro('ok'), [ true, 'skipped test' ]);

// *** unit/core.js ***
//   445: 	j.removeAttr("name");

$v.cm(j, 'removeAttr', [ 'name' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   449: 	var type = $("#check2").attr('type');

var type = $v.cm($v.cf($v.ro('$'), [ '#check2' ]), 'attr', [ 'type' ]);

// *** unit/core.js ***
//   450: 	var thrown = false;

var thrown = false;

// *** unit/core.js ***
//   451: 	try {
//   452: 		$("#check2").attr('type','hidden');
//   455: 	}

try {
$v.cm($v.cf($v.ro('$'), [ '#check2' ]), 'attr', [ 'type', 'hidden' ]);
} catch (ex___) {

// *** unit/core.js ***
//   453: 	} catch(e) {

try {
throw ___.tameException(ex___);
} catch (e) {

// *** unit/core.js ***
//   454: 		thrown = true;

thrown = true;
}
}

// *** unit/core.js ***
//   456: 	ok( thrown, "Exception thrown when trying to change type property" );

$v.cf($v.ro('ok'), [ thrown, 'Exception thrown when trying to change type property' ]);

// *** unit/core.js ***
//   457: 	equals( type, $("#check2").attr('type'), "Verify that you can't change the type of an input element" );

$v.cf($v.ro('equals'), [ type, $v.cm($v.cf($v.ro('$'), [ '#check2' ]), 'attr', [ 'type' ]), 'Verify that you can\'t change the type of an input element' ]);

// *** unit/core.js ***
//   459: 	var check = document.createElement("input");

var check = $v.cm($v.ro('document'), 'createElement', [ 'input' ]);

// *** unit/core.js ***
//   460: 	var thrown = true;

var thrown = true;

// *** unit/core.js ***
//   461: 	try {
//   462: 		$(check).attr('type','checkbox');
//   465: 	}

try {
$v.cm($v.cf($v.ro('$'), [ check ]), 'attr', [ 'type', 'checkbox' ]);
} catch (ex___) {

// *** unit/core.js ***
//   463: 	} catch(e) {

try {
throw ___.tameException(ex___);
} catch (e) {

// *** unit/core.js ***
//   464: 		thrown = false;

thrown = false;
}
}

// *** unit/core.js ***
//   466: 	ok( thrown, "Exception thrown when trying to change type property" );

$v.cf($v.ro('ok'), [ thrown, 'Exception thrown when trying to change type property' ]);

// *** unit/core.js ***
//   467: 	equals( "checkbox", $(check).attr('type'), "Verify that you can change the type of an input element that isn't in the DOM" );

$v.cf($v.ro('equals'), [ 'checkbox', $v.cm($v.cf($v.ro('$'), [ check ]), 'attr', [ 'type' ]), 'Verify that you can change the type of an input element that isn\'t in the DOM' ]);
})) ]);

// *** unit/core.js ***
//   470: if ( !isLocal ) {
//   472: 		expect(2);
//   473: 		stop();
//   478: 			});
//   483: 	});
//   484: }

if (!$v.ro('isLocal')) {

// *** unit/core.js ***
//   471: 	test("attr(String, Object) - Loaded via XML document", function() {

$v.cf($v.ro('test'), [ 'attr(String, Object) - Loaded via XML document', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/core.js ***
//   474: 		$.get('data/dashboard.xml', function(xml) {

$v.cm($v.ro('$'), 'get', [ 'data/dashboard.xml', $v.dis(___.frozenFunc(function ($dis, xml) {

// *** unit/core.js ***
//   475: 			var titles = [];

var titles = [ ];

// *** unit/core.js ***
//   476: 			$('tab', xml).each(function() {

$v.cm($v.cf($v.ro('$'), [ 'tab', xml ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm(titles, 'push', [ $v.cm($v.cf($v.ro('$'), [ $dis ]), 'attr', [ 'title' ]) ]);

// *** unit/core.js ***
//   477: 				titles.push($(this).attr('title'));

})) ]);

// *** unit/core.js ***
//   479: 			equals( titles[0], 'Location', 'attr() in XML context: Check first title' );

$v.cf($v.ro('equals'), [ $v.r(titles, 0), 'Location', 'attr() in XML context: Check first title' ]);

// *** unit/core.js ***
//   480: 			equals( titles[1], 'Users', 'attr() in XML context: Check second title' );

$v.cf($v.ro('equals'), [ $v.r(titles, 1), 'Users', 'attr() in XML context: Check second title' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/core.js ***
//   481: 			start();

})) ]);

// *** unit/core.js ***
//   482: 		});

})) ]);
}

// *** unit/core.js ***
//   486: test("css(String|Hash)", function() {

$v.cf($v.ro('test'), [ 'css(String|Hash)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 19 ]);

// *** unit/core.js ***
//   489: 	equals( $('#main').css("display"), 'none', 'Check for css property "display"');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#main' ]), 'css', [ 'display' ]), 'none', 'Check for css property \"display\"' ]);

// *** unit/core.js ***
//   491: 	ok( $('#foo').is(':visible'), 'Modifying CSS display: Assert element is visible');

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ ':visible' ]), 'Modifying CSS display: Assert element is visible' ]);

// *** unit/core.js ***
//   492: 	$('#foo').css({display: 'none'});

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ ___.initializeMap([ 'display', 'none' ]) ]);

// *** unit/core.js ***
//   493: 	ok( !$('#foo').is(':visible'), 'Modified CSS display: Assert element is hidden');

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ ':visible' ]), 'Modified CSS display: Assert element is hidden' ]);

// *** unit/core.js ***
//   494: 	$('#foo').css({display: 'block'});

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ ___.initializeMap([ 'display', 'block' ]) ]);

// *** unit/core.js ***
//   495: 	ok( $('#foo').is(':visible'), 'Modified CSS display: Assert element is visible');

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ ':visible' ]), 'Modified CSS display: Assert element is visible' ]);

// *** unit/core.js ***
//   497: 	$('#floatTest').css({styleFloat: 'right'});

$v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ ___.initializeMap([ 'styleFloat', 'right' ]) ]);

// *** unit/core.js ***
//   498: 	equals( $('#floatTest').css('styleFloat'), 'right', 'Modified CSS float using "styleFloat": Assert float is right');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'styleFloat' ]), 'right', 'Modified CSS float using \"styleFloat\": Assert float is right' ]);

// *** unit/core.js ***
//   499: 	$('#floatTest').css({cssFloat: 'left'});

$v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ ___.initializeMap([ 'cssFloat', 'left' ]) ]);

// *** unit/core.js ***
//   500: 	equals( $('#floatTest').css('cssFloat'), 'left', 'Modified CSS float using "cssFloat": Assert float is left');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'cssFloat' ]), 'left', 'Modified CSS float using \"cssFloat\": Assert float is left' ]);

// *** unit/core.js ***
//   501: 	$('#floatTest').css({'float': 'right'});

$v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ ___.initializeMap([ 'float', 'right' ]) ]);

// *** unit/core.js ***
//   502: 	equals( $('#floatTest').css('float'), 'right', 'Modified CSS float using "float": Assert float is right');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'float' ]), 'right', 'Modified CSS float using \"float\": Assert float is right' ]);

// *** unit/core.js ***
//   503: 	$('#floatTest').css({'font-size': '30px'});

$v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ ___.initializeMap([ 'font-size', '30px' ]) ]);

// *** unit/core.js ***
//   504: 	equals( $('#floatTest').css('font-size'), '30px', 'Modified CSS font-size: Assert font-size is 30px');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'font-size' ]), '30px', 'Modified CSS font-size: Assert font-size is 30px' ]);

// *** unit/core.js ***
//   506: 	$.each("0,0.25,0.5,0.75,1".split(','), function(i, n) {

$v.cm($v.ro('$'), 'each', [ $v.cm('0,0.25,0.5,0.75,1', 'split', [ ',' ]), $v.dis(___.frozenFunc(function ($dis, i, n) {

// *** unit/core.js ***
//   507: 		$('#foo').css({opacity: n});

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ ___.initializeMap([ 'opacity', n ]) ]);

// *** unit/core.js ***
//   508: 		equals( $('#foo').css('opacity'), parseFloat(n), "Assert opacity is " + parseFloat(n) + " as a String" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'opacity' ]), $v.cf($v.ro('parseFloat'), [ n ]), 'Assert opacity is ' + $v.cf($v.ro('parseFloat'), [ n ]) + ' as a String' ]);

// *** unit/core.js ***
//   509: 		$('#foo').css({opacity: parseFloat(n)});

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ ___.initializeMap([ 'opacity', $v.cf($v.ro('parseFloat'), [ n ]) ]) ]);
$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'opacity' ]), $v.cf($v.ro('parseFloat'), [ n ]), 'Assert opacity is ' + $v.cf($v.ro('parseFloat'), [ n ]) + ' as a Number' ]);

// *** unit/core.js ***
//   510: 		equals( $('#foo').css('opacity'), parseFloat(n), "Assert opacity is " + parseFloat(n) + " as a Number" );

})) ]);

// *** unit/core.js ***
//   512: 	$('#foo').css({opacity: ''});

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ ___.initializeMap([ 'opacity', '' ]) ]);

// *** unit/core.js ***
//   513: 	equals( $('#foo').css('opacity'), '1', "Assert opacity is 1 when set to an empty String" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'opacity' ]), '1', 'Assert opacity is 1 when set to an empty String' ]);
})) ]);

// *** unit/core.js ***
//   516: test("css(String, Object)", function() {

$v.cf($v.ro('test'), [ 'css(String, Object)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 21 ]);

// *** unit/core.js ***
//   518: 	ok( $('#foo').is(':visible'), 'Modifying CSS display: Assert element is visible');

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ ':visible' ]), 'Modifying CSS display: Assert element is visible' ]);

// *** unit/core.js ***
//   519: 	$('#foo').css('display', 'none');

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'display', 'none' ]);

// *** unit/core.js ***
//   520: 	ok( !$('#foo').is(':visible'), 'Modified CSS display: Assert element is hidden');

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ ':visible' ]), 'Modified CSS display: Assert element is hidden' ]);

// *** unit/core.js ***
//   521: 	$('#foo').css('display', 'block');

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'display', 'block' ]);

// *** unit/core.js ***
//   522: 	ok( $('#foo').is(':visible'), 'Modified CSS display: Assert element is visible');

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ ':visible' ]), 'Modified CSS display: Assert element is visible' ]);

// *** unit/core.js ***
//   524: 	$('#floatTest').css('styleFloat', 'left');

$v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'styleFloat', 'left' ]);

// *** unit/core.js ***
//   525: 	equals( $('#floatTest').css('styleFloat'), 'left', 'Modified CSS float using "styleFloat": Assert float is left');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'styleFloat' ]), 'left', 'Modified CSS float using \"styleFloat\": Assert float is left' ]);

// *** unit/core.js ***
//   526: 	$('#floatTest').css('cssFloat', 'right');

$v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'cssFloat', 'right' ]);

// *** unit/core.js ***
//   527: 	equals( $('#floatTest').css('cssFloat'), 'right', 'Modified CSS float using "cssFloat": Assert float is right');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'cssFloat' ]), 'right', 'Modified CSS float using \"cssFloat\": Assert float is right' ]);

// *** unit/core.js ***
//   528: 	$('#floatTest').css('float', 'left');

$v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'float', 'left' ]);

// *** unit/core.js ***
//   529: 	equals( $('#floatTest').css('float'), 'left', 'Modified CSS float using "float": Assert float is left');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'float' ]), 'left', 'Modified CSS float using \"float\": Assert float is left' ]);

// *** unit/core.js ***
//   530: 	$('#floatTest').css('font-size', '20px');

$v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'font-size', '20px' ]);

// *** unit/core.js ***
//   531: 	equals( $('#floatTest').css('font-size'), '20px', 'Modified CSS font-size: Assert font-size is 20px');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#floatTest' ]), 'css', [ 'font-size' ]), '20px', 'Modified CSS font-size: Assert font-size is 20px' ]);

// *** unit/core.js ***
//   533: 	$.each("0,0.25,0.5,0.75,1".split(','), function(i, n) {

$v.cm($v.ro('$'), 'each', [ $v.cm('0,0.25,0.5,0.75,1', 'split', [ ',' ]), $v.dis(___.frozenFunc(function ($dis, i, n) {

// *** unit/core.js ***
//   534: 		$('#foo').css('opacity', n);

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'opacity', n ]);

// *** unit/core.js ***
//   535: 		equals( $('#foo').css('opacity'), parseFloat(n), "Assert opacity is " + parseFloat(n) + " as a String" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'opacity' ]), $v.cf($v.ro('parseFloat'), [ n ]), 'Assert opacity is ' + $v.cf($v.ro('parseFloat'), [ n ]) + ' as a String' ]);

// *** unit/core.js ***
//   536: 		$('#foo').css('opacity', parseFloat(n));

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'opacity', $v.cf($v.ro('parseFloat'), [ n ]) ]);
$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'opacity' ]), $v.cf($v.ro('parseFloat'), [ n ]), 'Assert opacity is ' + $v.cf($v.ro('parseFloat'), [ n ]) + ' as a Number' ]);

// *** unit/core.js ***
//   537: 		equals( $('#foo').css('opacity'), parseFloat(n), "Assert opacity is " + parseFloat(n) + " as a Number" );

})) ]);

// *** unit/core.js ***
//   539: 	$('#foo').css('opacity', '');

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'opacity', '' ]);

// *** unit/core.js ***
//   540: 	equals( $('#foo').css('opacity'), '1', "Assert opacity is 1 when set to an empty String" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'opacity' ]), '1', 'Assert opacity is 1 when set to an empty String' ]);

// *** unit/core.js ***
//   542: 	if (jQuery.browser.msie) {
//   544: 	}

if ($v.r($v.r($v.ro('jQuery'), 'browser'), 'msie')) {

// *** unit/core.js ***
//   543: 		$('#foo').css("filter", "progid:DXImageTransform.Microsoft.Chroma(color='red');");

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'filter', 'progid:DXImageTransform.Microsoft.Chroma(color=\'red\');' ]);
}

// *** unit/core.js ***
//   545: 	equals( $('#foo').css('opacity'), '1', "Assert opacity is 1 when a different filter is set in IE, #1438" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'css', [ 'opacity' ]), '1', 'Assert opacity is 1 when a different filter is set in IE, #1438' ]);

// *** unit/core.js ***
//   548: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);

// *** unit/core.js ***
//   549: 	j.css("padding-left", "1px");

$v.cm(j, 'css', [ 'padding-left', '1px' ]);

// *** unit/core.js ***
//   550: 	equals( j.css("padding-left"), "1px", "Check node,textnode,comment css works" );

$v.cf($v.ro('equals'), [ $v.cm(j, 'css', [ 'padding-left' ]), '1px', 'Check node,textnode,comment css works' ]);

// *** unit/core.js ***
//   553: 	$("#t2037")[0].innerHTML = $("#t2037")[0].innerHTML

$v.s($v.r($v.cf($v.ro('$'), [ '#t2037' ]), 0), 'innerHTML', $v.r($v.r($v.cf($v.ro('$'), [ '#t2037' ]), 0), 'innerHTML'));

// *** unit/core.js ***
//   554: 	equals( $("#t2037 .hidden").css("display"), "none", "Make sure browser thinks it is hidden" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#t2037 .hidden' ]), 'css', [ 'display' ]), 'none', 'Make sure browser thinks it is hidden' ]);
})) ]);

// *** unit/core.js ***
//   557: test("jQuery.css(elem, 'height') doesn't clear radio buttons (bug #1095)", function () {

$v.cf($v.ro('test'), [ 'jQuery.css(elem, \'height\') doesn\'t clear radio buttons (bug #1095)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//   560: 	var $checkedtest = $("#checkedtest");

var $checkedtest = $v.cf($v.ro('$'), [ '#checkedtest' ]);

// *** unit/core.js ***
//   562: 	jQuery.css($checkedtest[0], "height");

$v.cm($v.ro('jQuery'), 'css', [ $v.r($checkedtest, 0), 'height' ]);

// *** unit/core.js ***
//   563: 	ok( !! $(":radio:first", $checkedtest).attr("checked"), "Check first radio still checked." );

$v.cf($v.ro('ok'), [ ! (!$v.cm($v.cf($v.ro('$'), [ ':radio:first', $checkedtest ]), 'attr', [ 'checked' ])), 'Check first radio still checked.' ]);

// *** unit/core.js ***
//   564: 	ok( ! $(":radio:last", $checkedtest).attr("checked"), "Check last radio still NOT checked." );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ ':radio:last', $checkedtest ]), 'attr', [ 'checked' ]), 'Check last radio still NOT checked.' ]);

// *** unit/core.js ***
//   565: 	ok( !! $(":checkbox:first", $checkedtest).attr("checked"), "Check first checkbox still checked." );

$v.cf($v.ro('ok'), [ ! (!$v.cm($v.cf($v.ro('$'), [ ':checkbox:first', $checkedtest ]), 'attr', [ 'checked' ])), 'Check first checkbox still checked.' ]);
$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ ':checkbox:last', $checkedtest ]), 'attr', [ 'checked' ]), 'Check last checkbox still NOT checked.' ]);

// *** unit/core.js ***
//   566: 	ok( ! $(":checkbox:last", $checkedtest).attr("checked"), "Check last checkbox still NOT checked." );

})) ]);
$v.cf($v.ro('test'), [ 'width()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 9 ]);

// *** unit/core.js ***
//   572: 	var $div = $("#nothiddendiv");

var $div = $v.cf($v.ro('$'), [ '#nothiddendiv' ]);
$v.cm($div, 'width', [ 30 ]);

// *** unit/core.js ***
//   574: 	equals($div.width(), 30, "Test set to 30 correctly");

$v.cf($v.ro('equals'), [ $v.cm($div, 'width', [ ]), 30, 'Test set to 30 correctly' ]);
$v.cm($div, 'width', [ -1 ]);

// *** unit/core.js ***
//   576: 	equals($div.width(), 30, "Test negative width ignored");

$v.cf($v.ro('equals'), [ $v.cm($div, 'width', [ ]), 30, 'Test negative width ignored' ]);

// *** unit/core.js ***
//   577: 	$div.css("padding", "20px");

$v.cm($div, 'css', [ 'padding', '20px' ]);

// *** unit/core.js ***
//   578: 	equals($div.width(), 30, "Test padding specified with pixels");

$v.cf($v.ro('equals'), [ $v.cm($div, 'width', [ ]), 30, 'Test padding specified with pixels' ]);

// *** unit/core.js ***
//   579: 	$div.css("border", "2px solid #fff");

$v.cm($div, 'css', [ 'border', '2px solid #fff' ]);

// *** unit/core.js ***
//   580: 	equals($div.width(), 30, "Test border specified with pixels");

$v.cf($v.ro('equals'), [ $v.cm($div, 'width', [ ]), 30, 'Test border specified with pixels' ]);

// *** unit/core.js ***
//   581: 	$div.css("padding", "2em");

$v.cm($div, 'css', [ 'padding', '2em' ]);

// *** unit/core.js ***
//   582: 	equals($div.width(), 30, "Test padding specified with ems");

$v.cf($v.ro('equals'), [ $v.cm($div, 'width', [ ]), 30, 'Test padding specified with ems' ]);

// *** unit/core.js ***
//   583: 	$div.css("border", "1em solid #fff");

$v.cm($div, 'css', [ 'border', '1em solid #fff' ]);

// *** unit/core.js ***
//   584: 	equals($div.width(), 30, "Test border specified with ems");

$v.cf($v.ro('equals'), [ $v.cm($div, 'width', [ ]), 30, 'Test border specified with ems' ]);

// *** unit/core.js ***
//   585: 	$div.css("padding", "2%");

$v.cm($div, 'css', [ 'padding', '2%' ]);

// *** unit/core.js ***
//   586: 	equals($div.width(), 30, "Test padding specified with percent");

$v.cf($v.ro('equals'), [ $v.cm($div, 'width', [ ]), 30, 'Test padding specified with percent' ]);
$v.cm($div, 'hide', [ ]);

// *** unit/core.js ***
//   588: 	equals($div.width(), 30, "Test hidden div");

$v.cf($v.ro('equals'), [ $v.cm($div, 'width', [ ]), 30, 'Test hidden div' ]);

// *** unit/core.js ***
//   590: 	$div.css({ display: "", border: "", padding: "" });

$v.cm($div, 'css', [ ___.initializeMap([ 'display', '', 'border', '', 'padding', '' ]) ]);

// *** unit/core.js ***
//   592: 	$("#nothiddendivchild").css({ padding: "3px", border: "2px solid #fff" });

$v.cm($v.cf($v.ro('$'), [ '#nothiddendivchild' ]), 'css', [ ___.initializeMap([ 'padding', '3px', 'border', '2px solid #fff' ]) ]);

// *** unit/core.js ***
//   593: 	equals($("#nothiddendivchild").width(), 20, "Test child width with border and padding");

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#nothiddendivchild' ]), 'width', [ ]), 20, 'Test child width with border and padding' ]);

// *** unit/core.js ***
//   594: 	$("#nothiddendiv, #nothiddendivchild").css({ border: "", padding: "", width: "" });

$v.cm($v.cf($v.ro('$'), [ '#nothiddendiv, #nothiddendivchild' ]), 'css', [ ___.initializeMap([ 'border', '', 'padding', '', 'width', '' ]) ]);
})) ]);
$v.cf($v.ro('test'), [ 'height()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 8 ]);

// *** unit/core.js ***
//   600: 	var $div = $("#nothiddendiv");

var $div = $v.cf($v.ro('$'), [ '#nothiddendiv' ]);

// *** unit/core.js ***
//   601: 	$div.height(30);

$v.cm($div, 'height', [ 30 ]);

// *** unit/core.js ***
//   602: 	equals($div.height(), 30, "Test set to 30 correctly");

$v.cf($v.ro('equals'), [ $v.cm($div, 'height', [ ]), 30, 'Test set to 30 correctly' ]);

// *** unit/core.js ***
//   603: 	$div.height(-1); // handle negative numbers by ignoring #1599

$v.cm($div, 'height', [ -1 ]);

// *** unit/core.js ***
//   604: 	equals($div.height(), 30, "Test negative height ignored");

$v.cf($v.ro('equals'), [ $v.cm($div, 'height', [ ]), 30, 'Test negative height ignored' ]);

// *** unit/core.js ***
//   605: 	$div.css("padding", "20px");

$v.cm($div, 'css', [ 'padding', '20px' ]);

// *** unit/core.js ***
//   606: 	equals($div.height(), 30, "Test padding specified with pixels");

$v.cf($v.ro('equals'), [ $v.cm($div, 'height', [ ]), 30, 'Test padding specified with pixels' ]);

// *** unit/core.js ***
//   607: 	$div.css("border", "2px solid #fff");

$v.cm($div, 'css', [ 'border', '2px solid #fff' ]);

// *** unit/core.js ***
//   608: 	equals($div.height(), 30, "Test border specified with pixels");

$v.cf($v.ro('equals'), [ $v.cm($div, 'height', [ ]), 30, 'Test border specified with pixels' ]);

// *** unit/core.js ***
//   609: 	$div.css("padding", "2em");

$v.cm($div, 'css', [ 'padding', '2em' ]);

// *** unit/core.js ***
//   610: 	equals($div.height(), 30, "Test padding specified with ems");

$v.cf($v.ro('equals'), [ $v.cm($div, 'height', [ ]), 30, 'Test padding specified with ems' ]);

// *** unit/core.js ***
//   611: 	$div.css("border", "1em solid #fff");

$v.cm($div, 'css', [ 'border', '1em solid #fff' ]);

// *** unit/core.js ***
//   612: 	equals($div.height(), 30, "Test border specified with ems");

$v.cf($v.ro('equals'), [ $v.cm($div, 'height', [ ]), 30, 'Test border specified with ems' ]);

// *** unit/core.js ***
//   613: 	$div.css("padding", "2%");

$v.cm($div, 'css', [ 'padding', '2%' ]);

// *** unit/core.js ***
//   614: 	equals($div.height(), 30, "Test padding specified with percent");

$v.cf($v.ro('equals'), [ $v.cm($div, 'height', [ ]), 30, 'Test padding specified with percent' ]);
$v.cm($div, 'hide', [ ]);

// *** unit/core.js ***
//   616: 	equals($div.height(), 30, "Test hidden div");

$v.cf($v.ro('equals'), [ $v.cm($div, 'height', [ ]), 30, 'Test hidden div' ]);
$v.cm($div, 'css', [ ___.initializeMap([ 'display', '', 'border', '', 'padding', '', 'height', '1px' ]) ]);

// *** unit/core.js ***
//   618: 	$div.css({ display: "", border: "", padding: "", height: "1px" });

})) ]);
$v.cf($v.ro('test'), [ 'text()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);

// *** unit/core.js ***
//   623: 	var expected = "This link has class=\"blog\": Simon Willison's Weblog";

var expected = 'This link has class=\"blog\": Simon Willison\'s Weblog';

// *** unit/core.js ***
//   624: 	equals( $('#sap').text(), expected, 'Check for merged text of more then one element.' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), expected, 'Check for merged text of more then one element.' ]);
})) ]);

// *** unit/core.js ***
//   627: test("wrap(String|Element)", function() {

$v.cf($v.ro('test'), [ 'wrap(String|Element)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 8 ]);

// *** unit/core.js ***
//   629: 	var defaultText = 'Try them out:'

var defaultText = 'Try them out:';

// *** unit/core.js ***
//   630: 	var result = $('#first').wrap('<div class="red"><span><�span><�div>').text();

var result = $v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'wrap', [ '\x3cdiv class=\"red\"\x3e\x3cspan\x3e\x3c/span\x3e\x3c/div\x3e' ]), 'text', [ ]);

// *** unit/core.js ***
//   631: 	equals( defaultText, result, 'Check for wrapping of on-the-fly html' );

$v.cf($v.ro('equals'), [ defaultText, result, 'Check for wrapping of on-the-fly html' ]);

// *** unit/core.js ***
//   632: 	ok( $('#first').parent().parent().is('.red'), 'Check if wrapper has class "red"' );

$v.cf($v.ro('ok'), [ $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'parent', [ ]), 'parent', [ ]), 'is', [ '.red' ]), 'Check if wrapper has class \"red\"' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   635: 	var defaultText = 'Try them out:'

var defaultText = 'Try them out:';

// *** unit/core.js ***
//   636: 	var result = $('#first').wrap(document.getElementById('empty')).parent();

var result = $v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'wrap', [ $v.cm($v.ro('document'), 'getElementById', [ 'empty' ]) ]), 'parent', [ ]);

// *** unit/core.js ***
//   637: 	ok( result.is('ol'), 'Check for element wrapping' );

$v.cf($v.ro('ok'), [ $v.cm(result, 'is', [ 'ol' ]), 'Check for element wrapping' ]);

// *** unit/core.js ***
//   638: 	equals( result.text(), defaultText, 'Check for element wrapping' );

$v.cf($v.ro('equals'), [ $v.cm(result, 'text', [ ]), defaultText, 'Check for element wrapping' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   641: 	$('#check1').click(function() {

$v.cm($v.cm($v.cf($v.ro('$'), [ '#check1' ]), 'click', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/core.js ***
//   642: 		var checkbox = this;

var checkbox = $dis;

// *** unit/core.js ***
//   643: 		ok( checkbox.checked, "Checkbox's state is erased after wrap() action, see #769" );

$v.cf($v.ro('ok'), [ $v.r(checkbox, 'checked'), 'Checkbox\'s state is erased after wrap() action, see #769' ]);

// *** unit/core.js ***
//   644: 		$(checkbox).wrap( '<div id="c1" style="display:none;"><�div>' );

$v.cm($v.cf($v.ro('$'), [ checkbox ]), 'wrap', [ '\x3cdiv id=\"c1\" style=\"display:none;\"\x3e\x3c/div\x3e' ]);
$v.cf($v.ro('ok'), [ $v.r(checkbox, 'checked'), 'Checkbox\'s state is erased after wrap() action, see #769' ]);

// *** unit/core.js ***
//   645: 		ok( checkbox.checked, "Checkbox's state is erased after wrap() action, see #769" );

})) ]), 'click', [ ]);

// *** unit/core.js ***
//   649: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);

// *** unit/core.js ***
//   650: 	j.wrap("<i><�i>");

$v.cm(j, 'wrap', [ '\x3ci\x3e\x3c/i\x3e' ]);

// *** unit/core.js ***
//   651: 	equals( $("#nonnodes > i").length, 3, "Check node,textnode,comment wraps ok" );

$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ '#nonnodes \x3e i' ]), 'length'), 3, 'Check node,textnode,comment wraps ok' ]);

// *** unit/core.js ***
//   652: 	equals( $("#nonnodes > i").text(), j.text() + j[1].nodeValue, "Check node,textnode,comment wraps doesn't hurt text" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#nonnodes \x3e i' ]), 'text', [ ]), $v.cm(j, 'text', [ ]) + $v.r($v.r(j, 1), 'nodeValue'), 'Check node,textnode,comment wraps doesn\'t hurt text' ]);
})) ]);

// *** unit/core.js ***
//   655: test("wrapAll(String|Element)", function() {

$v.cf($v.ro('test'), [ 'wrapAll(String|Element)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 8 ]);

// *** unit/core.js ***
//   657: 	var prev = $("#first")[0].previousSibling;

var prev = $v.r($v.r($v.cf($v.ro('$'), [ '#first' ]), 0), 'previousSibling');

// *** unit/core.js ***
//   658: 	var p = $("#first")[0].parentNode;

var p = $v.r($v.r($v.cf($v.ro('$'), [ '#first' ]), 0), 'parentNode');

// *** unit/core.js ***
//   659: 	var result = $('#first,#firstp').wrapAll('<div class="red"><div id="tmp"><�div><�div>');

var result = $v.cm($v.cf($v.ro('$'), [ '#first,#firstp' ]), 'wrapAll', [ '\x3cdiv class=\"red\"\x3e\x3cdiv id=\"tmp\"\x3e\x3c/div\x3e\x3c/div\x3e' ]);

// *** unit/core.js ***
//   660: 	equals( result.parent().length, 1, 'Check for wrapping of on-the-fly html' );

$v.cf($v.ro('equals'), [ $v.r($v.cm(result, 'parent', [ ]), 'length'), 1, 'Check for wrapping of on-the-fly html' ]);

// *** unit/core.js ***
//   661: 	ok( $('#first').parent().parent().is('.red'), 'Check if wrapper has class "red"' );

$v.cf($v.ro('ok'), [ $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'parent', [ ]), 'parent', [ ]), 'is', [ '.red' ]), 'Check if wrapper has class \"red\"' ]);

// *** unit/core.js ***
//   662: 	ok( $('#firstp').parent().parent().is('.red'), 'Check if wrapper has class "red"' );

$v.cf($v.ro('ok'), [ $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'parent', [ ]), 'parent', [ ]), 'is', [ '.red' ]), 'Check if wrapper has class \"red\"' ]);

// *** unit/core.js ***
//   663: 	equals( $("#first").parent().parent()[0].previousSibling, prev, "Correct Previous Sibling" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'parent', [ ]), 'parent', [ ]), 0), 'previousSibling'), prev, 'Correct Previous Sibling' ]);

// *** unit/core.js ***
//   664: 	equals( $("#first").parent().parent()[0].parentNode, p, "Correct Parent" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'parent', [ ]), 'parent', [ ]), 0), 'parentNode'), p, 'Correct Parent' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   667: 	var prev = $("#first")[0].previousSibling;

var prev = $v.r($v.r($v.cf($v.ro('$'), [ '#first' ]), 0), 'previousSibling');

// *** unit/core.js ***
//   668: 	var p = $("#first")[0].parentNode;

var p = $v.r($v.r($v.cf($v.ro('$'), [ '#first' ]), 0), 'parentNode');

// *** unit/core.js ***
//   669: 	var result = $('#first,#firstp').wrapAll(document.getElementById('empty'));

var result = $v.cm($v.cf($v.ro('$'), [ '#first,#firstp' ]), 'wrapAll', [ $v.cm($v.ro('document'), 'getElementById', [ 'empty' ]) ]);

// *** unit/core.js ***
//   670: 	equals( $("#first").parent()[0], $("#firstp").parent()[0], "Same Parent" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'parent', [ ]), 0), $v.r($v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'parent', [ ]), 0), 'Same Parent' ]);

// *** unit/core.js ***
//   671: 	equals( $("#first").parent()[0].previousSibling, prev, "Correct Previous Sibling" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'parent', [ ]), 0), 'previousSibling'), prev, 'Correct Previous Sibling' ]);
$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'parent', [ ]), 0), 'parentNode'), p, 'Correct Parent' ]);

// *** unit/core.js ***
//   672: 	equals( $("#first").parent()[0].parentNode, p, "Correct Parent" );

})) ]);

// *** unit/core.js ***
//   675: test("wrapInner(String|Element)", function() {

$v.cf($v.ro('test'), [ 'wrapInner(String|Element)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/core.js ***
//   677: 	var num = $("#first").children().length;

var num = $v.r($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'children', [ ]), 'length');

// *** unit/core.js ***
//   678: 	var result = $('#first').wrapInner('<div class="red"><div id="tmp"><�div><�div>');

var result = $v.cm($v.cf($v.ro('$'), [ '#first' ]), 'wrapInner', [ '\x3cdiv class=\"red\"\x3e\x3cdiv id=\"tmp\"\x3e\x3c/div\x3e\x3c/div\x3e' ]);

// *** unit/core.js ***
//   679: 	equals( $("#first").children().length, 1, "Only one child" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'children', [ ]), 'length'), 1, 'Only one child' ]);

// *** unit/core.js ***
//   680: 	ok( $("#first").children().is(".red"), "Verify Right Element" );

$v.cf($v.ro('ok'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'children', [ ]), 'is', [ '.red' ]), 'Verify Right Element' ]);

// *** unit/core.js ***
//   681: 	equals( $("#first").children().children().children().length, num, "Verify Elements Intact" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'children', [ ]), 'children', [ ]), 'children', [ ]), 'length'), num, 'Verify Elements Intact' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   684: 	var num = $("#first").children().length;

var num = $v.r($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'children', [ ]), 'length');

// *** unit/core.js ***
//   685: 	var result = $('#first').wrapInner(document.getElementById('empty'));

var result = $v.cm($v.cf($v.ro('$'), [ '#first' ]), 'wrapInner', [ $v.cm($v.ro('document'), 'getElementById', [ 'empty' ]) ]);

// *** unit/core.js ***
//   686: 	equals( $("#first").children().length, 1, "Only one child" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'children', [ ]), 'length'), 1, 'Only one child' ]);

// *** unit/core.js ***
//   687: 	ok( $("#first").children().is("#empty"), "Verify Right Element" );

$v.cf($v.ro('ok'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'children', [ ]), 'is', [ '#empty' ]), 'Verify Right Element' ]);

// *** unit/core.js ***
//   688: 	equals( $("#first").children().children().length, num, "Verify Elements Intact" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'children', [ ]), 'children', [ ]), 'length'), num, 'Verify Elements Intact' ]);
})) ]);

// *** unit/core.js ***
//   691: test("append(String|Element|Array&lt;Element&gt;|jQuery)", function() {

$v.cf($v.ro('test'), [ 'append(String|Element|Array\x26lt;Element\x26gt;|jQuery)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 21 ]);

// *** unit/core.js ***
//   693: 	var defaultText = 'Try them out:'

var defaultText = 'Try them out:';

// *** unit/core.js ***
//   694: 	var result = $('#first').append('<b>buga<�b>');

var result = $v.cm($v.cf($v.ro('$'), [ '#first' ]), 'append', [ '\x3cb\x3ebuga\x3c/b\x3e' ]);

// *** unit/core.js ***
//   695: 	equals( result.text(), defaultText + 'buga', 'Check if text appending works' );

$v.cf($v.ro('equals'), [ $v.cm(result, 'text', [ ]), defaultText + 'buga', 'Check if text appending works' ]);

// *** unit/core.js ***
//   696: 	equals( $('#select3').append('<option value="appendTest">Append Test<�option>').find('option:last-child').attr('value'), 'appendTest', 'Appending html options to select element');

$v.cf($v.ro('equals'), [ $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#select3' ]), 'append', [ '\x3coption value=\"appendTest\"\x3eAppend Test\x3c/option\x3e' ]), 'find', [ 'option:last-child' ]), 'attr', [ 'value' ]), 'appendTest', 'Appending html options to select element' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   699: 	var expected = "This link has class=\"blog\": Simon Willison's WeblogTry them out:";

var expected = 'This link has class=\"blog\": Simon Willison\'s WeblogTry them out:';

// *** unit/core.js ***
//   700: 	$('#sap').append(document.getElementById('first'));

$v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'append', [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ]);

// *** unit/core.js ***
//   701: 	equals( expected, $('#sap').text(), "Check for appending of element" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for appending of element' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   704: 	expected = "This link has class=\"blog\": Simon Willison's WeblogTry them out:Yahoo";

expected = 'This link has class=\"blog\": Simon Willison\'s WeblogTry them out:Yahoo';

// *** unit/core.js ***
//   705: 	$('#sap').append([document.getElementById('first'), document.getElementById('yahoo')]);

$v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'append', [ [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]), $v.cm($v.ro('document'), 'getElementById', [ 'yahoo' ]) ] ]);

// *** unit/core.js ***
//   706: 	equals( expected, $('#sap').text(), "Check for appending of array of elements" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for appending of array of elements' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   709: 	expected = "This link has class=\"blog\": Simon Willison's WeblogTry them out:Yahoo";

expected = 'This link has class=\"blog\": Simon Willison\'s WeblogTry them out:Yahoo';

// *** unit/core.js ***
//   710: 	$('#sap').append($("#first, #yahoo"));

$v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'append', [ $v.cf($v.ro('$'), [ '#first, #yahoo' ]) ]);

// *** unit/core.js ***
//   711: 	equals( expected, $('#sap').text(), "Check for appending of jQuery object" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for appending of jQuery object' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   714: 	$("#sap").append( 5 );

$v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'append', [ 5 ]);

// *** unit/core.js ***
//   715: 	ok( $("#sap")[0].innerHTML.match( /5$/ ), "Check for appending a number" );

$v.cf($v.ro('ok'), [ $v.cm($v.r($v.r($v.cf($v.ro('$'), [ '#sap' ]), 0), 'innerHTML'), 'match', [ $v.construct(RegExp, [ '5$' ]) ]), 'Check for appending a number' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   718: 	$("#sap").append( " text with spaces " );

$v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'append', [ ' text with spaces ' ]);

// *** unit/core.js ***
//   719: 	ok( $("#sap")[0].innerHTML.match(/ text with spaces $/), "Check for appending text with spaces" );

$v.cf($v.ro('ok'), [ $v.cm($v.r($v.r($v.cf($v.ro('$'), [ '#sap' ]), 0), 'innerHTML'), 'match', [ $v.construct(RegExp, [ ' text with spaces $' ]) ]), 'Check for appending text with spaces' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   722: 	ok( $("#sap").append([]), "Check for appending an empty array." );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'append', [ [ ] ]), 'Check for appending an empty array.' ]);

// *** unit/core.js ***
//   723: 	ok( $("#sap").append(""), "Check for appending an empty string." );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'append', [ '' ]), 'Check for appending an empty string.' ]);

// *** unit/core.js ***
//   724: 	ok( $("#sap").append(document.getElementsByTagName("foo")), "Check for appending an empty nodelist." );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'append', [ $v.cm($v.ro('document'), 'getElementsByTagName', [ 'foo' ]) ]), 'Check for appending an empty nodelist.' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   727: 	$("#sap").append(document.getElementById('form'));

$v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'append', [ $v.cm($v.ro('document'), 'getElementById', [ 'form' ]) ]);

// *** unit/core.js ***
//   728: 	equals( $("#sap>form").size(), 1, "Check for appending a form" ); // Bug #910

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#sap\x3eform' ]), 'size', [ ]), 1, 'Check for appending a form' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   731: 	var pass = true;

var pass = true;

// *** unit/core.js ***
//   732: 	try {
//   733: 		$( $("#iframe")[0].contentWindow.document.body ).append("<div>test<�div>");
//   736: 	}

try {
$v.cm($v.cf($v.ro('$'), [ $v.r($v.r($v.r($v.r($v.cf($v.ro('$'), [ '#iframe' ]), 0), 'contentWindow'), 'document'), 'body') ]), 'append', [ '\x3cdiv\x3etest\x3c/div\x3e' ]);
} catch (ex___) {

// *** unit/core.js ***
//   734: 	} catch(e) {

try {
throw ___.tameException(ex___);
} catch (e) {

// *** unit/core.js ***
//   735: 		pass = false;

pass = false;
}
}

// *** unit/core.js ***
//   738: 	ok( pass, "Test for appending a DOM node to the contents of an IFrame" );

$v.cf($v.ro('ok'), [ pass, 'Test for appending a DOM node to the contents of an IFrame' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   741: 	$('<fieldset/>').appendTo('#form').append('<legend id="legend">test<�legend>');

$v.cm($v.cm($v.cf($v.ro('$'), [ '\x3cfieldset/\x3e' ]), 'appendTo', [ '#form' ]), 'append', [ '\x3clegend id=\"legend\"\x3etest\x3c/legend\x3e' ]);

// *** unit/core.js ***
//   742: 	t( 'Append legend', '#legend', ['legend'] );

$v.cf($v.ro('t'), [ 'Append legend', '#legend', [ 'legend' ] ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   745: 	$('#select1').append('<OPTION>Test<�OPTION>');

$v.cm($v.cf($v.ro('$'), [ '#select1' ]), 'append', [ '\x3cOPTION\x3eTest\x3c/OPTION\x3e' ]);

// *** unit/core.js ***
//   746: 	equals( $('#select1 option:last').text(), "Test", "Appending &lt;OPTION&gt; (all caps)" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#select1 option:last' ]), 'text', [ ]), 'Test', 'Appending \x26lt;OPTION\x26gt; (all caps)' ]);

// *** unit/core.js ***
//   748: 	$('#table').append('<colgroup><�colgroup>');

$v.cm($v.cf($v.ro('$'), [ '#table' ]), 'append', [ '\x3ccolgroup\x3e\x3c/colgroup\x3e' ]);

// *** unit/core.js ***
//   749: 	ok( $('#table colgroup').length, "Append colgroup" );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#table colgroup' ]), 'length'), 'Append colgroup' ]);

// *** unit/core.js ***
//   751: 	$('#table colgroup').append('<col/>');

$v.cm($v.cf($v.ro('$'), [ '#table colgroup' ]), 'append', [ '\x3ccol/\x3e' ]);

// *** unit/core.js ***
//   752: 	ok( $('#table colgroup col').length, "Append col" );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#table colgroup col' ]), 'length'), 'Append col' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   755: 	$('#table').append('<caption><�caption>');

$v.cm($v.cf($v.ro('$'), [ '#table' ]), 'append', [ '\x3ccaption\x3e\x3c/caption\x3e' ]);

// *** unit/core.js ***
//   756: 	ok( $('#table caption').length, "Append caption" );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#table caption' ]), 'length'), 'Append caption' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   760: 		.append('<select id="appendSelect1"><�select>')
//   761: 		.append('<select id="appendSelect2"><option>Test<�option><�select>');

$v.cm($v.cm($v.cf($v.ro('$'), [ 'form:last' ]), 'append', [ '\x3cselect id=\"appendSelect1\"\x3e\x3c/select\x3e' ]), 'append', [ '\x3cselect id=\"appendSelect2\"\x3e\x3coption\x3eTest\x3c/option\x3e\x3c/select\x3e' ]);

// *** unit/core.js ***
//   763: 	t( "Append Select", "#appendSelect1, #appendSelect2", ["appendSelect1", "appendSelect2"] );

$v.cf($v.ro('t'), [ 'Append Select', '#appendSelect1, #appendSelect2', [ 'appendSelect1', 'appendSelect2' ] ]);

// *** unit/core.js ***
//   766: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);

// *** unit/core.js ***
//   767: 	var d = $("<div/>").appendTo("#nonnodes").append(j);

var d = $v.cm($v.cm($v.cf($v.ro('$'), [ '\x3cdiv/\x3e' ]), 'appendTo', [ '#nonnodes' ]), 'append', [ j ]);

// *** unit/core.js ***
//   768: 	equals( $("#nonnodes").length, 1, "Check node,textnode,comment append moved leaving just the div" );

$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ '#nonnodes' ]), 'length'), 1, 'Check node,textnode,comment append moved leaving just the div' ]);

// *** unit/core.js ***
//   769: 	ok( d.contents().length >= 2, "Check node,textnode,comment append works" );

$v.cf($v.ro('ok'), [ $v.r($v.cm(d, 'contents', [ ]), 'length') >= 2, 'Check node,textnode,comment append works' ]);

// *** unit/core.js ***
//   770: 	d.contents().appendTo("#nonnodes");

$v.cm($v.cm(d, 'contents', [ ]), 'appendTo', [ '#nonnodes' ]);
$v.cm(d, 'remove', [ ]);
$v.cf($v.ro('ok'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]), 'length') >= 2, 'Check node,textnode,comment append cleanup worked' ]);

// *** unit/core.js ***
//   772: 	ok( $("#nonnodes").contents().length >= 2, "Check node,textnode,comment append cleanup worked" );

})) ]);

// *** unit/core.js ***
//   775: test("appendTo(String|Element|Array&lt;Element&gt;|jQuery)", function() {

$v.cf($v.ro('test'), [ 'appendTo(String|Element|Array\x26lt;Element\x26gt;|jQuery)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/core.js ***
//   777: 	var defaultText = 'Try them out:'

var defaultText = 'Try them out:';

// *** unit/core.js ***
//   778: 	$('<b>buga<�b>').appendTo('#first');

$v.cm($v.cf($v.ro('$'), [ '\x3cb\x3ebuga\x3c/b\x3e' ]), 'appendTo', [ '#first' ]);

// *** unit/core.js ***
//   779: 	equals( $("#first").text(), defaultText + 'buga', 'Check if text appending works' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#first' ]), 'text', [ ]), defaultText + 'buga', 'Check if text appending works' ]);

// *** unit/core.js ***
//   780: 	equals( $('<option value="appendTest">Append Test<�option>').appendTo('#select3').parent().find('option:last-child').attr('value'), 'appendTest', 'Appending html options to select element');

$v.cf($v.ro('equals'), [ $v.cm($v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '\x3coption value=\"appendTest\"\x3eAppend Test\x3c/option\x3e' ]), 'appendTo', [ '#select3' ]), 'parent', [ ]), 'find', [ 'option:last-child' ]), 'attr', [ 'value' ]), 'appendTest', 'Appending html options to select element' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   783: 	var expected = "This link has class=\"blog\": Simon Willison's WeblogTry them out:";

var expected = 'This link has class=\"blog\": Simon Willison\'s WeblogTry them out:';

// *** unit/core.js ***
//   784: 	$(document.getElementById('first')).appendTo('#sap');

$v.cm($v.cf($v.ro('$'), [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ]), 'appendTo', [ '#sap' ]);

// *** unit/core.js ***
//   785: 	equals( expected, $('#sap').text(), "Check for appending of element" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for appending of element' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   788: 	expected = "This link has class=\"blog\": Simon Willison's WeblogTry them out:Yahoo";

expected = 'This link has class=\"blog\": Simon Willison\'s WeblogTry them out:Yahoo';

// *** unit/core.js ***
//   789: 	$([document.getElementById('first'), document.getElementById('yahoo')]).appendTo('#sap');

$v.cm($v.cf($v.ro('$'), [ [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]), $v.cm($v.ro('document'), 'getElementById', [ 'yahoo' ]) ] ]), 'appendTo', [ '#sap' ]);

// *** unit/core.js ***
//   790: 	equals( expected, $('#sap').text(), "Check for appending of array of elements" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for appending of array of elements' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   793: 	expected = "This link has class=\"blog\": Simon Willison's WeblogTry them out:Yahoo";

expected = 'This link has class=\"blog\": Simon Willison\'s WeblogTry them out:Yahoo';

// *** unit/core.js ***
//   794: 	$("#first, #yahoo").appendTo('#sap');

$v.cm($v.cf($v.ro('$'), [ '#first, #yahoo' ]), 'appendTo', [ '#sap' ]);

// *** unit/core.js ***
//   795: 	equals( expected, $('#sap').text(), "Check for appending of jQuery object" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for appending of jQuery object' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   798: 	$('#select1').appendTo('#foo');

$v.cm($v.cf($v.ro('$'), [ '#select1' ]), 'appendTo', [ '#foo' ]);
$v.cf($v.ro('t'), [ 'Append select', '#foo select', [ 'select1' ] ]);

// *** unit/core.js ***
//   799: 	t( 'Append select', '#foo select', ['select1'] );

})) ]);

// *** unit/core.js ***
//   802: test("prepend(String|Element|Array&lt;Element&gt;|jQuery)", function() {

$v.cf($v.ro('test'), [ 'prepend(String|Element|Array\x26lt;Element\x26gt;|jQuery)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 5 ]);

// *** unit/core.js ***
//   804: 	var defaultText = 'Try them out:'

var defaultText = 'Try them out:';

// *** unit/core.js ***
//   805: 	var result = $('#first').prepend('<b>buga<�b>');

var result = $v.cm($v.cf($v.ro('$'), [ '#first' ]), 'prepend', [ '\x3cb\x3ebuga\x3c/b\x3e' ]);

// *** unit/core.js ***
//   806: 	equals( result.text(), 'buga' + defaultText, 'Check if text prepending works' );

$v.cf($v.ro('equals'), [ $v.cm(result, 'text', [ ]), 'buga' + defaultText, 'Check if text prepending works' ]);

// *** unit/core.js ***
//   807: 	equals( $('#select3').prepend('<option value="prependTest">Prepend Test<�option>').find('option:first-child').attr('value'), 'prependTest', 'Prepending html options to select element');

$v.cf($v.ro('equals'), [ $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#select3' ]), 'prepend', [ '\x3coption value=\"prependTest\"\x3ePrepend Test\x3c/option\x3e' ]), 'find', [ 'option:first-child' ]), 'attr', [ 'value' ]), 'prependTest', 'Prepending html options to select element' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   810: 	var expected = "Try them out:This link has class=\"blog\": Simon Willison's Weblog";

var expected = 'Try them out:This link has class=\"blog\": Simon Willison\'s Weblog';

// *** unit/core.js ***
//   811: 	$('#sap').prepend(document.getElementById('first'));

$v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'prepend', [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ]);

// *** unit/core.js ***
//   812: 	equals( expected, $('#sap').text(), "Check for prepending of element" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for prepending of element' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   815: 	expected = "Try them out:YahooThis link has class=\"blog\": Simon Willison's Weblog";

expected = 'Try them out:YahooThis link has class=\"blog\": Simon Willison\'s Weblog';

// *** unit/core.js ***
//   816: 	$('#sap').prepend([document.getElementById('first'), document.getElementById('yahoo')]);

$v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'prepend', [ [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]), $v.cm($v.ro('document'), 'getElementById', [ 'yahoo' ]) ] ]);

// *** unit/core.js ***
//   817: 	equals( expected, $('#sap').text(), "Check for prepending of array of elements" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for prepending of array of elements' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   820: 	expected = "Try them out:YahooThis link has class=\"blog\": Simon Willison's Weblog";

expected = 'Try them out:YahooThis link has class=\"blog\": Simon Willison\'s Weblog';

// *** unit/core.js ***
//   821: 	$('#sap').prepend($("#first, #yahoo"));

$v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'prepend', [ $v.cf($v.ro('$'), [ '#first, #yahoo' ]) ]);
$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for prepending of jQuery object' ]);

// *** unit/core.js ***
//   822: 	equals( expected, $('#sap').text(), "Check for prepending of jQuery object" );

})) ]);

// *** unit/core.js ***
//   825: test("prependTo(String|Element|Array&lt;Element&gt;|jQuery)", function() {

$v.cf($v.ro('test'), [ 'prependTo(String|Element|Array\x26lt;Element\x26gt;|jQuery)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/core.js ***
//   827: 	var defaultText = 'Try them out:'

var defaultText = 'Try them out:';

// *** unit/core.js ***
//   828: 	$('<b>buga<�b>').prependTo('#first');

$v.cm($v.cf($v.ro('$'), [ '\x3cb\x3ebuga\x3c/b\x3e' ]), 'prependTo', [ '#first' ]);

// *** unit/core.js ***
//   829: 	equals( $('#first').text(), 'buga' + defaultText, 'Check if text prepending works' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#first' ]), 'text', [ ]), 'buga' + defaultText, 'Check if text prepending works' ]);

// *** unit/core.js ***
//   830: 	equals( $('<option value="prependTest">Prepend Test<�option>').prependTo('#select3').parent().find('option:first-child').attr('value'), 'prependTest', 'Prepending html options to select element');

$v.cf($v.ro('equals'), [ $v.cm($v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '\x3coption value=\"prependTest\"\x3ePrepend Test\x3c/option\x3e' ]), 'prependTo', [ '#select3' ]), 'parent', [ ]), 'find', [ 'option:first-child' ]), 'attr', [ 'value' ]), 'prependTest', 'Prepending html options to select element' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   833: 	var expected = "Try them out:This link has class=\"blog\": Simon Willison's Weblog";

var expected = 'Try them out:This link has class=\"blog\": Simon Willison\'s Weblog';

// *** unit/core.js ***
//   834: 	$(document.getElementById('first')).prependTo('#sap');

$v.cm($v.cf($v.ro('$'), [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ]), 'prependTo', [ '#sap' ]);

// *** unit/core.js ***
//   835: 	equals( expected, $('#sap').text(), "Check for prepending of element" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for prepending of element' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   838: 	expected = "Try them out:YahooThis link has class=\"blog\": Simon Willison's Weblog";

expected = 'Try them out:YahooThis link has class=\"blog\": Simon Willison\'s Weblog';

// *** unit/core.js ***
//   839: 	$([document.getElementById('yahoo'), document.getElementById('first')]).prependTo('#sap');

$v.cm($v.cf($v.ro('$'), [ [ $v.cm($v.ro('document'), 'getElementById', [ 'yahoo' ]), $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ] ]), 'prependTo', [ '#sap' ]);

// *** unit/core.js ***
//   840: 	equals( expected, $('#sap').text(), "Check for prepending of array of elements" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for prepending of array of elements' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   843: 	expected = "Try them out:YahooThis link has class=\"blog\": Simon Willison's Weblog";

expected = 'Try them out:YahooThis link has class=\"blog\": Simon Willison\'s Weblog';

// *** unit/core.js ***
//   844: 	$("#yahoo, #first").prependTo('#sap');

$v.cm($v.cf($v.ro('$'), [ '#yahoo, #first' ]), 'prependTo', [ '#sap' ]);

// *** unit/core.js ***
//   845: 	equals( expected, $('#sap').text(), "Check for prepending of jQuery object" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#sap' ]), 'text', [ ]), 'Check for prepending of jQuery object' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   848: 	$('<select id="prependSelect1"><�select>').prependTo('form:last');

$v.cm($v.cf($v.ro('$'), [ '\x3cselect id=\"prependSelect1\"\x3e\x3c/select\x3e' ]), 'prependTo', [ 'form:last' ]);

// *** unit/core.js ***
//   849: 	$('<select id="prependSelect2"><option>Test<�option><�select>').prependTo('form:last');

$v.cm($v.cf($v.ro('$'), [ '\x3cselect id=\"prependSelect2\"\x3e\x3coption\x3eTest\x3c/option\x3e\x3c/select\x3e' ]), 'prependTo', [ 'form:last' ]);
$v.cf($v.ro('t'), [ 'Prepend Select', '#prependSelect1, #prependSelect2', [ 'prependSelect1', 'prependSelect2' ] ]);

// *** unit/core.js ***
//   851: 	t( "Prepend Select", "#prependSelect1, #prependSelect2", ["prependSelect1", "prependSelect2"] );

})) ]);

// *** unit/core.js ***
//   854: test("before(String|Element|Array&lt;Element&gt;|jQuery)", function() {

$v.cf($v.ro('test'), [ 'before(String|Element|Array\x26lt;Element\x26gt;|jQuery)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//   856: 	var expected = 'This is a normal link: bugaYahoo';

var expected = 'This is a normal link: bugaYahoo';

// *** unit/core.js ***
//   857: 	$('#yahoo').before('<b>buga<�b>');

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'before', [ '\x3cb\x3ebuga\x3c/b\x3e' ]);

// *** unit/core.js ***
//   858: 	equals( expected, $('#en').text(), 'Insert String before' );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert String before' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   861: 	expected = "This is a normal link: Try them out:Yahoo";

expected = 'This is a normal link: Try them out:Yahoo';

// *** unit/core.js ***
//   862: 	$('#yahoo').before(document.getElementById('first'));

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'before', [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ]);

// *** unit/core.js ***
//   863: 	equals( expected, $('#en').text(), "Insert element before" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert element before' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   866: 	expected = "This is a normal link: Try them out:diveintomarkYahoo";

expected = 'This is a normal link: Try them out:diveintomarkYahoo';

// *** unit/core.js ***
//   867: 	$('#yahoo').before([document.getElementById('first'), document.getElementById('mark')]);

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'before', [ [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]), $v.cm($v.ro('document'), 'getElementById', [ 'mark' ]) ] ]);

// *** unit/core.js ***
//   868: 	equals( expected, $('#en').text(), "Insert array of elements before" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert array of elements before' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   871: 	expected = "This is a normal link: Try them out:diveintomarkYahoo";

expected = 'This is a normal link: Try them out:diveintomarkYahoo';

// *** unit/core.js ***
//   872: 	$('#yahoo').before($("#first, #mark"));

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'before', [ $v.cf($v.ro('$'), [ '#first, #mark' ]) ]);
$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert jQuery before' ]);

// *** unit/core.js ***
//   873: 	equals( expected, $('#en').text(), "Insert jQuery before" );

})) ]);

// *** unit/core.js ***
//   876: test("insertBefore(String|Element|Array&lt;Element&gt;|jQuery)", function() {

$v.cf($v.ro('test'), [ 'insertBefore(String|Element|Array\x26lt;Element\x26gt;|jQuery)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//   878: 	var expected = 'This is a normal link: bugaYahoo';

var expected = 'This is a normal link: bugaYahoo';

// *** unit/core.js ***
//   879: 	$('<b>buga<�b>').insertBefore('#yahoo');

$v.cm($v.cf($v.ro('$'), [ '\x3cb\x3ebuga\x3c/b\x3e' ]), 'insertBefore', [ '#yahoo' ]);

// *** unit/core.js ***
//   880: 	equals( expected, $('#en').text(), 'Insert String before' );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert String before' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   883: 	expected = "This is a normal link: Try them out:Yahoo";

expected = 'This is a normal link: Try them out:Yahoo';

// *** unit/core.js ***
//   884: 	$(document.getElementById('first')).insertBefore('#yahoo');

$v.cm($v.cf($v.ro('$'), [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ]), 'insertBefore', [ '#yahoo' ]);

// *** unit/core.js ***
//   885: 	equals( expected, $('#en').text(), "Insert element before" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert element before' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   888: 	expected = "This is a normal link: Try them out:diveintomarkYahoo";

expected = 'This is a normal link: Try them out:diveintomarkYahoo';

// *** unit/core.js ***
//   889: 	$([document.getElementById('first'), document.getElementById('mark')]).insertBefore('#yahoo');

$v.cm($v.cf($v.ro('$'), [ [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]), $v.cm($v.ro('document'), 'getElementById', [ 'mark' ]) ] ]), 'insertBefore', [ '#yahoo' ]);

// *** unit/core.js ***
//   890: 	equals( expected, $('#en').text(), "Insert array of elements before" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert array of elements before' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   893: 	expected = "This is a normal link: Try them out:diveintomarkYahoo";

expected = 'This is a normal link: Try them out:diveintomarkYahoo';

// *** unit/core.js ***
//   894: 	$("#first, #mark").insertBefore('#yahoo');

$v.cm($v.cf($v.ro('$'), [ '#first, #mark' ]), 'insertBefore', [ '#yahoo' ]);
$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert jQuery before' ]);

// *** unit/core.js ***
//   895: 	equals( expected, $('#en').text(), "Insert jQuery before" );

})) ]);

// *** unit/core.js ***
//   898: test("after(String|Element|Array&lt;Element&gt;|jQuery)", function() {

$v.cf($v.ro('test'), [ 'after(String|Element|Array\x26lt;Element\x26gt;|jQuery)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//   900: 	var expected = 'This is a normal link: Yahoobuga';

var expected = 'This is a normal link: Yahoobuga';

// *** unit/core.js ***
//   901: 	$('#yahoo').after('<b>buga<�b>');

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'after', [ '\x3cb\x3ebuga\x3c/b\x3e' ]);

// *** unit/core.js ***
//   902: 	equals( expected, $('#en').text(), 'Insert String after' );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert String after' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   905: 	expected = "This is a normal link: YahooTry them out:";

expected = 'This is a normal link: YahooTry them out:';

// *** unit/core.js ***
//   906: 	$('#yahoo').after(document.getElementById('first'));

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'after', [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ]);

// *** unit/core.js ***
//   907: 	equals( expected, $('#en').text(), "Insert element after" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert element after' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   910: 	expected = "This is a normal link: YahooTry them out:diveintomark";

expected = 'This is a normal link: YahooTry them out:diveintomark';

// *** unit/core.js ***
//   911: 	$('#yahoo').after([document.getElementById('first'), document.getElementById('mark')]);

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'after', [ [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]), $v.cm($v.ro('document'), 'getElementById', [ 'mark' ]) ] ]);

// *** unit/core.js ***
//   912: 	equals( expected, $('#en').text(), "Insert array of elements after" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert array of elements after' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   915: 	expected = "This is a normal link: YahooTry them out:diveintomark";

expected = 'This is a normal link: YahooTry them out:diveintomark';

// *** unit/core.js ***
//   916: 	$('#yahoo').after($("#first, #mark"));

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'after', [ $v.cf($v.ro('$'), [ '#first, #mark' ]) ]);
$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert jQuery after' ]);

// *** unit/core.js ***
//   917: 	equals( expected, $('#en').text(), "Insert jQuery after" );

})) ]);

// *** unit/core.js ***
//   920: test("insertAfter(String|Element|Array&lt;Element&gt;|jQuery)", function() {

$v.cf($v.ro('test'), [ 'insertAfter(String|Element|Array\x26lt;Element\x26gt;|jQuery)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//   922: 	var expected = 'This is a normal link: Yahoobuga';

var expected = 'This is a normal link: Yahoobuga';

// *** unit/core.js ***
//   923: 	$('<b>buga<�b>').insertAfter('#yahoo');

$v.cm($v.cf($v.ro('$'), [ '\x3cb\x3ebuga\x3c/b\x3e' ]), 'insertAfter', [ '#yahoo' ]);

// *** unit/core.js ***
//   924: 	equals( expected, $('#en').text(), 'Insert String after' );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert String after' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   927: 	expected = "This is a normal link: YahooTry them out:";

expected = 'This is a normal link: YahooTry them out:';

// *** unit/core.js ***
//   928: 	$(document.getElementById('first')).insertAfter('#yahoo');

$v.cm($v.cf($v.ro('$'), [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ]), 'insertAfter', [ '#yahoo' ]);

// *** unit/core.js ***
//   929: 	equals( expected, $('#en').text(), "Insert element after" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert element after' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   932: 	expected = "This is a normal link: YahooTry them out:diveintomark";

expected = 'This is a normal link: YahooTry them out:diveintomark';

// *** unit/core.js ***
//   933: 	$([document.getElementById('mark'), document.getElementById('first')]).insertAfter('#yahoo');

$v.cm($v.cf($v.ro('$'), [ [ $v.cm($v.ro('document'), 'getElementById', [ 'mark' ]), $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ] ]), 'insertAfter', [ '#yahoo' ]);

// *** unit/core.js ***
//   934: 	equals( expected, $('#en').text(), "Insert array of elements after" );

$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert array of elements after' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   937: 	expected = "This is a normal link: YahooTry them out:diveintomark";

expected = 'This is a normal link: YahooTry them out:diveintomark';

// *** unit/core.js ***
//   938: 	$("#mark, #first").insertAfter('#yahoo');

$v.cm($v.cf($v.ro('$'), [ '#mark, #first' ]), 'insertAfter', [ '#yahoo' ]);
$v.cf($v.ro('equals'), [ expected, $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Insert jQuery after' ]);

// *** unit/core.js ***
//   939: 	equals( expected, $('#en').text(), "Insert jQuery after" );

})) ]);

// *** unit/core.js ***
//   942: test("replaceWith(String|Element|Array&lt;Element&gt;|jQuery)", function() {

$v.cf($v.ro('test'), [ 'replaceWith(String|Element|Array\x26lt;Element\x26gt;|jQuery)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 10 ]);

// *** unit/core.js ***
//   944: 	$('#yahoo').replaceWith('<b id="replace">buga<�b>');

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'replaceWith', [ '\x3cb id=\"replace\"\x3ebuga\x3c/b\x3e' ]);

// *** unit/core.js ***
//   945: 	ok( $("#replace")[0], 'Replace element with string' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#replace' ]), 0), 'Replace element with string' ]);

// *** unit/core.js ***
//   946: 	ok( !$("#yahoo")[0], 'Verify that original element is gone, after string' );

$v.cf($v.ro('ok'), [ !$v.r($v.cf($v.ro('$'), [ '#yahoo' ]), 0), 'Verify that original element is gone, after string' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   949: 	$('#yahoo').replaceWith(document.getElementById('first'));

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'replaceWith', [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ]);

// *** unit/core.js ***
//   950: 	ok( $("#first")[0], 'Replace element with element' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#first' ]), 0), 'Replace element with element' ]);

// *** unit/core.js ***
//   951: 	ok( !$("#yahoo")[0], 'Verify that original element is gone, after element' );

$v.cf($v.ro('ok'), [ !$v.r($v.cf($v.ro('$'), [ '#yahoo' ]), 0), 'Verify that original element is gone, after element' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   954: 	$('#yahoo').replaceWith([document.getElementById('first'), document.getElementById('mark')]);

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'replaceWith', [ [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]), $v.cm($v.ro('document'), 'getElementById', [ 'mark' ]) ] ]);

// *** unit/core.js ***
//   955: 	ok( $("#first")[0], 'Replace element with array of elements' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#first' ]), 0), 'Replace element with array of elements' ]);

// *** unit/core.js ***
//   956: 	ok( $("#mark")[0], 'Replace element with array of elements' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#mark' ]), 0), 'Replace element with array of elements' ]);

// *** unit/core.js ***
//   957: 	ok( !$("#yahoo")[0], 'Verify that original element is gone, after array of elements' );

$v.cf($v.ro('ok'), [ !$v.r($v.cf($v.ro('$'), [ '#yahoo' ]), 0), 'Verify that original element is gone, after array of elements' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   960: 	$('#yahoo').replaceWith($("#first, #mark"));

$v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'replaceWith', [ $v.cf($v.ro('$'), [ '#first, #mark' ]) ]);

// *** unit/core.js ***
//   961: 	ok( $("#first")[0], 'Replace element with set of elements' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#first' ]), 0), 'Replace element with set of elements' ]);

// *** unit/core.js ***
//   962: 	ok( $("#mark")[0], 'Replace element with set of elements' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#mark' ]), 0), 'Replace element with set of elements' ]);
$v.cf($v.ro('ok'), [ !$v.r($v.cf($v.ro('$'), [ '#yahoo' ]), 0), 'Verify that original element is gone, after set of elements' ]);

// *** unit/core.js ***
//   963: 	ok( !$("#yahoo")[0], 'Verify that original element is gone, after set of elements' );

})) ]);

// *** unit/core.js ***
//   966: test("replaceAll(String|Element|Array&lt;Element&gt;|jQuery)", function() {

$v.cf($v.ro('test'), [ 'replaceAll(String|Element|Array\x26lt;Element\x26gt;|jQuery)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 10 ]);

// *** unit/core.js ***
//   968: 	$('<b id="replace">buga<�b>').replaceAll("#yahoo");

$v.cm($v.cf($v.ro('$'), [ '\x3cb id=\"replace\"\x3ebuga\x3c/b\x3e' ]), 'replaceAll', [ '#yahoo' ]);

// *** unit/core.js ***
//   969: 	ok( $("#replace")[0], 'Replace element with string' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#replace' ]), 0), 'Replace element with string' ]);

// *** unit/core.js ***
//   970: 	ok( !$("#yahoo")[0], 'Verify that original element is gone, after string' );

$v.cf($v.ro('ok'), [ !$v.r($v.cf($v.ro('$'), [ '#yahoo' ]), 0), 'Verify that original element is gone, after string' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   973: 	$(document.getElementById('first')).replaceAll("#yahoo");

$v.cm($v.cf($v.ro('$'), [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]) ]), 'replaceAll', [ '#yahoo' ]);

// *** unit/core.js ***
//   974: 	ok( $("#first")[0], 'Replace element with element' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#first' ]), 0), 'Replace element with element' ]);

// *** unit/core.js ***
//   975: 	ok( !$("#yahoo")[0], 'Verify that original element is gone, after element' );

$v.cf($v.ro('ok'), [ !$v.r($v.cf($v.ro('$'), [ '#yahoo' ]), 0), 'Verify that original element is gone, after element' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   978: 	$([document.getElementById('first'), document.getElementById('mark')]).replaceAll("#yahoo");

$v.cm($v.cf($v.ro('$'), [ [ $v.cm($v.ro('document'), 'getElementById', [ 'first' ]), $v.cm($v.ro('document'), 'getElementById', [ 'mark' ]) ] ]), 'replaceAll', [ '#yahoo' ]);

// *** unit/core.js ***
//   979: 	ok( $("#first")[0], 'Replace element with array of elements' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#first' ]), 0), 'Replace element with array of elements' ]);

// *** unit/core.js ***
//   980: 	ok( $("#mark")[0], 'Replace element with array of elements' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#mark' ]), 0), 'Replace element with array of elements' ]);

// *** unit/core.js ***
//   981: 	ok( !$("#yahoo")[0], 'Verify that original element is gone, after array of elements' );

$v.cf($v.ro('ok'), [ !$v.r($v.cf($v.ro('$'), [ '#yahoo' ]), 0), 'Verify that original element is gone, after array of elements' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//   984: 	$("#first, #mark").replaceAll("#yahoo");

$v.cm($v.cf($v.ro('$'), [ '#first, #mark' ]), 'replaceAll', [ '#yahoo' ]);

// *** unit/core.js ***
//   985: 	ok( $("#first")[0], 'Replace element with set of elements' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#first' ]), 0), 'Replace element with set of elements' ]);

// *** unit/core.js ***
//   986: 	ok( $("#mark")[0], 'Replace element with set of elements' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#mark' ]), 0), 'Replace element with set of elements' ]);
$v.cf($v.ro('ok'), [ !$v.r($v.cf($v.ro('$'), [ '#yahoo' ]), 0), 'Verify that original element is gone, after set of elements' ]);

// *** unit/core.js ***
//   987: 	ok( !$("#yahoo")[0], 'Verify that original element is gone, after set of elements' );

})) ]);
$v.cf($v.ro('test'), [ 'end()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);

// *** unit/core.js ***
//   992: 	equals( 'Yahoo', $('#yahoo').parent().end().text(), 'Check for end' );

$v.cf($v.ro('equals'), [ 'Yahoo', $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'parent', [ ]), 'end', [ ]), 'text', [ ]), 'Check for end' ]);

// *** unit/core.js ***
//   993: 	ok( $('#yahoo').end(), 'Check for end with nothing to end' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'end', [ ]), 'Check for end with nothing to end' ]);

// *** unit/core.js ***
//   995: 	var x = $('#yahoo');

var x = $v.cf($v.ro('$'), [ '#yahoo' ]);
$v.cm(x, 'parent', [ ]);

// *** unit/core.js ***
//   997: 	equals( 'Yahoo', $('#yahoo').text(), 'Check for non-destructive behaviour' );

$v.cf($v.ro('equals'), [ 'Yahoo', $v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'text', [ ]), 'Check for non-destructive behaviour' ]);
})) ]);

// *** unit/core.js ***
//  1000: test("find(String)", function() {

$v.cf($v.ro('test'), [ 'find(String)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);

// *** unit/core.js ***
//  1002: 	equals( 'Yahoo', $('#foo').find('.blogTest').text(), 'Check for find' );

$v.cf($v.ro('equals'), [ 'Yahoo', $v.cm($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'find', [ '.blogTest' ]), 'text', [ ]), 'Check for find' ]);

// *** unit/core.js ***
//  1005: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);

// *** unit/core.js ***
//  1006: 	equals( j.find("div").length, 0, "Check node,textnode,comment to find zero divs" );

$v.cf($v.ro('equals'), [ $v.r($v.cm(j, 'find', [ 'div' ]), 'length'), 0, 'Check node,textnode,comment to find zero divs' ]);
})) ]);
$v.cf($v.ro('test'), [ 'clone()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 20 ]);

// *** unit/core.js ***
//  1011: 	equals( 'This is a normal link: Yahoo', $('#en').text(), 'Assert text for #en' );

$v.cf($v.ro('equals'), [ 'This is a normal link: Yahoo', $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Assert text for #en' ]);

// *** unit/core.js ***
//  1012: 	var clone = $('#yahoo').clone();

var clone = $v.cm($v.cf($v.ro('$'), [ '#yahoo' ]), 'clone', [ ]);

// *** unit/core.js ***
//  1013: 	equals( 'Try them out:Yahoo', $('#first').append(clone).text(), 'Check for clone' );

$v.cf($v.ro('equals'), [ 'Try them out:Yahoo', $v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'append', [ clone ]), 'text', [ ]), 'Check for clone' ]);

// *** unit/core.js ***
//  1014: 	equals( 'This is a normal link: Yahoo', $('#en').text(), 'Reassert text for #en' );

$v.cf($v.ro('equals'), [ 'This is a normal link: Yahoo', $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'text', [ ]), 'Reassert text for #en' ]);

// *** unit/core.js ***
//  1016: 	var cloneTags = [
//  1017: 		"<table/>", "<tr/>", "<td/>", "<div/>",
//  1018: 		"<button/>", "<ul/>", "<ol/>", "<li/>",
//  1019: 		"<input type='checkbox' />", "<select/>", "<option/>", "<textarea/>",
//  1020: 		"<tbody/>", "<thead/>", "<tfoot/>", "<iframe/>"
//  1021: 	];

var cloneTags = [ '\x3ctable/\x3e', '\x3ctr/\x3e', '\x3ctd/\x3e', '\x3cdiv/\x3e', '\x3cbutton/\x3e', '\x3cul/\x3e', '\x3col/\x3e', '\x3cli/\x3e', '\x3cinput type=\'checkbox\' /\x3e', '\x3cselect/\x3e', '\x3coption/\x3e', '\x3ctextarea/\x3e', '\x3ctbody/\x3e', '\x3cthead/\x3e', '\x3ctfoot/\x3e', '\x3ciframe/\x3e' ];

// *** unit/core.js ***
//  1022: 	for (var i = 0; i < cloneTags.length; i++) {
//  1025: 	}

for (var i = 0; i < $v.r(cloneTags, 'length'); i++) {

// *** unit/core.js ***
//  1023: 		var j = $(cloneTags[i]);

var j = $v.cf($v.ro('$'), [ $v.r(cloneTags, i) ]);

// *** unit/core.js ***
//  1024: 		equals( j[0].tagName, j.clone()[0].tagName, 'Clone a &lt;' + cloneTags[i].substring(1));

$v.cf($v.ro('equals'), [ $v.r($v.r(j, 0), 'tagName'), $v.r($v.r($v.cm(j, 'clone', [ ]), 0), 'tagName'), 'Clone a \x26lt;' + $v.cm($v.r(cloneTags, i), 'substring', [ 1 ]) ]);
}

// *** unit/core.js ***
//  1028: 	var cl = $("#nonnodes").contents().clone();

var cl = $v.cm($v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]), 'clone', [ ]);

// *** unit/core.js ***
//  1029: 	ok( cl.length >= 2, "Check node,textnode,comment clone works (some browsers delete comments on clone)" );

$v.cf($v.ro('ok'), [ $v.r(cl, 'length') >= 2, 'Check node,textnode,comment clone works (some browsers delete comments on clone)' ]);
})) ]);

// *** unit/core.js ***
//  1032: if (!isLocal) {
//  1034: 	expect(2);
//  1035: 	stop();
//  1044: });
//  1045: }

if (!$v.ro('isLocal')) {

// *** unit/core.js ***
//  1033: test("clone() on XML nodes", function() {

$v.cf($v.ro('test'), [ 'clone() on XML nodes', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/core.js ***
//  1036: 	$.get("data/dashboard.xml", function (xml) {

$v.cm($v.ro('$'), 'get', [ 'data/dashboard.xml', $v.dis(___.frozenFunc(function ($dis, xml) {

// *** unit/core.js ***
//  1037: 		var root = $(xml.documentElement).clone();

var root = $v.cm($v.cf($v.ro('$'), [ $v.r(xml, 'documentElement') ]), 'clone', [ ]);

// *** unit/core.js ***
//  1038: 		$("tab:first", xml).text("origval");

$v.cm($v.cf($v.ro('$'), [ 'tab:first', xml ]), 'text', [ 'origval' ]);

// *** unit/core.js ***
//  1039: 		$("tab:first", root).text("cloneval");

$v.cm($v.cf($v.ro('$'), [ 'tab:first', root ]), 'text', [ 'cloneval' ]);

// *** unit/core.js ***
//  1040: 		equals($("tab:first", xml).text(), "origval", "Check original XML node was correctly set");

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'tab:first', xml ]), 'text', [ ]), 'origval', 'Check original XML node was correctly set' ]);

// *** unit/core.js ***
//  1041: 		equals($("tab:first", root).text(), "cloneval", "Check cloned XML node was correctly set");

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'tab:first', root ]), 'text', [ ]), 'cloneval', 'Check cloned XML node was correctly set' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/core.js ***
//  1042: 		start();

})) ]);

// *** unit/core.js ***
//  1043: 	});

})) ]);
}

// *** unit/core.js ***
//  1047: test("is(String)", function() {

$v.cf($v.ro('test'), [ 'is(String)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 26 ]);

// *** unit/core.js ***
//  1049: 	ok( $('#form').is('form'), 'Check for element: A form must be a form' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#form' ]), 'is', [ 'form' ]), 'Check for element: A form must be a form' ]);

// *** unit/core.js ***
//  1050: 	ok( !$('#form').is('div'), 'Check for element: A form is not a div' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#form' ]), 'is', [ 'div' ]), 'Check for element: A form is not a div' ]);

// *** unit/core.js ***
//  1051: 	ok( $('#mark').is('.blog'), 'Check for class: Expected class "blog"' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#mark' ]), 'is', [ '.blog' ]), 'Check for class: Expected class \"blog\"' ]);

// *** unit/core.js ***
//  1052: 	ok( !$('#mark').is('.link'), 'Check for class: Did not expect class "link"' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#mark' ]), 'is', [ '.link' ]), 'Check for class: Did not expect class \"link\"' ]);

// *** unit/core.js ***
//  1053: 	ok( $('#simon').is('.blog.link'), 'Check for multiple classes: Expected classes "blog" and "link"' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#simon' ]), 'is', [ '.blog.link' ]), 'Check for multiple classes: Expected classes \"blog\" and \"link\"' ]);

// *** unit/core.js ***
//  1054: 	ok( !$('#simon').is('.blogTest'), 'Check for multiple classes: Expected classes "blog" and "link", but not "blogTest"' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#simon' ]), 'is', [ '.blogTest' ]), 'Check for multiple classes: Expected classes \"blog\" and \"link\", but not \"blogTest\"' ]);

// *** unit/core.js ***
//  1055: 	ok( $('#en').is('[lang="en"]'), 'Check for attribute: Expected attribute lang to be "en"' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'is', [ '[lang=\"en\"]' ]), 'Check for attribute: Expected attribute lang to be \"en\"' ]);

// *** unit/core.js ***
//  1056: 	ok( !$('#en').is('[lang="de"]'), 'Check for attribute: Expected attribute lang to be "en", not "de"' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#en' ]), 'is', [ '[lang=\"de\"]' ]), 'Check for attribute: Expected attribute lang to be \"en\", not \"de\"' ]);

// *** unit/core.js ***
//  1057: 	ok( $('#text1').is('[type="text"]'), 'Check for attribute: Expected attribute type to be "text"' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'is', [ '[type=\"text\"]' ]), 'Check for attribute: Expected attribute type to be \"text\"' ]);

// *** unit/core.js ***
//  1058: 	ok( !$('#text1').is('[type="radio"]'), 'Check for attribute: Expected attribute type to be "text", not "radio"' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'is', [ '[type=\"radio\"]' ]), 'Check for attribute: Expected attribute type to be \"text\", not \"radio\"' ]);

// *** unit/core.js ***
//  1059: 	ok( $('#text2').is(':disabled'), 'Check for pseudoclass: Expected to be disabled' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#text2' ]), 'is', [ ':disabled' ]), 'Check for pseudoclass: Expected to be disabled' ]);

// *** unit/core.js ***
//  1060: 	ok( !$('#text1').is(':disabled'), 'Check for pseudoclass: Expected not disabled' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'is', [ ':disabled' ]), 'Check for pseudoclass: Expected not disabled' ]);

// *** unit/core.js ***
//  1061: 	ok( $('#radio2').is(':checked'), 'Check for pseudoclass: Expected to be checked' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#radio2' ]), 'is', [ ':checked' ]), 'Check for pseudoclass: Expected to be checked' ]);

// *** unit/core.js ***
//  1062: 	ok( !$('#radio1').is(':checked'), 'Check for pseudoclass: Expected not checked' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#radio1' ]), 'is', [ ':checked' ]), 'Check for pseudoclass: Expected not checked' ]);

// *** unit/core.js ***
//  1063: 	ok( $('#foo').is(':has(p)'), 'Check for child: Expected a child "p" element' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ ':has(p)' ]), 'Check for child: Expected a child \"p\" element' ]);

// *** unit/core.js ***
//  1064: 	ok( !$('#foo').is(':has(ul)'), 'Check for child: Did not expect "ul" element' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ ':has(ul)' ]), 'Check for child: Did not expect \"ul\" element' ]);

// *** unit/core.js ***
//  1065: 	ok( $('#foo').is(':has(p):has(a):has(code)'), 'Check for childs: Expected "p", "a" and "code" child elements' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ ':has(p):has(a):has(code)' ]), 'Check for childs: Expected \"p\", \"a\" and \"code\" child elements' ]);

// *** unit/core.js ***
//  1066: 	ok( !$('#foo').is(':has(p):has(a):has(code):has(ol)'), 'Check for childs: Expected "p", "a" and "code" child elements, but no "ol"' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ ':has(p):has(a):has(code):has(ol)' ]), 'Check for childs: Expected \"p\", \"a\" and \"code\" child elements, but no \"ol\"' ]);

// *** unit/core.js ***
//  1067: 	ok( !$('#foo').is(0), 'Expected false for an invalid expression - 0' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ 0 ]), 'Expected false for an invalid expression - 0' ]);

// *** unit/core.js ***
//  1068: 	ok( !$('#foo').is(null), 'Expected false for an invalid expression - null' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ null ]), 'Expected false for an invalid expression - null' ]);

// *** unit/core.js ***
//  1069: 	ok( !$('#foo').is(''), 'Expected false for an invalid expression - ""' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ '' ]), 'Expected false for an invalid expression - \"\"' ]);

// *** unit/core.js ***
//  1070: 	ok( !$('#foo').is(undefined), 'Expected false for an invalid expression - undefined' );

$v.cf($v.ro('ok'), [ !$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'is', [ $v.ro('undefined') ]), 'Expected false for an invalid expression - undefined' ]);

// *** unit/core.js ***
//  1073: 	ok( $('#en').is('[lang="en"],[lang="de"]'), 'Comma-seperated; Check for lang attribute: Expect en or de' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'is', [ '[lang=\"en\"],[lang=\"de\"]' ]), 'Comma-seperated; Check for lang attribute: Expect en or de' ]);

// *** unit/core.js ***
//  1074: 	ok( $('#en').is('[lang="de"],[lang="en"]'), 'Comma-seperated; Check for lang attribute: Expect en or de' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'is', [ '[lang=\"de\"],[lang=\"en\"]' ]), 'Comma-seperated; Check for lang attribute: Expect en or de' ]);

// *** unit/core.js ***
//  1075: 	ok( $('#en').is('[lang="en"] , [lang="de"]'), 'Comma-seperated; Check for lang attribute: Expect en or de' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'is', [ '[lang=\"en\"] , [lang=\"de\"]' ]), 'Comma-seperated; Check for lang attribute: Expect en or de' ]);

// *** unit/core.js ***
//  1076: 	ok( $('#en').is('[lang="de"] , [lang="en"]'), 'Comma-seperated; Check for lang attribute: Expect en or de' );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '#en' ]), 'is', [ '[lang=\"de\"] , [lang=\"en\"]' ]), 'Comma-seperated; Check for lang attribute: Expect en or de' ]);
})) ]);

// *** unit/core.js ***
//  1079: test("$.extend(Object, Object)", function() {

$v.cf($v.ro('test'), [ '$.extend(Object, Object)', $v.dis(___.frozenFunc(function ($dis) {
function func$_caller($dis) {
}
___.func(func$_caller, 'func$_caller');
var func;
;

// *** unit/core.js ***
//  1133: 	function func() {}

func = $v.dis(___.primFreeze(func$_caller), 'func');
$v.cf($v.ro('expect'), [ 20 ]);

// *** unit/core.js ***
//  1082: 	var settings = { xnumber1: 5, xnumber2: 7, xstring1: "peter", xstring2: "pan" },
//  1083: 		options = { xnumber2: 1, xstring2: "x", xxx: "newstring" },
//  1084: 		optionsCopy = { xnumber2: 1, xstring2: "x", xxx: "newstring" },
//  1085: 		merged = { xnumber1: 5, xnumber2: 1, xstring1: "peter", xstring2: "x", xxx: "newstring" },
//  1086: 		deep1 = { foo: { bar: true } },
//  1087: 		deep1copy = { foo: { bar: true } },
//  1088: 		deep2 = { foo: { baz: true }, foo2: document },
//  1089: 		deep2copy = { foo: { baz: true }, foo2: document },
//  1090: 		deepmerged = { foo: { bar: true, baz: true }, foo2: document };

var settings = ___.initializeMap([ 'xnumber1', 5, 'xnumber2', 7, 'xstring1', 'peter', 'xstring2', 'pan' ]), options = ___.initializeMap([ 'xnumber2', 1, 'xstring2', 'x', 'xxx', 'newstring' ]), optionsCopy = ___.initializeMap([ 'xnumber2', 1, 'xstring2', 'x', 'xxx', 'newstring' ]), merged = ___.initializeMap([ 'xnumber1', 5, 'xnumber2', 1, 'xstring1', 'peter', 'xstring2', 'x', 'xxx', 'newstring' ]), deep1 = ___.initializeMap([ 'foo', ___.initializeMap([ 'bar', true ]) ]), deep1copy = ___.initializeMap([ 'foo', ___.initializeMap([ 'bar', true ]) ]), deep2 = ___.initializeMap([ 'foo', ___.initializeMap([ 'baz', true ]), 'foo2', $v.ro('document') ]), deep2copy = ___.initializeMap([ 'foo', ___.initializeMap([ 'baz', true ]), 'foo2', $v.ro('document') ]), deepmerged = ___.initializeMap([ 'foo', ___.initializeMap([ 'bar', true, 'baz', true ]), 'foo2', $v.ro('document') ]);

// *** unit/core.js ***
//  1092: 	jQuery.extend(settings, options);

$v.cm($v.ro('jQuery'), 'extend', [ settings, options ]);

// *** unit/core.js ***
//  1093: 	isObj( settings, merged, "Check if extended: settings must be extended" );

$v.cf($v.ro('isObj'), [ settings, merged, 'Check if extended: settings must be extended' ]);

// *** unit/core.js ***
//  1094: 	isObj( options, optionsCopy, "Check if not modified: options must not be modified" );

$v.cf($v.ro('isObj'), [ options, optionsCopy, 'Check if not modified: options must not be modified' ]);

// *** unit/core.js ***
//  1096: 	jQuery.extend(settings, null, options);

$v.cm($v.ro('jQuery'), 'extend', [ settings, null, options ]);

// *** unit/core.js ***
//  1097: 	isObj( settings, merged, "Check if extended: settings must be extended" );

$v.cf($v.ro('isObj'), [ settings, merged, 'Check if extended: settings must be extended' ]);

// *** unit/core.js ***
//  1098: 	isObj( options, optionsCopy, "Check if not modified: options must not be modified" );

$v.cf($v.ro('isObj'), [ options, optionsCopy, 'Check if not modified: options must not be modified' ]);

// *** unit/core.js ***
//  1100: 	jQuery.extend(true, deep1, deep2);

$v.cm($v.ro('jQuery'), 'extend', [ true, deep1, deep2 ]);

// *** unit/core.js ***
//  1101: 	isObj( deep1.foo, deepmerged.foo, "Check if foo: settings must be extended" );

$v.cf($v.ro('isObj'), [ $v.r(deep1, 'foo'), $v.r(deepmerged, 'foo'), 'Check if foo: settings must be extended' ]);

// *** unit/core.js ***
//  1102: 	isObj( deep2.foo, deep2copy.foo, "Check if not deep2: options must not be modified" );

$v.cf($v.ro('isObj'), [ $v.r(deep2, 'foo'), $v.r(deep2copy, 'foo'), 'Check if not deep2: options must not be modified' ]);

// *** unit/core.js ***
//  1103: 	equals( deep1.foo2, document, "Make sure that a deep clone was not attempted on the document" );

$v.cf($v.ro('equals'), [ $v.r(deep1, 'foo2'), $v.ro('document'), 'Make sure that a deep clone was not attempted on the document' ]);

// *** unit/core.js ***
//  1105: 	var nullUndef;

var nullUndef;

// *** unit/core.js ***
//  1106: 	nullUndef = jQuery.extend({}, options, { xnumber2: null });

nullUndef = $v.cm($v.ro('jQuery'), 'extend', [ ___.initializeMap([ ]), options, ___.initializeMap([ 'xnumber2', null ]) ]);

// *** unit/core.js ***
//  1107: 	ok( nullUndef.xnumber2 === null, "Check to make sure null values are copied");

$v.cf($v.ro('ok'), [ $v.r(nullUndef, 'xnumber2') === null, 'Check to make sure null values are copied' ]);

// *** unit/core.js ***
//  1109: 	nullUndef = jQuery.extend({}, options, { xnumber2: undefined });

nullUndef = $v.cm($v.ro('jQuery'), 'extend', [ ___.initializeMap([ ]), options, ___.initializeMap([ 'xnumber2', $v.ro('undefined') ]) ]);

// *** unit/core.js ***
//  1110: 	ok( nullUndef.xnumber2 === options.xnumber2, "Check to make sure undefined values are not copied");

$v.cf($v.ro('ok'), [ $v.r(nullUndef, 'xnumber2') === $v.r(options, 'xnumber2'), 'Check to make sure undefined values are not copied' ]);

// *** unit/core.js ***
//  1112: 	nullUndef = jQuery.extend({}, options, { xnumber0: null });

nullUndef = $v.cm($v.ro('jQuery'), 'extend', [ ___.initializeMap([ ]), options, ___.initializeMap([ 'xnumber0', null ]) ]);

// *** unit/core.js ***
//  1113: 	ok( nullUndef.xnumber0 === null, "Check to make sure null values are inserted");

$v.cf($v.ro('ok'), [ $v.r(nullUndef, 'xnumber0') === null, 'Check to make sure null values are inserted' ]);

// *** unit/core.js ***
//  1115: 	var target = {};

var target = ___.initializeMap([ ]);

// *** unit/core.js ***
//  1116: 	var recursive = { foo:target, bar:5 };

var recursive = ___.initializeMap([ 'foo', target, 'bar', 5 ]);

// *** unit/core.js ***
//  1117: 	jQuery.extend(true, target, recursive);

$v.cm($v.ro('jQuery'), 'extend', [ true, target, recursive ]);

// *** unit/core.js ***
//  1118: 	isObj( target, { bar:5 }, "Check to make sure a recursive obj doesn't go never-ending loop by not copying it over" );

$v.cf($v.ro('isObj'), [ target, ___.initializeMap([ 'bar', 5 ]), 'Check to make sure a recursive obj doesn\'t go never-ending loop by not copying it over' ]);

// *** unit/core.js ***
//  1120: 	var ret = jQuery.extend(true, { foo: [] }, { foo: [0] } ); // 1907

var ret = $v.cm($v.ro('jQuery'), 'extend', [ true, ___.initializeMap([ 'foo', [ ] ]), ___.initializeMap([ 'foo', [ 0 ] ]) ]);

// *** unit/core.js ***
//  1121: 	equals( ret.foo.length, 1, "Check to make sure a value with coersion 'false' copies over when necessary to fix #1907" );

$v.cf($v.ro('equals'), [ $v.r($v.r(ret, 'foo'), 'length'), 1, 'Check to make sure a value with coersion \'false\' copies over when necessary to fix #1907' ]);

// *** unit/core.js ***
//  1123: 	var ret = jQuery.extend(true, { foo: "1,2,3" }, { foo: [1, 2, 3] } );

var ret = $v.cm($v.ro('jQuery'), 'extend', [ true, ___.initializeMap([ 'foo', '1,2,3' ]), ___.initializeMap([ 'foo', [ 1, 2, 3 ] ]) ]);

// *** unit/core.js ***
//  1124: 	ok( typeof ret.foo != "string", "Check to make sure values equal with coersion (but not actually equal) overwrite correctly" );

$v.cf($v.ro('ok'), [ $v.typeOf($v.r(ret, 'foo')) != 'string', 'Check to make sure values equal with coersion (but not actually equal) overwrite correctly' ]);

// *** unit/core.js ***
//  1126: 	var ret = jQuery.extend(true, { foo:"bar" }, { foo:null } );

var ret = $v.cm($v.ro('jQuery'), 'extend', [ true, ___.initializeMap([ 'foo', 'bar' ]), ___.initializeMap([ 'foo', null ]) ]);

// *** unit/core.js ***
//  1127: 	ok( typeof ret.foo !== 'undefined', "Make sure a null value doesn't crash with deep extend, for #1908" );

$v.cf($v.ro('ok'), [ $v.typeOf($v.r(ret, 'foo')) !== 'undefined', 'Make sure a null value doesn\'t crash with deep extend, for #1908' ]);

// *** unit/core.js ***
//  1129: 	var obj = { foo:null };

var obj = ___.initializeMap([ 'foo', null ]);

// *** unit/core.js ***
//  1130: 	jQuery.extend(true, obj, { foo:"notnull" } );

$v.cm($v.ro('jQuery'), 'extend', [ true, obj, ___.initializeMap([ 'foo', 'notnull' ]) ]);

// *** unit/core.js ***
//  1131: 	equals( obj.foo, "notnull", "Make sure a null value can be overwritten" );

$v.cf($v.ro('equals'), [ $v.r(obj, 'foo'), 'notnull', 'Make sure a null value can be overwritten' ]);
;

// *** unit/core.js ***
//  1134: 	jQuery.extend(func, { key: "value" } );

$v.cm($v.ro('jQuery'), 'extend', [ func, ___.initializeMap([ 'key', 'value' ]) ]);

// *** unit/core.js ***
//  1135: 	equals( func.key, "value", "Verify a function can be extended" );

$v.cf($v.ro('equals'), [ $v.r(func, 'key'), 'value', 'Verify a function can be extended' ]);

// *** unit/core.js ***
//  1137: 	var defaults = { xnumber1: 5, xnumber2: 7, xstring1: "peter", xstring2: "pan" },
//  1138: 		defaultsCopy = { xnumber1: 5, xnumber2: 7, xstring1: "peter", xstring2: "pan" },
//  1139: 		options1 = { xnumber2: 1, xstring2: "x" },
//  1140: 		options1Copy = { xnumber2: 1, xstring2: "x" },
//  1141: 		options2 = { xstring2: "xx", xxx: "newstringx" },
//  1142: 		options2Copy = { xstring2: "xx", xxx: "newstringx" },
//  1143: 		merged2 = { xnumber1: 5, xnumber2: 1, xstring1: "peter", xstring2: "xx", xxx: "newstringx" };

var defaults = ___.initializeMap([ 'xnumber1', 5, 'xnumber2', 7, 'xstring1', 'peter', 'xstring2', 'pan' ]), defaultsCopy = ___.initializeMap([ 'xnumber1', 5, 'xnumber2', 7, 'xstring1', 'peter', 'xstring2', 'pan' ]), options1 = ___.initializeMap([ 'xnumber2', 1, 'xstring2', 'x' ]), options1Copy = ___.initializeMap([ 'xnumber2', 1, 'xstring2', 'x' ]), options2 = ___.initializeMap([ 'xstring2', 'xx', 'xxx', 'newstringx' ]), options2Copy = ___.initializeMap([ 'xstring2', 'xx', 'xxx', 'newstringx' ]), merged2 = ___.initializeMap([ 'xnumber1', 5, 'xnumber2', 1, 'xstring1', 'peter', 'xstring2', 'xx', 'xxx', 'newstringx' ]);

// *** unit/core.js ***
//  1145: 	var settings = jQuery.extend({}, defaults, options1, options2);

var settings = $v.cm($v.ro('jQuery'), 'extend', [ ___.initializeMap([ ]), defaults, options1, options2 ]);

// *** unit/core.js ***
//  1146: 	isObj( settings, merged2, "Check if extended: settings must be extended" );

$v.cf($v.ro('isObj'), [ settings, merged2, 'Check if extended: settings must be extended' ]);

// *** unit/core.js ***
//  1147: 	isObj( defaults, defaultsCopy, "Check if not modified: options1 must not be modified" );

$v.cf($v.ro('isObj'), [ defaults, defaultsCopy, 'Check if not modified: options1 must not be modified' ]);

// *** unit/core.js ***
//  1148: 	isObj( options1, options1Copy, "Check if not modified: options1 must not be modified" );

$v.cf($v.ro('isObj'), [ options1, options1Copy, 'Check if not modified: options1 must not be modified' ]);
$v.cf($v.ro('isObj'), [ options2, options2Copy, 'Check if not modified: options2 must not be modified' ]);

// *** unit/core.js ***
//  1149: 	isObj( options2, options2Copy, "Check if not modified: options2 must not be modified" );

})) ]);
$v.cf($v.ro('test'), [ 'val()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//  1154: 	equals( $("#text1").val(), "Test", "Check for value of input element" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'val', [ ]), 'Test', 'Check for value of input element' ]);

// *** unit/core.js ***
//  1155: 	equals( !$("#text1").val(), "", "Check for value of input element" );

$v.cf($v.ro('equals'), [ !$v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'val', [ ]), '', 'Check for value of input element' ]);

// *** unit/core.js ***
//  1157: 	equals( $("#first").val(), "", "Check a paragraph element to see if it has a value" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#first' ]), 'val', [ ]), '', 'Check a paragraph element to see if it has a value' ]);

// *** unit/core.js ***
//  1158: 	ok( $([]).val() === undefined, "Check an empty jQuery object will return undefined from val" );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ [ ] ]), 'val', [ ]) === $v.ro('undefined'), 'Check an empty jQuery object will return undefined from val' ]);
})) ]);

// *** unit/core.js ***
//  1161: test("val(String)", function() {

$v.cf($v.ro('test'), [ 'val(String)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//  1163: 	document.getElementById('text1').value = "bla";

$v.s($v.cm($v.ro('document'), 'getElementById', [ 'text1' ]), 'value', 'bla');

// *** unit/core.js ***
//  1164: 	equals( $("#text1").val(), "bla", "Check for modified value of input element" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'val', [ ]), 'bla', 'Check for modified value of input element' ]);

// *** unit/core.js ***
//  1165: 	$("#text1").val('test');

$v.cm($v.cf($v.ro('$'), [ '#text1' ]), 'val', [ 'test' ]);

// *** unit/core.js ***
//  1166: 	ok ( document.getElementById('text1').value == "test", "Check for modified (via val(String)) value of input element" );

$v.cf($v.ro('ok'), [ $v.r($v.cm($v.ro('document'), 'getElementById', [ 'text1' ]), 'value') == 'test', 'Check for modified (via val(String)) value of input element' ]);

// *** unit/core.js ***
//  1168: 	$("#select1").val("3");

$v.cm($v.cf($v.ro('$'), [ '#select1' ]), 'val', [ '3' ]);

// *** unit/core.js ***
//  1169: 	equals( $("#select1").val(), "3", "Check for modified (via val(String)) value of select element" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#select1' ]), 'val', [ ]), '3', 'Check for modified (via val(String)) value of select element' ]);

// *** unit/core.js ***
//  1172: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);
$v.cm(j, 'val', [ 'asdf' ]);

// *** unit/core.js ***
//  1174: 	equals( j.val(), "asdf", "Check node,textnode,comment with val()" );

$v.cf($v.ro('equals'), [ $v.cm(j, 'val', [ ]), 'asdf', 'Check node,textnode,comment with val()' ]);
$v.cm(j, 'removeAttr', [ 'value' ]);

// *** unit/core.js ***
//  1175: 	j.removeAttr("value");

})) ]);
$v.so('scriptorder', 0);

// *** unit/core.js ***
//  1180: test("html(String)", function() {

$v.cf($v.ro('test'), [ 'html(String)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 11 ]);

// *** unit/core.js ***
//  1182: 	var div = $("#main > div");

var div = $v.cf($v.ro('$'), [ '#main \x3e div' ]);

// *** unit/core.js ***
//  1183: 	div.html("<b>test<�b>");

$v.cm(div, 'html', [ '\x3cb\x3etest\x3c/b\x3e' ]);

// *** unit/core.js ***
//  1184: 	var pass = true;

var pass = true;

// *** unit/core.js ***
//  1185: 	for ( var i = 0; i < div.size(); i++ ) {
//  1187: 	}

for (var i = 0; i < $v.cm(div, 'size', [ ]); i++) {

// *** unit/core.js ***
//  1186: 		if ( div.get(i).childNodes.length != 1 ) pass = false;

if ($v.r($v.r($v.cm(div, 'get', [ i ]), 'childNodes'), 'length') != 1) pass = false;
}

// *** unit/core.js ***
//  1188: 	ok( pass, "Set HTML" );

$v.cf($v.ro('ok'), [ pass, 'Set HTML' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//  1192: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);

// *** unit/core.js ***
//  1193: 	j.html("<b>bold<�b>");

$v.cm(j, 'html', [ '\x3cb\x3ebold\x3c/b\x3e' ]);

// *** unit/core.js ***
//  1196: 	j.find('b').removeData();

$v.cm($v.cm(j, 'find', [ 'b' ]), 'removeData', [ ]);

// *** unit/core.js ***
//  1197: 	equals( j.html().toLowerCase(), "<b>bold<�b>", "Check node,textnode,comment with html()" );

$v.cf($v.ro('equals'), [ $v.cm($v.cm(j, 'html', [ ]), 'toLowerCase', [ ]), '\x3cb\x3ebold\x3c/b\x3e', 'Check node,textnode,comment with html()' ]);

// *** unit/core.js ***
//  1199: 	$("#main").html("<select/>");

$v.cm($v.cf($v.ro('$'), [ '#main' ]), 'html', [ '\x3cselect/\x3e' ]);

// *** unit/core.js ***
//  1200: 	$("#main select").html("<option>O1<�option><option selected='selected'>O2<�option><option>O3<�option>");

$v.cm($v.cf($v.ro('$'), [ '#main select' ]), 'html', [ '\x3coption\x3eO1\x3c/option\x3e\x3coption selected=\'selected\'\x3eO2\x3c/option\x3e\x3coption\x3eO3\x3c/option\x3e' ]);

// *** unit/core.js ***
//  1201: 	equals( $("#main select").val(), "O2", "Selected option correct" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#main select' ]), 'val', [ ]), 'O2', 'Selected option correct' ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/core.js ***
//  1205: 	$("#main").html('<script type="text/javascript">ok( true, "$().html().evalScripts() Evals Scripts Twice in Firefox, see #975" );<�script>');

$v.cm($v.cf($v.ro('$'), [ '#main' ]), 'html', [ '\x3cscript type=\"text/javascript\"\x3eok( true, \"$().html().evalScripts() Evals Scripts Twice in Firefox, see #975\" );\x3c/script\x3e' ]);

// *** unit/core.js ***
//  1207: 	$("#main").html('foo <form><script type="text/javascript">ok( true, "$().html().evalScripts() Evals Scripts Twice in Firefox, see #975" );<�script><�form>');

$v.cm($v.cf($v.ro('$'), [ '#main' ]), 'html', [ 'foo \x3cform\x3e\x3cscript type=\"text/javascript\"\x3eok( true, \"$().html().evalScripts() Evals Scripts Twice in Firefox, see #975\" );\x3c/script\x3e\x3c/form\x3e' ]);

// *** unit/core.js ***
//  1210: 	$("#main").html("<script>equals(scriptorder++, 0, 'Script is executed in order');equals($('#scriptorder').length, 1,'Execute after html (even though appears before)')<\/script><span id='scriptorder'><script>equals(scriptorder++, 1, 'Script (nested) is executed in order');equals($('#scriptorder').length, 1,'Execute after html')<\/script><�span><script>equals(scriptorder++, 2, 'Script (unnested) is executed in order');equals($('#scriptorder').length, 1,'Execute after html')<\/script>");

$v.cm($v.cf($v.ro('$'), [ '#main' ]), 'html', [ '\x3cscript\x3eequals(scriptorder++, 0, \'Script is executed in order\');equals($(\'#scriptorder\').length, 1,\'Execute after html (even though appears before)\')\x3c/script\x3e\x3cspan id=\'scriptorder\'\x3e\x3cscript\x3eequals(scriptorder++, 1, \'Script (nested) is executed in order\');equals($(\'#scriptorder\').length, 1,\'Execute after html\')\x3c/script\x3e\x3c/span\x3e\x3cscript\x3eequals(scriptorder++, 2, \'Script (unnested) is executed in order\');equals($(\'#scriptorder\').length, 1,\'Execute after html\')\x3c/script\x3e' ]);
$v.cf($v.ro('setTimeout'), [ $v.ro('start'), 100 ]);

// *** unit/core.js ***
//  1212: 	setTimeout( start, 100 );

})) ]);
$v.cf($v.ro('test'), [ 'filter()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/core.js ***
//  1217: 	isSet( $("#form input").filter(":checked").get(), q("radio2", "check1"), "filter(String)" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#form input' ]), 'filter', [ ':checked' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'radio2', 'check1' ]), 'filter(String)' ]);

// *** unit/core.js ***
//  1218: 	isSet( $("p").filter("#ap, #sndp").get(), q("ap", "sndp"), "filter('String, String')" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ 'p' ]), 'filter', [ '#ap, #sndp' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'ap', 'sndp' ]), 'filter(\'String, String\')' ]);

// *** unit/core.js ***
//  1219: 	isSet( $("p").filter("#ap,#sndp").get(), q("ap", "sndp"), "filter('String,String')" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ 'p' ]), 'filter', [ '#ap,#sndp' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'ap', 'sndp' ]), 'filter(\'String,String\')' ]);

// *** unit/core.js ***
//  1220:   isSet( $("p").filter(function() { return !$("a", this).length }).get(), q("sndp", "first"), "filter(Function)" );
$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ 'p' ]), 'filter', [ $v.dis(___.frozenFunc(function ($dis) {
  return !$v.r($v.cf($v.ro('$'), [ 'a', $dis ]), 'length');
})) ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'sndp', 'first' ]), 'filter(Function)' ]);

// *** unit/core.js ***
//  1223: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);

// *** unit/core.js ***
//  1224: 	equals( j.filter("span").length, 1, "Check node,textnode,comment to filter the one span" );

$v.cf($v.ro('equals'), [ $v.r($v.cm(j, 'filter', [ 'span' ]), 'length'), 1, 'Check node,textnode,comment to filter the one span' ]);

// *** unit/core.js ***
//  1225: 	equals( j.filter("[name]").length, 0, "Check node,textnode,comment to filter the one span" );

$v.cf($v.ro('equals'), [ $v.r($v.cm(j, 'filter', [ '[name]' ]), 'length'), 0, 'Check node,textnode,comment to filter the one span' ]);
})) ]);
$v.cf($v.ro('test'), [ 'not()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 8 ]);

// *** unit/core.js ***
//  1230: 	equals( $("#main > p#ap > a").not("#google").length, 2, "not('selector')" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#main \x3e p#ap \x3e a' ]), 'not', [ '#google' ]), 'length'), 2, 'not(\'selector\')' ]);

// *** unit/core.js ***
//  1231: 	equals( $("#main > p#ap > a").not(document.getElementById("google")).length, 2, "not(DOMElement)" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#main \x3e p#ap \x3e a' ]), 'not', [ $v.cm($v.ro('document'), 'getElementById', [ 'google' ]) ]), 'length'), 2, 'not(DOMElement)' ]);

// *** unit/core.js ***
//  1232: 	isSet( $("p").not(".result").get(), q("firstp", "ap", "sndp", "en", "sap", "first"), "not('.class')" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ 'p' ]), 'not', [ '.result' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'firstp', 'ap', 'sndp', 'en', 'sap', 'first' ]), 'not(\'.class\')' ]);

// *** unit/core.js ***
//  1233: 	isSet( $("p").not("#ap, #sndp, .result").get(), q("firstp", "en", "sap", "first"), "not('selector, selector')" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ 'p' ]), 'not', [ '#ap, #sndp, .result' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'firstp', 'en', 'sap', 'first' ]), 'not(\'selector, selector\')' ]);

// *** unit/core.js ***
//  1234: 	isSet( $("p").not($("#ap, #sndp, .result")).get(), q("firstp", "en", "sap", "first"), "not(jQuery)" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ 'p' ]), 'not', [ $v.cf($v.ro('$'), [ '#ap, #sndp, .result' ]) ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'firstp', 'en', 'sap', 'first' ]), 'not(jQuery)' ]);

// *** unit/core.js ***
//  1235: 	equals( $("p").not(document.getElementsByTagName("p")).length, 0, "not(Array-like DOM collection)" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ 'p' ]), 'not', [ $v.cm($v.ro('document'), 'getElementsByTagName', [ 'p' ]) ]), 'length'), 0, 'not(Array-like DOM collection)' ]);

// *** unit/core.js ***
//  1236: 	isSet( $("#form option").not("option.emptyopt:contains('Nothing'),[selected],[value='1']").get(), q("option1c", "option1d", "option2c", "option3d" ), "not('complex selector')");

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#form option' ]), 'not', [ 'option.emptyopt:contains(\'Nothing\'),[selected],[value=\'1\']' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'option1c', 'option1d', 'option2c', 'option3d' ]), 'not(\'complex selector\')' ]);

// *** unit/core.js ***
//  1238: 	var selects = $("#form select");

var selects = $v.cf($v.ro('$'), [ '#form select' ]);

// *** unit/core.js ***
//  1239: 	isSet( selects.not( selects[1] ), q("select1", "select3"), "filter out DOM element");

$v.cf($v.ro('isSet'), [ $v.cm(selects, 'not', [ $v.r(selects, 1) ]), $v.cf($v.ro('q'), [ 'select1', 'select3' ]), 'filter out DOM element' ]);
})) ]);
$v.cf($v.ro('test'), [ 'andSelf()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//  1244: 	isSet( $("#en").siblings().andSelf().get(), q("sndp", "sap","en"), "Check for siblings and self" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#en' ]), 'siblings', [ ]), 'andSelf', [ ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'sndp', 'sap', 'en' ]), 'Check for siblings and self' ]);

// *** unit/core.js ***
//  1245: 	isSet( $("#foo").children().andSelf().get(), q("sndp", "en", "sap", "foo"), "Check for children and self" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'children', [ ]), 'andSelf', [ ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'sndp', 'en', 'sap', 'foo' ]), 'Check for children and self' ]);

// *** unit/core.js ***
//  1246: 	isSet( $("#en, #sndp").parent().andSelf().get(), q("foo","en","sndp"), "Check for parent and self" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#en, #sndp' ]), 'parent', [ ]), 'andSelf', [ ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'foo', 'en', 'sndp' ]), 'Check for parent and self' ]);

// *** unit/core.js ***
//  1247: 	isSet( $("#groups").parents("p, div").andSelf().get(), q("ap", "main", "groups"), "Check for parents and self" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#groups' ]), 'parents', [ 'p, div' ]), 'andSelf', [ ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'ap', 'main', 'groups' ]), 'Check for parents and self' ]);
})) ]);

// *** unit/core.js ***
//  1250: test("siblings([String])", function() {

$v.cf($v.ro('test'), [ 'siblings([String])', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 5 ]);

// *** unit/core.js ***
//  1252: 	isSet( $("#en").siblings().get(), q("sndp", "sap"), "Check for siblings" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#en' ]), 'siblings', [ ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'sndp', 'sap' ]), 'Check for siblings' ]);

// *** unit/core.js ***
//  1253: 	isSet( $("#sndp").siblings(":has(code)").get(), q("sap"), "Check for filtered siblings (has code child element)" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#sndp' ]), 'siblings', [ ':has(code)' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'sap' ]), 'Check for filtered siblings (has code child element)' ]);

// *** unit/core.js ***
//  1254: 	isSet( $("#sndp").siblings(":has(a)").get(), q("en", "sap"), "Check for filtered siblings (has anchor child element)" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#sndp' ]), 'siblings', [ ':has(a)' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'en', 'sap' ]), 'Check for filtered siblings (has anchor child element)' ]);

// *** unit/core.js ***
//  1255: 	isSet( $("#foo").siblings("form, b").get(), q("form", "lengthtest", "testForm", "floatTest"), "Check for multiple filters" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'siblings', [ 'form, b' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'form', 'lengthtest', 'testForm', 'floatTest' ]), 'Check for multiple filters' ]);

// *** unit/core.js ***
//  1256: 	isSet( $("#en, #sndp").siblings().get(), q("sndp", "sap", "en"), "Check for unique results from siblings" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#en, #sndp' ]), 'siblings', [ ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'sndp', 'sap', 'en' ]), 'Check for unique results from siblings' ]);
})) ]);

// *** unit/core.js ***
//  1259: test("children([String])", function() {

$v.cf($v.ro('test'), [ 'children([String])', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);

// *** unit/core.js ***
//  1261: 	isSet( $("#foo").children().get(), q("sndp", "en", "sap"), "Check for children" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'children', [ ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'sndp', 'en', 'sap' ]), 'Check for children' ]);

// *** unit/core.js ***
//  1262: 	isSet( $("#foo").children(":has(code)").get(), q("sndp", "sap"), "Check for filtered children" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'children', [ ':has(code)' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'sndp', 'sap' ]), 'Check for filtered children' ]);

// *** unit/core.js ***
//  1263: 	isSet( $("#foo").children("#en, #sap").get(), q("en", "sap"), "Check for multiple filters" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'children', [ '#en, #sap' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'en', 'sap' ]), 'Check for multiple filters' ]);
})) ]);

// *** unit/core.js ***
//  1266: test("parent([String])", function() {

$v.cf($v.ro('test'), [ 'parent([String])', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 5 ]);

// *** unit/core.js ***
//  1268: 	equals( $("#groups").parent()[0].id, "ap", "Simple parent check" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#groups' ]), 'parent', [ ]), 0), 'id'), 'ap', 'Simple parent check' ]);

// *** unit/core.js ***
//  1269: 	equals( $("#groups").parent("p")[0].id, "ap", "Filtered parent check" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#groups' ]), 'parent', [ 'p' ]), 0), 'id'), 'ap', 'Filtered parent check' ]);

// *** unit/core.js ***
//  1270: 	equals( $("#groups").parent("div").length, 0, "Filtered parent check, no match" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#groups' ]), 'parent', [ 'div' ]), 'length'), 0, 'Filtered parent check, no match' ]);

// *** unit/core.js ***
//  1271: 	equals( $("#groups").parent("div, p")[0].id, "ap", "Check for multiple filters" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#groups' ]), 'parent', [ 'div, p' ]), 0), 'id'), 'ap', 'Check for multiple filters' ]);

// *** unit/core.js ***
//  1272: 	isSet( $("#en, #sndp").parent().get(), q("foo"), "Check for unique results from parent" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#en, #sndp' ]), 'parent', [ ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'foo' ]), 'Check for unique results from parent' ]);
})) ]);

// *** unit/core.js ***
//  1275: test("parents([String])", function() {

$v.cf($v.ro('test'), [ 'parents([String])', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 5 ]);

// *** unit/core.js ***
//  1277: 	equals( $("#groups").parents()[0].id, "ap", "Simple parents check" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#groups' ]), 'parents', [ ]), 0), 'id'), 'ap', 'Simple parents check' ]);

// *** unit/core.js ***
//  1278: 	equals( $("#groups").parents("p")[0].id, "ap", "Filtered parents check" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#groups' ]), 'parents', [ 'p' ]), 0), 'id'), 'ap', 'Filtered parents check' ]);

// *** unit/core.js ***
//  1279: 	equals( $("#groups").parents("div")[0].id, "main", "Filtered parents check2" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#groups' ]), 'parents', [ 'div' ]), 0), 'id'), 'main', 'Filtered parents check2' ]);

// *** unit/core.js ***
//  1280: 	isSet( $("#groups").parents("p, div").get(), q("ap", "main"), "Check for multiple filters" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#groups' ]), 'parents', [ 'p, div' ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'ap', 'main' ]), 'Check for multiple filters' ]);

// *** unit/core.js ***
//  1281: 	isSet( $("#en, #sndp").parents().get(), q("foo", "main", "dl", "body", "html"), "Check for unique results from parents" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#en, #sndp' ]), 'parents', [ ]), 'get', [ ]), $v.cf($v.ro('q'), [ 'foo', 'main', 'dl', 'body', 'html' ]), 'Check for unique results from parents' ]);
})) ]);

// *** unit/core.js ***
//  1284: test("next([String])", function() {

$v.cf($v.ro('test'), [ 'next([String])', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//  1286: 	equals( $("#ap").next()[0].id, "foo", "Simple next check" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'next', [ ]), 0), 'id'), 'foo', 'Simple next check' ]);

// *** unit/core.js ***
//  1287: 	equals( $("#ap").next("div")[0].id, "foo", "Filtered next check" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'next', [ 'div' ]), 0), 'id'), 'foo', 'Filtered next check' ]);

// *** unit/core.js ***
//  1288: 	equals( $("#ap").next("p").length, 0, "Filtered next check, no match" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'next', [ 'p' ]), 'length'), 0, 'Filtered next check, no match' ]);

// *** unit/core.js ***
//  1289: 	equals( $("#ap").next("div, p")[0].id, "foo", "Multiple filters" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'next', [ 'div, p' ]), 0), 'id'), 'foo', 'Multiple filters' ]);
})) ]);

// *** unit/core.js ***
//  1292: test("prev([String])", function() {

$v.cf($v.ro('test'), [ 'prev([String])', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//  1294: 	equals( $("#foo").prev()[0].id, "ap", "Simple prev check" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'prev', [ ]), 0), 'id'), 'ap', 'Simple prev check' ]);

// *** unit/core.js ***
//  1295: 	equals( $("#foo").prev("p")[0].id, "ap", "Filtered prev check" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'prev', [ 'p' ]), 0), 'id'), 'ap', 'Filtered prev check' ]);

// *** unit/core.js ***
//  1296: 	equals( $("#foo").prev("div").length, 0, "Filtered prev check, no match" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'prev', [ 'div' ]), 'length'), 0, 'Filtered prev check, no match' ]);

// *** unit/core.js ***
//  1297: 	equals( $("#foo").prev("p, div")[0].id, "ap", "Multiple filters" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'prev', [ 'p, div' ]), 0), 'id'), 'ap', 'Multiple filters' ]);
})) ]);
$v.cf($v.ro('test'), [ 'show()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 15 ]);

// *** unit/core.js ***
//  1302: 	var pass = true, div = $("div");

var pass = true, div = $v.cf($v.ro('$'), [ 'div' ]);

// *** unit/core.js ***
//  1303: 	div.show().each(function(){

$v.cm($v.cm(div, 'show', [ ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/core.js ***
//  1304: 		if ( this.style.display == "none" ) pass = false;

if ($v.r($v.r($dis, 'style'), 'display') == 'none') pass = false;
})) ]);

// *** unit/core.js ***
//  1306: 	ok( pass, "Show" );

$v.cf($v.ro('ok'), [ pass, 'Show' ]);

// *** unit/core.js ***
//  1308: 	$("#main").append('<div id="show-tests"><div><p><a href="#"><�a><�p><code><�code><pre><�pre><span><�span><�div><table><thead><tr><th><�th><�tr><�thead><tbody><tr><td><�td><�tr><�tbody><�table><ul><li><�li><�ul><�div>');

$v.cm($v.cf($v.ro('$'), [ '#main' ]), 'append', [ '\x3cdiv id=\"show-tests\"\x3e\x3cdiv\x3e\x3cp\x3e\x3ca href=\"#\"\x3e\x3c/a\x3e\x3c/p\x3e\x3ccode\x3e\x3c/code\x3e\x3cpre\x3e\x3c/pre\x3e\x3cspan\x3e\x3c/span\x3e\x3c/div\x3e\x3ctable\x3e\x3cthead\x3e\x3ctr\x3e\x3cth\x3e\x3c/th\x3e\x3c/tr\x3e\x3c/thead\x3e\x3ctbody\x3e\x3ctr\x3e\x3ctd\x3e\x3c/td\x3e\x3c/tr\x3e\x3c/tbody\x3e\x3c/table\x3e\x3cul\x3e\x3cli\x3e\x3c/li\x3e\x3c/ul\x3e\x3c/div\x3e' ]);

// *** unit/core.js ***
//  1309: 	var test = {
//  1310: 		"div"      : "block",
//  1311: 		"p"        : "block",
//  1312: 		"a"        : "inline",
//  1313: 		"code"     : "inline",
//  1314: 		"pre"      : "block",
//  1315: 		"span"     : "inline",
//  1316: 		"table"    : $.browser.msie ? "block" : "table",
//  1317: 		"thead"    : $.browser.msie ? "block" : "table-header-group",
//  1318: 		"tbody"    : $.browser.msie ? "block" : "table-row-group",
//  1319: 		"tr"       : $.browser.msie ? "block" : "table-row",
//  1320: 		"th"       : $.browser.msie ? "block" : "table-cell",
//  1321: 		"td"       : $.browser.msie ? "block" : "table-cell",
//  1322: 		"ul"       : "block",
//  1323: 		"li"       : $.browser.msie ? "block" : "list-item"
//  1324: 	};

var test = ___.initializeMap([ 'div', 'block', 'p', 'block', 'a', 'inline', 'code', 'inline', 'pre', 'block', 'span', 'inline', 'table', $v.r($v.r($v.ro('$'), 'browser'), 'msie') ? 'block': 'table', 'thead', $v.r($v.r($v.ro('$'), 'browser'), 'msie') ? 'block': 'table-header-group', 'tbody', $v.r($v.r($v.ro('$'), 'browser'), 'msie') ? 'block': 'table-row-group', 'tr', $v.r($v.r($v.ro('$'), 'browser'), 'msie') ? 'block': 'table-row', 'th', $v.r($v.r($v.ro('$'), 'browser'), 'msie') ? 'block': 'table-cell', 'td', $v.r($v.r($v.ro('$'), 'browser'), 'msie') ? 'block': 'table-cell', 'ul', 'block', 'li', $v.r($v.r($v.ro('$'), 'browser'), 'msie') ? 'block': 'list-item' ]);

// *** unit/core.js ***
//  1326: 	$.each(test, function(selector, expected) {

$v.cm($v.ro('$'), 'each', [ test, $v.dis(___.frozenFunc(function ($dis, selector, expected) {

// *** unit/core.js ***
//  1327: 		var elem = $(selector, "#show-tests").show();

var elem = $v.cm($v.cf($v.ro('$'), [ selector, '#show-tests' ]), 'show', [ ]);
$v.cf($v.ro('equals'), [ $v.cm(elem, 'css', [ 'display' ]), expected, 'Show using correct display type for ' + selector ]);

// *** unit/core.js ***
//  1328: 		equals( elem.css("display"), expected, "Show using correct display type for " + selector );

})) ]);

// *** unit/core.js ***
//  1329: 	});

})) ]);

// *** unit/core.js ***
//  1332: test("addClass(String)", function() {

$v.cf($v.ro('test'), [ 'addClass(String)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);

// *** unit/core.js ***
//  1334: 	var div = $("div");

var div = $v.cf($v.ro('$'), [ 'div' ]);

// *** unit/core.js ***
//  1335: 	div.addClass("test");

$v.cm(div, 'addClass', [ 'test' ]);

// *** unit/core.js ***
//  1336: 	var pass = true;

var pass = true;

// *** unit/core.js ***
//  1337: 	for ( var i = 0; i < div.size(); i++ ) {
//  1339: 	}

for (var i = 0; i < $v.cm(div, 'size', [ ]); i++) {

// *** unit/core.js ***
//  1338: 	 if ( div.get(i).className.indexOf("test") == -1 ) pass = false;

if ($v.cm($v.r($v.cm(div, 'get', [ i ]), 'className'), 'indexOf', [ 'test' ]) == -1) pass = false;
}

// *** unit/core.js ***
//  1340: 	ok( pass, "Add Class" );

$v.cf($v.ro('ok'), [ pass, 'Add Class' ]);

// *** unit/core.js ***
//  1343: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);

// *** unit/core.js ***
//  1344: 	j.addClass("asdf");

$v.cm(j, 'addClass', [ 'asdf' ]);
$v.cf($v.ro('ok'), [ $v.cm(j, 'hasClass', [ 'asdf' ]), 'Check node,textnode,comment for addClass' ]);

// *** unit/core.js ***
//  1345: 	ok( j.hasClass("asdf"), "Check node,textnode,comment for addClass" );

})) ]);

// *** unit/core.js ***
//  1348: test("removeClass(String) - simple", function() {

$v.cf($v.ro('test'), [ 'removeClass(String) - simple', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//  1350: 	var div = $("div").addClass("test").removeClass("test"),
//  1351: 		pass = true;

var div = $v.cm($v.cm($v.cf($v.ro('$'), [ 'div' ]), 'addClass', [ 'test' ]), 'removeClass', [ 'test' ]), pass = true;

// *** unit/core.js ***
//  1352: 	for ( var i = 0; i < div.size(); i++ ) {
//  1354: 	}

for (var i = 0; i < $v.cm(div, 'size', [ ]); i++) {

// *** unit/core.js ***
//  1353: 		if ( div.get(i).className.indexOf("test") != -1 ) pass = false;

if ($v.cm($v.r($v.cm(div, 'get', [ i ]), 'className'), 'indexOf', [ 'test' ]) != -1) pass = false;
}

// *** unit/core.js ***
//  1355: 	ok( pass, "Remove Class" );

$v.cf($v.ro('ok'), [ pass, 'Remove Class' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//  1358: 	var div = $("div").addClass("test").addClass("foo").addClass("bar");

var div = $v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ 'div' ]), 'addClass', [ 'test' ]), 'addClass', [ 'foo' ]), 'addClass', [ 'bar' ]);

// *** unit/core.js ***
//  1359: 	div.removeClass("test").removeClass("bar").removeClass("foo");

$v.cm($v.cm($v.cm(div, 'removeClass', [ 'test' ]), 'removeClass', [ 'bar' ]), 'removeClass', [ 'foo' ]);

// *** unit/core.js ***
//  1360: 	var pass = true;

var pass = true;

// *** unit/core.js ***
//  1361: 	for ( var i = 0; i < div.size(); i++ ) {
//  1363: 	}

for (var i = 0; i < $v.cm(div, 'size', [ ]); i++) {

// *** unit/core.js ***
//  1362: 	 if ( div.get(i).className.match(/test|bar|foo/) ) pass = false;

if ($v.cm($v.r($v.cm(div, 'get', [ i ]), 'className'), 'match', [ $v.construct(RegExp, [ 'test|bar|foo' ]) ])) pass = false;
}

// *** unit/core.js ***
//  1364: 	ok( pass, "Remove multiple classes" );

$v.cf($v.ro('ok'), [ pass, 'Remove multiple classes' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//  1367: 	var div = $("div:eq(0)").addClass("test").removeClass("");

var div = $v.cm($v.cm($v.cf($v.ro('$'), [ 'div:eq(0)' ]), 'addClass', [ 'test' ]), 'removeClass', [ '' ]);

// *** unit/core.js ***
//  1368: 	ok( div.is('.test'), "Empty string passed to removeClass" );

$v.cf($v.ro('ok'), [ $v.cm(div, 'is', [ '.test' ]), 'Empty string passed to removeClass' ]);

// *** unit/core.js ***
//  1371: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);

// *** unit/core.js ***
//  1372: 	j.removeClass("asdf");

$v.cm(j, 'removeClass', [ 'asdf' ]);
$v.cf($v.ro('ok'), [ !$v.cm(j, 'hasClass', [ 'asdf' ]), 'Check node,textnode,comment for removeClass' ]);

// *** unit/core.js ***
//  1373: 	ok( !j.hasClass("asdf"), "Check node,textnode,comment for removeClass" );

})) ]);

// *** unit/core.js ***
//  1376: test("toggleClass(String)", function() {

$v.cf($v.ro('test'), [ 'toggleClass(String)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);

// *** unit/core.js ***
//  1378: 	var e = $("#firstp");

var e = $v.cf($v.ro('$'), [ '#firstp' ]);

// *** unit/core.js ***
//  1379: 	ok( !e.is(".test"), "Assert class not present" );

$v.cf($v.ro('ok'), [ !$v.cm(e, 'is', [ '.test' ]), 'Assert class not present' ]);

// *** unit/core.js ***
//  1380: 	e.toggleClass("test");

$v.cm(e, 'toggleClass', [ 'test' ]);

// *** unit/core.js ***
//  1381: 	ok( e.is(".test"), "Assert class present" );

$v.cf($v.ro('ok'), [ $v.cm(e, 'is', [ '.test' ]), 'Assert class present' ]);

// *** unit/core.js ***
//  1382: 	e.toggleClass("test");

$v.cm(e, 'toggleClass', [ 'test' ]);
$v.cf($v.ro('ok'), [ !$v.cm(e, 'is', [ '.test' ]), 'Assert class not present' ]);

// *** unit/core.js ***
//  1383: 	ok( !e.is(".test"), "Assert class not present" );

})) ]);

// *** unit/core.js ***
//  1386: test("removeAttr(String", function() {

$v.cf($v.ro('test'), [ 'removeAttr(String', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);

// *** unit/core.js ***
//  1388: 	equals( $('#mark').removeAttr("class")[0].className, "", "remove class" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#mark' ]), 'removeAttr', [ 'class' ]), 0), 'className'), '', 'remove class' ]);
})) ]);

// *** unit/core.js ***
//  1391: test("text(String)", function() {

$v.cf($v.ro('test'), [ 'text(String)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/core.js ***
//  1393: 	equals( $("#foo").text("<div><b>Hello<�b> cruel world!<�div>")[0].innerHTML, "&lt;div&gt;&lt;b&gt;Hello&lt;/b&gt; cruel world!&lt;/div&gt;", "Check escaped text" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'text', [ '\x3cdiv\x3e\x3cb\x3eHello\x3c/b\x3e cruel world!\x3c/div\x3e' ]), 0), 'innerHTML'), '\x26lt;div\x26gt;\x26lt;b\x26gt;Hello\x26lt;/b\x26gt; cruel world!\x26lt;/div\x26gt;', 'Check escaped text' ]);

// *** unit/core.js ***
//  1396: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);
$v.cm(j, 'text', [ 'hi!' ]);

// *** unit/core.js ***
//  1398: 	equals( $(j[0]).text(), "hi!", "Check node,textnode,comment with text()" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ $v.r(j, 0) ]), 'text', [ ]), 'hi!', 'Check node,textnode,comment with text()' ]);

// *** unit/core.js ***
//  1399: 	equals( j[1].nodeValue, " there ", "Check node,textnode,comment with text()" );

$v.cf($v.ro('equals'), [ $v.r($v.r(j, 1), 'nodeValue'), ' there ', 'Check node,textnode,comment with text()' ]);
$v.cf($v.ro('equals'), [ $v.r($v.r(j, 2), 'nodeType'), 8, 'Check node,textnode,comment with text()' ]);

// *** unit/core.js ***
//  1400: 	equals( j[2].nodeType, 8, "Check node,textnode,comment with text()" );

})) ]);

// *** unit/core.js ***
//  1403: test("$.each(Object,Function)", function() {

$v.cf($v.ro('test'), [ '$.each(Object,Function)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 12 ]);

// *** unit/core.js ***
//  1405: 	$.each( [0,1,2], function(i, n){

$v.cm($v.ro('$'), 'each', [ [ 0, 1, 2 ], $v.dis(___.frozenFunc(function ($dis, i, n) {
$v.cf($v.ro('equals'), [ i, n, 'Check array iteration' ]);

// *** unit/core.js ***
//  1406: 		equals( i, n, "Check array iteration" );

})) ]);

// *** unit/core.js ***
//  1409: 	$.each( [5,6,7], function(i, n){

$v.cm($v.ro('$'), 'each', [ [ 5, 6, 7 ], $v.dis(___.frozenFunc(function ($dis, i, n) {
$v.cf($v.ro('equals'), [ i, n - 5, 'Check array iteration' ]);

// *** unit/core.js ***
//  1410: 		equals( i, n - 5, "Check array iteration" );

})) ]);

// *** unit/core.js ***
//  1413: 	$.each( { name: "name", lang: "lang" }, function(i, n){

$v.cm($v.ro('$'), 'each', [ ___.initializeMap([ 'name', 'name', 'lang', 'lang' ]), $v.dis(___.frozenFunc(function ($dis, i, n) {
$v.cf($v.ro('equals'), [ i, n, 'Check object iteration' ]);

// *** unit/core.js ***
//  1414: 		equals( i, n, "Check object iteration" );

})) ]);

// *** unit/core.js ***
//  1417:         var total = 0;

var total = 0;
$v.cm($v.ro('jQuery'), 'each', [ [ 1, 2, 3 ], $v.dis(___.frozenFunc(function ($dis, i, v) {

// *** unit/core.js ***
//  1418:         jQuery.each([1,2,3], function(i,v){ total += v; });

total = total + v;
})) ]);

// *** unit/core.js ***
//  1419:         equals( total, 6, "Looping over an array" );

$v.cf($v.ro('equals'), [ total, 6, 'Looping over an array' ]);

// *** unit/core.js ***
//  1420:         total = 0;

total = 0;
$v.cm($v.ro('jQuery'), 'each', [ [ 1, 2, 3 ], $v.dis(___.frozenFunc(function ($dis, i, v) {
total = total + v;

// *** unit/core.js ***
//  1421:         jQuery.each([1,2,3], function(i,v){ total += v; if ( i == 1 ) return false; });

if (i == 1) return false;
})) ]);

// *** unit/core.js ***
//  1422:         equals( total, 3, "Looping over an array, with break" );

$v.cf($v.ro('equals'), [ total, 3, 'Looping over an array, with break' ]);

// *** unit/core.js ***
//  1423:         total = 0;

total = 0;
$v.cm($v.ro('jQuery'), 'each', [ ___.initializeMap([ 'a', 1, 'b', 2, 'c', 3 ]), $v.dis(___.frozenFunc(function ($dis, i, v) {

// *** unit/core.js ***
//  1424:         jQuery.each({"a":1,"b":2,"c":3}, function(i,v){ total += v; });

total = total + v;
})) ]);

// *** unit/core.js ***
//  1425:         equals( total, 6, "Looping over an object" );

$v.cf($v.ro('equals'), [ total, 6, 'Looping over an object' ]);

// *** unit/core.js ***
//  1426:         total = 0;

total = 0;
$v.cm($v.ro('jQuery'), 'each', [ ___.initializeMap([ 'a', 3, 'b', 3, 'c', 3 ]), $v.dis(___.frozenFunc(function ($dis, i, v) {

// *** unit/core.js ***
//  1427:         jQuery.each({"a":3,"b":3,"c":3}, function(i,v){ total += v; return false; });

total = total + v;
return false;
})) ]);
$v.cf($v.ro('equals'), [ total, 3, 'Looping over an object, with break' ]);

// *** unit/core.js ***
//  1428:         equals( total, 3, "Looping over an object, with break" );

})) ]);
$v.cf($v.ro('test'), [ '$.prop', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
var handle = ___.frozenFunc(function () {
function handle$_var$($dis) {

// *** unit/core.js ***
//  1433: 	var handle = function() { return this.id };

return $v.r($dis, 'id');
}
___.func(handle$_var$, 'handle$_var$');
;
var handle$_var = $v.dis(___.primFreeze(handle$_var$), 'handle$_var');
return handle$_var;
}).CALL___();

// *** unit/core.js ***
//  1434: 	equals( $.prop($("#ap")[0], handle), "ap", "Check with Function argument" );

$v.cf($v.ro('equals'), [ $v.cm($v.ro('$'), 'prop', [ $v.r($v.cf($v.ro('$'), [ '#ap' ]), 0), handle ]), 'ap', 'Check with Function argument' ]);

// *** unit/core.js ***
//  1435: 	equals( $.prop($("#ap")[0], "value"), "value", "Check with value argument" );

$v.cf($v.ro('equals'), [ $v.cm($v.ro('$'), 'prop', [ $v.r($v.cf($v.ro('$'), [ '#ap' ]), 0), 'value' ]), 'value', 'Check with value argument' ]);
})) ]);

// *** unit/core.js ***
//  1438: test("$.className", function() {

$v.cf($v.ro('test'), [ '$.className', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/core.js ***
//  1440: 	var x = $("<p>Hi<�p>")[0];

var x = $v.r($v.cf($v.ro('$'), [ '\x3cp\x3eHi\x3c/p\x3e' ]), 0);

// *** unit/core.js ***
//  1441: 	var c = $.className;

var c = $v.r($v.ro('$'), 'className');
$v.cm(c, 'add', [ x, 'hi' ]);

// *** unit/core.js ***
//  1443: 	equals( x.className, "hi", "Check single added class" );

$v.cf($v.ro('equals'), [ $v.r(x, 'className'), 'hi', 'Check single added class' ]);

// *** unit/core.js ***
//  1444: 	c.add(x, "foo bar");

$v.cm(c, 'add', [ x, 'foo bar' ]);

// *** unit/core.js ***
//  1445: 	equals( x.className, "hi foo bar", "Check more added classes" );

$v.cf($v.ro('equals'), [ $v.r(x, 'className'), 'hi foo bar', 'Check more added classes' ]);
$v.cm(c, 'remove', [ x ]);

// *** unit/core.js ***
//  1447: 	equals( x.className, "", "Remove all classes" );

$v.cf($v.ro('equals'), [ $v.r(x, 'className'), '', 'Remove all classes' ]);

// *** unit/core.js ***
//  1448: 	c.add(x, "hi foo bar");

$v.cm(c, 'add', [ x, 'hi foo bar' ]);

// *** unit/core.js ***
//  1449: 	c.remove(x, "foo");

$v.cm(c, 'remove', [ x, 'foo' ]);

// *** unit/core.js ***
//  1450: 	equals( x.className, "hi bar", "Check removal of one class" );

$v.cf($v.ro('equals'), [ $v.r(x, 'className'), 'hi bar', 'Check removal of one class' ]);

// *** unit/core.js ***
//  1451: 	ok( c.has(x, "hi"), "Check has1" );

$v.cf($v.ro('ok'), [ $v.cm(c, 'has', [ x, 'hi' ]), 'Check has1' ]);
$v.cf($v.ro('ok'), [ $v.cm(c, 'has', [ x, 'bar' ]), 'Check has2' ]);

// *** unit/core.js ***
//  1452: 	ok( c.has(x, "bar"), "Check has2" );

})) ]);
$v.cf($v.ro('test'), [ '$.data', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 5 ]);

// *** unit/core.js ***
//  1457: 	var div = $("#foo")[0];

var div = $v.r($v.cf($v.ro('$'), [ '#foo' ]), 0);

// *** unit/core.js ***
//  1458: 	equals( jQuery.data(div, "test"), undefined, "Check for no data exists" );

$v.cf($v.ro('equals'), [ $v.cm($v.ro('jQuery'), 'data', [ div, 'test' ]), $v.ro('undefined'), 'Check for no data exists' ]);

// *** unit/core.js ***
//  1459: 	jQuery.data(div, "test", "success");

$v.cm($v.ro('jQuery'), 'data', [ div, 'test', 'success' ]);

// *** unit/core.js ***
//  1460: 	equals( jQuery.data(div, "test"), "success", "Check for added data" );

$v.cf($v.ro('equals'), [ $v.cm($v.ro('jQuery'), 'data', [ div, 'test' ]), 'success', 'Check for added data' ]);

// *** unit/core.js ***
//  1461: 	jQuery.data(div, "test", "overwritten");

$v.cm($v.ro('jQuery'), 'data', [ div, 'test', 'overwritten' ]);

// *** unit/core.js ***
//  1462: 	equals( jQuery.data(div, "test"), "overwritten", "Check for overwritten data" );

$v.cf($v.ro('equals'), [ $v.cm($v.ro('jQuery'), 'data', [ div, 'test' ]), 'overwritten', 'Check for overwritten data' ]);

// *** unit/core.js ***
//  1463: 	jQuery.data(div, "test", undefined);

$v.cm($v.ro('jQuery'), 'data', [ div, 'test', $v.ro('undefined') ]);

// *** unit/core.js ***
//  1464: 	equals( jQuery.data(div, "test"), "overwritten", "Check that data wasn't removed");

$v.cf($v.ro('equals'), [ $v.cm($v.ro('jQuery'), 'data', [ div, 'test' ]), 'overwritten', 'Check that data wasn\'t removed' ]);

// *** unit/core.js ***
//  1465: 	jQuery.data(div, "test", null);

$v.cm($v.ro('jQuery'), 'data', [ div, 'test', null ]);
$v.cf($v.ro('ok'), [ $v.cm($v.ro('jQuery'), 'data', [ div, 'test' ]) === null, 'Check for null data' ]);

// *** unit/core.js ***
//  1466: 	ok( jQuery.data(div, "test") === null, "Check for null data");

})) ]);
$v.cf($v.ro('test'), [ '.data()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 18 ]);

// *** unit/core.js ***
//  1471: 	var div = $("#foo");

var div = $v.cf($v.ro('$'), [ '#foo' ]);

// *** unit/core.js ***
//  1472: 	equals( div.data("test"), undefined, "Check for no data exists" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test' ]), $v.ro('undefined'), 'Check for no data exists' ]);

// *** unit/core.js ***
//  1473: 	div.data("test", "success");

$v.cm(div, 'data', [ 'test', 'success' ]);

// *** unit/core.js ***
//  1474: 	equals( div.data("test"), "success", "Check for added data" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test' ]), 'success', 'Check for added data' ]);

// *** unit/core.js ***
//  1475: 	div.data("test", "overwritten");

$v.cm(div, 'data', [ 'test', 'overwritten' ]);

// *** unit/core.js ***
//  1476: 	equals( div.data("test"), "overwritten", "Check for overwritten data" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test' ]), 'overwritten', 'Check for overwritten data' ]);

// *** unit/core.js ***
//  1477: 	div.data("test", undefined);

$v.cm(div, 'data', [ 'test', $v.ro('undefined') ]);

// *** unit/core.js ***
//  1478: 	equals( div.data("test"), "overwritten", "Check that data wasn't removed");

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test' ]), 'overwritten', 'Check that data wasn\'t removed' ]);

// *** unit/core.js ***
//  1479: 	div.data("test", null);

$v.cm(div, 'data', [ 'test', null ]);

// *** unit/core.js ***
//  1480: 	ok( div.data("test") === null, "Check for null data");

$v.cf($v.ro('ok'), [ $v.cm(div, 'data', [ 'test' ]) === null, 'Check for null data' ]);

// *** unit/core.js ***
//  1482: 	div.data("test", "overwritten");

$v.cm(div, 'data', [ 'test', 'overwritten' ]);

// *** unit/core.js ***
//  1483: 	var hits = {test:0}, gets = {test:0};

var hits = ___.initializeMap([ 'test', 0 ]), gets = ___.initializeMap([ 'test', 0 ]);
$v.cm($v.cm($v.cm($v.cm(div, 'bind', [ 'setData', $v.dis(___.frozenFunc(function ($dis, e, key, value) {
$v.s(hits, key, $v.r(hits, key) + value);

// *** unit/core.js ***
//  1486: 		.bind("setData",function(e,key,value){ hits[key] += value; })

})) ]), 'bind', [ 'setData.foo', $v.dis(___.frozenFunc(function ($dis, e, key, value) {
$v.s(hits, key, $v.r(hits, key) + value);

// *** unit/core.js ***
//  1487: 		.bind("setData.foo",function(e,key,value){ hits[key] += value; })

})) ]), 'bind', [ 'getData', $v.dis(___.frozenFunc(function ($dis, e, key) {
$v.s(gets, key, $v.r(gets, key) + 1);

// *** unit/core.js ***
//  1488: 		.bind("getData",function(e,key){ gets[key] += 1; })

})) ]), 'bind', [ 'getData.foo', $v.dis(___.frozenFunc(function ($dis, e, key) {
$v.s(gets, key, $v.r(gets, key) + 3);

// *** unit/core.js ***
//  1489: 		.bind("getData.foo",function(e,key){ gets[key] += 3; });

})) ]);

// *** unit/core.js ***
//  1491: 	div.data("test.foo", 2);

$v.cm(div, 'data', [ 'test.foo', 2 ]);

// *** unit/core.js ***
//  1492: 	equals( div.data("test"), "overwritten", "Check for original data" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test' ]), 'overwritten', 'Check for original data' ]);

// *** unit/core.js ***
//  1493: 	equals( div.data("test.foo"), 2, "Check for namespaced data" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test.foo' ]), 2, 'Check for namespaced data' ]);

// *** unit/core.js ***
//  1494: 	equals( div.data("test.bar"), "overwritten", "Check for unmatched namespace" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test.bar' ]), 'overwritten', 'Check for unmatched namespace' ]);

// *** unit/core.js ***
//  1495: 	equals( hits.test, 2, "Check triggered setter functions" );

$v.cf($v.ro('equals'), [ $v.r(hits, 'test'), 2, 'Check triggered setter functions' ]);

// *** unit/core.js ***
//  1496: 	equals( gets.test, 5, "Check triggered getter functions" );

$v.cf($v.ro('equals'), [ $v.r(gets, 'test'), 5, 'Check triggered getter functions' ]);
$v.s(hits, 'test', 0);
$v.s(gets, 'test', 0);

// *** unit/core.js ***
//  1501: 	div.data("test", 1);

$v.cm(div, 'data', [ 'test', 1 ]);

// *** unit/core.js ***
//  1502: 	equals( div.data("test"), 1, "Check for original data" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test' ]), 1, 'Check for original data' ]);

// *** unit/core.js ***
//  1503: 	equals( div.data("test.foo"), 2, "Check for namespaced data" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test.foo' ]), 2, 'Check for namespaced data' ]);

// *** unit/core.js ***
//  1504: 	equals( div.data("test.bar"), 1, "Check for unmatched namespace" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test.bar' ]), 1, 'Check for unmatched namespace' ]);

// *** unit/core.js ***
//  1505: 	equals( hits.test, 1, "Check triggered setter functions" );

$v.cf($v.ro('equals'), [ $v.r(hits, 'test'), 1, 'Check triggered setter functions' ]);

// *** unit/core.js ***
//  1506: 	equals( gets.test, 5, "Check triggered getter functions" );

$v.cf($v.ro('equals'), [ $v.r(gets, 'test'), 5, 'Check triggered getter functions' ]);
$v.s(hits, 'test', 0);
$v.s(gets, 'test', 0);
$v.cm($v.cm(div, 'bind', [ 'getData', $v.dis(___.frozenFunc(function ($dis, e, key) {

// *** unit/core.js ***
//  1512: 		.bind("getData",function(e,key){ return key + "root"; })

return key + 'root';
})) ]), 'bind', [ 'getData.foo', $v.dis(___.frozenFunc(function ($dis, e, key) {

// *** unit/core.js ***
//  1513: 		.bind("getData.foo",function(e,key){ return key + "foo"; });

return key + 'foo';
})) ]);

// *** unit/core.js ***
//  1515: 	equals( div.data("test"), "testroot", "Check for original data" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test' ]), 'testroot', 'Check for original data' ]);

// *** unit/core.js ***
//  1516: 	equals( div.data("test.foo"), "testfoo", "Check for namespaced data" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test.foo' ]), 'testfoo', 'Check for namespaced data' ]);

// *** unit/core.js ***
//  1517: 	equals( div.data("test.bar"), "testroot", "Check for unmatched namespace" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test.bar' ]), 'testroot', 'Check for unmatched namespace' ]);
})) ]);

// *** unit/core.js ***
//  1520: test("$.removeData", function() {

$v.cf($v.ro('test'), [ '$.removeData', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);

// *** unit/core.js ***
//  1522: 	var div = $("#foo")[0];

var div = $v.r($v.cf($v.ro('$'), [ '#foo' ]), 0);

// *** unit/core.js ***
//  1523: 	jQuery.data(div, "test", "testing");

$v.cm($v.ro('jQuery'), 'data', [ div, 'test', 'testing' ]);

// *** unit/core.js ***
//  1524: 	jQuery.removeData(div, "test");

$v.cm($v.ro('jQuery'), 'removeData', [ div, 'test' ]);

// *** unit/core.js ***
//  1525: 	equals( jQuery.data(div, "test"), undefined, "Check removal of data" );

$v.cf($v.ro('equals'), [ $v.cm($v.ro('jQuery'), 'data', [ div, 'test' ]), $v.ro('undefined'), 'Check removal of data' ]);
})) ]);

// *** unit/core.js ***
//  1528: test(".removeData()", function() {

$v.cf($v.ro('test'), [ '.removeData()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/core.js ***
//  1530: 	var div = $("#foo");

var div = $v.cf($v.ro('$'), [ '#foo' ]);

// *** unit/core.js ***
//  1531: 	div.data("test", "testing");

$v.cm(div, 'data', [ 'test', 'testing' ]);

// *** unit/core.js ***
//  1532: 	div.removeData("test");

$v.cm(div, 'removeData', [ 'test' ]);

// *** unit/core.js ***
//  1533: 	equals( div.data("test"), undefined, "Check removal of data" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test' ]), $v.ro('undefined'), 'Check removal of data' ]);

// *** unit/core.js ***
//  1535: 	div.data("test", "testing");

$v.cm(div, 'data', [ 'test', 'testing' ]);

// *** unit/core.js ***
//  1536: 	div.data("test.foo", "testing2");

$v.cm(div, 'data', [ 'test.foo', 'testing2' ]);

// *** unit/core.js ***
//  1537: 	div.removeData("test.bar");

$v.cm(div, 'removeData', [ 'test.bar' ]);

// *** unit/core.js ***
//  1538: 	equals( div.data("test.foo"), "testing2", "Make sure data is intact" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test.foo' ]), 'testing2', 'Make sure data is intact' ]);

// *** unit/core.js ***
//  1539: 	equals( div.data("test"), "testing", "Make sure data is intact" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test' ]), 'testing', 'Make sure data is intact' ]);

// *** unit/core.js ***
//  1541: 	div.removeData("test");

$v.cm(div, 'removeData', [ 'test' ]);

// *** unit/core.js ***
//  1542: 	equals( div.data("test.foo"), "testing2", "Make sure data is intact" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test.foo' ]), 'testing2', 'Make sure data is intact' ]);

// *** unit/core.js ***
//  1543: 	equals( div.data("test"), undefined, "Make sure data is intact" );

$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test' ]), $v.ro('undefined'), 'Make sure data is intact' ]);

// *** unit/core.js ***
//  1545: 	div.removeData("test.foo");

$v.cm(div, 'removeData', [ 'test.foo' ]);
$v.cf($v.ro('equals'), [ $v.cm(div, 'data', [ 'test.foo' ]), $v.ro('undefined'), 'Make sure data is intact' ]);

// *** unit/core.js ***
//  1546: 	equals( div.data("test.foo"), undefined, "Make sure data is intact" );

})) ]);
$v.cf($v.ro('test'), [ 'remove()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/core.js ***
//  1551: 	$("#ap").children().remove();

$v.cm($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'children', [ ]), 'remove', [ ]);

// *** unit/core.js ***
//  1552: 	ok( $("#ap").text().length > 10, "Check text is not removed" );

$v.cf($v.ro('ok'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'text', [ ]), 'length') > 10, 'Check text is not removed' ]);

// *** unit/core.js ***
//  1553: 	equals( $("#ap").children().length, 0, "Check remove" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'children', [ ]), 'length'), 0, 'Check remove' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/core.js ***
//  1556: 	$("#ap").children().remove("a");

$v.cm($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'children', [ ]), 'remove', [ 'a' ]);

// *** unit/core.js ***
//  1557: 	ok( $("#ap").text().length > 10, "Check text is not removed" );

$v.cf($v.ro('ok'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'text', [ ]), 'length') > 10, 'Check text is not removed' ]);

// *** unit/core.js ***
//  1558: 	equals( $("#ap").children().length, 1, "Check filtered remove" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'children', [ ]), 'length'), 1, 'Check filtered remove' ]);

// *** unit/core.js ***
//  1561: 	equals( $("#nonnodes").contents().length, 3, "Check node,textnode,comment remove works" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]), 'length'), 3, 'Check node,textnode,comment remove works' ]);

// *** unit/core.js ***
//  1562: 	$("#nonnodes").contents().remove();

$v.cm($v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]), 'remove', [ ]);

// *** unit/core.js ***
//  1563: 	equals( $("#nonnodes").contents().length, 0, "Check node,textnode,comment remove works" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]), 'length'), 0, 'Check node,textnode,comment remove works' ]);
})) ]);
$v.cf($v.ro('test'), [ 'empty()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);

// *** unit/core.js ***
//  1568: 	equals( $("#ap").children().empty().text().length, 0, "Check text is removed" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'children', [ ]), 'empty', [ ]), 'text', [ ]), 'length'), 0, 'Check text is removed' ]);

// *** unit/core.js ***
//  1569: 	equals( $("#ap").children().length, 4, "Check elements are not removed" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'children', [ ]), 'length'), 4, 'Check elements are not removed' ]);

// *** unit/core.js ***
//  1572: 	var j = $("#nonnodes").contents();

var j = $v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]);
$v.cm(j, 'empty', [ ]);
$v.cf($v.ro('equals'), [ $v.cm(j, 'html', [ ]), '', 'Check node,textnode,comment empty works' ]);

// *** unit/core.js ***
//  1574: 	equals( j.html(), "", "Check node,textnode,comment empty works" );

})) ]);
$v.cf($v.ro('test'), [ 'slice()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 5 ]);

// *** unit/core.js ***
//  1579: 	isSet( $("#ap a").slice(1,2), q("groups"), "slice(1,2)" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cf($v.ro('$'), [ '#ap a' ]), 'slice', [ 1, 2 ]), $v.cf($v.ro('q'), [ 'groups' ]), 'slice(1,2)' ]);

// *** unit/core.js ***
//  1580: 	isSet( $("#ap a").slice(1), q("groups", "anchor1", "mark"), "slice(1)" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cf($v.ro('$'), [ '#ap a' ]), 'slice', [ 1 ]), $v.cf($v.ro('q'), [ 'groups', 'anchor1', 'mark' ]), 'slice(1)' ]);

// *** unit/core.js ***
//  1581: 	isSet( $("#ap a").slice(0,3), q("google", "groups", "anchor1"), "slice(0,3)" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cf($v.ro('$'), [ '#ap a' ]), 'slice', [ 0, 3 ]), $v.cf($v.ro('q'), [ 'google', 'groups', 'anchor1' ]), 'slice(0,3)' ]);

// *** unit/core.js ***
//  1582: 	isSet( $("#ap a").slice(-1), q("mark"), "slice(-1)" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cf($v.ro('$'), [ '#ap a' ]), 'slice', [ -1 ]), $v.cf($v.ro('q'), [ 'mark' ]), 'slice(-1)' ]);
$v.cf($v.ro('isSet'), [ $v.cm($v.cf($v.ro('$'), [ '#ap a' ]), 'eq', [ 1 ]), $v.cf($v.ro('q'), [ 'groups' ]), 'eq(1)' ]);

// *** unit/core.js ***
//  1584: 	isSet( $("#ap a").eq(1), q("groups"), "eq(1)" );

})) ]);
$v.cf($v.ro('test'), [ 'map()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);

// *** unit/core.js ***
//  1591: 		$("#ap").map(function(){

$v.cf($v.ro('isSet'), [ $v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'map', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/core.js ***
//  1592: 			return $(this).find("a").get();

return $v.cm($v.cm($v.cf($v.ro('$'), [ $dis ]), 'find', [ 'a' ]), 'get', [ ]);

// *** unit/core.js ***
//  1594: 		q("google", "groups", "anchor1", "mark"),

})) ]), $v.cf($v.ro('q'), [ 'google', 'groups', 'anchor1', 'mark' ]), 'Array Map' ]);

// *** unit/core.js ***
//  1599: 		$("#ap > a").map(function(){

$v.cf($v.ro('isSet'), [ $v.cm($v.cf($v.ro('$'), [ '#ap \x3e a' ]), 'map', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/core.js ***
//  1600: 			return this.parentNode;

return $v.r($dis, 'parentNode');

// *** unit/core.js ***
//  1602: 		q("ap","ap","ap"),

})) ]), $v.cf($v.ro('q'), [ 'ap', 'ap', 'ap' ]), 'Single Map' ]);

// *** unit/core.js ***
//  1606: 	return;//these haven't been accepted yet

return;

// *** unit/core.js ***
//  1609: 	var keys = $.map( {a:1,b:2}, function( v, k ){
//  1611: 	}, [ ] );

var keys = $v.cm($v.ro('$'), 'map', [ ___.initializeMap([ 'a', 1, 'b', 2 ]), $v.dis(___.frozenFunc(function ($dis, v, k) {

// *** unit/core.js ***
//  1610: 		return k;

return k;
})), [ ] ]);

// *** unit/core.js ***
//  1613: 	equals( keys.join(""), "ab", "Map the keys from a hash to an array" );

$v.cf($v.ro('equals'), [ $v.cm(keys, 'join', [ '' ]), 'ab', 'Map the keys from a hash to an array' ]);

// *** unit/core.js ***
//  1615: 	var values = $.map( {a:1,b:2}, function( v, k ){
//  1617: 	}, [ ] );

var values = $v.cm($v.ro('$'), 'map', [ ___.initializeMap([ 'a', 1, 'b', 2 ]), $v.dis(___.frozenFunc(function ($dis, v, k) {

// *** unit/core.js ***
//  1616: 		return v;

return v;
})), [ ] ]);

// *** unit/core.js ***
//  1619: 	equals( values.join(""), "12", "Map the values from a hash to an array" );

$v.cf($v.ro('equals'), [ $v.cm(values, 'join', [ '' ]), '12', 'Map the values from a hash to an array' ]);

// *** unit/core.js ***
//  1621: 	var scripts = document.getElementsByTagName("script");

var scripts = $v.cm($v.ro('document'), 'getElementsByTagName', [ 'script' ]);

// *** unit/core.js ***
//  1622: 	var mapped = $.map( scripts, function( v, k ){
//  1624: 	}, {length:0} );

var mapped = $v.cm($v.ro('$'), 'map', [ scripts, $v.dis(___.frozenFunc(function ($dis, v, k) {

// *** unit/core.js ***
//  1623: 		return v;

return v;
})), ___.initializeMap([ 'length', 0 ]) ]);

// *** unit/core.js ***
//  1626: 	equals( mapped.length, scripts.length, "Map an array(-like) to a hash" );

$v.cf($v.ro('equals'), [ $v.r(mapped, 'length'), $v.r(scripts, 'length'), 'Map an array(-like) to a hash' ]);

// *** unit/core.js ***
//  1628: 	var flat = $.map( Array(4), function( v, k ){
//  1630: 	});

var flat = $v.cm($v.ro('$'), 'map', [ $v.cf($v.ro('Array'), [ 4 ]), $v.dis(___.frozenFunc(function ($dis, v, k) {

// *** unit/core.js ***
//  1629: 		return k % 2 ? k : [k,k,k];//try mixing array and regular returns

return k % 2? k: [ k, k, k ];
})) ]);

// *** unit/core.js ***
//  1632: 	equals( flat.join(""), "00012223", "try the new flatten technique(#2616)" );

$v.cf($v.ro('equals'), [ $v.cm(flat, 'join', [ '' ]), '00012223', 'try the new flatten technique(#2616)' ]);
})) ]);

// *** unit/core.js ***
//  1635: test("contents()", function() {

$v.cf($v.ro('test'), [ 'contents()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 12 ]);

// *** unit/core.js ***
//  1637: 	equals( $("#ap").contents().length, 9, "Check element contents" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'contents', [ ]), 'length'), 9, 'Check element contents' ]);

// *** unit/core.js ***
//  1638: 	ok( $("#iframe").contents()[0], "Check existance of IFrame document" );

$v.cf($v.ro('ok'), [ $v.r($v.cm($v.cf($v.ro('$'), [ '#iframe' ]), 'contents', [ ]), 0), 'Check existance of IFrame document' ]);

// *** unit/core.js ***
//  1639: 	var ibody = $("#loadediframe").contents()[0].body;

var ibody = $v.r($v.r($v.cm($v.cf($v.ro('$'), [ '#loadediframe' ]), 'contents', [ ]), 0), 'body');

// *** unit/core.js ***
//  1640: 	ok( ibody, "Check existance of IFrame body" );

$v.cf($v.ro('ok'), [ ibody, 'Check existance of IFrame body' ]);

// *** unit/core.js ***
//  1642: 	equals( $("span", ibody).text(), "span text", "Find span in IFrame and check its text" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'span', ibody ]), 'text', [ ]), 'span text', 'Find span in IFrame and check its text' ]);

// *** unit/core.js ***
//  1644: 	$(ibody).append("<div>init text<�div>");

$v.cm($v.cf($v.ro('$'), [ ibody ]), 'append', [ '\x3cdiv\x3einit text\x3c/div\x3e' ]);

// *** unit/core.js ***
//  1645: 	equals( $("div", ibody).length, 2, "Check the original div and the new div are in IFrame" );

$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ 'div', ibody ]), 'length'), 2, 'Check the original div and the new div are in IFrame' ]);

// *** unit/core.js ***
//  1647: 	equals( $("div:last", ibody).text(), "init text", "Add text to div in IFrame" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'div:last', ibody ]), 'text', [ ]), 'init text', 'Add text to div in IFrame' ]);

// *** unit/core.js ***
//  1649: 	$("div:last", ibody).text("div text");

$v.cm($v.cf($v.ro('$'), [ 'div:last', ibody ]), 'text', [ 'div text' ]);

// *** unit/core.js ***
//  1650: 	equals( $("div:last", ibody).text(), "div text", "Add text to div in IFrame" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'div:last', ibody ]), 'text', [ ]), 'div text', 'Add text to div in IFrame' ]);

// *** unit/core.js ***
//  1652: 	$("div:last", ibody).remove();

$v.cm($v.cf($v.ro('$'), [ 'div:last', ibody ]), 'remove', [ ]);

// *** unit/core.js ***
//  1653: 	equals( $("div", ibody).length, 1, "Delete the div and check only one div left in IFrame" );

$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ 'div', ibody ]), 'length'), 1, 'Delete the div and check only one div left in IFrame' ]);

// *** unit/core.js ***
//  1655: 	equals( $("div", ibody).text(), "span text", "Make sure the correct div is still left after deletion in IFrame" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'div', ibody ]), 'text', [ ]), 'span text', 'Make sure the correct div is still left after deletion in IFrame' ]);

// *** unit/core.js ***
//  1657: 	$("<table/>", ibody).append("<tr><td>cell<�td><�tr>").appendTo(ibody);

$v.cm($v.cm($v.cf($v.ro('$'), [ '\x3ctable/\x3e', ibody ]), 'append', [ '\x3ctr\x3e\x3ctd\x3ecell\x3c/td\x3e\x3c/tr\x3e' ]), 'appendTo', [ ibody ]);

// *** unit/core.js ***
//  1658: 	$("table", ibody).remove();

$v.cm($v.cf($v.ro('$'), [ 'table', ibody ]), 'remove', [ ]);

// *** unit/core.js ***
//  1659: 	equals( $("div", ibody).length, 1, "Check for JS error on add and delete of a table in IFrame" );

$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ 'div', ibody ]), 'length'), 1, 'Check for JS error on add and delete of a table in IFrame' ]);

// *** unit/core.js ***
//  1662: 	var c = $("#nonnodes").contents().contents();

var c = $v.cm($v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]), 'contents', [ ]);

// *** unit/core.js ***
//  1663: 	equals( c.length, 1, "Check node,textnode,comment contents is just one" );

$v.cf($v.ro('equals'), [ $v.r(c, 'length'), 1, 'Check node,textnode,comment contents is just one' ]);

// *** unit/core.js ***
//  1664: 	equals( c[0].nodeValue, "hi", "Check node,textnode,comment contents is just the one from span" );

$v.cf($v.ro('equals'), [ $v.r($v.r(c, 0), 'nodeValue'), 'hi', 'Check node,textnode,comment contents is just the one from span' ]);
})) ]);

// *** unit/core.js ***
//  1667: test("$.makeArray", function(){

$v.cf($v.ro('test'), [ '$.makeArray', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 15 ]);

// *** unit/core.js ***
//  1670: 	equals( $.makeArray($('html>*'))[0].nodeName, "HEAD", "Pass makeArray a jQuery object" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.ro('$'), 'makeArray', [ $v.cf($v.ro('$'), [ 'html\x3e*' ]) ]), 0), 'nodeName'), 'HEAD', 'Pass makeArray a jQuery object' ]);

// *** unit/core.js ***
//  1672: 	equals( $.makeArray(document.getElementsByName("PWD")).slice(0,1)[0].name, "PWD", "Pass makeArray a nodelist" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cm($v.ro('$'), 'makeArray', [ $v.cm($v.ro('document'), 'getElementsByName', [ 'PWD' ]) ]), 'slice', [ 0, 1 ]), 0), 'name'), 'PWD', 'Pass makeArray a nodelist' ]);
$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.dis(___.frozenFunc(function ($dis) {
var a___ = ___.args(arguments);
return $v.cm($v.ro('$'), 'makeArray', [ Array.slice(a___, 1) ]);

// *** unit/core.js ***
//  1674: 	equals( (function(){ return $.makeArray(arguments); })(1,2).join(""), "12", "Pass makeArray an arguments array" );

})), [ 1, 2 ]), 'join', [ '' ]), '12', 'Pass makeArray an arguments array' ]);

// *** unit/core.js ***
//  1676: 	equals( $.makeArray([1,2,3]).join(""), "123", "Pass makeArray a real array" );

$v.cf($v.ro('equals'), [ $v.cm($v.cm($v.ro('$'), 'makeArray', [ [ 1, 2, 3 ] ]), 'join', [ '' ]), '123', 'Pass makeArray a real array' ]);

// *** unit/core.js ***
//  1678: 	equals( $.makeArray().length, 0, "Pass nothing to makeArray and expect an empty array" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.ro('$'), 'makeArray', [ ]), 'length'), 0, 'Pass nothing to makeArray and expect an empty array' ]);

// *** unit/core.js ***
//  1680: 	equals( $.makeArray( 0 )[0], 0 , "Pass makeArray a number" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.ro('$'), 'makeArray', [ 0 ]), 0), 0, 'Pass makeArray a number' ]);

// *** unit/core.js ***
//  1682: 	equals( $.makeArray( "foo" )[0], "foo", "Pass makeArray a string" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.ro('$'), 'makeArray', [ 'foo' ]), 0), 'foo', 'Pass makeArray a string' ]);

// *** unit/core.js ***
//  1684: 	equals( $.makeArray( true )[0].constructor, Boolean, "Pass makeArray a boolean" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.ro('$'), 'makeArray', [ true ]), 0), 'constructor'), $v.ro('Boolean'), 'Pass makeArray a boolean' ]);

// *** unit/core.js ***
//  1686: 	equals( $.makeArray( document.createElement("div") )[0].nodeName, "DIV", "Pass makeArray a single node" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.ro('$'), 'makeArray', [ $v.cm($v.ro('document'), 'createElement', [ 'div' ]) ]), 0), 'nodeName'), 'DIV', 'Pass makeArray a single node' ]);

// *** unit/core.js ***
//  1688: 	equals( $.makeArray( {length:2, 0:"a", 1:"b"} ).join(""), "ab", "Pass makeArray an array like map (with length)" );

$v.cf($v.ro('equals'), [ $v.cm($v.cm($v.ro('$'), 'makeArray', [ ___.initializeMap([ 'length', 2, '0', 'a', '1', 'b' ]) ]), 'join', [ '' ]), 'ab', 'Pass makeArray an array like map (with length)' ]);

// *** unit/core.js ***
//  1690: 	equals( $.makeArray( document.documentElement.childNodes ).slice(0,1)[0].nodeName, "HEAD", "Pass makeArray a childNodes array" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.cm($v.ro('$'), 'makeArray', [ $v.r($v.r($v.ro('document'), 'documentElement'), 'childNodes') ]), 'slice', [ 0, 1 ]), 0), 'nodeName'), 'HEAD', 'Pass makeArray a childNodes array' ]);
$v.cf($v.ro('equals'), [ $v.cm($v.cm($v.ro('$'), 'makeArray', [ $v.dis(___.frozenFunc(function ($dis) {
return 1;

// *** unit/core.js ***
//  1693: 	equals( $.makeArray( function(){ return 1;} )[0](), 1, "Pass makeArray a function" );

})) ]), 0, [ ]), 1, 'Pass makeArray a function' ]);

// *** unit/core.js ***
//  1695: 	equals( $.makeArray(window)[0], window, "Pass makeArray the window" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.ro('$'), 'makeArray', [ $v.ro('window') ]), 0), $v.ro('window'), 'Pass makeArray the window' ]);

// *** unit/core.js ***
//  1697: 	equals( $.makeArray(/a/)[0].constructor, RegExp, "Pass makeArray a regex" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cm($v.ro('$'), 'makeArray', [ $v.construct(RegExp, [ 'a' ]) ]), 0), 'constructor'), $v.ro('RegExp'), 'Pass makeArray a regex' ]);

// *** unit/core.js ***
//  1699: 	ok( $.makeArray(document.getElementById('form')).length >= 13, "Pass makeArray a form (treat as elements)" );

$v.cf($v.ro('ok'), [ $v.r($v.cm($v.ro('$'), 'makeArray', [ $v.cm($v.ro('document'), 'getElementById', [ 'form' ]) ]), 'length') >= 13, 'Pass makeArray a form (treat as elements)' ]);
})) ]);
}
} catch (ex___) {
___.getNewModuleHandler().handleUncaughtException(ex___, $v.ro('onerror'), 'unit/core.js', '1');
}
IMPORTS___.htmlEmitter___.pc('\n\t');

// *** unit/dimensions.js ***
//     2: 
//     4: 	expect(3);
//     5: 
//     7: 	// set styles
//     8: 	$div.css({
//     9: 		margin: 10,
//    11: 		width: 30
//    12: 	});
//    13: 	
//    17: 	$div.hide();
//    19: 	
//    20: 	// reset styles
//    22: });
//    23: 
//    25: 	expect(3);
//    26: 	
//    28: 	// set styles
//    29: 	$div.css({
//    30: 		margin: 10,
//    32: 		height: 30
//    33: 	});
//    34: 	
//    38: 	$div.hide();
//    40: 	
//    41: 	// reset styles
//    43: });
//    44: 
//    46: 	expect(6);
//    47: 	
//    50: 	
//    60: 	$div.hide();
//    62: 	
//    63: 	// reset styles
//    65: });
//    66: 
//    68: 	expect(6);
//    69: 	
//    72: 	
//    81: 	$div.hide();
//    83: 	
//    84: 	// reset styles
//    86: });

try {
{

// *** unit/dimensions.js ***
//     1: module("dimensions");

$v.cf($v.ro('module'), [ 'dimensions' ]);

// *** unit/dimensions.js ***
//     3: test("innerWidth()", function() {

$v.cf($v.ro('test'), [ 'innerWidth()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);

// *** unit/dimensions.js ***
//     6: 	var $div = $("#nothiddendiv");

var $div = $v.cf($v.ro('$'), [ '#nothiddendiv' ]);

// *** unit/dimensions.js ***
//    10: 		border: "2px solid #fff",

$v.cm($div, 'css', [ ___.initializeMap([ 'margin', 10, 'border', '2px solid #fff', 'width', 30 ]) ]);

// *** unit/dimensions.js ***
//    14: 	equals($div.innerWidth(), 30, "Test with margin and border");

$v.cf($v.ro('equals'), [ $v.cm($div, 'innerWidth', [ ]), 30, 'Test with margin and border' ]);

// *** unit/dimensions.js ***
//    15: 	$div.css("padding", "20px");

$v.cm($div, 'css', [ 'padding', '20px' ]);

// *** unit/dimensions.js ***
//    16: 	equals($div.innerWidth(), 70, "Test with margin, border and padding");

$v.cf($v.ro('equals'), [ $v.cm($div, 'innerWidth', [ ]), 70, 'Test with margin, border and padding' ]);
$v.cm($div, 'hide', [ ]);

// *** unit/dimensions.js ***
//    18: 	equals($div.innerWidth(), 70, "Test hidden div");

$v.cf($v.ro('equals'), [ $v.cm($div, 'innerWidth', [ ]), 70, 'Test hidden div' ]);

// *** unit/dimensions.js ***
//    21: 	$div.css({ display: "", border: "", padding: "", width: "", height: "" });

$v.cm($div, 'css', [ ___.initializeMap([ 'display', '', 'border', '', 'padding', '', 'width', '', 'height', '' ]) ]);
})) ]);

// *** unit/dimensions.js ***
//    24: test("innerHeight()", function() {

$v.cf($v.ro('test'), [ 'innerHeight()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);

// *** unit/dimensions.js ***
//    27: 	var $div = $("#nothiddendiv");

var $div = $v.cf($v.ro('$'), [ '#nothiddendiv' ]);

// *** unit/dimensions.js ***
//    31: 		border: "2px solid #fff",

$v.cm($div, 'css', [ ___.initializeMap([ 'margin', 10, 'border', '2px solid #fff', 'height', 30 ]) ]);

// *** unit/dimensions.js ***
//    35: 	equals($div.innerHeight(), 30, "Test with margin and border");

$v.cf($v.ro('equals'), [ $v.cm($div, 'innerHeight', [ ]), 30, 'Test with margin and border' ]);

// *** unit/dimensions.js ***
//    36: 	$div.css("padding", "20px");

$v.cm($div, 'css', [ 'padding', '20px' ]);

// *** unit/dimensions.js ***
//    37: 	equals($div.innerHeight(), 70, "Test with margin, border and padding");

$v.cf($v.ro('equals'), [ $v.cm($div, 'innerHeight', [ ]), 70, 'Test with margin, border and padding' ]);
$v.cm($div, 'hide', [ ]);

// *** unit/dimensions.js ***
//    39: 	equals($div.innerHeight(), 70, "Test hidden div");

$v.cf($v.ro('equals'), [ $v.cm($div, 'innerHeight', [ ]), 70, 'Test hidden div' ]);

// *** unit/dimensions.js ***
//    42: 	$div.css({ display: "", border: "", padding: "", width: "", height: "" });

$v.cm($div, 'css', [ ___.initializeMap([ 'display', '', 'border', '', 'padding', '', 'width', '', 'height', '' ]) ]);
})) ]);

// *** unit/dimensions.js ***
//    45: test("outerWidth()", function() {

$v.cf($v.ro('test'), [ 'outerWidth()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/dimensions.js ***
//    48: 	var $div = $("#nothiddendiv");

var $div = $v.cf($v.ro('$'), [ '#nothiddendiv' ]);

// *** unit/dimensions.js ***
//    49: 	$div.css("width", 30);

$v.cm($div, 'css', [ 'width', 30 ]);

// *** unit/dimensions.js ***
//    51: 	equals($div.outerWidth(), 30, "Test with only width set");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerWidth', [ ]), 30, 'Test with only width set' ]);

// *** unit/dimensions.js ***
//    52: 	$div.css("padding", "20px");

$v.cm($div, 'css', [ 'padding', '20px' ]);

// *** unit/dimensions.js ***
//    53: 	equals($div.outerWidth(), 70, "Test with padding");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerWidth', [ ]), 70, 'Test with padding' ]);

// *** unit/dimensions.js ***
//    54: 	$div.css("border", "2px solid #fff");

$v.cm($div, 'css', [ 'border', '2px solid #fff' ]);

// *** unit/dimensions.js ***
//    55: 	equals($div.outerWidth(), 74, "Test with padding and border");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerWidth', [ ]), 74, 'Test with padding and border' ]);

// *** unit/dimensions.js ***
//    56: 	$div.css("margin", "10px");

$v.cm($div, 'css', [ 'margin', '10px' ]);

// *** unit/dimensions.js ***
//    57: 	equals($div.outerWidth(), 74, "Test with padding, border and margin without margin option");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerWidth', [ ]), 74, 'Test with padding, border and margin without margin option' ]);

// *** unit/dimensions.js ***
//    58: 	$div.css("position", "absolute");

$v.cm($div, 'css', [ 'position', 'absolute' ]);

// *** unit/dimensions.js ***
//    59: 	equals($div.outerWidth(true), 94, "Test with padding, border and margin with margin option");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerWidth', [ true ]), 94, 'Test with padding, border and margin with margin option' ]);
$v.cm($div, 'hide', [ ]);

// *** unit/dimensions.js ***
//    61: 	equals($div.outerWidth(true), 94, "Test hidden div with padding, border and margin with margin option");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerWidth', [ true ]), 94, 'Test hidden div with padding, border and margin with margin option' ]);

// *** unit/dimensions.js ***
//    64: 	$div.css({ position: "", display: "", border: "", padding: "", width: "", height: "" });

$v.cm($div, 'css', [ ___.initializeMap([ 'position', '', 'display', '', 'border', '', 'padding', '', 'width', '', 'height', '' ]) ]);
})) ]);

// *** unit/dimensions.js ***
//    67: test("outerHeight()", function() {

$v.cf($v.ro('test'), [ 'outerHeight()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/dimensions.js ***
//    70: 	var $div = $("#nothiddendiv");

var $div = $v.cf($v.ro('$'), [ '#nothiddendiv' ]);

// *** unit/dimensions.js ***
//    71: 	$div.css("height", 30);

$v.cm($div, 'css', [ 'height', 30 ]);

// *** unit/dimensions.js ***
//    73: 	equals($div.outerHeight(), 30, "Test with only width set");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerHeight', [ ]), 30, 'Test with only width set' ]);

// *** unit/dimensions.js ***
//    74: 	$div.css("padding", "20px");

$v.cm($div, 'css', [ 'padding', '20px' ]);

// *** unit/dimensions.js ***
//    75: 	equals($div.outerHeight(), 70, "Test with padding");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerHeight', [ ]), 70, 'Test with padding' ]);

// *** unit/dimensions.js ***
//    76: 	$div.css("border", "2px solid #fff");

$v.cm($div, 'css', [ 'border', '2px solid #fff' ]);

// *** unit/dimensions.js ***
//    77: 	equals($div.outerHeight(), 74, "Test with padding and border");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerHeight', [ ]), 74, 'Test with padding and border' ]);

// *** unit/dimensions.js ***
//    78: 	$div.css("margin", "10px");

$v.cm($div, 'css', [ 'margin', '10px' ]);

// *** unit/dimensions.js ***
//    79: 	equals($div.outerHeight(), 74, "Test with padding, border and margin without margin option");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerHeight', [ ]), 74, 'Test with padding, border and margin without margin option' ]);

// *** unit/dimensions.js ***
//    80: 	equals($div.outerHeight(true), 94, "Test with padding, border and margin with margin option");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerHeight', [ true ]), 94, 'Test with padding, border and margin with margin option' ]);
$v.cm($div, 'hide', [ ]);

// *** unit/dimensions.js ***
//    82: 	equals($div.outerHeight(true), 94, "Test hidden div with padding, border and margin with margin option");

$v.cf($v.ro('equals'), [ $v.cm($div, 'outerHeight', [ true ]), 94, 'Test hidden div with padding, border and margin with margin option' ]);

// *** unit/dimensions.js ***
//    85: 	$div.css({ display: "", border: "", padding: "", width: "", height: "" });

$v.cm($div, 'css', [ ___.initializeMap([ 'display', '', 'border', '', 'padding', '', 'width', '', 'height', '' ]) ]);
})) ]);
}
} catch (ex___) {
___.getNewModuleHandler().handleUncaughtException(ex___, $v.ro('onerror'), 'unit/dimensions.js', '1');
}
IMPORTS___.htmlEmitter___.pc('\n\t');

// *** unit/selector.js ***
//     2: 
//     3: test("element", function() {
//     4: 	expect(9);
//    16: 	
//    19: });
//    20: 
//    31: 
//    32: test("broken", function() {
//    33: 	expect(7);
//    41: });
//    42: 
//    43: test("id", function() {
//    44: 	expect(25);
//    54: 	
//    61: 	
//    65: 	
//    68: 	
//    73: 	
//    75: 
//    77: });
//    78: 
//    79: test("class", function() {
//    80: 	expect(16);
//    85: 	
//    92: 	
//    99: });
//   100: 
//   101: test("multiple", function() {
//   102: 	expect(4);
//   107: });
//   108: 
//   110: 	expect(37);
//   124: 	
//   127: 	
//   130: 	
//   133: 
//   152: });
//   153: 
//   154: test("attributes", function() {
//   155: 	expect(20);
//   159: 	
//   166: 	
//   168: 	
//   172: 	
//   176: 	
//   178: 	
//   182: });
//   183: 
//   185: 	expect(35);
//   201: 	
//   213: 	
//   221: 
//   224: });

try {
{

// *** unit/selector.js ***
//     1: module("selector");

$v.cf($v.ro('module'), [ 'selector' ]);
$v.cf($v.ro('test'), [ 'element', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 9 ]);

// *** unit/selector.js ***
//     5: 	ok( $("*").size() >= 30, "Select all" );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ '*' ]), 'size', [ ]) >= 30, 'Select all' ]);

// *** unit/selector.js ***
//     6: 	var all = $("*"), good = true;

var all = $v.cf($v.ro('$'), [ '*' ]), good = true;

// *** unit/selector.js ***
//     7: 	for ( var i = 0; i < all.length; i++ )
//     8: 		if ( all[i].nodeType == 8 )
//     9: 			good = false;

for (var i = 0; i < $v.r(all, 'length'); i++) if ($v.r($v.r(all, i), 'nodeType') == 8) good = false;

// *** unit/selector.js ***
//    10: 	ok( good, "Select all elements, no comment nodes" );

$v.cf($v.ro('ok'), [ good, 'Select all elements, no comment nodes' ]);

// *** unit/selector.js ***
//    11: 	t( "Element Selector", "p", ["firstp","ap","sndp","en","sap","first"] );

$v.cf($v.ro('t'), [ 'Element Selector', 'p', [ 'firstp', 'ap', 'sndp', 'en', 'sap', 'first' ] ]);

// *** unit/selector.js ***
//    12: 	t( "Element Selector", "body", ["body"] );

$v.cf($v.ro('t'), [ 'Element Selector', 'body', [ 'body' ] ]);

// *** unit/selector.js ***
//    13: 	t( "Element Selector", "html", ["html"] );

$v.cf($v.ro('t'), [ 'Element Selector', 'html', [ 'html' ] ]);

// *** unit/selector.js ***
//    14: 	t( "Parent Element", "div p", ["firstp","ap","sndp","en","sap","first"] );

$v.cf($v.ro('t'), [ 'Parent Element', 'div p', [ 'firstp', 'ap', 'sndp', 'en', 'sap', 'first' ] ]);

// *** unit/selector.js ***
//    15: 	equals( $("param", "#object1").length, 2, "Object/param as context" );

$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ 'param', '#object1' ]), 'length'), 2, 'Object/param as context' ]);

// *** unit/selector.js ***
//    17: 	ok( $("#length").length, '&lt;input name="length"&gt; cannot be found under IE, see #945' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#length' ]), 'length'), '\x26lt;input name=\"length\"\x26gt; cannot be found under IE, see #945' ]);

// *** unit/selector.js ***
//    18: 	ok( $("#lengthtest input").length, '&lt;input name="length"&gt; cannot be found under IE, see #945' );

$v.cf($v.ro('ok'), [ $v.r($v.cf($v.ro('$'), [ '#lengthtest input' ]), 'length'), '\x26lt;input name=\"length\"\x26gt; cannot be found under IE, see #945' ]);
})) ]);

// *** unit/selector.js ***
//    21: if ( location.protocol != "file:" ) {
//    23: 		expect(1);
//    24: 		stop();
//    29: 	});
//    30: }

if ($v.r($v.ro('location'), 'protocol') != 'file:') {

// *** unit/selector.js ***
//    22: 	test("Element Selector with underscore", function() {

$v.cf($v.ro('test'), [ 'Element Selector with underscore', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/selector.js ***
//    25: 		$.get("data/with_fries.xml", function(xml) {

$v.cm($v.ro('$'), 'get', [ 'data/with_fries.xml', $v.dis(___.frozenFunc(function ($dis, xml) {

// *** unit/selector.js ***
//    26: 			equals( $("foo_bar", xml).length, 1, "Element Selector with underscore" );

$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ 'foo_bar', xml ]), 'length'), 1, 'Element Selector with underscore' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/selector.js ***
//    27: 			start();

})) ]);

// *** unit/selector.js ***
//    28: 		});

})) ]);
}
$v.cf($v.ro('test'), [ 'broken', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 7 ]);

// *** unit/selector.js ***
//    34: 	t( "Broken Selector", "[", [] );

$v.cf($v.ro('t'), [ 'Broken Selector', '[', [ ] ]);

// *** unit/selector.js ***
//    35: 	t( "Broken Selector", "(", [] );

$v.cf($v.ro('t'), [ 'Broken Selector', '(', [ ] ]);

// *** unit/selector.js ***
//    36: 	t( "Broken Selector", "{", [] );

$v.cf($v.ro('t'), [ 'Broken Selector', '{', [ ] ]);

// *** unit/selector.js ***
//    37: 	t( "Broken Selector", "<", [] );

$v.cf($v.ro('t'), [ 'Broken Selector', '\x3c', [ ] ]);

// *** unit/selector.js ***
//    38: 	t( "Broken Selector", "()", [] );

$v.cf($v.ro('t'), [ 'Broken Selector', '()', [ ] ]);

// *** unit/selector.js ***
//    39: 	t( "Broken Selector", "<>", [] );

$v.cf($v.ro('t'), [ 'Broken Selector', '\x3c\x3e', [ ] ]);
$v.cf($v.ro('t'), [ 'Broken Selector', '{}', [ ] ]);

// *** unit/selector.js ***
//    40: 	t( "Broken Selector", "{}", [] );

})) ]);
$v.cf($v.ro('test'), [ 'id', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 25 ]);

// *** unit/selector.js ***
//    45: 	t( "ID Selector", "#body", ["body"] );

$v.cf($v.ro('t'), [ 'ID Selector', '#body', [ 'body' ] ]);

// *** unit/selector.js ***
//    46: 	t( "ID Selector w/ Element", "body#body", ["body"] );

$v.cf($v.ro('t'), [ 'ID Selector w/ Element', 'body#body', [ 'body' ] ]);

// *** unit/selector.js ***
//    47: 	t( "ID Selector w/ Element", "ul#first", [] );

$v.cf($v.ro('t'), [ 'ID Selector w/ Element', 'ul#first', [ ] ]);

// *** unit/selector.js ***
//    48: 	t( "ID selector with existing ID descendant", "#firstp #simon1", ["simon1"] );

$v.cf($v.ro('t'), [ 'ID selector with existing ID descendant', '#firstp #simon1', [ 'simon1' ] ]);

// *** unit/selector.js ***
//    49: 	t( "ID selector with non-existant descendant", "#firstp #foobar", [] );

$v.cf($v.ro('t'), [ 'ID selector with non-existant descendant', '#firstp #foobar', [ ] ]);

// *** unit/selector.js ***
//    50: 	t( "ID selector using UTF8", "#台北Táiběi", ["台北Táiběi"] );

$v.cf($v.ro('t'), [ 'ID selector using UTF8', '#\u53f0\u5317Ta\u0301ibe\u030ci', [ '\u53f0\u5317Ta\u0301ibe\u030ci' ] ]);

// *** unit/selector.js ***
//    51: 	t( "Multiple ID selectors using UTF8", "#台北Táiběi, #台北", ["台北Táiběi","台北"] );

$v.cf($v.ro('t'), [ 'Multiple ID selectors using UTF8', '#\u53f0\u5317Ta\u0301ibe\u030ci, #\u53f0\u5317', [ '\u53f0\u5317Ta\u0301ibe\u030ci', '\u53f0\u5317' ] ]);

// *** unit/selector.js ***
//    52: 	t( "Descendant ID selector using UTF8", "div #台北", ["台北"] );

$v.cf($v.ro('t'), [ 'Descendant ID selector using UTF8', 'div #\u53f0\u5317', [ '\u53f0\u5317' ] ]);

// *** unit/selector.js ***
//    53: 	t( "Child ID selector using UTF8", "form > #台北", ["台北"] );

$v.cf($v.ro('t'), [ 'Child ID selector using UTF8', 'form \x3e #\u53f0\u5317', [ '\u53f0\u5317' ] ]);

// *** unit/selector.js ***
//    55: 	t( "Escaped ID", "#foo\\:bar", ["foo:bar"] );

$v.cf($v.ro('t'), [ 'Escaped ID', '#foo\\:bar', [ 'foo:bar' ] ]);

// *** unit/selector.js ***
//    56: 	t( "Escaped ID", "#test\\.foo\\[5\\]bar", ["test.foo[5]bar"] );

$v.cf($v.ro('t'), [ 'Escaped ID', '#test\\.foo\\[5\\]bar', [ 'test.foo[5]bar' ] ]);

// *** unit/selector.js ***
//    57: 	t( "Descendant escaped ID", "div #foo\\:bar", ["foo:bar"] );

$v.cf($v.ro('t'), [ 'Descendant escaped ID', 'div #foo\\:bar', [ 'foo:bar' ] ]);

// *** unit/selector.js ***
//    58: 	t( "Descendant escaped ID", "div #test\\.foo\\[5\\]bar", ["test.foo[5]bar"] );

$v.cf($v.ro('t'), [ 'Descendant escaped ID', 'div #test\\.foo\\[5\\]bar', [ 'test.foo[5]bar' ] ]);

// *** unit/selector.js ***
//    59: 	t( "Child escaped ID", "form > #foo\\:bar", ["foo:bar"] );

$v.cf($v.ro('t'), [ 'Child escaped ID', 'form \x3e #foo\\:bar', [ 'foo:bar' ] ]);

// *** unit/selector.js ***
//    60: 	t( "Child escaped ID", "form > #test\\.foo\\[5\\]bar", ["test.foo[5]bar"] );

$v.cf($v.ro('t'), [ 'Child escaped ID', 'form \x3e #test\\.foo\\[5\\]bar', [ 'test.foo[5]bar' ] ]);

// *** unit/selector.js ***
//    62: 	t( "ID Selector, child ID present", "#form > #radio1", ["radio1"] ); // bug #267

$v.cf($v.ro('t'), [ 'ID Selector, child ID present', '#form \x3e #radio1', [ 'radio1' ] ]);

// *** unit/selector.js ***
//    63: 	t( "ID Selector, not an ancestor ID", "#form #first", [] );

$v.cf($v.ro('t'), [ 'ID Selector, not an ancestor ID', '#form #first', [ ] ]);

// *** unit/selector.js ***
//    64: 	t( "ID Selector, not a child ID", "#form > #option1a", [] );

$v.cf($v.ro('t'), [ 'ID Selector, not a child ID', '#form \x3e #option1a', [ ] ]);

// *** unit/selector.js ***
//    66: 	t( "All Children of ID", "#foo > *", ["sndp", "en", "sap"] );

$v.cf($v.ro('t'), [ 'All Children of ID', '#foo \x3e *', [ 'sndp', 'en', 'sap' ] ]);

// *** unit/selector.js ***
//    67: 	t( "All Children of ID with no children", "#firstUL/*", [] );

$v.cf($v.ro('t'), [ 'All Children of ID with no children', '#firstUL/*', [ ] ]);

// *** unit/selector.js ***
//    69: 	$('<a name="tName1">tName1 A<�a><a name="tName2">tName2 A<�a><div id="tName1">tName1 Div<�div>').appendTo('#main');

$v.cm($v.cf($v.ro('$'), [ '\x3ca name=\"tName1\"\x3etName1 A\x3c/a\x3e\x3ca name=\"tName2\"\x3etName2 A\x3c/a\x3e\x3cdiv id=\"tName1\"\x3etName1 Div\x3c/div\x3e' ]), 'appendTo', [ '#main' ]);

// *** unit/selector.js ***
//    70: 	equals( $("#tName1")[0].id, 'tName1', "ID selector with same value for a name attribute" );

$v.cf($v.ro('equals'), [ $v.r($v.r($v.cf($v.ro('$'), [ '#tName1' ]), 0), 'id'), 'tName1', 'ID selector with same value for a name attribute' ]);

// *** unit/selector.js ***
//    71: 	equals( $("#tName2").length, 0, "ID selector non-existing but name attribute on an A tag" );

$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ '#tName2' ]), 'length'), 0, 'ID selector non-existing but name attribute on an A tag' ]);

// *** unit/selector.js ***
//    72: 	t( "ID Selector on Form with an input that has a name of 'id'", "#lengthtest", ["lengthtest"] );

$v.cf($v.ro('t'), [ 'ID Selector on Form with an input that has a name of \'id\'', '#lengthtest', [ 'lengthtest' ] ]);

// *** unit/selector.js ***
//    74: 	t( "ID selector with non-existant ancestor", "#asdfasdf #foobar", [] ); // bug #986

$v.cf($v.ro('t'), [ 'ID selector with non-existant ancestor', '#asdfasdf #foobar', [ ] ]);

// *** unit/selector.js ***
//    76: 	isSet( $("body").find("div#form"), [], "ID selector within the context of another element" );

$v.cf($v.ro('isSet'), [ $v.cm($v.cf($v.ro('$'), [ 'body' ]), 'find', [ 'div#form' ]), [ ], 'ID selector within the context of another element' ]);
})) ]);
$v.cf($v.ro('test'), [ 'class', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 16 ]);

// *** unit/selector.js ***
//    81: 	t( "Class Selector", ".blog", ["mark","simon"] );

$v.cf($v.ro('t'), [ 'Class Selector', '.blog', [ 'mark', 'simon' ] ]);

// *** unit/selector.js ***
//    82: 	t( "Class Selector", ".blog.link", ["simon"] );

$v.cf($v.ro('t'), [ 'Class Selector', '.blog.link', [ 'simon' ] ]);

// *** unit/selector.js ***
//    83: 	t( "Class Selector w/ Element", "a.blog", ["mark","simon"] );

$v.cf($v.ro('t'), [ 'Class Selector w/ Element', 'a.blog', [ 'mark', 'simon' ] ]);

// *** unit/selector.js ***
//    84: 	t( "Parent Class Selector", "p .blog", ["mark","simon"] );

$v.cf($v.ro('t'), [ 'Parent Class Selector', 'p .blog', [ 'mark', 'simon' ] ]);

// *** unit/selector.js ***
//    86: 	t( "Class selector using UTF8", ".台北Táiběi", ["utf8class1"] );

$v.cf($v.ro('t'), [ 'Class selector using UTF8', '.\u53f0\u5317Ta\u0301ibe\u030ci', [ 'utf8class1' ] ]);

// *** unit/selector.js ***
//    87: 	t( "Class selector using UTF8", ".台北", ["utf8class1","utf8class2"] );

$v.cf($v.ro('t'), [ 'Class selector using UTF8', '.\u53f0\u5317', [ 'utf8class1', 'utf8class2' ] ]);

// *** unit/selector.js ***
//    88: 	t( "Class selector using UTF8", ".台北Táiběi.台北", ["utf8class1"] );

$v.cf($v.ro('t'), [ 'Class selector using UTF8', '.\u53f0\u5317Ta\u0301ibe\u030ci.\u53f0\u5317', [ 'utf8class1' ] ]);

// *** unit/selector.js ***
//    89: 	t( "Class selector using UTF8", ".台北Táiběi, .台北", ["utf8class1","utf8class2"] );

$v.cf($v.ro('t'), [ 'Class selector using UTF8', '.\u53f0\u5317Ta\u0301ibe\u030ci, .\u53f0\u5317', [ 'utf8class1', 'utf8class2' ] ]);

// *** unit/selector.js ***
//    90: 	t( "Descendant class selector using UTF8", "div .台北Táiběi", ["utf8class1"] );

$v.cf($v.ro('t'), [ 'Descendant class selector using UTF8', 'div .\u53f0\u5317Ta\u0301ibe\u030ci', [ 'utf8class1' ] ]);

// *** unit/selector.js ***
//    91: 	t( "Child class selector using UTF8", "form > .台北Táiběi", ["utf8class1"] );

$v.cf($v.ro('t'), [ 'Child class selector using UTF8', 'form \x3e .\u53f0\u5317Ta\u0301ibe\u030ci', [ 'utf8class1' ] ]);

// *** unit/selector.js ***
//    93: 	t( "Escaped Class", ".foo\\:bar", ["foo:bar"] );

$v.cf($v.ro('t'), [ 'Escaped Class', '.foo\\:bar', [ 'foo:bar' ] ]);

// *** unit/selector.js ***
//    94: 	t( "Escaped Class", ".test\\.foo\\[5\\]bar", ["test.foo[5]bar"] );

$v.cf($v.ro('t'), [ 'Escaped Class', '.test\\.foo\\[5\\]bar', [ 'test.foo[5]bar' ] ]);

// *** unit/selector.js ***
//    95: 	t( "Descendant scaped Class", "div .foo\\:bar", ["foo:bar"] );

$v.cf($v.ro('t'), [ 'Descendant scaped Class', 'div .foo\\:bar', [ 'foo:bar' ] ]);

// *** unit/selector.js ***
//    96: 	t( "Descendant scaped Class", "div .test\\.foo\\[5\\]bar", ["test.foo[5]bar"] );

$v.cf($v.ro('t'), [ 'Descendant scaped Class', 'div .test\\.foo\\[5\\]bar', [ 'test.foo[5]bar' ] ]);

// *** unit/selector.js ***
//    97: 	t( "Child escaped Class", "form > .foo\\:bar", ["foo:bar"] );

$v.cf($v.ro('t'), [ 'Child escaped Class', 'form \x3e .foo\\:bar', [ 'foo:bar' ] ]);

// *** unit/selector.js ***
//    98: 	t( "Child escaped Class", "form > .test\\.foo\\[5\\]bar", ["test.foo[5]bar"] );

$v.cf($v.ro('t'), [ 'Child escaped Class', 'form \x3e .test\\.foo\\[5\\]bar', [ 'test.foo[5]bar' ] ]);
})) ]);
$v.cf($v.ro('test'), [ 'multiple', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/selector.js ***
//   103: 	t( "Comma Support", "a.blog, p", ["mark","simon","firstp","ap","sndp","en","sap","first"] );

$v.cf($v.ro('t'), [ 'Comma Support', 'a.blog, p', [ 'mark', 'simon', 'firstp', 'ap', 'sndp', 'en', 'sap', 'first' ] ]);

// *** unit/selector.js ***
//   104: 	t( "Comma Support", "a.blog , p", ["mark","simon","firstp","ap","sndp","en","sap","first"] );

$v.cf($v.ro('t'), [ 'Comma Support', 'a.blog , p', [ 'mark', 'simon', 'firstp', 'ap', 'sndp', 'en', 'sap', 'first' ] ]);

// *** unit/selector.js ***
//   105: 	t( "Comma Support", "a.blog ,p", ["mark","simon","firstp","ap","sndp","en","sap","first"] );

$v.cf($v.ro('t'), [ 'Comma Support', 'a.blog ,p', [ 'mark', 'simon', 'firstp', 'ap', 'sndp', 'en', 'sap', 'first' ] ]);

// *** unit/selector.js ***
//   106: 	t( "Comma Support", "a.blog,p", ["mark","simon","firstp","ap","sndp","en","sap","first"] );

$v.cf($v.ro('t'), [ 'Comma Support', 'a.blog,p', [ 'mark', 'simon', 'firstp', 'ap', 'sndp', 'en', 'sap', 'first' ] ]);
})) ]);

// *** unit/selector.js ***
//   109: test("child and adjacent", function() {

$v.cf($v.ro('test'), [ 'child and adjacent', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 37 ]);

// *** unit/selector.js ***
//   111: 	t( "Child", "p > a", ["simon1","google","groups","mark","yahoo","simon"] );

$v.cf($v.ro('t'), [ 'Child', 'p \x3e a', [ 'simon1', 'google', 'groups', 'mark', 'yahoo', 'simon' ] ]);

// *** unit/selector.js ***
//   112: 	t( "Child", "p> a", ["simon1","google","groups","mark","yahoo","simon"] );

$v.cf($v.ro('t'), [ 'Child', 'p\x3e a', [ 'simon1', 'google', 'groups', 'mark', 'yahoo', 'simon' ] ]);

// *** unit/selector.js ***
//   113: 	t( "Child", "p >a", ["simon1","google","groups","mark","yahoo","simon"] );

$v.cf($v.ro('t'), [ 'Child', 'p \x3ea', [ 'simon1', 'google', 'groups', 'mark', 'yahoo', 'simon' ] ]);

// *** unit/selector.js ***
//   114: 	t( "Child", "p>a", ["simon1","google","groups","mark","yahoo","simon"] );

$v.cf($v.ro('t'), [ 'Child', 'p\x3ea', [ 'simon1', 'google', 'groups', 'mark', 'yahoo', 'simon' ] ]);

// *** unit/selector.js ***
//   115: 	t( "Child w/ Class", "p > a.blog", ["mark","simon"] );

$v.cf($v.ro('t'), [ 'Child w/ Class', 'p \x3e a.blog', [ 'mark', 'simon' ] ]);

// *** unit/selector.js ***
//   116: 	t( "All Children", "code > *", ["anchor1","anchor2"] );

$v.cf($v.ro('t'), [ 'All Children', 'code \x3e *', [ 'anchor1', 'anchor2' ] ]);

// *** unit/selector.js ***
//   117: 	t( "All Grandchildren", "p > * > *", ["anchor1","anchor2"] );

$v.cf($v.ro('t'), [ 'All Grandchildren', 'p \x3e * \x3e *', [ 'anchor1', 'anchor2' ] ]);

// *** unit/selector.js ***
//   118: 	t( "Adjacent", "a + a", ["groups"] );

$v.cf($v.ro('t'), [ 'Adjacent', 'a + a', [ 'groups' ] ]);

// *** unit/selector.js ***
//   119: 	t( "Adjacent", "a +a", ["groups"] );

$v.cf($v.ro('t'), [ 'Adjacent', 'a +a', [ 'groups' ] ]);

// *** unit/selector.js ***
//   120: 	t( "Adjacent", "a+ a", ["groups"] );

$v.cf($v.ro('t'), [ 'Adjacent', 'a+ a', [ 'groups' ] ]);

// *** unit/selector.js ***
//   121: 	t( "Adjacent", "a+a", ["groups"] );

$v.cf($v.ro('t'), [ 'Adjacent', 'a+a', [ 'groups' ] ]);

// *** unit/selector.js ***
//   122: 	t( "Adjacent", "p + p", ["ap","en","sap"] );

$v.cf($v.ro('t'), [ 'Adjacent', 'p + p', [ 'ap', 'en', 'sap' ] ]);

// *** unit/selector.js ***
//   123: 	t( "Comma, Child, and Adjacent", "a + a, code > a", ["groups","anchor1","anchor2"] );

$v.cf($v.ro('t'), [ 'Comma, Child, and Adjacent', 'a + a, code \x3e a', [ 'groups', 'anchor1', 'anchor2' ] ]);

// *** unit/selector.js ***
//   125: 	t( "First Child", "p:first-child", ["firstp","sndp"] );

$v.cf($v.ro('t'), [ 'First Child', 'p:first-child', [ 'firstp', 'sndp' ] ]);

// *** unit/selector.js ***
//   126: 	t( "Nth Child", "p:nth-child(1)", ["firstp","sndp"] );

$v.cf($v.ro('t'), [ 'Nth Child', 'p:nth-child(1)', [ 'firstp', 'sndp' ] ]);

// *** unit/selector.js ***
//   128: 	t( "Last Child", "p:last-child", ["sap"] );

$v.cf($v.ro('t'), [ 'Last Child', 'p:last-child', [ 'sap' ] ]);

// *** unit/selector.js ***
//   129: 	t( "Last Child", "a:last-child", ["simon1","anchor1","mark","yahoo","anchor2","simon"] );

$v.cf($v.ro('t'), [ 'Last Child', 'a:last-child', [ 'simon1', 'anchor1', 'mark', 'yahoo', 'anchor2', 'simon' ] ]);

// *** unit/selector.js ***
//   131: 	t( "Nth-child", "#main form#form > *:nth-child(2)", ["text2"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#main form#form \x3e *:nth-child(2)', [ 'text2' ] ]);

// *** unit/selector.js ***
//   132: 	t( "Nth-child", "#main form#form > :nth-child(2)", ["text2"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#main form#form \x3e :nth-child(2)', [ 'text2' ] ]);

// *** unit/selector.js ***
//   134: 	t( "Nth-child", "#form select:first option:nth-child(3)", ["option1c"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(3)', [ 'option1c' ] ]);

// *** unit/selector.js ***
//   135: 	t( "Nth-child", "#form select:first option:nth-child(0n+3)", ["option1c"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(0n+3)', [ 'option1c' ] ]);

// *** unit/selector.js ***
//   136: 	t( "Nth-child", "#form select:first option:nth-child(1n+0)", ["option1a", "option1b", "option1c", "option1d"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(1n+0)', [ 'option1a', 'option1b', 'option1c', 'option1d' ] ]);

// *** unit/selector.js ***
//   137: 	t( "Nth-child", "#form select:first option:nth-child(1n)", ["option1a", "option1b", "option1c", "option1d"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(1n)', [ 'option1a', 'option1b', 'option1c', 'option1d' ] ]);

// *** unit/selector.js ***
//   138: 	t( "Nth-child", "#form select:first option:nth-child(n)", ["option1a", "option1b", "option1c", "option1d"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(n)', [ 'option1a', 'option1b', 'option1c', 'option1d' ] ]);

// *** unit/selector.js ***
//   139: 	t( "Nth-child", "#form select:first option:nth-child(even)", ["option1b", "option1d"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(even)', [ 'option1b', 'option1d' ] ]);

// *** unit/selector.js ***
//   140: 	t( "Nth-child", "#form select:first option:nth-child(odd)", ["option1a", "option1c"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(odd)', [ 'option1a', 'option1c' ] ]);

// *** unit/selector.js ***
//   141: 	t( "Nth-child", "#form select:first option:nth-child(2n)", ["option1b", "option1d"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(2n)', [ 'option1b', 'option1d' ] ]);

// *** unit/selector.js ***
//   142: 	t( "Nth-child", "#form select:first option:nth-child(2n+1)", ["option1a", "option1c"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(2n+1)', [ 'option1a', 'option1c' ] ]);

// *** unit/selector.js ***
//   143: 	t( "Nth-child", "#form select:first option:nth-child(3n)", ["option1c"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(3n)', [ 'option1c' ] ]);

// *** unit/selector.js ***
//   144: 	t( "Nth-child", "#form select:first option:nth-child(3n+1)", ["option1a", "option1d"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(3n+1)', [ 'option1a', 'option1d' ] ]);

// *** unit/selector.js ***
//   145: 	t( "Nth-child", "#form select:first option:nth-child(3n+2)", ["option1b"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(3n+2)', [ 'option1b' ] ]);

// *** unit/selector.js ***
//   146: 	t( "Nth-child", "#form select:first option:nth-child(3n+3)", ["option1c"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(3n+3)', [ 'option1c' ] ]);

// *** unit/selector.js ***
//   147: 	t( "Nth-child", "#form select:first option:nth-child(3n-1)", ["option1b"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(3n-1)', [ 'option1b' ] ]);

// *** unit/selector.js ***
//   148: 	t( "Nth-child", "#form select:first option:nth-child(3n-2)", ["option1a", "option1d"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(3n-2)', [ 'option1a', 'option1d' ] ]);

// *** unit/selector.js ***
//   149: 	t( "Nth-child", "#form select:first option:nth-child(3n-3)", ["option1c"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(3n-3)', [ 'option1c' ] ]);

// *** unit/selector.js ***
//   150: 	t( "Nth-child", "#form select:first option:nth-child(3n+0)", ["option1c"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(3n+0)', [ 'option1c' ] ]);

// *** unit/selector.js ***
//   151: 	t( "Nth-child", "#form select:first option:nth-child(-n+3)", ["option1a", "option1b", "option1c"] );

$v.cf($v.ro('t'), [ 'Nth-child', '#form select:first option:nth-child(-n+3)', [ 'option1a', 'option1b', 'option1c' ] ]);
})) ]);
$v.cf($v.ro('test'), [ 'attributes', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 20 ]);

// *** unit/selector.js ***
//   156: 	t( "Attribute Exists", "a[title]", ["google"] );

$v.cf($v.ro('t'), [ 'Attribute Exists', 'a[title]', [ 'google' ] ]);

// *** unit/selector.js ***
//   157: 	t( "Attribute Exists", "*[title]", ["google"] );

$v.cf($v.ro('t'), [ 'Attribute Exists', '*[title]', [ 'google' ] ]);

// *** unit/selector.js ***
//   158: 	t( "Attribute Exists", "[title]", ["google"] );

$v.cf($v.ro('t'), [ 'Attribute Exists', '[title]', [ 'google' ] ]);

// *** unit/selector.js ***
//   160: 	t( "Attribute Equals", "a[rel='bookmark']", ["simon1"] );

$v.cf($v.ro('t'), [ 'Attribute Equals', 'a[rel=\'bookmark\']', [ 'simon1' ] ]);

// *** unit/selector.js ***
//   161: 	t( "Attribute Equals", 'a[rel="bookmark"]', ["simon1"] );

$v.cf($v.ro('t'), [ 'Attribute Equals', 'a[rel=\"bookmark\"]', [ 'simon1' ] ]);

// *** unit/selector.js ***
//   162: 	t( "Attribute Equals", "a[rel=bookmark]", ["simon1"] );

$v.cf($v.ro('t'), [ 'Attribute Equals', 'a[rel=bookmark]', [ 'simon1' ] ]);

// *** unit/selector.js ***
//   163: 	t( "Multiple Attribute Equals", "#form input[type='hidden'],#form input[type='radio']", ["hidden1","radio1","radio2"] );

$v.cf($v.ro('t'), [ 'Multiple Attribute Equals', '#form input[type=\'hidden\'],#form input[type=\'radio\']', [ 'hidden1', 'radio1', 'radio2' ] ]);

// *** unit/selector.js ***
//   164: 	t( "Multiple Attribute Equals", "#form input[type=\"hidden\"],#form input[type='radio']", ["hidden1","radio1","radio2"] );

$v.cf($v.ro('t'), [ 'Multiple Attribute Equals', '#form input[type=\"hidden\"],#form input[type=\'radio\']', [ 'hidden1', 'radio1', 'radio2' ] ]);

// *** unit/selector.js ***
//   165: 	t( "Multiple Attribute Equals", "#form input[type=hidden],#form input[type=radio]", ["hidden1","radio1","radio2"] );

$v.cf($v.ro('t'), [ 'Multiple Attribute Equals', '#form input[type=hidden],#form input[type=radio]', [ 'hidden1', 'radio1', 'radio2' ] ]);

// *** unit/selector.js ***
//   167: 	t( "Attribute selector using UTF8", "span[lang=中文]", ["台北"] );

$v.cf($v.ro('t'), [ 'Attribute selector using UTF8', 'span[lang=\u4e2d\u6587]', [ '\u53f0\u5317' ] ]);

// *** unit/selector.js ***
//   169: 	t( "Attribute Begins With", "a[href ^= 'http://www']", ["google","yahoo"] );

$v.cf($v.ro('t'), [ 'Attribute Begins With', 'a[href ^= \'http://www\']', [ 'google', 'yahoo' ] ]);

// *** unit/selector.js ***
//   170: 	t( "Attribute Ends With", "a[href $= 'org/']", ["mark"] );

$v.cf($v.ro('t'), [ 'Attribute Ends With', 'a[href $= \'org/\']', [ 'mark' ] ]);

// *** unit/selector.js ***
//   171: 	t( "Attribute Contains", "a[href *= 'google']", ["google","groups"] );

$v.cf($v.ro('t'), [ 'Attribute Contains', 'a[href *= \'google\']', [ 'google', 'groups' ] ]);

// *** unit/selector.js ***
//   173: 	t("Select options via [selected]", "#select1 option[selected]", ["option1a"] );

$v.cf($v.ro('t'), [ 'Select options via [selected]', '#select1 option[selected]', [ 'option1a' ] ]);

// *** unit/selector.js ***
//   174: 	t("Select options via [selected]", "#select2 option[selected]", ["option2d"] );

$v.cf($v.ro('t'), [ 'Select options via [selected]', '#select2 option[selected]', [ 'option2d' ] ]);

// *** unit/selector.js ***
//   175: 	t("Select options via [selected]", "#select3 option[selected]", ["option3b", "option3c"] );

$v.cf($v.ro('t'), [ 'Select options via [selected]', '#select3 option[selected]', [ 'option3b', 'option3c' ] ]);

// *** unit/selector.js ***
//   177: 	t( "Grouped Form Elements", "input[name='foo[bar]']", ["hidden2"] );

$v.cf($v.ro('t'), [ 'Grouped Form Elements', 'input[name=\'foo[bar]\']', [ 'hidden2' ] ]);

// *** unit/selector.js ***
//   179: 	t( ":not() Existing attribute", "#form select:not([multiple])", ["select1", "select2"]);

$v.cf($v.ro('t'), [ ':not() Existing attribute', '#form select:not([multiple])', [ 'select1', 'select2' ] ]);

// *** unit/selector.js ***
//   180: 	t( ":not() Equals attribute", "#form select:not([name=select1])", ["select2", "select3"]);

$v.cf($v.ro('t'), [ ':not() Equals attribute', '#form select:not([name=select1])', [ 'select2', 'select3' ] ]);

// *** unit/selector.js ***
//   181: 	t( ":not() Equals quoted attribute", "#form select:not([name='select1'])", ["select2", "select3"]);

$v.cf($v.ro('t'), [ ':not() Equals quoted attribute', '#form select:not([name=\'select1\'])', [ 'select2', 'select3' ] ]);
})) ]);

// *** unit/selector.js ***
//   184: test("pseudo (:) selectors", function() {

$v.cf($v.ro('test'), [ 'pseudo (:) selectors', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 35 ]);

// *** unit/selector.js ***
//   186: 	t( "First Child", "p:first-child", ["firstp","sndp"] );

$v.cf($v.ro('t'), [ 'First Child', 'p:first-child', [ 'firstp', 'sndp' ] ]);

// *** unit/selector.js ***
//   187: 	t( "Last Child", "p:last-child", ["sap"] );

$v.cf($v.ro('t'), [ 'Last Child', 'p:last-child', [ 'sap' ] ]);

// *** unit/selector.js ***
//   188: 	t( "Only Child", "a:only-child", ["simon1","anchor1","yahoo","anchor2"] );

$v.cf($v.ro('t'), [ 'Only Child', 'a:only-child', [ 'simon1', 'anchor1', 'yahoo', 'anchor2' ] ]);

// *** unit/selector.js ***
//   189: 	t( "Empty", "ul:empty", ["firstUL"] );

$v.cf($v.ro('t'), [ 'Empty', 'ul:empty', [ 'firstUL' ] ]);

// *** unit/selector.js ***
//   190: 	t( "Enabled UI Element", "#form input:enabled", ["text1","radio1","radio2","check1","check2","hidden1","hidden2","name"] );

$v.cf($v.ro('t'), [ 'Enabled UI Element', '#form input:enabled', [ 'text1', 'radio1', 'radio2', 'check1', 'check2', 'hidden1', 'hidden2', 'name' ] ]);

// *** unit/selector.js ***
//   191: 	t( "Disabled UI Element", "#form input:disabled", ["text2"] );

$v.cf($v.ro('t'), [ 'Disabled UI Element', '#form input:disabled', [ 'text2' ] ]);

// *** unit/selector.js ***
//   192: 	t( "Checked UI Element", "#form input:checked", ["radio2","check1"] );

$v.cf($v.ro('t'), [ 'Checked UI Element', '#form input:checked', [ 'radio2', 'check1' ] ]);

// *** unit/selector.js ***
//   193: 	t( "Selected Option Element", "#form option:selected", ["option1a","option2d","option3b","option3c"] );

$v.cf($v.ro('t'), [ 'Selected Option Element', '#form option:selected', [ 'option1a', 'option2d', 'option3b', 'option3c' ] ]);

// *** unit/selector.js ***
//   194: 	t( "Text Contains", "a:contains('Google')", ["google","groups"] );

$v.cf($v.ro('t'), [ 'Text Contains', 'a:contains(\'Google\')', [ 'google', 'groups' ] ]);

// *** unit/selector.js ***
//   195: 	t( "Text Contains", "a:contains('Google Groups')", ["groups"] );

$v.cf($v.ro('t'), [ 'Text Contains', 'a:contains(\'Google Groups\')', [ 'groups' ] ]);

// *** unit/selector.js ***
//   196: 	t( "Element Preceded By", "p ~ div", ["foo","fx-queue","fx-tests", "moretests"] );

$v.cf($v.ro('t'), [ 'Element Preceded By', 'p ~ div', [ 'foo', 'fx-queue', 'fx-tests', 'moretests' ] ]);

// *** unit/selector.js ***
//   197: 	t( "Not", "a.blog:not(.link)", ["mark"] );

$v.cf($v.ro('t'), [ 'Not', 'a.blog:not(.link)', [ 'mark' ] ]);

// *** unit/selector.js ***
//   198: 	t( "Not - multiple", "#form option:not(:contains('Nothing'),#option1b,:selected)", ["option1c", "option1d", "option2b", "option2c", "option3d"] );

$v.cf($v.ro('t'), [ 'Not - multiple', '#form option:not(:contains(\'Nothing\'),#option1b,:selected)', [ 'option1c', 'option1d', 'option2b', 'option2c', 'option3d' ] ]);

// *** unit/selector.js ***
//   199: 	t( "Not - complex", "#form option:not([id^='opt']:gt(0):nth-child(-n+3))", [ "option1a", "option1d", "option2d", "option3d"] );

$v.cf($v.ro('t'), [ 'Not - complex', '#form option:not([id^=\'opt\']:gt(0):nth-child(-n+3))', [ 'option1a', 'option1d', 'option2d', 'option3d' ] ]);

// *** unit/selector.js ***
//   200: 	t( "Not - recursive", "#form option:not(:not(:selected))[id^='option3']", [ "option3b", "option3c"] );

$v.cf($v.ro('t'), [ 'Not - recursive', '#form option:not(:not(:selected))[id^=\'option3\']', [ 'option3b', 'option3c' ] ]);

// *** unit/selector.js ***
//   202: 	t( "nth Element", "p:nth(1)", ["ap"] );

$v.cf($v.ro('t'), [ 'nth Element', 'p:nth(1)', [ 'ap' ] ]);

// *** unit/selector.js ***
//   203: 	t( "First Element", "p:first", ["firstp"] );

$v.cf($v.ro('t'), [ 'First Element', 'p:first', [ 'firstp' ] ]);

// *** unit/selector.js ***
//   204: 	t( "Last Element", "p:last", ["first"] );

$v.cf($v.ro('t'), [ 'Last Element', 'p:last', [ 'first' ] ]);

// *** unit/selector.js ***
//   205: 	t( "Even Elements", "p:even", ["firstp","sndp","sap"] );

$v.cf($v.ro('t'), [ 'Even Elements', 'p:even', [ 'firstp', 'sndp', 'sap' ] ]);

// *** unit/selector.js ***
//   206: 	t( "Odd Elements", "p:odd", ["ap","en","first"] );

$v.cf($v.ro('t'), [ 'Odd Elements', 'p:odd', [ 'ap', 'en', 'first' ] ]);

// *** unit/selector.js ***
//   207: 	t( "Position Equals", "p:eq(1)", ["ap"] );

$v.cf($v.ro('t'), [ 'Position Equals', 'p:eq(1)', [ 'ap' ] ]);

// *** unit/selector.js ***
//   208: 	t( "Position Greater Than", "p:gt(0)", ["ap","sndp","en","sap","first"] );

$v.cf($v.ro('t'), [ 'Position Greater Than', 'p:gt(0)', [ 'ap', 'sndp', 'en', 'sap', 'first' ] ]);

// *** unit/selector.js ***
//   209: 	t( "Position Less Than", "p:lt(3)", ["firstp","ap","sndp"] );

$v.cf($v.ro('t'), [ 'Position Less Than', 'p:lt(3)', [ 'firstp', 'ap', 'sndp' ] ]);

// *** unit/selector.js ***
//   210: 	t( "Is A Parent", "p:parent", ["firstp","ap","sndp","en","sap","first"] );

$v.cf($v.ro('t'), [ 'Is A Parent', 'p:parent', [ 'firstp', 'ap', 'sndp', 'en', 'sap', 'first' ] ]);

// *** unit/selector.js ***
//   211: 	t( "Is Visible", "#form input:visible", ["text1","text2","radio1","radio2","check1","check2","name"] );

$v.cf($v.ro('t'), [ 'Is Visible', '#form input:visible', [ 'text1', 'text2', 'radio1', 'radio2', 'check1', 'check2', 'name' ] ]);

// *** unit/selector.js ***
//   212: 	t( "Is Hidden", "#form input:hidden", ["hidden1","hidden2"] );

$v.cf($v.ro('t'), [ 'Is Hidden', '#form input:hidden', [ 'hidden1', 'hidden2' ] ]);

// *** unit/selector.js ***
//   214: 	t( "Form element :input", "#form :input", ["text1", "text2", "radio1", "radio2", "check1", "check2", "hidden1", "hidden2", "name", "button", "area1", "select1", "select2", "select3"] );

$v.cf($v.ro('t'), [ 'Form element :input', '#form :input', [ 'text1', 'text2', 'radio1', 'radio2', 'check1', 'check2', 'hidden1', 'hidden2', 'name', 'button', 'area1', 'select1', 'select2', 'select3' ] ]);

// *** unit/selector.js ***
//   215: 	t( "Form element :radio", "#form :radio", ["radio1", "radio2"] );

$v.cf($v.ro('t'), [ 'Form element :radio', '#form :radio', [ 'radio1', 'radio2' ] ]);

// *** unit/selector.js ***
//   216: 	t( "Form element :checkbox", "#form :checkbox", ["check1", "check2"] );

$v.cf($v.ro('t'), [ 'Form element :checkbox', '#form :checkbox', [ 'check1', 'check2' ] ]);

// *** unit/selector.js ***
//   217: 	t( "Form element :text", "#form :text", ["text1", "text2", "hidden2", "name"] );

$v.cf($v.ro('t'), [ 'Form element :text', '#form :text', [ 'text1', 'text2', 'hidden2', 'name' ] ]);

// *** unit/selector.js ***
//   218: 	t( "Form element :radio:checked", "#form :radio:checked", ["radio2"] );

$v.cf($v.ro('t'), [ 'Form element :radio:checked', '#form :radio:checked', [ 'radio2' ] ]);

// *** unit/selector.js ***
//   219: 	t( "Form element :checkbox:checked", "#form :checkbox:checked", ["check1"] );

$v.cf($v.ro('t'), [ 'Form element :checkbox:checked', '#form :checkbox:checked', [ 'check1' ] ]);

// *** unit/selector.js ***
//   220: 	t( "Form element :checkbox:checked, :radio:checked", "#form :checkbox:checked, #form :radio:checked", ["check1", "radio2"] );

$v.cf($v.ro('t'), [ 'Form element :checkbox:checked, :radio:checked', '#form :checkbox:checked, #form :radio:checked', [ 'check1', 'radio2' ] ]);

// *** unit/selector.js ***
//   222: 	t( "Headers", ":header", ["header", "banner", "userAgent"] );

$v.cf($v.ro('t'), [ 'Headers', ':header', [ 'header', 'banner', 'userAgent' ] ]);
$v.cf($v.ro('t'), [ 'Has Children - :has()', 'p:has(a)', [ 'firstp', 'ap', 'en', 'sap' ] ]);

// *** unit/selector.js ***
//   223: 	t( "Has Children - :has()", "p:has(a)", ["firstp","ap","en","sap"] );

})) ]);
}
} catch (ex___) {
___.getNewModuleHandler().handleUncaughtException(ex___, $v.ro('onerror'), 'unit/selector.js', '1');
}
IMPORTS___.htmlEmitter___.pc('\n\t');

// *** unit/event.js ***
//     1: module("event");
//     2: 
//     4: 	expect(3);
//    10: 
//    12: });
//    13: 
//    15: 	expect(4);
//    23: });
//    24: 
//    26: 	expect(2);
//    38: });
//    39: 
//    41: 	expect(1);
//    46: });
//    47: 
//    49: 	// events don't work with iframes, see #939 - this test fails in IE because of contentDocument
//    50: 	// var doc = document.getElementById("iframe").contentDocument;
//    51: 	// 
//    52: 	// doc.body.innerHTML = "<input type='text'/>";
//    53: 	//
//    54: 	// var input = doc.getElementsByTagName("input")[0];
//    55: 	//
//    56: 	// $(input).bind("click",function() {
//    57: 	// 	ok( true, "Binding to element inside iframe" );
//    58: 	// }).click();
//    59: });
//    60: 
//    62: 	expect(3);
//    70: });
//    71: 
//    73: 	expect(6);
//    74: 
//    77: 	});
//    78: 
//    81: 	});
//    82: 
//    85: 	});
//    86: 
//    87: 	// Trigger both bound fn (2)
//    89: 
//    90: 	// Trigger one bound fn (1)
//    92: 
//    93: 	// Remove only the one fn
//    95: 
//    96: 	// Trigger the remaining fn (1)
//    98: 
//    99: 	// Remove the remaining fn
//   101: 
//   102: 	// Trigger the remaining fn (0)
//   104: 
//   105: 	// using contents will get comments regular, text, and comment nodes
//   109: 
//   110: 	// Make sure events stick with appendTo'd elements (which are cloned) #2027
//   113: });
//   114: 
//   116: 	expect(6);
//   122: 	}).click();
//   123: 	
//   126: 	}).click();
//   127: 	
//   131: 	};
//   134: 	
//   138: 	};
//   141: 	
//   145: });
//   146: 
//   148: 	expect(8);
//   150: 	el.click(function() {
//   152: 	});
//   156: 	});
//   158: 	
//   162: 
//   167: 
//   170: 	
//   171: 	reset();
//   182: });
//   183: 
//   185: 	expect(67);
//   186: 
//   194: 
//   201: 
//   209: 
//   216: 
//   217: 	// Simulate a "native" click
//   220: 	};
//   221: 
//   222: 	// Triggers handlrs and native
//   223: 	// Trigger 5
//   225: 
//   226: 	// Triggers handlers, native, and extra fn
//   227: 	// Triggers 9
//   229: 
//   230: 	// Simulate a "native" click
//   233: 	};
//   234: 
//   235: 	// Triggers handlers, native, and extra fn
//   236: 	// Triggers 7
//   238: 
//   239: 	// Trigger only the handlers (no native)
//   240: 	// Triggers 5
//   242: 
//   243: 	// Trigger only the handlers (no native) and extra fn
//   244: 	// Triggers 8
//   246: 
//   247: 	// Build fake click event to pass in
//   249: 
//   250: 	// Trigger only the handlers (no native), with external event obj
//   251: 	// Triggers 5
//   253: 
//   254: 	// Trigger only the handlers (no native) and extra fn, with external event obj
//   255: 	// Triggers 9
//   258: 	
//   268: 
//   269: 	// have the extra handler override the return
//   270: 	// Triggers 9
//   272: 
//   273: 	// have the extra handler leave the return value alone
//   274: 	// Triggers 9
//   276: });
//   277: 
//   279: 	expect(11);
//   280: 	
//   288: 
//   292: 
//   296: 		$(this).toggle(function() {
//   298: 		}, function() {
//   300: 		});
//   303: 	
//   316: 	
//   318: 	$div.click();
//   320: 	$div.click();
//   322: 	$div.click();
//   324: 	$div.click();
//   326: 	$div.click();
//   328: 	
//   332: });
//   333: 
//   335: 	stop();
//   340: });
//   341: 
//   343: 	stop();
//   348: });

try {
{
$v.cf($v.ro('module'), [ 'event' ]);

// *** unit/event.js ***
//     3: test("bind(), with data", function() {

$v.cf($v.ro('test'), [ 'bind(), with data', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);

// *** unit/event.js ***
//     8: 	};

var handler = ___.frozenFunc(function () {

// *** unit/event.js ***
//     5: 	var handler = function(event) {
//     6: 		ok( event.data, "bind() with data, check passed data exists" );
//     7: 		equals( event.data.foo, "bar", "bind() with data, Check value of passed data" );

function handler$_var$($dis, event) {
$v.cf($v.ro('ok'), [ $v.r(event, 'data'), 'bind() with data, check passed data exists' ]);
$v.cf($v.ro('equals'), [ $v.r($v.r(event, 'data'), 'foo'), 'bar', 'bind() with data, Check value of passed data' ]);
}
___.func(handler$_var$, 'handler$_var$');
;
var handler$_var = $v.dis(___.primFreeze(handler$_var$), 'handler$_var');
return handler$_var;
}).CALL___();

// *** unit/event.js ***
//     9: 	$("#firstp").bind("click", {foo: "bar"}, handler).click().unbind("click", handler);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'bind', [ 'click', ___.initializeMap([ 'foo', 'bar' ]), handler ]), 'click', [ ]), 'unbind', [ 'click', handler ]);

// *** unit/event.js ***
//    11: 	ok( !jQuery.data($("#firstp")[0], "events"), "Event handler unbound when using data." );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'data', [ $v.r($v.cf($v.ro('$'), [ '#firstp' ]), 0), 'events' ]), 'Event handler unbound when using data.' ]);
})) ]);

// *** unit/event.js ***
//    14: test("bind(), with data, trigger with data", function() {

$v.cf($v.ro('test'), [ 'bind(), with data, trigger with data', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/event.js ***
//    21: 	};

var handler = ___.frozenFunc(function () {

// *** unit/event.js ***
//    16: 	var handler = function(event, data) {
//    17: 		ok( event.data, "check passed data exists" );
//    18: 		equals( event.data.foo, "bar", "Check value of passed data" );
//    19: 		ok( data, "Check trigger data" );
//    20: 		equals( data.bar, "foo", "Check value of trigger data" );

function handler$_var$($dis, event, data) {
$v.cf($v.ro('ok'), [ $v.r(event, 'data'), 'check passed data exists' ]);
$v.cf($v.ro('equals'), [ $v.r($v.r(event, 'data'), 'foo'), 'bar', 'Check value of passed data' ]);
$v.cf($v.ro('ok'), [ data, 'Check trigger data' ]);
$v.cf($v.ro('equals'), [ $v.r(data, 'bar'), 'foo', 'Check value of trigger data' ]);
}
___.func(handler$_var$, 'handler$_var$');
;
var handler$_var = $v.dis(___.primFreeze(handler$_var$), 'handler$_var');
return handler$_var;
}).CALL___();

// *** unit/event.js ***
//    22: 	$("#firstp").bind("click", {foo: "bar"}, handler).trigger("click", [{bar: "foo"}]).unbind("click", handler);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'bind', [ 'click', ___.initializeMap([ 'foo', 'bar' ]), handler ]), 'trigger', [ 'click', [ ___.initializeMap([ 'bar', 'foo' ]) ] ]), 'unbind', [ 'click', handler ]);
})) ]);

// *** unit/event.js ***
//    25: test("bind(), multiple events at once", function() {

$v.cf($v.ro('test'), [ 'bind(), multiple events at once', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);

// *** unit/event.js ***
//    27: 	var clickCounter = 0,
//    28: 		mouseoverCounter = 0;

var clickCounter = 0, mouseoverCounter = 0;

// *** unit/event.js ***
//    34: 	};

var handler = ___.frozenFunc(function () {

// *** unit/event.js ***
//    29: 	var handler = function(event) {
//    32: 		else if (event.type == "mouseover")
//    33: 			mouseoverCounter += 1;

function handler$_var$($dis, event) {

// *** unit/event.js ***
//    30: 		if (event.type == "click")
//    31: 			clickCounter += 1;

if ($v.r(event, 'type') == 'click') clickCounter = clickCounter + 1;
else if ($v.r(event, 'type') == 'mouseover') mouseoverCounter = mouseoverCounter + 1;
}
___.func(handler$_var$, 'handler$_var$');
;
var handler$_var = $v.dis(___.primFreeze(handler$_var$), 'handler$_var');
return handler$_var;
}).CALL___();

// *** unit/event.js ***
//    35: 	$("#firstp").bind("click mouseover", handler).trigger("click").trigger("mouseover");

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'bind', [ 'click mouseover', handler ]), 'trigger', [ 'click' ]), 'trigger', [ 'mouseover' ]);

// *** unit/event.js ***
//    36: 	equals( clickCounter, 1, "bind() with multiple events at once" );

$v.cf($v.ro('equals'), [ clickCounter, 1, 'bind() with multiple events at once' ]);
$v.cf($v.ro('equals'), [ mouseoverCounter, 1, 'bind() with multiple events at once' ]);

// *** unit/event.js ***
//    37: 	equals( mouseoverCounter, 1, "bind() with multiple events at once" );

})) ]);

// *** unit/event.js ***
//    40: test("bind(), no data", function() {

$v.cf($v.ro('test'), [ 'bind(), no data', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);

// *** unit/event.js ***
//    44: 	};

var handler = ___.frozenFunc(function () {

// *** unit/event.js ***
//    42: 	var handler = function(event) {
//    43: 		ok ( !event.data, "Check that no data is added to the event object" );

function handler$_var$($dis, event) {
$v.cf($v.ro('ok'), [ !$v.r(event, 'data'), 'Check that no data is added to the event object' ]);
}
___.func(handler$_var$, 'handler$_var$');
;
var handler$_var = $v.dis(___.primFreeze(handler$_var$), 'handler$_var');
return handler$_var;
}).CALL___();
$v.cm($v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'bind', [ 'click', handler ]), 'trigger', [ 'click' ]);

// *** unit/event.js ***
//    45: 	$("#firstp").bind("click", handler).trigger("click");

})) ]);

// *** unit/event.js ***
//    48: test("bind(), iframes", function() {

$v.cf($v.ro('test'), [ 'bind(), iframes', $v.dis(___.frozenFunc(function ($dis) {
})) ]);

// *** unit/event.js ***
//    61: test("bind(), trigger change on select", function() {

$v.cf($v.ro('test'), [ 'bind(), trigger change on select', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/event.js ***
//    65: 		equals( event.data, counter++, "Event.data is not a global event object" );

function selectOnChange$_caller($dis, event) {
$v.cf($v.ro('equals'), [ $v.r(event, 'data'), counter++, 'Event.data is not a global event object' ]);
}
___.func(selectOnChange$_caller, 'selectOnChange$_caller');
var selectOnChange;
;

// *** unit/event.js ***
//    64: 	function selectOnChange(event) {

selectOnChange = $v.dis(___.primFreeze(selectOnChange$_caller), 'selectOnChange');
$v.cf($v.ro('expect'), [ 3 ]);

// *** unit/event.js ***
//    63: 	var counter = 0;

var counter = 0;

// *** unit/event.js ***
//    66: 	};

;
;

// *** unit/event.js ***
//    67: 	$("#form select").each(function(i){

$v.cm($v.cm($v.cf($v.ro('$'), [ '#form select' ]), 'each', [ $v.dis(___.frozenFunc(function ($dis, i) {
$v.cm($v.cf($v.ro('$'), [ $dis ]), 'bind', [ 'change', i, selectOnChange ]);

// *** unit/event.js ***
//    68: 		$(this).bind('change', i, selectOnChange);

})) ]), 'trigger', [ 'change' ]);

// *** unit/event.js ***
//    69: 	}).trigger('change');

})) ]);

// *** unit/event.js ***
//    72: test("bind(), namespaced events, cloned events", function() {

$v.cf($v.ro('test'), [ 'bind(), namespaced events, cloned events', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/event.js ***
//    75: 	$("#firstp").bind("custom.test",function(e){

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'bind', [ 'custom.test', $v.dis(___.frozenFunc(function ($dis, e) {
$v.cf($v.ro('ok'), [ true, 'Custom event triggered' ]);

// *** unit/event.js ***
//    76: 		ok(true, "Custom event triggered");

})) ]);

// *** unit/event.js ***
//    79: 	$("#firstp").bind("click",function(e){

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'bind', [ 'click', $v.dis(___.frozenFunc(function ($dis, e) {
$v.cf($v.ro('ok'), [ true, 'Normal click triggered' ]);

// *** unit/event.js ***
//    80: 		ok(true, "Normal click triggered");

})) ]);

// *** unit/event.js ***
//    83: 	$("#firstp").bind("click.test",function(e){

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'bind', [ 'click.test', $v.dis(___.frozenFunc(function ($dis, e) {
$v.cf($v.ro('ok'), [ true, 'Namespaced click triggered' ]);

// *** unit/event.js ***
//    84: 		ok(true, "Namespaced click triggered");

})) ]);

// *** unit/event.js ***
//    88: 	$("#firstp").trigger("click");

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'trigger', [ 'click' ]);

// *** unit/event.js ***
//    91: 	$("#firstp").trigger("click.test");

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'trigger', [ 'click.test' ]);

// *** unit/event.js ***
//    94: 	$("#firstp").unbind("click.test");

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'unbind', [ 'click.test' ]);

// *** unit/event.js ***
//    97: 	$("#firstp").trigger("click");

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'trigger', [ 'click' ]);

// *** unit/event.js ***
//   100: 	$("#firstp").unbind(".test");

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'unbind', [ '.test' ]);

// *** unit/event.js ***
//   103: 	$("#firstp").trigger("custom");

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'trigger', [ 'custom' ]);

// *** unit/event.js ***
//   106: 	$("#nonnodes").contents().bind("tester", function () {

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#nonnodes' ]), 'contents', [ ]), 'bind', [ 'tester', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('equals'), [ $v.r($dis, 'nodeType'), 1, 'Check node,textnode,comment bind just does real nodes' ]);

// *** unit/event.js ***
//   107: 		equals(this.nodeType, 1, "Check node,textnode,comment bind just does real nodes" );
//   108: 	}).trigger("tester");

})) ]), 'trigger', [ 'tester' ]);
$v.cm($v.cm($v.cf($v.ro('$'), [ '\x3ca href=\'#fail\' class=\'test\'\x3etest\x3c/a\x3e' ]), 'click', [ $v.dis(___.frozenFunc(function ($dis) {
return false;

// *** unit/event.js ***
//   111: 	$("<a href='#fail' class='test'>test<�a>").click(function(){ return false; }).appendTo("p");

})) ]), 'appendTo', [ 'p' ]);

// *** unit/event.js ***
//   112: 	ok( $("a.test:first").triggerHandler("click") === false, "Handler is bound to appendTo'd elements" );

$v.cf($v.ro('ok'), [ $v.cm($v.cf($v.ro('$'), [ 'a.test:first' ]), 'triggerHandler', [ 'click' ]) === false, 'Handler is bound to appendTo\'d elements' ]);
})) ]);

// *** unit/event.js ***
//   115: test("trigger() shortcuts", function() {

$v.cf($v.ro('test'), [ 'trigger() shortcuts', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/event.js ***
//   117: 	$('<li><a href="#">Change location<�a><�li>').prependTo('#firstUL').find('a').bind('click', function() {

$v.cm($v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '\x3cli\x3e\x3ca href=\"#\"\x3eChange location\x3c/a\x3e\x3c/li\x3e' ]), 'prependTo', [ '#firstUL' ]), 'find', [ 'a' ]), 'bind', [ 'click', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/event.js ***
//   118: 		var close = $('spanx', this); // same with $(this).find('span');

var close = $v.cf($v.ro('$'), [ 'spanx', $dis ]);

// *** unit/event.js ***
//   119: 		equals( close.length, 0, "Context element does not exist, length must be zero" );

$v.cf($v.ro('equals'), [ $v.r(close, 'length'), 0, 'Context element does not exist, length must be zero' ]);

// *** unit/event.js ***
//   120: 		ok( !close[0], "Context element does not exist, direct access to element must return undefined" );

$v.cf($v.ro('ok'), [ !$v.r(close, 0), 'Context element does not exist, direct access to element must return undefined' ]);

// *** unit/event.js ***
//   121: 		return false;

return false;
})) ]), 'click', [ ]);

// *** unit/event.js ***
//   124: 	$("#check1").click(function() {

$v.cm($v.cm($v.cf($v.ro('$'), [ '#check1' ]), 'click', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ true, 'click event handler for checkbox gets fired twice, see #815' ]);

// *** unit/event.js ***
//   125: 		ok( true, "click event handler for checkbox gets fired twice, see #815" );

})) ]), 'click', [ ]);

// *** unit/event.js ***
//   128: 	var counter = 0;

var counter = 0;
$v.s($v.r($v.cf($v.ro('$'), [ '#firstp' ]), 0), 'onclick', ___.frozenFunc(function () {

// *** unit/event.js ***
//   129: 	$('#firstp')[0].onclick = function(event) {

function onclick$_meth$($dis, event) {

// *** unit/event.js ***
//   130: 		counter++;

counter++;
}
___.func(onclick$_meth$, 'onclick$_meth$');
;
var onclick$_meth = $v.dis(___.primFreeze(onclick$_meth$), 'onclick$_meth');
return onclick$_meth;
}).CALL___());

// *** unit/event.js ***
//   132: 	$('#firstp').click();

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'click', [ ]);

// *** unit/event.js ***
//   133: 	equals( counter, 1, "Check that click, triggers onclick event handler also" );

$v.cf($v.ro('equals'), [ counter, 1, 'Check that click, triggers onclick event handler also' ]);

// *** unit/event.js ***
//   135: 	var clickCounter = 0;

var clickCounter = 0;
$v.s($v.r($v.cf($v.ro('$'), [ '#simon1' ]), 0), 'onclick', ___.frozenFunc(function () {

// *** unit/event.js ***
//   136: 	$('#simon1')[0].onclick = function(event) {

function onclick$_meth$($dis, event) {

// *** unit/event.js ***
//   137: 		clickCounter++;

clickCounter++;
}
___.func(onclick$_meth$, 'onclick$_meth$');
;
var onclick$_meth = $v.dis(___.primFreeze(onclick$_meth$), 'onclick$_meth');
return onclick$_meth;
}).CALL___());

// *** unit/event.js ***
//   139: 	$('#simon1').click();

$v.cm($v.cf($v.ro('$'), [ '#simon1' ]), 'click', [ ]);

// *** unit/event.js ***
//   140: 	equals( clickCounter, 1, "Check that click, triggers onclick event handler on an a tag also" );

$v.cf($v.ro('equals'), [ clickCounter, 1, 'Check that click, triggers onclick event handler on an a tag also' ]);

// *** unit/event.js ***
//   142: 	$('<img />').load(function(){

$v.cm($v.cm($v.cf($v.ro('$'), [ '\x3cimg /\x3e' ]), 'load', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ true, 'Trigger the load event, using the shortcut .load() (#2819)' ]);

// *** unit/event.js ***
//   143: 		ok( true, "Trigger the load event, using the shortcut .load() (#2819)");

})) ]), 'load', [ ]);

// *** unit/event.js ***
//   144: 	}).load();

})) ]);

// *** unit/event.js ***
//   147: test("unbind(event)", function() {

$v.cf($v.ro('test'), [ 'unbind(event)', $v.dis(___.frozenFunc(function ($dis) {
var $caja$43;
var $caja$44;
$v.cf($v.ro('expect'), [ 8 ]);

// *** unit/event.js ***
//   149: 	var el = $("#firstp");

var el = $v.cf($v.ro('$'), [ '#firstp' ]);
$v.cm(el, 'click', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ true, 'Fake normal bind' ]);

// *** unit/event.js ***
//   151: 		ok( true, "Fake normal bind" );

})) ]);

// *** unit/event.js ***
//   153: 	el.click(function(event) {

$v.cm(el, 'click', [ $v.dis(___.frozenFunc(function ($dis, event) {

// *** unit/event.js ***
//   154: 		el.unbind(event);

$v.cm(el, 'unbind', [ event ]);
$v.cf($v.ro('ok'), [ true, 'Fake onebind' ]);

// *** unit/event.js ***
//   155: 		ok( true, "Fake onebind" );

})) ]);

// *** unit/event.js ***
//   157: 	el.click().click();

$v.cm($v.cm(el, 'click', [ ]), 'click', [ ]);
$v.cm(el, 'click', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/event.js ***
//   159: 	el.click(function() { return; });

return;
})) ]);

// *** unit/event.js ***
//   160: 	el.unbind('click');

$v.cm(el, 'unbind', [ 'click' ]);

// *** unit/event.js ***
//   161: 	ok( !el[0].onclick, "Handler is removed" ); // Bug #964

$v.cf($v.ro('ok'), [ !$v.r($v.r(el, 0), 'onclick'), 'Handler is removed' ]);
$v.cm(el, 'click', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/event.js ***
//   163: 	el.click(function() { return; });

return;
})) ]);
$v.cm(el, 'unbind', [ 'change', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/event.js ***
//   164: 	el.unbind('change',function(){ return; });

return;
})) ]);
{
$caja$43 = $v.keys($v.r($v.cm($v.ro('jQuery'), 'data', [ $v.r(el, 0), 'events' ]), 'click'));
for ($caja$44 = 0; $caja$44 < ($caja$43.length_canRead___? $caja$43.length: ___.readPub($caja$43, 'length')); ++$caja$44) {

// *** unit/event.js ***
//   165: 	for (var ret in jQuery.data(el[0], "events")['click']) break;

var ret = ___.readPub($caja$43, $caja$44);
break;
}
}

// *** unit/event.js ***
//   166: 	ok( ret, "Extra handlers weren't accidentally removed." );

$v.cf($v.ro('ok'), [ ret, 'Extra handlers weren\'t accidentally removed.' ]);

// *** unit/event.js ***
//   168: 	el.unbind('click');

$v.cm(el, 'unbind', [ 'click' ]);

// *** unit/event.js ***
//   169: 	ok( !jQuery.data(el[0], "events"), "Removed the events expando after all handlers are unbound." );

$v.cf($v.ro('ok'), [ !$v.cm($v.ro('jQuery'), 'data', [ $v.r(el, 0), 'events' ]), 'Removed the events expando after all handlers are unbound.' ]);
$v.cf($v.ro('reset'), [ ]);

// *** unit/event.js ***
//   172: 	var clickCounter = (mouseoverCounter = 0);

var clickCounter = $v.so('mouseoverCounter', 0);

// *** unit/event.js ***
//   178: 	};

var handler = ___.frozenFunc(function () {

// *** unit/event.js ***
//   173: 	var handler = function(event) {
//   176: 		else if (event.type == "mouseover")
//   177: 			mouseoverCounter += 1;

function handler$_var$($dis, event) {

// *** unit/event.js ***
//   174: 		if (event.type == "click")
//   175: 			clickCounter += 1;

if ($v.r(event, 'type') == 'click') clickCounter = clickCounter + 1;
else if ($v.r(event, 'type') == 'mouseover') $v.so('mouseoverCounter', $v.ro('mouseoverCounter') + 1);
}
___.func(handler$_var$, 'handler$_var$');
;
var handler$_var = $v.dis(___.primFreeze(handler$_var$), 'handler$_var');
return handler$_var;
}).CALL___();

// *** unit/event.js ***
//   179: 	$("#firstp").bind("click mouseover", handler).unbind("click mouseover", handler).trigger("click").trigger("mouseover");

$v.cm($v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'bind', [ 'click mouseover', handler ]), 'unbind', [ 'click mouseover', handler ]), 'trigger', [ 'click' ]), 'trigger', [ 'mouseover' ]);

// *** unit/event.js ***
//   180: 	equals( clickCounter, 0, "unbind() with multiple events at once" );

$v.cf($v.ro('equals'), [ clickCounter, 0, 'unbind() with multiple events at once' ]);
$v.cf($v.ro('equals'), [ $v.ro('mouseoverCounter'), 0, 'unbind() with multiple events at once' ]);

// *** unit/event.js ***
//   181: 	equals( mouseoverCounter, 0, "unbind() with multiple events at once" );

})) ]);

// *** unit/event.js ***
//   184: test("trigger(event, [data], [fn])", function() {

$v.cf($v.ro('test'), [ 'trigger(event, [data], [fn])', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 67 ]);

// *** unit/event.js ***
//   193: 	};

var handler = ___.frozenFunc(function () {

// *** unit/event.js ***
//   187: 	var handler = function(event, a, b, c) {
//   188: 		equals( event.type, "click", "check passed data" );
//   189: 		equals( a, 1, "check passed data" );
//   190: 		equals( b, "2", "check passed data" );
//   191: 		equals( c, "abc", "check passed data" );

function handler$_var$($dis, event, a, b, c) {
$v.cf($v.ro('equals'), [ $v.r(event, 'type'), 'click', 'check passed data' ]);
$v.cf($v.ro('equals'), [ a, 1, 'check passed data' ]);
$v.cf($v.ro('equals'), [ b, '2', 'check passed data' ]);
$v.cf($v.ro('equals'), [ c, 'abc', 'check passed data' ]);

// *** unit/event.js ***
//   192: 		return "test";

return 'test';
}
___.func(handler$_var$, 'handler$_var$');
;
var handler$_var = $v.dis(___.primFreeze(handler$_var$), 'handler$_var');
return handler$_var;
}).CALL___();

// *** unit/event.js ***
//   200: 	};

var handler2 = ___.frozenFunc(function () {

// *** unit/event.js ***
//   195: 	var handler2 = function(a, b, c) {
//   196: 		equals( a, 1, "check passed data" );
//   197: 		equals( b, "2", "check passed data" );
//   198: 		equals( c, "abc", "check passed data" );

function handler2$_var$($dis, a, b, c) {
$v.cf($v.ro('equals'), [ a, 1, 'check passed data' ]);
$v.cf($v.ro('equals'), [ b, '2', 'check passed data' ]);
$v.cf($v.ro('equals'), [ c, 'abc', 'check passed data' ]);

// *** unit/event.js ***
//   199: 		return false;

return false;
}
___.func(handler2$_var$, 'handler2$_var$');
;
var handler2$_var = $v.dis(___.primFreeze(handler2$_var$), 'handler2$_var');
return handler2$_var;
}).CALL___();

// *** unit/event.js ***
//   208: 	};

var handler3 = ___.frozenFunc(function () {

// *** unit/event.js ***
//   202: 	var handler3 = function(a, b, c, v) {
//   203: 		equals( a, 1, "check passed data" );
//   204: 		equals( b, "2", "check passed data" );
//   205: 		equals( c, "abc", "check passed data" );
//   206: 		equals( v, "test", "check current value" );

function handler3$_var$($dis, a, b, c, v) {
$v.cf($v.ro('equals'), [ a, 1, 'check passed data' ]);
$v.cf($v.ro('equals'), [ b, '2', 'check passed data' ]);
$v.cf($v.ro('equals'), [ c, 'abc', 'check passed data' ]);
$v.cf($v.ro('equals'), [ v, 'test', 'check current value' ]);

// *** unit/event.js ***
//   207: 		return "newVal";

return 'newVal';
}
___.func(handler3$_var$, 'handler3$_var$');
;
var handler3$_var = $v.dis(___.primFreeze(handler3$_var$), 'handler3$_var');
return handler3$_var;
}).CALL___();

// *** unit/event.js ***
//   215: 	};

var handler4 = ___.frozenFunc(function () {

// *** unit/event.js ***
//   210: 	var handler4 = function(a, b, c, v) {
//   211: 		equals( a, 1, "check passed data" );
//   212: 		equals( b, "2", "check passed data" );
//   213: 		equals( c, "abc", "check passed data" );
//   214: 		equals( v, "test", "check current value" );

function handler4$_var$($dis, a, b, c, v) {
$v.cf($v.ro('equals'), [ a, 1, 'check passed data' ]);
$v.cf($v.ro('equals'), [ b, '2', 'check passed data' ]);
$v.cf($v.ro('equals'), [ c, 'abc', 'check passed data' ]);
$v.cf($v.ro('equals'), [ v, 'test', 'check current value' ]);
}
___.func(handler4$_var$, 'handler4$_var$');
;
var handler4$_var = $v.dis(___.primFreeze(handler4$_var$), 'handler4$_var');
return handler4$_var;
}).CALL___();

// *** unit/event.js ***
//   218: 	$("#firstp")[0].click = function(){

$v.s($v.r($v.cf($v.ro('$'), [ '#firstp' ]), 0), 'click', ___.frozenFunc(function () {

// *** unit/event.js ***
//   219: 		ok( true, "Native call was triggered" );

function click$_meth$($dis) {
$v.cf($v.ro('ok'), [ true, 'Native call was triggered' ]);
}
___.func(click$_meth$, 'click$_meth$');
;
var click$_meth = $v.dis(___.primFreeze(click$_meth$), 'click$_meth');
return click$_meth;
}).CALL___());

// *** unit/event.js ***
//   224: 	$("#firstp").bind("click", handler).trigger("click", [1, "2", "abc"]);

$v.cm($v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'bind', [ 'click', handler ]), 'trigger', [ 'click', [ 1, '2', 'abc' ] ]);

// *** unit/event.js ***
//   228: 	$("#firstp").trigger("click", [1, "2", "abc"], handler4);

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'trigger', [ 'click', [ 1, '2', 'abc' ], handler4 ]);

// *** unit/event.js ***
//   231: 	$("#firstp")[0].click = function(){

$v.s($v.r($v.cf($v.ro('$'), [ '#firstp' ]), 0), 'click', ___.frozenFunc(function () {

// *** unit/event.js ***
//   232: 		ok( false, "Native call was triggered" );

function click$_meth$($dis) {
$v.cf($v.ro('ok'), [ false, 'Native call was triggered' ]);
}
___.func(click$_meth$, 'click$_meth$');
;
var click$_meth = $v.dis(___.primFreeze(click$_meth$), 'click$_meth');
return click$_meth;
}).CALL___());

// *** unit/event.js ***
//   237: 	$("#firstp").trigger("click", [1, "2", "abc"], handler2);

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'trigger', [ 'click', [ 1, '2', 'abc' ], handler2 ]);

// *** unit/event.js ***
//   241: 	equals( $("#firstp").triggerHandler("click", [1, "2", "abc"]), "test", "Verify handler response" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'triggerHandler', [ 'click', [ 1, '2', 'abc' ] ]), 'test', 'Verify handler response' ]);

// *** unit/event.js ***
//   245: 	equals( $("#firstp").triggerHandler("click", [1, "2", "abc"], handler2), false, "Verify handler response" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'triggerHandler', [ 'click', [ 1, '2', 'abc' ], handler2 ]), false, 'Verify handler response' ]);

// *** unit/event.js ***
//   248: 	var eventObj = jQuery.event.fix({ type: "foo", target: document.body });

var eventObj = $v.cm($v.r($v.ro('jQuery'), 'event'), 'fix', [ ___.initializeMap([ 'type', 'foo', 'target', $v.r($v.ro('document'), 'body') ]) ]);

// *** unit/event.js ***
//   252: 	equals( $("#firstp").triggerHandler("click", [eventObj, 1, "2", "abc"]), "test", "Verify handler response" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'triggerHandler', [ 'click', [ eventObj, 1, '2', 'abc' ] ]), 'test', 'Verify handler response' ]);

// *** unit/event.js ***
//   256: 	eventObj = jQuery.event.fix({ type: "foo", target: document.body });

eventObj = $v.cm($v.r($v.ro('jQuery'), 'event'), 'fix', [ ___.initializeMap([ 'type', 'foo', 'target', $v.r($v.ro('document'), 'body') ]) ]);

// *** unit/event.js ***
//   257: 	equals( $("#firstp").triggerHandler("click", [eventObj, 1, "2", "abc"], handler), "test", "Verify handler response" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'triggerHandler', [ 'click', [ eventObj, 1, '2', 'abc' ], handler ]), 'test', 'Verify handler response' ]);

// *** unit/event.js ***
//   259: 	var pass = true;

var pass = true;

// *** unit/event.js ***
//   260: 	try {
//   261: 		$('input:first')
//   262: 			.hide()
//   263: 			.trigger('focus');
//   266: 	}

try {
$v.cm($v.cm($v.cf($v.ro('$'), [ 'input:first' ]), 'hide', [ ]), 'trigger', [ 'focus' ]);
} catch (ex___) {

// *** unit/event.js ***
//   264: 	} catch(e) {

try {
throw ___.tameException(ex___);
} catch (e) {

// *** unit/event.js ***
//   265: 		pass = false;

pass = false;
}
}

// *** unit/event.js ***
//   267: 	ok( pass, "Trigger focus on hidden element" );

$v.cf($v.ro('ok'), [ pass, 'Trigger focus on hidden element' ]);

// *** unit/event.js ***
//   271: 	equals( $("#firstp").triggerHandler("click", [1, "2", "abc"], handler3), "newVal", "Verify triggerHandler return is overwritten by extra function" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'triggerHandler', [ 'click', [ 1, '2', 'abc' ], handler3 ]), 'newVal', 'Verify triggerHandler return is overwritten by extra function' ]);

// *** unit/event.js ***
//   275: 	equals( $("#firstp").triggerHandler("click", [1, "2", "abc"], handler4), "test", "Verify triggerHandler return is not overwritten by extra function" );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'triggerHandler', [ 'click', [ 1, '2', 'abc' ], handler4 ]), 'test', 'Verify triggerHandler return is not overwritten by extra function' ]);
})) ]);

// *** unit/event.js ***
//   278: test("toggle(Function, Function, ...)", function() {

$v.cf($v.ro('test'), [ 'toggle(Function, Function, ...)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 11 ]);

// *** unit/event.js ***
//   281: 	var count = 0,
//   285: 		link = $('#mark');

var count = 0, fn1 = ___.frozenFunc(function () {

// *** unit/event.js ***
//   282: 		fn1 = function(e) { count++; },

function fn1$_var$($dis, e) {
count++;
}
___.func(fn1$_var$, 'fn1$_var$');
;
var fn1$_var = $v.dis(___.primFreeze(fn1$_var$), 'fn1$_var');
return fn1$_var;
}).CALL___(), fn2 = ___.frozenFunc(function () {

// *** unit/event.js ***
//   283: 		fn2 = function(e) { count--; },

function fn2$_var$($dis, e) {
count--;
}
___.func(fn2$_var$, 'fn2$_var$');
;
var fn2$_var = $v.dis(___.primFreeze(fn2$_var$), 'fn2$_var');
return fn2$_var;
}).CALL___(), preventDefault = ___.frozenFunc(function () {

// *** unit/event.js ***
//   284: 		preventDefault = function(e) { e.preventDefault() },

function preventDefault$_var$($dis, e) {
$v.cm(e, 'preventDefault', [ ]);
}
___.func(preventDefault$_var$, 'preventDefault$_var$');
;
var preventDefault$_var = $v.dis(___.primFreeze(preventDefault$_var$), 'preventDefault$_var');
return preventDefault$_var;
}).CALL___(), link = $v.cf($v.ro('$'), [ '#mark' ]);

// *** unit/event.js ***
//   286: 	link.click(preventDefault).click().toggle(fn1, fn2).click().click().click().click().click();

$v.cm($v.cm($v.cm($v.cm($v.cm($v.cm($v.cm($v.cm(link, 'click', [ preventDefault ]), 'click', [ ]), 'toggle', [ fn1, fn2 ]), 'click', [ ]), 'click', [ ]), 'click', [ ]), 'click', [ ]), 'click', [ ]);

// *** unit/event.js ***
//   287: 	equals( count, 1, "Check for toggle(fn, fn)" );

$v.cf($v.ro('equals'), [ count, 1, 'Check for toggle(fn, fn)' ]);

// *** unit/event.js ***
//   289: 	$("#firstp").toggle(function () {

$v.cm($v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'toggle', [ $v.dis(___.frozenFunc(function ($dis) {
var a___ = ___.args(arguments);
$v.cf($v.ro('equals'), [ $v.r(Array.slice(a___, 1), 'length'), 4, 'toggle correctly passes through additional triggered arguments, see #1701' ]);

// *** unit/event.js ***
//   290: 		equals(arguments.length, 4, "toggle correctly passes through additional triggered arguments, see #1701" )

})), $v.dis(___.frozenFunc(function ($dis) {

// *** unit/event.js ***
//   291: 	}, function() {}).trigger("click", [ 1, 2, 3 ]);

})) ]), 'trigger', [ 'click', [ 1, 2, 3 ] ]);

// *** unit/event.js ***
//   293: 	var first = 0;

var first = 0;

// *** unit/event.js ***
//   294: 	$("#simon1").one("click", function() {

$v.cm($v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#simon1' ]), 'one', [ 'click', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/event.js ***
//   295: 		ok( true, "Execute event only once" );

$v.cf($v.ro('ok'), [ true, 'Execute event only once' ]);
$v.cm($v.cf($v.ro('$'), [ $dis ]), 'toggle', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('equals'), [ first++, 0, 'toggle(Function,Function) assigned from within one(\'xxx\'), see #1054' ]);

// *** unit/event.js ***
//   297: 			equals( first++, 0, "toggle(Function,Function) assigned from within one('xxx'), see #1054" );

})), $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('equals'), [ first, 1, 'toggle(Function,Function) assigned from within one(\'xxx\'), see #1054' ]);

// *** unit/event.js ***
//   299: 			equals( first, 1, "toggle(Function,Function) assigned from within one('xxx'), see #1054" );

})) ]);

// *** unit/event.js ***
//   301: 		return false;

return false;

// *** unit/event.js ***
//   302: 	}).click().click().click();

})) ]), 'click', [ ]), 'click', [ ]), 'click', [ ]);

// *** unit/event.js ***
//   304: 	var turn = 0;

var turn = 0;

// *** unit/event.js ***
//   305: 	var fns = [
//   306: 		function(){
//   308: 		},
//   309: 		function(){
//   311: 		},
//   312: 		function(){
//   314: 		}
//   315: 	];

var fns = [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/event.js ***
//   307: 			turn = 1;

turn = 1;
})), $v.dis(___.frozenFunc(function ($dis) {

// *** unit/event.js ***
//   310: 			turn = 2;

turn = 2;
})), $v.dis(___.frozenFunc(function ($dis) {

// *** unit/event.js ***
//   313: 			turn = 3;

turn = 3;
})) ];

// *** unit/event.js ***
//   317: 	var $div = $("<div>&nbsp;<�div>").toggle( fns[0], fns[1], fns[2] );

var $div = $v.cm($v.cf($v.ro('$'), [ '\x3cdiv\x3e\x26nbsp;\x3c/div\x3e' ]), 'toggle', [ $v.r(fns, 0), $v.r(fns, 1), $v.r(fns, 2) ]);
$v.cm($div, 'click', [ ]);

// *** unit/event.js ***
//   319: 	equals( turn, 1, "Trying toggle with 3 functions, attempt 1 yields 1");

$v.cf($v.ro('equals'), [ turn, 1, 'Trying toggle with 3 functions, attempt 1 yields 1' ]);
$v.cm($div, 'click', [ ]);

// *** unit/event.js ***
//   321: 	equals( turn, 2, "Trying toggle with 3 functions, attempt 2 yields 2");

$v.cf($v.ro('equals'), [ turn, 2, 'Trying toggle with 3 functions, attempt 2 yields 2' ]);
$v.cm($div, 'click', [ ]);

// *** unit/event.js ***
//   323: 	equals( turn, 3, "Trying toggle with 3 functions, attempt 3 yields 3");

$v.cf($v.ro('equals'), [ turn, 3, 'Trying toggle with 3 functions, attempt 3 yields 3' ]);
$v.cm($div, 'click', [ ]);

// *** unit/event.js ***
//   325: 	equals( turn, 1, "Trying toggle with 3 functions, attempt 4 yields 1");

$v.cf($v.ro('equals'), [ turn, 1, 'Trying toggle with 3 functions, attempt 4 yields 1' ]);
$v.cm($div, 'click', [ ]);

// *** unit/event.js ***
//   327: 	equals( turn, 2, "Trying toggle with 3 functions, attempt 5 yields 2");

$v.cf($v.ro('equals'), [ turn, 2, 'Trying toggle with 3 functions, attempt 5 yields 2' ]);

// *** unit/event.js ***
//   329: 	$div.unbind('click',fns[0]);

$v.cm($div, 'unbind', [ 'click', $v.r(fns, 0) ]);

// *** unit/event.js ***
//   330: 	var data = $.data( $div[0], 'events' );

var data = $v.cm($v.ro('$'), 'data', [ $v.r($div, 0), 'events' ]);
$v.cf($v.ro('ok'), [ !data, 'Unbinding one function from toggle unbinds them all' ]);

// *** unit/event.js ***
//   331: 	ok( !data, "Unbinding one function from toggle unbinds them all");

})) ]);

// *** unit/event.js ***
//   334: test("jQuery(function($) {})", function() {

$v.cf($v.ro('test'), [ 'jQuery(function($) {})', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('stop'), [ ]);

// *** unit/event.js ***
//   336: 	jQuery(function($) {

$v.cf($v.ro('jQuery'), [ $v.dis(___.frozenFunc(function ($dis, $) {

// *** unit/event.js ***
//   337: 		equals(jQuery, $, "ready doesn't provide an event object, instead it provides a reference to the jQuery function, see http://docs.jquery.com/Events/ready#fn");

$v.cf($v.ro('equals'), [ $v.ro('jQuery'), $, 'ready doesn\'t provide an event object, instead it provides a reference to the jQuery function, see http://docs.jquery.com/Events/ready#fn' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/event.js ***
//   338: 		start();

})) ]);

// *** unit/event.js ***
//   339: 	});

})) ]);

// *** unit/event.js ***
//   342: test("event properties", function() {

$v.cf($v.ro('test'), [ 'event properties', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('stop'), [ ]);

// *** unit/event.js ***
//   344: 	$("#simon1").click(function(event) {

$v.cm($v.cm($v.cf($v.ro('$'), [ '#simon1' ]), 'click', [ $v.dis(___.frozenFunc(function ($dis, event) {

// *** unit/event.js ***
//   345: 		ok( event.timeStamp, "assert event.timeStamp is present" );

$v.cf($v.ro('ok'), [ $v.r(event, 'timeStamp'), 'assert event.timeStamp is present' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/event.js ***
//   346: 		start();

})) ]), 'click', [ ]);

// *** unit/event.js ***
//   347: 	}).click();

})) ]);
}
} catch (ex___) {
___.getNewModuleHandler().handleUncaughtException(ex___, $v.ro('onerror'), 'unit/event.js', '1');
}
IMPORTS___.htmlEmitter___.pc('\n\t');

// *** unit/ajax.js ***
//     1: module("ajax");
//     2: 
//     3: // Safari 3 randomly crashes when running these tests,
//     4: // but only in the full suite - you can run just the Ajax
//     5: // tests and they'll pass
//     6: //if ( !jQuery.browser.safari ) {
//     7: 

try {
{
$v.cf($v.ro('module'), [ 'ajax' ]);

// *** unit/ajax.js ***
//     8: if ( !isLocal ) {
//     9: 
//    11: 	expect( 8 );
//    12: 	
//    14: 	
//    15: 	stop();
//    16: 	
//    17: 	setTimeout(function(){	
//    20:         }).ajaxStop(function(){
//    23:         }).ajaxSend(function(){
//    25:         }).ajaxComplete(function(){
//    27:         }).ajaxError(function(){
//    29:         }).ajaxSuccess(function(){
//    31:         });
//    32:         
//    33:         $.ajax({
//    41: });
//    42: 
//    44:     expect( 8 );
//    45:     stop();
//    46:     
//    49:     }).ajaxStop(function(){
//    52:     }).ajaxSend(function(){
//    54:     }).ajaxComplete(function(){
//    56:     }).ajaxError(function(){
//    58:     }).ajaxSuccess(function(){
//    60:     });
//    61:     
//    63:     
//    64:     $.ajax({
//    71: });
//    72: 
//    74: 	expect( 3 );
//    75: 	stop();
//    76: 	
//    79: 	}).ajaxStop(function(){
//    81: 	}).ajaxSend(function(){
//    83: 	}).ajaxComplete(function(){
//    85: 	}).ajaxError(function(){
//    87: 	}).ajaxSuccess(function(){
//    89: 	});
//    90: 	
//    91: 	$.ajax({
//    92: 		global: false,
//    97: 		complete: function(){
//   100:         }
//   102: });
//   103: 
//   105: 	expect(3);
//   106: 	stop();
//   107: 	$.ajax({
//   109: 	  dataType: "xml",
//   115: 	  }
//   117: });
//   118: 
//   120: 	expect(1);
//   121: 	stop();
//   122: 	
//   124: 	
//   126: 	
//   127: 	$.ajax({
//   131: 		},
//   135: 		}
//   137: });
//   138: 
//   140: 	expect(2);
//   158: });
//   159: 
//   160: var foobar;
//   161: 
//   163: 	expect(5);
//   164: 	stop();
//   165: 	
//   166: 	foobar = null;
//   167: 	testFoo = undefined;
//   168: 
//   174: 
//   175: 	$.ajax({
//   176: 	  dataType: "html",
//   182: 	  }
//   184: });
//   185: 
//   186: test("serialize()", function() {
//   187: 	expect(6);
//   188: 	
//   192: 		
//   196: 	
//   200: 		
//   204: 		
//   208: 		
//   212: });
//   213: 
//   214: test("$.param()", function() {
//   215: 	expect(4);
//   218: 	
//   221: 	
//   224: 	
//   227: });
//   228: 
//   230: 	expect(1);
//   232: });
//   233: 
//   235: 	expect(2);
//   239: });
//   240: 
//   242: 	expect(8);
//   243: 	stop(true);
//   244: 	
//   255: 	});
//   261: 	});
//   262: 	
//   268: });
//   269: 
//   270: test("ajax cache", function () {
//   271: 	expect(18);
//   272: 	stop();
//   273: 	
//   275: 
//   290: 	});
//   291: 
//   298: });
//   299: 
//   301: 	expect(2);
//   302: 
//   306: 
//   308: 
//   310: 	t.data = {};
//   311:     $.ajax(t);
//   313: 
//   316:     $.ajax(t);
//   318: 	
//   320: });
//   321: 
//   322: test("load(String)", function() {
//   323: 	expect(1);
//   324: 	stop(true); // check if load can be called with only url
//   326: });
//   327: 
//   329: 	expect(1);
//   330: 	stop(true); // check if load can be called with only url
//   335: });
//   336: 
//   338: 	expect(1);
//   339: 	stop();
//   346: 	});
//   348: });
//   349: 
//   351: 	expect(2);
//   352: 	stop();
//   357: });
//   358: 
//   360: 	expect(7);
//   361: 	stop();
//   363: 	window.foobar = null;
//   375: });
//   376: 
//   378: 	expect(3);
//   379: 	stop();
//   380: 	testFoo = undefined;
//   386: });
//   387: 
//   389: 	expect(2);
//   390: 	stop();
//   395: 		});
//   400: });
//   401: 
//   403: 	expect(2);
//   404: 	stop();
//   405: 	window.foobar = null;
//   410: });
//   411: 
//   413: 	expect(1);
//   414: 	stop(true);
//   416: });
//   417: 
//   419: 	expect(7);
//   420: 
//   423: 
//   424: 	stop();
//   425: 
//   426: 	$.ajax({
//   427: 		url: "data/jsonp.php",
//   428: 		dataType: "jsonp",
//   432: 		},
//   436: 		}
//   437: 	});
//   438: 
//   439: 	$.ajax({
//   441: 		dataType: "jsonp",
//   445: 		},
//   449: 		}
//   450: 	});
//   451: 
//   452: 	$.ajax({
//   453: 		url: "data/jsonp.php",
//   454: 		dataType: "jsonp",
//   455: 		data: "callback=?",
//   459: 		},
//   463: 		}
//   464: 	});
//   465: 
//   466: 	$.ajax({
//   467: 		url: "data/jsonp.php",
//   468: 		dataType: "jsonp",
//   469: 		jsonp: "callback",
//   473: 		},
//   477: 		}
//   478: 	});
//   479: 
//   480: 	$.ajax({
//   481: 		type: "POST",
//   482: 		url: "data/jsonp.php",
//   483: 		dataType: "jsonp",
//   487: 		},
//   491: 		}
//   492: 	});
//   493: 
//   494: 	$.ajax({
//   495: 		type: "POST",
//   496: 		url: "data/jsonp.php",
//   497: 		data: "callback=?",
//   498: 		dataType: "jsonp",
//   502: 		},
//   506: 		}
//   507: 	});
//   508: 
//   509: 	$.ajax({
//   510: 		type: "POST",
//   511: 		url: "data/jsonp.php",
//   512: 		jsonp: "callback",
//   513: 		dataType: "jsonp",
//   517: 		},
//   521: 		}
//   523: });
//   524: 
//   526: 	expect(4);
//   527: 
//   530: 
//   532: 
//   533: 	stop();
//   534: 
//   535: 	$.ajax({
//   537: 		dataType: "jsonp",
//   541: 		},
//   545: 		}
//   546: 	});
//   547: 
//   548: 	$.ajax({
//   550: 		dataType: "jsonp",
//   554: 		},
//   558: 		}
//   559: 	});
//   560: 
//   561: 	$.ajax({
//   563: 		dataType: "jsonp",
//   564: 		data: "callback=?",
//   568: 		},
//   572: 		}
//   573: 	});
//   574: 
//   575: 	$.ajax({
//   577: 		dataType: "jsonp",
//   578: 		jsonp: "callback",
//   582: 		},
//   586: 		}
//   588: });
//   589: 
//   591: 	expect(2);
//   592: 
//   594: 
//   595: 	stop();
//   596: 
//   597: 	window.foobar = null;
//   598: 	$.ajax({
//   600: 		dataType: "script",
//   604: 		}
//   606: });
//   607: 
//   609: 	expect(3);
//   610: 
//   612: 
//   613: 	stop();
//   614: 
//   615: 	window.foobar = null;
//   616: 	$.ajax({
//   618: 		type: "POST",
//   619: 		dataType: "script",
//   624: 		}
//   626: });
//   627: 
//   629: 	expect(2);
//   630: 
//   633: 
//   634: 	stop();
//   635: 
//   636: 	window.foobar = null;
//   637: 	$.ajax({
//   639: 		dataType: "script",
//   643: 		}
//   645: });
//   646: 
//   648: 	expect(4);
//   649: 	stop();
//   657: });
//   658: 
//   660: 	expect(2);
//   661: 	stop();
//   667: });
//   668: 
//   670: 	expect(2);
//   671: 
//   673: 
//   674: 	stop();
//   680: });
//   681: 
//   683: 	expect(4);
//   684: 	stop();
//   690: 	});
//   691: 
//   696: 		 });
//   699: });
//   700: 
//   702: 	stop();
//   703: 	
//   705: 
//   707: 	
//   716: 	
//   721: 	
//   723: 	
//   724: 	$.ajax({
//   725: 	  type: "GET",
//   727: 	  error: pass,
//   728: 	  success: fail
//   729: 	});
//   730: 	
//   731: 	// reset timeout
//   733: });
//   734: 
//   736: 	stop();
//   738: 
//   739: 	$.ajax({
//   740: 	  type: "GET",
//   741: 	  timeout: 5000,
//   743: 	  error: function() {
//   746: 	  },
//   747: 	  success: function() {
//   750: 	  }
//   751: 	});
//   752: 
//   753: 	// reset timeout
//   755: });
//   756: 
//   758: 	expect(1);
//   759: 	stop();
//   760: 	$.ajax({
//   761: 	  type: "GET",
//   766: 	  }
//   768: });
//   769: 
//   771: 	expect(1);
//   772: 	stop();
//   773: 	$.ajax({
//   774: 	  type: "POST",
//   776: 	  data: "name=peter",
//   780: 	  }
//   782: });
//   783: 
//   784: test("ajaxSetup()", function() {
//   785: 	expect(1);
//   786: 	stop();
//   787: 	$.ajaxSetup({
//   792: 		}
//   793: 	});
//   795: });
//   796: 
//   798: 	stop();
//   799: 	$.ajax({
//   801: 		timeout: 500,
//   806: 		}
//   808: });
//   809: 
//   811: 	stop();
//   812: 	$.ajax({
//   814: 		data: {
//   815: 			key: function() {
//   817: 			}
//   818: 		},
//   822: 		}
//   824: });
//   825: 
//   826: }

if (!$v.ro('isLocal')) {

// *** unit/ajax.js ***
//    10: test("$.ajax() - success callbacks", function() {

$v.cf($v.ro('test'), [ '$.ajax() - success callbacks', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 8 ]);

// *** unit/ajax.js ***
//    13: 	$.ajaxSetup({ timeout: 0 });

$v.cm($v.ro('$'), 'ajaxSetup', [ ___.initializeMap([ 'timeout', 0 ]) ]);
$v.cf($v.ro('stop'), [ ]);
$v.cf($v.ro('setTimeout'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/ajax.js ***
//    18:         $('#foo').ajaxStart(function(){

$v.cm($v.cm($v.cm($v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'ajaxStart', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ true, 'ajaxStart' ]);

// *** unit/ajax.js ***
//    19:             ok( true, "ajaxStart" );

})) ]), 'ajaxStop', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/ajax.js ***
//    21:             ok( true, "ajaxStop" );

$v.cf($v.ro('ok'), [ true, 'ajaxStop' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//    22:             start();

})) ]), 'ajaxSend', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ true, 'ajaxSend' ]);

// *** unit/ajax.js ***
//    24:             ok( true, "ajaxSend" );

})) ]), 'ajaxComplete', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ true, 'ajaxComplete' ]);

// *** unit/ajax.js ***
//    26:             ok( true, "ajaxComplete" );

})) ]), 'ajaxError', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ false, 'ajaxError' ]);

// *** unit/ajax.js ***
//    28:             ok( false, "ajaxError" );

})) ]), 'ajaxSuccess', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ true, 'ajaxSuccess' ]);

// *** unit/ajax.js ***
//    30:             ok( true, "ajaxSuccess" );

})) ]);

// *** unit/ajax.js ***
//    34:             url: url("data/name.html"),

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', $v.cf($v.ro('url'), [ 'data/name.html' ]), 'beforeSend', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    35:             beforeSend: function(){ ok(true, "beforeSend"); },

function beforeSend$_lit$($dis) {
$v.cf($v.ro('ok'), [ true, 'beforeSend' ]);
}
___.func(beforeSend$_lit$, 'beforeSend$_lit$');
;
var beforeSend$_lit = $v.dis(___.primFreeze(beforeSend$_lit$), 'beforeSend$_lit');
return beforeSend$_lit;
}).CALL___(), 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    36:             success: function(){ ok(true, "success"); },

function success$_lit$($dis) {
$v.cf($v.ro('ok'), [ true, 'success' ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    37:             error: function(){ ok(false, "error"); },

function error$_lit$($dis) {
$v.cf($v.ro('ok'), [ false, 'error' ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___(), 'complete', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    38:             complete: function(){ ok(true, "complete"); }

function complete$_lit$($dis) {
$v.cf($v.ro('ok'), [ true, 'complete' ]);
}
___.func(complete$_lit$, 'complete$_lit$');
;
var complete$_lit = $v.dis(___.primFreeze(complete$_lit$), 'complete$_lit');
return complete$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//    39:         });

})), 13 ]);

// *** unit/ajax.js ***
//    40:     }, 13);

})) ]);

// *** unit/ajax.js ***
//    43: test("$.ajax() - error callbacks", function() {

$v.cf($v.ro('test'), [ '$.ajax() - error callbacks', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 8 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//    47:     $('#foo').ajaxStart(function(){

$v.cm($v.cm($v.cm($v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'ajaxStart', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ true, 'ajaxStart' ]);

// *** unit/ajax.js ***
//    48:         ok( true, "ajaxStart" );

})) ]), 'ajaxStop', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/ajax.js ***
//    50:         ok( true, "ajaxStop" );

$v.cf($v.ro('ok'), [ true, 'ajaxStop' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//    51:         start();

})) ]), 'ajaxSend', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ true, 'ajaxSend' ]);

// *** unit/ajax.js ***
//    53:         ok( true, "ajaxSend" );

})) ]), 'ajaxComplete', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ true, 'ajaxComplete' ]);

// *** unit/ajax.js ***
//    55:         ok( true, "ajaxComplete" );

})) ]), 'ajaxError', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ true, 'ajaxError' ]);

// *** unit/ajax.js ***
//    57:         ok( true, "ajaxError" );

})) ]), 'ajaxSuccess', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ false, 'ajaxSuccess' ]);

// *** unit/ajax.js ***
//    59:         ok( false, "ajaxSuccess" );

})) ]);

// *** unit/ajax.js ***
//    62:     $.ajaxSetup({ timeout: 500 });

$v.cm($v.ro('$'), 'ajaxSetup', [ ___.initializeMap([ 'timeout', 500 ]) ]);

// *** unit/ajax.js ***
//    65:         url: url("data/name.php?wait=5"),

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', $v.cf($v.ro('url'), [ 'data/name.php?wait=5' ]), 'beforeSend', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    66:         beforeSend: function(){ ok(true, "beforeSend"); },

function beforeSend$_lit$($dis) {
$v.cf($v.ro('ok'), [ true, 'beforeSend' ]);
}
___.func(beforeSend$_lit$, 'beforeSend$_lit$');
;
var beforeSend$_lit = $v.dis(___.primFreeze(beforeSend$_lit$), 'beforeSend$_lit');
return beforeSend$_lit;
}).CALL___(), 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    67:         success: function(){ ok(false, "success"); },

function success$_lit$($dis) {
$v.cf($v.ro('ok'), [ false, 'success' ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    68:         error: function(){ ok(true, "error"); },

function error$_lit$($dis) {
$v.cf($v.ro('ok'), [ true, 'error' ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___(), 'complete', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    69:         complete: function(){ ok(true, "complete"); }

function complete$_lit$($dis) {
$v.cf($v.ro('ok'), [ true, 'complete' ]);
}
___.func(complete$_lit$, 'complete$_lit$');
;
var complete$_lit = $v.dis(___.primFreeze(complete$_lit$), 'complete$_lit');
return complete$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//    70:     });

})) ]);

// *** unit/ajax.js ***
//    73: test("$.ajax() - disabled globals", function() {

$v.cf($v.ro('test'), [ '$.ajax() - disabled globals', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//    77: 	$('#foo').ajaxStart(function(){

$v.cm($v.cm($v.cm($v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'ajaxStart', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ false, 'ajaxStart' ]);

// *** unit/ajax.js ***
//    78: 		ok( false, "ajaxStart" );

})) ]), 'ajaxStop', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ false, 'ajaxStop' ]);

// *** unit/ajax.js ***
//    80: 		ok( false, "ajaxStop" );

})) ]), 'ajaxSend', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ false, 'ajaxSend' ]);

// *** unit/ajax.js ***
//    82: 		ok( false, "ajaxSend" );

})) ]), 'ajaxComplete', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ false, 'ajaxComplete' ]);

// *** unit/ajax.js ***
//    84: 		ok( false, "ajaxComplete" );

})) ]), 'ajaxError', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ false, 'ajaxError' ]);

// *** unit/ajax.js ***
//    86: 		ok( false, "ajaxError" );

})) ]), 'ajaxSuccess', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('ok'), [ false, 'ajaxSuccess' ]);

// *** unit/ajax.js ***
//    88: 		ok( false, "ajaxSuccess" );

})) ]);

// *** unit/ajax.js ***
//    93: 		url: url("data/name.html"),

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'global', false, 'url', $v.cf($v.ro('url'), [ 'data/name.html' ]), 'beforeSend', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    94: 		beforeSend: function(){ ok(true, "beforeSend"); },

function beforeSend$_lit$($dis) {
$v.cf($v.ro('ok'), [ true, 'beforeSend' ]);
}
___.func(beforeSend$_lit$, 'beforeSend$_lit$');
;
var beforeSend$_lit = $v.dis(___.primFreeze(beforeSend$_lit$), 'beforeSend$_lit');
return beforeSend$_lit;
}).CALL___(), 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    95: 		success: function(){ ok(true, "success"); },

function success$_lit$($dis) {
$v.cf($v.ro('ok'), [ true, 'success' ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    96: 		error: function(){ ok(false, "error"); },

function error$_lit$($dis) {
$v.cf($v.ro('ok'), [ false, 'error' ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___(), 'complete', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//    98: 		  ok(true, "complete");

function complete$_lit$($dis) {
$v.cf($v.ro('ok'), [ true, 'complete' ]);
$v.cf($v.ro('setTimeout'), [ $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//    99: 		  setTimeout(function(){ start(); }, 13);

})), 13 ]);
}
___.func(complete$_lit$, 'complete$_lit$');
;
var complete$_lit = $v.dis(___.primFreeze(complete$_lit$), 'complete$_lit');
return complete$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   101: 	});

})) ]);

// *** unit/ajax.js ***
//   104: test("$.ajax - xml: non-namespace elements inside namespaced elements", function() {

$v.cf($v.ro('test'), [ '$.ajax - xml: non-namespace elements inside namespaced elements', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   108: 	  url: url("data/with_fries.xml"),

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', $v.cf($v.ro('url'), [ 'data/with_fries.xml' ]), 'dataType', 'xml', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   110: 	  success: function(resp) {
//   111: 	    equals( $("properties", resp).length, 1, 'properties in responseXML' );
//   112: 	    equals( $("jsconf", resp).length, 1, 'jsconf in responseXML' );
//   113: 	    equals( $("thing", resp).length, 2, 'things in responseXML' );
//   114: 	    start();

function success$_lit$($dis, resp) {
$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ 'properties', resp ]), 'length'), 1, 'properties in responseXML' ]);
$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ 'jsconf', resp ]), 'length'), 1, 'jsconf in responseXML' ]);
$v.cf($v.ro('equals'), [ $v.r($v.cf($v.ro('$'), [ 'thing', resp ]), 'length'), 2, 'things in responseXML' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   116: 	});

})) ]);

// *** unit/ajax.js ***
//   119: test("$.ajax - beforeSend", function() {

$v.cf($v.ro('test'), [ '$.ajax - beforeSend', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   123: 	var check = false;

var check = false;

// *** unit/ajax.js ***
//   125: 	$.ajaxSetup({ timeout: 0 });

$v.cm($v.ro('$'), 'ajaxSetup', [ ___.initializeMap([ 'timeout', 0 ]) ]);

// *** unit/ajax.js ***
//   128: 		url: url("data/name.html"), 

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', $v.cf($v.ro('url'), [ 'data/name.html' ]), 'beforeSend', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   129: 		beforeSend: function(xml) {

function beforeSend$_lit$($dis, xml) {

// *** unit/ajax.js ***
//   130: 			check = true;

check = true;
}
___.func(beforeSend$_lit$, 'beforeSend$_lit$');
;
var beforeSend$_lit = $v.dis(___.primFreeze(beforeSend$_lit$), 'beforeSend$_lit');
return beforeSend$_lit;
}).CALL___(), 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   132: 		success: function(data) {
//   133: 			ok( check, "check beforeSend was executed" );
//   134: 			start();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ check, 'check beforeSend was executed' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   136: 	});

})) ]);

// *** unit/ajax.js ***
//   139: test("$.ajax - beforeSend, cancel request (#2688)", function() {

$v.cf($v.ro('test'), [ '$.ajax - beforeSend, cancel request (#2688)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);

// *** unit/ajax.js ***
//   141: 	var request = $.ajax({
//   142: 		url: url("data/name.html"), 
//   143: 		beforeSend: function() {
//   146: 		},
//   147: 		success: function() {
//   149: 		},
//   150: 		complete: function() {
//   152: 		},
//   153: 		error: function() {
//   155: 		}
//   156: 	});

var request = $v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', $v.cf($v.ro('url'), [ 'data/name.html' ]), 'beforeSend', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   144: 			ok( true, "beforeSend got called, canceling" );

function beforeSend$_lit$($dis) {
$v.cf($v.ro('ok'), [ true, 'beforeSend got called, canceling' ]);

// *** unit/ajax.js ***
//   145: 			return false;

return false;
}
___.func(beforeSend$_lit$, 'beforeSend$_lit$');
;
var beforeSend$_lit = $v.dis(___.primFreeze(beforeSend$_lit$), 'beforeSend$_lit');
return beforeSend$_lit;
}).CALL___(), 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   148: 			ok( false, "request didn't get canceled" );

function success$_lit$($dis) {
$v.cf($v.ro('ok'), [ false, 'request didn\'t get canceled' ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'complete', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   151: 			ok( false, "request didn't get canceled" );

function complete$_lit$($dis) {
$v.cf($v.ro('ok'), [ false, 'request didn\'t get canceled' ]);
}
___.func(complete$_lit$, 'complete$_lit$');
;
var complete$_lit = $v.dis(___.primFreeze(complete$_lit$), 'complete$_lit');
return complete$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   154: 			ok( false, "request didn't get canceled" );

function error$_lit$($dis) {
$v.cf($v.ro('ok'), [ false, 'request didn\'t get canceled' ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);
$v.cf($v.ro('ok'), [ request === false, 'canceled request must return false instead of XMLHttpRequest instance' ]);

// *** unit/ajax.js ***
//   157: 	ok( request === false, "canceled request must return false instead of XMLHttpRequest instance" );

})) ]);
$v.initOuter('foobar');

// *** unit/ajax.js ***
//   162: test("$.ajax - dataType html", function() {

$v.cf($v.ro('test'), [ '$.ajax - dataType html', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 5 ]);
$v.cf($v.ro('stop'), [ ]);
$v.so('foobar', null);
$v.so('testFoo', $v.ro('undefined'));

// *** unit/ajax.js ***
//   169: 	var verifyEvaluation = function() {
//   173: 	};

var verifyEvaluation = ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   170: 	  equals( testFoo, "foo", 'Check if script was evaluated for datatype html' );
//   171: 	  equals( foobar, "bar", 'Check if script src was evaluated for datatype html' );
//   172: 	  start();

function verifyEvaluation$_var$($dis) {
$v.cf($v.ro('equals'), [ $v.ro('testFoo'), 'foo', 'Check if script was evaluated for datatype html' ]);
$v.cf($v.ro('equals'), [ $v.ro('foobar'), 'bar', 'Check if script src was evaluated for datatype html' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(verifyEvaluation$_var$, 'verifyEvaluation$_var$');
;
var verifyEvaluation$_var = $v.dis(___.primFreeze(verifyEvaluation$_var$), 'verifyEvaluation$_var');
return verifyEvaluation$_var;
}).CALL___();

// *** unit/ajax.js ***
//   177: 	  url: url("data/test.html"),

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'dataType', 'html', 'url', $v.cf($v.ro('url'), [ 'data/test.html' ]), 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   178: 	  success: function(data) {
//   179: 	  	$("#ap").html(data);
//   180: 	    ok( data.match(/^html text/), 'Check content for datatype html' );
//   181: 	    setTimeout(verifyEvaluation, 600);

function success$_lit$($dis, data) {
$v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'html', [ data ]);
$v.cf($v.ro('ok'), [ $v.cm(data, 'match', [ $v.construct(RegExp, [ '^html text' ]) ]), 'Check content for datatype html' ]);
$v.cf($v.ro('setTimeout'), [ verifyEvaluation, 600 ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   183: 	});

})) ]);
$v.cf($v.ro('test'), [ 'serialize()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 6 ]);

// *** unit/ajax.js ***
//   189: 	equals( $('#form').serialize(),
//   190: 		"action=Test&radio2=on&check=on&hidden=&foo%5Bbar%5D=&name=name&select1=&select2=3&select3=1&select3=2",
//   191: 		'Check form serialization as query string');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#form' ]), 'serialize', [ ]), 'action=Test\x26radio2=on\x26check=on\x26hidden=\x26foo%5Bbar%5D=\x26name=name\x26select1=\x26select2=3\x26select3=1\x26select3=2', 'Check form serialization as query string' ]);

// *** unit/ajax.js ***
//   193: 	equals( $('#form :input').serialize(),
//   194: 		"action=Test&radio2=on&check=on&hidden=&foo%5Bbar%5D=&name=name&select1=&select2=3&select3=1&select3=2",
//   195: 		'Check input serialization as query string');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#form :input' ]), 'serialize', [ ]), 'action=Test\x26radio2=on\x26check=on\x26hidden=\x26foo%5Bbar%5D=\x26name=name\x26select1=\x26select2=3\x26select3=1\x26select3=2', 'Check input serialization as query string' ]);

// *** unit/ajax.js ***
//   197: 	equals( $('#testForm').serialize(), 
//   198: 		'T3=%3F%0AZ&H1=x&H2=&PWD=&T1=&T2=YES&My+Name=me&S1=abc&S3=YES&S4=', 
//   199: 		'Check form serialization as query string');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#testForm' ]), 'serialize', [ ]), 'T3=%3F%0AZ\x26H1=x\x26H2=\x26PWD=\x26T1=\x26T2=YES\x26My+Name=me\x26S1=abc\x26S3=YES\x26S4=', 'Check form serialization as query string' ]);

// *** unit/ajax.js ***
//   201: 	equals( $('#testForm :input').serialize(), 
//   202: 		'T3=%3F%0AZ&H1=x&H2=&PWD=&T1=&T2=YES&My+Name=me&S1=abc&S3=YES&S4=', 
//   203: 		'Check input serialization as query string');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#testForm :input' ]), 'serialize', [ ]), 'T3=%3F%0AZ\x26H1=x\x26H2=\x26PWD=\x26T1=\x26T2=YES\x26My+Name=me\x26S1=abc\x26S3=YES\x26S4=', 'Check input serialization as query string' ]);

// *** unit/ajax.js ***
//   205: 	equals( $('#form, #testForm').serialize(),
//   206: 		"action=Test&radio2=on&check=on&hidden=&foo%5Bbar%5D=&name=name&select1=&select2=3&select3=1&select3=2&T3=%3F%0AZ&H1=x&H2=&PWD=&T1=&T2=YES&My+Name=me&S1=abc&S3=YES&S4=",
//   207: 		'Multiple form serialization as query string');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#form, #testForm' ]), 'serialize', [ ]), 'action=Test\x26radio2=on\x26check=on\x26hidden=\x26foo%5Bbar%5D=\x26name=name\x26select1=\x26select2=3\x26select3=1\x26select3=2\x26T3=%3F%0AZ\x26H1=x\x26H2=\x26PWD=\x26T1=\x26T2=YES\x26My+Name=me\x26S1=abc\x26S3=YES\x26S4=', 'Multiple form serialization as query string' ]);

// *** unit/ajax.js ***
//   209: 	equals( $('#form, #testForm :input').serialize(),
//   210: 		"action=Test&radio2=on&check=on&hidden=&foo%5Bbar%5D=&name=name&select1=&select2=3&select3=1&select3=2&T3=%3F%0AZ&H1=x&H2=&PWD=&T1=&T2=YES&My+Name=me&S1=abc&S3=YES&S4=",

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#form, #testForm :input' ]), 'serialize', [ ]), 'action=Test\x26radio2=on\x26check=on\x26hidden=\x26foo%5Bbar%5D=\x26name=name\x26select1=\x26select2=3\x26select3=1\x26select3=2\x26T3=%3F%0AZ\x26H1=x\x26H2=\x26PWD=\x26T1=\x26T2=YES\x26My+Name=me\x26S1=abc\x26S3=YES\x26S4=', 'Mixed form/input serialization as query string' ]);

// *** unit/ajax.js ***
//   211: 		'Mixed form/input serialization as query string');

})) ]);
$v.cf($v.ro('test'), [ '$.param()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/ajax.js ***
//   216: 	var params = {foo:"bar", baz:42, quux:"All your base are belong to us"};

var params = ___.initializeMap([ 'foo', 'bar', 'baz', 42, 'quux', 'All your base are belong to us' ]);

// *** unit/ajax.js ***
//   217: 	equals( $.param(params), "foo=bar&baz=42&quux=All+your+base+are+belong+to+us", "simple" );

$v.cf($v.ro('equals'), [ $v.cm($v.ro('$'), 'param', [ params ]), 'foo=bar\x26baz=42\x26quux=All+your+base+are+belong+to+us', 'simple' ]);

// *** unit/ajax.js ***
//   219: 	params = {someName: [1, 2, 3], regularThing: "blah" };

params = ___.initializeMap([ 'someName', [ 1, 2, 3 ], 'regularThing', 'blah' ]);

// *** unit/ajax.js ***
//   220: 	equals( $.param(params), "someName=1&someName=2&someName=3&regularThing=blah", "with array" );

$v.cf($v.ro('equals'), [ $v.cm($v.ro('$'), 'param', [ params ]), 'someName=1\x26someName=2\x26someName=3\x26regularThing=blah', 'with array' ]);

// *** unit/ajax.js ***
//   222: 	params = {"foo[]":["baz", 42, "All your base are belong to us"]};

params = ___.initializeMap([ 'foo[]', [ 'baz', 42, 'All your base are belong to us' ] ]);

// *** unit/ajax.js ***
//   223: 	equals( $.param(params), "foo%5B%5D=baz&foo%5B%5D=42&foo%5B%5D=All+your+base+are+belong+to+us", "more array" );

$v.cf($v.ro('equals'), [ $v.cm($v.ro('$'), 'param', [ params ]), 'foo%5B%5D=baz\x26foo%5B%5D=42\x26foo%5B%5D=All+your+base+are+belong+to+us', 'more array' ]);

// *** unit/ajax.js ***
//   225: 	params = {"foo[bar]":"baz", "foo[beep]":42, "foo[quux]":"All your base are belong to us"};

params = ___.initializeMap([ 'foo[bar]', 'baz', 'foo[beep]', 42, 'foo[quux]', 'All your base are belong to us' ]);

// *** unit/ajax.js ***
//   226: 	equals( $.param(params), "foo%5Bbar%5D=baz&foo%5Bbeep%5D=42&foo%5Bquux%5D=All+your+base+are+belong+to+us", "even more arrays" );

$v.cf($v.ro('equals'), [ $v.cm($v.ro('$'), 'param', [ params ]), 'foo%5Bbar%5D=baz\x26foo%5Bbeep%5D=42\x26foo%5Bquux%5D=All+your+base+are+belong+to+us', 'even more arrays' ]);
})) ]);

// *** unit/ajax.js ***
//   229: test("synchronous request", function() {

$v.cf($v.ro('test'), [ 'synchronous request', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);

// *** unit/ajax.js ***
//   231: 	ok( /^{ "data"/.test( $.ajax({url: url("data/json_obj.js"), async: false}).responseText ), "check returned text" );

$v.cf($v.ro('ok'), [ $v.cm($v.construct(RegExp, [ '^{ \"data\"' ]), 'test', [ $v.r($v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', $v.cf($v.ro('url'), [ 'data/json_obj.js' ]), 'async', false ]) ]), 'responseText') ]), 'check returned text' ]);
})) ]);

// *** unit/ajax.js ***
//   234: test("synchronous request with callbacks", function() {

$v.cf($v.ro('test'), [ 'synchronous request with callbacks', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);

// *** unit/ajax.js ***
//   236: 	var result;

var result;
$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', $v.cf($v.ro('url'), [ 'data/json_obj.js' ]), 'async', false, 'success', ___.frozenFunc(function () {
function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ true, 'sucess callback executed' ]);

// *** unit/ajax.js ***
//   237: 	$.ajax({url: url("data/json_obj.js"), async: false, success: function(data) { ok(true, "sucess callback executed"); result = data; } });

result = data;
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);
$v.cf($v.ro('ok'), [ $v.cm($v.construct(RegExp, [ '^{ \"data\"' ]), 'test', [ result ]), 'check returned text' ]);

// *** unit/ajax.js ***
//   238: 	ok( /^{ "data"/.test( result ), "check returned text" );

})) ]);

// *** unit/ajax.js ***
//   241: test("pass-through request object", function() {

$v.cf($v.ro('test'), [ 'pass-through request object', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 8 ]);
$v.cf($v.ro('stop'), [ true ]);

// *** unit/ajax.js ***
//   245: 	var target = "data/name.html";

var target = 'data/name.html';

// *** unit/ajax.js ***
//   246: 	var successCount = 0;

var successCount = 0;

// *** unit/ajax.js ***
//   247: 	var errorCount = 0;

var errorCount = 0;

// *** unit/ajax.js ***
//   248:   var errorEx = "";

var errorEx = '';

// *** unit/ajax.js ***
//   249: 	var success = function() {
//   251: 	};

var success = ___.frozenFunc(function () {
function success$_var$($dis) {

// *** unit/ajax.js ***
//   250: 		successCount++;

successCount++;
}
___.func(success$_var$, 'success$_var$');
;
var success$_var = $v.dis(___.primFreeze(success$_var$), 'success$_var');
return success$_var;
}).CALL___();

// *** unit/ajax.js ***
//   252: 	$("#foo").ajaxError(function (e, xml, s, ex) {

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'ajaxError', [ $v.dis(___.frozenFunc(function ($dis, e, xml, s, ex) {

// *** unit/ajax.js ***
//   253: 		errorCount++;

errorCount++;

// *** unit/ajax.js ***
//   254:     errorEx += ": " + xml.status;

errorEx = errorEx + (': ' + $v.r(xml, 'status'));
})) ]);

// *** unit/ajax.js ***
//   256: 	$("#foo").one('ajaxStop', function () {

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'one', [ 'ajaxStop', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/ajax.js ***
//   257: 		equals(successCount, 5, "Check all ajax calls successful");

$v.cf($v.ro('equals'), [ successCount, 5, 'Check all ajax calls successful' ]);

// *** unit/ajax.js ***
//   258: 		equals(errorCount, 0, "Check no ajax errors (status" + errorEx + ")");

$v.cf($v.ro('equals'), [ errorCount, 0, 'Check no ajax errors (status' + errorEx + ')' ]);

// *** unit/ajax.js ***
//   259: 		$("#foo").unbind('ajaxError');

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'unbind', [ 'ajaxError' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//   260: 		start();

})) ]);

// *** unit/ajax.js ***
//   263: 	ok( $.get(url(target), success), "get" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('$'), 'get', [ $v.cf($v.ro('url'), [ target ]), success ]), 'get' ]);

// *** unit/ajax.js ***
//   264: 	ok( $.post(url(target), success), "post" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('$'), 'post', [ $v.cf($v.ro('url'), [ target ]), success ]), 'post' ]);

// *** unit/ajax.js ***
//   265: 	ok( $.getScript(url("data/test.js"), success), "script" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('$'), 'getScript', [ $v.cf($v.ro('url'), [ 'data/test.js' ]), success ]), 'script' ]);

// *** unit/ajax.js ***
//   266: 	ok( $.getJSON(url("data/json_obj.js"), success), "json" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('$'), 'getJSON', [ $v.cf($v.ro('url'), [ 'data/json_obj.js' ]), success ]), 'json' ]);
$v.cf($v.ro('ok'), [ $v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', $v.cf($v.ro('url'), [ target ]), 'success', success ]) ]), 'generic' ]);

// *** unit/ajax.js ***
//   267: 	ok( $.ajax({url: url(target), success: success}), "generic" );

})) ]);
$v.cf($v.ro('test'), [ 'ajax cache', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 18 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   274: 	var count = 0;

var count = 0;

// *** unit/ajax.js ***
//   276: 	$("#firstp").bind("ajaxSuccess", function (e, xml, s) {

$v.cm($v.cf($v.ro('$'), [ '#firstp' ]), 'bind', [ 'ajaxSuccess', $v.dis(___.frozenFunc(function ($dis, e, xml, s) {

// *** unit/ajax.js ***
//   277: 		var re = /_=(.*?)(&|$)/g;

var re = $v.construct(RegExp, [ '_=(.*?)(\x26|$)', 'g' ]);

// *** unit/ajax.js ***
//   278:     var oldOne = null;

var oldOne = null;

// *** unit/ajax.js ***
//   279: 		for (var i = 0; i < 6; i++) {
//   285: 		}

for (var i = 0; i < 6; i++) {

// *** unit/ajax.js ***
//   280:       var ret = re.exec(s.url);

var ret = $v.cm(re, 'exec', [ $v.r(s, 'url') ]);

// *** unit/ajax.js ***
//   281: 			if (!ret) {
//   283: 			}

if (!ret) {

// *** unit/ajax.js ***
//   282: 				break;

break;
}

// *** unit/ajax.js ***
//   284:       oldOne = ret[1];

oldOne = $v.r(ret, 1);
}

// *** unit/ajax.js ***
//   286: 		equals(i, 1, "Test to make sure only one 'no-cache' parameter is there");

$v.cf($v.ro('equals'), [ i, 1, 'Test to make sure only one \'no-cache\' parameter is there' ]);

// *** unit/ajax.js ***
//   287: 		ok(oldOne != "tobereplaced555", "Test to be sure parameter (if it was there) was replaced");

$v.cf($v.ro('ok'), [ oldOne != 'tobereplaced555', 'Test to be sure parameter (if it was there) was replaced' ]);

// *** unit/ajax.js ***
//   288: 		if(++count == 6)
//   289: 			start();

if (++count == 6) $v.cf($v.ro('start'), [ ]);
})) ]);

// *** unit/ajax.js ***
//   292: 	ok( $.ajax({url: "data/text.php", cache:false}), "test with no parameters" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/text.php', 'cache', false ]) ]), 'test with no parameters' ]);

// *** unit/ajax.js ***
//   293: 	ok( $.ajax({url: "data/text.php?pizza=true", cache:false}), "test with 1 parameter" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/text.php?pizza=true', 'cache', false ]) ]), 'test with 1 parameter' ]);

// *** unit/ajax.js ***
//   294: 	ok( $.ajax({url: "data/text.php?_=tobereplaced555", cache:false}), "test with _= parameter" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/text.php?_=tobereplaced555', 'cache', false ]) ]), 'test with _= parameter' ]);

// *** unit/ajax.js ***
//   295: 	ok( $.ajax({url: "data/text.php?pizza=true&_=tobereplaced555", cache:false}), "test with 1 parameter plus _= one" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/text.php?pizza=true\x26_=tobereplaced555', 'cache', false ]) ]), 'test with 1 parameter plus _= one' ]);

// *** unit/ajax.js ***
//   296: 	ok( $.ajax({url: "data/text.php?_=tobereplaced555&tv=false", cache:false}), "test with 1 parameter plus _= one before it" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/text.php?_=tobereplaced555\x26tv=false', 'cache', false ]) ]), 'test with 1 parameter plus _= one before it' ]);

// *** unit/ajax.js ***
//   297: 	ok( $.ajax({url: "data/text.php?name=David&_=tobereplaced555&washere=true", cache:false}), "test with 2 parameters surrounding _= one" );

$v.cf($v.ro('ok'), [ $v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/text.php?name=David\x26_=tobereplaced555\x26washere=true', 'cache', false ]) ]), 'test with 2 parameters surrounding _= one' ]);
})) ]);

// *** unit/ajax.js ***
//   300: test("global ajaxSettings", function() {

$v.cf($v.ro('test'), [ 'global ajaxSettings', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);

// *** unit/ajax.js ***
//   303: 	var tmp = jQuery.extend({}, jQuery.ajaxSettings);

var tmp = $v.cm($v.ro('jQuery'), 'extend', [ ___.initializeMap([ ]), $v.r($v.ro('jQuery'), 'ajaxSettings') ]);

// *** unit/ajax.js ***
//   304:     var orig = { url: "data/with_fries.xml" };

var orig = ___.initializeMap([ 'url', 'data/with_fries.xml' ]);

// *** unit/ajax.js ***
//   305: 	var t;

var t;

// *** unit/ajax.js ***
//   307: 	$.ajaxSetup({ data: {foo: 'bar', bar: 'BAR'} });

$v.cm($v.ro('$'), 'ajaxSetup', [ ___.initializeMap([ 'data', ___.initializeMap([ 'foo', 'bar', 'bar', 'BAR' ]) ]) ]);

// *** unit/ajax.js ***
//   309:     t = jQuery.extend({}, orig);

t = $v.cm($v.ro('jQuery'), 'extend', [ ___.initializeMap([ ]), orig ]);
$v.s(t, 'data', ___.initializeMap([ ]));
$v.cm($v.ro('$'), 'ajax', [ t ]);

// *** unit/ajax.js ***
//   312: 	ok( t.url.indexOf('foo') > -1 && t.url.indexOf('bar') > -1, "Check extending {}" );

$v.cf($v.ro('ok'), [ $v.cm($v.r(t, 'url'), 'indexOf', [ 'foo' ]) > -1 && $v.cm($v.r(t, 'url'), 'indexOf', [ 'bar' ]) > -1, 'Check extending {}' ]);

// *** unit/ajax.js ***
//   314:     t = jQuery.extend({}, orig);

t = $v.cm($v.ro('jQuery'), 'extend', [ ___.initializeMap([ ]), orig ]);

// *** unit/ajax.js ***
//   315: 	t.data = { zoo: 'a', ping: 'b' };

$v.s(t, 'data', ___.initializeMap([ 'zoo', 'a', 'ping', 'b' ]));
$v.cm($v.ro('$'), 'ajax', [ t ]);

// *** unit/ajax.js ***
//   317: 	ok( t.url.indexOf('ping') > -1 && t.url.indexOf('zoo') > -1 && t.url.indexOf('foo') > -1 && t.url.indexOf('bar') > -1, "Check extending { zoo: 'a', ping: 'b' }" );

$v.cf($v.ro('ok'), [ $v.cm($v.r(t, 'url'), 'indexOf', [ 'ping' ]) > -1 && $v.cm($v.r(t, 'url'), 'indexOf', [ 'zoo' ]) > -1 && $v.cm($v.r(t, 'url'), 'indexOf', [ 'foo' ]) > -1 && $v.cm($v.r(t, 'url'), 'indexOf', [ 'bar' ]) > -1, 'Check extending { zoo: \'a\', ping: \'b\' }' ]);
$v.s($v.ro('jQuery'), 'ajaxSettings', tmp);

// *** unit/ajax.js ***
//   319: 	jQuery.ajaxSettings = tmp;

})) ]);
$v.cf($v.ro('test'), [ 'load(String)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('stop'), [ true ]);
$v.cm($v.cf($v.ro('$'), [ '#first' ]), 'load', [ 'data/name.html', $v.ro('start') ]);

// *** unit/ajax.js ***
//   325: 	$('#first').load("data/name.html", start);

})) ]);

// *** unit/ajax.js ***
//   328: test("load('url selector')", function() {

$v.cf($v.ro('test'), [ 'load(\'url selector\')', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('stop'), [ true ]);

// *** unit/ajax.js ***
//   331: 	$('#first').load("data/test3.html div.user", function(){

$v.cm($v.cf($v.ro('$'), [ '#first' ]), 'load', [ 'data/test3.html div.user', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/ajax.js ***
//   332: 		equals( $(this).children("div").length, 2, "Verify that specific elements were injected" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($v.cf($v.ro('$'), [ $dis ]), 'children', [ 'div' ]), 'length'), 2, 'Verify that specific elements were injected' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//   333: 		start();

})) ]);

// *** unit/ajax.js ***
//   334: 	});

})) ]);

// *** unit/ajax.js ***
//   337: test("load(String, Function) with ajaxSetup on dataType json, see #2046", function() {

$v.cf($v.ro('test'), [ 'load(String, Function) with ajaxSetup on dataType json, see #2046', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   340: 	$.ajaxSetup({ dataType: "json" });

$v.cm($v.ro('$'), 'ajaxSetup', [ ___.initializeMap([ 'dataType', 'json' ]) ]);

// *** unit/ajax.js ***
//   341: 	$("#first").ajaxComplete(function (e, xml, s) {

$v.cm($v.cf($v.ro('$'), [ '#first' ]), 'ajaxComplete', [ $v.dis(___.frozenFunc(function ($dis, e, xml, s) {

// *** unit/ajax.js ***
//   342: 		equals( s.dataType, "html", "Verify the load() dataType was html" );

$v.cf($v.ro('equals'), [ $v.r(s, 'dataType'), 'html', 'Verify the load() dataType was html' ]);

// *** unit/ajax.js ***
//   343: 		$("#first").unbind("ajaxComplete");

$v.cm($v.cf($v.ro('$'), [ '#first' ]), 'unbind', [ 'ajaxComplete' ]);

// *** unit/ajax.js ***
//   344: 		$.ajaxSetup({ dataType: "" });

$v.cm($v.ro('$'), 'ajaxSetup', [ ___.initializeMap([ 'dataType', '' ]) ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//   345: 		start();

})) ]);
$v.cm($v.cf($v.ro('$'), [ '#first' ]), 'load', [ 'data/test3.html' ]);

// *** unit/ajax.js ***
//   347: 	$('#first').load("data/test3.html");

})) ]);

// *** unit/ajax.js ***
//   350: test("load(String, Function) - simple: inject text into DOM", function() {

$v.cf($v.ro('test'), [ 'load(String, Function) - simple: inject text into DOM', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   353: 	$('#first').load(url("data/name.html"), function() {

$v.cm($v.cf($v.ro('$'), [ '#first' ]), 'load', [ $v.cf($v.ro('url'), [ 'data/name.html' ]), $v.dis(___.frozenFunc(function ($dis) {

// *** unit/ajax.js ***
//   354: 		ok( /^ERROR/.test($('#first').text()), 'Check if content was injected into the DOM' );

$v.cf($v.ro('ok'), [ $v.cm($v.construct(RegExp, [ '^ERROR' ]), 'test', [ $v.cm($v.cf($v.ro('$'), [ '#first' ]), 'text', [ ]) ]), 'Check if content was injected into the DOM' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//   355: 		start();

})) ]);

// *** unit/ajax.js ***
//   356: 	});

})) ]);

// *** unit/ajax.js ***
//   359: test("load(String, Function) - check scripts", function() {

$v.cf($v.ro('test'), [ 'load(String, Function) - check scripts', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 7 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   362: 	window.testFoo = undefined;

$v.s($v.ro('window'), 'testFoo', $v.ro('undefined'));
$v.s($v.ro('window'), 'foobar', null);

// *** unit/ajax.js ***
//   364: 	var verifyEvaluation = function() {
//   368: 	};

var verifyEvaluation = ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   365: 		equals( foobar, "bar", 'Check if script src was evaluated after load' );
//   366: 		equals( $('#ap').html(), 'bar', 'Check if script evaluation has modified DOM');
//   367: 		 start();

function verifyEvaluation$_var$($dis) {
$v.cf($v.ro('equals'), [ $v.ro('foobar'), 'bar', 'Check if script src was evaluated after load' ]);
$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#ap' ]), 'html', [ ]), 'bar', 'Check if script evaluation has modified DOM' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(verifyEvaluation$_var$, 'verifyEvaluation$_var$');
;
var verifyEvaluation$_var = $v.dis(___.primFreeze(verifyEvaluation$_var$), 'verifyEvaluation$_var');
return verifyEvaluation$_var;
}).CALL___();

// *** unit/ajax.js ***
//   369: 	$('#first').load(url('data/test.html'), function() {

$v.cm($v.cf($v.ro('$'), [ '#first' ]), 'load', [ $v.cf($v.ro('url'), [ 'data/test.html' ]), $v.dis(___.frozenFunc(function ($dis) {

// *** unit/ajax.js ***
//   370: 		ok( $('#first').html().match(/^html text/), 'Check content after loading html' );

$v.cf($v.ro('ok'), [ $v.cm($v.cm($v.cf($v.ro('$'), [ '#first' ]), 'html', [ ]), 'match', [ $v.construct(RegExp, [ '^html text' ]) ]), 'Check content after loading html' ]);

// *** unit/ajax.js ***
//   371: 		equals( $('#foo').html(), 'foo', 'Check if script evaluation has modified DOM');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'html', [ ]), 'foo', 'Check if script evaluation has modified DOM' ]);

// *** unit/ajax.js ***
//   372: 		equals( testFoo, "foo", 'Check if script was evaluated after load' );

$v.cf($v.ro('equals'), [ $v.ro('testFoo'), 'foo', 'Check if script was evaluated after load' ]);
$v.cf($v.ro('setTimeout'), [ verifyEvaluation, 600 ]);

// *** unit/ajax.js ***
//   373: 		setTimeout(verifyEvaluation, 600);

})) ]);

// *** unit/ajax.js ***
//   374: 	});

})) ]);

// *** unit/ajax.js ***
//   377: test("load(String, Function) - check file with only a script tag", function() {

$v.cf($v.ro('test'), [ 'load(String, Function) - check file with only a script tag', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);
$v.cf($v.ro('stop'), [ ]);
$v.so('testFoo', $v.ro('undefined'));

// *** unit/ajax.js ***
//   381: 	$('#first').load(url('data/test2.html'), function() {

$v.cm($v.cf($v.ro('$'), [ '#first' ]), 'load', [ $v.cf($v.ro('url'), [ 'data/test2.html' ]), $v.dis(___.frozenFunc(function ($dis) {

// *** unit/ajax.js ***
//   382: 		equals( $('#foo').html(), 'foo', 'Check if script evaluation has modified DOM');

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'html', [ ]), 'foo', 'Check if script evaluation has modified DOM' ]);

// *** unit/ajax.js ***
//   383: 		equals( testFoo, "foo", 'Check if script was evaluated after load' );

$v.cf($v.ro('equals'), [ $v.ro('testFoo'), 'foo', 'Check if script was evaluated after load' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//   384: 		start();

})) ]);

// *** unit/ajax.js ***
//   385: 	});

})) ]);

// *** unit/ajax.js ***
//   388: test("$.get(String, Hash, Function) - parse xml and use text() on nodes", function() {

$v.cf($v.ro('test'), [ '$.get(String, Hash, Function) - parse xml and use text() on nodes', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   391: 	$.get(url('data/dashboard.xml'), function(xml) {

$v.cm($v.ro('$'), 'get', [ $v.cf($v.ro('url'), [ 'data/dashboard.xml' ]), $v.dis(___.frozenFunc(function ($dis, xml) {

// *** unit/ajax.js ***
//   392: 		var content = [];

var content = [ ];

// *** unit/ajax.js ***
//   393: 		$('tab', xml).each(function() {

$v.cm($v.cf($v.ro('$'), [ 'tab', xml ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm(content, 'push', [ $v.cm($v.cf($v.ro('$'), [ $dis ]), 'text', [ ]) ]);

// *** unit/ajax.js ***
//   394: 			content.push($(this).text());

})) ]);

// *** unit/ajax.js ***
//   396: 		equals( content[0], 'blabla', 'Check first tab');

$v.cf($v.ro('equals'), [ $v.r(content, 0), 'blabla', 'Check first tab' ]);

// *** unit/ajax.js ***
//   397: 		equals( content[1], 'blublu', 'Check second tab');

$v.cf($v.ro('equals'), [ $v.r(content, 1), 'blublu', 'Check second tab' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//   398: 		start();

})) ]);

// *** unit/ajax.js ***
//   399: 	});

})) ]);

// *** unit/ajax.js ***
//   402: test("$.getScript(String, Function) - with callback", function() {

$v.cf($v.ro('test'), [ '$.getScript(String, Function) - with callback', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('stop'), [ ]);
$v.s($v.ro('window'), 'foobar', null);

// *** unit/ajax.js ***
//   406: 	$.getScript(url("data/test.js"), function() {

$v.cm($v.ro('$'), 'getScript', [ $v.cf($v.ro('url'), [ 'data/test.js' ]), $v.dis(___.frozenFunc(function ($dis) {

// *** unit/ajax.js ***
//   407: 		equals( foobar, "bar", 'Check if script was evaluated' );

$v.cf($v.ro('equals'), [ $v.ro('foobar'), 'bar', 'Check if script was evaluated' ]);
$v.cf($v.ro('setTimeout'), [ $v.ro('start'), 100 ]);

// *** unit/ajax.js ***
//   408: 		setTimeout(start, 100);

})) ]);

// *** unit/ajax.js ***
//   409: 	});

})) ]);

// *** unit/ajax.js ***
//   412: test("$.getScript(String, Function) - no callback", function() {

$v.cf($v.ro('test'), [ '$.getScript(String, Function) - no callback', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('stop'), [ true ]);
$v.cm($v.ro('$'), 'getScript', [ $v.cf($v.ro('url'), [ 'data/test.js' ]), $v.ro('start') ]);

// *** unit/ajax.js ***
//   415: 	$.getScript(url("data/test.js"), start);

})) ]);

// *** unit/ajax.js ***
//   418: test("$.ajax() - JSONP, Local", function() {

$v.cf($v.ro('test'), [ '$.ajax() - JSONP, Local', $v.dis(___.frozenFunc(function ($dis) {
function plus$_caller($dis) {

// *** unit/ajax.js ***
//   422: 	function plus(){ if ( ++count == 7 ) start(); }

if (++count == 7) $v.cf($v.ro('start'), [ ]);
}
___.func(plus$_caller, 'plus$_caller');
var plus;
;
plus = $v.dis(___.primFreeze(plus$_caller), 'plus');
$v.cf($v.ro('expect'), [ 7 ]);

// *** unit/ajax.js ***
//   421: 	var count = 0;

var count = 0;
;
$v.cf($v.ro('stop'), [ ]);
$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/jsonp.php', 'dataType', 'jsonp', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   429: 		success: function(data){
//   430: 			ok( data.data, "JSON results returned (GET, no callback)" );
//   431: 			plus();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.r(data, 'data'), 'JSON results returned (GET, no callback)' ]);
$v.cf(plus, [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   433: 		error: function(data){
//   434: 			ok( false, "Ajax error JSON (GET, no callback)" );
//   435: 			plus();

function error$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ false, 'Ajax error JSON (GET, no callback)' ]);
$v.cf(plus, [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   440: 		url: "data/jsonp.php?callback=?",

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/jsonp.php?callback=?', 'dataType', 'jsonp', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   442: 		success: function(data){
//   443: 			ok( data.data, "JSON results returned (GET, url callback)" );
//   444: 			plus();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.r(data, 'data'), 'JSON results returned (GET, url callback)' ]);
$v.cf(plus, [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   446: 		error: function(data){
//   447: 			ok( false, "Ajax error JSON (GET, url callback)" );
//   448: 			plus();

function error$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ false, 'Ajax error JSON (GET, url callback)' ]);
$v.cf(plus, [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);
$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/jsonp.php', 'dataType', 'jsonp', 'data', 'callback=?', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   456: 		success: function(data){
//   457: 			ok( data.data, "JSON results returned (GET, data callback)" );
//   458: 			plus();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.r(data, 'data'), 'JSON results returned (GET, data callback)' ]);
$v.cf(plus, [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   460: 		error: function(data){
//   461: 			ok( false, "Ajax error JSON (GET, data callback)" );
//   462: 			plus();

function error$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ false, 'Ajax error JSON (GET, data callback)' ]);
$v.cf(plus, [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);
$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/jsonp.php', 'dataType', 'jsonp', 'jsonp', 'callback', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   470: 		success: function(data){
//   471: 			ok( data.data, "JSON results returned (GET, data obj callback)" );
//   472: 			plus();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.r(data, 'data'), 'JSON results returned (GET, data obj callback)' ]);
$v.cf(plus, [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   474: 		error: function(data){
//   475: 			ok( false, "Ajax error JSON (GET, data obj callback)" );
//   476: 			plus();

function error$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ false, 'Ajax error JSON (GET, data obj callback)' ]);
$v.cf(plus, [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);
$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'type', 'POST', 'url', 'data/jsonp.php', 'dataType', 'jsonp', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   484: 		success: function(data){
//   485: 			ok( data.data, "JSON results returned (POST, no callback)" );
//   486: 			plus();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.r(data, 'data'), 'JSON results returned (POST, no callback)' ]);
$v.cf(plus, [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   488: 		error: function(data){
//   489: 			ok( false, "Ajax error JSON (GET, data obj callback)" );
//   490: 			plus();

function error$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ false, 'Ajax error JSON (GET, data obj callback)' ]);
$v.cf(plus, [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);
$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'type', 'POST', 'url', 'data/jsonp.php', 'data', 'callback=?', 'dataType', 'jsonp', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   499: 		success: function(data){
//   500: 			ok( data.data, "JSON results returned (POST, data callback)" );
//   501: 			plus();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.r(data, 'data'), 'JSON results returned (POST, data callback)' ]);
$v.cf(plus, [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   503: 		error: function(data){
//   504: 			ok( false, "Ajax error JSON (POST, data callback)" );
//   505: 			plus();

function error$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ false, 'Ajax error JSON (POST, data callback)' ]);
$v.cf(plus, [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);
$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'type', 'POST', 'url', 'data/jsonp.php', 'jsonp', 'callback', 'dataType', 'jsonp', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   514: 		success: function(data){
//   515: 			ok( data.data, "JSON results returned (POST, data obj callback)" );
//   516: 			plus();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.r(data, 'data'), 'JSON results returned (POST, data obj callback)' ]);
$v.cf(plus, [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   518: 		error: function(data){
//   519: 			ok( false, "Ajax error JSON (POST, data obj callback)" );
//   520: 			plus();

function error$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ false, 'Ajax error JSON (POST, data obj callback)' ]);
$v.cf(plus, [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   522: 	});

})) ]);

// *** unit/ajax.js ***
//   525: test("$.ajax() - JSONP, Remote", function() {

$v.cf($v.ro('test'), [ '$.ajax() - JSONP, Remote', $v.dis(___.frozenFunc(function ($dis) {
function plus$_caller($dis) {

// *** unit/ajax.js ***
//   529: 	function plus(){ if ( ++count == 4 ) start(); }

if (++count == 4) $v.cf($v.ro('start'), [ ]);
}
___.func(plus$_caller, 'plus$_caller');
var plus;
;
plus = $v.dis(___.primFreeze(plus$_caller), 'plus');
$v.cf($v.ro('expect'), [ 4 ]);

// *** unit/ajax.js ***
//   528: 	var count = 0;

var count = 0;
;

// *** unit/ajax.js ***
//   531: 	var base = window.location.href.replace(/\?.*$/, "");

var base = $v.cm($v.r($v.r($v.ro('window'), 'location'), 'href'), 'replace', [ $v.construct(RegExp, [ '\\?.*$' ]), '' ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   536: 		url: base + "data/jsonp.php",

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', base + 'data/jsonp.php', 'dataType', 'jsonp', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   538: 		success: function(data){
//   539: 			ok( data.data, "JSON results returned (GET, no callback)" );
//   540: 			plus();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.r(data, 'data'), 'JSON results returned (GET, no callback)' ]);
$v.cf(plus, [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   542: 		error: function(data){
//   543: 			ok( false, "Ajax error JSON (GET, no callback)" );
//   544: 			plus();

function error$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ false, 'Ajax error JSON (GET, no callback)' ]);
$v.cf(plus, [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   549: 		url: base + "data/jsonp.php?callback=?",

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', base + 'data/jsonp.php?callback=?', 'dataType', 'jsonp', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   551: 		success: function(data){
//   552: 			ok( data.data, "JSON results returned (GET, url callback)" );
//   553: 			plus();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.r(data, 'data'), 'JSON results returned (GET, url callback)' ]);
$v.cf(plus, [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   555: 		error: function(data){
//   556: 			ok( false, "Ajax error JSON (GET, url callback)" );
//   557: 			plus();

function error$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ false, 'Ajax error JSON (GET, url callback)' ]);
$v.cf(plus, [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   562: 		url: base + "data/jsonp.php",

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', base + 'data/jsonp.php', 'dataType', 'jsonp', 'data', 'callback=?', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   565: 		success: function(data){
//   566: 			ok( data.data, "JSON results returned (GET, data callback)" );
//   567: 			plus();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.r(data, 'data'), 'JSON results returned (GET, data callback)' ]);
$v.cf(plus, [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   569: 		error: function(data){
//   570: 			ok( false, "Ajax error JSON (GET, data callback)" );
//   571: 			plus();

function error$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ false, 'Ajax error JSON (GET, data callback)' ]);
$v.cf(plus, [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   576: 		url: base + "data/jsonp.php",

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', base + 'data/jsonp.php', 'dataType', 'jsonp', 'jsonp', 'callback', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   579: 		success: function(data){
//   580: 			ok( data.data, "JSON results returned (GET, data obj callback)" );
//   581: 			plus();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.r(data, 'data'), 'JSON results returned (GET, data obj callback)' ]);
$v.cf(plus, [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___(), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   583: 		error: function(data){
//   584: 			ok( false, "Ajax error JSON (GET, data obj callback)" );
//   585: 			plus();

function error$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ false, 'Ajax error JSON (GET, data obj callback)' ]);
$v.cf(plus, [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   587: 	});

})) ]);

// *** unit/ajax.js ***
//   590: test("$.ajax() - script, Remote", function() {

$v.cf($v.ro('test'), [ '$.ajax() - script, Remote', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);

// *** unit/ajax.js ***
//   593: 	var base = window.location.href.replace(/\?.*$/, "");

var base = $v.cm($v.r($v.r($v.ro('window'), 'location'), 'href'), 'replace', [ $v.construct(RegExp, [ '\\?.*$' ]), '' ]);
$v.cf($v.ro('stop'), [ ]);
$v.s($v.ro('window'), 'foobar', null);

// *** unit/ajax.js ***
//   599: 		url: base + "data/test.js",

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', base + 'data/test.js', 'dataType', 'script', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   601: 		success: function(data){
//   602: 			ok( foobar, "Script results returned (GET, no callback)" );
//   603: 			start();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.ro('foobar'), 'Script results returned (GET, no callback)' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   605: 	});

})) ]);

// *** unit/ajax.js ***
//   608: test("$.ajax() - script, Remote with POST", function() {

$v.cf($v.ro('test'), [ '$.ajax() - script, Remote with POST', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);

// *** unit/ajax.js ***
//   611: 	var base = window.location.href.replace(/\?.*$/, "");

var base = $v.cm($v.r($v.r($v.ro('window'), 'location'), 'href'), 'replace', [ $v.construct(RegExp, [ '\\?.*$' ]), '' ]);
$v.cf($v.ro('stop'), [ ]);
$v.s($v.ro('window'), 'foobar', null);

// *** unit/ajax.js ***
//   617: 		url: base + "data/test.js",

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', base + 'data/test.js', 'type', 'POST', 'dataType', 'script', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   620: 		success: function(data, status){
//   621: 			ok( foobar, "Script results returned (GET, no callback)" );
//   622: 			equals( status, "success", "Script results returned (GET, no callback)" );
//   623: 			start();

function success$_lit$($dis, data, status) {
$v.cf($v.ro('ok'), [ $v.ro('foobar'), 'Script results returned (GET, no callback)' ]);
$v.cf($v.ro('equals'), [ status, 'success', 'Script results returned (GET, no callback)' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   625: 	});

})) ]);

// *** unit/ajax.js ***
//   628: test("$.ajax() - script, Remote with scheme-less URL", function() {

$v.cf($v.ro('test'), [ '$.ajax() - script, Remote with scheme-less URL', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);

// *** unit/ajax.js ***
//   631: 	var base = window.location.href.replace(/\?.*$/, "");

var base = $v.cm($v.r($v.r($v.ro('window'), 'location'), 'href'), 'replace', [ $v.construct(RegExp, [ '\\?.*$' ]), '' ]);

// *** unit/ajax.js ***
//   632: 	base = base.replace(/^.*?\/\//, "//");

base = $v.cm(base, 'replace', [ $v.construct(RegExp, [ '^.*?\\/\\/' ]), '//' ]);
$v.cf($v.ro('stop'), [ ]);
$v.s($v.ro('window'), 'foobar', null);

// *** unit/ajax.js ***
//   638: 		url: base + "data/test.js",

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', base + 'data/test.js', 'dataType', 'script', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   640: 		success: function(data){
//   641: 			ok( foobar, "Script results returned (GET, no callback)" );
//   642: 			start();

function success$_lit$($dis, data) {
$v.cf($v.ro('ok'), [ $v.ro('foobar'), 'Script results returned (GET, no callback)' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   644: 	});

})) ]);

// *** unit/ajax.js ***
//   647: test("$.getJSON(String, Hash, Function) - JSON array", function() {

$v.cf($v.ro('test'), [ '$.getJSON(String, Hash, Function) - JSON array', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   650: 	$.getJSON(url("data/json.php"), {json: "array"}, function(json) {

$v.cm($v.ro('$'), 'getJSON', [ $v.cf($v.ro('url'), [ 'data/json.php' ]), ___.initializeMap([ 'json', 'array' ]), $v.dis(___.frozenFunc(function ($dis, json) {

// *** unit/ajax.js ***
//   651: 	  equals( json[0].name, 'John', 'Check JSON: first, name' );

$v.cf($v.ro('equals'), [ $v.r($v.r(json, 0), 'name'), 'John', 'Check JSON: first, name' ]);

// *** unit/ajax.js ***
//   652: 	  equals( json[0].age, 21, 'Check JSON: first, age' );

$v.cf($v.ro('equals'), [ $v.r($v.r(json, 0), 'age'), 21, 'Check JSON: first, age' ]);

// *** unit/ajax.js ***
//   653: 	  equals( json[1].name, 'Peter', 'Check JSON: second, name' );

$v.cf($v.ro('equals'), [ $v.r($v.r(json, 1), 'name'), 'Peter', 'Check JSON: second, name' ]);

// *** unit/ajax.js ***
//   654: 	  equals( json[1].age, 25, 'Check JSON: second, age' );

$v.cf($v.ro('equals'), [ $v.r($v.r(json, 1), 'age'), 25, 'Check JSON: second, age' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//   655: 	  start();

})) ]);

// *** unit/ajax.js ***
//   656: 	});

})) ]);

// *** unit/ajax.js ***
//   659: test("$.getJSON(String, Function) - JSON object", function() {

$v.cf($v.ro('test'), [ '$.getJSON(String, Function) - JSON object', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   662: 	$.getJSON(url("data/json.php"), function(json) {

$v.cm($v.ro('$'), 'getJSON', [ $v.cf($v.ro('url'), [ 'data/json.php' ]), $v.dis(___.frozenFunc(function ($dis, json) {

// *** unit/ajax.js ***
//   663: 	  equals( json.data.lang, 'en', 'Check JSON: lang' );

$v.cf($v.ro('equals'), [ $v.r($v.r(json, 'data'), 'lang'), 'en', 'Check JSON: lang' ]);

// *** unit/ajax.js ***
//   664: 	  equals( json.data.length, 25, 'Check JSON: length' );

$v.cf($v.ro('equals'), [ $v.r($v.r(json, 'data'), 'length'), 25, 'Check JSON: length' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//   665: 	  start();

})) ]);

// *** unit/ajax.js ***
//   666: 	});

})) ]);

// *** unit/ajax.js ***
//   669: test("$.getJSON(String, Function) - JSON object with absolute url to local content", function() {

$v.cf($v.ro('test'), [ '$.getJSON(String, Function) - JSON object with absolute url to local content', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);

// *** unit/ajax.js ***
//   672: 	var base = window.location.href.replace(/\?.*$/, "");

var base = $v.cm($v.r($v.r($v.ro('window'), 'location'), 'href'), 'replace', [ $v.construct(RegExp, [ '\\?.*$' ]), '' ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   675: 	$.getJSON(url(base + "data/json.php"), function(json) {

$v.cm($v.ro('$'), 'getJSON', [ $v.cf($v.ro('url'), [ base + 'data/json.php' ]), $v.dis(___.frozenFunc(function ($dis, json) {

// *** unit/ajax.js ***
//   676: 	  equals( json.data.lang, 'en', 'Check JSON: lang' );

$v.cf($v.ro('equals'), [ $v.r($v.r(json, 'data'), 'lang'), 'en', 'Check JSON: lang' ]);

// *** unit/ajax.js ***
//   677: 	  equals( json.data.length, 25, 'Check JSON: length' );

$v.cf($v.ro('equals'), [ $v.r($v.r(json, 'data'), 'length'), 25, 'Check JSON: length' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//   678: 	  start();

})) ]);

// *** unit/ajax.js ***
//   679: 	});

})) ]);

// *** unit/ajax.js ***
//   682: test("$.post(String, Hash, Function) - simple with xml", function() {

$v.cf($v.ro('test'), [ '$.post(String, Hash, Function) - simple with xml', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   685: 	$.post(url("data/name.php"), {xml: "5-2"}, function(xml){

$v.cm($v.ro('$'), 'post', [ $v.cf($v.ro('url'), [ 'data/name.php' ]), ___.initializeMap([ 'xml', '5-2' ]), $v.dis(___.frozenFunc(function ($dis, xml) {

// *** unit/ajax.js ***
//   686: 	  $('math', xml).each(function() {

$v.cm($v.cf($v.ro('$'), [ 'math', xml ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/ajax.js ***
//   687: 		    equals( $('calculation', this).text(), '5-2', 'Check for XML' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'calculation', $dis ]), 'text', [ ]), '5-2', 'Check for XML' ]);
$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'result', $dis ]), 'text', [ ]), '3', 'Check for XML' ]);

// *** unit/ajax.js ***
//   688: 		    equals( $('result', this).text(), '3', 'Check for XML' );

})) ]);

// *** unit/ajax.js ***
//   689: 		 });

})) ]);

// *** unit/ajax.js ***
//   692: 	$.post(url("data/name.php?xml=5-2"), {}, function(xml){

$v.cm($v.ro('$'), 'post', [ $v.cf($v.ro('url'), [ 'data/name.php?xml=5-2' ]), ___.initializeMap([ ]), $v.dis(___.frozenFunc(function ($dis, xml) {

// *** unit/ajax.js ***
//   693: 	  $('math', xml).each(function() {

$v.cm($v.cf($v.ro('$'), [ 'math', xml ]), 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/ajax.js ***
//   694: 		    equals( $('calculation', this).text(), '5-2', 'Check for XML' );

$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'calculation', $dis ]), 'text', [ ]), '5-2', 'Check for XML' ]);
$v.cf($v.ro('equals'), [ $v.cm($v.cf($v.ro('$'), [ 'result', $dis ]), 'text', [ ]), '3', 'Check for XML' ]);

// *** unit/ajax.js ***
//   695: 		    equals( $('result', this).text(), '3', 'Check for XML' );

})) ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/ajax.js ***
//   697: 	  start();

})) ]);

// *** unit/ajax.js ***
//   698: 	});

})) ]);

// *** unit/ajax.js ***
//   701: test("$.ajaxSetup({timeout: Number}) - with global timeout", function() {

$v.cf($v.ro('test'), [ '$.ajaxSetup({timeout: Number}) - with global timeout', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   704: 	var passed = 0;

var passed = 0;

// *** unit/ajax.js ***
//   706: 	$.ajaxSetup({timeout: 1000});

$v.cm($v.ro('$'), 'ajaxSetup', [ ___.initializeMap([ 'timeout', 1000 ]) ]);

// *** unit/ajax.js ***
//   708: 	var pass = function() {
//   715: 	};

var pass = ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   711: 			ok( true, 'Check local and global callbacks after timeout' );
//   712: 	     	$('#main').unbind("ajaxError");
//   713: 			start();
//   714: 		}

function pass$_var$($dis) {

// *** unit/ajax.js ***
//   709: 		passed++;

passed++;

// *** unit/ajax.js ***
//   710: 		if ( passed == 2 ) {

if (passed == 2) {
$v.cf($v.ro('ok'), [ true, 'Check local and global callbacks after timeout' ]);
$v.cm($v.cf($v.ro('$'), [ '#main' ]), 'unbind', [ 'ajaxError' ]);
$v.cf($v.ro('start'), [ ]);
}
}
___.func(pass$_var$, 'pass$_var$');
;
var pass$_var = $v.dis(___.primFreeze(pass$_var$), 'pass$_var');
return pass$_var;
}).CALL___();

// *** unit/ajax.js ***
//   720: 	};

var fail = ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   717: 	var fail = function(a,b,c) {
//   718: 		ok( false, 'Check for timeout failed ' + a + ' ' + b );
//   719: 		start();

function fail$_var$($dis, a, b, c) {
$v.cf($v.ro('ok'), [ false, 'Check for timeout failed ' + a + ' ' + b ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(fail$_var$, 'fail$_var$');
;
var fail$_var = $v.dis(___.primFreeze(fail$_var$), 'fail$_var');
return fail$_var;
}).CALL___();

// *** unit/ajax.js ***
//   722: 	$('#main').ajaxError(pass);

$v.cm($v.cf($v.ro('$'), [ '#main' ]), 'ajaxError', [ pass ]);

// *** unit/ajax.js ***
//   726: 	  url: url("data/name.php?wait=5"),

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'type', 'GET', 'url', $v.cf($v.ro('url'), [ 'data/name.php?wait=5' ]), 'error', pass, 'success', fail ]) ]);
$v.cm($v.ro('$'), 'ajaxSetup', [ ___.initializeMap([ 'timeout', 0 ]) ]);

// *** unit/ajax.js ***
//   732: 	$.ajaxSetup({timeout: 0});

})) ]);

// *** unit/ajax.js ***
//   735: test("$.ajaxSetup({timeout: Number}) with localtimeout", function() {

$v.cf($v.ro('test'), [ '$.ajaxSetup({timeout: Number}) with localtimeout', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   737: 	$.ajaxSetup({timeout: 50});

$v.cm($v.ro('$'), 'ajaxSetup', [ ___.initializeMap([ 'timeout', 50 ]) ]);

// *** unit/ajax.js ***
//   742: 	  url: url("data/name.php?wait=1"),

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'type', 'GET', 'timeout', 5000, 'url', $v.cf($v.ro('url'), [ 'data/name.php?wait=1' ]), 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   744: 		   ok( false, 'Check for local timeout failed' );
//   745: 		   start();

function error$_lit$($dis) {
$v.cf($v.ro('ok'), [ false, 'Check for local timeout failed' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___(), 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   748: 	    ok( true, 'Check for local timeout' );
//   749: 	    start();

function success$_lit$($dis) {
$v.cf($v.ro('ok'), [ true, 'Check for local timeout' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);
$v.cm($v.ro('$'), 'ajaxSetup', [ ___.initializeMap([ 'timeout', 0 ]) ]);

// *** unit/ajax.js ***
//   754: 	$.ajaxSetup({timeout: 0});

})) ]);

// *** unit/ajax.js ***
//   757: test("$.ajax - simple get", function() {

$v.cf($v.ro('test'), [ '$.ajax - simple get', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   762: 	  url: url("data/name.php?name=foo"),

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'type', 'GET', 'url', $v.cf($v.ro('url'), [ 'data/name.php?name=foo' ]), 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   763: 	  success: function(msg){
//   764: 	    equals( msg, 'bar', 'Check for GET' );
//   765: 	    start();

function success$_lit$($dis, msg) {
$v.cf($v.ro('equals'), [ msg, 'bar', 'Check for GET' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   767: 	});

})) ]);

// *** unit/ajax.js ***
//   770: test("$.ajax - simple post", function() {

$v.cf($v.ro('test'), [ '$.ajax - simple post', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   775: 	  url: url("data/name.php"),

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'type', 'POST', 'url', $v.cf($v.ro('url'), [ 'data/name.php' ]), 'data', 'name=peter', 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   777: 	  success: function(msg){
//   778: 	    equals( msg, 'pan', 'Check for POST' );
//   779: 	    start();

function success$_lit$($dis, msg) {
$v.cf($v.ro('equals'), [ msg, 'pan', 'Check for POST' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   781: 	});

})) ]);
$v.cf($v.ro('test'), [ 'ajaxSetup()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   788: 		url: url("data/name.php?name=foo"),

$v.cm($v.ro('$'), 'ajaxSetup', [ ___.initializeMap([ 'url', $v.cf($v.ro('url'), [ 'data/name.php?name=foo' ]), 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   789: 		success: function(msg){
//   790: 	    	equals( msg, 'bar', 'Check for GET' );
//   791: 			start();

function success$_lit$($dis, msg) {
$v.cf($v.ro('equals'), [ msg, 'bar', 'Check for GET' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);
$v.cm($v.ro('$'), 'ajax', [ ]);

// *** unit/ajax.js ***
//   794: 	$.ajax();

})) ]);

// *** unit/ajax.js ***
//   797: test("custom timeout does not set error message when timeout occurs, see #970", function() {

$v.cf($v.ro('test'), [ 'custom timeout does not set error message when timeout occurs, see #970', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   800: 		url: "data/name.php?wait=10",

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/name.php?wait=10', 'timeout', 500, 'error', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   802: 		error: function(request, status) {
//   803: 			ok( status != null, "status shouldn't be null in error handler" );
//   804: 			equals( "timeout", status );
//   805: 			start();

function error$_lit$($dis, request, status) {
$v.cf($v.ro('ok'), [ status != null, 'status shouldn\'t be null in error handler' ]);
$v.cf($v.ro('equals'), [ 'timeout', status ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(error$_lit$, 'error$_lit$');
;
var error$_lit = $v.dis(___.primFreeze(error$_lit$), 'error$_lit');
return error$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   807: 	});

})) ]);

// *** unit/ajax.js ***
//   810: test("data option: evaluate function values (#2806)", function() {

$v.cf($v.ro('test'), [ 'data option: evaluate function values (#2806)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('stop'), [ ]);

// *** unit/ajax.js ***
//   813: 		url: "data/echoQuery.php",

$v.cm($v.ro('$'), 'ajax', [ ___.initializeMap([ 'url', 'data/echoQuery.php', 'data', ___.initializeMap([ 'key', ___.frozenFunc(function () {
function key$_lit$($dis) {

// *** unit/ajax.js ***
//   816: 				return "value";

return 'value';
}
___.func(key$_lit$, 'key$_lit$');
;
var key$_lit = $v.dis(___.primFreeze(key$_lit$), 'key$_lit');
return key$_lit;
}).CALL___() ]), 'success', ___.frozenFunc(function () {

// *** unit/ajax.js ***
//   819: 		success: function(result) {
//   820: 			equals( result, "key=value" );
//   821: 			start();

function success$_lit$($dis, result) {
$v.cf($v.ro('equals'), [ result, 'key=value' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(success$_lit$, 'success$_lit$');
;
var success$_lit = $v.dis(___.primFreeze(success$_lit$), 'success$_lit');
return success$_lit;
}).CALL___() ]) ]);

// *** unit/ajax.js ***
//   823: 	})

})) ]);
}
}
} catch (ex___) {
___.getNewModuleHandler().handleUncaughtException(ex___, $v.ro('onerror'), 'unit/ajax.js', '1');
}
IMPORTS___.htmlEmitter___.pc('\n\t');

// *** unit/fx.js ***
//     1: module("fx");
//     2: 
//     4: 	expect(1);
//     5: 	stop();
//    12: });
//    13: 
//    15: 	expect(1);
//    16: 	stop();
//    17: 
//    19: 
//    22: 		// should finish after unqueued animation so second
//    24: 	});
//    26: 		// short duration and out of queue so should finish first
//    28: 	}});
//    30: 		// queued behind the first animation so should finish third 
//    31: 		order.push(3);
//    35: });
//    36: 
//    38: 	expect(2);
//    39: 	stop();
//    40: 
//    48: 	// clean up after test
//    49: 	$foo.queue([]);
//    50: 
//    52: });
//    53: 
//    54: test("stop()", function() {
//    55: 	expect(3);
//    56: 	stop();
//    57: 
//    61: 
//    63: 	setTimeout(function(){
//    66: 		$foo.stop();
//    67: 
//    70: 		setTimeout(function(){
//    75: });
//    76: 
//    78: 	expect(4);
//    79: 	stop();
//    80: 
//    84: 
//    88: 	setTimeout(function(){
//    92: 		$foo.stop();
//    93: 
//    97: 		$foo.stop(true);
//   100: });
//   101: 
//   103: 	expect(4);
//   104: 	stop();
//   105: 
//   109: 
//   113: 	setTimeout(function(){
//   116: 		$foo.stop(true);
//   117: 
//   120: 
//   122: 		setTimeout(function(){
//   127: });
//   128: 
//   130: 	expect(3);
//   131: 	stop();
//   132: 
//   136: 
//   141: 	setTimeout(function(){
//   145: 
//   148: 
//   149: 		setTimeout(function(){
//   151: 			$foo.stop(true);
//   155: });
//   156: 
//   157: test("toggle()", function() {
//   158: 	expect(3);
//   161: 	x.toggle();
//   163: 	x.toggle();
//   165: });
//   166: 
//   167: var visible = {
//   171: 	},
//   174: 	}
//   175: };
//   176: 
//   177: var from = {
//   182: 	},
//   187: 	},
//   191: 	},
//   195: 	},
//   199: 	},
//   203: 	},
//   207: 	},
//   211: 	}
//   212: };
//   213: 
//   214: var to = {
//   218: 	},
//   222: 	},
//   226: 	},
//   229: 	},
//   233: 	}
//   234: };
//   235: 
//   243: }
//   244: 
//   246: 	expect(2);
//   247: 	stop();
//   256: });
//   257: 		
//   259: 	expect(2);
//   260: 	stop();
//   268: });
//   269: 
//   274: 	
//   281: 			
//   283: 			
//   293: 			
//   294: 			expect(num);
//   295: 			stop();
//   296: 	
//   298: 	
//   302: 					
//   305: 					
//   308: 					
//   311: 	
//   314: 					
//   317: 					
//   323: 					
//   331: 					
//   339: 				
//   345: 	
//   350: });
//   351: 
//   353: 
//   364: };
//   365: 
//   373: }
//   374: 
//   375: // Chaining Tests
//   378: });
//   381: });
//   382: 
//   385: });
//   388: });
//   389: 
//   392: });
//   395: });
//   396: 
//   399: });
//   402: });
//   403: 
//   406: });
//   409: });
//   410: 
//   425: }
//   426: 
//   427: makeTest.id = 1;

try {
{
$v.so('checkOverflowDisplay', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   238: 
//   239: 	equals(o, "visible", "Overflow should be visible: " + o);
//   241: 
//   242: 	start();

function checkOverflowDisplay$_caller($dis) {

// *** unit/fx.js ***
//   237: 	var o = jQuery.css( this, "overflow" );

var o = $v.cm($v.ro('jQuery'), 'css', [ $dis, 'overflow' ]);
$v.cf($v.ro('equals'), [ o, 'visible', 'Overflow should be visible: ' + o ]);

// *** unit/fx.js ***
//   240: 	equals(jQuery.css( this, "display" ), "inline", "Display shouldn't be tampered with.");

$v.cf($v.ro('equals'), [ $v.cm($v.ro('jQuery'), 'css', [ $dis, 'display' ]), 'inline', 'Display shouldn\'t be tampered with.' ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(checkOverflowDisplay$_caller, 'checkOverflowDisplay$_caller');
var checkOverflowDisplay;
;

// *** unit/fx.js ***
//   236: function checkOverflowDisplay(){

checkOverflowDisplay = $v.dis(___.primFreeze(checkOverflowDisplay$_caller), 'checkOverflowDisplay');
return checkOverflowDisplay;
}).CALL___());
$v.so('checkState', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   368: 	jQuery.each(this.save, function(c,v){
//   371: 	});
//   372: 	start();

function checkState$_caller($dis) {

// *** unit/fx.js ***
//   367: 	var self = this;

var self = $dis;
$v.cm($v.ro('jQuery'), 'each', [ $v.r($dis, 'save'), $v.dis(___.frozenFunc(function ($dis, c, v) {

// *** unit/fx.js ***
//   369: 		var cur = jQuery.css(self,c);

var cur = $v.cm($v.ro('jQuery'), 'css', [ self, c ]);
$v.cf($v.ro('equals'), [ v, cur, 'Make sure that ' + c + ' is reset (Old: ' + v + ' Cur: ' + cur + ')' ]);

// *** unit/fx.js ***
//   370: 		equals( v, cur, "Make sure that " + c + " is reset (Old: " + v + " Cur: " + cur + ")");

})) ]);
$v.cf($v.ro('start'), [ ]);
}
___.func(checkState$_caller, 'checkState$_caller');
var checkState;
;

// *** unit/fx.js ***
//   366: function checkState(){

checkState = $v.dis(___.primFreeze(checkState$_caller), 'checkState');
return checkState;
}).CALL___());
$v.so('makeTest', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   415: 
//   416: 	$("<h4><�h4>")
//   417: 		.text( text )
//   418: 		.appendTo("#fx-tests")
//   419: 		.click(function(){
//   421: 		})
//   422: 		.after( elem );
//   423: 

function makeTest$_caller($dis, text) {
var x0___;

// *** unit/fx.js ***
//   412: 	var elem = $("<div><�div>")
//   413: 		.attr("id", "test" + makeTest.id++)
//   414: 		.addClass("box");

var elem = $v.cm($v.cm($v.cf($v.ro('$'), [ '\x3cdiv\x3e\x3c/div\x3e' ]), 'attr', [ 'id', 'test' + (void 0, x0___ = +$v.r(makeTest, 'id'), $v.s(makeTest, 'id', x0___ + 1), x0___) ]), 'addClass', [ 'box' ]);
$v.cm($v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '\x3ch4\x3e\x3c/h4\x3e' ]), 'text', [ text ]), 'appendTo', [ '#fx-tests' ]), 'click', [ $v.dis(___.frozenFunc(function ($dis) {
$v.cm($v.cm($v.cf($v.ro('$'), [ $dis ]), 'next', [ ]), 'toggle', [ ]);

// *** unit/fx.js ***
//   420: 			$(this).next().toggle();

})) ]), 'after', [ elem ]);

// *** unit/fx.js ***
//   424: 	return elem;

return elem;
}
___.func(makeTest$_caller, 'makeTest$_caller');
var makeTest;
;

// *** unit/fx.js ***
//   411: function makeTest( text ){

makeTest = $v.dis(___.primFreeze(makeTest$_caller), 'makeTest');
return makeTest;
}).CALL___());
$v.cf($v.ro('module'), [ 'fx' ]);

// *** unit/fx.js ***
//     3: test("animate(Hash, Object, Function)", function() {

$v.cf($v.ro('test'), [ 'animate(Hash, Object, Function)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/fx.js ***
//     6: 	var hash = {opacity: 'show'};

var hash = ___.initializeMap([ 'opacity', 'show' ]);

// *** unit/fx.js ***
//     7: 	var hashCopy = $.extend({}, hash);

var hashCopy = $v.cm($v.ro('$'), 'extend', [ ___.initializeMap([ ]), hash ]);

// *** unit/fx.js ***
//     8: 	$('#foo').animate(hash, 0, function() {

$v.cm($v.cf($v.ro('$'), [ '#foo' ]), 'animate', [ hash, 0, $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//     9: 		equals( hash.opacity, hashCopy.opacity, 'Check if animate changed the hash parameter' );

$v.cf($v.ro('equals'), [ $v.r(hash, 'opacity'), $v.r(hashCopy, 'opacity'), 'Check if animate changed the hash parameter' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/fx.js ***
//    10: 		start();

})) ]);

// *** unit/fx.js ***
//    11: 	});

})) ]);

// *** unit/fx.js ***
//    14: test("animate option (queue === false)", function () {

$v.cf($v.ro('test'), [ 'animate option (queue === false)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 1 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/fx.js ***
//    18: 	var order = [];

var order = [ ];

// *** unit/fx.js ***
//    20: 	var $foo = $("#foo");

var $foo = $v.cf($v.ro('$'), [ '#foo' ]);

// *** unit/fx.js ***
//    21: 	$foo.animate({width:'100px'}, 200, function () {

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', '100px' ]), 200, $v.dis(___.frozenFunc(function ($dis) {
$v.cm(order, 'push', [ 2 ]);

// *** unit/fx.js ***
//    23: 		order.push(2);

})) ]);

// *** unit/fx.js ***
//    25: 	$foo.animate({fontSize:'2em'}, {queue:false, duration:10, complete:function () {

$v.cm($foo, 'animate', [ ___.initializeMap([ 'fontSize', '2em' ]), ___.initializeMap([ 'queue', false, 'duration', 10, 'complete', ___.frozenFunc(function () {

// *** unit/fx.js ***
//    27: 		order.push(1);

function complete$_lit$($dis) {
$v.cm(order, 'push', [ 1 ]);
}
___.func(complete$_lit$, 'complete$_lit$');
;
var complete$_lit = $v.dis(___.primFreeze(complete$_lit$), 'complete$_lit');
return complete$_lit;
}).CALL___() ]) ]);

// *** unit/fx.js ***
//    29: 	$foo.animate({height:'100px'}, 10, function() {

$v.cm($foo, 'animate', [ ___.initializeMap([ 'height', '100px' ]), 10, $v.dis(___.frozenFunc(function ($dis) {
$v.cm(order, 'push', [ 3 ]);

// *** unit/fx.js ***
//    32: 		isSet( order, [ 1, 2, 3], "Animations finished in the correct order" );

$v.cf($v.ro('isSet'), [ order, [ 1, 2, 3 ], 'Animations finished in the correct order' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/fx.js ***
//    33: 		start();

})) ]);

// *** unit/fx.js ***
//    34: 	});

})) ]);

// *** unit/fx.js ***
//    37: test("queue() defaults to 'fx' type", function () {

$v.cf($v.ro('test'), [ 'queue() defaults to \'fx\' type', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/fx.js ***
//    41: 	var $foo = $("#foo");

var $foo = $v.cf($v.ro('$'), [ '#foo' ]);

// *** unit/fx.js ***
//    42: 	$foo.queue("fx", [ "sample", "array" ]);

$v.cm($foo, 'queue', [ 'fx', [ 'sample', 'array' ] ]);

// *** unit/fx.js ***
//    43: 	var arr = $foo.queue();

var arr = $v.cm($foo, 'queue', [ ]);

// *** unit/fx.js ***
//    44: 	isSet(arr, [ "sample", "array" ], "queue() got an array set with type 'fx'");

$v.cf($v.ro('isSet'), [ arr, [ 'sample', 'array' ], 'queue() got an array set with type \'fx\'' ]);

// *** unit/fx.js ***
//    45: 	$foo.queue([ "another", "one" ]);

$v.cm($foo, 'queue', [ [ 'another', 'one' ] ]);

// *** unit/fx.js ***
//    46: 	var arr = $foo.queue("fx");

var arr = $v.cm($foo, 'queue', [ 'fx' ]);

// *** unit/fx.js ***
//    47: 	isSet(arr, [ "another", "one" ], "queue('fx') got an array set with no type");

$v.cf($v.ro('isSet'), [ arr, [ 'another', 'one' ], 'queue(\'fx\') got an array set with no type' ]);
$v.cm($foo, 'queue', [ [ ] ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/fx.js ***
//    51: 	start();

})) ]);
$v.cf($v.ro('test'), [ 'stop()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/fx.js ***
//    58: 	var $foo = $("#nothiddendiv");

var $foo = $v.cf($v.ro('$'), [ '#nothiddendiv' ]);

// *** unit/fx.js ***
//    59: 	var w = 0;

var w = 0;

// *** unit/fx.js ***
//    60: 	$foo.hide().width(200).width();

$v.cm($v.cm($v.cm($foo, 'hide', [ ]), 'width', [ 200 ]), 'width', [ ]);

// *** unit/fx.js ***
//    62: 	$foo.animate({ width:'show' }, 1000);

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', 'show' ]), 1000 ]);
$v.cf($v.ro('setTimeout'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//    64: 		var nw = $foo.width();

var nw = $v.cm($foo, 'width', [ ]);

// *** unit/fx.js ***
//    65: 		ok( nw != w, "An animation occurred " + nw + "px " + w + "px");

$v.cf($v.ro('ok'), [ nw != w, 'An animation occurred ' + nw + 'px ' + w + 'px' ]);
$v.cm($foo, 'stop', [ ]);

// *** unit/fx.js ***
//    68: 		nw = $foo.width();

nw = $v.cm($foo, 'width', [ ]);

// *** unit/fx.js ***
//    69: 		ok( nw != w, "Stop didn't reset the animation " + nw + "px " + w + "px");

$v.cf($v.ro('ok'), [ nw != w, 'Stop didn\'t reset the animation ' + nw + 'px ' + w + 'px' ]);
$v.cf($v.ro('setTimeout'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//    71: 			equals( nw, $foo.width(), "The animation didn't continue" );

$v.cf($v.ro('equals'), [ nw, $v.cm($foo, 'width', [ ]), 'The animation didn\'t continue' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/fx.js ***
//    72: 			start();

})), 100 ]);

// *** unit/fx.js ***
//    73: 		}, 100);

})), 100 ]);

// *** unit/fx.js ***
//    74: 	}, 100);

})) ]);

// *** unit/fx.js ***
//    77: test("stop() - several in queue", function() {

$v.cf($v.ro('test'), [ 'stop() - several in queue', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/fx.js ***
//    81: 	var $foo = $("#nothiddendiv");

var $foo = $v.cf($v.ro('$'), [ '#nothiddendiv' ]);

// *** unit/fx.js ***
//    82: 	var w = 0;

var w = 0;

// *** unit/fx.js ***
//    83: 	$foo.hide().width(200).width();

$v.cm($v.cm($v.cm($foo, 'hide', [ ]), 'width', [ 200 ]), 'width', [ ]);

// *** unit/fx.js ***
//    85: 	$foo.animate({ width:'show' }, 1000);

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', 'show' ]), 1000 ]);

// *** unit/fx.js ***
//    86: 	$foo.animate({ width:'hide' }, 1000);

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', 'hide' ]), 1000 ]);

// *** unit/fx.js ***
//    87: 	$foo.animate({ width:'show' }, 1000);

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', 'show' ]), 1000 ]);
$v.cf($v.ro('setTimeout'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//    89: 		equals( $foo.queue().length, 3, "All 3 still in the queue" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($foo, 'queue', [ ]), 'length'), 3, 'All 3 still in the queue' ]);

// *** unit/fx.js ***
//    90: 		var nw = $foo.width();

var nw = $v.cm($foo, 'width', [ ]);

// *** unit/fx.js ***
//    91: 		ok( nw != w, "An animation occurred " + nw + "px " + w + "px");

$v.cf($v.ro('ok'), [ nw != w, 'An animation occurred ' + nw + 'px ' + w + 'px' ]);
$v.cm($foo, 'stop', [ ]);

// *** unit/fx.js ***
//    94: 		nw = $foo.width();

nw = $v.cm($foo, 'width', [ ]);

// *** unit/fx.js ***
//    95: 		ok( nw != w, "Stop didn't reset the animation " + nw + "px " + w + "px");

$v.cf($v.ro('ok'), [ nw != w, 'Stop didn\'t reset the animation ' + nw + 'px ' + w + 'px' ]);

// *** unit/fx.js ***
//    96: 		equals( $foo.queue().length, 2, "The next animation continued" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($foo, 'queue', [ ]), 'length'), 2, 'The next animation continued' ]);
$v.cm($foo, 'stop', [ true ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/fx.js ***
//    98: 		start();

})), 100 ]);

// *** unit/fx.js ***
//    99: 	}, 100);

})) ]);

// *** unit/fx.js ***
//   102: test("stop(clearQueue)", function() {

$v.cf($v.ro('test'), [ 'stop(clearQueue)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 4 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/fx.js ***
//   106: 	var $foo = $("#nothiddendiv");

var $foo = $v.cf($v.ro('$'), [ '#nothiddendiv' ]);

// *** unit/fx.js ***
//   107: 	var w = 0;

var w = 0;

// *** unit/fx.js ***
//   108: 	$foo.hide().width(200).width();

$v.cm($v.cm($v.cm($foo, 'hide', [ ]), 'width', [ 200 ]), 'width', [ ]);

// *** unit/fx.js ***
//   110: 	$foo.animate({ width:'show' }, 1000);

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', 'show' ]), 1000 ]);

// *** unit/fx.js ***
//   111: 	$foo.animate({ width:'hide' }, 1000);

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', 'hide' ]), 1000 ]);

// *** unit/fx.js ***
//   112: 	$foo.animate({ width:'show' }, 1000);

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', 'show' ]), 1000 ]);
$v.cf($v.ro('setTimeout'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   114: 		var nw = $foo.width();

var nw = $v.cm($foo, 'width', [ ]);

// *** unit/fx.js ***
//   115: 		ok( nw != w, "An animation occurred " + nw + "px " + w + "px");

$v.cf($v.ro('ok'), [ nw != w, 'An animation occurred ' + nw + 'px ' + w + 'px' ]);
$v.cm($foo, 'stop', [ true ]);

// *** unit/fx.js ***
//   118: 		nw = $foo.width();

nw = $v.cm($foo, 'width', [ ]);

// *** unit/fx.js ***
//   119: 		ok( nw != w, "Stop didn't reset the animation " + nw + "px " + w + "px");

$v.cf($v.ro('ok'), [ nw != w, 'Stop didn\'t reset the animation ' + nw + 'px ' + w + 'px' ]);

// *** unit/fx.js ***
//   121: 		equals( $foo.queue().length, 0, "The animation queue was cleared" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($foo, 'queue', [ ]), 'length'), 0, 'The animation queue was cleared' ]);
$v.cf($v.ro('setTimeout'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   123: 			equals( nw, $foo.width(), "The animation didn't continue" );

$v.cf($v.ro('equals'), [ nw, $v.cm($foo, 'width', [ ]), 'The animation didn\'t continue' ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/fx.js ***
//   124: 			start();

})), 100 ]);

// *** unit/fx.js ***
//   125: 		}, 100);

})), 100 ]);

// *** unit/fx.js ***
//   126: 	}, 100);

})) ]);

// *** unit/fx.js ***
//   129: test("stop(clearQueue, gotoEnd)", function() {

$v.cf($v.ro('test'), [ 'stop(clearQueue, gotoEnd)', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/fx.js ***
//   133: 	var $foo = $("#nothiddendiv");

var $foo = $v.cf($v.ro('$'), [ '#nothiddendiv' ]);

// *** unit/fx.js ***
//   134: 	var w = 0;

var w = 0;

// *** unit/fx.js ***
//   135: 	$foo.hide().width(200).width();

$v.cm($v.cm($v.cm($foo, 'hide', [ ]), 'width', [ 200 ]), 'width', [ ]);

// *** unit/fx.js ***
//   137: 	$foo.animate({ width:'show' }, 1000);

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', 'show' ]), 1000 ]);

// *** unit/fx.js ***
//   138: 	$foo.animate({ width:'hide' }, 1000);

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', 'hide' ]), 1000 ]);

// *** unit/fx.js ***
//   139: 	$foo.animate({ width:'show' }, 1000);

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', 'show' ]), 1000 ]);

// *** unit/fx.js ***
//   140: 	$foo.animate({ width:'hide' }, 1000);

$v.cm($foo, 'animate', [ ___.initializeMap([ 'width', 'hide' ]), 1000 ]);
$v.cf($v.ro('setTimeout'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   142: 		var nw = $foo.width();

var nw = $v.cm($foo, 'width', [ ]);

// *** unit/fx.js ***
//   143: 		ok( nw != w, "An animation occurred " + nw + "px " + w + "px");

$v.cf($v.ro('ok'), [ nw != w, 'An animation occurred ' + nw + 'px ' + w + 'px' ]);

// *** unit/fx.js ***
//   144: 		$foo.stop(false, true);

$v.cm($foo, 'stop', [ false, true ]);

// *** unit/fx.js ***
//   146: 		nw = $foo.width();

nw = $v.cm($foo, 'width', [ ]);

// *** unit/fx.js ***
//   147: 		equals( nw, 200, "Stop() reset the animation" );

$v.cf($v.ro('equals'), [ nw, 200, 'Stop() reset the animation' ]);
$v.cf($v.ro('setTimeout'), [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   150: 			equals( $foo.queue().length, 3, "The next animation continued" );

$v.cf($v.ro('equals'), [ $v.r($v.cm($foo, 'queue', [ ]), 'length'), 3, 'The next animation continued' ]);
$v.cm($foo, 'stop', [ true ]);
$v.cf($v.ro('start'), [ ]);

// *** unit/fx.js ***
//   152: 			start();

})), 100 ]);

// *** unit/fx.js ***
//   153: 		}, 100);

})), 100 ]);

// *** unit/fx.js ***
//   154: 	}, 100);

})) ]);
$v.cf($v.ro('test'), [ 'toggle()', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 3 ]);

// *** unit/fx.js ***
//   159: 	var x = $("#foo");

var x = $v.cf($v.ro('$'), [ '#foo' ]);

// *** unit/fx.js ***
//   160: 	ok( x.is(":visible"), "is visible" );

$v.cf($v.ro('ok'), [ $v.cm(x, 'is', [ ':visible' ]), 'is visible' ]);
$v.cm(x, 'toggle', [ ]);

// *** unit/fx.js ***
//   162: 	ok( x.is(":hidden"), "is hidden" );

$v.cf($v.ro('ok'), [ $v.cm(x, 'is', [ ':hidden' ]), 'is hidden' ]);
$v.cm(x, 'toggle', [ ]);
$v.cf($v.ro('ok'), [ $v.cm(x, 'is', [ ':visible' ]), 'is visible again' ]);

// *** unit/fx.js ***
//   164: 	ok( x.is(":visible"), "is visible again" );

})) ]);
$v.so('visible', ___.initializeMap([ 'Normal', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   168: 	Normal: function(elem){},

function Normal$_lit$($dis, elem) {
}
___.func(Normal$_lit$, 'Normal$_lit$');
;
var Normal$_lit = $v.dis(___.primFreeze(Normal$_lit$), 'Normal$_lit');
return Normal$_lit;
}).CALL___(), 'CSS Hidden', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   169: 	"CSS Hidden": function(elem){
//   170: 		$(this).addClass("hidden");

function badName$_lit$($dis, elem) {
$v.cm($v.cf($v.ro('$'), [ $dis ]), 'addClass', [ 'hidden' ]);
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), 'JS Hidden', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   172: 	"JS Hidden": function(elem){
//   173: 		$(this).hide();

function badName$_lit$($dis, elem) {
$v.cm($v.cf($v.ro('$'), [ $dis ]), 'hide', [ ]);
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___() ]));
$v.so('from', ___.initializeMap([ 'CSS Auto', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   178: 	"CSS Auto": function(elem,prop){
//   179: 		$(elem).addClass("auto" + prop)
//   180: 			.text("This is a long string of text.");

function badName$_lit$($dis, elem, prop) {
$v.cm($v.cm($v.cf($v.ro('$'), [ elem ]), 'addClass', [ 'auto' + prop ]), 'text', [ 'This is a long string of text.' ]);

// *** unit/fx.js ***
//   181: 		return "";

return '';
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), 'JS Auto', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   183: 	"JS Auto": function(elem,prop){
//   184: 		$(elem).css(prop,"auto")
//   185: 			.text("This is a long string of text.");

function badName$_lit$($dis, elem, prop) {
$v.cm($v.cm($v.cf($v.ro('$'), [ elem ]), 'css', [ prop, 'auto' ]), 'text', [ 'This is a long string of text.' ]);

// *** unit/fx.js ***
//   186: 		return "";

return '';
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), 'CSS 100', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   188: 	"CSS 100": function(elem,prop){
//   189: 		$(elem).addClass("large" + prop);

function badName$_lit$($dis, elem, prop) {
$v.cm($v.cf($v.ro('$'), [ elem ]), 'addClass', [ 'large' + prop ]);

// *** unit/fx.js ***
//   190: 		return "";

return '';
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), 'JS 100', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   192: 	"JS 100": function(elem,prop){
//   193: 		$(elem).css(prop,prop == "opacity" ? 1 : "100px");

function badName$_lit$($dis, elem, prop) {
$v.cm($v.cf($v.ro('$'), [ elem ]), 'css', [ prop, prop == 'opacity'? 1: '100px' ]);

// *** unit/fx.js ***
//   194: 		return prop == "opacity" ? 1 : 100;

return prop == 'opacity'? 1: 100;
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), 'CSS 50', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   196: 	"CSS 50": function(elem,prop){
//   197: 		$(elem).addClass("med" + prop);

function badName$_lit$($dis, elem, prop) {
$v.cm($v.cf($v.ro('$'), [ elem ]), 'addClass', [ 'med' + prop ]);

// *** unit/fx.js ***
//   198: 		return "";

return '';
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), 'JS 50', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   200: 	"JS 50": function(elem,prop){
//   201: 		$(elem).css(prop,prop == "opacity" ? 0.50 : "50px");

function badName$_lit$($dis, elem, prop) {
$v.cm($v.cf($v.ro('$'), [ elem ]), 'css', [ prop, prop == 'opacity'? 0.5: '50px' ]);

// *** unit/fx.js ***
//   202: 		return prop == "opacity" ? 0.5 : 50;

return prop == 'opacity'? 0.5: 50;
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), 'CSS 0', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   204: 	"CSS 0": function(elem,prop){
//   205: 		$(elem).addClass("no" + prop);

function badName$_lit$($dis, elem, prop) {
$v.cm($v.cf($v.ro('$'), [ elem ]), 'addClass', [ 'no' + prop ]);

// *** unit/fx.js ***
//   206: 		return "";

return '';
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), 'JS 0', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   208: 	"JS 0": function(elem,prop){
//   209: 		$(elem).css(prop,prop == "opacity" ? 0 : "0px");

function badName$_lit$($dis, elem, prop) {
$v.cm($v.cf($v.ro('$'), [ elem ]), 'css', [ prop, prop == 'opacity'? 0: '0px' ]);

// *** unit/fx.js ***
//   210: 		return 0;

return 0;
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___() ]));
$v.so('to', ___.initializeMap([ 'show', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   215: 	"show": function(elem,prop){
//   216: 		$(elem).hide().addClass("wide"+prop);

function show$_lit$($dis, elem, prop) {
$v.cm($v.cm($v.cf($v.ro('$'), [ elem ]), 'hide', [ ]), 'addClass', [ 'wide' + prop ]);

// *** unit/fx.js ***
//   217: 		return "show";

return 'show';
}
___.func(show$_lit$, 'show$_lit$');
;
var show$_lit = $v.dis(___.primFreeze(show$_lit$), 'show$_lit');
return show$_lit;
}).CALL___(), 'hide', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   219: 	"hide": function(elem,prop){
//   220: 		$(elem).addClass("wide"+prop);

function hide$_lit$($dis, elem, prop) {
$v.cm($v.cf($v.ro('$'), [ elem ]), 'addClass', [ 'wide' + prop ]);

// *** unit/fx.js ***
//   221: 		return "hide";

return 'hide';
}
___.func(hide$_lit$, 'hide$_lit$');
;
var hide$_lit = $v.dis(___.primFreeze(hide$_lit$), 'hide$_lit');
return hide$_lit;
}).CALL___(), '100', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   223: 	"100": function(elem,prop){
//   224: 		$(elem).addClass("wide"+prop);

function badName$_lit$($dis, elem, prop) {
$v.cm($v.cf($v.ro('$'), [ elem ]), 'addClass', [ 'wide' + prop ]);

// *** unit/fx.js ***
//   225: 		return prop == "opacity" ? 1 : 100;

return prop == 'opacity'? 1: 100;
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), '50', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   227: 	"50": function(elem,prop){

function badName$_lit$($dis, elem, prop) {

// *** unit/fx.js ***
//   228: 		return prop == "opacity" ? 0.50 : 50;

return prop == 'opacity'? 0.5: 50;
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___(), '0', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   230: 	"0": function(elem,prop){
//   231: 		$(elem).addClass("noback");

function badName$_lit$($dis, elem, prop) {
$v.cm($v.cf($v.ro('$'), [ elem ]), 'addClass', [ 'noback' ]);

// *** unit/fx.js ***
//   232: 		return 0;

return 0;
}
___.func(badName$_lit$, 'badName$_lit$');
;
var badName$_lit = $v.dis(___.primFreeze(badName$_lit$), 'badName$_lit');
return badName$_lit;
}).CALL___() ]));
;

// *** unit/fx.js ***
//   245: test("JS Overflow and Display", function() {

$v.cf($v.ro('test'), [ 'JS Overflow and Display', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/fx.js ***
//   248: 	makeTest( "JS Overflow and Display" )
//   249: 		.addClass("widewidth")
//   250: 		.css({ overflow: "visible", display: "inline" })
//   251: 		.addClass("widewidth")
//   252: 		.text("Some sample text.")
//   253: 		.before("text before")
//   254: 		.after("text after")

$v.cm($v.cm($v.cm($v.cm($v.cm($v.cm($v.cm($v.cf($v.ro('makeTest'), [ 'JS Overflow and Display' ]), 'addClass', [ 'widewidth' ]), 'css', [ ___.initializeMap([ 'overflow', 'visible', 'display', 'inline' ]) ]), 'addClass', [ 'widewidth' ]), 'text', [ 'Some sample text.' ]), 'before', [ 'text before' ]), 'after', [ 'text after' ]), 'animate', [ ___.initializeMap([ 'opacity', 0.5 ]), 'slow', $v.ro('checkOverflowDisplay') ]);

// *** unit/fx.js ***
//   255: 		.animate({ opacity: 0.5 }, "slow", checkOverflowDisplay);

})) ]);

// *** unit/fx.js ***
//   258: test("CSS Overflow and Display", function() {

$v.cf($v.ro('test'), [ 'CSS Overflow and Display', $v.dis(___.frozenFunc(function ($dis) {
$v.cf($v.ro('expect'), [ 2 ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/fx.js ***
//   261: 	makeTest( "CSS Overflow and Display" )
//   262: 		.addClass("overflow inline")
//   263: 		.addClass("widewidth")
//   264: 		.text("Some sample text.")
//   265: 		.before("text before")
//   266: 		.after("text after")

$v.cm($v.cm($v.cm($v.cm($v.cm($v.cm($v.cf($v.ro('makeTest'), [ 'CSS Overflow and Display' ]), 'addClass', [ 'overflow inline' ]), 'addClass', [ 'widewidth' ]), 'text', [ 'Some sample text.' ]), 'before', [ 'text before' ]), 'after', [ 'text after' ]), 'animate', [ ___.initializeMap([ 'opacity', 0.5 ]), 'slow', $v.ro('checkOverflowDisplay') ]);

// *** unit/fx.js ***
//   267: 		.animate({ opacity: 0.5 }, "slow", checkOverflowDisplay);

})) ]);

// *** unit/fx.js ***
//   270: jQuery.each( from, function(fn, f){

$v.cm($v.ro('jQuery'), 'each', [ $v.ro('from'), $v.dis(___.frozenFunc(function ($dis, fn, f) {

// *** unit/fx.js ***
//   271: 	jQuery.each( to, function(tn, t){

$v.cm($v.ro('jQuery'), 'each', [ $v.ro('to'), $v.dis(___.frozenFunc(function ($dis, tn, t) {

// *** unit/fx.js ***
//   272: 		test(fn + " to " + tn, function() {

$v.cf($v.ro('test'), [ fn + ' to ' + tn, $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   273: 			var elem = makeTest( fn + " to " + tn );

var elem = $v.cf($v.ro('makeTest'), [ fn + ' to ' + tn ]);

// *** unit/fx.js ***
//   275: 			var t_w = t( elem, "width" );

var t_w = $v.cf(t, [ elem, 'width' ]);

// *** unit/fx.js ***
//   276: 			var f_w = f( elem, "width" );

var f_w = $v.cf(f, [ elem, 'width' ]);

// *** unit/fx.js ***
//   277: 			var t_h = t( elem, "height" );

var t_h = $v.cf(t, [ elem, 'height' ]);

// *** unit/fx.js ***
//   278: 			var f_h = f( elem, "height" );

var f_h = $v.cf(f, [ elem, 'height' ]);

// *** unit/fx.js ***
//   279: 			var t_o = t( elem, "opacity" );

var t_o = $v.cf(t, [ elem, 'opacity' ]);

// *** unit/fx.js ***
//   280: 			var f_o = f( elem, "opacity" );

var f_o = $v.cf(f, [ elem, 'opacity' ]);

// *** unit/fx.js ***
//   282: 			var num = 0;

var num = 0;

// *** unit/fx.js ***
//   284: 			if ( t_h == "show" ) num++;

if (t_h == 'show') num++;

// *** unit/fx.js ***
//   285: 			if ( t_w == "show" ) num++;

if (t_w == 'show') num++;

// *** unit/fx.js ***
//   286: 			if ( t_w == "hide"||t_w == "show" ) num++;

if (t_w == 'hide' || t_w == 'show') num++;

// *** unit/fx.js ***
//   287: 			if ( t_h == "hide"||t_h == "show" ) num++;

if (t_h == 'hide' || t_h == 'show') num++;

// *** unit/fx.js ***
//   288: 			if ( t_o == "hide"||t_o == "show" ) num++;

if (t_o == 'hide' || t_o == 'show') num++;

// *** unit/fx.js ***
//   289: 			if ( t_w == "hide" ) num++;

if (t_w == 'hide') num++;

// *** unit/fx.js ***
//   290: 			if ( t_o.constructor == Number ) num += 2;

if ($v.r(t_o, 'constructor') == $v.ro('Number')) num = num + 2;

// *** unit/fx.js ***
//   291: 			if ( t_w.constructor == Number ) num += 2;

if ($v.r(t_w, 'constructor') == $v.ro('Number')) num = num + 2;

// *** unit/fx.js ***
//   292: 			if ( t_h.constructor == Number ) num +=2;

if ($v.r(t_h, 'constructor') == $v.ro('Number')) num = num + 2;
$v.cf($v.ro('expect'), [ num ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/fx.js ***
//   297: 			var anim = { width: t_w, height: t_h, opacity: t_o };

var anim = ___.initializeMap([ 'width', t_w, 'height', t_h, 'opacity', t_o ]);

// *** unit/fx.js ***
//   299: 			elem.animate(anim, 50, function(){

$v.cm(elem, 'animate', [ anim, 50, $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   300: 				if ( t_w == "show" )
//   301: 					equals( this.style.display, "block", "Showing, display should block: " + this.style.display);

if (t_w == 'show') $v.cf($v.ro('equals'), [ $v.r($v.r($dis, 'style'), 'display'), 'block', 'Showing, display should block: ' + $v.r($v.r($dis, 'style'), 'display') ]);

// *** unit/fx.js ***
//   303: 				if ( t_w == "hide"||t_w == "show" )
//   304: 					equals(this.style.width.indexOf(f_w), 0, "Width must be reset to " + f_w + ": " + this.style.width);

if (t_w == 'hide' || t_w == 'show') $v.cf($v.ro('equals'), [ $v.cm($v.r($v.r($dis, 'style'), 'width'), 'indexOf', [ f_w ]), 0, 'Width must be reset to ' + f_w + ': ' + $v.r($v.r($dis, 'style'), 'width') ]);

// *** unit/fx.js ***
//   306: 				if ( t_h == "hide"||t_h == "show" )
//   307: 					equals(this.style.height.indexOf(f_h), 0, "Height must be reset to " + f_h + ": " + this.style.height);

if (t_h == 'hide' || t_h == 'show') $v.cf($v.ro('equals'), [ $v.cm($v.r($v.r($dis, 'style'), 'height'), 'indexOf', [ f_h ]), 0, 'Height must be reset to ' + f_h + ': ' + $v.r($v.r($dis, 'style'), 'height') ]);

// *** unit/fx.js ***
//   309: 				var cur_o = jQuery.attr(this.style, "opacity");

var cur_o = $v.cm($v.ro('jQuery'), 'attr', [ $v.r($dis, 'style'), 'opacity' ]);

// *** unit/fx.js ***
//   310: 				if ( cur_o !== "" ) cur_o = parseFloat( cur_o );

if (cur_o !== '') cur_o = $v.cf($v.ro('parseFloat'), [ cur_o ]);

// *** unit/fx.js ***
//   312: 				if ( t_o == "hide"||t_o == "show" )
//   313: 					equals(cur_o, f_o, "Opacity must be reset to " + f_o + ": " + cur_o);

if (t_o == 'hide' || t_o == 'show') $v.cf($v.ro('equals'), [ cur_o, f_o, 'Opacity must be reset to ' + f_o + ': ' + cur_o ]);

// *** unit/fx.js ***
//   315: 				if ( t_w == "hide" )
//   316: 					equals(this.style.display, "none", "Hiding, display should be none: " + this.style.display);

if (t_w == 'hide') $v.cf($v.ro('equals'), [ $v.r($v.r($dis, 'style'), 'display'), 'none', 'Hiding, display should be none: ' + $v.r($v.r($dis, 'style'), 'display') ]);

// *** unit/fx.js ***
//   318: 				if ( t_o.constructor == Number ) {
//   319: 					equals(cur_o, t_o, "Final opacity should be " + t_o + ": " + cur_o);
//   320: 					
//   321: 					ok(jQuery.curCSS(this, "opacity") != "" || cur_o == t_o, "Opacity should be explicitly set to " + t_o + ", is instead: " + cur_o);
//   322: 				}

if ($v.r(t_o, 'constructor') == $v.ro('Number')) {
$v.cf($v.ro('equals'), [ cur_o, t_o, 'Final opacity should be ' + t_o + ': ' + cur_o ]);
$v.cf($v.ro('ok'), [ $v.cm($v.ro('jQuery'), 'curCSS', [ $dis, 'opacity' ]) != '' || cur_o == t_o, 'Opacity should be explicitly set to ' + t_o + ', is instead: ' + cur_o ]);
}

// *** unit/fx.js ***
//   324: 				if ( t_w.constructor == Number ) {
//   325: 					equals(this.style.width, t_w + "px", "Final width should be " + t_w + ": " + this.style.width);
//   326: 					
//   328: 
//   329: 					ok(this.style.width != "" || cur_w == t_w, "Width should be explicitly set to " + t_w + ", is instead: " + cur_w);
//   330: 				}

if ($v.r(t_w, 'constructor') == $v.ro('Number')) {
$v.cf($v.ro('equals'), [ $v.r($v.r($dis, 'style'), 'width'), t_w + 'px', 'Final width should be ' + t_w + ': ' + $v.r($v.r($dis, 'style'), 'width') ]);

// *** unit/fx.js ***
//   327: 					var cur_w = jQuery.css(this,"width");

var cur_w = $v.cm($v.ro('jQuery'), 'css', [ $dis, 'width' ]);
$v.cf($v.ro('ok'), [ $v.r($v.r($dis, 'style'), 'width') != '' || cur_w == t_w, 'Width should be explicitly set to ' + t_w + ', is instead: ' + cur_w ]);
}

// *** unit/fx.js ***
//   332: 				if ( t_h.constructor == Number ) {
//   333: 					equals(this.style.height, t_h + "px", "Final height should be " + t_h + ": " + this.style.height);
//   334: 					
//   336: 
//   337: 					ok(this.style.height != "" || cur_h == t_h, "Height should be explicitly set to " + t_h + ", is instead: " + cur_w);
//   338: 				}

if ($v.r(t_h, 'constructor') == $v.ro('Number')) {
$v.cf($v.ro('equals'), [ $v.r($v.r($dis, 'style'), 'height'), t_h + 'px', 'Final height should be ' + t_h + ': ' + $v.r($v.r($dis, 'style'), 'height') ]);

// *** unit/fx.js ***
//   335: 					var cur_h = jQuery.css(this,"height");

var cur_h = $v.cm($v.ro('jQuery'), 'css', [ $dis, 'height' ]);
$v.cf($v.ro('ok'), [ $v.r($v.r($dis, 'style'), 'height') != '' || cur_h == t_h, 'Height should be explicitly set to ' + t_h + ', is instead: ' + cur_w ]);
}

// *** unit/fx.js ***
//   340: 				if ( t_h == "show" ) {
//   342: 					$(elem).append("<br/>Some more text<br/>and some more...");
//   343: 					ok(old_h != jQuery.css(this, "height" ), "Make sure height is auto.");
//   344: 				}

if (t_h == 'show') {

// *** unit/fx.js ***
//   341: 					var old_h = jQuery.curCSS(this, "height");

var old_h = $v.cm($v.ro('jQuery'), 'curCSS', [ $dis, 'height' ]);
$v.cm($v.cf($v.ro('$'), [ elem ]), 'append', [ '\x3cbr/\x3eSome more text\x3cbr/\x3eand some more...' ]);
$v.cf($v.ro('ok'), [ old_h != $v.cm($v.ro('jQuery'), 'css', [ $dis, 'height' ]), 'Make sure height is auto.' ]);
}
$v.cf($v.ro('start'), [ ]);

// *** unit/fx.js ***
//   346: 				start();

})) ]);

// *** unit/fx.js ***
//   347: 			});

})) ]);

// *** unit/fx.js ***
//   348: 		});

})) ]);

// *** unit/fx.js ***
//   349: 	});

})) ]);

// *** unit/fx.js ***
//   352: var check = ['opacity','height','width','display','overflow'];

$v.so('check', [ 'opacity', 'height', 'width', 'display', 'overflow' ]);

// *** unit/fx.js ***
//   354: jQuery.fn.saveState = function(){

$v.s($v.r($v.ro('jQuery'), 'fn'), 'saveState', ___.frozenFunc(function () {

// *** unit/fx.js ***
//   355: 	expect(check.length);
//   356: 	stop();

function saveState$_meth$($dis) {
$v.cf($v.ro('expect'), [ $v.r($v.ro('check'), 'length') ]);
$v.cf($v.ro('stop'), [ ]);

// *** unit/fx.js ***
//   357: 	return this.each(function(){
//   359: 		self.save = {};
//   360: 		jQuery.each(check, function(i,c){
//   363: 	});

return $v.cm($dis, 'each', [ $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   358: 		var self = this;

var self = $dis;
$v.s(self, 'save', ___.initializeMap([ ]));
$v.cm($v.ro('jQuery'), 'each', [ $v.ro('check'), $v.dis(___.frozenFunc(function ($dis, i, c) {
$v.s($v.r(self, 'save'), c, $v.cm($v.ro('jQuery'), 'css', [ self, c ]));

// *** unit/fx.js ***
//   361: 			self.save[c] = jQuery.css(self,c);

})) ]);

// *** unit/fx.js ***
//   362: 		});

})) ]);
}
___.func(saveState$_meth$, 'saveState$_meth$');
;
var saveState$_meth = $v.dis(___.primFreeze(saveState$_meth$), 'saveState$_meth');
return saveState$_meth;
}).CALL___());
;

// *** unit/fx.js ***
//   376: test("Chain fadeOut fadeIn", function() {

$v.cf($v.ro('test'), [ 'Chain fadeOut fadeIn', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   377: 	$('#fadein div').saveState().fadeOut('fast').fadeIn('fast',checkState);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#fadein div' ]), 'saveState', [ ]), 'fadeOut', [ 'fast' ]), 'fadeIn', [ 'fast', $v.ro('checkState') ]);
})) ]);

// *** unit/fx.js ***
//   379: test("Chain fadeIn fadeOut", function() {

$v.cf($v.ro('test'), [ 'Chain fadeIn fadeOut', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   380: 	$('#fadeout div').saveState().fadeIn('fast').fadeOut('fast',checkState);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#fadeout div' ]), 'saveState', [ ]), 'fadeIn', [ 'fast' ]), 'fadeOut', [ 'fast', $v.ro('checkState') ]);
})) ]);

// *** unit/fx.js ***
//   383: test("Chain hide show", function() {

$v.cf($v.ro('test'), [ 'Chain hide show', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   384: 	$('#show div').saveState().hide('fast').show('fast',checkState);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#show div' ]), 'saveState', [ ]), 'hide', [ 'fast' ]), 'show', [ 'fast', $v.ro('checkState') ]);
})) ]);

// *** unit/fx.js ***
//   386: test("Chain show hide", function() {

$v.cf($v.ro('test'), [ 'Chain show hide', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   387: 	$('#hide div').saveState().show('fast').hide('fast',checkState);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#hide div' ]), 'saveState', [ ]), 'show', [ 'fast' ]), 'hide', [ 'fast', $v.ro('checkState') ]);
})) ]);

// *** unit/fx.js ***
//   390: test("Chain toggle in", function() {

$v.cf($v.ro('test'), [ 'Chain toggle in', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   391: 	$('#togglein div').saveState().toggle('fast').toggle('fast',checkState);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#togglein div' ]), 'saveState', [ ]), 'toggle', [ 'fast' ]), 'toggle', [ 'fast', $v.ro('checkState') ]);
})) ]);

// *** unit/fx.js ***
//   393: test("Chain toggle out", function() {

$v.cf($v.ro('test'), [ 'Chain toggle out', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   394: 	$('#toggleout div').saveState().toggle('fast').toggle('fast',checkState);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#toggleout div' ]), 'saveState', [ ]), 'toggle', [ 'fast' ]), 'toggle', [ 'fast', $v.ro('checkState') ]);
})) ]);

// *** unit/fx.js ***
//   397: test("Chain slideDown slideUp", function() {

$v.cf($v.ro('test'), [ 'Chain slideDown slideUp', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   398: 	$('#slidedown div').saveState().slideDown('fast').slideUp('fast',checkState);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#slidedown div' ]), 'saveState', [ ]), 'slideDown', [ 'fast' ]), 'slideUp', [ 'fast', $v.ro('checkState') ]);
})) ]);

// *** unit/fx.js ***
//   400: test("Chain slideUp slideDown", function() {

$v.cf($v.ro('test'), [ 'Chain slideUp slideDown', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   401: 	$('#slideup div').saveState().slideUp('fast').slideDown('fast',checkState);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#slideup div' ]), 'saveState', [ ]), 'slideUp', [ 'fast' ]), 'slideDown', [ 'fast', $v.ro('checkState') ]);
})) ]);

// *** unit/fx.js ***
//   404: test("Chain slideToggle in", function() {

$v.cf($v.ro('test'), [ 'Chain slideToggle in', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   405: 	$('#slidetogglein div').saveState().slideToggle('fast').slideToggle('fast',checkState);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#slidetogglein div' ]), 'saveState', [ ]), 'slideToggle', [ 'fast' ]), 'slideToggle', [ 'fast', $v.ro('checkState') ]);
})) ]);

// *** unit/fx.js ***
//   407: test("Chain slideToggle out", function() {

$v.cf($v.ro('test'), [ 'Chain slideToggle out', $v.dis(___.frozenFunc(function ($dis) {

// *** unit/fx.js ***
//   408: 	$('#slidetoggleout div').saveState().slideToggle('fast').slideToggle('fast',checkState);

$v.cm($v.cm($v.cm($v.cf($v.ro('$'), [ '#slidetoggleout div' ]), 'saveState', [ ]), 'slideToggle', [ 'fast' ]), 'slideToggle', [ 'fast', $v.ro('checkState') ]);
})) ]);
;
$v.s($v.ro('makeTest'), 'id', 1);
}
} catch (ex___) {
___.getNewModuleHandler().handleUncaughtException(ex___, $v.ro('onerror'), 'unit/fx.js', '1');
}
IMPORTS___.htmlEmitter___.pc('\n\n\n\n    ');
try {
{
$v.so('go', ___.frozenFunc(function () {

// *** index2.html ***
//    22:       document.nodeType = 9;
//    24:       runTest();

function go$_caller($dis) {
$v.s($v.ro('document'), 'nodeType', 9);

// *** index2.html ***
//    23:       $('#userAgent').html(navigator.userAgent); 

$v.cm($v.cf($v.ro('$'), [ '#userAgent' ]), 'html', [ $v.r($v.ro('navigator'), 'userAgent') ]);
$v.cf($v.ro('runTest'), [ ]);
}
___.func(go$_caller, 'go$_caller');
var go;
;

// *** index2.html ***
//    21:     function go() {

go = $v.dis(___.primFreeze(go$_caller), 'go');
return go;
}).CALL___());
;
}
} catch (ex___) {
___.getNewModuleHandler().handleUncaughtException(ex___, $v.ro('onerror'), 'index2.html', '21');
}
IMPORTS___.htmlEmitter___.pc('\n\t').b('h1').a('id', 'header-' + IMPORTS___.getIdClass___()).f(false).ih('jQuery Test Suite').e('h1').pc('\n\t').b('h2').a('id', 'banner-' + IMPORTS___.getIdClass___()).f(false).e('h2').pc('\n\t').b('h2').a('id', 'userAgent-' + IMPORTS___.getIdClass___()).f(false).e('h2').pc('\n\t\n\t\n\t').b('div').a('id', 'nothiddendiv-' + IMPORTS___.getIdClass___()).a('style', 'height: 1px;\nbackground: white').f(false).pc('\n\t\t').b('div').a('id', 'nothiddendivchild-' + IMPORTS___.getIdClass___()).f(false).e('div').pc('\n\t').e('div').pc('\n\t\n\t\n\t').b('dl').a('id', 'dl-' + IMPORTS___.getIdClass___()).a('style', 'display: none').f(false).pc('\n\t').b('div').a('id', 'main-' + IMPORTS___.getIdClass___()).a('style', 'display: none').f(false).pc('\n\t\t').b('p').a('id', 'firstp-' + IMPORTS___.getIdClass___()).f(false).pc('See ').b('a').a('id', 'simon1-' + IMPORTS___.getIdClass___()).a('href', 'http://simon.incutio.com/archive/2003/03/25/#getElementsBySelector').a('rel', 'bookmark').a('target', '_blank').f(false).ih('this \n\t\t  blog entry').e('a').pc(' for more information.').e('p').pc('\n\t\t').b('p').a('id', 'ap-' + IMPORTS___.getIdClass___()).f(false).pc('\n\t\t\tHere are some links in a normal paragraph: ').b('a').a('id', 'google-' + IMPORTS___.getIdClass___()).a('href', 'http://www.google.com/').a('title', 'Google!').a('target', '_blank').f(false).ih('Google').e('a').pc(', \n\t\t\t').b('a').a('id', 'groups-' + IMPORTS___.getIdClass___()).a('href', 'http://groups.google.com/').a('target', '_blank').f(false).ih('Google Groups').e('a').pc('. \n\t\t\tThis link has ').b('code').f(false).b('a').a('href', 'http://smin').a('id', 'anchor1-' + IMPORTS___.getIdClass___()).a('target', '_blank').f(false).ih('class=\x26quot;blog\x26quot;').e('a').e('code').pc(': \n\t\t\t').b('a').a('href', 'http://diveintomark.org/').a('class', 'blog').a('hreflang', 'en').a('id', 'mark-' + IMPORTS___.getIdClass___()).a('target', '_blank').f(false).ih('diveintomark').e('a').pc('\n\n\t\t').e('p').pc('\n\t\t').b('div').a('id', 'foo-' + IMPORTS___.getIdClass___());
IMPORTS___.htmlEmitter___.f(false).pc('\n\t\t\t').b('p').a('id', 'sndp-' + IMPORTS___.getIdClass___()).f(false).ih('Everything inside the red border is inside a div with \x3ccode\x3eid=\x26quot;foo\x26quot;\x3c/code\x3e.').e('p').pc('\n\t\t\t').b('p').a('lang', 'en').a('id', 'en-' + IMPORTS___.getIdClass___()).f(false).pc('This is a normal link: ').b('a').a('id', 'yahoo-' + IMPORTS___.getIdClass___()).a('href', 'http://www.yahoo.com/').a('class', 'blogTest').a('target', '_blank').f(false).ih('Yahoo').e('a').e('p').pc('\n\t\t\t').b('p').a('id', 'sap-' + IMPORTS___.getIdClass___()).f(false).pc('This link has ').b('code').f(false).b('a').a('href', '#2').a('id', 'anchor2-' + IMPORTS___.getIdClass___()).a('target', '_blank').f(false).ih('class=\x26quot;blog\x26quot;').e('a').e('code').pc(': ').b('a').a('href', 'http://simon.incutio.com/').a('class', 'blog link').a('id', 'simon-' + IMPORTS___.getIdClass___()).a('target', '_blank').f(false).ih('Simon Willison\x26#39;s Weblog').e('a').e('p').pc('\n\n\t\t').e('div').pc('\n\t\t').b('p').a('id', 'first-' + IMPORTS___.getIdClass___()).f(false).ih('Try them out:').e('p').pc('\n\t\t').b('ul').a('id', 'firstUL-' + IMPORTS___.getIdClass___()).f(false).e('ul').pc('\n\t\t').b('ol').a('id', 'empty-' + IMPORTS___.getIdClass___()).f(false).e('ol').pc('\n\t\t').b('form').a('id', 'form-' + IMPORTS___.getIdClass___()).a('action', 'formaction').a('onsubmit', 'return false').f(false).pc('\n\t\t\t').b('input').a('type', 'text').a('name', 'action').a('value', 'Test').a('id', 'text1-' + IMPORTS___.getIdClass___()).a('maxlength', '30').f(true).pc('\n\t\t\t').b('input').a('type', 'text').a('name', 'text2').a('value', 'Test').a('id', 'text2-' + IMPORTS___.getIdClass___()).a('disabled', 'disabled').f(true).pc('\n\t\t\t').b('input').a('type', 'radio').a('name', 'radio1').a('id', 'radio1-' + IMPORTS___.getIdClass___()).a('value', 'on').f(true).pc('\n\n\t\t\t').b('input').a('type', 'radio').a('name', 'radio2').a('id', 'radio2-' + IMPORTS___.getIdClass___()).a('checked', 'checked');
IMPORTS___.htmlEmitter___.f(true).pc('\n\t\t\t').b('input').a('type', 'checkbox').a('name', 'check').a('id', 'check1-' + IMPORTS___.getIdClass___()).a('checked', 'checked').f(true).pc('\n\t\t\t').b('input').a('type', 'checkbox').a('id', 'check2-' + IMPORTS___.getIdClass___()).a('value', 'on').f(true).pc('\n\n\t\t\t').b('input').a('type', 'hidden').a('name', 'hidden').a('id', 'hidden1-' + IMPORTS___.getIdClass___()).f(true).pc('\n\t\t\t').b('input').a('type', 'text').a('style', 'display: none').a('name', 'foo[bar]').a('id', 'hidden2-' + IMPORTS___.getIdClass___()).f(true).pc('\n\t\t\t\n\t\t\t').b('input').a('type', 'text').a('id', 'name-' + IMPORTS___.getIdClass___()).a('name', 'name').a('value', 'name').f(true).pc('\n\t\t\t\n\t\t\t').b('button').a('id', 'button-' + IMPORTS___.getIdClass___()).a('name', 'button').f(false).ih('Button').e('button').pc('\n\t\t\t\n\t\t\t').b('textarea').a('id', 'area1-' + IMPORTS___.getIdClass___()).f(false).ih('foobar').e('textarea').pc('\n\t\t\t\n\t\t\t').b('select').a('name', 'select1').a('id', 'select1-' + IMPORTS___.getIdClass___()).f(false).pc('\n\t\t\t\t').b('option').a('id', 'option1a-' + IMPORTS___.getIdClass___()).a('class', 'emptyopt').a('value', '').f(false).ih('Nothing').e('option').pc('\n\t\t\t\t').b('option').a('id', 'option1b-' + IMPORTS___.getIdClass___()).a('value', '1').f(false).ih('1').e('option').pc('\n\t\t\t\t').b('option').a('id', 'option1c-' + IMPORTS___.getIdClass___()).a('value', '2').f(false).ih('2').e('option').pc('\n\t\t\t\t').b('option').a('id', 'option1d-' + IMPORTS___.getIdClass___()).a('value', '3').f(false).ih('3').e('option').pc('\n\t\t\t').e('select').pc('\n\t\t\t').b('select').a('name', 'select2').a('id', 'select2-' + IMPORTS___.getIdClass___()).f(false).pc('\n\t\t\t\t').b('option').a('id', 'option2a-' + IMPORTS___.getIdClass___()).a('class', 'emptyopt').a('value', '').f(false).ih('Nothing').e('option').pc('\n\t\t\t\t').b('option').a('id', 'option2b-' + IMPORTS___.getIdClass___()).a('value', '1');
IMPORTS___.htmlEmitter___.f(false).ih('1').e('option').pc('\n\t\t\t\t').b('option').a('id', 'option2c-' + IMPORTS___.getIdClass___()).a('value', '2').f(false).ih('2').e('option').pc('\n\t\t\t\t').b('option').a('id', 'option2d-' + IMPORTS___.getIdClass___()).a('selected', 'selected').a('value', '3').f(false).ih('3').e('option').pc('\n\t\t\t').e('select').pc('\n\t\t\t').b('select').a('name', 'select3').a('id', 'select3-' + IMPORTS___.getIdClass___()).a('multiple', 'multiple').f(false).pc('\n\t\t\t\t').b('option').a('id', 'option3a-' + IMPORTS___.getIdClass___()).a('class', 'emptyopt').a('value', '').f(false).ih('Nothing').e('option').pc('\n\t\t\t\t').b('option').a('id', 'option3b-' + IMPORTS___.getIdClass___()).a('selected', 'selected').a('value', '1').f(false).ih('1').e('option').pc('\n\t\t\t\t').b('option').a('id', 'option3c-' + IMPORTS___.getIdClass___()).a('selected', 'selected').a('value', '2').f(false).ih('2').e('option').pc('\n\t\t\t\t').b('option').a('id', 'option3d-' + IMPORTS___.getIdClass___()).a('value', '3').f(false).ih('3').e('option').pc('\n\t\t\t').e('select').pc('\n\t\t\t\n\t\t\t\n\t\t\t\n\t\t\t').b('span').a('id', '\u53f0\u5317Ta\u0301ibe\u030ci-' + IMPORTS___.getIdClass___()).f(false).e('span').pc('\n\t\t\t').b('span').a('id', '\u53f0\u5317-' + IMPORTS___.getIdClass___()).a('lang', '\u4e2d\u6587').f(false).e('span').pc('\n\t\t\t').b('span').a('id', 'utf8class1-' + IMPORTS___.getIdClass___()).a('class', '\u53f0\u5317Ta\u0301ibe\u030ci \u53f0\u5317').f(false).e('span').pc('\n\t\t\t').b('span').a('id', 'utf8class2-' + IMPORTS___.getIdClass___()).a('class', '\u53f0\u5317').f(false).e('span').pc('\n\t\t\t').b('span').a('id', 'foo:bar-' + IMPORTS___.getIdClass___()).a('class', 'foo:bar').f(false).e('span').pc('\n\t\t\t').b('span').a('id', 'test.foo[5]bar-' + IMPORTS___.getIdClass___()).a('class', 'test.foo[5]bar').f(false).e('span').pc('\n\t\t\t\n\t\t\t\n\t\t').e('form').pc('\n\t\t').b('b').a('id', 'floatTest-' + IMPORTS___.getIdClass___()).f(false);
IMPORTS___.htmlEmitter___.ih('Float test.').e('b').pc('\n\t\t\n\t\t').b('form').a('id', 'lengthtest-' + IMPORTS___.getIdClass___()).a('onsubmit', 'return false').f(false).pc('\n\t\t\t').b('input').a('type', 'text').a('id', 'length-' + IMPORTS___.getIdClass___()).a('name', 'test').f(true).pc('\n\t\t\t').b('input').a('type', 'text').a('id', 'idTest-' + IMPORTS___.getIdClass___()).a('name', 'id').f(true).pc('\n\t\t').e('form').pc('\n\t\t').b('table').a('id', 'table-' + IMPORTS___.getIdClass___()).f(false).e('table').pc('\n\t\t\n\t\t').b('div').a('id', 'fx-queue-' + IMPORTS___.getIdClass___()).f(false).pc('\n\t\t\t').b('div').a('id', 'fadein-' + IMPORTS___.getIdClass___()).a('class', 'chain test').f(false).ih('fadeIn\x3cdiv\x3efadeIn\x3c/div\x3e').e('div').pc('\n\t\t\t').b('div').a('id', 'fadeout-' + IMPORTS___.getIdClass___()).a('class', 'chain test out').f(false).ih('fadeOut\x3cdiv\x3efadeOut\x3c/div\x3e').e('div').pc('\n\t\t\t\n\t\t\t').b('div').a('id', 'show-' + IMPORTS___.getIdClass___()).a('class', 'chain test').f(false).ih('show\x3cdiv\x3eshow\x3c/div\x3e').e('div').pc('\n\t\t\t').b('div').a('id', 'hide-' + IMPORTS___.getIdClass___()).a('class', 'chain test out').f(false).ih('hide\x3cdiv\x3ehide\x3c/div\x3e').e('div').pc('\n\t\t\t\n\t\t\t').b('div').a('id', 'togglein-' + IMPORTS___.getIdClass___()).a('class', 'chain test').f(false).ih('togglein\x3cdiv\x3etogglein\x3c/div\x3e').e('div').pc('\n\t\t\t').b('div').a('id', 'toggleout-' + IMPORTS___.getIdClass___()).a('class', 'chain test out').f(false).ih('toggleout\x3cdiv\x3etoggleout\x3c/div\x3e').e('div').pc('\n\t\t\n\t\t\t\n\t\t\t').b('div').a('id', 'slideup-' + IMPORTS___.getIdClass___()).a('class', 'chain test').f(false).ih('slideUp\x3cdiv\x3eslideUp\x3c/div\x3e').e('div').pc('\n\t\t\t').b('div').a('id', 'slidedown-' + IMPORTS___.getIdClass___()).a('class', 'chain test out').f(false).ih('slideDown\x3cdiv\x3eslideDown\x3c/div\x3e').e('div').pc('\n\t\t\t\n\t\t\t').b('div').a('id', 'slidetogglein-' + IMPORTS___.getIdClass___()).a('class', 'chain test').f(false).ih('slideToggleIn\x3cdiv\x3eslideToggleIn\x3c/div\x3e').e('div').pc('\n\t\t\t').b('div').a('id', 'slidetoggleout-' + IMPORTS___.getIdClass___()).a('class', 'chain test out').f(false).ih('slideToggleOut\x3cdiv\x3eslideToggleOut\x3c/div\x3e').e('div');
IMPORTS___.htmlEmitter___.pc('\n\t\t').e('div').pc('\n\t\t\n\t\t').b('div').a('id', 'fx-tests-' + IMPORTS___.getIdClass___()).f(false).e('div').pc('\n\n\t\t').b('form').a('id', 'testForm-' + IMPORTS___.getIdClass___()).a('method', 'get').a('onsubmit', 'return false').f(false).ih('\n\t\t\t\x3ctextarea name=\"T3\" rows=\"2\" cols=\"15\"\x3e?\nZ\x3c/textarea\x3e\n\t\t\t\x3cinput type=\"hidden\" name=\"H1\" value=\"x\" /\x3e\n\t\t\t\x3cinput type=\"hidden\" name=\"H2\" /\x3e\n\t\t\t\x3cinput name=\"PWD\" value=\"\" /\x3e\n\t\t\t\x3cinput name=\"T1\" type=\"text\" /\x3e\n\t\t\t\x3cinput name=\"T2\" type=\"text\" value=\"YES\" readonly=\"readonly\" /\x3e\n\t\t\t\x3cinput type=\"checkbox\" name=\"C1\" value=\"1\" /\x3e\n\t\t\t\x3cinput type=\"checkbox\" name=\"C2\" /\x3e\n\t\t\t\x3cinput type=\"radio\" name=\"R1\" value=\"1\" /\x3e\n\t\t\t\x3cinput type=\"radio\" name=\"R1\" value=\"2\" /\x3e\n\t\t\t\x3cinput type=\"text\" name=\"My Name\" value=\"me\" /\x3e\n\t\t\t\x3cinput type=\"reset\" name=\"reset\" value=\"NO\" /\x3e\n\t\t\t\x3cselect name=\"S1\"\x3e\n\t\t\t\t\x3coption value=\"abc\"\x3eABC\x3c/option\x3e\n\t\t\t\t\x3coption value=\"abc\"\x3eABC\x3c/option\x3e\n\t\t\t\t\x3coption value=\"abc\"\x3eABC\x3c/option\x3e\n\t\t\t\x3c/select\x3e\n\t\t\t\x3cselect name=\"S2\" multiple=\"multiple\" size=\"3\"\x3e\n\t\t\t\t\x3coption value=\"abc\"\x3eABC\x3c/option\x3e\n\t\t\t\t\x3coption value=\"abc\"\x3eABC\x3c/option\x3e\n\t\t\t\t\x3coption value=\"abc\"\x3eABC\x3c/option\x3e\n\t\t\t\x3c/select\x3e\n\t\t\t\x3cselect name=\"S3\"\x3e\n\t\t\t\t\x3coption selected=\"selected\"\x3eYES\x3c/option\x3e\n\t\t\t\x3c/select\x3e\n\t\t\t\x3cselect name=\"S4\"\x3e\n\t\t\t\t\x3coption value=\"\" selected=\"selected\"\x3eNO\x3c/option\x3e\n\t\t\t\x3c/select\x3e\n\t\t\t\x3cinput type=\"submit\" name=\"sub1\" value=\"NO\" /\x3e\n\t\t\t\x3cinput type=\"submit\" name=\"sub2\" value=\"NO\" /\x3e\n\t\t\t\x3cinput type=\"image\" name=\"sub3\" value=\"NO\" /\x3e\n\t\t\t\x3cbutton name=\"sub4\" type=\"submit\" value=\"NO\"\x3eNO\x3c/button\x3e\n\t\t\t\x3cinput name=\"D1\" type=\"text\" value=\"NO\" disabled=\"disabled\" /\x3e\n\t\t\t\x3cinput type=\"checkbox\" checked=\"checked\" disabled=\"disabled\" name=\"D2\" value=\"NO\" /\x3e\n\t\t\t\x3cinput type=\"radio\" name=\"D3\" value=\"NO\" checked=\"checked\" disabled=\"disabled\" /\x3e\n\t\t\t\x3cselect name=\"D4\" disabled=\"disabled\"\x3e\n\t\t\t\t\x3coption selected=\"selected\" value=\"NO\"\x3eNO\x3c/option\x3e\n\t\t\t\x3c/select\x3e\n\t\t').e('form').pc('\n\t\t').b('div').a('id', 'moretests-' + IMPORTS___.getIdClass___()).f(false).pc('\n\t\t\t').b('form').a('onsubmit', 'return false').f(false).pc('\n\t\t\t\t').b('div').a('id', 'checkedtest-' + IMPORTS___.getIdClass___()).a('style', 'display: none').f(false).ih('\n\t\t\t\t\t\x3cinput type=\"radio\" name=\"checkedtestradios\" checked=\"checked\" /\x3e\n\t\t\t\t\t\x3cinput type=\"radio\" name=\"checkedtestradios\" value=\"on\" /\x3e\n\t\t\t\t\t\x3cinput type=\"checkbox\" name=\"checkedtestcheckboxes\" checked=\"checked\" /\x3e\n\t\t\t\t\t\x3cinput type=\"checkbox\" name=\"checkedtestcheckboxes\" /\x3e\n\t\t\t\t').e('div').pc('\n\t\t\t').e('form').pc('\n\t\t\t').b('div').a('id', 'nonnodes-' + IMPORTS___.getIdClass___()).f(false).ih('\x3cspan\x3ehi\x3c/span\x3e there ').e('div').pc('\n\t\t\t').b('div').a('id', 't2037-' + IMPORTS___.getIdClass___()).f(false).ih('\n\t\t\t\t\x3cdiv\x3e\x3cdiv class=\"hidden\"\x3ehidden\x3c/div\x3e\x3c/div\x3e\n\t\t\t').e('div').pc('\n\t\t').e('div').pc('\n\t').e('div').pc('\n\t').e('dl').pc('\n\t\n\t').b('ol').a('id', 'tests-' + IMPORTS___.getIdClass___()).f(false).e('ol').pc('\n\n\n');
try {
{
moduleResult___ = $v.cf($v.ro('go'), [ ]);
}
} catch (ex___) {
___.getNewModuleHandler().handleUncaughtException(ex___, $v.ro('onerror'), 'index2.html', '19');
}
return moduleResult___;
});
}

