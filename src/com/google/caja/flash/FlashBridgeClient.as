// Copyright 2010 Google Inc. All rights reserved
package com.google.caja.flash {
  import com.google.caja.flash.events.FunctionCallEvent;
  import com.google.caja.flash.events.FunctionResponseEvent;
  import flash.display.DisplayObject;
  import flash.display.LoaderInfo;
  import flash.system.ApplicationDomain;
  
  /**
   * Client for the Synchronous Flash Bridge
   * @author evn@google.com (Eduardo Vela)
   */
  public class FlashBridgeClient {
    private static var counter:Number = 0;
    private static var responses:Object;
    private static var movieLoaderInfo:LoaderInfo;
    public static function init(movie:DisplayObject):void {
      initLoader(movie.loaderInfo);
    }
    private static function initLoader(li:LoaderInfo):void {
          try {
            responses = [];
            movieLoaderInfo = li;
            movieLoaderInfo.sharedEvents.addEventListener(
                FunctionResponseEvent.FUNCTION_RESPONSE,
                catchResponse);
          } catch (e:Error) {
            throw e;
          }
    }
    private static function catchResponse(ev:Object):void {
      responses[ev.originalEvent.transaction] = ev;
    }
    public static function emit(callEvent:FunctionCallEvent):* {
      if (!movieLoaderInfo) {
        if (ApplicationDomain.currentDomain.hasDefinition(
                "com.google.caja.flash.Rewriter")) {
          var rw:Class = ApplicationDomain.currentDomain.getDefinition(
              "com.google.caja.flash.Rewriter") as Class;
          initLoader(rw['movieLoaderInfo']);
        } else {
          throw new Error("No movieLoaderInfo nor Rewriter");
        }
      }
      movieLoaderInfo.sharedEvents.dispatchEvent(callEvent);
      if (callEvent.transaction in responses) {
        var responseEvent:Object = responses[callEvent.transaction];
        if (responseEvent.error) {
          throw responseEvent.error;
        }
        return responseEvent.response;
      }
    }
  }
}
