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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.ancillary.jsdoc.HtmlRenderer;
import com.google.caja.ancillary.jsdoc.Jsdoc;
import com.google.caja.ancillary.jsdoc.JsdocException;
import com.google.caja.ancillary.linter.Linter;
import com.google.caja.ancillary.opt.JsOptimizer;
import com.google.caja.lang.css.CssSchema;
import com.google.caja.lang.html.HTML;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.HtmlLexer;
import com.google.caja.lexer.HtmlTokenType;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Punctuation;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.AttribKey;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.ElKey;
import com.google.caja.parser.html.HtmlQuasiBuilder;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Statement;
import com.google.caja.plugin.CssPropertyPartType;
import com.google.caja.plugin.CssRewriter;
import com.google.caja.plugin.CssValidator;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.plugin.stages.EmbeddedContent;
import com.google.caja.plugin.stages.HtmlEmbeddedContentFinder;
import com.google.caja.render.Concatenator;
import com.google.caja.render.CssMinimalPrinter;
import com.google.caja.render.CssPrettyPrinter;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageTypeInt;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;
import com.google.caja.util.Multimap;
import com.google.caja.util.Multimaps;
import com.google.caja.util.Name;
import com.google.caja.util.Sets;
import com.google.caja.util.Strings;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * The tools servlet's transformation engine.
 *
 * @author mikesamuel@gmail.com
 */
class Processor {
  private final Request req;
  private final MessageQueue mq;

  Processor(Request req, MessageQueue mq) {
    this.req = req;
    this.mq = mq;
  }

  /** Produce a list of output jobs from a list of input jobs. */
  List<Job> process(List<Job> inputJobs) throws IOException {
    List<Job> jobs = Lists.newArrayList();
    // Pull JS out of <script> elements, and similarly for style, and pull
    // JS and CSS out of onclick and style attributes.
    for (Job job : inputJobs) {
      jobs.addAll(extractJobs(job));
    }

    if (req.lint) { lint(jobs); }

    if (req.opt) { optimize(jobs); }

    // Reverse of extractJobs.
    // Put optimized JS back into the script element from which it came, and
    // similarly for style elements and attributes.
    if (req.minify || req.opt) {
      reincorporateExtracted(jobs);
    }

    List<Job> output = Lists.newArrayList();
    switch (req.verb) {
      case DOC:
        try {
          output.add(doc(jobs, req, mq));
        } catch (JsdocException ex) {
          ex.toMessageQueue(mq);
        }
        break;
      case LINT:
        output.add(Job.html(LintPage.render(reduce(jobs), req, mq), null));
        break;
      default:
        for (Job job : jobs) {
          if (job.origin == null) { output.add(job); }
        }
        break;
    }

    // Filter out some messages from the CssValidator and other linty bits.
    removeCajolerSpecificMessages(mq);

    return output;
  }

