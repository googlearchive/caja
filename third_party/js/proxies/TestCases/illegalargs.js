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
  id: "illegalargs",
  path: "TestCases/illegalargs.js",

  description: 'proxies gracefully deal with illegal arguments passed to handler traps',

  test: function testcase() {
        
    var handler = {
      getOwnPropertyDescriptor: function(name) { assert('gopd name', name!==null); return undefined; },
      getPropertyDescriptor: function(name) { assert('gpd name', name!==null); return undefined; },
      getOwnPropertyNames: function() { return []; },
      defineProperty: function(name, desc) {
        assert('dP name', name!==null);
        assert('dP desc', desc!==null);
        return {};
      },
      'delete': function(name) { assert('delete name', name!==null); return true; },
      fix: function() { return undefined; }, // don't fix this proxy
       has: function(name) { assert('has name',name!==null); return false; },
      hasOwn: function(name) { assert('hasOwn name',name!==null); return false; },
      get: function(name, proxy) {
        assert('get proxy',proxy!==null);
        assert('get name',name!==null);
        return undefined;
      },
      set: function(name, val, proxy) {
        assert('set proxy',proxy!==null);
        assert('set name',name!==null);
        assert('set val',val===null);
        return true;
      },
      enumerate: function() { return []; },
      keys: function() { return []; }
    };
    
    function triggerTrapsOn(proxy) {
      var result;

      result = Object.getOwnPropertyDescriptor(proxy, null);
      assertEq('getOwnPropertyDescriptor null', undefined, result);

      // getPropertyDescriptor
      // FIXME: can't test as long as Object.getPropertyDescriptor is missing  

      // can't pass illegal arguments to getOwnPropertyNames()

      var p = Object.defineProperty(proxy, null, {});
      assert('defineProperty null', p === proxy);

      assertThrows('defineProperty null pd',TypeError, function() {
        Object.defineProperty(proxy, null, null);
      });

      result = delete proxy[null];
      assertEq('delete null', true, result);

      // can't pass illegal arguments to enumerate()

      // can't pass illegal arguments to fix()

      result = null in proxy;
      assertEq('has null', false, result);

      result = ({}).hasOwnProperty.call(proxy, null);
      assertEq('hasOwn null', false, result);

      result = (proxy[null] = null);
      assertEq('set null', null, result);

      result = proxy[null];
      assertEq('get null', undefined, result);

      // can't pass illegal arguments to keys() 
    }
    
    triggerTrapsOn(Proxy.create(handler));
    
    var funProxy = Proxy.createFunction(
      handler,
      function(var_args) { return null; });
    
    triggerTrapsOn(funProxy);
    assertEq('call null', null, funProxy());

    // Gotcha: we use slice.call on the args list, which promotes its
    // 'this' to an object.
    /*
    assertThrows('call with illegal args', TypeError,
      function() { Function.prototype.apply.call(funProxy,this,0) });
    */

    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});
