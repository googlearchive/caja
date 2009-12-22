includeScript('factorial');

/**
 *  Use the factorial() function.
 */

(function() {
  var testDiv = document.createElement('div');
  document.body.appendChild(testDiv);
  testDiv.innerHTML = '<p>3! is: ' + factorial(3) + '</p>';
})();

/**
 *  Use the square() function.
 */

(function() {
  var testDiv = document.createElement('div');
  document.body.appendChild(testDiv);
  testDiv.innerHTML = '<p>The square of 3 is: ' + square(3) + '</p>';
})();

/**
 * Load libCube and use the cube() function.
 */

Q.when(includeScript.async('libCube'), function() {
  var testDiv = document.createElement('div');
  document.body.appendChild(testDiv);
  testDiv.innerHTML = '<p>The cube of 3 is: ' + cube(3) + '</p>';
});
