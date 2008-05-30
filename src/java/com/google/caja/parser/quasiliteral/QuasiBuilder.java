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

package com.google.caja.parser.quasiliteral;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.JsLexer;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionDeclaration;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.Parser;
import com.google.caja.parser.js.Reference;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.reporting.DevNullMessageQueue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.URI;

/**
 * Creates a JavaScript {@link QuasiNode} tree given a JavaScript
 * {@link com.google.caja.parser.ParseTreeNode} tree.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class QuasiBuilder {

  /**
   * The stub {@code InputSource} associated with quasiliteral strings that appear
   * directly in source code.
   */
  public static final InputSource NULL_INPUT_SOURCE
      = new InputSource(URI.create("built-in:///js-quasi-literals"));

  private static final Map<String, QuasiNode> patternCache
      = new HashMap<String, QuasiNode>();

  /**
   * Match a quasiliteral pattern against a specimen.
   *
   * @param patternText a quasiliteral pattern.
   * @param specimen a specimen parse tree node.
   * @return whether the match succeeded.
   * @see QuasiNode#match(com.google.caja.parser.ParseTreeNode)
   */
  public static boolean match(String patternText, ParseTreeNode specimen) {
    return match(patternText, specimen, new HashMap<String, ParseTreeNode>());
  }

  /**
   * Match a quasiliteral pattern against a specimen, returning any hole bindings found in
   * a client supplied map.
   *
   * @param patternText a quasiliteral pattern.
   * @param specimen a specimen parse tree node.
   * @param bindings a map into which hole bindings resulting from the match will be placed.
   * @return whether the match succeeded.
   * @see QuasiNode#match(com.google.caja.parser.ParseTreeNode)
   */
  public static boolean match(
      String patternText,
      ParseTreeNode specimen,
      Map<String, ParseTreeNode> bindings) {
    Map<String, ParseTreeNode> tempBindings = getPatternNode(patternText).match(specimen);

    if (tempBindings != null) {
      bindings.putAll(tempBindings);
      return true;
    }
    return false;
  }

  /**
   * Substitute variables into a quasiliteral pattern, returning a concrete parse tree node.
   *
   * @param patternText a quasiliteral pattern.
   * @param bindings a set of bindings from names to parse tree nodes.
   * @return a new parse tree node resulting from the substitution.
   * @see QuasiNode#substitute(java.util.Map)
   */
  public static ParseTreeNode subst(String patternText, Map<String, ParseTreeNode> bindings) {
    return getPatternNode(patternText).substitute(bindings);
  }

  /**
   * Substitute variables into a quasiliteral pattern, returning a concrete parse tree node,
   * passing the bindings as a variable arguments list.
   *
   * @param patternText a quasiliteral pattern.
   * @param args an even number of values arranged in pairs of
   * ({@code String}, {@code ParseTreeNode}) representing bindings to substitute into the pattern.
   * @return a new parse tree node resulting from the substitution.
   * @see #subst(String, java.util.Map)
   */
  public static ParseTreeNode substV(String patternText, Object... args) {
    if (args.length %2 != 0) throw new RuntimeException("Wrong # of args for subst()");
    Map<String, ParseTreeNode> bindings = new HashMap<String, ParseTreeNode>();
    for (int i = 0; i < args.length; ) {
      bindings.put(
          (String)args[i++],
          (ParseTreeNode)args[i++]);
    }
    ParseTreeNode result = subst(patternText, bindings);
    if (result == null) {
      throw new NullPointerException(
          "'" + patternText + "' > " + bindings.keySet());
    }
    return result;
  }

  /**
   * Given a quasiliteral pattern expressed as text, return a {@code QuasiNode}
   * representing the pattern.
   *
   * @param inputSource description of input source of pattern text.
   * @param pattern a quasiliteral pattern.
   * @return the QuasiNode representation of the input.
   * @exception ParseException if there is a parsing problem.
   */
  public static QuasiNode parseQuasiNode(
      InputSource inputSource,
      String pattern) throws ParseException {
    ParseTreeNode topLevelNode = parse(inputSource, pattern);

    // The top-level node returned from the parser is always a Block.
    if (!(topLevelNode instanceof Block)) {
      throw new RuntimeException("Panic: top level is not a Block");
    }

    // If the top-level Block contains a single child, promote it to allow it to match anywhere.
    if (topLevelNode.children().size() == 1) {
      topLevelNode = topLevelNode.children().get(0);
    }

    // If the top level is an ExpressionStmt, with one child, then promote its single child to
    // the top level to allow the contained expression to match anywhere.
    if (topLevelNode instanceof ExpressionStmt &&
        topLevelNode.children().size() == 1) {
      topLevelNode = topLevelNode.children().get(0);
    }

    // If the top level is a FunctionDeclaration, promote its single child to the top level
    // to allow the contained FunctionConstructor to match in any context.
    if (topLevelNode instanceof FunctionDeclaration) {
      topLevelNode = ((FunctionDeclaration)topLevelNode).getInitializer();
    }

    return build(topLevelNode);
  }

  /**
   * @see #parseQuasiNode(InputSource,String)
   * @see #NULL_INPUT_SOURCE
   */
  public static QuasiNode parseQuasiNode(
      String pattern) throws ParseException {
    return parseQuasiNode(NULL_INPUT_SOURCE, pattern);
  }

  private static QuasiNode getPatternNode(String patternText) {
    if (!patternCache.containsKey(patternText)) {
      try {
        patternCache.put(
            patternText,
            QuasiBuilder.parseQuasiNode(patternText));
      } catch (ParseException e) {
        // Pattern programming error
        throw new RuntimeException(e);
      }
    }
    return patternCache.get(patternText);
  }

  private static QuasiNode build(ParseTreeNode n) {
    if (n instanceof ExpressionStmt &&
        n.children().size() == 1 &&
        n.children().get(0) instanceof Reference &&
        ((Reference)n.children().get(0)).getIdentifierName().startsWith("@") &&
        !((Reference)n.children().get(0)).getIdentifierName().endsWith("_")) {
      return buildMatchNode(Statement.class, ((Reference)n.children().get(0)).getIdentifierName());
    }

    if (n instanceof Reference &&
        ((Reference)n).getIdentifierName().startsWith("@") &&
        !((Reference)n).getIdentifierName().endsWith("_")) {
      return buildMatchNode(Expression.class, ((Reference)n).getIdentifierName());
    }

    if (n instanceof FormalParam &&
        ((FormalParam)n).getIdentifierName().startsWith("@")) {
      return buildMatchNode(FormalParam.class, ((FormalParam)n).getIdentifierName());
    }

    if (n instanceof Identifier &&
        ((Identifier)n).getValue() != null &&
        ((Identifier)n).getValue().startsWith("@")) {
      if (((Identifier)n).getValue().endsWith("_")) {
        return buildTrailingUnderscoreMatchNode(((Identifier)n).getValue());
      } else {
        return buildMatchNode(Identifier.class, ((Identifier)n).getValue());
      }
    }

    if (n instanceof ObjectConstructor) {
      if (n.children().size() == 2) {
        if (n.children().get(0) instanceof StringLiteral) {
          String key = ((StringLiteral)n.children().get(0)).getUnquotedValue();
          if (key.startsWith("@") && key.endsWith("*")) {
            if (n.children().get(1) instanceof Reference) {
              String val = ((Reference)n.children().get(1)).getIdentifierName();
              if (val.startsWith("@") && val.endsWith("*")) {
                return buildObjectConstructorMatchNode(key, val);
              }
            }
          }
        }
      }
    }

    return buildSimpleNode(n);
  }

  private static QuasiNode buildSimpleNode(ParseTreeNode n) {
    return new SimpleQuasiNode(
        n.getClass(),
        n.getValue(),
        buildChildrenOf(n));
  }

  private static QuasiNode buildMatchNode(
      Class<? extends ParseTreeNode> matchedClass,
      String quasiString) {
    assert(quasiString.startsWith("@"));
    if (quasiString.endsWith("*")) {
      return new MultipleQuasiHole(
          matchedClass,
          quasiString.substring(1, quasiString.length() - 1));
    } else if (quasiString.endsWith("+")) {
      return new MultipleNonemptyQuasiHole(
          matchedClass,
          quasiString.substring(1, quasiString.length() - 1));
    } else if (quasiString.endsWith("?")) {
      return new SingleOptionalQuasiHole(
          matchedClass,
          quasiString.substring(1, quasiString.length() - 1));
    } else {
      return new SingleQuasiHole(
          matchedClass,
          quasiString.substring(1, quasiString.length()));
    }
  }

  private static QuasiNode buildTrailingUnderscoreMatchNode(String quasiString) {
    assert(quasiString.startsWith("@"));
    assert(quasiString.endsWith("_"));
    quasiString = quasiString.substring(1, quasiString.length());
    int numberOfUnderscores = 0;
    while (quasiString.endsWith("_")) {
      quasiString = quasiString.substring(0, quasiString.length() - 1);
      numberOfUnderscores++;
    }
    return new TrailingUnderscoresHole(quasiString, numberOfUnderscores);
  }

  private static QuasiNode buildObjectConstructorMatchNode(String keyExpr, String valueExpr) {
    keyExpr = keyExpr.substring(1, keyExpr.length() - 1);
    valueExpr = valueExpr.substring(1, valueExpr.length() - 1);
    return new ObjectConstructorHole(keyExpr, valueExpr);
  }

  private static QuasiNode[] buildChildrenOf(ParseTreeNode n) {
    List<QuasiNode> children = new ArrayList<QuasiNode>();
    for (ParseTreeNode child : n.children()) children.add(build(child));
    return children.toArray(new QuasiNode[children.size()]);
  }

  private static ParseTreeNode parse(
      InputSource inputSource,
      String sourceText) throws ParseException {
    Parser parser = new Parser(
        new JsTokenQueue(
            new JsLexer(
                CharProducer.Factory.create(new StringReader(sourceText),
                inputSource),
                true),
            inputSource,
            JsTokenQueue.NO_NON_DIRECTIVE_COMMENT),
        DevNullMessageQueue.singleton(),
        true);

    Statement topLevelStatement = parser.parse();
    parser.getTokenQueue().expectEmpty();
    return topLevelStatement;
  }
}
