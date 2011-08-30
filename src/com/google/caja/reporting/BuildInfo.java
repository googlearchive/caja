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

import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.io.IOException;

/**
 * Utility to return information about the version and progeny of the
 * software build, for use in the output of commands, logging, etc.
 *
 * @author ihab.awad@gmail.com
 */
public class BuildInfo {
  private static BuildInfo instance;

  public static BuildInfo getInstance() {
    if (instance == null) instance = new BuildInfo();
    return instance;
  }

  private ResourceBundle properties;

  /* package private */ BuildInfo(ResourceBundle properties) {
    this.properties = properties;
  }

  protected BuildInfo() {
    try {
      properties =
          ResourceBundle.getBundle("com/google/caja/reporting/buildInfo");
    } catch (MissingResourceException e) {
      properties = null;
    }
  }

  private String getPropertyValue(String name) {
    if (properties != null) {
      try {
        return properties.getString(name);
      } catch (MissingResourceException e) {
        // Fall through
      }
    }
    return "<unknown>";
  }

  private MessagePart wrapValue(final String value) {
    return new MessagePart() {
      public void format(MessageContext context, Appendable out)
          throws IOException {
        out.append(value);
      }
    };
  }

  /**
   * Adds a build information message to the given {@code MessageQueue}.
   *
   * @param mq a message queue.
   */
  public void addBuildInfo(MessageQueue mq) {
    mq.addMessage(
        MessageType.BUILD_INFO,
        wrapValue(getBuildVersion()),
        wrapValue(getBuildTimestamp()));
  }

  /**
   * Gets a plain string containing the build info.
   *
   * @return the build info as a string.
   */
  public String getBuildInfo() {
    MessageQueue mq = new SimpleMessageQueue();
    addBuildInfo(mq);
    return mq.getMessages().get(0).format(new MessageContext());
  }

  /**
   * @return the revision control version of the current build.
   */
  public String getBuildVersion() { return getPropertyValue("svnVersion"); }

  /**
   * @return the timestamp of the current build.
   */
  public String getBuildTimestamp() { return getPropertyValue("timestamp"); }

  /**
   * @return the current system date, suitable for a timestamp of the currently
   * executing operation. This allows us to sequester any nondeterminism in the
   * cajoler's behavior.
   */
  public long getCurrentTime() { return System.currentTimeMillis(); }
}