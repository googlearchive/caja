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

function defGoogle(frame) {

  var googlePolicy = (function() {

    var google = {};

    function drawBeforeAdvice(f, self, args) {
      var result = [ args[0] ];
      for (var i = 1; i < args.length; i++) {
        result.push(copyJson(args[i]));
      }
      return result;
    }

    function opaqueNodeAdvice(f, self, args) {
      return [ opaqueNode(args[0]) ];
    }

    ////////////////////////////////////////////////////////////////////////
    // gViz integration

    google.visualization = {};

    /** @constructor */
    google.visualization.DataTable = function(opt_data, opt_version) {};
    google.visualization.DataTable.__super__ = Object;
    google.visualization.DataTable.prototype.getNumberOfRows = function() {};
    google.visualization.DataTable.prototype.getNumberOfColumns = function() {};
    google.visualization.DataTable.prototype.clone = function() {};
    google.visualization.DataTable.prototype.getColumnId = function(columnIndex) {};
    google.visualization.DataTable.prototype.getColumnIndex = function(columnId) {};
    google.visualization.DataTable.prototype.getColumnLabel = function(columnIndex) {};
    google.visualization.DataTable.prototype.getColumnPattern = function(columnIndex) {};
    google.visualization.DataTable.prototype.getColumnRole = function(columnIndex) {};
    google.visualization.DataTable.prototype.getColumnType = function(columnIndex) {};
    google.visualization.DataTable.prototype.getValue = function(rowIndex, columnIndex) {};
    google.visualization.DataTable.prototype.getFormattedValue = function(rowIndex, columnIndex) {};
    google.visualization.DataTable.prototype.getProperty = function(rowIndex, columnIndex, property) {};
    google.visualization.DataTable.prototype.getProperties = function(rowIndex, columnIndex) {};
    google.visualization.DataTable.prototype.getTableProperties = function() {};
    google.visualization.DataTable.prototype.getTableProperty = function(property) {};
    google.visualization.DataTable.prototype.setTableProperties = function(properties) {};
    google.visualization.DataTable.prototype.setTableProperty = function(property, value) {};
    google.visualization.DataTable.prototype.setValue = function(rowIndex, columnIndex, value) {};
    google.visualization.DataTable.prototype.setFormattedValue = function(rowIndex, columnIndex, formattedValue) {};
    google.visualization.DataTable.prototype.setProperties = function(rowIndex, columnIndex, properties) {};
    google.visualization.DataTable.prototype.setProperty = function(rowIndex, columnIndex, property, value) {};
    google.visualization.DataTable.prototype.setCell = function(rowIndex, columnIndex, opt_value, opt_formattedValue, opt_properties) {};
    google.visualization.DataTable.prototype.setRowProperties = function(rowIndex, properties) {};
    google.visualization.DataTable.prototype.setRowProperty = function(rowIndex, property, value) {};
    google.visualization.DataTable.prototype.getRowProperty = function(rowIndex, property) {};
    google.visualization.DataTable.prototype.getRowProperties = function(rowIndex) {};
    google.visualization.DataTable.prototype.setColumnLabel = function(columnIndex, newLabel) {};
    google.visualization.DataTable.prototype.setColumnProperties = function(columnIndex, properties) {};
    google.visualization.DataTable.prototype.setColumnProperty = function(columnIndex, property, value) {};
    google.visualization.DataTable.prototype.getColumnProperty = function(columnIndex, property) {};
    google.visualization.DataTable.prototype.getColumnProperties = function(columnIndex) {};
    google.visualization.DataTable.prototype.insertColumn = function(atColIndex, type, opt_label, opt_id) {};
    google.visualization.DataTable.prototype.addColumn = function(type, opt_label, opt_id) {};
    google.visualization.DataTable.prototype.insertRows = function(atRowIndex, numOrArray) {};
    google.visualization.DataTable.prototype.addRows = function(numOrArray) {};
    google.visualization.DataTable.prototype.addRow = function(opt_cellArray) {};
    google.visualization.DataTable.prototype.getColumnRange = function(columnIndex) {};
    google.visualization.DataTable.prototype.getSortedRows = function(sortColumns) {};
    google.visualization.DataTable.prototype.sort = function(sortColumns) {};
    google.visualization.DataTable.prototype.getDistinctValues = function(column) {};
    google.visualization.DataTable.prototype.getFilteredRows = function(columnFilters) {};
    google.visualization.DataTable.prototype.removeRows = function(fromRowIndex, numRows) {};
    google.visualization.DataTable.prototype.removeRow = function(rowIndex) {};
    google.visualization.DataTable.prototype.removeColumns = function(fromColIndex, numCols) {};
    google.visualization.DataTable.prototype.removeColumn = function(colIndex) {};

    /** @return {string} JSON representation. */
    google.visualization.DataTable.prototype.toJSON = function() {
      return copyJson(this.toJSON());
    };
    google.visualization.DataTable.prototype.toJSON.__subst__ = true;

    google.visualization.arrayToDataTable = function(arr) {};

    /** @constructor */
    google.visualization.AreaChart = function(container) {};
    google.visualization.AreaChart.__before__ = [ opaqueNodeAdvice ];
    google.visualization.AreaChart.__super__ = Object;
    google.visualization.AreaChart.prototype.draw = function(data, opt_options, opt_state) {};
    google.visualization.AreaChart.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.AreaChart.prototype.clearChart = function() {};
    // google.visualization.AreaChart.prototype.getSelection = function() {};
    // google.visualization.AreaChart.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.BarChart = function(container) {};
    google.visualization.BarChart.__before__ = [ opaqueNodeAdvice ];
    google.visualization.BarChart.__super__ = Object;
    google.visualization.BarChart.prototype.draw = function(data, opt_options, opt_state) {};
    google.visualization.BarChart.prototype.clearChart = function() {};
    // google.visualization.BarChart.prototype.getSelection = function() {};
    // google.visualization.BarChart.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.BubbleChart = function(container) {};
    google.visualization.BubbleChart.__before__ = [ opaqueNodeAdvice ];
    google.visualization.BubbleChart.__super__ = Object;
    google.visualization.BubbleChart.prototype.draw = function(data, opt_options, opt_state) {};
    google.visualization.BubbleChart.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.BubbleChart.prototype.clearChart = function() {};
    // google.visualization.BubbleChart.prototype.getSelection = function() {};
    // google.visualization.BubbleChart.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.CandlestickChart = function(container) {};
    google.visualization.CandlestickChart.__before__ = [ opaqueNodeAdvice ];
    google.visualization.CandlestickChart.__super__ = Object;
    google.visualization.CandlestickChart.prototype.draw = function(data, opt_options, opt_state) {};
    google.visualization.CandlestickChart.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.CandlestickChart.prototype.clearChart = function() {};
    // google.visualization.CandlestickChart.prototype.getSelection = function() {};
    // google.visualization.CandlestickChart.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.ColumnChart = function(container) {};
    google.visualization.ColumnChart.__before__ = [ opaqueNodeAdvice ];
    google.visualization.ColumnChart.__super__ = Object;
    google.visualization.ColumnChart.prototype.draw = function(data, opt_options, opt_state) {};
    google.visualization.ColumnChart.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.ColumnChart.prototype.clearChart = function() {};
    // google.visualization.ColumnChart.prototype.getSelection = function() {};
    // google.visualization.ColumnChart.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.ComboChart = function(container) {};
    google.visualization.ComboChart.__before__ = [ opaqueNodeAdvice ];
    google.visualization.ComboChart.__super__ = Object;
    google.visualization.ComboChart.prototype.draw = function(data, opt_options, opt_state) {};
    google.visualization.ComboChart.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.ComboChart.prototype.clearChart = function() {};
    // google.visualization.ComboChart.prototype.getSelection = function() {};
    // google.visualization.ComboChart.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.Gauge = function(container) {};
    google.visualization.Gauge.__before__ = [ opaqueNodeAdvice ];
    google.visualization.Gauge.__super__ = Object;
    google.visualization.Gauge.prototype.draw = function(dataTable, opt_options) {};
    google.visualization.Gauge.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.Gauge.prototype.clearChart = function() {};

    /** @constructor */
    google.visualization.GeoChart = function(container) {};
    google.visualization.GeoChart.__before__ = [ opaqueNodeAdvice ];
    google.visualization.GeoChart.__super__ = Object;
    // google.visualization.GeoChart.mapExists = function(userOptions) {};
    google.visualization.GeoChart.prototype.clearChart = function() {};
    google.visualization.GeoChart.prototype.draw = function(dataTable, userOptions, opt_state) {};
    google.visualization.GeoChart.prototype.draw.__before__ = [ drawBeforeAdvice ];
    // google.visualization.GeoChart.prototype.getSelection = function() {};
    // google.visualization.GeoChart.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.LineChart = function(container) {};
    google.visualization.LineChart.__before__ = [ opaqueNodeAdvice ];
    google.visualization.LineChart.__super__ = Object;
    google.visualization.LineChart.prototype.draw = function(data, opt_options, opt_state) {};
    google.visualization.LineChart.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.LineChart.prototype.clearChart = function() {};
    // google.visualization.LineChart.prototype.getSelection = function() {};
    // google.visualization.LineChart.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.PieChart = function(container) {};
    google.visualization.PieChart.__before__ = [ opaqueNodeAdvice ];
    google.visualization.PieChart.__super__ = Object;
    google.visualization.PieChart.prototype.draw = function(data, opt_options, opt_state) {};
    google.visualization.PieChart.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.PieChart.prototype.clearChart = function() {};
    // google.visualization.PieChart.prototype.getSelection = function() {};
    // google.visualization.PieChart.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.ScatterChart = function(container) {};
    google.visualization.ScatterChart.__before__ = [ opaqueNodeAdvice ];
    google.visualization.ScatterChart.__super__ = Object;
    google.visualization.ScatterChart.prototype.draw = function(data, opt_options, opt_state) {};
    google.visualization.ScatterChart.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.ScatterChart.prototype.clearChart = function() {};
    // google.visualization.ScatterChart.prototype.getSelection = function() {};
    // google.visualization.ScatterChart.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.SteppedAreaChart = function(container) {};
    google.visualization.SteppedAreaChart.__before__ = [ opaqueNodeAdvice ];
    google.visualization.SteppedAreaChart.__super__ = Object;
    google.visualization.SteppedAreaChart.prototype.draw = function(data, opt_options, opt_state) {};
    google.visualization.SteppedAreaChart.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.SteppedAreaChart.prototype.clearChart = function() {};
    // google.visualization.SteppedAreaChart.prototype.getSelection = function() {};
    // google.visualization.SteppedAreaChart.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.Table = function(container) {};
    google.visualization.Table.__before__ = [ opaqueNodeAdvice ];
    google.visualization.Table.__super__ = Object;
    google.visualization.Table.prototype.draw = function(dataTable, opt_options) {};
    google.visualization.Table.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.Table.prototype.clearChart = function() {};
    // google.visualization.Table.prototype.getSortInfo = function() {};
    // google.visualization.Table.prototype.getSelection = function() {};
    // google.visualization.Table.prototype.setSelection = function(selection) {};

    /** @constructor */
    google.visualization.TreeMap = function(container) {};
    google.visualization.TreeMap.__before__ = [ opaqueNodeAdvice ];
    google.visualization.TreeMap.__super__ = Object;
    google.visualization.TreeMap.prototype.draw = function(dataTable, opt_options) {};
    google.visualization.TreeMap.prototype.draw.__before__ = [ drawBeforeAdvice ];
    google.visualization.TreeMap.prototype.clearChart = function() {};
    // google.visualization.TreeMap.prototype.getSelection = function() {};
    // google.visualization.TreeMap.prototype.setSelection = function(selection) {};

    ////////////////////////////////////////////////////////////////////////
    // OnePick integration

    google.picker = {};

    google.picker.DocsUploadView = function() {};
    google.picker.DocsUploadView.__super__ = Object;
    google.picker.DocsUploadView.prototype.setIncludeFolders = function(boolean) {};

    google.picker.View = function() {};
    google.picker.View.__super__ = Object;
    google.picker.View.prototype.getId = function() {};
    google.picker.View.prototype.setMimeTypes = function() {};
    google.picker.View.prototype.setQuery = function() {};

    google.picker.DocsView = function() {};
    google.picker.DocsView.__super__ = ['google', 'picker', 'View'];
    google.picker.DocsView.prototype.setIncludeFolders = function() {};
    google.picker.DocsView.prototype.setMode = function() {};
    google.picker.DocsView.prototype.setOwnedByMe = function() {};
    google.picker.DocsView.prototype.setStarred = function() {};

    google.picker.DocsViewMode = {};
    google.picker.DocsViewMode.GRID = 1;
    google.picker.DocsViewMode.LIST = 1;

    google.picker.Feature = {};
    google.picker.Feature.MINE_ONLY = 1;
    google.picker.Feature.MULTISELECT_ENABLED = 1;
    google.picker.Feature.NAV_HIDDEN = 1;
    google.picker.Feature.SIMPLE_UPLOAD_ENABLED = 1;

    google.picker.ImageSearchView = function() {};
    google.picker.ImageSearchView.__super__ = ['google', 'picker', 'View'];
    google.picker.ImageSearchView.prototype.setLicense = function() {};
    google.picker.ImageSearchView.prototype.setSite = function() {};
    google.picker.ImageSearchView.prototype.setSize = function() {};

    google.picker.ImageSearchView.License = {};
    google.picker.ImageSearchView.License.NONE = 1;
    google.picker.ImageSearchView.License.COMMERCIAL_REUSE = 1;
    google.picker.ImageSearchView.License.COMMERCIAL_REUSE_WITH_MODIFICATION = 1;
    google.picker.ImageSearchView.License.REUSE = 1;
    google.picker.ImageSearchView.License.REUSE_WITH_MODIFICATION = 1;

    google.picker.ImageSearchView.Size = {};
    google.picker.ImageSearchView.Size.SIZE_QSVGA = 1;
    google.picker.ImageSearchView.Size.SIZE_VGA = 1;
    google.picker.ImageSearchView.Size.SIZE_SVGA = 1;
    google.picker.ImageSearchView.Size.SIZE_XGA = 1;
    google.picker.ImageSearchView.Size.SIZE_WXGA = 1;
    google.picker.ImageSearchView.Size.SIZE_WXGA2 = 1;
    google.picker.ImageSearchView.Size.SIZE_2MP = 1;
    google.picker.ImageSearchView.Size.SIZE_4MP = 1;
    google.picker.ImageSearchView.Size.SIZE_6MP = 1;
    google.picker.ImageSearchView.Size.SIZE_8MP = 1;
    google.picker.ImageSearchView.Size.SIZE_10MP = 1;
    google.picker.ImageSearchView.Size.SIZE_12MP = 1;
    google.picker.ImageSearchView.Size.SIZE_15MP = 1;
    google.picker.ImageSearchView.Size.SIZE_20MP = 1;
    google.picker.ImageSearchView.Size.SIZE_40MP = 1;
    google.picker.ImageSearchView.Size.SIZE_70MP = 1;
    google.picker.ImageSearchView.Size.SIZE_140MP = 1;

    google.picker.MapsView = function() {};
    google.picker.MapsView.__super__ = ['google', 'picker', 'View'];
    google.picker.MapsView.prototype.setCenter = function() {};
    google.picker.MapsView.prototype.setZoom = function() {};

    google.picker.PhotoAlbumsView = function() {};
    google.picker.PhotoAlbumsView.__super__ = ['google', 'picker', 'View'];

    google.picker.PhotosView = function() {};
    google.picker.PhotosView.__super__ = ['google', 'picker', 'View'];
    google.picker.PhotosView.prototype.setType = function() {};

    google.picker.PhotosView.Type = {};
    google.picker.PhotosView.Type.FEATURED = 1;
    google.picker.PhotosView.Type.UPLOADED = 1;

    var SECRET = {};

    google.picker.Picker = function() {
      if (arguments[0] !== SECRET) { throw new TypeError(); }
      this.v = arguments[1];
    };
    google.picker.Picker.__super__ = Object;
    google.picker.Picker.__subst__ = true;
    google.picker.Picker.prototype.isVisible = function() {
      return this.v.isVisible();
    };
    google.picker.Picker.prototype.setCallback = function(c) {
      this.v.setCallback(c);
    };
    google.picker.Picker.prototype.setRelayUrl = function(u) {
      this.v.setRelayUrl(u);
    };
    google.picker.Picker.prototype.setVisible = function(b) {
      this.v.setVisible(b);
    };

/*
    google.picker.PickerBuilder = function() {};
    google.picker.PickerBuilder.__super__ = Object;
    google.picker.PickerBuilder.prototype.addView = function() {};
    google.picker.PickerBuilder.prototype.addViewGroup = function() {};
    google.picker.PickerBuilder.prototype.build = function() {};
    google.picker.PickerBuilder.prototype.disableFeature = function() {};
    google.picker.PickerBuilder.prototype.enableFeature = function() {};
    google.picker.PickerBuilder.prototype.getRelayUrl = function() {};
    google.picker.PickerBuilder.prototype.getTitle = function() {};
    google.picker.PickerBuilder.prototype.hideTitleBar = function() {};
    google.picker.PickerBuilder.prototype.isFeatureEnabled = function() {};
    google.picker.PickerBuilder.prototype.setAppId = function() {};
    google.picker.PickerBuilder.prototype.setAuthUser = function() {};
    google.picker.PickerBuilder.prototype.setCallback = function() {};
    google.picker.PickerBuilder.prototype.setDocument = function() {};
    google.picker.PickerBuilder.prototype.setLocale = function() {};
    google.picker.PickerBuilder.prototype.setRelayUrl = function() {};
    google.picker.PickerBuilder.prototype.setSelectableMimeTypes = function() {};
    google.picker.PickerBuilder.prototype.setSize = function() {};
    google.picker.PickerBuilder.prototype.setTitle = function() {};  // TODO: Add "trusted path" annotation
    google.picker.PickerBuilder.prototype.setUploadToAlbumId = function() {};
    google.picker.PickerBuilder.prototype.toUri = function() {};
*/

    google.picker.PickerBuilder = function() {
      this.v = new window.google.picker.PickerBuilder();
    };
    google.picker.PickerBuilder.__subst__ = true;
    google.picker.PickerBuilder.__super__ = Object;
    google.picker.PickerBuilder.prototype.addView = function() {
      this.v.addView.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.addViewGroup = function() {
      this.v.addViewGroup.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.disableFeature = function() {
      this.v.disableFeature.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.enableFeature = function() {
      this.v.enableFeature.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.getRelayUrl = function() {
      this.v.getRelayUrl.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.getTitle = function() {
      this.v.getTitle.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.hideTitleBar = function() {
      this.v.hideTitleBar.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.isFeatureEnabled = function() {
      this.v.isFeatureEnabled.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.setAppId = function() {
      this.v.setAppId.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.setAuthUser = function() {
      this.v.setAuthUser.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.setCallback = function() {
      this.v.setCallback.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.setDocument = function() {
      this.v.setDocument.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.setLocale = function() {
      this.v.setLocale.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.setRelayUrl = function() {
      this.v.setRelayUrl.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.setSelectableMimeTypes = function() {
      this.v.setSelectableMimeTypes.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.setSize = function() {
      this.v.setSize.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.setTitle = function() {
      this.v.setTitle.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.setUploadToAlbumId = function() {
      this.v.setUploadToAlbumId.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.toUri = function() {
      this.v.toUri.apply(this.v, arguments);
      return this;
    };
    google.picker.PickerBuilder.prototype.build = function() {
      return new google.picker.Picker(SECRET, this.v.build.apply(this.v, arguments));
    };    

    google.picker.ResourceId = {};
    google.picker.ResourceId.generate = function() {};

    google.picker.VideoSearchView = function() {};
    google.picker.VideoSearchView.__super__ = ['google', 'picker', 'View'];
    google.picker.VideoSearchView.prototype.setSite = function() {};

    google.picker.VideoSearchView.YOUTUBE = 1;

    google.picker.ViewGroup = function() {};
    google.picker.ViewGroup.__super__ = Object;
    google.picker.ViewGroup.prototype.addLabel = function() {};
    google.picker.ViewGroup.prototype.addView = function() {};
    google.picker.ViewGroup.prototype.addViewGroup = function() {};

    google.picker.ViewId = {};
    google.picker.ViewId.DOCS = 1;
    google.picker.ViewId.DOCS_IMAGES = 1;
    google.picker.ViewId.DOCS_IMAGES_AND_VIDEOS = 1;
    google.picker.ViewId.DOCS_VIDEOS = 1;
    google.picker.ViewId.DOCUMENTS = 1;
    google.picker.ViewId.FOLDERS = 1;
    google.picker.ViewId.FORMS = 1;
    google.picker.ViewId.IMAGE_SEARCH = 1;
    google.picker.ViewId.PDFS = 1;
    google.picker.ViewId.PHOTO_ALBUMS = 1;
    google.picker.ViewId.PHOTO_UPLOAD = 1;
    google.picker.ViewId.PHOTOS = 1;
    google.picker.ViewId.PRESENTATIONS = 1;
    google.picker.ViewId.RECENTLY_PICKED = 1;
    google.picker.ViewId.SPREADSHEETS = 1;
    google.picker.ViewId.VIDEO_SEARCH = 1;
    google.picker.ViewId.WEBCAM = 1;
    google.picker.ViewId.YOUTUBE = 1;

    google.picker.WebCamView = function() {};
    google.picker.WebCamView.__super__ = ['google', 'picker', 'View'];

    google.picker.WebCamViewType = {};
    google.picker.WebCamViewType.STANDARD = 1;
    google.picker.WebCamViewType.VIDEOS = 1;

    google.picker.Action = {};
    google.picker.Action.CANCEL = 1;
    google.picker.Action.PICKED = 1;

    google.picker.Audience = {};
    google.picker.Audience.OWNER_ONLY = 1;
    google.picker.Audience.LIMITED = 1;
    google.picker.Audience.ALL_PERSONAL_CIRCLES = 1;
    google.picker.Audience.EXTENDED_CIRCLES = 1;
    google.picker.Audience.DOMAIN_PUBLIC = 1;
    google.picker.Audience.PUBLIC = 1;

    google.picker.Document = {};
    google.picker.Document.ADDRESS_LINES = 1;
    google.picker.Document.AUDIENCE = 1;
    google.picker.Document.DESCRIPTION = 1;
    google.picker.Document.DURATION = 1;
    google.picker.Document.EMBEDDABLE_URL = 1;
    google.picker.Document.ICON_URL = 1;
    google.picker.Document.ID = 1;
    google.picker.Document.IS_NEW = 1;
    google.picker.Document.LAST_EDITED_UTC = 1;
    google.picker.Document.LATITUDE = 1;
    google.picker.Document.LONGITUDE = 1;
    google.picker.Document.MIME_TYPE = 1;
    google.picker.Document.NAME = 1;
    google.picker.Document.NUM_CHILDREN = 1;
    google.picker.Document.PARENT_ID = 1;
    google.picker.Document.PHONE_NUMBERS = 1;
    google.picker.Document.SERVICE_ID = 1;
    google.picker.Document.THUMBNAILS = 1;
    google.picker.Document.TYPE = 1;
    google.picker.Document.URL = 1;

    google.picker.Response = {};
    google.picker.Response.ACTION = 1;
    google.picker.Response.DOCUMENTS = 1;
    google.picker.Response.PARENTS = 1;
    google.picker.Response.VIEW = 1;

    google.picker.ServiceId = {};
    google.picker.ServiceId.DOCS = 1;
    google.picker.ServiceId.MAPS = 1;
    google.picker.ServiceId.PHOTOS = 1;
    google.picker.ServiceId.SEARCH_API = 1;
    google.picker.ServiceId.URL = 1;
    google.picker.ServiceId.YOUTUBE = 1;

    google.picker.Thumbnail = {};
    google.picker.Thumbnail.HEIGHT = 1;
    google.picker.Thumbnail.WIDTH = 1;
    google.picker.Thumbnail.URL = 1;

    google.picker.Type = {};
    google.picker.Type.ALBUM = 1;
    google.picker.Type.DOCUMENT = 1;
    google.picker.Type.PHOTO = 1;
    google.picker.Type.URL = 1;
    google.picker.Type.VIDEO = 1;

    ////////////////////////////////////////////////////////////////////////

    google.setOnLoadCallback = function(olc) {
      throw 'Cannot set onLoadCallback once modules loaded';
    }
    google.setOnLoadCallback.__subst__ = true;

    return google;
  })();

  function copyJson(o) {
    if (!o) { return undefined; }
    return JSON.parse(JSON.stringify(o, function(key, value) {
      return /__$/.test(key) ? void 0 : value;
    }));
  }

  function opaqueNode(guestNode) {
    var d = guestNode.ownerDocument.createElement('div');
    frame.imports.tameNodeAsForeign___(d);
    guestNode.appendChild(d);
    return d;
  }

  function forallkeys(obj, cb) {
    for (var k in obj) {
      if (!/.*__$/.test(k)) {
        cb(k);
      }
    }
  }

  function targ(obj, policy) {
    return policy.__subst__ ? policy : obj;
  }

  ////////////////////////////////////////////////////////////////////////
  
  function grantRead(o, k) {
    if (o[k + '__grantRead__']) { return; }
    console.log('  + grantRead');
    caja.grantRead(o, k);
    o[k + '__grantRead__'] = true;
  }

  function grantMethod(o, k) {
    if (o[k + '__grantMethod__']) { return; }
    caja.grantMethod(o, k);
    console.log('  + grantMethod');
    o[k + '__grantMethod__'] = true;
  }

  function markFunction(o) {
    if (o.__markFunction__) { return o; }
    var r = caja.markFunction(o);
    console.log('  + markFunction');
    o.__markFunction__ = true;
    return r;
  }

  function markCtor(o, sup) {
    if (o.__markCtor__) { return o; }
    var r = caja.markCtor(o, sup);
    console.log('  + markCtor');
    o.__markCtor__ = true;
    return r;
  }

  function adviseFunctionBefore(o, advices) {
    if (o.__adviseFunctionBefore__) { return o; }
    for (var i = 0; i < advices.length; i++) {
      caja.adviseFunctionBefore(o, advices[i]);
    }
    console.log('  + adviseFunctionBefore');
    return o;
  }

  ////////////////////////////////////////////////////////////////////////

  function defCtor(path, obj, policy) {
    console.log(path + ' defCtor');
    forallkeys(policy, function(name) {
      if (!obj[name]) {
        console.log(path + '.' + name + ' skip');
        return;
      }
      console.log(path + '.' + name + ' grant static');
      grantRead(obj, name);
      if (typeof policy[name] === 'function') {
        markFunction(obj[name]);
      }
    });
    forallkeys(policy.prototype, function(name) {
      if (!obj.prototype[name]) {
        console.log(path + '.prototype.' + name + ' skip');
        return;
      }
      console.log(path + '.prototype.' + name + ' grant instance');
      if (typeof policy.prototype[name] === 'function') {
        if (policy.prototype[name].__before__) {
          adviseFunctionBefore(obj.prototype[name], policy.prototype[name].__before__);
        }
        grantMethod(obj.prototype, name);
      } else {
        grantRead(obj.prototype, name);
      }
    });
    var sup;
    if (policy.__super__ === Object) {
      sup = Object;
    } else {
      sup = window;
      for (var i = 0; i < policy.__super__.length; i++) {
        sup = sup[policy.__super__[i]];
      }
    }

    if (obj.__before__) {
      adviseFunctionBefore(obj, obj.__before__);
    }

    return markCtor(obj, sup);
  }

  function defFcn(path, obj, policy) {
    console.log(path + ' defFcn');
    if (obj.__before__) {
      adviseFunctionBefore(obj, obj.__before__);
    }
    return markFunction(obj);
  }

  function defObj(path, obj, policy) {
    console.log(path + ' defObj');
    var r = {};
    forallkeys(policy, function(name) {
      var sub_obj = obj[name];
      if (!sub_obj) {
        console.log(path + '.' + name + ' skip');
        return;
      }
      var sub_policy = policy[name];
      var sub_path = path + '.' + name;
      var t_sub_policy = typeof sub_policy;
      if (t_sub_policy === 'function') {
        if (sub_policy.__super__) {
          r[name] = defCtor(sub_path, targ(sub_obj, sub_policy), sub_policy);
        } else {
          r[name] = defFcn(sub_path, targ(sub_obj, sub_policy), sub_policy);
        }
      } else if (t_sub_policy === 'object'){
        r[name] = defObj(sub_path, targ(sub_obj, sub_policy), sub_policy);
      } else {
        console.log(path + '.' + name + ' grant static');
        r[name] = targ(sub_obj, sub_policy);
        grantRead(r, name);
      }
    });
    return caja.markReadOnlyRecord(r);
  }

  ////////////////////////////////////////////////////////////////////////

  return defObj('google', window['google'], googlePolicy);
}