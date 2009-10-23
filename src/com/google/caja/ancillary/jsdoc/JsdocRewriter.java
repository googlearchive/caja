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

import com.google.caja.lang.html.HTML;
import com.google.caja.lang.html.HtmlSchema;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Token;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.Visitor;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Conditional;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.Loop;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.ReturnStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.SwitchCase;
import com.google.caja.parser.js.WhileLoop;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Criterion;
import com.google.caja.util.Name;
import com.google.caja.util.Pair;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A rewriter that rewrites javascript to store documentation strings with
 * javascript values so that we can evaluate the javascript and inspect the
 * resulting environment to get at the public API, a la pydoc.
 *
 * @author mikesamuel@gmail.com
 */
class JsdocRewriter {
  private final MessageQueue mq;
  private final MessageContext mc;
  private final Set<Token<?>> consumed = new HashSet<Token<?>>();
  private final AnnotationHandlers handlers;

  JsdocRewriter(
      AnnotationHandlers handlers, MessageContext mc, MessageQueue mq) {
    this.mc = mc;
    this.mq = mq;
    this.handlers = handlers;
  }

  ParseTreeNode rewriteFile(ParseTreeNode js) {
    Expression fileOverview = extractFileOverview(js);
    ParseTreeNode fileBody = rewrite(js);
    if (fileOverview != null) {
      return QuasiBuilder.substV(
          "{ jsdoc___.documentFile(@fileName, @fileOverview); @fileBody; }",
          "fileName", StringLiteral.valueOf(
              js.getFilePosition(), format(js.getFilePosition().source(), mc)),
          "fileOverview", fileOverview,
          "fileBody", fileBody);
    }
    return fileBody;
  }

  ParseTreeNode rewritePackageDocs(InputSource is, Comment cmt) {
    Expression packageDocs = commentToJson(normalizeHtml(cmt));
    if (packageDocs == null) { return null; }
    return QuasiBuilder.substV(
        "{ jsdoc___.documentFile(@packageName, @packageDocs); }",
        "packageName", StringLiteral.valueOf(
            cmt.getFilePosition(), format(is, mc)),
        "packageDocs", packageDocs);
  }

  private Expression extractFileOverview(ParseTreeNode js) {
    Comment fileOverview = getDocComment(js, new Criterion<Comment>() {
      public boolean accept(Comment c) {
        for (Annotation a : c.children()) {
          if (!(a instanceof BlockAnnotation)) { continue; }
          String name = ((BlockAnnotation) a).getValue();
          if ("fileoverview".equals(name)) {
            return true;
          }
        }
        return false;
      }
    });
    if (fileOverview == null && js instanceof Block
        && !js.children().isEmpty()) {
      return extractFileOverview(js.children().get(0));
    }
    if (fileOverview != null) {
      List<Annotation> flattened = new ArrayList<Annotation>();
      for (Annotation a : fileOverview.children()) {
        if (a instanceof BlockAnnotation
            && "fileoverview".equals(a.getValue())) {
          flattened.addAll(a.children());
        } else {
          flattened.add(a);
        }
      }
      Comment overview = new Comment(
          normalizeHtml(flattened), fileOverview.getFilePosition());
      return commentToJson(overview);
    }
    return null;
  }

  private ParseTreeNode rewrite(ParseTreeNode js) {
    ParseTreeNode result;
    if (js instanceof FunctionConstructor
              || js instanceof FunctionDeclaration) {
      result = documentFunction(js);
    } else if (js instanceof Declaration) {
      // TODO: handle the first declaration in a multi-declaration
      result = documentDeclaration((Declaration) js);
    } else if (js instanceof ObjectConstructor) {
      result = documentObjectConstructor((ObjectConstructor) js);
    } else if (js instanceof Expression) {
      result = documentExpression((Expression) js);
    } else if (js instanceof ExpressionStmt) {
      result = documentExpressionStmt((ExpressionStmt) js);
    } else if (js instanceof ReturnStmt) {
      result = documentReturnStmt((ReturnStmt) js);
    } else if (js instanceof Loop) {
      result = documentLoop((Loop) js);
    } else if (js instanceof Conditional) {
      result = documentConditional((Conditional) js);
    } else if (isScopeBlock(js)) {
      result = delayDocingOfUninitializedVariables((Statement) js);
    } else {
      result = ParseTreeNodes.newNodeInstance(
          js.getClass(), js.getFilePosition(), js.getValue(),
          rewriteAll(js.children()));
    }
    return result;
  }

