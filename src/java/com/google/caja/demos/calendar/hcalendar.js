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
 * @author msamuel@google.com
 */


var ICAL_PROD_ID = '-//hcalendar.js v1.0//EN';
var extractHcal;

(function () {
/**
 * given a DOM node, finds all hcalendar objects underneath it.
 * @return {Array.<Array.<ContentLine>>} a list of lists of content lines.
 *   Each list of content lines is one VEVENT.
 */
function extractHcal(node) {
  var globalProps = {
    title: '$TITLE$',
    url: '$SOURCE$',
    language: null,
    method: 'PUBLISH',
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
  doExtract_(node, globalProps, events);
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
      titleLine.attributes_.push('LANGUAGE', globalProps.language);
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
}

/**
 * finds and processes top level entities.  This handles both VEVENTs and
 * VCALENDARs since VEVENTs are not typically grouped inside a VCALENDAR.
 * If a VCALENDAR is found, it is used to populate globalProps.
 * @param {Node} node root of the subtree to examine
 * @param {Object} globalProps calendar properties.  may be modified
 * @param {Array.<Array.<ContentLine>>} output array of events to append to.
 */
function doExtract_(node, globalProps, events) {
  if (node.nodeType !== 1/*ELEMENT_NODE*/) { return; }
  if (VEVENT_CLASS_(node)) {
    var contentLines = [];
    parseMicroFormat(node, VEVENT_SCHEMA_, globalProps, contentLines);
    events.push(contentLines);
  } else {
    if (VCALENDAR_CLASS_(node)) {
      // if we see a vcalendar class, then we should fill globalProps
      var calLines = [];
      parseMicroFormat(node, VCALENDAR_SCHEMA_, globalProps, calLines);
      for (var i = calLines.length; --i >= 0;) {
        var line = calLines[i];
        var name = line.name_.toLowerCase();
        if (name in globalProps) {
          globalProps[name] = line.values_[0];
        }
      }
    }
    for (var child = node.firstChild; child; child = child.nextSibling) {
      doExtract_(child, globalProps, events);
    }
  }
}


// Schema Predicates
var VEVENT_CLASS_ = classMatcher_(['vevent']);
var VCALENDAR_CLASS_ = classMatcher_(['vcalendar']);
var DUR_TIME_ = '(T(\\d+H(\\d+M(\\d+S)?)?|\\d+M(\\d+S)?|\\d+S))';
var PERIOD_RE_ = new RegExp(
    '^[+-]?P(\\d+[WD]' + DUR_TIME_ + '?|' + DUR_TIME_ + ')$');
var RRULE_RE_ = new RegExp(
    '^FREQ=(YEAR|MONTH|DAI|WEEK|HOUR|MINUTE|SECOND)LY(;[A-Z0-9;,]+)?$', 'i');
var NON_NEG_INT_RE_ = new RegExp('^\\\d+$');
var POS_INT_RE_ = new RegExp('^(0+|0*[1-9]\\d+)$');
var INT_RE_ = new RegExp('^[+-]?(0+|0*[1-9]\\d+)$');
var NON_ZERO_INT_RE_ = new RegExp('^[+-]?0*[1-9]\\d+$');
var WDAY_RE_ = new RegExp('^(MO|TU|WE|TH|FR|SA|SU)$', 'i');
var BYDAY_RE_ = new RegExp('^([+-]0*[1-9]\\d+)?(MO|TU|WE|TH|FR|SA|SU)$', 'i');
var STATUS_RE_ = new RegExp('^(?:TENTATIVE|CONFIRMED|CANCELLED)$', 'i');
var CLASS_RE_ = new RegExp('^(?:PUBLIC|PRIVATE|CONFIDENTIAL)$', 'i');
var TRANSP_RE_ = new RegExp('^(?:TRANSPARENT|OPAQUE)$', 'i');
var ROLE_RE_ = new RegExp('^(?:CHAIR|(?:REQ|OPT|NON)-PARTICIPANT)$', 'i');
var RSVP_RE_ = new RegExp('^(?:TRUE|FALSE)$', 'i');
var PSTAT_RE_ = new RegExp(
    '^(?:NEEDS-ACTION|ACCEPTED|DECLINED|TENTATIVE|DELEGATED)$', 'i');
var CUTYPE_RE_ = new RegExp(
    '^(?:INDIVIDUAL|GROUP|RESOURCE|ROOM|UNKNOWN)$', 'i');
var MAILTO_RE_ = new RegExp('^mailto:', 'i');

/** defines vcalendar level properties. */
var VCALENDAR_SCHEMA_ = {
  'method': [handleTextContent_],
  'vevent': [handleNoDescentContent_],
};

/** @see http://microformats.org/wiki/hcalendar */
var VEVENT_SCHEMA_ = {
  'uid': [handleUrlContent_,
          handleTextContent_],
  'url': [handleUrlContent_],
  'attach': [handleUrlContent_],
  'description': [handleHtmlContent_],
  'summary': [handleHtmlContent_],
  'class': [handleValidatingtextContent_(CLASS_RE_, toUpper)],
  'dtstart': [handleDateContent_],
  'dtend': [handleDateContent_],
  'dtstamp': [handleDateContent_],
  'duration': [handleValidatingtextContent_(PERIOD_RE_)],
  'rrule': [handleValidatingtextContent_(RRULE_RE_, toUpper),
            handleRruleContent_],
  'exrule': [handleValidatingtextContent_(RRULE_RE_, toUpper),
             handleRruleContent_],
  'rdate': [handleRdateContent_],
  'exdate': [handleRdateContent_],
  'location': [handleHcardContent_,
               handleAdrContent_,
               handleTextContent_],
  'attendee': [handleHcardContent_,
               handleParticipantContent_,
               handleTextEmailContent_],
  'contact': [handleHcardContent_,
              handleParticipantContent_,
              handleTextEmailContent_],
  'organizer': [handleHcardContent_,
                handleParticipantContent_,
                handleTextEmailContent_],
  'category': [handleTextContent_],
  'sequence': [handleValidatingtextContent_(NON_NEG_INT_RE_)],
  'status': [handleValidatingtextContent_(STATUS_RE_, toUpper)],
  'transp': [handleValidatingtextContent_(TRANSP_RE_, toUpper)],
  'del': [handleNoDescentContent_]  // indicates deleted content
};

/** see RFC 2445 section 4.6.4 for the meaning of these. */
var RRULE_SCHEMA_ = {
  'freq': [handleValidatingtextContent_(
              /^(?:YEAR|MONTH|DAI|WEEK|HOUR|MINUTE|SECOND)LY$/i, toUpper)],
  'until': [handleDateContent_],
  'interval': [handleValidatingtextContent_(POS_INT_RE_)],
  'count': [handleValidatingtextContent_(NON_NEG_INT_RE_)],
  'bysecond': [handleValidatingtextContent_(NON_NEG_INT_RE_)],
  'byminute': [handleValidatingtextContent_(NON_NEG_INT_RE_)],
  'byhour': [handleValidatingtextContent_(NON_NEG_INT_RE_)],
  'byday': [handleValidatingtextContent_(BYDAY_RE_, toUpper)],
  'bymonth': [handleValidatingtextContent_(INT_RE_)],
  'bymonthday': [handleValidatingtextContent_(NON_ZERO_INT_RE_)],
  'byyearday': [handleValidatingtextContent_(NON_ZERO_INT_RE_)],
  'byweekno': [handleValidatingtextContent_(NON_ZERO_INT_RE_)],
  'bysetpos': [handleValidatingtextContent_(NON_ZERO_INT_RE_)],
  'wkst': [handleValidatingtextContent_(WDAY_RE_, toUpper)]
};

/** @see http://microformats.org/wiki/hcard */
var HCARD_SCHEMA_ = {
  'url': [handleUrlContent_],
  'email': [handleUrlContent_,
            handleTextEmailContent_],
  'photo': [handleAttribContent_('img', 'src')],
  'uid': [handleUrlContent_,
          handleTextContent_],
  'location': [handleGeoContent_],
  'bday': [handleDateContent_],
  'tel': [handleTelContent_],
  'fn': [handleTextContent_],
  'n': [handleTextContent_],
  'org': [handleTextContent_],
  'adr': [handleAdrContent_],
  'title': [handleTextContent_],
  'role': [handleTextContent_],
  'org': [handleTextContent_]
};

/** this schema is not in the documentation, but appears in the testcases */
var PARTICIPANT_SCHEMA_ = {
  'value': [handleUrlContent_],
  'cn': [handleTextContent_],
  'cutype': [handleValidatingtextContent_(CUTYPE_RE_, toUpper)],
  'delegated-from': [handleUrlContent_,
                     handleValidatingtextContent_(MAILTO_RE_)],
  'delegated-to': [handleUrlContent_,
                   handleValidatingtextContent_(MAILTO_RE_)],
  'dir': [handleUrlContent_],
  'member': [handleUrlContent_],
  'partstat': [handleValidatingtextContent_(PSTAT_RE_, toUpper)],
  'role': [handleValidatingtextContent_(ROLE_RE_, toUpper)],
  'rsvp': [handleValidatingtextContent_(RSVP_RE_, toUpper)],
  'sent-by': [handleUrlContent_]
};

/** a telephone number */
var TEL_SCHEMA_ = {
  'type': [handleTextContent_],
  'value': [handleTextContent_]
};

/** a geograhic location.  @see http://microformats.org/wiki/geo */
GEO_SCHEMA_ = {
  'latitude': [handleTextContent_],
  'longitude': [handleTextContent_]
};

/** a physical address.  @see http://microformats.org/wiki/adr */
ADR_SCHEMA_ = {
  'street-address': [handleTextContent_],
  'extended-address': [handleTextContent_],
  'post-office-box': [handleTextContent_],
  'locality': [handleTextContent_],
  'region': [handleTextContent_],
  'postal-code': [handleTextContent_],
  'country-name': [handleTextContent_]
};


// Schema content handlers

/**
 * concatenates all text nodes under node together to produce the value.
 * @param {Node} node.
 * @param {Object} globalProps
 * @return {ContentLine|null}
 */
function handleTextContent_(node, globalProps) {
  var content = checkAttributesForText_(node, globalProps);
  if (content) { return content; }

  // Otherwise, concatenate all the text nodes.
  var text = [];
  handleTextContentHelper_(node, globalProps, text);
  if (!text.length) { return null; }
  text = text.join('').replace(/ $/gm, '');
  if (!text) { return null; }
  content = new ContentLine();
  content.values_.push(text);
  var language = node.getAttribute('xml:lang') || globalProps.language;
  if (language) {
    content.attributes_.push('LANGUAGE', language);
  }
  return content;
}

/**
 * handles tags that store their textual content in attributes.
 * @param {Element} node
 * @param {Object} globalProps
 * @return {ContentLine}
 */
function checkAttributesForText_(node, globalProps) {
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
  content.values_.push(attribValue);
  var language = node.getAttribute('xml:lang') || globalProps.language;
  if (language) {
    content.attributes_.push('LANGUAGE', language);
  }
  return content;
}

/**
 * set of HTML 4.0 tag names that contain cdata do not contain human readable
 * content.
 */
var HTML_TAGS_WITH_NON_HUMAN_READABLE_CONTENT_ = {
  script: true,
  style: true
};

/**
 * walk the DOM BFS and concatenate all text nodes.
 * After this is called, out will contain the values of text nodes, as they
 * appear to an xml reader.
 * @private
 */
function handleTextContentHelper_(node, globalProps, out) {
  if (node.nodeType === 3/*TEXT_NODE*/ ||
      node.nodeType === 4/*CDATA_SECTION_NODE*/) {
    var text = node.nodeValue;
    // if this is not preformatted, then collapse runs of whitespace.
    if (!isPreformatted(node.parentNode)) {
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
    if (nodeName in HTML_TAGS_WITH_NON_HUMAN_READABLE_CONTENT_) { return; }
    if (nodeName == 'br') {
      out.push('\n');
    } else {
      for (var child = node.firstChild; child; child = child.nextSibling) {
        handleTextContentHelper_(child, globalProps, out);
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
function handleTextEmailContent_(node, globalProps) {
  var cl = handleTextContent_(node, globalProps);
  if (!cl) { return null; }

  var value = cl.values_[0];
  value = value.replace(/^\s+|\s+$/g, '');
  if (!/.@\w+(\.\w+)+?$/.test(value)) { return null; }

  // make sure it's mailto, converting to a mailto: if not a url
  var protocol = value.match(/^([\w-]+):/);
  if (protocol) {
    if (protocol[1].toLowerCase() !== 'mailto') { return null; }
  } else {
    cl.values_[0] = 'MAILTO:' + encodeURI(value);
  }
  cl.attributes_ = [];
  return cl;
}

/**
 * returns a content handler that finds the first instance of the named node
 * that has the named attribute, and yields the attributes value.
 * @param {string} nodeName an HTML4 or XHTML node name
 * @param {string} attribName an HTML4 or XHTML attribute name
 * @return {Function} a function that maps dom nodes to ContentLines.
 */
function handleAttribContent_(nodeName, attribName) {
  return function (node, globalProps) {
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
      content.values_ = out;
      return content;
    }
  };
}

/**
 * given a link, returns a content-line whose value is the url.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleUrlContent_(node, globalProps) {
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
  content.values_.push(attrValue);

  var type = node.getAttribute('type');
  if (type) {
    content.attributes_.push('FMTTYPE', type);
  }
  return content;
}

/**
 * like {@link #handleTextContent_ the text html handler}, but
 * returns html with questionable tags stripped.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleHtmlContent_(node, globalProps) {
  var content = checkAttributesForText_(node, globalProps);
  if (content) {
    content.values_[0] = content.values_[0]
      .replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/\"/g, '&quot;');
    return content;
  }

  var html = sanitizeHtml(node.innerHTML)
      .replace(/[ \t\r\n]+/g, ' ').replace(/^\s+|\s+$/g, '');
  if (!html) { return null; }
  content = new ContentLine();
  content.values_.push(html);
  var language = node.getAttribute('xml:lang') || globalProps.language;
  if (language) {
    content.attributes_.push('LANGUAGE', language);
  }
  return content;
}

function handleDateContent_(node, globalProps) {
  var value = handleTextContent_(node, globalProps);
  if (!value) { return null; }
  return convertDate_(value.values_[0]);
}

/**
 * parse RDATE values -- a comma separated list of dates, date-times, or
 * periods.
 */
function handleRdateContent_(node, globalProps) {
  var line = handleTextContent_(node, globalProps);
  if (!line) { return null; }
  var parts = line.values_[0].split(/,/g);
  var resultType = null;
  var results = [];
  for (var i = 0; i < parts.length; ++i) {
    var text = parts[i];
    var value = null;
    var type = null;
    var slash = text.indexOf('/');
    if (slash < 0) {
      var line = convertDate_(text);
      type = line.getAttribute_('VALUE');
      value = line.values_[0];
    } else {
      var start = convertDate_(text.substring(0, slash));
      var end = convertDate_(text.substring(slash + 1));
      if (start && end &&
          start.getAttribute_('VALUE') === end.getAttribute_('VALUE')) {
        value = start.values_[0] + '/' + end.values_[0];
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
  cl.values_ = results;
  cl.attributes_.push('VALUE', resultType);
  return cl;
}

/**
 * returns a content handler that yields the title of the given node iff
 * it matches the given pattern.
 * @param {RegExp} pattern a regular expression
 * @return {Function} a function that maps dom nodes to ContentLines.
 */
function handleValidatingtextContent_(pattern, xform) {
  return function (node, globalProps) {
    var contentLine = handleTextContent_(node, globalProps);
    if (!(contentLine && pattern.test(contentLine.values_[0]))) {
      return null;
    }
    contentLine.attributes_ = [];
    if (xform) {
      contentLine.values_[0] = xform(contentLine.values_[0]);
    }
    return contentLine;
  };
}

/**
 * a content handler that parses a nested hcard.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleHcardContent_(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, HCARD_SCHEMA_, globalProps, contentLines);
  if (!contentLines.length) {
    return null;
  }
  var name = null;
  var email = null;
  var other = [];
  for (var i = 0; i < contentLines.length; ++i) {
    var cl = contentLines[i];
    if (!cl.values_.length || !cl.name_) { continue; }
    switch (cl.name_) {
      case 'FN': case 'N':
        name = cl.values_[0];
        break;
      case 'EMAIL':
        if (!email) { email = cl.values_[0]; }
        break;
      default:
        for (var j = 0; j < cl.values_.length; ++j) {
          var attribName = cl.name_;
          if (attribName !== 'ROLE') {
            attribName = 'X-HCARD-' + attribName;
          }
          other.push(attribName, cl.values_[j]);
        }
        break;
    }
  }
  if (!email) { return null; }
  var outputContentLine = new ContentLine();
  outputContentLine.values_.push(email);
  if (name) {
    outputContentLine.attributes_.push('CN', name);
  }
  outputContentLine.attributes_.push.apply(
      outputContentLine.attributes_, other);
  return outputContentLine;
}

/**
 * do not allow the containing instance to infer elements from the hcard.
 */
handleHcardContent_.noDescend_ = true;

/**
 * parses a participant using an undocumented format that appears in the
 * unittests.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleParticipantContent_(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, PARTICIPANT_SCHEMA_, globalProps, contentLines);
  if (!contentLines.length) { return null; }
  var groupings = { 'DELEGATED-FROM': 'DELEGATED-FROM',
                    'DELEGATED-TO': 'DELEGATED-TO' };
  group(contentLines, groupings);
  var value = null;
  var attribs = [];
  for (var i = 0; i < contentLines.length; ++i) {
    var cl = contentLines[i];
    if (!cl.values_.length || !cl.name_) { continue; }
    if (cl.name_ == 'VALUE') {
      value = cl.values_[0];
    } else {
      attribs.push(cl.name_, cl.values_[0]);
    }
  }
  if (!value) { return null; }
  var contentLine = new ContentLine();
  contentLine.values_.push(value);
  contentLine.attributes_ = attribs;
  return contentLine;
}
/**
 * do not allow the containing instance to infer elements from the hcard.
 */
handleParticipantContent_.noDescend_ = true;

/**
 * a content handler that parses a nested telephone number.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleTelContent_(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, TEL_SCHEMA_, globalProps, contentLines);
  if (!contentLines.length) {
    return null;
  }
  var type = null;
  var value = null;
  for (var i = 0; i < contentLines.length; ++i) {
    var cl = contentLines[i];
    if (!cl.values_.length || !cl.name_) { continue; }
    switch (cl.name_) {
      case 'TYPE':
        type = cl.values_[0];
        break;
      case 'VALUE':
        value = cl.values_[0];
        break;
    }
  }
  if (!(type && value)) { return null; }
  var outputContentLine = new ContentLine();
  outputContentLine.name.attributes_.push('TYPE', type);
  outputContentLine.name.values_.push(value);
  return outputContentLine;
}
handleTelContent_.noDescend_ = true;

/**
 * a content handler that parses a nested geographic-location.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleGeoContent_(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, GEO_SCHEMA_, globalProps, contentLines);
  if (!contentLines.length) {
    return null;
  }
  var latitude = null;
  var longitude = null;
  for (var i = 0; i < contentLines.length; ++i) {
    var cl = contentLines[i];
    if (!cl.values_.length || !cl.name_) { continue; }
    switch (cl.name_) {
      case 'LATITUDE':
        latitude = cl.values_[0];
        break;
      case 'LONGITUDE':
        longitude = cl.values_[0];
        break;
    }
  }
  if (!(latitude && longitude)) { return null; }
  var outputContentLine = new ContentLine();
  outputContentLine.name.values_.push(latitude, longitude);
  return outputContentLine;
}
handleGeoContent_.noDescend_ = true;

/**
 * a content handler that parses a nested physical address.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleAdrContent_(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, ADR_SCHEMA_, globalProps, contentLines);
  if (!contentLines.length) {
    return null;
  }
  var parts = {};
  for (var i = 0; i < contentLines.length; ++i) {
    var cl = contentLines[i];
    if (!cl.values_.length || !cl.name_) { continue; }
    parts[cl.name_.toLowerCase()] = cl.values_[0];
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
  outputContentLine.values_.push(formatted.join('\n'));
  return outputContentLine;
}
handleAdrContent_.noDescend_ = true;

/**
 * a content handler that parses a nested rrule.
 * @param {DOMElement} node.
 * @return {ContentLine|null}
 */
function handleRruleContent_(node, globalProps) {
  var contentLines = [];
  parseMicroFormat(node, RRULE_SCHEMA_, globalProps, contentLines);

  var freq = null;
  for (var i = contentLines.length; --i >= 0;) {
    if ('FREQ' === contentLines[i].name_) {
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
  var values = ['FREQ=' + freq.values_[0]];
  for (var i = 0; i < contentLines.length; ++i) {
    var contentLine = contentLines[i];
    values.push(contentLine.name_ + '=' + contentLine.values_.join(','));
  }
  var contentLine = new ContentLine();
  contentLine.values_.push(values.join(';'));
  contentLine.noEscape_ = true;
  return contentLine;
}
handleRruleContent_.noDescend_ = true;

/** blocks descent */
function handleNoDescentContent_(node, globalProps) {
  return NO_CONTENT;
}
handleNoDescentContent_.noDescend_ = true;


function toUpper(s) { return s.toUpperCase(); }


// Export the public API
this.extractHcal = extractHcal;
})();
