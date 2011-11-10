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

import com.google.caja.gwtbeans.shared.Taming;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TamingConfiguration {
  private static final String TAMING_COMMON_INTERFACE =
      Taming.class.getCanonicalName();
  private final Set<JClassType>
      tamingInterfaces =
      new HashSet<JClassType>();
  private final Map<JClassType, JClassType>
      tamingInterfaceByBeanClass =
      new HashMap<JClassType, JClassType>();
  private final Map<JClassType, JClassType>
      beanClassByTamingInterface =
      new HashMap<JClassType, JClassType>();
  private final Map<JClassType, JClassType>
      tamingImplementationByTamingInterface =
      new HashMap<JClassType, JClassType>();

  public TamingConfiguration(TreeLogger logger, GeneratorContext context)
      throws UnableToCompleteException {
    harvestTamingInterfaces(logger, context);
    harvestTamingImplementations(logger, context);
  }

  public JClassType getTamingInterfaceByBeanClass(
      JClassType beanClass) {
    return tamingInterfaceByBeanClass.get(beanClass);
  }

  public JClassType getBeanClassByTamingInterface(
      JClassType tamingInterface) {
    return beanClassByTamingInterface.get(tamingInterface);
  }

  public JClassType getTamingImplementationByTamingInterface(
      JClassType tamingInterface) {
    return tamingImplementationByTamingInterface.get(tamingInterface);
  }

  private void harvestTamingInterfaces(
      TreeLogger logger,
      GeneratorContext context)
      throws UnableToCompleteException {
    for (String name : Properties.getTamingInterfaces(context)) {
      harvestTamingInterface(logger, context, name);
    }
  }

  private void harvestTamingInterface(
      TreeLogger logger,
      GeneratorContext context,
      String tamingInterfaceName)
      throws UnableToCompleteException {
    JClassType tamingInterface =
        context.getTypeOracle().findType(tamingInterfaceName);

    if (tamingInterface == null) {
      logger.log(Type.ERROR,
          "Taming type " + tamingInterfaceName + " not found in source path");
      throw new UnableToCompleteException();
    }

    if (tamingInterfaces.contains(tamingInterface)) {
      logger.log(Type.ERROR,
          "Taming type " + tamingInterfaceName +
          " is multiply specified in GWT XML");
      throw new UnableToCompleteException();
    }

    if (tamingInterface.isInterface() == null) {
      logger.log(Type.ERROR,
          "Taming type " + tamingInterfaceName + " must be an interface");
      throw new UnableToCompleteException();
    }

    if (tamingInterface.isGenericType() != null) {
      logger.log(Type.ERROR,
          "Taming type " + tamingInterfaceName + " cannot be generic");
      throw new UnableToCompleteException();
    }

    if (tamingInterface.getImplementedInterfaces().length != 1) {
      logger.log(Type.ERROR,
          "Taming type " + tamingInterfaceName
              + " must only extend one interface, " + TAMING_COMMON_INTERFACE);
      throw new UnableToCompleteException();
    }

    if (tamingInterface.getImplementedInterfaces()[0].isParameterized()
        == null) {
      logger.log(Type.ERROR,
          "Taming type " + tamingInterfaceName
              + " must extend " + TAMING_COMMON_INTERFACE
              + " parameterized by bean class or interface");
      throw new UnableToCompleteException();
    }

    JParameterizedType tamingSuperParameterized =
        tamingInterface.getImplementedInterfaces()[0].isParameterized();

    JClassType tamingSuperRaw =
        context.getTypeOracle().findType(TAMING_COMMON_INTERFACE)
            .isGenericType().getRawType();

    if (tamingSuperParameterized.getRawType() != tamingSuperRaw) {
      logger.log(Type.ERROR,
          "Taming type " + tamingInterfaceName
              + " must extend " + TAMING_COMMON_INTERFACE);
      throw new UnableToCompleteException();
    }

    JClassType beanClass = tamingSuperParameterized.getTypeArgs()[0];
    String beanTypeName = beanClass.getParameterizedQualifiedSourceName();

    if (beanClass.isGenericType() != null) {
      logger.log(Type.ERROR,
          "Bean type " + beanTypeName
          + " may not be generic");
      throw new UnableToCompleteException();
    }

    if (beanClass.isInterface() != null) {
      logger.log(Type.ERROR,
          "Bean type " + beanTypeName +
          " may not be an interface because GWT RTTI (java.lang.Class)" +
          " does not support getInterfaces()");
      throw new UnableToCompleteException();
    }

    tamingInterfaces.add(tamingInterface);
    tamingInterfaceByBeanClass.put(beanClass, tamingInterface);
    beanClassByTamingInterface.put(tamingInterface, beanClass);
  }

  private void harvestTamingImplementations(
      TreeLogger logger,
      GeneratorContext context)
      throws UnableToCompleteException {
    for (String name : Properties.getTamingImplementations(context)) {
      harvestTamingImplementation(logger, context, name);
    }
  }

  private void harvestTamingImplementation(
      TreeLogger logger,
      GeneratorContext context,
      String tamingImplementationName)
      throws UnableToCompleteException {
    JClassType tamingImplementation =
        context.getTypeOracle().findType(tamingImplementationName);

    if (tamingImplementation == null) {
      logger.log(Type.ERROR,
          "Taming type " + tamingImplementationName
          + " not found in source path");
      throw new UnableToCompleteException();
    }

    if (tamingImplementation.isInterface() != null) {
      logger.log(Type.ERROR,
          "Taming implementation " + tamingImplementationName
          + " must not be an interface");
      throw new UnableToCompleteException();
    }

    if (tamingImplementation.isGenericType() != null) {
      logger.log(Type.ERROR,
          "Taming implementation " + tamingImplementationName
          + " cannot be generic");
      throw new UnableToCompleteException();
    }

    if (tamingImplementation.getImplementedInterfaces().length == 0) {
      logger.log(Type.ERROR,
          "Taming implementation " + tamingImplementationName
          + " must extend " + TAMING_COMMON_INTERFACE);
      throw new UnableToCompleteException();
    }

    JClassType tamingInterface = null;

    for (JClassType i : tamingImplementation.getImplementedInterfaces()) {
      if (tamingInterfaces.contains(i)) {
        if (tamingInterface == null) {
          tamingInterface = i;
        } else {
          logger.log(Type.ERROR,
              "Taming implementation " + tamingImplementationName
              + " implements multiple known taming interfaces");
          throw new UnableToCompleteException();
        }
      }
    }

    if (tamingInterface == null) {
      logger.log(Type.ERROR,
          "Taming implementation " + tamingImplementationName
          + " does not implement a known taming interfaces");
      throw new UnableToCompleteException();
    }

    tamingImplementationByTamingInterface.put(
        tamingInterface, tamingImplementation);
  }
}
