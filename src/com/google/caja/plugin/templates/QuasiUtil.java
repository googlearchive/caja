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

package com.google.caja.plugin.templates;

import com.google.caja.lexer.FilePosition;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.parser.js.Expression;
import com.google.caja.parser.js.ExpressionStmt;
import com.google.caja.parser.js.Statement;
import com.google.caja.parser.quasiliteral.QuasiBuilder;

public final class QuasiUtil {
  public static Statement quasiStmt(String quasi, Object... args) {
    ParseTreeNode n = QuasiBuilder.substV(quasi, args);
    if (n instanceof Expression) {
      return new ExpressionStmt(FilePosition.UNKNOWN, (Expression) n);
    }
    return (Statement) n;
  }
}
