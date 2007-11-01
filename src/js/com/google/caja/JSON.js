/*
    JSON.js
    2007-10-02

    Based on Doug Crockford's Public Domain
    json.js
    2007-09-27

...............................................................................

This file adds these methods to JavaScript:

        JSON.serialize(value, optFilter)

            JSON.serialize(value) does much the same job that
            value.toJSONString() is supposed to do in the original
            json.js library on normal JSON objects. However, they
            provide different hooks for having their behavior extended.

            For json.js, other objects can provide their own
            implementation of toJSONString(), in which case JSON
            serialization relies on these objects to return a correct
            JSON string. But if one object instead returns an
            unbalanced part of a JSON string and another object
            returns a compensating unbalanced string, then an outer
            toJSONString() can produce quoting confusions that invite
            XSS-like attacks. The primary purpose of JSON.js is
            to prevent such attacks.

            The design of JSON.js borrows ideas from Java's object
            serialization streams.

        JSON.unserialize(string, optFilter)

            JSON.unserialize(string, optFilter) acts like json.js's
            string.parseJSON(optFilter). This version also fixes a bug
            in the original: json.js specifies "If [the optional filter]
            returns undefined then the member is deleted." However,
            the implemenation in json.js instead defines the property to
            have the value undefined. JSON.unserialize() does indeed
            delete the property in this case.

            Bug: If the JSON expression to be unserialized contains
            the key "__proto__", this will be silently ignored on
            Firefox independent of the behavior of optFilter. json.js
            exhibits the same bug on Firefox. Whether this is a bug in
            these JSON libraries, in the Javascript spec, or in the
            Firefox implementation of Javascript is open to debate. In
            any case, this problem is unlikely to be fixed.

        JSON.defaultFilter(baseObj, key)

            A filter is a function that takes a baseObj and a key for
            indexing into that baseObj -- the name of one of its
            properties. A filter can:
              * return baseObj[key], in which case serialization
                or unserialization proceeds normally.
              * return undefined, suppressing the apparent existence
                of this property on this baseObj.
              * return something else, in which case it will be used
                instead of baseObj[key].
              * throw, terminating serialization or unserialization.

            If a filter is provided to serialize(), it is applied in
            top-down order, so traversal proceeds only into the
            results of filtering. If a filter is provided to
            unserialize(), it is applied in bottom-up order, so that
            reconstructed parts are available for building
            reconstructed wholes.

            If no optFilter argument is provided to serialize(),
            this defaultFilter is used. It is part of the API so that
            other filters can be built by composing with it. 
            This default filter will return undefined unless key is a
            string and an own-property of baseObj, or if key is a
            number and baseObj is an array. If baseObj[key]
            implements toJSON(), the default filter will return
            baseObj[key].toJSON(), enabling individual objects (such
            as dates) to offer replacements for themselves. Otherwise,
            it returns baseObj[key].

            If no optFilter argument is provided to unserialize(),
            then the result is just the literal tree of JSON objects.

        Date.toJSON()

            Returns an ISO string encoding the date. When serializing
            with the default filter, this brings about the same effect
            as json.js's Date.toJSONString().

Use your own copy. It is extremely unwise to load untrusted third
party code into your pages.  
*/


/**
 * Like the date.toJSONString() method defined in json.js, except
 * without the surrounding quotes. This should be identical to
 * Date.prototype.toISOString when that is defined, as it is in caja.js
 */
Date.prototype.toJSON = function () {
    function f(n) {
        return n < 10 ? '0' + n : n;
    }
    return (this.getUTCFullYear()     + '-' +
            f(this.getUTCMonth() + 1) + '-' +
            f(this.getUTCDate())      + 'T' +
            f(this.getUTCHours())     + ':' +
            f(this.getUTCMinutes())   + ':' +
            f(this.getUTCSeconds())   + 'Z');
};


