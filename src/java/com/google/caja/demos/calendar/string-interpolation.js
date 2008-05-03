/**
 * @fileoverview
 *
 * Allow Perl style string interpolation
 *
 *   var sql = open(Template("SELECT * FROM $table WHERE foo=$foo"));
 *
 * But in a way that allows a sql library to interpret $table and $foo in
 * context, and apply the proper escaping scheme.
 *
 * This file defines the syntactic sugar that enables the open(Template(...))
 * syntax above.  See section SYNTACTIC SUGAR below.
 *
 * This file also introduces a StringInterpolation class that libraries can
 * use to implement their own escaping schemes.
 *
 * See string-interpolation-examples.js for examples of how libraries can
 * implement their own interpolation schemes and string-interpolation-test.html
 * for an interactive demo.
 *
 * @author mikesamuel@gmail.com
 */


// PUBLIC API
// ==========
// First let's define a string interpolation as an object that contains
// interpolated parts.  Some of these parts are literal strings, and some
// are substitutions.  Our constructor will take an array, and will treat
// even parts as literal strings, and odd parts as substitutions.
/**
 * A string like object that encapsulates a string interpolation's template and
 * substitutions, and provides a mechanism by which substitutions can be
 * re-escaped using information available at the strings final output point.
 *
 * @param {Array} args an array whose even elements are literal strings, and
 *   whose odd parts are substitutions.
 * @constructor
 */
function StringInterpolation(args) {
  this.args = args;
}
// Next, let's make it string-like.  If used in a string context, it should
// coerce to a string for debugging.
StringInterpolation.prototype.toString = function () {
  return this.args.join('');
};
StringInterpolation.prototype.valueOf = function (type) {
  if (type === 'string') { return this.toString(); }
  if (type === 'boolean') { return this.toString() !== ''; }
  return this;
};
StringInterpolation.prototype.append = function (interp) {
  var args0 = this.args;
  var args1 = interp.args;

  var m = args0.length;
  var n = args1.length;

  // Make sure that even arguments are literals.
  if (m & 1) {
    args0[m - 1] += args1[0];
  } else {
    args0[m++] = args1[0];
  }

  var last = m - 1;
  for (var i = n; --i > 0;) {
    args0[last + i] = args1[i];
  }
};
// Next, let's allow a SQL library, an HTML library, or any other library that
// needs simple string templating to hook in and implement its own secure
// interpolation scheme.
// This function walks over the arguments, and feeds the literalStrings to the
// contextScanner, so it can keep track of what kind of token is expected next.
// The substitutions are fed to the escaper, along with the context.
/**
 * Perform custom interpolation.
 * @param {Function} contextScanner takes (string, context, outputBuffer) and
 *   returns an updated context.  The type of context is up to the caller.
 * @param {Function} escaper takes (substitution, context, outputBuffer),
 *   appends an appropriate string-form of substitution onto outputBuffer,
 *   and returns the context.
 *   Substitution may be of any type.  Zero or more strings may be pushed onto
 *   outputBuffer which will be joined on the empty string to produce the
 *   interpolated string.
 * @param {Array.<string>} outputBuffer the buffer that output is written to.
 *   An Array that can be joined on the empty string to produce the
 *   interpolated string.
 * @return the context.  This is returned so that the caller can examine it and
 *   rror out if it is not a valid ending state.
 *   E.g., an XML templater could throw an exception if tags weren't closed.
 */
StringInterpolation.prototype.interpolate = function (
    contextScanner, escaper, initialContext, outputBuffer) {
  var context = initialContext;
  for (var i = 0, n = this.args.length; i < n; ++i) {
    var arg = this.args[i];
    if (i & 1) {  // args is a substitution
      if (arg instanceof StringInterpolation) {
        // Recurse.  This allows, e.g., production of a WHERE clause as one
        // interpolation which can then be substituted into a SELECT stmt.
        context = arg.interpolate(
            contextScanner, escaper, context, outputBuffer);
      } else {
        context = escaper(arg, context, outputBuffer);
      }
    } else {
      outputBuffer.push(arg);
      context = contextScanner(arg, context, outputBuffer);
    }
  }
  return context;
};

// And to make sure that users NEVER EVER NEED TO WORK AROUND THE SYSTEM, allow
// for easily audited exemptions.
/**
 * A string interpolation that always outputs the same content.  This allows
 * exemptions to the normal escaping scheme, so should be used sparingly.
 * To audit all uses, run
 * <p>
 * $ egrep -nH '\bRawStringSubstitution\b' &lt;your source files&gt;
 *
 * @param {string} s raw text that should not be escaped.
 * @constructor
 */