  private static boolean isScopeBlock(ParseTreeNode js) {
    return js instanceof Block || js instanceof SwitchCase;
  }

  private Expression documentExpression(Expression js) {
    return documentExpression(js, getDocCommentJson(js));
  }

  private Expression documentExpression(Expression js, Expression doc) {
    FilePosition pos = js.getFilePosition();
    if (js instanceof Operation) {
      Operation operation = (Operation) js;
      Operator op = operation.getOperator();
      if (doc != null && Operator.ASSIGN == op) {
        return (Expression) QuasiBuilder.substV(
            "@lhs = jsdoc___.document(@rhs, @doc)",
            "lhs", rewrite(js.children().get(0)),
            "rhs", rewrite(js.children().get(1)),
            "doc", doc);
      } else {
        switch (op) {
          case TYPEOF:
            return (Expression) QuasiBuilder.substV(
                "typeof @e === 'undefined'"
                + "? 'undefined'"
                + ": typeof jsdoc___.unwrap(@e)",
                "e", rewrite(operation.children().get(0)));
          case NOT:
          case EQUAL:
          case NOT_EQUAL:
          case STRICTLY_EQUAL:
          case STRICTLY_NOT_EQUAL:
            List<Expression> unwrappedChildren = new ArrayList<Expression>();
            for (Expression child : operation.children()) {
              unwrappedChildren.add(
                  (Expression) QuasiBuilder.substV(
                      "jsdoc___.unwrap(@e)",
                      "e", rewrite(child)));
            }
            return Operation.create(
                pos, op, unwrappedChildren.toArray(new Expression[0]));
          case LOGICAL_OR:
            return (Expression) QuasiBuilder.substV(
                "jsdoc___.unwrap(@a) ? @a : @b",
                "a", rewrite(operation.children().get(0)),
                "b", rewrite(operation.children().get(1)));
          case LOGICAL_AND:
            return (Expression) QuasiBuilder.substV(
                "!jsdoc___.unwrap(@a) ? @a : @b",
                "a", rewrite(operation.children().get(0)),
                "b", rewrite(operation.children().get(1)));
          case TERNARY:
            return (Expression) QuasiBuilder.substV(
                "jsdoc___.unwrap(@a) ? @b : @c",
                "a", rewrite(operation.children().get(0)),
                "b", rewrite(operation.children().get(1)),
                "c", rewrite(operation.children().get(2)));
          default: break;
        }
      }
    }

    Expression rewritten = ParseTreeNodes.newNodeInstance(
        js.getClass(), pos, js.getValue(), rewriteAll(js.children()));
    if (doc != null) {
      return (Expression) QuasiBuilder.substV(
          "jsdoc___.document(@e, @doc)",
          "e", rewritten,
          "doc", doc);
    }

    // HACK HACK
    if (rewritten instanceof Reference
        && ((Reference) rewritten).getIdentifierName().equals("__iterator__")) {
      return new Reference(new Identifier(pos, "_iterator_"));
    }

    return rewritten;
  }

  private ExpressionStmt documentExpressionStmt(ExpressionStmt js) {
    Expression doc = getDocCommentJson(js);
    return new ExpressionStmt(
        js.getFilePosition(), documentExpression(js.getExpression(), doc));
  }

