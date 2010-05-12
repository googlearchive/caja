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

package com.google.caja.parser.quasiliteral;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.CajoledModuleExpression;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.parser.js.UncajoledModule;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;

import java.util.List;
import java.util.Map;

/**
 * Add a top-level module envelope
 * Call the CajitaRewriter to do the actual rewriting
 *
 * @author maoziqing@gmail.com
 */
@RulesetDescription(
    name="Caja Transformation Rules",
    synopsis="Top-level module envelop"
  )
public class CajitaModuleRewriter extends Rewriter {
  private final BuildInfo buildInfo;
  private final UriFetcher uriFetcher;
  private final boolean isFromValija;

  final public Rule[] cajaRules = {
    new Rule() {
      @Override
      @RuleDescription(
          name="topLevelModule",
          synopsis="produce a module map and add a wrapper;"
            + "only apply to top level module",
          reason="",
          matches="<an UncajoledModule>",
          substitutes=(
              "      ({"
              + "      instantiate: function (___, IMPORTS___) {"
              + "        var moduleResult___ = ___.NO_RESULT;"
              + "        var moduleMap___ = {};"
              + "        @setModules*;"
              + "        moduleResult___ ="
              + "          moduleMap___[0](IMPORTS___);"
              + "        return moduleResult___;"
              + "      },"
              + "      cajolerName: @cajolerName,"
              + "      cajolerVersion: @cajolerVersion,"
              + "      cajoledDate: @cajoledDate"
              + "    })"))
      public ParseTreeNode fire(ParseTreeNode node, Scope scope) {
        if (node instanceof UncajoledModule) {
          ModuleManager moduleManager = new ModuleManager(
              buildInfo, uriFetcher, mq, isFromValija);
          moduleManager.appendUncajoledModule((UncajoledModule)node);

          List<ParseTreeNode> moduleDefs = Lists.newArrayList();
          Map<Integer, CajoledModule> modules
              = moduleManager.getModuleIndexMap();

          if (modules.size() == 1) {
            return modules.get(0);
          }
          else {
            for (int k : modules.keySet()) {
              ParseTreeNode e = QuasiBuilder.substV(
                "moduleMap___[@moduleIndex] = @cajoledModuleExpression;",
                "moduleIndex", new IntegerLiteral(
                    FilePosition.UNKNOWN, k),
                "cajoledModuleExpression", new CajoledModuleExpression(
                    FilePosition.UNKNOWN, modules.get(k)));

              moduleDefs.add(new ExpressionStmt((Expression)e));
            }

            ObjectConstructor moduleObjectLiteral = (ObjectConstructor) substV(
                "setModules", new ParseTreeNodeContainer(moduleDefs),
                "cajolerName", new StringLiteral(
                    FilePosition.UNKNOWN, "com.google.caja"),
                "cajolerVersion", new StringLiteral(
                    FilePosition.UNKNOWN,
                    buildInfo.getBuildVersion()),
                "cajoledDate", new IntegerLiteral(
                    FilePosition.UNKNOWN,
                    buildInfo.getCurrentTime()));
            return new CajoledModule(moduleObjectLiteral);
          }
        }
        return NONE;
      }
    }
  };

  /**
   * Creates a CajitaModuleRewriter
   */
  public CajitaModuleRewriter(
      BuildInfo buildInfo, UriFetcher fetcher,
      MessageQueue mq, boolean logging, boolean isFromValija) {
    super(mq, false, logging);
    this.buildInfo = buildInfo;
    this.uriFetcher = fetcher;
    this.isFromValija = isFromValija;
    addRules(cajaRules);
  }

  public CajitaModuleRewriter(
      BuildInfo buildInfo, MessageQueue mq, boolean logging,
      boolean isFromValija) {
    this(buildInfo, UriFetcher.NULL_NETWORK, mq, logging, isFromValija);
  }
}
