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

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.parser.html.HtmlQuasiBuilder;
import com.google.caja.parser.html.Namespaces;
import com.google.caja.parser.html.Nodes;
import com.google.caja.reporting.HtmlSnippetProducer;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessagePart;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;

import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Produces HTML from a message queue.
 *
 * @author mikesamuel@gmail.com
 */
final class Reporter {
  static DocumentFragment messagesToFragment(
      MessageQueue mq, Request req, HtmlQuasiBuilder b) {
    HtmlSnippetProducer sr = new HtmlSnippetProducer(req.srcMap, req.mc);
    DocumentFragment f = b.getDocument().createDocumentFragment();
    List<Message> messages = Lists.newArrayList(mq.getMessages());
    Collections.sort(messages, MESSAGE_COMPARATOR);
    for (Message msg : messages) {
      if (msg.getMessageLevel().compareTo(req.minLevel) < 0
          || req.toIgnore.contains(msg.getMessageType().name())) {
        continue;
      }
      Node help = null;
      String helpFile = msg.getMessageType().name() + "_tip.html";
      if (req.staticFiles.exists("files/" + helpFile)) {
        help = b.substV(
            // U+FF1F is a full-width question mark which makes for a good icon.
            " <a class=help href=files-@cid/@helpPath target=help>&#xff1f;</a>",
            "helpPath", helpFile,
            "cid", req.staticFiles.cacheId);
      }
      String snippetHtml = sr.getSnippet(msg);
      Element snippet = null;
      if (snippetHtml != null) {  // TODO: don't do this.
        try {
          DocumentFragment snippetF = b.toFragment(snippetHtml);
          snippet = b.getDocument().createElementNS(
              Namespaces.HTML_NAMESPACE_URI, "pre");
          snippet.setAttributeNS(  // See third_party/js/prettify
              Namespaces.HTML_NAMESPACE_URI, "class", "prettyprint snippet");
          for (Node snippetPart : Nodes.childrenOf(snippetF)) {
            snippet.appendChild(snippetPart);
          }
        } catch (ParseException ex) {
          ex.printStackTrace();
        }
      }
      f.appendChild(b.substV(
          "<h2 title=@msgname>@text @help</h2>@snippet",
          // Output the message name so that people can weed out message types
          // using the ign CGI parameter.
          "msgname", msg.getMessageType().name(),
          "text", msg.getMessageLevel().name() + ": " + msg.format(req.mc),
          "help", help != null ? help : "",
          "snippet", snippet != null ? snippet : ""));
    }
    return f;
  }

  private static final Comparator<Message> MESSAGE_COMPARATOR
      = new Comparator<Message>() {
    public int compare(Message a, Message b) {
      int delta = compare(firstPart(a), firstPart(b));
      if (delta == 0) {
        delta = a.getMessageType().name().compareTo(b.getMessageType().name());
      }
      return delta;
    }

    private MessagePart firstPart(Message m) {
      List<? extends MessagePart> parts = m.getMessageParts();
      if (parts.isEmpty()) { return null; }
      return parts.get(0);
    }

    private int compare(MessagePart a, MessagePart b) {
      if (a == null) { return b == null ? 0 : -1; }
      if (b == null) { return 1; }
      URI aUri = null, bUri = null;
      int aStart = -1, bStart = -1;
      int aEnd = -1, bEnd = -1;
      if (a instanceof FilePosition) {
        FilePosition p = (FilePosition) a;
        aUri = p.source().getUri();
        aStart = p.startCharInFile();
        aEnd = p.endCharInFile();
      } else if (a instanceof InputSource) {
        aUri = ((InputSource) a).getUri();
      }
      if (b instanceof FilePosition) {
        FilePosition p = (FilePosition) b;
        bUri = p.source().getUri();
        bStart = p.startCharInFile();
        bEnd = p.endCharInFile();
      } else if (b instanceof InputSource) {
        bUri = ((InputSource) b).getUri();
      }
      int delta = aUri != null ? bUri != null ? aUri.compareTo(bUri) : -1
           : bUri != null ? 1 : 0;
      if (delta == 0) {
        delta = Long.signum(((long) aStart) - bStart);
        if (delta == 0) {
          delta = Long.signum(((long) aEnd) - bEnd);
        }
      }
      return delta;
    }
  };
}
