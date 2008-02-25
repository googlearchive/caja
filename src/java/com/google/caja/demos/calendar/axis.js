// Copyright (C) 2008 Google Inc.
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
 * @enum {object}
 */
var Axis = {
  X: {
    position: function (rect) { return rect.col0; },
    extent: function (rect) { return rect.col1 - rect.col0; },
    toString: function () { return 'Axis.X'; }
  },
  Y: {
    position: function (rect) { return rect.row0; },
    extent: function (rect) { return rect.row1 - rect.row0; },
    toString: function () { return 'Axis.Y'; }
  }
};
