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
 * A widget that models a spring graph as described at
 * http://en.wikipedia.org/wiki/Force-based_algorithms.
 *
 * <p>This model takes a graph whose nodes are represented as an array of
 * DOMElements.  It internally maintains a matrix of weights that can
 * can be set.
 *
 * <p>It assumes that the DOM nodes are absolutely positioned and have the same
 * reference parent, and it positions nodes by their rectangular center-point.
 *
 * @author mikesamuel@gmail.com
 */

/**
 * a non-directed graph where nodes correspond to DOM nodes, and edges
 * correspond to the strength of the connection -- stronger connections imply
 * the nodes should be closer spatially.
 * @param {Array.<DOMElement>} nodes
 */
function Graph(nodes) {
  /** @type {Array.<DOMElement>} */
  this.nodes_ = [];
  for (var i = nodes.length; --i >= 0;) {
    this.nodes_[i] = { domNode: nodes[i], dx: 0, dy: 0 };
  }
  /**
   * a simple, triangular dense matrix implementation whose diagonal is 0.
   * <p>
   * Layout the matrix to minimize space and number of accesses, so a 4x4 matrix
   * looks like <code>[(0, 1), (0, 2), (1, 2), (0, 3), (1, 3), (2, 3)]</code> or
   * in-reverse
   * <pre>
   * [ X X X X
   *   0 X X X
   *   1 2 X X
   *   3 4 5 X ]
   * </pre>
   * Assume i < j and i >= 0 and j < n.
   * Put element (0, j) at (j * (j - 1)) / 2 since (1, 2, 3, ..., n) sums to
   * 2n(n + 1).
   * Put (i, j) at (i + (j * (j - 1)) / 2).
   *
   * <p>
   * In an n*n matrix, the last element is (n - 2, n - 1) so it takes
   *   (n - 2 + ((n - 1) * (n - 2)) / 2) + 1 space, which is the same as
   *   ((n + 1) * (n - 2)) / 2 + 1.
   *
   * @type {Array.<number>}
   */
  this.edges_ = [];
  for (var i = ((nodes.length + 1) * (nodes.length - 2)) / 2 + 1; --i >= 0;) {
    this.edges_[i] = 0;
  }
}

/**
 * sets the weight of an edge.
 * @param {number} i a node index.
 * @param {number} j another node index.
 * @param {number} weight strength of the edge.
 */
Graph.prototype.setWeight = function (i, j, weight) {
  if (i !== j) {
    this.edges_[i < j ? i + ((j * (j - 1)) >> 1)
                      : j + ((i * (i - 1)) >> 1)] = weight;
  }
};

/**
 * returns the weight of the edge from node i to node j.
 * If there is no edge, returns 0.
 * @param {number} i a node index.
 * @param {number} j another node index.
 */
Graph.prototype.getWeight = function (i, j) {
  if (i === j) { return 0; }
  return this.edges_[i < j ? i + ((j * (j - 1)) >> 1)
                           : j + ((i * (i - 1)) >> 1)];

};

var COEFF_FRICTION = .632;

/**
 * computes new positions for the nodes, and moves them.
 * @param {number} nSteps number of steps to advance
 * @param {number} scale the length in pixels of an edge with weight 1.
 * @param {number} threshold for convergence.  Based on sum speed of all nodes.
 */
