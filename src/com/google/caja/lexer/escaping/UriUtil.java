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

package com.google.caja.lexer.escaping;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.util.Join;
import com.google.caja.util.Strings;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for dealing with URIs.
 *
 * @author mikesamuel@gmail.com
 */
public class UriUtil {

  /** Matches a URI extracting bits with different escaping conventions. */
  private static final Pattern RFC_3986 = Pattern.compile(
      // Derived from RFC 3986 Appendix B
      //   1                 2          3             4            5
      "^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?$",
      Pattern.DOTALL);

  /**
   * Convert a URI to a string, %xx escaping some codepoints that are in the
   * RFC3986 reserved set, but not in contexts where they are significant.
   * This works around problems with inconsistencies in escaping conventions
   * in CSS URIs, but still allows us to make sure that URIs don't look like
   * code that a badly written error recovery routine might jump into.
   * <p>
   * This escaping of codepoints is allowed by section 2.4.2 of RFC 2396:
   * <blockquote>
   *   In some cases, data that could be represented by an unreserved
   *   character may appear escaped; for example, some of the unreserved
   *   "mark" characters are automatically escaped by some systems.  If the
   *   given URI scheme defines a canonicalization algorithm, then
   *   unreserved characters may be unescaped according to that algorithm.
   *   For example, "%7e" is sometimes used instead of "~" in an http URL
   *   path, but the two are equivalent for an http URL.
   * </blockquote>
   *
   * @param uri a non-opaque URI.
   */
  public static String normalizeUri(String uri) throws URISyntaxException {
    uri = normalizeSpecialCharacters(uri);

    // We don't use java.net.URI to recompose the URI because of problems with
    // encoding in the multi-argument constructor as described at
    // http://blog.limewire.org/?p=261:
    //     And it seems that the multi-arg constructors, which do URL encoding
    //     for you, do NOT provide a way for you to encode these characters -
    //     which means you can only ever use them for their reserved (unescaped)
    //     purpose.
    //     For example, suppose I want to produce this URL:
    //       http://foo.com/bar?a=b&c=jon%26doe
    //       uri = new URI("http", null, "foo.com", -1, "/bar",
    //                     "a=b&c=jon%26doe", null);
    //       uri.toASCIIString() -> http://foo.com/bar?a=b&c=jon%2526doe
    //     ...
    //     The upshot of all of this is that I claim the multi-arg constructors
    //     are unusable, unless you restrict your URLs to never use reserved
    //     characters as values. In our use case, we can't do that because we
    //     don't control what URIs are incoming / outgoing.
    Matcher m = RFC_3986.matcher(uri);
    // The RFC_2396 matches all strings.
    m.matches();
    String scheme = m.group(1);
    String authority = m.group(2);
    String path = m.group(3);
    String query = m.group(4);
    String fragment = m.group(5);

    // Path must start with / if the path is not empty or there is an authority
    // or scheme.
    // Remove unnecessary .. components in path.

    StringBuilder sb = new StringBuilder(uri.length());
    if (scheme != null) {
      normalizeScheme(scheme, sb);
      sb.append(':');
    }
    if (authority != null) {
      if ("".equals(authority) && !Strings.eqIgnoreCase("file", scheme)) {
        throw new URISyntaxException(uri, "Blank authority");
      }
      sb.append("//");
      normalizeAuthority(authority, sb);
    } else if (scheme != null
               && !(Strings.eqIgnoreCase("file", scheme)
                    || isOpaque(scheme))) {
      throw new URISyntaxException(uri, "Missing authority");
    }
    if (path.length() != 0 || sb.length() != 0) {
      normalizePath(path, sb.length() != 0 && !isOpaque(scheme), sb);
    }
    if (query != null) {
      sb.append('?');
      normalizeQuery(query, sb);
    }
    if (fragment != null) {
      sb.append('#');
      normalizeFragment(fragment, sb);
    }
    return sb.toString();
  }

