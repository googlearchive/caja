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

package com.google.caja.plugin.templates;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.js.ArrayConstructor;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.FunctionConstructor;
import com.google.caja.parser.js.Literal;
import com.google.caja.parser.js.NumberLiteral;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.RenderContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Given a series of javascript expressions, produces an expression that is the
 * result of appending the result of coercing each to a string.
 *
 * @author mikesamuel@gmail.com
 */
public final class JsConcatenator {
  private final List<Part> parts = new ArrayList<Part>();

  public void append(FilePosition pos, String s) {
    append(StringLiteral.valueOf(pos, s));
  }

  public void append(Expression e) {
    StringLiteral sl = Emitter.asStringLiteral(e);
    if (sl != null && sl.getUnquotedValue().length() == 0) { return; }
    if (Emitter.is(e, Operator.ADDITION)) {
      List<? extends Expression> operands = ((Operation) e).children();
      if (Emitter.isStringy(operands.get(0), false)
          || Emitter.isStringy(operands.get(1), false)) {
        append(operands.get(0));
        append(operands.get(1));
        return;
      }
    }
    parts.add(new StringPart(sl != null ? sl : e));
  }

  public void forSideEffect(Expression e) {
    e = e.simplifyForSideEffect();
    if (e != null) {
      parts.add(new SideEffectPart(e, true));
    }
  }

  /**
   * Returns an expression describing the result of concatenating the result of
   * each expression passed to {@link #append}.
   *
   * @param mustBeString true if the typeof result must be {@code string}.
   *      Otherwise, we guarantee that the result is an expression whose result,
   *      when coerced to a string, would be the same as the result of the
   *      expression produced by this method when this parameter is true.
   * @return an expression that evaluates to the concatenation of the string
   *      form of the results of evaluating the expressions passed to
   *      {@link #append} in order.
   */
  public Expression toExpression(boolean mustBeString) {
    return new Emitter(new ArrayList<Part>(parts)).toExpression(mustBeString);
  }
}

abstract class Part { /* marker base class */ }
final class StringPart extends Part {
  final Expression e;
  StringPart(Expression e) { this.e = e; }
}
final class SideEffectPart extends Part {
  final Expression e;
  final boolean canReorder;
  SideEffectPart(Expression e, boolean canReorder) {
    this.e = e;
    this.canReorder = canReorder;
  }
}

final class Emitter {
  final List<Part> parts;
  Emitter(List<Part> parts) { this.parts = parts; }

  Expression toExpression(boolean mustBeString) {
    moveSideEffectsBack();
    foldAdjacentStringLiterals();
    foldSideEffectsIntoStringPart(true);
    checkThatOneOperandIsStringLike(mustBeString);
    if (parts.isEmpty()) {
      return StringLiteral.valueOf(FilePosition.UNKNOWN, "");
    }
    Expression e = null;
    for (Part p : parts) {
      Expression pe;
      if (p instanceof StringPart) {
        pe = ((StringPart) p).e;
      } else if (p instanceof SideEffectPart) {
        SideEffectPart sep = (SideEffectPart) p;
        FilePosition epos = sep.e.getFilePosition();
        pe = Operation.createInfix(
            Operator.COMMA, sep.e, StringLiteral.valueOf(epos, ""));
      } else {
        throw new IllegalStateException();
      }
      if (e != null) {
        e = Operation.createInfix(Operator.ADDITION, e, pe);
      } else {
        e = pe;
      }
    }
    return e;
  }

