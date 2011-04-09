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

import java.io.Serializable;

/**
 * A key into {@link SyntheticAttributes} which asserts the type of the
 * corresponding value.
 *
 * @author mikesamuel@gmail.com
 */
public final class SyntheticAttributeKey<T>
    implements Comparable<SyntheticAttributeKey<?>>, Serializable {
  private static final long serialVersionUID = -419622209228551075L;
  private final Class<T> type;
  private final String name;

  public SyntheticAttributeKey(Class<T> type, String name) {
    if (null == type || null == name) { throw new NullPointerException(); }
    this.type = type;
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public Class<T> getType() {
    return this.type;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SyntheticAttributeKey<?>)) {
      return false;
    }
    SyntheticAttributeKey<?> that = (SyntheticAttributeKey<?>) obj;
    return type.equals(that.type) && this.name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode() ^ type.hashCode();
  }

  @Override
  public String toString() {
    String shortType = type.getName();
    shortType = shortType.substring(shortType.lastIndexOf('.') + 1);
    return name + ":" + shortType;
  }

  public int compareTo(SyntheticAttributeKey<?> that) {
    int delta = this.name.compareTo(that.name);
    if (0 == delta) {
      delta = this.type.getName().compareTo(that.type.getName());
    }
    return delta;
  }

}
