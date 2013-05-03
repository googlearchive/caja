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
 
/**
 * Helper function to test double lifting (using proxies as handlers)
 * @param expectedOp the name of the trap expected to be invoked on the proxy
 * @param expectedArgs the arguments expected to be passed to the invoked trap
 * @param legalReturnvalueForOp a legal return value for the invoked trap, that will be
 *        returned by this generic proxy to answer the request
 * @param optArgChecker an optional function to be used for checking the arguments
 *        (when checking arguments using === is not sufficient).
 */
function genericProxyExpecting(expectedOp, expectedArgs, legalReturnvalueForOp,
                               optArgChecker) {
  var argChecker = optArgChecker ||
                     function(actArgs) { return sameStructure(expectedArgs, actArgs); };
  // note: handlerProxy is both a handler for other proxies and a proxy itself
  var handlerProxy = Proxy.create({
    get: function(rcvr, name) {
      assertEq(expectedOp, expectedOp, name);
      return function() {
        var args = Array.prototype.slice.call(arguments);
        assert(expectedOp+' args', argChecker(args));
        return legalReturnvalueForOp; 
      };
    }
  });

  return Proxy.create(handlerProxy); 
}