Graph.prototype.step = function (nSteps, scale, threshold) {
  if (!nSteps) { return false; }
  var edges = this.edges_;
  var nodes = this.nodes_;

  var nNodes = nodes.length;

  // apply friction so that things eventually stop moving
  for (var i = nNodes; --i >= 0;) {
    var graphNode = nodes[i];
    graphNode.dx *= COEFF_FRICTION;
    graphNode.dy *= COEFF_FRICTION;
  }

  // calculate centerpoints of nodes
  for (var i = nNodes; --i >= 0;) {
    var graphNode = nodes[i];
    var domNode = graphNode.domNode;
    graphNode.x = domNode.getOffsetLeft() + (domNode.getOffsetWidth() >>> 1);
    graphNode.y = domNode.getOffsetTop() + (domNode.getOffsetHeight() >>> 1);
  }

  var naturalSpringLength = scale;
  var kC = 30, k = .01;
  var sqrt = Math.sqrt;

  // update velocities
  var e = edges.length;  // iterator over sparse matrix
  for (var j = nNodes; --j >= 1;) {
    var nodeJ = nodes[j];
    var xj = nodeJ.x, yj = nodeJ.y;
    var ddx = 0, ddy = 0;
    for (var i = j; --i >= 0;) {
      var weight = edges[--e];
      var nodeI = nodes[i];

      // displacent
      var sx = (xj - nodeI.x), sy = (yj - nodeI.y);
      if (!sx) { sx = i % 2 ? 1 : -1; }
      if (!sy) { sy = j % 2 ? 1 : -1; }
      var sxSqr = sx * sx, sySqr = sy * sy;
      var sSqr = sxSqr + sySqr;
      var s = sqrt(sSqr);

      // force due to a spring is -k * |s - natural-length|
      // repulsion force is kC / s ** 2,
      var springForce = weight && (s - naturalSpringLength / weight) * k;
      var chargeForce = kC / sSqr;
      var force = springForce - chargeForce;

      // acceleration in each dimension
      var ax = 0, ay = 0;
      if (sxSqr) {
        if (sySqr) {
          ax = force * sqrt(sxSqr / sSqr) * (sx < 0 ? -1 : 1);
          ay = force * sqrt(sySqr / sSqr) * (sy < 0 ? -1 : 1);
        } else {
          ax = force * (sx < 0 ? -1 : 1);
        }
      } else {
        ay = force * (sy < 0 ? -1 : 1);
      }
//    log('s=' + s + ', force=' + force + ', ax=' + ax + ', ay=' + ay +
//        ', weight=' + weight);

      nodeI.dx += ax;
      nodeI.dy += ay;
      ddx += ax;
      ddy += ay;
    }
    nodeJ.dx -= ddx;
    nodeJ.dy -= ddy;
  }
  var totVelSqr = 0;
  for (var i = nNodes; --i >= 1;) {
    var node = nodes[i];
    var dx = node.dx, dy = node.dy;
    totVelSqr += dx * dx + dy * dy;
  }

  // apply nSteps worth of velocities
  for (var i = nNodes; --i >= 1;) {
    var graphNode = nodes[i];
    var domNode = graphNode.domNode;
    var width = domNode.getOffsetWidth(),
        height = domNode.getOffsetHeight();
    var newStyle = position(
      graphNode.x + graphNode.dx * nSteps - width / 2,
      graphNode.y + graphNode.dy * nSteps - height / 2,
      width,
      height);
//  log('i=' + i + ' : id=' + domNode.getId() + ', x=' + graphNode.x + ', y=' +
//      graphNode.y +
//      ', dx=' + graphNode.dx + ', dy=' + graphNode.dy +
//      '\n\tnewStyle=' + newStyle.toString().replace(/\n/g, ' ') +
//      '\n\toldStyle=' + domNode.getStyle().toString().replace(/\n/g, ' '));
    domNode.setStyle(newStyle);
  }

  log('totVelSqr=' + totVelSqr + ' for ' + nSteps + ' steps');
  // return true to indicate we've reached equilibrium
  return !(totVelSqr >= threshold);
};

/** arrange the nodes initially in a ring around the anchor. */
Graph.prototype.initLayout = function () {
  var nodes = this.nodes_;
  var nNodes = nodes.length;

  if (nNodes < 2) { return; }

  // assume node 0 is anchored and arrange other nodes around it.
  var anchor = nodes[0].domNode;
  var x0 = anchor.getOffsetLeft() + (anchor.getOffsetWidth() / 2),
      y0 = anchor.getOffsetTop() + (anchor.getOffsetHeight() / 2);
  var xExtent = x0 * .8, yExtent = y0 * .8;

  // arrange in an ellipse around anchor
  var angle = 2 * Math.PI / (nNodes - 1);
  for (var i = nNodes; --i >= 1;) {
    var graphNode = nodes[i];
    var domNode = graphNode.domNode;
    var theta = (i - 1) * angle;
    domNode.setStyle(
        position(
            x0 + Math.cos(theta) * xExtent,
            y0 - Math.sin(theta) * yExtent,
            domNode.getOffsetWidth(),
            domNode.getOffsetHeight()));
  }

};

var selectedNode = null;
function selectNode(domNode) {
  if (selectedNode) {
    selectedNode.setClass('node');
  }
  if (domNode) {
    domNode.setClass('node selected');
    setSelectedUser(domNode.getFirstChild().getInnerHTML());
  }
  selectedNode = domNode;
}
