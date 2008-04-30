// Open questions:
// - Characters in identifiers.
// - Should we allow Octal literals
// - Parser grammar.  Can we just use Crock's once the lexer is simplified?
//   We can filter out this and arguments, but not statement labels.

var cajita = (function () {

// Character sets.
var charSetUnicodeNewline = '\\x0C\\n\\r\\x85\\u2028\u2029';
var charSetCajitaBanned = (
    '\\u200C-\\u200F\\u202A-\\u202E\\u206A-\\u206F'
    + '\\uFDD0-\\uFDEF\\uFEFF\\uFFF0-\\uFFF8\\uFFFE\uFFFF');
  
//// Define Cajita tokens and parts of Canita tokens as regular
//// expression snippets that introduce no capturing groups.

// An escape character allowed in a string literal.
var charEscape = '\\\\(?:u[0-9a-fA-F]{4}|[brntf\\\\\"\'/])';
var escChars = {
  'b': '\b',   'r': '\r',   'n': '\n',
  't': '\t',   'f': '\f',   '\\': '\\',
  '\'': '\'',   '"': '"',   '/': '/'
};

// For inconsistently interpreted characters.
var programFilter = new RegExp(
    '(?:^|[^\\\\])(?:\\\\\\\\)*\\\\[' + charSetUnicodeNewline + ']|\0');

// Whitespace and comments.
var ignorable = (
    '(?:'
    + [
       '[ \\t\\x0b\\n\\r\\u00A0]+',  // Whitespace.
       '//[^' + charSetUnicodeNewline + ']*',  // Line comment.
       // Block comments disallow [:Cf:] that might appear between '*' and '/'.
       '/\\*(?:[^\\*]|\\*+[^' + charSetCajitaBanned + '\\*/])*\\*+/'
       ].join('|')
    + ')+');

// The set of characters that can start an identifier, and that are significant.
// Note: there are no extended unicode codepoints in either this or the
// identifier char set.
var identifierStart = (
    '[\\$A-Z_a-z\\xaa\\xb5\\xba\\xc0-\\xd6\\xd8-\\xf6\\xf8-\\u0131'
    + '\\u0134-\\u0137\\u0139-\\u013e\\u0141-\\u0148\\u014c-\\u0180\\u0189'
    + '\\u0190-\\u0192\\u0197\\u019a\\u019f-\\u01a1\\u01ab\\u01ae-\\u01b0'
    + '\\u01b6\\u01cd-\\u01dc\\u01de-\\u01ed\\u01f0\\u01f4-\\u01f5'
    + '\\u01fa-\\u0217\\u0261\\u02b0\\u02b2-\\u02b3\\u02b7-\\u02b8'
    + '\\u02e1-\\u02e3\\u0386\\u0388-\\u038a\\u038c\\u038e\\u0391-\\u0392'
    + '\\u0395-\\u0397\\u0399-\\u039a\\u039c-\\u039d\\u039f\\u03a1'
    + '\\u03a4-\\u03a5\\u03a7\\u03aa-\\u03ab\\u03b2\\u03bc\\u03d0'
    + '\\u03d2-\\u03d4\\u1e00-\\u1e99\\u1e9b\\u1ea0-\\u1ef9\\u1f08-\\u1f0f'
    + '\\u1f18-\\u1f1d\\u1f28-\\u1f2f\\u1f38-\\u1f3f\\u1f48-\\u1f4d\\u1f59'
    + '\\u1f5b\\u1f5d\\u1f5f\\u1f88-\\u1f8f\\u1f98-\\u1f9f\\u1fb8-\\u1fbc'
    + '\\u1fc8-\\u1fcc\\u1fd8-\\u1fdb\\u1fe8-\\u1fec\\u1ff8-\\u1ff9\\u207f'
    + '\\u2102\\u2107\\u210a-\\u2113\\u2115\\u2119-\\u211d\\u2124\\u2128'
    + '\\u212a-\\u212d\\u212f-\\u2131\\u2133-\\u2134\\uff21-\\uff3a'
    + '\\uff41-\\uff5a]');
// The set of characters that can appear as the second or subsequent character
// in an identifier, and that are significant.
var identifierChar = (
    '[\\$0-9A-Z_a-z\\xaa\\xb5\\xba\\xc0-\\xd6\\xd8-\\xf6\\xf8-\\u0131'
    + '\\u0134-\\u0137\\u0139-\\u013e\\u0141-\\u0148\\u014c-\\u0180\\u0189'
    + '\\u0190-\\u0192\\u0197\\u019a\\u019f-\\u01a1\\u01ab\\u01ae-\\u01b0'
    + '\\u01b6\\u01cd-\\u01dc\\u01de-\\u01ed\\u01f0\\u01f4-\\u01f5'
    + '\\u01fa-\\u0217\\u0261\\u02b0\\u02b2-\\u02b3\\u02b7-\\u02b8'
    + '\\u02e1-\\u02e3\\u0300-\\u0345\\u0360-\\u0361\\u0386\\u0388-\\u038a'
    + '\\u038c\\u038e\\u0391-\\u0392\\u0395-\\u0397\\u0399-\\u039a'
    + '\\u039c-\\u039d\\u039f\\u03a1\\u03a4-\\u03a5\\u03a7\\u03aa-\\u03ab'
    + '\\u03b2\\u03bc\\u03d0\\u03d2-\\u03d4\\u0483-\\u0486\\u0591-\\u05a1'
    + '\\u05a3-\\u05b9\\u05bb-\\u05bd\\u05bf\\u05c1-\\u05c2\\u05c4'
    + '\\u064b-\\u0652\\u0660-\\u0669\\u0670\\u06d6-\\u06dc\\u06df-\\u06e4'
    + '\\u06e7-\\u06e8\\u06ea-\\u06ed\\u06f0-\\u06f9\\u0901-\\u0903\\u093c'
    + '\\u093e-\\u094d\\u0951-\\u0954\\u0962-\\u0963\\u0966-\\u096f'
    + '\\u0981-\\u0983\\u09bc\\u09be-\\u09c4\\u09c7-\\u09c8\\u09cb-\\u09cd'
    + '\\u09d7\\u09e2-\\u09e3\\u09e6-\\u09ef\\u0a02\\u0a3c\\u0a3e-\\u0a42'
    + '\\u0a47-\\u0a48\\u0a4b-\\u0a4d\\u0a66-\\u0a71\\u0a81-\\u0a83\\u0abc'
    + '\\u0abe-\\u0ac5\\u0ac7-\\u0ac9\\u0acb-\\u0acd\\u0ae6-\\u0aef'
    + '\\u0b01-\\u0b03\\u0b3c\\u0b3e-\\u0b43\\u0b47-\\u0b48\\u0b4b-\\u0b4d'
    + '\\u0b56-\\u0b57\\u0b66-\\u0b6f\\u0b82\\u0bbe-\\u0bc2\\u0bc6-\\u0bc8'
    + '\\u0bca-\\u0bcd\\u0bd7\\u0be7-\\u0bef\\u0c01-\\u0c03\\u0c3e-\\u0c44'
    + '\\u0c46-\\u0c48\\u0c4a-\\u0c4d\\u0c55-\\u0c56\\u0c66-\\u0c6f'
    + '\\u0c82-\\u0c83\\u0cbe-\\u0cc4\\u0cc6-\\u0cc8\\u0cca-\\u0ccd'
    + '\\u0cd5-\\u0cd6\\u0ce6-\\u0cef\\u0d02-\\u0d03\\u0d3e-\\u0d43'
    + '\\u0d46-\\u0d48\\u0d4a-\\u0d4d\\u0d57\\u0d66-\\u0d6f\\u0e31'
    + '\\u0e34-\\u0e3a\\u0e47-\\u0e4e\\u0e50-\\u0e59\\u0eb1\\u0eb4-\\u0eb9'
    + '\\u0ebb-\\u0ebc\\u0ec8-\\u0ecd\\u0ed0-\\u0ed9\\u0f18-\\u0f19'
    + '\\u0f20-\\u0f29\\u0f35\\u0f37\\u0f39\\u0f3e-\\u0f3f\\u0f71-\\u0f84'
    + '\\u0f86-\\u0f87\\u0f90-\\u0f95\\u0f97\\u0f99-\\u0fad\\u0fb1-\\u0fb7'
    + '\\u0fb9\\u1e00-\\u1e99\\u1e9b\\u1ea0-\\u1ef9\\u1f08-\\u1f0f'
    + '\\u1f18-\\u1f1d\\u1f28-\\u1f2f\\u1f38-\\u1f3f\\u1f48-\\u1f4d\\u1f59'
    + '\\u1f5b\\u1f5d\\u1f5f\\u1f88-\\u1f8f\\u1f98-\\u1f9f\\u1fb8-\\u1fbc'
    + '\\u1fc8-\\u1fcc\\u1fd8-\\u1fdb\\u1fe8-\\u1fec\\u1ff8-\\u1ff9\\u207f'
    + '\\u20d0-\\u20dc\\u20e1\\u2102\\u2107\\u210a-\\u2113\\u2115'
    + '\\u2119-\\u211d\\u2124\\u2128\\u212a-\\u212d\\u212f-\\u2131'
    + '\\u2133-\\u2134\\u302a-\\u302f\\u3099-\\u309a\\ufb1e\\ufe20-\\ufe23'
    + '\\uff10-\\uff19\\uff21-\\uff3a\\uff41-\\uff5a]');

// Words that cannot be used as a Cajita identifier.  Includes all ES3
// and ES4 reserved keywords, and ES4 contextually reserved keywords.
var reservedKeywords = set(
    'abstract', 'boolean', 'break', 'byte', 'case', 'catch', 'char',
    'class', 'const', 'continue', 'debugger', 'default', 'delete', 'do',
    'double', 'else', 'enum', 'export', 'extends', 'false', 'final',
    'finally', 'float', 'for', 'function', 'goto', 'if', 'implements',
    'import', 'in', 'instanceof', 'int', 'interface', 'let', 'long',
    'namespace', 'native', 'new', 'null', 'override', 'package',
    'private', 'protected', 'public', 'return', 'short', 'static',
    'super', 'switch', 'synchronized', 'this', 'throw', 'throws',
    'transient', 'true', 'try', 'typeof', 'use', 'var', 'void',
    'volatile', 'while', 'with', 'yield'
    );

// Strings that can appear adjacent to other tokens without an intervening
// ignorable token.
// This includes all ES3.1 and ES4 operators.
var punctuation = set(
    '!', '!=', '!==', '%', '%=', '&', '&&', '&&=', '&=', '(', ')',
    '*', '*=', '+', '++', '+=', ',', '-', '--', '-=', '.', '..', '...',
    ':', '::', ';', '<', '<<',  '<<=', '<=', '=', '==', '===', '>', '>=',
    '>>', '>>=', '>>>', '>>>=', '?', '[', ']', '^', '^=', '{', '|', '|=',
    '||', '||=', '}', '~', '/', '/='
    );

// Use a trie to parse punctuation.  This is more complicated than
//     '^(?:' + map(filter(...), escapeRegex).join('|') + ')'
// but guarantees eager matching left-to-right.
var punctuationPatternExclDot = toTriePattern(
    filter(members(punctuation), function (s) { return /^[^.]/.test(s); }));
 
// Regular expression text for literals, operators, identifiers, and keywords.
// The (?! ... ) blocks below prevent matches if the token is followed
// by something matching the block contents, so /a(?!b)/ will not match "ab",
// but will match "a" or "ac".
var significant = (
    '(?:'
    + [
       // String literals do not allow \v since it's interpreted inconsistently,
       '\"(?:[^\\\\\"' + charSetUnicodeNewline + ']+|' + charEscape + ')*\"',
       '\'(?:[^\\\\\'' + charSetUnicodeNewline + ']+|' + charEscape + ')*\'',

       // Numbers do not have sign attached.  "- 0" tokenizes as "-0".
       // By disallowing identifier chars after them, we can expand to handle
       // number suffixes like the proposed 'm' suffix for floating decimal.
       '(?:0[xX][0-9a-fA-F]+(?!' + identifierChar + '|\\.))',  // Hex Literal
       '(?:0[0-7]+(?!' + identifierChar + '|\\.))',  // Octal Literal
       ('(?:' + (
                 '(?:\\.[0-9]+|(?:0|[1-9][0-9]*)(?:\\.[0-9]*)?)'
                 + '(?:[eE][+-]?[0-9]+)?'
                 + '(?!' + identifierChar + ')'
                 )
        + ')'),

       // Punctuation strings can abut any, but . needs to be handled
       // specially to disambiguate when its part of a decimal literal.
       '(?:' + punctuationPatternExclDot + ')',
       '(?:\\.{1,3}(?![0-9]))',

       // Separating keywords from non-keywords is done in a post-lex token
       // classification phase.
       '(?:' + identifierStart + identifierChar + '*)',
       ].join('|')
    + ')');

// Matches a cajita token at the beginning of a string, capturing it in group 1
// iff it is significant.
var cajitaToken = new RegExp('^(?:' + ignorable + '|(' + significant + '))');

// Token type tags.
var KEYWORD_TOKEN = '#KEYWORD';
var NUMBER_TOKEN = '#NUMBER';
var STRING_TOKEN = '#STRING';
var PUNCTUATION_TOKEN = '#PUNCTUATION';
var WORD_TOKEN = '#WORD';

/**
 * Classifies token text as a string, number, word, keyword, or other.
 * @param {string} text of a non-comment, non-whitespace token.
 * @return {string} one of the *_TOKEN constants defined above.
 */
function classifyToken(tok) {
  if (punctuation.hasOwnProperty(tok)) { return PUNCTUATION_TOKEN; }
  switch (tok.charAt(0)) {
    case '"': case '\'': return STRING_TOKEN;
    case '0': case '1': case '2': case '3': case '4':
    case '5': case '6': case '7': case '8': case '9': return NUMBER_TOKEN;
    case '.':
      return (/^\.[0-9]/.test(tok)) ? NUMBER_TOKEN : PUNCTUATION_TOKEN;
    default:
      return reservedKeywords.hasOwnProperty(tok) ? KEYWORD_TOKEN : WORD_TOKEN;
  }
}
function Token(text, charPos, type) {
  /** The text of the token. */
  this.text = text;
  /**
   * The position in the input stream.  To test for adjacency of tokens from
   * the same stream, do {@code a.charPos + a.text.length === b.charPos}.
   */
  this.charPos = charPos;
  /**
   * One of the *_TOKEN constants.
   */
  this.type = type;
}
Token.prototype.toString = function () {
  return '[Token ' + this.text + ' ' + this.type + '@' + this.charPos + ']';
};

/**
 * A value whose meaning depends on {@code this.type}, e.g. the number
 * correspoinding to a numeric literal, or the decoded value of a string
 * literal.
 */
Token.prototype.getValue = function () {
  var text = this.text;
  switch (this.type) {
    case STRING_TOKEN:
      return text.substring(1, text.substring(text.length - 1)).replace(
          /\\(?:u([a-f0-9]{4})|([bfnrt\\\'\"]))/,
          function (_, hex, ch) {
            return hex ? String.fromCharCode(parseInt(hex, 16)) : escChars[ch];
          });
      break;
    case NUMBER_TOKEN:
      if (/[\.xX]/.test(text)) { return Number(text); }  // Hex and decimal.
      // parseInt chooses octal on most platforms if text starts with
      // '0' but doesn't have to according to ES262.
      return parseInt(text, /^0/.test(text) ? 8 : 10);
    case KEYWORD_TOKEN:
      switch (text) {
        case 'false': return false;
        case 'true': return true;
        case 'null': return null;
      }
      break;
    case WORD_TOKEN:
      // TODO: Decode unicode escapes here.
      break;
  }
  return text;
};

// Tokens that can precede '/' or '/='.  This is a subset of those that
// ES3.1 allows
// Note: This is NOT sufficient to prevent lexically valid Cajita from parsing
// as a string of EcmaScript that includes a regexp literal, because of
// semicolon insertion.  It is sufficient if the lexed string contains no
// newlines.
var divOpPreceders = {
  ')': true,
  ']': true
};
divOpPreceders[NUMBER_TOKEN] = divOpPreceders[WORD_TOKEN] = true;

/**
 * Returns a generator over non-whitespace, non-comment tokens in cajitaSrc.
 *
 * @param {string} of Cajita source code starting at the beginning of the
 *   Program production.
 * @return {Function} from void->Token
 */
function Lexer(cajitaSrc) {
  if (!(this instanceof Lexer)) { throw new Error('Constructor sans new'); }
  if ('string' !== typeof cajitaSrc) { throw new Error(typeof cajitaSrc); }

  if (programFilter.test(cajitaSrc)) {
    // TODO: good error messages.
    throw new SyntaxError(
        'Bad content : "' + cajitaSrc.match(programFilter) + '"');
  }

  function die(msg) {
    var snippet = cEscape(cajitaSrc.length > 13
                          ? cajitaSrc.substring(0, 10) + '...'
                          : cajitaSrc);
    throw new SyntaxError(
        msg + ' at char ' + charPos + ' : "' + snippet + '"');
  }

  var charPos = 0;
  var lastToken = null;

  return function lexer() {
    while (cajitaSrc) {
      var match = cajitaSrc.match(cajitaToken);
      if (!match) {
        die('Cajita syntax error');
      }
      var n = match[0].length;
      if (!n) { die('Internal error'); }  // Should never occur
      var result;
      if (match[1]) {  // A real token
        var text = match[1];
        var type = classifyToken(text);
        if (text.charAt(0) === '/') {
          if (!lastToken) {
            die('Program cannot start with a division operator');
          } else if (!(divOpPreceders.hasOwnProperty(lastToken.type)
                       || divOpPreceders.hasOwnProperty(lastToken.text))) {
            die('Division operator cannot follow "'
                + cEscape(lastToken.text || '') + '"');
          }
        }
        result = new Token(text, charPos, type);
        if (result.type === WORD_TOKEN && /__$/.test(result.getValue())) {
          die('Bad identifier ' + text);
        }
        lastToken = result;
      }
      cajitaSrc = cajitaSrc.substring(n);
      charPos += n;
      if (result) { return result; }
    }
  };
}




return {
  KEYWORD_TOKEN: KEYWORD_TOKEN,
  NUMBER_TOKEN: NUMBER_TOKEN,
  PUNCTUATION_TOKEN: PUNCTUATION_TOKEN,
  STRING_TOKEN: STRING_TOKEN,
  WORD_TOKEN: WORD_TOKEN,
  Lexer: Lexer,
  Token: Token
};



 
// Utilities.
function filter(array, predicate) {
  var out = [];
  for (var i = 0, n = array.length, k = -1; i < n; ++i) {
    var el = array[i];
    if (predicate(el)) { out[++k] = el; }
  }
  return out;
}

function map(array, xform) {
  var out = [];
  for (var i = array.length; --i >= 0;) { out[i] = xform(array[i]); }
  return out;
}

/** An Object that maps all the input strings to true. */
function set(var_args) {
  var result = {};
  for (var i = 0, n = arguments.length; i < n; ++i) {
    result[arguments[i]] = true;
  }
  return result;
}

/** The dual of set. */
function members(obj) {
  var keys = [];
  var i = -1;
  for (keys[++i] in obj);
  return keys;
}

function escapeRegex(s) {
  return s.replace(/[^a-zA-Z0-9_!=<>&%,:;]/g, '\\$&');
}

function cEscape(s) {
  return s.replace(/\r/g, '\\r').replace(/\n/g, '\\n')
      .replace(/[\\\"]/g, '\\$&')
      .replace(/[^\x20-\x7e]/g, function (ch) {
                                  var esc = ch.charCodeAt(0).toString(16);
                                  while (esc.length < 4) { esc = '0' + esc; }
                                  return '\\u' + esc;
                                });
}

function toTriePattern(strs) {
  strs = strs.slice(0);
  strs.sort();  // Defaults to lexical ordering.
  // Produce a Trie where str is the prefix string for that node, and terminal
  // is true iff that node's prefix is in strs.
  var root = { str: '', children: [], terminal: false };
  var stack = [ root ];
  for (var i = 0, n = strs.length; i < n; ++i) {
    var str = strs[i];
    var topIdx = stack.length;
    while (--topIdx > 0) {
      var top = stack[topIdx];
      var topStr = top.str;
      if (topStr.length < str.length
          && str.substring(0, topStr.length) === topStr) {
        break;
      }
    }
    var top = stack[topIdx];
    var leaf = { str: str, children: [], terminal: true };
    top.children.push(leaf);
    stack.splice(topIdx + 1, stack.length - topIdx - 1, leaf);
  }

  // Compose a regular expression that will match all and only strings in strs.
  var regexBuf = [];
  function toRegex(trie, prefixLen) {
    regexBuf.push(escapeRegex(trie.str.substring(prefixLen)));
    var n = trie.children.length;
    var strLen = trie.str.length;
    if (n) {
      regexBuf.push('(?:');
      for (var i = 0; i < n; ++i) {
        var child = trie.children[i];
        if (i) { regexBuf.push('|'); }
        toRegex(child, strLen);
      }

      // Only output a ? iff trie is a node that produces a complete
      // match, since otherwise one of its descendents is required to
      // produce a valid match.
      regexBuf.push(trie.terminal ? ')?' : ')');
    }
  }
  toRegex(root, 0);
  return regexBuf.join('');
}

})();