  /**
   * Move side effects before runs of StringParts so that we can more
   * efficiently fold adjacent string literal portions.
   */
  private void moveSideEffectsBack() {
    // Move side effects before runs of string parts so that we can attach the
    // side effect to the string part.
    // This allows us to turn the naive sequence:
    //    o___.push('<a');
    //    foo();
    //    o___.push(' href="');
    // which has 5 lookups, and 3 method calls to
    //    o___.push((foo(), '<a href="'));
    // which has 3 lookups and 2 method calls and is semantically equivalent
    // assuming foo cannot reference o___.
    List<Part> newParts = new ArrayList<Part>();
    List<StringPart> stringParts = new ArrayList<StringPart>();
    for (Part p : parts) {
      if (p instanceof StringPart) {
        if (((StringPart) p).e instanceof Literal) {
          // We can move Literals past ones that have side effect, since their
          // meaning is independent of position, and they have no side effect.
          stringParts.add((StringPart) p);
          continue;
        }
      } else if (p instanceof SideEffectPart) {
        if (((SideEffectPart) p).canReorder) {
          newParts.add(p);
          continue;
        }
      }
      newParts.addAll(stringParts);
      stringParts.clear();
      newParts.add(p);
    }
    newParts.addAll(stringParts);
    stringParts = null;

    parts.clear();

    // Combine runs of side effects into a single comma operation.
    SideEffectPart lastSep = null;
    for (Part p : newParts) {
      if (p instanceof SideEffectPart) {
        SideEffectPart sep = (SideEffectPart) p;
        lastSep = lastSep == null
            ? sep
            // Left associative combination
            : new SideEffectPart(
                Operation.createInfix(Operator.COMMA, lastSep.e, sep.e),
                lastSep.canReorder && sep.canReorder);
      } else {
        if (lastSep != null) {
          parts.add(lastSep);
          lastSep = null;
        }
        parts.add(p);
      }
    }
    if (lastSep != null) { parts.add(lastSep); }
  }

  private void foldAdjacentStringLiterals() {
    for (int i = parts.size(); --i >= 0;) {
      Part p = parts.get(i);
      if (!(p instanceof StringPart)) { continue; }
      StringPart sp = (StringPart) p;
      if (!(sp.e instanceof StringLiteral)) { continue; }
      int j = i;
      while (j > 0 && isStringLiteralPart(parts.get(j - 1))) { --j; }
      if (i != j) {
        List<Part> toFold = parts.subList(j, i + 1);
        i = j;
        StringBuilder sb = new StringBuilder();
        for (Part tf : toFold) {
          sb.append(((StringLiteral) ((StringPart) tf).e).getUnquotedValue());
        }
        StringPart first = (StringPart) toFold.get(0);
        StringPart last = (StringPart) toFold.get(toFold.size() - 1);
        FilePosition pos = FilePosition.span(
            first.e.getFilePosition(), last.e.getFilePosition());
        toFold.clear();
        toFold.add(new StringPart(StringLiteral.valueOf(pos, sb.toString())));
      }
    }
  }

  private static boolean isStringLiteralPart(Part p) {
    return p instanceof StringPart
        && ((StringPart) p).e instanceof StringLiteral;
  }

  private void foldSideEffectsIntoStringPart(boolean alwaysFold) {
    for (int i = parts.size(); --i >= 0;) {
      Part p = parts.get(i);
      if (p instanceof SideEffectPart) {
        SideEffectPart sep = (SideEffectPart) p;
        Part np = i + 1 < parts.size() ? parts.get(i + 1) : null;
        Part pp = i > 0 ? parts.get(i - 1) : null;
        if (np instanceof StringPart
            && (alwaysFold || pp instanceof StringPart)) {
          Expression se = ((StringPart) np).e;
          StringPart combined = new StringPart(Operation.create(
              FilePosition.UNKNOWN, Operator.COMMA, sep.e, se));
          List<Part> both = parts.subList(i, i + 2);
          both.clear();
          both.add(combined);
        } else if (alwaysFold) {
          FilePosition pos = sep.e.getFilePosition();
          parts.set(i, new StringPart(Operation.create(
              pos, Operator.COMMA, sep.e,
              new StringLiteral(FilePosition.endOf(pos), ""))));
        }
      }
    }
  }

