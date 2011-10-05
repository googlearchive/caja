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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

/**
 * The introspected information about a GWT Bean class which is used by
 * the taming generator to generate a taming implementation.
 */
public interface GwtBeanInfo {

  /**
   * @return the Bean type.
   */
  JType getType();
  
  /**
   * @return whether this Bean class is to be considered a primitive
   * type for the purposes of taming. A primitive is passed thorough the
   * GWT call layer without taming or untaming. Candidates for this status
   * include integers and strings, which are automatically handled properly
   * by GWT JSNI and do not constitute a security risk.
   */
  boolean isTamingPrimitiveType();
  
  /**
   * @return the taming interface for this Bean class. The taming interface
   * must be an empty interface that extends interface Taming, parameterized
   * by the Bean type.
   */
  JClassType getTamingInterface();
  
  /**
   * @return the taming implementation class for this Bean class. If this
   * class is provided, it will be used and the taming generator will forego
   * generating a taming. If this method returns {@code null}, the taming
   * generator will automatically generate a taming class.
   */
  JClassType getTamingImplementation();
  
  /**
   * @return the properties of the Bean class which should be tamed by the
   * generated taming. Properties which should be hidden from the untrusted
   * JavaScript should not be included here.  
   */
  GwtBeanPropertyDescriptor[] getProperties();

  /**
   * @return the methods of the Bean class which should be tamed by the
   * generated taming. Methods which should be hidden from the untrusted
   * JavaScript should not be included here.
   */
  JMethod[] getMethods();
}
