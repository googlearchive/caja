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


// Interaction between proxies and the instanceof operator:
// Two cases to consider:
//  case 1: obj instanceof aFunctionProxy
//    aFunctionProxy can influence the result of this operator if its handler’s get
//    trap returns a meaningful value for the 'prototype' property. Internally, the
//    instanceof operator invokes the built-in method [[HasInstance]] on the function,
//    which queries the function’s prototype property.
//
//  case 2: anObjectProxy instanceof aConstructorFunction
//    The proto object passed to the Proxy.create method is used to determine the
//    prototype of the proxy.
//
// Source: http://wiki.ecmascript.org/doku.php?id=harmony:proxies (rev 2/25/10)
ES5Harness.registerTest( {
  id: "instanceof",
  path: "TestCases/instanceof.js",

  description: 'test interaction between proxies and instanceof',

  test: function testcase() {
    function TestType() { };
    // Patched for Caja: ES5/3 gotcha: inherit only from frozen
    Object.freeze(TestType.prototype);
    
    //  case 1: obj instanceof aFunctionProxy
    
    // Patched for Caja: ES5/3 gotcha: can't intercept instanceof
    /*
    var funProxy = Proxy.createFunction({
      get: function(receiver, name) {
        assert('get trap called for "prototype"', name === 'prototype');
        return TestType.prototype;
      }
    }, function(var_args) { return null; });
    
    var instance = new TestType();
    assert('instance instanceof funProxy', instance instanceof funProxy);
    assert('{} !instanceof funProxy', false === ({} instanceof funProxy));
    
    
    // test what happens when a function proxy returns no meaningful value
    // for 'prototype'
    assertThrows('instance instanceof bogusFunction', TypeError, function() {
      instance instanceof (Proxy.createFunction({
        get: function(receiver, name) {
          assert('get trap called for "prototype"', name === 'prototype');
          return undefined;
        }
      }, function(var_args){return null;}));
    });
    */
    
    //  case 2: anObjectProxy instanceof aConstructorFunction
    
    var objProxy = Proxy.create({}, TestType.prototype);
    assert('objProxy instanceof TestType', objProxy instanceof TestType);
    
    // Patched for Caja: ES5/3 gotcha:
    // Don't want to freeze String.prototype and proxies
    // can't inherit from non-extensible objects.
    /*
    var objProxyWithPrimitivePrototype = Proxy.create({}, String.prototype);
    assert('objProxyWPrimType instanceof String',
           objProxyWithPrimitivePrototype instanceof String);
    */
           
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});
