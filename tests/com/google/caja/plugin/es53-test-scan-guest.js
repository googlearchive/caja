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
 * Walks the reference graph looking for problems.
 *
 * @author kpreid@switchb.org
 *
 * @requires JSON, WeakMap, Proxy, document, console, location,
 *     setTimeout, clearTimeout, setInterval, clearInterval,
 *     cajaVM, directAccess, inES5Mode, getUrlParam,
 *     assertTrue, assertEquals, pass, jsunitFail,
 *     Event, HTMLInputElement, HTMLMediaElement, HTMLTableRowElement,
 *     HTMLTableSectionElement, HTMLTableElement,
 *     Audio, Image, Option, XMLHttpRequest, Window, Document, Node,
 *     Attr, Text, CSSStyleDeclaration, CanvasRenderingContext2D,
 *     CanvasGradient, ImageData, Location
 * @overrides window
 */

(function() {

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

  /** Fake evaluator for ES5/3 compatibility */
  function simpleEval(env, expr) {
    var match;
    if ((match = /^(.*)\.([\w$]+)$/.exec(expr))) {
      return simpleEval(env, match[1])[match[2]];
    } else if ((match = /^([\w$]+)$/.exec(expr))) {
      return env[match[1]];
    } else {
      throw new EvalError('simpleEval does not implement: ' + expr);
    }
  }

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

  var whichFrame = function(){
    var tamingOCtor = directAccess.evalInTamingFrame('Object');
    var hostOCtor = directAccess.evalInHostFrame('Object');
    return function whichFrame_(object) {
      if (object instanceof Object || object === Object.prototype) {
        return 'guest';
      } else if (object instanceof tamingOCtor ||
          object === tamingOCtor.prototype) {
        return 'taming';
      } else if (object instanceof hostOCtor ||
          object === hostOCtor.prototype) {
        return 'host';
      } else if (Object.getPrototypeOf(object) === null) {
        return 'neutral';
      } else {
        return 'unknown';
      }
    };
  }();

  var isToxicFunction;
  if (inES5Mode) {
    isToxicFunction = function isToxicFunctionSES(object) { return false; };
  } else {
    var tamingFrameFunctionPrototype =
        directAccess.evalInTamingFrame('Function.prototype');
    isToxicFunction = function isToxicFunctionES53(object) {
      return object === Function.prototype ||
          object === tamingFrameFunctionPrototype;
    };
  }

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
    if (typeof fun !== 'function' || isToxicFunction(fun)) {
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
    function invokeConstructor(ctor, infoThunk) {
      var x;
      try {
        x = new ctor();
      } catch (e) {
        noteGap('Need custom obtainInstance for ctor; threw ' + e + '\n' +
            infoThunk());
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
    function obtainInstance(ctor, infoThunk) {
      if (!instances.has(ctor)) {
        instances.set(ctor, invokeConstructor(ctor, infoThunk));
      }
      return instances.get(ctor);
    }
    obtainInstance.define = instances.set.bind(instances);
    return obtainInstance;
  }

  // thisArg special markers:
  var CONSTRUCT = {};  // invoke as a constructor
  var THIS = {};  // invoke as a method

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

    function setByIdentity(obj, value) {
      switch (typeof obj) {
        case 'object':
        case 'function':
          identityTable.set(obj, value);
          break;
        case 'undefined':
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
    this.addSpecialCase = addSpecialCase;
  }

  var Context = (function() {
    /**
     * Holds an object, information about where it came from, and information
     * how to use it in context (such as the thisArg for a function invocation).
     *
     * @param object The object of interest.
     * @param path A textual short description of how we got the object.
     * @param getSelfC Returns the context of something to use instead of the
     *     object on methods (i.e. an instance if the object is a prototype).
     * @param getThisArgC Ditto but for the thisArg for invoking the object
     *     itself as a function/method.
     *
     *     getSelfC and getThisArgC are thunks (lazy) so that we can avoid
     *     attempting to obtain an instance of a not-actually-a-constructor.
     * @param getProgram A thunk for program source which returns the object.
     */
    function makeContext(object, path, getSelfC, getThisArgC, getProgram) {
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
              'self',
              getSelfC,
              getterProgram);
        },
        setter: function(name, setter) {
          return makeContext(
              setter,
              prefix + 'set ' + name,
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
            var protoctx = makeContext(pval, subpath,
                function() {
                  var selfC = getSelfC();
                  return makeContext(
                      obtainInstance(selfC.get(), selfC.toDetailsString),
                      path + '<instance>',
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
                getSelfC,
                function() { return noThisContext; },
                function() {
                  return 'Object.getPrototypeOf(' + context.getProgram() + ')';
                });
          } else {
            return makeContext(
                pval,
                subpath,
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
        function() { return noThisContext; },
        function() { return noThisContext; },
        function() { return 'undefined'; });

    return {
      root: function(o, path, code) {
        return makeContext(
            o,
            path,
            function() { return noThisContext; },
            function() { return noThisContext; },
            function() { return code; });
      }
    };
  })();

  // Sort JS things before DOM things and ctors before instances.
  var hardPropertyOrdering = [
    // JS builtins
    "Object",
    "Array",
    "Boolean",
    "Date",
    "decodeURI",
    "decodeURIComponent",
    "encodeURI",
    "encodeURIComponent",
    "Error",
    "escape",
    "EvalError",
    "Function",
    "Infinity",
    "isFinite",
    "isNaN",
    "JSON",
    "Math",
    "NaN",
    "Number",
    "parseFloat",
    "parseInt",
    "RangeError",
    "ReferenceError",
    "RegExp",
    "String",
    "StringMap",
    "SyntaxError",
    "TypeError",
    "undefined",
    "unescape",
    "URIError",
    "WeakMap",
    "cajaVM",

    // Early DOM pieces
    "Node",
    "Element",
    "Document",
    "Attr",
    "HTMLElement",
    "HTMLDocument"
  ];
  function propertyRank(name) {
    var i = hardPropertyOrdering.indexOf(name);
    if (i >= 0) {
      return i;
    } else {
      // ctors first
      return hardPropertyOrdering.length + (/^[A-Z]/.test(name) ? 0 : 1);
    }
  }
  function comparePropertyNames(a, b) {
    return (propertyRank(a) - propertyRank(b)) || (a < b ? -1 : a > b ? 1 : 0);
  }

  /**
   * Main driver code; this is in a class mainly to separate out the algorithm
   * from all the special cases and wiring.
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

    // exported configuration
    var expectedUnfrozen = this.expectedUnfrozen = new MatchingMap();
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

    function noteGap(description) {
      gapCount++;
      logGap(description);
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

        // deduplication and logging
        var seenName = seenTable.get(object);
        if (seenName) {
          log(path, ' === ', seenName, '\n');
          return;
        }
        seenTable.set(object, path);
        log(path, '\n');

        var frameOfObject = whichFrame(object);
        var objectShouldBeFrozen = !expectedUnfrozen.get(context);

        // tests
        // TODO(kpreid): Make the tests a function parameter to Scanner
        if (frameOfObject !== 'guest' && frameOfObject !== 'taming' &&
            frameOfObject !== 'neutral') {
          noteProblem('Object from a ' + frameOfObject + ' frame', context);
          return; // no point in further analyzing, is doomed
        }
        if (!Object.isFrozen(object) && objectShouldBeFrozen) {
          if (Object.isExtensible(object)) {
            // Be more specific about why it is not frozen -- we also check
            // individual properties below.
            noteProblem('Object is extensible', context);
          } else {
            noteProblem('Object is not frozen', context);
          }
        }
        // TODO(kpreid): factor out recognition
        if (typeof object === 'function' &&
            /function Tame(?!XMLHttpRequest)/
                .test(object.toString())) {
          noteProblem('Object is a taming ctor', context);
        }

        // function invocation
        if (typeof object === 'function') {
          var argGenerator = functionArgs.get(context);
          if (!argGenerator) {
            noteGap('No argument generator for function\n' +
                context.toDetailsString());
            argGenerator = G.none;
          }
          argGenerator(function(tuple) {
            var thisArg = tuple[0];
            var args = tuple[1];
            var hook = tuple[2];
            var result;
            var thisArgStr = '<' + thisArg + '>';
            var thrown;
            if (thisArg === CONSTRUCT) {
              thisArgStr = '<CONSTRUCT>';
              try {
                switch (args.length) {
                  case 0: result = new object(); break;
                  case 1: result = new object(args[0]); break;
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
            // TODO(kpreid): Make these live objects instead of strings, so that
            // we can print clearer paths and more complete programs.
            var subcontext = context.invocation(result, thisArgStr + '(' +
                args.map(printNamedly) + ')', thrown);
            if (hook) {
              // TODO(kpreid): Context should include thrown flag
              hook(subcontext, thrown);
            }
            traverse(subcontext);
          });
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
          noteGap('getOwnPropertyNames(_) threw ' + e + '\n' +
              context.toDetailsString());
          ownPropertyNames = [];
        }
        ownPropertyNames.sort(comparePropertyNames);
        ownPropertyNames.forEach(function(name) {
          var desc = undefined;
          try {
            desc = Object.getOwnPropertyDescriptor(object, name);
          } catch (e) {
            noteGap('getOwnPropertyDescriptor(_, "' + name + '") threw ' + e +
                '\n' + context.toDetailsString());
          }
          if (desc) { recurse(name, desc); }
        });
        var prototype = Object.getPrototypeOf(object);
        if (!isToxicFunction(prototype)) {
          recurse('[[Prototype]]', {value: prototype});
        }
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

  window.testUniverse = function testUniverse() {
    var scanner = new Scanner({
      pathFilter: getUrlParam('scan-only'),
      breakOnProblem: !!getUrlParam('break'),  // TODO(kpreid): UI to set flag
      // functions are defined below
      log: write,
      logProblem: logProblem,
      logGap: logGap,
      setProgress: setProgress,
      finished: finished
    });
    var expectedUnfrozen = scanner.expectedUnfrozen;

    // skip test environment things
    scanner.skip('$');
    for (var v in window) {
      if (/^assert/.test(v)) {
        scanner.skip(v);
      }
    }
    scanner.skip('canonInnerHtml');
    scanner.skip('console');
    scanner.skip('crossFrameFreezeBug');
    scanner.skip('directAccess');
    scanner.skip('error');
    scanner.skip('expectFailure');
    scanner.skip('fail');
    scanner.skip('getUrlParam');
    scanner.skip('jsunitCallback');
    scanner.skip('JsUnitException');
    scanner.skip('jsunitFail');
    scanner.skip('jsunitPass');
    scanner.skip('jsunitRegister');
    scanner.skip('jsunitRegisterIf');
    scanner.skip('jsunitRun');
    scanner.skip('makeReadOnly');
    scanner.skip('matchColor');
    scanner.skip('modifyUrlParam');
    scanner.skip('onerror');
    scanner.skip('pass');
    scanner.skip('readyToTest');
    scanner.skip('setUp');
    scanner.skip('tearDown');
    // our own symbols
    scanner.skip('toggleNonErrors');
    scanner.skip('testUniverse');
    scanner.skip('testScanner');

    // TODO(kpreid): add more useful erroneous invocations
    var genString = G.value(null /* deliberately wrong */, '', 'foo', 'bar',
        '1', 'NaN');
    var genNoArgMethod = G.value([THIS, []]);
    var genBoolean = G.value(undefined, false, true);
    var genStrictBoolean = G.value(false, true);
    var genTypeName = G.value('undefined', 'object', 'boolean', 'number',
        'string', 'function', 'foo', null);
    var genClassName = G.value(undefined, null, 'testcontainer', 'not.a.class');
    var genElementName = G.value(undefined, null, 'a', 'div', 'form', 'foo',
        'caja-v-foo');
    var genEventClass = G.value(undefined, 'foo', 'HTMLEvents', 'KeyEvent');
    var genEventName = G.value(undefined, null, '', 'click');
    var genCSSSelector = G.value(null, {}, 'foo', '[');
    var genCSSPropertyName = G.value(undefined, null, '', 'background-color',
        'backgroundColor', 'content', 'foo');
    var genCSSColor = G.value(undefined, null, '', 'red', '#17F', 'octarine',
        '}{');
    var genMediaType = G.value(undefined, '', '/', 'text', 'text/*',
        'text/plain', 'image/*', 'image/gif', 'audio/*', 'audio/x-wav');
    var genJS = G.value(null, '', '{', 'return true;');
    var genEventHandlerSet = G.tuple(G.value(THIS), G.tuple(genJS));
    var largestSmallInteger = 63;
    var genSmallInteger = G.value(-1, 0, 1, 2, 63, NaN, null, 1.5, '1', 'a');
    var genNumber = G.any(genSmallInteger, G.value(Infinity, -Infinity,
        Math.pow(2, 53), Math.pow(2, 60)));
    var genRegex = G.value(null, '', 'a(.*)b', '*');
    var genPseudoElement = G.value(null, ':', ':after', ':before', ':marker',
        ':line-marker');
    var genJSONValue = G.any(genString, genSmallInteger,
        G.value(function(){/*invalid*/}, {'foo:bar':'baz'}, null, undefined));
    var genJSON = G.value(undefined, '{', '{}', '{x:1}');
    var genProperty = G.any(
      G.record({
        enumerable: genStrictBoolean,
        configurable: genStrictBoolean,
        writable: genStrictBoolean,
        value: genJSONValue
      }),
      G.record({
        enumerable: genStrictBoolean,
        configurable: genStrictBoolean,
        get: G.value(function() { throw new Error('boo'); }),
        set: G.value(function(v) { throw new Error('boo'); })
      })
    );
    var genFancyRecord = G.apply(function(name, desc) {
      var o = {};
      Object.defineProperty(o, name, desc);
      expectedUnfrozen.setByIdentity(o, true);
      return o;
    }, G.value(null, 'foo', 'bar'), genProperty);
    function genNumbers(count) {
      // avoids exponential results of G.tuple(genSmallInteger*)
      if (count <= 0) {
        return G.value([]);
      } else {
        return function genNumbersGen(c) {
          genSmallInteger(function (int) {
            var tuple = [];
            for (var i = 0; i < count; i++) {
              tuple.push(int++);
            }
            c(tuple);
          });
        };
      }
    }
    function genEjector(c) {
      c(function(v) { });
      c(function(v) { throw Object.freeze(['ejectorArg', v]); });
    }
    function genConcat(ag, bg) {
      return G.apply(function(a, b) {
        return a.concat(b);
      }, ag, bg);
    }
    function genCall(var_args) {
      return G.tuple(G.value(undefined), G.tuple.apply(G.tuple, arguments));
    }
    function genCallAlt(args) {
      return G.tuple(G.value(undefined), args);
    }
    function genNew(var_args) {
      return G.tuple(G.value(CONSTRUCT), G.tuple.apply(G.tuple, arguments));
    }
    function genMethod(var_args) {
      return G.tuple(G.value(THIS), G.tuple.apply(G.tuple, arguments));
    }
    function genMethodAlt(args) {
      return G.tuple(G.value(THIS), args);
    }
    // Setters return nothing interesting so we just want to make them crash
    // which we hopefully can do by using an object of no particular type
    var genAccessorSet = genMethod(G.value(cajaVM.def({
      toString: function() { return '<setter garbage>'; }
    })));
    function arrayLikeStubLength() { return 2; }
    function arrayLikeStubGet(i) { return i; }
    var genArrayLikeCall = genCallAlt(G.apply(function() {
      // TODO(kpreid): test interesting abuses of the interface
      return [Object.create(ArrayLike.prototype), arrayLikeStubLength,
          arrayLikeStubGet];
    }));
    /** Add third value-callback to an arguments generator */
    function annotate(calls, callback) {
      return G.apply(function (call) {
        return [call[0], call[1], callback];
      }, calls);
    }
    /** Note that the result is fresh and therefore expected to be extensible */
    function freshResult(calls) {
      return annotate(calls, function(context, thrown) {
        expectedUnfrozen.setByIdentity(context.get(), true);
      });
    }

    var elementSpecimen = document.createElement('an-element-for-tests');
    var ArrayLike = cajaVM.makeArrayLike(largestSmallInteger);

    function isArrayLikeCtor(o) {
      // matching code text is a kludge to be able to recognize usurped
      // smaller ArrayLikes.
      return o === ArrayLike || /proto instanceof BAL/.test(o.toString());
    }

    var functionArgs = scanner.functionArgs;
    functionArgs.addSpecialCase(function(context) {
      var fun = context.get();
      var path = context.getPath();
      var str = String(fun);
      var name = getFunctionName(fun);
      if (/^\[domado inert constructor(?:.*)\]$/.test(str)) {
        // inert ctor -- should throw
        return G.tuple(G.value(CONSTRUCT), G.tuple());
      } else if (/(\.|^)get \w+$/.test(path)) {
        // TODO(kpreid): Test invocation with an alternate this?
        return G.value([THIS, []]);
      } else if (/(\.|^)set on\w+$/.test(path)) {
        // Event handler accessor
        return genEventHandlerSet;
      } else if (name === 'tamingClassSetter') {
        // Don't invoke these setters because they notably mangle the global
        // environment.
        return G.none;
      } else if (/(\.|^)set (\w+)$/.test(path)) {
        var isLive = false;
        var thisArg = context.getThisArg();
        try { isLive = isLive || thisArg.parentNode; } catch (e) {}
        try { isLive = isLive || thisArg.ownerElement.parentNode; } catch (e) {}
        if (isLive) {
          // Don't modify live DOM
          return G.none;
        } else {
          // Generic accessor
          return genAccessorSet;
        }
      } else if (isArrayLikeCtor(fun)) {
        return genArrayLikeCall;
      } else {
        return null;
      }
    });
    function argsByIdentity(obj, g) {
      functionArgs.setByIdentity(obj, g);
      return g;
    }
    function argsByProp(p, g) {
      functionArgs.setByPropertyName(p, g);
      functionArgs.setByPropertyName('get ' + p + '<THIS>()', g);
      return g;
    }
    function argsBySuffix(p, g) {
      functionArgs.setByPathSuffix(p, g);
      return g;
    }
    var tamingEnv = directAccess.evalInTamingFrame('cajaVM.sharedImports');
    function argsByAnyFrame(code, g) {
      // actually, any _should-be-visible_ frame, i.e. guest and taming.

      // Note: Can't use evalInTamingFrame to get from the taming frame, because
      // we want the ES5/3-virtualized view.
      try {
        functionArgs.setByIdentity(simpleEval(tamingEnv, code), g);
        functionArgs.setByIdentity(simpleEval(window, code), g);
      } catch (e) {
        // ignore ES5/3 restriction
        if (e.message !== 'Properties may not end in double underscore.') {
          throw e;
        }
      }
    }

    argsByAnyFrame('Object', genNew());
    argsByAnyFrame('Object.create', freshResult(genMethod(
        G.value(null, Object.prototype))));
    argsByAnyFrame('Object.defineProperties', G.none);  // TODO abuse
    argsByAnyFrame('Object.defineProperty', G.none);  // TODO abuse
    argsByAnyFrame('Object.freeze', G.none);  // TODO abuse
    argsByAnyFrame('Object.getOwnPropertyDescriptor', G.none);  // TODO abuse
    argsByAnyFrame('Object.getOwnPropertyNames', G.none);  // TODO abuse
    argsByAnyFrame('Object.getPrototypeOf', G.none);  // TODO abuse
    argsByAnyFrame('Object.is', G.none);  // TODO abuse
    argsByAnyFrame('Object.isExtensible', G.none);  // TODO abuse
    argsByAnyFrame('Object.isFrozen', G.none);  // TODO abuse
    argsByAnyFrame('Object.isSealed', G.none);  // TODO abuse
    argsByAnyFrame('Object.keys', G.none);  // TODO abuse
    argsByAnyFrame('Object.preventExtensions', G.none);  // TODO abuse
    argsByAnyFrame('Object.seal', G.none);  // TODO abuse
    argsByAnyFrame('Object.prototype.__defineGetter__', G.none);  // TODO abuse
    argsByAnyFrame('Object.prototype.__defineSetter__', G.none);  // TODO abuse
    argsByAnyFrame('Object.prototype.__lookupGetter__', G.none);  // TODO abuse
    argsByAnyFrame('Object.prototype.__lookupSetter__', G.none);  // TODO abuse

    argsByProp('toString', annotate(genNoArgMethod, function(context, thrown) {
      if (thrown) {
        scanner.noteProblem('toString threw ' + context.get(), context);
      } else if (typeof context.get() !== 'string') {
        scanner.noteProblem('toString returned non-string', context);
      }
    }));
    argsByProp('toLocaleString', genNoArgMethod);
    argsByProp('valueOf', genNoArgMethod);
    argsByProp('hasOwnProperty', genMethod(genString));
    argsByProp('isPrototypeOf', G.none);  // TODO abuse
    argsByProp('propertyIsEnumerable', genMethod(genString));  // TODO abuse

    argsByAnyFrame('Function.prototype.apply', G.none);  // TODO abuse
    argsByAnyFrame('Function.prototype.bind', G.none);  // TODO abuse
    argsByAnyFrame('Function.prototype.call', G.none);  // TODO abuse
    argsByAnyFrame('Function.prototype.toString', G.none); // known to throw

    argsByIdentity(Number, genCall(genSmallInteger));
    argsByAnyFrame('Number.prototype.toExponential', G.none);  // TODO abuse
    argsByAnyFrame('Number.prototype.toFixed', G.none);  // TODO abuse
    argsByAnyFrame('Number.prototype.toPrecision', G.none);  // TODO abuse

    argsByAnyFrame('Array', G.any(genNew(), genNew(genSmallInteger)));
    argsByAnyFrame('Array.isArray', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.concat', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.every', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.filter', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.forEach', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.indexOf', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.join', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.lastIndexOf', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.map', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.pop', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.push', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.reduce', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.reduceRight', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.reverse', genNoArgMethod);
    argsByAnyFrame('Array.prototype.shift', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.slice', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.some', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.sort', genNoArgMethod);
    argsByAnyFrame('Array.prototype.splice', G.none);  // TODO abuse
    argsByAnyFrame('Array.prototype.unshift', G.none);  // TODO abuse

    argsByIdentity(Boolean, genCall(genBoolean));

    argsByIdentity(String, genCall(genString));
    argsByAnyFrame('String.fromCharCode', genMethod(genSmallInteger));
    ['big', 'blink', 'bold', 'fixed', 'italics', 'small', 'strong', 'strike',
        'sub', 'sup'].forEach(function(name) {
      argsByAnyFrame('String.prototype.' + name, genNoArgMethod);
    });
    ['anchor', 'fontcolor', 'fontsize', 'link'].forEach(function(name) {
      argsByAnyFrame('String.prototype.' + name, genMethod(genString));
    });
    argsByAnyFrame('String.prototype.charAt', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.charCodeAt', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.concat', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.indexOf', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.lastIndexOf', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.localeCompare', genMethod());
        // TODO abuse
    argsByAnyFrame('String.prototype.match', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.replace', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.search', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.slice', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.split', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.substr', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.substring', G.none);  // TODO abuse
    argsByAnyFrame('String.prototype.toLocaleLowerCase', genNoArgMethod);
    argsByAnyFrame('String.prototype.toLocaleUpperCase', genNoArgMethod);
    argsByAnyFrame('String.prototype.toLowerCase', genNoArgMethod);
    argsByAnyFrame('String.prototype.toUpperCase', genNoArgMethod);
    argsByAnyFrame('String.prototype.trim', genNoArgMethod);
    argsByAnyFrame('String.prototype.trimLeft', genNoArgMethod);
    argsByAnyFrame('String.prototype.trimRight', genNoArgMethod);

    argsByAnyFrame('Function', G.none);  // TODO deal with function return val
    argsByAnyFrame('Function.prototype', genCall());

    var genErrorConstruct = G.tuple(G.value(CONSTRUCT), G.tuple(genString));
    argsByAnyFrame('Error', genErrorConstruct);
    argsByAnyFrame('EvalError', genErrorConstruct);
    argsByAnyFrame('RangeError', genErrorConstruct);
    argsByAnyFrame('ReferenceError', genErrorConstruct);
    argsByAnyFrame('SyntaxError', genErrorConstruct);
    argsByAnyFrame('TypeError', genErrorConstruct);
    argsByAnyFrame('URIError', genErrorConstruct);

    argsByIdentity(cajaVM.allKeys, genMethod(genJSONValue));
    argsByIdentity(cajaVM.callWithEjector, genMethod(/* bad */));
    argsByIdentity(cajaVM.compileExpr, G.none);
    argsByIdentity(cajaVM.compileModule, G.none);
        // skip dealing with the function return values for now
    argsByIdentity(cajaVM.copyToImports,
          genMethod(genFancyRecord, genFancyRecord));
    argsByIdentity(cajaVM.constFunc, genMethod(G.value(null)));
    argsByIdentity(cajaVM.def, G.none);

    function ejectFn1(v) { return v; }
    function ejectFn2(v) { throw v; }
    cajaVM.def([ejectFn1, ejectFn2]);
    argsByIdentity(ejectFn1, G.none);
    argsByIdentity(ejectFn2, G.none);
    argsByIdentity(cajaVM.eject, genMethod(
        G.value(undefined, ejectFn1, ejectFn2, 'not a function'),
        G.value(undefined, 10, ejectFn1)));

    argsByIdentity(cajaVM.enforce, genMethod( // ES5/3 only
        G.value(function(){return false;},
                function(){return true;}),
        genString));
    argsByIdentity(cajaVM.enforceNat, genMethod(genSmallInteger));  //ES5/3 only
    argsByIdentity(cajaVM.Nat, genMethod(genNumber));  //SES only
    argsByIdentity(cajaVM.enforceType, genMethod(genString, genTypeName));
        //ES5/3 only
    argsByIdentity(cajaVM['ev'+'al'], genMethod(genJS));
    argsByIdentity(cajaVM.confine, genMethod(genJS));
        // TODO(kpreid): provide opt_endowments arg and code which abuses it
    argsByIdentity(cajaVM.guard, G.none);
    argsByIdentity(cajaVM.identity, genMethod(genString));  // ES5/3 only
    argsByIdentity(cajaVM.is, G.none);
    argsByIdentity(cajaVM.isFunction, genMethod(genJSONValue));  // ES5/3 only
    argsByIdentity(cajaVM.log, genMethod(genString));
    argsByIdentity(cajaVM.manifest, genMethod());
    argsByIdentity(cajaVM.makeArrayLike, genMethod(genSmallInteger));
    argsByIdentity(cajaVM.makeImports, freshResult(genMethod()));
    argsByIdentity(cajaVM.makeSealerUnsealerPair, genMethod());
    argsByIdentity(cajaVM.makeTableGuard, G.none);
    argsByIdentity(cajaVM.passesGuard, G.none);
    argsByIdentity(cajaVM.stamp, G.none);
    argsByIdentity(cajaVM.tamperProof, G.none);
    argsByIdentity(cajaVM.Token, genMethod(genString));  // ES5/3 only
    argsByIdentity(cajaVM.Trademark, genNew(genString));
    argsByIdentity(window.cajaHandleEmbed, G.none);  // TODO abuse

    argsByIdentity(window.Proxy && Proxy.create, G.none);  // TODO abuse
    argsByIdentity(window.Proxy && Proxy.createFunction, G.none);  // TODO abuse

    argsByIdentity(window.escape, genCall(genString));
    argsByIdentity(window.unescape, genCall(genString));
    argsByIdentity(encodeURI, genCall(genString));
    argsByIdentity(encodeURIComponent, genCall(genString));
    argsByIdentity(decodeURI, genCall(genString));
    argsByIdentity(decodeURIComponent, genCall(genString));
    argsByIdentity(document.open, G.none);  // global effect
    argsByIdentity(document.close, G.none);  // global effect
    argsByIdentity(document.write, G.none);  // global effect
    argsByIdentity(document.writeln, G.none);  // global effect
    var coordinateMethod = genMethod(genSmallInteger, genSmallInteger);
    argsByIdentity(window.close, G.none);  // global effect
    argsByIdentity(window.moveBy, coordinateMethod);
    argsByIdentity(window.moveTo, coordinateMethod);
    argsByIdentity(window.print, genNoArgMethod);
    argsByIdentity(window.resizeBy, G.none);  // global effect
    argsByIdentity(window.resizeTo, G.none);  // global effect
    argsByIdentity(window.scrollBy, coordinateMethod);
    argsByIdentity(window.scrollTo, coordinateMethod);
    argsByIdentity(window.getComputedStyle, freshResult(genMethod(
        G.value(elementSpecimen), genPseudoElement)));
    argsByIdentity(window.stop, G.none);  // global effect
    argsByIdentity(HTMLInputElement.prototype.select, genNoArgMethod);
    argsByIdentity(HTMLInputElement.prototype.stepDown, genNoArgMethod);
    argsByIdentity(HTMLInputElement.prototype.stepUp, genNoArgMethod);
    argsByIdentity(HTMLMediaElement.prototype.canPlayType,
        genMethod(genMediaType));
    argsByIdentity(HTMLMediaElement.prototype.fastSeek, genMethod(genNumber));
    argsByIdentity(HTMLMediaElement.prototype.load, genNoArgMethod);
    argsByIdentity(HTMLMediaElement.prototype.pause, genNoArgMethod);
    argsByIdentity(HTMLMediaElement.prototype.play, genNoArgMethod);
    argsByIdentity(HTMLTableRowElement.prototype.insertCell,
        genMethod(genSmallInteger));
    argsByIdentity(HTMLTableRowElement.prototype.deleteCell,
        genMethod(genSmallInteger));
    argsByIdentity(HTMLTableSectionElement.prototype.insertRow,
        genMethod(genSmallInteger));
    argsByIdentity(HTMLTableSectionElement.prototype.deleteRow,
        genMethod(genSmallInteger));
    argsByIdentity(HTMLTableElement.prototype.createCaption, genMethod());
    argsByIdentity(HTMLTableElement.prototype.deleteCaption, genMethod());
    argsByIdentity(HTMLTableElement.prototype.createTHead, genMethod());
    argsByIdentity(HTMLTableElement.prototype.deleteTHead, genMethod());
    argsByIdentity(HTMLTableElement.prototype.createTFoot, genMethod());
    argsByIdentity(HTMLTableElement.prototype.deleteTFoot, genMethod());
    argsByIdentity(HTMLTableElement.prototype.insertRow,
        genMethod(genSmallInteger));
    argsByIdentity(HTMLTableElement.prototype.deleteRow,
        genMethod(genSmallInteger));
    argsByIdentity(Image, genNew());  // TODO args
    argsByIdentity(Option, genNew());  // TODO args
    argsByIdentity(Audio, genNew());  // TODO args
    argsByIdentity(XMLHttpRequest, genNew());
    argsByIdentity(XMLHttpRequest.prototype.open, G.none);  // TODO stateful
    argsByIdentity(XMLHttpRequest.prototype.setRequestHeader, G.none);
        // TODO stateful
    argsByIdentity(XMLHttpRequest.prototype.send, G.none);  // TODO stateful
    argsByIdentity(XMLHttpRequest.prototype.abort, G.none);  // TODO stateful
    argsByIdentity(XMLHttpRequest.prototype.getAllResponseHeaders, G.none);
        // TODO stateful
    argsByIdentity(XMLHttpRequest.prototype.getResponseHeader, G.none);
        // TODO stateful

    argsByIdentity(setTimeout, genCall(genString, genSmallInteger));
    argsByIdentity(clearTimeout, genCall());
    argsByIdentity(setInterval, G.none);
    argsByIdentity(clearInterval, genCall());
    argsByIdentity(isNaN, genCall(genSmallInteger));
    argsByIdentity(parseInt, genCall(genString));
    argsByIdentity(parseFloat, genCall(genString));
    argsByIdentity(isFinite, genCall(genSmallInteger));

    argsByIdentity(window.StringMap /* SES only */, genNew());
    argsBySuffix('StringMap<CONSTRUCT>().get', genMethod(genString));
    argsBySuffix('StringMap<CONSTRUCT>().has', genMethod(genString));
    argsBySuffix('StringMap<CONSTRUCT>().delete', genMethod(genString));
    argsBySuffix('StringMap<CONSTRUCT>().set',
        genMethod(genString, genJSONValue));

    argsByIdentity(WeakMap, genNew());
    argsByIdentity(WeakMap.prototype['delete'],
        argsBySuffix('WeakMap<CONSTRUCT>().delete', G.none)); // TODO abuse
    argsByIdentity(WeakMap.prototype.get,
        argsBySuffix('WeakMap<CONSTRUCT>().get', G.none)); // TODO abuse
    argsByIdentity(WeakMap.prototype.set,
        argsBySuffix('WeakMap<CONSTRUCT>().set', G.none)); // TODO abuse
    argsByIdentity(WeakMap.prototype.has,
        argsBySuffix('WeakMap<CONSTRUCT>().has', G.none)); // TODO abuse
    argsBySuffix('WeakMap<CONSTRUCT>().delete___',
        argsBySuffix('WeakMap<CONSTRUCT>().get___',
        argsBySuffix('WeakMap<CONSTRUCT>().has___',
        argsBySuffix('WeakMap<CONSTRUCT>().set___',
        argsBySuffix('WeakMap<CONSTRUCT>().permitHostObjects___',
        G.none)))));  // known implementation details leak. TODO abuse

    argsByIdentity(RegExp, genNew(genRegex));
    argsByIdentity(RegExp.prototype.exec, freshResult(genMethod(genString)));
    argsByIdentity(RegExp.prototype.test, genMethod(genString));
    argsByIdentity(Math.random, genMethod());
    ['abs', 'acos', 'asin', 'atan', 'ceil', 'cos', 'exp', 'floor', 'log',
        'round', 'sin', 'sqrt', 'tan', 'atan2', 'pow', 'max', 'min'
        ].forEach(function(name) {
      argsByIdentity(Math[name], genMethod(genSmallInteger));
    });
    argsByIdentity(Date, G.tuple(G.value(CONSTRUCT, undefined),
                                    G.any(G.tuple(),
                                          G.tuple(genSmallInteger))));
    argsByIdentity(Date.now, genMethod());
    argsByIdentity(Date.parse, genMethod(genString));
    argsByIdentity(Date.UTC, G.any(
        genMethod(),  // "implementation defined" per ES5 so interesting
        genMethod(genSmallInteger),
        genMethod(genSmallInteger, genSmallInteger)));
    Object.getOwnPropertyNames(Date.prototype).forEach(function(name) {
      if (/^get|^to/.test(name)) {
        argsByIdentity(Date.prototype[name], genMethod());
      } else if (/^set/.test(name)) {
        argsByIdentity(Date.prototype[name], genMethod(genSmallInteger));
      }
    });
    argsByIdentity(JSON.stringify, genMethod(genJSONValue));
    argsByIdentity(JSON.parse, freshResult(genMethod(genJSON)));

    argsByProp('focus', genNoArgMethod);
    argsByProp('blur', genNoArgMethod);
    argsByProp('submit', G.none);
    argsByProp('reset', genNoArgMethod);
    argsByProp('createElement', genMethod(genElementName));
    argsByProp('createComment', genMethod(genString));
    argsByProp('createTextNode', genMethod(genString));
    argsByProp('createDocumentFragment', genNoArgMethod);
    argsByProp('createEvent', freshResult(genMethod(genEventClass)));
    argsByProp('initEvent', genMethod(genEventName, genBoolean, genBoolean));
    argsByProp('initUIEvent', G.none);  // implemented like initEvent
    argsByProp('initKeyEvent', G.none);  // implemented like initEvent
    argsByProp('initKeyboardEvent', G.none);  // implemented like initEvent
    argsByProp('initMouseEvent', G.none);  // implemented like initEvent
    argsByProp('dispatchEvent', genMethod(G.lazyValue(function() {
        return obtainInstance(Event, function() { return 'Event'; }); })));
    argsByProp('getAttribute',
        argsByProp('getAttributeNode',
        argsByProp('hasAttribute', genMethod(genString))));
    argsByProp('setAttribute',
        genMethod(G.value(null, 'baz'), genString));
    argsByProp('removeAttribute',
        genMethod(G.value(null, 'baz', 'definitely-absent')));
    argsByProp('removeChild', genMethod(G.value(null, elementSpecimen)));
        // TODO successful case
    argsByProp('appendChild', G.value([elementSpecimen, [elementSpecimen]]));
        // TODO successful case, handle impls wanting other 'this's
    argsByProp('insertBefore',
        G.value([elementSpecimen, [elementSpecimen, elementSpecimen]]));
        // TODO successful case, handle impls wanting other 'this's
    argsByProp('replaceChild',
        G.value([elementSpecimen, [elementSpecimen, elementSpecimen]]));
        // TODO successful case, handle impls wanting other 'this's
    argsByProp('contains',
        genMethod(G.value(elementSpecimen, document.body, null)));
    argsByProp('compareDocumentPosition',
        genMethod(G.value(elementSpecimen, document.body, null)));
    argsByProp('hasChildNodes', genNoArgMethod);
    argsByProp('cloneNode', genMethod(genBoolean));
    argsByProp('getBoundingClientRect', freshResult(genNoArgMethod));
    argsByProp('updateStyle', genNoArgMethod);
    argsByProp('getPropertyValue', genMethod(genCSSPropertyName));
    argsByProp('set stack', G.none);
    argsByProp('getContext', genMethod(G.value(undefined, null, 'bogus', '2d',
        'webgl', 'experimental-webgl')));
    argsByProp('querySelector', genMethod(genCSSSelector));
    argsByProp('querySelectorAll', freshResult(genMethod(genCSSSelector)));
    argsByProp('getElementById', genMethod(G.value(
        undefined, null, 'testUniverse', 'not an/id')));
    argsByProp('getElementsByTagName', freshResult(genMethod(genElementName)));
    argsByProp('getElementsByClassName', freshResult(genMethod(genClassName)));
    argsByProp('addEventListener', argsByProp('removeEventListener',
        genMethod(genEventName, G.value(function stubL() {}), genBoolean)));

    // NodeList and friends (currently have no exported type)
    argsByProp('item', genMethod(genSmallInteger));
    argsByProp('namedItem', genMethod(genString));
    argsByProp('add', genMethod(genString));
    argsByProp('remove', genMethod(genString));
    argsByProp('toggle', genMethod(genString));

    // 2D context (and friends) methods
    var canvas2DProto = CanvasRenderingContext2D.prototype;
    argsByIdentity(canvas2DProto.arc, genMethodAlt(genNumbers(5)));
    argsByIdentity(canvas2DProto.arcTo, genMethodAlt(genNumbers(5)));
    argsByIdentity(canvas2DProto.beginPath, genNoArgMethod);
    argsByIdentity(canvas2DProto.bezierCurveTo, genMethodAlt(genNumbers(6)));
    argsByIdentity(canvas2DProto.clearRect,
        argsByIdentity(canvas2DProto.fillRect,
            argsByIdentity(canvas2DProto.strokeRect,
                genMethodAlt(genNumbers(4)))));
    argsByIdentity(canvas2DProto.clip, genNoArgMethod);
        // TODO(kpreid): Path obj
    argsByIdentity(canvas2DProto.closePath, genNoArgMethod);
    argsByIdentity(canvas2DProto.createImageData, genMethodAlt(G.value(
        // restricting size in order to avoid large pixel arrays
        [2, 2], [-1, 2], [1, NaN])));
    argsByIdentity(canvas2DProto.createLinearGradient,
        genMethodAlt(G.value([0, 1, 2, 3])));
    argsByIdentity(canvas2DProto.createRadialGradient,
        genMethodAlt(G.value([0, 1, 2, 3, 4, 5])));
    argsByIdentity(canvas2DProto.createPattern,
        genMethodAlt(G.value([null, null])));
        // TODO(kpreid): args for createPattern (not implemented yet though)
    argsByIdentity(canvas2DProto.drawImage, genMethodAlt(G.none));
        // TODO(kpreid): unstub
    argsByIdentity(canvas2DProto.ellipse, genMethodAlt(genNumbers(7)));
    argsByIdentity(canvas2DProto.fill, genNoArgMethod);
        // TODO(kpreid): Path obj
    argsByIdentity(canvas2DProto.fillText, argsByProp('strokeText',
        genMethodAlt(genConcat(G.tuple(genString), genNumbers(3)))));
    argsByIdentity(canvas2DProto.getImageData, genMethodAlt(genConcat(
        // restricting size in order to avoid large pixel arrays
        genNumbers(2), G.value([2, 2]))));
    argsByIdentity(canvas2DProto.lineTo, genMethodAlt(genNumbers(2)));
    argsByIdentity(canvas2DProto.measureText, genMethod(genString));
    argsByIdentity(canvas2DProto.moveTo, genMethodAlt(genNumbers(2)));
    argsByIdentity(canvas2DProto.quadraticCurveTo, genMethodAlt(genNumbers(4)));
    argsByIdentity(canvas2DProto.rect, genMethodAlt(genNumbers(4)));
    argsByIdentity(canvas2DProto.save, genNoArgMethod);
    argsByIdentity(canvas2DProto.scale, genMethodAlt(genNumbers(2)));
    argsByIdentity(canvas2DProto.stroke, genNoArgMethod);
        // TODO(kpreid): Path obj
    argsByIdentity(canvas2DProto.restore, genNoArgMethod);
    argsByIdentity(canvas2DProto.rotate, genMethodAlt(genNumbers(2)));
    argsByIdentity(canvas2DProto.isPointInPath, genMethodAlt(genNumbers(2)));
        // TODO(kpreid): Path obj
    argsByIdentity(canvas2DProto.putImageData, genMethodAlt(G.none));
        // TODO(kpreid): unstub
    argsByIdentity(canvas2DProto.setTransform, genMethodAlt(genNumbers(6)));
    argsByIdentity(canvas2DProto.translate, genMethodAlt(genNumbers(2)));
    argsByIdentity(canvas2DProto.transform, genMethodAlt(genNumbers(6)));
    argsByIdentity(CanvasGradient.prototype.addColorStop,
        genMethod(genSmallInteger, genCSSColor));
    argsByIdentity(
        Object.getOwnPropertyDescriptor(ImageData.prototype, 'data').get,
        annotate(G.value([THIS, []]), function(context, thrown) {
          // Pixel arrays are not frozen
          expectedUnfrozen.setByIdentity(context.get(), true);
        }));
    argsByProp('_d_canvas_writeback', G.none);  // TODO(kpreid): hide

    // Event methods
    argsByProp('stopPropagation', G.none);
    argsByProp('preventDefault', G.none);

    // cajaVM methods etc.
    // TODO(kpreid): better variety of things to coerce
    argsByProp('coerce', genMethod(genJSONValue, genEjector));
    argsBySuffix('makeSealerUnsealerPair<THIS>().seal',
        genMethod(genJSONValue));
    // TODO(kpreid): provide actual boxes / wrong boxes
    argsBySuffix('makeSealerUnsealerPair<THIS>().unseal',
        argsBySuffix('makeSealerUnsealerPair<THIS>().optUnseal',
        genMethod(genString)));

    var tamingFrameError = tamingEnv.Error;
    expectedUnfrozen.addSpecialCase(function(context) {
      var object = context.get();
      var path = context.getPath();
      if (/get (childNodes|rows|cells|tBodies|options|style|forms|attributes)<THIS>\(\)$/
            .test(path)) {
        // non-live node lists independent of type
        return true;
      }
      var currentArrayLike = tamingEnv.cajaVM.makeArrayLike(0);
      if (object instanceof currentArrayLike) {
        return true;
      }
      if (object === currentArrayLike.prototype &&
          cajaVM.makeArrayLike.canBeFullyLive) {
        // The SES or ES5/3 proxy-based ArrayLike implementation's prototype
        // appears extensible because it is a proxy (with an unbounded set of
        // numeric properties).
        return true;
      }
      if (!inES5Mode && whichFrame(object) === 'guest') {
        // ES5/3 guest not frozen
        return true;
      }
    });
    expectedUnfrozen.setByConstructor(Window, true);
    expectedUnfrozen.setByConstructor(Document, true);
    expectedUnfrozen.setByConstructor(Node, true);
    assertTrue(location.constructor !== Object);
    expectedUnfrozen.setByConstructor(location.constructor, true);
    expectedUnfrozen.setByConstructor(Error, true);
    expectedUnfrozen.setByConstructor(tamingEnv.Error, true);
    // these types can't be coherently exported due to ArrayLike gimmick
    //expectedUnfrozen.setByConstructor(NodeList, true);
    //expectedUnfrozen.setByConstructor(NamedNodeMap, true);
    //expectedUnfrozen.setByConstructor(HTMLOptionsCollection, true);
    expectedUnfrozen.setByPathSuffix('.get stack', true);
    expectedUnfrozen.setByPathSuffix('.set stack', true);
    expectedUnfrozen.setByPathSuffix('.get stack.prototype', true);
    expectedUnfrozen.setByPathSuffix('.set stack.prototype', true);

    var obtainInstance = scanner.obtainInstance;

    // Preload instance table with nonconstructible objects
    [
      'a','abbr','acronym','address','applet','area','article','aside',
      'audio','b','base','basefont','bdi','bdo','big','blockquote','body',
      'br','button','canvas','caption','center','cite','code','col',
      'colgroup','command','data','datalist','dd','del','details','dfn',
      'dialog','dir','div','dl','dt','em','fieldset','figcaption','figure',
      'font','footer','form','frame','frameset','h1','h2','h3','h4','h5','h6',
      'head','header','hgroup','hr','html','i','iframe','img','input','ins',
      'isindex','kbd','keygen','label','legend','li','link','map','mark',
      'menu','meta','meter','nav','nobr','noembed','noframes','noscript',
      'object','ol','optgroup','option','output','p','param','pre','progress',
      'q','s','samp','script','section','select','small','source','span',
      'strike','strong','style','sub','summary','sup','table','tbody','td',
      'textarea','tfoot','th','thead','time','title','tr','track','tt','u',
      'ul','var','video','wbr'
    ].forEach(function(name) {
      var el = document.createElement(name);
      if (el.setAttribute) {  // TODO(kpreid): arguably a bug that this is
                              // necessary (for <style> which tames as opaque)
        // has attribute 'foo' but not 'bar', cf. genString
        el.setAttribute('foo', 'foo');
      }
      for (var obj = el;
           obj && getFunctionName(obj.constructor) !== 'Object';
           obj = Object.getPrototypeOf(obj)) {
        if (obj.constructor) { // conditional to workaround Firefox+ES5/3 oddity
          obtainInstance.define(obj.constructor, el);
          argsByIdentity(obj.constructor, G.none);
        }
      }
    });
    obtainInstance.define(Function, function() {});
    obtainInstance.define(Text, document.createTextNode('foo'));
    obtainInstance.define(Document, document); // TODO(kpreid): createDocument
    obtainInstance.define(Window, window);
    obtainInstance.define(Location, window.location);
    obtainInstance.define(Event, document.createEvent('HTMLEvents'));
    obtainInstance.define(Attr, (function() {
          var el = document.createElement('span');
          el.className = 'foo';
          return el.attributes[0];
        }()));
    obtainInstance.define(CSSStyleDeclaration,
        document.createElement('div').style);
    obtainInstance.define(ArrayLike, ArrayLike(
      Object.create(ArrayLike.prototype),
      function() { return 100; }, function(i) { return i; }));
    obtainInstance.define(CanvasRenderingContext2D,
        document.createElement('canvas').getContext('2d'));
    obtainInstance.define(CanvasGradient,
        document.createElement('canvas').getContext('2d').createLinearGradient(
            0, 1, 2, 3));
    obtainInstance.define(ImageData,
        document.createElement('canvas').getContext('2d').createImageData(
            2, 2));

    var output = document.getElementById('testUniverse-report');
    function write(var_args) {
      var el = document.createElement('span');
      el.className = 'scan-log';
      el.appendChild(document.createTextNode(
        Array.prototype.join.call(arguments, '')));
      output.appendChild(el);
    }
    function logProblem(description, context) {
      var text = 'Problem: ' + description + '\n' + context.toDetailsString();
      var el = document.createElement('strong');
      el.className = 'scan-error';
      el.appendChild(document.createTextNode(text + '\n'));
      output.appendChild(el);
      console.error(text);
    }
    function logGap(description) {
      var text = 'Coverage gap: ' + description;
      var el = document.createElement('strong');
      el.className = 'scan-gap';
      el.appendChild(document.createTextNode(text + '\n'));
      output.appendChild(el);
      console.warn(text);
    }

    var stopped = false;
    document.getElementById('testUniverse-stop').onclick = function() {
      stopped = true;  // for toggleNonErrors
      scanner.stop();
      return true;
    };

    var progressMeter = document.getElementById('testUniverse-progress');
    function setProgress(fraction) {
      progressMeter.setAttribute('value', String(fraction));
      directAccess.scrollToEnd();
    }

    // ES5/3 has a separate taming frame and guest frames; ES5 currently does
    // not.
    if (tamingEnv.Object !== Object) {
      // This ensures that taming-frame prototypes get meaningful names. It also
      // makes sure that its Object.prototype isn't found indirectly via the
      // .prototype.[[Prototype]] of some unfortunate function which doesn't
      // work very well as a ctor.
      ['Object', 'Function', 'Array'].forEach(function(type) {
        scanner.queue(Context.root(tamingEnv[type].prototype,
            '<taming env>.' + type + '.prototype',
            '<taming env>.' + type + '.prototype'));
      });
    }

    scanner.queue(Context.root(window, '', 'window'));

    var t0 = Date.now();
    scanner.scan();

    function finished(problemCount, gapCount, totalDone) {
      // Performance reporting
      var t1 = Date.now();
      console.info(totalDone + ' references scanned. Total elapsed time ' +
          (t1 - t0) + 'ms, speed ' + (totalDone / (t1 - t0) * 1000).toFixed(2) +
          '/s.');

      // Remove progress bar etc. as it is now clutter.
      document.getElementById('testUniverse-running-controls').style.display
          = 'none';

      // Final report, and communicate to jsunit
      if (problemCount === 0 && gapCount === 0) {
        pass('testUniverse');
      } else {
        if (!stopped) { toggleNonErrors(true); }
        jsunitFail('testUniverse',
            'Failed: ' + problemCount + ' problems and ' + gapCount +
            ' coverage gaps.');
      }
    }
  };

  /** Test the scanner's own functionality. */
  window.testScanner = function testScanner() {
    // non-identifier props in programs
    var problemPrograms = ['bar["!"]', 'bar[1]', 'bar.a'];
    var scanner = new Scanner({
      logProblem: function(description, context) {
        assertEquals(problemPrograms.shift(), context.getProgram());
      },
      finished: function() {
        assertEquals(0, problemPrograms.length);
        pass('testScanner');
      }
    });
    scanner.skip(Object.prototype);
    // each property here is an 'object is extensible' problem
    scanner.queue(Context.root(Object.freeze({a: {}, 1: {}, '!': {}}),
        'foo', 'bar'));
    scanner.scan();

    // TODO(kpreid): more meta-tests
  };

  // wire up checkbox
  var hideCheckbox;
  function toggleNonErrors(opt_state) {
    var el = document.getElementById('testUniverse-report');
    var hide = opt_state !== undefined ? opt_state : hideCheckbox.checked;
    if (hide) {
      el.className = el.className + ' scan-hide-non-errors';
    } else {
      el.className = el.className.replace(/ scan-hide-non-errors/g, '');
    }
    hideCheckbox.checked = hide;
  }
  document.addEventListener('DOMContentLoaded', function() {
    hideCheckbox = document.getElementById('hide-non-errors-checkbox');
    hideCheckbox.onclick = function() { toggleNonErrors(); };
  }, false);
  window.toggleNonErrors = toggleNonErrors;
}());
