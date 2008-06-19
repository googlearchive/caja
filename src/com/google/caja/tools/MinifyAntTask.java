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
 * An ANT task that minifies a set of files.
 *
 * Given an ANT task like {@code
 * <minify>
 *   <job output="foo/bar.js">
 *     <include file="baz/input1.js"/>
 *     <include file="baz/input2.js"/>
 *   </job>
 * </minify>
 * }
 * concatenates input1.js and input2.js stripping unnecessary comments and
 * whitespace
 * <p>
 * A single {@code <minify>} element can have multiple {@code <job>}s.
 * <p>
 * The minify element current serves no purpose, but can be extended to provide
 * options over a bunch of jobs.
 *
 * @author mikesamuel@gmail.com
 */
public class MinifyAntTask extends AbstractCajaAntTask {
  @Override
  protected boolean run(BuildService buildService, PrintWriter logger,
                        List<File> depends, List<File> inputs, File output,
                        Map<String, Object> options)
       throws BuildException {
    return buildService.minify(logger, depends, inputs, output, options);
  }
}
