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
 * @fileoverview Exports a {@code ses.logger} which logs to the
 * console if one exists.
 *
 * <p>This <code>logger.js</code> file both defines the logger API and
 * provides default implementations for its methods. Because
 * <code>logger.js</code> is normally packaged in
 * <code>initSES.js</code>, it is built to support being overridden by
 * a script run <i>earlier</i>. For example, for better diagnostics,
 * consider loading and initializing <code>useHTMLLogger.js</code> first.
 *
 * <p>The {@code ses.logger} API consists of
 * <dl>
 *   <dt>log, info, warn, and error methods</dt>
 *     <dd>each of which take a list of arguments which should be
 *         stringified and appended together. The logger should
 *         display this string associated with that severity level. If
 *         any of the arguments has an associated stack trace
 *         (presumably Error objects), then the logger <i>may</i> also
 *         show this stack trace. If no {@code ses.logger} already
 *         exists, the default provided here forwards to the
 *         pre-existing global {@code console} if one
 *         exists. Otherwise, all four of these do nothing. If we
 *         default to forwarding to the pre-existing {@code console} ,
 *         we prepend an empty string as first argument since we do
 *         not want to obligate all loggers to implement the console's
 *         "%" formatting. </dd>
 *   <dt>classify(postSeverity)</dt>
 *     <dd>where postSeverity is a severity
 *         record as defined by {@code ses.severities} in
 *         <code>repairES5.js</code>, and returns a helpful record
 *         consisting of a
 *         <dl>
 *           <dt>consoleLevel:</dt>
 *             <dd>which is one of 'log', 'info', 'warn', or
 *                 'error', which can be used to select one of the above
 *                 methods.</dd>
 *           <dt>note:</dt>
 *             <dd>containing some helpful text to display
 *                 explaining the impact of this severity on SES.</dd>
 *         </dl>
 *   <dt>reportRepairs(reports)</dt>
 *     <dd>where {@code reports} is the list of repair reports, each
 *         of which contains
 *       <dl>
 *         <dt>description:</dt>
 *           <dd>a string describing the problem</dd>
 *         <dt>preSeverity:</dt>
 *           <dd>a severity record (as defined by {@code
 *               ses.severities} in <code>repairES5.js</code>)
 *               indicating the level of severity of this problem if
 *               unrepaired. Or, if !canRepair, then the severity
 *               whether or not repaired.</dd>
 *         <dt>canRepair:</dt>
 *           <dd>a boolean indicating "if the repair exists and the test
 *               subsequently does not detect a problem, are we now ok?"</dd>
 *         <dt>urls:</dt>
 *           <dd>a list of URL strings, each of which points at a page
 *               relevant for documenting or tracking the bug in
 *               question. These are typically into bug-threads in issue
 *               trackers for the various browsers.</dd>
 *         <dt>sections:</dt>
 *           <dd>a list of strings, each of which is a relevant ES5.1
 *               section number.</dd>
 *         <dt>tests:</dt>
 *           <dd>a list of strings, each of which is the name of a
 *               relevant test262 or sputnik test case.</dd>
 *         <dt>postSeverity:</dt>
 *           <dd>a severity record (as defined by {@code
 *               ses.severities} in <code>repairES5.js</code>)
 *               indicating the level of severity of this problem
 *               after all repairs have been attempted.</dd>
 *         <dt>beforeFailure:</dt>
 *           <dd>The outcome of the test associated with this record
 *               as run before any attempted repairs. If {@code
 *               false}, it means there was no failure. If {@code
 *               true}, it means that the test fails in some way that
 *               the authors of <code>repairES5.js</code>
 *               expected. Otherwise it returns a string describing
 *               the symptoms of an unexpected form of failure. This
 *               is typically considered a more severe form of failure
 *               than {@code true}, since the authors have not
 *               anticipated the consequences and safety
 *               implications.</dd>
 *         <dt>afterFailure:</dt>
 *           <dd>The outcome of the test associated with this record
 *               as run after all attempted repairs.</dd>
 *       </dl>
 *       The default behavior here is to be silent.</dd>
 *   <dt>reportMax()</dt>
 *     <dd>Displays only a summary of the worst case
 *         severity seen so far, according to {@code ses.maxSeverity} as
 *         interpreted by {@code ses.logger.classify}.</dd>
 *   <dt>reportDiagnosis(severity, status, problemList)</dt>
 *     <dd>where {@code severity} is a severity record, {@code status}
 *         is a brief string description of a list of problems, and
 *         {@code problemList} is a list of strings, each of which is
 *         one occurrence of the described problem.
 *         The default behavior here should only the number of
 *         problems, not the individual problems.</dd>
 * </dl>
 *
 * <p>Assumes only ES3. Compatible with ES5, ES5-strict, or
 * anticipated ES6.
 *
 * //provides ses.logger
 * @author Mark S. Miller
 * @requires console
 * @overrides ses, loggerModule
 */
