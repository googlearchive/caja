// Copyright (C) 2007 Google Inc.
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
// .............................................................................

// This module overrides the generic fault-handlers from the Caja
// runtime library in order to permit access to virtually all
// pre-existing JavaScript APIs (such as the browser environment).
// Loading this module thereby waives all protection. Instead, it logs
// the first access in order to prioritize taming decisions. After
// logging the first access, it explicitly allows further accesses, so
// that they won't generate further faults. After paying for these
// initial faults, the program will then run at full Caja speed,
// giving us a realistic measure of the slowdown as well.

// This module is written in Javascript, not Caja, and would be
// rejected by the Caja translator. It depends on caja.js, and should
// only be loaded after caja.js.


(function() {
  
  ___.log('BEWARE: By loading permissive.js, ' +
          'all Caja security is hereby waived');
  
  /**
   * 
   */
  function find(obj, name) {
    var result = obj;
    while (typeof result === 'object' || typeof result === 'function') {
      if (___.hasOwnProp(result, name)) {
        return result;
      }
      if (___.hasOwnProp(result, '__proto__')) {
        result = result.__proto__;
      } else {
        result = ___.directConstructor(result).prototype;
      }
    }
    ___.fail("Internal: can't find '.", name, "' in: ", obj);
  }
  
  var oldKeeper = ___.getKeeper();
  
  ___.setKeeper({

    /**
     *
     */
    toString: function() { return '<Permissive Keeper>'; },

    /**
     * 
     */
    handleRead: function(obj, name) {
      if (name in obj) {
        var proto = find(obj, name);
        if (proto === obj) {
          ___.log('Allowing read of (' + obj + ').' + name);
        } else {
          var constr = proto.constructor;
          ___.log('Allowing read of ".' + name + '" for: ' + constr);
        }
        ___.allowRead(proto, name);
        return obj[name];
      }
      return oldKeeper.handleRead(obj, name);
    },

    /**
     * 
     */
    handleCall: function(obj, name, args) {
      if (typeof obj[name] === 'function') {
        var proto = find(obj, name);
        if (proto === obj) {
          ___.log('Allowing call of (' + obj + ').' + name + '()');
        } else {
          var constr = proto.constructor;
          ___.log('Allowing call of ".' + name + '()" for: ' + constr);
        }
        ___.allowCall(proto, name);
        return obj[name].apply(obj, args);
      }
      return oldKeeper.handleCall(obj, name, args);
    },

    /**
     * 
     */
    handleSet: function(obj, name, val) {
      ___.log('Allowing (' + obj + ').' + name + ' = ...');
      ___.allowSet(obj, name);
      obj[name] = val;
      if (obj[name] === val) {
        return val;
      }
      return oldKeeper.handleSet(obj, name, val);
    },

    /**
     * 
     */
    handleDelete: function(obj, name) {
      if (___.hasOwnProp(obj, name)) {
        ___.log('Allowing delete (' + obj + ').' + name);
        ___.allowDelete(obj, name);
        if (delete obj[name]) {
          return true;
        }
      }
      return oldKeeper.handleDelete(obj, name);
    }
  });
  
})();
