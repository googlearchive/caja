// Copyright (C) 2007 Google Inc.
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

package com.google.caja.plugin;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.js.UncajoledModule;

/**
 * A parse tree that is awaiting rewriting, compiling, or rendering.
 */
public final class Job {
  public static enum JobType {
    CSS,
    JAVASCRIPT,
    HTML
    ;
  }

  private final AncestorChain<?> root;
  private final Job.JobType type;

  public Job(AncestorChain<?> root) {
    assert root != null;
    this.root = root;
    ParseTreeNode rootNode = root.node;
    if (rootNode instanceof Statement
        || rootNode instanceof Expression
        || rootNode instanceof UncajoledModule
        || rootNode instanceof CajoledModule) {
      this.type = Job.JobType.JAVASCRIPT;
    } else if (rootNode instanceof Dom) {
      this.type = Job.JobType.HTML;
    } else if (rootNode instanceof CssTree.StyleSheet) {
      this.type = Job.JobType.CSS;
    } else {
      throw new SomethingWidgyHappenedError("Unknown input type " + rootNode);
    }
  }

  public AncestorChain<?> getRoot() { return root; }

  public Job.JobType getType() { return type; }

  @Override
  public String toString() {
    return "(Job " + getType().name() + " " + getRoot() + ")";
  }
}