JSON = (function () {

    function defaultFilter(baseObj, key) {
        var result;

        if (typeof key === 'string') {
            if (!Object.prototype.hasOwnProperty.call(baseObj, key)) {
                return undefined;
            }
        } else if (typeof key === 'number') {
            if (!(baseObj instanceof Array)) {
                return undefined;
            }
        } else {
            return undefined;
        }
        result = baseObj[key];
        if (result && typeof result.toJSON === 'function') {
            return result.toJSON();
        } else {
            return result;
        }
    }
    
    /** m is a table of character substitutions. */
    var m = {
        '\b': '\\b',
        '\t': '\\t',
        '\n': '\\n',
        '\f': '\\f',
        '\r': '\\r',
        '"' : '\\"',
        '\\': '\\\\'
    };

    return {
        defaultFilter: defaultFilter,

        serialize: function(value, optFilter) {
            var out = []; // array holding partial texts
            // var stack = []; // for diagnosing cycles
            var filter = optFilter || defaultFilter;

            /**
             * The internal recursive serialization function.
             */
            function serialize(value) {
                var i,j; // loop counters
                var len; // array lengths;
                var needComma = false;
                var k,v; // property key and value
                
                // stack.push(value);
                
                switch (typeof value) {
                case 'object':
                    if (value === null) {
                        out.push('null');
                        
                    } else if (value instanceof Array) {
                        len = value.length;
                        out.push('[');
                        for (i = 0; i < len; i += 1) {
                            v = filter(value, i);
                            if (v !== undefined) {
                                if (needComma) {
                                    out.push(',');
                                } else {
                                    needComma = true;
                                }
                                serialize(v);
                            }
                        }
                        out.push(']');
                        
                    } else {
                        out.push('{');
                        for (k in value) {
                            v = filter(value, k);
                            if (v !== undefined) {
                                if (needComma) {
                                    out.push(',');
                                } else {
                                    needComma = true;
                                }
                                serialize(k);
                                out.push(':');
                                serialize(v);
                            }
                        }
                        out.push('}');
                    }
                    break;
                    
                case 'string':
                    // If the string contains no control characters, no quote
                    // characters, and no backslash characters, then we can
                    // simply slap some quotes around it.  Otherwise we must
                    // also replace the offending characters with safe
                    // sequences.
                    if ((/["\\\x00-\x1f]/).test(value)) { //"])){
                        out.push('"' + 
                                 value.replace((/[\x00-\x1f\\"]/g), //"]),
                                               function (a) {
                            var c = m[a];
                            if (c) {
                                return c;
                            }
                            c = a.charCodeAt();
                            return '\\u00' + Math.floor(c / 16).toString(16) +
                                                       (c % 16).toString(16);
                        }) + '"');
                    } else {
                        out.push('"' + value + '"');
                    }
                    break;

                case 'number':
                    // JSON numbers must be finite. Encode non-finite numbers
                    // as null. 
                    out.push(isFinite(value) ? String(value) : 'null');
                    break;

                case 'boolean':
                    out.push(String(value));
                    break;

                default:
                    out.push('null');
                }
                // stack.pop();
            }

            var fakeRoot = [value];
            serialize(filter(fakeRoot, 0));
            return out.join('');
        },

        unserialize: function(str, optFilter) {

            var result;
            
            function walk(value) {
                var i,len,k,v;

                if (value && typeof value === 'object') {
                    if (value instanceof Array) {
                        len = value.length;
                        for (i = 0; i < len; i += 1) {
                            walk(value[i]);
                            v = optFilter(value, i);
                            if (v === undefined) {
                                delete value[i];
                            } else {
                                value[i] = v;
                            }
                        }
                    } else {
                        for (k in value) {
                            walk(value[k]);
                            v = optFilter(value, k);
                            if (v === undefined) {
                                delete value[k];
                            } else {
                                value[k] = v;
                            }
                        }
                    }
                }
                
            }

            if ((/^[,:{}\[\]0-9.\-+Eaeflnr-u \n\r\t]*$/).
                test(str.
                     replace((/\\./g), '@').
                     replace((/"[^"\\\n\r]*"/g), ''))) { //"))) {
                result = eval('(' + str + ')');
                if (optFilter) {
                    var fakeRoot = [result];
                    walk(fakeRoot);
                    return fakeRoot[0];
                } else {
                    return result;
                }
            }
            throw new SyntaxError('parseJSON');
        }
    };
})();
