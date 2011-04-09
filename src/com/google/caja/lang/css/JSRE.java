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

package com.google.caja.lang.css;

import com.google.caja.lexer.escaping.Escaping;
import com.google.caja.util.Lists;
import com.google.caja.util.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

abstract class JSRE {
  static JSRE cat(JSRE... parts) { return new Concatenation(parts); }
  static JSRE cat(List<JSRE> parts) { return new Concatenation(parts); }
  static JSRE alt(JSRE... parts) { return new Alternation(parts); }
  static JSRE alt(List<JSRE> parts) { return new Alternation(parts); }
  static JSRE rep(JSRE body, int min, int max) {
    return new Repetition(body, min, max);
  }
  static JSRE opt(JSRE body) { return rep(body, 0, 1); }
  static JSRE any(JSRE body) { return rep(body, 0, Integer.MAX_VALUE); }
  static JSRE many(JSRE body) { return rep(body, 1, Integer.MAX_VALUE); }
  static JSRE raw(String atom) {
    return "".equals(atom) ? new Noop() : new Atom(atom);
  }
  static JSRE lit(String literal) {
    int n = literal.length();
    JSRE[] parts = new JSRE[n];
    StringBuilder sb = new StringBuilder();
    for (int i = n; --i >= 0;) {
      sb.setLength(0);
      Escaping.escapeRegex(literal.substring(i, i + 1), false, false, sb);
      parts[i] = raw(sb.toString());
    }
    return cat(parts);
  }

  abstract JSRE optimize();
  abstract void render(StringBuilder sb);
  abstract Priority priority();
  @Override public abstract boolean equals(Object o);
  @Override public abstract int hashCode();
  JSRE without(List<JSRE> head, List<JSRE> tail) {
    switch (head.size() + tail.size()) {
      case 0: return this;
      case 1:
        if ((head.isEmpty() ? tail : head).get(0).equals(this)) {
          return new Noop();
        }
        break;
    }
    throw new IllegalArgumentException();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    render(sb);
    return sb.toString();
  }

  void renderChild(JSRE child, StringBuilder out) {
    if (child.priority().compareTo(priority()) <= 0) {
      out.append("(?:");
      child.render(out);
      out.append(')');
    } else {
      child.render(out);
    }
  }

  enum Priority {
    ALTERNATION,
    CONCATENATION,
    REPETITION,
    MAX,
    ;
  }

  static final class Noop extends JSRE {
    private Noop() { /* no public zero-argument ctor */ }

    @Override public boolean equals(Object o) { return o instanceof Noop; }

    @Override public int hashCode() { return 0; }

    @Override JSRE optimize() { return this; }

    @Override Priority priority() { return Priority.MAX; }

    @Override
    void render(StringBuilder sb) { /* no output */ }
  }

  static final class Repetition extends JSRE {
    final JSRE body;
    final int min, max;

    private Repetition(JSRE body, int min, int max) {
      this.body = body;
      this.min = min;
      this.max = max;
    }

    @Override
    JSRE optimize() {
      JSRE newBody = body.optimize();
      if (min == 1 && max == 1) { return newBody; }
      if (newBody instanceof Noop || max == 0) { return newBody; }
      if (newBody instanceof Repetition) {
        Repetition r = (Repetition) newBody;
        if (r.max == 1) {
          // (x?)? => x?
          // (x?)* => x*
          // (x?)+ => x*
          // (x?){2,} => x*
          // (x?){2,4} => x{0,4}
          return new Repetition(r.body, 0, max);
        } else if (r.max == Integer.MAX_VALUE) {
          if (r.min == 0) {
            // (x*)? => x*
            // (x*)* => x*
            // (x*)+ => x*
            // (x*){2,3} => x*
            return new Repetition(r.body, 0, Integer.MAX_VALUE);
          } else if (r.min == 1) {
            // (x+)? => x*
            // (x+)* => x*
            // (x+)+ => x+
            // (x+){2,4} => x{2,}
            return new Repetition(r.body, min, Integer.MAX_VALUE);
          }
        }
      }
      return newBody == body ? this : new Repetition(newBody, min, max);
    }

    @Override Priority priority() { return Priority.REPETITION; }

