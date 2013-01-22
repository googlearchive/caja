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
 * @requires caja, JSON
 */
caja.tamingGoogleLoader.addPolicyFactory('google.visualization', function(frame, utils) {

  var v = {};


  ////////////////////////////////////////////////////////////////////////
  // Utilities to support a common pattern for a component:
  //   - The ctor accepts a JSON structure with a field 'containerId'
  //   - The instance supports get/setContainerId()
  //   - The 'containerId' is the DOM ID of the container DOM element
  // We need to map IDs back and forth because we sneakily embed an opaque
  // node inside the "container" supplied by the guest, and put the actual
  // visualization inside *that*.

  function containerIdBeforeCtor(f, self, args) {
    var tameContainerId = undefined;
    if (args.length < 1) { return []; }
    var spec = args[0];
    if (typeof spec === 'string') { spec = JSON.parse(spec); }
    spec = utils.copyJson(spec);
    if (spec.containerId) {
      tameContainerId = spec.containerId;
      spec.containerId = utils.opaqueNodeById(spec.containerId);
    }
    if (spec.dataSourceUrl) {
      spec.dataSourceUrl = rewriteDataSourceUrl(spec.dataSourceUrl);
    }
    self.tameContainerId___ = tameContainerId;
    return [spec];
  }

  function containerIdAfterGet(f, self, result) {
    return self.tameContainerId___;
  }

  function containerIdBeforeSet(f, self, args) {
    self.tameContainerId___ = args[0];
    return [utils.opaqueNodeById(args[0])];
  }


  ////////////////////////////////////////////////////////////////////////
  // Equivalent of containerIdBeforeCtor, but for static functions.

  function containerIdBeforeStatic(f, self, args) {
    var tameContainerId;
    if (args.length < 1) { return []; }
    var spec = args[0];
    if ((typeof spec) === 'string') { spec = JSON.parse(spec); }
    spec = utils.copyJson(spec);
    if (spec.containerId) {
      spec.containerId= utils.opaqueNodeById(spec.containerId);
    }
    if (spec.dataSourceUrl) {
      spec.dataSourceUrl = rewriteDataSourceUrl(spec.dataSourceUrl);
    }
    return [spec];
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
  // Copy 2nd argument of draw() on a visualization, ensuring that the
  // result has HTML in tooltips turned off

  function copyDrawOpts(opts) {
    opts = utils.copyJson(opts);
    opts.allowHtml = false;
    opts.tooltip = { isHtml: false };
    return opts;
  }

  ////////////////////////////////////////////////////////////////////////
  // DataTable

  v.DataTable = function(opt_data, opt_version) {};
  v.DataTable.__super__ = Object;
  v.DataTable.__before__ = [ utils.mapArgs(utils.copyMixed, utils.identity) ];
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
  v.DataTable.prototype.setTableProperties.__before__ = [ utils.mapArgs(utils.copyMixed) ];
  v.DataTable.prototype.setTableProperty = function(property, value) {};
  v.DataTable.prototype.setValue = function(rowIndex, columnIndex, value) {};
  v.DataTable.prototype.setFormattedValue = function(rowIndex, columnIndex, formattedValue) {};
  v.DataTable.prototype.setProperties = function(rowIndex, columnIndex, properties) {};
  v.DataTable.prototype.setProperties.__before__ = [ utils.mapArgs(utils.identity, utils.identity, utils.copyMixed) ];
  v.DataTable.prototype.setProperty = function(rowIndex, columnIndex, property, value) {};
  v.DataTable.prototype.setCell = function(rowIndex, columnIndex, opt_value, opt_formattedValue, opt_properties) {};
  v.DataTable.prototype.setRowProperties = function(rowIndex, properties) {};
  v.DataTable.prototype.setRowProperties.__before__ = [ utils.mapArgs(utils.identity, utils.copyMixed) ];
  v.DataTable.prototype.setRowProperty = function(rowIndex, property, value) {};
  v.DataTable.prototype.getRowProperty = function(rowIndex, property) {};
  v.DataTable.prototype.getRowProperties = function(rowIndex) {};
  v.DataTable.prototype.setColumnLabel = function(columnIndex, newLabel) {};
  v.DataTable.prototype.setColumnProperties = function(columnIndex, properties) {};
  v.DataTable.prototype.setColumnProperties.__before__ = [ utils.mapArgs(utils.identity, utils.copyMixed) ];
  v.DataTable.prototype.setColumnProperty = function(columnIndex, property, value) {};
  v.DataTable.prototype.getColumnProperty = function(columnIndex, property) {};
  v.DataTable.prototype.getColumnProperties = function(columnIndex) {};
  v.DataTable.prototype.insertColumn = function(atColIndex, type, opt_label, opt_id) {};
  v.DataTable.prototype.addColumn = function(type, opt_label, opt_id) {};
  v.DataTable.prototype.addColumn.__before__ = [
    function(f, self, args) {
      if (args.length === 1) {
        return [ utils.copyJson(args[0]) ];
      } else {
        return args;
      }
    }
  ];
  v.DataTable.prototype.insertRows = function(atRowIndex, numOrArray) {};
  v.DataTable.prototype.insertRows.__before__ = [ utils.mapArgs(utils.identity, utils.copyMixed) ];
  v.DataTable.prototype.addRows = function(numOrArray) {};
  v.DataTable.prototype.addRows.__before__ = [ utils.mapArgs(utils.copyMixed) ];
  v.DataTable.prototype.addRow = function(opt_cellArray) {};
  v.DataTable.prototype.addRow.__before__ = [ utils.mapArgs(utils.copyMixed) ];
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
  v.ChartWrapper.__before__ = [ containerIdBeforeCtor ];
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
  v.ChartWrapper.prototype.setContainerId = function(containerId) {};
  v.ChartWrapper.prototype.setContainerId.__before__ = [ containerIdBeforeSet ];
  v.ChartWrapper.prototype.setQuery = function(query) {};
  v.ChartWrapper.prototype.setRefreshInterval = function(refreshInterval) {};
  v.ChartWrapper.prototype.setView = function(view) {};
  v.ChartWrapper.prototype.setOption = function(key, value) {};
  v.ChartWrapper.prototype.setOptions = function(options) {};
  v.ChartWrapper.prototype.setOptions.__before__ = [ utils.mapArgs(utils.copyJson) ];
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

  v.ArrowFormat = function(opt_options) {};
  v.ArrowFormat.__super__ = Object;
  v.ArrowFormat.__before__ = [ utils.mapArgs(utils.copyJson) ];
  v.ArrowFormat.prototype.format = function(dataTable, columnIndex) {};

  v.BarFormat = function(opt_options) {};
  v.BarFormat.__super__ = Object;
  v.BarFormat.__before__ = [ utils.mapArgs(utils.copyJson) ];
  v.BarFormat.prototype.format = function(dataTable, columnIndex) {};

  v.ColorFormat = function() {};
  v.ColorFormat.__super__ = Object;
  v.ColorFormat.__before__ = [ utils.mapArgs(utils.copyJson) ];
  v.ColorFormat.prototype.addRange = function(from, to, color, bgcolor) {};
  v.ColorFormat.prototype.addGradientRange = function(from, to, color, fromBgColor, toBgColor) {};
  v.ColorFormat.prototype.format = function(dataTable, columnIndex) {};

  v.DateFormat = function(opt_options) {};
  v.DateFormat.__super__ = Object;
  v.DateFormat.__before__ = [ utils.mapArgs(utils.copyJson) ];
  v.DateFormat.prototype.format = function(dataTable, columnIndex) {};
  v.DateFormat.prototype.formatValue = function(value) {};

  v.NumberFormat = function(opt_options) {};
  v.NumberFormat.__super__ = Object;
  v.NumberFormat.__before__ = [ utils.mapArgs(utils.copyJson) ];
  v.NumberFormat.prototype.format = function(dataTable, columnIndex) {};
  v.NumberFormat.prototype.formatValue = function(value) {};
  v.NumberFormat.DECIMAL_SEP = 1;
  v.NumberFormat.GROUP_SEP = 1;
  v.NumberFormat.DECIMAL_PATTERN = 1;

  v.PatternFormat = function(pattern) {};
  v.PatternFormat.__super__ = Object;
  v.PatternFormat.prototype.format = function(dataTable, srcColumnIndices, opt_dstColumnIndex) {};
  v.PatternFormat.prototype.format.__before__ = [ utils.mapArgs(utils.identity, utils.copyJson, utils.identity) ];


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

  v.errors.addError = function(container, message, opt_detailedMessage, opt_options) {};
  v.errors.addError.__before__ = [ utils.mapArgs(utils.identity, utils.identity, utils.identity, utils.copyJson) ];
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
  v.arrayToDataTable.__before__ = [ utils.mapArgs(utils.copyJson, utils.identity) ];

  v.drawChart = function(json_string_or_object) {};
  v.drawChart.__before__ = [ containerIdBeforeStatic ];

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
  v.PieChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.PieChart.prototype.clearChart = function() {};
  v.PieChart.prototype.getSelection = function() {};
  v.PieChart.prototype.setSelection = function(selection) {};

  v.ScatterChart = function(container) {};
  v.ScatterChart.__super__ = Object;
  v.ScatterChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.ScatterChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.ScatterChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.ScatterChart.prototype.clearChart = function() {};
  v.ScatterChart.prototype.getSelection = function() {};
  v.ScatterChart.prototype.setSelection = function(selection) {};

  v.Gauge = function(container) {};
  v.Gauge.__super__ = Object;
  v.Gauge.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.Gauge.prototype.draw = function(dataTable, opt_options) {};
  v.Gauge.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.Gauge.prototype.clearChart = function() {};

  v.GeoChart = function(container) {};
  v.GeoChart.__super__ = Object;
  v.GeoChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.GeoChart.mapExists = function(userOptions) {};
  v.GeoChart.prototype.clearChart = function() {};
  v.GeoChart.prototype.draw = function(dataTable, userOptions, opt_state) {};
  v.GeoChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.GeoChart.prototype.getSelection = function() {};
  v.GeoChart.prototype.setSelection = function(selection) {};

  v.Table = function(container) {};
  v.Table.__super__ = Object;
  v.Table.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.Table.prototype.draw = function(dataTable, opt_options) {};
  v.Table.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.Table.prototype.clearChart = function() {};
  v.Table.prototype.getSortInfo = function() {};
  v.Table.prototype.getSelection = function() {};
  v.Table.prototype.setSelection = function(selection) {};

  v.TreeMap = function(container) {};
  v.TreeMap.__super__ = Object;
  v.TreeMap.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.TreeMap.prototype.draw = function(dataTable, opt_options) {};
  v.TreeMap.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.TreeMap.prototype.clearChart = function() {};
  v.TreeMap.prototype.getSelection = function() {};
  v.TreeMap.prototype.setSelection = function(selection) {};

  v.ComboChart = function(container) {};
  v.ComboChart.__super__ = Object;
  v.ComboChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.ComboChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.ComboChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.ComboChart.prototype.clearChart = function() {};
  v.ComboChart.prototype.getSelection = function() {};
  v.ComboChart.prototype.setSelection = function(selection) {};

  v.LineChart = function(container) {};
  v.LineChart.__super__ = Object;
  v.LineChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.LineChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.LineChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.LineChart.prototype.clearChart = function() {};
  v.LineChart.prototype.getSelection = function() {};
  v.LineChart.prototype.setSelection = function(selection) {};

  v.BarChart = function(container) {};
  v.BarChart.__super__ = Object;
  v.BarChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.BarChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.BarChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.BarChart.prototype.clearChart = function() {};
  v.BarChart.prototype.getSelection = function() {};
  v.BarChart.prototype.setSelection = function(selection) {};

  v.ColumnChart = function(container) {};
  v.ColumnChart.__super__ = Object;
  v.ColumnChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.ColumnChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.ColumnChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.ColumnChart.prototype.clearChart = function() {};
  v.ColumnChart.prototype.getSelection = function() {};
  v.ColumnChart.prototype.setSelection = function(selection) {};

  v.AreaChart = function(container) {};
  v.AreaChart.__super__ = Object;
  v.AreaChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.AreaChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.AreaChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.AreaChart.prototype.clearChart = function() {};
  v.AreaChart.prototype.getSelection = function() {};
  v.AreaChart.prototype.setSelection = function(selection) {};

  v.CandlestickChart = function(container) {};
  v.CandlestickChart.__super__ = Object;
  v.CandlestickChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.CandlestickChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.CandlestickChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.CandlestickChart.prototype.clearChart = function() {};
  v.CandlestickChart.prototype.getSelection = function() {};
  v.CandlestickChart.prototype.setSelection = function(selection) {};

  // TODO(ihab.awad): AnnotatedTimeLine data is garbled in testing under ES5.
  // This is disabled until we fix this.

  // v.AnnotatedTimeLine = function(container) {};
  // v.AnnotatedTimeLine.__super__ = Object;
  // v.AnnotatedTimeLine.__before__ = [
  //   function(f, self, args) {
  //     var outer = args[0];
  //     var inner = utils.opaqueNode(outer);
  //     inner.style.width = outer.style.width;
  //     inner.style.height = outer.style.height;
  //     return [ inner ];
  //   }
  // ];
  // v.AnnotatedTimeLine.prototype.draw = function(data, opt_options) {};
  // v.AnnotatedTimeLine.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  // v.AnnotatedTimeLine.prototype.getSelection = function() {};
  // v.AnnotatedTimeLine.prototype.getVisibleChartRange = function() {};
  // v.AnnotatedTimeLine.prototype.setVisibleChartRange = function(firstDate, lastDate, opt_animate) {};
  // v.AnnotatedTimeLine.prototype.showDataColumns = function(columnIndexes) {};
  // v.AnnotatedTimeLine.prototype.hideDataColumns = function(columnIndexes) {};

  v.GeoMap = function(container) {};
  v.GeoMap.__super__ = Object;
  v.GeoMap.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.GeoMap.clickOnRegion = function(id, zoomLevel, segmentBy, instanceIndex) {};
  v.GeoMap.prototype.draw = function(dataTable, opt_options) {};
  v.GeoMap.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.GeoMap.prototype.getSelection = function() {};
  v.GeoMap.prototype.setSelection = function(selection) {};

  v.IntensityMap = function(container) {};
  v.IntensityMap.__super__ = Object;
  v.IntensityMap.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.IntensityMap.prototype.draw = function(dataTable, opt_options) {};
  v.IntensityMap.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.IntensityMap.prototype.getSelection = function() {};
  v.IntensityMap.prototype.setSelection = function(selection) {};

  v.OrgChart = function(container) {};
  v.OrgChart.__super__ = Object;
  v.OrgChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.OrgChart.prototype.draw = function(dataTable, opt_options) {};
  v.OrgChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
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
  // v.Map.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  // v.Map.prototype.getSelection = function() {};
  // v.Map.prototype.setSelection = function(selection) {};

  v.MotionChart = function(container) {};
  v.MotionChart.__super__ = Object;
  v.MotionChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.MotionChart.prototype.draw = function(dataTable, opt_options) {};
  v.MotionChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.MotionChart.prototype.getState = function() {};

  v.BubbleChart = function(container) {};
  v.BubbleChart.__super__ = Object;
  v.BubbleChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.BubbleChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.BubbleChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
  v.BubbleChart.prototype.clearChart = function() {};
  v.BubbleChart.prototype.getSelection = function() {};
  v.BubbleChart.prototype.setSelection = function(selection) {};

  v.SteppedAreaChart = function(container) {};
  v.SteppedAreaChart.__super__ = Object;
  v.SteppedAreaChart.__before__ = [ utils.mapArgs(utils.opaqueNode) ];
  v.SteppedAreaChart.prototype.draw = function(data, opt_options, opt_state) {};
  v.SteppedAreaChart.prototype.draw.__before__ = [ utils.mapArgs(utils.identity, copyDrawOpts, utils.copyJson) ];
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
  v.ControlWrapper.__before__ = [ containerIdBeforeCtor ];
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
  v.ControlWrapper.prototype.setControlName = function(name) {};
  v.ControlWrapper.prototype.setContainerId = function(id) {};
  v.ControlWrapper.prototype.setContainerId.__before__ = [ containerIdBeforeSet ];
  v.ControlWrapper.prototype.setOption = function(key, value) {};
  v.ControlWrapper.prototype.setOptions = function(options_obj) {};
  v.ControlWrapper.prototype.setOptions.__before__ = [ utils.mapArgs(utils.copyJson) ];
  v.ControlWrapper.prototype.setState = function(state_obj) {};
  v.ControlWrapper.prototype.setState.__before__ = [ utils.mapArgs(utils.copyJson) ];


  return {
    version: '1.0',
    value: {
      google: {
        visualization: v
      }
    }
  };
});
