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

package com.google.caja.lexer.escaping;

import java.net.URISyntaxException;

import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class UriUtilTest extends TestCase {
  public final void testUriNormalization() throws URISyntaxException {
    // Don't muck with ':' after protocol or before port, or '=' in query
    // parameters.
    assertEquals(
        "http://www.foo.org:80/path?q=query#fragmento",
        UriUtil.normalizeUri("http://www.foo.org:80/path?q=query#fragmento"));
    // But normalize = before '?', and ':' afterwards.
    assertEquals("x%3dfoo/bar?a=b%3ac=d",
                 UriUtil.normalizeUri("x=foo/bar?a=b:c=d"));
    // Spaces escaped using %20, not '+'
    assertEquals("/foo%20bar", UriUtil.normalizeUri("/foo bar"));
    assertEquals("/foo%20bar", UriUtil.normalizeUri("/foo%20bar"));
    // A domain relative URI
    assertEquals(
        "//bar.net:1234/?shazam",
        UriUtil.normalizeUri("//bar.net:1234?shazam"));
    // Path folding.
    assertEquals("../foo", UriUtil.normalizeUri("../foo"));
    assertEquals("../../foo", UriUtil.normalizeUri("../../foo"));
    assertEquals("../../bar", UriUtil.normalizeUri("../../foo/../bar"));
    assertEquals("../../bar", UriUtil.normalizeUri("../../foo/./../bar"));
    assertEquals("/bar", UriUtil.normalizeUri("/../../bar"));
    assertEquals(
        "../../bar", UriUtil.normalizeUri("%2e%2e/%2e./foo/%2E/.%2E/bar"));
    assertEquals(
        "http://foo/baz",
        UriUtil.normalizeUri("http://foo/bar/../../../baz/"));
    // Malformed escapes
    assertEquals(
        "foo%25%20.bar%25%7e://baz%252/boo%252?foo%25%5b#far%25",
        UriUtil.normalizeUri("foo% .bar%~://baz%2/boo%2?foo%[#far%"));
    // Don't interfere with well-formed escapes
    assertEquals(
        "foo%41.bar%41://baz%41/boo%41?foo%41#far%41",
        UriUtil.normalizeUri("foo%41.bar%41://baz%41/boo%41?foo%41#far%41"));
    // Cruft in various URI parts
    assertEquals(
        "foo%21.bar%21://baz%21/boo%21?foo%21#far%21",
        UriUtil.normalizeUri("foo!.bar!://baz!/boo!?foo!#far!"));
    // Check reserved characters in various URI parts.
    assertEquals(
        "a-b.1+2://Foo/hiya", UriUtil.normalizeUri("A-b.1+2://Foo/hiya"));
    assertEquals(
        "http://Hello+There-Now.com:80/",
        UriUtil.normalizeUri("HTTP://Hello+There-Now.com:80"));
    assertEquals(
        "/Hello+There-N0w.",
        UriUtil.normalizeUri("/Hello+There-N0w."));
    assertEquals(
        "zounds?foo=BAR&four=2+2-0.0",
        UriUtil.normalizeUri("zounds?foo=BAR&four=2+2-0.0"));
    assertEquals(
        "#Hiya.%20%201-+1%3d0.0",
        UriUtil.normalizeUri("#Hiya.  1-+1=0.0"));
    // Test non Latin characters in various places.
    String enc = "%ef%bd%a1";
    assertEquals(
        "s" + enc + "://A" + enc + "/P" + enc + "?Q" + enc + "#F" + enc,
        UriUtil.normalizeUri("S\uff61://A\uff61/P\uff61?Q\uff61#F\uff61"));
  }

  public final void testBlankAuthority1() {
    String result;
    try {
      result = UriUtil.normalizeUri("http:///www.google.com/foo");
    } catch (URISyntaxException ex) {
      return;  // OK
    }
    // No way to reconstruct user intent so fail.
    fail(result);
  }

  public final void testBlankAuthority2() {
    String result;
    try {
      result = UriUtil.normalizeUri("///www.google.com/foo");
    } catch (URISyntaxException ex) {
      return;  // OK
    }
    // No way to reconstruct user intent so fail.
    fail(result);
  }

  public final void testMissingAuthority() {
    String result;
    try {
      result = UriUtil.normalizeUri("http:/www.google.com/foo");
    } catch (URISyntaxException ex) {
      return;  // OK
    }
    // No way to reconstruct user intent so fail.
    fail(result);
  }

  public final void testMissingScheme() {
    String result;
    try {
      result = UriUtil.normalizeUri(" javascript:alert(1)");
    } catch (URISyntaxException ex) {
      return;  // OK
    }
    // No way to reconstruct user intent so fail.
    fail(result);
  }

  public final void testMalformedPort() {
    String result;
    try {
      result = UriUtil.normalizeUri("http://www.google.com:paypal.com/foo");
    } catch (URISyntaxException ex) {
      return;  // OK
    }
    // No way to reconstruct user intent so fail.
    fail(result);
  }

  public final void testEncode() {
    assertEquals("", UriUtil.encode(""));
    assertEquals("foo", UriUtil.encode("foo"));
    assertEquals("foo%2bbar", UriUtil.encode("foo+bar"));
    assertEquals("foo%2fbar", UriUtil.encode("foo/bar"));
    assertEquals("%e1%88%b4", UriUtil.encode("\u1234"));
    assertEquals("%3a%23%3f%2f%3d%26%40%25", UriUtil.encode(":#?/=&@%"));
  }
}
