/// Copyright (c) 2011 Vrije Universiteit Brussel 
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
  id: "createFunction",
  path: "TestCases/createFunction.js",
  
  description: 'test Proxy.createFunction',

  test: function testcase() {
    var p;
    
    assertThrows('handler must be an object, given number', TypeError, function() {
      Proxy.createFunction(1, null);
    });
    assertThrows('handler must be an object, given string', TypeError, function() {
      Proxy.createFunction("s", null);
    });
    assertThrows('handler must be an object, given null', TypeError, function() {
      Proxy.createFunction(null, null);
    });
    assertThrows('handler must be an object, given undefined', TypeError, function() {
      Proxy.createFunction(undefined, null);
    });
    assertThrows('handler must be an object, given boolean', TypeError, function() {
      Proxy.createFunction(true, null);
    });
    
    assertThrows('call trap must be callable, given number', TypeError,
      function() { Proxy.createFunction({}, 1); });
    assertThrows('call trap must be callable, given object', TypeError,
      function() { Proxy.createFunction({}, {}); });
    
    assertThrows('construct trap must be callable, given number', TypeError,
      function() { Proxy.createFunction({}, function(){}, 1); });
    assertThrows('construct trap must be callable, given object', TypeError,
      function() { Proxy.createFunction({}, function(){}, {}); });
    
    // construct trap is optional
    p = Proxy.createFunction({}, function(){});
    p = Proxy.createFunction({}, function(){}, undefined);
    
    var calltrap = function() { return 42; };
    var obj = {};
    var constructtrap = function() { return obj; }
    p = Proxy.createFunction({}, calltrap, constructtrap);
    assertEq('second argument is the calltrap', 42, p());
    assertEq('third argument is the constructtrap', obj, new p());
    assertEq('[[Class]] of a function proxy', "[object Function]",
              Object.prototype.toString.call(p));
    assertEq('function proxies are born extensible', true, Object.isExtensible(p));
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});