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
import com.google.caja.parser.AbstractParseTreeNode;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.Pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Infers file positions for synthetic nodes based on surrounding nodes, so that
 * we can generate useful error messages.
 *
 * <p>This does some very simple inference by assuming that a parent node spans
 * all of its children, and if the children come from multiple
 * {@link InputSource source}s chooses the region spanning the source from which
 * the most children come.
 *
 * <p>We could do more advanced propagation by using a constraint solver to
 * capture constraints like child-follows-sibling and parent-contains-children
 * but the current approach is sufficient to do useful inference on<pre>
 *    ___.readPub(tmp___, 'y')
 * </pre>
 * where {@code 'y'} has a proper FilePosition, perhaps because it was
 * derived from {@link com.google.caja.parser.quasiliteral.Rule#toStringLiteral}
 * which propagates position info.
 *
 * @author mikesamuel@gmail.com
 */
public class InferFilePositionsStage implements Pipeline.Stage<Jobs> {
  public boolean apply(Jobs jobs) {
    for (Job job : jobs.getJobs()) {
      inferFilePositions(job.getRoot().node);
      inferFilePositions(job.getRoot().node);
    }
    return true;
  }

  private static List<FilePosition> inferFilePositions(ParseTreeNode node) {
    if (hasFilePosition(node)) {
      for (ParseTreeNode child : node.children()) {
        inferFilePositions(child);
      }
      return Collections.singletonList(node.getFilePosition());
    } else if (!node.children().isEmpty()) {
      List<FilePosition> positions = new ArrayList<FilePosition>();
      for (ParseTreeNode child : node.children()) {
        positions.addAll(inferFilePositions(child));
      }
      if (positions.isEmpty()) { return Collections.<FilePosition>emptyList(); }
      Collections.sort(
          positions,
          new Comparator<FilePosition>() {
            public int compare(FilePosition a, FilePosition b) {
              return a.source().getUri().compareTo(b.source().getUri());
            }
          });
      if (!positions.get(0).source().equals(
              positions.get(positions.size() - 1).source())) {
        InputSource last = null;
        int bestStart = -1;
        int runStart = 0;
        int bestRun = 0;
        int n = positions.size();
        for (int i = 0; i < n; ++i) {
          InputSource is = positions.get(i).source();
          if (!is.equals(last)) {
            if (i - runStart > bestRun) {
              bestStart = runStart;
              bestRun = i - runStart;
            }
            last = is;
            runStart = i;
          }
        }
        if (n - runStart > bestRun) {
          bestStart = runStart;
          bestRun = n - runStart;
        }
        positions = positions.subList(bestStart, bestStart + bestRun);
      }

      FilePosition min = positions.get(0), max = min;
      for (FilePosition pos : positions.subList(1, positions.size())) {
        if (min.startCharInFile() > pos.startCharInFile()) {
          min = pos;
        }
        if (max.endCharInFile() < pos.endCharInFile()) {
          max = pos;
        }
      }
      FilePosition span = FilePosition.span(min, max);
      setFilePosition(node, span);
      return Collections.singletonList(span);
    } else {
      return Collections.<FilePosition>emptyList();
    }
  }

  private static void setFilePosition(ParseTreeNode node, FilePosition pos) {
    ((AbstractParseTreeNode) node).setFilePosition(pos);
  }

  private static boolean hasFilePosition(ParseTreeNode node) {
    FilePosition pos = node.getFilePosition();
    return !FilePosition.UNKNOWN.equals(pos);
  }
}
