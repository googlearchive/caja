// Copyright 2010 Google Inc. All rights reserved
package com.google.caja.flash {
  /**
   * Test for the FlashBridge
   * @author evn@google.com (Eduardo Vela)
   */
  
  import com.google.caja.flash.external.ExternalInterface;
  import com.google.caja.flash.net.navigateToURL;
  import flash.display.MovieClip;
  import flash.net.navigateToURL;
  import flash.display.Sprite;
  import flash.external.ExternalInterface;
  import flash.net.URLRequest;
  import flash.system.ApplicationDomain;
  import flash.system.LoaderContext;
  import flash.system.System;
  
  public class FlashBridgeTest extends Sprite {
    public function FlashBridgeTest() {
      FlashBridgeClient.init(this);
      var errors:Array=[];
      // try external interface
      try {
        flash.external.ExternalInterface.call(
            "alert",
            "flash.external.ExternalInterface.call");
      } catch (e:Error) {
        errors.push(e.message);
      }
      try {
        com.google.caja.flash.external.ExternalInterface.call(
            "alert",
            "com.google.caja.flash.ExternalInterface.call");
      } catch (e:Error) {
        errors.push(e.message);
      }
      
      throw new Error("Errors: "+errors.join('\n'));
    }
  }
}