  /**
   * Boil multiple jobs down into a single output.  This may involve
   * concatenating jobs of the same type or combining heterogeneous types into
   * a single HTML file.
   */
  Content reduce(List<Job> jobs) {
    ContentType otype = req.otype;
    if (otype == null) {  // Guess if none was specified.
      ContentType commonType = null;
      for (Job job : jobs) {
        if (commonType == null) {
          commonType = job.t;
        } else if (commonType != job.t) {
          commonType = null;
          break;
        }
      }
      otype = commonType != null ? commonType : ContentType.HTML;
    }

    // Do we need to combine everything into a single HTML file.
    if (otype != ContentType.XML && otype != ContentType.HTML) {
      for (Job job : jobs) {
        if (job.t != otype) {
          mq.addMessage(
              CajaWebToolsMessageType.INCOMPATIBLE_OUTPUT_TYPE,
              MessagePart.Factory.valueOf(job.t.name()),
              MessagePart.Factory.valueOf(otype.name()));
          otype = ContentType.HTML;
          break;
        }
      }
    }

    // If the output is not textual, we're done.
    if (!otype.isText) {
      if (jobs.size() != 1) { throw new AssertionError(); }
      return new Content((byte[]) jobs.get(0).root, otype);
    }

    // Format each of the jobs using the preferences in Request.
    StringBuilder outBuf = new StringBuilder();
    RenderContext out = makeRenderContext(outBuf, otype);
    switch (otype) {
      case XML:
      case HTML:
        HtmlQuasiBuilder b = HtmlQuasiBuilder.getBuilder(
            DomParser.makeDocument(null, null));
        DocumentFragment f = b.getDocument().createDocumentFragment();
        for (Job job : jobs) {
          Node toAdd;
          switch (job.t) {
            case XML: case HTML:
              toAdd = f.getOwnerDocument().importNode(
                  (DocumentFragment) job.root, true);
              break;
            case JS: {
              StringBuilder sb = new StringBuilder();
              RenderContext rc = makeRenderContext(sb, ContentType.JS)
                  .withEmbeddable(true);
              ((Block) job.root).renderBody(rc);
              rc.getOut().noMoreTokens();
              toAdd = b.substV("<script>@js</script>",
                               "js", sb.toString());
              break;
            }
            case CSS: {
              StringBuilder sb = new StringBuilder();
              RenderContext rc = makeRenderContext(sb, ContentType.CSS)
                  .withEmbeddable(true);
              ((CssTree.StyleSheet) job.root).render(rc);
              rc.getOut().noMoreTokens();
              toAdd = b.substV("<style>",
                               "css", sb.toString());
              break;
            }
            default:
              throw new AssertionError(job.t.name());
          }
          if (toAdd instanceof DocumentFragment) {
            for (Node child : Nodes.childrenOf(toAdd)) {
              f.appendChild(child);
            }
          } else {
            f.appendChild(toAdd);
          }
        }
        Nodes.render(f, out);
        break;
      case JS:
        List<Statement> stmts = Lists.newArrayList();
        for (Job job : jobs) { stmts.addAll(((Block) job.root).children()); }
        new Block(FilePosition.UNKNOWN, stmts).renderBody(out);
        break;
      case JSON:
        for (Job job : jobs) {
          ((Expression) job.root).render(out);
        }
        break;
      case CSS:
        for (Job job : jobs) {
          ((CssTree.StyleSheet) job.root).render(out);
        }
        break;
      case ZIP: default: throw new AssertionError(otype.name());
    }
    out.getOut().noMoreTokens();
    return new Content(outBuf.toString(), otype);
  }

  /** Parse a job from input parameters. */
  Job parse(CharProducer cp, ContentType contentType, Node src, URI baseUri)
      throws ParseException {
    FilePosition inputRange = cp.filePositionForOffsets(
        cp.getOffset(), cp.getLimit());
    InputSource is = inputRange.source();
    switch (contentType) {
      case HTML:
      case XML: {
        HtmlLexer lexer = new HtmlLexer(cp.clone());
        DomParser p;
        if (contentType == ContentType.HTML) {
          Token<HtmlTokenType> firstTag = null;
          while (lexer.hasNext()) {
            Token<HtmlTokenType> t = lexer.next();
            if (t.type == HtmlTokenType.TAGBEGIN) {
              firstTag = t;
              break;
            }
          }
          p = new DomParser(new HtmlLexer(cp), is, mq);
          if (firstTag != null
              && Strings.equalsIgnoreCase(firstTag.text, "<html")) {
            Element el = p.parseDocument();
            DocumentFragment f = el.getOwnerDocument().createDocumentFragment();
            f.appendChild(el);
            return Job.html(f, baseUri);
          }
        } else {
          lexer.setTreatedAsXml(contentType == ContentType.XML);
          TokenQueue<HtmlTokenType> tq = new TokenQueue<HtmlTokenType>(
              lexer, is);
          tq.setInputRange(inputRange);
          p = new DomParser(tq, contentType == ContentType.XML, mq);
        }
        return Job.html(p.parseFragment(), baseUri);
      }
      case JS: {
        JsLexer lexer = new JsLexer(cp);
        JsTokenQueue tq = new JsTokenQueue(lexer, is);
        if (tq.isEmpty()) {
          return Job.js(
              new Block(inputRange, Collections.<Statement>emptyList()), src,
              baseUri);
        }
        tq.setInputRange(inputRange);
        Block program = new Parser(tq, mq, false).parse();
        tq.expectEmpty();
        return Job.js(program, src, baseUri);
      }
      case JSON: {  // TODO: use a JSON only lexer.
        JsLexer lexer = new JsLexer(cp);
        JsTokenQueue tq = new JsTokenQueue(lexer, is);
        if (!tq.lookaheadToken(Punctuation.LCURLY)) {
          tq.expectToken(Punctuation.LCURLY);
        }
        tq.setInputRange(inputRange);
        Expression e = new Parser(tq, mq, false).parseExpressionPart(true);
        tq.expectEmpty();
        return Job.json((ObjectConstructor) e, baseUri);
      }
      case CSS: {
        TokenQueue<CssTokenType> tq = CssParser.makeTokenQueue(cp, mq, false);
        tq.setInputRange(inputRange);
        CssParser p = new CssParser(tq, mq, MessageLevel.WARNING);
        Job job;
        if (src instanceof Attr) {
          CssTree.DeclarationGroup dg = p.parseDeclarationGroup();
          job = Job.css(dg, (Attr) src, baseUri);
        } else {
          CssTree.StyleSheet ss = p.parseStyleSheet();
          job = Job.css(ss, (Element) src, baseUri);  // src may be null
        }
        tq.expectEmpty();
        return job;
      }
      default:
        throw new AssertionError(contentType.name());
    }
  }

