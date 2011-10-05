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

package com.google.caja.gwtbeans.compile;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JType;

/**
 * Introspects on GWT Bean types.
 */
public interface GwtBeanIntrospector {
  /**
   * Create a GwtBeanInfo for a supplied type.
   *
   * @param context a {@code GeneratorContext}. 
   * @param logger a {@code TreeLogger}.
   * @param type a type that appears in a tamed interface.
   * @return the appropriate GwtBeanInfo, or {@code null} to indicate that this
   *     type is not permitted to appear in the interface (which will result in
   *     a deferred binding compilation error).
   * @exception UnableToCompleteException if a problem occurred.
   */
  GwtBeanInfo create(GeneratorContext context, TreeLogger logger, JType type)
      throws UnableToCompleteException;
}
