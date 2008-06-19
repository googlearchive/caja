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

package com.google.caja.opensocial.service;

import junit.framework.TestCase;

public class ContentTypeCheckTest extends TestCase {
  public void testStrictContentTypeCheck() {
    ContentTypeCheck ctc = new StrictContentTypeCheck();
    assertFalse(ctc.check("image/*", ""));
    assertFalse(ctc.check("image/*", "bogus-nonsense"));
    assertFalse(ctc.check("image/*", "; hellow=world"));
    assertTrue(ctc.check("text/plain", "text/plain"));
    assertFalse(ctc.check("text/plain", "text/javascript"));
    assertTrue(ctc.check("text/*", "text/plain"));
    assertTrue(ctc.check("text/*", "text/javascript"));
    assertTrue(ctc.check("text/javascript", "text/javascript"));
    assertTrue(ctc.check("text/javascript", "text/javascript;charset=UTF-8"));
    assertTrue(ctc.check("text/javascript", "text/javascript; charset=UTF-8"));
    assertTrue(ctc.check("text/javascript", "text/javascript; e4x=1"));
    assertFalse(ctc.check("text/javascript", "application/x-javascript"));
    assertFalse(ctc.check("text/vbscript", "text/javascript"));
    assertTrue(ctc.check("*/*", "text/html"));
    assertTrue(ctc.check("*/*", "text/javascript"));
    assertTrue(ctc.check("*/*", "image/jpg"));
    assertTrue(ctc.check("image/*", "image/jpg"));
  }

  public void testLooseContentTypeCheck() {
    ContentTypeCheck ctc = new LooseContentTypeCheck();
    assertFalse(ctc.check("image/*", ""));
    assertFalse(ctc.check("image/*", "bogus-nonsense"));
    assertFalse(ctc.check("image/*", "; hellow=world"));
    assertTrue(ctc.check("text/plain", "text/plain"));
    assertFalse(ctc.check("text/plain", "text/javascript"));
    assertTrue(ctc.check("text/*", "text/plain"));
    assertTrue(ctc.check("text/*", "text/javascript"));
    assertTrue(ctc.check("text/javascript", "text/javascript"));
    assertTrue(ctc.check("text/javascript", "text/javascript;charset=UTF-8"));
    assertTrue(ctc.check("text/javascript", "text/javascript; charset=UTF-8"));
    assertTrue(ctc.check("text/javascript", "text/javascript; e4x=1"));
    assertTrue(ctc.check("text/javascript", "application/x-javascript"));
    assertFalse(ctc.check("text/vbscript", "text/javascript"));
    assertTrue(ctc.check("*/*", "text/html"));
    assertTrue(ctc.check("*/*", "text/javascript"));
    assertTrue(ctc.check("*/*", "image/jpg"));
    assertTrue(ctc.check("image/*", "image/jpg"));
  }
}