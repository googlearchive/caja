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

import com.google.caja.parser.ParseTreeNode;

import java.util.List;
import java.util.Map;

/**
 * A statement with a label, such as
 * <code>label: for (...) { ... }</code>.
 *
 * <p>Javascript bizarrely allows labels on any statements, but the labels are
 * only useful when applied to loops or switches.</p>
 *
 * @author mikesamuel@gmail.com
 */
public abstract class LabeledStatement
    extends AbstractStatement<ParseTreeNode> {
  private String label;

  protected LabeledStatement(String label) {
    this.label = label;
  }

  public String getLabel() { return label; }

  @Override
  public void breaks(Map<String, List<BreakStmt>> labels) {
    super.breaks(labels);
    labels.remove(this.label);
  }

  @Override
  public void continues(Map<String, List<ContinueStmt>> labels) {
    super.continues(labels);
    labels.remove(this.label);
  }

  @Override
  public final Object getValue() { return this.label; }
}
