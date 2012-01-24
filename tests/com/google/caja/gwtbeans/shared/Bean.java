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

import java.util.Date;

public class Bean {
  private BeanResults results;

  public Bean(BeanResults results) {
    this.results = results;
  }
  
  // Some fields for testing
  public String testPublicField = BeanReturnValues.stringRetval;
  public final String testFinalPublicField = BeanReturnValues.stringRetval;
  protected String testProtectedField;
  public static String testStaticField;
  @SuppressWarnings("unused")
  private String testPrivateField;
  String testPackagePrivateField;

  // Method name starting with underscore
  public void _getValue() {
    results.invoked = true;
  }

  // Method returning void
  public void invoke() {
    results.invoked = true;
  }
  // Method returning primitive
  public int fetchPrimitive() {
    results.invoked = true;
    return BeanReturnValues.primitiveRetval0;
  }
  // Method returning bean
  public Friend fetchBean() {
    results.invoked = true;
    return BeanReturnValues.beanRetval0;
  }

  // Method accepting primitive
  public void invokeWithPrimitive(int a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  // Method accepting bean
  public void invokeWithBean(Friend a0) {
    results.invoked = true;
    results.arg0 = a0;
  }

  // Read/write primitive property
  public int getPrimitive() {
    results.invoked = true;
    return BeanReturnValues.primitiveRetval0;
  }
  public void setPrimitive(int a0) {
    results.invoked = true;
    results.arg0 = a0;
  }

  // Read-only primitive property
  public int getPrimitiveRO() {
    results.invoked = true;
    return BeanReturnValues.primitiveRetval0;
  }

  // Write-only primitive property
  public void setPrimitiveWO(int a0) {
    results.invoked = true;
    results.arg0 = a0;
  }

  // Read/write bean property
  public Friend getBean() {
    results.invoked = true;
    return BeanReturnValues.beanRetval0;
  }
  public void setBean(Friend a0) {
    results.invoked = true;
    results.arg0 = a0;
  }

  // Methods with autoboxable types

  public void invokeWithBooleanObj(Boolean a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public Boolean fetchBooleanObj() {
    results.invoked = true;
    return BeanReturnValues.booleanRetval;
  }
  public Boolean fetchBooleanObjNull() {
    results.invoked = true;
    return null;
  }
  public void invokeWithByteObj(Byte a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public Byte fetchByteObj() {
    results.invoked = true;
    return BeanReturnValues.byteRetval;
  }
  public Byte fetchByteObjNull() {
    results.invoked = true;
    return null;
  }
  public void invokeWithDoubleObj(Double a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public Double fetchDoubleObj() {
    results.invoked = true;
    return BeanReturnValues.doubleRetval;
  }
  public Double fetchDoubleObjNull() {
    results.invoked = true;
    return null;
  }
  public void invokeWithFloatObj(Float a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public Float fetchFloatObj() {
    results.invoked = true;
    return BeanReturnValues.floatRetval;
  }
  public Float fetchFloatObjNull() {
    results.invoked = true;
    return null;
  }
  public void invokeWithIntegerObj(Integer a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public Integer fetchIntegerObj() {
    results.invoked = true;
    return BeanReturnValues.integerRetval;
  }
  public Integer fetchIntegerObjNull() {
    results.invoked = true;
    return null;
  }
  public void invokeWithShortObj(Short a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public Short fetchShortObj() {
    results.invoked = true;
    return BeanReturnValues.shortRetval;
  }
  public Short fetchShortObjNull() {
    results.invoked = true;
    return null;
  }

  // Tests of GWT primitive types

  public void invokeWithBooleanP(boolean a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public boolean fetchBooleanP() {
    results.invoked = true;
    return BeanReturnValues.booleanRetval;
  }
  public void invokeWithByteP(byte a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public byte fetchByteP() {
    this.results.invoked = true;
    return BeanReturnValues.byteRetval;
  }
  public void invokeWithCharacterP(char a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public char fetchCharacterP() {
    results.invoked = true;
    return BeanReturnValues.characterRetval;
  }
  public void invokeWithDoubleP(double a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public double fetchDoubleP() {
    results.invoked = true;
    return BeanReturnValues.doubleRetval;
  }
  public void invokeWithFloatP(float a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public float fetchFloatP() {
    results.invoked = true;
    return BeanReturnValues.floatRetval;
  }
  public void invokeWithIntegerP(int a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public int fetchIntegerP() {
    results.invoked = true;
    return BeanReturnValues.integerRetval;
  }
  public void invokeWithShortP(short a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public short fetchShortP() {
    results.invoked = true;
    return BeanReturnValues.shortRetval;
  }
  public void invokeWithStringObj(String a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public String fetchStringObj() {
    results.invoked = true;
    return BeanReturnValues.stringRetval;
  }
  public String fetchStringObjNull() {
    results.invoked = true;
    return null;
  }

  // Test of Date taming

  public void invokeWithDateObj(Date a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public Date fetchDateObj() {
    results.invoked = true;
    return BeanReturnValues.dateRetval;
  }
  public Date fetchDateObjNull() {
    results.invoked = true;
    return null;
  }

  // Methods testing taming of arrays

  public void invokeWithBeanArray(Friend[] a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public Friend[] fetchBeanArray() {
    results.invoked = true;
    return new Friend[] { BeanReturnValues.beanRetval0, BeanReturnValues.beanRetval1 };
  }
  public Friend[] fetchBeanArrayNull() {
    results.invoked = true;
    return null;
  }

  public void invokeWithPrimitiveArray(int[] a0) {
    results.invoked = true;
    results.arg0 = a0;
  }
  public int[] fetchPrimitiveArray() {
    results.invoked = true;
    return new int[] { BeanReturnValues.primitiveRetval0, BeanReturnValues.primitiveRetval1 };
  }
  public int[] fetchPrimitiveArrayNull() {
    results.invoked = true;
    return null;
  }

  // Var args

  public void invokeWithVarArgs(Friend a0, Friend... a1) {
    results.invoked = true;
    results.arg0 = a0;
    results.arg1 = a1;
  }

  // Simple overloaded methods

  public void invokeOverloaded(int a0, Friend a1) {
    results.invoked = true;
    results.arg0 = a0;
    results.arg1 = a1;
  }

  public void invokeOverloaded(Friend a0, int a1) {
    results.invoked = true;
    results.arg0 = a0;
    results.arg1 = a1;
  }

  public void invokeOverloaded(int a0, boolean a1) {
    results.invoked = true;
    results.arg0 = a0;
    results.arg1 = a1;
  }

  public void invokeOverloaded(boolean a0, int a1) {
    results.invoked = true;
    results.arg0 = a0;
    results.arg1 = a1;
  }

  // More overloaded methods to test arity-based selection

  public void invokeArityOverloaded(Friend a0) {
    results.invoked = true;
    results.arg0 = a0;
    results.hint = "1 arg form";
  }

  public void invokeArityOverloaded(Friend a0, Friend a1) {
    results.invoked = true;
    results.arg0 = a0;
    results.arg1 = a1;
    results.hint = "2 arg form";
  }

  // The following two functions cannot be distinguished if the caller supplies
  // only an (int), but the second form is uniquely determined if the caller
  // supplies an (int,Friend[]).

  public void invokeAmbiguousOverloaded(int a0) {
    results.invoked = true;
    results.arg0 = a0;
  }

  public void invokeAmbiguousOverloaded(int a0, Friend... a1) {
    results.invoked = true;
    results.arg0 = a0;
    results.arg1 = a1;
  }

  // The following two functions can be distinguished if the caller supplies
  // concrete arguments, but not if the caller supplies (undefined).

  public void invokeAmbiguousWithTamedObj(Friend a0) {
    results.invoked = true;
    results.arg0 = a0;
  }

  public void invokeAmbiguousWithTamedObj(Bean a0) {
    results.invoked = true;
    results.arg0 = a0;
  }

  // Methods that should not be visible
  protected void doProtected() { return; }
  @SuppressWarnings("unused")
  private void doPrivate() { return; }
  void doPackagePrivate() { return; }

  // Properties that should not be visible
  protected String getProtected() { return null; }
  /** @param s Unused. */
  protected void setProtected(String s) { return; }
  @SuppressWarnings("unused")
  private String getPrivate() { return null; }
  @SuppressWarnings("unused")
  private void setPrivate(String s) { return; }
  String getPackagePrivate() { return null; }
  /** @param s Unused. */
  void setPackagePrivate(String s) { return; }
}