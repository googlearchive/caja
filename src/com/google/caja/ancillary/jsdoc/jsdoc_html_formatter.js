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
 * Tools for generating portions of JSDoc HTML from JSON extracted by
 * com.google.caja.ancillary.jsdoc.Jsdoc.
 * <p>
 * This assumes that this code will run in an environment where:
 * <ul>
 * <li>A function {@code linkToApiElement(apiElementName)} is defined which
 *   returns a record like {@code { href: 'foo.html', target: '_self' }}
 *   such that a link with that URL and target would navigate to
 *   a page containing the {@code renderApiElement} content for that API
 *   element.
 * <li>A function {@code linkToFile(path)} is defined which
 *   returns a record like {@code { href: 'foo.html', target: '_self' }}
 *   such that a link with that URL and target would navigate to
 *   a page containing the {@code renderFile} content for that API
 *   element.
 * <li>A function {@code linkToSource(path, lineNumber)} is defined which
 *   returns a record like {@code { href: 'foo.html', target: '_self' }}
 *   such that a link with that URL and target would navigate to a page
 *   containing the source code for the specified input file at the given 
 *   line number.
 * <li>A function {@code getJsdocRoot()} returns the JSON documentation tree.
 * </ul>
 *
 * @author mikesamuel@gmail.com
 * @require getJsdocRoot, linkToApiElement, linkToFile
 */

/**
 * Generates a documentation tree that doesn't take too much horizontal space.
 * @param {Object} jsonDocRoot
 * @param {Array.<string>} htmlOut buffer onto which html chunks are pushed.
 */
var renderCompactIndex;

/**
 * Generates a chunk of HTML.
 * @param {string} apiElementName
 * @param {Object} jsonDoc
 * @param {Array.<string>} htmlOut buffer onto which html chunks are pushed.
 */
var renderApiElement;

/**
 * Generates a chunk of HTML.
 * @param {string} filePath
 * @param {Object} jsonDoc
 * @param {Array.<string>} htmlOut buffer onto which html chunks are pushed.
 */
var renderFile;

/**
 * Expand a toggleable control.
 * @param {HTMLElement|string} element
 */
var expand;

/**
 * Invokes callback with the path and JSDoc for each file and directory.
 * @param {function (string, object)} callback.
 */
var forEachFile;

/**
 * Invokes callback with the path and JSDoc for each API element.
 * @param {function (string, object)} callback.
 */
var forEachApiElement;

/**
 * Given an API element, returns the portion of jsdoc corresponding to it.
 * @param {string} a dotted name like {@code foo.bar.baz}.
 * @return {Object}
 */
function lookupApiElement(apiElementName) {
  var jsonDoc = getJsdocRoot();
  if (apiElementName) {
    var unqualifiedNames = apiElementName.split(/\./g);
    for (var i = 0, n = unqualifiedNames.length; i < n; ++i) {
      jsonDoc = jsonDoc[unqualifiedNames[i]];
    }
  }
  return jsonDoc;
}

/**
 * Given a file path, returns the portion of jsdoc corresponding to it.
 * @param {string} a file path.
 * @return {Object}
 */
function lookupFile(path) {
  path = path.replace(/^[\/\\]+|[\/\\]+$/g, '');
  var jsonDoc = getJsdocRoot()['@fileoverview'];
  if (path) {
    var fileNames = path.split(/[\/\\]+/g);
    for (var i = 0, n = fileNames.length; i < n; ++i) {
      jsonDoc = jsonDoc[fileNames[i]];
    }
  }
  return jsonDoc;
}


