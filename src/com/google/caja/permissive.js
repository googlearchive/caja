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
// rejected by the Caja translator. It depends on cajita.js, and should
// only be loaded after cajita.js.


(function() {

  ___.log('BEWARE: permissive.js loaded');
  
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

  var enabled = false;
  
  ___.setKeeper({

    /**
     *
     */
    toString: function() { return '<Permissive Keeper>'; },

    /**
     *
     */
    isEnabled: function() { return enabled; },

    /**
     * After loading permissive, one can 
     * <tt>___.getKeeper().setEnabled(false)</tt>
     * to disable the permissive behavior of the permissive keeper.
     * <p>
     * Note that this only causes it to stop allowing newly faulted
     * things, but does not reverse the allowances it has already made
     * in reaction to faults when it was enabled. In other words,
     * disabling this keeper is a <i>desist</i>, not an
     * <i>undo</i>. To get the effect of an undo, you must reload the
     * page. (Or, in a non-browser environment, you must still somehow
     * rebuild your live JavaScript environment.)
     */
    setEnabled: function(newEnabled) { 
      if (newEnabled) {
        ___.log('BEWARE: By enabling permissive.js, ' +
                'all Caja security is hereby waived.');
      } else {
        ___.log('BEWARE: Disabling permissive.js only stops it ' +
                'from allowing further operations in response to new ' +
                'faults. It does not disallow those operations ' +
                'already allowed. Consider reloading the page.');
      }
      enabled = newEnabled; 
    },

    /**
     * 
     */
    handleRead: function(obj, name) {
      if (enabled && name in obj) {
        var proto = find(obj, name);
        if (proto === obj) {
          ___.log('Allowing read of (' + obj + ').' + name);
        } else {
          var constr = proto.constructor;
          ___.log('Allowing read of ".' + name + '" for: ' + constr);
        }
        ___.grantRead(proto, name);
        return obj[name];
      }
      return oldKeeper.handleRead(obj, name);
    },

    /**
     * 
     */
    handleCall: function(obj, name, args) {
      if (enabled && typeof obj[name] === 'function') {
        var proto = find(obj, name);
        if (proto === obj) {
          ___.log('Allowing call of (' + obj + ').' + name + '()');
        } else {
          var constr = proto.constructor;
          ___.log('Allowing call of ".' + name + '()" for: ' + constr);
        }
        ___.grantCall(proto, name);
        return obj[name].apply(obj, args);
      }
      return oldKeeper.handleCall(obj, name, args);
    },

    /**
     * 
     */
    handleSet: function(obj, name, val) {
      if (enabled) {
        ___.log('Allowing (' + obj + ').' + name + ' = ...');
        ___.grantSet(obj, name);
        obj[name] = val;
        if (obj[name] === val) {
          return val;
        }
      }
      return oldKeeper.handleSet(obj, name, val);
    },

    /**
     * 
     */
    handleDelete: function(obj, name) {
      if (enabled && ___.hasOwnProp(obj, name)) {
        ___.log('Allowing delete (' + obj + ').' + name);
        ___.grantDelete(obj, name);
        if (delete obj[name]) {
          return true;
        }
      }
      return oldKeeper.handleDelete(obj, name);
    }
  });
  
})();
