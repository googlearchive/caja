// Copyright (C) 2010 Google Inc.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.util.Pair;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.mozilla.intl.chardet.nsPSMDetector;

/**
 * Utilities for dealing with converting byte streams with unknown character
 * sets to character streams.
 *
 * @author Mike Samuel <mikesamuel@gmail.com>
 */
public final class Chardet {
  private Chardet() { /* uninstantiable */ }

  private static final String UTF8 = "UTF-8";
  private static final String UTF16BE = "UTF-16BE";
  private static final String UTF16LE = "UTF-16LE";
  private static final String UTF32BE = "UTF-32BE";
  private static final String UTF32LE = "UTF-32LE";
  private static final String UTF7 = "UTF-7";
  private static final String UTF1 = "UTF-1";
  private static final String ISO_8859_1 = "ISO-8859-1";

  /**
   * Given a byte stream, figure out an encoding and return a character stream
   * and the encoding used to convert bytes to characters.
   */
  public static Pair<Reader, String> guessCharset(InputStream in)
      throws IOException {
    ByteArrayOutputStream buffered = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    boolean isAscii = true;
    int len = in.read(buf);
    if (len <= 0) { return Pair.pair((Reader) new StringReader(""), UTF8); }
    String charset = findCharset(buf, len);
    if (charset != null) {
      // If the charset is specified in the document, use that.
      buffered.write(buf, 0, len);
    // Otherwise, look for a BOM at the start of the content.
    } else if (hasUtf8BOM(buf, len)) {
      charset = UTF8;
      buffered.write(buf, 3, len - 3);
    // Check UTF32 before UTF16 since a little endian UTF16 BOM is a prefix of
    // a little endian UTF32 BOM.
    } else if (hasUtf32BEBOM(buf, len)) {
      charset = UTF32BE;
      buffered.write(buf, 4, len - 4);
    } else if (hasUtf32LEBOM(buf, len)) {
      charset = UTF32LE;
      buffered.write(buf, 4, len - 4);
    } else if (hasUtf16BEBOM(buf, len)) {
      charset = UTF16BE;
      buffered.write(buf, 2, len - 2);
    } else if (hasUtf16LEBOM(buf, len)) {
      charset = UTF16LE;
      buffered.write(buf, 2, len - 2);
    } else if (hasUtf7BOM(buf, len)) {
      charset = UTF7;
      buffered.write(buf, 4, len - 4);
    } else if (hasUtf1BOM(buf, len)) {
      charset = UTF1;
      buffered.write(buf, 3, len - 3);
    } else {
      // Use jchardet which tries a variety of heuristics to choose an encoding.
      nsDetector det = new nsDetector(nsPSMDetector.ALL);
      // The below is adapted from the main method in HtmlCharsetDetector.
      Observer observer = new Observer();
      det.Init(observer);
      do {
        buffered.write(buf, 0, len);
        if (isAscii) { isAscii = det.isAscii(buf, len); }
        if (!isAscii) {
          if (det.DoIt(buf, len, false)) { break; }
        }
      } while ((len = in.read(buf)) > 0);
      det.DataEnd();
      charset = observer.charset;
    }
    if (charset != null) { charset = supportedCharsetName(charset); }
    if (charset == null) { charset = UTF8; }
    return Pair.pair(
        joinStreamsWithCharset(buffered.toByteArray(), in, charset),
        charset);
  }

  static final class Observer implements nsICharsetDetectionObserver {
    String charset;
    public void Notify(String charset) {
      this.charset = charset;
    }
  }

  private static final byte[] CHARSET_BYTES;
  private static final byte[] ENCODING_BYTES;
  static {
    try {
      CHARSET_BYTES = "charset".getBytes(ISO_8859_1);
      ENCODING_BYTES = "encoding".getBytes(ISO_8859_1);
    } catch (UnsupportedEncodingException ex) {
      throw new SomethingWidgyHappenedError(ex);
    }
  }

  /**
   * Looks for sequences like {@code charset="..."} inside angle brackets to
   * match {@code <meta value="text/html;charset=...">} and after {@code <?}
   * sequences like {@code encoding="..."} to match XML prologs.
   */
  private static String findCharset(final byte[] buf, final int len) {
    for (int i = 0; i < len; ++i) {
      if ('<' != buf[i]) { continue; }
      byte lastByte = '<';
      byte[] attrBytes = CHARSET_BYTES;
      // Now we're inside <, so look for attrBytes.
      for (int j = i + 1, n = len; j < n; ++j) {
        byte b = buf[j];
        if (b == 0) { continue; }
        if (b == '?' && lastByte == '<') { attrBytes = ENCODING_BYTES; }
        if ((b | 0x20) == attrBytes[0] && !isAlnum(lastByte)) {
          int wordLen = attrBytes.length;
          int pos = j + 1, k = 1;
          // Match attrBytes against buf[pos:]
          while (pos < n && k < wordLen) {
            b = buf[pos];
            if (b == 0 || b == '-') {  // Skip over NULs in UTF-16 and UTF-32.
              ++pos;
            } else if ((b | 0x20) == attrBytes[k]) {
              ++k;
              ++pos;
            } else {
              break;
            }
          }
          if (k == wordLen) {
            // Now we've found the attribute or parameter name.
            // Skip over spaces and NULs looking for '='
            while (pos < len) {
              b = buf[pos];
              if (b == '=') {
                // Skip over spaces and NULs looking for alnum or quote.
                while (++pos < len) {
                  b = buf[pos];
                  if (b == 0 || isSpace(b)) { continue; }
                  int start;
                  if (b == '"' || b == '\'') {
                    start = pos + 1;
                  } else if (isAlnum(b)) {
                    start = pos;
                  } else {
                    break;
                  }
                  int end = start;
                  boolean sawLetter = false;
                  // Now, find the end of the charset.
                  while (end < len) {
                    b = buf[end];
                    if (b == 0 || b == '-' || b == '_') {
                      ++end;
                    } else if (isAlnum(b)) {
                      sawLetter = true;
                      ++end;
                    } else {
                      break;
                    }
                  }
                  if (sawLetter) {
                    StringBuilder sb = new StringBuilder(end - start);
                    for (int bi = start; bi < end; ++bi) {
                      if (buf[bi] != 0) { sb.append((char) buf[bi]); }
                    }
                    // Only use the charset if it's recognized.
                    // Otherwise, we continue looking.
                    String charset = supportedCharsetName(sb.toString());
                    if (charset != null) { return charset; }
                  }
                }
                break;
              }
              if (b != 0 && !isSpace(b)) {
                break;
              }
              ++pos;
            }
          }
          if (b == '<' || b == '>') {
            i = pos - 1;
            break;
          }
        } else if (b == '<' || b == '>') {
          i = j - 1;
          break;
        }
        lastByte = buf[j];
      }
    }
    return null;
  }

