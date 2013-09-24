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

import java.util.Collection;
import java.util.Set;

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.util.Multimap;
import com.google.caja.util.Multimaps;
import com.google.common.collect.Sets;

/**
 * Groups nodes in a parse tree by type.
 *
 * @author mikesamuel@gmail.com
 */
final class NodeBuckets {
  private final Multimap<Class<? extends ParseTreeNode>, AncestorChain<?>>
      buckets = Multimaps.newListHashMultimap();

  /**
   * @param ac the root of the tree to bucket.
   * @param classes the set of nodes for which {@link #get} will return
   *     non-null.
   */
  private NodeBuckets load(
      final AncestorChain<?> ac,
      final Iterable<Class<? extends ParseTreeNode>> classes) {
    for (Class<? extends ParseTreeNode> cl : classes) {
      if (cl.isInstance(ac.node)) {
        buckets.put(cl, ac);
      }
    }
    for (ParseTreeNode child : ac.node.children()) {
      load(AncestorChain.instance(ac, child), classes);
    }
    return this;
  }

  @SuppressWarnings("unchecked")
  public <T extends ParseTreeNode>
  Collection<AncestorChain<T>> get(Class<T> cl) {
    Collection<AncestorChain<?>> nodes = buckets.get(cl);
    Collection<? extends AncestorChain<?>> wider = nodes;
    // This is type-safe because the constructor only adds chains of type cl to
    // the list corresponding to cl.
    return (Collection<AncestorChain<T>>) wider;
  }

  static class Maker {
    private final Set<Class<? extends ParseTreeNode>> types = Sets.newLinkedHashSet();
    private Maker() { /* no public zero-argument ctor */ }
    Maker with(Class<? extends ParseTreeNode> cl) {
      types.add(cl);
      return this;
    }
    NodeBuckets under(AncestorChain<?> ac) {
      return new NodeBuckets().load(ac, types);
    }
  }

  static Maker maker() { return new Maker(); }
}
