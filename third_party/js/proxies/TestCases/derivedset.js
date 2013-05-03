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

// A forwarding handler that only implements the fundamental traps
// based on http://wiki.ecmascript.org/doku.php?id=harmony:proxy_defaulthandler
function FundamentalHandler(target) {
  this.target = target;
}
FundamentalHandler.prototype = {
  // Object.getOwnPropertyDescriptor(proxy, name) -> pd | undefined
  getOwnPropertyDescriptor: function(name) {
    var desc = Object.getOwnPropertyDescriptor(this.target, name);
    if (desc !== undefined) { desc.configurable = true; }
    return desc;
  },
  // Object.getPropertyDescriptor(proxy, name) -> pd | undefined
  getPropertyDescriptor: function(name) {
    // Note: this function does not exist in ES5
    // var desc = Object.getPropertyDescriptor(this.target, name);
    // fall back on manual prototype-chain-walk:
    var desc = Object.getOwnPropertyDescriptor(this.target, name);
    var parent = Object.getPrototypeOf(this.target);
    while (desc === undefined && parent !== null) {
      desc = Object.getOwnPropertyDescriptor(parent, name);
      parent = Object.getPrototypeOf(parent);
    }
    if (desc !== undefined) { desc.configurable = true; }
    return desc;
  },
  // Object.getOwnPropertyNames(proxy) -> [ string ]
  getOwnPropertyNames: function() {
    return Object.getOwnPropertyNames(this.target);
  },
  // Object.getPropertyNames(proxy) -> [ string ]
  getPropertyNames: function() {
    // Note: this function does not exist in ES5
    // return Object.getPropertyNames(this.target);
    // fall back on manual prototype-chain-walk:
    var props = Object.getOwnPropertyNames(this.target);
    var parent = Object.getPrototypeOf(this.target);
    while (parent !== null) {
      props = props.concat(Object.getOwnPropertyNames(parent));
      parent = Object.getPrototypeOf(parent);
    }
    // FIXME: remove duplicates from props
    return props;
  },
  // Object.defineProperty(proxy, name, pd) -> undefined
  defineProperty: function(name, desc) {
    return Object.defineProperty(this.target, name, desc);
  },
  // delete proxy[name] -> boolean
  'delete': function(name) { return delete this.target[name]; },
  fix: function() {
    return undefined;
  },
};

ES5Harness.registerTest( {
  id: "derivedset",
  path: "TestCases/derivedset.js",

  description: 'test the derived set trap implementation',

  test: function testcase() {
    var a = 2;
    var target = {
      dataProp: 1,
      get accessorProp() { return a; },
      set accessorProp(v) { a = v; },
      get readonlyaccessor() { return 3; }
    };
    Object.defineProperty(target, 'readonlyDataProp', {value:4,writable:false});
    
    var handler = new FundamentalHandler(target);
    var proxy = Proxy.create(handler, Object.getPrototypeOf(target));
    
    assert("proxy.dataProp (before)", proxy.dataProp === 1);
    proxy.dataProp = 2;
    assert("proxy.dataProp (after =2)", proxy.dataProp === 2);
    
    assert("proxy.readonlyDataProp (before)", proxy.readonlyDataProp === 4);
    try {
      proxy.readonlyDataProp = 5;
    } catch (e) {
      assert("proxy.readonlyDataProp (try)", e instanceof TypeError);
    }
    assert("proxy.readonlyDataProp (after =5)", proxy.readonlyDataProp === 4);
    
    assert("proxy.readonlyaccessor (before)", proxy.readonlyaccessor === 3);
    try {
      proxy.readonlyaccessor = 4;
    } catch (e) {
      assert("proxy.readonlyaccessor (try)", e instanceof TypeError);
    }
    assert("proxy.readonlyaccessor (after =4)", proxy.readonlyaccessor === 3);
    
    assert("proxy.accessorProp (before)", proxy.accessorProp === 2);
    proxy.accessorProp = 3;
    assert("proxy.accessorProp (after =3)", proxy.accessorProp === 3);
    
    return true;
  },

  precondition: function precond() {
    return typeof Proxy !== "undefined";
  }
});