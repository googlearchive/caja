// a file to test the javascript lexer

var i = 1 + 1;
i = 1+1;
i = 1 +1;  
i = 1+-1;
i = 1 - -1;

'a string constant';
s = 'a string that \
spans multiple physical lines \
but not logical ones';
'a string with "double quotes" inside and ' +
'a string with an escaped \' inside';
{'a string with multiple escaped \\characters\' inside'};
var s = "double quotes work inside strings too.\
pretty well actually";

// a line comment that ends with a \
'does not absorb the next line';

/* multiline comments also *\
/ "don't care about backslash newline" */

/*/ try and confuse the lexer
    with a star-slash before
    the end of the comment.
 */

/* comments can have embedded "strings" */
"and /*vice-versa*/ "

we(need - to + {{{test punctuation thoroughly}}});

left <<= shift_amount;

14.0005e-6 is one token?

// check that exponentials with signs extracted properly during splitting
var num = 1000-1e+2*2;

// check that dotted identifiers split, but decimal numbers not.
foo.bar = 4.0;
foo2.bar = baz;

.5  // a numeric token

// javascript does not have line continuations.
foo\
bar;

ellipsis...;

/* and extending the example at line 30 " interleaved */ " */\
"also /* " /* */

// Backslashes in character sets do not end regexs.
r = /./, /\//, /[/]/, /[\/]\//

isNaN(NaN);

// leave some whitespace at the end of this file  
