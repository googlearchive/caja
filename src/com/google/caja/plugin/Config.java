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
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Strings;

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
      "i", "input", "Input URI containing HTML, JS, or CSS.", true);

  private final Option OUTPUT_JS = defineOption(
      "j", "output_js",
      "Output file path for translated JS (defaults to input with \".js\")",
      true);

  private final Option OUTPUT_BASE = defineOption(
      "o", "out",
      "Path to which the appropriate extension is added to form output files.",
      true);

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

  private final Option SERVICE_PORT = defineOption(
      "port",
      "The port on which cajoling service is run.",
      true);

  private final Option VIEW = defineOption(
      "v", "view", "Gadget view to render (default is 'canvas')", true);

  private final Option DEBUG_MODE = defineBooleanOption(
      "g", "debug", "Set to add debugging info to cajoled output.");

  private final Option CAJA_MODE = defineBooleanOption(
      "a", "caja", "Enables Caja (as opposed to Cajita) mode.");

  private final Option RENDERER = defineOption(
      "r",
      "renderer",
      "The output renderer ('minify', 'pretty', or 'sidebyside')",
      true);

  public enum SourceRenderMode {
    MINIFY,
    PRETTY,
    SIDEBYSIDE,
    ;
  }

  private final Class<?> mainClass;
  private final PrintWriter stderr;
  private final String usageText;
  private List<URI> inputUris;
  private File outputBase;
  private File outputJsFile;
  private URI cssPropertyWhitelistUri;
  private URI htmlAttributeWhitelistUri;
  private URI htmlElementWhitelistUri;
  private URI baseUri;
  private String gadgetView;
  private boolean debugMode;
  private boolean cajaMode;
  private SourceRenderMode renderer;
  private int servicePort;

  public Config(Class<?> mainClass, PrintStream stderr, String usageText) {
    this(mainClass, new PrintWriter(stderr), usageText);
  }

  public Config(Class<?> mainClass, PrintWriter stderr, String usageText) {
    this.mainClass = mainClass;
    this.stderr = stderr;
    this.usageText = usageText;
  }

  public Collection<URI> getInputUris() { return inputUris; }
  public File getOutputJsFile() { return outputJsFile; }
  public File getOutputBase() { return outputBase; }
  public int getServicePort() { return servicePort; }
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

  public boolean debugMode() { return debugMode; }

  public boolean cajaMode() { return cajaMode; }

  public SourceRenderMode renderer() { return renderer; }

  public boolean processArguments(String[] argv) {
    try {
      CommandLine cl;
      try {
        cl = new BasicParser().parse(options, argv, false);
      } catch (org.apache.commons.cli.ParseException e) {
        usage(e.getMessage(), stderr);
        return false;
      }

      inputUris = new ArrayList<URI>();
      if (cl.getOptionValues(INPUT.getOpt()) != null) {
        for (String input : cl.getOptionValues(INPUT.getOpt())) {
          URI inputUri;
          try {
            if (input.indexOf(':') >= 0) {
              inputUri = new URI(input);
            } else {
              File inputFile = new File(input);

              if (!inputFile.exists()) {
                usage("File \"" + input + "\" does not exist", stderr);
                return false;
              }
              if (!inputFile.isFile()) {
                usage("File \"" + input + "\" is not a regular file", stderr);
                return false;
              }

              inputUri = inputFile.getAbsoluteFile().toURI();
            }
          } catch (URISyntaxException ex) {
            usage("Input \"" + input + "\" is not a valid URI", stderr);
            return false;
          }

          inputUris.add(inputUri);
        }
      }
      if (inputUris.isEmpty()) {
        usage("Option \"--" + INPUT.getLongOpt() + "\" missing", stderr);
        return false;
      }

      if (cl.getOptionValue(OUTPUT_BASE.getOpt()) != null) {
        outputBase = new File(cl.getOptionValue(OUTPUT_BASE.getOpt()));

        outputJsFile = substituteExtension(outputBase, "js");

        if (cl.getOptionValue(OUTPUT_JS.getOpt()) != null) {
          usage("Can't specify both --out and --output_js", stderr);
          return false;
        }
      } else {
        URI inputUri = inputUris.get(0);

        outputJsFile = cl.getOptionValue(OUTPUT_JS.getOpt()) == null
            ? toFileWithExtension(inputUri, "out.js")
            : new File(cl.getOptionValue(OUTPUT_JS.getOpt()));

        if (outputJsFile == null) {
          usage("Please specify js output via " + OUTPUT_JS.getLongOpt(),
                stderr);
        }
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
          baseUri = inputUris.get(0);
        }
      } catch (URISyntaxException ex) {
        stderr.println("Invalid whitelist URI: " + ex.getInput() + "\n    "
                       + ex.getReason());
        return false;
      }

      gadgetView = cl.getOptionValue(VIEW.getOpt(), "canvas");
      debugMode = cl.hasOption(DEBUG_MODE.getOpt());
      cajaMode = cl.hasOption(CAJA_MODE.getOpt());

      String servicePortString;
      try {
        servicePortString = cl.getOptionValue(SERVICE_PORT.getOpt(), "8887");
        servicePort = Integer.parseInt(servicePortString);
      } catch ( NumberFormatException e ) {
        stderr.println(
            "Invalid service port: " + SERVICE_PORT.getOpt() + "\n    "
            + e.getMessage());
        return false;
      }

      String renderString = cl.getOptionValue(RENDERER.getOpt());
      if (renderString != null) {
        renderer = SourceRenderMode.valueOf(renderString.toUpperCase());
        if (renderer == null) {
          stderr.println("Invalid renderer: " + renderString);
          return false;
        }
      } else {
        renderer = SourceRenderMode.PRETTY;
      }

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
         + " --input <in.html> [--output_js <out.js> | --out <out>]"),
        "\n", options,
        HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD,
        "\n" + usageText, false);
  }

  private static File toFileWithExtension(URI uri, String extension) {
    if (!Strings.equalsIgnoreCase("file", uri.getScheme())) {
      return null;
    }
    return substituteExtension(new File(uri.getPath()), extension);
  }

  private static File substituteExtension(File file, String extension) {
    String fileName = file.getName();
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot < 0) { lastDot = fileName.length(); }
    return new File(file.getParentFile(),
                    fileName.substring(0, lastDot) + "." + extension);
  }

  private static WhiteList whitelist(URI uri, MessageQueue mq) {
    InputSource src = new InputSource(uri);
    try {
      return ConfigUtil.loadWhiteListFromJson(
          uri, ConfigUtil.RESOURCE_RESOLVER, mq);
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
        }
      };
  }

  private Option defineOption(
      String shortFlag, String longFlag, String help, boolean optional) {
    Option opt = new Option(shortFlag, longFlag, /* hasArg: */ true, help);
    opt.setOptionalArg(optional);
    options.addOption(opt);
    return opt;
  }

  private Option defineBooleanOption(
      String shortFlag, String longFlag, String help) {
    Option opt = new Option(shortFlag, longFlag, false, help);
    opt.setOptionalArg(true);
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