  /**
   * Browsers typically treat non-Latin variants of URI special characters as
   * special so that Chinese users can copy and paste URLs rendered using
   * full-width characters.
   */
  private static String normalizeSpecialCharacters(String uri) {
    StringBuilder sb = null;
    int pos = 0;
    int n = uri.length();
    for (int i = n; --i >= 0;) {
      char subst;
      // The mapping below was derived by running the below.
      // We want to use a standard list so that we don't miss code-points on
      // older Java versions.
      // Map<String, List<String>> m = Maps.newLinkedHashMap();
      // m.put(":", new ArrayList<String>());
      // m.put("/", new ArrayList<String>());
      // m.put("?", new ArrayList<String>());
      // m.put("#", new ArrayList<String>());
      // m.put("=", new ArrayList<String>());
      // m.put("&", new ArrayList<String>());
      // m.put(".", new ArrayList<String>());
      // StringBuilder sb = new StringBuilder();
      // for (int i = 0; i < Character.MAX_CODE_POINT; ++i) {
      //   sb.setLength(0);
      //   sb.appendCodePoint(i);
      //   String abnormal = sb.toString();
      //   String normal = Normalizer.normalize(abnormal, Normalizer.Form.NFKD);
      //   List<String> abnormalForms = m.get(normal);
      //   if (abnormalForms != null && !abnormal.equals(normal)) {
      //     abnormalForms.add(abnormal);
      //   }
      // }
      // for (Map.Entry<String, List<String>> e : m.entrySet()) {
      //   System.out.println(e.getKey());
      //   for (String s : e.getValue()) {
      //     System.out.println("\t0x" + Integer.toString(s.codePointAt(0), 16)
      //                        + " : " + s);
      //   }
      // }
      switch (uri.charAt(i)) {
        case 0xfe13: case 0xfe55: case 0xff1a:               subst = ':'; break;
        case 0xff0f:                                         subst = '/'; break;
        case 0xfe16: case 0xfe56: case 0xff1f:               subst = '?'; break;
        case 0xfe5f: case 0xff03:                            subst = '#'; break;
        case 0x207c: case 0x208c: case 0xfe66: case 0xff1d : subst = '='; break;
        case 0xfe60: case 0xff06:                            subst = '&'; break;
        case 0x2024: case 0xfe52: case 0xff0e:               subst = '.'; break;
        default: continue;
      }
      if (sb == null) { sb = new StringBuilder(n); }
      sb.append(uri, pos, i).append(subst);
      pos = i + 1;
    }
    return sb == null ? uri : sb.toString();
  }

  public static URI resolve(URI base, String relative)
      throws URISyntaxException {
    URI abs = base.resolve(normalizeUri(relative));
    if (!abs.isOpaque()) {
      String path = abs.getPath();
      // Workaround a bug in java.net.URI.
      // TODO(mikesamuel): stop using java.net.URI and use a decent URL
      // implementation instead.
      if (path != null && (path.startsWith("/../") || path.equals("/.."))) {
        return null;
      }
    }
    return abs;
  }

  public static String encode(String part) {
    for (int i = 0, n = part.length(); i < n; ++i) {
      char ch = part.charAt(i);
      if (ch >= 0x80 || Escaping.URI_ESCAPES.getEscape(ch) != null) {
        StringBuilder out = new StringBuilder(n + (n >> 2));
        Escaping.escapeUri(part, i, out);
        return out.toString();
      }
    }
    return part;
  }

  private static void normalizeScheme(String scheme, StringBuilder out) {
    // Section 3.1:
    // scheme        = alpha *( alpha | digit | "+" | "-" | "." )
    int pos = 0, n = scheme.length();
    for (int i = 0; i < n; ++i) {
      char ch = scheme.charAt(i);
      if (ch == '%') {
        if (isInvalidEsc(scheme, i)) {
          out.append(scheme, pos, i).append("%25");
          pos = i + 1;
        }
      } else if ('A' <= ch && ch <= 'Z') {
        // From Section 3.1.
        // Although schemes are case-insensitive, the canonical form is
        // lowercase and documents that specify schemes must do so with
        // lowercase letters.
        // ...
        // An implementation should accept uppercase letters as equivalent to
        // lowercase in scheme names (e.g., allow "HTTP" as well as "http") for
        // the sake of robustness but should only produce lowercase scheme names
        // for consistency.
        out.append(scheme, pos, i).append((char) (ch | 32));
        pos = i + 1;
      } else if (!(('a' <= ch && ch <= 'z')
                 || ('0' <= ch && ch <= '9')
                 || ch == '+' || ch == '-' || ch == '.')) {
        out.append(scheme, pos, i);
        pos = i + 1;
        pctEncode(ch, out);
      }
    }
    out.append(scheme, pos, n);
  }