function RawStringSubstitution(s) {
  this.s = String(s);
}
// Subclass StringInterpolation so that StringInterpolation.interpolate will
// delegate to this class when its used as a substitution value.
RawStringSubstitution.prototype = new StringInterpolation([]);
RawStringSubstitution.prototype.constructor = RawStringSubstitution;
// Now override interpolate to not invoke the escaper.
RawStringSubstitution.prototype.interpolate = function (
    contextScanner, escaper, initialContext, outputBuffer) {
  outputBuffer.push(this.s);
  return initialContext;
};
RawStringSubstitution.prototype.toString = function () { return this.s; };

// And for performance, let's allow context scanners to cache their results.
/**
 * Given a context scanner that takes immutable contexts, and that consistently
 * returns the same context for any (literalString, context) pair, cache results
 * to improve string interpolation performance.
 *
 * @param {Function} contextScanner a context scanner as specified in the
 *     StringInterpolation.interpolate method's API.
 * @return {Function} a contextScanner that is equivalent to the input if the
 *     conditions in this method's description hold.
 */
function cachingContextScanner(contextScanner) {
  var cache = {};
  var size = 0;

  return function (literalString, context) {
    var cacheKey = literalString + '\0' + context;
    if (cacheKey in cache) {
      return cache[cacheKey];
    }
    if (++size === 100) { cache = {}; }
    return cache[cacheKey] = contextScanner(literalString, context);
  };
}
// END PUBLIC API






// SYNTACTIC SUGAR
// ===============
// Since javascript doesn't have string interpolation built-in, let users do
// string interpolation via
//    open(Template("SELECT COUNT(*) FROM $table WHERE foo=${foo+1}"))
// This rather odd syntax would not be needed if the language was designed to
// do string interpolation properly, and the point of this rant is how to do
// it properly, not how to bolt it onto an existing language.

// Warning: Voodoo ahead.
function Template(interpolation) {
  var parts = interpSplitStringIntoParts(interpolation);
  return makeStringInterpolationConstructor(parts);
}
/** @private */
function interpSplitStringIntoParts(interpolation) {
  var parts = [];  // even elements are literal strings.  odd are substitutions
  while (interpolation) {
    var m = interpolation.match(
        /^(?:(\$\w+)|(?:\$\{\{\{[^:]*:|\}\}\})|(\$\{[\s\S]*?\})|([^\$]+|\$))/);
    if (m[1]) {  // $foo
      interpAddExpression(parts, m[1].substring(1));
    } else if (m[2]) {  // ${x}
      interpAddExpression(parts, m[2].substring(2, m[2].length - 1));
    } else if (m[3]) {  // lone $ or a literal portion of the string
      interpAddStringPart(parts, m[3]);
    }
    interpolation = interpolation.substring(m[0].length);
  }

  if (!(parts.length & 1)) {
    parts.push('');
  }
  return parts;
}
/** @private */
function interpAddExpression(parts, javascriptSource) {
  if (!(parts.length & 1)) {
    parts.push('');
  }
  parts.push(javascriptSource);
}
/** @private */
function interpAddStringPart(parts, literalString) {
  if (parts.length & 1) {
    parts[parts.length - 1] += literalString;
  } else {
    parts.push(literalString);
  }
}
/** @private */
function makeStringInterpolationConstructor(parts) {
  // Make a fake String Interpolation constructor, so that interp (aka eval)
  // will evaluate it in the context of the local variables it appears with.
  var javascript = ['new StringInterpolation(['];
  for (var i = 0, n = parts.length; i < n; ++i) {
    if (i) { javascript.push(','); }
    if (i & 1) {
      javascript.push('(', parts[i], ')');
    } else {
      javascript.push(javascriptStringLiteral(parts[i]));
    }
  }
  javascript.push('])');
  return javascript.join('');
}
// END SYNTACTIC SUGAR





/**
 * Given a plain text string produce a javascript string literal.
 * param {string} s plain text
 * return {string} a javascript string literal such that
 *     (s === eval(javascriptStringLiteral(s))).
 * @private
 */
var javascriptStringLiteral;
if (this.uneval) {
  javascriptStringLiteral = this.uneval;
} else {
  /**
   * Maps characters to the typical javascript character escape sequence..
   * @private
   */
  var JS_ESC = {
      '\\': '\\\\',
      '\"': '\\\"',
      '\'': '\\\'',
      '\r': '\\r',
      '\n': '\\n',
      '\u2028': '\\u2028',
      '\u2029': '\\u2029'
      };

  javascriptStringLiteral = function javascriptStringLiteral(s) {
    // TODO: This implementation does not correctly handle [:Cf:] characters.
    return ('"' + s.replace(/[\\\"\r\n\u2028\u2029]/g,
                            function (ch) { return JS_ESC[ch]; })
            + '"');
  };
}
