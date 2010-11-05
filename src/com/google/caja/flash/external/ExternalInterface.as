// Copyright 2010 Google Inc. All rights reserved
package com.google.caja.flash.external {
  /**
   * ExternalInterface replacement for the CAJA bridge
   * @author evn@google.com (Eduardo Vela)
   */
  import com.google.caja.flash.FlashBridgeClient;
  import com.google.caja.flash.events.ExternalInterfaceAddCallbackEvent;
  import com.google.caja.flash.events.ExternalInterfaceCallEvent;
  import com.google.caja.flash.events.FunctionCallEvent;
  import flash.external.ExternalInterface;


  public class ExternalInterface {
    public static function call(functionName:String, ... rest):* {
      var callEvent:FunctionCallEvent =
          new ExternalInterfaceCallEvent(
              ExternalInterfaceCallEvent.CALL,
              functionName,
              rest);

      return FlashBridgeClient.emit(callEvent);
    }
    public static function addCallback(
        functionName:String, functionClosure:Function):void {
      var callEvent:FunctionCallEvent =
          new ExternalInterfaceAddCallbackEvent(
              ExternalInterfaceAddCallbackEvent.ADD_CALLBACK,
              functionName,
              functionClosure);

      FlashBridgeClient.emit(callEvent);
    }
    public static function get available():Boolean {
      return flash.external.ExternalInterface.available;
    }
    public static function get objectID():String {
      return flash.external.ExternalInterface.objectID;
    }
    public static function get marshallExceptions():Boolean {
      return flash.external.ExternalInterface.marshallExceptions;
    }
    public static function set marshallExceptions(n:Boolean):void {
      flash.external.ExternalInterface.marshallExceptions = n;
    }
  }
}
