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

public class DontRecognizeBeanPropertiesTest extends GWTBeansTestCase {
  public void testCorrectAttributes() {
    // Method names (and not properties) should be present
    assertNotNull(getAttrsKey(beanJso, "invoke"));
    assertNotNull(getAttrsKey(beanJso, "fetchPrimitive"));
    assertNotNull(getAttrsKey(beanJso, "fetchBean"));
    assertNotNull(getAttrsKey(beanJso, "invokeWithPrimitive"));
    assertNotNull(getAttrsKey(beanJso, "invokeWithBean"));
    assertNotNull(getAttrsKey(beanJso, "getPrimitive"));
    assertNotNull(getAttrsKey(beanJso, "setPrimitive"));
    assertNotNull(getAttrsKey(beanJso, "getPrimitiveRO"));
    assertNotNull(getAttrsKey(beanJso, "setPrimitiveWO"));
    assertNotNull(getAttrsKey(beanJso, "getBean"));
  }
  
  public void testGetter() throws Exception {
    int o = callMethodReturningInt(beanJso, "getPrimitive");
    assertTrue(bean.invoked);
    assertEquals(bean.primitiveRetval0, o);
  }

  public void testSetter() throws Exception {
    callMethodVoidWithArgInt(beanJso, "setPrimitive", 42);
    assertTrue(bean.invoked);
    assertEquals(42, bean.arg0);  // autoboxed
  }

  @Override
  public String getModuleName() {
    return "com.google.caja.gwtbeans.DontRecognizeBeanPropertiesModule";
  }
}