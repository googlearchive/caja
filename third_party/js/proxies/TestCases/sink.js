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
  id: "sink",
  path: "TestCases/sink.js",

  description: 'test sink handler that absorbs all operations',

  test: function testcase() {
    
    var sink;
    var sinkHandler = {
    	getOwnPropertyDescriptor: function(name) { return undefined; },
    	getPropertyDescriptor: function(name) { return undefined; },
    	getOwnPropertyNames: function() { return []; },
    	defineProperty: function(name, desc) { return true; },
    	'delete': function(name) { return true; },
    	fix: function() { return undefined; }, // sink can't be fixed
     	has: function(name) { return false; },
    	hasOwn: function(name) { return false; },
    	get: function(receiver, name) { return sink; },
    	// Patched for Caja: always strict mode, and set isn't expected to throw
    	set: function(receiver, name, val) { return true; },
    	enumerate: function() { return []; },
    	keys: function() { return []; }
    };
    sink = Proxy.createFunction(sinkHandler, function() { return sink; });
    
    var result;
    
    result = Object.getOwnPropertyDescriptor(sink, 'a');
    assertEq('getOwnPropertyDescriptor sink', undefined, result);
    
    // getPropertyDescriptor
    // FIXME: can't test as long as Object.getPropertyDescriptor is missing  
    
    result = Object.getOwnPropertyNames(sink);
    assert('getOwnPropertyNames sink', sameStructure([], result));
    
    result = Object.defineProperty(sink, 'foo', {});
    assertEq('defineProperty sink', sink, result);
    
    result = delete sink.foo;
    assertEq('delete sink', true, result);
    
    result = [];
    for (var p in sink) { result.push(p); }
    assert('enumerate sink', sameStructure([], result));

    result = 'foo' in sink;
    assertEq('has sink', false, result);

    result = ({}).hasOwnProperty.call(sink, 'foo');
    assertEq('hasOwn sink', false, result);
  
    result = (sink.foo = 0);
    assertEq('set sink', 0, result);
    
    result = sink.foo;
    assertEq('get sink', sink, result);
  
    result = sink.foo(1,2,3);
    assertEq('invoke sink', sink, result);
    
    result = sink.foo(1,2,3).bar(4,5,6);
    assertEq('invoke sink chained', sink, result);
    
    result = Object.keys(sink);
    assert('keys sink', sameStructure([], result));
        
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});
