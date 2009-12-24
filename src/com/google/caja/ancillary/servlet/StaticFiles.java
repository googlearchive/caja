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

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.ParseException;
import com.google.caja.reporting.DevNullMessageQueue;
import com.google.caja.util.ContentType;
import com.google.caja.util.Lists;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves static files from the class path.
 * Files are stored in the <tt>files</tt> directory in this package.  There
 * are several kinds of file:<ul>
 * <li>Images, scripts, and help files linked to from other pages.
 * <li>Help files based on identifiers in the java code.  E.g. the help text
 * for a warning message {@code MessageType.FOO_BAR} would be in the html
 * file <tt>files/FOO_BAR_tip.html</tt>.
 * </ul>
 *
 * @author mikesamuel@gmail.com
 */
final class StaticFiles {
  // Use a bigger cache to store the fact that a file does not exist, since
  // we don't have to use much memory for it.
  private final Map<String, Boolean> exists = mruCache(1024);
  private final Map<String, Content> files = mruCache(128);
  private final long startupTime = System.currentTimeMillis();
  private final long expiryDate = Math.max(
      startupTime + 3600 * 24 * 366000, (0x7fffffffL * 1000));
  final String cacheId;

  StaticFiles(String cacheId) {
    this.cacheId = cacheId;
  }

  boolean exists(String path) {
    if (files.containsKey(path)) { return true; }
    Boolean b = exists.get(path);
    if (b != null) { return b; }
    InputStream in = StaticFiles.class.getResourceAsStream(path);
    boolean fileExists = in != null;
    if (fileExists) {
      try {
        in.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
    exists.put(path, fileExists);
    return fileExists;
  }

  void serve(String path, HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    if (req.getDateHeader("If-modified-since") >= startupTime) {
      resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }
    Content content = files.get(path);
    if (content == null && !Boolean.FALSE.equals(exists.get(path))) {
      InputStream in = StaticFiles.class.getResourceAsStream(path);
      if (in == null) {
        exists.put(path, Boolean.FALSE);
      } else {
        try {
          // TODO(mikesamuel): SVN has it in svn:mime-type, but that is not
          // available via the ClassLoader.  Is there any way to get at it?
          ContentType t = GuessContentType.guess(null, path, null);
          if (t != null && t.isText) {
            InputSource is;
            try {
              is = new InputSource(StaticFiles.class.getResource(path).toURI());
            } catch (URISyntaxException ex) {
              ex.printStackTrace();
              is = InputSource.UNKNOWN;
            }
            CharProducer cp = CharProducer.Factory.create(
                new InputStreamReader(in, "UTF-8"), is);
            // Minimize it before serving.
            Request min = Request.create(Verb.ECHO, this);
            min.minify = true;
            min.opt = true;
            min.otype = t;
            Processor p = new Processor(min, DevNullMessageQueue.singleton());
            try {
              Job j = p.parse(cp, t, null, is.getUri());
              List<Job> out = p.process(Lists.newArrayList(j));
              if (out.size() == 1) {
                content = p.reduce(out);
              }
            } catch (ParseException ex) {
              // fall through to case below
              ex.printStackTrace();
            }
            if (content == null) {
              content = new Content(cp.toString(), t);
            }
          } else {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            for (int n; (n = in.read(bytes)) > 0;) { buf.write(bytes, 0, n); }
            content = new Content(bytes, t);
          }
        } finally {
          in.close();
        }
        files.put(path, content);
      }
    }
    if (content != null) {
      resp.setStatus(200);
      String mimeType = mimeTypeFor(content.type, path);
      if (mimeType != null) {
        resp.setContentType(mimeType);
      }
      resp.setDateHeader("Last-modified", startupTime);
      resp.setDateHeader("Expires", expiryDate);
      if (content.isText()) {
        Writer out = resp.getWriter();
        content.toWriter(out);
        out.close();
      } else {
        OutputStream out = resp.getOutputStream();
        content.toOutputStream(out);
        out.close();
      }
    } else {
      resp.setStatus(404);
    }
  }

  private static String mimeTypeFor(ContentType t, String path) {
    if (t != null) {
      return t.isText ? t.mimeType + "; charset=UTF-8" : t.mimeType;
    }
    int dot = path.lastIndexOf('.');
    if (dot >= 0) {
      String ext = path.substring(dot + 1);
      if ("gif".equals(ext)) { return "image/gif"; }
      if ("png".equals(ext)) { return "image/png"; }
      if ("jpg".equals(ext)) { return "image/jpeg"; }
    }
    return null;
  }

  private static <K, V> Map<K, V> mruCache(final int maxSize) {
    return Collections.synchronizedMap(new LinkedHashMap<K, V>() {
      @Override
      public boolean removeEldestEntry(Map.Entry<K, V> e) {
        return size() > maxSize;
      }
    });
  }
}
