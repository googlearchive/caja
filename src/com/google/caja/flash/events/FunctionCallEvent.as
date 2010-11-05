// Copyright 2010 Google Inc. All rights reserved
package com.google.caja.flash.events {
  import flash.events.Event;
  /**
   * Basic Event class for function calls
   * @author evn@google.com (Eduardo Vela)
   */
  public class FunctionCallEvent extends Event {
    private static var transactionCounter:Number = 0;
    public var transaction:Number;
    public function FunctionCallEvent(type:String) {
      this.transaction = transactionCounter++;
      super(type, false, false);
    }
  }
}
