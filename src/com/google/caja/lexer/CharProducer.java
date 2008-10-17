// Copyright (C) 2005 Google Inc.
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

import com.google.caja.util.Strings;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * A character reader that tracks character file position information.
 *
 * @author mikesamuel@gmail.com
 */
public interface CharProducer extends Closeable {

  /** The next character or -1 if end of file reached. */
  public int read() throws IOException;

  /**
   * The position of the next character to read or the position past the end
   * of the file if end of file reached.
   */
  public FilePosition getCurrentPosition();

  /**
   * Stores the current position as described at {@link #getCurrentPosition()}
   * in the given buffer.
   * @param posBuffer non null.  modified in place.
   */
  public boolean getCurrentPosition(MutableFilePosition posBuffer);

  /**
   * Convenience methods for creating producers.
   *
   * <p>TODO(mikesamuel): separate this into two factories in separate files,
   * one with java.IO dependencies that won't work under GWT, and one that
   * handles the escaping conventions that will.
   */
  public static final class Factory {
    public static CharProducer create(Reader r, FilePosition pos) {
      return new CharProducerImpl(r, pos);
    }

    public static CharProducer create(Reader r, InputSource src) {
      return create(r, FilePosition.startOfFile(src));
    }

    public static CharProducer fromJsString(CharProducer p) {
      return new JsUnEscapeWrapper(p);
    }

    public static CharProducer fromHtmlAttribute(CharProducer p) {
      return new HtmlUnEscapeWrapper(p);
    }

    /**
     * A CharProducer that produces the concatenation of the given character
     * producers.  It produces all the characters in turn from its first
     * argument, and when that is exhausted proceeds to the next input.
     * The inputs are consumed.
     */
    public static CharProducer chain(CharProducer... p) {
      // defensive copy
      final CharProducer[] producers = new CharProducer[p.length];
      System.arraycopy(p, 0, producers, 0, p.length);
      return new CharProducer() {
        int k = 0;

        public void close() throws IOException {
          IOException rethrown = null;
          int nFailures = 0;
          for (int i = 0; i < producers.length; ++i) {
            try {
              producers[i].close();
            } catch (IOException ex) {
              if (null == rethrown) { rethrown = ex; }
              nFailures++;
            }
          }
          if (null != rethrown) {
            IOException ioe = new IOException(
                "Failed to close " + nFailures + " in chain");
            ioe.initCause(rethrown);
            throw ioe;
          }
        }

        public int read() throws IOException {
          int ch = -1;
          while (k < producers.length && (ch = producers[k].read()) < 0) {
            ++k;
          }
          return ch;
        }

        public FilePosition getCurrentPosition() {
          return (k < producers.length
                  ? producers[k]
                  : producers[producers.length - 1]).getCurrentPosition();
        }

        public boolean getCurrentPosition(MutableFilePosition posBuf) {
          return (k < producers.length
                  ? producers[k]
                  : producers[producers.length - 1]).getCurrentPosition(posBuf);
        }
      };
    }

    private Factory() {
      // uninstantiable
    }

    private static final class CharProducerImpl
        implements CharProducer, Closeable {
      private final Reader r;
      private final MutableFilePosition posBuf;
      private final char[] buf = new char[1024];
      private int offset = 1;
      private int read = 0;

      CharProducerImpl(Reader r, FilePosition pos) {
        this.r = r;
        posBuf = new MutableFilePosition(pos);
      }

      @SuppressWarnings("fallthrough")
      public int read() throws IOException {
        int lookback = buf[offset - 1];  // Will read zero on first entry.

        if (offset >= read) {
          read = r.read(buf);
          if (read <= 0) { return -1; }
          offset = 0;
        }
        char ch = buf[offset++];
        ++posBuf.charInFile;
        ++posBuf.charInLine;
        switch (ch) {
          case '\n':
            if (lookback == '\r') { --posBuf.lineNo; }
            // fall through
          case '\r':
            ++posBuf.lineNo;
            posBuf.charInLine = 1;
            break;
        }
        return ch;
      }

      public void close() throws IOException {
        r.close();
        read = 0;
      }

      public FilePosition getCurrentPosition() {
        return posBuf.toFilePosition();
      }

      public boolean getCurrentPosition(MutableFilePosition posBuf) {
        this.posBuf.copyTo(posBuf);
        return true;
      }
    }

