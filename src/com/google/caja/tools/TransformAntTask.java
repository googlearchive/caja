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

import com.google.caja.util.Sets;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.tools.ant.BuildException;

/**
 * An ANT task that Cajoles a set of files.
 *
 * Given an ANT task like {@code
 * <transform>
 *   <input file="baz/input1.js"/>
 *   <input file="baz/input2.css"/>
 *   <depend file="baz/boo.css"/>
 *   <output language="cajita" debug="false" file="output-file-1.js" canLink="thinkfu.com" />
 *   <output language="valija" debug="true" file="output-file-2.js" canLink="bar.com foo.com"
 *    ignore="YOUR_CODES_ARE_ON_FIRE"/>
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
 * <p>
 * The optional {@code canLink} attribute specifies a set of urls that cajoled code is allowed
 * to link to.
 * The default is none.
 * <p>
 * The optional {@code ignore} attribute specifies a set of message names to
 * ignore if the build otherwise succeeds.
 * The default is none -- no messages above
 * {@link com.google.caja.reporting.MessageLevel#LOG} are ignored
 * but sometimes we build demos specifically because we want to show that
 * attacks fail, so we ignore expected warnings from those.  The value is a
 * comma or space separated list of message
 * {@link com.google.caja.reporting.MessageTypeInt#name name}s.
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
    private boolean debug, rename, onlyJsEmitted;
    private String language;
    private String renderer = "pretty";
    private Set<String> messagesToIgnore = Sets.newHashSet();
    private Set<String> allowedToLink = Sets.newHashSet();

    @Override
    public Map<String, Object> getOptions() {
      Map<String, Object> options = super.getOptions();
      options.put("debug", debug);
      options.put("language", language);
      options.put("renderer", renderer);
      options.put("toIgnore", messagesToIgnore);
      options.put("rename", rename);
      options.put("onlyJsEmitted", onlyJsEmitted);
      options.put("canLink", allowedToLink);
      return options;
    }

    // Invoked reflectively by ANT.
    public void setDebug(boolean debug) { this.debug = debug; }
    public void setRename(boolean rename) { this.rename = rename; }
    public void setOnlyJsEmitted(boolean onlyJsEmitted) {
      this.onlyJsEmitted = onlyJsEmitted;
    }
    public void setLanguage(String language) { this.language = language; }
    public void setRenderer(String renderer) { this.renderer = renderer; }
    public void setIgnore(String messageTypeNames) {
      this.messagesToIgnore = split(messageTypeNames);
    }
    public void setCanlink(String links) {
      this.allowedToLink = split(links);
    }
    private Set<String> split(String spaceSeparated) {
      Set<String> result = Sets.newHashSet();
      spaceSeparated = spaceSeparated.trim();
      if (!"".equals(spaceSeparated)) {
        result.addAll(Arrays.asList(spaceSeparated.split("[\\s,]+")));
      }
      return result;
    }
  }
}
