// Copyright (C) 2007 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * wrap a constructor to hide all members except those listed in publicMethods.
 *
 * <p>
 * Example: <br>
 * To protect privileged methods, you can do the following <pre>
 * (function () {  // an anonymous function defines a closed scope
 *   // Create a token that is shared by privileged code.
 *   // <b>Never</b> return the token outside the privileged scope, and
 *   // <b>never</b> call eval inside the privileged scope.
 *   var token = {};
 *
 *   // Define a class that encapsulates the capability
 *   function File(name) {
 *     this.name = name;
 *   }
 *   File.prototype.getName = function () { return name; };
 *
 *   // Protect the class
 *   var constructorAndUnlocker = protect(File, token, ['getName']);
 *   var ProtectefFile = constructorAndUnlocker[0];
 *   var unlock = constructorAndUnlocker[1];
 *
 *   // Define operations on it
 *   /** pop up a dialog to let the user choose a file &#42;/
 *   function chooseFile() {
 *     ...
 *     return new ProtectedFile(token, filename);
 *   }
 *   function readFile(file) {
 *     // <b>Always</b> operate on the underlying class since untrusted code
 *     // could replace the getName method.
 *     file = unlock(file);
 *     ...
 *   }
 *
 *   // Export the operations so that untrusted code can access them.
 *   // <b>Never</b> export the underlying constructor because that would allow
 *   // spoofing of capabilities.
 *   this.chooseFile = chooseFile;
 *   this.readFile = readFile;
 * })();
 * </pre>
 *
 * <p>
 * Properties: <ul>
 * <li>overhead of 2 function calls when boundary crossed from unprivileged to
 *     privileged code.  No overhead once boundary crossed.
 * <li>2 function calls overhead at capability instantiation time
 * <li>capability issuer can control which methods are called.
 *     all fields hidden
 * <li>capability issuer can retain a token to get access to protected
 *     methods & fields at runtime
 * <li>capability receiver can add/replace methods and add fields as a
 *     normal javascript object
 * <li>instanceof will not work -- the capabilities class and its
 *     superclass are hidden
 * <li>works in pure javascript without source rewriting, though protecting
 *     the things the capability needs to guard will often require
 *     source rewriting.
 * <li>if a capability receiver has modify access to the prototype, he can
 *     make changes that will be seen by other receivers, though not changes
 *     that would be seen by the issuer in privileged mode.
 *     <br>Source rewriting can disallow prototype modification.
 * </ul>
 *
 * <p>Assumptions:
 * <ul>
 * <li>Untrusted code cannot replace the Object constructor.
 *     If it can then the following<br><code>
 *         var allObjects = [];<br>
 *         Object = function () { allObjects.push(this); }<br></code>
 *     would cause all objects, including tokens and underlying instances to
 *     be put on a global list.  Malicious code could then try tokens in turn
 *     to escalate privileges.  Variants of this approach would serve the same
 *     purpose without the obvious problems.
 *     <br>Any attempts to avoid this exploit by, e.g. executing untrusted code
 *     inside <code>with({Object: Object}) { ... }</code> can be defeated by
 *     doing <code>delete Object</code>.
 * <li>Untrusted code cannot modify prototypes for base classes such as Object.
 *     If it can then <code>Object.toString</code> could be modified to
 *     similarly gain access to tokens and underlying instances unless onerous
 *     precautions were taken.
 * <li>An untrusted function cannot access the caller of a function.
 *     <code>__caller__</code> has been removed from Firefox, but
 *     <code>Function::caller</code> is still a problem.
 *     It allows any untrusted function that is called by trusted code to get
 *     a reference to the trusted function.  It is possible to defeat this
 *     by having a dummy function that recurses to itself before calling the
 *     untrusted code, but that is inefficient, puts onerous requirements on
 *     the writer of trusted code, and will not work for older versions of
 *     some browsers that still support __caller__.
 * </ul>
 * All these assumptions can be enforced by source code rewriting of untrusted
 * code.  All global references can be rewritten to point to a
 * namespace, and any attempts to access a prototype can be disallowed while
 * still allowing untrusted code to subclass via a privileged
 * <code>subClass</code> method.
 *
 * <p>Caveats:
 * <ul>
 * <li><b>Never return the token</b> outside the privileged scope.  Doing so
 *     would allow untrusted code to run as if it were privileged since it
 *     would be able to access and modify the underlying class.
 * <li><b>Never call <code>eval</code></b> inside the privileged scope.  In the
 *     example above, the token is available to the privileged code since it
 *     is defined as a var in the same scope.  If one of those functions called
 *     <code>eval</code>, the evaled code would have access to the token and
 *     to the unprotected constructor.
 * <li><b>Privileged code must only operate on the underlying instance</b>,
 *     never on the safe instance.  Untrusted code can modify the safe
 *     instance, adding and replacing methods, adding fields, etc.
 *     In the worst case, untrusted code could replace a method with
 *     <code>eval</code> causing the problems described above.
 *     Untrusted code cannot access or modify the underlying implementation, so
 *     always operate on the underlying implementation as <code>readFile</code>
 *     does above.
 * <li><b>Primitive tokens require extra care</b>.
 *     If the token is a primitive, a string or number, then the scope
 *     in which it is defined should not be referencable, since functions can
 *     be printed out.  For example, if you define a main function
 *     <code>function main() { var token = 1234; ... }</code>
 *     you've skroobed yourself since the token can be extracted via
 *     <code>Number(String(main).match(/var token = (\d+)/)[1])</code>.
 *     The example above does not suffer this vulnerability because the
 *     defining scope is never reachable from the global namespace, and because
 *     the token is an object, so does not compare as equal to other objects.
 *     If there is not a compelling need, use a token of <code>{}</code>.
 * </ul>
 *
 * <p>Tested on Firefox 2.0, IE 6.0, Safari 2.0, Opera 9.10, and Rhino
 *
 * <p>TODO(mikesamuel): return an Object like { unlock: ..., constructor }
 * instead of an array so clients don't have to unpack.</p>
 *
 * @param {Function} constructor
 * @param {Array<String>} publicMethods names of methods to expose.
 * @return {Array} a pair containing the protecting constructor, and an unlock
 *   function that, given an instance from the protecting constructor will
 *   return the underlying instance.
 */
function protect(constructor, token, publicMethods) {
  // Create a temporary subclass of constructor so that we can pass arbitrary
  // numbers of parameters to constructor.  Function.apply doesn't work on
  // constructors, so this constructor takes the arguments array of its caller
  // and does the apply when calling to its supercalss.
  var tempConstructor = function (args) {
    constructor.apply(this, args);
  };
  tempConstructor.prototype = constructor.prototype;

  // A location shared by unlock, and the __underlying__ method.
  // To prevent replay attacks if someone replaces __underlying__, we use this
  // shared location to store the result from __underlying__.
  var unlocked = null;
  // Given the wrapped instance, returns the underlying instance.  This function
  // should never be exposed to untrusted code.
  var unlock = function (protectingInstance) {
    unlocked = null;
    protectingInstance.__underlying__();
    var protectedInstance = unlocked;
    unlocked = null;
    return protectedInstance;
  };

  // The protected constructor.
  var safeConstructor = function (tokenIn, var_args) {
    // Prevent capability spoofing via
    //   newCapability = new (oldCapability.constructor)(args);
    // We throw an exception since returning null or undefined causes this to be
    // used.
    if (tokenIn !== token) { throw 'bad token'; }
    // Create an instance of the protected class.
    var args = [];
    for (var i = arguments.length; --i >= 1;) {
      args[i - 1] = arguments[i];
    }
    var underlying = new tempConstructor(args);
    // Attach an instance method which returns the underlying instance to
    // privileged code.
    this.__underlying__ = function () { unlocked = underlying; };
  };
  // Returns a function that knows how to access a protected method.
  function access(methodName) {
    return function (var_args) {
      var self = unlock(this);
      return self[methodName].apply(self, arguments);
    };
  }
  // Provide access for each protected method.
  for (var i = publicMethods.length; --i >= 0;) {
    var k = publicMethods[i];
    safeConstructor.prototype[k] = access(k);
  }

  return [safeConstructor, unlock];
}
