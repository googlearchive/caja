// a test file for the parser

var i = 1 + 1;
/*! function int(int, int, int, int, int, int) */
function F(a, b, c, d, e) {
  // test operator precedence
  return a + b * c / d + e;
}

// warn on keywords as identifier
/*! int */ var x = 0, y, z, else;

x += y = z = 14;

if (y === 1 - 1 - 1) {
  print(z);
}

if (x < y) {
  print(x);
} else if (x == y)
  print(y)
else
  print(z);

arr = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,,13];

for (/*!int */ var i = 0; i < a.length; ++i) {
  print(a[i] * 2);
}

/*! Object with int x, int y */
var obj = {
  x: 2,
  y: 3,  // should warn on this trailing comma
};

// test switch statements
switch (foo()) {
  case 1:
    return "panic";
  case (2):
    if (a === 4) {
      break;
    }
  case BAR:
    f();
    g();
  default:
    zowie(wowie());;;
}

for (var i in array) {
  f(i);
}

for (i in [1, 2, 3]) {
  f(i);
}

var o = new Object;

switch (n) {
  case 0:
  case 1:
    {
      foo();
    }
    break;
  default:
    panic();
  case 2:
    bar();
}

// test that lexer properly treats slash as a division sign
var n = (1. / 2);

foo: while (x) {
  if (f()) { continue foo; }
  --x;
}

continue bar;

label: switch (x) {
  case 4:
    break label;
}

// you can label other statements;
useless: hello;

for (arr[arr.length - 1] in o);

a/*
multiline comments act as newlines for the purposes of semicolon insertion
according to Section 5.1.2 of ES3.
*/b
