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

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;

public class TamingGeneratorFacade extends Generator {
  @Override
  public String generate(
      TreeLogger logger,
      GeneratorContext context,
      String tamingInterfaceName)
      throws UnableToCompleteException {
    try {
      return new TamingGenerator(logger, context, tamingInterfaceName)
          .generate();
    } catch (Throwable e) {
      logger.log(Type.ERROR, e.toString());
      e.printStackTrace(System.err);
      throw new UnableToCompleteException();
    }
  }
}
