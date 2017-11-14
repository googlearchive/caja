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
 
// - The handler may delegate to other objects and its delegation chain
//   is completely independent from that of the proxy it handles.
// - Invoking a trap explicitly on a proxy will not trigger
//   the handler’s corresponding traps. Instead, the call will be reified like any other,
//   Traps can only be invoked explicitly on a proxy’s handler, not on the proxy itself.
//
// Source: http://wiki.ecmascript.org/doku.php?id=harmony:proxies (rev 2/25/10)
ES5Harness.registerTest( {
  id: "stratification",
  path: "TestCases/stratification.js",

  description: 'handler and proxy are stratified',

  test: function testcase() {
    
    function HandlerProto() { };
    HandlerProto.prototype = {
      get: function(rcvr, name) {
        assert('get trap called for has', name === 'has');
        return function(var_args) {
          assertEq('result of get trap called with 1 arg', 1, arguments.length);
          return arguments[0];
        };
      },
      has: function(name) { return (name === 'foo'); }
    };
    
    var handler = new HandlerProto();
    
    var proxy = Proxy.create(handler, Object.prototype);
    
    assert('prototype of handler is HandlerProto.prototype',
           HandlerProto.prototype === Object.getPrototypeOf(handler));
    assert('prototype of proxy is Object.prototype',
           Object.prototype === Object.getPrototypeOf(proxy));    
    
    // proxy.has('foo') !== handler.has('foo') but handler.get(proxy,'has')('foo')
    assertEq('proxy.has(foo)', 'foo', proxy.has('foo'));
    assertEq('handler.has(foo)', true, handler.has('foo'));
    assertEq('"foo" in proxy', true, 'foo' in proxy);
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});
