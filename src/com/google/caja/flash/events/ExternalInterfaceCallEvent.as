// Copyright 2010 Google Inc. All rights reserved
package com.google.caja.flash.events {
  /**
   * Event to send to (and receive from) ExternalInterface call actions
   * @author evn@google.com (Eduardo Vela)
   */
  public class ExternalInterfaceCallEvent extends FunctionCallEvent {
    public var functionName:String;
    public var functionArguments:Array;
    public static const CALL:String = "onExternalInterfaceCall";
    public function ExternalInterfaceCallEvent(
        type:String, funcName:String, funcArguments:Array) {
      this.functionName = funcName;
      this.functionArguments = funcArguments;
      super(type);
      return;
    }
  }
}
