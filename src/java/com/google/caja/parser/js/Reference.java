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

import com.google.caja.plugin.ReservedNames;
import com.google.caja.reporting.RenderContext;

import java.io.IOException;

/**
 *
 * @author mikesamuel@gmail.com
 */
public class Reference extends AbstractExpression<Expression> {
  private String identifier;

  public Reference(String identifier) {
    if (null == identifier) {
      throw new NullPointerException("null identifier");
    }
    this.identifier = identifier;
    childrenChanged();
  }

  public String getIdentifier() { return this.identifier; }
  public void setIdentifier(String identifier) { this.identifier = identifier; }

  @Override
  public Object getValue() { return this.identifier; }

  @Override
  public boolean isLeftHandSide() {
    return true;
  }

  public boolean isThis() {
    return this.identifier.equals("this") || this.identifier.equals(ReservedNames.LOCAL_THIS);
  }

  public void render(RenderContext rc) throws IOException {
    rc.out.append(identifier);
  }

  public boolean isSuper() {
    return this.identifier.equals(ReservedNames.SUPER);
  }
}
