// Copyright (C) 2013 Google Inc.
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
 * This file exists to be concatenated into the taming frame file that caja.js
 * loads to bail out in a controlled fashion if SES fails to load (rather than
 * losing the callback signal when later dependent code fails by trying to use
 * SES features).
 *
 * @author kpreid@switchb.org
 * @requires ses, cajaIframeDone___
 */

if (typeof ses !== 'undefined' && ses.ok && !ses.ok()) {
  cajaIframeDone___();

  // Cause a well-defined error to prevent anything further from happening
  // (such as a more cryptic error or a second call to cajaIframeDone___).
  throw new Error('SES not supported, aborting taming frame initialization.');
}
