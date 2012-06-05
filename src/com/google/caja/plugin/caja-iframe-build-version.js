// Copyright (C) 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview
 * This file is concatenated into the top of the iframe scripts that caja.js
 * loads. It supplies the current build version of Caja. This is interpolated
 * into this file via build.xml rules.
 *
 * @provides cajaBuildVersion
 * @overrides window
 */

var cajaBuildVersion = '%VERSION%';

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['cajaBuildVersion'] = cajaBuildVersion;
}
