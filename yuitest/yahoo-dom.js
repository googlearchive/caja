/**
 * The YAHOO object is the single global object used by YUI Library.  It
 * contains utility function for setting up namespaces, inheritance, and
 * logging.  YAHOO.util, YAHOO.widget, and YAHOO.example are namespaces
 * created automatically for and used by the library.
 * @module yahoo
 * @title  YAHOO Global
 */

/**
 * YAHOO_config is not included as part of the library.  Instead it is an 
 * object that can be defined by the implementer immediately before 
 * including the YUI library.  The properties included in this object
 * will be used to configure global properties needed as soon as the 
 * library begins to load.
 * @class YAHOO_config
 * @static
 */

/**
 * A reference to a function that will be executed every time a YAHOO module
 * is loaded.  As parameter, this function will receive the version
 * information for the module. See <a href="YAHOO.env.html#getVersion">
 * YAHOO.env.getVersion</a> for the description of the version data structure.
 * @property listener
 * @type Function
 * @static
 * @default undefined
 */

/**
 * Set to true if the library will be dynamically loaded after window.onload.
 * Defaults to false 
 * @property injecting
 * @type boolean
 * @static
 * @default undefined
 */

/**
 * Instructs the yuiloader component to dynamically load yui components and
 * their dependencies.  See the yuiloader documentation for more information
 * about dynamic loading
 * @property load
 * @static
 * @default undefined
 * @see yuiloader
 */

/**
 * Forces the use of the supplied locale where applicable in the library
 * @property locale
 * @type string
 * @static
 * @default undefined
 */

if (typeof YAHOO == "undefined" || !YAHOO) {
    /**
     * The YAHOO global namespace object.  If YAHOO is already defined, the
     * existing YAHOO object will not be overwritten so that defined
     * namespaces are preserved.
     * @class YAHOO
     * @static
     */
    var YAHOO = {};
}

/**
 * Returns the namespace specified and creates it if it doesn't exist
 * <pre>
 * YAHOO.namespace("property.package");
 * YAHOO.namespace("YAHOO.property.package");
 * </pre>
 * Either of the above would create YAHOO.property, then
 * YAHOO.property.package
 *
 * Be careful when naming packages. Reserved words may work in some browsers
 * and not others. For instance, the following will fail in Safari:
 * <pre>
 * YAHOO.namespace("really.long.nested.namespace");
 * </pre>
 * This fails because "long" is a future reserved word in ECMAScript
 *
 * For implementation code that uses YUI, do not create your components
 * in the namespaces created by the library.  defined by YUI -- create 
 * your own (YAHOO.util, YAHOO.widget, YAHOO.lang, YAHOO.env)
 *
 * @method namespace
 * @static
 * @param  {String*} arguments 1-n namespaces to create 
 * @return {Object}  A reference to the last namespace object created
 */
YAHOO.namespace = function() {
    var a=arguments, o=null, i, j, d;
    for (i=0; i<a.length; i=i+1) {
        d=(""+a[i]).split(".");
        o=YAHOO;

        // YAHOO is implied, so it is ignored if it is included
        for (j=(d[0] == "YAHOO") ? 1 : 0; j<d.length; j=j+1) {
            o[d[j]]=o[d[j]] || {};
            o=o[d[j]];
        }
    }

    return o;
};

/**
 * Uses YAHOO.widget.Logger to output a log message, if the widget is
 * available.
 *
 * @method log
 * @static
 * @param  {String}  msg  The message to log.
 * @param  {String}  cat  The log category for the message.  Default
 *                        categories are "info", "warn", "error", time".
 *                        Custom categories can be used as well. (opt)
 * @param  {String}  src  The source of the the message (opt)
 * @return {Boolean}      True if the log operation was successful.
 */
YAHOO.log = function(msg, cat, src) {
    var l=YAHOO.widget.Logger;
    if(l && l.log) {
        return l.log(msg, cat, src);
    } else {
        return false;
    }
};

/**
 * Registers a module with the YAHOO object
 * @method register
 * @static
 * @param {String}   name    the name of the module (event, slider, etc)
 * @param {Function} mainClass a reference to class in the module.  This
 *                             class will be tagged with the version info
 *                             so that it will be possible to identify the
 *                             version that is in use when multiple versions
 *                             have loaded
 * @param {Object}   data      metadata object for the module.  Currently it
 *                             is expected to contain a "version" property
 *                             and a "build" property at minimum.
 */
YAHOO.register = function(name, mainClass, data) {
    var mods = YAHOO.env.modules, m, v, b, ls, i;

    if (!mods[name]) {
        mods[name] = { 
            versions:[], 
            builds:[] 
        };
    }

    m  = mods[name];
    v  = data.version;
    b  = data.build;
    ls = YAHOO.env.listeners;

    m.name = name;
    m.version = v;
    m.build = b;
    m.versions.push(v);
    m.builds.push(b);
    m.mainClass = mainClass;

    // fire the module load listeners
    for (i=0;i<ls.length;i=i+1) {
        ls[i](m);
    }
    // label the main class
    if (mainClass) {
        mainClass.VERSION = v;
        mainClass.BUILD = b;
    } else {
        YAHOO.log("mainClass is undefined for module " + name, "warn");
    }
};

/**
 * YAHOO.env is used to keep track of what is known about the YUI library and
 * the browsing environment
 * @class YAHOO.env
 * @static
 */
YAHOO.env = YAHOO.env || {

    /**
     * Keeps the version info for all YUI modules that have reported themselves
     * @property modules
     * @type Object[]
     */
    modules: [],
    
    /**
     * List of functions that should be executed every time a YUI module
     * reports itself.
     * @property listeners
     * @type Function[]
     */
    listeners: []
};

/**
 * Returns the version data for the specified module:
 *      <dl>
 *      <dt>name:</dt>      <dd>The name of the module</dd>
 *      <dt>version:</dt>   <dd>The version in use</dd>
 *      <dt>build:</dt>     <dd>The build number in use</dd>
 *      <dt>versions:</dt>  <dd>All versions that were registered</dd>
 *      <dt>builds:</dt>    <dd>All builds that were registered.</dd>
 *      <dt>mainClass:</dt> <dd>An object that was was stamped with the
 *                 current version and build. If 
 *                 mainClass.VERSION != version or mainClass.BUILD != build,
 *                 multiple versions of pieces of the library have been
 *                 loaded, potentially causing issues.</dd>
 *       </dl>
 *
 * @method getVersion
 * @static
 * @param {String}  name the name of the module (event, slider, etc)
 * @return {Object} The version info
 */
YAHOO.env.getVersion = function(name) {
    return YAHOO.env.modules[name] || null;
};

/**
 * Do not fork for a browser if it can be avoided.  Use feature detection when
 * you can.  Use the user agent as a last resort.  YAHOO.env.ua stores a version
 * number for the browser engine, 0 otherwise.  This value may or may not map
 * to the version number of the browser using the engine.  The value is 
 * presented as a float so that it can easily be used for boolean evaluation 
 * as well as for looking for a particular range of versions.  Because of this, 
 * some of the granularity of the version info may be lost (e.g., Gecko 1.8.0.9 
 * reports 1.8).
 * @class YAHOO.env.ua
 * @static
 */
YAHOO.env.ua = function() {
    var o={

        /**
         * Internet Explorer version number or 0.  Example: 6
         * @property ie
         * @type float
         */
        ie:0,

        /**
         * Opera version number or 0.  Example: 9.2
         * @property opera
         * @type float
         */
        opera:0,

        /**
         * Gecko engine revision number.  Will evaluate to 1 if Gecko 
         * is detected but the revision could not be found. Other browsers
         * will be 0.  Example: 1.8
         * <pre>
         * Firefox 1.0.0.4: 1.7.8   <-- Reports 1.7
         * Firefox 1.5.0.9: 1.8.0.9 <-- Reports 1.8
         * Firefox 2.0.0.3: 1.8.1.3 <-- Reports 1.8
         * Firefox 3 alpha: 1.9a4   <-- Reports 1.9
         * </pre>
         * @property gecko
         * @type float
         */
        gecko:0,

        /**
         * AppleWebKit version.  KHTML browsers that are not WebKit browsers 
         * will evaluate to 1, other browsers 0.  Example: 418.9.1
         * <pre>
         * Safari 1.3.2 (312.6): 312.8.1 <-- Reports 312.8 -- currently the 
         *                                   latest available for Mac OSX 10.3.
         * Safari 2.0.2:         416     <-- hasOwnProperty introduced
         * Safari 2.0.4:         418     <-- preventDefault fixed
         * Safari 2.0.4 (419.3): 418.9.1 <-- One version of Safari may run
         *                                   different versions of webkit
         * Safari 2.0.4 (419.3): 419     <-- Tiger installations that have been
         *                                   updated, but not updated
         *                                   to the latest patch.
         * Webkit 212 nightly:   522+    <-- Safari 3.0 precursor (with native SVG
         *                                   and many major issues fixed).  
         * 3.x yahoo.com, flickr:422     <-- Safari 3.x hacks the user agent
         *                                   string when hitting yahoo.com and 
         *                                   flickr.com.
         * Safari 3.0.4 (523.12):523.12  <-- First Tiger release - automatic update
         *                                   from 2.x via the 10.4.11 OS patch
         * Webkit nightly 1/2008:525+    <-- Supports DOMContentLoaded event.
         *                                   yahoo.com user agent hack removed.
         *                                   
         * </pre>
         * http://developer.apple.com/internet/safari/uamatrix.html
         * @property webkit
         * @type float
         */
        webkit: 0,

        /**
         * The mobile property will be set to a string containing any relevant
         * user agent information when a modern mobile browser is detected.
         * Currently limited to Safari on the iPhone/iPod Touch, Nokia N-series
         * devices with the WebKit-based browser, and Opera Mini.  
         * @property mobile 
         * @type string
         */
        mobile: null,

        /**
         * Adobe AIR version number or 0.  Only populated if webkit is detected.
         * Example: 1.0
         * @property air
         * @type float
         */
        air: 0,

        /**
         * Google Caja version number or 0.
         * @property caja
         * @type float
         */
        caja: 0

    },

    ua = navigator.userAgent, 
    
    m;

    // Modern KHTML browsers should qualify as Safari X-Grade
    if ((/KHTML/).test(ua)) {
        o.webkit=1;
    }
    // Modern WebKit browsers are at least X-Grade
    m=ua.match(/AppleWebKit\/([^\s]*)/);
    if (m&&m[1]) {
        o.webkit=parseFloat(m[1]);

        // Mobile browser check
        if (/ Mobile\//.test(ua)) {
            o.mobile = "Apple"; // iPhone or iPod Touch
        } else {
            m=ua.match(/NokiaN[^\/]*/);
            if (m) {
                o.mobile = m[0]; // Nokia N-series, ex: NokiaN95
            }
        }

        m=ua.match(/AdobeAIR\/([^\s]*)/);
        if (m) {
            o.air = m[0]; // Adobe AIR 1.0 or better
        }

    }

    if (!o.webkit) { // not webkit
        // @todo check Opera/8.01 (J2ME/MIDP; Opera Mini/2.0.4509/1316; fi; U; ssr)
        m=ua.match(/Opera[\s\/]([^\s]*)/);
        if (m&&m[1]) {
            o.opera=parseFloat(m[1]);
            m=ua.match(/Opera Mini[^;]*/);
            if (m) {
                o.mobile = m[0]; // ex: Opera Mini/2.0.4509/1316
            }
        } else { // not opera or webkit
            m=ua.match(/MSIE\s([^;]*)/);
            if (m&&m[1]) {
                o.ie=parseFloat(m[1]);
            } else { // not opera, webkit, or ie
                m=ua.match(/Gecko\/([^\s]*)/);
                if (m) {
                    o.gecko=1; // Gecko detected, look for revision
                    m=ua.match(/rv:([^\s\)]*)/);
                    if (m&&m[1]) {
                        o.gecko=parseFloat(m[1]);
                    }
                }
            }
        }
    }

    m=ua.match(/Caja\/([^\s]*)/);
    if (m&&m[1]) {
        o.caja=parseFloat(m[1]);
    }
    
    return o;
}();

