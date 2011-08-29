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


// What is the semantics of an operation if the handler does not define a trap
// method for it? When a proxy implementation queries a handler for the name
// of a derived trap, if the result of the lookup is undefined, the proxy
// implementation should fall back on the “default implementation” of the
// derived trap in terms of the required, fundamental traps.
// Source: http://wiki.ecmascript.org/doku.php?id=harmony:proxies (rev 08/05/10)
ES5Harness.registerTest( {
  id: "missing-derivedtraps",
  path: "TestCases/missing-derivedtraps.js",

  description: 'triggering a missing derived handler trap',

  test: function testcase() {
    
    function triggerMissingDerivedTrapsOn(proxy) {
      var result;
      
      // has
      result = 'foo' in proxy;
      assert('default has', result === true);

      // hasOwn
      result = ({}).hasOwnProperty.call(proxy, 'foo');
      assert('default hasOwn', result === true);

      // get
      result = proxy.foo;
      assert('default get', result === 42);

      // set
      proxy.foo = 0;
      assert('default set took effect', proxy.foo === 0);
      
      // keys
      result = Object.keys(proxy);
      assert('default keys', sameStructure(['foo'], result));
    }
    
    function createFundamentalsOnlyHandlerFor(obj) {
      // a forwarding handler that only implements the fundamental traps
      // and assumes the default implementation for all of its derived traps
      return {
        getOwnPropertyDescriptor: function(name) {
          return Object.getOwnPropertyDescriptor(obj, name);
        },
        getPropertyDescriptor: function(name) {
          // FIXME: should be able to just call:
          // return Object.getPropertyDescriptor(obj, name);
          var cur = obj;
          var pd = Object.getOwnPropertyDescriptor(obj, name);
          while (pd === undefined && cur !== Object.prototype) {
            cur = Object.getPrototypeOf(cur);
            pd = Object.getOwnPropertyDescriptor(cur, name);
          }
          return pd;
        },
        getOwnPropertyNames: function() {
          return Object.getOwnPropertyNames(obj);
        },
        defineProperty: function(name, pd) {
          return Object.defineProperty(obj, name, pd);
        },
        delete: function(name) {
          delete obj[name];
          return true;
        },
        // TODO: enumerate is now a derived trap, but requires
        // support for Object.getPropertyDescriptor and Object.getPropertyNames
        // see: http://wiki.ecmascript.org/doku.php?id=harmony:extended_object_api
        enumerate: function() {
          var props = [];
          for (var prop in obj) { props.push(prop); }
          return props;
        },
        fix: function() { return undefined; }
      };
    };
    
    triggerMissingDerivedTrapsOn(
      Proxy.create(createFundamentalsOnlyHandlerFor({foo:42})));
      
    triggerMissingDerivedTrapsOn(
      Proxy.createFunction(createFundamentalsOnlyHandlerFor({foo:42}), function(){}));
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});
