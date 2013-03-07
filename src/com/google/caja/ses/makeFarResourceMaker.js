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
 * @fileoverview Makes a "makeFarResource" function using a given
 * serializer/unserializer pair. A makeFarResource function makes a
 * farPromise for an (assumed remote) resource for a given URL.
 *
 * //provides ses.makeFarResourceMaker
 * @author Mark S. Miller, but interim only until I examine how
 * ref_send/web_send (Tyler Close), qcomm (Kris Kowal), and BCap (Mark
 * Lentczner, Arjun Guha, Joe Politz) deal with similar issues.
 * @overrides ses
 * @requires Q, cajaVM
 * @requires UniformRequest, AnonXMLHttpRequest, XMLHttpRequest
 */

var ses;

(function() {
   "use strict";

   if (ses && !ses.ok()) { return; }

   var bind = Function.prototype.bind;
   // See
   // http://wiki.ecmascript.org/doku.php?id=conventions:safe_meta_programming
   var uncurryThis = bind.bind(bind.call);

   var applyFn = uncurryThis(bind.apply);
   var mapFn = uncurryThis([].map);

   var freeze = Object.freeze;
   var constFunc = cajaVM.constFunc;

   var XHR;
   if (typeof UniformRequest !== 'undefined') {
     // Prefer UniformRequest
     XHR = UniformRequest;
   } else if (typeof AnonXMLHttpRequest !== 'undefined') {
     // AnonXMLHttpRequest is our next preference
     XHR = AnonXMLHttpRequest;
   } else {
     // If we can find a way to turn off the sending of credentials
     // for same-origin requests even in this case, we should.
     XHR = XMLHttpRequest;
   }

   /**
    * Makes a makeFarResource function using a given
    * serializer/unserializer pair. A makeFarResource function makes a
    * farPromise for an (assumed remote) resource for a given URL.
    *
    * <p>The optional serializer, if omitted, defaults to passing
    * through undefined and simple coercion of to string of everything
    * else. The optional unserializer defaults to passing back the
    * undefined or resultText that it is given. If both are omitted,
    * the resulting maker makes text resources, that provide access to
    * the text representation of the resource named at that url.
    */
   function makeFarResourceMaker(opt_serialize, opt_unserialize) {
     var serialize = opt_serialize || function(opt_input) {
       if (opt_input === void 0) { return void 0; }
       return '' + opt_input;
     };
     var unserialize = opt_unserialize || function(opt_resultText) {
       return opt_resultText;
     };

     /**
      * Makes a farPromise for an (assumed remote) resource for a given
      * URL.
      */
     function makeFarResource(url) {
       url = '' + url;

       var nextSlot = Q.defer();

       function farDispatch(OP, args) {
         var opt_name = args[0];
         var opt_entityBody = serialize(args[1]);
         var xhr = new XHR();
         if (opt_name !== void 0) {
           // This should be a safe encoding
           url = url + '&q=' + encodeURIComponent(opt_name);
         }
         xhr.open(OP, url);

         return Q.promise(function(resolve,reject) {
           xhr.onreadystatechange = function() {
             if (this.readyState === 4) {
               // TODO(erights): On the status codes, do what mzero
               // suggests. Seek to interoperate not just with ourselves
               // but at least with ref_send, qcomm, and bcap.
               if (this.status === 200) {
                 resolve(unserialize(this.responseText));
  
            // } else if... { // What about other success statuses besides 200?
                 // And do we deal with any redirects here, such as a
                 // permanent redirect?
  
               } else if (this.status === 410) {
                 var rejected = Q.reject(new Error('Resource Gone'));
                 nextSlot.resolve(freeze({value: rejected}));
                 resolve(rejected);
  
               } else {
                 // TODO(erights): better diagnostics. Include
                 // responseText in Error?
                 reject(new Error('xhr ' + OP +
                                  ' failed with status: ' + this.status));
               }
             }
           };
           if (opt_entityBody === void 0) {
             xhr.send();
           } else {
             xhr.send(opt_entityBody);
           }
         });
       }

       return Q.makeFar(farDispatch, nextSlot.promise);
     }
     return constFunc(makeFarResource);
   }
   ses.makeFarResourceMaker = constFunc(makeFarResourceMaker);

 })();
