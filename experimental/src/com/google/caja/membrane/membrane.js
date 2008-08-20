/*
 * This membrane code is meant to wrap API objects/functions. If an object is
 * passed in, a shadow object and control gate will be returned. The shadow
 * object can be passed to cajoled code as if it were the original, except that 
 * the shadow will self-enforce the appropriate taming decisions. 
 * 
 * The shadow object is stateless on its own. When the shadow object's non-
 * existant properties or functions are accessed, an error will occur and control 
 * will be passed to the wrapper's read/set/call handlers.  The read/set/call 
 * handlers will check that the control gate is enabled & that the API policy
 * allows access to the specified property/function.  Wrapped objects or data 
 * values will then be returned if the premissions are appropriate.
 *
 * An exception for how this works is made for array-like objects, due to a 
 * Cajita optimization that gives cajoled code direct access to arrays without
 * using ___.readPub().  Array-like objects therefore have state and are wrapped
 * copies of their originals.
 *
 * All of the shadow's children and methods will be recursively wrapped, as will
 * their return values.
 *
 * @author: Adrienne Felt (adriennefelt@gmail.com)
 */

var PERM_DENIED = false;

// Checks to see if an obj is an array by checking to see if it has
// a non-enumerable numeric length property
function arrayLike(obj) {
  if (typeof obj !== 'object') { return false; }
  if (obj === null) { return false; }
  if (!('length' in obj)) { return false; }
  if (Object.prototype.propertyIsEnumerable('length')) { return false; }
  if (typeof obj.length !== 'number') { return false; }
  return obj.length >= 0;
}

// Creates a membrane for a target object; the membrane is propagated 
// recursively to all of the object's properties, return values, and
// incoming arguments.
function makeMembrane(target) {

  // "caretaker" is what is actually returned
  var caretaker = { };  
  // "shadow" is the placeholder obj that accesses to the target go through
  caretaker.shadow;
  
  // "gate" turns on/off access to the target object; all properties etc
  // are tied to the same gate.  this makes obj access revocable
  caretaker.gate = {
    enabled: true,
    enable: function () { caretaker.gate.enabled = true; },
    disable: function () { caretaker.gate.enabled = false; }
  };
  
  // Recursively wraps functions/objects until data is found
  function recWrap(arg, around, pname) {
    if (arg == null) { return null; }
    var tmp = typeof(arg);
    switch (tmp) {
      
    // Simply return data values
    case "number": return arg;
    case "string": return arg;
    case "boolean": return arg;
    case "undefined": return arg;
      
    // Objects are wrapped & replaced with their own shadows
    case "object": return makeShadow(arg);
      
    // Function argments and return values need to be wrapped too
    case "function":
      return function () {
				if (caretaker.gate.enabled && checkCall(around,pname,arguments)) {
					var wrappedArgs = new Array();
					for (var i = 0; i < arguments.length; i++) {
						if (arguments[i].wrapped___) {
							// If they are both wrapped, give access to the original
							wrappedArgs[i] = arguments[i].unwrap___();
						} else {
							// any "this" in an argument normally would point to global scope;
							// we don't want to allow that, so it goes to caja.USELESS
							wrappedArgs[i] = recWrap(arguments[i], caja.USELESS);
						}
					}
					// the function itself needs to be called on the shadow object (around)
					var ret_result = arg.apply(around,wrappedArgs);
					// the return value points to caja.USELESS in case it returns "this"
					return recWrap(ret_result, caja.USELESS);
				}
      };
    }
  }
  
  // Associates an object with a new shadow
  function makeShadow(obj) {

    var shadow;

    // Arrays are snapshotted and treated as if they are fixed-length
    // to handle the optimization in which numerically indexed arrays are
    // looked up regularly without Caja.js's interposition
    if (arrayLike(obj)) {
      shadow = [];
      for (var i = 0; i < obj.length; i++) {
        shadow[i] = recWrap(obj[i], caja.USELESS);
      }
    } else { 
      shadow = { };
    }
    
		// Since "shadow" lacks a correct prop list, this supports for-in loops.
    // Do NOT try to use the direct prop list off of shadow.
    obj.getKeys___ = function () {
      var result = [];
      for (var k in obj) {
        if (caja.canInnocentEnum(obj, k)) {
          result.push(k);
        }
      }
      return recWrap(result,obj);
    };
	
    shadow.handleRead___ = function (p) {
      if (caretaker.gate.enabled && checkRead(obj,p)) {
        return recWrap(obj[p], obj);
      } else {
        return PERM_DENIED;
      }
    }
    
    shadow.handleSet___ = function (p, rhs) {
      if (caretaker.gate.enabled && checkSet(obj,p,rhs)) {
        obj[p] = recWrap(rhs, shadow);
				if (arrayLike(obj)) {
					shadow[p] = obj[p];
				}
      } else {
        return PERM_DENIED;
      }
    }

    shadow.handleCall___ = function (p, args) {			
			// We don't want getKeys___ to be subject to the API check
			// The return array is wrapped inside getKeys___
			if (obj[p] === obj.getKeys___ && caretaker.gate.enabled) {
				return obj.getKeys___();
				
			// This check here is not the actual deciding factor on access, that
			// happens inside the wrapped function itself
			} else if (caretaker.gate.enabled) {
				var m = recWrap(obj[p], obj, p);
				return m.apply(shadow, args);
			} else {
				return PERM_DENIED;
			}
    };
		
		shadow.wrapped___ = true;
    shadow.unwrap___ = function () {
      return obj;
    };
   
    return shadow;
  }
  
  // Since makeMembrane is called as a function, its <tt>this</tt>
  // binds to the global <tt>this</tt>, which is bad -- so caja.USELESS
  caretaker.shadow = recWrap(target, caja.USELESS);
  
  return caretaker;
}

