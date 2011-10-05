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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;

public final class DefaultGwtBeanIntrospector implements GwtBeanIntrospector {
  private final Map<JType, GwtBeanInfo> cache =
      new HashMap<JType, GwtBeanInfo>();
  
  @Override public GwtBeanInfo create(
      GeneratorContext context,
      TreeLogger logger,
      JType type)
      throws UnableToCompleteException {
    if (!cache.containsKey(type)) {
      cache.put(type, createImpl(context, logger, type));
    }
    return cache.get(type);
  }
  
  private GwtBeanInfo createImpl(
      GeneratorContext context,
      TreeLogger logger,
      JType type)
      throws UnableToCompleteException {
    if (isAllowablePrimitiveType(context.getTypeOracle(), type)) {
      return makePrimitiveGwtBeanInfo(type);
    } else if (type instanceof JClassType) {
      return new DefaultGwtBeanInfo(context, logger, (JClassType) type);
    } else {
      logger.log(Type.ERROR,
          "Type " + type.getQualifiedSourceName()
          + " is neither a class nor an allowable primitive type");
      throw new UnableToCompleteException();
    }
  }
  
  private GwtBeanInfo makePrimitiveGwtBeanInfo(final JType type) {
    return new GwtBeanInfo() {
      @Override public JType getType() {
        return type;
      }
      @Override public boolean isTamingPrimitiveType() {
        return true;
      }
      @Override public JClassType getTamingInterface() {
        return null;
      }
      @Override public JClassType getTamingImplementation() {
        return null;
      }
      @Override public GwtBeanPropertyDescriptor[] getProperties() {
        return null;
      }
      @Override public JMethod[] getMethods() {
        return null;
      }
    };
  }
  
  /**
   * Whether the supplied type is a valid GWT primitive type.
   *
   * TODO(ihab.awad): Must *still* explicitly provide a taming for "primitives"
   * to ensure that (e.g.) values are sanitized so that an attacker cannot pass
   * a JS object as a primitive-valued argument and cause toString() or
   * toValue() to be called at the wrong place.
   */
  private boolean isAllowablePrimitiveType(TypeOracle to, JType type) {
    // Note that we do not include GWT class Element in this list, though it is
    // treated by GWT JSNI as a primitive. Instead, we hard-code an actual
    // Taming class for class Element that does the necessary DOM taming.
    return
        (type instanceof JPrimitiveType && type != JPrimitiveType.LONG) ||
        (type == to.findType("java.lang.Boolean")) ||
        (type == to.findType("java.lang.Byte")) ||
        (type == to.findType("java.lang.Double")) ||
        (type == to.findType("java.lang.Float")) ||
        (type == to.findType("java.lang.Integer")) ||
        (type == to.findType("java.lang.Short")) ||
        (type == to.findType("java.lang.String"));
    // Other candidates, and why they do not show up in the above list:
    //   java.lang.Number         - has no direct instances
    //   java.lang.AtomicInteger  - no need for it; have not investigated
    //   java.lang.AtomicLong     - long values are weird in GWT
    //   java.lang.BigDecimal     - long values are weird in GWT
    //   java.lang.BigInteger     - long values are weird in GWT
    //   java.lang.Long           - long values are weird in GWT
  }
}
