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
package com.google.caja.reporting;

/**
 * A stub for class {@link BuildInfo} that provides deterministic information
 * for testing.
 *
 * @author ihab.awad@gmail.com
 */
public class TestBuildInfo extends BuildInfo {
  // Intentionally mask static method in superclass, since getInstance shows
  // by auto-completion on TestBuildInfo, but does not do what is intended.
  public static TestBuildInfo getInstance() { return new TestBuildInfo(); }

  @Override
  public String getBuildInfo() {
    return "testBuildInfo";
  }

  @Override
  public String getBuildVersion() {
    return "testBuildVersion";
  }

  @Override
  public String getBuildTimestamp() {
    return "testBuildTimestamp";
  }

  @Override
  public long getCurrentTime() {
    return 0L;
  }
}
