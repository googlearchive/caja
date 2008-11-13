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

package com.google.caja.util;

import java.io.Reader;
import java.io.IOException;

/**
 * Given any {@code Reader}, captures the characters that are read and makes
 * them available for later use.
 *
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class CapturingReader extends Reader {
  public static final int DEFAULT_INITIAL_SIZE = 1024;

  private final Reader delegate;
  private StringBuffer buf;
  private CharSequence capture = null;

  /**
   * Creates a {@code CapturingReader}.
   *
   * @param delegate the underlying {@code Reader} to which reads are delegated.
   * @param initialSize the intial size of the buffer for the cache.
   */
  public CapturingReader(Reader delegate, int initialSize) {
    this.delegate = delegate;
    buf = new StringBuffer(initialSize);
  }

  /**
   * Creates a {@code CapturingReader} with a default size for the cache.
   *
   * @param delegate the underlying {@code Reader} to which reads are delegated.
   */
  public CapturingReader(Reader delegate) {
    this(delegate, DEFAULT_INITIAL_SIZE);
  }

  /**
   * @see java.io.Reader#read(char[], int, int) 
   */
  public int read(char[] chars, int off, int len) throws IOException {
    if (buf == null) { throw new IOException("Reader is closed"); }
    int bytesRead = delegate.read(chars, off, len);
    if (bytesRead < 0) { return bytesRead; }
    buf.append(chars, off, bytesRead);
    return bytesRead;
  }

  /**
   * @see java.io.Reader#close()
   */
  public void close() throws IOException {
    if (buf == null) { return; }
    capture = buf.toString();
    buf = null;
  }

  /**
   * Close the underlying {@code Reader} and return the captured contents.
   *
   * @return the captured characters.
   * @throws IOException if the underlying {@code Reader} threw an exception
   * when it was being closed.
   */
  public CharSequence getCapture() throws IOException {
    close();
    return capture;
  }
}
