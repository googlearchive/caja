// Copyright (C) 2010 Google Inc.
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

package com.google.caja.flash {
  
  import com.google.caja.flash.events.ExternalInterfaceAddCallbackEvent;
  import com.google.caja.flash.events.ExternalInterfaceCallEvent;
  import com.google.caja.flash.events.FunctionResponseEvent;
  import com.google.caja.flash.events.navigateToURLEvent;
  import flash.display.StageAlign;
  import flash.display.StageScaleMode;
  
  import flash.display.Loader;
  import flash.display.Sprite;
  import flash.events.Event;
  import flash.events.IOErrorEvent;
  import flash.events.SecurityErrorEvent;
  import flash.external.ExternalInterface;
  import flash.net.URLRequest;
  import flash.net.URLVariables;
  import flash.net.navigateToURL;
  
  /**
   * Synchronous FlashBridge Loader for CAJA
   * @author evn@google.com (Eduardo Vela)
   */
  public class FlashBridge extends Sprite {
    private var movieRequest:URLRequest;
    private var loader:Loader;
    private var cajaContext:String;
    public function FlashBridge():void {
      if (stage) { init(); }
      else { addEventListener(Event.ADDED_TO_STAGE, init); }
    }
    
    private function init(e:Event = null):void {
      removeEventListener(Event.ADDED_TO_STAGE, init);
      // Send exceptions to JS
      ExternalInterface.marshallExceptions = true;
      this.loader = new Loader();
      this.loader.contentLoaderInfo.addEventListener(
          Event.INIT,
          this.onLoaderInit);
      
      this.loader.contentLoaderInfo.addEventListener(
          IOErrorEvent.IO_ERROR,
          this.onLoaderError);
      
      this.loader.contentLoaderInfo.addEventListener(
          SecurityErrorEvent.SECURITY_ERROR,
          this.onLoaderError);
      
      // FlashBridge Events
      this.loader.contentLoaderInfo.sharedEvents.addEventListener(
          ExternalInterfaceAddCallbackEvent.ADD_CALLBACK,
          this.onExternalInterfaceAddCallback);
      
      this.loader.contentLoaderInfo.sharedEvents.addEventListener(
          ExternalInterfaceCallEvent.CALL,
          this.onExternalInterfaceCall);
      
      this.loader.contentLoaderInfo.sharedEvents.addEventListener(
          navigateToURLEvent.NAVIGATE_TO_URL,
          this.onNavigateToURL);
      
      var incFlashVars:Object = loaderInfo.parameters;
      var src:String = incFlashVars.__CAJA_src;
      if (!src) {
        throw new Error("__CAJA_src missing");
      } else {
        validateSrc(src, 'Invalid __CAJA_src');
        movieRequest = new URLRequest(src);
      }
      if (!incFlashVars.__CAJA_cajaContext) {
        throw new Error("__CAJA_cajaContext missing");
      } else {
        cajaContext = incFlashVars.__CAJA_cajaContext;
      }
      var newFlashVars:URLVariables = new URLVariables();
      for (var name:String in incFlashVars) {
        if (!name.match(/^__CAJA_/)) {
          newFlashVars[name] = incFlashVars[name];
        }
      }
      movieRequest.data = newFlashVars;
      this.loader.load(movieRequest);
      stage.align = StageAlign.TOP_LEFT;
      stage.scaleMode = StageScaleMode.NO_SCALE;
      addChild(this.loader);
    }

    private function validateSrc(src:String, errorIntro:String):void {
      var u:Object = urlParse(src, errorIntro);
      if (u.scheme !== 'http' && u.scheme !== 'https') {
        throw new Error(errorIntro + ': bad scheme');
      }

      var parent:String =
          ExternalInterface.call('window.location.href.toString');
      var v:Object = urlParse(parent, 'Invalid parent url');
      if (v.host === u.host) {
        throw new Error(errorIntro + ': Host must not be ' + u.host);
        // Sandboxing relies on allowScriptAccess=same-domain, so loading
        // the swf is unsafe if it's in the same domain.
      }
    }

    static private var urlPattern:RegExp =
      /^(\w+):\/\/([^\/:]+)/i;

    private function urlParse(s:String, errorIntro:String):Object {
      var m:Array = s.match(urlPattern);
      if (!m) {
        throw new Error(errorIntro + ': Parse error');
      }
      return {
        'scheme': m[1].toLowerCase(),
        'host': m[2].toLowerCase()
      };
    }

    private function onLoaderInit(... rest):void {
      ExternalInterface.call("caja.policy.flash.onLoaderInit", cajaContext);
    }
    
    private function onLoaderError(... rest):void {
      ExternalInterface.call("caja.policy.flash.onLoaderError", cajaContext);
    }
    
    
    // FlashBridge Event Handlers
    
    private function onExternalInterfaceAddCallback(data:Object):void {
      var ex:Error;
      try {
        // Since addCallback adds a function to the dom node for the Flash
        // object, we prepend 'caja_' to ensure we can't be tricked into
        // invoking a dom method.
        flash.external.ExternalInterface.addCallback(
            'caja_' + data.functionName,
            data.functionCallback);
        ExternalInterface.call(
            'caja.policy.flash.onAddCallback',
            cajaContext,
            'caja_' + data.functionName);
      } catch (e:Error) {
        ex = e;
      }
      this.loader.contentLoaderInfo.sharedEvents.dispatchEvent(
          new FunctionResponseEvent(
              FunctionResponseEvent.FUNCTION_RESPONSE,
              null,
              ex,
              data));
    }
    
    private function onExternalInterfaceCall(data:Object):void {
      var ex:Error;
      var res:Object;
      try {
        res = flash.external.ExternalInterface.call(
            "caja.policy.flash.onCall",
            cajaContext,
            data.functionName,
            data.functionArguments);
      } catch (e:Error) {
        ex = e;
      }
      this.loader.contentLoaderInfo.sharedEvents.dispatchEvent(
          new FunctionResponseEvent(
              FunctionResponseEvent.FUNCTION_RESPONSE,
              res,
              ex,
              data));
    }

    private function onNavigateToURL(data:Object):void {
      var ex:Error;
      var res:Object;
      try {
        res = flash.external.ExternalInterface.call(
            "caja.policy.flash.onNavigateToURL",
            cajaContext,
            data.url);
      } catch (e:Error) {
        ex = e;
      }
      this.loader.contentLoaderInfo.sharedEvents.dispatchEvent(
          new FunctionResponseEvent(
              FunctionResponseEvent.FUNCTION_RESPONSE,
              res,
              ex,
              data));
    }
  }
  
}
