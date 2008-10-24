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

import com.google.caja.plugin.BuildServiceImplementation;
import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * An ANT task that operates on a set of files to produce a single output file
 * via a {@link BuildService}.
 *
 * @see <a href="http://ant.apache.org/manual/tutorial-writing-tasks.html">ANT
 *     Task Tutorial</a>
 * @see <a href="http://ant.apache.org/manual/CoreTasks/typedef.html">Deploting
 *     Tasks</a>
 *
 * @author mikesamuel@gmail.com
 */
public abstract class AbstractCajaAntTask extends Task {
  /** Input files to compile. */
  private final List<Include> includes = new ArrayList<Include>();
  /** Files that the inputs might include. */
  private final List<Depend> depends = new ArrayList<Depend>();
  /** Outputs to generate. */
  private final List<Output> outputs = new ArrayList<Output>();
  
  /** Called to actually execute a job by invoking the BuildService. */
  protected abstract boolean run(BuildService buildService, PrintWriter logger,
                                 List<File> depends, List<File> inputs,
                                 File output, Map<String, Object> options)
      throws BuildException;

  @Override
  public void execute() throws BuildException {
    if (includes.isEmpty()) {
      throw new BuildException("caja task must have one or more <include>s");
    }
    try {
      for (Include include : includes) { include.requireExecutable(); }
      for (Output output : outputs) { output.requireExecutable(); }
      for (Depend depend : depends) { depend.requireExecutable(); }

      long youngest = Long.MIN_VALUE;
      List<File> inputs = new ArrayList<File>();
      for (Include include : includes) {
        inputs.add(include.file);
        youngest = Math.max(youngest, include.file.lastModified());
      }
      List<File> dependees = new ArrayList<File>();
      for (Depend depend : depends) {
        dependees.add(depend.file);
        youngest = Math.max(youngest, depend.file.lastModified());
      }

      BuildService buildService = getBuildService();
      PrintWriter logger = getLogger();
      try {
        for (Output output : outputs) {
          output.build(inputs, dependees, youngest, buildService, logger);
        }
      } finally {
        logger.flush();
      }
    } catch (RuntimeException ex) {
      ex.printStackTrace();
      throw new BuildException(ex);
    }
  }

  /**
   * Wrap {@link Task#log(String)} in a PrintWriter so BuildService doesn't have
   * to know about ANT.
   */
  PrintWriter getLogger() {
    return new PrintWriter(
        new Writer() {
          StringBuilder sb = new StringBuilder();
          @Override
          public void write(char[] cbuf, int off, int len) {
            sb.append(cbuf, off, len);
          }
          @Override
          public void flush() {
            if (sb.length() != 0) {
              log(sb.toString().trim());
              sb = new StringBuilder();
            }
          }
          @Override
          public void close() {}
        }, true);
  }

  BuildService getBuildService() throws BuildException {
    return new BuildServiceImplementation();
  }

  /** Invoked reflectively whenever {@code <include>} is seen. */
  public Include createInclude() {
    Include include = new Include();
    includes.add(include);
    return include;
  }

  /** Invoked reflectively whenever {@code <depend>} is seen. */
  public Depend createDepend() {
    Depend depend = new Depend();
    depends.add(depend);
    return depend;
  }
  
  /** Invoked reflectively whenever {@code <output>} is seen. */
  public final Output createOutput() {
    Output output = makeOutput();
    outputs.add(output);
    return output;
  }
  
  abstract Output makeOutput();

  /** Encapsulates input files and the output files that should be produced. */
  public abstract class Output {
    /** The file to build. */
    private File output;

    /**
     * The below are invoked reflectively by ant when {@code <job>} attributes
     * are seen.
     */
    public void setFile(File output) { this.output = output; }

    /** Checks that there is enough information to execute before execution. */
    void requireExecutable() throws BuildException {
      if (output == null) {
        throw new BuildException(
            "<output> at " + getLocation() + "missing 'file' attribute");
      }
    }

    /** Builds output, throwing a BuildException if build fails. */
    void build(List<File> inputs, List<File> dependees, long youngest,
               BuildService buildService, PrintWriter logger)
        throws BuildException {
      long outputModified = output.lastModified();
      boolean modified  // -> the output file is older than any inputs.
          = (outputModified == 0L    // 0L -> !output.exists()
              || outputModified < youngest);

      if (modified) {
        // Make sure the output directory exists.
        File outputDir = output.getParentFile();
        if (!outputDir.exists()) {
          logger.println("mkdir " + outputDir);
          outputDir.mkdirs();
        }

        logger.println("building " + inputs.size() + " files to " + output);
        Map<String, Object> options = getOptions();
        if (!run(buildService, logger, dependees, inputs, output, options)) {
          if (output.exists()) { output.delete(); }
          throw new BuildException("Failed to build " + output);
        }
      }
    }
    
    /**
     * A map of options that are specified on the {@code output} element as
     * expected by {@link AbstractCajaAntTask#run}.
     */
    public Map<String, Object> getOptions() {
      return new HashMap<String, Object>();
    }
  }

  /** An input file. */
  public class Include {
    /** An input file.  Could use FileSet to get globbing? */
    private File file;

    public void setFile(File file) { this.file = file; }

    void requireExecutable() throws BuildException {
      if (file == null) {
        throw new BuildException("<include> at " + getLocation()
                                 + "missing 'file' attribute");
      }
      if (!file.canRead()) {
        throw new BuildException("<include> at " + getLocation()
                                 + "of '" + file + "' is not readable");
      }
    }
  }

  /** An ancillary file. */
  public class Depend {
    /** An ancillary file.  Could use FileSet to get globbing? */
    private File file;

    public void setFile(File file) { this.file = file; }

    void requireExecutable() throws BuildException {
      if (file == null) {
        throw new BuildException("<depend> at " + getLocation()
                                 + "missing 'file' attribute");
      }
      if (!file.canRead()) {
        throw new BuildException("<depend> at " + getLocation()
                                 + "of '" + file + "' is not readable");
      }
    }
  }
}
