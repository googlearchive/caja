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
  id: "propertyNames",
  path: "TestCases/propertynames.js",

  description: 'property names are always coerced to string in trap handlers',

  test: function testcase() {
    
    var proxy = Proxy.create({
      has: function(propName) {
        assert(propName+' is a string', typeof propName === "string");
        return true;
      },
      get: function(propName, proxy) {
        assert(propName+' is a string', typeof propName === "string");
        return propName;
      }
    });
    
    assertEq('.foo', 'foo', proxy.foo);
    // Gotcha: can't intercept numeric indices
    /*
    assertEq('[15]', '15', proxy[15]);
    assertEq("['15']", '15', proxy['15']);
    assertEq("['1'+'5']", '15', proxy['1'+'5']);
    assertEq("[15.0]", '15', proxy[15.0]);
    assertEq("['15.0']", '15.0', proxy['15.0']);
    assertEq("[15.2]", '15.2', proxy[15.2]);
    assertEq('[0]', '0', proxy[0]);
    assertEq('[2<<17]', ''+(2<<17), proxy[2<<17]);
    */
    assert('foo', 'foo' in proxy);
    assert('15', 15 in proxy);
    assert("'15'", '15' in proxy);
    assert("'1'+'5'", ('1'+'5') in proxy);
    assert("15.0", 15.0 in proxy);
    assert("'15.0'", '15.0' in proxy);
    assert("15.2", 15.2 in proxy);
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});
