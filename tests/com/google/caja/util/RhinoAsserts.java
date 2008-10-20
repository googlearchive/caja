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

package com.google.caja.util;

import com.google.caja.lexer.escaping.Escaping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * Utilities for dealing with values produced by the Rhino JS interpreter.
 *
 * @author mikesamuel@gmail.com
 */
public final class RhinoAsserts {

  /**
   * Coerces a JavaScript object graph to a canonical form that can be compared
   * to another object graph for equivalence across JavaScript execution
   * contexts.
   * <p>
   * Properties ending in two-underscores are not considered significant, and
   * non-element properties on Arrays are not considered significant.
   * <p>
   * No attempt is made to serialize functions to a structural form, and since
   * functions can't be compared, neither can instances of user defined classes.
   *
   * @param o the result of a JavaScript computation.
   * @return a string that would be recognized by SpiderMonkey's eval.
   *   This form differs from JSON in that it uses SpiderMonkey's {@code uneval}
   *   result format to deal with objects that have multiple in-bound edges.
   */
  public static String structuralForm(Object o) {
    final Map<Object, VisitationRecord> visited
        = new IdentityHashMap<Object, VisitationRecord>();

    // Walk the object graph and figure out which objects have multiple in-bound
    // edges, and assign a number to them.
    walk(o, new JsObjVisitor() {
      @Override
      void visitArray(Scriptable s, Object[] values) {
        if (visit(s)) {
          for (Object value : values) { walk(value, this); }
        }
      }
      @Override
      void visitObject(Scriptable s, List<Pair<String, Object>> props) {
        if (visit(s)) {
          for (Pair<String, Object> prop : props) {
            walk(prop.b, this);
          }
        }
      }

      int counter = 0;
      private boolean visit(Object o) {
        VisitationRecord r = visited.get(o);
        if (r == null) {
          visited.put(o, r = new VisitationRecord());
          return true;
        } else {
          if (r.key == 0) { r.key = ++counter; }
          return false;
        }
      }
    });

    // Now that we know which object's need to have a numeric label attached
    // we can serialize them to a String.
    final StringBuilder sb = new StringBuilder();
    walk(o, new JsObjVisitor() {
      // Primitives are rendered as in JavaScript.
      @Override
      void visitString(String s) {
        sb.append('"');
        Escaping.escapeJsString(s, true, false, sb);
        sb.append('"');
      }
      @Override
      void visitNumber(Number n) {
        sb.append(n);
      }
      @Override
      void visitBoolean(Boolean b) {
        sb.append(b);
      }
      @Override
      void visitNull() { sb.append("null"); }
      @Override
      void visitUndefined() { sb.append("undefined"); }
      // We serialize Arrays using the abbreviated syntax, and ignore
      // non-integral keys.
      @Override
      void visitArray(Scriptable s, Object[] values) {
        if (checkDupePrefix(s)) {
          sb.append('[');
          for (int i = 0, n = values.length; i < n; ++i) {
            if (i != 0) { sb.append(", "); }
            walk(values[i], this);
          }
          sb.append(']');
        }
      }
      // We serialize Objects using the abbreviates {...} syntax ignoring
      // properties that end in __.
      @Override
      void visitObject(Scriptable s, List<Pair<String, Object>> props) {
        if (checkDupePrefix(s)) {
          sb.append('{');
          boolean sawOne = false;
          for (Pair<String, Object> prop : props) {
            if (sawOne) {
              sb.append(", ");
            } else {
              sawOne = true;
            }
            sb.append('"');
            Escaping.escapeJsString(prop.a, true, false, sb);
            sb.append("\": ");
            walk(prop.b, this);
          }
          sb.append('}');
        }
      }

      /**
       * If this is the first mention of an object with multiple in-bound edges,
       * attach a numeric label like {@code #1=}.
       * If this is the second or subsequent use, emit a reference like
       * {@code #1#}, and return false so that the renderer knows not to render
       * the object.
       * @return true if o should be rendered.
       */
      private boolean checkDupePrefix(Object o) {
        VisitationRecord r = visited.get(o);
        if (r.key == 0) { return true; }
        sb.append('#').append(r.key);
        if (r.written) {
          sb.append('#');
          return false;
        }
        r.written = true;
        sb.append('=');
        return true;
      }
    });

    return sb.toString();
  }

