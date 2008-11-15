// Copyright (C) 2008 Google Inc.
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

package com.google.caja.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.apache.tools.ant.BuildException;

/**
 * Invokes a java class to build an output.  This leverages the input and
 * dependency filestamp checking of AbstractCajaAntTask but does not invoke
 * the BuildService.
 *
 * @author mikesamuel@gmail.com
 */
public class GenRuleAntTask extends AbstractCajaAntTask {
  private Class<? extends BuildCommand> clazz;

  @Override
  protected boolean run(BuildService buildService, PrintWriter logger,
                        List<File> depends, List<File> inputs, File output,
                        Map<String, Object> options)
       throws BuildException {
    boolean succeeded = false;
    try {
      clazz.newInstance().build(inputs, depends, output);
      succeeded = true;
      return true;
    } catch (IOException ex) {
      throw new BuildException(ex);
    } catch (IllegalAccessException ex) {
      throw new BuildException(ex);
    } catch (InstantiationException ex) {
      throw new BuildException(ex);
    } finally {
      if (!succeeded) { output.delete(); }
    }
  }

  /** Invoked reflectively by ANT. */
  public void setClass(String className) throws ClassNotFoundException {
    ClassLoader loader = getClass().getClassLoader();
    if (loader == null) { loader = ClassLoader.getSystemClassLoader(); }
    Class<?> clazz = loader.loadClass(className);
    this.clazz = clazz.asSubclass(BuildCommand.class);
  }

  @Override
  Output makeOutput() { return new Output() {}; }
}
