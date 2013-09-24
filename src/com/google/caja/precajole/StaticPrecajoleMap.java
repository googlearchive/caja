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

package com.google.caja.precajole;

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.util.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

/**
 * This is a PrecajoleMap that looks up precajoled modules from a
 * directory in the caja jar (or in the filesystem).
 * <p>
 * The directory contains serialized baked CajoledModules, stored
 * one per file.  Filenames are hex-encoded sha1 key of the uncajoled
 * source text.
 * <p>
 * There's also a file index.dat that contains a serialized map
 * of URI -> key.
 */

public class StaticPrecajoleMap implements PrecajoleMap {

  private static String SUBDIR_PATH = "com/google/caja/precajole/data/";
  private static String INDEX_NAME = "index.dat";

  public static StaticPrecajoleMap getInstance() {
    return InstanceHolder.instance;
  }

  private static class InstanceHolder {
    static StaticPrecajoleMap instance = new StaticPrecajoleMap("");
  }

  private static class Entry implements Serializable {
    private static final long serialVersionUID = 1L;
    final String[] uris;
    final String source;
    final CajoledModule minified;
    final CajoledModule pretty;
    final String id;

    Entry(String[] uris, String source, CajoledModule cajoled) {
      this.uris = uris;
      this.source = normalizeSource(source);
      this.minified = cajoled.flatten(true);
      this.pretty = cajoled.flatten(false);
      this.id = idForSource(this.source);
    }

    static Entry from(byte[] serial) {
      return (Entry) deserialize(serial);
    }
  }

  private static class Index implements Serializable {
    private static final long serialVersionUID = 1L;
    final public Map<String, String> map = Maps.newHashMap();
    public long modTime = 0L;  // millisecond timestamp
  }

  //----

  private final String dir;
  private final Index index;

  public StaticPrecajoleMap(File baseDir) {
    this(baseDir.toString());
  }

  public StaticPrecajoleMap(String baseDir) {
    if (!baseDir.equals("") && !baseDir.endsWith("/")) {
      baseDir += "/";
    }
    this.dir = baseDir + SUBDIR_PATH;
    this.index = readIndex();
  }

  private Index readIndex() {
    Object o = deserialize(load(INDEX_NAME));
    if (o != null && o instanceof Index) {
      return (Index) o;
    } else {
      return new Index();
    }
  }

  public void put(List<String> uris, String source, CajoledModule cajoled) {
    put(uris.toArray(new String[uris.size()]), source, cajoled);
  }

  public void put(String[] uris, String source, CajoledModule cajoled) {
    Entry entry = new Entry(uris, source, cajoled);
    byte[] serial = serialize(entry);
    for (int k = 0; k < uris.length; k++) {
      index.map.put(normalizeUri(uris[k]), entry.id);
    }
    save(entry.id, serial);
  }

  public long getModTime() {
    return index.modTime;
  }

  public void setModTime(long millitime) {
    index.modTime = millitime;
  }

  public void finish() {
    save(INDEX_NAME, serialize(index));
  }

  @Override
  public CajoledModule lookupUri(String uri, boolean minify) {
    Entry e = Entry.from(load(idForUri(uri)));
    if (e != null && e.uris != null) {
      for (int k = 0; k < e.uris.length; k++) {
        if (uri.equals(e.uris[k])) {
          return minify ? e.minified : e.pretty;
        }
      }
    }
    return null;
  }

  @Override
  public CajoledModule lookupSource(String source, boolean minify) {
    source = normalizeSource(source);
    Entry e = Entry.from(load(idForSource(source)));
    if (e != null && source.equals(e.source)) {
      return minify ? e.minified : e.pretty;
    }
    return null;
  }

  public List<List<String>> getUrlGroups() {
    Map<String, List<String>> idMap = Maps.newHashMap();
    for (String url : index.map.keySet()) {
      String id = index.map.get(url);
      List<String> urls = idMap.get(id);
      if (urls == null) {
        urls = Lists.newArrayList();
        idMap.put(id, urls);
      }
      urls.add(url);
    }
    List<List<String>> urlGroups = Lists.newArrayList();
    for (List<String> urls : idMap.values()) {
      urlGroups.add(urls);
    }
    return urlGroups;
  }

  public static String normalizeUri(String uri) {
    try {
      URI u = new URI(uri).normalize();
      if (u.getHost() != null) {
        u = new URI(
            lowercase(u.getScheme()),
            u.getUserInfo(),
            lowercase(u.getHost()),
            u.getPort(),
            u.getPath(),
            u.getQuery(),
            u.getFragment());
      } else if (u.getScheme() != null) {
        u = new URI(
            lowercase(u.getScheme()),
            u.getSchemeSpecificPart(),
            u.getFragment());
      }
      return u.toString();
    } catch (URISyntaxException e) {
      return uri;
    }
  }

  private static String lowercase(String s) {
    return s == null ? null : Strings.lower(s);
  }

  private void save(String id, byte[] data) {
    try {
      new File(dir).mkdirs();
      FileOutputStream o = new FileOutputStream(new File(dir, id));
      o.write(data);
      o.close();
    } catch (IOException e) {
      throw new SomethingWidgyHappenedError(e);
    }
  }

  private String idForUri(String uri) {
    return index.map.get(normalizeUri(uri));
  }

  private static String idForSource(String source) {
    return computeHash(source);
  }

  private static String normalizeSource(String source) {
    // TODO(felix8a): I'd like to minify js here, but minifier is too slow
    return source.trim();
  }

  private byte[] load(String id) {
    byte[] result = loadResource(id);
    return result != null ? result : loadFile(id);
  }

  private byte[] loadFile(String id) {
    if (id == null) {
      return null;
    }
    try {
      return Files.toByteArray(new File(dir, id));
    } catch (IOException e) {
      return null;
    }
  }

  private byte[] loadResource(String id) {
    if (id == null) {
      return null;
    }
    ClassLoader cl = StaticPrecajoleMap.class.getClassLoader();
    InputStream is = cl.getResourceAsStream(dir + id);
    if (is == null) {
      return null;
    }
    try {
      try {
        return ByteStreams.toByteArray(is);
      } finally {
        is.close();
      }
    } catch (IOException e) {
      return null;
    }
  }

  private static String computeHash(String s) {
    try {
      MessageDigest sha1 = MessageDigest.getInstance("SHA1");
      byte[] digest = sha1.digest(s.getBytes("UTF-8"));
      return DatatypeConverter.printHexBinary(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new SomethingWidgyHappenedError(e);
    } catch (UnsupportedEncodingException e) {
      throw new SomethingWidgyHappenedError(e);
    }
  }

  // TODO(felix8a): protobuf is much faster
  private static byte[] serialize(Object obj) {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    try {
      ObjectOutputStream ostr = new ObjectOutputStream(buf);
      ostr.writeObject(obj);
      ostr.close();
    } catch (IOException e) {
      throw new SomethingWidgyHappenedError(e);
    }
    return buf.toByteArray();
  }

  private static Object deserialize(byte[] serial) {
    if (serial == null) {
      return null;
    }
    try {
      ByteArrayInputStream b = new ByteArrayInputStream(serial);
      ObjectInputStream i = new ObjectInputStream(b);
      return i.readObject();
    } catch (IOException e) {
      return null;
    } catch (ClassNotFoundException e) {
      return null;
    }
  }
}
