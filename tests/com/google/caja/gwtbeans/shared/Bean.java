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

@HasTaming(typeName = "com.google.caja.gwtbeans.shared.BeanTaming")
public class Bean {

  // Canned return values
  public final String primitiveRetval = "primitive return value";
  public final Friend beanRetval = new Friend();

  // Machinery to introspect on what was called
  public boolean invoked;
  public Object arg0;

  // Some fields for testing
  public String testPublicField;
  protected String testProtectedField;
  private String testPrivateField;
  String testPackagePrivateField;

  // Method name starting with underscore
  public void _getValue() {
    invoked = true;
  }

  // Method returning void
  public void invoke() {
    invoked = true;
  }
  // Method returning primitive
  public String fetchPrimitive() {
    invoked = true;
    return primitiveRetval;
  }
  // Method returning bean
  public Friend fetchBean() {
    invoked = true;
    return beanRetval;
  }

  // Method accepting primitive
  public void invokeWithPrimitive(String a0) {
    invoked = true;
    this.arg0 = a0;
  }
  // Method accepting bean
  public void invokeWithBean(Friend a0) {
    invoked = true;
    this.arg0 = a0;
  }

  // Read/write primitive property
  public String getPrimitive() {
    invoked = true;
    return primitiveRetval;
  }
  public void setPrimitive(String a0) {
    invoked = true;
    this.arg0 = a0;
  }

  // Read-only primitive property
  public String getPrimitiveRO() {
    invoked = true;
    return primitiveRetval;
  }

  // Write-only primitive property
  public void setPrimitiveWO(String a0) {
    invoked = true;
    this.arg0 = a0;
  }

  // Read/write bean property
  public Friend getBean() {
    invoked = true;
    return beanRetval;
  }
  public void setBean(Friend a0) {
    invoked = true;
    this.arg0 = a0;
  }

  // Methods that should not be visible
  protected void doProtected() {}
  private void doPrivate() {}
  void doPackagePrivate() {}

  // Properties that should not be visible
  protected String getProtected() { return null; }
  protected void setProtected(String s) {}
  private String getPrivate() { return null; }
  private void setPrivate(String s) {}
  String getPackagePrivate() { return null; }
  void setPackagePrivate(String s) {}
}