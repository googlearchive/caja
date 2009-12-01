// Copyright (C) 2009 Google Inc.
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

package com.google.caja.ancillary.servlet;

import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Multimap;
import com.google.caja.util.Multimaps;
import com.google.caja.util.Sets;
import com.google.caja.util.Strings;

import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Encapsulates all the information extracted from an HTTP request of the
 * {@link CajaWebToolsServlet web tools servlet}.
 *
 * @author mikesamuel@gmail.com
 */
final class Request implements Cloneable {
  /** The set of static files visible to the request. */
  StaticFiles staticFiles;
  /** The action to perform. */
  Verb verb;
  /** The type of output that the client requested. */
  ContentType otype;
  /**
   * True if we should run the linter over inputs before other processing steps.
   */
  boolean lint;
  /**
   * True if we should run an optimizer over inputs.
   */
  boolean opt;
  /**
   * True if we should render outputs using a minimal printer instead of a
   * pretty printer.
   */
  boolean minify;
  /** True if the outputs should be rendered using only ASCII code-points. */
  boolean asciiOnly;
  /**
   * A pattern that matches user agent strings so that the optimizer can
   * incorporate knowledge about the user agent that will receive the output.
   */
  Pattern userAgent;
  /**
   * The minimum level of messages to report.
   */
  MessageLevel minLevel = MessageLevel.WARNING;
  /** The HTML schema to use. */
  HtmlSchema htmlSchema;
  /** The CSS schema to use. */
  CssSchema cssSchema;
  /** A URI against which relative URIs in the inputs are resolved. */
  URI baseUri = URI.create("http://www.example.com/");
  /** Inputs to process. */
  List<Input> inputs = Lists.newArrayList();
  /** Maps input paths to sources. */
  Map<InputSource, CharProducer> srcMap = Maps.newLinkedHashMap();
  Set<String> toIgnore = Sets.newHashSet();
  MessageContext mc = new MessageContext();
  /** temporary used when assembling inputs.  A path name. */
  String ifile;
  /** temporary used when assembling inputs.  A mime-type. */
  String itype;

  @Override
  public Request clone() {
    Request clone;
    try {
      clone = (Request) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new RuntimeException(ex);
    }
    clone.inputs = Lists.newArrayList(clone.inputs);
    clone.srcMap = Maps.newLinkedHashMap(clone.srcMap);
    clone.toIgnore = Sets.newHashSet(clone.toIgnore);
    clone.mc = new MessageContext();
    for (InputSource is : mc.getInputSources()) { clone.mc.addInputSource(is); }
    return clone;
  }

  static Collection<String> paramsAllowed(Verb v) {
    return PARAMS_ALLOWED.get(v);
  }

  static ParamHandler handler(final Verb v, String name) {
    ParamHandler ph = ALL_PARAM_HANDLERS.get(name);
    if (ph == null) {
      return new ParamHandler() {
        public void handle(String name, String val, Request c)
            throws BadInputException {
          throw new BadInputException(
              "Unrecognized param " + name + " not in "
              + PARAMS_ALLOWED.get(c.verb), null);
        }
        public String manual() { throw new UnsupportedOperationException(); }
      };
    }
    if (!PARAMS_ALLOWED.get(v).contains(name)) {
      return new ParamHandler() {
        public void handle(String name, String val, Request c)
            throws BadInputException {
          throw new BadInputException(
              "Param " + name + " not allowed on " + v.requestPath, null);
        }
        public String manual() { throw new UnsupportedOperationException(); }
      };
    }
    return ph;
  }

  /** Returns a fresh request with defaults for the given verb. */
  static Request create(Verb verb, StaticFiles staticFiles) {
    Request req = REQUEST_BY_VERB.get(verb).clone();
    req.staticFiles = staticFiles;
    return req;
  }

