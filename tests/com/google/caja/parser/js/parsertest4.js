// test parenthesization corner cases

3 + 1 * 2;
(3 + 1) * 2;
3 + (1 * 2);
-(-3);
+(+4);
-(3);
(a ? b : c) ? (d ? e : f) : (g ? h : i);
(function () { })();
f((a, b), c, d);
o = { x: (f(), n), y: -1 };
[ (f(), i), b ];
a || (b && c) && (a || d);
a || ((b && c) && (a || d));
(a || (b && c)) && (a || d);
(3).foo;
/foo//1;