  /** Make a renderer using the preferences specified in Request. */
  RenderContext makeRenderContext(StringBuilder out, ContentType ot) {
    Concatenator cat = new Concatenator(out);
    TokenConsumer tc;
    switch (ot) {
      case HTML: case XML:
        tc = cat;
        break;
      case CSS:
        if (req.minify) {
          tc = new CssMinimalPrinter(cat);
        } else {
          tc = new CssPrettyPrinter(cat);
        }
        break;
      case JS:
      case JSON:
        if (req.minify) {
          tc = new JsMinimalPrinter(cat);
        } else {
          tc = new JsPrettyPrinter(cat);
        }
        break;
      default:
        throw new AssertionError(ot.name());
    }
    RenderContext rc = new RenderContext(tc);
    rc = rc.withAsXml(ot == ContentType.XML);
    rc = rc.withAsciiOnly(req.asciiOnly);
    rc = rc.withJson(ot == ContentType.JSON);
    rc = rc.withRawObjKeys(req.minify);
    return rc;
  }

  /**
   * Pull the bodies of script and style elements out into their own jobs,
   * and similarly for event handlers and style attributes.
   */
  private List<Job> extractJobs(Job job) {
    List<Job> all = Lists.newArrayList(job);
    if (job.t == ContentType.XML || job.t == ContentType.HTML) {
      extractJobs((Node) job.root, job.baseUri, all);
    }
    return all;
  }

  private void extractJobs(Node node, URI baseUri, List<Job> out) {
    HtmlEmbeddedContentFinder f = new HtmlEmbeddedContentFinder(
        req.htmlSchema, req.baseUri, mq, req.mc);
    PluginEnvironment env = new PluginEnvironment() {
      public CharProducer loadExternalResource(
          ExternalReference ref, String mimeType) {
        return null;
      }

      public String rewriteUri(ExternalReference uri, String mimeType) {
        return null;
      }
    };
    for (EmbeddedContent c : f.findEmbeddedContent(node)) {
      if (c.getType() != null && c.getContentLocation() == null) {
        Node src = c.getSource();
        ParseTreeNode t;
        try {
          t = c.parse(env, mq);
        } catch (ParseException ex) {
          ex.toMessageQueue(mq);
          continue;
        }
        switch (c.getType()) {
          case JS:
            if (src instanceof Element) {
              out.add(Job.js((Block) t, (Element) src, baseUri));
            } else {
              out.add(Job.js((Block) t, (Attr) src, baseUri));
            }
            break;
          case CSS:
            if (src instanceof Element) {
              out.add(Job.css((CssTree.StyleSheet) t, (Element) src, baseUri));
            } else {
              out.add(Job.css(
                  (CssTree.DeclarationGroup) t, (Attr) src, baseUri));
            }
            break;
          default: throw new SomethingWidgyHappenedError();
        }
      }
    }
  }