  private static final Map<String, ParamHandler> ALL_PARAM_HANDLERS
      = Maps.newHashMap();
  static {
    ALL_PARAM_HANDLERS.put("it", new ParamHandler() {  // input type
      public void handle(String name, String val, Request c) { c.itype = val; }
      public String manual() { return "mime-type of the next input"; }
    });
    ALL_PARAM_HANDLERS.put("ip", new ParamHandler() {  // input path
      public void handle(String name, String val, Request c) { c.ifile = val; }
      public String manual() { return "path of the next input"; }
    });
    ALL_PARAM_HANDLERS.put("i", new ParamHandler() {  // input source
      public void handle(String name, String val, Request c)
          throws BadInputException {
        String mimeType = c.itype;
        String path = c.ifile;
        if (mimeType != null && !"".equals(mimeType)) {
          int semi = mimeType.indexOf(';');
          if (semi >= 0) { mimeType = mimeType.substring(0, semi); }
        }
        ContentType ct = ContentType.guess(mimeType, path, val);
        if (path == null || "".equals(path)) {
          path = "unnamed-" + c.inputs.size() + "." + ct.ext;
        }
        c.itype = c.ifile = null;
        if (IndexPage.DEFAULT_SOURCE_INPUT.equals(val)) {
          // The default input value.  If people add other inputs and leave
          // it there, the output is forced to HTML.
          return;
        }
        c.inputs.add(new Input(ct, path, val));
      }
      public String manual() { return "an input source file"; }
    });
    ALL_PARAM_HANDLERS.put("ot", new ParamHandler() {  // output type
      public void handle(String name, String val, Request c)
          throws BadInputException {
        c.otype = enumFromCgiParam(ContentType.class, name, val);
      }
      public String manual() { return "desired output type"; }
    });
    ALL_PARAM_HANDLERS.put("lint", new ParamHandler() {
      public void handle(String name, String val, Request c)
          throws BadInputException {
        c.lint = enumFromCgiParam(Boolish.class, name, val).bool;
      }
      public String manual() { return "true to sanity check source code"; }
    });
    ALL_PARAM_HANDLERS.put("opt", new ParamHandler() {
      public void handle(String name, String val, Request c)
          throws BadInputException {
        c.opt = enumFromCgiParam(Boolish.class, name, val).bool;
      }
      public String manual() { return "true to optimize source code"; }
    });
    ALL_PARAM_HANDLERS.put("minify", new ParamHandler() {
      public void handle(String name, String val, Request c)
          throws BadInputException {
        c.minify = enumFromCgiParam(Boolish.class, name, val).bool;
      }
      public String manual() {
        return "true to render output with minimal whitespace";
      }
    });
    ALL_PARAM_HANDLERS.put("asciiOnly", new ParamHandler() {
      public void handle(String name, String val, Request c)
          throws BadInputException {
        c.asciiOnly = enumFromCgiParam(Boolish.class, name, val).bool;
      }
      public String manual() {
        return "true to render output using only ascii characters";
      }
    });
    ALL_PARAM_HANDLERS.put("userAgent", new ParamHandler() {
      public void handle(String name, String val, Request c)
          throws BadInputException {
        c.opt = true;
        c.userAgent = "*".equals(val) ? null : Glob.globToRegex(val);
      }
      public String manual() {
        return "a glob that matches browser user agents strings"
            + " used to inform optimizations.";
      }
    });
    ALL_PARAM_HANDLERS.put("minLevel", new ParamHandler() {
      public void handle(String name, String val, Request c)
          throws BadInputException {
        c.minLevel = enumFromCgiParam(MessageLevel.class, name, val);
      }
      public String manual() {
        return "level of minimum log messages reported";
      }
    });
    ALL_PARAM_HANDLERS.put("ign", new ParamHandler() {
      public void handle(String name, String val, Request c)
          throws BadInputException {
        List<String> msgTypes = Arrays.asList(
            Strings.toUpperCase(val).split("[\\s,]+"));
        c.toIgnore.addAll(msgTypes);
        c.toIgnore.remove("");
      }
      public String manual() { return "names of message to ignore"; }
    });
    ALL_PARAM_HANDLERS.put("baseUri", new ParamHandler() {
      public void handle(String name, String val, Request c)
          throws BadInputException {
        try {
          c.baseUri = new URI(val);
        } catch (URISyntaxException ex) {
          throw new BadInputException("Malformed URI " + name + "=" + val, ex);
        }
      }
      public String manual() {
        return "URI against which to resolve relative URIs";
      }
    });
    ALL_PARAM_HANDLERS.put("verb", new ParamHandler() {
      public void handle(String name, String val, Request c)
          throws BadInputException {
        c.verb = enumFromCgiParam(Verb.class, name, val);
      }
      public String manual() {
        return "action to take";
      }
    });
  }
  private static final Multimap<Verb, String> PARAMS_ALLOWED
      = Multimaps.newSetHashMultimap();
  static {
    PARAMS_ALLOWED.putAll(Verb.INDEX, Arrays.asList(
        "i", "it", "ip", "minLevel", "baseUri", "ign", "minLevel", "baseUri",
        "verb"));
    PARAMS_ALLOWED.putAll(Verb.ECHO, Arrays.asList(
        "ot", "i", "it", "ip", "lint", "opt", "minify", "asciiOnly",
        "userAgent", "minLevel", "ign", "baseUri"));
    PARAMS_ALLOWED.putAll(Verb.LINT, Arrays.asList(
        "ot", "i", "it", "ip", "lint", "opt", "minify", "asciiOnly",
        "userAgent", "minLevel", "ign", "baseUri"));
    PARAMS_ALLOWED.putAll(Verb.DOC, Arrays.asList(
        "ot", "i", "it", "ip", "minify", "asciiOnly", "minLevel", "ign",
        "baseUri"));

    Set<String> used = Sets.newLinkedHashSet();
    for (Verb verb : PARAMS_ALLOWED.keySet()) {
      used.addAll(PARAMS_ALLOWED.get(verb));
    }
    Set<String> avail = ALL_PARAM_HANDLERS.keySet();
    Set<String> unused = Sets.newLinkedHashSet(avail);
    unused.removeAll(used);
    Set<String> undef = Sets.newLinkedHashSet(used);
    undef.removeAll(avail);
    if (!(unused.isEmpty() && undef.isEmpty())) {
      throw new AssertionError("Unused " + unused + ", undef " + undef);
    }
  }

