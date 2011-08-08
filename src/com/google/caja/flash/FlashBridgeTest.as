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
