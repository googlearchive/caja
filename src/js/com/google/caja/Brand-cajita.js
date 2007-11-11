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

// This module is written in the Caja subset of Javascript. It should
// work whether run translated or untranslated. Either way, it depends
// on caja.js, but not on anything else.

/**
 * Returns a matched Sealer/Unsealer pair, where the boxes produced by
 * a Sealer can be unsealed only by the Unsealer of the same pair.
 */
function Brand(name) {
    caja.requireType(name,'string');
    var flag = false;
    var squirrel = null;

    /** Returns a sealed box containing the payload. */
    function Sealer(payload) {

        /** 
         * Encapsulates the payload, but makes it available to its
         * Unsealer.
         */
        function Box() {
            squirrel = payload;
            flag = true;
        }
        caja.def(Box, Object, {}, {
            toString: function() { return '<'+name+' box>'; }
        });
        return Box;
    }
    caja.def(Sealer, Object, {}, {
        toString: function() { return '<'+name+' sealer>'; }
    });

    /**
     * Obtains the payload sealed within a Box sealer only by our Sealer.
     */
    function Unsealer(box) {
        flag = false; squirrel = null;
        box();
        caja.require(flag,'not my box: '+box);
        var result = squirrel;
        // next two lines are probably unneeded, but just in case
        flag = false; squirrel = null;
        return result;
    }
    caja.def(Unsealer, Object, {}, {
        toString: function() { return '<'+name+' unsealer>'; }
    });
    return caja.freeze({
        toString: function() { return '<'+name+' brand>'; },
        seal: Sealer,
        unseal: Unsealer
    });
}
