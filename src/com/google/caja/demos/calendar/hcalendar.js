// Copyright (C) 2008 Google Inc.
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
 * @fileoverview a parser for http://microformats.org/wiki/hcalendar.
 * This file provides an {@link #extractHcal} method that extracts all VEVENTS
 * under a given DOM Node return a list of VEvents represented as lists of
 * content lines.
 *
 * @see http://microformats.org/wiki/microformats
 * @see http://microformats.org/wiki/hcalendar
 *
 * @author mikesamuel@gmail.com
 */


var ICAL_PROD_ID = '-//hcalendar.js v1.0//EN';

/**
 * given a DOM node, finds all hcalendar objects underneath it.
 * @return {Array.<Array.<ContentLine>>} a list of lists of content lines.
 *   Each list of content lines is one VEVENT.
 */
var extractHcal;

(function () {
extractHcal = function (node) {
  var globalProps = {
    title: '$TITLE$',
    url: '$SOURCE$',
    language: null,
    method: 'PUBLISH'
  };

  if (node.ownerDocument) {
    var htmlNode = node.ownerDocument.documentElement;

    var lang = htmlNode.getAttribute('lang');
    if (!lang) { lang = htmlNode.getAttribute('xml:lang'); }
    if (lang) {
      globalProps.language = lang;
    }

    globalProps.title = node.ownerDocument.title;

    var baseNodes = htmlNode.getElementsByTagName('base');
    var baseUrl = null;
    if (baseNodes.length) {
      baseUrl = baseNodes[0].getAttribute('href');
    }
    globalProps.url = baseUrl || node.ownerDocument.URL;
  }

  var events = [];
  doExtract(node, globalProps, events);
  for (var i = 0; i < events.length; ++i) {
    group(events[i], { CATEGORY: 'CATEGORIES' });
  }

  var calendar = [];
  calendar.push(new ContentLine('BEGIN', 'VCALENDAR'));
  calendar.push(new ContentLine('METHOD', globalProps.method));
  if (globalProps.url) {
    calendar.push(new ContentLine('X-ORIGINAL-URL', globalProps.url));
  }
  if (globalProps.title) {
    var titleLine = new ContentLine('X-WR-CALNAME', globalProps.title);
    if (globalProps.language) {
      titleLine.pushAttributes('LANGUAGE', globalProps.language);
    }
    calendar.push(titleLine);
  }
  calendar.push(new ContentLine('PRODID', ICAL_PROD_ID));
  calendar.push(new ContentLine('VERSION', '2.0'));

  for (var i = 0; i < events.length; ++i) {
    calendar.push(new ContentLine('BEGIN', 'VEVENT'));
    calendar.push.apply(calendar, events[i]);
    calendar.push(new ContentLine('END', 'VEVENT'));
  }
  calendar.push(new ContentLine('END', 'VCALENDAR'));
  return calendar;
};

/**
 * finds and processes top level entities.  This handles both VEVENTs and
 * VCALENDARs since VEVENTs are not typically grouped inside a VCALENDAR.
 * If a VCALENDAR is found, it is used to populate globalProps.
 * @param {Node} node root of the subtree to examine
 * @param {Object} globalProps calendar properties.  may be modified
 * @param {Array.<Array.<ContentLine>>} output array of events to append to.
 */
function doExtract(node, globalProps, events) {
  if (node.nodeType !== 1/*ELEMENT_NODE*/) { return; }
  if (VEVENT_CLASS(node)) {
    var contentLines = [];
    parseMicroFormat(node, VEVENT_SCHEMA, globalProps, contentLines);
    events.push(contentLines);
  } else {
    if (VCALENDAR_CLASS(node)) {
      // if we see a vcalendar class, then we should fill globalProps
      var calLines = [];
      parseMicroFormat(node, VCALENDAR_SCHEMA, globalProps, calLines);
      for (var i = calLines.length; --i >= 0;) {
        var line = calLines[i];
        var name = line.getName().toLowerCase();
        if (name in globalProps) {
          globalProps[name] = line.getValues()[0];
        }
      }
    }
    for (var child = node.firstChild; child; child = child.nextSibling) {
      doExtract(child, globalProps, events);
    }
  }
}


// Schema Predicates
var VEVENT_CLASS = classMatcher(['vevent']);
var VCALENDAR_CLASS = classMatcher(['vcalendar']);
var DUR_TIME = '(T(\\d+H(\\d+M(\\d+S)?)?|\\d+M(\\d+S)?|\\d+S))';
var PERIOD_RE = new RegExp(
    '^[+-]?P(\\d+[WD]' + DUR_TIME + '?|' + DUR_TIME + ')$');
var RRULE_RE = new RegExp(
    '^FREQ=(YEAR|MONTH|DAI|WEEK|HOUR|MINUTE|SECOND)LY(;[A-Z0-9;,]+)?$', 'i');
var NON_NEG_INT_RE = new RegExp('^\\\d+$');
var POS_INT_RE = new RegExp('^(0+|0*[1-9]\\d+)$');
var INT_RE = new RegExp('^[+-]?(0+|0*[1-9]\\d+)$');
var NON_ZERO_INT_RE = new RegExp('^[+-]?0*[1-9]\\d+$');
var WDAY_RE = new RegExp('^(MO|TU|WE|TH|FR|SA|SU)$', 'i');
var BYDAY_RE = new RegExp('^([+-]0*[1-9]\\d+)?(MO|TU|WE|TH|FR|SA|SU)$', 'i');
var STATUS_RE = new RegExp('^(?:TENTATIVE|CONFIRMED|CANCELLED)$', 'i');
var CLASS_RE = new RegExp('^(?:PUBLIC|PRIVATE|CONFIDENTIAL)$', 'i');
var TRANSP_RE = new RegExp('^(?:TRANSPARENT|OPAQUE)$', 'i');
var ROLE_RE = new RegExp('^(?:CHAIR|(?:REQ|OPT|NON)-PARTICIPANT)$', 'i');
var RSVP_RE = new RegExp('^(?:TRUE|FALSE)$', 'i');
var PSTAT_RE = new RegExp(
    '^(?:NEEDS-ACTION|ACCEPTED|DECLINED|TENTATIVE|DELEGATED)$', 'i');
var CUTYPE_RE = new RegExp(
    '^(?:INDIVIDUAL|GROUP|RESOURCE|ROOM|UNKNOWN)$', 'i');
var MAILTO_RE = new RegExp('^mailto:', 'i');



// Schema content handlers

/**
 * concatenates all text nodes under node together to produce the value.
 * @param {Node} node.
 * @param {Object} globalProps
 * @return {ContentLine|null}
 */
function handleTextContent(node, globalProps) {
  var content = checkAttributesForText(node, globalProps);
  if (content) { return content; }

  // Otherwise, concatenate all the text nodes.
  var text = [];
  handleTextContentHelper(node, globalProps, text);
  if (!text.length) { return null; }
  text = text.join('').replace(/ $/gm, '');
  if (!text) { return null; }
  content = new ContentLine();
  content.pushValues(text);
  var language = node.getAttribute('xml:lang') || globalProps.language;
  if (language) {
    content.pushAttributes('LANGUAGE', language);
  }
  return content;
}
var textContentHandler = { handle: handleTextContent };

/**
 * handles tags that store their textual content in attributes.
 * @param {Element} node
 * @param {Object} globalProps
 * @return {ContentLine}
 */
function checkAttributesForText(node, globalProps) {
  var attribValue = null;
  switch (node.nodeName.toLowerCase()) {
    case 'abbr':
      // The ABBR's title tag is used to stores values, especially structured
      // data
      attribValue = node.getAttribute('title');
      break;
    case 'img': case 'area':
      // An IMG's alt text may be used for a textual attribute's value
      attribValue = node.getAttribute('alt');
      break;
  }

  if (!attribValue) { return null; }
  // no normalization since attributes are assumed to be preformatted
  var content = new ContentLine();
  content.pushValues(attribValue);
  var language = node.getAttribute('xml:lang') || globalProps.language;
  if (language) {
    content.pushAttributes('LANGUAGE', language);
  }
  return content;
}

/**
 * set of HTML 4.0 tag names that contain cdata do not contain human readable
 * content.
 */
var HTML_TAGS_WITH_NON_HUMAN_READABLE_CONTENT = {
  script: true,
  style: true
};

/**
 * walk the DOM BFS and concatenate all text nodes.
 * After this is called, out will contain the values of text nodes, as they
 * appear to an xml reader.
 * @private
 */
function handleTextContentHelper(node, globalProps, out) {
  if (node.nodeType === 3/*TEXT_NODE*/ ||
      node.nodeType === 4/*CDATA_SECTION_NODE*/) {
    var text = node.nodeValue;
    // if this is not preformatted, then collapse runs of whitespace.
    if (node.parentNode && !isPreformatted(node.parentNode)) {
      text = text
          // flatten runs of whitespace without affecting &nbsp;
          .replace(/[ \t\r\n]+/g, ' ')
          // convert &nbsp;s, which match \s but not [ \t\r\n], to space
          .replace(/\s/g, ' ');
      if (text.length && /\s$/.test(text[text.length - 1])) {
        // trim leading space
        text = text.replace(/^ /, '');
      }
    }
    if (text) { out.push(text); }
  } else if (node.nodeType === 6/*ENTITY_NODE*/) {
    var value = node.nodeValue;
    if (value) { out.push(value); }
  } else if (node.nodeType === 1/*ELEMENT*/) {
    var nodeName = node.nodeName.toLowerCase();
    if (nodeName in HTML_TAGS_WITH_NON_HUMAN_READABLE_CONTENT) { return; }
    if (nodeName == 'br') {
      out.push('\n');
    } else {
      for (var child = node.firstChild; child; child = child.nextSibling) {
        handleTextContentHelper(child, globalProps, out);
      }
    }
  }
}

/**
 * extracts an email line by looking at the text.
 * If the text looks like a valid email or a mailto: url
 * (an RFC 822 ADDR-SPEC), it will return it, otherwise it will return null.
 * @param {Node} node.
 * @return {ContentLine|null}
 */
function handleTextEmailContent(node, globalProps) {
  var cl = handleTextContent(node, globalProps);
  if (!cl) { return null; }

  var value = cl.getValues()[0];
  value = value.replace(/^\s+|\s+$/g, '');
  if (!/.@\w+(\.\w+)+?$/.test(value)) { return null; }

  // make sure it's mailto, converting to a mailto: if not a url
  var protocol = value.match(/^([\w-]+):/);
  if (protocol) {
    if (protocol[1].toLowerCase() !== 'mailto') { return null; }
  } else {
    cl.setValue(0, 'MAILTO:' + encodeURI(value));
  }
  cl.setAttributes([]);
  return cl;
}
var textEmailContentHandler = { handle: handleTextEmailContent };

/**
 * returns a content handler that finds the first instance of the named node
 * that has the named attribute, and yields the attributes value.
 * @param {string} nodeName an HTML4 or XHTML node name
 * @param {string} attribName an HTML4 or XHTML attribute name
 * @return {Object} a content handler that maps dom nodes to ContentLines.
 */
function makeAttribContentHandler(nodeName, attribName) {
  return {
        handle: function (node, globalProps) {
          var els = node.getElementsByTagName(nodeName);
          var values = [];
          for (var i = 0; i < els.length; ++i) {
            var attrib = node.getAttribute(attribName);
            if (attrib != null) {
              values.push(attrib);
            }
          }
          if (values.length) {
            var content = new ContentLine();
            content.pushValues.apply(content, values);
            return content;
          }
        }
      };
}

/**
 * given a link, returns a content-line whose value is the url.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleUrlContent(node, globalProps) {
  var attrValue = null;
  switch (node.nodeName.toLowerCase()) {
    case 'a': case 'area':
      // TODO(mikesamuel): resolve the href attrib relative to globalProps.url?
      attrValue = node.href  // correctly resolved relative to this document
        || node.getAttribute('href');
      break;
    case 'img':
      // TODO(mikesamuel): resolve the href attrib relative to globalProps.url?
      attrValue = node.src  // correctly resolved relative to this document
        || node.getAttribute('src');
      break;
    case 'object':
      attrValue = node.data  // correctly resolved relative to this document
        || node.getAttribute('data');
      break;
      // TODO(mikesamuel): other url node/attrib pairs?
  }
  if (!attrValue) { return null; }
  var content = new ContentLine();
  content.pushValues(attrValue);

  var type = node.getAttribute('type');
  if (type) {
    content.pushAttributes('FMTTYPE', type);
  }
  return content;
}
var urlContentHandler = { handle: handleUrlContent };

/**
 * A simple html sanitizer that only preserves a few formatting elements
 * in event summaries and descriptions.
 */
var sanitizeHtml = (function () {
  var allowedTags = { 'p': true, 'b': true, 'i': true, 'u': true, 'br': true,
                      'blockquote': true, 'address': true, 'ul': true,
                      'ol': true, 'li': true, 'sub': true, 'sup': true };
  var sanitize = html.makeHtmlSanitizer(
      function (tagName, attribs) {
        if (tagName in allowedTags) {
          attribs.length = 0;
          return attribs;
        } else {
          return null;
        }
      });
  return function (html) {
    var out = [];
    sanitize(html, out);
    return out.join('');
  };
})();

/**
 * like {@link #handleTextContent the text html handler}, but
 * returns html with questionable tags stripped.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleHtmlContent(node, globalProps) {
  var content = checkAttributesForText(node, globalProps);
  if (content) {
    content.setValue(
        0,
        (content.getValues()[0]
         .replace(/&/g, '&amp;').replace(/</g, '&lt;')
         .replace(/>/g, '&gt;').replace(/\"/g, '&quot;')));
    return content;
  }

  var html = sanitizeHtml(node.innerHTML)
      .replace(/[ \t\r\n]+/g, ' ').replace(/^\s+|\s+$/g, '');
  if (!html) { return null; }
  content = new ContentLine();
  content.pushValues(html);
  var language = node.getAttribute('xml:lang') || globalProps.language;
  if (language) {
    content.pushAttributes('LANGUAGE', language);
  }
  return content;
}
var htmlContentHandler = { handle: handleHtmlContent };

function handleDateContent(node, globalProps) {
  var value = handleTextContent(node, globalProps);
  if (!value) { return null; }
  return convertDate(value.getValues()[0]);
}
var dateContentHandler = { handle: handleDateContent };

/**
 * parse RDATE values -- a comma separated list of dates, date-times, or
 * periods.
 */
function handleRdateContent(node, globalProps) {
  var line = handleTextContent(node, globalProps);
  if (!line) { return null; }
  var parts = line.getValues()[0].split(/,/g);
  var resultType = null;
  var results = [];
  for (var i = 0; i < parts.length; ++i) {
    var text = parts[i];
    var value = null;
    var type = null;
    var slash = text.indexOf('/');
    if (slash < 0) {
      var line = convertDate(text);
      type = line.getAttribute('VALUE');
      value = line.getValues()[0];
    } else {
      var start = convertDate(text.substring(0, slash));
      var end = convertDate(text.substring(slash + 1));
      if (start && end &&
          start.getAttribute('VALUE') === end.getAttribute('VALUE')) {
        value = start.getValues()[0] + '/' + end.getValues()[0];
        type = 'PERIOD';
      }
    }
    if (!resultType) {
      resultType = type;
    } else if (type !== resultType) {
      return null;
    }
    results.push(value);
  }
  if (!results.length) { return null; }
  var cl = new ContentLine();
  cl.pushValues.apply(cl, results);
  cl.pushAttributes('VALUE', resultType);
  return cl;
}
var rDateContentHandler = { handle: handleRdateContent };

/**
 * returns a content handler that yields the title of the given node iff
 * it matches the given pattern.
 * @param {RegExp} pattern a regular expression
 * @return {Object} a handler that maps dom nodes to ContentLines.
 */
function makeValidatingTextContentHandler(pattern, xform) {
  return {
        handle: function (node, globalProps) {
          var contentLine = handleTextContent(node, globalProps);
          if (!contentLine) { return null; }
          var values = contentLine.getValues();
          if (!pattern.test(values[0])) { return null; }
          contentLine.setAttributes([]);
          if (xform) {
            contentLine.setValue(0, xform(values[0]));
          }
          return contentLine;
        },
        noDescend: false
      };
}

/**
 * a content handler that parses a nested hcard.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleHcardContent(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, HCARD_SCHEMA, globalProps, contentLines);
  if (!contentLines.length) {
    return null;
  }
  var name = null;
  var email = null;
  var other = [];
  for (var i = 0; i < contentLines.length; ++i) {
    var cl = contentLines[i];
    var clName = cl.getName(), values = cl.getValues();
    if (!values.length || !clName) { continue; }
    switch (clName) {
      case 'FN': case 'N':
        name = values[0];
        break;
      case 'EMAIL':
        if (!email) { email = values[0]; }
        break;
      default:
        for (var j = 0; j < values.length; ++j) {
          var attribName = clName;
          if (attribName !== 'ROLE') {
            attribName = 'X-HCARD-' + attribName;
          }
          other.push(attribName, values[j]);
        }
        break;
    }
  }
  if (!email) { return null; }
  var outputContentLine = new ContentLine();
  outputContentLine.pushValues(email);
  if (name) {
    outputContentLine.pushAttributes('CN', name);
  }
  outputContentLine.pushAttributes.apply(outputContentLine, other);
  return outputContentLine;
}
/**
 * do not allow the containing instance to infer elements from the hcard.
 */
var hcardContentHandler = { handle: handleHcardContent, noDescend: true };

/**
 * parses a participant using an undocumented format that appears in the
 * unittests.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleParticipantContent(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, PARTICIPANT_SCHEMA, globalProps, contentLines);
  if (!contentLines.length) { return null; }
  var groupings = { 'DELEGATED-FROM': 'DELEGATED-FROM',
                    'DELEGATED-TO': 'DELEGATED-TO' };
  group(contentLines, groupings);
  var value = null;
  var attribs = [];
  for (var i = 0; i < contentLines.length; ++i) {
    var cl = contentLines[i];
    var name = cl.getName(), values = cl.getValues();
    if (!values.length || !name) { continue; }
    if (name === 'VALUE') {
      value = values[0];
    } else {
      attribs.push(name, values[0]);
    }
  }
  if (!value) { return null; }
  var contentLine = new ContentLine();
  contentLine.pushValues(value);
  contentLine.setAttributes(attribs);
  return contentLine;
}
/**
 * do not allow the containing instance to infer elements from the hcard.
 */
var participantContentHandler = {
    handle: handleParticipantContent, noDescend: true };

/**
 * a content handler that parses a nested telephone number.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleTelContent(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, TEL_SCHEMA, globalProps, contentLines);
  if (!contentLines.length) {
    return null;
  }
  var type = null;
  var value = null;
  for (var i = 0; i < contentLines.length; ++i) {
    var cl = contentLines[i];
    var values = cl.getValues(), name = cl.getName();
    if (!values.length || !name) { continue; }
    switch (name) {
      case 'TYPE':
        type = values[0];
        break;
      case 'VALUE':
        value = values[0];
        break;
    }
  }
  if (!(type && value)) { return null; }
  var outputContentLine = new ContentLine();
  outputContentLine.name.pushAttributes('TYPE', type);
  outputContentLine.name.pushValues(value);
  return outputContentLine;
}
var telContentHandler = { handle: handleTelContent, noDescend: true };

/**
 * a content handler that parses a nested geographic-location.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleGeoContent(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, GEO_SCHEMA, globalProps, contentLines);
  if (!contentLines.length) {
    return null;
  }
  var latitude = null;
  var longitude = null;
  for (var i = 0; i < contentLines.length; ++i) {
    var cl = contentLines[i];
    var name = cl.getName(), values = cl.getValues();
    if (!values.length || !name) { continue; }
    switch (name) {
      case 'LATITUDE':
        latitude = values[0];
        break;
      case 'LONGITUDE':
        longitude = values[0];
        break;
    }
  }
  if (!(latitude && longitude)) { return null; }
  var outputContentLine = new ContentLine();
  outputContentLine.name.pushValues(latitude, longitude);
  return outputContentLine;
}
var geoContentHandler = { handle: handleGeoContent, noDescend: true };

/**
 * a content handler that parses a nested physical address.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleAdrContent(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, ADR_SCHEMA, globalProps, contentLines);
  if (!contentLines.length) {
    return null;
  }
  var parts = {};
  for (var i = 0; i < contentLines.length; ++i) {
    var cl = contentLines[i];
    var name = cl.getName();
    if (!name) { continue; }
    var values = cl.getValues();
    if (!values.length) { continue; }
    parts[name.toLowerCase()] = values[0];
  }
  var formatted = [];
  var partsInOrder = [ 'street-address', 'extended-address',
                       'post-office-box', 'locality', 'region',
                       'postal-code', 'country-name' ];
  for (var i = 0; i < partsInOrder.length; ++i) {
    var part = partsInOrder[i];
    if (part in parts) {
      formatted.push(parts[part]);
    }
  }
  if (!formatted.length) { return null; }

  var outputContentLine = new ContentLine();
  outputContentLine.pushValues(formatted.join('\n'));
  return outputContentLine;
}
var adrContentHandler = { handle: handleAdrContent, noDescend: true };

/**
 * a content handler that parses a nested rrule.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleRruleContent(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, RRULE_SCHEMA, globalProps, contentLines);

  var freq = null;
  for (var i = contentLines.length; --i >= 0;) {
    if ('FREQ' === contentLines[i].getName()) {
      freq = contentLines[i];
      contentLines.splice(i, 1);
      break;
    }
  }

  group(contentLines, { BYSECOND: 'BYSECOND',
                        BYMINUTE: 'BYMINUTE',
                        BYHOUR: 'BYHOUR',
                        BYDAY: 'BYDAY',
                        BYMONTHDAY: 'BYMONTHDAY',
                        BYYEARDAY: 'BYYEARDAY',
                        BYWEEKNO: 'BYWEEKNO',
                        BYMONTH: 'BYMONTH',
                        BYSETPOS: 'BYSETPOS' });
  var values = ['FREQ=' + freq.getValues()[0]];
  for (var i = 0; i < contentLines.length; ++i) {
    var contentLine = contentLines[i];
    values.push(
        contentLine.getName() + '=' + contentLine.getValues().join(','));
  }
  var contentLine = new ContentLine();
  contentLine.pushValues(values.join(';'));
  contentLine.setNoEscape(true);
  return contentLine;
}
var rruleContentHandler = { handle: handleRruleContent, noDescend: true };

/** blocks descent */
function handleNoDescentContent(node, globalProps) {
  return NO_CONTENT;
}
var noDescentContentHandler = {
    handle: handleNoDescentContent, noDescend: true };


function toUpper(s) { return s.toUpperCase(); }



// Microformat Schemas
/** defines vcalendar level properties. */
var VCALENDAR_SCHEMA = {
  'method': [textContentHandler],
  'vevent': [noDescentContentHandler]
};

/** @see http://microformats.org/wiki/hcalendar */
var VEVENT_SCHEMA = {
  'uid': [urlContentHandler,
          textContentHandler],
  'url': [urlContentHandler],
  'attach': [urlContentHandler],
  'description': [htmlContentHandler],
  'summary': [htmlContentHandler],
  'class': [makeValidatingTextContentHandler(CLASS_RE, toUpper)],
  'dtstart': [dateContentHandler],
  'dtend': [dateContentHandler],
  'dtstamp': [dateContentHandler],
  'duration': [makeValidatingTextContentHandler(PERIOD_RE)],
  'rrule': [makeValidatingTextContentHandler(RRULE_RE, toUpper),
            rruleContentHandler],
  'exrule': [makeValidatingTextContentHandler(RRULE_RE, toUpper),
             rruleContentHandler],
  'rdate': [rDateContentHandler],
  'exdate': [rDateContentHandler],
  'location': [hcardContentHandler,
               adrContentHandler,
               textContentHandler],
  'attendee': [hcardContentHandler,
               participantContentHandler,
               textEmailContentHandler],
  'contact': [hcardContentHandler,
              participantContentHandler,
              textEmailContentHandler],
  'organizer': [hcardContentHandler,
                participantContentHandler,
                textEmailContentHandler],
  'category': [textContentHandler],
  'sequence': [makeValidatingTextContentHandler(NON_NEG_INT_RE)],
  'status': [makeValidatingTextContentHandler(STATUS_RE, toUpper)],
  'transp': [makeValidatingTextContentHandler(TRANSP_RE, toUpper)],
  'del': [noDescentContentHandler]  // indicates deleted content
};

/** see RFC 2445 section 4.6.4 for the meaning of these. */
var RRULE_SCHEMA = {
  'freq': [makeValidatingTextContentHandler(
              /^(?:YEAR|MONTH|DAI|WEEK|HOUR|MINUTE|SECOND)LY$/i, toUpper)],
  'until': [dateContentHandler],
  'interval': [makeValidatingTextContentHandler(POS_INT_RE)],
  'count': [makeValidatingTextContentHandler(NON_NEG_INT_RE)],
  'bysecond': [makeValidatingTextContentHandler(NON_NEG_INT_RE)],
  'byminute': [makeValidatingTextContentHandler(NON_NEG_INT_RE)],
  'byhour': [makeValidatingTextContentHandler(NON_NEG_INT_RE)],
  'byday': [makeValidatingTextContentHandler(BYDAY_RE, toUpper)],
  'bymonth': [makeValidatingTextContentHandler(INT_RE)],
  'bymonthday': [makeValidatingTextContentHandler(NON_ZERO_INT_RE)],
  'byyearday': [makeValidatingTextContentHandler(NON_ZERO_INT_RE)],
  'byweekno': [makeValidatingTextContentHandler(NON_ZERO_INT_RE)],
  'bysetpos': [makeValidatingTextContentHandler(NON_ZERO_INT_RE)],
  'wkst': [makeValidatingTextContentHandler(WDAY_RE, toUpper)]
};

/** @see http://microformats.org/wiki/hcard */
var HCARD_SCHEMA = {
  'url': [urlContentHandler],
  'email': [urlContentHandler,
            textEmailContentHandler],
  'photo': [makeAttribContentHandler('img', 'src')],
  'uid': [urlContentHandler,
          textContentHandler],
  'location': [geoContentHandler],
  'bday': [dateContentHandler],
  'tel': [telContentHandler],
  'fn': [textContentHandler],
  'n': [textContentHandler],
  'org': [textContentHandler],
  'adr': [adrContentHandler],
  'title': [textContentHandler],
  'role': [textContentHandler],
  'org': [textContentHandler]
};

/** this schema is not in the documentation, but appears in the testcases */
var PARTICIPANT_SCHEMA = {
  'value': [urlContentHandler],
  'cn': [textContentHandler],
  'cutype': [makeValidatingTextContentHandler(CUTYPE_RE, toUpper)],
  'delegated-from': [urlContentHandler,
                     makeValidatingTextContentHandler(MAILTO_RE)],
  'delegated-to': [urlContentHandler,
                   makeValidatingTextContentHandler(MAILTO_RE)],
  'dir': [urlContentHandler],
  'member': [urlContentHandler],
  'partstat': [makeValidatingTextContentHandler(PSTAT_RE, toUpper)],
  'role': [makeValidatingTextContentHandler(ROLE_RE, toUpper)],
  'rsvp': [makeValidatingTextContentHandler(RSVP_RE, toUpper)],
  'sent-by': [urlContentHandler]
};

/** a telephone number */
var TEL_SCHEMA = {
  'type': [textContentHandler],
  'value': [textContentHandler]
};

/** a geograhic location.  @see http://microformats.org/wiki/geo */
var GEO_SCHEMA = {
  'latitude': [textContentHandler],
  'longitude': [textContentHandler]
};

/** a physical address.  @see http://microformats.org/wiki/adr */
var ADR_SCHEMA = {
  'street-address': [textContentHandler],
  'extended-address': [textContentHandler],
  'post-office-box': [textContentHandler],
  'locality': [textContentHandler],
  'region': [textContentHandler],
  'postal-code': [textContentHandler],
  'country-name': [textContentHandler]
};
                             
})();