  /** Find problems in code. */
  private void lint(List<Job> jobs) {
    List<Block> jsJobs = Lists.newArrayList();
    for (Job job : jobs) {
      switch (job.t) {
        case XML: case HTML:
          lintMarkup((DocumentFragment) job.root);
          break;
        case CSS:
          lintCss((CssTree) job.root);
          break;
        case JS:
          jsJobs.add((Block) job.root);
          break;
        case JSON: break;
        case ZIP: throw new IllegalArgumentException();
      }
    }
    lintJs(jsJobs);
  }

  private void lintMarkup(Node node) {
    if (node instanceof Element) {
      Element el = (Element) node;
      ElKey elKey = ElKey.forElement(el);
      HTML.Element elInfo = req.htmlSchema.lookupElement(elKey);
      if (elInfo == null) {
        mq.addMessage(
            CajaWebToolsMessageType.UNKNOWN_ELEMENT,
            Nodes.getFilePositionFor(el), elKey);
      }
      for (Attr a : Nodes.attributesOf(el)) {
        AttribKey aKey = AttribKey.forAttribute(elKey, a);
        HTML.Attribute aInfo = req.htmlSchema.lookupAttribute(aKey);
        if (aInfo == null) {
          FilePosition aPos = Nodes.getFilePositionFor(a);
          mq.addMessage(
              CajaWebToolsMessageType.UNKNOWN_ATTRIB, aPos, aKey, elKey);
        } else if (!aInfo.getValueCriterion().accept(a.getValue())) {
          FilePosition aPos = Nodes.getFilePositionForValue(a);
          mq.addMessage(
              CajaWebToolsMessageType.BAD_ATTRIB_VALUE, aPos, aKey,
              MessagePart.Factory.valueOf(a.getValue()));
        }
      }
    }
    for (Node child : Nodes.childrenOf(node)) { lintMarkup(child); }
  }

  private void lintCss(CssTree t) {
    CssValidator v = new CssValidator(req.cssSchema, req.htmlSchema, mq);
    v.validateCss(AncestorChain.instance(t));
  }

  private void lintJs(List<Block> programs) {
    if (programs.isEmpty()) { return; }
    List<Linter.LintJob> lintJobs = Lists.newArrayList();
    for (Block program : programs) {
      lintJobs.add(Linter.makeLintJob(program, mq));
    }
    Linter.lint(lintJobs, Linter.BROWSER_ENVIRONMENT, mq); // TODO: parameterize
  }

  /** Replace jobs with more compact, semantically identical jobs. */
  private void optimize(List<Job> jobs) {
    ListIterator<Job> jobIt = jobs.listIterator();
    while (jobIt.hasNext()) {
      Job job = jobIt.next();
      switch (job.t) {
        case JS: job = optimizeJs(job); break;
        case HTML: job = optimizeHtml(job); break;
        case CSS: job = optimizeCss(job); break;
        default: continue;
      }
      jobIt.set(job);
    }
  }

  private Job optimizeJs(Job job) {
    JsOptimizer opt = new JsOptimizer(mq);
    opt.addInput((Block) job.root);

    if (req.userAgent != null && req.userAgentDb != null) {
      opt.setEnvJson(req.userAgentDb.lookupEnvJson(req.userAgent, 2000 /*ms*/));
    } else {
      opt.setEnvJson(new ObjectConstructor(FilePosition.UNKNOWN));
    }
    opt.setRename(true);
    Statement optimized = opt.optimize();
    if (!(optimized instanceof Block)) {
      optimized = new Block(
          optimized.getFilePosition(), Collections.singletonList(optimized));
    }
    return Job.js((Block) optimized, job.origin, job.baseUri);
  }

  private Job optimizeHtml(Job job) {
    DocumentFragment f = (DocumentFragment) job.root;
    optimizeHtml(f);
    return job;
  }

