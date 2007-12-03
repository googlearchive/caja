// Copyright (C) 2007 Google Inc.
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

package com.google.caja.opensocial;

import com.google.caja.util.TestUtil;
import junit.framework.TestCase;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;

/**
 * @author ihab.awad@gmail.com (Ihab Awad)
 */
public class DefaultGadgetRewriterTest extends TestCase {

  private static final UriCallback uriCallback = new UriCallback() {
    public UriCallbackOption getOption(URI uri, String mimeType) {
      return UriCallbackOption.RETRIEVE;
    }

    public InputStream retrieve(URI uri, String mimeType) throws UriCallbackException {
      throw new UriCallbackException(uri, "Rewrite unsupported");
    }

    public URI rewrite(URI uri, String mimeType) throws UriCallbackException {
      throw new UriCallbackException(uri, "Rewrite unsupported");
    }
  };

  private GadgetRewriter rewriter;

  public void setUp() { rewriter = new DefaultGadgetRewriter(); }

  public void tearDown() { rewriter = null; }
  
  public void testBasic() throws Exception {
    Reader input = new StringReader(TestUtil.readResource(getClass(), "listfriends-inline.xml"));
    Writer output = new StringWriter();
    rewriter.rewrite(null, input, uriCallback, output);
    System.out.println(output.toString());
  }
}