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

import com.google.gwt.core.client.JavaScriptObject;

import java.util.Date;

public class SimpleTest extends GWTBeansTestCase {
  public void testCorrectAttributes() {
    // Selected method and property names should be present
    assertNotNull(getAttrsKey(beanJso, "_getValue"));
    assertNotNull(getAttrsKey(beanJso, "invoke"));
    assertNotNull(getAttrsKey(beanJso, "fetchPrimitive"));
    assertNotNull(getAttrsKey(beanJso, "fetchBean"));
    assertNotNull(getAttrsKey(beanJso, "invokeWithPrimitive"));
    assertNotNull(getAttrsKey(beanJso, "invokeWithBean"));
    assertNotNull(getAttrsKey(beanJso, "primitive"));
    assertNotNull(getAttrsKey(beanJso, "primitiveRO"));
    assertNotNull(getAttrsKey(beanJso, "primitiveWO"));
    assertNotNull(getAttrsKey(beanJso, "bean"));

    // Getters and setters of properties should not be individually present
    assertNull(getAttrsKey(beanJso, "getPrimitive"));
    assertNull(getAttrsKey(beanJso, "setPrimitive"));
    assertNull(getAttrsKey(beanJso, "getPrimitiveRO"));
    assertNull(getAttrsKey(beanJso, "setPrimitiveWO"));
    assertNull(getAttrsKey(beanJso, "getBean"));
    assertNull(getAttrsKey(beanJso, "setBean"));

    // No evidence of any non-public methods should be present
    assertNull(getAttrsKey(beanJso, "doProtected"));
    assertNull(getAttrsKey(beanJso, "doPrivate"));
    assertNull(getAttrsKey(beanJso, "doPackagePrivate"));
    assertNull(getAttrsKey(beanJso, "protected"));
    assertNull(getAttrsKey(beanJso, "getProtected"));
    assertNull(getAttrsKey(beanJso, "setProtected"));
    assertNull(getAttrsKey(beanJso, "private"));
    assertNull(getAttrsKey(beanJso, "getPrivate"));
    assertNull(getAttrsKey(beanJso, "setPrivate"));
    assertNull(getAttrsKey(beanJso, "packagePrivate"));
    assertNull(getAttrsKey(beanJso, "getPackagePrivate"));
    assertNull(getAttrsKey(beanJso, "setPackagePrivate"));

    // No evidence of fields should be present
    assertNull(getAttrsKey(beanJso, "testPublicField"));
    assertNull(getAttrsKey(beanJso, "testProtectedField"));
    assertNull(getAttrsKey(beanJso, "testPrivateField"));
    assertNull(getAttrsKey(beanJso, "testPackagePrivateField"));
  }

  public void testFunctionReturningVoid() throws Exception {
    callMethodVoid(beanJso, "invoke");
    assertTrue(bean.invoked);
  }

  public void testMethodNameWithUnderscore() throws Exception {
    callMethodVoid(beanJso, "_getValue");
    assertTrue(bean.invoked);
  }

  public void testMethodReturningPrimitive() {
    int o = callMethodReturningInt(beanJso, "fetchPrimitive");
    assertTrue(bean.invoked);
    assertEquals(bean.primitiveRetval0, o);
  }

  public void testMethodReturningBean() {
    JavaScriptObject o = callMethodReturningJso(beanJso, "fetchBean");
    assertTrue(bean.invoked);
    assertSame(friend0Jso, o);
  }

  public void testMethodAcceptingPrimitive() {
    callMethodVoidWithArgInt(beanJso, "invokeWithPrimitive", 42);
    assertTrue(bean.invoked);
    assertEquals(42, bean.arg0);  // autoboxed
  }

  public void testMethodAcceptingBean() {
    callMethodVoidWithArgJso(beanJso, "invokeWithBean", friend0Jso);
    assertTrue(bean.invoked);
    // Argument should have been unwrapped
    assertSame(friend0, bean.arg0);
  }

  public void testPropertyPrimitiveGet() {
    assertNotNull(getPropertyGetter(beanJso, "primitive"));
    int o = getPropertyInt(beanJso, "primitive");
    assertTrue(bean.invoked);
    assertEquals(bean.primitiveRetval0, o);
  }

  public void testPropertyPrimitiveSet() {
    assertNotNull(getPropertySetter(beanJso, "primitive"));
    setPropertyInt(beanJso, "primitive", 42);
    assertTrue(bean.invoked);
    assertEquals(42, bean.arg0);  // autoboxed
  }

