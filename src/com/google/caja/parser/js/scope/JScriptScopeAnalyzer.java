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

package com.google.caja.parser.js.scope;

public class JScriptScopeAnalyzer<S extends AbstractScope>
    extends ES5ScopeAnalyzer<S> {
  public JScriptScopeAnalyzer(ScopeListener<S> listener) { super(listener); }

  @Override protected boolean fnCtorsDeclareInBody() { return false; }
  @Override protected boolean fnCtorsDeclareInContaining() { return true; }
}
