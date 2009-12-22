// Notice that 'libCube' module can assume the existence of the square()
// function. The Valija environment acts like a regular JavaScript global
// scope, and since 'libSquare' is expected to have been already loaded,
// the square() function is available.
function cube(x) { return x * square(x); }
