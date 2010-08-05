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

package com.google.caja.reporting;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.escaping.Escaping;
import java.io.IOException;
import java.util.Map;

/**
 * A snippet producer that adds span tags around file positioned text instead
 * of using <tt>^^^</tt> style markers like the default snippet producer.
 *
 * <p>The containing page should define a {@code .problem} style rule and
 * a javascript function
 * {@code selectLine(uri, startLine, startOffset, endLine, endOffset)}.</p>
 *
 * @author mikesamuel@gmail.com
 */
public class HtmlSnippetProducer extends SnippetProducer {

  public HtmlSnippetProducer(
       Map<InputSource, ? extends CharSequence> originalSrc,
       MessageContext mc) {
    super(originalSrc, mc);
  }

  // Override the default behavior to create messages that link nicely.
  @Override
  protected void formatSnippet(FilePosition errorPosition,
      FilePosition snippetPos, CharSequence line, int start, int end,
      Appendable out)
      throws IOException {
    formatFilePosition(errorPosition, out);
    out.append(": ")
      .append(html(line.subSequence(0, start)))
      .append("<span class=\"problem\">")
      .append(html(line.subSequence(start, end)))
      .append("</span>")
      .append(html(line.subSequence(end, line.length())));
  }

  @Override
  protected void formatFilePosition(FilePosition pos, Appendable out)
      throws IOException {
    StringBuilder filename = new StringBuilder();
    pos.source().format(mc, filename);

    out.append("<a href=\"#\" class=\"filepos nocode\" onclick=\"selectLine(")
      .append(html(js(pos.source().getUri().toString())))
      .append(",")
      .append(String.valueOf(pos.startLineNo()))
      .append(",")
      .append(String.valueOf(pos.startCharInLine()))
      .append(",")
      .append(String.valueOf(pos.endLineNo()))
      .append(",")
      .append(String.valueOf(pos.endCharInLine()))
      .append(")\">")
      .append(html(filename))
      .append(":")
      .append(String.valueOf(pos.startLineNo()))
      .append("</a>");
  }

  private static String html(CharSequence s) {
    StringBuilder sb = new StringBuilder();
    Escaping.escapeXml(s, false, sb);
    return sb.toString();
  }

  private static String js(CharSequence s) {
    StringBuilder sb = new StringBuilder();
    sb.append('\'');
    Escaping.escapeJsString(s, false, true, sb);
    sb.append('\'');
    return sb.toString();
  }

}
