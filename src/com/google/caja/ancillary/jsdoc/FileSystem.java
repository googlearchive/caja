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

package com.google.caja.ancillary.jsdoc;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/** Abstracts away the file system. */
public interface FileSystem {
  String basename(String path);
  String canonicalPath(String path) throws IOException;
  String dirname(String path);
  boolean exists(String path);
  boolean isFile(String path);
  boolean isDirectory(String path);
  /**
   * Returns a path of the file path relative to dir.
   * @return path if dir is the empty string.  Otherwise the concatenation of
   *     the two paths with the appropriate separator in between.
   */
  String join(String dir, String path);
  CharProducer read(String path) throws IOException;
  InputSource toInputSource(String path);
  void mkdir(String path) throws IOException;
  OutputStream writeBytes(String path) throws IOException;
  Writer write(String path) throws IOException;
}
