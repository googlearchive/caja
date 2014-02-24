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

package com.google.caja.parser.js;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.RenderContext;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A node which can appear at the beginning of a program or function body to
 * place restrictions on code that follows.
 *
 * <p>The {@code DirectivePrologue} of a JavaScript {@code Program} or
 * {@code FunctionBody} is a sequence of zero or more
 * {@code ExpressionStatement}s each of which consists entirely of a
 * {@code StringLiteral}. For example:
 *
 * <pre>
 *   'use strict';
 *   <em>Remainder of the program...</em>
 * </pre>
 *
 * <p>or:
 *
 * <pre>
 *   function foo() {
 *     'use strict';
 *     <em>Remainder of the function body...</em>
 *   }
 * </pre>
 *
 * <p>For more details, see Section 14.1 of
 * <a href="http://wiki.ecmascript.org/lib/exe/fetch.php?id=es3.1%3Aes3.1_proposal_working_draft&cache=cache&media=es3.1:es5-tc392008-040.pdf">the ES5 spec</a>.
 *
 * @author mikesamuel@gmail.com
 */
public final class DirectivePrologue extends AbstractStatement {
  private static final long serialVersionUID = 2485949503702983868L;

  /** @param value unused.  This ctor is provided for reflection. */
  @ReflectiveCtor
  public DirectivePrologue(
      FilePosition pos, Void value, List<? extends Directive> children) {
    this(pos, children);
  }

  public DirectivePrologue(
      FilePosition pos, List<? extends Directive> children) {
    super(pos, Directive.class);
    createMutation().appendChildren(children).execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (children().isEmpty()) { throw new IndexOutOfBoundsException(); }
    for (ParseTreeNode us : children()) {
      if (!(us instanceof Directive)) {
        throw new ClassCastException(us.getClass().getName());
      }
    }
  }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<? extends Directive> children() {
    return childrenAs(Directive.class);
  }

  public boolean hasDirective(String directive) {
    return getDirectives().contains(directive);
  }

  public Set<String> getDirectives() {
    Set<String> directives = new LinkedHashSet<String>();
    for (Directive us : children()) {
      directives.add(us.getDirectiveString());
    }
    return Collections.unmodifiableSet(directives);
  }

  public void render(RenderContext rc) {
    for (Directive d : children()) { d.render(rc); }
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  public boolean hasHangingConditional() { return false; }
}
