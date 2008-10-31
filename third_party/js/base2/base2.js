// timestamp: Sun, 06 Jan 2008 18:17:45
/*
  base2 - copyright 2007-2008, Dean Edwards
  http://code.google.com/p/base2/
  http://www.opensource.org/licenses/mit-license.php
  
  Contributors:
    Doeke Zanstra
*/

var base2 = {
  name:    "base2",
  version: "1.0 (beta 2)",
  exports:
    "Base,Package,Abstract,Module,Enumerable,Map,Collection,RegGrp,"+
    "assert,assertArity,assertType,assignID,copy,detect,extend,"+
    "forEach,format,global,instanceOf,match,rescape,slice,trim,typeOf,"+
    "I,K,Undefined,Null,True,False,bind,delegate,flip,not,unbind",
  
  global: this, // the window object in a browser environment
  
  // this is defined here because it must be defined in the global scope
  detect: new function(_) {  
    // Two types of detection:
    //  1. Object detection
    //    e.g. detect("(java)");
    //    e.g. detect("!(document.addEventListener)");
    //  2. Platform detection (browser sniffing)
    //    e.g. detect("MSIE");
    //    e.g. detect("MSIE|opera");
        
    var global = _;
    var jscript = NaN/*@cc_on||@_jscript_version@*/; // http://dean.edwards.name/weblog/2007/03/sniff/#comment85164
    var java = _.java ? true : false;
    if (_.navigator) {
      var MSIE = /MSIE[\d.]+/g;
      var element = document.createElement("span");
      // Close up the space between name and version number.
      //  e.g. MSIE 6 -> MSIE6
      var userAgent = navigator.userAgent.replace(/([a-z])[\s\/](\d)/gi, "$1$2");
      // Fix opera's (and others) user agent string.
      if (!jscript) userAgent = userAgent.replace(MSIE, "");
      if (MSIE.test(userAgent)) userAgent = userAgent.match(MSIE)[0] + " " + userAgent.replace(MSIE, "");
      userAgent = navigator.platform + " " + userAgent;
      java &= navigator.javaEnabled();
    }
    
    return function(expression) {
      var r = false;
      var not = expression.charAt(0) == "!";
      if (not) expression = expression.slice(1);
      if (expression.charAt(0) == "(") {
        // Object detection.
        try {
          eval("r=!!" + expression);
        } catch (e) {
          // the test failed
        }
      } else {
        // Browser sniffing.
        r = new RegExp("(" + expression + ")", "i").test(userAgent);
      }
      return !!(not ^ r);
    };
  }(this)
};

