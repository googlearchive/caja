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

public class SimpleTest extends GWTBeansTestCase {

  public void testCorrectAttributes() {
    setupCaja();

    // Method and property names should be present
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
    setupCaja();
    callMethodVoid(beanJso, "invoke");
    assertTrue(bean.invoked);
  }

  public void testMethodNameWithUnderscore() throws Exception {
    setupCaja();
    callMethodVoid(beanJso, "_getValue");
    assertTrue(bean.invoked);
  }

  public void testMethodReturningPrimitive() {
    setupCaja();
    Object o = callMethodReturningPrimitive(beanJso, "fetchPrimitive");
    assertTrue(bean.invoked);
    assertTrue(o instanceof String);
    assertEquals(bean.primitiveRetval, o);
  }

  public void testMethodReturningBean() {
    setupCaja();
    JavaScriptObject o = callMethodReturningJso(beanJso, "fetchBean");
    assertTrue(bean.invoked);
    assertSame(friendJso, o);
  }

  public void testMethodAcceptingPrimitive() {
    setupCaja();
    String argSent = "hello world";
    callMethodVoidWithArgPrimitive(beanJso, "invokeWithPrimitive", argSent);
    assertTrue(bean.invoked);
    assertEquals(argSent, bean.arg0);
  }

  public void testMethodAcceptingBean() {
    setupCaja();
    callMethodVoidWithArgJso(beanJso, "invokeWithBean", friendJso);
    assertTrue(bean.invoked);
    // Argument should have been unwrapped
    assertSame(friend, bean.arg0);
  }

  public void testPropertyPrimitiveGet() {
    setupCaja();
    assertNotNull(getPropertyGetter(beanJso, "primitive"));
    Object o = getPropertyPrimitive(beanJso, "primitive");
    assertTrue(bean.invoked);
    assertTrue(o instanceof String);
    assertEquals(bean.primitiveRetval, o);
  }

  public void testPropertyPrimitiveSet() {
    setupCaja();
    assertNotNull(getPropertySetter(beanJso, "primitive"));
    String argSent = "hello world";
    setPropertyPrimitive(beanJso, "primitive", argSent);
    assertTrue(bean.invoked);
    assertEquals(argSent, bean.arg0);
  }

  public void testPropertyPrimitiveROGet() {
    setupCaja();
    assertNotNull(getPropertyGetter(beanJso, "primitiveRO"));
    Object o = getPropertyPrimitive(beanJso, "primitiveRO");
    assertTrue(bean.invoked);
    assertTrue(o instanceof String);
    assertEquals(bean.primitiveRetval, o);
  }

  public void testPropertyPrimitiveROSet() {
    setupCaja();
    assertNull(getPropertySetter(beanJso, "primitiveRO"));
  }

  public void testPropertyPrimitiveWOGet() {
    setupCaja();
    assertNull(getPropertyGetter(beanJso, "primitiveWO"));
  }

  public void testPropertyPrimitiveWOSet() {
    setupCaja();
    assertNotNull(getPropertySetter(beanJso, "primitiveWO"));
    String argSent = "hello world";
    setPropertyPrimitive(beanJso, "primitiveWO", argSent);
    assertTrue(bean.invoked);
    assertEquals(argSent, bean.arg0);
  }

  public void testPropertyBeanGet() {
    setupCaja();
    JavaScriptObject o = getPropertyBean(beanJso, "bean");
    assertTrue(bean.invoked);
    assertSame(friendJso, o);
  }

  public void testPropertyBeanSet() {
    setupCaja();
    setPropertyBean(beanJso, "bean", friendJso);
    assertTrue(bean.invoked);
    // Argument should have been unwrapped
    assertSame(friend, bean.arg0);
  }

  @Override
  public String getModuleName() {
    return "com.google.caja.gwtbeans.SimpleModule";
  }
}