    private static class JsUnEscapeWrapper extends LookaheadCharProducer {
      JsUnEscapeWrapper(CharProducer p) { super(p, 4); }

      @Override
      public int read() throws IOException {
        int ch = super.read();
        if ('\\' != ch) { return ch; }
        // we've found an escaped character.
        fetch(1);
        int ch2 = super.read();
// for javascript escaping conventions see
// http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Guide:Literals
        switch (ch2) {
          case 'b': return '\b';
          case 'r': return '\r';
          case 'n': return '\n';
          case 'f': return '\f';
          case 't': return '\t';
          case 'v': return '\u000b';
          // unicode and hex escapes
          case 'u': case 'x':
          {
            int nHex = ch2 == 'u' ? 4 : 2;
            fetch(nHex);
            return NumericEscapes.unescapeHex(this, 0, nHex, nHex, ch2);
          }
          // octal escapes in 0-377
          case '0': case '1': case '2': case '3':
          case '4': case '5': case '6': case '7':
          {
            pushback();  // Move ch2 back into the live region of lookahead
            fetch(3);  // Make sure we have 3 digits to perform lookahead on
            return NumericEscapes.unescapeOctal(
                this,
                // We only accept octal literals in the range 0-377, so clip
                // one character from limit if the first character is >= '4'
                limit() + (ch2 <= '3' ? 0 : -1));
          }
          default:
            return ch2;
        }
      }
    }

    private static class HtmlUnEscapeWrapper extends LookaheadCharProducer {
      HtmlUnEscapeWrapper(CharProducer p) { super(p, 7); }

      @Override
      public int read() throws IOException {
        int ch = super.read();
        if ('&' != ch) { return ch; }
        // We've found an escaped character.
        fetch(7);  // (length "#x0000;") == 7

        int end = -1;
        for (int i = 1; i < limit(); ++i) {
          if (';' == peek(i)) {
            end = i;
            break;
          }
        }
        if (end < 0) { return ch; }
        // Now we know where the entity ends, and that there is at least one
        // character in the entity name

        // We only handle the xml escapes and apos for now.
        int ch2 = peek(0);
        if ('#' == ch2) {
          // numeric entity
          if ('x' == peek(1) || 'X' == peek(1)) {
            // hex literal
            return NumericEscapes.unescapeHex(this, 2, end, end + 1, ch);
          } else {
            // decimal literal
            return NumericEscapes.unescapeDecimal(this, 1, end, end + 1, ch);
          }
        } else {
          StringBuilder sb = new StringBuilder(end);
          for (int i = 0; i < end; ++i) {
            sb.append((char) peek(i));
          }
          Character c = ENTITY_TABLE.get(sb.toString());
          if (null == c) {
            c = ENTITY_TABLE.get(Strings.toLowerCase(sb.toString()));
          }
          if (null == c) { return ch; }
          consume(end + 1);
          return c.charValue();
        }
      }
    }

