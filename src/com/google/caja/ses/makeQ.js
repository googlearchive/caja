// Copyright (C) 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview Implements the EcmaScript
 * http://wiki.ecmascript.org/doku.php?id=strawman:concurrency
 * strawman, securely when run on a Caja or SES platform.
 *
 * //provides ses.makeQ
 * @author Mark S. Miller, based on earlier designs by Tyler Close,
 * Kris Kowal, and Kevin Reid.
 * @overrides ses
 * @requires WeakMap, cajaVM
 */

var ses;

(function() {
   "use strict";

   if (ses && !ses.ok()) { return; }

   var bind = Function.prototype.bind;
   // See
   // http://wiki.ecmascript.org/doku.php?id=conventions:safe_meta_programming
   var uncurryThis = bind.bind(bind.call);

   var bindFn = uncurryThis(bind);
   var applyFn = uncurryThis(bind.apply);
   var sliceFn = uncurryThis([].slice);
   var toStringFn = uncurryThis({}.toString);

   var freeze = Object.freeze;
   var constFunc = cajaVM.constFunc;
   var def = cajaVM.def;
   var is = cajaVM.is;


   /**
    * Tests if the presumably thrown error is simply signaling the end
    * of a generator's iteration.
    *
    * <p>TODO(erights): Find some way to accomodate Firefox's
    * pre-harmony iterators, at least for pre-harmony testing. Take a
    * look at how Kris Kowal's q library handles this.
    *
    * <p>See
    * http://wiki.ecmascript.org/doku.php?id=harmony:iterators#stopiteration
    */
   function isStopIteration(err) {
     return toStringFn(err) === '[object StopIteration]';
   }


   /**
    * Makes a Q object which uses the provided setTimeout function to
    * postpone events to future turns.
    */
   function makeQ(setTimeout) {

     /**
      * Maps from promises to their handlers.
      *
      * <p>All handlers and the "handlers" map must never escape. A
      * handler holds <i>all</i> the state of its promise, serving as a
      * private state record, but more flexible in two ways we take
      * advantage of:
      * <ul>
      * <li>A promise can change state and behavior by changing which
      * handler it is associated with.
      * <li>More than one promise can share the same handler, making them
      * essentially identical except for identity.
      * </ul>
      */
     var handlers = WeakMap();

     /**
      * Among objects, all and only promises have handlers.
      */
     function isPromise(value) {
       if (value !== Object(value)) { return false; }
       return !!handlers.get(value);
     }

     /**
      * Get the "best" handler associated with this promise, shortening
      * "became" chains in the process.
      */
     function handle(prom) {
       var handler = handlers.get(prom);
       if (!handler || !handler.became) { return handler; }
       while (handler.became) {
         handler = handler.became;
       }
       handlers.set(prom, handler);
       return handler;
     }

     /**
      * Run the thunk later in its own turn, but immediately return a
      * promise for what its outcome will be.
      */
     function postpone(thunk) {
       return promise(function(resolve, reject) {
         setTimeout(function() {
           var value;
           try {
             resolve(thunk());
           } catch (reason) {
             reject(reason);
           }
         }, 0);
       });
     }

     /**
      * To deliver a messenger to a handler is to eventually ask the handler to
      * dispatch on the meesage carried by the messenger, and to return the
      * outcome to the messenger's resolver.
      *
      * <p>A messenger is a record with
      * <ul>
      * <li>OP - the name of one of the concrete handler methods spelled
      * in all upper case, which is currently GET, POST, PUT, DELETE, and
      * THEN. We might add the other HTTP verbs, HEAD and OPTION. And we
      * might add the other needed reference bookkeeping operation,
      * WHEN_REJECTED, so that a farPromise can notify if it later becomes
      * rejected.
      * <li>args - the array of arguments to use when calling the
      * handler's OP method.
      * <li>resolve - a resolver function, for reporting the outcome of
      * eventually asking the handler to dispatch the messenger's message.
      * </ul>
      * The messenger's message consists of its OP and args.
      *
      * <p>A handler's dispatch method may deliver the messenger's message
      * to this handler, buffer the messenger for later, or ask another
      * handler to dispatch it.
      */
     function deliver(handler, messenger) {
       var value;
       setTimeout(function() {
         try {
           value = handler.dispatch(messenger.OP,
                                    messenger.args);
         } catch (reason) {
           value = rejected(reason);
         }
         messenger.resolve(value);
       }, 0);
     }


     /*************************************************************************
      * A near promise's resolution is a non-promise.
      *
      * <p>"prom" must be a near promise whose handler is this
      * handler. "target" must be a non-promise. The NearHandler
      * constructor does not actually use its "prom" argument, but it
      * is there to support the general HandlerConstructor API as assumed
      * by the HiddenPromise constructor.
      */
     function NearHandler(prom, target) {
       this.target = target;
     }
     NearHandler.prototype = {

       stateName: 'near',

       shorten: function() { return this.target; },

       dispatch: function(OP, args) {
         return applyFn(this[OP], this, args);
       },

       POST: function(opt_name, args) {
         var target = this.target;
         if (opt_name === null || opt_name === void 0) {
           return applyFn(target, void 0, args);
         } else {
           return applyFn(target[opt_name], target, args);
         }
       },

       GET: function(name)        { return this.target[name]; },
       PUT: function(name, value) { this.target[name] = value; return void 0; },
       DELETE: function(name)     { return delete this.target[name]; },

       /** Just invoke sk, the success continuation */
       THEN:function(sk, fk)      { return sk(this.target); }
     };

     /**
      * Returns the promise form of value.
      *
      * <p>If value is already a promise, return it. Else if it is
      * null, undefined, or a primitive value, then wrap it in a
      * promise that is already resolved to value, even if
      * Object(value) would be considered thenable. Else return a
      * promise now for the result of testing the thenability of value
      * in a separate turn. To prevent plan interference attacks, this
      * testing must be in a separate turn.
      *
      * <p>In that later turn, if value is a thenable, then use its
      * "then" behavior to determine the resolution of the previously
      * returned promise. Else, fulfil the previously returned promise
      * with value.
      */
     function Q(value) {
       if (isPromise(value)) { return value; }
       if (value !== Object(value)) {
         return new HiddenPromise(NearHandler, value);
       }
       return promise(function(resolve, reject) {
         setTimeout(function() {
           if (typeof value.then === 'function') {
             try {
               value.then(resolve, reject);
             } catch (reason) {
               reject(reason);
             }
           } else {
             resolve(new HiddenPromise(NearHandler, value));
           }
         }, 0);
       });
     }


     /*************************************************************************
      * A rejected promise will never deliver any operations because of the
      * stated reason.
      *
      * <p>"prom" must be a rejected promise whose handler is this handler.
      * "reason" will typically be a thrown Error. An originally rejected
      * promise's resolution is itself. A rejected promise's resolution is
      * a rejected promise just like itself, except possibly for identity.
      */
     function RejectedHandler(prom, reason) {
       this.promise = prom;
       this.reason = reason;

       this.stateName = 'rejected (' + reason + ')';
     }
     RejectedHandler.prototype = {

       shorten: function() { return this.promise; },

       dispatch: function(OP, args) {
         if (OP === 'THEN') { return this.THEN(args[0], args[1]); }
         return this.promise;
       },

       /** Just invoke fk, the failure continuation */
       THEN:  function(sk, fk) { return fk(this.reason); }
     };

     /**
      * Rejected makes a new rejected promise which reports "reason" as the
      * alleged reason why it is rejected.
      *
      * <p>Does a def(reason), which (transitively under SES) freezes
      * reason.
      */
     function rejected(reason) {
       reason = def(reason);
       try {
         return new HiddenPromise(RejectedHandler, reason);
       } catch (err) {
         // Workaround undiagnosed intermittent FF bug. TODO(erights):
         // isolate and report.
         // debugger;
         reason = 'Failing to report error for mysterious reasons';
       }
       return new HiddenPromise(RejectedHandler, reason);
     }

     /**
      * Resolving a promise to itself rejects all promises in the loop
      * with the reason being an Error complaining of a vicious promise
      * cycle.
      */
     var theViciousCycle;
     var theViciousCycleHandler;


     /*************************************************************************
      * The handler for a local pending promise, as made by promise().
      *
      * <p>"prom" must be a local pending promise.
      */
     function PendingHandler(prom, queue) {
       this.promise = prom;
       this.queue = queue;
     }
     PendingHandler.prototype = {

       stateName: 'pending',

       shorten: function() { return this.promise; },

       dispatch: function(OP, args) {
         return promise(function(resolve) {
           this.queue({
             resolve: resolve,
             OP: OP,
             args: args
           });
         }.bind(this));
       }
     };

     /**
      * Have all promises which were using oldHandler as their handler
      * instead use newPromise's handler as their handler.
      *
      * <p>oldHandler must be a become-able kind of handler, i.e., a
      * PendingHandler, FarHandler, or RemoteHandler. It also must
      * not yet have become anything.
      */
     function become(oldHandler, newPromise) {
       oldHandler.became = theViciousCycleHandler;
       var newHandler = handle(newPromise);
       oldHandler.became = newHandler;
       return newHandler;
     }

     /**
      * User-visible function for making a promise
      */
     function promise(func) {
       var buffer = [];
       function queue(messenger) {
         if (buffer) {
           buffer.push(messenger);
         } else {
           // This case seems to have happened once but I have not yet
           // been able to reproduce it.
           debugger;
         }
       }
       var resultP = new HiddenPromise(PendingHandler, queue);
       var handler = handle(resultP);

       function resolve(value) {
         if (!buffer) { return; } // silent
         // assert(handler === handle(resultP)) since, the only way this
         // becomes untrue is by a prior call to resolve, which will
         // clear buffer, so we would never get here.

         var buf = buffer;
         buffer = void 0;

         var newHandler = become(handler, Q(value));
         handle(resultP); // just to shorten
         handler = void 0; // A dead resolver should not retain dead objects
         resultP = void 0;

         var forward;
         if (newHandler instanceof PendingHandler) {
           // A nice optimization but not strictly necessary.
           forward = newHandler.queue;
         } else {
           forward = bindFn(deliver, void 0, newHandler);
         }

         for (var i = 0, len = buf.length; i < len; i++) {
           forward(buf[i]);
         }
       }

       function reject(reason) {
         resolve(rejected(reason));
       }

       // compat with sensible subset of DOMFuture construction API
       resolve.resolve = resolve;
       resolve.reject = reject;

       try {
         func(constFunc(resolve), constFunc(reject));
       } catch (reason) {
         reject(reason);
       }
       return resultP;
     }


     /*************************************************************************
      * A far promise is a fulfilled promise to a possibly remote
      * object whose behavior is locally represented by a farDispatch
      * function.
      *
      * <p>The farDispatch function acts like the dispatch method of the
      * FarHandler, except that it gets only the HTTP verb operations,
      * not the THEN operation.
      *
      * <p>To support the reporting of partition, for those farDispatches
      * whose failure model makes partition visible, a far promise may
      * become rejected.
      */
     function FarHandler(prom, dispatch) {
       this.promise = prom;
       this.dispatch = dispatch;
     }
     FarHandler.prototype = {
       stateName: 'far',

       shorten: function() { return this.promise; },

       /** Just invoke sk, the success continuation */
       THEN: function(sk, fk) { return sk(this.promise); }
     };

     function makeFar(farDispatch, nextSlotP) {
       var farPromise;

       function dispatch(OP, args) {
         if (OP === 'THEN') { return farPromise.THEN(args[0], args[1]); }
         return farDispatch(OP, args);
       }
       farPromise = new HiddenPromise(FarHandler, dispatch);


       function rejectFar(reason) {
         // Note that a farPromise is resolved, so its shorten()
         // identity must be stable, even when it becomes
         // rejected. Thus, we do not become(farHandler, rejected(reason))
         // or become(farHandler, nextSlot.value). Rather, we switch
         // to a new rejected handler whose promise is this same
         // farPromise.
         var farHandler = handle(farPromise);
         var rejectedHandler = new RejectedHandler(farPromise, reason);
         handlers.set(farPromise, rejectedHandler);
         become(farHandler, farPromise);
       }

       Q(nextSlotP).get('value').then(function(v) {
         rejectFar(new Error(
             'A farPromise can only further resolve to rejected'));
       }, rejectFar).end();

       return farPromise;
     };


     /*************************************************************************
      * A remote promise is a pending promise with a possibly remote
      * resolver, where the behavior of sending a message to a remote
      * promise may be to send the message to that destination (e.g. for
      * promise pipelining). The actual behavior is locally represented
      * by a remoteDispatch function.
      *
      * <p>The remoteDispatch function acts like the dispatch method of the
      * RemoteHandler, except that it gets only the HTTP verb operations,
      * not the THEN operation. Instead, the THEN operations are
      * forwarded on to the promise for the remote promise's next
      * resolution.
      */
     function RemoteHandler(prom, dispatch) {
       this.promise = prom;
       this.dispatch = dispatch;
     }
     RemoteHandler.prototype = {
       stateName: 'pending remote',

       shorten: function()       { return this.promise; }
     };

     function makeRemote(remoteDispatch, nextSlotP) {
       var remotePromise;

       function dispatch(OP, args) {
         if (OP === 'THEN') {
           // Send "then"s to the remote promise's eventual next
           // resolution. This has the effect of buffering them locally
           // until there is such a next resolution.
           return Q(nextSlotP).get('value').then(args[0], args[1]);
         }
         return remoteDispatch(OP, args);
       }
       remotePromise = new HiddenPromise(RemoteHandler, remoteDispatch);


       Q(nextSlotP).then(function(nextSlot) {
         become(handle(remotePromise, Q(nextSlot.value)));
       }, function(reason) {
         become(handle(remotePromise, rejected(reason)));
       }).end();

       return remotePromise;
     };


     /*************************************************************************
      * <p>A promise is an object which represents a reference to some
      * other object, where the other object might be elsewhere (e.g., on
      * a remote machine) or elsewhen (e.g., not yet computed).
      *
      * <p>The HiddenPromise constructor must not escape. Clients of this module
      * use the Q function to make promises from non-promises. Since
      * HiddenPromise.prototype does escape, it must not point back at
      * HiddenPromise.
      *
      * <p>The various methods on a genuine promise never execute "user
      * code", i.e., possibly untrusted client code, during the immediate
      * call to the promise method, protecting the caller from plan
      * interference hazards. Rather, any such execution happens on later
      * turns scheduled by the promise method. Except for "end", which
      * returns nothing, all other promise methods return genuine
      * promises, enabling safe chaining.
      */
     function HiddenPromise(HandlerMaker, arg) {
       var handler = new HandlerMaker(this, arg);
       handlers.set(this, handler);
       freeze(this);
     }
     promise.prototype = HiddenPromise.prototype = {
       constructor: promise,

       toString: function() {
         return '[' + handle(this).stateName + ' promise]';
       },
       post: function(opt_name, args) {
         var that = this;
         return postpone(function() {
           return handle(that).dispatch('POST', [opt_name, args]);
         });
       },
       send: function(opt_name, var_args) {
         return applyFn(this.post, this, [opt_name, sliceFn(arguments, 1)]);
       },
       fcall: function(var_args) {
         return applyFn(this.post, this, [void 0, sliceFn(arguments, 0)]);
       },
       get: function(name) {
         var that = this;
         return postpone(function() {
           return handle(that).dispatch('GET', [name]);
         });
       },
       put: function(name, value) {
         var that = this;
         return postpone(function() {
           return handle(that).dispatch('PUT', [name, value]);
         });
       },
       'delete': function(name) {
         var that = this;
         return postpone(function() {
           return handle(that).dispatch('DELETE', [name]);
         });
       },
       then: function(callback, opt_errback) {
         var errback = opt_errback || function(reason) { throw reason; };
         var done = false;

         /** success continuation */
         function sk(value) {
           if (done) { throw new Error('This "then" already done.'); }
           done = true;
           return postpone(function() { return callback(value); });
         }
         /** failure continuation */
         function fk(reason) {
           if (done) { throw new Error('This "then" already done.'); }
           done = true;
           return postpone(function() { return errback(reason); });
         }

         var that = this;
         return postpone(function() {
           return handle(that).dispatch('THEN', [sk, fk]);
         });
       },
       end: function() {
         this.then(function(){},
                   function(reason) {
           // So if this setTimeout logs throws that terminate a turn, it
           // will also log this reason.
           setTimeout(function() { throw reason; }, 0);
         });
       }
     };
     def(promise);

     function shorten(target1) {
       var optHandler = handle(target1);
       if (!optHandler) { return target1; }
       return optHandler.shorten();
     }

     // Will be relevant for remote
     //var passByCopies = WeakMap();
     function passByCopy(obj) {
       freeze(obj);
       //passByCopies.set(obj, true);
       return obj;
     }

     //////////////////////////////////////////////////////////////////////////

     Q.rejected = rejected;
     Q.promise = promise;
     Q.isPromise = isPromise;

     Q.makeFar = makeFar;

     Q.makeRemote = makeRemote;

     Q.shorten = shorten;

     theViciousCycle = rejected(new Error('vicious promise cycle'));
     theViciousCycleHandler = handle(theViciousCycle);

     Q.passByCopy = passByCopy;

     //////////////////////////////////////////////////////////////////////////
     // Non-fundamental conveniences below.

     Q.delay = function(millis, opt_answer) {
       return promise(function(resolve) {
         setTimeout(function() { resolve(opt_answer); }, millis);
       });
     };

     Q.race = function(answerPs) {
       return promise(function(resolve,reject) {
         answerPs.forEach(function(answerP) {
           Q(answerP).then(resolve,reject);
         });
       });
     };

     Q.all = function(answerPs) {
       var countDown = answerPs.length;
       var answers = [];
       if (countDown === 0) { return Q(answers); }
       return promise(function(resolve,reject) {
         answerPs.forEach(function(answerP, index) {
           Q(answerP).then(function(answer) {
             answers[index] = answer;
             if (--countDown === 0) {
               // Note: Only a shallow freeze(), not a def().
               resolve(Object.freeze(answers));
             }
           }, reject);
         });
       });
     };

     Q.join = function(var_args) {
       var args = sliceFn(arguments, 0);
       var len = args.length;
       if (len === 0) {
         return Q.rejected(new Error('No references joined'));
       }
       return Q.all(args).then(function(fulfilleds) {
         var first = fulfilleds[0];
         for (var i = 1; i < len; i++) {
           if (!is(first, fulfilleds[i])) {
             throw new Error("not the same");
           }
         }
         // is() guarantees there's no observable difference between
         // first and any of the others
         return first;
       });
     };

     Q.memoize = function(oneArgFuncP, opt_memoMap) {
       var memoMap = opt_memoMap || WeakMap();

       function oneArgMemo(arg) {
         var resultP = memoMap.get(arg);
         if (!resultP) {
           resultP = Q(oneArgFuncP).send(void 0, arg);
           memoMap.set(arg, resultP);
         }
         return resultP;
       }
       return constFunc(oneArgMemo);
     };

     /**
      * On platforms with generators (either ES-Harmony or existing
      * FF), this can be used with generators to express <a href=
      * "http://wiki.ecmascript.org/doku.php?id=strawman:async_functions"
      * >Asynchronous Functions</a>. Please see that page for further
      * explanation.
      */
     Q.async = function(generatorFunc) {
       function asyncFunc(var_args) {
         var args = sliceFn(arguments, 0);
         var generator = generatorFunc.apply(this, args);
         var callback = continuer.bind(void 0, 'send');
         var errback = continuer.bind(void 0, 'throw');

         function continuer(verb, valueOrErr) {
           var promisedValue;
           try {
             promisedValue = generator[verb](valueOrErr);
           } catch (err) {
             if (isStopIteration(err)) { return Q(err.value); }
             return Q.rejected(err);
           }
           return Q(promisedValue).then(callback, errback);
         }

         return callback(void 0);
       }
       return constFunc(asyncFunc);
     };

     Q.defer = function() {
       var deferred = {};
       deferred.promise = promise(function(resolve, reject) {
         deferred.resolve = resolve;
         deferred.reject = reject;
       });
       return freeze(deferred);
     };

     return def(Q);
   };
   def(makeQ);
   ses.makeQ = makeQ;
 })();
