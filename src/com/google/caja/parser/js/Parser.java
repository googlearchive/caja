// Copyright (C) 2005 Google Inc.
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

package com.google.caja.parser.js;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.JsTokenQueue;
import com.google.caja.lexer.JsTokenType;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.Punctuation;
import com.google.caja.lexer.Token;
import com.google.caja.lexer.TokenQueue.Mark;
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParserBase;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.reporting.MessageType;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Pair;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Parses javascript source returning a javascript parse tree.
 *
 * <p>
 * The grammar below is a context-free representation of the grammar this
 * parser parses.  It disagrees with EcmaScript 262 Edition 3 (ES3) where
 * implementations disagree with ES3.  The rules for semicolon insertion and
 * the possible backtracking in expressions needed to properly handle
 * backtracking are commented thoroughly in code, since semicolon insertion
 * requires information from both the lexer and parser and is not determinable
 * with finite lookahead.
 * <p>
 * Noteworthy features
 * <ul>
 * <li>Reports warnings on a queue where an error doesn't prevent any further
 *   errors, so that we can report multiple errors in a single compile pass
 *   instead of forcing developers to play whack-a-mole.
 * <li>Does not parse Firefox style {@code catch (<Identifier> if <Expression>)}
 *   since those don't work on IE and many other interpreters.
 * <li>Recognizes {@code const} since many interpreters do (not IE) but warns.
 * <li>Allows, but warns, on trailing commas in {@code Array} and {@code Object}
 *   constructors.
 * <li>Allows keywords as identifier names but warns since different
 *   interpreters have different keyword sets.  This allows us to use an
 *   expansive keyword set.</li>
 * </ul>
 * To parse strict code, pass in a {@code PedanticWarningMessageQueue} that
 * converts {@link MessageLevel#WARNING} and above to
 * {@link MessageLevel#FATAL_ERROR}.
 *
 * <xmp>
 * // Terminal productions
 * Program                 => <DirectivePrologue>? <TerminatedStatement>*
 * Statement               => <TerminalStatement>
 *                          | <NonTerminalStatement>
 * Expression              => <CommaOperator>
 *
 * // Non-terminal productions.  Indentation indicates that a nonterminal is
 * // used only by the unindented nonterminal above it.
 * DirectivePrologue       => <Directive> <DirectivePrologue>?
 * Directive               => <StringLiteral> ';'
 * TerminatedStatement     => <BlockStatement>
 *                          | <SimpleStatement> <StatementTerminator>
 *   StatementTerminator   => ';'
 *                          | <InsertedSemicolon>
 *   BlockStatement        => <StatementLabel>? <BlockStatementAtom>
 *   SimpleStatement       => <StatementLabel>? <SimpleStatementAtom>
 *   InsertedSemicolon     => &epsilon;  // abort if disallowed in context
 *   StatementLabel        => <Identifier> ':'
 *   BlockStatementAtom    => <Block>
 *                          | <Conditional>
 *                          | <For>
 *                          // Noop is a block statement atom since it's
 *                          // guaranteed to end with a semicolon.  If it were
 *                          // a simple statement epsilon, then it could be
 *                          // inserted anywhere that semicolon insertion is
 *                          // allowed.
 *                          | <Noop>
 *                          | <Switch>
 *                          | <Try>
 *                          | <While>
 *                          | <With>
 *   SimpleStatementAtom   => <Break>
 *                          | <Continue>
 *                          | <Debugger>
 *                          // Do-While loops are simple because, unlike other
 *                          // loops their body isn't last, so they aren't
 *                          // guaranteed to end with a right-curly or semi.
 *                          | <Do>
 *                          | <ExprStatement>
 *                          | <FunctionDeclaration>
 *                          | <Declaration>
 *                          | <Throw>
 *                          | <Return>
 *
 * Identifier              != <Keyword>
 *                          | <Word>
 * Body                    => <TerminatedStatement>
 *
 * Block                   => '{' <TerminatedStatement>* '}'
 * Conditional             => 'if' '(' <Expression> ')' <Body> <Else>
 *   Else                  => 'else' <Body>
 *                          | &epsilon;
 * For                     => 'for' '(' ';' <ExpressionOrNoop> <Expression>? ')'
 *                            <Body>
 *                          | 'for' '(' <DeclarationStart> 'in' <Expression> ')'
 *                            <Body>
 *                          | 'for' '(' <LValue> 'in' <Expression> ')'
 *                            <Body>
 *                          | 'for' '(' <Declaration> ';' <ExpressionOrNoop>
 *                            <Expression>? ')' <Body>
 *                          | 'for' '(' <Expression> ';' <ExpressionOrNoop>
 *                            <Expression>? ')' <Body>
 * Noop                    => ';'
 * Switch                  => 'switch' '(' <Expression> ')' '{' <Cases> '}'
 *   Cases                 => <Case>* <DefaultAndCases>?
 *     DefaultAndCases     => <Default> <Case>*
 *   Case                  => 'case' <Expression> ':' <TerminatedStatement>*
 *   Default               => 'default' ':' <TerminatedStatement>*
 * Try                     => 'try' <Body> <TryClauses>
 *   TryClauses            => <Catch> <Finally>?
 *                          | <Finally>
 *   Catch                 => 'catch' '(' <Identifier> ')' <Body>
 *   Finally               => 'finally' <Body>
 * While                   => 'while' '(' <Expression> ')' <Body>
 * With                    => 'with' '(' <Expression> ')' <Body>
 * Do                      => 'do' <Body> 'while' '(' <Expression> ')'
 * Break                   => 'break' [no LineTerminator] <StatementLabel>?
 * Continue                => 'continue' [no LineTerminator] <StatementLabel>?
 * Debugger                => 'debugger'
 * Return                  => 'return' [no LineTerminator] <Expression>?
 * Throw                   => 'throw' [no LineTerminator] <Expression>
 * ExprStatement           => <Expression>
 *
 * ExpressionOrNoop        => <Expression>?
 * DeclarationStart        => <DeclarationKeyword> <Identifier>
 * Declaration             => <DeclarationKeyword> <DeclarationBodyList>
 *   DeclarationKeyword    => 'var' | 'const'
 *   DeclarationBodyList   => <DeclarationBody> <DeclarationBodyTail>
 *   DeclarationBodyTail   => ',' <DeclarationBodyList>
 *                          | &epsilon;
 *   DeclarationBody       => <Identifier> <InitialValue>?
 *   InitialValue          => '=' <Expression>
 *
 * FunctionDeclaration     => <NamedFunction>
 * FunctionConstructor     => <NamedFunction>
 *                          | <AnonymousFunction>
 *   AnonymousFunction     => 'function' <Formals> <FunctionBody>
 * NamedFunction           => 'function' <Identifier> <Formals> <FunctionBody>
 * FunctionBody            => '{' <Program> '}'
 * Formals                 => '(' <IdentifierList>? ')'
 *   IdentifierList        => <Identifier> <IdentifierListTail>
 *   IdentifierListTail    => ',' <IdentifierList>
 *
 * // All of the operators have an associated precedence defined in the Operator
 * // enum.  The p variable on the right of the productions is the precedence
 * // of the most recently consumed operator.
 * CommaOperator           => <Operator(MAX)>
 *                          | <CommaOperator> ',' <Operator(MAX)>
 * Operator(prec)          => <OperatorHead(prec)> <OperatorTail(prec)>
 *   OperatorHead(prec)    => <PrefixOperator(p < prec)> <Operator(p + 1)>
 *                          | <ExpressionAtom>
 *   OperatorTail(prec)    => [no LineTerminator] <PostfixOperator(p < prec)>
 *                          | <RightAssocInfixOperator(p <= prec)> <Operator(p)>
 *                          | <LeftAssocInfixOperator(p < prec)> <Operator(p)>
 *                         // below only match if p < prec
 *                          | '[' <Expression> ']'
 *                          | '(' <ActualList>? ')'
 *                          | '?' <Operator(TERNARY)> ':' <Operator(TERNARY)>
 *                          | <ExpressionAtom>
 *     // ActualList uses Operator(MAX) to prevent parsing actuals from being
 *     // treated as operands to the comma operator.  Since the parentheses
 *     // recurse to Expression under ExpressionAtom, the result of a comma
 *     // operator can be passed to a function by nesting in parens.
 *     ActualList          => <Operator(MAX)> <ActualListTail>?
 *     ActualListTail      => ',' <ActualList>
 *                          | &epsilon;
 *   ExpressionAtom        => <StringLiteral>
 *                          | <NumberLiteral>
 *                          | <RegexpLiteral>
 *                          | 'null' | 'true' | 'false' | 'this'
 *                          | <FunctionConstructor>
 *                          | <Keyword>  // error and treat as identifier
 *                          | <Identifier>
 *                          | '(' <Expression> ')'
 *                          | <ArrayConstructor>
 *                          | <ObjectConstructor>
 *                          | &epsilon;  // in error-tolerant mode
 *   ArrayConstructor      => '[' <ArrayElements> <TrailingComma>? ']'
 *     ArrayElements       => <ArrayElementsHead>* <Expression>
 *                          | &epsilon;
 *     ArrayElementsHead   => <Expression>? ','
 *   ObjectConstructor     => '{' <ObjPropertyList> '}'
 *     ObjPropertyList     => <ObjPropery> <ObjProperties>* <TrailingComma>?
 *     ObjProperties       => ',' <ObjectPreperty>
 *     ObjProperty         => <ValueProperty>
 *                          | <GetterProperty>
 *                          | <SetterProperty>
 *       ValueProperty     => <PropName> ':' <Expression>
 *       GetterProperty    => 'get' <PropName> <Formals> <FunctionBody>
 *       SetterProperty    => 'set' <PropName> <Formals> <FunctionBody>
 *         ObjPropertyKey  => <Identifier>
 *                          | <StringLiteral>
 *                          | <NumberLiteral>
 *                          | <Keyword>  // set allowed interpreter dependent
 *   TrailingComma         => ','  // warn
 * </xmp>
 * See also {@link Keyword} for definitions of keywords, {@link Operator} for
 * operator precedences and associativity, and {@link JsTokenType} for
 * definitions of Word, NumberLiteral, and StringLiteral.
 *
 * <p>Questions as to whether a semicolon should be inserted can only be
 * answered at the lexical level.  A lexer can emit a comment-like-token where
 * semicolons can be inserted though this is not the approach taken in this
 * implementation.</p>
 *
 * @author mikesamuel@gmail.com
 */
public final class Parser extends ParserBase {
  private boolean recoverFromFailure;

  public Parser(JsTokenQueue tq, MessageQueue mq) {
    this(tq, mq, false);
  }

  public Parser(JsTokenQueue tq, MessageQueue mq, boolean isQuasiliteral) {
    super(tq, mq, isQuasiliteral);
  }

  /**
   * True iff the parser will attempt to recover from errors that can be
   * attributed to a missing run of parenthetically balanced tokens by inserting
   * a placeholder reference "_".
   */
  public boolean getRecoverFromFailure() {
    return recoverFromFailure;
  }

  /**
   * Setter corresponding to {@link #getRecoverFromFailure}.
   */
  public void setRecoverFromFailure(boolean shouldRecover) {
    this.recoverFromFailure = shouldRecover;
  }

  /** Parses a top level block. */
  public Block parse() throws ParseException {
    Block program = parseProgram();
    tq.expectEmpty();
    return program;
  }

  /** Parses and returns a Statement. */
  public Statement parseStatement() throws ParseException {
    // look for any labels preceding statement
    Mark m = tq.mark();
    Token<JsTokenType> t = tq.peek();
    if (JsTokenType.WORD == t.type) {
      String label = parseIdentifier(false);
      FilePosition labelPos = t.pos;
      if (tq.checkToken(Punctuation.COLON)) {
        t = tq.peek();
        AbstractStatement s = null;
        if (JsTokenType.KEYWORD == t.type) {
          switch (Keyword.fromString(t.text)) {
            case FOR: case DO: case WHILE: case SWITCH:
              s = parseLoopOrSwitch(labelPos, label);
              break;
            default:
              break;
          }
        }
        if (null == s) {
          Statement labelless = parseStatementWithoutLabel();
          s = new LabeledStmtWrapper(posFrom(m), label, labelless);
        }
        finish(s, m);
        return s;
      }
      tq.rewind(m);
    }
    return parseStatementWithoutLabel();
  }

  private Block parseFunctionBody() throws ParseException {
    return parseProgramOrFunctionBody(true);
  }

  private Block parseProgram() throws ParseException {
    return parseProgramOrFunctionBody(false);
  }

  private Block parseProgramOrFunctionBody(boolean requireBrackets)
      throws ParseException {
    Mark m = tq.mark();
    if (requireBrackets) { tq.expectToken(Punctuation.LCURLY); }
    List<Statement> stmts = Lists.newArrayList();
    DirectivePrologue prologue = parseOptionalDirectivePrologue();
    if (prologue != null) { stmts.add(prologue); }
    while (!tq.isEmpty() && !tq.lookaheadToken(Punctuation.RCURLY)) {
      stmts.add(parseTerminatedStatement());
    }
    if (requireBrackets) { tq.expectToken(Punctuation.RCURLY); }
    Block b = new Block(posFrom(m), stmts);
    finish(b, m);
    return b;
  }

  private DirectivePrologue parseOptionalDirectivePrologue()
      throws ParseException {
    // Quick return if we are sure we will not accumulate anything
    if (tq.isEmpty() || tq.peek().type != JsTokenType.STRING) { return null; }

    Mark startOfPrologue = tq.mark();
    List<Directive> directives = Lists.newArrayList();

    while (!tq.isEmpty() && tq.peek().type == JsTokenType.STRING) {
      Mark startOfDirective = tq.mark();
      Token<JsTokenType> quotedString = tq.pop();

      if (!tq.checkToken(Punctuation.SEMI)) {
        Token<JsTokenType> t = !tq.isEmpty() ? tq.peek() : null;
        if ((t == null || !continuesExpr(t.text)) && semicolonInserted()) {
          FilePosition semiPoint = FilePosition.endOf(tq.lastPosition());
          MessageLevel lvl = tq.isEmpty()
              || tq.lookaheadToken(Punctuation.RCURLY)
              ? MessageLevel.LOG : MessageLevel.LINT;
          mq.addMessage(MessageType.SEMICOLON_INSERTED, lvl, semiPoint);
        } else {
          tq.rewind(startOfDirective);
          break;
        }
      }

      String unquoted = quotedString.text.substring(
          1, quotedString.text.length() - 1);
      String decoded = StringLiteral.getUnquotedValueOf(quotedString.text);
      if (!unquoted.equals(decoded)
          || !Directive.RecognizedValue.isDirectiveStringRecognized(unquoted)) {
        mq.addMessage(
            MessageType.UNRECOGNIZED_DIRECTIVE_IN_PROLOGUE,
            quotedString.pos, MessagePart.Factory.valueOf(unquoted));
      }

      Directive d = new Directive(posFrom(quotedString.pos), unquoted);
      finish(d, startOfDirective);
      directives.add(d);
    }

    if (directives.isEmpty()) { return null; }

    DirectivePrologue prologue = new DirectivePrologue(
        posFrom(startOfPrologue), directives);
    finish(prologue, startOfPrologue);
    return prologue;
  }
  private static boolean continuesExpr(String tokenText) {
    return Operator.lookupOperation(tokenText, OperatorType.INFIX) != null
        || Operator.lookupOperation(tokenText, OperatorType.BRACKET) != null
        || Operator.lookupOperation(tokenText, OperatorType.TERNARY) != null;
  }

  private LabeledStatement parseLoopOrSwitch(FilePosition start, String label)
      throws ParseException {
    Token<JsTokenType> t = tq.peek();
    LabeledStatement s;
    switch (Keyword.fromString(t.text)) {
      case FOR:
      {
        tq.advance();
        tq.expectToken(Punctuation.LPAREN);
        if (tq.checkToken(Punctuation.SEMI)) {
          Statement initializer = noop(tq.lastPosition());
          Expression condition = parseExpressionOrNoop(new BooleanLiteral(
              FilePosition.startOf(tq.currentPosition()), true), true);
          Statement increment;

          if (!tq.checkToken(Punctuation.RPAREN)) {
            increment = parseExpressionStmt(true);
            tq.expectToken(Punctuation.RPAREN);
          } else {
            increment = noop(tq.lastPosition());
          }
          Statement body = parseBody(true);

          s = new ForLoop(
              posFrom(start), label, initializer, condition, increment, body);
        } else {
          Statement initializer = parseDeclarationsOrExpression(true);
          Expression initializerExpr = null;

          if ((initializer instanceof Declaration  // no multi-decls
               && null == ((Declaration) initializer).getInitializer()
               && tq.checkToken(Keyword.IN))
              || (!tq.lookaheadToken(Punctuation.SEMI)
                  && (initializerExpr = checkInExprWithLhs(initializer)) != null
                  )) {

            Expression iterable;
            Expression lvalue;
            if (null == initializerExpr) {
              iterable = parseExpressionInt(true);
              lvalue = null;
            } else {
              Operation op = (Operation) initializerExpr;
              lvalue = op.children().get(0);
              iterable = op.children().get(1);
            }

            tq.expectToken(Punctuation.RPAREN);
            Statement body = parseBody(true);

            if (null == lvalue) {
              s = new ForEachLoop(
                  posFrom(start),
                  label, (Declaration) initializer, iterable, body);
            } else {
              s = new ForEachLoop(
                  posFrom(start), label, lvalue, iterable, body);
            }

          } else {
            Mark m = tq.mark();
            tq.expectToken(Punctuation.SEMI);
            Expression condition = parseExpressionOrNoop(
                new BooleanLiteral(posFrom(m), true), true);
            Statement increment;

            if (!tq.checkToken(Punctuation.RPAREN)) {
              increment = parseExpressionStmt(true);
              tq.expectToken(Punctuation.RPAREN);
            } else {
              increment = noop(tq.lastPosition());
            }
            Statement body = parseBody(true);
            s = new ForLoop(
                posFrom(start), label, initializer, condition, increment, body);
          }
        }
        break;
      }
      case WHILE:
      {
        tq.advance();
        tq.expectToken(Punctuation.LPAREN);
        Expression cond = parseExpressionInt(true);
        tq.expectToken(Punctuation.RPAREN);
        Statement body = parseBody(true);
        s = new WhileLoop(posFrom(start), label, cond, body);
        break;
      }
      case DO:
      {
        tq.advance();
        Statement body = parseBody(false);
        tq.expectToken(Keyword.WHILE);
        tq.expectToken(Punctuation.LPAREN);
        Expression cond = parseExpressionInt(true);
        tq.expectToken(Punctuation.RPAREN);
        s = new DoWhileLoop(posFrom(start), label, body, cond);
        // http://code.google.com/p/google-caja/issues/detail?id=1316
        // ES[35] requires ; after do-while, but browsers are ok without it.
        // Here we either eat a ; or warn if it's missing.
        if (!tq.checkToken(Punctuation.SEMI)) {
          FilePosition pos = FilePosition.endOf(tq.lastPosition());
          mq.addMessage(
              MessageType.SEMICOLON_INSERTED, MessageLevel.LINT, pos);
        }
        break;
      }
      case SWITCH:
      {
        tq.advance();
        tq.expectToken(Punctuation.LPAREN);
        Expression switchValue = parseExpressionInt(true);
        tq.expectToken(Punctuation.RPAREN);
        tq.expectToken(Punctuation.LCURLY);
        List<SwitchCase> cases = Lists.newArrayList();
        while (!tq.checkToken(Punctuation.RCURLY)) {
          Mark caseMark = tq.mark();
          Expression caseValue;
          if (tq.checkToken(Keyword.DEFAULT)) {
            caseValue = null;
          } else {
            tq.expectToken(Keyword.CASE);
            caseValue = parseExpressionInt(false);
          }
          tq.expectToken(Punctuation.COLON);
          FilePosition colonPos = tq.lastPosition();
          Mark caseBodyStart = tq.mark();
          List<Statement> caseBodyContents = Lists.newArrayList();
          while (!(tq.lookaheadToken(Keyword.DEFAULT)
                   || tq.lookaheadToken(Keyword.CASE)
                   || tq.lookaheadToken(Punctuation.RCURLY))) {
            caseBodyContents.add(parseTerminatedStatement());
          }
          FilePosition caseBodyPos = caseBodyContents.isEmpty()
              ? FilePosition.endOf(colonPos) : posFrom(caseBodyStart);
          Block caseBody = new Block(caseBodyPos, caseBodyContents);
          finish(caseBody, caseBodyStart);
          SwitchCase caseStmt = (null != caseValue)
              ? new CaseStmt(posFrom(caseMark), caseValue, caseBody)
              : new DefaultCaseStmt(posFrom(caseMark), caseBody);
          finish(caseStmt, caseMark);
          cases.add(caseStmt);
        }

        s = new SwitchStmt(posFrom(start), label, switchValue, cases);
        break;
      }
      default:
        throw new SomethingWidgyHappenedError(t.text);
    }
    return s;
  }

  private AbstractStatement parseStatementWithoutLabel() throws ParseException {
    Mark m = tq.mark();

    Token<JsTokenType> t = tq.peek();

    if (JsTokenType.KEYWORD == t.type) {
      AbstractStatement s;
      switch (Keyword.fromString(t.text)) {
        case FOR: case DO: case WHILE: case SWITCH:
          s = parseLoopOrSwitch(t.pos, "");
          break;
        case IF:
        {
          tq.advance();
          List<Pair<Expression, Statement>> clauses = Lists.newArrayList();
          Statement elseClause = null;
          boolean sawElse;
          do {
            tq.expectToken(Punctuation.LPAREN);
            Expression cond = parseExpressionInt(true);
            tq.expectToken(Punctuation.RPAREN);
            Statement body = parseBody(false);
            sawElse = tq.checkToken(Keyword.ELSE);
            clauses.add(new Pair<Expression, Statement>(cond, body));
          } while (sawElse && tq.checkToken(Keyword.IF));
          if (sawElse) {
            elseClause = parseBody(true);
          }
          s = new Conditional(posFrom(m), clauses, elseClause);
          break;
        }
        case VAR:
        case CONST:
          return parseDeclarationsOrExpression(false);
        case FUNCTION:
        {
          Mark fs = tq.mark();
          tq.advance();
          if (tq.lookaheadToken(Punctuation.LPAREN)) {
            // If no name, then treat it as an expression
            tq.rewind(fs);
            return parseExpressionStmt(false);
          } else {  // a function declaration
            Identifier identifier = parseIdentifierNode(false);
            tq.expectToken(Punctuation.LPAREN);
            FormalParamList params = parseFormalParams();
            tq.expectToken(Punctuation.RPAREN);
            Block body = parseFunctionBody();
            FunctionConstructor fc = new FunctionConstructor(
                posFrom(m), identifier, params.params, body);
            finish(fc, m);
            s = new FunctionDeclaration(fc);
            finish(s, m);
          }
          break;
        }
        case RETURN:
        {
          tq.advance();
          AbstractExpression value;
          // Check for semicolon insertion without lookahead since return is a
          // restricted production. See the grammar above and ES3 or ES5 S7.9.1
          if (semicolonInserted() || tq.lookaheadToken(Punctuation.SEMI)) {
            value = null;
          } else {
            value = parseExpressionInt(false);
          }
          s = new ReturnStmt(posFrom(m), value);
          break;
        }
        case BREAK:
        {
          tq.advance();
          String targetLabel = "";
          if (!semicolonInserted() && JsTokenType.WORD == tq.peek().type) {
            targetLabel = parseIdentifier(false);
          }
          s = new BreakStmt(posFrom(m), targetLabel);
          break;
        }
        case CONTINUE:
        {
          tq.advance();
          String targetLabel = "";
          if (!semicolonInserted() && JsTokenType.WORD == tq.peek().type) {
            targetLabel = parseIdentifier(false);
          }
          s = new ContinueStmt(posFrom(m), targetLabel);
          break;
        }
        case DEBUGGER:
        {
          tq.advance();
          s = new DebuggerStmt(posFrom(m));
          break;
        }
        case THROW:
        {
          tq.advance();
          if (semicolonInserted()) {
            throw new ParseException(new Message(
                MessageType.EXPECTED_TOKEN,
                FilePosition.endOf(tq.lastPosition()),
                MessagePart.Factory.valueOf("<expression>"),
                MessagePart.Factory.valueOf("<newline>")));
          }
          Expression ex = parseExpressionInt(false);
          s = new ThrowStmt(posFrom(m), ex);
          break;
        }
        case TRY:
        {
          tq.advance();
          Block body = parseBodyBlock();
          CatchStmt handler;
          FinallyStmt finallyBlock;
          Mark m2 = tq.mark();
          boolean sawFinally = tq.checkToken(Keyword.FINALLY);
          if (sawFinally) {
            handler = null;
          } else {
            tq.expectToken(Keyword.CATCH);
            tq.expectToken(Punctuation.LPAREN);
            Identifier idNode = parseIdentifierNode(false);
            Declaration exvar = new Declaration(
                idNode.getFilePosition(), idNode, (Expression)null);
            exvar.setComments(idNode.getComments());
            tq.expectToken(Punctuation.RPAREN);
            Block catchBody = parseBodyBlock();
            handler = new CatchStmt(posFrom(m2), exvar, catchBody);
            finish(handler, m2);
            m2 = tq.mark();
            sawFinally = tq.checkToken(Keyword.FINALLY);
          }
          if (sawFinally) {
            Block st = parseBodyBlock();
            finallyBlock = new FinallyStmt(posFrom(m2), st);
            finish(finallyBlock, m2);
          } else {
            finallyBlock = null;
          }
          s = new TryStmt(posFrom(m), body, handler, finallyBlock);
          break;
        }
        case WITH:
        {
          tq.advance();
          tq.expectToken(Punctuation.LPAREN);
          Expression scopeObject = parseExpressionInt(true);
          tq.expectToken(Punctuation.RPAREN);
          Statement body = parseBody(true);
          s = new WithStmt(posFrom(m), scopeObject, body);
          break;
        }
        default:
          return parseExpressionStmt(false);
      }
      finish(s, m);
      return s;
    } else if (tq.checkToken(Punctuation.LCURLY)) {
      // In a statement a curly block opens a block.
      // Blocks don't have a scope associated, so are effectively useless,
      // except to group statements in a loop.
      List<Statement> blockParts = Lists.newArrayList();
      while (!tq.checkToken(Punctuation.RCURLY)) {
        blockParts.add(parseTerminatedStatement());
      }
      Block b = new Block(posFrom(m), blockParts);
      finish(b, m);
      return b;
    } else if (tq.checkToken(Punctuation.SEMI)) {
      return noop(tq.lastPosition());
    } else {
      return parseExpressionStmt(false);
    }
  }

  /**
   * Parses an expression.
   * @param insertionProtected true iff the expression appears directly inside
   *     parentheses or square brackets or in some other context where
   *     semicolons cannot be inserted.  For example, the x in {@code f(x);}
   *     appears in an insertionProtected contexts, but the x in {@code x = 1;}
   *     does not.
   * @return non null.
   */
  public Expression parseExpression(boolean insertionProtected)
      throws ParseException {
    return parseExpressionInt(insertionProtected);
  }

  private AbstractExpression parseExpressionInt(boolean insertionProtected)
      throws ParseException {
    Mark m = tq.mark();
    AbstractExpression e = parseOp(Integer.MAX_VALUE, insertionProtected);
    // Handle comma operator
    while (tq.checkToken(Punctuation.COMMA)) {
      // The comma operator is left-associative so parse expression part in loop
      // instead of recursing
      Expression right = parseExpressionPart(insertionProtected);
      e = Operation.create(posFrom(m), Operator.COMMA, e, right);
      finish(e, m);
    }
    return e;
  }

  /**
   * Parses an expression part -- one that can appear inside a comma separated
   * list.  This differs from {@link #parseExpression} in that it will not parse
   * <code>a, b</code> as the comma will be interpreted as a terminator,
   * although it will parse <code>(a, b)</code>.
   * @return non null.
   */
  public Expression parseExpressionPart(boolean insertionProtected)
      throws ParseException {
    return parseOp(Integer.MAX_VALUE, insertionProtected);
  }

  private AbstractExpression parseOp(
      int precedence, boolean insertionProtected)
      throws ParseException {
    AbstractExpression left = null;
    // Handle prefix operations
    {
      Token<JsTokenType> t = tq.peek();
      Operator op = Operator.lookupOperation(t.text, OperatorType.PREFIX);
      if (null != op) {
        Mark m = tq.mark();
        tq.advance();
        int opprec = op.getPrecedence();
        if (opprec < precedence) {
          // The opprec + 1 may look a bit odd but it allows binary operators
          // to associate when they have the same precedence as the prefix
          // op preceding them.  This is the desired behavior:
          // new Foo[4] should parenthesize as new (Foo[4]) as verified by
          // the fact that new Object['toString'] fails to parse in FF.
          // It introduces no problem since there are no right-associative
          // binary operators with precedence 2 or 5.
          left = parseOp(opprec + 1, insertionProtected);
        } else {
          throw new ParseException(
              new Message(MessageType.UNEXPECTED_TOKEN, t.pos,
                          MessagePart.Factory.valueOf(t.text)));
        }
        if (op == Operator.CONSTRUCTOR && tq.checkToken(Punctuation.LPAREN)) {
          List<Expression> operands = Lists.newArrayList();
          operands.add(left);
          if (!tq.checkToken(Punctuation.RPAREN)) {
            do {
              operands.add(parseExpressionPart(true));
            } while (tq.checkToken(Punctuation.COMMA));
            tq.expectToken(Punctuation.RPAREN);
          }
          left = new SpecialOperation(posFrom(m), op, operands);
        } else {
          try {
            left = Operation.create(posFrom(m), op, left);
          } catch (IllegalArgumentException e) {
            throw new ParseException(
                new Message(MessageType.ASSIGN_TO_NON_LVALUE, t.pos,
                    MessagePart.Factory.valueOf(t.text)));
          }
        }
        finish(left, m);
        // Not pulling multiple operators off the stack means that
        // some prefix operator nestings are impossible.  This is intended.
        // This prevents such things as (new (++i)).
        // This only affects the new operator though since it is the only
        // prefix operator with a precedence != 4.
      }
      if (null == left) {
        left = parseExpressionAtom();
      }
    }

    // Parse binary operators, except comma.
    while (!tq.isEmpty()) {
      Token<JsTokenType> t = tq.peek();
      // If it is a binary op then we should consider using it
      Operator op = Operator.lookupOperation(t.text, OperatorType.INFIX);
      if (null == op) {
        op = Operator.lookupOperation(t.text, OperatorType.BRACKET);
        if (null == op) {
          op = Operator.lookupOperation(t.text, OperatorType.TERNARY);
          // Check for semicolon insertion since postfix operators are
          // "restricted productions" according to ES3 or ES5 S7.9.1.
          if (null == op) {
            if (!semicolonInserted()) {
              op = Operator.lookupOperation(t.text, OperatorType.POSTFIX);
            }
            if (null == op) { break; }
          }
        }
      } else if (Operator.COMMA == op) {
        break;
      }
      int opprec = op.getPrecedence();
      if (!(opprec < precedence
            || (opprec == precedence
                && Associativity.RIGHT == op.getAssociativity()))) {
        break;
      }

      if (op.getType() == OperatorType.BRACKET) {
        checkForMissingSemicolon();
      }

      Mark opStart = tq.mark();
      int nMessages = mq.getMessages().size();
      tq.advance();  // Consume the operator token

      Expression right;
      try {
        // Recurse to parse operator arguments.
        if (OperatorType.BRACKET == op.getType()) {
          if (Operator.FUNCTION_CALL == op) {
            List<Expression> actuals;
            if (tq.checkToken(op.getClosingSymbol())) {
              actuals = Collections.<Expression>emptyList();
            } else {
              actuals = Lists.newArrayList();
              do {
                actuals.add(parseExpressionPart(true));
              } while (tq.checkToken(Punctuation.COMMA));
              tq.expectToken(op.getClosingSymbol());
            }

            right = new ActualList(actuals);
          } else {
            right = parseExpressionInt(true);
            tq.expectToken(op.getClosingSymbol());
          }
        } else if (OperatorType.POSTFIX == op.getType()) {
          right = null;
        } else if (OperatorType.TERNARY == op.getType()) {
          right = parseExpressionPart(insertionProtected);
        } else if (Operator.MEMBER_ACCESS != op) {
          right = parseOp(opprec, insertionProtected);
        } else {
          // The . operator only accepts a reference on the right.
          // No a.b.4 or a.b.(c.d)
          right = parseReference(true);
        }
      } catch (ParseException ex) {
        // According to
        // http://www.mozilla.org/js/language/js20/rationale/syntax.html
        // semicolon insertion requires that we reconsider the decision to
        // treat op as a binary op if it could be a prefix op.

        // Line-Break Semicolon Insertion
        // If the first through the nth tokens of a JavaScript program form
        // are grammatically valid but the first through the n+1st tokens
        // are not and there is a line break between the nth tokens and the
        // n+1st tokens, then the parser tries to parse the program again
        // after inserting a VirtualSemicolon token between the nth and the
        // n+1st tokens.
        if ((Operator.FUNCTION_CALL == op
             || null != Operator.lookupOperation(
                 op.getOpeningSymbol(), OperatorType.PREFIX))
            && !insertionProtected) {
          Mark m3 = tq.mark();
          tq.rewind(opStart);
          if (semicolonInserted()) {
            List<Message> messages = mq.getMessages();
            if (nMessages < messages.size()) {
              messages.subList(nMessages, messages.size()).clear();
            }
            FilePosition semiPoint = FilePosition.endOf(tq.lastPosition());
            messages.add(new Message(
                             MessageType.SEMICOLON_INSERTED, semiPoint));
            return left;
          } else {
            tq.rewind(m3);
          }
        }
        throw ex;
      }
      switch (op.getType()) {
        case TERNARY:
          {
            tq.expectToken(op.getClosingSymbol());
            Expression farRight = parseExpressionPart(insertionProtected);
            left = Operation.create(posFrom(left), op, left, right, farRight);
          }
          break;
          case BRACKET:
            assert right != null;
            if (Operator.FUNCTION_CALL == op) {
              // Function calls can take nothing or multiple on the right, so
              // we wrap function calls up in an ActualList.
              ActualList actuals = (ActualList) right;
              List<? extends Expression> params = actuals.children();
              Expression[] operands = new Expression[params.size() + 1];
              operands[0] = left;
              for (int i = 1; i < operands.length; ++i) {
                operands[i] = params.get(i - 1);
              }
              left = Operation.create(posFrom(left), op, operands);
            } else {
              left = Operation.create(posFrom(left), op, left, right);
            }
            break;
          case INFIX:
            if (op.getCategory() == OperatorCategory.ASSIGNMENT
                && !left.isLeftHandSide()) {
              throw new ParseException(
                  new Message(MessageType.ASSIGN_TO_NON_LVALUE,
                              t.pos, MessagePart.Factory.valueOf(t.text)));
            }
            left = Operation.create(posFrom(left), op, left, right);
            break;
          case POSTFIX:
            if (op.getCategory() == OperatorCategory.ASSIGNMENT
                && !left.isLeftHandSide()) {
              throw new ParseException(
                  new Message(MessageType.ASSIGN_TO_NON_LVALUE,
                              t.pos, MessagePart.Factory.valueOf(t.text)));
            }
            left = Operation.create(posFrom(left), op, left);
            break;
          default:
            throw new SomethingWidgyHappenedError();
      }
    }
    return left;
  }

  private boolean semicolonInserted() throws ParseException {
    if (tq.isEmpty() || tq.lookaheadToken(Punctuation.RCURLY)) { return true; }
    FilePosition last = tq.lastPosition(),
              current = tq.currentPosition();
    if (last == null) { return true; }  // Can insert at beginning
    if (current.startLineNo() == last.endLineNo()) { return false; }
    for (Token<JsTokenType> filtered : tq.filteredTokens()) {
      if (filtered.type == JsTokenType.LINE_CONTINUATION) { return false; }
    }
    return true;
  }

  private void checkForMissingSemicolon() throws ParseException {
    FilePosition current = tq.currentPosition();
    FilePosition last = tq.lastPosition();
    if (current.source().equals(last.source())
        && current.startLineNo() > last.endLineNo()) {
      mq.addMessage(MessageType.MAYBE_MISSING_SEMI, FilePosition.endOf(last));
    }
  }

  private static double toNumber(Token<JsTokenType> t) {
    // Double.parseDouble is not locale dependent.
    return Double.parseDouble(t.text);
  }

  private static String floatToString(Token<JsTokenType> t)
      throws ParseException {
    try {
      return NumberLiteral.numberToString(new BigDecimal(t.text));
    } catch (NumberFormatException e) {
      throw new ParseException(
          new Message(
              MessageType.MALFORMED_NUMBER, t.pos,
              MessagePart.Factory.valueOf(t.text)));
    }
  }

  private static NumberLiteral toNumberLiteral(Token<JsTokenType> t) {
    return new RealLiteral(t.pos, toNumber(t));
  }

  private static BigInteger MAX_REPRESENTABLE =
    BigInteger.valueOf((1L << 51) -1);
  private static BigInteger MIN_REPRESENTABLE =
    BigInteger.valueOf(-(1L << 51));
  private strictfp long toInteger(Token<JsTokenType> t) {
    try {
      Number longValue = Long.decode(t.text);
      // Make sure that the number fits in a 51 bit mantissa
      long lv = longValue.longValue();
      if (0 != ((lv < 0 ? ~lv : lv) & ~((1L << 51) - 1))) {
        mq.addMessage(MessageType.UNREPRESENTABLE_INTEGER_LITERAL,
                      t.pos, MessagePart.Factory.valueOf(t.text));
        double dv = lv;  // strictfp affects this.
        return (long) dv;
      }
      return lv;
    } catch (NumberFormatException e) {
      Pair<String, Integer> p = breakOutRadix(t.text);
      BigInteger bi = new BigInteger(p.a, p.b);
      if (bi.compareTo(MIN_REPRESENTABLE) < 0 ||
          bi.compareTo(MAX_REPRESENTABLE) > 0) {
        mq.addMessage(MessageType.UNREPRESENTABLE_INTEGER_LITERAL,
            t.pos, MessagePart.Factory.valueOf(t.text));
      }
      return bi.longValue();
    }
  }

  private static Pair<String, Integer> breakOutRadix(String n) {
    if (n.startsWith("0X") || n.startsWith("0x")) {
      return Pair.pair(n.substring(2), 16);
    } else if (n.startsWith("0")) {
      return Pair.pair(n.substring(1), 8);
    } else {
      return Pair.pair(n, 10);
    }
  }

  private NumberLiteral toIntegerLiteral(Token<JsTokenType> t) {
    try {
      Number longValue = Long.decode(t.text);

      // Make sure that the number fits in a 51 bit mantissa
      long lv = longValue.longValue();
      if (0 != ((lv < 0 ? ~lv : lv) & ~((1L << 51) - 1))) {
        mq.addMessage(MessageType.UNREPRESENTABLE_INTEGER_LITERAL,
                      t.pos, MessagePart.Factory.valueOf(t.text));
        return new RealLiteral(t.pos, lv);
      }

      return new IntegerLiteral(t.pos, lv);
    } catch (NumberFormatException e) {
      Pair<String, Integer> p = breakOutRadix(t.text);
      BigInteger bi = new BigInteger(p.a, p.b);
      if (bi.compareTo(MIN_REPRESENTABLE) < 0 ||
          bi.compareTo(MAX_REPRESENTABLE) > 0) {
        mq.addMessage(MessageType.UNREPRESENTABLE_INTEGER_LITERAL,
            t.pos, MessagePart.Factory.valueOf(t.text));
        return new RealLiteral(t.pos, bi.longValue());
      }
      return new IntegerLiteral(t.pos, bi.longValue());
    }
  }

  @SuppressWarnings("fallthrough")
  private AbstractExpression parseExpressionAtom() throws ParseException {
    AbstractExpression e;
    Mark m = tq.mark();

    Token<JsTokenType> t = tq.pop();
    typeswitch: switch (t.type) {
      case STRING:
        issueLintWarningsForProblematicEscapes(t, mq);
        e = new StringLiteral(t.pos, t.text);
        break;
      case INTEGER:
        if (integerPartIsOctal(t.text)) {
          mq.addMessage(
              MessageType.OCTAL_LITERAL, MessageLevel.LINT,
              t.pos, MessagePart.Factory.valueOf(t.text));
        }
        e = toIntegerLiteral(t);
        break;
      case FLOAT:
        if (integerPartIsOctal(t.text)) {
          mq.addMessage(
              MessageType.OCTAL_LITERAL, MessageLevel.ERROR,
              t.pos, MessagePart.Factory.valueOf(t.text));
        }
        e = toNumberLiteral(t);
        break;
      case REGEXP:
      {
        e = new RegexpLiteral(t.pos, t.text);
        // Check letters.  Warn on s suffix character as non-FF.
        String modifiers = t.text.substring(t.text.lastIndexOf("/") + 1);
        if (!RegexpLiteral.areRegexpModifiersValid(modifiers)) {
          mq.addMessage(
                  MessageType.UNRECOGNIZED_REGEX_MODIFIERS, t.pos,
                  MessagePart.Factory.valueOf(modifiers));
        }
        break;
      }
      case KEYWORD:
      {
        Keyword k = Keyword.fromString(t.text);
        if (null != k) {
          switch (k) {
            case NULL:
              e = new NullLiteral(t.pos);
              break typeswitch;
            case TRUE:
              e = new BooleanLiteral(t.pos, true);
              break typeswitch;
            case FALSE:
              e = new BooleanLiteral(t.pos, false);
              break typeswitch;
            case FUNCTION:
            {
              Identifier identifier = null;
              if (!tq.isEmpty() && JsTokenType.WORD == tq.peek().type) {
                identifier = parseIdentifierNode(false);
              } else {
                identifier = new Identifier(
                    FilePosition.endOf(tq.lastPosition()), null);
              }
              tq.expectToken(Punctuation.LPAREN);
              FormalParamList params = parseFormalParams();
              tq.expectToken(Punctuation.RPAREN);
              Block body = parseFunctionBody();
              e = new FunctionConstructor(
                  posFrom(m), identifier, params.params, body);
              break typeswitch;
            }
            default:
              break;  // Will be handled by the word handler below
          }
        }
        // fall through
      }
      case WORD:
      {
        String identifier;
        if (Keyword.THIS.toString().equals(t.text)) {
          // this is allowed, but not t\u0068is as per the grammar
          identifier = Keyword.THIS.toString();
        } else {
          tq.rewind(m);
          identifier = parseIdentifier(false);
        }
        Identifier idNode = new Identifier(posFrom(m), identifier);
        finish(idNode, m);
        e = new Reference(idNode);
        break;
      }
      case PUNCTUATION:
        switch (Punctuation.fromString(t.text)) {
          case LPAREN:
            e = parseExpressionInt(true);
            tq.expectToken(Punctuation.RPAREN);
            return e;  // Don't pull comments outside parens inside
          case LSQUARE:
          {
            List<Expression> elements = Lists.newArrayList();

            if (!tq.checkToken(Punctuation.RSQUARE)) {
              Mark comma = null;
              do {
                // Handle adjacent commas that specify undefined values.
                // E.g. [1,,2]
                for (Mark cm = tq.mark(); tq.checkToken(Punctuation.COMMA);
                     cm = tq.mark()) {
                  comma = cm;
                  Elision vl = new Elision(posFrom(cm));
                  finish(vl, cm);
                  elements.add(vl);
                }
                if (tq.lookaheadToken(Punctuation.RSQUARE)) { break; }
                comma = null;
                elements.add(parseExpressionPart(true));
              } while (tq.checkToken(Punctuation.COMMA));
              if (comma != null) {
                // On IE, [1,] has length 2 unlike on other browsers.
                mq.addMessage(MessageType.NOT_IE, comma.getFilePosition());
              }
              tq.expectToken(Punctuation.RSQUARE);
            }

            e = new ArrayConstructor(posFrom(m), elements);
            break;
          }
          case LCURLY:
          {
            List<ObjProperty> properties = Lists.newArrayList();
            if (!tq.checkToken(Punctuation.RCURLY)) {
              boolean sawComma;
              do {
                Mark km = tq.mark();
                Token<JsTokenType> keyToken = tq.peek();
                String propertyType = null;
                Mark beforeProperty = km;
                if (keyToken.type == JsTokenType.WORD) {
                  if ("get".equals(keyToken.text)
                      || "set".equals(keyToken.text)) {
                    tq.advance();
                    Mark afterWord = tq.mark();
                    if (!tq.checkToken(Punctuation.COLON)) {
                      propertyType = keyToken.text;
                      beforeProperty = afterWord;
                      keyToken = tq.peek();
                    }
                    tq.rewind(beforeProperty);
                  }
                }
                StringLiteral key;
                switch (keyToken.type) {
                  case STRING:
                    tq.advance();
                    key = new StringLiteral(posFrom(km), keyToken.text);
                    break;
                  case FLOAT:
                    tq.advance();
                    key = StringLiteral.valueOf(
                        posFrom(km), floatToString(keyToken));
                    break;
                  case INTEGER:
                    tq.advance();
                    key = StringLiteral.valueOf(
                        posFrom(km), "" + toInteger(keyToken));
                    break;
                  default:
                    String ident = parseIdentifier(true);
                    key = new StringLiteral(posFrom(km), ident);
                    break;
                }
                finish(key, beforeProperty);
                ObjProperty prop;
                if (propertyType == null) {
                  tq.expectToken(Punctuation.COLON);
                  Expression value = parseExpressionPart(true);
                  prop = new ValueProperty(posFrom(km), key, value);
                } else {
                  Mark beforeFormals = tq.mark();
                  Identifier ident = new Identifier(tq.currentPosition(), null);
                  tq.expectToken(Punctuation.LPAREN);
                  FormalParamList params = parseFormalParams();
                  tq.expectToken(Punctuation.RPAREN);
                  Block body = parseFunctionBody();
                  FunctionConstructor fn = new FunctionConstructor(
                      posFrom(beforeFormals), ident, params.params, body);
                  if ("get".equals(propertyType)) {
                    prop = new GetterProperty(posFrom(km), key, fn);
                  } else {
                    assert "set".equals(propertyType);
                    prop = new SetterProperty(posFrom(km), key, fn);
                  }
                }
                finish(prop, km);
                properties.add(prop);
                Mark cm = tq.mark();
                sawComma = tq.checkToken(Punctuation.COMMA);
                if (sawComma && tq.lookaheadToken(Punctuation.RCURLY)) {
                  tq.rewind(cm);
                  mq.addMessage(MessageType.NOT_IE, tq.currentPosition());
                  tq.advance();
                  break;
                }
              } while (sawComma);
              tq.expectToken(Punctuation.RCURLY);
            }

            e = new ObjectConstructor(posFrom(m), properties);
            break;
          }
          default:
            e = null;
            break;
        }
        break;
      default:
        e = null;
        break;
    }

    if (null == e) {
      if (recoverFromFailure) {
        tq.rewind(m);
        // create a placeholder expression
        FilePosition pos = FilePosition.span(
            tq.lastPosition(), tq.currentPosition());
        mq.addMessage(MessageType.PLACEHOLDER_INSERTED, pos);
        Identifier idNode = new Identifier(pos, "_");
        e = new Reference(idNode);
      } else {
        throw new ParseException(
            new Message(
                MessageType.UNEXPECTED_TOKEN, t.pos,
                MessagePart.Factory.valueOf(t.text)));
      }
    }

    finish(e, m);
    return e;
  }

  private Reference parseReference(boolean allowReservedWords)
      throws ParseException {
    Mark m = tq.mark();
    Identifier idNode = parseIdentifierNode(allowReservedWords);
    Reference r = new Reference(idNode);
    finish(r, m);
    return r;
  }

  private Identifier parseIdentifierNode(boolean allowReservedWords)
      throws ParseException {
    Mark m = tq.mark();
    String identifierName = parseIdentifier(allowReservedWords);
    Identifier ident = new Identifier(posFrom(m), identifierName);
    finish(ident, m);
    return ident;
  }

  private ExpressionStmt parseExpressionStmt(boolean insertionProtected)
      throws ParseException {
    Mark m = tq.mark();
    Expression e = parseExpressionInt(insertionProtected);
    ExpressionStmt es = new ExpressionStmt(posFrom(m), e);
    finish(es, m);
    return es;
  }

  private Expression parseExpressionOrNoop(
      AbstractExpression def, boolean insertionProtected)
      throws ParseException {
    Mark m = tq.mark();
    if (tq.checkToken(Punctuation.SEMI)) {
      finish(def, m);
      return def;
    }
    Expression e = parseExpressionInt(insertionProtected);
    tq.expectToken(Punctuation.SEMI);
    return e;
  }

  private static boolean isTerminal(Statement s) {
    if (s instanceof LabeledStmtWrapper) {
      return isTerminal(((LabeledStmtWrapper) s).getBody());
    }
    // http://code.google.com/p/google-caja/issues/detail?id=1316
    // Note since DoWhileLoop is a Loop, we're allowing do-while to omit
    // the semicolon, which deviates from ES[35] but is allowed by browsers.
    return (s instanceof Loop
            || s instanceof Conditional || s instanceof FunctionDeclaration
            || s instanceof Block || s instanceof TryStmt
            || s instanceof ForEachLoop || s instanceof SwitchStmt)
            || s instanceof Noop || s instanceof WithStmt;
  }

  private Statement parseTerminatedStatement() throws ParseException {
    Statement s = parseStatement();
    if (!isTerminal(s)) { checkSemicolon(); }
    return s;
  }

  private void checkSemicolon() throws ParseException {
    // Look for a semicolon
    if (tq.checkToken(Punctuation.SEMI)) { return; }
    // None found, so maybe do insertion.
    if (tq.isEmpty()) { return; }
    if (semicolonInserted()) {
      FilePosition semiPoint = FilePosition.endOf(tq.lastPosition());
      MessageLevel lvl = tq.isEmpty() || tq.lookaheadToken(Punctuation.RCURLY)
          ? MessageLevel.LOG : MessageLevel.LINT;
      mq.addMessage(MessageType.SEMICOLON_INSERTED, lvl, semiPoint);
    } else {
      tq.expectToken(Punctuation.SEMI);  // Just used to throw an exception
    }
  }

  // Visible for testing.
  static boolean integerPartIsOctal(String numberLiteral) {
    for (int i = 0, n = numberLiteral.length(); i < n; ++i) {
      char ch = numberLiteral.charAt(i);
      if (ch == '.') { return false; }
      if (ch != '0') { return i != 0 && ch >= '1' && ch <= '9'; }
    }
    return false;
  }

  private AbstractStatement parseDeclarationsOrExpression(
      boolean insertionProtected)
      throws ParseException {
    Mark m = tq.mark();

    boolean isDeclaration;

    if (tq.checkToken(Keyword.VAR)) {
      isDeclaration = true;
    } else if (tq.checkToken(Keyword.CONST)) {
      isDeclaration = true;
      mq.addMessage(MessageType.NOT_IE, posFrom(m));
    } else {
      isDeclaration = false;
    }

    if (isDeclaration) {
      AbstractStatement s;
      Declaration d;
      {
        Identifier idNode = parseIdentifierNode(false);
        Expression initializer = null;
        if (tq.checkToken(Punctuation.EQ)) {
          initializer = parseExpressionPart(insertionProtected);
        }
        d = new Declaration(posFrom(m), idNode, initializer);
        finish(d, m);
      }
      if (tq.checkToken(Punctuation.COMMA)) {
        List<Declaration> decls = Lists.newArrayList();
        decls.add(d);
        do {
          Mark m2 = tq.mark();
          Identifier idNode = parseIdentifierNode(false);
          Expression initializer = null;
          if (tq.checkToken(Punctuation.EQ)) {
            initializer = parseExpressionPart(insertionProtected);
          }
          Declaration d2 = new Declaration(posFrom(m2), idNode, initializer);
          finish(d2, m2);
          decls.add(d2);
        } while (tq.checkToken(Punctuation.COMMA));
        MultiDeclaration md = new MultiDeclaration(posFrom(m), decls);
        finish(md, m);
        s = md;
      } else {
        s = d;
      }
      return s;
    } else {
      return parseExpressionStmt(insertionProtected);
    }
  }

  private Statement parseBody(boolean terminal) throws ParseException {
    if (terminal || tq.lookaheadToken(Punctuation.LCURLY)) {
      return parseTerminatedStatement();
    } else {
      Statement s = parseStatement();
      tq.checkToken(Punctuation.SEMI);
      return s;
    }
  }

  private Block parseBodyBlock() throws ParseException {
    if (!tq.lookaheadToken(Punctuation.LCURLY)) {
      tq.expectToken(Punctuation.LCURLY);
    }
    return (Block) parseTerminatedStatement();
  }

  private FormalParamList parseFormalParams() throws ParseException {
    List<FormalParam> params = Lists.newArrayList();
    if (!tq.lookaheadToken(Punctuation.RPAREN)) {
      do {
        Mark m = tq.mark();
        FormalParam param = new FormalParam(parseIdentifierNode(false));
        finish(param, m);
        params.add(param);
      } while (tq.checkToken(Punctuation.COMMA));
    }

    return new FormalParamList(params, mq);
  }

  private static Noop noop(FilePosition fp) {
    Noop n = new Noop(fp);
    n.setComments(Collections.<Token<?>>emptyList());
    return n;
  }

  /** The file position that spans from startMark to the current position. */
  private FilePosition posFrom(Mark startMark) throws ParseException {
    return posFrom(startMark.getFilePosition());
  }

  private FilePosition posFrom(ParseTreeNode childFlushWithStart)
      throws ParseException {
    return posFrom(childFlushWithStart.getFilePosition());
  }

  private FilePosition posFrom(FilePosition start) throws ParseException {
    return (tq.isEmpty() || tq.currentPosition() != start)
        ? FilePosition.span(start, tq.lastPosition())
        : FilePosition.startOf(start);
  }

  /**
   * Attaches to the parse tree filtered tokens,
   * such as type annotation carrying comments.
   */
  private void finish(AbstractParseTreeNode n, Mark startMark)
      throws ParseException {
    Mark endMark = tq.mark();
    tq.rewind(startMark);
    try {
      n.setComments(tq.filteredTokens());
    } finally {
      tq.rewind(endMark);
    }
  }

  private static class FormalParamList {
    public List<FormalParam> params;

    public FormalParamList(List<FormalParam> params, MessageQueue mq) {
      Set<String> paramNames = Sets.newHashSet();
      paramNames.add("arguments");
      paramNames.add("this");
      for (FormalParam p : params) {
        if (!paramNames.add(p.getIdentifierName())) {
         mq.addMessage(
             MessageType.DUPLICATE_FORMAL_PARAM,
             p.getFilePosition(),
             MessagePart.Factory.valueOf(p.getIdentifierName()));
        }
      }
      this.params = params;
    }
  }

  /**
   * Placeholder node for the actuals in a function call.  Never appears in the
   * final tree.
   */
  private static class ActualList extends AbstractExpression {
    private static final long serialVersionUID = 4826666277822756805L;

    ActualList(List<Expression> actuals) {
      super(FilePosition.UNKNOWN, Expression.class);
      createMutation().appendChildren(actuals).execute();
    }

    @Override
    public List<? extends Expression> children() {
      return childrenAs(Expression.class);
    }

    @Override
    public Object getValue() { return null; }

    public void render(RenderContext rc) {
      throw new UnsupportedOperationException();
    }

    public String typeOf() { return null; }
  }

  private static void issueLintWarningsForProblematicEscapes(
      Token<JsTokenType> t, MessageQueue mq) {
    String body = t.text.substring(1, t.text.length() - 1);
    for (int i = -1; (i = body.indexOf('\\', i + 1)) >= 0; ++i) {
      char next = body.charAt(i + 1);
      switch (next) {
        // control character escapes
        // \b is problematic since it has a different meaning in a regexp than
        // in a string literal.
        case 'b': case 'f': case 'n': case 'r': case 't':
        // numeric escape prefixes
        case 'u': case 'x': case 'X':
        case '0': case '1': case '2': case '3':
        case '4': case '5': case '6': case '7':
        // special characters that can appear in strings.
        // The / is used since it is often escaped to prevent close tags
        // in strings from appearing to end a script block.
        case '\\': case '/': case '\'': case '"':
          break;
        // specified in ES3 but not implemented consistently.
        case 'v':
          mq.addMessage(
              MessageType.AMBIGUOUS_ESCAPE_SEQUENCE,
              t.pos, MessagePart.Factory.valueOf("\\" + next));
          break;
        case 's': case 'w': case 'S': case 'W':
        case '+': case '?': case '*': case '.': case '-': case '|':
        case '^': case '$':
        case '[': case ']': case '(': case ')': case '{': case '}':
          mq.addMessage(
              MessageType.REDUNDANT_ESCAPE_SEQUENCE,
              t.pos, MessagePart.Factory.valueOf("\\" + next));
          break;
      }
    }
  }

  private static Operation checkInExprWithLhs(Statement s) {
    if (!(s instanceof ExpressionStmt)) { return null; }
    Expression e = ((ExpressionStmt) s).getExpression();
    if (!(e instanceof Operation)) { return null; }
    Operation op = (Operation) e;
    if (Operator.IN != op.getOperator()) { return null; }
    return op.children().get(0).isLeftHandSide() ? op : null;
  }
}
