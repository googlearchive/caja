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

import com.google.caja.parser.AncestorChain;
import com.google.caja.parser.MutableParseTreeNode;
import com.google.caja.parser.js.Block;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.plugin.Job;
import com.google.caja.plugin.Jobs;
import com.google.caja.plugin.TreeConstruction;
import com.google.caja.util.Pipeline;
import static com.google.caja.plugin.SyntheticNodes.s;

import java.util.Arrays;
import java.util.Collections;
import java.util.ListIterator;

/**
 * Put all the top level javascript code into an initializer block
 * that will set up the plugin.
 *
 * @author mikesamuel@gmail.com
 */
public final class ConsolidateCodeStage implements Pipeline.Stage<Jobs> {
  public boolean apply(Jobs jobs) {
    // create an initializer function
    Block initFunctionBody = s(new Block(Collections.<Statement>emptyList()));

    MutableParseTreeNode.Mutation mut = initFunctionBody.createMutation();
    
    ListIterator<Job> it = jobs.getJobs().listIterator();
    while (it.hasNext()) {
      Job job = it.next();
      if (Job.JobType.JAVASCRIPT != job.getType()) { continue; }
      
      Block body = (Block) job.getRoot().node;
      MutableParseTreeNode.Mutation old = body.createMutation();
      for (Statement s : body.children()) {
        old.removeChild(s);
        mut.appendChild(s);
      }
      old.execute();
      
      it.remove();
    }
    mut.execute();
    // Now initFunctionBody contains all the top level statements.

    // ___.loadModule(function (<namespace>) { <compiled code> })
    Block jsTree = s(new Block(Arrays.asList(
        s(new ExpressionStmt(TreeConstruction.call(
            TreeConstruction.memberAccess("___", "loadModule"),
            TreeConstruction.function(  // function (___OUTERS___)
                null, initFunctionBody,
                jobs.getPluginMeta().namespaceName)))))));

    jobs.getJobs().add(new Job(new AncestorChain<Block>(jsTree)));

    return jobs.hasNoFatalErrors();
  }
}

