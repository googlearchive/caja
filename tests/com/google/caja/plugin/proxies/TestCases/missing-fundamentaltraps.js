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


// Missing fundamental traps should raise a TypeError
ES5Harness.registerTest( {
  id: "missing-fundamentaltraps",
  path: "TestCases/missing-fundamentaltraps.js",

  description: 'triggering a missing fundamental handler trap',

  test: function testcase() {
    
    function triggerMissingTrapsOn(proxy) {
      assertThrows('getOwnPropertyDescriptor missing', TypeError, function() {
        Object.getOwnPropertyDescriptor(proxy, 'foo');      
      });

      // getPropertyDescriptor
      // FIXME: can't test as long as Object.getPropertyDescriptor is missing  

      assertThrows('getOwnPropertyNames missing', TypeError, function() {
        Object.getOwnPropertyNames(proxy);
      });

      assertThrows('defineProperty missing', TypeError, function() {
        Object.defineProperty(proxy, 'foo', {});
      });

      assertThrows('delete missing', TypeError, function() {
        delete proxy.foo;
      });

      // TODO: enumerate is no longer a fundamental trap, but requires
      // support for Object.getPropertyDescriptor and Object.getPropertyNames
      // to be expressed as a derived trap.
      // see: http://wiki.ecmascript.org/doku.php?id=harmony:extended_object_api
      assertThrows('enumerate missing', TypeError, function() {
        for (var name in proxy) { }
      });
      
      assertThrows('fix missing', TypeError, function() {
        Object.preventExtensions(proxy);
      });
    }
    
    triggerMissingTrapsOn(Proxy.create({}));
    triggerMissingTrapsOn(Proxy.createFunction({}, function(){}));
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});
