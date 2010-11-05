// Copyright 2010 Google Inc. All rights reserved
package com.google.caja.flash.net {
  import com.google.caja.flash.FlashBridgeClient;
  import com.google.caja.flash.events.navigateToURLEvent;
  import com.google.caja.flash.events.FunctionCallEvent;
  import flash.net.URLRequest;
  
  public function navigateToURL (u:URLRequest):void {
    var callEvent:FunctionCallEvent = new navigateToURLEvent(
    	navigateToURLEvent.NAVIGATE_TO_URL,
    	u);
    FlashBridgeClient.emit(callEvent);
  }
}