  private void checkThatOneOperandIsStringLike(boolean mustBeString) {
    switch (parts.size()) {
      case 0: return;
      case 1:  // We need to introduce a + operator unless we can prove that the
               // single argument is a string.
        if (!mustBeString || isStringy(parts.get(0), true)) { return; }
        break;
      default:
        // We need to make sure that one of the first two results produces a
        // stringy result so that addition operators added between exprs act
        // as string concatenation instead of numeric addition.
        if (isStringy(parts.get(0), false) || isStringy(parts.get(1), false)) {
          return;
        }
        break;
    }
    Expression e = ((StringPart) parts.get(0)).e;
    parts.set(0, new StringPart(makeStringy(e, parts.size() == 1)));
  }

  static boolean isStringy(Part p, boolean strict) {
    return p instanceof StringPart && isStringy(((StringPart) p).e, strict);
  }

  static boolean isStringy(Expression e, boolean strict) {
    if (e instanceof StringLiteral) { return true; }
    if (e instanceof Operation) {
      Operation op = (Operation) e;
      List<? extends Expression> operands = op.children();
      switch (op.getOperator()) {
        case ADDITION:
          return isStringy(operands.get(0), false)
              || isStringy(operands.get(1), false);
        case TERNARY:
        case LOGICAL_AND:
        case LOGICAL_OR:
          return isStringy(operands.get(operands.size() - 2), strict)
              && isStringy(operands.get(operands.size() - 1), strict);
        case COMMA:
          return isStringy(operands.get(operands.size() - 1), strict);
        case CONSTRUCTOR:
          return !strict;
        default:
          return false;
      }
    }
    if (e instanceof ArrayConstructor
        || e instanceof FunctionConstructor
        || e instanceof ObjectConstructor) {
      return !strict;
    }
    return false;
  }

  static Expression makeStringy(Expression e, boolean strict) {
    if (isStringy(e, strict)) { return e; }

    Expression stringier = null;
    if (e instanceof Operation) {
      Operation op = (Operation) e;
      List<? extends Expression> operands = op.children();
      switch (op.getOperator()) {
        // Change arguments to ||,&& would change semantics.
        case TERNARY:
          stringier = Operation.create(
              e.getFilePosition(), Operator.TERNARY, operands.get(0),
              makeStringy(operands.get(1), strict),
              makeStringy(operands.get(2), strict));
          break;
        case COMMA:
          stringier = Operation.create(
              e.getFilePosition(), Operator.COMMA, operands.get(0),
              makeStringy(operands.get(1), strict));
          break;
        default: break;
      }
    }
    if (stringier == null) {
      stringier = Operation.createInfix(
          Operator.ADDITION,
          StringLiteral.valueOf(FilePosition.startOf(e.getFilePosition()), ""),
          e);
    }
    assert isStringy(stringier, strict);
    return stringier;
  }

  static boolean is(Expression e, Operator op) {
    return e instanceof Operation && op == ((Operation) e).getOperator();
  }

  static boolean isStringConcat(Expression e) {
    if (!is(e, Operator.ADDITION)) { return false; }
    Operation op = (Operation) e;
    return isStringy(op.children().get(0), false)
        || isStringy(op.children().get(1), false);
  }

  static String asString(Literal l) {
    if (l instanceof NumberLiteral) {
      return NumberLiteral.numberToString(((NumberLiteral) l).doubleValue());
    } else {
      StringBuilder sb = new StringBuilder();
      JsMinimalPrinter p = new JsMinimalPrinter(sb);
      l.render(new RenderContext(p));
      p.noMoreTokens();
      return sb.toString();
    }
  }

  static StringLiteral asStringLiteral(Expression e) {
    if (e instanceof Literal) {
      if (e instanceof StringLiteral) { return (StringLiteral) e; }
      return StringLiteral.valueOf(e.getFilePosition(), asString((Literal) e));
    } else if (is(e, Operator.NEGATION)) {
      Expression child = ((Operation) e).children().get(0);
      if (child instanceof NumberLiteral) {
        return StringLiteral.valueOf(
            e.getFilePosition(), "-" + asString((Literal) child));
      }
    }
    return null;
  }
}
