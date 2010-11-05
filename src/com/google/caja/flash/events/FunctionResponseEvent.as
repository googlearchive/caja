// Copyright 2010 Google Inc. All rights reserved
package com.google.caja.flash.events {
  import flash.events.Event;
  /**
   * Event to handle all responses from the untrusted application
   * @author evn@google.com (Eduardo Vela)
   */
  public class FunctionResponseEvent extends Event {
    public var response:*;
    public var error:Error;
    public var originalEvent:Object;
    public static const FUNCTION_RESPONSE:String = "onFunctionResponse";
    public function FunctionResponseEvent(
        type:String, res:Object, e:Error, origEv:Object) {
      this.response = res;
      this.error = e;
      this.originalEvent = origEv;
      super(type, false, false);
    }
  }
}
