// Copyright 2010 Google Inc. All rights reserved
package com.google.caja.flash.events {
  import flash.net.URLRequest;
  /**
   * Event to handle URL navigation actions
   * @author evn@google.com (Eduardo Vela)
   */
  public class navigateToURLEvent extends FunctionCallEvent {
    public var url:URLRequest;
    public static const NAVIGATE_TO_URL:String = "onNavigateToURL";
    public function navigateToURLEvent(type:String, u:URLRequest) {
      this.url = u;
      super(type);
      return;
    }
  }
}
