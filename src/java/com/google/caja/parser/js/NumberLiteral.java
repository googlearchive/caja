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

package com.google.caja.parser.js;

/**
 * A literal numeric value.  This class is abstract so that we can distinguish
 * literals based on the format they were parsed from.
 * If in doubt about which implementation to use, use {@link RealLiteral}.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class NumberLiteral extends Literal {
  protected NumberLiteral() { }

  @Override
  public abstract Number getValue();

  /** Same as {@code getValue().doubleValue()} */
  public abstract double doubleValue();

  @Override
  public boolean getValueInBooleanContext() {
    double n = doubleValue();
    return !(Double.isNaN(n) || 0D == n);
  }
}
