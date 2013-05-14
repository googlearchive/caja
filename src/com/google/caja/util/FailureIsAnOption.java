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

package com.google.caja.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test that is known to fail.
 * It is appropriate to use this in several cases:
 * <ul>
 * <li>To document a bug with a test case before fixing it.</li>
 * <li>To allow a change to commit while coordinating overlapping changes with
 *     another person who has a pending fix.</li>
 * </ul>
 *
 * Known failing tests will be identified in the test reports, but will not
 * cause the build to fail, and so won't stop {@code myvn submit}.
 *
 * Use {@link com.google.caja.tools.TestSummary#isFailureAnOption(Class, String, Function)}
 * to check for failure annotations on dynamically-generated tests as well as
 * simple method annotations.
 *
 * @author mikesamuel@gmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FailureIsAnOption {
  // Reason for failure
  String value() default "Unknown reason";
}