/*
 * Initializes the global by creating the default namespaces and applying
 * any new configuration information that is detected.  This is the setup
 * for env.
 * @method init
 * @static
 * @private
 */
(function() {
    YAHOO.namespace("util", "widget", "example");
    /*global YAHOO_config*/
    if ("undefined" !== typeof YAHOO_config) {
        var l=YAHOO_config.listener,ls=YAHOO.env.listeners,unique=true,i;
        if (l) {
            // if YAHOO is loaded multiple times we need to check to see if
            // this is a new config object.  If it is, add the new component
            // load listener to the stack
            for (i=0;i<ls.length;i=i+1) {
                if (ls[i]==l) {
                    unique=false;
                    break;
                }
            }
            if (unique) {
                ls.push(l);
            }
        }
    }
})();
/**
 * Provides the language utilites and extensions used by the library
 * @class YAHOO.lang
 */
YAHOO.lang = YAHOO.lang || {};

(function() {


var L = YAHOO.lang,

    ARRAY_TOSTRING = '[object Array]',
    FUNCTION_TOSTRING = '[object Function]',
    OP = Object.prototype,

    // ADD = ["toString", "valueOf", "hasOwnProperty"],
    ADD = ["toString", "valueOf"],

    OB = {

    /**
     * Determines wheather or not the provided object is an array.
     * @method isArray
     * @param {any} o The object being testing
     * @return {boolean} the result
     */
    isArray: function(o) { 
        return OP.toString.apply(o) === ARRAY_TOSTRING;
    },

    /**
     * Determines whether or not the provided object is a boolean
     * @method isBoolean
     * @param {any} o The object being testing
     * @return {boolean} the result
     */
    isBoolean: function(o) {
        return typeof o === 'boolean';
    },
    
    /**
     * Determines whether or not the provided object is a function.
     * Note: Internet Explorer thinks certain functions are objects:
     *
     * var obj = document.createElement("object");
     * YAHOO.lang.isFunction(obj.getAttribute) // reports false in IE
     *
     * var input = document.createElement("input"); // append to body
     * YAHOO.lang.isFunction(input.focus) // reports false in IE
     *
     * You will have to implement additional tests if these functions
     * matter to you.
     *
     * @method isFunction
     * @param {any} o The object being testing
     * @return {boolean} the result
     */
    isFunction: function(o) {
        return OP.toString.apply(o) === FUNCTION_TOSTRING;
    },
        
    /**
     * Determines whether or not the provided object is null
     * @method isNull
     * @param {any} o The object being testing
     * @return {boolean} the result
     */
    isNull: function(o) {
        return o === null;
    },
        
    /**
     * Determines whether or not the provided object is a legal number
     * @method isNumber
     * @param {any} o The object being testing
     * @return {boolean} the result
     */
    isNumber: function(o) {
        return typeof o === 'number' && isFinite(o);
    },
      
    /**
     * Determines whether or not the provided object is of type object
     * or function
     * @method isObject
     * @param {any} o The object being testing
     * @return {boolean} the result
     */  
    isObject: function(o) {
return (o && (typeof o === 'object' || L.isFunction(o))) || false;
    },
        
    /**
     * Determines whether or not the provided object is a string
     * @method isString
     * @param {any} o The object being testing
     * @return {boolean} the result
     */
    isString: function(o) {
        return typeof o === 'string';
    },
        
    /**
     * Determines whether or not the provided object is undefined
     * @method isUndefined
     * @param {any} o The object being testing
     * @return {boolean} the result
     */
    isUndefined: function(o) {
        return typeof o === 'undefined';
    },
    
 
    /**
     * IE will not enumerate native functions in a derived object even if the
     * function was overridden.  This is a workaround for specific functions 
     * we care about on the Object prototype. 
     * @property _IEEnumFix
     * @param {Function} r  the object to receive the augmentation
     * @param {Function} s  the object that supplies the properties to augment
     * @static
     * @private
     */
    _IEEnumFix: (YAHOO.env.ua.ie) ? function(r, s) {
            var i, fname, f;
            for (i=0;i<ADD.length;i=i+1) {

                fname = ADD[i];
                f = s[fname];

                if (L.isFunction(f) && f!=OP[fname]) {
                    r[fname]=f;
                }
            }
    } : function(){},
       
    /**
     * Utility to set up the prototype, constructor and superclass properties to
     * support an inheritance strategy that can chain constructors and methods.
     * Static members will not be inherited.
     *
     * @method extend
     * @static
     * @param {Function} subc   the object to modify
     * @param {Function} superc the object to inherit
     * @param {Object} overrides  additional properties/methods to add to the
     *                              subclass prototype.  These will override the
     *                              matching items obtained from the superclass 
     *                              if present.
     */
    extend: function(subc, superc, overrides) {
        if (!superc||!subc) {
            throw new Error("extend failed, please check that " +
                            "all dependencies are included.");
        }
        var F = function() {}, i;
        F.prototype=superc.prototype;
        subc.prototype=new F();
        subc.prototype.constructor=subc;
        subc.superclass=superc.prototype;
        if (superc.prototype.constructor == OP.constructor) {
            superc.prototype.constructor=superc;
        }
    
        if (overrides) {
            for (i in overrides) {
                if (L.hasOwnProperty(overrides, i)) {
                    subc.prototype[i]=overrides[i];
                }
            }

            L._IEEnumFix(subc.prototype, overrides);
        }
    },
   
    /**
     * Applies all properties in the supplier to the receiver if the
     * receiver does not have these properties yet.  Optionally, one or 
     * more methods/properties can be specified (as additional 
     * parameters).  This option will overwrite the property if receiver 
     * has it already.  If true is passed as the third parameter, all 
     * properties will be applied and _will_ overwrite properties in 
     * the receiver.
     *
     * @method augmentObject
     * @static
     * @since 2.3.0
     * @param {Function} r  the object to receive the augmentation
     * @param {Function} s  the object that supplies the properties to augment
     * @param {String*|boolean}  arguments zero or more properties methods 
     *        to augment the receiver with.  If none specified, everything
     *        in the supplier will be used unless it would
     *        overwrite an existing property in the receiver. If true
     *        is specified as the third parameter, all properties will
     *        be applied and will overwrite an existing property in
     *        the receiver
     */
    augmentObject: function(r, s) {
        if (!s||!r) {
            throw new Error("Absorb failed, verify dependencies.");
        }
        var a=arguments, i, p, overrideList=a[2];
        if (overrideList && overrideList!==true) { // only absorb the specified properties
            for (i=2; i<a.length; i=i+1) {
                r[a[i]] = s[a[i]];
            }
        } else { // take everything, overwriting only if the third parameter is true
            for (p in s) { 
                if (overrideList || !(p in r)) {
                    r[p] = s[p];
                }
            }
            
            L._IEEnumFix(r, s);
        }
    },
 
    /**
     * Same as YAHOO.lang.augmentObject, except it only applies prototype properties
     * @see YAHOO.lang.augmentObject
     * @method augmentProto
     * @static
     * @param {Function} r  the object to receive the augmentation
     * @param {Function} s  the object that supplies the properties to augment
     * @param {String*|boolean}  arguments zero or more properties methods 
     *        to augment the receiver with.  If none specified, everything 
     *        in the supplier will be used unless it would overwrite an existing 
     *        property in the receiver.  if true is specified as the third 
     *        parameter, all properties will be applied and will overwrite an 
     *        existing property in the receiver
     */
    augmentProto: function(r, s) {
        if (!s||!r) {
            throw new Error("Augment failed, verify dependencies.");
        }
        //var a=[].concat(arguments);
        var a=[r.prototype,s.prototype], i;
        for (i=2;i<arguments.length;i=i+1) {
            a.push(arguments[i]);
        }
        L.augmentObject.apply(this, a);
    },

      
    /**
     * Returns a simple string representation of the object or array.
     * Other types of objects will be returned unprocessed.  Arrays
     * are expected to be indexed.  Use object notation for
     * associative arrays.
     * @method dump
     * @since 2.3.0
     * @param o {Object} The object to dump
     * @param d {int} How deep to recurse child objects, default 3
     * @return {String} the dump result
     */
    dump: function(o, d) {
        var i,len,s=[],OBJ="{...}",FUN="f(){...}",
            COMMA=', ', ARROW=' => ';

        // Cast non-objects to string
        // Skip dates because the std toString is what we want
        // Skip HTMLElement-like objects because trying to dump 
        // an element will cause an unhandled exception in FF 2.x
        if (!L.isObject(o)) {
            return o + "";
        } else if (o instanceof Date || ("nodeType" in o && "tagName" in o)) {
            return o;
        } else if  (L.isFunction(o)) {
            return FUN;
        }

        // dig into child objects the depth specifed. Default 3
        d = (L.isNumber(d)) ? d : 3;

        // arrays [1, 2, 3]
        if (L.isArray(o)) {
            s.push("[");
            for (i=0,len=o.length;i<len;i=i+1) {
                if (L.isObject(o[i])) {
                    s.push((d > 0) ? L.dump(o[i], d-1) : OBJ);
                } else {
                    s.push(o[i]);
                }
                s.push(COMMA);
            }
            if (s.length > 1) {
                s.pop();
            }
            s.push("]");
        // objects {k1 => v1, k2 => v2}
        } else {
            s.push("{");
            for (i in o) {
                if (L.hasOwnProperty(o, i)) {
                    s.push(i + ARROW);
                    if (L.isObject(o[i])) {
                        s.push((d > 0) ? L.dump(o[i], d-1) : OBJ);
                    } else {
                        s.push(o[i]);
                    }
                    s.push(COMMA);
                }
            }
            if (s.length > 1) {
                s.pop();
            }
            s.push("}");
        }

        return s.join("");
    },

    /**
     * Does variable substitution on a string. It scans through the string 
     * looking for expressions enclosed in { } braces. If an expression 
     * is found, it is used a key on the object.  If there is a space in
     * the key, the first word is used for the key and the rest is provided
     * to an optional function to be used to programatically determine the
     * value (the extra information might be used for this decision). If 
     * the value for the key in the object, or what is returned from the
     * function has a string value, number value, or object value, it is 
     * substituted for the bracket expression and it repeats.  If this
     * value is an object, it uses the Object's toString() if this has
     * been overridden, otherwise it does a shallow dump of the key/value
     * pairs.
     * @method substitute
     * @since 2.3.0
     * @param s {String} The string that will be modified.
     * @param o {Object} An object containing the replacement values
     * @param f {Function} An optional function that can be used to
     *                     process each match.  It receives the key,
     *                     value, and any extra metadata included with
     *                     the key inside of the braces.
     * @return {String} the substituted string
     */
    substitute: function (s, o, f) {
        var i, j, k, key, v, meta, saved=[], token, 
            DUMP='dump', SPACE=' ', LBRACE='{', RBRACE='}',
            dump;


        for (;;) {
            i = s.lastIndexOf(LBRACE);
            if (i < 0) {
                break;
            }
            j = s.indexOf(RBRACE, i);
            if (i + 1 >= j) {
                break;
            }

            //Extract key and meta info 
            token = s.substring(i + 1, j);
            key = token;
            meta = null;
            k = key.indexOf(SPACE);
            if (k > -1) {
                meta = key.substring(k + 1);
                key = key.substring(0, k);
            }

            // lookup the value
            v = o[key];

            // if a substitution function was provided, execute it
            if (f) {
                v = f(key, v, meta);
            }

            if (L.isObject(v)) {
                if (L.isArray(v)) {
                    v = L.dump(v, parseInt(meta, 10));
                } else {
                    meta = meta || "";

                    // look for the keyword 'dump', if found force obj dump
                    dump = meta.indexOf(DUMP);
                    if (dump > -1) {
                        meta = meta.substring(4);
                    }

                    // use the toString if it is not the Object toString 
                    // and the 'dump' meta info was not found
                    if (v.toString===OP.toString || dump>-1) {
                        v = L.dump(v, parseInt(meta, 10));
                    } else {
                        v = v.toString();
                    }
                }
            } else if (!L.isString(v) && !L.isNumber(v)) {
                // This {block} has no replace string. Save it for later.
                v = "~-" + saved.length + "-~";
                saved[saved.length] = token;

                // break;
            }

            s = s.substring(0, i) + v + s.substring(j + 1);


        }

        // restore saved {block}s
        for (i=saved.length-1; i>=0; i=i-1) {
            s = s.replace(new RegExp("~-" + i + "-~"), "{"  + saved[i] + "}", "g");
        }

        return s;
    },


    /**
     * Returns a string without any leading or trailing whitespace.  If 
     * the input is not a string, the input will be returned untouched.
     * @method trim
     * @since 2.3.0
     * @param s {string} the string to trim
     * @return {string} the trimmed string
     */
    trim: function(s){
        try {
            return s.replace(/^\s+|\s+$/g, "");
        } catch(e) {
            return s;
        }
    },

    /**
     * Returns a new object containing all of the properties of
     * all the supplied objects.  The properties from later objects
     * will overwrite those in earlier objects.
     * @method merge
     * @since 2.3.0
     * @param arguments {Object*} the objects to merge
     * @return the new merged object
     */
    merge: function() {
        var o={}, a=arguments, l=a.length, i;
        for (i=0; i<l; i=i+1) {
            L.augmentObject(o, a[i], true);
        }
        return o;
    },

    /**
     * Executes the supplied function in the context of the supplied 
     * object 'when' milliseconds later.  Executes the function a 
     * single time unless periodic is set to true.
     * @method later
     * @since 2.4.0
     * @param when {int} the number of milliseconds to wait until the fn 
     * is executed
     * @param o the context object
     * @param fn {Function|String} the function to execute or the name of 
     * the method in the 'o' object to execute
     * @param data [Array] data that is provided to the function.  This accepts
     * either a single item or an array.  If an array is provided, the
     * function is executed with one parameter for each array item.  If
     * you need to pass a single array parameter, it needs to be wrapped in
     * an array [myarray]
     * @param periodic {boolean} if true, executes continuously at supplied 
     * interval until canceled
     * @return a timer object. Call the cancel() method on this object to 
     * stop the timer.
     */
    later: function(when, o, fn, data, periodic) {
        when = when || 0; 
        o = o || {};
        var m=fn, d=data, f, r;

        if (L.isString(fn)) {
            m = o[fn];
        }

        if (!m) {
            throw new TypeError("method undefined");
        }

        if (!L.isArray(d)) {
            d = [data];
        }

        f = function() {
            m.apply(o, d);
        };

        r = (periodic) ? setInterval(f, when) : setTimeout(f, when);

        return {
            interval: periodic,
            cancel: function() {
                if (this.interval) {
                    clearInterval(r);
                } else {
                    clearTimeout(r);
                }
            }
        };
    },
    
    /**
     * A convenience method for detecting a legitimate non-null value.
     * Returns false for null/undefined/NaN, true for other values, 
     * including 0/false/''
     * @method isValue
     * @since 2.3.0
     * @param o {any} the item to test
     * @return {boolean} true if it is not null/undefined/NaN || false
     */
    isValue: function(o) {
        // return (o || o === false || o === 0 || o === ''); // Infinity fails
return (L.isObject(o) || L.isString(o) || L.isNumber(o) || L.isBoolean(o));
    }

};

/**
 * Determines whether or not the property was added
 * to the object instance.  Returns false if the property is not present
 * in the object, or was inherited from the prototype.
 * This abstraction is provided to enable hasOwnProperty for Safari 1.3.x.
 * There is a discrepancy between YAHOO.lang.hasOwnProperty and
 * Object.prototype.hasOwnProperty when the property is a primitive added to
 * both the instance AND prototype with the same value:
 * <pre>
 * var A = function() {};
 * A.prototype.foo = 'foo';
 * var a = new A();
 * a.foo = 'foo';
 * alert(a.hasOwnProperty('foo')); // true
 * alert(YAHOO.lang.hasOwnProperty(a, 'foo')); // false when using fallback
 * </pre>
 * @method hasOwnProperty
 * @param {any} o The object being testing
 * @param prop {string} the name of the property to test
 * @return {boolean} the result
 */
L.hasOwnProperty = (OP.hasOwnProperty) ?
    function(o, prop) {
        return o && o.hasOwnProperty(prop);
    } : function(o, prop) {
        return !L.isUndefined(o[prop]) && 
                o.constructor.prototype[prop] !== o[prop];
    };

// new lang wins
OB.augmentObject(L, OB, true);

/*
 * An alias for <a href="YAHOO.lang.html">YAHOO.lang</a>
 * @class YAHOO.util.Lang
 */
YAHOO.util.Lang = L;
 
/**
 * Same as YAHOO.lang.augmentObject, except it only applies prototype 
 * properties.  This is an alias for augmentProto.
 * @see YAHOO.lang.augmentObject
 * @method augment
 * @static
 * @param {Function} r  the object to receive the augmentation
 * @param {Function} s  the object that supplies the properties to augment
 * @param {String*|boolean}  arguments zero or more properties methods to 
 *        augment the receiver with.  If none specified, everything
 *        in the supplier will be used unless it would
 *        overwrite an existing property in the receiver.  if true
 *        is specified as the third parameter, all properties will
 *        be applied and will overwrite an existing property in
 *        the receiver
 */
L.augment = L.augmentProto;

/**
 * An alias for <a href="YAHOO.lang.html#augment">YAHOO.lang.augment</a>
 * @for YAHOO
 * @method augment
 * @static
 * @param {Function} r  the object to receive the augmentation
 * @param {Function} s  the object that supplies the properties to augment
 * @param {String*}  arguments zero or more properties methods to 
 *        augment the receiver with.  If none specified, everything
 *        in the supplier will be used unless it would
 *        overwrite an existing property in the receiver
 */
YAHOO.augment = L.augmentProto;
       
/**
 * An alias for <a href="YAHOO.lang.html#extend">YAHOO.lang.extend</a>
 * @method extend
 * @static
 * @param {Function} subc   the object to modify
 * @param {Function} superc the object to inherit
 * @param {Object} overrides  additional properties/methods to add to the
 *        subclass prototype.  These will override the
 *        matching items obtained from the superclass if present.
 */
YAHOO.extend = L.extend;

})();
YAHOO.register("yahoo", YAHOO, {version: "@VERSION@", build: "@BUILD@"});
/**
 * The dom module provides helper methods for manipulating Dom elements.
 * @module dom
 *
 */

