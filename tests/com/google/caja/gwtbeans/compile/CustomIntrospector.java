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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

// This is a very simple GwtBeanIntrospector that is only applicable to the
// two test bean classes, "Bean" and "Friend".
public class CustomIntrospector implements GwtBeanIntrospector {
  @Override public GwtBeanInfo create(
      GeneratorContext context,
      TreeLogger logger,
      JType type)
      throws UnableToCompleteException {
    final JClassType beanType = (JClassType) type;
    final JClassType tamingType = context.getTypeOracle()
        .findType(type.getQualifiedSourceName() + "Taming");
    return new com.google.caja.gwtbeans.compile.GwtBeanInfo() {
      @Override public JType getType() {
        return beanType;
      }
      @Override public boolean isTamingPrimitiveType() {
        return false;
      }
      @Override public JClassType getTamingInterface() {
        return tamingType;
      }
      @Override public JClassType getTamingImplementation() {
        return null;
      }
      @Override public GwtBeanPropertyDescriptor[] getProperties() {
        return new GwtBeanPropertyDescriptor[0];
      }
      @Override public JMethod[] getMethods() {
        return new JMethod[] {
          beanType.findMethod("invoke", new JType[0])
        };
      }
    };
  }
}
