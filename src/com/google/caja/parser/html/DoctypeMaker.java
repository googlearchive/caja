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

package com.google.caja.parser.html;

import com.google.caja.util.Function;
import com.google.caja.util.Strings;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentType;

class DoctypeMaker {

  public static Function<DOMImplementation, DocumentType> parse(String text) {
    // We recognize a subset of the XML DOCTYPE grammar.  Specifically, we
    // do not recognize embedded entity declarations to avoid XXE, or
    // annotations.

    // As noted above, we do not recognize the intSubset portion.
    Matcher m = DOCTYPE_PATTERN.matcher(text);
    if (!m.matches()) { return null; }

    String name = m.group(1), system2 = dequote(m.group(2)),
        pubid = dequote(m.group(3)), system4 = dequote(m.group(4));
    final String system = system2 == null ? system4 : system2;
    boolean isHtml = isHtml(name, pubid, system);
    if (isHtml && name.indexOf(':') < 0) {
      name = Strings.lower(name);
    }
    final String qname = name;
    final String publicId = pubid;
    final String systemId = system;
    return new Function<DOMImplementation, DocumentType>() {
      public DocumentType apply(DOMImplementation impl) {
        return impl.createDocumentType(qname, publicId, systemId);
      }
    };
  }

  /**
   * This implementation is based on the grammar in the
   * <a href="http://www.w3.org/TR/REC-xml/#NT-doctypedecl">XML spec S 2.8</a>
   */
  private static final Pattern DOCTYPE_PATTERN;
  static {
    // S             ::=  (#x20 | #x9 | #xD | #xA)+
    String s = "[ \\t\\r\\n]+";
    String sStar = "[ \\t\\r\\n]*";
    // NameStartChar ::=  ":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6]
    //                 |  [#xF8-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF]
    //                 |  [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF]
    //                 |  [#x3001-#xD7FF] | [#xF900-#xFDCF] | [#xFDF0-#xFFFD]
    //                 |  [#x10000-#xEFFFF]
    String nameStartCharSet = (
        "A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F"
        + "\u1FFF\u200C\u200D\u2070-\u218F\u2C00\u2FEF\u3001\uD7FF\uF900-\uFDCF"
        + "\uFDF0-\uFFFD");
    String nameStartChar = "[" + nameStartCharSet + "]";
    // NameChar      ::=  NameStartChar | "-" | "." | [0-9] | #xB7
    //                 |  [#x0300-#x036F] | [#x203F-#x2040]
    String nameChar = (
        "[" + nameStartCharSet + "\\-.0-9\u0087\u0300-\u036F\u203F-\u2040]");
    // Name          ::=  NameStartChar (NameChar)*
    String name = "(?:" + nameStartChar + nameChar + "*)";
    // SystemLiteral ::=  ('"' [^"]* '"') | ("'" [^']* "'")
    String systemLiteral = "(?:\"[^\"]*\"|'[^']*')";
    // PubidChar     ::=  #x20 | #xD | #xA | [a-zA-Z0-9] | [-'()+,./:=?;!*#@$_%]
    String pubidChar = "[ \\r\\na-zA-Z0-9\\-'()+,./:=?;!*#$_%]";
    // PubidLiteral  ::=  '"' PubidChar* '"' | "'" (PubidChar - "'")* "'"
    String pubidLiteral = (
        "(?:\"" + pubidChar + "*\"|\'" + pubidChar.replace("'", "\"") + "*')");
    // ExternalID    ::=  'SYSTEM' S  SystemLiteral
    //                 |  'PUBLEIC' S PubidLiteral S SystemLiteral
    String externalId = (
        "(?:SYSTEM" + s + "(" + systemLiteral + ")"
        + "|PUBLIC" + s + "("+ pubidLiteral + ")"
        // XML does not allow the system id to be omitted, but HTML does.
        // Also, whitespaces between public id and system id can be omitted.
        + "(?:" + sStar + "(" + systemLiteral + "))?)");
    String intSubset = "[^\\]>]*";
    // '<!DOCTYPE' S  Name (S  ExternalID)? S? ('[' intSubset ']' S?)? '>'
    // Groups: Name 1, SystemLiteral 2 or 4, PubidLiteral 3.
    DOCTYPE_PATTERN = Pattern.compile(
        "<!DOCTYPE" + s + "(" + name + ")(?:" + s + externalId + ")?"
        + "(?:" + s + ")?(?:\\[" + intSubset + "\\](?:" + s + ")?)?>",
        Pattern.CASE_INSENSITIVE);
  }

  private static final Map<String, String> BY_SYSTEM_ID
      = new ImmutableMap.Builder<String, String>()
      .put("http://www.w3.org/TR/html4/*.dtd", Namespaces.HTML_NAMESPACE_URI)
      .put("http://www.w3.org/TR/xhtml1/DTD/*.dtd",
           Namespaces.HTML_NAMESPACE_URI)
      .put("http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/*.dtd",
          Namespaces.SVG_NAMESPACE_URI)
      .put("http://www.w3.org/Graphics/SVG/1.1/DTD/*.dtd",
           Namespaces.SVG_NAMESPACE_URI)
      .build();

  public static String systemIdToNsUri(String systemId) {
    String nsUri = BY_SYSTEM_ID.get(systemId);
    if (nsUri == null && systemId != null) {
      String wildcard = systemId.replaceFirst("/[^/]+\\.dtd$", "/*.dtd");
      nsUri = BY_SYSTEM_ID.get(wildcard);
    }
    return nsUri;
  }

  private static String dequote(String s) {
    if (s == null) { return s; }
    int len = s.length();
    if (len < 2) { return s; }
    char ch0 = s.charAt(0);
    if (ch0 != '"' && ch0 != '\'') { return s; }
    if (ch0 != s.charAt(len - 1)) { return s; }
    return s.substring(1, len - 1);
  }

  static boolean isHtml(String name, String pubid, String systemId) {
    String nsUri = systemIdToNsUri(systemId);
    if (nsUri != null && Namespaces.isHtml(nsUri)) { return true; }
    if (pubid != null) {
      pubid = Strings.lower(pubid).replaceAll("\\s+", " ").trim();
      return pubid.startsWith("-//w3c//dtd html ")
          || pubid.startsWith("-//w3c//dtd xhtml ")
          || pubid.startsWith("-//ietf//dtd html");
    } else if (systemId == null) {
      // <!DOCTYPE html>
      return Strings.eqIgnoreCase("html", name);
    }
    return false;
  }
}
