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

package com.google.caja.ancillary.servlet;

import com.google.caja.util.ContentType;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * Content that can be sent over the wire.
 *
 * @author mikesamuel@gmail.com
 */
final class Content {
  private final String codeUnits;
  private byte[] bytes;
  final ContentType type;

  /** Textual content. */
  Content(String codeUnits, ContentType type) {
    assert type == null || type.isText;
    if (codeUnits == null) { throw new NullPointerException(); }
    this.codeUnits = codeUnits;
    this.bytes = null;
    this.type = type;
  }
  /** Binary content. */
  Content(byte[] bytes, ContentType type) {
    assert type == null || !type.isText;
    if (bytes == null) { throw new NullPointerException(); }
    this.codeUnits = null;
    this.bytes = bytes;
    this.type = type;
  }

  void toWriter(Writer w) throws IOException {
    if (codeUnits == null) { throw new IOException("Cannot write byte data"); }
    w.write(codeUnits);
  }

  void toOutputStream(OutputStream o) throws IOException {
    if (bytes != null) {
      o.write(bytes);
    } else {
      Writer w = new OutputStreamWriter(o, "UTF-8");
      toWriter(w);
      w.flush();
    }
  }

  long byteLength() {
    if (bytes == null) {
      try {
        bytes = codeUnits.getBytes("UTF-8");
      } catch (UnsupportedEncodingException ex) {
        throw new RuntimeException(ex);
      }
    }
    return bytes.length;
  }

  String getText() throws IOException {
    if (codeUnits == null) { throw new IOException("cannot read binary file"); }
    return codeUnits;
  }

  boolean isText() { return codeUnits != null; }
}
