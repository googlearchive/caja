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

package com.google.caja.plugin.stages;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.reporting.MessageContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A module-specific collection of file positions of key pieces of cajoled code
 * that can be bundled with a cajoled module.
 *
 * @author mikesamuel@gmail.com
 */
final class DebuggingSymbols {
  private Map<FilePosition, Integer> positions
      = new LinkedHashMap<FilePosition, Integer>();

  /** Returns an index into the debugging symbols table. */
  public int indexForPosition(FilePosition pos) {
    Integer index = positions.get(pos);
    if (index == null) {
      positions.put(pos, index = positions.size());
    }
    return index;
  }

  /**
   * Produces a set of actuals that can be consumed by
   * {@code ___.useDebugSymbols} from cajita-debugmode.js.
   */
  public ParseTreeNodeContainer toJavascriptSideTable() {
    MessageContext mc = new MessageContext();
    for (InputSource is : allInputSources()) { mc.addInputSource(is); }
    List<Expression> debugTable = new ArrayList<Expression>(
        positions.size() * 2  - 1);
    String last = null;
    for (FilePosition p : positions.keySet()) {
      String posStr = formatPos(p, mc);
      int prefixLen = 0;
      if (last != null) {
        prefixLen = commonPrefixLength(posStr, last);
        debugTable.add(new IntegerLiteral(FilePosition.UNKNOWN, prefixLen));
      }
      debugTable.add(StringLiteral.valueOf(
          FilePosition.UNKNOWN, posStr.substring(prefixLen)));
      last = posStr;
    }
    return new ParseTreeNodeContainer(debugTable);
  }

  public boolean isEmpty() { return positions.isEmpty(); }

  private Set<InputSource> allInputSources() {
    Set<InputSource> sources = new HashSet<InputSource>();
    for (FilePosition p : positions.keySet()) {
      sources.add(p.source());
    }
    return sources;
  }

  /** Length of the longest string that is a prefix of both. */
  private static int commonPrefixLength(String a, String b) {
    int n = Math.min(a.length(), b.length());
    int prefixLen = 0;
    while (prefixLen < n && a.charAt(prefixLen) == b.charAt(prefixLen)) {
      ++prefixLen;
    }
    return prefixLen;
  }

  private static String formatPos(FilePosition pos, MessageContext mc) {
    StringBuilder sb = new StringBuilder();
    try {
      pos.format(mc, sb);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return sb.toString();
  }
}
