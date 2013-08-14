function testLocation(beSpecific) {
  var assembledHost = window.location.port === ''
      ? window.location.hostname
      : window.location.hostname + ':' + window.location.port;

  assertEquals(
      'host consistent',
      assembledHost,
      window.location.host);
  assertEquals(
      'href consistent',
      window.location.protocol + '//' + assembledHost + window.location.pathname
          + window.location.search + window.location.hash,
      window.location.href);
  // TODO(jasvir): Uncomment after wiring up document.domain
  // by untangling the cyclic dependence between
  // TameWindow and TameDocument
  //    assertEquals(
  //        window.location.hostname,
  //        document.domain);

  // Check that the right answers are given for a specific known URL (whereas
  // the other asserts are only that we are internally consistent).
  if (beSpecific) {
    // (The specific URL shown here is left over from the old location of this
    // test and could perfectly well be changed to something else.)
    assertEquals(
        'href specific',
        'http://' + window.location.hostname
        + ':' + window.location.port
        + '/ant-testlib/com/google/caja/plugin/test-domado-dom-guest.html',
        window.location.href);
    assertEquals(
        'hash',
        '',
        window.location.hash);
    assertEquals(
        'pathname specific',
        '/ant-testlib/com/google/caja/plugin/test-domado-dom-guest.html',
        window.location.pathname);
    assertTrue(
        'port',
        /^[1-9][0-9]+$/.test(window.location.port));
    assertEquals(
        'protocol',
        'http:',
        window.location.protocol);
    assertEquals(
        'search',
        '',
        window.location.search);
  }

  // document.location is an identical property to window.location
  assertTrue('document.location', window.location === document.location);
}
