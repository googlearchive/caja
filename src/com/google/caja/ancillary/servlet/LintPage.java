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

import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.HtmlQuasiBuilder;
import com.google.caja.reporting.Message;
import com.google.caja.reporting.MessageLevel;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Join;
import com.google.caja.util.Lists;
import com.google.caja.util.Multimap;
import com.google.caja.util.Multimaps;
import com.google.caja.util.Strings;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

/**
 * A page that shows the output along with messages from the message queue.
 *
 * @author mikesamuel@gmail.com
 */
public class LintPage {
  private static final String LINT_PAGE_TEMPLATE = Resources.readRequired(
      IndexPage.class, "lint.quasi.html").toString();

  private static final DecimalFormat PCT = new DecimalFormat(
      "#,##0.0%", new DecimalFormatSymbols(Locale.ENGLISH));

  private static final List<MessageLevel> LEVELS
      = Arrays.asList(MessageLevel.values());
  static { Collections.reverse(LEVELS); }

  static DocumentFragment render(Content content, Request req, MessageQueue mq)
      throws IOException {
    String contentText = content.getText();

    Document doc = DomParser.makeDocument(null, null);
    HtmlQuasiBuilder b = HtmlQuasiBuilder.getBuilder(doc);
    DocumentFragment messages = Reporter.messagesToFragment(mq, req, b);
    Multimap<MessageLevel, Message> byLevel = Multimaps.newListHashMultimap();
    for (Message msg : mq.getMessages()) {
      byLevel.put(msg.getMessageLevel(), msg);
    }
    List<String> summaryParts = Lists.newArrayList();
    for (MessageLevel lvl : LEVELS) {
      int count = byLevel.get(lvl).size();
      if (count != 0) {
        // CRITICAL_WARNING -> CRITICAL WARNING
        String name = lvl.name().replace('_', ' ');
        // ERROR -> Error
        name = name.charAt(0) + Strings.toLowerCase(name.substring(1));
        summaryParts.add(count + " " + name + (count != 1 ? "s" : ""));
      }
    }
    String messageSummary = summaryParts.isEmpty()
        ? "No Problems" : Join.join(", ", summaryParts);

    mq.getMessages().clear();  // Now they're included in the output.

    int inputSize = -1;
    if (req.minify) {
      inputSize = 0;
      for (Input inp : req.inputs) {
        inputSize += inp.code.length();
      }
    }

    Object stats = "";
    if (inputSize != -1) {
      int outSize = contentText.length();
      stats = b.substV(
          "(@insize B &rarr; @outsize B; output is @change  of the original)",
          "insize", "" + inputSize,
          "outsize", "" + outSize,
          "change", PCT.format(outSize / ((double) inputSize)));
    }

    DocumentFragment f = doc.createDocumentFragment();
    f.appendChild(b.substV(
        LINT_PAGE_TEMPLATE,
        "messages", messages,
        "code", contentText,
        "class", contentText.length() < 2048 ? "prettyprint" : "",
        "messageSummary", messageSummary,
        "stats", stats,
        "cid", req.staticFiles.cacheId));
    return f;
  }
}
