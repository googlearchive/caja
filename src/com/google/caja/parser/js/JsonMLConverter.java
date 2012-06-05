// Copyright (C) 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      case http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.Keyword;
import com.google.caja.lexer.SourceBreaks;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodes;
import com.google.caja.parser.ParserBase;
import com.google.caja.util.Lists;
import com.google.caja.util.Maps;
import com.google.javascript.jscomp.jsonml.JsonML;
import com.google.javascript.jscomp.jsonml.TagAttr;
import com.google.javascript.jscomp.jsonml.TagType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Translates JsonML into a Caja parse tree.
 *
 * @author mikesamuel@gmail.com
 */
public final class JsonMLConverter {
  private static final Expression[] NO_EXPRS = new Expression[0];

  private final Map<String, SourceBreaks> breaksPerFile;

  public JsonMLConverter(Map<String, SourceBreaks> breaksPerFile) {
    this.breaksPerFile = Maps.newHashMap(breaksPerFile);
  }

  public ParseTreeNode toNode(JsonML jsonML) {
    return toNode(jsonML, ParseTreeNode.class);
  }

  public <T extends ParseTreeNode> T toNode(JsonML jsonML, Class<T> clazz) {
    ParseTreeNode node = toParseTreeNode(jsonML);
    if (Statement.class == clazz && node instanceof Expression) {
      node = new ExpressionStmt((Expression) node);
    }
    return clazz.cast(node);
  }