/* Checks the policy of the class associated with the object for READING.
 * It's assumed that there is a variable named tame_%ClassName% in JSON format
 * with "properties" and "functions" lists.  Each property/function in the list
 * has a "perm" field that is either "allow" or "deny".  If the property/
 * function is not found, then read access is assumed to be denied.
 * For reference, see the model Test class in taming/MyAPI.js.
*/
function checkRead(obj, prop) {
  var class = "tame_" + getClass(obj) + "___";
  var perms = eval("(" + class + ")");		// TODO: get rid of this eval!
  
  if (!perms) {
		caja.fail(class," does not have a defined API policy.");
  }
  
  if (perms.properties[prop] && perms.properties[prop].perm === "allow") {
		return true;
  } else if (perms.functions[prop] && perms.functions[prop].perm === "allow") {
		return true;
  } else if (perms.properties[prop]) {
		caja.fail("Read of ",class,".",prop," fails: ",perms.properties[prop].comment);
  } else if (perms.functions[prop]) {
		caja.fail("Read of ",class,".",prop," fails: ",perms.functions[prop].comment);
  } else {
		caja.fail("Read of ",class,".",prop," fails: ","not included in API.");
  }
  return false;
}

/* Checks the policy of the class associated with the object for CALLING.
 * Properties may never be called.  Functions are callable if their "perm"
 * field is set to "allow".  Arguments may have an associated array of values
 * or a filtering function.  Example:
 *	"giveNumber": {
 *			"perm": "allow",
 *			"arg1_arr": ["1","2","3"],
 *			"arg2_filter": "filterxyz",
 *			"arg3_arr": ["1","2","3"]
 *  }
 * The array labels need to end in "_arr" and the function labels need to
 * endin "_filter".  Filter arguments should be the name of the function.
 * Only one (array or function label) should be provided.  If neither an
 * array nor a filter is provided, it's assumed that all values are OK.
 * For reference, see the model Test class in taming/MyAPI.js.
*/
function checkCall(obj, prop, args) {
  var class = "tame_" + getClass(obj) + "___";
  var perms = eval("(" + class + ")");		// TODO: get rid of this eval!
  
  if (!perms) {
		caja.fail(class," does not have a defined API policy.");
  }
  
  if (perms.functions[prop] && perms.functions[prop].perm === "allow") {
		var counter = 0;
		for (var i in perms.functions[prop]) {
			if (caja.canInnocentEnum(perms.functions[prop],i)) {
				if (i !== "perm" && i !== "comment") {

					// Make sure there are the same number of argument rules as arguments
					if (!args[counter]) {
						caja.fail("Calling ",obj,".",prop," failed: Permissions mismatch"+
								" on arg ",i);
						return false;
					}
					
					// For when the argument has an array filter
					if (i.match(/_arr$/)) {
						// The arg needs to be either a number or string for the comparison
						var rhs = args[counter];
						if (typeof rhs === "number") { rhs = rhs.toString(); }
						if (typeof rhs !== "string") { 
							caja.fail("Calling ",class,".",prop," fails: arg ",rhs.toString(),
									" is not a valid value.");
							return false;
						}
						// Check the string against the policy array
						var flipswitch = false;
						for (var k in perms.functions[prop][i]) {
							if (rhs === perms.functions[prop][i][k]) { 
								flipswitch = true;
							}
						}
						if (flipswitch !== true) {
							caja.fail("Calling ",class,".",prop," fails: arg ",
									args[counter].toString()," is not a valid value.");
							return false;
						}
					} else if (i.match(/_filter$/)) {
						var filterfunc = eval("("+perms.functions[prop][i]+")");
						var testfunc = filterfunc(args[counter]);
						if (testfunc === false) {
							caja.fail("Calling ",class,".",prop," fails: arg ",rhs.toString(),
									" is not a valid value.");
							return false;
						}
					}
					counter++;
				}
			}
		}
		// If it got to here, it must have not failed along the way
		return true;
  } else if (perms.functions[prop]) {
		caja.fail("Calling ",class,".",prop," is denied: ",
				perms.functions[prop].comment);
  } else if (perms.properties[prop]) {
		caja.fail("Setting ",class,".",prop," fails: API properties are not"+
				" callable.");
  } else {
		caja.fail("Setting ",class,".",prop," fails: not included in API.");
  }
  return false;
}

