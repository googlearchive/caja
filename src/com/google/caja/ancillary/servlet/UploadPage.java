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
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.html.DomParser;
import com.google.caja.parser.html.HtmlQuasiBuilder;
import com.google.caja.parser.html.Nodes;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.quasiliteral.QuasiBuilder;
import com.google.caja.render.Concatenator;
import com.google.caja.render.JsMinimalPrinter;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Lists;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Invoked from a file upload form in <tt>files/upload.html</tt> to upload
 * a source code file.  This calls back into {@code uploaded} defined in
 * <tt>files/index.js</tt> with a JSON object containing the uploaded files
 * so that they can be folded into the parent frame's form.
 *
 * @author mikesamuel@gmail.com
 */
final class UploadPage {
  static void doUpload(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // Process the uploaded items
    List<ObjectConstructor> uploads = Lists.newArrayList();

    if (ServletFileUpload.isMultipartContent(req)) {
      // Create a factory for disk-based file items
      DiskFileItemFactory factory = new DiskFileItemFactory();

      int maxUploadSizeBytes = 1 << 18;
      factory.setSizeThreshold(maxUploadSizeBytes);  // In-memory threshold
      factory.setRepository(new File("/dev/null"));  // Do not store on disk
      ServletFileUpload upload = new ServletFileUpload(factory);
      upload.setSizeMax(maxUploadSizeBytes);

      writeHeader(resp);

      // Parse the request
      List<?> items;
      try {
        items = upload.parseRequest(req);
      } catch (FileUploadException ex) {
        ex.printStackTrace();
        resp.getWriter().write(Nodes.encode(ex.getMessage()));
        return;
      }

      for (Object fileItemObj : items) {
        FileItem item = (FileItem) fileItemObj;  // Written for pre-generic java.
        if (!item.isFormField()) {  // Then is a file
          FilePosition unk = FilePosition.UNKNOWN;
          String ct = item.getContentType();
          uploads.add((ObjectConstructor) QuasiBuilder.substV(
              "({ i: @i, ip: @ip, it: @it? })",
              "i", StringLiteral.valueOf(unk, item.getString()),
              "ip", StringLiteral.valueOf(unk, item.getName()),
              "it", ct != null ? StringLiteral.valueOf(unk, ct) : null));
        }
      }
    } else if (req.getParameter("url") != null) {
      List<URI> toFetch = Lists.newArrayList();
      boolean failed = false;
      for (String value : req.getParameterValues("url")) {
        try {
          toFetch.add(new URI(value));
        } catch (URISyntaxException ex) {
          if (!failed) {
            failed = true;
            resp.setStatus(500);
            resp.setContentType("text/html;charset=UTF-8");
          }
          resp.getWriter().write(
              "<p>Bad URI: " + Nodes.encode(ex.getMessage()));
        }
      }
      if (failed) { return; }
      writeHeader(resp);
      FilePosition unk = FilePosition.UNKNOWN;
      for (URI uri : toFetch) {
        try {
          Content c = UriFetcher.fetch(uri);
          if (c.isText()) {
            uploads.add((ObjectConstructor) QuasiBuilder.substV(
                "({ i: @i, ip: @ip, it: @it? })",
                "i", StringLiteral.valueOf(unk, c.getText()),
                "ip", StringLiteral.valueOf(unk, uri.toString()),
                "it", StringLiteral.valueOf(unk, c.type.mimeType)));
          }
        } catch (IOException ex) {
          resp.getWriter().write(
              "<p>" + Nodes.encode(
                  "Failed to fetch URI: " + uri + " : " + ex.getMessage()));
        }
      }
    } else {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.getWriter().write("Content not multipart");
      return;
    }

    Expression notifyParent = (Expression) QuasiBuilder.substV(
        "window.parent.uploaded([@uploads*], window.name)",
        "uploads", new ParseTreeNodeContainer(uploads));
    StringBuilder jsBuf = new StringBuilder();
    RenderContext rc = new RenderContext(
        new JsMinimalPrinter(new Concatenator(jsBuf))).withEmbeddable(true);
    notifyParent.render(rc);
    rc.getOut().noMoreTokens();

    HtmlQuasiBuilder b = HtmlQuasiBuilder.getBuilder(
        DomParser.makeDocument(null, null));

    Writer out = resp.getWriter();
    out.write(Nodes.render(b.substV(
        "<script>@js</script>", "js", jsBuf.toString())));
  }

  private static void writeHeader(HttpServletResponse resp) throws IOException {
    resp.setStatus(200);
    resp.setContentType("text/html;charset=UTF-8");
    Writer out = resp.getWriter();
    out.write("<h1>Loading&hellip;</h1>");
    out.flush();
  }
}