  /**
   * Tracks whether a particular Object has been seen before.
   * This is used to generate a SpiderMonkey-style representation.
   * Spidermonkey's {@code uneval} will generate a string like
   * {@code #1=[#1#]} for an Array that contains only itself
   */
  private static class VisitationRecord {
    /**
     * A key, unique within the context of a particular object graph, that
     * can be used to identify an object that has multiple in-bound links in
     * that graph.
     * <p>A value of zero indicates that the corresponding object only has one
     * in-bound link.
     */
    int key;
    /** True if the object has already been written out. */
    boolean written;
  }

  /** A visitor over a JavaScript object graph. */
  private static abstract class JsObjVisitor {
    /** @param s unused in this default implementation. */
    void visitString(String s) {}
    /** @param n unused in this default implementation. */
    void visitNumber(Number n) {}
    /** @param b unused in this default implementation. */
    void visitBoolean(Boolean b) {}
    void visitNull() {}
    void visitUndefined() {}
    /**
     * @param s the JS array.
     * @param values elements of the array
     */
    void visitArray(Scriptable s, Object[] values) {}
    /**
     * @param s the JSON object.
     * @param props name to value map of cajita mentionable properties.
     */
    void visitObject(Scriptable s, List<Pair<String, Object>> props) {}
  }

  /**
   * Dispatches a node in a JavaScript object graph to a {@link JsObjVisitor}.
   */
  private static void walk(Object o, JsObjVisitor visitor) {
    if (o == null) {
      visitor.visitNull();
    } else if (o instanceof String) {
      visitor.visitString((String) o);
    } else if (o instanceof Number) {
      visitor.visitNumber((Number) o);
    } else if (o instanceof Boolean) {
      visitor.visitBoolean((Boolean) o);
    } else if (o instanceof Undefined) {
      visitor.visitUndefined();
    } else if (o instanceof NativeArray) {
      Scriptable s = ((Scriptable) o);
      Scriptable globalScope = s.getParentScope();

      Object lengthVal = s.get("length", globalScope);
      int length = ((Number) lengthVal).intValue();
      Object[] elements = new Object[length];
      for (int i = 0; i < length; ++i) {
        elements[i] = s.get(i, globalScope);
      }
      visitor.visitArray(s, elements);
    } else if (o instanceof Scriptable && isBaseObject((Scriptable) o)) {
      Scriptable s = ((Scriptable) o);
      Scriptable globalScope = s.getParentScope();

      Object[] ids = s.getIds();
      // Ensure a consistent key ordering, placing array indices first.
      Arrays.sort(ids, new Comparator<Object>() {
        public int compare(Object a, Object b) {
          if (a instanceof Number) {
            if (b instanceof Number) {
              double d = ((Number) a).doubleValue(),
              e = ((Number) b).doubleValue();
              return (
                  Double.isNaN(d)
                  ? (Double.isNaN(e)
                     ? 0
                     : 1)
                  : (d < e
                     ? -1
                     : (d == e ? 0 : 1)));
            } else {
              return -1;
            }
          } else if (b instanceof Number) {
            return 1;
          } else {
            return a.toString().compareTo(b.toString());
          }
        }
      });
      List<Pair<String, Object>> props = new ArrayList<Pair<String, Object>>();
      for (Object id : ids) {
        if (id instanceof Number) {
          Number n = (Number) id;
          int i = n.intValue();
          if (i == n.doubleValue() && i >= 0) {
            props.add(Pair.pair("" + i, s.get(i, globalScope)));
            continue;
          }
        }
        String k = id.toString();
        if (!k.endsWith("__")) {
          props.add(Pair.pair(k, s.get(k, globalScope)));
        }
      }
      visitor.visitObject(s, props);
    } else {
      String typeHint = "";
      if (o instanceof Scriptable) {
        typeHint = " : " + ((Scriptable) o).getClassName();
      }
      throw new IllegalArgumentException(
          "Cannot compare structure of " + o + typeHint);
    }
  }

  private static boolean isBaseObject(Scriptable s) {
    // A direct instance of Object is the only Object whose prototype's
    // prototype is null.
    Scriptable proto = s.getPrototype();
    return proto != null && proto.getPrototype() == null;
  }

  private RhinoAsserts() { /* uninstantiable */ }
}
