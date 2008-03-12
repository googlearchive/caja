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

import com.google.caja.config.ConfigUtil;
import com.google.caja.config.WhiteList;
import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageQueue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Flag processing for main methods.
 * TODO(mikesamuel): make this subclassable so opensocial specific flags can be
 * separated out.
 *
 * @author mikesamuel@gmail.com
 */
public final class Config {
  private final Options options = new Options();

  private final Option INPUT = defineOption(
      "i", "input", "Input file path containing mixed HTML, JS, and CSS.",
      false);

  private final Option OUTPUT_JS = defineOption(
      "j", "output_js",
      "Output file path for translated JS" +
      " (defaults to input with \".js\")",
      true);

  private final Option OUTPUT_CSS = defineOption(
      "c", "output_css",
      "Output file path for translated CSS" +
      " (defaults to input with \".css\")",
      true);

  private final Option OUTPUT_BASE = defineOption(
      "o", "out",
      "Path to which the appropriate extension is added to form output files.",
      true);

  private final Option CSS_PREFIX = defineOption(
      "p", "css_prefix", "Plugin CSS namespace prefix",
      false);

  private final Option CSS_PROPERTY_WHITELIST = defineOption(
      "css_prop_schema",
      "A file: or resource: URI of the CSS Property Whitelist to use.",
      true);

  private final Option HTML_ATTRIBUTE_WHITELIST = defineOption(
      "html_attrib_schema",
      "A file: or resource: URI of the HTML attribute Whitelist to use.",
      true);

  private final Option HTML_ELEMENT_WHITELIST = defineOption(
      "html_property_schema",
      "A file: or resource: URI of the HTML element Whitelist to use.",
      true);

  private final Option BASE_URI = defineOption(
      "base_uri",
      "The URI relative to which URIs in the inputs are resolved.",
      true);

  private final Option VIEW = defineOption(
      "v", "view", "Gadget view to render (default is 'canvas')", true);
 
  private final Class<?> mainClass;
  private final PrintWriter stderr;
  private final String usageText;
  private List<File> inputFiles;
  private File outputBase;
  private File outputJsFile;
  private File outputCssFile;
  private String cssPrefix;
  private URI cssPropertyWhitelistUri;
  private URI htmlAttributeWhitelistUri;
  private URI htmlElementWhitelistUri;
  private URI baseUri;
  private String gadgetView;

  public Config(Class<?> mainClass, PrintStream stderr, String usageText) {
    this(mainClass, new PrintWriter(stderr), usageText);
  }

  public Config(Class<?> mainClass, PrintWriter stderr, String usageText) {
    this.mainClass = mainClass;
    this.stderr = stderr;
    this.usageText = usageText;
  }

  public Collection<File> getInputFiles() { return inputFiles; }  
  public File getOutputJsFile() { return outputJsFile; }
  public File getOutputCssFile() { return outputCssFile; }
  public File getOutputBase() { return outputBase; }
  public String getCssPrefix() { return cssPrefix; }
  public URI getCssPropertyWhitelistUri() {
    return cssPropertyWhitelistUri;
  }
  public URI getHtmlAttributeWhitelistUri() {
    return htmlAttributeWhitelistUri;
  }
  public URI getHtmlElementWhitelistUri() {
    return htmlElementWhitelistUri;
  }
  public URI getBaseUri() { return baseUri; }

  public CssSchema getCssSchema(MessageQueue mq) {
    return new CssSchema(
        whitelist(cssPropertyWhitelistUri, mq),
        whitelist(URI.create(
            "resource:///com/google/caja/lang/css/css21-fns.json"), mq));
  }

  public HtmlSchema getHtmlSchema(MessageQueue mq) {
    return new HtmlSchema(
        whitelist(htmlElementWhitelistUri, mq),
        whitelist(htmlAttributeWhitelistUri, mq));
  }

  public String getGadgetView() { return gadgetView; }

  public boolean processArguments(String[] argv) {
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

      try {
        cssPropertyWhitelistUri = new URI(cl.getOptionValue(
            CSS_PROPERTY_WHITELIST.getOpt(),
            "resource:///com/google/caja/lang/css/css21.json"));
        htmlAttributeWhitelistUri = new URI(cl.getOptionValue(
            HTML_ATTRIBUTE_WHITELIST.getOpt(),
            "resource:///com/google/caja/lang/html/html4-attributes.json"));
        htmlElementWhitelistUri = new URI(cl.getOptionValue(
            HTML_ELEMENT_WHITELIST.getOpt(),
            "resource:///com/google/caja/lang/html/html4-elements.json"));

        if (cl.getOptionValue(BASE_URI.getOpt()) != null) {
          baseUri = new URI(cl.getOptionValue(BASE_URI.getOpt()));
        } else {
          baseUri = inputFiles.get(0).toURI();
        }
      } catch (URISyntaxException ex) {
        stderr.println("Invalid whitelist URI: " + ex.getInput() + "\n    "
                       + ex.getReason());
        return false;
      }

      gadgetView = cl.getOptionValue(VIEW.getOpt(), "canvas");

      return true;
    } finally {
      stderr.flush();
    }
  }

  public void usage(String msg, PrintWriter out) {
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

  private static WhiteList whitelist(URI uri, MessageQueue mq) {
    InputSource src = new InputSource(uri);
    try {
      return ConfigUtil.loadWhiteListFromJson(
          ConfigUtil.openConfigResource(uri, null),
          FilePosition.startOfFile(src), mq);
    } catch (IOException ex) {
      mq.addMessage(MessageType.IO_ERROR, src);
    } catch (ParseException ex) {
      ex.toMessageQueue(mq);
    }
    // Return a Null instance if unable to load.
    return new WhiteList() {
        public Set<String> allowedItems() {
          return Collections.<String>emptySet();
        }
        public Map<String, TypeDefinition> typeDefinitions() {
          return Collections.<String, TypeDefinition>emptyMap();
        };
      };
  }

  private Option defineOption(
      String shortFlag, String longFlag, String help, boolean optional) {
    Option opt = new Option(shortFlag, longFlag, /* hasArg: */ true, help);
    opt.setOptionalArg(optional);
    options.addOption(opt);
    return opt;
  }

  private Option defineOption(String longFlag, String help, boolean optional) {
    return defineOption(longFlag, longFlag, help, optional);
  }

  public static void main(String[] argv) {
    Config config = new Config(Config.class, System.err, "Does some stuff.");
    System.err.println(config.processArguments(argv));
  }
}
