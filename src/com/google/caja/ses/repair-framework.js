// Copyright (C) 2011-2013 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview Framework for monkey-patching to avoid bugs and add
 * features. Applies only necessary patches and verifies they succeeded.
 *
 * //requires ses.acceptableProblems, ses.maxAcceptableSeverityName
 * //provides ses.statuses, ses.severities, ses._Repairer, ses._repairer,
 * //    ses._EarlyStringMap
 *
 * @author Mark S. Miller
 * @author Kevin Reid
 * @overrides ses
 */

var ses;

(function() {
  "use strict";

  var logger = ses.logger;

  /**
   * The severity levels.
   *
   * <dl>
   *   <dt>MAGICAL_UNICORN</dt>
   *     <dd>Unachievable magical mode used for testing.</dd>
   *   <dt>SAFE</dt><dd>no problem.</dd>
   *   <dt>SAFE_SPEC_VIOLATION</dt>
   *     <dd>safe (in an integrity sense) even if unrepaired. May
   *         still lead to inappropriate failures.</dd>
   *   <dt>UNSAFE_SPEC_VIOLATION</dt>
   *     <dd>a safety issue only indirectly, in that this spec
   *         violation may lead to the corruption of assumptions made
   *         by other security critical or defensive code.</dd>
   *   <dt>NOT_OCAP_SAFE</dt>
   *     <dd>a violation of object-capability rules among objects
   *         within a coarse-grained unit of isolation.</dd>
   *   <dt>NOT_ISOLATED</dt>
   *     <dd>an inability to reliably sandbox even coarse-grain units
   *         of isolation.</dd>
   *   <dt>NEW_SYMPTOM</dt>
   *     <dd>some test failed in a way we did not expect.</dd>
   *   <dt>NOT_SUPPORTED</dt>
   *     <dd>this platform cannot even support SES development in an
   *         unsafe manner.</dd>
   * </dl>
   */
  var severities = ses.severities = {
    MAGICAL_UNICORN:       { level: -1, description: 'Testing only' },
    SAFE:                  { level: 0, description: 'Safe' },
    SAFE_SPEC_VIOLATION:   { level: 1, description: 'Safe spec violation' },
    UNSAFE_SPEC_VIOLATION: { level: 3, description: 'Unsafe spec violation' },
    NOT_OCAP_SAFE:         { level: 4, description: 'Not ocap safe' },
    NOT_ISOLATED:          { level: 5, description: 'Not isolated' },
    NEW_SYMPTOM:           { level: 6, description: 'New symptom' },
    NOT_SUPPORTED:         { level: 7, description: 'Not supported' }
  };

  /**
   * Statuses.
   *
   * <dl>
   *   <dt>ALL_FINE</dt>
   *     <dd>test passed before and after.</dd>
   *   <dt>REPAIR_FAILED</dt>
   *     <dd>test failed before and after repair attempt.</dd>
   *   <dt>NOT_REPAIRED</dt>
   *     <dd>test failed before and after, with no repair to attempt.</dd>
   *   <dt>REPAIR_SKIPPED</dt>
   *     <dd>test failed before and after, and ses.acceptableProblems
   *         specified not to repair it.</dd>
   *   <dt>REPAIRED_UNSAFELY</dt>
   *     <dd>test failed before and passed after repair attempt, but
   *         the repair is known to be inadequate for security, so the
   *         real problem remains.</dd>
   *   <dt>REPAIRED</dt>
   *     <dd>test failed before and passed after repair attempt,
   *         repairing the problem (canRepair was true).</dd>
   *   <dt>SYMPTOM_INTERMITTENT</dt>
   *      <dd>test failed before and passed after, despite no repair
   *          to attempt. Success was either intermittent or triggered
   *          by some other attempted repair. Since we don't know why
   *          it now seems to work, we must conservatively
   *          assume that the underlying problem remains.</dd>
   *   <dt>BROKEN_BY_OTHER_ATTEMPTED_REPAIRS</dt>
   *      <dd>test passed before and failed after, indicating that
   *          some other attempted repair created the problem.</dd>
   * </dl>
   */
  var statuses = ses.statuses = {
    ALL_FINE:                          'All fine',
    REPAIR_FAILED:                     'Repair failed',
    NOT_REPAIRED:                      'Not repaired',
    REPAIR_SKIPPED:                    'Repair skipped',
    REPAIRED_UNSAFELY:                 'Repaired unsafely',
    REPAIRED:                          'Repaired',
    SYMPTOM_INTERMITTENT:              'Symptom intermittent',
    BROKEN_BY_OTHER_ATTEMPTED_REPAIRS: 'Broken by other attempted repairs'
  };

  function validateSeverityName(severityName, failIfInvalid) {
    if (severityName) {
      var sev = ses.severities[severityName];
      if (sev && typeof sev.level === 'number' &&
        sev.level >= ses.severities.MAGICAL_UNICORN.level &&
        sev.level < ses.severities.NOT_SUPPORTED.level) {
        // do nothing
      } else if (failIfInvalid) {
        throw new RangeError('Bad SES severityName: ' + severityName);
      } else {
        logger.error('Ignoring bad severityName: ' + severityName + '.');
        severityName = 'SAFE_SPEC_VIOLATION';
      }
    } else {
      severityName = 'SAFE_SPEC_VIOLATION';
    }
    return severityName;
  }

  function lookupSeverityName(severityName, failIfInvalid) {
    return ses.severities[validateSeverityName(severityName, failIfInvalid)];
  }

  //////// General utilities /////////

  /**
   * Needs to work on ES3, since we want to correctly report failure
   * on an ES3 platform.
   */
  function strictForEachFn(list, callback) {
    for (var i = 0, len = list.length; i < len; i++) {
      callback(list[i], i);
    }
  }

  /**
   * Needs to work on ES3, since we want to correctly report failure
   * on an ES3 platform.
   */
  function strictMapFn(list, callback) {
    var result = [];
    for (var i = 0, len = list.length; i < len; i++) {
      result.push(callback(list[i], i));
    }
    return result;
  }

  /**
   * The Repairer and several test/repair routines want string-keyed maps.
   * Unfortunately, our exported StringMap is not yet available, and our repairs
   * include one which breaks Object.create(null). So, an ultra-minimal,
   * ES3-compatible implementation.
   */
  function EarlyStringMap() {
    var objAsMap = {};
    var self = {
      get: function(key) {
        return objAsMap[key + '$'];
      },
      set: function(key, value) {
        objAsMap[key + '$'] = value;
      },
      has: function(key) {
        return (key + '$') in objAsMap;
      },
      'delete': function(key) {
        return delete objAsMap[key + '$'];
      },
      forEach: function(callback) {
        for (var key in objAsMap) {
          if (key.lastIndexOf('$') === key.length - 1) {
            callback(objAsMap[key], key.slice(0, -1), self);
          }
        }
      }
    };
    return self;
  }

  // Exported for use in repairES5.js.
  ses._EarlyStringMap = EarlyStringMap;

  //////// The repairer /////////

  /**
   * A Repairer has a table of problems to detect and/or repair, and keeps track
   * of whether they have been (successfully or unsuccessfully) repaired.
   *
   * Ordinary SES initialization has only one Repairer; it is written as a class
   * for testing purposes.
   */
  function Repairer() {
    var self = this;

    /**
     * Configuration: the max post-repair severity that is considered acceptable
     * for SES operation.
     */
    var maxAcceptableSeverity = ses.severities.SAFE;

    /**
     * Configuration: an object whose enumerable keys are problem names and
     * whose values are records containing the following boolean properties,
     * defaulting to false if omitted:
     * <dl>
     *
     * <dt>{@code permit}
     * <dd>If this problem is not repaired, continue even if its severity
     * would otherwise be too great (currentSeverity will be as if this
     * problem does not exist). Use this for problems which are known
     * to be acceptable for the particular use case of SES.
     *
     * <p>THIS CONFIGURATION IS POTENTIALLY EXTREMELY DANGEROUS. Ignoring
     * problems can make SES itself insecure in subtle ways even if you
     * do not use any of the affected features in your own code. Do not
     * use it without full understanding of the implications.
     *
     * <p>TODO(kpreid): Add a flag to problem records to indicate whether
     * the problems may be ignored and check it here.
     * </dd>
     *
     * <dt>{@code doNotRepair}
     * <dd>Do not attempt to repair this problem.
     * Use this for problems whose repairs have unacceptable disadvantages.
     *
     * <p>Observe that if {@code permit} is also false, then this means to
     * abort rather than repairing, whereas if {@code permit} is true then
     * this means to continue without repairing the problem even if it is
     * repairable.
     *
     * </dl>
     */
    var acceptableProblems = {};

    /**
     * Whether acceptableProblems has been used and therefore should not be
     * modified.
     */
    var acceptableProblemsLocked = false;

    /**
     * As we start to repair, this will track the worst *post-repair* severity
     * seen so far.
     *
     * See also yetToRepair; the "current" severity is the maximum of
     * plannedSeverity and the contents of yetToRepair.
     */
    var plannedSeverity = ses.severities.SAFE;

    /**
     * All registered problem records, indexed by ID. See docs and
     * implementation of the registerProblem method for format details.
     *
     * These records are never exposed to clients.
     */
    var problemRecords = new EarlyStringMap();

    /**
     * All problem records whose test/repair/report steps have not yet been
     * executed; a subset of problemRecords.
     */
    var notDoneProblems = new EarlyStringMap();

    /**
     * This is all problems which have not been either repaired or shown not to
     * be present.
     */
    var yetToRepair = new EarlyStringMap();

    /**
     * Outcomes of the earliest test run (before repairs). Keys are problem IDs
     * and values are return values of test functions.
     */
    var earliestTests = new EarlyStringMap();

    /**
     * Outcomes of the latest test run (after repairs, or before repairs if
     * repairs have not been run yet). Keys are problem IDs and values are
     * return values of test functions.
     */
    var latestTests = new EarlyStringMap();

    /**
     * For reporting; contains the same keys as latestTests.
     */
    var reports = new EarlyStringMap();

    /**
     * All repair functions which have been executed and therefore should not
     * be retried.
     *
     * This is a table of repair functions and not of problem records because
     * multiple problem records may share the same repair.
     */
    var repairsPerformed = [];

    var postTestKludge = undefined;

    var aboutTo = void 0;

    //////// Internals /////////

    var defaultDisposition = { permit: false, doNotRepair: false };
    function disposition(problem) {
      acceptableProblemsLocked = true;
      return Object.prototype.hasOwnProperty.call(acceptableProblems,
          problem.id) ? acceptableProblems[problem.id] : defaultDisposition;
    }

    /**
     * Run all test functions.
     *
     * @param problems Array of problem records with tests to run.
     * @param doing What to put in aboutTo.
     */
    function runTests(problems, doing) {
      strictForEachFn(problems, function(problem) {
        var id = problem.id;
        aboutTo = [doing, ': ', problem.description];
        var result = (0,problem.test)();
        if (!earliestTests.has(id)) {
          earliestTests.set(id, result);
        }
        latestTests.set(id, result);

        var report = computeReport(problem);
        reports.set(problem.id, report);

        var repairPerformed =
          repairsPerformed.lastIndexOf(problem.repair) !== -1;

        // Update yetToRepair and plannedSeverity
        if (repairPerformed || !problem.repair ||
            disposition(problem).doNotRepair) {  // repair attempted/absent

          if (report.postSeverity.level > severities.SAFE.level
              && disposition(problem).permit) {
            logger.warn('Problem ignored by configuration (' +
                report.postSeverity.description + '): ' + problem.description);
          } else {
            // Lock in the failure if any, since it is no longer going to be
            // yetToRepair and so won't be counted in currentSeverity.
            self.updateMaxSeverity(report.postSeverity);
          }

          yetToRepair['delete'](id);  // quoted for ES3 compatibility

        } else if (!result) {  // test says OK
          yetToRepair['delete'](id);  // quoted for ES3 compatibility

        } else {  // repair not yet run
          yetToRepair.set(id, problem);
        }
      });
      aboutTo = void 0;
    }

    function computeReport(problem) {
      var status = statuses.ALL_FINE;
      var postSeverity = severities.SAFE;
      var beforeFailure = earliestTests.get(problem.id);
      // TODO(kpreid): We need to define new statuses, and employ them here,
      // for when a test or repair has not yet been run. (In the previous
      // design, reporting could only happen after test/repair/test phases.)
      var afterFailure = latestTests.get(problem.id);
      if (beforeFailure) { // failed before
        if (afterFailure) { // failed after
          if (disposition(problem).doNotRepair) {
            postSeverity = problem.preSeverity;
            status = statuses.REPAIR_SKIPPED;
          } else if (problem.repair) {
            postSeverity = problem.preSeverity;
            status = statuses.REPAIR_FAILED;
          } else {
            if (!problem.canRepair) {
              postSeverity = problem.preSeverity;
            } // else no repair + canRepair -> problem isn't safety issue
            status = statuses.NOT_REPAIRED;
          }
        } else { // succeeded after
          if (problem.repair &&
              repairsPerformed.lastIndexOf(problem.repair) !== -1) {
            if (!problem.canRepair) {
              // repair for development, not safety
              postSeverity = problem.preSeverity;
              status = statuses.REPAIRED_UNSAFELY;
            } else {
              status = statuses.REPAIRED;
            }
          } else {
            postSeverity = problem.preSeverity;
            status = statuses.SYMPTOM_INTERMITTENT;
          }
        }
      } else { // succeeded before
        if (afterFailure) { // failed after
          if (problem.repair || !problem.canRepair) {
            postSeverity = problem.preSeverity;
          } // else no repair + canRepair -> problem isn't safety issue
          status = statuses.BROKEN_BY_OTHER_ATTEMPTED_REPAIRS;
        } else { // succeeded after
          // nothing to see here, move along
        }
      }

      if (typeof beforeFailure === 'string') {
        logger.error('New Symptom (pre-repair, ' + problem.id + '): ' +
            beforeFailure);
        postSeverity = severities.NEW_SYMPTOM;
      }
      if (typeof afterFailure === 'string') {
        logger.error('New Symptom (post-repair, ' + problem.id + '): ' +
            afterFailure);
        postSeverity = severities.NEW_SYMPTOM;
      }

      return {
        id:            problem.id,
        description:   problem.description,
        preSeverity:   problem.preSeverity,
        canRepair:     problem.canRepair,
        urls:          problem.urls,
        sections:      problem.sections,
        tests:         problem.tests,
        status:        status,
        postSeverity:  postSeverity,
        beforeFailure: beforeFailure,
        afterFailure:  afterFailure
      };
    }

    // algorithm for the two ok methods
    function computeOk(actualSeverity, opt_criterionSeverity) {
      if ('string' === typeof opt_criterionSeverity) {
        opt_criterionSeverity = lookupSeverityName(opt_criterionSeverity, true);
      }
      if (!opt_criterionSeverity) {
        opt_criterionSeverity = maxAcceptableSeverity;
      }
      return actualSeverity.level <= opt_criterionSeverity.level;
    }

    //////// Methods /////////

    this.setMaxAcceptableSeverity = function(value) {
      // TODO(kpreid): Check some condition? Do only once?
      // Maybe make this external to the repairer?
      maxAcceptableSeverity = value;
    };

    this.setAcceptableProblems = function(value) {
      if (acceptableProblemsLocked) {
        throw new Error('Too late to setAcceptableProblems.');
      }
      acceptableProblems = value;
    };

    /**
     * The severity of problems which would be known to be observed by code
     * running now with no further repairs. This value may increase if new tests
     * are run or decrease if problems are repaired.
     *
     * This value should be used to determine whether it is yet safe to rely on
     * the guarantees that SES intends to provide.
     *
     * Tests which are registered but not yet run are counted.
     */
    this.getCurrentSeverity = function() {
      var severity = plannedSeverity;
      yetToRepair.forEach(function(problem) {
        if (problem.preSeverity.level > severity.level &&
            !disposition(problem).permit) {
          severity = problem.preSeverity;
        }
      });
      return severity;
    };

    /**
     * The severity of problems which have been confirmed to be present and
     * which are known to be unrepairable. This value can only increase.
     *
     * This value should be used to determine whether SES startup is futile
     * and should be aborted.
     */
    this.getPlannedSeverity = function() {
      return plannedSeverity;
    };

    this.addPostTestKludge = function(value) {
      if (postTestKludge) {
        throw new Error('Only one post-test kludge is supported');
      }
    };

    /**
     * Update the max based on the provided severity.
     *
     * <p>If the provided severity exceeds the max so far, update the
     * max to match.
     */
    // TODO(kpreid): Replace uses of this with higher level ops
    this.updateMaxSeverity = function updateMaxSeverity(severity) {
      if (severity.level > plannedSeverity.level) {
        // This is a useful breakpoint for answering the question "why is the
        // severity as high as it is".
        // if (severity.level > maxAcceptableSeverity.level) {
        //   console.info('Increasing planned severity.');
        // }
        plannedSeverity = severity;
      }
    };

    /**
     * Are all registered problems nonexistent, repaired, or no more severe than
     * opt_criterionSeverity (defaulting to maxAcceptableSeverity)?
     */
    this.okToUse = function okToUse(opt_criterionSeverity) {
      return computeOk(self.getCurrentSeverity(), opt_criterionSeverity);
    };

    /**
     * Are all registered problems nonexistent, repaired, not yet repaired, or
     * no more severe than maxAcceptableSeverity?
     */
    this.okToLoad = function okToLoad() {
      return computeOk(plannedSeverity);
    };

    /**
     * Each problem record has a <dl>
     *   <dt>id:</dt>
     *     <dd>a string uniquely identifying the record, which must
     *         be an UPPERCASE_WITH_UNDERSCORES style identifier.</dd>
     *   <dt>description:</dt>
     *     <dd>a string describing the problem</dd>
     *   <dt>test:</dt>
     *     <dd>a predicate testing for the presence of the problem</dd>
     *   <dt>repair:</dt>
     *     <dd>a function which attempts repair, or undefined if no
     *         repair is attempted for this problem</dd>
     *   <dt>preSeverity:</dt>
     *     <dd>an enum from ses.severities indicating the level of severity
     *         of this problem if unrepaired. Or, if !canRepair, then
     *         the severity whether or not repaired.</dd>
     *   <dt>canRepair:</dt>
     *     <dd>a boolean indicating "if the repair exists and the test
     *         subsequently does not detect a problem, are we now ok?"</dd>
     *   <dt>urls: (optional)</dt>
     *     <dd>a list of URL strings, each of which points at a page
     *         relevant for documenting or tracking the bug in
     *         question. These are typically into bug-threads in issue
     *         trackers for the various browsers.</dd>
     *   <dt>sections: (optional)</dt>
     *     <dd>a list of strings, each of which is a relevant ES5.1
     *         section number.</dd>
     *   <dt>tests: (optional)</dt>
     *     <dd>a list of strings, each of which is the name of a
     *         relevant test262 or sputnik test case.</dd>
     * </dl>
     * These problem records are the meta-data driving the testing and
     * repairing.
     */
    this.registerProblem = function(record) {
      var fullRecord = {
        id:            record.id,
        description:   record.description || record.id,
        test:          record.test,
        repair:        record.repair,
        preSeverity:   record.preSeverity,
        canRepair:     record.canRepair,
        urls:          record.urls || [],
        sections:      record.sections || [],
        tests:         record.tests || []
      };
      // check minimum requirements
      if (typeof fullRecord.id !== 'string') {
        throw new TypeError('record.id not a string');
      }
      if (!/^[A-Z0-9_]+$/.test(fullRecord.id)) {
        // This restriction, besides being a consistent naming convention,
        // ensures that problem IDs can be used as keys indiscriminately, as
        // Object.prototype has no all-uppercase properties.
        throw new TypeError(
            'record.id must contain only uppercase, numbers, and underscores');
      }
      if (typeof fullRecord.test !== 'function') {
        throw new TypeError('record.test not a function');
      }
      if (problemRecords.has(fullRecord.id)) {
        throw new Error('duplicate problem ID: ' + fullRecord.id);
      }
      // TODO(kpreid): validate preSeverity
      problemRecords.set(fullRecord.id, fullRecord);
      notDoneProblems.set(fullRecord.id, fullRecord);
      yetToRepair.set(fullRecord.id, fullRecord);
    };

    this.runTests = function runTestsMethod() {
      var todo = [];
      notDoneProblems.forEach(function(record) { todo.push(record); });
      runTests(todo, 'requested test');
    };

    /**
     * Run a set of tests & repairs.
     *
     * <ol>
     * <li>First run all the tests before repairing anything.
     * <li>Then repair all repairable failed tests.
     * <li>Some repair might fix multiple problems, but run each repair at most
     *     once.
     * <li>Then run all the tests again, in case some repairs break other tests.
     * </ol>
     */
    this.testAndRepair = function testAndRepair() {
      // snapshot for consistency paranoia
      var todo = [];
      notDoneProblems.forEach(function(record) { todo.push(record); });

      runTests(todo, 'pre test');
      strictForEachFn(todo, function(problem) {
        if (latestTests.get(problem.id) && !disposition(problem).doNotRepair) {
          var repair = problem.repair;
          if (repair && repairsPerformed.lastIndexOf(repair) === -1) {
            aboutTo = ['repair: ', problem.description];
            repair();
            repairsPerformed.push(repair);
          }
        }
      });
      runTests(todo, 'post test');

      // TODO(kpreid): Refactor to remove the need for this kludge; repairES5
      // needs a cleanup operation.
      if (postTestKludge) { postTestKludge(); }

      strictForEachFn(todo, function(problem, i) {
        // quoted for ES3 compatibility
        notDoneProblems['delete'](problem.id);
      });

      logger.reportRepairs(strictMapFn(todo, function(problem) {
        return reports.get(problem.id);
      }));
    };

    /**
     * Return a fresh array of all problem reports.
     *
     * Does not include problem records whose tests have not yet been run, but
     * that may be added in the future. TODO(kpreid): do that and define a
     * status enum value for it.
     *
     * Callers should not modify the report records but may deep freeze them
     * (this is not done automatically as Object.freeze may be broken).
     */
    this.getReports = function() {
      var array = [];
      reports.forEach(function(report) {
        array.push(report);
      });
      return array;
    };

    this.wasDoing = function() {
      return aboutTo ? '(' + aboutTo.join('') + ') ' : '';
    };
  }

  // exposed for unit testing
  ses._Repairer = Repairer;

  //////// Singleton repairer /////////

  /**
   * {@code ses.maxAcceptableSeverity} is the max post-repair severity
   * that is considered acceptable for proceeding with initializing SES
   * and enabling the execution of untrusted code.
   *
   * <p>[TODO(kpreid): Rewrite the rest of this comment to better
   * discuss repair-framework vs repairES5.]
   *
   * <p>Although <code>repairES5.js</code> can be used standalone for
   * partial ES5 repairs, its primary purpose is to repair as a first
   * stage of <code>initSES.js</code> for purposes of supporting SES
   * security. In support of that purpose, we initialize
   * {@code ses.maxAcceptableSeverity} to the post-repair severity
   * level at which we should report that we are unable to adequately
   * support SES security. By default, this is set to
   * {@code ses.severities.SAFE_SPEC_VIOLATION}, which is the maximum
   * severity that we believe results in no loss of SES security.
   *
   * <p>If {@code ses.maxAcceptableSeverityName} is already set (to a
   * severity property name of a severity below {@code
   * ses.NOT_SUPPORTED}), then we use that setting to initialize
   * {@code ses.maxAcceptableSeverity} instead. For example, if we are
   * using SES only for isolation, then we could set it to
   * 'NOT_OCAP_SAFE', in which case repairs that are inadequate for
   * object-capability (ocap) safety would still be judged safe for
   * our purposes.
   *
   * <p>As repairs proceed, they update
   * {@code ses._repairer.getPlannedSeverity()} to track the worst case
   * post-repair severity seen so far. When {@code ses.ok()} is called,
   * it return whether {@code ses._repairer.getPlannedSeverity()} is
   * still less than or equal to {@code ses.maxAcceptableSeverity},
   * indicating that this platform still seems adequate for supporting
   * SES.
   *
   * <p>See also {@code ses.acceptableProblems} for overriding the
   * severity of specific known problems.
   */
  ses.maxAcceptableSeverityName =
    validateSeverityName(ses.maxAcceptableSeverityName, false);
  // TODO(kpreid): revisit whether this exists
  ses.maxAcceptableSeverity = ses.severities[ses.maxAcceptableSeverityName];

  ses.acceptableProblems = validateAcceptableProblems(ses.acceptableProblems);

  function validateAcceptableProblems(opt_problems) {
    var validated = {};
    if (opt_problems) {
      for (var problem in opt_problems) {
        // TODO(kpreid): Validate problem names.
        var flags = opt_problems[problem];
        if (typeof flags !== 'object') {
          throw new Error('ses.acceptableProblems["' + problem + '"] is not' +
              ' an object, but ' + flags);
        }
        var valFlags = {permit: false, doNotRepair: false};
        for (var flag in flags) {
          if (valFlags.hasOwnProperty(flag)) {
            valFlags[flag] = Boolean(flags[flag]);
          }
        }
        validated[problem] = valFlags;
      }
    }
    return validated;
  }

  // global instance for normal code path
  // TODO: Think about whether this is a "private" thing.
  ses._repairer = new Repairer();
  ses._repairer.setMaxAcceptableSeverity(ses.maxAcceptableSeverity);
  ses._repairer.setAcceptableProblems(ses.acceptableProblems);
}());
