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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

public class Properties {

  private static final String INTROSPECTOR_CLASS_PROP =
      "com.google.caja.gwtbeans.introspectorClass";

  private static final String DEFAULT_INTROSPECTOR_CLASS =
      DefaultGwtBeanIntrospector.class.getCanonicalName();

  private static final String RECOGNIZE_BEAN_PROPERTIES_PROP =
      "com.google.caja.gwtbeans.recognizeBeanProperties";

  private static final boolean DEFAULT_RECOGNIZE_BEAN_PROPERTIES =
      true;
  
  public static String getIntrospectorClass(
      GeneratorContext context,
      TreeLogger logger)
      throws UnableToCompleteException {
    ConfigurationProperty cp =
        getSingleValuedProperty(context, logger, INTROSPECTOR_CLASS_PROP);
    return (cp == null || cp.getValues().get(0) == null)
        ? DEFAULT_INTROSPECTOR_CLASS
        : cp.getValues().get(0);
  }

  public static boolean isRecognizeBeanProperties(
      GeneratorContext context,
      TreeLogger logger)
      throws UnableToCompleteException {
    ConfigurationProperty cp = getSingleValuedProperty(
        context, logger, RECOGNIZE_BEAN_PROPERTIES_PROP);
    return (cp == null || cp.getValues().get(0) == null)
        ? DEFAULT_RECOGNIZE_BEAN_PROPERTIES
        : Boolean.valueOf(cp.getValues().get(0));
  }

  private static ConfigurationProperty getSingleValuedProperty(
      GeneratorContext context,
      TreeLogger logger,
      String name)
      throws UnableToCompleteException {
    try {
      ConfigurationProperty cp = context.getPropertyOracle()
          .getConfigurationProperty(name);
      if (cp.getValues().size() != 1) {
        logger.log(TreeLogger.Type.ERROR,
            "Must specify exactly one value for property " + name);
        throw new UnableToCompleteException();
      }
      return cp;
    } catch (BadPropertyValueException e) {
      return null;
    }
  }
}
