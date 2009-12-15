/**
 * Guardian holding a reference to a deterministic model.
 *
 * @param moduleId the module name for the model.
 * @param queue a shared event queue object.
 * @param Q the promise API.
 *
 * @return a guardian object allowing invocations on the model and providing a
 *     simple pub/sub event channel that allows the model to publish
 *     events without leaking nondeterminism to the model. The return value is
 *     a promise for a guardian that gets resolved when everything is loaded.
 */
'use strict';
'use cajita';

var listeners = {};
var diagnosticListeners = [];
var events = [];

var listen = function(name, callback) {
  if (!listeners[name]) { listeners[name] = []; }
  listeners[name].push(callback);
};

var publish = function(name, event) {
  // TODO(ihab.awad): Ensure 'event' does not allow listeners to leak ND back to model
  if (!listeners[name]) { return; }
  for (var i = 0; i < listeners[name].length; i++) {
    // The callback may be a Valija Disfunction; use '.call' to call it
    listeners[name][i].call(cajita.USELESS, event);
  }
};

var broadcastDiagnostic = function(text) {
  for (var i = 0; i < diagnosticListeners.length; i++) {
    // The callback may be a Valija Disfunction; use '.call' to call it
    diagnosticListeners[i].call(cajita.USELESS, { text: text });
  }
};

Q.when(load.async(moduleId), function(modelModule) {

  var model = modelModule({ publish: publish });

  var applyToModel = function(model, op, args, log) {
    var before = model.toString();
    var result;
    var ex;
    try {
      result = model[op].apply(cajita.USELESS, args);
    } catch (e) {
      ex = e;
    }
    var after = model.toString();
    if (log) {
      broadcastDiagnostic(
          '[' + before + '] -> ' +
          op + '(' + args + ') -> ' +
          '[' + after + '] ' +
          (ex ? ('threw: "' + ex + '"') : ''));
    }
    return result;
  }

  var read = function(op, args) {
    var tempModel = modelModule({ publish: function() { } });
    for (var i = 0; i < events.length; i++) {
      applyToModel(tempModel, events[i].op, events[i].args);
    }
    return applyToModel(tempModel, op, args);
  };

  var queueClient = Q.ref({
    notify: function(event) {
      events.push(event);
      applyToModel(model, event.op, event.args, true);
    }
  });

  return Q.when(Q.post(queue, 'openConnection', [queueClient]), function(connection) {

    return cajita.freeze({
      representativeFacet: cajita.freeze({
        write: function(op, args) {
          return Q.post(connection, 'enqueue', [{ op: op, args: args }]);
        },
        read: read,
        listen: listen
      }),
      diagnosticFacet: cajita.freeze({
        listen: function(name, listener) {
          diagnosticListeners.push(listener);
        }
      })
    });
  });
});