(function() {
    // for use with generateId (global to save state if Dom is overwritten)
    YAHOO.env._id_counter = YAHOO.env._id_counter || 0;

    // internal shorthand
    var Y = YAHOO.util,
        lang = YAHOO.lang,
        UA = YAHOO.env.ua,
        trim = YAHOO.lang.trim,
        propertyCache = {}, // for faster hyphen converts
        reCache = {}, // cache className regexes
        RE_TABLE = /^t(?:able|d|h)$/i, // for _calcBorders
        RE_COLOR = /color$/i,

        // DOM aliases 
        document = window.document,     
        documentElement = document.documentElement,

        // string constants
        OWNER_DOCUMENT = 'ownerDocument',
        DEFAULT_VIEW = 'defaultView',
        DOCUMENT_ELEMENT = 'documentElement',
        COMPAT_MODE = 'compatMode',
        OFFSET_LEFT = 'offsetLeft',
        OFFSET_TOP = 'offsetTop',
        OFFSET_PARENT = 'offsetParent',
        PARENT_NODE = 'parentNode',
        NODE_TYPE = 'nodeType',
        TAG_NAME = 'tagName',
        SCROLL_LEFT = 'scrollLeft',
        SCROLL_TOP = 'scrollTop',
        GET_BOUNDING_CLIENT_RECT = 'getBoundingClientRect',
        GET_COMPUTED_STYLE = 'getComputedStyle',
        CURRENT_STYLE = 'currentStyle',
        CSS1_COMPAT = 'CSS1Compat',
        _BACK_COMPAT = 'BackCompat',
        _CLASS = 'class', // underscore due to reserved word
        CLASS_NAME = 'className',
        EMPTY = '',
        SPACE = ' ',
        C_START = '(?:^|\\s)',
        C_END = '(?= |$)',
        G = 'g',
        POSITION = 'position',
        FIXED = 'fixed',
        RELATIVE = 'relative',
        LEFT = 'left',
        TOP = 'top',
        MEDIUM = 'medium',
        BORDER_LEFT_WIDTH = 'borderLeftWidth',
        BORDER_TOP_WIDTH = 'borderTopWidth',
    
    // brower detection
        isOpera = UA.opera,
        isSafari = UA.webkit, 
        isGecko = UA.gecko, 
        isIE = UA.ie; 
    
    /**
     * Provides helper methods for DOM elements.
     * @namespace YAHOO.util
     * @class Dom
     * @requires yahoo, event
     */
    Y.Dom = {
        CUSTOM_ATTRIBUTES: (!documentElement.hasAttribute) ? { // IE < 8
            'for': 'htmlFor',
            'class': CLASS_NAME
        } : { // w3c
            'htmlFor': 'for',
            'className': _CLASS
        },

        /**
         * Returns an HTMLElement reference.
         * @method get
         * @param {String | HTMLElement |Array} el Accepts a string to use as an ID for getting a DOM reference, an actual DOM reference, or an Array of IDs and/or HTMLElements.
         * @return {HTMLElement | Array} A DOM reference to an HTML element or an array of HTMLElements.
         */
        get: function(el) {
            var id, nodes, c, i, len;

            if (el) {
                if (el[NODE_TYPE] || el.item) { // Node, or NodeList
                    return el;
                }

                if (typeof el === 'string') { // id
                    id = el;
                    el = document.getElementById(el);
                    if (el && el.id === id) { // IE: avoid false match on "name" attribute
                    return el;
                    } else if (el && document.all) { // filter by name
                        el = null;
                        nodes = document.all[id];
                        for (i = 0, len = nodes.length; i < len; ++i) {
                            if (nodes[i].id === id) {
                                return nodes[i];
                            }
                        }
                    }
                    return el;
                }
                
                if (YAHOO.util.Element && el instanceof YAHOO.util.Element) {
                    el = el.get('element');
                }

                if ('length' in el) { // array-like 
                    c = [];
                    for (i = 0, len = el.length; i < len; ++i) {
                        c[c.length] = Y.Dom.get(el[i]);
                    }
                    
                    return c;
                }

                return el; // some other object, just pass it back
            }

            return null;
        },
    
        getComputedStyle: function(el, property) {
            if (window[GET_COMPUTED_STYLE]) {
                return el[OWNER_DOCUMENT][DEFAULT_VIEW][GET_COMPUTED_STYLE](el, null)[property];
            } else if (el[CURRENT_STYLE]) {
                return Y.Dom.IE_ComputedStyle.get(el, property);
            }
        },

        /**
         * Normalizes currentStyle and ComputedStyle.
         * @method getStyle
         * @param {String | HTMLElement |Array} el Accepts a string to use as an ID, an actual DOM reference, or an Array of IDs and/or HTMLElements.
         * @param {String} property The style property whose value is returned.
         * @return {String | Array} The current value of the style property for the element(s).
         */
        getStyle: function(el, property) {
            return Y.Dom.batch(el, Y.Dom._getStyle, property);
        },

        // branching at load instead of runtime
        _getStyle: function() {
            if (window[GET_COMPUTED_STYLE]) { // W3C DOM method
                return function(el, property) {
                    property = (property === 'float') ? property = 'cssFloat' :
                            Y.Dom._toCamel(property);

                    var value = el.style[property],
                        computed;
                    
                    if (!value) {
                        computed = el[OWNER_DOCUMENT][DEFAULT_VIEW][GET_COMPUTED_STYLE](el, null);
                        if (computed) { // test computed before touching for safari
                            value = computed[property];
                        }
                    }
                    
                    return value;
                };
            } else if (documentElement[CURRENT_STYLE]) {
                return function(el, property) {                         
                    var value;

                    switch(property) {
                        case 'opacity' :// IE opacity uses filter
                            value = 100;
                            try { // will error if no DXImageTransform
                                value = el.filters['DXImageTransform.Microsoft.Alpha'].opacity;

                            } catch(e) {
                                try { // make sure its in the document
                                    value = el.filters('alpha').opacity;
                                } catch(err) {
                                    YAHOO.log('getStyle: IE filter failed',
                                            'error', 'Dom');
                                }
                            }
                            return value / 100;
                        case 'float': // fix reserved word
                            property = 'styleFloat'; // fall through
                        default: 
                            property = Y.Dom._toCamel(property);
                            value = el[CURRENT_STYLE] ? el[CURRENT_STYLE][property] : null;
                            return ( el.style[property] || value );
                    }
                };
            }
        }(),
    
        /**
         * Wrapper for setting style properties of HTMLElements.  Normalizes "opacity" across modern browsers.
         * @method setStyle
         * @param {String | HTMLElement | Array} el Accepts a string to use as an ID, an actual DOM reference, or an Array of IDs and/or HTMLElements.
         * @param {String} property The style property to be set.
         * @param {String} val The value to apply to the given property.
         */
        setStyle: function(el, property, val) {
            Y.Dom.batch(el, Y.Dom._setStyle, { prop: property, val: val });
        },

        _setStyle: function() {
            if (isIE) {
                return function(el, args) {
                    var property = Y.Dom._toCamel(args.prop),
                        val = args.val;

                    if (el) {
                        switch (property) {
                            case 'opacity':
                                if ( lang.isString(el.style.filter) ) { // in case not appended
                                    el.style.filter = 'alpha(opacity=' + val * 100 + ')';
                                    
                                    if (!el[CURRENT_STYLE] || !el[CURRENT_STYLE].hasLayout) {
                                        el.style.zoom = 1; // when no layout or cant tell
                                    }
                                }
                                break;
                            case 'float':
                                property = 'styleFloat';
                            default:
                            el.style[property] = val;
                        }
                    } else {
                        YAHOO.log('element ' + el + ' is undefined', 'error', 'Dom');
                    }
                };
            } else {
                return function(el, args) {
                    var property = Y.Dom._toCamel(args.prop),
                        val = args.val;
                    if (el) {
                        if (property == 'float') {
                            property = 'cssFloat';
                        }
                        el.style[property] = val;
                    } else {
                        YAHOO.log('element ' + el + ' is undefined', 'error', 'Dom');
                    }
                };
            }

        }(),
        
        /**
         * Gets the current position of an element based on page coordinates. 
         * Element must be part of the DOM tree to have page coordinates (display:none or elements not appended return false).
         * @method getXY
         * @param {String | HTMLElement | Array} el Accepts a string to use as an ID, an actual DOM
         * reference, or an Array of IDs and/or HTMLElements
         * @return {Array} The XY position of the element(s)
         */
        getXY: function(el) {
            return Y.Dom.batch(el, Y.Dom._getXY);
        },

        _canPosition: function(el) {
            return ( Y.Dom._getStyle(el, 'display') !== 'none' && Y.Dom._inDoc(el) );
        },

        _getXY: function() {
            if (document[DOCUMENT_ELEMENT][GET_BOUNDING_CLIENT_RECT]) {
                return function(node) {
                    var scrollLeft, scrollTop, box, doc,
                        off1, off2, mode, bLeft, bTop,
                        floor = Math.floor, // TODO: round?
                        xy = false;

                    if (Y.Dom._canPosition(node)) {
                        box = node[GET_BOUNDING_CLIENT_RECT]();
                        doc = node[OWNER_DOCUMENT];
                        scrollLeft = Y.Dom.getDocumentScrollLeft(doc);
                        scrollTop = Y.Dom.getDocumentScrollTop(doc);
                        xy = [floor(box[LEFT]), floor(box[TOP])];

                        if (isIE && UA.ie < 8) { // IE < 8: viewport off by 2
                            off1 = 2;
                            off2 = 2;
                            mode = doc[COMPAT_MODE];

                            if (UA.ie === 6) {
                                if (mode !== _BACK_COMPAT) {
                                    off1 = 0;
                                    off2 = 0;
                                }
                            }
                            
                            if ((mode === _BACK_COMPAT)) {
                                bLeft = _getComputedStyle(doc[DOCUMENT_ELEMENT], BORDER_LEFT_WIDTH);
                                bTop = _getComputedStyle(doc[DOCUMENT_ELEMENT], BORDER_TOP_WIDTH);
                                if (bLeft !== MEDIUM) {
                                    off1 = parseInt(bLeft, 10);
                                }
                                if (bTop !== MEDIUM) {
                                    off2 = parseInt(bTop, 10);
                                }
                            }
                            
                            xy[0] -= off1;
                            xy[1] -= off2;

                        }

                        if ((scrollTop || scrollLeft)) {
                            xy[0] += scrollLeft;
                            xy[1] += scrollTop;
                        }

                        // gecko may return sub-pixel (non-int) values
                        xy[0] = floor(xy[0]);
                        xy[1] = floor(xy[1]);
                    } else {
                        YAHOO.log('getXY failed: element not positionable (either not in a document or not displayed)', 'error', 'Dom');
                    }

                    return xy;
                };
            } else {
                return function(node) { // ff2, safari: manually calculate by crawling up offsetParents
                    var docScrollLeft, docScrollTop,
                        scrollTop, scrollLeft,
                        bCheck,
                        xy = false,
                        parentNode = node;

                    if  (Y.Dom._canPosition(node) ) {
                        xy = [node[OFFSET_LEFT], node[OFFSET_TOP]];
                        docScrollLeft = Y.Dom.getDocumentScrollLeft(node[OWNER_DOCUMENT]);
                        docScrollTop = Y.Dom.getDocumentScrollTop(node[OWNER_DOCUMENT]);

                        // TODO: refactor with !! or just falsey
                        bCheck = ((isGecko || UA.webkit > 519) ? true : false);

                        // TODO: worth refactoring for TOP/LEFT only?
                        while ((parentNode = parentNode[OFFSET_PARENT])) {
                            xy[0] += parentNode[OFFSET_LEFT];
                            xy[1] += parentNode[OFFSET_TOP];
                            if (bCheck) {
                                xy = Y.Dom._calcBorders(parentNode, xy);
                            }
                        }

                        // account for any scrolled ancestors
                        if (Y.Dom._getStyle(node, POSITION) !== FIXED) {
                            parentNode = node;

                            while ((parentNode = parentNode[PARENT_NODE]) && parentNode[TAG_NAME]) {
                                scrollTop = parentNode[SCROLL_TOP];
                                scrollLeft = parentNode[SCROLL_LEFT];

                                //Firefox does something funky with borders when overflow is not visible.
                                if (isGecko && (Y.Dom._getStyle(parentNode, 'overflow') !== 'visible')) {
                                        xy = Y.Dom._calcBorders(parentNode, xy);
                                }

                                if (scrollTop || scrollLeft) {
                                    xy[0] -= scrollLeft;
                                    xy[1] -= scrollTop;
                                }
                            }
                            xy[0] += docScrollLeft;
                            xy[1] += docScrollTop;

                        } else {
                            //Fix FIXED position -- add scrollbars
                            if (isOpera) {
                                xy[0] -= docScrollLeft;
                                xy[1] -= docScrollTop;
                            } else if (isSafari || isGecko) {
                                xy[0] += docScrollLeft;
                                xy[1] += docScrollTop;
                            }
                        }
                        //Round the numbers so we get sane data back
                        xy[0] = Math.floor(xy[0]);
                        xy[1] = Math.floor(xy[1]);
                    } else {
                        YAHOO.log('getXY failed: element not positionable (either not in a document or not displayed)', 'error', 'Dom');
                    }
                    return xy;                
                };
            }
        }(), // NOTE: Executing for loadtime branching
        
        /**
         * Gets the current X position of an element based on page coordinates.  The element must be part of the DOM tree to have page coordinates (display:none or elements not appended return false).
         * @method getX
         * @param {String | HTMLElement | Array} el Accepts a string to use as an ID, an actual DOM reference, or an Array of IDs and/or HTMLElements
         * @return {Number | Array} The X position of the element(s)
         */
        getX: function(el) {
            var f = function(el) {
                return Y.Dom.getXY(el)[0];
            };
            
            return Y.Dom.batch(el, f, Y.Dom, true);
        },
        
        /**
         * Gets the current Y position of an element based on page coordinates.  Element must be part of the DOM tree to have page coordinates (display:none or elements not appended return false).
         * @method getY
         * @param {String | HTMLElement | Array} el Accepts a string to use as an ID, an actual DOM reference, or an Array of IDs and/or HTMLElements
         * @return {Number | Array} The Y position of the element(s)
         */
        getY: function(el) {
            var f = function(el) {
                return Y.Dom.getXY(el)[1];
            };
            
            return Y.Dom.batch(el, f, Y.Dom, true);
        },
        
        /**
         * Set the position of an html element in page coordinates, regardless of how the element is positioned.
         * The element(s) must be part of the DOM tree to have page coordinates (display:none or elements not appended return false).
         * @method setXY
         * @param {String | HTMLElement | Array} el Accepts a string to use as an ID, an actual DOM reference, or an Array of IDs and/or HTMLElements
         * @param {Array} pos Contains X & Y values for new position (coordinates are page-based)
         * @param {Boolean} noRetry By default we try and set the position a second time if the first fails
         */
        setXY: function(el, pos, noRetry) {
            Y.Dom.batch(el, Y.Dom._setXY, { pos: pos, noRetry: noRetry });
        },

        _setXY: function(node, args) {
            var pos = Y.Dom._getStyle(node, POSITION),
                setStyle = Y.Dom.setStyle,
                xy = args.pos,
                noRetry = args.noRetry,

                delta = [ // assuming pixels; if not we will have to retry
                    parseInt( Y.Dom.getComputedStyle(node, LEFT), 10 ),
                    parseInt( Y.Dom.getComputedStyle(node, TOP), 10 )
                ],

                currentXY,
                newXY;
        
            if (pos == 'static') { // default to relative
                pos = RELATIVE;
                setStyle(node, POSITION, pos);
            }

            currentXY = Y.Dom._getXY(node);

            if (!xy || currentXY === false) { // has to be part of doc to have xy
                YAHOO.log('xy failed: node not available', 'error', 'Node');
                return false; 
            }
            
            if ( isNaN(delta[0]) ) {// in case of 'auto'
                delta[0] = (pos == RELATIVE) ? 0 : node[OFFSET_LEFT];
            } 
            if ( isNaN(delta[1]) ) { // in case of 'auto'
                delta[1] = (pos == RELATIVE) ? 0 : node[OFFSET_TOP];
            } 

            if (xy[0] !== null) { // from setX
                setStyle(node, LEFT, xy[0] - currentXY[0] + delta[0] + 'px');
            }

            if (xy[1] !== null) { // from setY
                setStyle(node, TOP, xy[1] - currentXY[1] + delta[1] + 'px');
            }
          
            if (!noRetry) {
                newXY = Y.Dom._getXY(node);

                // if retry is true, try one more time if we miss 
               if ( (xy[0] !== null && newXY[0] != xy[0]) || 
                    (xy[1] !== null && newXY[1] != xy[1]) ) {
                   Y.Dom._setXY(node, { pos: xy, noRetry: true });
               }
            }        

            YAHOO.log('setXY setting position to ' + xy, 'info', 'Node');
        },
        
        /**
         * Set the X position of an html element in page coordinates, regardless of how the element is positioned.
         * The element must be part of the DOM tree to have page coordinates (display:none or elements not appended return false).
         * @method setX
         * @param {String | HTMLElement | Array} el Accepts a string to use as an ID, an actual DOM reference, or an Array of IDs and/or HTMLElements.
         * @param {Int} x The value to use as the X coordinate for the element(s).
         */
        setX: function(el, x) {
            Y.Dom.setXY(el, [x, null]);
        },
        
        /**
         * Set the Y position of an html element in page coordinates, regardless of how the element is positioned.
         * The element must be part of the DOM tree to have page coordinates (display:none or elements not appended return false).
         * @method setY
         * @param {String | HTMLElement | Array} el Accepts a string to use as an ID, an actual DOM reference, or an Array of IDs and/or HTMLElements.
         * @param {Int} x To use as the Y coordinate for the element(s).
         */
        setY: function(el, y) {
            Y.Dom.setXY(el, [null, y]);
        },
        
        /**
         * Returns the region position of the given element.
         * The element must be part of the DOM tree to have a region (display:none or elements not appended return false).
         * @method getRegion
         * @param {String | HTMLElement | Array} el Accepts a string to use as an ID, an actual DOM reference, or an Array of IDs and/or HTMLElements.
         * @return {Region | Array} A Region or array of Region instances containing "top, left, bottom, right" member data.
         */
        getRegion: function(el) {
            var f = function(el) {
                var region = false;
                if ( Y.Dom._canPosition(el) ) {
                    region = Y.Region.getRegion(el);
                    YAHOO.log('getRegion returning ' + region, 'info', 'Dom');
                } else {
                    YAHOO.log('getRegion failed: element not positionable (either not in a document or not displayed)', 'error', 'Dom');
                }

                return region;
            };
            
            return Y.Dom.batch(el, f, Y.Dom, true);
        },
        
        /**
         * Returns the width of the client (viewport).
         * @method getClientWidth
         * @deprecated Now using getViewportWidth.  This interface left intact for back compat.
         * @return {Int} The width of the viewable area of the page.
         */
        getClientWidth: function() {
            return Y.Dom.getViewportWidth();
        },
        
        /**
         * Returns the height of the client (viewport).
         * @method getClientHeight
         * @deprecated Now using getViewportHeight.  This interface left intact for back compat.
         * @return {Int} The height of the viewable area of the page.
         */
        getClientHeight: function() {
            return Y.Dom.getViewportHeight();
        },

        /**
         * Returns an array of HTMLElements with the given class.
         * For optimized performance, include a tag and/or root node when possible.
         * Note: This method operates against a live collection, so modifying the 
         * collection in the callback (removing/appending nodes, etc.) will have
         * side effects.  Instead you should iterate the returned nodes array,
         * as you would with the native "getElementsByTagName" method. 
         * @method getElementsByClassName
         * @param {String} className The class name to match against
         * @param {String} tag (optional) The tag name of the elements being collected
         * @param {String | HTMLElement} root (optional) The HTMLElement or an ID to use as the starting point 
         * @param {Function} apply (optional) A function to apply to each element when found 
         * @param {Any} o (optional) An optional arg that is passed to the supplied method
         * @param {Boolean} overrides (optional) Whether or not to override the scope of "method" with "o"
         * @return {Array} An array of elements that have the given class name
         */
        getElementsByClassName: function(className, tag, root, apply, o, overrides) {
            className = lang.trim(className);
            tag = tag || '*';
            root = (root) ? Y.Dom.get(root) : null || document; 
            if (!root) {
                return [];
            }

            var nodes = [],
                elements = root.getElementsByTagName(tag),
                hasClass = Y.Dom.hasClass;

            for (var i = 0, len = elements.length; i < len; ++i) {
                if ( hasClass(elements[i], className) ) {
                    nodes[nodes.length] = elements[i];
                }
            }
            
            if (apply) {
                Y.Dom.batch(nodes, apply, o, overrides);
            }

            return nodes;
        },

        /**
         * Determines whether an HTMLElement has the given className.
         * @method hasClass
         * @param {String | HTMLElement | Array} el The element or collection to test
         * @param {String} className the class name to search for
         * @return {Boolean | Array} A boolean value or array of boolean values
         */
        hasClass: function(el, className) {
            return Y.Dom.batch(el, Y.Dom._hasClass, className);
        },

        _hasClass: function(el, className) {
            var ret = false,
                current;
            
            if (el && className) {
                current = Y.Dom._getAttribute(el, CLASS_NAME) || EMPTY;
                if (className.exec) {
                    ret = className.test(current);
                } else {
                    ret = className && (SPACE + current + SPACE).
                        indexOf(SPACE + className + SPACE) > -1;
                }
            } else {
                YAHOO.log('hasClass called with invalid arguments', 'warn', 'Dom');
            }

            return ret;
        },
    
        /**
         * Adds a class name to a given element or collection of elements.
         * @method addClass         
         * @param {String | HTMLElement | Array} el The element or collection to add the class to
         * @param {String} className the class name to add to the class attribute
         * @return {Boolean | Array} A pass/fail boolean or array of booleans
         */
        addClass: function(el, className) {
            return Y.Dom.batch(el, Y.Dom._addClass, className);
        },

        _addClass: function(el, className) {
            var ret = false,
                current;

            if (el && className) {
                current = Y.Dom._getAttribute(el, CLASS_NAME) || EMPTY;
                if ( !Y.Dom._hasClass(el, className) ) {
                    Y.Dom.setAttribute(el, CLASS_NAME, trim(current + SPACE + className));
                    ret = true;
                }
            } else {
                YAHOO.log('addClass called with invalid arguments', 'warn', 'Dom');
            }

            return ret;
        },
    
        /**
         * Removes a class name from a given element or collection of elements.
         * @method removeClass         
         * @param {String | HTMLElement | Array} el The element or collection to remove the class from
         * @param {String} className the class name to remove from the class attribute
         * @return {Boolean | Array} A pass/fail boolean or array of booleans
         */
        removeClass: function(el, className) {
            return Y.Dom.batch(el, Y.Dom._removeClass, className);
        },
        
        _removeClass: function(el, className) {
            var ret = false,
                current,
                newClass,
                attr;

            if (el && className) {
                current = Y.Dom._getAttribute(el, CLASS_NAME) || EMPTY;
                Y.Dom.setAttribute(el, CLASS_NAME, current.replace(Y.Dom._getClassRegex(className), EMPTY));

                newClass = Y.Dom._getAttribute(el, CLASS_NAME);
                if (current !== newClass) { // else nothing changed
                    Y.Dom.setAttribute(el, CLASS_NAME, trim(newClass)); // trim after comparing to current class
                    ret = true;

                    if (Y.Dom._getAttribute(el, CLASS_NAME) === '') { // remove class attribute if empty
                        attr = (el.hasAttribute && el.hasAttribute(_CLASS)) ? _CLASS : CLASS_NAME;
                        YAHOO.log('removeClass removing empty class attribute', 'info', 'Dom');
                        el.removeAttribute(attr);
                    }
                }

            } else {
                YAHOO.log('removeClass called with invalid arguments', 'warn', 'Dom');
            }

            return ret;
        },
        
        /**
         * Replace a class with another class for a given element or collection of elements.
         * If no oldClassName is present, the newClassName is simply added.
         * @method replaceClass  
         * @param {String | HTMLElement | Array} el The element or collection to remove the class from
         * @param {String} oldClassName the class name to be replaced
         * @param {String} newClassName the class name that will be replacing the old class name
         * @return {Boolean | Array} A pass/fail boolean or array of booleans
         */
        replaceClass: function(el, oldClassName, newClassName) {
            return Y.Dom.batch(el, Y.Dom._replaceClass, { from: oldClassName, to: newClassName });
        },

        _replaceClass: function(el, classObj) {
            var className,
                from,
                to,
                ret = false,
                current;

            if (el && classObj) {
                from = classObj.from;
                to = classObj.to;

                if (!to) {
                    ret = false;
                }  else if (!from) { // just add if no "from"
                    ret = Y.Dom._addClass(el, classObj.to);
                } else if (from !== to) { // else nothing to replace
                    // May need to lead with DBLSPACE?
                    current = Y.Dom._getAttribute(el, CLASS_NAME) || EMPTY;
                    className = (SPACE + current.replace(Y.Dom._getClassRegex(from), SPACE + to)).
                               split(Y.Dom._getClassRegex(to));

                    // insert to into what would have been the first occurrence slot
                    className.splice(1, 0, SPACE + to);
                    Y.Dom.setAttribute(el, CLASS_NAME, trim(className.join(EMPTY)));
                    ret = true;
                }
            } else {
                YAHOO.log('replaceClass called with invalid arguments', 'warn', 'Dom');
            }

            return ret;
        },
        
        /**
         * Returns an ID and applies it to the element "el", if provided.
         * @method generateId  
         * @param {String | HTMLElement | Array} el (optional) An optional element array of elements to add an ID to (no ID is added if one is already present).
         * @param {String} prefix (optional) an optional prefix to use (defaults to "yui-gen").
         * @return {String | Array} The generated ID, or array of generated IDs (or original ID if already present on an element)
         */
        generateId: function(el, prefix) {
            prefix = prefix || 'yui-gen';

            var f = function(el) {
                if (el && el.id) { // do not override existing ID
                    YAHOO.log('generateId returning existing id ' + el.id, 'info', 'Dom');
                    return el.id;
                }

                var id = prefix + YAHOO.env._id_counter++;
                YAHOO.log('generateId generating ' + id, 'info', 'Dom');

                if (el) {
                    if (el[OWNER_DOCUMENT] && el[OWNER_DOCUMENT].getElementById(id)) { // in case one already exists
                        // use failed id plus prefix to help ensure uniqueness
                        return Y.Dom.generateId(el, id + prefix);
                    }
                    el.id = id;
                }
                
                return id;
            };

            // batch fails when no element, so just generate and return single ID
            return Y.Dom.batch(el, f, Y.Dom, true) || f.apply(Y.Dom, arguments);
        },
        
        /**
         * Determines whether an HTMLElement is an ancestor of another HTML element in the DOM hierarchy.
         * @method isAncestor
         * @param {String | HTMLElement} haystack The possible ancestor
         * @param {String | HTMLElement} needle The possible descendent
         * @return {Boolean} Whether or not the haystack is an ancestor of needle
         */
        isAncestor: function(haystack, needle) {
            haystack = Y.Dom.get(haystack);
            needle = Y.Dom.get(needle);
            
            var ret = false;

            if ( (haystack && needle) && (haystack[NODE_TYPE] && needle[NODE_TYPE]) ) {
                if (haystack.contains && haystack !== needle) { // contains returns true when equal
                    ret = haystack.contains(needle);
                }
                else if (haystack.compareDocumentPosition) { // gecko
                    ret = !!(haystack.compareDocumentPosition(needle) & 16);
                }
            } else {
                YAHOO.log('isAncestor failed; invalid input: ' + haystack + ',' + needle, 'error', 'Dom');
            }
            YAHOO.log('isAncestor(' + haystack + ',' + needle + ' returning ' + ret, 'info', 'Dom');
            return ret;
        },
        
        /**
         * Determines whether an HTMLElement is present in the current document.
         * @method inDocument         
         * @param {String | HTMLElement} el The element to search for
         * @param {Object} doc An optional document to search, defaults to element's owner document 
         * @return {Boolean} Whether or not the element is present in the current document
         */
        inDocument: function(el, doc) {
            return Y.Dom._inDoc(Y.Dom.get(el), doc);
        },

        _inDoc: function(el, doc) {
            var ret = false;
            if (el && el[TAG_NAME]) {
                doc = doc || el[OWNER_DOCUMENT]; 
                ret = Y.Dom.isAncestor(doc[DOCUMENT_ELEMENT], el);
            } else {
                YAHOO.log('inDocument failed: invalid input', 'error', 'Dom');
            }
            return ret;
        },
        
        /**
         * Returns an array of HTMLElements that pass the test applied by supplied boolean method.
         * For optimized performance, include a tag and/or root node when possible.
         * Note: This method operates against a live collection, so modifying the 
         * collection in the callback (removing/appending nodes, etc.) will have
         * side effects.  Instead you should iterate the returned nodes array,
         * as you would with the native "getElementsByTagName" method. 
         * @method getElementsBy
         * @param {Function} method - A boolean method for testing elements which receives the element as its only argument.
         * @param {String} tag (optional) The tag name of the elements being collected
         * @param {String | HTMLElement} root (optional) The HTMLElement or an ID to use as the starting point 
         * @param {Function} apply (optional) A function to apply to each element when found 
         * @param {Any} o (optional) An optional arg that is passed to the supplied method
         * @param {Boolean} overrides (optional) Whether or not to override the scope of "method" with "o"
         * @return {Array} Array of HTMLElements
         */
        getElementsBy: function(method, tag, root, apply, o, overrides, firstOnly) {
            tag = tag || '*';
            root = (root) ? Y.Dom.get(root) : null || document; 

            if (!root) {
                return [];
            }

            var nodes = [],
                elements = root.getElementsByTagName(tag);
            
            for (var i = 0, len = elements.length; i < len; ++i) {
                if ( method(elements[i]) ) {
                    if (firstOnly) {
                        nodes = elements[i]; 
                        break;
                    } else {
                        nodes[nodes.length] = elements[i];
                    }
                }
            }

            if (apply) {
                Y.Dom.batch(nodes, apply, o, overrides);
            }

            YAHOO.log('getElementsBy returning ' + nodes, 'info', 'Dom');
            
            return nodes;
        },
        
        /**
         * Returns the first HTMLElement that passes the test applied by the supplied boolean method.
         * @method getElementBy
         * @param {Function} method - A boolean method for testing elements which receives the element as its only argument.
         * @param {String} tag (optional) The tag name of the elements being collected
         * @param {String | HTMLElement} root (optional) The HTMLElement or an ID to use as the starting point 
         * @return {HTMLElement}
         */
        getElementBy: function(method, tag, root) {
            return Y.Dom.getElementsBy(method, tag, root, null, null, null, true); 
        },

        /**
         * Runs the supplied method against each item in the Collection/Array.
         * The method is called with the element(s) as the first arg, and the optional param as the second ( method(el, o) ).
         * @method batch
         * @param {String | HTMLElement | Array} el (optional) An element or array of elements to apply the method to
         * @param {Function} method The method to apply to the element(s)
         * @param {Any} o (optional) An optional arg that is passed to the supplied method
         * @param {Boolean} overrides (optional) Whether or not to override the scope of "method" with "o"
         * @return {Any | Array} The return value(s) from the supplied method
         */
        batch: function(el, method, o, overrides) {
            var collection = [],
                scope = (overrides) ? o : window;
                
            el = (el && (el[TAG_NAME] || el.item)) ? el : Y.Dom.get(el); // skip get() when possible
            if (el && method) {
                if (el[TAG_NAME] || el.length === undefined) { // element or not array-like 
                    return method.call(scope, el, o);
                } 

                for (var i = 0; i < el.length; ++i) {
                    collection[collection.length] = method.call(scope, el[i], o);
                }
            } else {
                YAHOO.log('batch called with invalid arguments', 'warn', 'Dom');
                return false;
            } 
            return collection;
        },
        
        /**
         * Returns the height of the document.
         * @method getDocumentHeight
         * @return {Int} The height of the actual document (which includes the body and its margin).
         */
        getDocumentHeight: function() {
            var scrollHeight = (document[COMPAT_MODE] != CSS1_COMPAT || isSafari) ? document.body.scrollHeight : documentElement.scrollHeight,
                h = Math.max(scrollHeight, Y.Dom.getViewportHeight());

            YAHOO.log('getDocumentHeight returning ' + h, 'info', 'Dom');
            return h;
        },
        
        /**
         * Returns the width of the document.
         * @method getDocumentWidth
         * @return {Int} The width of the actual document (which includes the body and its margin).
         */
        getDocumentWidth: function() {
            var scrollWidth = (document[COMPAT_MODE] != CSS1_COMPAT || isSafari) ? document.body.scrollWidth : documentElement.scrollWidth,
                w = Math.max(scrollWidth, Y.Dom.getViewportWidth());
            YAHOO.log('getDocumentWidth returning ' + w, 'info', 'Dom');
            return w;
        },

        /**
         * Returns the current height of the viewport.
         * @method getViewportHeight
         * @return {Int} The height of the viewable area of the page (excludes scrollbars).
         */
        getViewportHeight: function() {
            var height = self.innerHeight, // Safari, Opera
                mode = document[COMPAT_MODE];
        
            if ( (mode || isIE) && !isOpera ) { // IE, Gecko
                height = (mode == CSS1_COMPAT) ?
                        documentElement.clientHeight : // Standards
                        document.body.clientHeight; // Quirks
            }
        
            YAHOO.log('getViewportHeight returning ' + height, 'info', 'Dom');
            return height;
        },
        
        /**
         * Returns the current width of the viewport.
         * @method getViewportWidth
         * @return {Int} The width of the viewable area of the page (excludes scrollbars).
         */
        
        getViewportWidth: function() {
            var width = self.innerWidth,  // Safari
                mode = document[COMPAT_MODE];
            
            if (mode || isIE) { // IE, Gecko, Opera
                width = (mode == CSS1_COMPAT) ?
                        documentElement.clientWidth : // Standards
                        document.body.clientWidth; // Quirks
            }
            YAHOO.log('getViewportWidth returning ' + width, 'info', 'Dom');
            return width;
        },

       /**
         * Returns the nearest ancestor that passes the test applied by supplied boolean method.
         * For performance reasons, IDs are not accepted and argument validation omitted.
         * @method getAncestorBy
         * @param {HTMLElement} node The HTMLElement to use as the starting point 
         * @param {Function} method - A boolean method for testing elements which receives the element as its only argument.
         * @return {Object} HTMLElement or null if not found
         */
        getAncestorBy: function(node, method) {
            while ( (node = node[PARENT_NODE]) ) { // NOTE: assignment
                if ( Y.Dom._testElement(node, method) ) {
                    YAHOO.log('getAncestorBy returning ' + node, 'info', 'Dom');
                    return node;
                }
            } 

            YAHOO.log('getAncestorBy returning null (no ancestor passed test)', 'error', 'Dom');
            return null;
        },
        
        /**
         * Returns the nearest ancestor with the given className.
         * @method getAncestorByClassName
         * @param {String | HTMLElement} node The HTMLElement or an ID to use as the starting point 
         * @param {String} className
         * @return {Object} HTMLElement
         */
        getAncestorByClassName: function(node, className) {
            node = Y.Dom.get(node);
            if (!node) {
                YAHOO.log('getAncestorByClassName failed: invalid node argument', 'error', 'Dom');
                return null;
            }
            var method = function(el) { return Y.Dom.hasClass(el, className); };
            return Y.Dom.getAncestorBy(node, method);
        },

        /**
         * Returns the nearest ancestor with the given tagName.
         * @method getAncestorByTagName
         * @param {String | HTMLElement} node The HTMLElement or an ID to use as the starting point 
         * @param {String} tagName
         * @return {Object} HTMLElement
         */
        getAncestorByTagName: function(node, tagName) {
            node = Y.Dom.get(node);
            if (!node) {
                YAHOO.log('getAncestorByTagName failed: invalid node argument', 'error', 'Dom');
                return null;
            }
            var method = function(el) {
                 return el[TAG_NAME] && el[TAG_NAME].toUpperCase() == tagName.toUpperCase();
            };

            return Y.Dom.getAncestorBy(node, method);
        },

        /**
         * Returns the previous sibling that is an HTMLElement. 
         * For performance reasons, IDs are not accepted and argument validation omitted.
         * Returns the nearest HTMLElement sibling if no method provided.
         * @method getPreviousSiblingBy
         * @param {HTMLElement} node The HTMLElement to use as the starting point 
         * @param {Function} method A boolean function used to test siblings
         * that receives the sibling node being tested as its only argument
         * @return {Object} HTMLElement or null if not found
         */
        getPreviousSiblingBy: function(node, method) {
            while (node) {
                node = node.previousSibling;
                if ( Y.Dom._testElement(node, method) ) {
                    return node;
                }
            }
            return null;
        }, 

        /**
         * Returns the previous sibling that is an HTMLElement 
         * @method getPreviousSibling
         * @param {String | HTMLElement} node The HTMLElement or an ID to use as the starting point 
         * @return {Object} HTMLElement or null if not found
         */
        getPreviousSibling: function(node) {
            node = Y.Dom.get(node);
            if (!node) {
                YAHOO.log('getPreviousSibling failed: invalid node argument', 'error', 'Dom');
                return null;
            }

            return Y.Dom.getPreviousSiblingBy(node);
        }, 

        /**
         * Returns the next HTMLElement sibling that passes the boolean method. 
         * For performance reasons, IDs are not accepted and argument validation omitted.
         * Returns the nearest HTMLElement sibling if no method provided.
         * @method getNextSiblingBy
         * @param {HTMLElement} node The HTMLElement to use as the starting point 
         * @param {Function} method A boolean function used to test siblings
         * that receives the sibling node being tested as its only argument
         * @return {Object} HTMLElement or null if not found
         */
        getNextSiblingBy: function(node, method) {
            while (node) {
                node = node.nextSibling;
                if ( Y.Dom._testElement(node, method) ) {
                    return node;
                }
            }
            return null;
        }, 

        /**
         * Returns the next sibling that is an HTMLElement 
         * @method getNextSibling
         * @param {String | HTMLElement} node The HTMLElement or an ID to use as the starting point 
         * @return {Object} HTMLElement or null if not found
         */
        getNextSibling: function(node) {
            node = Y.Dom.get(node);
            if (!node) {
                YAHOO.log('getNextSibling failed: invalid node argument', 'error', 'Dom');
                return null;
            }

            return Y.Dom.getNextSiblingBy(node);
        }, 

        /**
         * Returns the first HTMLElement child that passes the test method. 
         * @method getFirstChildBy
         * @param {HTMLElement} node The HTMLElement to use as the starting point 
         * @param {Function} method A boolean function used to test children
         * that receives the node being tested as its only argument
         * @return {Object} HTMLElement or null if not found
         */
        getFirstChildBy: function(node, method) {
            var child = ( Y.Dom._testElement(node.firstChild, method) ) ? node.firstChild : null;
            return child || Y.Dom.getNextSiblingBy(node.firstChild, method);
        }, 

        /**
         * Returns the first HTMLElement child. 
         * @method getFirstChild
         * @param {String | HTMLElement} node The HTMLElement or an ID to use as the starting point 
         * @return {Object} HTMLElement or null if not found
         */
        getFirstChild: function(node, method) {
            node = Y.Dom.get(node);
            if (!node) {
                YAHOO.log('getFirstChild failed: invalid node argument', 'error', 'Dom');
                return null;
            }
            return Y.Dom.getFirstChildBy(node);
        }, 

        /**
         * Returns the last HTMLElement child that passes the test method. 
         * @method getLastChildBy
         * @param {HTMLElement} node The HTMLElement to use as the starting point 
         * @param {Function} method A boolean function used to test children
         * that receives the node being tested as its only argument
         * @return {Object} HTMLElement or null if not found
         */
        getLastChildBy: function(node, method) {
            if (!node) {
                YAHOO.log('getLastChild failed: invalid node argument', 'error', 'Dom');
                return null;
            }
            var child = ( Y.Dom._testElement(node.lastChild, method) ) ? node.lastChild : null;
            return child || Y.Dom.getPreviousSiblingBy(node.lastChild, method);
        }, 

        /**
         * Returns the last HTMLElement child. 
         * @method getLastChild
         * @param {String | HTMLElement} node The HTMLElement or an ID to use as the starting point 
         * @return {Object} HTMLElement or null if not found
         */
        getLastChild: function(node) {
            node = Y.Dom.get(node);
            return Y.Dom.getLastChildBy(node);
        }, 

        /**
         * Returns an array of HTMLElement childNodes that pass the test method. 
         * @method getChildrenBy
         * @param {HTMLElement} node The HTMLElement to start from
         * @param {Function} method A boolean function used to test children
         * that receives the node being tested as its only argument
         * @return {Array} A static array of HTMLElements
         */
        getChildrenBy: function(node, method) {
            var child = Y.Dom.getFirstChildBy(node, method),
                children = child ? [child] : [];

            Y.Dom.getNextSiblingBy(child, function(node) {
                if ( !method || method(node) ) {
                    children[children.length] = node;
                }
                return false; // fail test to collect all children
            });

            return children;
        },
 
        /**
         * Returns an array of HTMLElement childNodes. 
         * @method getChildren
         * @param {String | HTMLElement} node The HTMLElement or an ID to use as the starting point 
         * @return {Array} A static array of HTMLElements
         */
        getChildren: function(node) {
            node = Y.Dom.get(node);
            if (!node) {
                YAHOO.log('getChildren failed: invalid node argument', 'error', 'Dom');
            }

            return Y.Dom.getChildrenBy(node);
        },

        /**
         * Returns the left scroll value of the document 
         * @method getDocumentScrollLeft
         * @param {HTMLDocument} document (optional) The document to get the scroll value of
         * @return {Int}  The amount that the document is scrolled to the left
         */
        getDocumentScrollLeft: function(doc) {
            doc = doc || document;
            return Math.max(doc[DOCUMENT_ELEMENT].scrollLeft, doc.body.scrollLeft);
        }, 

        /**
         * Returns the top scroll value of the document 
         * @method getDocumentScrollTop
         * @param {HTMLDocument} document (optional) The document to get the scroll value of
         * @return {Int}  The amount that the document is scrolled to the top
         */
        getDocumentScrollTop: function(doc) {
            doc = doc || document;
            return Math.max(doc[DOCUMENT_ELEMENT].scrollTop, doc.body.scrollTop);
        },

        /**
         * Inserts the new node as the previous sibling of the reference node 
         * @method insertBefore
         * @param {String | HTMLElement} newNode The node to be inserted
         * @param {String | HTMLElement} referenceNode The node to insert the new node before 
         * @return {HTMLElement} The node that was inserted (or null if insert fails) 
         */
        insertBefore: function(newNode, referenceNode) {
            newNode = Y.Dom.get(newNode); 
            referenceNode = Y.Dom.get(referenceNode); 
            
            if (!newNode || !referenceNode || !referenceNode[PARENT_NODE]) {
                YAHOO.log('insertAfter failed: missing or invalid arg(s)', 'error', 'Dom');
                return null;
            }       

            return referenceNode[PARENT_NODE].insertBefore(newNode, referenceNode); 
        },

        /**
         * Inserts the new node as the next sibling of the reference node 
         * @method insertAfter
         * @param {String | HTMLElement} newNode The node to be inserted
         * @param {String | HTMLElement} referenceNode The node to insert the new node after 
         * @return {HTMLElement} The node that was inserted (or null if insert fails) 
         */
        insertAfter: function(newNode, referenceNode) {
            newNode = Y.Dom.get(newNode); 
            referenceNode = Y.Dom.get(referenceNode); 
            
            if (!newNode || !referenceNode || !referenceNode[PARENT_NODE]) {
                YAHOO.log('insertAfter failed: missing or invalid arg(s)', 'error', 'Dom');
                return null;
            }       

            if (referenceNode.nextSibling) {
                return referenceNode[PARENT_NODE].insertBefore(newNode, referenceNode.nextSibling); 
            } else {
                return referenceNode[PARENT_NODE].appendChild(newNode);
            }
        },

        /**
         * Creates a Region based on the viewport relative to the document. 
         * @method getClientRegion
         * @return {Region} A Region object representing the viewport which accounts for document scroll
         */
        getClientRegion: function() {
            var t = Y.Dom.getDocumentScrollTop(),
                l = Y.Dom.getDocumentScrollLeft(),
                r = Y.Dom.getViewportWidth() + l,
                b = Y.Dom.getViewportHeight() + t;

            return new Y.Region(t, r, b, l);
        },

        /**
         * Provides a normalized attribute interface. 
         * @method setAttribute
         * @param {String | HTMLElement} el The target element for the attribute.
         * @param {String} attr The attribute to set.
         * @param {String} val The value of the attribute.
         */
        setAttribute: function(el, attr, val) {
            Y.Dom.batch(el, Y.Dom._setAttribute, { attr: attr, val: val });
        },

        _setAttribute: function(el, args) {
            var attr = Y.Dom._toCamel(args.attr),
                val = args.val;

            if (el && el.setAttribute) {
                attr = Y.Dom.CUSTOM_ATTRIBUTES[attr] || attr;
                el.setAttribute(attr, val);
            } else {
                YAHOO.log('setAttribute method not available for ' + el, 'error', 'Dom');
            }
        },

        /**
         * Provides a normalized attribute interface. 
         * @method getAttribute
         * @param {String | HTMLElement} el The target element for the attribute.
         * @param {String} attr The attribute to get.
         * @return {String} The current value of the attribute. 
         */
        getAttribute: function(el, attr) {
            return Y.Dom.batch(el, Y.Dom._getAttribute, attr);
        },


        _getAttribute: function(el, attr) {
            var val;
            attr = Y.Dom.CUSTOM_ATTRIBUTES[attr] || attr;

            if (el && el.getAttribute) {
                val = el.getAttribute(attr);
            } else {
                YAHOO.log('getAttribute method not available for ' + el, 'error', 'Dom');
            }

            return val;
        },

        _toCamel: function(property) {
            var c = propertyCache;

            function tU(x,l) {
                return l.toUpperCase();
            }

            return c[property] || (c[property] = property.indexOf('-') === -1 ? 
                                    property :
                                    property.replace( /-([a-z])/gi, tU ));
        },

        _getClassRegex: function(className) {
            var re;
            if (className !== undefined) { // allow empty string to pass
                if (className.exec) { // already a RegExp
                    re = className;
                } else {
                    re = reCache[className];
                    if (!re) {
                        // escape special chars (".", "[", etc.)
                        className = className.replace(Y.Dom._patterns.CLASS_RE_TOKENS, '\\$1');
                        re = reCache[className] = new RegExp(C_START + className + C_END, G);
                    }
                }
            }
            return re;
        },

        _patterns: {
            ROOT_TAG: /^body|html$/i, // body for quirks mode, html for standards,
            CLASS_RE_TOKENS: /([\.\(\)\^\$\*\+\?\|\[\]\{\}])/g
        },


        _testElement: function(node, method) {
            return node && node[NODE_TYPE] == 1 && ( !method || method(node) );
        },

        _calcBorders: function(node, xy2) {
            var t = parseInt(Y.Dom[GET_COMPUTED_STYLE](node, BORDER_TOP_WIDTH), 10) || 0,
                l = parseInt(Y.Dom[GET_COMPUTED_STYLE](node, BORDER_LEFT_WIDTH), 10) || 0;
            if (isGecko) {
                if (RE_TABLE.test(node[TAG_NAME])) {
                    t = 0;
                    l = 0;
                }
            }
            xy2[0] += l;
            xy2[1] += t;
            return xy2;
        }
    };
        
    var _getComputedStyle = Y.Dom[GET_COMPUTED_STYLE];
    // fix opera computedStyle default color unit (convert to rgb)
    if (UA.opera) {
        Y.Dom[GET_COMPUTED_STYLE] = function(node, att) {
            var val = _getComputedStyle(node, att);
            if (RE_COLOR.test(att)) {
                val = Y.Dom.Color.toRGB(val);
            }

            return val;
        };

    }

    // safari converts transparent to rgba(), others use "transparent"
    if (UA.webkit) {
        Y.Dom[GET_COMPUTED_STYLE] = function(node, att) {
            var val = _getComputedStyle(node, att);

            if (val === 'rgba(0, 0, 0, 0)') {
                val = 'transparent'; 
            }

            return val;
        };

    }
})();
/**
 * A region is a representation of an object on a grid.  It is defined
 * by the top, right, bottom, left extents, so is rectangular by default.  If 
 * other shapes are required, this class could be extended to support it.
 * @namespace YAHOO.util
 * @class Region
 * @param {Int} t the top extent
 * @param {Int} r the right extent
 * @param {Int} b the bottom extent
 * @param {Int} l the left extent
 * @constructor
 */
