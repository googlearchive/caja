/// Copyright (c) 2010 Vrije Universiteit Brussel 
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
 
// Disallowing recursive fixing: what should be the semantics of calling fix() on a proxy handler while the same proxy is already being fixed (an earlier call to fix() is already on the stack)? Proposed solution is to throw a type error when calling fix() recursively.
// Source: http://wiki.ecmascript.org/doku.php?id=harmony:proxies (rev 9/27/10)
ES5Harness.registerTest( {
  id: "recursive-fix",
  path: "TestCases/recursive-fix.js",
  
  description: 'test whether recursive fixing is disallowed',

  test: function testcase() {
    var proxy = Proxy.create({
      fix: function() {
        Object.preventExtensions(proxy); // triggers 'fix()' recursively
        return {};
      }
    });
    assertThrows('recursive fixing is disallowed', TypeError, function() {
      Object.preventExtensions(proxy); // triggers 'fix'
    });
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined" && !!Object.preventExtensions;
  }
});