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

package com.google.caja.ancillary.jsdoc;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.BooleanLiteral;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.NullLiteral;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Join;
import com.google.caja.util.Pair;
import com.google.caja.util.Sets;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An extensible registry of {@link AnnotationHandler}s.
 *
 * @author mikesamuel@gmail.com
 */
public final class AnnotationHandlers {
  private final Map<String, AnnotationHandler> handlers
      = new HashMap<String, AnnotationHandler>();
  private final MessageContext mc;

  public AnnotationHandlers(MessageContext mc) { this.mc = mc; }

  public AnnotationHandlers register(String name, AnnotationHandler handler) {
    if (handler == null || name == null || handlers.containsKey(name)) {
      throw new IllegalArgumentException(name);
    }
    handlers.put(name, handler);
    return this;
  }

  public AnnotationHandler handlerFor(Annotation a) {
    return delayCalls(rawHandlerFor(a));
  }

  private AnnotationHandler rawHandlerFor(Annotation a) {
    if (a instanceof TextAnnotation) { return new TextAnnotationHandler(); }
    AnnotationHandler h = handlers.get(a.getValue());
    return h != null ? h : new UnrecognizedAnnotationHandler();
  }


  // Define default annotation handlers
  {
    register(
        "author",
        procedure(
            requireBlock(),
            oneOf(
                htmlLink("mailto", "http", "https"),  // link to homepage
                person())));  // A name and/or email addy
    register(
        "code",
        procedure(
            requireInline(),
            htmlElement(concat(), "code", "class=\"prettyprint\"", "${0}")));
    register("constructor", booleanHandler());
    register("deprecated", booleanHandler());
    register("define", unimplemented());  // TODO: implement
    register("desc", unimplemented());  // TODO: implement
    register(
        "enum",
        procedure(requireBlock(), split(Pair.pair("type", type()))));
    // TODO: add check to make sure that target is a function
    register("extends", procedure(requireBlock(), globalName()));
    register(
        "fileoverview",
        procedure(
            requireBlock(),
            concat()));
    register("final", booleanHandler());
    register("hidden", unimplemented());  // TODO: implement
    register("inheritDoc", unimplemented());  // TODO: implement
    register(
        "link",
        procedure(
            requireInline(),
            htmlElement(
                oneOf(
                    tee(docLink(), concat()),
                    list(docLink(), concat())),
                "a", "href=\"${0}\"", "${1}")));
    register("namespace", booleanHandler());
    register("notypecheck", booleanHandler());
    register("override", booleanHandler());
    register("overrides", series(docLink()));
    register(
        "param",
        procedure(
            requireBlock(),
            oneOf(
               split(
                   Pair.pair("type", type()),
                   Pair.pair("name", requireParamName(identifier())),
                   Pair.pair("summary", concat())),
               split(
                   Pair.pair("name", requireParamName(identifier())),
                   Pair.pair("summary", concat())))));
    register("provides", series(docLink()));
    register("requires", series(docLink()));
    register(
        "return",
        procedure(
            requireBlock(),
            oneOf(
               split(
                   Pair.pair("type", type()),
                   Pair.pair("summary", concat())),
               split(
                   Pair.pair("summary", concat())))));
    register("private", booleanHandler());
    register("protected", booleanHandler());
    register("public", booleanHandler());
    register(
        "see",
        procedure(
            requireBlock(),
            oneOf(
                split(Pair.pair("url", docLink())),
                htmlLink("http", "https", "mailto"))));
    register(
        "this",
        procedure(
            requireBlock(),
            type()));
    register(
        "throws",
        procedure(
            requireBlock(),
            oneOf(
               split(
                   Pair.pair("type", type()),
                   Pair.pair("summary", concat())),
               split(
                   Pair.pair("summary", concat())))));
    register(
        "type",
        procedure(
            requireBlock(),
            type()));
    register(
        "updoc",
        procedure(
            requireInline(),
            updoc()));

    // Fake entry for the description and summary
    register("::description", procedure(requireBlock(), concat()));
  }

  private static class TextAnnotationHandler implements AnnotationHandler {
    public Expression handle(Annotation a, MessageQueue mq) {
      return stringFrom(a, a.getValue());
    }
  }

