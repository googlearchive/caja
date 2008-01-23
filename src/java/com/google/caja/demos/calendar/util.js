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
 * @fileoverview
 * Misc utility functions for dealing with DOM nodes and other structured data.
 *
 * @author msamuel@google.com
 */

/**
 * returns a function that given a DOM node, returns the first of the given
 * nmTokens that appears in it's class attribute, or null if none.
 * @return {string|null}
 */
function classMatcher_(nmTokens) {
  var regex = [' (?:'];
  for (var i = 0; i < nmTokens.length; ++i) {
    if (i) { regex.push('|'); }
    regex.push(nmTokens[i]);
  }
  regex.push(') ');
  regex = new RegExp(regex.join(''), 'ig');
  return function (node) {
    var names = (' ' + node.className.replace(/\s+/g, '  ') + ' ')
        .match(regex);
    if (!names) { return null; }
    for (var i = names.length; --i >= 0;) {
      var name = names[i];
      names[i] = name.substring(1, name.length - 1);
    }
    return names;
  };
}

/**
 * true if text content in the given element should be considered
 * preformatted.  E.g. <tt>&lt;pre&gt;</tt> and
 * <tt>&lt;div style="white-space:pre"&gt;</tt> both contain text where
 * whitespace is significant.
 *
 * @param {Element} element
 * @return {boolean}
 */
function isPreformatted(element) {
  var whitespaceStyle;
  if (element.currentStyle) {  // IE
    return element.currentStyle.whiteSpace;
  } else if (window.getComputedStyle) {  // FF
    whitespaceStyle = window.getComputedStyle(element, "").whiteSpace;
  } else {
    return element.tagName === 'pre';
  }
  return whitespaceStyle === 'pre';
}

/**
 * given a semi-structured date, convert it to an RFC2445 formatted
 * DATE or DATE-TIME value, returning null if the input is too malformed.
 *
 * @param {string} structuredDate
 * @return {string|null} null if unrecognizable.
 */
function convertDate_(structuredDate) {
  structuredDate = structuredDate.replace(/^\s+|\s+$/g, '');
  // don't assume too much about the date format
  // match dates like
  //   20060105
  //   2006-01-05
  // date times like
  //   20060105T155700
  //   20060105T155700Z
  //   2006-01-05T15:57:00Z
  // or with an offset like
  //   2006-01-05T15:57:00Z-05:30

  // Is it UTC?  if it ends with a Z, then it is.
  // We do this before checking for a timezone offset since the two are
  // exclusive.
  var isUtc = /Z$/.test(structuredDate);

  // Split off any timezone offset
  var offset = null;
  var m = structuredDate.match(/[+-]\d{1,2}:?[0-5][0-9]$/);
  if (m) {
    if ((typeof m) !== 'string') { m = m[0]; }
    offset = m;
    structuredDate = structuredDate.substring(
        0, structuredDate.length - m.length);
  }
  // Split the date and time portions
  var m = structuredDate.match(/^([^\sT]+)(?:[\sT]+([^\sT][\s\S]*))?$/);
  if (!m) { return; }
  var chunks = [];
  for (var i = m.length; --i >= 1;) { chunks[i - 1] = m[i] || ''; }
  if (chunks.length == 1) { chunks.push(''); }

  // Strip any fractional seconds from the end of the time portion.
  if (chunks[1]) {
    chunks[1] = chunks[1].replace(/\.\d+$/, '');
  }

  // Add the timezone offset chunk back.
  chunks.push(offset || '');
  for (var i = 0; i < chunks.length; ++i) {
    var isNegative = /^-/.test(chunks[i]);  // allow negative year
    // Wherever there is a run odd number of digits, make it even by padding
    // with zeros
    // E.g. 2005-9-2 1:00:00 -> 2005-09-02 01:00:00.
    // This is safe because we've already eliminated any decimal points
    chunks[i] = chunks[i].replace(/\b([0-9](?:[0-9][0-9])*)\b/g, '0$1');
    // Throw out any non numeric characters.
    chunks[i] = (isNegative ? '-' : '') + chunks[i].replace(/[^\d]+/g, '');
  }

  var dateParts = chunks[0].match(
      /^(-?[0-9]{4,})(1[0-2]|0[1-9])(3[01]|[12][0-9]|0[1-9])$/);
  if (!dateParts) { return null; }
  var timeParts = null;
  var zoneParts = null;
  if (chunks[1]) {
    timeParts = chunks[1].match(
        /^(2[0-3]|[01][0-9]|[0-9])([0-5][0-9])([0-5][0-9])?$/);
    if (!timeParts) { return null; }

    if (chunks[2]) {
      zoneParts = chunks[2].match(/^(-)?(\d{1,2})([0-5][0-9])$/);
      if (!zoneParts) { return null; }
    }
  }

  // split parts
  var year = Number(dateParts[1]);
  var month = Number(dateParts[2]);
  var day = Number(dateParts[3]);

  // reconstitute it as an RFC2445 date or date-time
  var content = new ContentLine();
  if (timeParts) {
    var hour = Number(timeParts[1]);
    var minute = Number(timeParts[2]);
    var second = Number(timeParts[3]) || 0;

    if (zoneParts) {
      var sign = zoneParts[1] == '-' ? -1 : 1;
      hour -= zoneParts[2] * sign;
      minute -= zoneParts[3] * sign;
      isUtc = true;

      // normalize the date
      var d = new Date(Date.UTC(year, month - 1, day) +
                       (((hour * 60) + minute) * 60 + second) * 1000);
      year = d.getUTCFullYear();
      month = d.getUTCMonth() + 1;
      day = d.getUTCDate();
      hour = d.getUTCHours();
      minute = d.getUTCMinutes();
      second = d.getUTCSeconds();
    }

    content.attributes_.push('VALUE', 'DATE-TIME');
    content.values_.push(pad_(year, 4) + pad_(month, 2) + pad_(day, 2) +
                         'T' + pad_(hour, 2) + pad_(minute, 2) +
                         pad_(second, 2) + (isUtc ? 'Z' : ''));
  } else {
    content.attributes_.push('VALUE', 'DATE');
    content.values_.push(pad_(year, 4) + pad_(month, 2) + pad_(day, 2));
  }
  return content;
}

/**
 * format a number to contain at least a certain number of digits by padding
 * with zeros.
 */
function pad_(n, digits) {
  var s = String(n);
  var delta = digits - s.length;
  while (delta > 0) {
    var ceil = delta;
    if (ceil > 16) { ceil = 16; }
    s = '0000000000000000'.substring(0, ceil) + s;
    delta -= ceil;
  }
  return s;
}
