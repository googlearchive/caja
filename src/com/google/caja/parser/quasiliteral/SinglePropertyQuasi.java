// Copyright (C) 2010 Google Inc.
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

package com.google.caja.parser.quasiliteral;

import com.google.caja.parser.ParseTreeNode;
import com.google.caja.util.Lists;

import java.util.List;
import java.util.Map;

/**
 * Matches a single key/value pair in an {@link ObjectCtorQuasiNode}.
 *
 * @author mikesamuel@gmail.com
 */
final class SinglePropertyQuasi extends QuasiNode {

  SinglePropertyQuasi(QuasiNode key, QuasiNode value) {
    super(key, value);
  }

  QuasiNode getKey() { return getChildren().get(0); }

  QuasiNode getValue() { return getChildren().get(1); }

  @Override
  protected boolean consumeSpecimens(
      List<ParseTreeNode> specimens, Map<String, ParseTreeNode> bindings) {
    QuasiNode key = getKey();
    QuasiNode value = getValue();
    for (int i = 0, n = specimens.size(); i < n; i += 2) {
      ParseTreeNode candidate = specimens.get(i);
      List<ParseTreeNode> keyCandidate = Lists.newArrayList(1);
      keyCandidate.add(candidate);
      if (key.consumeSpecimens(keyCandidate, bindings)
          && keyCandidate.isEmpty()) {
        List<ParseTreeNode> valueCandidate = Lists.newArrayList(1);
        valueCandidate.add(specimens.get(i + 1));
        if (value.consumeSpecimens(valueCandidate, bindings)) {
          if (valueCandidate.isEmpty()) {
            specimens.subList(i, i + 2).clear();
          }
          return true;
        }
      }
    }
    return false;
  }

  @Override
  protected boolean createSubstitutes(
      List<ParseTreeNode> substitutes, Map<String, ParseTreeNode> bindings) {
    int subsSize = substitutes.size();
    if (getKey().createSubstitutes(substitutes, bindings)) {
      int subsSizeKey = substitutes.size();
      if (subsSizeKey == subsSize + 1) {
        if (getValue().createSubstitutes(substitutes, bindings)) {
          int subsSizeValue = substitutes.size();
          if (subsSizeValue == subsSizeKey + 1) {
            return true;
          }
        }
      }
    }
    substitutes.subList(subsSize, substitutes.size()).clear();
    return false;
  }
}
