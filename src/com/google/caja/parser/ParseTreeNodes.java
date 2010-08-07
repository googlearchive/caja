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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
   *        (see {@link ParseTreeNode#getValue()}).
   * @param children the children of the new node.
   * @return the newly constructed {@code ParseTreeNode}.
   */
  public static <T extends ParseTreeNode> T newNodeInstance(
      Class<T> clazz, FilePosition pos, Object value,
      List<? extends ParseTreeNode> children) {
    Constructor<T> ctor = findCloneCtor(clazz);
    try {
      return ctor.newInstance(pos, value, children);
    } catch (InstantiationException e) {
      throw new SomethingWidgyHappenedError(
          getCtorErrorMessage(ctor, value, children), e);
    } catch (IllegalAccessException e) {
      throw new SomethingWidgyHappenedError(
          getCtorErrorMessage(ctor, value, children), e);
    } catch (InvocationTargetException e) {
      throw new SomethingWidgyHappenedError(
          getCtorErrorMessage(ctor, value, children), e);
    } catch (IllegalArgumentException e) {
      throw new SomethingWidgyHappenedError(
          getCtorErrorMessage(ctor, value, children), e);
    }
  }

  /**
   * Perform a deep equality test on a pair of {@code ParseTreeNode}s.
   *
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

  private static final Map<Class<? extends ParseTreeNode>, Constructor<?>>
      cloneCtorCache = Collections.synchronizedMap(
          new HashMap<Class<? extends ParseTreeNode>, Constructor<?>>());
  private static <T extends ParseTreeNode>
  Constructor<T> findCloneCtor(Class<T> clazz) {
    {
      Constructor<T> ctor = fromCtorCache(clazz);
      if (ctor != null) { return ctor; }
    }
    for (Constructor<T> ctor : declaredCtors(clazz)) {
      Class<?>[] parameterTypes = ctor.getParameterTypes();
      if (parameterTypes.length == 3
          && FilePosition.class.equals(parameterTypes[0])
          && parameterTypes[2].isAssignableFrom(List.class)
          && isReflectiveCtorAnnotated(ctor)) {
        cloneCtorCache.put(clazz, ctor);
        return ctor;
      }
    }
    throw new SomethingWidgyHappenedError(
        "Cannot find clone ctor for node " + clazz);
  }

  private static final boolean isReflectiveCtorAnnotated(Constructor<?> ctor) {
    for (int i = 0; i < ctor.getDeclaredAnnotations().length; ++i) {
      if (ctor.getDeclaredAnnotations()[i]
          instanceof ParseTreeNode.ReflectiveCtor) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private static <T extends ParseTreeNode>
  Constructor<T> fromCtorCache(Class<T> clazz) {
    return (Constructor<T>) cloneCtorCache.get(clazz);
  }

  @SuppressWarnings({"cast", "rawtypes", "unchecked"})
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
