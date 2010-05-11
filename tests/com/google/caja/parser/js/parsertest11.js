({
   foo: bar,
   get: set,
   set: get,
   get x() { return 1; },
   set x(v) { this.x_ = v; },
   get "y"(n) { "use strict"; return n; },
   get get(n) { return 2; },
   set "a-b"(x, y) { x(y); }
 })
