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

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.HelpFormatter;
import com.google.caja.lexer.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * Executable that invokes {@link HtmlPluginCompiler}.
 *
 * @author ihab.awad@gmail.com
 */
public final class HtmlPluginCompilerMain {
  private static final Option INPUT =
      new Option("i", "input", true,
          "Input file path containing mixed HTML, JS and CSS");

  private static final Option OUTPUT_JS =
      new Option("j", "output_js", true,
          "Output file path for translated JS" +
          " (defaults to input with \".js\")");

  private static final Option OUTPUT_CSS =
      new Option("c", "output_css", true,
          "Output file path for translated CSS" +
          " (defaults to input with \".css\")");

  private static final Option JS_NAME =
      new Option("n", "js_name", true,
          "Plugin JS variable name");

  private static final Option CSS_PREFIX =
      new Option("p", "css_prefix", true,
          "Plugin CSS namespace prefix");

  private static final Option ROOT_DIV_ID =
      new Option("r", "root_div_id", true,
          "ID of root <div> into which generated JS will inject content");

  private static final Option IS_BAJA =
      new Option("b", "baja", false, "Emit Baja code instead of Aaja code");

  private static final Options options = new Options();

  static {
    options.addOption(INPUT);
    options.addOption(OUTPUT_JS);
    options.addOption(OUTPUT_CSS);
    options.addOption(JS_NAME);
    options.addOption(CSS_PREFIX);
    options.addOption(ROOT_DIV_ID);
    options.addOption(IS_BAJA);
  }

  private File inputFile = null;
  private File outputJsFile = null;
  private File outputCssFile = null;
  private String jsName = null;
  private String cssPrefix = null;
  private String rootDivId = null;
  private boolean isBaja = false;

  private HtmlPluginCompilerMain() {}

  public static void main(String[] argv) {
    System.exit(new HtmlPluginCompilerMain().run(argv));
  }

  private int run(String[] argv) {
    int rc = processArguments(argv);
    if (rc != 0) return rc;

    if (isBaja)
      jsName += "___OUTERS___";

    HtmlPluginCompiler compiler =
        new HtmlPluginCompiler(readFile(inputFile), jsName,
            cssPrefix, rootDivId, isBaja);

    try {
      if (!compiler.run()) {
        throw new RuntimeException(compiler.getErrors());
      }
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }

    writeFile(outputJsFile, compiler.getOutputJs());
    writeFile(outputCssFile, compiler.getOutputCss());

    return 0;
  }

  private int processArguments(String[] argv) {
    CommandLine cl;
    try {
      cl = new BasicParser().parse(options, argv);
    } catch (org.apache.commons.cli.ParseException e) {
      throw new RuntimeException(e);
    }

    if (cl.hasOption(IS_BAJA.getOpt()))
      isBaja = true;

    if (cl.getOptionValue(INPUT.getOpt()) == null)
      return usage("Option \"" + INPUT.getLongOpt() + "\" missing");
    inputFile = new File(cl.getOptionValue(INPUT.getOpt()));
    if (!inputFile.exists())
      return usage("File \"" + inputFile + "\" does not exist");
    if (!inputFile.isFile())
      return usage("File \"" + inputFile + "\" is not a regular file");

    outputJsFile =
        cl.getOptionValue(OUTPUT_JS.getOpt()) == null ?
            substituteExtension(inputFile, "js") :
            new File(cl.getOptionValue(OUTPUT_JS.getOpt()));

    outputCssFile =
        cl.getOptionValue(OUTPUT_CSS.getOpt()) == null ?
            substituteExtension(inputFile, "css") :
            new File(cl.getOptionValue(OUTPUT_CSS.getOpt()));

    jsName = cl.getOptionValue(JS_NAME.getOpt());
    if (jsName == null)
      return usage("Option \"" + JS_NAME.getLongOpt() + "\" missing");

    cssPrefix = cl.getOptionValue(CSS_PREFIX.getOpt());
    if (cssPrefix == null)
      return usage("Option \"" + CSS_PREFIX.getLongOpt() + "\" missing");

    rootDivId = cl.getOptionValue(ROOT_DIV_ID.getOpt());
    if (rootDivId == null)
      return usage("Option \"" + ROOT_DIV_ID.getLongOpt() + "\" missing");

    return 0;
  }

  private int usage(String msg) {
    System.out.println(msg);
    new HelpFormatter().printHelp(getClass().getName(), options);
    return -1;
  }

  private String readFile(File path) {
    Reader r;
    try {
      r = new BufferedReader(new FileReader(path));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }

    Writer w = new StringWriter();

    try {
      for (int c; (c = r.read()) != -1; ) w.write(c);
    } catch (IOException e)  {
      throw new RuntimeException(e);
    }

    try {
      r.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return w.toString();
  }

  private void writeFile(File path, String contents) {
    Writer w;
    try {
      w = new BufferedWriter(new FileWriter(path, false));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      w.write(contents);
      if (contents.length() > 0 && !contents.endsWith("\n")) {
        w.write("\n");
      }
    } catch (IOException e)  {
      throw new RuntimeException(e);
    }

    try {
      w.flush();
      w.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private File substituteExtension(File originalPath, String extension) {
    String originalPathString = originalPath.getAbsolutePath();
    String basePath = originalPathString.indexOf('.') == -1 ?
        originalPathString :
        originalPathString.substring(0, originalPathString.lastIndexOf('.'));
    return new File(basePath + '.' + extension);
  }
}
