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

import com.google.caja.SomethingWidgyHappenedError;
import org.mortbay.resource.Resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;

/**
 * A Resource that rewrites textual content based on regular expressions.
 *
 * @author ihab.awad@gmail.com
 */
public class RewriteResource extends Resource {
  private final Resource delegate;
  private final String match;
  private final String replace;

  public RewriteResource(Resource delegate,
                         String match,
                         String replace) {
    this.delegate = delegate;
    this.match = match;
    this.replace = replace;
  }

  @Override
  public void release() { delegate.release(); }

  @Override
  public boolean exists() {
    // if (!delegate.exists()), we've got problems, but oh well.
    return delegate.exists();
  }

  @Override
  public boolean isDirectory() {
    // If (delegate.isDirectory()), we've got problems, but oh well.
    return delegate.isDirectory();
  }

  @Override
  public long lastModified() { return delegate.lastModified(); }

  @Override
  public long length() {
    try {
      return getContent().getBytes("UTF-8").length;
    } catch (IOException e) {
      throw new SomethingWidgyHappenedError(e);
    }
  }

  @Override
  public URL getURL() {
    // Don't return the URL directly because the client may go around us
    // and read it without applying the rewrite. Fail early instead.
    throw new UnsupportedOperationException();
  }

  @Override
  public File getFile() {
    // Don't return the File directly because the client may go around us
    // and read it without applying the rewrite. Fail early instead.
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() { return delegate.getName(); }

  @Override
  public InputStream getInputStream() throws IOException {
    // Dealing only in bytes; encoding issues left to the caller
    return new ByteArrayInputStream(getContent().getBytes("UTF-8"));
  }

  @Override
  public OutputStream getOutputStream() {
    // We don't support 2-way rewriting, so this method can only be the
    // beginning of trouble. Fail early.
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean delete() { return delegate.delete(); }

  @Override
  public boolean renameTo(Resource resource) {
    // If renamed to something else (whatever that means), how will this affect
    // the rewriting? Better to fail early.
    throw new UnsupportedOperationException();
  }

  @Override
  public String[] list() {
    // Expecting delegate.list() to work correctly means that this Resource
    // did not represent a plain file in the first place. Weird -> fail early.
    throw new UnsupportedOperationException();
  }

  @Override
  public Resource addPath(String s)
      throws IOException {
    return delegate.addPath(s);
  }

  private String getContent() throws IOException {
    // All Caja source and generated code is safe to interpret as UTF-8
    Reader in = new InputStreamReader(delegate.getInputStream(), "UTF-8");
    StringBuilder sb = new StringBuilder();
    for (int i; (i = in.read()) != -1;) {
      sb.append((char) i);
    }
    return sb.toString().replaceAll(match, replace);
  }
}
