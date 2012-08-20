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
 * Policy factory for test api named "google.foo"; see es53-test-apitaming.js.
 */
caja.tamingGoogleLoader.addPolicyFactory('foo', function(frame, utils) {

  var f = {};

  f.getValue = function() {};

  f.advisedBA = function() {};
  f.advisedBA.__before__ = [
    utils.mapArgs(function(x) { return 'b' + x + 'b'; })
  ];
  f.advisedBA.__after__ = [
    utils.mapResult(function(x) { return 'a' + x + 'a'; })
  ];

  f.advisedAround = function() {};
  f.advisedAround.__around__ = [
    function(f, self, args) {
      return 'a' + f(self, args) + 'a';
    }
  ];

  f.Sup = function(x) {};
  f.Sup.__super__ = Object;
  f.Sup.someStatic = function(x) {};
  f.Sup.prototype.getX = function() {};
  f.Sup.prototype.setX = function(x) {};

  f.Sub = function(x, y) {};
  f.Sub.__super__ = [ 'google', 'foo', 'Sup' ];
  f.Sub.prototype.getY = function() {};
  f.Sub.prototype.setY = function(y) {};

  return f;
});
