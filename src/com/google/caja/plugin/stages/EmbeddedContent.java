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

package com.google.caja.plugin.stages;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.Parser;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.ContentType;
import com.google.caja.util.Function;

import java.net.URI;
import java.util.Collections;

import org.w3c.dom.Node;

/**
 * Content in another language extracted from an HTML document.
 *
 * @author mikesamuel@gmail.com
 */
public final class EmbeddedContent {
  private final HtmlEmbeddedContentFinder finder;
  private final FilePosition pos;
  private final Function<PluginEnvironment, CharProducer> getter;
  private final ExternalReference contentLocation;
  private final boolean deferred;
  private final Node source;
  private final ContentType type;

  EmbeddedContent(
      HtmlEmbeddedContentFinder finder, FilePosition pos,
      Function<PluginEnvironment, CharProducer> getter,
      ExternalReference contentLocation, boolean deferred, Node source,
      ContentType type) {
    this.finder = finder;
    this.pos = pos;
    this.getter = getter;
    this.deferred = deferred;
    this.contentLocation = contentLocation;
    this.source = source;
    this.type = type;
  }

  public URI getBaseUri() {
    return contentLocation != null
        ? contentLocation.getUri() : finder.getBaseUri();
  }
  public FilePosition getPosition() { return pos; }
  /**
   * The message queue associated with the HtmlEmbeddedContentFinder that
   * creates this instance will receive a message if fetching external content
   * failed.
   * In this case, content will be returned that is semantically equivalent,
   * such as code to raise a JS exception to trigger <tt>onerror</tt>
   * handlers.
   */
  public CharProducer getContent(PluginEnvironment env) {
    return getter.apply(env);
  }
  /** Non null for remote content. */
  public ExternalReference getContentLocation() { return contentLocation; }
  public boolean isDeferred() { return deferred; }
  public Node getSource() { return source; }
  /**
   * Returns a parse tree node containing the content.  For content from
   * elements this does not include any information from modifying attributes
   * such as the <tt>media</tt> attribute on {@code <link>} and
   * {@code <style>} elements.
   * @param mq receives messages about parsing problems but not about
   *     content fetching.
   */
  public ParseTreeNode parse(PluginEnvironment env, MessageQueue mq)
      throws ParseException {
    if (type == null) { return null; }  // Malformed content
    CharProducer cp = getContent(env);
    FilePosition p = cp.filePositionForOffsets(cp.getOffset(), cp.getLimit());
    switch (type) {
      case JS: {
        Parser parser = finder.makeJsParser(cp, mq);
        if (parser.getTokenQueue().isEmpty()) { return new Block(p); }
        return parser.parse();
      }
      case CSS: {
        CssParser parser = finder.makeCssParser(cp, mq);
        if (source.getNodeType() == Node.ELEMENT_NODE) {
          if (parser.getTokenQueue().isEmpty()) {
            return new CssTree.StyleSheet(
                p, Collections.<CssTree.CssStatement>emptyList());
          }
          return parser.parseStyleSheet();
        } else {
          if (parser.getTokenQueue().isEmpty()) {
            return new CssTree.DeclarationGroup(
                p, Collections.<CssTree.Declaration>emptyList());
          }
          return parser.parseDeclarationGroup();
        }
      }
      default: throw new SomethingWidgyHappenedError(type.toString());
    }
  }
  /** Null for bad content. */
  public ContentType getType() { return type; }
}