  private class UnrecognizedAnnotationHandler implements AnnotationHandler {
    public Expression handle(Annotation a, MessageQueue mq) {
      mq.addMessage(
          JsdocMessageType.UNRECOGNIZED_ANNOTATION,
          a.getFilePosition(), MessagePart.Factory.valueOf(a.getValue()));
      // Look through the list of registered names to generate a
      // "did you mean" message.
      String match = closestMatch(a.getValue(), handlers.keySet());
      if (match != null) {
        mq.addMessage(
            JsdocMessageType.DID_YOU_MEAN, a.getFilePosition(),
            MessagePart.Factory.valueOf(match),
            MessagePart.Factory.valueOf(a.getValue()));
      }
      return null;
    }
  }

  private static String closestMatch(String target, Set<String> candidates) {
    // TODO: implement proper Levenshtein edit-distance
    // For now, this just does longest-prefix.
    // See StringUtils.getLevenshteinDistance at
    // http://commons.apache.org/lang/api-release/index.html
    int longestPrefix = 0;
    String best = null;
    for (String candidate : candidates) {
      if (candidate.length() <= longestPrefix) { continue; }
      int i = 0;
      for (int n = Math.min(target.length(), candidate.length()); i < n; ++i) {
        if (target.charAt(i) != candidate.charAt(i)) { break; }
      }
      if (i > longestPrefix) {
        longestPrefix = i;
        best = candidate;
      }
    }
    return best;
  }

  /**
   * Fake value used to indicate that processing passed but that no useful
   * value was returned.  See {@link #procedure} for a handler that ignore
   * the successful result of some sub-handlers.
   */
  private static final Expression OK = new NullLiteral(FilePosition.UNKNOWN);