  private Job optimizeCss(Job job) {
    final CssValidator v = new CssValidator(
        req.cssSchema, req.htmlSchema, DevNullMessageQueue.singleton());
    CssTree t = (CssTree) job.root;
    v.validateCss(AncestorChain.instance(t));
    t.acceptPostOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ac) {
        if (ac.node instanceof CssTree.RuleSet
            || ac.node instanceof CssTree.DeclarationGroup) {
          optimizeCssDeclarations(ac.cast(CssTree.class), v);
        } else if (ac.node instanceof CssTree.IdentLiteral) {
          Name part = ac.parent.node.getAttributes().get(
              CssValidator.CSS_PROPERTY_PART);
          if (part == null) { return true; }
          String partS = part.getCanonicalForm();
          if ("color".equals(partS) || partS.endsWith("::color")) {
            CssTree.IdentLiteral id = ac.cast(CssTree.IdentLiteral.class).node;
            CssTree.HashLiteral hash = CssRewriter.colorHash(
                id.getFilePosition(), Name.css(id.getValue()));
            if (hash != null
                && hash.getValue().length() < id.getValue().length()) {
              hash.getAttributes().putAll(id.getAttributes());
              ((CssTree) ac.parent.node).replaceChild(hash, id);
            }
          }
        } else if (ac.node instanceof CssTree.HashLiteral
                   && (ac.parent.node.getAttributes()
                           .get(CssValidator.CSS_PROPERTY_PART_TYPE)
                       == CssPropertyPartType.COLOR)) {
          CssTree.HashLiteral hash = ac.cast(CssTree.HashLiteral.class).node;
          String color = hash.getValue();
          if (color.length() == 7) {
            int hex = Integer.valueOf(color.substring(1), 16);
            CssTree.HashLiteral shortHash = CssRewriter.colorHash(
                hash.getFilePosition(), hex);
            if (shortHash.getValue().length() < hash.getValue().length()) {
              shortHash.getAttributes().putAll(hash.getAttributes());
              ((CssTree) ac.parent.node).replaceChild(shortHash, hash);
            }
          }
        }
        return true;
      }
    }, null);
    return job;
  }

  private static Name propertyPrefix(Name propertyName) {
    String canon = propertyName.getCanonicalForm();
    int dash = canon.lastIndexOf('-');
    if (dash < 0) { return null; }
    return Name.css(canon.substring(0, dash));
  }

  private void optimizeCssDeclarations(
      AncestorChain<? extends CssTree> cont, CssValidator v) {
    List<CssTree.Declaration> decls = Lists.newArrayList();
    for (CssTree t : cont.node.children()) {
      // RuleSets have non selectors too
      if (!(t instanceof CssTree.Declaration)) { continue; }
      decls.add((CssTree.Declaration) t);
    }
    // Maintain a prefix map so that we don't accidentally reduce two
    // property names to the same prefix which would break them.
    Multimap<Name, Name> propertyPrefixes = Multimaps.newListHashMultimap();
    for (Iterator<CssTree.Declaration> it = decls.iterator(); it.hasNext();) {
      CssTree.Declaration d = it.next();
      if (d instanceof CssTree.EmptyDeclaration) {
        cont.node.removeChild(d);
        it.remove();
      } else if (d instanceof CssTree.PropertyDeclaration) {
        CssTree.PropertyDeclaration pd = (CssTree.PropertyDeclaration) d;
        Name propName = pd.getProperty().getPropertyName();
        for (Name n = propName; n != null; n = propertyPrefix(n)) {
          propertyPrefixes.put(n, propName);
        }
      } else if (d instanceof CssTree.UserAgentHack) {
        for (CssTree h : d.children()) {
          CssTree.PropertyDeclaration pd = (CssTree.PropertyDeclaration) h;
          Name propName = pd.getProperty().getPropertyName();
          for (Name n = propName; n != null; n = propertyPrefix(n)) {
            propertyPrefixes.put(n, propName);
          }
        }
      }
    }
    for (CssTree.Declaration d : decls) {
      if (!(d instanceof CssTree.PropertyDeclaration)) { continue; }
      CssTree.PropertyDeclaration pd = (CssTree.PropertyDeclaration) d;
      CssTree.Property p = pd.getProperty();
      Name pName = p.getPropertyName();
      CssSchema.CssPropertyInfo i = req.cssSchema.getCssProperty(pName);
      if (i == null) { continue; }
      Name shortName = pName;
      List<String> pExprTypes = null;
      CssTree.Expr shortened = null;
      for (Name prefix = shortName;(prefix = propertyPrefix(prefix)) != null;) {
        if (propertyPrefixes.get(prefix).size() != 1) { break; }
        CssSchema.CssPropertyInfo si = req.cssSchema.getCssProperty(prefix);
        if (si == null) { break; }
        // If we can shorten the name and get the same types out, then
        // do so.
        CssTree.Expr e = (CssTree.Expr) pd.getExpr().clone();
        clearAttributes(e);
        if (!v.applySignature(prefix, e, si.sig)) { break; }
        if (pExprTypes == null) { pExprTypes = cssExprParts(pd.getExpr()); }
        if (!cssExprPartsConsistent(
                cssExprParts(e), pExprTypes, pName.getCanonicalForm())) {
          break;
        }
        shortName = prefix;
        shortened = e;
      }
      if (shortName != pName) {
        pd.replaceChild(shortened, pd.getExpr());
        pd.replaceChild(
            new CssTree.Property(p.getFilePosition(), shortName), p);
      }
    }
  }

  private static void clearAttributes(CssTree t) {
    t.getAttributes().remove(CssValidator.CSS_PROPERTY_PART);
    t.getAttributes().remove(CssValidator.CSS_PROPERTY_PART_TYPE);
    for (CssTree c : t.children()) { clearAttributes(c); }
  }

  private static List<String> cssExprParts(CssTree t) {
    final List<String> out = Lists.newArrayList();
    t.acceptPostOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ac) {
        Name part = ac.node.getAttributes().get(CssValidator.CSS_PROPERTY_PART);
        if (part != null) { out.add(part.getCanonicalForm()); }
        return true;
      }
    }, null);
    return Collections.unmodifiableList(out);
  }

  private static boolean cssExprPartsConsistent(
      List<? extends String> a, List<? extends String> b, String toIgnore) {
    int n = a.size();
    if (n != b.size()) { return false; }
    for (int i = 0; i < n; ++i) {
      String sa = a.get(i), sb = b.get(i);
      if (sa == null) { return sb == null; }
      if (sb == null) { return false; }
      if (!withoutCssPartPrefix(sa, toIgnore).equals(
              withoutCssPartPrefix(sb, toIgnore))) {
        return false;
      }
    }
    return true;
  }

  private static String withoutCssPartPrefix(String part, String prefix) {
    int n = part.length(), pn = prefix.length();
    int i = 0;
    // :: is used to separate parts in a CssPropertyPart, as in
    // background-color::color.
    // For a prefix like "background", skip over any parts that match the prefix
    // or that have (prefix + "-") as a prefix.
    while (i < n) {
      if (!part.regionMatches(i, prefix, 0, pn)) { break; }
      int e = i + pn;
      if (e != n) {
        int dc = part.indexOf("::", e);
        char ch = part.charAt(e);
        // Skip over the part since '-' next establishes it is ignorable.
        if (ch != '-' && e != dc) { break; }
        i = dc < 0 ? n : dc + 2;  // end of the next :: separator
      } else {
        i = e;
      }
    }
    return part.substring(i);
  }


  private void optimizeHtml(Node n) {
    if (n instanceof Element) {
      Element el = (Element) n;
      ElKey elKey = ElKey.forElement(el);
      List<Attr> toRemove = Lists.newArrayList();
      for (Attr a : Nodes.attributesOf(el)) {
        AttribKey aKey = AttribKey.forAttribute(elKey, a);
        HTML.Attribute aInfo = req.htmlSchema.lookupAttribute(aKey);
        if (aInfo != null && a.getValue().equals(aInfo.getDefaultValue())) {
          toRemove.add(a);
        }
      }
      for (Attr a : toRemove) {
        el.removeAttributeNode(a);
      }
      HTML.Element elInfo = req.htmlSchema.lookupElement(elKey);
      if (elInfo != null && !elInfo.canContainText()) {
        for (Node c = el.getFirstChild(); c != null;)  {
          Node next = c.getNextSibling();
          if (c instanceof Text && "".equals(c.getNodeValue().trim())) {
            el.removeChild(c);
          }
          c = next;
        }
      }
    }
    for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
      optimizeHtml(c);
    }
  }

  /** Instrument and run code to generate a documentation zip file. */
  private Job doc(List<Job> jobs, Request req, MessageQueue mq)
      throws IOException, JsdocException {
    Jsdoc jsdoc = new Jsdoc(req.mc, mq);
    for (Job job : jobs) {
      // Do not doc handlers.
      if (job.t != ContentType.JS || job.origin instanceof Attr) { continue; }
      Block program = (Block) job.root;
      jsdoc.addSource(program);
    }
    try {
      jsdoc.addInitFile(
          "/js/jqueryjs/runtest/env.js",
          "" + Resources.read(
              CajaWebToolsServlet.class, "/js/jqueryjs/runtest/env.js")
          );
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    ObjectConstructor json = jsdoc.extract();
    if (req.otype == ContentType.JSON) {
      return Job.json(json, null);
    } else {
      ZipFileSystem fs = new ZipFileSystem("/jsdoc");
      StringBuilder jsonSb = new StringBuilder();
      RenderContext rc = new RenderContext(
          new JsMinimalPrinter(new Concatenator(jsonSb))).withJson(true);
      json.render(rc);
      rc.getOut().noMoreTokens();
      HtmlRenderer.buildHtml(
          "" + jsonSb, fs, new File("/jsdoc"), req.srcMap.values(),
          req.mc);
      return fs.toZip();
    }
  }

  /** The reversal of {@link #extractJobs}. */
  private void reincorporateExtracted(List<Job> jobs) {
    Iterator<Job> jobsIt = jobs.iterator();
    while (jobsIt.hasNext()) {
      Job job = jobsIt.next();
      if (job.origin == null) { continue; }
      StringBuilder sb = new StringBuilder();
      RenderContext rc = makeRenderContext(sb, job.t).withEmbeddable(true);
      if (job.root instanceof Block) {
        ((Block) job.root).renderBody(rc);
      } else {
        ((ParseTreeNode) job.root).render(rc);
      }
      rc.getOut().noMoreTokens();
      String rendered = sb.toString();
      if (job.origin instanceof Element) {
        Element origin = (Element) job.origin;
        while (origin.getFirstChild() != null) {
          origin.removeChild(origin.getFirstChild());
        }
        origin.appendChild(origin.getOwnerDocument().createTextNode(rendered));
        jobsIt.remove();
      } else if (job.origin instanceof Attr) {
        Attr origin = (Attr) job.origin;
        if (job.t == ContentType.JS) {
          HTML.Attribute aInfo = req.htmlSchema.lookupAttribute(
              AttribKey.forAttribute(
                  ElKey.forElement(origin.getOwnerElement()), origin));
          if (aInfo != null && aInfo.getType() == HTML.Attribute.Type.URI) {
            rendered = "javascript:" + UriUtil.encode(rendered);
          }
        }
        origin.setNodeValue(rendered);
        jobsIt.remove();
      }
    }
  }

  private static final Set<MessageTypeInt> IGNORED = Sets.immutableSet(
      (MessageTypeInt) PluginMessageType.DISALLOWED_CSS_PROPERTY_IN_SELECTOR,
      PluginMessageType.UNSAFE_CSS_PROPERTY,
      PluginMessageType.UNSAFE_TAG,
      PluginMessageType.CSS_ATTRIBUTE_TYPE_NOT_ALLOWED_IN_SELECTOR,
      PluginMessageType.CSS_ATTRIBUTE_NAME_NOT_ALLOWED_IN_SELECTOR,
      PluginMessageType.CSS_DASHMATCH_ATTRIBUTE_OPERATOR_NOT_ALLOWED,
      PluginMessageType.IMPORTS_NOT_ALLOWED_HERE,
      PluginMessageType.FONT_FACE_NOT_ALLOWED
  );

  private static void removeCajolerSpecificMessages(MessageQueue mq) {
    for (Iterator<Message> i = mq.getMessages().iterator(); i.hasNext();) {
      if (IGNORED.contains(i.next().getMessageType())) { i.remove(); }
    }
  }
}
