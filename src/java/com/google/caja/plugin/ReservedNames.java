// Copyright (C) 2007 Google Inc.
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

package com.google.caja.plugin;

/**
 * Caja reserved names.
 *
 * @author benl@google.com (Ben Laurie)
 */
public class ReservedNames {
  public static final String LOCAL_THIS = "t___";
  public static final String SUPER = "Super";
  static final String TEMP = "x___";
  public static final String ARGUMENTS = "arguments";
  public static final String LOCAL_ARGUMENTS = "a___";

  private ReservedNames() {
  }

}
