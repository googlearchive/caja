// Copyright (C) 2005-2006 Google Inc.
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

import com.google.caja.parser.quasiliteral.ParseTreeNodeContainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A utility class for common operations on {@link ParseTreeNode}s.
 *
 * @author ihab.awad@gmail.com
 */
public class ParseTreeNodes {

  /**
   * Construct a new {@code ParseTreeNode} via reflection assuming the existence
   * of a constructor having the following signature:
   *
   * <pre>ctor(T value, List&lt;? extends ParseTreeNode&gt; children)</pre>
   *
   * where {@code T} is the type of the values for the specific node class.
   *
   * @param clazz the concrete class of {@code ParseTreeNode} to instantiate.
   * @param value the value for the new node
   *        (see {@link com.google.caja.parser.ParseTreeNode#getValue()}).
   * @param children the children of the new node.  The constructor recursively
   *        traverses the children, replacing all ParseTreeNodeContainers with
   *        their children.  This flattens containers in containers.
   *        (see {@link com.google.caja.parser.ParseTreeNode#children()})).
   * @return the newly constructed {@code ParseTreeNode}.
   */
  public static <T extends ParseTreeNode> T newNodeInstance(
      Class<T> clazz, Object value, List<? extends ParseTreeNode> children) {
    children = flattenNodeList(children);
    Constructor<T> ctor = findCloneCtor(clazz);
    try {
      return ctor.newInstance(value, children);
    } catch (InstantiationException e) {
      throw new RuntimeException(getCtorErrorMessage(ctor, value, children), e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(getCtorErrorMessage(ctor, value, children), e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(getCtorErrorMessage(ctor, value, children), e);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(getCtorErrorMessage(ctor, value, children), e);
    }
  }

  // TODO(ihab): Instead of creating a new list each time, pass the list in and
  // append to it.
  // TODO(mikesamuel): clean up dependency.  This package should not depend on
  // quasiliterals.
  private static List<? extends ParseTreeNode> flattenNodeList(
      List<? extends ParseTreeNode> nodes) {
    List<ParseTreeNode> results = new ArrayList<ParseTreeNode>();
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i) instanceof ParseTreeNodeContainer) {
        results.addAll(flattenNodeList(nodes.get(i).children()));
      } else {
        results.add(nodes.get(i));
      }
    }
    return results;
  }

  /**
   * Perform a deep equality test on a pair of {@code ParseTreeNode}s.
   *
   * @param x a {@code ParseTreeNode}.
   * @param y another {@code ParseTreeNode}.
   * @return whether the trees rooted at {@code this} and {@code n} are equal.
   */
  public static boolean deepEquals(ParseTreeNode x, ParseTreeNode y) {
    if (x.getClass() == y.getClass()) {
      if ((x.getValue() == null && y.getValue() == null) ||
          (x.getValue() != null && x.getValue().equals(y.getValue()))) {
        if (x.children().size() == y.children().size()) {
          for (int i = 0; i < x.children().size(); i++) {
            if (!deepEquals(x.children().get(i), y.children().get(i)))
              return false;
          }
          return true;
        }
      }
    }
    return false;
  }

  private static <T extends ParseTreeNode> Constructor<T> findCloneCtor(
      Class<T> clazz) {
    for (Constructor<T> ctor : declaredCtors(clazz)) {
      if (ctor.getParameterTypes().length != 2) continue;
      if (ctor.getParameterTypes()[1].isAssignableFrom(List.class)) {
        return ctor;
      }
    }
    throw new RuntimeException("Cannot find clone ctor for node " + clazz);
  }

  @SuppressWarnings({"unchecked", "cast"})
  private static <T> List<Constructor<T>> declaredCtors(Class<T> clazz) {
    // This is typesafe because a constructor yields exactly the type T,
    // never a subclass.  Unfortunately getDeclaredConstructors loses type info
    // since it returns an array.
    return (List<Constructor<T>>) (List)
        Arrays.asList(clazz.getDeclaredConstructors());
  }

  private static String getCtorErrorMessage(
      Constructor<? extends ParseTreeNode> ctor, Object value,
      List<? extends ParseTreeNode> children) {
    return "Error calling ctor " + ctor.toString()
        +" with value = " + value
        +" (" + (value == null ? "" : value.getClass()) + ")"
        +" with children = " + children
        +" (" + (children == null ? "" : children.getClass()) + ")";
  }
}
