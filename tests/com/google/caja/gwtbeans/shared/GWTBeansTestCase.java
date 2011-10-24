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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Date;

public abstract class GWTBeansTestCase extends GWTTestCase {

  // Set up variables in native window to emulate the parts of Caja we use.
  private native void setupCajaStub() /*-{
    // A marker used to ensure that objects have passed through the "defensible"
    // boundary. This value is not in scope for code under test, so provides
    // assurance of proper code path.
    var defensibleMarker = {};

    // Implements |makeDefensibleObject| and friends so that the returned JSO
    // allows inspection of the property descriptor, to test the internals of
    // the structure that the generated taming class creates.
    $wnd.caja = {
      iframe: {
        contentWindow: {
          ___: {
            makeDefensibleObject: function(attrs) {
              return {
                defensible: defensibleMarker,
                attrs: attrs
              };
            },
            makeDefensibleFunction: function(f) {
              return {
                defensible: defensibleMarker,
                f: f
              };
            }
          }
        }
      }
    };

    $wnd.getDef = function(jso, key) {
      if (jso.defensible !== defensibleMarker) {
        throw new Error('Jso is not defensible: ' + jso);
      }
      return jso[key];
    };

    $wnd.log = function(s) {
      @java.lang.System::err.@java.io.PrintStream::println(Ljava/lang/String;)(s);
    };
  }-*/;

  private native JavaScriptObject makeJsoDate() /*-{
    return new Date(Math.pow(2, 52));
  }-*/;

  protected Frame frame;

  protected Bean bean;
  protected Friend friend0;
  protected Friend friend1;
  protected Friend friend2;
  protected Date date;
  protected JavaScriptObject beanJso;
  protected JavaScriptObject friend0Jso;
  protected JavaScriptObject friend1Jso;
  protected JavaScriptObject friend2Jso;
  protected JavaScriptObject dateJso;

  public void gwtSetUp() throws Exception {
    setupCajaStub();
    // For our testing, there is no need to stub out any parts of the native
    // Caja |frame| object, so we just set some arbitrary non-null value.
    frame = new FrameImpl(newJso());

    bean = new Bean();
    friend0 = bean.beanRetval0;
    friend1 = bean.beanRetval1;
    friend2 = bean.beanRetval2;
    date = new Date((long) Math.pow(2, 52));
    beanJso = ((BeanTaming) GWT.create(BeanTaming.class)).getJso(frame, bean);
    friend0Jso = ((FriendTaming) GWT.create(FriendTaming.class))
        .getJso(frame, bean.beanRetval0);
    friend1Jso = ((FriendTaming) GWT.create(FriendTaming.class))
        .getJso(frame, bean.beanRetval1);
    friend2Jso = ((FriendTaming) GWT.create(FriendTaming.class))
        .getJso(frame, bean.beanRetval2);
    dateJso = makeJsoDate();
  }

  protected native JavaScriptObject getAttrsKey(
      JavaScriptObject jso,
      String key) /*-{
    return $wnd.getDef(jso, 'attrs')[key];
  }-*/;

  protected native JavaScriptObject getMethod(
      JavaScriptObject jso,
      String key) /*-{
    return $wnd.getDef($wnd.getDef(jso, 'attrs')[key].value, 'f');
  }-*/;

  protected native void callMethodVoid(
      JavaScriptObject jso,
      String key) /*-{
    $wnd.getDef($wnd.getDef(jso, 'attrs')[key].value, 'f')();
  }-*/;
  
  protected native int callMethodReturningInt(
      JavaScriptObject jso,
      String key) /*-{
    return $wnd.getDef($wnd.getDef(jso, 'attrs')[key].value, 'f')();
  }-*/;

  protected native String callMethodReturningString(
      JavaScriptObject jso,
      String key) /*-{
    return $wnd.getDef($wnd.getDef(jso, 'attrs')[key].value, 'f')();
  }-*/;
  
  protected native JavaScriptObject callMethodReturningJso(
      JavaScriptObject jso,
      String key) /*-{
    return $wnd.getDef($wnd.getDef(jso, 'attrs')[key].value, 'f')();
  }-*/;

  protected native void callMethodVoidWithArgInt(
      JavaScriptObject jso,
      String key,
      int arg) /*-{
    $wnd.getDef($wnd.getDef(jso, 'attrs')[key].value, 'f')(arg);
  }-*/;

  protected native void callMethodVoidWithArgString(
      JavaScriptObject jso,
      String key,
      String arg) /*-{
    $wnd.getDef($wnd.getDef(jso, 'attrs')[key].value, 'f')(arg);
  }-*/;
  
  protected native void callMethodVoidWithArgJso(
      JavaScriptObject jso,
      String key,
      JavaScriptObject arg) /*-{
    $wnd.getDef($wnd.getDef(jso, 'attrs')[key].value, 'f')(arg);
  }-*/;

  protected native Object getPropertyGetter(
      JavaScriptObject jso,
      String key) /*-{
    return $wnd.getDef(jso, 'attrs')[key].get;
  }-*/;

  protected native Object getPropertySetter(
      JavaScriptObject jso,
      String key) /*-{
    return $wnd.getDef(jso, 'attrs')[key].set;
  }-*/;
  
  protected native int getPropertyInt(
      JavaScriptObject jso,
      String key) /*-{
    return $wnd.getDef($wnd.getDef(jso, 'attrs')[key].get, 'f')();
  }-*/;

  protected native void setPropertyInt(
      JavaScriptObject jso,
      String key,
      int value) /*-{
    $wnd.getDef($wnd.getDef(jso, 'attrs')[key].set, 'f')(value);
  }-*/;

  protected native JavaScriptObject getPropertyBean(
      JavaScriptObject jso,
      String key) /*-{
    return $wnd.getDef($wnd.getDef(jso, 'attrs')[key].get, 'f')();
  }-*/;

  protected native Object setPropertyBean(
      JavaScriptObject jso,
      String key,
      JavaScriptObject value) /*-{
    $wnd.getDef($wnd.getDef(jso, 'attrs')[key].set, 'f')(value);
  }-*/;

  protected native static JavaScriptObject newJso() /*-{
    return {};
  }-*/;
}