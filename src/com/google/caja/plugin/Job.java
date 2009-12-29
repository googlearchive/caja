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
import com.google.caja.util.ContentType;

import java.net.URI;

/**
 * A parse tree that is awaiting rewriting, compiling, or rendering.
 */
public final class Job {
  private final AncestorChain<?> root;
  private final ContentType type;
  private final URI baseUri;

  public static Job job(AncestorChain<?> root, URI baseUri) {
    ContentType type;
    ParseTreeNode rootNode = root.node;
    if (rootNode instanceof Statement
        || rootNode instanceof Expression
        || rootNode instanceof UncajoledModule
        || rootNode instanceof CajoledModule) {
      type = ContentType.JS;
    } else if (rootNode instanceof Dom) {
      type = ContentType.HTML;
      assert baseUri != null;
    } else if (rootNode instanceof CssTree.StyleSheet) {
      type = ContentType.CSS;
      assert baseUri != null;
    } else {
      throw new SomethingWidgyHappenedError("Unknown input type " + rootNode);
    }
    return new Job(root, type, baseUri);
  }

  public static Job jsJob(AncestorChain<? extends Statement> root) {
    return new Job(root, ContentType.JS, null);
  }

  public static Job exprJob(AncestorChain<? extends Expression> root) {
    return new Job(root, ContentType.JS, null);
  }

  public static Job moduleJob(AncestorChain<? extends UncajoledModule> root) {
    return new Job(root, ContentType.JS, null);
  }

  public static Job cajoledJob(AncestorChain<? extends CajoledModule> root) {
    return new Job(root, ContentType.JS, null);
  }

  public static Job domJob(AncestorChain<? extends Dom> root, URI baseUri) {
    assert baseUri != null;
    return new Job(root, ContentType.HTML, baseUri);
  }

  public static Job cssJob(
      AncestorChain<? extends CssTree.StyleSheet> root, URI baseUri) {
    assert baseUri != null;
    return new Job(root, ContentType.CSS, baseUri);
  }

  private Job(AncestorChain<?> root, ContentType type, URI baseUri) {
    assert root != null;
    this.root = root;
    this.type = type;
    this.baseUri = baseUri;
  }

  public AncestorChain<?> getRoot() { return root; }

  public ContentType getType() { return type; }

  public URI getBaseUri() { return baseUri; }

  @Override
  public String toString() {
    return "(Job " + getType().name() + " " + getRoot() + ")";
  }
}