YAHOO.util.Region = function(t, r, b, l) {

    /**
     * The region's top extent
     * @property top
     * @type Int
     */
    this.top = t;
    
    /**
     * The region's top extent
     * @property y
     * @type Int
     */
    this.y = t;
    
    /**
     * The region's top extent as index, for symmetry with set/getXY
     * @property 1
     * @type Int
     */
    this[1] = t;

    /**
     * The region's right extent
     * @property right
     * @type int
     */
    this.right = r;

    /**
     * The region's bottom extent
     * @property bottom
     * @type Int
     */
    this.bottom = b;

    /**
     * The region's left extent
     * @property left
     * @type Int
     */
    this.left = l;
    
    /**
     * The region's left extent
     * @property x
     * @type Int
     */
    this.x = l;
    
    /**
     * The region's left extent as index, for symmetry with set/getXY
     * @property 0
     * @type Int
     */
    this[0] = l;

    /**
     * The region's total width 
     * @property width 
     * @type Int
     */
    this.width = this.right - this.left;

    /**
     * The region's total height 
     * @property height 
     * @type Int
     */
    this.height = this.bottom - this.top;
};

/**
 * Returns true if this region contains the region passed in
 * @method contains
 * @param  {Region}  region The region to evaluate
 * @return {Boolean}        True if the region is contained with this region, 
 *                          else false
 */
