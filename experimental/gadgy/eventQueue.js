/**
 * Shared event queue enforcing agreed order.
 *
 * @param Q the promise API.
 * @param setTimeout privileged function with the same API as the HTML DOM
 *     setTimeout function.
 * @param modelModule the module representing the model.
 *
 * @return an event queue.
 */
'use strict';
'use cajita';

var clientCount = 0;
var events = [];
var clients = {};
var model;

var makeNotifyTask = function(clientIndex, event) {
  return function() {
    Q.post(clients[clientIndex].ref, 'notify', [event]);
  };
};

var startOutgoingFeed = function(index) {
  var next = function() {
    if (!clients[index]) { return; }
    var delay = Math.floor(Math.random() * 2000);
    setTimeout(function() {
      if (!clients[index].queue.isEmpty()) {
        clients[index].queue.dequeue()();
      }
      next();
    }, delay);
  };
  next();
};

var addClient = function(clientRef) {
  var index = clientCount++;
  clients[index] = { ref: clientRef, queue: load('queue')({}) };
  for (var i = 0; i < events.length; i++) {
    clients[index].queue.enqueue(makeNotifyTask(index, events[i]));
  }
  startOutgoingFeed(index);
  return index;
};

var removeClient = function(index) {
  clients[index] = undefined;  
};

var applyEventToModel = function(event) {
  model[event.op].apply(cajita.USELESS, event.args);
};

var recoverModel = function() {
  model = modelModule({ publish: function() { } });
  for (var i = 0; i < events.length; i++) {
    applyEventToModel(events[i]);
  }
};

var enqueue = function(clientIndex, event) {
  var r = Q.defer();

  try {

    applyEventToModel(event);

    // The event is good, so save it to agreed order
    events.push(event);

    // Resolve the promise to the initiating client
    clients[clientIndex].queue.enqueue(function() {
      r.resolve(cajita.USELESS);
    });

    // Notify all clients of new message
    for (var i = 0; i < clientCount; i++) {
      if (clients[i]) {
        clients[i].queue.enqueue(makeNotifyTask(i, event));
      }
    }

  } catch (e) {

    recoverModel();

    // Reject the promise to the initiating client
    clients[clientIndex].queue.enqueue(function() {
      r.resolve(Q.reject(e));
    });
  }

  return r.promise;
};

recoverModel();

Q.ref({
  openConnection: function(clientRef) {
    var clientIndex = addClient(clientRef);
    return Q.ref({
      enqueue: function(event) { return enqueue(clientIndex, event); },
      close: function() { removeClient(clientIndex); }
    });
  }
});
