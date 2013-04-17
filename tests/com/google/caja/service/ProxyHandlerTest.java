// Copyright (C) 2013 Google Inc.
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

package com.google.caja.service;

import com.google.caja.reporting.BuildInfo;

/**
 * @author kpreid@switchb.org (Kevin Reid)
 */
public class ProxyHandlerTest extends ServiceTestCase {
  public final void testJsonp() throws Exception {
    registerUri("http://foo/bar", "body {}", "text/css");
    String s = (String) requestGet("?url=http://foo/bar"
        + "&input-mime-type=text/css"
        + "&alt=json-in-script"
        + "&callback=foo"
        + "&transform=PROXY"
        + "&build-version=" + BuildInfo.getInstance().getBuildVersion(),
        "text/javascript");
    assertCallbackInJsonp(s, "foo");
    assertSubstringsInJsonp(s, "html", "body {}");
  }

  public final void testJson() throws Exception {
    registerUri("http://foo/bar", "body {}", "text/css");
    String s = (String) requestGet("?url=http://foo/bar"
        + "&input-mime-type=text/css"
        + "&alt=json"
        + "&callback=foo"
        + "&transform=PROXY"
        + "&build-version=" + BuildInfo.getInstance().getBuildVersion(),
        "application/json");
    assertSubstringsInJson(s, "html", "body {}");
  }

  public final void testJsonpAbsent() throws Exception {
    // no registerUri; we want a failure
    requestGet("?url=http://foo/bar"
        + "&input-mime-type=text/css"
        + "&alt=json-in-script"
        + "&callback=foo"
        + "&transform=PROXY"
        + "&build-version=" + BuildInfo.getInstance().getBuildVersion(),
        "text/javascript");
    // TODO(kpreid): assertions about content, not just mime type
  }

  public final void testJsonAbsent() throws Exception {
    // no registerUri; we want a failure
    requestGet("?url=http://foo/bar"
        + "&input-mime-type=text/css"
        + "&alt=json"
        + "&callback=foo"
        + "&transform=PROXY"
        + "&build-version=" + BuildInfo.getInstance().getBuildVersion(),
        "application/json");
    // TODO(kpreid): assertions about content, not just mime type
  }
}
