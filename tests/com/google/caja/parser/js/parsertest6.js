// Make sure prologue directives are rendered respecting escaping
"use foo";  // Should be rendered
"use f\u0008oo";  // Should not be rendered

// Make sure code is safe to embed in script tags and CDATA sections.
var w = /<script>/;
var x = a </script>/;  // Try to escape script
var y = [[0]]>1;  // Try to escape CDATA section.
var s = '</script>' + '<\/script>';
var t = ']]>';
var r = /[\]]>/;


// Make sure noone can sneak a DOCTYPE declaration through.
// If they could they could do bad things with external entities.
var DOCTYPE = null;
var d = 0<!DOCTYPE;
var e = '<!DOCTYPE';
var f = /<!DOCTYPE [ ]>/;


// There shouldn't be any comments in the output, but let's be paranoid.
// <script>foo</script>
// <![CDATA[ bar() ]]>
// <!DOCTYPE foo [ ]>

var g = 0&amp;

// Make sure we don't treat string literals in Object constructors differently.
var o = { '</script>': 0, ']]>': 1, '<!DOCTYPE': 2 };

// Make sure that comment markers and escaping text spans can't appear in
// output.  c should be parenthesized since HTML4 allows space between the
// -- and >
var p = a <!-- b && c --> (d);
