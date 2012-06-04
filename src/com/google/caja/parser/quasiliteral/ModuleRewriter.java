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
import com.google.caja.parser.js.CajoledModule;
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
   * This used to produce a module containing a {@code moduleMap___}
   * definition for {@code load(...)} rule expansion, but that's gone now.
   * Now this just turns a list of modules into a single module.
   */
  public CajoledModule rewrite(List<CajoledModule> modules) {
    if (modules.size() == 1) { return modules.get(0); }

    FilePosition unk = FilePosition.UNKNOWN;
    BuildInfo buildInfo = mgr.getBuildInfo();

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
        + "    return @moduleInvocations?;"
        + "  },"
        + "  cajolerName: @cajolerName,"
        + "  cajolerVersion: @cajolerVersion,"
        + "  cajoledDate: @cajoledDate"
        + "})",
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

  /**
   * @param isFromValija not used since obsolete but this is part of a public
   *     API that may be used by external clients.
   */
  public ModuleRewriter(
      PluginMeta meta, BuildInfo buildInfo,
      boolean isFromValija, MessageQueue mq) {
    this(meta, buildInfo, UriFetcher.NULL_NETWORK, mq);
  }
}
