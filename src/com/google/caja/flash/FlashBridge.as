// Copyright 2010 Google Inc. All rights reserved
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
      if (!incFlashVars.__CAJA_src) {
        throw new Error("__CAJA_src missing");
      } else {
        movieRequest = new URLRequest(incFlashVars.__CAJA_src);
      }
      if (!incFlashVars.__CAJA_cajaContext) {
        throw new Error ("__CAJA_cajaContext missing");
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
    
    private function onLoaderInit(... rest):void {
      ExternalInterface.call("___.flash.onLoaderInit", cajaContext);
    }
    
    private function onLoaderError(... rest):void {
      ExternalInterface.call("___.flash.onLoaderError", cajaContext);
    }
    
    
    // FlashBridge Event Handlers
    
    private function onExternalInterfaceAddCallback(data:Object):void {
      var ex:Error;
      try {
        flash.external.ExternalInterface.addCallback(
            data.functionName,
            data.functionCallback);
        ExternalInterface.call(
            '___.flash.onAddCallback',
            cajaContext,
            data.functionName);
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
            "___.flash.onCall",
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
      try {
        flash.external.ExternalInterface.call(
            "___.flash.onNavigateToURL",
            cajaContext,
            data.url);
      } catch (e:Error) {
        ex = e;
      }
    }
  }
  
}
