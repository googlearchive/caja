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

package com.google.caja.reporting;

import junit.framework.TestCase;

import java.util.ListResourceBundle;

/**
 * @author ihab.awad@gmail.com
 */
@SuppressWarnings("static-method")
public class BuildInfoTest extends TestCase {
  private static BuildInfo getDefaultBuildInfo() {
    return new BuildInfo(
      new ListResourceBundle() {
        @Override
        public Object[][] getContents() {
          return new Object[][] {
              { "svnVersion", "345M", },
              { "timestamp", "January 1, 2007", },
          };
        }
      });
  }

  private static Message getMessage(BuildInfo bi) {
    MessageQueue mq = new SimpleMessageQueue();
    bi.addBuildInfo(mq);
    assertEquals(1, mq.getMessages().size());
    return mq.getMessages().get(0);
  }

  public final void testNonemptyInfo() {
    Message m = getMessage(getDefaultBuildInfo());
    assertEquals(MessageLevel.LOG, m.getMessageLevel());
    String s = m.format(new MessageContext());
    assertTrue(s.contains("345M"));
    assertTrue(s.contains("January 1, 2007"));
  }

  public final void testEmptyInfo() {
    Message m = getMessage(new BuildInfo(null));
    // Just ensure that nothing failed dramatically
    assertEquals(MessageLevel.LOG, m.getMessageLevel());
  }
}
