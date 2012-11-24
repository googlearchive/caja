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
 * Policy factory for "google.maps" API.
 *
 * @author ihab.awad@gmail.com
 * @requires caja
 * @overrides window
 */
caja.tamingGoogleLoader.addPolicyFactory('google.maps', function(frame, utils) {

  function copyLocation(o) {
    if (typeof o === 'string') { return o; }
    return new window.google.maps.LatLng(o.lat(), o.lng());
  }

  function copyLocationArray(r, o, p) {
    if (o[p] && o[p].length > 0) {
      r[p] = [];
      for (var i = 0; i < o[p].length; i++) {
        r[p].push(copyLocation(o[p][i]));
      }
    }
  }

  function resolve(base, url) {
    var URI = caja.iframe.contentWindow.URI;
    return '' + URI.resolve(URI.parse(base), URI.parse(url));
  }

  var google = {};
  google.maps = {};

/*
 * Copyright 2010 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Externs for the Google Maps v3 API.
 * @see http://code.google.com/apis/maps/documentation/javascript/reference.html
 * @externs
 */

google.maps = {};

/**
 * @enum {number|string}
 */
google.maps.Animation = {
  BOUNCE: 1,
  DROP: 1
};

/**
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.BicyclingLayer = function() {};
google.maps.BicyclingLayer.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.BicyclingLayer.prototype.getMap = function() {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.BicyclingLayer.prototype.setMap = function(map) {};

/**
 * @param {(google.maps.CircleOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.Circle = function(opt_opts) {};
google.maps.Circle.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.LatLngBounds}
 */
google.maps.Circle.prototype.getBounds = function() {};

/**
 * @nosideeffects
 * @return {google.maps.LatLng}
 */
google.maps.Circle.prototype.getCenter = function() {};

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Circle.prototype.getEditable = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.Circle.prototype.getMap = function() {};

/**
 * @nosideeffects
 * @return {number}
 */
google.maps.Circle.prototype.getRadius = function() {};

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Circle.prototype.getVisible = function() {};

/**
 * @param {google.maps.LatLng} center
 * @return {undefined}
 */
google.maps.Circle.prototype.setCenter = function(center) {};

/**
 * @param {boolean} editable
 * @return {undefined}
 */
google.maps.Circle.prototype.setEditable = function(editable) {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.Circle.prototype.setMap = function(map) {};

/**
 * @param {google.maps.CircleOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.Circle.prototype.setOptions = function(options) {};

/**
 * @param {number} radius
 * @return {undefined}
 */
google.maps.Circle.prototype.setRadius = function(radius) {};

/**
 * @param {boolean} visible
 * @return {undefined}
 */
google.maps.Circle.prototype.setVisible = function(visible) {};

/**
 * @constructor
 */
google.maps.CircleOptions = function() {};
google.maps.CircleOptions.__super__ = Object;

/**
 * @type {google.maps.LatLng}
 */
google.maps.CircleOptions.prototype.center = 1;

/**
 * @type {boolean}
 */
google.maps.CircleOptions.prototype.clickable = 1;

/**
 * @type {boolean}
 */
google.maps.CircleOptions.prototype.editable = 1;

/**
 * @type {string}
 */
google.maps.CircleOptions.prototype.fillColor = 1;

/**
 * @type {number}
 */
google.maps.CircleOptions.prototype.fillOpacity = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.CircleOptions.prototype.map = 1;

/**
 * @type {number}
 */
google.maps.CircleOptions.prototype.radius = 1;

/**
 * @type {string}
 */
google.maps.CircleOptions.prototype.strokeColor = 1;

/**
 * @type {number}
 */
google.maps.CircleOptions.prototype.strokeOpacity = 1;

/**
 * @type {number}
 */
google.maps.CircleOptions.prototype.strokeWeight = 1;

/**
 * @type {boolean}
 */
google.maps.CircleOptions.prototype.visible = 1;

/**
 * @type {number}
 */
google.maps.CircleOptions.prototype.zIndex = 1;

/**
 * @enum {number|string}
 */
google.maps.ControlPosition = {
  BOTTOM: 1,  // Not listed in API but part of demos
  BOTTOM_CENTER: 1,
  BOTTOM_LEFT: 1,
  BOTTOM_RIGHT: 1,
  LEFT_BOTTOM: 1,
  LEFT_CENTER: 1,
  LEFT_TOP: 1,
  RIGHT_BOTTOM: 1,
  RIGHT_CENTER: 1,
  RIGHT_TOP: 1,
  TOP_CENTER: 1,
  TOP_LEFT: 1,
  TOP_RIGHT: 1
};


/**
 * @enum
 *
 * TODO(ihab.awad): This was missing from the original Maps externs; what
 * other keys are there in this enumeration?
 */
google.maps.DirectionsTravelMode = {
  BICYCLING: 1,
  DRIVING: 1,
  /* TRANSIT: 1, */
  WALKING: 1
};

/**
 * @constructor
 */
google.maps.DirectionsLeg = function() {};
google.maps.DirectionsLeg.__super__ = Object;

/**
 * @type {google.maps.Distance}
 */
google.maps.DirectionsLeg.prototype.arrival_time = 1;

/**
 * @type {google.maps.Duration}
 */
google.maps.DirectionsLeg.prototype.departure_time = 1;

/**
 * @type {google.maps.Distance}
 */
google.maps.DirectionsLeg.prototype.distance = 1;

/**
 * @type {google.maps.Duration}
 */
google.maps.DirectionsLeg.prototype.duration = 1;

/**
 * @type {string}
 */
google.maps.DirectionsLeg.prototype.end_address = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.DirectionsLeg.prototype.end_location = 1;

/**
 * @type {string}
 */
google.maps.DirectionsLeg.prototype.start_address = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.DirectionsLeg.prototype.start_location = 1;

/**
 * @type {Array.<google.maps.DirectionsStep>}
 */
google.maps.DirectionsLeg.prototype.steps = 1;

/**
 * @type {Array.<google.maps.LatLng>}
 */
google.maps.DirectionsLeg.prototype.via_waypoints = 1;

/**
 * @param {(google.maps.DirectionsRendererOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.DirectionsRenderer = function(opt_opts) {};
google.maps.DirectionsRenderer.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.DirectionsResult}
 */
google.maps.DirectionsRenderer.prototype.getDirections = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.DirectionsRenderer.prototype.getMap = function() {};

/**
 * @nosideeffects
 * @return {Node}
 */
google.maps.DirectionsRenderer.prototype.getPanel = function() {};

/**
 * @nosideeffects
 * @return {number}
 */
google.maps.DirectionsRenderer.prototype.getRouteIndex = function() {};

/**
 * @param {google.maps.DirectionsResult} directions
 * @return {undefined}
 */
google.maps.DirectionsRenderer.prototype.setDirections =
    function(directions) {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.DirectionsRenderer.prototype.setMap = function(map) {};

/**
 * @param {google.maps.DirectionsRendererOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.DirectionsRenderer.prototype.setOptions = function(options) {};

/**
 * @param {Node} panel
 * @return {undefined}
 */
google.maps.DirectionsRenderer.prototype.setPanel = function(panel) {};

/**
 * @param {number} routeIndex
 * @return {undefined}
 */
google.maps.DirectionsRenderer.prototype.setRouteIndex =
    function(routeIndex) {};

/**
 * @constructor
 */
google.maps.DirectionsRendererOptions = function() {};
google.maps.DirectionsRendererOptions.__super__ = Object;

/**
 * @type {google.maps.DirectionsResult}
 */
google.maps.DirectionsRendererOptions.prototype.directions = 1;

/**
 * @type {boolean}
 */
google.maps.DirectionsRendererOptions.prototype.draggable = 1;

/**
 * @type {boolean}
 */
google.maps.DirectionsRendererOptions.prototype.hideRouteList = 1;

/**
 * @type {google.maps.InfoWindow}
 */
google.maps.DirectionsRendererOptions.prototype.infoWindow = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.DirectionsRendererOptions.prototype.map = 1;

/**
 * @type {google.maps.MarkerOptions|Object.<string>}
 */
google.maps.DirectionsRendererOptions.prototype.markerOptions = 1;

/**
 * @type {Node}
 */
google.maps.DirectionsRendererOptions.prototype.panel = 1;

/**
 * @type {google.maps.PolylineOptions|Object.<string>}
 */
google.maps.DirectionsRendererOptions.prototype.polylineOptions = 1;

/**
 * @type {boolean}
 */
google.maps.DirectionsRendererOptions.prototype.preserveViewport = 1;

/**
 * @type {number}
 */
google.maps.DirectionsRendererOptions.prototype.routeIndex = 1;

/**
 * @type {boolean}
 */
google.maps.DirectionsRendererOptions.prototype.suppressBicyclingLayer = 1;

/**
 * @type {boolean}
 */
google.maps.DirectionsRendererOptions.prototype.suppressInfoWindows = 1;

/**
 * @type {boolean}
 */
google.maps.DirectionsRendererOptions.prototype.suppressMarkers = 1;

/**
 * @type {boolean}
 */
google.maps.DirectionsRendererOptions.prototype.suppressPolylines = 1;

/**
 * @constructor
 */
google.maps.DirectionsRequest = function() {};
google.maps.DirectionsRequest.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.DirectionsRequest.prototype.avoidHighways = 1;

/**
 * @type {boolean}
 */
google.maps.DirectionsRequest.prototype.avoidTolls = 1;

/**
 * @type {google.maps.LatLng|string}
 */
google.maps.DirectionsRequest.prototype.destination = 1;

/**
 * @type {boolean}
 */
google.maps.DirectionsRequest.prototype.optimizeWaypoints = 1;

/**
 * @type {google.maps.LatLng|string}
 */
google.maps.DirectionsRequest.prototype.origin = 1;

/**
 * @type {boolean}
 */
google.maps.DirectionsRequest.prototype.provideRouteAlternatives = 1;

/**
 * @type {string}
 */
google.maps.DirectionsRequest.prototype.region = 1;

/**
 * @type {google.maps.TransitOptions|Object.<string>}
 */
google.maps.DirectionsRequest.prototype.transitOptions = 1;

/**
 * @type {google.maps.TravelMode}
 */
google.maps.DirectionsRequest.prototype.travelMode = 1;

/**
 * @type {google.maps.UnitSystem}
 */
google.maps.DirectionsRequest.prototype.unitSystem = 1;

/**
 * @type {Array.<google.maps.DirectionsWaypoint>}
 */
google.maps.DirectionsRequest.prototype.waypoints = 1;

/**
 * @constructor
 */
google.maps.DirectionsResult = function() {};
google.maps.DirectionsResult.__super__ = Object;

/**
 * @type {Array.<google.maps.DirectionsRoute>}
 */
google.maps.DirectionsResult.prototype.routes = 1;

/**
 * @constructor
 */
google.maps.DirectionsRoute = function() {};
google.maps.DirectionsRoute.__super__ = Object;

/**
 * @type {google.maps.LatLngBounds}
 */
google.maps.DirectionsRoute.prototype.bounds = 1;

/**
 * @type {string}
 */
google.maps.DirectionsRoute.prototype.copyrights = 1;

/**
 * @type {Array.<google.maps.DirectionsLeg>}
 */
google.maps.DirectionsRoute.prototype.legs = 1;

/**
 * @type {Array.<google.maps.LatLng>}
 */
google.maps.DirectionsRoute.prototype.overview_path = 1;

/**
 * @type {Array.<string>}
 */
google.maps.DirectionsRoute.prototype.warnings = 1;

/**
 * @type {Array.<number>}
 */
google.maps.DirectionsRoute.prototype.waypoint_order = 1;

/**
 * @constructor
 */
google.maps.DirectionsService = function() {};
google.maps.DirectionsService.__super__ = Object;

/**
 * @param {google.maps.DirectionsRequest|Object.<string>} request
 * @param {function(google.maps.DirectionsResult, google.maps.DirectionsStatus)} callback
 * @return {undefined}
 */
google.maps.DirectionsService.prototype.route = function(request, callback) {};
google.maps.DirectionsService.prototype.route.__before__ = [
  utils.mapArgs(
    copyDirectionsServiceRouteRequest,
    utils.identity)
];

function copyDirectionsServiceRouteRequest(o) {
  var r = {};
  utils.directCopy(r, o, [
    'avoidHighways',
    'avoidTolls',
    'destination',
    'optimizeWaypoints',
    'origin',
    'provideRouteAlternatives',
    'region',
    'transitOptions',
    'travelMode',
    'unitSystem'
  ]);
  if (o.waypoints && o.waypoints.length > 0) {
    r.waypoints = [];
    for (var i = 0; i < o.waypoints.length; i++) {
      var w = {};
      utils.directCopy(w, o.waypoints[i], ['location', 'stopover']);
      r.waypoints.push(w);
    }
  }
  return r;
}


/**
 * @enum {number|string}
 */
google.maps.DirectionsStatus = {
  INVALID_REQUEST: 1,
  MAX_WAYPOINTS_EXCEEDED: 1,
  NOT_FOUND: 1,
  OK: 1,
  OVER_QUERY_LIMIT: 1,
  REQUEST_DENIED: 1,
  UNKNOWN_ERROR: 1,
  ZERO_RESULTS: 1
};

/**
 * @constructor
 */
google.maps.DirectionsStep = function() {};
google.maps.DirectionsStep.__super__ = Object;

/**
 * @type {google.maps.Distance}
 */
google.maps.DirectionsStep.prototype.distance = 1;

/**
 * @type {google.maps.Duration}
 */
google.maps.DirectionsStep.prototype.duration = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.DirectionsStep.prototype.end_location = 1;

/**
 * @type {string}
 */
google.maps.DirectionsStep.prototype.instructions = 1;

/**
 * @type {Array.<google.maps.LatLng>}
 */
google.maps.DirectionsStep.prototype.path = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.DirectionsStep.prototype.start_location = 1;

/**
 * @type {google.maps.DirectionsStep}
 */
google.maps.DirectionsStep.prototype.steps = 1;

/**
 * @type {google.maps.TransitDetails}
 */
google.maps.DirectionsStep.prototype.transit = 1;

/**
 * @type {google.maps.TravelMode}
 */
google.maps.DirectionsStep.prototype.travel_mode = 1;

/**
 * @constructor
 */
google.maps.DirectionsWaypoint = function() {};
google.maps.DirectionsWaypoint.__super__ = Object;

/**
 * @type {google.maps.LatLng|string}
 */
google.maps.DirectionsWaypoint.prototype.location = 1;

/**
 * @type {boolean}
 */
google.maps.DirectionsWaypoint.prototype.stopover = 1;

/**
 * @constructor
 */
google.maps.Distance = function() {};
google.maps.Distance.__super__ = Object;

/**
 * @type {string}
 */
google.maps.Distance.prototype.text = 1;

/**
 * @type {number}
 */
google.maps.Distance.prototype.value = 1;

/**
 * @enum {number|string}
 */
google.maps.DistanceMatrixElementStatus = {
  NOT_FOUND: 1,
  OK: 1,
  ZERO_RESULTS: 1
};

/**
 * @constructor
 */
google.maps.DistanceMatrixRequest = function() {};
google.maps.DistanceMatrixRequest.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.DistanceMatrixRequest.prototype.avoidHighways = 1;

/**
 * @type {boolean}
 */
google.maps.DistanceMatrixRequest.prototype.avoidTolls = 1;

/**
 * @type {Array.<google.maps.LatLng>|Array.<string>}
 */
google.maps.DistanceMatrixRequest.prototype.destinations = 1;

/**
 * @type {Array.<google.maps.LatLng>|Array.<string>}
 */
google.maps.DistanceMatrixRequest.prototype.origins = 1;

/**
 * @type {string}
 */
google.maps.DistanceMatrixRequest.prototype.region = 1;

/**
 * @type {google.maps.TravelMode}
 */
google.maps.DistanceMatrixRequest.prototype.travelMode = 1;

/**
 * @type {google.maps.UnitSystem}
 */
google.maps.DistanceMatrixRequest.prototype.unitSystem = 1;

/**
 * @constructor
 */
google.maps.DistanceMatrixResponse = function() {};
google.maps.DistanceMatrixResponse.__super__ = Object;

/**
 * @type {Array.<string>}
 */
google.maps.DistanceMatrixResponse.prototype.destinationAddresses = 1;

/**
 * @type {Array.<string>}
 */
google.maps.DistanceMatrixResponse.prototype.originAddresses = 1;

/**
 * @type {Array.<google.maps.DistanceMatrixResponseRow>}
 */
google.maps.DistanceMatrixResponse.prototype.rows = 1;

/**
 * @constructor
 */
google.maps.DistanceMatrixResponseElement = function() {};
google.maps.DistanceMatrixResponseElement.__super__ = Object;

/**
 * @type {google.maps.Distance}
 */
google.maps.DistanceMatrixResponseElement.prototype.distance = 1;

/**
 * @type {google.maps.Duration}
 */
google.maps.DistanceMatrixResponseElement.prototype.duration = 1;

/**
 * @type {google.maps.DistanceMatrixElementStatus}
 */
google.maps.DistanceMatrixResponseElement.prototype.status = 1;

/**
 * @constructor
 */
google.maps.DistanceMatrixResponseRow = function() {};
google.maps.DistanceMatrixResponseRow.__super__ = Object;

/**
 * @type {Array.<google.maps.DistanceMatrixResponseElement>}
 */
google.maps.DistanceMatrixResponseRow.prototype.elements = 1;

/**
 * @constructor
 */
google.maps.DistanceMatrixService = function() {};
google.maps.DistanceMatrixService.__super__ = Object;

/**
 * @param {google.maps.DistanceMatrixRequest|Object.<string>} request
 * @param {function(google.maps.DistanceMatrixResponse, google.maps.DistanceMatrixStatus)} callback
 * @return {undefined}
 */
google.maps.DistanceMatrixService.prototype.getDistanceMatrix = function(request, callback) {};
google.maps.DistanceMatrixService.prototype.getDistanceMatrix.__before__ = [
  utils.mapArgs(
    copyDistanceMatrixServiceGetDistanceMatrixRequest,
    utils.identity)
];

function copyDistanceMatrixServiceGetDistanceMatrixRequest(o) {
  var r = {};
  utils.directCopy(r, o, [
    'avoidHighways',
    'avoidTolls',
    'region',
    'travelMode',
    'unitSystem'
  ]);

  copyLocationArray(r, o, 'origins');
  copyLocationArray(r, o, 'destinations');

  return r;
}


/**
 * @enum {number|string}
 */
google.maps.DistanceMatrixStatus = {
  INVALID_REQUEST: 1,
  MAX_DIMENSIONS_EXCEEDED: 1,
  MAX_ELEMENTS_EXCEEDED: 1,
  OK: 1,
  OVER_QUERY_LIMIT: 1,
  REQUEST_DENIED: 1,
  UNKNOWN_ERROR: 1
};

/**
 * @constructor
 */
google.maps.Duration = function() {};
google.maps.Duration.__super__ = Object;

/**
 * @type {string}
 */
google.maps.Duration.prototype.text = 1;

/**
 * @type {number}
 */
google.maps.Duration.prototype.value = 1;

/**
 * @constructor
 */
google.maps.ElevationResult = function() {};
google.maps.ElevationResult.__super__ = Object;

/**
 * @type {number}
 */
google.maps.ElevationResult.prototype.elevation = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.ElevationResult.prototype.location = 1;

/**
 * @type {number}
 */
google.maps.ElevationResult.prototype.resolution = 1;

/**
 * @constructor
 */
google.maps.ElevationService = function() {};
google.maps.ElevationService.__super__ = Object;

/**
 * @param {google.maps.PathElevationRequest|Object.<string>} request
 * @param {function(Array.<google.maps.ElevationResult>, google.maps.ElevationStatus)} callback
 * @return {undefined}
 */
google.maps.ElevationService.prototype.getElevationAlongPath =
    function(request, callback) {};

google.maps.ElevationService.prototype.getElevationAlongPath.__before__ = [
  utils.mapArgs(
    copyElevationServiceGetElevationAlongPathRequest,
    utils.identity)
];

function copyElevationServiceGetElevationAlongPathRequest(o) {
  var r = {};
  utils.directCopy(r, o, ['samples']);
  copyLocationArray(r, o, 'path');
  return r;
}

/**
 * @param {google.maps.LocationElevationRequest|Object.<string>} request
 * @param {function(Array.<google.maps.ElevationResult>, google.maps.ElevationStatus)} callback
 * @return {undefined}
 */
google.maps.ElevationService.prototype.getElevationForLocations =
    function(request, callback) {};
google.maps.ElevationService.prototype.getElevationForLocations.__before__ = [
  utils.mapArgs(
    copyElevationServiceGetElevationForLocationsRequest,
    utils.identity)
];

function copyElevationServiceGetElevationForLocationsRequest(o) {
  var r = {};
  copyLocationArray(r, o, 'path');
  return r;
}

/**
 * @enum {number|string}
 */
google.maps.ElevationStatus = {
  INVALID_REQUEST: 1,
  OK: 1,
  OVER_QUERY_LIMIT: 1,
  REQUEST_DENIED: 1,
  UNKNOWN_ERROR: 1
};

/**
 * @constructor
 */
google.maps.FusionTablesCell = function() {};
google.maps.FusionTablesCell.__super__ = Object;

/**
 * @type {string}
 */
google.maps.FusionTablesCell.prototype.columnName = 1;

/**
 * @type {string}
 */
google.maps.FusionTablesCell.prototype.value = 1;

/**
 * @constructor
 */
google.maps.FusionTablesHeatmap = function() {};
google.maps.FusionTablesHeatmap.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.FusionTablesHeatmap.prototype.enabled = 1;

/**
 * @param {google.maps.FusionTablesLayerOptions|Object.<string>} options
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.FusionTablesLayer = function(options) {};
google.maps.FusionTablesLayer.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.FusionTablesLayer.prototype.getMap = function() {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.FusionTablesLayer.prototype.setMap = function(map) {};

/**
 * @param {google.maps.FusionTablesLayerOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.FusionTablesLayer.prototype.setOptions = function(options) {};

/**
 * @constructor
 */
google.maps.FusionTablesLayerOptions = function() {};
google.maps.FusionTablesLayerOptions.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.FusionTablesLayerOptions.prototype.clickable = 1;

/**
 * @type {google.maps.FusionTablesHeatmap}
 */
google.maps.FusionTablesLayerOptions.prototype.heatmap = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.FusionTablesLayerOptions.prototype.map = 1;

/**
 * @type {google.maps.FusionTablesQuery}
 */
google.maps.FusionTablesLayerOptions.prototype.query = 1;

/**
 * @type {Array.<google.maps.FusionTablesStyle>}
 */
google.maps.FusionTablesLayerOptions.prototype.styles = 1;

/**
 * @type {boolean}
 */
google.maps.FusionTablesLayerOptions.prototype.suppressInfoWindows = 1;

/**
 * @constructor
 */
google.maps.FusionTablesMarkerOptions = function() {};
google.maps.FusionTablesMarkerOptions.__super__ = Object;

/**
 * @type {string}
 */
google.maps.FusionTablesMarkerOptions.prototype.iconName = 1;

/**
 * @constructor
 */
google.maps.FusionTablesMouseEvent = function() {};
google.maps.FusionTablesMouseEvent.__super__ = Object;

/**
 * @type {string}
 */
google.maps.FusionTablesMouseEvent.prototype.infoWindowHtml = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.FusionTablesMouseEvent.prototype.latLng = 1;

/**
 * @type {google.maps.Size}
 */
google.maps.FusionTablesMouseEvent.prototype.pixelOffset = 1;

/**
 * @type {Object}
 */
google.maps.FusionTablesMouseEvent.prototype.row = 1;

/**
 * @constructor
 */
google.maps.FusionTablesPolygonOptions = function() {};
google.maps.FusionTablesPolygonOptions.__super__ = Object;

/**
 * @type {string}
 */
google.maps.FusionTablesPolygonOptions.prototype.fillColor = 1;

/**
 * @type {number}
 */
google.maps.FusionTablesPolygonOptions.prototype.fillOpacity = 1;

/**
 * @type {string}
 */
google.maps.FusionTablesPolygonOptions.prototype.strokeColor = 1;

/**
 * @type {number}
 */
google.maps.FusionTablesPolygonOptions.prototype.strokeOpacity = 1;

/**
 * @type {number}
 */
google.maps.FusionTablesPolygonOptions.prototype.strokeWeight = 1;

/**
 * @constructor
 */
google.maps.FusionTablesPolylineOptions = function() {};
google.maps.FusionTablesPolylineOptions.__super__ = Object;

/**
 * @type {string}
 */
google.maps.FusionTablesPolylineOptions.prototype.strokeColor = 1;

/**
 * @type {number}
 */
google.maps.FusionTablesPolylineOptions.prototype.strokeOpacity = 1;

/**
 * @type {number}
 */
google.maps.FusionTablesPolylineOptions.prototype.strokeWeight = 1;

/**
 * @constructor
 */
google.maps.FusionTablesQuery = function() {};
google.maps.FusionTablesQuery.__super__ = Object;

/**
 * @type {string}
 */
google.maps.FusionTablesQuery.prototype.from = 1;

/**
 * @type {number}
 */
google.maps.FusionTablesQuery.prototype.limit = 1;

/**
 * @type {number}
 */
google.maps.FusionTablesQuery.prototype.offset = 1;

/**
 * @type {string}
 */
google.maps.FusionTablesQuery.prototype.orderBy = 1;

/**
 * @type {string}
 */
google.maps.FusionTablesQuery.prototype.select = 1;

/**
 * @type {string}
 */
google.maps.FusionTablesQuery.prototype.where = 1;

/**
 * @constructor
 */
google.maps.FusionTablesStyle = function() {};
google.maps.FusionTablesStyle.__super__ = Object;

/**
 * @type {google.maps.FusionTablesMarkerOptions|Object.<string>}
 */
google.maps.FusionTablesStyle.prototype.markerOptions = 1;

/**
 * @type {google.maps.FusionTablesPolygonOptions|Object.<string>}
 */
google.maps.FusionTablesStyle.prototype.polygonOptions = 1;

/**
 * @type {google.maps.FusionTablesPolylineOptions|Object.<string>}
 */
google.maps.FusionTablesStyle.prototype.polylineOptions = 1;

/**
 * @type {string}
 */
google.maps.FusionTablesStyle.prototype.where = 1;

/**
 * @constructor
 */
google.maps.Geocoder = function() {};
google.maps.Geocoder.__super__ = Object;

/**
 * @param {google.maps.GeocoderRequest|Object.<string>} request
 * @param {function(Array.<google.maps.GeocoderResult>, google.maps.GeocoderStatus)} callback
 * @return {undefined}
 */
google.maps.Geocoder.prototype.geocode = function(request, callback) {};
google.maps.Geocoder.prototype.geocode.__before__ = [
  utils.mapArgs(
    copyGeocoderGeocodeRequest,
    utils.identity)
];

function copyGeocoderGeocodeRequest(o) {
  var r = {};
  utils.directCopy(r, o, ['address', 'region']);
  if (o.bounds) {
    r.bounds = new window.google.maps.LatLngBounds(
        new window.google.maps.LatLng(
            o.bounds.getSouthWest().lat(),
            o.bounds.getSouthWest().lng()),
        new window.google.maps.LatLng(
            o.bounds.getNorthEast.lat(),
            o.bounds.getNorthEast.lng()));
  }
  if (o.location) {
    r.location = new window.google.maps.LatLng(
        o.location.lat(),
        o.location.lng());
  }
  if (o.latLng) {
    r.latLng = new window.google.maps.LatLng(
        o.latLng.lat(),
        o.latLng.lng());
  }
  return r;
}

/**
 * @constructor
 */
google.maps.GeocoderAddressComponent = function() {};
google.maps.GeocoderAddressComponent.__super__ = Object;

/**
 * @type {string}
 */
google.maps.GeocoderAddressComponent.prototype.long_name = 1;

/**
 * @type {string}
 */
google.maps.GeocoderAddressComponent.prototype.short_name = 1;

/**
 * @type {Array.<string>}
 */
google.maps.GeocoderAddressComponent.prototype.types = 1;

/**
 * @constructor
 */
google.maps.GeocoderGeometry = function() {};
google.maps.GeocoderGeometry.__super__ = Object;

/**
 * @type {google.maps.LatLngBounds}
 */
google.maps.GeocoderGeometry.prototype.bounds = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.GeocoderGeometry.prototype.location = 1;

/**
 * @type {google.maps.GeocoderLocationType}
 */
google.maps.GeocoderGeometry.prototype.location_type = 1;

/**
 * @type {google.maps.LatLngBounds}
 */
google.maps.GeocoderGeometry.prototype.viewport = 1;

/**
 * @enum {number|string}
 */
google.maps.GeocoderLocationType = {
  APPROXIMATE: 1,
  GEOMETRIC_CENTER: 1,
  RANGE_INTERPOLATED: 1,
  ROOFTOP: 1
};

/**
 * @constructor
 */
google.maps.GeocoderRequest = function() {};
google.maps.GeocoderRequest.__super__ = Object;

/**
 * @type {string}
 */
google.maps.GeocoderRequest.prototype.address = 1;

/**
 * @type {google.maps.LatLngBounds}
 */
google.maps.GeocoderRequest.prototype.bounds = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.GeocoderRequest.prototype.location = 1;

/**
 * @type {string}
 */
google.maps.GeocoderRequest.prototype.region = 1;

/**
 * @constructor
 */
google.maps.GeocoderResult = function() {};
google.maps.GeocoderResult.__super__ = Object;

/**
 * @type {Array.<google.maps.GeocoderAddressComponent>}
 */
google.maps.GeocoderResult.prototype.address_components = 1;

/**
 * @type {string}
 */
google.maps.GeocoderResult.prototype.formatted_address = 1;

/**
 * @type {google.maps.GeocoderGeometry}
 */
google.maps.GeocoderResult.prototype.geometry = 1;

/**
 * @type {Array.<string>}
 */
google.maps.GeocoderResult.prototype.types = 1;

/**
 * @enum {number|string}
 */
google.maps.GeocoderStatus = {
  ERROR: 1,
  INVALID_REQUEST: 1,
  OK: 1,
  OVER_QUERY_LIMIT: 1,
  REQUEST_DENIED: 1,
  UNKNOWN_ERROR: 1,
  ZERO_RESULTS: 1
};

/**
 * @param {string} url
 * @param {google.maps.LatLngBounds} bounds
 * @param {(google.maps.GroundOverlayOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.GroundOverlay = function(url, bounds, opt_opts) {};
google.maps.GroundOverlay.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.LatLngBounds}
 */
google.maps.GroundOverlay.prototype.getBounds = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.GroundOverlay.prototype.getMap = function() {};

/**
 * @nosideeffects
 * @return {number}
 */
google.maps.GroundOverlay.prototype.getOpacity = function() {};

/**
 * @nosideeffects
 * @return {string}
 */
google.maps.GroundOverlay.prototype.getUrl = function() {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.GroundOverlay.prototype.setMap = function(map) {};

/**
 * @param {number} opacity
 * @return {undefined}
 */
google.maps.GroundOverlay.prototype.setOpacity = function(opacity) {};

/**
 * @constructor
 */
google.maps.GroundOverlayOptions = function() {};
google.maps.GroundOverlayOptions.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.GroundOverlayOptions.prototype.clickable = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.GroundOverlayOptions.prototype.map = 1;

/**
 * @type {number}
 */
google.maps.GroundOverlayOptions.prototype.opacity = 1;

/**
 * @constructor
 */
google.maps.IconSequence = function() {};
google.maps.IconSequence.__super__ = Object;

/**
 * @type {google.maps.Symbol}
 */
google.maps.IconSequence.prototype.icon = 1;

/**
 * @type {string}
 */
google.maps.IconSequence.prototype.offset = 1;

/**
 * @type {string}
 */
google.maps.IconSequence.prototype.repeat = 1;

/**
 * @param {google.maps.ImageMapTypeOptions|Object.<string>} opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.ImageMapType = function(opts) {};
google.maps.ImageMapType.__super__ = Object;
google.maps.ImageMapType.__before__ = [
  function(f, self, args) {
    var getTileUrl = args[0].getTileUrl;
    if (getTileUrl) {
      args[0].getTileUrl = function(coord, zoom) {
        return resolve(frame.getUrl(), getTileUrl.call({}, coord, zoom));
      };
    }
    return args;
  }
];

/**
 * @nosideeffects
 * @return {number}
 */
google.maps.ImageMapType.prototype.getOpacity = function() {};

/**
 * @param {number} opacity
 * @return {undefined}
 */
google.maps.ImageMapType.prototype.setOpacity = function(opacity) {};

/**
 * @constructor
 */
google.maps.ImageMapTypeOptions = function() {};
google.maps.ImageMapTypeOptions.__super__ = Object;

/**
 * @type {string}
 */
google.maps.ImageMapTypeOptions.prototype.alt = 1;

/**
 * @type {function(google.maps.Point, number):string}
 */
google.maps.ImageMapTypeOptions.prototype.getTileUrl = 1;

/**
 * @type {number}
 */
google.maps.ImageMapTypeOptions.prototype.maxZoom = 1;

/**
 * @type {number}
 */
google.maps.ImageMapTypeOptions.prototype.minZoom = 1;

/**
 * @type {string}
 */
google.maps.ImageMapTypeOptions.prototype.name = 1;

/**
 * @type {number}
 */
google.maps.ImageMapTypeOptions.prototype.opacity = 1;

/**
 * @type {google.maps.Size}
 */
google.maps.ImageMapTypeOptions.prototype.tileSize = 1;

/**
 * @param {(google.maps.InfoWindowOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.InfoWindow = function(opt_opts) {};
google.maps.InfoWindow.__super__ = Object;

/**
 * @return {undefined}
 */
google.maps.InfoWindow.prototype.close = function() {};

/**
 * @nosideeffects
 * @return {string|Node}
 */
google.maps.InfoWindow.prototype.getContent = function() {};

/**
 * @nosideeffects
 * @return {google.maps.LatLng}
 */
google.maps.InfoWindow.prototype.getPosition = function() {};

/**
 * @nosideeffects
 * @return {number}
 */
google.maps.InfoWindow.prototype.getZIndex = function() {};

/**
 * @param {(google.maps.Map|google.maps.StreetViewPanorama)=} opt_map
 * @param {google.maps.MVCObject=} opt_anchor
 * @return {undefined}
 */
google.maps.InfoWindow.prototype.open = function(opt_map, opt_anchor) {};

/**
 * @param {string|Node} content
 * @return {undefined}
 */
google.maps.InfoWindow.prototype.setContent = function(content) {};

/**
 * @param {google.maps.InfoWindowOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.InfoWindow.prototype.setOptions = function(options) {};

/**
 * @param {google.maps.LatLng} position
 * @return {undefined}
 */
google.maps.InfoWindow.prototype.setPosition = function(position) {};

/**
 * @param {number} zIndex
 * @return {undefined}
 */
google.maps.InfoWindow.prototype.setZIndex = function(zIndex) {};

/**
 * @constructor
 */
google.maps.InfoWindowOptions = function() {};
google.maps.InfoWindowOptions.__super__ = Object;

/**
 * @type {string|Node}
 */
google.maps.InfoWindowOptions.prototype.content = 1;

/**
 * @type {boolean}
 */
google.maps.InfoWindowOptions.prototype.disableAutoPan = 1;

/**
 * @type {number}
 */
google.maps.InfoWindowOptions.prototype.maxWidth = 1;

/**
 * @type {google.maps.Size}
 */
google.maps.InfoWindowOptions.prototype.pixelOffset = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.InfoWindowOptions.prototype.position = 1;

/**
 * @type {number}
 */
google.maps.InfoWindowOptions.prototype.zIndex = 1;

/**
 * @constructor
 */
google.maps.KmlAuthor = function() {};
google.maps.KmlAuthor.__super__ = Object;

/**
 * @type {string}
 */
google.maps.KmlAuthor.prototype.email = 1;

/**
 * @type {string}
 */
google.maps.KmlAuthor.prototype.name = 1;

/**
 * @type {string}
 */
google.maps.KmlAuthor.prototype.uri = 1;

/**
 * @constructor
 */
google.maps.KmlFeatureData = function() {};
google.maps.KmlFeatureData.__super__ = Object;

/**
 * @type {google.maps.KmlAuthor}
 */
google.maps.KmlFeatureData.prototype.author = 1;

/**
 * @type {string}
 */
google.maps.KmlFeatureData.prototype.description = 1;

/**
 * @type {string}
 */
google.maps.KmlFeatureData.prototype.id = 1;

/**
 * @type {string}
 */
google.maps.KmlFeatureData.prototype.infoWindowHtml = 1;

/**
 * @type {string}
 */
google.maps.KmlFeatureData.prototype.name = 1;

/**
 * @type {string}
 */
google.maps.KmlFeatureData.prototype.snippet = 1;

/**
 * @param {string} url
 * @param {(google.maps.KmlLayerOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.KmlLayer = function(url, opt_opts) {};
google.maps.KmlLayer.__super__ = Object;
google.maps.KmlLayer.__before__ = [
  function(f, self, args) {
    args = args.slice(0);
    args[0] = frame.getUriPolicy().rewrite('' + args[0]);
    return args;
  }
];

/**
 * @nosideeffects
 * @return {google.maps.LatLngBounds}
 */
google.maps.KmlLayer.prototype.getDefaultViewport = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.KmlLayer.prototype.getMap = function() {};

/**
 * @nosideeffects
 * @return {google.maps.KmlLayerMetadata}
 */
google.maps.KmlLayer.prototype.getMetadata = function() {};

/**
 * @nosideeffects
 * @return {google.maps.KmlLayerStatus}
 */
google.maps.KmlLayer.prototype.getStatus = function() {};

/**
 * @nosideeffects
 * @return {string}
 */
google.maps.KmlLayer.prototype.getUrl = function() {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.KmlLayer.prototype.setMap = function(map) {};

/**
 * @constructor
 */
google.maps.KmlLayerMetadata = function() {};
google.maps.KmlLayerMetadata.__super__ = Object;

/**
 * @type {google.maps.KmlAuthor}
 */
google.maps.KmlLayerMetadata.prototype.author = 1;

/**
 * @type {string}
 */
google.maps.KmlLayerMetadata.prototype.description = 1;

/**
 * @type {string}
 */
google.maps.KmlLayerMetadata.prototype.name = 1;

/**
 * @type {string}
 */
google.maps.KmlLayerMetadata.prototype.snippet = 1;

/**
 * @constructor
 */
google.maps.KmlLayerOptions = function() {};
google.maps.KmlLayerOptions.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.KmlLayerOptions.prototype.clickable = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.KmlLayerOptions.prototype.map = 1;

/**
 * @type {boolean}
 */
google.maps.KmlLayerOptions.prototype.preserveViewport = 1;

/**
 * @type {boolean}
 */
google.maps.KmlLayerOptions.prototype.suppressInfoWindows = 1;

/**
 * @enum {number|string}
 */
google.maps.KmlLayerStatus = {
  DOCUMENT_NOT_FOUND: 1,
  DOCUMENT_TOO_LARGE: 1,
  FETCH_ERROR: 1,
  INVALID_DOCUMENT: 1,
  INVALID_REQUEST: 1,
  LIMITS_EXCEEDED: 1,
  OK: 1,
  TIMED_OUT: 1,
  UNKNOWN: 1
};

/**
 * @constructor
 */
google.maps.KmlMouseEvent = function() {};
google.maps.KmlMouseEvent.__super__ = Object;

/**
 * @type {google.maps.KmlFeatureData}
 */
google.maps.KmlMouseEvent.prototype.featureData = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.KmlMouseEvent.prototype.latLng = 1;

/**
 * @type {google.maps.Size}
 */
google.maps.KmlMouseEvent.prototype.pixelOffset = 1;

/**
 * @param {number} lat
 * @param {number} lng
 * @param {boolean=} opt_noWrap
 * @constructor
 */
google.maps.LatLng = function(lat, lng, opt_noWrap) {};
google.maps.LatLng.__super__ = Object;

/**
 * @param {google.maps.LatLng} other
 * @return {boolean}
 */
google.maps.LatLng.prototype.equals = function(other) {};

/**
 * @return {number}
 */
google.maps.LatLng.prototype.lat = function() {};

/**
 * @return {number}
 */
google.maps.LatLng.prototype.lng = function() {};

/**
 * @return {string}
 */
/* google.maps.LatLng.prototype.toString = function() {}; */

/**
 * @param {number=} opt_precision
 * @return {string}
 */
google.maps.LatLng.prototype.toUrlValue = function(opt_precision) {};

google.maps.LatLng.prototype.toString = function() {};

/**
 * @param {google.maps.LatLng=} opt_sw
 * @param {google.maps.LatLng=} opt_ne
 * @constructor
 */
google.maps.LatLngBounds = function(opt_sw, opt_ne) {};
google.maps.LatLngBounds.__super__ = Object;

/**
 * @param {google.maps.LatLng} latLng
 * @return {boolean}
 */
google.maps.LatLngBounds.prototype.contains = function(latLng) {};

/**
 * @param {google.maps.LatLngBounds} other
 * @return {boolean}
 */
google.maps.LatLngBounds.prototype.equals = function(other) {};

/**
 * @param {google.maps.LatLng} point
 * @return {google.maps.LatLngBounds}
 */
google.maps.LatLngBounds.prototype.extend = function(point) {};

/**
 * @nosideeffects
 * @return {google.maps.LatLng}
 */
google.maps.LatLngBounds.prototype.getCenter = function() {};

/**
 * @nosideeffects
 * @return {google.maps.LatLng}
 */
google.maps.LatLngBounds.prototype.getNorthEast = function() {};

/**
 * @nosideeffects
 * @return {google.maps.LatLng}
 */
google.maps.LatLngBounds.prototype.getSouthWest = function() {};

/**
 * @param {google.maps.LatLngBounds} other
 * @return {boolean}
 */
google.maps.LatLngBounds.prototype.intersects = function(other) {};

/**
 * @return {boolean}
 */
google.maps.LatLngBounds.prototype.isEmpty = function() {};

/**
 * @return {google.maps.LatLng}
 */
google.maps.LatLngBounds.prototype.toSpan = function() {};

/**
 * @return {string}
 */
/* google.maps.LatLngBounds.prototype.toString = function() {}; */

/**
 * @param {number=} opt_precision
 * @return {string}
 */
google.maps.LatLngBounds.prototype.toUrlValue = function(opt_precision) {};

/**
 * @param {google.maps.LatLngBounds} other
 * @return {google.maps.LatLngBounds}
 */
google.maps.LatLngBounds.prototype.union = function(other) {};

/**
 * @constructor
 */
google.maps.LocationElevationRequest = function() {};
google.maps.LocationElevationRequest.__super__ = Object;

/**
 * @type {Array.<google.maps.LatLng>}
 */
google.maps.LocationElevationRequest.prototype.locations = 1;

/**
 * @param {Array=} opt_array
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.MVCArray = function(opt_array) {};
google.maps.MVCArray.__super__ = ['google', 'maps', 'MVCObject'];

/*
 * @type {number}
 */
// CAJA NOTE -- we cannot (in es5/3) whitelist '.length' as a property
// because of our array index optimization
// TODO(ihab.awad): Revisit once we've pushed ES5 everywhere
// google.maps.MVCArray.prototype.length = 1;

/**
 * @return {undefined}
 */
google.maps.MVCArray.prototype.clear = function() {};

/**
 * @param {function(?, number)} callback
 * @return {undefined}
 */
google.maps.MVCArray.prototype.forEach = function(callback) {};

/**
 * @nosideeffects
 * @return {Array}
 */
google.maps.MVCArray.prototype.getArray = function() {};

/**
 * @param {number} i
 * @return {*}
 */
google.maps.MVCArray.prototype.getAt = function(i) {};

/**
 * @nosideeffects
 * @return {number}
 */
google.maps.MVCArray.prototype.getLength = function() {};

/**
 * @param {number} i
 * @param {*} elem
 * @return {undefined}
 */
google.maps.MVCArray.prototype.insertAt = function(i, elem) {};

/**
 * @return {*}
 */
google.maps.MVCArray.prototype.pop = function() {};

/**
 * @param {*} elem
 * @return {number}
 */
google.maps.MVCArray.prototype.push = function(elem) {};

/**
 * @param {number} i
 * @return {*}
 */
google.maps.MVCArray.prototype.removeAt = function(i) {};

/**
 * @param {number} i
 * @param {*} elem
 * @return {undefined}
 */
google.maps.MVCArray.prototype.setAt = function(i, elem) {};

/**
 * @constructor
 */
google.maps.MVCObject = function() {};
google.maps.MVCObject.__super__ = Object;

/**
 * @param {string} key
 * @param {google.maps.MVCObject} target
 * @param {?string=} opt_targetKey
 * @param {boolean=} opt_noNotify
 * @return {undefined}
 */
google.maps.MVCObject.prototype.bindTo = function(key, target, opt_targetKey, opt_noNotify) {};

/**
 * @param {string} key
 * @return {undefined}
 */
google.maps.MVCObject.prototype.changed = function(key) {};

/**
 * @param {string} key
 * @return {*}
 */
google.maps.MVCObject.prototype.get = function(key) {};

/**
 * @param {string} key
 * @return {undefined}
 */
google.maps.MVCObject.prototype.notify = function(key) {};

/**
 * @param {string} key
 * @param {?} value
 * @return {undefined}
 */
google.maps.MVCObject.prototype.set = function(key, value) {};

/**
 * @param {Object|undefined} values
 * @return {undefined}
 */
google.maps.MVCObject.prototype.setValues = function(values) {};

/**
 * @param {string} key
 * @return {undefined}
 */
google.maps.MVCObject.prototype.unbind = function(key) {};

/**
 * @return {undefined}
 */
google.maps.MVCObject.prototype.unbindAll = function() {};

/**
 * @param {Node} mapDiv
 * @param {(google.maps.MapOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.Map = function(mapDiv, opt_opts) {};
google.maps.Map.__super__ = Object;

/**
 * @type {Array.<google.maps.MVCArray.<Node>>}
 */
google.maps.Map.prototype.controls = 1;

/**
 * @type {google.maps.MapTypeRegistry}
 */
google.maps.Map.prototype.mapTypes = 1;

/**
 * @type {google.maps.MVCArray.<google.maps.MapType>}
 */
google.maps.Map.prototype.overlayMapTypes = 1;

/**
 * @param {google.maps.LatLngBounds} bounds
 * @return {undefined}
 */
google.maps.Map.prototype.fitBounds = function(bounds) {};

/**
 * @nosideeffects
 * @return {google.maps.LatLngBounds}
 */
google.maps.Map.prototype.getBounds = function() {};

/**
 * @nosideeffects
 * @return {google.maps.LatLng}
 */
google.maps.Map.prototype.getCenter = function() {};

/**
 * @nosideeffects
 * @return {Node}
 */
google.maps.Map.prototype.getDiv = function() {};

/**
 * @nosideeffects
 * @return {number}
 */
google.maps.Map.prototype.getHeading = function() {};

/**
 * @nosideeffects
 * @return {google.maps.MapTypeId|string}
 */
google.maps.Map.prototype.getMapTypeId = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Projection}
 */
google.maps.Map.prototype.getProjection = function() {};

/**
 * @nosideeffects
 * @return {google.maps.StreetViewPanorama}
 */
google.maps.Map.prototype.getStreetView = function() {};

/**
 * @nosideeffects
 * @return {number}
 */
google.maps.Map.prototype.getTilt = function() {};

/**
 * @nosideeffects
 * @return {number}
 */
google.maps.Map.prototype.getZoom = function() {};

/**
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 */
google.maps.Map.prototype.panBy = function(x, y) {};

/**
 * @param {google.maps.LatLng} latLng
 * @return {undefined}
 */
google.maps.Map.prototype.panTo = function(latLng) {};

/**
 * @param {google.maps.LatLngBounds} latLngBounds
 * @return {undefined}
 */
google.maps.Map.prototype.panToBounds = function(latLngBounds) {};

/**
 * @param {google.maps.LatLng} latlng
 * @return {undefined}
 */
google.maps.Map.prototype.setCenter = function(latlng) {};

/**
 * @param {number} heading
 * @return {undefined}
 */
google.maps.Map.prototype.setHeading = function(heading) {};

/**
 * @param {google.maps.MapTypeId|string} mapTypeId
 * @return {undefined}
 */
google.maps.Map.prototype.setMapTypeId = function(mapTypeId) {};

/**
 * @param {google.maps.MapOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.Map.prototype.setOptions = function(options) {};

/**
 * @param {google.maps.StreetViewPanorama} panorama
 * @return {undefined}
 */
google.maps.Map.prototype.setStreetView = function(panorama) {};

/**
 * @param {number} tilt
 * @return {undefined}
 */
google.maps.Map.prototype.setTilt = function(tilt) {};

/**
 * @param {number} zoom
 * @return {undefined}
 */
google.maps.Map.prototype.setZoom = function(zoom) {};

/**
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.MapCanvasProjection = function() {};
google.maps.MapCanvasProjection.__super__ = Object;

/**
 * @param {google.maps.Point} pixel
 * @param {boolean=} opt_nowrap
 * @return {google.maps.LatLng}
 */
google.maps.MapCanvasProjection.prototype.fromContainerPixelToLatLng = function(pixel, opt_nowrap) {};

/**
 * @param {google.maps.Point} pixel
 * @param {boolean=} opt_nowrap
 * @return {google.maps.LatLng}
 */
google.maps.MapCanvasProjection.prototype.fromDivPixelToLatLng = function(pixel, opt_nowrap) {};

/**
 * @param {google.maps.LatLng} latLng
 * @return {google.maps.Point}
 */
google.maps.MapCanvasProjection.prototype.fromLatLngToContainerPixel = function(latLng) {};

/**
 * @param {google.maps.LatLng} latLng
 * @return {google.maps.Point}
 */
google.maps.MapCanvasProjection.prototype.fromLatLngToDivPixel = function(latLng) {};

/**
 * @nosideeffects
 * @return {number}
 */
google.maps.MapCanvasProjection.prototype.getWorldWidth = function() {};

/**
 * @constructor
 */
google.maps.MapOptions = function() {};
google.maps.MapOptions.__super__ = Object;

/**
 * @type {string}
 */
google.maps.MapOptions.prototype.backgroundColor = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.MapOptions.prototype.center = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.disableDefaultUI = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.disableDoubleClickZoom = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.draggable = 1;

/**
 * @type {string}
 */
google.maps.MapOptions.prototype.draggableCursor = 1;

/**
 * @type {string}
 */
google.maps.MapOptions.prototype.draggingCursor = 1;

/**
 * @type {number}
 */
google.maps.MapOptions.prototype.heading = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.keyboardShortcuts = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.mapMaker = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.mapTypeControl = 1;

/**
 * @type {google.maps.MapTypeControlOptions|Object.<string>}
 */
google.maps.MapOptions.prototype.mapTypeControlOptions = 1;

/**
 * @type {google.maps.MapTypeId}
 */
google.maps.MapOptions.prototype.mapTypeId = 1;

/**
 * @type {number}
 */
google.maps.MapOptions.prototype.maxZoom = 1;

/**
 * @type {number}
 */
google.maps.MapOptions.prototype.minZoom = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.noClear = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.overviewMapControl = 1;

/**
 * @type {google.maps.OverviewMapControlOptions|Object.<string>}
 */
google.maps.MapOptions.prototype.overviewMapControlOptions = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.panControl = 1;

/**
 * @type {google.maps.PanControlOptions|Object.<string>}
 */
google.maps.MapOptions.prototype.panControlOptions = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.rotateControl = 1;

/**
 * @type {google.maps.RotateControlOptions|Object.<string>}
 */
google.maps.MapOptions.prototype.rotateControlOptions = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.scaleControl = 1;

/**
 * @type {google.maps.ScaleControlOptions|Object.<string>}
 */
google.maps.MapOptions.prototype.scaleControlOptions = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.scrollwheel = 1;

/**
 * @type {google.maps.StreetViewPanorama}
 */
google.maps.MapOptions.prototype.streetView = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.streetViewControl = 1;

/**
 * @type {google.maps.StreetViewControlOptions|Object.<string>}
 */
google.maps.MapOptions.prototype.streetViewControlOptions = 1;

/**
 * @type {Array.<google.maps.MapTypeStyle>}
 */
google.maps.MapOptions.prototype.styles = 1;

/**
 * @type {number}
 */
google.maps.MapOptions.prototype.tilt = 1;

/**
 * @type {number}
 */
google.maps.MapOptions.prototype.zoom = 1;

/**
 * @type {boolean}
 */
google.maps.MapOptions.prototype.zoomControl = 1;

/**
 * @type {google.maps.ZoomControlOptions|Object.<string>}
 */
google.maps.MapOptions.prototype.zoomControlOptions = 1;

/**
 * @constructor
 */
google.maps.MapPanes = function() {};
google.maps.MapPanes.__super__ = Object;

/**
 * @type {Node}
 */
google.maps.MapPanes.prototype.floatPane = 1;

/**
 * @type {Node}
 */
google.maps.MapPanes.prototype.floatShadow = 1;

/**
 * @type {Node}
 */
google.maps.MapPanes.prototype.mapPane = 1;

/**
 * @type {Node}
 */
google.maps.MapPanes.prototype.overlayImage = 1;

/**
 * @type {Node}
 */
google.maps.MapPanes.prototype.overlayLayer = 1;

/**
 * @type {Node}
 */
google.maps.MapPanes.prototype.overlayMouseTarget = 1;

/**
 * @type {Node}
 */
google.maps.MapPanes.prototype.overlayShadow = 1;

/**
 * @constructor
 */
google.maps.MapType = function() {};
google.maps.MapType.__super__ = Object;

/**
 * @type {string}
 */
google.maps.MapType.prototype.alt = 1;

/**
 * @type {number}
 */
google.maps.MapType.prototype.maxZoom = 1;

/**
 * @type {number}
 */
google.maps.MapType.prototype.minZoom = 1;

/**
 * @type {string}
 */
google.maps.MapType.prototype.name = 1;

/**
 * @type {google.maps.Projection}
 */
google.maps.MapType.prototype.projection = 1;

/**
 * @type {number}
 */
google.maps.MapType.prototype.radius = 1;

/**
 * @type {google.maps.Size}
 */
google.maps.MapType.prototype.tileSize = 1;

/**
 * @param {google.maps.Point} tileCoord
 * @param {number} zoom
 * @param {Document} ownerDocument
 * @return {Node}
 */
google.maps.MapType.prototype.getTile =
    function(tileCoord, zoom, ownerDocument) {};

/**
 * @param {Node} tile
 * @return {undefined}
 */
google.maps.MapType.prototype.releaseTile = function(tile) {};

/**
 * @constructor
 */
google.maps.MapTypeControlOptions = function() {};
google.maps.MapTypeControlOptions.__super__ = Object;

/**
 * @type {Array.<google.maps.MapTypeId>|Array.<string>}
 */
google.maps.MapTypeControlOptions.prototype.mapTypeIds = 1;

/**
 * @type {google.maps.ControlPosition}
 */
google.maps.MapTypeControlOptions.prototype.position = 1;

/**
 * @type {google.maps.MapTypeControlStyle}
 */
google.maps.MapTypeControlOptions.prototype.style = 1;

/**
 * @enum {number|string}
 */
google.maps.MapTypeControlStyle = {
  DEFAULT: 1,
  DROPDOWN_MENU: 1,
  HORIZONTAL_BAR: 1
};

/**
 * @enum {number|string}
 */
google.maps.MapTypeId = {
  HYBRID: 1,
  ROADMAP: 1,
  SATELLITE: 1,
  TERRAIN: 1
};

/**
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.MapTypeRegistry = function() {};
google.maps.MapTypeRegistry.__super__ = Object;

/**
 * @param {string} id
 * @param {google.maps.MapType} mapType
 * @return {undefined}
 * @override
 */
google.maps.MapTypeRegistry.prototype.set = function(id, mapType) {};

/**
 * @constructor
 */
google.maps.MapTypeStyle = function() {};
google.maps.MapTypeStyle.__super__ = Object;

/**
 * @type {string}
 */
google.maps.MapTypeStyle.prototype.elementType = 1;

/**
 * @type {string}
 */
google.maps.MapTypeStyle.prototype.featureType = 1;

/**
 * @type {Array.<google.maps.MapTypeStyler>}
 */
google.maps.MapTypeStyle.prototype.stylers = 1;

/**
 * @constructor
 */
google.maps.MapTypeStyler = function() {};
google.maps.MapTypeStyler.__super__ = Object;

/**
 * @type {number}
 */
google.maps.MapTypeStyler.prototype.gamma = 1;

/**
 * @type {string}
 */
google.maps.MapTypeStyler.prototype.hue = 1;

/**
 * @type {boolean}
 */
google.maps.MapTypeStyler.prototype.invert_lightness = 1;

/**
 * @type {number}
 */
google.maps.MapTypeStyler.prototype.lightness = 1;

/**
 * @type {number}
 */
google.maps.MapTypeStyler.prototype.saturation = 1;

/**
 * @type {string}
 */
google.maps.MapTypeStyler.prototype.visibility = 1;

/**
 * @constructor
 */
google.maps.MapsEventListener = function() {};
google.maps.MapsEventListener.__super__ = Object;

/**
 * @param {(google.maps.MarkerOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.Marker = function(opt_opts) {};
google.maps.Marker.__super__ = Object;
google.maps.Marker.__before__ = [
  utils.mapArgs(
    copyMarkerOptions)
];

function copyMarkerOptions(o) {
  var r = {};
  utils.directCopy(r, o, [
    'animation',
    'clickable',
    'cursor',
    'draggable',
    'flat',
    'map',
    'optimized',
    'raiseOnDrag',
    'shape',
    'title',
    'visible',
    'zIndex'
  ]);

  function copyImageLike(p) {
    if (o[p] && (typeof o[p] === 'string')) {
      r[p] = resolve(frame.getUrl(), o[p]);
    } else {
      utils.directCopy(r, o, [p]);
    }
  }

  copyImageLike('icon');
  copyImageLike('position');
  copyImageLike('shadow');

  return r;
}

/**
 * @nosideeffects
 * @return {?google.maps.Animation}
 */
google.maps.Marker.prototype.getAnimation = function() {};

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Marker.prototype.getClickable = function() {};

/**
 * @nosideeffects
 * @return {string}
 */
google.maps.Marker.prototype.getCursor = function() {};

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Marker.prototype.getDraggable = function() {};

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Marker.prototype.getFlat = function() {};

/**
 * @nosideeffects
 * @return {string|google.maps.MarkerImage}
 */
google.maps.Marker.prototype.getIcon = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Map|google.maps.StreetViewPanorama}
 */
google.maps.Marker.prototype.getMap = function() {};

/**
 * @nosideeffects
 * @return {google.maps.LatLng}
 */
google.maps.Marker.prototype.getPosition = function() {};

/**
 * @nosideeffects
 * @return {string|google.maps.MarkerImage}
 */
google.maps.Marker.prototype.getShadow = function() {};

/**
 * @nosideeffects
 * @return {google.maps.MarkerShape}
 */
google.maps.Marker.prototype.getShape = function() {};

/**
 * @nosideeffects
 * @return {string}
 */
google.maps.Marker.prototype.getTitle = function() {};

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Marker.prototype.getVisible = function() {};

/**
 * @nosideeffects
 * @return {number}
 */
google.maps.Marker.prototype.getZIndex = function() {};

/**
 * @param {?google.maps.Animation} animation
 * @return {undefined}
 */
google.maps.Marker.prototype.setAnimation = function(animation) {};

/**
 * @param {boolean} flag
 * @return {undefined}
 */
google.maps.Marker.prototype.setClickable = function(flag) {};

/**
 * @param {string} cursor
 * @return {undefined}
 */
google.maps.Marker.prototype.setCursor = function(cursor) {};

/**
 * @param {?boolean} flag
 * @return {undefined}
 */
google.maps.Marker.prototype.setDraggable = function(flag) {};

/**
 * @param {boolean} flag
 * @return {undefined}
 */
google.maps.Marker.prototype.setFlat = function(flag) {};

/**
 * @param {string|google.maps.MarkerImage} icon
 * @return {undefined}
 */
google.maps.Marker.prototype.setIcon = function(icon) {};

/**
 * @param {google.maps.Map|google.maps.StreetViewPanorama} map
 * @return {undefined}
 */
google.maps.Marker.prototype.setMap = function(map) {};

/**
 * @param {google.maps.MarkerOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.Marker.prototype.setOptions = function(options) {};

/**
 * @param {google.maps.LatLng} latlng
 * @return {undefined}
 */
google.maps.Marker.prototype.setPosition = function(latlng) {};

/**
 * @param {string|google.maps.MarkerImage} shadow
 * @return {undefined}
 */
google.maps.Marker.prototype.setShadow = function(shadow) {};

/**
 * @param {google.maps.MarkerShape} shape
 * @return {undefined}
 */
google.maps.Marker.prototype.setShape = function(shape) {};

/**
 * @param {string} title
 * @return {undefined}
 */
google.maps.Marker.prototype.setTitle = function(title) {};

/**
 * @param {boolean} visible
 * @return {undefined}
 */
google.maps.Marker.prototype.setVisible = function(visible) {};

/**
 * @param {number} zIndex
 * @return {undefined}
 */
google.maps.Marker.prototype.setZIndex = function(zIndex) {};

/**
 * @constant
 * @type {number|string}
 */
google.maps.Marker.MAX_ZINDEX = 1;

/**
 * @param {string} url
 * @param {google.maps.Size=} opt_size
 * @param {google.maps.Point=} opt_origin
 * @param {google.maps.Point=} opt_anchor
 * @param {google.maps.Size=} opt_scaledSize
 * @constructor
 */
google.maps.MarkerImage =
    function(url, opt_size, opt_origin, opt_anchor, opt_scaledSize) {};
google.maps.MarkerImage.__super__ = Object;
google.maps.MarkerImage.__before__ = [
  function(f, self, args) {
    args = args.slice(0);
    args[0] = resolve(frame.getUrl(), args[0]);
    return args;
  }
];

/**
 * @type {google.maps.Point}
 */
google.maps.MarkerImage.prototype.anchor = 1;

/**
 * @type {google.maps.Point}
 */
google.maps.MarkerImage.prototype.origin = 1;

/**
 * @type {google.maps.Size}
 */
google.maps.MarkerImage.prototype.scaledSize = 1;

/**
 * @type {google.maps.Size}
 */
google.maps.MarkerImage.prototype.size = 1;

/**
 * @type {string}
 */
google.maps.MarkerImage.prototype.url = 1;

/**
 * @constructor
 */
google.maps.MarkerOptions = function() {};
google.maps.MarkerOptions.__super__ = Object;

/**
 * @type {google.maps.Animation}
 */
google.maps.MarkerOptions.prototype.animation = 1;

/**
 * @type {boolean}
 */
google.maps.MarkerOptions.prototype.clickable = 1;

/**
 * @type {string}
 */
google.maps.MarkerOptions.prototype.cursor = 1;

/**
 * @type {boolean}
 */
google.maps.MarkerOptions.prototype.draggable = 1;

/**
 * @type {boolean}
 */
google.maps.MarkerOptions.prototype.flat = 1;

/**
 * @type {string|google.maps.MarkerImage|google.maps.Symbol}
 */
google.maps.MarkerOptions.prototype.icon = 1;

/**
 * @type {google.maps.Map|google.maps.StreetViewPanorama}
 */
google.maps.MarkerOptions.prototype.map = 1;

/**
 * @type {boolean}
 */
google.maps.MarkerOptions.prototype.optimized = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.MarkerOptions.prototype.position = 1;

/**
 * @type {boolean}
 */
google.maps.MarkerOptions.prototype.raiseOnDrag = 1;

/**
 * @type {string|google.maps.MarkerImage|google.maps.Symbol}
 */
google.maps.MarkerOptions.prototype.shadow = 1;

/**
 * @type {google.maps.MarkerShape}
 */
google.maps.MarkerOptions.prototype.shape = 1;

/**
 * @type {string}
 */
google.maps.MarkerOptions.prototype.title = 1;

/**
 * @type {boolean}
 */
google.maps.MarkerOptions.prototype.visible = 1;

/**
 * @type {number}
 */
google.maps.MarkerOptions.prototype.zIndex = 1;

/**
 * @constructor
 */
google.maps.MarkerShape = function() {};
google.maps.MarkerShape.__super__ = Object;

/**
 * @type {Array.<number>}
 */
google.maps.MarkerShape.prototype.coords = 1;

/**
 * @type {string}
 */
google.maps.MarkerShape.prototype.type = 1;

/**
 * @constructor
 */
google.maps.MaxZoomResult = function() {};
google.maps.MaxZoomResult.__super__ = Object;

/**
 * @type {google.maps.MaxZoomStatus}
 */
google.maps.MaxZoomResult.prototype.status = 1;

/**
 * @type {number}
 */
google.maps.MaxZoomResult.prototype.zoom = 1;

/**
 * @constructor
 */
google.maps.MaxZoomService = function() {};
google.maps.MaxZoomService.__super__ = Object;

/**
 * @param {google.maps.LatLng} latlng
 * @param {function(google.maps.MaxZoomResult)} callback
 * @return {undefined}
 */
google.maps.MaxZoomService.prototype.getMaxZoomAtLatLng =
    function(latlng, callback) {};

/**
 * @enum {number|string}
 */
google.maps.MaxZoomStatus = {
  ERROR: 1,
  OK: 1
};

/**
 * @constructor
 */
google.maps.MouseEvent = function() {};
google.maps.MouseEvent.__super__ = Object;

/**
 * @type {google.maps.LatLng}
 */
google.maps.MouseEvent.prototype.latLng = 1;

/**
 * @return {undefined}
 */
google.maps.MouseEvent.prototype.stop = function() {};

/**
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.OverlayView = function() {};
google.maps.OverlayView.__super__ = ['google', 'maps', 'MVCObject'];

/**
 * @return {undefined}
 */
google.maps.OverlayView.prototype.draw = function() {};
google.maps.OverlayView.prototype.__draw_OVERRIDE__ = 1;

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.OverlayView.prototype.getMap = function() {};

/**
 * @nosideeffects
 * @return {google.maps.MapPanes}
 */
google.maps.OverlayView.prototype.getPanes = function() {};

/**
 * @nosideeffects
 * @return {google.maps.MapCanvasProjection}
 */
google.maps.OverlayView.prototype.getProjection = function() {};

/**
 * @return {undefined}
 */
google.maps.OverlayView.prototype.onAdd = function() {};
google.maps.OverlayView.prototype.__onAdd_OVERRIDE__ = 1;

/**
 * @return {undefined}
 */
google.maps.OverlayView.prototype.onRemove = function() {};
google.maps.OverlayView.prototype.__onRemove_OVERRIDE__ = 1;

/**
 * @param {google.maps.Map|google.maps.StreetViewPanorama} map
 * @return {undefined}
 */
google.maps.OverlayView.prototype.setMap = function(map) {};

/**
 * @constructor
 */
google.maps.OverviewMapControlOptions = function() {};
google.maps.OverviewMapControlOptions.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.OverviewMapControlOptions.prototype.opened = 1;

/**
 * @constructor
 */
google.maps.PanControlOptions = function() {};
google.maps.PanControlOptions.__super__ = Object;

/**
 * @type {google.maps.ControlPosition}
 */
google.maps.PanControlOptions.prototype.position = 1;

/**
 * @constructor
 */
google.maps.PathElevationRequest = function() {};
google.maps.PathElevationRequest.__super__ = Object;

/**
 * @type {Array.<google.maps.LatLng>}
 */
google.maps.PathElevationRequest.prototype.path = 1;

/**
 * @type {number}
 */
google.maps.PathElevationRequest.prototype.samples = 1;

/**
 * @param {number} x
 * @param {number} y
 * @constructor
 */
google.maps.Point = function(x, y) {};
google.maps.Point.__super__ = Object;

/**
 * @type {number}
 */
google.maps.Point.prototype.x = 1;
google.maps.Point.prototype.__x_OVERRIDE__ = 1;

/**
 * @type {number}
 */
google.maps.Point.prototype.y = 1;
google.maps.Point.prototype.__y_OVERRIDE__ = 1;

/**
 * @param {google.maps.Point} other
 * @return {boolean}
 */
google.maps.Point.prototype.equals = function(other) {};

/**
 * @return {string}
 */
/* google.maps.Point.prototype.toString = function() {}; */

/**
 * @extends {google.maps.MouseEvent}
 * @constructor
 */
google.maps.PolyMouseEvent = function() {};
google.maps.PolyMouseEvent.__super__ = Object;

/**
 * @type {number}
 */
google.maps.PolyMouseEvent.prototype.edge = 1;

/**
 * @type {number}
 */
google.maps.PolyMouseEvent.prototype.path = 1;

/**
 * @type {number}
 */
google.maps.PolyMouseEvent.prototype.vertex = 1;

/**
 * @param {(google.maps.PolygonOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.Polygon = function(opt_opts) {};
google.maps.Polygon.__super__ = Object;

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Polygon.prototype.getEditable = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.Polygon.prototype.getMap = function() {};

/**
 * @nosideeffects
 * @return {google.maps.MVCArray.<google.maps.LatLng>}
 */
google.maps.Polygon.prototype.getPath = function() {};

/**
 * @nosideeffects
 * @return {google.maps.MVCArray.<google.maps.MVCArray.<google.maps.LatLng>>}
 */
google.maps.Polygon.prototype.getPaths = function() {};

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Polygon.prototype.getVisible = function() {};

/**
 * @param {boolean} editable
 * @return {undefined}
 */
google.maps.Polygon.prototype.setEditable = function(editable) {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.Polygon.prototype.setMap = function(map) {};

/**
 * @param {google.maps.PolygonOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.Polygon.prototype.setOptions = function(options) {};

/**
 * @param {google.maps.MVCArray.<google.maps.LatLng>|Array.<google.maps.LatLng>} path
 * @return {undefined}
 */
google.maps.Polygon.prototype.setPath = function(path) {};

/**
 * @param {google.maps.MVCArray.<google.maps.MVCArray.<google.maps.LatLng>>|google.maps.MVCArray.<google.maps.LatLng>|Array.<Array.<google.maps.LatLng>>|Array.<google.maps.LatLng>} paths
 * @return {undefined}
 */
google.maps.Polygon.prototype.setPaths = function(paths) {};

/**
 * @param {boolean} visible
 * @return {undefined}
 */
google.maps.Polygon.prototype.setVisible = function(visible) {};

/**
 * @constructor
 */
google.maps.PolygonOptions = function() {};
google.maps.PolygonOptions.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.PolygonOptions.prototype.clickable = 1;

/**
 * @type {boolean}
 */
google.maps.PolygonOptions.prototype.editable = 1;

/**
 * @type {string}
 */
google.maps.PolygonOptions.prototype.fillColor = 1;

/**
 * @type {number}
 */
google.maps.PolygonOptions.prototype.fillOpacity = 1;

/**
 * @type {boolean}
 */
google.maps.PolygonOptions.prototype.geodesic = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.PolygonOptions.prototype.map = 1;

/**
 * @type {google.maps.MVCArray.<google.maps.MVCArray.<google.maps.LatLng>>|google.maps.MVCArray.<google.maps.LatLng>|Array.<Array.<google.maps.LatLng>>|Array.<google.maps.LatLng>}
 */
google.maps.PolygonOptions.prototype.paths = 1;

/**
 * @type {string}
 */
google.maps.PolygonOptions.prototype.strokeColor = 1;

/**
 * @type {number}
 */
google.maps.PolygonOptions.prototype.strokeOpacity = 1;

/**
 * @type {number}
 */
google.maps.PolygonOptions.prototype.strokeWeight = 1;

/**
 * @type {boolean}
 */
google.maps.PolygonOptions.prototype.visible = 1;

/**
 * @type {number}
 */
google.maps.PolygonOptions.prototype.zIndex = 1;

/**
 * @param {(google.maps.PolylineOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.Polyline = function(opt_opts) {};
google.maps.Polyline.__super__ = Object;

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Polyline.prototype.getEditable = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.Polyline.prototype.getMap = function() {};

/**
 * @nosideeffects
 * @return {google.maps.MVCArray.<google.maps.LatLng>}
 */
google.maps.Polyline.prototype.getPath = function() {};

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Polyline.prototype.getVisible = function() {};

/**
 * @param {boolean} editable
 * @return {undefined}
 */
google.maps.Polyline.prototype.setEditable = function(editable) {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.Polyline.prototype.setMap = function(map) {};

/**
 * @param {google.maps.PolylineOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.Polyline.prototype.setOptions = function(options) {};

/**
 * @param {google.maps.MVCArray.<google.maps.LatLng>|Array.<google.maps.LatLng>} path
 * @return {undefined}
 */
google.maps.Polyline.prototype.setPath = function(path) {};

/**
 * @param {boolean} visible
 * @return {undefined}
 */
google.maps.Polyline.prototype.setVisible = function(visible) {};

/**
 * @constructor
 */
google.maps.PolylineOptions = function() {};
google.maps.PolylineOptions.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.PolylineOptions.prototype.clickable = 1;

/**
 * @type {boolean}
 */
google.maps.PolylineOptions.prototype.editable = 1;

/**
 * @type {boolean}
 */
google.maps.PolylineOptions.prototype.geodesic = 1;

/**
 * @type {Array.<google.maps.IconSequence>}
 */
google.maps.PolylineOptions.prototype.icons = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.PolylineOptions.prototype.map = 1;

/**
 * @type {google.maps.MVCArray.<google.maps.LatLng>|Array.<google.maps.LatLng>}
 */
google.maps.PolylineOptions.prototype.path = 1;

/**
 * @type {string}
 */
google.maps.PolylineOptions.prototype.strokeColor = 1;

/**
 * @type {number}
 */
google.maps.PolylineOptions.prototype.strokeOpacity = 1;

/**
 * @type {number}
 */
google.maps.PolylineOptions.prototype.strokeWeight = 1;

/**
 * @type {boolean}
 */
google.maps.PolylineOptions.prototype.visible = 1;

/**
 * @type {number}
 */
google.maps.PolylineOptions.prototype.zIndex = 1;

/**
 * @constructor
 */
google.maps.Projection = function() {};
google.maps.Projection.__super__ = Object;

/**
 * @param {google.maps.LatLng} latLng
 * @param {google.maps.Point=} opt_point
 * @return {google.maps.Point}
 */
google.maps.Projection.prototype.fromLatLngToPoint =
    function(latLng, opt_point) {};

/**
 * @param {google.maps.Point} pixel
 * @param {boolean=} opt_nowrap
 * @return {google.maps.LatLng}
 */
google.maps.Projection.prototype.fromPointToLatLng =
    function(pixel, opt_nowrap) {};

/**
 * @param {(google.maps.RectangleOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.Rectangle = function(opt_opts) {};
google.maps.Rectangle.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.LatLngBounds}
 */
google.maps.Rectangle.prototype.getBounds = function() {};

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Rectangle.prototype.getEditable = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.Rectangle.prototype.getMap = function() {};

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.Rectangle.prototype.getVisible = function() {};

/**
 * @param {google.maps.LatLngBounds} bounds
 * @return {undefined}
 */
google.maps.Rectangle.prototype.setBounds = function(bounds) {};

/**
 * @param {boolean} editable
 * @return {undefined}
 */
google.maps.Rectangle.prototype.setEditable = function(editable) {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.Rectangle.prototype.setMap = function(map) {};

/**
 * @param {google.maps.RectangleOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.Rectangle.prototype.setOptions = function(options) {};

/**
 * @param {boolean} visible
 * @return {undefined}
 */
google.maps.Rectangle.prototype.setVisible = function(visible) {};

/**
 * @constructor
 */
google.maps.RectangleOptions = function() {};
google.maps.RectangleOptions.__super__ = Object;

/**
 * @type {google.maps.LatLngBounds}
 */
google.maps.RectangleOptions.prototype.bounds = 1;

/**
 * @type {boolean}
 */
google.maps.RectangleOptions.prototype.clickable = 1;

/**
 * @type {boolean}
 */
google.maps.RectangleOptions.prototype.editable = 1;

/**
 * @type {string}
 */
google.maps.RectangleOptions.prototype.fillColor = 1;

/**
 * @type {number}
 */
google.maps.RectangleOptions.prototype.fillOpacity = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.RectangleOptions.prototype.map = 1;

/**
 * @type {string}
 */
google.maps.RectangleOptions.prototype.strokeColor = 1;

/**
 * @type {number}
 */
google.maps.RectangleOptions.prototype.strokeOpacity = 1;

/**
 * @type {number}
 */
google.maps.RectangleOptions.prototype.strokeWeight = 1;

/**
 * @type {boolean}
 */
google.maps.RectangleOptions.prototype.visible = 1;

/**
 * @type {number}
 */
google.maps.RectangleOptions.prototype.zIndex = 1;

/**
 * @constructor
 */
google.maps.RotateControlOptions = function() {};
google.maps.RotateControlOptions.__super__ = Object;

/**
 * @type {google.maps.ControlPosition}
 */
google.maps.RotateControlOptions.prototype.position = 1;

/**
 * @constructor
 */
google.maps.ScaleControlOptions = function() {};
google.maps.ScaleControlOptions.__super__ = Object;

/**
 * @type {google.maps.ControlPosition}
 */
google.maps.ScaleControlOptions.prototype.position = 1;

/**
 * @type {google.maps.ScaleControlStyle}
 */
google.maps.ScaleControlOptions.prototype.style = 1;

/**
 * @enum {number|string}
 */
google.maps.ScaleControlStyle = {
  DEFAULT: 1
};

/**
 * @param {number} width
 * @param {number} height
 * @param {string=} opt_widthUnit
 * @param {string=} opt_heightUnit
 * @constructor
 */
google.maps.Size = function(width, height, opt_widthUnit, opt_heightUnit) {};
google.maps.Size.__super__ = Object;

/**
 * @type {number}
 */
google.maps.Size.prototype.height = 1;

/**
 * @type {number}
 */
google.maps.Size.prototype.width = 1;

/**
 * @param {google.maps.Size} other
 * @return {boolean}
 */
google.maps.Size.prototype.equals = function(other) {};

/**
 * @return {string}
 */
/* google.maps.Size.prototype.toString = function() {}; */

/**
 * @constructor
 */
google.maps.StreetViewAddressControlOptions = function() {};
google.maps.StreetViewAddressControlOptions.__super__ = Object;

/**
 * @type {google.maps.ControlPosition}
 */
google.maps.StreetViewAddressControlOptions.prototype.position = 1;

/**
 * @constructor
 */
google.maps.StreetViewControlOptions = function() {};
google.maps.StreetViewControlOptions.__super__ = Object;

/**
 * @type {google.maps.ControlPosition}
 */
google.maps.StreetViewControlOptions.prototype.position = 1;

/**
 * @constructor
 */
google.maps.StreetViewLink = function() {};
google.maps.StreetViewLink.__super__ = Object;

/**
 * @type {string}
 */
google.maps.StreetViewLink.prototype.description = 1;

/**
 * @type {number}
 */
google.maps.StreetViewLink.prototype.heading = 1;

/**
 * @type {string}
 */
google.maps.StreetViewLink.prototype.pano = 1;

/**
 * @constructor
 */
google.maps.StreetViewLocation = function() {};
google.maps.StreetViewLocation.__super__ = Object;

/**
 * @type {string}
 */
google.maps.StreetViewLocation.prototype.description = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.StreetViewLocation.prototype.latLng = 1;

/**
 * @type {string}
 */
google.maps.StreetViewLocation.prototype.pano = 1;

/**
 * @param {Node} container
 * @param {(google.maps.StreetViewPanoramaOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.StreetViewPanorama = function(container, opt_opts) {};
google.maps.StreetViewPanorama.__super__ = Object;
google.maps.StreetViewPanorama.__before__ = [
  utils.mapArgs(
    utils.identity,
    copyStreetViewPanoramaOptions)
];

// TODO(ihab.awad): Horrible boilerplate code!! Must invent some syntactic
// approach to do these copies, or figure out how to make things so that the
// taming hashmap keys don't break host code.

function copyStreetViewPanoramaOptions(o) {
  var r = {};
  utils.directCopy(r, o, [
    'addressControl',
    'clickToGo',
    'disableDoubleClickZoom',
    'enableCloseButton',
    'imageDateControl',
    'linksControl',
    'panControl',
    'pano',
    'scrollwheel',
    'visible',
    'zoomControl'
  ]);

  if (o.addressControlOptions) {
    r.addressControlOptions = {};
    utils.directCopy(r.addressControlOptions, o.addressControlOptions, [
      'position'
    ]);
  }

  if (o.panControlOptions) {
    r.panControlOptions	= {};
    utils.directCopy(r.panControlOptions, o.panControlOptions, [
      'position'
    ]);
  }

  if (o.position) {
    r.position = new window.google.maps.LatLng(
        o.position.lat(),
        o.position.lng());
  }

  if (o.pov) {
    r.pov = {};
    utils.directCopy(r.pov, o.pov, [
      'heading',
      'pitch',
      'zoom'
    ]);
  }

  if (o.zoomControlOptions) {
    r.zoomControlOptions = {};
    utils.directCopy(r.zoomControlOptions, o.zoomControlOptions, [
      'position',
      'style'
    ]);
  }

  if (o && o.panoProvider) {
    var origPanoProvider = o.panoProvider;
    r.panoProvider = frame.markFunction(function(pano, zoom, tileX, tileY) {
      var result = origPanoProvider(pano, zoom, tileX, tileY);
      if (result && result.tiles && result.tiles.getTileUrl) {
        var origGetTileUrl = result.tiles.getTileUrl;
        result.tiles.getTileUrl =
            frame.markFunction(function(pano, zoom, tileX, tileY) {
              return resolve(
                  frame.getUrl(),
                  origGetTileUrl(pano, zoom, tileX, tileY));
            });
      }
      return result;
    });
  }

  return r;
}

/**
 */
google.maps.StreetViewPanorama.prototype.setOptions = function(o) {};
google.maps.StreetViewPanorama.prototype.setOptions.__before__ = [
  utils.mapArgs(copyStreetViewPanoramaOptions)
];

/**
 * @type {Array.<google.maps.MVCArray.<Node>>}
 */
google.maps.StreetViewPanorama.prototype.controls = 1;

/**
 * @nosideeffects
 * @return {Array.<google.maps.StreetViewLink>}
 */
google.maps.StreetViewPanorama.prototype.getLinks = function() {};

/**
 * @nosideeffects
 * @return {string}
 */
google.maps.StreetViewPanorama.prototype.getPano = function() {};

/**
 * @nosideeffects
 * @return {google.maps.LatLng}
 */
google.maps.StreetViewPanorama.prototype.getPosition = function() {};

/**
 * @nosideeffects
 * @return {google.maps.StreetViewPov}
 */
google.maps.StreetViewPanorama.prototype.getPov = function() {};
google.maps.StreetViewPanorama.prototype.getPov.__after__ = [
  function(f, self, r) {
    // Feral function returns google.maps.StreetViewPov but this is
    // not an exposed class, so we don't know how to tame it.
    return {
      heading: r.heading,
      pitch: r.pitch,
      zoom: r.zoom
    };
  }
];

/**
 * @nosideeffects
 * @return {boolean}
 */
google.maps.StreetViewPanorama.prototype.getVisible = function() {};

/**
 * @param {function(string):google.maps.StreetViewPanoramaData} provider
 * @return {undefined}
 */
google.maps.StreetViewPanorama.prototype.registerPanoProvider =
    function(provider) {};

/**
 * @param {string} pano
 * @return {undefined}
 */
google.maps.StreetViewPanorama.prototype.setPano = function(pano) {};

/**
 * @param {google.maps.LatLng} latLng
 * @return {undefined}
 */
google.maps.StreetViewPanorama.prototype.setPosition = function(latLng) {};

/**
 * @param {google.maps.StreetViewPov} pov
 * @return {undefined}
 */
google.maps.StreetViewPanorama.prototype.setPov = function(pov) {};

/**
 * @param {boolean} flag
 * @return {undefined}
 */
google.maps.StreetViewPanorama.prototype.setVisible = function(flag) {};

/**
 * @constructor
 */
google.maps.StreetViewPanoramaData = function() {};
google.maps.StreetViewPanoramaData.__super__ = Object;

/**
 * @type {string}
 */
google.maps.StreetViewPanoramaData.prototype.copyright = 1;

/**
 * @type {string}
 */
google.maps.StreetViewPanoramaData.prototype.imageDate = 1;

/**
 * @type {Array.<google.maps.StreetViewLink>}
 */
google.maps.StreetViewPanoramaData.prototype.links = 1;

/**
 * @type {google.maps.StreetViewLocation}
 */
google.maps.StreetViewPanoramaData.prototype.location = 1;

/**
 * @type {google.maps.StreetViewTileData}
 */
google.maps.StreetViewPanoramaData.prototype.tiles = 1;

/**
 * @constructor
 */
google.maps.StreetViewPanoramaOptions = function() {};
google.maps.StreetViewPanoramaOptions.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.StreetViewPanoramaOptions.prototype.addressControl = 1;

/**
 * @type {google.maps.StreetViewAddressControlOptions|Object.<string>}
 */
google.maps.StreetViewPanoramaOptions.prototype.addressControlOptions = 1;

/**
 * @type {boolean}
 */
google.maps.StreetViewPanoramaOptions.prototype.clickToGo = 1;

/**
 * @type {boolean}
 */
google.maps.StreetViewPanoramaOptions.prototype.disableDoubleClickZoom = 1;

/**
 * @type {boolean}
 */
google.maps.StreetViewPanoramaOptions.prototype.enableCloseButton = 1;

/**
 * @type {boolean}
 */
google.maps.StreetViewPanoramaOptions.prototype.imageDateControl = 1;

/**
 * @type {boolean}
 */
google.maps.StreetViewPanoramaOptions.prototype.linksControl = 1;

/**
 * @type {boolean}
 */
google.maps.StreetViewPanoramaOptions.prototype.panControl = 1;

/**
 * @type {google.maps.PanControlOptions|Object.<string>}
 */
google.maps.StreetViewPanoramaOptions.prototype.panControlOptions = 1;

/**
 * @type {string}
 */
google.maps.StreetViewPanoramaOptions.prototype.pano = 1;

/**
 * @type {function(string):google.maps.StreetViewPanoramaData}
 */
google.maps.StreetViewPanoramaOptions.prototype.panoProvider = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.StreetViewPanoramaOptions.prototype.position = 1;

/**
 * @type {google.maps.StreetViewPov}
 */
google.maps.StreetViewPanoramaOptions.prototype.pov = 1;

/**
 * @type {boolean}
 */
google.maps.StreetViewPanoramaOptions.prototype.scrollwheel = 1;

/**
 * @type {boolean}
 */
google.maps.StreetViewPanoramaOptions.prototype.visible = 1;

/**
 * @type {boolean}
 */
google.maps.StreetViewPanoramaOptions.prototype.zoomControl = 1;

/**
 * @type {google.maps.ZoomControlOptions|Object.<string>}
 */
google.maps.StreetViewPanoramaOptions.prototype.zoomControlOptions = 1;

/**
 * @constructor
 */
google.maps.StreetViewPov = function() {};
google.maps.StreetViewPov.__super__ = Object;

/**
 * @type {number}
 */
google.maps.StreetViewPov.prototype.heading = 1;

/**
 * @type {number}
 */
google.maps.StreetViewPov.prototype.pitch = 1;

/**
 * @type {number}
 */
google.maps.StreetViewPov.prototype.zoom = 1;

/**
 * @constructor
 */
google.maps.StreetViewService = function() {};
google.maps.StreetViewService.__super__ = Object;

/**
 * @param {string} pano
 * @param {function(google.maps.StreetViewPanoramaData, google.maps.StreetViewStatus)} callback
 * @return {undefined}
 */
google.maps.StreetViewService.prototype.getPanoramaById =
    function(pano, callback) {};

/**
 * @param {google.maps.LatLng} latlng
 * @param {number} radius
 * @param {function(google.maps.StreetViewPanoramaData, google.maps.StreetViewStatus)} callback
 * @return {undefined}
 */
google.maps.StreetViewService.prototype.getPanoramaByLocation =
    function(latlng, radius, callback) {};

/**
 * @enum {number|string}
 */
google.maps.StreetViewStatus = {
  OK: 1,
  UNKNOWN_ERROR: 1,
  ZERO_RESULTS: 1
};

/**
 * @constructor
 */
google.maps.StreetViewTileData = function() {};
google.maps.StreetViewTileData.__super__ = Object;

/**
 * @type {number}
 */
google.maps.StreetViewTileData.prototype.centerHeading = 1;

/**
 * @type {google.maps.Size}
 */
google.maps.StreetViewTileData.prototype.tileSize = 1;

/**
 * @type {google.maps.Size}
 */
google.maps.StreetViewTileData.prototype.worldSize = 1;

/**
 * @param {string} pano
 * @param {number} tileZoom
 * @param {number} tileX
 * @param {number} tileY
 * @return {string}
 */
google.maps.StreetViewTileData.prototype.getTileUrl =
    function(pano, tileZoom, tileX, tileY) {};

/**
 * @param {Array.<google.maps.MapTypeStyle>} styles
 * @param {(google.maps.StyledMapTypeOptions|Object.<string>)=} opt_options
 * @constructor
 */
google.maps.StyledMapType = function(styles, opt_options) {};
google.maps.StyledMapType.__super__ = Object;

/**
 * @constructor
 */
google.maps.StyledMapTypeOptions = function() {};
google.maps.StyledMapTypeOptions.__super__ = Object;

/**
 * @type {string}
 */
google.maps.StyledMapTypeOptions.prototype.alt = 1;

/**
 * @type {number}
 */
google.maps.StyledMapTypeOptions.prototype.maxZoom = 1;

/**
 * @type {number}
 */
google.maps.StyledMapTypeOptions.prototype.minZoom = 1;

/**
 * @type {string}
 */
google.maps.StyledMapTypeOptions.prototype.name = 1;

/**
 * @constructor
 */
google.maps.Symbol = function() {};
google.maps.Symbol.__super__ = Object;

/**
 * @type {google.maps.Point}
 */
google.maps.Symbol.prototype.anchor = 1;

/**
 * @type {string}
 */
google.maps.Symbol.prototype.fillColor = 1;

/**
 * @type {number}
 */
google.maps.Symbol.prototype.fillOpacity = 1;

/**
 * @type {google.maps.SymbolPath|string}
 */
google.maps.Symbol.prototype.path = 1;

/**
 * @type {number}
 */
google.maps.Symbol.prototype.rotation = 1;

/**
 * @type {number}
 */
google.maps.Symbol.prototype.scale = 1;

/**
 * @type {string}
 */
google.maps.Symbol.prototype.strokeColor = 1;

/**
 * @type {number}
 */
google.maps.Symbol.prototype.strokeOpacity = 1;

/**
 * @type {number}
 */
google.maps.Symbol.prototype.strokeWeight = 1;

/**
 * @enum {number|string}
 */
google.maps.SymbolPath = {
  BACKWARD_CLOSED_ARROW: 1,
  BACKWARD_OPEN_ARROW: 1,
  CIRCLE: 1,
  FORWARD_CLOSED_ARROW: 1,
  FORWARD_OPEN_ARROW: 1
};

/**
 * @constructor
 */
google.maps.Time = function() {};
google.maps.Time.__super__ = Object;

/**
 * @type {string}
 */
google.maps.Time.prototype.text = 1;

/**
 * @type {string}
 */
google.maps.Time.prototype.time_zone = 1;

/**
 * @type {Date}
 */
google.maps.Time.prototype.value = 1;

/**
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.TrafficLayer = function() {};
google.maps.TrafficLayer.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.TrafficLayer.prototype.getMap = function() {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.TrafficLayer.prototype.setMap = function(map) {};

/**
 * @constructor
 */
google.maps.TransitAgency = function() {};
google.maps.TransitAgency.__super__ = Object;

/**
 * @type {string}
 */
google.maps.TransitAgency.prototype.name = 1;

/**
 * @type {string}
 */
google.maps.TransitAgency.prototype.phone = 1;

/**
 * @type {string}
 */
google.maps.TransitAgency.prototype.url = 1;

/**
 * @constructor
 */
google.maps.TransitDetails = function() {};
google.maps.TransitDetails.__super__ = Object;

/**
 * @type {google.maps.TransitStop}
 */
google.maps.TransitDetails.prototype.arrival_stop = 1;

/**
 * @type {google.maps.Time}
 */
google.maps.TransitDetails.prototype.arrival_time = 1;

/**
 * @type {google.maps.TransitStop}
 */
google.maps.TransitDetails.prototype.departure_stop = 1;

/**
 * @type {google.maps.Time}
 */
google.maps.TransitDetails.prototype.departure_time = 1;

/**
 * @type {string}
 */
google.maps.TransitDetails.prototype.headsign = 1;

/**
 * @type {number}
 */
google.maps.TransitDetails.prototype.headway = 1;

/**
 * @type {google.maps.TransitLine}
 */
google.maps.TransitDetails.prototype.line = 1;

/**
 * @type {number}
 */
google.maps.TransitDetails.prototype.num_stops = 1;

/**
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.TransitLayer = function() {};
google.maps.TransitLayer.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.TransitLayer.prototype.getMap = function() {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.TransitLayer.prototype.setMap = function(map) {};

/**
 * @constructor
 */
google.maps.TransitLine = function() {};
google.maps.TransitLine.__super__ = Object;

/**
 * @type {Array.<google.maps.TransitAgency>}
 */
google.maps.TransitLine.prototype.agencies = 1;

/**
 * @type {string}
 */
google.maps.TransitLine.prototype.color = 1;

/**
 * @type {string}
 */
google.maps.TransitLine.prototype.icon = 1;

/**
 * @type {string}
 */
google.maps.TransitLine.prototype.name = 1;

/**
 * @type {string}
 */
google.maps.TransitLine.prototype.short_name = 1;

/**
 * @type {string}
 */
google.maps.TransitLine.prototype.text_color = 1;

/**
 * @type {string}
 */
google.maps.TransitLine.prototype.url = 1;

/**
 * @type {google.maps.TransitVehicle}
 */
google.maps.TransitLine.prototype.vehicle = 1;

/**
 * @constructor
 */
google.maps.TransitOptions = function() {};
google.maps.TransitOptions.__super__ = Object;

/**
 * @type {Date}
 */
google.maps.TransitOptions.prototype.arrivalTime = 1;

/**
 * @type {Date}
 */
google.maps.TransitOptions.prototype.departureTime = 1;

/**
 * @constructor
 */
google.maps.TransitStop = function() {};
google.maps.TransitStop.__super__ = Object;

/**
 * @type {google.maps.LatLng}
 */
google.maps.TransitStop.prototype.location = 1;

/**
 * @type {string}
 */
google.maps.TransitStop.prototype.name = 1;

/**
 * @constructor
 */
google.maps.TransitVehicle = function() {};
google.maps.TransitVehicle.__super__ = Object;

/**
 * @type {string}
 */
google.maps.TransitVehicle.prototype.icon = 1;

/**
 * @type {string}
 */
google.maps.TransitVehicle.prototype.local_icon = 1;

/**
 * @type {string}
 */
google.maps.TransitVehicle.prototype.name = 1;

/**
 * @enum {number|string}
 */
google.maps.TravelMode = {
  BICYCLING: 1,
  DRIVING: 1,
  TRANSIT: 1,
  WALKING: 1
};

/**
 * @enum {number|string}
 */
google.maps.UnitSystem = {
  IMPERIAL: 1,
  METRIC: 1
};

/**
 * @constructor
 */
google.maps.ZoomControlOptions = function() {};
google.maps.ZoomControlOptions.__super__ = Object;

/**
 * @type {google.maps.ControlPosition}
 */
google.maps.ZoomControlOptions.prototype.position = 1;

/**
 * @type {google.maps.ZoomControlStyle}
 */
google.maps.ZoomControlOptions.prototype.style = 1;

/**
 * @enum {number|string}
 */
google.maps.ZoomControlStyle = {
  DEFAULT: 1,
  LARGE: 1,
  SMALL: 1
};

// Namespace
google.maps.adsense = {};

/**
 * @enum {number|string}
 */
google.maps.adsense.AdFormat = {
  BANNER: 1,
  BUTTON: 1,
  HALF_BANNER: 1,
  LARGE_RECTANGLE: 1,
  LEADERBOARD: 1,
  MEDIUM_RECTANGLE: 1,
  SKYSCRAPER: 1,
  SMALL_RECTANGLE: 1,
  SMALL_SQUARE: 1,
  SQUARE: 1,
  VERTICAL_BANNER: 1,
  WIDE_SKYSCRAPER: 1
};

/**
 * @param {Node} container
 * @param {google.maps.adsense.AdUnitOptions|Object.<string>} opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.adsense.AdUnit = function(container, opts) {};
google.maps.adsense.AdUnit.__super__ = Object;

/**
 * @nosideeffects
 * @return {string}
 */
google.maps.adsense.AdUnit.prototype.getChannelNumber = function() {};

/**
 * @nosideeffects
 * @return {Node}
 */
google.maps.adsense.AdUnit.prototype.getContainer = function() {};

/**
 * @nosideeffects
 * @return {google.maps.adsense.AdFormat}
 */
google.maps.adsense.AdUnit.prototype.getFormat = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.adsense.AdUnit.prototype.getMap = function() {};

/**
 * @nosideeffects
 * @return {google.maps.ControlPosition}
 */
google.maps.adsense.AdUnit.prototype.getPosition = function() {};

/**
 * @nosideeffects
 * @return {string}
 */
google.maps.adsense.AdUnit.prototype.getPublisherId = function() {};

/**
 * @param {string} channelNumber
 * @return {undefined}
 */
google.maps.adsense.AdUnit.prototype.setChannelNumber =
    function(channelNumber) {};

/**
 * @param {google.maps.adsense.AdFormat} format
 * @return {undefined}
 */
google.maps.adsense.AdUnit.prototype.setFormat = function(format) {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.adsense.AdUnit.prototype.setMap = function(map) {};

/**
 * @param {google.maps.ControlPosition} position
 * @return {undefined}
 */
google.maps.adsense.AdUnit.prototype.setPosition = function(position) {};

/**
 * @constructor
 */
google.maps.adsense.AdUnitOptions = function() {};

/**
 * @type {string}
 */
google.maps.adsense.AdUnitOptions.prototype.channelNumber = 1;

/**
 * @type {google.maps.adsense.AdFormat}
 */
google.maps.adsense.AdUnitOptions.prototype.format = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.adsense.AdUnitOptions.prototype.map = 1;

/**
 * @type {google.maps.ControlPosition}
 */
google.maps.adsense.AdUnitOptions.prototype.position = 1;

/**
 * @type {string}
 */
google.maps.adsense.AdUnitOptions.prototype.publisherId = 1;

// Namespace
google.maps.drawing = {};

/**
 * @constructor
 */
google.maps.drawing.DrawingControlOptions = function() {};

/**
 * @type {Array.<google.maps.drawing.OverlayType>}
 */
google.maps.drawing.DrawingControlOptions.prototype.drawingModes = 1;

/**
 * @type {google.maps.ControlPosition}
 */
google.maps.drawing.DrawingControlOptions.prototype.position = 1;

/**
 * @param {(google.maps.drawing.DrawingManagerOptions|Object.<string>)=} opt_options
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.drawing.DrawingManager = function(opt_options) {};

/**
 * @nosideeffects
 * @return {?google.maps.drawing.OverlayType}
 */
google.maps.drawing.DrawingManager.prototype.getDrawingMode = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.drawing.DrawingManager.prototype.getMap = function() {};

/**
 * @param {?google.maps.drawing.OverlayType} drawingMode
 * @return {undefined}
 */
google.maps.drawing.DrawingManager.prototype.setDrawingMode =
    function(drawingMode) {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.drawing.DrawingManager.prototype.setMap = function(map) {};

/**
 * @param {google.maps.drawing.DrawingManagerOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.drawing.DrawingManager.prototype.setOptions = function(options) {};

/**
 * @constructor
 */
google.maps.drawing.DrawingManagerOptions = function() {};

/**
 * @type {google.maps.CircleOptions|Object.<string>}
 */
google.maps.drawing.DrawingManagerOptions.prototype.circleOptions = 1;

/**
 * @type {boolean}
 */
google.maps.drawing.DrawingManagerOptions.prototype.drawingControl = 1;

/**
 * @type {google.maps.drawing.DrawingControlOptions|Object.<string>}
 */
google.maps.drawing.DrawingManagerOptions.prototype.drawingControlOptions = 1;

/**
 * @type {google.maps.drawing.OverlayType}
 */
google.maps.drawing.DrawingManagerOptions.prototype.drawingMode = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.drawing.DrawingManagerOptions.prototype.map = 1;

/**
 * @type {google.maps.MarkerOptions|Object.<string>}
 */
google.maps.drawing.DrawingManagerOptions.prototype.markerOptions = 1;

/**
 * @type {google.maps.PolygonOptions|Object.<string>}
 */
google.maps.drawing.DrawingManagerOptions.prototype.polygonOptions = 1;

/**
 * @type {google.maps.PolylineOptions|Object.<string>}
 */
google.maps.drawing.DrawingManagerOptions.prototype.polylineOptions = 1;

/**
 * @type {google.maps.RectangleOptions|Object.<string>}
 */
google.maps.drawing.DrawingManagerOptions.prototype.rectangleOptions = 1;

/**
 * @constructor
 */
google.maps.drawing.OverlayCompleteEvent = function() {};

/**
 * @type {google.maps.Marker|google.maps.Polygon|google.maps.Polyline|google.maps.Rectangle|google.maps.Circle}
 */
google.maps.drawing.OverlayCompleteEvent.prototype.overlay = 1;

/**
 * @type {google.maps.drawing.OverlayType}
 */
google.maps.drawing.OverlayCompleteEvent.prototype.type = 1;

/**
 * @enum {number|string}
 */
google.maps.drawing.OverlayType = {
  CIRCLE: 1,
  MARKER: 1,
  POLYGON: 1,
  POLYLINE: 1,
  RECTANGLE: 1
};

// Namespace
google.maps.event = {};

/**
 * @param {Object} instance
 * @param {string} eventName
 * @param {!Function} handler
 * @param {boolean=} opt_capture
 * @return {google.maps.MapsEventListener}
 */
google.maps.event.addDomListener =
    function(instance, eventName, handler, opt_capture) {};

/**
 * @param {Object} instance
 * @param {string} eventName
 * @param {!Function} handler
 * @param {boolean=} opt_capture
 * @return {google.maps.MapsEventListener}
 */
google.maps.event.addDomListenerOnce =
    function(instance, eventName, handler, opt_capture) {};

/**
 * @param {Object} instance
 * @param {string} eventName
 * @param {!Function} handler
 * @return {google.maps.MapsEventListener}
 */
google.maps.event.addListener = function(instance, eventName, handler) {};

/**
 * @param {google.maps.MapsEventListener} listener
 * @return {undefined}
 */
google.maps.event.removeListener = function(listener) {};

/**
 * @param {Object} instance
 * @param {string} eventName
 * @param {!Function} handler
 * @return {google.maps.MapsEventListener}
 */
google.maps.event.addListenerOnce = function(instance, eventName, handler) {};

(function() {
  // The "addListener" functions return a MapsEventListener object which can be
  // used to remove the listener. The problem is that the MEL "class" is not
  // published as part of the API, so we cannot tame it. We therefore return a
  // little object instead, containing a field "lis" that contains the MEL. This
  // field will appear to have a value of "undefined" to the guest code.

  var addListenerAdvice = function(addListener, self, args) {
    var targetObj = args[0];
    var eventName = args[1];
    var originalListener = args[2];
    var opt_capture = args.length > 3 && args[3];

    var delegateListener = function(event) {
      originalListener.call(this, utils.copyOneLevel(event));
    };

    return {
      lis: addListener(self, [
        targetObj,
        eventName,
        delegateListener,
        opt_capture
      ])
    };
  };

  var removeListenerAdvice = function(removeListener, self, args) {
    // If a guest crafts some weird object with a weird "lis" field and passes
    // it to us, it should not create a vuln -- we are simply asking to "remove
    // this listener" and it would just not be found and be a no-op.
    removeListener(self, [ args[0].lis ]);
  };

  google.maps.event.addDomListener.__around__ = [ addListenerAdvice ];
  google.maps.event.addDomListenerOnce.__around__ = [ addListenerAdvice ];
  google.maps.event.addListener.__around__ = [ addListenerAdvice ];
  google.maps.event.addListenerOnce.__around__ = [ addListenerAdvice ];
  google.maps.event.removeListener.__around__ = [ removeListenerAdvice ];
})();


/**
 * @param {Object} instance
 * @return {undefined}
 */
google.maps.event.clearInstanceListeners = function(instance) {};

/**
 * @param {Object} instance
 * @param {string} eventName
 * @return {undefined}
 */
google.maps.event.clearListeners = function(instance, eventName) {};

/**
 * @param {Object} instance
 * @param {string} eventName
 * @param {...*} var_args
 * @return {undefined}
 */
google.maps.event.trigger = function(instance, eventName, var_args) {};

// Namespace
google.maps.geometry = {};

// Namespace
google.maps.geometry.encoding = {};

/**
 * @param {string} encodedPath
 * @return {Array.<google.maps.LatLng>}
 */
google.maps.geometry.encoding.decodePath = function(encodedPath) {};

/**
 * @param {Array.<google.maps.LatLng>|google.maps.MVCArray.<google.maps.LatLng>} path
 * @return {string}
 */
google.maps.geometry.encoding.encodePath = function(path) {};

// Namespace
google.maps.geometry.poly = {};

/**
 * @param {google.maps.LatLng} point
 * @param {google.maps.Polygon} polygon
 * @return {boolean}
 */
google.maps.geometry.poly.containsLocation = function(point, polygon) {};

/**
 * @param {google.maps.LatLng} point
 * @param {google.maps.Polygon|google.maps.Polyline} poly
 * @param {number=} opt_tolerance
 * @return {boolean}
 */
google.maps.geometry.poly.isLocationOnEdge =
    function(point, poly, opt_tolerance) {};

// Namespace
google.maps.geometry.spherical = {};

/**
 * @param {Array.<google.maps.LatLng>|google.maps.MVCArray.<google.maps.LatLng>} path
 * @param {number=} opt_radius
 * @return {number}
 */
google.maps.geometry.spherical.computeArea = function(path, opt_radius) {};

/**
 * @param {google.maps.LatLng} from
 * @param {google.maps.LatLng} to
 * @param {number=} opt_radius
 * @return {number}
 */
google.maps.geometry.spherical.computeDistanceBetween =
    function(from, to, opt_radius) {};

/**
 * @param {google.maps.LatLng} from
 * @param {google.maps.LatLng} to
 * @return {number}
 */
google.maps.geometry.spherical.computeHeading = function(from, to) {};

/**
 * @param {Array.<google.maps.LatLng>|google.maps.MVCArray.<google.maps.LatLng>} path
 * @param {number=} opt_radius
 * @return {number}
 */
google.maps.geometry.spherical.computeLength = function(path, opt_radius) {};

/**
 * @param {google.maps.LatLng} from
 * @param {number} distance
 * @param {number} heading
 * @param {number=} opt_radius
 * @return {google.maps.LatLng}
 */
google.maps.geometry.spherical.computeOffset =
    function(from, distance, heading, opt_radius) {};

/**
 * @param {Array.<google.maps.LatLng>|google.maps.MVCArray.<google.maps.LatLng>} loop
 * @param {number=} opt_radius
 * @return {number}
 */
google.maps.geometry.spherical.computeSignedArea =
    function(loop, opt_radius) {};

/**
 * @param {google.maps.LatLng} from
 * @param {google.maps.LatLng} to
 * @param {number} fraction
 * @return {google.maps.LatLng}
 */
google.maps.geometry.spherical.interpolate = function(from, to, fraction) {};

// Namespace
google.maps.panoramio = {};

/**
 * @constructor
 */
google.maps.panoramio.PanoramioFeature = function() {};

/**
 * @type {string}
 */
google.maps.panoramio.PanoramioFeature.prototype.author = 1;

/**
 * @type {string}
 */
google.maps.panoramio.PanoramioFeature.prototype.photoId = 1;

/**
 * @type {string}
 */
google.maps.panoramio.PanoramioFeature.prototype.title = 1;

/**
 * @type {string}
 */
google.maps.panoramio.PanoramioFeature.prototype.url = 1;

/**
 * @type {string}
 */
google.maps.panoramio.PanoramioFeature.prototype.userId = 1;

/**
 * @param {(google.maps.panoramio.PanoramioLayerOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.panoramio.PanoramioLayer = function(opt_opts) {};

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.panoramio.PanoramioLayer.prototype.getMap = function() {};

/**
 * @nosideeffects
 * @return {string}
 */
google.maps.panoramio.PanoramioLayer.prototype.getTag = function() {};

/**
 * @nosideeffects
 * @return {string}
 */
google.maps.panoramio.PanoramioLayer.prototype.getUserId = function() {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.panoramio.PanoramioLayer.prototype.setMap = function(map) {};

/**
 * @param {google.maps.panoramio.PanoramioLayerOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.panoramio.PanoramioLayer.prototype.setOptions =
    function(options) {};

/**
 * @param {string} tag
 * @return {undefined}
 */
google.maps.panoramio.PanoramioLayer.prototype.setTag = function(tag) {};

/**
 * @param {string} userId
 * @return {undefined}
 */
google.maps.panoramio.PanoramioLayer.prototype.setUserId = function(userId) {};

/**
 * @constructor
 */
google.maps.panoramio.PanoramioLayerOptions = function() {};

/**
 * @type {boolean}
 */
google.maps.panoramio.PanoramioLayerOptions.prototype.clickable = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.panoramio.PanoramioLayerOptions.prototype.map = 1;

/**
 * @type {boolean}
 */
google.maps.panoramio.PanoramioLayerOptions.prototype.suppressInfoWindows = 1;

/**
 * @type {string}
 */
google.maps.panoramio.PanoramioLayerOptions.prototype.tag = 1;

/**
 * @type {string}
 */
google.maps.panoramio.PanoramioLayerOptions.prototype.userId = 1;

/**
 * @constructor
 */
google.maps.panoramio.PanoramioMouseEvent = function() {};

/**
 * @type {google.maps.panoramio.PanoramioFeature}
 */
google.maps.panoramio.PanoramioMouseEvent.prototype.featureDetails = 1;

/**
 * @type {string}
 */
google.maps.panoramio.PanoramioMouseEvent.prototype.infoWindowHtml = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.panoramio.PanoramioMouseEvent.prototype.latLng = 1;

/**
 * @type {google.maps.Size}
 */
google.maps.panoramio.PanoramioMouseEvent.prototype.pixelOffset = 1;

// Namespace
google.maps.places = {};

/**
 * @param {HTMLInputElement} inputField
 * @param {(google.maps.places.AutocompleteOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.places.Autocomplete = function(inputField, opt_opts) {};
google.maps.places.Autocomplete.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.LatLngBounds}
 */
google.maps.places.Autocomplete.prototype.getBounds = function() {};

/**
 * @nosideeffects
 * @return {google.maps.places.PlaceResult}
 */
google.maps.places.Autocomplete.prototype.getPlace = function() {};

/**
 * @param {google.maps.LatLngBounds} bounds
 * @return {undefined}
 */
google.maps.places.Autocomplete.prototype.setBounds = function(bounds) {};

/**
 * @param {google.maps.places.ComponentRestrictions} restrictions
 * @return {undefined}
 */
google.maps.places.Autocomplete.prototype.setComponentRestrictions =
    function(restrictions) {};

/**
 * @param {Array.<string>} types
 * @return {undefined}
 */
google.maps.places.Autocomplete.prototype.setTypes = function(types) {};

/**
 * @constructor
 */
google.maps.places.AutocompleteOptions = function() {};
google.maps.places.AutocompleteOptions.__super__ = Object;

/**
 * @type {google.maps.LatLngBounds}
 */
google.maps.places.AutocompleteOptions.prototype.bounds = 1;

/**
 * @type {google.maps.places.ComponentRestrictions}
 */
google.maps.places.AutocompleteOptions.prototype.componentRestrictions = 1;

/**
 * @type {Array.<string>}
 */
google.maps.places.AutocompleteOptions.prototype.types = 1;

/**
 * @constructor
 */
google.maps.places.ComponentRestrictions = function() {};
google.maps.places.ComponentRestrictions.__super__ = Object;

/**
 * @type {string}
 */
google.maps.places.ComponentRestrictions.prototype.country = 1;

/**
 * @constructor
 */
google.maps.places.PlaceDetailsRequest = function() {};
google.maps.places.PlaceDetailsRequest.__super__ = Object;

/**
 * @type {string}
 */
google.maps.places.PlaceDetailsRequest.prototype.reference = 1;

/**
 * @constructor
 */
google.maps.places.PlaceGeometry = function() {};
google.maps.places.PlaceGeometry.__super__ = Object;

/**
 * @type {google.maps.LatLng}
 */
google.maps.places.PlaceGeometry.prototype.location = 1;

/**
 * @type {google.maps.LatLngBounds}
 */
google.maps.places.PlaceGeometry.prototype.viewport = 1;

/**
 * @constructor
 */
google.maps.places.PlaceResult = function() {};
google.maps.places.PlaceResult.__super__ = Object;

/**
 * @type {Array.<google.maps.GeocoderAddressComponent>}
 */
google.maps.places.PlaceResult.prototype.address_components = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceResult.prototype.formatted_address = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceResult.prototype.formatted_phone_number = 1;

/**
 * @type {google.maps.places.PlaceGeometry}
 */
google.maps.places.PlaceResult.prototype.geometry = 1;

/**
 * @type {Array.<string>}
 */
google.maps.places.PlaceResult.prototype.html_attributions = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceResult.prototype.icon = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceResult.prototype.id = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceResult.prototype.international_phone_number = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceResult.prototype.name = 1;

/**
 * @type {number}
 */
google.maps.places.PlaceResult.prototype.rating = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceResult.prototype.reference = 1;

/**
 * @type {Array.<string>}
 */
google.maps.places.PlaceResult.prototype.types = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceResult.prototype.url = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceResult.prototype.vicinity = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceResult.prototype.website = 1;

/**
 * @constructor
 */
google.maps.places.PlaceSearchPagination = function() {};
google.maps.places.PlaceSearchPagination.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.places.PlaceSearchPagination.prototype.hasNextPage = 1;

/**
 * @return {undefined}
 */
google.maps.places.PlaceSearchPagination.prototype.nextPage = function() {};

/**
 * @constructor
 */
google.maps.places.PlaceSearchRequest = function() {};
google.maps.places.PlaceSearchRequest.__super__ = Object;

/**
 * @type {google.maps.LatLngBounds}
 */
google.maps.places.PlaceSearchRequest.prototype.bounds = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceSearchRequest.prototype.keyword = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.places.PlaceSearchRequest.prototype.location = 1;

/**
 * @type {string}
 */
google.maps.places.PlaceSearchRequest.prototype.name = 1;

/**
 * @type {number}
 */
google.maps.places.PlaceSearchRequest.prototype.radius = 1;

/**
 * @type {google.maps.places.RankBy}
 */
google.maps.places.PlaceSearchRequest.prototype.rankBy = 1;

/**
 * @type {Array.<string>}
 */
google.maps.places.PlaceSearchRequest.prototype.types = 1;

/**
 * @param {HTMLDivElement|google.maps.Map} attrContainer
 * @constructor
 */
google.maps.places.PlacesService = function(attrContainer) {};
google.maps.places.PlacesService.__super__ = Object;

/**
 * @param {google.maps.places.PlaceDetailsRequest|Object.<string>} request
 * @param {function(google.maps.places.PlaceResult, google.maps.places.PlacesServiceStatus)} callback
 * @return {undefined}
 */
google.maps.places.PlacesService.prototype.getDetails =
    function(request, callback) {};

/**
 * @param {google.maps.places.PlaceSearchRequest|Object.<string>} request
 * @param {function(Array.<google.maps.places.PlaceResult>, google.maps.places.PlacesServiceStatus,
               google.maps.places.PlaceSearchPagination)} callback
 * @return {undefined}
 */
google.maps.places.PlacesService.prototype.nearbySearch =
    function(request, callback) {};

/**
 * @param {google.maps.places.TextSearchRequest|Object.<string>} request
 * @param {function(Array.<google.maps.places.PlaceResult>, google.maps.places.PlacesServiceStatus)} callback
 * @return {undefined}
 */
google.maps.places.PlacesService.prototype.textSearch =
    function(request, callback) {};

/**
 * @enum {number|string}
 */
google.maps.places.PlacesServiceStatus = {
  INVALID_REQUEST: 1,
  OK: 1,
  OVER_QUERY_LIMIT: 1,
  REQUEST_DENIED: 1,
  UNKNOWN_ERROR: 1,
  ZERO_RESULTS: 1
};

/**
 * @enum {number|string}
 */
google.maps.places.RankBy = {
  DISTANCE: 1,
  PROMINENCE: 1
};

/**
 * @constructor
 */
google.maps.places.TextSearchRequest = function() {};
google.maps.places.TextSearchRequest.__super__ = Object;

/**
 * @type {google.maps.LatLngBounds}
 */
google.maps.places.TextSearchRequest.prototype.bounds = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.places.TextSearchRequest.prototype.location = 1;

/**
 * @type {string}
 */
google.maps.places.TextSearchRequest.prototype.query = 1;

/**
 * @type {number}
 */
google.maps.places.TextSearchRequest.prototype.radius = 1;

// Namespace
google.maps.visualization = {};

/**
 * @param {(google.maps.visualization.HeatmapLayerOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.visualization.HeatmapLayer = function(opt_opts) {};
google.maps.visualization.HeatmapLayer.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.MVCArray.<google.maps.LatLng|google.maps.visualization.WeightedLocation>}
 */
google.maps.visualization.HeatmapLayer.prototype.getData = function() {};

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.visualization.HeatmapLayer.prototype.getMap = function() {};

/**
 * @param {google.maps.MVCArray.<google.maps.LatLng|google.maps.visualization.WeightedLocation>|Array.<google.maps.LatLng|google.maps.visualization.WeightedLocation>} data
 * @return {undefined}
 */
google.maps.visualization.HeatmapLayer.prototype.setData = function(data) {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.visualization.HeatmapLayer.prototype.setMap = function(map) {};

/**
 * @constructor
 */
google.maps.visualization.HeatmapLayerOptions = function() {};
google.maps.visualization.HeatmapLayerOptions.__super__ = Object;

/**
 * @type {google.maps.MVCArray.<google.maps.LatLng>}
 */
google.maps.visualization.HeatmapLayerOptions.prototype.data = 1;

/**
 * @type {boolean}
 */
google.maps.visualization.HeatmapLayerOptions.prototype.dissipating = 1;

/**
 * @type {Array.<string>}
 */
google.maps.visualization.HeatmapLayerOptions.prototype.gradient = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.visualization.HeatmapLayerOptions.prototype.map = 1;

/**
 * @type {number}
 */
google.maps.visualization.HeatmapLayerOptions.prototype.maxIntensity = 1;

/**
 * @type {number}
 */
google.maps.visualization.HeatmapLayerOptions.prototype.opacity = 1;

/**
 * @type {number}
 */
google.maps.visualization.HeatmapLayerOptions.prototype.radius = 1;

/**
 * @constructor
 */
google.maps.visualization.WeightedLocation = function() {};
google.maps.visualization.WeightedLocation.__super__ = Object;

/**
 * @type {google.maps.LatLng}
 */
google.maps.visualization.WeightedLocation.prototype.location = 1;

/**
 * @type {number}
 */
google.maps.visualization.WeightedLocation.prototype.weight = 1;

// Namespace
google.maps.weather = {};

/**
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.weather.CloudLayer = function() {};
google.maps.weather.CloudLayer.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.weather.CloudLayer.prototype.getMap = function() {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.weather.CloudLayer.prototype.setMap = function(map) {};

/**
 * @enum {number|string}
 */
google.maps.weather.LabelColor = {
  BLACK: 1,
  WHITE: 1
};

/**
 * @enum {number|string}
 */
google.maps.weather.TemperatureUnit = {
  CELSIUS: 1,
  FAHRENHEIT: 1
};

/**
 * @constructor
 */
google.maps.weather.WeatherConditions = function() {};
google.maps.weather.WeatherConditions.__super__ = Object;

/**
 * @type {string}
 */
google.maps.weather.WeatherConditions.prototype.day = 1;

/**
 * @type {string}
 */
google.maps.weather.WeatherConditions.prototype.description = 1;

/**
 * @type {number}
 */
google.maps.weather.WeatherConditions.prototype.high = 1;

/**
 * @type {number}
 */
google.maps.weather.WeatherConditions.prototype.humidity = 1;

/**
 * @type {number}
 */
google.maps.weather.WeatherConditions.prototype.low = 1;

/**
 * @type {string}
 */
google.maps.weather.WeatherConditions.prototype.shortDay = 1;

/**
 * @type {number}
 */
google.maps.weather.WeatherConditions.prototype.temperature = 1;

/**
 * @type {string}
 */
google.maps.weather.WeatherConditions.prototype.windDirection = 1;

/**
 * @type {number}
 */
google.maps.weather.WeatherConditions.prototype.windSpeed = 1;

/**
 * @constructor
 */
google.maps.weather.WeatherFeature = function() {};
google.maps.weather.WeatherFeature.__super__ = Object;

/**
 * @type {google.maps.weather.WeatherConditions}
 */
google.maps.weather.WeatherFeature.prototype.current = 1;

/**
 * @type {Array.<google.maps.weather.WeatherForecast>}
 */
google.maps.weather.WeatherFeature.prototype.forecast = 1;

/**
 * @type {string}
 */
google.maps.weather.WeatherFeature.prototype.location = 1;

/**
 * @type {google.maps.weather.TemperatureUnit}
 */
google.maps.weather.WeatherFeature.prototype.temperatureUnit = 1;

/**
 * @type {google.maps.weather.WindSpeedUnit}
 */
google.maps.weather.WeatherFeature.prototype.windSpeedUnit = 1;

/**
 * @constructor
 */
google.maps.weather.WeatherForecast = function() {};
google.maps.weather.WeatherForecast.__super__ = Object;

/**
 * @type {string}
 */
google.maps.weather.WeatherForecast.prototype.day = 1;

/**
 * @type {string}
 */
google.maps.weather.WeatherForecast.prototype.description = 1;

/**
 * @type {number}
 */
google.maps.weather.WeatherForecast.prototype.high = 1;

/**
 * @type {number}
 */
google.maps.weather.WeatherForecast.prototype.low = 1;

/**
 * @type {string}
 */
google.maps.weather.WeatherForecast.prototype.shortDay = 1;

/**
 * @param {(google.maps.weather.WeatherLayerOptions|Object.<string>)=} opt_opts
 * @extends {google.maps.MVCObject}
 * @constructor
 */
google.maps.weather.WeatherLayer = function(opt_opts) {};
google.maps.weather.WeatherLayer.__super__ = Object;

/**
 * @nosideeffects
 * @return {google.maps.Map}
 */
google.maps.weather.WeatherLayer.prototype.getMap = function() {};

/**
 * @param {google.maps.Map} map
 * @return {undefined}
 */
google.maps.weather.WeatherLayer.prototype.setMap = function(map) {};

/**
 * @param {google.maps.weather.WeatherLayerOptions|Object.<string>} options
 * @return {undefined}
 */
google.maps.weather.WeatherLayer.prototype.setOptions = function(options) {};

/**
 * @constructor
 */
google.maps.weather.WeatherLayerOptions = function() {};
google.maps.weather.WeatherLayerOptions.__super__ = Object;

/**
 * @type {boolean}
 */
google.maps.weather.WeatherLayerOptions.prototype.clickable = 1;

/**
 * @type {google.maps.weather.LabelColor}
 */
google.maps.weather.WeatherLayerOptions.prototype.labelColor = 1;

/**
 * @type {google.maps.Map}
 */
google.maps.weather.WeatherLayerOptions.prototype.map = 1;

/**
 * @type {boolean}
 */
google.maps.weather.WeatherLayerOptions.prototype.suppressInfoWindows = 1;

/**
 * @type {google.maps.weather.TemperatureUnit}
 */
google.maps.weather.WeatherLayerOptions.prototype.temperatureUnits = 1;

/**
 * @type {google.maps.weather.WindSpeedUnit}
 */
google.maps.weather.WeatherLayerOptions.prototype.windSpeedUnits = 1;

/**
 * @constructor
 */
google.maps.weather.WeatherMouseEvent = function() {};
google.maps.weather.WeatherMouseEvent.__super__ = Object;

/**
 * @type {google.maps.weather.WeatherFeature}
 */
google.maps.weather.WeatherMouseEvent.prototype.featureDetails = 1;

/**
 * @type {string}
 */
google.maps.weather.WeatherMouseEvent.prototype.infoWindowHtml = 1;

/**
 * @type {google.maps.LatLng}
 */
google.maps.weather.WeatherMouseEvent.prototype.latLng = 1;

/**
 * @type {google.maps.Size}
 */
google.maps.weather.WeatherMouseEvent.prototype.pixelOffset = 1;

/**
 * @enum {number|string}
 */
google.maps.weather.WindSpeedUnit = {
  KILOMETERS_PER_HOUR: 1,
  METERS_PER_SECOND: 1,
  MILES_PER_HOUR: 1
};

  return {
    value: { google: google },
    customGoogleLoad: function(name, info) {
      var cb = info.callback;
      window.google.load('maps', '3.0', {
        callback: function() {
          window.google.maps.MVCArray.prototype.constructor =
              window.google.maps.MVCArray;
          window.google.maps.OverlayView.prototype.onAdd = function() {};
          window.google.maps.OverlayView.prototype.onRemove = function() {};
          window.google.maps.OverlayView.prototype.draw = function() {};
          cb();
        },
        other_params:
            'sensor=false&libraries=adsense,drawing,geometry,' +
            'panoramio,places,visualization,weather'
      });
    }
  };
});
