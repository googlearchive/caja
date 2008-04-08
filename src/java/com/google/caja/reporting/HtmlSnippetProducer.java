
package com.google.caja.reporting;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.escaping.Escaping;
import java.io.IOException;
import java.util.Map;

public class HtmlSnippetProducer extends SnippetProducer {

  public HtmlSnippetProducer(
       Map<InputSource, ? extends CharSequence> originalSrc,
       MessageContext mc) {
    super(originalSrc, mc);
  }

  // Override the default behavior to create messages that link nicely.
  @Override
  protected void formatSnippet(
      FilePosition pos, CharSequence line, int start, int end,
      Appendable out)
      throws IOException {
    formatFilePosition(pos, out);
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

    out.append("<a href=# class=\"filepos\" onclick=\"selectLine(")
      .append(html(js(pos.source().getUri().toString())))
      .append(",")
      .append(String.valueOf(pos.startLineNo()))
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
