// Copyright (C) 2009 Google Inc.
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

package com.google.caja.ancillary.servlet;

import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.ObjectConstructor;

import org.w3c.dom.Attr;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A parse tree with meta information.
 *
 * @author mikesamuel@gmail.com
 */
final class Job {
  /** The type of the root. */
  final ContentType t;
  /** A parse tree node or other content. */
  final Object root;
  /**
   * The place in the input HTML from where the job was extracted.
   * For JS this might be a {@code script} element or {@code onclick} attribute.
   */
  final Node origin;

  private Job(ContentType t, Object root, Node origin) {
    this.t = t;
    this.root = root;
    this.origin = origin;
  }

  static Job js(Block root, Node origin) {
    return new Job(ContentType.JS, root, origin);
  }

  static Job json(ObjectConstructor root) {
    return new Job(ContentType.JSON, root, null);
  }

  static Job css(CssTree.StyleSheet css, Element origin) {
    return new Job(ContentType.CSS, css, origin);
  }

  static Job css(CssTree.DeclarationGroup css, Attr origin) {
    return new Job(ContentType.CSS, css, origin);
  }

  static Job html(DocumentFragment fragment) {
    return new Job(ContentType.HTML, fragment, null);
  }

  static Job zip(byte[] zipBody) {
    return new Job(ContentType.ZIP, zipBody, null);
  }

  @Override
  public String toString() { return "[Job " + t + "]"; }
}
