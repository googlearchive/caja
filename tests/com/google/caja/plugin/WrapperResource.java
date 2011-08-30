// Copyright (C) 2011 Google Inc.
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

package com.google.caja.plugin;

import org.mortbay.resource.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * A Resource that wraps another Resource and stubs out all but the most
 * crucial methods. Used to ensure that, when masquerading one Resource as
 * another, we don't introduce spuriously inconsistent semantics (like serving
 * it under some path but returning an inconsistent value from {@code getFile}).
 *
 * @author ihab.awad@gmail.com
 */
public class WrapperResource extends Resource {
  private final Resource delegate;

  public WrapperResource(Resource delegate) {
    this.delegate = delegate;
  }

  @Override
  public void release() { }
  
  @Override
  public boolean exists() { return true; }

  @Override
  public boolean isDirectory() { return false; }

  @Override
  public long lastModified() { return -1L; }

  @Override
  public long length() {
    return delegate.length();
  }

  @Override
  public URL getURL() {
    throw new UnsupportedOperationException();
  }

  @Override
  public File getFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return delegate.getInputStream();
  }

  @Override
  public OutputStream getOutputStream() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean delete() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean renameTo(Resource resource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String[] list() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Resource addPath(String s) {
    throw new UnsupportedOperationException();
  }
}
