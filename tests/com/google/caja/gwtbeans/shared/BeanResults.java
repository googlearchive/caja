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

/**
 * Encapsulates the results of calls on an {@link Bean} object, while keeping
 * the actual interface of the {@link Bean} clean for GWTBeans introspection.
 */
public class BeanResults {
  public boolean invoked;
  public Object arg0;
  public Object arg1;
  public Object hint;  // General hint for some methods to use
}
