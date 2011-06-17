// Copyright (C) 2010 Google Inc.
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

var api = (function() {

  var data = (function() {
    var makeRandomData = function(n) {
      var result = [];
      for (var i = 0; i < n; i++) {
        result[i] = Math.random();
      }
      return result;
    };

    var makeColumn = function(name, units, data) {
      var min = 0, max = 0;
      if (data.length) {
        min = max = data[0];
        for (var i = 0; i < data.length; i++) {
          min = Math.min(min, data[i]);
          max = Math.max(max, data[i]);
        }
      }
      return {
        header: {
          name: name,
          units: units,
          min: min,
          max: max
        },
        values: data
      };
    };

    var columns = [];
    var numRows = 50;

    var makeRandomColumn = function(name, units) {
      columns.push(makeColumn(name, units, makeRandomData(numRows)));
    };

    var getNumRows = function() { return numRows; };

    var getNumCols = function() { return columns.length; };

    var getColHeader = function(j) {
      if (j < 0 || j >= columns.length) { return undefined; }
      return columns[j].header;
    };

    var get = function(i, j) {
      if (i < 0 || i >= numRows) { return undefined; }
      if (j < 0 || j >= columns.length) { return undefined; }
      return columns[j].values[i];
    };

    makeRandomColumn('Temp low',  'deg C');
    makeRandomColumn('Temp high', 'deg C');
    makeRandomColumn('Salinity',  '');

    return {
      getNumRows: getNumRows,
      getNumCols: getNumCols,
      getColHeader: getColHeader,
      makeRandomColumn: makeRandomColumn,
      get: get
    };
  })();

  var selection = undefined;
  var selectionListeners = [];

  // The selection getter function must ensure that it does not expose
  // mutable internal state to the guest. In this case, it returns a
  // defensive copy of the 'selection' array. Our taming copies arrays
  // across the taming boundary anyway, but we do not recommend that Caja
  // users rely on this for the moment.
  var getSelection = function() {
    return selection ? selection.slice(0) : undefined;
  };

  // The selection setter function is called by guest code and must defend
  // itself against bad input
  var setSelection = function(sel) {
    if (!sel) { return; }
    var boundedNumber = function(x, bound) {
      x = Number(x);
      if (x < 0) {
        return 0; 
      } else if (x >= bound) {
        return bound - 1;
      } else {
        return x;
      }
    };
    selection = [
      boundedNumber(sel[0], data.nRows),
      boundedNumber(sel[1], data.nCols)
    ];
    fireSelectionChanged();
  };

  // When the host calls a function provided by guest code, it must provide
  // a safe 'this' value to avoid exposing its internal state to the guest
  var fireSelectionChanged = function() {
    for (var i = 0; i < selectionListeners.length; i++) {
      try {
        selectionListeners[i].call({});
      } catch (e) {
        log('Selection listener threw: ' + e);
      }
    }
  };

  // Host code must ensure that selection listeners provided by guest code
  // will not cause host code to throw unexpectedly, since that may cause the
  // host to violate its invariants
  var addSelectionListener = function(l) {
    if (typeof l === 'function') { selectionListeners.push(l); }
  };

  return {
    getSelection: getSelection,
    setSelection: setSelection,
    addSelectionListener: addSelectionListener,
    data: data
  };
})();