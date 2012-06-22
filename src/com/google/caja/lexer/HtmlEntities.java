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

package com.google.caja.lexer;

import java.util.HashMap;
import java.util.Map;

import com.google.caja.util.Strings;


/**
 * Utilities for decoding HTML entities.
 */
public class HtmlEntities {

  /** A trie that maps entity names to codepoints. */
  public static final PunctuationTrie<Integer> ENTITY_TRIE;

  /**
   * Decodes any HTML entity at the given location.  This handles both named and
   * numeric entities.
   *
   * @param chars HTML text.
   * @param offset the position of the sequence to decode.
   * @param limit the last position in chars that could be part of the sequence
   *    to decode.
   * @return The offset after the end of the decoded sequence and the decoded
   *    codepoint or code-unit packed into a long.
   *    The first 32 bits are the offset, and the second 32 bits are a codepoint
   *    or a code-unit.
   */
  public static long decodeEntityAt(char[] chars, int offset, int limit) {
    char ch = chars[offset];
    if ('&' != ch) {
      return ((offset + 1L) << 32) | ch;
    }

    int entityLimit = Math.min(limit, offset + 10);
    int end = -1;
    for (int i = offset + 1; i < entityLimit; ++i) {
      if (';' == chars[i]) {
        end = i;
        break;
      }
    }
    if (end < 0 || offset + 2 >= end) {
      return ((offset + 1L) << 32) | '&';
    }
    // Now we know where the entity ends, and that there is at least one
    // character in the entity name
    char ch1 = chars[offset + 1];
    char ch2 = chars[offset + 2];
    int codepoint = -1;
    if ('#' == ch1) {
      // numeric entity
      if ('x' == ch2 || 'X' == ch2) {
        codepoint = 0;
        // hex literal
        digloop:
        for (int i = offset + 3; i < end; ++i) {
          char digit = chars[i];
          switch (digit & 0xfff8) {
            case 0x30: case 0x38: // ASCII 48-57 are '0'-'9'
              int decDig = digit & 0xf;
              if (decDig < 10) {
                codepoint = (codepoint << 4) | decDig;
              } else {
                codepoint = -1;
                break digloop;
              }
              break;
            // ASCII 65-70 and 97-102 are 'A'-'Z' && 'a'-'z'
            case 0x40: case 0x60:
              int hexDig = (digit & 0x7);
              if (hexDig != 0 && hexDig < 7) {
                codepoint = (codepoint << 4) | (hexDig + 9);
              } else {
                codepoint = -1;
                break digloop;
              }
              break;
            default:
              codepoint = -1;
              break digloop;
          }
        }
      } else {
        codepoint = 0;
        // decimal literal
        digloop:
        for (int i = offset + 2; i < end; ++i) {
          char digit = chars[i];
          switch (digit & 0xfff8) {
            case 0x30: case 0x38: // ASCII 48-57 are '0'-'9'
              int decDig = digit - '0';
              if (decDig < 10) {
                codepoint = (codepoint * 10) + decDig;
              } else {
                codepoint = -1;
                break digloop;
              }
              break;
            default:
              codepoint = -1;
              break digloop;
          }
        }
      }
    } else {
      PunctuationTrie<Integer> t = HtmlEntities.ENTITY_TRIE;
      for (int i = offset + 1; i < end; ++i) {
        char nameChar = chars[i];
        t = t.lookup(nameChar);
        if (t == null) { break; }
      }
      if (t == null) {
        t = HtmlEntities.ENTITY_TRIE;
        for (int i = offset + 1; i < end; ++i) {
          char nameChar = chars[i];
          if ('Z' >= nameChar && nameChar >= 'A') { nameChar |= 32; }
          t = t.lookup(nameChar);
          if (t == null) { break; }
        }
      }
      if (t != null && t.isTerminal()) {
        codepoint = t.getValue().intValue();
      }
    }
    if (codepoint < 0) {
      return ((offset + 1L) << 32) | '&';
    } else {
      return ((end + 1L) << 32) | codepoint;
    }
  }

  /** A possible entity name like "amp" or "gt". */
  public static boolean isEntityName(String name) {
    PunctuationTrie<Integer> t = HtmlEntities.ENTITY_TRIE;
    int n = name.length();

    // Treat AMP the same amp, but not Amp.
    boolean isUcase = true;
    for (int i = 0; i < n; ++i) {
      char ch = name.charAt(i);
      if (!('A' <= ch && ch <= 'Z')) {
        isUcase = false;
        break;
      }
    }

    if (isUcase) { name = Strings.lower(name); }

    for (int i = 0; i < n; ++i) {
      t = t.lookup(name.charAt(i));
      if (t == null) { return false; }
    }
    return t.isTerminal();
  }

