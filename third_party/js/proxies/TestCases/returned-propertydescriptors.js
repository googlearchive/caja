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
  id: "returned-propertydescriptors",
  path: "TestCases/returned-propertydescriptors.js",

  description: 'Tests the properties of property descriptors returned from traps',
  
  test: function testcase() {
    
    var proxy;
    var result;
    
    // empty property descriptor
    proxy = Proxy.create({
      getOwnPropertyDescriptor: function(name) {
        return { configurable: true };
      }
    });
    result = Object.getOwnPropertyDescriptor(proxy, 'foo');
    
    // TODO: should the resulting property descriptor be completed?
    // If so, in the case of an empty PD, should it be completed as a
    // data or as an accessor property?
    
    
    // incomplete property descriptor
    proxy = Proxy.create({
      getOwnPropertyDescriptor: function(name) {
        return { value: 42, configurable: true };
      }
    });
    result = Object.getOwnPropertyDescriptor(proxy, 'foo');
    assert('incomplete pd - value', result.value === 42);
    assert('incomplete pd - enumerable', result.enumerable === false);
    assert('incomplete pd - configurable', result.configurable === true);
    
        
    // illegal property descriptor
    proxy = Proxy.create({
      getOwnPropertyDescriptor: function(name) {
        return { value: 42, get: function() { return 24; } };
      }
    });
    
    // does the Proxy implementation throw or pass the invalid descriptor through?
    assertThrows('illegal property descriptor', TypeError, function () {
      Object.getOwnPropertyDescriptor(proxy, 'foo');      
    });
    
    // illegal non-configurable property descriptor
    proxy = Proxy.create({
      getOwnPropertyDescriptor: function(name) {
        return { configurable: false };
      }
    });
    
    // does the Proxy implementation throw or pass the invalid descriptor through?
    assertThrows('cannot return non-configurable property descriptor', TypeError,
      function () {
        Object.getOwnPropertyDescriptor(proxy, 'foo');      
      });
    
    // property descriptor with non-standard attributes
    proxy = Proxy.create({
      getOwnPropertyDescriptor: function(name) {
        return { value: 42, extra: true, configurable: true };
      }
    });
    result = Object.getOwnPropertyDescriptor(proxy, 'foo');
    
    // TODO: are implementations allowed to truncate property descriptors
    // (i.e. to remove non-standard attributes)?
    assert('non-standard pd attribute present', result.extra === true);
    
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});