  private static <T extends Enum<T>> T enumFromCgiParam(
      Class<T> type, String name, String val)
      throws BadInputException {
    try {
      return Enum.valueOf(type, Strings.toUpperCase(val));
    } catch (IllegalArgumentException ex) {
      throw new BadInputException(
          "Bad CGI param " + name + "=" + val + " but expected one of "
          + EnumSet.allOf(type), ex);
    }
  }

  private enum Boolish {
    TRUE(true),
    FALSE(false),
    YES(true),
    NO(false),
    T(true),
    F(false),
    ;

    final boolean bool;

    Boolish(boolean bool) { this.bool = bool; }
  }

  // Maps verbs, which correspond to URI paths (e.g. /lint, and /index) to a
  // cloneable template of a request for that verb.
  private static final Map<Verb, Request> REQUEST_BY_VERB
      = new EnumMap<Verb, Request>(Verb.class);
  static {
    Request doc = new Request();
    Request echo = new Request();
    Request help = new Request();
    Request index = new Request();
    Request lint = new Request();

    EchoingMessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(System.err), new MessageContext());
    echo.cssSchema = lint.cssSchema = doc.cssSchema
        = CssSchema.getDefaultCss21Schema(mq);
    echo.htmlSchema = lint.htmlSchema = doc.htmlSchema
        = HtmlSchema.getDefault(mq);
    lint.minLevel = MessageLevel.LINT;

    doc.lint = lint.lint = true;
    doc.opt = false;
    echo.minify = true;
    echo.opt = true;

    REQUEST_BY_VERB.put(Verb.DOC, doc);
    REQUEST_BY_VERB.put(Verb.ECHO, echo);
    REQUEST_BY_VERB.put(Verb.HELP, help);
    REQUEST_BY_VERB.put(Verb.INDEX, index);
    REQUEST_BY_VERB.put(Verb.LINT, lint);

    Set<Verb> verbsWoRequest = EnumSet.allOf(Verb.class);
    verbsWoRequest.removeAll(REQUEST_BY_VERB.keySet());
    if (!verbsWoRequest.isEmpty()) {
      throw new AssertionError("Missing request " + verbsWoRequest);
    }

    for (Map.Entry<Verb, Request> e : REQUEST_BY_VERB.entrySet()) {
      e.getValue().verb = e.getKey();
    }
  }
}