new function(_) { ////////////////////  BEGIN: CLOSURE  ////////////////////

// =========================================================================
// base2/lang/header.js
// =========================================================================

var _namespace = "function base(o,a){return o.base.apply(o,a)};";
eval(_namespace);

var detect = base2.detect;

var Undefined = K(), Null = K(null), True = K(true), False = K(false);

// private
var _FORMAT = /%([1-9])/g;
var _LTRIM = /^\s\s*/;
var _RTRIM = /\s\s*$/;
var _RESCAPE = /([\/()[\]{}|*+-.,^$?\\])/g;             // safe regular expressions
var _BASE = /eval/.test(detect) ? /\bbase\s*\(/ : /.*/; // some platforms don't allow decompilation
var _HIDDEN = ["constructor", "toString", "valueOf"];   // only override these when prototyping
var _MSIE_NATIVE_FUNCTION = detect("(jscript)") ?
  new RegExp("^" + rescape(isNaN).replace(/isNaN/, "\\w+") + "$") : {test: False};

var _counter = 1;
var _slice = Array.prototype.slice;

var slice = Array.slice || function(array) {
  return _slice.apply(array, _slice.call(arguments, 1));
};

_Function_forEach(); // make sure this is initialised

// =========================================================================
// base2/Base.js
// =========================================================================

// http://dean.edwards.name/weblog/2006/03/base/

var _subclass = function(_instance, _static) {
  // Build the prototype.
  base2.__prototyping = this.prototype;
  var _prototype = new this;
  extend(_prototype, _instance);
  delete base2.__prototyping;
  
  // Create the wrapper for the constructor function.
  var _constructor = _prototype.constructor;
  function _class() {
    // Don't call the constructor function when prototyping.
    if (!base2.__prototyping) {
      if (this.constructor == arguments.callee || this.__constructing) {
        // Instantiation.
        this.__constructing = true;
        _constructor.apply(this, arguments);
        delete this.__constructing;
      } else {
        // Casting.
        return extend(arguments[0], _prototype);
      }
    }
    return this;
  };
  _prototype.constructor = _class;
  
  // Build the static interface.
  for (var i in Base) _class[i] = this[i];
  _class.ancestor = this;
  _class.base = Undefined;
  _class.init = Undefined;
  extend(_class, _static);
  _class.prototype = _prototype;
  _class.init();
  
  // introspection (removed when packed)
  ;;; _class.toString = K(String(_constructor));
  ;;; _class["#implements"] = [];
  ;;; _class["#implemented_by"] = [];
  
  return _class;
};

var Base = _subclass.call(Object, {
  constructor: function() {
    if (arguments.length > 0) {
      this.extend(arguments[0]);
    }
  },
  
  base: function() {
    // Call this method from any other method to invoke the current method's ancestor (super).
  },
  
  extend: delegate(extend)  
}, Base = {
  ancestorOf: delegate(_ancestorOf),
  
  extend: _subclass,
    
  forEach: delegate(_Function_forEach),
  
  implement: function(source) {
    if (typeof source == "function") {
      // If we are implementing another classs/module then we can use
      // casting to apply the interface.
      if (_ancestorOf(Base, source)) {
        source(this.prototype); // cast
        // introspection (removed when packed)
        ;;; this["#implements"].push(source);
        ;;; source["#implemented_by"].push(this);
      }
    } else {
      // Add the interface using the extend() function.
      extend(this.prototype, source);
    }
    return this;
  }
});

// =========================================================================
// base2/Package.js
// =========================================================================

var Package = Base.extend({
  constructor: function(_private, _public) {
    this.extend(_public);
    if (this.init) this.init();
    
    if (this.name != "base2") {
      if (!this.parent) this.parent = base2;
      this.parent.addName(this.name, this);
      this.namespace = format("var %1=%2;", this.name, String(this).slice(1, -1));
    }
    
    var LIST = /[^\s,]+/g; // pattern for comma separated list
    
    if (_private) {
      // This string should be evaluated immediately after creating a Package object.
      _private.imports = Array2.reduce(this.imports.match(LIST), function(namespace, name) {
        eval("var ns=base2." + name);
        assert(ns, format("Package not found: '%1'.", name), ReferenceError);
        return namespace += ns.namespace;
      }, _namespace + base2.namespace + JavaScript.namespace);
      
      // This string should be evaluated after you have created all of the objects
      // that are being exported.
      _private.exports = Array2.reduce(this.exports.match(LIST), function(namespace, name) {
        var fullName = this.name + "." + name;
        this.namespace += "var " + name + "=" + fullName + ";";
        return namespace += "if(!" + fullName + ")" + fullName + "=" + name + ";";
      }, "", this);
    }
  },

  exports: "",
  imports: "",
  name: "",
  namespace: "",
  parent: null,

  addName: function(name, value) {
    if (!this[name]) {
      this[name] = value;
      this.exports += "," + name;
      this.namespace += format("var %1=%2.%1;", name, this.name);
    }
  },

  addPackage: function(name) {
    this.addName(name, new Package(null, {name: name, parent: this}));
  },
  
  toString: function() {
    return format("[%1]", this.parent ? String(this.parent).slice(1, -1) + "." + this.name : this.name);
  }
});

// =========================================================================
// base2/Abstract.js
// =========================================================================

var Abstract = Base.extend({
  constructor: function() {
    throw new TypeError("Class cannot be instantiated.");
  }
});

// =========================================================================
// base2/Module.js
// =========================================================================

var Module = Abstract.extend(null, {
  extend: function(_interface, _static) {
    // Extend a module to create a new module.
    var module = this.base();
    // Inherit class methods.
    module.implement(this);
    // Implement module (instance AND static) methods.
    module.implement(_interface);
    // Implement static properties and methods.
    extend(module, _static);
    module.init();
    return module;
  },
  
  implement: function(_interface) {
    var module = this;
    if (typeof _interface == "function") {
      if (!_ancestorOf(_interface, module)) {
        this.base(_interface);
      }
      if (_ancestorOf(Module, _interface)) {
        // Implement static methods.
        forEach (_interface, function(property, name) {
          if (!module[name]) {
            if (typeof property == "function" && property.call && _interface.prototype[name]) {
              property = function() { // Late binding.
                return _interface[name].apply(_interface, arguments);
              };
            }
            module[name] = property;
          }
        });
      }
    } else {
      // Add static interface.
      extend(module, _interface);
      // Add instance interface.
      _Function_forEach (Object, _interface, function(source, name) {
        if (name.charAt(0) == "@") { // object detection
          if (detect(name.slice(1))) {
            forEach (source, arguments.callee);
          }
        } else if (typeof source == "function" && source.call) {
          module.prototype[name] = function() { // Late binding.
            var args = _slice.call(arguments);
            args.unshift(this);
            return module[name].apply(module, args);
          };
          ;;; module.prototype[name]._module = module; // introspection
        }
      });
    }
    return module;
  }
});

// =========================================================================
// base2/Enumerable.js
// =========================================================================

var Enumerable = Module.extend({
  every: function(object, test, context) {
    var result = true;
    try {
      this.forEach (object, function(value, key) {
        result = test.call(context, value, key, object);
        if (!result) throw StopIteration;
      });
    } catch (error) {
      if (error != StopIteration) throw error;
    }
    return !!result; // cast to boolean
  },
  
  filter: function(object, test, context) {
    var i = 0;
    return this.reduce(object, function(result, value, key) {
      if (test.call(context, value, key, object)) {
        result[i++] = value;
      }
      return result;
    }, []);
  },
  
  invoke: function(object, method) {
    // Apply a method to each item in the enumerated object.
    var args = _slice.call(arguments, 2);
    return this.map(object, (typeof method == "function") ? function(item) {
      return (item == null) ? undefined : method.apply(item, args);
    } : function(item) {
      return (item == null) ? undefined : item[method].apply(item, args);
    });
  },
  
  map: function(object, block, context) {
    var result = [], i = 0;
    this.forEach (object, function(value, key) {
      result[i++] = block.call(context, value, key, object);
    });
    return result;
  },
  
  pluck: function(object, key) {
    return this.map(object, function(item) {
      return (item == null) ? undefined : item[key];
    });
  },
  
  reduce: function(object, block, result, context) {
    var initialised = arguments.length > 2;
    this.forEach (object, function(value, key) {
      if (initialised) { 
        result = block.call(context, result, value, key, object);
      } else { 
        result = value;
        initialised = true;
      }
    });
    return result;
  },
  
  some: function(object, test, context) {
    return !this.every(object, not(test), context);
  }
}, {
  forEach: forEach
});


// =========================================================================
// base2/Map.js
// =========================================================================

// http://wiki.ecmascript.org/doku.php?id=proposals:dictionary

var _HASH = "#";

var Map = Base.extend({
  constructor: function(values) {
    this.merge(values);
  },

  copy: delegate(copy),

  forEach: function(block, context) {
    for (var key in this) if (key.charAt(0) == _HASH) {
      block.call(context, this[key], key.slice(1), this);
    }
  },

  get: function(key) {
    return this[_HASH + key];
  },

  getKeys: function() {
    return this.map(flip(I));
  },

  getValues: function() {
    return this.map(I);
  },

  // Ancient browsers throw an error when we use "in" as an operator.
  has: function(key) {
  /*@cc_on @*/
  /*@if (@_jscript_version < 5.5)
    return $Legacy.has(this, _HASH + key);
  @else @*/
    return _HASH + key in this;
  /*@end @*/
  },

  merge: function(values) {
    var put = flip(this.put);
    forEach (arguments, function(values) {
      forEach (values, put, this);
    }, this);
    return this;
  },

  remove: function(key) {
    delete this[_HASH + key];
  },

  put: function(key, value) {
    if (arguments.length == 1) value = key;
    // create the new entry (or overwrite the old entry).
    this[_HASH + key] = value;
  },

  size: function() {
    // this is expensive because we are not storing the keys
    var size = 0;
    for (var key in this) if (key.charAt(0) == _HASH) size++;
    return size;
  },

  union: function(values) {
    return this.merge.apply(this.copy(), arguments);
  }
});

Map.implement(Enumerable);

// =========================================================================
// base2/Collection.js
// =========================================================================

// A Map that is more array-like (accessible by index).

// Collection classes have a special (optional) property: Item
// The Item property points to a constructor function.
// Members of the collection must be an instance of Item.

// The static create() method is responsible for all construction of collection items.
// Instance methods that add new items (add, put, insertAt, putAt) pass *all* of their arguments
// to the static create() method. If you want to modify the way collection items are 
// created then you only need to override this method for custom collections.

var _KEYS = "~";

var Collection = Map.extend({
  constructor: function(values) {
    this[_KEYS] = new Array2;
    this.base(values);
  },
  
  add: function(key, item) {
    // Duplicates not allowed using add().
    // But you can still overwrite entries using put().
    assert(!this.has(key), "Duplicate key '" + key + "'.");
    this.put.apply(this, arguments);
  },

  copy: function() {
    var copy = this.base();
    copy[_KEYS] = this[_KEYS].copy();
    return copy;
  },

  forEach: function(block, context) { // optimised (refers to _HASH)
    var keys = this[_KEYS];
    var length = keys.length;
    for (var i = 0; i < length; i++) {
      block.call(context, this[_HASH + keys[i]], keys[i], this);
    }
  },

  getAt: function(index) {
    if (index < 0) index += this[_KEYS].length; // starting from the end
    var key = this[_KEYS][index];
    return (key === undefined)  ? undefined : this[_HASH + key];
  },

  getKeys: function() {
    return this[_KEYS].concat();
  },

  indexOf: function(key) {
    return this[_KEYS].indexOf(String(key));
  },

  insertAt: function(index, key, item) {
    assert(Math.abs(index) < this[_KEYS].length, "Index out of bounds.");
    assert(!this.has(key), "Duplicate key '" + key + "'.");
    this[_KEYS].insertAt(index, String(key));
    this[_HASH + key] == null; // placeholder
    this.put.apply(this, _slice.call(arguments, 1));
  },
  
  item: function(keyOrIndex) {
    return this[typeof keyOrIndex == "number" ? "getAt" : "get"](keyOrIndex);
  },

  put: function(key, item) {
    if (arguments.length == 1) item = key;
    if (!this.has(key)) {
      this[_KEYS].push(String(key));
    }
    var klass = this.constructor;
    if (klass.Item && !instanceOf(item, klass.Item)) {
      item = klass.create.apply(klass, arguments);
    }
    this[_HASH + key] = item;
  },

  putAt: function(index, item) {
    assert(Math.abs(index) < this[_KEYS].length, "Index out of bounds.");
    arguments[0] = this[_KEYS].item(index);
    this.put.apply(this, arguments);
  },

  remove: function(key) {
    // The remove() method of the Array object can be slow so check if the key exists first.
    if (this.has(key)) {
      this[_KEYS].remove(String(key));
      delete this[_HASH + key];
    }
  },

  removeAt: function(index) {
    var key = this[_KEYS].removeAt(index);
    delete this[_HASH + key];
  },

  reverse: function() {
    this[_KEYS].reverse();
    return this;
  },

  size: function() {
    return this[_KEYS].length;
  },

  sort: function(compare) { // optimised (refers to _HASH)
    if (compare) {
      var self = this;
      this[_KEYS].sort(function(key1, key2) {
        return compare(self[_HASH + key1], self[_HASH + key2], key1, key2);
      });
    } else this[_KEYS].sort();
    return this;
  },

  toString: function() {
    return String(this[_KEYS]);
  }
}, {
  Item: null, // If specified, all members of the collection must be instances of Item.
  
  create: function(key, item) {
    return this.Item ? new this.Item(key, item) : item;
  },
  
  extend: function(_instance, _static) {
    var klass = this.base(_instance);
    klass.create = this.create;
    extend(klass, _static);
    if (!klass.Item) {
      klass.Item = this.Item;
    } else if (typeof klass.Item != "function") {
      klass.Item = (this.Item || Base).extend(klass.Item);
    }
    klass.init();
    return klass;
  }
});

// =========================================================================
// base2/RegGrp.js
// =========================================================================

// A collection of regular expressions and their associated replacement values.
// A Base class for creating parsers.

var _RG_BACK_REF        = /\\(\d+)/g,
    _RG_ESCAPE_CHARS    = /\\./g,
    _RG_ESCAPE_BRACKETS = /\(\?[:=!]|\[[^\]]+\]/g,
    _RG_BRACKETS        = /\(/g,
    _RG_LOOKUP          = /\$(\d+)/,
    _RG_LOOKUP_SIMPLE   = /^\$\d+$/;

var RegGrp = Collection.extend({
  constructor: function(values, flags) {
    this.base(values);
    if (typeof flags == "string") {
      this.global = /g/.test(flags);
      this.ignoreCase = /i/.test(flags);
    }
  },

  global: true, // global is the default setting
  ignoreCase: false,

  exec: function(string, replacement) { // optimised (refers to _HASH/_KEYS)
    var flags = (this.global ? "g" : "") + (this.ignoreCase ? "i" : "");
    string = String(string) + ""; // type-safe
    if (arguments.length == 1) {
      var self = this;
      var keys = this[_KEYS];
      replacement = function(match) {
        if (match) {
          var item, offset = 1, i = 0;
          // Loop through the RegGrp items.
          while ((item = self[_HASH + keys[i++]])) {
            var next = offset + item.length + 1;
            if (arguments[offset]) { // do we have a result?
              var replacement = item.replacement;
              switch (typeof replacement) {
                case "function":
                  return replacement.apply(self, _slice.call(arguments, offset, next));
                case "number":
                  return arguments[offset + replacement];
                default:
                  return replacement;
              }
            }
            offset = next;
          }
        }
        return "";
      };
    }
    return string.replace(new RegExp(this, flags), replacement);
  },

  insertAt: function(index, expression, replacement) {
    if (instanceOf(expression, RegExp)) {
      arguments[1] = expression.source;
    }
    return base(this, arguments);
  },

  test: function(string) {
    return this.exec(string) != string;
  },
  
  toString: function() {
    var length = 0;
    return "(" + this.map(function(item) {
      // Fix back references.
      var ref = String(item).replace(_RG_BACK_REF, function(match, index) {
        return "\\" + (1 + Number(index) + length);
      });
      length += item.length + 1;
      return ref;
    }).join(")|(") + ")";
  }
}, {
  IGNORE: "$0",
  
  init: function() {
    forEach ("add,get,has,put,remove".split(","), function(name) {
      _override(this, name, function(expression) {
        if (instanceOf(expression, RegExp)) {
          arguments[0] = expression.source;
        }
        return base(this, arguments);
      });
    }, this.prototype);
  },
  
  Item: {
    constructor: function(expression, replacement) {
      if (typeof replacement == "number") replacement = String(replacement);
      else if (replacement == null) replacement = "";    
      
      // does the pattern use sub-expressions?
      if (typeof replacement == "string" && _RG_LOOKUP.test(replacement)) {
        // a simple lookup? (e.g. "$2")
        if (_RG_LOOKUP_SIMPLE.test(replacement)) {
          // store the index (used for fast retrieval of matched strings)
          replacement = parseInt(replacement.slice(1));
        } else { // a complicated lookup (e.g. "Hello $2 $1")
          // build a function to do the lookup
          var Q = /'/.test(replacement.replace(/\\./g, "")) ? '"' : "'";
          replacement = replacement.replace(/\n/g, "\\n").replace(/\r/g, "\\r").replace(/\$(\d+)/g, Q +
            "+(arguments[$1]||" + Q+Q + ")+" + Q);
          replacement = new Function("return " + Q + replacement.replace(/(['"])\1\+(.*)\+\1\1$/, "$1") + Q);
        }
      }
      
      this.length = RegGrp.count(expression);
      this.replacement = replacement;
      this.toString = K(String(expression));
    },
    
    length: 0,
    replacement: ""
  },
  
  count: function(expression) {
    // Count the number of sub-expressions in a RegExp/RegGrp.Item.
    expression = String(expression).replace(_RG_ESCAPE_CHARS, "").replace(_RG_ESCAPE_BRACKETS, "");
    return match(expression, _RG_BRACKETS).length;
  }
});

// =========================================================================
// JavaScript/package.js
// =========================================================================

var JavaScript = {
  name:      "JavaScript",
  version:   base2.version,
  exports:   "Array2,Date2,String2",
  namespace: "", // fixed later
  
  bind: function(host) {
    forEach (this.exports.match(/\w+/g), function(name2) {
      var name = name2.slice(0, -1);
      extend(host[name], this[name2]);
      this[name2](host[name].prototype); // cast
    }, this);
    return this;
  }
};

// =========================================================================
// JavaScript/~/Date.js
// =========================================================================

// Fix Date.get/setYear() (IE5-7)

if ((new Date).getYear() > 1900) {
  Date.prototype.getYear = function() {
    return this.getFullYear() - 1900;
  };
  Date.prototype.setYear = function(year) {
    return this.setFullYear(year + 1900);
  };
}

// =========================================================================
// JavaScript/~/Function.js
// =========================================================================

// Some browsers don't define this.

Function.prototype.prototype = {};

// =========================================================================
// JavaScript/~/String.js
// =========================================================================

// A KHTML bug.

if ("".replace(/^/, K("$$")) == "$") {
  extend(String.prototype, "replace", function(expression, replacement) {
    if (typeof replacement == "function") {
      var fn = replacement;
      replacement = function() {
        return String(fn.apply(null, arguments)).split("$").join("$$");
      };
    }
    return this.base(expression, replacement);
  });
}

// =========================================================================
// JavaScript/Array2.js
// =========================================================================

var Array2 = _createObject2(
  Array,
  Array,
  "concat,join,pop,push,reverse,shift,slice,sort,splice,unshift", // generics
  [Enumerable, {
    combine: function(keys, values) {
      // Combine two arrays to make a hash.
      if (!values) values = keys;
      return this.reduce(keys, function(hash, key, index) {
        hash[key] = values[index];
        return hash;
      }, {});
    },

    contains: function(array, item) {
      return this.indexOf(array, item) != -1;
    },

    copy: function(array) {
      var copy = _slice.call(array);
      if (!copy.swap) this(copy); // cast to Array2
      return copy;
    },

    flatten: function(array) {
      var length = 0;
      return this.reduce(array, function(result, item) {
        if (this.like(item)) {
          this.reduce(item, arguments.callee, result, this);
        } else {
          result[length++] = item;
        }
        return result;
      }, [], this);
    },
    
    forEach: _Array_forEach,
    
    indexOf: function(array, item, fromIndex) {
      var length = array.length;
      if (fromIndex == null) {
        fromIndex = 0;
      } else if (fromIndex < 0) {
        fromIndex = Math.max(0, length + fromIndex);
      }
      for (var i = fromIndex; i < length; i++) {
        if (array[i] === item) return i;
      }
      return -1;
    },
    
    insertAt: function(array, index, item) {
      this.splice(array, index, 0, item);
      return item;
    },
    
    item: function(array, index) {
      if (index < 0) index += array.length; // starting from the end
      return array[index];
    },
    
    lastIndexOf: function(array, item, fromIndex) {
      var length = array.length;
      if (fromIndex == null) {
        fromIndex = length - 1;
      } else if (fromIndex < 0) {
        fromIndex = Math.max(0, length + fromIndex);
      }
      for (var i = fromIndex; i >= 0; i--) {
        if (array[i] === item) return i;
      }
      return -1;
    },
  
    map: function(array, block, context) {
      var result = [];
      this.forEach (array, function(item, index) {
        result[index] = block.call(context, item, index, array);
      });
      return result;
    },
    
    remove: function(array, item) {
      var index = this.indexOf(array, item);
      if (index != -1) this.removeAt(array, index);
      return item;
    },

    removeAt: function(array, index) {
      return this.splice(array, index, 1);
    },

    swap: function(array, index1, index2) {
      if (index1 < 0) index1 += array.length; // starting from the end
      if (index2 < 0) index2 += array.length;
      var temp = array[index1];
      array[index1] = array[index2];
      array[index2] = temp;
      return array;
    }
  }]
);

Array2.reduce = Enumerable.reduce; // Mozilla does not implement the thisObj argument

Array2.like = function(object) {
  // is the object like an array?
  return !!(object && typeof object == "object" && typeof object.length == "number");
};

// introspection (removed when packed)
;;; Enumerable["#implemented_by"].pop();
;;; Enumerable["#implemented_by"].push(Array2);

// =========================================================================
// JavaScript/Date2.js
// =========================================================================

// http://developer.mozilla.org/es4/proposals/date_and_time.html

// big, ugly, regular expression
var _DATE_PATTERN = /^((-\d+|\d{4,})(-(\d{2})(-(\d{2}))?)?)?T((\d{2})(:(\d{2})(:(\d{2})(\.(\d{1,3})(\d)?\d*)?)?)?)?(([+-])(\d{2})(:(\d{2}))?|Z)?$/;  
var _DATE_PARTS = { // indexes to the sub-expressions of the RegExp above
  FullYear: 2,
  Month: 4,
  Date: 6,
  Hours: 8,
  Minutes: 10,
  Seconds: 12,
  Milliseconds: 14
};
var _TIMEZONE_PARTS = { // idem, but without the getter/setter usage on Date object
  Hectomicroseconds: 15, // :-P
  UTC: 16,
  Sign: 17,
  Hours: 18,
  Minutes: 20
};

var _TRIM_ZEROES   = /(((00)?:0+)?:0+)?\.0+$/;
var _TRIM_TIMEZONE = /(T[0-9:.]+)$/;

var Date2 = _createObject2(
  Date, 
  function(yy, mm, dd, h, m, s, ms) {
    switch (arguments.length) {
      case 0: return new Date;
      case 1: return new Date(yy);
      default: return new Date(yy, mm, arguments.length == 2 ? 1 : dd, h || 0, m || 0, s || 0, ms || 0);
    }
  }, "", [{
    toISOString: function(date) {
      var string = "####-##-##T##:##:##.###";
      for (var part in _DATE_PARTS) {
        string = string.replace(/#+/, function(digits) {
          var value = date["getUTC" + part]();
          if (part == "Month") value++; // js month starts at zero
          return ("000" + value).slice(-digits.length); // pad
        });
      }
      // remove trailing zeroes, and remove UTC timezone, when time's absent
      return string.replace(_TRIM_ZEROES, "").replace(_TRIM_TIMEZONE, "$1Z");
    }
  }]
);

Date2.now = function() {
  return (new Date).valueOf(); // milliseconds since the epoch
};

Date2.parse = function(string, defaultDate) {
  if (arguments.length > 1) {
    assertType(defaultDate, "number", "defaultDate should be of type 'number'.");
  }
  // parse ISO date
  var match = String(string).match(_DATE_PATTERN);
  if (match) {
    if (match[_DATE_PARTS.Month]) match[_DATE_PARTS.Month]--; // js months start at zero
    // round milliseconds on 3 digits
    if (match[_TIMEZONE_PARTS.Hectomicroseconds] >= 5) match[_DATE_PARTS.Milliseconds]++;
    var date = new Date(defaultDate || 0);
    var prefix = match[_TIMEZONE_PARTS.UTC] || match[_TIMEZONE_PARTS.Hours] ? "UTC" : "";
    for (var part in _DATE_PARTS) {
      var value = match[_DATE_PARTS[part]];
      if (!value) continue; // empty value
      // set a date part
      date["set" + prefix + part](value);
      // make sure that this setting does not overflow
      if (date["get" + prefix + part]() != match[_DATE_PARTS[part]]) {
        return NaN;
      }
    }
    // timezone can be set, without time being available
    // without a timezone, local timezone is respected
    if (match[_TIMEZONE_PARTS.Hours]) {
      var Hours = Number(match[_TIMEZONE_PARTS.Sign] + match[_TIMEZONE_PARTS.Hours]);
      var Minutes = Number(match[_TIMEZONE_PARTS.Sign] + (match[_TIMEZONE_PARTS.Minutes] || 0));
      date.setUTCMinutes(date.getUTCMinutes() + (Hours * 60) + Minutes);
    } 
    return date.valueOf();
  } else {
    return Date.parse(string);
  }
};

// =========================================================================
// JavaScript/String2.js
// =========================================================================

var String2 = _createObject2(
  String, 
  function(string) {
    return new String(arguments.length == 0 ? "" : string);
  },
  "charAt,charCodeAt,concat,indexOf,lastIndexOf,match,replace,search,slice,split,substr,substring,toLowerCase,toUpperCase",
  [{trim: trim}]
);

// =========================================================================
// JavaScript/functions.js
// =========================================================================

function _createObject2(Native, constructor, generics, extensions) {
  // Clone native objects and extend them.

  // Create a Module that will contain all the new methods.
  var INative = Module.extend();
  // http://developer.mozilla.org/en/docs/New_in_JavaScript_1.6#Array_and_String_generics
  forEach (generics.match(/\w+/g), function(name) {
    INative[name] = unbind(Native.prototype[name]);
  });
  forEach (extensions, INative.implement, INative);

  // create a faux constructor that augments the native object
  var Native2 = function() {
    return INative(this.constructor == INative ? constructor.apply(null, arguments) : arguments[0]);
  };
  Native2.prototype = INative.prototype;

  // Remove methods that are already implemented.
  forEach (INative, function(method, name) {
    if (Native[name]) {
      INative[name] = Native[name];
      delete INative.prototype[name];
    }
    Native2[name] = INative[name];
  });
  Native2.ancestor = Object;
  delete Native2.extend;
  if (Native != Array) delete Native2.forEach; 

  return Native2;
};

// =========================================================================
// lang/extend.js
// =========================================================================

function extend(object, source) { // or extend(object, key, value)
  if (object && source) {
    if (arguments.length > 2) { // Extending with a key/value pair.
      var key = source;
      source = {};
      source[key] = arguments[2];
    }
    var proto = (typeof source == "function" ? Function : Object).prototype;
    // Add constructor, toString etc
    var i = _HIDDEN.length, key;
    if (base2.__prototyping) {
      while (key = _HIDDEN[--i]) {
        var value = source[key];
        if (value != proto[key]) {
          if (_BASE.test(value)) {
            _override(object, key, value);
          } else {
            object[key] = value;
          }
        }
      }
    }
    // Copy each of the source object's properties to the target object.
    for (key in source) {
      if (proto[key] === undefined) {
        var value = source[key];
        // Object detection.
        if (key.charAt(0) == "@") {
          if (detect(key.slice(1))) arguments.callee(object, value);
          continue;
        }
        // Check for method overriding.
        var ancestor = object[key];
        if (ancestor && typeof value == "function") {
          if (value != ancestor && (!ancestor.method || !_ancestorOf(value, ancestor))) {
            if (_BASE.test(value)) {
              _override(object, key, value);
            } else {
              value.ancestor = ancestor;
              object[key] = value;
            }
          }
        } else {
          object[key] = value;
        }
      }
    }
  }
  return object;
};

function _ancestorOf(ancestor, fn) {
  // Check if a function is in another function's inheritance chain.
  while (fn) {
    if (!fn.ancestor) return false;
    fn = fn.ancestor;
    if (fn == ancestor) return true;
  }
  return false;
};

function _override(object, name, method) {
  // Override an existing method.
  var ancestor = object[name];
  var superObject = base2.__prototyping; // late binding for classes
  if (superObject && ancestor != superObject[name]) superObject = null;
  function _base() {
    var previous = this.base;
    this.base = superObject ? superObject[name] : ancestor;
    var returnValue = method.apply(this, arguments);
    this.base = previous;
    return returnValue;
  };
  _base.ancestor = ancestor;
  object[name] = _base;
  // introspection (removed when packed)
  ;;; _base.toString = K(String(method));
};

// =========================================================================
// lang/forEach.js
// =========================================================================

// http://dean.edwards.name/weblog/2006/07/enum/

if (typeof StopIteration == "undefined") {
  StopIteration = new Error("StopIteration");
}

function forEach(object, block, context, fn) {
  if (object == null) return;
  if (!fn) {
    if (typeof object == "function" && object.call) {
      // Functions are a special case.
      fn = Function;
    } else if (typeof object.forEach == "function" && object.forEach != arguments.callee) {
      // The object implements a custom forEach method.
      object.forEach(block, context);
      return;
    } else if (typeof object.length == "number") {
      // The object is array-like.
      _Array_forEach(object, block, context);
      return;
    }
  }
  _Function_forEach(fn || Object, object, block, context);
};

// These are the two core enumeration methods. All other forEach methods
//  eventually call one of these two.

function _Array_forEach(array, block, context) {
  if (array == null) return;
  var length = array.length, i; // preserve length
  if (typeof array == "string") {
    for (i = 0; i < length; i++) {
      block.call(context, array.charAt(i), i, array);
    }
  } else { // Cater for sparse arrays.
    for (i = 0; i < length; i++) {    
    /*@cc_on @*/
    /*@if (@_jscript_version < 5.2)
      if ($Legacy.has(array, i))
    @else @*/
      if (i in array)
    /*@end @*/
        block.call(context, array[i], i, array);
    }
  }
};

function _Function_forEach(fn, object, block, context) {
  // http://code.google.com/p/base2/issues/detail?id=10
  
  // Run the test for Safari's buggy enumeration.
  var Temp = function(){this.i=1;};
  Temp.prototype = {i:1};
  var count = 0;
  for (var i in new Temp) count++;
  
  // Overwrite the main function the first time it is called.
  _Function_forEach = (count > 1) ? function(fn, object, block, context) {
    // Safari fix (pre version 3)
    var processed = {};
    for (var key in object) {
      if (!processed[key] && fn.prototype[key] === undefined) {
        processed[key] = true;
        block.call(context, object[key], key, object);
      }
    }
  } : function(fn, object, block, context) {
    // Enumerate an object and compare its keys with fn's prototype.
    for (var key in object) {
      if (fn.prototype[key] === undefined) {
        block.call(context, object[key], key, object);
      }
    }
  };
  
  _Function_forEach(fn, object, block, context);
};

// =========================================================================
// lang/typeOf.js
// =========================================================================

// http://wiki.ecmascript.org/doku.php?id=proposals:typeof

function typeOf(object) {
  var type = typeof object;
  switch (type) {
    case "object":
      return object === null ? "null" : typeof object.call == "function" || _MSIE_NATIVE_FUNCTION.test(object) ? "function" : type;
    case "function":
      return typeof object.call == "function" ? type : "object";
    default:
      return type;
  }
};

// =========================================================================
// lang/instanceOf.js
// =========================================================================

function instanceOf(object, klass) {
  // Handle exceptions where the target object originates from another frame.
  // This is handy for JSON parsing (amongst other things).
  
  if (typeof klass != "function") {
    throw new TypeError("Invalid 'instanceOf' operand.");
  }

  if (object == null) return false;
  
  /*@cc_on  
  // COM objects don't have a constructor
  if (typeof object.constructor != "function") {
    return typeOf(object) == typeof klass.prototype.valueOf();
  }
  @*/
  /*@if (@_jscript_version < 5.1)
    if ($Legacy.instanceOf(object, klass)) return true;
  @else @*/
    if (object instanceof klass) return true;
  /*@end @*/

  // If the class is a base2 class then it would have passed the test above.
  if (Base.ancestorOf == klass.ancestorOf) return false;
  
  // base2 objects can only be instances of Object.
  if (Base.ancestorOf == object.constructor.ancestorOf) return klass == Object;
  
  switch (klass) {
    case Array: // This is the only troublesome one.
      return !!(typeof object == "object" && object.join && object.splice);
    case Function:
      return typeOf(object) == "function";
    case RegExp:
      return typeof object.constructor.$1 == "string";
    case Date:
      return !!object.getTimezoneOffset;
    case String:
    case Number:  // These are bullet-proof.
    case Boolean:
      return typeof object == typeof klass.prototype.valueOf();
    case Object:
      return true;
  }
  
  return false;
};

// =========================================================================
// lang/assert.js
// =========================================================================

function assert(condition, message, ErrorClass) {
  if (!condition) {
    throw new (ErrorClass || Error)(message || "Assertion failed.");
  }
};

function assertArity(args, arity, message) {
  if (arity == null) arity = args.callee.length;
  if (args.length < arity) {
    throw new SyntaxError(message || "Not enough arguments.");
  }
};

function assertType(object, type, message) {
  if (type && (typeof type == "function" ? !instanceOf(object, type) : typeOf(object) != type)) {
    throw new TypeError(message || "Invalid type.");
  }
};

// =========================================================================
// lang/core.js
// =========================================================================

function assignID(object) {
  // Assign a unique ID to an object.
  if (!object.base2ID) object.base2ID = "b2_" + _counter++;
  return object.base2ID;
};

function copy(object) {
  var fn = function(){};
  fn.prototype = object;
  return new fn;
};

// String/RegExp.

function format(string) {
  // Replace %n with arguments[n].
  // e.g. format("%1 %2%3 %2a %1%3", "she", "se", "lls");
  // ==> "she sells sea shells"
  // Only %1 - %9 supported.
  var args = arguments;
  var pattern = new RegExp("%([1-" + arguments.length + "])", "g");
  return String(string).replace(pattern, function(match, index) {
    return args[index];
  });
};

function match(string, expression) {
  // Same as String.match() except that this function will return an empty 
  // array if there is no match.
  return String(string).match(expression) || [];
};

function rescape(string) {
  // Make a string safe for creating a RegExp.
  return String(string).replace(_RESCAPE, "\\$1");
};

// http://blog.stevenlevithan.com/archives/faster-trim-javascript
function trim(string) {
  return String(string).replace(_LTRIM, "").replace(_RTRIM, "");
};

// =========================================================================
// lang/functional.js
// =========================================================================

function I(i) {
    return i;
};

function K(k) {
  return function() {
    return k;
  };
};

function bind(fn, context) {
  var args = _slice.call(arguments, 2);
  return args.length == 0 ? function() {
    return fn.apply(context, arguments);
  } : function() {
    return fn.apply(context, args.concat.apply(args, arguments));
  };
};

function delegate(fn, context) {
  return function() {
    var args = _slice.call(arguments);
    args.unshift(this);
    return fn.apply(context, args);
  };
};

function flip(fn) {
  return function() {
    return fn.apply(this, Array2.swap(arguments, 0, 1));
  };
};

function not(fn) {
  return function() {
    return !fn.apply(this, arguments);
  };
};

function unbind(fn) {
  return function(context) {
    return fn.apply(context, _slice.call(arguments, 1));
  };
};

// =========================================================================
// base2/init.js
// =========================================================================

base2 = new Package(this, base2);
eval(this.exports);

base2.extend = extend;

// the enumerable methods are extremely useful so we'll add them to the base2
//  namespace for convenience
forEach (Enumerable, function(method, name) {
  if (!Module[name]) base2.addName(name, bind(method, Enumerable));
});

JavaScript = new Package(this, JavaScript);
eval(this.exports);

}; ////////////////////  END: CLOSURE  /////////////////////////////////////
