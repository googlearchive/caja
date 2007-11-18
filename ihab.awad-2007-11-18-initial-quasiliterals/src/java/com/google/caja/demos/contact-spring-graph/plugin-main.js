// Copyright (C) 2006 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview
 * Initialization code for the spring-graph calendar plugin.
 * <p>
 * The spring graph plugin displays a graph of relationships between people
 * based on how frequently they co-attend events on a calendar.
 *
 * <h2>External dependencies</h2>
 * The graph widget itself is defined externally in <tt>spring-graph.js</tt>,
 * and, like this file, is untrusted code.
 * <p><tt>plugin-api.js</tt> contains the methods that the embedding Calendar
 * application exports to this plugin.
 * 
 * @author mikesamuel@gmail.com
 */

if (!Date.now) {
  Date.now = function () { return (new Date()).getTime(); };
}

(function () {
  var calendar = getCalendar();
  var events = calendar.getEvents();

  // create nodes for each person in the contact list
  var contactSet = {};
  for (var i = events.length; --i >= 0;) {
    var guests = events[i].getAttendees();
    for (var j = guests.length; --j >= 0;) {
      var email = guests[j];
      contactSet[email] = null;
    }
  }
  var contacts = [];
  for (var email in contactSet) {
    var name = email.replace(/@.*/, '');
    name = name.charAt(0).toUpperCase() + name.substring(1);
    contacts.push({ description: email, name: name });
  }
  log('got ' + contacts.length + ' contacts');
  // The comparator function is called with "this" as the window, but since this
  // implementation doesn't reference "this", the compiler doesn't insert a
  // runtime check that "this" is not the global scope.
  contacts.sort(
       function (a, b) {
         a = a.description;
         b = b.description;
         if (a === b) { return 0; }
         // sort ME first, so that it is the anchor
         if (ME === a) { return -1; }
         if (ME === b) { return 1; }
         return a < b ? -1 : 1;
       });
  for (var i = contacts.length; --i >= 0;) {
    contactSet[contacts[i].description] = i;
  }
  // contactSet now maps email addresses to index in the node list

  // create the DOM representation of the graph
  getElementById('base').setInnerHTML(graph(contacts));

  var domNodes = [];
  var graphContainer = getElementById('graph');
  for (var domNode = graphContainer.getFirstChild();
       domNode; domNode = domNode.getNextSibling()) {
    if (domNode.getNodeType() === /* DOM_NODE_ELEMENT */ 1) {
      domNodes.push(domNode);
    }
  }
  // element 0 is the anchor and 1 is me

  // create a graph from the event-list
  var springGraph = new Graph(domNodes);

  // build the edges
  log('computing edge weights');
  var edgeWeights = {};
  // . . For each event
  for (var i = events.length; --i >= 0;) {
    var guests = events[i].getAttendees();
    // . . get the list of guests, and for each pair of guests
    for (var j = guests.length - 1; --j >= 0;) {
      var nodeIndex1 = contactSet[guests[j]];
      for (var k = guests.length; --k >= j;) {
        // TODO(mikesamuel): This key logic belongs in spring-graph.js
        // Maybe provide an incrWeight if we really need to avoid getWeight
        // followed by setWeight
        var nodeIndex2 = contactSet[guests[k]];
        var key;
        if (nodeIndex1 > nodeIndex2) {
          key = (nodeIndex2 << 16) | nodeIndex1;
        } else {
          key = (nodeIndex1 << 16) | nodeIndex2;
        }
        // . . add 1 to the edge between those guests
        edgeWeights[key] = (edgeWeights[key] || 0) + 1;
      }
    }
  }
  for (var indexPair in edgeWeights) {
    var nodeIndex1 = indexPair >> 16;
    var nodeIndex2 = indexPair & 0xffff;
    if (nodeIndex1 === nodeIndex2) { continue; }
    // normalize weights by taking into account the total number of times
    // each appears
    var count1 = edgeWeights[(nodeIndex1 << 16) | nodeIndex1],
        count2 = edgeWeights[(nodeIndex2 << 16) | nodeIndex2];
    var weight = edgeWeights[indexPair] * 2 / (count1 + count2);
    if (weight) {
      springGraph.setWeight(nodeIndex1, nodeIndex2, weight);
    }
  }

  // set up the graph
  springGraph.initLayout();

  // set up a timeout to update the layout periodically
  var scale = graphContainer.getOffsetWidth() / 6;
  var count = 0;
  var t = Date.now();
  var MAX_STEPS = 70, PERIOD = 100;
  function step() {
    var t1 = Date.now();
    //log('t1=' + t1 + ', t=' + t);
    var nSteps = ((t1 - t) / PERIOD) | 0;
    var done = springGraph.step(nSteps, scale, 3.0);
    t += nSteps * PERIOD;
    if (!done && ++count < MAX_STEPS) {
      setTimeout(step, PERIOD);
    }
  }
  step();

})();
