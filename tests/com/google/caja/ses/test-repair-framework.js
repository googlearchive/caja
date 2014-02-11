// Copyright (C) 2013 Google Inc.
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

function load(script, callback) {
  var el = document.createElement('script');
  el.src = script;
  el.onload = callback;
  document.body.appendChild(el);
}
load('/src/com/google/caja/ses/logger.js', function() {
  load('/src/com/google/caja/ses/repair-framework.js', function() {
    ses.logger.endStartup();  // balance console
    main();
  });
});

function main() {
  'use strict';

  var severities = ses.severities;
  var statuses = ses.statuses;

  jsunitRegister('testOkAndSeverities', function() {
    var repairer = new ses._Repairer();

    // doesn't test okToLoad, but that currently uses the same algorithm, and
    // yetToRepair cases are tested by testSeverityTransitions

    repairer.setMaxAcceptableSeverity(severities.NOT_OCAP_SAFE);
    assertTrue('initial default', repairer.okToUse());
    assertTrue('initial safe',    repairer.okToUse(severities.SAFE));
    assertTrue('initial bad',     repairer.okToUse(severities.NOT_ISOLATED));
  
    repairer.updateMaxSeverity(severities.SAFE_SPEC_VIOLATION);
    assertTrue( 'ssv default', repairer.okToUse());
    assertFalse('ssv safe',    repairer.okToUse(severities.SAFE));
    assertTrue( 'ssv bad',     repairer.okToUse(severities.NOT_ISOLATED));
  
    repairer.updateMaxSeverity(severities.SAFE);  // should have no effect
    assertTrue ('noop default', repairer.okToUse());
    assertFalse('noop safe',    repairer.okToUse(severities.SAFE));
    assertTrue ('noop bad',     repairer.okToUse(severities.NOT_ISOLATED));

    repairer.updateMaxSeverity(severities.NOT_ISOLATED);
    assertFalse('fatal default', repairer.okToUse());
    assertFalse('fatal safe',    repairer.okToUse(severities.SAFE));
    assertTrue ('fatal bad',     repairer.okToUse(severities.NOT_ISOLATED));

    jsunitPass();
  });

  jsunitRegister('testRegisterProblem', function() {
    var repairer = new ses._Repairer();

    expectFailure(function() {
      repairer.registerProblem({ test: function(){} });
    }, 'no id', function(e) { return /\.id\b/.test(e); });

    expectFailure(function() {
      repairer.registerProblem({ id: 'toString', test: function(){} });
    }, 'bad id', function(e) { return /\buppercase\b/.test(e); });

    expectFailure(function() {
      repairer.registerProblem({ id: 'NO_TEST' });
    }, 'no test', function(e) { return /\.test\b/.test(e); });

    repairer.registerProblem({ id: 'DUP', test: function(){} });
    expectFailure(function() {
      repairer.registerProblem({ id: 'DUP', test: function(){} });
    }, 'duplicate', function(e) { return /\bduplicate\b/.test(e); });
    
    jsunitPass();
  });

  jsunitRegister('testSeverityTransitions', function() {
    var repairer = new ses._Repairer();

    assertEquals(severities.SAFE, repairer.getCurrentSeverity());
    assertEquals(severities.SAFE, repairer.getPlannedSeverity());

    var repaired = false;
    repairer.registerProblem({
      id: 'TEST_REPAIRABLE',
      test: function() { return !repaired; },
      repair: function() { repaired = true; },
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true,
    });
    // After registration, problem is assumed to be present since we haven't
    // yet tested for it
    assertEquals('cur reg', severities.SAFE_SPEC_VIOLATION,
        repairer.getCurrentSeverity());
    assertEquals('plan reg', severities.SAFE, repairer.getPlannedSeverity());
    assertEquals('okToUse reg', false, repairer.okToUse());
    assertEquals('okToLoad reg', true, repairer.okToLoad());

    repairer.runTests('pass 1');
    // We have found a problem, and not yet repaired it
    assertEquals('cur 1', severities.SAFE_SPEC_VIOLATION,
        repairer.getCurrentSeverity());
    assertEquals('plan 1', severities.SAFE, repairer.getPlannedSeverity());
    assertEquals('okToUse 1', false, repairer.okToUse());
    assertEquals('okToLoad 1', true, repairer.okToLoad());

    repairer.testAndRepair();

    // It is repaired
    assertEquals('cur repair 1', severities.SAFE, repairer.getCurrentSeverity());
    assertEquals('plan repair 1', severities.SAFE,
        repairer.getPlannedSeverity());
    assertEquals('okToUse repair 1', true, repairer.okToUse());
    assertEquals('okToLoad repair 1', true, repairer.okToLoad());

    repairer.registerProblem({
      id: 'TEST_UNREPAIRABLE',
      test: function() { return true; },
      repair: void 0,
      preSeverity: severities.NOT_OCAP_SAFE,
      canRepair: false,
    });
    repairer.testAndRepair();
    // We have found a problem and it has not been repaired
    assertEquals('cur 2', severities.NOT_OCAP_SAFE,
        repairer.getCurrentSeverity());
    assertEquals('plan 2', severities.NOT_OCAP_SAFE,
        repairer.getPlannedSeverity());
    assertEquals('okToUse 2', false, repairer.okToUse());
    assertEquals('okToLoad 2', false, repairer.okToLoad());

    jsunitPass();
  });

  jsunitRegister('testAcceptableProblems', function() {
    var repairer = new ses._Repairer();

    repairer.setAcceptableProblems({
      'DNR': { doNotRepair: true },
      'PERMIT': { permit: true },
      'PERMIT_DNR': { permit: true, doNotRepair: true },
    });
    var repaired_pd = false;
    var repaired_d = false;
    repairer.registerProblem({
      id: 'PERMIT',
      test: function() { return true; },
      repair: undefined,
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: false,
    });
    repairer.registerProblem({
      id: 'DNR',
      test: function() { return !repaired_d; },
      repair: function() { repaired_d = true; },
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true,
    });
    repairer.registerProblem({
      id: 'PERMIT_DNR',
      test: function() { return !repaired_pd; },
      repair: function() { repaired_pd = true; },
      preSeverity: severities.UNSAFE_SPEC_VIOLATION,
      canRepair: true,
    });
    repairer.runTests('test without repair');
    assertEquals('cur sev 1', severities.SAFE_SPEC_VIOLATION,
        repairer.getCurrentSeverity());
    assertEquals('plan sev 1', severities.SAFE_SPEC_VIOLATION,
        repairer.getPlannedSeverity());
    repairer.testAndRepair();
    assertFalse('not repaired DNR', repaired_d);
    assertFalse('not repaired permit&DNR', repaired_pd);
    // We expect SAFE_SPEC_VIOLATION because problem 'DNR' is doNotRepair but
    // it is not permitted, so its severity should show up.
    assertEquals('cur sev 2', severities.SAFE_SPEC_VIOLATION,
        repairer.getCurrentSeverity());
    assertEquals('plan sev 2', severities.SAFE_SPEC_VIOLATION,
        repairer.getPlannedSeverity());

    jsunitPass();
  });

  jsunitRegister('testRepairOutcomes', function() {
    var repairer = new ses._Repairer();

    repairer.registerProblem({
      id: 'TEST_ALL_FINE',
      test: function() { return false; },
      repair: function() { throw new Error('should not be called'); },
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true
    });
    repairer.registerProblem({
      id: 'TEST_REPAIR_FAILED',
      test: function() { return true; },
      repair: function() {},
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true
    });
    repairer.registerProblem({
      id: 'TEST_NOT_REPAIRED',
      test: function() { return true; },
      repair: void 0,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: false
    });
    repairer.registerProblem({
      id: 'TEST_REPAIR_SKIPPED',
      test: function() { return true; },
      repair: function() { throw new Error('should not be called'); },
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true
    });
    repairer.setAcceptableProblems({
      'TEST_REPAIR_SKIPPED': {doNotRepair: true, permit: true}
    });
    var called_repair_TEST_REPAIRED_UNSAFELY = false;
    repairer.registerProblem({
      id: 'TEST_REPAIRED_UNSAFELY',
      test: function() { return !called_repair_TEST_REPAIRED_UNSAFELY; },
      repair: function() { called_repair_TEST_REPAIRED_UNSAFELY = true; },
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: false
    });
    var called_repair_TEST_REPAIRED = false;
    repairer.registerProblem({
      id: 'TEST_REPAIRED',
      test: function() { return !called_repair_TEST_REPAIRED; },
      repair: function() { called_repair_TEST_REPAIRED = true; },
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true
    });
    repairer.registerProblem({
      id: 'TEST_ACCIDENTALLY_REPAIRED',
      test: function() { return !called_repair_TEST_REPAIRED; },  // not ours!
      repair: void 0,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: false
    });
    repairer.registerProblem({
      id: 'TEST_BROKEN_BY_OTHER_ATTEMPTED_REPAIRS',
      test: function() { return called_repair_TEST_REPAIRED; },  // not ours!
      repair: function() { throw new Error('should not be called'); },
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true
    });
    repairer.registerProblem({
      id: 'TEST_NEW_SYMPTOM',
      test: function() { return 'new symptom!'; },
      repair: function() {},
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true
    });
    // TODO(kpreid): Add tests for all other combinations

    repairer.testAndRepair();
    var reports = repairer.getReports();

    // TODO(kpreid): We should have the repairer provide the reports in this form
    // (or as a StringMap), as this is done here and in repairES5
    var table = {};
    reports.forEach(function (report) {
      table[report.id] = report;
    });

    function assertReport(report, expectStatus, expectBefore, expectAfter,
        expectPostSeverity) {
      assertEquals(report.id + '.status', expectStatus, report.status);
      assertEquals(report.id + '.beforeFailure', expectBefore,
          report.beforeFailure);
      assertEquals(report.id + '.afterFailure', expectAfter,
          report.afterFailure);
      assertEquals(report.id + '.preSeverity',
          severities.SAFE_SPEC_VIOLATION.description,
          report.preSeverity.description);
      assertEquals(report.id + '.postSeverity',
          expectPostSeverity.description,
          report.postSeverity.description);
    }

    assertReport(table.TEST_ALL_FINE,
        statuses.ALL_FINE, false, false, severities.SAFE);
    assertReport(table.TEST_REPAIR_FAILED,
        statuses.REPAIR_FAILED, true, true, severities.SAFE_SPEC_VIOLATION);
    assertReport(table.TEST_NOT_REPAIRED,
        statuses.NOT_REPAIRED, true, true, severities.SAFE_SPEC_VIOLATION);
    assertReport(table.TEST_REPAIR_SKIPPED,
        statuses.REPAIR_SKIPPED, true, true, severities.SAFE_SPEC_VIOLATION);
    assertReport(table.TEST_REPAIRED_UNSAFELY,
        statuses.REPAIRED_UNSAFELY, true, false,
        severities.SAFE_SPEC_VIOLATION);
    assertReport(table.TEST_REPAIRED,
        statuses.REPAIRED, true, false, severities.SAFE);
    assertReport(table.TEST_ACCIDENTALLY_REPAIRED,
        statuses.ACCIDENTALLY_REPAIRED, true, false, severities.SAFE);
    assertReport(table.TEST_BROKEN_BY_OTHER_ATTEMPTED_REPAIRS,
        statuses.BROKEN_BY_OTHER_ATTEMPTED_REPAIRS, false, true,
        severities.SAFE_SPEC_VIOLATION);
    assertReport(table.TEST_NEW_SYMPTOM,
        statuses.REPAIR_FAILED, 'new symptom!', 'new symptom!',
        severities.NEW_SYMPTOM);

    // TODO(kpreid): Add test for NEW_SYMPTOM that verifies the global severity
    // effect of the new symptom, and one which tests the "apparently repaired"
    // behavior (test goes from 'new symptom' -> false)

    jsunitPass();
  });

  jsunitRegister('testSharedRepair', function() {
    var repairer = new ses._Repairer();

    var invoked = 0;
    function repair() { invoked++; }

    repairer.registerProblem({
      id: 'TEST_1',
      test: function() { return invoked == 0; },
      repair: repair,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true
    });
    repairer.registerProblem({
      id: 'TEST_2',
      test: function() { return invoked == 0; },
      repair: repair,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true
    });
    repairer.testAndRepair();
    assertEquals(1, invoked);

    // A second pass doesn't re-invoke either, even if the test fails
    repairer.registerProblem({
      id: 'TEST_3',
      test: function() { return true; },
      repair: repair,
      preSeverity: severities.SAFE_SPEC_VIOLATION,
      canRepair: true
    });
    repairer.testAndRepair();
    assertEquals(1, invoked);

    jsunitPass();
  });

  // Not exactly part of the external interface, but worth testing
  jsunitRegister('testEarlyStringMap', function() {
    var map = new ses._EarlyStringMap();

    assertFalse(map.has('toString'));

    var calls = 0;
    map.set('a', 'b');
    map.forEach(function (value, key, self) {
      assertEquals('a', key);
      assertEquals('b', value);
      assertTrue(self === map);
      calls++;
    });
    assertEquals(1, calls);

    jsunitPass();
  });

  readyToTest();
  jsunitRun();
}