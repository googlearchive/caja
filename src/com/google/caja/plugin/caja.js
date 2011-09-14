// Copyright (C) 2010 Google Inc.
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
 * @fileoverview ... TODO ihab.awad
 * @author kpreid@switchb.org
 * @author ihab.awad@gmail.com
 * @author jasvir@gmail.com
 * @requires document, setTimeout, window
 * @provides caja
 */

var caja = (function () {
  var cajaBuildVersion = '%VERSION%';

  var console = undefined;

  function identity(x) { return x; }
  
  function joinUrl(base, path) {
    while (base[base.length - 1] === '/') {
      base = base.slice(0, base.length - 1);
    }
    while (path[0] === '/') {
      path = path.slice(1, path.length);
    }
    return base + '/' + path;
  }

  function documentBaseUrl() {
    var bases = document.getElementsByTagName('base');
    if (bases.length == 0) {
      return document.location.toString();
    } else if (bases.length == 1) {
      var href = bases[0].href;
      if (typeof href !== 'string') {
        throw new Error('Caja loader error: <base> without a href.');
      }
      return href;
    } else {
      throw new Error('Caja loader error: document contains multiple <base>.');
    }
  }

  function createIframe(opt_className) {
    // create iframe to load Cajita runtime in
    // TODO: Put a class on this so if the host page cares about 'all iframes'
    // it can filter this out?
    var frame = document.createElement("iframe");
    // hide it
    frame.style.display = "none";
    frame.width = 0;
    frame.height = 0;
    frame.className = opt_className || '';
    // stick it arbitrarily in the document
    document.body.appendChild(frame);
    return frame;
  }

  function installScript(frame, scriptUrl) {
    // .contentDocument not IE-compatible
    var fd = frame.contentWindow.document;
    var fscript = fd.createElement('script');
    fscript.setAttribute('type', 'text/javascript');
    fscript.src = scriptUrl;
    fd.body.appendChild(fscript);
  }

  /**
   * Enforces {@code typeof specimen === typename}, in which case
   * specimen is returned.
   * <p>
   * If not, throws an informative TypeError
   * <p>
   * opt_name, if provided, should be a name or description of the
   * specimen used only to generate friendlier error messages.
   */
  function enforceType(specimen, typename, opt_name) {
    if (typeof specimen !== typename) {
      throw new TypeError('expected ' + typename + ' instead of ' +
          typeof specimen + ': ' + (opt_name || specimen));
    }
    return specimen;
  }

  var registeredImports = [];

  /**
   * Gets or assigns the id associated with this (assumed to be)
   * imports object, registering it so that
   * <tt>getImports(getId(imports)) === imports</tt>.
   * <p>
   * This system of registration and identification allows us to
   * cajole html such as
   * <pre>&lt;a onmouseover="alert(1)"&gt;Mouse here&lt;/a&gt;</pre>
   * into html-writing JavaScript such as<pre>
   * IMPORTS___.document.innerHTML = "
   *  &lt;a onmouseover=\"
   *    (function(IMPORTS___) {
   *      IMPORTS___.alert(1);
   *    })(___.getImports(" + ___.getId(IMPORTS___) + "))
   *  \"&gt;Mouse here&lt;/a&gt;
   * ";
   * </pre>
   * If this is executed by a plugin whose imports is assigned id 42,
   * it generates html with the same meaning as<pre>
   * &lt;a onmouseover="___.getImports(42).alert(1)"&gt;Mouse here&lt;/a&gt;
   * </pre>
   * <p>
   * An imports is not registered and no id is assigned to it until the
   * first call to <tt>getId</tt>. This way, an imports that is never
   * registered, or that has been <tt>unregister</tt>ed since the last
   * time it was registered, will still be garbage collectable.
   */
  function getId(imports) {
    enforceType(imports, 'object', 'imports');
    var id;
    if ('id___' in imports) {
      id = enforceType(imports.id___, 'number', 'id');
    } else {
      id = imports.id___ = registeredImports.length;
    }
    registeredImports[id] = imports;
    return id;
  }

  /**
   * Gets the imports object registered under this id.
   * <p>
   * If it has been <tt>unregistered</tt> since the last
   * <tt>getId</tt> on it, then <tt>getImports</tt> will fail.
   */
  function getImports(id) {
    var result = registeredImports[enforceType(id, 'number', 'id')];
    if (result === void 0) {
      throw new Error('Internal: imports#', id, ' unregistered');
    }
    return result;
  }

  /**
   * If you know that this <tt>imports</tt> no longer needs to be
   * accessed by <tt>getImports</tt>, then you should
   * <tt>unregister</tt> it so it can be garbage collected.
   * <p>
   * After unregister()ing, the id is not reassigned, and the imports
   * remembers its id. If asked for another <tt>getId</tt>, it
   * reregisters itself at its old id.
   */
  function unregister(imports) {
    enforceType(imports, 'object', 'imports');
    if ('id___' in imports) {
      var id = enforceType(imports.id___, 'number', 'id');
      registeredImports[id] = void 0;
    }
  }

  var guestDocumentIdIndex = 0;

  /**
   * Configure a Caja frame group. A frame group maintains a relationship with a
   * Caja server and some configuration parameters. Most Web pages will only
   * need to create one frame group.
   *
   * Recognized configuration parameters are:
   *
   *     cajaServer - whe URL to a Caja server. Except for unique cases,
   *         this must be the server from which the "caja.js" script was
   *         sourced.
   *
   *     debug - whether debugging is supported. At the moment, debug support
   *         means that the files loaded by Caja are un-minified to help with
   *         tracking down problems.
   *
   *     forceES5Mode - If set to true or false, forces (or prohibits,
   *         respectively) operation in ES5 (as opposed to ES5/3) mode, rather
   *         than autodetecting browser capabilities. This should be used
   *         strictly for debugging purposes.
   *
   * @param config an object literal containing configuration paramters.
   * @param callback function to be called back with a reference to
   *     the newly created frame group.
   */
  function configure(config, callback) {
    if (!window.Object.FERAL_FRAME_OBJECT___) { initFeralFrame(window); }

    config = !config ? {} : config;
    var cajaServer = String(config.cajaServer || 'http://caja.appspot.com/');
    var debug = Boolean(config.debug);
    var forceES5Mode = "forceES5Mode" in config
        ? Boolean(config.forceES5Mode) : undefined;

    function loadCajaFrame(filename, callback) {
      var iframe = createIframe(filename);

      var url = joinUrl(cajaServer,
          filename + '-' + cajaBuildVersion + (debug ? '.js' : '.opt.js'));

      // The particular interleaving of async events shown below has been found
      // necessary to get the right behavior on Firefox 3.6. Otherwise, the
      // iframe silently fails to invoke the cajaIframeDone___ callback.
      setTimeout(function () {
        // Arrange to be notified when the frame is ready. For Firefox, it is
        // important that we do this before we do the async installScript below.
        iframe.contentWindow.cajaIframeDone___ = function () {
          if (cajaBuildVersion !== iframe.contentWindow.cajaBuildVersion) {
            var msg = 'Version error: ' +
                'caja.js version ' + cajaBuildVersion + ' ' +
                'does not match ' +
                filename + ' version ' + iframe.contentWindow.cajaBuildVersion;
            if (console) { console.log(msg); }
            throw new Error(msg);
          }
          callback(iframe);
        };
        setTimeout(function() {
          installScript(iframe, url);
        }, 0);
      }, 0);
    }
    
    /**
     * Internal: Prepare per-guest-frame components which are common to the ES5
     * and ES5/3 modes.
     */
    function guestFrameCommon(div, tame, markFunction, executeCompiledModule) {
      var idSuffix = 'CajaGadget-' + guestDocumentIdIndex++ + '___';

      var outerContainer = undefined;
      var innerContainer = undefined;

      if (div) {
        outerContainer = div.ownerDocument.createElement('div');
        innerContainer = div.ownerDocument.createElement('div');

        outerContainer.setAttribute('class', 'caja_outerContainer___');
        innerContainer.setAttribute('class', 'caja_innerContainer___');

        innerContainer.setAttribute('title', '<Untrusted Content Title>');

        // Copy over any existing children (like static HTML produced by
        // the cajoler) into the inner container.
        while (div.childNodes[0]) {
          innerContainer.appendChild(div.childNodes[0]);
        }

        div.appendChild(outerContainer);
        outerContainer.appendChild(innerContainer);
      }
    
      function runMaker(guestCode) {
        return {
          /**
           * Run some guest code in this ES5 frame.
           *
           * @param extraImports a map of extra imports to be provided
           *     as global variables to the guest HTML.
           * @param callback a function that is called providing the
           *     completion value of the guest code.
           */
          run: function(extraImports, opt_callback) {
              if (!extraImports) {
                extraImports = {};
              }
              if (!('onerror' in extraImports)) {
                extraImports.onerror = tame(markFunction(
                    function (message, source, lineNum) {
                      if (!console) { return; }
                      console.log('Uncaught script error: ' + message +
                          ' in source: "' + source +
                          '" at line: ' + lineNum);
                    }));
              }
              executeCompiledModule(guestCode, extraImports, opt_callback);
            }
        };
      }
      
      return {
        innerContainer: innerContainer,
        outerContainer: outerContainer,
        runMaker: runMaker,
        idSuffix: idSuffix
      };
    }

    function useSESTamingFrame(sesTamingFrame) {
      var tamingWindow = sesTamingFrame.contentWindow;
      var ses = tamingWindow.ses;
      if (ses.maxSeverity.level > ses.severities.NOT_ISOLATED.level
          && !forceES5Mode) {
        // TODO(kpreid): What severity level is sufficient to protect us here?
        if (console) {
          console.warn("Failed to initialize SES; switching to ES5/3 mode.");
        }
        cannotSES();
      } else {
        // TODO(kpreid): Remove noise when this case is in production
        if (console) {
          console.info("SES taming frame loaded!");
        }

        var Q = tamingWindow.Q;

        var bridal = tamingWindow.bridalMaker(identity, document);
        /**
         * Download the content of the given URL asynchronously, and return a
         * promise for a XHR-ish record containing the response.
         *
         * TODO(kpreid): modify this interface to support streaming download
         * (readyState 3), and make use of it in loadContent.
         */
        function fetch(url) {
          // TODO(kpreid): Review this for robustness/exposing all relevant info
          var pair = Q.defer();
          var resolve = pair.resolve;
          var xhr = bridal.makeXhr();
          xhr.open('GET', url, true);
          xhr.onreadystatechange = function() {
            if (xhr.readyState === 4) {
              if (xhr.status === 200) {
                resolve({
                  contentType: xhr.getResponseHeader('Content-Type'),
                  responseText: xhr.responseText
                });
              } else {
                resolve(Q.reject(xhr.status + ' ' + xhr.statusText));
              }
            }
          };
          xhr.send(null);
          return pair.promise;
        }

        function notCajoling() {
          throw new Error('Operating in SES mode; pre-cajoled content is ' +
              'not needed and cannot be loaded.');
        }

        var stubMembrane = true;

        function notMembraned(obj) {
          if (stubMembrane) {
            return obj;
          } else {
            throw new Error('Operating in SES mode; taming membrane not ' + 
                ' implemented.');
          }
        }

        var domado = tamingWindow.Domado(null);
        tamingWindow.plugin_dispatchToHandler___ =
            domado.plugin_dispatchToHandler;

        // TODO(kpreid): Reduce code/policy duplication between the two
        // different makeES5Frame-s, insofar as possible.
        /**
         * Make a new ES5 frame.
         *
         * @param div a <DIV> in the parent document within which the guest
         *     HTML's virtual document will be confined. This parameter may be
         *     undefined, in which case a secure DOM document will not be
         *     constructed.
         * @param uriPolicy a policy callback that is called to allow or
         *     disallow access each time guest code attempts to fetch from a
         *     URI. This is of the form <code>uriPolicy(uri, mimeType)</code>,
         *     where <code>uri</code> is a string URI, and <code>mimeType</code>
         *     is a string MIME type based on the context in which the URI is
         *     being requested.
         * @param callback a function that is called back when the newly
         *     constructed ES5 frame has been created.
         */
        function makeES5Frame(div, uriPolicy, callback) {
          if (div && (document !== div.ownerDocument)) {
            throw '<div> provided for ES5 frame must be in main document';
          }

          loadCajaFrame('ses-guest-frame', function (sesGuestFrame) {
            var guestWindow = sesGuestFrame.contentWindow;

            var imports, domicile;

            function executeCompiledModuleSES(
                promise, extraImports, opt_callback) {
              // TODO(kpreid): right enumerable/own behavior?
              Object.getOwnPropertyNames(extraImports).forEach(
                  function (i) {
                Object.defineProperty(imports, i,
                    Object.getOwnPropertyDescriptor(extraImports, i));
              });
              Q.when(promise, function (compiledFunc) {
                var result = compiledFunc(imports);
                if (opt_callback) {
                  opt_callback(result);
                }
              }, function (failure) {
                if (console) {
                  console.error("Failed to load guest content: " + failure);
                }
              });
            }

            var c = guestFrameCommon(
                div, identity, identity, executeCompiledModuleSES);

            if (div) {
              // The Domita implementation is obtained from the taming window,
              // since we wish to protect Domita and its dependencies from the
              // ability of guest code to modify the shared primordials.

              // TODO(kpreid): This is probably wrong: we're replacing the feral
              // record imports with the tame constructed object 'window'.

              domicile = domado.attachDocument(
                  '-' + c.idSuffix, uriPolicy, c.innerContainer);
              imports = domicile.window;
              
              // The following code copied from the ES5/3 mode is mostly
              // commented out because the features it supports are not yet
              // available in the extremely incomplete ES5 mode. It is left in
              // as a reminder to implement the corresponding features.
              // In the ES5/SES/CES world, there is no ___ suffix to hide
              // properties, so all such things must be protected by other
              // means.
              //
              // TODO(kpreid): All of this code should disappear as the missing
              // features are implemented, but if it doesn't, remove it or check
              // for what we lost.

              // Add JavaScript globals to the DOM window object.
              guestWindow.cajaVM.copyToImports(
                  imports, guestWindow.cajaVM.sharedImports);

              void new tamingWindow.HtmlEmitter(
                  identity, c.innerContainer, domicile, guestWindow);
              //imports.rewriteUriInCss___ =
              //    domicile.rewriteUriInCss.bind(domicile);
              //imports.rewriteUriInAttribute___ =
              //    domicile.rewriteUriInAttribute.bind(domicile);
              //imports.getIdClass___ = domicile.getIdClass.bind(domicile);
              //imports.emitCss___ = domicile.emitCss.bind(domicile);

              var divWindow = div.ownerDocument.defaultView ||
                  div.ownerDocument.parentWindow;
              //divWindow.___.getId =
              //    tamingWindow.___.getId =
              //    guestWindow.___.getId = getId;
              //divWindow.___.getImports =
              //    tamingWindow.___.getImports =
              //    guestWindow.___.getImports = getImports;
              //divWindow.___.unregister =
              //    tamingWindow.___.unregister =
              //    guestWindow.___.unregister = unregister;
              //getId(imports);
              //if (!divWindow.___.tamingFrames) {
              //  divWindow.___.tamingFrames = {};
              //}
              //divWindow.___.tamingFrames[imports.id___] = tamingWindow;
              //guestWindow.plugin_dispatchEvent___ = 
              //    domado.plugin_dispatchEvent;
              //divWindow.plugin_dispatchToHandler___ = 
              //    function (pluginId, handler, args) {
              //      return divWindow.___.tamingFrames[pluginId].
              //          plugin_dispatchToHandler___(pluginId, handler, args);
              //    };
              //// TODO(felix8a): should be conditional on builderState.flash
              //var twc = tamingWindow.cajaFlash;
              //if (twc && twc.init) {
              //  twc.init(divWindow, imports, tamingWindow, domicile);
              //}
            } else {
              imports = {};
              domicile = null;
            }
            
            /* TODO(felix8a): not right for multiple guests */
            function enableFlash() {
              var twf = tamingWindow.cajaFlash;
              if (domicile && twf && twf.init) {
                twf.init(divWindow, imports, tamingWindow, domicile);
              }
            }

            /**
             * Given a promise for a fetch() response record, return a promise
             * for its Caja interpretation, a function of (extraImports).
             */
            function loadContent(contentPromise, opt_expectedContentType) {
              return Q.when(contentPromise, function (xhrRecord) {
                // TODO(kpreid): Is this safe? Does this match the cajoling
                // service's behavior? Should we reject if these two do not
                // agree?
                var contentType = opt_expectedContentType
                    || xhrRecord.contentType;
                    
                var theContent = xhrRecord.responseText;
                
                if (contentType === 'text/javascript'
                    || contentType === 'application/javascript'
                    || contentType === 'application/x-javascript'
                    || contentType === 'text/ecmascript'
                    || contentType === 'application/ecmascript'
                    || contentType === 'text/jscript') {
                  // TODO(kpreid): Make sure there's only one place (in JS)
                  // where this big list of content-type synonyms is defined.
                  
                  // TODO(kpreid): needs to return completion value unless we
                  // deprecate that feature.
                  return Q.ref(guestWindow.cajaVM.compileExpr(
                      '(function () {' + theContent + '})()'));
                  
                } else if (contentType === 'text/html') {
                  return Q.ref(function (importsAgain) {
                    // importsAgain always === imports, so ignored
                    
                    // TODO(kpreid): Make fetch() support streaming download,
                    // then use it here via repeated document.write().
                    imports.document.write(theContent);
                    domicile.signalLoaded();
                  });
                  
                } else {
                  throw new TypeError("Unimplemented content-type " +
                      contentType);
                }
              });
            }

            function content(url, theContent, contentType) {
              if (console) { console.log("CJ content called"); }
              return c.runMaker(loadContent(Q.ref({
                contentType: contentType,
                responseText: theContent
              })), executeCompiledModuleSES);
            }

            function url(theUrl, contentType) {
              return c.runMaker(loadContent(fetch(theUrl), contentType),
                                executeCompiledModuleSES);
            }

            callback({
                url: url,
                urlCajoled: notCajoling,
                content: content,
                contentCajoled: notCajoling,
                div: div,
                innerContainer: c.innerContainer,
                outerContainer: c.outerContainer,
                idSuffix: c.idSuffix,
                iframe: sesGuestFrame,
                imports: imports,
                enableFlash: enableFlash,
                domicile: domicile  // Currently exposed only for the test suite
                               // TODO(kpreid): Make it more obviously internal?
                //loader: loader
              });
          });
        }

        // A frame group (ES5 version)
        var frameGroup = {
          tame: notMembraned,
          markReadOnlyRecord: notMembraned,
          markFunction: notMembraned,
          markCtor: notMembraned,
          markXo4a: notMembraned,
          grantMethod: notMembraned,
          grantRead: notMembraned,
          grantReadWrite: notMembraned,
          iframe: sesTamingFrame,
          makeES5Frame: makeES5Frame
        };

        callback(frameGroup);
      }
    }

    function cannotSES() {
      loadCajaFrame('es53-taming-frame', function (tamingFrame) {
        var tamingWindow = tamingFrame.contentWindow;
        var cajolingServiceClient =
            tamingWindow.cajolingServiceClientMaker(
                joinUrl(cajaServer, 'cajole'),
                tamingWindow.jsonRestTransportMaker(),
                true,
                config.debug,
                console);

        /**
         * Tame an object graph by applying reasonable defaults to all
         * structures reachable from a supplied root object.
         *
         * @param o a root object.
         * @return the root object of a tamed version of the object graph.
         */
        function tame(o) {
          return tamingWindow.___.tame(o);
        }

        /**
         * Freeze an object, such that guest code cannot change the values of
         * its properties, or add or delete properties.
         *
         * @param o an object.
         */
        function markReadOnlyRecord(o) {
          return tamingWindow.___.markTameAsReadOnlyRecord(o);
        }

        /**
         * Mark a host function such that <code>tame()</code> allows guest
         * code to call it. This function will be tamed as a "pure function",
         * in other words, the <code>this</code> value supplied by the guest
         * caller will not be transmitted through the taming boundary.
         *
         * @param f a function.
         */
        function markFunction(f) {
          return tamingWindow.___.markTameAsFunction(f);
        }

        /**
         * Mark a host function such that <code>tame()</code> allows guest
         * code to call it. This function will be tamed as a constructor,
         * in other words, guest code will be able to invoke it via
         * <code>new</code>, and will see the resulting objects as being
         * <code>instanceof</code> this function.
         *
         * @param c a constructor function.
         * @param opt_super the superclass constructor function.
         * @param name the name of the constructor, provided to help improve
         *     debugging and error messages.
         */
        function markCtor(c, opt_super, name) {
          return tamingWindow.___.markTameAsCtor(c, opt_super, name);
        }

        /**
         * Mark a host function such that <code>tame()</code> allows guest
         * code to call it. This function will be tamed as an exophoric
         * function, in other words, the <code>this</code> value supplied by
         * the guest caller will be transmitted through the taming boundary.
         *
         * @param f an exophoric function.
         */
        function markXo4a(f) {
          return tamingWindow.___.markTameAsXo4a(f);
        }

        /**
         * Grant access to a method of a constructor such that
         * <code>tame()</code> will allow guest code to call it on instances of
         * the constructor.
         *
         * @param c a constructor function, which must previously have been
         *     marked via <code>markCtor()</code>.
         * @param name a method name.
         */
        function grantMethod(c, name) {
          tamingWindow.___.grantTameAsMethod(c, name);
        }

        /**
         * Grant access to a field of an object such that <code>tame</code> will
         * allow guest code to read the field. This grant is inherited along the
         * JavaScript prototype chain.
         *
         * @param o an object.
         * @pram name a property name.
         */
        function grantRead(o, name) {
          tamingWindow.___.grantTameAsRead(o, name);
        }

        /**
         * Grant access to a field of an object such that <code>tame</code> will
         * allow guest code to read and write the field. This grant is inherited
         * along the JavaScript prototype chain.
         *
         * @param o an object.
         * @pram name a property name.
         */
        function grantReadWrite(o, name) {
          tamingWindow.___.grantTameAsReadWrite(o, name);
        }
      

        /** 
         * Permit this function to be called by cajoled code without modifying
         * the arguments. This should only be used for stuff which ignores the
         * taming membrane deliberately.
         */
        function markCallableWithoutMembrane(func) {
          if (func !== undefined && !func.i___) {
            func.i___ = function () {
              // hide that this is being invoked as a method
              return Function.prototype.apply.call(func, undefined, arguments);
            };
            func.new___ = function () {
              if (arguments.length !== 0) {
                throw new TypeError("construction with args not implemented");
              } else {
                return new func();
              }
            };
            func.call_m___ = func;
            func.apply_m___ = func;
            tamingWindow.___.tamesTo(func, func);
          }
          return func;
        }
        markCallableWithoutMembrane(markCallableWithoutMembrane);
      
        /**
         * This function adds magic ES5/3-runtime properties on an object from
         * the host DOM such that it can be accessed as if it were a guest
         * object. It effectively whitelists everything.
         *
         * This completely breaks the invariants of the ES5/3 taming membrane
         * and the resulting object should under no circumstance be given to 
         * untrusted code.
         * 
         * It returns its argument, both for convenience and because bridal.js
         * is written to be adaptable to an environment where this action
         * requires wrappers. (Domado is not.)
         */
        function makeDOMAccessible(o) {
          //console.debug('makeDOMAccessible:', o);
        
          // This accepts functions because some objects are incidentally
          // functions. makeDOMAccessible does not make functions callable.
          //
          // Testing for own properties, not 'in', because some quirk of Firefox
          // makes  event objects appear as if they have the taming frame's
          // prototype after being passed into taming frame code (!), so we want
          // to be able to override Object.prototype.v___ etc. Except for that, 
          // it would be safer to not allow applying this to apparently defined-
          // in-taming-frame objects.
          if ((typeof o === 'object' || typeof o === 'function')
                  && o !== null
                  && !Object.prototype.hasOwnProperty.call(o, 'v___')) {
            o.v___ = function (p) {
              return this[p];
            };
            o.w___ = function (p, v) {
              this[p] = v;
            };
            o.m___ = function (p, as) {
              // From es53 tameObjectWithMethods without the membrane features.
              p = '' + p;
              if (('' + (+p)) !== p && !(/__$/).test(p)) {
                var method = o[p];
                if (typeof method === 'function') {
                  return method.apply(o, as);
                }
              }
              throw new TypeError('Not a function: ' + p);
            };
          
            o.HasProperty___ = function (p) { return p in this; };
          }
          return o;
        }
        markCallableWithoutMembrane(makeDOMAccessible);

        /**
         * Allow a guest constructed object (such as Domado's DOM wrappers) to
         * be passed through the taming membrane (largely uselessly) by giving
         * it a stub feral twin. This exists primarily for the test suite.
         */
        function permitUntaming(o) {
          if (typeof o === 'object' || typeof o === 'function') {
            tamingWindow.___.tamesTo(new FeralTwinStub(), o);
          } // else let primitives go normally
        }
        markCallableWithoutMembrane(permitUntaming);
        function FeralTwinStub() {}
        FeralTwinStub.prototype.toString = function () {
          return "[feral twin stub:" + tamingWindow.___.tame(this) + "]";
        };
      
        function insiderTame(f) {
          return tame(f);
        }
        markCallableWithoutMembrane(insiderTame);

        function insiderUntame(f) {
          return tamingWindow.___.untame(f);
        }
        markCallableWithoutMembrane(insiderUntame);
      
        function insiderTamesTo(f, t) {
          return tamingWindow.___.tamesTo(f, t);
        }
        markCallableWithoutMembrane(insiderTamesTo);
      
        function hasTameTwin(f) {
          return "TAMED_TWIN___" in f;
        }
        markCallableWithoutMembrane(hasTameTwin);
      
        // On Firefox 4.0.1, at least, canvas pixel arrays cannot have added
        // properties (such as our w___). Therefore to be able to write them we
        // need uncajoled code to do it. An alternative approach would be to
        // muck with the "Uint8ClampedArray" prototype.
        function writeToPixelArray(source, target, length) {
          for (var i = length-1; i >= 0; i--) {
            target[+i] = source[+i];
          }
        }
        markCallableWithoutMembrane(writeToPixelArray);
      
        var domado = tamingWindow.Domado(tame(markReadOnlyRecord({
          makeDOMAccessible: makeDOMAccessible,
          makeFunctionAccessible: markCallableWithoutMembrane,
          permitUntaming: permitUntaming,
          tame: insiderTame,
          untame: insiderUntame,
          tamesTo: insiderTamesTo,
          hasTameTwin: hasTameTwin,
          writeToPixelArray: writeToPixelArray,
          getId: markCallableWithoutMembrane(getId),
          getImports: markCallableWithoutMembrane(getImports)
        })));
        tamingWindow.plugin_dispatchToHandler___ =
            domado.plugin_dispatchToHandler;

        /**
         * Make a new ES5 frame.
         *
         * @param div a <DIV> in the parent document within which the guest
         *     HTML's virtual document will be confined. This parameter may be
         *     undefined, in which case a secure DOM document will not be 
         *     constructed.
         * @param uriPolicy a policy callback that is called to allow or
         *     disallow access each time guest code attempts to fetch from a 
         *     URI. This is of the form <code>uriPolicy(uri, mimeType)</code>,
         *     where <code>uri</code> is a string URI, and <code>mimeType</code>
         *     is a string MIME type based on the context in which the URI is
         *     being requested.
         * @param callback a function that is called back when the newly
         *     constructed ES5 frame has been created.
         */
        function makeES5Frame(div, uriPolicy, callback) {
          if (div && (document !== div.ownerDocument)) {
            throw '<div> provided for ES5 frame must be in main document';
          }
        
          // Needs to be accessible by Domado. But markFunction must be done at
          // most once, so markFunction(uriPolicy.rewrite) would only work once,
          // and having side effects on our arguments is best avoided. Another
          // option would be to require the caller to tame the uriPolicy.
          // TODO(kpreid): Revisit this implementation choice.
          var uriPolicyForTaming = markReadOnlyRecord({
            rewrite: markFunction(function () {
              return uriPolicy.rewrite.apply(uriPolicy, arguments);
            })
          });
          
          loadCajaFrame('es53-guest-frame', function (guestFrame) {
            var guestWindow = guestFrame.contentWindow;

            var loader = guestWindow.loadModuleMaker(
                documentBaseUrl(),
                cajolingServiceClient);

            var imports, domicile;
            
            function executeCompiledModuleES53(
                func, extraImports, opt_callback) {
              tamingWindow.___.copyToImports(imports, extraImports);
              func(imports, opt_callback);
            }

            var c = guestFrameCommon(
                div, tame, markFunction, executeCompiledModuleES53);

            if (div) {
              // The Domita implementation is obtained from the taming window,
              // since we wish to protect Domita and its dependencies from the
              // ability of guest code to modify the shared primordials.
            
              // TODO(kpreid): This is probably wrong: we're replacing the feral
              // record imports with the tame constructed object 'window'.
            
              domicile = domado.attachDocument(
                  '-' + c.idSuffix,
                  tame(uriPolicyForTaming),
                  c.innerContainer);
              imports = domicile.window;
            
              // Add JavaScript globals to the DOM window object.
              tamingWindow.___.copyToImports(
                  imports, guestWindow.___.sharedImports);
            
              // These ___ variables are interfaces used by cajoled code.
              imports.htmlEmitter___ = new tamingWindow.HtmlEmitter(
                  makeDOMAccessible, c.innerContainer, domicile, guestWindow);
              imports.rewriteUriInCss___ =
                  domicile.rewriteUriInCss.bind(domicile);
              imports.rewriteUriInAttribute___ =
                  domicile.rewriteUriInAttribute.bind(domicile);
              imports.getIdClass___ = domicile.getIdClass.bind(domicile);
              imports.emitCss___ = domicile.emitCss.bind(domicile);
              imports.tameNodeAsForeign___ =
                  domicile.tameNodeAsForeign.bind(domicile);
            
              var divWindow = div.ownerDocument.defaultView ||
                  div.ownerDocument.parentWindow;
              divWindow.___.getId =
                  tamingWindow.___.getId =
                  guestWindow.___.getId = getId;
              divWindow.___.getImports =
                  tamingWindow.___.getImports =
                  guestWindow.___.getImports = getImports;
              divWindow.___.unregister =
                  tamingWindow.___.unregister =
                  guestWindow.___.unregister = unregister;
              getId(imports);
              if (!divWindow.___.tamingFrames) {
                divWindow.___.tamingFrames = {};
              }
              divWindow.___.tamingFrames[imports.id___] = tamingWindow;
              guestWindow.plugin_dispatchEvent___ =
                  domado.plugin_dispatchEvent;
              divWindow.plugin_dispatchToHandler___ = 
                  function (pluginId, handler, args) {
                    return divWindow.___.tamingFrames[pluginId].
                        plugin_dispatchToHandler___(pluginId, handler, args);
                  };
            } else {
              imports = guestWindow.___.copy(guestWindow.___.sharedImports);
              domicile = null;
            }

            /* TODO(felix8a): not right for multiple guests */
            function enableFlash() {
              var twf = tamingWindow.cajaFlash;
              if (domicile && twf && twf.init) {
                twf.init(
                  divWindow, imports, tamingWindow, domicile, guestWindow);
              }
            }
          
            /**
             * Instantiate a prepared module using our imports object. This is
             * not just module(imports) because that merely adds variables
             * to the environment rather than replacing the global object.
             */
            function instModule(module) {
              return module.instantiate___(guestWindow.___, imports);
            }
            
            function cajoledRunner(baseUrl, cajoledJs, opt_staticHtml) {
              if (!div && opt_staticHtml) {
                throw new Error('Must have supplied a div in order to ' +
                  'set staticHtml.');
              }
              // TODO: How to tell the module to use baseUrl?
              // Or does that have to happen at cajoling time?
              return function(imports, opt_callback) {
                  if (opt_staticHtml) {
                    c.innerContainer.innerHTML = opt_staticHtml;
                  }
                  var preparedModule;
                  try {
                    preparedModule =
                        guestWindow.prepareModuleFromText___(cajoledJs);
                  } catch (ex) {
                    if (console) { console.log(ex); }
                    throw ex;
                  }
                  var result = instModule(preparedModule);
                  // If a callback is provided, we call it 
                  // with the completion value.
                  if (opt_callback) {
                    opt_callback(result);
                  }
                };
            }

            function contentCajoled(baseUrl, cajoledJs, opt_staticHtml) {
              return c.runMaker(
                  cajoledRunner(baseUrl, cajoledJs, opt_staticHtml),
                  executeCompiledModuleES53);
            }

            function urlCajoled(baseUrl, cajoledJsUrl, opt_staticHtmlUrl) {
              return c.runMaker(function (imports, opt_callback) {
                  // XHR get the cajoled content.
                  // cajoledRunner(
                  //     url,
                  //     content.js,
                  //     content.staticHtml)(
                  //     imports,
                  //     opt_callback);
                  throw new Error('Not yet implemented.');
                },
                executeCompiledModuleES53);
            }

            function content(url, inputContent, mimeType) {
              return c.runMaker(function (imports, opt_callback) {
                  tamingWindow.Q.when(
                      cajolingServiceClient.cajoleContent(
                          url,
                          inputContent,
                          mimeType),
                      function (moduleJson) {
                        guestWindow.Q.when(
                            loader.loadCajoledJson___(url, moduleJson),
                            function(moduleFunc) {
                              var result = instModule(moduleFunc);
                              if (opt_callback) {
                                opt_callback(result);
                              }
                            },
                            function(ex) {
                              if (console) {
                                console.log('Error in module loading: ' + ex);
                              }
                            });
                      },
                      function (err) {
                        throw new Error('Error cajoling content: ' + err);
                      });
                }, executeCompiledModuleES53);
            }

            function url(theUrl, contentType) {
              return c.runMaker(function (imports, opt_callback) {
                  guestWindow.Q.when(
                      loader.async(theUrl, contentType),
                      function (moduleFunc) {
                        var result = instModule(moduleFunc);
                        if (opt_callback) {
                          opt_callback(result);
                        }
                      },
                      function (err) {
                        if (console) {
                          console.log('Error in module loading: ' + err);
                        }
                      });
                }, executeCompiledModuleES53);
            }

            callback({
                url: url,
                urlCajoled: urlCajoled,
                content: content,
                contentCajoled: contentCajoled,
                div: div,
                innerContainer: c.innerContainer,
                outerContainer: c.outerContainer,
                idSuffix: c.idSuffix,
                iframe: guestFrame,
                imports: imports,
                enableFlash: enableFlash,
                domicile: domicile,  // Currently exposed only for testing
                               // TODO(kpreid): Make it more obviously internal?
                loader: loader
              });
          });
        }

        // A frame group (ES5/3 version)
        var frameGroup = {
          tame: tame,
          markReadOnlyRecord: markReadOnlyRecord,
          markFunction: markFunction,
          markCtor: markCtor,
          markXo4a: markXo4a,
          grantMethod: grantMethod,
          grantRead: grantRead,
          grantReadWrite: grantReadWrite,
          iframe: tamingFrame,
          makeES5Frame: makeES5Frame
        };
      
        callback(frameGroup);
      });
    }

    if (forceES5Mode === false) {
      cannotSES();
    } else {
      // This conditional should not exist, but the SES frame will currently
      // crash (not invoke the callback) if loaded on a non-ES5 browser. Also,
      // SES mode is not feature-complete.
      if (forceES5Mode === true) {
        loadCajaFrame('ses-taming-frame', useSESTamingFrame);
      } else {
        cannotSES();
      }
    }
  }

  function initFeralFrame(aWindow) {
    if (aWindow.Object.FERAL_FRAME_OBJECT___ === aWindow.Object) { return; }
    // Apply styles to current document
    var style = aWindow.document.createElement('style');
    style.setAttribute('type', 'text/css');
    style.innerHTML =
        '.caja_outerContainer___ {' +
        '  padding: 0px;' +
        '  margin: 0px;' +
        '  display: inline;' +
        '  position: relative;' +
        '  overflow: auto;' +
        '}' +
        '.caja_innerContainer___, .caja_outerContainer___ > * {' +
        '  padding: 0px;' +
        '  margin: 0px;' +
        '  height: 100%;' +
        '  position: relative;' +
        '}';
    aWindow.document.getElementsByTagName('head')[0].appendChild(style);
    // Attach safety marker to 'window' object
    aWindow.___ = {};
    // Attach recognition marker to 'Object' constructor
    aWindow.Object.FERAL_FRAME_OBJECT___ = aWindow.Object;
  }

  // Initialization state machine for this caja object
  var UNREADY = 0;
  var PENDING = 1;
  var READY = 2;
  var initializationState = UNREADY;

  // Stub which throws an error if called before caja is READY
  function errorMaker(name) {
    return function() {
      throw new Error('Calling method "' + name + '" before caja is ready');
    };
  }

  var policy = {
      net: {
        NO_NETWORK: { rewrite: function () { return null; } },
        ALL: { rewrite: function(uri) { return uri; } },
        only: function(url) {
            var whitelistedUrl = String(url);
            return {
              rewrite: function(uri) {
                var candidateUrl = String(uri);
                return whitelistedUrl === candidateUrl ? whitelistedUrl : null;
              }
            };
          }
      }
  };

  var frameGroup_;
  var callbacks_ = [];
  function initialize(config) {
    if (initializationState != UNREADY) {
      throw new Error('Caja cannot be initialized more than once');
    }
    console = config.console || window.console;
    initializationState = PENDING;
    caja.configure(config, function (frameGroup) {
      frameGroup_ = frameGroup;
      caja.tame = frameGroup_.tame;
      caja.markReadOnlyRecord = frameGroup_.markReadOnlyRecord;
      caja.markFunction = frameGroup_.markFunction;
      caja.markCtor = frameGroup_.markCtor;
      caja.markXo4a = frameGroup_.markXo4a;
      caja.grantMethod = frameGroup_.grantMethod;
      caja.grantRead = frameGroup_.grantRead;
      caja.grantReadWrite = frameGroup_.grantReadWrite;
      caja.iframe = frameGroup_.iframe;
      initializationState = READY;
      whenReady();
    });
  }

  function whenReady(cb) {
    callbacks_.push(cb);
    if (initializationState == READY) {
      for (var i = 0; i < callbacks_.length; i++) {
        var callback = callbacks_[i];
        if ("function" === typeof callback) {
          setTimeout(callback, 0);
        }
      }
      callbacks_ = [];
    }
  }

  function load(div, uriCallback, loadCallback) {
    var builderState = {
        // Dispatcher
        primaryMethod: undefined,
        
        // Authority
        api: undefined,
        
        // Content
        uri: undefined,
        mimeType: undefined,
        content: undefined,
        
        // Cache
        cajoledUri: undefined,
        cajoledJs: undefined,
        cajoledHtml: undefined,

        // Flash defaults to enabled
        flash: true
    };
    
    // User did not call initialize
    // Configure with default defaults
    if (initializationState == UNREADY) {
      initialize({});
    }

    whenReady(function() {
      frameGroup_.makeES5Frame(div, uriCallback || caja.policy.net.NO_NETWORK,
        function (frame) {
          function run(resultCallback) {
            if (!builderState.primaryMethod) {
              throw new Error('Use "code"|"cajoled" to specify content');
            }
            if (builderState.flash) {
              frame.enableFlash();
            }
            if ("content" === builderState.primaryMethod) {
              frame.content(builderState.uri, 
                  builderState.content, 
                  builderState.mimeType || 'text/html')
                  .run(builderState.api, resultCallback);
            } else if ("url" === builderState.primaryMethod) {
              frame.url(builderState.uri, builderState.mimeType || 'text/html')
                  .run(builderState.api, resultCallback);
            } else if ("contentCajoled" === builderState.primaryMethod) {
              frame.contentCajoled(builderState.uri,
                  builderState.cajoledJs,
                  builderState.cajoledHtml)
                  .run(builderState.api, resultCallback);
            } else {
              throw new Error('Internal error: Unknown embedding method');
            }
          }
          
          function cajoled(uri, js, html) {
            if (undefined != builderState.primaryMethod) {
              throw new Error('"code"|"cajoled" should be called only once');
            }
            builderState.primaryMethod = "contentCajoled";
            builderState.cajoledUri = uri;
            builderState.cajoledJs = js;
            builderState.cajoledHtml = html;
            return this;
          }
          
          function code(uri, mimeType, content) {
            if (undefined != builderState.primaryMethod) {
              throw new Error('"code"|"cajoled" should be called only once');
            }
            if (!content) {
              builderState.uri = uri;
              builderState.mimeType = mimeType;
              builderState.primaryMethod = "url";
            } else {
              builderState.uri = uri;
              builderState.mimeType = mimeType;
              builderState.content = content;
              builderState.primaryMethod = "content";
            }
            return this;
          }
      
          function api(apis) {
            builderState.api = apis;
            return this;
          }

          function flash(flag) {
            builderState.flash = !!flag;
            return this;
          }

          loadCallback({
            cajoled: cajoled,
            run: run,
            code: code,
            api: api,
            flash: flash,
            div: frame.div, // TODO(jasvir): Needed?
            innerContainer: frame.innerContainer,
            outerContainer: frame.outerContainer,
            idSuffix: frame.idSuffix,
            iframe: frame.iframe,
            imports: frame.imports,
            loader: frame.loader,
            domicile: frame.domicile // For test suite; see comments on frame
          });
        });
    });
  }
  
  // The global singleton Caja object
  return {
    configure: configure,
    initFeralFrame: initFeralFrame,
    load: load,
    policy: policy,
    initialize: initialize,
    whenReady: whenReady,
    tame: errorMaker('tame'),
    markReadOnlyRecord: errorMaker('markReadOnlyRecord'),
    markFunction: errorMaker('markFunction'),
    markCtor: errorMaker('markCtor'),
    markXo4a: errorMaker('markXo4a'),
    grantMethod: errorMaker('grantMethod'),
    grantRead: errorMaker('grantRead'),
    grantReadWrite: errorMaker('grantReadWrite'),
    iframe: errorMaker('iframe')
  };
})();
