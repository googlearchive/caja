// Copyright (C) 2007 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.reporting.BuildInfo;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Flag processing for main methods.
 *
 * @author mikesamuel@gmail.com
 */
final class Config {
  private final Option INPUT = new Option(
      "i", "input", true,
      "Input file path containing mixed HTML, JS, and CSS.");
  { INPUT.setOptionalArg(false); }

  private final Option OUTPUT_JS = new Option(
      "j", "output_js", true,
      "Output file path for translated JS" +
      " (defaults to input with \".js\")");
  { OUTPUT_JS.setOptionalArg(true); }

  private final Option OUTPUT_CSS = new Option(
      "c", "output_css", true,
      "Output file path for translated CSS" +
      " (defaults to input with \".css\")");
  { OUTPUT_CSS.setOptionalArg(true); }

  private final Option OUTPUT_BASE = new Option(
      "o", "out", true,
      "Path to which the appropriate extension is added to form an output file."
      );
  { OUTPUT_BASE.setOptionalArg(true); }

  private final Option CSS_PREFIX = new Option(
      "p", "css_prefix", true,
      "Plugin CSS namespace prefix");
  { CSS_PREFIX.setOptionalArg(false); }

  private final Options options = new Options();

  private final Class<?> mainClass;
  private final PrintWriter stderr;
  private final String usageText;
  private List<File> inputFiles;
  private File outputBase;
  private File outputJsFile;
  private File outputCssFile;
  private String cssPrefix;

  Config(Class<?> mainClass, PrintStream stderr, String usageText) {
    this(mainClass, new PrintWriter(stderr), usageText);
  }

  Config(Class<?> mainClass, PrintWriter stderr, String usageText) {
    options.addOption(INPUT);
    options.addOption(OUTPUT_JS);
    options.addOption(OUTPUT_CSS);
    options.addOption(OUTPUT_BASE);
    options.addOption(CSS_PREFIX);
    this.mainClass = mainClass;
    this.stderr = stderr;
    this.usageText = usageText;
  }

  Collection<File> getInputFiles() { return inputFiles; }  
  File getOutputJsFile() { return outputJsFile; }
  File getOutputCssFile() { return outputCssFile; }
  File getOutputBase() { return outputBase; }
  String getCssPrefix() { return cssPrefix; }

  boolean processArguments(String[] argv) {
    try {
      CommandLine cl;
      try {
        cl = new BasicParser().parse(options, argv, false);
      } catch (org.apache.commons.cli.ParseException e) {
        stderr.println(e.getMessage());
        return false;
      }

      inputFiles = new ArrayList<File>();
      if (cl.getOptionValues(INPUT.getOpt()) != null) {
        for (String input : cl.getOptionValues(INPUT.getOpt())) {
          File inputFile = new File(input);
          if (!inputFile.exists()) {
            usage("File \"" + inputFile + "\" does not exist", stderr);
            return false;
          }
          if (!inputFile.isFile()) {
            usage("File \"" + inputFile + "\" is not a regular file", stderr);
            return false;
          }
          inputFiles.add(inputFile);
        }
      }
      if (inputFiles.isEmpty()) {
        usage("Option \"--" + INPUT.getLongOpt() + "\" missing", stderr);
        return false;
      }

      if (cl.getOptionValue(OUTPUT_BASE.getOpt()) != null) {
        outputBase = new File(cl.getOptionValue(OUTPUT_BASE.getOpt()));

        outputJsFile = substituteExtension(outputBase, "js");
        outputCssFile = substituteExtension(outputBase, "css");

        if (cl.getOptionValue(OUTPUT_JS.getOpt()) != null) {
          stderr.println("Can't specify both --out and --output_js");
          return false;
        }
        if (cl.getOptionValue(OUTPUT_CSS.getOpt()) != null) {
          stderr.println("Can't specify both --out and --output_css");
          return false;
        }
      } else {
        File inputFile = inputFiles.get(0);

        outputJsFile = cl.getOptionValue(OUTPUT_JS.getOpt()) == null
            ? substituteExtension(inputFile, "js")
            : new File(cl.getOptionValue(OUTPUT_JS.getOpt()));

        outputCssFile = cl.getOptionValue(OUTPUT_CSS.getOpt()) == null
            ? substituteExtension(inputFile, "css")
            : new File(cl.getOptionValue(OUTPUT_CSS.getOpt()));
      }

      if (outputJsFile.equals(outputCssFile)) {
        stderr.println(
            "Output JS & CSS files must be distinct: " + outputJsFile);
        return false;
      }

      cssPrefix = cl.getOptionValue(CSS_PREFIX.getOpt());
      if (cssPrefix == null) {
        usage("Option \"--" + CSS_PREFIX.getLongOpt() + "\" missing", stderr);
        return false;
      }

      return true;
    } finally {
      stderr.flush();
    }
  }

  void usage(String msg, PrintWriter out) {
    out.println(BuildInfo.getInstance().getBuildInfo());
      out.println();
    if (msg != null && !"".equals(msg)) {
      out.println(msg);
      out.println();
    }
    new HelpFormatter().printHelp(
        out, HelpFormatter.DEFAULT_WIDTH,
        (mainClass.getSimpleName()
         + " --input <in.html> --css_prefix <prefix>"
         + " [[--output_js <out.js> [--output_css <out.css>]] | --out <out>]"),
        "\n", options,
        HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD,
        "\n" + usageText, false);
  }

  private File substituteExtension(File file, String extension) {
    String fileName = file.getName();
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot < 0) {
      lastDot = fileName.length();
    }
    return new File(file.getParentFile(),
                    fileName.substring(0, lastDot) + "." + extension);
  }

  public static void main(String[] argv) {
    Config config = new Config(Config.class, System.err, "Does some stuff.");
    System.err.println(config.processArguments(argv));
  }
}
