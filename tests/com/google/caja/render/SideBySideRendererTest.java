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

package com.google.caja.render;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.reporting.MessageContext;
import com.google.caja.util.TestUtil;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mikesamuel@gmail.com
 */
public class SideBySideRendererTest extends TestCase {
  public void testRendering() throws Exception {
    runTest(
        "side-by-side-golden.js", "rewritten-input.txt",
        "test-input1.js", "test-input2.html", "test-input3.css");
  }

  private void runTest(String goldenFile, String rewrittenFile,
                       String... originalSourceFiles) throws Exception {
    final MessageContext mc = new MessageContext();

    Map<InputSource, String> originalSrcs = new HashMap<InputSource, String>();
    for (String originalSourceFile : originalSourceFiles) {
      InputSource is = new InputSource(
          TestUtil.getResource(getClass(), originalSourceFile));
      originalSrcs.put(
          is, TestUtil.readResource(getClass(), originalSourceFile));
    }
    mc.inputSources = originalSrcs.keySet();

    StringBuilder actual = new StringBuilder();
    SideBySideRenderer r = new TabularSideBySideRenderer(
        originalSrcs, mc, actual, null);
    for (String line
         : TestUtil.readResource(getClass(), rewrittenFile).split("\n")) {
      if (line.startsWith("#")) {
        line = line.substring(1).trim();
        if ("<null>".equals(line)) {
          r.mark(null);
        } else {
          Matcher m = Pattern.compile(
              "(.*):(\\d+)\\+(\\d+)-(?:(\\d+)\\+)?(\\d+)$")
              .matcher(line);
          if (!m.matches()) { throw new RuntimeException(line); }
          String basename = m.group(1);
          int sln = Integer.parseInt(m.group(2));
          int slc = Integer.parseInt(m.group(3));
          String g4 = m.group(4);
          int eln = g4 != null ? Integer.parseInt(g4) : sln;
          int elc = Integer.parseInt(m.group(5));
          InputSource src = null;
          for (InputSource candidate : originalSrcs.keySet()) {
            if (candidate.getUri().getPath().endsWith(basename)) {
              src = candidate;
            }
          }
          if (src == null) { throw new RuntimeException(basename); }
          r.mark(FilePosition.instance(
              src, sln, sln, sln, slc, eln, eln, eln, elc));
        }
      } else {
        r.consume(line);
      }
    }
    r.noMoreTokens();

    assertEquals(
        TestUtil.readResource(getClass(), goldenFile).replace("@", "\uFFFD"),
        actual.toString());
  }
}
