// Copyright (C) 2012 Google Inc.
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

/**
 * Canned return values to use by tests of {@link Bean}, to be used for
 * comparison by test cases.
 */
public class BeanReturnValues {
  public static final int primitiveRetval0 = 42;
  public static final int primitiveRetval1 = 13;
  public static final Friend beanRetval0 = new Friend();
  public static final Friend beanRetval1 = new Friend();
  public static final Friend beanRetval2 = new Friend();
  public static final Boolean booleanRetval = true;
  public static final Byte byteRetval = (byte) 0x08;
  public static final Double doubleRetval = 0.12345;
  public static final Float floatRetval = 0.12345f;
  public static final Integer integerRetval = 42;
  public static final Short shortRetval = (short) 42;
  public static final Character characterRetval = 'b';
  public static final String stringRetval = "hello world";
  public static final Date dateRetval = new Date((long) Math.pow(2, 52));
}
