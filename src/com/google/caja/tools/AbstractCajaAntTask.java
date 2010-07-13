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
import com.google.caja.util.Lists;
import com.google.caja.util.Sets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * An ANT task that operates on a set of files to produce a single output file
 * via a {@link BuildService}.
 *
 * @see <a href="http://ant.apache.org/manual/tutorial-writing-tasks.html">ANT
 *     Task Tutorial</a>
 * @see <a href="http://ant.apache.org/manual/CoreTasks/typedef.html">Deploying
 *     Tasks</a>
 *
 * @author mikesamuel@gmail.com
 */
public abstract class AbstractCajaAntTask extends Task {
  /** Input files to compile. */
  private final List<FileGroup> inputs = Lists.newArrayList();
  /** Files that the inputs might depend upon. */
  private final List<FileGroup> depends = Lists.newArrayList();
  /** Outputs to generate. */
  private final List<Output> outputs = Lists.newArrayList();

  /** Called to actually execute a job by invoking the BuildService. */
  protected abstract boolean run(BuildService buildService, PrintWriter logger,
                                 List<File> depends, List<File> inputs,
                                 File output, Map<String, Object> options)
      throws BuildException;

  @Override
  public void execute() throws BuildException {
    try {
      for (FileGroup input : inputs) { input.requireExecutable(); }
      for (Output output : outputs) { output.requireExecutable(); }
      for (FileGroup depend : depends) { depend.requireExecutable(); }

      long youngest = Long.MIN_VALUE;
      List<File> inputFiles = Lists.newArrayList();
      for (FileGroup input : inputs) { inputFiles.addAll(input.files); }
      for (File file : inputFiles) {
        youngest = Math.max(youngest, file.lastModified());
      }
      if (Sets.newHashSet(inputFiles).size() != inputFiles.size()) {
        List<File> dupes = Lists.newArrayList(inputFiles);
        for (File f : Sets.newHashSet(inputFiles)) { dupes.remove(f); }
        throw new BuildException("Duplicate inputs: " + dupes);
      }
      List<File> dependees = Lists.newArrayList();
      for (FileGroup depend : depends) { dependees.addAll(depend.files); }
      for (File file : dependees) {
        youngest = Math.max(youngest, file.lastModified());
      }

      BuildService buildService = getBuildService();
      PrintWriter logger = getLogger();
      try {
        for (Output output : outputs) {
          output.build(inputFiles, dependees, youngest, buildService, logger);
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
          public void close() { /* noop */ }
        }, true);
  }

  BuildService getBuildService() throws BuildException {
    return new BuildServiceImplementation();
  }

  /** Invoked reflectively whenever {@code <input>} is seen. */
  public FileGroup createInput() {
    FileGroup input = new FileGroup("<input>");
    inputs.add(input);
    return input;
  }

  /** Invoked reflectively whenever {@code <depend>} is seen. */
  public FileGroup createDepend() {
    FileGroup depend = new FileGroup("<depend>");
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
      File outputTimeFile;
      if (output.isDirectory()) {
        outputTimeFile = new File(output, ".tstamp");
      } else {
        outputTimeFile = output;
      }
      long outputModified = outputTimeFile.lastModified();
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
        boolean success = false;
        try {
          success = run(
              buildService, logger, dependees, inputs, output, options);
          if (success) {
            if (!outputTimeFile.exists()) {
              try {
                (new FileOutputStream(outputTimeFile)).close();
              } catch (IOException ex) {
                throw new BuildException("Failed to build " + output, ex);
              }
            }
          } else {
            throw new BuildException("Failed to build " + output);
          }
        } finally {
          if (!success) {
            if (outputTimeFile.exists()) { outputTimeFile.delete(); }
          }
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

  /**
   * A group of files.  This supports both the {file="path"} attribute and
   * nested filesets.
   */
  public class FileGroup {
    private final List<File> files = Lists.newArrayList();
    private final String originTag;

    FileGroup(String originTag) { this.originTag = originTag; }

    // Invoked reflectively by ant
    public void setFile(File file) {
      this.files.add(file);
    }
    // See http://ant.apache.org/manual/develop.html#nested-elements
    public void addConfiguredFileSet(FileSet fs) {
      DirectoryScanner scanner = fs.getDirectoryScanner(getProject());
      File baseDir = scanner.getBasedir();
      scanner.scan();
      String[] includedFiles = scanner.getIncludedFiles();
      Arrays.sort(includedFiles);
      for (String localPath : includedFiles) {
        files.add(new File(baseDir, localPath));
      }
    }

    void requireExecutable() throws BuildException {
      if (files.isEmpty()) {
        throw new BuildException(originTag + " at " + getLocation()
                                 + "missing 'file' attribute and '<fileset>'");
      }
      for (File file : files) {
        if (!file.canRead()) {
          throw new BuildException(originTag + " at " + getLocation()
                                   + "of '" + file + "' is not readable");
        }
      }
    }
  }
}
