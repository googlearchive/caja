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
import java.util.Collections;
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
  /** A set of {@code <job>} tasks. */
  private final List<Job> jobs = new ArrayList<Job>();

  /** Called to actually execute a job by invoking the BuildService. */
  protected abstract boolean run(BuildService buildService, PrintWriter logger,
                                 List<File> depends, List<File> inputs,
                                 File output, Map<String, Object> options)
      throws BuildException;

  @Override
  public void execute() throws BuildException {
    if (jobs.isEmpty()) {
      throw new BuildException("caja task must have one or more <job>s");
    }
    for (Job job : jobs) { job.requireExecutable(); }

    BuildService buildService = getBuildService();
    PrintWriter logger = getLogger();
    try {
      for (Job job : jobs) {
        job.execute(buildService, logger);
      }
    } finally {
      logger.flush();
    }
  }

  /** Invoked reflectively by ANT when a <job> is seen. */
  public Job createJob() {
    Job job = new Job();
    jobs.add(job);
    return job;
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

  /** Encapsulates input files and the output files that should be produced. */
  public class Job {
    /** The file to build. */
    private File output;
    /** Inputs files to compile. */
    private final List<Include> includes = new ArrayList<Include>();
    /** Files that the inputs might include. */
    private final List<Depend> depends = new ArrayList<Depend>();

    public Include createInclude() {
      Include include = new Include();
      includes.add(include);
      return include;
    }

    public Depend createDepend() {
      Depend depend = new Depend();
      depends.add(depend);
      return depend;
    }

    /**
     * The below are invoked reflectively by ant when {@code <job>} attributes
     * are seen.
     */
    public void setOutput(File output) { this.output = output; }

    /** Checks that there is enough information to execute before execution. */
    void requireExecutable() throws BuildException {
      if (output == null) {
        throw new BuildException(
            "<job> at " + getLocation() + "missing 'output' attribute");
      }
      if (includes.isEmpty()) {
        throw new BuildException(
            "<job> at " + getLocation() + "missing <include>s");
      }
      for (Include include : includes) {
        include.requireExecutable();
      }
    }

    /** Builds output, throwing a BuildException if build fails. */
    void execute(BuildService buildService, PrintWriter logger)
        throws BuildException {
      long outputModified = output.lastModified();
      boolean modified = false;  // -> the output file is older than any inputs.
      if (outputModified == 0L) { modified = true; }  // 0L -> !output.exists()

      // Make sure the output directory exists.
      File outputDir = output.getParentFile();
      if (!outputDir.exists()) {
        logger.println("mkdir " + outputDir);
        outputDir.mkdirs();
      }

      List<File> inputs = new ArrayList<File>();
      for (Include include : includes) {
        if (!modified && include.file.lastModified() > outputModified) {
          modified = true;
        }
        inputs.add(include.file);
      }
      List<File> dependees = new ArrayList<File>();
      for (Depend depend : depends) {
        if (!modified && depend.file.lastModified() > outputModified) {
          modified = true;
        }
        dependees.add(depend.file);
      }
      if (modified) {
        logger.println("compiling " + inputs.size() + " files to " + output);
        if (!run(buildService, logger, dependees, inputs, output,
                 Collections.<String, Object>emptyMap())) {
          if (output.exists()) { output.delete(); }
          throw new BuildException("Failed to build " + output);
        }
      }
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
