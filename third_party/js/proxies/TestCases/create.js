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
  id: "create",
  path: "TestCases/create.js",
  
  description: 'test Proxy.create',

  test: function testcase() {
    var p;
    
    assertThrows('handler must be an object, given number', TypeError, function() {
      Proxy.create(1, null);
    });
    assertThrows('handler must be an object, given string', TypeError, function() {
      Proxy.create("s", null);
    });
    assertThrows('handler must be an object, given null', TypeError, function() {
      Proxy.create(null, null);
    });
    assertThrows('handler must be an object, given undefined', TypeError, function() {
      Proxy.create(undefined, null);
    });
    assertThrows('handler must be an object, given boolean', TypeError, function() {
      Proxy.create(true, null);
    });
    
    assertThrows('prototype must be an object or null, given number', TypeError,
      function() { Proxy.create({}, 1); });
    assertThrows('prototype must be an object or null, given string', TypeError,
      function() { Proxy.create({}, "s"); });
    assertThrows('prototype must be an object or null, given boolean', TypeError,
      function() { Proxy.create({}, true); });
    
    p = Proxy.create({});
    assertEq('prototype defaults to null', null, Object.getPrototypeOf(p));
    
    p = Proxy.create({}, null);
    assertEq('null is allowed as a prototype', null, Object.getPrototypeOf(p));

    p = Proxy.create({}, undefined);
    assertEq('undefined is replaced by a null prototype', null, Object.getPrototypeOf(p));
    
    var fun = function(){};
    fun.has = function(name) { return name === "test"; };
    p = Proxy.create(fun);
    assertEq('function is a legal handler', true, "test" in p);
    
    var regexp = /x/;
    regexp.has = function(name) { return name === "test"; };
    p = Proxy.create(regexp);
    assertEq('regexp is a legal handler', true, "test" in p);
    
    var proto = {};
    p = Proxy.create({}, proto);
    assertEq('second argument sets the prototype', proto, Object.getPrototypeOf(p));
    assertEq('[[Class]] of an object proxy', "[object Object]",
              Object.prototype.toString.call(p));
    assertEq('proxies are born extensible', true, Object.isExtensible(p));
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});