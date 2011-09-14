/// Copyright (c) 2010 Mozilla Foundation 
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


// If a proxy handler would return a property descriptor from
// get{Own}PropertyDescriptor whose configurable attribute is false,
// the proxy implementation throws an exception.
// The rationale is that the implementation cannot verify that the property is
// indeed non-configurable: a proxy could deceive a program by returning a
// non-configurable property descriptor but nothing would constrain it to still
// configure the property afterward. Hence, proxies must always reveal their
// properties as being configurable. Instead of having the proxy implementation
// throw an exception when it sees a non-configurable property descriptor,
// it could also silently modify the descriptor to be configurable before returning it,
// but that would be likely to mask errors.
// Source: http://wiki.ecmascript.org/doku.php?id=harmony:proxies (rev 2/25/10)
ES5Harness.registerTest( {
  id: "nonconfigurable",
  path: "TestCases/nonconfigurable.js",

  description: 'Proxies cannot return non-configurable property descriptors',

  test: function testcase() {

    var proxy = Proxy.create({
      getOwnPropertyDescriptor: function(name) {
        return {
          value: 'test',
          configurable: false // should cause an error
        };
      },
      getPropertyDescriptor: function(name) {
        return {
          value: 'test',
          configurable: false // should cause an error
        };
      }
    });

    assertThrows('getOwnPropertyDescriptor throws', TypeError, function() {
      var pd = Object.getOwnPropertyDescriptor(proxy, 'foo'); // this call should fail
    });
    
    // FIXME: can't test as long as getPropertyDescriptor is not implemented
    //assertThrows('getPropertyDescriptor throws', TypeError, function() {
    //  Object.getPropertyDescriptor(proxy, 'foo'); // this call should fail      
    //});
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined" && !!Object.getOwnPropertyDescriptor;
  }
});