(function () {
  function html(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/\"/g, '&quot;');
  }

  function titleHtml(s) {
    return s.replace(/<\/?\w[^>]*>/g, '').replace(/\"/g, '&quot;');
  }

  function jsstring(s) {
    return (
        '"'
        + s.replace(
            /[^\x20\x21\x23-\x5b\x5d-\x7e]/g,
            function (m) {
              var codeUnit = m.charCodeAt(0);
              return ((codeUnit < 0x100
                       ? (codeUnit < 0x10 ? '\\x0' : '\\x')
                       : (codeUnit < 0x1000 ? '\\u0' : '\\u'))
                      + codeUnit.toString(16));
            })
        + '"');
  }

  function trim(s) {
    return s ? s.replace(/^\s+|\s+$/g, '') : '';
  }

  var hasOwnProperty = Object.prototype.hasOwnProperty;

  /**
   * Compare two object keys.  If both describe valid array indices, they are
   * compared numerically.  If both are not, they are compared lexically.
   * Array indices compare before string keys.
   */
  function keyComparator(a, b) {
    var na = +a, nb = +b;
    if (na === (a & 0x7fffffff)) {
      if (nb === (b & 0x7fffffff)) {
        a = na, b = nb;
      } else {
        return -1;
      }
    } else if (nb === (b & 0x7fffffff)) {
      return 1;
    }
    return a < b ? -1 : a === b ? 0 : 1;
  }

  /** {@code "foo"} => {@code "Foo"}. */
  function capitalize(s) {
    return s && s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  function map(arr, f) {
    var result = [];
    for (var i = arr.length; --i >= 0;) { result[i] = f(arr[i]); }
    return result;
  }

  function filter(arr, predicate) {
    var result = [];
    for (var i = 0, n = arr.length, k = -1; i < n; ++i) {
      var v = arr[i];
      if (predicate(v)) { result[++k] = v; }
    }
    return result;
  }

  function defined(v) { return v !== undefined; }

  /** An array containing the unqualified names of child API elements. */
  function childNamesInOrder(jsonDoc) {
    var names = [];
    for (var k in jsonDoc) {
      if (hasOwnProperty.call(jsonDoc, k) && !/^@/.test(k)) {
        names.push(k);
      }
    }
    names.sort(keyComparator);
    return names;
  }

  /**
   * True iff this node contains API elements that can be expanded?
   * An expandable element is one where not all information contained will be
   * rendered in the API page for that element.
   */
  function hasExpandableChildren(jsonDoc) {
    for (var k in jsonDoc) {
      if (hasOwnProperty.call(jsonDoc, k) && !/^@/.test(k)) {
        // All functions have an enumerable prototype field, but typically it
        // is empty for non-classes.
        if ('prototype' !== k || hasExpandableChildren(jsonDoc.prototype)) {
          return true;
        }
      }
    }
    return false;
  }

  var apiElementsByFile = null;
  /** Determine the API elements defined in a file. */
  function apiElementsDefinedInFile(path) {
    if (!apiElementsByFile) {
      var byFile = {};
      function walk(apiElementName, jsonDoc) {
        var filepos = jsonDoc['@pos'];
        if (filepos) {
          var path = filepos.match(/^(.*):/)[1];
          path = path.replace(/^[\/\\]+|[\/\\]+$/, '').replace(/[\/\\]+/g, '/');
          var elementList = byFile[path];
          if (!elementList) { elementList = byFile[path] = []; }
          elementList.push(apiElementName);
        }
        var children = childNamesInOrder(jsonDoc);
        for (var i = 0, n = children.length; i < n; ++i) {
          var k = children[i];
          walk(apiElementName ? apiElementName + '.' + k : k, jsonDoc[k]);
        }
      }
      walk('', getJsdocRoot());
      apiElementsByFile = byFile;
    }
    return apiElementsByFile[path] || [];
  }

  renderCompactIndex = function (jsonDocRoot, htmlOut) {
    htmlOut.push('<ul class="compactindex" id="compactindex">');
    renderCompactIndexHelper(
        jsonDocRoot,
        linkToApiElement, 'compactindex:api', 'globals', '.', [], htmlOut);
    renderCompactIndexHelper(
        jsonDocRoot['@fileoverview'],
        linkToFile, 'compactindex:file', 'files', '/', [], htmlOut);
    htmlOut.push('</ul>');
  };

  function makeLink(v) {
    if (v.url) {
      return '<a href="' + html(v.url) + '">'
          + html(v.name || v.url.replace(/^\w+:/, '')) + '</a>';
    } else {
      return v.name;
    }
  }

  /**
   * Maps annotation names to functions that can be applied to annotation
   * members to produce snippets of HTML, or undefined to skip the value.
   */
  var transforms = {
        '@author': makeLink,
        '@extends': function (v) {
          if (v === 'Object') { return undefined; }
          if (lookupApiElement(v)) {
            var link = linkToApiElement(v);
            return '<a href="' + html(link.href) + '">' + html(v)
                + '</a>';
          }
          return '<span class="type">' + html(v) + '</span>';
        },
        '@return': function (v) {
          return '<span class="return">'
              + (v.type
                 ? ' : <span class="type">' + html(v.type) + '</span>'
                 : '')
              + ' <span class="summary">' + v.summary + '</span>'
              + '</span>';
        },
        '@see': makeLink,
        '@param': function (v) {
            return '<span class="param">'
              + '<span class="name">' + v.name + '</span>'
              + (v.type
                 ? ' : <span class="type">' + html(v.type) + '</span>'
                 : '')
              + ' <span class="summary">' + v.summary + '</span>'
              + '</span>';
        },
        '@throws': function (v) {
          return '<span class="throws">'
              + (v.type
                 ? ' : <span class="type">' + html(v.type) + '</span>'
                 : '')
              + ' <span class="summary">' + v.summary + '</span>'
              + '</span>';
        }
      };

  /**
   * Extract annotations.  If two definitions alias each other, this allows
   * using the documentation extracted for one in place of the other.
   */
  function getDocInfo(jsonDoc, apiElementName) {
    var flags = [];
    var annotations = {};

    var defApiElementName = apiElementName;
    var defDoc = jsonDoc;
    if (jsonDoc['@aliases']) {
      defApiElementName = jsonDoc['@aliases'];
      defDoc = lookupApiElement(defApiElementName) || {};
      annotations.Aliases = ['<code>' + html(defApiElementName) + '</code>'];
    }
    for (var k in defDoc) {
      if (!/^@/.test(k)) { continue; }
      var v = defDoc[k];
      if (v[0] === true) {
        flags.push(k.substring(1));
      } else if (k in transforms) {
        var annotationValues = filter(map(v, transforms[k]), defined);
        if (annotationValues.length) {
          annotations[k.substring(1)] = annotationValues;
        }
      }
    }

    return {
          annotations: annotations,
          flags: flags,
          apiElementName: defApiElementName,
          doc: defDoc
        };
  }

  function renderChunk(
      apiElementName, jsonDoc, renderHeader, getMembers, htmlOut) {
    var docInfo = getDocInfo(jsonDoc, apiElementName);

    htmlOut.push('<div class="apiElement">');
    renderHeader(apiElementName, docInfo.flags, docInfo.doc, htmlOut);

    var descHtml = trim(docInfo.doc['@description']);
    if (/\S/.test(descHtml)) {
      htmlOut.push('<div class="description">', descHtml, '</div>');
    }

    var annotationNames = childNamesInOrder(docInfo.annotations);
    for (var i = 0, n = annotationNames.length; i < n; ++i) {
      var annotationName = annotationNames[i];
      htmlOut.push('<div class="annotation">');
      htmlOut.push(
          '<span class=annotationName>', html(capitalize(annotationName)),
          '</span>');
      var annotationValues = docInfo.annotations[annotationName];
      for (var j = 0, m = annotationValues.length; j < m; ++j) {
        htmlOut.push(
            ' <span class="annotationValue">', annotationValues[j], '</span>');
      }
      htmlOut.push('</div>');
    }

    var sections = getMembers(docInfo.apiElementName, docInfo.doc);
    if (sections.length) {
      htmlOut.push('<div class="subheader">Overview</div>');
    }
    for (var j = 0, m = sections.length; j < m; j += 2) {
      var sectionName = sections[j];
      var members = sections[j + 1];
      if (members.length) {
        htmlOut.push(
            '<table class="section"><tr><th colspan="2" class="section-title">',
            sectionName, '</th>');
        for (var i = 0, n = members.length; i < n; ++i) {
          var member = members[i];
          var memberName = member.unqualifiedName;
          var detailAnchor = sectionName + ':' + memberName;
          member.info = getDocInfo(member.doc, member.qualifiedName);
          member.detailAnchor = detailAnchor;
          htmlOut.push('<tr class="member"><td>');
          var memberFlags = member.info.flags;
          if (memberFlags.length) {
            htmlOut.push(
                '<span class="flags">', memberFlags.join(' '), '</span><br>');
          }
          htmlOut.push(
              '<td><a class=\"name\" href="#' + html(detailAnchor) + '">');
          htmlOut.push(html(memberName), '</a><td>');
          var memberType = member.doc['@type'] && member.doc['@type'][0];
          if (memberType) {
            htmlOut.push(' : <span class="type">', html(memberType), '</span>');
          }
          var summaryHtml = trim(member.doc['@summary']);
          if (summaryHtml) {
            htmlOut.push(
                ' &mdash; <span class="summary">', summaryHtml, '</span>');
          }
          htmlOut.push('</tr>');
        }
      }
      htmlOut.push('</table>');
    }

    if (sections.length) {
      htmlOut.push('<div class="subheader">Details</div>');
    }
    for (var j = 0, m = sections.length; j < m; j += 2) {
      var sectionName = sections[j];
      var members = sections[j + 1];
      if (members.length) {
        htmlOut.push(
            '<table class="section"><tr><th colspan="2" class="section-title">',
            sectionName, '</th>');
        for (var i = 0, n = members.length; i < n; ++i) {
          var member = members[i];
          var memberName = member.unqualifiedName;
          var memberType = member.doc['@type'] && member.doc['@type'][0];
          var descHtml = trim(member.doc['@description']);
          var memberInfo = member.info;
          htmlOut.push('<tr class="member"><td>');
          var memberFlags = member.info.flags;
          if (memberFlags.length) {
            htmlOut.push(
                '<span class="flags">', memberFlags.join(' '), '</span><br>');
          }
          htmlOut.push('<td>');
          if (member.navigationLink) {
            htmlOut.push(
                '<a class=\"name\" name="', html(member.detailAnchor),
                '" href="', html(member.navigationLink.href),
                '" target="', html(member.navigationLink.target), '">');
          }
          htmlOut.push(html(memberName));
          if (member.navigationLink) { htmlOut.push('</a>'); }
          htmlOut.push('<td>');
          if (memberType) {
            htmlOut.push(' : <span class="type">', html(memberType), '</span>');
          }
          if (descHtml) {
            htmlOut.push(
                ' &mdash; <span class="description">', descHtml, '</span>');
          }
          var memberAnnotationNames = childNamesInOrder(memberInfo.annotations);
          if (memberAnnotationNames.length) {
            for (var k = 0, na = memberAnnotationNames.length; k < na; ++k) {
              var memberAnnotationName = memberAnnotationNames[k];
              htmlOut.push('<tr><td><td class="annotationName">',
                           memberAnnotationName);
              htmlOut.push('<td class="annotationValue">',
                           memberInfo.annotations[memberAnnotationName]
                           .join('<br>'));
            }
          }
        }
      }
      htmlOut.push('</table>');
    }
  }

  renderApiElement = function (apiElementName, jsonDoc, htmlOut) {
    renderChunk(
        apiElementName, jsonDoc,
        function (apiElementName, flags, jsonDoc, htmlOut) {
          var type = jsonDoc['@type'] && jsonDoc['@type'][0];
          var pos = jsonDoc['@pos'];
          var posLink;
          if (pos) {
            var posParts = pos.match(/^(.*):(\d+)\+\d+(?: - \d+(?:\+\d+)?)?$/);
            if (posParts) {
              posLink = linkToSource(posParts[1], +posParts[2]);              
            }
          }
          htmlOut.push(
              '<div class="header">',
              (flags.length
               ? '<span class="flags">' + flags.join(' ') + '</span> '
               : ''),
              '<span class="name">',
              (html(apiElementName).replace(
                   /(^|\.)([^.]+)$/,
                   '$1<span class="unqualifiedName">$2</span>')
               || 'Globals'),
              '</span>',
              (type ? ' : <span class="type">' + html(type) + '</span>' : ''),
              (posLink
               ? (' @ <a class="pos" href="' + html(posLink.href) + '"'
                  + ' target="' + html(posLink.target) + '">' + pos + '</a>')
               : pos
               ? ' @ <span class="pos">' + pos + '</span>'
               : ''),
              '</div>');
        },
        function (apiElementName, jsonDoc) {  // Enumerate the members lists
          // We need to fold a constructor's prototype's members into the list
          var isCtor = (hasOwnProperty.call(jsonDoc, 'prototype')
                        && hasOwnProperty.call(jsonDoc, '@type')
                        && jsonDoc['@type'][0] === 'Function'
                        && (hasExpandableChildren(jsonDoc.prototype)
                            || jsonDoc['@constructor']));
          var sections = [];
          if (!apiElementName) {
            sections.push({ name: '',
                            doc: jsonDoc,
                            title: 'Globals',
                            navigable: true });
          } else if (isCtor) {
            sections.push({ name: apiElementName,
                            doc: jsonDoc,
                            title: 'Statics',
                            navigable: true
                          },
                          { name: apiElementName + '.prototype',
                            doc: jsonDoc.prototype,
                            title: 'Prototype Members',
                            navigable: true
                          },
                          { name: apiElementName + '.@field',
                            doc: jsonDoc['@field'],
                            title: 'Instance Members',
                            // Instance members aren't real API elements since
                            // they don't appear on the constructor, so the
                            // linkToApiElement function won't work.
                            navigable: false
                          });
          } else {
            sections.push({ name: apiElementName,
                            doc: jsonDoc,
                            title: 'Members',
                            navigable: true });
          }
          var memberLists = [];
          for (var i = 0, n = sections.length; i < n; ++i) {
            var section = sections[i];
            var members = map(
                childNamesInOrder(section.doc),
                function (memberName) {
                  var qualifiedName;
                  if (section.name) {
                    qualifiedName = section.name + '.' + memberName;
                  } else {
                    qualifiedName = memberName;
                  }
                  var doc = section.doc[memberName];
                  if (doc['@aliases']) {
                    doc = lookupApiElement(doc['@aliases']) || {};
                  }
                  var navLink = null;
                  if (section.navigable) {
                    navLink = linkToApiElement(qualifiedName);
                  }
                  return {
                        unqualifiedName: memberName,
                        qualifiedName: qualifiedName,
                        doc: doc,
                        navigationLink: navLink
                      };
                });
            memberLists.push(section.title, members);
          }
          return memberLists;
        },
        htmlOut);
  };

  renderFile = function (filePath, jsonDoc, htmlOut) {
    renderChunk(
        filePath, jsonDoc,
        function (apiElementName, flags, jsonDoc, htmlOut) {
          htmlOut.push(
              '<div class="header">',
              (flags.length
               ? '<span class="flags">' + flags.join(' ') + '</span> '
               : ''),
              '<span class="name">',
              (html(apiElementName).replace(
                   /\.([^.]+)$/, '.<span class="unqualifiedName">$1</span>')
               || '.'),
              '</span>',
              '</div>');
        },
        function (path, jsonDoc) {
          var subfiles = map(
              childNamesInOrder(jsonDoc),
              function (fileName) {
                var qualifiedName = (path ? path + '/' : '') + fileName;
                return {
                    unqualifiedName: fileName,
                    doc: jsonDoc[fileName],
                    qualifiedName: qualifiedName,
                    navigationLink: linkToFile(qualifiedName)
                  };
              });
          var apiElements = map(
              apiElementsDefinedInFile(path),
              function (apiElementName) {
                return {
                    unqualifiedName: apiElementName,
                    qualifiedName: apiElementName,
                    doc: lookupApiElement(apiElementName),
                    navigationLink: linkToApiElement(apiElementName)
                  };
              });
          return ['Files', subfiles, 'Members', apiElements];
        },
        htmlOut);
  };

  var ARROW = '\u21B3';
  /** Outputs the compact index as a list of unordered list. */
  function renderCompactIndexHelper(
      jsonDoc, linkMaker, idPrefix, name, separator, nameParts, htmlOut) {
    var apiElementName = nameParts.join(separator);
    var summaryHtml = trim(jsonDoc['@summary']) || html(apiElementName);
    var expandable = hasExpandableChildren(jsonDoc);
    htmlOut.push('<li>', ARROW, '&nbsp;');
    if (expandable) {
      htmlOut.push(
          ' <span class="expando" onclick="expand(this);return false">'
          + '+</span>');
    } else {
      htmlOut.push('<span class="noexpando">&nbsp;</span>');
    }
    var link = linkMaker(apiElementName);
    htmlOut.push(
        '&nbsp;<a href="', html(link.href), '" target="', html(link.target),
        '" title="', titleHtml(summaryHtml), '">',
        html(name), '</a>');
    if (expandable) {
      htmlOut.push('<ul class="foldable"',
                   ' id="' + idPrefix + ':', html(apiElementName), '">');
      var depth = nameParts.length;
      var childNames = childNamesInOrder(jsonDoc);
      for (var i = 0, n = childNames.length; i < n; ++i) {
        var k = childNames[i];
        if (!hasOwnProperty.call(jsonDoc, k) || /^@/.test(k)) { continue; }
        nameParts[depth] = k;
        renderCompactIndexHelper(
            jsonDoc[k], linkMaker, idPrefix, k, separator, nameParts, htmlOut);
      }
      nameParts.length = depth;
      htmlOut.push('</ul>');
    }
    htmlOut.push('</li>');
  }

  function expandNode(toggle, opt_expand) {
    var container = toggle.parentNode;
    var newClass = container.className.replace(/\s*\bexpanded\b/g, '');
    var expand = (opt_expand === undefined
                  ? newClass === container.className  // toggle
                  : opt_expand);
    if (expand) {
      toggle.innerHTML = '-';
      newClass += ' expanded';
    } else {
      toggle.innerHTML = '+';
    }
    container.className = newClass;
  }

  expand = function (nodeOrName) {
    if (typeof nodeOrName === 'string') {
      var apiElementName = nodeOrName;
      do {
        var newApiElementName = apiElementName.replace(/(?:^|\.)[^.]*$/, '');
        if (newApiElementName === apiElementName) { break; }
        apiElementName = newApiElementName;

        var container = document.getElementById(
            'compactindex:api:' + apiElementName);
        if (container) {
          for (var n = container.parentNode.firstChild; n; n = n.nextSibling) {
            if (n.nodeType === 1 && /\bexpando\b/.test(n.className)) {
              expandNode(n, true);
              break;
            }
          }
        }
      } while (apiElementName);
    } else {
      expandNode(nodeOrName);
    }
  };

  function docIterator(callback, docJson, pathSeparator) {
    var pathParts = [];
    var pathPartPattern = /^[^@]/;
    function walk(docJson) {
      var path = pathParts.join(pathSeparator);
      callback(path, docJson);
      for (var k in docJson) {
        if (hasOwnProperty.call(docJson, k) && pathPartPattern.test(k)) {
          var len = pathParts.length;
          pathParts[len] = k;
          walk(docJson[k]);
          pathParts.length = len;
        }
      }
    }
    walk(docJson);
  }

  forEachFile = function (callback) {
    var fileRoot = getJsdocRoot()['@fileoverview'];
    if (fileRoot) {
      docIterator(callback, fileRoot, '/');
    }
  };

  forEachApiElement = function (callback) {
    docIterator(callback, getJsdocRoot(), '.');
  };
})();
