function assertLexedCss(cssText, var_args) {
  var golden = Array.prototype.slice.call(arguments, 1);
  assertEquals(cssText, '\n' + golden.join('|||'),
               '\n' + lexCss(cssText).join('|||'));
}

jsunitRegister('testLex01', function testLex01() {
  assertLexedCss(
    'body {\n\
	color:green;\n\
}\r\n\
\n\
div#foo { content:"bar{foo}"; }',
    'body', ' ', '{', ' ',
    'color', ':', 'green', ';', ' ',
    '}', ' ',
    'div', '#foo', ' ', '{', ' ', 'content', ':', '"bar{foo}"', ';', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex02', function testLex02() {
  assertLexedCss(
    'body div {\n\
\tcolor:red;\n\
}\n',
    'body', ' ', 'div', ' ', '{', ' ',
    'color', ':', 'red', ';', ' ',
    '}', ' ');
  jsunit.pass();
});

jsunitRegister('testLex03', function testLex03() {
  assertLexedCss(
    'div#foo { background:url(img/blubb.png) top left repeat-y; }\n\
\n\
body { font-family:Verdana, Geneva, Arial, Helvetica, sans-serif; font-size:12px; }\n\
\n\
@import url("foo.css");\n\
\n\
@import "bar.css" screen;',
    'div', '#foo', ' ', '{',
    ' ', 'background', ':', 'url("img/blubb.png")',
    ' ', 'top', ' ', 'left', ' ', 'repeat-y', ';', ' ', '}', ' ',
    'body', ' ', '{', ' ', 'font-family', ':', 'Verdana', ',',
    ' ', 'Geneva', ',', ' ', 'Arial', ',', ' ', 'Helvetica', ',',
    ' ', 'sans-serif', ';',
    ' ', 'font-size', ':', '12px', ';', ' ', '}', ' ', 
    '@import', ' ', 'url("foo.css")', ';', ' ', 
    '@import', ' ', '"bar.css"', ' ', 'screen', ';');
  jsunit.pass();
});

jsunitRegister('testLex04', function testLex04() {
  assertLexedCss(
    '\n\
\n\
/* Komentar! */\n\
@media projection {\n\
\t#blubb {\n\
\t\tfont-weight: /* Komentar! */ bold;\n\
\t\tcontent:\';{!""())!"\';\n\
\t}\n\
}\n\
#gnnf{\n\
\tbackground:green url(\'img/beispiel.png\') top left no-repeat;\n\
\ttext-align:left\n\
}',
    '@media', ' ', 'projection', ' ', '{', ' ', 
    '#blubb', ' ', '{', ' ', 
    'font-weight', ':', ' ', 'bold', ';', ' ', 
    'content', ':', '";{!\\22 \\22 ())!\\22 "', ';', ' ', 
    '}', ' ', 
    '}', ' ', 
    '#gnnf', '{', ' ', 
    'background', ':', 'green', ' ', 'url("img/beispiel.png")',
    ' ', 'top', ' ', 'left', ' ', 'no-repeat', ';', ' ',
    'text-align', ':', 'left', ' ',
    '}');
  jsunit.pass();
});

jsunitRegister('testLex05', function testLex05() {
  assertLexedCss(
    '/**\n\
 * FETTER Komentar!\n\
 * \n\
 * Bla bla bla\n\
 */\n\
@media screen {\n\
\t#test[foo] {\n\
\t\tcolor:red !important;\n\
\t}\n\
\t#test[foo] {\n\
\t\tcolor:blue;\n\
\t}\n\
}',
    '@media', ' ', 'screen', ' ', '{', ' ', 
    '#test', '[', 'foo', ']', ' ', '{', ' ', 
    'color', ':', 'red', ' ', '!', 'important', ';', ' ', 
    '}', ' ', 
    '#test', '[', 'foo', ']', ' ', '{', ' ', 
    'color', ':', 'blue', ';', ' ', 
    '}', ' ', 
    '}');
  jsunit.pass();
});

jsunitRegister('testLex06', function testLex06() {
  assertLexedCss(
    '#blah[rel="/{_-;!"] div > #blargh span.narf {\n\
\tbackground:green;\n\
\ttext-align:left;\n\
}',
    '#blah', '[', 'rel', '=', '"/{_-;!"', ']', ' ', 'div',
    ' ', '>', ' ', '#blargh', ' ', 'span', '.', 'narf', ' ', '{', ' ',
    'background', ':', 'green', ';', ' ',
    'text-align', ':', 'left', ';', ' ',
    '}');
  jsunit.pass();
});

jsunitRegister('testLex07', function testLex07() {
  assertLexedCss(
    '/* Komentar! */\n\
@media print {\n\
\t#gnarf {\n\
\t\tfont-weight:normal;\n\
\t\tfont-size:2em\n\
\t}\n\
}',
    '@media', ' ', 'print', ' ', '{', ' ',
    '#gnarf', ' ', '{', ' ',
    'font-weight', ':', 'normal', ';', ' ',
    'font-size', ':', '2em', ' ',
    '}', ' ',
    '}');
  jsunit.pass();
});

jsunitRegister('testLex08', function testLex08() {
  assertLexedCss(
    '#foobar {\n\
\tfont-family:"Trebuchet MS", Verdana, Arial, sans-serif;\n\
}',
    '#foobar', ' ', '{', ' ',
    'font-family', ':', '"Trebuchet MS"', ',', ' ', 'Verdana', ',',
    ' ', 'Arial', ',', ' ', 'sans-serif', ';', ' ',
    '}');
  jsunit.pass();
});

jsunitRegister('testLex09', function testLex09() {
  assertLexedCss(
    'p { color:red !important; }\n\
.foo { color:green; }',
    'p', ' ', '{', ' ', 'color', ':', 'red', ' ', '!', 'important', ';',
    ' ', '}', ' ',
    '.', 'foo', ' ', '{', ' ', 'color', ':', 'green', ';', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex10', function testLex10() {
  assertLexedCss(
    '@media screen{\n\
\t#wrapper {\n\
\t\tcolor:blue;\n\
\t\tfont-weight:bold !important;\n\
\t\ttext-decoration:underline;\n\
\t}\n\
\t#wrapper {\n\
\t\tcolor:red;\n\
\t\tfont-weight:normal;\n\
\t\tfont-style:italic;\n\
\t}\n\
}\n\
\n\
@media print {\n\
\t#wrapper {\n\
\t\tcolor:green;\n\
\t}\n\
}',
    '@media', ' ', 'screen', '{', ' ',
    '#wrapper', ' ', '{', ' ',
    'color', ':', 'blue', ';', ' ',
    'font-weight', ':', 'bold', ' ', '!', 'important', ';', ' ',
    'text-decoration', ':', 'underline', ';', ' ',
    '}', ' ',
    '#wrapper', ' ', '{', ' ',
    'color', ':', 'red', ';', ' ',
    'font-weight', ':', 'normal', ';', ' ',
    'font-style', ':', 'italic', ';', ' ',
    '}', ' ',
    '}', ' ',
    '@media', ' ', 'print', ' ', '{', ' ',
    '#wrapper', ' ', '{', ' ',
    'color', ':', 'green', ';', ' ',
    '}', ' ',
    '}');
  jsunit.pass();
});

jsunitRegister('testLex11', function testLex11() {
  assertLexedCss(
    '\n\
ADDRESS,\n\
BLOCKQUOTE, \n\
BODY, DD, DIV, \n\
DL, DT, \n\
FIELDSET, FORM,\n\
FRAME, FRAMESET,\n\
H1, H2, H3, H4, \n\
H5, H6, IFRAME, \n\
NOFRAMES, \n\
OBJECT, OL, P, \n\
UL, APPLET, \n\
CENTER, DIR, \n\
HR, MENU, PRE   { display: block }\n\
LI              { display: list-item }\n\
HEAD            { display: none }\n\
TABLE           { display: table }\n\
TR              { display: table-row }\n\
THEAD           { display: table-header-group }\n\
TBODY           { display: table-row-group }\n\
TFOOT           { display: table-footer-group }\n\
COL             { display: table-column }\n\
COLGROUP        { display: table-column-group }\n\
TD, TH          { display: table-cell }\n\
CAPTION         { display: table-caption }\n\
TH              { font-weight: bolder; text-align: center }\n\
CAPTION         { text-align: center }\n\
BODY            { padding: 8px; line-height: 1.33 }\n\
H1              { font-size: 2em; margin: .67em 0 }\n\
H2              { font-size: 1.5em; margin: .83em 0 }\n\
H3              { font-size: 1.17em; margin: 1em 0 }\n\
H4, P,\n\
BLOCKQUOTE, UL,\n\
FIELDSET, FORM,\n\
OL, DL, DIR,\n\
MENU            { margin: 1.33em 0 }\n\
H5              { font-size: .83em; line-height: 1.17em; margin: 1.67em 0 }\n\
H6              { font-size: .67em; margin: 2.33em 0 }\n\
H1, H2, H3, H4,\n\
H5, H6, B,\n\
STRONG          { font-weight: bolder }\n\
BLOCKQUOTE      { margin-left: 40px; margin-right: 40px }\n\
I, CITE, EM,\n\
VAR, ADDRESS    { font-style: italic }\n\
PRE, TT, CODE,\n\
KBD, SAMP       { font-family: monospace }\n\
PRE             { white-space: pre }\n\
BIG             { font-size: 1.17em }\n\
SMALL, SUB, SUP { font-size: .83em }\n\
SUB             { vertical-align: sub }\n\
SUP             { vertical-align: super }\n\
S, STRIKE, DEL  { text-decoration: line-through }\n\
HR              { border: 1px inset }\n\
OL, UL, DIR,\n\
MENU, DD        { margin-left: 40px }\n\
OL              { list-style-type: decimal }\n\
OL UL, UL OL,\n\
UL UL, OL OL    { margin-top: 0; margin-bottom: 0 }\n\
U, INS          { text-decoration: underline }\n\
CENTER          { text-align: center }\n\
BR:before       { content: "\\A" }\n\
COLOR_NOHASH\t{ color:987E81 }',
   'ADDRESS', ',', ' ',
   'BLOCKQUOTE', ',', ' ',
   'BODY', ',', ' ', 'DD', ',', ' ', 'DIV', ',', ' ',
   'DL', ',', ' ', 'DT', ',', ' ',
   'FIELDSET', ',', ' ', 'FORM', ',', ' ',
   'FRAME', ',', ' ', 'FRAMESET', ',', ' ',
   'H1', ',', ' ', 'H2', ',', ' ', 'H3', ',', ' ', 'H4', ',', ' ',
   'H5', ',', ' ', 'H6', ',', ' ', 'IFRAME', ',', ' ',
   'NOFRAMES', ',', ' ',
   'OBJECT', ',', ' ', 'OL', ',', ' ', 'P', ',', ' ',
   'UL', ',', ' ', 'APPLET', ',', ' ',
   'CENTER', ',', ' ', 'DIR', ',', ' ',
   'HR', ',', ' ', 'MENU', ',', ' ', 'PRE', ' ', '{',
   ' ', 'display', ':', ' ', 'block', ' ', '}', ' ',
   'LI', ' ', '{', ' ', 'display', ':', ' ', 'list-item', ' ', '}', ' ',
   'HEAD', ' ', '{', ' ', 'display', ':', ' ', 'none', ' ', '}', ' ',
   'TABLE', ' ', '{', ' ', 'display', ':', ' ', 'table', ' ', '}', ' ',
   'TR', ' ', '{', ' ', 'display', ':', ' ', 'table-row', ' ', '}', ' ',
   'THEAD', ' ', '{',
   ' ', 'display', ':', ' ', 'table-header-group', ' ', '}', ' ',
   'TBODY', ' ', '{',
   ' ', 'display', ':', ' ', 'table-row-group', ' ', '}', ' ',
   'TFOOT', ' ', '{',
   ' ', 'display', ':', ' ', 'table-footer-group', ' ', '}', ' ',
   'COL', ' ', '{', ' ', 'display', ':', ' ', 'table-column', ' ', '}', ' ',
   'COLGROUP', ' ', '{',
   ' ', 'display', ':', ' ', 'table-column-group', ' ', '}', ' ',
   'TD', ',', ' ', 'TH', ' ', '{',
   ' ', 'display', ':', ' ', 'table-cell', ' ', '}', ' ',
   'CAPTION', ' ', '{',
   ' ', 'display', ':', ' ', 'table-caption', ' ', '}', ' ',
   'TH', ' ', '{',
   ' ', 'font-weight', ':', ' ', 'bolder', ';',
   ' ', 'text-align', ':', ' ', 'center', ' ', '}', ' ',
   'CAPTION', ' ', '{',
   ' ', 'text-align', ':', ' ', 'center', ' ', '}', ' ',
   'BODY', ' ', '{',
   ' ', 'padding', ':', ' ', '8px', ';',
   ' ', 'line-height', ':', ' ', '1.33', ' ', '}', ' ',
   'H1', ' ', '{',
   ' ', 'font-size', ':', ' ', '2em', ';',
   ' ', 'margin', ':', ' ', '.67em', ' ', '0', ' ', '}', ' ',
   'H2', ' ', '{',
   ' ', 'font-size', ':', ' ', '1.5em', ';',
   ' ', 'margin', ':', ' ', '.83em', ' ', '0', ' ', '}', ' ',
   'H3', ' ', '{',
   ' ', 'font-size', ':', ' ', '1.17em', ';',
   ' ', 'margin', ':', ' ', '1em', ' ', '0', ' ', '}', ' ',
   'H4', ',', ' ', 'P', ',', ' ',
   'BLOCKQUOTE', ',', ' ', 'UL', ',', ' ',
   'FIELDSET', ',', ' ', 'FORM', ',', ' ',
   'OL', ',', ' ', 'DL', ',', ' ', 'DIR', ',', ' ',
   'MENU', ' ', '{', ' ', 'margin', ':', ' ', '1.33em', ' ', '0', ' ', '}', ' ',
   'H5', ' ', '{', ' ', 'font-size', ':', ' ', '.83em', ';',
   ' ', 'line-height', ':', ' ', '1.17em',  ';',
   ' ', 'margin', ':', ' ', '1.67em', ' ', '0', ' ', '}', ' ',
   'H6', ' ', '{', ' ', 'font-size', ':', ' ', '.67em', ';',
   ' ', 'margin', ':', ' ', '2.33em', ' ', '0', ' ', '}', ' ',
   'H1', ',', ' ', 'H2', ',', ' ', 'H3', ',', ' ', 'H4', ',', ' ',
   'H5', ',', ' ', 'H6', ',', ' ', 'B', ',', ' ',
   'STRONG', ' ', '{', ' ', 'font-weight', ':', ' ', 'bolder', ' ', '}', ' ',
   'BLOCKQUOTE', ' ', '{', ' ', 'margin-left', ':', ' ', '40px', ';',
   ' ', 'margin-right', ':', ' ', '40px', ' ', '}', ' ',
   'I', ',', ' ', 'CITE', ',', ' ', 'EM', ',', ' ',
   'VAR', ',', ' ', 'ADDRESS', ' ', '{',
   ' ', 'font-style', ':', ' ', 'italic', ' ', '}', ' ',
   'PRE', ',', ' ', 'TT', ',', ' ', 'CODE', ',', ' ',
   'KBD', ',', ' ', 'SAMP', ' ', '{',
   ' ', 'font-family', ':', ' ', 'monospace', ' ', '}', ' ',
   'PRE', ' ', '{', ' ', 'white-space', ':', ' ', 'pre', ' ', '}', ' ',
   'BIG', ' ', '{', ' ', 'font-size', ':', ' ', '1.17em', ' ', '}', ' ',
   'SMALL', ',', ' ', 'SUB', ',', ' ', 'SUP', ' ', '{', ' ', 'font-size', ':', ' ', '.83em', ' ', '}', ' ',
   'SUB', ' ', '{', ' ', 'vertical-align', ':', ' ', 'sub', ' ', '}', ' ',
   'SUP', ' ', '{', ' ', 'vertical-align', ':', ' ', 'super', ' ', '}', ' ',
   'S', ',', ' ', 'STRIKE', ',', ' ', 'DEL', ' ', '{',
   ' ', 'text-decoration', ':', ' ', 'line-through', ' ', '}', ' ',
   'HR', ' ', '{', ' ', 'border', ':', ' ', '1px', ' ', 'inset', ' ', '}', ' ',
   'OL', ',', ' ', 'UL', ',', ' ', 'DIR', ',', ' ',
   'MENU', ',', ' ', 'DD', ' ', '{',
   ' ', 'margin-left', ':', ' ', '40px', ' ', '}', ' ',
   'OL', ' ', '{', ' ', 'list-style-type', ':', ' ', 'decimal', ' ', '}', ' ',
   'OL', ' ', 'UL', ',', ' ', 'UL', ' ', 'OL', ',', ' ',
   'UL', ' ', 'UL', ',', ' ', 'OL', ' ', 'OL', ' ', '{',
   ' ', 'margin-top', ':', ' ', '0', ';',
   ' ', 'margin-bottom', ':', ' ', '0', ' ', '}', ' ',
   'U', ',', ' ', 'INS', ' ', '{',
   ' ', 'text-decoration', ':', ' ', 'underline', ' ', '}', ' ',
   'CENTER', ' ', '{', ' ', 'text-align', ':', ' ', 'center', ' ', '}', ' ',
   'BR', ':', 'before', ' ', '{',
   ' ', 'content', ':', ' ', '"\\a "', ' ', '}', ' ',
   'COLOR_NOHASH', ' ', '{', ' ', 'color', ':', '987E81', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex12', function testLex12() {
  assertLexedCss(
    '/* An example of style for HTML 4.0\'s ABBR/ACRONYM elements */\n\
\n\
ABBR, ACRONYM   { font-variant: small-caps; letter-spacing: 0.1em }\n\
A[href]         { text-decoration: underline }\n\
:focus          { outline: thin dotted invert }',
    'ABBR', ',', ' ', 'ACRONYM', ' ', '{',
    ' ', 'font-variant', ':', ' ', 'small-caps', ';', 
    ' ', 'letter-spacing', ':', ' ', '0.1em', ' ', '}', ' ',
    'A', '[', 'href', ']', ' ', '{',
    ' ', 'text-decoration', ':', ' ', 'underline', ' ', '}', ' ',
    ':', 'focus', ' ', '{',
    ' ', 'outline', ':', ' ', 'thin', ' ', 'dotted', ' ', 'invert', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex13', function testLex13() {
  assertLexedCss(
    '/* Begin bidirectionality settings (do not change) */\n\
BDO[DIR="ltr"]  { direction: ltr; unicode-bidi: bidi-override }\n\
BDO[DIR="rtl"]  { direction: rtl; unicode-bidi: bidi-override }\n\
\n\
*[DIR="ltr"]    { direction: ltr; unicode-bidi: embed }\n\
*[DIR="rtl"]    { direction: rtl; unicode-bidi: embed }\n\
\n\
/* Elements that are block-level in HTML4 */\n\
ADDRESS, BLOCKQUOTE, BODY, DD, DIV, DL, DT, FIELDSET, \n\
FORM, FRAME, FRAMESET, H1, H2, H3, H4, H5, H6, IFRAME,\n\
NOSCRIPT, NOFRAMES, OBJECT, OL, P, UL, APPLET, CENTER, \n\
DIR, HR, MENU, PRE, LI, TABLE, TR, THEAD, TBODY, TFOOT, \n\
COL, COLGROUP, TD, TH, CAPTION \n\
                { unicode-bidi: embed }\n\
/* End bidi settings */',
    'BDO', '[', 'DIR', '=', '"ltr"', ']', ' ', '{',
    ' ', 'direction', ':', ' ', 'ltr', ';',
    ' ', 'unicode-bidi', ':', ' ', 'bidi-override', ' ', '}', ' ',
    'BDO', '[', 'DIR', '=', '"rtl"', ']', ' ', '{',
    ' ', 'direction', ':', ' ', 'rtl', ';',
    ' ', 'unicode-bidi', ':', ' ', 'bidi-override', ' ', '}', ' ',
    '*', '[', 'DIR', '=', '"ltr"', ']', ' ', '{',
    ' ', 'direction', ':', ' ', 'ltr', ';',
    ' ', 'unicode-bidi', ':', ' ', 'embed', ' ', '}', ' ',
    '*', '[', 'DIR', '=', '"rtl"', ']', ' ', '{',
    ' ', 'direction', ':', ' ', 'rtl', ';',
    ' ', 'unicode-bidi', ':', ' ', 'embed', ' ', '}', ' ',
    'ADDRESS', ',', ' ', 'BLOCKQUOTE', ',', ' ', 'BODY', ',',
    ' ', 'DD', ',', ' ', 'DIV', ',', ' ', 'DL', ',', ' ', 'DT', ',',
    ' ', 'FIELDSET', ',', ' ',
    'FORM', ',', ' ', 'FRAME', ',', ' ', 'FRAMESET', ',', ' ', 'H1', ',',
    ' ', 'H2', ',', ' ', 'H3', ',', ' ', 'H4', ',', ' ', 'H5', ',',
    ' ', 'H6', ',', ' ', 'IFRAME', ',', ' ',
    'NOSCRIPT', ',', ' ', 'NOFRAMES', ',', ' ', 'OBJECT', ',',
    ' ', 'OL', ',', ' ', 'P', ',', ' ', 'UL', ',', ' ', 'APPLET', ',',
    ' ', 'CENTER', ',', ' ',
    'DIR', ',', ' ', 'HR', ',', ' ', 'MENU', ',', ' ', 'PRE', ',',
    ' ', 'LI', ',', ' ', 'TABLE', ',', ' ', 'TR', ',', ' ', 'THEAD', ',',
    ' ', 'TBODY', ',', ' ', 'TFOOT', ',', ' ',
    'COL', ',', ' ', 'COLGROUP', ',', ' ', 'TD', ',', ' ', 'TH', ',',
    ' ', 'CAPTION', ' ',
    '{', ' ', 'unicode-bidi', ':', ' ', 'embed', ' ', '}', ' ');
  jsunit.pass();
});

jsunitRegister('testLex14', function testLex14() {
  assertLexedCss(
    '\n\
@media print {\n\
  /* @page         { margin: 10% }  */ /* not allowed according to spec */\n\
  H1, H2, H3,\n\
  H4, H5, H6    { page-break-after: avoid; page-break-inside: avoid }\n\
  BLOCKQUOTE, \n\
  PRE           { page-break-inside: avoid }\n\
  UL, OL, DL    { page-break-before: avoid }\n\
}',
    '@media', ' ', 'print', ' ', '{', ' ',
    'H1', ',', ' ', 'H2', ',', ' ', 'H3', ',', ' ',
    'H4', ',', ' ', 'H5', ',', ' ', 'H6', ' ', '{',
    ' ', 'page-break-after', ':', ' ', 'avoid', ';',
    ' ', 'page-break-inside', ':', ' ', 'avoid', ' ', '}', ' ',
    'BLOCKQUOTE', ',', ' ',
    'PRE', ' ', '{', ' ', 'page-break-inside', ':', ' ', 'avoid', ' ', '}', ' ',
    'UL', ',', ' ', 'OL', ',', ' ', 'DL', ' ', '{',
    ' ', 'page-break-before', ':', ' ', 'avoid', ' ', '}', ' ',
    '}');
  jsunit.pass();
});

jsunitRegister('testLex15', function testLex15() {
  assertLexedCss(
    '@media speech {\n\
  H1, H2, H3, \n\
  H4, H5, H6    { voice-family: paul, male; stress: 20; richness: 90 }\n\
  H1            { pitch: x-low; pitch-range: 90 }\n\
  H2            { pitch: x-low; pitch-range: 80 }\n\
  H3            { pitch: low; pitch-range: 70 }\n\
  H4            { pitch: medium; pitch-range: 60 }\n\
  H5            { pitch: medium; pitch-range: 50 }\n\
  H6            { pitch: medium; pitch-range: 40 }\n\
  LI, DT, DD    { pitch: medium; richness: 60 }\n\
  DT            { stress: 80 }\n\
  PRE, CODE, TT { pitch: medium; pitch-range: 0; stress: 0; richness: 80 }\n\
  EM            { pitch: medium; pitch-range: 60; stress: 60; richness: 50 }\n\
  STRONG        { pitch: medium; pitch-range: 60; stress: 90; richness: 90 }\n\
  DFN           { pitch: high; pitch-range: 60; stress: 60 }\n\
  S, STRIKE     { richness: 0 }\n\
  I             { pitch: medium; pitch-range: 60; stress: 60; richness: 50 }\n\
  B             { pitch: medium; pitch-range: 60; stress: 90; richness: 90 }\n\
  U             { richness: 0 }\n\
  A:link        { voice-family: harry, male }\n\
  A:visited     { voice-family: betty, female }\n\
  A:active      { voice-family: betty, female; pitch-range: 80; pitch: x-high }\n\
}',
    '@media', ' ', 'speech', ' ', '{', ' ',
    'H1', ',', ' ', 'H2', ',', ' ', 'H3', ',', ' ',
    'H4', ',', ' ', 'H5', ',', ' ', 'H6',
    ' ', '{', ' ', 'voice-family', ':', ' ', 'paul', ',', ' ', 'male', ';',
    ' ', 'stress', ':', ' ', '20', ';', ' ', 'richness', ':', ' ', '90',
    ' ', '}', ' ',
    'H1', ' ', '{', ' ', 'pitch', ':', ' ', 'x-low', ';',
    ' ', 'pitch-range', ':', ' ', '90', ' ', '}', ' ',
    'H2', ' ', '{', ' ', 'pitch', ':', ' ', 'x-low', ';',
    ' ', 'pitch-range', ':', ' ', '80', ' ', '}', ' ',
    'H3', ' ', '{', ' ', 'pitch', ':', ' ', 'low', ';',
    ' ', 'pitch-range', ':', ' ', '70', ' ', '}', ' ',
    'H4', ' ', '{', ' ', 'pitch', ':', ' ', 'medium', ';',
    ' ', 'pitch-range', ':', ' ', '60', ' ', '}', ' ',
    'H5', ' ', '{', ' ', 'pitch', ':', ' ', 'medium', ';',
    ' ', 'pitch-range', ':', ' ', '50', ' ', '}', ' ',
    'H6', ' ', '{', ' ', 'pitch', ':', ' ', 'medium', ';',
    ' ', 'pitch-range', ':', ' ', '40', ' ', '}', ' ',
    'LI', ',', ' ', 'DT', ',', ' ', 'DD', ' ', '{',
    ' ', 'pitch', ':', ' ', 'medium', ';', 
    ' ', 'richness', ':', ' ', '60', ' ', '}', ' ',
    'DT', ' ', '{', ' ', 'stress', ':', ' ', '80', ' ', '}', ' ',
    'PRE', ',', ' ', 'CODE', ',', ' ', 'TT', ' ', '{',
    ' ', 'pitch', ':', ' ', 'medium', ';',
    ' ', 'pitch-range', ':', ' ', '0', ';',
    ' ', 'stress', ':', ' ', '0', ';',
    ' ', 'richness', ':', ' ', '80', ' ', '}', ' ',
    'EM', ' ', '{', ' ', 'pitch', ':', ' ', 'medium', ';',
    ' ', 'pitch-range', ':', ' ', '60', ';',
    ' ', 'stress', ':', ' ', '60', ';',
    ' ', 'richness', ':', ' ', '50', ' ', '}', ' ',
    'STRONG', ' ', '{', ' ', 'pitch', ':', ' ', 'medium', ';',
    ' ', 'pitch-range', ':', ' ', '60', ';',
    ' ', 'stress', ':', ' ', '90', ';',
    ' ', 'richness', ':', ' ', '90', ' ', '}', ' ',
    'DFN', ' ', '{', ' ', 'pitch', ':', ' ', 'high', ';',
    ' ', 'pitch-range', ':', ' ', '60', ';', ' ',
    'stress', ':', ' ', '60', ' ', '}', ' ',
    'S', ',', ' ', 'STRIKE', ' ', '{',
    ' ', 'richness', ':', ' ', '0', ' ', '}', ' ',
    'I', ' ', '{', ' ', 'pitch', ':', ' ', 'medium', ';',
    ' ', 'pitch-range', ':', ' ', '60', ';',
    ' ', 'stress', ':', ' ', '60', ';',
    ' ', 'richness', ':', ' ', '50', ' ', '}', ' ',
    'B', ' ', '{', ' ', 'pitch', ':', ' ', 'medium', ';',
    ' ', 'pitch-range', ':', ' ', '60', ';',
    ' ', 'stress', ':', ' ', '90', ';',
    ' ', 'richness', ':', ' ', '90', ' ', '}', ' ',
    'U', ' ', '{', ' ', 'richness', ':', ' ', '0', ' ', '}', ' ',
    'A', ':', 'link', ' ', '{',
    ' ', 'voice-family', ':', ' ', 'harry', ',', ' ', 'male', ' ', '}', ' ',
    'A', ':', 'visited', ' ', '{',
    ' ', 'voice-family', ':', ' ', 'betty', ',', ' ', 'female', ' ', '}', ' ',
    'A', ':', 'active', ' ', '{',
    ' ', 'voice-family', ':', ' ', 'betty', ',', ' ', 'female', ';',
    ' ', 'pitch-range', ':', ' ', '80', ';',
    ' ', 'pitch', ':', ' ', 'x-high', ' ', '}', ' ',
    '}');
  jsunit.pass();
});

jsunitRegister('testLex16', function testLex16() {
  assertLexedCss(
    'FOO > BAR + BAZ {  }',
    'FOO', ' ', '>', ' ', 'BAR', ' ', '+', ' ', 'BAZ', ' ', '{', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex17', function testLex17() {
  assertLexedCss(
    'A[href] BOO[zwop |= \'hello\']:blinky {\n\
  color: #fff;\n\
  background: +#000000 ! important\n\
}',
    'A', '[', 'href', ']',
    ' ', 'BOO', '[', 'zwop', ' ', '|=', ' ', '"hello"', ']', ':', 'blinky',
    ' ', '{', ' ',
    'color', ':', ' ', '#fff', ';', ' ',
    'background', ':', ' ', '+', '#000000', ' ', '!', ' ', 'important', ' ',
    '}');
  jsunit.pass();
});

jsunitRegister('testLex18', function testLex18() {
  assertLexedCss(
    '.myclass[attr ~= almost] #id:hover(languidly) {\n\
  font-weight: super(bold / italic)\n\
}',
    '.', 'myclass', '[', 'attr', ' ', '~=', ' ', 'almost', ']',
    ' ', '#id', ':', 'hover(', 'languidly', ')', ' ', '{', ' ',
    'font-weight', ':', ' ', 'super(', 'bold', ' ', '/', ' ', 'italic', ')',
    ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex19', function testLex19() {
  assertLexedCss(
    '/* The RHS of the attribute comparison operator in the following cases\n\
 * will parse to a StringLiteral since it is surrounded by quotes. */\n\
foo[attr = \'bar\'] {}\n\
foo[attr = "bar"] {}\n\
foo[attr ~= \'bar baz\'] {}\n\
foo[attr |= \'bar-baz\'] {}',
'foo', '[', 'attr', ' ', '=', ' ', '"bar"', ']', ' ', '{', '}', ' ',
'foo', '[', 'attr', ' ', '=', ' ', '"bar"', ']', ' ', '{', '}', ' ',
'foo', '[', 'attr', ' ', '~=', ' ', '"bar baz"', ']', ' ', '{', '}', ' ',
'foo', '[', 'attr', ' ', '|=', ' ', '"bar-baz"', ']', ' ', '{', '}');
  jsunit.pass();
});

jsunitRegister('testLex20', function testLex20() {
  assertLexedCss(
    '/* The RHS of the attribute comparison operator in the following cases\n\
 * will parse to an IdentLiteral since it is unquoted. */\n\
foo[attr = bar] {}\n\
foo[attr |= bar-baz] {}',
    'foo', '[', 'attr', ' ', '=', ' ', 'bar', ']', ' ', '{', '}', ' ',
    'foo', '[', 'attr', ' ', '|=', ' ', 'bar-baz', ']', ' ', '{', '}');
  jsunit.pass();
});

jsunitRegister('testLex21', function testLex21() {
  assertLexedCss(
    'foo.bar { }',
    'foo', '.', 'bar', ' ', '{', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex22', function testLex22() {
  assertLexedCss(
    'foo .bar { }',
    'foo', ' ', '.', 'bar', ' ', '{', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex23', function testLex23() {
  assertLexedCss(
    'foo .quoted { content: \'contains \\\'quotes\\\'\' }',
    'foo', ' ', '.', 'quoted', ' ', '{', ' ', 'content', ':', ' ',
    '"contains \'quotes\'"', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex24', function testLex24() {
  assertLexedCss(
    'foo .dquoted { content: "\'contains\'\\\\\\"double quotes\\"" }',
    'foo', ' ', '.', 'dquoted', ' ', '{', ' ', 'content', ':', ' ',
    '"\'contains\'\\\\\\22 double quotes\\22 "', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex25', function testLex25() {
  assertLexedCss(
    'foo .long { content: \'spans \\\n\
multiple \\\n\
lines\' }\n',
    'foo', ' ', '.', 'long', ' ', '{', ' ', 'content', ':', ' ',
    '"spans multiple lines"', ' ', '}', ' ');
  jsunit.pass();
});

jsunitRegister('testLex26', function testLex26() {
  assertLexedCss(
    'foo .extended-unicode { content: \'a1 \\61\\31  \\0000611 \\000061 1 \\0061\\0031\' }',
    'foo', ' ', '.', 'extended-unicode', ' ', '{', ' ', 'content', ':', ' ',
    '"a1 a1 a1 a1 a1"', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex27', function testLex27() {
  assertLexedCss(
    '/* CSS 2.1 allows _ in identifiers */\n\
#a_b {}\n\
.a_b {}',
    '#a_b', ' ', '{', '}', ' ', '.', 'a_b', ' ', '{', '}');
  jsunit.pass();
});

jsunitRegister('testLex28', function testLex28() {
  assertLexedCss(
    '#xxx {\n\
  filter:alpha(opacity=50);\n\
}',
    '#xxx', ' ', '{', ' ',
    'filter', ':', 'alpha(', 'opacity', '=', '50', ')', ';', ' ',
    '}');
  jsunit.pass();
});

jsunitRegister('testLex29', function testLex29() {
  assertLexedCss(
    'p { margin: -3px -3px }\n\
p { margin: -3px 3px }',
    'p', ' ', '{', ' ', 'margin', ':', ' ', '-3px', ' ', '-3px', ' ', '}', ' ',
    'p', ' ', '{', ' ', 'margin', ':', ' ', '-3px', ' ', '3px', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex30', function testLex30() {
  assertLexedCss(
    '<!-- \n\
p { content: \'-->foo<!--\' }  /* - -> bar <!--- */\n\
-->',
    'p', ' ', '{', ' ', 'content', ':',
    ' ', '"--\\3e foo\\3c !--"', ' ', '}', ' ');
  jsunit.pass();
});

jsunitRegister('testLex31', function testLex31() {
  assertLexedCss(
    '@bogus hello {\n\
  balanced { curly "brackets" };\n\
}',
    '@bogus', ' ', 'hello', ' ', '{', ' ',
    'balanced', ' ', '{', ' ', 'curly', ' ', '"brackets"', ' ', '}', ';',
    ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex32', function testLex32() {
  assertLexedCss(
    '/* Not treated as part of the bogus symbol block */\n\
* { color: red }',
    '*', ' ', '{', ' ', 'color', ':', ' ', 'red', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex33', function testLex33() {
  assertLexedCss(
    '@unknown(\'hi\');',
    '@unknown', '(', '"hi"', ')', ';');
  jsunit.pass();
});

jsunitRegister('testLex34', function testLex34() {
  assertLexedCss(
    '/* list applies to body, input, and td.  Extraneous , skip. */\n\
body, input, , td {\n\
  /* missing property name causes skip until ; */\n\
  Arial, sans-serif;\n\
  color: blue;\n\
  /* missing value.  skipped. */\n\
  background-color:\n\
}',
    'body', ',', ' ', 'input', ',', ' ', ',', ' ', 'td',
    ' ', '{', ' ', 'Arial', ',', ' ', 'sans-serif', ';',
    ' ', 'color', ':', ' ', 'blue', ';',
    ' ', 'background-color', ':', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex35', function testLex35() {
  assertLexedCss(
    '/* not thrown out, but 2 digit color is discarded */\n\
@media print {\n\
  * { color: black !important; background-color: #ff }\n\
}',
    '@media', ' ', 'print', ' ', '{',
    ' ', '*', ' ', '{', ' ', 'color', ':', ' ', 'black', ' ', '!', 'important', ';', ' ', 'background-color', ':', ' ', '#ff', ' ', '}', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex36', function testLex36() {
  assertLexedCss(
    '@page :{broken { margin-left: 4cm; }  /* extra { */',
    '@page', ' ', ':', '{', 'broken', ' ', '{',
    ' ', 'margin-left', ':', ' ', '4cm', ';', ' ', '}', ' ');
  jsunit.pass();
});

jsunitRegister('testLex37', function testLex37() {
  assertLexedCss(
    '@page .broken {}  /* no colon */',
    '@page', ' ', '.', 'broken', ' ', '{', '}', ' ');
  jsunit.pass();
});

jsunitRegister('testLex38', function testLex38() {
  assertLexedCss(
    '@page :{}  /* no pseudo-page */',
    '@page', ' ', ':', '{', '}', ' ');
  jsunit.pass();
});

jsunitRegister('testLex39', function testLex39() {
  assertLexedCss(
    '@page :broken {  /* missing \'}\' */',
    '@page', ' ', ':', 'broken', ' ', '{', ' ');
  jsunit.pass();
});

jsunitRegister('testLex40', function testLex40() {
  assertLexedCss(
    '@page :left { margin-left: 4cm;; size: 8.5in 11in; }  /* ok */',
    '@page', ' ', ':', 'left', ' ', '{',
    ' ', 'margin-left', ':', ' ', '4cm', ';', ';',
    ' ', 'size', ':', ' ', '8.5in', ' ', '11in', ';', ' ', '}', ' ');
  jsunit.pass();
});

jsunitRegister('testLex41', function testLex41() {
  assertLexedCss(
    '/* missing property */\n\
body { : blue }',
    'body', ' ', '{', ' ', ':', ' ', 'blue', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex42', function testLex42() {
  assertLexedCss(
    'color: blue;',
    'color', ':', ' ', 'blue', ';');
  jsunit.pass();
});

jsunitRegister('testLex43', function testLex43() {
  assertLexedCss(
    'a:visited, :unvisited, a::before { color: blue }',
    'a', ':', 'visited', ',',
    ' ', ':', 'unvisited', ',',
    ' ', 'a', ':', ':', 'before',
    ' ', '{', ' ', 'color', ':', ' ', 'blue', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex44', function testLex44() {
  assertLexedCss(
    '/* not a valid wildcard wiseguy */\n\
? { color: blue }',
    '?', ' ', '{', ' ', 'color', ':', ' ', 'blue', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex45', function testLex45() {
  assertLexedCss(
    '/* lots of invalid selectors */\n\
.3, #333, a[href=\'foo\', a[href=], a[=\'foo\'], body:, ok {}',
    '.3', ',',
    ' ', '#333', ',',
    ' ', 'a', '[', 'href', '=', '"foo"', ',',
    ' ', 'a', '[', 'href', '=', ']', ',',
    ' ', 'a', '[', '=', '"foo"', ']', ',',
    ' ', 'body', ':', ',',
    ' ', 'ok', ' ', '{', '}');
  jsunit.pass();
});

jsunitRegister('testLex46', function testLex46() {
  assertLexedCss(
    '/* all invalid selectors */\n\
#333, .3, .,  {}',
    '#333', ',', ' ', '.3', ',', ' ', '.', ',', ' ', '{', '}');
  jsunit.pass();
});

jsunitRegister('testLex47', function testLex47() {
  assertLexedCss(
    '/* valid selectors missing a body */\n\
a, b, i, p, q, s, u, ;',
    'a', ',', ' ', 'b', ',', ' ', 'i', ',', ' ', 'p', ',', ' ', 'q', ',',
    ' ', 's', ',', ' ', 'u', ',', ' ', ';');
  jsunit.pass();
});

jsunitRegister('testLex48', function testLex48() {
  assertLexedCss(
    '/* expression cruft. Make sure parsing before and after ok. */\n\
a1 { a: ok;  color: red:;              a: ok }  /* cruft after : */\n\
a2 { a: ok;  width: 0 !import;         a: ok }  /* !important misspelled */\n\
a3 { a: ok;  unicode-range: U+0-FFFF;  a: ok }  /* ok */ \n\
a4 { a: ok;  color: #g00;              a: ok }  /* bad hex digit */\n\
a5 { a: ok;  image: url(\'::\');       a: ok }  /* malformed URI */\n\
a6 { a: ok;  image: url(::);           a: ok }  /* malformed URI */',
    'a1', ' ', '{', ' ', 'a', ':', ' ', 'ok', ';',
    ' ', 'color', ':', ' ', 'red', ':', ';',
    ' ', 'a', ':', ' ', 'ok', ' ', '}', ' ',
    'a2', ' ', '{', ' ', 'a', ':', ' ', 'ok', ';',
    ' ', 'width', ':', ' ', '0', ' ', '!', 'import', ';',
    ' ', 'a', ':', ' ', 'ok', ' ', '}', ' ',
    'a3', ' ', '{', ' ', 'a', ':', ' ', 'ok', ';',
    ' ', 'unicode-range', ':', ' ', 'U+0-FFFF', ';',
    ' ', 'a', ':', ' ', 'ok', ' ', '}', ' ',
    'a4', ' ', '{', ' ', 'a', ':', ' ', 'ok', ';',
    ' ', 'color', ':', ' ', '#g00',
    ';', ' ', 'a', ':', ' ', 'ok', ' ', '}', ' ',
    'a5', ' ', '{', ' ', 'a', ':', ' ', 'ok', ';',
    ' ', 'image', ':', ' ', 'url("::")', ';',
    ' ', 'a', ':', ' ', 'ok', ' ', '}', ' ',
    'a6', ' ', '{', ' ', 'a', ':', ' ', 'ok', ';',
    ' ', 'image', ':', ' ', 'url("::")', ';',
    ' ', 'a', ':', ' ', 'ok', ' ', '}', ' ');
  jsunit.pass();
});

jsunitRegister('testLex49', function testLex49() {
  assertLexedCss(
    '/* functions allow for lots of mischief */\n\
a7 { a: ok;  font-size: expression(Math.random());  a: ok }  /* ok.  TODO */\n\
a8 { a: ok;  font-size: expression(Math.random();   a: ok }  /* missing paren */\n\
a9 { a: ok;  font-size: expression();               a: ok }  /* missing param */\n\
aa { a: ok;  font-size: expression({});             a: ok }  /* bad param */',
    'a7', ' ', '{', ' ', 'a', ':', ' ', 'ok', ';', ' ', 'font-size', ':',
    ' ', 'expression(', 'Math', '.', 'random(', ')', ')', ';',
    ' ', 'a', ':', ' ', 'ok', ' ', '}', ' ',
    'a8', ' ', '{', ' ', 'a', ':', ' ', 'ok', ';', ' ', 'font-size', ':',
    ' ', 'expression(', 'Math', '.', 'random(', ')', ';',
    ' ', 'a', ':', ' ', 'ok', ' ', '}', ' ',
    'a9', ' ', '{', ' ', 'a', ':', ' ', 'ok', ';', ' ', 'font-size', ':',
    ' ', 'expression(', ')', ';', ' ', 'a', ':', ' ', 'ok', ' ', '}', ' ',
    'aa', ' ', '{', ' ', 'a', ':', ' ', 'ok', ';', ' ', 'font-size', ':',
    ' ', 'expression(', '{', '}', ')', ';',
    ' ', 'a', ':', ' ', 'ok', ' ', '}', ' ');
  jsunit.pass();
});

jsunitRegister('testLex50', function testLex50() {
  assertLexedCss(
    '@font-face; @font-face {}\n\
@font-face @font-face { font-family: Letters; src: url(\'Letters.ttf\') }',
    '@font-face', ';', ' ', '@font-face', ' ', '{', '}', ' ',
    '@font-face', ' ', '@font-face', ' ', '{',
    ' ', 'font-family', ':', ' ', 'Letters', ';',
    ' ', 'src', ':', ' ', 'url("Letters.ttf")', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex51', function testLex51() {
  assertLexedCss(
    '@charset "utf-8";',
    '@charset', ' ', '"utf-8"', ';');
  jsunit.pass();
});

jsunitRegister('testLex52', function testLex52() {
  assertLexedCss(
    '@import url(\'nonsense.css\') mumbling, blather;',
    '@import', ' ', 'url("nonsense.css")',
    ' ', 'mumbling', ',', ' ', 'blather', ';');
  jsunit.pass();
});

jsunitRegister('testLex53', function testLex53() {
  assertLexedCss(
    '@page { background: url(\'sparkley.jpg\'); }',
    '@page', ' ', '{', ' ', 'background', ':',
    ' ', 'url("sparkley.jpg")', ';', ' ', '}');
  jsunit.pass();
});

jsunitRegister('testLex54', function testLex54() {
  assertLexedCss(
    '@charset "non-utf-8";',
    '@charset', ' ', '"non-utf-8"', ';');
  jsunit.pass();
});

jsunitRegister('testLex55', function testLex55() {
  assertLexedCss(
    '/* non utf-8 */\n\
@import \'foo.css\';\n\
@unknown(\'hi\');',
    '@import', ' ', '"foo.css"', ';', ' ',
    '@unknown', '(', '"hi"', ')', ';');
  jsunit.pass();
});

jsunitRegister('testLex56', function testLex56() {
  assertLexedCss(
    '\ufeffvalues: 100% -12.5% \'\' "" .5em 0 12 url() url(\'\') url("");',
    'values', ':', ' ', '100%', ' ', '-12.5%', ' ', '""', ' ', '""',
    ' ', '.5em', ' ', '0', ' ', '12',
    ' ', 'url("")', ' ', 'url("")', ' ', 'url("")', ';');
  jsunit.pass();
});

jsunitRegister('testLex57', function testLex56() {
  assertLexedCss(
    '// line comment 1\nline2\n//line comment 3\r\nline4//line comment 4\f',
    'line2', ' ', 'line4', ' ');
  jsunit.pass();
});
