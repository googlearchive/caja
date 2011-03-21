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

/**
 * Root Photon object. Responsible for instantiating views of
 * objects on demand, and acts as a powerbox for mediating the
 * transfer of objects between mutually suspicious components.
 *
 * @param instantiateInTameElement a tamed function that takes
 *     a Domita tamed element, an HTML module URL and an optional
 *     map of outer variables and instantiates the module within
 *     the provided element.
 * @param log a tamed function that logs messages to the console.
 */

'use strict';

var self = {};

var introducer = load('introducer')({ 
  photon: self
});

/**
 * Instantiate an HTML module in an HTML element. Clears out the supplied
 * element and replaces its contents with the contents of the HTML module.
 * @param element an HTML element, such as a <span> or <div>.
 * @param moduleUrl the URL of an HTML module; a URL ending in ".html".
 * @param outers an object literal containing additional outers to add
 *     as bindings for the free variables of the module.
 * @return the completion value of the scripts in the HTML module.
 */
self.instantiateInElement = function(element, moduleUrl, outers) {
  instantiateInTameElement(element, moduleUrl, outers);
};

self.log = function(msg) {
  log(String(msg));
};

self.makeDragSource = introducer.makeDragSource;
self.makeDropTarget = introducer.makeDropTarget;

/* return */ Object.freeze({
  publicFacet: Object.freeze(self),
  friendFacet: Object.freeze({
    createIntroducerView: introducer.createView
  })
});