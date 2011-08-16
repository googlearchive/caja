// Copyright (C) 2011 Google Inc.
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

/**
 * @fileoverview Configure initSES to operate as expected by caja.js
 * taming and guest frames.
 *
 * In particular, we disable the maximum-problem-severity check, because
 * caja.js will check the security itself and we want our code to
 * not fail partway through loading.
 *
 * TODO(kpreid): This strategy is insufficient to deal with non-ES5-capable
 * browsers, because we will crash partway through loading Domado or its deps.
 * Instead, we need to wrap code-after-initSES in a function and not run it
 * if SES failed, or load a separate frame to probe SES status in, or load
 * initSES with a callback and then load Domado etc. afterward.
 *
 * @author kpreid@switchb.org (Kevin Reid)
 * @overrides ses
 */

var ses;
if (!ses) { ses = {}; }
ses.maxAcceptableSeverityName = 'NEW_SYMPTOM';
