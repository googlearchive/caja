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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.caja.gwtbeans.shared.ElementTaming;
import com.google.caja.gwtbeans.shared.ElementTamingImpl;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.dom.client.Element;

public final class DefaultGwtBeanInfo implements GwtBeanInfo {
  private static final Map<String, String> knownTamingInterfaces =
      new HashMap<String, String>();
  private static final Map<String, String> knownTamingImplementations =
      new HashMap<String, String>();

  static {
    knownTamingInterfaces.put(
        Element.class.getCanonicalName(),
        ElementTaming.class.getCanonicalName());
    knownTamingImplementations.put(
        ElementTaming.class.getCanonicalName(),
        ElementTamingImpl.class.getCanonicalName());
  }

  private final TreeLogger logger;
  private final GeneratorContext context;
  private final JClassType type;
  private final JClassType tamingInterface;
  private JClassType tamingImplementation;
  private final List<GwtBeanPropertyDescriptor> properties =
      new ArrayList<GwtBeanPropertyDescriptor>();
  private final List<JMethod> methods =
      new ArrayList<JMethod>();  

  public DefaultGwtBeanInfo(
      TreeLogger logger,
      GeneratorContext context,
      JClassType type,
      JClassType tamingInterface)
      throws UnableToCompleteException {
    this.logger = logger;
    this.context = context;
    this.type = type;
    this.tamingInterface = tamingInterface;
    build();
  }

  @Override
  public JClassType getType() {
    return type;
  }

  @Override
  public JClassType getTamingInterface() {
    return tamingInterface;
  }

  @Override
  public JClassType getTamingImplementation() {
    return tamingImplementation;
  }

  @Override
  public GwtBeanPropertyDescriptor[] getProperties() {
    return properties.toArray(new GwtBeanPropertyDescriptor[] {});
  }

  @Override
  public JMethod[] getMethods() {
    return methods.toArray(new JMethod[] {});
  }

  private void build() throws UnableToCompleteException {
    if (knownTamingImplementations.containsKey(
        tamingInterface.getQualifiedSourceName())) {
      tamingImplementation = context.getTypeOracle().findType(
          knownTamingImplementations.get(
              tamingInterface.getQualifiedSourceName()));
      if (tamingImplementation == null) {
        logger.log(Type.ERROR,
            "Taming implementation " +
            knownTamingImplementations.get(
                tamingInterface.getQualifiedSourceName()) +
            " not found");
        throw new UnableToCompleteException();
      }
    } else {
      tamingImplementation = null;
    }

    if (tamingImplementation != null) {
      return;
    }
    
    List<JMethod> allMethods = getAllPublicMethods(type);

    boolean recognizeBeanProperties =
        Properties.isRecognizeBeanProperties(logger, context);

    while (!allMethods.isEmpty()) {
      String propertyName = recognizeBeanProperties
          ? getPropertyName(allMethods.get(0).getName())
          : null;
      if (propertyName != null) {
        properties.add(makePropertyDescriptor(allMethods, propertyName));
      } else {
        methods.add(allMethods.remove(0));
      }
    }
  }

  private GwtBeanPropertyDescriptor makePropertyDescriptor(
      List<JMethod> allMethods, 
      String propertyName)
      throws UnableToCompleteException {
    JMethod is = removeOne(allMethods, getIsName(propertyName));
    JMethod get = removeOne(allMethods, getGetName(propertyName));
    JMethod write = removeOne(allMethods, getSetName(propertyName));
    if (is != null && get != null) {
      logger.log(Type.ERROR,
          "Found duplicate Bean-style read methods for property \"" +
          propertyName + "\"" +
          " at " + is.getJsniSignature() +
          " and " + get.getJsniSignature());
      throw new UnableToCompleteException();
    }
    JMethod read = (is != null) ? is : get;
    if (read == null && write == null) {
      // Should never get here since we derived propertyName
      /// by looking at *some* method
      logger.log(Type.ERROR,
          "Found null getter and setter for property \"" +
          propertyName + "\"" +
          " (should never happen)");
      throw new UnableToCompleteException();
    }
    JType propType;
    if (read != null) {
      // Harvest property type from reader
      propType = read.getReturnType();
      if (propType == JPrimitiveType.VOID) {
        logger.log(Type.ERROR,
            "Bean-style read method returns void for property \"" + 
            propertyName + "\"" +
            " at " + read.getJsniSignature());
        throw new UnableToCompleteException();
      }
      // Ensure writer matches
      if (write != null) {
        if (write.getParameters().length != 1) {
          logger.log(Type.ERROR,
              "Bean-style write method for property \"" + 
              propertyName + "\"" +
              " does not declare exactly 1 parameter" +
              " at " + write.getJsniSignature());
          throw new UnableToCompleteException();
        }
        if (write.getParameters()[0].getType() != propType) {
          logger.log(Type.ERROR,
              "Parameter of Bean-style write method for property \"" +
              propertyName + "\"" +
              " at " + write.getJsniSignature() +        
              " does not match return type of read method" +
              " at " + read.getJsniSignature());
          throw new UnableToCompleteException();
        }
      }
    } else {
      // Harvest property type from writer
      if (write.getParameters().length != 1) {
        logger.log(Type.ERROR,
            "Bean-style write method for property \"" + 
            propertyName + "\"" +
            " does not declare exactly 1 parameter" +
            " at " + write.getJsniSignature());
        throw new UnableToCompleteException();
      }
      propType = write.getParameters()[0].getType();
      if (propType == type.getOracle().findType("java.lang.Void")) {
        logger.log(Type.ERROR,
            "Bean-style write method property \"" +
            propertyName + "\"" +
            " has parameter of disallowed type java.lang.Void " +
            " at " + write.getJsniSignature());
        throw new UnableToCompleteException();
      }
    }
    return new GwtBeanPropertyDescriptor(propertyName, propType, read, write);
  }

  private JMethod removeOne(List<JMethod> allMethods, String methodName)
      throws UnableToCompleteException {
    JMethod result = null;    
    for (int i = 0; i < allMethods.size(); i++) {
      JMethod m = allMethods.get(i);
      if (methodName.equals(m.getName())) {
        if (result != null) {
          logger.log(Type.ERROR,
              "Found duplicate Bean-style methods called \"" +
              methodName + "\"" +
              " at " + result.getJsniSignature() +
              " and " + m.getJsniSignature());
          throw new UnableToCompleteException();
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
  
  private List<JMethod> getAllPublicMethods(JClassType clazz) {
    Map<String, JMethod> methodsBySignature = new HashMap<String, JMethod>();
    JClassType object = clazz.getOracle().findType("java.lang.Object");
    for (JClassType t = clazz; t != object; t = t.getSuperclass()) {
      for (JMethod m : clazz.getMethods()) {
        // Note that subclass methods take priority over
        // superclass ones in our map
        if (!methodsBySignature.containsKey(m.getJsniSignature())
            && m.isPublic()) {
          // TODO(ihab.awad): Methods overridden by subclass with contravariant
          // args are not considered by this algorithm, which could cause false
          // rejection of valid write-only Bean properties
          methodsBySignature.put(m.getJsniSignature(), m);
        }
      }
    }
    return new ArrayList<JMethod>(methodsBySignature.values());
  }
}