  private static boolean isOpaque(String scheme) {
    return Strings.eqIgnoreCase("mailto", scheme)
        || Strings.eqIgnoreCase("javascript", scheme)
        || Strings.eqIgnoreCase("content", scheme)
        || Strings.eqIgnoreCase("data", scheme)
        || Strings.eqIgnoreCase("clsid", scheme);
  }

  private static void normalizeAuthority(String authority, StringBuilder out)
      throws URISyntaxException {
    // Section 3.2:
    // The authority component is preceded by a double slash "//" and is
    // terminated by the next slash "/", question-mark "?", or by the end of
    // the URI.  Within the authority component, the characters ";", ":",
    // "@", "?", and "/" are reserved.

    // The above quote from the RFC ignores the fact that '#' ends the authority
    // in URI references, but the pattern from Appendix B recognizes this.
    // We escape '@' since it is a significant character in CSS in @import
    // directives, as well as in conditional compilation comments.
    // This does not affect web applications in practice, since browsers
    // disallow '@' in authorities in HTTP and HTTPS since only a tiny number of
    // HTTP servers recognized it, but it was widely used by phishers.
    int pos = 0, n = authority.length();
    for (int i = 0; i < n; ++i) {
      char ch = authority.charAt(i);
      if (ch == '%') {
        if (isInvalidEsc(authority, i)) {
          out.append(authority, pos, i).append("%25");
          pos = i + 1;
        }
      } else if (ch == ':') {
        // We assume a subset of server-based URIs that only recognizes
        //     host[:port]
        // intentionally ignoring registry-based authorities and the user
        // portion of server-based URIs.
        for (int j = i + 1; j < n; ++j) {
          ch = authority.charAt(j);
          if (!('0' <= ch && ch <= '9')) {
            throw new URISyntaxException(
                authority, "Bad port " + authority.substring(i + 1), j);
          }
        }
        break;
      } else if (!(('a' <= ch && ch <= 'z')
                   || ('A' <= ch && ch <= 'Z')
                   || ('0' <= ch && ch <= '9')
                   // Escapes ; and @.
                   || ch == '-' || ch == '+' || ch == '.')) {
        out.append(authority, pos, i);
        pos = i + 1;
        pctEncode(ch, out);
      }
    }
    out.append(authority, pos, n);
  }

  private static void normalizePath(
      String path, boolean requireAbsPath, StringBuilder out) {
    String normPath = normalizeEscapesInPath(path);
    boolean isAbs = requireAbsPath;
    if (normPath.startsWith("/")) {
      normPath = normPath.substring(1);
      isAbs = true;
    }
    List<String> pathParts = Lists.newArrayList(normPath.split("/"));
    int i = 0;
    while (i < pathParts.size()) {
      String dottedPart = pathParts.get(i).replace("%2e", ".")
          .replace("%2E", ".");
      if (".".equals(dottedPart)) {
        pathParts.remove(i);
      } else if ("..".equals(dottedPart)) {
        if (i > 0 && !"..".equals(pathParts.get(i - 1))) {
          // back up over the previous part which will soon contain the next
          // part to process.
          --i;
          pathParts.subList(i, i + 2).clear();
        } else if (isAbs) {  // can't get to parent of the root.
          pathParts.remove(i);
        } else {
          // normalize so the "..".equals(pathParts.get(i - 1)) check above
          // works.
          pathParts.set(i, "..");
          ++i;
        }
      } else {
        ++i;
      }
    }
    if (isAbs) {
      // All paths for absolute URIs must start with '/'.
      // The URL http://foo?bar is not strictly legal.
      out.append('/');
    }
    Join.join(out, "/", pathParts);
  }

