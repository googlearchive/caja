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
import com.google.caja.parser.ParseTreeNodeContainer;
import com.google.caja.parser.js.CajoledModule;
import com.google.caja.parser.js.CajoledModuleExpression;
import com.google.caja.parser.js.Declaration;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.IntegerLiteral;
import com.google.caja.parser.js.ObjectConstructor;
import com.google.caja.parser.js.Operation;
import com.google.caja.parser.js.Operator;
import com.google.caja.parser.js.StringLiteral;
import com.google.caja.plugin.PluginMeta;
import com.google.caja.plugin.UriFetcher;
import com.google.caja.reporting.BuildInfo;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.Lists;

import java.util.List;

/**
 * Adds a top level module map.
 *
 * @author maoziqing@gmail.com
 */
public class ModuleRewriter {
  private final ModuleManager mgr;

  public ModuleManager getModuleManager() { return mgr; }

  /**
   * Produces a module containing a {@code moduleMap___} definition that
   * introduces all the bindings needed by the {@code load(...)} rule
   * expansion.
   */
  public CajoledModule rewrite(List<CajoledModule> modules) {
    List<CajoledModule> byIndex = mgr.getModuleMap();
    if (modules.size() == 1 && byIndex.isEmpty()) { return modules.get(0); }

    FilePosition unk = FilePosition.UNKNOWN;
    BuildInfo buildInfo = mgr.getBuildInfo();

    Declaration moduleMap = null;
    if (!byIndex.isEmpty()) {
      List<Expression> indexModules = Lists.newArrayList();
      for (int i = 0, n = byIndex.size(); i < n; ++i) {
        CajoledModule m = byIndex.get(i);
        indexModules.add(new CajoledModuleExpression(m));
      }
      moduleMap = (Declaration) QuasiBuilder.substV(
          "var moduleMap___ = [@modules*];",
          "modules", new ParseTreeNodeContainer(indexModules));
    }
    Expression moduleInvocations = null;
    for (CajoledModule module : modules) {
      Expression invocation = (Expression) QuasiBuilder.substV(
              "___.prepareModule(@moduleBody).instantiate___(___, IMPORTS___)",
              "moduleBody", module.getModuleBody());
      moduleInvocations = moduleInvocations != null
          ? Operation.createInfix(Operator.COMMA, moduleInvocations, invocation)
          : invocation;
    }

    ObjectConstructor oc = (ObjectConstructor) QuasiBuilder.substV(
        ""
        + "({"
        + "  instantiate: function (___, IMPORTS___) {"
        + "    @moduleMap?;"
        + "    return @moduleInvocations?;"
        + "  },"
        + "  cajolerName: @cajolerName,"
        + "  cajolerVersion: @cajolerVersion,"
        + "  cajoledDate: @cajoledDate"
        + "})",
        "moduleMap", moduleMap,
        "moduleInvocations", moduleInvocations,
        "cajolerName", StringLiteral.valueOf(unk, "com.google.caja"),
        "cajolerVersion", StringLiteral.valueOf(
            unk, buildInfo.getBuildVersion()),
        "cajoledDate", new IntegerLiteral(unk, buildInfo.getCurrentTime()));
    return new CajoledModule(oc);
  }

  public ModuleRewriter(ModuleManager mgr) {
    this.mgr = mgr;
  }

  public ModuleRewriter(
      PluginMeta meta, BuildInfo buildInfo, UriFetcher uriFetcher,
      MessageQueue mq) {
    this(new ModuleManager(meta, buildInfo, uriFetcher, mq));
  }

  public ModuleRewriter(
      PluginMeta meta, BuildInfo buildInfo,
      boolean isFromValija, MessageQueue mq) {
    this(meta, buildInfo, UriFetcher.NULL_NETWORK, mq);
  }
}
