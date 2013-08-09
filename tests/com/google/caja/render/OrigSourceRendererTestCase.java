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

import com.google.caja.SomethingWidgyHappenedError;
import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.lexer.TokenConsumer;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.RenderContext;
import com.google.caja.util.Maps;
import com.google.caja.util.TestUtil;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 * Test case base class for a renderer that includes snippets of the original
 * source code along with the cajoled output code.
 *
 * @author mikesamuel@gmail.com
 */
public abstract class OrigSourceRendererTestCase extends TestCase {

  /**
   * Runs a test for a given case of translation.
   *
   * @param goldenFile the resource name of a file containing the complete
   *     output that the final rendering stage is expected to produce.
   * @param rewrittenFile the resource name of a file containing tokens, one
   *     per line, that represents the cajoled output. At each point, a line
   *     beginning with a '#' indicates a stringified FilePosition that will
   *     be translated to a call to TokenConsumer.mark().
   * @param originalSourceFiles the resource names of the original source files
   *     that were used to generate the cajoled tokens. File positions in the
   *     'rewrittenFile' refer to positions in these files.
   */
  protected void runTest(String goldenFile, String rewrittenFile,
                         String... originalSourceFiles) throws Exception {
    final MessageContext mc = new MessageContext();

    Map<InputSource, String> originalSrcs = Maps.newHashMap();
    for (String originalSourceFile : originalSourceFiles) {
      URI resourceUri = TestUtil.getResource(getClass(), originalSourceFile);
      if (resourceUri == null) { throw new IOException(originalSourceFile); }
      InputSource is = new InputSource(resourceUri);
      originalSrcs.put(
          is, TestUtil.readResource(getClass(), originalSourceFile));
    }
    for (InputSource is : originalSrcs.keySet()) { mc.addInputSource(is); }

    StringBuilder actual = new StringBuilder();
    RenderContext rc = new RenderContext(new Concatenator(actual));
    TokenConsumer r = createRenderer(originalSrcs, mc, rc);
    for (String line
         : TestUtil.readResource(getClass(), rewrittenFile).split("\n")) {
      if (line.startsWith("#")) {
        line = line.substring(1).trim();
        if ("<null>".equals(line)) {
          r.mark(null);
        } else {
          r.mark(toFilePosition(line, originalSrcs));
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

  /**
   * Creates an instance of the renderer that is being tested.
   */
  protected abstract TokenConsumer createRenderer(
      Map<InputSource, ? extends CharSequence> originalSource,
      MessageContext mc, RenderContext rc);

  private static FilePosition toFilePosition(
      String testInputLine, Map<InputSource, String> originalSrcs) {
    Matcher m = Pattern.compile(
        "(.*):(\\d+)\\+(\\d+)-(?:(\\d+)\\+)?(\\d+)$")
        .matcher(testInputLine);
    if (!m.matches()) {
      throw new SomethingWidgyHappenedError(
          "Format of file position has changed");
    }
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
    if (src == null) {
      throw new SomethingWidgyHappenedError(
          "No sources specified");
    }

    return FilePosition.fromLinePositions(src, sln, slc, eln, elc);
  }
}