  public void testPropertyPrimitiveROGet() {
    assertNotNull(getPropertyGetter(beanJso, "primitiveRO"));
    int o = getPropertyInt(beanJso, "primitiveRO");
    assertTrue(bean.invoked);
    assertEquals(bean.primitiveRetval0, o);
  }

  public void testPropertyPrimitiveROSet() {
    assertNull(getPropertySetter(beanJso, "primitiveRO"));
  }

  public void testPropertyPrimitiveWOGet() {
    assertNull(getPropertyGetter(beanJso, "primitiveWO"));
  }

  public void testPropertyPrimitiveWOSet() {
    assertNotNull(getPropertySetter(beanJso, "primitiveWO"));
    setPropertyInt(beanJso, "primitiveWO", 42);
    assertTrue(bean.invoked);
    assertEquals(42, bean.arg0);  // autoboxed
  }

  public void testPropertyBeanGet() {
    JavaScriptObject o = getPropertyBean(beanJso, "bean");
    assertTrue(bean.invoked);
    assertSame(friend0Jso, o);
  }

  public void testPropertyBeanSet() {
    setPropertyBean(beanJso, "bean", friend0Jso);
    assertTrue(bean.invoked);
    // Argument should have been unwrapped
    assertSame(friend0, bean.arg0);
  }

  private static native JavaScriptObject makeJsBoolean(boolean n) /*-{
    return new Boolean(n);
  }-*/;

  private static native JavaScriptObject makeJsFixedPoint(int n) /*-{
    return new Number(n);
  }-*/;

  private static native JavaScriptObject makeJsFloatingPoint(double n) /*-{
    return new Number(n);
  }-*/;

  public void testMethodAcceptingBoolean() {
    JavaScriptObject jso = makeJsBoolean(true);
    callMethodVoidWithArgJso(beanJso, "invokeWithBooleanObj", jso);
    assertTrue(bean.invoked);
    assertEquals(true, ((Boolean) bean.arg0).booleanValue());
  }

  public void testMethodAcceptingBooleanNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithBooleanObj", null);
    assertTrue(bean.invoked);
    assertNull(bean.arg0);
  }

  public void testMethodReturningBoolean() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchBooleanObj");
    Boolean i = new BooleanTamingImpl().getBean(frame, jso);
    assertTrue(bean.invoked);
    assertEquals(bean.booleanRetval, i);
  }

  public void testMethodReturningBooleanNull() {
    JavaScriptObject jso =
        callMethodReturningJso(beanJso, "fetchBooleanObjNull");
    assertTrue(bean.invoked);
    assertNull(jso);
  }

  public void testMethodAcceptingByte() {
    JavaScriptObject jso = makeJsFixedPoint(42);
    callMethodVoidWithArgJso(beanJso, "invokeWithByteObj", jso);
    assertTrue(bean.invoked);
    assertEquals(42, ((Byte) bean.arg0).byteValue());
  }

  public void testMethodAcceptingByteNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithByteObj", null);
    assertTrue(bean.invoked);
    assertNull(bean.arg0);
  }

  public void testMethodReturningByte() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchByteObj");
    Byte i = new ByteTamingImpl().getBean(frame, jso);
    assertTrue(bean.invoked);
    assertEquals(bean.byteRetval, i);
  }

  public void testMethodReturningByteNull() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchByteObjNull");
    assertTrue(bean.invoked);
    assertNull(jso);
  }

  public void testMethodAcceptingDouble() {
    JavaScriptObject jso = makeJsFloatingPoint(42.0);
    callMethodVoidWithArgJso(beanJso, "invokeWithDoubleObj", jso);
    assertTrue(bean.invoked);
    assertEquals(42.0, ((Double) bean.arg0).doubleValue());
  }

  public void testMethodAcceptingDoubleNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithDoubleObj", null);
    assertTrue(bean.invoked);
    assertNull(bean.arg0);
  }

  public void testMethodReturningDouble() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchDoubleObj");
    Double i = new DoubleTamingImpl().getBean(frame, jso);
    assertTrue(bean.invoked);
    assertEquals(bean.doubleRetval, i);
  }

  public void testMethodReturningDoubleNull() {
    JavaScriptObject jso =
        callMethodReturningJso(beanJso, "fetchDoubleObjNull");
    assertTrue(bean.invoked);
    assertNull(jso);
  }

  public void testMethodAcceptingFloat() {
    JavaScriptObject jso = makeJsFloatingPoint(42.0);
    callMethodVoidWithArgJso(beanJso, "invokeWithFloatObj", jso);
    assertTrue(bean.invoked);
    assertEquals(42.0f, ((Float) bean.arg0).floatValue());
  }

  public void testMethodAcceptingFloatNull() {
    JavaScriptObject jso = makeJsFloatingPoint(42.0);
    callMethodVoidWithArgJso(beanJso, "invokeWithFloatObj", null);
    assertTrue(bean.invoked);
    assertNull(bean.arg0);
  }

  public void testMethodReturningFloat() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchFloatObj");
    Float i = new FloatTamingImpl().getBean(frame, jso);
    assertTrue(bean.invoked);
    assertEquals(bean.floatRetval, i);
  }

  public void testMethodReturningFloatNull() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchFloatObjNull");
    assertTrue(bean.invoked);
    assertNull(jso);
  }

  public void testMethodAcceptingInteger() {
    JavaScriptObject jso = makeJsFixedPoint(42);
    callMethodVoidWithArgJso(beanJso, "invokeWithIntegerObj", jso);
    assertTrue(bean.invoked);
    assertEquals(42, ((Integer) bean.arg0).intValue());
  }

  public void testMethodAcceptingIntegerNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithIntegerObj", null);
    assertTrue(bean.invoked);
    assertNull(bean.arg0);
  }

  public void testMethodReturningInteger() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchIntegerObj");
    Integer i = new IntegerTamingImpl().getBean(frame, jso);
    assertTrue(bean.invoked);
    assertEquals(bean.integerRetval, i);
  }

  public void testMethodReturningIntegerNull() {
    JavaScriptObject jso =
        callMethodReturningJso(beanJso, "fetchIntegerObjNull");
    assertTrue(bean.invoked);
    assertNull(jso);
  }

  public void testMethodAcceptingShort() {
    JavaScriptObject jso = makeJsFixedPoint(42);
    callMethodVoidWithArgJso(beanJso, "invokeWithShortObj", jso);
    assertTrue(bean.invoked);
    assertEquals(42, ((Short) bean.arg0).shortValue());
  }

  public void testMethodAcceptingShortNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithShortObj", null);
    assertTrue(bean.invoked);
    assertNull(bean.arg0);
  }

  public void testMethodReturningShort() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchShortObj");
    Short i = new ShortTamingImpl().getBean(frame, jso);
    assertTrue(bean.invoked);
    assertEquals(bean.shortRetval, i);
  }

  public void testMethodReturningShortNull() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchShortObjNull");
    assertTrue(bean.invoked);
    assertNull(jso);
  }

  public void testMethodAcceptingString() {
    callMethodVoidWithArgString(beanJso, "invokeWithStringObj", "hello java");
    assertTrue(bean.invoked);
    assertEquals("hello java", (String) bean.arg0);
  }

  public void testMethodAcceptingStringNull() {
    callMethodVoidWithArgString(beanJso, "invokeWithStringObj", null);
    assertTrue(bean.invoked);
    assertNull(bean.arg0);
  }

  public void testMethodReturningString() {
    String i = callMethodReturningString(beanJso, "fetchStringObj");
    assertTrue(bean.invoked);
    assertEquals(bean.stringRetval, i);
  }

  public void testMethodReturningStringNull() {
    String i = callMethodReturningString(beanJso, "fetchStringObjNull");
    assertTrue(bean.invoked);
    assertNull(i);
  }

  public void testMethodAcceptingDate() {
    callMethodVoidWithArgJso(beanJso, "invokeWithDateObj", dateJso);
    assertTrue(bean.invoked);
    assertTrue(bean.arg0 instanceof Date);
    assertEquals(date, bean.arg0);
  }

  public void testMethodAcceptingDateNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithDateObj", null);
    assertTrue(bean.invoked);
    assertNull(bean.arg0);
  }

  private static native String getDateValue(JavaScriptObject dateJso) /*-{
    return '' + (dateJso - 0);
  }-*/;

  public void testMethodReturningDate() {
    JavaScriptObject i = callMethodReturningJso(beanJso, "fetchDateObj");
    assertTrue(bean.invoked);
    assertEquals(date.getTime(), Long.parseLong(getDateValue(i)));
  }

  public void testMethodReturningDateNull() {
    JavaScriptObject i = callMethodReturningJso(beanJso, "fetchDateObjNull");
    assertTrue(bean.invoked);
    assertNull(i);
  }

  private native JavaScriptObject makeJsoArray(
      JavaScriptObject x0,
      JavaScriptObject x1) /*-{
    return [ x0, x1 ];
  }-*/;

  private native JavaScriptObject getFromJsoArray(
      JavaScriptObject a,
      int idx) /*-{
    return a[idx];
  }-*/;

  public void testMethodAcceptingBeanArray() {
    callMethodVoidWithArgJso(beanJso, "invokeWithBeanArray", makeJsoArray(
        friend0Jso,
        friend1Jso));
    assertTrue(bean.invoked);
    assertSame(friend0, ((Friend[]) bean.arg0)[0]);
    assertSame(friend1, ((Friend[]) bean.arg0)[1]);
  }

  public void testMethodAcceptingBeanArrayNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithBeanArray", null);
    assertTrue(bean.invoked);
    assertNull(bean.arg0);
  }

  public void testMethodReturningBeanArray() {
    JavaScriptObject i = callMethodReturningJso(beanJso, "fetchBeanArray");
    assertTrue(bean.invoked);
    assertSame(friend0Jso, getFromJsoArray(i, 0));
    assertSame(friend1Jso, getFromJsoArray(i, 1));
  }

  public void testMethodReturningBeanArrayNull() {
    JavaScriptObject i = callMethodReturningJso(beanJso, "fetchBeanArrayNull");
    assertTrue(bean.invoked);
    assertNull(i);
  }

  private native JavaScriptObject makeIntArray(
      int x0,
      int x1) /*-{
    return [ x0, x1 ];
  }-*/;

  private native int getFromIntArray(
      JavaScriptObject a,
      int idx) /*-{
    return a[idx];
  }-*/;

  public void testMethodAcceptingPrimitiveArray() {
    JavaScriptObject o = makeIntArray(
        42,
        13);
    callMethodVoidWithArgJso(beanJso, "invokeWithPrimitiveArray", makeIntArray(
        42,
        13));
    assertTrue(bean.invoked);
    assertSame(42, ((int[]) bean.arg0)[0]);
    assertSame(13, ((int[]) bean.arg0)[1]);
  }

  public void testMethodAcceptingPrimitiveArrayNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithPrimitiveArray", null);
    assertTrue(bean.invoked);
    assertNull(bean.arg0);
  }

  public void testMethodReturningPrimitiveArray() {
    JavaScriptObject i = callMethodReturningJso(beanJso, "fetchPrimitiveArray");
    assertTrue(bean.invoked);
    assertEquals(bean.primitiveRetval0, getFromIntArray(i, 0));
    assertEquals(bean.primitiveRetval1, getFromIntArray(i, 1));
  }

  public void testMethodReturningPrimitiveArrayNull() {
    JavaScriptObject i = callMethodReturningJso(beanJso,
        "fetchPrimitiveArrayNull");
    assertTrue(bean.invoked);
    assertNull(i);
  }

  private static native void call3(
      JavaScriptObject method,
      JavaScriptObject arg0,
      JavaScriptObject arg1,
      JavaScriptObject arg2) /*-{
    method(arg0, arg1, arg2);
  }-*/;

  public void testVarArgs() {
    call3(
        getMethod(beanJso, "invokeWithVarArgs"),
        friend0Jso, friend1Jso, friend2Jso);
    assertTrue(bean.invoked);
    assertSame(friend0, bean.arg0);
    assertTrue(bean.arg1 instanceof Friend[]);
    assertEquals(2, ((Friend[]) bean.arg1).length);
    assertSame(friend1, ((Friend[]) bean.arg1)[0]);
    assertSame(friend2, ((Friend[]) bean.arg1)[1]);
  }

  public void testVarArgsEmpty() {
    callMethodVoidWithArgJso(beanJso, "invokeWithVarArgs", friend0Jso);
    assertTrue(bean.invoked);
    assertSame(friend0, bean.arg0);
    assertTrue(bean.arg1 instanceof Friend[]);
    assertEquals(0, ((Friend[]) bean.arg1).length);
  }

  @Override
  public String getModuleName() {
    return "com.google.caja.gwtbeans.SimpleModule";
  }
}