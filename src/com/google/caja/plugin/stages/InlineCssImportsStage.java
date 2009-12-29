// Copyright (C) 2008 Google Inc.
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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.CssTokenType;
import com.google.caja.lexer.ExternalReference;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.lexer.TokenQueue;
import com.google.caja.lexer.escaping.UriUtil;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.css.CssParser;
import com.google.caja.parser.css.CssTree;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.PluginEnvironment;
import com.google.caja.plugin.PluginMessageType;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.ContentType;
import com.google.caja.util.Name;
import com.google.caja.util.Pipeline;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Identify CSS imports and inline them per
 * <a href="http://www.w3.org/TR/CSS21/cascade.html#at-import"
 * >CSS2.1 rules</a>:<blockquote>
 *    The '&#64;import' rule allows users to import style rules from other
 *    style sheets. Any &#64;import rules must precede all other rules
 *    (except the &#64;charset rule, if present). The '&#64;import' keyword
 *    must be followed by the URI of the style sheet to include. A
 *    string is also allowed; it will be interpreted as if it had
 *    url(...) around it.
 *    <p>
 *    Example(s):
 *    <p>
 *    The following lines are equivalent in meaning and illustrate
 *    both '&#64;import' syntaxes (one with "url()" and one with a bare
 *    string):
 *    <pre>
 *    &#64;import "mystyle.css";
 *    &#64;import url("mystyle.css");
 *    </pre>
 *    So that user agents can avoid retrieving resources for
 *    unsupported media types, authors may specify media-dependent
 *    &#64;import rules. These conditional imports specify comma-separated
 *    media types after the URI.
 *    <p>
 *    Example(s):
 *    <p>
 *    The following rules illustrate how &#64;import rules can be made
 *    media-dependent:
 *    <pre>
 *    &#64;import url("fineprint.css") print;
 *    &#64;import url("bluish.css") projection, tv;
 *    </pre>
 *    In the absence of any media types, the import is
 *    unconditional. Specifying 'all' for the medium has the same
 *    effect.
 * </blockquote>
 *
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class InlineCssImportsStage implements Pipeline.Stage<Jobs> {
  public boolean apply(Jobs jobs) {
    for (Job job : jobs.getJobsByType(ContentType.CSS)) {
      inlineImports(job.getRoot().cast(CssTree.StyleSheet.class).node,
                    job.getBaseUri(),
                    MAXIMUM_IMPORT_DEPTH,
                    jobs.getPluginMeta().getPluginEnvironment(),
                    jobs.getMessageQueue());
    }
    return jobs.hasNoErrors();
  }

  /** Avoid cycles among imported style-sheets by limiting import depth. */
  private static final int MAXIMUM_IMPORT_DEPTH = 10;

  /** Inline imports at the beginning of ss. */
  private static void inlineImports(
      CssTree.StyleSheet ss, URI baseUri, int depth, PluginEnvironment env,
      MessageQueue mq) {
    MutableParseTreeNode.Mutation mut = ss.createMutation();
    for (CssTree t : ss.children()) {
      if (!(t instanceof CssTree.Import)) { break; }
      CssTree.Import importNode = (CssTree.Import) t;
      if (depth == 0) {
        mq.addMessage(
            PluginMessageType.CYCLIC_INCLUDE,
            importNode.getFilePosition(),
            MessagePart.Factory.valueOf(importNode.getUri().getValue()));
        return;
      }
      try {
        inlineImport(importNode, baseUri, depth, env, mq, mut);
      } catch (ParseException ex) {
        ex.toMessageQueue(mq);
      }
    }
    mut.execute();
  }

  /**
   * @param importNode specifies the URI to import, and the file position to
   *     resolve that URI relative to.
   * @param env used to resolve the URI.
   * @param mq receives error messages
   * @param mut mutation that receives changes that replace importNode with the
   *     content of the URI.
   */
  private static void inlineImport(
      CssTree.Import importNode, URI baseUri, int depth, PluginEnvironment env,
      MessageQueue mq, MutableParseTreeNode.Mutation mut)
      throws ParseException {
    CssTree.UriLiteral uriNode = importNode.getUri();
    // Compute the URI to import
    ExternalReference importUrl = null;
    URI absUri = UriUtil.resolve(baseUri, uriNode.getValue());
    if (absUri != null) {
      importUrl = new ExternalReference(absUri, uriNode.getFilePosition());
    }
    if (importUrl == null) {
      mq.addMessage(
          PluginMessageType.MALFORMED_URL,
          uriNode.getFilePosition(),
          MessagePart.Factory.valueOf(uriNode.getValue()));
      return;
    }

    // Import it and recursively import its imports
    InputSource is = new InputSource(importUrl.getUri());
    CharProducer cp = env.loadExternalResource(importUrl, "text/css");
    if (cp == null) {
      mq.addMessage(PluginMessageType.FAILED_TO_LOAD_EXTERNAL_URL,
                    MessageLevel.ERROR, importUrl.getReferencePosition(), is);
      return;
    }
    CssTree.StyleSheet importedSs = parseCss(cp, mq);
    inlineImports(importedSs, importUrl.getUri(), depth - 1, env, mq);

    // Create a set of blocks to import by taking the union of media types on
    // the import block and the media blocks in the style-sheet.
    List<CssTree.Medium> media = importNode.getMedia();
    if (!media.isEmpty()) {
      Set<Name> mediaTypes = toMediaTypeSet(media);
      if (!mediaTypes.contains(Name.css("all"))) {
        restrictToMediaTypes(importedSs, mediaTypes);
      }
    }
    for (CssTree t : importedSs.children()) {
      mut.insertBefore(t, importNode);
    }
    mut.removeChild(importNode);
  }

  /**
   * Makes sure content of ss only applies to the given media types.
   * @param mediaTypes per
   *   <a href="http://www.w3.org/TR/CSS2/media.html#media-types"
   *    >CSS media types</a>.
   */
  private static void restrictToMediaTypes(
      CssTree.StyleSheet ss, Set<Name> mediaTypes) {
    MutableParseTreeNode.Mutation mut = ss.createMutation();
    int nonMedia = 0;
    int n = ss.children().size();
    for (int i = 0; i < n; ++i) {
      CssTree child = ss.children().get(i);
      if (child instanceof CssTree.Media) {
        CssTree.Media media = (CssTree.Media) child;
        // Take a chunk and of non @media blocks and put them in an @media block
        wrapInMediaBlock(ss.children().subList(nonMedia, i), mediaTypes, mut);
        nonMedia = i + 1;

        // Remove any medium nodes that aren't in mediaTypes.
        MutableParseTreeNode.Mutation mediaMut = media.createMutation();
        boolean oneAllowed = false;
        List<CssTree.Medium> mediaNodes = media.getMedia();
        if (toMediaTypeSet(mediaNodes).contains(Name.css("all"))) {
          oneAllowed = true;
          CssTree.Medium medium0 = mediaNodes.get(0);
          FilePosition pos = medium0.getFilePosition();
          for (Name mediaType : mediaTypes) {
            mediaMut.insertBefore(
                new CssTree.Medium(pos, mediaType), medium0);
          }
          for (CssTree.Medium medium : mediaNodes) {
            mediaMut.removeChild(medium);
          }
        } else {
          for (CssTree.Medium medium : mediaNodes) {
            if (!mediaTypes.contains(medium.getValue())) {
              mediaMut.removeChild(medium);
            } else {
              oneAllowed = true;
            }
          }
        }
        if (!oneAllowed) {
          mut.removeChild(media);
        } else {
          mediaMut.execute();
        }
      }
    }
    wrapInMediaBlock(ss.children().subList(nonMedia, n), mediaTypes, mut);
    mut.execute();
  }

  /**
   * Wraps the given text in
   * <code>&#64;media &lt;mediaTypes&gt; { &lt;nodes&gt; }</code>.
   */
  private static void wrapInMediaBlock(
      List<? extends CssTree> nodes, Set<Name> mediaTypes,
      MutableParseTreeNode.Mutation mut) {
    if (nodes.isEmpty()) { return; }
    List<CssTree> mediaBlockChildren = new ArrayList<CssTree>();
    FilePosition pos = FilePosition.startOf(nodes.get(0).getFilePosition());
    for (Name mediaType : mediaTypes) {
      mediaBlockChildren.add(new CssTree.Medium(pos, mediaType));
    }
    mediaBlockChildren.addAll(nodes);
    CssTree.Media wrappedBlock = new CssTree.Media(pos, mediaBlockChildren);

    mut.insertBefore(wrappedBlock, nodes.get(0));
    for (CssTree node : nodes) { mut.removeChild(node); }
  }

  private static CssTree.StyleSheet parseCss(CharProducer cp, MessageQueue mq)
      throws ParseException {
    TokenQueue<CssTokenType> tq = CssParser.makeTokenQueue(cp, mq, false);
    CssParser p = new CssParser(tq, mq, MessageLevel.WARNING);
    return p.parseStyleSheet();
  }

  private static Set<Name> toMediaTypeSet(List<CssTree.Medium> media) {
    Set<Name> mediaTypes = new LinkedHashSet<Name>();
    for (CssTree.Medium medium : media) {
      mediaTypes.add(medium.getValue());
    }
    return mediaTypes;
  }
}
