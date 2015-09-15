// Copyright (C) 2005 Google Inc.
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

/**
 * A pure function that accepts or rejects instances of a class T.
 *
 * @author mikesamuel@gmail.com
 */
public interface Criterion<T> {
  // TODO(mikesamuel): replace "may be null" and "not null" shorthands with
  // @Nullable and @NotNull annotations
  // TODO(mikesamuel): replace with com.google.common.Predicate and the
  // factory with Predicates

  /**
   * @return true iff the candidate is accepted by this criterion.
   * @param candidate may be null.
   */
  public boolean accept(T candidate);

  public static class Factory {

    private static final Criterion<?> OPTIMIST_SINGLETON =
      new Criterion<Object>() {
        public boolean accept(Object x) {
          return true;
        }
        @Override
        public String toString() { return "OptimistCriterion"; }
      };

    private static final Criterion<?> PESSIMIST_SINGLETON =
      new Criterion<Object>() {
        public boolean accept(Object x) {
          return false;
        }
        @Override
        public String toString() { return "PessimistCriterion"; }
      };

    /**
     * A criterion that accepts all instances of the given class,
     * including null.
     * @return non null.
     */
    @SuppressWarnings("unchecked")
    public static <T> Criterion<T> optimist() {
      return (Criterion<T>) OPTIMIST_SINGLETON;
    }

    /**
     * A criterion that accepts nothing, not even null.
     * @return non null.
     */
    @SuppressWarnings("unchecked")
    public static <T> Criterion<T> pessimist() {
      return (Criterion<T>) PESSIMIST_SINGLETON;
    }

    /**
     * A criterion that is true iff any of the given criteria accept
     * the candidate.
     * @param a non null.
     * @param b non null.
     * @return non null.
     */
    public static <T> Criterion<T> or(
        final Criterion<T> a, final Criterion<? super T> b) {
      if (a == null || b == null) { throw new NullPointerException(); }
      return new Criterion<T>() {
          public boolean accept(T candidate) {
            return a.accept(candidate) || b.accept(candidate);
          }
        };
    }

    /**
     * A criterion that is true iff all of the given criteria accept
     * the candidate.
     * @param a non null.
     * @param b non null.
     * @return non null.
     */
    public static <T> Criterion<T> and(
        final Criterion<T> a, final Criterion<? super T> b) {
      if (null == a || null == b) { throw new NullPointerException(); }
      return new Criterion<T>() {
          public boolean accept(T candidate) {
            return a.accept(candidate) && b.accept(candidate);
          }
        };
    }

    /**
     * A criterion that accepts the inverse of the set of inputs accepted by
     * criterion
     * @param criterion non null.
     * @return non null.
     */
    public static <T> Criterion<T> not(final Criterion<T> criterion) {
      return !(criterion instanceof Contrarian<?>)
          ? new Contrarian<T>(criterion)
          : ((Contrarian<T>) criterion).c;
    }

    private static final class Contrarian<T> implements Criterion<T> {
      private Criterion<T> c;

      public Contrarian(Criterion<T> c) {
        this.c = c;
      }

      public boolean accept(T candidate) {
        return !c.accept(candidate);
      }

      @Override
      public String toString() { return "!" + c; }
    }
  }
}
