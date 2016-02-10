// Copyright (C) 2012 Google Inc.
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
 * Policy factory for "google.visualization" API.
 *
 * @author ihab.awad@gmail.com
 * @requires caja, JSON, window
 */
caja.tamingGoogleLoader.addPolicyFactory('google.visualization',
    function(frame, utils) {
  "use strict";

  var v = {};

  // TODO(kpreid): this is a generic utility, move?
  function whitelisting(whitelist, postprocessor) {
    if (!postprocessor) {
      postprocessor = utils.identity;
    }
    function whitelistApplier(input) {
      if (!input) {
        return input;
      } else {
        var output = {};
        for (var prop in input) {
          if (Object.prototype.hasOwnProperty.call(whitelist, prop)) {
            output[prop] = whitelist[prop](input[prop]);
          }
        }
        return postprocessor(output);
      }
    }
    return whitelistApplier;
  }

  function arrayOf(f) {
    return function arraySanitizer(array) {
      return Array.prototype.map.call(array, f);
    };
  }

  // Like arrayOf, but allow any object or array with numeric indices
  function sparseArrayOf(f) {
    return function sparseArraySanitizer(sparseArray) {
      var output = {};
      for (var key in sparseArray) {
        if ('' + (+key) === key) {
          output[key] = f(sparseArray[key]);
        }
      }
      return output;
    };
  }

  // Like sparseArrayOf, but allow any properties whatsoever
  function objectOf(f) {
    return function objectSanitizer(obj) {
      var output = {};
      for (var key in obj) {
        output[key] = f(obj[key]);
      }
      return output;
    };
  }

  function oneOfValues(array) {
    array = Array.prototype.slice.call(array);
    return function enumSanitizer(value) {
      return array[array.indexOf(value)];
    };
  }

  // Helper for looking up properties in a whitelisting() function
  function sanitizeOneProperty(whitelistFunc, key, value, notFoundValue) {
    var obj = {};
    obj[key] = value;
    var safe = whitelistFunc(obj);
    return key in safe ? safe[key] : notFoundValue;
  }

  ////////////////////////////////////////////////////////////////////////
  // Sanitizing chart options, which may have the 'allowHtml' option we must
  // reject.

  function sanitizeDate(value) {
    return new Date(value);
  }

  // Docs say "HTML color string". This filter is protection against the value
  // being passed through into HTML or CSS text unchanged.
  function sanitizeColorString(value) {
    value = '' + value;
    return /^(\w+|#[0-9A-Fa-f]+)$/.test(value) ? value : '';
  }
  var sanitizeColorObject = whitelisting({
    fill: sanitizeColorString,
    stroke: sanitizeColorString,
    strokeOpacity: Number,
    strokeWidth: Number
  });
  function sanitizeColor(value) {
    if (typeof value === 'object') {
      return sanitizeColorObject(value);
    } else {
      return sanitizeColorString(value);
    }
  }

  // Sanitize values documented as being lengths/positions and can be strings
  // e.g. chartArea.left "Type: number or string / Default: auto".
  // Assumption is that letters and numbers can't express an attack.
  function sanitizeStringDimension(value) {
    if (typeof value === 'number') {
      return value;
    } else {
      value = '' + value;
      return /^\w+$/.test(value) ? value : '';
    }
  }

  // Arbitrary values unsafe -- can be used to invoke global functions.
  var sanitizeChartType = oneOfValues([
    'AnnotatedTimeLine',
    'AnnotationChart',
    'AreaChart',
    'BarChart',
    'BubbleChart',
    'Calendar',
    'CandlestickChart',
    'ColumnChart',
    'ComboChart',
    'Gantt',
    'Gauge',
    'GeoChart',
    'Histogram',
    'IntensityMap',
    'LineChart',
    'Map',
    'MotionChart',
    'OrgChart',
    'PieChart',
    'Sankey',
    'ScatterChart',
    'SteppedAreaChart',
    'Table',
    'Timeline',
    'TreeMap',
    'WordTree'
  ]);

  var sanitizeChartGridlines = whitelisting({
    color: sanitizeColorString,
    count: Number
    // TODO(kpreid): allow 'units'
  });

  var sanitizeTextStyle = whitelisting({
    color: sanitizeColorString,
    fontName: String,
    fontSize: Number,
    bold: Boolean,
    italic: Boolean
  });

  var sanitizeChartAxis = whitelisting({  // BarChart, AreaChart, ...
    allowContainerBoundaryTextCutoff: Boolean,
    baseline: Number,
    baselineColor: sanitizeColorString,
    direction: Number,
    format: String,
    gridlines: sanitizeChartGridlines,
    maxAlternation: Number,
    maxTextLines: Number,
    maxValue: Number,
    minorGridlines: sanitizeChartGridlines,
    minTextSpacing: Number,
    minValue: Number,
    logScale: Boolean,
    scaleType: String,
    showTextEvery: Number,
    slantedText: Boolean,
    slantedTextAngle: Number,
    textPosition: String,
    textStyle: sanitizeTextStyle,
    ticks: arrayOf(function(v) { return sanitizeCellObject(v); }),
    title: String,
    titleTextStyle: sanitizeTextStyle,
    viewWindowMode: String,
    viewWindow: whitelisting({ max: Number, min: Number })
  });

  var sanitizeChartAnnotations = whitelisting({
    alwaysOutside: Boolean,
    boxStyle: whitelisting({
      rx: Number,
      ry: Number,
      stroke: sanitizeColorString,
      strokeWidth: Number,
      gradient: whitelisting({
        color1: sanitizeColorString,
        color2: sanitizeColorString,
        useObjectBoundingBoxUnits: Boolean,
        x1: String,
        x2: String,
        y1: String,
        y2: String
      })
    }),
    datum: whitelisting({
      stem: whitelisting({ color: sanitizeColorString, length: Number }),
      style: String
    }),
    domain: whitelisting({
      stem: whitelisting({ color: sanitizeColorString, length: Number }),
      style: String
    }),
    highContrast: Boolean,
    stem: whitelisting({ color: sanitizeColorString, length: Number }),
    style: String,
    textStyle: sanitizeTextStyle
  });

  var _sanitizeCrosshair = whitelisting({
    color: sanitizeColorString,
    focused: sanitizeCrosshair,
    opacity: Number,
    orientation: String,
    selected: sanitizeCrosshair,
    trigger: String
  });
  function sanitizeCrosshair(value) {  // for self-reference
    return _sanitizeCrosshair(value);
  }

  // TODO(kpreid): Didn't find reference documentation; this is constructed
  // from examples.
  var sanitizeIntervalStyle = whitelisting({
    color: sanitizeColorString,
    curveType: String,
    barWidth: Number,
    fillOpacity: Number,
    lineWidth: Number,
    pointSize: Number,
    style: String
  });

  var sanitizeDrawOpts = whitelisting({
    aggregationTarget: String,
    allowCollapse: Boolean,  // OrgChart
    allowHtml: function () { return false; },
    allValuesSuffix: String,  // AnnotationChart
    alternatingRowStyle: Boolean,  // Table
    animation: whitelisting({
      duration: Number,
      easing: String,
      startup: Boolean
    }),
    annotations: sanitizeChartAnnotations,
    annotationsWidth: Number,  // AnnotationChart
    areaOpacity: Number, // AreaChart
    avoidOverlappingGridLines: Boolean,  // Timeline
    axisTitlesPosition: String,
    backgroundColor: sanitizeColor,
    bar: whitelisting({  // BarChart, Histogram
      groupWidth: String
    }),
    bars: String,    // BarChart
    calendar: whitelisting({  // Calendar chart
      cellColor: sanitizeColorObject,
      cellSize: Number,
      dayOfWeekLabel: sanitizeTextStyle,
      dayOfWeekRightSpace: Number,
      daysOfWeek: String,
      focusedCellColor: sanitizeColorObject,
      monthLabel: sanitizeTextStyle,
      monthOutlineColor: sanitizeColorObject,
      underMonthSpace: Number,
      unusedMonthOutlineColor: sanitizeColorObject
    }),
    candlestick: whitelisting({
      hollowIsRising: Boolean,
      fallingColor: sanitizeColorObject,
      risingColor: sanitizeColorObject
    }),
    chart: whitelisting({
      title: String,
      subtitle: String
    }),
    chartArea: whitelisting({
      backgroundColor: sanitizeColor,
      left: sanitizeStringDimension,
      top: sanitizeStringDimension,
      width: sanitizeStringDimension,
      height: sanitizeStringDimension,
      subtitle: String,
      title: String
    }),
    color: sanitizeColorString,  // OrgChart
    colorAxis: whitelisting({  // Calendar, GeoChart
      colors: arrayOf(sanitizeColorString),
      maxValue: Number,
      minValue: Number,
      values: arrayOf(Number)
    }),
    colors: arrayOf(sanitizeColorString),
    crosshair: sanitizeCrosshair,  // AreaChart
    // cssClassNames prohibited (Table)
    curveType: String,  // LineChart
    dataOpacity: Number,
    datalessRegionColor: sanitizeColorString,
    dateFormat: String,
    defaultColor: sanitizeColorString,
    diff: whitelisting({
      innerCircle: whitelisting({
        borderFactor: Number,
        radiusFactor: Number
      }),
      newData: whitelisting({
        opacity: Number,
        widthFactor: Number
      }),
      oldData: whitelisting({
        inCenter: Boolean,
        opacity: Number
      })
    }),
    displayAnnotations: Boolean,  // AnnotatedTimeLine, AnnotationChart
    displayAnnotationsFilter: Boolean,
    displayDateBarSeparator: Boolean,
    displayExactValues: Boolean,
    displayLegendDots: Boolean,
    displayLegendValues: Boolean,
    displayMode: String,  // GeoChart
    displayRangeSelector: Boolean,
    displayZoomButtons: Boolean,
    domain: String,  // GeoChart
    enableInteractivity: Boolean,
    enableRegionInteractivity: Boolean,
    enableScrollWheel: Boolean,
    explorer: whitelisting({
      actions: arrayOf(String),
      axis: String,
      keepInBounds: Boolean,
      maxZoomIn: Number,
      maxZoomOut: Number,
      zoomDelta: Number
    }),
    fill: Number,  // AnnotationChart
    firstRowNumber: Number,  // Table
    focusTarget: String,
    fontColor: sanitizeColorString,  // TreeMap
    fontFamily: String,  // TreeMap
    fontName: String,
    fontSize: Number,
    forceIFrame: Boolean,
    frozenColumns: Number,  // Table
    gantt: whitelisting({
      arrow: whitelisting({
        angle: Number,
        color: sanitizeColorString,
        length: Number,
        radius: Number,
        spaceAfter: Number,
        width: Number
      }),
      barCornerRadius: Number,
      barHeight: Number,
      criticalPathEnabled: Boolean,
      criticalPathStyle: sanitizeColorObject,
      defaultStartDate: sanitizeDate,
      innerGridHorizLine: sanitizeColorObject,
      innerGridTrack: sanitizeColorObject,
      innerGridDarkTrack: sanitizeColorObject,
      labelMaxWidth: Number,
      labelStyle: sanitizeTextStyle,
      percentEnabled: Boolean,
      percentStyle: sanitizeColorObject,
      shadowEnabled: Boolean,
      shadowColor: sanitizeColorString,
      shadowOffset: Number,
      trackHeight: Number
    }),
    greenColor: sanitizeColorString,  // Gauge
    greenFrom: Number,  // Gauge
    greenTo: Number,  // Gauge
    hAxes: arrayOf(sanitizeChartAxis),
    hAxis: sanitizeChartAxis,
    headerColor: sanitizeColorString,  // TreeMap
    headerHeight: Number,  // TreeMap
    headerHighlightColor: sanitizeColorString,  // TreeMap
    highlightOnMouseOver: Boolean,  // TreeMap
    height: Number,
    hintOpacity: Number,  // TreeMap
    histogram: whitelisting({
      bucketSize: Number,
      hideBucketItems: Boolean,
      lastBucketPercentile: Number
    }),
    // icons (Map) option prohibited due to unconstrained URL fetching.
    //   TODO(kpreid): use frame.rewriteUri
    interpolateNulls: Boolean,
    interval: objectOf(sanitizeIntervalStyle),
    intervals: sanitizeIntervalStyle,
    is3D: Boolean,  // PieChart
    isStacked: utils.copyJson,
    keepAspectRatio: Boolean,  // GeoChart
    legend: (function() {
      var legendObjectWhitelist = whitelisting({  // BarChart, AreaChart
        alignment: String,
        maxLines: Number,
        numberFormat: String,
        position: String,
        textStyle: sanitizeTextStyle
      });
      return function(value) {
        if (typeof value === 'string') {
          // Not documented, but legend can take on string values apparently
          // corresponding to legend.position.
          return value;
        } else {
          return legendObjectWhitelist(value);
        }
      };
    }()),
    legendPosition: String,  // AnnotationChart
    lineColor: sanitizeColorString,
    lineDashStyle: arrayOf(Number),
    lineWidth: Number,
    magnifyingGlass: whitelisting({  // GeoChart
      enable: Boolean,
      zoomFactor: Number
    }),
    majorTicks: arrayOf(String),  // Gauge
    maps: objectOf(whitelisting({
      name: String,
      styles: utils.copyJson
    })),
    mapType: String,
    mapTypeIds: arrayOf(String),
    markerOpacity: Number,  // GeoChart
    max: Number,
    maxColor: sanitizeColorString,  // TreeMap
    maxColorValue: Number,  // TreeMap
    maxDepth: Number,  // TreeMap
    maxFontSize: Number,  // WordTree
    maxHighlightColor: sanitizeColorString,  // TreeMap
    maxPostDepth: Number,  // TreeMap
    midColor: sanitizeColorString,  // TreeMap
    midHighlightColor: sanitizeColorString,  // TreeMap
    min: Number,
    minColor: sanitizeColorString,  // TreeMap
    minColorValue: Number,  // TreeMap
    minHighlightColor: sanitizeColorString,  // TreeMap
    minorTicks: Number,  // Gauge
    // nodeClass: prohibited because it is a CSS class name
    noColor: sanitizeColorString,  // TreeMap
    noHighlightColor: sanitizeColorString,  // TreeMap
    noDataPattern: whitelisting({
      backgroundColor: sanitizeColorString,
      color: sanitizeColorString
    }),
    numberFormats: utils.copyJson,  // AnnotationChart
    region: String,  // GeoChart
    resolution: String,  // GeoChart
    reverseCategories: Boolean,
    orientation: String,
    page: String,  // Table
    pageSize: Number,  // Table
    pagingButtons: String,  // Table
    pieHole: Number,
    pieSliceBorderColor: sanitizeColorString,
    pieSliceText: String,
    pieSliceTextStyle: sanitizeTextStyle,
    pieStartAngle: Number,
    pieResidueSliceColor: sanitizeColorString,
    pieResidueSliceLabel: String,
    pointShape: String,
    pointSize: Number,
    pointsVisible: Boolean,
    redColor: sanitizeColorString,  // Gauge
    redFrom: Number,  // Gauge
    redTo: Number,  // Gauge
    reverseCategories: Boolean,  // AreaChart
    rtlTable: Boolean,  // Table
    sankey: whitelisting({
      iterations: Number,
      link: whitelisting({
        color: sanitizeColor,
        colors: arrayOf(sanitizeColor),
        colorMode: String
      }),
      node: whitelisting({
        colorMode: String,
        colors: arrayOf(sanitizeColor),
        interactivity: Boolean,
        label: sanitizeTextStyle,
        labelPadding: Number,
        nodePadding: Number,
        width: Number
      })
    }),
    scaleColumns: arrayOf(Number),  // AnnotationChart
    scaleFormat: String,
    scaleType: String,
    scrollLeftStartPosition: Number,  // Table
    // selectedNodeClass: prohibited because it is a CSS class name
    selectionColor: sanitizeColorObject,  // OrgChart
    selectionMode: String,  // AreaChart
    series: sparseArrayOf(whitelisting({  // BarChart, AreaChart, Histogram
      annotations: sanitizeChartAnnotations,
      areaOpacity: Number,
      color: sanitizeColorString,
      curveType: String,  // ComboChart
      fallingColor: sanitizeColorObject,  // CandlestickChart
      labelInLegend: String,
      lineDashStyle: arrayOf(Number),
      lineWidth: Number,
      pointShape: String,
      pointSize: Number,
      pointsVisible: Boolean,
      risingColor: sanitizeColorObject,  // CandlestickChart
      targetAxisIndex: Number,
      type: String,  // ComboChart
      visibleInLegend: Boolean
    })),
    seriesType: String,  // ComboChart
    showLine: Boolean,  // Map
    showRowNumber: Boolean,  // Table
    showScale: Boolean,  // TreeMap
    showTip: Boolean,  // Map
    showTooltips: Boolean,  // TreeMap
    size: String,  // OrgChart
    sizeAxis: whitelisting({  // GeoChart
      maxSize: Number,
      maxValue: Number,
      minSize: Number,
      minValue: Number
    }),
    slices: sparseArrayOf(whitelisting({  // PieChart
      color: sanitizeColorString,
      offset: Number,
      textStyle: sanitizeTextStyle
    })),
    sliceVisibilityThreshold: Number,  // PieChart
    sort: String,  // Table
    sortAscending: Boolean,  // Table
    sortColumn: Number,  // Table
    startPage: Number,  // Table
    table: whitelisting({  // AnnotationChart
      sortAscending: Boolean,
      sortColumn: Number
    }),
    textStyle: sanitizeTextStyle,  // TreeMap
    theme: String,
    thickness: Number,  // AnnotationChart
    timeline: whitelisting({
      barLabelStyle: sanitizeTextStyle,
      colorByRowLabel: Boolean,
      groupByRowLabel: Boolean,
      rowLabelStyle: sanitizeTextStyle,
      showBarLabels: Boolean,
      showRowLabels: Boolean,
      singleColor: sanitizeColorString
    }),
    title: String,
    titlePosition: String,
    titleTextStyle: sanitizeTextStyle,
    tooltip: whitelisting({
      ignoreBounds: function() { return false; },
      isHtml: function() { return false; },
      showColorCode: Boolean,
      textStyle: sanitizeTextStyle,
      trigger: String
    }),
    trendlines: sparseArrayOf(whitelisting({
      color: sanitizeColorString,
      degree: Number,
      labelInLegend: String,
      lineWidth: Number,
      opacity: Number,
      pointSize: Number,
      pointsVisible: Boolean,
      showR2: Boolean,
      type: String,
      visibleInLegend: Boolean
    })),
    useMapTypeControl: Boolean,  // Map
    useWeightedAverageForAggregation: Boolean,  // TreeMap
    vAxes: sparseArrayOf(sanitizeChartAxis),
    vAxis: sanitizeChartAxis,  // BarChart
    width: Number,
    wordtree: whitelisting({
      format: String,
      sentenceRegex: String,
      type: String,
      word: String,
      wordRegex: String
    }),
    yellowColor: sanitizeColorString,  // Gauge
    yellowFrom: Number,  // Gauge
    yellowTo: Number,  // Gauge
    zoomEndTime: sanitizeDate,  // AnnotationChart
    zoomLevel: Number,  // Map
    zoomStartTime: sanitizeDate  // AnnotationChart
  });

  var sanitizeControlOpts = whitelisting({
    caseSensitive: Boolean,
    filterColumnIndex: Number,
    filterColumnLabel: String,
    matchType: String,
    maxValue: Number,
    minValue: Number,
    values: utils.copyJson,
    useFormattedValue: Boolean,
    ui: whitelisting({
      allowNone: Boolean,
      allowMultiple: Boolean,
      allowTyping: Boolean,
      blockIncrement: Number,
      caption: String,
      chartType: sanitizeChartType,
      chartOptions: sanitizeDrawOpts,
      // TODO(kpreid): allow chartView with only sanitized literals
      // cssClass not permitted; could be permitted with transform
      format: utils.copyJson,
      label: String,
      labelSeparator: String,
      labelStacking: String,
      minRangeSize: Number,
      orientation: String,
      realtimeTrigger: Boolean,
      showRangeValues: Boolean,
      snapToData: Boolean,
      sortValues: Boolean,
      selectedValuesLayout: String,
      step: String,
      ticks: Number,
      unitIncrement: String
    })
  });

  function sanitizeDrawOpt(f, self, args) {
    return [args[0], sanitizeOneProperty(
      sanitizeDrawOpts, args[0], args[1], null)];
  }

  function sanitizeControlOpt(f, self, args) {
    return [args[0], sanitizeOneProperty(
        sanitizeControlOpts, args[0], args[1], null)];
  }


  ////////////////////////////////////////////////////////////////////////
  // (Literal) Data Table sanitization

  var sanitizeTableProperties = whitelisting({
    // TODO(kpreid): allow safe props
  });

  var sanitizeColumnProperties = whitelisting({
    // TODO(kpreid): allow safe props
  });

  var sanitizeRowProperties = whitelisting({
    // TODO(kpreid): allow safe props
  });

  var sanitizeCellProperties = whitelisting({
    // TODO(kpreid): allow safe props
  });

  var sanitizeColumnDescriptionObject = whitelisting({
    type: oneOfValues([
      'boolean', 'number', 'string', 'date', 'datetime', 'timeofday',
    ]),
    label: String,
    id: String,
    role: oneOfValues([
      'annotation', 'annotationText', 'certainty', 'data', 'domain',
      'emphasis', 'interval', 'scope',
      // 'style', hazardous but not known unsafe
      'tooltip'  // safe as long as we don't allow HTML tooltips
    ]),
    pattern: String,
    p: sanitizeColumnProperties
  });
  function sanitizeColumnDescription(value) {
    switch (typeof value) {
      case 'object':
        return sanitizeColumnDescriptionObject(value);
      default:
        // Strings are allowed.
        // Miscellaneous non-strings are stringified as a safe default.
        return '' + value;
    }
  }

  var sanitizeCellObject = whitelisting({
    v: utils.identity,
    f: String,  // We separately reject the allowHtml option, so this is safe.
    p: sanitizeCellProperties
  });
  function sanitizeCellValue(value) {
    switch (typeof value) {
      case 'object':
        return sanitizeCellObject(value);
      case 'boolean':
      case 'number':
      case 'string':
        return value;
      default:
        return '' + value;
    }
  }

  var sanitizeRowArray = arrayOf(sanitizeCellValue);
  var sanitizeRowObject = whitelisting({
    c: sanitizeRowArray,
    p: sanitizeRowProperties
  });

  // A number or an array of arrays of cell values.
  function sanitizeInsertedRows(value) {
    if (typeof value === 'number') {
      return +value;
    } else {
      return arrayOf(sanitizeRowArray)(value);
    }
  }

  var sanitizeLiteralDataTable = whitelisting({
    cols: arrayOf(sanitizeColumnDescription),
    rows: arrayOf(sanitizeRowObject)
  });

  function sanitizeDataTable(value) {
    if (value instanceof window.google.visualization.DataTable) {
      return value;
    } else {
      return sanitizeLiteralDataTable(value);
    }
  }

  function sanitizeArrayData(value) {
    var length = value.length;
    var output = [];
    output[0] = arrayOf(sanitizeColumnDescription)(value[0]);
    for (var i = 1; i < length; i++) {
      output[i] = sanitizeRowArray(value[i]);
    }
    return output;
  }

  function sanitizeDataTableOrArray(value) {
    if (value.length) {  // TODO(kpreid): Better frame-independent test
      return sanitizeArrayData(value);
    } else {
      return sanitizeDataTable(value);
    }
  }

  function sanitizeSetTableProperty(f, self, args) {
    return [args[0],
        sanitizeOneProperty(sanitizeTableProperties, args[0], args[1], null)];
  }

  function sanitizeSetRowProperty(f, self, args) {
    return [args[0], args[1],
        sanitizeOneProperty(sanitizeRowProperties, args[1], args[2], null)];
  }

  function sanitizeSetColumnProperty(f, self, args) {
    return [args[0], args[1],
      sanitizeOneProperty(sanitizeColumnProperties, args[1], args[2], null)];
  }

  function sanitizeSetProperty(f, self, args) {
    return [args[0], args[1], args[2],
        sanitizeOneProperty(sanitizeCellProperties, args[2], args[3], null)];
  }


  ////////////////////////////////////////////////////////////////////////
  // Filter for ChartWrapper and ControlWrapper, and support for the pattern
  // of:
  //   - The ctor accepts a JSON structure with a field 'containerId'
  //   - The instance supports get/setContainerId()
  //   - The 'containerId' is the DOM ID of the container DOM element
  // We need to map IDs back and forth because we sneakily embed an opaque
  // node inside the "container" supplied by the guest, and put the actual
  // visualization inside *that*.

  var chartSpecWhitelist = whitelisting({
    // containerId is handled specially.
    options: sanitizeDrawOpts,
    chartType: sanitizeChartType,
    dataTable: sanitizeDataTableOrArray,
    dataSourceUrl: rewriteDataSourceUrl,
    query: String,
    refreshInterval: Number,
    view: utils.copyJson
  });

  // Arbitrary values unsafe -- can be used to invoke global functions.
  var sanitizeControlType = oneOfValues([
    'CategoryFilter',
    'ChartRangeFilter',
    'DateRangeFilter',
    'NumberRangeFilter',
    'StringFilter'
  ]);

  var controlSpecWhitelist = whitelisting({
    // containerId is handled specially.
    options: sanitizeControlOpts,
    controlType: sanitizeControlType,
    state: utils.copyJson
  });

  function wrapperCtorFilter(specWhitelist) {
    return function(f, self, args) {
      if (args.length < 1) { return []; }
      var specIn = args[0];
      if (typeof specIn === 'string') { specIn = JSON.parse(specIn); }
      var specOut = specWhitelist(specIn);
      var containerId = specIn.containerId;
      if (containerId) {
        containerId = '' + containerId;
        specOut.containerId = utils.opaqueNodeById(specIn.containerId);
        self.tameContainerId___ = containerId;
      }
      return [specOut];
    };
  }

  function wrapperStaticFilter(specWhitelist) {
    return function(f, self, args) {
      var specIn = args[0] || {};
      if (typeof specIn === 'string') { specIn = JSON.parse(specIn); }
      var specOut = specWhitelist(specIn);
      var containerId = specIn.containerId;
      if (containerId) {
        containerId = '' + containerId;
        specOut.containerId = utils.opaqueNodeById(containerId);
      }
      return [specOut];
    };
  }

  function containerIdAfterGet(f, self, result) {
    return self.tameContainerId___;
  }

  function containerIdBeforeSet(f, self, args) {
    self.tameContainerId___ = args[0];
    return [utils.opaqueNodeById(args[0])];
  }

  ////////////////////////////////////////////////////////////////////////
  // Data Source URL rewriting

  function rewriteDataSourceUrl(url) {
    return frame.rewriteUri(
      url,
      'application/javascript',
      { 'TYPE': 'GVIZ_DATA_SOURCE_URL' });
  }

  ////////////////////////////////////////////////////////////////////////
  // DataTable

  v.DataTable = function(opt_data, opt_version) {};
  v.DataTable.__super__ = Object;
  v.DataTable.__before__ = [ utils.mapArgs(sanitizeLiteralDataTable, utils.identity) ];
  v.DataTable.prototype.getNumberOfRows = function() {};
  v.DataTable.prototype.getNumberOfColumns = function() {};
  v.DataTable.prototype.clone = function() {};
  v.DataTable.prototype.getColumnId = function(columnIndex) {};
  v.DataTable.prototype.getColumnIndex = function(columnId) {};
  v.DataTable.prototype.getColumnLabel = function(columnIndex) {};
  v.DataTable.prototype.getColumnPattern = function(columnIndex) {};
  v.DataTable.prototype.getColumnRole = function(columnIndex) {};
  v.DataTable.prototype.getColumnType = function(columnIndex) {};
  v.DataTable.prototype.getValue = function(rowIndex, columnIndex) {};
  v.DataTable.prototype.getFormattedValue = function(rowIndex, columnIndex) {};
  v.DataTable.prototype.getProperty = function(rowIndex, columnIndex, property) {};
  v.DataTable.prototype.getProperties = function(rowIndex, columnIndex) {};
  v.DataTable.prototype.getTableProperties = function() {};
  v.DataTable.prototype.getTableProperty = function(property) {};
  v.DataTable.prototype.setTableProperties = function(properties) {};
  v.DataTable.prototype.setTableProperties.__before__ = [ utils.mapArgs(sanitizeTableProperties) ];
  v.DataTable.prototype.setTableProperty = function(property, value) {};
  v.DataTable.prototype.setTableProperty.__before__ = [ sanitizeSetTableProperty ];
  v.DataTable.prototype.setValue = function(rowIndex, columnIndex, value) {};
  v.DataTable.prototype.setFormattedValue = function(rowIndex, columnIndex, formattedValue) {};
  v.DataTable.prototype.setProperties = function(rowIndex, columnIndex, properties) {};
  v.DataTable.prototype.setProperties.__before__ =
    [ utils.mapArgs(utils.identity, utils.identity, sanitizeCellProperties) ];
  v.DataTable.prototype.setProperty = function(rowIndex, columnIndex, property, value) {};
  v.DataTable.prototype.setProperty.__before__ = [ sanitizeSetProperty ];
  v.DataTable.prototype.setCell = function(rowIndex, columnIndex, opt_value, opt_formattedValue, opt_properties) {};
  v.DataTable.prototype.setRowProperties = function(rowIndex, properties) {};
  v.DataTable.prototype.setRowProperties.__before__ = [ utils.mapArgs(utils.identity, sanitizeRowProperties) ];
  v.DataTable.prototype.setRowProperty = function(rowIndex, property, value) {};
  v.DataTable.prototype.setRowProperty.__before__ = [ sanitizeSetRowProperty ];
  v.DataTable.prototype.getRowProperty = function(rowIndex, property) {};
  v.DataTable.prototype.getRowProperties = function(rowIndex) {};
  v.DataTable.prototype.setColumnLabel = function(columnIndex, newLabel) {};
  v.DataTable.prototype.setColumnProperties = function(columnIndex, properties) {};
  v.DataTable.prototype.setColumnProperties.__before__ = [ utils.mapArgs(utils.identity, sanitizeColumnProperties) ];
  v.DataTable.prototype.setColumnProperty = function(columnIndex, property, value) {};
  v.DataTable.prototype.setColumnProperty.__before__ = [ sanitizeSetColumnProperty ];
  v.DataTable.prototype.getColumnProperty = function(columnIndex, property) {};
  v.DataTable.prototype.getColumnProperties = function(columnIndex) {};
  v.DataTable.prototype.insertColumn = function(atColIndex, type, opt_label, opt_id) {};
  v.DataTable.prototype.addColumn = function(type, opt_label, opt_id) {};
  v.DataTable.prototype.addColumn.__before__ = [
    function(f, self, args) {
      if (typeof args[0] === 'object') {
        return [ sanitizeColumnDescription(args[0]) ];
      } else {
        return [ '' + args[0], '' + (args[1] || ""), '' + (args[2] || '') ];
      }
    }
  ];
  v.DataTable.prototype.insertRows = function(atRowIndex, numOrArray) {};
  v.DataTable.prototype.insertRows.__before__ = [ utils.mapArgs(utils.identity, sanitizeInsertedRows) ];
  v.DataTable.prototype.addRows = function(numOrArray) {};
  v.DataTable.prototype.addRows.__before__ = [ utils.mapArgs(sanitizeInsertedRows) ];
  v.DataTable.prototype.addRow = function(opt_cellArray) {};
  v.DataTable.prototype.addRow.__before__ = [ utils.mapArgs(arrayOf(sanitizeCellValue)) ];
  v.DataTable.prototype.getColumnRange = function(columnIndex) {};
  v.DataTable.prototype.getSortedRows = function(sortColumns) {};
  v.DataTable.prototype.sort = function(sortColumns) {};
  v.DataTable.prototype.getDistinctValues = function(column) {};
  v.DataTable.prototype.getFilteredRows = function(columnFilters) {};
  v.DataTable.prototype.removeRows = function(fromRowIndex, numRows) {};
  v.DataTable.prototype.removeRow = function(rowIndex) {};
  v.DataTable.prototype.removeColumns = function(fromColIndex, numCols) {};
  v.DataTable.prototype.removeColumn = function(colIndex) {};
  v.DataTable.prototype.toJSON = function() {};


  ////////////////////////////////////////////////////////////////////////
  // DataView

  v.DataView = function(dataTable) {};
  v.DataView.__super__ = Object;
  v.DataView.fromJSON = function(dataTable, viewJSON) {};
  v.DataView.fromJSON.__before__ = [ utils.mapArgs(utils.identity, utils.copyJson) ];
  v.DataView.prototype.setColumns = function(colIndices) {};
  v.DataView.prototype.setRows = function(arg0, opt_arg1) {};
  v.DataView.prototype.getViewColumns = function() {};
  v.DataView.prototype.getViewRows = function() {};
  v.DataView.prototype.hideColumns = function(colIndices) {};
  v.DataView.prototype.hideRows = function(arg0, opt_arg1) {};
  v.DataView.prototype.getViewColumnIndex = function(tableColumnIndex) {};
  v.DataView.prototype.getViewRowIndex = function(tableRowIndex) {};
  v.DataView.prototype.getTableColumnIndex = function(viewColumnIndex) {};
  v.DataView.prototype.getUnderlyingTableColumnIndex = function(viewColumnIndex) {};
  v.DataView.prototype.getTableRowIndex = function(viewRowIndex) {};
  v.DataView.prototype.getUnderlyingTableRowIndex = function(viewRowIndex) {};
  v.DataView.prototype.getNumberOfRows = function() {};
  v.DataView.prototype.getNumberOfColumns = function() {};
  v.DataView.prototype.getColumnId = function(columnIndex) {};
  v.DataView.prototype.getColumnIndex = function(columnId) {};
  v.DataView.prototype.getColumnLabel = function(columnIndex) {};
  v.DataView.prototype.getColumnPattern = function(columnIndex) {};
  v.DataView.prototype.getColumnRole = function(columnIndex) {};
  v.DataView.prototype.getColumnType = function(columnIndex) {};
  v.DataView.prototype.getValue = function(rowIndex, columnIndex) {};
  v.DataView.prototype.getFormattedValue = function(rowIndex, columnIndex) {};
  v.DataView.prototype.getProperty = function(rowIndex, columnIndex, property) {};
  v.DataView.prototype.getColumnProperty = function(columnIndex, property) {};
  v.DataView.prototype.getColumnProperties = function(columnIndex) {};
  v.DataView.prototype.getTableProperty = function(property) {};
  v.DataView.prototype.getTableProperties = function() {};
  v.DataView.prototype.getRowProperty = function(rowIndex, property) {};
  v.DataView.prototype.getRowProperties = function(rowIndex) {};
  v.DataView.prototype.getColumnRange = function(columnIndex) {};
  v.DataView.prototype.getDistinctValues = function(columnIndex) {};
  v.DataView.prototype.getSortedRows = function(sortColumns) {};
  v.DataView.prototype.getFilteredRows = function(columnFilters) {};
  v.DataView.prototype.toDataTable = function() {};
  v.DataView.prototype.toJSON = function() {};


  ////////////////////////////////////////////////////////////////////////
  // ChartWrapper

  v.ChartWrapper = function(opt_specification) {};
  v.ChartWrapper.__super__ = Object;
  v.ChartWrapper.__before__ = [ wrapperCtorFilter(chartSpecWhitelist) ];
  v.ChartWrapper.prototype.draw = function(opt_container) {};
  v.ChartWrapper.prototype.draw.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.ChartWrapper.prototype.getDataSourceUrl = function() {};
  v.ChartWrapper.prototype.getDataTable = function() {};
  v.ChartWrapper.prototype.getChart = function() {};
  v.ChartWrapper.prototype.getChartName = function() {};
  v.ChartWrapper.prototype.getChartType = function() {};
  v.ChartWrapper.prototype.getContainerId = function() {};
  v.ChartWrapper.prototype.getContainerId.__after__ = [ containerIdAfterGet ];
  v.ChartWrapper.prototype.getQuery = function() {};
  v.ChartWrapper.prototype.getRefreshInterval = function() {};
  v.ChartWrapper.prototype.getView = function() {};
  v.ChartWrapper.prototype.getOption = function(key, opt_default) {};
  v.ChartWrapper.prototype.getOptions = function() {};
  v.ChartWrapper.prototype.setDataSourceUrl = function(dataSourceUrl) {};
  v.ChartWrapper.prototype.setDataSourceUrl.__before__ = [
      utils.mapArgs(rewriteDataSourceUrl)
  ];
  v.ChartWrapper.prototype.setDataTable = function(dataTable) {};
  v.ChartWrapper.prototype.setChartName = function(chartName) {};
  v.ChartWrapper.prototype.setChartType = function(chartType) {};
  v.ChartWrapper.prototype.setChartType.__before__ = [ utils.mapArgs(sanitizeChartType) ];
  v.ChartWrapper.prototype.setContainerId = function(containerId) {};
  v.ChartWrapper.prototype.setContainerId.__before__ = [ containerIdBeforeSet ];
  v.ChartWrapper.prototype.setQuery = function(query) {};
  v.ChartWrapper.prototype.setRefreshInterval = function(refreshInterval) {};
  v.ChartWrapper.prototype.setView = function(view) {};
  v.ChartWrapper.prototype.setOption = function(key, value) {};
  v.ChartWrapper.prototype.setOption.__before__ = [ sanitizeDrawOpt ];
  v.ChartWrapper.prototype.setOptions = function(options) {};
  v.ChartWrapper.prototype.setOptions.__before__ = [ utils.mapArgs(sanitizeDrawOpts) ];
  v.ChartWrapper.prototype.toJSON = function() {};


  ////////////////////////////////////////////////////////////////////////
  // ChartEditor

  v.ChartEditor = function(opt_config) {};
  v.ChartEditor.__super__ = Object;
  // Worried about clients passing HTML element; masking off all args
  v.ChartEditor.__before__ = [ utils.mapArgs() ];
  v.ChartEditor.prototype.openDialog = function(chartWrapper, opt_options) {};
  // Worried about clients passing HTML element; masking off all but 1st arg
  v.ChartEditor.prototype.openDialog.__before__ = [ utils.mapArgs(utils.identity) ];
  v.ChartEditor.prototype.getChartWrapper = function() {};
  v.ChartEditor.prototype.setChartWrapper = function(chartWrapper) {};
  v.ChartEditor.prototype.closeDialog = function() {};


  ////////////////////////////////////////////////////////////////////////
  // Data manipulation methods

  v.data = {};

  v.data.group = function(data_table, keys, columns) {};
  v.data.group.__before__ = [ utils.mapArgs(utils.identity, utils.copyMixed, utils.copyMixed) ];

  v.data.month = function() {};
  v.data.avg = function() {};
  v.data.count = function() {};
  v.data.max = function() {};
  v.data.min = function() {};
  v.data.sum = function() {};

  v.data.join = function(dt1, dt2, joinMethod, keys, dt1Columns, dt2Columns) {};
  v.data.join.__before__ = [ utils.mapArgs(utils.identity, utils.identity, utils.identity, utils.copyJson, utils.copyJson, utils.copyJson) ];


  ////////////////////////////////////////////////////////////////////////
  // Formatters

  function formatter(name, cb) {
    cb(name + 'Format');
    cb('Table' + name + 'Format');
  }

  formatter('Arrow', function(name) {
    v[name] = function(opt_options) {};
    v[name].__super__ = Object;
    v[name].__before__ = [ utils.mapArgs(utils.copyJson) ];
    v[name].prototype.format = function(dataTable, columnIndex) {};
  });

  formatter('Bar', function(name) {
    v[name] = function(opt_options) {};
    v[name].__super__ = Object;
    v[name].__before__ = [ utils.mapArgs(utils.copyJson) ];
    v[name].prototype.format = function(dataTable, columnIndex) {};
  });

  formatter('Color', function(name) {
    v[name] = function() {};
    v[name].__super__ = Object;
    v[name].__before__ = [ utils.mapArgs(utils.copyJson) ];
    v[name].prototype.addRange = function(from, to, color, bgcolor) {};
    v[name].prototype.addGradientRange =
        function(from, to, color, fromBgColor, toBgColor) {};
    v[name].prototype.format = function(dataTable, columnIndex) {};
  });

  formatter('Date', function(name) {
    v[name] = function(opt_options) {};
    v[name].__super__ = Object;
    v[name].__before__ = [ utils.mapArgs(utils.copyJson) ];
    v[name].prototype.format = function(dataTable, columnIndex) {};
    v[name].prototype.formatValue = function(value) {};
  });

  formatter('Number', function(name) {
    v[name] = function(opt_options) {};
    v[name].__super__ = Object;
    v[name].__before__ = [ utils.mapArgs(utils.copyJson) ];
    v[name].prototype.format = function(dataTable, columnIndex) {};
    v[name].prototype.formatValue = function(value) {};
    v[name].DECIMAL_SEP = 1;
    v[name].GROUP_SEP = 1;
    v[name].DECIMAL_PATTERN = 1;
  });

  formatter('Pattern', function(name) {
    v[name] = function(pattern) {};
    v[name].__super__ = Object;
    v[name].prototype.format =
        function(dataTable, srcColumnIndices, opt_dstColumnIndex) {};
    v[name].prototype.format.__before__ = [
        utils.mapArgs(utils.identity, utils.copyJson, utils.identity)
      ];
  });

  ////////////////////////////////////////////////////////////////////////
  // GadgetHelper

  // This class is not supported in Caja since it is not relevant for any
  // embeddings so far.


  ////////////////////////////////////////////////////////////////////////
  // Query

  v.Query = function(dataSourceUrl, opt_options) {};
  v.Query.__super__ = Object;
  v.Query.__before__ = [ utils.mapArgs(rewriteDataSourceUrl, utils.copyJson) ];
  v.Query.prototype.abort = function() {};
  v.Query.prototype.setRefreshInterval = function(seconds) {};
  v.Query.prototype.setTimeout = function(seconds) {};
  v.Query.prototype.setQuery = function(queryString) {};
  v.Query.prototype.send = function(callback) {};


  ////////////////////////////////////////////////////////////////////////
  // QueryResponse

  v.QueryResponse = function(responseObj) {};
  v.QueryResponse.__super__ = Object;
  v.QueryResponse.prototype.getDataTable = function() {};
  v.QueryResponse.prototype.getDetailedMessage = function() {};
  v.QueryResponse.prototype.getMessage = function() {};
  v.QueryResponse.prototype.getReasons = function() {};
  v.QueryResponse.prototype.isError = function() {};
  v.QueryResponse.prototype.hasWarning = function() {};


  ////////////////////////////////////////////////////////////////////////
  // Errors

  v.errors = {};

  v.errors.addError = function(
      container, message, opt_detailedMessage, opt_options) {};
  v.errors.addError.__before__ = [ utils.mapArgs(
      utils.identity, utils.identity, utils.identity, utils.copyJson) ];
  v.errors.removeAll = function(container) {};
  v.errors.addErrorFromQueryResponse = function(container, response) {};
  v.errors.removeError = function(id) {};
  v.errors.getContainer = function(errorId) {};


  ////////////////////////////////////////////////////////////////////////
  // Events

  v.events = {};

  // addListener() returns an opaque "listener handle" which is an instance of
  // a hidden class, which we haven't tamed (and can't portably name). To make
  // it available to the guest, we wrap it in a closure. We must whitelist the
  // closure via markFunction() or else the membrane will tame it to undefined.
  v.events.addListener = function(eventSource, eventName, eventHandler) {};
  v.events.addListener.__after__ = [
    function(f, self, result) {
      return frame.markFunction(function() {
        return result;
      });
    }
  ];
  v.events.trigger = function(eventSource, eventName, eventDetails) {};
  v.events.removeListener = function(listener) {};
  v.events.removeListener.__before__ = [
    function(f, self, args) {
      // We call the function defensively since it's chosen by the guest and
      // so may or may not actually be one of our own wrapper closures.
      return [ args[0].apply(caja.USELESS, []) ];
    }
  ];
  v.events.removeAllListeners = function(eventSource) {};


  ////////////////////////////////////////////////////////////////////////
  // Assorted static methods

  v.arrayToDataTable = function(arr) {};
  v.arrayToDataTable.__before__ = [ function (f, self, args) {
    var data = args[0];
    var opt_firstRowIsData = args[1];
    if (opt_firstRowIsData) {
      return [arrayOf(sanitizeRowArray)(data), true];
    } else {
      return [sanitizeArrayData(data), false];
    }
  } ];

  v.drawChart = function(json_string_or_object) {};
  v.drawChart.__before__ = [ wrapperStaticFilter(chartSpecWhitelist) ];

  // TODO: Not currently whitelisted because 'components' JSON contains
  // external URLs to data sources in various places, and so we need to
  // write custom code to audit.
  // google.visualization.drawToolbar = function(container, components) {};


  ////////////////////////////////////////////////////////////////////////
  // Specific chart types

  v.PieChart = function(container) {};
  v.PieChart.__super__ = Object;
  v.PieChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.PieChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.PieChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.PieChart.prototype.clearChart = function() {};
  v.PieChart.prototype.getSelection = function() {};
  v.PieChart.prototype.setSelection = function(selection) {};

  v.ScatterChart = function(container) {};
  v.ScatterChart.__super__ = Object;
  v.ScatterChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.ScatterChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.ScatterChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.ScatterChart.prototype.clearChart = function() {};
  v.ScatterChart.prototype.getSelection = function() {};
  v.ScatterChart.prototype.setSelection = function(selection) {};

  v.Gauge = function(container) {};
  v.Gauge.__super__ = Object;
  v.Gauge.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.Gauge.prototype.draw = function(dataTable, opt_options) {};
  v.Gauge.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.Gauge.prototype.clearChart = function() {};

  v.GeoChart = function(container) {};
  v.GeoChart.__super__ = Object;
  v.GeoChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.GeoChart.mapExists = function(userOptions) {};
  v.GeoChart.prototype.clearChart = function() {};
  v.GeoChart.prototype.draw = function(dataTable, userOptions, opt_state) {};
  v.GeoChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.GeoChart.prototype.getSelection = function() {};
  v.GeoChart.prototype.setSelection = function(selection) {};

  v.Table = function(container) {};
  v.Table.__super__ = Object;
  v.Table.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.Table.prototype.draw = function(dataTable, opt_options) {};
  v.Table.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.Table.prototype.clearChart = function() {};
  v.Table.prototype.getSortInfo = function() {};
  v.Table.prototype.getSelection = function() {};
  v.Table.prototype.setSelection = function(selection) {};

  v.TreeMap = function(container) {};
  v.TreeMap.__super__ = Object;
  v.TreeMap.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.TreeMap.prototype.draw = function(dataTable, opt_options) {};
  v.TreeMap.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.TreeMap.prototype.clearChart = function() {};
  v.TreeMap.prototype.getSelection = function() {};
  v.TreeMap.prototype.setSelection = function(selection) {};

  v.ComboChart = function(container) {};
  v.ComboChart.__super__ = Object;
  v.ComboChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.ComboChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.ComboChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.ComboChart.prototype.clearChart = function() {};
  v.ComboChart.prototype.getSelection = function() {};
  v.ComboChart.prototype.setSelection = function(selection) {};

  v.LineChart = function(container) {};
  v.LineChart.__super__ = Object;
  v.LineChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.LineChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.LineChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.LineChart.prototype.clearChart = function() {};
  v.LineChart.prototype.getSelection = function() {};
  v.LineChart.prototype.setSelection = function(selection) {};

  v.BarChart = function(container) {};
  v.BarChart.__super__ = Object;
  v.BarChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.BarChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.BarChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.BarChart.prototype.clearChart = function() {};
  v.BarChart.prototype.getSelection = function() {};
  v.BarChart.prototype.setSelection = function(selection) {};

  v.ColumnChart = function(container) {};
  v.ColumnChart.__super__ = Object;
  v.ColumnChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.ColumnChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.ColumnChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.ColumnChart.prototype.clearChart = function() {};
  v.ColumnChart.prototype.getSelection = function() {};
  v.ColumnChart.prototype.setSelection = function(selection) {};

  v.AreaChart = function(container) {};
  v.AreaChart.__super__ = Object;
  v.AreaChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.AreaChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.AreaChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.AreaChart.prototype.clearChart = function() {};
  v.AreaChart.prototype.getSelection = function() {};
  v.AreaChart.prototype.setSelection = function(selection) {};

  v.CandlestickChart = function(container) {};
  v.CandlestickChart.__super__ = Object;
  v.CandlestickChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.CandlestickChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.CandlestickChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.CandlestickChart.prototype.clearChart = function() {};
  v.CandlestickChart.prototype.getSelection = function() {};
  v.CandlestickChart.prototype.setSelection = function(selection) {};

  v.AnnotatedTimeLine = function(container) {};
  v.AnnotatedTimeLine.__super__ = Object;
  v.AnnotatedTimeLine.__before__ = [
     function(f, self, args) {
       var outer = args[0];
       var inner = utils.opaqueNode(outer);
       inner.style.width = outer.style.width;
       inner.style.height = outer.style.height;
       return [ inner ];
     }
  ];
  v.AnnotatedTimeLine.prototype.draw = function(data, opt_options) {};
  v.AnnotatedTimeLine.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.AnnotatedTimeLine.prototype.getSelection = function() {};
  v.AnnotatedTimeLine.prototype.getVisibleChartRange = function() {};
  v.AnnotatedTimeLine.prototype.setVisibleChartRange = function(firstDate, lastDate, opt_animate) {};
  v.AnnotatedTimeLine.prototype.showDataColumns = function(columnIndexes) {};
  v.AnnotatedTimeLine.prototype.hideDataColumns = function(columnIndexes) {};

  v.GeoMap = function(container) {};
  v.GeoMap.__super__ = Object;
  v.GeoMap.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.GeoMap.clickOnRegion = function(id, zoomLevel, segmentBy, instanceIndex) {};
  v.GeoMap.prototype.draw = function(dataTable, opt_options) {};
  v.GeoMap.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.GeoMap.prototype.getSelection = function() {};
  v.GeoMap.prototype.setSelection = function(selection) {};

  v.IntensityMap = function(container) {};
  v.IntensityMap.__super__ = Object;
  v.IntensityMap.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.IntensityMap.prototype.draw = function(dataTable, opt_options) {};
  v.IntensityMap.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.IntensityMap.prototype.getSelection = function() {};
  v.IntensityMap.prototype.setSelection = function(selection) {};

  v.OrgChart = function(container) {};
  v.OrgChart.__super__ = Object;
  v.OrgChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.OrgChart.prototype.draw = function(dataTable, opt_options) {};
  v.OrgChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.OrgChart.prototype.getSelection = function() {};
  v.OrgChart.prototype.setSelection = function(selection) {};
  v.OrgChart.prototype.getCollapsedNodes = function() {};
  v.OrgChart.prototype.getChildrenIndexes = function(rowInd) {};
  v.OrgChart.prototype.collapse = function(rowInd, collapse) {};

  // TODO(ihab.awad): Map does not seem to work. It appears to be written
  // for an obsolete version of the Google Maps API. Investigate.
  //
  // v.Map = function(container) {};
  // v.Map.__super__ = Object;
  // v.Map.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  // v.Map.prototype.draw = function(dataTable, opt_options) {};
  // v.Map.prototype.draw.__before__ = [ utils.mapArgs(
  //     utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  // v.Map.prototype.getSelection = function() {};
  // v.Map.prototype.setSelection = function(selection) {};

  v.MotionChart = function(container) {};
  v.MotionChart.__super__ = Object;
  v.MotionChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.MotionChart.prototype.draw = function(dataTable, opt_options) {};
  v.MotionChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.MotionChart.prototype.getState = function() {};

  v.BubbleChart = function(container) {};
  v.BubbleChart.__super__ = Object;
  v.BubbleChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.BubbleChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.BubbleChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.BubbleChart.prototype.clearChart = function() {};
  v.BubbleChart.prototype.getSelection = function() {};
  v.BubbleChart.prototype.setSelection = function(selection) {};

  v.SteppedAreaChart = function(container) {};
  v.SteppedAreaChart.__super__ = Object;
  v.SteppedAreaChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.SteppedAreaChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.SteppedAreaChart.prototype.draw.__before__ = [ utils.mapArgs(
      utils.identity, sanitizeDrawOpts, utils.copyJson) ];
  v.SteppedAreaChart.prototype.clearChart = function() {};
  v.SteppedAreaChart.prototype.getSelection = function() {};
  v.SteppedAreaChart.prototype.setSelection = function(selection) {};


  ////////////////////////////////////////////////////////////////////////
  // Dashboard

  v.Dashboard = function(container) {};
  v.Dashboard.__super__ = Object;
  v.Dashboard.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.Dashboard.prototype.bind = function(controls, charts) {};
  v.Dashboard.prototype.draw = function(dataTable) {};


  ////////////////////////////////////////////////////////////////////////
  // ControlWrapper

  v.ControlWrapper = function(opt_spec) {};
  v.ControlWrapper.__super__ = Object;
  v.ControlWrapper.__before__ = [ wrapperCtorFilter(controlSpecWhitelist) ];
  v.ControlWrapper.prototype.draw = function() {};
  v.ControlWrapper.prototype.toJSON = function() {};
  v.ControlWrapper.prototype.clone = function() {};
  v.ControlWrapper.prototype.getControlType = function() {};
  v.ControlWrapper.prototype.getControlName = function() {};
  v.ControlWrapper.prototype.getControl = function() {};
  v.ControlWrapper.prototype.getContainerId = function() {};
  v.ControlWrapper.prototype.getContainerId.__after__ = [ containerIdAfterGet ];
  v.ControlWrapper.prototype.getOption = function(key, opt_default_val) {};
  v.ControlWrapper.prototype.getOptions = function() {};
  v.ControlWrapper.prototype.getState = function() {};
  v.ControlWrapper.prototype.setControlType = function(type) {};
  v.ControlWrapper.prototype.setControlType.__before__ = [ utils.mapArgs(sanitizeControlType) ];
  v.ControlWrapper.prototype.setControlName = function(name) {};
  v.ControlWrapper.prototype.setContainerId = function(id) {};
  v.ControlWrapper.prototype.setContainerId.__before__ = [ containerIdBeforeSet ];
  v.ControlWrapper.prototype.setOption = function(key, value) {};
  v.ControlWrapper.prototype.setOption.__before__ = [ sanitizeControlOpt ];
  v.ControlWrapper.prototype.setOptions = function(options_obj) {};
  v.ControlWrapper.prototype.setOptions.__before__ = [ utils.mapArgs(sanitizeControlOpts) ];
  v.ControlWrapper.prototype.setState = function(state_obj) {};
  v.ControlWrapper.prototype.setState.__before__ = [ utils.mapArgs(utils.copyMixed) ];


  return {
    version: '1.0',
    value: {
      google: {
        visualization: v
      }
    }
  };
});
