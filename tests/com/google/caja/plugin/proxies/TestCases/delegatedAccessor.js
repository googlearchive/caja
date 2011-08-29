/// Copyright (c) 2010 Vrije Universiteit Brussel. 
/// 
/// Redistribution and use in source and binary forms, with or without modification, are permitted provided
/// that the following conditions are met: 
///    * Redistributions of source code must retain the above copyright notice, this list of conditions and
///      the following disclaimer. 
///    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
///      the following disclaimer in the documentation and/or other materials provided with the distribution.  
///    * Neither the name of Vrije Universiteit Brussel nor the names of its contributors may be used to
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
  id: "delegatedAccessor",
  path: "TestCases/delegatedAccessor.js",

  description: 'Test accessor properties on a proxy on the prototype chain',

  test: function testcase() {
    
    var delegator;
    
    var foo = 42;
    var target = {
      get foo()  { assert('this in get', this === delegator); return foo; },
      set foo(v) { assert('this in set', this === delegator); foo = v;    }
    };
    
    var proxy = Proxy.create({
      getPropertyDescriptor: function(name) {
        // print('gPD '+name);
        return Object.getOwnPropertyDescriptor(target, name);
      }
      /*
      , has: function(name) {
        print('has '+name);
        return name in target;
      }
      , get: function(name, proxy) {
        print('get '+name);
        return Object.getOwnPropertyDescriptor(target, name).get.call(receiver);
      }
      , set: function(name, val, proxy) {
        print('set '+name);
        Object.getOwnPropertyDescriptor(target, name).set.call(proxy, val);
      }*/
    });
    
    delegator = Object.create(proxy);
    
    // observed behavior:
    // first, the has('foo') trap is called, falling back on getPropertyDescriptor('foo')
    // when not defined. Next, if proxy implements 'get', calls the 'get' trap, otherwise
    // calls getPropertyDescriptor and calls the getter with |this| bound to delegator
    assert('get return', delegator.foo === 42);
    
    
    // observed behavior:
    // first, the has('foo') trap is called, falling back on getPropertyDescriptor('foo')
    // when not defined. Next, regardless of whether proxy implements 'set', always calls
    // getPropertyDescriptor and calls the setter with |this| bound to delegator
    delegator.foo = 24;
    
    assert('after set', delegator.foo === 24);
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});
