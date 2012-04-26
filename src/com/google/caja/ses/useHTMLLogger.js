// Copyright (C) 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview Exports a {@code ses.logger} which logs to elements
 * on an HTML page.
 *
 * <p>To use the file, before loading <code>logger.js</code> or
 * <code>initSES*.js</code> which includes <code>logger.js</code>, you
 * must load and initialize this file. <code>logger.js</code> will
 * then detect that there is already a logger in place and not
 * overwrite it. For example, the beginning of your html file might
 * read
 * <pre>  &lt;div id="reports"&gt;&lt;/div&gt;
 *   &lt;div id="console"&gt;&lt;/div&gt;
 *   &lt;script src="useHTMLLogger.js"&gt;&lt;/script&gt;
 *   &lt;script&gt;
 *     function gebi(id) { return document.getElementById(id); };
 *     useHTMLLogger(gebi("reports"), gebi("console"));
 *   &lt;/script&gt;
 *   &lt;script src="initSES.js"&gt;&lt;/script&gt;
 * </pre>
 *
 * <p>Assumes only ES3. Compatible with ES5, ES5-strict, or
 * anticipated ES6.
 *
 * @author Mark S. Miller
 * @requires document
 * @overrides ses, window
 * @provides useHTMLLogger
 */
var ses;
if (!ses) { ses = {}; }

