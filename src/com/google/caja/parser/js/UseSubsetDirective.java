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

import com.google.caja.lexer.escaping.Escaping;
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
 * <p>
 * "Use subsets" are defined in ES3.1.
 * An <a href="http://wiki.ecmascript.org/lib/exe/fetch.php?id=es3.1%3Aes3.1_proposal_working_draft&amp;cache=cache&amp;media=es3.1:tc39-es31-draft27oct08.pdf"
 * >ES3.1 draft</a> defines the use grammar as:<blockquote><pre>
 * UseSubsetDirective opt :
 *     " use SubsetList(opt) " ;
 * SubsetList :
 *     Subset
 *     SubsetList, Subset
 * Subset : one of
 *     strict
 * </pre></blockquote>
 * and later
 * <blockquote>
 * The production UseSubsetDirectiveopt : " use SubsetList " ; is evaluated
 * as follows:<ol>
 *   <li>Evaluate SubsetList
 *   <li>Return Result(1)
 * </ol>
 * TODO(mikesamuel): check after Kona meeting
 *
 * <p>
 * The production SubsetList : Subset is evaluated as follows:<ol>
 *   <li>If Subset is not the name of a usage subset that is supported by this
 *     ECMAScript implementation, return an empty internal list.
 *   <li>Return an internal list containing one element which is the Subset.
 * </ol>
 *
 * The production SubsetList : SubsetList , Subset is evaluated as follows:
 * <ol>
 *   <li>Evaluate SubsetList.
 *   <li>If Subset is not the name of a usage subset that is supported by this
 *     ECMAScript implementation, return Result(1)
 *   <li>If Subset is already an element of Result(1), return Result(1)
 *   <li>Return an internal list whose length is one greater than the length
 *     of Result(1) and whose items are the items of Result(1), in order,
 *     followed at the end by Subset, which is the last item of the new list.
 * </ol>
 * </blockquote>
 *
 * <p>The spec will change to relax the Subset definition and to clear up the
 * verbiage around what should be considered a {@code UseSubsetDirective},
 * but the idea of having programs carry claims that they fit within fail-stop
 * named subsets of the language seems to have been agreed to.
 *
 * @author mikesamuel@gmail.com
 */
public final class UseSubsetDirective extends AbstractStatement {
  /** @param value unused.  This ctor is provided for reflection. */
  public UseSubsetDirective(Void value, List<? extends UseSubset> children) {
    this(children);
  }

  public UseSubsetDirective(List<? extends UseSubset> children) {
    super(UseSubset.class);
    createMutation().appendChildren(children).execute();
  }

  @Override
  protected void childrenChanged() {
    super.childrenChanged();
    if (children().isEmpty()) { throw new IndexOutOfBoundsException(); }
    for (ParseTreeNode us : children()) {
      if (!(us instanceof UseSubset)) {
        throw new ClassCastException(us.getClass().getName());
      }
    }
  }

  @Override
  public Object getValue() { return null; }

  @Override
  public List<UseSubset> children() {
    return childrenAs(UseSubset.class);
  }

  public Set<String> getSubsetNames() {
    Set<String> names = new LinkedHashSet<String>();
    for (UseSubset us : children()) {
      names.add(us.getSubsetName());
    }
    return Collections.unmodifiableSet(names);
  }

  public void render(RenderContext rc) {
    StringBuilder sb = new StringBuilder();
    sb.append("use");
    String sep = " ";
    for (UseSubset us : children()) {
      sb.append(sep).append(us.getValue());
      sep = ",";
    }
    StringBuilder escaped = new StringBuilder();
    escaped.append('\'');
    Escaping.escapeJsString(sb, true, true, escaped);
    escaped.append('\'');
    rc.getOut().consume(escaped.toString());
    rc.getOut().consume(";");
  }

  @Override
  public boolean isTerminal() {
    return true;
  }
}