YAHOO.util.Region.prototype.contains = function(region) {
    return ( region.left   >= this.left   && 
             region.right  <= this.right  && 
             region.top    >= this.top    && 
             region.bottom <= this.bottom    );

    // this.logger.debug("does " + this + " contain " + region + " ... " + ret);
};

/**
 * Returns the area of the region
 * @method getArea
 * @return {Int} the region's area
 */
YAHOO.util.Region.prototype.getArea = function() {
    return ( (this.bottom - this.top) * (this.right - this.left) );
};

/**
 * Returns the region where the passed in region overlaps with this one
 * @method intersect
 * @param  {Region} region The region that intersects
 * @return {Region}        The overlap region, or null if there is no overlap
 */
YAHOO.util.Region.prototype.intersect = function(region) {
    var t = Math.max( this.top,    region.top    ),
        r = Math.min( this.right,  region.right  ),
        b = Math.min( this.bottom, region.bottom ),
        l = Math.max( this.left,   region.left   );
    
    if (b >= t && r >= l) {
        return new YAHOO.util.Region(t, r, b, l);
    } else {
        return null;
    }
};

/**
 * Returns the region representing the smallest region that can contain both
 * the passed in region and this region.
 * @method union
 * @param  {Region} region The region that to create the union with
 * @return {Region}        The union region
 */
