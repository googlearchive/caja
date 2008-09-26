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
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.apache.tools.ant.BuildException;

/**
 * An ANT task that Cajoles a set of files.
 *
 * Given an ANT task like {@code
 * <cajole debug="false">
 *   <job output="foo/bar.js">
 *     <include file="baz/input1.js"/>
 *     <include file="baz/input2.css"/>
 *     <depend file="baz/boo.css"/>
 *   </job>
 * </cajole>
 * }
 * cajoles input1.js and input2.css to foo/bar.js allowing the inputs to
 * reference {@code baz/boo.css}, e.g. via an {@code @import}.
 * <p>
 * A single {@code <cajole>} element can have multiple {@code <job>s}.
 * <p>
 * The cajole element current serves no purpose, but can be extended to provide
 * options over a bunch of jobs, such as a debugging/production mode switch.
 *
 * @author mikesamuel@gmail.com
 */
public class CajoleAntTask extends AbstractCajaAntTask {
  private boolean debug;
  private String languageMode;

  @Override
  protected boolean run(BuildService buildService, PrintWriter logger,
                        List<File> depends, List<File> inputs, File output,
                        Map<String, Object> options)
       throws BuildException {
    options.put("debug", debug);
    options.put("languageMode", languageMode);
    return buildService.cajole(logger, depends, inputs, output, options);
  }

  /** Invoked reflectively by ANT. */
  public void setDebug(boolean debug) { this.debug = debug; }

  /** Invoked reflectively by ANT. */
  public void setLanguageMode(String languageMode) {
    this.languageMode = languageMode;
  }
}
