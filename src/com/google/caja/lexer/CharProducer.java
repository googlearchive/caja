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

import com.google.caja.SomethingWidgyHappenedError;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;

import java.util.Arrays;

import javax.annotation.WillClose;

/**
 * A character reader that tracks character file position information.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class CharProducer implements CharSequence, Cloneable {
  private int offset;
  private final int limit;
  private final char[] buf;

  CharProducer(char[] buf, int limit) {
    this.buf = buf;
    this.limit = limit;
  }

  /**
   * The count of consumed characters in the {@link #getBuffer char buffer}.
   * If {@code offset == limit} then the end of input has been reached.
   */
  public final int getOffset() { return offset; }
  /**
   * The count of valid and unconsumed characters in {@link #getBuffer buffer}.
   */
  public final int getLimit() { return limit; }
  /**
   * A buffer which contains un{@link #consume consumed} characters in indices
   * [offset(), limit() - 1].
   * Buffers should not be modified by clients of this class.
   */
  public final char[] getBuffer() { return buf; }
  /**
   * Updates the offset and limit so that the first n unconsumed characters
   * are changed to consumed.
   */
  public final void consume(int n) {
    consumeTo(offset + n);
  }
  public final void consumeTo(int end) {
    assert offset <= end && end <= limit;
    offset = end;
  }
  /**
   * @param start an offset in [0, limit].
   * @param end an offset in [start, limit].
   * @return a String of the characters in {@code buf[start:end]}.
   */
  public final String toString(int start, int end) {
    return String.valueOf(buf, start, end - start);
  }

  @Override
  public final String toString() {
    return toString(offset, limit);
  }

  /** Number of characters available in buffer between the offset and limit. */
  public final int getLength() { return limit - offset; }

  /** True iff the {@link #getOffset offset} is at the end of the input. */
  public final boolean isEmpty() { return offset == limit; }

  /** The index of the character at {@code getBuffer()[offset]} in the input. */
  public abstract int getCharInFile(int offset);
  /** The source breaks associated with {@code getBuffer()[offset]}. */
  public abstract SourceBreaks getSourceBreaks(int offset);

  public FilePosition getCurrentPosition() {
    return getSourceBreaks(offset).toFilePosition(getCharInFile(offset));
  }

  public FilePosition filePositionForOffsets(int start, int end) {
    return getSourceBreaks(start)
        .toFilePosition(getCharInFile(start), getCharInFile(end));
  }

  public CharSequence subSequence(int start, int end) {
    if (end > limit || start < 0 || end < start) {
      throw new IndexOutOfBoundsException();
    }
    return new BufferBackedSequence(buf, start + offset, end + offset);
  }

  public final int length() { return limit - offset; }  // For CharProducer
  public char charAt(int i) {
    if (i < 0 || (i += offset) >= limit) {
      throw new IndexOutOfBoundsException();
    }
    return buf[i];
  }

  /** Returns a distinct instance initialized with the same offset and limit. */
  @Override
  public abstract CharProducer clone();

  /**
   * Convenience methods for creating producers.
   */
  public static final class Factory {
    /**
     * @param r read and closed as a side-effect of this operation.
     */
    public static CharProducer create(@WillClose Reader r, FilePosition pos)
        throws IOException {
      int limit = 0;
      char[] buf = new char[4096];
      try {
        for (int n = 0; (n = r.read(buf, limit, buf.length - limit)) > 0;) {
          limit += n;
          if (limit == buf.length) {
            char[] newBuf = new char[buf.length * 2];
            System.arraycopy(buf, 0, newBuf, 0, limit);
            buf = newBuf;
          }
        }
      } finally {
        r.close();
      }
      return new CharProducerImpl(buf, limit, pos);
    }

    public static CharProducer fromFile(File f, String encoding)
        throws IOException {
      return fromFile(f, Charset.forName(encoding));
    }

    public static CharProducer fromFile(File f, Charset encoding)
        throws IOException {
      FileInputStream in = new FileInputStream(f);
      try {
        CharProducer cp = create(
            new InputStreamReader(in, encoding), new InputSource(f.toURI()));
        return cp;
      } finally {
        in.close();
      }
    }

    public static CharProducer fromString(CharSequence s, InputSource src) {
      return fromString(s, FilePosition.startOfFile(src));
    }

    public static CharProducer fromString(CharSequence s, FilePosition pos) {
      char[] buf;
      if (s instanceof String) {
        buf = ((String) s).toCharArray();
      } else {
        buf = new char[s.length()];
        for (int i = 0; i < buf.length; ++i) {
          buf[i] = s.charAt(i);
        }
      }
      return new CharProducerImpl(buf, buf.length, pos);
    }

    public static CharProducer create(@WillClose Reader r, InputSource src)
        throws IOException {
      return create(r, FilePosition.startOfFile(src));
    }

    public static CharProducer create(
        @WillClose StringReader r, InputSource src) {
      try {
        return create((Reader) r, FilePosition.startOfFile(src));
      } catch (IOException ex) {
        throw new SomethingWidgyHappenedError(
            "Error reading chars from String");
      }
    }

    public static CharProducer create(
        @WillClose StringReader r, FilePosition pos) {
      try {
        return create((Reader) r, pos);
      } catch (IOException ex) {
        throw new SomethingWidgyHappenedError(
            "Error reading chars from String");
      }
    }

    public static CharProducer fromJsString(@WillClose CharProducer p) {
      return DecodingCharProducer.make(new DecodingCharProducer.Decoder() {
        @Override
        void decode(char[] chars, int offset, int limit) {
          char ch = chars[offset];
          if ('\\' != ch || offset + 1 >= limit) {
            this.codePoint = ch;
            this.end = offset + 1;
            return;
          }
          // We've found an escaped character.
          int ch2 = chars[offset + 1];
  // for javascript escaping conventions see
  // http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Guide:Literals
          int codePoint;
          int end = offset + 2;
          switch (ch2) {
            case 'b': codePoint = '\b'; break;
            case 'r': codePoint = '\r'; break;
            case 'n': codePoint = '\n'; break;
            case 'f': codePoint = '\f'; break;
            case 't': codePoint = '\t'; break;
            case 'v': codePoint = '\u000b'; break;
            // unicode and hex escapes
            case 'u': case 'x':
            {
              int nHex = ch2 == 'u' ? 4 : 2;
              int hexStart = offset + 2;
              int hexEnd = offset + 2 + nHex;
              if (hexEnd <= limit
                  && decodeHex(chars, hexStart, hexEnd, hexEnd)) {
                return;
              }
              codePoint = ch2;
              break;
            }
            // octal escapes in 0-377
            case '0': case '1': case '2': case '3':
            case '4': case '5': case '6': case '7':
            {
              decodeOctal(
                  chars,
                  offset + 1,
                  // We only accept octal literals in the range 0-377, so clip
                  // one character from limit if the first character is >= '4'
                  offset + (ch2 <= '3' ? 4 : 3));
              return;
            }
            default:
              codePoint = ch2;
              break;
          }
          this.codePoint = codePoint;
          this.end = end;
        }
      }, p);
    }

    public static CharProducer fromHtmlAttribute(
        @WillClose CharProducer p) {
      return DecodingCharProducer.make(new DecodingCharProducer.Decoder() {
        @Override
        void decode(char[] chars, int offset, int limit) {
          long packedEndAndCodepoint = HtmlEntities.decodeEntityAt(
              chars, offset, limit);
          this.codePoint = (int) (packedEndAndCodepoint & 0xffffffL);
          this.end = (int) (packedEndAndCodepoint >>> 32);
        }
      }, p);
    }

    public static CharProducer fromUri(@WillClose CharProducer p) {
      return DecodingCharProducer.make(new UriDecoder(), p);
    }

    /**
     * A CharProducer that contains the concatenation of the given character
     * producers.  It produces all the characters in turn from its first
     * argument, and when that is exhausted proceeds to the next input.
     * The inputs are not consumed.
     */
    public static CharProducer chain(CharProducer... srcs) {
      if (srcs.length == 0) {
        return new CharProducerImpl(new char[0], 0, FilePosition.UNKNOWN);
      } else if (srcs.length == 1) {
        return srcs[0];
      }
      return ChainCharProducer.make(srcs);
    }

    private Factory() {
      // uninstantiable
    }

    private static final class CharProducerImpl extends CharProducer {
      private final SourceBreaks breaks;
      private final int charInFile;

      CharProducerImpl(char[] buf, int limit, FilePosition pos) {
        super(buf, limit);
        this.charInFile = pos.startCharInFile();
        this.breaks = new SourceBreaks(pos.source(), pos.startLineNo() - 1);
        this.breaks.lineStartsAt(charInFile - pos.startCharInLine() + 1);

        for (int i = 0; i < limit; ++i) {
          char ch = buf[i];
          if (ch == '\n'
              || (ch == '\r' && i + 1 < limit && buf[i + 1] != '\n')) {
            this.breaks.lineStartsAt(charInFile + i + 1);
          }
        }
      }

      private CharProducerImpl(CharProducerImpl orig) {
        super(orig.getBuffer(), orig.getLimit());
        this.breaks = orig.breaks;
        this.charInFile = orig.charInFile;
        this.consume(orig.getOffset());
      }

      @Override
      public int getCharInFile(int offset) {
        return charInFile + offset;
      }

      @Override
      public SourceBreaks getSourceBreaks(int offset) { return breaks; }

      @Override
      public CharProducer clone() {
        return new CharProducerImpl(this);
      }
    }
  }

  private static class ChainCharProducer extends CharProducer {
    private final int[] ends;
    private final CharProducer[] srcs;

    private ChainCharProducer(
        char[] concatenation, int[] ends, CharProducer... srcs) {
      super(concatenation, concatenation.length);
      this.ends = ends;
      this.srcs = srcs;
    }

    private ChainCharProducer(ChainCharProducer orig) {
      super(orig.getBuffer(), orig.getLimit());
      this.ends = orig.ends;
      this.srcs = orig.srcs;
      this.consume(orig.getOffset());
    }

    static CharProducer make(CharProducer... srcs) {
      int[] ends = new int[srcs.length];
      for (int i = 0; i < srcs.length; ++i) {
        CharProducer s = srcs[i];
        int length = s.getLimit() - s.getOffset();
        ends[i] = i != 0 ? ends[i - 1] + length : length;
      }
      char[] concatenation = new char[ends[ends.length - 1]];
      int pos = 0;
      for (CharProducer s : srcs) {
        int len = s.getLimit() - s.getOffset();
        System.arraycopy(s.getBuffer(), s.getOffset(), concatenation, pos, len);
        pos += len;
      }
      return new ChainCharProducer(concatenation, ends, srcs);
    }

    @Override
    public int getCharInFile(int offset) {
      int i = Arrays.binarySearch(ends, offset);
      if (i < 0) { i = ~i; }
      int prev = i == 0 ? 0 : ends[i - 1];
      return srcs[i].getCharInFile(offset - prev);
    }

    @Override
    public SourceBreaks getSourceBreaks(int offset) {
      int i = Arrays.binarySearch(ends, offset);
      if (i < 0) { i = ~i; }
      int prev = i == 0 ? 0 : ends[i - 1];
      return srcs[i].getSourceBreaks(offset - prev);
    }

    @Override
    public CharProducer clone() {
      return new ChainCharProducer(this);
    }
  }
}

class BufferBackedSequence implements CharSequence {
  private final int start;
  private final int end;
  private final char[] buf;

  BufferBackedSequence(char[] buf, int start, int end) {
    this.start = start;
    this.end = end;
    this.buf = buf;
  }

  public char charAt(int index) {
    return buf[start + index];
  }

  public int length() { return end - start; }

  public CharSequence subSequence(int start, int end) {
    if (start < 0 || end < start || this.start + end > this.end) {
      throw new IndexOutOfBoundsException();
    }
    if (start == end) { return ""; }
    if (start == 0 && end == this.end) { return this; }
    return new BufferBackedSequence(buf, start + this.start, end + this.start);
  }
}
