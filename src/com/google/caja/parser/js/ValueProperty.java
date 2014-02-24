package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.reporting.RenderContext;
import java.util.List;

/**
 * The most common type of object property, a name and a value expression.
 *
 * @author mikesamuel@gmail.com
 */
public class ValueProperty extends ObjProperty {
  private static final long serialVersionUID = -8361603805798322752L;

  public ValueProperty(StringLiteral name, Expression value) {
    super(name, value);
  }

  public ValueProperty(FilePosition pos, StringLiteral name, Expression value) {
    super(pos, name, value);
  }

  /**
   * Provided for reflection.
   * @param value unused
   */
  @ReflectiveCtor
  public ValueProperty(
      FilePosition pos, Void value, List<? extends Expression> children) {
    super(pos, (StringLiteral) children.get(0), children.get(1));
  }

  public Expression getValueExpr() { return children().get(1); }

  public void render(RenderContext rc) {
    Expression value = children().get(1);
    TokenConsumer out = rc.getOut();
    renderPropertyName(rc, false);
    out.consume(":");
    if (!Operation.is(value, Operator.COMMA)) {
      value.render(rc);
    } else {
      out.mark(value.getFilePosition());
      out.consume("(");
      value.render(rc);
      out.consume(")");
    }
  }
}
