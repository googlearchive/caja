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

    var makeSeries = function(n, step) {
      var result = [];
      for (var i = 0; i < n; i++) {
	result[i] = i * step;
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

    var numRows = 100;

    var columns = [
      makeColumn('Time',        'sec',    makeSeries(numRows, .25)),
      makeColumn('Density',     'kg/m^3', makeRandomData(numRows)),
      makeColumn('Temperature', 'degK',   makeRandomData(numRows)),
      makeColumn('Pressure',    'N/m^2',  makeRandomData(numRows))
    ];

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

    return {
      getNumRows: getNumRows,
      getNumCols: getNumCols,
      getColHeader: getColHeader,
      get: get,
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
      selectionListeners[i].call({});
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

var log = function(s) {
  document.getElementById('log').innerHTML += s + '\n';
};

var addGadgetDiv = function() {
  var td = document.createElement('td');
  td.style.verticalAlign = 'top';
  var outerDiv = document.createElement('div');
  document.getElementById('gadgets').appendChild(td);
  td.appendChild(outerDiv);
  return outerDiv;
};

var defaultUriPolicy = {
  rewrite: function(uri, mimeType) { return undefined; }
};

caja.configure({
  cajaServer: 'http://caja.appspot.com/',
  debug: true
}, function(frameGroup) {

  var tameApi = frameGroup.tame(api);
  
  function loadGadget(url) {
    frameGroup.makeES5Frame(addGadgetDiv(), defaultUriPolicy, function (frame) {
      var extraOuters = {
	api: tameApi,
	log: frameGroup.tame(function(s) {
          log(url + ': ' + String(s));
	})
      };
      frame.run(url, extraOuters, function (result) { 
	log('Gadget ' + url + ' loaded');
      });
    });
  }

  loadGadget('./table.html');
  loadGadget('./graph.html');
});
