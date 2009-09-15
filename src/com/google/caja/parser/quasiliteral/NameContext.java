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

import com.google.caja.CajaException;
import com.google.caja.lexer.FilePosition;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.caja.util.Maps;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * A mapping of identifiers which ensures that no use of an identifier in a
 * scope masks a use of an identifier in an outer scope.
 * <p>
 * Javascript is not a block scoped language, but IHTML constructs are block
 * scoped, so we alpha-rename all variables to prevent collisions.
 *
 * @param <NAME> a type that can work as a hashtable key
 * @param <BINDING> type of data associated with a particular name.
 *    This can be any object related to the result of resolving the name.
 *
 * @author mikesamuel@gmail.com
 */
public final class NameContext<NAME, BINDING> {
  private final NameContext<NAME, BINDING> parent;
  private final Iterator<String> nameGenerator;
  /** maps names in original source to details about the renamed instance. */
  private final Map<NAME, VarInfo<NAME, BINDING>> vars
      = Maps.newLinkedHashMap();

  public static final class VarInfo<NAME, BINDING> {
    public final NAME origName;
    public final String newName;
    public final FilePosition declaredAt;
    private BINDING binding;

    private VarInfo(NAME origName, String newName, FilePosition declaredAt) {
      assert origName != null;
      this.origName = origName;
      this.newName = newName;
      this.declaredAt = declaredAt;
    }

    public BINDING getBinding() { return binding; }
    public void bind(BINDING binding) { this.binding = binding; }

    @Override
    public String toString() {
      return "(" + getClass().getSimpleName() + " " + origName + ")";
    }
  }

  /**
   * @param nameGenerator an infinite iterator that returns safe identifiers
   *    and that never returns the same String twice.
   *    Typically, a {@link com.google.caja.util.SafeIdentifierMaker}.
   */
  public NameContext(Iterator<String> nameGenerator) {
    this(null, nameGenerator);
  }

  private NameContext(
      NameContext<NAME, BINDING> parent, Iterator<String> nameGenerator) {
    this.parent = parent;
    this.nameGenerator = nameGenerator;
  }

  public NameContext<NAME, BINDING> makeChildContext() {
    return new NameContext<NAME, BINDING>(this, this.nameGenerator);
  }

  public NameContext<NAME, BINDING> getParentContext() { return parent; }

  public VarInfo<NAME, BINDING> declare(NAME origName, FilePosition declSite)
      throws RedeclarationException {
    VarInfo<NAME, BINDING> d = vars.get(origName);
    if (d == null) {
      String newName = nameGenerator.next();
      VarInfo<NAME, BINDING> vi = new VarInfo<NAME, BINDING>(
          origName, newName, declSite);
      vars.put(origName, vi);
      return vi;
    } else {
      FilePosition dPos = d.declaredAt;
      throw new RedeclarationException(new Message(
          RewriterMessageType.CANNOT_REDECLARE_VAR, declSite,
          MessagePart.Factory.valueOf(origName.toString()), dPos));
    }
  }

  public VarInfo<NAME, BINDING> lookup(NAME originalName) {
    for (NameContext<NAME, BINDING> c = this; c != null; c = c.parent) {
      VarInfo<NAME, BINDING> vi = c.vars.get(originalName);
      if (vi != null) { return vi; }
    }
    return null;
  }

  public Iterable<VarInfo<NAME, BINDING>> vars() {
    return Collections.unmodifiableMap(vars).values();
  }

  public Iterator<String> getNameGenerator() { return nameGenerator; }

  public static class RedeclarationException extends CajaException {
    public RedeclarationException(Message m, Throwable th) { super(m, th); }
    public RedeclarationException(Message m) { this(m, null); }
  }
}
