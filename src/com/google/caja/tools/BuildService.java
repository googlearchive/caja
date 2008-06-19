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

/**
 * A stable interface between a build system task and the cajoler.
 * The implementation is loaded via reflection from
 * {@link com.google.caja.plugin.BuildServiceImplementation} so that a build
 * system task can use a cajoler built by a dependency.
 *
 * @author mikesamuel@gmail.com
 */
public interface BuildService {
  /**
   * Cajoles inputs to output writing any messages to logger, returning true
   * iff the task passes.
   *
   * @param logger receives messages that should be included in the build log.
   * @param dependees files which may be referenced from inputs.
   * @param inputs files to cajole.
   * @param output file to write cajoled output to.
   * @param options a key/value map supplying optional parameters that may
   *   differ depending on the version of the client.  Unrecognized options
   *   should be ignored.
   * @return true iff output contains the successfully cajoled inputs.
   */
  boolean cajole(PrintWriter logger, List<File> dependees, List<File> inputs,
                 File output, Map<String, Object> options);
  /**
   * Minifies inputs to output writing any messages to logger, returning true
   * iff the task passes.
   *
   * @param logger receives messages that should be included in the build log.
   * @param dependees files which may be referenced from inputs.
   * @param inputs files to minify.
   * @param output file to write minified output to.
   * @param options a key/value map supplying optional parameters that may
   *   differ depending on the version of the client.  Unrecognized options
   *   should be ignored.
   * @return true iff output contains the successfully cajoled inputs.
   */
  boolean minify(PrintWriter logger, List<File> dependees, List<File> inputs,
                 File output, Map<String, Object> options);
}
