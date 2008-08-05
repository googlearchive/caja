var htmlInterp = (function () {
function safeHtml(s) {
  var buf = [];
  if (s instanceof StringInterpolation) {
    var state =  s.interpolate(
        htmlScanner_, htmlEscaper_, html.State.PCDATA, buf);
    if (!isValidEndState(state)) {
      throw new Error('Interpolation ' + s + ' ended in invalid state');
    }
  } else {
    htmlEscaper_(s, html.State.PCDATA, buf);
  }
  return buf.join('');
}

var htmlScanner_ = (function () {
  window.cache = {};
  var size = 0;

  return function (s, state, buf) {
    if (isRecording(state)) { return process(s, state, buf); }
    var cacheKey = s + ':' + state;
    var endState = window.cache[cacheKey];
    if (endState !== undefined) { return endState; }
    endState = process(s, state, buf);
    if (!isRecording(endState)) {
      if (++size === 100) { window.cache = {}; }
      window.cache[cacheKey] = endState;
    }
    return endState;
  };
})();

var HTML_ENTITIES_ = {
  amp: '&',
  quot: '"',
  lt: '<',
  gt: '>',
  apos: '\'',
  nbsp: '\xA0'
};
function decodeHtmlEntity(entity) {
  if (entity.charAt(0) === '#') {
    if (entity.length > 1) {
      var ch1 = entity.charAt(1);
      var codepoint;
      if (ch1 === 'x' || ch1 === 'X') {
        codepoint = parseInt(entity.substring(2), 16);
      } else {
        codepoint = parseInt(entity.substring(1), 10);
      }
      return isNaN(codepoint) ? null : String.fromCharCode(codepoint);
    }
  } else {
    return HTML_ENTITIES_[entity.toLowerCase()];
  }
}

function htmlEscaper_(substitution, currentState, out) {
  var stateId = getStateId(currentState);
  switch (stateId) {
    case html.State.PCDATA:
    case html.State.RCDATA:
    case html.State.DQ_ATTRIB_VALUE:
    case html.State.SQ_ATTRIB_VALUE:
      out.push(htmlEscape_(substitution.toString()));
      break;
    case html.State.SP_ATTRIB_VALUE:
      out.push(htmlSpEscape_(substitution.toString()));
      break;
    case html.State.CDATA:
      // TODO: check for apparent end tag
      out.push(substitution.toString());
      break;
    case html.State.DQ_CSS:
      if (typeof substitution == 'number') {
        out.push(substitution);
      } else {
        esc_('\'', substitution, out, cssEscape_, htmlEscape_);
      }
      break;
    case html.State.SQ_CSS:
      if (typeof substitution == 'number') {
        out.push(substitution);
      } else {
        esc_('"', substitution, out, cssEscape_, htmlEscape_);
      }
      break;
    case html.State.SP_CSS:
      if (typeof substitution == 'number') {
        out.push(substitution);
      } else {
        esc_('&#43;', substitution, out, cssEscape_, htmlSpEscape_);
      }
      break;
    case html.State.CDATA_CSS:
      if (typeof substitution == 'number') {
        out.push(substitution);
      } else {
        esc_('"', substitution, out, cssEscape_);
      }
      break;
    case html.State.CDATA_CSS_DQ:
    case html.State.CDATA_CSS_SQ:
      esc_('', substitution, out, cssEscape_);
      break;
    case html.State.DQ_CSS_DQ:
    case html.State.DQ_CSS_SQ:
    case html.State.SQ_CSS_DQ:
    case html.State.SQ_CSS_SQ:
      esc_('', substitution, out, cssEscape_, htmlEscape_);
      break;
    case html.State.CDATA_JS:
    case html.State.DQ_JS:
    case html.State.SQ_JS:
    case html.State.SP_JS:
      switch (typeof substitution) {
        case 'object':
          if (null !== substitution) { break; }
          // fall through
        case 'number':
        case 'boolean':
        case 'undefined':
          out.push('' + substitution);
          return currentState;
      }
      switch (stateId) {
        case html.State.CDATA_JS:
          esc_('\'', substitution, out, jsEscape_);
          break;
        case html.State.DQ_JS:
          esc_('\'', substitution, out, jsEscape_, htmlEscape_);
          break;
        case html.State.SQ_JS:
          esc_('"', substitution, out, jsEscape_, htmlEscape_);
          break;
        default:
          esc_('&#39;', substitution, out, jsEscape_, htmlSpEscape_);
          break;
      }
      break;
    case html.State.CDATA_JS_DQ_STRING:
    case html.State.CDATA_JS_SQ_STRING:
      esc_('', substitution, out, jsEscape_);
      break;
    case html.State.DQ_JS_DQ_STRING:
    case html.State.DQ_JS_SQ_STRING:
    case html.State.SQ_JS_DQ_STRING:
    case html.State.SQ_JS_SQ_STRING:
      esc_('', substitution, out, jsEscape_, htmlEscape_);
      break;
    case html.State.SP_CSS_DQ:
    case html.State.SP_CSS_SQ:
      esc_('', substitution, out, cssEscape_, htmlSpEscape_);
      break;
    case html.State.SP_JS_DQ_STRING:
    case html.State.SP_JS_SQ_STRING:
      esc_('', substitution, out, jsEscape_, htmlSpEscape_);
      break;
    case html.State.TAG_AFTER_EQ:
      out.push('"');
      switch (variableAttribNameValue(currentState)) {
        case html.variable.AttribName.onblur:
          currentState = withStateId(currentState, html.State.DQ_JS);
          break;
        case html.variable.AttribName.style:
          currentState = withStateId(currentState, html.State.DQ_CSS);
          break;
        default:
          currentState = withStateId(currentState, html.State.DQ_ATTRIB_VALUE);
          break;
      }
      htmlEscaper_(substitution, currentState, out);
      out.push('"');
      currentState = html.State.TAG_BODY;
      break;
    case html.State.TAG_BODY:
      // Should start recording attrib name somehow
      currentState = withStateId(currentState, html.State.ATTRIB_NAME);
      // fall through
    case html.State.ATTRIB_NAME:
    case html.State.TAG_NAME:
      substitution = substitution.toString();
      if (/[^a-z0-9]i/.test(substitution)) {
        throw new Error('Bad HTML identifier `' + substitution + '`');
      }
      out.push(substitution);
      break;
    default:
      throw new Error('Bad state ' + currentState);
      break;
  }
  return currentState;
}

var allHtmlRe_ = /[&<>\"\']/;
var allJsRe_ = /[<\"\'\\\r\n\u2028\u2029\/]/;
var allCssRe_ = /[<\"\'\\\r\n]/;
var allHtmlSpRe_ = /[&<>\"\' \t\r\n\/]/;

var ampRe_ = /&/g;
var ltRe_ = /</g;
var gtRe_ = />/g;
var quotRe_ = /\"/g;
var aposRe_ = /\'/g;
var bslashRe_ = /\\/g;
var slashRe_ = /\//g;
var crRe_ = /\r/g;
var lfRe_ = /\n/g;
var u2028Re_ = /\u2028/g;
var u2029Re_ = /\u2029/g;
var spRe_ = / /g;
var tabRe_ = /\t/g;

function htmlEscape_(str) {
  if (!allHtmlRe_.test(str)) { return str; }
  if (str.indexOf('&') !== -1) { str = str.replace(ampRe_, '&amp;'); }
  if (str.indexOf('<') !== -1) { str = str.replace(ltRe_, '&lt;'); }
  if (str.indexOf('>') !== -1) { str = str.replace(gtRe_, '&gt;'); }
  if (str.indexOf('"') !== -1) { str = str.replace(quotRe_, '&#34;'); }
  if (str.indexOf("'") !== -1) { str = str.replace(aposRe_, '&#39;'); }
  return str;
}

function cssEscape_(str) {
  if (!allCssRe_.test(str)) { return str; }
  if (str.indexOf("\\") !== -1) { str = str.replace(bslashRe_, '\\5C '); }
  if (str.indexOf('<') !== -1) { str = str.replace(ltRe_, '\\34 '); }
  if (str.indexOf('"') !== -1) { str = str.replace(quotRe_, '\\22 '); }
  if (str.indexOf("'") !== -1) { str = str.replace(aposRe_, '\\26 '); }
  if (str.indexOf("\r") !== -1) { str = str.replace(crRe_, '\\D '); }
  if (str.indexOf("\n") !== -1) { str = str.replace(lfRe_, '\\A '); }
  return str;
}

function jsEscape_(str) {
  // TODO: escape format control characters
  if (!allJsRe_.test(str)) { return str; }
  if (str.indexOf("\\") !== -1) { str = str.replace(bslashRe_, '\\134'); }
  if (str.indexOf("\u2028") !== -1) {
    str = str.replace(u2028Re_, '\\u2028');
  }
  if (str.indexOf("\u2029") !== -1) {
    str = str.replace(u2029Re_, '\\u2029');
  }
  if (str.indexOf("\r") !== -1) { str = str.replace(crRe_, '\\r'); }
  if (str.indexOf("\n") !== -1) { str = str.replace(lfRe_, '\\n'); }
  if (str.indexOf('<') !== -1) { str = str.replace(ltRe_, '\\074'); }
  if (str.indexOf('"') !== -1) { str = str.replace(quotRe_, '\\042'); }
  if (str.indexOf("'") !== -1) { str = str.replace(aposRe_, '\\047'); }
  if (str.indexOf("/") !== -1) { str = str.replace(slashRe_, '\\057'); }
  return str;
}

/** Escape characters that end a space delimited attribute. */
function htmlSpEscape_(str) {
  if (!allHtmlSpRe_.test(str)) { return str; }
  if (str.indexOf('&') !== -1) { str = str.replace(ampRe_, '&amp;'); }
  if (str.indexOf('<') !== -1) { str = str.replace(ltRe_, '&lt;'); }
  if (str.indexOf('>') !== -1) { str = str.replace(gtRe_, '&gt;'); }
  if (str.indexOf('"') !== -1) { str = str.replace(quotRe_, '&#34;'); }
  if (str.indexOf("'") !== -1) { str = str.replace(aposRe_, '&#39;'); }
  if (str.indexOf(' ') !== -1) { str = str.replace(spRe_, '&#32;'); }
  if (str.indexOf('\t') !== -1) { str = str.replace(tabRe_, '&#9;'); }
  if (str.indexOf('\r') !== -1) { str = str.replace(crRe_, '&#14;'); }
  if (str.indexOf('\n') !== -1) { str = str.replace(lfRe_, '&#10;'); }
  if (str.indexOf('/') !== -1) { str = str.replace(slashRe_, '&#47;'); }
  return str;
}

function esc_(delimiter, str, out, var_args) {
  for (var i = 3, n = arguments.length; i < n; ++i) {
    var esc = arguments[i];
    str = esc(str);
  }
  if (delimiter) {
    out.push(delimiter, str, delimiter);
  } else {
    out.push(str);
  }
}

var html = html || {};
html.variable = html.variable || {};
/**
 * State values usable with {@link #is_state()}.
 * Only public states and start and end states appear in this list.
 * @enum{number}
 */
html.State = {
    APP_DIR: 1,
    APP_DIR_QM: 2,
    ATTRIB_NAME: 3,
    CDATA: 4,
    CDATA_APPARENT_CLOSE_TAG: 5,
    CDATA_CSS: 6,
    CDATA_CSS_COMMENT: 7,
    CDATA_CSS_COMMENT_AST: 8,
    CDATA_CSS_COMMENT_SLASH: 9,
    CDATA_CSS_DQ: 10,
    CDATA_CSS_DQ_ESC: 11,
    CDATA_CSS_SLASH: 12,
    CDATA_CSS_SQ: 13,
    CDATA_CSS_SQ_ESC: 14,
    CDATA_JS: 15,
    CDATA_JS_BLOCK_COMMENT: 16,
    CDATA_JS_BLOCK_COMMENT_AST: 17,
    CDATA_JS_DQ_STRING: 18,
    CDATA_JS_DQ_STRING_ESC: 19,
    CDATA_JS_LINE_COMMENT: 20,
    CDATA_JS_SLASH: 21,
    CDATA_JS_SQ_STRING: 22,
    CDATA_JS_SQ_STRING_ESC: 23,
    CDATA_LT: 24,
    CDATA_LT_SLASH: 25,
    CDATA_POSSIBLE_END_TAG1: 26,
    CDATA_POSSIBLE_END_TAG2: 27,
    CDATA_POSSIBLE_END_TAG_NAME: 28,
    CLOSE_TAG_START: 29,
    DIRECTIVE: 30,
    DIRECTIVE_DQ: 31,
    DIRECTIVE_SQ: 32,
    DQ_ATTRIB_VALUE: 33,
    DQ_CSS: 34,
    DQ_CSS_COMMENT: 35,
    DQ_CSS_COMMENT_AST: 36,
    DQ_CSS_COMMENT_SLASH: 37,
    DQ_CSS_DQ: 38,
    DQ_CSS_DQ_ESC: 39,
    DQ_CSS_SLASH: 40,
    DQ_CSS_SQ: 41,
    DQ_CSS_SQ_ESC: 42,
    DQ_JS: 43,
    DQ_JS_BLOCK_COMMENT: 44,
    DQ_JS_BLOCK_COMMENT_AST: 45,
    DQ_JS_DQ_STRING: 46,
    DQ_JS_DQ_STRING_ESC: 47,
    DQ_JS_LINE_COMMENT: 48,
    DQ_JS_SLASH: 49,
    DQ_JS_SQ_STRING: 50,
    DQ_JS_SQ_STRING_ESC: 51,
    HTML_COMMENT: 52,
    HTML_END_COMMENT1: 53,
    HTML_END_COMMENT2: 54,
    HTML_ENTITY: 55,
    HTML_ENTITY_BODY: 56,
    HTML_START_COMMENT: 57,
    LT_BANG: 58,
    PCDATA: 59,
    PLAIN_TEXT: 60,
    RCDATA: 61,
    RCDATA_APPARENT_CLOSE_TAG: 62,
    RCDATA_LT: 63,
    RCDATA_LT_SLASH: 64,
    SP_ATTRIB_VALUE: 65,
    SP_CSS: 66,
    SP_CSS_COMMENT: 67,
    SP_CSS_COMMENT_AST: 68,
    SP_CSS_COMMENT_SLASH: 69,
    SP_CSS_DQ: 70,
    SP_CSS_DQ_ESC: 71,
    SP_CSS_SLASH: 72,
    SP_CSS_SQ: 73,
    SP_CSS_SQ_ESC: 74,
    SP_JS: 75,
    SP_JS_BLOCK_COMMENT: 76,
    SP_JS_BLOCK_COMMENT_AST: 77,
    SP_JS_DQ_STRING: 78,
    SP_JS_DQ_STRING_ESC: 79,
    SP_JS_LINE_COMMENT: 80,
    SP_JS_SLASH: 81,
    SP_JS_SQ_STRING: 82,
    SP_JS_SQ_STRING_ESC: 83,
    SQ_ATTRIB_VALUE: 84,
    SQ_CSS: 85,
    SQ_CSS_COMMENT: 86,
    SQ_CSS_COMMENT_AST: 87,
    SQ_CSS_COMMENT_SLASH: 88,
    SQ_CSS_DQ: 89,
    SQ_CSS_DQ_ESC: 90,
    SQ_CSS_SLASH: 91,
    SQ_CSS_SQ: 92,
    SQ_CSS_SQ_ESC: 93,
    SQ_JS: 94,
    SQ_JS_BLOCK_COMMENT: 95,
    SQ_JS_BLOCK_COMMENT_AST: 96,
    SQ_JS_DQ_STRING: 97,
    SQ_JS_DQ_STRING_ESC: 98,
    SQ_JS_LINE_COMMENT: 99,
    SQ_JS_SLASH: 100,
    SQ_JS_SQ_STRING: 101,
    SQ_JS_SQ_STRING_ESC: 102,
    TAG_AFTER_ATTRIB: 103,
    TAG_AFTER_EQ: 104,
    TAG_BODY: 105,
    TAG_NAME: 106,
    TAG_START: 107
    };
/**
 * Value indices for the variable "attrib_name".
 * @enum{number}
 */
html.variable.AttribName = {
    'onblur': 1,
    'style': 2
    };
function variableAttribNameValue(currentState) {
  if ("number" !== typeof currentState) {
    currentState = currentState[currentState.length - 1];
  }
  return (currentState & 0x180000) >> 19;
}
var alias_table = { '\r': '\n', ' ': '\t', '1': '0', '2': '0', '3': '0', '4': '0', '5': '0', '6': '0', '7': '0', '8': '0', '9': '0', 'B': 'A', 'C': 'A', 'D': 'A', 'E': 'A', 'F': 'A', 'G': 'A', 'H': 'A', 'I': 'A', 'J': 'A', 'K': 'A', 'L': 'A', 'M': 'A', 'N': 'A', 'O': 'A', 'P': 'A', 'Q': 'A', 'R': 'A', 'S': 'A', 'T': 'A', 'U': 'A', 'V': 'A', 'W': 'A', 'X': 'A', 'Y': 'A', 'Z': 'A', 'a': 'A', 'b': 'A', 'c': 'A', 'd': 'A', 'e': 'A', 'f': 'A', 'g': 'A', 'h': 'A', 'i': 'A', 'j': 'A', 'k': 'A', 'l': 'A', 'm': 'A', 'n': 'A', 'o': 'A', 'p': 'A', 'q': 'A', 'r': 'A', 's': 'A', 't': 'A', 'u': 'A', 'v': 'A', 'w': 'A', 'x': 'A', 'y': 'A', 'z': 'A', '\u2028': '\n', '\u2029': '\n' };
var var_value_to_index = [
    /* __return_pc #0: */ { 'app_dir': 1, 'app_dir_qm': 2, 'attrib_name': 3, 'cdata': 4, 'cdata_apparent_close_tag': 5, 'cdata_css': 6, 'cdata_css_comment': 7, 'cdata_css_comment_ast': 8, 'cdata_css_comment_slash': 9, 'cdata_css_dq': 10, 'cdata_css_dq_esc': 11, 'cdata_css_slash': 12, 'cdata_css_sq': 13, 'cdata_css_sq_esc': 14, 'cdata_js': 15, 'cdata_js_block_comment': 16, 'cdata_js_block_comment_ast': 17, 'cdata_js_dq_string': 18, 'cdata_js_dq_string_esc': 19, 'cdata_js_line_comment': 20, 'cdata_js_slash': 21, 'cdata_js_sq_string': 22, 'cdata_js_sq_string_esc': 23, 'cdata_lt': 24, 'cdata_lt_slash': 25, 'cdata_possible_end_tag1': 26, 'cdata_possible_end_tag2': 27, 'cdata_possible_end_tag_name': 28, 'close_tag_start': 29, 'directive': 30, 'directive_dq': 31, 'directive_sq': 32, 'dq_attrib_value': 33, 'dq_css': 34, 'dq_css_comment': 35, 'dq_css_comment_ast': 36, 'dq_css_comment_slash': 37, 'dq_css_dq': 38, 'dq_css_dq_esc': 39, 'dq_css_slash': 40, 'dq_css_sq': 41, 'dq_css_sq_esc': 42, 'dq_js': 43, 'dq_js_block_comment': 44, 'dq_js_block_comment_ast': 45, 'dq_js_dq_string': 46, 'dq_js_dq_string_esc': 47, 'dq_js_line_comment': 48, 'dq_js_slash': 49, 'dq_js_sq_string': 50, 'dq_js_sq_string_esc': 51, 'html_comment': 52, 'html_end_comment1': 53, 'html_end_comment2': 54, 'html_entity': 55, 'html_entity_body': 56, 'html_start_comment': 57, 'lt_bang': 58, 'pcdata': 59, 'plain_text': 60, 'rcdata': 61, 'rcdata_apparent_close_tag': 62, 'rcdata_lt': 63, 'rcdata_lt_slash': 64, 'sp_attrib_value': 65, 'sp_css': 66, 'sp_css_comment': 67, 'sp_css_comment_ast': 68, 'sp_css_comment_slash': 69, 'sp_css_dq': 70, 'sp_css_dq_esc': 71, 'sp_css_slash': 72, 'sp_css_sq': 73, 'sp_css_sq_esc': 74, 'sp_js': 75, 'sp_js_block_comment': 76, 'sp_js_block_comment_ast': 77, 'sp_js_dq_string': 78, 'sp_js_dq_string_esc': 79, 'sp_js_line_comment': 80, 'sp_js_slash': 81, 'sp_js_sq_string': 82, 'sp_js_sq_string_esc': 83, 'sq_attrib_value': 84, 'sq_css': 85, 'sq_css_comment': 86, 'sq_css_comment_ast': 87, 'sq_css_comment_slash': 88, 'sq_css_dq': 89, 'sq_css_dq_esc': 90, 'sq_css_slash': 91, 'sq_css_sq': 92, 'sq_css_sq_esc': 93, 'sq_js': 94, 'sq_js_block_comment': 95, 'sq_js_block_comment_ast': 96, 'sq_js_dq_string': 97, 'sq_js_dq_string_esc': 98, 'sq_js_line_comment': 99, 'sq_js_slash': 100, 'sq_js_sq_string': 101, 'sq_js_sq_string_esc': 102, 'tag_after_attrib': 103, 'tag_after_eq': 104, 'tag_body': 105, 'tag_name': 106, 'tag_start': 107 },
    /* apparent_tag_name #1: */ { 'plaintext': 1, 'script': 2, 'style': 3, 'textarea': 4, 'title': 5, 'xmp': 6 },
    /* attrib_delim #2: */ { ' ': 1, '"': 2, '\'': 3 },
    /* attrib_name #3: */ { 'onload': 1, 'onblur': 1, 'onsubmit': 1, 'onmousedown': 1, 'onclick': 1, 'onmouseout': 1, 'onkeypress': 1, 'onchange': 1, 'onreset': 1, 'onunload': 1, 'onkeydown': 1, 'onselect': 1, 'onmousemove': 1, 'onmouseover': 1, 'onmouseup': 1, 'onkeyup': 1, 'onfocus': 1, 'ondblclick': 1, 'onresize': 1, 'style': 2 },
    /* entity_name #4: */ {  },
    /* tag_name #5: */ { 'plaintext': 1, 'script': 2, 'style': 3, 'textarea': 4, 'title': 5, 'xmp': 6 }
    ];
var var_values = [
    /* __return_pc #0: */ [ null, 'app_dir', 'app_dir_qm', 'attrib_name', 'cdata', 'cdata_apparent_close_tag', 'cdata_css', 'cdata_css_comment', 'cdata_css_comment_ast', 'cdata_css_comment_slash', 'cdata_css_dq', 'cdata_css_dq_esc', 'cdata_css_slash', 'cdata_css_sq', 'cdata_css_sq_esc', 'cdata_js', 'cdata_js_block_comment', 'cdata_js_block_comment_ast', 'cdata_js_dq_string', 'cdata_js_dq_string_esc', 'cdata_js_line_comment', 'cdata_js_slash', 'cdata_js_sq_string', 'cdata_js_sq_string_esc', 'cdata_lt', 'cdata_lt_slash', 'cdata_possible_end_tag1', 'cdata_possible_end_tag2', 'cdata_possible_end_tag_name', 'close_tag_start', 'directive', 'directive_dq', 'directive_sq', 'dq_attrib_value', 'dq_css', 'dq_css_comment', 'dq_css_comment_ast', 'dq_css_comment_slash', 'dq_css_dq', 'dq_css_dq_esc', 'dq_css_slash', 'dq_css_sq', 'dq_css_sq_esc', 'dq_js', 'dq_js_block_comment', 'dq_js_block_comment_ast', 'dq_js_dq_string', 'dq_js_dq_string_esc', 'dq_js_line_comment', 'dq_js_slash', 'dq_js_sq_string', 'dq_js_sq_string_esc', 'html_comment', 'html_end_comment1', 'html_end_comment2', 'html_entity', 'html_entity_body', 'html_start_comment', 'lt_bang', 'pcdata', 'plain_text', 'rcdata', 'rcdata_apparent_close_tag', 'rcdata_lt', 'rcdata_lt_slash', 'sp_attrib_value', 'sp_css', 'sp_css_comment', 'sp_css_comment_ast', 'sp_css_comment_slash', 'sp_css_dq', 'sp_css_dq_esc', 'sp_css_slash', 'sp_css_sq', 'sp_css_sq_esc', 'sp_js', 'sp_js_block_comment', 'sp_js_block_comment_ast', 'sp_js_dq_string', 'sp_js_dq_string_esc', 'sp_js_line_comment', 'sp_js_slash', 'sp_js_sq_string', 'sp_js_sq_string_esc', 'sq_attrib_value', 'sq_css', 'sq_css_comment', 'sq_css_comment_ast', 'sq_css_comment_slash', 'sq_css_dq', 'sq_css_dq_esc', 'sq_css_slash', 'sq_css_sq', 'sq_css_sq_esc', 'sq_js', 'sq_js_block_comment', 'sq_js_block_comment_ast', 'sq_js_dq_string', 'sq_js_dq_string_esc', 'sq_js_line_comment', 'sq_js_slash', 'sq_js_sq_string', 'sq_js_sq_string_esc', 'tag_after_attrib', 'tag_after_eq', 'tag_body', 'tag_name', 'tag_start' ],
    /* apparent_tag_name #1: */ [ null, 'plaintext', 'script', 'style', 'textarea', 'title', 'xmp' ],
    /* attrib_delim #2: */ [ null, ' ', '"', '\'' ],
    /* attrib_name #3: */ [ null, 'onblur', 'style' ],
    /* entity_name #4: */ [ null ],
    /* tag_name #5: */ [ null, 'plaintext', 'script', 'style', 'textarea', 'title', 'xmp' ]
    ];
var functions = [
    function attrib_value(t) {
      var valueIndex = (t.currentState & 0x180000) >> 19;
      switch (valueIndex) {
        case 2: /* 'style' */
          // Invoking function css_attrib
          functions[3](t);
          break;
        case 1: /* 'onresize' */
          // Invoking function js_attrib
          functions[6](t);
          break;
        default:
          // Invoking function html_attrib_value
          functions[5](t);
          break;
      }
    },
    function cdata_check_close_tag(t) {
      var valueIndex = (t.currentState & 0x1c000) >> 14;
      if (var_values[1][valueIndex] === var_values[5][(t.currentState & 0xe00000) >> 21]) {
        // Clearing value of variable tag_name
        t.currentState &= 0xff1fffff;
        // Clearing value of variable apparent_tag_name
        t.currentState &= 0xfffe3fff;
        // Running as state tag_body
        var stateTable = transition_table[105];
        var dest = stateTable[t.ch];
        if (dest === undefined) { dest = stateTable[""]; }
        if (dest >= 0) {
          t.currentState = (t.currentState & 0xffffff80) | dest;
        } else {
          functions[~dest](t);
        }
      } else {
        // Running as state cdata
        var stateTable = transition_table[4];
        var dest = stateTable[t.ch];
        if (dest === undefined) { dest = stateTable[""]; }
        if (dest >= 0) {
          t.currentState = (t.currentState & 0xffffff80) | dest;
        } else {
          functions[~dest](t);
        }
      }
    },
    function cdata_check_end_tag(t) {
      var valueIndex = (t.currentState & 0x1c000) >> 14;
      if (var_values[1][valueIndex] === var_values[5][(t.currentState & 0xe00000) >> 21]) {
        // Clearing value of variable apparent_tag_name
        t.currentState &= 0xfffe3fff;
        // Clearing value of variable tag_name
        t.currentState &= 0xff1fffff;
        // Running as state tag_body
        var stateTable = transition_table[105];
        var dest = stateTable[t.ch];
        if (dest === undefined) { dest = stateTable[""]; }
        if (dest >= 0) {
          t.currentState = (t.currentState & 0xffffff80) | dest;
        } else {
          functions[~dest](t);
        }
      } else {
        // Returning from subroutine
        var returnIndex = (t.currentState & 0x3f80) >> 7;
        var stateMap = transition_table[returnIndex];
        var transition = (t.decoded && stateMap["\\" + t.ch]) || stateMap[t.ch];
        if (transition === undefined) {
          transition = stateMap[""];
        }
        if (transition >= 0) {
          t.currentState = transition | (t.currentState & 0xffffff80);
        } else {
          functions[~transition](t);
        }
        delete t.decoded;
      }
    },
    function css_attrib(t) {
      var valueIndex = (t.currentState & 0x60000) >> 17;
      switch (valueIndex) {
        case 2: /* '"' */
          // Goto state dq_css
          t.currentState = ((t.currentState & 0xffffff80) | 0x22);
          break;
        case 3: /* "'" */
          // Goto state sq_css
          t.currentState = ((t.currentState & 0xffffff80) | 0x55);
          break;
        default:
          // Goto state sp_css
          t.currentState = ((t.currentState & 0xffffff80) | 0x42);
          break;
      }
    },
    function end_tag(t) {
      var valueIndex = (t.currentState & 0xe00000) >> 21;
      switch (valueIndex) {
        case 6: /* 'xmp' */
          // Goto state cdata
          t.currentState = ((t.currentState & 0xffffff80) | 0x4);
          break;
        case 3: /* 'style' */
          // Store value of attrib_delim

          var value = fetchTail(t, 2);
          t.recordings[2] = undefined;
          var value_index = var_value_to_index[2][value] || 0;
          t.currentState = (t.currentState & 0xfff9ffff) | (value_index << 17);
          // Goto state cdata_css
          t.currentState = ((t.currentState & 0xffffff80) | 0x6);
          break;
        case 2: /* 'script' */
          // Store value of attrib_delim

          var value = fetchTail(t, 2);
          t.recordings[2] = undefined;
          var value_index = var_value_to_index[2][value] || 0;
          t.currentState = (t.currentState & 0xfff9ffff) | (value_index << 17);
          // Goto state cdata_js
          t.currentState = ((t.currentState & 0xffffff80) | 0xf);
          break;
        case 5: /* 'title' */
        case 4: /* 'textarea' */
          // Goto state rcdata
          t.currentState = ((t.currentState & 0xffffff80) | 0x3d);
          break;
        case 1: /* 'plaintext' */
          // Goto state plain_text
          t.currentState = ((t.currentState & 0xffffff80) | 0x3c);
          break;
        default:
          // Goto state pcdata
          t.currentState = ((t.currentState & 0xffffff80) | 0x3b);
          break;
      }
    },
    function html_attrib_value(t) {
      var valueIndex = (t.currentState & 0x60000) >> 17;
      switch (valueIndex) {
        case 2: /* '"' */
          // Goto state dq_attrib_value
          t.currentState = ((t.currentState & 0xffffff80) | 0x21);
          break;
        case 3: /* "'" */
          // Goto state sq_attrib_value
          t.currentState = ((t.currentState & 0xffffff80) | 0x54);
          break;
        default:
          // Goto state sp_attrib_value
          t.currentState = ((t.currentState & 0xffffff80) | 0x41);
          break;
      }
    },
    function js_attrib(t) {
      var valueIndex = (t.currentState & 0x60000) >> 17;
      switch (valueIndex) {
        case 2: /* '"' */
          // Goto state dq_js
          t.currentState = ((t.currentState & 0xffffff80) | 0x2b);
          break;
        case 3: /* "'" */
          // Goto state sq_js
          t.currentState = ((t.currentState & 0xffffff80) | 0x5e);
          break;
        default:
          // Goto state sp_js
          t.currentState = ((t.currentState & 0xffffff80) | 0x4b);
          break;
      }
    },
    function rcdata_check_close_tag(t) {
      var valueIndex = (t.currentState & 0x1c000) >> 14;
      if (var_values[1][valueIndex] === var_values[5][(t.currentState & 0xe00000) >> 21]) {
        // Clearing value of variable tag_name
        t.currentState &= 0xff1fffff;
        // Clearing value of variable apparent_tag_name
        t.currentState &= 0xfffe3fff;
        // Running as state tag_body
        var stateTable = transition_table[105];
        var dest = stateTable[t.ch];
        if (dest === undefined) { dest = stateTable[""]; }
        if (dest >= 0) {
          t.currentState = (t.currentState & 0xffffff80) | dest;
        } else {
          functions[~dest](t);
        }
      } else {
        // Running as state rcdata
        var stateTable = transition_table[61];
        var dest = stateTable[t.ch];
        if (dest === undefined) { dest = stateTable[""]; }
        if (dest >= 0) {
          t.currentState = (t.currentState & 0xffffff80) | dest;
        } else {
          functions[~dest](t);
        }
      }
    },
    function anon$8___gosub_55_(t) {
      // Storing the current state in a variable
      t.currentState = (t.currentState & 0xffffc07f) | ((t.currentState & 0x7f) << 7);
      // Jumping to a subroutine
      t.currentState = (t.currentState & 0xffffff80) | 55;
    },
    function anon$9___error_expected_letter_after_(t) {
      // Reporting error 'expected letter after </'
      throw new Error('expected letter after </');
    },
    function anon$10___invoke_4_(t) {
      // Invoking function end_tag
      functions[4](t);
    },
    function anon$11___gosub_26_(t) {
      // Storing the current state in a variable
      t.currentState = (t.currentState & 0xffffc07f) | ((t.currentState & 0x7f) << 7);
      // Jumping to a subroutine
      t.currentState = (t.currentState & 0xffffff80) | 26;
    },
    function anon$12___goto_103_(t) {
      // Store value of attrib_name

      var value = fetchTail(t, 3);
      value = value && value.toLowerCase();
      t.recordings[3] = undefined;
      var value_index = var_value_to_index[3][value] || 0;
      t.currentState = (t.currentState & 0xffe7ffff) | (value_index << 19);
      // Goto state tag_after_attrib
      t.currentState = ((t.currentState & 0xffffff80) | 0x67);
    },
    function anon$13___goto_104_(t) {
      // Store value of attrib_name

      var value = fetchTail(t, 3);
      value = value && value.toLowerCase();
      t.recordings[3] = undefined;
      var value_index = var_value_to_index[3][value] || 0;
      t.currentState = (t.currentState & 0xffe7ffff) | (value_index << 19);
      // Goto state tag_after_eq
      t.currentState = ((t.currentState & 0xffffff80) | 0x68);
    },
    function anon$14___error_(t) {
      // Reporting error ''
      throw new Error();
    },
    function anon$15___invoke_7_(t) {
      // Store value of apparent_tag_name

      var value = fetchTail(t, 1);
      value = value && value.toLowerCase();
      t.recordings[1] = undefined;
      var value_index = var_value_to_index[1][value] || 0;
      t.currentState = (t.currentState & 0xfffe3fff) | (value_index << 14);
      // Invoking function rcdata_check_close_tag
      functions[7](t);
    },
    function anon$16___goto_5_(t) {
      // Start recording value of apparent_tag_name

      var buftail = t.buf.length - 1;
      t.recordings[1] = (buftail << 16) | t.i;
      // Goto state cdata_apparent_close_tag
      t.currentState = ((t.currentState & 0xffffff80) | 0x5);
    },
    function anon$17___goto_3_(t) {
      // Start recording value of attrib_name

      var buftail = t.buf.length - 1;
      t.recordings[3] = (buftail << 16) | t.i;
      // Goto state attrib_name
      t.currentState = ((t.currentState & 0xffffff80) | 0x3);
    },
    function anon$18___error_in_comment_(t) {
      // Reporting error '-- in comment'
      throw new Error('-- in comment');
    },
    function anon$19___error_should_be_followed_by_(t) {
      // Reporting error '<!- should be followed by -'
      throw new Error('<!- should be followed by -');
    },
    function anon$20___goto_106_(t) {
      // Start recording value of tag_name

      var buftail = t.buf.length - 1;
      t.recordings[5] = (buftail << 16) | t.i;
      // Goto state tag_name
      t.currentState = ((t.currentState & 0xffffff80) | 0x6a);
    },
    function anon$21___error_Nested_comment_in_CSS_(t) {
      // Reporting error 'Nested comment in CSS'
      throw new Error('Nested comment in CSS');
    },
    function anon$22___invoke_2_(t) {
      // Store value of apparent_tag_name

      var value = fetchTail(t, 1);
      value = value && value.toLowerCase();
      t.recordings[1] = undefined;
      var value_index = var_value_to_index[1][value] || 0;
      t.currentState = (t.currentState & 0xfffe3fff) | (value_index << 14);
      // Invoking function cdata_check_end_tag
      functions[2](t);
    },
    function anon$23___invoke_0_(t) {
      // Setting value of variable attrib_delim to ' '
      t.currentState = (t.currentState & 0xfff9ffff) | 0x20000;
      // Invoking function attrib_value
      functions[0](t);
    },
    function anon$24___invoke_0_(t) {
      // Setting value of variable attrib_delim to '"'
      t.currentState = (t.currentState & 0xfff9ffff) | 0x40000;
      // Invoking function attrib_value
      functions[0](t);
    },
    function anon$25___invoke_0_(t) {
      // Setting value of variable attrib_delim to "'"
      t.currentState = (t.currentState & 0xfff9ffff) | 0x60000;
      // Invoking function attrib_value
      functions[0](t);
    },
    function anon$26___invoke_1_(t) {
      // Store value of apparent_tag_name

      var value = fetchTail(t, 1);
      value = value && value.toLowerCase();
      t.recordings[1] = undefined;
      var value_index = var_value_to_index[1][value] || 0;
      t.currentState = (t.currentState & 0xfffe3fff) | (value_index << 14);
      // Invoking function cdata_check_close_tag
      functions[1](t);
    },
    function anon$27___goto_62_(t) {
      // Start recording value of apparent_tag_name

      var buftail = t.buf.length - 1;
      t.recordings[1] = (buftail << 16) | t.i;
      // Goto state rcdata_apparent_close_tag
      t.currentState = ((t.currentState & 0xffffff80) | 0x3e);
    },
    function anon$28___goto_28_(t) {
      // Start recording value of apparent_tag_name

      var buftail = t.buf.length - 1;
      t.recordings[1] = (buftail << 16) | t.i;
      // Goto state cdata_possible_end_tag_name
      t.currentState = ((t.currentState & 0xffffff80) | 0x1c);
    },
    function anon$29___goto_105_(t) {
      // Store value of tag_name

      var value = fetchTail(t, 5);
      value = value && value.toLowerCase();
      t.recordings[5] = undefined;
      var value_index = var_value_to_index[5][value] || 0;
      t.currentState = (t.currentState & 0xff1fffff) | (value_index << 21);
      // Goto state tag_body
      t.currentState = ((t.currentState & 0xffffff80) | 0x69);
    },
    function anon$30___invoke_4_(t) {
      // Store value of tag_name

      var value = fetchTail(t, 5);
      value = value && value.toLowerCase();
      t.recordings[5] = undefined;
      var value_index = var_value_to_index[5][value] || 0;
      t.currentState = (t.currentState & 0xff1fffff) | (value_index << 21);
      // Invoking function end_tag
      functions[4](t);
    },
    function anon$31___goto_56_(t) {
      // Start recording value of entity_name

      var buftail = t.buf.length - 1;
      t.recordings[4] = (buftail << 16) | t.i;
      // Goto state html_entity_body
      t.currentState = ((t.currentState & 0xffffff80) | 0x38);
    },
    function anon$32___return_(t) {
      // Returning from subroutine
      var returnIndex = (t.currentState & 0x3f80) >> 7;
      var stateMap = transition_table[returnIndex];
      var transition = (t.decoded && stateMap["\\" + t.ch]) || stateMap[t.ch];
      if (transition === undefined) {
        transition = stateMap[""];
      }
      if (transition >= 0) {
        t.currentState = transition | (t.currentState & 0xffffff80);
      } else {
        functions[~transition](t);
      }
      delete t.decoded;
    },
    function anon$33___return_(t) {
      // decoding the content of variable entity_name
      t.ch = decodeHtmlEntity(fetchTail(t, 4));
      t.ch = alias_table[t.ch] || t.ch;
      t.decoded = true;
      t.recordings[4] = undefined;
      // Returning from subroutine
      var returnIndex = (t.currentState & 0x3f80) >> 7;
      var stateMap = transition_table[returnIndex];
      var transition = (t.decoded && stateMap["\\" + t.ch]) || stateMap[t.ch];
      if (transition === undefined) {
        transition = stateMap[""];
      }
      if (transition >= 0) {
        t.currentState = transition | (t.currentState & 0xffffff80);
      } else {
        functions[~transition](t);
      }
      delete t.decoded;
    },
    function anon$34___error_does_not_start_either_a_comment_or_a_doctype_(t) {
      // Reporting error '<! does not start either a comment or a doctype'
      throw new Error('<! does not start either a comment or a doctype');
    }
    ];
var transition_table = [
    null,
    /* app_dir #1: */ { '?': 2 /* app_dir_qm */, '': 1 /* app_dir */ },
    /* app_dir_qm #2: */ { '>': 59 /* pcdata */, '': 2 /* app_dir_qm */ },
    /* attrib_name #3: */ { '': -15 /* anon$14___error_ */, '\t': -13 /* anon$12___goto_103_ */, '\n': -13 /* anon$12___goto_103_ */, '0': 3 /* attrib_name */, '=': -14 /* anon$13___goto_104_ */, '>': -11 /* anon$10___invoke_4_ */, 'A': 3 /* attrib_name */ },
    /* cdata #4: */ { '<': 24 /* cdata_lt */, '': 4 /* cdata */ },
    /* cdata_apparent_close_tag #5: */ { '': -27 /* anon$26___invoke_1_ */, 'A': 5 /* cdata_apparent_close_tag */ },
    /* cdata_css #6: */ { '"': 10 /* cdata_css_dq */, '\'': 13 /* cdata_css_sq */, '/': 12 /* cdata_css_slash */, '<': -12 /* anon$11___gosub_26_ */, '': 6 /* cdata_css */ },
    /* cdata_css_comment #7: */ { '*': 8 /* cdata_css_comment_ast */, '/': 9 /* cdata_css_comment_slash */, '<': -12 /* anon$11___gosub_26_ */, '': 7 /* cdata_css_comment */ },
    /* cdata_css_comment_ast #8: */ { '/': 6 /* cdata_css */, '<': -12 /* anon$11___gosub_26_ */, '': 8 /* cdata_css_comment_ast */ },
    /* cdata_css_comment_slash #9: */ { '*': -22 /* anon$21___error_Nested_comment_in_CSS_ */, '<': -12 /* anon$11___gosub_26_ */, '': 9 /* cdata_css_comment_slash */ },
    /* cdata_css_dq #10: */ { '"': 6 /* cdata_css */, '<': -12 /* anon$11___gosub_26_ */, '\\': 11 /* cdata_css_dq_esc */, '': 10 /* cdata_css_dq */ },
    /* cdata_css_dq_esc #11: */ { '': 10 /* cdata_css_dq */, '<': -12 /* anon$11___gosub_26_ */ },
    /* cdata_css_slash #12: */ { '*': 7 /* cdata_css_comment */, '<': -12 /* anon$11___gosub_26_ */, '': 12 /* cdata_css_slash */ },
    /* cdata_css_sq #13: */ { '\'': 6 /* cdata_css */, '<': -12 /* anon$11___gosub_26_ */, '\\': 14 /* cdata_css_sq_esc */, '': 13 /* cdata_css_sq */ },
    /* cdata_css_sq_esc #14: */ { '': 13 /* cdata_css_sq */, '<': -12 /* anon$11___gosub_26_ */ },
    /* cdata_js #15: */ { '': 15 /* cdata_js */, '"': 18 /* cdata_js_dq_string */, '\'': 22 /* cdata_js_sq_string */, '/': 21 /* cdata_js_slash */, '<': -12 /* anon$11___gosub_26_ */ },
    /* cdata_js_block_comment #16: */ { '*': 17 /* cdata_js_block_comment_ast */, '<': -12 /* anon$11___gosub_26_ */, '': 16 /* cdata_js_block_comment */ },
    /* cdata_js_block_comment_ast #17: */ { '/': 15 /* cdata_js */, '<': -12 /* anon$11___gosub_26_ */, '': 17 /* cdata_js_block_comment_ast */ },
    /* cdata_js_dq_string #18: */ { '\n': -15 /* anon$14___error_ */, '"': 15 /* cdata_js */, '<': -12 /* anon$11___gosub_26_ */, '\\': 19 /* cdata_js_dq_string_esc */, '': 18 /* cdata_js_dq_string */ },
    /* cdata_js_dq_string_esc #19: */ { '': 18 /* cdata_js_dq_string */, '<': -12 /* anon$11___gosub_26_ */ },
    /* cdata_js_line_comment #20: */ { '\n': 15 /* cdata_js */, '<': -12 /* anon$11___gosub_26_ */, '': 20 /* cdata_js_line_comment */ },
    /* cdata_js_slash #21: */ { '': 15 /* cdata_js */, '*': 16 /* cdata_js_block_comment */, '/': 20 /* cdata_js_line_comment */, '<': -12 /* anon$11___gosub_26_ */ },
    /* cdata_js_sq_string #22: */ { '\n': -15 /* anon$14___error_ */, '\'': 15 /* cdata_js */, '<': -12 /* anon$11___gosub_26_ */, '\\': 23 /* cdata_js_sq_string_esc */, '': 22 /* cdata_js_sq_string */ },
    /* cdata_js_sq_string_esc #23: */ { '': 22 /* cdata_js_sq_string */, '<': -12 /* anon$11___gosub_26_ */ },
    /* cdata_lt #24: */ { '/': 25 /* cdata_lt_slash */, '': 24 /* cdata_lt */ },
    /* cdata_lt_slash #25: */ { 'A': -17 /* anon$16___goto_5_ */, '': 25 /* cdata_lt_slash */ },
    /* cdata_possible_end_tag1 #26: */ { '/': 27 /* cdata_possible_end_tag2 */, '<': -12 /* anon$11___gosub_26_ */, '': 26 /* cdata_possible_end_tag1 */ },
    /* cdata_possible_end_tag2 #27: */ { '<': -12 /* anon$11___gosub_26_ */, 'A': -29 /* anon$28___goto_28_ */, '': 27 /* cdata_possible_end_tag2 */ },
    /* cdata_possible_end_tag_name #28: */ { '': -23 /* anon$22___invoke_2_ */, '<': -12 /* anon$11___gosub_26_ */, 'A': 28 /* cdata_possible_end_tag_name */ },
    /* close_tag_start #29: */ { '': -10 /* anon$9___error_expected_letter_after_ */, 'A': 106 /* tag_name */ },
    /* directive #30: */ { '"': 31 /* directive_dq */, '\'': 32 /* directive_sq */, '>': 59 /* pcdata */, '': 30 /* directive */ },
    /* directive_dq #31: */ { '"': 30 /* directive */, '': 31 /* directive_dq */ },
    /* directive_sq #32: */ { '\'': 30 /* directive */, '': 32 /* directive_sq */ },
    /* dq_attrib_value #33: */ { '"': 105 /* tag_body */, '': 33 /* dq_attrib_value */ },
    /* dq_css #34: */ { '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '\'': 41 /* dq_css_sq */, '/': 40 /* dq_css_slash */, '\\"': 38 /* dq_css_dq */, '': 34 /* dq_css */ },
    /* dq_css_comment #35: */ { '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '*': 36 /* dq_css_comment_ast */, '/': 37 /* dq_css_comment_slash */, '': 35 /* dq_css_comment */ },
    /* dq_css_comment_ast #36: */ { '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '/': 34 /* dq_css */, '': 36 /* dq_css_comment_ast */ },
    /* dq_css_comment_slash #37: */ { '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '*': -22 /* anon$21___error_Nested_comment_in_CSS_ */, '': 37 /* dq_css_comment_slash */ },
    /* dq_css_dq #38: */ { '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '\\': 39 /* dq_css_dq_esc */, '\\"': 34 /* dq_css */, '': 38 /* dq_css_dq */ },
    /* dq_css_dq_esc #39: */ { '': 38 /* dq_css_dq */, '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */ },
    /* dq_css_slash #40: */ { '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '*': 35 /* dq_css_comment */, '': 40 /* dq_css_slash */ },
    /* dq_css_sq #41: */ { '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '\'': 34 /* dq_css */, '\\': 42 /* dq_css_sq_esc */, '': 41 /* dq_css_sq */ },
    /* dq_css_sq_esc #42: */ { '': 41 /* dq_css_sq */, '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */ },
    /* dq_js #43: */ { '': 43 /* dq_js */, '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '\'': 50 /* dq_js_sq_string */, '/': 49 /* dq_js_slash */, '\\"': 46 /* dq_js_dq_string */ },
    /* dq_js_block_comment #44: */ { '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '*': 45 /* dq_js_block_comment_ast */, '': 44 /* dq_js_block_comment */ },
    /* dq_js_block_comment_ast #45: */ { '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '/': 43 /* dq_js */, '': 45 /* dq_js_block_comment_ast */ },
    /* dq_js_dq_string #46: */ { '\n': -15 /* anon$14___error_ */, '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '\\': 47 /* dq_js_dq_string_esc */, '\\"': 43 /* dq_js */, '': 46 /* dq_js_dq_string */ },
    /* dq_js_dq_string_esc #47: */ { '': 46 /* dq_js_dq_string */, '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */ },
    /* dq_js_line_comment #48: */ { '\n': 43 /* dq_js */, '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '': 48 /* dq_js_line_comment */ },
    /* dq_js_slash #49: */ { '': 43 /* dq_js */, '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '*': 44 /* dq_js_block_comment */, '/': 48 /* dq_js_line_comment */ },
    /* dq_js_sq_string #50: */ { '\n': -15 /* anon$14___error_ */, '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '\'': 43 /* dq_js */, '\\': 51 /* dq_js_sq_string_esc */, '': 50 /* dq_js_sq_string */ },
    /* dq_js_sq_string_esc #51: */ { '': 50 /* dq_js_sq_string */, '"': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */ },
    /* html_comment #52: */ { '-': 53 /* html_end_comment1 */, '': 52 /* html_comment */ },
    /* html_end_comment1 #53: */ { '': 52 /* html_comment */, '-': 54 /* html_end_comment2 */ },
    /* html_end_comment2 #54: */ { '': -19 /* anon$18___error_in_comment_ */, '>': 59 /* pcdata */ },
    /* html_entity #55: */ { '': -33 /* anon$32___return_ */, '#': -32 /* anon$31___goto_56_ */, 'A': -32 /* anon$31___goto_56_ */ },
    /* html_entity_body #56: */ { '': -15 /* anon$14___error_ */, '0': 56 /* html_entity_body */, ';': -34 /* anon$33___return_ */, 'A': 56 /* html_entity_body */ },
    /* html_start_comment #57: */ { '': -20 /* anon$19___error_should_be_followed_by_ */, '-': 52 /* html_comment */ },
    /* lt_bang #58: */ { '': -35 /* anon$34___error_does_not_start_either_a_comment_or_a_doctype_ */, '-': 57 /* html_start_comment */, 'A': 30 /* directive */ },
    /* pcdata #59: */ { '<': 107 /* tag_start */, '': 59 /* pcdata */ },
    /* plain_text #60: */ { '': 60 /* plain_text */ },
    /* rcdata #61: */ { '<': 63 /* rcdata_lt */, '': 61 /* rcdata */ },
    /* rcdata_apparent_close_tag #62: */ { '': -16 /* anon$15___invoke_7_ */, 'A': 62 /* rcdata_apparent_close_tag */ },
    /* rcdata_lt #63: */ { '/': 64 /* rcdata_lt_slash */, '': 63 /* rcdata_lt */ },
    /* rcdata_lt_slash #64: */ { 'A': -28 /* anon$27___goto_62_ */, '': 64 /* rcdata_lt_slash */ },
    /* sp_attrib_value #65: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '': 65 /* sp_attrib_value */ },
    /* sp_css #66: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '"': 70 /* sp_css_dq */, '&': -9 /* anon$8___gosub_55_ */, '\'': 73 /* sp_css_sq */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '\\/': 72 /* sp_css_slash */, '': 66 /* sp_css */ },
    /* sp_css_comment #67: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '*': 68 /* sp_css_comment_ast */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '\\/': 69 /* sp_css_comment_slash */, '': 67 /* sp_css_comment */ },
    /* sp_css_comment_ast #68: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '\\/': 66 /* sp_css */, '': 68 /* sp_css_comment_ast */ },
    /* sp_css_comment_slash #69: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '*': -22 /* anon$21___error_Nested_comment_in_CSS_ */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '': 69 /* sp_css_comment_slash */ },
    /* sp_css_dq #70: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '"': 66 /* sp_css */, '&': -9 /* anon$8___gosub_55_ */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '\\': 71 /* sp_css_dq_esc */, '': 70 /* sp_css_dq */ },
    /* sp_css_dq_esc #71: */ { '': 70 /* sp_css_dq */, '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */ },
    /* sp_css_slash #72: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '*': 67 /* sp_css_comment */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '': 72 /* sp_css_slash */ },
    /* sp_css_sq #73: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '\'': 66 /* sp_css */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '\\': 74 /* sp_css_sq_esc */, '': 73 /* sp_css_sq */ },
    /* sp_css_sq_esc #74: */ { '': 73 /* sp_css_sq */, '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */ },
    /* sp_js #75: */ { '': 75 /* sp_js */, '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '"': 78 /* sp_js_dq_string */, '&': -9 /* anon$8___gosub_55_ */, '\'': 82 /* sp_js_sq_string */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '\\/': 81 /* sp_js_slash */ },
    /* sp_js_block_comment #76: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '*': 77 /* sp_js_block_comment_ast */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '': 76 /* sp_js_block_comment */ },
    /* sp_js_block_comment_ast #77: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '\\/': 75 /* sp_js */, '': 77 /* sp_js_block_comment_ast */ },
    /* sp_js_dq_string #78: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '"': 75 /* sp_js */, '&': -9 /* anon$8___gosub_55_ */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '\\': 79 /* sp_js_dq_string_esc */, '\\\n': -15 /* anon$14___error_ */, '': 78 /* sp_js_dq_string */ },
    /* sp_js_dq_string_esc #79: */ { '': 78 /* sp_js_dq_string */, '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */ },
    /* sp_js_line_comment #80: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '\\\n': 75 /* sp_js */, '': 80 /* sp_js_line_comment */ },
    /* sp_js_slash #81: */ { '': 75 /* sp_js */, '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '*': 76 /* sp_js_block_comment */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '\\/': 80 /* sp_js_line_comment */ },
    /* sp_js_sq_string #82: */ { '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '\'': 75 /* sp_js */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, '\\': 83 /* sp_js_sq_string_esc */, '\\\n': -15 /* anon$14___error_ */, '': 82 /* sp_js_sq_string */ },
    /* sp_js_sq_string_esc #83: */ { '': 82 /* sp_js_sq_string */, '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '&': -9 /* anon$8___gosub_55_ */, '/': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */ },
    /* sq_attrib_value #84: */ { '\'': 105 /* tag_body */, '': 84 /* sq_attrib_value */ },
    /* sq_css #85: */ { '"': 89 /* sq_css_dq */, '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '/': 91 /* sq_css_slash */, '\\\'': 92 /* sq_css_sq */, '': 85 /* sq_css */ },
    /* sq_css_comment #86: */ { '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '*': 87 /* sq_css_comment_ast */, '/': 88 /* sq_css_comment_slash */, '': 86 /* sq_css_comment */ },
    /* sq_css_comment_ast #87: */ { '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '/': 85 /* sq_css */, '': 87 /* sq_css_comment_ast */ },
    /* sq_css_comment_slash #88: */ { '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '*': -22 /* anon$21___error_Nested_comment_in_CSS_ */, '': 88 /* sq_css_comment_slash */ },
    /* sq_css_dq #89: */ { '"': 85 /* sq_css */, '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '\\': 90 /* sq_css_dq_esc */, '': 89 /* sq_css_dq */ },
    /* sq_css_dq_esc #90: */ { '': 89 /* sq_css_dq */, '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */ },
    /* sq_css_slash #91: */ { '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '*': 86 /* sq_css_comment */, '': 91 /* sq_css_slash */ },
    /* sq_css_sq #92: */ { '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '\\': 93 /* sq_css_sq_esc */, '\\\'': 85 /* sq_css */, '': 92 /* sq_css_sq */ },
    /* sq_css_sq_esc #93: */ { '': 92 /* sq_css_sq */, '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */ },
    /* sq_js #94: */ { '': 94 /* sq_js */, '"': 97 /* sq_js_dq_string */, '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '/': 100 /* sq_js_slash */, '\\\'': 101 /* sq_js_sq_string */ },
    /* sq_js_block_comment #95: */ { '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '*': 96 /* sq_js_block_comment_ast */, '': 95 /* sq_js_block_comment */ },
    /* sq_js_block_comment_ast #96: */ { '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '/': 94 /* sq_js */, '': 96 /* sq_js_block_comment_ast */ },
    /* sq_js_dq_string #97: */ { '\n': -15 /* anon$14___error_ */, '"': 94 /* sq_js */, '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '\\': 98 /* sq_js_dq_string_esc */, '': 97 /* sq_js_dq_string */ },
    /* sq_js_dq_string_esc #98: */ { '': 97 /* sq_js_dq_string */, '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */ },
    /* sq_js_line_comment #99: */ { '\n': 94 /* sq_js */, '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '': 99 /* sq_js_line_comment */ },
    /* sq_js_slash #100: */ { '': 94 /* sq_js */, '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '*': 95 /* sq_js_block_comment */, '/': 99 /* sq_js_line_comment */ },
    /* sq_js_sq_string #101: */ { '\n': -15 /* anon$14___error_ */, '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */, '\\': 102 /* sq_js_sq_string_esc */, '\\\'': 94 /* sq_js */, '': 101 /* sq_js_sq_string */ },
    /* sq_js_sq_string_esc #102: */ { '': 101 /* sq_js_sq_string */, '&': -9 /* anon$8___gosub_55_ */, '\'': 105 /* tag_body */ },
    /* tag_after_attrib #103: */ { '': -15 /* anon$14___error_ */, '=': 104 /* tag_after_eq */ },
    /* tag_after_eq #104: */ { '': -15 /* anon$14___error_ */, '\t': 104 /* tag_after_eq */, '\n': 104 /* tag_after_eq */, '"': -25 /* anon$24___invoke_0_ */, '\'': -26 /* anon$25___invoke_0_ */, '0': -24 /* anon$23___invoke_0_ */, '>': -11 /* anon$10___invoke_4_ */, 'A': -24 /* anon$23___invoke_0_ */ },
    /* tag_body #105: */ { '': -15 /* anon$14___error_ */, '\t': 105 /* tag_body */, '\n': 105 /* tag_body */, '>': -11 /* anon$10___invoke_4_ */, 'A': -18 /* anon$17___goto_3_ */ },
    /* tag_name #106: */ { '': -15 /* anon$14___error_ */, '\t': -30 /* anon$29___goto_105_ */, '\n': -30 /* anon$29___goto_105_ */, '/': -30 /* anon$29___goto_105_ */, '0': 106 /* tag_name */, '>': -31 /* anon$30___invoke_4_ */, 'A': 106 /* tag_name */ },
    /* tag_start #107: */ { '!': 58 /* lt_bang */, '/': 29 /* close_tag_start */, '?': 1 /* app_dir */, 'A': -21 /* anon$20___goto_106_ */, '': 107 /* tag_start */ }
    ];

function process(s, currentState, buf) {
  var traversal;
  var recordings = [];
  currentState = unpackRecordings(currentState, recordings);
  
  for (var i = 0, n = s.length; i < n; ++i) {
    var ch = s.charAt(i);
    
    ch = alias_table[ch] || ch;
    
    var stateMap = transition_table[currentState & 0x7f];
    var transition = stateMap[ch];
    if (transition === undefined) { transition = stateMap[""]; }
    
    if (transition >= 0) {
      currentState = transition | (currentState & 0xffffff80);
      
    } else {
      if (!traversal) {
        traversal = { buf: buf, s: s, recordings: recordings };
      }
      traversal.currentState = currentState;
      traversal.ch = ch;
      traversal.i = i;
      
      functions[~transition](traversal);
      currentState = traversal.currentState;
      
    }
  }

  currentState = packRecordings(currentState, recordings);
  
  return currentState;
}

/** True iff the given state is a valid end state. */
function isValidEndState(currentState) {
  if ((typeof currentState) === 'object') {
    currentState = currentState[currentState.length - 1];
  }
  switch (currentState & 0x7f) {
    case 59:
      return true;
    default:
      return false;
  }
}
/**
 * Returns the state id for the given state.
 * @param {number|Array} currentState the opaque state value.
 * @return {number} a value from the html.State enum.
 */
function getStateId(currentState) {
  if ((typeof currentState) === 'object') {
    currentState = currentState[currentState.length - 1];
  }
  return currentState & 0x7f;
}

/**
 * Returns the state replaced with the given state id.
 * @param {number|Array} currentState the opaque state value.
 * @param {number} stateId from the html.State enum.
 * @return {number|Array} an opaque state value.
 */
function withStateId(currentState, stateId) {
  if ((typeof currentState) === 'object') {
    currentState = currentState[currentState.length - 1];
  }
  return (currentState & 0xffffff80) | stateId;
}


/** dual of record operation */
function fetchTail(t, var_index) {
  var recording = t.recordings[var_index];
  if (recording == null) { return null; }  // Match null or undefined
  var chunk = (recording >> 16) & 0xffff;
  var posInChunk = recording & 0xffff;

  var buf = t.buf;
  var tail;
  var last = buf.length - 1;

  if (chunk === last) {
    tail = buf[chunk].substring(posInChunk, t.i);
  } else {
    var sb = [buf[chunk].substring(posInChunk)];
    for (var i = chunk + 1; i < last; ++i) {
      sb.push(buf[i]);
    }
    sb.push(buf[last].substring(0, t.i));
    tail = sb.join('');
  }

  return tail;
}


function unpackRecordings(currentState, recordings) {
  if ((typeof currentState) !== 'number') {
    var last = currentState.length - 1;
    
    for (var i = last; --i >= 0;) {
      recordings[i] = currentState[i];
    }
    return currentState[last];
  }
  var compressed = (currentState / 0x1000000) | 0;
  
  if (compressed) {
    var varIndex = compressed % 6;
    recordings[varIndex] = ((compressed / 6) | 0) - 1;
    
  }
  return currentState & 0xffffff;
}

function isRecording(currentState) {
  return ((currentState & 0xff000000) | 1) !== 1;
}

function packRecordings(currentState, recordings) {
  
  var index;
  for (var i = recordings.length; --i >= 0;) {
    if (recordings[i] !== undefined) {
      if (index !== undefined) {
        index = -1;
        break;
      } else {
        index = i;
      }
    }
  }
  if (index === undefined) {
    
    return currentState;
  }
  if (index >= 0) {
    var compressed = index + 6 * (recordings[index] + 1);
    if (compressed < 0xfffffff) {
      // TODO(mikesamuel): if there's an easy way to interleave bits when
      // computing members of the recordings array, it would make better use
      // of the leftover bits.
      // By interleave bits, I mean combine two bitstrings A0A1... and B0B1...
      // to produce A0B0A1B1... though interleaving by nibble or byte would
      // still give an advantage.
      ;
      return currentState + compressed * 0x1000000;
    }
  }
  recordings.push(currentState);
  
  return recordings;
}
return { safeHtml : safeHtml };
})();
