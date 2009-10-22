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

package com.google.caja.parser.quasiliteral;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.CatchStmt;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.FormalParam;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Identifier;
import com.google.caja.parser.js.NullLiteral;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.Reference;
import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.caja.util.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A transformation from an expression with possibly masking identifiers to one
 * that has no masking identifiers, and that only has free variables within an
 * allowed set.
 *
 * <h3>Definitions</h3>
 * <dl>
 *   <dt>Masking</dt>
 *     <dd>An identifier I' masks an identifier I when two conditions are met
 *     <ol>
 *       <li>I' and I have the same name.
 *       <li>I' is defined in an inner scope of the scope in which I is defined.
 *     </ol>
 * </dl>
 *
 * <p>
 * This code assumes that there is some external context which is being renamed
 * so as to separate the namespace for user defined code from that defined by
 * generated code.
 * A {@link NameContext} is used to map user defined names to names guaranteed
 * not to conflict with generated code names.
 *
 * <p>
 * In the code snippet below, we show the interaction between user-generated
 * code, generated code, and the alpha renamer.
 * First, each source of code needs its own identifiers.
 * <pre>
 * var foo, bar;     // user declared variables
 * var out___ = [];  // machine generated identifier
 * (foo + bar)       // user defined code.
 * </pre>
 * We generate a {@link NameContext} for user defined code that uses a
 * {@link com.google.caja.util.SafeIdentifierMaker} to remap variable names:
 * <pre>
 *   foo &rarr; a
 *   bar &rarr; b
 * </pre>
 * then, the code generator wants to combine the code.  Naively, it might
 * do something like:
 * <pre>
 *   out___.push(foo + bar);
 * </pre>
 * but malicious or naive user code might conflict with generated code
 * identifiers.  And the code generator might want to examine properties
 * of the user generated code to allow it to generate more efficient code.
 * So we alpha-rename code which allows us to move all user-generated
 * identifiers into a distinct namespace, makes analysis easier, and
 * makes it easy for us to segregate pieces of user-code by bounding
 * the set of free identifiers of any given expression.
 * The alpha renaming of the above would leave us with:
 * <pre>
 * // Machine generated variables
 * var out___;
 * // User generated variables;
 * var a, b;
 * // Interacting code
 * out___.push(a + b);
 * </pre>
 *
 * <p>
 * This class deals with renaming expressions only.  It could be extended to
 * rename arbitrary JavaScript parse trees, but there is an added wrinkle that
 * will have to be considered when rewriting a program.  Top level declarations
 * in a program alias properties in the global object, so we cannot simply
 * rewrite top-level declarations and remain semantics-preserving.
 *
 * @author mikesamuel@gmail.com
 */
public final class AlphaRenaming {

  /**
   * Returns an expression that is structurally the same as e, but with
   * identifiers renamed using the alpha renaming.
   *
   * <p>
   * If the input expression contains synthetic references or declarations, then
   * those synthetic identifiers will not be rewritten.
   *
   * <p>
   * So after alpha renaming, matching references and declarations with
   * non-synthetic identifiers is trivial.
   *
   * <p>
   * This renaming does not affect property names, so any relationship between
   * a property name and a global variable will be broken.  For this reason,
   * all free variables must be specified in the input context or
   * freeSynthetics set.
   *
   * @param e the expression to transform.
   * @param renamableExterns maps e's free identifiers to the names to which
   *     they should be renamed, and provides an identifier generator used to
   *     generate new identifiers for variables declared within e.
   * @param fixedExterns a set of names of free synthetic identifiers allowed to
   *     appear as free variables in e.  Since they are synthetic, they are
   *     not renamed.
   * @param mq receives error messages about free variables.
   */
  public static Expression rename(
      Expression e, NameContext<String, ?> renamableExterns,
      Set<String> fixedExterns, MessageQueue mq) {
    // Run the rewriter.
    Expression f = (Expression) new AlphaRenamingRewriter(mq, renamableExterns)
        .expand(e);

    // Performs a sanity check on the output by creating a scope that declares
    // locally everything in renamableExterns, and reruns Scope to make sure
    // that now there are no free variables.
    Map<String, FormalParam> rewrittenNames = Maps.newLinkedHashMap();
    for (NameContext<String, ?> p = renamableExterns; p != null;
         p = p.getParentContext()) {
      for (NameContext.VarInfo<String, ?> ni : p.vars()) {
        String rewrittenName = ni.newName;
        rewrittenNames.put(
            rewrittenName,
            new FormalParam(new Identifier(ni.declaredAt, rewrittenName)));
      }
    }
    for (String fixedExtern : fixedExterns) {
      rewrittenNames.put(
          fixedExtern,
          new FormalParam(new Identifier(FilePosition.UNKNOWN, fixedExtern)));
    }
    // If the input NameContext contains (foo => a, bar => b) then the program
    // looks like { (function (a, b) { @rewrittenExpression; }; }
    Block program = (Block) QuasiBuilder.substV(
        "{ (function (@formals*) { @f; }); }",
        "formals", new ParseTreeNodeContainer(
            Lists.newArrayList(rewrittenNames.values())),
        "f", new ExpressionStmt(f));
    MessageQueue sanityCheckMq = DevNullMessageQueue.singleton();
    Set<String> freeIdents = Sets.newLinkedHashSet();
    Scope programScope = Scope.fromProgram(program, sanityCheckMq);
    checkScope(program, programScope, freeIdents);

    if (!freeIdents.isEmpty()) {
      List<MessagePart> freeVarParts = Lists.newArrayList();
      for (String freeIdent : freeIdents) {
        freeVarParts.add(MessagePart.Factory.valueOf(freeIdent));
      }
      mq.addMessage(
          RewriterMessageType.ALPHA_RENAMING_FAILURE, e.getFilePosition(),
          MessagePart.Factory.valueOf(freeVarParts));
      mq.getMessages().addAll(sanityCheckMq.getMessages());
      // Substitute a safe value.  Errors have already been reported on mq.
      return new NullLiteral(e.getFilePosition());
    }
    return f;
  }

  private static void checkScope(ParseTreeNode n, Scope s, Set<String> outers) {
    Scope childScope = s;
    if (n instanceof FunctionConstructor) {
      childScope = Scope.fromFunctionConstructor(s, (FunctionConstructor) n);
    } else if (n instanceof CatchStmt) {
      childScope = Scope.fromCatchStmt(s, (CatchStmt) n);
    } else if (n instanceof Reference) {
      String name = ((Reference) n).getIdentifierName();
      if (isOuter(name, s)) { outers.add(name); }
    } else if (Operation.is(n, Operator.MEMBER_ACCESS)) {
      Operation op = (Operation) n;
      checkScope(op.children().get(0), s, outers);
      return;
    }
    for (ParseTreeNode child : n.children()) {
      checkScope(child, childScope, outers);
    }
  }

  private static boolean isOuter(String name, Scope s) {
    if ("this".equals(name) || "arguments".equals(name)) {
      return s.isOuter();
    } else {
      return s.isOuter(name);
    }
  }

  private AlphaRenaming() { /* uninstantiable */ }
}
