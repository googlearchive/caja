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
 * <transform>
 *   <include file="baz/input1.js"/>
 *   <include file="baz/input2.css"/>
 *   <depend file="baz/boo.css"/>
 *   <output language="cajita" debug="false" file="output-file-1.js"/>
 *   <output language="valija" debug="true" file="output-file-2.js"/>
 * </transform>
 * }
 * cajoles input1.js and input2.css to foo/bar.js allowing the inputs to
 * reference {@code baz/boo.css}, e.g. via an {@code @import}.
 * <p>
 * A single {@code <transform>} element can have multiple {@code <output>s}.
 * <p>
 * The cajole element current serves no purpose, but can be extended to provide
 * options over a bunch of jobs, such as a debugging/production mode switch.
 * <p>
 * The {@code language} attribute takes the values "cajita" to cajole cajita,
 * "valija" to cajole valija, and "javascript" to skip cajoling.
 * <p>
 * The optional {@code renderer} attribute specifies how to render the output.
 * "pretty" is the default and uses the
 * {@link com.google.caja.render.JsPrettyPrinter}.
 * "minify" to use {@link com.google.caja.render.JsMinimalPrinter}.
 *
 * @author mikesamuel@gmail.com
 */
public class TransformAntTask extends AbstractCajaAntTask {
  @Override
  protected boolean run(BuildService buildService, PrintWriter logger,
                        List<File> depends, List<File> inputs, File output,
                        Map<String, Object> options)
       throws BuildException {
    return buildService.cajole(logger, depends, inputs, output, options);
  }

  @Override
  Output makeOutput() {
    return new TranslateTaskOutput();
  }
  
  public class TranslateTaskOutput extends Output {
    private boolean debug;
    private String language;
    private String renderer = "pretty";

    @Override
    public Map<String, Object> getOptions() {
      Map<String, Object> options = super.getOptions();
      options.put("debug", debug);
      options.put("language", language);
      options.put("renderer", renderer);
      return options;
    }

    // Invoked reflectively by ANT.
    public void setDebug(boolean debug) { this.debug = debug; }
    public void setLanguage(String language) { this.language = language; }
    public void setRenderer(String renderer) { this.renderer = renderer; }
  }
}