    private static final Map<String, Character> ENTITY_TABLE =
        new HashMap<String, Character>();
    static {
      /* XML 1.0 */
      ENTITY_TABLE.put("apos", '\'');

      /* HTML4 entities */
      ENTITY_TABLE.put("nbsp", Character.valueOf('\u00a0'));
      ENTITY_TABLE.put("iexcl", Character.valueOf('\u00a1'));
      ENTITY_TABLE.put("cent", Character.valueOf('\u00a2'));
      ENTITY_TABLE.put("pound", Character.valueOf('\u00a3'));
      ENTITY_TABLE.put("curren", Character.valueOf('\u00a4'));
      ENTITY_TABLE.put("yen", Character.valueOf('\u00a5'));
      ENTITY_TABLE.put("brvbar", Character.valueOf('\u00a6'));
      ENTITY_TABLE.put("sect", Character.valueOf('\u00a7'));
      ENTITY_TABLE.put("uml", Character.valueOf('\u00a8'));
      ENTITY_TABLE.put("copy", Character.valueOf('\u00a9'));
      ENTITY_TABLE.put("ordf", Character.valueOf('\u00aa'));
      ENTITY_TABLE.put("laquo", Character.valueOf('\u00ab'));
      ENTITY_TABLE.put("not", Character.valueOf('\u00ac'));
      ENTITY_TABLE.put("shy", Character.valueOf('\u00ad'));
      ENTITY_TABLE.put("reg", Character.valueOf('\u00ae'));
      ENTITY_TABLE.put("macr", Character.valueOf('\u00af'));
      ENTITY_TABLE.put("deg", Character.valueOf('\u00b0'));
      ENTITY_TABLE.put("plusmn", Character.valueOf('\u00b1'));
      ENTITY_TABLE.put("sup2", Character.valueOf('\u00b2'));
      ENTITY_TABLE.put("sup3", Character.valueOf('\u00b3'));
      ENTITY_TABLE.put("acute", Character.valueOf('\u00b4'));
      ENTITY_TABLE.put("micro", Character.valueOf('\u00b5'));
      ENTITY_TABLE.put("para", Character.valueOf('\u00b6'));
      ENTITY_TABLE.put("middot", Character.valueOf('\u00b7'));
      ENTITY_TABLE.put("cedil", Character.valueOf('\u00b8'));
      ENTITY_TABLE.put("sup1", Character.valueOf('\u00b9'));
      ENTITY_TABLE.put("ordm", Character.valueOf('\u00ba'));
      ENTITY_TABLE.put("raquo", Character.valueOf('\u00bb'));
      ENTITY_TABLE.put("frac14", Character.valueOf('\u00bc'));
      ENTITY_TABLE.put("frac12", Character.valueOf('\u00bd'));
      ENTITY_TABLE.put("frac34", Character.valueOf('\u00be'));
      ENTITY_TABLE.put("iquest", Character.valueOf('\u00bf'));
      ENTITY_TABLE.put("Agrave", Character.valueOf('\u00c0'));
      ENTITY_TABLE.put("Aacute", Character.valueOf('\u00c1'));
      ENTITY_TABLE.put("Acirc", Character.valueOf('\u00c2'));
      ENTITY_TABLE.put("Atilde", Character.valueOf('\u00c3'));
      ENTITY_TABLE.put("Auml", Character.valueOf('\u00c4'));
      ENTITY_TABLE.put("Aring", Character.valueOf('\u00c5'));
      ENTITY_TABLE.put("AElig", Character.valueOf('\u00c6'));
      ENTITY_TABLE.put("Ccedil", Character.valueOf('\u00c7'));
      ENTITY_TABLE.put("Egrave", Character.valueOf('\u00c8'));
      ENTITY_TABLE.put("Eacute", Character.valueOf('\u00c9'));
      ENTITY_TABLE.put("Ecirc", Character.valueOf('\u00ca'));
      ENTITY_TABLE.put("Euml", Character.valueOf('\u00cb'));
      ENTITY_TABLE.put("Igrave", Character.valueOf('\u00cc'));
      ENTITY_TABLE.put("Iacute", Character.valueOf('\u00cd'));
      ENTITY_TABLE.put("Icirc", Character.valueOf('\u00ce'));
      ENTITY_TABLE.put("Iuml", Character.valueOf('\u00cf'));
      ENTITY_TABLE.put("ETH", Character.valueOf('\u00d0'));
      ENTITY_TABLE.put("Ntilde", Character.valueOf('\u00d1'));
      ENTITY_TABLE.put("Ograve", Character.valueOf('\u00d2'));
      ENTITY_TABLE.put("Oacute", Character.valueOf('\u00d3'));
      ENTITY_TABLE.put("Ocirc", Character.valueOf('\u00d4'));
      ENTITY_TABLE.put("Otilde", Character.valueOf('\u00d5'));
      ENTITY_TABLE.put("Ouml", Character.valueOf('\u00d6'));
      ENTITY_TABLE.put("times", Character.valueOf('\u00d7'));
      ENTITY_TABLE.put("Oslash", Character.valueOf('\u00d8'));
      ENTITY_TABLE.put("Ugrave", Character.valueOf('\u00d9'));
      ENTITY_TABLE.put("Uacute", Character.valueOf('\u00da'));
      ENTITY_TABLE.put("Ucirc", Character.valueOf('\u00db'));
      ENTITY_TABLE.put("Uuml", Character.valueOf('\u00dc'));
      ENTITY_TABLE.put("Yacute", Character.valueOf('\u00dd'));
      ENTITY_TABLE.put("THORN", Character.valueOf('\u00de'));
      ENTITY_TABLE.put("szlig", Character.valueOf('\u00df'));
      ENTITY_TABLE.put("agrave", Character.valueOf('\u00e0'));
      ENTITY_TABLE.put("aacute", Character.valueOf('\u00e1'));
      ENTITY_TABLE.put("acirc", Character.valueOf('\u00e2'));
      ENTITY_TABLE.put("atilde", Character.valueOf('\u00e3'));
      ENTITY_TABLE.put("auml", Character.valueOf('\u00e4'));
      ENTITY_TABLE.put("aring", Character.valueOf('\u00e5'));
      ENTITY_TABLE.put("aelig", Character.valueOf('\u00e6'));
      ENTITY_TABLE.put("ccedil", Character.valueOf('\u00e7'));
      ENTITY_TABLE.put("egrave", Character.valueOf('\u00e8'));
      ENTITY_TABLE.put("eacute", Character.valueOf('\u00e9'));
      ENTITY_TABLE.put("ecirc", Character.valueOf('\u00ea'));
      ENTITY_TABLE.put("euml", Character.valueOf('\u00eb'));
      ENTITY_TABLE.put("igrave", Character.valueOf('\u00ec'));
      ENTITY_TABLE.put("iacute", Character.valueOf('\u00ed'));
      ENTITY_TABLE.put("icirc", Character.valueOf('\u00ee'));
      ENTITY_TABLE.put("iuml", Character.valueOf('\u00ef'));
      ENTITY_TABLE.put("eth", Character.valueOf('\u00f0'));
      ENTITY_TABLE.put("ntilde", Character.valueOf('\u00f1'));
      ENTITY_TABLE.put("ograve", Character.valueOf('\u00f2'));
      ENTITY_TABLE.put("oacute", Character.valueOf('\u00f3'));
      ENTITY_TABLE.put("ocirc", Character.valueOf('\u00f4'));
      ENTITY_TABLE.put("otilde", Character.valueOf('\u00f5'));
      ENTITY_TABLE.put("ouml", Character.valueOf('\u00f6'));
      ENTITY_TABLE.put("divide", Character.valueOf('\u00f7'));
      ENTITY_TABLE.put("oslash", Character.valueOf('\u00f8'));
      ENTITY_TABLE.put("ugrave", Character.valueOf('\u00f9'));
      ENTITY_TABLE.put("uacute", Character.valueOf('\u00fa'));
      ENTITY_TABLE.put("ucirc", Character.valueOf('\u00fb'));
      ENTITY_TABLE.put("uuml", Character.valueOf('\u00fc'));
      ENTITY_TABLE.put("yacute", Character.valueOf('\u00fd'));
      ENTITY_TABLE.put("thorn", Character.valueOf('\u00fe'));
      ENTITY_TABLE.put("yuml", Character.valueOf('\u00ff'));

      /* Latin Extended-B */
      ENTITY_TABLE.put("fnof", Character.valueOf('\u0192'));

      /* Greek */
      ENTITY_TABLE.put("Alpha", Character.valueOf('\u0391'));
      ENTITY_TABLE.put("Beta", Character.valueOf('\u0392'));
      ENTITY_TABLE.put("Gamma", Character.valueOf('\u0393'));
      ENTITY_TABLE.put("Delta", Character.valueOf('\u0394'));
      ENTITY_TABLE.put("Epsilon", Character.valueOf('\u0395'));
      ENTITY_TABLE.put("Zeta", Character.valueOf('\u0396'));
      ENTITY_TABLE.put("Eta", Character.valueOf('\u0397'));
      ENTITY_TABLE.put("Theta", Character.valueOf('\u0398'));
      ENTITY_TABLE.put("Iota", Character.valueOf('\u0399'));
      ENTITY_TABLE.put("Kappa", Character.valueOf('\u039a'));
      ENTITY_TABLE.put("Lambda", Character.valueOf('\u039b'));
      ENTITY_TABLE.put("Mu", Character.valueOf('\u039c'));
      ENTITY_TABLE.put("Nu", Character.valueOf('\u039d'));
      ENTITY_TABLE.put("Xi", Character.valueOf('\u039e'));
      ENTITY_TABLE.put("Omicron", Character.valueOf('\u039f'));
      ENTITY_TABLE.put("Pi", Character.valueOf('\u03a0'));
      ENTITY_TABLE.put("Rho", Character.valueOf('\u03a1'));
      ENTITY_TABLE.put("Sigma", Character.valueOf('\u03a3'));
      ENTITY_TABLE.put("Tau", Character.valueOf('\u03a4'));
      ENTITY_TABLE.put("Upsilon", Character.valueOf('\u03a5'));
      ENTITY_TABLE.put("Phi", Character.valueOf('\u03a6'));
      ENTITY_TABLE.put("Chi", Character.valueOf('\u03a7'));
      ENTITY_TABLE.put("Psi", Character.valueOf('\u03a8'));
      ENTITY_TABLE.put("Omega", Character.valueOf('\u03a9'));

      ENTITY_TABLE.put("alpha", Character.valueOf('\u03b1'));
      ENTITY_TABLE.put("beta", Character.valueOf('\u03b2'));
      ENTITY_TABLE.put("gamma", Character.valueOf('\u03b3'));
      ENTITY_TABLE.put("delta", Character.valueOf('\u03b4'));
      ENTITY_TABLE.put("epsilon", Character.valueOf('\u03b5'));
      ENTITY_TABLE.put("zeta", Character.valueOf('\u03b6'));
      ENTITY_TABLE.put("eta", Character.valueOf('\u03b7'));
      ENTITY_TABLE.put("theta", Character.valueOf('\u03b8'));
      ENTITY_TABLE.put("iota", Character.valueOf('\u03b9'));
      ENTITY_TABLE.put("kappa", Character.valueOf('\u03ba'));
      ENTITY_TABLE.put("lambda", Character.valueOf('\u03bb'));
      ENTITY_TABLE.put("mu", Character.valueOf('\u03bc'));
      ENTITY_TABLE.put("nu", Character.valueOf('\u03bd'));
      ENTITY_TABLE.put("xi", Character.valueOf('\u03be'));
      ENTITY_TABLE.put("omicron", Character.valueOf('\u03bf'));
      ENTITY_TABLE.put("pi", Character.valueOf('\u03c0'));
      ENTITY_TABLE.put("rho", Character.valueOf('\u03c1'));
      ENTITY_TABLE.put("sigmaf", Character.valueOf('\u03c2'));
      ENTITY_TABLE.put("sigma", Character.valueOf('\u03c3'));
      ENTITY_TABLE.put("tau", Character.valueOf('\u03c4'));
      ENTITY_TABLE.put("upsilon", Character.valueOf('\u03c5'));
      ENTITY_TABLE.put("phi", Character.valueOf('\u03c6'));
      ENTITY_TABLE.put("chi", Character.valueOf('\u03c7'));
      ENTITY_TABLE.put("psi", Character.valueOf('\u03c8'));
      ENTITY_TABLE.put("omega", Character.valueOf('\u03c9'));
      ENTITY_TABLE.put("thetasym", Character.valueOf('\u03d1'));
      ENTITY_TABLE.put("upsih", Character.valueOf('\u03d2'));
      ENTITY_TABLE.put("piv", Character.valueOf('\u03d6'));

      /* General Punctuation */
      ENTITY_TABLE.put("bull", Character.valueOf('\u2022'));
      ENTITY_TABLE.put("hellip", Character.valueOf('\u2026'));
      ENTITY_TABLE.put("prime", Character.valueOf('\u2032'));
      ENTITY_TABLE.put("Prime", Character.valueOf('\u2033'));
      ENTITY_TABLE.put("oline", Character.valueOf('\u203e'));
      ENTITY_TABLE.put("frasl", Character.valueOf('\u2044'));

      /* Letterlike Symbols */
      ENTITY_TABLE.put("weierp", Character.valueOf('\u2118'));
      ENTITY_TABLE.put("image", Character.valueOf('\u2111'));
      ENTITY_TABLE.put("real", Character.valueOf('\u211c'));
      ENTITY_TABLE.put("trade", Character.valueOf('\u2122'));
      ENTITY_TABLE.put("alefsym", Character.valueOf('\u2135'));

      /* Arrows */
      ENTITY_TABLE.put("larr", Character.valueOf('\u2190'));
      ENTITY_TABLE.put("uarr", Character.valueOf('\u2191'));
      ENTITY_TABLE.put("rarr", Character.valueOf('\u2192'));
      ENTITY_TABLE.put("darr", Character.valueOf('\u2193'));
      ENTITY_TABLE.put("harr", Character.valueOf('\u2194'));
      ENTITY_TABLE.put("crarr", Character.valueOf('\u21b5'));
      ENTITY_TABLE.put("lArr", Character.valueOf('\u21d0'));
      ENTITY_TABLE.put("uArr", Character.valueOf('\u21d1'));
      ENTITY_TABLE.put("rArr", Character.valueOf('\u21d2'));
      ENTITY_TABLE.put("dArr", Character.valueOf('\u21d3'));
      ENTITY_TABLE.put("hArr", Character.valueOf('\u21d4'));

      /* Mathematical Operators */
      ENTITY_TABLE.put("forall", Character.valueOf('\u2200'));
      ENTITY_TABLE.put("part", Character.valueOf('\u2202'));
      ENTITY_TABLE.put("exist", Character.valueOf('\u2203'));
      ENTITY_TABLE.put("empty", Character.valueOf('\u2205'));
      ENTITY_TABLE.put("nabla", Character.valueOf('\u2207'));
      ENTITY_TABLE.put("isin", Character.valueOf('\u2208'));
      ENTITY_TABLE.put("notin", Character.valueOf('\u2209'));
      ENTITY_TABLE.put("ni", Character.valueOf('\u220b'));
      ENTITY_TABLE.put("prod", Character.valueOf('\u220f'));
      ENTITY_TABLE.put("sum", Character.valueOf('\u2211'));
      ENTITY_TABLE.put("minus", Character.valueOf('\u2212'));
      ENTITY_TABLE.put("lowast", Character.valueOf('\u2217'));
      ENTITY_TABLE.put("radic", Character.valueOf('\u221a'));
      ENTITY_TABLE.put("prop", Character.valueOf('\u221d'));
      ENTITY_TABLE.put("infin", Character.valueOf('\u221e'));
      ENTITY_TABLE.put("ang", Character.valueOf('\u2220'));
      ENTITY_TABLE.put("and", Character.valueOf('\u2227'));
      ENTITY_TABLE.put("or", Character.valueOf('\u2228'));
      ENTITY_TABLE.put("cap", Character.valueOf('\u2229'));
      ENTITY_TABLE.put("cup", Character.valueOf('\u222a'));
      ENTITY_TABLE.put("int", Character.valueOf('\u222b'));
      ENTITY_TABLE.put("there4", Character.valueOf('\u2234'));
      ENTITY_TABLE.put("sim", Character.valueOf('\u223c'));
      ENTITY_TABLE.put("cong", Character.valueOf('\u2245'));
      ENTITY_TABLE.put("asymp", Character.valueOf('\u2248'));
      ENTITY_TABLE.put("ne", Character.valueOf('\u2260'));
      ENTITY_TABLE.put("equiv", Character.valueOf('\u2261'));
      ENTITY_TABLE.put("le", Character.valueOf('\u2264'));
      ENTITY_TABLE.put("ge", Character.valueOf('\u2265'));
      ENTITY_TABLE.put("sub", Character.valueOf('\u2282'));
      ENTITY_TABLE.put("sup", Character.valueOf('\u2283'));
      ENTITY_TABLE.put("nsub", Character.valueOf('\u2284'));
      ENTITY_TABLE.put("sube", Character.valueOf('\u2286'));
      ENTITY_TABLE.put("supe", Character.valueOf('\u2287'));
      ENTITY_TABLE.put("oplus", Character.valueOf('\u2295'));
      ENTITY_TABLE.put("otimes", Character.valueOf('\u2297'));
      ENTITY_TABLE.put("perp", Character.valueOf('\u22a5'));
      ENTITY_TABLE.put("sdot", Character.valueOf('\u22c5'));

      /* Miscellaneous Technical */
      ENTITY_TABLE.put("lceil", Character.valueOf('\u2308'));
      ENTITY_TABLE.put("rceil", Character.valueOf('\u2309'));
      ENTITY_TABLE.put("lfloor", Character.valueOf('\u230a'));
      ENTITY_TABLE.put("rfloor", Character.valueOf('\u230b'));
      ENTITY_TABLE.put("lang", Character.valueOf('\u2329'));
      ENTITY_TABLE.put("rang", Character.valueOf('\u232a'));

      /* Geometric Shapes */
      ENTITY_TABLE.put("loz", Character.valueOf('\u25ca'));

      /* Miscellaneous Symbols */
      ENTITY_TABLE.put("spades", Character.valueOf('\u2660'));
      ENTITY_TABLE.put("clubs", Character.valueOf('\u2663'));
      ENTITY_TABLE.put("hearts", Character.valueOf('\u2665'));
      ENTITY_TABLE.put("diams", Character.valueOf('\u2666'));

      /* C0 Controls and Basic Latin */
      ENTITY_TABLE.put("quot", Character.valueOf('\u0022'));
      ENTITY_TABLE.put("amp", Character.valueOf('\u0026'));
      ENTITY_TABLE.put("lt", Character.valueOf('\u003c'));
      ENTITY_TABLE.put("gt", Character.valueOf('\u003e'));

      /* Latin Extended-A */
      ENTITY_TABLE.put("OElig", Character.valueOf('\u0152'));
      ENTITY_TABLE.put("oelig", Character.valueOf('\u0153'));
      ENTITY_TABLE.put("Scaron", Character.valueOf('\u0160'));
      ENTITY_TABLE.put("scaron", Character.valueOf('\u0161'));
      ENTITY_TABLE.put("Yuml", Character.valueOf('\u0178'));

      /* Spacing Modifier Letters */
      ENTITY_TABLE.put("circ", Character.valueOf('\u02c6'));
      ENTITY_TABLE.put("tilde", Character.valueOf('\u02dc'));

      /* General Punctuation */
      ENTITY_TABLE.put("ensp", Character.valueOf('\u2002'));
      ENTITY_TABLE.put("emsp", Character.valueOf('\u2003'));
      ENTITY_TABLE.put("thinsp", Character.valueOf('\u2009'));
      ENTITY_TABLE.put("zwnj", Character.valueOf('\u200c'));
      ENTITY_TABLE.put("zwj", Character.valueOf('\u200d'));
      ENTITY_TABLE.put("lrm", Character.valueOf('\u200e'));
      ENTITY_TABLE.put("rlm", Character.valueOf('\u200f'));
      ENTITY_TABLE.put("ndash", Character.valueOf('\u2013'));
      ENTITY_TABLE.put("mdash", Character.valueOf('\u2014'));
      ENTITY_TABLE.put("lsquo", Character.valueOf('\u2018'));
      ENTITY_TABLE.put("rsquo", Character.valueOf('\u2019'));
      ENTITY_TABLE.put("sbquo", Character.valueOf('\u201a'));
      ENTITY_TABLE.put("ldquo", Character.valueOf('\u201c'));
      ENTITY_TABLE.put("rdquo", Character.valueOf('\u201d'));
      ENTITY_TABLE.put("bdquo", Character.valueOf('\u201e'));
      ENTITY_TABLE.put("dagger", Character.valueOf('\u2020'));
      ENTITY_TABLE.put("Dagger", Character.valueOf('\u2021'));
      ENTITY_TABLE.put("permil", Character.valueOf('\u2030'));
      ENTITY_TABLE.put("lsaquo", Character.valueOf('\u2039'));
      ENTITY_TABLE.put("rsaquo", Character.valueOf('\u203a'));
      ENTITY_TABLE.put("euro", Character.valueOf('\u20ac'));
    }
  }

