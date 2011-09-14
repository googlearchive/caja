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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;

/* package private */ class FrameImpl implements Frame {
  private final JavaScriptObject frame;
  private final Map<Object, JavaScriptObject> jsoByBean =
      new HashMap<Object, JavaScriptObject>();
  private final Map<JavaScriptObject, Object> beanByJso =
      new HashMap<JavaScriptObject, Object>();
  private final Map<JavaScriptObject, List<String>> classesByJso =
      new HashMap<JavaScriptObject, List<String>>();
  
  private static interface JsoCb {
    void cb(JavaScriptObject jso);
  }
  
  public FrameImpl(JavaScriptObject frame) {
    this.frame = frame;
  }

  @Override
  public native Frame cajoled(String uri, String js, String html) /*-{
    (this.@com.google.caja.demos.gwtbeans.shared.FrameImpl::frame).cajoled(uri, js, html);
    return this;
  }-*/;

  @Override
  public native Frame code(String uri, String mimeType, String content) /*-{
    (this.@com.google.caja.demos.gwtbeans.shared.FrameImpl::frame).code(uri, mimeType, content);
    return this;
  }-*/;
    
  @Override
  public Frame api(Map<String, JavaScriptObject> api) {
    JavaScriptObject apiJso = JavaScriptObject.createObject();
    for (String k : api.keySet()) {
      addToObject(apiJso, k, api.get(k));
    }
    return api(apiJso);
  }
  
  @Override
  public native Frame api(JavaScriptObject api) /*-{
    (this.@com.google.caja.demos.gwtbeans.shared.FrameImpl::frame).api(api);
    return this;
  }-*/;

  @Override
  public JavaScriptObject getNative() { return frame; }

  private static native void addToObject(JavaScriptObject o, String key, JavaScriptObject value) /*-{
    o[key] = value;
  }-*/;
    
  @Override
  public void run(final AsyncCallback<JavaScriptObject> callback) {
    runNative(new JsoCb() {
      @Override
      public void cb(JavaScriptObject jso) {
        if (callback != null) { callback.onSuccess(jso); }
      }
    });
  }

  private native void runNative(JsoCb cb) /*-{
    (this.@com.google.caja.demos.gwtbeans.shared.FrameImpl::frame)
        .run(function(result) {
          cb.@com.google.caja.demos.gwtbeans.shared.FrameImpl.JsoCb::cb(Lcom/google/gwt/core/client/JavaScriptObject;)(result);
        });
  }-*/;
  
  /* package private */ JavaScriptObject getFrame() {
    return frame;
  }
  
  /* package private */ Map<Object, JavaScriptObject> getJsoByBean() {
    return jsoByBean; 
  }

  /* package private */ Map<JavaScriptObject, Object> getBeanByJso() {
    return beanByJso;
  }
  
  /* package private */ Map<JavaScriptObject, List<String>> getClassesByJso() {
    return classesByJso;
  }
}
