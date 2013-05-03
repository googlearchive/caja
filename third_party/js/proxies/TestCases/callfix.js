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
  id: "callfix",
  path: "TestCases/callfix.js",

  description: 'proxies are fixed by means of Object.{freeze|seal|preventExtensions}',

  test: function testcase() {
    
    var handler = {
      get: function(rcvr, name) {
        if (name === 'isTrapping') return true;
      },
      fix: function() {
        return {
          isTrapping: { value: false }
        }
      }
    };
    
    assert('preventExtensions freezes object proxy',
           ! (Object.preventExtensions(Proxy.create(handler))).isTrapping);
    assert('seal freezes object proxy',
           ! (Object.seal(Proxy.create(handler))).isTrapping);
    assert('freeze freezes object proxy',
           ! (Object.freeze(Proxy.create(handler))).isTrapping);
    assert('preventExtensions freezes function proxy',
           ! (Object.preventExtensions(Proxy.createFunction(handler, function(){}))).isTrapping);
    assert('seal freezes function proxy',
           ! (Object.seal(Proxy.createFunction(handler, function(){}))).isTrapping);
    assert('freeze freezes function proxy',
           ! (Object.freeze(Proxy.createFunction(handler, function(){}))).isTrapping);

    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined" &&
           !!Object.freeze &&
           !!Object.seal &&
           !!Object.preventExtensions;
  }
});
