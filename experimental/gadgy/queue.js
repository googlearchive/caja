/**
 * A queue data type.
 *
 * @return a queue.
 */
'use strict';
'use cajita';

// Array Remove - By John Resig (MIT Licensed)
var arrayRemove = function(array, from, to) {
  var rest = array.slice((to || from) + 1 || array.length);
  array.length = from < 0 ? array.length + from : from;
  return array.push.apply(array, rest);
};

var contents = [];

cajita.freeze({
  enqueue: function(item) {
    contents.push(item);
  },
  dequeue: function() {
    if (contents.length === 0) { return undefined; }
    var result = contents[0];
    arrayRemove(contents, 0);
    return result;
  },
  size: function() {
    return contents.length;
  },
  isEmpty: function() {
    return contents.length === 0;
  }
});
