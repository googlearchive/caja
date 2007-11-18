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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
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

    public static CharProducer create(InputSource src) throws IOException {
      URL url = src.getUri().toURL();
      URLConnection conn = url.openConnection();
      conn.setDoInput(true);
      conn.setDoOutput(false);
      conn.setAllowUserInteraction(false);
      conn.connect();
      String encoding = conn.getContentEncoding();
      // TODO(mikesamuel): Make a better guess at encoding.  We have a few
      // options.
      // (1) Look for coding attribute in the -*- emacs prologue -*- per
      //     http://www.gnu.org/software/emacs/manual/html_mono/emacs.html#Recognize-Coding
      // (2) Use the system encoding
      // (3) Guess encoding by looking at char distribution.
      // (4) Reject responses that don't specify an encoding
      // (5) Take a default encoding parameter and assume the caller knows more
      //     than we do.
      // I'm leaning towards a combination of (1), (3) by looking for a BOM,
      // and (2).
      if (null == encoding) { encoding = "UTF-8"; }
      Reader r = new InputStreamReader(conn.getInputStream(), encoding);
      return create(r, src);
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
            // fallthru
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

    /**
     * A CharProducer that wraps another CharProducer to provide a finite amount
     * of lookahead.
     */
    private abstract static class LookaheadCharProducer
        implements CharProducer {
      /**
       * The characters that have been read from p but not yet consumed are in
       * {@code lookahead[lookaheadPos : lookaheadLimit]}.
       */
      final int[] lookahead;
      /** Positions corresponding to the lookehead chars. */
      final MutableFilePosition[] lookaheadLoc;
      /**
       * The position of the first character in lookahead that has been read
       * from p but which has not yet been consumed.
       * If lookaheadLimit == lookaheadPos, then there is no such char.
       */
      int lookaheadPos;
      /**
       * The position past the last character in lookahead that has been read
       * from p but which has not yet been consumed.
       */
      int lookaheadLimit;
      /** The underlying char producer. */
      final CharProducer p;

      LookaheadCharProducer(CharProducer p, int lookaheadLength) {
        this.lookahead = new int[lookaheadLength];
        this.lookaheadLoc = new MutableFilePosition[lookaheadLength];
        for (int i = lookaheadLoc.length; --i >= 0;) {
          lookaheadLoc[i] = new MutableFilePosition();
        }
        this.p = p;
      }

      public FilePosition getCurrentPosition() {
        FilePosition fp;
        if (lookaheadLimit == lookaheadPos) {
          fp = p.getCurrentPosition();
        } else {
          fp = lookaheadLoc[lookaheadPos].toFilePosition();
        }
        return fp;
      }

      public boolean getCurrentPosition(MutableFilePosition posBuf) {
        boolean result;
        if (lookaheadLimit == lookaheadPos) {
          result = p.getCurrentPosition(posBuf);
        } else {
          lookaheadLoc[lookaheadPos].copyTo(posBuf);
          result = true;
        }
        return result;
      }

      public void close() throws IOException { p.close(); }

      /** Fetch n characters into the lookahead buffer. */
      void lookahead(int n) throws IOException {
        int limit;
        if (lookaheadLimit == lookaheadPos) {
          lookaheadLimit = lookaheadPos = 0;
          limit = n;
        } else {
          int nInLookahead = lookaheadLimit - lookaheadPos;
          if (lookaheadPos + n > lookahead.length) {
            // need more space, so shift stuff back
            System.arraycopy(
                lookahead, lookaheadPos, lookahead, 0, nInLookahead);
            System.arraycopy(
                lookaheadLoc, lookaheadPos, lookaheadLoc, 0, nInLookahead);
            lookaheadLimit = nInLookahead;
            lookaheadPos = 0;
            limit = n;
          } else {
            limit = lookaheadPos + n;
          }
        }
        while (lookaheadLimit < limit) {
          p.getCurrentPosition(lookaheadLoc[lookaheadLimit]);
          lookahead[lookaheadLimit++] = p.read();
        }
      }

      int readOne() throws IOException {
        return (lookaheadPos == lookaheadLimit)
            ? p.read()
            : lookahead[lookaheadPos++];
      }

      int unescapeHex(int start, int end, int toConsumeTo, int fallback) {
        if (start == end) { return fallback; }
        int codePoint = 0;
        for (int k = start; k < end; ++k) {
          int ch = lookahead[k];
          switch (ch) {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
              codePoint = (codePoint << 4) | (ch - '0');
              break;
            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
              codePoint = (codePoint << 4) | (ch - 'a' + 10);
              break;
            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
              codePoint = (codePoint << 4) | (ch - 'A' + 10);
              break;
            default:
              // invalid unicode literal.  leave the rest in lookahead
              return fallback;
          }
        }
        lookaheadPos = toConsumeTo;
        return codePoint;
      }

      int unescapeOctal(int start, int end) {
        int codePoint = 0;
        for (int k = start; k < end; ++k) {
          int ch = lookahead[k];
          if ('0' <= ch && '7' >= ch) {
            codePoint = (codePoint << 3) | (ch - '0');
          } else {
            // end of octal literal.  leave the rest in lookahead
            end = k;
            break;
          }
        }
        lookaheadPos = end;  // consume characters from lookahead
        return codePoint;
      }

      int unescapeDecimal(int start, int end, int toConsumeTo, int fallback) {
        int codePoint = 0;
        for (int k = start; k < end; ++k) {
          int ch = lookahead[k];
          switch (ch) {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
              codePoint = (codePoint * 10) + (ch - '0');
              break;
            default:
              // invalid unicode literal.  leave the rest in lookahead
              return fallback;
          }
        }
        lookaheadPos = toConsumeTo;  // consume characters from lookahead
        return codePoint;
      }

      void debugLookahead() {
        System.err.print(
            "pos=" + lookaheadPos + ", limit=" + lookaheadLimit + "[");
        for (int i = 0; i < lookahead.length; ++i) {
          System.err.print(lookahead[i] + ",");
        }
        System.err.println("]");
      }
    }

    private static class JsUnEscapeWrapper extends LookaheadCharProducer {
      JsUnEscapeWrapper(CharProducer p) { super(p, 4); }

      public int read() throws IOException {
        int ch = readOne();
        if ('\\' != ch) { return ch; }
        // we've found an escaped character.
        lookahead(1);
        int ch2 = lookahead[lookaheadPos++];
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
            lookahead(nHex);
            int end = lookaheadPos + nHex;
            return unescapeHex(lookaheadPos, end, end, ch2);
          }
          // octal escapes in 0-377
          case '0': case '1': case '2': case '3':
          case '4': case '5': case '6': case '7':
          {
            --lookaheadPos;  // Move ch2 back into the live region of lookahead
            lookahead(3);  // Make sure we have 3 digits to perform lookahead on
            return unescapeOctal(
                lookaheadPos,
                // We only accept octal literals in the range 0-377, so clip
                // one character from limit if the first character is >= '4'
                ch2 <= '3' ? lookaheadLimit : lookaheadLimit - 1);
          }
          default:
            return ch2;
        }
      }
    }

    private static class HtmlUnEscapeWrapper extends LookaheadCharProducer {
      HtmlUnEscapeWrapper(CharProducer p) { super(p, 7); }

      public int read() throws IOException {
        int ch = readOne();
        if ('&' != ch) { return ch; }
        // We've found an escaped character.
        lookahead(7);

        int end = -1;
        for (int i = lookaheadPos + 1; i < lookaheadLimit; ++i) {
          if (';' == lookahead[i]) {
            end = i;
            break;
          }
        }
        if (end < 0) { return ch; }
        // Now we know where the entity ends, and that there is at least one
        // character in the entity name

        // We only handle the xml escapes and apos for now.
        int ch2 = lookahead[lookaheadPos];
        if ('#' == ch2) {
          // numeric entity
          int k = lookaheadPos + 1;
          if ('x' == lookahead[k] || 'X' == lookahead[k]) {
            // hex literal
            return unescapeHex(++k, end, end + 1, ch);
          } else {
            // decimal literal
            return unescapeDecimal(k, end, end + 1, ch);
          }
        } else {
          StringBuilder sb = new StringBuilder(end - lookaheadPos);
          for (int i = lookaheadPos; i < end; ++i) {
            sb.append(Character.toLowerCase((char) lookahead[i]));
          }
          Character c = ENTITY_TABLE.get(sb.toString());
          if (null == c) { return ch; }
          lookaheadPos = end + 1;  // Consume lookahead characters
          return c.charValue();
        }
      }
    }

    private static final Map<String, Character> ENTITY_TABLE =
      new HashMap<String, Character>();
    static {
      ENTITY_TABLE.put("lt", Character.valueOf('<'));
      ENTITY_TABLE.put("gt", Character.valueOf('>'));
      ENTITY_TABLE.put("apos", Character.valueOf('\''));
      ENTITY_TABLE.put("quot", Character.valueOf('"'));
      ENTITY_TABLE.put("amp", Character.valueOf('&'));
      ENTITY_TABLE.put("nbsp", Character.valueOf('\u00a0'));
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