/* Checks the policy of the class associated with the object for SETTING.
 * Functions may never be set.  Properties may be set; each property in the
 * "properties" list has a "rw" field that is either "write" or "read-only."
 * If setting is allowed, we check "RHS_arr" and "RHS_func" for allowed
 * values.  "RHS_arr" is given precedence and is assumed to be a string array.
 * "RHS_func" contains the name of a filtering function that should be defined 
 * elsewhere.  If neither RHS_arr or RHS_func are set, all values are permitted.
 * For reference, see the model Test class in taming/MyAPI.js.
*/
function checkSet(obj, prop, rhs) {
  var class = "tame_" + getClass(obj) + "___";
  var perms = eval("(" + class + ")");		// TODO: get rid of this eval!
  
  if (!perms) {
		caja.fail(class," does not have a defined API policy.");
  }
  
  if (perms.properties[prop] && perms.properties[prop].rw === "write") {
		if (perms.properties[prop].RHS_arr) {
			// The RHS must be a number or string for the comparison to work
			if (typeof rhs === "number") { rhs = rhs.toString(); }
			if (typeof rhs !== "string") { 
				caja.fail("Setting of ",class,".",prop," fails: RHS ",rhs.toString(),
						" is not a valid value.");
				return false;
			}
			for (var i in perms.properties[prop].RHS_arr) {
				if (rhs === perms.properties[prop].RHS_arr[i]) { return true; }
			}
			caja.fail("Setting of ",class,".",prop," fails: RHS ",rhs.toString()," is not a valid value.");
			return false;
		} else if (perms.properties[prop].RHS_func) {
			var filterfunc = eval("("+perms.functions[prop][i]+")");
			var testfunc = filterfunc(args[counter]);
			if (testfunc === false) {
				caja.fail("Calling ",class,".",prop," fails: arg ",rhs.toString(),
						" is not a valid value.");
			return false;
		} 
		return true;
  } else if (perms.properties[prop]) {
		caja.fail("Setting of ",class,".",prop," is read-only: ",perms.properties[prop].comment);
  } else if (perms.functions[prop]) {
		caja.fail("Setting ",class,".",prop," fails: API functions are not settable.");
  } else {
		caja.fail("Setting ",class,".",prop," fails: not included in API.");
  }
  return false;
}

// Gets the class of an object.
function getClass(obj) {  
  if (obj && obj.constructor && obj.constructor.toString) {  
    var arr = obj.constructor.toString().match(/function\s*(\w+)/);  
    if (arr && arr.length === 2) {  
      return arr[1];  
    }  
  }  
  return undefined;  
}