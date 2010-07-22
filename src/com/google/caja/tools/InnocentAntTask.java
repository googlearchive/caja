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
 * An ANT task that transforms a SINGLE file of innocent code.
 *
 * Given an ANT task like {@code
 * <innocent>
 *   <output file="foo/bar.js"/>
 *   <input file="baz/input1.js"/>
 * </innocent>
 * }
 * innocent code transforms the input.  This is intended for uncajoled code
 * that needs to interact with cajoled code.  It removes protected elements from
 * for-in loops and puts checks on methods that access THIS to make sure they
 * don't accidentally refer to the global THIS.
 * <p>
 * A single {@code <innocent>} element can have multiple {@code <job>}s,
 * but each {@code <job>} takes a single input/output file.
 *
 * @author adriennefelt@gmail.com
 */
public class InnocentAntTask extends AbstractCajaAntTask {
  @Override
  protected boolean run(BuildService buildService, PrintWriter logger,
                        List<File> depends, List<File> input, File output,
                        Map<String, Object> options)
       throws BuildException {
    return buildService.transfInnocent(logger, depends, input, output, options);
  }

  @Override
  Output makeOutput() { return new Output() { /* concrete */ }; }
}
