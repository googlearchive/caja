// Copyright (C) 2008 Google Inc.
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
 * This file combines the JSON.parse method defined by the original
 * json_sans_eval.js with the stringify method from the original
 * json2.js. Like json2.js, it defines a JSON object if one does not
 * already exist, and it initializes its parse and stringify methods
 * only if JSON does not currently have such methods (functions at
 * those property names). Additionally, if there is no
 * <tt>Date.prototype.toJSON</tt>, this file defines an ES5 compliant
 * one as well as the <tt>toJSON</tt> methods for <tt>String</tt>,
 * <tt>Number</tt>, and <tt>Boolean</tt>. The latter three are no
 * longer part of ES5, but are expected by the parts of this file
 * derived from json2.js.
 *
 * Of course, the reason this is included in the Caja distribution is
 * so that Caja can expose an ES5 compliant but Caja-safe JSON object
 * to cajoled code. Caja's wrapping of the provided JSON therefore
 * must work with either a real ES5 JSON or the one defined in this
 * file. To do so, Caja uses the replacer and reviver
 * hooks. Fortunately, ES5 and json2.js both specify that only own
 * properties of an object are stringified, and the the replacer is
 * called on the result of a <tt>toJSON</tt> call, making it possible
 * for the replacer to do its job.
 *
 * Comment from original json2.js:
 *
    http://www.JSON.org/json2.js
    2009-08-17

    Public Domain.

    NO WARRANTY EXPRESSED OR IMPLIED. USE AT YOUR OWN RISK.

    Based on json2.js from http://www.JSON.org/js.html
    but with the parse method to be provided by json_sans_eval.js

    This file creates a global JSON object containing two methods: stringify
    and parse.

        JSON.stringify(value, replacer, space)
            value       any JavaScript value, usually an object or array.

            replacer    an optional parameter that determines how object
                        values are stringified for objects. It can be a
                        function or an array of strings.

            space       an optional parameter that specifies the indentation
                        of nested structures. If it is omitted, the text will
                        be packed without extra whitespace. If it is a number,
                        it will specify the number of spaces to indent at each
                        level. If it is a string (such as '\t' or '&nbsp;'),
                        it contains the characters used to indent at each level.

            This method produces a JSON text from a JavaScript value.

            When an object value is found, if the object contains a toJSON
            method, its toJSON method will be called and the result will be
            stringified. A toJSON method does not serialize: it returns the
            value represented by the name/value pair that should be serialized,
            or undefined if nothing should be serialized. The toJSON method
            will be passed the key associated with the value, and this will be
            bound to the value

            For example, this would serialize Dates as ISO strings.

                Date.prototype.toJSON = function (key) {
                    function f(n) {
                        // Format integers to have at least two digits.
                        return n < 10 ? '0' + n : n;
                    }

                    return this.getUTCFullYear()   + '-' +
                         f(this.getUTCMonth() + 1) + '-' +
                         f(this.getUTCDate())      + 'T' +
                         f(this.getUTCHours())     + ':' +
                         f(this.getUTCMinutes())   + ':' +
                         f(this.getUTCSeconds())   + 'Z';
                };

            You can provide an optional replacer method. It will be passed the
            key and value of each member, with this bound to the containing
            object. The value that is returned from your method will be
            serialized. If your method returns undefined, then the member will
            be excluded from the serialization.

            If the replacer parameter is an array of strings, then it will be
            used to select the members to be serialized. It filters the results
            such that only members with keys listed in the replacer array are
            stringified.

            Values that do not have JSON representations, such as undefined or
            functions, will not be serialized. Such values in objects will be
            dropped; in arrays they will be replaced with null. You can use
            a replacer function to replace those with JSON values.
            JSON.stringify(undefined) returns undefined.

            The optional space parameter produces a stringification of the
            value that is filled with line breaks and indentation to make it
            easier to read.

            If the space parameter is a non-empty string, then that string will
            be used for indentation. If the space parameter is a number, then
            the indentation will be that many spaces.

            Example:

            text = JSON.stringify(['e', {pluribus: 'unum'}]);
            // text is '["e",{"pluribus":"unum"}]'


            text = JSON.stringify(['e', {pluribus: 'unum'}], null, '\t');
            // text is '[\n\t"e",\n\t{\n\t\t"pluribus": "unum"\n\t}\n]'

            text = JSON.stringify([new Date()], function (key, value) {
                return this[key] instanceof Date ?
                    'Date(' + this[key] + ')' : value;
            });
            // text is '["Date(---current time---)"]'


        JSON.parse(text, reviver)
            This method parses a JSON text to produce an object or array.
            It can throw a SyntaxError exception.

            The optional reviver parameter is a function that can filter and
            transform the results. It receives each of the keys and values,
            and its return value is used instead of the original value.
            If it returns what it received, then the structure is not modified.
            If it returns undefined then the member is deleted.

            Example:

            // Parse the text. Values that look like ISO date strings will
            // be converted to Date objects.

            myData = JSON.parse(text, function (key, value) {
                var a;
                if (typeof value === 'string') {
                    a =
/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*)?)Z$/.exec(value);
                    if (a) {
                        return new Date(Date.UTC(+a[1], +a[2] - 1, +a[3], +a[4],
                            +a[5], +a[6]));
                    }
                }
                return value;
            });

            myData = JSON.parse('["Date(09/09/2001)"]', function (key, value) {
                var d;
                if (typeof value === 'string' &&
                        value.slice(0, 5) === 'Date(' &&
                        value.slice(-1) === ')') {
                    d = new Date(value.slice(5, -1));
                    if (d) {
                        return d;
                    }
                }
                return value;
            });


    This is a reference implementation. You are free to copy, modify, or
    redistribute.

    This code should be minified before deployment.
    See http://javascript.crockford.com/jsmin.html

    USE YOUR OWN COPY. IT IS EXTREMELY UNWISE TO LOAD CODE FROM SERVERS YOU DO
    NOT CONTROL.
 *
 * Comment from original json_sans_eval.js:
 *
 * Parses a string of well-formed JSON text.
 *
 * If the input is not well-formed, then behavior is undefined, but it is
 * deterministic and is guaranteed not to modify any object other than its
 * return value.
 *
 * This does not use `eval` so is less likely to have obscure security bugs than
 * json2.js.
 * It is optimized for speed, so is much faster than json_parse.js.
 *
 * This library should be used whenever security is a concern (when JSON may
 * come from an untrusted source), speed is a concern, and erroring on malformed
 * JSON is *not* a concern.
 *
 *                      Pros                   Cons
 *                    +-----------------------+-----------------------+
 * json_sans_eval.js  | Fast, secure          | Not validating        |
 *                    +-----------------------+-----------------------+
 * json_parse.js      | Validating, secure    | Slow                  |
 *                    +-----------------------+-----------------------+
 * json2.js           | Fast, some validation | Potentially insecure  |
 *                    +-----------------------+-----------------------+
 *
 * json2.js is very fast, but potentially insecure since it calls `eval` to
 * parse JSON data, so an attacker might be able to supply strange JS that
 * looks like JSON, but that executes arbitrary javascript.
 * If you do have to use json2.js with untrusted data, make sure you keep
 * your version of json2.js up to date so that you get patches as they're
 * released.
 *
 * @param {string} json per RFC 4627
 * @param {function} opt_reviver optional function that reworks JSON objects
 *     post-parse per Chapter 15.12 of EcmaScript3.1.
 *     If supplied, the function is called with a string key, and a value.
 *     The value is the property of 'this'.  The reviver should return
 *     the value to use in its place.  So if dates were serialized as
 *     {@code { "type": "Date", "time": 1234 }}, then a reviver might look like
 *     {@code
 *     function (key, value) {
 *       if (value && typeof value === 'object' && 'Date' === value.type) {
 *         return new Date(value.time);
 *       } else {
 *         return value;
 *       }
 *     }}.
 *     If the reviver returns {@code undefined} then the property named by key
 *     will be deleted from its container.
 *     {@code this} is bound to the object containing the specified property.
 * @return {Object|Array}
 * @author Mike Samuel <mikesamuel@gmail.com>
 */

