// Copyright 2010 Google Inc. All rights reserved
package com.google.caja.flash.events {
  /**
   * Event to send to (and receive from) AddCallback actions
   * @author evn@google.com (Eduardo Vela)
   */
  public class ExternalInterfaceAddCallbackEvent extends FunctionCallEvent {
    public var functionName:String;
    public var functionCallback:Function;
    public static const ADD_CALLBACK:String =
        "onExternalInterfaceAddCallback";
    public function ExternalInterfaceAddCallbackEvent(
        type:String, funcName:String, funcCallback:Function) {
      this.functionName = funcName;
      this.functionCallback = funcCallback;
      super(type);
      return;
    }
  }
}
