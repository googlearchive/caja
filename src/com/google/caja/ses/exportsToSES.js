// Copyright (C) 2012 Google Inc.
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
 * @fileoverview
 * Moves exported properties from "export" to "ses" to keep the global
 * environment clean.  The methods are added to ses.rewriter_ since
 * these are not part of the public ses interface.
 *
 * @author jasvir@gmail.com
 * \@overrides this
 * \@overrides ses
 * \@overrides exports
 */

var ses;

(function(global) {
  'use strict';
  global.ses = global.ses || {};

  ses.rewriter_ = {};
  ses.rewriter_.tokTypes = exports.tokTypes;
  ses.rewriter_.traverse = exports.traverse;
  ses.rewriter_.parse = exports.parse;
  ses.rewriter_.generate = exports.generate;

})(this);
