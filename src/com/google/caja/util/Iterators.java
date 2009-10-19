
package com.google.caja.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Utility methods for dealing with {@link Iterator iterators}.
 *
 * @author mikesamuel@gmail.com
 */
public final class Iterators {

  public static <T> Iterator<T> filter(
      final Iterator<T> underlying, final Set<T> forbidden) {
    return new Iterator<T>() {
      boolean hasNext;
      T next;

      public boolean hasNext() {
        fetch();
        return hasNext;
      }

      public T next() {
        fetch();
        if (!hasNext) { throw new NoSuchElementException(); }
        T result = next;
        hasNext = false;
        next = null;
        return result;
      }

      public void remove() {
        underlying.remove();
      }

      private void fetch() {
        if (hasNext) { return; }
        while (underlying.hasNext()) {
          next = underlying.next();
          if (!forbidden.contains(next)) {
            hasNext = true;
            return;
          }
        }
        next = null;
      }
    };
  }

  private Iterators() { /* not instantiable */ }
}
