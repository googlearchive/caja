/// Copyright (c) 2010 Mozilla Foundation 
/// 
/// Redistribution and use in source and binary forms, with or without modification, are permitted provided
/// that the following conditions are met: 
///    * Redistributions of source code must retain the above copyright notice, this list of conditions and
///      the following disclaimer. 
///    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
///      the following disclaimer in the documentation and/or other materials provided with the distribution.  
///    * Neither the name of Mozilla Foundation nor the names of its contributors may be used to
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
  id: "objectproxies",
  path: "TestCases/objectproxies.js",

  description: 'Object Proxies',

  test: function testcase() {

    function noopHandlerMaker(obj) {
        return {
      getOwnPropertyDescriptor: function(name) {
          var desc = Object.getOwnPropertyDescriptor(obj);
          // a trapping proxy's properties must always be configurable
          desc.configurable = true;
          return desc;
      },
      getPropertyDescriptor: function(name) {
          var desc = Object.getPropertyDescriptor(obj); // assumed
          // a trapping proxy's properties must always be configurable
          desc.configurable = true;
          return desc;
      },
      getOwnPropertyNames: function() {
          return Object.getOwnPropertyNames(obj);
      },
      defineProperty: function(name, desc) {
          return Object.defineProperty(obj, name, desc);
      },
      'delete': function(name) { return delete obj[name]; },
      fix: function() {
          // As long as obj is not frozen, the proxy won't allow itself to be fixed
          // if (!Object.isFrozen(obj)) [not implemented in SpiderMonkey]
          //     return undefined;
          // return Object.getOwnProperties(obj); // assumed [not implemented in SpiderMonkey]
          var props = { isTrapping: { value: false } };
          for (x in obj)
        props[x] = Object.getOwnPropertyDescriptor(obj, x);
          return props;
      },
       has: function(name) { return name in obj; },
      hasOwn: function(name) { return ({}).hasOwnProperty.call(obj, name); },
      get: function(name, proxy) {
        if (name === 'isTrapping') return true;
        return obj[name];
      },
      set: function(name, val, proxy) { obj[name] = val; return true; }, // bad behavior when set fails in non-strict mode
      enumerate: function() {
          var result = [];
          for (name in obj) { result.push(name); };
          return result;
      },
      keys: function() { return Object.keys(obj); }
        };
    };

    function testNoopHandler(obj, proxy) {
        /* Check that both objects see the same properties. */
        for (x in obj)
      assertEq("for-in " + x, obj[x], proxy[x]);
        for (x in proxy)
      assertEq("for-in " + x, obj[x], proxy[x]);
        /* Check that the iteration order is the same. */
        var a = [], b = [];
        for (x in obj)
      a.push(x);
        for (x in proxy)
      b.push(x);
        assertEq("iter-order", uneval(a), uneval(b));
    }

    var obj = new Date();
    var proxy = Proxy.create(noopHandlerMaker(obj));

    testNoopHandler(obj, proxy);
    assertEq("isTrapping(Proxy) before fix", proxy.isTrapping, true);
    assertEq("typeof object proxy", typeof proxy, "object");
    Object.preventExtensions(proxy);
    assertEq("isTrapping(Proxy) after fix", proxy.isTrapping, false);
    assertEq("typeof object proxy after fix", typeof proxy, "object");
    testNoopHandler(obj, proxy);

    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined" && !!Object.preventExtensions;
  }
});
