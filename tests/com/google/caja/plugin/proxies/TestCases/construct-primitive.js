/// Copyright (c) 2010 Mozilla Foundation. 
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
  id: "construct-primitive",
  path: "TestCases/construct-primitive.js",

  description: 'Function proxies can return non-object values from their construct trap',

  test: function testcase() {
    
    var handler = {
      get: function(name, proxy) { return Function.prototype[name]; },
    };

    var fproxy = Proxy.createFunction(handler,
      function() { }, // call trap, not used here
      function() { return 42; });

    var result = new fproxy();
    assert("function proxy returns number",
           typeof result === "number" && result === 42);    
    
    fproxy = Proxy.createFunction(handler,
      function() { return 42; }); // call trap is the construct trap
    result = new fproxy();
    assert("function proxy with default construct trap returns number",
           typeof result === "number" && result === 42);

    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});
