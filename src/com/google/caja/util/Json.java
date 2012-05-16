// Copyright (C) 2008 Google Inc.
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

package com.google.caja.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
/**
 * Utility class for manipulating JSON objects
 *
 * @author jasvir@google.com (Jasvir Nagra)
 *
 */
public class Json {
  public static JSONObject formatAsJson(Object... members) {
    JSONObject o = new JSONObject();
    putJson(o, members);
    return o;
  }

  @SuppressWarnings("unchecked")
  public static void put(JSONObject o, Object... members) {
    for (int i = 0, n = members.length; i < n; i += 2) {
      String name = (String) members[i];
      Object value = toJsonValue(members[i + 1]);
      o.put(name, value);
    }
  }

  @Deprecated
  public static void putJson(JSONObject o, Object... members) {
    put(o, members);
  }

  @SuppressWarnings("unchecked")
  public static void push(JSONArray a, Object... members) {
    for (Object member : members) {
      a.add(toJsonValue(member));
    }
  }

  @Deprecated
  public static void pushJson(JSONArray a, Object... members) {
    push(a, members);
  }

  public static Object toJsonValue(Object value) {
    if (value == null || value instanceof Boolean || value instanceof Number
        || value instanceof JSONObject || value instanceof JSONArray) {
      return value;
    }
    return value.toString();
  }
}
