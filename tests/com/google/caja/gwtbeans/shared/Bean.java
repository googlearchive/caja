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

public class Bean {

  // Canned return values
  public final int primitiveRetval0 = 42;
  public final int primitiveRetval1 = 13;
  public final Friend beanRetval0 = new Friend();
  public final Friend beanRetval1 = new Friend();
  public final Friend beanRetval2 = new Friend();
  public final Boolean booleanRetval = true;
  public final Byte byteRetval = (byte) 0x08;
  public final Double doubleRetval = 0.12345;
  public final Float floatRetval = 0.12345f;
  public final Integer integerRetval = 42;
  public final Short shortRetval = (short) 42;
  public final String stringRetval = "hello world";

  // Machinery to introspect on what was called
  public boolean invoked;
  public Object arg0;
  public Object arg1;

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
  public int fetchPrimitive() {
    invoked = true;
    return primitiveRetval0;
  }
  // Method returning bean
  public Friend fetchBean() {
    invoked = true;
    return beanRetval0;
  }

  // Method accepting primitive
  public void invokeWithPrimitive(int a0) {
    invoked = true;
    this.arg0 = a0;
  }
  // Method accepting bean
  public void invokeWithBean(Friend a0) {
    invoked = true;
    this.arg0 = a0;
  }

  // Read/write primitive property
  public int getPrimitive() {
    invoked = true;
    return primitiveRetval0;
  }
  public void setPrimitive(int a0) {
    invoked = true;
    this.arg0 = a0;
  }

  // Read-only primitive property
  public int getPrimitiveRO() {
    invoked = true;
    return primitiveRetval0;
  }

  // Write-only primitive property
  public void setPrimitiveWO(int a0) {
    invoked = true;
    this.arg0 = a0;
  }

  // Read/write bean property
  public Friend getBean() {
    invoked = true;
    return beanRetval0;
  }
  public void setBean(Friend a0) {
    invoked = true;
    this.arg0 = a0;
  }

  // Methods with autoboxable types

  public void invokeWithBooleanObj(Boolean a0) {
    invoked = true;
    this.arg0 = a0;
  }
  public Boolean fetchBooleanObj() {
    invoked = true;
    return booleanRetval;
  }
  public Boolean fetchBooleanObjNull() {
    invoked = true;
    return null;
  }
  public void invokeWithByteObj(Byte a0) {
    invoked = true;
    this.arg0 = a0;
  }
  public Byte fetchByteObj() {
    invoked = true;
    return byteRetval;
  }
  public Byte fetchByteObjNull() {
    invoked = true;
    return null;
  }
  public void invokeWithDoubleObj(Double a0) {
    invoked = true;
    this.arg0 = a0;
  }
  public Double fetchDoubleObj() {
    invoked = true;
    return doubleRetval;
  }
  public Double fetchDoubleObjNull() {
    invoked = true;
    return null;
  }
  public void invokeWithFloatObj(Float a0) {
    invoked = true;
    this.arg0 = a0;
  }
  public Float fetchFloatObj() {
    invoked = true;
    return floatRetval;
  }
  public Float fetchFloatObjNull() {
    invoked = true;
    return null;
  }
  public void invokeWithIntegerObj(Integer a0) {
	invoked = true;
    this.arg0 = a0;
  }
  public Integer fetchIntegerObj() {
    invoked = true;
    return integerRetval;
  }
  public Integer fetchIntegerObjNull() {
    invoked = true;
    return null;
  }
  public void invokeWithShortObj(Short a0) {
    invoked = true;
    this.arg0 = a0;
  }
  public Short fetchShortObj() {
    invoked = true;
    return shortRetval;
  }
  public Short fetchShortObjNull() {
    invoked = true;
    return null;
  }
  public void invokeWithStringObj(String a0) {
    invoked = true;
    this.arg0 = a0;
  }
  public String fetchStringObj() {
    invoked = true;
    return stringRetval;
  }
  public String fetchStringObjNull() {
    invoked = true;
    return null;
  }

  // Methods testing taming of arrays
  
  public void invokeWithBeanArray(Friend[] a0) {
    invoked = true;
    this.arg0 = a0;
  }
  public Friend[] fetchBeanArray() {
    invoked = true;
    return new Friend[] { beanRetval0, beanRetval1 };
  }
  public Friend[] fetchBeanArrayNull() {
    invoked = true;    
    return null;
  }

  public void invokeWithPrimitiveArray(int[] a0) {
    invoked = true;
    this.arg0 = a0;
  }
  public int[] fetchPrimitiveArray() {
    invoked = true;
    return new int[] { primitiveRetval0, primitiveRetval1 };
  }
  public int[] fetchPrimitiveArrayNull() {
    invoked = true;    
    return null;
  }

  // Var args

  public void invokeWithVarArgs(Friend a0, Friend... a1) {
    invoked = true;
    this.arg0 = a0;
    this.arg1 = a1;
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