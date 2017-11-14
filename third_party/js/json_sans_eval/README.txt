From http://code.google.com/p/json-sans-eval/

         ============================================
         A fast and secure JSON parser in JavaScript?
         ============================================

This JSON parser does not attempt to validate the JSON, so may
return a result given a syntactically invalid input, but does not
use eval so is deterministic and is guaranteed not to modify any
object other than its return value.

There are a number of JSON parsers in JavaScript? at
json.org. This implementation should be used whenever security is
a concern (when JSON may come from an untrusted source), speed is
a concern, and erroring on malformed JSON is not a concern.

	Pros 	Cons
This implementation 	Fast, secure 	Not validating
json_parse.js 	Validating, secure 	Slow
json2.js 	Fast, some validation 	Potentially insecure

json2.js is very fast, but potentially insecure since it calls
eval to parse JSON data, so an attacker might be able to supply
strange JS that looks like JSON, but that executes arbitrary
javascript.

If you do have to use json2.js with untrusted data, make sure you
keep your version of json2.js up to date so that you get patches
as they're released.

To use, download
http://json-sans-eval.googlecode.com/svn/trunk/src/json_sans_eval.js
and include it in your webpage. Then you can call the global
jsonParse function to parse JSON. That function takes a string
argument which must be valid JSON as defined in RFC 4627.
