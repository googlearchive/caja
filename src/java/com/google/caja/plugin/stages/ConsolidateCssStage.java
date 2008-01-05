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

package com.google.caja.plugin.stages;

import com.google.caja.lexer.FilePosition;
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.css.CssTree;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.util.Pipeline;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Consolidate different CSS inputs from multiple {@code <style>} tags into
 * one CssTree.
 * 
 * @author mikesamuel@gmail.com
 */
public final class ConsolidateCssStage implements Pipeline.Stage<Jobs> {
  public boolean apply(Jobs jobs) {
    List<CssTree.CssStatement> children = new ArrayList<CssTree.CssStatement>();
    ListIterator<Job> it = jobs.getJobs().listIterator();
    while (it.hasNext()) {
      Job job = it.next();
      if (job.getType() != Job.JobType.CSS) { continue; }

      CssTree.StyleSheet styleSheet = (CssTree.StyleSheet) job.getRoot().node;
      int firstToRemove = children.size();
      for (CssTree child : styleSheet.children()) {
        children.add((CssTree.CssStatement) child);
      }

      it.remove();
    }

    if (!children.isEmpty()) {
      // Create a file position for the root since a FilePosition cannot span
      // multiple InputSources
      FilePosition pos = FilePosition.startOfFile(
          new InputSource(URI.create("consolidated:///css")));

      CssTree.StyleSheet stylesheet = new CssTree.StyleSheet(pos, children);
      
      jobs.getJobs().add(
          new Job(new AncestorChain<CssTree.StyleSheet>(stylesheet)));
    }
    return true;
  }
}