function useHTMLLogger(reportsElement, consoleElement) {
  "use strict";

  var slice = [].slice;

  var maxElement = void 0;

  /**
   * Needs to work on ES3
   */
  function forEach(list, callback) {
    for (var i = 0, len = list.length; i < len; i++) {
      callback(list[i], i);
    }
  }

  function appendNew(parent, tagName) {
    var result = document.createElement(tagName);
    parent.appendChild(result);
    return result;
  }

  function prependNew(parent, tagName) {
    var result = document.createElement(tagName);
    parent.insertBefore(result, parent.firstChild);
    return result;
  }

  function appendText(parent, text) {
    var result = document.createTextNode(text);
    parent.appendChild(result);
    return result;
  }

  function textAdder(parent, style) {
    return function(text) {
      var p = appendNew(parent, 'p');
      appendText(p, text);
      p.className = style;
      return p;
    };
  }

  var INFLATE = '[+]';
  var DEFLATE = '[-]';
  function deflate(toggler, inflatables, opt_sep) {
    var sep = opt_sep !== void 0 ? opt_sep : ' ';
    var toggle = prependNew(toggler, 'tt');
    var icon = appendText(toggle, INFLATE);
    appendText(toggle, sep);
    forEach(inflatables, function(inflatable) {
      inflatable.style.display = 'none';
    });
    toggler.addEventListener('click', function(event) {
      if (icon.data === INFLATE) {
        forEach(inflatables, function(inflatable) {
          inflatable.style.removeProperty('display');
        });
        icon.data = DEFLATE;
      } else {
        forEach(inflatables, function(inflatable) {
          inflatable.style.display = 'none';
        });
        icon.data = INFLATE;
      }
    }, false);
    toggler.style.cursor = 'pointer';
  }

  /** modeled on textAdder */
  function makeLogFunc(parent, style) {
    return function logFunc(var_args) {
      var p = appendNew(parent, 'p');
      var args = slice.call(arguments, 0);

      // See debug.js
      var getStack = ses.getStack;

      for (var i = 0, len = args.length; i < len; i++) {
        var span = appendNew(p, 'span');
        appendText(span, '' + args[i]);

        if (getStack) {
          var stack = getStack(args[i]);
          if (stack) {
            var stackNode = appendNew(p, 'pre');
            appendText(stackNode, stack);
            deflate(span, [stackNode], '');
          }
        }
      }
      p.className = style;
      return p;
    };
  }

  var logger = {
    log:   makeLogFunc(consoleElement, 'log'),
    info:  makeLogFunc(consoleElement, 'info'),
    warn:  makeLogFunc(consoleElement, 'warn'),
    error: makeLogFunc(consoleElement, 'error')
  };

  var TestIDPattern = /^(Sbp|S)?([\d\.]*)/;

  /**
   *
   */
  function linkToTest(test) {
    var match = TestIDPattern.exec(test);
    if (match) {
      var parts = match[2].split('.');
      var result = 'http://hg.ecmascript.org/tests/test262/file/' +
        'c84161250e66/' + // TODO(erights): How do I get the tip automatically?
        'test/suite/';
      if (match[1] === void 0) {
        result += 'chapter';
      } else if (match[1] === 'S') {
        result += 'ch';
      } else if (match[1] === 'Sbp') {
        result += 'bestPractice';
      }
      var len = parts.length;
      if (len === 0) {
        result += '/';
      } else {
        result += (parts[0].length === 1 ? '0' : '') + parts[0] + '/';
        for (var i = 1; i < len; i++) {
          result += parts.slice(0, i+1).join('.') + '/';
        }
      }
      result += test + '.js';
      return result;
    }

    var site = test.charAt(0) === 'S' ?
      '+site%3Acode.google.com' : '+site%3Aes5conform.svn.codeplex.com';
    return 'http://www.google.com/search?btnI=&q=' +
      encodeURIComponent(test) + site;
  }

  /**
   * Logs a report suitable for display on a web page.
   */
  logger.reportRepairs = function reportRepairs(reports) {
    var numFineElement = appendNew(reportsElement, 'p');
    var ul = appendNew(reportsElement, 'ul');

    var fineElements = [];

    forEach(reports, function(report, i) {
      var li = appendNew(ul, 'li');
      if (report.status === ses.statuses.ALL_FINE) {
        fineElements.push(li);
        li.style.listStyleType = 'none';
      }

      var reportElement = appendNew(li, 'p');

      var classification = ses.logger.classify(report.postSeverity);
      reportElement.className = classification.consoleLevel;

      appendText(reportElement, i + ') ' + report.status + ': ' +
                 report.description + '. ' + classification.note);

      if (typeof report.beforeFailure === 'string') {
        var beforeElement = appendNew(reportElement, 'p');
        appendText(beforeElement, 'New pre symptom: ' + report.beforeFailure);
      }
      if (typeof report.afterFailure === 'string') {
        var afterElement = appendNew(reportElement, 'p');
        appendText(afterElement, 'New post symptom: ' + report.afterFailure);
      }

      var linksBlock = appendNew(li, 'blockquote');
      deflate(reportElement, [linksBlock]);

      // TODO(erights): sort by URL relevance based on platform
      forEach(report.urls, function(url, i) {
        var linkElement = appendNew(linksBlock, 'p');
        if (i === 0) { appendText(linkElement, 'See '); }
        var link = appendNew(linkElement, 'a');
        link.href = url;
        link.target = '_blank';
        appendText(link, url);
        // TODO(erights): spawn a task to fetch the title of the bug
        // and use it to replace the link text.
      });

      forEach(report.sections, function(section, i) {
        var linkElement = appendNew(linksBlock, 'p');
        if (i === 0) { appendText(linkElement, 'See '); }
        var link = appendNew(linkElement, 'a');
        link.href = 'http://es5.github.com/#x' + encodeURIComponent(section);
        link.target = '_blank';
        appendText(link, 'Section ' + section);
      });

      forEach(report.tests, function(test, i) {
        var linkElement = appendNew(linksBlock, 'p');
        if (i === 0) { appendText(linkElement, 'See '); }
        var link = appendNew(linkElement, 'a');
        link.href = linkToTest(test);
        link.target = '_blank';
        appendText(link, 'Test ' + test);
      });
    });

    if (fineElements.length >= 1) {
      appendText(numFineElement, fineElements.length + ' Fine.');
      deflate(numFineElement, fineElements);
    }
  };

  logger.reportMax = function reportMax() {
    if (!maxElement) {
      maxElement = appendNew(reportsElement, 'p');
    } else {
      maxElement.textContent = '';
    }
    if (ses.maxSeverity.level > ses.severities.SAFE.level) {
      var maxClassification = ses.logger.classify(ses.maxSeverity);
      maxElement.className = maxClassification.consoleLevel;
      appendText(maxElement, 'Max Severity: ' + maxClassification.note);
    }
  };

  logger.reportDiagnosis = function reportDiagnosis(severity,
                                                    status,
                                                    problemList) {
    var diagnosisElement = appendNew(reportsElement, 'p');
    var classification = ses.logger.classify(severity);
    var head = textAdder(diagnosisElement, classification.consoleLevel)(
      problemList.length + ' ' + status + '. ' + classification.note);
    var tail = appendNew(diagnosisElement, 'blockquote');
    textAdder(tail, classification.consoleLevel)(
      problemList.sort().join(' '));
    deflate(head, [tail]);
  };

  ses.logger = logger;
}

// Exports for closure compiler.
if (typeof window !== 'undefined') {
  window['useHTMLLogger'] = useHTMLLogger;
}
