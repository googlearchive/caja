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
  id: "enum-add",
  path: "TestCases/enum-add.js",

  description: 'add a proxy property while enumerating its properties',

  test: function testcase() {
   
    var target = {a:0,b:1,c:2};
    var proxy = Proxy.create({
      has: function(name) {
        return target.hasOwnProperty(name);
      },
      get: function(rcvr, name) {
        return target[name];
      },
      set: function(rcvr, name, val) {
        target[name] = val;
        return true;
      },
      enumerate: function() {
        return ['a','b','c'];
      }
    });

    var results = [];
    for (var prop in proxy) {
      results.push(prop);
      if (prop === 'b') {
        proxy['d'] = 3; // add a new property in the middle of an enumeration
      }
    }
    
    return sameStructure(['a','b','c'], results);
    
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});