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

package com.google.caja.demos.playground.server;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.plugin.stages.JobCache;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.util.ContentType;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

final class AppEngineJobCacheKey implements JobCache.Key, Serializable {
  private static final long serialVersionUID = 594623347086143778L;

  private final byte[] hashBytes;
  private final int first32Bits;

  // TODO(jasvir): Avoid hard to debug problems due to hash collisions
  // by doing a full serialization and exact value comparison rather than hash
  // comparison -- depending on speed
  AppEngineJobCacheKey(ContentType type, ParseTreeNode node) {
    Hasher hasher = new Hasher(type);
    hasher.hash(node);
    hasher.hash(BuildInfo.getInstance().getBuildVersion());
    this.hashBytes = hasher.getHashBytes();
    this.first32Bits = (hashBytes[0] & 0xff)
        | ((hashBytes[1] & 0xff) << 8)
        | ((hashBytes[2] & 0xff) << 16)
        | ((hashBytes[3] & 0xff) << 24);
  }

  public AppEngineJobCacheKeys asSingleton() {
    return new AppEngineJobCacheKeys(this);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof AppEngineJobCacheKey &&
        Arrays.equals(hashBytes, ((AppEngineJobCacheKey) o).hashBytes);
  }

  @Override
  public int hashCode() {
    return first32Bits;
  }


  /** A helper that walks a tree to feed tree details to a hash fn. */
  private static final class Hasher {
    final MessageDigest md;
    /** Buffer that captures output to allow md to amortize hashing. */
    final byte[] buffer = new byte[1024];
    /** Index of last byte in buffer that needs to be updated to md. */
    int posInBuffer = -1;

    Hasher(ContentType t) {
      try {
        md = MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException ex) {
        // We can't recover if a basic algorithm like MD5 is not supported.
        throw (AssertionError) (new AssertionError().initCause(ex));
      }
      md.update((byte) t.ordinal());
    }

    /** Returns the hash of anything passed to {@link #hash(ParseTreeNode)}. */
    byte[] getHashBytes() {
      flushBuffer();
      return md.digest();
    }

    /** Hashes the given parse tree. */
    void hash(ParseTreeNode node) {
      hash(System.identityHashCode(node.getClass()));

      Object value = node.getValue();
      if (value != null) {
        if (value instanceof String) {
          hash((String) value);
        } else if (value instanceof Node) {
          hash((Node) value);
        } else {
          hash(value.hashCode());
        }
      }

      List<? extends ParseTreeNode> children = node.children();
      hash((short) children.size());

      for (ParseTreeNode child : children) {
        hash(child);
      }
    }

    private void hash(Node node) {
      short nodeType = node.getNodeType();
      hash(nodeType);
      switch (nodeType) {
        case Node.ATTRIBUTE_NODE:
        case Node.ELEMENT_NODE:
          hash(node.getNodeName());
          break;
        case Node.TEXT_NODE:
        case Node.CDATA_SECTION_NODE:
          hash(node.getNodeValue());
          break;
      }

      hash((short) node.getChildNodes().getLength());

      if (nodeType == Node.ELEMENT_NODE) {
        NamedNodeMap attrs = node.getAttributes();
        int nAttrs = attrs.getLength();
        hash((short) nAttrs);
        for (int i = 0; i < nAttrs; ++i) {
          hash(attrs.item(i));
        }
      }

      for (Node child = node.getFirstChild(); child != null;
           child = child.getNextSibling()) {
        hash(child);
      }
    }

    private void hash(int n) {
      requireSpaceInBuffer(4);
      buffer[++posInBuffer] = (byte) ((n >> 24) & 0xff);
      buffer[++posInBuffer] = (byte) ((n >> 16) & 0xff);
      buffer[++posInBuffer] = (byte) ((n >> 8) & 0xff);
      buffer[++posInBuffer] = (byte) (n & 0xff);
    }

    private void hash(short n) {
      requireSpaceInBuffer(2);
      buffer[++posInBuffer] = (byte) ((n >> 8) & 0xff);
      buffer[++posInBuffer] = (byte) (n & 0xff);
    }

    private void hash(String text) {
      int n = text.length();
      for (int i = 0; i < n; ++i) {
        char ch = text.charAt(i);
        if (ch < 0x0080) {
          requireSpaceInBuffer(1);
          buffer[++posInBuffer] = (byte) ch;
        } else if (ch < 0x080) {
          requireSpaceInBuffer(2);
          buffer[++posInBuffer] = (byte) (((ch >> 6) & 0x1f) | 0xc0);
          buffer[++posInBuffer] = (byte) ((ch & 0x3f) | 0x80);
        } else {
          requireSpaceInBuffer(3);
          buffer[++posInBuffer] = (byte) (((ch >> 12) & 0x0f) | 0xe0);
          buffer[++posInBuffer] = (byte) (((ch >> 6) & 0x3f) | 0x80);
          buffer[++posInBuffer] = (byte) ((ch & 0x3f) | 0x80);
        }
      }
    }

    /** Flushes the buffer if there is not enough space. */
    private void requireSpaceInBuffer(int space) {
      if (posInBuffer + space >= buffer.length) {
        flushBuffer();
      }
    }

    /** Writes the buffer content to the message digest. */
    private void flushBuffer() {
      md.update(buffer, 0, posInBuffer + 1);
      posInBuffer = -1;  // Reset the buffer.
    }
  }
}
