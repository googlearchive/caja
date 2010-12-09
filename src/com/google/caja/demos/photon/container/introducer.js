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
 * Introducer which mediates between mutually suspicious objects
 * in the code based on user interactions.
 *
 * @param photon the Photon object.
 */
'use strict';

var states = Object.freeze({
  HIDDEN:   'hidden',
  DISABLED: 'disabled',
  ENABLED:  'enabled',
  ACTIVE:   'active'
});

var introducerEvents = load('events')({
  names: [ 'highlight', 'active', 'providerVisible' ]
});

var makeDndController = function(
    initiators, receptors, 
    activeInitiatorSlot, activeReceptorSlot,
    rendezvous) {
  var cancel = function() {
    initiators.each(function(o) {
      o.setState(states.ENABLED);
    });
    receptors.each(function(o) {
      o.setState(states.ENABLED);
    });
    activeInitiatorSlot.value = undefined;
    introducerEvents.fire('active');
  };

  var onInitiatorActivated = function(initiator) {
    if (activeReceptorSlot.value) { return; }
    initiators.each(function(o) {
      if (o === initiator) {
        o.setState(states.ACTIVE);
      } else {
        o.setState(states.DISABLED);
      }
    });
    receptors.each(function(o) {
      if (o.dataType === initiator.dataType) {
        o.setState(states.ENABLED);
      } else {
        o.setState(states.HIDDEN);
      }
    });
    activeInitiatorSlot.value = initiator;
    introducerEvents.fire('active');
  };

  var onReceptorActivated = function(receptor) {
    if (!activeInitiatorSlot.value) { return; }
    rendezvous(activeInitiatorSlot.value, receptor);
    cancel();
  };

  return Object.freeze({
    onInitiatorActivated: onInitiatorActivated,
    onReceptorActivated: onReceptorActivated,
    cancel: cancel
  });
};

var highlightedDnd = undefined;

var makeDndModel = function(
    dndList, text, 
    myInitiatorController, myReceptorController, 
    dataType, description, handler) {
  myInitiatorController.cancel();
  myReceptorController.cancel();

  var events = load('events')({
    names: [ 'state', 'destroyed' ]
  });

  var state = states.ENABLED;

  // The "friend" interface is for collaborators
  var iFriend = {
    dataType: dataType,
    text: text,
    description: description,
    handler: handler,
    listen: events.listen,
    unlisten: events.unlisten,
    getState: function() { 
      return state; 
    },
    setState: function(newState) {
      state = newState;
      events.fire('state');
    },
    activate: function() {
      if (state === states.ACTIVE) {
        myInitiatorController.cancel();
      } else if (state === states.ENABLED) {
        myInitiatorController.onInitiatorActivated(iFriend);
        myReceptorController.onReceptorActivated(iFriend);
      }
    },
    createView : function(anElement) {
      return photon.instantiateInElement(anElement, 'dndView.html', {
        photon: photon,
        model: iFriend
      });
    },
    highlight: function(isHighlighted) {
      if (state == states.ENABLED || state == states.ACTIVE) {
        highlightedDnd = isHighlighted ? iFriend : undefined;
        introducerEvents.fire('highlight');
      }
    },
    destroy: function() {
      dndList.remove(iFriend);
      iFriend.setState(states.HIDDEN);
      myInitiatorController.cancel();
      myReceptorController.cancel();
      events.fire('destroyed');
    }
  };

  dndList.push(Object.freeze(iFriend));

  // The public interface is for suspect (non-Photon) components
  return Object.freeze({
    createView: iFriend.createView,
    destroy: iFriend.destroy,
    listen: iFriend.listen,
    getState: iFriend.getState
  });
};

var dragSources = load('list')({});
var dropTargets = load('list')({});

var activeDragSourceSlot = { value: undefined };
var activeDropTargetSlot = { value: undefined };

var doRendezvous = function(dragSource, dropTarget) {
  var dragSourceCb = dragSource.handler;
  var dropTargetCb = dropTarget.handler;
  try {
    var value = dragSourceCb.call(undefined);
    dropTargetCb.call(undefined, value);
  } catch (e) {
    cajaVM.log('Exception completing drag/drop: ' + e);
  }
};

var dragSourceController = makeDndController(
  dragSources, dropTargets,
  activeDragSourceSlot, activeDropTargetSlot,
  function(dragSource, dropTarget) {
    doRendezvous(dragSource, dropTarget);
  });

var dropTargetController = makeDndController(
  dropTargets, dragSources,
  activeDropTargetSlot, activeDragSourceSlot,
  function(dropTarget, dragSource) {
    doRendezvous(dragSource, dropTarget);
  });

/**
 * Make a drag source.
 *
 * @param dataType the data type string.
 * @param description the description explaining the claim.
 * @param handler a zero-argument function that returns the
 *     object to be dragged from this source.
 * @return a drag source, which has a createView() and a
 *     destroy() method.
 */
var makeDragSource = function(dataType, description, handler) {
  return makeDndModel(
      dragSources,
      "&#x25C9;&nbsp;&#x25B6;",
      dragSourceController,
      dropTargetController,
      dataType,
      description,
      handler);
};

/**
 * Make a drop target.
 *
 * @param dataType the data type string.
 * @param description the description explaining the claim.
 * @param handler a one-argument function that accepts the
 *     object being dragged into this target.
 * @return a drop target, which has a createView() and a
 *     destroy() method.
 */
var makeDropTarget = function(dataType, description, handler) {
  return makeDndModel(
      dropTargets,
      "&#x25B7;&nbsp;&#x25CE;",
      dropTargetController,
      dragSourceController,
      dataType,
      description,
      handler);
};

var providers = load('list')({});

/**
 * Put a source into the introducer's chrome.
 *
 * @param dataTypes an array of data type strings.
 * @param description the description explaining the claim.
 * @param createView a view creator function that creates a
 *     view of the provider when requested.
 * @return an opaque provider object, which has a destroy() method.
 */
var makeProvider = function(dataTypes, description, createView) {

  var provider = Object.freeze({
    dataTypes: dataTypes,
    description: description,
    createView: createView
  });

  providers.push(provider);

  return Object.freeze({
    destroy: function() {
      providers.remove(provider);
    }
  });
};

/**
 * Create a view of this Introducer. This essentially creates the
 * "trusted path" chrome which the Introducer uses to signal the user
 * about the state of the current operation.
 *
 * @param anElement a DOM element in which to instantiate the view.
 */
var createView = function(anElement) {
  return photon.instantiateInElement(anElement, 'introducerChrome.html', {
    photon: photon,
    model: Object.freeze({
      getHighlighted: function() {
        return highlightedDnd;
      },
      isActive: function() {
        return activeDragSourceSlot.value || activeDropTargetSlot.value;
      },
      // The introducer view should not manipulate the providers list directly
      providers: providers.asReadOnly,
      cancel: function() {
        dragSourceController.cancel();
        dropTargetController.cancel();
      },
      listen: introducerEvents.listen
    })
  });
};

/* return */ Object.freeze({
  states: states,
  makeDragSource: makeDragSource,
  makeDropTarget: makeDropTarget,
  /* TODO(ihab.awad): makeProvider: makeProvider, */ 
  createView: createView
});
