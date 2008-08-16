// Copyright (C) 2006 Google Inc.
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
 * @fileoverview
 * Provide a factory that allows transformations on HTML.
 * @author mikesamuel@gmail.com
 */


/** @namespace */
var html = (function () {

  var ENTITIES = {
    LT   : '<',
    GT   : '>',
    AMP  : '&',
    NBSP : '\240',
    QUOT : '"',
    APOS : '\''
  };

  var decimalEscapeRe = /^#(\d+)$/;
  var hexEscapeRe = /^#x([0-9A-F]+)$/;
  function lookupEntity(name) {
    name = name.toUpperCase();  // TODO: &pi; is different from &Pi;
    if (ENTITIES.hasOwnProperty(name)) { return ENTITIES[name]; }
    var m = name.match(decimalEscapeRe);
    if (m) {
      return String.fromCharCode(parseInt(m[1], 10));
    } else if (!!(m = name.match(hexEscapeRe))) {
      return String.fromCharCode(parseInt(m[1], 16));
    }
    return '';
  }

  function decodeOneEntity(_, name) {
    return lookupEntity(name);
  }

  var entityRe = /&(#\d+|#x[\da-f]+|\w+);/g;
  function unescapeEntities(s) {
    return s.replace(entityRe, decodeOneEntity);
  }

  var ampRe = /&/g;
  var looseAmpRe = /&([^a-z#]|#(?:[^0-9x]|x(?:[^0-9a-f]|$)|$)|$)/gi;
  var ltRe = /</g;
  var gtRe = />/g;
  var quotRe = /\"/g;

  function escapeAttrib(s) {
    return s.replace(ampRe, '&amp;').replace(ltRe, '&lt;').replace(gtRe, '&gt;')
        .replace(quotRe, '&quot;');
  }

  /**
   * Escape entities in RCDATA that can be escaped without changing the meaning.
   */
  function normalizeRCData(rcdata) {
    return rcdata
        .replace(looseAmpRe, '&amp;$1')
        .replace(ltRe, '&lt;')
        .replace(gtRe, '&gt;');
  }


  /** token definitions. */
  var INSIDE_TAG_TOKEN = new RegExp(
      // Don't capture space.
      '^\\s*(?:'
      // Capture an attribute name in group 1, and value in groups 2-4.
      + ('(?:'
         + '([a-z][a-z-]*)'
         + ('(?:'
            + '\\s*=\\s*'
            + ('(?:'
               + '\"([^\"]*)\"'
               + '|\'([^\']*)\''
               + '|([^>\"\'\\s]*)'
               + ')'
               )
            + ')'
            ) + '?'
         + ')'
         )
      // End of tag captured in group 5.
      + '|(/?>)'
      // Don't capture cruft
      + '|[^\\w\\s>]+)',
      'i');

  var OUTSIDE_TAG_TOKEN = new RegExp(
      '^(?:'
      // Entity captured in group 1.
      + '&(\\#[0-9]+|\\#[x][0-9a-f]+|\\w+);'
      // Comment, doctypes, and processing instructions not captured.
      + '|<!--[\\s\\S]*?-->|<!\w[^>]*>|<\\?[^>*]*>'
      // '/' captured in group 2 for close tags, and name captured in group 3.
      + '|<(/)?([a-z][a-z0-9]*)'
      // Text captured in group 4.
      + '|([^<&]+)'
      // Cruft captured in group 5.
      + '|([<&]))',
      'i');

  /**
   * Given a SAX-like event handler, produce a function that feeds those
   * events and a parameter to the event handler.
   *
   * The event handler has the form:<pre>
   * {
   *   // Name is an upper-case HTML tag name.  Attribs is an array of
   *   // alternating upper-case attribute names, and attribute values.  The
   *   // attribs array is reused by the parser.  Param is the value passed to
   *   // the saxParser.
   *   startTag: function (name, attribs, param) { ... },
   *   endTag:   function (name, param) { ... },
   *   pcdata:   function (text, param) { ... },
   *   rcdata:   function (text, param) { ... },
   *   cdata:    function (text, param) { ... },
   *   startDoc: function (param) { ... },
   *   endDod:   function (param) { ... },
   * }</pre>
   *
   * @param {Object} event handler.
   * @return {Function} that takes a chunk of html and a parameter.
   *   The parameter is passed on to the handler methods.
   */
  function makeSaxParser(handler) {
    return function parse(htmlText, param) {
      htmlText = String(htmlText);
      var htmlUpper = null;

      var inTag = false;  // True iff we're currently processing a tag.
      var attribs = [];  // Accumulates attribute names and values.
      var tagName;  // The name of the tag currently being processed.
      var eflags;  // The element flags for the current tag.
      var openTag;  // True if the current tag is an open tag.

      handler.startDoc && handler.startDoc(param);

      while (htmlText) {
        var m = htmlText.match(inTag ? INSIDE_TAG_TOKEN : OUTSIDE_TAG_TOKEN);
        htmlText = htmlText.substring(m[0].length);

        if (inTag) {
          if (m[1]) { // attribute
            // setAttribute with uppercase names doesn't work on IE6.
            var attribName = m[1].toLowerCase();
            var encodedValue = m[2] || m[3] || m[4];
            var decodedValue;
            if (encodedValue !== null && encodedValue !== void 0) {
              decodedValue = unescapeEntities(encodedValue);
            } else {
              // Use name as value for valueless attribs, so
              //   <input type=checkbox checked>
              // gets attributes ['type', 'checkbox', 'checked', 'checked']
              decodedValue = attribName;
            }
            attribs.push(attribName, decodedValue);
          } else if (m[5]) {
            if (eflags !== void 0) {  // False if not in whitelist.
              if (openTag) {
                handler.startTag && handler.startTag(tagName, attribs, param);
              } else {
                handler.endTag && handler.endTag(tagName, param);
              }
            }

            if (openTag
                && (eflags & (html4.eflags.CDATA | html4.eflags.RCDATA))) {
              if (htmlUpper === null) {
                htmlUpper = htmlText.toLowerCase();
              } else {
                htmlUpper = htmlUpper.substring(
                    htmlUpper.length - htmlText.length);
              }
              var dataEnd = htmlUpper.indexOf('</' + tagName);
              if (dataEnd < 0) { dataEnd = htmlText.length; }
              if (eflags & html4.eflags.CDATA) {
                handler.cdata
                    && handler.cdata(htmlText.substring(0, dataEnd), param);
              } else if (handler.rcdata) {
                handler.rcdata(
                    normalizeRCData(htmlText.substring(0, dataEnd)), param);
              }
              htmlText = htmlText.substring(dataEnd);
            }

            tagName = eflags = openTag = void 0;
            attribs.length = 0;
            inTag = false;
          }
        } else {
          if (m[1]) {  // Entity
            handler.pcdata && handler.pcdata(m[0], param);
          } else if (m[3]) {  // Tag
            openTag = !m[2];
            inTag = true;
            tagName = m[3].toLowerCase();
            eflags = html4.ELEMENTS.hasOwnProperty(tagName)
                ? html4.ELEMENTS[tagName] : void 0;
          } else if (m[4]) {  // Text
            handler.pcdata && handler.pcdata(m[4], param);
          } else if (m[5]) {  // Cruft
            handler.pcdata
                && handler.pcdata(m[5] === '&' ? '&amp;' : '&lt;', param);
          }
        }
      }

      handler.endDoc && handler.endDoc(param);
    };
  }

  return {
    normalizeRCData: normalizeRCData,
    escapeAttrib: escapeAttrib,
    unescapeEntities: unescapeEntities,
    makeSaxParser: makeSaxParser
  };
})();

/**
 * Returns a function that strips unsafe tags and attributes from html.
 * @param {Function} sanitizeAttributes
 *     from tagName, attribs[]) to null or a sanitized attribute array.
 *     The attribs array can be arbitrarily modified, but the same array
 *     instance is reused, so should not be held.
 * @return {Function} from html to sanitized html
 */
html.makeHtmlSanitizer = function (sanitizeAttributes) {
  var stack = [];
  var ignoring = false;
  return html.makeSaxParser({
        startDoc: function (_) {
          stack = [];
          ignoring = false;
        },
        startTag: function (tagName, attribs, out) {
          if (ignoring) { return; }
          if (!html4.ELEMENTS.hasOwnProperty(tagName)) { return; }
          var eflags = html4.ELEMENTS[tagName];
          if (eflags & html4.eflags.UNSAFE) {
            ignoring = !(eflags & html4.eflags.EMPTY);
            return;
          }
          attribs = sanitizeAttributes(tagName, attribs);
          if (attribs) {
            if (!(eflags & html4.eflags.EMPTY)) {
              stack.push(tagName);
            }

            out.push('<', tagName);
            for (var i = 0, n = attribs.length; i < n; i += 2) {
              var attribName = attribs[i],
                  value = attribs[i + 1];
              if (value !== null && value !== void 0) {
                out.push(' ', attribName, '="', html.escapeAttrib(value), '"');
              }
            }
            out.push('>');
          }
        },
        endTag: function (tagName, out) {
          if (ignoring) {
            ignoring = false;
            return;
          }
          if (!html4.ELEMENTS.hasOwnProperty(tagName)) { return; }
          var eflags = html4.ELEMENTS[tagName];
          if (!(eflags & (html4.eflags.UNSAFE | html4.eflags.EMPTY))) {
            var index;
            if (eflags & html4.eflags.OPTIONAL_ENDTAG) {
              for (index = stack.length; --index >= 0;) {
                var stackEl = stack[index];
                if (stackEl === tagName) { break; }
                if (!(html4.ELEMENTS[stackEl] & html4.eflags.OPTIONAL_ENDTAG)) {
                  // Don't pop non optional end tags looking for a match.
                  return;
                }
              }
            } else {
              for (index = stack.length; --index >= 0;) {
                if (stack[index] === tagName) { break; }
              }
            }
            if (index < 0) { return; }  // Not opened.
            for (var i = stack.length; --i > index;) {
              var stackEl = stack[i];
              if (!(html4.ELEMENTS[stackEl] & html4.eflags.OPTIONAL_ENDTAG)) {
                out.push('</', stackEl, '>');
              }
            }
            stack.length = index;
            out.push('</', tagName, '>');
          }
        },
        pcdata: function (text, out) {
          if (!ignoring) { out.push(text); }
        },
        rcdata: function (text, out) {
          if (!ignoring) { out.push(text); }
        },
        cdata: function (text, out) {
          if (!ignoring) { out.push(text); }
        },
        endDoc: function (out) {
          for (var i = stack.length; --i >= 0;) {
            out.push('</', stack[i], '>');
          }
          stack.length = 0;
        }
      });
};


/**
 * Strips unsafe tags and attributes from html.
 * @param {string} html to sanitize
 * @param {Function} opt_urlXform : string -> string? -- a transform to apply to
 *     url attribute values.
 * @param {Function} opt_nmTokenXform : string -> string? -- a transform to
 *     apply to names, ids, and classes.
 * @return {string} html
 */
function html_sanitize(htmlText, opt_urlPolicy, opt_nmTokenPolicy) {
  var out = [];
  html.makeHtmlSanitizer(
      function sanitizeAttribs(tagName, attribs) {
        for (var i = 0; i < attribs.length; i += 2) {
          var attribName = attribs[i];
          var value = attribs[i + 1];
          if (html4.ATTRIBS.hasOwnProperty(attribName)) {
            switch (html4.ATTRIBS[attribName]) {
              case html4.atype.SCRIPT:
              case html4.atype.STYLE:
                value = null;
              case html4.atype.IDREF:
              case html4.atype.NAME:
              case html4.atype.NMTOKENS:
                value = opt_nmTokenPolicy ? opt_nmTokenPolicy(value) : value;
                break;
              case html4.atype.URI:
                value = opt_urlPolicy && opt_urlPolicy(value);
                break;
            }
          } else {
            value = null;
          }
          attribs[i + 1] = value;
        }
        return attribs;
      })(htmlText, out);
  return out.join('');
}
