(function () {
  var SVG_NS_URI = 'http://www.w3.org/2000/svg';
  var COLORS = ['blue', 'red', 'green', 'orange', 'yellow', 'magenta'];

  function createSvgNode(name, attribs) {
    var node = document.createElementNS(SVG_NS_URI, name);
    for (var k in attribs) {
      node.setAttributeNS(SVG_NS_URI, k, '' + attribs[k]);
    }
    return node;
  }

  function unrollSet(set) {
    var keys = [];
    for (var k in set) { keys.push(k); }
    keys.sort();
    return keys;
  }

  function seriesMin(series, opt_initial) {
    var value = opt_initial === undefined ? Infinity : opt_initial;
    for (var i = series.length; --i >= 0;) {
      if (series[i].value < value) { value = series[i].value; }
    }
    return value;
  }

  function seriesMax(series, opt_initial) {
    var value = opt_initial === undefined ? -Infinity : opt_initial;
    for (var i = series.length; --i >= 0;) {
      if (series[i].value > value) { value = series[i].value; }
    }
    return value;
  }

  /**
   * Produces a function that can generate charts over the given series.
   * @param {Object} series maps variables to Arrays of objects of the form
   *     <code>{ instant: x-axis-value, value: number }</code>.
   */
  function charter(series) {
    /**
     * @param {Array.<string>} variables a set of variable names.
     *     keys into series.
     * @param {Object} opt_config
     */
    return function(variables, opt_config) {
      var sameAxis = (opt_config && opt_config.sameAxis !== undefined)
          ? opt_config.sameAxis
          : variables.length > 2;
      var width = (opt_config && opt_config.width) || 500;
      var height = (opt_config && opt_config.height) || 250;
      var chartStyles = (opt_config && opt_config.chartStyles) || {};

      var chart = createSvgNode(
          'svg',
          { version: 1.1, width: width + 'px', height: height + 'px',
            viewBox: '0 0 ' + width + ' ' + height });
      chart.setAttribute('xmlns', SVG_NS_URI);
      chart.appendChild(createSvgNode(
          'rect',
          { x: 1, y: 1, width: width - 2, height: height - 2,
            stroke: 'blue', fill: 'none' }));

      var instants = {};
      for (var j = -1; ++j < variables.length;) {
        var oneSeries = series[variables[j]];
        for (var i = oneSeries.length; --i >= 0;) {
          instants[oneSeries[i].instant] = null;
        }
      }
      var xAxis = unrollSet(instants);

      var y0, y1;
      function chooseDomain(min, max) {
        var dy = max - min;
        if (dy < 0.001) {
          dy = (Math.abs(min) * .0625) || 0.1;
        }
        y0 = min - dy * 0.125;
        y1 = max + dy * 0.125;
      }

      if (sameAxis) {
        var min = undefined, max = undefined;
        for (var j = -1; ++j < variables.length;) {
          var oneSeries = series[variables[j]];
          min = seriesMin(oneSeries, min);
          max = seriesMax(oneSeries, max);
        }
        chooseDomain(min, max);
      }

      for (var j = -1; ++j < variables.length;) {
        var varName = variables[j];
        var oneSeries = series[varName];
        if (!sameAxis) {
          var min = seriesMin(oneSeries), max = seriesMax(oneSeries);
          chooseDomain(min, max);
        }

        var pathParts = [];
        var lastValue = min;
        for (var i = -1, k = 0; ++i < xAxis.length;) {
          var x = xAxis[i];
          var y = lastValue;
          if (k < oneSeries.length && oneSeries[k].instant === x) {
            y = lastValue = oneSeries[k++].value;
          }
          pathParts.push(
              (i ? 'L' : 'M')  // M is Move pen, L is Line to.
              + Math.round(width * i / (xAxis.length - 1))
              + ','
              + Math.round(height * (1 - ((y - y0) / (y1 - y0)))));
        }
        var color = chartStyles[varName] && chartStyles[varName].color;
        var strokeWidth = chartStyles[varName] && chartStyles[varName].width;;
        if (!color) {
          color = COLORS[j % COLORS.length];
          if (!chartStyles[varName]) { chartStyles[varName] = {}; }
          chartStyles[varName].color = color;
        }
        if (!strokeWidth) {
          // Make earlier strokes fatter so they show up under later strokes.
          strokeWidth = 2 + (variables.length - j)
          if (!chartStyles[varName]) { chartStyles[varName] = {}; }
          chartStyles[varName].width = strokeWidth;
        }
        chart.appendChild(createSvgNode(
            'path',
            { stroke: color,
              'stroke-width': strokeWidth,
              fill: 'none',
              d: pathParts.join(' ') }));
      }

      return chart;
    }
  };

  this.charter = charter;
})();