  private ParseTreeNode toParseTreeNode(JsonML jsonML) {
    FilePosition pos = positionFrom(jsonML);
    List<? extends JsonML> children = jsonML.getChildren();
    switch (jsonML.getType()) {
      case ArrayExpr: {
        List<Expression> elements = Lists.newArrayList(children.size());
        for (JsonML child : children) {
          if (child.getType() == TagType.Empty) {
            elements.add(new Elision(pos));
          } else {
            elements.add(toNode(child, Expression.class));
          }
        }
        return new ArrayConstructor(pos, elements);
      }
      case ObjectExpr:
        return new ObjectConstructor(pos, toNodes(children, ObjProperty.class));
      case AssignExpr: case BinaryExpr: case CountExpr: case MemberExpr:
      case UnaryExpr: {
        String symbol = (String) jsonML.getAttribute(TagAttr.OP);
        OperatorType opType;
        if ("()".equals(symbol) || "[]".equals(symbol)) {
          opType = OperatorType.BRACKET;
          symbol = symbol.substring(0, 1);
        } else {
          switch (children.size()) {
            case 1:
              opType = Boolean.FALSE.equals(
                  jsonML.getAttribute(TagAttr.IS_PREFIX))
                  ? OperatorType.POSTFIX : OperatorType.PREFIX;
              break;
            case 2: opType = OperatorType.INFIX; break;
            case 3: opType = OperatorType.TERNARY; break;
            default: throw new AssertionError();
          }
        }
        Operator op = Operator.lookupOperation(symbol, opType);
        Expression[] operands = toNodes(children, Expression.class)
            .toArray(NO_EXPRS);
        if (op == Operator.MEMBER_ACCESS) {
          String member = ((StringLiteral) operands[1]).getUnquotedValue();
          if (ParserBase.isJavascriptIdentifier(member)
              && !Keyword.isKeyword(member)) {
            operands[1] = new Reference(new Identifier(
                operands[1].getFilePosition(), member));
          } else {
            op = Operator.SQUARE_BRACKET;
          }
        }
        return Operation.create(pos, op, operands);
      }
      case CallExpr:
        return Operation.create(
            pos, Operator.FUNCTION_CALL,
            toNodes(children, Expression.class).toArray(NO_EXPRS));
      case ConditionalExpr:
        return Operation.create(
            pos, Operator.TERNARY,
            toNodes(children, Expression.class).toArray(NO_EXPRS));
      case DeleteExpr:
        return Operation.create(
            pos, Operator.DELETE,
            toNodes(children, Expression.class).toArray(NO_EXPRS));
      case EvalExpr: {
        Expression[] operands = new Expression[children.size() + 1];
        toNodes(children, Expression.class).toArray(operands);
        System.arraycopy(operands, 0, operands, 1, children.size());
        operands[0] = new Reference(
            new Identifier(FilePosition.startOf(pos), "eval"));
        return Operation.create(pos, Operator.FUNCTION_CALL, operands);
      }
      case InvokeExpr: {
        Expression obj = toNode(children.get(0), Expression.class);
        Expression key = toNode(children.get(1), Expression.class);
        Operator op = Operator.SQUARE_BRACKET;
        if (".".equals(jsonML.getAttribute(TagAttr.OP))) {
          String name = ((StringLiteral) key).getUnquotedValue();
          if (ParserBase.isJavascriptIdentifier(name)
              && !Keyword.isKeyword(name)) {
            key = new Reference(new Identifier(key.getFilePosition(), name));
            op = Operator.MEMBER_ACCESS;
          }
        }
        List<Expression> operands = Lists.newArrayList(children.size() - 1);
        operands.add(Operation.create(
            FilePosition.span(obj.getFilePosition(), key.getFilePosition()),
            op, obj, key));
        operands.addAll(toNodes(
            children.subList(2, children.size()), Expression.class));
        return Operation.create(
            pos, Operator.FUNCTION_CALL, operands.toArray(NO_EXPRS));
      }
      case LogicalAndExpr:
        return Operation.create(
            pos, Operator.LOGICAL_AND,
            toNodes(children, Expression.class).toArray(NO_EXPRS));
      case LogicalOrExpr:
        return Operation.create(
            pos, Operator.LOGICAL_OR,
            toNodes(children, Expression.class).toArray(NO_EXPRS));
      case NewExpr: {
        return Operation.create(
            pos, Operator.CONSTRUCTOR,
            toNodes(children, Expression.class).toArray(NO_EXPRS));
      }
      case TypeofExpr:
        return Operation.create(
            pos, Operator.TYPEOF,
            toNodes(children, Expression.class).toArray(NO_EXPRS));
      case LiteralExpr: {
        String type = (String) jsonML.getAttribute(TagAttr.TYPE);
        if ("string".equals(type)) {
          return new StringLiteral(
              pos,
              StringLiteral.toQuotedValue(
                  (String) jsonML.getAttribute(TagAttr.VALUE)));
        } else if ("boolean".equals(type)) {
          return new BooleanLiteral(
              pos, (Boolean) jsonML.getAttribute(TagAttr.VALUE));
        } else if ("number".equals(type)) {
          Number number = (Number) jsonML.getAttribute(TagAttr.VALUE);
          if (number instanceof Integer) {
            return new IntegerLiteral(pos, number.intValue());
          } else {
            return new RealLiteral(pos, number.doubleValue());
          }
        } else if ("null".equals(type)) {
          return new NullLiteral(pos);
        } else {
          throw new IllegalArgumentException(type);
        }
      }
      case RegExpExpr: {
        String flags = (String) jsonML.getAttribute(TagAttr.FLAGS);
        if (flags == null) { flags = ""; }
        String regex = "/" + jsonML.getAttribute(TagAttr.BODY) + "/" + flags;
        return new RegexpLiteral(pos, regex);
      }
      case ThisExpr:
        return new Reference(new Identifier(pos, "this"));
      case BlockStmt: case Program:
        return new Block(pos, toNodes(children, Statement.class));
      case BreakStmt: {
        String label = (String) jsonML.getAttribute(TagAttr.LABEL);
        if (label == null) { label = ""; }
        return new BreakStmt(pos, label);
      }
      case ContinueStmt: {
        String label = (String) jsonML.getAttribute(TagAttr.LABEL);
        if (label == null) { label = ""; }
        return new ContinueStmt(pos, label);
      }
      case DebuggerStmt:
        return new DebuggerStmt(pos);
      case DoWhileStmt:
        return new DoWhileLoop(
            pos, "", toNode(children.get(0), Statement.class),
            toNode(children.get(1), Expression.class));
      case EmptyStmt:
        return new Noop(pos);
      case ForInStmt:
        if (children.get(0).getType() == TagType.VarDecl) {
          return new ForEachLoop(
              pos, "", toNode(children.get(0), Declaration.class),
              toNode(children.get(1), Expression.class),
              toNode(children.get(2), Statement.class));
        } else {
          return new ForEachLoop(
              pos, "", toNode(children.get(0), Expression.class),
              toNode(children.get(1), Expression.class),
              toNode(children.get(2), Statement.class));
        }
      case ForStmt:
        return new ForLoop(
            pos, "",
            toNodeOrNoop(children.get(0)),
            toNodeOrTrue(children.get(1)),
            toNodeOrNoop(children.get(2)),
            toNode(children.get(3), Statement.class));
      case FunctionExpr:
        return toFunctionConstructor(jsonML);
      case IdExpr:
        return new Reference(
            new Identifier(pos, (String) jsonML.getAttribute(TagAttr.NAME)));
      case IfStmt: {
        Expression cond = toNode(children.get(0), Expression.class);
        Statement thenClause = toNode(children.get(1), Statement.class);
        Statement elseClause = children.get(2).getType() != TagType.EmptyStmt
            ? toNode(children.get(2), Statement.class) : null;
        List<ParseTreeNode> childNodes = Lists.newArrayList();
        childNodes.add(cond);
        childNodes.add(thenClause);
        if (elseClause != null) {
          if (elseClause instanceof Conditional) {
            childNodes.addAll(elseClause.children());
          } else {
            childNodes.add(elseClause);
          }
        }
        return new Conditional(pos, null, childNodes);
      }
      case LabelledStmt: {
        String label = (String) jsonML.getAttribute(TagAttr.LABEL);
        Statement body = toNode(children.get(0), Statement.class);
        if (body instanceof LabeledStatement
            && !(body instanceof LabeledStmtWrapper)) {
          return ParseTreeNodes.newNodeInstance(
              body.getClass(), body.getFilePosition(), label, body.children());
        } else {
          return new LabeledStmtWrapper(pos, label, body);
        }
      }
      case ReturnStmt:
        if (children.isEmpty()) {
          return new ReturnStmt(pos, null);
        } else {
          return new ReturnStmt(pos, toNode(children.get(0), Expression.class));
        }
      case SwitchStmt:
        return new SwitchStmt(
            pos, "", toNode(children.get(0), Expression.class),
            toNodes(children.subList(1, children.size()), SwitchCase.class));
      case ThrowStmt:
        return new ThrowStmt(pos, toNode(children.get(0), Expression.class));
      case TryStmt: {
        Block body = toNode(children.get(0), Block.class);
        CatchStmt catchStmt = null;
        FinallyStmt finallyStmt = null;
        int i = 1, n = children.size();
        if (children.get(i).getType() == TagType.CatchClause) {
          catchStmt = toNode(children.get(i++), CatchStmt.class);
        } else if (children.get(i).getType() == TagType.Empty) {
          ++i;
        }
        if (i < n) {
          Block block = toNode(children.get(i), Block.class);
          finallyStmt = new FinallyStmt(block.getFilePosition(), block);
        }
        return new TryStmt(pos, body, catchStmt, finallyStmt);
      }
      case WhileStmt:
        return new WhileLoop(
            pos, "", toNode(children.get(0), Expression.class),
            toNode(children.get(1), Statement.class));
      case WithStmt:
        return new WithStmt(
            pos, toNode(children.get(0), Expression.class),
            toNode(children.get(1), Statement.class));
      case FunctionDecl:
        return new FunctionDeclaration(toFunctionConstructor(jsonML));
      case ParamDecl:
        throw new IllegalStateException("Orphaned param");
      case PrologueDecl:
        throw new IllegalStateException("Orphaned prologue");
      case VarDecl: {
        List<Declaration> decls = Lists.newArrayList();
        for (JsonML child : children) {
          FilePosition childPos = positionFrom(child);
          if (child.getType() == TagType.InitPatt) {
            List<JsonML> initPattParts = child.getChildren();
            decls.add(new Declaration(
                childPos, toNode(initPattParts.get(0), Identifier.class),
                toNode(initPattParts.get(1), Expression.class)));
          } else {
            decls.add(new Declaration(
                childPos, toNode(child, Identifier.class), null));
          }
        }
        if (decls.size() == 1) {
          return decls.get(0);
        }
        return new MultiDeclaration(pos, decls);
      }
      case DataProp:
        return new ValueProperty(
            pos,
            StringLiteral.valueOf(
                FilePosition.span(pos, positionFrom(children.get(0))),
                (String) jsonML.getAttribute(TagAttr.NAME)),
            toNode(children.get(0), Expression.class));
      case GetterProp:
        return new GetterProperty(
            pos,
            StringLiteral.valueOf(
                FilePosition.span(pos, positionFrom(children.get(0))),
                (String) jsonML.getAttribute(TagAttr.NAME)),
            toFunctionConstructor(children.get(0)));
      case SetterProp:
        return new SetterProperty(
            pos,
            StringLiteral.valueOf(
                FilePosition.span(pos, positionFrom(children.get(0))),
                (String) jsonML.getAttribute(TagAttr.NAME)),
            toFunctionConstructor(children.get(0)));
      case IdPatt:
        return new Identifier(pos, (String) jsonML.getAttribute(TagAttr.NAME));
      case InitPatt:
        throw new IllegalStateException("Orphaned init patt");
      case Case: {
        Expression value = toNode(children.get(0), Expression.class);
        int n = children.size();
        List<Statement> body = toNodes(children.subList(1, n), Statement.class);
        FilePosition bodyPos = body.isEmpty()
            ? FilePosition.span(
                FilePosition.endOf(value.getFilePosition()), pos)
            : FilePosition.span(
                FilePosition.startOf(body.get(0).getFilePosition()), pos);
        return new CaseStmt(pos, value, new Block(bodyPos, body));
      }
      case DefaultCase: {
        List<Statement> body = toNodes(children, Statement.class);
        FilePosition bodyPos = body.isEmpty()
            ? pos
            : FilePosition.span(
                FilePosition.startOf(body.get(0).getFilePosition()), pos);
        return new DefaultCaseStmt(pos, new Block(bodyPos, body));
      }
      case CatchClause: {
        Identifier exName = toNode(children.get(0), Identifier.class);
        return new CatchStmt(
            pos, new Declaration(exName.getFilePosition(), exName, null),
            toNode(children.get(1), Block.class));
      }
      case Empty:
        throw new IllegalStateException("Orphaned empty");
    }
    return null;
  }