  private ParseTreeNode documentDeclaration(Declaration js) {
    FilePosition pos = js.getFilePosition();
    if (js.getInitializer() != null) {
      // Uninitialized declarations handled elsewhere
      Expression doc = getDocCommentJson(js);
      if (doc != null) {
        if (js instanceof FunctionDeclaration) {
          return new ParseTreeNodeContainer(
              Arrays.<ParseTreeNode>asList(
                  ParseTreeNodes.newNodeInstance(
                      js.getClass(), pos, js.getValue(),
                      rewriteAll(js.children())),
                  new ExpressionStmt(
                      pos,
                      (Expression) QuasiBuilder.substV(
                          "jsdoc___.document(@name, @doc);",
                          "name", new Reference(js.getIdentifier()),
                          "doc", doc))
              ));
        } else {
          return QuasiBuilder.substV(
              "var @name = jsdoc___.document(@initial, @doc);",
              "name", js.getIdentifier(),
              "initial", rewrite(js.getInitializer()),
              "doc", doc);
        }
      }
    }
    return ParseTreeNodes.newNodeInstance(
        js.getClass(), pos, js.getValue(), rewriteAll(js.children()));
  }

  private ParseTreeNode documentFunction(ParseTreeNode fn) {
    FilePosition pos = fn.getFilePosition();
    FunctionDeclaration decl;
    FunctionConstructor ctor;
    if (fn instanceof FunctionDeclaration) {
      decl = (FunctionDeclaration) fn;
      ctor = decl.getInitializer();
    } else {
      decl = null;
      ctor = (FunctionConstructor) fn;
    }

    // Look for assignment to members of this in the function body.
    // This helps us identify instance members added by a constructor.
    Expression doc = getDocCommentJson(fn);
    if (doc == null) {
      doc = new ObjectConstructor(pos);
    }
    // Now build a set of fields by eliminating duplicate members.
    final Map<String, Expression> fieldDocsByName
        = new LinkedHashMap<String, Expression>();
    ctor.getBody().acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ac) {
        if (ac.node instanceof FunctionConstructor) { return false; }
        if (!(ac.node instanceof Operation)) { return true; }
        Map<String, ParseTreeNode> bindings
            = new HashMap<String, ParseTreeNode>();
        if (QuasiBuilder.match("this.@field", ac.node, bindings)
            || QuasiBuilder.match("this.@field = @rhs", ac.node, bindings)) {
          String name = ((Reference) bindings.get("field")).getIdentifierName();
          Expression doc = getDocCommentJson(ac.node);
          if (doc == null && ac.parent.node instanceof ExpressionStmt) {
            doc = getDocCommentJson(ac.parent.node);
          }
          if (!fieldDocsByName.containsKey(name)) {
            fieldDocsByName.put(name, doc);
          } else if (doc != null) {
            Expression oldDocs = fieldDocsByName.get(name);
            if (oldDocs != null) {
              mq.addMessage(
                  JsdocMessageType.DUPLICATE_DOCUMENTATION,
                  ac.node.getFilePosition(), oldDocs.getFilePosition());
            } else {
              fieldDocsByName.put(name, doc);
            }
          }
        }
        return true;
      }
    }, null);

    List<Pair<Literal, Expression>> fieldMembers
        = new ArrayList<Pair<Literal, Expression>>();
    for (Map.Entry<String, Expression> e : fieldDocsByName.entrySet()) {
      Expression memberDoc = e.getValue();
      if (memberDoc == null) { memberDoc = new ObjectConstructor(pos); }
      fieldMembers.add(Pair.pair(
          (Literal) StringLiteral.valueOf(pos, e.getKey()), memberDoc));
    }
    ObjectConstructor fields = new ObjectConstructor(pos, fieldMembers);

    ParseTreeNodeContainer formals = new ParseTreeNodeContainer(
        ctor.getParams());
    ParseTreeNodeContainer expandedBody = new ParseTreeNodeContainer(
        rewriteAll(ctor.getBody().children()));

    FunctionConstructor newCtor = (FunctionConstructor) QuasiBuilder.substV(
        "function @name?(@formals*) { @body* }",
        "name", ctor.getIdentifier(),
        "formals", formals,
        "body", expandedBody);

    if (decl == null) {
      Expression newDocCall = (Expression) QuasiBuilder.substV(
          "jsdoc___.documentFunction(@ctor, @doc, @fields)",
          "ctor", newCtor,
          "doc", doc,
          "fields", fields);
      return newDocCall;
    } else {
      FunctionDeclaration newDecl = new FunctionDeclaration(newCtor);
      Statement docCall = new ExpressionStmt(pos,
          (Expression) QuasiBuilder.substV(
              "jsdoc___.documentFunction(@name, @doc, @fields);",
              "name", new Reference(ctor.getIdentifier()),
              "doc", doc,
              "fields", fields));
      return new ParseTreeNodeContainer(Arrays.asList(newDecl, docCall));
    }
  }

  private ParseTreeNode documentObjectConstructor(ObjectConstructor o) {
    List<? extends Expression> children = o.children();
    List<Pair<Literal, Expression>> entries
        = new ArrayList<Pair<Literal, Expression>>();
    for (int i = 0, n = children.size(); i < n; i += 2) {
      Literal key = (Literal) children.get(i);
      Expression value = children.get(i + 1);

      Expression docComment = getDocCommentJson(key);
      if (docComment == null) {
        docComment = getDocCommentJson(value);
      }
      entries.add(Pair.pair(key, documentExpression(value, docComment)));
    }
    return new ObjectConstructor(o.getFilePosition(), entries);
  }

  private ParseTreeNode documentReturnStmt(ReturnStmt js) {
    Expression doc = getDocCommentJson(js);
    if (js.getReturnValue() == null) { return js; }
    return new ReturnStmt(
        js.getFilePosition(), documentExpression(js.getReturnValue(), doc));
  }

  private Loop documentLoop(Loop js) {
    List<ParseTreeNode> rewritten = rewriteAll(js.children());
    int condIndex = js instanceof WhileLoop ? 0 : 1;
    rewritten.set(
        condIndex,
        QuasiBuilder.substV(
            "jsdoc___.unwrap(@cond)", "cond", rewritten.get(condIndex)));
    return ParseTreeNodes.newNodeInstance(
        js.getClass(), js.getFilePosition(), js.getLabel(), rewritten);
  }

  private Conditional documentConditional(Conditional js) {
    List<ParseTreeNode> rewritten = rewriteAll(js.children());
    for (int i = 0; i + 1 < rewritten.size(); i += 2) {
      rewritten.set(
          i,
          QuasiBuilder.substV(
              "jsdoc___.unwrap(@cond)", "cond", rewritten.get(i)));
    }
    return ParseTreeNodes.newNodeInstance(
        js.getClass(), js.getFilePosition(), js.getValue(), rewritten);
  }

  private Statement delayDocingOfUninitializedVariables(final Statement s) {
    final List<Declaration> uninitialized = new ArrayList<Declaration>();
    s.acceptPreOrder(new Visitor() {
      public boolean visit(AncestorChain<?> ac) {
        if (ac.node == s) { return true; }
        if (ac.node instanceof FunctionConstructor
            || isScopeBlock(ac.node)) {
          return false;
        }
        if (ac.node instanceof Declaration) {
          Declaration decl = ac.cast(Declaration.class).node;
          if (decl.getInitializer() == null) { uninitialized.add(decl); }
        }
        return true;
      }
    }, null);

    List<Statement> docStmts = new ArrayList<Statement>();
    for (Declaration decl : uninitialized) {
      Expression doc = getDocCommentJson(decl);
      if (doc != null) {
        docStmts.add(new ExpressionStmt(
            s.getFilePosition(),
            (Expression) QuasiBuilder.substV(
                "@name = jsdoc___.document(@name, @doc);",
                "name", new Reference(decl.getIdentifier()),
                "doc", doc)));
      }
    }

    Statement rewrittenStmt = ParseTreeNodes.newNodeInstance(
        s.getClass(), s.getFilePosition(), s.getValue(),
        rewriteAll(s.children()));
    if (docStmts.isEmpty()) {
      return rewrittenStmt;
    } else {
      return (Statement) QuasiBuilder.substV(
          "try { @stmt; } finally { @docStmts*; }",
          "stmt", rewrittenStmt,
          "docStmts", new ParseTreeNodeContainer(docStmts));
    }
  }

  private List<ParseTreeNode> rewriteAll(List<? extends ParseTreeNode> nodes) {
    List<ParseTreeNode> rewritten = new ArrayList<ParseTreeNode>();
    for (ParseTreeNode node : nodes) {
      ParseTreeNode rnode = rewrite(node);
      if (rnode instanceof ParseTreeNodeContainer) {
        rewritten.addAll(rnode.children());
      } else {
        rewritten.add(rnode);
      }
    }
    return rewritten;
  }

  private Comment getDocComment(ParseTreeNode js, Criterion<Comment> filter) {
    for (Token<?> t : js.getComments()) {
      if (!t.text.startsWith("/**")) { continue; }  //*/
      Comment cmt;
      try {
        cmt = CommentParser.parseStructuredComment(
            CharProducer.Factory.create(new StringReader(t.text), t.pos));
      } catch (ParseException ex) {
        ex.toMessageQueue(mq);
        continue;
      }
      if (!consumed.contains(t) && filter.accept(cmt)) {
        consumed.add(t);
        return cmt;
      }
    }
    return null;
  }

  private Expression getDocCommentJson(ParseTreeNode js) {
    Comment cmt = getDocComment(js, Criterion.Factory.<Comment>optimist());
    if (cmt == null) { return null; }
    return commentToJson(cmt);
  }

  private Expression commentToJson(Comment cmt) {
    FilePosition pos = cmt.getFilePosition();
    List<Annotation> description = new ArrayList<Annotation>();
    Map<String, List<Expression>> blocks
        = new LinkedHashMap<String, List<Expression>>();
    for (Annotation a : cmt.children()) {
      if (a instanceof BlockAnnotation) {
        Expression e = handlers.handlerFor(a).handle(a, mq);
        if (e != null) {
          String name = a.getValue();
          if (!blocks.containsKey(name)) {
            blocks.put(name, new ArrayList<Expression>());
          }
          blocks.get(name).add(e);
        }
      } else {  // Part of the summary
        description.add(a);
      }
    }

    List<Pair<Literal, Expression>> docEntries
        = new ArrayList<Pair<Literal, Expression>>();

    if (!description.isEmpty()) {
      BlockAnnotation desc = new BlockAnnotation(
          "::description", description, cmt.getFilePosition());
      BlockAnnotation summary = Summarizer.summarize(desc);
      Expression descExpr = handlers.handlerFor(desc)
          .handle(normalizeHtml(desc), mq);
      Expression summaryExpr = handlers.handlerFor(summary)
          .handle(normalizeHtml(summary), mq);
      if (descExpr != null) {
        docEntries.add(Pair.pair(
            (Literal) StringLiteral.valueOf(pos, "@description"), descExpr));
      }
      if (summaryExpr != null) {
        docEntries.add(Pair.pair(
            (Literal) StringLiteral.valueOf(pos, "@summary"), summaryExpr));
      }
    }

    docEntries.add(Pair.<Literal, Expression>pair(
        StringLiteral.valueOf(pos, "@pos"),
        StringLiteral.valueOf(pos, format(pos, mc))));
    for (Map.Entry<String, List<Expression>> block : blocks.entrySet()) {
      docEntries.add(Pair.pair(
          (Literal) StringLiteral.valueOf(pos, "@" + block.getKey()),
          (Expression) new ArrayConstructor(pos, block.getValue())));
    }
    return new ObjectConstructor(pos, docEntries);
  }

  static String format(FilePosition p, MessageContext mc) {
    StringBuilder sb = new StringBuilder(format(p.source(), mc));
    sb.append(':')
      .append(p.startLineNo())
      .append('+')
      .append(p.startCharInLine())
      .append(" - ");
    if (p.startLineNo() != p.endLineNo()) {
      sb.append(p.endLineNo()).append('+');
    }
    return sb.append(p.endCharInLine()).toString();
  }

  /**
   * Simplify a URI by removing the common path elements from the front.
   * If all the input sources start with "file:///src/" then that prefix will
   * be skipped.
   */
  static String format(InputSource s, MessageContext mc) {
    String uriStr = s.getUri().toString();
    int tail = uriStr.lastIndexOf('/');
    for (InputSource t : mc.getInputSources()) {
      if (tail < 0) { break; }
      String uriStr2 = t.getUri().toString();
      int common = Math.min(tail, Math.min(uriStr.length(), uriStr2.length()));
      for (int i = 0; i < common; ++i) {
        if (uriStr.charAt(i) != uriStr2.charAt(i)) {
          common = i;
          break;
        }
      }
      if (common < tail) { tail = uriStr.lastIndexOf('/', common); }
    }
    return uriStr.substring(tail + 1);
  }

  private static final Pattern TAG = Pattern.compile(
      "(</?)([a-z]+[1-6]?)([^<>]*>)|"
      + "(&(?:amp|lt|gt|quot|#(?:x[0-9a-f]+|[0-9]+));)"
      + "|<!--.*?-->"
      + "|[<>&]",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  /**
   * Normalize HTML so that unclosed tags don't interfere with the formatting
   * of the embedding page.  This is not meant to enforce any security
   * properties.
   */
  private List<Annotation> normalizeHtml(
      List<? extends Annotation> annotations) {
    HtmlSchema schema = HtmlSchema.getDefault(mq);

    List<Name> open = new ArrayList<Name>();
    List<Annotation> normalized = new ArrayList<Annotation>();
    for (int i = 0, n = annotations.size(); i < n; ++i) {
      Annotation a = annotations.get(i);
      if (a instanceof TextAnnotation) {
        Matcher m = TAG.matcher(a.getValue());
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
          m.appendReplacement(sb, "");
          if (m.group(1) != null) {
            boolean end = "</".equals(m.group(1));
            Name name = Name.html(m.group(2));
            HTML.Element el = schema.lookupElement(name);
            if (el != null) {
              if (end) {
                int lindex = open.lastIndexOf(name);
                if (lindex >= 0) {
                  for (int j = open.size(); --j >= lindex;) {
                    sb.append("</").append(open.get(j)).append('>');
                  }
                  open.subList(lindex, open.size()).clear();
                }
              } else {
                sb.append(m.group(0));
                if (!el.isEmpty()) { open.add(name); }
              }
            } else {
              sb.append("&lt;").append(m.group(0).substring(1));
            }
          } else if (m.group(4) != null) {
            sb.append(m.group(0));
          } else {
            String s = m.group(0);
            if (s.startsWith("<!--")) {
              sb.append("<!---->");
            } else if ("&".equals(s)) {
              sb.append("&amp;");
            } else if (">".equals(s)) {
              sb.append("&gt;");
            } else {
              sb.append("&lt;");
            }
          }
        }
        m.appendTail(sb);
        for (int j = open.size(); --j >= 0;) {
          sb.append("</").append(open.get(j)).append('>');
        }
        normalized.add(new TextAnnotation(sb.toString(), a.getFilePosition()));
      } else {
        normalized.add(a);
      }
    }
    return normalized;
  }
  private Comment normalizeHtml(Comment c) {
    return new Comment(normalizeHtml(c.children()), c.getFilePosition());
  }
  private BlockAnnotation normalizeHtml(BlockAnnotation a) {
    return new BlockAnnotation(
        a.getValue(), normalizeHtml(a.children()), a.getFilePosition());
  }
}
