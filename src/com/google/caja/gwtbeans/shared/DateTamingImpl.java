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

/**
 * This taming is touchy because Java dates are represented as <code>long</code>
 * values (in milliseconds since Epoch), but GWT does not directly support the
 * transfer of <code>long</code>s across JSNI method boundaries.
 *
 * <p>Our solution (h/t metaweta@gmail.com) is to transfer across the boundary
 * the <em>string</em> value of the appropriate number, and rely on each side
 * to do its best to convert the string into a number, and construct a
 * <code>Date</code> object (either in Java or JavaScript, as appropriate) from
 * that.
 *
 * <p>In practice, from testing on Google Chrome and Firefox, we have found that
 * the JavaScript Date constructor will succeed for input up to 2^52, and fail
 * for input = 2^53. The Java Date constructor (as tested in GWT Java running in
 * Firefox) will succeed for all values up to and including Long.MAX_VALUE.
 */
public class DateTamingImpl implements DateTaming {
  @Override
  public JavaScriptObject getJso(Frame m, Date bean) {
    return bean == null
        ? null
        : jsoDateFromNumber(Long.toString(bean.getTime()));
  }

  private static native JavaScriptObject jsoDateFromNumber(String number) /*-{
    return new Date(Number(number));
  }-*/;

  @Override
  public Date getBean(Frame m, JavaScriptObject jso) {
    return jso == null
        ? null
        : new Date(Long.parseLong(jsoDateToNumber(jso)));
  }

  private static native String jsoDateToNumber(JavaScriptObject date) /*-{
    return String(Number(date));
  }-*/;
}