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

package com.google.caja.parser;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulation of the ordered collection of child nodes of a
 * {@link com.google.caja.parser.ParseTreeNode}.
 *
 * @author ihab.awad@gmail.com
 */
final class ChildNodes<T extends ParseTreeNode> implements Serializable {
  private static final long serialVersionUID = -3349416361229204091L;

  private boolean immutable = false;

  public boolean makeImmutable() {
    boolean wasMadeImmutable = true;
    if (backingList != null) {
      for (ParseTreeNode n : backingList) {
        wasMadeImmutable = wasMadeImmutable && n.makeImmutable();
      }
    }
    return immutable = wasMadeImmutable;
  }

  /**
   * The actual storage of collection elements. Constructed lazily in case it
   * is never used.
   */
  private List<T> backingList;

  /**
   * The class of the collection elements. Used to implement runtime
   * type safety of the collection.
   */
  private final Class<? extends T> elementClass;

  /**
   * The facet of this collection supporting mutations to the collection.
   */
  private List<T> mutableFacet;

  /**
   * The facet of this collection through which mutations are not allowed.
   */
  private List<T> immutableFacet;

  /**
   * The implementation of the mutable facet, which checks that newly added
   * elements are of type (erasure(T)) which is correct as long as
   * ParseTreeNodes are not parameterized.
   *
   * <p>We could have made this an anonymous class assigned to 'mutableFacet',
   * but implementing it as a named class allows us to instantiate it lazily
   * (see getMutableFacet()).
   */
  private class MutableFacet extends AbstractList<T> implements Serializable {
    private static final long serialVersionUID = 3989291162782482786L;

    @Override
    public int size() { return getBackingList().size(); }

    @Override
    public T get(int i) { return getBackingList().get(i); }

    @Override
    public T set(int i, T element) {
      return getBackingList().set(i, elementClass.cast(element));
    }

    @Override
    public void add(int i, T element) {
      getBackingList().add(i, elementClass.cast(element));
    }

    @Override
    public T remove(int i) { return getBackingList().remove(i); }
  }

  /**
   * Creates a new ChildNodes.
   *
   * @param elementClass the class of elements that will be added to the
   * collection.
   */
  public ChildNodes(Class<? extends T> elementClass) {
    assert elementClass.getTypeParameters().length == 0;
    this.elementClass = elementClass;
  }

  /**
   * Creates a clone of an existing ChildNodes.
   *
   * @param source a ChildNodes object to copy.
   */
  public ChildNodes(ChildNodes<? extends T> source) {
    this.backingList = new ArrayList<T>(source.backingList);
    this.elementClass = source.elementClass;
  }

  /**
   * @return the class of elements that this ChildNodes can contain.
   */
  public Class<? extends T> getElementClass() { return elementClass; }

  /**
   * @return a List interface to this collection that supports mutations. This
   * interface checks any insertions at runtime to ensure they are instances
   * of {@link #getElementClass()}.
   */
  public List<T> getMutableFacet() {
    if (immutable) {
      throw new UnsupportedOperationException();
    }
    if (mutableFacet == null) {
      mutableFacet = new MutableFacet();
    }
    return mutableFacet;
  }

  /**
   * @return a List interface to this collection that does not support
   * mutations.
   */
  public List<T> getImmutableFacet() {
    if (immutableFacet == null) {
      immutableFacet = Collections.unmodifiableList(getBackingList());
    }
    return immutableFacet;
  }

  /**
   * Statically cast this ChildNodes object to represent a collection
   * containing a subtype of the original. This only succeeds if the dynamic
   * type of this ChildNodes is such that the cast is safe.
   *
   * @param subClass the desired class of the elements of the result.
   * @param <SubT> the desired class of the elements of the result.
   * @return a narrowed reference to this ChildNodes.
   */
  @SuppressWarnings("unchecked")
  public <SubT extends T> ChildNodes<? extends SubT> as(Class<SubT> subClass) {
    elementClass.asSubclass(subClass);
    return (ChildNodes<SubT>) this;
  }

  // Accessor for 'backingList' to implement lazy construction.
  private List<T> getBackingList() {
    if (backingList == null) {
      backingList = new ArrayList<T>();
    }
    return backingList;
  }
}