if (typeof Date.prototype.toJSON !== 'function') {
  Date.prototype.toJSON = function (key) {
    return isFinite(this.valueOf()) ?
    this.getUTCFullYear()   + '-' +
    f(this.getUTCMonth() + 1) + '-' +
    f(this.getUTCDate())      + 'T' +
    f(this.getUTCHours())     + ':' +
    f(this.getUTCMinutes())   + ':' +
    f(this.getUTCSeconds())   + 'Z' : null;
  };

  String.prototype.toJSON =
  Number.prototype.toJSON =
  Boolean.prototype.toJSON = function (key) {
    return this.valueOf();
  };
}

var json_sans_eval = (function() {

   var hop = Object.hasOwnProperty;

   ///////////////////// from json2.js //////////////////////////

   function f(n) {
     // Format integers to have at least two digits.
     return n < 10 ? '0' + n : n;
   }

   var cx = /[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
   escapable = /[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
   gap,
   indent,
   meta = {    // table of character substitutions
     '\b': '\\b',
     '\t': '\\t',
     '\n': '\\n',
     '\f': '\\f',
     '\r': '\\r',
     '"' : '\\"',
     '\\': '\\\\'
   },
   rep;


   function quote(string) {

     // If the string contains no control characters, no quote
     // characters, and no
     // backslash characters, then we can safely slap some quotes around it.
     // Otherwise we must also replace the offending characters with safe escape
     // sequences.

     escapable.lastIndex = 0;
     return escapable.test(string) ?
       '"' + string.replace(escapable, function (a) {
                              var c = meta[a];
                              return typeof c === 'string' ? c :
                                '\\u' + ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
                            }) + '"' :
     '"' + string + '"';
   }


   function str(key, holder) {

     // Produce a string from holder[key].

     var i,          // The loop counter.
     k,          // The member key.
     v,          // The member value.
     length,
     mind = gap,
     partial,
     value = holder[key];

     // If the value has a toJSON method, call it to obtain a replacement value.

     if (value && typeof value === 'object' &&
         typeof value.toJSON === 'function') {
       value = value.toJSON(key);
     }

     // If we were called with a replacer function, then call the replacer to
     // obtain a replacement value.

     if (typeof rep === 'function') {
       value = rep.call(holder, key, value);
     }

     // What happens next depends on the value's type.

     switch (typeof value) {
     case 'string':
       return quote(value);

     case 'number':

       // JSON numbers must be finite. Encode non-finite numbers as null.

       return isFinite(value) ? String(value) : 'null';

     case 'boolean':
     case 'null':

       // If the value is a boolean or null, convert it to a string. Note:
       // typeof null does not produce 'null'. The case is included here in
       // the remote chance that this gets fixed someday.

       return String(value);

       // If the type is 'object', we might be dealing with an object
       // or an array or
       // null.

     case 'object':

       // Due to a specification blunder in ECMAScript, typeof null is 'object',
       // so watch out for that case.

       if (!value) {
         return 'null';
       }

       // Make an array to hold the partial results of stringifying
       // this object value.

       gap += indent;
       partial = [];

       // Is the value an array?

       if (Object.prototype.toString.apply(value) === '[object Array]') {

         // The value is an array. Stringify every element. Use null
         // as a placeholder
         // for non-JSON values.

         length = value.length;
         for (i = 0; i < length; i += 1) {
           partial[i] = str(i, value) || 'null';
         }

         // Join all of the elements together, separated with commas,
         // and wrap them in
         // brackets.

         v = partial.length === 0 ? '[]' :
           gap ? '[\n' + gap +
           partial.join(',\n' + gap) + '\n' +
           mind + ']' :
           '[' + partial.join(',') + ']';
         gap = mind;
         return v;
       }

       // If the replacer is an array, use it to select the members to
       // be stringified.

       if (rep && typeof rep === 'object') {
         length = rep.length;
         for (i = 0; i < length; i += 1) {
           k = rep[i];
           if (typeof k === 'string') {
             v = str(k, value);
             if (v) {
               partial.push(quote(k) + (gap ? ': ' : ':') + v);
             }
           }
         }
       } else {

         // Otherwise, iterate through all of the keys in the object.

         for (k in value) {
           if (hop.call(value, k)) {
             v = str(k, value);
             if (v) {
               partial.push(quote(k) + (gap ? ': ' : ':') + v);
             }
           }
         }
       }

       // Join all of the member texts together, separated with commas,
       // and wrap them in braces.

       v = partial.length === 0 ? '{}' :
         gap ? '{\n' + gap + partial.join(',\n' + gap) + '\n' +
         mind + '}' : '{' + partial.join(',') + '}';
       gap = mind;
       return v;
     }
   }

   function stringify (value, replacer, space) {

     // The stringify method takes a value and an optional replacer,
     // and an optional space parameter, and returns a JSON
     // text. The replacer can be a function that can replace
     // values, or an array of strings that will select the keys. A
     // default replacer method can be provided. Use of the space
     // parameter can produce text that is more easily readable.

     var i;
     gap = '';
     indent = '';

     // If the space parameter is a number, make an indent string
     // containing that
     // many spaces.

     if (typeof space === 'number') {
       for (i = 0; i < space; i += 1) {
         indent += ' ';
       }

       // If the space parameter is a string, it will be used as the
       // indent string.

     } else if (typeof space === 'string') {
       indent = space;
     }

     // If there is a replacer, it must be a function or an array.
     // Otherwise, throw an error.

     rep = replacer;
     if (replacer && typeof replacer !== 'function' &&
         (typeof replacer !== 'object' ||
          typeof replacer.length !== 'number')) {
       throw new Error('json_sans_eval.stringify');
     }

     // Make a fake root object containing our value under the key of ''.
     // Return the result of stringifying the value.

     return str('', {'': value});
   }

   var number
       = '(?:-?\\b(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?\\b)';
   var oneChar = '(?:[^\\0-\\x08\\x0a-\\x1f\"\\\\]'
       + '|\\\\(?:[\"/\\\\bfnrt]|u[0-9A-Fa-f]{4}))';
   var string = '(?:\"' + oneChar + '*\")';

   // Will match a value in a well-formed JSON file.
   // If the input is not well-formed, may match strangely, but not in an unsafe
   // way.
   // Since this only matches value tokens, it does not match
   // whitespace, colons,
   // or commas.
   var significantToken = new RegExp(
       '(?:false|true|null|[\\{\\}\\[\\]]'
       + '|' + number
       + '|' + string
       + ')', 'g');

   // Matches escape sequences in a string literal
   var escapeSequence = new RegExp('\\\\(?:([^u])|u(.{4}))', 'g');

   // Decodes escape sequences in object literals
   var escapes = {
     '"': '"',
     '/': '/',
     '\\': '\\',
     'b': '\b',
     'f': '\f',
     'n': '\n',
     'r': '\r',
     't': '\t'
   };
   function unescapeOne(_, ch, hex) {
     return ch ? escapes[ch] : String.fromCharCode(parseInt(hex, 16));
   }

   // A non-falsy value that coerces to the empty string when used as a key.
   var EMPTY_STRING = new String('');
   var SLASH = '\\';

   var completeToken = new RegExp(
       '(?:false|true|null|[ \t\r\n]+|[\\{\\}\\[\\],:]'
       + '|' + number
       + '|' + string
       + '|.)', 'g');

   function blank(arr, s, e) { while (--e >= s) { arr[e] = ''; } }

   function checkSyntax(text, keyFilter) {
     var toks = ('' + text).match(completeToken);
     var i = 0, n = toks.length;
     checkArray();
     if (i < n) {
       throw new Error('Trailing tokens ' + toks.slice(i - 1).join(''));
     }
     return toks.join('');

     function checkArray() {
       while (i < n) {
         var t = toks[i++];
         switch (t) {
           case ']': return;
           case '[': checkArray(); break;
           case '{': checkObject(); break;
         }
       }
     }
     function checkObject() {
       // For the tokens    {  "a"  :  null  ,  "b" ...
       // the state is         0    1  2     3  0
       var state = 0;
       // If we need to sanitize instead of validating, uncomment:
       // var skip = 0;  // The index of the first token to skip or 0.
       while (i < n) {
         var t = toks[i++];
         switch (t.charCodeAt(0)) {
           case 0x09: case 0x0a: case 0x0d: case 0x20: continue; // space chars
           case 0x22: // "
             var len = t.length;
             if (len === 1) { throw new Error(t); }
             if (state === 0) {
               if (keyFilter && !keyFilter(
                       t.substring(1, len - 1)
                       .replace(escapeSequence, unescapeOne))) {
                 throw new Error(t);
                 // If we need to sanitize instead of validating, uncomment:
                 // skip = i - 1;
               }
             } else if (state !== 2) { throw new Error(t); }
             break;
           case 0x27: throw new Error(t);  // '
           case 0x2c: // ,
             if (state !== 3) { throw new Error(t); }
             state = 0;
             // If we need to sanitize instead of validating, uncomment:
             // if (skip) { blank(toks, skip, i); skip = 0; }
             continue;
           case 0x3a: // :
             if (state !== 1) { throw new Error(t); }
             break;
           case 0x5b:  // [
             if (state !== 2) { throw new Error(t); }
             checkArray();
             break;
           case 0x7b:  // {
             if (state !== 2) { throw new Error(t); }
             checkObject();
             break;
           case 0x7d:  // }
             // If we need to sanitize instead of validating, uncomment:
             // if (skip) { blank(toks, skip, i - 1); skip = 0; }
             return;
           default:
             if (state !== 2) { throw new Error(t); }
             break;
         }
         ++state;
       }
     }
   };

   function parse (json, opt_reviver) {
     // Split into tokens
     var toks = json.match(significantToken);
     // Construct the object to return
     var result;
     var tok = toks[0];
     if ('{' === tok) {
       result = {};
     } else if ('[' === tok) {
       result = [];
     } else {
       throw new Error(tok);
     }

     // If undefined, the key in an object key/value record to use
     // for the next
     // value parsed.
     var key;
     // Loop over remaining tokens maintaining a stack of
     // uncompleted objects and
     // arrays.
     var stack = [result];
     for (var i = 1, n = toks.length; i < n; ++i) {
       tok = toks[i];

       var cont;
       switch (tok.charCodeAt(0)) {
       default:  // sign or digit
         cont = stack[0];
         cont[key || cont.length] = +(tok);
         key = void 0;
         break;
       case 0x22:  // '"'
         tok = tok.substring(1, tok.length - 1);
         if (tok.indexOf(SLASH) !== -1) {
           tok = tok.replace(escapeSequence, unescapeOne);
         }
         cont = stack[0];
         if (!key) {
           if (cont instanceof Array) {
             key = cont.length;
           } else {
             key = tok || EMPTY_STRING;  // Use as key for next value seen.
             break;
           }
         }
         cont[key] = tok;
         key = void 0;
         break;
       case 0x5b:  // '['
         cont = stack[0];
         stack.unshift(cont[key || cont.length] = []);
         key = void 0;
         break;
       case 0x5d:  // ']'
         stack.shift();
         break;
       case 0x66:  // 'f'
         cont = stack[0];
         cont[key || cont.length] = false;
         key = void 0;
         break;
       case 0x6e:  // 'n'
         cont = stack[0];
         cont[key || cont.length] = null;
         key = void 0;
         break;
       case 0x74:  // 't'
         cont = stack[0];
         cont[key || cont.length] = true;
         key = void 0;
         break;
       case 0x7b:  // '{'
         cont = stack[0];
         stack.unshift(cont[key || cont.length] = {});
         key = void 0;
         break;
       case 0x7d:  // '}'
         stack.shift();
         break;
       }
     }
     // Fail if we've got an uncompleted object.
     if (stack.length) { throw new Error(); }

     if (opt_reviver) {
       // Based on walk as implemented in http://www.json.org/json2.js
       var walk = function (holder, key) {
         var value = holder[key];
         if (value && typeof value === 'object') {
           var toDelete = null;
           for (var k in value) {
             if (hop.call(value, k) && value !== holder) {
               // Recurse to properties first.  This has the effect of causing
               // the reviver to be called on the object graph depth-first.

               // Since 'this' is bound to the holder of the property, the
               // reviver can access sibling properties of k including ones
               // that have not yet been revived.

               // The value returned by the reviver is used in place of the
               // current value of property k.
               // If it returns undefined then the property is deleted.
               var v = walk(value, k);
               if (v !== void 0) {
                 value[k] = v;
               } else {
                 // Deleting properties inside the loop has vaguely defined
                 // semantics in ES3.
                 if (!toDelete) { toDelete = []; }
                 toDelete.push(k);
               }
             }
           }
           if (toDelete) {
             for (var i = toDelete.length; --i >= 0;) {
               delete value[toDelete[i]];
             }
           }
         }
         return opt_reviver.call(holder, key, value);
       };
       result = walk({ '': result }, '');
     }

     return result;
   }

   return {
     checkSyntax: checkSyntax,
     parse: parse,
     stringify: stringify
   };
})();

if (typeof JSON === 'undefined') { var JSON = {}; }
if (typeof JSON.stringify !== 'function') {
  JSON.stringify = json_sans_eval.stringify;
}
if (typeof JSON.parse !== 'function') {
  JSON.parse = json_sans_eval.parse;
}