  private FilePosition positionFrom(JsonML jsonML) {
    SourceBreaks breaks = breaksPerFile.get(jsonML.getAttribute(TagAttr.SOURCE));
    if (breaks == null) { return FilePosition.UNKNOWN; }
    int pos = (Integer) jsonML.getAttribute(TagAttr.OPAQUE_POSITION);
    int start = pos >>> 16;
    return breaks.toFilePosition(start, (pos & 0xfff) + start);
  }

  private Expression toNodeOrTrue(JsonML jsonML) {
    if (jsonML.getType() == TagType.Empty) {
      return new BooleanLiteral(positionFrom(jsonML), true);
    } else {
      return toNode(jsonML, Expression.class);
    }
  }

  private Statement toNodeOrNoop(JsonML jsonML) {
    if (jsonML.getType() == TagType.Empty) {
      return new Noop(positionFrom(jsonML));
    } else {
      return toNode(jsonML, Statement.class);
    }
  }

  private <T extends ParseTreeNode> List<T> toNodes(
      List<? extends JsonML> jsonMLs, Class<T> clazz) {
    int n = jsonMLs.size();
    if (n == 0) { return Collections.emptyList(); }
    List<T> out = Lists.newArrayList(n);
    int i = 0;
    if (jsonMLs.get(0).getType() == TagType.PrologueDecl) {
      FilePosition start = null;
      FilePosition end;
      List<Directive> directives = Lists.newArrayList();
      do {
        JsonML decl = jsonMLs.get(i++);
        end = positionFrom(decl);
        if (start == null) { start = end; }
        directives.add(new Directive(
            end, (String) decl.getAttribute(TagAttr.DIRECTIVE)));
      } while (i < n && jsonMLs.get(i).getType() == TagType.PrologueDecl);
      DirectivePrologue directive = new DirectivePrologue(
          FilePosition.span(start, end), null, directives);
      out.add(clazz.cast(directive));
    }
    while (i < n) {
      out.add(toNode(jsonMLs.get(i++), clazz));
    }
    return out;
  }

  private FunctionConstructor toFunctionConstructor(JsonML jsonML) {
    FilePosition pos = positionFrom(jsonML);
    List<? extends JsonML> children = jsonML.getChildren();
    JsonML name = children.get(0);
    Identifier nameNode;
    if (name.getType() == TagType.Empty) {
      nameNode = new Identifier(positionFrom(name), null);
    } else {
      nameNode = toNode(name, Identifier.class);
    }
    List<FormalParam> params = Lists.newArrayList();
    for (JsonML param : children.get(1).getChildren()) {
      params.add(new FormalParam(toNode(param, Identifier.class)));
    }
    FilePosition blockEnd = FilePosition.endOf(pos);
    FilePosition blockStart = blockEnd;
    List<Statement> bodyParts = toNodes(
        children.subList(2, children.size()), Statement.class);
    if (!bodyParts.isEmpty()) {
      blockStart = bodyParts.get(0).getFilePosition();
    }
    Block body = new Block(FilePosition.span(blockStart, blockEnd), bodyParts);
    return new FunctionConstructor(pos, nameNode, params, body);
  }
}