  /**
   * Handles an annotation where no value is expected.  The presence of the
   * annotation is the only useful info.
   */
  private AnnotationHandler booleanHandler() {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        if (a instanceof InlineAnnotation) {
          mq.addMessage(
              JsdocMessageType.ANNOTATION_OUT_OF_PLACE, a.getFilePosition(),
              toMessagePart(a));
          return null;
        }
        for (Annotation child : a.children()) {
          if (!(child instanceof TextAnnotation &&
                "".equals(child.getValue().trim()))) {
            mq.addMessage(
                JsdocMessageType.UNEXPECTED_CONTENT, a.getFilePosition(),
                toMessagePart(a));
            return null;
          }
        }
        return new BooleanLiteral(a.getFilePosition(), true);
      }
    };
  }

  /**
   * Applies this {AnnotationHandler}'s handlers to children and concatenates
   * the result as a string.
   */
  private AnnotationHandler concat() {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        List<Expression> exprs = applyChildren(a, mq);
        if (exprs == null) { return null; }
        if (exprs.isEmpty()) {
          mq.addMessage(
              JsdocMessageType.EXPECTED_DOCUMENTATION_TEXT,
              a.getFilePosition(), toMessagePart(a));
          return null;
        }
        Expression e = null;
        for (Expression e2 : exprs) { e = concatenate(e, e2); }
        return e;
      }
    };
  }

  private AnnotationHandler delayCalls(final AnnotationHandler handler) {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        Expression e = handler.handle(a, mq);
        if (e == null) { return null; }
        if (hasCall(e)) {
          return (Expression) QuasiBuilder.substV(
              "("
              + "function (docRoot, apiElementName) {"
              + "  var apiElement = this;"
              + "  return @e;"
              + "})",
              "e", e);
        } else {
          return e;
        }
      }
      private boolean hasCall(Expression e) {
        System.err.println("hasCall " + e);
        if (e instanceof FunctionConstructor) { return false; }
        if (Operation.is(e, Operator.FUNCTION_CALL)) { return true; }
        for (ParseTreeNode child : e.children()) {
          if (child instanceof Expression && hasCall((Expression) child)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  private static final Pattern QUALIFIED_NAME_PATTERN = Pattern.compile(
      "(?<![\\p{Digit}])"  // Does not start with a number
      + "[_\\p{L}$][\\p{Alnum}$_]*"  // Identifiers separated by dots
      + "(?:\\s*\\.\\s*[_\\p{L}$][\\p{Alnum}$_]*)*");
  private static final Set<String> SCHEMES = Sets.newHashSet("http", "https");
  /**
   * A handler that produces a link to an API element if the annotation is
   * a javascript LHS, or a link if the input is a URL.
   */
  private AnnotationHandler docLink() {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        String value = contentAsString(a, mq);
        if (value == null) { return null; }
        value = value.trim();
        // If it looks like a qualified name, assume it is.
        if (QUALIFIED_NAME_PATTERN.matcher(value).matches()) {
          Expression target = toIdentifierChain(value, a.getFilePosition(), mq);
          if (target != null) {
            return (Expression) QuasiBuilder.substV(
                ""
                + "jsdoc___.linkTo("
                + "    apiElement, apiElementName, @target, @name, @pos?)",
                "target", target,
                "name", stringFrom(a, value),
                "pos", stringFrom(a, format(a.getFilePosition())));
          }
        }
        // Else if it looks like a URL, assume it is.
        if (value.indexOf('/') >= 0 || value.indexOf(':') > 0
            || value.indexOf('#') >= 0 || value.endsWith(".js")) {
          URI uri = parseDocUri(value, SCHEMES, a.getFilePosition(), mq);
          if (uri != null) {
            return stringFrom(a, uri.toString());
          }
        }
        mq.addMessage(
            JsdocMessageType.EXPECTED_URL_OR_REFERENCE, a.getFilePosition(),
            toMessagePart(a));
        return null;
      }
    };
  }

  /**
   * Returns the global name of the symbol pointed to by the given qualified
   * identifier in the local context.
   */
  private AnnotationHandler globalName() {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        String value = contentAsString(a, mq);
        if (value == null) { return null; }
        value = value.trim();
        if (value.startsWith("{") && value.endsWith("}")) {
          value = value.substring(1, value.length() - 1).trim();
        }
        Expression e = toIdentifierChain(value, a.getFilePosition(), mq);
        if (e == null) { return null; }
        return (Expression) QuasiBuilder.substV("jsdoc___.nameOf(@e)", "e", e);
      }
    };
  }

  /**
   * Produces a string of HTML text.
   * @param bodyHandler an annotation that is applied to the annotation to
   *     produce a value that is used to fill placeholders.
   *     If the bodyHandler returns an ArrayConstructor then placeholders are
   *     resolved by mapping placeholder indices to element indices.
   *     Otherwise, item 0 maps to the result.
   *     If this handler fails, then the htmlElement handler fails.
   * @param tagName an HTML element name.
   * @param attribAndBody an array like ['foo="bar ${0}"', '${1}'] where
   *     all but the last are HTML attributes, and the last is the content
   *     that appears between the start and end tags.  The ${...}}} constructs
   *     are placeholders, and are replaced with portions of bodyHandler's
   *     return value.
   */
  private AnnotationHandler htmlElement(
      final AnnotationHandler bodyHandler, String tagName,
      String... attribAndBody) {
    List<String> attribs = Arrays.asList(attribAndBody);
    attribs = attribs.subList(0, attribs.size() - 1);
    String body = attribAndBody[attribs.size()];
    final String tmpl = (
        "<" + tagName
        + (attribs.size() == 0 ? "" : " " + Join.join(" ", attribs))
        + ">" + body + "</" + tagName + ">");
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        Expression e = bodyHandler.handle(a, mq);
        if (e == null) { return null; }
        List<? extends Expression> parts;
        if (e instanceof ArrayConstructor) {
          parts = ((ArrayConstructor) e).children();
        } else {
          parts = Collections.singletonList(e);
        }

        // Replace ${1} in the template with the corresponding part
        Expression element = null;
        int done = 0;
        for (int st, et; (st = tmpl.indexOf("${", done)) > 0; done = et + 1) {
          et = tmpl.indexOf('}', st);
          int index = Integer.parseInt(tmpl.substring(st + 2, et));
          element = concatenate(
              element, stringFrom(a, tmpl.substring(done, st)));
          Expression html = (Expression) QuasiBuilder.substV(
              "jsdoc___.html(@e)", "e", parts.get(index));
          element = concatenate(element, html);
        }
        return concatenate(
            element, stringFrom(a, tmpl.substring(done)));
      }
    };
  }

  /** Matches an HTML link, an A element, and extracts the HREF and body */
  private AnnotationHandler htmlLink(String... schemes) {
    final Set<String> schemeSet = new HashSet<String>(Arrays.asList(schemes));
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        String content = contentAsString(a, mq);
        if (content == null) { return null; }
        if (content.startsWith("<")) {
          FilePosition p = a.getFilePosition();
          Element link;
          try {
            CharProducer cp = CharProducer.Factory.create(
                new StringReader(content), p);
            HtmlLexer lexer = new HtmlLexer(cp);
            Document doc = DomParser.makeDocument(null, null);
            DomParser parser = new DomParser(lexer, p.source(), mq);
            DocumentFragment f = parser.parseFragment(doc);
            parser.getTokenQueue().expectEmpty();
            Node fFirst = f.getFirstChild();
            if (fFirst == null
                || !(fFirst instanceof Element)
                || !"a".equals(fFirst.getNodeName())) {
              link = null;
            } else {
              link = (Element) fFirst;
            }
          } catch (ParseException ex) {
            ex.toMessageQueue(mq);
            return null;
          }
          if (link != null) {
            Attr href = link.getAttributeNode("href");
            if (href != null) {
              URI uri = parseDocUri(
                  href.getValue(), schemeSet,
                  Nodes.getFilePositionForValue(href), mq);
              if (uri != null) {
                StringBuilder name = new StringBuilder();
                Concatenator tc = new Concatenator(name, null);
                RenderContext rc = new RenderContext(tc);
                for (Node child : Nodes.childrenOf(link)) {
                  Nodes.render(child, rc);
                }
                tc.noMoreTokens();
                return (Expression) QuasiBuilder.substV(
                    "({ 'name': @name, 'url': @url })",
                    "name", stringFrom(a, name.toString()),
                    "url", stringFrom(a, uri.toString()));
              }
            }
          }
        }
        mq.addMessage(
            JsdocMessageType.BAD_LINK, a.getFilePosition(),
            MessagePart.Factory.valueOf(content));
        return null;
      }
    };
  }

  private static final Pattern IDENTIFIER = Pattern.compile("^[\\p{L}_$]\\w*$");
  /** Matches and returns a javascript identifier. */
  private AnnotationHandler identifier() {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        String value = contentAsString(a, mq);
        if (value == null) { return null; }
        if (!IDENTIFIER.matcher(value).matches()) {
          mq.addMessage(
              JsdocMessageType.EXPECTED_IDENTIFIER, a.getFilePosition(),
              toMessagePart(a));
          return null;
        }
        return stringFrom(a, value);
      }
    };
  }

  private static final Pattern COMMENT_TOKEN = Pattern.compile(
      "^\\s*([/\\w\\.\\-#@?&:]+(?=\\s|$)|\\{[^}]*\\})\\s*");
  /**
   * Given n annotation handlers, breaks the annotation's initial text part
   * into (n-1) tokens which are either whitespace delimited, or a block of
   * non-nesting text inside <code>{...}</code>.
   * The last handler is applied to the remainder of the annotation, which
   * need not be textual.
   * The result is returned as a {@link ArrayConstructor javascript array}.
   */
  private AnnotationHandler list(final AnnotationHandler... itemHandlers) {
    assert itemHandlers.length > 0;
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        if (!(a.children().get(0) instanceof TextAnnotation)) {
          mq.addMessage(
              JsdocMessageType.EXPECTED_DOCUMENTATION_TEXT,
              a.getFilePosition(), toMessagePart(a));
          return null;
        }
        TextAnnotation first = (TextAnnotation) a.children().get(0);
        String value = first.getValue();
        int last = itemHandlers.length - 1;
        List<Expression> items = new ArrayList<Expression>();
        int consumed = 0;
        for (int i = 0; i < last; ++i) {
          Matcher m = COMMENT_TOKEN.matcher(value.substring(consumed));
          if (!m.find()) {
            TextAnnotation tail = first.slice(consumed, value.length());
            mq.addMessage(
                JsdocMessageType.EXPECTED_DOCUMENTATION_TEXT,
                tail.getFilePosition(),
                MessagePart.Factory.valueOf('"' + tail.getValue() + '"'));
            return null;
          }
          TextAnnotation tok = first.slice(
              consumed + m.start(1), consumed + m.end(1));
          Expression item = itemHandlers[i].handle(tok, mq);
          if (item == null) { return null; }
          items.add(item);
          consumed += m.end();
        }
        List<? extends Annotation> children = a.children();
        TextAnnotation unusedText = first.slice(consumed, value.length());
        Annotation tail;
        if (a instanceof BlockAnnotation) {
          List<Annotation> newChildren = new ArrayList<Annotation>();
          newChildren.add(unusedText);
          newChildren.addAll(children.subList(1, children.size()));
          tail = new BlockAnnotation(
              a.getValue(), newChildren,
              FilePosition.span(
                  unusedText.getFilePosition(), a.getFilePosition()));
        } else {
          tail = new InlineAnnotation(
              a.getValue(), unusedText, unusedText.getFilePosition());
        }
        Expression item = itemHandlers[last].handle(tail, mq);
        if (item == null) { return null; }
        items.add(item);
        return new ArrayConstructor(a.getFilePosition(), items);
      }
    };
  }

  /**
   * Applies options in turn, returning the result of the first to succeed
   * and failing if all fail.
   * This suppresses error messages output by failing handlers.
   */
  private AnnotationHandler oneOf(final AnnotationHandler... options) {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        int mqCount = mq.getMessages().size();
        for (AnnotationHandler option : options) {
          mq.getMessages().subList(mqCount, mq.getMessages().size()).clear();
          Expression e = option.handle(a, mq);
          if (e != null) { return e; }
        }
        return null;
      }
    };
  }

  /**
   * Applies handlers to the same annotation in series, failing if any fail,
   * and returning the result of the last if all succeed.
   */
  private AnnotationHandler procedure(final AnnotationHandler... handlers) {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        Expression e = null;
        for (AnnotationHandler handler : handlers) {
          e = handler.handle(a, mq);
          if (e == null) { return null; }
        }
        return e;
      }
    };
  }

  /**
   * Fails if the given annotation is not a {@link BlockAnnotation}.  If it is
   * returns a non-null but useless value.  May be used in a {@link #procedure}
   * to cause the procedure to fail.
   */
  private AnnotationHandler requireBlock() {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        if (a instanceof BlockAnnotation) { return OK; }
        mq.addMessage(
            JsdocMessageType.ANNOTATION_OUT_OF_PLACE, a.getFilePosition(),
            toMessagePart(a));
        return null;
      }
    };
  }

  /**
   * Fails if the given annotation is not a {@link InlineAnnotation}.  If it is
   * returns a non-null but useless value.  May be used in a {@link #procedure}
   * to cause the procedure to fail.
   */
  private static AnnotationHandler requireInline() {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        if (a instanceof InlineAnnotation) { return OK; }
        mq.addMessage(
            JsdocMessageType.ANNOTATION_OUT_OF_PLACE, a.getFilePosition(),
            toMessagePart(a));
        return null;
      }
    };
  }

  /**
   * Add a function that is called at runtime to issue an error message if
   * the parameter name is not part of the function object.
   */
  private AnnotationHandler requireParamName(final AnnotationHandler name) {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        Expression e = name.handle(a, mq);
        if (e == null) { return null; }
        return (Expression) QuasiBuilder.substV(
            ""
            + "jsdoc___.requireParam("
            + "    apiElement, apiElementName,"
            + "     jsdoc___.resolvePromise("
            + "        @name, docRoot, apiElementName, apiElement))",
            "name", e);
      }
    };
  }

  private AnnotationHandler series(final AnnotationHandler ah) {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        String value = contentAsString(a, mq);
        if (value == null) { return null; }
        List<Expression> elements = new ArrayList<Expression>();
        boolean nullPart = false;
        for (String part : value.split("[\\s,]+")) {
          Expression e = ah.handle(
              new TextAnnotation(part, a.getFilePosition()), mq);
          if (e != null) {
            elements.add(e);
          } else {
            nullPart = true;
          }
        }
        if (nullPart) { return null; }
        return (Expression) QuasiBuilder.substV(
            "@arr.join(' ')",
            "arr", new ArrayConstructor(a.getFilePosition(), elements));
      }
    };
  }

  /**
   * Treats content as {@link Updoc} and returns a function that will execute
   * the tests contained therein/
   */
  private AnnotationHandler updoc() {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        String value = contentAsString(a, mq);
        if (value == null) { return null; }
        try {
          Updoc updoc = new UpdocParser(mq).parseComplete(
              CharProducer.Factory.create(
                  new StringReader(value), a.getFilePosition()));
          List<ObjectConstructor> runs = new ArrayList<ObjectConstructor>();
          for (Updoc.Run run : updoc.getRuns()) {
            runs.add((ObjectConstructor) QuasiBuilder.substV(
                ""
                + "({ doc:    @runSource,"
                + "   input:  function () { return @input; },"
                + "   result: function () { return @result; },"
                + "   pos:    @runPos })",
                "runSource", stringFrom(a, render(run, false)),
                "input", run.getInput(),
                "result", run.getResult(),
                "runPos", stringFrom(a, format(run.getFilePosition()))
                ));
          }

          return (Expression) QuasiBuilder.substV(
              "jsdoc___.updoc([@runs*])",
              "runs", new ParseTreeNodeContainer(runs));
        } catch (ParseException ex) {
          ex.toMessageQueue(mq);
          return null;
        }
      }
    };
  }

  private AnnotationHandler split(Pair<String, AnnotationHandler> a) {
    return split(Collections.singletonList(a));
  }

  private AnnotationHandler split(
      Pair<String, AnnotationHandler> a,
      Pair<String, AnnotationHandler> b) {
    List<Pair<String, AnnotationHandler>> pairs
        = new ArrayList<Pair<String, AnnotationHandler>>();
    pairs.add(a);
    pairs.add(b);
    return split(pairs);
  }

  private AnnotationHandler split(
      Pair<String, AnnotationHandler> a,
      Pair<String, AnnotationHandler> b,
      Pair<String, AnnotationHandler> c) {
    List<Pair<String, AnnotationHandler>> pairs
        = new ArrayList<Pair<String, AnnotationHandler>>();
    pairs.add(a);
    pairs.add(b);
    pairs.add(c);
    return split(pairs);
  }

  private AnnotationHandler split(
      List<Pair<String, AnnotationHandler>> entries) {
    final List<Pair<String, AnnotationHandler>> tokenEntries
        = entries.subList(0, entries.size() - 1);
    final Pair<String, AnnotationHandler> remainder
        = entries.get(entries.size() - 1);
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        List<Expression> parts = applyChildren(a, mq);
        if (parts == null) { return null; }
        if (parts.isEmpty() || !(parts.get(0) instanceof StringLiteral)) {
          mq.addMessage(
              JsdocMessageType.EXPECTED_DOCUMENTATION_TEXT,
              a.getFilePosition(), toMessagePart(a));
          return null;
        }

        List<Pair<Literal, Expression>> mapEntries
            = new ArrayList<Pair<Literal, Expression>>();

        StringLiteral sl = (StringLiteral) parts.get(0);
        FilePosition slpos = sl.getFilePosition();
        String s = sl.getUnquotedValue();
        int consumed = 0;
        for (Pair<String, AnnotationHandler> entry : tokenEntries) {
          Matcher m = COMMENT_TOKEN.matcher(s.substring(consumed));
          if (!m.find()) { return null; }
          Expression value = entry.b.handle(
              TextAnnotation.slice(
                  s, slpos, consumed + m.start(1), consumed + m.end(1)),
                  mq);
          if (value == null) { return null; }
          mapEntries.add(
              Pair.pair((Literal) stringFrom(a, entry.a), value));
          // Consume the beginning by blanking it out
          consumed += m.end();
        }
        List<Annotation> tailMembers = new ArrayList<Annotation>(a.children());
        tailMembers.set(
            0, TextAnnotation.slice(s, slpos, consumed, s.length()));
        BlockAnnotation block = new BlockAnnotation(
            a.getValue(), tailMembers,
            FilePosition.span(
                tailMembers.get(0).getFilePosition(),
                a.getFilePosition()));
        Expression value = remainder.b.handle(block, mq);
        if (value == null) { return null; }
        mapEntries.add(
            Pair.pair((Literal) stringFrom(a, remainder.a), value));
        return new ObjectConstructor(a.getFilePosition(), mapEntries);
      }
    };
  }

  /**
   * A handler that applies each of handlers to the same input and returns an
   * array of the results, failing if any of handlers fail.
   */
  private AnnotationHandler tee(final AnnotationHandler... handlers) {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        List<Expression> results = new ArrayList<Expression>();
        for (AnnotationHandler h : handlers) {
          Expression e = h.handle(a, mq);
          if (e == null) { return null; }
          results.add(e);
        }
        return new ArrayConstructor(a.getFilePosition(), results);
      }
    };
  }

  private static final Set<String> BUILTIN_TYPE_IDENT = new HashSet<String>(
      Arrays.asList("boolean", "number", "object", "string", "undefined"));
  private AnnotationHandler type() {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        String s = contentAsString(a, mq);
        if (s != null) {
          s = s.trim();
          // TODO: parse @type and @this annotations even without {}s
          if (s.startsWith("{") && s.endsWith("}")) {
            String typeString = s.substring(1, s.length() - 1).trim();
            List<Expression> symbolNames = new ArrayList<Expression>();
            Matcher m = QUALIFIED_NAME_PATTERN.matcher(typeString);
            while (m.find()) {
              String ident = m.group(0);
              if ("".equals(ident) || BUILTIN_TYPE_IDENT.contains(ident)
                  || Keyword.isKeyword(ident)) {
                continue;
              }
              Expression ref = toIdentifierChain(
                  ident, a.getFilePosition(), mq);
              if (ref == null) { return null; }
              symbolNames.add(
                  (Expression) QuasiBuilder.substV(
                      "function () { return @ref; }", "ref", ref));
              symbolNames.add(stringFrom(a, ident));
            }
            StringLiteral type = stringFrom(a, typeString);
            if (symbolNames.isEmpty()) { return type; }
            return (Expression) QuasiBuilder.substV(
                "jsdoc___.requireTypeAtoms(@pos, [@symbols*]), @type",
                "pos", stringFrom(a, format(a.getFilePosition())),
                "symbols", new ParseTreeNodeContainer(symbolNames),
                "type", type);
          }
        }
        mq.addMessage(
            JsdocMessageType.EXPECTED_TYPE, a.getFilePosition(),
            MessagePart.Factory.valueOf(s));
        return null;
      }
    };
  }

  private static final String NAME = "[^\\s<>()@][^<>()@]*[^\\s<>()@]+";
  private static final String EMAIL = "[\\w.-]+@(?:\\w+(?:\\.\\w+)*)";
  private static final Pattern EMAIL_OR_NAME = Pattern.compile(
      "^\\s*(?:"
      + "(?:(" + NAME + ")\\s*)?(?:[<(]\\s*(" + EMAIL + ")\\s*[)>])?"
      + "|(" + EMAIL + ")(?:\\s*\\(\\s*(" + NAME + ")\\s*\\))?"
      + ")\\s*$");
  private AnnotationHandler person() {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        String value = contentAsString(a, mq);
        if (value == null) { return null; }
        Matcher m = EMAIL_OR_NAME.matcher(value);
        if (m.matches()) {
          String name = m.group(1);
          String email = m.group(2);
          if (email == null) { email = m.group(3); }
          if (name == null) { name = m.group(4); }
          URI uri = null;
          if (email != null) {
            uri = parseDocUri(
                email, Collections.singleton("mailto"), a.getFilePosition(),
                mq);
            if (uri == null) { return null; }
            if (!uri.isAbsolute()) {
              mq.addMessage(
                  JsdocMessageType.EXPECTED_EMAIL_OR_NAME,
                  a.getFilePosition(), MessagePart.Factory.valueOf(value));
              return null;
            }
          }
          if (name != null || uri != null) {
            List<Pair<Literal, Expression>> entries
                = new ArrayList<Pair<Literal, Expression>>();
            if (name != null) {
              entries.add(Pair.<Literal, Expression>pair(
                  stringFrom(a, "name"), stringFrom(a, name)));
            }
            if (uri != null) {
              entries.add(Pair.<Literal, Expression>pair(
                  stringFrom(a, "url"),
                  stringFrom(a, "" + uri)));
            }
            return new ObjectConstructor(a.getFilePosition(), entries);
          }
        }
        mq.addMessage(
            JsdocMessageType.EXPECTED_EMAIL_OR_NAME,
            a.getFilePosition(), MessagePart.Factory.valueOf(value));
        return null;
      }
    };
  }

  /**
   * Emits the empty string.
   */
  private AnnotationHandler unimplemented() {
    return new AnnotationHandler() {
      public Expression handle(Annotation a, MessageQueue mq) {
        return stringFrom(a, "");
      }
    };
  }


  private String contentAsString(
      Annotation a, MessageQueue mq) {
    if (a instanceof TextAnnotation) { return a.getValue(); }
    StringBuilder sb = new StringBuilder();
    for (Annotation child : a.children()) {
      if (!(child instanceof TextAnnotation)) {
        mq.addMessage(
            JsdocMessageType.EXPECTED_DOCUMENTATION_TEXT,
            child.getFilePosition(), toMessagePart(a));
        return null;
      }
      sb.append(child.getValue());
    }
    return sb.toString().trim();
  }

  private List<Expression> applyChildren(
      Annotation a, MessageQueue mq) {
    List<? extends Annotation> children = a.children();
    List<Expression> exprs = new ArrayList<Expression>();
    Expression last = null;
    for (Annotation child : children) {
      Expression e = rawHandlerFor(child).handle(child, mq);
      if (e == null) { return null; }
      if (e instanceof StringLiteral && last instanceof StringLiteral) {
        exprs.remove(exprs.size() - 1);
        last = stringFrom(a,
            ((StringLiteral) last).getUnquotedValue()
            + ((StringLiteral) e).getUnquotedValue());
      } else {
        last = e;
      }
      exprs.add(last);
    }
    return exprs;
  }

  private Expression toIdentifierChain(
      String s, FilePosition pos, MessageQueue mq) {
    String[] parts = s.split("\\s*\\.\\s*");
    Expression e = null;
    for (String part : parts) {
      if (!IDENTIFIER.matcher(part).matches()
          || (Keyword.isKeyword(part)
              && !Keyword.THIS.toString().equals(part))) {
        mq.addMessage(
            JsdocMessageType.EXPECTED_IDENTIFIER, pos,
            MessagePart.Factory.valueOf("'" + s + "'"));
        return null;
      }
      Reference r = new Reference(new Identifier(pos, part));
      e = e == null ? r : Operation.createInfix(Operator.MEMBER_ACCESS, e, r);
    }
    return e;
  }

  private static Expression concatenate(Expression a, Expression b) {
    if (a == null) { return b; }
    return Operation.createInfix(Operator.ADDITION, a, b);
  }

  /**
   * Coerces a user-supplied URL-like string to a URL, issuing an error message
   * if it could not be done, or if the result uses an unrecognized scheme.
   */
  private URI parseDocUri(
      String s, Set<String> schemes, FilePosition p, MessageQueue mq) {
    if (!"".equals(s)) {
      if (s.indexOf('@') >= 0 && s.indexOf(':') < 0 && s.indexOf('/') < 0
          && s.indexOf('#') < 0 && schemes.contains("mailto")) {
        s = "mailto:" + s;
      }
      try {
        URI uri = new URI(s);
        if (!uri.isAbsolute()) { return uri; }
        String scheme = uri.getScheme().toLowerCase(Locale.ENGLISH);
        if (schemes.contains(scheme)) { return uri; }
      } catch (URISyntaxException ex) {
        // Message issued below.
      }
    }
    mq.addMessage(
        JsdocMessageType.BAD_LINK, p, MessagePart.Factory.valueOf(s));
    return null;
  }

  private String format(FilePosition pos) {
    return JsdocRewriter.format(pos, mc);
  }

  private String render(ParseTreeNode n, boolean minimal) {
    StringBuilder sb = new StringBuilder();
    TokenConsumer tc = minimal
        ? new JsMinimalPrinter(new Concatenator(sb))
        : n.makeRenderer(sb, null);
    RenderContext rc = new RenderContext(tc);
    n.render(rc);
    tc.noMoreTokens();
    return sb.toString();
  }

  private static MessagePart toMessagePart(Annotation a) {
    if (a instanceof TextAnnotation) {
      String value = a.getValue();
      if (value.length() > 40) {
        value = value.substring(0, 37) + "...";
      }
      return MessagePart.Factory.valueOf("'" + value + "'");
    }
    return MessagePart.Factory.valueOf("@" + a.getValue());
  }

  private static StringLiteral stringFrom(Annotation a, CharSequence s) {
    return StringLiteral.valueOf(a.getFilePosition(), s);
  }
}
