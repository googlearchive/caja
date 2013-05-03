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
 
// Object.defineProperty(proxy, 'foo', desc) should
//  1) turn 'desc' into an internal property descriptor 'pd' and sanitize standard Property Descriptor attributes
//  2) turn 'pd' into a real ECMAScript object 'descObj'
//  3) copy all non-standard own attributes from 'desc' to 'descObj'
//  4) pass 'descObj' as the 2nd argument to the 'defineProperty' trap
// Source: http://wiki.ecmascript.org/doku.php?id=harmony:proxies_semantics#modifications_to_other_object_built-ins
// (see semantics for Object.defineProperty, rev. 10/02/2010)
ES5Harness.registerTest( {
  id: "defineProperty-attributes",
  path: "TestCases/defineProperty-attributes.js",
  
  description: 'test whether defineProperty has access to non-standard attributes',

  test: function testcase() {
    var result;
    
    var proxy = Proxy.create({
      defineProperty: function(name, descObj) {
        result = (descObj.non_standard === 1);
        return true;
      }
    });
    
    var desc = { value: 42, writable: true, non_standard: 1 };
    Object.defineProperty(proxy, 'foo', desc);
    
    return result;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined" && !!Object.defineProperty;
  }
});