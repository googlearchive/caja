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
  id: "handlersharing",
  path: "TestCases/handlersharing.js",

  description: 'handler can handle multiple proxies',

  test: function testcase() {
    
    var proxy1;
    var proxy2;
    
    var handler = {
      get: function(name, proxy) {
        if (name === 'isTrapping') {
          return true;
        }
        if (proxy === proxy1) {
          return 1;
        } else {
          return 2;
        }
      },
      fix: function() {
        return {
          isTrapping: { value: false }
        };
      }
    };
    
    proxy1 = Proxy.create(handler);
    proxy2 = Proxy.create(handler);
    
    assertEq('proxy1.foo', 1, proxy1.foo);
    assertEq('proxy2.foo', 2, proxy2.foo);
    
    assert('proxy1 trapping', proxy1.isTrapping);
    assert('proxy2 trapping', proxy2.isTrapping);

    // assert('proxy1 fixed', Proxy.fix(proxy1));
    Object.preventExtensions(proxy1);

    assert('proxy1 not trapping', ! proxy1.isTrapping);
    assert('proxy2 still trapping', proxy2.isTrapping);
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined" && !!Object.preventExtensions;
  }
});
