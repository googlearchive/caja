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
 *     requestAnimationFrame, cancelAnimationFrame,
 *     cajaVM, directAccess, inES5Mode, getUrlParam,
 *     assertTrue, assertEquals, pass, jsunitFail,
 *     Event, HTMLInputElement, HTMLMediaElement, HTMLTableRowElement,
 *     HTMLTableSectionElement, HTMLTableElement, HTMLImageElement,
 *     HTMLTextAreaElement, HTMLVideoElement, HTMLButtonElement,
 *     Audio, Image, Option, XMLHttpRequest, Window, Document, Node, Element,
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

  function getPropertyDescriptor(object, prop) {
    if (object === null) { return null; }
    return Object.getOwnPropertyDescriptor(object, prop) ||
        getPropertyDescriptor(Object.getPrototypeOf(object), prop);
  }

  function getGetter(object, prop) {
    return (getPropertyDescriptor(object, prop) || {}).get;
  }
  function getSetter(object, prop) {
    return (getPropertyDescriptor(object, prop) || {}).set;
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
                      obtainInstance(selfC.get(), selfC),
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
        'self',
        function() { return noThisContext; },
        function() { return 'undefined'; });

    return {
      root: function(o, path, code) {
        return makeContext(
            o,
            path,
            'self',
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
    var checkObject = optional(options.checkObject, function() {});

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

          argGenerator(function(tuple) {
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
          });

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
        ownPropertyNames.sort(comparePropertyNames);
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
      checkObject: checkObject,
      log: write,
      logProblem: logProblem,
      logGap: logGap,
      setProgress: setProgress,
      finished: finished
    });
    var expectedUnfrozen = scanner.expectedUnfrozen;
    var expectedAlwaysThrow = scanner.expectedAlwaysThrow;
    var functionArgs = scanner.functionArgs;
    var obtainInstance = scanner.obtainInstance;

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

    // returns its parameters to help catch leaks
    function dummyFunction() {
      return Object.freeze([this].concat(
          Array.prototype.slice.call(arguments)));
    }
    cajaVM.constFunc(dummyFunction);

    // value we expect NOT to see
    var refLeakCanary = Object.freeze({
      toString: function() { return 'reference leak canary'; }
    });

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
    var genSmallInteger = G.value(-1, 0, 1, 2, 63, NaN, null, 1.5, '1', 'foo');
    var genNumber = G.any(genSmallInteger, G.value(Infinity, -Infinity,
        Math.pow(2, 53), Math.pow(2, 60)));
    var genRegexBase = G.value(null, '', 'f(.*)o', '*');
    var genRegex = G.any(
        genRegexBase,
        G.apply(function() { return new RegExp('f(.*)o'); }));
    var genPseudoElement = G.value(null, ':', ':after', ':before', ':marker',
        ':line-marker');
    var genArray = G.value(undefined, null, 'abc',
        Object.freeze(['a', , 'c']),
        Object.freeze({length: 3, 0: 'a', 2: 'c'}));
    var genFreshArray = G.any(G.value(undefined, null, 'abc'),
        G.lazyValue(function() {
          var o = ['a', , 'c'];
          expectedUnfrozen.setByIdentity(o, true);
          return o;
        }),
        G.lazyValue(function() {
          var o = {length: 3, 0: 'a', 2: 'c'};
          expectedUnfrozen.setByIdentity(o, true);
          return o;
        }));
    var genObject = G.value(Object.freeze({'foo': 1}));
    var genFreshObject = G.any(G.value(undefined, null, 'abc'),
        G.lazyValue(function() {
          var o = {'foo': 1};  // but not bar
          expectedUnfrozen.setByIdentity(o, true);
          return o;
        }));
    var genFreshFunction = G.apply(function() {
      function freshFunction() {
        return dummyFunction.apply(this, arguments);
      }
      argsByIdentity(freshFunction, genCall());
      return freshFunction;
    });
    var genJSONValue = G.any(genString, genSmallInteger,
        G.value(dummyFunction, {'foo:bar':'baz'}, null, undefined));
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
    function genAllCall(var_args) {
      return genAllCallAlt(G.tuple.apply(G.tuple, arguments));
    }
    function genMethodAlt(argGens) {
      return G.tuple(G.value(THIS), argGens);
    }
    function genAllCallAlt(argGens) {
      return G.tuple(
          G.value(CONSTRUCT, THIS, undefined),
          argGens);
    }
    // Setters return nothing interesting so we just want to make them crash
    // which we hopefully can do by using an object of no particular type
    var genAccessorSet = genMethod(G.value(cajaVM.def({
      toString: function() { return '<setter garbage>'; }
    })));
    /** Add third value-callback to an arguments generator */
    function annotate(calls, callback) {
      return G.apply(function (call) {
        function composed(context, thrown) {
          original(context, thrown);
          callback(context, thrown);
        }
        if (call[2]) {
          var original = call[2];
          return [call[0], call[1], composed];
        } else {
          return [call[0], call[1], callback];
        }
      }, calls);
    }
    /** Note that the result is fresh and therefore expected to be extensible */
    function freshResult(calls) {
      return annotate(calls, function(context, thrown) {
        var object = context.get();
        if (object === Object(object)) {  // not a primitive
          expectedUnfrozen.setByIdentity(object, true);
        }
      });
    }

    var elementSpecimen = document.createElement('an-element-for-tests');
    var genFreshElement = G.apply(function() {
      return document.createElement('fresh-element');
    });
    
    var ArrayLike = cajaVM.makeArrayLike(largestSmallInteger);

    function isArrayLikeCtor(o) {
      // matching code text is a kludge to be able to recognize usurped
      // smaller ArrayLikes.
      return o === ArrayLike || /proto instanceof BAL/.test(o.toString());
    }

    var tamingEnv = directAccess.evalInTamingFrame('cajaVM.sharedImports');
    function forEachFrame(code, callback) {
      // actually, any _should-be-visible_ frame, i.e. guest and taming.

      // Note: Can't use evalInTamingFrame to get from the taming frame, because
      // we want the ES5/3-virtualized view.
      try {
        if (tamingEnv.Object !== window.Object) {
          callback(simpleEval(tamingEnv, code));
          callback(simpleEval(window, code));
        } else {
          callback(simpleEval(window, code));
        }
      } catch (e) {
        // ignore ES5/3 restriction
        if (e.message !== 'Property name may not end in double underscore.') {
          throw e;
        } else {
          return Ref.all();
        }
      }
    }
    function RefAnyFrame(code) {
      var refs = [];
      forEachFrame(code, function(obj) {
        refs.push(Ref.is(obj));
      });
      return Ref.all.apply(Ref, refs);
    }

    function arrayLikeStubLength() { return 2; }
    function arrayLikeStubGet(i) { return i; }
    functionArgs.addSpecialCase(function(context) {
      var fun = context.get();
      var path = context.getPath();
      var str = String(fun);
      var name = getFunctionName(fun);
      if (/^\[domado inert constructor(?:.*)\]$/.test(str)) {
        // inert ctor -- should throw
        expectedAlwaysThrow.setByIdentity(fun, true);  // just in time!
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
        // TODO(kpreid): test more abuses of the interface
        return freshResult(genCallAlt(G.apply(
            function(arrayLikeProto) {
              var ourProto = Object.freeze(Object.create(arrayLikeProto));
              return [ourProto, arrayLikeStubLength, arrayLikeStubGet];
            },
            G.value(fun.prototype, Object.prototype))));
      } else {
        return null;
      }
    });
    // TODO(kpreid): Replace all of these shorthands with direct use of ref objs
    function argsByIdentity(obj, g) {
      functionArgs.set(Ref.is(obj), g);
      return g;
    }
    function argsByProp(p, g) {
      functionArgs.set(Ref.prop(p), g);
      return g;
    }
    function argsBySuffix(p, g) {
      functionArgs.set(Ref.path(p), g);
      return g;
    }
    function argsByAnyFrame(code, g) {
      functionArgs.set(RefAnyFrame(code), g);
      return g;
    }

    // can be made to not throw, but we don't want to do that and wipe output
    expectedAlwaysThrow.setByPathSuffix('.set body', true);

    argsByAnyFrame('Object', freshResult(G.any(
        genAllCall(),
        genAllCall(genJSONValue))));
    argsByAnyFrame('Object.create', freshResult(genAllCall(
        G.value(null, Object.prototype))));
    var oneOrNoObj = G.any(genMethod(), genMethod(genJSONValue));
    argsByAnyFrame('Object.defineProperties',
        genAllCall(genFreshObject, G.apply(
            function(s, p) {
              var o = {};
              o[s] = p;
              return Object.freeze(o);
            },
            genString,
            genProperty)));
    argsByAnyFrame('Object.defineProperty',
        genAllCall(genFreshObject, genString, genProperty));
    argsByAnyFrame('Object.getOwnPropertyDescriptor',
        freshResult(genMethod(genObject, genString)));
    argsByAnyFrame('Object.getOwnPropertyNames',
        argsByAnyFrame('Object.keys', freshResult(oneOrNoObj)));
    // to avoid a matching problem, don't invoke getPrototypeOf on a function
    // and receive Function.prototype without identifying it as a .prototype
    argsByAnyFrame('Object.getPrototypeOf', G.any(genMethod(), genMethod(
      G.any(genObject, G.value(null)))));
    argsByAnyFrame('Object.is', G.none);  // TODO abuse
    argsByAnyFrame('Object.isExtensible', oneOrNoObj);
    argsByAnyFrame('Object.isFrozen', oneOrNoObj);
    argsByAnyFrame('Object.isSealed', oneOrNoObj);
    argsByAnyFrame('Object.freeze', genMethod(genFreshObject));
    argsByAnyFrame('Object.preventExtensions', genMethod(genFreshObject));
    argsByAnyFrame('Object.seal', genMethod(genFreshObject));
    argsByAnyFrame('Object.prototype.__defineGetter__', G.none);  // TODO abuse
    argsByAnyFrame('Object.prototype.__defineSetter__', G.none);  // TODO abuse
    argsByAnyFrame('Object.prototype.__lookupGetter__', G.none);  // TODO abuse
    argsByAnyFrame('Object.prototype.__lookupSetter__', G.none);  // TODO abuse

    // Chrome has a "non-generic" setter here
    if (inES5Mode) {
      expectedAlwaysThrow.setByIdentity(
          getSetter(Object.prototype, '__proto__'), true);
    }

    argsByProp('toString', annotate(genNoArgMethod, function(context, thrown) {
      if (thrown) {
        scanner.noteProblem('toString threw ' + context.get(), context);
      } else if (typeof context.get() !== 'string') {
        scanner.noteProblem('toString returned non-string', context);
      }
    }));
    argsByProp('valueOf', genNoArgMethod);
    argsByProp('hasOwnProperty', genMethod(genString));
    argsByProp('isPrototypeOf', genMethod(genObject));  // TODO abuse more
    argsByProp('propertyIsEnumerable', genMethod(genString));  // TODO abuse

    argsByProp('toLocaleString', genNoArgMethod);
    // Workaround for
    // <https://code.google.com/p/google-caja/issues/detail?id=1840>.
    if (!inES5Mode) {
      ['String.prototype.localeCompare',
          'Number.prototype.toLocaleString',
          'Date.prototype.toLocaleString',
          'Date.prototype.toLocaleDateString',
          'Date.prototype.toLocaleTimeString'].forEach(function(expr) {
        expectedAlwaysThrow.mark(RefAnyFrame(expr));
      });
    }

    argsByAnyFrame('Function', G.none);  // TODO deal with function return val
    argsByAnyFrame('Function.prototype', genCall());
    argsByIdentity(dummyFunction, genCall());
    argsByAnyFrame('Function.prototype.apply', genMethod(genObject, genArray));
    argsByAnyFrame('Function.prototype.bind',
        annotate(genMethod(genObject), function(context, thrown) {
          if (!thrown) {
            expectedUnfrozen.setByIdentity(context.get(), true);
            argsByIdentity(context.get(), G.value([refLeakCanary, []]));
          }
        }));
    argsByAnyFrame('Function.prototype.call', genMethod(genObject, genObject));
    argsByAnyFrame('Function.prototype.toString',
        // TODO test invocation on Function.prototype itself
        G.tuple(G.value(THIS), G.tuple()));
    [function guestFn(){}, window.setTimeout].forEach(function(f) {
      if (!inES5Mode) {
        // Function.prototype() is necessarily broken in ES5/3
        var proto = Object.getPrototypeOf(f);
        expectedAlwaysThrow.setByIdentity(proto, true);
      }
      expectedAlwaysThrow.setByIdentity(getGetter(f, 'arguments'), true);
      expectedAlwaysThrow.setByIdentity(getGetter(f, 'caller'), true);
    });


    argsByIdentity(Number, genAllCall(genSmallInteger));
    argsByAnyFrame('Number.prototype.toExponential',
        genMethod(genSmallInteger));
    argsByAnyFrame('Number.prototype.toFixed', genMethod(genSmallInteger));
    argsByAnyFrame('Number.prototype.toPrecision', genMethod(genSmallInteger));

    function genArrayCall(argsGen) {
      return G.tuple(
          G.any(genFreshArray, genArray, G.value(undefined)),
          argsGen);
    }
    argsByAnyFrame('Array', freshResult(G.any(
        genAllCall(),
        genAllCall(genSmallInteger))));
    argsByAnyFrame('Array.isArray', genArrayCall(G.tuple(genArray)));
    argsByAnyFrame('Array.prototype.concat',
        freshResult(genArrayCall(G.tuple(genArray))));
    argsByAnyFrame('Array.prototype.every',
        argsByAnyFrame('Array.prototype.some',
            genArrayCall(G.value([dummyFunction]))));
    argsByAnyFrame('Array.prototype.filter',
        freshResult(genArrayCall(G.value([dummyFunction]))));
    argsByAnyFrame('Array.prototype.forEach',
        genArrayCall(G.value([dummyFunction])));
    argsByAnyFrame('Array.prototype.indexOf',
        genArrayCall(G.value(['a', 'z'])));
    argsByAnyFrame('Array.prototype.join', genArrayCall(G.tuple(genString)));
    argsByAnyFrame('Array.prototype.lastIndexOf',
        genArrayCall(G.tuple(genJSONValue)));
    argsByAnyFrame('Array.prototype.map',
        freshResult(genArrayCall(G.tuple(G.value(dummyFunction), genArray))));
    argsByAnyFrame('Array.prototype.pop', genArrayCall(G.value([])));
    argsByAnyFrame('Array.prototype.push', genArrayCall(G.value([])));
    argsByAnyFrame('Array.prototype.reduce',
        argsByAnyFrame('Array.prototype.reduceRight',
            genArrayCall(G.tuple(G.value(dummyFunction), genJSONValue))));
    argsByAnyFrame('Array.prototype.reverse',
        genArrayCall(G.value([])));
    argsByAnyFrame('Array.prototype.shift', genArrayCall(G.value([])));
    argsByAnyFrame('Array.prototype.slice', freshResult(
        genArrayCall(genNumbers(2))));
    argsByAnyFrame('Array.prototype.sort', G.none);  // TODO wedges in Chrome
    expectedAlwaysThrow.setByIdentity(Array.prototype.sort, true);
    argsByAnyFrame('Array.prototype.splice', freshResult(
        genArrayCall(genNumbers(2))));
    argsByAnyFrame('Array.prototype.unshift',
        genArrayCall(G.tuple(genJSONValue)));
    if (!inES5Mode) {
      // without this, call would be applied to frozen array
      // in ES5 mode Array.prototype.length is a data property
      // Note: Used to use genSmallInteger but that triggered a crash bug in
      // Chrome. TODO(kpreid): Isolate and report bug.
      forEachFrame('Array.prototype', function (proto) {
        argsByIdentity(getSetter(proto, 'length'),
            genArrayCall(G.tuple(G.value(0))));
      });
    }

    argsByAnyFrame('Boolean', genAllCall(genBoolean));

    argsByAnyFrame('String', genAllCall(genString));
    argsByAnyFrame('String.fromCharCode', genMethod(genSmallInteger));
    ['big', 'blink', 'bold', 'fixed', 'italics', 'small', 'strong', 'strike',
        'sub', 'sup'].forEach(function(name) {
      argsByAnyFrame('String.prototype.' + name, genNoArgMethod);
    });
    ['anchor', 'fontcolor', 'fontsize', 'link'].forEach(function(name) {
      argsByAnyFrame('String.prototype.' + name, genMethod(genString));
    });
    argsByAnyFrame('String.prototype.charAt', genMethod(genNumber));
    argsByAnyFrame('String.prototype.charCodeAt', genMethod(genNumber));
    argsByAnyFrame('String.prototype.concat', genMethod(genString));
    argsByAnyFrame('String.prototype.indexOf', genMethod(genString));
    argsByAnyFrame('String.prototype.lastIndexOf', genMethod(genString));
    argsByAnyFrame('String.prototype.localeCompare', genMethod(genString));
    argsByAnyFrame('String.prototype.match',
        argsByAnyFrame('String.prototype.search',
            freshResult(genMethod(genRegex))));
    argsByAnyFrame('String.prototype.replace',
        genMethod(genRegex, genString));
    argsByAnyFrame('String.prototype.split',
        freshResult(genMethod(genRegex)));
    argsByAnyFrame('String.prototype.slice',
        argsByAnyFrame('String.prototype.substr',
            argsByAnyFrame('String.prototype.substring',
                G.any(
                  genMethod,
                  genMethod(genSmallInteger),
                  genMethod(genSmallInteger, genSmallInteger)))));
    argsByAnyFrame('String.prototype.toLocaleLowerCase', genNoArgMethod);
    argsByAnyFrame('String.prototype.toLocaleUpperCase', genNoArgMethod);
    argsByAnyFrame('String.prototype.toLowerCase', genNoArgMethod);
    argsByAnyFrame('String.prototype.toUpperCase', genNoArgMethod);
    argsByAnyFrame('String.prototype.trim', genNoArgMethod);
    argsByAnyFrame('String.prototype.trimLeft', genNoArgMethod);
    argsByAnyFrame('String.prototype.trimRight', genNoArgMethod);

    var genErrorConstruct = genAllCall(genString);
    argsByAnyFrame('Error', genErrorConstruct);
    argsByAnyFrame('EvalError', genErrorConstruct);
    argsByAnyFrame('RangeError', genErrorConstruct);
    argsByAnyFrame('ReferenceError', genErrorConstruct);
    argsByAnyFrame('SyntaxError', genErrorConstruct);
    argsByAnyFrame('TypeError', genErrorConstruct);
    argsByAnyFrame('URIError', genErrorConstruct);

    argsByIdentity(cajaVM.allKeys, genMethod(genJSONValue));
    function cweF1(ejector) {
      ejector('foo');
      // TODO should be noteProblem but context isn't optional
      scanner.noteGap("ejector didn't throw");
    }
    function cweF2(ejector) {
      return 1;
    }
    argsByIdentity(cajaVM.callWithEjector, genMethod(G.value(cweF1, cweF2)));
    argsByIdentity(cajaVM.compileExpr, G.none);
    argsByIdentity(cajaVM.compileModule, G.none);
        // skip dealing with the function return values for now
    argsByIdentity(cajaVM.copyToImports,
          genMethod(genFancyRecord, genFancyRecord));
    argsByIdentity(cajaVM.constFunc, genMethod(G.any(
          G.value(null), genFreshFunction)));
    argsByIdentity(cajaVM.def, G.none);

    function ejectFn1(v) { return v; }
    function ejectFn2(v) { throw v; }
    cajaVM.def([ejectFn1, ejectFn2]);
    argsByIdentity(ejectFn1, G.none);
    argsByIdentity(ejectFn2, G.none);
    argsByIdentity(cajaVM.eject, genMethod(
        G.value(undefined, ejectFn1, ejectFn2, 'not a function'),
        G.value(undefined, 10, ejectFn1)));
    expectedAlwaysThrow.setByIdentity(cajaVM.eject, true);

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
    argsByIdentity(cajaVM.makeTableGuard, G.none);
    argsByIdentity(cajaVM.passesGuard, G.none);
    argsByIdentity(cajaVM.stamp,
        // TODO more cases
        genMethod(G.tuple(G.apply(function() {
          return cajaVM.Trademark('foo').stamp;
        })), genFreshObject));
    argsByIdentity(cajaVM.tamperProof, G.none);
    argsByIdentity(cajaVM.Token, genMethod(genString));  // ES5/3 only
    argsByIdentity(window.cajaHandleEmbed, G.none);  // TODO abuse

    // sealers
    var otherSealer = cajaVM.makeSealerUnsealerPair();
    var otherBox = otherSealer.seal(refLeakCanary);
    argsByIdentity(cajaVM.makeSealerUnsealerPair, annotate(genMethod(),
        function(context, thrown) {
          if (!thrown) {
            var pair = context.get();
            var box = pair.seal(1);
            argsByIdentity(pair.seal, genMethod(genJSONValue));
            var unsealMethod = genMethod(G.any(
                genString, // bogus
                G.value(otherBox),
                G.value(box)));
            argsByIdentity(pair.unseal, unsealMethod);
            argsByIdentity(pair.optUnseal, freshResult(unsealMethod));
          }
        }));

    // guards
    function genCoerceCall(genValue) {
      // includes failure
      return genMethod(G.any(genJSONValue, genValue), genEjector);
    }
    argsByIdentity(cajaVM.GuardT.coerce, genCoerceCall(G.value(cajaVM.GuardT)));
    //argsByProp('coerce', genMethod(genJSONValue, genEjector));
    argsByIdentity(cajaVM.Trademark, annotate(genNew(genString),
        function(context, thrown) {
          if (!thrown) {
            var trademark = context.get();
            var instance = {};
            cajaVM.stamp([trademark.stamp], instance);
            argsByIdentity(trademark.guard.coerce,
                genCoerceCall(G.value(instance)));
          }
        }));

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

    var numInputSpecimen = document.createElement('input');
    numInputSpecimen.type = 'number';
    numInputSpecimen.value = 1;  // must have a value else stepUp throws
    var plainInputSpecimen = document.createElement('input');
    argsByIdentity(HTMLInputElement.prototype.select, genNoArgMethod);
    argsByIdentity(HTMLInputElement.prototype.stepDown,
        argsByIdentity(HTMLInputElement.prototype.stepUp,
            G.tuple(
                G.value(numInputSpecimen, plainInputSpecimen, null),
                G.value([]))));
    argsByIdentity(getSetter(HTMLInputElement.prototype, 'valueAsNumber'),
        G.tuple(
            G.value(numInputSpecimen, plainInputSpecimen, null),
            G.tuple(genNumber)));
    argsByIdentity(getSetter(HTMLInputElement.prototype, 'size'),
        genMethod(genSmallInteger));
    // Temporary workaround:
    // Firefox does not implement any case where stepUp and friends don't throw
    if (/Gecko/.test(window.navigator.userAgent)) {
      expectedAlwaysThrow.setByIdentity(
          HTMLInputElement.prototype.stepUp, true);
      expectedAlwaysThrow.setByIdentity(
          HTMLInputElement.prototype.stepDown, true);
      expectedAlwaysThrow.setByIdentity(
          getSetter(HTMLInputElement.prototype, 'valueAsNumber'), true);
    }

    argsByIdentity(HTMLMediaElement.prototype.canPlayType,
        genMethod(genMediaType));
    argsByIdentity(HTMLMediaElement.prototype.fastSeek, genMethod(genNumber));
    argsByIdentity(HTMLMediaElement.prototype.load, genNoArgMethod);
    argsByIdentity(HTMLMediaElement.prototype.pause, genNoArgMethod);
    argsByIdentity(HTMLMediaElement.prototype.play, genNoArgMethod);
    // TODO(kpreid): Why do these throw; can we set up the initial state so they
    // don't?
    if (/Gecko/.test(window.navigator.userAgent)) {
      expectedAlwaysThrow.setByIdentity(
          getSetter(HTMLMediaElement.prototype, 'defaultPlaybackRate'), true);
      expectedAlwaysThrow.setByIdentity(
          getSetter(HTMLMediaElement.prototype, 'playbackRate'), true);
    }

    function genIndexMethod(parentName, childName, opt_grandchild) {
      return G.tuple(
        G.apply(function() {
          var p = document.createElement(parentName);
          var c = document.createElement(childName);
          p.appendChild(c);
          if (opt_grandchild) {
            c.appendChild(document.createElement(opt_grandchild));
          }
          return p;
        }),
        G.tuple(genSmallInteger));
    }
    argsByIdentity(HTMLTableRowElement.prototype.insertCell,
        genIndexMethod('tr', 'td'));
    argsByIdentity(HTMLTableRowElement.prototype.deleteCell,
        genIndexMethod('tr', 'td'));
    argsByIdentity(HTMLTableSectionElement.prototype.insertRow,
        genIndexMethod('tbody', 'tr'));
    argsByIdentity(HTMLTableSectionElement.prototype.deleteRow,
        genIndexMethod('tbody', 'tr'));
    argsByIdentity(HTMLTableElement.prototype.createCaption, genMethod());
    argsByIdentity(HTMLTableElement.prototype.deleteCaption, genMethod());
    argsByIdentity(HTMLTableElement.prototype.createTHead, genMethod());
    argsByIdentity(HTMLTableElement.prototype.deleteTHead, genMethod());
    argsByIdentity(HTMLTableElement.prototype.createTFoot, genMethod());
    argsByIdentity(HTMLTableElement.prototype.deleteTFoot, genMethod());
    argsByIdentity(HTMLTableElement.prototype.insertRow,
        genIndexMethod('table', 'tbody', 'tr'));
    argsByIdentity(HTMLTableElement.prototype.deleteRow,
        genIndexMethod('table', 'tbody', 'tr'));

    argsByIdentity(Image, genNew());  // TODO args
    argsByIdentity(Option, genNew());  // TODO args
    argsByIdentity(Audio, genNew());  // TODO args

    argsByIdentity(XMLHttpRequest, genAllCall());
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
    if (window.requestAnimationFrame) {
      argsByIdentity(requestAnimationFrame, genCall(genString));
      argsByIdentity(cancelAnimationFrame, genCall());
    }
    argsByIdentity(isNaN, genCall(genSmallInteger));
    argsByIdentity(parseInt, genCall(genString));
    argsByIdentity(parseFloat, genCall(genString));
    argsByIdentity(isFinite, genCall(genSmallInteger));

    argsByIdentity(window.StringMap /* SES only */, annotate(
        freshResult(genAllCall()), function(context, thrown) {
      if (!thrown) {
        argsByIdentity(context.get().get, genMethod(genString));
        argsByIdentity(context.get().has, genMethod(genString));
        argsByIdentity(context.get()['delete'], genMethod(genString));
        argsByIdentity(context.get().set,
            genMethod(genString, genJSONValue));
      }
    }));

    argsByIdentity(WeakMap, annotate(
        freshResult(genAllCall()), function(context, thrown) {
      if (!thrown) {
        if (inES5Mode) {  // would fail on es53
          // known harmless implementation details leak. TODO abuse anyway
          argsByIdentity(context.get()['delete___'],
              argsByIdentity(context.get()['get___'],
              argsByIdentity(context.get()['has___'],
              argsByIdentity(context.get()['set___'],
              argsByIdentity(context.get()['permitHostObjects___'],
              G.none)))));
        }

        argsByIdentity(context.get()['delete'], G.none); // TODO abuse
        argsByIdentity(context.get().get, G.none); // TODO abuse
        argsByIdentity(context.get().set, G.none); // TODO abuse
        argsByIdentity(context.get().has, G.none); // TODO abuse
      }
    }));
    // some WeakMap impls are prototype based, some are closure based
    argsByIdentity(WeakMap.prototype['delete'], G.none); // TODO abuse
    argsByIdentity(WeakMap.prototype.get, G.none); // TODO abuse
    argsByIdentity(WeakMap.prototype.set, G.none); // TODO abuse
    argsByIdentity(WeakMap.prototype.has, G.none); // TODO abuse

    expectedAlwaysThrow.setByIdentity(
        getSetter(window, 'location'), true);

    argsByIdentity(RegExp, freshResult(genAllCall(genRegex)));
    argsByIdentity(RegExp.prototype.exec, freshResult(genMethod(genString)));
    argsByIdentity(RegExp.prototype.test, genMethod(genString));
    argsByIdentity(Math.random, genMethod());
    ['abs', 'acos', 'asin', 'atan', 'ceil', 'cos', 'exp', 'floor', 'log',
        'round', 'sin', 'sqrt', 'tan', 'atan2', 'pow', 'max', 'min'
        ].forEach(function(name) {
      argsByIdentity(Math[name], genMethod(genSmallInteger));
    });
    argsByIdentity(Date, freshResult(genAllCallAlt(G.any(
        G.tuple(),
        G.tuple(genSmallInteger)))));
    argsByIdentity(Date.now, genMethod());
    argsByIdentity(Date.parse, genMethod(genString));
    argsByIdentity(Date.UTC, G.any(
        genMethod(),  // "implementation defined" per ES5 so interesting
        genMethod(genSmallInteger),
        genMethod(genSmallInteger, genSmallInteger)));
    // TODO instead of genFreshDate, generalize obtainInstance so it can take
    // care of this.
    var genFreshDate = G.apply(function() { return new Date(); });
    Object.getOwnPropertyNames(Date.prototype).forEach(function(name) {
      if (/^get|^to/.test(name)) {
        argsByIdentity(Date.prototype[name], G.tuple(genFreshDate, G.tuple()));
      } else if (/^set/.test(name)) {
        argsByIdentity(Date.prototype[name],
            G.tuple(genFreshDate, G.tuple(genSmallInteger)));
      }
    });
    argsByIdentity(JSON.stringify, genMethod(genJSONValue));
    argsByIdentity(JSON.parse, freshResult(genMethod(genJSON)));

    argsByProp('focus', genNoArgMethod);
    argsByProp('blur', genNoArgMethod);
    argsByProp('createElement', genMethod(genElementName));
    argsByProp('createComment', genMethod(genString));
    argsByProp('createTextNode', genMethod(genString));
    argsByProp('createDocumentFragment', genNoArgMethod);
    argsByProp('createEvent', freshResult(genMethod(genEventClass)));
    argsByProp('initEvent', genMethod(genEventName, genBoolean, genBoolean));
    argsByProp('initCustomEvent', genMethod(genEventName, genBoolean,
          genBoolean, genJSONValue /* TODO(kpreid): interesting objects */));
    argsByProp('initUIEvent', G.none);  // implemented like initEvent
    argsByProp('initKeyEvent', G.none);  // implemented like initEvent
    argsByProp('initKeyboardEvent', G.none);  // implemented like initEvent
    argsByProp('initMouseEvent', G.none);  // implemented like initEvent
    argsByProp('dispatchEvent', genMethod(G.apply(function() {
      var e = document.createEvent('CustomEvent');
      e.initCustomEvent('foo', true, true, 1);
      return e;
    })));
    argsByProp('getAttribute',
        argsByProp('getAttributeNode',
        argsByProp('hasAttribute', genMethod(genString))));
    argsByProp('setAttribute',
        genMethod(G.value(null, 'baz'), genString));
    argsByProp('removeAttribute',
        genMethod(G.value(null, 'baz', 'definitely-absent')));
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
    argsByProp('getElementsByName', freshResult(genMethod(genString)));
    argsByProp('addEventListener', argsByProp('removeEventListener',
        genMethod(genEventName, G.value(function stubL() {}), genBoolean)));

    // Node manipulation
    argsByIdentity(Element.prototype.removeChild, G.any(
        G.value([undefined, [elementSpecimen]]),
        G.value([elementSpecimen, [elementSpecimen]]),
        G.value([elementSpecimen, [null]]),
        G.apply(function() {
          var e1 = document.createElement('removeChild1');
          var e2 = document.createElement('removeChild2');
          e1.appendChild(e2);
          return [e1, [e2]];
        })));
    argsByIdentity(Element.prototype.appendChild, G.any(
        G.value([elementSpecimen, [elementSpecimen]]),  // fails
        G.tuple(genFreshElement, G.tuple(genFreshElement))));
    argsByIdentity(Element.prototype.insertBefore,
        argsByIdentity(Element.prototype.replaceChild, G.any(
            G.value([elementSpecimen, [elementSpecimen]]),
            G.value([elementSpecimen, [null]]),
            G.apply(function() {
              var e1 = document.createElement('insertBefore1');
              var e2 = document.createElement('insertBefore2');
              var e3 = document.createElement('insertBefore3');
              e1.appendChild(e2);
              return [e1, [e3, e2]];
            }))));
    // global side effect
    [
      Document.prototype.appendChild,
      Document.prototype.removeChild,
      Document.prototype.insertBefore,
      Document.prototype.replaceChild
    ].forEach(function(o) {
      argsByIdentity(o, G.none);
      expectedAlwaysThrow.setByIdentity(o, true);  // don't complain no coverage
    });

    // Attr stubs
    [
      Attr.prototype.cloneNode,
      Attr.prototype.appendChild,
      Attr.prototype.removeChild,
      Attr.prototype.insertBefore,
      Attr.prototype.replaceChild,
      getGetter(Attr.prototype, 'attributes')
    ].forEach(function(o) {
      argsByIdentity(o, genMethod());
      expectedAlwaysThrow.setByIdentity(o, true);
    });

    // NodeList and friends (currently have no exported type)
    argsByProp('item', genMethod(genSmallInteger));
    argsByProp('namedItem', genMethod(genString));
    argsByProp('add', genMethod(genString));
    argsByProp('remove', genMethod(genString));
    argsByProp('toggle', genMethod(genString));

    // Forms
    argsByProp('submit', G.none);
    argsByProp('reset', genNoArgMethod);
    functionArgs.set(
        Ref.all(
            Ref.is(getSetter(HTMLInputElement.prototype, 'type')),
            Ref.is(getSetter(HTMLButtonElement.prototype, 'type'))),
        genMethod(G.value('text', 'submit', 'range')));
    expectedAlwaysThrow.mark(
        Ref.is(getSetter(HTMLTextAreaElement.prototype, 'type')));

    // Misc elements
    var roDims = Ref.all(
        Ref.is(getSetter(HTMLImageElement.prototype, 'naturalHeight')),
        Ref.is(getSetter(HTMLImageElement.prototype, 'naturalWidth')),
        Ref.is(getSetter(HTMLVideoElement.prototype, 'videoHeight')),
        Ref.is(getSetter(HTMLVideoElement.prototype, 'videoWidth')));
    functionArgs.set(roDims, genMethod(genSmallInteger));
    expectedAlwaysThrow.mark(roDims);
    expectedAlwaysThrow.mark(
        Ref.is(getSetter(HTMLImageElement.prototype, 'complete')));


    // 2D context (and friends) methods
    var canvas2DProto = CanvasRenderingContext2D.prototype;
    argsByIdentity(canvas2DProto.arc, genMethodAlt(
        genConcat(genNumbers(5), genBoolean)));
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
    expectedAlwaysThrow.setByIdentity(canvas2DProto.createPattern, true);
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
    argsByIdentity(canvas2DProto.rotate, genMethodAlt(genNumbers(1)));
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

    // TODO(kpreid): Primitive wrappers are likely indicative of a mistake and
    // should be complained about (except when we do things like messing with
    // Array.prototype.concat, which is why that isn't done already.)
    expectedUnfrozen.setByConstructor(String, true);
    expectedUnfrozen.setByConstructor(Number, true);
    expectedUnfrozen.setByConstructor(Boolean, true);
    expectedUnfrozen.setByConstructor(tamingEnv.String, true);
    expectedUnfrozen.setByConstructor(tamingEnv.Number, true);
    expectedUnfrozen.setByConstructor(tamingEnv.Boolean, true);

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
    obtainInstance.define(Function, dummyFunction);
    if (!inES5Mode) {
      obtainInstance.define(tamingEnv.Function, tamingEnv.cajaVM.identity);
    }
    obtainInstance.define(Text, document.createTextNode('foo'));
    obtainInstance.define(Document, document); // TODO(kpreid): createDocument
    obtainInstance.define(Window, window);
    obtainInstance.define(Location, window.location);
    obtainInstance.define(Event, (function() {
      var e = document.createEvent('HTMLEvents');
      e.initEvent('foo', true, true);
      return e;
    }()));
    obtainInstance.define(Attr, (function() {
          var el = document.createElement('span');
          el.className = 'foo';
          return el.attributes[0];
        }()));
    obtainInstance.define(CSSStyleDeclaration,
        document.createElement('div').style);
    obtainInstance.define(Array, Object.freeze(['a', , 'c']));
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

    function checkObject(context) {
      var object = context.get();
      var frameOfObject = whichFrame(object);
      // TODO(kpreid): Make the tests a function parameter to Scanner
      if (frameOfObject !== 'guest' && frameOfObject !== 'taming' &&
          frameOfObject !== 'neutral') {
        scanner.noteProblem('Object from a ' + frameOfObject + ' frame',
            context);
        return true; // no point in further analyzing, is doomed
      }
      if (typeof object === 'function' &&
          /function Tame(?!XMLHttpRequest)/
              .test(object.toString())) {
        scanner.noteProblem('Object is a taming ctor', context);
      }
      if (object === refLeakCanary) {
        scanner.noteProblem('Reference leak canary leaked', context);
      }
      return false;
    }

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
    function logGap(description, opt_context) {
      var text = 'Coverage gap: ' + description;
      if (opt_context) {
        text += '\n' + opt_context.toDetailsString();
      }
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
      // String (wrapper) is included because it can be found in ES5/3 as
      // Array.prototype.concat.call('foo', ...).
      ['Object', 'Function', 'Array', 'String'].forEach(function(type) {
        var ctorC = Context.root(tamingEnv[type],
            '<taming env>.' + type,
            '<taming env>.' + type);
        var proto = ctorC.get().prototype;
        var protoC = ctorC.property('prototype', proto,
            obtainInstance);
        scanner.queue(protoC);
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
    scanner.skip('Object');
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