YAHOO.util.Region.prototype.union = function(region) {
    var t = Math.min( this.top,    region.top    ),
        r = Math.max( this.right,  region.right  ),
        b = Math.max( this.bottom, region.bottom ),
        l = Math.min( this.left,   region.left   );

    return new YAHOO.util.Region(t, r, b, l);
};

/**
 * toString
 * @method toString
 * @return string the region properties
 */
YAHOO.util.Region.prototype.toString = function() {
    return ( "Region {"    +
             "top: "       + this.top    + 
             ", right: "   + this.right  + 
             ", bottom: "  + this.bottom + 
             ", left: "    + this.left   + 
             ", height: "  + this.height + 
             ", width: "    + this.width   + 
             "}" );
};

/**
 * Returns a region that is occupied by the DOM element
 * @method getRegion
 * @param  {HTMLElement} el The element
 * @return {Region}         The region that the element occupies
 * @static
 */
YAHOO.util.Region.getRegion = function(el) {
    var p = YAHOO.util.Dom.getXY(el),
        t = p[1],
        r = p[0] + el.offsetWidth,
        b = p[1] + el.offsetHeight,
        l = p[0];

    return new YAHOO.util.Region(t, r, b, l);
};

/////////////////////////////////////////////////////////////////////////////


/**
 * A point is a region that is special in that it represents a single point on 
 * the grid.
 * @namespace YAHOO.util
 * @class Point
 * @param {Int} x The X position of the point
 * @param {Int} y The Y position of the point
 * @constructor
 * @extends YAHOO.util.Region
 */
