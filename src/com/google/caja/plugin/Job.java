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
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.html.Dom;
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
  private final ParseTreeNode root;
  private final ContentType type;
  private final URI baseUri;

  public static Job job(ParseTreeNode rootNode, URI baseUri) {
    ContentType type;
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
    return new Job(rootNode, type, baseUri);
  }

  public static Job jsJob(Statement root, URI baseUri) {
    return new Job(root, ContentType.JS, baseUri);
  }

  public static Job exprJob(Expression root, URI baseUri) {
    return new Job(root, ContentType.JS, baseUri);
  }

  public static Job moduleJob(UncajoledModule root, URI baseUri) {
    return new Job(root, ContentType.JS, baseUri);
  }

  public static Job cajoledJob(CajoledModule root) {
    return new Job(root, ContentType.JS, null);
  }

  public static Job domJob(Dom root, URI baseUri) {
    assert baseUri != null;
    return new Job(root, ContentType.HTML, baseUri);
  }

  public static Job cssJob(CssTree.StyleSheet root, URI baseUri) {
    assert baseUri != null;
    return new Job(root, ContentType.CSS, baseUri);
  }

  Job(ParseTreeNode root, ContentType type, URI baseUri) {
    this.root = root;
    this.type = type;
    this.baseUri = baseUri;
  }

  public ParseTreeNode getRoot() { return root; }

  /** Indicates the type of parse tree returned by {@link #getRoot}. */
  public ContentType getType() { return type; }

  /** The URI against which relative URIs are resolved. */
  public URI getBaseUri() { return baseUri; }

  @Override
  public String toString() {
    return "(Job " + getType().name() + " " + getRoot() + ")";
  }
}
