// Copyright (C) 2011 Google Inc.
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

package com.google.caja.demos.gwtbeans.shared;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class Caja {
  public static native void initialize(String cajaServer) /*-{
    $wnd.caja.initFeralFrame(window);  // note 'window' not '$wnd'
    $wnd.caja.initialize({
      cajaServer: cajaServer,
      debug: true
    });
  }-*/;

  public static void load(Element element, JavaScriptObject uriPolicy, final AsyncCallback<Frame> callback) {
    loadNative(element, uriPolicy, new FrameCb() {
      public void cb(JavaScriptObject frame) {
        callback.onSuccess(new FrameImpl(frame));
      }
    });
  }

  public static native JavaScriptObject getNative() /*-{
    return $wnd.caja;
  }-*/;

  /* -- end of public interface -- */
  
  private static interface FrameCb {
    void cb(JavaScriptObject frame);
  }
  
  private static native void loadNative(Element element, JavaScriptObject uriPolicy, FrameCb cb) /*-{
    $wnd.caja.load(element, uriPolicy, function(frame) {
      cb.@com.google.caja.demos.gwtbeans.shared.Caja.FrameCb::cb(Lcom/google/gwt/core/client/JavaScriptObject;)(frame);
    });
  }-*/;
}
