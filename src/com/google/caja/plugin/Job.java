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
import com.google.caja.plugin.stages.JobCache;
import com.google.caja.util.ContentType;

import java.net.URI;

/**
 * A parse tree that is awaiting rewriting, compiling, or rendering.
 */
public final class Job {
  private final ParseTreeNode root;
  private final ContentType type;
  private final URI baseUri;
  private final JobCache.Keys keys;

  public static Job job(
      JobCache.Keys keys, ParseTreeNode rootNode, URI baseUri) {
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
    return new Job(keys, rootNode, type, baseUri);
  }

  public static Job jsJob(JobCache.Keys keys, Statement root, URI baseUri) {
    return new Job(keys, root, ContentType.JS, baseUri);
  }

  public static Job exprJob(JobCache.Keys keys, Expression root, URI baseUri) {
    return new Job(keys, root, ContentType.JS, baseUri);
  }

  public static Job moduleJob(
      JobCache.Keys keys, UncajoledModule root, URI baseUri) {
    return new Job(keys, root, ContentType.JS, baseUri);
  }

  public static Job cajoledJob(JobCache.Keys keys, CajoledModule root) {
    return new Job(keys, root, ContentType.JS, null);
  }

  public static Job domJob(JobCache.Keys keys, Dom root, URI baseUri) {
    assert baseUri != null;
    return new Job(keys, root, ContentType.HTML, baseUri);
  }

  public static Job cssJob(
      JobCache.Keys keys, CssTree.StyleSheet root, URI baseUri) {
    assert baseUri != null;
    return new Job(keys, root, ContentType.CSS, baseUri);
  }

  private Job(
      JobCache.Keys keys, ParseTreeNode root, ContentType type,
      URI baseUri) {
    assert root != null;
    this.root = root;
    this.type = type;
    this.baseUri = baseUri;
    this.keys = keys == null ? JobCache.none() : keys;
  }

  public ParseTreeNode getRoot() { return root; }

  public ContentType getType() { return type; }

  public URI getBaseUri() { return baseUri; }

  public JobCache.Keys getCacheKeys() { return keys; }

  @Override
  public String toString() {
    return "(Job " + getType().name() + " " + getRoot() + ")";
  }
}
