// Copyright (C) 2009 Google Inc.
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
 * Tamed apis that are exposed to Caja playground.
 *
 * @author Jasvir Nagra (jasvir@gmail.com)
 * @requires document, window
 * @requires swfobject
 * @requires ___, attachDocumentStub, cajita
 * @provides caja___
 */
var caja___ = (function () {
    var cajaDomSuffix = 'g___';

  // Simple flash taming that does not allow script access to the page
  function tameSimpleFlash(imports) {
    imports.outers.swfobject = {};
    imports.outers.swfobject.embedSWF = function(swfUrl, id, width, height, 
        version, expressInstall, flashvars, params, attributes, cb) {
      var tameSwfUrl = !/^https?:\/\//i.test(swfUrl) ? null : swfUrl;
      var tameId = id + '-' + cajaDomSuffix;
      var tameWidth = +width;
      var tameHeight = +height;
      // Default to 9.0 if unspecified or specified < 9
      // else use whatever variant of version 9 the user suggests
      var tameVersion = version || "9.0";
      if (!/^9|([1-9][0-9])\./.test(tameVersion)) {
        tameVersion = "9.0";
      }
      var tameExpressInstall = false;
      var tameParams = { "allowScriptAccess" : "never", 
                         "allowNetworking" : "internal"};
      // TODO(jasvir): rewrite attributes
      var tameAttr = null;
      swfobject.embedSWF(tameSwfUrl, tameId, tameWidth, tameHeight, tameVersion,
          tameExpressInstall, flashvars, tameParams, tameAttr, ___.untame(cb));
    };
    ___.grantRead(imports.outers, 'swfobject');
    ___.grantFunc(imports.outers.swfobject, 'embedSWF');
  }

  // Advanced flash taming that does allows a flash object attentuated access to the page
  function tameBridgedFlash(imports) {
    imports.outers.gadgets = {};
    imports.outers.gadgets.flash = {};
    imports.outers.gadgets.flash.embedFlash = (function () {
      var cleanse = (function () {
        // Gets a fresh Array and Object constructor that 
        // doesn't have the caja properties on it.  This is 
        // important for passing objects across the boundary 
        // to flash code.
        var ifr = document.createElement("iframe");
        ifr.width = 1; ifr.height = 1; ifr.border = 0;
        document.body.appendChild(ifr);
        var A = ifr.contentWindow.Array;
        var O = ifr.contentWindow.Object;
        document.body.removeChild(ifr);
      
        var c = function(obj) {
          var t = typeof obj, i;
          if (t === 'number' || t === 'boolean' || t === 'string') { 
              return obj; 
          }
          if (t === 'object') {
            var o;
            if (obj instanceof Array) { o = new A; }
            else if (obj instanceof Object) { o = new O; }
            for (i in obj) {
              if (/__$/.test(i)) { continue; }
              o[i] = c(obj[i]);
            }
            return o;
          }
          return (void 0);
        };
        return c;
      })();
  
      return ___.frozenFunc(function tamedEmbedFlash(
             swfUrl, 
             swfContainer,
             swfVersion, 
             opt_params) {
        // Check that swfContainer is a wrapped node
        if (typeof swfContainer === "string") {
          // This assumes that there's only one gadget in the frame.
          var $v = ___.getNewModuleHandler().getImports().$v;
          swfContainer = $v.cm(
              $v.ro("document"), 
              "getElementById", 
              [swfContainer]);
        } else if (typeof swfContainer !== "object" || !swfContainer.node___) {
          return false;
        }
  
        // Generate a random number for use as the channel name
        // for communication between the bridge and the contained
        // flash object.
        // TODO: Use true randomness.
        var channel = "_flash" + ("" + Math.random()).substring(2);
  
        // Strip out allowNetworking and allowScriptAccess, 
        //   as well as any caja-specific properties.
        var new_params = {};
        for (i in opt_params) {
          if (i.match(/___$/)) { continue; }
          var ilc = i.toLowerCase();
          if (ilc === "allownetworking" || ilc === "allowscriptaccess") {
            continue;
          }
          var topi = typeof opt_params[i];
          if (topi !== "string" && topi !== "number") { continue; }
          new_params[i] = opt_params[i];
        }
        new_params.allowNetworking = "never";
        new_params.allowScriptAccess = "none";
        if (!new_params.flashVars) { new_params.flashVars = ""; }
        new_params.flashVars += "&channel=" + channel;
  
        // Load the flash.
        gadgets.flash.embedFlash(swfUrl, swfContainer.node___, 10, new_params);
  
        if (bridge___.channels) {
          // If the bridge hasn't loaded, queue up the channel names
          // for later registration
          bridge___.channels.push(channel);
        } else {
          // Otherwise, register the channel immediately.
          bridge___.registerChannel(channel);
        }
  
        // Return the ability to talk to the boxed swf.
        return ___.primFreeze({
          callSWF: (function (channel) { 
            return ___.func(function (methodName, argv) {
                return bridge___.callSWF(
                    "" + channel, 
                    "" + methodName, 
                    cleanse(argv));
              });
          })(channel)
        });
      });
    })());
  
    var d = document.createElement('div');
    d.appendChild(document.createTextNode("bridge"));
    document.body.appendChild(d);
    
    gadgets.flash.embedFlash(
        "Bridge.swf", 
        d,
        10,
        {
          allowNetworking: "always",
          allowScriptAccess: "all",
          width: 0,
          height: 0,
          flashvars: "logging=true"
        });
    bridge___ = d.childNodes[0];
    bridge___.channels = [];
    
    callJS = function (functionName, argv) {
      // This assumes that there's a single gadget in the frame.
      var $v = ___.getNewModuleHandler().getImports().$v;
      return $v.cf($v.ro(functionName), [argv]);
    };
        
    onFlashBridgeReady = function () {
      var len = bridge___.channels.length;
      for(var i = 0; i < len; ++i) {
        bridge___.registerChannel(bridge___.channels[i]);
      }
      delete bridge___.channels;
      var outers = ___.getNewModuleHandler().getImports().$v.getOuters();
      if (outers.onFlashBridgeReady) {
        callJS("onFlashBridgeReady");
      }
    };
  
  }

  function tameFlash(imports) {
    tameSimpleFlash();
    tameBridgedFlash();
  }


  function tameAlert(imports) {
    imports.outers.alert = (function() {
      var remainingAlerts = 10;
      var useConsole = false;
      function tameAlert(msg) {
        if (useConsole) {
          cajita.log(msg);
        } else {
          if (remainingAlerts > 0) {
    	    remainingAlerts--;
    	    alert(msg);
    	  } else {
    	    if (confirm("Redirect remaining alerts to console?")) {
    	      useConsole = true;
    	    } else {
    	      remainingAlerts = 10;
    	    }
    	  }
        }
      };
      return tameAlert;
    })();
    ___.grantFunc(imports.outers, 'alert');
  }
  
  function enable() {
    var imports = ___.copy(___.sharedImports);
    imports.outers = imports;
    var uriCallback = {
      rewrite: function (uri, mimeType) {
          if (!/^https?:\/\//i.test(uri)) { return null; }
          if (/^image[/]/.test(mimeType)) { return uri; }
          return ('http://caja.appspot.com/cajole?url='
              + encodeURIComponent(uri)
              + '&mimeType=' + encodeURIComponent(mimeType));
      }
    };

    var gadgetRoot = document.getElementById('cajoled-output');
    gadgetRoot.className = cajaDomSuffix;
    
    imports.htmlEmitter___ = new HtmlEmitter(gadgetRoot);
    attachDocumentStub('-' + cajaDomSuffix, uriCallback, imports, gadgetRoot);
    imports.$v = valijaMaker.CALL___(imports.outers);
    ___.getNewModuleHandler().setImports(imports);
    
    tameAlert(imports);
    tameFlash(imports);
  }

  return {
    enable: enable  
  };
})();