  /**
   * A mutable equivalent of FilePosition.
   * This allows us to keep file positions for "real" tokens, but without
   * incurring the cost of instantiating an object per character.
   */
  public static class MutableFilePosition {
    InputSource source;
    int lineNo;
    int charInFile;
    int charInLine;

    public MutableFilePosition() {
      // Must be copied into or otherwise filled before being used to create
      // a file position
    }

    public MutableFilePosition(FilePosition pos) {
      this(pos.source(), pos.startLineNo(), pos.startCharInFile(),
           pos.startCharInLine());
    }

    public MutableFilePosition(InputSource source) {
      this(source, 1, 1, 1);
    }

    public MutableFilePosition(
        InputSource source, int lineNo, int charInFile, int charInLine) {
      this.source = source;
      this.lineNo = lineNo;
      this.charInFile = charInFile;
      this.charInLine = charInLine;
    }
    public FilePosition toFilePosition() {
      return toFilePosition(0);
    }

    public FilePosition toFilePosition(int logicalDelta) {
      return FilePosition.instance(
          source, lineNo, lineNo + logicalDelta, charInFile, charInLine);
    }

    public void copyTo(MutableFilePosition posBuf) {
      posBuf.source = this.source;
      posBuf.lineNo = this.lineNo;
      posBuf.charInFile = this.charInFile;
      posBuf.charInLine = this.charInLine;
    }

    @Override
    public String toString() {
      String sourceStr = null != source ? source.toString() : "<unknown>";
      return (sourceStr.substring(sourceStr.lastIndexOf(File.separator) + 1)
              + ":" + this.lineNo + "+" + charInLine + "@" + charInFile);
    }
  }
}
