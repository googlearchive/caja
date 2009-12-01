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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import junit.framework.TestCase;

public class ZipFileSystemTest extends TestCase {
  public final void testCanonFile() throws IOException {
    ZipFileSystem zfs = new ZipFileSystem("/foo");
    assertEquals("/foo", zfs.canonicalPath(""));
    assertEquals("/foo", zfs.canonicalPath("."));
    assertEquals("/foo", zfs.canonicalPath("bar/.."));
    assertEquals("/foo/bar", zfs.canonicalPath("bar"));
    assertEquals("/foo/baz", zfs.canonicalPath(".//baz"));
    assertEquals("/foo/baz", zfs.canonicalPath(".//baz/"));
    assertEquals("/foo/bar/baz", zfs.canonicalPath("/foo/bar/baz"));
    assertEquals("/foo/bar/baz", zfs.canonicalPath("/foo/bar/baz/"));
    try {
      zfs.canonicalPath("..");
      fail();
    } catch (IOException ex) {
      // ok
    }
    try {
      zfs.canonicalPath("foo/../..");
      fail();
    } catch (IOException ex) {
      // ok
    }
    try {
      zfs.canonicalPath("foo/../../boo");
      fail();
    } catch (IOException ex) {
      // ok
    }
    try {
      zfs.canonicalPath("/boo");
      fail();
    } catch (IOException ex) {
      // ok
    }
  }

  public final void testBaseName() throws IOException {
    ZipFileSystem zfs = new ZipFileSystem("/foo");
    assertEquals("", zfs.basename(""));
    assertEquals("/", zfs.basename("/"));
    assertEquals("foo", zfs.basename("foo"));
    assertEquals("bar", zfs.basename("bar"));
    assertEquals("bar", zfs.basename("/foo/bar"));
    assertEquals("bar", zfs.basename("/foo/bar/"));
  }

  public final void testDirName() throws IOException {
    ZipFileSystem zfs = new ZipFileSystem("/foo");
    assertEquals(null, zfs.dirname(""));
    assertEquals("/", zfs.dirname("/"));
    assertEquals(null, zfs.dirname("foo"));
    assertEquals(null, zfs.dirname("bar"));
    assertEquals("/foo", zfs.dirname("/foo/bar"));
    assertEquals("/foo", zfs.dirname("/foo/bar/"));
    assertEquals("foo", zfs.dirname("foo/bar"));
  }

  public final void testJoin() throws IOException {
    ZipFileSystem zfs = new ZipFileSystem("/foo");
    assertEquals("foo", zfs.join(null, "foo"));
    assertEquals("bar", zfs.join(null, "bar"));
    assertEquals("foo/bar", zfs.join("foo", "bar"));
    assertEquals("/foo/bar", zfs.join("/foo", "bar"));
    assertEquals("foo", zfs.join("foo", ""));
    assertEquals("/foo", zfs.join("/foo", ""));
  }

  public final void testWriteAndRetrieveText() throws IOException {
    ZipFileSystem zfs = new ZipFileSystem("/foo");
    assertFalse(zfs.exists("/foo/bar"));
    try {
      assertEquals("", zfs.read("/foo/bar").toString());
      fail();
    } catch (IOException ex) {
      // ok
    }
    Writer out = zfs.write("bar");
    assertTrue(zfs.exists("/foo/bar"));
    assertEquals("", zfs.read("bar").toString());
    assertEquals("", zfs.read("/foo/bar").toString());
    out.write("Hello, World!");
    out.close();
    assertTrue(zfs.exists("/foo/bar"));
    assertEquals("Hello, World!", zfs.read("bar").toString());
    assertEquals("Hello, World!", zfs.read("/foo/bar").toString());
  }

  public final void testWriteAndRetrieveBinary() throws IOException {
    ZipFileSystem zfs = new ZipFileSystem("/foo");
    assertFalse(zfs.exists("/foo/bar"));
    try {
      assertEquals("", zfs.read("/foo/bar").toString());
      fail();
    } catch (IOException ex) {
      // ok
    }
    OutputStream out = zfs.writeBytes("bar");
    assertTrue(zfs.exists("/foo/bar"));
    // Empty binary files are empty text
    assertEquals("", zfs.read("bar").toString());
    assertEquals("", zfs.read("/foo/bar").toString());
    out.write(new byte[] { 0x12, 0x34 });
    out.close();
    assertTrue(zfs.exists("/foo/bar"));
    // Not readable as text
    try {
      zfs.read("/foo/bar");
      fail();
    } catch (IOException ex) {
      assertEquals("cannot read binary file", ex.getMessage());
    }
  }

  public final void testToZip() throws IOException {
    ZipFileSystem zfs = new ZipFileSystem("/root");
    zfs.mkdir("/root/dir");
    Writer text = zfs.write("dir/text.txt");
    text.write("TEXT");
    text.close();
    OutputStream binary = zfs.writeBytes("binary.bin");
    binary.write(new byte[] { 0x12, 0x34, 0x56, 0x78 });
    binary.close();
    Job zipFile = zfs.toZip();
    assertEquals(ContentType.ZIP, zipFile.t);
    assertTrue(zipFile.root instanceof byte[]);
    ZipInputStream in = new ZipInputStream(
        new ByteArrayInputStream((byte[]) zipFile.root));
    ZipEntry e;

    e = in.getNextEntry();
    assertEquals("/root/", e.getName());
    assertTrue(e.isDirectory());

    e = in.getNextEntry();
    assertEquals("/root/dir/", e.getName());
    assertTrue(e.isDirectory());

    e = in.getNextEntry();
    assertEquals("/root/dir/text.txt", e.getName());
    assertFalse(e.isDirectory());
    assertEquals('T', in.read());
    assertEquals('E', in.read());
    assertEquals('X', in.read());
    assertEquals('T', in.read());
    assertEquals(-1, in.read());

    e = in.getNextEntry();
    assertEquals("/root/binary.bin", e.getName());
    assertFalse(e.isDirectory());
    assertEquals(0x12, in.read());
    assertEquals(0x34, in.read());
    assertEquals(0x56, in.read());
    assertEquals(0x78, in.read());
    assertEquals(-1, in.read());

    assertNull(in.getNextEntry());
    in.close();
  }
}
