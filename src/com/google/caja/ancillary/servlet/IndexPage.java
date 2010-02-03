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
import com.google.caja.reporting.MessageLevel;
import com.google.caja.util.ContentType;
import com.google.caja.util.Join;
import com.google.caja.util.Lists;
import com.google.caja.util.Sets;

import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

/**
 * The main servlet page that presents a web form to let developers
 * interactively check their code.
 *
 * @author mikesamuel@gmail.com
 * @see Verb#INDEX
 */
final class IndexPage {
  private static final String INDEX_PAGE_TEMPLATE = Resources.readOptimized(
      IndexPage.class, "index.quasi.html").toString();

  static final String DEFAULT_SOURCE_INPUT = "<script></script>";

  static DocumentFragment render(Request req) {
    Document doc = DomParser.makeDocument(null, null);
    HtmlQuasiBuilder b = HtmlQuasiBuilder.getBuilder(doc);

    DocumentFragment inputsF = doc.createDocumentFragment();
    List<Input> inputs = Lists.newArrayList(req.inputs);
    if (inputs.isEmpty()) {
      inputs.add(new Input(null, null, DEFAULT_SOURCE_INPUT));
    }
    for (Input inp : inputs) {
      inputsF.appendChild(b.substV(
          Join.join(
              "\n",
              "<li>",
              // Order is significant for these inputs.  See Request.java
              // handling of "it", "ip", and "i" inputs for more details.
              "<select class=type-select name=it title=Content-Type>",
              "<option value=''></option>",
              "<option value=@jsT selected=@isJS>JavaScript</option>",
              "<option value=@cssT selected=@isCSS>CSS</option>",
              "<option value=@htmlT selected=@isHTML>HTML</option>",
              "</select>",
              "<input type=hidden name=ip value=@file>",
              "<div class=source-code-cont>",
              "<textarea name=i class=source-code-input>@code</textarea>",
              "</div>",
              "<span class=cursor-pos></span>",
              "<br clear=all>",
              "</li>"),
          "code", inp.code,
          "file", inp.path != null ? inp.path : "",
          "it", inp.t != null ? inp.t.mimeType : "",
          "jsT", ContentType.JS.mimeType,
          "isJS", inp.t == ContentType.JS,
          "cssT", ContentType.CSS.mimeType,
          "isCSS", inp.t == ContentType.CSS,
          "htmlT", ContentType.HTML.mimeType,
          "isHTML", inp.t == ContentType.HTML
          ));
    }

    DocumentFragment messageLevels = doc.createDocumentFragment();
    for (MessageLevel lvl : MessageLevel.values()) {
      messageLevels.appendChild(b.substV(
          "<option value=@lvl selected=@selected>@lvl</option>",
          "lvl", lvl.name(),
          "selected", req.minLevel == lvl));
    }

    boolean isLint, isEcho, isDoc;
    isLint = req.verb == Verb.LINT || req.verb == Verb.INDEX;
    isDoc = req.verb == Verb.DOC;
    isEcho = !(isLint || isDoc);

    DocumentFragment f = doc.createDocumentFragment();
    f.appendChild(b.substV(
        INDEX_PAGE_TEMPLATE,
        "inputs", inputsF,
        "messageLevels", messageLevels,
        "lint", isLint,
        "echo", isEcho,
        "doc", isDoc,
        "ignores", Join.join(" ", Sets.newTreeSet(req.toIgnore)),
        "baseUri", req.baseUri.toString(),
        "minifyTrue", req.minify,
        "minifyFalse", !req.minify,
        "asciiOnly", req.asciiOnly,
        "userAgent", req.userAgent != null ? req.userAgent : "*",
        "cacheId", req.staticFiles.cacheId));
    return f;
  }
}
