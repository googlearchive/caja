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


// The type of a Proxy:
//
// For spec purposes:
// Type(anObjectProxy) === Object
// Type(aFunctionproxy) === Object
// anObjectProxy.[[Class]] === “Object”
// aFunctionProxy.[[Class]] === “Function”
// 
// For ES code:
// typeof anObjectProxy === “object”
// typeof aFunctionProxy === “function”
// anObjectProxy instanceof C iff anObjectProxy.[[Prototype]] equals or inherits from C.prototype
// aFunctionProxy instanceof Function
//
// Source: http://wiki.ecmascript.org/doku.php?id=harmony:proxies#the_type_of_a_proxy (rev 2/25/10)
ES5Harness.registerTest( {
  id: "proxytypes",
  path: "TestCases/proxytypes.js",

  description: 'test the types of object and function proxies',

  test: function testcase() {
    
    var objProxy = Proxy.create({}, Object.prototype);
    var funProxy = Proxy.createFunction({}, function() {});
    
    // typeof
    assertEq('typeof objProxy is "object"', 'object', typeof objProxy);
    assertEq('typeof funProxy is "function"', 'function', typeof funProxy);
    
    // instanceof
    assert('objProxy instanceof Object', objProxy instanceof Object);
    assert('funProxy instanceof Function', funProxy instanceof Function);

    // [[Class]]
    assertEq('[[Class]] of object proxy is "Object"',
             Object.prototype.toString.call({}),
             Object.prototype.toString.call(objProxy));
    
    assertEq('[[Class]] of function proxy is "Function"',
             Object.prototype.toString.call(function(){}),
             Object.prototype.toString.call(funProxy));
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});