  /**
   * Produces a character stream from an underlying byte stream.
   * @param buffered lookahead bytes read from tail.
   * @param tail the unread portion of the stream
   * @param charset the character set to use to decode the bytes in buffered and
   *     tail.
   */
  private static Reader joinStreamsWithCharset(
      byte[] buffered, InputStream tail, String charset)
      throws IOException {
    return new InputStreamReader(new JoinedStream(buffered, tail), charset);
  }

  static final class JoinedStream extends InputStream {
    byte[] buffered;
    int pos;
    final InputStream tail;

    JoinedStream(byte[] buffered, InputStream tail) {
      this.buffered = buffered;
      this.tail = tail;
    }

    @Override
    public int read() throws IOException {
      if (buffered != null) {
        if (pos < buffered.length) { return buffered[pos++]; }
        buffered = null;
      }
      return tail.read();
    }

    @Override
    public int read(byte[] out, int off, int len) throws IOException {
      int nRead = 0;
      if (buffered != null) {
        int avail = buffered.length - pos;
        if (avail != 0) {
          int k = Math.min(len, avail);
          int p1 = pos + k;
          int p2 = off + k;
          pos = p1;
          while (--p2 >= off) { out[p2] = buffered[--p1]; }
          off += k;
          len -= k;
          nRead = k;
        } else {
          buffered = null;
        }
      }
      if (len == 0) { return nRead; }
      int nFromTail = tail.read(out, off, len);
      if (nFromTail > 0) { return nFromTail + nRead; }
      return nRead != 0 ? nRead : -1;
    }

    @Override
    public void close() throws IOException {
      buffered = null;
      tail.close();
    }
  }


  private static boolean isAlnum(byte b) {
    if (b < '0' || b > 'z') { return false; }
    if (b < 'A') { return b <= '9'; }
    return b >= 'a' || b <= 'Z';
  }

  private static boolean isSpace(byte b) {
    return b <= ' '
        && (b == ' ' || b == '\r' || b == '\n' || b == '\t' || b == '\f');
  }

  static String supportedCharsetName(String s) {
    try {
      return Charset.forName(s).name();
    } catch (UnsupportedCharsetException ex) {
      return null;
    } catch (IllegalCharsetNameException ex) {
      return null;
    }
  }

  private static final byte
      _00 = (byte) 0,
      _2B = (byte) 0x2b,
      _2F = (byte) 0x2f,
      _38 = (byte) 0x38,
      _39 = (byte) 0x39,
      _4C = (byte) 0x4c,
      _64 = (byte) 0x64,
      _76 = (byte) 0x76,
      _BB = (byte) 0xbb,
      _BF = (byte) 0xbf,
      _EF = (byte) 0xef,
      _F7 = (byte) 0xf7,
      _FE = (byte) 0xfe,
      _FF = (byte) 0xff;

  // See http://en.wikipedia.org/wiki/Byte_order_mark for a table of byte
  // sequences.
  private static boolean hasUtf8BOM(byte[] b, int len) {
    return len >= 3 && b[0] == _EF && b[1] == _BB && b[2] == _BF;
  }

  private static boolean hasUtf16BEBOM(byte[] b, int len) {
    return len >= 2 && b[0] == _FE && b[1] == _FF;
  }

  private static boolean hasUtf16LEBOM(byte[] b, int len) {
    return len >= 2 && b[0] == _FF && b[1] == _FE;
  }

  private static boolean hasUtf32BEBOM(byte[] b, int len) {
    return len >= 4 && b[0] == _00 && b[1] == _00
        && b[2] == _FE && b[3] == _FF;
  }

  private static boolean hasUtf32LEBOM(byte[] b, int len) {
    return len >= 4 && b[0] == _FF && b[1] == _FE
        && b[2] == _00 && b[3] == _00;
  }

  private static boolean hasUtf7BOM(byte[] b, int len) {
    if (len < 4 || b[0] != _2B || b[1] != _2F || b[2] != _76) {
      return false;
    }
    byte b3 = b[3];
    return b3 == _38 || b3 == _39 || b3 == _2B || b3 == _2F;
  }

  private static boolean hasUtf1BOM(byte[] b, int len) {
    return len >= 3 && b[0] == _F7 && b[1] == _64 && b[2] == _4C;
  }
}