  private static String normalizeEscapesInPath(String path) {
    // Section 3.3
    // The path may consist of a sequence of path segments separated by a
    // single slash "/" character.  Within a path segment, the characters
    // "/", ";", "=", and "?" are reserved.  Each path segment may include a
    // sequence of parameters, indicated by the semicolon ";" character.
    // The parameters are not significant to the parsing of relative
    // references.

    // The assertion that '?' is reserved in a path segment is inconsistent with
    // the grammar in that same section.
    // Parameters to path segments are not widely used, and ';' is a CSS special
    // character and a natural target for a badly written error recovery scheme.
    StringBuilder sb = new StringBuilder();
    // escape all but [\w\d.:+-]
    int pos = 0, n = path.length();
    for (int i = 0; i < n; ++i) {
      char ch = path.charAt(i);
      if (ch == '%') {
        if (isInvalidEsc(path, i)) {
          sb.append(path, pos, i).append("%25");
          pos = i + 1;
        }
      } else if (!(('a' <= ch && ch <= 'z')
                   || ('A' <= ch && ch <= 'Z')
                   || ('0' <= ch && ch <= '9')
                   // Escapes ';' and '='
                   || ch == ':' || ch == '-' || ch == '+'
                   || ch == '.' || ch == '/' || ch == ',' || ch == '$')) {
        sb.append(path, pos, i);
        pos = i + 1;
        pctEncode(ch, sb);
      }
    }
    sb.append(path, pos, n);
    return sb.toString();
  }

  private static void normalizeQuery(String query, StringBuilder out) {
    // 3.4. Query Component
    // The query component is a string of information to be interpreted by
    // the resource.
    // Within a query component, the characters ";", "/", "?", ":", "@",
    // "&", "=", "+", ",", and "$" are reserved.

    // We preserve '&', '=', and the initial '?' but escape most others that are
    // CSS or JS special characters including ';', ':', and '@' for reasons
    // described above.

    int pos = 0, n = query.length();
    for (int i = 0; i < n; ++i) {
      char ch = query.charAt(i);
      if (ch == '%') {
        if (isInvalidEsc(query, i)) {
          out.append(query, pos, i).append("%25");
          pos = i + 1;
        }
      } else if (!(('a' <= ch && ch <= 'z')
                   || ('A' <= ch && ch <= 'Z')
                   || ('0' <= ch && ch <= '9')
                   // Escapes ';', ':' and '@'
                   || ch == '-' || ch == '+' || ch == '.' || ch == '='
                   || ch == '&' || ch == ',')) {
        out.append(query, pos, i);
        pos = i + 1;
        pctEncode(ch, out);
      }
    }
    out.append(query, pos, n);
  }

  private static void normalizeFragment(String fragment, StringBuilder out) {
    // Section 4.1
    // fragment      = *uric

    int pos = 0, n = fragment.length();
    for (int i = 0; i < n; ++i) {
      char ch = fragment.charAt(i);
      if (ch == '%') {
        if (isInvalidEsc(fragment, i)) {
          out.append(fragment, pos, i).append("%25");
          pos = i + 1;
        }
      } else if (!(('a' <= ch && ch <= 'z')
                   || ('A' <= ch && ch <= 'Z')
                   || ('0' <= ch && ch <= '9')
                   || ch == '-' || ch == '+' || ch == '.')) {
        out.append(fragment, pos, i);
        pos = i + 1;
        pctEncode(ch, out);
      }
    }
    out.append(fragment, pos, n);
  }

  private static boolean isInvalidEsc(String uriPart, int pctIdx) {
    return pctIdx + 2 >= uriPart.length()
        || !isHexDigit(uriPart.charAt(pctIdx + 1))
        || !isHexDigit(uriPart.charAt(pctIdx + 2));
  }

  private static boolean isHexDigit(char ch) {
    return ('0' <= ch && ch <= '9') || ('a' <= ch && ch <= 'f')
        || ('A' <= ch && ch <= 'F');
  }

  private static void pctEncode(char ch, StringBuilder out) {
    try {
      Escaping.pctEncode(ch, out);
    } catch (IOException ex) {
      throw new SomethingWidgyHappenedError(
          "StringBuilders shouldn't throw IOException", ex);
    }
  }

  private UriUtil() { /* not instantiable */ }
}