    @Override void render(StringBuilder sb) {
      renderChild(body, sb);
      if (min == 0) {
        if (max == 1) {
          sb.append('?');
          return;
        } else if (max == Integer.MAX_VALUE) {
          sb.append('*');
          return;
        }
      } else if (min == 1 && max == Integer.MAX_VALUE) {
        sb.append('+');
        return;
      }
      sb.append('{');
      sb.append(min);  // Leaving out 0 does not work in JS.
      if (max != min) {
        sb.append(',');
        if (max != Integer.MAX_VALUE) { sb.append(max); }
      }
      sb.append('}');
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Repetition)) { return false; }
      Repetition that = (Repetition) o;
      return this.min == that.min && this.max == that.max
          && this.body.equals(that.body);
    }

    @Override
    public int hashCode() {
      return 31 * (this.min + 31 * this.max) + this.body.hashCode();
    }
  }

  static final class Alternation extends JSRE {
    final List<JSRE> children;
    final boolean isCharSet;

    private Alternation(JSRE... children) {
      this(Arrays.asList(children));
    }

    private Alternation(Collection<JSRE> children) {
      List<JSRE> clone = Lists.newArrayList(children);
      boolean isCharSet = true;
      for (JSRE child : clone) {
        if (child instanceof Atom) {
          String atom = ((Atom) child).atom;
          if (atom.length() == 1
              || (atom.length() == 2 && '\\' == atom.charAt(0))) {
            continue;
          }
        }
        isCharSet = false;
        break;
      }
      if (isCharSet) {
        // Normalize escapes.
        for (int i = clone.size(); --i >= 0;) {
          Atom child = (Atom) clone.get(i);
          if (child.atom.length() == 2) {
            char ch = child.atom.charAt(1);
            if (!(('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z')
                  || ('0' <= ch && ch <= '9'))) {
              clone.set(i, new Atom(Character.toString(ch)));
            }
          }
        }
        // Sort so we can collapse ranges in render.
        Collections.sort(clone, new Comparator<JSRE>() {
          public int compare(JSRE a, JSRE b) {
            return ((Atom) a).atom.compareTo((((Atom) b).atom));
          }
        });
      }
      this.isCharSet = isCharSet;
      this.children = Collections.unmodifiableList(clone);
    }

    @Override
    JSRE optimize() {
      boolean different = false;
      boolean optional = false;
      Set<JSRE> optimized = Sets.newLinkedHashSet();
      for (JSRE child : children) {
        JSRE opt = child.optimize();
        if (opt instanceof Alternation) {
          optimized.addAll(((Alternation) opt).children);
          different = true;
        } else if (opt instanceof Noop) {
          optional = different = true;
        } else {
          if (!optimized.add(opt)) { different = true; }
          different |= opt != child;
        }
      }
      if (optimized.isEmpty()) { return new Noop(); }
      JSRE out;
      if (optimized.size() == 1) {
        // (a|a) => a
        out = optimized.iterator().next();
      } else {
        List<JSRE> commonHead = null;
        List<JSRE> commonTail = null;
        int minParts = Integer.MAX_VALUE;
        for (JSRE child : optimized) {
          List<JSRE> parts = asList(child);
          minParts = Math.min(minParts, parts.size());
          if (commonHead == null) {
            commonHead = commonTail = parts;
            minParts = parts.size();
          } else {
            commonHead = commonPrefix(commonHead, parts);
            commonTail = commonSuffix(commonTail, parts);
            if (commonHead.isEmpty() && commonTail.isEmpty()) { break; }
          }
        }
        assert commonHead != null && commonTail != null : "no children";
        if (!commonHead.isEmpty() || !commonTail.isEmpty()) {
          // (foo|far|faz) => f(oo|ar|az)
          // (oof|raf|zaf) => (oo|ra|za)f
          if (commonHead.size() + commonTail.size() > minParts) {
            // Avoid problems with overlapping things like:
            //   ababa|aba
            // where commonHead = aba and commonTail = aba.
            // This will cause that to optimize to:
            //   a(ba|)ba
            commonHead = commonHead.subList(0, minParts - commonTail.size());
          }
          List<JSRE> reducedChildren = Lists.newArrayList();
          for (JSRE child : optimized) {
            reducedChildren.add(child.without(commonHead, commonTail));
          }
          List<JSRE> parts = Lists.newArrayList();
          parts.addAll(commonHead);
          parts.add(new Alternation(reducedChildren));
          parts.addAll(commonTail);
          out = new Concatenation(parts).optimize();
        } else {
          out = different ? new Alternation(optimized) : this;
        }
      }
      // Could do cross product optimization by looking for common middle.
      // (aox|aoy|aoz|box|boy|boz|cox|coy|coz) => (a|b|c)o(d|e|f)
      // But complicated.

      // (|b) => b?
      return optional ? new Repetition(out, 0, 1) : out;
    }

    @Override Priority priority() {
      return isCharSet ? Priority.MAX : Priority.ALTERNATION;
    }

    private void renderCharInSet(char ch, StringBuilder out) {
      switch (ch) {
        case '\\': case '-': case '^': case ']':
          out.append('\\').append(ch);
          break;
        default:
          out.append(ch);
          break;
      }
    }

    private void renderRangeTail(int start, int last, StringBuilder out) {
      if (last != -1 && last != start) {
        if (last > start + 1) { out.append('-'); }
        renderCharInSet((char) last, out);
      }
    }

    @Override
    void render(StringBuilder sb) {
      if (isCharSet) {
        sb.append('[');
        int rangeStart = -1;
        int lastInRange = -1;
        for (JSRE child : children) {
          // Either a single character or a \ escaped character.
          String atom = ((Atom) child).atom;
          if (atom.length() != 1) {
            renderRangeTail(rangeStart, lastInRange, sb);
            rangeStart = lastInRange = -1;
            sb.append(atom);
          } else {
            char ch = atom.charAt(0);
            if (lastInRange == -1 || ch != lastInRange + 1) {
              renderRangeTail(rangeStart, lastInRange, sb);
              lastInRange = rangeStart = ch;
              renderCharInSet(ch, sb);
            } else {
              lastInRange = ch;
            }
          }
        }
        renderRangeTail(rangeStart, lastInRange, sb);
        sb.append(']');
      } else {
        for (int i = 0, n = children.size(); i < n; ++i) {
          if (i != 0) { sb.append('|'); }
          renderChild(children.get(i), sb);
        }
      }
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Alternation)) { return false; }
      return children.equals(((Alternation) o).children);
    }

    @Override
    public int hashCode() {
      return getClass().hashCode() ^ children.hashCode();
    }
  }

  private static List<JSRE> asList(JSRE jsre) {
    if (jsre instanceof Concatenation) {
      return ((Concatenation) jsre).children;
    } else if (jsre instanceof Noop) {
      return Collections.<JSRE>emptyList();
    } else {
      return Collections.singletonList(jsre);
    }
  }

  private static <T> List<T> commonPrefix(List<T> a, List<T> b) {
    int n = Math.min(a.size(), b.size());
    int i = 0;
    while (i < n && a.get(i).equals(b.get(i))) { ++i; }
    // Wrapping in a new array list because List.subList is not serializable
    return new ArrayList(a.subList(0, i));
  }

  private static <T> List<T> commonSuffix(List<T> a, List<T> b) {
    int m = a.size(), n = b.size();
    int i = m, j = n;
    while (--i >= 0 && --j >= 0 && a.get(i).equals(b.get(j))) {
      // work done in condition
    }
    // Wrapping in a new array list because List.subList is not serializable
    return new ArrayList(a.subList(i + 1, m));
  }

  static final class Concatenation extends JSRE {
    final List<JSRE> children;

    private Concatenation(JSRE... children) {
      this(Arrays.asList(children));
    }

    private Concatenation(Collection<JSRE> children) {
      this.children = Collections.unmodifiableList(
          Lists.newArrayList(children));
    }

    @Override
    JSRE optimize() {
      boolean different = false;
      List<JSRE> optimized = Lists.newArrayList();
      for (JSRE child : children) {
        JSRE opt = child.optimize();
        if (opt instanceof Noop) {
          different = true;
        } else if (opt instanceof Concatenation) {
          optimized.addAll(((Concatenation) opt).children);
          different = true;
        } else {
          optimized.add(opt);
          different |= opt != child;
        }
      }
      if (foldAdjacentReps(optimized)) { different = true; }
      if (foldOptMany(optimized)) { different = true; }
      switch (optimized.size()) {
        case 0: return new Noop();
        case 1: return optimized.get(0);
        default: return different ? new Concatenation(optimized) : this;
      }
    }

    private static boolean foldAdjacentReps(List<JSRE> items) {
      boolean changed = false;
      int n = items.size();
      for (int i = 1; i < n; ++i) {
        JSRE item = items.get(i);
        JSRE prior = items.get(i - 1);

        List<JSRE> parts0 = items.subList(i - 1, i);
        List<JSRE> parts1 = items.subList(i, i + 1);
        JSRE body0, body1;
        int min0, max0, min1, max1;

        if (item instanceof Repetition) {
          Repetition r = (Repetition) item;
          body1 = r.body;
          min1 = r.min;
          max1 = r.max;
        } else if (prior instanceof Repetition
                   && ((Repetition) prior).body instanceof Concatenation) {
          int catn = ((Concatenation) ((Repetition) prior).body)
              .children.size();
          if (i + catn <= n) {
            body1 = new Concatenation(parts1 = items.subList(i, i + catn));
            min1 = max1 = 1;
          } else {
            body1 = item;
            min1 = max1 = 1;
          }
        } else {
          body1 = item;
          min1 = max1 = 1;
        }
        if (prior instanceof Repetition) {
          Repetition r = (Repetition) prior;
          body0 = r.body;
          min0 = r.min;
          max0 = r.max;
        } else if (body1 instanceof Concatenation) {
          int catn = ((Concatenation) body1).children.size();
          if (i >= catn) {
            body0 = new Concatenation(parts0 = items.subList(i - catn, i));
            min0 = max0 = 1;
          } else {
            body0 = prior;
            min0 = max0 = 1;
          }
        } else {
          body0 = prior;
          min0 = max0 = 1;
        }

        if (body0.equals(body1)
            && (min0 != 1 || min1 != 1 || max0 != 1 || max1 != 1
                || shouldCombine(body0))) {
          long min = ((long) min0) + min1;
          long max = ((long) max0) + max1;
          if (min > Integer.MAX_VALUE) { min = Integer.MAX_VALUE; }
          if (max > Integer.MAX_VALUE) { max = Integer.MAX_VALUE; }
          items.set(i - parts0.size(),
                    new Repetition(body0, (int) min, (int) max));
          int nexti = i - parts0.size() + 1;
          items.subList(nexti, i + parts1.size()).clear();
          i = nexti;
          n = items.size();
          changed = true;
        }
      }
      return changed;
    }

    private static boolean foldOptMany(List<JSRE> items) {
      boolean changed = false;
      int n = items.size();
      for (int i = 1; i < n; ++i) {
        JSRE item = items.get(i);
        JSRE prior = items.get(i - 1);
        if (item instanceof Repetition && prior instanceof Repetition) {
          Repetition r0 = (Repetition) prior;
          Repetition r1 = (Repetition) item;
          if (r0.max == Integer.MAX_VALUE && r1.max == 1
              && r1.body instanceof Concatenation) {
            Concatenation b = (Concatenation) r1.body;
            JSRE r10 = b.children.get(0);
            if (// x*(?:xy)? => x*y?
                r0.equals(r10)
                // x*(?:x+y)? => x*y?
                || (r10 instanceof Repetition
                    && ((Repetition) r10).body.equals(r0))) {
              changed = true;
              items.set(
                  i,
                  JSRE.rep(
                      JSRE.cat(b.children.subList(1, b.children.size())),
                      r1.min, r1.max).optimize());
            }
          }
        }
      }
      return changed;
    }

    private static boolean shouldCombine(JSRE p) {
      return p.toString().length() >= 3;
    }

    @Override Priority priority() { return Priority.CONCATENATION; }

    @Override
    void render(StringBuilder sb) {
      for (JSRE child : children) { renderChild(child, sb); }
    }

    @Override
    JSRE without(List<JSRE> head, List<JSRE> tail) {
      int n = children.size();
      int h = head.size();
      int t = tail.size();
      assert head.equals(children.subList(0, h));
      assert tail.equals(children.subList(n - t, n));
      if (h + t == n) { return new Noop(); }
      return new Concatenation(children.subList(h, n - t));
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Concatenation)) { return false; }
      return children.equals(((Concatenation) o).children);
    }

    @Override
    public int hashCode() {
      return getClass().hashCode() ^ children.hashCode();
    }
  }

  static final class Atom extends JSRE {
    final String atom;

    private Atom(String atom) { this.atom = atom; }

    @Override JSRE optimize() { return this; }
    @Override Priority priority() { return Priority.MAX; }
    @Override void render(StringBuilder sb) { sb.append(atom); }

    @Override
    public boolean equals(Object o) {
      return o instanceof Atom && atom.equals(((Atom) o).atom);
    }

    @Override public int hashCode() { return atom.hashCode(); }
  }
}
