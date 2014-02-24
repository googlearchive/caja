// Copyright (C) 2010 Google Inc.
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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.reporting.RenderContext;
import java.util.List;

/**
 * A getter property per section 11.1.5
 * <code>PropertyAssignment : set PropertyName ( ) { FunctionBody }</code>
 *
 * @author mikesamuel@gmail.com
 */
public final class SetterProperty extends ObjProperty {
  private static final long serialVersionUID = 1472590418386289731L;

  public SetterProperty(StringLiteral name, FunctionConstructor fn) {
    super(name, fn);
  }

  public SetterProperty(
      FilePosition pos, StringLiteral name, FunctionConstructor fn) {
    super(pos, name, fn);
  }

  public FunctionConstructor getFunction() {
    return (FunctionConstructor) children().get(1);
  }

  /**
   * Provided for reflection.
   * @param value unused
   */
  @ReflectiveCtor
  public SetterProperty(
      FilePosition pos, Void value, List<? extends Expression> children) {
    super(
        pos, (StringLiteral) children.get(0),
        (FunctionConstructor) children.get(1));
  }

  @Override
  public void childrenChanged() {
    super.childrenChanged();
    FunctionConstructor fn = getFunction();
    // The property name is not bound to the setter function in the body of the
    // setter function.
    // From section 11.1.5:
    //     Let closure be the result of creating a new Function object as
    //     specified in 13.2 with parameters specified by
    //     PropertySetParameterList and body specified by FunctionBody.
    //     Pass in the LexicalEnvironment of the running execution context as
    //     the Scope. Pass in true as the Strict flag if the PropertyAssignment
    //     is contained in strict code or if its FunctionBody is strict code.
    // The property name is never exposed to the closure constructor.
    if (fn.getIdentifierName() != null) {
      throw new IllegalStateException();
    }
  }

  public void render(RenderContext r) {
    TokenConsumer out = r.getOut();
    out.mark(getFilePosition());
    out.consume("set");
    out.consume(" ");
    // ES5 allows quoted property names after get/set, but many ES3
    // implementations that implement a getter/setter abbreviated syntax
    // do not, and it introduces no ambiguity.
    renderPropertyName(r, true);
    FunctionConstructor fn = getFunction();
    fn.renderActuals(r);
    fn.renderBody(r);
  }
}