YAHOO.util.Point = function(x, y) {
   if (YAHOO.lang.isArray(x)) { // accept input from Dom.getXY, Event.getXY, etc.
      y = x[1]; // dont blow away x yet
      x = x[0];
   }
 
    YAHOO.util.Point.superclass.constructor.call(this, y, x, y, x);
};

YAHOO.extend(YAHOO.util.Point, YAHOO.util.Region);

(function() {
/**
 * Add style management functionality to DOM.
 * @module dom
 * @for Dom
 */

var Y = YAHOO.util, 
    CLIENT_TOP = 'clientTop',
    CLIENT_LEFT = 'clientLeft',
    PARENT_NODE = 'parentNode',
    RIGHT = 'right',
    HAS_LAYOUT = 'hasLayout',
    PX = 'px',
    OPACITY = 'opacity',
    AUTO = 'auto',
    BORDER_LEFT_WIDTH = 'borderLeftWidth',
    BORDER_TOP_WIDTH = 'borderTopWidth',
    BORDER_RIGHT_WIDTH = 'borderRightWidth',
    BORDER_BOTTOM_WIDTH = 'borderBottomWidth',
    VISIBLE = 'visible',
    TRANSPARENT = 'transparent',
    HEIGHT = 'height',
    WIDTH = 'width',
    STYLE = 'style',
    CURRENT_STYLE = 'currentStyle',

// IE getComputedStyle
// TODO: unit-less lineHeight (e.g. 1.22)
    re_size = /^width|height$/,
    re_unit = /^(\d[.\d]*)+(em|ex|px|gd|rem|vw|vh|vm|ch|mm|cm|in|pt|pc|deg|rad|ms|s|hz|khz|%){1}?/i,

    ComputedStyle = {
        get: function(el, property) {
            var value = '',
                current = el[CURRENT_STYLE][property];

            if (property === OPACITY) {
                value = Y.Dom.getStyle(el, OPACITY);        
            } else if (!current || (current.indexOf && current.indexOf(PX) > -1)) { // no need to convert
                value = current;
            } else if (Y.Dom.IE_COMPUTED[property]) { // use compute function
                value = Y.Dom.IE_COMPUTED[property](el, property);
            } else if (re_unit.test(current)) { // convert to pixel
                value = Y.Dom.IE.ComputedStyle.getPixel(el, property);
            } else {
                value = current;
            }

            return value;
        },

        getOffset: function(el, prop) {
            var current = el[CURRENT_STYLE][prop],                        // value of "width", "top", etc.
                capped = prop.charAt(0).toUpperCase() + prop.substr(1), // "Width", "Top", etc.
                offset = 'offset' + capped,                             // "offsetWidth", "offsetTop", etc.
                pixel = 'pixel' + capped,                               // "pixelWidth", "pixelTop", etc.
                value = '',
                actual;

            if (current == AUTO) {
                actual = el[offset]; // offsetHeight/Top etc.
                if (actual === undefined) { // likely "right" or "bottom"
                    value = 0;
                }

                value = actual;
                if (re_size.test(prop)) { // account for box model diff 
                    el[STYLE][prop] = actual; 
                    if (el[offset] > actual) {
                        // the difference is padding + border (works in Standards & Quirks modes)
                        value = actual - (el[offset] - actual);
                    }
                    el[STYLE][prop] = AUTO; // revert to auto
                }
            } else { // convert units to px
                if (!el[STYLE][pixel] && !el[STYLE][prop]) { // need to map style.width to currentStyle (no currentStyle.pixelWidth)
                    el[STYLE][prop] = current;              // no style.pixelWidth if no style.width
                }
                value = el[STYLE][pixel];
            }
            return value + PX;
        },

        getBorderWidth: function(el, property) {
            // clientHeight/Width = paddingBox (e.g. offsetWidth - borderWidth)
            // clientTop/Left = borderWidth
            var value = null;
            if (!el[CURRENT_STYLE][HAS_LAYOUT]) { // TODO: unset layout?
                el[STYLE].zoom = 1; // need layout to measure client
            }

            switch(property) {
                case BORDER_TOP_WIDTH:
                    value = el[CLIENT_TOP];
                    break;
                case BORDER_BOTTOM_WIDTH:
                    value = el.offsetHeight - el.clientHeight - el[CLIENT_TOP];
                    break;
                case BORDER_LEFT_WIDTH:
                    value = el[CLIENT_LEFT];
                    break;
                case BORDER_RIGHT_WIDTH:
                    value = el.offsetWidth - el.clientWidth - el[CLIENT_LEFT];
                    break;
            }
            return value + PX;
        },

        getPixel: function(node, att) {
            // use pixelRight to convert to px
            var val = null,
                styleRight = node[CURRENT_STYLE][RIGHT],
                current = node[CURRENT_STYLE][att];

            node[STYLE][RIGHT] = current;
            val = node[STYLE].pixelRight;
            node[STYLE][RIGHT] = styleRight; // revert

            return val + PX;
        },

        getMargin: function(node, att) {
            var val;
            if (node[CURRENT_STYLE][att] == AUTO) {
                val = 0 + PX;
            } else {
                val = Y.Dom.IE.ComputedStyle.getPixel(node, att);
            }
            return val;
        },

        getVisibility: function(node, att) {
            var current;
            while ( (current = node[CURRENT_STYLE]) && current[att] == 'inherit') { // NOTE: assignment in test
                node = node[PARENT_NODE];
            }
            return (current) ? current[att] : VISIBLE;
        },

        getColor: function(node, att) {
            return Y.Dom.Color.toRGB(node[CURRENT_STYLE][att]) || TRANSPARENT;
        },

        getBorderColor: function(node, att) {
            var current = node[CURRENT_STYLE],
                val = current[att] || current.color;
            return Y.Dom.Color.toRGB(Y.Dom.Color.toHex(val));
        }

    },

//fontSize: getPixelFont,
    IEComputed = {};

IEComputed.top = IEComputed.right = IEComputed.bottom = IEComputed.left = 
        IEComputed[WIDTH] = IEComputed[HEIGHT] = ComputedStyle.getOffset;

IEComputed.color = ComputedStyle.getColor;

IEComputed[BORDER_TOP_WIDTH] = IEComputed[BORDER_RIGHT_WIDTH] =
        IEComputed[BORDER_BOTTOM_WIDTH] = IEComputed[BORDER_LEFT_WIDTH] =
        ComputedStyle.getBorderWidth;

IEComputed.marginTop = IEComputed.marginRight = IEComputed.marginBottom =
        IEComputed.marginLeft = ComputedStyle.getMargin;

IEComputed.visibility = ComputedStyle.getVisibility;
IEComputed.borderColor = IEComputed.borderTopColor =
        IEComputed.borderRightColor = IEComputed.borderBottomColor =
        IEComputed.borderLeftColor = ComputedStyle.getBorderColor;

Y.Dom.IE_COMPUTED = IEComputed;
Y.Dom.IE_ComputedStyle = ComputedStyle;
})();
(function() {
/**
 * Add style management functionality to DOM.
 * @module dom
 * @for Dom
 */

var TO_STRING = 'toString',
    PARSE_INT = parseInt,
    RE = RegExp,
    Y = YAHOO.util;

Y.Dom.Color = {
    KEYWORDS: {
        black: '000',
        silver: 'c0c0c0',
        gray: '808080',
        white: 'fff',
        maroon: '800000',
        red: 'f00',
        purple: '800080',
        fuchsia: 'f0f',
        green: '008000',
        lime: '0f0',
        olive: '808000',
        yellow: 'ff0',
        navy: '000080',
        blue: '00f',
        teal: '008080',
        aqua: '0ff'
    },

    re_RGB: /^rgb\(([0-9]+)\s*,\s*([0-9]+)\s*,\s*([0-9]+)\)$/i,
    re_hex: /^#?([0-9A-F]{2})([0-9A-F]{2})([0-9A-F]{2})$/i,
    re_hex3: /([0-9A-F])/gi,

    toRGB: function(val) {
        if (!Y.Dom.Color.re_RGB.test(val)) {
            val = Y.Dom.Color.toHex(val);
        }

        if(Y.Dom.Color.re_hex.exec(val)) {
            val = 'rgb(' + [
                PARSE_INT(RE.$1, 16),
                PARSE_INT(RE.$2, 16),
                PARSE_INT(RE.$3, 16)
            ].join(', ') + ')';
        }
        return val;
    },

    toHex: function(val) {
        val = Y.Dom.Color.KEYWORDS[val] || val;
        if (Y.Dom.Color.re_RGB.exec(val)) {
            var r = (RE.$1.length === 1) ? '0' + RE.$1 : Number(RE.$1),
                g = (RE.$2.length === 1) ? '0' + RE.$2 : Number(RE.$2),
                b = (RE.$3.length === 1) ? '0' + RE.$3 : Number(RE.$3);

            val = [
                r[TO_STRING](16),
                g[TO_STRING](16),
                b[TO_STRING](16)
            ].join('');
        }

        if (val.length < 6) {
            val = val.replace(Y.Dom.Color.re_hex3, '$1$1');
        }

        if (val !== 'transparent' && val.indexOf('#') < 0) {
            val = '#' + val;
        }

        return val.toLowerCase();
    }
};
}());
YAHOO.register("dom", YAHOO.util.Dom, {version: "@VERSION@", build: "@BUILD@"});
