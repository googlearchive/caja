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

    // Only public non-static fields should be present
    assertNotNull(getAttrsKey(beanJso, "testPublicField"));
    assertNotNull(getAttrsKey(beanJso, "testFinalPublicField"));
    assertNull(getAttrsKey(beanJso, "testProtectedField"));
    assertNull(getAttrsKey(beanJso, "testPrivateField"));
    assertNull(getAttrsKey(beanJso, "testPackagePrivateField"));
    assertNull(getAttrsKey(beanJso, "testStaticField"));
  }

  public void testFunctionReturningVoid() throws Exception {
    callMethodVoid(beanJso, "invoke");
    assertTrue(beanResults.invoked);
  }

  public void testMethodNameWithUnderscore() throws Exception {
    callMethodVoid(beanJso, "_getValue");
    assertTrue(beanResults.invoked);
  }

  public void testMethodReturningPrimitive() {
    int o = callMethodReturningInt(beanJso, "fetchPrimitive");
    assertTrue(beanResults.invoked);
    assertEquals(BeanReturnValues.primitiveRetval0, o);
  }

  public void testMethodReturningBean() {
    JavaScriptObject o = callMethodReturningJso(beanJso, "fetchBean");
    assertTrue(beanResults.invoked);
    assertSame(friend0Jso, o);
  }

  public void testMethodAcceptingPrimitive() {
    callMethodVoidWithArgInt(beanJso, "invokeWithPrimitive", 42);
    assertTrue(beanResults.invoked);
    assertEquals(42, beanResults.arg0);  // autoboxed
  }

  public void testMethodAcceptingBean() {
    callMethodVoidWithArgJso(beanJso, "invokeWithBean", friend0Jso);
    assertTrue(beanResults.invoked);
    // Argument should have been unwrapped
    assertSame(friend0, beanResults.arg0);
  }

  public void testPropertyPrimitiveGet() {
    assertNotNull(getPropertyGetter(beanJso, "primitive"));
    int o = getPropertyInt(beanJso, "primitive");
    assertTrue(beanResults.invoked);
    assertEquals(BeanReturnValues.primitiveRetval0, o);
  }

  public void testPropertyPrimitiveSet() {
    assertNotNull(getPropertySetter(beanJso, "primitive"));
    setPropertyInt(beanJso, "primitive", 42);
    assertTrue(beanResults.invoked);
    assertEquals(42, beanResults.arg0);  // autoboxed
  }

  public void testPropertyPrimitiveROGet() {
    assertNotNull(getPropertyGetter(beanJso, "primitiveRO"));
    int o = getPropertyInt(beanJso, "primitiveRO");
    assertTrue(beanResults.invoked);
    assertEquals(BeanReturnValues.primitiveRetval0, o);
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
    assertTrue(beanResults.invoked);
    assertEquals(42, beanResults.arg0);  // autoboxed
  }

  public void testPropertyBeanGet() {
    JavaScriptObject o = getPropertyBean(beanJso, "bean");
    assertTrue(beanResults.invoked);
    assertSame(friend0Jso, o);
  }

  public void testPropertyBeanSet() {
    setPropertyBean(beanJso, "bean", friend0Jso);
    assertTrue(beanResults.invoked);
    // Argument should have been unwrapped
    assertSame(friend0, beanResults.arg0);
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

  public void testMethodAcceptingBooleanObj() {
    JavaScriptObject jso = makeJsBoolean(true);
    callMethodVoidWithArgJso(beanJso, "invokeWithBooleanObj", jso);
    assertTrue(beanResults.invoked);
    assertEquals(true, ((Boolean) beanResults.arg0).booleanValue());
  }

  public void testMethodAcceptingBooleanObjNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithBooleanObj", null);
    assertTrue(beanResults.invoked);
    assertNull(beanResults.arg0);
  }

  public void testMethodReturningBooleanObj() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchBooleanObj");
    Boolean i = new BooleanTamingImpl().getBean(frame, jso);
    assertTrue(beanResults.invoked);
    assertEquals(BeanReturnValues.booleanRetval, i);
  }

  public void testMethodReturningBooleanObjNull() {
    JavaScriptObject jso =
        callMethodReturningJso(beanJso, "fetchBooleanObjNull");
    assertTrue(beanResults.invoked);
    assertNull(jso);
  }

  public void testMethodAcceptingByteObj() {
    JavaScriptObject jso = makeJsFixedPoint(42);
    callMethodVoidWithArgJso(beanJso, "invokeWithByteObj", jso);
    assertTrue(beanResults.invoked);
    assertEquals(42, ((Byte) beanResults.arg0).byteValue());
  }

  public void testMethodAcceptingByteObjNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithByteObj", null);
    assertTrue(beanResults.invoked);
    assertNull(beanResults.arg0);
  }

  public void testMethodReturningByteObj() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchByteObj");
    Byte i = new ByteTamingImpl().getBean(frame, jso);
    assertTrue(beanResults.invoked);
    assertEquals(BeanReturnValues.byteRetval, i);
  }

  public void testMethodReturningByteObjNull() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchByteObjNull");
    assertTrue(beanResults.invoked);
    assertNull(jso);
  }

  public void testMethodAcceptingDoubleObj() {
    JavaScriptObject jso = makeJsFloatingPoint(42.0);
    callMethodVoidWithArgJso(beanJso, "invokeWithDoubleObj", jso);
    assertTrue(beanResults.invoked);
    assertEquals(42.0, ((Double) beanResults.arg0).doubleValue());
  }

  public void testMethodAcceptingDoubleObjNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithDoubleObj", null);
    assertTrue(beanResults.invoked);
    assertNull(beanResults.arg0);
  }

  public void testMethodReturningDoubleObj() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchDoubleObj");
    Double i = new DoubleTamingImpl().getBean(frame, jso);
    assertTrue(beanResults.invoked);
    assertEquals(BeanReturnValues.doubleRetval, i);
  }

  public void testMethodReturningDoubleObjNull() {
    JavaScriptObject jso =
        callMethodReturningJso(beanJso, "fetchDoubleObjNull");
    assertTrue(beanResults.invoked);
    assertNull(jso);
  }

  public void testMethodAcceptingFloatObj() {
    JavaScriptObject jso = makeJsFloatingPoint(42.0);
    callMethodVoidWithArgJso(beanJso, "invokeWithFloatObj", jso);
    assertTrue(beanResults.invoked);
    assertEquals(42.0f, ((Float) beanResults.arg0).floatValue());
  }

  public void testMethodAcceptingFloatObjNull() {
    @SuppressWarnings("unused")
    JavaScriptObject jso = makeJsFloatingPoint(42.0);
    callMethodVoidWithArgJso(beanJso, "invokeWithFloatObj", null);
    assertTrue(beanResults.invoked);
    assertNull(beanResults.arg0);
  }

  public void testMethodReturningFloatObj() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchFloatObj");
    Float i = new FloatTamingImpl().getBean(frame, jso);
    assertTrue(beanResults.invoked);
    assertEquals(BeanReturnValues.floatRetval, i);
  }

  public void testMethodReturningFloatObjNull() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchFloatObjNull");
    assertTrue(beanResults.invoked);
    assertNull(jso);
  }

  public void testMethodAcceptingIntegerObj() {
    JavaScriptObject jso = makeJsFixedPoint(42);
    callMethodVoidWithArgJso(beanJso, "invokeWithIntegerObj", jso);
    assertTrue(beanResults.invoked);
    assertEquals(42, ((Integer) beanResults.arg0).intValue());
  }

  public void testMethodAcceptingIntegerObjNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithIntegerObj", null);
    assertTrue(beanResults.invoked);
    assertNull(beanResults.arg0);
  }

  public void testMethodReturningIntegerObj() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchIntegerObj");
    Integer i = new IntegerTamingImpl().getBean(frame, jso);
    assertTrue(beanResults.invoked);
    assertEquals(BeanReturnValues.integerRetval, i);
  }

  public void testMethodReturningIntegerObjNull() {
    JavaScriptObject jso =
        callMethodReturningJso(beanJso, "fetchIntegerObjNull");
    assertTrue(beanResults.invoked);
    assertNull(jso);
  }

  public void testMethodAcceptingShortObj() {
    JavaScriptObject jso = makeJsFixedPoint(42);
    callMethodVoidWithArgJso(beanJso, "invokeWithShortObj", jso);
    assertTrue(beanResults.invoked);
    assertEquals(42, ((Short) beanResults.arg0).shortValue());
  }

  public void testMethodAcceptingShortObjNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithShortObj", null);
    assertTrue(beanResults.invoked);
    assertNull(beanResults.arg0);
  }

  public void testMethodReturningShortObj() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchShortObj");
    Short i = new ShortTamingImpl().getBean(frame, jso);
    assertTrue(beanResults.invoked);
    assertEquals(BeanReturnValues.shortRetval, i);
  }

  public void testMethodReturningShortObjNull() {
    JavaScriptObject jso = callMethodReturningJso(beanJso, "fetchShortObjNull");
    assertTrue(beanResults.invoked);
    assertNull(jso);
  }

  private native void callWithNull(JavaScriptObject m) /*-{
    m(null);
  }-*/;

  private native void callWithBooleanP(JavaScriptObject m) /*-{
    m(true);
  }-*/;

  private native boolean callReturningBooleanP(JavaScriptObject m) /*-{
    return m();
  }-*/;

  private native void callWithFixedPointP(JavaScriptObject m) /*-{
    m(42);
  }-*/;

  private native void callWithFloatingPointP(JavaScriptObject m) /*-{
    m(42.372);
  }-*/;

  private native byte callReturningByteP(JavaScriptObject m) /*-{
    return m();
  }-*/;

  private native char callReturningCharacterP(JavaScriptObject m) /*-{
    return m();
  }-*/;

  private native double callReturningDoubleP(JavaScriptObject m) /*-{
    return m();
  }-*/;

  private native float callReturningFloatP(JavaScriptObject m) /*-{
    return m();
  }-*/;

  private native int callReturningIntegerP(JavaScriptObject m) /*-{
    return m();
  }-*/;

  private native short callReturningShortP(JavaScriptObject m) /*-{
    return m();
  }-*/;

  // Boolean primitive

  public void testMethodAcceptingBooleanP() {
    callWithBooleanP(getMethod(beanJso, "invokeWithBooleanP"));
    assertTrue(beanResults.invoked);
    assertEquals(
        true,
        ((Boolean) beanResults.arg0).booleanValue());
  }

  public void testMethodAcceptingBooleanPNull() {
    try {
      callWithNull(getMethod(beanJso, "invokeWithBooleanP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a boolean"));
      assert(e.toString().contains("invokeWithBooleanP(Z)"));
    }
  }

  public void testMethodAcceptingBooleanPWrong() {
    try {
      callWithFloatingPointP(getMethod(beanJso, "invokeWithBooleanP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a boolean"));
      assert(e.toString().contains("invokeWithBooleanP(Z)"));
    }
  }

  public void testMethodReturningBooleanP() {
    boolean i = callReturningBooleanP(getMethod(beanJso, "fetchBooleanP"));
    assertTrue(beanResults.invoked);
    assertEquals((boolean) BeanReturnValues.booleanRetval, i);
  }

  // Byte primitive

  public void testMethodAcceptingByteP() {
    callWithFixedPointP(getMethod(beanJso, "invokeWithByteP"));
    assertTrue(beanResults.invoked);
    assertEquals(
        42,
        ((Byte) beanResults.arg0).byteValue());
  }

  public void testMethodAcceptingBytePNull() {
    try {
      callWithNull(getMethod(beanJso, "invokeWithByteP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as byte)"));
      assert(e.toString().contains("invokeWithByteP(B)"));
    }
  }

  public void testMethodAcceptingBytePWrong() {
    try {
      callWithBooleanP(getMethod(beanJso, "invokeWithByteP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as byte)"));
      assert(e.toString().contains("invokeWithByteP(B)"));
    }
  }

  public void testMethodReturningByteP() {
    byte i = callReturningByteP(getMethod(beanJso, "fetchByteP"));
    assertTrue(beanResults.invoked);
    assertEquals((byte) BeanReturnValues.byteRetval, i);
  }

  // Character primitive

  public void testMethodAcceptingCharacterP() {
    callWithFixedPointP(getMethod(beanJso, "invokeWithCharacterP"));
    assertTrue(beanResults.invoked);
    assertEquals(
        42,
        ((Character) beanResults.arg0).charValue());
  }

  public void testMethodAcceptingCharacterPNull() {
    try {
      callWithNull(getMethod(beanJso, "invokeWithCharacterP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as char)"));
      assert(e.toString().contains("invokeWithCharacterP(C)"));
    }
  }

  public void testMethodAcceptingCharacterPWrong() {
    try {
      callWithBooleanP(getMethod(beanJso, "invokeWithCharacterP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as char)"));
      assert(e.toString().contains("invokeWithCharacterP(C)"));
    }
  }

  public void testMethodReturningCharacterP() {
    char i = callReturningCharacterP(getMethod(beanJso, "fetchCharacterP"));
    assertTrue(beanResults.invoked);
    assertEquals((char) BeanReturnValues.characterRetval, i);
  }

  // Double primitive

  public void testMethodAcceptingDoubleP() {
    callWithFloatingPointP(getMethod(beanJso, "invokeWithDoubleP"));
    assertTrue(beanResults.invoked);
    assertEquals(
        42.372,
        ((Double) beanResults.arg0).doubleValue());
  }

  public void testMethodAcceptingDoublePNull() {
    try {
      callWithNull(getMethod(beanJso, "invokeWithDoubleP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as double)"));
      assert(e.toString().contains("invokeWithDoubleP(D)"));
    }
  }

  public void testMethodAcceptingDoublePWrong() {
    try {
      callWithBooleanP(getMethod(beanJso, "invokeWithDoubleP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as double)"));
      assert(e.toString().contains("invokeWithDoubleP(D)"));
    }
  }

  public void testMethodReturningDoubleP() {
    double i = callReturningDoubleP(getMethod(beanJso, "fetchDoubleP"));
    assertTrue(beanResults.invoked);
    assertEquals((double) BeanReturnValues.doubleRetval, i);
  }

  // Float primitive

  public void testMethodAcceptingFloatP() {
    callWithFloatingPointP(getMethod(beanJso, "invokeWithFloatP"));
    assertTrue(beanResults.invoked);
    assertEquals(
        42.372f,
        ((Float) beanResults.arg0).floatValue());
  }

  public void testMethodAcceptingFloatPNull() {
    try {
      callWithNull(getMethod(beanJso, "invokeWithFloatP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as float)"));
      assert(e.toString().contains("invokeWithFloatP(F)"));
    }
  }

  public void testMethodAcceptingFloatPWrong() {
    try {
      callWithBooleanP(getMethod(beanJso, "invokeWithFloatP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as float)"));
      assert(e.toString().contains("invokeWithFloatP(F)"));
    }
  }

  public void testMethodReturningFloatP() {
    float i = callReturningFloatP(getMethod(beanJso, "fetchFloatP"));
    assertTrue(beanResults.invoked);
    assertEquals((float) BeanReturnValues.floatRetval, i);
  }

  // Integer primitive

  public void testMethodAcceptingIntegerP() {
    callWithFixedPointP(getMethod(beanJso, "invokeWithIntegerP"));
    assertTrue(beanResults.invoked);
    assertEquals(
        42,
        ((Integer) beanResults.arg0).intValue());
  }

  public void testMethodAcceptingIntegerPNull() {
    try {
      callWithNull(getMethod(beanJso, "invokeWithIntegerP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as int)"));
      assert(e.toString().contains("invokeWithIntegerP(I)"));
    }
  }

  public void testMethodAcceptingIntegerPWrong() {
    try {
      callWithBooleanP(getMethod(beanJso, "invokeWithIntegerP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as int)"));
      assert(e.toString().contains("invokeWithIntegerP(I)"));
    }
  }

  public void testMethodReturningIntegerP() {
    int i = callReturningIntegerP(getMethod(beanJso, "fetchIntegerP"));
    assertTrue(beanResults.invoked);
    assertEquals((int) BeanReturnValues.integerRetval, i);
  }

  // Short primitive

  public void testMethodAcceptingShortP() {
    callWithFixedPointP(getMethod(beanJso, "invokeWithShortP"));
    assertTrue(beanResults.invoked);
    assertEquals(
        42,
        ((Short) beanResults.arg0).shortValue());
  }

  public void testMethodAcceptingShortPNull() {
    try {
      callWithNull(getMethod(beanJso, "invokeWithShortP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as short)"));
      assert(e.toString().contains("invokeWithShortP(S)"));
    }
  }

  public void testMethodAcceptingShortPWrong() {
    try {
      callWithBooleanP(getMethod(beanJso, "invokeWithShortP"));
      fail();
    } catch (Exception e) {
      assertFalse(beanResults.invoked);
      assert(e.toString().contains("Not a number (cannot pass as short)"));
      assert(e.toString().contains("invokeWithShortP(S)"));
    }
  }

  public void testMethodReturningShortP() {
    short i = callReturningShortP(getMethod(beanJso, "fetchShortP"));
    assertTrue(beanResults.invoked);
    assertEquals((short) BeanReturnValues.shortRetval, i);
  }

  // String primitive

  public void testMethodAcceptingStringObj() {
    callMethodVoidWithArgString(beanJso, "invokeWithStringObj", "hello java");
    assertTrue(beanResults.invoked);
    assertEquals("hello java", (String) beanResults.arg0);
  }

  public void testMethodAcceptingStringObjNull() {
    callMethodVoidWithArgString(beanJso, "invokeWithStringObj", null);
    assertTrue(beanResults.invoked);
    assertNull(beanResults.arg0);
  }

  public void testMethodReturningStringObj() {
    String i = callMethodReturningString(beanJso, "fetchStringObj");
    assertTrue(beanResults.invoked);
    assertEquals(BeanReturnValues.stringRetval, i);
  }

  public void testMethodReturningStringObjNull() {
    String i = callMethodReturningString(beanJso, "fetchStringObjNull");
    assertTrue(beanResults.invoked);
    assertNull(i);
  }

  // Date objects

  public void testMethodAcceptingDate() {
    callMethodVoidWithArgJso(beanJso, "invokeWithDateObj", dateJso);
    assertTrue(beanResults.invoked);
    assertTrue(beanResults.arg0 instanceof Date);
    assertEquals(date, beanResults.arg0);
  }

  public void testMethodAcceptingDateNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithDateObj", null);
    assertTrue(beanResults.invoked);
    assertNull(beanResults.arg0);
  }

  private static native String getDateValue(JavaScriptObject dateJso) /*-{
    return '' + (dateJso - 0);
  }-*/;

  public void testMethodReturningDate() {
    JavaScriptObject i = callMethodReturningJso(beanJso, "fetchDateObj");
    assertTrue(beanResults.invoked);
    assertEquals(date.getTime(), Long.parseLong(getDateValue(i)));
  }

  public void testMethodReturningDateNull() {
    JavaScriptObject i = callMethodReturningJso(beanJso, "fetchDateObjNull");
    assertTrue(beanResults.invoked);
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
    assertTrue(beanResults.invoked);
    assertSame(friend0, ((Friend[]) beanResults.arg0)[0]);
    assertSame(friend1, ((Friend[]) beanResults.arg0)[1]);
  }

  public void testMethodAcceptingBeanArrayNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithBeanArray", null);
    assertTrue(beanResults.invoked);
    assertNull(beanResults.arg0);
  }

  public void testMethodReturningBeanArray() {
    JavaScriptObject i = callMethodReturningJso(beanJso, "fetchBeanArray");
    assertTrue(beanResults.invoked);
    assertSame(friend0Jso, getFromJsoArray(i, 0));
    assertSame(friend1Jso, getFromJsoArray(i, 1));
  }

  public void testMethodReturningBeanArrayNull() {
    JavaScriptObject i = callMethodReturningJso(beanJso, "fetchBeanArrayNull");
    assertTrue(beanResults.invoked);
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
    @SuppressWarnings("unused")
    JavaScriptObject o = makeIntArray(
        42,
        13);
    callMethodVoidWithArgJso(beanJso, "invokeWithPrimitiveArray", makeIntArray(
        42,
        13));
    assertTrue(beanResults.invoked);
    assertSame(42, ((int[]) beanResults.arg0)[0]);
    assertSame(13, ((int[]) beanResults.arg0)[1]);
  }

  public void testMethodAcceptingPrimitiveArrayNull() {
    callMethodVoidWithArgJso(beanJso, "invokeWithPrimitiveArray", null);
    assertTrue(beanResults.invoked);
    assertNull(beanResults.arg0);
  }

  public void testMethodReturningPrimitiveArray() {
    JavaScriptObject i = callMethodReturningJso(beanJso, "fetchPrimitiveArray");
    assertTrue(beanResults.invoked);
    assertEquals(BeanReturnValues.primitiveRetval0, getFromIntArray(i, 0));
    assertEquals(BeanReturnValues.primitiveRetval1, getFromIntArray(i, 1));
  }

  public void testMethodReturningPrimitiveArrayNull() {
    JavaScriptObject i = callMethodReturningJso(beanJso,
        "fetchPrimitiveArrayNull");
    assertTrue(beanResults.invoked);
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
    assertTrue(beanResults.invoked);
    assertSame(friend0, beanResults.arg0);
    assertTrue(beanResults.arg1 instanceof Friend[]);
    assertEquals(2, ((Friend[]) beanResults.arg1).length);
    assertSame(friend1, ((Friend[]) beanResults.arg1)[0]);
    assertSame(friend2, ((Friend[]) beanResults.arg1)[1]);
  }

  public void testVarArgsEmpty() {
    callMethodVoidWithArgJso(beanJso, "invokeWithVarArgs", friend0Jso);
    assertTrue(beanResults.invoked);
    assertSame(friend0, beanResults.arg0);
    assertTrue(beanResults.arg1 instanceof Friend[]);
    assertEquals(0, ((Friend[]) beanResults.arg1).length);
  }

  private static native void callIntJso(
      JavaScriptObject method,
      int arg0,
      JavaScriptObject arg1) /*-{
    method(arg0, arg1);
  }-*/;

  public void testOverloadedIntFriend() {
    callIntJso(
        getMethod(beanJso, "invokeOverloaded"),
        42, friend0Jso);
    assertTrue(beanResults.invoked);
    assertEquals(42, beanResults.arg0);
    assertSame(friend0, beanResults.arg1);
  }

  private static native void callJsoInt(
      JavaScriptObject method,
      JavaScriptObject arg0,
      int arg1) /*-{
    method(arg0, arg1);
  }-*/;

  public void testOverloadedFriendInt() {
    callJsoInt(
        getMethod(beanJso, "invokeOverloaded"),
        friend0Jso, 42);
    assertTrue(beanResults.invoked);
    assertSame(friend0, beanResults.arg0);
    assertEquals(42, beanResults.arg1);
  }

  private static native void callIntBoolean(
      JavaScriptObject method,
      int arg0,
      boolean arg1) /*-{
    method(arg0, arg1);
  }-*/;

  public void testOverloadedIntBoolean() {
    callIntBoolean(
        getMethod(beanJso, "invokeOverloaded"),
        42, true);
    assertTrue(beanResults.invoked);
    assertTrue(beanResults.arg0 instanceof Integer);
    assertEquals(42, beanResults.arg0);
    assertTrue(beanResults.arg1 instanceof Boolean);
    assertEquals(true, beanResults.arg1);
  }

  private static native void callBooleanInt(
      JavaScriptObject method,
      boolean arg0,
      int arg1) /*-{
    method(arg0, arg1);
  }-*/;

  public void testOverloadedBooleanInt() {
    callBooleanInt(
        getMethod(beanJso, "invokeOverloaded"),
        true, 42);
    assertTrue(beanResults.invoked);
    assertTrue(beanResults.arg0 instanceof Boolean);
    assertEquals(true, beanResults.arg0);
    assertTrue(beanResults.arg1 instanceof Integer);
    assertEquals(42, beanResults.arg1);
  }

  private static native void callUndefinedUndefined(
      JavaScriptObject method) /*-{
    method(undefined, undefined);
  }-*/;

  public void testPrimitivesDoNotAcceptUndefined() {
    // Because all forms of 'invokeOverloaded' contain a primitive
    // in the argument, they will not accept 'undefined' -- there is
    // no way to convert a JS 'undefined' to a Java primitive like 'int'.
    try {
      callUndefinedUndefined(
          getMethod(beanJso, "invokeOverloaded"));
    } catch (Exception e) {
      String s = e.toString();
      assert(s.contains(
          "Supplied arguments do not match"));
      assert(s.contains(
          "invokeOverloaded(ILcom/google/caja/gwtbeans/shared/Friend;)"));
      assert(s.contains(
          "invokeOverloaded(Lcom/google/caja/gwtbeans/shared/Friend;I)"));
      assert(s.contains(
          "invokeOverloaded(IZ)"));
      assert(s.contains(
          "invokeOverloaded(ZI)"));
    }
  }

  private static native void callJso(
      JavaScriptObject method,
      JavaScriptObject a0) /*-{
    method(a0);
  }-*/;

  private static native void callJsoJso(
      JavaScriptObject method,
      JavaScriptObject a0,
      JavaScriptObject a1) /*-{
    method(a0, a1);
  }-*/;

  public void testArityOverloaded1ArgForm() {
    callJso(
        getMethod(beanJso, "invokeArityOverloaded"),
        friend0Jso);
    assertTrue(beanResults.invoked);
    assertEquals(friend0, beanResults.arg0);
    assertEquals("1 arg form", beanResults.hint);
  }

  public void testArityOverloaded2ArgForm() {
    callJsoJso(
        getMethod(beanJso, "invokeArityOverloaded"),
        friend0Jso,
        friend1Jso);
    assertTrue(beanResults.invoked);
    assertEquals(friend0, beanResults.arg0);
    assertEquals(friend1, beanResults.arg1);
    assertEquals("2 arg form", beanResults.hint);
  }

  private static native void callInt(
      JavaScriptObject method,
      int arg0) /*-{
    method(arg0);
  }-*/;

  public void testAmbiguousOverloadedFails() {
    try {
      callInt(
          getMethod(beanJso, "invokeAmbiguousOverloaded"),
          42);
      fail("Must not allow call with ambiguous args");
    } catch (Exception e) {
      String s = e.toString();
      assert(s.contains(
          "Supplied arguments insufficient to determine method"));
      assert(s.contains(
          "invokeAmbiguousOverloaded(I)"));
      assert(s.contains(
          "invokeAmbiguousOverloaded(I[Lcom/google/caja/gwtbeans/shared/Friend;)"));
    }
    assertFalse(beanResults.invoked);
  }

  private static native void callIntJsoJso(
      JavaScriptObject method,
      int arg0,
      JavaScriptObject arg1,
      JavaScriptObject arg2) /*-{
    method(arg0, arg1, arg2);
  }-*/;

  public void testAmbiguousOverloadedSucceeds() {
    callIntJsoJso(
        getMethod(beanJso, "invokeAmbiguousOverloaded"),
        42, friend0Jso, friend1Jso);
    assertTrue(beanResults.invoked);
    assertEquals(42, beanResults.arg0);
    assertTrue(beanResults.arg1 instanceof Friend[]);
    assertEquals(2, ((Friend[]) beanResults.arg1).length);
    assertSame(friend0, ((Friend[]) beanResults.arg1)[0]);
    assertSame(friend1, ((Friend[]) beanResults.arg1)[1]);
  }

  public void testAmbiguousWithBeanSucceeds() {
    callJso(
        getMethod(beanJso, "invokeAmbiguousWithTamedObj"),
        beanJso);
    assertTrue(beanResults.invoked);
    assertEquals(bean, beanResults.arg0);
  }

  public void testAmbiguousWithFriendSucceeds() {
    callJso(
        getMethod(beanJso, "invokeAmbiguousWithTamedObj"),
        friend0Jso);
    assertTrue(beanResults.invoked);
    assertEquals(friend0, beanResults.arg0);
  }

  private static native void callUndefined(
      JavaScriptObject method) /*-{
    method(undefined);
  }-*/;

  public void testAmbiguousWithUndefinedFails() {
    try {
      callUndefined(
          getMethod(beanJso, "invokeAmbiguousWithTamedObj"));
      fail("Must not allow call with ambiguous args");
    } catch (Exception e) {
      String s = e.toString();
      assert(s.contains(
          "Supplied arguments do not match"));
      assert(s.contains(
          "invokeAmbiguousWithTamedObj(Lcom/google/caja/gwtbeans/shared/Friend;)"));
      assert(s.contains(
          "invokeAmbiguousWithTamedObj(Lcom/google/caja/gwtbeans/shared/Bean;)"));
    }
    assertFalse(beanResults.invoked);
  }

  private static native String getStringField(
      JavaScriptObject bean,
      String name) /*-{
    return $wnd.getDef($wnd.getDef(bean, 'attrs')[name].get, 'f')();
  }-*/;

  private static native void setStringField(
      JavaScriptObject bean,
      String name,
      String value) /*-{
    return $wnd.getDef($wnd.getDef(bean, 'attrs')[name].set, 'f')(value);
  }-*/;
  
  public void testPublicField() {
    assertEquals(
        BeanReturnValues.stringRetval,
        getStringField(beanJso, "testPublicField"));
    setStringField(beanJso, "testPublicField", "new value");
    assertEquals("new value", bean.testPublicField);
  }

  public void testFinalPublicField() {
    assertEquals(
        BeanReturnValues.stringRetval,
        getStringField(beanJso, "testFinalPublicField"));
    try {
      setStringField(beanJso, "testFinalPublicField", "new value");
      fail("Setting value of final field did not fail as it should have");
    } catch (Exception e) { }
    assertEquals(
        BeanReturnValues.stringRetval,
        getStringField(beanJso, "testFinalPublicField"));
  }

  @Override
  public String getModuleName() {
    return "com.google.caja.gwtbeans.SimpleModule";
  }
}