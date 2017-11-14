/// Copyright (c) 2010 Google Inc. 
/// 
/// Redistribution and use in source and binary forms, with or without modification, are permitted provided
/// that the following conditions are met: 
///    * Redistributions of source code must retain the above copyright notice, this list of conditions and
///      the following disclaimer. 
///    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
///      the following disclaimer in the documentation and/or other materials provided with the distribution.  
///    * Neither the name of Google Inc. nor the names of its contributors may be used to
///      endorse or promote products derived from this software without specific prior written permission.
/// 
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
/// IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
/// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
/// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
/// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
/// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
/// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
/// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 

ES5Harness.registerTest( {
  id: "missingargs",
  path: "TestCases/missingargs.js",

  description: 'call Proxy.create{Function} with missing/illegal arguments',

  test: function testcase() {
    
    assertThrows('Proxy.create requires more than 0 args', TypeError,
                 function() { Proxy.create(); });
    
    assertThrows('handler is not non-null object', TypeError,
                 function() { Proxy.create(null); });
    
    assertThrows('handler is not non-null object', TypeError,
                 function() { Proxy.create(0); });
    
    // Patched for Caja: ES5/3 gotcha: cannot inherit null
    /*
    assertEq('default prototype is null',
             null,
             Object.getPrototypeOf(Proxy.create({})));
    */

    assertThrows('Proxy.createFunction requires more than 0 args', TypeError,
                 function() { Proxy.createFunction(); });

    assertThrows('Proxy.createFunction requires more than 1 arg', TypeError,
                 function() { Proxy.createFunction({}); });

    assertThrows('Proxy.createFunction requires more than 1 arg', TypeError,
                 function() { Proxy.createFunction(null); });
    
    assertThrows('Proxy.createFunction arg is not a function', TypeError,
                 function() { Proxy.createFunction({}, null); });    
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});