var ses;
if (!ses) { ses = {}; }

(function loggerModule() {
  "use strict";

  var logger;
  function logNowhere(str) {}

  var slice = [].slice;
  var apply = slice.apply;



  if (ses.logger) {
    logger = ses.logger;

  } else if (typeof console !== 'undefined' && 'log' in console) {
    // We no longer test (typeof console.log === 'function') since,
    // on IE9 and IE10preview, in violation of the ES5 spec, it
    // is callable but has typeof "object".
    // See https://connect.microsoft.com/IE/feedback/details/685962/
    //   console-log-and-others-are-callable-but-arent-typeof-function

    // We manually wrap each call to a console method because <ul>
    // <li>On some platforms, these methods depend on their
    //     this-binding being the console.
    // <li>All this has to work on platforms that might not have their
    //     own {@code Function.prototype.bind}, and has to work before
    //     we install an emulated bind.
    // </ul>

    var forward = function(level, args) {
      args = slice.call(args, 0);
      // We don't do "console.apply" because "console" is not a function
      // on IE 10 preview 2 and it has no apply method. But it is a
      // callable that Function.prototype.apply can successfully apply.
      // This code most work on ES3 where there's no bind. When we
      // decide support defensiveness in contexts (frames) with mutable
      // primordials, we will need to revisit the "call" below.
      apply.call(console[level], console, [''].concat(args));

      // See debug.js
      var getStack = ses.getStack;
      if (getStack) {
        for (var i = 0, len = args.length; i < len; i++) {
          var stack = getStack(args[i]);
          if (stack) {
            console[level]('', stack);
          }
        }
      }
    };

    logger = {
      log:   function log(var_args)   { forward('log', arguments); },
      info:  function info(var_args)  { forward('info', arguments); },
      warn:  function warn(var_args)  { forward('warn', arguments); },
      error: function error(var_args) { forward('error', arguments); }
    };
  } else {
    logger = {
      log:   logNowhere,
      info:  logNowhere,
      warn:  logNowhere,
      error: logNowhere
    };
  }

  /**
   * Returns a record that's helpful for displaying a severity.
   *
   * <p>The record contains {@code consoleLevel} and {@code note}
   * properties whose values are strings. The {@code consoleLevel} is
   * {@code "log", "info", "warn", or "error"}, which can be used as
   * method names for {@code logger}, or, in an html context, as a css
   * class name. The {@code note} is a string stating the severity
   * level and its consequences for SES.
   */
  function defaultClassify(postSeverity) {
    var MAX_SES_SAFE = ses.severities.SAFE_SPEC_VIOLATION;

    var consoleLevel = 'log';
    var note = '';
    if (postSeverity.level > ses.severities.SAFE.level) {
      consoleLevel = 'info';
      note = postSeverity.description + '(' + postSeverity.level + ')';
      if (postSeverity.level > ses.maxAcceptableSeverity.level) {
        consoleLevel = 'error';
        note += ' is not suitable for SES';
      } else if (postSeverity.level > MAX_SES_SAFE.level) {
        consoleLevel = 'warn';
        note += ' is not SES-safe';
      }
      note += '.';
    }
    return {
      consoleLevel: consoleLevel,
      note: note
    };
  }

  if (!logger.classify) {
    logger.classify = defaultClassify;
  }

  /**
   * By default is chatty
   */
  function defaultReportRepairs(reports) {
    for (var i = 0; i < reports.length; i++) {
      var report = reports[i];
      if (report.status !== 'All fine') {
        logger.warn(report.status + ': ' + report.description);
      }
    }
  }

  if (!logger.reportRepairs) {
    logger.reportRepairs = defaultReportRepairs;
  }

  /**
   * By default, logs a report suitable for display on the console.
   */
  function defaultReportMax() {
    if (ses.maxSeverity.level > ses.severities.SAFE.level) {
      var maxClassification = ses.logger.classify(ses.maxSeverity);
      logger[maxClassification.consoleLevel](
        'Max Severity: ' + maxClassification.note);
    }
  }

  if (!logger.reportMax) {
    logger.reportMax = defaultReportMax;
  }

  function defaultReportDiagnosis(severity, status, problemList) {
    var classification = ses.logger.classify(severity);
    ses.logger[classification.consoleLevel](
      problemList.length + ' ' + status);
  }

  if (!logger.reportDiagnosis) {
    logger.reportDiagnosis = defaultReportDiagnosis;
  }

  ses.logger = logger;

})();
