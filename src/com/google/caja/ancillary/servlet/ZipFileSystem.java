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

import com.google.caja.ancillary.jsdoc.FileSystem;
import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.util.Maps;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A virtual file system backed by an in-memory ZIP file.
 *
 * @author mikesamuel@gmail.com
 */
class ZipFileSystem implements FileSystem {
  private final String root;
  private final URI rootUri;
  private final Map<String, Content> files = Maps.newLinkedHashMap();

  private static final Pattern BAD_PATH = Pattern.compile(
      "(?:^|/)(?:\\.\\.?)(?:/|$)");
  private static final Content DIRECTORY = null;

  /**
   * @param root an absolute directory under which all files must be organized.
   *    E.g. {@code /foo}.
   */
  ZipFileSystem(String root) {
    if (!root.startsWith("/") || root.endsWith("/")) {
      throw new IllegalArgumentException(root);
    }
    this.root = root;
    this.rootUri = fileUriWithPath(root + "/");
    files.put("/", DIRECTORY);
    files.put(root, DIRECTORY);
  }
  private static URI fileUriWithPath(String path) {
    try {
      return new URI("file", null, path, null, null);
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
  }

  public String basename(String path) {
    int end = path.length();
    while (end > 0 && path.charAt(end - 1) == '/') { --end; }
    if (end == 0 && path.length() != 0) { return "/"; }
    int lastSlash = path.lastIndexOf('/', end - 1);
    return path.substring(lastSlash + 1, end);
  }

  public String canonicalPath(final String path) throws IOException {
    URI canon = rootUri.resolve(path);
    if (canon == null) { throw new IOException(path); }
    String canonPath = canon.getPath();
    if (BAD_PATH.matcher(canonPath).find()) { throw new IOException(path); }
    canonPath = canonPath.replaceAll("/{2,}", "/");
    if (canonPath.endsWith("/") && !"/".equals(canonPath)) {
      canonPath = canonPath.substring(0, canonPath.length() - 1);
    }
    if (root.equals(canonPath) || canonPath.startsWith(root + "/")) {
      return canonPath;
    } else {
      throw new IOException(path);
    }
  }

  public String dirname(String path) {
    int end = path.length();
    while (end > 0 && path.charAt(end - 1) == '/') { --end; }
    if (end == 0 && path.length() != 0) { return "/"; }

    int lastSlash = path.lastIndexOf('/', end - 1);
    if (lastSlash < 0) { return null; }
    if (lastSlash == 0) { return "/"; }
    return path.substring(0, lastSlash);
  }

  public boolean exists(String path) {
    try { path = canonicalPath(path); } catch (IOException ex) { return false; }
    return files.containsKey(path);
  }

  public boolean isDirectory(String path) {
    try { path = canonicalPath(path); } catch (IOException ex) { return false; }
    return files.containsKey(path) && DIRECTORY == files.get(path);
  }

  public boolean isFile(String path) {
    try { path = canonicalPath(path); } catch (IOException ex) { return false; }
    return files.get(path) != DIRECTORY;
  }

  public String join(String dir, String path) {
    if (path == null) { throw new NullPointerException(); }
    if (path.startsWith("/")) { throw new IllegalArgumentException(path); }
    if (dir == null || "".equals(dir)) { return path; }
    if ("".equals(path)) { return dir; }
    return (dir + "/" + path).replace("//", "/");
  }

  private void requireParent(String path) throws IOException {
    String parent = dirname(path);
    if (!(files.containsKey(parent) && files.get(parent) == DIRECTORY)) {
      throw new IOException(parent + " is not a directory");
    }
  }

  public void mkdir(String path) throws IOException {
    path = canonicalPath(path);
    requireParent(path);
    if (files.get(path) != DIRECTORY) {
      throw new IOException(path + " is a file");
    }
    files.put(path, null);
  }

  public CharProducer read(String path) throws IOException {
    path = canonicalPath(path);
    Content content = files.get(path);
    if (content != DIRECTORY) {
      return CharProducer.Factory.fromString(
          content.getText(), toInputSource(path));
    } else {
      throw new FileNotFoundException(path);
    }
  }

  public InputSource toInputSource(String path) {
    return new InputSource(fileUriWithPath(path));
  }

  public Writer write(String path) throws IOException {
    path = canonicalPath(path);
    requireParent(path);
    if (files.containsKey(path) && files.get(path) == DIRECTORY) {
      throw new IOException(path + " is a directory");
    }
    files.put(path, new Content("", null));
    final String outPath = path;
    return new StringWriter() {
      @Override
      public void close() throws IOException {
        super.close();
        files.put(outPath, new Content(this.toString(), null));
      }
    };
  }

  public OutputStream writeBytes(String path) throws IOException {
    path = canonicalPath(path);
    requireParent(path);
    if (files.containsKey(path) && files.get(path) == DIRECTORY) {
      throw new IOException(path + " is a directory");
    }
    files.put(path, new Content("", null));
    final String outPath = path;
    return new ByteArrayOutputStream() {
      @Override
      public void close() throws IOException {
        super.close();
        files.put(outPath, new Content(this.toByteArray(), null));
      }
    };
  }

  public Job toZip() throws IOException {
    ByteArrayOutputStream zippedBytes = new ByteArrayOutputStream();
    ZipOutputStream zipOut = new ZipOutputStream(zippedBytes);
    for (Map.Entry<String, Content> file : files.entrySet()) {
      String path = file.getKey();
      Content content = file.getValue();
      if (content == DIRECTORY) {
        if ("/".equals(path)) { continue; }
        // Zip file format treats paths that end in "/" as dirs.
        ZipEntry ze = new ZipEntry(path + "/");
        zipOut.putNextEntry(ze);
      } else {
        ZipEntry ze = new ZipEntry(path);
        ze.setSize(content.byteLength());
        zipOut.putNextEntry(ze);
        content.toOutputStream(zipOut);
      }
    }
    zipOut.close();
    return Job.zip(zippedBytes.toByteArray());
  }
}
