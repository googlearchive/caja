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

package com.google.caja.demos.gwtbeans.compile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;

public final class GwtBeanInfo {
  private final JClassType clazz;
  private final List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();
  private final List<JMethod> methods = new ArrayList<JMethod>();  
  
  public static class PropertyDescriptor {
    public final String name;
    public final JType type;
    public final JMethod readMethod;
    public final JMethod writeMethod;
    
    public PropertyDescriptor(String name, JType type, JMethod readMethod, JMethod writeMethod) {
      this.name = name;
      this.type = type;
      this.readMethod = readMethod;
      this.writeMethod = writeMethod;
    }
  }
  
  public GwtBeanInfo(JClassType clazz) {
    this.clazz = clazz;
    build();
  }
  
  public JClassType getClazz() {
    return clazz;
  }
  
  public PropertyDescriptor[] getProperties() {
    return properties.toArray(new PropertyDescriptor[] {});
  }

  public JMethod[] getMethods() {
    return methods.toArray(new JMethod[] {});
  }

  private void build() {
    List<JMethod> allMethods = getAllMethods(clazz);
    while (!allMethods.isEmpty()) {
      String propertyName = getPropertyName(allMethods.get(0).getName());
      if (propertyName != null) {
        properties.add(makePropertyDescriptor(allMethods, propertyName));
      } else {
        methods.add(allMethods.remove(0));
      }
    }
  }

  private PropertyDescriptor makePropertyDescriptor(List<JMethod> allMethods, String propertyName) {
    JMethod is = removeOne(allMethods, getIsName(propertyName));
    JMethod get = removeOne(allMethods, getGetName(propertyName));
    JMethod write = removeOne(allMethods, getSetName(propertyName));
    if (is != null && get != null) {
      throw new RuntimeException(
          "Found duplicate Bean-style read methods for property \"" + propertyName + "\"" +
          " at " + is.getJsniSignature() +
          " and " + get.getJsniSignature());
    }
    JMethod read = (is != null) ? is : get;
    if (read == null && write == null) {
      // Should never get here since we derived propertyName by looking at *some* method
      throw new RuntimeException(
          "Found null getter and setter for property \"" + propertyName + "\"" +
          " (should never happen)");
    }
    JType type;
    if (read != null) {
      // Harvest property type from reader
      type = read.getReturnType();
      if (type == JPrimitiveType.VOID) {
        throw new RuntimeException(
            "Bean-style read method returns void for property \"" + propertyName + "\"" +
            " at " + read.getJsniSignature());
      }
      // Ensure writer matches
      if (write != null) {
        if (write.getParameters().length != 1) {
          throw new RuntimeException(
              "Bean-style write method for property \"" + propertyName + "\"" +
              " does not declare exactly 1 parameter" +
              " at " + write.getJsniSignature());
        }
        if (write.getParameters()[0].getType() != type) {
          throw new RuntimeException(
              "Parameter of Bean-style write method for property \"" + propertyName + "\"" +
              " at " + write.getJsniSignature() +        
              " does not match return type of read method" +
              " at " + read.getJsniSignature());
        }
      }
    } else {
      // Harvest property type from writer
      if (write.getParameters().length != 1) {
        throw new RuntimeException(
            "Bean-style write method for property \"" + propertyName + "\"" +
            " does not declare exactly 1 parameter" +
            " at " + write.getJsniSignature());
      }
      type = write.getParameters()[0].getType();
      if (type == clazz.getOracle().findType("java.lang.Void")) {
        throw new RuntimeException(
            "Bean-style write method property \"" + propertyName + "\"" +
            " has parameter of disallowed type java.lang.Void " +
            " at " + write.getJsniSignature());
      }
    }
    return new PropertyDescriptor(propertyName, type, read, write);
  }

  private JMethod removeOne(List<JMethod> allMethods, String methodName) {
    JMethod result = null;    
    for (int i = 0; i < allMethods.size(); i++) {
      JMethod m = allMethods.get(i);
      if (methodName.equals(m.getName())) {
        if (result != null) {
          throw new RuntimeException(
              "Found duplicate Bean-style methods called \"" + methodName + "\"" +
              " at " + result.getJsniSignature() +
              " and " + m.getJsniSignature());
        }
        result = m;
        allMethods.remove(i);
      }
    }
    return result;
  }
  
  private String getIsName(String propertyName) {
    return "is" + upCaseFirst(propertyName);
  }
  
  private String getGetName(String propertyName) {
    return "get" + upCaseFirst(propertyName);
  }
  
  private String getSetName(String propertyName) {
    return "set" + upCaseFirst(propertyName);
  }
  
  private String getPropertyName(String methodName) {
    String name = null;
    if (methodName.startsWith("get") || methodName.startsWith("set")) {
      name = methodName.substring(3);
    }
    if (methodName.startsWith("is")) {
      name = methodName.substring(2);      
    }
    if (name == null || name.length() == 0) { return null; }
    return downCaseFirst(name);
  }
  
  private String downCaseFirst(String s) {
    return s.substring(0, 1).toLowerCase() + s.substring(1);    
  }
  
  private String upCaseFirst(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);    
  }
  
  private List<JMethod> getAllMethods(JClassType clazz) {
    Map<String, JMethod> methodsBySignature = new HashMap<String, JMethod>();
    JClassType object = clazz.getOracle().findType("java.lang.Object");
    for (JClassType t = clazz; t != object; t = t.getSuperclass()) {
      for (JMethod m : clazz.getMethods()) {
        // Note that subclass methods take priority over superclass ones in our map
        if (!methodsBySignature.containsKey(m.getJsniSignature())) {
          // TODO(ihab.awad): Methods overridden by subclass with contravariant
          // args are not considered by this algorithm, which could cause false rejection
          // of valid write-only Bean properties
          methodsBySignature.put(m.getJsniSignature(), m);
        }
      }
    }
    return new ArrayList<JMethod>(methodsBySignature.values());
  }
}
