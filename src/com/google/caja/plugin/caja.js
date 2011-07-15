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
 * @requires document, setTimeout, console, window
 * @provides caja
 */

var caja = (function () {
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

  function copyToImports(imports, source) {
    for (var p in source) {
      if (source.hasOwnProperty(p)) {
        // No need to use DefineOwnProperty___ since this is native code and
        // the module function created in es53.js "prepareModule" does the
        // necessary conversion.
        imports[p] = source[p];
      }
    }
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
   * @param config an object literal containing configuration paramters.
   * @param callback function to be called back with a reference to
   *     the newly created frame group.
   */
  function configure(config, callback) {
    if (!window.Object.FERAL_FRAME_OBJECT___) { initFeralFrame(window); }

    config = !config ? {} : config;
    var cajaServer = String(config.cajaServer || 'http://caja.appspot.com/');
    var debug = Boolean(config.debug);

    function loadCajaFrame(filename, callback) {
      var iframe = createIframe(filename);

      var url = joinUrl(cajaServer,
          debug ? filename + '.js' : filename + '.opt.js');

      // The particular interleaving of async events shown below has been found
      // necessary to get the right behavior on Firefox 3.6. Otherwise, the
      // iframe silently fails to invoke the cajaIframeDone___ callback.
      setTimeout(function () {
        // Arrange to be notified when the frame is ready. For Firefox, it is
        // important that we do this before we do the async installScript below.
        iframe.contentWindow.cajaIframeDone___ = function () {
          callback(iframe);
        };
        setTimeout(function() {
          installScript(iframe, url);
        }, 0);
      }, 0);
    }

    loadCajaFrame('es53-taming-frame', function (tamingFrame) {
      var tamingWindow = tamingFrame.contentWindow;
      var cajolingServiceClient =
          tamingWindow.cajolingServiceClientMaker(
              joinUrl(cajaServer, 'cajole'),
              tamingWindow.jsonRestTransportMaker(),
              true,
              config.debug);

      /**
       * Tame an object graph by applying reasonable defaults to all structures
       * reachable from a supplied root object.
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
       * Grant access to a method of a constructor such that <code>tame()</code>
       * will allow guest code to call it on instances of the constructor.
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
       * Make a new ES5 frame.
       *
       * @param div a <DIV> in the parent document within which the guest HTML's
       *     virtual document will be confined. This parameter may be undefined,
       *     in which case a secure DOM document will not be constructed.
       * @param uriPolicy a policy callback that is called to allow or
       *     disallow access each time guest code attempts to fetch from a URI.
       *     This is of the form <code>uriPolicy(uri, mimeType)</code>, where
       *     <code>uri</code> is a string URI, and <code>mimeType</code> is a
       *     string MIME type based on the context in which the URI is being
       *     requested.
       * @param callback a function that is called back when the newly
       *     constructed ES5 frame has been created.
       */
      function makeES5Frame(div, uriPolicy, callback) {
        if (div && (document !== div.ownerDocument)) {
          throw '<div> provided for ES5 frame must be in main document';
        }

        var idSuffix = 'CajaGadget-' + guestDocumentIdIndex++ + '___';

        var outerContainer;
        var innerContainer;

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

        loadCajaFrame('es53-guest-frame', function (guestFrame) {
          var guestWindow = guestFrame.contentWindow;
          var imports = {};

          var loader = guestWindow.loadModuleMaker(
              documentBaseUrl(),
              cajolingServiceClient);

          if (div) {
            // The Domita implementation is obtained from the taming window,
            // since we wish to protect Domita and its dependencies from the
            // ability of guest code to modify the shared primordials.
            tamingWindow.attachDocumentStub(
                '-' + idSuffix,
                uriPolicy,
                imports,
                innerContainer);
            imports.htmlEmitter___ =
                new tamingWindow.HtmlEmitter(innerContainer, imports.document);
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
                tamingWindow.plugin_dispatchEvent___;
            divWindow.plugin_dispatchToHandler___ = 
                function (pluginId, handler, args) {
                  return divWindow.___.tamingFrames[pluginId].
                      plugin_dispatchToHandler___(pluginId, handler, args);
                };
          }

          function runMaker(func) {
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
                            console.log('Uncaught script error: ' + message +
                                ' in source: "' + source +
                                '" at line: ' + lineNum);
                          }));
                    }
                    copyToImports(imports, extraImports);
                    func(imports, opt_callback);
                  }
              };
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
                  innerContainer.innerHTML = opt_staticHtml;
                }
                var preparedModule = guestWindow.prepareModuleFromText___(
                   cajoledJs);
                var result = preparedModule(imports);
                // If a callback is provided, we call it 
                // with the completion value.
                if (opt_callback) {
                  opt_callback(result);
                }
              };
          }

          function contentCajoled(baseUrl, cajoledJs, opt_staticHtml) {
            return runMaker(cajoledRunner(baseUrl, cajoledJs, opt_staticHtml));
          }

          function urlCajoled(baseUrl, cajoledJsUrl, opt_staticHtmlUrl) {
            return runMaker(function (imports, opt_callback) {
                // XHR get the cajoled content.
                // cajoledRunner(
                //     url,
                //     content.js,
                //     content.staticHtml)(
                //     imports,
                //     opt_callback);
                throw new Error('Not yet implemented.');
              });
          }

          function content(url, inputContent, mimeType) {
            return runMaker(function (imports, opt_callback) {
                tamingWindow.Q.when(
                    cajolingServiceClient.cajoleContent(
                        url,
                        inputContent,
                        mimeType),
                    function (moduleJson) {
                      guestWindow.Q.when(
                          loader.loadCajoledJson___(url, moduleJson),
                          function(moduleFunc) {
                            var result = moduleFunc(imports);
                            if (opt_callback) {
                              opt_callback(result);
                            }
                          },
                          function(ex) {
                            throw new Error(ex);
                          });
                    },
                    function (err) {
                      throw new Error('Error cajoling content: ' + err);
                    });
              });
          }

          function url(theUrl, contentType) {
            return runMaker(function (imports, opt_callback) {
                guestWindow.Q.when(
                    loader.async(theUrl, contentType),
                    function (moduleFunc) {
                      var result = moduleFunc(imports);
                      if (opt_callback) {
                        opt_callback(result);
                      }
                    },
                    function (err) {
                      console.log('Error in module loading: ' + err);
                    });
              });
          }

          callback({
              url: url,
              urlCajoled: urlCajoled,
              content: content,
              contentCajoled: contentCajoled,
              div: div,
              innerContainer: innerContainer,
              outerContainer: outerContainer,
              idSuffix: idSuffix,
              iframe: guestFrame,
              imports: imports,
              loader: loader
            });
        });
      }

      // A frame group
      callback({
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
      });
    });
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
        cajoledHtml: undefined
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
      
          loadCallback({
            cajoled: cajoled,
            run: run,
            code: code,
            api: api,
            div: frame.div, // TODO(jasvir): Needed?
            innerContainer: frame.innerContainer,
            outerContainer: frame.outerContainer,
            idSuffix: frame.idSuffix,
            iframe: frame.iframe,
            imports: frame.imports,
            loader: frame.loader
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
