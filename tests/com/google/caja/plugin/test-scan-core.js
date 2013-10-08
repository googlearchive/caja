// Copyright (C) 2013 Google Inc.
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
 * A generic tool for walking the reference graph looking for problems.
 * To use it, create a new scanning.Scanner({...}), configure its tables to
 * understand the objects it will encounter, and then .scan().
 *
 * See test-scan-guest.js for the specific application to the Caja environment.
 *
 * @author kpreid@switchb.org
 *
 * @requires cajaVM
 * @requires window
 * @requires WeakMap, JSON, setTimeout
 * @provides scanning
 */

var scanning;  // exports
(function() {
  "use strict";

  function isPrefix(prefix, specimen) {
    return String(specimen).indexOf(prefix) === 0;
  }
  function isSuffix(suffix, specimen) {
    specimen = String(specimen);
    var i = specimen.lastIndexOf(suffix);
    return i !== -1 && i === specimen.length - suffix.length;
  }
  function optional(a, b) {
    return a === undefined ? b : a;
  }

  function isNativeFunction(f) {
    try {
      return (/^[^{}]*\{\s*\[native code\]\s*}\s*$/
          .test(Function.prototype.toString.call(f)));
    } catch (e) {
      // ES5/3 Function.prototype is semi-toxic and throws in this case.
      //
      // Also, Function.prototype is not a native function in the sense we care
      // about (it is exercised separately), and also does not throw, so we
      // don't care about the return value.
      return false;
    }
  }
  // self-test
  isNativeFunction(Function.prototype);  // don't care, must not throw
  if (!isNativeFunction(Math.sin)) {  // arbitrary boring native function
    throw new Error('isNativeFunction: failed on Math.sin');
  }
  if (isNativeFunction(function() {})) {
    throw new Error('isNativeFunction: failed on non-native');
  }

  var trueApply = (function() {
    function makeApplier(length) {
      var args = '';
      for (var i = 0; i < length; i++) {
        var name = 'as[' + i + ']';
        args += args !== '' ? ',' + name : name;
      }
      return Function('f,as', 'return f(' + args + ');');
    }
    var appliers = [];
    /** Apply fn to args without using Function.prototype.apply. */
    return function trueApply(fn, args) {
      var length = args.length;
      return (
          appliers[length] || (appliers[length] = makeApplier(length))
        )(fn, args);
    };
  }());

  /**
   * Tools for generating combinations.
   *
   * Each returns a function which calls the provided callback with each of
   * the generated values in succession.
   */
  var G = cajaVM.def({
    /* Itself a generator of no values. */
    none: function none(callback) {},
    /** All of the provided values. */
    value: function value(var_args) {
      var a = Array.prototype.slice.call(arguments);
      return function GvalueGen(callback) {
        for (var i = 0; i < a.length; i++) {
          callback(a[i]);
        }
      };
    },
    /** One value, computed on demand. */
    lazyValue: function lazyValue(thunk) {
      return function GlazyValueGen(callback) {
        callback(thunk());
      };
    },
    /** All of the provided generators. */
    any: function any(var_args) {
      var a = Array.prototype.slice.call(arguments);
      return function GanyGen(callback) {
        for (var i = 0; i < a.length; i++) {
          a[i](callback);
        }
      };
    },
    /** All combinations of the generators, each in a tuple. */
    tuple: function tuple(iter1, var_rest) {
      if (arguments.length === 0) {
        return function GtupleBaseGen(callback) {
          callback([]);
        };
      } else {
        var iterN = G.tuple.apply(undefined,
            Array.prototype.slice.call(arguments, 1));
        return function GtupleGen(callback) {
          iter1(function GtupleFirstCollector(value1) {
            iterN(function GtupleRestCollector(valueN) {
              callback([value1].concat(valueN));
            });
          });
        };
      }
    },
    /** Apply a function of values to the generator args. */
    apply: function apply(fun, iter1, var_rest) {
      var argsGen = G.tuple.apply(undefined,
          Array.prototype.slice.call(arguments, 1));
      return function GapplyGen(callback) {
        argsGen(function GapplyCollector(args) {
          callback(fun.apply(undefined, args));
        });
      };
    },
    /** Record with generated values. */
    record: function record(template) {
      var keys = Object.keys(template);
      var values = keys.map(function(k) { return template[k]; });
      return G.apply(function(var_args) {
        var o = {};
        for (var i = 0; i < keys.length; i++) {
          o[keys[i]] = arguments[i];
        }
        return o;
      }, G.tuple.apply(values));
    }
  });

  function shouldTreatAccessorAsValue(object, name, desc) {
    return (
        // work around SES patching frozen value properties into accessors
        // Note: pattern must match minified or unminified code
        /defProp\(this, ?name, ?\{/.exec(String(desc.set)) ||
        // or Domado doing the same, slightly differently
        getFunctionName(desc.get) === 'overrideGetter');
  }

  function getFunctionName(fun) {
    var m;
    if (typeof fun !== 'function') {
      return '';
    } else if (typeof fun.name === 'string') {
      return fun.name;
    } else if ((m = /^function ([\w$]+)/.exec(
        Function.prototype.toString.call(fun)))) {
      return m[1];
    } else {
      return '';
    }
  }

  function printNamedly(object) {
    if (typeof object === 'function') {
      var name = getFunctionName(object);
      return name === '' ? object.toString().replace(/\s+/, ' ') : name;
    } else {
      return String(object);
    }
  }

  function safeToString(obj) {
    // We're not concerned about toStrings that throw so much as
    // '' + Object.create(null).
    try {
      return '' + obj;
    } catch (e) {
      return '<stringification threw: ' + e + '>';
    }
  }

  /**
   * Provide instances of objects given constructors.
   */
  function UniversalConstructor(noteGap) {
    function invokeConstructor(ctor, context) {
      var x;
      try {
        x = new ctor();
      } catch (e) {
        noteGap('Need custom obtainInstance for ctor; threw ' + e, context);
        return undefined;
      }
      Object.freeze(x);  // avoid registering as extensibility problem
      if (!(x instanceof ctor)) {
        debugger;
        throw new Error('Got a non-instance');
      }
      return x;
    }
    var instances = new WeakMap();
    function obtainInstance(ctor, context) {
      if (!instances.has(ctor)) {
        instances.set(ctor, invokeConstructor(ctor, context));
      }
      return instances.get(ctor);
    }
    obtainInstance.define = instances.set.bind(instances);
    return obtainInstance;
  }

  // thisArg special markers:
  var CONSTRUCT = {};  // invoke as a constructor
  var THIS = {};  // invoke as a method
  var PLAIN_CALL = {};  // invoke without using Function.prototype.apply

  /**
   * A map from (various ways to recognize objects) to values. The values must
   * be truthy.
   */
  function MatchingMap() {
    // Note: identityTable and prototypeTable are object-keyed maps; they don't
    // especially need to be weak (but that doesn't hurt either).
    var identityTable = new WeakMap();
    var lastPathElementTable = {};
    var suffixTable = [];
    var prototypeTable = new WeakMap();
    var specialCases = [];

    var self = this;

    function setByIdentity(obj, value) {
      switch (typeof obj) {
        case 'object':
        case 'function':
          identityTable.set(obj, value);
          break;
        case 'undefined':
          // TODO(kpreid): make this looseness optional, so things which
          // aren't optional aren't subject to lack of typo checking
          return;
        default:
          throw new TypeError('setByIdentity: key is not an object: ' + obj);
      }
    }

    function setByPropertyName(name, value) {
      // name mangled to protect against ES5/3 magic for 'toString' at least
      lastPathElementTable[' ' + name] = value;
    }

    function setByPathSuffix(name, value) {
      suffixTable.push([name, value]);
    }

    function setByConstructor(ctor, value) {
      prototypeTable.set(ctor.prototype, value);
    }

    /** Use a Ref object to add to this map. */
    function set(ref, value) {
      ref.putInMap(self, value);
    }

    /** Shorthand for this.set(ref, true) for boolean-valued maps. */
    function mark(ref) {
      set(ref, true);
    }

    /**
     * Add a function which will be called to try to obtain a value.
     */
    function addSpecialCase(fn) {
      specialCases.push(fn);
    }

    function get(context) {
      var obj = context.get();
      var path = context.getPath();
      var value, m;

      if ((value = identityTable.get(obj))) {
        return value;
      }

      if ((m = /\.([^.]+)$/.exec(path)) &&
          Object.prototype.hasOwnProperty.call(lastPathElementTable,
              ' ' + m[1])) {
        return lastPathElementTable[' ' + m[1]];
      }

      if ((m = suffixTable.filter(function(record) {
            return isSuffix(record[0], path);
          })).length) {
        if (m.length > 1) {
          debugger;
          throw new Error('Suffix conflict (' + path + '): ' + m);
        }
        return m[0][1];
      }

      for (var proto = Object.getPrototypeOf(obj);
           proto !== null;
           proto = Object.getPrototypeOf(proto)) {
        if ((value = prototypeTable.get(proto))) {
          return value;
        }
      }

      for (var i = 0; i < specialCases.length; i++) {
        if (value = specialCases[i].call(undefined, context)) {
          return value;
        }
      }

      return undefined;
    }

    this.get = get;
    this.setByIdentity = setByIdentity;
    this.setByPropertyName = setByPropertyName;
    this.setByPathSuffix = setByPathSuffix;
    this.setByConstructor = setByConstructor;
    this.set = set;
    this.mark = mark;
    this.addSpecialCase = addSpecialCase;
  }
  
  /**
   * Designate one or more objects, in the ways MatchingMap supports.
   */
  var Ref = cajaVM.def({
    is: function refIs(obj) {
      return {
        putInMap: function(map, value) {
          map.setByIdentity(obj, value);
        }
      };
    },
    prop: function refProp(prop) {
      return {
        putInMap: function(map, value) {
          map.setByPropertyName(prop, value);
          map.setByPropertyName('get ' + prop + '<THIS>()', value);
        }
      };
    },
    path: function refPath(path) {
      return {
        putInMap: function(map, value) {
          map.setByPathSuffix(path, value);
        }
      };
    },
    ctor: function refCtor(ctor) {
      return {
        putInMap: function(map, value) {
          map.setByConstructor(ctor, value);
        }
      };
    },
    all: function refAll(var_args) {
      var refs = Array.prototype.slice.call(arguments);
      return {
        putInMap: function(map, value) {
          refs.forEach(function(ref) {
            ref.putInMap(map, value);
          });
        }
      };
    }
  });
  
  var Context = (function() {
    /**
     * Holds an object, information about where it came from, and information
     * how to use it in context (such as the thisArg for a function invocation).
     *
     * @param object The object of interest.
     * @param path A textual short description of how we got the object.
     * @param depth Count of sequential operations performed to get the object.
     * @param getSelfC Returns the context of something to use instead of the
     *     object on methods (i.e. an instance if the object is a prototype).
     * @param getThisArgC Ditto but for the thisArg for invoking the object
     *     itself as a function/method.
     *
     *     getSelfC and getThisArgC are thunks (lazy) so that we can avoid
     *     attempting to obtain an instance of a not-actually-a-constructor.
     * @param getProgram A thunk for program source which returns the object.
     */
    function makeContext(object, path, depth, getSelfC, getThisArgC,
        getProgram) {
      var context;
      var prefix = path === '' ? '' : path + '.';
      if (getSelfC === 'self') {
        getSelfC = function() { return context; };
      }
      context = {
        toString: function() {
          return '' + object + ' in context[' + getSelfC + ',' + getThisArgC +
              ']';
        },
        toDetailsString: function() {
          return (
              '| Path:     ' + path + '\n' +
              '| Program:  ' + getProgram() + '\n' +
              '| toString: ' + safeToString(object));
        },
        get: function() { return object; },
        getPath: function() { return path; },
        getDepth: function() { return depth; },
        getter: function(name, getter) {
          function getterProgram() {
            return 'Object.getOwnPropertyDescriptor(' +
                context.getProgram() + ', "' + name + '").get';
          }
          getterProgram.invocationShortcut = function() {
            return context.getProgram() + '.' + name;
          };
          return makeContext(
              getter,
              prefix + 'get ' + name,
              depth + 1,
              'self',
              getSelfC,
              getterProgram);
        },
        setter: function(name, setter) {
          return makeContext(
              setter,
              prefix + 'set ' + name,
              depth + 1,
              'self',
              getSelfC,
              function() {
                return 'Object.getOwnPropertyDescriptor(' +
                    context.getProgram() + ', "' + name + '").set';
              });
        },
        property: function(p, pval, obtainInstance) {
          // TODO(kpreid): obtainInstance parameter here is a kludge
          var subpath = prefix + p;
          if (p === 'prototype') {
            // When invoking methods on a prototype, use an instance of this
            // ctor instead as 'this'.
            var protoctx = makeContext(pval, subpath, depth + 1,
                function() {
                  var selfC = getSelfC();
                  return makeContext(
                      obtainInstance(selfC.get(), selfC),
                      path + '<instance>',
                      depth + 1,
                      'self',
                      function() { return noThisContext; },
                      protoctx,
                      function() {
                        return 'obtainInstance(' + selfC.getProgram() + ')';
                      });
                },
                getSelfC,
                function() {
                  return context.getProgram() + '.prototype';
                });
            return protoctx;
          } else if (p === '[[Prototype]]') {
            return makeContext(
                pval,
                subpath,
                depth + 1,
                getSelfC,
                function() { return noThisContext; },
                function() {
                  return 'Object.getPrototypeOf(' + context.getProgram() + ')';
                });
          } else {
            return makeContext(
                pval,
                subpath,
                depth + 1,
                'self',
                getSelfC,
                function() {
                  if (/^[a-z_]\w*$/i.test(p)) {
                    return context.getProgram() + '.' + p;
                  } else if (/^-?[0-9]+$/.test(p)) {
                    p = parseInt(p);
                  }
                  return context.getProgram() + '[' + JSON.stringify(p) + ']';
                });
          }
        },
        invocation: function(ival, argstr, thrown) {
          return makeContext(
              ival,
              path + argstr + (thrown ? ' thrown ' : ''),
              depth + 1,
              'self',
              getSelfC,
              function() {
                if (thrown) {
                  return 'thrown(' + context.getProgram() +
                      ', ...)';
                } else if ('invocationShortcut' in context.getProgram) {
                  // TODO(kpreid): check args once that is formalized
                  return context.getProgram.invocationShortcut();
                } else {
                  return context.getProgram() + '.call(...)';
                }
              });
        },
        getThisArg: function() { return getThisArgC().get(); },
        /** Return a program which evaluates to the object. */
        getProgram: getProgram
      };
      return context;
    }
    var noThisContext = makeContext(
        undefined,
        '<noThis>',
        'self',
        0,
        function() { return noThisContext; },
        function() { return 'undefined'; });

    return {
      root: function(o, path, code) {
        return makeContext(
            o,
            path,
            0,
            'self',
            function() { return noThisContext; },
            function() { return code; });
      }
    };
  })();

  /**
   * Main scan driver.
   */
  function Scanner(options) {
    // parameters
    var pathFilter = optional(options.pathFilter, '');
    var breakOnProblem = optional(options.breakOnProblem, false);
    var log = optional(options.log, function() {});
    var logProblem = optional(options.logProblem, function() {});
    var logGap = optional(options.logGap, function() {});
    var setProgress = optional(options.setProgress, function() {});
    var finished = optional(options.finished, function() {});
    var checkObject = optional(options.checkObject, function() {});
    var sortPropertyNames = optional(options.sortPropertyNames,
        function(a) { a.sort(); });

    // exported configuration
    var expectedUnfrozen = this.expectedUnfrozen = new MatchingMap();
    var expectedAlwaysThrow = this.expectedAlwaysThrow = new MatchingMap();
    var functionArgs = this.functionArgs = new MatchingMap();
    var obtainInstance = this.obtainInstance =
        new UniversalConstructor(noteGap);

    // internal state
    var ran = false;
    var problemCount = 0;
    var gapCount = 0;
    var totalDone = 0;
    var seenTable = new WeakMap();

    function noteProblem(description, context) {
      problemCount++;
      logProblem(description, context);
      if (breakOnProblem) { debugger; }
    }
    // exported to allow callbacks to record problems
    this.noteProblem = noteProblem;

    function noteGap(description, opt_context) {
      gapCount++;
      logGap(description, opt_context);
    }

    // Async loop to avoid stalling browser
    var queued = [], current = [];
    function queue(context, last) {
      if (last) {
        queued.unshift(context);
      } else {
        current.push(context);
      }
    }

    this.queue = function(context) {
      if (ran) { throw new Error('already started'); }
      queue(context, false);
    };

    this.skip = function(globalName) {
      if (ran) { throw new Error('already started'); }

      var o = window[globalName];
      if (!o) { return; }
      seenTable.set(o, globalName);
      if (typeof o === 'function') {
        var p = o.prototype;
        if (typeof p === 'function' || typeof p === 'object') {
          seenTable.set(p, globalName + '.prototype');
        }
      }
    };

    this.scan = function() {
      if (ran) { throw new Error('scan() called again'); }
      ran = true;

      function traverse(context) {
        var object = context.get();
        var path = context.getPath();
        switch (typeof object) {
          case 'boolean':
          case 'number':
          case 'string':
          case 'undefined':
            return;
          case 'object':
          case 'function':
            if (object === null) { return; }
            break;
          default: throw new Error('Unhandled typeof ' + typeof object);
        }

        if (!isPrefix(pathFilter, path) && !isPrefix(path, pathFilter)) {
          log(path, ' FILTERED\n');
          return;
        }

        if (context.getDepth() > 16) {
          noteGap('Depth limit reached', context);
          return;
        }

        // deduplication and logging
        var seenName = seenTable.get(object);
        if (seenName) {
          log(path, ' === ', seenName, '\n');
          return;
        }
        seenTable.set(object, path);
        log(path, '\n');
        if (object instanceof Error) {
          log('  ' + object + '\n');
        }

        // general tests
        var doomed = checkObject(context);
        if (doomed) { return; }

        // frozenness test
        // This is not in checkObject because it is tied into individual
        // property checks and because then the ownership of expectedUnfrozen
        // would be odd.
        var objectShouldBeFrozen = !expectedUnfrozen.get(context);
        if (!Object.isFrozen(object) && objectShouldBeFrozen) {
          if (Object.isExtensible(object)) {
            // Be more specific about why it is not frozen -- we also check
            // individual properties below.
            noteProblem('Object is extensible', context);
          } else {
            noteProblem('Object is not frozen', context);
          }
        }

        // function invocation
        if (typeof object === 'function') {
          var argGenerator = functionArgs.get(context);
          if (!argGenerator) {
            noteGap('No argument generator for function', context);
            argGenerator = G.none;
          }

          var didSomeCall = false;
          var didNonThrowingCall = false;

          // TODO(kpreid): the '.get stack' rule is a misplaced kludge which
          // should at least live in the caller. It is needed to avoid infinite
          // descent. See
          // <https://code.google.com/p/google-caja/issues/detail?id=1848>.
          var shouldPlainCall = isNativeFunction(object) &&
              !(/\.get stack/.test(path));
          var didPlainCall = false;

          var doInvocation = function(tuple) {
            var thisArg = tuple[0];
            var args = tuple[1];
            var hook = tuple[2];
            if (tuple.length < 2 || tuple.length > 3 ||
                !(args instanceof Array) ||
                !(hook === undefined || hook instanceof Function)) {
              throw new Error('Malformed invocation description: ' + tuple +
                  '\n' + context.toDetailsString());
            }
            var result;
            var thisArgStr = '<' + thisArg + '>';
            var thrown;
            if (thisArg === CONSTRUCT) {
              thisArgStr = '<CONSTRUCT>';
              try {
                switch (args.length) {
                  case 0: result = new object(); break;
                  case 1: result = new object(args[0]); break;
                  case 2: result = new object(args[0], args[1]); break;
                  case 3: result = new object(args[0], args[1], args[2]); break;
                  default:
                    noteGap('Construction for ' + args.length +
                        ' args not implemented');
                    result = undefined;
                }
                // Constructor products are assumed to be fresh and therefore OK
                // to be extensible.
                expectedUnfrozen.setByIdentity(result, true);
                thrown = false;
              } catch (e) {
                result = e;
                thrown = true;
              }
            } else if (thisArg === PLAIN_CALL) {
              thisArgStr = '<PLAIN>';
              didPlainCall = true;
              // Do a plain function call, literally, with no call/apply
              try {
                result = trueApply(object, args);
                thrown = false;
              } catch (e) {
                result = e;
                thrown = true;
              }
            } else {
              if (thisArg === THIS) {
                thisArg = context.getThisArg();
                thisArgStr = '<THIS>';
              }
              try {
                result = Function.prototype.apply.call(object, thisArg, args);
                thrown = false;
              } catch (e) {
                result = e;
                thrown = true;
              }
            }
            didSomeCall = true;
            didNonThrowingCall = didNonThrowingCall || !thrown;
            // TODO(kpreid): Make these live objects instead of strings, so that
            // we can print clearer paths and more complete programs.
            var subcontext = context.invocation(result, thisArgStr + '(' +
                args.map(printNamedly) + ')', thrown);
            if (hook) {
              // TODO(kpreid): Context should include thrown flag
              hook(subcontext, thrown);
            }
            traverse(subcontext);

            // Special plain call hook
            // We want to do the plain call _and_ a normal reflective one, and
            // preserve the hook
            if (shouldPlainCall && thisArg === undefined) {
              doInvocation([PLAIN_CALL, args, hook]);
            }
          };
          argGenerator(doInvocation);

          if (shouldPlainCall && !didPlainCall) {
            doInvocation([PLAIN_CALL, []]);
          }

          if (didSomeCall && !didNonThrowingCall &&
              !expectedAlwaysThrow.get(context)) {
            noteGap('No non-throwing call', context);
          }
        }

        // traversal
        var propProblems = {writable: false, configurable: false};
        var problematicProps = [];
        function recurse(name, desc) {
          if (name === ''+(+name) && +name > 10) {
            // kludge for ArrayLike's large virtual array
            return;
          }

          // defer "upward reference" things to get better pathnames
          var last = name === 'ownerDocument' || name === 'ownerElement' ||
              name === '[[Prototype]]';

          // We also check that objects are frozen, but let's note the
          // individual problematic flags
          if (objectShouldBeFrozen) {
            if (desc.writable) {
              propProblems.writable = true;
            }
            if (desc.configurable) {
              propProblems.configurable = true;
            }
            if (desc.writable || desc.configurable) {
              problematicProps.push(name);
            }
          }

          // Check enumerable flags
          // TODO(kpreid): Enable this test and fix problems it reveals
          // TODO(kpreid): Make these tests a function parameter to Scanner
          // TODO(kpreid): Check in general that overrides have same
          // enumerability as overridden.
          if (false && Object.prototype.hasOwnProperty(name)) {
            var protoEnum = Object.getOwnPropertyDescriptor(Object.prototype,
                name).enumerable;
            if (desc.enumerable !== protoEnum) {
              noteProblem(name + ' is enumerable:' + desc.enumerable +
                  ' but Object.prototype\'s is ' + protoEnum, context);
            }
          }

          if (!('value' in desc) &&
              shouldTreatAccessorAsValue(object, name, desc)) {
            desc.value = desc.get.call(object);
            delete desc.get;
            delete desc.set;
          }

          var v;
          if ((v = desc.value)) {
            queue(context.property(name, v, obtainInstance), last);
          }
          if ((v = desc.get)) {
            queue(context.getter(name, v), last);
          }
          if ((v = desc.set)) {
            queue(context.setter(name, v), last);
          }
        }
        var ownPropertyNames;
        try {
          ownPropertyNames = Object.getOwnPropertyNames(object);
        } catch (e) {
          noteGap('getOwnPropertyNames(_) threw ' + e, context);
          ownPropertyNames = [];
        }
        sortPropertyNames(ownPropertyNames);
        ownPropertyNames.forEach(function(name) {
          var desc = undefined;
          try {
            desc = Object.getOwnPropertyDescriptor(object, name);
          } catch (e) {
            noteGap('getOwnPropertyDescriptor(_, "' + name + '") threw ' + e,
                context);
          }
          if (desc) { recurse(name, desc); }
        });
        var prototype = Object.getPrototypeOf(object);
        recurse('[[Prototype]]', {value: prototype});
        if (problematicProps.length) {
          problematicProps.sort();
          noteProblem('Properties are ' +
              (propProblems.writable && propProblems.configurable
                  ? 'writable and/or configurable' :
                  propProblems.writable ? 'writable' : 'configurable') + ': ' +
              problematicProps, context);
        }
      }

      // Progress reporting
      function updateProgress() {
        var totalQueued = queued.length + current.length;

        // The exponent is an arbitrary curve-fudge-factor to account for that
        // early on we don't know how many items there are to scan and later
        // there are lots of leaves to finish quickly.
        setProgress(Math.pow(totalDone/(totalDone + totalQueued || 1), 3));
      }

      function loop() {
        for (var blockStart = Date.now(); Date.now() - blockStart < 200;) {
          if (queued.length || current.length) {
            // simulates ordinary recursive-call ordering
            queued.push.apply(queued, current.reverse());
            current = [];
            var context = queued.pop();
            traverse(context);
            totalDone++;
          } else {
            updateProgress();
            internalFinished();
            return;
          }
        }
        // Note this loop is not in jsunitCallback; since there is only one test
        // in this file it gives us no advantage.
        setTimeout(loop, 0);
        updateProgress();
      }
      loop();
    };

    this.stop = function() {
      if (!ran) { throw new Error('not started'); }
      noteGap('Stopped by user.');
      queued = current = [];
    };

    function internalFinished() {
      // Log the presence of a filter.
      if (pathFilter !== '') {
        noteGap('Filter applied: ' + pathFilter);
      }

      finished(problemCount, gapCount, totalDone);
    }
  }
  
  scanning = Object.freeze({
    // Scanner and needed supporting components
    Scanner: Scanner,
    Context: Context,
    Ref: Ref,
    G: G,
    CONSTRUCT: CONSTRUCT,
    THIS: THIS,
    PLAIN_CALL: PLAIN_CALL,

    // Useful utilities:
    getFunctionName: getFunctionName,

    // Exported for self-testing purposes only
    trueApply: trueApply
  });
}());
