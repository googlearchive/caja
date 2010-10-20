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

package com.google.caja.service;

import java.util.Arrays;

/**
 * @author jasvir@google.com (Jasvir Nagra)
 */
public class ImageHandlerTest extends ServiceTestCase {
  public final void testImage() throws Exception {
    byte[] byteData = { (byte) 0x47, (byte) 0x49, (byte) 0x46,
                        (byte) 0x39, (byte) 0x38, (byte) 0x61 };
    registerUri("http://foo/bar.gif", byteData, "image/gif", null);
    assertTrue(Arrays.equals(
        (byte[]) requestGet("?url=http://foo/bar.gif&input-mime-type=image/*"),
        byteData));
  }
}
