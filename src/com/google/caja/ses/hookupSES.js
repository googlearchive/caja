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
 * @fileoverview Call {@code ses.startSES} to turn this frame into a
 * SES environment following object-capability rules.
 *
 * <p>Assumes ES5 plus WeakMap. Compatible with ES5-strict or
 * anticipated ES6.
 *
 * @author Mark S. Miller
 * @requires this
 * @overrides ses, hookupSESModule
 */

(function hookupSESModule(global) {
  "use strict";

  if (!ses.ok()) {
    return;
  }

  try {
    ses.startSES(global,
                 ses.whitelist,
                 ses.atLeastFreeVarNames,
                 function () { return {}; });
  } catch (err) {
    ses.updateMaxSeverity(ses.severities.NOT_SUPPORTED);
    ses.logger.error('hookupSES failed with: ', err);
  }
})(this);