  static {
    Map<String, Integer> entities = new HashMap<String, Integer>();
    // C0 Controls and Basic Latin
    entities.put("quot", Integer.valueOf('"'));
    entities.put("amp", Integer.valueOf('&'));
    entities.put("lt", Integer.valueOf('<'));
    entities.put("gt", Integer.valueOf('>'));

    // XML 1.0
    entities.put("apos", Integer.valueOf('\''));

    // HTML4 entities
    entities.put("nbsp", Integer.valueOf('\u00a0'));
    entities.put("iexcl", Integer.valueOf('\u00a1'));
    entities.put("cent", Integer.valueOf('\u00a2'));
    entities.put("pound", Integer.valueOf('\u00a3'));
    entities.put("curren", Integer.valueOf('\u00a4'));
    entities.put("yen", Integer.valueOf('\u00a5'));
    entities.put("brvbar", Integer.valueOf('\u00a6'));
    entities.put("sect", Integer.valueOf('\u00a7'));
    entities.put("uml", Integer.valueOf('\u00a8'));
    entities.put("copy", Integer.valueOf('\u00a9'));
    entities.put("ordf", Integer.valueOf('\u00aa'));
    entities.put("laquo", Integer.valueOf('\u00ab'));
    entities.put("not", Integer.valueOf('\u00ac'));
    entities.put("shy", Integer.valueOf('\u00ad'));
    entities.put("reg", Integer.valueOf('\u00ae'));
    entities.put("macr", Integer.valueOf('\u00af'));
    entities.put("deg", Integer.valueOf('\u00b0'));
    entities.put("plusmn", Integer.valueOf('\u00b1'));
    entities.put("sup2", Integer.valueOf('\u00b2'));
    entities.put("sup3", Integer.valueOf('\u00b3'));
    entities.put("acute", Integer.valueOf('\u00b4'));
    entities.put("micro", Integer.valueOf('\u00b5'));
    entities.put("para", Integer.valueOf('\u00b6'));
    entities.put("middot", Integer.valueOf('\u00b7'));
    entities.put("cedil", Integer.valueOf('\u00b8'));
    entities.put("sup1", Integer.valueOf('\u00b9'));
    entities.put("ordm", Integer.valueOf('\u00ba'));
    entities.put("raquo", Integer.valueOf('\u00bb'));
    entities.put("frac14", Integer.valueOf('\u00bc'));
    entities.put("frac12", Integer.valueOf('\u00bd'));
    entities.put("frac34", Integer.valueOf('\u00be'));
    entities.put("iquest", Integer.valueOf('\u00bf'));
    entities.put("Agrave", Integer.valueOf('\u00c0'));
    entities.put("Aacute", Integer.valueOf('\u00c1'));
    entities.put("Acirc", Integer.valueOf('\u00c2'));
    entities.put("Atilde", Integer.valueOf('\u00c3'));
    entities.put("Auml", Integer.valueOf('\u00c4'));
    entities.put("Aring", Integer.valueOf('\u00c5'));
    entities.put("AElig", Integer.valueOf('\u00c6'));
    entities.put("Ccedil", Integer.valueOf('\u00c7'));
    entities.put("Egrave", Integer.valueOf('\u00c8'));
    entities.put("Eacute", Integer.valueOf('\u00c9'));
    entities.put("Ecirc", Integer.valueOf('\u00ca'));
    entities.put("Euml", Integer.valueOf('\u00cb'));
    entities.put("Igrave", Integer.valueOf('\u00cc'));
    entities.put("Iacute", Integer.valueOf('\u00cd'));
    entities.put("Icirc", Integer.valueOf('\u00ce'));
    entities.put("Iuml", Integer.valueOf('\u00cf'));
    entities.put("ETH", Integer.valueOf('\u00d0'));
    entities.put("Ntilde", Integer.valueOf('\u00d1'));
    entities.put("Ograve", Integer.valueOf('\u00d2'));
    entities.put("Oacute", Integer.valueOf('\u00d3'));
    entities.put("Ocirc", Integer.valueOf('\u00d4'));
    entities.put("Otilde", Integer.valueOf('\u00d5'));
    entities.put("Ouml", Integer.valueOf('\u00d6'));
    entities.put("times", Integer.valueOf('\u00d7'));
    entities.put("Oslash", Integer.valueOf('\u00d8'));
    entities.put("Ugrave", Integer.valueOf('\u00d9'));
    entities.put("Uacute", Integer.valueOf('\u00da'));
    entities.put("Ucirc", Integer.valueOf('\u00db'));
    entities.put("Uuml", Integer.valueOf('\u00dc'));
    entities.put("Yacute", Integer.valueOf('\u00dd'));
    entities.put("THORN", Integer.valueOf('\u00de'));
    entities.put("szlig", Integer.valueOf('\u00df'));
    entities.put("agrave", Integer.valueOf('\u00e0'));
    entities.put("aacute", Integer.valueOf('\u00e1'));
    entities.put("acirc", Integer.valueOf('\u00e2'));
    entities.put("atilde", Integer.valueOf('\u00e3'));
    entities.put("auml", Integer.valueOf('\u00e4'));
    entities.put("aring", Integer.valueOf('\u00e5'));
    entities.put("aelig", Integer.valueOf('\u00e6'));
    entities.put("ccedil", Integer.valueOf('\u00e7'));
    entities.put("egrave", Integer.valueOf('\u00e8'));
    entities.put("eacute", Integer.valueOf('\u00e9'));
    entities.put("ecirc", Integer.valueOf('\u00ea'));
    entities.put("euml", Integer.valueOf('\u00eb'));
    entities.put("igrave", Integer.valueOf('\u00ec'));
    entities.put("iacute", Integer.valueOf('\u00ed'));
    entities.put("icirc", Integer.valueOf('\u00ee'));
    entities.put("iuml", Integer.valueOf('\u00ef'));
    entities.put("eth", Integer.valueOf('\u00f0'));
    entities.put("ntilde", Integer.valueOf('\u00f1'));
    entities.put("ograve", Integer.valueOf('\u00f2'));
    entities.put("oacute", Integer.valueOf('\u00f3'));
    entities.put("ocirc", Integer.valueOf('\u00f4'));
    entities.put("otilde", Integer.valueOf('\u00f5'));
    entities.put("ouml", Integer.valueOf('\u00f6'));
    entities.put("divide", Integer.valueOf('\u00f7'));
    entities.put("oslash", Integer.valueOf('\u00f8'));
    entities.put("ugrave", Integer.valueOf('\u00f9'));
    entities.put("uacute", Integer.valueOf('\u00fa'));
    entities.put("ucirc", Integer.valueOf('\u00fb'));
    entities.put("uuml", Integer.valueOf('\u00fc'));
    entities.put("yacute", Integer.valueOf('\u00fd'));
    entities.put("thorn", Integer.valueOf('\u00fe'));
    entities.put("yuml", Integer.valueOf('\u00ff'));

    // Latin Extended-B
    entities.put("fnof", Integer.valueOf('\u0192'));

    // Greek
    entities.put("Alpha", Integer.valueOf('\u0391'));
    entities.put("Beta", Integer.valueOf('\u0392'));
    entities.put("Gamma", Integer.valueOf('\u0393'));
    entities.put("Delta", Integer.valueOf('\u0394'));
    entities.put("Epsilon", Integer.valueOf('\u0395'));
    entities.put("Zeta", Integer.valueOf('\u0396'));
    entities.put("Eta", Integer.valueOf('\u0397'));
    entities.put("Theta", Integer.valueOf('\u0398'));
    entities.put("Iota", Integer.valueOf('\u0399'));
    entities.put("Kappa", Integer.valueOf('\u039a'));
    entities.put("Lambda", Integer.valueOf('\u039b'));
    entities.put("Mu", Integer.valueOf('\u039c'));
    entities.put("Nu", Integer.valueOf('\u039d'));
    entities.put("Xi", Integer.valueOf('\u039e'));
    entities.put("Omicron", Integer.valueOf('\u039f'));
    entities.put("Pi", Integer.valueOf('\u03a0'));
    entities.put("Rho", Integer.valueOf('\u03a1'));
    entities.put("Sigma", Integer.valueOf('\u03a3'));
    entities.put("Tau", Integer.valueOf('\u03a4'));
    entities.put("Upsilon", Integer.valueOf('\u03a5'));
    entities.put("Phi", Integer.valueOf('\u03a6'));
    entities.put("Chi", Integer.valueOf('\u03a7'));
    entities.put("Psi", Integer.valueOf('\u03a8'));
    entities.put("Omega", Integer.valueOf('\u03a9'));

    entities.put("alpha", Integer.valueOf('\u03b1'));
    entities.put("beta", Integer.valueOf('\u03b2'));
    entities.put("gamma", Integer.valueOf('\u03b3'));
    entities.put("delta", Integer.valueOf('\u03b4'));
    entities.put("epsilon", Integer.valueOf('\u03b5'));
    entities.put("zeta", Integer.valueOf('\u03b6'));
    entities.put("eta", Integer.valueOf('\u03b7'));
    entities.put("theta", Integer.valueOf('\u03b8'));
    entities.put("iota", Integer.valueOf('\u03b9'));
    entities.put("kappa", Integer.valueOf('\u03ba'));
    entities.put("lambda", Integer.valueOf('\u03bb'));
    entities.put("mu", Integer.valueOf('\u03bc'));
    entities.put("nu", Integer.valueOf('\u03bd'));
    entities.put("xi", Integer.valueOf('\u03be'));
    entities.put("omicron", Integer.valueOf('\u03bf'));
    entities.put("pi", Integer.valueOf('\u03c0'));
    entities.put("rho", Integer.valueOf('\u03c1'));
    entities.put("sigmaf", Integer.valueOf('\u03c2'));
    entities.put("sigma", Integer.valueOf('\u03c3'));
    entities.put("tau", Integer.valueOf('\u03c4'));
    entities.put("upsilon", Integer.valueOf('\u03c5'));
    entities.put("phi", Integer.valueOf('\u03c6'));
    entities.put("chi", Integer.valueOf('\u03c7'));
    entities.put("psi", Integer.valueOf('\u03c8'));
    entities.put("omega", Integer.valueOf('\u03c9'));
    entities.put("thetasym", Integer.valueOf('\u03d1'));
    entities.put("upsih", Integer.valueOf('\u03d2'));
    entities.put("piv", Integer.valueOf('\u03d6'));

    // General Punctuation
    entities.put("bull", Integer.valueOf('\u2022'));
    entities.put("hellip", Integer.valueOf('\u2026'));
    entities.put("prime", Integer.valueOf('\u2032'));
    entities.put("Prime", Integer.valueOf('\u2033'));
    entities.put("oline", Integer.valueOf('\u203e'));
    entities.put("frasl", Integer.valueOf('\u2044'));

    // Letterlike Symbols
    entities.put("weierp", Integer.valueOf('\u2118'));
    entities.put("image", Integer.valueOf('\u2111'));
    entities.put("real", Integer.valueOf('\u211c'));
    entities.put("trade", Integer.valueOf('\u2122'));
    entities.put("alefsym", Integer.valueOf('\u2135'));

    // Arrows
    entities.put("larr", Integer.valueOf('\u2190'));
    entities.put("uarr", Integer.valueOf('\u2191'));
    entities.put("rarr", Integer.valueOf('\u2192'));
    entities.put("darr", Integer.valueOf('\u2193'));
    entities.put("harr", Integer.valueOf('\u2194'));
    entities.put("crarr", Integer.valueOf('\u21b5'));
    entities.put("lArr", Integer.valueOf('\u21d0'));
    entities.put("uArr", Integer.valueOf('\u21d1'));
    entities.put("rArr", Integer.valueOf('\u21d2'));
    entities.put("dArr", Integer.valueOf('\u21d3'));
    entities.put("hArr", Integer.valueOf('\u21d4'));

    // Mathematical Operators
    entities.put("forall", Integer.valueOf('\u2200'));
    entities.put("part", Integer.valueOf('\u2202'));
    entities.put("exist", Integer.valueOf('\u2203'));
    entities.put("empty", Integer.valueOf('\u2205'));
    entities.put("nabla", Integer.valueOf('\u2207'));
    entities.put("isin", Integer.valueOf('\u2208'));
    entities.put("notin", Integer.valueOf('\u2209'));
    entities.put("ni", Integer.valueOf('\u220b'));
    entities.put("prod", Integer.valueOf('\u220f'));
    entities.put("sum", Integer.valueOf('\u2211'));
    entities.put("minus", Integer.valueOf('\u2212'));
    entities.put("lowast", Integer.valueOf('\u2217'));
    entities.put("radic", Integer.valueOf('\u221a'));
    entities.put("prop", Integer.valueOf('\u221d'));
    entities.put("infin", Integer.valueOf('\u221e'));
    entities.put("ang", Integer.valueOf('\u2220'));
    entities.put("and", Integer.valueOf('\u2227'));
    entities.put("or", Integer.valueOf('\u2228'));
    entities.put("cap", Integer.valueOf('\u2229'));
    entities.put("cup", Integer.valueOf('\u222a'));
    entities.put("int", Integer.valueOf('\u222b'));
    entities.put("there4", Integer.valueOf('\u2234'));
    entities.put("sim", Integer.valueOf('\u223c'));
    entities.put("cong", Integer.valueOf('\u2245'));
    entities.put("asymp", Integer.valueOf('\u2248'));
    entities.put("ne", Integer.valueOf('\u2260'));
    entities.put("equiv", Integer.valueOf('\u2261'));
    entities.put("le", Integer.valueOf('\u2264'));
    entities.put("ge", Integer.valueOf('\u2265'));
    entities.put("sub", Integer.valueOf('\u2282'));
    entities.put("sup", Integer.valueOf('\u2283'));
    entities.put("nsub", Integer.valueOf('\u2284'));
    entities.put("sube", Integer.valueOf('\u2286'));
    entities.put("supe", Integer.valueOf('\u2287'));
    entities.put("oplus", Integer.valueOf('\u2295'));
    entities.put("otimes", Integer.valueOf('\u2297'));
    entities.put("perp", Integer.valueOf('\u22a5'));
    entities.put("sdot", Integer.valueOf('\u22c5'));

    // Miscellaneous Technical
    entities.put("lceil", Integer.valueOf('\u2308'));
    entities.put("rceil", Integer.valueOf('\u2309'));
    entities.put("lfloor", Integer.valueOf('\u230a'));
    entities.put("rfloor", Integer.valueOf('\u230b'));
    entities.put("lang", Integer.valueOf('\u2329'));
    entities.put("rang", Integer.valueOf('\u232a'));

    // Geometric Shapes
    entities.put("loz", Integer.valueOf('\u25ca'));

    // Miscellaneous Symbols
    entities.put("spades", Integer.valueOf('\u2660'));
    entities.put("clubs", Integer.valueOf('\u2663'));
    entities.put("hearts", Integer.valueOf('\u2665'));
    entities.put("diams", Integer.valueOf('\u2666'));

    // Latin Extended-A
    entities.put("OElig", Integer.valueOf('\u0152'));
    entities.put("oelig", Integer.valueOf('\u0153'));
    entities.put("Scaron", Integer.valueOf('\u0160'));
    entities.put("scaron", Integer.valueOf('\u0161'));
    entities.put("Yuml", Integer.valueOf('\u0178'));

    // Spacing Modifier Letters
    entities.put("circ", Integer.valueOf('\u02c6'));
    entities.put("tilde", Integer.valueOf('\u02dc'));

    // General Punctuation
    entities.put("ensp", Integer.valueOf('\u2002'));
    entities.put("emsp", Integer.valueOf('\u2003'));
    entities.put("thinsp", Integer.valueOf('\u2009'));
    entities.put("zwnj", Integer.valueOf('\u200c'));
    entities.put("zwj", Integer.valueOf('\u200d'));
    entities.put("lrm", Integer.valueOf('\u200e'));
    entities.put("rlm", Integer.valueOf('\u200f'));
    entities.put("ndash", Integer.valueOf('\u2013'));
    entities.put("mdash", Integer.valueOf('\u2014'));
    entities.put("lsquo", Integer.valueOf('\u2018'));
    entities.put("rsquo", Integer.valueOf('\u2019'));
    entities.put("sbquo", Integer.valueOf('\u201a'));
    entities.put("ldquo", Integer.valueOf('\u201c'));
    entities.put("rdquo", Integer.valueOf('\u201d'));
    entities.put("bdquo", Integer.valueOf('\u201e'));
    entities.put("dagger", Integer.valueOf('\u2020'));
    entities.put("Dagger", Integer.valueOf('\u2021'));
    entities.put("permil", Integer.valueOf('\u2030'));
    entities.put("lsaquo", Integer.valueOf('\u2039'));
    entities.put("rsaquo", Integer.valueOf('\u203a'));
    entities.put("euro", Integer.valueOf('\u20ac'));

    ENTITY_TRIE = new PunctuationTrie<Integer>(entities);
  }
}
