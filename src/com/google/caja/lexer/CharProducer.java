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
            sb.append(Character.toLowerCase((char) peek(i)));
          }
          Character c = ENTITY_TABLE.get(sb.toString());
          if (null == c) { return ch; }
          consume(end + 1);
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
