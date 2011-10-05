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

public class CustomIntrospectorTest extends GWTBeansTestCase {
  public void testCorrectAttributes() {
    setupCaja();

    // Method and property names should be present
    assertNotNull(getAttrsKey(beanJso, "invoke"));
    assertNotNull(getAttrsKey(friendJso, "invoke"));

    // Stuff that custom introspector does not expose should be absent
    assertNull(getAttrsKey(beanJso, "fetchPrimitive"));
  }
  
  public void testFunctionReturningVoid() throws Exception {
    setupCaja();
    callMethodVoid(beanJso, "invoke");
    assertTrue(bean.invoked);
  }

  @Override
  public String getModuleName() {
    return "com.google.caja.gwtbeans.CustomIntrospectorModule";
  }
}