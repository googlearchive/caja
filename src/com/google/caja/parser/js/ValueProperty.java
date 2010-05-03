package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.parser.ParserBase;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsPrettyPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Callback;

import java.io.IOException;

import java.util.List;

/**
 * The most common type of object property, a name and a value expression.
 *
 * @author mikesamuel@gmail.com
 */
public class ValueProperty extends ObjProperty {

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

  @Override
  public Object getValue() { return null; }

  public Expression getValueExpr() { return children().get(1); }

  @Override
  public TokenConsumer makeRenderer(
      Appendable out, Callback<IOException> handler) {
    return new JsPrettyPrinter(new Concatenator(out, handler));
  }

  @Override
  public void render(RenderContext rc) {
    StringLiteral key = (StringLiteral) children().get(0);
    Expression value = children().get(1);
    TokenConsumer out = rc.getOut();

    String uqVal;
    if (rc.rawObjKeys()
        && ParserBase.isJavascriptIdentifier(
            uqVal = key.getUnquotedValue())
        && !("get".equals(uqVal) || "set".equals(uqVal))) {
      out.consume(uqVal);
    } else {
      key.render(rc);
    }
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
