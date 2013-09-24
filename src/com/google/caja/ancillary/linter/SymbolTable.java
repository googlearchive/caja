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

package com.google.caja.ancillary.linter;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.js.Declaration;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * A mapping from symbol names to declarations.
 *
 * @author mikesamuel@gmail.com
 */
final class SymbolTable {
  private final Map<String, Symbol> symbols = Maps.newLinkedHashMap();

  Symbol getSymbol(String symbolName) {
    return symbols.get(symbolName);
  }

  Collection<String> symbolNames() {
    return Collections.unmodifiableSet(symbols.keySet());
  }

  void declare(AncestorChain<Declaration> decl) {
    declare(decl.node.getIdentifierName(), decl);
  }

  void declare(String symbolName, AncestorChain<?> decl) {
    Symbol s = symbols.get(symbolName);
    if (s == null) {
      s = new Symbol();
      symbols.put(symbolName, s);
    }
    s.decls.add(decl);
  }

  final static class Symbol {
    private final Collection<AncestorChain<?>> decls = Lists.newArrayList();

    Collection<AncestorChain<?>> getDeclarations() {
      return Collections.unmodifiableCollection(decls);
    }
  }
}
