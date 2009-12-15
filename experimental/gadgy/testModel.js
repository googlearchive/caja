/**
 * Gadgy test model object.
 *
 * @param publish event publishing API provided by guardian.
 *
 * @return an object representing a counter, providing methods "get"
 *     and "increment".
 */
'use strict';
'use cajita';

var count = 0;

cajita.freeze({
  get: function() {
    var result = count;
    count = -999;  // Enter invalid state
    publish('count', { count: count });  // So UI sees our plight
    return result;
  },
  increment: function(x) {
    if (count < 0) { return; }  // Invalid state
    if (count + x > 10) {
      count = -999;  // Enter invalid state
      publish('count', { count: count });  // So UI sees our plight
      throw 'This would make the count too large (max is 10)';
    }
    count += x;
    publish('count', { count: count });
    return count;
  },
  divide: function(x) {
    if (count < 0) { return; }  // Invalid state
    if (count === 0) {
      count = -999;  // Enter invalid state
      publish('count', { count: count });  // So UI sees our plight
      throw 'Divide by zero not allowed';
    }
    count = Math.floor(x / count);
    publish('count', { count: count });
  },
  toString: function() {
    return 'count=' + count;
  }
});
