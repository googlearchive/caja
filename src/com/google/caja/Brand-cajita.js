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

// This module is written in the Cajita subset of Javascript. It should
// work whether run translated or untranslated. Either way, it depends
// on cajita.js, but not on anything else.

/**
 * Returns a matched sealer/unsealer pair, where the boxes produced by
 * a sealer can be unsealed only by the unsealer of the same pair.
 *
 * @author Mark S. Miller, based on a pattern invented by Marc Stiegler.
 */
function Brand(name) {
  cajita.enforceType(name,'string');
  var flag = false;
  var squirrel = null;
  
  var sealer = cajita.freeze({
    toString: function() { return '<'+name+' sealer>'; },
    
    /** Returns a sealed box containing the payload. */
    seal: function(payload) {
      
      /** 
       * Encapsulates the payload, but makes it available to its
       * unsealer when provoked.
       */
      return cajita.freeze({
        toString: function() { return '<'+name+' box>'; },
        provoke: function() {
          squirrel = payload;
          flag = true;
        }
      });
    }
  });
  
  var unsealer = cajita.freeze({
    toString: function() { return '<'+name+' unsealer>'; },
    
    /**
     * Obtains the payload sealed within a box sealer only by our sealer.
     */
    unseal: function(box) {
      flag = false; 
      squirrel = null;
      box.provoke();
      if (!flag) { cajita.fail('not my box: ',box); }
      var result = squirrel;
      // next two lines are probably unneeded, but just in case
      flag = false; 
      squirrel = null;
      return result;
    }
  });
  return cajita.freeze({
    toString: function() { return '<'+name+' brand>'; },
    sealer: sealer,
    unsealer: unsealer
  });
}
