// Copyright (C) 2006 Google Inc.
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

import java.net.URI;

import junit.framework.TestCase;

/**
 * @author mikesamuel@gmail.com (Mike Samuel)
 */
public class UrlUtilTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testIsDomainlessUrl() throws Exception {
    assertTrue(UrlUtil.isDomainlessUrl(""));
    assertTrue(UrlUtil.isDomainlessUrl("hello"));
    assertTrue(UrlUtil.isDomainlessUrl("hello/world"));
    assertTrue(UrlUtil.isDomainlessUrl("/hello/world"));
    assertTrue(!UrlUtil.isDomainlessUrl("//foo.com/hello/world"));
    assertTrue(!UrlUtil.isDomainlessUrl("http://foo.com/hello/world"));
    assertTrue(!UrlUtil.isDomainlessUrl("http://foo.com"));
    assertTrue(!UrlUtil.isDomainlessUrl("http://bar@foo.com"));
  }

  public void testTranslateUrl() throws Exception {
    assertEquals("/foo", UrlUtil.translateUrl(new URI(""), "/foo"));
    assertEquals("/foo/bar", UrlUtil.translateUrl(new URI("/bar"), "/foo"));
    assertEquals("/foo?a=b+c",
                 UrlUtil.translateUrl(new URI("?a=b+c"), "/foo"));
    assertEquals("/foo/bar?a=b+c",
                 UrlUtil.translateUrl(new URI("/bar?a=b+c"), "/foo"));
    assertEquals("/foo#hello",
                 UrlUtil.translateUrl(new URI("#hello"), "/foo"));
  }
}
