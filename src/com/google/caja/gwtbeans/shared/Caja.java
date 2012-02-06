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

package com.google.caja.gwtbeans.shared;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class Caja {
  public static void initialize(String cajaServer) {
    initialize(cajaServer, false);
  }

  public static native void initialize(String cajaServer, boolean debug) /*-{
    $wnd.caja.initFeralFrame(window);  // note 'window' not '$wnd'
    $wnd.caja.initialize({
      cajaServer: cajaServer,
      debug: debug
    });
  }-*/;

  public static native void initialize(String cajaServer, boolean debug,
      boolean forceES5Mode) /*-{
    $wnd.caja.initialize({
      cajaServer: cajaServer,
      debug: debug,
      forceES5Mode: forceES5Mode
    });
  }-*/;

  public static void load(
      Element element,
      JavaScriptObject uriPolicy,
      final AsyncCallback<Frame> callback,
      final Map<String,String> domOpts) {
    String opt_idClass = domOpts.get("idClass");
    String opt_title = domOpts.get("title");
    loadNative(element, uriPolicy, new FrameCb() {
      @Override
      public void cb(JavaScriptObject frame) {
        callback.onSuccess(new FrameImpl(frame));
      }
    }, opt_idClass, opt_title);
    // opt_idClass at the end is awkward, but consistent with caja.js
  }

  public static native JavaScriptObject getNative() /*-{
    return $wnd.caja;
  }-*/;

  /* -- end of public interface -- */

  private static interface FrameCb {
    void cb(JavaScriptObject frame);
  }

  private static native void loadNative(
      Element element,
      JavaScriptObject uriPolicy,
      FrameCb cb,
      String opt_idClass,
      String opt_title) /*-{
    $wnd.caja.load(element, uriPolicy, function(frame) {
      cb.@com.google.caja.gwtbeans.shared.Caja.FrameCb::cb(Lcom/google/gwt/core/client/JavaScriptObject;)(frame);
    }, {
      "idClass" : opt_idClass,
      "title" : opt_title });
  }-*/;
}
