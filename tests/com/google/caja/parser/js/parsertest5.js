(function () { alert('hello world'); })();

(function a() { alert('hello world'); })();

(function () { alert('hello world'); });  // semi is part of expression
(function b() { alert('hello world'); });  // not a declaration either

function c() { alert('hello world'); };  // semi is actually its own statement

// without a name, it's not a declaration, so semi is part of the expression
function () { alert('hello world'); };

var d = function () { alert('hello world'); };

// declared name can be different from the name of the value
var e = function f() { alert('hello world'); };

({
  x: 4,
  y: 5,
  invoke: function () { doSomething(this, this.x + this.y); }
}).invoke();

// a block
{ block: 4 }

// an object constructor
({ objet: 4 });

ev\u0061l('foo');

// e-accent v full-width-a l.  Should not be rendered with hex escapes.
\u00e9v\uff41l('foo');
