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

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.DomTree;
import com.google.caja.parser.js.Statement;

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
  private final AncestorChain<?> target;
  private final Job.JobType type;

  public Job(AncestorChain<?> root) {
    this(root, null);
  }

  /**
   * @param target the location to insert the compiled output or null.
   */
  public Job(AncestorChain<?> root, AncestorChain<?> target) {
    assert root != null && (target == null || target.parent != null);
    this.root = root;
    this.target = target;
    ParseTreeNode rootNode = root.node;
    if (rootNode instanceof Statement) {
      this.type = Job.JobType.JAVASCRIPT;
    } else if (rootNode instanceof DomTree.Fragment
               || rootNode instanceof DomTree.Tag) {
      this.type = Job.JobType.HTML;
    } else if (rootNode instanceof CssTree.StyleSheet) {
      this.type = Job.JobType.CSS;
    } else {
      throw new RuntimeException("Unknown input type " + rootNode);
    }
  }

  public AncestorChain<?> getRoot() { return root; }

  /**
   * The node to replace with the compiled or rewritten root, or
   * null.
   */
  public AncestorChain<?> getTarget() { return target; }

  public Job.JobType getType() { return type; }

  @Override
  public String toString() {
    return "(Job " + getType().name() + " " + getRoot() + ")";
  }
}
