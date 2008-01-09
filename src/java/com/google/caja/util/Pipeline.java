// Copyright (C) 2007 Google Inc.
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

import java.util.ArrayList;
import java.util.List;

/**
 * A pipeline takes an input and passes it through multiple stages, like a
 * conveyor belt that passes a bucket of spare parts through a factory until
 * the bucket reaches the end hopefully containing something resembling a
 * bicycle.
 *
 * @author mikesamuel@gmail.com
 */
public class Pipeline<T> {
  private final List<Stage<T>> stages = new ArrayList<Stage<T>>();

  public final List<Stage<T>> getStages() { return stages; }

  public final boolean apply(T input) {
    for (Stage<T> stage : stages) {
      if (!applyStage(stage, input)) { return false; }
    }
    return true;
  }

  protected boolean applyStage(Stage<? super T> stage, T input) {
    return stage.apply(input);
  }

  public interface Stage<S> {
    /**
     * Operates on an input and returns true iff processing should proceed to
     * the next stage.
     */
    boolean apply(S input);
  }
}
